package ro.cst.tsearch.servers.types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.Tidy;

import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.TaxDocument;
import com.stewart.ats.base.document.TaxDocumentI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.Party;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PinI;
import com.stewart.ats.base.property.PinI.PinType;
import com.stewart.ats.base.property.Property;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.base.taxutils.Installment;
import com.stewart.ats.tsrindex.client.Receipt;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.SavedFromType;

@SuppressWarnings("deprecation")
public class OHFranklinTR2 extends TSServer {

	/**
	 * @author MihaiB
	 * 
	 */
	private static final long serialVersionUID = -1221988288078469076L;
	private static final Pattern NEXT_LINK = Pattern.compile("(?is)\\bhref=\\\"([^\\\"]+)[^>]*>Get next page\\b");
	private static final Pattern NEXT_LINK_ELSE = Pattern.compile("(?is)<td[^>]*>\\s*\\d+\\s*</td>\\s*<td[^>]*>\\s*<a.*?href=\\\"([^\\\"]+)");
	private static final Pattern TOWNSHIP_RANGE_PAT = Pattern.compile("(?is)\\bR(\\d+)\\s+T(\\d+)");

	public OHFranklinTR2(long searchId) {
		super(searchId);
	}

	public OHFranklinTR2(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;

		FilterResponse addressFilter = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
		
		PinFilterResponse  pinFilter = new PinFilterResponse(SearchAttributes.LD_PARCELNO, searchId){

			private static final long serialVersionUID = -5980159941802276097L;
			
			@Override
			protected Set<String> getRefPin(){
				if (parcelNumber != null){
					return parcelNumber;
				}
				Set<String> ret = new HashSet<String>();
				
				
				List<String> refPins = Arrays.asList(sa.getAtribute(saKey).trim().split(","));
				for (String refPin : refPins) {
					refPin = refPin.replaceAll("-", "");
					if (refPin.length() == 11){
						refPin = refPin.substring(0, 9);
					}
					ret.add(refPin);
				}
				return ret;
			}
			
			@Override
			protected Set<String> prepareCandPin(Set<String> candPins){
				Set<String> output = new HashSet<String>();
				for (String pin : candPins){
					pin = pin.trim().toLowerCase();
					pin = pin.replaceAll("-", "");
					if (pin.length() == 11){
						pin = pin.substring(0, 9);
					}
					output.add(pin);
				}
				return output;
			}
			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void computeScores(Vector rows){
//				Set<String> refPins = getRefPin();
				
				if (rows.size() > 1){
					boolean differentProperties = false;
					Set<String> pins = new HashSet<String>();
					try {
						for (int i = 0; i < rows.size(); i++) {
							ParsedResponse row = (ParsedResponse) rows.get(i);
							DocumentI doc = row.getDocument();
							Set<PropertyI> properties = doc.getProperties();
							for (PropertyI propertyI : properties) {
								String pid = propertyI.getPin(PinType.PID);
								pid = pid.replaceAll("-", "");
								if (pid.length() > 8) {
									pid = pid.substring(0, 9);
								}
								pins.add(pid);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (pins.size() > 1){
						differentProperties = true;
					} else{
						mSearch.setAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN, Boolean.TRUE);
					}
					
					if (differentProperties){
						for (int i = 0; i < rows.size(); i++) {
				            IndividualLogger.info("Processing result " + i + " of total " + rows.size(),searchId);
							ParsedResponse row = (ParsedResponse)rows.get(i);
							BigDecimal score = getScoreOneRow(row);
							scores.put(row.getResponse(), score);
							if (score.compareTo(bestScore) > 0){
								bestScore = score;
								IndividualLogger.info("ROW SCORE:" + score,searchId);
							} else if (score.compareTo(bestScore) == 0){
								super.computeScores(rows);
							}
						}
					} else {
						super.computeScores(rows);
					}
				} else {
					super.computeScores(rows);
				}
			}
		};
		pinFilter.setStartWith(true);
		pinFilter.setIgNoreZeroes(false);

		// search by parcel ID
		if (hasPin()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.addFilter(pinFilter);
			modules.add(module);
		}

		// search by address
		if (hasStreet()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
			module.addFilter(addressFilter);
			module.addFilter(pinFilter);
			module.addFilter(nameFilterHybrid);
			modules.add(module);
		}

		// search by owner
		if (hasOwner()) {
			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.OWNER_OBJECT);

			module.addFilter(nameFilterHybrid);
			module.addFilter(addressFilter);
			module.addFilter(pinFilter);
			
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
														.getConfigurableNameIterator(module, searchId, new String[] {"L;F;"});
			module.addIterator(nameIterator);

			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);
	}
	
	@Override
	protected int getResultType(){
		if(mSearch.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) == Boolean.TRUE ||
				(mSearch.getSa().getPins(-1).size() > 1 && (Search.AUTOMATIC_SEARCH == mSearch.getSearchType()))){
			return MULTIPLE_RESULT_TYPE; 
		} else {
			return UNIQUE_RESULT_TYPE;
		}
	}
	
	@Override
    public boolean anotherSearchForThisServer(ServerResponse sr) {
		
		//010-282381 --> 3 entries were found: 010-282381-80, 010-282381-90, 010-282381-99
		
		int pins =  mSearch.getSa().getPins(-1).size();
		if (pins > 0){
			Collection<String> refPins = mSearch.getSa().getPins(-1);
			Set<String> ret = new HashSet<String>();
			for (String refPin : refPins) {
				refPin = refPin.replaceAll("-", "");
				if (refPin.length() == 11){
					refPin = refPin.substring(0, 9);
				}
				ret.add(refPin);
			}
			DocumentsManagerI docManager = getSearch().getDocManager();
			try {
				docManager.getAccess();
				List<DocumentI> taxDocs = docManager.getDocumentsWithType(DType.TAX);
				if (taxDocs != null && taxDocs.size() > 0){
					int noOfDocs = 0;
					for (DocumentI doc : taxDocs) {
						Set<PropertyI> properties = ((TaxDocumentI) doc).getProperties();
						if (properties != null){
							for (PropertyI prop : properties) {
								String pin = prop.getPin().getPin(PinType.PID);
								if (StringUtils.isNotEmpty(pin)){
									pin = pin.replaceAll("-", "");
									if (pin.length() == 11){
										pin = pin.substring(0, 9);
									}
									for (String refPin : ret) {
										if (pin.equals(refPin)){
											noOfDocs++;
										}
									}
								}
							} 
						}
					}
					if (noOfDocs >= pins){
						return false;
					}
				}
			} finally {
				docManager.releaseAccess();
			}
		}
		return  true;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String result = Response.getResult().replaceAll("[^\\p{Alnum}\\p{Punct}\\s]+", "");
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		if (result.contains("No entries were found that match your search criteria")) {
			Response.getParsedResponse().setError("No Records Found.");
			return;
		} else if (result.contains("No entries were found that match your inputs")) {
			Response.getParsedResponse().setError("No Records Found.");
			return;
		} else if (result.contains("We are performing daily data updates")) {
			Response.getParsedResponse().setError("The official site are performing daily data updates");
			return;
		}
		
		if (viParseID == ID_DETAILS && result.contains("Displaying entries")){
			viParseID = ID_SEARCH_BY_NAME;
		}
		
		switch (viParseID) {
			case ID_SEARCH_BY_NAME:
			case ID_SEARCH_BY_ADDRESS:
				
				if (result.contains("Owner Information")){
					ParseResponse(sAction, Response, ID_DETAILS);
					return;
				}
				
				StringBuilder outputTable = new StringBuilder();
				Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, result, outputTable);

				if (smartParsedResponses.size() == 0) {
					return;
				}

				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());

				break;
								
			case ID_DETAILS:
			case ID_SAVE_TO_TSD:
				
				String details = getDetails(result, Response);
				
				String docNo = "";
				HtmlParser3 htmlparser3 = new HtmlParser3(details);
					
				docNo = htmlparser3.getValueFromAbsoluteCell(0, 1,  "Parcel ID").trim();
					
				Node imageLinkNode = htmlparser3.getNodeById("pdfImageLinkRow");
				String imageLink = new String();
				
