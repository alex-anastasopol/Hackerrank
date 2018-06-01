/**
 * 
 */
package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.connection.http2.MIGenericOaklandAOTR.SRC_PRODUCT;
import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_SELECT_BOX;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_TEXT_FIELD;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setHiddenParamMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setRequiredCriticalMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.ORIENTATION_HORIZONTAL;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.PIXELS;

import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.CityFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterResponse2;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.FormatException;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author radu bacrau
 *
 */
public abstract class MIGenericOaklandAOTR extends TSServer {

	
	private static final long serialVersionUID = 5479348337118381398L;
	
	protected static final int RESID_ADDRESS_MODULE_IDX = 1;
	protected static final int RESID_PIN_MODULE_IDX     = 2;
	protected static final int COMM_ADDRESS_MODULE_IDX  = 3;
	protected static final int COMM_PIN_MODULE_IDX      = 4;
	protected static final int TAX_ADDRESS_MODULE_IDX   = 5;
	protected static final int TAX_PIN_MODULE_IDX       = 6;
	
	protected boolean downloadingForSave;
    
	
	protected static String citiesSelectString = 
		"<SELECT NAME=\"cvt_cd\">" +
		"<OPTION VALUE=\"\" SELECTED>Select a City, Village, or Township (Optional)</OPTION>" +
		"<OPTION VALUE=\"02\">CITY OF AUBURN HILLS (02)</OPTION>" +
		"<OPTION VALUE=\"04\">CITY OF BERKLEY (04)</OPTION>" +
		"<OPTION VALUE=\"08\">CITY OF BIRMINGHAM (08)</OPTION>" +
		"<OPTION VALUE=\"12\">CITY OF BLOOMFIELD HILLS (12)</OPTION>" +
		"<OPTION VALUE=\"16\">CITY OF CLAWSON (16)</OPTION>" +
		"<OPTION VALUE=\"20\">CITY OF FARMINGTON (20)</OPTION>" +
		"<OPTION VALUE=\"22\">CITY OF FARMINGTON HILLS (22)</OPTION>" +
		"<OPTION VALUE=\"23\">CITY OF FENTON (23)</OPTION>" +
		"<OPTION VALUE=\"24\">CITY OF FERNDALE (24)</OPTION>" +
		"<OPTION VALUE=\"28\">CITY OF HAZEL PARK (28)</OPTION>" +
		"<OPTION VALUE=\"32\">CITY OF HUNTINGTON WOODS (32)</OPTION>" +
		"<OPTION VALUE=\"36\">CITY OF KEEGO HARBOR (36)</OPTION>" +
		"<OPTION VALUE=\"38\">CITY OF LAKE ANGELUS (38)</OPTION>" +
		"<OPTION VALUE=\"40\">CITY OF LATHRUP VILLAGE (40)</OPTION>" +
		"<OPTION VALUE=\"44\">CITY OF MADISON HEIGHTS (44)</OPTION>" +
		"<OPTION VALUE=\"48\">CITY OF NORTHVILLE (48)</OPTION>" +
		"<OPTION VALUE=\"50\">CITY OF NOVI (50)</OPTION>" +
		"<OPTION VALUE=\"52\">CITY OF OAK PARK (52)</OPTION>" +
		"<OPTION VALUE=\"56\">CITY OF ORCHARD LAKE (56)</OPTION>" +
		"<OPTION VALUE=\"60\">CITY OF PLEASANT RIDGE (60)</OPTION>" +
		"<OPTION VALUE=\"64\">CITY OF PONTIAC (64)</OPTION>" +
		"<OPTION VALUE=\"68\">CITY OF ROCHESTER (68)</OPTION>" +
		"<OPTION VALUE=\"70\">CITY OF ROCHESTER HILLS (70)</OPTION>" +
		"<OPTION VALUE=\"72\">CITY OF ROYAL OAK (72)</OPTION>" +
		"<OPTION VALUE=\"80\">CITY OF SOUTH LYON (80)</OPTION>" +
		"<OPTION VALUE=\"76\">CITY OF SOUTHFIELD (76)</OPTION>" +
		"<OPTION VALUE=\"84\">CITY OF SYLVAN LAKE (84)</OPTION>" +
		"<OPTION VALUE=\"88\">CITY OF TROY (88)</OPTION>" +
		"<OPTION VALUE=\"14\">CITY OF VILLAGE OF CLARKSTON (14)</OPTION>" +
		"<OPTION VALUE=\"92\">CITY OF WALLED LAKE (92)</OPTION>" +
		"<OPTION VALUE=\"96\">CITY OF WIXOM (96)</OPTION>" +
		"<OPTION VALUE=\"A\">TOWNSHIP OF ADDISON (A)</OPTION>" +
		"<OPTION VALUE=\"C\">TOWNSHIP OF BLOOMFIELD (C)</OPTION>" +
		"<OPTION VALUE=\"D\">TOWNSHIP OF BRANDON (D)</OPTION>" +
		"<OPTION VALUE=\"E\">TOWNSHIP OF COMMERCE (E)</OPTION>" +
		"<OPTION VALUE=\"G\">TOWNSHIP OF GROVELAND (G)</OPTION>" +
		"<OPTION VALUE=\"H\">TOWNSHIP OF HIGHLAND (H)</OPTION>" +
		"<OPTION VALUE=\"I\">TOWNSHIP OF HOLLY (I)</OPTION>" +
		"<OPTION VALUE=\"J\">TOWNSHIP OF INDEPENDENCE (J)</OPTION>" +
		"<OPTION VALUE=\"K\">TOWNSHIP OF LYON (K)</OPTION>" +
		"<OPTION VALUE=\"L\">TOWNSHIP OF MILFORD (L)</OPTION>" +
		"<OPTION VALUE=\"M\">TOWNSHIP OF NOVI (M)</OPTION>" +
		"<OPTION VALUE=\"N\">TOWNSHIP OF OAKLAND (N)</OPTION>" +
		"<OPTION VALUE=\"O\">TOWNSHIP OF ORION (O)</OPTION>" +
		"<OPTION VALUE=\"P\">TOWNSHIP OF OXFORD (P)</OPTION>" +
		"<OPTION VALUE=\"R\">TOWNSHIP OF ROSE (R)</OPTION>" +
		"<OPTION VALUE=\"S\">TOWNSHIP OF ROYAL OAK (S)</OPTION>" +
		"<OPTION VALUE=\"T\">TOWNSHIP OF SOUTHFIELD (T)</OPTION>" +
		"<OPTION VALUE=\"U\">TOWNSHIP OF SPRINGFIELD (U)</OPTION>" +
		"<OPTION VALUE=\"W\">TOWNSHIP OF WATERFORD (W)</OPTION>" +
		"<OPTION VALUE=\"X\">TOWNSHIP OF WEST BLOOMFIELD (X)</OPTION>" +
		"<OPTION VALUE=\"Y\">TOWNSHIP OF WHITE LAKE (Y)</OPTION>" +
		"<OPTION VALUE=\"TH\">VILLAGE OF BEVERLY HILLS (TH)</OPTION>" +
		"<OPTION VALUE=\"TB\">VILLAGE OF BINGHAM FARMS (TB)</OPTION>" +
		"<OPTION VALUE=\"TF\">VILLAGE OF FRANKLIN (TF)</OPTION>" +
		"<OPTION VALUE=\"RH\">VILLAGE OF HOLLY (RH)</OPTION>" +
		"<OPTION VALUE=\"IH\">VILLAGE OF HOLLY (IH)</OPTION>" +
		"<OPTION VALUE=\"OL\">VILLAGE OF LAKE ORION (OL)</OPTION>" +
		"<OPTION VALUE=\"AL\">VILLAGE OF LEONARD (AL)</OPTION>" +
		"<OPTION VALUE=\"LM\">VILLAGE OF MILFORD (LM)</OPTION>" +
		"<OPTION VALUE=\"DO\">VILLAGE OF ORTONVILLE (DO)</OPTION>" +
		"<OPTION VALUE=\"PO\">VILLAGE OF OXFORD (PO)</OPTION>" +
		"<OPTION VALUE=\"EW\">VILLAGE OF WOLVERINE LAKE (EW)</OPTION>" +
		"</SELECT>";
	
	
	/**
	 * Used to deduct the cvt parameter from the city
	 */
	protected static Hashtable<String, String> cities = new Hashtable<String, String>();
	static {
		Pattern optionPattern = Pattern.compile("<OPTION VALUE=\"([^\"]+)\">([^<]+)</OPTION>");
		Matcher optionMatcher = optionPattern.matcher(citiesSelectString);
		while (optionMatcher.find()){
			String code = optionMatcher.group(1);
			String municip = optionMatcher.group(2).replaceAll("\\([^)]+\\)", "").trim().toLowerCase();
			cities.put(municip, code);
			String city = municip.replaceFirst("(?i)(City|Township|Village) of", "").trim();
			cities.put(city, code);
		}
	}
	
