package org.tagsys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.rfid.TagTAS;
import org.rfid.libdanrfid.DDMData;
import org.rfid.libdanrfid.DDMTag;
import org.rfid.libdanrfid.Util;

import com.tagsys.protocol.stxe.TSTXeErrorException;
import com.tagsys.reader.mediop0xx.TMedioP0xxISO15693Slot;

/**
 * Monitor for RFID presence
 * keeps track of "visible" Tags and caches their content
 * maintains known/visible/new tags in tagHash, ageHash and repeatHash respectively
 * 
 * @author uhahn
 * 
 */
public class WatchTags implements Runnable {

	public MyTMedioP0xx getTheReader() {
		return theReader;
	}

	public String getLasterror() {
		return lasterror;
	}

	public void setTheReader(MyTMedioP0xx givenReader) {
		theReader = givenReader;
	}

	public ArrayList<String> tagList;

	/**
	 * 
	 * @param givenReader allows for assigning a reader to the thread
	 */
	public WatchTags(MyTMedioP0xx givenReader) {
		this();
		theReader=givenReader;
	}

	/**
	 * 
	 * @param givenHostname assign a network reader as hostname (or port)  
	 */
	public WatchTags(String givenHostname) {
		this();
		portdev=givenHostname;
	}
	
	public WatchTags() {
		super();
		tagHash= new HashMap<String, DDMTag>(); // local tag store
		repeatHash= new HashMap<String, Integer>(); // list of tags to be printed out
		ageHash= new HashMap<String, Integer>(); // countdown for visibility of each tag
		tagList= new ArrayList<String>();
	}

	private final Integer tagVisibleCount = 10;
	private final Integer tagForgetCount = -100;
	public int debug = 0;
	public final int DEBUG_TRACE = 1;
	public final int DEBUG_NET = 2;
	public final int DEBUG_GUI = 4;
	public final int DEBUG_KBD = 8;
	
	protected String portdev="p200"; // preset a reader as host p200, if not set by properties
	protected MyTMedioP0xx theReader = null;
	public static HashMap<String, DDMTag> tagHash;
	protected static HashMap<String, Integer> ageHash;
	protected static HashMap<String, Integer> repeatHash;

	private boolean changeTagVisibility = false; // triggers display update?
	
	protected static Properties myprops = new Properties(); // filled from main class
	
	protected String strTasUrl = "http://www.eanco.de/tagtas.cgi"; // default, if not read from properties
	public boolean exiting = true; // stops mainloop() and represents running state, too
	protected String barcodePattern;
	protected int triggerPort = 0;
	private int repeatlimit = 0;
	public String lasterror;
	protected String propISIL="DE-705"; // default 
	
	/**
	 * announce the final round: thread will exit. 
	 */
	public void terminate(){
		exiting=true;
	}
	
	protected void mainloop() {
			
			while(!exiting){ // signal to exit main loop
				changeTagVisibility=false;
				// TODO what if theReader is null ~ not set yet?
				inventory();
				if(0<(getlimit())) // count down allowed prints
					repeatnext(); // print out the next Barcode
				if(0==triggerPort)setlimit(2000); // ignore limit when not listening ;-)
				if(1500<getlimit())setlimit(2000); // keep running if above 1500
				agetags();
				
				if(0 == tagHash.size()){ // nichts mehr beobachtet: idle
					theReader.pause(1000); 
				}
				
				if(changeTagVisibility){ 
					gRedraw();  // TODO UI notify?
					if(0<(debug&DEBUG_TRACE))
						dumptaghash(); // tracing
				}
			}
		}

	/**
	 * self service: dont type out barcodes without being triggered! 
	 * @return true if we are in triggered mode for self service
	 */
	public boolean modeSelfService() {
		return true; // TODO always true?
	}

	public boolean modeJWS() {
	//		return (null!=jwslistener);
			return false;
		}

	public synchronized int getlimit() {
		return repeatlimit;
	}

	public synchronized int declimit() {
		return repeatlimit--;
	}

	public synchronized void setlimit(int l) {
		repeatlimit=l;
	}

	/**
	 * 
	 * @return TODO json representation from tags not yet repeated
	 */
	public String repeatjson() {
	//		System.err.println("repeatjson");
	//		Iterator<DDMTag> it = repeatHash.values().iterator();  // now holds number of items for this barcode..
	//		Collection<DDMTag> coll = repeatHash.values();
	return		repeatHash.keySet().toString(); // 
	//		return coll.toString();
		}

	public static String repeatdummyjson() {
		return "[{\"UID\":\"e00402\",\"bar\":\"705/0$1234567\"}," +
				"{\"UID\":\"e00402\",\"bar\":\"705/0$1234568\"}"+
				" ]";
	}

