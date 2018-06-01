package ro.cst.tsearch.servers.types;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.connection.dasl.DaslConnectionSiteInterface;
import ro.cst.tsearch.connection.dasl.DaslConnectionSiteInterface.DaslResponse;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.NameSet;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.templates.AddDocsTemplates;
import ro.cst.tsearch.templates.InstrumentStruct;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;


/**
 * @author Cristian Stochina 
 */
public class GenericDASLRV extends TSServerDASLAdapter implements TSServerROLikeI {

	private static String PARCEL_ID = TSServerInfo.PARCEL_ID_MODULE_IDX+"";
	private static final String RV_FAKE_RESPONSE = StringUtils.fileReadToString(FAKE_FOLDER+"DASLFakeResponse.xml");
	private static final long serialVersionUID = 176327600973256L;
	
	private static final String DASL_TEMPLATE_CODE = "DASL_RV";
	private static final ServerPersonalData pers = new ServerPersonalData();
	static{
		pers.setTemplatePrefixName(DASL_TEMPLATE_CODE);
		pers.addXPath( 0 , "//PropertySummary/Property", ID_SEARCH_BY_NAME );
		pers.addXPath( 0 , "//TitleRecord/TitleDocument", ID_DETAILS );
	}
	
	private static Set<String> usePinFromNDB = new HashSet<String>();
	static {
		usePinFromNDB.add( "FLBrowardRV" );
		usePinFromNDB.add( "FLFranklinRV" );
		usePinFromNDB.add( "FLSumterRV" );
	}

	public GenericDASLRV(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
		mSearch = currentInstance.getCrtSearchContext();
		DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(
				currentInstance.getCurrentCommunity().getID().intValue(), 
				mid);
		String p1 = dat.getIndex();
		String p2 = dat.getP2();
		msServerID = "p1="+p1+"&p2="+p2;
	}
	
	public GenericDASLRV(long searchId) {
		super(searchId);
		mSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		msServerID = "p1=076&p2=19";
	}
	