	/**
	 * Constructor
	 * @param searchId
	 */
	public MIGenericOaklandAOTR(long searchId) {
		super(searchId);
	}

	/**
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 */
	public MIGenericOaklandAOTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	/**
	 * 
	 * @param si
	 * @param moduleIndex
	 * @param name
	 * @param searchProduct
	 */
	protected void setupAddressSearchModule(TSServerInfo si, int moduleIndex, String name, String searchProduct, boolean isTax, String destination) {
		
		String contractedName = name.replaceAll(" ", "");
		
		TSServerInfoModule sim = si.ActivateModule(moduleIndex, 3);
		sim.setName(contractedName);
		sim.setDestinationPage(destination);
		sim.setRequestMethod(TSConnectionURL.idPOST);
		sim.setParserID(ID_SEARCH_BY_ADDRESS);

		PageZone pz = new PageZone(contractedName, name, ORIENTATION_HORIZONTAL, null, 500, 50, PIXELS, true);

		try {

			HTMLControl 
			addr = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 1, 1, 25, sim.getFunction(0), "prop_addr", "Number & Street", null, searchId), 
			cvt = new HTMLControl(HTML_SELECT_BOX, 1, 1, 2, 2, 1, sim.getFunction(1), "cvt_cd", "City/Village/Twp.", "", searchId), 
			type = new HTMLControl(	HTML_TEXT_FIELD, 1, 1, 1, 1, 1, sim.getFunction(2), SRC_PRODUCT, "Search Product", searchProduct, searchId);

			setupSelectBox(sim.getFunction(1), citiesSelectString);
			setHiddenParamMulti(true, type);
			setRequiredCriticalMulti(true, addr);
			pz.addHTMLObjectMulti(addr, cvt, type);

		} catch (FormatException e) {
			e.printStackTrace();
		}

