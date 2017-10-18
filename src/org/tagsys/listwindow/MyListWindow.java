package org.tagsys.listwindow;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.metal.MetalCheckBoxIcon;

import org.rfid.libdanrfid.DDMData;
import org.rfid.libdanrfid.DDMTag;
import org.tagsys.WatchTags;

/* ListDemo.java requires no other files. */
//public class ListDemo extends JPanel  {

public class MyListWindow extends JFrame implements ListSelectionListener,KeyListener, WindowListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 2600930511953552248L;
	private JList list;
	private DefaultListModel listModel;
	private JScrollPane listScrollPane;
	
	// ressources?
	private static String titleString = "JKeyboardRF";
	private static final String strUnsecure = "Entsichern";
	private static final String strSecure = "Sichern";
	private static final String strClone = "Tag klonen";

	private JkbdRF mainme;
	
	// moved to local 
	// private JButton btSecure;
	private JTextField edInfo;
	private JTextField edConvert;
	
	static MyListWindow frame;
	static KbdRobot myrobot;
	
	public boolean AFIsecureWanted=false;
	public boolean AFIunsecureWanted=false;
	public boolean WindowCloseWanted=false;
	JComponent newContentPane;
	protected ImageIcon ic_lock;
	protected ImageIcon ic_unlock;
	private ImageIcon ic_lock_dist;
	private ImageIcon ic_unlock_dist;
	private int iconSize=30;
	private String strStatus="Status";
	private String strConvert="Konvertieren";
	public static final int SHBT_AFI=1;
	public static final int SHBT_CLONE=2;
	public static final int SHBT_STAT=4;
	public static final int SHBT_CONV=8;
	
	private int bitmapButtons=SHBT_AFI; // erstmal nur AFI zeigen
	
	protected static final Runtime ourRuntime = Runtime.getRuntime();
	
	public MyListWindow() {
		this("",SHBT_AFI,null);
		frame=this;
	}
	
	public MyListWindow(String WindowTitle,int bitmapButtons,JkbdRF mainme) {
		this.bitmapButtons=bitmapButtons;
		frame=this;
		titleString=WindowTitle;
		//super(new BorderLayout());
		this.mainme=mainme;

		// UH this is rather experimental ..
		// and does still not close the window on windows shutdown
		ourRuntime.addShutdownHook(new Thread(new Runnable() {
			
			@Override
			public void run() {
				// request window closing
//				windowClosed(null);
				frame.WindowCloseWanted=true;
				JkbdRF.me.exiting=true;
			}
		}));
		
		initGUI();
		
/*
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.setResizable(true);
		this.setTitle("show me");
*/	
		//Display the window.
		pack();
//		if(0!=bitmapButtons)
			setVisible(true);
	}

	public MyListWindow(String WindowTitle) {
		this(WindowTitle,SHBT_AFI,null);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		JkbdRF mainme=new JkbdRF();
		frame=new MyListWindow("testing..",(SHBT_AFI),mainme);
		
//		JWSocketListener jl = new JWSocketListener();
		
		byte[] buffer=new byte[128];
		
		while(!frame.WindowCloseWanted){
			try {
				int len=System.in.read(buffer);
				
//				InputStreamReader sr = new InputStreamReader(System.in);
//				len=sr.read(cbuf);
				
// 				StringBuilder sb=new StringBuilder();
				StringBuffer sb=new StringBuffer(); // StringBuffer is threadsafe
				for(int i=0;i<len;i++)
					sb.append((char)buffer[i]);

				System.out.println(sb.toString());
				
//				myrobot.repeat(sb.toString());
				switch(buffer[0]){//some command jokes
				case '-':
					frame.removefromlist(); // special: remove last entry
					break;
					
				case '+':
					frame.testAFItoggle();
					break;
				
				default:
					DDMTag tag=new DDMTag();
					tag.setBarcode(sb.toString());
					//tag.setAFI(DDMTag.AFI_OFF);
					frame.add2list(tag);
										
				}
			} catch (IOException e) {
				//  Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	private void testAFItoggle() {
		// TODO comment this out after testing...
		// toggle the AFI value of the last item in list
		DDMTag theTag=((DDMTag)(listModel.firstElement()));
		byte bAFIwas = theTag.getAFI();
		theTag.setAFI(DDMTag.AFI_ON == bAFIwas? DDMTag.AFI_OFF:DDMTag.AFI_ON); // toggle the AFI

		redraw();
	}

	public void resizeicon(ImageIcon ic,int size){
			try {
				ic.setImage(ic.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
			} catch (Exception e) {
				//  Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	private void initGUI(){
		//Create and set up the window.
		
		setTitle(titleString);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		addWindowListener(this);
		
		setAlwaysOnTop(true);
			
		ic_lock_dist=new ImageIcon(getClass().getResource("/drawable/ic_locktrans.png"),"Gesichert");
		ic_unlock_dist=new ImageIcon(getClass().getResource("/drawable/ic_unlocktrans.png"),"Entsichert");
		ic_lock=ic_lock_dist; resizeicon(ic_lock,iconSize);
		ic_unlock=ic_unlock_dist; resizeicon(ic_unlock,iconSize);
		/*/ Create and set up the content pane. (defaults OK)
		newContentPane = new JPanel(new BorderLayout(0, 0));
		newContentPane.setOpaque(true); //content panes must be opaque
		
		setContentPane(newContentPane);
		*/
		
	listModel = new  DefaultListModel();
/*	listModel.addElement("Jane Doe");
	listModel.addElement("John Smith");
	listModel.addElement("Kathy Green");
	*/
//Create the list and put it in a scroll pane.
	list = new JList(listModel);
	list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	list.setSelectedIndex(0);
	list.addListSelectionListener(this);
	list.setVisibleRowCount(10);
	list.setCellRenderer(new MyCellRenderer());
	
	listScrollPane = new JScrollPane(list);
	
//	listScrollPane.addKeyListener(this);
	
	
	add(listScrollPane, BorderLayout.CENTER);
	
	JPanel buttonPane = new JPanel();
	buttonPane.setLayout(new BoxLayout(buttonPane,BoxLayout.PAGE_AXIS));
	add(buttonPane, BorderLayout.SOUTH);
	buttonPane.add(prepareInfoPane(), BorderLayout.CENTER);
	if(0<(bitmapButtons&SHBT_AFI))buttonPane.add(prepareAFIButtonPane(), BorderLayout.CENTER);
//	add(list);
	
	if(0<(bitmapButtons&SHBT_CLONE))buttonPane.add(prepareCloneButtonPane(), BorderLayout.CENTER);
	if(0<(bitmapButtons&SHBT_STAT))buttonPane.add(prepareStatuButtonPane(), BorderLayout.CENTER);
	if(0<(bitmapButtons&SHBT_CONV))buttonPane.add(prepareConvertButtonPane(), BorderLayout.CENTER);
	
	try {
		myrobot = new KbdRobot(3);
	} catch (AWTException e1) {
		// Auto-generated catch block
		e1.printStackTrace();
	}
	
	}

	private void unusedGridbag(){
		GridBagLayout thisLayout = new GridBagLayout();
//		this.getContentPane().setLayout(thisLayout);
		thisLayout.columnWidths = new int[] {1,1,1};
		thisLayout.rowHeights = new int[] {1,1,1};
		thisLayout.columnWeights = new double[] {0.1,0.1,0.1};
		thisLayout.rowWeights = new double[] {0.1,0.1,0.1};
		this.setSize(new java.awt.Dimension(400,300));
		this.setLocale(new Locale("de", "German"));

	}
	
	/**
	 * 
	 * @return status line
	 */
	protected JPanel prepareInfoPane(){
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane,BoxLayout.LINE_AXIS));
		buttonPane.add(Box.createHorizontalStrut(5));
		edInfo=new JTextField("welcome..");
		buttonPane.add(edInfo);
		buttonPane.add(Box.createHorizontalStrut(5));
		return buttonPane;
	}
	
	/**
	 * 
	 * @return the sichern/entsichern panel
	 */
	protected JPanel prepareAFIButtonPane(){
		//Create a panel that uses BoxLayout.
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane,BoxLayout.LINE_AXIS));
		
		JButton btSecure=new JButton(strSecure);
		btSecure.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
//				onFireButtonClicked();	
				AFIsecureWanted=true;
//				edInfo.setText("AFIsecure");

				mainme.setAFI(true);
			}
		});

		
		buttonPane.add(btSecure);
		buttonPane.add(new JSeparator(SwingConstants.VERTICAL));

		JButton jResetButton = new JButton();
		jResetButton.setText(strUnsecure);
