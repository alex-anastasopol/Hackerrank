package ro.cst.tsearch.searchsites.client;

import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Util {
	
	public static final int	SS_COLUMN_STATE_ABREV			= 0;
	public static final int	SS_COLUMN_COUNTY_NAME			= 1;
	public static final int	SS_COLUMN_SITE_TYPE_ABREV		= 2;
	public static final int	SS_COLUMN_PARSER_NAME			= 3;
	public static final int	SS_COLUMN_SERVER_CLASS_NAME		= 4;
	public static final int	SS_COLUMN_CONN_CLASS_NAME		= 5;
	public static final int	SS_COLUMN_LINK					= 6;
	public static final int	SS_COLUMN_MAX_SESSIONS			= 7;
	public static final int	SS_COLUMN_TIMEOUT_PANEL			= 8;
	public static final int	SS_COLUMN_TIME_BETWEEN_REQUETS	= 9;
	public static final int	SS_COLUMN_MAX_REQUESTS_PANEL	= 10;
	public static final int	SS_COLUMN_ENABLE_STATUS			= 11;
	public static final int	SS_COLUMN_EFFECTIVE_START_DATE	= 12;
	public static final int	SS_COLUMN_DOCTYPE				= 13;
	public static final int	SS_COLUMN_CERTIFIED				= 14;
	
	public static final int CS_COLUMN_COMMUNITY_NAME		= 0;
	public static final int CS_COLUMN_STATE_ABREV			= 1;
	public static final int CS_COLUMN_COUNTY_NAME			= 2;
	public static final int CS_COLUMN_SITE_TYPE_ABREV		= 3;
	public static final int	CS_COLUMN_ENABLE_STATUS			= 4;
	public static final int	CS_COLUMN_COMMUNITY_ID			= 5;
	public static final int	CS_COLUMN_COUNTY_ID				= 6;
	
	
    /**
     * Creates a string that contains double values separated by commas
     * @param values the array of input double values
     * @return the tring containing the input values separated by commas
     */
	public static String createStringFromDoubleArray(double[] values){
		if(values == null)
			return "";
		if(values.length == 0)
			return "";
		StringBuffer retVal = new StringBuffer(200);
		retVal.append(Double.toString(values[0]));
		for(int i=1; i<values.length; i++){
			retVal.append(",");
			retVal.append(Double.toString(values[i]));
		}
		return retVal.toString();
	}
	
	/**
	 * Creates an array of double values from a string of numbers separated by commas
	 * @param values the string containing a list of real numbers separated by commas
	 * @return the array of double values containing the values from the input string
	 */
	public static double[] createDoubleArrayFromString(String values){
		try{
			String [] tokens = values.split(",");
			double [] retVal = new double[tokens.length];
			for(int i=0; i<tokens.length; i++){
				retVal[i] = Double.parseDouble(tokens[i]);
			}
			return retVal;
		}catch(Exception e){
			// in case there was some garbage in the string return null
			e.printStackTrace();
			return null;
		}
	}	
	
	public static class SiteDataCompact implements IsSerializable{

		/* Community -> State -> County -> STCountySRV -> GwtDataSite map */
		private 
					Map<String,
						Map<String,
							Map<String,
								GWTDataSite>>> sitesData = null;
		
		private int siteType = -1;
		
		public Map<String, Map<String, Map<String, GWTDataSite>>> getSitesData() {
			return sitesData;
		}
		
		public void setSitesData(Map<String, Map<String, Map<String, GWTDataSite>>> sitesData) {
			this.sitesData = sitesData;
		}

		public int getSiteType() {
			return siteType;
		}
		public void setSiteType(int siteType) {
			this.siteType = siteType;
		}
		
	}

	public static class DataCompact implements IsSerializable{
		
		private Map<Integer,String> communities = null;
		
		private String[] types = null;
		private String[] states = null;
		private String[] counties = null;

		public Map<Integer, String> getCommunities() {
			return communities;
		}
		public void setCommunities(Map<Integer, String> communities) {
			this.communities = communities;
		}
		
		public String[] getStates() {
			return states;
		}
		public void setStates(String[] states) {
			this.states = states;
		}
		public String[] getTypes() {
			return types;
		}
		public void setTypes(String[] types) {
			this.types = types;
		}
		public String[] getCounties() {
			return counties;
		}
		public void setCounties(String[] counties) {
			this.counties = counties;
		}
	}
	
	
	public  final static  String WAITING_MESSAGE = "<font color='blue'><h1> Working ...  </h1></font>";
	public final static int WAITING_POPUP_COL = 450;
	public final static int WAITING_POPUP_ROW = 200;
	
	//the mask must have just one bit set
	public static final int SITE_ENABLED_MASK =  								1;
	public static final int SITE_ENABLED_AUTOMATIC_FULL_SEARCH_MASK =			2;
	public static final int SITE_ENABLED_NAME_BOOT_STRAP_MASK =					4;
	public static final int SITE_ENABLED_NAME_DERIVATION_MASK =		 			8;
	public static final int SITE_ENABLED_LINK_ONLY_MASK = 						16;
	public static final int SITE_ENABLED_OCR_MASK = 							32;
	public static final int SITE_ENABLED_AUTOMATIC_CRT_OWNER_MASK = 			64;
	public static final int SITE_ENABLED_AUTOMATIC_CONSTRUCTION_MASK = 			128;
	public static final int SITE_ENABLED_AUTOMATIC_COMMERCIAL_MASK = 			256;
	public static final int SITE_ENABLED_AUTOMATIC_REFINANCE_MASK = 			512;
	public static final int SITE_ENABLED_AUTOMATIC_OE_MASK	=					1024;
	public static final int SITE_ENABLED_AUTOMATIC_LIENS_MASK = 				2048;
	public static final int SITE_ENABLED_AUTOMATIC_ACREAGE_MASK = 				4096;
	public static final int SITE_ENABLED_AUTOMATIC_SUBLOT_MASK = 				8192;
	public static final int SITE_ENABLED_AUTOMATIC_UPDATE_MASK = 				16384;
	public static final int SITE_ENABLED_INCLUDE_IN_TSR =		 				32768;
	public static final int SITE_ENABLED_ADDRESS_BOOT_STRAP_MASK = 				65536;
	public static final int SITE_ENABLED_LEGAL_BOOT_STRAP_MASK = 				131072;
	public static final int SITE_ENABLED_AUTOMATIC_FVS_UPDATE_MASK = 			262144;
	public static final int SITE_ENABLED_INCLUDE_IMAGE_IN_TSR = 				524288;
	
	
	
	//certified flag
	public static final int SITE_CERTIFIED =		 							1;
	
	// suffix has a lower weight
	public static final String DEFAULT_ADDRESS_TOKEN = "1.0,1.0,1.0,0.4,1.0,1.0,1.0";

	// pre-dir, suffix, post dir can be missing without a high impact
	public static final String DEFAULT_ADDRESS_TOKEN_MISS = "0.0,0.8,0.0,0.8,0.8,0.0,0.0";

	public static final int MODULE_TYPE_SEARCH_SITES 		= 0;
	public static final int MODULE_TYPE_COMMUNITY_SETTINGS 	= 1;
	
	public static boolean isSiteEnabled(int dbenabled){
		return  ( ( SITE_ENABLED_MASK  & dbenabled ) != 0);
	}
	
	public static boolean isSiteEnabledOCR(int dbenabled){
		return  ( ( SITE_ENABLED_OCR_MASK  & dbenabled ) != 0);
	}
	
	public static boolean isSiteEnabledLinkOnly(int dbenabled){
		return  ( ( SITE_ENABLED_LINK_ONLY_MASK  & dbenabled ) != 0);
	}
	
	public static boolean isSiteEnabledIncludeInTsr(int dbenabled){
		return  ( ( SITE_ENABLED_INCLUDE_IN_TSR  & dbenabled ) != 0);
	}
	
	public static boolean isSiteEnabledIncludeImageInTsr(int dbenabled) {
		return  ( ( SITE_ENABLED_INCLUDE_IMAGE_IN_TSR  & dbenabled ) != 0);
	}
	
	public static boolean isSiteCertified(int dbenabled){
		return  ( ( SITE_CERTIFIED & dbenabled ) != 0);
	}
	
	public static boolean isSiteEnabledAutomaticForProduct(int productId, int dbenabled){
		if(productId == 1) {
			return ( ( SITE_ENABLED_AUTOMATIC_FULL_SEARCH_MASK  & dbenabled ) != 0);
		} else if(productId == 2) {
			return ( ( SITE_ENABLED_AUTOMATIC_CRT_OWNER_MASK  & dbenabled ) != 0);
		} else if(productId == 3) {
			return ( ( SITE_ENABLED_AUTOMATIC_CONSTRUCTION_MASK  & dbenabled ) != 0);
		} else if(productId == 4) {
			return ( ( SITE_ENABLED_AUTOMATIC_COMMERCIAL_MASK  & dbenabled ) != 0);
		} else if(productId == 5) {
			return ( ( SITE_ENABLED_AUTOMATIC_REFINANCE_MASK  & dbenabled ) != 0);
		} else if(productId == 6) {
			return ( ( SITE_ENABLED_AUTOMATIC_OE_MASK  & dbenabled ) != 0);
		} else if(productId == 7) {
			return ( ( SITE_ENABLED_AUTOMATIC_LIENS_MASK  & dbenabled ) != 0);
		} else if(productId == 8) {
			return ( ( SITE_ENABLED_AUTOMATIC_ACREAGE_MASK  & dbenabled ) != 0);
		} else if(productId == 9) {
			return ( ( SITE_ENABLED_AUTOMATIC_SUBLOT_MASK  & dbenabled ) != 0);
		} else if(productId == 10) {
			return ( ( SITE_ENABLED_AUTOMATIC_UPDATE_MASK  & dbenabled ) != 0);
		}  else if(productId == 12) {
			return ( ( SITE_ENABLED_AUTOMATIC_FVS_UPDATE_MASK  & dbenabled ) != 0);
		} else {
			return false;
		}
	}
	
	public static boolean isSiteEnabledNameBootstrap(int dbenabled){
		return  ( ( SITE_ENABLED_NAME_BOOT_STRAP_MASK  & dbenabled ) != 0);
	}
	
	public static boolean isSiteEnabledAddressBootstrap(int dbenabled){
		return  ( ( SITE_ENABLED_ADDRESS_BOOT_STRAP_MASK  & dbenabled ) != 0);
	}
	
	public static boolean isSiteEnabledLegalBootstrap(int dbenabled){
		return  ( ( SITE_ENABLED_LEGAL_BOOT_STRAP_MASK  & dbenabled ) != 0);
	}
	
	public static boolean isSiteEnabledDerivation(int dbenabled){
		return  ( ( SITE_ENABLED_NAME_DERIVATION_MASK  & dbenabled ) != 0);
	}
	
	public static int disableSite(int dbenabled){
		return ( (~SITE_ENABLED_MASK) & dbenabled );
	}
	
	public static int disableSiteOCR(int dbenabled){
		return ( (~SITE_ENABLED_OCR_MASK) & dbenabled );
	}
	
	public static int disableSiteIncludeInTsr(int dbenabled){
		return ( (~SITE_ENABLED_INCLUDE_IN_TSR) & dbenabled );
	}
	
	public static int disableSiteLinkOnly(int dbenabled){
		return ( (~SITE_ENABLED_LINK_ONLY_MASK) & dbenabled );
	}
	
	public static int disableSiteAutomatic(int dbenabled){
		return ( (~SITE_ENABLED_AUTOMATIC_FULL_SEARCH_MASK) & dbenabled );
	}
	
	public static int disableSiteNameBootstrap(int dbenabled){
		return ( (~SITE_ENABLED_NAME_BOOT_STRAP_MASK) & dbenabled );
	}
	
	public static int disableSiteAddressBootstrap(int dbenabled){
		return ( (~SITE_ENABLED_ADDRESS_BOOT_STRAP_MASK) & dbenabled );
	}
	
	public static int disableSiteLegalBootstrap(int dbenabled){
		return ( (~SITE_ENABLED_LEGAL_BOOT_STRAP_MASK) & dbenabled );
	}

	public static int disableSiteNameDerivation(int dbenabled){
		return ( (~SITE_ENABLED_NAME_DERIVATION_MASK) & dbenabled );
	}
	
	public static int enableSite(int dbenabled){
		return ( SITE_ENABLED_MASK | dbenabled );
	}
	
	public static int enableSiteOCR(int dbenabled){
		return ( SITE_ENABLED_OCR_MASK | dbenabled );
	}
	
	public static int enableSiteLinkOnly(int dbenabled){
		return ( SITE_ENABLED_LINK_ONLY_MASK  | dbenabled );
	}
	
	public static int enableSiteAutomaticForProduct(int productId, int dbenabled){
		if(productId == 1) {
			return ( SITE_ENABLED_AUTOMATIC_FULL_SEARCH_MASK | dbenabled );
		} else if(productId == 2) {
			return ( SITE_ENABLED_AUTOMATIC_CRT_OWNER_MASK | dbenabled );
		} else if(productId == 3) {
			return ( SITE_ENABLED_AUTOMATIC_CONSTRUCTION_MASK | dbenabled );
		} else if(productId == 4) {
			return ( SITE_ENABLED_AUTOMATIC_COMMERCIAL_MASK | dbenabled );
		} else if(productId == 5) {
			return ( SITE_ENABLED_AUTOMATIC_REFINANCE_MASK | dbenabled );
		} else if(productId == 6) {
			return ( SITE_ENABLED_AUTOMATIC_OE_MASK | dbenabled );
		} else if(productId == 7) {
			return ( SITE_ENABLED_AUTOMATIC_LIENS_MASK | dbenabled );
		} else if(productId == 8) {
			return ( SITE_ENABLED_AUTOMATIC_ACREAGE_MASK | dbenabled );
		} else if(productId == 9) {
			return ( SITE_ENABLED_AUTOMATIC_SUBLOT_MASK | dbenabled );
		} else if(productId == 10){
			return ( SITE_ENABLED_AUTOMATIC_UPDATE_MASK | dbenabled );
		} else {
			return ( SITE_ENABLED_AUTOMATIC_FVS_UPDATE_MASK | dbenabled );
		}
		
	}
	
	public static int enableSiteNameBootstrap(int dbenabled){
		return ( SITE_ENABLED_NAME_BOOT_STRAP_MASK | dbenabled );
	}
	
	public static int enableSiteAddressBootstrap(int dbenabled){
		return ( SITE_ENABLED_ADDRESS_BOOT_STRAP_MASK | dbenabled );
	}
	
	public static int enableSiteLegalBootstrap(int dbenabled){
		return ( SITE_ENABLED_LEGAL_BOOT_STRAP_MASK | dbenabled );
	}

	public static int enableSiteNameDerivation(int dbenabled){
		return ( SITE_ENABLED_NAME_DERIVATION_MASK | dbenabled );
	}
	
	public static int enableSiteIncludeInTsr(int dbenabled){
		return ( SITE_ENABLED_INCLUDE_IN_TSR | dbenabled );
	}
	
	public static int enableSiteIncludeImageInTsr(int enableStatus) {
		return ( SITE_ENABLED_INCLUDE_IMAGE_IN_TSR | enableStatus );
	}
		
	public static int enableCertified(int dbenabled){
		return ( SITE_CERTIFIED | dbenabled );
	}
	
	public static boolean isEmpty(String s){
		if (s==null||"".equals(s)){
			return true;
		}else{
			return false;
		}
	}
	
}
