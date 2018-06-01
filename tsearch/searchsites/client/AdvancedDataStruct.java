package ro.cst.tsearch.searchsites.client;


import com.google.gwt.user.client.rpc.IsSerializable;

public class AdvancedDataStruct implements IsSerializable{

	//public String siteName	=	"";
	
	public String adressToken 		=	"";
	
	public String adressTokenMiss	=	"";
	
	public String alternateLink = null;
	
	public int connType = GWTDataSite.HTTP_CONNECTION;
	
	public String[] siteNameOrder 	=	null;
	
	public String dueDate = null;
	
	public String payDate = null;
	
	public String cityName = null;
	
	public int taxYearMode = TaxSiteData.TAX_YEAR_PD_YEAR;
	
	public int numberOfYears = TaxSiteData.NUMBER_OF_TAX_YEARS;
	
	public String stateAbbreviation = "";
	public String countyName = "";
	public String siteTypeAbbreviation = "";
	
	public String getName() {
		return stateAbbreviation + countyName + siteTypeAbbreviation;
	}
	
}
