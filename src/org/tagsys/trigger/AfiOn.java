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
public class AfiOn implements HttpHandler{


	@Override
	public void handle(HttpExchange exchange) throws IOException {
		// TODO read the Barcode from Url
		String query=exchange.getRequestURI().getQuery();
//		System.err.println(this.getClass().getName()+": "+query);

		Headers rh = exchange.getResponseHeaders();
		rh.add("Content-Type", "text/plain");
		
		String body="securing "+query;
		
		exchange.sendResponseHeaders(200, body.length());
		OutputStream resp = exchange.getResponseBody();
		resp.write(body.getBytes());
//		System.err.println("body of "+body.length()+" bytes written");
		resp.close();
		
		JkbdRF.me.setAFI(true,query);
		
		// PLUS: repeat the next Barcode on keyboard
		JkbdRF.me.setlimit(1);

		
	}

}
