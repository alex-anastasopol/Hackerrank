package ro.cst.tsearch.connection.ftp;
import java.io.Serializable;

public class TXTParsedData implements Serializable{
    
    static final long serialVersionUID = 10000000;

     private String NumDoc="";
     private String Grantor="";
	 private String Grantee="";
	 private String PIN="";
	 private String Partial_Legal="";
	 private String Remarks="";
	 
	 
	 public TXTParsedData()
	 {
	 }
	 public TXTParsedData(String a,String b,String c,String d,String e,String f)
	 {
	 	Grantor=a;
	 	Grantee=b;
	 	PIN=c;
	 	Partial_Legal=d;
	 	Remarks=e;
	 	NumDoc=f;
	 }
	/**
	 * @return
	 */
	public String getGrantor() {
		return Grantor;
	}

	/**
	 * @return
	 */
	public String getGrantee() {
		return Grantee;
	}

	/**
	 * @return
	 */
	public String getPIN() {
		return PIN;
	}

	/**
	 * @return
	 */
	public String getPartial_Legal() {
		return Partial_Legal;
	}

	/**
	 * @return
	 */
	public String getRemarks() {
		return Remarks;
	}
	
	/**
	 * @return
	 */
	public String getNumDoc() {
		return NumDoc;
	}

	
	/**
	 * @param string
	 */
	public void setGrantor(String string) {
		Grantor = string;
	}
	
	/**
	 * @param string
	 */
	public void setNumDoc(String string) {
		NumDoc = string;
	}
   public String toString()
   {
   	StringBuffer rez=new StringBuffer();
   	rez.append("---------------------------------------------------\n");
   	rez.append("Document number="+getNumDoc()+"\n");
   	rez.append("Grantor="+getGrantor()+"\n");
	rez.append("Grantee="+getGrantee()+"\n");
	rez.append("PIN="+getPIN()+"\n");
	rez.append("Partial_Legal="+getPartial_Legal()+"\n");
	rez.append("Remarks="+getRemarks()+"\n");
	rez.append("---------------------------------------------------\n");
   	
   	return rez.toString();   	
   }
	/**
	 * @param string
	 */
	public void setGrantee(String string) {
		Grantee = string;
	}

	/**
	 * @param string
	 */
	public void setPIN(String string) {
		PIN = string;
	}

	/**
	 * @param string
	 */
	public void setPartial_Legal(String string) {
		Partial_Legal = string;
	}

	/**
	 * @param string
	 */
	public void setRemarks(String string) {
		Remarks = string;
	}


}