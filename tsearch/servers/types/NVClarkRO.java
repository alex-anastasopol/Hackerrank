package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.module.OcrFakeIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.datatree.DataTreeAccount;
import com.stewart.datatree.DataTreeConn;
import com.stewart.datatree.DataTreeImageException;
import com.stewart.datatree.DataTreeManager;
import com.stewart.datatree.DataTreeStruct;

/**
 * @author costi
 */
@SuppressWarnings("deprecation")
public class NVClarkRO extends TSServer implements TSServerROLikeI {
	transient protected List<DataTreeStruct> datTreeList;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NVClarkRO(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public NVClarkRO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
		datTreeList = initDataTreeStruct();
	}

	protected List<DataTreeStruct> initDataTreeStruct() {
		DataSite dat = HashCountyToIndex.getDateSiteForMIServerID(
				InstanceManager.getManager().getCommunityId(searchId),
				miServerID);
		return DataTreeManager.getProfileDataUsingStateAndCountyFips(
				dat.getCountyFIPS(), dat.getStateFIPS());
	}

	@Override
	protected void ParseResponse(String sAction, ServerResponse response,
			int viParseID) throws ServerResponseException {
		String rsResponse = response.getResult();
		ParsedResponse parsedResponse = response.getParsedResponse();

		// Check if an error occurred.
		if (rsResponse
				.contains("Search returned more than 10,000 records, please narrow your search.")) {
			parsedResponse
					.setError("<font color=\"red\">Official Site Says: </font>"
							+ "Search returned more than 10,000 records, please narrow your search.");
			response.setError(parsedResponse.getError());
			response.setResult("");
			return;
		}
		if (rsResponse.contains("Please refine your search")) {
			parsedResponse
					.setError("<font color=\"red\">Official Site Says: </font>"
							+ "Please refine your search by selecting a smaller range between the Start Date and End Date.");
			response.setError(parsedResponse.getError());
			response.setResult("");
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_PARCEL: {
			// Search by PIN.
			StringBuilder outputTable = new StringBuilder();

			Collection<ParsedResponse> smartParsedResponses = null;
			try {
				// Parse intermediary responses.
				smartParsedResponses = smartParseIntermediary(response,
						rsResponse, outputTable, this);
			} catch (Exception e) {
				logger.error(e);
			}
			if (smartParsedResponses != null && smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(
						smartParsedResponses));
				parsedResponse.setOnlyResponse(rsResponse);

				if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
					String header = CreateSaveToTSDFormHeader(
							URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION,
							"POST");
					header += parsedResponse.getHeader();

					parsedResponse.setHeader(header);

					Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);
					if (numberOfUnsavedDocument != null
							&& numberOfUnsavedDocument instanceof Integer) {
						parsedResponse.setFooter(parsedResponse.getFooter()
								+ CreateSaveToTSDFormEnd(
										SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID,
										(Integer) numberOfUnsavedDocument));
					} else {
						parsedResponse.setFooter(parsedResponse.getFooter()
								+ CreateSaveToTSDFormEnd(
										SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID,
										-1));
					}

				}
				if (viParseID == ID_SEARCH_BY_INSTRUMENT_NO) {
					response.getParsedResponse().setNextLink("");
				}

			} else {
				// No intermediary response found.
				parsedResponse
						.setError("<font color=\"red\">No results found.</font> Please try again.");
				return;
			}
		}
			break;
		case ID_DETAILS:
		case ID_SAVE_TO_TSD:
			// Parse the document response.
			searchId = getSearch().getID();
			HashMap<String, String> contentsArray = ro.cst.tsearch.servers.functions.NVClarkRO
					.getDetailedContent(rsResponse);
			if (contentsArray == null || contentsArray.size() == 0
					|| StringUtils.isEmpty(contentsArray.get("html"))) {
				parsedResponse
						.setError("<font color=\"red\">Could not parse document.</font>  Please search again.");
				return;
			}
			String contents = contentsArray.get("html");
			parsedResponse.setResponse(contents);
			String keyCode = contentsArray.get("instrNr");
			
			// Add image link
			String book = "",
			page = "",
			year = "",
			month = "",
			day = "",
			dataTreeIndex = "",
			dataTreeDesc = "",
			instr = "";
			String[] dtImgInfo = new String[] { "", "" };
			keyCode = keyCode.replaceAll("[-\\s]", "");
			Matcher m = Pattern.compile("(?is)(\\d{4})(\\d{2})(\\d{2})(\\d+)")
					.matcher(keyCode);
			if (m.find()) {
				year = m.group(1);
				month = m.group(2);
				day = m.group(3);
				// Remove leading zeroes.
				instr = m.group(4).replaceFirst("^0+(?!$)", "");
			}
			
			String link = CreatePartialLink(TSConnectionURL.idGET)
					+ "look_for_dt_image&id=" + dtImgInfo[0] + "&description="
					+ dtImgInfo[1] + "&instr=" + instr + "&book=" + book
					+ "&page=" + page + "&year=" + year + "&month=" + month
					+ "&day=" + day + "&dataTreeIndex=" + dataTreeIndex
					+ "&dataTreeDesc=" + dataTreeDesc;
			response.getParsedResponse().addImageLink(
					new ImageLinkInPage(link, instr + ".tiff"));
			
			if (viParseID == ID_SAVE_TO_TSD) {
				// Save document.
				msSaveToTSDFileName = keyCode + ".html";
				response.getParsedResponse().setFileName(
						getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = "<form>" + contents
						+ CreateFileAlreadyInTSD();
				smartParseDetails(response, contents, false);
			} else {
				// Get document details.
				String originalLink = sAction + "&shortened=true";
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET)
						+ originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", contentsArray.get("type"));
				data.put("book", book);
				data.put("page", page);
				data.put("year", year);
				data.put("month", month);
				data.put("day", day);
				data.put("instrNo", ro.cst.tsearch.servers.functions.NVClarkRO.getShortInstrumentNumber((keyCode)));
				
				if (isInstrumentSaved(keyCode, response.getParsedResponse().getDocument(), data)) {
					contents += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, contents);
					contents = addSaveToTsdButton(contents, sSave2TSDLink,
							viParseID);
				}
				response.getParsedResponse().setResponse(contents);
				response.getParsedResponse().setPageLink(
						new LinkInPage(sSave2TSDLink, originalLink,
								TSServer.REQUEST_SAVE_TO_TSD));
			}
			break;
		case ID_GET_LINK:
			if (sAction.contains("DocumentDetails.aspx"))
				ParseResponse(sAction, response, ID_DETAILS);
			break;
		}
	}

	protected static boolean downloadImageFromDataTree(InstrumentI i,
			List<DataTreeStruct> list, String path, Search search,
			String month, String day) {

		String commId = String.valueOf(search.getCommId());

		logger.info("(((((((((( downloadImageFromDataTree ---  instrument=" + i
				+ " path=" + path + " list=" + list);

		DataTreeAccount acc = DataTreeManager.getDatatTreeAccount(commId);
		List<DataTreeStruct> toSearch = new ArrayList<DataTreeStruct>(2);

		for (DataTreeStruct struct : list) {
			if ("DAILY_DOCUMENT".equalsIgnoreCase(struct.getDataTreeDocType())) {
				toSearch.add(struct);
			}
		}

		logger.info("(((((((((( downloadImageFromDataTree ---  instrument=" + i
				+ " path=" + path + " toSearch=" + toSearch);

		boolean imageDownloaded = false;
		List<DataTreeImageException> exceptions = new ArrayList<DataTreeImageException>();
		for (DataTreeStruct struct : toSearch) {
			try {
				if ((imageDownloaded = DataTreeManager
						.downloadImageFromDataTree(acc, struct, i, path, month,
								day))) {
					break;
				}
			} catch (DataTreeImageException e) {
				exceptions.add(e);
			}
		}
		if (!imageDownloaded && toSearch.size() == exceptions.size()
				&& !exceptions.isEmpty()) {
			DataTreeConn.logDataTreeImageException(i, search.getSearchID(), exceptions, true);
		}

		logger.info("(((((((((( downloadImageFromDataTree ---  return ="
				+ imageDownloaded);

		return imageDownloaded;
	}

	@Override
	public DownloadImageResult saveImage(ImageI image)
			throws ServerResponseException {
		try {
			long siteId = TSServersFactory.getSiteId(
					getSearch().getSa().getAtribute(SearchAttributes.P_STATE_ABREV), 
					getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY_NAME), 
					"DG");
			NVClarkDTG nvClarkDTG = (NVClarkDTG)TSServersFactory.GetServerInstance((int)siteId, getSearch().getID());
			int commId = getSearch().getCommId();
			if( nvClarkDTG.getDataSite().isEnableSite(commId) && nvClarkDTG.getDataSite().isEnabledIncludeInTsr(commId)) {
				return nvClarkDTG.saveImage(image);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Parse the intermediary response.
	 * 
	 * @param response
	 * @param rsResponse
	 * @param outputTable
	 * @param nvClarkRO
	 * @return
	 * @throws Exception
	 */
	private Collection<ParsedResponse> smartParseIntermediary(
			ServerResponse response, String rsResponse,
			StringBuilder outputTable, NVClarkRO nvClarkRO) throws Exception {
		Collection<ParsedResponse> responses = new Vector<ParsedResponse>();
		if (rsResponse == null || response == null) {
			return responses;
		}
		Search search = nvClarkRO.getSearch();
		searchId = search.getID();
		HtmlParser3 parser = new HtmlParser3(rsResponse);
		TableTag mainTableNode = (TableTag) parser.getNodeByAttribute("class",
				"rgMasterTable", true);
		if (mainTableNode == null) {
			return responses;
		}
		TableRow[] rows = mainTableNode.getRows();
		// Parse each row.
		for (int i = 2; i < rows.length; i++) {
			ResultMap resultMap = new ResultMap();
			TableRow row = rows[i];
			if (row.getAttribute("class").equals("rgRow") == false
					&& row.getAttribute("class").equals("rgAltRow") == false)
				// Not an usable row.
				continue;

			// Change external link to internal one.
			NodeList linkList = row.getChildren().extractAllNodesThatMatch(
					new TagNameFilter("a"), true);
			String link = "";
			if (linkList.size() > 0) {
				LinkTag linkTag = (LinkTag) linkList.elementAt(0);
				String onClick = linkTag.getAttribute("onclick").trim();
				Matcher m = Pattern.compile("(&quot;)(.*)(&quot;,)").matcher(
						onClick);
				while (m.find()) {
					link = m.group(2);
				}
				link = CreatePartialLink(TSConnectionURL.idGET)
						+ "RecorderEcommerce/" + link.replace("&amp;", "&");
				linkTag.setLink(link);
				linkTag.removeAttribute("onclick");
			}

			// Parse columns.
			TableColumn[] columns = row.getColumns();
			if (columns.length < 8)
				return responses;
			String instrNr = columns[2].toPlainTextString().trim();
			String serverDocType = columns[3].toPlainTextString().trim();
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");
			resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(),
					ro.cst.tsearch.servers.functions.NVClarkRO.getShortInstrumentNumber((instrNr)));
			resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), columns[5]
					.toPlainTextString().trim().split(" ")[0]);
			resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(),
					serverDocType);
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
					columns[6].toPlainTextString().trim());

			// Parse grantor/grantee.
			String grantor = columns[0].toPlainTextString().trim();
			String grantee = columns[1].toPlainTextString().trim();

			if (StringUtils.isNotEmpty(grantor)) {
				resultMap.put("SaleDataSet.Grantor", grantor);
			}
			if (StringUtils.isNotEmpty(grantee)) {
				resultMap.put("SaleDataSet.Grantee", grantee);
			}

			ro.cst.tsearch.servers.functions.NVClarkRO.addNames("GrantorSet",
					resultMap, grantor, mSearch.getSearchID());
			ro.cst.tsearch.servers.functions.NVClarkRO.addNames("GranteeSet",
					resultMap, grantee, mSearch.getSearchID());

			HashMap<String, String> data = new HashMap<String, String>();
			data.put("type", serverDocType);
			boolean saved = nvClarkRO.isInstrumentSavedInIntermediary(instrNr,
					data);
			String checkbox = saved ? "saved"
					: "<input type='checkbox' name='docLink' value='" + link
							+ "'>";

			// Add current response.
			ParsedResponse currentResponse = new ParsedResponse();
			String rowAsString = row.toHtml().replaceFirst("(<tr.*>)",
					"$1<td align=\"left\">" + checkbox + "</td>");
			Bridge b = new Bridge(currentResponse, resultMap, searchId);
			b.mergeInformation();
			currentResponse.setParentSite(response.isParentSiteSearch());
			currentResponse.setOnlyResponse(rowAsString);
			LinkInPage linkInPage = new LinkInPage(link, link,
					TSServer.REQUEST_SAVE_TO_TSD);
			currentResponse.setPageLink(linkInPage);
			currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,
					rowAsString);

			Bridge bridge = new Bridge(currentResponse, resultMap, searchId);
			RegisterDocumentI document = (RegisterDocumentI) bridge
					.importData();
			currentResponse.setDocument(document);

			responses.add(currentResponse);

		}

		// Create the table header.
		response.getParsedResponse()
				.setHeader(
						"<table align=\"center\" width=\"90%\" cellspacing=\"0\" class=\"rgMasterTable\" rules=\"all\" border=\"1\" id=\"ctl00_ContentPlaceHolder1_RadGrid3_ctl00\">\n"
								+ "<tr><th scope=\"col\" class=\"rgHeader\">"
								+ SELECT_ALL_CHECKBOXES
								+ "</th><th scope=\"col\" class=\"rgHeader\">First Party Name</th><th scope=\"col\" class=\"rgHeader\">First Cross Party Name</th><th scope=\"col\" class=\"rgHeader\">Instrument #</th><th scope=\"col\" class=\"rgHeader\">Document Type</th><th scope=\"col\" class=\"rgHeader\">Modifier</th><th scope=\"col\" class=\"rgHeader\">Record Date</th><th scope=\"col\" class=\"rgHeader\">Parcel #</th><th scope=\"col\" class=\"rgHeader\">Remarks</th><th scope=\"col\" class=\"rgHeader\">Total Value</th></tr>");
		response.getParsedResponse().setFooter("</table>");
		outputTable.append(rsResponse);

		return responses;
	}

	@Override
	public DocumentI smartParseDetails(ServerResponse response,
			String detailsHtml, boolean fillServerResponse) {
		DocumentI document = null;
		try {
			ResultMap map = new ResultMap();
			String imageLink = (String) parseAndFillResultMap(response,
					detailsHtml, map);
			map.removeTempDef();// this is for removing tmp items. we remove
								// them here to not remove them in every place
								// when we parse something.
			Bridge bridge = new Bridge(response.getParsedResponse(), map,
					searchId);
			document = bridge.importData();

			if (imageLink != null) {
				// Not yet implemented, since getting the image requires us to
				// be logged in.
				getSearch().addImagesToDocument(document, imageLink);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		response.getParsedResponse().setOnlyResponse(
				detailsHtml.replaceAll("<a\\s+href[^>]+>([^<]+)</a>", "$1")
						.replace("View Image", ""));
		if (document != null) {
			response.getParsedResponse().setDocument(document);
		}
		return document;
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response,
			String detailsHtml, ResultMap resultMap) {
		return ro.cst.tsearch.servers.functions.NVClarkRO
				.parseAndFillResultsMap(response, detailsHtml, resultMap,
						getSearch());
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
		
		if (super.isInstrumentSaved(instrumentNo, null, data) || 
				super.isInstrumentSaved(ro.cst.tsearch.servers.functions.NVClarkRO.getShortInstrumentNumber(instrumentNo), null, data)) {
			return true;
		}
		
		DocumentsManagerI documentManager = getSearch().getDocManager();
		try {
			documentManager.getAccess();
			boolean validServerDoctype = false;
			String docCateg = null;

			InstrumentI instr = new com.stewart.ats.base.document.Instrument(ro.cst.tsearch.servers.functions.NVClarkRO.getShortInstrumentNumber(instrumentNo));
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
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module = null;

		RejectAlreadySavedDocumentsFilterResponse rejectSavedDocuments = (new RejectAlreadySavedDocumentsFilterResponse(
				searchId));
		rejectSavedDocuments.setIgnoreDocumentCategory(true);
		rejectSavedDocuments.setIgnoreDocumentSubcategory(true);
		DocsValidator betweenDatesValidator = BetweenDatesFilterResponse
				.getDefaultIntervalFilter(searchId).getValidator();

		if (hasPin()) {
			// Search by PIN.
			module = new TSServerInfoModule(
					serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
			module.addFilter(rejectSavedDocuments);
			module.addValidator(betweenDatesValidator);
			modules.add(module);
		}
		
		//OCR last transfer
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
		module.clearSaKeys();
		OcrFakeIterator ocrFakeIterator = new OcrFakeIterator(searchId);
		ocrFakeIterator.setInitAgain(true);
		module.addIterator(ocrFakeIterator);
		modules.add(module);
		
		serverInfo.setModulesForAutoSearch(modules);
	}
	
	
	/**
     * Looks for the a document having the same instrumentNo
     * @param instrumentNo
     * @param documentToCheck if not null will only compare its instrument with saved documents
     * @param data
     * @return
     */
	@Override
    public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		if(documentToCheck != null) {
    			if(documentManager.getDocument(documentToCheck.getInstrument()) != null){
    				if (GWTDataSite.isRealRoLike(dataSite.getSiteTypeInt())){
	    				RegisterDocumentI docFound = (RegisterDocumentI) documentManager.getDocument(documentToCheck.getInstrument());
	    				RegisterDocumentI docToCheck = (RegisterDocumentI) documentToCheck;
	    				
	    				docToCheck.mergeDocumentsInformation(docFound, searchId, true, false);
    				}
    				return true;
    			}
    		} else {
	    		InstrumentI instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
	    		if(data != null) {
		    		if(!StringUtils.isEmpty(data.get("type"))) {
		        		String serverDocType = data.get("type");
		    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId); 
		            	instr.setDocType(docCateg);
		            	instr.setDocSubType("MISCELLANEOUS");
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
	    			
    				if(data!=null) {
    					if(!StringUtils.isEmpty(data.get("type"))){
			        		String serverDocType = data.get("type"); 
			    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
			    	    	for (DocumentI documentI : almostLike) {
			    	    		if(documentI.getDocType().equals(docCateg)){
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
    	
    	if(data == null) {
			return false;
		}
    	try {
    		documentManager.getAccess();
    		for(DocumentI e: documentManager.getDocumentsWithDataSource(false, "DG")){
    			InstrumentI savedInst = e.getInstrument();
    			if( savedInst.getInstno().equals(data.get("instrNo"))  
    					&& (savedInst.getBook().equals(data.get("book")) && savedInst.getPage().equals(data.get("page")))  
    					&& (savedInst.getYear()+"").equals(data.get("year"))
    			){
    				return true;
    			}
    		}
    	} finally {
    		documentManager.releaseAccess();
    	}
		
    	return false;
    }

}