//		jResetButton.setLocale(new Locale("en", "US"));
//		this.getContentPane().
//		add(jResetButton, new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0, 10, 1, new Insets(5,5, 5, 5), 0, 0));
		buttonPane.add(jResetButton);//, new Constraints(0, 2, 2, 1, 0.0, 0.0, 10, 1, new Insets(5,5, 5, 5), 0, 0));

		jResetButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
//				jResetButtonActionPerformed(evt);
				AFIunsecureWanted=true;

				mainme.setAFI(false);
			}

		});
		
		buttonPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

		return buttonPane;
	}
	
	/**
	 * 
	 * @return a panel to trigger a tag clone
	 */
	protected JPanel prepareCloneButtonPane(){
		//Create a panel that uses BoxLayout.
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane,BoxLayout.LINE_AXIS));
		
		JButton btClone=new JButton(strClone);
		btClone.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				onCloneButtonClicked();	
			}
		});

		
		buttonPane.add(btClone);
		
		buttonPane.add(Box.createHorizontalStrut(5));
		buttonPane.add(new JSeparator(SwingConstants.VERTICAL));
		buttonPane.add(Box.createHorizontalStrut(5));
		/*
		edInfo=new JTextField("welcome..");
		buttonPane.add(edInfo);
		buttonPane.add(Box.createHorizontalStrut(5));
		buttonPane.add(new JSeparator(SwingConstants.VERTICAL));
		buttonPane.add(Box.createHorizontalStrut(5));
		*/
		
		buttonPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		
		return buttonPane;
	}
	
	/**
	 * create a clone on a given empty tag from the current valid tag
	 */
	protected void onCloneButtonClicked() {
		// TODO clone one Tag to another
		String todo="TODO: call appropriate web service to print the tag\n"+
				"or: copy the content to another (blank) tag";
		System.err.println(todo);
		if(!listModel.isEmpty()){
			System.err.println(((DDMData)listModel.get(0)).Barcode());
		}
			
	}

	/**
	 * 
	 * @return a panel to change the tag's status byte
	 */
	protected JPanel prepareStatuButtonPane(){
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane,BoxLayout.LINE_AXIS));
		
		JButton bt=new JButton(strStatus);
		bt.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
	//			onCloneButtonClicked();	
			}
		});

		
		buttonPane.add(bt);
		
		buttonPane.add(Box.createHorizontalStrut(5));
		buttonPane.add(new JSeparator(SwingConstants.VERTICAL));
		buttonPane.add(Box.createHorizontalStrut(5));
		/*
		edInfo=new JTextField("welcome..");
		buttonPane.add(edInfo);
		buttonPane.add(Box.createHorizontalStrut(5));
		buttonPane.add(new JSeparator(SwingConstants.VERTICAL));
		buttonPane.add(Box.createHorizontalStrut(5));
		*/
		
		buttonPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		
		return buttonPane;
	}
	
	/**
	 * 
	 * @return a JPanel with a button called conversion 
	 */
	protected JPanel prepareConvertButtonPane(){
		//Create a panel that uses BoxLayout.
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane,BoxLayout.LINE_AXIS));
		
		JButton bt=new JButton(strConvert);
		bt.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				onConvertButtonClicked();	
			}
		});

		
		edConvert=new JTextField("barcode");
		edConvert.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent arg0) {
				// Auto-generated method stub
				
			}
			
			@Override
			public void focusGained(FocusEvent arg0) {
				edConvert.selectAll();
			}
		});
		
		
		buttonPane.add(edConvert);
		buttonPane.add(new JSeparator(SwingConstants.VERTICAL));

		
		buttonPane.add(bt);

