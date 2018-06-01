/*
 * Created on Jan 25, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ro.cst.tsearch.servers.types;
import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPManager;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPSite;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.TSInterface.DownloadImageResult.Status;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringCleaner;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.Image;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.ImageI.IType;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.connection.sureclose.SureCloseConn;
import com.stewart.datatree.DataTreeAccount;
import com.stewart.datatree.DataTreeConn;
import com.stewart.datatree.DataTreeImageException;
import com.stewart.datatree.DataTreeManager;
import com.stewart.datatree.DataTreeStruct;


public class MOClayRO extends TSServerROLike implements TSServerROLikeI {
    
	transient protected List<DataTreeStruct> datTreeList;
	
	public static final Pattern DOCTYPE = Pattern.compile("(?is)<td[^>]*>Instrument</td>\\s*<td[^>]*>\\s*(.*?)</td>");
	
	private static final long serialVersionUID = 1L;

	private static final int ID_SEARCH_BY_BOOK_PAGE_ADV = 102;
		
    
    public static Pattern resultSet = Pattern.compile( "<A.*?title=\" View Result Set (\\d+)\"\\s*href=\"([^\"]*)\"" );
    public static Pattern viewImagePattern = Pattern.compile( "(?s)OpenWindow\\('([^']*)'\\)" );
    public static Pattern imageLinkPattern = Pattern.compile("(?is)value=\\\"Click Here To View Document\\\" onclick=\"javascript:OpenWindow\\('([^']+)'\\)");
    public static Pattern bookPageDummyPattern = Pattern.compile( "(?s)<TR[^>]*>\\s*<TD[^>]*>\\s*<A HREF='([^\']*)'.*?</A>\\s*</TD>\\s*<TD[^>]*>.*?</TD>\\s*<TD[^>]*>.*?</TD>\\s*<TD[^>]*>.*?</TD>\\s*<TD[^>]*>.*?</TD>\\s*<TD[^>]*>.*?</TD>\\s*<TD[^>]*>.*?</TD>\\s*<TD[^>]*>.*?</TD>\\s*<TD[^>]*>(.*?)</TD>\\s*<TD[^>]*>.*?</TD>\\s*<TD[^>]*>.*?</TD>\\s*<TD[^>]*>.*?</TD>\\s*<TD[^>]*>.*?</TD>\\s*<TD[^>]*>.*?</TD>\\s*<TD[^>]*>.*?</TD>\\s*<TD[^>]*>.*?</TD>\\s*<TD[^>]*>.*?</TD>\\s*</TR>" );
    private static final Pattern certDatePattern = Pattern.compile("(?ism)Certification Date.*?<b>(.*?)</b>");
    
    private static String instrumentTypes = "<select name=\"INSTRUMENT_TYPES\" size=5 multiple>" +
    							"<option value=\"AFF\">AFFIDAVIT</option>" + 
                                "<option value=\"AGR\">AGREEMENT</option>" + 
                                "<option value=\"APPT\">APPOINTMENT</option>" + 
                                "<option value=\"AS\">ASSIGNMENT</option>" + 
                                "<option value=\"ASSN\">ASSIGNMENT OF DT</option>" + 
                                "<option value=\"AUCC\">AMENDED UCC/FINANCING ST</option>" + 
                                "<option value=\"BD\">BENEFICIARY DEED</option>" + 
                                "<option value=\"BIRTH\">BIRTH CERT</option>" + 
                                "<option value=\"BOND\">BOND</option>" + 
                                "<option value=\"CD\">CEMETERY DEED</option>" + 
                                "<option value=\"CERT\">CERTIFICATE</option>" + 
                                "<option value=\"CONS\">CONSENT</option>" + 
                                "<option value=\"CONT\">CONTRACT/CONTRACT FOR DEED</option>" + 
                                "<option value=\"CORP\">CORPORATE</option>" + 
                                "<option value=\"DB\">DANGEROUS BUILDING</option>" + 
                                "<option value=\"DEATH\">DEATH CERT</option>" + 
                                "<option value=\"DEED\">DEED</option>" + 
                                "<option value=\"DISC\">DISCLAIMER</option>" + 
                                "<option value=\"DOJ\">DEPT OF JUSTICE LIEN</option>" + 
                                "<option value=\"DT\">DEED OF TRUST</option>" + 
                                "<option value=\"EASE\">EASEMENT</option>" + 
                                "<option value=\"EXP\">EXPUNGE AFFIDAVIT</option>" + 
                                "<option value=\"FS\">FINAL SETTLEMENT</option>" + 
                                "<option value=\"FTL\">FEDERAL TAX LIEN</option>" + 
                                "<option value=\"HS\">HOME SCHOOL</option>" + 
                                "<option value=\"JUDG\">JUDGMENT</option>" + 
                                "<option value=\"LEASE\">LEASE</option>" + 
                                "<option value=\"LEVY\">LEVY</option>" + 
                                "<option value=\"LIEN\">LIEN</option>" + 
                                "<option value=\"LP\">LIS PENDENS</option>" + 
                                "<option value=\"MD\">MILITARY DISCHARGE</option>" + 
                                "<option value=\"MISC\">MISCELLANEOUS</option>" + 
                                "<option value=\"MLN\">MECHANIC'S LIEN NOTICE</option>" + 
                                "<option value=\"MOD\">MODIFICATION</option>" + 
                                "<option value=\"MR\">MARRIAGE REGISTER</option>" + 
                                "<option value=\"MTG\">MORTGAGE</option>" + 
                                "<option value=\"NOT\">NOTICE</option>" + 
                                "<option value=\"OPT\">OPTION</option>" + 
                                "<option value=\"ORD\">ORDINANCE</option>" + 
                                "<option value=\"ORDER\">ORDER</option>" + 
                                "<option value=\"PERMIT\">PERMIT</option>" + 
                                "<option value=\"PL\">PLAT</option>" + 
                                "<option value=\"POA\">POWER OF ATTORNEY</option>" + 
                                "<option value=\"PRBT\">PROBATE</option>" + 
                                "<option value=\"PRFTL\">PARTIAL REL FD TX LN</option>" + 
                                "<option value=\"PRSTL\">PARTIAL REL ST TX LN</option>" + 
                                "<option value=\"PTREL\">PARTIAL RELEASE</option>" + 
                                "<option value=\"QC\">QUIT CLAIM</option>" + 
                                "<option value=\"RAGR\">RELEASE AGREEMENT</option>" + 
                                "<option value=\"RAT\">RATIFICATION</option>" + 
                                "<option value=\"RCOMM\">REPORT OF COMMISSIONERS</option>" + 
                                "<option value=\"RDB\">RELEASE DANGEROUS BUILDING</option>" + 
                                "<option value=\"RDOJ\">REL DEPT OF JUSTICE LIEN</option>" + 
                                "<option value=\"REL\">RELEASE</option>" + 
                                "<option value=\"REPD\">REPRESENTATIVES DEED</option>" + 
                                "<option value=\"REQ\">REQUEST NOTICE</option>" + 
                                "<option value=\"REST\">RESTRICTIONS</option>" + 
                                "<option value=\"REV\">REVOCATION</option>" + 
                                "<option value=\"RFTL\">REL FEDERAL TAX LIEN</option>" + 
                                "<option value=\"RLIEN\">RELEASE LIEN</option>" + 
                                "<option value=\"RLP\">REL LIS PENDENS</option>" + 
                                "<option value=\"RMLN\">REL MECHANICS LIEN</option>" + 
                                "<option value=\"ROW\">RIGHT OF WAY</option>" + 
                                "<option value=\"RSTL\">REL STATE TAX LIEN</option>" + 
                                "<option value=\"STL\">STATE TAX LIEN</option>" + 
                                "<option value=\"SUB\">SUBORDINATION</option>" + 
                                "<option value=\"SURV\">SURVEY</option>" + 
                                "<option value=\"TERM\">TERMINATION</option>" + 
                                "<option value=\"TR\">TRUST</option>" + 
                                "<option value=\"TRD\">TRUSTEE'S DEED</option>" + 
                                "<option value=\"TRDUS\">TRUSTEE'S DEED UNDER SALE</option>" + 
                                "<option value=\"UCC\">UCC/FINANCING STATEMENT</option>" + 
                                "<option value=\"WAV\">WAIVER</option>" + 
                                "<option value=\"WD\">WARRANTY DEED</option></select>";
        
        private boolean downloadingForSave;
        public static  SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

        public MOClayRO( String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink,long searchId, int mid)
        {
            super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,searchId, mid);
            resultType = MULTIPLE_RESULT_TYPE;
            datTreeList = initDataTreeStruct();
        }
        
        protected List<DataTreeStruct> initDataTreeStruct(){
    		DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(
    				InstanceManager.getManager().getCommunityId(searchId), 
    				miServerID);
    		return DataTreeManager.getProfileDataUsingStateAndCountyFips(dat.getCountyFIPS(), dat.getStateFIPS());
    	}
        
        protected static boolean downloadImageFromDataTree(InstrumentI i, List<DataTreeStruct> list, String path , Search search, String month, String day){
        	
        	String commId = String.valueOf(search.getCommId());
    		
    		logger.info("(((((((((( downloadImageFromDataTree ---  instrument="+i+" path="+path+" list="+list);
    		
    		DataTreeAccount acc = DataTreeManager.getDatatTreeAccount(commId);
    		List<DataTreeStruct> toSearch = new ArrayList<DataTreeStruct>(2);
    		
    		
    		for(DataTreeStruct struct:list){
    			if("DAILY_DOCUMENT".equalsIgnoreCase(struct.getDataTreeDocType())){
    				toSearch.add(struct);
    			}
    		}
    		
    		logger.info("(((((((((( downloadImageFromDataTree ---  instrument="+i+" path="+path+" toSearch="+toSearch);
    		
    		boolean imageDownloaded = false;
    		List<DataTreeImageException> exceptions = new ArrayList<DataTreeImageException>();
    		for(DataTreeStruct struct:toSearch){
    			try {
					if((imageDownloaded = DataTreeManager.downloadImageFromDataTree(acc, struct, i, path, month, day))){
						break;
					}
				} catch (DataTreeImageException e) {
					exceptions.add(e);
				}
    		}

			if(!imageDownloaded && toSearch.size() == exceptions.size() && !exceptions.isEmpty()) {
				boolean throwException = true;
				DataTreeConn.logDataTreeImageException(i, search.getSearchID(), exceptions, throwException);
			}
    		
    		logger.info("(((((((((( downloadImageFromDataTree ---  return =" + imageDownloaded);
    		
    		return imageDownloaded;
    	}
        
        @Override
        public DownloadImageResult saveImage(ImageI image) throws ServerResponseException {
    		HashMap<String, String> map = HttpUtils.getParamsFromLink(image.getLink(0));
    		
    		String book 	 = org.apache.commons.lang.StringUtils.defaultString(map.get("book")) ;
    		String page 	 = org.apache.commons.lang.StringUtils.defaultString(map.get("page")) ;
    		String docNumber = org.apache.commons.lang.StringUtils.defaultString(map.get("instrNo"));
    		String year 	 = org.apache.commons.lang.StringUtils.defaultString(map.get("year")) ;
    		String type 	 = org.apache.commons.lang.StringUtils.defaultString(map.get("type")) ;	
    		String instrumentPK 	 = org.apache.commons.lang.StringUtils.defaultString(map.get("instrumentPK")) ;
    		String typePK 	 = org.apache.commons.lang.StringUtils.defaultString(map.get("typePK")) ;
    		
    		if(docNumber.length() >= 10){
    			if(StringUtils.isEmpty(year)){
    				year = docNumber.substring(0,4);
    			}
    			docNumber = docNumber.substring(4).replaceAll("^0+", "");
    		}else if(docNumber.length() <= 6){
    			if(docNumber.matches("[A-Z]\\d+")){
    				docNumber = docNumber.substring(0,1) + "." + docNumber.substring(1);
    			}
    		}
    		
        	InstrumentI i = new Instrument();
        	i.setBook(book);
        	i.setPage(page);
        	i.setInstno(docNumber);
        	try{i.setYear(Integer.parseInt(year));}catch(Exception e){}
        	i.setDocType(type);
        	
        	DownloadImageResult result = new DownloadImageResult();
			result.setStatus(Status.ERROR);
			result.setImageContent(null);
			
			if(downloadImageFromDataTree(i, datTreeList, image.getPath(), getSearch() , null, null)){
				try {
					byte []tiffBytes = org.apache.commons.io.FileUtils.readFileToByteArray(new File(image.getPath()));
					byte[] pdf = SureCloseConn.convertToPDF(tiffBytes);
					org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(image.getPath()), pdf);
					result.setImageContent(pdf);
				} catch (IOException e) {
					e.printStackTrace();
				}
				result.setContentType("application/pdf");
				result.setStatus(Status.OK);
				SearchLogger.info("<br/>Image(searchId=" + searchId + " ) book=" +
						book + " page=" + page + " inst=" +
						docNumber + " was taken from DataTree<br/>", searchId);
			} else{
				//download image from official site
				
				HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
				try {
					HTTPRequest req = new HTTPRequest("http://recorder.claycogov.com/iRecordClient/DisplayDocument.aspx?CATEGORY=" + typePK + "&INSTRUMENT_PK=" + instrumentPK);
					HTTPResponse res = site.process(req);
					byte[] images = res.getResponseAsByte();
					
					if(res != null && res.getReturnCode() == 200){
						if("application/pdf".equalsIgnoreCase(res.getContentType())){
							result.setImageContent(images);
							result.setContentType(res.getContentType());
							result.setStatus(Status.OK);
							SearchLogger.info("<br/>Image(searchId=" + searchId + " ) book=" +
									book + " page=" + page + " inst=" +
									docNumber + " was taken from MOClayRO official site<br/>", searchId);
							FileUtils.writeByteArrayToFile(images, image.getPath());
						}
					}
					
				}catch(Exception e) {
					e.printStackTrace();
				}finally {
					HttpManager.releaseSite(site);
				}
				
			}
			return result;
        }
        
        public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException{   
            String[] instrTypes = null;
            String instrTypeFilter = "";
    
            if(getSearch().getSearchType() == Search.PARENT_SITE_SEARCH){
                //parent site search
                
                if(sd != null && sd instanceof SearchDataWrapper){
                    if(((SearchDataWrapper)sd).getRequest() != null){
                        switch(module.getModuleIdx()){
                            case TSServerInfo.ADV_SEARCH_MODULE_IDX:
                                instrTypes = ((SearchDataWrapper)sd).getRequest().getParameterValues("param_12_8");
                                break;
                            case TSServerInfo.GENERIC_MODULE_IDX:
                                instrTypes = ((SearchDataWrapper)sd).getRequest().getParameterValues("param_18_8");
                                break;
                        }
                    }
                }
                if(instrTypes != null){
                    for(int i = 0; i < instrTypes.length; i++){
                        instrTypeFilter += instrTypes[i];
                        
                        if(i != instrTypes.length - 1){
                            instrTypeFilter += " ";
                        }
                    }      
                    module.getFunction(8).setParamValue(instrTypeFilter);
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
	    	if (initialFollowedLnk.matches("(?i).*[^a-z]+dummy[=]([0-9a-z_]+)[^0-9a-z][&=a-z]+crossRefSource[=]([a-z]*)[^a-z]*.*"))
	    	{
	    		String retStr = initialFollowedLnk.replaceFirst(
	    				"(?i).*[^a-z]+dummy[=]([0-9a-z_]+)[^0-9a-z][&=a-z]+crossRefSource[=]([a-z]*)[^a-z]*.*", 
	    				"Instrument " + "$1 "+ " " + "$2" + 
	    				" has already been processed from a previous search in the log file. ");
	    		retStr =  "<br/><span class='followed'>"+retStr+"</span><br/>";
	    		
	    		return retStr;
	    	}
	    	
	    	return "<br/><span class='followed'>Link already followed</span>:" + initialFollowedLnk + "<br/>";
	    }

        
        public TSServerInfo getDefaultServerInfo(){

        	TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
    		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX);
    			
    		if(tsServerInfoModule != null) {
                setupSelectBox(tsServerInfoModule.getFunction(8), instrumentTypes);
                tsServerInfoModule.getFunction(8).setRequired(true);
    		}
    		
    		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.GENERIC_MODULE_IDX);
    		
    		if(tsServerInfoModule != null) {
                setupSelectBox(tsServerInfoModule.getFunction(8), instrumentTypes);
                tsServerInfoModule.getFunction(8).setRequired(true);
    		}
    		
    		msiServerInfoDefault.setupParameterAliases();
            setModulesForAutoSearch( msiServerInfoDefault );
            setModulesForGoBackOneLevelSearch(msiServerInfoDefault);

            
            return msiServerInfoDefault;
        }
        
        protected String GetInfo(String what)
        {           
            return "";
        }
        
        protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
        	List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
            
            FilterResponse legalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
            DocsValidator defaultLegalValidator = legalFilter.getValidator();
    		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId,true,true).getValidator(); 
    		DocsValidator addressHighPassValidator = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d).getValidator();
    		DocsValidator recordedDateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId).getValidator();
    		DocsValidator subdivisionNameValidator = NameFilterFactory.getNameFilterForSubdivisionWithCleaner(searchId, StringCleaner.MO_CLAY_RO_SUB).getValidator();
    		ConfigurableNameIterator nameIterator = null;
    		boolean validateWithDates = applyDateFilter();
    		
            TSServerInfoModule m;
            {
	            // simple instrument number search from search page - needed to be executed before bootstrapping any book&page # from RO (related to incident # 36330)
	            m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
	            m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	            		TSServerInfoConstants.VALUE_PARAM_LIST_SEARCH_PAGE_INSTR);
	            m.clearSaKeys();
	            m.setSaObjKey(SearchAttributes.LD_INSTRNO);
	            m.setIteratorType(ModuleStatesIterator.TYPE_INSTRUMENT_LIST_SEARCH);
	            m.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
	            
	        	m.addValidator( recordedDateValidator );
	            m.addCrossRefValidator( defaultLegalValidator );
				m.addCrossRefValidator( addressHighPassValidator );
				m.addCrossRefValidator( recordedDateValidator );
				
	            l.add(m);
            }  
            
            {
	            // search by book and page list from Search Page - needed to be executed before bootstrapping any instrument # from RO (related to incident #36330)
	            m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
	            m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	            		TSServerInfoConstants.VALUE_PARAM_LIST_SEARCH_PAGE_BP);
	            m.clearSaKeys();
	            m.setSaObjKey(SearchAttributes.LD_BOOKPAGE);
	            m.setIteratorType(ModuleStatesIterator.TYPE_BOOK_PAGE_LIST_SEARCH);
	            m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
	            m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
	            
	            m.addFilter(new GenericInstrumentFilter(searchId));
	            m.addValidator( recordedDateValidator );
	            
	            m.addCrossRefValidator( defaultLegalValidator );
				m.addCrossRefValidator( addressHighPassValidator );
		        m.addCrossRefValidator( pinValidator );
		        m.addCrossRefValidator( recordedDateValidator );
	            
	            l.add(m); 
            }
            
            {
	            // search by grantor/owner - advanced
	            if(hasOwner()) {
	            	m = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.ADV_SEARCH_MODULE_IDX ) );
	            	m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
	            			TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
	            	DocsValidator recordedDateWithModuleValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
		            m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
		            //FilterResponse nameFilter =  NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
	//	            FilterResponse nameFilter =  NameFilterFactory.getNameFilterIgnoreMiddleOnEmpty(SearchAttributes.OWNER_OBJECT, searchId, m);
		            GenericNameFilter nameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
		            nameFilter.setIgnoreMiddleOnEmpty(true);
		            nameFilter.setIgnoreEmptyMiddleOnCandidat(true);
		            m.addFilter(nameFilter);
		            nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L;F;"} );
		 		   	m.addIterator( nameIterator );
		 		   	
		 		   	m.clearSaKey(5);
		 		    m.forceValue(6, "01/01/1960");
		 		   	m.setSaKey(7, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		 		   	
		 		   	m.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		 		   	
		            m.addValidator( defaultLegalValidator );
		            m.addValidator( subdivisionNameValidator );
					m.addValidator( addressHighPassValidator );
			        m.addValidator( pinValidator );
			        m.addValidator( recordedDateWithModuleValidator );
			        addFilterForUpdate(m, false);
			        
					m.addCrossRefValidator( defaultLegalValidator );
					m.addCrossRefValidator( addressHighPassValidator );
			        m.addCrossRefValidator( pinValidator );
			        m.addCrossRefValidator( recordedDateWithModuleValidator );
	            }
	            
	            l.add(m);
            }
            
            {
	            //simple instrument list search
	            m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
	            m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	            		TSServerInfoConstants.VALUE_PARAM_LIST_SEARCH_PAGE_BP);
	            m.setIteratorType(ModuleStatesIterator.TYPE_INSTRUMENT_LIST);
	            m.getFunction(0).setSaKey("");
	            m.getFunction(1).setSaKey("");
	            m.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
	            m.setSaObjKey(SearchAttributes.INSTR_LIST);
	            
	            m.addCrossRefValidator( defaultLegalValidator );
				m.addCrossRefValidator( addressHighPassValidator );
		        m.addCrossRefValidator( pinValidator );
		        m.addCrossRefValidator( recordedDateValidator );
	            if(validateWithDates) {
	            	m.addValidator( recordedDateValidator );
	            }
	            
	            l.add(m);
            }
            
            {
	            //simple book page list search
	            m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
	            m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
	            		TSServerInfoConstants.VALUE_PARAM_LIST_SEARCH_PAGE_BP);
	            m.clearSaKeys();
	            m.setIteratorType(ModuleStatesIterator.TYPE_BOOK_PAGE_LIST);
	            m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
	            m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
	            m.setSaObjKey(SearchAttributes.INSTR_LIST);
	            m.addFilter(new GenericInstrumentFilter(searchId));
	            m.addValidator( recordedDateValidator );
	            m.addCrossRefValidator( defaultLegalValidator );
				m.addCrossRefValidator( addressHighPassValidator );
		        m.addCrossRefValidator( pinValidator );
		        m.addCrossRefValidator( recordedDateValidator );
		        
	            l.add(m);                                  
            } 
            
            {
	            if( searchWithSubdivision() ){
	            	{	//advanced legal search
			            m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
			            m.clearSaKey(0);
			            m.clearSaKey(1);
			            m.clearSaKey(2);
			            m.addFilter(legalFilter);
			            m.addValidator(defaultLegalValidator);
			            m.addValidator( subdivisionNameValidator );
			            m.addCrossRefValidator( defaultLegalValidator );
			            m.addCrossRefValidator( subdivisionNameValidator );
						m.addCrossRefValidator( addressHighPassValidator );
			            if(validateWithDates) {
			            	m.addValidator( recordedDateValidator );
			            	m.addCrossRefValidator( recordedDateValidator );
			            }
			            
			            l.add(m);
	            	}
	            	
	            	{	// legal = subdivision
			            m = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.GENERIC_MODULE_IDX ) );
			            m.setIteratorType( ModuleStatesIterator.TYPE_MISSOURI_SUBDIVISION );
			            m.clearSaKey(0);
			            m.clearSaKey(1);
			            m.setSaKey(5, SearchAttributes.LD_SUBDIV_NAME );
			            m.getFunction( 5 ).setIteratorType( FunctionStatesIterator.ITERATOR_TYPE_MISSOURI_SBDIV_NAME );
			            m.getFunction( 8 ).setParamValue( "PL EASE REST" );
			            m.addFilter(legalFilter);
			            
			            m.addValidator(defaultLegalValidator);
			            m.addValidator( subdivisionNameValidator );
			            m.addCrossRefValidator( defaultLegalValidator );
			           	m.addCrossRefValidator( subdivisionNameValidator );
						m.addCrossRefValidator( addressHighPassValidator );
			            if(validateWithDates) {
			            	m.addValidator( recordedDateValidator );
			            	m.addCrossRefValidator( recordedDateValidator );
			            }
			            
			            l.add( m );
	            	}
	            
	            	{	// last name = subdivision
			            m = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.ADV_SEARCH_MODULE_IDX ) );
			            m.setIteratorType( ModuleStatesIterator.TYPE_MISSOURI_SUBDIVISION );
			            m.setSaKey(0, SearchAttributes.LD_SUBDIV_NAME );
			            m.clearSaKey(1);
			            m.clearSaKey(5);
			            
			            m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_MISSOURI_SBDIV_NAME );
			            
			            m.setParamValue(6, "01/01/1810");
			            m.getFunction( 8 ).setParamValue( "PL" );
			            m.addFilter(legalFilter);
			            m.addValidator(defaultLegalValidator);
			            m.addValidator( subdivisionNameValidator );
			            m.addCrossRefValidator( defaultLegalValidator );
			            m.addCrossRefValidator( subdivisionNameValidator );
						m.addCrossRefValidator( addressHighPassValidator );
			            if(validateWithDates) {
			            	m.addValidator( recordedDateValidator );
			            	m.addCrossRefValidator( recordedDateValidator );
			            }
			            
			            l.add( m );
			            //RESULTS ARE NOT FILTERED
	            	}
	            	
	            	{
			            m = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.ADV_SEARCH_MODULE_IDX ) );
			            m.setIteratorType( ModuleStatesIterator.TYPE_MISSOURI_SUBDIVISION );
			            m.clearSaKey(1);
			            m.clearSaKey(5);
			            m.setSaKey(0, SearchAttributes.LD_SUBDIV_NAME );
			            m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_MISSOURI_SBDIV_NAME );
			            m.getFunction( 8 ).setParamValue( "EASE REST" );
			            m.addFilter(legalFilter);
			            m.addValidator(defaultLegalValidator);
			            m.addValidator( subdivisionNameValidator );
			            m.addCrossRefValidator( defaultLegalValidator );
			            m.addCrossRefValidator( subdivisionNameValidator );
						m.addCrossRefValidator( addressHighPassValidator );
			            m.addValidator(subdivisionNameValidator);
			            m.addCrossRefValidator(subdivisionNameValidator);
			            if(validateWithDates) {
			            	m.addValidator( recordedDateValidator );
			            	m.addCrossRefValidator( recordedDateValidator );
			            }
			            l.add( m );
			            //RESULTS ARE NOT FILTERED
	            	}
	            	
	            } else {
	            	printSubdivisionException();
	            }
            }
            
            {
	            /*
	            // search by grantee/buyer - advanced
	            m = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.ADV_SEARCH_MODULE_IDX ) );
	            m.setSaObjKey(SearchAttributes.BUYER_OBJECT);
	            m.getFunction( 5 ).setSaKey( "" );
	            m.getFunction( 8 ).setParamValue("DT MTG AUCC DOJ FTL LIEN MLN STL UCC JUDG LP");
	            m.setIteratorType( ModuleStatesIterator.TYPE_REGISTER_NAME );
	            m.addFilterType( FilterResponse.TYPE_REGISTER_NAME );
	            m.addCrossRefValidatorType(DocsValidator.TYPE_REGISTER_SUBDIV_LOT);
	            l.add( m );
	            */
            }
            
            {	//OCR module
	            m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
	            m.setIteratorType(ModuleStatesIterator.TYPE_OCR);
	            m.getFunction(0).setSaKey("");
	            m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
	            m.getFunction(1).setSaKey("");
	            m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
	            m.getFunction(2).setSaKey("");
	            m.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
	            m.addFilter(new GenericInstrumentFilter(searchId));
	            m.addCrossRefValidator( defaultLegalValidator );
				m.addCrossRefValidator( addressHighPassValidator );
		        m.addCrossRefValidator( pinValidator );
		        m.addCrossRefValidator( recordedDateValidator );
		        
	            l.add(m);
            }
            
            {
	            m = new TSServerInfoModule( serverInfo.getModule( TSServerInfo.ADV_SEARCH_MODULE_IDX ) );
	            DocsValidator recordedDateWithModuleValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
	            
	            m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
//	            m.addFilter( NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m));
	            m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
	            GenericNameFilter nameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
	            nameFilter.setIgnoreMiddleOnEmpty(true);
	            nameFilter.setIgnoreEmptyMiddleOnCandidat(true);
	            m.addFilter(nameFilter);
	            
	            m.clearSaKey(5);
	 		   	m.forceValue(6, "01/01/1960");
	 		   	m.setSaKey(7, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
	 		   	
	 		   	m.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
	 		   	
	            m.addValidator( defaultLegalValidator );
	            m.addValidator( subdivisionNameValidator );
				m.addValidator( addressHighPassValidator );
		        m.addValidator( pinValidator );
		        m.addValidator( recordedDateWithModuleValidator );
		        
				m.addCrossRefValidator( defaultLegalValidator );
				m.addCrossRefValidator( subdivisionNameValidator );
				m.addCrossRefValidator( addressHighPassValidator );
		        m.addCrossRefValidator( pinValidator );
		        m.addCrossRefValidator( recordedDateWithModuleValidator );
		        
		        ArrayList< NameI> searchedNames = null;
				if(nameIterator!=null)
					nameIterator.getSearchedNames();
				else
					searchedNames = new ArrayList<NameI>();
				nameIterator = (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator( m, searchId, false, new String[]{"L;F;"} );
				nameIterator.setInitAgain( true );
				nameIterator.setSearchedNames( searchedNames );
				m.addIterator( nameIterator ); 
				
	        	l.add( m );
            }
        	
            serverInfo.setModulesForAutoSearch(l);
        }
        
        
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		ConfigurableNameIterator nameIterator = null;
		DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId,true, true).getValidator();
		DocsValidator addressHighPassValidator = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d).getValidator();

		SearchAttributes sa = getSearch().getSa();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		GBManager gbm = (GBManager) sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

		for (String id : gbm.getGbTransfers()) {

			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
			module.setIndexInGB(id);
			module.setTypeSearchGB("grantor");
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
			module.addFilter(NameFilterFactory.getDefaultNameFilter(
					SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			String date = gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
			if (date != null)
				module.getFunction(6).forceValue(date);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(module, searchId,
							new String[] { "L;F;" });
			module.addIterator(nameIterator);
			module.addValidator(defaultLegalValidator);
			module.addValidator(addressHighPassValidator);
			module.addValidator(pinValidator);
			module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(
					searchId, 0.90d, module).getValidator());
			module.addValidator(DateFilterFactory.getDateFilterForGoBack(
					SearchAttributes.GB_MANAGER_OBJECT, searchId, module)
					.getValidator());
			modules.add(module);

			if (gbm.getNamesForBrokenChain(id, searchId).size() > 0) {
				module = new TSServerInfoModule(
						serverInfo
								.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX));
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantee");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				module.addFilter(NameFilterFactory.getDefaultNameFilter(
						SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				date = gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy",
						searchId);
				if (date != null)
					module.getFunction(6).forceValue(date);
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
						.getConfigurableNameIterator(module, searchId,
								new String[] { "L;F;" });
				module.addIterator(nameIterator);
				module.addValidator(defaultLegalValidator);
				module.addValidator(addressHighPassValidator);
				module.addValidator(NameFilterFactory
						.getDefaultTransferNameFilter(searchId, 0.90d, module)
						.getValidator());
				module.addValidator(DateFilterFactory.getDateFilterForGoBack(
						SearchAttributes.GB_MANAGER_OBJECT, searchId, module)
						.getValidator());
				modules.add(module);

			}

		}

		serverInfo.setModulesForGoBackOneLevelSearch(modules);
	}

        
        /*protected String getFileNameFromLink(String link){
            String parcelId = null;
            String token = "dummy=";
            int istart = link.indexOf(token);
            
            if(istart != -1){
                int iend = link.indexOf("&", istart);
                if(iend == -1){
                    parcelId = link.substring(istart + token.length());
                } else{
                    parcelId = link.substring(istart + token.length(), iend);
                }
                
            } else{
                parcelId = "_";
            }
            
            return parcelId ;
        }*/
        
        private static final Pattern PAT_BOOK_PAGE = Pattern.compile("(?i)<TD[^>]+>(\\d+)</TD>\\s*</TR>\\s*<TR>\\s*<TD[^>]*>Page</TD>\\s*<TD[^>]*>(\\d+)</TD>");
       
        protected void ParseResponse( String sAction, ServerResponse Response, int viParseID)  throws ServerResponseException{
            String sTmp = "";
            String rsResponce = Response.getResult();
            
            int istart = -1, iend = -1;

            sTmp = CreatePartialLink(TSConnectionURL.idGET);

            rsResponce = rsResponce.replaceAll("", "");
            
            if (rsResponce.indexOf("No matching records were found") != -1){
				Response.getParsedResponse().setError("No matching records were found");
				return;
			} else if (rsResponce.indexOf("ERROR: Please specify a instrument number, book or legal description for the search") != -1){
				Response.getParsedResponse().setError("ERROR: Please specify a instrument number, book or legal description for the search");
				return;
			}
            
            switch( viParseID )
            {
                case ID_SEARCH_BY_NAME:
                case ID_SEARCH_BY_BOOK_AND_PAGE:
                case ID_SEARCH_BY_BOOK_PAGE_ADV:
                    
                	try {
   					 
    					StringBuilder outputTable = new StringBuilder();
    					ParsedResponse parsedResponse = Response.getParsedResponse();
    																		
    					Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponce, outputTable);
    											
    					if(smartParsedResponses.size() > 0) {
    						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
    						parsedResponse.setOnlyResponse(outputTable.toString());
    		            }
    					
    				} catch(Exception e) {
    					e.printStackTrace();
    				}
                	
                    break;
                case ID_DETAILS:
                    
                	rsResponce = getDetails(rsResponce, Response);
                	
                	Pattern patInstrumentPK = Pattern.compile("DisplayDocument.aspx[?]CATEGORY=([A-Za-z]*)[&]a?m?p?;?INSTRUMENT_PK=(\\d+)");
                	Matcher matInstrumentPk = patInstrumentPK.matcher(rsResponce);
                	String instrumentPK ="";
                	String typePK ="";
                	
                	if(matInstrumentPk.find()){
                		instrumentPK = matInstrumentPk.group(2);
                		typePK = matInstrumentPk.group(1);
                	} else{
                		patInstrumentPK = Pattern.compile("(?is)&instrumentPK=(\\d+)&typePK=([A-Za-z]*)");
                    	matInstrumentPk = patInstrumentPK.matcher(rsResponce);
                    	if(matInstrumentPk.find()){
                    		instrumentPK = matInstrumentPk.group(1);
                    		typePK = matInstrumentPk.group(2);
                    	}
                	}
                	                    
                    istart = rsResponce.indexOf("Document No.");
                    istart = rsResponce.indexOf("</td>", istart) + "</td>".length();
                    istart = rsResponce.indexOf(">", istart) + ">".length();
                    iend = rsResponce.indexOf("</td>", istart);
                    
                    String imageInstrNo = rsResponce.substring(istart, iend);
                    
                    String book ="";
                    String page = "";
                    Matcher matBookPage = PAT_BOOK_PAGE.matcher(rsResponce);
                    if( matBookPage.find() ){
                    	book = matBookPage.group(1);
                    	page = matBookPage.group(2);
                    }
                    
                    String imgLink = "/ImageCache?";
                    String fileName = imageInstrNo + ".html";
                    
                    imgLink += ("instrNo=" + imageInstrNo);
                    imgLink += ("&book=" + book);
                    imgLink += ("&page=" + page);
                    imgLink += ("&instrumentPK=" + instrumentPK);
                    imgLink += ("&typePK=" + typePK);
                    
                    if(!rsResponce.toLowerCase().contains("view image")){
	                    rsResponce = rsResponce.replaceAll("(?is)<A HREF='([^']*)'","<A HREF='" + sTmp + "/iRecordClient/$1'");
	                    rsResponce = rsResponce.replaceAll("(?is)<A HREF='([^']*.*)\\?(INSTRUMENT_PK=\\d*)'>Instr\\s*#:\\s*(\\d*)","<A HREF='$1&$2&dummy=$3'>Instr #: $3");
	                    rsResponce = rsResponce.replaceAll("(?is)<A HREF='([^']*.*)\\?(INSTRUMENT_PK=\\d*)'>Book:\\s*(\\d*)\\s*Page:\\s*(\\d*)","<A HREF='$1&$2&dummy=$3_$4'>Book: $3 Page: $4");
	                    rsResponce = rsResponce.replaceAll("(?is)<FORM>", "");
	                    rsResponce = rsResponce.replaceAll("(?is)</FORM>", "");

	                    rsResponce = rsResponce.replaceAll("(?is)<INPUT\\s+type=\\\"button\\\"\\s+name=\\\"BTN_VIEW_DOCUMENT\\\"\\s+value=\\\"Click Here To View Document\\\"\\s+onclick=\\\"javascript:OpenWindow\\('([^']+)'\\)[^>]*>", 
	                    					"<a target=\"_blank\" href=" + CreatePartialLink(TSConnectionURL.idGET) + imgLink + ">View Image<BR></a>" );
                    }
	                
                    String originalLink = sAction + "&" + "dummy=" + imageInstrNo + "&" + Response.getQuerry();
	                String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
	                if (imgLink != null){
	                	Response.getParsedResponse().addImageLink(new ImageLinkInPage (sTmp + imgLink,  imageInstrNo + ".pdf" ));
	        		}
                    String sDoctype = getDoctypeFromResponse(rsResponce);
					
                    HashMap<String, String> data = null;
					if (!StringUtils.isEmpty(sDoctype)) {
						data = new HashMap<String, String>();
						data.put("type", sDoctype);
						data.put("book", book);
		    			data.put("page", page);
						fileName = imageInstrNo + DocumentTypes.getDocumentCategory(sDoctype, searchId) + ".html";
					}
                    if(!downloadingForSave){
    					if (isInstrumentSaved(imageInstrNo, null, data)){
    					    rsResponce += CreateFileAlreadyInTSD();
    					} else{
    						mSearch.addInMemoryDoc(sSave2TSDLink, rsResponce);
                            rsResponce = addSaveToTsdButton(rsResponce, sSave2TSDLink, viParseID);
                        }
                        
                        Response.getParsedResponse().setPageLink( new LinkInPage( sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD ) );
                        parser.Parse( Response.getParsedResponse(), rsResponce, Parser.NO_PARSE);
                    } else{
                        msSaveToTSDFileName = fileName ;
                        //Response.getParsedResponse().setPageLink( new LinkInPage( sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD ) );
                        Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
                        msSaveToTSDResponce = rsResponce + CreateFileAlreadyInTSD();
                        
                        rsResponce = rsResponce.replaceAll("(?is)<a[^>]*>\\s*View Image\\s*<BR>\\s*</a>", "");
    					String resultForCross = rsResponce;
    					rsResponce = rsResponce.replaceAll("(?is)</?a[^>]*>", "");
                        
    					//smartParseDetails(Response, rsResponce);
    					
    					parser.Parse(
                                Response.getParsedResponse(),
                                rsResponce,
                                Parser.PAGE_DETAILS,
                                getLinkPrefix(TSConnectionURL.idGET),
                                TSServer.REQUEST_SAVE_TO_TSD);
                        
                        rsResponce = rsResponce.replaceAll( "<A HREF=.*?>([^<]*)</a>", "$1" );
                        Response.getParsedResponse().setOnlyResponse(rsResponce);
                        
                        Pattern crossRefLinkPattern = Pattern.compile("(?ism)<a[^>]*?href=[\\\"|'](.*?)[\\\"|']>");
    	                Matcher crossRefLinkMatcher = crossRefLinkPattern.matcher(resultForCross);
    	                while(crossRefLinkMatcher.find()) {
    		                ParsedResponse prChild = new ParsedResponse();
    		                String link = crossRefLinkMatcher.group(1) + "&isSubResult=true";
    		                LinkInPage pl = new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD);
    		                prChild.setPageLink(pl);
    		                Response.getParsedResponse().addOneResultRowOnly(prChild);
    	                }
                    }
                    break;
                case ID_GET_LINK:
                case ID_SAVE_TO_TSD:
                    
                    if( viParseID == ID_GET_LINK){
                        if(Response.getQuerry().indexOf("nextLink=") >= 0 || Response.getQuerry().indexOf("seq") >= 0){
                            ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
                        } else{
                            ParseResponse(sAction, Response, ID_DETAILS);
                        }
                    } else if(viParseID == ID_SAVE_TO_TSD){
                        downloadingForSave = true;
                        ParseResponse(sAction, Response, ID_DETAILS);
                        downloadingForSave = false;
                    }                                        
                    break;
            }
        }
        
        public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
    		LinkedHashMap<String, ParsedResponse> responses = new LinkedHashMap<String, ParsedResponse>();
    		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
    		
    		Search search = this.getSearch();
    		searchId = search.getID();
    		
    		/**
    		 * We need to find what was the original search module
    		 * in case we need some info from it like in the new PS interface
    		 */
    		TSServerInfoModule moduleSource = null;
    		Object objectModuleSource = response.getParsedResponse().getAttribute(
    				TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
    		if(objectModuleSource != null) {
    			if(objectModuleSource instanceof TSServerInfoModule) {
    				moduleSource = (TSServerInfoModule) objectModuleSource;
    			} 
    		} else {
    			objectModuleSource = search.getAdditionalInfo(
    					this.getKeyForSavingInIntermediaryNextLink(response.getQuerry()));
    			if (objectModuleSource instanceof TSServerInfoModule) {
    				moduleSource = (TSServerInfoModule) objectModuleSource;
    			}
    		}
    		
    		try {
    			table = table.replaceAll("(?is)(<td[^>]*>)\\s*(</td>)", "$1&nbsp;$2");
    			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
    			
    			NodeList nodeList = htmlParser.parse(null);
    			
    			TableTag mainTable = null;
    			
    			String linksTable = "";
    			
    			NodeList divList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true);
    			
    			if (divList != null && divList.size() > 0){
    				Div pagesDiv = (Div) divList.extractAllNodesThatMatch(new HasAttributeFilter("id", "ResultsPanel"), true).elementAt(0);
    				if (pagesDiv != null){
    					mainTable = (TableTag) pagesDiv.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"), true).elementAt(0);
    					
    					pagesDiv.getChildren().remove(mainTable);
    					linksTable = pagesDiv.toHtml();
    				}
    			}
    			

    			StringBuilder newTable = new StringBuilder();
    			newTable.append("<table BORDER='1' CELLPADDING='2'>");
    			
    			if (mainTable != null){
	    			TableRow[] rows = mainTable.getRows();
	    			
	    			boolean isBookPageSearch = false;
	    			if (response.getQuerry().contains("SEARCH_BOOK") || response.getQuerry().contains("SEARCH_INSTRUMENT_NUMBER")
	    					|| response.getQuerry().contains("RLBP") || table.contains("action=\"REALSearchByBookPage.aspx\"")){
	    				isBookPageSearch = true;
	    			}
	    			for(int i = 0; i < rows.length; i++ ) {
	    				TableRow row = rows[i];
	    				if (row.getHeaderCount() > 5){
	    					newTable.append(row.toHtml().replaceAll("(?is)(<TR[^>]*>)", "$1 <TH width=\"5%\" align=\"justify\" bgcolor=\"#003090\">" + SELECT_ALL_CHECKBOXES + "</TH><TH>View</TH>"));
	    				}
	    				if(row.getColumnCount() > 5) {
	    					
	    					TableColumn[] cols = row.getColumns();
	    					
	    					NodeList aList = cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"));
	    					if (aList.size() == 0) {
	    						continue;
	    					} else {
	    						
	    						String lnk = "/iRecordClient/" + ((LinkTag) aList.elementAt(0)).getLink().replaceAll("\\s", "");
	    						String nameFromLink = ((LinkTag) aList.elementAt(0)).getChildrenHTML().trim();
	    						String documentNumber = cols[8].toPlainTextString().replaceAll("&nbsp;", " ").trim();
	    						String serverDocType = cols[14].toPlainTextString().replaceAll("&nbsp;", " ").trim();
	    						String tmpBook = cols[10].toPlainTextString().replaceAll("&nbsp;", " ").trim();
	    						String tmpPage = cols[12].toPlainTextString().replaceAll("&nbsp;", " ").trim();
	    						
	    						if (isBookPageSearch){
	    							documentNumber = nameFromLink;
	    							tmpBook = cols[2].toPlainTextString().replaceAll("&nbsp;", " ").trim();
	    							tmpPage = cols[4].toPlainTextString().replaceAll("&nbsp;", " ").trim();
	    							serverDocType = cols[12].toPlainTextString().replaceAll("&nbsp;", " ").trim();
	    						}
	
	    						String key = documentNumber;
	    						if (StringUtils.isNotEmpty(tmpBook)){
	    							key += "_" + tmpBook + "_" + tmpPage;
	    						}
	    							
	    						ParsedResponse currentResponse = responses.get(key);							 
	    						if(currentResponse == null) {
	    							currentResponse = new ParsedResponse();
	    							responses.put(key, currentResponse);
	    						}
	    							
	    						RegisterDocumentI document = (RegisterDocumentI)currentResponse.getDocument();
	    						String tmpPartyGtor = "", tmpPartyGtee = "";
	    						if (isBookPageSearch){
	    							tmpPartyGtor = cols[8].toPlainTextString().replaceAll("&nbsp;", " ").trim();
	    							tmpPartyGtee = cols[10].toPlainTextString().replaceAll("&nbsp;", " ").trim();
	    								
	    						} else {
	    							if (cols[2].toPlainTextString().contains("GTOR")){
	    								tmpPartyGtor = nameFromLink;
	    								tmpPartyGtee = cols[4].toPlainTextString().replaceAll("&nbsp;", " ").trim();
	    							} else if (cols[2].toPlainTextString().contains("GTEE")){
	    								tmpPartyGtee = nameFromLink;
	    								tmpPartyGtor = cols[4].toPlainTextString().replaceAll("&nbsp;", " ").trim();
	    							}
	    						}
	    							
	    						ResultMap resultMap = new ResultMap();
	    							
	    						String link = CreatePartialLink(TSConnectionURL.idGET) + lnk;
	    						if(document == null) {	//first time we find this document
	    							
	    							String recDate = cols[6].toPlainTextString().replaceAll("&nbsp;", " ").trim();
	    								
	    							String legalDescription = "";
	    								
	    							if (isBookPageSearch){
	    								recDate = cols[6].toPlainTextString().replaceAll("&nbsp;", " ").trim();
	    								legalDescription = cols[14].toPlainTextString();
	    							} else {
	    								legalDescription = cols[16].toPlainTextString();
	    							}
	    							resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legalDescription);
	    								
	    							int count = 1;
	    								
	    							String rowHtml =  row.toHtml().replaceAll("(?is)</?a[^>]*>", "");
	    							rowHtml = rowHtml.replaceFirst("(?is)(<TR[^>]*>)\\s*(<TD[^>]*>)","$1\n<TD><a href=\"" + link + "\">View</a></TD>$2");
	    								
	    							tmpPartyGtor = StringEscapeUtils.unescapeHtml(tmpPartyGtor);
	    							tmpPartyGtee = StringEscapeUtils.unescapeHtml(tmpPartyGtee);
	    							resultMap.put("tmpPartyGtor", tmpPartyGtor);
	    							resultMap.put("tmpPartyGtee", tmpPartyGtee);
	    							resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");
	    							resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), tmpPartyGtor);
	    							resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), tmpPartyGtee);
	    							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), documentNumber);
	    							resultMap.put(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName(), documentNumber);
	    							resultMap.put(SaleDataSetKey.BOOK.getKeyName(), tmpBook);
	    							resultMap.put(SaleDataSetKey.PAGE.getKeyName(), tmpPage);
	    							resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), serverDocType);
	    							resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recDate);
	    							try {
	    								ro.cst.tsearch.servers.functions.MOClayRO.parseNameInterMOClayRO(resultMap, searchId);
	    								ro.cst.tsearch.servers.functions.MOClayRO.legalMOClayRO(resultMap, searchId);
	    							} catch (Exception e) {
	    								e.printStackTrace();
	    							}
	    			    			resultMap.removeTempDef();
	    			    				
	    			    			@SuppressWarnings("unchecked")
	    			    			Vector<PropertyIdentificationSet> pisVector = (Vector<PropertyIdentificationSet>) resultMap.get("PropertyIdentificationSet");
	    			    			if (pisVector != null && !pisVector.isEmpty()){
	    			    				for (PropertyIdentificationSet everyPis : pisVector){
	    			    					currentResponse.addPropertyIdentificationSet(everyPis);
	    			    				}
	    			    			}
	    			    				
	    			    			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<table BORDER='1' CELLPADDING='2'>" + 
	    																row.toHtml() + "</table>");
	    																
	    			    			Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
	    			    			document = (RegisterDocumentI) bridge.importData();
	    								
	    			    			currentResponse.setDocument(document);
	    			    			String checkBox = "checked";
	    			    			HashMap<String, String> data = new HashMap<String, String>();
	    			    			data.put("type", serverDocType);
	    			    			data.put("book", tmpBook);
	    			    			data.put("page", tmpPage);
	    								
	    			    			if (isInstrumentSavedInIntermediary(documentNumber, data) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
	    			    				checkBox = "saved";
	    			    				LinkInPage linkInPage = new LinkInPage(CreatePartialLink(TSConnectionURL.idGET) + lnk,
	    					    					                               CreatePartialLink(TSConnectionURL.idGET) + lnk);
	    			    				currentResponse.setPageLink(linkInPage);
	    					    	} else {
	    					    		LinkInPage linkInPage = new LinkInPage(
	    					    				CreatePartialLink(TSConnectionURL.idGET) + lnk, 
	    					    				CreatePartialLink(TSConnectionURL.idGET) + lnk, 
	    					    				TSServer.REQUEST_SAVE_TO_TSD);
	    					    		checkBox = "<input type=\"checkbox\" name=\"docLink\" value=\"" + link + "\">";
	    					    		currentResponse.setPageLink(linkInPage);
	    					    		
	    					    		/**
	    					    		 * Save module in key in additional info. The key is instrument number that should be always available. 
	    					    		 */
	    					    		String keyForSavingModules = this.getKeyForSavingInIntermediary(documentNumber);
	    					    		search.setAdditionalInfo(keyForSavingModules, moduleSource);
	    					    	}
	    			    			rowHtml = rowHtml.replaceAll("(?is)(<TR[^>]*>)", 
	    											"$1<TD  align=\"justify\" width=\"5%\" nowrap><font face=\"Verdana\" size=\"1\" rowspan=" + count + ">" + checkBox + "</TD>");
	    			    			currentResponse.setOnlyResponse(rowHtml);
	    			    			newTable.append(currentResponse.getResponse());
	    								
	    			    			count++;
	    			    			intermediaryResponse.add(currentResponse);
	    							
	    						} else {
	    							
	    							tmpPartyGtor = StringEscapeUtils.unescapeHtml(tmpPartyGtor);
	    							tmpPartyGtee = StringEscapeUtils.unescapeHtml(tmpPartyGtee);
	    							resultMap.put("tmpPartyGtor", tmpPartyGtor);
	    							resultMap.put("tmpPartyGtee", tmpPartyGtee);
	    							resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");
	    							resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), tmpPartyGtor);
	    							resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), tmpPartyGtee);
	    							resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), documentNumber);
	    							resultMap.put(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName(), documentNumber);
	    							resultMap.put(SaleDataSetKey.BOOK.getKeyName(), tmpBook);
	    							resultMap.put(SaleDataSetKey.PAGE.getKeyName(), tmpPage);
	    							
	    							try {
	    								ro.cst.tsearch.servers.functions.MOClayRO.parseNameInterMOClayRO(resultMap, searchId);
	    								ro.cst.tsearch.servers.functions.MOClayRO.legalMOClayRO(resultMap, searchId);
	    							} catch (Exception e) {
	    								e.printStackTrace();
	    							}
	    							@SuppressWarnings("unchecked")
	    							Vector<PropertyIdentificationSet> pisVector = (Vector<PropertyIdentificationSet>) resultMap.get("PropertyIdentificationSet");
	    							if (pisVector != null && !pisVector.isEmpty()){
	    								for (PropertyIdentificationSet everyPis : pisVector){
	    									currentResponse.addPropertyIdentificationSet(everyPis);
	    								}
	    							}
	    							resultMap.removeTempDef();
	    							Bridge bridge = new Bridge(currentResponse,resultMap,getSearch().getID());
	    							RegisterDocumentI documentTemp = (RegisterDocumentI)bridge.importData();
	    			    			
	    							for(NameI nameI : documentTemp.getGrantee().getNames()) {
	    								if(!document.getGrantee().contains(nameI)) {
	    									document.getGrantee().add(nameI);
	    			    				}
	    			    			}
	    							for(NameI nameI : documentTemp.getGrantor().getNames()) {
	    								if(!document.getGrantor().contains(nameI)) {
	    									document.getGrantor().add(nameI);
	    			    				}
	    			    			}
	    							String rawServerResponse = (String)currentResponse.getAttribute(ParsedResponse.SERVER_ROW_RESPONSE);
	    			    				
	    							currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rawServerResponse);
	    			    				
	    							String responseHtml = currentResponse.getResponse();
	    							String countString = StringUtils.extractParameter(responseHtml, "rowspan=(\\d)+");
	    							try {
	    								int count = Integer.parseInt(countString);
	    								responseHtml = responseHtml.replaceAll("rowspan=(\\d)+", "rowspan=" + (count + 1));
	    			    					
	    								currentResponse.setOnlyResponse(responseHtml);
	    			    			} catch (Exception e) {
	    			    				e.printStackTrace();
	    							}
	    						}
	    					}
	    			}
	    		}

	    		// parse and store parameters on search
	    		String page = "";
	    		Form form = new SimpleHtmlParser(table).getForm("FORM_CRITERIA");
	    		if (form != null){
		    		Map<String, String> params = form.getParams();
		    		params.remove("__EVENTTARGET");
		    		params.remove("ResetButton");
		    		params.remove("SearchButton");
		    		int seq = getSeq();
		    		mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
		    		page = form.action;
		    		if (page.contains("?")){
		    			page = page + "&";
		    		} else {
		    			page = page + "?";
		    		}
		    		//page = page.replaceAll("(?is)([^\\?]+).*", "$1");
		    		
		    		String nextLink = "";
	    			linksTable = linksTable.replaceAll("(?is)(href=[\\\"|']?)javascript:__doPostBack\\('([^']+)',\\s*''\\)", 
	    						"$1" + CreatePartialLink(TSConnectionURL.idPOST) + "/iRecordClient/" + page + "pageParam=$2&seq=" + seq);
	    			linksTable = linksTable.replaceAll("(?is)&nbsp;", " ");
	    			
	    			Pattern NEXT_PAT = Pattern.compile("(?i)<a\\s+.*? style=\\\"[^\\\"]+[^>]*>[^<]+</a>\\s*<a\\s+.*?href=\\\"([^\\\"]+)");
	    			Matcher matcher = NEXT_PAT.matcher(linksTable);
	    			if (matcher.find()){
	    				nextLink = "<a href=\"" + matcher.group(1) + "\">Next</a>";
	    				nextLink = nextLink.replaceAll("(?is)<a\\s+class\\s*=\\s*\\\"[^\\\"]*\\\"\\s+title\\s*=\\s*\\\"[^\\\"]*\\\"\\s+", "<a ");
	    				nextLink = nextLink.replaceAll("(?is)&amp;", "&");
	    				
	    				if (getSearch().getSearchType() == Search.AUTOMATIC_SEARCH) {
	    					response.getParsedResponse().setNextLink(nextLink);
	    				}
	    			}
	    		}
	    		String header1 = rows[1].toHtml();
    			header1 = header1.replaceAll("(?is)(<TR[^>]*>)", "$1 <TH width=\"5%\" align=\"justify\">" + SELECT_ALL_CHECKBOXES + "</TH><TH>View</TH>");

    			response.getParsedResponse().setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "GET") 
    								+ "<br>" + linksTable
    					+ "<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header1);
    				
    			response.getParsedResponse().setFooter("</table>" + linksTable +
    							"<br>" +  CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, 101, -1));
    		
    			}
    		newTable.append("</table>");
    		outputTable.append(newTable);
    			
    		}catch(Exception e) {
    			e.printStackTrace();
    		}
    		
    		return intermediaryResponse;
    	}

    	@Override
    	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap map) {
    		ro.cst.tsearch.servers.functions.MOClayRO.parseAndFillResultMap(response, detailsHtml, map, searchId);
    		return null;
    	}
    	
    	protected String getDetails(String response, ServerResponse Response){
    		
    		// if from memory - use it as is
    		if(!response.toLowerCase().contains("<html")){
    			return response;
    		}
    		
    		String details = "";
    		String docNo = "";
    		try {
    			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
    			NodeList mainList = htmlParser.parse(new TagNameFilter("table"));
    			docNo = HtmlParser3.getValueFromNextCell(mainList, "Document No.", "", false).trim();
    			if (mainList != null && mainList.size() > 0){
    				TableTag table = (TableTag) mainList.elementAt(1); 
    				table.removeAttribute("width");
    				table.setAttribute("id", "detailsPage");
    				table.setAttribute("align", "center");
    				details = table.toHtml();
    			}
    			
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    		
	
    		return details;
    	}
    	
    	public static String getDoctypeFromResponse(String response){
    		Matcher ma = DOCTYPE.matcher(response);
    		if (ma.find()){
    			return ma.group(1).replaceAll("\\s{2,}", " ").trim();
    		}
    		return "";
    	}
        
        public TSServerInfoModule getSearchByBookPageModule(TSServerInfo serverInfo, String book, String page, String instrumentNo){
            if("".equals(book) && "".equals(page) && "".equals(instrumentNo)){
                return null;
            }
            
            TSServerInfoModule retVal = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));

            retVal.getFunction(0).setSaKey("");
            retVal.getFunction(1).setSaKey("");
            retVal.getFunction(2).setSaKey("");
            retVal.getFunction(3).setSaKey("");
            retVal.getFunction(4).setSaKey("");
            retVal.getFunction(5).setSaKey("");
            
            if(!"".equals(book) && !"".equals(page)){
                retVal.getFunction(0).setParamValue(book);
                retVal.getFunction(1).setParamValue(page);
            }
            else if(!"".equals(instrumentNo)){
                retVal.getFunction(2).setParamValue(instrumentNo);
            }
            
            return retVal;
        }
        
       /* public ServerResponse FollowLink(String vsRequest, String imagePath) throws ServerResponseException {
        	ServerResponse sr = null;
        
        	if( vsRequest.contains( "ImageCache" ) ) {
        		sr = super.FollowLink(vsRequest + "&atmpt=1", imagePath);
        		
        		if(sr.isError()) {
        			//make a search for this document
        			
        			try {
        				String docPk = StringUtils.getTextBetweenDelimiters("ImageCache/", ".", vsRequest);
        				
        				HTTPRequest req = new HTTPRequest( "http://recorder.claycogov.com/iRecordClient/REALSummary.aspx?INSTRUMENT_PK=" + docPk );
        				HTTPResponse res = HTTPSiteManager.pairHTTPSiteForTSServer( "MOClayRO" ,searchId,miServerID).process( req ); 
        				
        				String siteResponse = res.getResponseAsString();
        				
                        Matcher imageLinkMatch = viewImagePattern.matcher( siteResponse );
                        String imgLink = null;
                        String linkURL = null;
                                                
                        if( imageLinkMatch.find() )
                        {
                            imgLink = imageLinkMatch.group(1);
                            imgLink = "http://recorder.claycogov.com/iRecordClient/" + imgLink;
                           
                            try
                            {
                                req = new HTTPRequest( imgLink );
                                res = HTTPSiteManager.pairHTTPSiteForTSServer( "MOClayRO" ,searchId,miServerID).process( req );
                                
                                siteResponse = res.getResponseAsString();
                            }
                            catch( Exception e )
                            {
                                
                            }
                            
                            
                            Matcher imageLink = imageLinkPattern.matcher( siteResponse );
                            
                            if( imageLink.find() )
                            {
                                linkURL = imageLink.group( 1 );
                                imgLink = CreatePartialLink(TSConnectionURL.idGET) + linkURL;
                            }                            
                        }
        				
        				sr = super.FollowLink(vsRequest + "&atmpt=2", imagePath);
        			} catch(Exception e) {
        				e.printStackTrace();
        			}
        		}
        	} else {
        		return super.FollowLink( vsRequest, imagePath );        		
        	}
        	
        	return sr;
        }*/
        
        public ServerResponse GetLink(String vsRequest, boolean vbEncoded)throws ServerResponseException {
        	ServerResponse sr = null;
        	if (vsRequest.contains("ImageCache")) {
        		ImageI i = new Image();
        		i.setContentType("application/pdf");
        		String filename = "" + (new Random()).nextInt(900000000) + ".pdf";
        		i.setFileName(filename);
        		i.setPath(getSearch().getImagesTempDir() + filename);
        		i.setType(IType.PDF);
        		i.setExtension("pdf");
        		Set<String> links =  new HashSet<String>();
        		links.add(vsRequest.substring(vsRequest.indexOf("ImageCache")));
        		i.setLinks(links);
        		
        		DownloadImageResult result = saveImage(i);
        		
        		if(result != null && Status.OK.equals(result.getStatus())){
        			writeImageToClient(i.getPath(), i.getContentType());
        			return null;
        		}
        		
        	} else {
        		return super.GetLink(vsRequest, vbEncoded);
        	}
        	
        	return sr;
        	
        }
        
    	public static boolean retrieveImageFromLink(String link, String fileName, long searchId){

    		// check whether the file exists
    		if(FileUtils.existPath(fileName)){
    			return true;
    		}

    		HttpSite site = HttpManager.getSite("MOClayRO", searchId);
    		try{
    			//URL=http://recorder.claycogov.com/ImageCache/20020047130.pdf---> exemplu de request
	    	    HTTPRequest req = new HTTPRequest( "http://recorder.claycogov.com/ImageCache/" + fileName + ".pdf" );
	    	    req.setMethod(HTTPRequest.GET);
	    	    HTTPResponse res = site.process(req);
	    		FileUtils.CreateOutputDir(fileName);
	    		FileUtils.writeStreamToFile(res.getResponseAsStream(), fileName);
    		} 
			catch(Exception e)
		    {
				logger.error(e);
		    }
		    finally 
		    {
		    	HttpManager.releaseSite(site);
		    }
    		// return result
    		return FileUtils.existPath(fileName);
    	 
    	}
    	        
	@Override
	protected void setCertificationDate() {

        try {
        	
        	if(false) {
        		if (CertificationDateManager.isCertificationDateInCache(dataSite)){
    				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
    				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
    			} else{	
		        	String html = "";
		
		        	HTTPSite site = HTTPManager.getSite("MOClayRO", searchId, miServerID);
		    	    HTTPRequest req = new HTTPRequest("http://recorder.claycogov.com/iRecordClient/REALSearchByName.aspx");
		    	    req.setHeader("Host", "recorder.claycogov.com");
		    	    HTTPResponse res = site.process(req);
		    	    html = res.getResponseAsString();
		    	    	
		            Matcher certDateMatcher = certDatePattern.matcher(html);
		            if(certDateMatcher.find()) {
		            	String date = certDateMatcher.group(1).trim();
		            	
		            	CertificationDateManager.cacheCertificationDate(dataSite, date);
		            	getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
		            }
    			}
        	}
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
	}
	
	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		String instrumentNumber = restoreDocumentDataI.getInstrumentNumber();
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		
		TSServerInfoModule module = null;
		if(StringUtils.isNotEmpty(instrumentNumber)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
		    module.forceValue(0, "");
			module.forceValue(1, "");
			module.forceValue(2, instrumentNumber);
			module.forceValue(3, "Search");
			module.forceValue(4, "RBTN_SORT_INSTRUMENT");
			module.forceValue(5, "");
		} else if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
		    module.forceValue(0, book);
			module.forceValue(1, page);
			module.forceValue(2, "");
			module.forceValue(3, "Search");
			module.forceValue(4, "RBTN_SORT_INSTRUMENT");
			module.forceValue(5, "");
			
			HashMap<String, String> filterCriteria = new HashMap<String, String>();
			filterCriteria.put("Book", book);
			filterCriteria.put("Page", page);
			GenericInstrumentFilter filter = new GenericInstrumentFilter(searchId, filterCriteria);
			module.getFilterList().clear();
			module.addFilter(filter);
		} 
		
		return module;
	}
	
	/**
	 * Looks for the a document having the same instrumentNo
	 * 
	 * @param instrumentNo
	 * @param data
	 * @return
	 */
	public boolean isInstrumentSavedInIntermediary(String instrumentNo,
			HashMap<String, String> data) {
		if (StringUtils.isEmpty(instrumentNo))
			return false;

		boolean firstTry = super.isInstrumentSaved(instrumentNo, null, data);

		if (firstTry) {
			return true;
		}

		DocumentsManagerI documentManager = getSearch().getDocManager();
		try {
			documentManager.getAccess();
			boolean validServerDoctype = false;
			String docCateg = null;

			InstrumentI instr = new com.stewart.ats.base.document.Instrument(
					instrumentNo);
			if (data != null) {

				if (!StringUtils.isEmpty(data.get("type"))) {
					String serverDocType = data.get("type");
					if (serverDocType.length() == 3) {
						validServerDoctype = true;
						docCateg = DocumentTypes.getDocumentCategory(
								serverDocType, searchId);
						instr.setDocType(docCateg);
						instr.setDocSubType(DocumentTypes
								.getDocumentSubcategory(serverDocType, searchId));
					} else {
						// in some intermediary we do not have the full document
						// type so we need to force ATS to ignore category
						docCateg = DocumentTypes.MISCELLANEOUS;
						instr.setDocType(docCateg);
						instr.setDocSubType(DocumentTypes.MISCELLANEOUS);
					}

				}

				instr.setDocno(data.get("docno"));
			}

			try {
				instr.setYear(Integer.parseInt(data.get("year")));
			} catch (Exception e) {
			}

			if (documentManager.getDocument(instr) != null) {
				return true; // we are very lucky
			} else {
				List<DocumentI> almostLike = documentManager
						.getDocumentsWithInstrumentsFlexible(false, instr);
				if (almostLike.size() == 0) {
					return false;
				}
				if (data != null) {
					if (StringUtils.isNotEmpty(docCateg)) {
						for (DocumentI documentI : almostLike) {
							if ((!validServerDoctype || documentI.getDocType()
									.equals(docCateg))) {
								return true;
							}
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}
		return false;
	}
	
	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
    	if(StringUtils.isEmpty(instrumentNo))
    		return false;
    	
    	/* The AO,TR document must be re-saved in date down searches: Bug 4584 */
    	if(mSearch.getSa().isDateDown() && isAssessorOrTaxServer()) {
    		return false;
    	}
    	
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		if(documentToCheck != null) {
    			if(documentManager.getDocument(documentToCheck.getInstrument()) != null)
    				return true;
    		} else {
	    		InstrumentI instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
	    		if(data != null) {
		    		if(!StringUtils.isEmpty(data.get("type"))) {
		        		String serverDocType = data.get("type");
		    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId); 
		            	instr.setDocType(docCateg);
		            	instr.setDocSubType(DocumentTypes.getDocumentSubcategory(serverDocType, searchId));
		    		}
		    		
		    		instr.setBook(data.get("book"));
		    		instr.setPage(data.get("page"));
		    		instr.setDocno(data.get("docno"));
	    		}
	    		
	    		try {
	    			instr.setYear(Integer.parseInt(data.get("year")));
	    		} catch (Exception e) {}
	    		
	    		if(documentManager.getDocument(instr) != null) {
	    			return true;
	    		} else {
	    			List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
	    			
	    			if(checkMiServerId) {
		    			boolean foundMssServerId = false;
	    				for (DocumentI documentI : almostLike) {
	    					if(miServerID==documentI.getSiteId()){
	    						foundMssServerId  = true;
	    						break;
	    					}
	    				}
		    			
	    				//task 8104
	    				if (instrumentNo.matches("[A-Z]\\d{5}")) {
		    				instrumentNo = instrumentNo.substring(instrumentNo.length()-5);
		    				instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
				    		if(data != null) {
					    		instr.setBook(data.get("book"));
					    		instr.setPage(data.get("page"));
					    		instr.setDocno(data.get("docno"));
				    		}
				    		try {
				    			instr.setYear(Integer.parseInt(data.get("year")));
				    		} catch (Exception e) {}
				    		List<DocumentI> documents = documentManager.getDocumentsList();
				    		for (DocumentI document: documents) {
				    			String instNo = document.getInstno();
				    			if (instNo.matches("\\d{6,7}")) {
				    				instNo = instNo.substring(instNo.length()-5);
				    				DocumentI newDoc = document.clone();
				    				newDoc.setInstno(instNo);
				    				if (newDoc.flexibleEquals(instr)) {
				    					return true;
				    				}
				    			}	
				    		}
		    			}
	    					    				
	    				if(!foundMssServerId){
	    					return false;
	    				}
	    			}
	    			
    				if(data!=null) {
    					if(!StringUtils.isEmpty(data.get("type"))){
			        		String serverDocType = data.get("type"); 
			    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
			    	    	String dataSource = data.get("dataSource");
			    	    	for (DocumentI documentI : almostLike) {
			    	    		if (serverDocType.equals("ASSESSOR") && dataSource != null) {
									if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))//B 4435, must save NDB and ISI doc of the same instrNo
										return true;
			    	    		} else if (serverDocType.equals("CNTYTAX") && dataSource != null) {
			    	    			if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))
										return true;
			    	    		} else if (serverDocType.equals("CITYTAX") && dataSource != null) {
			    	    			if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))
										return true;
			    	    		}else if( (!checkMiServerId || miServerID==documentI.getSiteId()) && documentI.getDocType().equals(docCateg)){
									return true;
			    	    		}
							}
			    	    	
    					}
		    		} else {
		    			EmailClient email = new EmailClient();
		    			email.addTo(MailConfig.getExceptionEmail());
		    			email.setSubject("isInstrumentNoSaved problem on " + URLMaping.INSTANCE_DIR + this.getClass().getName());
		    			email.addContent("We should at least have type!!!!\nSearchId=" + searchId);
		    			email.sendAsynchronous();
		    		}
	    		}
    		}
    		
    	} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}
    	return false;
    }
        
}