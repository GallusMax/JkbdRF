package org.tagsys.listwindow;

import java.awt.AWTException;
import java.awt.AWTKeyStroke;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

public class KbdRobot extends Robot{
	
	public int debug=0;
	static final int TRACE = 8;
	public boolean addlinefeed=true;

	public KbdRobot() throws AWTException {
		super();
		//  Auto-generated constructor stub
	}

	public KbdRobot(int debugme) throws AWTException {
		super();
		debug=debugme;
	}

	public void repeat(String in){
		for(int i=0;i<in.length();i++){
			repeat(in.charAt(i));
		}
		if(addlinefeed)linefeed();
	}

	public void repeat(byte[] in){
		for(int i=0;i<in.length;i++){
			repeat((char)in[i]);
		}
		if(addlinefeed)linefeed();
	}
	
	private void linefeed() {
		keyPress(KeyEvent.VK_ENTER);
		keyRelease(KeyEvent.VK_ENTER);
	}

	public void repeat(char c){
		int keycode=keycodefromchar((c));
		if(0<debug)System.err.println("repeatChar: " + c + ", keycode "+keycode);
		
		if(Character.isLetter(c)){
		if(shiftneeded(c)) keyPress(KeyEvent.VK_SHIFT);
		keyPress(keycode);
		keyRelease(keycode);
		if(shiftneeded(c)) keyRelease(KeyEvent.VK_SHIFT);
		return;
		}
		if(Character.isDigit(c)){
			keyPress(keycode);
			keyRelease(keycode);
			return;
		}
		AWTKeyStroke ks=null,ksfallback=null;
		switch(c){
		case '$': 
			ks = KeyStroke.getKeyStroke(KeyEvent.VK_4,InputEvent.SHIFT_DOWN_MASK);
			break;
		case '/': 
			//ks = KeyStroke.getKeyStroke(KeyEvent.VK_SLASH,0); // broken in windows..
			//ks = KeyStroke.getKeyStroke(KeyEvent.VK_7,InputEvent.SHIFT_DOWN_MASK);
			ks = KeyStroke.getKeyStroke(KeyEvent.VK_DIVIDE,0);
			break;
		case ';': 
			ks = KeyStroke.getKeyStroke(KeyEvent.VK_SEMICOLON,InputEvent.SHIFT_DOWN_MASK);
			break;
		case ':': 
			ks = KeyStroke.getKeyStroke(KeyEvent.VK_COLON,InputEvent.SHIFT_DOWN_MASK);
			break;
		}
		try {
			typekey(ks);
		} catch (Exception e) { // what if windows doesnt like ?
			typekey(ksfallback);
		}
	}
	
	protected void typekey(AWTKeyStroke ks){
		if(null!=ks){
			if(0<(InputEvent.SHIFT_DOWN_MASK&ks.getModifiers())) keyPress(KeyEvent.VK_SHIFT);
			if(0<debug)System.err.println("sending ks.keycode "+ks.getKeyCode());
			keyPress(ks.getKeyCode());
			keyRelease(ks.getKeyCode());
			if(0<(InputEvent.SHIFT_DOWN_MASK&ks.getModifiers())) keyRelease(KeyEvent.VK_SHIFT);
		}
	}
	
	protected int keycodefromchar(char c){
//		return KeyStroke.getKeyStroke('$').getKeyCode(); // nicht gut..
		
		if(0<(TRACE & debug)){
		System.err.print(c);
		System.err.print((int)c & 0x7f);
		System.err.print(" ");
		}
		c=Character.toUpperCase(c);
		if(0<(TRACE & debug)){
		System.err.print(c);
		System.err.println((int)c & 0x7f);
		}
//		if((char)0 == c)return KeyEvent.VK_ENTER;
		return (int)c & 0x7f;
	}

	protected static boolean shiftneeded(char ch){
//		return (((int)ch>=0x41) && ((int)ch <=0x5a));
		//\u0041' through '\u005a
		return !Character.isLowerCase(ch)&&Character.isLetter(ch);
	}
}

