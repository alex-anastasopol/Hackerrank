package ro.cst.tsearch.connection.ftp;
import java.io.Serializable;

public class Data_Property implements Serializable{
    
    static final long serialVersionUID = 10000000;

     private String NumDoc="";
     private String Grantor="";
	 private String Grantee="";
	 private String PIN="";
	 private String Partial_Legal="";
	 private String Remarks="";
	 private String Type="";
	 private String Date="";
	 private String Address="";
	 private String Certificate=""; 
	 
	 public Data_Property()
	 {
	 }
	 public Data_Property(String a,String b,String c,String d,String e,String f,String g,String h,String i,String j)
	 {
	 	Grantor=a;
	 	Grantee=b;
	 	PIN=c;
	 	Partial_Legal=d;
	 	Remarks=e;
	 	NumDoc=f;
	 	Type=g;
	 	Date=h;
	 	Address=i;
	 	Certificate=j;
	 	
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
	public String getType() {
		return Type;
	}
	
	/**
	 * @return
	 */
	public String getDate() {
		return Date;
	}
	/**
	 * @return
	 */
	public String getAddress() {
		return Address;
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
	 * @return
	 */
	public String getCertificate() {
		return Certificate;
	}

	
	/**
	 * @param string
	 */
	public void setCertificate(String string) {
		Certificate = string;
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
	public void setType(String string) {
		Type = string;
	}
	
	/**
	 * @param string
	 */
	public void setDate(String string) {
		Date = string;
	}
	/**
	 * @param string
	 */
	public void setAddress(String string) {
		Address = string;
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
   	if (getType()!="")
   		rez.append("Type="+getType()+"\n");
   	if (getNumDoc()!="")
   		rez.append("Document number="+getNumDoc()+"\n");
   	if (getDate()!="")
   		rez.append("Date="+getDate()+"\n");
   	if (getGrantor()!="")
   		rez.append("Grantor="+getGrantor()+"\n");
   	if (getGrantee()!="")
   		rez.append("Grantee="+getGrantee()+"\n");
   	if (getPIN()!="")
   		rez.append("PIN="+getPIN()+"\n");
   	if (getPartial_Legal()!="")
   		rez.append("Partial_Legal="+getPartial_Legal()+"\n");
   	if (getRemarks()!="")
   		rez.append("Remarks="+getRemarks()+"\n");
   	if (getAddress()!="")
   		rez.append("Address="+getAddress()+"\n");
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