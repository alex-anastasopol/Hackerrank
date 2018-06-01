/*
 * Created on Jan 25, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ro.cst.tsearch.servers.types;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.ParcelIDFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.TSServer.ADD_DOCUMENT_RESULT_TYPES;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.Ostermiller.util.Base64;
import com.stewart.ats.tsrindex.server.UtilForGwtServer;

public class MOClayTR extends TSServer
{
	 
		private static final long serialVersionUID = 4995214855080545368L;

		private static final Pattern pidPattern = Pattern.compile("(?is)Parcel Number.*?<span[^>]*>(.*?)</span");
	    
	    private static final String startDelim = "<table";
	    private static final String endDelim = "</table>";
	    private static final Pattern pp = Pattern.compile("<a href=\"(.*?)\">");
		
        
        private boolean downloadingForSave;
        
        public MOClayTR( String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink,long searchId, int mid)
        {
            super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,searchId,mid);
        }
      
        public ServerResponse SearchBy( TSServerInfoModule module, Object sd) throws ServerResponseException
        {   
            int serverInfoModuleID = module.getModuleIdx();
            
            if( serverInfoModuleID == TSServerInfo.NAME_MODULE_IDX && InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchType() == Search.AUTOMATIC_SEARCH ){
            	module.setSeparator( "" );
            	
            	String lname = module.getFunction( 0 ).getParamValue();
            	String fname = module.getFunction( 1 ).getParamValue();
            	
            	if( !"".equals( lname ) ){
            		lname = lname + "*";
            	}
            	
            	if( !"".equals( fname ) ){
            		fname = fname.replace(" ", "*");
            		fname =  fname + "*";
            	}
            	
    			module.getFunction(0).setParamValue("");
    			module.getFunction(1).setParamValue(lname + fname);            	
            }
            
            return super.SearchBy(module, sd);
        }
        
        protected void setModulesForAutoSearch(TSServerInfo serverInfo)
        {
            
            List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
            TSServerInfoModule m;
            
            Search s= InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
            SearchAttributes sa = s.getSa();
            
            String parcelID = sa.getAtribute( SearchAttributes.LD_PARCELNO );
            
            String streetNo = sa.getAtribute( SearchAttributes.P_STREETNO );
            String streetName = sa.getAtribute( SearchAttributes.P_STREETNAME );
            FilterResponse pinFilter = getPinFilter();
            FilterResponse addressFilter 	= AddressFilterFactory.getAddressHighPassFilter( searchId , 0.6d );
    		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , null );
            DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
            FilterResponse nameFilterHybridDoNotSkipUnique = null;
            
            // search by parcel ID
            if(!"".equals( parcelID ) ){
    	        m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
    	        m.addFilter(pinFilter);
    			l.add(m);
            }
    		
    		//search by property address
            //<*><Street No><*><Street name><*>
            if( !"".equals( streetName ) || !"".equals( streetNo ) ){
    			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));			
    			m.clearSaKey(0);
    			m.clearSaKey(1);
    			String search_str="*";
    			if( !"".equals( streetNo ) )
    			{
    				search_str += streetNo + "*";
    			}
    			search_str += streetName + "*";
    			m.getFunction( 1 ).setParamValue( search_str  );
    			//m.addFilterType(FilterResponse.TYPE_MOCLAYTR_PARCEL_ID);
    			//m.addFilterType( FilterResponse.TYPE_ASSESSOR_ADDRESS2 );
    			m.addFilter( pinFilter );
    			m.addFilter( addressFilter );
    			m.addFilter( nameFilterHybrid );
    			m.addValidator( defaultLegalValidator );
    			l.add(m);
            }
            
            //search by owner name
            if( hasOwner() ){
    			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
    			m.clearSaKeys();
                m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
                
                nameFilterHybridDoNotSkipUnique = 
                	NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , m );
                nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
                
                m.addFilter( nameFilterHybridDoNotSkipUnique );
                m.addFilter( pinFilter );
                m.addFilter( AddressFilterFactory.getAddressHybridFilter(searchId, 0.8d) );
  
    			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
    			m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
    			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
    			.getConfigurableNameIterator(m, searchId, new String[] {"L;F;", "L;f;", "L;M;", "L;m;"});
    			m.addIterator(nameIterator);
                
                l.add(m);
            }
            
    		serverInfo.setModulesForAutoSearch(l);	
        }

        private FilterResponse getPinFilter(){
        	ParcelIDFilterResponse pf = new ParcelIDFilterResponse(SearchAttributes.LD_PARCELNO, searchId);
			pf.setCheckOnlyPatterns(true);
			pf.setRejectPatterns(new String[]{"00.*"});				
			return pf;
        }
        
        protected String getFileNameFromLink(String link) {
            String parcelId = org.apache.commons.lang.StringUtils.substringBetween(link, "dummy=", "&");
            parcelId = parcelId.replaceAll("-", "");
            return parcelId + ".html";
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see ro.cst.tsearch.servers.types.TSServer#ParseResponse(java.lang.String,
         *      ro.cst.tsearch.servers.response.ServerResponse, int)
         */
        @SuppressWarnings("rawtypes")
		protected void ParseResponse( String sAction, ServerResponse Response, int viParseID)  throws ServerResponseException
        {
            
            String sTmp = "";
            String rsResponce = Response.getResult();
            
            // Check if no results were found.
    		if (rsResponce.indexOf("does not exist. Please type another one.") > -1) {
    			Response.getParsedResponse()
    					.setError(
    							"No results were found for your query! Please change your search criteria and try again.");
    			return;
    		}
            
            int istart = -1, iend = -1;
            
          	rsResponce = rsResponce.replaceAll("<\\s*TR\\s*>", "<tr>");
          	rsResponce = rsResponce.replaceAll("<\\s*/\\s*TR\\s*>", "</tr>");
          	rsResponce = rsResponce.replaceAll("<\\s*TABLE\\s*>", "<table>");
          	rsResponce = rsResponce.replaceAll("<\\s*/\\s*TABLE\\s*>", "</table>");      	
          	
          	sTmp = CreatePartialLink(TSConnectionURL.idPOST);
          	
          	if( rsResponce.indexOf( "system is currently unavailable" ) >= 0 ){
          		//system error --> return
          		return;
          	}
          	
            switch (viParseID) {
            case ID_SEARCH_BY_NAME:
            case ID_SEARCH_BY_ADDRESS:
            	Map params = parseIntermPage( rsResponce );
            	
            	mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsDetails:", params);

            	String allParams = "";
            	
            	try{
            		allParams = "ASCENDWEB_SESSION_CODE=" + params.get( "ASCENDWEB_SESSION_CODE" ) + "&"
            						+ "__POST_BACK_VARIABLES_DATA=" + params.get( "__POST_BACK_VARIABLES_DATA" ) + "&"
            						+ "__VIEWSTATE=" /*+ params.get( "__VIEWSTATE" )*/;
            	}
            	catch( Exception e ){
            		e.printStackTrace();
            	}
            	
            	if( rsResponce.indexOf(">0 records returned from your search input") >= 0){
            		return;
            	}
            	
            	try{
            		istart = rsResponce.indexOf("Parcel Number");
            		istart = rsResponce.lastIndexOf(startDelim, istart);
            		
            		iend = rsResponce.indexOf( endDelim , istart);
            		if( iend >= 0 ){
            			iend += endDelim.length();
            		}
            		
            		rsResponce = rsResponce.substring( istart , iend);
            		
            	}catch( Exception e ){
            		e.printStackTrace();
            		return;
            	}
            	/*
<tr>\\s+<td[^>]*><a [^>]*>[^<]*</a></td><td[^>]*>[^<]*</td><td[^>]*>PERSONAL PROPERTY SITUS[^<]*</td>\\s+</tr>
            	 */
            	
            	rsResponce = rsResponce.replaceAll( "(?is)<tr>\\s+<td[^>]*><a [^>]*>[^<]*</a></td><td[^>]*>[^<]*</td><td[^>]*>PERSONAL PROPERTY SITUS[^<]*</td>\\s+</tr>" , "");
            	rsResponce = rsResponce.replaceAll( "(?i)<a href=\"javascript:__doPostBack\\('(mResultscontrol:mGrid)','parcel_number=([^']*)'\\)\" target=\"_self\">" ,
    					"<a href=\"" + sTmp + "/ascend/result.aspx&__EVENTTARGET=mResultscontrol:mGrid&__EVENTARGUMENT=parcel_number=$2&dummy=$2&" + allParams + "\">");

            	parser.Parse(Response.getParsedResponse(), rsResponce,
                        Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idPOST),
                        TSServer.REQUEST_SAVE_TO_TSD);     
                            	
            break;
              	  
          case ID_SEARCH_BY_PARCEL:
          case ID_DETAILS:
        	  
        	  String receiptNo = "";
	          	
        	  String calculatedTaxes = "";
	          	
        	  Matcher pidMatcher = pidPattern.matcher( rsResponce );
    		  //if we have a PID, do a search for the calculated taxes
        	  if(pidMatcher.find()){
        		  receiptNo = pidMatcher.group(1);
        		
        		  //don't make all the requests once again if we have the doc in memory		
        		  if (rsResponce.toLowerCase().contains("<html")){
	        		  try {
			                	
	        			  HTTPRequest req = new HTTPRequest( "https://collector.claycogov.com/ascend/injected/TaxesBalancePaymentCalculator.aspx?parcel_number="+receiptNo/*03-800-03-36-00-0-00-000*/ );
			                    
	        			  HTTPResponse res = null;
			                    
	        			  HttpSite site = HttpManager.getSite(HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName(), searchId);
	        			  try {
	        				  res = site.process(req);
	        			  } finally 
	        			  {
	        				  HttpManager.releaseSite(site);
	        			  }
			                    
	        			  calculatedTaxes = res.getResponseAsString();
			                    
	        			  calculatedTaxes = StringUtils.getTextBetweenDelimiters( "decode64( '" , "'", calculatedTaxes);
	        			  calculatedTaxes = Base64.decode( calculatedTaxes );
			                    
	        			  calculatedTaxes = calculatedTaxes.replace("$", "\\$");
	        			  calculatedTaxes = calculatedTaxes.replaceAll("(?is)<!--DONT_PRINT_START-->.*?<!--DONT_PRINT_FINISH-->", "");
	        			  calculatedTaxes = calculatedTaxes.replaceAll( "(?is)<td[^>]*>Select to Pay</td>" , "");
	        			  calculatedTaxes = calculatedTaxes.replaceAll( "(?is)<td[^>]*><input type=.*?</td>" , "");
	        		  } catch( Exception e ){}
			          		
	        		  receiptNo = receiptNo.replaceAll("-", "");
			          	
	        		  //If no results found by parcel id, returns
	        		  if (receiptNo.equals(""))
	        			  return;
			          	
			          rsResponce = rsResponce.replaceAll( "(?is)<TD id=\"TaxesBalancePaymentCalculator\">.*?</TD>" , "<TD id=\"TaxesBalancePaymentCalculator\">" + calculatedTaxes + "</TD>");
			          	
			          try{
			        	  istart = rsResponce.indexOf( "Property Account Summary" );
			        	  istart = rsResponce.lastIndexOf("<tr", istart);
				          		
			        	  iend = rsResponce.indexOf( "MainFooter" , istart);
			        	  iend = rsResponce.lastIndexOf( "</tr>" , iend);
			        	  if( iend >= 0 ){
			        		  iend += 5;
				          }
				          		
			        	  rsResponce = rsResponce.substring( istart, iend);
			          }catch( Exception e ){
			        	  e.printStackTrace();
			          }
			          rsResponce = addReceipts(Response.getResult(), rsResponce);
			          	
			          rsResponce = rsResponce.replaceAll( "(?i)<a [^>]*>(.*?)</a>" , "$1");
			          	
			          rsResponce = rsResponce.replaceAll( "(?is)<script .*?</script>" ,"");
        		  }
        	  }
          	if ((!downloadingForSave)){
    			String qry = Response.getQuerry();
    			qry = "dummy=" + receiptNo + "&" + qry;
    			String originalLink = sAction + "&" + qry;
    			String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
    			
    			HashMap<String, String> data = new HashMap<String, String>();
				data.put("type","CNTYTAX");
				
				if(isInstrumentSaved(receiptNo, null, data)){
    				rsResponce += CreateFileAlreadyInTSD();
    			}
    			else {                    
    				mSearch.addInMemoryDoc(sSave2TSDLink, rsResponce);
    				rsResponce = addSaveToTsdButton(rsResponce, sSave2TSDLink, viParseID);
    			}
    			Response.getParsedResponse().setPageLink(
    				new LinkInPage(
    					sSave2TSDLink,
    					originalLink,
    					TSServer.REQUEST_SAVE_TO_TSD));
    			parser.Parse(
    				Response.getParsedResponse(),
    				rsResponce,
    				Parser.NO_PARSE);
    		}
    		else
    		{
    			rsResponce = rsResponce.replaceAll( "(?is)<span[^>]*>" , "");
		        rsResponce = rsResponce.replaceAll( "(?is)</span>" , "");
    			
    			//for html
    			msSaveToTSDFileName = receiptNo + ".html";
    			Response.getParsedResponse().setFileName(
    				getServerTypeDirectory() + msSaveToTSDFileName);
    			msSaveToTSDResponce = rsResponce + CreateFileAlreadyInTSD();
    			parser.Parse(
    				Response.getParsedResponse(),
    				rsResponce,
    				Parser.PAGE_DETAILS);
    			
    		}
              break;
            case ID_GET_LINK:
            	if (sAction.indexOf("/result.aspx") >= 0)
    				ParseResponse(sAction, Response, ID_DETAILS);
    			
    			
                
            break;
            case ID_SAVE_TO_TSD :
             // on save
    			downloadingForSave = true;
    			ParseResponse(sAction, Response, ID_DETAILS);
    			downloadingForSave = false;
    		
                break;
            }
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
		private Map parseIntermPage(String s){
            Map rez = new HashMap();
            try{
    	        String tmp = ro.cst.tsearch.utils.StringUtils.getTextBetweenDelimiters("<input type=\"hidden\" name=\"ASCENDWEB_SESSION_CODE\" value=\"", "\"", s);
    	        rez.put("ASCENDWEB_SESSION_CODE", URLEncoder.encode(tmp, "UTF-8") );
    	
    	        tmp = ro.cst.tsearch.utils.StringUtils.getTextBetweenDelimiters("<input type=\"hidden\" name=\"__POST_BACK_VARIABLES_DATA\" value=\"", "\"", s);
    	        rez.put("__POST_BACK_VARIABLES_DATA", tmp);        
    	        
    	        tmp = ro.cst.tsearch.utils.StringUtils.getTextBetweenDelimiters("<input type=\"hidden\" name=\"__VIEWSTATE\" value=\"", "\"", s);
    	        rez.put("__VIEWSTATE", tmp);
            } catch( Exception e ){
            	
            }
            return rez;
            
        }
        
        @SuppressWarnings({ "unchecked", "rawtypes" })
		public static void splitResultRows(
    			Parser p,
    			ParsedResponse pr,
    			String htmlString,
    			int pageId,
    			String linkStart,
    			int action,long searchId) throws ro.cst.tsearch.exceptions.ServerResponseException
    		{

        		int startidx=-1, endidx=-1;
        		//String table_head="";
        		startidx = htmlString.indexOf ("<tr>");
        		endidx  = htmlString.indexOf ("</tr>",startidx+1);
        		if (startidx!=-1 && endidx!=-1)
        		{
        			//table_head=htmlString.substring(startidx,endidx+"</tr>".length());
        			htmlString=htmlString.substring(0,startidx)+htmlString.substring(endidx);
        		}
        	
    			p.splitResultRows(
    				pr,
    				htmlString,
    				pageId,
    				"<tr",
    				"</table>",
    				linkStart,
    				action);
    			//remove table header and rows with subdivision with no -
    			Vector rows = pr.getResultRows();
    			boolean deleted = false;
                try
                {
        			for (int i = 0; i < rows.size(); i++)
        			{
        				ParsedResponse row = (ParsedResponse)rows.get(i);
        				PropertyIdentificationSet data =
        					row.getPropertyIdentificationSet(0);
        				 String pid=data.getAtribute("ParcelID");
        				 if (pid.indexOf("-") != -1)
        				     {
        				     	rows.setElementAt(null, i);
        				     	deleted = true;
        				     }
        				 else
        				 {
        				     String lnk = row.getResponse();
        			         Matcher m = pp.matcher(lnk);
        			         if (m.find())
        				     {
        				         lnk = m.group(1);
        				     }
        				   
        				     row.setPageLink(new LinkInPage(
        								lnk,
        								lnk,
        								TSServer.REQUEST_SAVE_TO_TSD));
        				 }
        			}
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
    			if (deleted)
    			{
    			    while (rows.contains(null))
    			    {
    			    for (int i = 0; i < rows.size(); i++)
    			        if (rows.get(i) == null)
    			            rows.remove(i);
    			    }
    			}
                
                if( rows.size() == 0 && !"".equals( pr.getNextLink() ) && InstanceManager.getManager().getCurrentInstance(searchId)
                						.getCrtSearchContext().getSearchType() == Search.PARENT_SITE_SEARCH  )
                {
                     // if we have a next link, but we don't have any valid result
                     // 
                     // instruct the user to go to next page
                     //
                    
                    ParsedResponse goToNextPage = new ParsedResponse();
                    goToNextPage.setOnlyResponse("<tr><td><BR><font style=\"font-size:20px; color:red\"><B>This page does not contain any real estate properties. Please go to next page.</B></font><BR><BR><BR></td></tr>");
                    pr.setHeader("<table>");
                    String footer = pr.getFooter();
                    pr.setFooter( footer );
                    
                    rows.add( goToNextPage );
                    
                }

                pr.setResultRows(rows);
    		}
        
        @SuppressWarnings({ "unchecked", "rawtypes" })
    	private String addReceipts(String fullResponse, String response) {
    		StringBuilder newResponse = new StringBuilder(response);
    		
    		try {
    			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
    			NodeList nodeList = htmlParser.parse(null);
    			
    			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
    				.extractAllNodesThatMatch(new HasAttributeFilter("id", "mTabGroup_Receipts_mReceipts_mGrid_RealDataGrid"));
    			if (tableList.size()>0) {
    				String link = "https://collector.claycogov.com/ascend/parcelinfo.aspx";
    				String table = tableList.toHtml();
    				Map params = parseIntermPage(fullResponse);
    				params.put("__EVENTTARGET", "mTabGroup:Receipts:mReceipts:mGrid");
    				Matcher matcher = Pattern.compile("(?is)<a.*?href=\"javascript:__doPostBack\\('mTabGroup:Receipts:mReceipts:mGrid','([^']*)'\\)\"")
    					.matcher(table);
    				while (matcher.find()) {
    					params.put("__EVENTARGUMENT", matcher.group(1));
    					mSearch.setAdditionalInfo(getCurrentServerName() + ":paramsReceipts:", params);
    					
    					String result = getLinkContents(link);
    					result = extractTable(result);
    					
    					newResponse.append(result);
    				}
    			}
    		} catch (Exception e) {
    			logger.error("Error while getting receipts", e);
    		}
    		
    		return newResponse.toString();
    	}
        
        public String extractTable(String result) {
    		try {
    			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(result, null);
    			NodeList nodeList = htmlParser.parse(null);
    			
    			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
    				.extractAllNodesThatMatch(new HasAttributeFilter("id", "mPanelTable"));
    			if (tableList.size()>0) {
    				String content = tableList.elementAt(0).toHtml();
    				content = content.replaceFirst("Official Tax Payment Receipt", "<b>$0</b>");
    				return content; 
    			}
    		} catch (Exception e) {
    			logger.error("Error while getting receipts", e);
    		}
    		
    		return "";
    	}
        
        protected String GetInfo(String what){           
            return "";
        }
        
        @Override
    	protected ADD_DOCUMENT_RESULT_TYPES addDocumentInATS(ServerResponse response, String htmlContent,
    			boolean forceOverritten) {
    		ADD_DOCUMENT_RESULT_TYPES result =  super.addDocumentInATS(response, htmlContent, forceOverritten);
    		try {
    			if(result.equals(ADD_DOCUMENT_RESULT_TYPES.ADDED)) {
    				UtilForGwtServer.uploadDocumentToSSF(searchId, response.getParsedResponse().getDocument());
    			}
    		} catch (Exception e) {
    			logger.error("Error while saving index for " + searchId, e);
    		}
    		return result;
    	}
        
}