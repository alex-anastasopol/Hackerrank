/*
 * Created on Jan 26, 2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NotFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.TaxYearFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.MultipleYearIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;

/* used for TNShelbyYB(Bartlett) and TNMontgomeryYA(Clarksville)*/

public class TNGenericMSServiceCT extends TSServerAssessorandTaxLike {
	
	static final long serialVersionUID = 10000000;
	private static final String viewDetailsUrl = "/citizens/RealEstate/ParcelBrowse.aspx";

	public TNGenericMSServiceCT(long searchId) {
		super(searchId);
	}
	
	public TNGenericMSServiceCT(String rsRequestSolverName, String rsSitePath,
            String rsServerID, String rsPrmNameLink, long searchId, int mid) {
        super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
    }
	
    public static String convertToEpPid(String pid) {
    	if(StringUtils.isEmpty(pid)) {
    		return "";
    	}
    	String pieces[] = pid.split("-");
    	if(pieces.length>5) {
    		/*AO Pid*/
    		return pieces[2] + "-" + pieces[1] + "-" + pieces[3].replaceAll( "\\." , "") + pieces[5] + pieces[0];
    	}else {
    		pieces = pid.split(" ");
    		if(pieces.length>3) {
    			/*TR Pid*/
    			String newPid = pieces[0] + "-" + pieces[1] + "-";
    			for(int i=2;i<pieces.length;i++) {
    				newPid += pieces[i];
    			}
    			return newPid;
    		}else {
    			return pid;	
    		}
    	}
	}


	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException
    	{
            module.getFunction( 0 ).setParamValue( module.getFunction( 0 ).getParamValue().toUpperCase() );
            module.getFunction( 1 ).setParamValue( module.getFunction( 1 ).getParamValue().toUpperCase() );
            module.getFunction( 2 ).setParamValue( module.getFunction( 2 ).getParamValue().toUpperCase() );
            module.getFunction( 3 ).setParamValue( module.getFunction( 3 ).getParamValue().toUpperCase() );
            module.getFunction( 4 ).setParamValue( module.getFunction( 4 ).getParamValue().toUpperCase() );
            
            if( "".equals( module.getFunction( 0 ).getParamValue() ) && "".equals( module.getFunction( 1 ).getParamValue()) && "".equals( module.getFunction( 2 ).getParamValue()) && "".equals( module.getFunction( 3 ).getParamValue())  ) {
                return new ServerResponse();
            }
            
    		return super.SearchBy(module, sd);
    }
  
    protected void setModulesForAutoSearch(TSServerInfo serverInfo) 
    {
        
    	List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
        TSServerInfoModule m;
                       
        String city = getSearchAttribute(SearchAttributes.P_CITY).toUpperCase();
		if(!StringUtils.isEmpty(city)){
			if(!city.contains("BARTLETT") && !city.contains("MORRISTOWN") && !city.contains("CLARKSVILLE")){
				return;
			}			
		}
       	
		Search global = getSearch();
		int searchType = global.getSearchType();
		
		if(searchType == Search.AUTOMATIC_SEARCH) {
		
			FilterResponse addressFilter 	= AddressFilterFactory.getAddressHighPassFilter( searchId , 0.7d );
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , null );
			DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
			TaxYearFilterResponse frYear = new TaxYearFilterResponse(searchId);
			frYear.setThreshold(new BigDecimal("0.95"));
							
			if(hasPin())
			{			
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.clearSaKeys();			
				m.getFunction(3).setSaKey(SearchAttributes.TN_MONTGOMERY_EP_PID);
				m.getFunction(5).setSaKey(SearchAttributes.CURRENT_TAX_YEAR);
				m.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR);
				m.addFilter(frYear);
				
				MultipleYearIterator yearIterator = (MultipleYearIterator) ModuleStatesIteratorFactory
												.getMultipleYearIterator(m, searchId, numberOfYearsAllowed, getCurrentTaxYear());
				m.addIterator(yearIterator);
				
				l.add(m);
			}
		
			if(hasStreet()){
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.clearSaKeys();
				m.getFunction(2).setSaKey( SearchAttributes.P_STREETNAME );
				m.getFunction(1).setSaKey( SearchAttributes.P_STREETNO );
				m.getFunction(5).setSaKey(SearchAttributes.CURRENT_TAX_YEAR);
				m.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR);
				//m.addFilter(frYear);
				m.addFilter(addressFilter);
				m.addFilter(nameFilterHybrid);
				//m.addValidator(defaultLegalValidator);
				
