package ro.cst.tsearch.connection.ftp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ro.cst.tsearch.propertyInformation.Instrument;

public class Txtparser {

	private Vector list_P=null;
	private Vector list_G=null;
	private Vector list_L=null;
	private String Current_line_parsed="";
	private String saveData="";
	private String Current_line = null;
	
	private String sType="";
	private String sNumDoc="";
	private String sDate="";
	private String sAddress="";
	private String sGrantor="";
	private String sGrantee="";
	private String sPin="";
	private String sPartialLegal="";
	private String sRemarks="";
	private String sCase_Number="";
	private String sSSNBUSID="";
	private String sLegals="";
	private String sSub="";
	private String sCertificate="";
	
	private String FileName="";
	
	
	//************************************************************************
	//START END
	//************************************************************************
	final int STATE_START=0;
	final int STATE_END=16;
	//************************************************************************
	//PROPERTY
	//************************************************************************
	final int STATE_PROPERTY=1;
	final int STATE_TYPE_DATA_P=2;
	final int STATE_NUM_DOC_DATA_P=3;
	final int STATE_DATE_DATA_P=4;
	final int STATE_GRANTOR_P=5;
	final int STATE_GRANTEE_P=6;
	final int STATE_GRANTOR_DATA_P=7;
	final int STATE_GRANTEE_DATA_P=8;
	final int STATE_PIN_P=9;
	final int STATE_PARTIAL_LEGAL_P=10;
	final int STATE_REMARKS_P=11;
	final int STATE_ADDRESS_P=12;
	final int STATE_PARTIAL_LEGAL_DATA_P=13;
	final int STATE_PIN_DATA_P=14;
	final int STATE_REMARKS_DATA_P=15;
	final int STATE_ADDRESS_DATA_P=18;
	final int STATE_CERTIFICATE_P=250;
	final int STATE_CERTIFICATE_DATA_P=251;
	//************************************************************************
	//GENERAL
	//************************************************************************
	final int STATE_GENERAL=17;
	final int STATE_TYPE_DATA_G=19;
	final int STATE_NUM_DOC_DATA_G=20;
	final int STATE_DATE_DATA_G=21;
	final int STATE_GRANTOR_G=22;
	final int STATE_GRANTEE_G=23;
	final int STATE_GRANTOR_DATA_G=24;
	final int STATE_GRANTEE_DATA_G=25;
	final int STATE_SSNBUSID_G=26;
	final int STATE_REMARKS_G=27;
	final int STATE_ADDRESS_G=28;
	final int STATE_SSNBUSID_DATA_G=29;
	final int STATE_ADDRESS_DATA_G=31;
	final int STATE_CASE_NUMBER_DATA_G=32;
	final int STATE_REMARKS_DATA_G=33;
	
	
	//************************************************************************
	//LOCATES
	//************************************************************************
	
	final int STATE_LOCATES=39;
	final int STATE_TYPE_DATA_L=40;
	final int STATE_NUM_DOC_DATA_L=41;
	final int STATE_DATE_DATA_L=42;
	final int STATE_LEGALS_L=43;
	final int STATE_LEGALS_DATA_L=44;
	final int STATE_SUB_L=45;
	final int STATE_SUB_DATA_L=46;
	final int STATE_PIN_L=47;
	final int STATE_PIN_DATA_L=48;
	final int STATE_PARTIAL_LEGAL_L=49;
	final int STATE_PARTIAL_LEGAL_DATA_L=50;		
	final int STATE_GRANTOR_L=51;
	final int STATE_GRANTEE_L=52;
	final int STATE_GRANTOR_DATA_L=53;
	final int STATE_GRANTEE_DATA_L=54;
	final int STATE_REMARKS_L=55;
	final int STATE_REMARKS_DATA_L=56;
	
	
	/**
	 * Constructor 
	 * @param Filename file's name to parse
	 */
	public Txtparser(String Filename){
		this.FileName=Filename;
	}
	
	/**
	 * Constructor 
	 */
	public Txtparser(){
	}
	
	/** 
	 * Returns the list of property object
	 * @return Vector  
	 */
	public Vector getPropertyList(){
		return list_P;
	}
	
	/** 
	 * Returns the list of General object
	 * @return Vector  
	 */
	public Vector getGeneralList(){
		return list_G;
	}
	
