package org.tagsys.desktop;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.HashSet;

import gnu.io.*;

public class USBCommPorts {

	/**
	 * 
	 * @return the highest USB port device - if none found, null will be returned
	 *
	 */
	public static String guessUSB(){
		String usbportguess=null;
		Iterator<String> itPorts=comPorts();

		while (itPorts.hasNext()){
			String currentdev=itPorts.next().toString();
//System.err.println(currentdev);

			if(currentdev.matches(".+USB.")){
				usbportguess=currentdev.substring(0);
			}
		}
		
		return usbportguess;
	}
/**
 * what are the valid serial port names?
 * @return an Iterator over all serial port names
 */
	public static Iterator<String> comPorts(){
		Enumeration<CommPortIdentifier> ce = CommPortIdentifier.getPortIdentifiers();
		HashSet<String> res=new HashSet<String>();
		
		while (ce.hasMoreElements()){
			String currentdev=((CommPortIdentifier)ce.nextElement()).getName();
			
			res.add(currentdev);
		}
		
		return res.iterator();		
	}
	
	
	public static void main(String[] args){
		Iterator iports=comPorts();
		
		while(iports.hasNext())
		System.out.println(iports.next().toString());

//		System.out.println("guessUSB: "+guessUSB());

	}
	
	
}

