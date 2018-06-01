/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.servers.types;

/**
 * @author 
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class TNKnoxEP extends TSServer
{
	protected static final Category logger= Logger.getLogger(TNKnoxEP.class);
	
	public static final Pattern currentPagePattern = Pattern.compile( "(?is)<font face=\"helvetica, arial, sans-serif\" size=\"2\"><b>\\s+(\\d+)" );
	
	static final long serialVersionUID = 10000000;

	private boolean downloadingForSave = false;


	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m = null;
		
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance( searchId ).getCrtSearchContext().getSa();
		
		String pid 			= sa.getAtribute( SearchAttributes.LD_PARCELNO ) ;
		String streetName 	= sa.getAtribute( SearchAttributes.P_STREETNAME ) ;
		String streetNo		= sa.getAtribute( SearchAttributes.P_STREETNO);
		FilterResponse nameFilterHybridDoNotSkipUnique = null;
		
		boolean emptyPid 	= StringUtils.isEmpty( pid );
		boolean emptyStreetName = StringUtils.isEmpty( streetName );
		String city = getSearchAttribute(SearchAttributes.P_CITY).toUpperCase();
		if(!StringUtils.isEmpty(city)){
			if(!city.startsWith("KNOXVILLE")){
				return;
			}			
		}
		if( !emptyPid ){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			m.clearSaKeys();
			m.setData(0, pid.replaceAll("[^A-z0-9 ]", ""));
			l.add(m);
		}

		if( !emptyStreetName ){
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.setData(2,( streetNo + " " + streetName ).trim() );
			m.addFilter( AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d) );
			m.addFilter( NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId, m) );
			l.add(m);
		}

		if( hasOwner()){
			
			nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
					SearchAttributes.OWNER_OBJECT , searchId , null );
			nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
			
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			m.clearSaKeys();
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.addFilter( nameFilterHybridDoNotSkipUnique );
			m.addFilter( AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d) );
			
			m.setIteratorType(0,FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(m, searchId, new String[] {"F;L;"});
			m.addIterator(nameIterator);
			
			l.add(m);
		}
		
		serverInfo.setModulesForAutoSearch(l);		
	}

	public TNKnoxEP(String rsRequestSolverName,String rsSitePath,String rsServerID,String rsPrmNameLink,long searchId, int mid)
	{		
		super(rsRequestSolverName,rsSitePath,rsServerID,rsPrmNameLink,searchId,mid);
	}


	/**
	 * @see TSServer#ParseResponse(java.lang.String, int, javax.servlet.http.HttpServletResponse)
	 */
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID)  throws ServerResponseException
	{
		
		String sTmp;
		int start, endIndex;
		String keyNumber = "";
		String prev="",next="";
		
		String rsResponce=Response.getResult();
		String initialResponse = rsResponce;
		switch (viParseID)
		{
			case ID_SEARCH_BY_NAME :
			case ID_SEARCH_BY_ADDRESS  :
			case ID_SEARCH_BY_PARCEL  :
			case ID_SEARCH_BY_SUBDIVISION_NAME :
				if(rsResponce.indexOf("No matching records")!=-1) {
					//Response.getParsedResponse().setError("No records found.");
					return;
				}

				int pageNo = 1;
				
				Matcher pageNoMatcher = currentPagePattern.matcher( rsResponce );
				if( pageNoMatcher.find() ) {
					pageNo = Integer.parseInt( pageNoMatcher.group( 1 ) );
				}
				
				Matcher nextLinkMatcher = Pattern.compile( "(?is)<a href=\"([^\"]*Results.ASP)\\?([^\"]*)\">" + (pageNo + 1) + "</a>" ).matcher( rsResponce );
				if( nextLinkMatcher.find() ) { 
					next = CreatePartialLink( TSConnectionURL.idGET ) + "/Tax_Search/" + nextLinkMatcher.group( 1 ) + "&" + nextLinkMatcher.group( 2 ); 
				}
				
				Matcher prevLinkMatcher = Pattern.compile( "(?is)<a href=\"([^\"]*Results.ASP)\\?([^\"]*)\">" + (pageNo - 1) + "</a>" ).matcher( rsResponce );
				if( prevLinkMatcher.find() ) { 
					prev = CreatePartialLink( TSConnectionURL.idGET ) + "/Tax_Search/" + prevLinkMatcher.group( 1 ) + "&" + prevLinkMatcher.group( 2 ); 
				}
				
				start = rsResponce.indexOf( "<!--BEGIN COLUMN HEADERS-->" );
				endIndex = rsResponce.indexOf( "</table>" , start);
				
				sTmp = CreatePartialLink( TSConnectionURL.idGET );
				
				rsResponce = "<table>" + rsResponce.substring(start, endIndex) + "</table>";
				rsResponce = rsResponce.replaceAll( "<img src=\"/images/[^\"]*details.gif\"[^>]*>" , "View Details");
				rsResponce = rsResponce.replaceAll( "(?is)<a href=\"[^\"]*\" class=\"QuickHelpHeader\"[^>]*>([^<]*)</a>" , "$1");
				rsResponce = rsResponce.replaceAll( "(?is)href=\"([^\"]*Detail.ASP)\\?([^\"]*)\"" , "href=\"" + sTmp + "/Tax_Search/$1&$2\"");

				if( !"".equals( prev ) ) {
					rsResponce += "<a href=\"" +prev + "\">Previous</a>";
				}
				
				if( !"".equals( next ) ) {
					rsResponce += "&nbsp;&nbsp;&nbsp;<a href=\"" + next + "\">Next</a>";
				}
				
				parser.Parse(Response.getParsedResponse(),rsResponce,Parser.PAGE_ROWS,getLinkPrefix(TSConnectionURL.idGET ), TSServer.REQUEST_SAVE_TO_TSD);
				Response.getParsedResponse().setNextLink(next);
				break;
			case ID_DETAILS :
					start = rsResponce.indexOf("Tax&nbsp;History&nbsp;Summary");
					start = rsResponce.indexOf( "<table" , start);
					endIndex = rsResponce.indexOf( "Revenue Office");
					endIndex = rsResponce.lastIndexOf("</table>", endIndex);
					
					if( start <= 0 || endIndex <= 0 ) {
						return;
					} 
					rsResponce = rsResponce.substring( start , endIndex) + "</table>";
                                
					//get parcel id
					keyNumber = getParcelNoFromResponse(rsResponce) ;
					rsResponce = rsResponce.replaceAll( "(?is)<a [^>]*>(.*?)</a>" , "$1" );
					rsResponce = rsResponce.replaceAll( "(?is)</?form[^>]*>" , "");

					start = rsResponce.indexOf( "For payment at a later date" );
					start = rsResponce.lastIndexOf( "<table" , start);
					endIndex = rsResponce.indexOf( "</table" , start + 1);
					if( start >=0 && endIndex >= 0 ){
						rsResponce = rsResponce .substring(0, start) + rsResponce.substring( endIndex +  8);
					}

					rsResponce = rsResponce.replaceAll( "(?is)<input name=\"PayNow\"[^>]*>" , "");
					
					if ((!downloadingForSave)){
						//String originalLink = response.getOriginalLink(keyNumber);
						String originalLink = "/Tax_Search/Detail.ASP&"+Response.getQuerry();
						String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

						if (FileAlreadyExist(keyNumber + ".html") ) {
							rsResponce += CreateFileAlreadyInTSD();
						} else {
							rsResponce =  addSaveToTsdButton(rsResponce, sSave2TSDLink, viParseID);
							mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
						}

						Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
						parser.Parse(Response.getParsedResponse(), rsResponce, Parser.NO_PARSE);
					}else{
						
			
						//for html
						msSaveToTSDFileName = keyNumber + ".html";
		
						Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);

						msSaveToTSDResponce = rsResponce + CreateFileAlreadyInTSD();
																
						//reparse the final answer (included all links)
						parser.Parse(Response.getParsedResponse(), rsResponce, Parser.PAGE_DETAILS);
					}
					break;
			case ID_SAVE_TO_TSD :					  					
			case ID_GET_LINK :			
			  if (!sAction.contains("Detail.ASP"))
				   ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);			  
			  else
				  if (viParseID == ID_GET_LINK)
					ParseResponse(sAction, Response, ID_DETAILS);
				 else {// on save
					downloadingForSave = true;
					ParseResponse(sAction, Response, ID_DETAILS);
					downloadingForSave = false;
				   }										  
				break;
			default :
				break;
		}
	}
	
	
	private String getParcelNoFromResponse(String s) {
		//get parcel id
		String r = "";
		try {
			int i=s.indexOf("Property&nbsp;ID");
			i = s.indexOf( "<td", i );
			i = s.indexOf( ">", i );
	        int j=s.indexOf( "</" , i + 1);
			r=s.substring(i + 1, j).replaceAll("\\&nbsp;", "").trim();
			r = r.replaceAll("/", "");
		} catch(Exception e) {
			e.printStackTrace();
		}
		return r;
	}


	protected String getFileNameFromLink(String url){//
		String parcelId = StringUtils.getTextBetweenDelimiters("propid=", "&", url).trim();
		return parcelId + ".html";
	}

	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action) throws ro.cst.tsearch.exceptions.ServerResponseException {
            p.splitResultRows(pr, htmlString, pageId, "<tr ","</table>", linkStart,  action);
            //special treatmen for the first row, which is not a true row, but the columns header
            Vector rows = pr.getResultRows();
            if (rows.size()>0){
                    ParsedResponse firstRow = (ParsedResponse) rows.remove(0);
                    pr.setResultRows(rows);
                    pr.setHeader(pr.getHeader()+firstRow.getResponse());
            }
        }
}
