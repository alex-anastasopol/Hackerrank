/*
 * Created on Oct 10, 2003
 */
package ro.cst.tsearch.bean;

/**
 * @author nae
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class UploadDocType {
	
	
		
	public static final String REGISTER_FOLDER="Register";
	public static final String COUNTY_FOLDER="County Tax";
	public static final String CITY_FOLDER="City Tax";
	public static final String AO_FOLDER="Assessor";
	
	
	public static final String REGISTER_DOC_NAME="Additional Information";
	//public static final String CITY_DOC_NAME="City Tax Information";
	public static final String CITY_DOC_NAME="City Tax";
	//public static final String COUNTY_DOC_NAME="County Tax Information";
	public static final String COUNTY_DOC_NAME="County Tax";
	public static final String AO_DOC_NAME="Assessor";
	
	public static final int FAKE_DOC_CITY = 0;
	public static final int FAKE_DOC_COUNTY =1;
	public static final int FAKE_DOC_REGISTER=2;
	public static final int FAKE_DOC_ASSESSOR=3;
	
	
	private  boolean isCounty =false;
	private  boolean isCity =false;
	private boolean isAssessor = false;
	private  int fakeDocType = FAKE_DOC_REGISTER;
	private  String destFolder="";
	private  String docName="";
	private  String delimiter="_";
	
	
	public UploadDocType(String docType){
		setDocType(docType);
	}

	/**
	 * @return
	 */
	public String getDelimiter() {
		return delimiter;
	}

	/**
	 * @param string
	 */
	public void setDelimiter() {
		if(isCity)
			delimiter = "";
		else if(isCounty)
			delimiter = "";
		else
			delimiter = "_";
		
	}
	
	 
	/**
	 * @return
	 */
	public boolean isCounty() {
		return isCounty;
	}
	
	public boolean isCity() {
		return isCity;
	}
	
	public boolean isAssessor(){
		return isAssessor;
	}
	
	/**
	 * @param b
	 */
	public void setType(String docType) {
		if(docType.equals("CNTYTAX")){
			isCounty = true;
			isCity = false;
			isAssessor = false;
			return;	
		} else if(docType.equals("CITYTAX")){
			isCounty = false;
			isCity = true;
			isAssessor = false;
			return;
		} else if(docType.equals("ASSESSOR")){
			isCounty = false;
			isCity = false;
			isAssessor = true;
			return;
		}
	}
	
	
	/**
	 * @return
	 */
	public  String getDestFolder() {		
		return destFolder;
	}

	/**
	 * @return
	 */
	public String getDocName() {
		return docName;
	}

	/**
	 * @param string
	 */
	public void setDestFolder() {
		if(isCity)
			destFolder = CITY_FOLDER;
		else if(isCounty)
			destFolder = COUNTY_FOLDER;
		else
			destFolder = REGISTER_FOLDER;
	}

	/**
	 * @param string
	 */
	public void setDocName() {
		if(isCity)
			docName = CITY_DOC_NAME;
		else if(isCounty)
			docName = COUNTY_DOC_NAME;
		else if(isAssessor)
			docName = AO_DOC_NAME;
		else
			docName = REGISTER_DOC_NAME;
	}
	
	public void setDocType(String docType){
		setType(docType);
		setDestFolder();
		setDocName();
		setDelimiter();
		setDocFake();
	}
	
	public void setDocFake(){
		if(isCity)
			fakeDocType = FAKE_DOC_CITY;
		else if (isCounty)
			fakeDocType = FAKE_DOC_COUNTY;
		else if(isAssessor)
			fakeDocType = FAKE_DOC_ASSESSOR;
		else
			fakeDocType = FAKE_DOC_REGISTER;	
	}
	public int getDocFake(){
		return fakeDocType;
	}
}
