package ro.cst.tsearch.ps2txt;
import java.io.Serializable;

public class TXTParsedData implements Serializable{
    
    static final long serialVersionUID = 10000000;

     private String date="";
	 private String dtype="";
	 private String dnumber="";
	 private String grantor="";
	 private String grantee="";
	 private String book="";
	 private String page="";
	 private String junk="";
	 
	 public TXTParsedData()
	 {
	 }
	 public TXTParsedData(String d,String type,String n,String gtor,String gtee,String j)
	 {
	 	date=d;
	 	dtype=type;
	 	dnumber=n;
	 	grantor=gtor;
	 	grantee=gtee;
	 	junk=j;
	 }
	/**
	 * @return
	 */
	public String getDate() {
		return date;
	}

	/**
	 * @return
	 */
	public String getDnumber() {
		return dnumber;
	}

	/**
	 * @return
	 */
	public String getDtype() {
		return dtype;
	}

	/**
	 * @return
	 */
	public String getGrantee() {
		return grantee;
	}

	/**
	 * @return
	 */
	public String getGrantor() {
		return grantor;
	}

	/**
	 * @return
	 */
	public String getJunk() {
		return junk;
	}

	/**
	 * @param string
	 */
	public void setJunk(String string) {
		junk = string;
	}
   public String toString()
   {
   	StringBuffer rez=new StringBuffer();
   	rez.append("---------------------------------------------------\n");
   	rez.append("Date="+getDate()+"\n");
	rez.append("Number="+getDnumber()+"\n");
	rez.append("Type="+getDtype()+"\n");
	rez.append("Grantee="+getGrantee()+"\n");
	rez.append("Grantor="+getGrantor()+"\n");
	rez.append("Junk="+getJunk()+"\n");
	rez.append("---------------------------------------------------\n");
   	
   	return rez.toString();   	
   }
	/**
	 * @param string
	 */
	public void setDate(String string) {
		date = string;
	}

	/**
	 * @param string
	 */
	public void setDnumber(String string) {
		dnumber = string;
	}

	/**
	 * @param string
	 */
	public void setDtype(String string) {
		dtype = string;
	}

	/**
	 * @param string
	 */
	public void setGrantee(String string) {
		grantee = string;
	}

	/**
	 * @param string
	 */
	public void setGrantor(String string) {
		grantor = string;
	}

	/**
	 * @return
	 */
	public String getBook() {
		return book;
	}

	/**
	 * @return
	 */
	public String getPage() {
		return page;
	}

	/**
	 * @param string
	 */
	public void setBook(String string) {
		book = string;
	}

	/**
	 * @param string
	 */
	public void setPage(String string) {
		page = string;
	}

}