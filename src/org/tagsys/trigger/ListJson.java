/**
 * 
 */
package org.tagsys.trigger;

import org.tagsys.listwindow.*;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * @author uhahn
 *
 */
public class ListJson implements HttpHandler{


	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String query=exchange.getRequestURI().getQuery();
		int newlimit=1;
//		System.err.println(this.getClass().getName()+": "+query);

		Headers rh = exchange.getResponseHeaders();
		rh.add("Content-Type", "text/plain");
		
		String body=JkbdRF.me.repeatjson();
		
		exchange.sendResponseHeaders(200, body.length());
		OutputStream resp = exchange.getResponseBody();
		resp.write(body.getBytes());
//		System.err.println("body of "+body.length()+" bytes written");
		resp.close();

		if(null!=query)
			try{
			newlimit=Integer.parseInt(query);
			}
			catch (Exception e){
				System.err.println("only numbers allowed: "+query);
			}
		// PLUS: repeat the next Barcode on keyboard
		JkbdRF.me.setlimit(newlimit);

		
	}

}
