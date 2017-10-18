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
public class Stop implements HttpHandler{


	@Override
	public void handle(HttpExchange exchange) throws IOException {
//		String query=exchange.getRequestURI().getQuery();
//		System.err.println(this.getClass().getName()+": "+query);

		Headers rh = exchange.getResponseHeaders();
		rh.add("Content-Type", "text/plain");
		
		String body="";
		
		exchange.sendResponseHeaders(200, body.length());
		OutputStream resp = exchange.getResponseBody();
		resp.write(body.getBytes());
//		System.err.println("body of "+body.length()+" bytes written");
		resp.close();
		
		// PLUS: hold back further barcodes
		JkbdRF.me.setlimit(0);

		
	}

}
