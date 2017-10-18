package org.tagsys;

import java.util.Iterator;

import org.rfid.libdanrfid.Util;
import org.tagsys.desktop.USBCommPorts;

import com.tagsys.protocol.stxe.TSTXe;
import com.tagsys.protocol.stxe.TSTXeErrorException;
import com.tagsys.protocol.stxe.TSTXeSerialRXTX;
import com.tagsys.protocol.stxe.TSTXeTCP;
import com.tagsys.reader.mediop0xx.TMedioP0xx;
import com.tagsys.reader.mediop0xx.TMedioP0xxISO15693Slot;


public class MyTMedioP0xx extends GenericReader {
	TMedioP0xx p0xxReader;
	TSTXe p0xxProtocol;

	private TMedioP0xxISO15693Slot[] slotData = TMedioP0xxISO15693Slot.createArray(32);
	private String[] UIDs=new String[32];

	private void instantiate(){
		TMedioP0xxISO15693Slot[] slotData = TMedioP0xxISO15693Slot.createArray(32);
		String[] UIDs=new String[32];
	}
	
	/**
	 * provides threadsafe ISO15693 chip functions on a Tagsys L-P101
	 * the port is found automagically
	 */
	public MyTMedioP0xx() throws TSTXeErrorException{
		findreader();
//		portdev=USBCommPorts.guessUSB();
//		initprotocol();
		initreader();
	}
	

	/**
	 * provides threadsafe ISO15693 chip functions on Tagsys Readers.
	 * Tested with L-P101 and Medio P200x
	 * @param portdev - the fixed port device name as configured.
	 * Pass a COMn or ttyXXX device and you will connect to the named port.
	 * Pass any other hostname and an IP connect will be started.
	 * @throws TSTXeErrorException
	 */
	public MyTMedioP0xx(String portdev) throws TSTXeErrorException{
		this.portdev=portdev;
		initprotocol();

		initreader();
	}
	
	/**
	 * 
	 * @param strHost - connect to Medio P200x at this host/address
	 * @param iPort - specify the port, if it differs from default 4001
	 * @throws TSTXeErrorException
	 */
	public MyTMedioP0xx(String strHost, int iPort) throws TSTXeErrorException{
		initprotocol(strHost, iPort);
		initreader();
	}
	
	public static void main(String[] args){
		debug=DEBUG_TRACE;
		
		try {
			MyTMedioP0xx me = new MyTMedioP0xx("192.168.0.2",4001);
			if(me.p0xxProtocol.isConnected())
				System.out.println("connected");
			else
				System.out.println("not connected");
			
		} catch (TSTXeErrorException e) {
			e.printStackTrace();
		}
		
		
	}
	
	/**
	 * get the protocol object and connect the reader
	 * field portdev is already set to the current port
	 * NEW: COM and tty names are taken as serial ports, all other connect to tcp
	 * now allowing reconnect to network devices, without searching for serial devices
	 * @throws TSTXeErrorException on failure
	 */
	private void initprotocol() throws TSTXeErrorException{
		
		if((portdev.matches("COM\\d"))||(portdev.contains("tty"))){ // guess: serial port wanted

			p0xxProtocol = new TSTXeSerialRXTX(portdev,115200);
			p0xxProtocol.setMode(TSTXe.FAST_MODE);

		}else{
			iphost=portdev; // TODO: cleanup
			p0xxProtocol = new TSTXeTCP(portdev, ipport);
		}
			p0xxReader = new TMedioP0xx(p0xxProtocol);
			readerFound=p0xxReader.getReaderVersion().toString();

	}
	
	/**
	 * only used on non-default IP Port (!= 4001)
	 * 
	 * get the protocol object and connect the reader
	 * @param strHost - Reader's address
	 * @param iPort - Reader's Port
	 * @throws TSTXeErrorException on failure
	 */
	private void initprotocol(String strHost, int iPort) throws TSTXeErrorException{
		iphost=strHost;
		ipport=iPort;
		p0xxProtocol = new TSTXeTCP(strHost, iPort);
		p0xxReader = new TMedioP0xx(p0xxProtocol);
	}
	