//		buttonPane.add(Box.createHorizontalStrut(5));
//		buttonPane.add(new JSeparator(SwingConstants.VERTICAL));
//		buttonPane.add(Box.createHorizontalStrut(5));
		
		buttonPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		
		return buttonPane;
	}
	
	
	/**
	 * write a given barcode on the tag(s)
	 * one tag: easy
	 * more than one: enumerate 1/n..n/n
	 * are the tags empty?
	 */
	protected void onConvertButtonClicked() {

//		System.err.printf("%d tags\n", listModel.getSize());
//		mainme.dumptaghash();
		
		DDMTag newTag = new DDMTag();
		String newbar=readBarcodeField();
		
		newTag.setBarcode(newbar);
		
		if(listModel.isEmpty()){
			printout("kein Tag?",false);
		}else{
			if(4 < listModel.getSize())
				printout("mehr als vier Tags?",false);
			else
			if(!newTag.barcodematch(mainme.getBarcodePattern())){ // check the barcode against given pattern
				printout("Barcode gültig?",false);
			}else{
			
			
//			System.err.println(((DDMData)listModel.get(0)).Barcode());
			System.err.println("writing barcode "+newbar);
			
			printout(newbar+" written",true);
			
			mainme.convert(newbar);
			// trigger flush of inventory
			mainme.forgettags(); // force a reread
			try {
				Thread.sleep(200L);
			} catch (InterruptedException e) {
//				e.printStackTrace();
			}
			// reset input
			resetBarcodeField();
			
		}}

		edConvert.requestFocusInWindow();
		edConvert.selectAll();
		
	}



	private String readBarcodeField() {
		return edConvert.getText();
	}

	private void resetBarcodeField() {
		edConvert.setText("barcode");
	}



	class MyCellRenderer extends JCheckBox implements ListCellRenderer{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -1563174072568111708L;

		@Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			DDMTag tag = (DDMTag)value;
			setIcon(new MetalCheckBoxIcon());
//			setIcon(null);
			String stat=((1==tag.getUsage())?"":tag.getUsageAsString());
			setText((tag.toDisplayLine())+ "   " +stat);
//			setFont(new Font("SansSerif",Font.PLAIN,10));
			setBorder(new EmptyBorder(0, 0, 0, 0));
//			setBorderPainted(true);
			if(tag.isAFIsecured()){
//				setBackground(Color.GRAY);
//				setIcon(new ImageIcon(frame.getClass().getResource("/drawable/ic_locktrans.png")));
				setIcon(ic_lock);
			}
				else{
				setBackground(Color.WHITE);
				setIcon(ic_unlock);
				}
			return this;
		}
	}