		sim.getFunction(0).setSaKey(SearchAttributes.P_STREET_NO_NAME);        
        
		sim.setModuleParentSiteLayout(pz);
	}
	
	/**
	 * 
	 * @param si
	 * @param moduleIndex
	 * @param name
	 * @param searchProduct
	 * @param useCity
	 */
	protected void setupPinSearchModule(TSServerInfo si, int moduleIndex, String name, String searchProduct, boolean isTax, String destination) {
		
		String contractedName = name.replaceAll(" ", "");
				
		TSServerInfoModule sim = si.ActivateModule(moduleIndex, isTax ? 3 : 2);
		sim.setName(contractedName);
		sim.setDestinationPage(destination);
		sim.setRequestMethod(TSConnectionURL.idPOST);
		sim.setParserID(ID_SEARCH_BY_PARCEL);

		PageZone pz = new PageZone(contractedName, name, ORIENTATION_HORIZONTAL, null, 500, 50, PIXELS, true);

		try {
			
			HTMLControl 
			pin = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 1, 1, 25, sim.getFunction(0), "Parcel_ID", "Parcel Id", null, searchId), 
			type = new HTMLControl(	HTML_TEXT_FIELD, 1, 1, 1, 1, 1, sim.getFunction(1), SRC_PRODUCT, "Search Product", searchProduct, searchId);
			
			setHiddenParamMulti(true, type);
			setRequiredCriticalMulti(true, pin);
			pz.addHTMLObjectMulti(pin, type);

			if(isTax){
				HTMLControl cvt = new HTMLControl(HTML_SELECT_BOX, 1, 1, 2, 2, 1, sim.getFunction(2), "cvt_cd", "City/Village/Twp.", "", searchId);
				setupSelectBox(sim.getFunction(2), citiesSelectString.replace("(Optional)", "(Req'd)"));
					
				cvt.setRequiredCritical(true);
				pz.addHTMLObject(cvt);
			}

		} catch (FormatException e) {
			e.printStackTrace();
		}
		
		sim.getFunction(0).setSaKey(SearchAttributes.LD_PARCELNO); 

		sim.setModuleParentSiteLayout(pz);
	}


	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
			throws ro.cst.tsearch.exceptions.ServerResponseException {
		htmlString = htmlString.replaceFirst("(?i)<table[^>]*>", "<table cellspacing=\"0\" cellpadding=\"0\" border=\"1\">");
		p.splitResultRows(pr, htmlString, pageId, "<TR", "</TABLE>", linkStart, action);

	}
	
	/**
	 * Add the derrivations for address search 
	 * @param serverInfo
	 * @param l
	 * @param moduleIdx
	 */
	protected void setAddressAutomaticSearch(TSServerInfo serverInfo, List<TSServerInfoModule> l, int moduleIdx){
				
		// check if we have street name
		String streetName = getSearchAttribute(SearchAttributes.P_STREETNAME);		
		streetName = streetName.replaceAll("\\s+", " ").trim();
		if ("".equals(streetName)) {
			return;
		}

		// use first word if it has more than 7 letters
		int idx = streetName.indexOf(" ");
		if(idx > 7){
			streetName = streetName.substring(0, idx);
		}
		
		String streetNo = getSearchAttribute(SearchAttributes.P_STREETNO);
		String dir = getSearchAttribute(SearchAttributes.P_STREETDIRECTION);		
		
		Set<String> cityCodes = new LinkedHashSet<String>();
		cityCodes.add(getCityCode());
		cityCodes.add("");
		
		CityFilterResponse cityFilter = new CityFilterResponse("",searchId);
		cityFilter.setThreshold(new BigDecimal("0.75"));
		cityFilter.setStrategyType( FilterResponse.STRATEGY_TYPE_BEST_RESULTS );
		
		for(String cityCode: cityCodes){
			
			TSServerInfoModule m = new TSServerInfoModule(serverInfo.getModule(moduleIdx));			
			m.clearSaKeys();
			String newName = streetNo + " " + streetName;
			m.getFunction(0).forceValue(newName);
			m.getFunction(1).forceValue(cityCode);					
			m.addFilter(new AddressFilterResponse2("",searchId));
			m.addFilter(cityFilter);
			l.add(m);
			
			// derrivation with direction
			if(!"".equals(dir)){				
				
				// full direction
				m = new TSServerInfoModule(serverInfo.getModule(moduleIdx));
				m.clearSaKeys();
				newName = streetNo + " " + dir + " " + streetName;
				m.getFunction(0).forceValue(newName);
				m.getFunction(1).forceValue(cityCode);	
				m.addFilter(new AddressFilterResponse2("",searchId));
				m.addFilter(cityFilter);
				l.add(m);
				
				// first letter of direction
				if(dir.length() != 1){
					m = new TSServerInfoModule(serverInfo.getModule(moduleIdx));
					m.clearSaKeys();
					newName = streetNo + " " + dir.substring(0,1) + " " + streetName;
					m.getFunction(0).forceValue(newName);
					m.getFunction(1).forceValue(cityCode);
					m.addFilter(new AddressFilterResponse2("",searchId));
					m.addFilter(cityFilter);
					l.add(m);					
				}
			}
		}
	}		
	
	protected String getCityCode(){

		String cityCode = "";									
		
		// try to find city code from municipality
		String municipality = getSearchAttribute(SearchAttributes.P_MUNICIPALITY).toLowerCase();
		if (!StringUtils.isEmpty(municipality)) {
			cityCode = cities.get(municipality);
		}			
		// try to find city code from city			
		if (StringUtils.isEmpty(cityCode)){
			String city = getSearchAttribute(SearchAttributes.P_CITY).toLowerCase();
			if(!StringUtils.isEmpty(city)){			
				cityCode = cities.get(city);
			}
		}				
		if(cityCode == null){
			cityCode = "";
		}
		
		return cityCode;
	}
}
