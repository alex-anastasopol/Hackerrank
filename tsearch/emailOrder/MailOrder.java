package ro.cst.tsearch.emailOrder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.util.PDFTextStripper;
import org.w3c.dom.Document;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.data.GenericCounty;
import ro.cst.tsearch.data.GenericState;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.ProductsMapper;
import ro.cst.tsearch.exceptions.InvalidEmailOrderException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.XMLExtractor;
import ro.cst.tsearch.extractor.xml.XMLUtils;
import ro.cst.tsearch.servers.ServletServerComm;
import ro.cst.tsearch.servers.response.NameSet;
import ro.cst.tsearch.servers.response.OtherInformationSet;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.UserValidation;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.name.NameSourceType;
import com.stewart.ats.base.parties.PartyI;

/**
 * 
 * @author marinela bacrau
 *
 */
public class MailOrder {
	
	public User savedUser = null;
	public String stateAbbrev = "";
	public String countyAbbrev = "";
	public String fileID = null;
	public String username = null;
	
	public boolean undefinedCounty = false;
	public boolean undefinedState = false;
	public boolean invalidUser = false;
	public boolean invalidPassword = false;
	public String product = "";
	private String password = null;
	public boolean setDates = true;
	
	/**
	 * 
	 * @param pdfFile
	 * @return
	 * @throws Exception
	 */
	public Search getSearch (String fileName) throws Exception{
		
		savedUser = null;
		
		undefinedCounty = false;
		undefinedState = false;
		invalidUser = false;
		username = null;
		
		User user = null;
		SearchAttributes sa = null;
		
		boolean txto = fileName.matches("(?i)^.*\\.txto$");
		Search search = null;
		if(!txto){
			// .pdf extension
			search = new Search(DBManager.getNextId(DBConstants.TABLE_SEARCH));
			String text = extractTextFromPDF(fileName);			
			ParsedResponse pr = parseText(text,search);			
			sa = getSAFromResponse(pr);			
			sa.setSet(true);	
			username = getUserNameFromResponse(pr);		
			user = getUser(username);
			if(user != null) {
				search.unFakeSearch(user);
			}
		} else {
			// .txto extension
			Map<String,String> params = FileUtils.loadParams(fileName, true);
			product = params.get("product");
			sa = getSAFromParams(params);
			username = params.get("agent_name");
			password = params.get("agent_passwd");
			user = getUser(username, password);	       
		}
					
		try {
		  for( String attribute : new String[] {
					SearchAttributes.P_STREETNAME, 
					SearchAttributes.P_STREETNO, 
					SearchAttributes.P_STREETDIRECTION,
					SearchAttributes.P_STREETSUFIX,
					SearchAttributes.P_STREETUNIT,
					SearchAttributes.P_CITY,
					SearchAttributes.P_ZIP,
					SearchAttributes.LD_PARCELNO,
					SearchAttributes.LD_INSTRNO,
					SearchAttributes.LD_BOOKPAGE,
					SearchAttributes.LD_LOTNO, 
					SearchAttributes.LD_SUBLOT, 
					SearchAttributes.LD_SUBDIV_NAME, 
					SearchAttributes.LD_SUBDIV_SEC,
					SearchAttributes.LD_SUBDIV_PHASE,
					SearchAttributes.LD_SUBDIV_BLOCK,
					SearchAttributes.LD_SUBDIV_TRACT
					} )	{
			  sa.getSearchPageManualFields().put(attribute,sa.getAtribute(attribute));
		  }
		}catch(Exception e) {
			e.printStackTrace();
		}
		  
		sa.cleanupNames();
		savedUser = user;
			
		if (user == null){
			invalidUser = true;
			if(invalidPassword){
				invalidUser = false;
			}
		}
		
		if(sa.getAtribute(SearchAttributes.P_COUNTY).length() == 0){
			undefinedCounty = true;
		}
		
		if(sa.getAtribute(SearchAttributes.P_STATE).length() == 0){
			undefinedState = true;
		}
		
		if(invalidUser || undefinedCounty || undefinedState || invalidPassword){
			InvalidEmailOrderException e = new InvalidEmailOrderException("");
			e.setInvalidUser(invalidUser);
			e.setUndefinedCounty(undefinedCounty);
			e.setUndefinedState(undefinedState);
			e.setUserName(username);
			e.setInvalidPassword(invalidPassword);
			throw e;
		}
		
		
		if(!txto){
			SearchManager.setSearch(search, user);
			sa.setSearchId(search.getID());
			search.setSa(sa);
		} else {
			search = SearchManager.addNewSearch(user, false);
			sa.setSearchId(search.getID());
			search.setSa(sa);	
		}
		        	
		
		return search;
	}
	