/**
 * force an update of the list view
 */
	
	public void redraw(){
//		update(getGraphics());
		repaint();
//		paint(getGraphics());
	}
	
	public void redraw(boolean OldImplementation){
		if(!listModel.isEmpty()){
			try {
				DDMData tag = (DDMData)listModel.get(listModel.getSize()-1);
				listModel.setElementAt(tag, listModel.getSize()-1); // uuh - ugly (whats this for?)
			} catch (ArrayIndexOutOfBoundsException e) {
				// TODO tracing the sporadic Exception source
				System.err.println("redraw caused an ArrayIndexOutOfBoundsEx: "+e.getMessage());
				//e.printStackTrace();
			}
		}
	}
	
	/**
	 * append another element + update the counter display
	 * @param tag - the new DDM element
	 */
	public void add2list(DDMData tag){
//		setVisible(true);
		listModel.addElement(tag);
		printout("" + listModel.getSize()); // empty display line
		
	}
	

	public void removefromlist(){ // testing only
		listModel.remove(listModel.getSize()-1);
	}
	
	/**
	 * reduce the list and update the count display
	 * @param tag - the DDM element to remove
	 */
	public void removefromlist(DDMData tag){
		listModel.removeElement(tag); // never fails!
//		if(listModel.isEmpty()) setVisible(false);
		if(0==listModel.getSize())
			printout("" + listModel.getSize(),false); // empty: red field
		else
			printout("" + listModel.getSize()); // white in other cases
	}

	public void printout(String s){
		edInfo.setText(s);
		edInfo.setBackground(Color.WHITE) ;
	}

	public void printout(String s, boolean green){
		edInfo.setText(s);
		edInfo.setBackground(green ? Color.GREEN : Color.RED) ;
	}
	
	@Override
	public void valueChanged(ListSelectionEvent e) {
//		System.err.println("valueChanged: "+list.getSelectedIndex());
		if(0<=list.getSelectedIndex())
			edInfo.setText(((DDMData)(listModel.elementAt(list.getSelectedIndex()))).Barcode());
	}

	@Override
	public void keyPressed(KeyEvent e) {
		System.err.println(e.toString());
		
	}

	@Override
	public void keyReleased(KeyEvent e) {
		System.err.println(e.toString());
		if(e.getKeyCode()==KeyEvent.VK_F1){
			e.consume();
			typeLines();
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		System.err.println(e.toString());
		
	}

	private void typeLines() {
		myrobot.repeat("testing Kbd");
	}

	@Override
	public void windowClosed(WindowEvent e) {
		//  Auto-generated method stub
	}

	@Override
	public void windowClosing(WindowEvent e) {
//		System.err.println("windowClosing event");
/*
		WindowCloseWanted=true;
		JkbdRF.me.terminate();
		try {
			Thread.sleep(22);
		} catch (InterruptedException e1) {
			//  Auto-generated catch block
			e1.printStackTrace();
		}
*/
	}
	
	@Override
	public void windowDeactivated(WindowEvent arg0) {
		//  Auto-generated method stub
		
	}
	
	@Override
	public void windowActivated(WindowEvent arg0) {
		//  Auto-generated method stub
		
	}
	
	@Override
	public void windowDeiconified(WindowEvent arg0) {
		System.err.print("deiconified event\n");
		// TODO PUBLIC debug OSX: windows flashes up and disappears forever.. 
		
	}
	
	@Override
	public void windowIconified(WindowEvent arg0) {
		//  debug 
		//System.err.print("iconified event\n");

	}
	
	@Override
	public void windowOpened(WindowEvent arg0) {
		//  Auto-generated method stub
		
	}
	
}