	/**
	 * change all (visible) tags 
	 * @param on - boolean secure on true, unsecure on false
	 * @return - currently always 0, count of changed tags not available
	 */
	public int setAFI(boolean on){
		//				System.err.println("processing AFI secure");
		String res=theReader.setAFI(on?DDMTag.AFI_ON:DDMTag.AFI_OFF, null); // set ALL tags
		if(null!=res){  //System.err.println(res);
			updateVisibleAFI(on?DDMTag.AFI_ON:DDMTag.AFI_OFF);
		}
		return 0; // TODO - always return 0 ??
	}
	
	protected void gRedraw(){
//		frame.redraw();  // TODO rather leave a message to the UI thread
	}
	
	protected void gRemoveFromList(String UID){
//		frame.removefromlist(tagHash.get(UID));
		tagList.remove(UID);
	}
	protected void gAdd2List(String UID){
//					frame.add2list(tagHash.get(UID));  // wieder anzeigen ohne neu lesen
		tagList.add(UID);
	}
	
	protected void gAlert(String s,boolean green){
//		frame.printout(s, green);
	}

	protected void gAlert(String s){
//		frame.printout(s);
	}
	
	/**
	 * change AFI of all (visible) tags with the barcode bar 
	 * @param on - boolean secure on true, unsecure on false
	 * @param bar - the barcode affected
	 * @return - the count of changed tags
	 */
	public int setAFI(boolean on, String bar) {
		if(null==bar)return 0;
		Iterator<String> it = tagHash.keySet().iterator();
		String curUID;
		int foundcount=0;
		while(it.hasNext()){
			curUID=it.next();
			if(bar.equals(tagHash.get(curUID).Barcode())){ // we found ONE 
				// address only VISIBLE tags (according to ageHash) ? does not hurt to address tags that wont listem..
				byte[] byteUID = Util.hexStringToByteArray(curUID);
				try{
					for(int i=0;i<3;i++)
						theReader.setAFI((on ? DDMTag.AFI_ON : DDMTag.AFI_OFF), byteUID);
					tagHash.get(curUID).setAFI((on ? DDMTag.AFI_ON : DDMTag.AFI_OFF));
				}
				catch(Exception e){
					System.err.println("setAFI "+e.getMessage());
				}
				foundcount++;
			}
		}
		gRedraw();
		return foundcount;
	}
	
	protected String[] splitISIL(){
		return propISIL.split("-");
	}
	
	/**
	 * 
	 * @return default country code for new tags.
	 * 
	 *  read from propISIL
	 */
	public String getdefaultCountry(){
//		return propISIL.substring(0,2).toUpperCase(); //uuuh
		String[] si=splitISIL();
		if(si.length>1)
			return si[0];
		System.err.println("ignoring malformed ISIL: "+propISIL);
		return "DE";
	}

	/**
	 * 
	 * @return default ISIL for new tags.
	 * 
	 *  read from propISIL
	 */
	public String getdefaultISIL(){
		String[] si=splitISIL();
		if(si.length>1)
			return si[1];
		return "705";
	}
	
	
	/**
	 * write the barcode to the tag, other data taken from default
	 * @param bar - the barcode to be written
	 * @return the count of tags involved
	 */
	public int convert(String bc){
		if(null==bc)return 0;
		Iterator<String> it = tagHash.keySet().iterator();
		int nParts = tagHash.keySet().size();
		String curUID;
		DDMTag newTag = new DDMTag();
		newTag.setBarcode(bc);
		newTag.setAFI(DDMTag.AFI_ON); // ??
		newTag.setCountry(getdefaultCountry());
		newTag.setISIL(getdefaultISIL());
		newTag.setofParts(nParts);
		newTag.setPartNum(1);
		newTag.setUsage(DDMTag.V1AUSLEIHBAR);
		newTag.updateCRC();
		
		int foundcount=0;
		while(it.hasNext()){	// TODO keep up with the counter
			foundcount++;
			curUID=it.next();
			newTag.setPartNum(foundcount); // prepare for multi tags
			newTag.updateCRC();
			byte[] byteUID=Util.hexStringToByteArray(curUID);
			// write new tag content to the tag curUID
			for(int i=0;i<8;i++)
				theReader.writeSingleBlock(i, byteUID, newTag.getblock(i));
		}
		
		return foundcount; // return count of converted tags
	}
	
	/**
	 * type one char on keyboard - to be implemented by subclass
	 * @param s
	 */
	protected void typeBarcode(String s){
//		myrobot.repeat(it.next());
	}
	
