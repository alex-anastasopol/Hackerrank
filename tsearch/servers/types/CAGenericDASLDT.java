package ro.cst.tsearch.servers.types;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.database.rowmapper.RequestCount;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.doctype.ExactDoctypeFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.functions.CAGenericDT;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.templates.AddDocsTemplates;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.XmlUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;
import com.stewart.ats.tsrindex.server.UploadImage;

/**
 * @author Cristian Stochina
 **/
public class CAGenericDASLDT extends TSServerDASLAdapter {

	private final String DT_FAKE_RESPONSE =  StringUtils.fileReadToString(FAKE_FOLDER+"CA_DT_DASLFakeResponse.xml");
	
	protected static final String DASL_TEMPLATE_CODE = "DASL_DT_CA";
	private static final ServerPersonalData pers = new ServerPersonalData();
	private static final Pattern PATTERN_START_HTML_TAG = Pattern.compile("(?is)<html[\\s>]");

	static {
		pers.setTemplatePrefixName(DASL_TEMPLATE_CODE);
		pers.addXPath(0, "//TitleDocument", ID_DETAILS);
		pers.addXPath(1, "//TitleSearchTaxReport", ID_DETAILS);
	}

	private static final long serialVersionUID = 4149962132981011252L;

	public CAGenericDASLDT(long searchId) {
		super(searchId);
	}
	
