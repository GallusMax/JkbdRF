/**
 * 
 */
package org.tagsys.trigger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

/**
 * @author uhahn
 *
 */
public class TriggerLocalHost{

	int thePort = 5656;
	private int backlog=0;
	protected InetAddress localhost;
	protected HttpServer server;

	/**
	 * create a server listening on port
	 * @param listenport
	 */
	public TriggerLocalHost(int listenport) {
		thePort=listenport;
		try {
			localhost = InetAddress.getByAddress(new byte[] {127,0,0,1});
			localhost = InetAddress.getByName("localhost");
			server = HttpServer.create(new InetSocketAddress(localhost,thePort), backlog);
		} catch (IOException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		}

		server.createContext("/off",new AfiOff()); // print the next Barcode
		server.createContext("/on",new AfiOn()); // secure and the next Barcode
		server.createContext("/list",new ListJson()); // the next Barcode
		server.createContext("/next",new ListJson()); // the next Barcode
		server.createContext("/stop",new Stop()); // stop repeating Barcodes
	
		server.start();
		
	}

	public void stop(){
		server.stop(2);
	}
	
}
