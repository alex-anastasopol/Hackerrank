package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.utils.XmlUtils.applyTransformation;
import static ro.cst.tsearch.utils.XmlUtils.parseXml;
import static ro.cst.tsearch.utils.XmlUtils.xpathQuery;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.stewart.ats.base.address.Address;
import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.AssessorDocument;
import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.TownShip;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.parties.Party;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.Pin;
import com.stewart.ats.base.property.PinI;
import com.stewart.ats.base.property.Property;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.SavedFromType;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http3.HttpManager3;
import ro.cst.tsearch.connection.http3.HttpSite3;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.StateContants;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.SimpleParseTokenResult;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.address.CityFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.MultiplePinFilterResponse;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.name.NameFactory;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.token.AddressAbrev;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.functions.GenericDASLNDBFunctions;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.parentsite.ModuleWrapperManager;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PartyNameSet.PartyNameSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.XmlUtils;

@SuppressWarnings("deprecation")
public class GenericCL extends TSServer {
	
	private static final long serialVersionUID = -9151556444745168593L;
	
	private static final String XSL_PATH = BaseServlet.REAL_PATH + File.separator + "WEB-INF" + File.separator + "classes" + File.separator + "resource" + File.separator + "CL" + File.separator;
	private static final String XSL_SUBJECT_PATH = XSL_PATH + "CLSubjectStyleSheet.xsl";
	private static final String XSL_LIEN_PATH = XSL_PATH + "CLLienStyleSheet.xsl";
	
	private static String subjectStyleSheet = "";
	private static String lienStyleSheet = "";
	
	public GenericCL(long searchId) {
		super(searchId);
	}

	public GenericCL(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();

		ModuleWrapperManager moduleWrapperManager = ModuleWrapperManager.getInstance();
		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID);
		String siteName = StateCountyManager.getInstance().getSTCounty(dataSite.getCountyId()) + dataSite.getSiteType();

		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
		if (tsServerInfoModule != null) {

			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.NAME_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX);
		if (tsServerInfoModule != null) {

			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.ADDRESS_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX);
		if (tsServerInfoModule != null) {

			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.PARCEL_ID_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADV_SEARCH_MODULE_IDX);
		if (tsServerInfoModule != null) {

			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.ADV_SEARCH_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ADDRESS_MODULE_IDX2);
		if (tsServerInfoModule != null) {

			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.ADDRESS_MODULE_IDX2, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SALES_MODULE_IDX);
		if (tsServerInfoModule != null) {

			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.SALES_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		return msiServerInfoDefault;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {

		String rsResponse = Response.getResult();
		rsResponse = rsResponse.replaceFirst("(?is)<!DOCTYPE\\b.*?>", "");
		
		ParsedResponse parsedResponse = Response.getParsedResponse();
		
		if (StringUtils.isEmpty(subjectStyleSheet)) {
			subjectStyleSheet = FileUtils.readFile(XSL_SUBJECT_PATH);
			if (StringUtils.isEmpty(subjectStyleSheet)) {
				return ;
			}
		}
		
		if (StringUtils.isEmpty(lienStyleSheet)) {
			lienStyleSheet = FileUtils.readFile(XSL_LIEN_PATH);
			if (StringUtils.isEmpty(lienStyleSheet)) {
				return ;
			}
		}
		
		if (viParseID==ID_SEARCH_BY_SALES) {		//Search by Voluntary and Involuntary Lien in Parent Site
			Node xmlLien = null;
			if (!StringUtils.isEmpty(rsResponse)) {
				try {
					xmlLien = XmlUtils.parseXml(rsResponse);
				} catch (RuntimeException e) {
					logger.error("XML parsing exception", e);
				}
			}
			
			if (xmlLien!=null) {
				String responseCode = XmlUtils.findNodeValue(xmlLien, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PRODUCT[1]/STATUS/@_Code");
				if ("1004".equals(responseCode)) {		//UNABLE TO ACCEPT REQUEST AS SUBMITTED.
					Response.getParsedResponse().setOnlyResponse(NO_DATA_FOUND);
					return;
				}
			}
			
			String referencesHtml = NO_DATA_FOUND;
			if (xmlLien!=null) {
				referencesHtml = applyTransformation(xmlLien, lienStyleSheet);
			}
			Response.getParsedResponse().setResponse(referencesHtml);
			return;
		}
		
		Node xmlDoc = null;
		AssessorDocumentI document = (AssessorDocumentI)parsedResponse.getDocument();
		if (document==null) {	//details (first time) or intermediary result
			try {
				xmlDoc = parseXml(rsResponse);
			} catch (RuntimeException e) {
				logger.error("XML parsing exception", e);
				return;
			}
			
			boolean success = false;
			if (xmlDoc!=null) {
				String responseCode = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/STATUS/@_Code");
				if ("0400".equals(responseCode)) {			//SUCCESSFULLY PROCESSED. NO RESPONSE-LEVEL ERRORS ENCOUNTERED
					success = true;
				}
			}
			
			if (!success) {
				Response.getParsedResponse().setOnlyResponse(NO_DATA_FOUND);
				return;
			}
			
			String responseCode2 = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PRODUCT[1]/STATUS/@_Code");
			if ("0310".equals(responseCode2)) {				//MULTIPLE PROPERTIES FOUND
				viParseID = ID_SEARCH_BY_NAME;
			} else if ("0315".equals(responseCode2)) {		//NO RECORDS FOUND FOR SEARCH CRITERIA SUBMITTED.
				Response.getParsedResponse().setOnlyResponse(NO_DATA_FOUND);
				return;
			}
		}
		
		switch (viParseID) {
		
		case ID_SEARCH_BY_NAME:
			
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() > 0) {
				parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
				parsedResponse.setOnlyResponse(outputTable.toString());
				parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE,	outputTable.toString());
			}
			
			break;

		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			StringBuilder serialNumber = new StringBuilder();
			String details = getDetails(Response, rsResponse, xmlDoc, serialNumber);
			String filename = serialNumber + ".html";

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				HashMap<String, String> data = new HashMap<String, String>();
				data.put("type", "ASSESSOR");
				data.put("dataSource", getDataSite().getSiteTypeAbrev());
				if (isInstrumentSaved(serialNumber.toString(), null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, Response);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}

				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);

			} else {
				smartParseDetails(Response, details);

				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(details);

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
			}
			break;

		case ID_GET_LINK:
			ParseResponse(sAction, Response, ID_DETAILS);
			break;