	/**
	 * inspect device at "portdev", either serial or IP and
	 * set up reader, if found. else clean up for next try
	 * @throws TSTXeErrorException
	 */
	void probereader()throws TSTXeErrorException{
//		p0xxProtocol=null; // done in findreader()
		Exception latestEx=null;
		System.err.print("trying "+portdev+" ..");
		
		try {
			initprotocol();
			readerFound=p0xxReader.getReaderVersion().toString();
//			if(0<(DEBUG_TRACE&debug))System.out.print("p0xxReader: "+readerFound);
//			System.err.println("initreader");
//			initreader();
			p0xxReader.setRf(false);
		} 
		catch (TSTXeErrorException e) {
			System.err.println(" no Tagsys reader");
			latestEx=e;
			if(0<(DEBUG_TRACE&debug))System.err.println(e.toString());
			if(null!=p0xxProtocol)p0xxProtocol.close();  // throws another Ex?
			p0xxProtocol=null;
			try {
				Thread.sleep(5);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		catch (Exception e){ //TODO - test this on DOS while COM3 is used by another application
			latestEx=e;
			System.err.println("another exception: ");
			p0xxProtocol=null;
		}		
		
//		if(null==p0xxProtocol)throw (TSTXeErrorException)latestEx; // dann halt nich
	}
	
	/**
	 * start the reader
	 * @return true on no error
	 * @throws TSTXeErrorException 
	 */
	synchronized boolean initreader() throws TSTXeErrorException {
//		try{
		// Reset reader 
		p0xxReader.resetReader();

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		}

		p0xxReader.setRf(true);

		// Set ISO15693 uplink to ASK
		p0xxReader.setUplinkISO15693(TMedioP0xx.ISO15693_ASK, TMedioP0xx.ISO15693_OOK);
		p0xxReader.setDownlinkISO15693(TMedioP0xx.ISO15693_FAST);
/*		}
		catch (TSTXeErrorException e){
			return false;
		}
		*/
		return true;
	}

	public synchronized int autoInventoryISO15693(boolean b, int iso15693NoAfi, int i,
			TMedioP0xxISO15693Slot[] slotData) throws TSTXeErrorException {
		return p0xxReader.autoInventoryISO15693(b, iso15693NoAfi, i, slotData);
	}

	/**
	 * test all com ports to find the reader
	 * @return the reader found - or throw an exception
	 */
	public TMedioP0xx findreader() throws TSTXeErrorException {
		p0xxProtocol=null;
	
		/*
		if(p0xxReader.getInput())
			System.out.println("findreader: p0xxReader.getInput is true");
		
		if(p0xxProtocol.isConnected())
			System.out.println("findreader: resusing connected Protocol");
		*/
		
		if(!"no host".equals(iphost)){
			portdev=iphost; // probe first for an ip reader we found in the first place
			probereader();
		}else{ // look for serial readers only if no iphost given
	
		Iterator<String> itPorts=USBCommPorts.comPorts();
		while((null==p0xxProtocol)&&itPorts.hasNext()){
			portdev=itPorts.next().toString();
			probereader();
		}}
		
		if(0<(debug&DEBUG_TRACE)&&(null!=p0xxProtocol))System.out.println(" ok - connected a "+readerFound);
		return p0xxReader; 
	}

	/**
	 * 
	 * @return p0xxProtocol.isConnected()
	 */
	public boolean isConnected(){
		return ((null!=p0xxProtocol) && p0xxProtocol.isConnected());
	}
	
	public boolean isOpened(){
		return ((null!=p0xxProtocol) && p0xxProtocol.isOpened());
	}
	
	/**
	 * setRf off, close the reader, close the protocol
	 * @return false on Exception
	 */
	public synchronized boolean exitreader() {
		System.err.println("exitreader()");
		try {
			p0xxReader.setRf(false);
			p0xxProtocol.close();
			// is the port really closed? we find stale UUCP lock files..
			// TODO try freeing p0xxProtocol  resource?
		} catch (TSTXeErrorException e) {
			e.printStackTrace();
			readerFound="disconnected by exitreader (failed)";
			return false;
		}
		readerFound="disconnected by exitreader";
		return true;
	}

	/**
	 * turn of RF power for 
	 * @param ms millis
	 */
	public synchronized void pause(int ms) {
		try {
			p0xxReader.setRf(false);
		} catch (TSTXeErrorException e) {
		}
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
			
		try {
			p0xxReader.setRf(true);
		} catch (TSTXeErrorException e) {
		}
	}

	/**
	 * read block i (from Tag UID)
	 * @param i
	 * @param byteUID
	 * @return
	 * TODO change return type to byte to match the ISO functions
	 */
	public synchronized String readSingleBlock(int i, byte[] byteUID) {
		
		TMedioP0xxISO15693Slot mySlot = new TMedioP0xxISO15693Slot();
		try
		{
			if(null==byteUID)
			p0xxReader.readBlockISO15693(i, TMedioP0xx.ISO15693_NONE, null, mySlot);
			else
			p0xxReader.readBlockISO15693(i, TMedioP0xx.ISO15693_ADDRESSED, byteUID, mySlot);
					
			if( !mySlot.containsData() )
			{
				System.err.println("containsData() fails ");
				return "";
			}
			
			if( mySlot.boChipError || mySlot.bySlotStatus != TMedioP0xxISO15693Slot.ERROR_OK )
			{
				System.err.println("ReadBlock Chip Error: "+mySlot.getDataAsString());
				return "";
			}
		}
		catch( TSTXeErrorException e )
		{
			System.err.println("readSingleBlock: "+e.getLocalizedMessage());//"Read failed...");
			return "";
		}
		
		return (Util.reverseHex(mySlot.getDataAsString())); // we know: tagsys switches byte order..
		
//		return (Util.toHex(mySlot.byData)); // nur der block 0 enthaelt daten?!
	}
	
	/**
	 * write one block
	 * @param i - block number
	 * @param byteUID - UID identifying the tag
	 * @param byBlock - the block data in "forward" order
	 */
	public synchronized void writeSingleBlock(int i, byte[] byteUID, byte[] byBlock) {
		
		TMedioP0xxISO15693Slot mySlot = new TMedioP0xxISO15693Slot();
		Util.reverse(byBlock, 0, 4);
		try
		{
			if(null==byteUID)
				return;
				//				throw new Exception("only addressed write, please");
			else
			p0xxReader.writeBlockISO15693(i, 4, byBlock, false, TMedioP0xx.ISO15693_ADDRESSED, byteUID, mySlot);
					
			if( !mySlot.containsData() )
			{
				System.err.println("containsData() fails ");
				return ;
			}
			
			if( mySlot.boChipError || mySlot.bySlotStatus != TMedioP0xxISO15693Slot.ERROR_OK )
			{
				System.err.println("ReadBlock Chip Error: "+mySlot.getDataAsString());
				return ;
			}
		}
		catch( TSTXeErrorException e )
		{
			System.err.println("writeSingleBlock: "+e.getLocalizedMessage());
			return ;
		}
		
	}

	/**
	 * reads the system info
	 * @param byteUID
	 * @return the slot containing the system info as hex string
	 * TODO change to byte[] return 
	 */
	public synchronized String getSystemInformation(byte[] byteUID) {
	//		private TMedioP0xxISO15693Slot getSystemInfo(byte[] byteUID){
				TMedioP0xxISO15693Slot mySlot = new TMedioP0xxISO15693Slot();
				try
				{
					if(null==byteUID)
					p0xxReader.systemInfoISO15693(TMedioP0xx.ISO15693_NONE, null, mySlot);
					else
					p0xxReader.systemInfoISO15693(TMedioP0xx.ISO15693_ADDRESSED, byteUID, mySlot);
							
					if( !mySlot.containsData() )
					{
						System.err.println("containsData() fails ");
						return null;
					}
					
					if( mySlot.boChipError || mySlot.bySlotStatus != TMedioP0xxISO15693Slot.ERROR_OK )
					{
						System.err.println("getSysInfo Chip Error: "+mySlot.getDataAsString());
						return null;
					}
				}
				catch( TSTXeErrorException e )
				{
					System.err.println("getSysInfo: "+e.getLocalizedMessage());//"Read failed...");
					return null;
				}
				
				return (Util.reverseHex(mySlot.getDataAsString()));
	//			return (new String(Util.reverse(mySlot.byData,0,14),0,14)); 
			}

	/**
	 * set the AFI to the value given in 
	 * @param iAFI
	 * @param byteUID - the UID. if not null, addressing mode is used, else ALL chips are changed
	 * @return
	 */
	public synchronized String setAFI(byte iAFI, byte[] byteUID) {
	//		private TMedioP0xxISO15693Slot getSystemInfo(byte[] byteUID){
				TMedioP0xxISO15693Slot mySlot = new TMedioP0xxISO15693Slot();
				boolean boOptionFlag=false;
				try
				{
					if(null==byteUID)
					p0xxReader.writeAfiISO15693(iAFI, boOptionFlag, TMedioP0xx.ISO15693_NONE, null, mySlot);
					else
					p0xxReader.writeAfiISO15693(iAFI, boOptionFlag, TMedioP0xx.ISO15693_ADDRESSED, byteUID, mySlot);
							
					if( !mySlot.containsData() )
					{
						System.err.println("containsData() fails ");
						return null;
					}
					
					if( mySlot.boChipError || mySlot.bySlotStatus != TMedioP0xxISO15693Slot.ERROR_OK )
					{
						System.err.println("Chip Error: "+mySlot.getDataAsString());
						return null;
					}
				}
				catch( TSTXeErrorException e )
				{
					System.err.println("getSysInfo: "+e.getLocalizedMessage());//"Read failed...");
					return null;
				}
				
	//			return (Util.reverseHex(mySlot.getDataAsString()));
	//			return (new String(Util.reverse(mySlot.byData,0,14),0,14)); 
				return mySlot.getSlotInfoAsString();
			}

	public TMedioP0xxISO15693Slot[] autoinventory() {
/*		lots of garbage?
		TMedioP0xxISO15693Slot[] slotData = TMedioP0xxISO15693Slot.createArray(32);
		String[] UIDs=new String[32];
*/		
		int iNbTags=0;
		try {
			iNbTags = autoInventoryISO15693(false, TMedioP0xx.ISO15693_NO_AFI, 32, slotData);
		} catch (TSTXeErrorException e) {
			if(0<(debug&DEBUG_TRACE))System.err.println("autoinventory failed with TSTXeException, portdev: "+portdev);
			try {p0xxProtocol.close();
			}catch (Exception eclose){
				System.err.println("p0xxProtocol was not open?");
			}
			try {
				// we find us here after hardware suspend
				Thread.sleep(1000);
				findreader();
				initreader();
			} catch (TSTXeErrorException e1) {
				System.err.println("inventory: Exc after find/initreader: "+e1.toString());
			} catch (InterruptedException e1) {
				// Auto-generated catch block
				e1.printStackTrace();
			}
			
		}
		
		int iTagIndex = 0;
		while( slotData[iTagIndex++].containsData() ){ // prepare to return a String[]
			UIDs[iTagIndex-1]=slotData[iTagIndex-1].getDataAsString().substring(0, 16); // n.b. there is one trailing null byte..
		}
		return slotData;
	}

	
}
