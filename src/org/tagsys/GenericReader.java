package org.tagsys;

public abstract class GenericReader {

	protected String portdev = "no device";
	protected String iphost = "no host";
	protected int ipport = 4001;
	protected String readerFound="no reader yet";
	public static int debug = 0;
	public static final int DEBUG_TRACE = 1;
	public static final int DEBUG_NET = 2;
	public static final int DEBUG_GUI = 4;

	public GenericReader() {
		super();
	}

	public String getReaderInfo(){
		return readerFound;
	}
	
	/**
	 * reads the system info
	 * @param byteUID
	 * @return the slot containing the system info as hex string
	 * TODO change to byte[] return 
	 */
	public abstract String getSystemInformation(byte[] byteUID);

	
	/**
	 * close the reader
	 * @return
	 */
	public abstract boolean exitreader();
	
	/**
	 * turn of RF power for 
	 * @param ms millis
	 */
	public abstract void pause(int ms);
	
	/**
	 * read block i (from Tag UID)
	 * @param i
	 * @param byteUID
	 * @return
	 * TODO change return type to byte to match the ISO functions
	 */
	public abstract String readSingleBlock(int i, byte[] byteUID);
	
	/**
	 * set the AFI to the value given in 
	 * @param iAFI
	 * @param byteUID - the UID. if not null, addressing mode is used, else ALL chips are changed
	 * @return
	 */
	public abstract String setAFI(byte iAFI, byte[] byteUID);
	
//	public abstract String[] autoinventory(); // TODO change to String[]
		
}