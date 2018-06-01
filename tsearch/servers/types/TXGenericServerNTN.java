package ro.cst.tsearch.servers.types;


import java.io.File;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.htmlparser.Text;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.HasChildFilter;
import org.htmlparser.filters.StringFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.w3c.dom.Document;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parentsitedescribe.ComboValue;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.GenericAddressFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.functions.TXGenericFunctionsNTN;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.XmlUtils;
import ro.cst.tsearch.utils.tags.WhateverTag;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.TaxDocumentI;


/**
 * @author mihaib
*/


@SuppressWarnings("deprecation")
public class TXGenericServerNTN extends TSServer{

	private static final long serialVersionUID = 1L;
	private boolean downloadingForSave;
	
	protected static final String CLASSES_FOLDER = BaseServlet.REAL_PATH
			+ File.separator + "WEB-INF" + File.separator + "classes"
			+ File.separator;
	
	protected static final String RESOURCE_FOLDER = (CLASSES_FOLDER
			+ "resource" + File.separator + "NTN" + File.separator).replaceAll("//", "/");
	
	private static final Pattern DETAIL_LINK_PAT = Pattern.compile("(?is)<xsl:attribute name=\\\"href\\\">\\s*([^<]+)<xsl:value-of select=\\\"table-rowid\\\"/>");

	public TXGenericServerNTN(long searchId) {
		super(searchId);
	}