		default:
			break;
		
		}

	}
	
	protected String cleanStreetNameFromSuffix (String streetName, String streetSuffix) {
		String cleanStrName = streetName;
		
		if (containsDuplicatedAdrSuffix(streetName, streetSuffix))  {
			String tmpAdr = ro.cst.tsearch.search.address.Normalize.normalizeString(cleanStrName);
			String tmpSuf = ro.cst.tsearch.search.address.Normalize.normalizeString(streetSuffix);
			cleanStrName = tmpAdr.replaceFirst("(?is)\\b" + tmpSuf + "\\b", "").trim();
		}
		
		return cleanStrName;
	}
	
	protected boolean containsDuplicatedAdrSuffix (String streetName, String streetSuffix) {
		String tmpAdr = ro.cst.tsearch.search.address.Normalize.normalizeString(streetName);
		String tmpSuf = ro.cst.tsearch.search.address.Normalize.normalizeString(streetSuffix);
		String[] tokens = tmpAdr.split("\\s");
		
		for (int i=0; i<tokens.length; i++) {
			String elem = tokens[i];
			if (Normalize.isSuffix(elem) || Normalize.isSpecialSuffix(elem)) {
				if (elem.equals(tmpSuf)) {
					return true;
				}
			} 
		}
		
		return false;
	}
	
	protected String getDetails(ServerResponse Response, String rsResponse, Node xmlDoc, StringBuilder instno) {
		
		ParsedResponse parsedResponse = Response.getParsedResponse();
		String modifiedSubjectStyleSheet = subjectStyleSheet;
		
		/* If from memory - use it as is */
		if (xmlDoc==null) {
			AssessorDocumentI document = (AssessorDocumentI)parsedResponse.getDocument();
			if (document!=null) {
				instno.append(document.getInstno());
			}
			return rsResponse;
		}
		
		SearchType searchType = SearchType.NA;
		LinkInPage lip = parsedResponse.getPageLink();
		if (lip!=null) {
			String link = lip.getLink();
			String source = StringUtils.extractParameter(link, "source=([^&?]*)");
			if (!"".equals(source)) {
				searchType = SearchType.valueOf(source);
			}
		} else {
			Object objectModuleSource = parsedResponse.getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
			if (objectModuleSource != null) {
				if (objectModuleSource instanceof TSServerInfoModule) {
					TSServerInfoModule moduleSource = (TSServerInfoModule) objectModuleSource;
					String source = moduleSource.getSearchType();
					if (!"".equals(source)) {
						searchType = SearchType.valueOf(source);
					}
				}
			}
		}
		
		String _StreetAddress = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_StreetAddress");
		String _City = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_City");
		String _State = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_State");
		String _PostalCode = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_PostalCode");
		String _County = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_County");
		String _PlusFourPostalCode = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_PlusFourPostalCode");
		
		String _StandardizedHouseNumber = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PARSED_STREET_ADDRESS/@_StandardizedHouseNumber");
		String _DirectionPrefix = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PARSED_STREET_ADDRESS/@_DirectionPrefix");
		String _StreetName = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PARSED_STREET_ADDRESS/@_StreetName");
		String _DirectionSuffix = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PARSED_STREET_ADDRESS/@_DirectionSuffix");
		String _StreetSuffix = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PARSED_STREET_ADDRESS/@_StreetSuffix");
		String _ApartmentOrUnit = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PARSED_STREET_ADDRESS/@_ApartmentOrUnit");
		
		String streetName = org.apache.commons.lang.StringUtils.defaultString(_DirectionPrefix) + " " +
				            org.apache.commons.lang.StringUtils.defaultString(_StreetName) + " " +
				            org.apache.commons.lang.StringUtils.defaultString(_StreetSuffix) + " " +
				            org.apache.commons.lang.StringUtils.defaultString(_DirectionSuffix) + " #" +
				            org.apache.commons.lang.StringUtils.defaultString(_ApartmentOrUnit);
		
		//OHFranklin, doc# 010-060390-00 --> TWENTY FOURTH AVENUE AVE  ==> in this case seems that street name is TWENTY FOURTH AVENUE !?!
//		String streetName = org.apache.commons.lang.StringUtils.defaultString(_DirectionPrefix) + " " +
//	            org.apache.commons.lang.StringUtils.defaultString(_StreetName);
//		streetName = cleanStreetNameFromSuffix(streetName, _StreetSuffix) + " " +
//				org.apache.commons.lang.StringUtils.defaultString(_StreetSuffix) + " " +
//				org.apache.commons.lang.StringUtils.defaultString(_DirectionSuffix) + " #" +
//	            org.apache.commons.lang.StringUtils.defaultString(_ApartmentOrUnit);
		
		String streetNameAndSuffix = org.apache.commons.lang.StringUtils.defaultString(_StreetName) + " " +
	            org.apache.commons.lang.StringUtils.defaultString(_StreetSuffix);
		
//		String streetNameAndSuffix = org.apache.commons.lang.StringUtils.defaultString(_StreetName) + " ";
//		streetNameAndSuffix = cleanStreetNameFromSuffix(streetNameAndSuffix, _StreetSuffix) + " " +
//				org.apache.commons.lang.StringUtils.defaultString(_StreetSuffix);
		
		String streetNameAndFullSuffix = org.apache.commons.lang.StringUtils.defaultString(_StreetName) + " " +
	            AddressAbrev.getFullSuffixFromAbbreviation(org.apache.commons.lang.StringUtils.defaultString(_StreetSuffix));
		
//		String streetNameAndFullSuffix = org.apache.commons.lang.StringUtils.defaultString(_StreetName);
//		streetNameAndFullSuffix = cleanStreetNameFromSuffix(streetNameAndFullSuffix, _StreetSuffix) + " " +
//				AddressAbrev.getFullSuffixFromAbbreviation(org.apache.commons.lang.StringUtils.defaultString(_StreetSuffix));
		
		streetName = streetName.replaceAll("\\s{2,}", " ");
		streetName = streetName.replaceFirst("#$", "").trim();
		
		streetName = streetName.replaceFirst("(?is)^VAC/COR\\s+", "").trim();
		
		if (streetName.contains("/")) {// e.g. SE 77 LN/SE 73 CT for PIN 111016-05510009-0010
			streetName = streetName.substring(0, streetName.indexOf("/"));
		}

		String _AssessorsParcelIdentifier = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_AssessorsParcelIdentifier");
		String _LotIdentifier = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_LotIdentifier");
		String _BlockIdentifier = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_BlockIdentifier");
		String _AssessorsAlternateParcelIdentifier = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_AssessorsAlternateParcelIdentifier");
		String _AssessmentYear = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_AssessmentYear");
		String _TotalAssessedValueAmount = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_TotalAssessedValueAmount");
		String _ImprovementValueAmount = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_ImprovementValueAmount");
		String _LandValueAmount = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_LandValueAmount");
		String _TotalTaxableValueAmount = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_TotalTaxableValueAmount");
		String _TaxYear = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_TaxYear");
		String _RealEstateTotalTaxAmount = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_RealEstateTotalTaxAmount");
		String _TractNumberIdentifier = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_TractNumberIdentifier");
		String _SubdivisionIdentifier = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_SubdivisionIdentifier");
		String _TextDescription = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_LEGAL_DESCRIPTION/@_TextDescription");
		String _SectionNumberIdentifier = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_LEGAL_DESCRIPTION/_UNPLATTED_LAND/@_SectionNumberIdentifier");
		String _TownshipNumber = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_LEGAL_DESCRIPTION/_UNPLATTED_LAND/@_TownshipNumber");
		String _RangeNumberIdentifier = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_LEGAL_DESCRIPTION/_UNPLATTED_LAND/@_RangeNumberIdentifier");
		String _LegalBookAndPageIdentifier = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/@_LegalBookAndPageIdentifier");
		
		//OHFranklin: 010-123030-00
		if (org.apache.commons.lang.StringUtils.isNotBlank(streetNameAndSuffix)){
			if (org.apache.commons.lang.StringUtils.isNotBlank(_TextDescription)){
				if (_TextDescription.matches("(?is).*\\b" + streetNameAndSuffix + "\\b.*")){
					_TextDescription = _TextDescription.replaceFirst("(?is)\\b" + streetNameAndSuffix + "\\b", "").trim();
				}
			}
			if (org.apache.commons.lang.StringUtils.isNotBlank(_SubdivisionIdentifier)){
				if (_SubdivisionIdentifier.matches("(?is).*\\b" + streetNameAndSuffix + "\\b.*")){
					_SubdivisionIdentifier = _SubdivisionIdentifier.replaceFirst("(?is)\\b" + streetNameAndSuffix + "\\b", "").trim();
				}
			}
		}
		if (org.apache.commons.lang.StringUtils.isNotBlank(streetNameAndFullSuffix)){
			if (org.apache.commons.lang.StringUtils.isNotBlank(_TextDescription)){
				if (_TextDescription.matches("(?is).*\\b" + streetNameAndFullSuffix + "\\b.*")){
					_TextDescription = _TextDescription.replaceFirst("(?is)\\b" + streetNameAndFullSuffix + "\\b", "").trim();
				}
			}
			if (org.apache.commons.lang.StringUtils.isNotBlank(_SubdivisionIdentifier)){
				if (_SubdivisionIdentifier.matches("(?is).*\\b" + streetNameAndFullSuffix + "\\b.*")){
					_SubdivisionIdentifier = _SubdivisionIdentifier.replaceFirst("(?is)\\b" + streetNameAndFullSuffix + "\\b", "").trim();
				}
			}
		}
		StringBuilder allPlatBookPages = new StringBuilder();
		String platBook = "";
		String platPage = "";
		String[] split = _LegalBookAndPageIdentifier.split("-");
		if (split.length==2) {
			platBook = split[0].replaceFirst("(?is)^[A-Z]+(\\d+)", "$1");	//MB33, FL Volusia 3242-20-01-5120
			platBook = platBook.replaceFirst("^0+", "");
			platPage = split[1].replaceFirst("^0+", "");
			allPlatBookPages.append(platBook).append("@@@@@").append(platPage).append(" ");
		}
		
		String _LastSalesDate = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_SALES_HISTORY/@_LastSalesDate");
		String _LastRecordingDate = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_SALES_HISTORY/@_LastRecordingDate");
		String _LastSalesPriceAmount = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_SALES_HISTORY/@_LastSalesPriceAmount");
		String _InstrumentNumber = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_SALES_HISTORY/@_InstrumentNumber");
		String _BookPage = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_SALES_HISTORY/@_BookPage");
		String _DocumentNumberIdentifier = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_SALES_HISTORY/@_DocumentNumberIdentifier");
		String _DeedTypeDescription = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_SALES_HISTORY/@_DeedTypeDescription");
		String _SellerName = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_SALES_HISTORY/@_SellerName");
		
		String _PriorSaleDate = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_SALES_HISTORY/@_PriorSaleDate");
		String _PriorRecordingDate = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_SALES_HISTORY/@_PriorRecordingDate");
		String _PriorSalePriceAmount = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_SALES_HISTORY/@_PriorSalePriceAmount");
		String _PriorInstrumentNumber = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_SALES_HISTORY/@_PriorInstrumentNumber");
		String _PriorBookPage = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_SALES_HISTORY/@_PriorBookPage");
		String _PriorDocumentNumberIdentifier = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_SALES_HISTORY/@_PriorDocumentNumberIdentifier");
		String _PriorDeedTypeDescription = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_SALES_HISTORY/@_PriorDeedTypeDescription");
		
		String _FirstMortgageAmount = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_MORTGAGE_HISTORY/@_FirstMortgageAmount");
		String _FirstMortgageTypeDescription = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_MORTGAGE_HISTORY/@_FirstMortgageTypeDescription");
		String _FirstMortgageInstrumentNumber = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_MORTGAGE_HISTORY/@_FirstMortgageInstrumentNumber");
		String _FirstMortgageBookPage = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_MORTGAGE_HISTORY/@_FirstMortgageBookPage");
		String _FirstMortgageDocumentNumber = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_MORTGAGE_HISTORY/@_FirstMortgageDocumentNumber");
		
		String _TransferSaleDate = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_OWNER_TRANSFER_INFORMATION/@_TransferSaleDate");
		String _TransferRecordingDate = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_OWNER_TRANSFER_INFORMATION/@_TransferRecordingDate");
		String _TransferSalePrice = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_OWNER_TRANSFER_INFORMATION/@_TransferSalePrice");
		String _TransferInstrumentNumber = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_OWNER_TRANSFER_INFORMATION/@_TransferInstrumentNumber");
		String _TransferBookPage = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_OWNER_TRANSFER_INFORMATION/@_TransferBookPage");
		String _TransferDocumentNumber = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_OWNER_TRANSFER_INFORMATION/@_TransferDocumentNumber");
		String _TransferDeedTypeDescription = XmlUtils.findNodeValue(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/_PROPERTY_HISTORY/_OWNER_TRANSFER_INFORMATION/@_TransferDeedTypeDescription");
		
		ResultMap resultMap = new ResultMap();
		
		String zip = _PostalCode + ("".equals(_PlusFourPostalCode)?"":"-"+_PlusFourPostalCode);
		resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
		
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		resultMap.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), _SubdivisionIdentifier);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), _SectionNumberIdentifier);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), _TownshipNumber);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), _RangeNumberIdentifier);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), _LotIdentifier);
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), _BlockIdentifier);
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), getDataSite().getSiteTypeAbrev());
		String ncbNo = "";
		String absNo = "";
		String section = "";
		String subdivisionCond = "";
		String subdivisionUnit = "";
		String subdivisionPhase = "";
		String tract = "";
		
		resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), _InstrumentNumber);
		resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), _LastSalesDate);
		String book = "";
		String page = "";
		String bookType = "";
		
		resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), _City);
		
		try {
			GenericFunctions1.correctZIPCode(resultMap, searchId);
			zip = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.ZIP.getKeyName()));
			
			GenericFunctions2.legalGenericDASLNDB(resultMap, searchId);
			
			_LotIdentifier = org.apache.commons.lang.StringUtils.defaultString((String) resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName()));
			_BlockIdentifier = org.apache.commons.lang.StringUtils.defaultString((String) resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName()));
			
			streetName = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.STREET_NAME.getKeyName()));
			ncbNo = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.NCB_NO.getKeyName()));
			absNo = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.ABS_NO.getKeyName()));
			
			if(org.apache.commons.lang.StringUtils.isBlank(absNo) && org.apache.commons.lang.StringUtils.isNotBlank(_TextDescription)) {
				SimpleParseTokenResult absNoFromLegal = GenericFunctions2.getAbsNoFromLegal(Integer.parseInt(getSearch().getStateId()), _TextDescription);
				if(absNoFromLegal != null && org.apache.commons.lang.StringUtils.isNotBlank(absNoFromLegal.getTokenParsed())) {
					absNo = absNoFromLegal.getTokenParsed();
					_TextDescription = absNoFromLegal.getFinalSource();
				}
			}
			
			if (org.apache.commons.lang.StringUtils.isBlank(tract) && org.apache.commons.lang.StringUtils.isNotBlank(_TextDescription)) {
				SimpleParseTokenResult tractFromLegal = GenericFunctions2.getTractFromLegal(Integer.parseInt(getSearch().getStateId()), _TextDescription);
				if (tractFromLegal != null && org.apache.commons.lang.StringUtils.isNotBlank(tractFromLegal.getTokenParsed())) {
					tract = tractFromLegal.getTokenParsed();
					if (org.apache.commons.lang.StringUtils.isNotEmpty(_TractNumberIdentifier)){
						if (_TractNumberIdentifier.matches(tract + "0+")){
							_TractNumberIdentifier = tract;
						}
					} else {
						if (org.apache.commons.lang.StringUtils.isNotEmpty(tract)){
							_TractNumberIdentifier = tract;
						}
					}
					_TextDescription = tractFromLegal.getFinalSource();
				}
			}
			section = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.SECTION.getKeyName()));
			
			platBook = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName()));
			platPage = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.PLAT_NO.getKeyName()));
			if (!StringUtils.isEmpty(platBook) && !StringUtils.isEmpty(platBook)) {
				allPlatBookPages.append(platBook).append("@@@@@").append(platPage).append(" ");
			}
				
			_SubdivisionIdentifier = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName()));
			subdivisionCond = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName()));
			subdivisionUnit = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName()));
			subdivisionPhase = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName()));
			
			if(org.apache.commons.lang.StringUtils.isBlank(subdivisionPhase) 
					&& org.apache.commons.lang.StringUtils.isNotBlank(_TextDescription)) {
				// extract phase from legal description
				String phasePatternPart = "\\d+\\w?|\\b[A-Z]\\b";
				String phasePattern = "\\bPH(?:(?:ASE)|S)?(( (" + phasePatternPart + ")(-" + phasePatternPart + ")?)+)\\b";
				Pattern p = Pattern.compile(phasePattern);
				Matcher m = p.matcher(_TextDescription);
				if (m.find()) {
					if (!m.group(1).trim().equals("0")) {
						subdivisionPhase = m.group(1).replaceAll("-0*", " ").trim().replaceFirst("^0+(\\w.*)", "$1");
					}
					_TextDescription = _TextDescription.replaceFirst(m.group(), "");
				}
			}
			
			_SectionNumberIdentifier = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName()));
			_TownshipNumber = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName()));
			_RangeNumberIdentifier = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName()));

			if (_SectionNumberIdentifier.isEmpty() || _TownshipNumber.isEmpty() || _RangeNumberIdentifier.isEmpty()) {
				ResultTable strRT = (ResultTable) resultMap.get("PropertyIdentificationSet");

				if (strRT != null) {
					String[] townshipHeader = strRT.getHead();
					String[][] townshipTable = strRT.getBody();

					if (townshipTable.length > 0 && townshipTable[0].length == townshipHeader.length) {

						for (int i = 0; i < townshipHeader.length; i++) {
							String value = org.apache.commons.lang.StringUtils.defaultString(townshipTable[0][i]);

							if (_SectionNumberIdentifier.isEmpty() && townshipHeader[i].equalsIgnoreCase("SubdivisionSection")) {
								_SectionNumberIdentifier = value;
							} else if (_TownshipNumber.isEmpty() && townshipHeader[i].equalsIgnoreCase("SubdivisionTownship")) {
								_TownshipNumber = value;
							} else if (_RangeNumberIdentifier.isEmpty() && townshipHeader[i].equalsIgnoreCase("SubdivisionRange")) {
								_RangeNumberIdentifier = value;
							}
						}
					}
				}
			}
			
			GenericFunctions2.instrumentNoFormatNDB(resultMap, searchId);
			_InstrumentNumber = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName()));
			book = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(SaleDataSetKey.BOOK.getKeyName()));
			page = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(SaleDataSetKey.PAGE.getKeyName()));
			bookType = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(SaleDataSetKey.BOOK_TYPE.getKeyName()));
			if (!"".equals(book) && !"".equals(page)) {
				_BookPage = book + "-" + page;
			}
			
			GenericFunctions2.removeCityAndZip(resultMap, searchId);
			_City = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.CITY.getKeyName()));
			zip = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.ZIP.getKeyName()));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//task 9752, KS Johnson HP99990000-0137
		if (!StringUtils.isEmpty(_SubdivisionIdentifier) && !StringUtils.isEmpty(_TextDescription)) {
			if (_TextDescription.matches("(?is)^" + _SubdivisionIdentifier + "\\b.+")) {
				ResultMap resultMap2 = new ResultMap();
				resultMap2.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), _TextDescription);
				if (CountyConstants.KS_Johnson==dataSite.getCountyId()) {
					resultMap2.put("tmpDoNotReplaceNumbers", "true");
				}
				try {
					GenericFunctions2.legalGenericDASLNDB(resultMap2, searchId);
				} catch (Exception e) {
					e.printStackTrace();
				}
				String new_SubdivisionIdentifier = org.apache.commons.lang.StringUtils.defaultString((String)resultMap2.get(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName()));
				if (!StringUtils.isEmpty(new_SubdivisionIdentifier) && new_SubdivisionIdentifier.matches("(?is)^" + _SubdivisionIdentifier + "\\b.+")) {
					_SubdivisionIdentifier = new_SubdivisionIdentifier;
				}
			}
		}
		
		//MO Jackson 60-220-11-24-00-0-00-000
		if (StringUtils.isEmpty(_SubdivisionIdentifier) && !StringUtils.isEmpty(_TextDescription)) {
			ResultMap resultMap3 = new ResultMap();
			resultMap3.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), _TextDescription);
			try {
				GenericFunctions2.legalGenericDASLNDB(resultMap3, searchId);
			} catch (Exception e) {
				e.printStackTrace();
			}
			_SubdivisionIdentifier = org.apache.commons.lang.StringUtils.defaultString((String)resultMap3.get(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName()));
		}
		
		Matcher matcherNcbNo = GenericFunctions2.NCB_NO_PATTERN.matcher(_TextDescription);
		StringBuilder newmatcherNcbNos = new StringBuilder(); 
		while (matcherNcbNo.find()) {
			newmatcherNcbNos.append(matcherNcbNo.group(0)).append(" ");
		}
		ncbNo = concatenateLegalValues(ncbNo, newmatcherNcbNos.toString());
		
		int currentYear = Calendar.getInstance().get(Calendar.YEAR) - 2000;
		
		//extract and then delete from _TextDescription (BKs must not remain because they can be mistaken as block)
		//these references will be last added because they can have less details 
		Vector<SaleDataSet> legalDescBookPageSDS = new Vector<SaleDataSet>();
		String patternLegalDescBookPage = "(?is)\\bBK-(\\d+)\\s+PG-(\\d+)\\s+([A-Z]+)\\s+(\\d{2})-(\\d{2})-(\\d{2}(?:\\d{2})?)\\b";
		Matcher matcherLegalDescBookPage = Pattern.compile(patternLegalDescBookPage).matcher(_TextDescription);
		while (matcherLegalDescBookPage.find()) {
			SaleDataSet newSDS = new SaleDataSet();
			newSDS.setAtribute(SaleDataSetKey.BOOK.getShortKeyName(), matcherLegalDescBookPage.group(1).replaceFirst("^0+", ""));
			newSDS.setAtribute(SaleDataSetKey.PAGE.getShortKeyName(), matcherLegalDescBookPage.group(2).replaceFirst("^0+", ""));
			newSDS.setAtribute(SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(), matcherLegalDescBookPage.group(3));
			try {
				String year = matcherLegalDescBookPage.group(6);
				if (year.length()==2) {
					int yearInt = Integer.parseInt(year);
					year = (yearInt<=currentYear?"20":"19") + year;
				}
				String recordedDate = year + "-" + matcherLegalDescBookPage.group(4) + "-" + matcherLegalDescBookPage.group(5);
				newSDS.setAtribute(SaleDataSetKey.RECORDED_DATE.getShortKeyName(), recordedDate);
			} catch (NumberFormatException nfe) {}
			legalDescBookPageSDS.add(newSDS);
		}
		_TextDescription = _TextDescription.replaceAll(patternLegalDescBookPage, "");
		
		Vector<SaleDataSet> legalDescInstrNoSDS = new Vector<SaleDataSet>();
		String patternLegalDescInstrNo = "(?is)\\bR(\\d+)\\s+([A-Z]+)\\s+(\\d{2})-(\\d{2})-(\\d{2}(?:\\d{2})?)\\b";
		Matcher matcherLegalDescInstrNo = Pattern.compile(patternLegalDescInstrNo).matcher(_TextDescription);
		while (matcherLegalDescInstrNo.find()) {
			SaleDataSet newSDS = new SaleDataSet();
			newSDS.setAtribute(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), matcherLegalDescInstrNo.group(1).replaceFirst("^0+", ""));
			newSDS.setAtribute(SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(), matcherLegalDescInstrNo.group(2));
			try {
				String year = matcherLegalDescInstrNo.group(5);
				if (year.length()==2) {
					int yearInt = Integer.parseInt(year);
					year = (yearInt<=currentYear?"20":"19") + year;
				}
				String recordedDate = year + "-" + matcherLegalDescInstrNo.group(3) + "-" + matcherLegalDescInstrNo.group(4);
				newSDS.setAtribute(SaleDataSetKey.RECORDED_DATE.getShortKeyName(), recordedDate);
			} catch (NumberFormatException nfe) {}
			legalDescInstrNoSDS.add(newSDS);
		}
		_TextDescription = _TextDescription.replaceAll(patternLegalDescInstrNo, "");
		
		_TextDescription = GenericFunctions1.replaceNumbers(_TextDescription);
		
		subdivisionPhase = concatenateLegalValues(subdivisionPhase, "");
		
		List<TownShip> strs = new ArrayList<TownShip>();
		if (!StringUtils.isEmpty(_SectionNumberIdentifier)||!StringUtils.isEmpty(_TownshipNumber)||!StringUtils.isEmpty(_RangeNumberIdentifier)) {
			TownShip township = new TownShip();
			township.setSection(_SectionNumberIdentifier.replaceFirst("^0+", ""));
			township.setTownship(_TownshipNumber.replaceFirst("^0+", ""));
			township.setRange(_RangeNumberIdentifier.replaceFirst("^0+", ""));
			strs.add(township);
		}
		
		Matcher matcherSTR = Pattern.compile("(?is)\\bSEC\\.?\\s+(\\d+)[\\s,]+T(?:WP)?\\s*(\\d+[NSEW]*)(?:-|\\s+)R(?:GE)?\\s*(\\d+[NSEW]*)\\b").matcher(_TextDescription);
		while (matcherSTR.find()) {
			addSTR(matcherSTR.group(1), matcherSTR.group(2), matcherSTR.group(3), strs);
			_TextDescription = _TextDescription.replace(matcherSTR.group(0), "");
		}
		
		String sectionPattern = "(?is)\\b(SEC\\.?T?I?O?N?)\\s*((?:\\d+|[A-Z])(?:(?:\\s+(?:AND|&))?\\s+(?:\\d+|[A-Z]))*)[\\s,]";
		Matcher matcherSection = Pattern.compile(sectionPattern).matcher(_TextDescription + " ");
		StringBuilder newSections = new StringBuilder(); 
		while (matcherSection.find()) {
			newSections.append(matcherSection.group(2)).append(" ");
		}
		_TextDescription = _TextDescription.replaceAll(sectionPattern, "").trim();
		section = concatenateLegalValues(section, newSections.toString());
		
		Matcher matcherPlatBP = Pattern.compile("(?is)\\bP(?:LAT)?\\s*B(?:OO)?K?\\s*([A-Z0-9]+)\\s+PA?G?E?S?\\s*([A-Z0-9]+)\\b").matcher(_TextDescription);
		while (matcherPlatBP.find()) {
			platBook = matcherPlatBP.group(1).replaceFirst("^0+", "");
			platPage = matcherPlatBP.group(2).replaceFirst("^0+", "");
			allPlatBookPages.append(platBook).append("@@@@@").append(platPage).append(" ");
		}
			
		matcherPlatBP = Pattern.compile("(?is)\\bMAPPLATB\\s+([A-Z0-9]+)\\s+MAPPLATP\\s+([A-Z0-9]+)\\b").matcher(_TextDescription);
		while (matcherPlatBP.find()) {
			platBook = matcherPlatBP.group(1).replaceFirst("^0+", "");
			platPage = matcherPlatBP.group(2).replaceFirst("^0+", "");
			allPlatBookPages.append(platBook).append("@@@@@").append(platPage).append(" ");
		}
		//CASolano: 0134-441-090
		matcherPlatBP = Pattern.compile("(?is)\\bBK\\s*-\\s*PG\\s+([A-Z0-9]+)\\s*-\\s*([A-Z0-9]+)\\b").matcher(_TextDescription);
		while (matcherPlatBP.find()) {
			platBook = matcherPlatBP.group(1).replaceFirst("^0+", "");
			platPage = matcherPlatBP.group(2).replaceFirst("^0+", "");
			_TextDescription = _TextDescription.replaceFirst(matcherPlatBP.group(), "");
			allPlatBookPages.append(platBook).append("@@@@@").append(platPage).append(" ");
		}
		
		String platBookPages = LegalDescription.cleanValues(allPlatBookPages.toString(), false, true);
		
		Vector<SaleDataSet> vectorSDS = new Vector<SaleDataSet>();
		Vector<SaleDataSet> possibleSDS = new Vector<SaleDataSet>();
		
		Matcher matcherRefInstrNo = Pattern.compile("(?is)\\bO(?:FF\\s+)?R(?:EC)?\\s+(\\d+)[-/](\\d+)\\b").matcher(_TextDescription);
		while (matcherRefInstrNo.find()) {
			SaleDataSet newSDS = new SaleDataSet();
			newSDS.setAtribute(SaleDataSetKey.RECORDED_DATE.getShortKeyName(), matcherRefInstrNo.group(1));
			newSDS.setAtribute(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), matcherRefInstrNo.group(2));
			possibleSDS.add(newSDS);
		}
		
		Matcher matcherRefBookPage = Pattern.compile("(?is)\\bO(?:FFICIAL)?\\s*R(?:ECS)?\\s+(\\d+)\\s+PG\\s+(\\d+)\\b").matcher(_TextDescription);
		while (matcherRefBookPage.find()) {
			addDataSet(vectorSDS, "", "", "", "", "", matcherRefBookPage.group(1) + "-" + matcherRefBookPage.group(2), "", "", "", "", "", "", "", "", "");
		}
		
		String a[] ={"X", "L", "C", "D", "M"};
		_TextDescription = Roman.normalizeRomanNumbersExceptTokens(_TextDescription,a);
		
		Set<String> allLots = prepareRawLotAndAdd(_LotIdentifier, null);
		
		//remove dimensions (e.g. '93 X 184.3' from 'COLONIAL ACRES, SEC. 3 PB 4 PG 29 LOT 233 93 X 184.3 IRR' on TN Sumner 159A-J-021.00)
		_TextDescription = _TextDescription.replaceAll("\\b" + GenericFunctions2.FLOAT_NUMBER_PATTERN + "\\s*[xX]\\s*" + GenericFunctions2.FLOAT_NUMBER_PATTERN + "\\b", "");
		
		String LotPattern1 = "(?is)\\bLOT\\s+[NSEW]\\s*\\d+\\s*FT\\s+OF\\s*(\\d+)(?:\\s*&\\s*[NSEW]\\s*\\d+\\s*FT\\s+OF\\s*(\\d+))?\\b";
		Matcher matcherLot1 = Pattern.compile(LotPattern1).matcher(_TextDescription);
		while (matcherLot1.find()) {
			
			String inputLot = matcherLot1.group(1);
			
			prepareRawLotAndAdd(inputLot, allLots);
			
			inputLot = matcherLot1.group(2);
			if (matcherLot1.group(2)!=null) {
				prepareRawLotAndAdd(inputLot, allLots);
			}
			_TextDescription = _TextDescription.replace(matcherLot1.group(0), "");
		}
		
		String partialTextDescription = _TextDescription;
		// remove subdiv name from legal desc when getting all lots to avoid parsing letters from the subdivision name;
		// e.g. PIN 300716-00570000-0020 FL Gilchrist K K L & S ESTATES
		if (org.apache.commons.lang.StringUtils.isNotBlank(_SubdivisionIdentifier) && org.apache.commons.lang.StringUtils.isNotBlank(_TextDescription)) {
			if (org.apache.commons.lang.StringUtils.containsIgnoreCase(partialTextDescription, _SubdivisionIdentifier)) {
				partialTextDescription = partialTextDescription.replaceFirst("(?i)" + _SubdivisionIdentifier, "");
			} else if (org.apache.commons.lang.StringUtils.containsIgnoreCase(_TextDescription.replaceAll("\\s+", ""),
					_SubdivisionIdentifier.replaceAll("\\s+", ""))) {
				Pattern pattern = Pattern.compile("(?is)(\\bP?LOT(?:S|D\\b)?)\\s*(.+)");
				Matcher matcher = pattern.matcher(_TextDescription);
				if (matcher.find()) {
					int subdivIdentifierIndex = matcher.group(2).indexOf(_SubdivisionIdentifier.trim().substring(0, 1));
					if (subdivIdentifierIndex > 0) {
						partialTextDescription = matcher.group(1)
								+ matcher.group(2).substring(0, subdivIdentifierIndex);
					}
				}
			}
		}

		Matcher matcherLot2 = Pattern.compile(
				"(?is)\\bP?LOT(?:S|D\\b)?\\s*:?\\s*(([A-Z]|\\d+[A-Z]*)(?:-([A-Z]|\\d+))?(\\s*[,&+.\\s]\\s*([A-Z]|\\d+)(?:-([A-Z]|\\d+))?)*\\b)")
				.matcher(partialTextDescription);
		while (matcherLot2.find()) {
			prepareRawLotAndAdd(matcherLot2.group(1), allLots);
			_TextDescription = _TextDescription.replace(matcherLot2.group(0), "@@@@@");
		}
	
		_LotIdentifier = LegalDescription.cleanValues(org.apache.commons.lang.StringUtils.join(allLots, " "), false, true);
		
		Matcher matcherBlock = Pattern.compile("(?is)\\bBL?O?C?K\\s*:?\\s*([A-Z0-9-]{1,2})\\b").matcher(_TextDescription);
		StringBuilder newBlock = new StringBuilder(); 
		while (matcherBlock.find()) {
			newBlock.append(matcherBlock.group(1)).append(" ");
			_TextDescription = _TextDescription.replace(matcherBlock.group(0), "@@@@@");
		}
		if (newBlock.length()>0) {
			_BlockIdentifier = concatenateLegalValues(_BlockIdentifier, newBlock.toString().replaceAll("[,&+.]", " "));
		}
		
		Matcher matcherUnit = Pattern.compile("(?is)\\bUN(?:I?T)?(?:\\s+NO)?(?:\\s+|\\.\\s*)([A-Z0-9-]+)\\b").matcher(_TextDescription);
		StringBuilder newUnits = new StringBuilder(); 
		while (matcherUnit.find()) {
			String newUnit = matcherUnit.group(1);
			newUnits.append(newUnit).append(" ");
			_TextDescription = _TextDescription.replace(matcherUnit.group(0), "@@@@@");
		}
		subdivisionUnit = concatenateLegalValues(subdivisionUnit, newUnits.toString());
		
		//task 9719, TN Davidson 155100B03500CO
		if (CountyConstants.TN_Davidson==dataSite.getCountyId()) {
			if (StringUtils.isEmpty(_SubdivisionIdentifier) && !StringUtils.isEmpty(_TextDescription)) {
				int index = _TextDescription.lastIndexOf("@@@@@");
				if (index!=-1) {
					_SubdivisionIdentifier = _TextDescription.substring(index + "@@@@@".length());
					_SubdivisionIdentifier = _SubdivisionIdentifier.replaceFirst("(?is)\\bADD(ITIO)?N?|REP(L(AT)?)?|S/D|(RE)?SUB(DIVISION)?\\s*$", "").trim();	
				}
			}
		}
		
		//Voluntary and Involuntary Lien (for references)
		String link = "";
		try {
			link = getBaseLink() + "lien.aspx?_StreetAddress=" +  URLEncoder.encode(_StreetAddress, "UTF-8") + "&_City=" + URLEncoder.encode(_City, "UTF-8") + 
				"&_State=" + URLEncoder.encode(_State, "UTF-8") + "&_PostalCode=" + URLEncoder.encode(_PostalCode, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		String lienDetails = "";
		Node xmlLien = null;
		for (int i=0;i<3&&xmlLien==null;i++) {
			HttpSite3 lienDetailsPage = HttpManager3.getSite(getCurrentServerName(), searchId);
			try {
				lienDetails = ((ro.cst.tsearch.connection.http3.GenericCL)lienDetailsPage).getPageWithPost(link);
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
				HttpManager3.releaseSite(lienDetailsPage);
			}
			if (!StringUtils.isEmpty(lienDetails)) {
				lienDetails = lienDetails.replaceFirst("(?is)<!DOCTYPE\\b.*?>", "");
				try {
					xmlLien = parseXml(lienDetails);
				} catch (RuntimeException e) {
					logger.error("XML parsing exception", e);
				}
			}
		}
		
		PropertyI prop = Property.createEmptyProperty();
		
		ResultTable pis = (ResultTable)resultMap.get("PropertyIdentificationSet");
		if (pis!=null) {
			try {
					int lenSTR = pis.getLength();
					for (int i=0;i<lenSTR;i++) {
						addSTR(pis.getItem(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), "", i), 
							   pis.getItem(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), "", i), 
							   pis.getItem(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), "", i),
							   strs);
					}	
					
				} catch (Exception e) {}
		}
		
		PartyI party = new Party(PType.GRANTOR);
		StringBuilder sb = new StringBuilder();
		NodeList ownerNodes = xpathQuery(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/PROPERTY/PROPERTY_OWNER");
		String previousLastName = "";
		for (int i=0;i<ownerNodes.getLength();i++) {
			Node ownerNode = ownerNodes.item(i);
			String _OwnerName = XmlUtils.findNodeValue(ownerNode, "@_OwnerName");
			
			//CA Los Angeles 3386-019-008
			if (i==0 && ownerNodes.getLength()==2) {
				String nextOwn = XmlUtils.findNodeValue(ownerNodes.item(i+1), "@_OwnerName");
				if (nextOwn.matches("(?is)\\s*INDUSTRIES(?:\\s+INC\\.?)?")) {
					_OwnerName += " " + nextOwn;
					i++;
				}
			}
			
			if (_OwnerName.matches("(?is)\\w+\\Z")) {
				if (i+1 < ownerNodes.getLength()) {
					String nextOwn =  XmlUtils.findNodeValue(ownerNodes.item(i+1), "@_OwnerName");
					if (nextOwn.matches("(?is)\\w+\\s+(?:JR|SR)\\Z")) {
						_OwnerName += " & " + nextOwn;
						i++;
					}
				}
			} else  {
				Matcher matchOwn = Pattern.compile("(?is)((?:\\w+\\s+){2}\\w+)\\s+((?:\\w+\\s+){2})(\\w+)").matcher(_OwnerName);
				if (matchOwn.matches()) { //task 9444
					_OwnerName = matchOwn.group(1) + " / " + matchOwn.group(3) + " " + matchOwn.group(2).trim();
				} else {
					if (!NameUtils.isCompany(_OwnerName)) {
						matchOwn = Pattern.compile("(?is)([A-Z])\\s+([A-Z])\\s+([A-Z-]{2,})").matcher(_OwnerName);	//M C DAWSON-ZIMMERMAN, FL Lee 17-45-24-C1-00075.0360
						if (matchOwn.matches()) {
							_OwnerName = matchOwn.group(3) + " " + matchOwn.group(1) + " " + matchOwn.group(2);
						}
					}
				}
			}
			
			//O DORISIO JAMES B / ANGELINA, CO Pueblo 1-5-15-2-02-039
			if (i>0 && _OwnerName.matches("[A-Z-]+") && NameFactory.getInstance().isFirstMiddle(_OwnerName)  && !StringUtils.isEmpty(previousLastName)) {
				_OwnerName = previousLastName + " " + _OwnerName;
			}
			
			if (!NameUtils.isCompany(_OwnerName)) {
				//IL Kane:15-11-276-032 WILSON DAVID E HEATHER K
				Matcher maTwoNames = Pattern.compile("(?is)^([A-Z]+\\s+[A-Z]+\\s+[A-Z])\\s+([A-Z]+(?:\\s+[A-Z])?)$").matcher(_OwnerName);
				if (maTwoNames.matches()) {
					String g2 = maTwoNames.group(2);
					boolean good = true;
					if (g2.matches(GenericFunctions1.nameSuffixString)||g2.matches(GenericFunctions1.nameTypeString)||g2.matches(GenericFunctions1.nameOtherTypeString)) {
						good = false;
					}
					if (good) {
						_OwnerName = maTwoNames.group(1) + " & " + g2;
					}
				}
			}
			
			//CA San Benito 055-050-020-000
			Matcher maProp = Pattern.compile("(?is)(.+?)\\s+PROP$").matcher(_OwnerName);
			if (maProp.find()) {
				String newName = maProp.group(1);
				if (!NameUtils.isCompany(newName)) {
					_OwnerName = newName;
				}
			}
			
			sb.append(_OwnerName).append(" / ");
			ResultMap m = new ResultMap();
			m.put("tmpOwnerFullName", _OwnerName);
			try {
				GenericFunctions1.partyNamesMax2(m, searchId);
				ResultTable parties = (ResultTable)m.get("PartyNameSet");
				if (parties!=null) {
					for (int j=0;j<parties.getLength();j++) {
						Name name = new Name();
		    			String lastName = parties.getItem(PartyNameSetKey.LAST_NAME.getShortKeyName(), "", j);
		    			if (j==0) {
		    				previousLastName = lastName;
		    			} else if (!lastName.equals(previousLastName)) {
		    				previousLastName = "";
		    			}
						name.setLastName(lastName);
		    			name.setFirstName(parties.getItem(PartyNameSetKey.FIRST_NAME.getShortKeyName(), "", j));
		    			name.setMiddleName(parties.getItem(PartyNameSetKey.MIDDLE_NAME.getShortKeyName(), "", j));
		    			name.setSufix(parties.getItem(PartyNameSetKey.SUFFIX.getShortKeyName(), "", j));
		    			name.setNameType(parties.getItem(PartyNameSetKey.TYPE.getShortKeyName(), "", j));
		    			name.setNameOtherType(parties.getItem(PartyNameSetKey.OTHER_TYPE.getShortKeyName(), "", j));
		    			name.setCompany(parties.getItem(PartyNameSetKey.IS_COMPANY.getShortKeyName(), "", j).length()>0?true:false);
		    			party.add(name);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		String str = sb.toString();
		str = str.replaceAll(" / $", "");
		party.setFreeParsedForm(str);
		prop.setOwner(party);
		
		if (!StringUtils.isEmpty(_StandardizedHouseNumber)) {
			streetName = _StandardizedHouseNumber + " " + streetName;
		}
		AddressI adr = new Address();
		adr = Bridge.fillAddressI(adr, streetName);
		adr.setCity(_City);
    	adr.setZip(zip);
    	adr.setCounty(_County);
    	adr.setState(_State);
    	adr.setFreeform(_StreetAddress);	    		    	
    	prop.setAddress(adr);
		
		String instrOHFranklin = new String();
		if (dataSite.getCountyId() == CountyConstants.OH_Franklin) {
			instrOHFranklin = GenericFunctions2.formatPIDforNB_OHFranklin(_AssessorsParcelIdentifier);
		}
		if (dataSite.getCountyId() == CountyConstants.TX_Collin) {
			// on TX Collin swap _AssessorsParcelIdentifier with _AssessorsAlternateParcelIdentifier
			String temp = _AssessorsParcelIdentifier;
			_AssessorsParcelIdentifier = _AssessorsAlternateParcelIdentifier;
			_AssessorsAlternateParcelIdentifier = temp;
		}

		PinI pin = new Pin();
		if (!instrOHFranklin.isEmpty()) {
			pin.addPin(PinI.PinType.PID, instrOHFranklin);
		}
		else {
			pin.addPin(PinI.PinType.PID, _AssessorsParcelIdentifier);
		}
    	pin.addPin(PinI.PinType.PID_ALT1, _AssessorsAlternateParcelIdentifier);
    	prop.setPin(pin);
    	
    	Legal legal = new Legal();
		legal.setFreeForm(_TextDescription);
	 	Subdivision subdiv = new Subdivision();
	 	if (_SubdivisionIdentifier.matches("(?is)^CITY/.*")) {
	 		_SubdivisionIdentifier = "";
	 	}
	 	if (!StringUtils.isEmpty(_SubdivisionIdentifier)) {
	 		subdiv.setName(_SubdivisionIdentifier);
	 	}
	 	subdiv.setSection(section);
		subdiv.setLot(_LotIdentifier);
		subdiv.setBlock(_BlockIdentifier);
		subdiv.setUnit(subdivisionUnit);
		subdiv.setPhase(subdivisionPhase);
		subdiv.setTract(_TractNumberIdentifier);
		subdiv.setNcbNumber(ncbNo);
		
		String[] pbpp = platBookPages.split(" ");
		if (pbpp.length>0) {
			String firstPBPP = pbpp[0];
			String[] spl = firstPBPP.split("@@@@@");
			if (spl.length==2) {
				subdiv.setPlatBook(spl[0]);
				subdiv.setPlatPage(spl[1]);
			}
		}
		
		legal.setSubdivision(subdiv);
		
		List<String> platBooks = new ArrayList<String>();
		List<String> platPages = new ArrayList<String>();
		for (int i=1;i<pbpp.length;i++) {
			String nextPBPP = pbpp[i];
			String[] spl = nextPBPP.split("@@@@@");
			if (spl.length==2) {
				platBooks.add(spl[0]);
				platPages.add(spl[1]);
			}
		}
		
		TownShip twn =  new TownShip();
		if (strs.size()>=1) {
			twn.setSection(strs.get(0).getSection());
			twn.setTownship(strs.get(0).getTownship());
			twn.setRange(strs.get(0).getRange());
		} else {
			if(org.apache.commons.lang.StringUtils.isNotBlank(section)) {
				twn.setSection(section);
			}
		}
		twn.setAbsNumber(absNo);
		legal.setTownShip(twn);
		
		prop.setLegal(legal);
    			
		if (!StringUtils.isEmpty(subdivisionCond)) {
			prop.setType(PropertyI.PType.CONDO);
		} else {
			prop.setType(PropertyI.PType.GENERIC);
		}
		
		if(org.apache.commons.lang.StringUtils.isNotBlank(_TextDescription)) {
			SimpleParseTokenResult acresNoFromLegal = GenericFunctions2.getAcresFromLegal(Integer.parseInt(getSearch().getStateId()), _TextDescription);
			if(acresNoFromLegal != null && org.apache.commons.lang.StringUtils.isNotBlank(acresNoFromLegal.getTokenParsed())) {
				try {
					prop.setAcres(Double.parseDouble(acresNoFromLegal.getTokenParsed()));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
				_TextDescription = acresNoFromLegal.getFinalSource();
			}
		}
		
		
    	//last sale
    	addDataSet(vectorSDS, _LastSalesDate, _LastRecordingDate, _LastSalesPriceAmount, SaleDataSetKey.SALES_PRICE.getShortKeyName(), 
    			_InstrumentNumber, _BookPage, bookType, _DocumentNumberIdentifier, _DeedTypeDescription, _SellerName, "", "", "", "", "");
    	
    	//prior sale
    	addDataSet(vectorSDS, _PriorSaleDate, _PriorRecordingDate, _PriorSalePriceAmount, SaleDataSetKey.SALES_PRICE.getShortKeyName(), 
    			_PriorInstrumentNumber, _PriorBookPage, "", _PriorDocumentNumberIdentifier, _PriorDeedTypeDescription, "", "", "", "", "", "");
    	
    	//first mortgage
    	addDataSet(vectorSDS, "", "", _FirstMortgageAmount, SaleDataSetKey.MORTGAGE_AMOUNT.getShortKeyName(), 
    			_FirstMortgageInstrumentNumber, _FirstMortgageBookPage, "", _FirstMortgageDocumentNumber, _FirstMortgageTypeDescription, "", "", "", "", "", "");
    	    	
    	//owner transfer
    	addDataSet(vectorSDS, _TransferSaleDate, _TransferRecordingDate, _TransferSalePrice, SaleDataSetKey.SALES_PRICE.getShortKeyName(), 
    			_TransferInstrumentNumber, _TransferBookPage, "", _TransferDocumentNumber, _TransferDeedTypeDescription, "", "", "", "", "", "");
    	
    	String _Apn = XmlUtils.findNodeValue(xmlLien, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/REPORT/REPORT_RESULT/VOLUNTARY_AND_INVOLUNTARY_LIEN/PROPERTY_INFORMATION/@_Apn");
    	if (_AssessorsParcelIdentifier.equals(_Apn)) {
    		
    		//these might be empty in responses from Subject Property Search and Range Search response, but not empty in response from Voluntary and Involuntary Lien 
    		String _AssessmentYear2 = XmlUtils.findNodeValue(xmlLien, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/REPORT/REPORT_RESULT/VOLUNTARY_AND_INVOLUNTARY_LIEN/ASSESSOR_INFORMATION/@_AssessmentYear");
    		String _TotalAssessedValueAmount2 = XmlUtils.findNodeValue(xmlLien, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/REPORT/REPORT_RESULT/VOLUNTARY_AND_INVOLUNTARY_LIEN/ASSESSOR_INFORMATION/@_TotalAssessedValue");
    		String _ImprovementValueAmount2 = XmlUtils.findNodeValue(xmlLien, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/REPORT/REPORT_RESULT/VOLUNTARY_AND_INVOLUNTARY_LIEN/ASSESSOR_INFORMATION/@_TotalImprovementValue");
    		String _LandValueAmount2 = XmlUtils.findNodeValue(xmlLien, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/REPORT/REPORT_RESULT/VOLUNTARY_AND_INVOLUNTARY_LIEN/ASSESSOR_INFORMATION/@_LandAssessmentValue");
    		String _TotalTaxableValueAmount2 = XmlUtils.findNodeValue(xmlLien, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/REPORT/REPORT_RESULT/VOLUNTARY_AND_INVOLUNTARY_LIEN/ASSESSOR_INFORMATION/@_TotalTaxableValue");
    		String _TaxYear2 = XmlUtils.findNodeValue(xmlLien, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/REPORT/REPORT_RESULT/VOLUNTARY_AND_INVOLUNTARY_LIEN/ASSESSOR_INFORMATION/@_TaxYear");
    		String _RealEstateTotalTaxAmount2 = XmlUtils.findNodeValue(xmlLien, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/REPORT/REPORT_RESULT/VOLUNTARY_AND_INVOLUNTARY_LIEN/ASSESSOR_INFORMATION/@_PropertyTaxAmount");
    		
    		DecimalFormat df = new DecimalFormat("$#,###.###");
    		
    		if (StringUtils.isEmpty(_AssessmentYear) && !StringUtils.isEmpty(_AssessmentYear2)) {
    			modifiedSubjectStyleSheet = modifiedSubjectStyleSheet.replaceFirst("(?is)<span\\s+id=\"_AssessmentYear\">.*?</span>", _AssessmentYear2);
    			_AssessmentYear = _AssessmentYear2;
    		}
    		if (StringUtils.isEmpty(_TotalAssessedValueAmount) && !StringUtils.isEmpty(_TotalAssessedValueAmount2)) {
    			modifiedSubjectStyleSheet = modifiedSubjectStyleSheet.replaceFirst("(?is)<span\\s+id=\"_TotalAssessedValueAmount\">.*?</span>", 
    					Matcher.quoteReplacement(df.format(Float.parseFloat(_TotalAssessedValueAmount2))));
    			_TotalAssessedValueAmount = _TotalAssessedValueAmount2;
    		}
    		if (StringUtils.isEmpty(_ImprovementValueAmount) && !StringUtils.isEmpty(_ImprovementValueAmount2)) {
    			modifiedSubjectStyleSheet = modifiedSubjectStyleSheet.replaceFirst("(?is)<span\\s+id=\"_ImprovementValueAmount\">.*?</span>", 
    					Matcher.quoteReplacement(df.format(Float.parseFloat(_ImprovementValueAmount2))));
    			_ImprovementValueAmount = _ImprovementValueAmount2;
    		}
    		if (StringUtils.isEmpty(_LandValueAmount) && !StringUtils.isEmpty(_LandValueAmount2)) {
    			modifiedSubjectStyleSheet = modifiedSubjectStyleSheet.replaceFirst("(?is)<span\\s+id=\"_LandValueAmount\">.*?</span>", 
    					Matcher.quoteReplacement(df.format(Float.parseFloat(_LandValueAmount2))));
    			_LandValueAmount = _LandValueAmount2;
    		}
    		if (StringUtils.isEmpty(_TotalTaxableValueAmount) && !StringUtils.isEmpty(_TotalTaxableValueAmount2)) {
    			modifiedSubjectStyleSheet = modifiedSubjectStyleSheet.replaceFirst("(?is)<span\\s+id=\"_TotalTaxableValueAmount\">.*?</span>", 
    					Matcher.quoteReplacement(df.format(Float.parseFloat(_TotalTaxableValueAmount2))));
    			_TotalTaxableValueAmount = _TotalTaxableValueAmount2;
    		}
    		if (StringUtils.isEmpty(_TaxYear) && !StringUtils.isEmpty(_TaxYear2)) {
    			modifiedSubjectStyleSheet = modifiedSubjectStyleSheet.replaceFirst("(?is)<span\\s+id=\"_TaxYear\">.*?</span>", _TaxYear2);
    			_TaxYear = _TaxYear2;
    		}
    		if (StringUtils.isEmpty(_RealEstateTotalTaxAmount) && !StringUtils.isEmpty(_RealEstateTotalTaxAmount2)) {
    			modifiedSubjectStyleSheet = modifiedSubjectStyleSheet.replaceFirst("(?is)<span\\s+id=\"_RealEstateTotalTaxAmount\">.*?</span>", 
    					Matcher.quoteReplacement(df.format(Float.parseFloat(_RealEstateTotalTaxAmount2))));
    			_RealEstateTotalTaxAmount = _RealEstateTotalTaxAmount2;
    		}
    		
    		NodeList mortgageNodes = xpathQuery(xmlLien, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/REPORT/REPORT_RESULT/VOLUNTARY_AND_INVOLUNTARY_LIEN/PROPERTY_LIEN_INFORMATION/PROPERTY_LIEN_INFO/MORTGAGE_LIEN_INFO");
        	for (int i=0;i<mortgageNodes.getLength();i++) {
    			Node node = mortgageNodes.item(i);
    			String _MortgageAmnt = XmlUtils.findNodeValue(node, "@_MortgageAmnt");
    			String _MortgageOriginationDate = XmlUtils.findNodeValue(node, "@_MortgageOriginationDate");
    			String _RecordingDate = XmlUtils.findNodeValue(node, "@_RecordingDate");
    			String _BookNbr = XmlUtils.findNodeValue(node, "@_BookNbr");
    			String _PageNbr = XmlUtils.findNodeValue(node, "@_PageNbr");
    			String _DocumentNbr = XmlUtils.findNodeValue(node, "@_DocumentNbr");
    			String _DocumentType = XmlUtils.findNodeValue(node, "@_DocumentType");
    			String _FinanceHistBookNumber = XmlUtils.findNodeValue(node, "@_FinanceHistBookNumber");
    			String _FinanceHistPageNumber = XmlUtils.findNodeValue(node, "@_FinanceHistPageNumber");
    			String bookPage = "";
    			if (!StringUtils.isEmpty(_BookNbr) && !StringUtils.isEmpty(_PageNbr)) {
    				bookPage =  _BookNbr + "-" + _PageNbr;
    			}
    			String financeRecDate = "";
    			if (org.apache.commons.lang.StringUtils.isNotEmpty(_FinanceHistBookNumber) && org.apache.commons.lang.StringUtils.isNotEmpty(_FinanceHistPageNumber)){
    				financeRecDate = _RecordingDate;
    				_RecordingDate = "";
    			}
    			addDataSet(vectorSDS, _MortgageOriginationDate, _RecordingDate, _MortgageAmnt, SaleDataSetKey.MORTGAGE_AMOUNT.getShortKeyName(), 
    	    			"", bookPage, "", _DocumentNbr, _DocumentType, "", "", "", _FinanceHistBookNumber, _FinanceHistPageNumber, financeRecDate);
        	}
        	
        	NodeList involuntaryLienNodes = xpathQuery(xmlLien, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/REPORT/REPORT_RESULT/VOLUNTARY_AND_INVOLUNTARY_LIEN/PROPERTY_LIEN_INFORMATION/PROPERTY_LIEN_INFO/INVOLUNTARY_LIEN_INFO");
        	for (int i=0;i<involuntaryLienNodes.getLength();i++) {
    			Node node = involuntaryLienNodes.item(i);
    			String _LienAmount = XmlUtils.findNodeValue(node, "@_LienAmount");
    			String _DocFilingDate = XmlUtils.findNodeValue(node, "@_DocFilingDate");
    			String _RecordingDate = XmlUtils.findNodeValue(node, "@_RecordingDate");
    			String _BookNbr = XmlUtils.findNodeValue(node, "@_BookNbr");
    			String _PageNbr = XmlUtils.findNodeValue(node, "@_PageNbr");
    			String _DocumentNbr = XmlUtils.findNodeValue(node, "@_DocumentNbr");
    			String _DocumentType = XmlUtils.findNodeValue(node, "@_DocumentType");
    			String bookPage = "";
    			if (!StringUtils.isEmpty(_BookNbr) && !StringUtils.isEmpty(_PageNbr)) {
    				bookPage =  _BookNbr + "-" + _PageNbr;
    			}
    			addDataSet(vectorSDS, _DocFilingDate, _RecordingDate, _LienAmount, SaleDataSetKey.SALES_PRICE.getShortKeyName(), 
    	    			"", bookPage, "", _DocumentNbr, _DocumentType, "", "", "", "", "", "");
        	}
        	
        	NodeList saleNodes = xpathQuery(xmlLien, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/REPORT/REPORT_RESULT/VOLUNTARY_AND_INVOLUNTARY_LIEN/PROPERTY_LIEN_INFORMATION/PROPERTY_LIEN_INFO/SALE_LIEN_INFO");
        	for (int i=0;i<saleNodes.getLength();i++) {
        		Node node = saleNodes.item(i);
    			String _SellerName_ = XmlUtils.findNodeValue(node, "@_SellerName");
    			String _BuyerName = XmlUtils.findNodeValue(node, "@_BuyerName");
    			String _SalePrice = XmlUtils.findNodeValue(node, "@_SalePrice");
    			String _SaleDate = XmlUtils.findNodeValue(node, "@_SaleDate");
    			String _RecordingDate = XmlUtils.findNodeValue(node, "@_RecordingDate");
    			String _BookNbr = XmlUtils.findNodeValue(node, "@_BookNbr");
    			String _PageNbr = XmlUtils.findNodeValue(node, "@_PageNbr");
    			String _DocumentNbr = XmlUtils.findNodeValue(node, "@_DocumentNbr");
    			String _DocumentType = XmlUtils.findNodeValue(node, "@_DocumentType");
    			String bookPage = "";
    			if (!StringUtils.isEmpty(_BookNbr) && !StringUtils.isEmpty(_PageNbr)) {
    				bookPage =  _BookNbr + "-" + _PageNbr;
    			}
    			addDataSet(vectorSDS, _SaleDate, _RecordingDate, _SalePrice, SaleDataSetKey.SALES_PRICE.getShortKeyName(), 
    	    			"", bookPage, "", _DocumentNbr, _DocumentType, _SellerName_, _BuyerName, "", "", "", "");
        	}
        	
        }
    	
    	for (SaleDataSet newSDS: legalDescBookPageSDS) {
    		String bk = newSDS.getAtribute(SaleDataSetKey.BOOK.getShortKeyName());
    		String pg = newSDS.getAtribute(SaleDataSetKey.PAGE.getShortKeyName());
    		String type = newSDS.getAtribute(SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName());
    		String recDate = newSDS.getAtribute(SaleDataSetKey.RECORDED_DATE.getShortKeyName());
    		addDataSet(vectorSDS, "", recDate, "", "", "", bk + "-" + pg, "", "", type, "", "", "", "", "", "");
    	}
    	
    	for (SaleDataSet newSDS: legalDescInstrNoSDS) {
    		String instNo = newSDS.getAtribute(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName());
    		String type = newSDS.getAtribute(SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName());
    		String recDate = newSDS.getAtribute(SaleDataSetKey.RECORDED_DATE.getShortKeyName());
    		addDataSet(vectorSDS, "", recDate, "", "", instNo, "", "", "", type, "", "", "", "", "", "");
    	}
    	
    	//the values in possibleSDS can be either year-instrNo (1997-2099, e.g. FL Baker 073S22009500000270)
    	//or book-page (22936-4633, e.g. FL Miami-Dade 35-3020-057-3590)
    	for (SaleDataSet newSDS: possibleSDS) {
    		String val1 = newSDS.getAtribute(SaleDataSetKey.RECORDED_DATE.getShortKeyName());
    		String val2 = newSDS.getAtribute(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName());
    		boolean found = false;
    		for (SaleDataSet each: vectorSDS) {
    			if (val1.equals(each.getAtribute(SaleDataSetKey.BOOK.getShortKeyName()))&&val2.equals(each.getAtribute(SaleDataSetKey.PAGE.getShortKeyName()))) {
    				found = true;		//found as book-page
    				break;
    			} else {
    				if (val2.equals(each.getAtribute(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName()))) {
    					String instrDate = each.getAtribute(SaleDataSetKey.INSTRUMENT_DATE.getShortKeyName());
    					if (instrDate.length()>3) {
    						if (val1.equals(instrDate.substring(0, 4))) {
    							found = true;		//found as year-instrNo
    							break;
    						}
    					}
    				}
    			}
    		}
    		if (!found) {
    			int year = -1;
    			try {
    				year = Integer.parseInt(val1);
    				
    			} catch (NumberFormatException nfe) {}
    			if (year<Calendar.getInstance().get(Calendar.YEAR) && year>1900) {	//consider it to be year-instrNo
    				addDataSet(vectorSDS, "", val1, "", "",val2, "", "", "", "", "", "", "", "", "", "");
    			} else {															//consider it to be book-page
    				addDataSet(vectorSDS, "", "", "", "", "", val1 + "-" + val2, "", "", "", "", "", "", "", "", "");
    			}
    		}
    	}
    	
    	correctDataSet(vectorSDS);
    	vectorSDS = removeDuplicates(vectorSDS);
    	
    	Vector<SaleDataSet> newVectorSDS = new Vector<SaleDataSet>();
    	for (SaleDataSet saleDataSet: vectorSDS) {
    		ResultMap resultMapSDS = new ResultMap();
    		@SuppressWarnings("unchecked")
			Iterator<String> it = saleDataSet.keyIterator();
    		while (it.hasNext()) {
    			String s = it.next();
    			resultMapSDS.put("SaleDataSet." + s, saleDataSet.getAtribute(s));
    		}
    		GenericDASLNDBFunctions.improveCrossRefsParsing(resultMapSDS, getSearch());
    		SaleDataSet newSaleDataSet = new SaleDataSet();
    		@SuppressWarnings({ "unchecked", "rawtypes" })
			Iterator<Entry> newIt = resultMapSDS.entrySetIterator();
    		while (newIt.hasNext()) {
    			@SuppressWarnings("rawtypes")
				Entry e = (Entry)newIt.next();
    			String key = (String)e.getKey();
    			key = key.replaceFirst(".*\\.", "");
    			newSaleDataSet.setAtribute(key, (String)e.getValue());
    		}
    		newVectorSDS.add(newSaleDataSet);
    	}
    	
		InstrumentI instr = new Instrument();
		if (!instrOHFranklin.isEmpty()) {
			instr.setInstno(instrOHFranklin);
		}
		else {
			instr.setInstno(_AssessorsParcelIdentifier);
		}
    	
    	instr.setDocType("ASSESSOR");
    	instr.setDocSubType("ASSESSOR");
    	int year = -1;
    	try {
    		year = Integer.parseInt(_AssessmentYear);
    	} catch (NumberFormatException nfe) {}
    	if (year!=-1) {
    		instr.setYear(year);
    	} else {
    		instr.setYear(Calendar.getInstance().get(Calendar.YEAR));
    	}
    	
    	AssessorDocument assessDoc = new AssessorDocument(DocumentsManager.generateDocumentUniqueId(searchId, instr));
    	assessDoc.setInstrument(instr);
    	
    	if (!StringUtils.isEmpty(_TotalAssessedValueAmount)){
	    	try {
	    		assessDoc.setTotalAssessement(Double.parseDouble(_TotalAssessedValueAmount));	
	    	} catch (NumberFormatException e){}	
    	}
    	
    	if (!StringUtils.isEmpty(_RealEstateTotalTaxAmount)){
	    	try {
	    		assessDoc.setTotalEstimatedTaxes(Double.parseDouble(_RealEstateTotalTaxAmount));	
	    	} catch (NumberFormatException e){}
    	}
    	
    	Set<RegisterDocumentI> trans = Bridge.getTransactioHistory(parsedResponse, newVectorSDS, searchId);
	    if(trans!=null){
	    	assessDoc.setTransactionHistory(trans);
	    	for (RegisterDocumentI c:trans){
    			assessDoc.addParsedReference(c.getInstrument());
			}
	    }
    	
    	assessDoc.setServerDocType("ASSESSOR");        	
    	assessDoc.setType(SimpleChapterUtils.DType.ASSESOR);
    	
    	assessDoc.addProperty(prop);
    	for (int i=0;i<platBooks.size();i++) {
    		PropertyI newProp = Property.createEmptyProperty();
    		Legal newLegal = new Legal();
    		Subdivision newSubdiv = new Subdivision();
    	 	newSubdiv.setPlatBook(platBooks.get(i));
    	 	newSubdiv.setPlatPage(platPages.get(i));
    		newLegal.setSubdivision(newSubdiv);
    		TownShip newTwn =  new TownShip();
    		newLegal.setTownShip(newTwn);
    		newProp.setLegal(newLegal);
    		assessDoc.addProperty(newProp);
    	}
    	for (int i=1;i<strs.size();i++) {
    		PropertyI newProp = Property.createEmptyProperty();
    		Legal newLegal = new Legal();
    		Subdivision newSubdiv = new Subdivision();
    	 	newLegal.setSubdivision(newSubdiv);
    		TownShip newTwn =  new TownShip();
    		newTwn.setSection(strs.get(i).getSection());
    		newTwn.setTownship(strs.get(i).getTownship());
    		newTwn.setRange(strs.get(i).getRange());
    		newLegal.setTownShip(newTwn);
    		newProp.setLegal(newLegal);
    		assessDoc.addProperty(newProp);
    	}
    	
    	assessDoc.setDataSource(getDataSite().getSiteTypeAbrev());
    	assessDoc.setSearchType(searchType);
    	assessDoc.setSiteId(getServerID());
    	
    	boolean isParentSite = parsedResponse.isParentSite();
		if(isParentSite) {
			assessDoc.setSavedFrom(SavedFromType.PARENT_SITE);
		} else {
			assessDoc.setSavedFrom(SavedFromType.AUTOMATIC);
		}
		
		Response.getParsedResponse().setDocument(assessDoc);;
		
		instno.append(_AssessorsParcelIdentifier);
		
		try {
			String detailsHtml = applyTransformation(xmlDoc, modifiedSubjectStyleSheet);
			String referencesHtml = "";
			if (xmlLien!=null) {
				referencesHtml = "<br/><br/>" + applyTransformation(xmlLien, lienStyleSheet);
			}
			detailsHtml = "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"2\"><tr><td>" + detailsHtml + referencesHtml + "</td></tr></table>";
			Response.setResult(detailsHtml);
			return detailsHtml;
		} catch (RuntimeException e) {
			logger.error("Error while getting details", e);
		}
		
		return null;

	}

	/**
	 * Cleans the <code>inputLot</code> and adds it to the <code>allLots</code> hash<br>
	 * If <code>allLots</code> is null a new object is created
	 * @param inputLot
	 * @param allLots
	 * @return allLots
	 */
	protected Set<String> prepareRawLotAndAdd(String inputLot, Set<String> allLots) {
		String newLot = inputLot.replaceAll("[,&+.]", " ").trim().replaceAll("\\b0+([A-Z0-9-]*)\\b", "$1");
		if(allLots == null) {
			allLots = new LinkedHashSet<>();
		}
		if(!allLots.contains(newLot)) {
			if(!allLots.contains(newLot.replaceAll("-", ""))) {
				allLots.add(newLot);
			}
		} 
		
		return allLots;
	}
	
	public String changeInstrumentNumberFormat(String instrumentNumber, String date){
		
		if (dataSite.getStateFIPS().equals(StateContants.CA_STRING_FIPS)){
			if (org.apache.commons.lang.StringUtils.isNotEmpty(instrumentNumber) && org.apache.commons.lang.StringUtils.isNotEmpty(date)){
				if (date.length() > 4){
					String year = date.substring(0, 4);
					if (!instrumentNumber.startsWith(year + "-")){
						instrumentNumber = year + "-" + org.apache.commons.lang.StringUtils.stripStart(instrumentNumber, "0");
					}
				}
			}
		}
		
		return instrumentNumber;
	}
	public static void addSTR(String s, String t, String r, List<TownShip> strs) {
		s = s.replaceFirst("^0+", "");
		t = t.replaceFirst("^0+", "");
		r = r.replaceFirst("^0+", "");
		for (int i=0;i<strs.size();i++) {
			int res = compareSTR(strs.get(i), s, t, r);
			if (res==0 || res==1) {
				continue;
			} else if (res==1) {
				strs.get(i).setSection(s);
				strs.get(i).setTownship(t);
				strs.get(i).setRange(r);
			} else {
				TownShip township = new TownShip();
				township.setSection(s);
				township.setTownship(t);
				township.setRange(r);
				strs.add(township);
			}
		}
	}
	
	/**
	 * returns 0 if the structure is equal to the triplet
	 * returns 1 if the structure is equal to the triplet (only the structure has directions), e.g. 16/12S/21E vs. 16/12/21
	 * returns 2 if the structure is equal to the triplet (only the structure has directions), e.g. 16/12/21 vs. 16/12S/21E
	 * returns -1 if structure is different from the triplet
	 */
	public static int compareSTR(TownShip township, String s, String t, String r) {
		if (township.getSection().equals(s) && township.getTownship().equals(t) && township.getRange().equals(r)) {
			return 0;
		}
		if (township.getSection().equals(s) && township.getTownship().replaceFirst("(?is)[NESW]+$", "").equals(t) && township.getRange().replaceFirst("(?is)[NESW]+$", "").equals(r)) {
			return 1;
		}
		if (township.getSection().equals(s) && township.getTownship().equals(t.replaceFirst("(?is)[NESW]+$", "")) && township.getRange().equals(r.replaceFirst("(?is)[NESW]+$", ""))) {
			return 2;
		}
		return -1;
	} 
	
	public static String concatenateLegalValues(String value1, String value2) {
		return LegalDescription.cleanValues(value1.replaceAll("\\b0+([A-Z0-9-]+)\\b", "$1") + " " + value2.replaceAll("\\b0+([A-Z0-9-]+)\\b", "$1"), false, true);
	}
	
	public void addDataSet(Vector<SaleDataSet> vectorSDS, String instrDate, String recDate, String amount, String amountKey,	String instrNo, 
			String bp, String bookType, String docNo, String type, String gtor, String gtee, 
			String finInstrNo, String finBook, String finPage, String finRecDate) {
		
		if (StringUtils.isEmpty(instrDate) && StringUtils.isEmpty(recDate) && 
				StringUtils.isEmpty(amount)  && StringUtils.isEmpty(instrNo) && 
				StringUtils.isEmpty(bp) && StringUtils.isEmpty(bookType) &&
				StringUtils.isEmpty(docNo) && StringUtils.isEmpty(type) &&
				StringUtils.isEmpty(gtor) && StringUtils.isEmpty(gtee) &&
				StringUtils.isEmpty(finInstrNo) && StringUtils.isEmpty(finBook) && 
				StringUtils.isEmpty(finPage) && StringUtils.isEmpty(finRecDate)) {
			return;
		}
		
		String book = "";
		String page = "";
		String[] bookPage = new String[1];
    	String[] bookPageFromDoc = new String[1];
    	if (StringUtils.isNotEmpty(bp)) {
    		bookPage = bp.split("-");
    		if (bp.contains("-") && StringUtils.isNotEmpty(docNo)){
    			if (bp.trim().equalsIgnoreCase(docNo.trim())){//it means that is not a doc number, is only book-page
    				docNo = "";
    			}
    		}
    	}
    	if (StringUtils.isNotEmpty(docNo)) {
    		docNo = docNo.replaceFirst("^0+", "");
        	bookPageFromDoc = docNo.split("-");
        	if (bookPageFromDoc.length == 2){//it means that is not a doc number, is only book-page
        		docNo = "";
        	}
        }
    	if (bookPage.length==2) {
    		book = bookPage[0].replaceFirst("^0+", "");
    		page = bookPage[1].replaceFirst("^0+", "");
    	} else if (bookPageFromDoc.length==2) {
    		book = bookPageFromDoc[0].replaceFirst("^0+", "");
    		page = bookPageFromDoc[1].replaceFirst("^0+", "");
    	}
    	if (StringUtils.isNotEmpty(instrNo)) {
    		instrNo = instrNo.replaceFirst("^0+", "");
    	} else if (StringUtils.isNotEmpty(docNo)) {
    		if (!docNo.equals(book + "-" + page)) {
    			instrNo = docNo;
        	}
    	}
    	if (instrNo.equals(docNo)) {
    		docNo = "";
    	}
    	
    	if (StringUtils.isNotEmpty(finInstrNo)) {
    		finInstrNo = finInstrNo.replaceFirst("^0+", "");
    	}
    	    		
    	boolean bpNotEmpty = !StringUtils.isEmpty(book) && !StringUtils.isEmpty(page);
    	boolean finBPNotEmpty = !StringUtils.isEmpty(finBook) && !StringUtils.isEmpty(finPage);
    	boolean instrNoNotEmpty = !StringUtils.isEmpty(instrNo);
    	boolean finInstrNoNotEmpty = !StringUtils.isEmpty(finInstrNo);
    	
		SaleDataSet sds = new SaleDataSet();
		//search if this SaleDataSet was already added; if found, merge the information
		boolean alreadyAdded = false;
		for (SaleDataSet s: vectorSDS) {
			String existingBook = s.getAtribute(SaleDataSetKey.BOOK.getShortKeyName());
			String existingPage = s.getAtribute(SaleDataSetKey.PAGE.getShortKeyName());
			String existingFinBook = s.getAtribute(SaleDataSetKey.FINANCE_BOOK.getShortKeyName());
			String existingFinPage = s.getAtribute(SaleDataSetKey.FINANCE_PAGE.getShortKeyName());
			String existingInstNo = s.getAtribute(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName());
			String existingFinInstNo = s.getAtribute(SaleDataSetKey.FINANCE_INST_NO.getShortKeyName());
			if ((bpNotEmpty && book.equals(existingBook) && page.equals(existingPage)) || (instrNoNotEmpty && instrNo.equals(existingInstNo)) ||
					(finBPNotEmpty && finBook.equals(existingFinBook) && finPage.equals(existingFinPage)) || 
					(finInstrNoNotEmpty && finInstrNo.equals(existingFinInstNo))) {
				sds = s;
				alreadyAdded = true;
				break;
			}
		}
		
		String existingInstrDate = sds.getAtribute(SaleDataSetKey.INSTRUMENT_DATE.getShortKeyName());
		if (!StringUtils.isEmpty(instrDate) && StringUtils.isEmpty(existingInstrDate)) {
			instrDate = instrDate.replaceFirst("^(\\d{4})(\\d{2})(\\d{2})$", "$1-$2-$3");
			instrDate = instrDate.replaceFirst("^(\\d{2})/(\\d{2})/(\\d{4})$", "$3-$1-$2");
			sds.setAtribute(SaleDataSetKey.INSTRUMENT_DATE.getShortKeyName(), instrDate);
		}
		
		String existingRecDate = sds.getAtribute(SaleDataSetKey.RECORDED_DATE.getShortKeyName());
		//if existingRecDate is only the year
		if (!StringUtils.isEmpty(existingRecDate) && !StringUtils.isEmpty(recDate) && recDate.startsWith(existingRecDate)) {
			existingRecDate = "";
		}
    	if (!StringUtils.isEmpty(recDate) && StringUtils.isEmpty(existingRecDate)) {
    		recDate = recDate.replaceFirst("^(\\d{4})(\\d{2})(\\d{2})$", "$1-$2-$3");
    		recDate = recDate.replaceFirst("^(\\d{2})/(\\d{2})/(\\d{4})$", "$3-$1-$2");
    		sds.setAtribute(SaleDataSetKey.RECORDED_DATE.getShortKeyName(), recDate);
    	}
    	String existingFinRecDate = sds.getAtribute(SaleDataSetKey.FINANCE_RECORDED_DATE.getShortKeyName());
    	if (!StringUtils.isEmpty(finRecDate) && StringUtils.isEmpty(existingFinRecDate)) {
    		finRecDate = finRecDate.replaceFirst("^(\\d{4})(\\d{2})(\\d{2})$", "$1-$2-$3");
    		finRecDate = finRecDate.replaceFirst("^(\\d{2})/(\\d{2})/(\\d{4})$", "$3-$1-$2");
    		sds.setAtribute(SaleDataSetKey.FINANCE_RECORDED_DATE.getShortKeyName(), finRecDate);
    	}
    	
    	String existingAmount = sds.getAtribute(amountKey);
    	if (!StringUtils.isEmpty(amount) && StringUtils.isEmpty(existingAmount)) {
    		sds.setAtribute(amountKey, amount);
    	}
    	
    	String existingInstrNo = sds.getAtribute(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName());
    	if (!StringUtils.isEmpty(instrNo) && StringUtils.isEmpty(existingInstrNo)) {
    		instrNo = changeInstrumentNumberFormat(instrNo.replaceFirst("^0+", ""), recDate);
    		sds.setAtribute(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName(), instrNo);
    	}
    	
    	String existingBook = sds.getAtribute(SaleDataSetKey.BOOK.getShortKeyName());
    	String existingPage = sds.getAtribute(SaleDataSetKey.PAGE.getShortKeyName());
    	if (!StringUtils.isEmpty(book) && !StringUtils.isEmpty(page) && (StringUtils.isEmpty(existingBook) || StringUtils.isEmpty(existingPage))) {
    		sds.setAtribute(SaleDataSetKey.BOOK.getShortKeyName(), book);
    		sds.setAtribute(SaleDataSetKey.PAGE.getShortKeyName(), page);
    	}
    	
    	String existingBookType = sds.getAtribute(SaleDataSetKey.BOOK_TYPE.getShortKeyName());
    	if (!StringUtils.isEmpty(bookType) && StringUtils.isEmpty(existingBookType)) {
    		sds.setAtribute(SaleDataSetKey.BOOK_TYPE.getShortKeyName(), bookType);
    	}
    	
    	String existingDocNo = sds.getAtribute(SaleDataSetKey.DOCUMENT_NUMBER.getShortKeyName());
    	if (!StringUtils.isEmpty(docNo) && StringUtils.isEmpty(existingDocNo)) {
    		sds.setAtribute(SaleDataSetKey.DOCUMENT_NUMBER.getShortKeyName(), docNo);
    	}
    	
    	String existingType = sds.getAtribute(SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName());
    	if (!StringUtils.isEmpty(type) && StringUtils.isEmpty(existingType)) {
    		sds.setAtribute(SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName(), type);
    	}
    	
    	String existingGtor = sds.getAtribute(SaleDataSetKey.GRANTOR.getShortKeyName());
    	if (!StringUtils.isEmpty(gtor) && StringUtils.isEmpty(existingGtor)) {
    		sds.setAtribute(SaleDataSetKey.GRANTOR.getShortKeyName(), gtor);
    	}
    	
    	String existingGtee = sds.getAtribute(SaleDataSetKey.GRANTEE.getShortKeyName());
    	if (!StringUtils.isEmpty(gtee) && StringUtils.isEmpty(existingGtee)) {
    		sds.setAtribute(SaleDataSetKey.GRANTEE.getShortKeyName(), gtee);
    	}
    	
    	String existingFinInstrNo = sds.getAtribute(SaleDataSetKey.FINANCE_INST_NO.getShortKeyName());
    	if (!StringUtils.isEmpty(finInstrNo) && StringUtils.isEmpty(existingFinInstrNo)) {
    		instrNo = changeInstrumentNumberFormat(finInstrNo.replaceFirst("^0+", ""), existingFinInstrNo);
    		sds.setAtribute(SaleDataSetKey.FINANCE_INST_NO.getShortKeyName(), finInstrNo);
    	}
    	
    	String existingFinBook = sds.getAtribute(SaleDataSetKey.FINANCE_BOOK.getShortKeyName());
    	if (!StringUtils.isEmpty(finBook) && StringUtils.isEmpty(existingFinBook)) {
    		sds.setAtribute(SaleDataSetKey.FINANCE_BOOK.getShortKeyName(), finBook.replaceFirst("^0+", ""));
    	}
    	
    	String existingFinPage = sds.getAtribute(SaleDataSetKey.FINANCE_PAGE.getShortKeyName());
    	if (!StringUtils.isEmpty(finPage) && StringUtils.isEmpty(existingFinPage)) {
    		sds.setAtribute(SaleDataSetKey.FINANCE_PAGE.getShortKeyName(), finPage.replaceFirst("^0+", ""));
    	}
    	
    	if (!alreadyAdded) {
    		vectorSDS.add(sds);
    	}
    	
	}
	
	public static void correctDataSet(Vector<SaleDataSet> vectorSDS) {
		//correct wrong instrument date, e.g. 1996-07-00 (FL Baker 09-3S-22-0118-0001-0040) 
		//retain only the year, necessary for searching on DG
		for (SaleDataSet s: vectorSDS) {
			String instrDate = s.getAtribute(SaleDataSetKey.INSTRUMENT_DATE.getShortKeyName());
			if (instrDate!=null) {
				Matcher ma = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})").matcher(instrDate);
				if (ma.find()) {
					Date date = null;
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					try{
						sdf.setLenient(false);
						date = sdf.parse(instrDate);
					}catch(ParseException e){}
					if (date==null) {
						instrDate = ma.group(1);
						s.setAtribute(SaleDataSetKey.INSTRUMENT_DATE.getShortKeyName(), instrDate);
					}
				}
			}
		}
	}
	
	public Vector<SaleDataSet> removeDuplicates(Vector<SaleDataSet> vectorSDS) {
		Vector<SaleDataSet> newVectorSDS = new Vector<SaleDataSet>();
		for (SaleDataSet s: vectorSDS) {
			String amountKey = SaleDataSetKey.SALES_PRICE.getShortKeyName();
			String amount = s.getAtribute(amountKey);
			if (amount==null) {
				amountKey = SaleDataSetKey.MORTGAGE_AMOUNT.getShortKeyName();
				amount = s.getAtribute(amountKey);
			}
			String bp = "";
			String book = s.getAtribute(SaleDataSetKey.BOOK.getShortKeyName());
			String page = s.getAtribute(SaleDataSetKey.PAGE.getShortKeyName());
			if (!StringUtils.isEmpty(book) && !StringUtils.isEmpty(page)) {
				bp = book + "-" + page;
			}
			addDataSet(newVectorSDS, s.getAtribute(SaleDataSetKey.INSTRUMENT_DATE.getShortKeyName()), s.getAtribute(SaleDataSetKey.RECORDED_DATE.getShortKeyName()), 
					amount, amountKey, s.getAtribute(SaleDataSetKey.INSTRUMENT_NUMBER.getShortKeyName()), bp, s.getAtribute(SaleDataSetKey.BOOK_TYPE.getShortKeyName()),
					s.getAtribute(SaleDataSetKey.DOCUMENT_NUMBER.getShortKeyName()), s.getAtribute(SaleDataSetKey.DOCUMENT_TYPE.getShortKeyName()), 
					s.getAtribute(SaleDataSetKey.GRANTOR.getShortKeyName()), s.getAtribute(SaleDataSetKey.GRANTEE.getShortKeyName()), 
					s.getAtribute(SaleDataSetKey.FINANCE_INST_NO.getShortKeyName()), 
					s.getAtribute(SaleDataSetKey.FINANCE_BOOK.getShortKeyName()), s.getAtribute(SaleDataSetKey.FINANCE_PAGE.getShortKeyName()), s.getAtribute(SaleDataSetKey.FINANCE_RECORDED_DATE.getShortKeyName()));
		}
		return newVectorSDS;
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		
		Node xmlDoc = null;
		try {
			xmlDoc = parseXml(table);
		} catch (RuntimeException e) {
			logger.error("XML parsing exception", e);
			return intermediaryResponse;
		}
		
		NodeList rows = xpathQuery(xmlDoc, "/RESPONSE_GROUP/RESPONSE/RESPONSE_DATA/PROPERTY_INFORMATION_RESPONSE/_PROPERTY_INFORMATION/_MULTIPLE_RECORDS");
		
		String currentState = getDataSite().getStateAbbreviation();
		
		int len = rows.getLength();
		
		if (len>0) {
			
			String source = "NA";
			SearchType searchType = SearchType.valueOf(source);
			Object objectModuleSource = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
			if (objectModuleSource != null) {
				if (objectModuleSource instanceof TSServerInfoModule) {
					TSServerInfoModule moduleSource = (TSServerInfoModule) objectModuleSource;
					source = moduleSource.getSearchType();
					if (!"".equals(source)) {
						searchType = SearchType.valueOf(source);
					}
				}
			}
			
			for (int i=0;i<rows.getLength();i++) {
				
				Node row = rows.item(i);
				
				String _State = XmlUtils.findNodeValue(row, "PROPERTY/@_State");
				String _City = XmlUtils.findNodeValue(row, "PROPERTY/@_City");
				String _PostalCode = XmlUtils.findNodeValue(row, "PROPERTY/@_PostalCode");
				String _PlusFourPostalCode = XmlUtils.findNodeValue(row, "PROPERTY/@_PlusFourPostalCode");
				String _County = XmlUtils.findNodeValue(row, "PROPERTY/@_County");
				String _AssessorsParcelIdentifier = XmlUtils.findNodeValue(row, "PROPERTY/@_AssessorsParcelIdentifier");
				String _StandardizedHouseNumber = XmlUtils.findNodeValue(row, "PROPERTY/_PARSED_STREET_ADDRESS/@_StandardizedHouseNumber");
				String _StreetName = XmlUtils.findNodeValue(row, "PROPERTY/_PARSED_STREET_ADDRESS/@_StreetName");
				String _StreetSuffix = XmlUtils.findNodeValue(row, "PROPERTY/_PARSED_STREET_ADDRESS/@_StreetSuffix");
				String _ApartmentOrUnit = XmlUtils.findNodeValue(row, "PROPERTY/_PARSED_STREET_ADDRESS/@_ApartmentOrUnit");
				String _DirectionPrefix = XmlUtils.findNodeValue(row, "PROPERTY/_PARSED_STREET_ADDRESS/@_DirectionPrefix");
				String _DirectionSuffix = XmlUtils.findNodeValue(row, "PROPERTY/_PARSED_STREET_ADDRESS/@_DirectionSuffix");
				String _OwnerName = XmlUtils.findNodeValue(row, "PROPERTY/PROPERTY_OWNER/@_OwnerName");
				
				String streetName = org.apache.commons.lang.StringUtils.defaultString(_DirectionPrefix) + " " +
			            			org.apache.commons.lang.StringUtils.defaultString(_StreetName) + " " +
			            			org.apache.commons.lang.StringUtils.defaultString(_StreetSuffix) + " " +
			            			org.apache.commons.lang.StringUtils.defaultString(_DirectionSuffix) + " #" +
			            			org.apache.commons.lang.StringUtils.defaultString(_ApartmentOrUnit);
				streetName = streetName.replaceAll("\\s{2,}", " ");
				streetName = streetName.replaceFirst("#$", "").trim();
				
				ResultMap resultMap = new ResultMap();
				
				String zip = _PostalCode + ("".equals(_PlusFourPostalCode)?"":"-"+_PlusFourPostalCode);
				resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
				
				resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
				
				resultMap.put(PropertyIdentificationSetKey.CITY.getKeyName(), _City);
				String newCity = "";
						
				try {
					GenericFunctions1.correctZIPCode(resultMap, searchId);
					zip = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.ZIP.getKeyName()));
					
					GenericFunctions2.legalGenericDASLNDB(resultMap, searchId);
					streetName = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.STREET_NAME.getKeyName()));
					
					GenericFunctions2.removeCityAndZip(resultMap, searchId);
					newCity = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.CITY.getKeyName()));
					zip = org.apache.commons.lang.StringUtils.defaultString((String)resultMap.get(PropertyIdentificationSetKey.ZIP.getKeyName()));
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				String lastName = "";
				String firstName = "";
				PartyI party = new Party(PType.GRANTOR);
				party.setFreeParsedForm(_OwnerName);
				ResultMap m = new ResultMap();
				m.put("tmpOwnerFullName", _OwnerName);
				try {
					GenericFunctions1.partyNamesMax2(m, searchId);
					ResultTable parties = (ResultTable)m.get("PartyNameSet");
					if (parties!=null) {
						for (int j=0;j<parties.getLength();j++) {
							Name name = new Name();
			    			name.setLastName(parties.getItem(PartyNameSetKey.LAST_NAME.getShortKeyName(), "", j));
			    			name.setFirstName(parties.getItem(PartyNameSetKey.FIRST_NAME.getShortKeyName(), "", j));
			    			name.setMiddleName(parties.getItem(PartyNameSetKey.MIDDLE_NAME.getShortKeyName(), "", j));
			    			name.setSufix(parties.getItem(PartyNameSetKey.SUFFIX.getShortKeyName(), "", j));
			    			name.setNameType(parties.getItem(PartyNameSetKey.TYPE.getShortKeyName(), "", j));
			    			name.setNameOtherType(parties.getItem(PartyNameSetKey.OTHER_TYPE.getShortKeyName(), "", j));
			    			name.setCompany(parties.getItem(PartyNameSetKey.IS_COMPANY.getShortKeyName(), "", j).length()>0?true:false);
			    			party.add(name);
						}
						if (parties.getLength()>0) {
							lastName = parties.getItem(PartyNameSetKey.LAST_NAME.getShortKeyName(), "", 0);
							firstName = parties.getItem(PartyNameSetKey.FIRST_NAME.getShortKeyName(), "", 0);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				ParsedResponse currentResponse = new ParsedResponse();
				
				String link = CreatePartialLink(TSConnectionURL.idPOST) + "/details.aspx?_AssessorsParcelIdentifier=" + _AssessorsParcelIdentifier + 
						("".equals(_StandardizedHouseNumber)?"":"&_StandardizedHouseNumber=" + _StandardizedHouseNumber) +
						("".equals(_StreetName)?"":"&_StreetName=" + _StreetName) +
						("".equals(lastName)?"":"&_OwnerLastName=" + lastName) +
						("".equals(firstName)?"":"&_OwnerFirstName=" + firstName) +
						"&source=" + source;
				
				currentResponse.setPageLink(new LinkInPage(link, link,	TSServer.REQUEST_SAVE_TO_TSD));

				String apn = "<td align=\"center\"><a href=\"" + link + "\">" + _AssessorsParcelIdentifier + "</a></td>";
				String addressLine1 = "";
				if (!StringUtils.isEmpty(_StandardizedHouseNumber)) {
					addressLine1 = _StandardizedHouseNumber + " " + streetName;
				} else {
					addressLine1 = streetName;
				}
				String addressLine2 = _City + ", " + _State + " " + _PostalCode + "-" + _PlusFourPostalCode;
				addressLine2 = addressLine2.replaceFirst("^\\s*,\\s*", "");
				addressLine2 = addressLine2.replaceFirst("\\s*-\\s*$", "");
				addressLine2 = addressLine2.replaceAll("\\s{2,}", " ").trim();
				if (currentState.equals(addressLine2)) {
					addressLine2 = "";
				}
				String address = "<td align=\"center\">" + addressLine1 + ("".equals(addressLine2)?"":"<br/>"+addressLine2) + "</td>";
				String owner = "<td align=\"center\">" + _OwnerName + "</td>";
				String htmlRow = "<tr>" + apn + address + owner + "</tr>";
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, htmlRow);
				currentResponse.setOnlyResponse(htmlRow);
				
				PropertyI prop = new Property();
				prop.setOwner(party);
				
				AddressI adr = new Address();
				adr = Bridge.fillAddressI(adr, addressLine1);
				adr.setCity(newCity);
    	    	adr.setZip(zip);
    	    	adr.setCounty(_County);
    	    	adr.setState(_State);
    	    	adr.setFreeform(addressLine1);	    		    	
    	    	prop.setAddress(adr);
    			
    	    	PinI pin = new Pin(); 
    	    	pin.addPin(PinI.PinType.PID, _AssessorsParcelIdentifier);
    	    	prop.setPin(pin);
    	    			
    	    	prop.setType(PropertyI.PType.GENERIC);
    	    	
				InstrumentI instr = new Instrument();
    	    	instr.setInstno(_AssessorsParcelIdentifier);
    	    	
    	    	instr.setDocType("ASSESSOR");
    	    	instr.setDocSubType("ASSESSOR");
    	    	
    	    	AssessorDocument assessDoc = new AssessorDocument(DocumentsManager.generateDocumentUniqueId(searchId, instr));
    	    	assessDoc.setInstrument(instr);
    	    	
    	    	assessDoc.setServerDocType("ASSESSOR");        	
    	    	assessDoc.setType(SimpleChapterUtils.DType.ASSESOR);
    	    	assessDoc.addProperty(prop);
    	    	assessDoc.setDataSource(getDataSite().getSiteTypeAbrev());
    	    	assessDoc.setSearchType(searchType);
    	    	assessDoc.setSiteId(getServerID());
    	    	
    	    	boolean isParentSite = currentResponse.isParentSite();
				if(isParentSite) {
					assessDoc.setSavedFrom(SavedFromType.PARENT_SITE);
				} else {
					assessDoc.setSavedFrom(SavedFromType.AUTOMATIC);
				}
				currentResponse.setDocument(assessDoc);
	        	
				currentResponse.setDocument(assessDoc);
				currentResponse.setUseDocumentForSearchLogRow(true);
			
				intermediaryResponse.add(currentResponse);
				
			}
			
			String header = "<table cellSpacing=\"0\" cellPadding=\"2\" border=\"1\" width=\"100%\"><tr><th>APN/PIN</th><th>Address</th><th>Owner</th></tr>";
			
			response.getParsedResponse().setHeader(header);
			response.getParsedResponse().setFooter("</table>");

			outputTable.append(table);
			
		}

		return intermediaryResponse;
	}
	
	@Override
	public DocumentI smartParseDetails(ServerResponse response, String detailsHtml, boolean fillServerResponse) {
		
		ParsedResponse parsedResponse = response.getParsedResponse();
		
		AssessorDocumentI document = (AssessorDocumentI)parsedResponse.getDocument();
		
		if (fillServerResponse) {
			parsedResponse.setResponse(detailsHtml);
			if (document != null) {
				parsedResponse.setDocument(document);
				parsedResponse.setUseDocumentForSearchLogRow(true);
			}
		}
		parsedResponse.setSearchId(searchId);
		
		return document;
	}

	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		if(getSearch().getSearchType() == Search.AUTOMATIC_SEARCH) {
			
			TSServerInfoModule module = null;
			FilterResponse adressFilter 	= AddressFilterFactory.getAddressHybridFilter( searchId , 0.82d , true, true);
			FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter( SearchAttributes.OWNER_OBJECT , searchId , module );
			FilterResponse nameFilterHybridDoNotSkipUnique = null;
			FilterResponse cityFilter = CityFilterFactory.getCityFilterDefault(searchId);
			FilterResponse multiplePINFilter = new MultiplePinFilterResponse(searchId);
			
			DocsValidator adressValidator = adressFilter.getValidator();
			adressValidator.setOnlyIfNotFiltered(true);
			DocsValidator nameValidatorHybrid = nameFilterHybrid.getValidator();
			nameValidatorHybrid.setOnlyIfNotFiltered(true);
			DocsValidator cityValidator = cityFilter.getValidator();
			cityValidator.setOnlyIfNotFiltered(true);
			DocsValidator multiplePINValidator = multiplePINFilter.getValidator();
			multiplePINValidator.setOnlyIfNotFiltered(true);
			DocsValidator legalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
			
			State state = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState();
			String stateAbrev = state.getStateAbv();
			
			Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			int searchType = global.getSearchType();
			
			if (Search.AUTOMATIC_SEARCH == searchType){
				// P0 - search by multiple PINs
				if ("IL".equals(stateAbrev)){
					Collection<String> pins = getSearchAttributes().getPins(-1);
					if(pins.size() > 1){			
						for(String pin: pins){
							module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
							module.clearSaKeys();
							module.getFunction(0).forceValue(pin);
							modules.add(module);	
						}			
						// set list for automatic search 
						serverInfo.setModulesForAutoSearch(modules);
						resultType = MULTIPLE_RESULT_TYPE;
						return;
					}
				}
			}
					
			// P1 : search by PIN
			String pin = getSearchAttribute(SearchAttributes.LD_PARCELNO_GENERIC_PRI);	
			if(!StringUtils.isEmpty(pin)){
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.setData( 0 , pin );  
				modules.add(module);		
			}
			if( pin.indexOf( "-" ) > 0 || pin.indexOf(".")>0 || pin.indexOf("/")>0){
				pin = pin.replaceAll( "[-./]", "" );
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.PARCEL_ID_MODULE_IDX));
				module.clearSaKeys();
				module.setData( 0 , pin );  
				modules.add(module);
			}
			String strNo   = getSearchAttribute(SearchAttributes.P_STREETNO);
			String strSuf  = getSearchAttribute(SearchAttributes.P_STREETSUFIX);
			String strUnit = getSearchAttribute(SearchAttributes.P_STREETUNIT_CLEANED);
			String strPostDirection = getSearchAttribute(SearchAttributes.P_STREET_POST_DIRECTION);
			
			// construct the list of street names
			String tmpName = getSearchAttribute(SearchAttributes.P_STREETNAME).trim();
			Set<String> strNames = new LinkedHashSet<String>(); 
			if(!StringUtils.isEmpty(tmpName)){
				strNames.add(tmpName);
			}
			
			//we have cases when they put "." in the name of the street St.Jhons
			tmpName = tmpName.replace(".", " ").replaceAll("\\s{2,}", " ").trim();
			if(!StringUtils.isEmpty(tmpName)){
				strNames.add(tmpName);
			}		
			
			// P2: search by address
			if(!StringUtils.isEmpty(strNo))	{
				for(String strName: strNames){
					
					// search with unit if present
					if(!StringUtils.isEmpty(strUnit)){
						module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
						module.clearSaKeys();
						module.setData( 0 , strNo );
						module.setData( 2 , strName );
						module.setData( 5 , strUnit );
						module.addFilter( adressFilter );
						module.addFilter( cityFilter );
						module.addFilter( nameFilterHybrid );
						if ("IL".equals(stateAbrev)){
							module.addFilter(multiplePINFilter);
						}
						module.addValidator( adressValidator );
						module.addValidator( cityValidator );
						module.addValidator( nameValidatorHybrid );
						if ("IL".equals(stateAbrev)){
							module.addValidator(multiplePINValidator);
						}
						module.addValidator(legalValidator);
						modules.add(module);
					}
					
					// search with suffix if present
					module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
					module.clearSaKeys();
					module.setData( 0 , strNo );
					module.setData( 2 , strName );
					module.setData( 3 , strSuf );
					module.addFilter( adressFilter );
					module.addFilter( cityFilter );
					module.addFilter( nameFilterHybrid );
					if ("IL".equals(stateAbrev)){
						module.addFilter(multiplePINFilter);
					}
					module.addValidator( adressValidator );
					module.addValidator( cityValidator );
					module.addValidator( nameValidatorHybrid );
					if ("IL".equals(stateAbrev)){
						module.addValidator(multiplePINValidator);
					}
					module.addValidator(legalValidator);
					modules.add(module);
					
					// search without suffix
					if(!StringUtils.isEmpty(strSuf)){
						module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
						module.clearSaKeys();
						module.setData( 0 , strNo );
						module.setData( 2 , strName );
						module.addFilter( adressFilter );
						module.addFilter( cityFilter );
						module.addFilter( nameFilterHybrid );
						if ("IL".equals(stateAbrev)){
							module.addFilter(multiplePINFilter);
						}
						module.addValidator( adressValidator );
						module.addValidator( cityValidator );
						module.addValidator( nameValidatorHybrid );
						if ("IL".equals(stateAbrev)){
							module.addValidator(multiplePINValidator);
						}
						module.addValidator(legalValidator);
						modules.add(module);				
					}
					
					// search with post direction
					if(!StringUtils.isEmpty(strPostDirection)){
						module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
						module.clearSaKeys();
						module.setData( 0 , strNo );
						module.setData( 2 , strName );
						module.setData( 4 , strPostDirection);
						module.addFilter( adressFilter );
						module.addFilter( cityFilter );
						module.addFilter( nameFilterHybrid );
						if ("IL".equals(stateAbrev)){
							module.addFilter(multiplePINFilter);
						}
						module.addValidator( adressValidator );
						module.addValidator( cityValidator );
						module.addValidator( nameValidatorHybrid );
						if ("IL".equals(stateAbrev)){
							module.addValidator(multiplePINValidator);
						}
						module.addValidator(legalValidator);
						modules.add(module);				
					}
					
					if(!(StringUtils.isEmpty(strNo)&&StringUtils.isEmpty(strSuf))){
						module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
						module.clearSaKeys();
						module.setData( 2 , strName );
						module.addFilter( adressFilter );
						module.addFilter( cityFilter );
						module.addFilter( nameFilterHybrid );
						if ("IL".equals(stateAbrev)){
							module.addFilter(multiplePINFilter);
						}
						module.addValidator( adressValidator );
						module.addValidator( cityValidator );
						module.addValidator( nameValidatorHybrid );
						if ("IL".equals(stateAbrev)){
							module.addValidator(multiplePINValidator);
						}
						module.addValidator(legalValidator);
						modules.add(module);	
					}
					
					// eliminate direction from street name
					String DIR = "NORTH|SOUTH|EAST|WEST|N|S|E|W|NORTHEAST|NORTHWEST|SOUTHEAST|SOUTHWEST|NE|NW|SE|SW";			
					String strName1 = strName.toUpperCase().replaceFirst("^(" + DIR + ")\\s(.+)", "$2");
					if(!strName.equalsIgnoreCase(strName1)){
						module =  new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
						module.clearSaKeys();
						module.setData( 0 , strNo );
						module.setData( 2 , strName1 );
						module.setData( 5 , strUnit );
						module.addFilter( adressFilter );
						module.addFilter( cityFilter );
						module.addFilter( nameFilterHybrid );
						if ("IL".equals(stateAbrev)){
							module.addFilter(multiplePINFilter);
						}
						module.addValidator( adressValidator );
						module.addValidator( cityValidator );
						module.addValidator( nameValidatorHybrid );
						if ("IL".equals(stateAbrev)){
							module.addValidator(multiplePINValidator);
						}
						module.addValidator(legalValidator);
						modules.add(module);				
					}
					
					//take the last word and use it as suffix
					String []names = strName.split("\\s+");
					if(names.length>=2){
						if(Normalize.isSuffix(names[names.length-1])
								||Normalize.isSpecialSuffix(names[names.length-1])
								||Normalize.isIstateSuffix(names[names.length-1])){
							
							StringBuilder newStreetName = new StringBuilder(names[0]);
							newStreetName.append(" "); 
							for(int i=1;i<names.length-1;i++){
								newStreetName.append(names[i]);
								newStreetName.append(" "); 
							} 
							
							module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
							module.clearSaKeys();
							module.setData( 0 , strNo );
							module.setData( 2 , newStreetName.toString().trim() );
							module.setData( 3 , names[names.length-1] );
							module.setData( 5 , strUnit );
							module.addFilter( adressFilter );
							module.addFilter( cityFilter );
							module.addFilter( nameFilterHybrid );
							if ("IL".equals(stateAbrev)){
								module.addFilter(multiplePINFilter);
							}
							module.addValidator( adressValidator );
							module.addValidator( cityValidator );
							module.addValidator( nameValidatorHybrid );
							if ("IL".equals(stateAbrev)){
								module.addValidator(multiplePINValidator);
							}
							module.addValidator(legalValidator);
							modules.add(module);	
							
							//without suffix
							module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
							module.clearSaKeys();
							module.setData( 0 , strNo );
							module.setData( 2 , newStreetName.toString().trim() );
							//module.setData( 3 , names[names.length-1] );
							module.setData( 5 , strUnit );
							module.addFilter( adressFilter );
							module.addFilter( cityFilter );
							module.addFilter( nameFilterHybrid );
							if ("IL".equals(stateAbrev)){
								module.addFilter(multiplePINFilter);
							}
							module.addValidator( adressValidator );
							module.addValidator( cityValidator );
							module.addValidator( nameValidatorHybrid );
							if ("IL".equals(stateAbrev)){
								module.addValidator(multiplePINValidator);
							}
							module.addValidator(legalValidator);
							modules.add(module);	
						}
					}
					
					//keep only the first word from the street name
					int idx = strName.indexOf(" ");
					String strName2 = strName;
					if(idx > 5){
						strName2 = strName.substring(0, idx);
					}
					if(!strName.equalsIgnoreCase(strName2)){
						module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.ADDRESS_MODULE_IDX));
						module.clearSaKeys();
						module.setData( 0 , strNo );
						module.setData( 2 , strName2 );
						module.setData( 5 , strUnit );
						module.addFilter( adressFilter );
						module.addFilter( cityFilter );
						module.addFilter( nameFilterHybrid );
						if ("IL".equals(stateAbrev)){
							module.addFilter(multiplePINFilter);
						}
						module.addValidator( adressValidator );
						module.addValidator( cityValidator );
						module.addValidator( nameValidatorHybrid );
						if ("IL".equals(stateAbrev)){
							module.addValidator(multiplePINValidator);
						}
						module.addValidator(legalValidator);
						modules.add(module);				
					}
				}
			}
			
			// P3: Search by owners
			if( hasOwner()){ 
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				module.clearSaKeys();			
				module.setSaObjKey(SearchAttributes.OWNER_OBJECT);
				nameFilterHybridDoNotSkipUnique = NameFilterFactory.getHybridNameFilter( 
						SearchAttributes.OWNER_OBJECT , searchId , module );
				nameFilterHybridDoNotSkipUnique.setSkipUnique(false);
				if( strNames.size()>0 ){
					module.addFilter( adressFilter );
				}
				if (hasCity()) {
					module.addFilter( cityFilter );
				}
				module.addFilter( nameFilterHybridDoNotSkipUnique );
				
				DocsValidator nameValidatorHybridDoNotSkipUnique = nameFilterHybridDoNotSkipUnique.getValidator();
				nameValidatorHybridDoNotSkipUnique.setOnlyIfNotFiltered(true);
				module.addValidator(nameValidatorHybridDoNotSkipUnique);
				
				if( strNames.size()>0 ){
					module.addValidator( adressValidator );
				}
				if (hasCity()) {
					module.addValidator( cityValidator );
				}
				module.addValidator(legalValidator);
				
				module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				
				ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[] {"L;F;","L;M;"});  
				nameIterator.setDoScottishNamesDerivations(true);
				nameIterator.clearSearchedNames();
				nameIterator.setInitAgain(true);
				module.addIterator(nameIterator);
				
				modules.add(module);	
			}
			
		}
		
		// set list for automatic search 
		serverInfo.setModulesForAutoSearch( modules );
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
		boolean result = mSearch.getSa().getPins(-1).size() > 1 &&
			    		 mSearch.getAdditionalInfo(AdditionalInfoKeys.MULTIPLE_PIN) != Boolean.TRUE;
		return result?true:super.anotherSearchForThisServer(sr);
	}

}
