package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterResponse2;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ParcelIdIterator;
import ro.cst.tsearch.search.iterator.PlatBookPageIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.MatchEquivalents;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class GenericSSF extends TSServerSSF{

	private static final long serialVersionUID = -6500809246514638719L;
	
	public GenericSSF(long searchId) {
		super(searchId);
	}

	public GenericSSF(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid){
		super(rsRequestSolverName,rsSitePath,rsServerID,rsPrmNameLink,searchId,mid);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void ParseResponse(String moduleIdx, ServerResponse response, int viParseID) throws ServerResponseException {
		ParsedResponse pr = response.getParsedResponse();		
		switch (viParseID){
			case ID_SAVE_TO_TSD:{
					String html = removeFormatting(pr.getResponse());
			        String instrNo = pr.getDocument().getInstno().replaceAll("[ \t\r\n]+","");
			        
			        if (moduleIdx.endsWith("_IMG")) {	//Image Search
			        	if (StringUtils.isBlank(instrNo)) {
	            			instrNo = pr.getDocument().getBook() + "-" + pr.getDocument().getPage();
	            					 
	            		}
			        	instrNo += "_" + pr.getDocument().getYear() + "_" + pr.getDocument().getDocType() + "_IMG";
			        }
		        	
		        	// set file name
		            msSaveToTSDFileName = instrNo + ".html";     
		            pr.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
		       
		            // save to TSD
		            msSaveToTSDResponce = html + CreateFileAlreadyInTSD(true);            
		            parser.Parse(pr, html, Parser.NO_PARSE, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
		        }
		    break;
			case ID_DETAILS:
				{
					boolean isAlreadySaved = false;
					long countSavedRows = 0;
					
			    	// parse all records
		            for ( ParsedResponse item: (Vector<ParsedResponse>)pr.getResultRows()) {            	
		            	String itemHtml = item.getResponse();
		            	String shortType = "SF";
		            	
		            	item.setParentSite(pr.isParentSite());
		            	item.setResponse( itemHtml );
		            	String instrNo="";
		            	String origInstrNo="";
		            	HashMap<String, String> data = new HashMap<String, String>();
		            	instrNo = item.getDocument().getInstno();  
		            		
		            	if (moduleIdx.equals(Integer.toString(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX))) {	//Image Search
		            		if (StringUtils.isBlank(instrNo)) {
		            			instrNo = item.getBook() + "-" + item.getPage(); 
		            		}
		            		origInstrNo = instrNo;
		            		instrNo += "_" + item.getYear() + "_" + item.getDocument().getDocType() + "_IMG";
		            	}
		            	
		            	if(StringUtils.isBlank(instrNo)){
		            		logger.warn(searchId + ": Document from SSF has NO Instrument number. It has been skipped!");;           		
		            		continue;
		            	}
		            	instrNo = instrNo.replaceAll("[ \t\r\n]+","");
		            	
		            	// create links
		            	String originalLink = PREFIX_FINAL_LINK + instrNo;  
		            	String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;   
			               
			            msSaveToTSDFileName = instrNo + ".html";     
			            
			            pr.setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
		            	String checkbox = "";
		            		            	
		            	boolean isSaved = false;
		            	DocumentI docSaved = item.getDocument();
		            	if (moduleIdx.equals(Integer.toString(TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX))) {	//Image Search
		            		//isSaved = this.isInstrumentSaved(origInstrNo, docSaved, data, false); 
		            		isSaved = super.isInstrumentSaved(origInstrNo, docSaved, data, false);
		            	} else {
		            		isSaved = super.isInstrumentSaved(instrNo, docSaved, data);
		            	}
		            	if (isSaved) {
	            			checkbox = "saved";
	            			countSavedRows++;
	            			isAlreadySaved = true;
	            		} else {
	            			checkbox = "<input type='checkbox' name='docLink' value='" + sSave2TSDLink + "'>";
	            			if(mSearch.getInMemoryDoc(sSave2TSDLink)==null){
	            				mSearch.addInMemoryDoc(sSave2TSDLink, item);
	            			}
	            		}
		                itemHtml =	"<tr> <td valign=\"center\" align=\"center\">" + checkbox + "</td> <td align=\"center\"><b>" + shortType + 
		                			"</b></td><td>" + itemHtml + "</td><tr>"; 
		                item.setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
		                parser.Parse(item, itemHtml,Parser.NO_PARSE, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
		            }
		            
		            if (mSearch.getSearchType() == Search.PARENT_SITE_SEARCH ) {
		            	String header = StringUtils.defaultString(pr.getHeader(), "");
		               	String footer = StringUtils.defaultString(pr.getFooter(), "");                           	
		            	header += CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
		            	header += "\n<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n<tr bgcolor=\"#cccccc\">\n<th width=\"1%\">" +
		            			"<div>" + SELECT_ALL_CHECKBOXES +"</div></th> " +
		            			"<th width=\"1%\">Type</th> \n<th width=\"98%\" align=\"left\">Document</th> \n</tr>";
		            	
		            	
		            	if(!footer.isEmpty()) {
		            		footer = "<tr><td colspan=\"3\">" + footer + "</td></tr>";
		            	}
		            	
		            	if (isAlreadySaved && pr.getResultsCount() == countSavedRows) {
		            		footer += "\n</table>" + CreateFileAlreadyInTSD();       
		            	} else { 
		            		footer += "\n</table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);        
		            	}
		            	
		            	pr.setHeader(header);
		            	pr.setFooter(footer);
		            }
				}
			break;
		}
	}

	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
    	if(StringUtils.isEmpty(instrumentNo))
    		return false;
    	
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		
    		InstrumentI instr = null;
    		if(documentToCheck != null) {
    			instr = documentToCheck.getInstrument();
    		} else {
    			instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
    		}
    		List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
	    			
	    	if(checkMiServerId) {
		    	for (DocumentI documentI : almostLike) {
	    			if(miServerID==documentI.getSiteId()){
	    				return true;
	    			}
	    		}
		    		
	    		return false;
	    	
	    	}
	    	
    	} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}
    	return false;
    }

	@Override
	protected ServerResponse performRequest(String page, int methodType, String action, int parserId, String imageLink, String vbRequest, Map<String, Object> extraParams)
			throws ServerResponseException {
		return new ServerResponse();
	}


	public void setServerID(int serverID) {
		super.setServerID(serverID);
		setRangeNotExpanded(true);
	}

   private static final String getEquivalent(String subdivisionname){
	   if(subdivisionname==null){
		   return "";
	   }
	   String str = subdivisionname.replaceAll("(?i)subdivision", "").replaceAll("[ \t\n\r]+", " ").trim();
	   str = str.replaceAll("(?i)F[0-9 /t/r/n,]+", " ");
	   str = str.replaceAll("(?i)FIL[I]?[N]?[G]?\\s*[0-9]*$", "");
	   str = str.replaceAll("(?i)SUB[D]?[I]?[V]?[I]?[S]?[I]?[O]?[N]?\\s*[0-9]*$", "");
	   str = str.replaceAll("TWNHMS", "TOWNHOMES");
	   
	   int comaPos = str.indexOf(',');
	   
	   if(comaPos>0){
		   str = str.substring(0,comaPos);
	   }
	   
	   int atPos = str.indexOf('@');
	   
	   if(atPos>0){
		   str = str.substring(0,atPos);
	   }
	   
	   str = str.trim();
	   
	   return str;
   }
   
   protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<Object> modules = new ArrayList<Object>();
		TSServerInfoModule module = null;
		
		GenericLegal defaultLegalFilter = (GenericLegal) LegalFilterFactory.getDefaultLegalFilter(searchId);
		FilterResponse subdivNameFilter = NameFilterFactory.getDefaultNameFilterForSubdivision(searchId);
		FilterResponse adressFilter 	= AddressFilterFactory.getAddressHighPassFilter( searchId , 0.8d );
		
		FilterResponse cityFilter = CityFilterFactory.getCityFilterDefault(searchId);
		FilterResponse rejectSavedDocuments = new RejectAlreadySavedDocumentsFilterResponse(searchId);
		FilterResponse pinFilter = PINFilterFactory.getDefaultPinFilter(searchId);
		
		FilterResponse []filtersSectional = {defaultLegalFilter,cityFilter, adressFilter, rejectSavedDocuments};
		FilterResponse []filtersForSubdivName = {subdivNameFilter,defaultLegalFilter,cityFilter,adressFilter,rejectSavedDocuments};
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		String aoAndTrLots = global.getSa().getAtribute(SearchAttributes.LD_LOTNO);
		HashSet<String> allAoAndTrlots = new HashSet<String>();
		
		String aoAndTrBloks = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
		HashSet<String> allAoAndTrBloks = new HashSet<String>();
		
		if(aoAndTrLots.contains(",")||aoAndTrLots.contains(" ")||aoAndTrLots.contains("-")||aoAndTrLots.contains(";")){
			if(!StringUtils.isEmpty(aoAndTrLots)){
				for (LotInterval interval:LotMatchAlgorithm.prepareLotInterval(aoAndTrLots)) {
					allAoAndTrlots.addAll(interval.getLotList());
				}
			}
		}else{
			if(!StringUtils.isEmpty(aoAndTrLots)){
				allAoAndTrlots.add(aoAndTrLots);
			}
		}
		
		if(!StringUtils.isEmpty(aoAndTrBloks)){
			allAoAndTrBloks.addAll( Arrays.asList(aoAndTrBloks.split("[ /t,-]")));
		}

		if(allAoAndTrBloks.size()==0){
    		allAoAndTrBloks.add("");
    	}
    	
    	if(allAoAndTrlots.size()==0){
    		allAoAndTrlots.add("");
    	}
		
		//parcel id search
		{
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.clearSaKeys();
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_PARCELID_FAKE);
			module.forceValue(1, "START");
			module.addFilter(pinFilter);
			module.addFilter(cityFilter);
			module.addFilter(rejectSavedDocuments);
			
			ParcelIdIterator it = new ParcelIdIterator(searchId);
			it.setCheckDocumentSource("SF");
			module.addIterator(it);
			modules.add(module);
        }
		
        {	
        	// P2: search by address
        	//address search
			String strNo   = getSearchAttribute(SearchAttributes.P_STREETNO);
			
        	if(!StringUtils.isEmpty(strNo))	{
				// construct the list of street names
				String tmpName = getSearchAttribute(SearchAttributes.P_STREETNAME).trim();
				Set<String> strNames = new LinkedHashSet<String>(); 
				if(!StringUtils.isEmpty(tmpName)){
					
					if(tmpName.toUpperCase().startsWith("VACANT")) {
						tmpName = "";
					} else {
						strNames.add(tmpName);
					}
				}
				
				//we have cases when they put "." in the name of the street St.Jhons
				tmpName = tmpName.replace(".", " ").replaceAll("\\s{2,}", " ").trim();
				if(!StringUtils.isEmpty(tmpName)){
					strNames.add(tmpName);
				}
			
				for(String strName: strNames){
					
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
					module.clearSaKeys();
					module.forceValue( 0 , strNo );
					module.forceValue( 1 , strName );
					module.forceValue(6, "START");
					module.addFilter( adressFilter );
					module.addFilter( cityFilter );
					module.addFilter( defaultLegalFilter );
					module.addFilter(rejectSavedDocuments);
					modules.add(module);
					
					
//					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
//					module.clearSaKeys();
//					module.forceValue( 0 , "" );
//					module.forceValue( 1 , strName );
//					module.forceValue(6, "START");
//					module.forceValue(8, "BASE");
//					module.addFilter( streetNameFilter );
//					module.addFilter( cityFilter );
//					module.addFilter( defaultLegalFilter );
//					module.addFilter(rejectSavedDocuments);
//					modules.add(module);
				}
			}//end address search
        }
        
        boolean doneLotSearch = false;
        
        {  
        	
        	// subdivided search with PB. PP. (no subdiv name, no lot, no block)
            addSubdivisionSearch(serverInfo, "","","","BASE", modules, defaultLegalFilter, cityFilter, rejectSavedDocuments);
            
            
            //subdivided search with Subdivision Name
            String subdivname = getSearchAttribute( SearchAttributes.LD_SUBDIV_NAME );
            if(StringUtils.isNotBlank(subdivname)){
            	Set<String> allSubdivs = new HashSet<String>();
            	String equivalents[] = {subdivname, MatchEquivalents.getInstance(searchId).getEquivalent(subdivname), getEquivalent(subdivname)};
                for(String equivalent:equivalents){
                	if(StringUtils.isNotBlank(equivalent)){
                		allSubdivs.add(equivalent);
                	}
                }
                
                for(String subdivname1:allSubdivs){
                	 addSubdivisionSearch(serverInfo, subdivname1, "", "", "BASE", modules, subdivNameFilter, defaultLegalFilter, cityFilter, rejectSavedDocuments);
                }
                
                for(String subdivname1:allSubdivs){
//                	for(String block:allAoAndTrBloks){
                		for(String lot:allAoAndTrlots){
                			//if(StringUtils.isNotBlank(lot+block)){
                			if(StringUtils.isNotBlank(lot)){
                				doneLotSearch = true;
                				
                				addSubdivisionSearch(serverInfo, "", lot, "", "PRIOR", modules, filtersSectional);
                				addSubdivisionSearch(serverInfo, subdivname1, lot, "", "PRIOR", modules, filtersForSubdivName);
                			}
//                			if(StringUtils.isNotBlank(lot)){
//                				addSubdivisionSearch(serverInfo, subdivname1, lot, block, "ALL", modules, filtersForSubdivName);
//                			} else {
//                				if(StringUtils.isNotBlank(block)){
//                					addSubdivisionSearch(serverInfo, subdivname1, lot, block, "BASE", modules, filtersForSubdivName);
//                				}
//                			}
                		}
//                	}
                }
            }
        }
        
        if(!doneLotSearch)
        {  //sectional search
        	String secStr = getSearchAttribute(SearchAttributes.LD_SUBDIV_SEC);
        	String tw = getSearchAttribute(SearchAttributes.LD_SUBDIV_TWN);
        	String rg = getSearchAttribute(SearchAttributes.LD_SUBDIV_RNG);
        	String arb = getSearchAttribute(SearchAttributes.ARB);
        	
        	if(!StringUtils.isBlank(tw)&&!StringUtils.isBlank(rg)){
	        	for(String sec:secStr.split("[ ,-]+")){
	        		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ARB_MODULE_IDX));
	            	module.clearSaKeys();
	            	module.forceValue(0, sec);
	            	module.forceValue(1, tw);
	            	module.forceValue(2, rg);
	            	module.forceValue(5, arb);
	            	module.forceValue(6, "START");
	            	for(FilterResponse fil:filtersSectional){
	         		   module.addFilter(fil);
	         	   }
	    			modules.add(module);
	        	}
        	}
        }
        
		serverInfo.setModulesForAutoSearch(modules);	
   }
   
   private  void addSubdivisionSearch(TSServerInfo serverInfo, String subdivname, String lot, String block, String fileType, List<Object> modules, FilterResponse ... filters){
	   TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
	   module.clearSaKeys();
	   module.forceValue(0, lot);
	   module.forceValue(1, block);
	   module.forceValue(8, fileType);
	   
	   for(FilterResponse fil:filters){
		   module.addFilter(fil);
	   }
	   
	   if(StringUtils.isNotBlank(subdivname)){
		   module.forceValue(5, subdivname);
		   module.forceValue(6, "START");
	   }else{
		   module.forceValue(6, "START");
		   module.setIteratorType(2,  FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
		   module.setIteratorType(3,  FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
		   module.addIterator(new PlatBookPageIterator(mSearch.getID()));
	   }
	   modules.add(module);
	   
   }
}