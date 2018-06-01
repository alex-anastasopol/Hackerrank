/*
 * Created on Feb 23, 2005
 */

package ro.cst.tsearch.servers.types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator.SEARCH_WITH_TYPE;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.StringUtils;


public class TNShelbyAO extends TSServer {
	
	static final long serialVersionUID = 10000000;
	
	private boolean downloadingForSave;	

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<Object> l = new ArrayList<Object>();
		
		TSServerInfoModule m = null;
		
		GenericNameFilter nameFilterHybrid = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT,
				searchId, m);
		nameFilterHybrid.setUseSynonymsForCandidates(true);

		FilterResponse addressFilter = AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d, true);
		
		if( hasPin() ){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			m.clearSaKeys();
			m.forceValue(0, getSearchAttribute(SearchAttributes.LD_PARCELNO).replaceAll("-", ""));
			l.add(m);
		}
   
		if( hasStreet() ){
	    	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
	    	m.setIteratorType(ModuleStatesIterator.TYPE_ADDRESS);
	    	m.addFilter(addressFilter);
	    	m.addFilter(nameFilterHybrid);
	    	l.add(m);
		}
        
        if( hasOwner()){

			// for person names
        	m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.clearSaKeys();
			GenericNameFilter nameFilterHybridDoNotSkipUnique = (GenericNameFilter) NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , m );
			nameFilterHybridDoNotSkipUnique.setUseSynonymsForCandidates(true);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			nameFilterHybridDoNotSkipUnique.setIgnoreMiddleOnEmpty(true);
			
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybridDoNotSkipUnique);
			
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(m, searchId, new String[] {"L;F;"});
			nameIterator.setSearchWithType(SEARCH_WITH_TYPE.PERSON_NAME);
			nameIterator.clearSearchedNames();
			nameIterator.setInitAgain(true);
			m.addIterator(nameIterator);
			l.add(m);

			// for company names
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX38));
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.clearSaKeys();
			nameFilterHybridDoNotSkipUnique = (GenericNameFilter) NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
			nameFilterHybridDoNotSkipUnique.setUseSynonymsForCandidates(true);
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			nameFilterHybridDoNotSkipUnique.setIgnoreMiddleOnEmpty(true);
			
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybridDoNotSkipUnique);

			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
					.getConfigurableNameIterator(m, searchId, new String[] { "L;;" });
			nameIterator.setSearchWithType(SEARCH_WITH_TYPE.COMPANY_NAME);
			nameIterator.clearSearchedNames();
			nameIterator.setInitAgain(true);
			m.addIterator(nameIterator);
			l.add(m);
        }
		
		serverInfo.setModulesForAutoSearch(l);
	}

	/**
	 * 
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 * @param searchId
	 * @param mid
	 */
	public TNShelbyAO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	static final Pattern PAT_OWNER_TABLE  = Pattern.compile("(?i)<table[ \t\n]+id[ \t\n]*=[ \t\n]*\"owner_info\"");
	static final Pattern PAT_ASSES_TABLE  = Pattern.compile("(?i)<table[ \t\n]+id[ \t\n]*=[ \t\n]*\"assess_info\"");
	static final Pattern PAT_DWELING_TABLE = Pattern.compile("(?i)<table[ \t\n]+id[ \t\n]*=[ \t\n]*\"dwell_info\"");
	static final Pattern PAT_SALE_TABLE = Pattern.compile("(?i)<table[^>]*id[ \n\t]*=[ \n\t]*\"dgList\"[^>]*>");
	static final Pattern PAT_DISCLAIMER = Pattern.compile("(?i)<table[^>]*class=\"PropertySearchDetail-Disclaim\"[^>]*>");
	
	
	private static final int getEndTablePos(int start,String rsResponse,boolean useLastIndexOf){
		int end = -1 ;
		if(useLastIndexOf){
			end	= rsResponse.lastIndexOf("</table>");
			if( end<0 ){
				end = rsResponse.lastIndexOf("</TABLE>");
			}
		}
		else{
			end	= rsResponse.indexOf("</table>",start);
			if( end<0 ){
				end = rsResponse.indexOf("</TABLE>",start);
			}
		}
		return end;
	}
	
	private static final void appendTable(Matcher mat, String rsResponse, StringBuffer finalResult, boolean useLastIndexOf){
		if(mat.find()){
			int start = mat.start();
			int end = getEndTablePos(start,rsResponse,useLastIndexOf);
			if(start>0 && end>0 && start<end){
				String tmp = rsResponse.substring(start, end);
				tmp = tmp.replaceAll("(?is)<a[^>]+>([^<]+)</a>", "$1");
				finalResult.append(tmp);
				finalResult.append("\n</table>");
			}
		}
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String sTmp = "";
		String rsResponse = Response.getResult();
		String initialResponse = rsResponse;
		int istart = -1, iend = -1;
		
		switch (viParseID) {
		
			case ID_SEARCH_BY_NAME :
			case ID_SEARCH_BY_ADDRESS :
			case ID_SEARCH_BY_PARCEL :
			case ID_SEARCH_BY_SUBDIVISION_NAME :
			case ID_SEARCH_BY_MODULE38:
			    
			    if (rsResponse.indexOf("Your search did not yield any results") != -1)
	                return;
	            if (rsResponse.indexOf("The page you requested could not be found") != -1)
	                return;
	            if (!StringUtils.extractParameter(rsResponse, "(?is)(Enter\\s+your\\s+search\\s+using\\s+one\\s+of\\s+these\\s+criteria\\s*:)").isEmpty()) {
					return;
				}
			    
				istart = rsResponse.indexOf("<table class=\"PropertySearchPage-GridStyle\"");
				if (istart!=-1) {
				    iend = rsResponse.indexOf("</table>",istart);
				}
				
				if ((istart!=-1) && (iend!=-1)) {
				    iend = iend + "</table>".length();
				    rsResponse = rsResponse.substring(istart,iend);
				}
				
				sTmp = CreatePartialLink(TSConnectionURL.idGET);
                rsResponse = rsResponse.replaceAll( "<a href=\"([^\"]*)\\?([^\"]*)\">", "<a href=\"" + sTmp + "/$1&$2\">" );
                
                // linkuri de next si previous                
                int isl = -1, iel = -1;
                isl = initialResponse.indexOf("<a id=\"PageNavigator1_hypNext\" title=\"Next Page in List\"");
                iel = initialResponse.indexOf(">",isl+1);
                String linkNext="";
                if ((isl!=-1) && (iel!=-1)) {
                    linkNext = initialResponse.substring(isl,iel+1);
                }
                linkNext = linkNext.replaceAll("id=\".*?\"","");
                linkNext = linkNext.replaceAll("title=\".*?\"","");
                linkNext = linkNext.replaceAll( "<a\\s+href=\"([^\"]*)\\?([^\"]*)\">", "<a href=\"" + sTmp + "/$1&$2\">" );
                linkNext = linkNext.replaceAll("http://www.assessor.shelby.tn.us/","");
                linkNext = linkNext.replaceAll( "&amp;", "&" );
                linkNext = linkNext + "Next" + "</a>";
                
                
                String linkPrevious="";
                
                isl = -1; iel = -1;
                isl = initialResponse.indexOf("<a id=\"PageNavigator1_hypPrev\" title=\"Previous Page in List\"");
                iel = initialResponse.indexOf(">",isl+1);
                
                if ((isl!=-1) && (iel!=-1)) {
                    linkPrevious = initialResponse.substring(isl,iel+1);
                }
                linkPrevious = linkPrevious.replaceAll("id=\".*?\"","");
                linkPrevious = linkPrevious.replaceAll("title=\".*?\"","");
                linkPrevious = linkPrevious.replaceAll( "<a\\s+href=\"([^\"]*)\\?([^\"]*)\">", "<a href=\"" + sTmp + "/$1&$2\">" );
                linkPrevious = linkPrevious.replaceAll("http://www.assessor.shelby.tn.us/","");
                linkPrevious = linkPrevious.replaceAll( "&amp;", "&" );
                linkPrevious = linkPrevious + "Previous" + "</a>";
              
                // has previous and next
                boolean hasNext = false, hasPrevious=false;
                String crtPage="";
                String allPages="";
                isl=-1;
                iel=-1;
                isl = initialResponse.indexOf("<input name=\"PageNavigator1:txtPage\" type=\"text\" value=\"");
                if (isl == -1){
                	isl = initialResponse.indexOf("<input name=\"PageNavigator1$txtPage\" type=\"text\" value=\"");
                }
                isl = isl+ "<input name=\"PageNavigator1:txtPage\" type=\"text\" value=\"".length();
                iel = initialResponse.indexOf("\"",isl);
                crtPage = initialResponse.substring(isl,iel);
                
                isl = -1;
                iel = -1;
                isl = initialResponse.indexOf("of <span id=\"PageNavigator1_spnPages\">");
                isl = isl+ "of <span id=\"PageNavigator1_spnPages\">".length();
                iel = initialResponse.indexOf("<",isl);
                allPages = initialResponse.substring(isl,iel);
                int nrCrtPage=0;
                int nrAllPages=0;
                nrCrtPage = Integer.parseInt(crtPage);
                nrAllPages = Integer.parseInt(allPages);
                if (nrCrtPage<nrAllPages) hasNext = true;
                if (nrCrtPage>1) hasPrevious = true;
                
            	if (hasPrevious) rsResponse = rsResponse + linkPrevious +"      ";
                if (hasNext) {
                    rsResponse = rsResponse + linkNext;
                }
                
                rsResponse = rsResponse.replaceAll("(?is)(<a[^>]+href=\")([^\"]+[^>]+>View</a>)", "$1" + sTmp + "$2");
                
				parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
				if (hasNext) Response.getParsedResponse().setNextLink(linkNext);
			
				break;
				
			case ID_DETAILS :
                StringBuffer finalResult = new StringBuffer();
                finalResult.append("<table> <tr>");
				rsResponse = rsResponse.replaceAll("(?i)<a[^>]*>[^>]*</a>","");
				
				Matcher mat = PAT_DISCLAIMER.matcher(rsResponse);
				if(mat.find()){
					rsResponse = rsResponse.substring(0, mat.start());
				}
				finalResult.append("<td>");
				mat = PAT_OWNER_TABLE.matcher(rsResponse);
				appendTable( mat, rsResponse, finalResult, false );
				finalResult.append("</td>");
				
				finalResult.append("<td>");
				mat = PAT_ASSES_TABLE.matcher(rsResponse);
				appendTable( mat, rsResponse, finalResult, false );
				finalResult.append("</td>");
				
				String link = "http://www.assessor.shelby.tn.us/PropertySearchSales.aspx?" + Response.getRawQuerry();
				String resSales = getLinkContents(link);
				
				finalResult.append("<td>");
				mat = PAT_SALE_TABLE.matcher(resSales);
				appendTable( mat, resSales, finalResult, false );
				finalResult.append("</td>");
				finalResult.append("</tr> <tr> <td colspan=\"3\">");
				
				mat = PAT_DWELING_TABLE.matcher(rsResponse);
				appendTable( mat, rsResponse, finalResult, true );
				finalResult.append("</td> </tr> </table>");
				
				rsResponse = finalResult.toString();
				rsResponse = rsResponse.replaceAll("(?i)class=\"[^\"]*\"", "");
				
				//get parcel id
				String keyNumber = getParcelNumber(rsResponse);
				if (!downloadingForSave) {
					
					String qry = Response.getRawQuerry();
					qry = "dummy=" + keyNumber + "&" + qry;
					String originalLink = sAction + "&" + qry;
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type","ASSESSOR");
					
					if(isInstrumentSaved(keyNumber, null, data)){
					    rsResponse += CreateFileAlreadyInTSD();
					} else {
					    rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink, viParseID);
						mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
					}

					Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
					parser.Parse( Response.getParsedResponse(), rsResponse, Parser.NO_PARSE);
					
				} else {

					//for html
					msSaveToTSDFileName = keyNumber + ".html";
					Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
					msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();

					if (rsResponse.indexOf("Property")!=-1) {
					    parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_DETAILS);
					} else {
					    parser.Parse(Response.getParsedResponse(), rsResponse, Parser.NO_PARSE);
					}
					
				}
				break;

			case ID_GET_LINK :
			case ID_SAVE_TO_TSD :
				
				if (sAction.equals("/PropertySearch.aspx"))
					ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
				else if (viParseID == ID_GET_LINK)
					ParseResponse(sAction, Response, ID_DETAILS);
				else { // on save
					downloadingForSave = true;
					ParseResponse(sAction, Response, ID_DETAILS);
					downloadingForSave = false;
				}
				break;

		}
	}
	
	/**
	 * 
	 * @param s
	 * @return
	 */
	private String getParcelNumber(String s) {
		String key = "";
		int istart = -1, iend = -1;
		istart = s.indexOf("Parcel ID:");
		if (istart != -1) {
			istart = s.indexOf("<span", istart);
			istart = s.indexOf(">",istart) + 1;
			iend = s.indexOf("</span>", istart);
			key = s.substring(istart, iend);
			key = key.replaceAll("\\s|-|\\.", "");
		}
		return key;
	}

	@Override
	protected String getFileNameFromLink(String url) {
		String id="";
		int i = -1;
		i = url.indexOf("&id=");
		if (i!=-1) {
		    i+=4;
		    id = url.substring(i,url.indexOf("&",i+1));
		    id = id.replaceAll("\\s","");
		}
		return id+".html";
	}
	
	/**
	 * 
	 * @param p
	 * @param pr
	 * @param htmlString
	 * @param pageId
	 * @param linkStart
	 * @param action
	 * @throws ro.cst.tsearch.exceptions.ServerResponseException
	 */
	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
	throws ro.cst.tsearch.exceptions.ServerResponseException {
	    
		int istart = htmlString.indexOf("<tr class=\"PropertySearchPage-HeaderStyle\">");
		int iend = htmlString.indexOf("</tr>")+"</tr>".length();		
		htmlString = htmlString.substring(0,istart) + htmlString.substring(iend);		
		p.splitResultRows(pr, htmlString, pageId, "<tr", "</table>", linkStart, action);

	}
}