	@Override
	protected DaslResponse  performImageSearch(String book, String page, String docno, String year, String month,String day, String type1, String isPlat, String DASLImageSearchType, int moduleIDX){
		String xmlQuery = "";
		HashMap<String, String> params = new HashMap<String, String>();
		DaslResponse daslResponse = null;
		
		ArrayList<String> types = new ArrayList<String>(2);
		types.add(type1);
		
		for(String type:types){
			//search with B P and type and year  
			if((!StringUtils.isEmpty(book)&&!StringUtils.isEmpty(page))){
				params.put("book", book);
				params.put("page", page);
				params.put("type", type);
				params.put("DASLImageSearchType", DASLImageSearchType);
				params.put("DASLSearchType", "IMG");
				if(!StringUtils.isEmpty(year)){
					params.put("year", year);
				}
				xmlQuery = buildSearchQuery(params, moduleIDX);
				if ( StringUtils.isEmpty(xmlQuery ) ) {
					return null;
				}
				
				daslResponse = getDaslSite().performSearch( xmlQuery, searchId );
			}
			
			if( 	daslResponse==null || daslResponse.xmlResponse==null ||
					daslResponse.xmlResponse.indexOf("<Content>")<0 
					|| daslResponse.status == DaslConnectionSiteInterface.ORDER_ERROR 
					|| daslResponse.status == DaslConnectionSiteInterface.ORDER_PLACED
					|| daslResponse.status == DaslConnectionSiteInterface.ORDER_REJECTED ){
				//search with Doc NO and type and year  
				params = new HashMap<String, String>();
				if(!StringUtils.isEmpty(docno)){
					params.put("type", type);
					params.put("docno", docno);
					params.put("DASLImageSearchType", DASLImageSearchType);
					params.put("DASLSearchType", "IMG");
					if(!StringUtils.isEmpty(year)){
						params.put("year", year);
					}
					
					// create XML query
					xmlQuery = buildSearchQuery(params, moduleIDX);
					if ( StringUtils.isEmpty(xmlQuery ) ) {
						return null;
					}
					daslResponse = getDaslSite().performSearch( xmlQuery, searchId );	
			
				}
			}
			
			if( 	!(daslResponse==null || daslResponse.xmlResponse.indexOf("<Content>")<0 
					|| daslResponse.status == DaslConnectionSiteInterface.ORDER_ERROR 
					|| daslResponse.status == DaslConnectionSiteInterface.ORDER_PLACED
					|| daslResponse.status == DaslConnectionSiteInterface.ORDER_REJECTED) ){
				break;
			}
		}
		
		
		return daslResponse;
	}
	
/* Pretty prints a link that was already followed when creating TSRIndex
 * (non-Javadoc)
 * @see ro.cst.tsearch.servers.types.TSServer#getPrettyFollowedLink(java.lang.String)
 */	
    public String getPrettyFollowedLink (String initialFollowedLnk)
    {
    	if (initialFollowedLnk.matches("(?i).*DL[_]{2,}([0-9]+)[_]{1,}([0-9]+)[^a-z]*([a-z]+).*")){
/*"Book 13676 Page 1504 which is a Court doc type has already been saved from a
previous search in the log file."*/
    		String retStr = initialFollowedLnk.replaceFirst(
    				"(?i).*DL[_]{2,}([0-9]+)[_]{1,}([0-9]+)[^a-z]*([a-z]*)[^a-z]*.*", 
    				"Book " + "$1" + " Page " + "$2" + " " + "$3" + 
    				" has already been processed from a previous search in the log file. ");
    		retStr =  "<br/><span class='followed'>"+retStr+"</span><br/>";
    		
    		return retStr;
    	}
    	return "<br/><span class='followed'>Link already followed</span>:" + initialFollowedLnk + "<br/>";
    }
	
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
		Search search = currentInstance.getCrtSearchContext();
		if(!this.isParentSite() && module.getModuleIdx()==TSServerInfo.NAME_MODULE_IDX
			/* locate property search can be address or name search*/){
			DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(
					currentInstance.getCurrentCommunity().getID().intValue(), 
					miServerID);
			String siteName =  dat.getName();
			Boolean makeAddressSearch = (Boolean)search.getAdditionalInfo(siteName+":"+"MAKE_ADDRESS_SEARCH");
			if(makeAddressSearch == null) {
				makeAddressSearch = true;
			}
			if(!makeAddressSearch){
				return new ServerResponse();
			}
		}else if( TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX == module.getModuleIdx()) {
			
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			if(!modules.isEmpty()) {
				return super.searchByMultipleInstrument(modules,sd, null);
			}
			
			if("plat".equalsIgnoreCase(module.getFunction(6).getParamValue())){
				TSServerInfoModule m = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX) );
				m.forceValue(0, module.getFunction(0).getParamValue());
				m.forceValue(1, module.getFunction(1).getParamValue());
				m.setParamValue( 2, "PLAT" );
				return SearchBy(m, sd);
			}
		
		}
		else if(module.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_MODULE_IDX){
			

			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			List<String> fakeResponses = new ArrayList<String>();
			List<TSServerInfoModule> fakeModules = new ArrayList<TSServerInfoModule>();
			
			if(modules.isEmpty()) {
				modules.add(module);
			}
			String platBook = getSearchAttribute(SearchAttributes.LD_BOOKNO_1);
			String platPage = getSearchAttribute(SearchAttributes.LD_PAGENO_1);
			
			for(TSServerInfoModule mod : modules ) {
				
				String folderName = getCrtSearchDir() + "Register" + File.separator;
				new File(folderName).mkdirs();
				String book = mod.getParamValue( 0 );
				String page = mod.getParamValue( 1 );
				String docType = mod.getParamValue( 2 );
				String docNo = mod.getParamValue(4);
		    	String fileName = folderName + book+"_"+page+docType + ".tiff";
		    	
		    	if(verifyModule(mod)){
		    	
			    	boolean isAutomaticPlatSearch = ( "PLAT_FAKE_BOOK".equals(book) && "PLAT_FAKE_PAGE".equals(page) );
			    	
			    	if(isAutomaticPlatSearch){
			    		String pBook = "";
						String pPage = "";
			    		DocumentsManagerI m = mSearch.getDocManager();
						try{
			    			m.getAccess();
			    			List<RegisterDocumentI> list = m.getRoLikeDocumentList();
			    			if(list.size()>0){
			    				DocumentUtils.sortDocuments(list, MultilineElementsMap.DATE_ORDER_DESC);
				    			for(RegisterDocumentI t:list){
				    				if(t.isOneOf("TRANSFER", "MORTGAGE")){
					    				for(PropertyI p:t.getProperties()){
					    					if(p.hasSubdividedLegal()){
					    						pBook = p.getLegal().getSubdivision().getPlatBook();
					    						pPage = p.getLegal().getSubdivision().getPlatPage();
					    						if(!StringUtils.isEmpty(pBook)&&!StringUtils.isEmpty(pPage)){
					    							break;
					    						}
					    					}
					    				}
				    				}
				    			}
			    			}
			    		}
			    		finally{
			    			m.releaseAccess();
			    		}
						if( (!StringUtils.isEmpty(pBook)&&!StringUtils.isEmpty(pPage)) && (!(pBook.equals(platBook)&&pPage.equals(platPage)))	){			
							mod.setData( 0, pBook );
							mod.setData( 1, pPage );
							return SearchBy(mod,sd);
						}
			    	}
			    	
			    	if(isAutomaticPlatSearch||!"PLAT".equalsIgnoreCase(docType)){
			    		if(isAutomaticPlatSearch||bookAndPageExists(book,page)){
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
			    	
					boolean imageDownloaded = GenericDASLRV.retrieveImage(book,page, docNo, docType, fileName, mSearch, msSiteRealPath, true);
					
					if(imageDownloaded ){
						String grantor  = "County of "+InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
						grantor=grantor==null?"":grantor;
						String grantee = mSearch.getSa().getAtribute(SearchAttributes.LD_SUBDIV_NAME);
						grantee=grantee==null?"":grantee;
						
						grantee = StringUtils.HTMLEntityEncode(grantee);
						grantor = StringUtils.HTMLEntityEncode(grantor);
						
						if(!"PLAT".equals(docType)){
							grantee="";
							grantor="";
						}
						String doc = RV_FAKE_RESPONSE.replaceAll("@@Grantee@@", grantee);
						doc = doc.replaceAll("@@Grantor@@", grantor);
						doc = doc.replaceAll("@@Book@@", book==null?"":book);
						doc = doc.replaceAll("@@Page@@", page==null?"":page);
						doc = doc.replaceAll("@@DocNo@@", docNo==null?"":docNo);
						doc = doc.replaceAll("@@Date@@", "01/01/1960");
						doc = doc.replaceAll("@@Type@@", docType);
						
						fakeResponses.add(doc);
						fakeModules.add(getDefaultServerInfo().getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
						
						if(modules.size() > 1) {
							SearchLogger.info("Found 1 result", searchId);
						}
					}else {
						if(modules.size() > 1) {
							SearchLogger.info("Found 0 results", searchId);
						}
					}
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
				
		} else if( module.getModuleIdx() == TSServerInfo.PARCEL_ID_MODULE_IDX ){
			preparePin( module );
		}
	
		return super.SearchBy(module, sd);
	}
	
	protected boolean verifyModule(TSServerInfoModule mod) {
		if (mod == null)
    		return false;
    	
    	if (mod.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_MODULE_IDX || mod.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX) {
			if (mod.getFunctionCount()>2 && 
					((StringUtils.isNotEmpty(mod.getFunction(0).getParamValue()) && StringUtils.isNotEmpty(mod.getFunction(1).getParamValue())) || 
					StringUtils.isNotEmpty(mod.getFunction(2).getParamValue()))) {
				return true;
			} 
			return false;
		}
    	
    	if(mod.getModuleIdx() == TSServerInfo.PARCEL_ID_MODULE_IDX){
    		if (mod.getFunctionCount()>0 && StringUtils.isNotEmpty(mod.getFunction(0).getParamValue()))
    			return true;
    		else 
    			return false;
    	}
    	
		System.err.println(this.getClass() + " : I shouldn't be here!!!");
		return false;
	}
	
	@SuppressWarnings("unchecked")
	protected void ParseResponse(String moduleIdx, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		ParsedResponse pr = Response.getParsedResponse();
		
		if( isIntermediaryResult(viParseID) ){
					
			Vector<ParsedResponse> newResultRows = new Vector<ParsedResponse>();
			Set<String> docs = new HashSet<String>();
	    	// parse all records
            for ( ParsedResponse item: ( Vector<ParsedResponse> ) pr.getResultRows() ) {  
            	item.setParentSite(pr.isParentSite());
            	// parse
            	String result[] = parse(item,viParseID);
            	String itemHtml = result[0];
            	String shortType = result[1];
            	item.setResponse(itemHtml);
            	//            	 get parsed XML document
            	Node doc = (Node) item.getAttribute(ParsedResponse.DASL_RECORD);
            	// determine instrument number - skip row if it has none
            	String instrNo;
            	try{
            		instrNo = getInstrFromXML(doc, null);            		
            	}catch(RuntimeException e){
            		logger.warn(searchId + ": Document from dasl RV has NO Instrument number. It has been skipped!");           		
            		continue;
            	}
            	// do not add the document twice
            	if( "".equals(instrNo) || docs.contains(instrNo) ){
            		continue;
            	}
            	docs.add(instrNo);
            	// add row
            	newResultRows.add(item);
                itemHtml =	"<tr> <td valign=\"center\" align=\"center\"> </td> <td align=\"center\"><b> "+shortType+"</b></td> <td>" + itemHtml + "</td> <tr>";   
               	
                Matcher mat =patFinalDocumentLink.matcher(itemHtml);
                if(mat.find()){
                	String value = mat.group(1);
                	itemHtml = itemHtml.replaceAll(LINK_TO_FINAL_DOCUMENT_REGEX,  createLinkForFinalDocument(value));
				}
                parser.Parse(item, itemHtml,Parser.ONE_ROW, getLinkPrefix(TSConnectionURL.idDASL), TSServer.REQUEST_SAVE_TO_TSD);
            }
            // set the result rows - does not contain instruments without instr no
            pr.setResultRows(newResultRows);         
            // set proper header and footer for parent site search
            if (mSearch.getSearchType() == Search.PARENT_SITE_SEARCH) {
            	String header = pr.getHeader();
               	String footer = pr.getFooter();                           	
            	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\"><tr bgcolor=\"#cccccc\"> <th width=\"1%\"></th><th width=\"1%\">Type</th><th width=\"98%\" align=\"left\">Document</th></tr>";
            	footer = "\n</table>";
            	pr.setHeader(header);
            	pr.setFooter(footer);
            }
            return;    
		}
		if(moduleIdx .equals(PARCEL_ID)&&!isParentSite()){
			Vector vec = pr.getResultRows();
			if(vec!=null){
				if(pr.getResultRows().size()>0){
					DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(
							InstanceManager.getManager().getCommunityId(searchId),
							miServerID);
					String siteName =  dat.getName();
					Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
					search.setAdditionalInfo(siteName+":"+"MAKE_ADDRESS_SEARCH", Boolean.FALSE);
				}
			}
		}
		
	    super.ParseResponse(moduleIdx, Response, viParseID);
	}

	@Override
	protected HashMap<String, Object> fillTemplatesParameters(Map<String, String> params) {
		CurrentInstance currentInstance =  InstanceManager.getManager().getCurrentInstance(searchId);
		Search search = currentInstance.getCrtSearchContext();
		HashMap<String, Object>  templateParams = super.fillTemplatesParameters(params);
		String APN = params.get("APN");
		DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(
				currentInstance.getCurrentCommunity().getID().intValue(), 
				miServerID);
		
		templateParams.put(AddDocsTemplates.DASLAPN, APN);
		String subdivision = params.get("subdivision");
		templateParams.put( AddDocsTemplates.DASLSubdivision,subdivision);
		String includeTax = params.get("includeTax");
		templateParams.put(AddDocsTemplates.DASLIncludeTaxFlag, includeTax);
		
		String DASLSearchType = params.get("DASLSearchType");
		templateParams.put(AddDocsTemplates.DASLSearchType,DASLSearchType);
		
		/*String isPlat = params.get("type");
		if("PLAT".equalsIgnoreCase(isPlat)||"PLAT".equalsIgnoreCase(DocumentTypes.getDocumentCategory(isPlat,searchId))){
			isPlat = "Yes";
		}
		else{
			isPlat = "No";
		}*/
		
		String isPlat = params.get("isPlat");
		
		if(StringUtils.isEmpty(isPlat)){
			isPlat = "No";
		}
		
		if("IMG".equalsIgnoreCase(DASLSearchType)&&"PLAT".equalsIgnoreCase(params.get("type"))){
			isPlat = "Yes";
		}
		
		templateParams.put(AddDocsTemplates.DASLIsPlat,isPlat);
		
		String book = params.get("book");
		templateParams.put(AddDocsTemplates.DASLBook,book);
		
		String page = params.get("page");
		templateParams.put(AddDocsTemplates.DASLPage,page);
		
		String docno = params.get("docno");
		templateParams.put(AddDocsTemplates.DASLDocumentNumber,docno);
		
		String firstName = params.get("firstName");
		templateParams.put(AddDocsTemplates.DASLFirstName, firstName );
		String firstName1 = params.get("firstName1");
		templateParams.put(AddDocsTemplates.DASLFirstName_1, firstName1 );
		String firstName2 = params.get("firstName2");
		if(firstName2!=null){
			firstName2  = firstName2.replaceAll("[*]", "");
		}
		templateParams.put(AddDocsTemplates.DASLFirstName_2, firstName2 );
		String firstName3 = params.get("firstName3");
		templateParams.put(AddDocsTemplates.DASLFirstName_3, firstName3 );
		String firstName4 = params.get("firstName4");
		templateParams.put(AddDocsTemplates.DASLFirstName_4, firstName4 );
		String firstName5 = params.get("firstName5");
		templateParams.put(AddDocsTemplates.DASLFirstName_5, firstName5 );
		String firstName6 = params.get("firstName6");
		templateParams.put(AddDocsTemplates.DASLFirstName_6, firstName6 );
		
		String middleName = params.get("middleName");
		templateParams.put(AddDocsTemplates.DASLMiddleName, middleName );
		
		String middleName1 = params.get("middleName1");
		templateParams.put(AddDocsTemplates.DASLMiddleName_1 , middleName1 );
		String middleName2 = params.get("middleName2");
		if(middleName2 !=null){
			middleName2  = middleName2 .replaceAll("[*]","");
		}
		templateParams.put(AddDocsTemplates.DASLMiddleName_2 , middleName2 );
		String middleName3 = params.get("middleName3");
		templateParams.put(AddDocsTemplates.DASLMiddleName_3 , middleName3 );
		String middleName4 = params.get( "middleName4" );
		templateParams.put(AddDocsTemplates.DASLMiddleName_4 , middleName4 );
		String middleName5 = params.get( "middleName5" );
		templateParams.put(AddDocsTemplates.DASLMiddleName_5 , middleName5 );
		String middleName6 = params.get( "middleName6" );
		templateParams.put(AddDocsTemplates.DASLMiddleName_6 , middleName6 );
		
		String lastName = params.get("lastName");
		templateParams.put(AddDocsTemplates.DASLLastName, lastName);
		String lastName1 = params.get("lastName1");
		templateParams.put(AddDocsTemplates.DASLLastName_1, lastName1 );
		String lastName2 = params.get("lastName2");
		if(lastName2 !=null){
			lastName2  = lastName2 .replaceAll("[*]","");
		}
		templateParams.put(AddDocsTemplates.DASLLastName_2, lastName2 );
		String lastName3 = params.get("lastName3");
		templateParams.put(AddDocsTemplates.DASLLastName_3, lastName3 );
		String lastName4 = params.get("lastName4");
		templateParams.put(AddDocsTemplates.DASLLastName_4, lastName4 );
		String lastName5 = params.get("lastName5");
		templateParams.put(AddDocsTemplates.DASLLastName_5, lastName5 );
		String lastName6 = params.get("lastName6");
		templateParams.put(AddDocsTemplates.DASLLastName_6 ,lastName6 );
		
		String streetNumber = params.get("streetNumber");
		templateParams.put(AddDocsTemplates.DASLStreetNumber, streetNumber );
		String streetName = params.get("streetName");
		templateParams.put(AddDocsTemplates.DASLStreetName, streetName);
		String streetDir = params.get("streetDir");
		templateParams.put(AddDocsTemplates.DASLStreetDirection, streetDir);
		String streetSuffix = params.get("streetSuffix");
		templateParams.put(AddDocsTemplates.DASLStreetSuffix, streetSuffix );
		
		String soundIndex = params.get("soundex");
		templateParams.put(AddDocsTemplates.DASLSoundIndex, soundIndex );
		
		String fromDate = params.get("fromDate");
		templateParams.put(AddDocsTemplates.DASLPropertySearchFromDate, fromDate );
		
		String toDate = params.get("toDate");
		templateParams.put(AddDocsTemplates.DASLPropertySearchToDate, toDate );
		
		templateParams.put(AddDocsTemplates.DASLCountyFIPS, dat.getCountyFIPS());
		templateParams.put(AddDocsTemplates.DASLStateFIPS, dat.getStateFIPS());
		
		String reference = search.getOrderNumber();
    	int commId = search.getCommId();
    	String stateAbv = InstanceManager.getManager().getCurrentInstance(search.getSearchID()).getCurrentState().getStateAbv();
    	
    	if (((commId == 3 || commId == 4 || commId ==10)&&stateAbv.equals("FL"))){
    		reference="test";
    	}

	    templateParams.put(AddDocsTemplates.DASLClientReference,reference);
	    templateParams.put(AddDocsTemplates.DASLClientTransactionReference,reference);
	    templateParams.put(AddDocsTemplates.DASLClientId, SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), "GenericRVSite", "DaslClienId"));
	    templateParams.put(AddDocsTemplates.DASLIndexOnly,params.get("indexOnly"));
	    
		return templateParams ;
	}

	protected ServerPersonalData getServerPersonalData() {
		return pers;
	}
	
	protected  String createLinkForImage(HashMap<String,String> value){
		 HashMap<String, String> map = (HashMap<String, String>)value;
		 String book = map .get("book");
		 String page = map.get("page");
		 String docno = map.get("docno");
		 String type = map.get("type");
		 if(type ==null){
			 type ="";
		 }
		 
		 TSServerInfoModule imgModule = getDefaultServerInfoWrapper().getModule(TSServerInfo.IMG_MODULE_IDX);
		 
		 StringBuilder build = new StringBuilder("");//<a href=\"
		 build .append(createPartialLink(TSConnectionURL.idDASL,TSServerInfo.IMG_MODULE_IDX));
		 build .append("DASLIMAGE&");
		 
		 build .append( imgModule.getParamAlias( 4 ) ); /*type*/
		 build .append("=");
		 build .append(type);
		 build .append("&");
		 
		 build .append( imgModule.getParamAlias( 0 ) ); /*book*/
		 build .append("=");
		 build .append(book);
		 build .append("&");
		 
		 build .append( imgModule.getParamAlias( 1 ) ); /*page*/
		 build .append("=");
		 build .append(page);
		 build .append("&");
		 
		 build .append( imgModule.getParamAlias( 2 ) ); /*docno*/
		 build .append("=");
		 build .append(docno);
		 return build.toString();
	}
	 
	 private String createLinkForFinalDocument(String value){
		 StringBuilder build = new StringBuilder();
		 build .append(createPartialLink(TSConnectionURL.idDASL,TSServerInfo.PARCEL_ID_MODULE_IDX));
		 build .append(DASLFINAL+"&");
		 build .append(getDefaultServerInfo().getModule(TSServerInfo.PARCEL_ID_MODULE_IDX).getParamAlias(0)); /*APN*/
		 build .append("=");
		 build .append(value);
		 build.append("&");
	     build.append("DASLSearchType=");
	     build.append("APN");
		 build.append("&");
	     build.append("dispatcher=");
	     build.append(TSServerInfo.PARCEL_ID_MODULE_IDX);
	     
	     StringBuilder link = new StringBuilder("<a ");
	     link.append("style='cursor: pointer; cursor: hand;' onclick=\"document.location='");
	     link.append(build);
	     link.append("';return false;\">");
		 link.append(value);
		 link.append("</a>");
		 return link.toString();
	 }
	 
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		
	  	ConfigurableNameIterator nameIterator = null;
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule module;	
	    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
	    FilterResponse doctypeFilter = DoctypeFilterFactory.getDoctypeBuyerFilter(searchId);

  for (String id : gbm.getGbTransfers()) {
			  		   	    	 
	         module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
	         module.setIndexInGB(id);
	         module.setTypeSearchGB("grantor");
	         module.clearSaKeys();
		     module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
		     String date=gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
		     module.getFunction(18).forceValue("85");
		     if (date!=null) 
		    	 module.getFunction(20).forceValue(date);
		    module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			module.addFilter( NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
			module.addFilter( doctypeFilter );
	    	module.addValidator( DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator() );
	    	module.setIteratorType( 3, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
			module.setIteratorType( 4, FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE );
			module.setIteratorType( 5, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
			//module.setIteratorType( 18, FunctionStatesIterator.ITERATOR_TYPE_SCORE );
	    	nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;M" } );
		 	module.addIterator(nameIterator);
		 	modules.add(module);
		    
		     
		     if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
		    	 module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			     module.setIndexInGB(id);
			     module.setTypeSearchGB("grantee");
			     module.clearSaKeys();
				 module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				 date=gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
				 module.getFunction(18).forceValue("85");
				 if (date!=null) 
					 module.getFunction(20).forceValue(date);
				 module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				 module.addFilter( NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
				 module.addFilter( doctypeFilter );
			     module.addValidator( DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator() );
			     module.setIteratorType( 3, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
				 module.setIteratorType( 4, FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE );
				 module.setIteratorType( 5, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
				// module.setIteratorType( 18, FunctionStatesIterator.ITERATOR_TYPE_SCORE );
			     nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;F;M" } );
				 module.addIterator(nameIterator);			
				 modules.add(module);
			 
		     }

	    }	 
  serverInfo.setModulesForGoBackOneLevelSearch(modules);
		
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
		SearchAttributes sa = currentInstance.getCrtSearchContext().getSa();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null ;
		
		FilterResponse nameFilterOwner 	= getDefaultNameFilter( SearchAttributes.OWNER_OBJECT, searchId, null );
		((GenericNameFilter)nameFilterOwner).setInitAgain(true);
		((GenericNameFilter)nameFilterOwner).setIgnoreMiddleOnEmpty(true);
		//6132 ((GenericNameFilter)nameFilterOwner).setIgnoreEmptyMiddleOnCandidat(false);
		FilterResponse nameFilterBuyer 	= getDefaultNameFilter( SearchAttributes.BUYER_OBJECT, searchId, null );
		((GenericNameFilter)nameFilterBuyer).setIgnoreMiddleOnEmpty(true);
		((GenericNameFilter)nameFilterBuyer).setIgnoreEmptyMiddleOnCandidat(false);
		FilterResponse legalFilter 		= LegalFilterFactory.getDefaultLegalFilter( searchId );
		FilterResponse addressFilter 	= AddressFilterFactory.getAddressHighPassFilter( searchId, 0.8d );
		
		if( mSearch == null ){
			mSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		}
		String fromDateStr = sa.getFromDateString("MM/dd/yyyy");
		
		addPinSearch(serverInfo, modules);
		
		// P2: search by address
		String strNo = sa.getAtribute(SearchAttributes.P_STREETNO);
		String strName = sa.getAtribute(SearchAttributes.P_STREETNAME);		
		if( !StringUtils.isEmpty(strNo) && !StringUtils.isEmpty(strName) ){
			module = new TSServerInfoModule ( serverInfo.getModule( TSServerInfo.NAME_MODULE_IDX ) );
			module.clearSaKeys();
			module.setSearchType(DocumentI.SearchType.AD.toString());
			module.addFilter( AddressFilterFactory.getAddressHybridFilter( searchId, 0.8d ) );
			module.setData( 24, strNo );
			if( !StringUtils.isEmpty( fromDateStr ) ){
				module.setData( 2, fromDateStr );
			}
			module.setData( 26, strName );
			module.addValidator( legalFilter.getValidator() );
			addBetweenDateTest(module, false, false, false);
			modules.add(module);				
		}
		
		//search by name with subdivision name
		String subdivisionName =  sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
		if( searchWithSubdivision() ){
			module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX) );
			module.clearSaKeys();
			module.setSearchType(DocumentI.SearchType.LD.toString());
			module.setData( 5, subdivisionName );
			module.setData( 18, "85" );
			module.addFilter( DoctypeFilterFactory.getDoctypeSubdivisionIsGranteeFilter( searchId ) );		
			module.addFilter( new GrantorGranteeSubdivisionNameFilter(searchId));
			module.addFilter( legalFilter );
			module.addFilter( addressFilter );
			addBetweenDateTest(module, false, false, false);
			modules.add(module);
		} else {
			printSubdivisionException();
		}
		  
		//search by owner
		FilterResponse[] filtersO 	= { nameFilterOwner, legalFilter, addressFilter,  new LastTransferDateFilter(searchId) };
		ArrayList<NameI> searchedNames = addNameSearch( filtersO, modules, serverInfo, SearchAttributes.OWNER_OBJECT, null );
		
		//search by buyer
		//FilterResponse[] filtersB = { nameFilterBuyer, DoctypeFilterFactory.getDoctypeBuyerFilter(searchId) };
		//addNameSearch( filtersB, modules, serverInfo, SearchAttributes.BUYER_OBJECT, searchedNames);
		
		if( !isUpdate() ){ 
			//search after plat
			String platBook = sa.getAtribute(SearchAttributes.LD_BOOKNO_1);
			String platPage = sa.getAtribute(SearchAttributes.LD_PAGENO_1);
			if( !StringUtils.isEmpty(platBook) && !StringUtils.isEmpty(platPage) ){
				addPlatBookPageSearch( platBook, platPage, "PLAT", modules, serverInfo,mSearch);
			}
			
			addPlatBookPageSearch( "PLAT_FAKE_BOOK", "PLAT_FAKE_PAGE", "PLAT", modules, serverInfo,mSearch);
			
			// search by book and page extracted from AO
			addAoLookUpSearchesBookPageAtDocNo(serverInfo, modules, getAllAoReferences(mSearch), searchId, isUpdate());
		}
		
        //OCR last transfer - book / page search
        module = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX ) );
        module.clearSaKeys();
        module.setIteratorType( ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER );
		module.setIteratorType( 0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH );
		module.setIteratorType( 1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH );
		module.setIteratorType( 6, FunctionStatesIterator.ITERATOR_TYPE_DOCTYPE_SEARCH );
		modules.add(module);	
		
		addNameSearch( filtersO, modules, serverInfo,SearchAttributes.OWNER_OBJECT, searchedNames==null?new ArrayList<NameI>():searchedNames );
		
		serverInfo.setModulesForAutoSearch( modules );	
	}

	public void addPinSearch(TSServerInfo serverInfo, List<TSServerInfoModule> modules) {
		SearchAttributes sa = getSearchAttributes();
		String pin = sa.getAtribute( SearchAttributes.LD_PARCELNO2 );
		if( usePinFromNDB.contains( getDataSite().getName() ) ){
			 pin = sa.getAtribute( SearchAttributes.LD_PARCELNONDB );
		}
		
		// we do not have PIN in parcel 2 and not from NDB then try to get it from search page
		if( StringUtils.isEmpty(pin) ){
			pin = sa.getAtribute(SearchAttributes.LD_PARCELNO);
			IndividualLogger.info("Using LD_PARCELNO on RV - check to see if the value could have been read when parsing TR", searchId);
		}
		
		addPinSearch( pin, modules, serverInfo, mSearch );
		
		//for counties like FL Polk where you can't decide always the good PIN
		String pinalt = sa.getAtribute( SearchAttributes.LD_PARCELNO2_ALTERNATE );	//used for Polk now	
		if( !pin.equals( pinalt ) ){
			addPinSearch( pinalt, modules, serverInfo, mSearch );		
		}
	}
	
	private static void addPlatBookPageSearch(String book,String page,String type,List<TSServerInfoModule> modules, TSServerInfo serverInfo,Search mSearch){
		TSServerInfoModule module = new TSServerInfoModule( serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX) );
		module.clearSaKeys();
		module.setData( 0, book );
		module.setData( 1, page );
		module.setData( 2, type );
		if( !(Products.isOneOfUpdateProductType(mSearch.getSearchProduct())) ){
			modules.add(module);
		}
	}
	
	protected void addPinSearch(String pin,List<TSServerInfoModule> modules,TSServerInfo serverInfo,Search mSearch){
		if( !StringUtils.isEmpty(pin) ){
			TSServerInfoModule module = new TSServerInfoModule ( serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX) ) ;
			module.clearSaKeys();
			module.setData( 0, pin );  
			addBetweenDateTest(module, false, false, false);
			modules.add(module);	
		}
	}
	
	private ArrayList<NameI>  addNameSearch( FilterResponse[] filters, List<TSServerInfoModule> modules, TSServerInfo serverInfo,String key, ArrayList<NameI> searchedNames ) {
		ConfigurableNameIterator nameIterator = null;
		
		TSServerInfoModule module = new TSServerInfoModule(serverInfo
				.getModule(TSServerInfo.GENERIC_MODULE_IDX));
		module.clearSaKeys();
		module.setSaObjKey(key);
		for (int i = 0; i < filters.length; i++) {
			module.addFilter(filters[i]);
		}
		addFilterForUpdate(module, true);
		
		module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE );
		module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE );
		module.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE );
		module.setIteratorType(18, FunctionStatesIterator.ITERATOR_TYPE_SCORE );
		module.setSaKey(20, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		module.setIteratorType(20, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		
		nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] { "L;F;M" });
		nameIterator.setScoreForCommonNameSearch( 95 );
		nameIterator.setScoreForNameSearch( 90 );
		
		nameIterator.setAllowMcnPersons( true );
		if ( searchedNames!=null ) {
			nameIterator.setInitAgain( true );
			nameIterator.setSearchedNames( searchedNames );
		}
		
		searchedNames = nameIterator.getSearchedNames() ;
		
		module.addIterator( nameIterator );
		modules.add( module );
	
		return searchedNames;
	}
	
	/**
     * Save an image. Called only during TSR creation
     */
    @Override
    protected DownloadImageResult saveImage(ImageLinkInPage image)  throws ServerResponseException{
    	int functionBook = 0;
    	int functionPage = 1;
    	int functionDocNo = 2;
    	int fuctionDASLSearchType = 3;
    	int functionType = 4;
    	
    	String link = image.getLink();
    	TSServerInfo info = getDefaultServerInfo();
    	TSServerInfoModule module = info.getModule(TSServerInfo.IMG_MODULE_IDX);
    	String bookAlias = module.getParamAlias(functionBook);
    	String pageAlias = module.getParamAlias(functionPage);
    	String docNumberAlias = module.getParamAlias(functionDocNo);
    	String typeAlias = module.getParamAlias(functionType);
    	String book = link.replaceAll(".*"+bookAlias+"=([^&]*)", "$1");
        String page  = link.replaceAll(".*"+pageAlias+"=([^&]*)", "$1");
    	String docNumber =  link.replaceAll(".*"+docNumberAlias+"=([^&]*)", "$1");
    	String type =  link.replaceAll(".*"+typeAlias+"=([^&]*)", "$1");
        
    	int poz = book.indexOf("&");
    	if(poz>0){
    		book = book.substring(0,poz);
    	}else{
    		if(book.contains("&")){
    			book="";
    		}
    		
    	}
    	poz = page.indexOf("&");
    	if(poz>0){
    		page = page.substring(0,poz);
    	}else{
    		if (page.contains("&")){
    			page="";
    		}
    	}
    	poz = docNumber.indexOf("&");
    	if(poz>0){
    		docNumber = docNumber.substring(0,poz);
    	}else{
    		if (docNumber.contains("&")){
    			docNumber="";
    		}
    	}
    	poz = type.indexOf("&");
    	if(poz>0){
    		type = type.substring(0,poz);
    	}else{
    		if (type.contains("&")){
    			type="";
    		}
    	}
    	module.setParamValue( functionBook, book );
    	module.setParamValue( functionPage, page );
    	module.setParamValue( functionDocNo,docNumber );
    	module.setParamValue( fuctionDASLSearchType, "IMG" );
    	module.setParamValue( functionType, type);
    
    	String imageName = image.getPath();
    	if(FileUtils.existPath(imageName)){
    		byte b[] = FileUtils.readBinaryFile(imageName);
    		return new DownloadImageResult( DownloadImageResult.Status.OK, b, image.getContentType() );
    	}
    	
    	ServerResponse response = searchBy( module, image, null );
    	DownloadImageResult res = response.getImageResult();
    	return res;
    }
    
    @Override
	protected int changeParserIdBasedOnXMLResponse(String xml, int moduleIDX,int oldParserId) {
    	/* address or name search that returns final  documents*/
    	if( moduleIDX == TSServerInfo.NAME_MODULE_IDX && xml.indexOf("TitleDocument")>0){
    		return ID_DETAILS;
    	}
    	return oldParserId;
	}  
    
    /* protected boolean downloadImageFromOtherSite(String instrument,String book, String page, String year, String type, ImageLinkInPage image) {
    	String state = InstanceManager.getManager().getCurrentInstance(mSearch.getID()).getCurrentState().getStateAbv();
    	String stateSever = state+"RV";
    	DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(miServerID);
    	String stateFIPS = dat.getStateFIPS();
    	String countyFips = dat.getCountyFIPS();
    	
    	String query = "FIPS="+stateFIPS+countyFips+",";
    	
    	
    	if( !StringUtils.isEmpty(book)&& !StringUtils.isEmpty(page)){
    		if(!"PLAT".equals(type)){
    			query += "Type=Rec,SubType=All,Book="+book+","+"Page="+page;
    		}
    		else{
    			query += "Type=Map,SubType=All,Book="+book+","+"Page="+page;
    		}
    		boolean ret = PropertyInsightImageRetriever.retrieveImage(stateSever, query, image.getPath());
    		if(ret){
    			System.err.println("Image(searchId="+mSearch.getID()+" )book="+book+"page="+page+"inst="+instrument+" was taken from PropertyInsight");
    		}
    		return ret;
    	}
    	else if(!StringUtils.isEmpty(instrument)){
    		if(!"PLAT".equals(type)){
    			query += "Type=Rec,SubType=All,Inst="+instrument;
    		}
    		else{
    			query += "Type=Map,SubType=Assessor,Inst="+instrument;
    		}
    		boolean ret = PropertyInsightImageRetriever.retrieveImage(stateSever, query, image.getPath());
    		if(ret){
    			System.err.println("Image(searchId="+mSearch.getID()+" )book="+book+"page="+page+"inst="+instrument+" was taken from PropertyInsight");
    		}
    		return ret;
    	}
    	return false;
    }*/
    
    public static boolean retrieveImage(String book, String page, String docNo, String type, String fileName,Search mSearch,String msSiteRealPath, boolean justImageLookUp){
    	String county = InstanceManager.getManager().getCurrentInstance(mSearch.getID()).getCurrentCounty().getName();
    	//    	do not retrieve the image twice
    	if(FileUtils.existPath(fileName)){
    		return true;
    	}
    	 
	 	TSInterface server = TSServersFactory.GetServerInstance((int)TSServersFactory.getSiteId("FL", county, "RV"), "", "", mSearch.getID());
		
		TSServerInfoModule module = server.getCurrentClassServerInfo().getModuleForSearch(TSServerInfo.IMG_MODULE_IDX, new SearchDataWrapper());
	
		ServerResponse res = null;
		
		server.setServerForTsd(mSearch, msSiteRealPath);
		if(!(StringUtils.isEmpty(book)||StringUtils.isEmpty(page))){
			module.setData( 0, book );
			module.setData( 1, page );
			module.setData( 4, type );
			try{
				res = ((TSServerDASL)server).searchBy(module, new ImageLinkInPage(justImageLookUp),null);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
		if(res!=null){
			DownloadImageResult dres = res.getImageResult();
			if(dres .getStatus() == DownloadImageResult.Status.OK){
				FileUtils.writeByteArrayToFile(dres.getImageContent(), fileName);
				return true;
			}
		}
		
		module = server.getCurrentClassServerInfo().getModuleForSearch(TSServerInfo.IMG_MODULE_IDX, new SearchDataWrapper());
		server.setServerForTsd(mSearch, msSiteRealPath);
		
		if(!StringUtils.isEmpty(docNo)){
			module.forceValue(2,docNo);
			try{
				res = ((TSServerDASL)server).searchBy(module, new ImageLinkInPage(justImageLookUp),null);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		if(res!=null){
			DownloadImageResult dres = res.getImageResult();
			if(dres .getStatus() == DownloadImageResult.Status.OK){
				FileUtils.writeByteArrayToFile(dres.getImageContent(), fileName);
				return true;
			}
		}
		
		return false;
    }
    
	
    private static FilterResponse getDefaultNameFilter(String saKey,long searchId,TSServerInfoModule module){
    	GenericNameFilterRV fr = new GenericNameFilterRV( saKey, searchId, false, module , true);
		fr.setThreshold(new BigDecimal(NameFilterFactory.NAME_FILTER_THRESHOLD));
		return fr;
	}
    
	private static class GenericNameFilterRV extends GenericNameFilter{
		
		private static final long serialVersionUID = -7004090373364435487L;
		
		public GenericNameFilterRV(String key, long searchId,boolean useSubdivisionName,TSServerInfoModule module, boolean ignoreSuffix){
			super(key, searchId, useSubdivisionName, module, ignoreSuffix);
		}
		
		@SuppressWarnings("unchecked")
		public void computeScores(Vector rows){
			for (int i = 0; i < rows.size(); i++){
	            IndividualLogger.info("Processing result " + i + " of total " + rows.size(),searchId);
				ParsedResponse row = (ParsedResponse)rows.get(i);
				BigDecimal score = null;
				if(rows.size() == 1 && isSkipUnique()){
					score = ATSDecimalNumberFormat.ONE;
				} else if(rows.size() > getMinRowsToActivate()){
					score = getScoreOneRow(row);
				} else {
					score = ATSDecimalNumberFormat.ONE;
				}
				
				if(score.compareTo(threshold)>=0){
					RegisterDocumentI doc = (RegisterDocumentI)row.getDocument();
					if(doc.getGrantee().size()==0 && doc.getGrantor().size()==0){
						for(int j=0;j<rows.size(); j++){
							if(j!=i){
								RegisterDocumentI docJ = (RegisterDocumentI)((ParsedResponse)rows.get(j)).getDocument();
								if(doc.flexibleEquals(docJ,true)){
									score = ATSDecimalNumberFormat.ZERO;
									break;
								}
							}
						}
					}
				}
				
				scores.put(row.getResponse(), score);
				if (score.compareTo(bestScore) > 0){
					bestScore = score;
				}
				IndividualLogger.info("ROW SCORE:" + score,searchId);
				logger.debug("\n\n ROW SCORE : [" + score + "]\nROW HTML: [" + row.getResponse() + "]\n");
			}
		}
		
		@Override
		public String getFilterCriteria(){
			if(isUseSubdivisionNameAsCandidat()){
				return "Subdiv Name '"+ getReferenceNameString() + "'";
			}
	    	return "Name='" + getReferenceNameString() + "'" + ((getPondereMiddle()==0.0)?"(ignoring middle name)":""+" (rejecting empty duplicates)");
	    }
		
		@Override
	    public String getFilterName(){
			if(isUseSubdivisionNameAsCandidat()){
				return "Filter by Subdiv Name";
			}
	    	return "Filter rejecting empty duplicates " + ((getPondereMiddle()==0.0)?"(and ignoring middle name)":"") + "by Name";
	    }
	}
	
	
	private void preparePin(TSServerInfoModule module){
		String pin = module.getFunction(0).getParamValue();
		if( "FLFranklinRV".equals( getCrtTSServerName(miServerID) )){
		    // Turn SS-TTT-RRR-xxxx-xxxx-xxxx  into SSTTTRRRxxxxxxxxxxxx  
			Pattern pattern = Pattern.compile("([A-Z0-9]{2})-?([A-Z0-9]{2})[A-Z0-9]-?([A-Z0-9]{2})[A-Z0-9]-?([A-Z0-9]{4})-?([A-Z0-9]{4})-?([A-Z0-9]{4})");
			Matcher matcher = pattern.matcher(pin);
			if(matcher.find()){
				pin = matcher.group(1) + matcher.group(2) + matcher.group(3) + matcher.group(4) + matcher.group(5) + matcher.group(6);						  
			}
		}else if("FLMartinRV".equals( getCrtTSServerName(miServerID) )){
			pin = pin.replaceAll("[.]+","");
			if(pin.length()>20){
				pin = pin.substring(0, pin.length()-4);
			}
		}else if("FLHendryRV".equals( getCrtTSServerName(miServerID) )){
			pin = pin.replaceAll("[.-]+","");
		}
		module.forceValue(0, pin);
	}
	
	private class GrantorGranteeSubdivisionNameFilter extends FilterResponse {

		private static final long serialVersionUID = -8006746080077811460L;

		private transient Set<InstrumentStruct> analized = new HashSet<InstrumentStruct>();
		
		public GrantorGranteeSubdivisionNameFilter(long searchId) {
			super(searchId);
			setThreshold(new BigDecimal(NameFilterFactory.NAME_FILTER_THRESHOLD_FOR_GRANTEE_IS_SUBDIVISION));		
		}

		public void computeScores(Vector rows) {
			analized.clear();
			super.computeScores(rows);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public BigDecimal getScoreOneRow(ParsedResponse row) {
		
			Vector<SaleDataSet> v = (Vector<SaleDataSet>)row.getSaleDataSet();
			InstrumentStruct str = null;
			
			if( v!=null ){
				if( v.size()>0 ){
					SaleDataSet set = v.get(0);
					str = new InstrumentStruct();
					str.book = set.getAtribute("Book");
					str.page = set.getAtribute("Page");
					str.instNo = set.getAtribute("InstrumentNumber");
					if ( analized.contains(str)  ) {
						return BigDecimal.valueOf(0.0d);
					}
				}
			}
			
			if( str!=null ){
				analized.add(str);
			}

			// check that we do have a reference
			String refSub = getSearchAttribute(SearchAttributes.LD_SUBDIV_NAME);
			if(StringUtils.isEmpty(refSub)){
				return ATSDecimalNumberFormat.ONE;
			}
			refSub = refSub.trim().toUpperCase();
			refSub = refSub.replaceAll("\\s{2,}", " ");
			refSub = cleanSub(refSub);
			
			// extract all candidate names
			Set<String> candSubs = new LinkedHashSet<String>();
			for(Vector<NameSet> names: new Vector[]{ row.getGranteeNameSet(), row.getGrantorNameSet()}){
				if(names == null){
					continue;
				}
				for(NameSet name: names){
					for(String prefix: new String[] {"Owner", "Spouse"}){
						String last = name.getAtribute(prefix + "LastName");
						String first = name.getAtribute(prefix + "FirstName");
						String middle = name.getAtribute(prefix + "MiddleName");
						String fullName = last + " " + first + " " + middle;
						fullName = fullName.trim().toUpperCase();
						fullName = fullName.replaceAll("\\s{2,}", " ");
						fullName = cleanSub(fullName);
						if(!StringUtils.isEmpty(fullName)){
							candSubs.add(fullName);
						}
					}
				}				
			}
			
			// check that we do have candidates
			if(candSubs.size() == 0){
				return ATSDecimalNumberFormat.ONE;
			}
			
			// compute maximum score
			double maxScore = 0.0d;
			double thresh = threshold.doubleValue();
			for(String candSub: candSubs){
				double score = getScore(candSub, refSub);
				if(score > maxScore){
					maxScore = score;
				}
				if(score > thresh){
					break;
				}
			}
			
			// return maximum score
			return new BigDecimal(maxScore);
		}
		
		/**
		 * Clean a subdivision name
		 */
		private String cleanSub(String input){

			String output = input;
			
			// remove unwanted particles
			output = output.replaceAll("\\bPHASE ([IVX]+)\\b", "");
			output = output.replaceAll("\\b(COND|CONDO|CONDOMINIUM|ASSN|INC)\\b", "");
			
			// substitute roman numbers with text
			output = " " + output + " ";
			String translation [][] = {
				{" I ", " one "},
				{" II ", " two "},
				{" III ", " three "},
				{" IV ", " four "},
				{" V ", " five "},
				{" VI ", " six "},
				{" VII ", " seven "},
				{" VIII ", " eight "},
				{" IX ", " nine "},
				{" X ", " ten "}
			};			
			for(String [] pair : translation){
				output = output.replace(pair[0], pair[1] + pair[1] + pair[1] + pair[1]);
			}		
			if(logger.isInfoEnabled()){
				logger.info("Cleaned '" + input + "'. Result = '" + output + "'");
			}
			
			output = output.trim().toUpperCase();
			output = output.replaceAll("\\s{2,}", " ");
			
			return output;
		}
		
		/**
		 * Match a candidate subdivision with the reference subdivision
		 */
		private double getScore(String cand, String ref){
				
			double score;
			if(ref.startsWith(cand) || cand.startsWith(ref)){
				score = 1.0d;
			} else {
				Set<String> candSet = new HashSet<String>();
				candSet.add(cand);
				score = GenericNameFilter.calculateMatchForCompanyOrSubdivision(candSet, ref, threshold.doubleValue(), null);
			}
			if(logger.isInfoEnabled()){
				logger.info("Match '" + cand + "' vs. '" + ref + "'. Score = " + score);
			}
			return score;
		}		
		
		@Override
		public String getFilterCriteria() {
			return "Grantor/Grantee = '" + getSearchAttribute(SearchAttributes.LD_SUBDIV_NAME) + "'";
		}

		@Override
		public String getFilterName() {
			return "Subdivision Name as Grantor/Grantee Filter";
		}
		
	}


	@Override
	public void specificParseLegalDescription(Document doc, ParsedResponse item, ResultMap resultMap) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void specificParseGrantorGrantee(Document doc, ParsedResponse item, ResultMap resultMap) {
	}
	
	
	private static boolean addAoLookUpSearchesBookPageAtDocNo(TSServerInfo serverInfo, List<TSServerInfoModule> modules, Set<InstrumentI> allAoRef,long searchId,  boolean isUpdate){
		boolean atLeastOne = false;
		final Set<String> searched = new HashSet<String>();
		
		for(InstrumentI inst:allAoRef){
			boolean t = addBookPageSearch(inst, serverInfo, modules, searchId, searched, isUpdate);
			atLeastOne = atLeastOne || t;
			
			/* The following rules do not apply any more after NDB switching to Core Logic
			 * 
			 * if( inst.hasInstrNo() ){
				String instNo = inst.getInstno();
				if(instNo.length()==10 && !instNo.startsWith("000")){ //Book Page
					InstrumentI newInst = inst.clone();
					newInst.setBook(instNo.substring(0,6));
					newInst.setPage(instNo.substring(6,10));
					boolean temp = addBookPageSearch(newInst, serverInfo, modules, searchId, searched, isUpdate);
					atLeastOne = atLeastOne || temp;
				}
				else{
					boolean temp = addInstNoSearch(inst, serverInfo, modules, searchId, searched, isUpdate);
					atLeastOne = atLeastOne || temp;
				}
			}
			
			if ( inst.hasDocNo()  ){
				String docNo = inst.getDocno();
				if(docNo.length()==10 && !docNo.startsWith("000")){ //Book Page
					InstrumentI newInst = inst.clone();
					newInst.setBook(docNo.substring(0,6));
					newInst.setPage(docNo.substring(6,10));
					boolean temp = addBookPageSearch(newInst, serverInfo, modules, searchId, searched, isUpdate);
					atLeastOne = atLeastOne || temp;
				}
				else{
					boolean temp = addDocNoSearch(inst, serverInfo, modules, searchId, searched, isUpdate);
					atLeastOne = atLeastOne || temp;
				}
			}*/
		}
		return atLeastOne;
	}
	
	private static boolean addBookPageSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, Set<String> searched,boolean isUpdate){
		if(inst.hasBookPage()){
			String book = inst.getBook().replaceFirst("^0+", "");
			String page = inst.getPage().replaceFirst("^0+", "");
			if(!searched.contains(book+"_"+page)){
				searched.add(book+"_"+page);
			}else{
				return false;
			}
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
			module.setData(0, book);
			module.setData(1, page);
			if (isUpdate) {
				module.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			modules.add(module);
			return true;
		}
		return false;
	}
	
	private static boolean addDocNoSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId,Set<String> searched, boolean isUpdate){
		if ( inst.hasDocNo() ){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
			String instr = inst.getDocno().replaceFirst("^0+", "");
			String year = String.valueOf(inst.getYear());
			if(!searched.contains(instr+year)){
				searched.add(instr+year);
			}else{
				return false;
			}
			module.setData(4, instr);
			//module.setData(2, year);
			if (isUpdate) {
				module.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			modules.add(module);
			return true;
		}
		return false;
	}
	
	
	private static boolean addInstNoSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, Set<String> searched, boolean isUpdate){
		if ( inst.hasInstrNo() ){
			String instr = inst.getInstno().replaceFirst("^0+", "");
			String year = String.valueOf(inst.getYear());
			if(!searched.contains(instr+year)){
				searched.add(instr+year);
			}else{
				return false;
			}
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
			module.setData(4, instr);
			//module.setData(2, year);
			if (isUpdate) {
				module.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			modules.add(module);
			return true;
		}
		return false;
	}
	
	private static Set<InstrumentI> getAllAoReferences(Search search){
		Set<InstrumentI> allAoRef = new HashSet<InstrumentI>();
		DocumentsManagerI manager = search.getDocManager();
		try{
			manager.getAccess();
			List<DocumentI> list = manager.getDocumentsWithType( true, DType.ASSESOR );
			for(DocumentI assessor:list){
				if (HashCountyToIndex.isLegalBootstrapEnabled(search.getCommId(), assessor.getSiteId())) {
					for(RegisterDocumentI reg : assessor.getReferences()){
						allAoRef.add(reg.getInstrument());
					}
					allAoRef.addAll(assessor.getParsedReferences());
				}
			}
		}
		finally {
			manager.releaseAccess();
		}
		return allAoRef;
	}
	
	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		TSServerInfoModule module = null;
		if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			if("PLAT".equalsIgnoreCase(restoreDocumentDataI.getCategory())) {
				module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
				module.forceValue(2, "PLAT");
				module.forceValue(3, "BOOK_PAGE");
			} else {
				module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX);
				module.forceValue(2, "true");
				module.forceValue(3, "INST");
				module.forceValue(5, "No");
			}
			module.forceValue(0, book);
			module.forceValue(1, page);
		} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber())) {
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX);
			module.forceValue(4, restoreDocumentDataI.getInstrumentNumber());
			module.forceValue(2, "true");
			module.forceValue(3, "INST");
			module.forceValue(5, "No");
		} else if(StringUtils.isNotEmpty(restoreDocumentDataI.getDocumentNumber())) {
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX);
			module.forceValue(4, restoreDocumentDataI.getDocumentNumber());
			module.forceValue(2, "true");
			module.forceValue(3, "INST");
			module.forceValue(5, "No");
		}
		return module;
	}
	
	public Object getImageDownloader(RestoreDocumentDataI document) {
		TSServerInfoModule imgModule = getDefaultServerInfo().getModule(TSServerInfo.IMG_MODULE_IDX);
		imgModule.forceValue(0, org.apache.commons.lang.StringUtils.defaultString(document.getBook()));
		imgModule.forceValue(1, org.apache.commons.lang.StringUtils.defaultString(document.getPage()));
		imgModule.forceValue(2, 
				org.apache.commons.lang.StringUtils.defaultIfEmpty(
						document.getDocumentNumber(), 
						document.getInstrumentNumber()));
		imgModule.forceValue(4, org.apache.commons.lang.StringUtils.defaultString(document.getCategory()));
		
		
		return imgModule;
	}

	@Override
	public void specificParseAddress(Document doc, ParsedResponse item,
			ResultMap resultMap) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void performAdditionalProcessingAfterRunningAutomatic() {
		super.performAdditionalProcessingAfterRunningAutomatic();
		
		Search search = getSearch();
		
		if (isUpdate()){
			SearchAttributes sa = search.getSa();
			
			if (!sa.getCertificationDateManager().hasCertificationDateForSite(dataSite.getSiteTypeInt())){
				Set<InstrumentI> allAoRef = getAllAoReferences(search);
				
				if (allAoRef.size() > 0){
					SearchLogger.info("\n</div><hr/><div><BR>Run additional searches to get Certification Date. <BR></div>\n", searchId);
					TSServerInfo serverInfo = getCurrentClassServerInfo();
					for(InstrumentI inst : allAoRef){
						try {
							if (inst.hasBookPage()){
								String book = inst.getBook().replaceFirst("^0+", "");
								String page = inst.getPage().replaceFirst("^0+", "");
									
								TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX));
								module.setData(0, book);
								module.setData(1, page);
								
								module.setData(3, "INST");
									
								ServerResponse response = SearchBy(module, null);
								
								if (sa.getCertificationDateManager().hasCertificationDateForSite(dataSite.getSiteTypeInt())){
									SearchLogger.info("\n</div><div><BR>Certification Date found!<BR><hr/></div>\n", searchId);
									break;
								}
							}
						}catch(Exception e) {
							e.printStackTrace();
						}
					}
					
					if (!sa.getCertificationDateManager().hasCertificationDateForSite(dataSite.getSiteTypeInt())){
						SearchLogger.info("\n</div><div><BR>Certification Date still not found!<BR><hr/></div>\n", searchId);
					}
				}
			}
		}
	}
	
	@Override
	protected ServerResponse getErrorResponse(String mess) {
		if (mess.contains("MaximumRecordCountExceeded")) {
			ServerResponse serverResponse = new ServerResponse();
			serverResponse.getParsedResponse().setError("Too many results. Please refine your search.");
			return serverResponse;
		}
		return super.getErrorResponse(mess);
	}
	
}