	/**
	 * pop off one tag and print the barcode
	 */
	public void repeatnext() {
	//		System.err.print("repeatnext ");
	//		if(modeJWS() && ! jwslistener.isAlive()) return; // dont waste a tag
			
			Iterator<String> it = repeatHash.keySet().iterator(); // keySet of barcodes
			if(it.hasNext()){
	//			System.err.print("calling robot");
					typeBarcode(it.next());
					it.remove(); // dont do that w/o .next() !
					declimit(); // count down 
			}
		}

	public void dumptaghash() {
		Iterator<String> it = tagHash.keySet().iterator();
		int i=0;
		while(it.hasNext()){
			String key=it.next();
			if(0<ageHash.get(key)){
				System.out.print(i + " ");
				System.out.print(key + " ");
				System.out.println(tagHash.get(key).Barcode());
	//			System.out.println(tagHash.get(key).toString());
			}
			i++;
		}}

	/**
	 * update AFI field in our cache (display..)
	 * @param newafi
	 */
	public void updateVisibleAFI(byte newafi) {
		Iterator<String> it = ageHash.keySet().iterator();
		while(it.hasNext()){
			String tagUID = it.next();
			int age=ageHash.get(tagUID);
			if(0<age){ // this tag is currently visible
				if(tagHash.containsKey(tagUID)){
					tagHash.get(tagUID).setAFI(newafi);
			}	}
		}
		gRedraw();
	}

	/**
	 * age out tags lost from current inventory after tagVisibleCount runs,
	 * forget tags only after tagForgetCount further runs
	 */
	public void agetags() {
			Iterator<String> it = ageHash.keySet().iterator();
			while(it.hasNext()){
				String key = it.next();
				int age=ageHash.get(key) - 1;
				if(0==age){ // item to become invisible
					if(tagHash.containsKey(key)){
					changeTagVisibility=true; // announce the dropping of this item
					
					gRemoveFromList(key);
//					frame.removefromlist(tagHash.get(key));
					
					repeatHash.remove(tagHash.get(key).Barcode()); // forget barcodes, too
					
				}  	}
				if(tagForgetCount>age){
	//				ageHash.remove(key); // would change hash backing the Set, backing the Iterator:bad!
					// remove from both hashes
					it.remove();
					if(tagHash.containsKey(key)){
						repeatHash.remove(tagHash.get(key).Barcode()); // forget barcodes, too
						tagHash.remove(key);
					}
					// announce remaining number of tags in hash
					if(0==tagHash.size())
						gAlert("los!", true);
//						frame.printout("los!", true);
					else
						gAlert(" "+tagHash.size()); 
//						frame.printout(" "+tagHash.size()); 
				}else // dont forget, but keep the new age
					ageHash.put(key, age);
			}
		}

	/**
	 * force empty current tags, forcing a reread
	 */
	public void forgettags() {
		Iterator<String> it = tagHash.keySet().iterator();
		while(it.hasNext()){
			String key = it.next();
			gRemoveFromList(key);

			repeatHash.remove(tagHash.get(key).Barcode()); // forget barcodes, too

//			frame.removefromlist(tagHash.get(key));
			it.remove();
		}
	}

	/**
	 * testing: fill some dummy values
	 * @param UID - the tag UID
	 * @param bc - the Tag's barcode
	 */
	public void populate(String UID, String bc) {
		DDMTag newTag = new DDMTag();
		newTag.setBarcode(bc);
		
		tagHash.put(UID, newTag);
		repeatHash.put(bc, 1);
	}

