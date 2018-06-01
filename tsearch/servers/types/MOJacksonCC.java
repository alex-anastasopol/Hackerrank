package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_SELECT_BOX;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.HTML_TEXT_FIELD;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLControl.setJustifyFieldMulti;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.ORIENTATION_HORIZONTAL;
import static ro.cst.tsearch.servers.info.spitHTML.HTMLObject.PIXELS;

import java.io.File;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.RejectAlreadyPresentFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.FormatException;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class MOJacksonCC extends TSServer {

	private static final long serialVersionUID = 6168125351690907654L;
	
	private boolean downloadingForSave = false;

	public MOJacksonCC(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public MOJacksonCC(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo si = new TSServerInfo(1);
		String serverAddress = dataSite.getServerAddress();
		
		si.setServerAddress(serverAddress);
		si.setServerIP(serverAddress);
		si.setServerLink(dataSite.getServerHomeLink());
		
        // build start date and end date formatted strings 
        String startDate="", endDate="";
        try{
        	SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
            SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
            Date start = sdf.parse(sa.getAtribute(SearchAttributes.FROMDATE));	            
            Date end = sdf.parse(sa.getAtribute(SearchAttributes.TODATE));	            
            sdf.applyPattern("MM/dd/yyyy");	            
            startDate = sdf.format(start);
            endDate = sdf.format(end);	            
        }catch (ParseException e){}
        
		TSServerInfoModule 	sim = si.ActivateModule(TSServerInfo.NAME_MODULE_IDX, 5);
		sim.setName("Ucc Search");
		sim.setDestinationPage("/results.asp");
		sim.setRequestMethod(TSConnectionURL.idPOST);
		sim.setParserID(ID_SEARCH_BY_NAME);

		PageZone pz = new PageZone("SearchByName", "UCC Search", ORIENTATION_HORIZONTAL, null, 550, 50, PIXELS , true);

		try{				
            HTMLControl 
            name = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 1, 1, 40, sim.getFunction(0), "txtTor", 		"Name", 		null, 		searchId),
            inst = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 2, 2, 20, sim.getFunction(1), "txtInstNum", 	"Instrument", 	"", 		searchId),
            loc  = new HTMLControl(HTML_SELECT_BOX, 1, 1, 3, 3,  1, sim.getFunction(2), "selLocation", 	"Location",		" ",  		searchId),
            from = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 4, 4, 10, sim.getFunction(3), "txtActDateFr", "Activity From",startDate, 	searchId),
            to   = new HTMLControl(HTML_TEXT_FIELD, 1, 1, 5, 5, 10, sim.getFunction(4), "txtActDateTo", "Activity To",	endDate, 	searchId);
                        	            				
            name.setFieldNote("Lastname, Firstname");
            from.setFieldNote("mm/dd/yyyy");
            to.setFieldNote("mm/dd/yyyy");            
            
            setupSelectBox(sim.getFunction(2), SELECT_LOCATION);	            
            setJustifyFieldMulti(true, name, inst, loc, from, to);
            pz.addHTMLObjectMulti(name, inst, loc, from, to);
            
            sim.getFunction(0).setSaKey(SearchAttributes.OWNER_LCFM_NAME);
            
		}catch(FormatException e){
			e.printStackTrace();
		}
		
		sim.setModuleParentSiteLayout(pz);

		si.setupParameterAliases();
		setModulesForAutoSearch(si);
		
		return si;
	}
	
    protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		int searchType = getSearch().getSearchType();
		
		if(searchType == Search.AUTOMATIC_SEARCH) {
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.clearIteratorTypes();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setSaKey(3, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
			module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			module.setSaKey(4, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
			
			GenericNameFilter nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, module);
	        nameFilter.setIgnoreMiddleOnEmpty(true);
	        module.addFilter(nameFilter);
	        module.addValidator(new RejectAlreadyPresentFilterResponse(searchId).getValidator());
	        
	        addBetweenDateTest(module, true, true, false);
			ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, new String[] {"L, F;;"});
			iterator.setInitAgain(true);
			module.addIterator(iterator);						
	        
			modules.add(module);
	
		}

		serverInfo.setModulesForAutoSearch(modules);
    }
    
	@Override
    protected String getFileNameFromLink(String url) {
    	String rez = url.replaceAll(".*insno=(.*?)(?=&|$)", "$1");
    	rez = rez.replace("&disableEncoding=true", "");
    	if (rez.trim().length() > 10) {
    		rez = rez.replaceAll("&parentSite=true", "");
    	}
        if (rez.trim().length()<=15){    
            return rez.trim() + ".html";
        } else {
            return msSaveToTSDFileName;
        }
    }

	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
        String rsResponce = Response.getResult();
        String initialResponse = rsResponce;
        String linkStart;
        int istart = -1, iend = -1;
		if (rsResponce.indexOf("No Matches Found.") != -1) {
			Response.getParsedResponse().setError("No Matches Found.");
			return;
		}
        switch (viParseID) {
        
        case ID_SEARCH_BY_NAME:
        	
        	linkStart = CreatePartialLink(TSConnectionURL.idGET);
        	
        	istart = rsResponce.indexOf("<table width=95% border=\"0\" cellpadding=\"2\" cellspacing=\"2\">");
        	if (istart == -1)
                return;
            
        	Pattern p = Pattern.compile("<font color=red><b>(\\d+)</b></font>");
            Matcher m = p.matcher(rsResponce);
            
            int iNext = -1;
            int iPrev = -1;
            String sNext, linkNext = null;
            String sPrev, linkPrev = null;
            
            if (m.find()) {
            	iNext = Integer.parseInt(m.group(1)) + 1;
            	iPrev = Integer.parseInt(m.group(1)) + -1;
            	sNext = "<a href=\"results.asp\\?pg=" + iNext +"\"";
            	sPrev = "<a href=\"results.asp\\?pg=" + iPrev +"\"";
            	
            	Pattern pn = Pattern.compile(sNext);
            	Pattern pp = Pattern.compile(sPrev);
            	
            	Matcher mn = pn.matcher(rsResponce);
            	Matcher mp = pp.matcher(rsResponce);
            	if (mn.find()) {
            		// avem link de next
            		linkNext = sNext + ">Next</a>";
            	}
            	if (mp.find()) {
            		// avem link de prev
            		linkPrev = sPrev+ ">Previous</a>";
            	}
            		
            }
            
            iend = rsResponce.indexOf("</table>", istart) + "</table>".length();
            rsResponce = rsResponce.substring(istart, iend);
            rsResponce = rsResponce.replaceAll("<!--.*?-->","").replaceAll("<!--.*?--'>","").replaceAll("<!--.*?--\">","");
            rsResponce = rsResponce.replaceAll("(?s)<tr>.*?<td colspan=99>(.*?)</tr>", "");
            
            // modificam link-urile ... marcam daca au sau nu imagine
            int startIdx = 0, endIdx = 0;
            String vsRowSeparator = "<tr valign=top>";
            String newHtml = "";
            startIdx = rsResponce.indexOf(vsRowSeparator, 0);
            if (startIdx != -1) {
                
                endIdx = rsResponce.indexOf(vsRowSeparator, startIdx + vsRowSeparator.length());
                endIdx = rsResponce.indexOf(vsRowSeparator, startIdx + vsRowSeparator.length());
	            while (endIdx != -1) {
	                
	                String row = rsResponce.substring(startIdx, endIdx);
	                newHtml += prepareRow(row, linkStart);
	                
	                startIdx = endIdx;
	
	                endIdx = rsResponce.indexOf(vsRowSeparator, startIdx + vsRowSeparator.length());
	            }
	            endIdx = rsResponce.indexOf("</tr>", startIdx);
	            if (endIdx == -1)
	                endIdx = rsResponce.length();

	            String row = rsResponce.substring(startIdx, endIdx);
	            newHtml += prepareRow(row, linkStart);
            }
            // rsResponce = "<table width=95% border=\"0\" cellpadding=\"2\" cellspacing=\"2\">" + newHtml + "</table>";
            newHtml = newHtml.replace("<td></td>", "<td>&nbsp;</td>");
            newHtml = newHtml.replaceFirst("<tr", "<tr bgcolor=#DDDDDD ");
            rsResponce = "<table width=100% border=\"1\" cellpadding=\"0\" cellspacing=\"0\">" + newHtml + "</table>";
           
            
            // adaugam eventualele link-uri de next
            rsResponce += "<br>";
            
            if (linkPrev != null) {
            	linkPrev = "<a href=\"" + linkStart + "results.asp&pg=" + iPrev + "\">Previous</a>";
           	 	rsResponce += linkPrev;
            }
            
            if (linkNext != null) {
                rsResponce += "&nbsp;&nbsp;&nbsp;&nbsp;";
            	linkNext = "<a href=\"" + linkStart + "results.asp&pg=" + iNext + "\">Next</a>";
            	rsResponce += linkNext;
            }
            
            // rewrite links
            // <a href=docdetail.asp?id=Tk5MDM2Uk%03%13M%21%21OQTMhLzAzO%21xhRkowM&ms=69592&cabinet=ucc&pg=&id2=y8xMCT%21ZQ%03%0FM1mo1fz%21yTkyJl%217lC8xO>1992J0303836</a>            
            rsResponce = rsResponce.replaceAll("href=(docdetail\\.asp)\\?([^>]+)>([^<]+)</a>","href='" + linkStart + "$1&$2&insno=$3&disableEncoding=true'>$3</a>");
            
            // se parseaza.
            parser.Parse(Response.getParsedResponse(), rsResponce, Parser.PAGE_ROWS,
                    getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);
            
            Response.getParsedResponse().setNextLink(linkNext);
        	break;
            
        case ID_DETAILS:
        	
        	rsResponce = rsResponce.replaceAll("<!--.*?-->","").replaceAll("<!--.*?--'>","").replaceAll("<!--.*?--\">","");
        	String instrNum = rsResponce.replaceAll("(?s).*Instrument Number:[^\\d]*?(\\d.*?)</td>.*", "$1");
        	
        	istart = rsResponce.indexOf("<table width=95% border=\"0\" cellpadding=\"2\" cellspacing=\"2\">");
        	if (istart == -1)
                return;
            
        	iend = rsResponce.indexOf("<img src=\"/graphics/fade3_hr.gif\" width=760 alt=\"\">", istart) -18;
            
            rsResponce = rsResponce.substring(istart, iend);
            rsResponce = rsResponce.replaceAll("<DIR>", "").replaceAll("</DIR>", "");
            
            linkStart = CreatePartialLink(TSConnectionURL.idGET);
            rsResponce = rsResponce.replaceAll("docdetail.asp\\?" + "([^>]*)>", linkStart + "docdetail.asp&" + "$1" + ">");
            
            boolean hasImage = false;
            String imageLink = "";
            if (Response.getQuerry().indexOf("img=1") != -1) {
                //has image
                int index = rsResponce.indexOf("</table>");
               
                String tmpRawQuerry = Response.getRawQuerry();
                if( tmpRawQuerry.contains( "dummy=" ) ){
                	//fix querry
                	tmpRawQuerry = tmpRawQuerry.replaceAll( "(?is).*dummy=" , "dummy=" );
                	
                	String[] queryParams = tmpRawQuerry.split( "&" );
                	String newQuerry = "";
                	
                	for( int i = 0 ; i < queryParams.length ; i ++ ){
                		int eqIndex = queryParams[i].indexOf( "=" );
                		if( eqIndex >= 0 ){
                			String paramName = queryParams[i].substring( 0 , eqIndex );
                			String paramValue = queryParams[i].substring( eqIndex + 1 );
                			try{
                				paramValue = URLEncoder.encode( paramValue , "UTF-8" );
                			}
                			catch( Exception e ){
                				
                			}
                			newQuerry += paramName + "=" + paramValue;
                		}
                		else{
                			newQuerry += queryParams[i];
                		}
                		
                		if( i != queryParams.length - 1 ){  
                			newQuerry += "&";
                		}
                	}
                	
                	tmpRawQuerry = newQuerry;
                }
                
                imageLink = linkStart + "docimage.asp&" + tmpRawQuerry;
                hasImage = true;
                
                rsResponce = rsResponce.substring(0, index)
            					+ "<tr valign=top><td align=right scope=\"row\"><b>Image:</b></td><td>"
            					+ "<a target=_blank href=" + imageLink + ">View Image<BR></a>"
            					+ "</td></tr>"
                			+ rsResponce.substring(index, rsResponce.length());
            }
            
        	if ((!downloadingForSave))
    		{
    			String qry = Response.getQuerry();
    			qry = "dummy=" + instrNum + "&" + qry;
    			String originalLink = sAction + "&" + qry;
    			String sSave2TSDLink =
    				getLinkPrefix(TSConnectionURL.idGET) + originalLink;
    			if (FileAlreadyExist(instrNum + ".html") )
    			{
    				rsResponce += CreateFileAlreadyInTSD();
    			}
    			else
    			{
    			   rsResponce = addSaveToTsdButton(rsResponce, sSave2TSDLink, viParseID);
    			   mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
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
    			//for html
    			msSaveToTSDFileName = instrNum + ".html";
    			Response.getParsedResponse().setFileName(
    				getServerTypeDirectory() + msSaveToTSDFileName);
    			
    			String sFileLink;
        	    
    			Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
    			msSaveToTSDResponce = rsResponce + CreateFileAlreadyInTSD(true);

	            //download view file if any;
	            if (hasImage) {
	                sFileLink = instrNum + ".tif";
	                Response.getParsedResponse().addImageLink(new ImageLinkInPage(imageLink, sFileLink));
	            }
	            
	            ParsedResponse pr = Response.getParsedResponse();
	            parser.Parse(pr, rsResponce, Parser.PAGE_DETAILS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);	            
	            Response.getParsedResponse().setOnlyResponse(rsResponce);
    		}
        	break;
        	
        case ID_GET_LINK:
        	//click pe next sau details
        	if (sAction.indexOf("results.asp") >= 0) {
        		ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
        		break;
        	} else if (sAction.indexOf("docdetail.asp") >= 0) {
        		ParseResponse(sAction, Response, ID_DETAILS);
        		break;
        	}
        	
        case ID_SAVE_TO_TSD :
        	// on save
   			downloadingForSave = true;
   			ParseResponse(sAction, Response, ID_DETAILS);
   			downloadingForSave = false;
        }
	}

    @SuppressWarnings("unchecked")
	public static void splitResultRows(Parser p, ParsedResponse pr,
            String htmlString, int pageId, String linkStart, int action)
            throws ro.cst.tsearch.exceptions.ServerResponseException {

        p.splitResultRows(pr, htmlString, pageId, "<tr valign=top>", "</tr>", linkStart, action);
        
        // remove table header
        /*
        Vector rows = pr.getResultRows();
        if (rows.size() > 0) {        	
            ParsedResponse firstRow = (ParsedResponse)rows.remove(0); 
            pr.setResultRows(rows);
            pr.setHeader(pr.getHeader() + firstRow.getResponse()); 
        }
        */
    }
    
	private String prepareRow(String row, String linkStart) {
	    
	    row = row.replaceAll("href=docimage.asp\\?"  + "([^>]*)>", "target=_blank href=" + linkStart + "docimage.asp&" + "$1" + ">");
	    row = row.replaceAll("<img border=0 src=\"/graphics/paper.gif\"" + "([^>]*)>", "View Image");
	    
	    if (row.indexOf("View Image") != -1) {
		    row = row.replaceAll("(?s)docdetail.asp\\?" + "(.*?)>(.*?)</a>", linkStart + "docdetail.asp&" + "$1"+"&insno=$2&img=1" + ">$2</a>");
	    }  else {
	        row = row.replaceAll("(?s)docdetail.asp\\?" + "(.*?)>(.*?)</a>", linkStart + "docdetail.asp&" + "$1"+"&insno=$2&disableEncoding=true" + ">$2</a>");
	    }
        
	    return row;
	}
	
	
	private static final String SELECT_LOCATION = 
		"<select name=\"selLocation\" id=\"selLocation\" size=1 >" +
		"<option value=\"\">&nbsp;</option>" +
		"<option value=\"E\" >MARRIAGE LICENSE-EASTERN JACKSON</option>" +
		"<option value=\"I\" >INDEPENDENCE</option>" +
		"<option value=\"K\" >KANSAS CITY</option>" +
		"<option value=\"RC\" >RECORDS CENTER</option>"+
		"</select>";

    @Override
    public ServerResponse GetLink(String vsRequest, boolean vbEncoded) throws ServerResponseException {
    	
    	// check if it is an image request
    	if(!vsRequest.contains("docimage.asp")){
    		return super.GetLink(vsRequest, vbEncoded);
    	}

    	// construct temporary image file name
    	String folderName = getCrtSearchDir() + getServerTypeDirectory();
    	new File(folderName).mkdirs();
    	String fileName =  folderName + "tmp-" + System.nanoTime() + ".tiff";
    	
    	// retrieve image
    	retrieveImage(vsRequest, fileName);
    	
		// write the image to the client web-browser
		boolean imageOK = writeImageToClient(fileName, "image/tiff");
		
		// delete temporary image
		new File(fileName).delete();
		
		// image not retrieved
		if(!imageOK){ 
	        // return error message
			ParsedResponse pr = new ParsedResponse();
			pr.setError("<br><font color=\"red\"><b>Image not found!</b></font> ");
			throw new ServerResponseException(pr);			
		}
		// return solved response
		return ServerResponse.createSolvedResponse();

    }
    
    /**
     * Retrieve image from server
     * @param request initial image link
     * @param fileName name of the image file on disk
     */
    private void retrieveImage(String request, String fileName){
    	
    	// check if it is not already downloaded
    	if(FileUtils.existPath(fileName)){
    		return;
    	}
    	
    	String link = request.substring(request.indexOf("docimage.asp"));
    	link = link.replaceFirst("&", "?");
    	
    	HttpSite site = HttpManager.getSite("MOJacksonCC", searchId);
    	try {
    		
    		// get 1st page
    		String link1 = "http://records.co.jackson.mo.us/" + link; 
    		HTTPRequest req1 = new HTTPRequest(link1, HTTPRequest.GET);
    		String page1 = site.process(req1).getResponseAsString();
    		String startPage = StringUtils.extractParameter(page1, "name=startPage(?:\\s+size=\\d+)?\\s+value=(\\d+)");
    		String endPage = StringUtils.extractParameter(page1, "name=endPage(?:\\s+size=\\d+)?\\s+value=(\\d+)");
    		String action = StringUtils.extractParameter(page1, "<form action=\"([^\"]+)\"");
    			
    		// get 2nd page
    		String link2 = "http://records.co.jackson.mo.us" + action;
    		HTTPRequest req2 = new HTTPRequest(link2, HTTPRequest.POST);    		
    		req2.setPostParameter("startPage", startPage);
    		req2.setPostParameter("endPage", endPage);
    		req2.setPostParameter("imgRetrieve", "Retrieve as TIFF");    		
    		String page2 = site.process(req2).getResponseAsString(); 
    		
    		// get the image
    		String link3 = StringUtils.extractParameter(page2, "(?i)window\\.open \\(\"(/imgcache/[a-z0-9-]+\\.tif)\", \"_new\"\\);");
    		link3 = "http://records.co.jackson.mo.us" + link3;
    		HTTPRequest req3 = new HTTPRequest(link3, HTTPRequest.GET);
    		HTTPResponse res3 = site.process(req3);
    		FileUtils.writeStreamToFile(res3.getResponseAsStream(), fileName);
    		afterDownloadImage();
    		return;
    		
    	} finally {
    		HttpManager.releaseSite(site);
    	}
    	
    }
    
    @Override
    protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {
	
		String fileName = image.getPath();
		// download image
		retrieveImage(image.getLink(), fileName);
		
		if(FileUtils.existPath(fileName)){
			byte b[] = FileUtils.readBinaryFile(fileName);
			return new DownloadImageResult( DownloadImageResult.Status.OK, b, image.getContentType() );
		}

		return new DownloadImageResult( DownloadImageResult.Status.ERROR, new byte[0], image.getContentType() );
    }
}
