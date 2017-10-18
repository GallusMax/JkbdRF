package org.tagsys.trigger;

import org.tagsys.listwindow.JkbdRF;

public class Localhost6666 extends Thread{

	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {

		new TriggerLocalHost(2666);

		JkbdRF me = new JkbdRF();
		me.populate("e00401020304", "705/0$1234567");
		me.populate("e00401020305", "705/0$1234568");
		me.populate("e00401020306", "705/0$1234566");
		me.populate("e00401020307", "705/0$1234567");
		me.populate("e00401020308", "705/0$1234568");
				
		while(true){
			sleep(1000L);
			if(0<(me.declimit())){ // count down allowed prints
				sleep(1000L);
				me.repeatnext(); // print out the next Barcode
			}
		}
	}

}