				if (imageLinkNode != null) {
					String sFileLink = docNo + ".pdf";
					String dataSiteLink = getDataSite().getLink();
					LinkTag imageLinkTag = (LinkTag) imageLinkNode.getFirstChild();
					imageLink = imageLinkTag.getLink();
					
					ImageLinkInPage ilip = new ImageLinkInPage(imageLink.replaceFirst(".*Link=(.*)", "$1"), sFileLink);
					Response.getParsedResponse().addImageLink(ilip);
					
					imageLinkTag.setLink(CreatePartialLink(TSConnectionURL.idGET)
							+ dataSiteLink.substring(0, dataSiteLink.lastIndexOf("/")) + imageLink);
					
					details = details.replaceFirst("(?is)(<div\\s*id=\"pdfImageLinkRow\"[^>]*>).*?(</div>)", "$1" + imageLinkTag.toHtml() + "$2");
				}
				if (viParseID == ID_SAVE_TO_TSD){
					details = details.replaceFirst("(?is)(<div\\s*id=\"pdfImageLinkRow\"[^>]*>).*?(</div>)", "");// remove image link from docIndex
					
					smartParseDetails(Response, details);
					
	                msSaveToTSDFileName = docNo.trim() + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
	                
	            } else{
	            	
	            	String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;
					
					HashMap<String, String> data = new HashMap<String, String>();
					data.put("type", "CNTYTAX");
	    				
					if (isInstrumentSaved(docNo, null, data)){
	                	details += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, viParseID);
					}
					
					Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink,originalLink,TSServer.REQUEST_SAVE_TO_TSD));
					Response.getParsedResponse().setResponse(details);
		
	            }
				break;
			
			case ID_GET_LINK:
				ParseResponse(sAction, Response, sAction.contains("DisplaySelectionList") ? ID_SEARCH_BY_NAME : ID_DETAILS);
				break;
		}
	}
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String result, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		ParsedResponse parsedResponse = response.getParsedResponse();
	
		HtmlParser3 htmlparser = new HtmlParser3(result);
		NodeList nodeList = htmlparser.getNodeList();
		outputTable.append("<table BORDER='1' CELLPADDING='2'>");
		
		if (nodeList != null){
			NodeList nl = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true).extractAllNodesThatMatch(new HasAttributeFilter("id", "sumtab"), true);
			if (nl != null && nl.size() > 0){
				Div divResult = null;
				try {
					divResult = (Div) nl.elementAt(0);
				} catch (Exception e) {
					logger.error("Div with results not found on OHFranklinTR2: " + searchId + "\n" + e);
				}
				if (divResult != null){
					String navigation = "";
					nl = null;
					nl = divResult.getChildren().extractAllNodesThatMatch(new TagNameFilter("div"), true).extractAllNodesThatMatch(new HasAttributeFilter("id", "cards"));
					if (nl != null && nl.size() > 0){
						Div divNav = (Div) nl.elementAt(0);
						navigation = divNav.toHtml();
					}
					nl = null;
					nl = divResult.getChildren().extractAllNodesThatMatch(new TagNameFilter("table"));
					if (nl != null && nl.size() > 0){
						TableTag table = null;
						try {
							table = (TableTag) nl.elementAt(0);
						} catch (Exception e) {
							logger.error("Table with results not found on OHFranklinTR2: " + searchId + "\n" + e);
						}
						if (table != null){
							TableRow[] rows = table.getRows();
							if (rows != null){
								Date payDate = HashCountyToIndex.getPayDate(getCommunityId(), dataSite.getStateAbbreviation(), dataSite.getCountyName(), DType.TAX);
								Calendar cal = Calendar.getInstance();
								cal.setTime(payDate);
								StringBuffer header = new StringBuffer();

								String atsLink = CreatePartialLink(TSConnectionURL.idGET);
								for (TableRow row : rows){
									TableColumn[] columns = row.getColumns();
									if (columns.length == 1){
										if (!"head2b1".equals(row.getAttribute("class"))){//this is navigation row
											header.append(row.toHtml());
										}
									} else if (columns.length == 4){
										if (row.toPlainTextString().contains("Parcel")){
											header.append(row.toHtml());
										} else {
											String link = "", parcelNo = "";
											InstrumentI instr = new Instrument();
											try {
												LinkTag linkTag = (LinkTag) columns[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true)
														.elementAt(0);
												link = atsLink + linkTag.extractLink();
												if (response.getQuerry().contains("lastName") || response.getQuerry().contains("searchType=name")){
													link += "&searchType=name";
												} else if (response.getQuerry().contains("streetName") || response.getQuerry().contains("searchType=address")){
													link += "&searchType=address";
												}
												linkTag.setLink(link);
												parcelNo = linkTag.getLinkText();
												instr.setInstno(parcelNo);
											} catch (Exception e) {
												logger.error("Can't extract details link from intemediary on OHFranklinTR2: " + searchId + "\n" + e);
											}
											
											PropertyI property = Property.createEmptyProperty();
											
											String address = columns[1].getStringText().trim();
											AddressI adr = property.getAddress();
											adr.setFreeform(address);
											adr = Bridge.fillAddressI(adr, address);
											
											PinI pin = property.getPin(); 
							    			pin.addPin(PinI.PinType.PID, parcelNo);
							    			
//											property.setPin(pin);
//											property.setAddress(adr);
											
											String names = columns[2].getStringText().trim();
											PartyI partyNames = property.getOwner();
											partyNames = parseNames(names, false);
											property.setOwner(partyNames);
											
											instr.setDocType("COUNTYTAX");
								    		instr.setDocSubType("Countytax");
							    			instr.setYear(cal.get(Calendar.YEAR));
							    			
											TaxDocument taxDocument = new TaxDocument(DocumentsManager.generateDocumentUniqueId(searchId, instr), DType.TAX);
											taxDocument.setInstrument(instr);
											taxDocument.setServerDocType("COUNTYTAX");
											taxDocument.addProperty(property);
											
											taxDocument.setType(SimpleChapterUtils.DType.TAX);
											taxDocument.setDataSource(dataSite.getSiteTypeAbrev());
											taxDocument.setSiteId(getServerID());
											if (response.getQuerry().contains("lastName") || response.getQuerry().contains("searchType=name")){
												taxDocument.setSearchType(SearchType.GT);
											} else if (response.getQuerry().contains("streetName") || response.getQuerry().contains("searchType=address")){
												taxDocument.setSearchType(SearchType.AD);
											}
											
											if (isParentSite()){
												taxDocument.setSavedFrom(SavedFromType.PARENT_SITE);
											} else{
												taxDocument.setSavedFrom(SavedFromType.AUTOMATIC);
											}
											
											ParsedResponse currentResponse = new ParsedResponse();
											currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, row.toHtml());
											currentResponse.setOnlyResponse(row.toHtml());
											currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));
											currentResponse.setUseDocumentForSearchLogRow(true);
											currentResponse.setDocument(taxDocument);

											intermediaryResponse.add(currentResponse);
											
											outputTable.append(currentResponse.getResponse());
										}
									}
								}
								navigation = navigation.replaceAll("(?is)(href=\\\")(/DisplaySelectionList[^>]*>)<img.*?alt\\s*=\\s*\\\"([^\\\"]*)[^>]*", "$1" + atsLink + "$2$3");
								navigation = navigation.replaceAll("(?is)(href=\\\")(/DisplaySelectionList)", "$1" + atsLink + "$2");
								navigation = navigation.replaceAll("(?is)<img[^>]*>", "");
								if (response.getQuerry().contains("lastName")){
									navigation = navigation.replaceAll("(?is)(srchcurpage=\\d+)", "$1&searchType=name");
								} else if (response.getQuerry().contains("streetName")){
									navigation = navigation.replaceAll("(?is)(srchcurpage=\\d+)", "$1&searchType=address");
								}
										
								parsedResponse.setHeader(navigation + "<table id=\"intermediaryResults\" align=\"center\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" width=\"95%\">"
										+ header.toString());
								
								parsedResponse.setFooter("</table>" + navigation);
								
								if (Search.AUTOMATIC_SEARCH == getSearch().getSearchType()){
									Matcher mat = NEXT_LINK.matcher(navigation);
									if (mat.find()){
										parsedResponse.setNextLink("<a href=\"" + mat.group(1) + "\">Next</a>");
									} else{
										mat.reset();
										mat = NEXT_LINK_ELSE.matcher(navigation);
										if (mat.find()){
											parsedResponse.setNextLink("<a href=\"" + mat.group(1) + "\">Next</a>");
										}
									}
								}
							}
						}
					}
				}
			}
		}
		outputTable.append("</table>");
		
		return intermediaryResponse;
	}
	
	@Override
	public DocumentI smartParseDetails(ServerResponse response, String detailsHtml, boolean fillServerResponse){
		ParsedResponse parsedResponse = response.getParsedResponse();
		
		TaxDocumentI document = null;
		try {
			
			detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ").replaceAll("(?is)&amp;", "&");
			
			HtmlParser3 htmlparser = new HtmlParser3(detailsHtml);
			NodeList tableList = htmlparser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true);
			
			String parcelNo = htmlparser.getValueFromAbsoluteCell(0, 1,  "Parcel ID").trim();
			
			PropertyI property = Property.createEmptyProperty();
			
			PinI pin = property.getPin();
			pin.addPin(PinI.PinType.PID, parcelNo);
			
//			property.setPin(pin);
			
			String addressOnServer = htmlparser.getValueFromAbsoluteCell(0, 1,  "Location", true, false).trim();
			if (StringUtils.isNotEmpty(addressOnServer)){
				AddressI address = property.getAddress();
				addressOnServer = addressOnServer.replaceFirst("(?is)\\A\\s*0+\\s+", "");
				addressOnServer = addressOnServer.replaceFirst("(?is)(.*?)(#\\s*(?:PS)?\\d+\\b)(.*)", "$1$3 $2");
				addressOnServer = addressOnServer.replaceFirst("(^\\s*\\d+\\s*)-\\d+(.*)", "$1 $2");
				address = Bridge.fillAddressI(address, addressOnServer);
				
				address.setFreeform(addressOnServer);
				address.setState("Ohio");
				address.setCounty("Franklin");
				
//				property.setAddress(address);
			}
			
			String namesOnServer = htmlparser.getValueFromAbsoluteCell(0, 1,  "Owner", true, false).trim();
			if (StringUtils.isNotEmpty(namesOnServer)){
				namesOnServer = namesOnServer.replaceAll("(?is)\\s*&\\s*,\\s*([A-Z]+\\s+[A-Z])\\s*$", " & $1");
				namesOnServer = namesOnServer.replaceAll("(?is)\\s*,\\s*((?:\\w+\\s+)?(?:\\w+\\s+)?LLC)\\b", " $1");
				namesOnServer = namesOnServer.replaceAll("(?is)\\s*,\\s*(ET)\\s*(AL|UX|VIR)", " $1$2");
				namesOnServer = namesOnServer.replaceAll("(?is)\\s*,\\s*", "<br>");
				if (namesOnServer.indexOf("@") != -1) {
					namesOnServer = namesOnServer.replaceAll("(?:^|<br>)\\s*@\\s*(?:\\(\\s*\\d*\\s*\\))?\\s*(<br>|$)", "$1");
				}
				PartyI partyNames = property.getOwner();
				partyNames = parseNames(namesOnServer, true);
				property.setOwner(partyNames);
			}
			
			String legalOnServer = htmlparser.getValueFromAbsoluteCell(0, 1,  "Legal Description", true, false).trim();
			if (StringUtils.isNotEmpty(legalOnServer)){
				legalOnServer = legalOnServer.replaceAll("(?is)(\\b(\\d+\\s+)?[A-Z]+\\s+(?:AVE|BLVD|CT|CRK|DR|LOOP|PL|RD|ST|TER|TRL))\\s+", "$1\n").trim();
				legalOnServer = legalOnServer.replaceFirst("(?is)\\s*\\bALL\\s*\\d+(?:\\s*&)?", "");
				//161-000172
				if (!legalOnServer.contains(" LOT ")){
					if (legalOnServer.matches("(?is).*?\\b[\\d-]+\\s+BLK\\b.*")){
						legalOnServer = legalOnServer.replaceAll("(?is)(.*?)\\b([\\d-]+\\s+BLK\\b.*)", "$1 LOT $2").trim();
					}
				}
				legalOnServer = legalOnServer.replaceAll("(?is)(?:(?:\\r)?\\n)\\s*SUBDIVISION PLAT OF\\s*((?:\\r)?\\n)", "$1").trim();
				legalOnServer = legalOnServer.replaceAll("(?is)(?:(?:\\r)?\\n)\\s*RESUB PART OF\\s*((?:\\r)?\\n)", "$1").trim();
				//010-284574-80
				legalOnServer = legalOnServer.replaceAll("(?is)\\b(CONDOMINIUMS? AT)\\s*(?:(?:\\r)?\\n)", "$1 ").trim();
				
				legalOnServer = legalOnServer.replaceAll("(?is)(?:(?:\\r)?\\n)(\\s*[A-Z]{1,3}\\s*((?:\\r)?\\n))", " $1").trim();//010-120965
				legalOnServer = legalOnServer.replaceAll("(?is)\\b(LOTS?)(\\d+)", "$1 $2");
				legalOnServer = legalOnServer.replaceAll("(?is)((?:\\r)?\\n)(\\s*BLK)(\\s+LOT)", " BLOCK $1 $3").trim();//010-041239
				
				legalOnServer = legalOnServer.replaceAll("(?is)\\s*((?:\\r)?\\n)(\\s*CONDOMINIUM)\\b", " $2 $1").trim();//010-272564
				
				String[] legals = legalOnServer.split("(?is)((?:\\r)?\\n)");
				String lgl = "";
				LegalI legal = property.getLegal();
				SubdivisionI subdiv = legal.getSubdivision();
				TownShipI township = legal.getTownShip();
				boolean subdivNameFound = false;
				String streetName = property.getAddress().getStreetName();
				boolean streetLineRemoved = false;
				
				for (int l = 0; l < legals.length; l++){
					String legalLine = legals[l];
					if (StringUtils.isNotEmpty(streetName) 
							&& !streetLineRemoved 
							&& ((StringUtils.containsIgnoreCase(legalLine, streetName) 
									|| StringUtils.containsIgnoreCase(legalLine, GenericFunctions1.switchNumbers(streetName, false)))
									 && !legalLine.contains(" CONDO"))) {
						Matcher mat = TOWNSHIP_RANGE_PAT.matcher(legalLine);
						if (mat.find()){
							String range = mat.group(1);
							township.setRange(range);
							
							String twp = mat.group(2);
							township.setTownship(twp);
						}
						streetLineRemoved = true;
						continue;
					}
					lgl += legalLine + " ";
					
					legalLine = legalLine.replaceAll("(?is)[\\d\\.]+\\s*ACRES?", "").trim();
					legalLine = legalLine.replaceAll("(?is)\\bACRES?\\s*[\\d\\.]+", "").trim();
					legalLine = legalLine.replaceAll("(?is)[\\d-]+\\s*(TIF VALUE|ABATED VAL|TAXABLE VAL)", "").trim();
					legalLine = legalLine.replaceAll("(?is)\\bLOT\\s+RES\\s+[A-Z]\\b", "").trim();
					legalLine = legalLine.replaceAll("(?is)\\b(\\d+\\s+)?([A-Z]{1,2}\\s+)?[A-Z]+\\s+(AVE|BLVD|CT|CRK|DR|LOOP|PL|RD|ST|TER|TRL)\\b", "").trim();
					
					if (StringUtils.isEmpty(legalLine)){
						continue;
					}
					
					if (!subdivNameFound && (l == 0 || l == 1)){
						String subdivName = legalLine.trim();
						
						if (legals.length > l + 1){//610-207473
							if (legals[l + 1].contains(" SEC ") || legals[l + 1].contains(" SECTION ") || subdivName.endsWith(" AT")) {
								subdivName += " " + legals[l + 1];
							}
						}
						subdivName = subdivName.replaceFirst("(?is)\\b[A-Z]\\s*\\d+/\\d+\\b", "");
						
						subdivName = subdivName.replaceFirst("(?is)\\b(SEC)(\\d+)\\b", "$1 $2");
						subdivName = subdivName.replaceFirst("(?is)\\b(SEC(?:TION)?\\s+\\d+)\\s+(LOT)\\s+(\\d+)\\b", "$1");
						
						String section = LegalDescription.extractSectionFromText(subdivName);
						if (StringUtils.isNotEmpty(section)){
							subdiv.setSection(section);
						}
						
						if (subdivName.matches("(?is)R\\d+\\s+T\\d+.*")){
							Matcher mat = TOWNSHIP_RANGE_PAT.matcher(subdivName);
							if (mat.find()){
								String range = mat.group(1);
								township.setRange(range);
								
								String twp = mat.group(2);
								township.setTownship(twp);
							}
						} else{
							
							if ((subdivName.contains("LOT ") || subdivName.contains("BLK")) && legals.length > l + 1){
								subdivName = legals[l + 1];
							}
							if (legals.length > l + 1 && legals[l + 1].matches("^\\s*(EST(ATE)?S|H(EIGH|G)?TS)\\b.*")) {
								subdivName += " " + legals[l + 1];
							}
							if (subdivName.trim().matches("(?is)\\d+")){
								continue;
							}
							subdivName = subdivName.replaceFirst("(?is)\\b(SEC(TION)?|ENTRY)\\s*(\\d+)", "");
							subdivName = subdivName.replaceFirst("(?is)(?:\\s+\\d+-\\d+)\\s*$", "");
							subdivName = subdivName.replaceAll("(?is)\\s+(?:LOT|PH(?:ASE)?)(\\s+\\d+)?\\s*$", "");
							subdivName = subdivName.replaceFirst("(?i)\\s*[\\d\\.]+\\s*FT\\s*(\\w{1,2})\\s+PT\\b", "");//010-041239-00
							subdivName = subdivName.replaceFirst("(?i)\\s*\\d+\\s*FT\\s*(\\w{1,2}\\s+)*(\\d*\\s*&?\\s*)?", ""); // 9498 e.g. '16 FT S S 238 &'
							subdivName = subdivName.replaceAll("(?is)\\s+", " ");
							if (StringUtils.isNotBlank(subdivName)) {
								subdiv.setName(subdivName.trim());
								subdivNameFound = true;
							}
						}
					} else{
						if (legalLine.matches("(?is)R\\d+\\s+T\\d+.*")){
							Matcher mat = TOWNSHIP_RANGE_PAT.matcher(legalLine);
							if (mat.find()){
								String range = mat.group(1);
								township.setRange(range);
								
								String twp = mat.group(2);
								township.setTownship(twp);
							}
						} 
					}
				}
				
				if (StringUtils.isNotEmpty(lgl)){
					legal.setFreeForm(lgl);
					
					lgl = lgl.replaceAll("(?is)[\\d\\.]+\\s*ACRES?", "").trim();
					lgl = lgl.replaceAll("(?is)\\bACRES?\\s*[\\d\\.]+", "").trim();
					lgl = lgl.replaceAll("(?is)[\\d-]+\\s*(TIF VALUE|ABATED VAL|TAXABLE VAL)", "").trim();
					lgl = lgl.replaceAll("(?is)\\bLOT\\s+RES\\s+[A-Z]\\b", "").trim();
					
					lgl = lgl.replaceAll("(?is)\\A\\s+LOT", "x LOT");
					lgl = lgl.replaceAll("(?is)\\bBLOCK\\s+(LOT)", "$1");
					String lot = LegalDescription.extractLotFromText(lgl);
					if (StringUtils.isNotEmpty(lot)){
						subdiv.setLot(lot);
					}
					String block = LegalDescription.extractBlockFromText(lgl);
					if (StringUtils.isNotEmpty(block)){
						subdiv.setBlock(block);
					}
					String phase = LegalDescription.extractPhaseFromText(lgl);
					if (StringUtils.isNotEmpty(phase)){
						subdiv.setPhase(phase);
					}
					String tract = LegalDescription.extractTractFromText(lgl);
					if (StringUtils.isNotEmpty(tract)){
						subdiv.setTract(tract);
					}
					String unit = LegalDescription.extractUnitFromText(lgl);
					if (StringUtils.isNotEmpty(unit)){
						subdiv.setUnit(unit);
					}
					String bldg = LegalDescription.extractBuildingFromText(lgl);
					if (StringUtils.isNotEmpty(bldg)){
						subdiv.setBuilding(bldg);
					}
				}
			}
			
			InstrumentI instr = new Instrument();
			instr.setInstno(parcelNo);
			instr.setDocType("COUNTYTAX");
    		instr.setDocSubType("Countytax");
    		
    		String year = htmlparser.getValueFromAbsoluteCell(0, 0,  "Tax Year", false, false).trim();
    		if (StringUtils.isNotEmpty(year)){
    			year = year.replaceAll("(?i)[\\w+\\s]+(\\d{4})[\\w+\\s\\p{Punct}]?", "$1");
    			if (year.matches("(?is)\\d{4}")){
    				instr.setYear(Integer.parseInt(year));
    			}
    		}
			
    		document = new TaxDocument(DocumentsManager.generateDocumentUniqueId(searchId, instr), DType.TAX);
    		document.setInstrument(instr);
    		document.setServerDocType("COUNTYTAX");
			document.addProperty(property);
			
    		document.setType(SimpleChapterUtils.DType.TAX);
    		document.setDataSource(dataSite.getSiteTypeAbrev());
    		document.setSiteId(getServerID());
    		
    		if (isParentSite()){
    			document.setSavedFrom(SavedFromType.PARENT_SITE);
			} else{
				document.setSavedFrom(SavedFromType.AUTOMATIC);
			}
    		
    		String query = response.getQuerry();
    		if (StringUtils.isNotEmpty(query)){
	    		if (query.contains("searchType=name") || query.contains("searchByOwner")){
	    			document.setSearchType(SearchType.GT);
				} else if (query.contains("searchType=address") || query.contains("searchByAddress")){
					document.setSearchType(SearchType.AD);
				} else if (query.contains("searchByParcelId")){
					document.setSearchType(SearchType.PN);
				} else{
					document.setSearchType(SearchType.PN);
				}
    		} else {
				document.setSearchType(parsedResponse.getSearchType());
			}
    		
    		Date payDate = HashCountyToIndex.getPayDate(getCommunityId(), dataSite.getStateAbbreviation(), dataSite.getCountyName(), DType.TAX);
    		Date dueDate = HashCountyToIndex.getDueDate(getCommunityId(), dataSite.getStateAbbreviation(), dataSite.getCountyName(), DType.TAX);
    		int taxYearMode = HashCountyToIndex.getTaxYearMode(getCommunityId(), dataSite.getStateAbbreviation(), dataSite.getCountyName(), DType.TAX);
    		
    		Bridge.setPayAndDueDates(instr, (TaxDocument) document, payDate, dueDate, taxYearMode);
			document.setTaxYearMode(taxYearMode);
			
			String currentTaxYear = HtmlParser3.findNode(htmlparser.getNodeList(), "Tax Year").getText().trim();
			if (currentTaxYear.matches("(?is)Tax Year (\\d{2}|\\d{4})")) {
				currentTaxYear = currentTaxYear.replaceFirst("(?is)Tax Year (\\d+)", "$1");
			} 
			
			String baseAmount = htmlparser.getValueFromAbsoluteCell(1, 0,  "Annual Taxes", true, false).trim();
			double BA = 0.00;
			if (StringUtils.isNotEmpty(baseAmount)){
				baseAmount = baseAmount.replaceAll("(?i)[\\$,]+", "");
    			if (baseAmount.matches("(?is)[\\d\\.]+")){
    				try {
    					BA = Double.parseDouble(baseAmount);
    		    		document.setBaseAmount(BA);	
    		    	} catch (NumberFormatException e){}
    			}
    		}

			{
				StringBuffer remarks = new StringBuffer();
				boolean hasBOR = false, hasCAUV = false;;
				String cauvStatus = htmlparser.getValueFromAbsoluteCell(1, 4,  "Market", true, false).trim();
				if (StringUtils.isNotEmpty(cauvStatus) && cauvStatus.matches("(?is)[\\d,]+")){
					if (!"0".equals(cauvStatus)){
						hasCAUV = true;
					}
					remarks.append("CAUV: ").append(cauvStatus);
				}
				String borStatus = htmlparser.getValueFromAbsoluteCell(1, 0,  "Board of Revision", true, false).trim();
				if (StringUtils.isNotEmpty(borStatus) && borStatus.matches("(?is)[A-Z]+")){
					if (remarks.length() > 0){
						remarks.append("\n");
					}
					remarks.append("BOR: ").append(borStatus);
					if (borStatus.toLowerCase().contains("yes")){
						hasBOR = true;
					}
				}
				if (remarks.length() > 0){
					document.setNote(remarks.toString());
					
					// if you modify this, check also
					//com.stewart.ats.tsrindex.client.ChaptersTable.addDocTypePanel(SimpleChapter, int)
					//com.stewart.ats.tsrindex.client.ChaptersTable.applyColorForChapter(SimpleChapter, ChapterAditional, String)
					//ro.cst.tsearch.Search.applyColorForChapter(SimpleChapter, List<SimpleChapter>)
					if (hasBOR || hasCAUV){
						document.setTsrIndexColorClass("gwt-tax-bor-cauv-status");
					}
				}
			}
			
			String landValue = htmlparser.getValueFromAbsoluteCell(1, 1,  "Market", true, false).trim();
			if (StringUtils.isNotEmpty(landValue)){
				landValue = landValue.replaceAll("(?i)[\\$,]+", "");
    			if (landValue.matches("(?is)[\\d\\.]+")){
    				try {
    					document.setAppraisedValueLand(Double.parseDouble(landValue));
    		    	} catch (NumberFormatException e){}
    			}
			}
			String improvValue = htmlparser.getValueFromAbsoluteCell(1, 2,  "Market", true, false).trim();
			if (StringUtils.isNotEmpty(improvValue)){
				improvValue = improvValue.replaceAll("(?i)[\\$,]+", "");
    			if (improvValue.matches("(?is)[\\d\\.]+")){
    				try {
    					document.setAppraisedValueImprovements(Double.parseDouble(improvValue));
    		    	} catch (NumberFormatException e){}
    			}
			}
			String totalAssessedValue = htmlparser.getValueFromAbsoluteCell(1, 3,  "Market", true, false).trim();
			if (StringUtils.isNotEmpty(totalAssessedValue)){
				totalAssessedValue = totalAssessedValue.replaceAll("(?i)[\\$,]+", "");
    			if (totalAssessedValue.matches("(?is)[\\d\\.]+")){
    				try {
    					document.setAppraisedValueTotal(Double.parseDouble(totalAssessedValue));
    		    	} catch (NumberFormatException e){}
    			}
			}
			
			String transferHistoryTable = "", paymentHistoryTable = "", installmentTable = "", specialAssessmentTable = "";
			for (int i = tableList.size() - 1; i > 0; i--){
				String table = tableList.elementAt(i).toHtml();
				if (table.contains("Detail of Special Assessment") && StringUtils.isEmpty(specialAssessmentTable)){
					specialAssessmentTable = table.replaceFirst("(?is)<tr[^>]*>\\s*<td[^>]*>\\s*Detail of Special Assessment\\s*</td>\\s*</tr>", "");
				} else if (table.contains("Current Tax Year Detail") && StringUtils.isEmpty(installmentTable)){
					installmentTable = table.replaceFirst("(?is)<tr[^>]*>\\s*<td[^>]*>\\s*Current Tax Year Detail\\s*</td>\\s*</tr>", "");
				} else if (table.contains("Payment Information") && StringUtils.isEmpty(paymentHistoryTable)){
					paymentHistoryTable = table.replaceFirst("(?is)<tr[^>]*>\\s*<td[^>]*>\\s*Payment Information\\s*</td>\\s*</tr>", "");
				} else if (table.contains("Conveyance No") && StringUtils.isEmpty(transferHistoryTable)){
					transferHistoryTable = table;
					break;
				}
			}
			if (StringUtils.isNotEmpty(paymentHistoryTable)){
				List<List<String>> paymentHistory = HtmlParser3.getTableAsList(paymentHistoryTable, false);
				if (paymentHistory != null && paymentHistory.size() > 0){
					ArrayList<Receipt> receipts = new ArrayList<Receipt>();

					for (List<String> list : paymentHistory) {
						if (list.size() > 6){
							String firstHalf = list.get(4);
							String secHalf = list.get(5);
							String shortYear = list.get(1).trim();
							if (shortYear.matches("\\d\\s*-\\s*(\\d{2})")) {
								shortYear = shortYear.replaceFirst("\\d\\s*-\\s*(\\d{2})", "$1");
							}
							if (StringUtils.isNotEmpty(firstHalf) && StringUtils.isNotEmpty(secHalf)){
								firstHalf = firstHalf.replaceAll("[\\$,]+", "");
								secHalf = secHalf.replaceAll("[\\$,]+", "");
								String receiptAmt = GenericFunctions.sum(firstHalf + "+" + secHalf, searchId);
								if (StringUtils.isNotEmpty(receiptAmt)){
									Receipt rcpt = new Receipt();
									rcpt.setReceiptAmount(receiptAmt);
									rcpt.setReceiptDate(list.get(0).trim());

									receipts.add(rcpt);
									
									if (currentTaxYear.length() == 4 && shortYear.equals(currentTaxYear.substring(2))) {
										document.setDatePaid(list.get(0).trim());
									}
								}
							}
						}
					}
					if (receipts.size() != 0){
						document.setReceipts(receipts);
					}
				}
			}
			
			if (StringUtils.isNotEmpty(installmentTable)){
				installmentTable = installmentTable.replaceFirst("(?is)<tr[^>]*>\\s*<td[^>]*>[^<]*</td>\\s*<td[^>]*>\\s*Prior\\s*</td>\\s*<td[^>]*>\\s*1st half\\s*</td>\\s*<td[^>]*>\\s*2nd half\\s*</td>\\s*</tr>", "");
				
				List<List<String>> installmentList = HtmlParser3.getTableAsList(installmentTable, false);
				if (installmentList != null && installmentList.size() > 0){
					
					Installment installment1 = new Installment();
					Installment installment2 = new Installment();
					
					String saChg1 = "", saChg2 = "";
					String saPaid1 = "", saPaid2 = "";
					
					for (List<String> list : installmentList) {
						String label = list.get(0);
						if (StringUtils.isNotEmpty(label)){
							if ("Homestead CR".equalsIgnoreCase(label.trim())){
								String he1 = list.get(3);
								he1 = he1.replaceAll("[\\$,]+", "").trim();
								try {
									installment1.setHomesteadExemption((Double.parseDouble(he1)));
								} catch (NumberFormatException e) {
								}
								
								String he2 = list.get(5);
								he2 = he2.replaceAll("[\\$,]+", "").trim();
								try {
									installment2.setHomesteadExemption(Double.parseDouble(he2));
								} catch (NumberFormatException e) {
								}
							} else if ("Net".equalsIgnoreCase(label.trim())){
								String ba1 = list.get(3);
								ba1 = ba1.replaceAll("[\\$,]+", "").trim();
								try {
									installment1.setBaseAmount(Double.parseDouble(ba1));
								} catch (NumberFormatException e) {
								}
								
								String ba2 = list.get(5);
								ba2 = ba2.replaceAll("[\\$,]+", "").trim();
								try {
									installment2.setBaseAmount(Double.parseDouble(ba2));
								} catch (NumberFormatException e) {
								}
							} else if ("Penalty/Int".equalsIgnoreCase(label.trim())){
								String pen1 = list.get(3);
								pen1 = pen1.replaceAll("[\\$,]+", "").trim();
								if (StringUtils.isNotEmpty(pen1)){
									try {
										installment1.setPenaltyAmount(Double.parseDouble(pen1));
									} catch (NumberFormatException e) {
									}
								}
								
								String pen2 = list.get(5);
								pen2 = pen2.replaceAll("[\\$,]+", "").trim();
								if (StringUtils.isNotEmpty(pen2)){
									try {
										installment2.setPenaltyAmount(Double.parseDouble(pen2));
									} catch (NumberFormatException e) {
									}
								}
							} else if ("RE Paid".equalsIgnoreCase(label.trim())){
								String ap1 = list.get(3);
								ap1 = ap1.replaceAll("[\\$,]+", "").trim();
								if (StringUtils.isNotEmpty(ap1)){
									try {
										installment1.setAmountPaid(Double.parseDouble(ap1));
									} catch (NumberFormatException e) {
									}
									installment1.setStatus("PAID");
								}
								
								String ap2 = list.get(5);
								ap2 = ap2.replaceAll("[\\$,]+", "").trim();
								if (StringUtils.isNotEmpty(ap2)){
									try {
										installment2.setAmountPaid(Double.parseDouble(ap2));
									} catch (NumberFormatException e) {
									}
									installment2.setStatus("PAID");
								}
							} else if ("SA Chg".equalsIgnoreCase(label.trim())){
								saChg1 = list.get(3);
								saChg1 = saChg1.replaceAll("[\\$,]+", "").trim();
								
								saChg2 = list.get(5);
								saChg2 = saChg2.replaceAll("[\\$,]+", "").trim();
								
							} else if ("SA Paid".equalsIgnoreCase(label.trim())){
								saPaid1 = list.get(3);
								saPaid1 = saPaid1.replaceAll("[\\$,]+", "").trim();
								if (StringUtils.isEmpty(saPaid1)){
									saPaid1 = "0.00";
								}
								
								saPaid2 = list.get(5);
								saPaid2 = saPaid2.replaceAll("[\\$,]+", "").trim();
								if (StringUtils.isEmpty(saPaid2)){
									saPaid2 = "0.00";
								}
								
							} else if ("Balance Due".equalsIgnoreCase(label.trim())){
								double saDue1 = 0.00, saDue2 = 0.00;
								if (StringUtils.isNotEmpty(saChg1) && StringUtils.isNotEmpty(saPaid1)){
									String saDue = GenericFunctions.sum(saChg1 + "+-" + saPaid1, searchId);
									saDue1 = Double.parseDouble(saDue);
								}
								if (StringUtils.isNotEmpty(saChg2) && StringUtils.isNotEmpty(saPaid2)){
									String saDue = GenericFunctions.sum(saChg2 + "+-" + saPaid2, searchId);
									saDue2 = Double.parseDouble(saDue);
								}
								
								String priorDelinq = list.get(1).trim();
								Double prior = 0.00;
								if (StringUtils.isNotEmpty(priorDelinq)){
									priorDelinq = priorDelinq.replaceAll("[\\$,]+", "").trim();
									try {
										prior = Double.parseDouble(priorDelinq);
										document.setFoundDelinquent(prior);
									} catch (NumberFormatException e) {
									}
								}
								
								String ad1 = list.get(3);
								ad1 = ad1.replaceAll("[\\$,]+", "").trim();
								if (StringUtils.isNotEmpty(ad1)){
									try {
										Double secAd1FromTable = Double.parseDouble(ad1);
										secAd1FromTable = secAd1FromTable - saDue1;
										if (prior > 0.0){
											installment1.setAmountDue(secAd1FromTable - prior);
										} else{
											installment1.setAmountDue(secAd1FromTable);
										}
									} catch (NumberFormatException e) {
									}
									installment1.setStatus("UNPAID");
								}
								
								String ad2 = list.get(5);
								ad2 = ad2.replaceAll("[\\$,]+", "").trim();
								if (StringUtils.isNotEmpty(ad2)){
									try {
										Double secAd2FromTable = Double.parseDouble(ad2);
										secAd2FromTable = secAd2FromTable - saDue2;
										Double secAd1FromTable = 0.00;
										
										if (StringUtils.isNotEmpty(ad1)){
											secAd1FromTable = Double.parseDouble(ad1);
										}
										
										secAd2FromTable = secAd2FromTable - secAd1FromTable;

										installment2.setAmountDue(secAd2FromTable);
										
									} catch (NumberFormatException e) {
									}
									installment2.setStatus("UNPAID");
								}
							}
						}
					}
					ArrayList<Installment> installments = new ArrayList<Installment>();
					installments.add(installment1);
					installments.add(installment2);
					document.setInstallments(installments);
					
		    		try {
		    			document.setAmountPaid(installment1.getAmountPaid() + installment2.getAmountPaid());	
		    		 } catch (NumberFormatException e){}

					try {
						document.setAmountDue(installment1.getAmountDue() + installment2.getAmountDue());	
				    } catch (NumberFormatException e){}
				}
			}
			
			if (StringUtils.isNotEmpty(specialAssessmentTable)){
				specialAssessmentTable = specialAssessmentTable.replaceFirst("(?is)<tr[^>]*>\\s*<td[^>]*>[^<]*</td>\\s*<td[^>]*>\\s*Prior\\s*</td>\\s*<td[^>]*>\\s*1st half\\s*</td>\\s*<td[^>]*>\\s*2nd half\\s*</td>\\s*</tr>", "");
				
				List<List<String>> saList = HtmlParser3.getTableAsList(specialAssessmentTable, false);
				if (saList != null && saList.size() > 0 && !(saList.size()==1&&saList.get(0).size()==1&&"No Records Found".equalsIgnoreCase(saList.get(0).get(0).trim()))){
					
					String saBA1 = "", saBA2 = "";
					String saPenInt1 = "", saPenInt2 = "";
					String saAD1 = "", saAD2 = "";
					String saAP1 = "", saAP2 = "";
					
					for (List<String> list : saList){
						String label = list.get(0).trim();
						label = label.replaceAll("(?is)\\s*/\\s*", "/");
						if (StringUtils.isNotEmpty(label)){
							if ("Charge".equalsIgnoreCase(label)){
								String ba1 = list.get(3);
								ba1 = ba1.replaceAll("[\\$,]+", "").trim();
								saBA1 += ba1 + "+";
								
								String ba2 = list.get(5);
								ba2 = ba2.replaceAll("[\\$,]+", "").trim();
								saBA2 += ba2 + "+";
							} else if ("Penalty/Int".equalsIgnoreCase(label)){
								String pi1 = list.get(3);
								pi1 = pi1.replaceAll("[\\$,]+", "").trim();
								saPenInt1 += pi1 + "+";
									
								String pi2 = list.get(5);
								pi2 = pi2.replaceAll("[\\$,]+", "").trim();
								saPenInt2 += pi2 + "+";
							} else if ("Paid".equalsIgnoreCase(label)){
								String ap1 = list.get(3);
								ap1 = ap1.replaceAll("[\\$,]+", "").trim();
								saAP1 += ap1 + "+";
								
								String ap2 = list.get(5);
								ap2 = ap2.replaceAll("[\\$,]+", "").trim();
								saAP2 += ap2 + "+";
							} else if ("Owed".equalsIgnoreCase(label)){
								String ad1 = list.get(3);
								ad1 = ad1.replaceAll("[\\$,]+", "").trim();
								saAD1 += ad1 + "+";
								
								String ad2 = list.get(5);
								ad2 = ad2.replaceAll("[\\$,]+", "").trim();
								saAD2 += ad2 + "+";
							}
						}
					}
					Installment installment1 = new Installment();
					if (StringUtils.isNotEmpty(saBA1)){
						try {
							saBA1 = GenericFunctions.sum(saBA1, searchId);
							installment1.setBaseAmount(Double.parseDouble(saBA1));
						} catch (Exception e) { }
					}
					if (StringUtils.isNotEmpty(saAP1)){
						try {
							saAP1 = GenericFunctions.sum(saAP1, searchId);
							installment1.setAmountPaid(Double.parseDouble(saAP1));
						} catch (Exception e) { }
					}
					if (StringUtils.isNotEmpty(saAD1)){
						try {
							saAD1 = GenericFunctions.sum(saAD1, searchId);
							installment1.setAmountDue(Double.parseDouble(saAD1));
						} catch (Exception e) { }
					}
					if (StringUtils.isNotEmpty(saPenInt1)){
						try {
							saPenInt1 = GenericFunctions.sum(saPenInt1, searchId);
							installment1.setPenaltyAmount(Double.parseDouble(saPenInt1));
						} catch (Exception e) { }
					}
					if (installment1.getAmountDue() > 0){
						installment1.setStatus("UNPAID");
					} else{
						installment1.setStatus("PAID");
					}
					
					Installment installment2 = new Installment();
					if (StringUtils.isNotEmpty(saBA2)){
						try {
							saBA2 = GenericFunctions.sum(saBA2, searchId);
							installment2.setBaseAmount(Double.parseDouble(saBA2));
						} catch (Exception e) { }
					}
					if (StringUtils.isNotEmpty(saAP2)){
						try {
							saAP2 = GenericFunctions.sum(saAP2, searchId);
							installment2.setAmountPaid(Double.parseDouble(saAP2));
						} catch (Exception e) { }
					}
					if (StringUtils.isNotEmpty(saAD2)){
						try {
							saAD2 = GenericFunctions.sum(saAD2, searchId);
							installment2.setAmountDue(Double.parseDouble(saAD2));
						} catch (Exception e) { }
					}
					if (StringUtils.isNotEmpty(saPenInt2)){
						try {
							saPenInt2 = GenericFunctions.sum(saPenInt2, searchId);
							installment2.setPenaltyAmount(Double.parseDouble(saPenInt2));
						} catch (Exception e) { }
					}
					if (installment2.getAmountDue() > 0){
						installment2.setStatus("UNPAID");
					} else{
						installment2.setStatus("PAID");
					}
					
					ArrayList<Installment> specialAssessments = new ArrayList<Installment>();
					specialAssessments.add(installment1);
					specialAssessments.add(installment2);
					document.setSpecialAssessmentInstallments(specialAssessments);
				}
					
			}
			
//			if (StringUtils.isNotEmpty(transferHistoryTable)){
//				List<List<String>> transfHistory = HtmlParser3.getTableAsList(transferHistoryTable, false);
//				if (transfHistory != null && transfHistory.size() > 0){
//					Vector<SaleDataSet> sdsVector = new Vector<SaleDataSet>();
//					
//					SaleDataSet sds = null;
//					for (List<String> list : transfHistory) {
//						if (list.size() > 6){
//							String instrumentNumber;
//							if (StringUtils.isNotEmpty(list.get(2).trim())){
//								instrumentNumber = list.get(2).trim();
//							} else if (StringUtils.isNotEmpty(list.get(3).trim())){
//								instrumentNumber = list.get(3).trim();
//							} else{
//								continue;
//							}
//							
//							sds = new SaleDataSet();
//							
//							String instrumentDate = list.get(0).trim();
//							sds.setAtribute(SaleDataSetKey.INSTRUMENT_DATE.getShortKeyName(), instrumentDate);
//							sds.setAtribute(SaleDataSetKey.GRANTOR.getShortKeyName(), list.get(1).trim());
//							
//							if (instrumentNumber.contains("PB") && instrumentNumber.contains("PG")){
//								sds.setAtribute(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), "");
//								Pattern pat = Pattern.compile("(?is)PB\\s*(\\d+)\\s+PG\\s*(\\d+)");
//								Matcher mat = pat.matcher(instrumentNumber);
//								if (mat.find()){
//									String pb = mat.group(1);
//									pb = StringUtils.stripStart(pb, "0");
//									String pp = mat.group(2);
//									pp = StringUtils.stripStart(pp, "0");
//									
//									sds.setAtribute(SaleDataSetKey.BOOK.getShortKeyName(), pb);
//									sds.setAtribute(SaleDataSetKey.PAGE.getShortKeyName(), pp);
//									
//									property.getLegal().getSubdivision().setPlatBook(pb);
//									property.getLegal().getSubdivision().setPlatPage(pp);
//								}
//							} else{
//								if (StringUtils.isNotEmpty(instrumentDate) && instrumentDate.matches("(?is)\\d{2}\\s*/\\s*\\d{2}\\s*/\\s*\\d{4}")){
//									if (instrumentNumber.length() > 7){
//										
//										instrumentNumber = instrumentNumber.replaceFirst("(?is)(\\d+)-[A-Z]", "$1");//1998913382-D on pid:010-000395
//										instrumentNumber = instrumentNumber.replaceFirst("(?is)-\\s*$", "");//090-004534
//										instrumentNumber = instrumentNumber.substring(4);
//										instrumentNumber = StringUtils.stripStart(instrumentNumber, "0");
//										instrumentNumber = instrumentDate.replaceFirst("(?is)(\\d{2})\\s*/\\s*(\\d{2})\\s*/\\s*(\\d{4})", "$3$1$2") 
//																		+ StringUtils.leftPad(instrumentNumber, 7, "0");
//									}
//								}
//								sds.setAtribute(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), instrumentNumber);
//								sds.setAtribute(SaleDataSetKey.BOOK.getShortKeyName(), "");
//								sds.setAtribute(SaleDataSetKey.PAGE.getShortKeyName(), "");
//							}
//							
//							sds.setAtribute(SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(), list.get(4).trim());
//							sds.setAtribute(SaleDataSetKey.SALES_PRICE.getShortKeyName(), list.get(6).replaceAll("(?is)\\$", "").trim());
//							sdsVector.add(sds);
//						}
//					}
//					if (!sdsVector.isEmpty()){
//						try{
//				    		Set<RegisterDocumentI> trans = Bridge.getTransactioHistory(parsedResponse, sdsVector, searchId);
//				    		if (trans != null){
//				    			for (RegisterDocumentI c : trans){
//				    				document.addParsedReference(c.getInstrument());
//				    			}
//				    		}
//				    	}catch(Exception e){
//				    		e.printStackTrace();
//				    	}
//					}
//				}
//			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (fillServerResponse) {
			response.getParsedResponse().setResponse(detailsHtml);
			if (document != null) {
				parsedResponse.setDocument(document);
				parsedResponse.setUseDocumentForSearchLogRow(true);
			}
		}
		parsedResponse.setSearchId(this.searchId);
		
		document.setChecked(true);
		document.setIncludeImage(true);
		document.setManualChecked(false);
		document.setManualIncludeImage(false);
		
		boolean isParentSite = false;
		 
		if (parsedResponse instanceof ParsedResponse) {
			isParentSite = ((ParsedResponse)parsedResponse).isParentSite();
			((ParsedResponse)parsedResponse).setDocument(document);
			if (isParentSite) {
				document.setSavedFrom(SavedFromType.PARENT_SITE);
			} else {
				document.setSavedFrom(SavedFromType.AUTOMATIC);
			}
		}
		
		return document;
	}
	
	protected String getDetails(String response, ServerResponse Response){
		
		// if from memory - use it as is
		if (!response.toLowerCase().contains("<html")){
			return response;
		}
		
		response = Tidy.tidyParse(response, null);
		StringBuffer detailsBuff = new StringBuffer();
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(response, null);
			NodeList mainList = htmlParser.parse(null);
			NodeList aList = null;
			
			if (mainList != null && mainList.size() > 0){
				NodeList tableList = mainList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
				if (tableList != null && tableList.size() > 0){

					for (int i = tableList.size() - 1; i > 0; i--) {
						String tabel = tableList.elementAt(i).toHtml();
						if (tabel.contains(" class=\"head1\"")){
							tabel = tabel.replaceFirst("(?is)\\bclass=\\\"head1\\\"", "style=\"text-align:center;font-size:16pt; font-weight:bold\"");
							detailsBuff.append(tabel);
							break;
						}
					}
				}
				NodeList divList = mainList.extractAllNodesThatMatch(new TagNameFilter("div"), true).extractAllNodesThatMatch(new HasAttributeFilter("id", "detailmenu"), true);
				if (divList != null && divList.size() > 0){

					aList = divList.extractAllNodesThatMatch(new TagNameFilter("a"), true);
				}
			}

			if (aList != null && aList.size() > 0){
				HttpSite3 site = HttpManager3.getSite(getCurrentServerName(), searchId);
				if (site != null) {
					try {
						for (int i = 0; i < aList.size(); i++){
							LinkTag linkTag = (LinkTag) aList.elementAt(i);
							if (linkTag != null){
								String label = linkTag.getLinkText().trim();
								if (label.equalsIgnoreCase("Property Profile") || label.equalsIgnoreCase("Land") || label.equalsIgnoreCase("Building")
										|| label.equalsIgnoreCase("Improvements") || label.equalsIgnoreCase("Transfer History") || label.equalsIgnoreCase("BOR Status")
										|| label.equalsIgnoreCase("CAUV Status") || label.contains("Payment Info") || label.equalsIgnoreCase("Current Levy Info")
										|| label.equalsIgnoreCase("Assessment Payoff")){
										
									String link = linkTag.getLink();
									if (label.equalsIgnoreCase("CAUV Status") && link.indexOf("extURL=") > 0){
										link = link.substring(link.indexOf("extURL=") + 7);
									}
										
									String rsp = ((ro.cst.tsearch.connection.http3.OHFranklinTR2) site).getPage(link, null);
									rsp = Tidy.tidyParse(rsp, null);
									org.htmlparser.Parser htmlParseoru = org.htmlparser.Parser.createParser(rsp, null);
									NodeList nodeList = htmlParseoru.parse(null);
										
									if (label.equalsIgnoreCase("BOR Status")){
										NodeList divList = nodeList.extractAllNodesThatMatch(new TagNameFilter("div"), true);
										if (divList != null && divList.size() > 0){
											detailsBuff.append("<br><br>");
											detailsBuff.append(divList.extractAllNodesThatMatch(new HasAttributeFilter("id", "Main_Header")).toHtml());
											detailsBuff.append(divList.extractAllNodesThatMatch(new HasAttributeFilter("id", "main_area")).toHtml());
											detailsBuff.append(divList.extractAllNodesThatMatch(new HasAttributeFilter("id", "date")).toHtml());
										}
									} else if (label.equalsIgnoreCase("CAUV Status")){
										NodeList formList = nodeList.extractAllNodesThatMatch(new TagNameFilter("form"), true);
										if (formList != null && formList.size() > 0){
											detailsBuff.append("<br><br>");
											String info = formList.extractAllNodesThatMatch(new HasAttributeFilter("id", "form1")).toHtml();
											info = info.replaceAll("(?is)<input[^>]*>", "");
											
											detailsBuff.append("<div style=\"text-align:center;font-size:16pt; font-weight:bold\">" + label + "</div></center><br><br>" + info);
										}
									} else if (label.equalsIgnoreCase("Assessment Payoff")) {
										Node tableNode = HtmlParser3.getNodeByID("sumtab", nodeList, true);
										if (tableNode != null) {
											detailsBuff.append("<br><br><div style=\"text-align:center;font-size:16pt; font-weight:bold\">"
													+ label + "</div></center><br><br>" + tableNode.toHtml() + "<br>");
										}
									} else if (rsp.contains("Parcel ID")){
										NodeList tableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true);
										if (tableList != null && tableList.size() > 0){
											for (int j = tableList.size() - 1; j > 0; j--) {
												String tabel = tableList.elementAt(j).toHtml();
												if (tabel.contains(" id=\"sumtab\"")){
													
													tabel = tabel.replaceAll("(?i)<img.*?greenchk\\.gif[^>]*>", "Check");
													
													if (tabel.contains(" id=\"tabsF1\"") && label.equalsIgnoreCase("Building")){
														HtmlParser3 tabelParser = new HtmlParser3(tabel);
														NodeList nl = tabelParser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("div"), true)
																		.extractAllNodesThatMatch(new HasAttributeFilter("id", "tabsF1"), true);
														if (nl != null && nl.size() > 0){
															
															String div = nl.toHtml();
															Pattern pat = Pattern.compile("(?is)<a.*? href=\\\"([^\\\"]*)\\\"[^>]*>\\s*(?:<span[^>]*>)?([^<]+)(</span></a>)");
															Matcher mat = pat.matcher(div);
															while (mat.find()){
																String href = mat.group(1);
																String tab = mat.group(2);
																if ("#".equals(href)){
																	tabel = tabel.replaceFirst("(?is)<div.*? id=\\\"tabsF1\\\">.*?</div>", "<b>" + tab + "</b>");
																} else if (href.contains("selectDisplay")){
																	if (link.indexOf(".com") > 0){
																		String host = link.substring(0, link.indexOf(".com") + 4);
																		String newPage = ((ro.cst.tsearch.connection.http3.OHFranklinTR2) site).getPage(host + href, null);
																		if (tabel.contains(" id=\"sumtab\"")){
																			tabelParser = new HtmlParser3(newPage);
																			nl = tabelParser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("div"), true)
																					.extractAllNodesThatMatch(new HasAttributeFilter("id", "sumtab"), true);
																			if (nl != null && nl.size() > 0){
																				String permitsDiv = nl.toHtml();
																				tabel = tabel.replaceFirst("(?is)</table>\\s*$", 
																						"<tr><td></td></tr><tr><td colspan=\"4\"><b>" + tab + "</b></td></tr>" 
																						+ "<tr><td></td></tr><tr><td colspan=\"4\">" 
																						+ Matcher.quoteReplacement(permitsDiv) + "</td></tr></table>");
																			}
																		}
																	}
																}
															}
															
														}
														
													}
													detailsBuff.append("<br><br><center><div style=\"text-align:center;font-size:16pt; font-weight:bold\">" + label + "</div></center><br><br>" + tabel);

													break;
												}
											}
										}
									}
								}
							}
						}
					} finally {
						// always release the HttpSite
						HttpManager3.releaseSite(site);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		String details = detailsBuff.toString();
		details = details.replaceAll("(?is)<a[^>]*>([^<]*)</a>", "$1");

		Pattern imageLinkPattern = Pattern.compile("(?is)<a href=\"([^\"]*)\"[^>]*>\\s*<img[^>]*View\\s+Prior\\s+History[^>]*>\\s*</a>");
		Matcher imageLinkMatcher = imageLinkPattern.matcher(details);
		String imageLink = new String();
		if (imageLinkMatcher.find()) {
			imageLink = imageLinkMatcher.group(1);
			details += "<div id=\"pdfImageLinkRow\" align=\"center\"><a target=\"_blank\" href=\""
					+ imageLink + "\" ><strong>View Image</strong></a></div>";
		}
		details = details.replaceAll("(?is)<img[^>]*>", "");
		details = details.replaceAll("(?is)</?body[^>]*>", "");
		details = details.replaceAll("(?is)\\|", "");
		details = details.replaceAll("(?i)>.*?click[^<]*", ">");
		
		details = details.replaceAll("(?i)<table ", "<table border=\"1\" ");
		
		return details;
	}

	public Party parseNames(String nameOnServer, boolean isDetails){
		String unparsedName = nameOnServer.trim();
		
		Party party = new Party(PType.GRANTOR);
		
		String[] names = unparsedName.split("(?is)\\s*<\\s*br\\s*/?\\s*>\\s*");
		if (names.length == 2){
			if (names[0].matches("(?is)[A-Z]+\\s+[A-Z]+(\\s+[A-Z](\\s+TR)?(\\s+[S|J]R)?)?")
					&& names[1].matches("(?is)[A-Z]+\\s+[A-Z]+(\\s+[A-Z](\\s+TR)?(\\s+[S|J]R)?)?")){
				//
			} else{
				if (!isDetails){
					unparsedName = names[0] + " " + names[1];
					names = unparsedName.split("(?is)\\s*<\\s*br\\s*/?\\s*>\\s*");
				}
			}
		}
		party.setFreeParsedForm(unparsedName);
		
		for (String nume : names){
			String[] nam = StringFormats.parseNameNashville(nume, true);
			
			if (nume.trim().startsWith("&")){//for details
				nume = nume.replaceFirst("(?is)\\A&\\s*", "");
				nam = StringFormats.parseNameDesotoRO(nume, true);
			}
			
			String[] type = GenericFunctions.extractAllNamesType(nam);
			String[] otherType = GenericFunctions.extractAllNamesOtherType(nam);
			String[] suffixes = GenericFunctions.extractNameSuffixes(nam);
			
			NameI atsName = null;
			
			if (StringUtils.isNotEmpty(nam[2])) {
				atsName = new Name();

				if (NameUtils.isCompany(nam[2])) {
					atsName.setLastName(nam[2]);
					atsName.setCompany(true);
					atsName.setNameOtherType(otherType[0]);
				} else {
					atsName.setFirstName(nam[0]);
					atsName.setMiddleName(nam[1]);
					atsName.setLastName(nam[2]);
	
					atsName.setSufix(suffixes[0]);
					atsName.setNameType(type[0]);
					atsName.setNameOtherType(otherType[0]);
	
				}
				if (atsName != null) {
					party.add(atsName);
				}
			}
			
			if (StringUtils.isNotEmpty(nam[5])) {
				atsName = new Name();

				if (NameUtils.isCompany(nam[5])) {
					atsName.setLastName(nam[5]);
					atsName.setCompany(true);
					atsName.setNameOtherType(otherType[1]);
				} else {
					atsName.setFirstName(nam[3]);
					atsName.setMiddleName(nam[4]);
					atsName.setLastName(nam[5]);

					atsName.setSufix(suffixes[1]);
					atsName.setNameType(type[1]);
					atsName.setNameOtherType(otherType[1]);
				}
				if (atsName != null) {
					party.add(atsName);
				}
			}
			
		}
		return party;
	}
}
