package ro.cst.tsearch.connection.ftp;
import java.io.Serializable;

public class Data_General implements Serializable{
    
    static final long serialVersionUID = 10000000;

     private String NumDoc="";//
     private String Grantor="";//
	 private String Grantee="";//
	 private String Case_Number="";////////
	 private String SSNBUSID="";//
	 private String Remarks="";//
	 private String Type="";//
	 private String Date="";//
	 private String Address="";//
	 
	 public Data_General()
	 {
	 }
	 public Data_General(String a,String b,String c,String e,String f,String g,String h,String i,String j)
	 {
	 	Grantor=a;
	 	Grantee=b;
	 	SSNBUSID=c;
	 	Remarks=e;
	 	NumDoc=f;
	 	Type=g;
	 	Date=h;
	 	Address=i;
	 	Case_Number=j;
	 	
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
	public String getSSNBUSID() {
		return SSNBUSID;
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
	public String getCase_Number() {
		return Case_Number;
	}

	
	/**
	 * @param string
	 */
	public void setCase_Number(String string) {
		Case_Number = string;
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
   	if (getSSNBUSID()!="")
		rez.append("SSNBUSID="+getSSNBUSID()+"\n");
   	if (getRemarks()!="")
		rez.append("Remarks="+getRemarks()+"\n");
   	if (getAddress()!="")
		rez.append("Address="+getAddress()+"\n");
   	if (getCase_Number()!="")
		rez.append("Case Number="+getCase_Number()+"\n");
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
	public void setSSNBUSID(String string) {
		SSNBUSID = string;
	}



	/**
	 * @param string
	 */
	public void setRemarks(String string) {
		Remarks = string;
	}


}