	public CAGenericDASLDT(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@Override
	protected ServerPersonalData getServerPersonalData() {
		return pers;
	}
	
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		if(module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX){
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			ServerResponse ret = null;
			
			if(!modules.isEmpty()) {
				ret = super.searchByMultipleInstrument(modules,sd, null);
			}else{
				ret = super.SearchBy(module, sd);
			} 
			return ret;
			
		} 
			
		if(module.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_MODULE_IDX){
			
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			List<String> fakeResponses = new ArrayList<String>();
			List<TSServerInfoModule> fakeModules = new ArrayList<TSServerInfoModule>();
			
			if(modules.isEmpty()) {
				modules.add(module);
			}
			
			for(TSServerInfoModule mod : modules ) {
				String folderName = getCrtSearchDir() + "Register" + File.separator;
				boolean created = new File(folderName).exists();
				if( !created ) created=new File(folderName).mkdirs();
				if( !created ){
					throw new RuntimeException("CAGenericDASLDT.SearchBy - could not create folder: "+folderName);
				}
				String book = mod.getParamValue( 0 );
				String page = mod.getParamValue( 1 );
				String docType =  mod.getParamValue( 2 ).replaceAll("\\s", "");
				String docno =  mod.getParamValue( 4 );
				String year = mod.getParamValue( 5 );
		    	
		    	
		    	boolean donotHaveBookPage = StringUtils.isEmpty(book)&& StringUtils.isEmpty(page);
		    	if((!"PLAT".equalsIgnoreCase(docType))){
		    		if( ((bookAndPageExists(book,page)&&!donotHaveBookPage))&&!this.isParentSite()){
		    			if(!"PARCELMAP".equalsIgnoreCase(docType)
				    			&& !"RECORDMAP".equalsIgnoreCase(docType)&& !"TRACTMAP".equalsIgnoreCase(docType)){
		    				if(modules.size() == 1) {
				    			ServerResponse sr = new ServerResponse();
								ParsedResponse pr = new ParsedResponse();
								sr.setParsedResponse(pr);
								sr.setResult("<b>&nbsp;&nbsp; Document already saved in TSR index</b>");
								pr.setResponse("<b>Document already saved in TSR index</b>");
								pr.setAttribute("ALREADY_IN_TSD", "ALREADY_IN_TSD");
								solveHtmlResponse(mod.getModuleIdx()+"", mod.getParserID(), "SearchBy", sr, sr.getResult());
								return sr;
		    				}
		    			}
		    		}
		    	} 
		    	
				DownloadImageResult res = CAGenericDASLDT.retrieveImage(book, page, docno, year, docType, new ImageLinkInPage(true), mSearch, msSiteRealPath);
				
				if(res.getStatus() ==  DownloadImageResult.Status.OK ){
					String grantor  = "County of "+InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
					grantor=grantor==null?"":grantor;
					String grantee = mSearch.getSa().getAtribute(SearchAttributes.LD_SUBDIV_NAME);
					grantee=grantee==null?"":grantee;
					SimpleDateFormat fr = new SimpleDateFormat("MM/dd/yyyy");
					
					grantee = StringUtils.HTMLEntityEncode(grantee);
					grantor = StringUtils.HTMLEntityEncode(grantor);
					
					if(!"PLAT".equals(docType)){
						grantee="";
						grantor="";
					}
					String doc = DT_FAKE_RESPONSE.replaceAll("@@Grantee@@", grantee);
					doc = doc.replaceAll("@@Grantor@@", grantor);
					doc = doc.replaceAll("@@Book@@", book==null?"":book);
					doc = doc.replaceAll("@@Page@@", page==null?"":page);
					doc = doc.replaceAll("@@DocNo@@", docno==null?"":docno);
					
					Calendar cal = Calendar.getInstance();
					int year1 = cal.get(Calendar.YEAR );
					try{
						year1 = Integer.parseInt(year);
					}catch(Exception e){}
					cal.set(Calendar.YEAR, year1);
					cal.set(Calendar.MONTH,Calendar.JANUARY);
					cal.set(Calendar.DATE, 1);
					
					doc = doc.replaceAll("@@Date@@", fr.format(cal.getTime()));
					doc = doc.replaceAll("@@Type@@", docType);
					
					fakeResponses.add(doc);
					fakeModules.add(getDefaultServerInfo().getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				}
			}

			if(!fakeModules.isEmpty()) {
				return searchByMultipleInstrument(fakeModules, sd, fakeResponses);
			}else{
				ServerResponse sr = new ServerResponse();
				ParsedResponse pr = new ParsedResponse();
				sr.setParsedResponse(pr);
				sr.setResult("<b>Could not download image</b>");
				solveHtmlResponse(module.getModuleIdx()+"", module.getParserID(), "SearchBy", sr, sr.getResult());
				return sr;
			}	
		}
	
		return super.SearchBy(module, sd);
	}
	
	/* Pretty prints a link that was already followed when creating TSRIndex
	 * (non-Javadoc)
	 * @see ro.cst.tsearch.servers.types.TSServer#getPrettyFollowedLink(java.lang.String)
	 */	
    public String getPrettyFollowedLink (String initialFollowedLnk)
    {
    	if (initialFollowedLnk.matches("(?i).*DL[_]{2,}([0-9]+)([a-z]*)[^a-z]*.*"))
    	{
		/*"Instrument 13676 which is a Court doc type has already been saved from a
		previous search in the log file."*/
    		String retStr = initialFollowedLnk.replaceFirst(
    				"(?i).*DL[_]{2,}([0-9]+)([a-z]*)[^a-z]*.*", 
    				"Instrument " + "$1" + " " + "$2" + 
    				" has already been processed from a previous search in the log file. ");
    		retStr =  "<br/><span class='followed'>"+retStr+"</span><br/>";
    		
    		return retStr;
    	}
    	
    	return "<br/><span class='followed'>Link already followed</span>:" + initialFollowedLnk + "<br/>";
    }
	
	// in California DT we have all names in last name field and we have also html raw file
	protected String modifyXMLResponse(String xml, int moduleIDX) {
		String ret = FLGenericDASLDT.updateXMLResponse( xml, moduleIDX );
		ret = ret.replaceAll("(?i)[<]\\s*lastName\\s*[>]", "<FullName>");
		ret = ret.replaceAll("(?i)[<]\\s*/\\s*lastName\\s*[>]", "</FullName>");
		ret = ret.replaceAll("(?i)[<]\\s*TitleRecord\\s*[>]\\s*[<]\\s*BaseStarterRecord\\s*[>]", "<TitleRecord>\n<TitleDocument>\n<BaseStarterRecord>");
		ret = ret.replaceAll("(?i)[<]\\s*/\\s*BaseStarterRecord\\s*[>]\\s*[<]\\s*/\\s*TitleRecord\\s*[>]", "</BaseStarterRecord>\n</TitleDocument>\n</TitleRecord>");
		int poz = ret.indexOf("<TitleSearchHTMLReport>");
		int pozEnd = ret.lastIndexOf("</TitleSearchHTMLReport>");
		
		if(poz>0 && pozEnd>0){
			pozEnd+="</TitleSearchHTMLReport>".length();
			String htmlresponse = ret.substring(poz,pozEnd);
			ret = ret.substring(0,poz)+ret.substring(pozEnd);
			StringBuilder build = new StringBuilder();
			poz = 0;
			pozEnd = 0;
			Matcher m = PATTERN_START_HTML_TAG.matcher(htmlresponse);
			while(  m.find() ) {
				poz = m.start();
				pozEnd = Math.max(htmlresponse.indexOf("</html>",pozEnd),htmlresponse.indexOf("</HTML>",pozEnd));
				if(pozEnd < 0)
					break;
				
				if(poz<pozEnd){
					build.append("<FRAME>");
					pozEnd+="</HTML>".length();
					String test = ro.cst.tsearch.utils.Tidy.tidyParse(htmlresponse.substring(poz,pozEnd).replaceAll("<TR[^<>]*<[^<>]*>", "<tr>"), null);
					if(    (test.contains("End Of Report") && test.contains("TAX ROLL")) 
						|| (test.contains("CURRENT TAXES THROUGH") && test.contains("END SEARCH")) ) {
						String taxDoc = test.replaceAll("bgcolor=\".*?\"", "")
											.replaceAll("class=\".*?\"", "")
											.replaceAll("COLOR:\"BLACK\";", "");
						mSearch.setAdditionalInfo("CA_RAW_LOG_TAX_DOCUMENT", taxDoc);
					}
					build.append(test);
					build.append("</FRAME>");
				}
			}
			SearchLogger.info(build.toString(), searchId);
		}
		return ret;
	}

	private static boolean isATSCounty(String countyName){
		return (
			"Tulare".equalsIgnoreCase(countyName.replaceAll("\\s", ""))||
			"SanBenito".equalsIgnoreCase(countyName.replaceAll("\\s", ""))||
			"Kings".equalsIgnoreCase(countyName.replaceAll("\\s", ""))||
			"Monterey".equalsIgnoreCase(countyName.replaceAll("\\s", ""))||
			"Stanislaus".equalsIgnoreCase(countyName.replaceAll("\\s", ""))
		);
	}
	
	private String calculateClientReferenceNo(){
		
		Search search =  InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		SearchAttributes sa = search.getSa();
    	String county= InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		
    	boolean isATS = isATSCounty(county);
		
    	// try community file no, then abstractor fileno
    	String orderNo = sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO);
    	if(StringUtils.isEmpty(orderNo)){
    		orderNo = sa.getAtribute(SearchAttributes.ORDERBY_FILENO);
    	}
    	
    	int poz = orderNo.indexOf('~');
    	if(poz>0 && poz<orderNo.length()-1){
    		orderNo = orderNo.substring(poz+1);
    	}
    	else if(poz>0){
    		orderNo = orderNo.substring(0,poz);
    	}
    	orderNo = (orderNo != null) ? orderNo : "";
    	
    	if( !( (isATS && matchATSConditions(orderNo)) || (!isATS && matchTeletitleConditions(orderNo)) ) ){
    		orderNo = searchId+"";
    	}
    	return orderNo;
		
	}
	
	private boolean matchTeletitleConditions(String orderNo) {
		/*
		NNNNNNNNNNNN 
        CCXXXXXXXXXX   - TWO LETTERS AND TEN PACKED CHARACTERS
                           THE LETTERS ARE A-Z, 0-9, BLANK, -, /, & AND #
                           THE PACKED ARE 0-9, BLANK, -, /, & AND # 
        N- Any single number
        C- Any single letter � see above for valid formats
        X- Any single packed character- see above for valid formats.*/
		return ( orderNo.length()>=2 && orderNo.length()<=12 && (orderNo.matches("[0-9]+")||orderNo.matches("[a-zA-Z][a-zA-Z][0-9 /&#-]*")) );
	}

