package org.tagsys.listwindow;

import java.awt.AWTException;
import java.awt.Frame;
//import java.util.HashMap;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.tagsys.listwindow.KbdRobot;
//import org.eclipse.jetty.util.log.Log;
//import org.rfid.libdanrfid.DDMTag;
//import org.tagsys.protocol.stxe.*;
import org.tagsys.MyTMedioP0xx;
import org.tagsys.WatchTags;
import org.tagsys.trigger.TriggerLocalHost;


public class JkbdRF extends WatchTags{

	private static final String AppName="JKbdRF";
	private static final String AppVersion="0.9.5.3";
	private static final String configFileName = "rfid.conf";
	
	private static int debug=0;

	private static int buttonMap=MyListWindow.SHBT_AFI; // bitmap value defining visibility of function buttons, see MyListWindow.java, default: AFI locking
	protected static int jwsPort=0; // enable JWebSocket server on this port
	
	protected static KbdRobot myrobot;
	protected TriggerLocalHost th = null;
	private static MyListWindow frame;

	public static JkbdRF me=null;
	private static Thread mt;
	
	public JkbdRF(){
				
		try {
			myrobot=new KbdRobot(debug);
		} catch (AWTException e1) {
			// Auto-generated catch block
			e1.printStackTrace();
		}

		// reader will be searched on serial ports, if not set as "port" in configFileName
		portdev=myprops.getProperty("port"); 
		if(0<(DEBUG_TRACE&debug)&&(null!=portdev))System.err.println("port from props: "+portdev);

		// accept only barcodes matching this pattern
		barcodePattern=(myprops.containsKey("barcodePattern") ? myprops.getProperty("barcodePattern") : ".*");
		if(0<(DEBUG_TRACE&debug))System.err.println("pattern from props: "+barcodePattern);
		
		// behave as web service on givon port, NO key events without request on this port
		triggerPort=(myprops.containsKey("triggerPort") ? Integer.parseInt(myprops.getProperty("triggerPort")) : 0);
		if(0<(DEBUG_TRACE&debug))System.err.println("triggerPort from props: "+triggerPort);
		if(0<triggerPort) th = new TriggerLocalHost(triggerPort);

		// only in branch jwebsock
//		jwsPort=(myprops.containsKey("jwsPort") ? Integer.parseInt(myprops.getProperty("jwsPort")) : 0);
//		if(0<jwsPort) jwslistener = new JWSocketListener();
				
		// shown only defined function buttons
		buttonMap=(myprops.containsKey("buttonmap")?Integer.parseInt(myprops.getProperty("buttonmap")) : MyListWindow.SHBT_AFI);

		// set my local ISIL 
		propISIL=myprops.getProperty("ISIL", "DE-705");
		
		// item UID TestAndSet 
		if(myprops.containsKey("TasUrl"))
			strTasUrl=myprops.getProperty("TasUrl"); 
		}
	
	public void initreader(){
		try{
		if(null==portdev) // not set in config file, will be guessed
			theReader=new MyTMedioP0xx(); 
		else
			theReader=new MyTMedioP0xx(portdev);
		}catch (Exception e){ // bail out here
			System.err.println("exiting - no Reader found at "+portdev+": "+e.getMessage());
			gAlert("no Reader at "+portdev, false);
			return ;
		}
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		getprops();
		if(myprops.containsKey("debug")) 
			debug=Integer.parseInt(myprops.getProperty("debug"));

		me = new JkbdRF(); // with respect to debug field

		frame=new MyListWindow(AppName+" v."+AppVersion,buttonMap,me);
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				frame.printout("interrupted");
				me.finish(); // double?!
			}
		});

		me.gAlert("connecting reader..");
		me.initreader();
		
		
		if(null!=me.theReader){ // connected to a Reader
			if(me.modeSelfService())frame.setState(Frame.ICONIFIED);
			me.gAlert(me.theReader.getReaderInfo());
			me.exiting=false;
			me.mainloop();

			// all done 
		}else{
			me.gAlert("no reader found - exiting.");			
			me.finish(); // close ports even when no reader found: fix 150409
		}
		

		
		try {
			Thread.sleep(1000); // give time to read about the failure
		} catch (InterruptedException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		}
		frame.dispose();
	}

	/**
	 * read program configuration from properties file
	 * given in global field configFileName
	 */
	public static void getprops() {
		try {
			FileInputStream propin = new FileInputStream(configFileName);
			WatchTags.myprops.load(propin);
		} catch (FileNotFoundException e) {
			System.err.println(configFileName+" not found, starting with default values");
		} catch (IOException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public String getBarcodePattern() {
		return this.barcodePattern;
	}
	
	/**
	 * type one char on keyboard - to be implemented by subclass
	 * @param s
	 */
	protected void typeBarcode(String s){
		myrobot.repeat(s);
	}
	
	protected void finish() {
		//super.finish(); ??
		if(null!=theReader)theReader.exitreader();
		if(null!=th)th.stop(); // end the server
	
	}

	/**
	 * 	 * self service: dont type out barcodes without being triggered! 
	 * @return true if we are in triggered mode for self service
	 */
	public boolean modeSelfService() {
		return (null != th);
	}

	protected void gRedraw() {
		frame.redraw();  // TODO rather leave a message to the UI thread
	}

	protected void gRemoveFromList(String UID) {
		frame.removefromlist(tagHash.get(UID));
	}

	protected void gAdd2List(String UID) {
		frame.add2list(tagHash.get(UID));  // wieder anzeigen ohne neu lesen
	}

	protected void gAlert(String s, boolean green) {
		frame.printout(s, green);
	}

	protected void gAlert(String s) {
		frame.printout(s);
	}



	
}