	/**
	 * Return the user designated by <tt>username</tt>
	 * @param username user name
	 * @return user
	 */
	public static User getUser(String username){
		
		if (username == null){
			return null;
		}
		
		UserAttributes userAttributes = UserManager.getUser(username, false);	
		
		if (userAttributes == null) {
			return null;
		}
				
		return getUser(userAttributes);		
	}
	
	/**
	 * Return the user designated by <tt>username</tt> and <tt>password</tt> 
	 * @param username user name
	 * @param password user password
	 * @return user
	 */	
	private User getUser(String username, String password){
		
		if(username == null || password == null){
			return null;
		}
		
		UserAttributes userAttributes = UserManager.getUser(username, false);	
		if (userAttributes == null) {
			return null;
		}
		if(!UserManager.checkCredentials(username, password, true)){
			invalidPassword = true;
			return null;
		}
		
		return getUser(userAttributes);
	}
	
	/**
	 * Return the user designated by the user attributes parameter
	 * @param userAttributes
	 * @return
	 */
	private static User getUser(UserAttributes userAttributes){
		User user = new User(UserValidation.getDirPath(ServerConfig.getFilePath()));
		user.setUserAttributes(userAttributes);		
		return user;
	}

	/**
	 * 
	 * @param str
	 * @return
	 */
	private String capitalizeFirst(String str){
		if(str == null) return str;
		if(str.length() == 0) return str;
		if(str.length() == 1) return str.toUpperCase();
		str = str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
		return str;
	}
	
	/**
	 * 
	 * @param pr
	 * @return
	 */
	private String getUserNameFromResponse(ParsedResponse pr) {
		
		OtherInformationSet ois = pr.getOtherInformationSet();
		String company = ois.getAtribute("AgentCompany");
		String agent = ois.getAtribute("AgentName");
		if ((company == null) || (company.length() == 0)){
			return null;
		}
		String cw;
		int ci = company.indexOf(" ");
		if (ci == -1){
			cw = company;
		} else{
			cw = company.substring(0, ci);
		} 
		if ((agent == null) || (agent.length() == 0)) {
			return capitalizeFirst(cw);
		} else {
			String aw;
			int ai = agent.indexOf(" ");
			if (ai == -1){
				aw = agent;
			} else{
				aw = agent.substring(0, ai);
			}			
			return capitalizeFirst(cw)+"."+capitalizeFirst(aw);
		}
	}
		
	private /*static*/ String extractTextFromPDF(String pdfFile) throws Exception{
		
        Writer output = null;
        PDDocument document = null;
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        
        try{
            document = PDDocument.load(pdfFile);       
            output = new OutputStreamWriter(bas);
            
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.writeText(document, output);
        }
        finally{
            if(output != null){
                output.close();
            }
            if(document != null){
                document.close();
            }
        }   
        
        return bas.toString();
	}
	
	/**
	 * 
	 * @param text
	 * @return
	 * @throws Exception
	 */
	private ParsedResponse parseText (String text,Search search) throws Exception{
		
        // parse the string according to .xml rules file; 
        // the relevant info will be stored in PropertyIdentificationSet and GranteeSet infsets 
        
        String context = BaseServlet.REAL_PATH;
        String xmlRulesLocation = BaseServlet.REAL_PATH + File.separator + "WEB-INF" + File.separator + "classes" + File.separator + "rules";
        
        Document rules = XMLUtils.read(new File(xmlRulesLocation + File.separator + "emailOrder.xml"), xmlRulesLocation);

        XMLExtractor xmle = new XMLExtractor(text, rules, context,search.getID(),"");        
        xmle.process();

        ResultMap res = xmle.getDefinitions();
        CurrentInstance ci = new CurrentInstance();
		ci.setCrtSearchContext(search);
		SearchAttributes sa = new SearchAttributes(search.getID());
		search.setSa(sa);
		InstanceManager.getManager().setCurrentInstance(search.getID(), ci);
        
        // process the info extracted from pdf when needed
        
        // translate state abreviation to state name in the result map
        String stateAbv = (String) res.get("PropertyIdentificationSet.State");
        if ((stateAbv != null) && (stateAbv.length() != 0)){        
        	GenericState state =  DBManager.getStateForAbv(stateAbv);
        	if(state != null) {
	        	search.getSa().setAtribute(SearchAttributes.P_STATE, String.valueOf(state.getId()));
	        	stateAbbrev = stateAbv;
	        	res.put("PropertyIdentificationSet.State", String.valueOf(state.getId()) );
	            String countyAbv = (String) res.get("PropertyIdentificationSet.County");
	            if ((countyAbv != null) && (countyAbv.length() != 0)){
	            	countyAbbrev = countyAbv;
	            	GenericCounty countyForOrder = DBManager.getCountyForNameAndStateId(countyAbv, state.getId());
	            	if(countyForOrder != null) {
		            	res.put("PropertyIdentificationSet.County", 
		            			String.valueOf(countyForOrder.getId()) );
		            	search.getSa().setAtribute(SearchAttributes.P_COUNTY, 
		            			String.valueOf(countyForOrder.getId()));
	            	}
	            } else {
	            	res.put("PropertyIdentificationSet.County", "");	
	            }
        	} else {
        		res.put("PropertyIdentificationSet.State", "");
        		res.put("PropertyIdentificationSet.County", "");
        	}
        }
                
        // when Plat Book & Page don't have a valid form (e.g. number-number), it means this info is missing or incomplete;
        // in this case use the empty string for plat book & page   
        String platBookPage = (String) res.get("PropertyIdentificationSet.PlatBook");
        if (!platBookPage.matches("\\d+-\\d+")) {
            res.put("PropertyIdentificationSet.PlatBook", "");        	
        }
                         
        // import parsed data into a ParsedResponse object
        ParsedResponse pr = new ParsedResponse();
        Bridge b = new Bridge(pr, res, search.getID());
        b.mergeInformation();
        
        return pr;	
	}	
	
