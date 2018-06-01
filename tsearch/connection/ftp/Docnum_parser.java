package ro.cst.tsearch.connection.ftp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class Docnum_parser {

	private String Current_line_parsed="";
	private String saveData="";
	private String Current_line = null;
	
	private String FileName="";
	
	protected Vector save_doclist=null;
	
	final int STATE_START=0;
	final int STATE_GETIMAGE=1;
	final int STATE_CONTINUE=2;
	
	/**
	 * Constructor 
	 * @param Filename file's name to parse
	 */
	public Docnum_parser(String Filename){
		this.FileName=Filename;
	}
	
	/**
	 * Constructor 
	 */
	public Docnum_parser(){
		save_doclist = new Vector();
	}
	
	
	/**
	 * Open FileName, put it in a String, call the preprocessor and parse it
	 *
	 */
	public void parse_file(){
		byte[] b=null;
		try{	
			FileInputStream in = new FileInputStream(FileName);
			DataInputStream bin = new DataInputStream(in);
			b = new byte[in.available()];
			bin.readFully(b);
			bin.close();
		}
		catch(Exception e){e.printStackTrace();}
		String txt = new String(b);
		try {
			//txt=preprocess(txt);
			//System.out.println(txt);
			parsedata(txt);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Parse FileName and fill up these vectors: list_P list_G list_L
	 *
	 */
	public  void parsedata(String in) throws Exception {
		BufferedReader bi = new BufferedReader(
			new InputStreamReader(new ByteArrayInputStream(in.getBytes())));
		int state=STATE_START;
		Current_line = bi.readLine();
		Current_line_parsed=Current_line;
		while (Current_line  != null) {
			switch(state){
			case STATE_START:
				if (getNextState("urlPages. href=.redirect.aspx\\?f=")){state=STATE_GETIMAGE;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_GETIMAGE:
				if (getNextState("[0-9]+-[0-9]{3}\\.TIF")){state=STATE_CONTINUE;SaveInfo(saveData);break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_CONTINUE:
				if (getNextState("urlPages. href=.redirect.aspx\\?f=")){state=STATE_GETIMAGE;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			}
		}
	}
	
	
	/**
	 * Returns true if expreg is found in Current_line_parsed otherwise false
	 * @return boolean
	 */
	private boolean getNextState(String expreg) throws Exception{
		Pattern pt_search = Pattern.compile(expreg);
		Matcher m= pt_search.matcher(Current_line_parsed);
		if (m.find()){
			saveData=m.group(0);
			Current_line_parsed=Current_line_parsed.substring(m.end(0),Current_line_parsed.length());
			return true;
		}
		saveData="";
		return false;
	}
		
	/*private String StringRepl(String word, String regexp,String repl){
		return word.replaceAll(regexp,repl);
	}*/
	
	private void SaveInfo(String value){
		save_doclist.add(value);
	}
	
	public void input2output(String inSTR, String outSTR) throws Exception {
		OutputStream out = new java.io.FileOutputStream(outSTR);
		Docnum_parser t = new Docnum_parser(inSTR);
		t.parse_file();
		StringBuffer sb = new StringBuffer();
		sb.append(save_doclist.toString());
		DataOutputStream dout = new DataOutputStream(out);
		dout.write(sb.toString().getBytes());
		dout.close();
	}

	
	public  static void main(String args[]) throws Exception {
		
		System.out.println("Start search");
		
		
		Docnum_parser t = new Docnum_parser();
		t.input2output("f:\\left1.txt","f:\\res.txt");
		
		System.out.println("End search");
	}
}