	/** 
	 * Returns the list of Locates object
	 * @return Vector  
	 */
	public Vector getLocatesList(){
		return list_L;
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
			txt=preprocess(txt);
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
	private  void parsedata(String in) throws Exception {
		list_P= new Vector();
		list_G= new Vector();
		list_L= new Vector();
		BufferedReader bi = new BufferedReader(
			new InputStreamReader(new ByteArrayInputStream(in.getBytes())));
		int state=STATE_START;
		Current_line = bi.readLine();
		Current_line_parsed=Current_line;
		while (Current_line  != null) {
			switch(state){
			//************************************************************************
			//START END
			//************************************************************************
			case STATE_START:
				if (getNextState("----Property search----")){state=STATE_PROPERTY;break;}
				if (getNextState("----General search----")){state=STATE_GENERAL;break;}
				if (getNextState("----Locates search----")){state=STATE_LOCATES;break;}
				if (getNextState("----End search----")){state=STATE_END;SaveInfo_property();break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_END:
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			//************************************************************************
			//PROPERTY
			//************************************************************************
			case STATE_PROPERTY:
				if (getNextState("----General search----")){state=STATE_GENERAL;break;}
				if (getNextState("----Locates search----")){state=STATE_LOCATES;break;}
				if (getNextState("----End search----")){state=STATE_END;SaveInfo_property();break;}
				if (getNextState("\\s[A-Z]+\\s")){
					sType+=StringRepl(saveData,"\\s","");state=STATE_TYPE_DATA_P;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_TYPE_DATA_P:
				if (getNextState("\\s([0-9]|[A-Z])+([0-9]|[A-Z])\\s")){state=STATE_NUM_DOC_DATA_P;sNumDoc+=StringRepl(saveData,"\\s","");break;}
				if (getNextState("\\d{2}/\\d{2}/\\d{4}")){state=STATE_DATE_DATA_P;sDate+=saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_NUM_DOC_DATA_P:
				if (getNextState("Grantor      :")){state=STATE_GRANTOR_P;break;}
				if (getNextStateSpecialCase2("\\s{2}/\\s{2}/\\s{2}","\\d{2}/\\d{2}/\\d{4}")){state=STATE_DATE_DATA_P;sDate+=saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_DATE_DATA_P:
				if (getNextState("Grantor      :")){state=STATE_GRANTOR_P;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_GRANTOR_P:
				if (getNextState("Grantee      :")){state=STATE_GRANTEE_P;break;}
				if (getNextState("\\S.*\\S|\\S")){state=STATE_GRANTOR_DATA_P;sGrantor+=saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_GRANTEE_P:
				if (getNextState("PIN          :")){state=STATE_PIN_P;break;}
				if (getNextState("\\S.*\\S|\\S")){state=STATE_GRANTEE_DATA_P;sGrantee+=saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;
				break;
			case STATE_PIN_P:
				if (getNextState("Partial Legal:")){state=STATE_PARTIAL_LEGAL_P;break;}//\\d*-\\d*-\\d*-\\d*-\\d*
				if (getNextState("Certificate #:")){state=STATE_CERTIFICATE_P;break;}
				if (getNextState("\\S.*\\S|\\S")){state=STATE_PIN_DATA_P;sPin+=StringRepl(saveData,"-","");break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;	break;
			case STATE_PARTIAL_LEGAL_P:
				if (getNextState("Remarks      :")){state=STATE_REMARKS_P;break;}//([A-Z]+|[0-9]+).*([A-Z]+|[0-9]+)\\.{0,1}
				if (getNextState("\\S.*\\S|\\S")){state=STATE_PARTIAL_LEGAL_DATA_P;sPartialLegal+=saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_REMARKS_P:
				if (getNextState("----End search----")){state=STATE_END;SaveInfo_property();break;}
				if (getNextState("----General search----")){state=STATE_GENERAL;SaveInfo_property();break;}
				if (getNextState("----Locates search----")){state=STATE_LOCATES;SaveInfo_property();break;}
				if (getNextState("Address      :")){state=STATE_ADDRESS_P;break;}
				//if (getNextState("\\s{2}MTGF\\s{2}|\\s{2}AG\\s{2}|\\s{2}CL\\s{2}|\\s{2}CTF\\s{2}|\\s{2}TCTF\\s{2}|\\s{2}LPN\\s{2}|\\s{2}NTC\\s{2}|\\s{2}BV\\s{2}|\\s{2}NTC\\s{2}|\\s{2}MTG\\s{2}|\\s{2}S\\s{2}|\\s{2}D\\s{2}|\\s{2}A\\s{2}|\\s{2}W\\s{2}|\\s{4}MTGE\\s{2}|\\s{4}MLC\\s{2}|\\s{4}AS\\s{2}|\\s{4}PA\\s{2}|\\s{4}WJ\\s{2}|\\s{4}R\\s{2}|\\s{4}MTGC\\s{2}|\\s{4}BA\\s{2}|\\s{4}Q\\s{2}")){
				if ((Current_line.indexOf("Remarks      :")==-1)&&getNextState("\\s[A-Z]+\\s")){
					state=STATE_TYPE_DATA_P;SaveInfo_property();
					sType+=StringRepl(saveData,"\\s","");break;}
				if (getNextState("\\S.*\\S|\\S")){
					state=STATE_REMARKS_DATA_P;sRemarks+=saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_ADDRESS_P:
				if (getNextState("----End search----")){state=STATE_END;SaveInfo_property();break;}
				if (getNextState("----General search----")){state=STATE_GENERAL;SaveInfo_property();break;}
				if (getNextState("----Locates search----")){state=STATE_LOCATES;SaveInfo_property();break;}
				//if (getNextState("\\s{2}MTGF\\s{2}|\\s{2}AG\\s{2}|\\s{2}CL\\s{2}|\\s{2}CTF\\s{2}|\\s{2}TCTF\\s{2}|\\s{2}LPN\\s{2}|\\s{2}NTC\\s{2}|\\s{2}BV\\s{2}|\\s{2}NTC\\s{2}|\\s{2}MTG\\s{2}|\\s{2}S\\s{2}|\\s{2}D\\s{2}|\\s{2}A\\s{2}|\\s{2}W\\s{2}|\\s{4}MTGE\\s{2}|\\s{4}MLC\\s{2}|\\s{4}AS\\s{2}|\\s{4}PA\\s{2}|\\s{4}WJ\\s{2}|\\s{4}R\\s{2}|\\s{4}MTGC\\s{2}|\\s{4}BA\\s{2}|\\s{4}Q\\s{2}")){
				if ((Current_line.indexOf("Address      :")==-1)&&getNextState("\\s[A-Z]+\\s")){
					state=STATE_TYPE_DATA_P;SaveInfo_property();sType+=StringRepl(saveData,"\\s","");break;}
				if (getNextState("\\S.*\\S|\\S")){
					state=STATE_ADDRESS_DATA_P;sAddress+=saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_GRANTOR_DATA_P:
				if (getNextState("Grantee      :")){state=STATE_GRANTEE_P;break;}
				if (getNextState("\\S.*\\S|\\S")){state=STATE_GRANTOR_DATA_P;sGrantor+=" ## " + saveData;break;}	
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_GRANTEE_DATA_P:
				if (getNextState("PIN          :")){state=STATE_PIN_P;break;}
				if (getNextState("\\S.*\\S|\\S")){
					state=STATE_GRANTEE_DATA_P;sGrantee+=" ## " + saveData;break;}	
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_PIN_DATA_P:
				if (getNextState("Partial Legal:")){state=STATE_PARTIAL_LEGAL_P;break;}
				if (getNextState("Certificate #:")){state=STATE_CERTIFICATE_P;break;}
				if (getNextState("\\S.*\\S|\\S")){state=STATE_PIN_DATA_P;sPin+=" ## " + StringRepl(saveData,"-","");break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_CERTIFICATE_P:
				if (getNextState("Partial Legal:")){state=STATE_PARTIAL_LEGAL_P;break;}
				if (getNextState("\\S.*\\S|\\S")){state=STATE_CERTIFICATE_DATA_P;sCertificate+=saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_CERTIFICATE_DATA_P:
				if (getNextState("Partial Legal:")){state=STATE_PARTIAL_LEGAL_P;break;}
				if (getNextState("\\S.*\\S|\\S")){state=STATE_CERTIFICATE_DATA_P;sCertificate+=" ## " +saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_PARTIAL_LEGAL_DATA_P:
				if (getNextState("Remarks      :")){state=STATE_REMARKS_P;break;}
				if (getNextState("\\S.*\\S|\\S")){
					state=STATE_PARTIAL_LEGAL_DATA_P;sPartialLegal+=" ## " + saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_REMARKS_DATA_P:
				if (getNextState("----End search----")){state=STATE_END;SaveInfo_property();break;}
				if (getNextState("----General search----")){state=STATE_GENERAL;SaveInfo_property();break;}
				if (getNextState("----Locates search----")){state=STATE_LOCATES;SaveInfo_property();break;}
				if (getNextState("Address      :")){state=STATE_ADDRESS_P;break;}
				if ((Current_line.indexOf("Remarks      :")==-1)&&isExprInLine(Current_line,".*/(\\s{2}|\\S{2})/.*/(\\s{2}|\\S{2})/.*")&&getNextState("\\s[A-Z]+\\s")){
					state=STATE_TYPE_DATA_P;SaveInfo_property();sType+=StringRepl(saveData,"\\s","");break;}
				if (getNextState("\\S.*\\S|\\S")){//([A-Z]+|[0-9]+).*([A-Z]+|[0-9]+)\\.{0,1}
					state=STATE_REMARKS_DATA_P;sRemarks+=" ## " + saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_ADDRESS_DATA_P:
				if (getNextState("----General search----")){
					state=STATE_GENERAL;SaveInfo_property();break;}
				if (getNextState("----End search----")){
					state=STATE_END;SaveInfo_property();break;}
				if (getNextState("----Locates search----")){
					state=STATE_LOCATES;SaveInfo_property();break;}
				if ((Current_line.indexOf("Address      :")==-1)&&isExprInLine(Current_line,".*/(\\s{2}|\\S{2})/.*/(\\s{2}|\\S{2})/.*")&&getNextState("\\s[A-Z]+\\s")){
					state=STATE_TYPE_DATA_P;SaveInfo_property();sType+=StringRepl(saveData,"\\s","");break;}
				if (getNextState("\\S.*\\S|\\S")){
					state=STATE_ADDRESS_DATA_P;sAddress+=" ## " +saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
						
				//************************************************************************
				//GENERAL
				//************************************************************************
			case STATE_GENERAL:
				if (getNextState("----Property search----")){state=STATE_PROPERTY;break;}
				if (getNextState("----Locates search----")){state=STATE_LOCATES;break;}
				if (getNextState("----End search----")){state=STATE_END;SaveInfo_property();break;}
				if (getNextState("[A-Z]+\\s")){sType+=StringRepl(saveData,"\\s","");state=STATE_TYPE_DATA_G;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_TYPE_DATA_G:
				if (getNextStateSpecialCase1("\\s([0-9]|[A-Z])+([0-9]|[A-Z])\\s","\\d{2}/\\d{2}/\\d{4}"))
					{state=STATE_NUM_DOC_DATA_G;sNumDoc+=StringRepl(saveData,"\\s","");break;}
				if (getNextState("\\d{2}/\\d{2}/\\d{4}")){state=STATE_DATE_DATA_G;sDate+=saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_NUM_DOC_DATA_G:
				if (getNextState("\\d{2}/\\d{2}/\\d{4}")){state=STATE_DATE_DATA_G;sDate+=saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_DATE_DATA_G:
				if (isExprInLine(Current_line,".*/(\\s{2}|\\S{2})/.*")&&getNextState("[0-9]([0-9]+\\s{0,4}|[a-zA-Z]+\\s{0,4})+([0-9]|[a-zA-Z])")){sCase_Number+=saveData;state=STATE_CASE_NUMBER_DATA_G;break;}
				if (getNextState("GRANTOR: ")){state=STATE_GRANTOR_G;break;}
				if (getNextState("GRANTEE: ")){state=STATE_GRANTEE_G;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_CASE_NUMBER_DATA_G:
				if (getNextState("GRANTOR: ")){state=STATE_GRANTOR_G;break;}
				if (getNextState("GRANTEE: ")){state=STATE_GRANTEE_G;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_GRANTOR_G:
				if (getNextState("\\S.*\\S|\\S")){state=STATE_GRANTOR_DATA_G;if (sGrantor!="")sGrantor+=" ## " + saveData;else sGrantor+=saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_GRANTEE_G:
				if (getNextState("\\S.*\\S|\\S")){state=STATE_GRANTEE_DATA_G;if (sGrantee!="")sGrantee+=" ## " + saveData;else sGrantee+=saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;
				break;
			case STATE_SSNBUSID_G:
				if (getNextState("\\S.*\\S|\\S")){state=STATE_SSNBUSID_DATA_G;sSSNBUSID+=saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;
				break;
			case STATE_ADDRESS_G:
				if (getNextState("\\S.*\\S|\\S")){
					state=STATE_ADDRESS_DATA_G;if (sAddress!="")sAddress+=" ## " + saveData;else sAddress+=saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_GRANTEE_DATA_G:
				if (getNextState("GRANTEE: ")){state=STATE_GRANTEE_G;break;}
				if (getNextState("REMARKS: ")){state=STATE_REMARKS_G;break;}
				if (getNextState("----Remarks----")){state=STATE_REMARKS_G;break;}
				if (getNextState("ADDRESS: ")){state=STATE_ADDRESS_G;break;}
				if (getNextState("SSN/BUS_ID: ")){state=STATE_SSNBUSID_G;break;}
				if (getNextState("GRANTOR: ")){state=STATE_GRANTOR_G;break;}
				if (getNextState("----Locates search----")){state=STATE_LOCATES;SaveInfo_general();break;}
				if (getNextState("----End search----")){state=STATE_END;SaveInfo_general();break;}
				if (getNextState("----Property search----")){state=STATE_PROPERTY;SaveInfo_general();break;}
				if (getNextState("[A-Z]+\\s")){state=STATE_TYPE_DATA_G;SaveInfo_general();sType+=StringRepl(saveData,"\\s","");break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_GRANTOR_DATA_G:
				if (getNextState("GRANTEE: ")){state=STATE_GRANTEE_G;break;}
				if (getNextState("REMARKS: ")){state=STATE_REMARKS_G;break;}				
				if (getNextState("----Remarks----")){state=STATE_REMARKS_G;break;}
				if (getNextState("ADDRESS: ")){state=STATE_ADDRESS_G;break;}
				if (getNextState("SSN/BUS_ID: ")){state=STATE_SSNBUSID_G;break;}
				if (getNextState("GRANTOR: ")){state=STATE_GRANTOR_G;break;}
				if (getNextState("----Locates search----")){state=STATE_LOCATES;SaveInfo_general();break;}
				if (getNextState("----End search----")){state=STATE_END;SaveInfo_general();break;}
				if (getNextState("----Property search----")){state=STATE_PROPERTY;SaveInfo_general();break;}
				if (getNextState("[A-Z]+\\s")){state=STATE_TYPE_DATA_G;SaveInfo_general();sType+=StringRepl(saveData,"\\s","");break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_SSNBUSID_DATA_G:
				if (getNextState("ADDRESS: ")){state=STATE_ADDRESS_G;break;}
				if (getNextState("----Remarks----")){state=STATE_REMARKS_G;break;}
				if (getNextState("----Locates search----")){state=STATE_LOCATES;SaveInfo_general();break;}
				if (getNextState("----End search----")){state=STATE_END;SaveInfo_general();break;}
				if (getNextState("----Property search----")){state=STATE_PROPERTY;SaveInfo_general();break;}
				if (getNextState("GRANTOR: ")){state=STATE_GRANTOR_G;break;}
				if (getNextState("GRANTEE: ")){state=STATE_GRANTEE_G;break;}				
				if (getNextState("REMARKS: ")){state=STATE_REMARKS_G;break;}
				if (getNextState("[A-Z]+\\s")){state=STATE_TYPE_DATA_G;SaveInfo_general();sType+=StringRepl(saveData,"\\s","");break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_ADDRESS_DATA_G:
				if (getNextState("ADDRESS: ")){state=STATE_ADDRESS_G;break;}
				if (getNextState("----Locates search----")){state=STATE_LOCATES;SaveInfo_general();break;}
				if (getNextState("----End search----")){state=STATE_END;SaveInfo_general();break;}
				if (getNextState("----Property search----")){state=STATE_PROPERTY;SaveInfo_general();break;}
				if (getNextState("GRANTOR: ")){state=STATE_GRANTOR_G;break;}
				if (getNextState("GRANTEE: ")){state=STATE_GRANTEE_G;break;}
				if (getNextState("SSN/BUS_ID: ")){state=STATE_SSNBUSID_G;break;}
				if (getNextState("REMARKS: ")){state=STATE_REMARKS_G;break;}
				if (getNextState("----Remarks----")){state=STATE_REMARKS_G;break;}
				if (getNextState("[A-Z]+\\s")){state=STATE_TYPE_DATA_G;SaveInfo_general();sType+=StringRepl(saveData,"\\s","");break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;				
			case STATE_REMARKS_G:
				if (getNextState("----Remarks----")){state=STATE_REMARKS_G;break;}
				if (getNextState("GRANTOR: ")){state=STATE_GRANTOR_G;break;}
				if (getNextState("GRANTEE: ")){state=STATE_GRANTEE_G;break;}
				if (getNextState("SSN/BUS_ID: ")){state=STATE_SSNBUSID_G;break;}
				if (getNextState("ADDRESS: ")){state=STATE_ADDRESS_G;break;}
				if (getNextState("----Locates search----")){state=STATE_LOCATES;SaveInfo_general();break;}
				if (getNextState("----End search----")){state=STATE_END;SaveInfo_general();break;}
				if (getNextState("----Property search----")){state=STATE_PROPERTY;SaveInfo_general();break;}
				if (getNextState("\\S.*\\S|\\S")){state=STATE_REMARKS_DATA_G;if (sRemarks!="")sRemarks+=" ## " + saveData;else sRemarks+=saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_REMARKS_DATA_G:
				if (getNextState("GRANTOR: ")){state=STATE_GRANTOR_G;break;}
				if (getNextState("GRANTEE: ")){state=STATE_GRANTEE_G;break;}
				if (getNextState("----Remarks----")){state=STATE_REMARKS_G;break;}
				if (getNextState("SSN/BUS_ID: ")){state=STATE_SSNBUSID_G;break;}
				if (getNextState("ADDRESS: ")){state=STATE_ADDRESS_G;break;}				
				if (getNextState("----Locates search----")){state=STATE_LOCATES;SaveInfo_general();break;}
				if (getNextState("----End search----")){state=STATE_END;SaveInfo_general();break;}
				if (getNextState("----Property search----")){state=STATE_PROPERTY;SaveInfo_general();break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
				//************************************************************************
				//LOCATES
				//************************************************************************
			case STATE_LOCATES:
				if (getNextState("----Property search----")){state=STATE_PROPERTY;break;}
				if (getNextState("----General search----")){state=STATE_GENERAL;break;}
				if (getNextState("----End search----")){state=STATE_END;SaveInfo_property();break;}
				if (getNextState("\\s[A-Z]+\\s")){sType+=StringRepl(saveData,"\\s","");state=STATE_TYPE_DATA_L;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_TYPE_DATA_L:
				if (getNextState("\\s([0-9]|[A-Z])+([0-9]|[A-Z])\\s"))
					{state=STATE_NUM_DOC_DATA_L;sNumDoc+=StringRepl(saveData,"\\s","");break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_NUM_DOC_DATA_L:
				if (getNextState("\\d{2}/\\d{2}/\\d{4}")){state=STATE_DATE_DATA_L;sDate+=saveData;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_DATE_DATA_L:
				if (getNextState("Legals       :")){state=STATE_LEGALS_L;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_LEGALS_L:
				if (getNextState("Sub          :")){state=STATE_SUB_L;break;}
				if (getNextState("\\S.*\\S|\\S")){if (sLegals!="")sLegals+=" ## " + saveData;else sLegals+=saveData;state=STATE_LEGALS_DATA_L;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_LEGALS_DATA_L:
				if (getNextState("Sub          :")){state=STATE_SUB_L;break;}
				if (getNextState("PIN          :")){state=STATE_PIN_L;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_SUB_L:
				if (getNextState("PIN          :")){state=STATE_PIN_L;break;}
				if (getNextState("\\S.*\\S|\\S")){if (sSub!="")sSub+=" ## " + saveData;else sSub+=saveData;state=STATE_SUB_DATA_L;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_SUB_DATA_L:
				if (getNextState("PIN          :")){state=STATE_PIN_L;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_PIN_L:
				if (getNextState("Partial Legal:")){state=STATE_PARTIAL_LEGAL_L;break;}
				if (getNextState("\\S.*\\S|\\S")){if (sPin!="")sPin+=" ## " + StringRepl(saveData,"-","");else sPin+=StringRepl(saveData,"-","");state=STATE_PIN_DATA_L;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_PIN_DATA_L:
				if (getNextState("Partial Legal:")){state=STATE_PARTIAL_LEGAL_L;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_PARTIAL_LEGAL_L:
				if (getNextState("Grantor      :")){state=STATE_GRANTOR_L;break;}
				if (getNextState("Legals       :")){state=STATE_LEGALS_L;break;}
				if (getNextState("\\S.*\\S|\\S")){if (sPartialLegal!="")sPartialLegal+=" ## " + saveData;else sPartialLegal+=saveData;state=STATE_PARTIAL_LEGAL_DATA_L;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_PARTIAL_LEGAL_DATA_L:
				if (getNextState("Grantor      :")){state=STATE_GRANTOR_L;break;}
				if (getNextState("Legals       :")){state=STATE_LEGALS_L;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_GRANTOR_L:
				if (getNextState("Grantee      : ")){state=STATE_GRANTEE_L;break;}
				if (getNextState("\\S.*\\S|\\S")){sGrantor+=saveData;state=STATE_GRANTOR_DATA_L;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_GRANTOR_DATA_L:
				if (getNextState("Grantee      : ")){state=STATE_GRANTEE_L;break;}
				if (getNextState("\\S.*\\S|\\S")){sGrantor+=" ## " + saveData;state=STATE_GRANTOR_DATA_L;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_GRANTEE_L:
				if (getNextState("Remarks      :")){state=STATE_REMARKS_L;break;}
				if (getNextState("\\S.*\\S|\\S")){sGrantee+=saveData;state=STATE_GRANTEE_DATA_L;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_GRANTEE_DATA_L:
				if (getNextState("Remarks      :")){state=STATE_REMARKS_L;break;}
				if (getNextState("\\S.*\\S|\\S")){sGrantee+=" ## " + saveData;state=STATE_GRANTEE_DATA_L;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_REMARKS_L:
				if (getNextState("----End search----")){state=STATE_END;SaveInfo_locates();break;}
				if (getNextState("----General search----")){state=STATE_GENERAL;SaveInfo_locates();break;}
				if (getNextState("----Property search----")){state=STATE_PROPERTY;SaveInfo_locates();break;}
				if ((Current_line.indexOf("Remarks      :")==-1)&&isExprInLine(Current_line,".*/(\\s{2}|\\S{2})/.*/(\\s{2}|\\S{2})/.*")&&getNextState("\\s[A-Z]+\\s")){
					state=STATE_TYPE_DATA_L;SaveInfo_locates();sType+=StringRepl(saveData,"\\s","");break;}		
				if (getNextState("\\S.*\\S|\\S")){sRemarks+=saveData;state=STATE_REMARKS_DATA_L;break;}
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			case STATE_REMARKS_DATA_L:
				if (getNextState("----End search----")){state=STATE_END;SaveInfo_locates();break;}
				if (getNextState("----General search----")){state=STATE_GENERAL;SaveInfo_locates();break;}
				if (getNextState("----Property search----")){state=STATE_PROPERTY;SaveInfo_locates();break;}
				if ((Current_line.indexOf("Remarks      :")==-1)&&isExprInLine(Current_line,".*/(\\s{2}|\\S{2})/.*/(\\s{2}|\\S{2})/.*")&&getNextState("\\s[A-Z]+\\s")){
					state=STATE_TYPE_DATA_L;SaveInfo_locates();sType+=StringRepl(saveData,"\\s","");break;}	
				Current_line = bi.readLine();Current_line_parsed=Current_line;break;
			}
		}
	}
	/**
	 * Returns true if a doctype is found in Current_line_parsed otherwise false
	 * @return boolean
	 */
	private boolean getdocState(){
		Pattern pt_search = Pattern.compile("");
		Matcher m= pt_search.matcher(Current_line_parsed);
		if (m.find()){
			saveData=m.group(0);
			Current_line_parsed=Current_line_parsed.substring(m.end(0),Current_line_parsed.length());
			return true;
		}
		saveData="";
		return false;
	}
	/**
	 * Returns true if expr is in line, otherwise false
	 * @return boolean
	 */
	private boolean isExprInLine(String line, String expr){
		Pattern pt_search = Pattern.compile(expr);
		Matcher m= pt_search.matcher(line);
		if (m.find()){
			return true;
		}
		return false;
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
	
		
	
	//CH                    07/07/1999    99CH  0009857      
	//CH       00456745     07/07/1999    99CH  0009857
	//test if expreg1 and expreg2 are found into Current_line_parsed and expreg1 is following by expreg2
	//return true if it is the case
	//else false
	private boolean getNextStateSpecialCase1(String expreg1,String expreg2) throws Exception{
		Pattern pt_search1 = Pattern.compile(expreg1);
		Matcher m1= pt_search1.matcher(Current_line_parsed);
		Pattern pt_search2 = Pattern.compile(expreg2);
		Matcher m2= pt_search2.matcher(Current_line_parsed);
		if (m1.find()&&m2.find()&&(m1.end(0)<m2.end(0))){
			saveData=m1.group(0);
			Current_line_parsed=Current_line_parsed.substring(m1.end(0),Current_line_parsed.length());
			return true;
		}
		saveData="";
		return false;
	}
	//10/29/1997    10/29/1997
	//  /  /		10/29/1997
	//test if expreg1 is not found and expreg2 is fould
	//return true if it is the case
	//else false
	private boolean getNextStateSpecialCase2(String expreg1,String expreg2) throws Exception{
		Pattern pt_search1 = Pattern.compile(expreg1);
		Matcher m1= pt_search1.matcher(Current_line_parsed);
		Pattern pt_search2 = Pattern.compile(expreg2);
		Matcher m2= pt_search2.matcher(Current_line_parsed);
		if (!m1.find()&&m2.find()){
			saveData=m2.group(0);
			Current_line_parsed="";
			return true;
		}
		saveData="";
		return false;
	}
	
	public Map getListInHmap(){
		Map m = new HashMap();
		m.put("PROPERTY",list_P);
		m.put("GENERAL",list_G);
		m.put("LOCATES",list_L);
		return m;
	}
	
	private String StringRepl(String word, String regexp,String repl){
		return word.replaceAll(regexp,repl);
	}
	
	private void SaveInfo_property(){
		Data_Property current=new Data_Property();
		current.setGrantor(sGrantor);
		current.setGrantee(sGrantee);
		current.setPIN(sPin);
		current.setPartial_Legal(sPartialLegal);
		current.setRemarks(sRemarks);
		current.setDate(sDate);
		current.setNumDoc(sNumDoc);
		current.setType(sType);
		current.setAddress(sAddress);
		current.setCertificate(sCertificate);
		list_P.add(current);
		InitializeData();
	}
	private void SaveInfo_general(){
		Data_General current=new Data_General();
		current.setGrantor(sGrantor);
		current.setGrantee(sGrantee);
		current.setSSNBUSID(sSSNBUSID);
		current.setRemarks(sRemarks);
		current.setDate(sDate);
		current.setNumDoc(sNumDoc);
		current.setType(sType);
		current.setAddress(sAddress);
		current.setCase_Number(sCase_Number);
		list_G.add(current);
		InitializeData();
	}
	private void SaveInfo_locates(){
		Data_Locates current=new Data_Locates();
		current.setGrantor(sGrantor);
		current.setGrantee(sGrantee);
		current.setLegals(sLegals);
		current.setRemarks(sRemarks);
		current.setDate(sDate);
		current.setNumDoc(sNumDoc);
		current.setType(sType);
		current.setSub(sSub);
		current.setPIN(sPin);
		current.setPartial_Legal(sPartialLegal);
		list_L.add(current);
		InitializeData();	
	}
	
	private void InitializeData(){
		sGrantor="";
		sGrantee="";
		sPin="";
		sPartialLegal="";
		sRemarks="";
		sType="";
		sNumDoc="";
		sDate="";
		sAddress="";
		sCase_Number="";
		sSSNBUSID="";
		sLegals="";
		sSub="";
		sCertificate="";
	}
//	/remove unwanted strings
	private String preprocess(String in) {
		String rez;
		if (in == null)
			return null;
		//remove page number		
		rez = in.replaceAll("Page \\d+", "");
		//Separate different parts
		rez= rez.replaceFirst("Landata, Inc. of Illinois  PROPERTY INDEX SEARCH (.)*","----Property search----");
		rez= rez.replaceFirst("Landata, Inc. of Illinois   GENERAL INDEX SEARCH (.)*","----General search----");
		rez= rez.replaceFirst("Landata, Inc. of Illinois     LOCATES SEARCH REPORT (.)*","----Locates search----");
		rez= rez.replaceFirst("Landata Information Services  TAX SEARCH REPORT (.)*","----End search----");
		rez = rez.replaceAll("\\*\\*\\* Trust Number Searched \\*\\*\\* (.)*", "");
		//remove special caracters
		rez= rez.replaceAll("\\(|\\)|\\[|\\]","");
		rez= rez.replaceAll("(Tr\\.#)(.)*","");
		rez=rez.replaceAll("(\\$)","");
		//rez=rez.replaceAll("(\\$[0-9]*(.)*)","");
		//rez= rez.replaceAll("\\)","");
		//remove the headers
		//rez= rez.replaceAll("(Landata, Inc. of Illinois  PROPERTY INDEX SEARCH (.)*)|(Landata, Inc. of Illinois   GENERAL INDEX SEARCH (.)*)","");
		rez= rez.replaceAll("(Landata, Inc. of Illinois   GENERAL INDEX SEARCH (.)*)","");
		rez = rez.replaceAll("(---------------------(.)*)|(\\*\\*\\* SUBDIVISION GENERAL \\*\\*\\*)|(\\*\\*\\* BLOCK/LOT GENERAL \\*\\*\\*)|(\\*\\*\\* REQUESTED TRACT \\*\\*\\*)", "");
		int i, j;
		String hdr="";
		//i = rez.indexOf("----Property search----")+"----Property search----".length();
		i = rez.indexOf("----Property search----");
		if (i!=-1){
			i=i+"----Property search----".length();
			j = rez.indexOf("Doc Type     Document #   Doc Date    Rec Date            Amount", i)+ "Doc Type     Document #   Doc Date    Rec Date            Amount".length();

			if (i<j){
				hdr = rez.substring(i, j);
				rez = rez.replaceAll(hdr, "");
			}
		}
		int i2,j2;
		String hdr2="";
		while(true){
			i2=rez.indexOf("Landata, Inc. of Illinois  PROPERTY INDEX SEARCH");
			if (i2==-1)
				break;
			else{
				j2=rez.indexOf("continued")+"continued".length();
		
				if (j2>8){
					if (j2-i2<500){
						//if (j2<i2) break;
						hdr2 = rez.substring(i2,j2);
					
						//rez=rez.replaceAll("(\\.)","~");
						rez = rez.replaceFirst(hdr2,"");
						//rez=rez.replaceAll("(\\~)",".");
						
					}
					else
					{
							rez = rez.replaceFirst("Landata, Inc. of Illinois  PROPERTY INDEX SEARCH(.|\\n)*","");
					}
				}
				else
				{
					break;
				}
			}
		}
		rez = rez.replaceAll("Search Completed:(.)*", "");
		rez= rez.replaceAll("Doc Type  (.)*","");
		rez= rez.replaceAll("\\d\\sPlat:(.)*","");
		rez = rez.replaceAll("(PROPERTY INDEX SEARCH FOR(.)*)|(LOCATES SEARCH FOR ORDER(.)*)|(GENERAL INDEX SEARCH FOR(.*))", "");
		rez = rez.replaceAll("\\*\\*\\* Name Searched \\*\\*\\* (.)*", "");
		i = rez.indexOf("----General search----")+"----General search----".length();
		j = rez.indexOf("TYPE   NUMBER         DATE          NUMBER                 AMOUNT",i) + "TYPE   NUMBER         DATE          NUMBER                 AMOUNT".length();
		if (i!=-1){
			hdr = rez.substring(i, j);
		rez = rez.replaceAll(hdr, "");
		}
		rez = rez.replaceAll("TYPE   NUMBER         DATE          NUMBER                 AMOUNT", "");
		rez = rez.replaceAll("DOC    DOCUMENT       RECORDING     CASE","");
		while(true){
			i2=rez.indexOf("Landata, Inc. of Illinois     LOCATES SEARCH REPORT");
			if (i2==-1)
				{break;}// 
			else{
				j2=rez.indexOf("continued",i2)+"continued".length();
				if (j2>"continued".length()){
					if (j2-i2<500){
						//if (j2<i2) break;
						hdr2 = rez.substring(i2,j2);
						rez = rez.replaceFirst(hdr2,"");
					}
					else
					{
						rez = rez.replaceFirst("Landata, Inc. of Illinois     LOCATES SEARCH REPORT(.|\\n)*","");
					}
				}
				else
				{
					break;
				}
			}
		}
		i = rez.indexOf("----Locates search----");
		if(i!=-1){
			i=i+"----Locates search----".length();
			j = rez.indexOf("Company: ",i);
			if (i<j){
				hdr = rez.substring(i, j);
				rez = rez.replaceAll(hdr, "");
			}
		}
		
		rez = rez.replaceAll("Order Number:(.|\\n)*", "");
		rez = rez.replaceAll("____________(.)*", "");
		rez = rez.replaceAll("Search completed -(.)*", "");
		rez = rez.replaceAll("GENERAL INDEX SEARCH FOR(.*)", "");		
		rez = rez.replaceAll("Company:(.*)", "");
		rez = rez.replaceAll("CONTINUED","");
		rez = rez.replaceAll("(Landata, Inc. of Illinois  PROPERTY INDEX SEARCH(.)*)|(Landata, Inc. of Illinois     LOCATES SEARCH REPORT(.)*)","");
		rez = rez.replaceAll("REMARKS:","\r\nREMARKS:");
		rez = rez.replaceAll("\\sA:","\r\n----Remarks----A: ");
		rez = rez.replaceAll("\\sAtty:","\r\n----Remarks----Atty:");
		rez = rez.replaceAll("\\sChap:","\r\n----Remarks----Chap:");
		rez = rez.replaceAll("\\sL:","\r\n----Remarks----L:");
		rez = rez.replaceAll("\\sDkt:","\r\n----Remarks----Dkt:");
		rez = rez.replaceAll("\\sPg:","\r\n----Remarks----Pg:");
		rez = rez.replaceAll("\\sDismissed","\r\n----Remarks----Dismissed");
		rez = rez.replaceAll("\\sDischarged","\r\n----Remarks----Discharged");
		rez = rez.replaceAll("Orig date filed: ","\r\n----Remarks----Orig date filed: ");;
		rez = rez.replaceAll("Calendar:","\r\n----Remarks----Calendar:");
		rez = rez.replaceAll("Foreclosure type:","\r\n----Remarks----Foreclosure type:");
		rez=rez.replaceAll("\\? -  -   -   -",""); 
		//// compact blank lines and spaces
		rez = rez.replaceAll("\n\\s*\n", "\n");
		return rez;
	}

	
	
	public void input2output(String inSTR, String outSTR) throws Exception {
		OutputStream out = new java.io.FileOutputStream(outSTR);
		Txtparser t = new Txtparser(inSTR);
		t.parse_file();
		StringBuffer sb = new StringBuffer();
		sb.append(t.getPropertyList().toString());
		sb.append(t.getGeneralList().toString());
		sb.append(t.getLocatesList().toString());
		DataOutputStream dout = new DataOutputStream(out);
		dout.write(sb.toString().getBytes());
		dout.close();
	}

	
	public  static void main(String args[]) throws Exception {
		//Txtparser t = new Txtparser("f:\\test\\ATSTEST31_SCH-ZJ-949240.doc");
		//t.parse_file();
		//String temp;
		//tp.parseInput(new FileInputStream("f:\\6.txt"));//.toString();
		System.out.println("Start search");
		//System.out.println("Property");
		Txtparser t = new Txtparser();
		t.input2output("f:\\test\\ATSTEST112_SCH-ZJ-1520839.doc","f:\\res.txt");
		/*System.out.println("----Property search----");
		for (int i=0;i<t.list_P.size();i++){
			System.out.println(((Data_Property)t.list_P.get(i)).toString());
		}/*
		System.out.println("----General search----");
		for (int i=0;i<t.list_G.size();i++){
			System.out.println(((Data_General)t.list_G.get(i)).toString());
		}
		System.out.println("----Locates search----");
		for (int i=0;i<t.list_L.size();i++){
			System.out.println(((Data_Locates)t.list_L.get(i)).toString());
		}
	*/
		System.out.println("End search");
	}
}