	private boolean matchATSConditions(String orderNo) {
		return ( orderNo.length()>=2 && orderNo.length()<=15 && orderNo.matches("[0-9a-zA-Z]+") );
	}

	@Override
	protected HashMap<String, Object> fillTemplatesParameters(Map<String, String> params) {
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
		Search search = currentInstance.getCrtSearchContext();
		HashMap <String, Object> templateParams = super.fillTemplatesParameters(params);
		DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(
				currentInstance.getCommunityId(),
				miServerID);
		String stateAbrev = dat.getStateAbrev();
		String county = dat.getCountyName();
		
		String APN = params.get( "APN" );
		templateParams.put( AddDocsTemplates.DASLAPN, APN );
		String chain = params.get("chain");
		templateParams.put(AddDocsTemplates.DASLPropertyChainOption, chain);
		String includeTax = params.get("includeTax");
		templateParams.put(AddDocsTemplates.DASLIncludeTaxFlag, includeTax);
		
		String role1 = params.get("role1");
		templateParams.put(AddDocsTemplates.DASLPartyRole_1, role1);
		String role2 = params.get("role2");
		templateParams.put(AddDocsTemplates.DASLPartyRole_2, role2);
		
		String firstName1 = params.get("firstName1");
		if(firstName1 !=null){
			firstName1  = firstName1.replaceAll("[*]", "");
		}
		templateParams.put(AddDocsTemplates.DASLFirstName_1, firstName1 );
		String firstName2 = params.get("firstName2");
		templateParams.put(AddDocsTemplates.DASLFirstName_2, firstName2 );
		
		String middleName1 = params.get("middleName1");
		if(middleName1 !=null){
			middleName1  = middleName1.replaceAll("[*]", "");
		}
		templateParams.put(AddDocsTemplates.DASLMiddleName_1 , middleName1 );
		String middleName2 = params.get("middleName2");
		templateParams.put(AddDocsTemplates.DASLMiddleName_2 , middleName2 );
		
		String lastName1 = params.get("lastName1");
		if(lastName1 !=null){
			lastName1  =  lastName1.replaceAll("[*]", "");
		}
		templateParams.put(AddDocsTemplates.DASLLastName_1, lastName1 );
		
		String lastName2 = params.get("lastName2");
		templateParams.put(AddDocsTemplates.DASLLastName_2, lastName2 );
		
		String nickName = params.get("nickName");
		templateParams.put(AddDocsTemplates.DASLNickName, nickName );
		
		String withProperty = params.get("withProperty");
		templateParams.put(AddDocsTemplates.DASLWithProperty, withProperty );
		
		String soundIndex = params.get("sounddex");
		templateParams.put(AddDocsTemplates.DASLSoundIndex, soundIndex );
		
		String fromDate = params.get("fromDate");
		templateParams.put(AddDocsTemplates.DASLPropertySearchFromDate, fromDate );
		templateParams.put(AddDocsTemplates.DASLPartySearchFromDate, fromDate );
		
		String toDate = params.get("toDate");
		templateParams.put(AddDocsTemplates.DASLPropertySearchToDate, toDate  );
		templateParams.put(AddDocsTemplates.DASLPartySearchToDate, toDate  );
		
		String searchPropType = params.get("DASLPropertySearchType");
		templateParams.put(AddDocsTemplates.DASLPropertySearchType, searchPropType );
		
		String searchPartyType = params.get("DASLPartySearchType");
		templateParams.put(AddDocsTemplates.DASLPartySearchType, searchPartyType );
		
		String  DASLImageSearchType = params.get("DASLImageSearchType");
		templateParams.put(AddDocsTemplates.DASLImageSearchType , DASLImageSearchType );
		
		String lot = params.get("lot");
		templateParams.put(AddDocsTemplates.DASLLot,lot);

		String lotThrough = params.get("lotThrough");
		templateParams.put(AddDocsTemplates.DASLLotThrough,lotThrough);
		
		String building  = params.get("building");
		templateParams.put(AddDocsTemplates.DASLBuilding,building);
		
		String unit = params.get("unit");
		templateParams.put(AddDocsTemplates.DASLUnit,unit);
		
		String sublot = params.get("sublot");
		templateParams.put(AddDocsTemplates.DASLSubLot,sublot);
		
		String block = params.get("block");
		templateParams.put(AddDocsTemplates.DASLBlock,block);
		
		String platBook = params.get("platBook");
		templateParams.put(AddDocsTemplates.DASLPlatBook,platBook);
		
		String platPage = params.get("platPage");
		templateParams.put(AddDocsTemplates.DASLPlatPage, platPage);
		
		String platDocyear = params.get("platYear");
		templateParams.put(AddDocsTemplates.DASLPlatDocumentYear, platDocyear);
		
		String platName = params.get("platName");
		templateParams.put( AddDocsTemplates.DASLPlatName, platName );
		
		String book = params.get("book");
		templateParams.put(AddDocsTemplates.DASLBook,book);
		
		String page = params.get("page");
		templateParams.put(AddDocsTemplates.DASLPage, page);
		
		String docno = params.get("docno");
		templateParams.put(AddDocsTemplates.DASLDocumentNumber, docno);
		
	    String platDocNo = params.get("platDocNo");
		templateParams.put(AddDocsTemplates.DASLPlatDocumentNumber, platDocNo);
	    
	    String section = params.get("section");
	    templateParams.put(AddDocsTemplates.DASLSection,section);
	    
	    String township = params.get("township");
	    templateParams.put(AddDocsTemplates.DASLTownship,township);
	    
	    String range = params.get("range");
	    templateParams.put(AddDocsTemplates.DASLRange,range);
	    
	    String quarterOrder = params.get("quarterOrder");
	    templateParams.put(AddDocsTemplates.DASLQuarterOrder,quarterOrder);
	    
	    String quaterValue = params.get("quarterValue");
	    templateParams.put(AddDocsTemplates.DASLQuaterValue,quaterValue);
	    
	    String arb = params.get("arb");
	    templateParams.put(AddDocsTemplates.DASLARB,arb);
	    
	    String parcel = params.get("parcel");
	    templateParams.put(AddDocsTemplates.DASLParcel,parcel);
	    
	    String reference = ( (search.getParentSearchId() == Search.NO_UPDATED_SEARCH) ? calculateClientReferenceNo() : search.getParentSearchId() )+ "" ;
	    if( reference.length()>12 ){
	    	reference = reference .substring( reference.length()-12 ); 
	    }
	    
	    templateParams.put( AddDocsTemplates.DASLClientReference, reference );
	    templateParams.put( AddDocsTemplates.DASLClientTransactionReference, reference );
	    
	    String nr = params.get("number");
	    templateParams.put(AddDocsTemplates.DASLStreetNumber,nr);
	    
	    String name = params.get("name");
	    templateParams.put(AddDocsTemplates.DASLStreetName,name);
	    
	    String suffix = params.get("suffix");
	    templateParams.put(AddDocsTemplates.DASLStreetSuffix,suffix);
	    
	    String dir = params.get("direction");
	    templateParams.put(AddDocsTemplates.DASLStreetDirection,dir);
	    
	    templateParams.put(AddDocsTemplates.DASLStateAbbreviation,stateAbrev);
	    
	    templateParams.put(AddDocsTemplates.DASLCounty, county);
	    templateParams.put(AddDocsTemplates.DASL_B_P_H, params.get("DASL_B_P_H"));
	    templateParams.put(AddDocsTemplates.DASLPlatName, params.get("DASLPlatName"));
	    templateParams.put(AddDocsTemplates.DASLPlatLabel, params.get("DASLPlatLabel"));
	    
	    templateParams.put(AddDocsTemplates.DASLStateAbbreviation,stateAbrev);
	    
	    templateParams.put(AddDocsTemplates.DASLCountyFIPS, dat.getCountyFIPS());
		
	    templateParams.put(AddDocsTemplates.DASLStateFIPS, dat.getStateFIPS());
	    
	    templateParams.put(AddDocsTemplates.DASLYearFiled, params.get("year"));
	    
	    templateParams.put(AddDocsTemplates.DASLimageId, params.get("DASLimageId") );
	    
	    templateParams.put(AddDocsTemplates.DASLSSN4, params.get("SSN4") );
	    templateParams.put(AddDocsTemplates.DASLSearchType, params.get("DASLSearchType") );
	    templateParams.put(AddDocsTemplates.DASL_TRACT, params.get("DASL_TRACT") );
	    templateParams.put(AddDocsTemplates.DASL_LOT_THROUGH, params.get("DASL_LOT_THROUGH") );
	    templateParams.put(AddDocsTemplates.DASL_BLOCK_THROUGH, params.get("DASL_BLOCK_THROUGH") );
	    templateParams.put(AddDocsTemplates.DASL_FULL_STREET, params.get("DASL_FULL_STREET") );
	    templateParams.put(AddDocsTemplates.DASLOwnerFirstName, params.get("firstName") );
	    templateParams.put(AddDocsTemplates.DASLOwnerFullName, params.get("fullName") );
	    templateParams.put(AddDocsTemplates.DASLOwnerLastName, params.get("lastName") );
	    templateParams.put(AddDocsTemplates.DASLOwnerMiddleName, params.get("middleName") );
	    templateParams.put(AddDocsTemplates.DASLProviderId, 
	    		getProviderId( 
	    				params.get("DASLImageSearchType"),
	    				dat.getCountyFIPS(),
	    				dat.getCountyName(),
	    				params.get("DASLPartySearchType")));
	    
	    String titleOfficer = search.getSa().getAtribute(SearchAttributes.TITLE_UNIT);
	    if(!StringUtils.isEmpty(titleOfficer)&&titleOfficer.length()>=2){
	    	titleOfficer = titleOfficer.substring(titleOfficer.length()-2,titleOfficer.length());
	    	templateParams.put( AddDocsTemplates.DASLTitleOfficer, titleOfficer);
	    }
	    
	    templateParams.put(AddDocsTemplates.DASLClientId, SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericDTSite", "DaslClienId"));
	    
	    return templateParams;
	}
	
	protected String getProviderId(String DASLImageSearchType,String DASLCountyFIPS,String DASLCounty,String DASLPartySearchType){
		if("DT".equals(DASLImageSearchType)){
			return "83";
		}
		 else{
			 if("059".equals(DASLCountyFIPS)){
				return "61";
			 }
			 else{
				 if("Alameda".equals(DASLCounty)){
					 return "68";
				 }
				 else{
					 if("Los\u0020Angeles".equals(DASLCounty)){
						 if(DASLPartySearchType==null){
							 return "60";
						 }
						 else{
							 if(!"".equals(DASLPartySearchType)){
								 return "61";
							 }
							 else{
								 return "60";
							 }
						 }
					 }
					 else{
						 if("San\u0020Diego".equals(DASLCounty)){
							 return "69";
						 }
						 else{
							 if("San\u0020Mateo".equals(DASLCounty)){
								 return "74";
							 }
							 else{
								 if("Ventura".equals(DASLCounty)){
									 return "73";
								 }
								 else{
									 if("Imperial".equals(DASLCounty)){
										 return "75";
									 }
									 else{		
										 if("Riverside".equals(DASLCounty)){	
											 return "71";
										 }
										 else{							
											 if("Tulare".equals(DASLCounty)){
												 return "81";
											 }
											 else{
												 if("Kings".equals(DASLCounty)){
													 return "77";
												 }
												 else{
													 if("San\u0020Benito".equals(DASLCounty)){
														 return "79";
													 }
													 else{
														 if("San\u0020Bernardino".equals(DASLCounty)){
															 return "82";
														 }
														 else{
															 return "63";
														 }
													 }

											 }
										 }
									 }
								 }
							 }
						 }
					 }		
				 }
			 }
		 }
	}

}

	
	protected ArrayList<NameI>  addBuyerNameSearch( List<TSServerInfoModule> modules, TSServerInfo serverInfo, ArrayList<NameI> searchedNames, boolean ignoreMiddle ){
		FilterResponse docTypeBuyerFilter = DoctypeFilterFactory.getDoctypeBuyerFilter( searchId ) ;
		TSServerInfoModule module = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.NAME_MODULE_IDX ) );
		module.clearSaKeys();
		module.setSaObjKey( SearchAttributes.BUYER_OBJECT );
		