				MultipleYearIterator yearIterator = (MultipleYearIterator) ModuleStatesIteratorFactory
													.getMultipleYearIterator(m, searchId, numberOfYearsAllowed, getCurrentTaxYear());
				m.addIterator(yearIterator);
				
				l.add(m);
			}
			
			if( hasOwner() )
		    {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.clearSaKeys();
				m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				m.getFunction(5).setSaKey(SearchAttributes.CURRENT_TAX_YEAR);
				m.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_MULTIPLE_YEAR);
				m.addFilter(NameFilterFactory.getDefaultNameFilterNoSinonims( SearchAttributes.OWNER_OBJECT, searchId, m));
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
				ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
										.getConfigurableNameIterator(numberOfYearsAllowed, m, searchId, new String[] {"L;F M;","L;f m;","L;f;","L;m;"});
				//m.addFilter(frYear);
				m.addFilter(addressFilter);
				m.addIterator(nameIterator);
				
				l.add(m);
		    }
		
		}
	
		serverInfo.setModulesForAutoSearch(l);
	
      }

    protected void ParseResponse(String sAction, ServerResponse Response,
            int viParseID) throws ServerResponseException {

        String rsResponse = Response.getResult();
        
		switch (viParseID) {

		case ID_SEARCH_BY_NAME:

			if (rsResponse.contains("No Parcels found based on specified search criteria.")
					|| rsResponse.contains("An error has occurred.")) {
				return;
			}

			try {

				StringBuilder outputTable = new StringBuilder();
				ParsedResponse parsedResponse = Response.getParsedResponse();

				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

				if (smartParsedResponses.size() > 0) {
					parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
					parsedResponse.setOnlyResponse(outputTable.toString());
					parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			break;
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			HtmlParser3 parser= new HtmlParser3(rsResponse);
			String accountId = parser.getNodePlainTextById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_ViewBill1_CategoryLabel");
			String taxYear = "";
			try {
				taxYear = parser
						.getNodePlainTextById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_ViewBill1_FiscalYearLabel");
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (StringUtils.isNotEmpty(taxYear)){
				taxYear = taxYear.replaceAll("<br>", "\n").replaceAll("[\\-\\$,]", "").trim();
			}
			
			if(StringUtils.isEmpty(accountId)) {
				return;
			}
			
			String details = getDetails(rsResponse, accountId);
			String filename = accountId + ".html";

			if (viParseID == ID_DETAILS) {
				String originalLink = sAction + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type","CITYTAX");
				data.put("year", taxYear);
				data.put("dataSource", getDataSite().getSiteTypeAbrev());
					
				if (isInstrumentSaved(accountId, null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink,
							viParseID);
				}

				Response.getParsedResponse().setPageLink(
						new LinkInPage(sSave2TSDLink, originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);

			} else {
				smartParseDetails(Response, details);

				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(
						getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();

			}
			break;
		case ID_GET_LINK:
		case ID_SEARCH_BY_ADDRESS: /* Just so parent site search redirects here. */
			ParseResponse(sAction, Response,rsResponse.contains("Found")
					? ID_SEARCH_BY_NAME
					: ID_DETAILS);
			break;

		}
    }
    
	private String getDetails(String response, String accountNo) {
		
		/* If from memory - use it as is */
		if(!response.contains("<html")){
			return response;
		}
						
		HtmlParser3 parser= new HtmlParser3(response);
		//TableTag mainTable = HtmlParser3.getFirstTag(parser.getNodeById("content").getChildren(),TableTag.class,true);
		Div viewBillDiv = (Div) parser.getNodeById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_ViewBill1_ViewBillControlPanel");
		String viewBill = "";
		if (viewBillDiv != null){//TNMontgomeryYA has veiw bill table in a div. TNShelbyYB remains old style
			viewBill = viewBillDiv.toHtml();
		}
		
				
		String propertyDetailPage = "", ownerInformationPage ="", assessmentPage ="", chargesPage = "", allBillsPage="" , allBillsPageTable="" , receiptsPage = "";
		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		try {
			
			
			propertyDetailPage = ((ro.cst.tsearch.connection.http2.TNMontgomeryEP)site).getPage("ParcelDetail.aspx");
			HtmlParser3 parserDetail = new HtmlParser3(propertyDetailPage);
			propertyDetailPage = parserDetail.getNodeById("ParcelTable").toHtml();
			
			ownerInformationPage = ((ro.cst.tsearch.connection.http2.TNMontgomeryEP)site).getPage("OwnerInformation.aspx");
			parserDetail = new HtmlParser3(ownerInformationPage);
			ownerInformationPage = parserDetail.getNodeByAttribute("class", "informationtable", true).toHtml();
			
			chargesPage = ((ro.cst.tsearch.connection.http2.TNMontgomeryEP)site).getPage("TaxCharges.aspx");
			parserDetail = new HtmlParser3(chargesPage);
			NodeList chargesNodes = parserDetail.getNodeById("content").getChildren();
			chargesNodes.keepAllNodesThatMatch(new NotFilter(new HasAttributeFilter("class", "h2background")));
			chargesNodes.keepAllNodesThatMatch(new NotFilter(new HasAttributeFilter("id", "breadcrumbs")));
			chargesPage = chargesNodes.toHtml();
			
			assessmentPage = ((ro.cst.tsearch.connection.http2.TNMontgomeryEP)site).getPage("Assessments.aspx");
			parserDetail = new HtmlParser3(assessmentPage);
			NodeList assessmentNodes = parserDetail.getNodeById("content").getChildren();
			assessmentNodes.keepAllNodesThatMatch(new NotFilter(new HasAttributeFilter("class", "h2background")));
			assessmentNodes.keepAllNodesThatMatch(new NotFilter(new HasAttributeFilter("id", "breadcrumbs")));
			assessmentPage = assessmentNodes.toHtml();
			
			receiptsPage = ((ro.cst.tsearch.connection.http2.TNMontgomeryEP)site).getPage("ViewPayments.aspx");
			parserDetail = new HtmlParser3(receiptsPage);
			receiptsPage = parserDetail.getNodeContentsByAttribute("class", "datatable nomargin");
			
			allBillsPage = ((ro.cst.tsearch.connection.http2.TNMontgomeryEP)site).getPage("AllBills.aspx");
			parserDetail = new HtmlParser3(allBillsPage);
			
			
			String taxYear = parser.getNodePlainTextById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_ViewBill1_FiscalYearLabel");
			
			TableTag allBillsTable = (TableTag)parserDetail.getNodeById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_BillsRepeater_ctl00_BillsGrid");
			
			for(TableRow row : allBillsTable.getRows()) {
				if(row.getColumnCount() > 4) {
					TableColumn[] cols = row.getColumns();
					String year = cols[2].toPlainTextString().trim();
					String paid = cols[4].toPlainTextString().trim();
					
					if("Outstanding".equalsIgnoreCase(paid) && !year.equalsIgnoreCase(taxYear)) {
						String link = HtmlParser3.getFirstTag(cols[5].getChildren(), LinkTag.class, true).getLink();
						String viewBillTarget = StringUtils.extractParameter(link,"(?ism)__doPostBack\\('(.*?)'");;	
						
						Map<String,String> params = HttpSite.fillConnectionParams(allBillsPage,
									new String[] { "__VIEWSTATE", "__EVENTVALIDATION", "__EVENTARGUMENT" }, 
									"aspnetForm");
						
						params.put("__EVENTTARGET", viewBillTarget);
						params.put("noparams", "true");
						((ro.cst.tsearch.connection.http2.TNMontgomeryEP) site).getPostPage("AllBills.aspx",params);
						String viewDelinquentBillPage =  ((ro.cst.tsearch.connection.http2.TNMontgomeryEP) site).getPage("ViewBill.aspx");
						viewDelinquentBillPage = viewDelinquentBillPage.replaceAll("(?i)<th([^>]*?)>", "<td$1>").replaceAll("(?i)</th>", "</td>");
						HtmlParser3 delinquentParser = new HtmlParser3(viewDelinquentBillPage);
						String delinqAmount = HtmlParser3.getValueFromNearbyCell(4,HtmlParser3.findNode(delinquentParser.getNodeList(),"TOTAL"),"",false);
						cols[4].setAttribute("class", delinqAmount);
						cols[5].removeChild(1);
						cols[5].getChild(0).setText(delinqAmount);
					} else {
						cols[5].removeChild(1);
					}
				}
			}
			
			allBillsPageTable = allBillsTable.toHtml();
				
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			HttpManager.releaseSite(site);
		}
		
		response = "<table border=1 style='border-collapse: collapse;' >" +
						"<tr><td>"+
						/*mainTable.toHtml() +*/ viewBill + 
						receiptsPage +
						"</td>" +
						"<td align='left'><strong>Owner Information</strong>"+
						ownerInformationPage +
						"</td></tr>" +  
						"<tr><td align='left'><strong>Property detail</strong>"+
						propertyDetailPage.replace("60%","80%")/*.replaceAll("<(td|th)","<$1 style='text-align:left'")*/ +
						"</td>" +  
						"<td align='left'>"+
						assessmentPage +
						"</td></tr>" +
						"<tr><td>"+
						chargesPage + 
						"</td>" +
						"<td align='left'><strong>All Bills</strong>"+
						allBillsPageTable +
						"</td></tr>" +  
					"</table>";
		
		response = response.replaceAll("(?ism)<a[^>]*?>(.*?)</a>", "$1")
				.replaceAll("(?ism)View Payments/Adjustments", "").replaceAll("(?is)<input\\s+type\\s*=\\s*\\\"submit\\\"[^>]+>", "");
		
		return response;
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		 Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();;
		
		try {
			table = table.replaceAll("(?ism)<a href=[^>]*?>(.*?)</a>", "$1");
			 			
			HtmlParser3 parser = new HtmlParser3(table);
			TableTag mainTable = (TableTag) parser.getNodeById("ctl00_ctl00_PrimaryPlaceHolder_ContentPlaceHolderMain_MolGridView1");
						
			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
			Map<String,String> params;
			try {
				params = HttpSite.fillConnectionParams(table,
						((ro.cst.tsearch.connection.http2.TNMontgomeryEP) site).getTargetArgumentParameters(), 
						"aspnetForm");
				params.remove("__EVENTTARGET");
			} finally {
				// always release the HttpSite
				HttpManager.releaseSite(site);
			}
			int seq = getSeq();
			mSearch.setAdditionalInfo(getCurrentServerName() + ":params:" + seq, params);
			
			TableRow[] rows = mainTable.getRows();
			for(TableRow row : rows ) {
				if(row.getColumnCount() > 0) {
					
					String link = HtmlParser3.getFirstTag(row.getColumns()[6].getChildren(), LinkTag.class, true).getLink();
					link = StringUtils.HTMLEntityDecode(link);
					String eventTarget = StringUtils.extractParameter(link,"(?ism)__doPostBack\\('(.*?)'");
						
					link = CreatePartialLink(TSConnectionURL.idPOST) + viewDetailsUrl+"?mode=details&__EVENTTARGET=" + /*URLEncoder.encode(*/eventTarget/*,"UTF-8")**/ + "&seq="+seq;
					String rowHtml =  row.toHtml();
					if (rowHtml.contains("Pay Bill"))
						rowHtml = rowHtml.replaceFirst("<a.*?>View Bill</a>\\s*(?:[^<]+<a[^>]+>\\s*Pay Bill\\s*</a>)?","<a href=\""+Matcher.quoteReplacement(link)+"\">View</a> (Pay Bill)" );
					else
						rowHtml = rowHtml.replaceFirst("<a.*?>View Bill</a>\\s*","<a href=\""+Matcher.quoteReplacement(link)+"\">View</a>" );
					
					ParsedResponse currentResponse = new ParsedResponse();
					currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
					currentResponse.setOnlyResponse(rowHtml);
					currentResponse.setPageLink(new LinkInPage(link,link,TSServer.REQUEST_SAVE_TO_TSD));
					
					ResultMap m = parseIntermediaryRow(row);
					
					Bridge bridge = new Bridge(currentResponse,m,searchId);
					
					DocumentI document = (TaxDocumentI)bridge.importData();				
					currentResponse.setDocument(document);
					
					intermediaryResponse.add(currentResponse);
				}
			}
			
		String header0 =  rows[0].toHtml();
		//String header1 = rows[1].toHtml();
		
		//if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH ) {
			response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n"+ header0 );
			response.getParsedResponse().setFooter("</table>");
	    //}
		
		outputTable.append(table);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	protected ResultMap parseIntermediaryRow(TableRow row) {
		return ro.cst.tsearch.servers.functions.TNMontgomeryEP.parseIntermediaryRow( row, miServerID);
	}
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap map) {
		ro.cst.tsearch.servers.functions.TNMontgomeryEP.parseAndFillResultMap(detailsHtml,map,searchId, miServerID);
		return null;
	}
	
	private static int seq = 0;	
	protected synchronized static int getSeq(){
		return seq++;
	}

}