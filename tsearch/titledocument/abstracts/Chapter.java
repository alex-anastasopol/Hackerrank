package ro.cst.tsearch.titledocument.abstracts;

import java.io.Serializable;
import java.util.Vector;
import org.apache.log4j.Category;

@Deprecated
public class Chapter implements Serializable {
    
	static public class Reference{
		public String bookAndPageNO="";
		public String instrumentNO="";
		public String documentNO="";
	}
	
	protected static final Category logger= Category.getInstance(Chapter.class.getName());
    static final long serialVersionUID = 10000000;
	public static String CHAPTER_CHECKED = "included";
	public static String CHAPTER_NOT_CHECKED = "not included";
						   
	//////////////////////////////////////////////////////////////////////////////
	public static class Types implements Serializable, Cloneable {
	
	    static final long serialVersionUID = 10000000;
	    
		public static final Types UNKNOWN=new Types(0),
									ASSESSOR=new Types(1),
									CNTY_TAX=new Types(2),
									CITY_TAX=new Types(3),
									REGISTER=new Types(4),
									ADDITIONAL=new Types(5),
									DAILYNEWS=new Types(6),
									PATRIOTS=new Types(7),
									COURT=new Types(8),
									PACER=new Types(9),
									UCC=new Types(10),
									ORBIT=new Types(11),
									STEWARTPRIOR=new Types(12);
		//////////////////////////////////////////////////////////////////////////
		private final int mType;
		//////////////////////////////////////////////////////////////////////////
		public Types(int value){mType=value;}
		
		public boolean equals(Types t)
		{
		    return t.mType == mType;
		}
		
		
		
		public String toString() {
			return mType + "";
		}
		
		public synchronized Object clone() {
		    
		    try {
		     
		        Types types = (Types) super.clone();
		        
		        return types;
		        
		    } catch (CloneNotSupportedException cnse) {
		        throw new InternalError();
		    }
		}
	}
	//////////////////////////////////////////////////////////////////////////////
	private String NAME ="";
	private String LINK = "";
	private String IMAGEPATH = "";
	private String CHECKBOX="";
	private String FILLED="";
	private String FILLEDTIME="";
	private String GRANTOR="";
	private String GRANTEE="";
	private String INSTTYPE="";
	private String SERVERINSTTYPE =""; 
	public String INSTNO="";
	private String BOOK="";
	private String PAGE="";
	private String DOCTYPEABBREV = "";
	private String AMOUNTPAID=""; 
	private String BASEAMOUNT="";
	private String DELIQUENTAMOUNT="";
	private String TAXYEAR="";
	//for tax documents
	private Vector RECEIPTNUMBER=new Vector();
	private Vector RECEIPTDATE=new Vector();
	private Vector RECEIPTAMOUNT=new Vector();
	
	private String REMARKS="";
	private String CHECKED=CHAPTER_CHECKED;
	private String REFERENCES="";
	private boolean isTransfer = false;
	private boolean uploaded = false;
	private Types TYPE=Types.UNKNOWN;
	private String INCLUDEIMAGE = "checked";
	
	private String INSTRUMENTDATE = "";
	private String GRANTEETR = "";
	private String GRANTEELANDER = "";
	private String MORTGAGEAMOUNT = "";
	private String CONSIDERATIONAMOUNT = "";
	private String DOCUMENTNUMBER = "";
	
	private String SRCTYPE = "";
	
	private String manualCheck = "";
	
	private boolean isCHANGED=false;
	private boolean isDONE=false;
	
	/**
	 * Enter here the new params you need. Currently used:
	 * 	-instrumentSubType
	 * 	-address
	 * 	-legalDescription
	 * 	-transfers
	 */
	private java.util.Hashtable<String, String> params = new java.util.Hashtable<String, String>();
	
	
}