		module.setSaKey(0, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		
		FilterResponse nameFilter = NameFilterFactory.getDefaultNameFilter( SearchAttributes.BUYER_OBJECT, searchId, module );
		if(ignoreMiddle){
			((GenericNameFilter)nameFilter).setPondereMiddle( 0 );
		}
		else{
			((GenericNameFilter)nameFilter).setIgnoreMiddleOnEmpty(true);
		}
		module.addFilter( nameFilter );
		if( searchedNames!=null ) {
			nameFilter.setInitAgain( true );
		}
		
		module.addFilter( docTypeBuyerFilter );
		module.setIteratorType( 2, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
		ConfigurableNameIterator iterator = (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, true, new String[]{"L F;;"});
		if( searchedNames!=null ) {
			iterator.setInitAgain( true );
			iterator.setSearchedNames( searchedNames );
		}
		module.addIterator( iterator );
		modules.add( module ) ;
	
		return iterator.getSearchedNames();
	}
	
	protected ArrayList<NameI>  addOwnerNameSearch( List<TSServerInfoModule> modules, TSServerInfo serverInfo, ArrayList<NameI> searchedNames, boolean ignoreMiddle ){
				
		FilterResponse nameFilter = null ;
		TSServerInfoModule module = null ;
		ConfigurableNameIterator ownerIterator = null ;
	
		module = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.NAME_MODULE_IDX ) );
		module.clearSaKeys();
		module.setSaObjKey( SearchAttributes.OWNER_OBJECT );
		