	/**
	 * 
	 * @param pr
	 * @return
	 * @throws Exception
	 */
	private SearchAttributes getSAFromResponse (ParsedResponse pr) throws Exception{
		
		SearchAttributes sa = new SearchAttributes(Search.FROM_MAIL_ORDER_SEARCH_ID);
		
		PropertyIdentificationSet pis = pr.getPropertyIdentificationSet(0);
		
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.P_STREETNO, "StreetNo");
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.P_STREET_FULL_NAME, "StreetName");
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.P_CITY, "City");
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.P_COUNTY, "County");
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.P_STATE, "State");		
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.P_ZIP, "Zip");
		
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.LD_PARCELNO, "ParcelID");
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.LD_SUBDIV_NAME, "SubdivisionName");				
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.LD_LOTNO, "SubdivisionLotNumber");
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.LD_SUBLOT, "SubLot");
		ServletServerComm.addFromIS(sa, pis, SearchAttributes.LD_BOOKPAGE, "PlatBook");
		
		NameSet gns = pr.getGranteeNameSet(0);
		
		NameI name = new Name(
				StringUtils.cleanInitialOrderName(pis.getAtribute("OwnerFirstName")), 
				StringUtils.cleanInitialOrderName(pis.getAtribute("OwnerMiddleName")), 
				StringUtils.cleanInitialOrderName(pis.getAtribute("OwnerLastName")));
		
		if(!name.isEmpty()) {
			sa.getSearchPageManualOwners().add(name);
			name.getNameFlags().setOnlyThisSourceType(new NameSourceType(NameSourceType.ORDER));
			sa.getOwners().add(name);
		}
		
		name = new Name(
				StringUtils.cleanInitialOrderName(pis.getAtribute("SpouseFirstName")), 
				StringUtils.cleanInitialOrderName(pis.getAtribute("SpouseMiddleName")), 
				StringUtils.cleanInitialOrderName(pis.getAtribute("SpouseLastName")));
		
		if(!name.isEmpty()) {
			sa.getSearchPageManualOwners().add(name);
			name.getNameFlags().setOnlyThisSourceType(new NameSourceType(NameSourceType.ORDER));
			sa.getOwners().add(name);
		}
		
		name = new Name(
				StringUtils.cleanInitialOrderName(gns.getAtribute("OwnerFirstName")), 
				StringUtils.cleanInitialOrderName(gns.getAtribute("OwnerMiddleName")), 
				StringUtils.cleanInitialOrderName(gns.getAtribute("OwnerLastName")));
		
		if(!name.isEmpty()) {
			name.getNameFlags().setOnlyThisSourceType(new NameSourceType(NameSourceType.ORDER));
			sa.getSearchPageManualBuyers().add(name);
			sa.getBuyers().add(name);
			if (parseProductType(product) == SearchAttributes.SEARCH_PROD_REFINANCE) {
				sa.getSearchPageManualOwners().add(name); //CR 3870
			}
		}
		
		name = new Name(
				StringUtils.cleanInitialOrderName(gns.getAtribute("SpouseFirstName")), 
				StringUtils.cleanInitialOrderName(gns.getAtribute("SpouseMiddleName")), 
				StringUtils.cleanInitialOrderName(gns.getAtribute("SpouseLastName")));
		
		if(!name.isEmpty()) {
			name.getNameFlags().setOnlyThisSourceType(new NameSourceType(NameSourceType.ORDER));
			sa.getSearchPageManualBuyers().add(name);
			sa.getBuyers().add(name);
			if (parseProductType(product) == SearchAttributes.SEARCH_PROD_REFINANCE) {
				sa.getSearchPageManualOwners().add(name); //CR 3870
			}
		}
		
		
		OtherInformationSet ois = pr.getOtherInformationSet();
		
		ServletServerComm.addFromIS(sa, ois, SearchAttributes.ORDERBY_FILENO, "FileNumber");
		ServletServerComm.addFromIS(sa, ois, SearchAttributes.ADDITIONAL_INFORMATION, "Comment");
	
		fileID = sa.getAtribute(SearchAttributes.ORDERBY_FILENO);
		if(fileID.equals("")){
			fileID = null;
		}
		
		return sa;
	}
	
	/**
	 * Set a search attribute if value is not null
	 * @param sa search attributes
	 * @param key key
	 * @param value value
	 */
	private static void setSaAttribute(SearchAttributes sa, String key, String value){
		if(value == null){ return; }
		sa.setAtribute(key, value);
	}
	
	/**
	 * Fill a search attributes structure from a parametes map
	 * @param params
	 * @return search attributes
	 */
	private SearchAttributes getSAFromParams(Map<String,String> params){
		
		SearchAttributes sa = new SearchAttributes(Search.FROM_MAIL_ORDER_SEARCH_ID);
		
		if (URLMaping.INSTANCE_DIR.startsWith("local") && "testATS".equals(sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO))) {
			sa.setAtribute(SearchAttributes.ABSTRACTOR_FILENO, "");
		}
		
		// general
		setSaAttribute(sa,SearchAttributes.ORDERBY_FILENO, params.get("file_id"));
		setSaAttribute(sa,SearchAttributes.ADDITIONAL_INFORMATION, params.get("notes"));	
		fileID = sa.getAtribute(SearchAttributes.ORDERBY_FILENO);
		
		// from date/to date
		SimpleDateFormat sdf1 = new SimpleDateFormat("MM/dd/yyyy");
		SimpleDateFormat sdf2 = new SimpleDateFormat("MMM d, yyyy");

		boolean startDateSet = false;
		String fromDate = params.get("start_date");
		if(fromDate != null && !"".equals(fromDate)){
			try{
				Date start = sdf1.parse(fromDate);	   
				setSaAttribute(sa, SearchAttributes.FROMDATE, sdf2.format(start));
				startDateSet = true;
			}catch(ParseException ignored){}
		}
		
		boolean endDateSet = false;
		String toDate = params.get("end_date");
		if(toDate != null && !"".equals(toDate)){
			try{
				Date end = sdf1.parse(toDate);
				setSaAttribute(sa, SearchAttributes.TODATE, sdf2.format(end));
				endDateSet = true;
			}catch(ParseException ignored){}
		}
		
		// if either one was not correctly set, then set setDates so that the defaults are used
		setDates = !startDateSet || !endDateSet;
		
		// property
		setSaAttribute(sa,SearchAttributes.P_STREETNO, params.get("street_no"));
		setSaAttribute(sa,SearchAttributes.P_STREETDIRECTION, params.get("street_dir"));
		setSaAttribute(sa,SearchAttributes.P_STREETNAME, params.get("street_name"));
		setSaAttribute(sa,SearchAttributes.P_STREETSUFIX, params.get("street_suffix"));
		setSaAttribute(sa,SearchAttributes.P_STREETUNIT, params.get("street_unit"));		
		
		setSaAttribute(sa,SearchAttributes.P_CITY, params.get("city"));		
		setSaAttribute(sa,SearchAttributes.P_ZIP, params.get("zip"));
		
		// county+state
        // translate state abbreviation to state name in the result map
        String stateAbv = params.get("state");
        if ((stateAbv != null) && (stateAbv.length() != 0)){        
        	GenericState state =  DBManager.getStateForAbv(stateAbv);
        	if(state != null) {
	        	stateAbbrev = stateAbv;
	        	String stateId = String.valueOf(state.getId());
	        	setSaAttribute(sa,SearchAttributes.P_STATE, stateId);
	            String countyAbv =  params.get("county");
	            if ((countyAbv != null) && (countyAbv.length() != 0)){
	            	countyAbbrev = countyAbv;
	            	GenericCounty countyForOrder = DBManager.getCountyForNameAndStateId(countyAbv, state.getId());
	            	if(countyForOrder != null) {
		            	setSaAttribute(sa,SearchAttributes.P_COUNTY, String.valueOf(countyForOrder.getId()));
	            	}
	            }
        	}
        }
         		
		
		setSaAttribute(sa,SearchAttributes.LD_PARCELNO, params.get("parcel_id"));
		setSaAttribute(sa,SearchAttributes.LD_SUBDIV_NAME, params.get("subdivision_name"));
		setSaAttribute(sa,SearchAttributes.LD_LOTNO, params.get("lot"));
		setSaAttribute(sa,SearchAttributes.LD_SUBLOT, org.apache.commons.lang.StringUtils.defaultString(params.get("sublot")));		
		setSaAttribute(sa,SearchAttributes.LD_SUBDIV_SEC, params.get("sec"));
		setSaAttribute(sa,SearchAttributes.LD_SUBDIV_BLOCK, params.get("blk"));
		setSaAttribute(sa,SearchAttributes.LD_BOOKPAGE, params.get("plat_book_page"));
		setSaAttribute(sa,SearchAttributes.LD_INSTRNO, params.get("instrument"));
						
		NameI name = new Name(
				StringUtils.cleanInitialOrderName(params.get("owner_first")), 
				StringUtils.cleanInitialOrderName(params.get("owner_middle")), 
				StringUtils.cleanInitialOrderName(params.get("owner_last")));
		
		if(!name.isEmpty()) {
			sa.getSearchPageManualOwners().add(name);
			name.getNameFlags().setOnlyThisSourceType(new NameSourceType(NameSourceType.ORDER));
			sa.getOwners().add(name);
		}
		
		name = new Name(
				StringUtils.cleanInitialOrderName(params.get("co_owner_first")), 
				StringUtils.cleanInitialOrderName(params.get("co_owner_middle")), 
				StringUtils.cleanInitialOrderName(params.get("co_owner_last")));
		
		if(!name.isEmpty()) {
			sa.getSearchPageManualOwners().add(name);
			name.getNameFlags().setOnlyThisSourceType(new NameSourceType(NameSourceType.ORDER));
			sa.getOwners().add(name);
		}
		
		name = new Name(
				StringUtils.cleanInitialOrderName(params.get("buyer_first")), 
				StringUtils.cleanInitialOrderName(params.get("buyer_middle")), 
				StringUtils.cleanInitialOrderName(params.get("buyer_last")));
		
		if(!name.isEmpty()) {
			name.getNameFlags().setOnlyThisSourceType(new NameSourceType(NameSourceType.ORDER));
			sa.getSearchPageManualBuyers().add(name);
			sa.getBuyers().add(name);
			if (parseProductType(product) == SearchAttributes.SEARCH_PROD_REFINANCE) {
				sa.getSearchPageManualOwners().add(name); //CR 3870
			}
		}
		
		name = new Name(
				StringUtils.cleanInitialOrderName(params.get("co_buyer_first")), 
				StringUtils.cleanInitialOrderName(params.get("co_buyer_middle")), 
				StringUtils.cleanInitialOrderName(params.get("co_buyer_last")));
		
		if(!name.isEmpty()) {
			name.getNameFlags().setOnlyThisSourceType(new NameSourceType(NameSourceType.ORDER));
			sa.getSearchPageManualBuyers().add(name);
			sa.getBuyers().add(name);
			if (parseProductType(product) == SearchAttributes.SEARCH_PROD_REFINANCE) {
				sa.getSearchPageManualOwners().add(name); //CR 3870
			}
		}
		
		return sa;
	}
	
	/**
     * 
     * @param prodName
     * @return
     */
	private int parseProductType(String prodName){
		int retVal = SearchAttributes.SEARCH_PROD_FULL;
		List<ProductsMapper> products = Products.getProductList();
		for (ProductsMapper prod : products) {
			if (prod.getAlias().equalsIgnoreCase(prodName)){
				retVal = prod.getProductId();
				break;
			}
		}
		return retVal;
	}
	
	public static void main(String[] args) {
		MailOrder mo = new MailOrder();
		Search search = null;
		try {
			//search = mo.getSearch("D:\\work\\Horton Order.PDF");
			//search = mo.getSearch("D:\\mailOrders\\ilcook.coloured2.txto");
			search = mo.getSearch("D:\\work\\COBroomfieldCity_autom_oldCOuntyName.txto");
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(search.getID());
	}

}