	public TXGenericServerNTN(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,	int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,	miServerID);
	}

	@Override
	public TSServerInfo getDefaultServerInfo() {
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
		
		String county = getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY_NAME).toLowerCase();
		
		if(tsServerInfoModule != null) {
			if (tsServerInfoModule.getModuleParentSiteLayout() != null){
		        PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
				for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
					String functionName = htmlControl.getCurrentTSSiFunc().getName();
					if(StringUtils.isNotEmpty(functionName)) {
						if(functionName.toLowerCase().contains("agency")) {
							LinkedList<ComboValue>  comboList = htmlControl.getComboList();
							if (comboList != null){
								for (ComboValue comboValue : comboList) {
									if (comboValue.getValue().toLowerCase().contains(county)){
										htmlControl.setDefaultValue(comboValue.getValue());
										htmlControl.getCurrentTSSiFunc().setDefaultValue(comboValue.getValue());
										break;
									}
								}
							}
						}
					}
				}
			}
		}
				
		setModulesForAutoSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
		
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		if(searchType == Search.AUTOMATIC_SEARCH) {
        
			int countyId = Integer.parseInt(global.getCountyId());
			
			TSServerInfoModule module = null;
			String abstrFileNo = getSearchAttribute(SearchAttributes.ABSTRACTOR_FILENO);
			
			GenericAddressFilter addressFilter 	= AddressFilterFactory.getGenericAddressHighPassFilter( searchId, 0.8d );
			FilterResponse defaultNameFilter = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT , searchId , module );
			//R + PID
			//Atascosa, Bastrop, Burnet, Denton, Grayson, Kerr, 
			//
			
			//PID
			//Bandera, Bell, Bowie, Brazos, Chambers, Cooke, Dallas, Ellis, Harris, Jefferson(from AO or NB without dashes and last digit),
			//Jim Wells, Panola, Potter(minus last 2 chars), Randall (first 12 digits),Rockwall, Smith, Tarrant(last 8 digits), Taylor, 
			//Tom Green, Wichita, Williamson, Zapata (adding leading 0 to length 12)
			
			//without dashes or points
			//, Brazoria(pid from NB=GEO from AO) 
			
			//(has already R)
			//Erath, Hays, Lubbock, Medina, 
			
			//PID from NB Ector, Fort Bend, Galveston, Hardin, Montgomery, Henderson, Hidalgo, Jim Hogg, Johnson, McLennan, Orange,
			//Potter(minus leading R and minus last 0), Potter(minus leading R)San Patricio
			
			//0+ + PID
			//Brooks(000000005908), Somervell(minus R and leading 0), Travis (adding zeros at end to reach 14 length), 
			if (hasPin()){
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();
				
				String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO);
				pin = preparePin(pin, countyId);

				module.forceValue(8, pin);
				module.forceValue(15, abstrFileNo);
				modules.add(module);
				
				if (countyId == CountyConstants.TX_Somervell && pin.length() == 4){
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
					module.clearSaKeys();

					module.forceValue(8, "1" + pin);
					module.forceValue(15, abstrFileNo);
					modules.add(module);
				}
			}			
			
			//GEO number from AO
			//with dashes or points
			//Angelina, Hardin (Long account number from AO), Hunt, Jim Hogg(other format), Kaufman, Navarro, 
			
			//without dashes or points
			//Bexar, Cameron, Collin, Comal, Coryell, El Paso, Guadalupe, Henderson, Hidalgo, Hood, Jim Hogg, Kendall,
			//La Salle, Liberty, McLennan, Maverick, Midland(without leading zeros), Nueces, Palo Pinto, Parker, Van Zandt, Victoria,
			//Waller, Webb, Wilson, Wise (adding ends 0 to length 15), 
			if (hasGeoNumber()){
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();
				
				String geoNumber = getSearchAttribute(SearchAttributes.LD_GEO_NUMBER);
				geoNumber = prepareGeoNumber(geoNumber, countyId);
				
				module.forceValue(8, geoNumber);
				module.forceValue(15, abstrFileNo);
				modules.add(module);
			}
			
			if (hasStreet()) {
				//Search by Property Address
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();
				module.setSaKey(4, SearchAttributes.P_STREETNO);
				module.setSaKey(5, SearchAttributes.P_STREETNAME);
				module.forceValue(15, abstrFileNo);
				module.addFilter(addressFilter);
				module.addFilter(defaultNameFilter);
				modules.add(module);
			}
			
			if (hasOwner()){
				//Search by Owner
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();
				
				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				module.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(7, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				module.forceValue(15, abstrFileNo);
				
				module.addFilter(addressFilter);
				module.addFilter(defaultNameFilter);
				ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
											.getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
				module.addIterator(nameIterator);
				modules.add(module);
			}        
	       
		}
	    serverInfo.setModulesForAutoSearch(modules);
	}
	
	private String preparePin(String pin, int countyId){
		
		if (countyId == CountyConstants.TX_Fort_Bend || countyId == CountyConstants.TX_Brazoria){
			pin = pin.replaceAll("\\p{Punct}", "");
		} else if (countyId == CountyConstants.TX_Jefferson){
			pin = pin.replaceAll("\\p{Punct}", "");
			if (pin.length() > 20){
				pin = pin.substring(0, pin.length() - 1);
			}
		} else if (countyId == CountyConstants.TX_Atascosa || countyId == CountyConstants.TX_Midland
					|| countyId == CountyConstants.TX_Bastrop || countyId == CountyConstants.TX_Burnet
					|| countyId == CountyConstants.TX_Grayson|| countyId == CountyConstants.TX_Kerr){
			if (!pin.startsWith("R")){
				pin = "R" + pin;
			}
		} else if (countyId == CountyConstants.TX_Denton){
			pin = pin.replaceAll("DEN$", "");
			if (!pin.startsWith("R")){
				pin = "R" + pin;
			}
		} else if (countyId == CountyConstants.TX_Collin){
			pin = org.apache.commons.lang.StringUtils.leftPad(pin, 8, '0');
		} else if (countyId == CountyConstants.TX_Brooks){
			pin = org.apache.commons.lang.StringUtils.leftPad(pin, 12, '0');
		} else if (countyId == CountyConstants.TX_Potter){
			if (pin.startsWith("R")){//NB PID
				pin = pin.replaceFirst("(?is)\\AR", "");
				pin = pin.substring(0, pin.length() - 1);
			} else {//AO PID
				pin = pin.substring(0, pin.length() - 2);
			}
		} else if (countyId == CountyConstants.TX_Randall){
			if (pin.startsWith("R")){//NB PID
				pin = pin.replaceFirst("(?is)\\AR", "");
			} else {//AO PID
				pin = pin.substring(0, 12);
			}
		} else if (countyId == CountyConstants.TX_Somervell){
			if (pin.startsWith("R")){//AO PID
				pin = pin.replaceFirst("(?is)\\AR", "");
				pin = org.apache.commons.lang.StringUtils.stripStart(pin, "0");
			}
		} else if (countyId == CountyConstants.TX_Wise){
			if (pin.length() == 11){
				pin = org.apache.commons.lang.StringUtils.rightPad(pin, 15, "0");
			}
		} else if (countyId == CountyConstants.TX_Zapata){
			if (pin.length() < 12){
				pin = org.apache.commons.lang.StringUtils.rightPad(pin, 12, "0");
			}
		}
		
		return pin;
	}
	
	private String prepareGeoNumber(String geoNumber, int countyId){
		
		if (countyId == CountyConstants.TX_Nueces || countyId == CountyConstants.TX_Liberty || countyId == CountyConstants.TX_Waller
				|| countyId == CountyConstants.TX_Guadalupe || countyId == CountyConstants.TX_Nueces || countyId == CountyConstants.TX_Comal
				|| countyId == CountyConstants.TX_Kendall || countyId == CountyConstants.TX_Collin || countyId == CountyConstants.TX_Parker
				|| countyId == CountyConstants.TX_Webb || countyId == CountyConstants.TX_Midland || countyId == CountyConstants.TX_Maverick
				|| countyId == CountyConstants.TX_Cameron || countyId == CountyConstants.TX_Coryell || countyId == CountyConstants.TX_Henderson
				|| countyId == CountyConstants.TX_La_Salle || countyId == CountyConstants.TX_McLennan || countyId == CountyConstants.TX_Palo_Pinto
				|| countyId == CountyConstants.TX_Wilson || countyId == CountyConstants.TX_Wise){
			geoNumber = geoNumber.replaceAll("\\p{Punct}", "");
		}
		
		if (countyId == CountyConstants.TX_Wise){
			geoNumber = org.apache.commons.lang.StringUtils.rightPad(geoNumber, 15, "0");
		}
		
		return geoNumber;
	}
	
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		
		if (rsResponse.indexOf("HOA Required Order Fields") != -1){
			Response.getParsedResponse().setError("The following additional information is required by the HOA Administrator for<br>"
						+ " the account(s) you have associated with this Tax Certification Order.");
			return;
		}
		
		switch (viParseID){
			case ID_SEARCH_BY_NAME :
				
				if (rsResponse.contains("Parcel Detail")) {
					ParseResponse(sAction, Response, ID_DETAILS);
					return;
				}
				
				try {

					StringBuilder outputTable = new StringBuilder();
					ParsedResponse parsedResponse = Response.getParsedResponse();

					Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(
							Response, rsResponse, outputTable);

					if (smartParsedResponses.size() > 0) {
						parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
						parsedResponse.setOnlyResponse(outputTable.toString());
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
				
				break;
				
				
			case ID_DETAILS:
				
				String details = getDetails(rsResponse, Response);
				String docNo = "";
				
				org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(details, null);

				try {
					NodeList mainList = htmlParser.parse(null);
					docNo = HtmlParser3.getValueFromAbsoluteCell(1, 0,  HtmlParser3.findNode(mainList, "ACCOUNT NUMBER"), "", true).trim();
					
					/*String legal = HtmlParser3.getValueFromAbsoluteCell(1, 0,  HtmlParser3.findNode(mainList, "LEGAL DESCRIPTION"), "", true).trim();
					String owners = HtmlParser3.getValueFromAbsoluteCell(1, 0,  HtmlParser3.findNode(mainList, "OWNER INFORMATION"), "", true).trim();
					String propertyAddress = HtmlParser3.getValueFromAbsoluteCell(1, 0,  HtmlParser3.findNode(mainList, "PROPERTY ADDRESS"), "", true).trim();
					
					if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(legal.trim())){
						FileWriter fw = new FileWriter(new File("D:\\NTN\\xx NTN legal.txt"),true);
						fw.write(docNo + "#####" + legal + "#####" + owners + "#####" + propertyAddress);
						fw.write("\n");
						fw.close();
					}*/
					
					/*LinkTag aList = (LinkTag) HtmlParser3.getNodeByID("imageLink", mainList, true);
					if (aList != null){
						String imageLink = aList.getLink();
						if (Response.getParsedResponse().getImageLinksCount() == 0){
							if (StringUtils.isNotEmpty(imageLink)){
								Response.getParsedResponse().addImageLink(new ImageLinkInPage(imageLink, docNo + ".pdf"));
							}
						}
					}*/
					
				} catch (ParserException e1) {
					e1.printStackTrace();
				} //catch (IOException e) {
					//e.printStackTrace();
				//}
				
				if ((!downloadingForSave)){	
	                String qry_aux = Response.getRawQuerry();
					qry_aux = "dummy=" + docNo + "&" + qry_aux;
					String originalLink = sAction + "&" + qry_aux;
					originalLink = originalLink.replaceAll("(?is)&$", "");
					try {
						originalLink = URLDecoder.decode(originalLink, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;
					
					HashMap<String, String> data = new HashMap<String, String>();
    				data.put("type", "CNTYTAX");
	    				
					if (isInstrumentSaved(docNo, null, data)){
	                	details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
					}
					Response.getParsedResponse().setPageLink(
							new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
					parser.Parse(Response.getParsedResponse(), details,	Parser.NO_PARSE); 
	            } 
				else {
					//details = details.replaceAll("(?is)<a[^>]*>[^<]*</a>", "");
					smartParseDetails(Response, details);
	                msSaveToTSDFileName = docNo + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
	               
				}
				
				break;
			case ID_SAVE_TO_TSD :
				
				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
				
				break;

			case ID_GET_LINK :
				if(sAction.contains("tc-property-detail")) {
    				ParseResponse(sAction, Response, ID_DETAILS);
    			}
				break;
			default:
				break;
		
		}
		
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String html, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		html = response.getResult();
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(html, null);
			
			WhateverTag addTags = new WhateverTag();
			htmlParser.setNodeFactory(addTags.addNodeFactory());
			
			NodeList nodeList = htmlParser.parse(null);
			NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("xml"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("id", "dqsource"), true);

			String newTAble = "";
			String header = "";
			
			if (tableList != null && tableList.size() > 0){
				newTAble = tableList.elementAt(0).getChildren().toHtml();
				newTAble = newTAble.replaceAll("(?is)\\bDBRecords\\b", "table");
				newTAble = newTAble.replaceAll("(?is)\\bDBRecord\\b", "tr");
				newTAble = newTAble.replaceAll("(?is)\\bPar-No\\b", "td");
				
				//for legal search
				newTAble = newTAble.replaceAll("(?is)\\bsubdivision\\b", "td");
				newTAble = newTAble.replaceAll("(?is)\\bblock\\b", "td");
				newTAble = newTAble.replaceAll("(?is)\\blot\\b", "td");
				//for legal search
				
				newTAble = newTAble.replaceAll("(?is)\\bdq-situs\\b", "td");
				newTAble = newTAble.replaceAll("(?is)\\bdq-ownername\\b", "td");
				newTAble = newTAble.replaceAll("(?is)\\btable-rowid\\b", "td");
				newTAble = newTAble.replaceAll("(?is)<xml-rowid>[^<]*</xml-rowid>", "");
				
				newTAble = newTAble.trim();
				
				htmlParser = org.htmlparser.Parser.createParser(newTAble, null);
				nodeList = htmlParser.parse(null);

				TableTag mainTable = (TableTag) nodeList.elementAt(0);
				
				TableRow[] rows = mainTable.getRows();
			
				Matcher detailLinkMat = DETAIL_LINK_PAT.matcher(response.getResult());
				
				if (detailLinkMat.find()){
					
					String lnk =  CreatePartialLink(TSConnectionURL.idGET) + detailLinkMat.group(1);
					
					String id = "", accountNumber = "", subdivisionCode = "", address = "", owner = "", block = "", lot = "";
					
					for(TableRow row : rows ) {
						TableColumn[] cols = row.getColumns();
						if(row.getColumnCount() == 4) {
							accountNumber = cols[0].toPlainTextString();
							address = cols[1].toPlainTextString();
							owner = cols[2].toPlainTextString();
							id = cols[3].toPlainTextString();
							if (StringUtils.isEmpty(header)){
								header = "<tr><th>Account Number</th><th>Address</th><th>Owner Name</th></tr>";
							}
							
						} else if(row.getColumnCount() == 7) {							
							accountNumber = cols[0].toPlainTextString();
							subdivisionCode = cols[1].toPlainTextString();
							block = cols[2].toPlainTextString();
							lot = cols[3].toPlainTextString();
							address = cols[4].toPlainTextString();
							owner = cols[5].toPlainTextString();
							id = cols[6].toPlainTextString();
							if (StringUtils.isEmpty(header)){
								header = "<tr><th>Account Number</th><th>Subdivision</th><th>Block</th>"
										+ "<th>Lot</th><th>Address</th><th>Owner Name</th></tr>";
							}
						} else {
							continue;
						}
							
						String link = lnk + id;

						String rowHtml =  row.toHtml();
							
						rowHtml = rowHtml.replaceAll("(?is)<td[^>]*>[^<]*</td>\\s*(</tr>)", "$1");
						rowHtml = rowHtml.replaceAll("(?is)(<td[^>]*>)([^<]*)(</td>)", "$1 <a href=\"" + link + "\">$2</a>$3");

						ParsedResponse currentResponse = new ParsedResponse();
						currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
						currentResponse.setOnlyResponse(rowHtml);
						currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
								
						ResultMap resultMap = new ResultMap();
							
						resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "NTN");
						resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountNumber);
						
						address = prepareAddress(address, mSearch);
						
						resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
						resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
						
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_CODE.getKeyName(), subdivisionCode);
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lot);
						resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);
						resultMap.put("tmpOwner", owner);
							
						partyNames(resultMap, searchId);
							
						resultMap.removeTempDef();
						Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
								
						DocumentI document = (TaxDocumentI)bridge.importData();
								
						currentResponse.setDocument(document);	
						intermediaryResponse.add(currentResponse);
					}
				}
			

			}
			
			StringBuilder newTable = new StringBuilder();
			newTable.append("<table BORDER='1' CELLPADDING='2'>")
					.append(header);
			
			response.getParsedResponse().setHeader("<table width=\"100%\" border=\"1\" cellspacing=\"0\" cellpadding=\"2\">\n" + header);
			
			response.getParsedResponse().setFooter("</table>");
			
			newTable.append("</table>");
			outputTable.append(newTable);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	protected String getDetails(String response, ServerResponse Response){
		
		// if from memory - use it as is
		if(!response.toLowerCase().contains("<html")){
			return response;
		}
		
		//StringBuffer pdfLink = new StringBuffer();
		
		StringBuffer details = new StringBuffer();
		
		Pattern viewCertificate = Pattern.compile("(?is)href=\\\"([^\\\"]+)\\\"[^>]*>\\s*View Certificate\\s*</A>");
		Matcher viewCertMat = viewCertificate.matcher(response);
		StringBuffer certificateLink = new StringBuffer();
		boolean noTaxCert = false;
		if (response.contains("No Active Order")){
			noTaxCert = true;
		} else {
			if (viewCertMat.find()){
				certificateLink.append(viewCertMat.group(1));
			} else {
				viewCertificate = Pattern.compile("(?is)baseTarget\\s*=\\s*'([^']+)");
				viewCertMat.usePattern(viewCertificate);
				viewCertMat = viewCertificate.matcher(response);
				if (viewCertMat.find()){
					certificateLink.append(viewCertMat.group(1)).append("&GoTo=ViewCertificate");
				}
			}
		}
		Pattern HEADER_PAT = Pattern.compile("(?i)(<nobr>Parcel Detail[^\\)]+\\s*\\)\\s*<nobr>)");
		Matcher mat = HEADER_PAT.matcher(response);
		if (mat.find()){
			details.append("<font size=\"3\">").append(mat.group(1)).append("</font>");
		}
		
		//String docNo = "";
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList mainList = htmlParser.parse(null);

			if (mainList != null && mainList.size() > 0){
				NodeList divList = mainList.extractAllNodesThatMatch(new TagNameFilter("div"), true);
				if (divList != null && divList.size() > 0){
					details.append(divList.extractAllNodesThatMatch(new HasAttributeFilter("id", "topinfo")).toHtml());
					details.append(divList.extractAllNodesThatMatch(new HasAttributeFilter("id", "property")).toHtml());
					details.append(divList.extractAllNodesThatMatch(new HasAttributeFilter("id", "characteristics")).toHtml());
					
					response = details.toString();
				}

				//docNo = HtmlParser3.getValueFromAbsoluteCell(1, 0,  HtmlParser3.findNode(mainList, "ACCOUNT NUMBER"), "", true).trim();

			}			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (noTaxCert){
			response = "No Tax Certificate. GF Number was not completed!<br><br>" + response;
		} else {
			
			
			
			
			String resp = getLinkContents(getDefaultServerInfoWrapper().getServerLink() + certificateLink.toString());
//			HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
//			try {
//				resp = ((ro.cst.tsearch.connection.http2.TXGenericConnNTN)site).getPage(certificateLink.toString()).getResponseAsString(); 
//			}catch(Exception e) {
//				e.printStackTrace();
//			}finally {
//				HttpManager.releaseSite(site);
//			}
			if (resp.contains("HOA Required Order Fields")){
				response = "Tax Certificate unavailable.<br><br>" 
							+ "Additional information is required by the HOA Administrator for"
							+ " the account(s) you have associated with this Tax Certification Order.<br><br>" + response;
			} else {
				
				try {
					org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(resp, null);
					
					WhateverTag addTags = new WhateverTag();
					htmlParser.setNodeFactory(addTags.addNodeFactory());
					
					NodeList mainList = htmlParser.parse(null);

					if (mainList != null && mainList.size() > 0){
						NodeList xmlList = mainList.extractAllNodesThatMatch(new TagNameFilter("xml"), true);
						if (xmlList != null && xmlList.size() > 0){
							String xmlSource = xmlList.extractAllNodesThatMatch(new HasAttributeFilter("id", "xmlsource"), true).toHtml();
														
							if (StringUtils.isNotEmpty(xmlSource)){
								Document xmlSourceDoc = XmlUtils.parseXml(xmlSource,"UTF-8");
								
								try {
									File style = new File(RESOURCE_FOLDER + "CertificateNTN.xsl");
						    	    TransformerFactory tFactory = TransformerFactory.newInstance();
						    	    Transformer transformer = tFactory.newTransformer(new StreamSource(style));
						    	    StringWriter outputWriter = new StringWriter();
						    	    transformer.transform(new DOMSource(xmlSourceDoc), new StreamResult (outputWriter));
						    	    
						    	    //transformer.transform(new DOMSource(xmlSourceDoc), 
						    	    		//new StreamResult (new FileOutputStream("D:\\NTN\\raspuns din ats.html")));
						    	    
						    	    String taxCertificate = outputWriter.toString();
						    	    taxCertificate = taxCertificate.replaceAll("(?is)</?HTML[^>]*>", "")
						    	    		.replaceAll("(?is)</?HEAD[^>]*>", "").replaceAll("(?is)</?META[^>]*>", "")
						    	    		.replaceAll("(?is)</?title[^>]*>", "").replaceAll("(?is)</?body[^>]*>", "");

						    	    details.append("<br><br><hr><br><br><center><h2><b>Tax Certificate</b></h2></center><br><br>").append(taxCertificate);
								} catch (Exception e) {
						    	    e.printStackTrace( );
					    	    }
							}
							response = details.toString();
						}

					}			
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				/*Response.getParsedResponse().addImageLink(new ImageLinkInPage(
							CreatePartialLink(TSConnectionURL.idGET) + pdfLink.toString(), docNo + ".pdf"));
				StringBuffer link = new StringBuffer("<a href=\"");
				link.append(CreatePartialLink(TSConnectionURL.idGET) + pdfLink.toString()).append("\" target=\"_blank\" id=\"imageLink\">View Certificate as PDF</a><HR>");
				
				response = link.toString() + response;*/				
			}
		}
		
		response = response.replaceAll("(?is)<img[^>]*>", "");
		response = response.replaceAll("(?is)<a[^>]*>([^<]*)</a>", "$1");
		
		return response;
	}
	/*
	@Override
	protected DownloadImageResult saveImage(ImageLinkInPage image) throws ServerResponseException {

		String link = image.getLink();

		byte[] imageBytes = null;

    	HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		try {
			imageBytes = ((ro.cst.tsearch.connection.http2.TXGenericConnNTN)site).getPage(link).getResponseAsByte(); 
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			HttpManager.releaseSite(site);
		}
		
		ServerResponse resp = new ServerResponse();
			
		if(imageBytes != null) {
			afterDownloadImage(true);
		} else {
			return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
		}
		
		String imageName = image.getPath();
		if(FileUtils.existPath(imageName)){
			imageBytes = FileUtils.readBinaryFile(imageName);
		   		return new DownloadImageResult( DownloadImageResult.Status.OK, imageBytes, image.getContentType());
		}
		    	
		resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes,
					((ImageLinkInPage)image).getContentType()));

		DownloadImageResult dres = resp.getImageResult();
		
	return dres;
	}*/
	
	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
			detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&amp;", "&")
									.replaceAll("(?is)</?p[^>]*>", "").replaceAll("(?is)</?strong[^>]*>", "");
						
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			
			WhateverTag addTags = new WhateverTag();
			htmlParser.setNodeFactory(addTags.addNodeFactory());
			
			NodeList mainList = htmlParser.parse(null);
			
			String pid = HtmlParser3.getValueFromAbsoluteCell(1, 0,  HtmlParser3.findNode(mainList, "ACCOUNT NUMBER"), "", true).trim();
			if (StringUtils.isNotEmpty(pid)){
				resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), pid.replaceAll("-", "").trim());
			}
			
			String geoId = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "GEO ID"), "", true).trim();
			if (StringUtils.isNotEmpty(geoId)){
				resultMap.put(PropertyIdentificationSetKey.GEO_NUMBER.getKeyName(), geoId.trim());
			}
			
			String propertyAddress = HtmlParser3.getValueFromAbsoluteCell(1, 0,  HtmlParser3.findNode(mainList, "PROPERTY ADDRESS"), "", true).trim();
			if (StringUtils.isNotEmpty(propertyAddress)){
				propertyAddress = prepareAddress(propertyAddress, mSearch);
								
				String[] addressParts = propertyAddress.split("(?is)\\s*<br/?>\\s*");
				if (addressParts.length > 0){
					resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(addressParts[0]));
					resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(addressParts[0]));
				}
				if (addressParts.length == 2){
					String zip = addressParts[1].replaceFirst("(?is).*?\\b([\\d-]+)\\s*$", "$1");
					String city = addressParts[1].replaceFirst(zip, "");
					city = city.replaceFirst("(?is)([^,]+),.*", "$1");
					
					resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), city);
					resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
				}
			}
			String owners = HtmlParser3.getValueFromAbsoluteCell(1, 0,  HtmlParser3.findNode(mainList, "OWNER INFORMATION"), "", true).trim();
			if (StringUtils.isNotEmpty(owners)){
				resultMap.put("tmpOwner", owners.trim());
			}
			
			String legal = HtmlParser3.getValueFromAbsoluteCell(1, 0,  HtmlParser3.findNode(mainList, "LEGAL DESCRIPTION"), "", true).trim();
			if (StringUtils.isNotEmpty(legal)){
				resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal.trim());
			}
			String impValue = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "IMP VALUE"), "", true).trim();
			if (StringUtils.isNotEmpty(impValue)){
				impValue = impValue.replaceAll("[\\$,]+", "");
				resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), impValue.trim());
			}
			
			String landValue = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "LAND VALUE"), "", true).trim();
			if (StringUtils.isNotEmpty(landValue)){
				landValue = landValue.replaceAll("[\\$,]+", "");
				resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), landValue.trim());
			}
			
			String totalValue = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "TOTAL VALUE"), "", true).trim();
			if (StringUtils.isNotEmpty(totalValue)){
				totalValue = totalValue.replaceAll("[\\$,]+", "");
				resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), totalValue.trim());
			}
			
			String saleDate = HtmlParser3.getValueFromAbsoluteCell(1, 1, HtmlParser3.findNode(mainList, "SALE INFORMATION"), "", true).trim();
			String saleDoc = HtmlParser3.getValueFromAbsoluteCell(2, 1, HtmlParser3.findNode(mainList, "SALE INFORMATION"), "", true).trim();
			String saleAmount = HtmlParser3.getValueFromAbsoluteCell(3, 1, HtmlParser3.findNode(mainList, "SALE INFORMATION"), "", true).trim();
			
			saleDoc = saleDoc.replaceAll("(?is)\\bDOC#,", "");
			saleDoc = saleDoc.replaceAll("(?is)\\bTD\\s+(\\d+)", "$1");
			String[] split;
			if (saleDoc.matches("\\d+/\\d+(,\\d+/\\d+)+")) {	//1795/1815,797/589 (Hays, account number R24894)
				split = saleDoc.split("[\\s+,]");
			} else {
				split = new String[1];
				split[0] = saleDoc;
			}
			
			@SuppressWarnings("rawtypes")
			List<List> body = new ArrayList<List>();
			
			for (String s: split) {
				String[] bp = s.split("(?is)[\\s+|,|/]");
				
				List<String> line = new ArrayList<String>();
				
				if (bp.length == 2 && bp[1].matches("(?is)\\d+")){
					if (saleDate.endsWith(bp[0])){
						line.add(org.apache.commons.lang.StringUtils.stripStart(bp[1], "0"));
						line.add("");
						line.add("");
						line.add(saleDate);
						line.add(saleAmount.replaceAll("[\\$,]+", ""));
					} else {
						line.add("");
						line.add(org.apache.commons.lang.StringUtils.stripStart(bp[0], "0").replaceFirst("\\*+$", ""));
						line.add(org.apache.commons.lang.StringUtils.stripStart(bp[1], "0"));
						line.add(saleDate);
						line.add(saleAmount.replaceAll("[\\$,]+", ""));
					}
					body.add(line);
				} else {
					s = s.replaceAll("[,\\-]", "").replaceAll("(?is)/?[A-Z]+", "").replaceAll("(?is)\\d+/\\d+/\\d+", "").trim();
					line.add(org.apache.commons.lang.StringUtils.stripStart(s, "0"));
					line.add("");
					line.add("");
					line.add(saleDate);
					line.add(saleAmount.replaceAll("[\\$,]+", ""));
					body.add(line);
				}
			}
			
			if (body != null && body.size() > 0) {
				ResultTable rt = new ResultTable();
				String[] header = {"InstrumentNumber", "Book", "Page", "InstrumentDate", "SalesPrice"};
				rt = GenericFunctions2.createResultTable(body, header);
				resultMap.put("SaleDataSet", rt);
			}
			
			NodeList tdList = mainList.extractAllNodesThatMatch(new TagNameFilter("td"), true).
									extractAllNodesThatMatch(new HasChildFilter(new StringFilter("Regular Bill"), true));
			
			StringBuffer baseAmount = new StringBuffer();
			if (tdList != null && tdList.size() > 0){
			
				String fromWhereToFind = HtmlParser3.getValueFromAbsoluteCell(2, 0, HtmlParser3.findNode(mainList, "CURRENT YEAR TAX INFORMATION"), "", true).trim();
				String taxYear = fromWhereToFind.replaceAll("(?is)[^\\(]*\\(([^\\)]+)\\)", "$1");
				if (StringUtils.isNotEmpty(taxYear) && taxYear.trim().matches("\\d{4}")){
					resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear.trim());
				}
				int offsetRow = 0;
				
				for (int j = 0; j < tdList.size(); j++) {
					String taxAmount = HtmlParser3.getValueFromAbsoluteCell(offsetRow, 2, HtmlParser3.findNode(mainList, fromWhereToFind), "", true).trim();
					if (StringUtils.isNotEmpty(taxAmount)){
						baseAmount.append(taxAmount.replaceAll("[\\$,]+", "").trim()).append("+");
					}
					offsetRow +=2;
				}				
				
				try {
					if (baseAmount.length() > 0) {
						resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), GenericFunctions.sum(baseAmount.toString(), searchId));
					}
				} catch (Exception e) {
					String county = mSearch.getSa().getAtribute(SearchAttributes.P_COUNTY_NAME);
					logger.error("Exception on parsing base amount on NTN for county-pin: " + county + "-" + pid + ":" + searchId, e);
				}
			}
			partyNames(resultMap, searchId);

			String acres = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "ACREAGE", true), "", true).trim();
			if(org.apache.commons.lang.StringUtils.isNotBlank(acres)) {
				try {
					Double.valueOf(acres);
					resultMap.put(PropertyIdentificationSetKey.ACRES.getKeyName(), acres);
				} catch (NumberFormatException nfe) {}
			}
			
			String absNo = HtmlParser3.getValueFromNextCell(HtmlParser3.findNode(mainList, "SUBDIVISION", true), "", true).trim();
			if(absNo.matches("[A-Z]*\\d+")) {
				resultMap.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNo.trim().replaceAll("^[A-Z]*0*", ""));
			}
			
			try {
				TXGenericFunctionsNTN.parseLegal(resultMap, mSearch);
			} catch (Exception e) {
				logger.error("Exception on parsing legal in TXGenericFunctionsNTN.parseLegal " + searchId, e);
			}
					
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	public static void partyNames(ResultMap resultMap, long searchId) throws Exception {
		
		String stringOwner = (String) resultMap.get("tmpOwner");
		
		if (StringUtils.isEmpty(stringOwner))
			return;		
		
		stringOwner = GenericFunctions2.cleanOwnerNameFromPrefix(stringOwner);
		stringOwner = GenericFunctions2.resolveOtherTypes(stringOwner);
		
		stringOwner = stringOwner.replaceAll("(?is)\\b(ET)\\s+(A)\\b", "ETAL");
		stringOwner = stringOwner.replaceAll("(?is)\\b(ET)\\s+(U)\\b", "ETUX");
		Matcher matcher = Pattern.compile("(?is)(.*?)\\s+([A-Z])\\s+([A-Z]+)(.*?)").matcher(stringOwner);
		while (matcher.find()) {
			String s2 = matcher.group(2);
			String s3 = matcher.group(3);
			if (s3.length()!=1  && !s3.matches("JR|SR|II|III|IV")) {
				stringOwner = matcher.group(1) + " " + s2 + " & " + s3 + matcher.group(4);
			}
		}
		stringOwner = stringOwner.replaceAll("(?is)\\b(JR|SR|II|III|IV)\\s+([A-Z]{2,})", "$1 & $2");
		stringOwner = stringOwner.replaceAll("(?is)\\s+([A-Z])\\s*/\\s*([A-Z]+)", " $1 & $2");
		stringOwner = stringOwner.replaceAll("(?is)\\b(CUSTODIAN|WF|WIFE|AKA)\\b", "&");
		stringOwner = stringOwner.replaceAll("(?is)\\b(ADDRESS\\s+)?UNKNOWN( AS)? PER [A-Z]\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\b((?:ADDRESS\\s+)?U|DECEASED)\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)\\b(P\\s*O\\s+BOX|SC\\s+#|VLB\\b).*", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bHCR\\b.*", "");//01170190001040 Maverick
		stringOwner = stringOwner.replaceAll("(?is)\\bD/B/A\\b", "<br>");
		stringOwner = stringOwner.replaceAll("(?is)\\bDO NOT HAVE A GOOD A\\b", "");
		stringOwner = stringOwner.replaceAll("(?is)[\\*\\\"]+", "");
		stringOwner = stringOwner.replaceAll("(?is)\\bAPT\\s+\\d+", "");
		stringOwner = stringOwner.replaceAll("(?is)\\d+", "");
		//ghertzo's
		stringOwner = stringOwner.replaceAll("(?is)\\bSIESTA VILLAGE.*", "");
		
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes, types, otherTypes;
		
		stringOwner = stringOwner.replaceAll("(?is)\\s*<br/?>\\s*$", "");
		
		String[] owners = stringOwner.split("(?is)\\s*<br/?>\\s*");
		if (stringOwner.contains("& CO-TRUSTEE")){//P650000000011700 Hidalgo :SMITH, ROBERT W & CO-TRUSTEE ALICE SMITH C
			owners = stringOwner.split("(?is)\\s*&\\s*");
		}
		
		//clean owners
		List<String> ownersList = new ArrayList<String>();
		
		for(String owner : owners){
			if(owner.split(" ").length == 1){
				boolean found = false;
				for(String own : ownersList){
					if(own.contains(owner)){
						found = true;
						break;
					}
				}
				if(!found){
					ownersList.add(owner);
				}
			} else {	
				if(!ownersList.contains(owner)){
					ownersList.add(owner);
				}
			}
		}
		
		for (String owner : ownersList) {
			names = StringFormats.parseNameNashville(owner, true);
			
			if (owner.contains("TRUSTEE")){
				owner = owner.replaceAll("(?is)\\A\\s*CO-(TRUSTEE)\\b(.*)", "$2 $1");
				names = StringFormats.parseNameDesotoRO(owner, true);
			}
			
			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
											NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
				
		GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), stringOwner.replaceAll("(?is)\\s*<br/?>\\s*", "/"));	
	}
	
	public static String prepareAddress(String address, Search search){
		
		int countyId = Integer.parseInt(search.getCountyId());
		String crtState = search.getSa().getAtribute(SearchAttributes.P_STATE_ABREV);
		String replaceString = "(?is)\\b(OFF?|NEAR|CORNER|BETWEEN|BTWN?|BYPASS)\\b.*";
		if ("TX".equals(crtState) && countyId==CountyConstants.TX_Travis) {
			replaceString = "(?is)\\b(OFF|NEAR|CORNER|BETWEEN|BTWN?|BYPASS)\\b.*";
		}
		
		address = address.replaceAll("(?is)\\A\\s*OFF?\\b", "");
		address = address.replaceAll(replaceString, "");
		address = address.replaceAll("(?is)\\b[\\d\\.]+\\s+MI(LES?)?\\b.*", "");
		address = address.replaceAll("(?is)\\b(REAR|-\\s*EASEMENT)\\s*$", "");
		address = address.replaceAll("(?is)\\bDEAD END\\b", "");
		address = address.replaceAll("(?is)\\b(LAND LOCKED|CROSS PROPERTY)\\b", "");
		address = address.replaceAll("(?is)\\b([NSWE]{1,2})(UNIT)\\b", "$1$2");
		
		address = address.replaceAll("(?is)\\([^\\)]*\\)", "");
		address = address.replaceAll("(?is)[\\[\\]]+", "");
		address = address.replaceAll("(?is)\\s*@.*", "");
		address = address.replaceAll("(?is)\\b/\\b.*", "");
		address = address.replaceAll("(?is)\\s*\\\"\\s*", " ");
		address = address.replaceAll("(?is)\\s*\\.\\s*", " ");
		address = address.replaceAll("(?is)\\b(county\\s+ro?a?d\\s+)unit", "$1 #");
		address = address.replaceAll("(?is)\\b(CR)(\\d+)", "$1 $2");
		address = address.replaceAll("(?is)\\bF\\s+M\\s+(\\d+)", "FM $1");
		address = address.replaceAll("(?is)\\bC\\s+R\\s+(\\d+)", "CR $1");
		
		return address.trim();
	}
	

}
		