		module.setSaKey(0, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		
		nameFilter = NameFilterFactory.getDefaultNameFilter( SearchAttributes.OWNER_OBJECT, searchId, module );
		if(ignoreMiddle){
			((GenericNameFilter)nameFilter).setPondereMiddle( 0 );
		}
		else{
			((GenericNameFilter)nameFilter).setIgnoreMiddleOnEmpty(true);
		}
		module.addFilter( nameFilter );
		if( searchedNames!=null ) {
			nameFilter.setInitAgain( true );
		}
		
		module.setIteratorType( 2, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
		module.setIteratorType( 7, FunctionStatesIterator.ITERATOR_TYPE_SSN);
		ownerIterator = (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, true , new String[]{"L F;;"}) ;
		if( searchedNames!=null ) {
			ownerIterator.setInitAgain( true );
			ownerIterator.setSearchedNames( searchedNames );
		}
		module.addIterator( ownerIterator );
		
		modules.add( module ) ;
		
		return ownerIterator.getSearchedNames();
	}

	
	
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
	   	ConfigurableNameIterator nameIterator = null;
	   	DocsValidator doctypeValidator = DoctypeFilterFactory.getDoctypeBuyerFilter(searchId).getValidator();

		
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule module;	
	    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

	    for (String id : gbm.getGbTransfers()) {
			  		   	    	 
	         module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	         module.setIndexInGB(id);
	         module.setTypeSearchGB("grantor");
	         module.clearSaKeys();
		     module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
			 module.setIteratorType( 2, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
			 module.setIteratorType( 7, FunctionStatesIterator.ITERATOR_TYPE_SSN);
		     module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
		     String date=gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
		     if (date!=null) 
		    	 module.getFunction(0).forceValue(date);
		     nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L F;;"} );
		 	 module.addIterator(nameIterator);
		 	 module.addValidator(doctypeValidator);
             module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
		  	 module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
		  	 module.addFilter( getRemoveDoctypesFilter() );
			 
		  	 modules.add(module);
		    
		     
		     if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
			     module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			     module.setIndexInGB(id);
			     module.setTypeSearchGB("grantee");
			     module.clearSaKeys();
				 module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				 module.setIteratorType( 2, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
				 module.setIteratorType( 7, FunctionStatesIterator.ITERATOR_TYPE_SSN);
				 module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				 date=gbm.getDateForSearchBrokenChain(id,"MM/dd/yyyy", searchId);
				 if (date!=null) 
					 module.getFunction(0).forceValue(date);
				 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L F;;"} );
				 module.addIterator(nameIterator);
				 module.addValidator(doctypeValidator);
			     module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
				 module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				 module.addFilter( getRemoveDoctypesFilter() );
	
				 modules.add(module);
			 
		     }

	    }	 
	    serverInfo.setModulesForGoBackOneLevelSearch(modules);	
	}
	
	private FilterResponse getRemoveDoctypesFilter(){
		ExactDoctypeFilterResponse filter = new ExactDoctypeFilterResponse(searchId);
		filter.addRejected("TRANSFER");
		filter.addRejected("MORTGAGE");
		filter.addRejected("ASSIGNMENT");
		filter.addRejected("APPOINTMENT");
		return filter;
	}
	
	protected String createLinkForImage(HashMap<String, String> value) {
		HashMap<String, String> map = (HashMap<String, String>) value;
		String book = map.get("book");
		String page = map.get("page");
		String docno = map.get("docno");
		String type = map.get("type");
		String year = map.get("year");
		
		if (type == null) {
			type = "";
		} else {
			type = DocumentTypes.getDocumentCategory(type, searchId);
		}

		StringBuilder build = new StringBuilder("");
		build.append(createPartialLink(TSConnectionURL.idDASL, TSServerInfo.IMG_MODULE_IDX));
		build.append("DASLIMAGE&");

		build.append("type");
		build.append("=");
		build.append(type);
		build.append("&");

		build.append("docno");
		build.append("=");
		build.append(docno);
		build.append("&");

		build.append("book");
		build.append("=");
		build.append(book);
		build.append("&");

		build.append("DASLImageSearchType");
		build.append("=");
		build.append("DT");
		build.append("&");

		build.append("page");
		build.append("=");
		build.append(page);
		build.append("&");

		build.append("year");
		build.append("=");
		build.append(year);
		
		return build.toString();
	}
	
	public static DownloadImageResult retrieveImage(String book, String page,String docNo, String year, String type,ImageLinkInPage ilip,Search mSearch, String msSiteRealPath){
	    
		ServerResponse res = new ServerResponse();
		String county = InstanceManager.getManager().getCurrentInstance(mSearch.getID()).getCurrentCounty().getName();
		 
	 	TSInterface server = TSServersFactory.GetServerInstance((int)TSServersFactory.getSiteId("CA", county, "DT"), "", "", mSearch.getID());
		TSServerInfoModule module = server.getCurrentClassServerInfo().getModuleForSearch(TSServerInfo.IMG_MODULE_IDX, new SearchDataWrapper());
		server.setServerForTsd(mSearch, msSiteRealPath);
		
		boolean hasBookpage = !(StringUtils.isEmpty(book) || StringUtils.isEmpty(page)) ;
		boolean hasDocNo 	= !StringUtils.isEmpty(docNo);
		
		if( hasBookpage ){
			module.setData( 2, book );
			module.setData( 3, page );
		}
		if( hasDocNo ){
			module.setData( 0, docNo );
		}
		
		module.setData( 4, type);
		module.setData( 5, year);
		
		try{
			res = ((TSServerDASL)server).searchBy(module, ilip , null);
		}
		catch(Exception e){  
			res.setImageResult(new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], ilip.getContentType() ));
		}	
		return res.getImageResult();
    }
	  
	  /**
		 * Save an image. Called only during TSR creation or OCR
		 */
		@Override
		protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {
			try{
				String link = image.getLink();
				//for old searches saved with previous version
				link = link.replace("param_155_5", "year").replace("param_155_2", "book") 
						   .replace("param_155_3", "page").replace("param_155_0", "docno")
						   .replace("param_155_4", "type");
				
				HashMap<String, String> map = HttpUtils.getParamsFromLink( link );
				
				String book 	 = map.get( "book" ) ;
				String page 	 = map.get( "page" ) ;
				String docNumber = map.get( "docno");
				String year 	 = map.get( "year" ) ;
				String type 	 = map.get( "type" ) ;	
				
				return  retrieveImage(book, page, docNumber, year, type, image, mSearch, msSiteRealPath);
					
			}
			catch(Exception e){
				return new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() );
			}
		}

		@Override
		public void addDocumentAdditionalProcessing(DocumentI doc, ServerResponse response) {
			try {
				if(doc.is(DType.TAX) && !doc.hasImage()) {
					
					String taxIndex = (String) mSearch.getAdditionalInfo("CA_RAW_LOG_TAX_DOCUMENT");
	        		if(taxIndex == null) return ;
	        		
					SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd");
		    		Date sdate =mSearch.getStartDate();
		    		    		
		    		String basePath = ServerConfig.getImageDirectory()+File.separator+format.format(sdate)+File.separator+searchId;
		    		File file= new File(basePath);
		    		if(!file.exists()){
		    			file.mkdirs();
		    		}
		    			    		
		        	String tiffFileName = doc.getId()+".tiff";
		        	String htmlFileName = doc.getId()+".htm";
		        	String fullHtmlFileName = basePath+File.separator+htmlFileName;
	
		        	File f = new File(fullHtmlFileName);
	        		f.createNewFile(); 
		        	FileUtils.writeTextFile(fullHtmlFileName, taxIndex);
		        	UploadImage.createTempTIFF(fullHtmlFileName, basePath, null);
		        	f.delete();
		        	
		        	
		        	String path 	= basePath+File.separator+tiffFileName;
		        	UploadImage.updateImage(doc, path, tiffFileName, "tiff", searchId);
		        	
		        	Set<String> links = new HashSet<String>();
		        	links.add(path);
		        	doc.getImage().setLinks( links );
		        	doc.setIncludeImage(true);
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void addDocumentAdditionalPostProcessing(DocumentI doc, ServerResponse response) {
			if(doc.is(DType.TAX) && doc.hasImage()) {
				doc.setIncludeImage(true);
			}
		}

		@Override
		public void specificParseLegalDescription(Document doc, ParsedResponse item, ResultMap resultMap) {
		}
		
		@Override
		public void specificParseGrantorGrantee(Document doc, ParsedResponse item, ResultMap resultMap) {
			try {
				NodeList partyInfo = XmlUtils.xpathQuery(doc, "/TitleDocument/Instrument/PartyInfo");
				
				ArrayList<List> bodyGtor = new ArrayList<List>();
				ArrayList<List> bodyGtee = new ArrayList<List>();

				for (int i = 0; i < partyInfo.getLength(); i++) {
					if(partyInfo.item(i) != null ) {
						String[] names = { "", "", "", "", "", "" };
						
						String partyRole = XmlUtils.findNodeValue(partyInfo.item(i), "PartyRole");
						String vestingType = XmlUtils.findNodeValue(partyInfo.item(i), "VestingType");
						String firstName = XmlUtils.findNodeValue(partyInfo.item(i), "Party/FirstName");
						String midName = XmlUtils.findNodeValue(partyInfo.item(i), "Party/MiddleName");
						String lastName = XmlUtils.findNodeValue(partyInfo.item(i), "Party/LastName");
						String fullName = XmlUtils.findNodeValue(partyInfo.item(i), "Party/FullName");
						
						firstName = firstName.replaceAll("\\bDECD\\b", "").replaceAll("\\bIND\\s+EXEC\\b", "").replaceAll("\\s+MRS\\b", "");
						midName = midName.replaceAll("\\bDECD\\b", "").replaceAll("\\bIND\\s+EXEC\\b", "").replaceAll("\\s+MRS\\b", "")
																	.replaceAll("(?is)\\A\\s*-\\s*$", "");
						lastName = lastName.replaceAll("\\bDECD\\b", "");
						
						if (i < partyInfo.getLength() - 1){
							String partyRoleNext = XmlUtils.findNodeValue(partyInfo.item(i+1), "PartyRole");
							if (partyRole.toLowerCase().equals(partyRoleNext.toLowerCase())){
								String firstNameNext = XmlUtils.findNodeValue(partyInfo.item(i+1), "Party/FirstName");
								String midNameNext = XmlUtils.findNodeValue(partyInfo.item(i+1), "Party/MiddleName");
								String lastNameNext = XmlUtils.findNodeValue(partyInfo.item(i+1), "Party/LastName");
								String fullNameNext = XmlUtils.findNodeValue(partyInfo.item(i+1), "Party/FullName");
								if (StringUtils.isEmpty(lastNameNext) && StringUtils.isEmpty(midNameNext) && StringUtils.isNotEmpty(firstNameNext) 
										&& StringUtils.isEmpty(fullNameNext)){
									if (firstNameNext.matches("\\A(?:ESTATE )?(?:OF|ETAL?|TRUSTEE)\\s*")){
										firstNameNext = firstNameNext.replaceAll("(?is)\\bETA\\b$", "ETAL");
										midName += " " + firstNameNext;
									} else {
										midName += " & " + firstNameNext;
									}
								} else if (lastNameNext.length() == 1  && StringUtils.isEmpty(midNameNext) && StringUtils.isNotEmpty(firstNameNext)){
									midName += " & " + firstNameNext + " " + lastNameNext;
								}
							}
						}
						
						if (StringUtils.isEmpty(lastName) && StringUtils.isEmpty(midName) && StringUtils.isNotEmpty(firstName) 
								&& firstName.matches("\\A(?:ESTATE )?(?:OF|ETAL?|TRUSTEE|\\bPR\\b)\\s*")){
							continue;
						}
						if (StringUtils.isNotEmpty(lastName) && StringUtils.isEmpty(midName) && StringUtils.isEmpty(firstName) 
								&& lastName.matches("\\A(?:ESTATE )?(?:OF|ETAL?|TRUSTEE|\\bPR\\b)\\s*")){
							continue;
						}
						if (StringUtils.isNotEmpty(lastName) && StringUtils.isEmpty(midName) && StringUtils.isNotEmpty(firstName) 
								&& firstName.matches("\\A(?:ETAL)\\s*")){
							continue;
						}
						
						String[] parts = fullName.split("###");
						if (parts.length > 1){
							for (String part:parts){
								names = StringFormats.parseNameDesotoRO(part, true);
								parseNames(part, names, partyRole, vestingType, bodyGtor, bodyGtee);
							}
						} else {		
							if (StringUtils.isNotEmpty(fullName)){
								names = StringFormats.parseNameNashville(fullName, true);
								parseNames(fullName, names, partyRole, vestingType, bodyGtor, bodyGtee);
								
							} else {
								String name = (lastName + " " + firstName + " " + midName).trim();
								names = StringFormats.parseNameNashville(name, true);
								parseNames(name, names, partyRole, vestingType, bodyGtor, bodyGtee);
							}
						}
					}
				}
				ResultTable rtGtor = new ResultTable();
				ResultTable rtGtee = new ResultTable();
				
				rtGtor = GenericFunctions.storeOwnerInSet(bodyGtor, true);
				resultMap.put("GrantorSet", rtGtor);
				rtGtee = GenericFunctions2.storeOwnerInSet(bodyGtee, true);
				resultMap.put("GranteeSet", rtGtee);
				
				CAGenericDT.fixGrantorGranteeSetDT(resultMap, searchId);
				GenericFunctions2.setGrantorGranteeDT(resultMap, searchId);
				
				try {
					GenericFunctions1.setGranteeLanderTrustee2(resultMap, searchId);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				
			} catch (Exception e) {
				logger.error("Exception in parsing of names in CAGenericDASLDT " + searchId, e);
			}
		}
		
		@SuppressWarnings("rawtypes")
		public void parseNames(String allName, String[] names, String partyRole, String vestingType, ArrayList<List> bodyGtor, ArrayList<List> bodyGtee) {
			
			String[] suffixes, type, otherType;
			
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			if (("Grantor".equalsIgnoreCase(partyRole) || "Party1".equalsIgnoreCase(vestingType)) 
					&& (!"Grantee".equalsIgnoreCase(partyRole) || !"Party2".equalsIgnoreCase(vestingType))){
				GenericFunctions.addOwnerNames(allName, names, suffixes[0], suffixes[1], type, otherType, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), bodyGtor);
			} else if ("Grantee".equalsIgnoreCase(partyRole) || "Party2".equalsIgnoreCase(vestingType)){
				GenericFunctions.addOwnerNames(allName, names, suffixes[0], suffixes[1], type, otherType, 
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), bodyGtee);
			}
			
		}
		
		@Override
	    protected String CreateSaveToTSDFormHeader(int action, String method) {
	    	String s = "<form name=\"SaveToTSD\" id=\"SaveToTSD\" action= \"" + msRequestSolverName + "\"" + " method=\"" + method + "\" > "
	                + "<input type=\"hidden\" name=\"dispatcher\" value=\""+ action + "\">"
	                + "<input type=\"hidden\" name=\"ServerID\" value=\"" + msServerID + "\">" 
	                + "<input type=\"hidden\" name=\"" + RequestParams.SEARCH_ID + "\" value=\"" + mSearch.getSearchID() + "\"> "
	                + "<input type=\"hidden\" name=\"" + RequestParams.PARENT_SITE_SAVE_TYPE + "\" id=\"" + RequestParams.PARENT_SITE_SAVE_TYPE + "\" " +
	                	"value=\"" + RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITH_CROSSREF + "\">";
	    	return s;
	    }
		
		@Override
		protected String CreateSaveToTSDFormEnd(String name, int parserId,
				int numberOfUnsavedRows) {
			if (name == null) {
	            name = SAVE_DOCUMENT_BUTTON_LABEL;
	        }
	    	        
	        String s = "";
	        
	    	if (numberOfUnsavedRows < 0 || numberOfUnsavedRows > 0) {
	        	s = "<input  type=\"checkbox\" checked title=\"Save selected document(s) with cross-references\" " +
	        		" onclick=\"javascript: if(document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE + "'))\r\n " +
	        		" if(this.checked) { " +
		        	" document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE + "').value='" + 
		        			RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITH_CROSSREF +
		        	"' } else { " +
	 	        	" document.getElementById('" + RequestParams.PARENT_SITE_SAVE_TYPE + "').value='" + 
	 	        			RequestParamsValues.PARENT_SITE_SAVE_TYPE_WITHOUT_CROSSREF +
		        	"' } \"> Save with cross-references<br>\r\n" +
		        	"<input type=\"checkbox\" name=\"" + RequestParams.PARENT_SITE_FOR_UPDATE_SEARCH_PARAMS + 
		        			"\" id=\"" + RequestParams.PARENT_SITE_FOR_UPDATE_SEARCH_PARAMS + 
		        			"\" title=\"Save search parameters from selected document(s) for further use\" > Save with search parameters<br>\r\n" + 
	        		"<input  type=\"button\" class=\"button\" name=\"Button\" value=\"" + name + "\" " +"onclick=\"javascript:submitForm();\" >\r\n";
	    	}
	        
	        
	        return s+"</form>\n";
		}
		
		/*
		
		@Override
		protected ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(
				ServerResponse response, String htmlContent, boolean forceOverritten) {

			ADD_DOCUMENT_RESULT_TYPES result = super.addDocumentInATS(response, htmlContent, forceOverritten);
			
			try {
				if(response.isParentSiteSearch()) {
					Object possibleModule = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_INDEX_SOURCE);
					Object possibleFlagForSavingForUpload = response.getParsedResponse().getAttribute(RequestParams.PARENT_SITE_FOR_UPDATE_SEARCH_PARAMS);
					if(Boolean.TRUE.equals(possibleFlagForSavingForUpload) && 
							possibleModule != null && possibleModule instanceof String) {
						String originalSourceModule = (String)possibleModule;
						if(Integer.toString(TSServerInfo.SUBDIVISION_MODULE_IDX).equals(originalSourceModule) || 
								Integer.toString(TSServerInfo.ADDRESS_MODULE_IDX).equals(originalSourceModule)) {
							
							RegisterDocumentI registerDocumentI = (RegisterDocumentI) response.getParsedResponse().getDocument();
							for (PropertyI propertyI : registerDocumentI.getProperties()) {
								//if (Integer.toString(TSServerInfo.ADDRESS_MODULE_IDX).equals(originalSourceModule)) {
								//	getSearchAttributes().addForUpdateSearchAddress(propertyI.getAddress());
								//} else {
									getSearchAttributes().addForUpdateSearchLegal(propertyI.getLegal(), getServerID());
									SearchLogger.info("<br>Saving legal from document " + registerDocumentI.prettyPrint() + " for future automatic search<br>", searchId);
								//}
							}
							
						} else if(Integer.toString(TSServerInfo.NAME_MODULE_IDX).equals(originalSourceModule)) {
							RegisterDocumentI registerDocumentI = (RegisterDocumentI) response.getParsedResponse().getDocument();
							getSearchAttributes().addForUpdateSearchGranteeNames(Arrays.asList(registerDocumentI.getGrantee().getNames().toArray(new NameI[0])), getServerID());
							
							try {
								Object obj = getSearch().getAdditionalInfo("Grantor_FIX_for" + registerDocumentI.getId());
								if (obj != null && obj instanceof Set<?>) {
									Set<?> names = (Set<?>) obj;
									getSearchAttributes().addForUpdateSearchGrantorNames(Arrays.asList(names.toArray(new NameI[0])), getServerID());
								} else {
									getSearchAttributes().addForUpdateSearchGrantorNames(Arrays.asList(registerDocumentI.getGrantor().getNames().toArray(new NameI[0])), getServerID());
								}
								
								
							} catch (Exception e) {
								logger.error("Internal error while getting grantors", e);
							}
							
							
							
							SearchLogger.info("<br>Saving names from document " + registerDocumentI.prettyPrint() + " for future automatic search<br>", searchId);
						}
					}
				}
			} catch (Exception e) {
				logger.error("Error while saving data for Update", e);
			}
			return result;
		}

		*/
		
		@Override
		public void specificParseAddress(Document doc, ParsedResponse item,
				ResultMap resultMap) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		protected Object getRequestCountType(int moduleIDX) {
			switch (moduleIDX) {
			case TSServerInfo.ARB_MODULE_IDX:
			case TSServerInfo.GENERIC_MODULE_IDX:
			case TSServerInfo.CONDOMIN_MODULE_IDX:
			case TSServerInfo.SERIAL_ID_MODULE_IDX:
			case TSServerInfo.SECTION_LAND_MODULE_IDX:
				return RequestCount.TYPE_LEGAL_COUNT;
			case TSServerInfo.NAME_MODULE_IDX:
				return RequestCount.TYPE_NAME_COUNT;
			case TSServerInfo.ADDRESS_MODULE_IDX:
				return RequestCount.TYPE_ADDR_COUNT;
			case TSServerInfo.PARCEL_ID_MODULE_IDX:
				return RequestCount.TYPE_PIN_COUNT;
			case TSServerInfo.IMG_MODULE_IDX:
				return RequestCount.TYPE_IMAGE_COUNT;
			case TSServerInfo.BOOK_AND_PAGE_MODULE_IDX:
				return RequestCount.TYPE_INSTRUMENT_COUNT;
			}
			
			try{
				throw new Exception("Bad module Id for counting request on " + getDataSite().getSTCounty());
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return null;
		}
}