	/**
	 * get UIDs of all currently visible tags
	 * read userdata into hash value
	 */
	public int inventory() {
			TMedioP0xxISO15693Slot[] slotData = theReader.autoinventory();
					
			int iTagIndex = 0;
			while( slotData[iTagIndex++].containsData() )
			{
				int myindex=iTagIndex - 1;
				String UID=slotData[myindex].getDataAsString().substring(0, 16); // n.b. there is one trailing null byte..
				if((tagHash.containsKey(UID))&&(0>=ageHash.get(UID))){ // noch im tagHash, aber war schon ausgeblendet
					gAdd2List(UID);
					//					frame.add2list(tagHash.get(UID));  // wieder anzeigen ohne neu lesen
					//putintorepeat(tagHash.get(UID)); // problem: repeats too fast - wieder zur Ausgabe freigeben
				}
				ageHash.put(UID, tagVisibleCount);
				if(!tagHash.containsKey(UID)){  // NUR beim ersten Mal..
					DDMTag newTag=new DDMTag();
						String newsysinfo=theReader.getSystemInformation(Util.hexStringToByteArray(UID));
	//					System.err.println(newsysinfo);
											
					if(null==newsysinfo) continue;
						newTag.addSystemInformation(newsysinfo);
	//					System.err.println("compare UID "+UID+" with tag.getUID "+newTag.getUID());
	//					System.err.println("block size: "+newTag.getblocksize()+", #blocks: "+newTag.getnBlocks());
						//TODO with respect to chip size..
						String userdata=readUserData(Util.hexStringToByteArray(UID));
					
					if(userdata.isEmpty()) continue; // bailing out
	
					// TODO: plausibility?
						newTag.addUserData(Util.hexStringToByteArray(userdata));
						tagHash.put(UID, newTag);
	//					System.err.println(UID);
						gAdd2List(UID);
//						frame.add2list(tagHash.get(UID)); 
	
	/*					
						// here, push the tag to inventory system
						try {
							if(modeJWS() && jwslistener.isAlive()){
								jwslistener.msgtag(newTag);
							}
						} catch (Exception e) {
							System.err.println("inventory: Websocket message sending failed: "+newTag.Barcode());
						}
	*/
						if(newTag.barcodematch(barcodePattern)){ 
							if(0<(DEBUG_NET & debug))System.err.println("spawning TagTAS");
							new Thread(new TagTAS(strTasUrl, UID, newTag.Barcode())).start();
	//						myrobot.repeat(newTag.Barcode()); // worked good until now..
							// fill repeathash here, and remove again, when removed from field?
							if(modeJWS()|| // inventory mode: all media!
									!modeSelfService() || (DDMData.V1AUSLEIHBAR == (newTag.getUsage()))) // only lendable media? TODO - DDMData Version dependent?
								putintorepeat(newTag);
						}else{ // foreign barcode? save it..
							TagTAS tasUdat=new TagTAS(strTasUrl, UID);
							tasUdat.addUserData(userdata);
							System.err.println("posting "+UID+":"+userdata);
							new Thread(tasUdat).start();
						}						
						changeTagVisibility=true; // remember refresh needed
				}
			}
			
			return iTagIndex;
	
		}

	/**
	 * put Tag into repeatHash, preparing output
	 * @param newtag
	 */
	private void putintorepeat(DDMData newTag) {
		
		// fill repeathash containing barcodes for output
		if(repeatHash.containsKey(newTag.Barcode())){ // more than one item (will never occur - see below :-)
			int items=repeatHash.get(newTag.Barcode());
			repeatHash.put(newTag.Barcode(), items + 1);
			System.err.format("repeatHash alert: additional tag %s (now %d..) \n",newTag.Barcode(),items+1);
		}else{
			if(newTag.getofParts()==visiblecount(newTag.Barcode())){ // only if all item parts are visible
			repeatHash.put(newTag.Barcode(),newTag.getofParts()); // keep track of tags to be typed out
			}}
		
		// fill inventoryhash, too
		//TODO - read UID from DDMTag
		//Log.info("UID from DDMTag "+newTag.getUID());
		//inventoryHash.put(newTag.getUID(),1);
	}

	/**
	 * find out how many items of this barcode are currently visible on antenna
	 * @param barcode
	 * 
	 * @return - the count of visible items with this barcode
	 */
	private int visiblecount(String barcode) {
		Iterator<DDMTag> it = tagHash.values().iterator();
		int count = 0;
		while(it.hasNext()){
			DDMData tag = it.next();
			if(barcode.equals(tag.Barcode())) count++;
		}
		return count;
	}

	private String readUserData(byte[] byteUID) {
		String res="";
		for(int i=0;i<8;i++){ // TODO respect blocksize
			String sblock=theReader.readSingleBlock(i,byteUID); // TODO: fix REVERSED order here!
			if(sblock.isEmpty()) return ""; // bail out here
			res=res.concat(sblock);
		}
		return res;
	}

	private void writeUserData(byte[] byteUID, byte[] udata) {
		byte[] byteBlock= new byte[4];
		for(int i=0;i<8;i++){ // TODO respect blocksize
			for(int j=0;j<4;j++)
				byteBlock[j]=udata[4*i+j];
			theReader.writeSingleBlock(i,byteUID, byteBlock); // TODO: fix REVERSED order here?
		}
	}

	/**
	 * probe the reader's connectivity
	 * @return the reader's isConnected() answer
	 */
	public boolean connected(){
		return (null!=theReader) && theReader.isConnected();
	}
	
	public String getReaderInfo(){
		return (null==theReader)?"no reader":theReader.getReaderInfo();
	}
	
	@Override
	public void run() {
		if(0<(debug&DEBUG_TRACE))System.err.println("Watchtags: new thread");
		exiting=false;
		if(null==theReader)
			try {
				theReader=new MyTMedioP0xx(portdev); // port from props or default hostname
			} catch (TSTXeErrorException e) {
				e.printStackTrace();
				lasterror=e.toString();
			}	
		if(connected())
			mainloop();
		
		exiting=true;
	}	

}