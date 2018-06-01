package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.URI;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.Node;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.bean.recoverdocument.ModuleShortDescription;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.log.SearchLogFactory;
import ro.cst.tsearch.log.SearchLogPage;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericMultipleLegalFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.data.LegalStruct;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.OcrOrBootStraperIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.parentsite.ModuleWrapperManager;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.threads.GPMaster;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.XStreamManager;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShip;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameFormater;
import com.stewart.ats.base.name.NameFormaterI;
import com.stewart.ats.base.name.NameFormaterI.PosType;
import com.stewart.ats.base.name.NameFormaterI.TitleType;
import com.stewart.ats.base.name.NameI;

/**
 * counties which don't have images on the official site and there is no TS for them (to take images from)
 * AZ Apache, AZ Cochise, AZ La Paz, AZ Santa Cruz
 * CO Baca, CO Dolores, CO Kiowa, CO Lincoln, CO San Juan, CO Sedgwick, CO Teller, CO Washington
 */
@SuppressWarnings("deprecation")
public class GenericCountyRecorderRO extends TSServerROLike {

	private static final long serialVersionUID = -187345462916860234L;
	
	private static HashMap<String, SelectLists> cachedSelectLists = new HashMap<String, SelectLists>();
	
	public static final String BOOK_PAGE_PATT = "([^-]+)-([^-]+)-([^\"]+)"; 
	
	public GenericCountyRecorderRO(long searchId) {
		super(searchId);
	}

	public GenericCountyRecorderRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	public static HashMap<String, SelectLists> getCachedSelectLists() {
		if (cachedSelectLists!=null) {
			return cachedSelectLists;
		}
		return new HashMap<String, SelectLists>();
	}

	public static void setCachedSelectLists(HashMap<String, SelectLists> cachedSelectLists) {
		GenericCountyRecorderRO.cachedSelectLists = cachedSelectLists;
	}
	
	public static class SelectLists {
//		private Calendar dateModified = null;
		private LinkedHashMap<String, String> bookTypeMap = null;
		private LinkedHashMap<String,LinkedHashMap<String,ArrayList<String>>> typeBookPageMap = null;
		private boolean hasDocumentGroup = false;
		private LinkedHashMap<String, String> documentGroupMap = null;
		private LinkedHashMap<String, String> documentTypeMap = null;
		private LinkedHashMap<String, String> subdivisionMap = null;
		private String resultsPerPageSelect = null;
//		public Calendar getDateModified() {
//			return dateModified;
//		}
//		public void setDateModified(Calendar dateModified) {
//			this.dateModified = dateModified;
//		}
		public LinkedHashMap<String, String> getBookTypeMap() {
			return bookTypeMap;
		}
		public void setBookTypeMap(LinkedHashMap<String, String> bookTypeMap) {
			this.bookTypeMap = bookTypeMap;
		}
		public LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> getTypeBookPageMap() {
			return typeBookPageMap;
		}
		public void setTypeBookPageMap(
				LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> typeBookPageMap) {
			this.typeBookPageMap = typeBookPageMap;
		}
		public boolean isHasDocumentGroup() {
			return hasDocumentGroup;
		}
		public void setHasDocumentGroup(boolean hasDocumentGroup) {
			this.hasDocumentGroup = hasDocumentGroup;
		}
		public LinkedHashMap<String, String> getDocumentGroupMap() {
			return documentGroupMap;
		}
		public void setDocumentGroupMap(LinkedHashMap<String, String> documentGroupMap) {
			this.documentGroupMap = documentGroupMap;
		}
		public LinkedHashMap<String, String> getDocumentTypeMap() {
			return documentTypeMap;
		}
		public void setDocumentTypeMap(LinkedHashMap<String, String> documentTypeMap) {
			this.documentTypeMap = documentTypeMap;
		}
		public LinkedHashMap<String, String> getSubdivisionMap() {
			return subdivisionMap;
		}
		public void setSubdivisionMap(LinkedHashMap<String, String> subdivisionMap) {
			this.subdivisionMap = subdivisionMap;
		}
		public String getResultsPerPageSelect() {
			return resultsPerPageSelect;
		}
		public void setResultsPerPageSelect(String resultsPerPageSelect) {
			this.resultsPerPageSelect = resultsPerPageSelect;
		}
	}
	
	private static LinkedHashMap<String,String> getBookTypeMap(long searchId) {
		
		CurrentInstance instance = InstanceManager.getManager().getCurrentInstance(searchId);
		String stateCounty = instance.getCurrentState().getStateAbv() + instance.getCurrentCounty().getName();
				
		SelectLists sl = getCachedSelectLists().get(stateCounty);
		if (sl!=null) {
			LinkedHashMap<String, String> bookTypeMap = sl.getBookTypeMap();
			if (bookTypeMap!=null) {
				return bookTypeMap;
			}
		}
		
		return new LinkedHashMap<String,String>();
		
	}
	
	public String getBookTypeFromCode(long searchId, String code) {
		Map<String,String> map = getBookTypeMap(searchId);
		String res = map.get(code);
		if (res!=null) {
			return res;
		}
		return code;
	}
	
	public String getCodeFromBookType(long searchId, String bookType) {
		Map<String,String> map = getBookTypeMap(searchId);
		for (Map.Entry<String, String> entry : map.entrySet()) {
			if (bookType.equals(entry.getValue())) {
	        	return entry.getKey();
	        }
		}
		return bookType;
	}
	
	public String getDocumentTypeFromCode(String code) {
		SelectLists sl = getCachedSelectLists().get(getStateCounty());
		if (sl!=null) {
			HashMap<String, String> documentTypeMap = sl.getDocumentTypeMap();
			if (documentTypeMap!=null) {
				String res = documentTypeMap.get(code);
				if (res!=null) {
					return res;
				}
			}
		}
		return code;
	}
	
	public String getSubdivisionCodeFromName(String name) {
		SelectLists sl = getCachedSelectLists().get(getStateCounty());
		if (sl!=null) {
			HashMap<String, String> subdivisionMap = sl.getSubdivisionMap();
			if (subdivisionMap!=null) {
				for (Map.Entry<String, String> entry : subdivisionMap.entrySet()) {
					if (name.equals(entry.getValue())) {
			        	return entry.getKey();
			        }
				}
			}
		}
		return "";
	}
	
	public String getNameFromSubdivisionCode(String code) {
		SelectLists sl = getCachedSelectLists().get(getStateCounty());
		if (sl!=null) {
			HashMap<String, String> subdivisionMap = sl.getSubdivisionMap();
			if (subdivisionMap!=null) {
				String res = subdivisionMap.get(code);
				if (res!=null) {
					return res;
				}
			}
		}
		return code;
	}
	
	public static String getDocumentGroupFromCode(String code, String stateCounty) {
		SelectLists sl = getCachedSelectLists().get(stateCounty);
		if (sl!=null) {
			HashMap<String, String> documentGroupMap = sl.getDocumentGroupMap();
			if (documentGroupMap!=null) {
				String res = documentGroupMap.get(code);
				if (res!=null) {
					return res;
				}
			}
		}	
		return code;
	}
	
	public String getStateCounty() {
		DataSite dataSite = getDataSite();
		return dataSite.getStateAbbreviation() + dataSite.getCountyName();
	}
	
	private void setSelectList() {
		boolean mustGetSelectLists = false;
		SelectLists selectLists = getCachedSelectLists().get(getStateCounty());
		if (selectLists==null) {
			mustGetSelectLists = true;
		} else {
//			Calendar dateModified = selectLists.getDateModified();
//			if (dateModified==null) {
//				mustGetSelectLists = true;
//			} else {
//				Calendar today = Calendar.getInstance();
//				if (today.get(Calendar.YEAR)==dateModified.get(Calendar.YEAR) && 
//						today.get(Calendar.MONTH)==dateModified.get(Calendar.MONTH) &&
//						today.get(Calendar.DAY_OF_MONTH)==dateModified.get(Calendar.DAY_OF_MONTH)) {
//					mustGetSelectLists = false;
//				}
//			}
		}
		if (mustGetSelectLists) {
			boolean hasFolder = true;
    		String folderPath = ServerConfig.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/SelectLists/");
    		File folder = new File(folderPath);
    		if (!folder.exists() || !folder.isDirectory()) {
    			hasFolder = false;
    		}
    		if (hasFolder) {
				String stateCounty = getStateCounty();
				File file = new File(folderPath + File.separator + stateCounty.replaceAll("\\s+", "") + "RO.xml");
				if (!file.exists() || file.isDirectory()) {
					//nothing to do
				} else {
					String xml = null;
					try {
						xml = FileUtils.readFileToString(file);
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (xml!=null) {
						XStreamManager xsm = XStreamManager.getInstance();
						Object o = xsm.fromXML(xml);
						if (o!=null && o instanceof SelectLists) {
							SelectLists newSelectLists = (SelectLists)o;
							HashMap<String, SelectLists> newCcachedSelectLists = getCachedSelectLists();
							newCcachedSelectLists.put(stateCounty, newSelectLists);
							setCachedSelectLists(newCcachedSelectLists);
						}
					}
				}
			}
		}
	}
	
	public void setFieldNote(TSServerInfo msiServerInfoDefault, int module, int index, ModuleWrapperManager moduleWrapperManager, String siteName) {
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(module);
		if(tsServerInfoModule != null) {
			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
				
			}
			PageZone pageZone = (PageZone)tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if(StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, index, nameToIndex.get(functionName));
					if(comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
	}
	
	@Override
	public TSServerInfo getDefaultServerInfo() {

		setSelectList();
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);

		if (tsServerInfoModule != null) {
			SelectLists sl = getCachedSelectLists().get(getStateCounty());
			if (sl!=null) {
				
				HashMap<String, String> bookTypeMap = sl.getBookTypeMap();
				if (bookTypeMap!=null) {
					StringBuilder sb1 = new StringBuilder();
					sb1.append("<select id=\"").append(ro.cst.tsearch.connection.http2.GenericCountyRecorderRO.BOOK_TYPE_PARAM).append("\" name=\"")
						.append(ro.cst.tsearch.connection.http2.GenericCountyRecorderRO.BOOK_TYPE_PARAM)
						.append("\" onchange=\"updateBookNumber(this)\">");
					Iterator<Entry<String, String>> it1 = bookTypeMap.entrySet().iterator();
					int i1=0;
					while (it1.hasNext()) {
						Entry<String, String> entry1 = it1.next();
						sb1.append("<option value=\"").append(entry1.getKey());
						if (i1==0) {
							sb1.append("\" selected=\"selected");
						}
						sb1.append("\">").append(entry1.getValue()).append("</option>");
						i1++;
					}
					sb1.append("</select>");
					if (tsServerInfoModule.getFunctionCount()>0) {
						setupSelectBox(tsServerInfoModule.getFunction(0), sb1.toString());
					}
				}
				
				LinkedHashMap<String,LinkedHashMap<String,ArrayList<String>>> typeBookPageMap = sl.getTypeBookPageMap();
				if (typeBookPageMap!=null) {
					Iterator<Entry<String, LinkedHashMap<String, ArrayList<String>>>> it2 = typeBookPageMap.entrySet().iterator();
					StringBuilder sb2 = new StringBuilder();
					if (it2.hasNext()) {
						 Map.Entry<String, LinkedHashMap<String, ArrayList<String>>> pairs2 = (Map.Entry<String, LinkedHashMap<String, ArrayList<String>>>)it2.next();
						 LinkedHashMap<String, ArrayList<String>> bookNumber = pairs2.getValue();
						 if (bookNumber.size()==0) {
							 sb2.append("<select id=\"").append(ro.cst.tsearch.connection.http2.GenericCountyRecorderRO.BOOK_NUMBER_PARAM)
								.append("\" name=\"").append(ro.cst.tsearch.connection.http2.GenericCountyRecorderRO.BOOK_NUMBER_PARAM)
								.append("\">").append("<option value=\"\">").append("").append("</option>").append("</select>");
						 } else {
							 sb2.append("<select id=\"").append(ro.cst.tsearch.connection.http2.GenericCountyRecorderRO.BOOK_NUMBER_PARAM)
								.append("\" name=\"").append(ro.cst.tsearch.connection.http2.GenericCountyRecorderRO.BOOK_NUMBER_PARAM)
								.append("\">");
							 Iterator<Entry<String, ArrayList<String>>> it3 = bookNumber.entrySet().iterator();
							 int i2=0;
							 while (it3.hasNext()) {
								 Entry<String, ArrayList<String>> pairs3 = it3.next();
								 sb2.append("<option value=\"").append(pairs3.getKey());
								 if (i2==0) {
										sb2.append("\" selected=\"selected");
									}
								 sb2.append("\">").append(pairs3.getKey()).append("</option>");
								 i2++;
							}
							 sb2.append("</select>");
						 }
					}
					if (tsServerInfoModule.getFunctionCount()>1) {
						setupSelectBox(tsServerInfoModule.getFunction(1), sb2.toString());
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.MODULE_IDX39);
		if (tsServerInfoModule!=null) {
			SelectLists sl = getCachedSelectLists().get(getStateCounty());
			if (sl!=null) {
				
				HashMap<String, String> documentGroupMap = sl.getDocumentGroupMap();
				if (documentGroupMap!=null) {
					StringBuilder sb1 = new StringBuilder();
					sb1.append("<select id=\"").append(ro.cst.tsearch.connection.http2.GenericCountyRecorderRO.DOCUMENT_GROUP_PARAM).append("\" name=\"")
						.append(ro.cst.tsearch.connection.http2.GenericCountyRecorderRO.DOCUMENT_GROUP_PARAM).append("\">");
					Iterator<Entry<String, String>> it1 = documentGroupMap.entrySet().iterator();
					int i1=0;
					while (it1.hasNext()) {
						Entry<String, String> entry1 = it1.next();
						sb1.append("<option value=\"").append(entry1.getKey());
						if (i1==0) {
							sb1.append("\" selected=\"selected");
						}
						sb1.append("\">").append(entry1.getValue()).append("</option>");
						i1++;
					}
					sb1.append("</select>");
					if (tsServerInfoModule.getFunctionCount()>20) {
						setupSelectBox(tsServerInfoModule.getFunction(20), sb1.toString());
					}
				}
				
				HashMap<String, String> documentTypeMap = sl.getDocumentTypeMap();
				if (documentTypeMap!=null) {
					StringBuilder sb1 = new StringBuilder();
					sb1.append("<select id=\"").append(ro.cst.tsearch.connection.http2.GenericCountyRecorderRO.DOCUMENT_TYPE_PARAM).append("\" name=\"")
						.append(ro.cst.tsearch.connection.http2.GenericCountyRecorderRO.DOCUMENT_TYPE_PARAM).append("\">");
					sb1.append("<option value=\"\" selected=\"selected\"></option>");
					Iterator<Entry<String, String>> it1 = documentTypeMap.entrySet().iterator();
					while (it1.hasNext()) {
						Entry<String, String> entry1 = it1.next();
						sb1.append("<option value=\"").append(entry1.getKey()).append("\">").append(entry1.getValue()).append("</option>");
					}
					sb1.append("</select>");
					if (tsServerInfoModule.getFunctionCount()>3) {
						setupSelectBox(tsServerInfoModule.getFunction(3), sb1.toString());
					}
				}
				
				HashMap<String, String> subdivisionMap = sl.getSubdivisionMap();
				if (subdivisionMap!=null) {
					StringBuilder sb1 = new StringBuilder();
					sb1.append("<select id=\"").append(ro.cst.tsearch.connection.http2.GenericCountyRecorderRO.SUBDIVISION_PARAM).append("\" name=\"")
						.append(ro.cst.tsearch.connection.http2.GenericCountyRecorderRO.SUBDIVISION_PARAM).append("\">");
					sb1.append("<option value=\"\" selected=\"selected\"></option>");
					Iterator<Entry<String, String>> it1 = subdivisionMap.entrySet().iterator();
					while (it1.hasNext()) {
						Entry<String, String> entry1 = it1.next();
						sb1.append("<option value=\"").append(entry1.getKey()).append("\">").append(entry1.getValue()).append("</option>");
					}
					sb1.append("</select>");
					if (tsServerInfoModule.getFunctionCount()>13) {
						setupSelectBox(tsServerInfoModule.getFunction(13), sb1.toString());
					}
				}
				
				String resultsPerPageSelect = sl.getResultsPerPageSelect();
				if (tsServerInfoModule.getFunctionCount()>17 && resultsPerPageSelect!=null) {
					setupSelectBox(tsServerInfoModule.getFunction(17), resultsPerPageSelect);
				}
			}
		}
		
		ModuleWrapperManager moduleWrapperManager = ModuleWrapperManager.getInstance();
		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID);
		String siteName = StateCountyManager.getInstance().getSTCounty(dataSite.getCountyId()) + dataSite.getSiteType();
		
		setFieldNote(msiServerInfoDefault, TSServerInfo.BOOK_AND_PAGE_MODULE_IDX, 3, moduleWrapperManager, siteName);
		setFieldNote(msiServerInfoDefault, TSServerInfo.INSTR_NO_MODULE_IDX, 0, moduleWrapperManager, siteName);
		setFieldNote(msiServerInfoDefault, TSServerInfo.MODULE_IDX38, 1, moduleWrapperManager, siteName);
		setFieldNote(msiServerInfoDefault, TSServerInfo.MODULE_IDX39, 2, moduleWrapperManager, siteName);
		
		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		if (rsResponse.indexOf("Book Type must be selected.") > -1) {
			Response.getParsedResponse().setError("Book Type must be selected.");
			return;
		}
		
		if (rsResponse.indexOf("Book Number must be selected.") > -1) {
			Response.getParsedResponse().setError("Book Number must be selected.");
			return;
		}
		
		if (rsResponse.indexOf("Page Number must be selected.") > -1) {
			Response.getParsedResponse().setError("Page Number must be selected.");
			return;
		}
		
		if (rsResponse.indexOf("Invalid Number Format.") > -1) {
			Response.getParsedResponse().setError("Invalid Number Format.");
			return;
		}
		
		/*if (rsResponse.indexOf("Book Type must be selected.") > -1) {
			Response.getParsedResponse().setError("Book Type must be selected.");
			return;
		}*/
		
		if (rsResponse.indexOf("Tracking ID must be numeric.") > -1) {
			Response.getParsedResponse().setError("Tracking ID must be numeric.");
			return;
		}
		
		if (rsResponse.indexOf("You must specify search parameters before selecting Execute Search.") > -1) {
			Response.getParsedResponse().setError("You must specify search parameters when using Combined Search.");
			return;
		}
		
		if (rsResponse.indexOf("Document Not Found.") > -1) {	//Search by Reception Number, Book and Page, Tracking ID
			Response.getParsedResponse().setError("No data found.");
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
		}
		
		if (rsResponse.indexOf("No documents were found that match the search parameters.") > -1) {	//Combined Search
			Response.getParsedResponse().setError("No data found.");
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
		}
		
		if (rsResponse.indexOf("Unrecoverable Error") > -1) {			//Error appeared when searching with values which  
			Response.getParsedResponse().setError("No data found.");	//does not exist in select lists, e.g. MAPS COUNTRYRD 20
			Response.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE, "true");
			return;
		}

		switch (viParseID) {
		
		case ID_SEARCH_BY_MODULE39:				//Combined Search
		
			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() == 0) {
				return;
			}

			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());

			String header = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST");
			String footer = "";

			String navigationLinks = getNavigationLinks(rsResponse);
				
			header += "<table><tr><td>" + navigationLinks +  "<table>" + "<tr><th>" + SELECT_ALL_CHECKBOXES + "</th>" + 
				"<th>Item #</th><th>Reception #</th><th>Recording Date</th><th>Document Type</th><th>Document Name</th><th>Name Type</th></tr>";

			Object numberOfUnsavedDocument = GetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS);

			if (numberOfUnsavedDocument != null && numberOfUnsavedDocument instanceof Integer) {
				footer = "</table>" + navigationLinks + "</td></tr></table>"
					+ CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, (Integer) numberOfUnsavedDocument);
			} else {
				footer = "</table>" + navigationLinks + "</td></tr></table>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);
			}

			parsedResponse.setHeader(header);
			parsedResponse.setFooter(footer);
			
			break;

		case ID_DETAILS:
		case ID_SEARCH_BY_INSTRUMENT_NO:		//Search by Reception Number
		case ID_SEARCH_BY_BOOK_AND_PAGE:		//Search by Book and Page
		case ID_SEARCH_BY_MODULE38:				//Search by Tracking ID
		case ID_SAVE_TO_TSD:
			
			StringBuilder serialNumber = new StringBuilder();
			HashMap<String, String> data = new HashMap<String, String>();
			String details = getDetails(rsResponse, serialNumber, data, Response);
			String filename = serialNumber + ".html";
			
			if (viParseID != ID_SAVE_TO_TSD) {
				try {
					String originalLink = sAction.replace("?", "&") + "&" + Response.getRawQuerry();
					String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + URLDecoder.decode(originalLink, "UTF-8");
					
					if (isInstrumentSaved(serialNumber.toString(), null, data, false)){
						details += CreateFileAlreadyInTSD();
					}
					else {
						mSearch.addInMemoryDoc(sSave2TSDLink, details);
						details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
					}
					Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
					Response.getParsedResponse().setResponse(details);
					
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				
			} else {
				smartParseDetails(Response, details);
				String detailsWithLinks = details;
				
				//remove links
				details = details.replaceAll("(?is)<a[^>]+href=[^>]*>([^<]*)</a>", "$1");
				//remove View Image text
				details = details.replaceAll("(?is)<tr>\\s*<td[^>]+>\\s*View\\s+Image\\s*</td>.*?</tr>", "");
				details = details.replaceAll("(?is)(<span\\s+id=\\\"MainContent_searchMainContent_ctl00_tbDescription\"\\s*>)(.*?</span>)"
						, "$1&nbsp;&nbsp;&nbsp;&nbsp;$2");
				
				msSaveToTSDFileName = filename;
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);	
				Response.getParsedResponse().setResponse(details);
				
				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				saveRelatedDocuments(Response, detailsWithLinks);
				
			}
			break;

			case ID_GET_LINK:
				ParseResponse(sAction, Response, sAction.contains("/Document.aspx") ? ID_DETAILS : ID_SEARCH_BY_MODULE39);
			break;
		}
	}
	
	protected void saveRelatedDocuments(ServerResponse Response, String detailsWithLinks) {
		
		Matcher ma = Pattern.compile("(?is)href=\"([^\"]+)\"").matcher(detailsWithLinks);
		while (ma.find()) {
			ParsedResponse prChild = new ParsedResponse();
			String link = ma.group(1) + "&isSubResult=true";
			if (link.contains("Document.aspx") && !link.contains("&img=")) {
				LinkInPage pl = new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD);
				prChild.setPageLink(pl);
				Response.getParsedResponse().addOneResultRowOnly(prChild);
			}
		}
		
	}
	
	private String extractAlternateInstrumentNumber(ServerResponse response, NodeList nodeList) {
		String result = "";
		
		URI lastUri = response.getLastURI();
		if (lastUri!=null) {
			result = StringUtils.extractParameter(lastUri.toString(), "DK=([^&?]*)");
		} else {
			NodeList alternativeInstrumentNumberList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "alternativeInstrumentNumber"));
			if (alternativeInstrumentNumberList.size()>0) {
				result = alternativeInstrumentNumberList.elementAt(0).toPlainTextString();
			}
		}
		
		return result;
	}
	
	public static boolean instrNoIsBookPage(String instrNo, long searchId) {
		Matcher ma = Pattern.compile(BOOK_PAGE_PATT).matcher(instrNo);
		if (ma.matches()) {
			String bookType = ma.group(1);
			Map<String, String> map = getBookTypeMap(searchId);
			for (Map.Entry<String, String> entry : map.entrySet()) {
				if (bookType.equals(entry.getValue())) {
		        	return true;
		        }
			}
		}
		return false;
	}
	
	protected String getDetails(String rsResponse, StringBuilder accountId, HashMap<String, String> data, ServerResponse Response) {

		try {
			StringBuilder details = new StringBuilder();
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);

			NodeList nodeList = htmlParser.parse(null);

			NodeList tdList = nodeList.extractAllNodesThatMatch(new TagNameFilter("td"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "tableMain_Content"));

			if (tdList.size() != 1) {
				return null;
			}

			String instrumentNumber = "";
			String bookType = "";
			String book = "";
			String page = "";
			String year = "";
			String type = "";
			String trackingID = extractAlternateInstrumentNumber(Response, nodeList);
			
			boolean hasBookPage = false;
			NodeList bookPageList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_tbBookPage"));
				if (bookPageList.size()>0) {
					String value = bookPageList.elementAt(0).toHtml();
					Matcher ma = Pattern.compile("(?is)value=\"" + BOOK_PAGE_PATT + "\"").matcher(value);
					if (ma.find()) {
						bookType = ma.group(1);
						book = ma.group(2);
						page = ma.group(3);
						hasBookPage = true;
					}
				} else {
					bookPageList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
						.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_tbBookPage"));
					if (bookPageList.size()>0) {
						String value = bookPageList.elementAt(0).toPlainTextString().trim();
						Matcher ma = Pattern.compile(BOOK_PAGE_PATT).matcher(value);
						if (ma.find()) {
							bookType = ma.group(1);
							book = ma.group(2);
							page = ma.group(3);
							hasBookPage = true;
						}
					}
				}
			
			NodeList instrumentNumberList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_tbReceptionNo"));
			if (instrumentNumberList.size()>0) {
				String value = instrumentNumberList.elementAt(0).toHtml();
				Matcher ma = Pattern.compile("(?is)value=\"([^\"]+)\"").matcher(value);
				if (ma.find()) {
					instrumentNumber = ma.group(1);
					if ("sensitive".equalsIgnoreCase(instrumentNumber) && !hasBookPage) {
						instrumentNumber = trackingID;
						rsResponse = rsResponse.replace("$", "\\$")
							.replaceFirst("(?is)(<input[^>]+value=\")Sensitive(\"[^>]+id=\"MainContent_searchMainContent_ctl00_tbReceptionNo\"[^>]*>)", 
								"$1" + instrumentNumber + "$2");
					}
				} else if (!hasBookPage) {	//if Reception number is missing (e.g. CO Huerfano Tracking ID 265567)
					instrumentNumber = trackingID;
					rsResponse = rsResponse.replace("$", "\\$")
						.replaceFirst("(?is)(<input[^>]+type=\"text\"[^>]+)(id=\"MainContent_searchMainContent_ctl00_tbReceptionNo\"[^>]*>)", 
							"$1value=\"" + instrumentNumber + "\" $2");
				}
			} else {
				instrumentNumberList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_tbReceptionNo"));
				if (instrumentNumberList.size()>0) {
					instrumentNumber = instrumentNumberList.elementAt(0).toPlainTextString().trim();
				}
			}
			
			instrumentNumber = instrumentNumber.replaceFirst("^0+", "").trim();
			if (instrNoIsBookPage(instrumentNumber, searchId)) {
				if (!hasBookPage) {
					Matcher ma = Pattern.compile(BOOK_PAGE_PATT).matcher(instrumentNumber);
					if (ma.find()) {
						bookType = ma.group(1);
						book = ma.group(2);
						page = ma.group(3);
						hasBookPage = true;
					}
				}
				instrumentNumber = "";
			}
			
			NodeList recordedDateList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_tbReceptionDate"));
			if (recordedDateList.size()>0) {
				String value = recordedDateList.elementAt(0).toHtml();
				Matcher ma = Pattern.compile("(?is)value=\"(\\d{1,2}-\\d{1,2}-(\\d{4})[^\"]+)\"").matcher(value);
				if (ma.find()) {
					year = ma.group(2);
				}
			} else {
				recordedDateList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_tbReceptionDate"));
				if (recordedDateList.size()>0) {
					String value = recordedDateList.elementAt(0).toPlainTextString();
					Matcher ma = Pattern.compile("\\d{1,2}-\\d{1,2}-(\\d{4})").matcher(value);
					if (ma.find()) {
						year = ma.group(1);
					}
				}
			}
			
			NodeList typeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_tbDocumentType"));
			if (typeList.size()>0) {
				String value = typeList.elementAt(0).toHtml();
				Matcher ma = Pattern.compile("value=\"([^\"]+)\"").matcher(value);
				if (ma.find()) {
					type = ma.group(1).trim();
					type = cleanType(type);
				} 
			} else {
				typeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_tbDocumentType"));
				if (typeList.size()>0) {
					type = typeList.elementAt(0).toPlainTextString().trim();
					type = cleanType(type);
				}
			}
			
			data.put("instrno", instrumentNumber);
			data.put("book", book);
			data.put("page", page);
			data.put("year", year);
			data.put("type", type);

			String instrOrBookPage = instrumentNumber;
			if (StringUtils.isEmpty(instrOrBookPage)) {
				instrOrBookPage = bookType + "-" + book + "-" + page;
			}
			accountId.append(instrOrBookPage);
			
			String typeLink = type.replaceAll("[^\\w]", "");
			String imageLink = "";
			if (hasImage()) {
				
				boolean hasImageButtonOrLink = false;
				String buttonValue = "";
				NodeList buttonList = nodeList.extractAllNodesThatMatch(new TagNameFilter("input"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_btnViewImage"));
				if (buttonList.size()>0) {
					buttonValue = buttonList.elementAt(0).toHtml().trim();
					if (buttonValue.contains("value=\"View Image\"") && !buttonValue.contains("disabled=\"disabled\"")) {
						hasImageButtonOrLink = true;
					}
				} else {
					Matcher ma = Pattern.compile("(?is)<a[^>]+>Image</a>").matcher(rsResponse);
					if (ma.find()) {
						hasImageButtonOrLink = true;
					}
				}
				
				if (hasImageButtonOrLink) {
					String lastURL = "";
					URI uri = Response.getLastURI();
					if (uri!=null) {
						lastURL = uri.getURI();
					} else {
						NodeList newList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("id", "lastURL"));
						if (newList.size()>0) {
							lastURL = newList.elementAt(0).toPlainTextString().trim();
						}
					}
					
					bookType = getCodeFromBookType(searchId, bookType);
					
					imageLink = CreatePartialLink(HTTPRequest.POST) + lastURL + "&img=true&instrno=" + instrumentNumber + "&type=" + typeLink +
						"&bookType=" + bookType + "&book=" + book + "&page=" + page;
				}
				
			} else if (hasImageFromTS()) {
				imageLink = "viewImage.asp?instrno=" + instrumentNumber + "&book=" + book + "&page=" + page + "&type=" + typeLink;
			}
			
			String sFileLink = instrOrBookPage + ".tiff";
			addImageLinkInResponse(Response, imageLink, sFileLink);
			
			/* If from memory - use it as is */
			if (!rsResponse.contains("<html")) {
				return rsResponse;
			}
			
			htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			nodeList = htmlParser.parse(null);
			tdList = nodeList.extractAllNodesThatMatch(new TagNameFilter("td"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "tableMain_Content"));
			
			String detailsString = tdList.elementAt(0).toHtml();
			
			//add alternate instrument number in a hidden span
			detailsString = detailsString.replaceFirst("(?is)(<td[^>]+class=\"DocumentTitle\"[^>]*>[^<]+</td>\\s*<td>)",
				"$1<span id=\"alternativeInstrumentNumber\" style=\"visibility:hidden;display:none\">" + trackingID + "</span>");
			
			//replace text areas with spans
			Matcher ma1 = Pattern.compile("(?is)(.*?)<textarea[^>]*(id=\"[^\"]+\")[^>]*>(.*?)</textarea>(.*)").matcher(detailsString);
			if (ma1.find()) {
				detailsString = ma1.group(1) + "<span " + ma1.group(2) + ">" + 
					ma1.group(3).replaceFirst("\r\n", "").replaceAll("\r\n", "<br>") + "</span>" + ma1.group(4); 
			}
			
			//replace inputs with spans
			detailsString = detailsString.replaceAll("(?is)<input[^>]+type=\"text\"[^>]+value=\"([^\"]*)\"[^>]+id=\"([^\"]+)\"[^>]*>", 
				"<span id=\"$2\">$1</span>");
			detailsString = detailsString.replaceAll("(?is)<input[^>]+type=\"text\"[^>]+id=\"([^\"]+)\"[^>]*>", 
				"<span id=\"$1\"></span>");
			
			String partialLink = CreatePartialLink(TSConnectionURL.idGET);
			
			//add image link
			detailsString = addImageLinkInDetails(detailsString, imageLink);
			
			//remove unnecessary text
			detailsString = detailsString.replaceAll("(?is)Requested By", "");
			
			//replace links for cross-references
			Matcher ma2 = Pattern.compile("(?is)(<a[^>]+href=\")([^\"]+DK=)([^\"]+)(\">)([^<]+)(</a>)").matcher(detailsString);
			while (ma2.find()) {
				if (!ma2.group(3).contains("&img=")) {	//image link
					String relInstrNo = ma2.group(5);
					if (!relInstrNo.matches("\\d+(-\\d+)?") && !relInstrNo.matches("[^-]+-([^-]+)-([^\"]+)")) {	//replace instrument number of related document
						relInstrNo = ma2.group(3);
					}
					detailsString = detailsString.replace(ma2.group(0), 
						ma2.group(1) + partialLink + ma2.group(2) + ma2.group(3) + ma2.group(4) + relInstrNo + ma2.group(6));
				}
			}
			
			details.append(detailsString);

			return details.toString();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "";
	}
	
	protected boolean hasImage() {
		return false;
	}
	
	protected boolean hasImageFromTS() {
		return false;
	}
	
	protected void addImageLinkInResponse(ServerResponse Response, String imageFakeLink, String sFileLink) {}
	
	protected String addImageLinkInDetails(String detailsString, String imageFakeLink) {
		detailsString = detailsString.replaceAll("(?is)Page Count", "");
		detailsString = detailsString.replaceAll("(?is)<span[^>]+id=\"MainContent_searchMainContent_ctl00_tbPageCount\"[^>]*>[^<]+</span>", "");
		detailsString = detailsString.replaceAll("(?is)View Image", "");
		detailsString = detailsString.replaceAll("(?is)Image Not Available", "");
		return detailsString;
	}
	
	protected String getNavigationLinks(String response) {

		try {
			
			org.htmlparser.Parser parser = org.htmlparser.Parser.createParser(response, null);
			NodeList nodeList = parser.parse(null);
			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "Table13"), true);
			
			if (mainTableList.size()>0) {
				TableTag mainTable = (TableTag)mainTableList.elementAt(0);
				String table = mainTable.toHtml();
				String partialLink = CreatePartialLink(TSConnectionURL.idGET);
				table = table.replaceAll("(?is)href=\"([^\"]+)\"", "href=\"" + partialLink + "$1\""); 
				return table;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		return "";
	}
	
	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		int numberOfUncheckedElements = 0;

		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(table, null);
			NodeList nodeList = htmlParser.parse(null);

			NodeList mainTableList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_Table2"), true);
			
			if (mainTableList.size()==0) {
				return intermediaryResponse;
			}

			TableTag tbl = (TableTag) mainTableList.elementAt(0);
			TableRow[] rows = tbl.getRows();

			int index = 1;
			int i = 1;
			while (i<rows.length) {
				
				int len = 6;
				int currentLen = rows[i].getColumnCount();
				if (currentLen!=len) {
					continue;
				}
				
				List<List<String>> trs = new ArrayList<List<String>>();
				
				//first row
				List<String> tds = new ArrayList<String>(); 
				for (int j=0;j<len;j++) {
					if (j==1) {	//if Reception number is missing (e.g. Tracking ID 265567)
						String instNo = rows[i].getColumns()[j].toPlainTextString();
						if ("".equals(instNo)) {	//if Reception number is missing (e.g. Tracking ID 265567)
							tds.add(rows[i].getColumns()[j].toHtml()
								.replaceFirst("(?is)(<td>\\s*<a[^>]+href=\"[^\"]+DK=)([^\"]+)(\">\\s*)(</a>\\s*</td>)", "$1$2$3$2$4"));
						} else if ("sensitive".equalsIgnoreCase(instNo)) {		//if Reception number is missing (e.g. Tracking ID 55741)
							tds.add(rows[i].getColumns()[j].toHtml()
								.replaceFirst("(?is)(<td>\\s*<a[^>]+href=\"[^\"]+DK=)([^\"]+)(\">)[^<]+(</a>\\s*</td>)", "$1$2$3$2$4"));
						} else {
							tds.add(rows[i].getColumns()[j].toHtml());
						}
					} else {
						tds.add(rows[i].getColumns()[j].toHtml());
					}
				}
				trs.add(tds);
				i++;
				
				//concatenate rows which belong to the same document
				while (i<rows.length && !"top".equalsIgnoreCase(rows[i].getAttribute("valign"))) {
					tds = new ArrayList<String>(); 
					for (int j=0;j<len;j++) {
						tds.add(rows[i].getColumns()[j].toHtml());
					}
					trs.add(tds);
					i++;
				}
				
				StringBuilder sb = new StringBuilder();
				for (int j=0;j<len;j++) {
					int rowsPerColumn = 0;
					for (int k=0;k<trs.size();k++) {
						if (!trs.get(k).get(j).matches("(?is)\\s*<td>\\s*</td>\\s*")) {
							rowsPerColumn++;
						}
					}
					if (rowsPerColumn==1) {
						if (j==len-2 || j==len-1) {		//'Document Name' and 'Name Type' columns
							sb.append("<td><table><tr>").append(trs.get(0).get(j)).append("</tr></table></td>");
						} else {
							sb.append(trs.get(0).get(j));
						}
					} else {
						sb.append("<td><table>");
						for (int k=0;k<trs.size();k++) {
							sb.append("<tr>").append(trs.get(k).get(j)).append("</tr>");
						}
						sb.append("</table></td>");
					}
				}
				
				ResultMap m = ro.cst.tsearch.servers.functions.GenericCountyRecorderRO.parseIntermediaryRow(sb.toString(), searchId);
				
				String tmpGrantor = (String)m.get("tmpGrantor");
				String tmpGrantee = (String)m.get("tmpGrantee");
				m.remove("tmpGrantor");
				m.remove("tmpGrantee");
				int seq = getSeq();
				if (!StringUtils.isEmpty(tmpGrantor)) {
					mSearch.setAdditionalInfo(getCurrentServerName() + ":grantor:" + seq, tmpGrantor);
				}
				if (!StringUtils.isEmpty(tmpGrantee)) {
					mSearch.setAdditionalInfo(getCurrentServerName() + ":grantee:" + seq, tmpGrantee);
				}
				
				String docLink = "";
				Matcher ma = Pattern.compile("href=\"([^\"]+)\"").matcher(sb.toString());
				if (ma.find()) {
					docLink = CreatePartialLink(TSConnectionURL.idGET) + ma.group(1) + "&seq=" + seq;
				}
				
				String rowString = sb.toString();
				rowString = rowString.replaceAll("(?is)href=\"([^\"]+)\"", "href=\"" + docLink + "\"");
					
				ParsedResponse currentResponse = new ParsedResponse();
				currentResponse.setPageLink(new LinkInPage(docLink, docLink, TSServer.REQUEST_SAVE_TO_TSD));
				
				Bridge bridge = new Bridge(currentResponse, m, searchId);
				DocumentI document = (RegisterDocumentI) bridge.importData();
				currentResponse.setDocument(document);
				
				String checkBox = "checked";
				String instrNo = org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName()));
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("instrno", instrNo);
				data.put("type", org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName())));
				data.put("book", org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.BOOK.getKeyName())));
				data.put("page", org.apache.commons.lang.StringUtils.defaultString((String)m.get(SaleDataSetKey.PAGE.getKeyName())));
				
				if (isInstrumentSaved(instrNo, document, data, false)) {
					checkBox = "saved";
				} else {
					numberOfUncheckedElements++;
					LinkInPage linkInPage = new LinkInPage(docLink, docLink, TSServer.REQUEST_SAVE_TO_TSD);
					checkBox = "<input type='checkbox' name='docLink' value='" + docLink + "'>";
					currentResponse.setPageLink(linkInPage);
				}
				String rowType = "1";
				if (index%2==0) {
					rowType = "2";
				}
				String rowHtml = "<tr class=\"row" + rowType + "\"><td>" + checkBox + "</td>" + rowString + "</tr>";
				currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml);
				currentResponse.setOnlyResponse(rowHtml);
				intermediaryResponse.add(currentResponse);
				index++;
			
			}
			outputTable.append(table);
			SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return intermediaryResponse;
	}
	
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			map.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");
			
			NodeList instrumentNumberList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_tbReceptionNo"));
			if (instrumentNumberList.size()>0) {
				String instrNo = instrumentNumberList.elementAt(0).toPlainTextString().trim();
				instrNo = instrNo.trim().replaceFirst("^0+", "");
				if (!instrNoIsBookPage(instrNo, searchId)) {
					map.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrNo);
				}
			}
			
			NodeList bookPageList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_tbBookPage"));
			if (bookPageList.size()>0) {
				String value = bookPageList.elementAt(0).toPlainTextString().trim();
				Matcher ma = Pattern.compile(BOOK_PAGE_PATT).matcher(value);
				if (ma.find()) {
					map.put(SaleDataSetKey.BOOK_TYPE.getKeyName(), ma.group(1));
					map.put(SaleDataSetKey.BOOK.getKeyName(), ma.group(2));
					map.put(SaleDataSetKey.PAGE.getKeyName(), ma.group(3));
				}
			}
			
			NodeList recordedDateList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_tbReceptionDate"));
			if (recordedDateList.size()>0) {
				String value = recordedDateList.elementAt(0).toPlainTextString().trim();
				Matcher ma = Pattern.compile("(\\d{1,2}-\\d{1,2}-\\d{4})").matcher(value);
				if (ma.find()) {
					map.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), ma.group(1));
				}
			}
			
			NodeList documentTypeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_tbDocumentType"));
			if (documentTypeList.size()>0) {
				String type = documentTypeList.elementAt(0).toPlainTextString().trim();
				type = cleanType(type);
				map.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), type);
			}
			
			NodeList legalDescriptionList = nodeList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_tbDescription"));
			if (legalDescriptionList.size()>0) {
				String value = legalDescriptionList.elementAt(0).toHtml().replaceAll("</?span[^>]*>", "").replaceAll("(?is)<br>", "\r\n");
				value = StringEscapeUtils.unescapeXml(value);
				map.put(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName(), value);
			}
			
			String seq = "";
			LinkInPage lip = response.getParsedResponse().getPageLink();
			if (lip!=null) {
				String link = lip.getLink();
				seq = StringUtils.extractParameter(link, "seq=([^&?]*)");
			}
			List<String> grantorLst = new ArrayList<String>();
			List<String> granteeLst = new ArrayList<String>();
			if (!StringUtils.isEmpty(seq)) {
				String tmpGrantor = (String)mSearch.getAdditionalInfo(getCurrentServerName() + ":grantor:" + seq);
				String tmpGrantee = (String)mSearch.getAdditionalInfo(getCurrentServerName() + ":grantee:" + seq);
				if (!StringUtils.isEmpty(tmpGrantor)) {
					String[] list = tmpGrantor.split("<br>");
					grantorLst =  Arrays.asList(list);
				}
				if (!StringUtils.isEmpty(tmpGrantee)) {
					String[] list = tmpGrantee.split("<br>");
					granteeLst =  Arrays.asList(list);
				}
			}
			
			StringBuilder sbGrantor = new StringBuilder();
			StringBuilder sbGrantee = new StringBuilder(); 
			
			String label = "";
			NodeList grantorList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_Table2"));
			if (grantorList.size()>0) {
				TableTag table = (TableTag)grantorList.elementAt(0);
				Node parent = table.getParent();
				if (parent!=null) {
					Node prevSibling = parent.getPreviousSibling();
					if (prevSibling!=null) {
						label = prevSibling.toPlainTextString().trim();
						if (StringUtils.isEmpty(label)) {
							prevSibling = prevSibling.getPreviousSibling();
							if (prevSibling!=null) {
								label = prevSibling.toPlainTextString().trim();
							}	
						}
					}
				}
				 
				if ("Grantor".equalsIgnoreCase(label)) {
					for (int i=0;i<table.getRowCount();i++) {
						TableRow row = table.getRow(i);
						if (row.getColumnCount()==1) {
							sbGrantor.append(row.getColumns()[0].toPlainTextString().trim()).append("<br>");
						}
					}
				} else {
					for (int i=0;i<table.getRowCount();i++) {
						TableRow row = table.getRow(i);
						if (row.getColumnCount()==1) {
							String s = row.getColumns()[0].toPlainTextString().trim();
							if (grantorLst.contains(s)) {
								sbGrantor.append(s).append("<br>");
							} else if (granteeLst.contains(s)) {
								sbGrantee.append(s).append("<br>");
							} else {
								sbGrantor.append(s).append("<br>");
							}
						}
					}
				}
			}

			label = "";
			NodeList granteeList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_Table3"));
			if (granteeList.size()>0) {
				TableTag table = (TableTag)granteeList.elementAt(0);
				Node parent = table.getParent();
				if (parent!=null) {
					Node prevSibling = parent.getPreviousSibling();
					if (prevSibling!=null) {
						label = prevSibling.toPlainTextString().trim();
						if (StringUtils.isEmpty(label)) {
							prevSibling = prevSibling.getPreviousSibling();
							if (prevSibling!=null) {
								label = prevSibling.toPlainTextString().trim();
							}	
						}
					}
				}
				
				if ("Grantee".equalsIgnoreCase(label)) {
					for (int i=0;i<table.getRowCount();i++) {
						TableRow row = table.getRow(i);
						if (row.getColumnCount()==1) {
							sbGrantee.append(row.getColumns()[0].toPlainTextString().trim()).append("<br>");
						}
					}
				} else {
					for (int i=0;i<table.getRowCount();i++) {
						TableRow row = table.getRow(i);
						if (row.getColumnCount()==1) {
							String s = row.getColumns()[0].toPlainTextString().trim();
							if (grantorLst.contains(s)) {
								sbGrantor.append(s).append("<br>");
							} else if (granteeLst.contains(s)) {
								sbGrantee.append(s).append("<br>");
							} else {
								sbGrantor.append(s).append("<br>");
							}
						}
					}
				}
			}
			
			String tmpGrantor = sbGrantor.toString().replaceFirst("<br>$", "");
			if (!StringUtils.isEmpty(tmpGrantor)) {
				map.put("tmpGrantor", tmpGrantor);
			}
			String tmpGrantee = sbGrantee.toString().replaceFirst("<br>$", "");
			if (!StringUtils.isEmpty(tmpGrantee)) {
				map.put("tmpGrantee", tmpGrantee);
			}
			
			NodeList relatedDocList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_Table8"));
			if (relatedDocList.size()>0) {
				TableTag table = (TableTag)relatedDocList.elementAt(0);
				TableRow[] rows = table.getRows();
				@SuppressWarnings("rawtypes")
				List<List> bodyCR = new ArrayList<List>();
				List<String> line;

				for (int j=1;j<rows.length;j++) {
					if (rows[j].getColumnCount() == 3) {
						line = new ArrayList<String>();
						String instrNo = rows[j].getColumns()[0].toPlainTextString().trim();
						if (instrNoIsBookPage(instrNo, searchId)) {
							Matcher ma = Pattern.compile(GenericCountyRecorderRO.BOOK_PAGE_PATT).matcher(instrNo);
							if (ma.find()) {
								line.add("");	//instrument number
								line.add(ma.group(1));
								line.add(ma.group(2));
								line.add(ma.group(3));
							} else {
								line.add("");	//instrument number
								line.add("");	//book page type
								line.add("");	//book
								line.add("");	//page
							}
						} else {
							line.add(instrNo);
							line.add("");	//book page type
							line.add("");	//book
							line.add("");	//page
						}
						line.add(rows[j].getColumns()[1].toPlainTextString().trim());
						bodyCR.add(line);
					}
				}

				if (bodyCR.size() > 0) {
					String[] header = { CrossRefSetKey.INSTRUMENT_NUMBER.getShortKeyName(), CrossRefSetKey.BOOK_PAGE_TYPE.getShortKeyName(),
							CrossRefSetKey.BOOK.getShortKeyName(), CrossRefSetKey.PAGE.getShortKeyName(),
							CrossRefSetKey.INSTRUMENT_REF_TYPE.getShortKeyName()};
					ResultTable rt = GenericFunctions2.createResultTable(bodyCR, header);
					map.put("CrossRefSet", rt);
				}
			}
			
			String tractPatt = "TR\\s+([A-Z0-9]+)";
			String unitPatt = "UN\\s*(\\d+)";

			@SuppressWarnings("rawtypes")
			List<List> bodyPIS = new ArrayList<List>();
			List<String> line;
			NodeList legalList1 = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_Table1"));
			if (legalList1.size()>0) {
				TableTag table = (TableTag)legalList1.elementAt(0);
				TableRow[] rows = table.getRows();
				for (int j=1;j<rows.length;j++) {
					if (rows[j].getColumnCount() == 3) {
						line = new ArrayList<String>();
						line.add("");	//plat book
						line.add("");	//plat no
						String lot = rows[j].getColumns()[0].toPlainTextString();
						lot = lot.replaceFirst("^(?is).*?\\bOF\\s+(\\d+)", "$1");	//E12' OF 6 (CO Baca 403952)
						lot = lot.replaceFirst("^0+", "");
						Matcher ma1 = Pattern.compile(tractPatt).matcher(lot);
						if (ma1.matches()) {
							line.add("");	//lot
							line.add(ma1.group(1));	//tract
						} else {
							line.add(lot);	//lot
							line.add("");	//tract
						}
						String block = rows[j].getColumns()[1].toPlainTextString().trim().replaceFirst("^0+", "");
						Matcher ma2 = Pattern.compile(unitPatt).matcher(block);
						if (ma2.matches()) {
							line.add("");	//block
							line.add(ma2.group(1));	//unit
						} else {
							line.add(block);	//block
							line.add("");	//unit
						}
						line.add("");	//section
						line.add("");	//township
						line.add("");	//range
						String subdName = rows[j].getColumns()[2].toPlainTextString().trim(); 
						line.add(subdName);		//subdivision name	
						if (subdName.matches(".*\\bCONDO.*")) {
							line.add(subdName);		//condo
						} else {
							line.add("");
						}
						line.add("");	//parcel id parcel
						bodyPIS.add(line);
					}
				}
			}
			NodeList legalList2 = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_Table5"));
			if (legalList2.size()>0) {
				TableTag table = (TableTag)legalList2.elementAt(0);
				TableRow[] rows = table.getRows();
				for (int j=1;j<rows.length;j++) {
					if (rows[j].getColumnCount() == 3) {
						line = new ArrayList<String>();
						line.add("");	//plat book
						line.add("");	//plat no
						line.add("");	//lot
						line.add("");	//tract
						line.add("");	//block
						line.add("");	//unit
						line.add(rows[j].getColumns()[0].toPlainTextString().trim().replaceFirst("^0+", ""));	//section
						line.add(rows[j].getColumns()[1].toPlainTextString().trim().replaceFirst("^0+", ""));	//township
						line.add(rows[j].getColumns()[2].toPlainTextString().trim().replaceFirst("^0+", ""));	//range
						line.add("");	//subdivision name
						line.add("");	//condo
						line.add("");	//parcel id parcel
						bodyPIS.add(line);
					}
				}
			}
			
			NodeList parcelList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_Table4"));
			if (parcelList.size()>0) {
				TableTag table = (TableTag)parcelList.elementAt(0);
				for (int i=1;i<table.getRowCount();i++) {
					TableRow row = table.getRow(i);
					if (row.getColumnCount()>0) {
						line = new ArrayList<String>();
						line.add("");	//plat book
						line.add("");	//plat no
						line.add("");	//lot
						line.add("");	//tract
						line.add("");	//block
						line.add("");	//unit
						line.add("");	//section
						line.add("");	//township
						line.add("");	//range
						line.add("");	//subdivision name
						line.add("");	//condo
						line.add(row.getColumns()[0].toPlainTextString().trim());
						bodyPIS.add(line);
					}
				}
			}
			if (bodyPIS.size() > 0) {
				String[] header = {PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(),
						   PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(),
						   PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(),
					       PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(),
				           PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(),
				           PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(),
				           PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(),
				           PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(),
				           PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(),
				           PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(),
				           PropertyIdentificationSetKey.SUBDIVISION_COND.getShortKeyName(),
				           PropertyIdentificationSetKey.PARCEL_ID_PARCEL.getShortKeyName()};
				ResultTable rt = GenericFunctions2.createResultTable(bodyPIS, header);
				map.put("PropertyIdentificationSet", rt);
			}
			
			//test cases: AZ Greenlee 2007-00930, 2010-00514, 2012-00624
			NodeList addressList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "MainContent_searchMainContent_ctl00_Table6"));
			if (addressList.size()>0) {
				TableTag table = (TableTag)addressList.elementAt(0);
				if (table.getRowCount()>1) {
					TableRow row = table.getRow(1);
					if (row.getColumnCount()>0) {
						map.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), row.getColumns()[0].toPlainTextString().trim());
					}
					if (row.getColumnCount()>0) {
						String cityZip = row.getColumns()[1].toPlainTextString().trim();
						Matcher ma = Pattern.compile("(.*?)\\s*,\\s*[A-Z]{2}(\\s+\\d{5})?").matcher(cityZip);
						if (ma.find()) {
							map.put(PropertyIdentificationSetKey.CITY.getKeyName(), ma.group(1).trim());
							String zip = ma.group(2);
							if (!StringUtils.isEmpty(zip)) {
								map.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
							}
						} else {
							map.put(PropertyIdentificationSetKey.CITY.getKeyName(), cityZip);
						}
					}
				}
			}
			
			ro.cst.tsearch.servers.functions.GenericCountyRecorderRO.parseNames(map);
			ro.cst.tsearch.servers.functions.GenericCountyRecorderRO.parseAddress(map);
			parseLegal(map);
			map.removeTempDef();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	public void parseLegal(ResultMap resultMap) {
		
		String legalDescription = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
		
		List<String> line;
		line = new ArrayList<String>();
		
		String platBookPagepattern = "(?is)\\bB(\\d+)\\s+PG(\\d+)\\b";
		Matcher ma = Pattern.compile(platBookPagepattern).matcher(legalDescription);
		if (ma.find()) {
			line.add(ma.group(1));
			line.add(ma.group(2));
		} else {
			line.add("");
			line.add("");
		}
		
		String lotExpr1 = "(?is)\\bLOTS\\s+(\\d+(?:-\\d+)+)";
		List<String> lot1 = RegExUtils.getMatches(lotExpr1, legalDescription, 1);
		legalDescription = legalDescription.replaceAll(lotExpr1, " LOT ");
		StringBuilder subdivisionLot = new StringBuilder();
		for (int i=0; i<lot1.size(); i++) {
			subdivisionLot.append(lot1.get(i).replaceAll("-", " ")).append(" ");
		}
		String lotExpr2 = "(?is)\\bLOTS\\s+(\\d+(?:[\\s,]+\\d+)*(?:\\s*,)?\\s*(&\\s*\\d+)?)";
		List<String> lot2 = RegExUtils.getMatches(lotExpr2, legalDescription, 1);
		legalDescription = legalDescription.replaceAll(lotExpr2, " LOT ");
		for (int i=0; i<lot2.size(); i++) {
			subdivisionLot.append(lot2.get(i).replaceAll("[,&]", " ")).append(" ");
		}
		String lotExpr3 = "(?is)LOTS?\\s+(\\d+(?:\\s+\\d+)*)";
		List<String> lot3 = RegExUtils.getMatches(lotExpr3 + "(?:\\r\\n)?", legalDescription, 1);
		legalDescription = legalDescription.replaceAll(lotExpr3, " LOT ");
		for (int i=0; i<lot3.size(); i++) {
			subdivisionLot.append(lot3.get(i)).append(" ");
		}
		String lotExpr4 = "(?is)\\bLOTS?\\s+(\\d+(\\s+(?:AND|&)\\s+\\d+)?)";
		List<String> lot4 = RegExUtils.getMatches(lotExpr4, legalDescription, 1);
		legalDescription = legalDescription.replaceAll(lotExpr4, " LOT ");
		for (int i=0; i<lot4.size(); i++) {
			subdivisionLot.append(lot4.get(i).replaceAll("(AND|&)", " ")).append(" ");
		}
		String subdivisionLotString = subdivisionLot.toString().trim();
		if (subdivisionLotString.length() != 0) {
			subdivisionLotString = LegalDescription.cleanValues(subdivisionLotString, false, true);
			subdivisionLotString = sortValues(subdivisionLotString);
			line.add(subdivisionLotString);
		} else {
			line.add("");
		}
		
		line.add("");	//tract
		
		String blockExpr = "(?is)\\bBL?K\\s+(\\d+|[A-Z](?:-\\d+)?)";
		List<String> block = RegExUtils.getMatches(blockExpr, legalDescription, 1);
		legalDescription = legalDescription.replaceAll(blockExpr, " BLOCK ");
		StringBuilder subdivisionBlock = new StringBuilder();
		for (int i=0; i<block.size(); i++) {
			subdivisionBlock.append(block.get(i)).append(" ");
		} 
		String subdivisionBlockString = subdivisionBlock.toString().trim();
		if (subdivisionBlockString.length() != 0) {
			subdivisionBlockString = LegalDescription.cleanValues(subdivisionBlockString, false, true);
			subdivisionBlockString = sortValues(subdivisionBlockString);
			line.add(subdivisionBlockString);
		} else {
			line.add("");
		}
		
		String unitExpr = "\\bUNITS?\\s+(([A-Z]+-?\\d+|\\d+[A-Z]+)(,\\s*([A-Z]+-?\\d+|\\d+[A-Z]+))*)";
		List<String> unit = RegExUtils.getMatches("(?is)"+unitExpr, legalDescription, 1);
		legalDescription = legalDescription.replaceAll("(?is)(.*:\\s*)"+unitExpr, " UNIT ");
		StringBuilder subdivisionUnit = new StringBuilder();
		for (int i=0; i<unit.size(); i++) {
			subdivisionUnit.append(unit.get(i)).append(" ");
		} 
		String subdivisionUnitString = subdivisionUnit.toString().trim();
		if (subdivisionUnitString.length() != 0) {
			subdivisionUnitString = LegalDescription.cleanValues(subdivisionUnitString, false, true);
			subdivisionUnitString = sortValues(subdivisionUnitString);
			line.add(subdivisionUnitString);
		} else {
			line.add("");
		}
		
		String strExpr = "(?is)\\bS(?:EC)?\\s*(\\d+(?:NE|NW|SE|SW|N|S|E|W)?)\\s+T\\s*(\\d+(?:NE|NW|SE|SW|N|S|E|W)?)\\s+R(\\d+(?:NE|NW|SE|SW|N|S|E|W)?)";
		Matcher ma1 = Pattern.compile(strExpr).matcher(legalDescription);
		if (ma1.find()) {
			line.add(ma1.group(1).replaceFirst("^0+", ""));
			line.add(ma1.group(2).replaceFirst("^0+", ""));
			line.add(ma1.group(3).replaceFirst("^0+", ""));
		} else {
			line.add("");
			line.add("");
			line.add("");
		}
		
		Matcher ma2 = Pattern.compile("(?i)(?:LOT|UNIT)(.+)").matcher(legalDescription);
		if (ma2.find()) {
			String subdName = ma2.group(1);
			subdName = subdName.replaceAll("(?is)\\b(PT\\s+)?LOT\\b", "");
			subdName = subdName.replaceAll("(?is)\\b(PT\\s+)?BLOCK\\b", "");
			subdName = subdName.replaceAll("(?is)\\bMAP\\s+\\d+\\b", "");
			subdName = subdName.replaceAll("(?is)\\s+AKA\\s*$", "");
			subdName = subdName.replaceAll("(?is)\\b(TO\\s+THE\\s+)?CITY\\s+OF\\s+.+$", "");	//ATENCIO ADDITION CITY OF WALSENBURG (instrNo 372260)
			Matcher mm = Pattern.compile("(?is)(.+?)\\bOF\\b.*").matcher(subdName);
			if (mm.find()) {
				if (!mm.group(1).trim().matches("(?is)CITY|TOWN")) {
					subdName = mm.group(1); 
				}
			}
			subdName = subdName.replaceAll("(?is)\\s*\\b(NE|SW|SE|SW|N|S|E|W)\\d+((NE|SW|SE|SW|N|S|E|W)\\d+)?\\b.*", "");
			subdName = subdName.replaceAll("^(\\s*[,-])*\\s*", "");
			subdName = subdName.replaceAll("\\s*[,-]\\s*$", "");
			subdName = subdName.replaceAll("(?is)\\s*\\b(NE|SW|SE|SW|N|S|E|W)\\W((NE|SW|SE|SW|N|S|E|W)\\W)?\\b.*", "");
			subdName = subdName.trim();
			if (subdName.matches(unitExpr)) {
				subdName = "";
			} 
			
			if (!StringUtils.isEmpty(subdName)) {
				String[] split = legalDescription.split("\r\n");
				//instrNo 338743
				if (split.length==2 && split[0].endsWith(subdName) && split[1].matches("NOTES:.+") && 
						!split[1].matches(".*\\bLOT\\b") && !split[1].matches(".*\\bBLOCK\\b") &&
						!split[1].matches(".+,\\s*[A-Z]{2}\\s+\\d{5}")) {
							subdName += " " + split[1].replaceAll("NOTES:", "");
							subdName = subdName.replaceAll("\\s{2,}", " ").trim();
				} 
			} else if (StringUtils.isEmpty(subdName)) {
				Matcher ma3 = Pattern.compile("(?im)^REMARKS:(.+)$").matcher(legalDescription);	//instrNo 359089
				if (ma3.find()) {
					subdName = ma3.group(1);
					subdName = subdName.replaceAll("(?is)\\b(TO\\s+THE\\s+)?CITY\\s+OF\\s+.+$", "").trim();
					if (subdName.matches(".*\\bLOT\\b") || subdName.matches(".*\\bBLOCK\\b")) {
						subdName = "";
					}
				}
			}
			
			if (!StringUtils.isEmpty(subdName)) {
				subdName = correctSubdivisionName(subdName);
				line.add(subdName);
				if (subdName.matches(".*\\bCONDO.*")) {
					line.add(subdName);		//condo
				} else {
					line.add("");
				}
			} else {
				line.add("");
				line.add("");
			}
		} else {
			line.add("");
			line.add("");
		}
		line.add("");	//parcel id parcel
		
		String lt = line.get(2);
		String blk = line.get(4);
		String subd = line.get(9);
		if (!StringUtils.isEmpty(lt) || !StringUtils.isEmpty(blk) || !StringUtils.isEmpty(subd)) {
			String[] split = lt.split("\\s+");
			int[] isEnabled = new int[split.length];
			for (int i=0;i<isEnabled.length;i++) {
				isEnabled[i] = 1;
			}
			split[0] = "";
			if (!StringUtils.isEmpty(lt)) {
				split = lt.split("\\s+");
			}
			ResultTable existingRT = (ResultTable)resultMap.get("PropertyIdentificationSet");
			if (existingRT!=null) {
				String[][] body = existingRT.getBody();
				for (int i=0;i<split.length;i++) {
					boolean found = false;
					for (int j=0;j<body.length;j++) {
						if (body[j].length==12 && split[i].equals(body[j][2]) && blk.equals(body[j][4]) && equalSubdivisions(subd,body[j][9])) {
							found = true;
							break;
						}
					}
					if (found) {
						isEnabled[i] = 0;
					}
				}
				boolean somethingLeft = false;
				StringBuilder sb = new StringBuilder();
				for (int i=0;i<isEnabled.length;i++) {
					if (isEnabled[i]==1) {
						somethingLeft = true;
						sb.append(split[i]).append(" ");
					}
				}
				if (somethingLeft) {
					String newLot = sb.toString();
					newLot = LegalDescription.cleanValues(newLot, false, true);
					line.set(2, newLot);
					line.set(4, blk);
					line.set(9, subd);
				} else {
					line.set(2, "");
					line.set(4, "");
					line.set(9, "");
				}
			}
		}
		
		
		String sec = line.get(6);
		String twp = line.get(7);
		String rng = line.get(8);
		if (!StringUtils.isEmpty(sec) || !StringUtils.isEmpty(twp) || !StringUtils.isEmpty(rng)) {
			ResultTable existingRT = (ResultTable)resultMap.get("PropertyIdentificationSet");
			if (existingRT!=null) {
				String[][] body = existingRT.getBody();
				for (int j=0;j<body.length;j++) {
					if (body[j].length==12 && sec.equals(body[j][6]) && twp.equals(body[j][7]) && rng.equals(body[j][8])) {
						line.set(6, "");
						line.set(7, "");
						line.set(8, "");
						break;
					}
				}
			}	
		}
		
		boolean hasSomething = false;
		for (int i=0;i<line.size();i++) {
			if (line.get(i).length()>0) {
				hasSomething = true;
				break;
			}
		}
		
		if (hasSomething) {
			List<List> bodyPIS = new ArrayList<List>();
			bodyPIS.add(line);
			String[] header = {PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(),
					           PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(),
					           PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(),
							   PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(),
			           		   PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(),
			           		   PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(),
			           		   PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(),
			           		   PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(),
					           PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(),
					           PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(),
					           PropertyIdentificationSetKey.SUBDIVISION_COND.getShortKeyName(),
					           PropertyIdentificationSetKey.PARCEL_ID_PARCEL.getShortKeyName()};
			ResultTable rt = GenericFunctions2.createResultTable(bodyPIS, header);
			
			ResultTable existingRT = (ResultTable)resultMap.get("PropertyIdentificationSet");
			if (existingRT!=null) {
				try {
					rt = ResultTable.joinVertical(rt, existingRT, true);
					resultMap.put("PropertyIdentificationSet", rt);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				resultMap.put("PropertyIdentificationSet", rt);
			}
		}
	}
	
	public static boolean equalSubdivisions(String s1, String s2) {
		//AMENDED DUNCAN TOWNSITE and DUNCAN TOWNSITE
		if (s1.replaceAll("(?is)\\bAMENDED\\b", "").trim().equals(s2.replaceAll("(?is)\\bAMENDED\\b", "").trim())) {
			return true;
		}
		return s1.equals(s2);
	}
	
	public static String sortValues(String s) {
		StringBuilder res = new StringBuilder();
		String[] split = s.split("\\s+");
		List<String> digits = new ArrayList<String>();
		List<String> nondigits = new ArrayList<String>();
		for (int i=0;i<split.length;i++) {
			if (split[i].matches("\\d+(-\\d+)?")) {
				digits.add(split[i]);
			} else {
				nondigits.add(split[i]);
			}
		}
		Collections.sort(nondigits);
		for (String el: digits) {
			res.append(el).append(" ");
		}
		for (String el: nondigits) {
			res.append(el).append(" ");
		}
		
		return res.toString().trim();
	}
	
	public static void addUnitName(List<String> possibleCorrection, String subdivisionName) {
		String newSubdivisionName = subdivisionName.replaceAll("(?is)\\bUNIT\\s+", "#");
		if (!possibleCorrection.contains(newSubdivisionName)) {
			possibleCorrection.add(newSubdivisionName);
		}
	}
	
	public static void addUnitValue(List<String> possibleCorrection, String subdivisionName) {
		String newSubdivisionName = subdivisionName.replaceAll("(?is)\\b(UNIT\\s+|#)([A-Z]+)(\\d+)", "$1$2-$3");
		if (!possibleCorrection.contains(newSubdivisionName)) {
			possibleCorrection.add(newSubdivisionName);
			addUnitName(possibleCorrection, newSubdivisionName);
		}
	}
	
	public String correctSubdivisionName(String subdivisionName) {
		
		if (!StringUtils.isEmpty(subdivisionName)) {
			
			List<String> possibleCorrection = new ArrayList<String>();
			
			possibleCorrection.add(subdivisionName);
			String newSubdivisionName = subdivisionName.replaceAll("(?is)\\bCONDO\\b", "CONDOMINIUM");
			if (!possibleCorrection.contains(newSubdivisionName)) {
				possibleCorrection.add(newSubdivisionName);
				addUnitName(possibleCorrection, newSubdivisionName);
				addUnitValue(possibleCorrection, newSubdivisionName);
			}
			
			//FORT CONDO -> FORT CONDOMINIUMS
			newSubdivisionName = subdivisionName.replaceAll("(?is)\\bCONDO\\b", "CONDOMINIUMS");
			if (!possibleCorrection.contains(newSubdivisionName)) {
				possibleCorrection.add(newSubdivisionName);
				addUnitName(possibleCorrection, newSubdivisionName);
				addUnitValue(possibleCorrection, newSubdivisionName);
			}
			
			//COLO LAND & GRAZING RANCH UNIT CC-2 -> COLORADO LAND & GRAZING RANCH UNIT CC-2
			newSubdivisionName = subdivisionName.replaceAll("(?is)\\bCOLO\\b", "COLORADO");
			if (!possibleCorrection.contains(newSubdivisionName)) {
				possibleCorrection.add(newSubdivisionName);
				addUnitName(possibleCorrection, newSubdivisionName);
				addUnitValue(possibleCorrection, newSubdivisionName);
			}
			
			//ABEYTA CRK ACRES -> ABEYTA CREEK ACRES
			newSubdivisionName = subdivisionName.replaceAll("(?is)\\bCRK\\b", "CREEK");
			if (!possibleCorrection.contains(newSubdivisionName)) {
				possibleCorrection.add(newSubdivisionName);
				addUnitName(possibleCorrection, newSubdivisionName);
				addUnitValue(possibleCorrection, newSubdivisionName);
			}
			
			//SPANISH PEAKS FILING NO 4 -> SPANISH PEAKS FILING # 4
			newSubdivisionName = subdivisionName.replaceAll("(?is)\\bNO\\s*(\\d+)", "#$1");
			if (!possibleCorrection.contains(newSubdivisionName)) {
				possibleCorrection.add(newSubdivisionName);
				addUnitName(possibleCorrection, newSubdivisionName);
				addUnitValue(possibleCorrection, newSubdivisionName);
			}
			
			//ASPEN MTN RANCH -> ASPEN MOUNTAIN RANCH
			newSubdivisionName = subdivisionName.replaceAll("(?is)\\bMTN\\b", "MOUNTAIN");
			if (!possibleCorrection.contains(newSubdivisionName)) {
				possibleCorrection.add(newSubdivisionName);
				addUnitName(possibleCorrection, newSubdivisionName);
				addUnitValue(possibleCorrection, newSubdivisionName);
			}
			
			//PINKERTON ADDITION TO THE -> PINKERTON ADDITION  
			newSubdivisionName = subdivisionName.replaceAll("(?is)\\s+TO\\s+THE(\\s+TOWN\\s+OF.+)?$", "");
			if (!possibleCorrection.contains(newSubdivisionName)) {
				possibleCorrection.add(newSubdivisionName);
				addUnitName(possibleCorrection, newSubdivisionName);
				addUnitValue(possibleCorrection, newSubdivisionName);
			}
			
			//FILING 5, SILVER SPURS RANCH -> SILVER SPURS RANCH FILING #5
			newSubdivisionName = subdivisionName.replaceAll("(?is)^(FILING)\\s+(\\d+),(.+)", "$3 $1 #$2").trim();
			if (!possibleCorrection.contains(newSubdivisionName)) {
				possibleCorrection.add(newSubdivisionName);
				addUnitName(possibleCorrection, newSubdivisionName);
				addUnitValue(possibleCorrection, newSubdivisionName);
			}
			
			//WLSBG -> WALSENBURG 
			newSubdivisionName = subdivisionName.replaceAll("(?is)\\bWLSBG\\b", "WALSENBURG");
			if (!possibleCorrection.contains(newSubdivisionName)) {
				possibleCorrection.add(newSubdivisionName);
				addUnitName(possibleCorrection, newSubdivisionName);
				addUnitValue(possibleCorrection, newSubdivisionName);
			}
			
			//TOWN OF DUNCAN -> DUNCAN TOWNSITE 
			newSubdivisionName = subdivisionName.replaceAll("(?is)\\b(TOWN)\\s+OF\\s+(.+)", "$2 $1SITE");
			if (!possibleCorrection.contains(newSubdivisionName)) {
				possibleCorrection.add(newSubdivisionName);
				addUnitName(possibleCorrection, newSubdivisionName);
				addUnitValue(possibleCorrection, newSubdivisionName);
			}
			
			//J V HILL -> J.V.HILL SUBDIVISION 
			newSubdivisionName = subdivisionName.replaceAll("(?is)\\b([A-Z])\\s+([A-Z])\\s+([A-Z]+)", "$1.$2.$3");
			if (!possibleCorrection.contains(newSubdivisionName)) {
				possibleCorrection.add(newSubdivisionName);
				addUnitName(possibleCorrection, newSubdivisionName);
				addUnitValue(possibleCorrection, newSubdivisionName);
				String addSubdivisionName = newSubdivisionName + " SUBDIVISION";
				if (!possibleCorrection.contains(addSubdivisionName)) {
					possibleCorrection.add(addSubdivisionName);
					addUnitName(possibleCorrection, addSubdivisionName);
					addUnitValue(possibleCorrection, addSubdivisionName);
				}
			}
			
			//ADD -> ADDITION 
			newSubdivisionName = newSubdivisionName.replaceAll("(?is)\\bADD\\b", "ADDITION");
			if (!possibleCorrection.contains(newSubdivisionName)) {
				possibleCorrection.add(newSubdivisionName);
				addUnitName(possibleCorrection, newSubdivisionName);
				addUnitValue(possibleCorrection, newSubdivisionName);
			}
			
			newSubdivisionName += " ADDITION";
			if (!possibleCorrection.contains(newSubdivisionName)) {
				possibleCorrection.add(newSubdivisionName);
				addUnitName(possibleCorrection, newSubdivisionName);
				addUnitValue(possibleCorrection, newSubdivisionName);
			}
			
			//LAVETA -> LA VETA ADDITION
			newSubdivisionName = newSubdivisionName.replaceFirst("(?is)^(LA)(.+)$", "$1 $2");
			if (!possibleCorrection.contains(newSubdivisionName)) {
				possibleCorrection.add(newSubdivisionName);
				addUnitName(possibleCorrection, newSubdivisionName);
				addUnitValue(possibleCorrection, newSubdivisionName);
			}
			
			for (String s: possibleCorrection) {
				String result = getSubdivisionCodeFromName(s);
				if (!StringUtils.isEmpty(result)) {
					return s;
				}	
			}
		}
		
		return subdivisionName;
	}
	
	public static String cleanType(String type) {
		type = StringEscapeUtils.unescapeXml(type);
		return type;
	}
	
	@Override
	protected void setCertificationDate() {
		try {


			if (CertificationDateManager.isCertificationDateInCache(dataSite)){
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			} else{
	
				String html = "";
				HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
				try {
					html = ((ro.cst.tsearch.connection.http2.GenericCountyRecorderRO)site).getCertificationDateHtml(); 
				} catch (RuntimeException e) {
					e.printStackTrace();
				} finally {
					HttpManager.releaseSite(site);
				}
	
				if (StringUtils.isNotEmpty(html)) {
					HtmlParser3 parser = new HtmlParser3(html);
					String certDate = HtmlParser3.getNodeValue(parser, "Recording Date", 0, 1).trim();
					String oldFormat = "MM-dd-yyyy KK:mm a";
					String newFormat = "MM/dd/yyyy";
					SimpleDateFormat dateFormat = new SimpleDateFormat(oldFormat);
					Date certificationDate = dateFormat.parse(certDate);
					dateFormat.applyPattern(newFormat);
					certDate = dateFormat.format(certificationDate);
					
					if (certificationDate != null){
						CertificationDateManager.cacheCertificationDate(dataSite, certDate);
						getSearch().getSa().updateCertificationDateObject(dataSite, certificationDate);
					}
				}
				}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}
	
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		Search search = getSearch();
		
		TSServerInfoModule module = null;
		
		GenericMultipleLegalFilter defaultLegalFilter = new GenericMultipleLegalFilter(searchId);
		defaultLegalFilter.setAdditionalInfoKey(AdditionalInfoKeys.RO_LOOK_UP_DATA);
		defaultLegalFilter.setUseLegalFromSearchPage(true);
		
		FilterResponse defaultSingleLegalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
		LastTransferDateFilter lastTransferDateFilter = new LastTransferDateFilter(searchId);
    	GenericNameFilter defaultNameFilter = (GenericNameFilter) NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
		
		boolean lookupWasDoneWithInstrument = false;
		
		//search with references (instrument number) from AO/Tax like documents for finding Legal
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId);
		instrumentGenericIterator.enableInstrumentNumber();
				
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				"Search with references from AO/Tax like documents");
		module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
		if (isUpdate()) {
			module.addValidator(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module).getValidator());
		}
		module.addIterator(instrumentGenericIterator);
			
		if(!lookupWasDoneWithInstrument) {
			lookupWasDoneWithInstrument = !instrumentGenericIterator.createDerrivations().isEmpty();
		}
		
		modules.add(module);
		
		//search with references (book and page for every book type) from AO/Tax like documents for finding Legal	
		InstrumentGenericIterator bookPageGenericIterator = getBookPageIterator();
		bookPageGenericIterator.enableBookPage();
		
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				"Search with references from AO/Tax like documents");
		if (hasBookTypeIterator()) {
			module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE);
		}
		module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
		module.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
		if (isUpdate()) {
			module.addValidator(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module).getValidator());
		}
		module.addIterator(bookPageGenericIterator);
			
		if(!lookupWasDoneWithInstrument) {
			lookupWasDoneWithInstrument = !bookPageGenericIterator.createDerrivations().isEmpty();
		}
		
		modules.add(module);
		
		//if no references found on AO/Tax like documents, search with owners for finding Legal
		if(!lookupWasDoneWithInstrument) {
			addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS, null, 
					new FilterResponse[]{defaultNameFilter, lastTransferDateFilter}, 
					new DocsValidator[]{defaultSingleLegalFilter.getValidator()}, 
					new DocsValidator[]{defaultSingleLegalFilter.getValidator(),
										lastTransferDateFilter.getValidator()});
		}
		
		//search with legal
		LegalDescriptionIterator it = getLegalDescriptionIterator(!lookupWasDoneWithInstrument);
			
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX39));
		module.clearSaKeys();

		module.setSaKey(1, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		module.setSaKey(2, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		module.setIteratorType(11, FunctionStatesIterator.ITERATOR_TYPE_LOT);
		module.setIteratorType(12, FunctionStatesIterator.ITERATOR_TYPE_BLOCK);
		module.setIteratorType(13, FunctionStatesIterator.ITERATOR_TYPE_NAME_TYPE);
		module.setIteratorType(14, FunctionStatesIterator.ITERATOR_TYPE_SECTION);
		module.setIteratorType(15, FunctionStatesIterator.ITERATOR_TYPE_TOWNSHIP);
		module.setIteratorType(16, FunctionStatesIterator.ITERATOR_TYPE_RANGE);
		module.forceValue(17, "100");	//Results Per Page
			
		module.addValidator(defaultLegalFilter.getValidator());
		module.addValidator(lastTransferDateFilter.getValidator());
		module.addCrossRefValidator(defaultLegalFilter.getValidator());
		module.addCrossRefValidator(lastTransferDateFilter.getValidator());
		
		module.addIterator(it);
		modules.add(module);
		
		ArrayList<NameI> searchedNames = null;
		
		//search with owners
		if (hasOwner()) {
			searchedNames = addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS, null,
					new FilterResponse[]{defaultNameFilter, lastTransferDateFilter},
					new DocsValidator[]{defaultLegalFilter.getValidator()},
					new DocsValidator[]{defaultLegalFilter.getValidator(),
										lastTransferDateFilter.getValidator()});
		}
		
		
		//OCR last transfer - instrument number search with 'Search by Reception Number' module
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		module.clearSaKeys();
		module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
		if (isUpdate()) {
			module.addValidator(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module).getValidator());
		}
		module.addValidator(defaultLegalFilter.getValidator());
		modules.add(module);
		
		//OCR last transfer - book and page search
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		module.clearSaKeys();
		module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE);
		module.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
		module.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
		
		OcrOrBootStraperIterator ocrBPIteratoriterator = getOcrBookPageIterator();
		ocrBPIteratoriterator.setInitAgain(true);
	    module.addIterator(ocrBPIteratoriterator);
		if (isUpdate()) {
			module.addValidator(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module).getValidator());
		}
		module.addValidator(defaultLegalFilter.getValidator());
		modules.add(module);
		
		//search with extra owners from search page (for example added by OCR)	
		searchedNames = addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS, searchedNames,
				new FilterResponse[]{defaultNameFilter, lastTransferDateFilter},
				new DocsValidator[]{defaultLegalFilter.getValidator()},
				new DocsValidator[]{defaultLegalFilter.getValidator(),
									lastTransferDateFilter.getValidator()});
	    	
		//search with buyers
		if(hasBuyer()) {
			FilterResponse nameFilterBuyer = NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT, getSearch().getID(), null);
			
			addNameSearch(modules, serverInfo, SearchAttributes.BUYER_OBJECT, TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS, null,
					new FilterResponse[]{nameFilterBuyer, DoctypeFilterFactory.getDoctypeBuyerFilter(searchId), lastTransferDateFilter},
					new DocsValidator[]{defaultLegalFilter.getValidator()},
					new DocsValidator[]{defaultLegalFilter.getValidator(),
										lastTransferDateFilter.getValidator()});
		}
		
		serverInfo.setModulesForAutoSearch(modules);
	
	}
	
	protected String[] getBookTypeList(long searchId) {
		List<String> list = new ArrayList<String>();
		LinkedHashMap<String,String> map = getBookTypeMap(searchId);
		if (map!=null) {
			Set<String> keys = map.keySet();
			for (String key: keys) {
				list.add(key);
			}
		}
		return list.toArray(new String[list.size()]);
	}
	
	//to be overridden in subclasses, if necessary
	protected InstrumentGenericIterator getBookPageIterator() {
		
		InstrumentGenericIterator iterator = new InstrumentGenericIterator(searchId);
		String[] bookTypeList = getBookTypeList(searchId);
		if (bookTypeList.length>0) {
			iterator.setUseInstrumentType(true); 
			iterator.setForceInstrumentTypes(bookTypeList);
		}
		
		return iterator;
	}
	
	//to be overridden in subclasses, if necessary
	protected boolean hasBookTypeIterator() {
		return true;
	}
	
	//to be overridden in subclasses, if necessary
	protected OcrOrBootStraperIterator getOcrBookPageIterator() {
		return new OcrOrBootStraperIterator(searchId);
	}
	
	protected LegalDescriptionIterator getLegalDescriptionIterator(boolean lookupWasDoneWithName) {
		
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, lookupWasDoneWithName, false, getDataSite()) {

			private static final long serialVersionUID = -3336972059858512740L;

			@Override
			protected void loadDerrivation(TSServerInfoModule module, LegalStruct str) {
				for (Object functionObject : module.getFunctionList()) {
					if (functionObject instanceof TSServerInfoFunction) {
						TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
						switch (function.getIteratorType()) {
						case FunctionStatesIterator.ITERATOR_TYPE_LOT:
							function.setParamValue(str.getLot());
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_BLOCK:
							function.setParamValue(str.getBlock());
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_NAME_TYPE:
							String subdName = str.getAddition();
							subdName = correctSubdivisionName(subdName);
							subdName = getSubdivisionCodeFromName(subdName);
							function.setParamValue(subdName);
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_SECTION:
							function.setParamValue(str.getSection());
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_TOWNSHIP:
							function.setParamValue(str.getTownship());
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_RANGE:
							function.setParamValue(str.getRange());
							break;
						}
					}
				}
			}
			
		};
		
		it.setAdditionalInfoKey(AdditionalInfoKeys.RO_LOOK_UP_DATA);
		it.setEnableSubdividedLegal(false);
		it.setEnableSubdivision(true);
		it.setEnableTownshipLegal(true);
		
		return it;
	}

	protected ArrayList<NameI> addNameSearch(
			List<TSServerInfoModule> modules, 
			TSServerInfo serverInfo,
			String key, 
			String extraInformation,
			ArrayList<NameI> searchedNames, 
			FilterResponse[] filters,
			DocsValidator[] docsValidators,
			DocsValidator[] docsValidatorsCrossref) {
		return addNameSearch(modules, serverInfo, key, extraInformation, searchedNames, filters, docsValidators, docsValidatorsCrossref, null);
	}
	
	protected ArrayList<NameI> addNameSearch(
			List<TSServerInfoModule> modules, 
			TSServerInfo serverInfo,
			String key, 
			String extraInformation,
			ArrayList<NameI> searchedNames, 
			FilterResponse[] filters,
			DocsValidator[] docsValidators,
			DocsValidator[] docsValidatorsCrossref,
			TSServerInfoModule module) {
		
		if(module == null) {
    		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX39));
		}
    	
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, extraInformation);
		module.setSaObjKey(key);
		module.clearSaKeys();
		module.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
		module.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
		module.setIteratorType(7, FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
		module.setSaKey(1, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		module.setSaKey(2, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
		module.forceValue(17, "100");		//Results Per Page
		
		if(filters != null) {
			for (FilterResponse filterResponse : filters) {
				module.addFilter(filterResponse);
			}
		}
		addFilterForUpdate(module, true);
		if(docsValidators != null) {
			for (DocsValidator docsValidator : docsValidators) {
				module.addValidator(docsValidator);
			}
		}
		if(docsValidatorsCrossref != null) {
			for (DocsValidator docsValidator : docsValidatorsCrossref) {
				module.addCrossRefValidator(docsValidator);
			}
		}
		module.addCrossRefValidator(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, module).getValidator());
		
		ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
			.getConfigurableNameIterator(module, false, searchId, new String[] {"L;F;" });
		nameIterator.setInitAgain(true);		//initialize again after all parameters are set
		
		if (searchedNames!=null) {
			nameIterator.setSearchedNames(searchedNames);
		}
		searchedNames = nameIterator.getSearchedNames() ;
		
		module.addIterator(nameIterator);
		modules.add(module);
		
		return searchedNames;
	}
	
	@Override
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {

		ConfigurableNameIterator nameIterator = null;
		String endDate = new SimpleDateFormat("MM/dd/yyyy").format(new Date());

		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule module;
		GBManager gbm = (GBManager) sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

		for (String id : gbm.getGbTransfers()) {

			module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX39));
			module.setIndexInGB(id);
			module.setTypeSearchGB("grantor");
			module.clearSaKeys();
			module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);

			String date = gbm.getDateForSearch(id, "MM/dd/yyyy", searchId);
			if (date != null) {
				module.getFunction(1).forceValue(date);
			}
			module.setValue(2, endDate);
			module.forceValue(17, "100");

			module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			module.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
			module.setIteratorType(7, FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
			nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,
					new String[] { "L;F;M", "L;F;" });
			module.addIterator(nameIterator);
			module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
			module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
			modules.add(module);

			if (gbm.getNamesForBrokenChain(id, searchId).size() > 0) {
				module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX39));
				module.setIndexInGB(id);
				module.setTypeSearchGB("grantee");
				module.clearSaKeys();
				module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				
				date = gbm.getDateForSearchBrokenChain(id, "MM/dd/yyyy", searchId);
				if (date != null) {
					module.getFunction(1).forceValue(date);
				}
				module.setValue(2, endDate);
				module.forceValue(17, "100");
				
				module.setIteratorType(5, FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
				module.setIteratorType(6, FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
				module.setIteratorType(7, FunctionStatesIterator.ITERATOR_TYPE_MIDDLE_NAME_FAKE);
				module.addFilter(NameFilterFactory.getDefaultNameFilter(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId,
						new String[] { "L;F;M", "L;F;" });
				module.addIterator(nameIterator);
				module.addFilter(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module));
				module.addFilter(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module));
				modules.add(module);
			}
		}
		serverInfo.setModulesForGoBackOneLevelSearch(modules);
	}
	
	@Override
	public String saveSearchedParameters(TSServerInfoModule module) {
		
		String globalResult = null;
		
		if(ArrayUtils.contains(getModuleIdsForSavingLegal(), module.getModuleIdx())) {
			
			LegalI legalI = getLegalFromModule(module);
			StringBuilder fullLegal = new StringBuilder();
			
			if(legalI != null) {
			
				List<LegalI> alreadySavedLegals = getSearchAttributes().getForUpdateSearchLegalsNotNull(getServerID());
				
				ArrayList<LegalI> alreadySavedSubdividedLegal = new ArrayList<LegalI>();
				
				ArrayList<LegalI> alreadySavedTownshipLegal = new ArrayList<LegalI>(); 
				
				for (LegalI alreadySavedLegal : alreadySavedLegals) {
											
					if(alreadySavedLegal.hasSubdividedLegal()  ){
						alreadySavedSubdividedLegal.add(alreadySavedLegal);
					}
					if (alreadySavedLegal.hasTownshipLegal()){
						alreadySavedTownshipLegal.add(alreadySavedLegal);
					}
					
				}
				
				boolean addSubdivision = alreadySavedSubdividedLegal.isEmpty();
				boolean addTownship = alreadySavedTownshipLegal.isEmpty();
					
				if(legalI.hasSubdividedLegal() ){
					for (LegalI alreadySavedLegal : alreadySavedSubdividedLegal)
						if(!addSubdivision 
								&& !alreadySavedLegal.getSubdivision().equals(legalI.getSubdivision())) {
							addSubdivision = true;
						}	
				}
				
				if (legalI.hasTownshipLegal()){
					for (LegalI alreadySavedLegal : alreadySavedTownshipLegal)
						if(!addTownship 
								&& !alreadySavedLegal.getTownShip().equals(legalI.getTownShip())) {
							addTownship = true;
						}
				}
				
				if(addSubdivision && legalI.hasSubdividedLegal()) {
					LegalI toAdd = new Legal();
					toAdd.setSubdivision(legalI.getSubdivision());
					if(fullLegal.length() > 0) {
						fullLegal.append(" | ");
					}
					fullLegal.append(legalI.getSubdivision().shortFormString());
					alreadySavedLegals.add(toAdd);
					getSearchAttributes().addForUpdateSearchLegal(toAdd, getServerID());
				}
				
				if(addTownship && legalI.hasTownshipLegal()) {
					LegalI toAdd = new Legal();
					toAdd.setTownShip(legalI.getTownShip());
					if(fullLegal.length() > 0) {
						fullLegal.append(" | ");
					}
					fullLegal.append(legalI.getTownShip().shortFormString());
					alreadySavedLegals.add(toAdd);
					getSearchAttributes().addForUpdateSearchLegal(toAdd, getServerID());
				}
			}
			
			if(fullLegal.length() == 0) {
				SearchLogger.info("<br><font color='red'>NO</font> legal was saved from searched parameters for future automatic search<br>", searchId);
				globalResult = "NO legal was saved from searched parameters for future automatic search";
			} else {
				SearchLogger.info("<br><font color='green'><b>Saving</b></font> legal: [" + fullLegal.toString() + "] from searched parameters for future automatic search<br>", searchId);
				globalResult = "Saved legal: [" + fullLegal.toString() + "] from searched parameters for future automatic search";
			}
			
		} 
		
		if (ArrayUtils.contains(getModuleIdsForSavingName(), module.getModuleIdx())) {
			
			List<NameI> alreadySavedNames = getSearchAttributes().getForUpdateSearchGrantorNamesNotNull(getServerID());
			List<NameI> newNamesAdded = new ArrayList<NameI>();
			
			NameI candName = getNameFromModule(module);
			if(candName != null) {
				NameI toAdd = candName;
				String candString =  candName.getFirstName() + candName.getMiddleName() + candName.getSufix() + candName.getLastName();
				for (NameI reference : alreadySavedNames) {
					if(
							GenericNameFilter.isMatchGreaterThenScore(candName, reference, NameFilterFactory.NAME_FILTER_THRESHOLD)
							&&(candName.isCompany()==reference.isCompany())
					) {
						/*
						 * found same name - do not save it
						 */
						String refString = reference.getFirstName() + reference.getMiddleName() + reference.getSufix() + reference.getLastName();
						if(refString.length() <= candString.length()){
							if(newNamesAdded.contains(reference)) {
								newNamesAdded.remove(reference);
							}
							
							reference.setLastName(candName.getLastName());
							reference.setFirstName(candName.getFirstName());
							reference.setMiddleName(candName.getMiddleName());
							reference.setCompany(candName.isCompany());
							reference.setSufix(candName.getSufix());
							reference.setPrefix(candName.getPrefix());
							
							newNamesAdded.add(reference);
						}
						toAdd = null;
						break;	//no need to check other cases
					} 
				}
				if(toAdd != null) {
					alreadySavedNames.add(toAdd);
					newNamesAdded.add(toAdd);
				}
			}
			
			if(newNamesAdded.size() == 0) {
				SearchLogger.info("<br><font color='red'>NO</font> name was saved from searched parameters for future automatic search<br>", searchId);
				if(globalResult == null) {
					globalResult += "NO name was saved from searched parameters for future automatic search";
				} else {
					globalResult += "\nNO name was saved from searched parameters for future automatic search";
				}
				
			} else {
				for (NameI nameI : newNamesAdded) {
					NameFormaterI nf = new NameFormater(PosType.LFM, TitleType.NO_CHANGE);
					String nameFormatted = nf.format(nameI);
					SearchLogger.info("<br><font color='green'><b>Saving</b></font> name: [" + nameFormatted + "] from searched parameters for future automatic search<br>", searchId);
					if(globalResult == null) {
						globalResult += "Saving name: [" + nameFormatted + "] from searched parameters for future automatic search";
					} else {
						globalResult += "\nSaving name: [" + nameFormatted + "] from searched parameters for future automatic search";
					}
				}
			}
			
		} 
		
		return globalResult;
	}
	
	@Override
	protected int[] getModuleIdsForSavingName() {
		return new int[]{TSServerInfo.MODULE_IDX39};
	}
	
	@Override
	protected NameI getNameFromModule(TSServerInfoModule module) {
		NameI name = new Name();
		if (module.getModuleIdx() == TSServerInfo.MODULE_IDX39 && module.getFunctionCount() > 18) {
			String nameType = module.getFunction(18).getParamValue();
			String lastName = module.getFunction(5).getParamValue();
			String firstName = module.getFunction(6).getParamValue();
			String middleName = module.getFunction(7).getParamValue();
			String businessName = module.getFunction(9).getParamValue();
			boolean found = false;
			if ("radioDocName1".equals(nameType)) {
				if (!StringUtils.isEmpty(lastName)) {
					name.setLastName(lastName);
					found = true;
				}
				if (!StringUtils.isEmpty(firstName)) {
					name.setFirstName(firstName);
					found = true;
				}
				if (!StringUtils.isEmpty(middleName)) {
					name.setMiddleName(middleName);
					found = true;
				}
				if (found) {
					return name;
				}
			} else if ("radioDocName2".equals(nameType)) {
				if (!StringUtils.isEmpty(businessName)) {
					name.setLastName(businessName);
					name.setCompany(true);
					found = true;
				}
				if (found) {
					return name;
				}
			}
		}
		return null;
	}

	@Override
	protected int[] getModuleIdsForSavingLegal() {
		return new int[]{TSServerInfo.MODULE_IDX39};
	}
	
	@Override
	protected LegalI getLegalFromModule(TSServerInfoModule module) {
		LegalI legal = new Legal();

		if (module.getModuleIdx() == TSServerInfo.MODULE_IDX39 && module.getFunctionCount() > 16) {
			
			SubdivisionI subdivision = new Subdivision();
			subdivision.setLot(module.getFunction(11).getParamValue());
			subdivision.setBlock(module.getFunction(12).getParamValue());
			String subdName = module.getFunction(13).getParamValue();
			if (!"-1".equals(subdName)) {
				String htmlformat = module.getFunction(13).getHtmlformat();
				Matcher ma = Pattern.compile("(?is)<option\\s+value=\"" + subdName + "\">([^<]+)</option>").matcher(htmlformat);
				if (ma.find()) {	//get name from code
					subdivision.setName(ma.group(1));
				}
			}
			
			TownShipI townShip = new TownShip();
			townShip.setSection(module.getFunction(14).getParamValue());
			townShip.setTownship(module.getFunction(15).getParamValue());
			townShip.setRange(module.getFunction(16).getParamValue());
			
			legal = new Legal();
			legal.setSubdivision(subdivision);
			legal.setTownShip(townShip);
			
		}
		
		return legal;
	}

	/*
	 * to be overridden in subclasses, if necessary
	 */
	@Override
	public List<TSServerInfoModule> getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		
		if(restoreDocumentDataI == null) {
			return null;
		}
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module = null;
		String instrumentNumber = restoreDocumentDataI.getInstrumentNumber();
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		
		if (StringUtils.isNotEmpty(instrumentNumber)) {
			module = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.forceValue(0, instrumentNumber);
			module.getFilterList().clear();
			modules.add(module);
		}
		
		if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			String bookTypeList[] = getBookTypeList(searchId);
			for (String bpType: bookTypeList) {
				module = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				module.forceValue(0, bpType);
				module.forceValue(1, book);
				module.forceValue(2, page);
				modules.add(module);
			}
		}	
			
		//module for document with "Sensitive" instrument number
		//this type of documents are recovered using Tracking ID module 
		if (StringUtils.isNotEmpty(instrumentNumber)) {
			module = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.MODULE_IDX38));
			module.forceValue(0, instrumentNumber);
			module.getFilterList().clear();
			modules.add(module);
		}
		
		return modules;
	}
	
	@Override
	protected void logSearchBy(TSServerInfoModule module, Map<String, String> params){
    	
    	if(module.isVisible() || "GB_MANAGER_OBJECT".equals(module.getSaObjKey())) {//B 4511
        
	    	// get parameters formatted properly
	        Map<String,String> moduleParams = params;
	        if(moduleParams == null){
	        	moduleParams = module.getParamsForLog();
	        }
	        Search search = getSearch();
	        // determine whether it's an automatic search
	        boolean automatic = (search.getSearchType() != Search.PARENT_SITE_SEARCH) 
	        		|| (GPMaster.getThread(searchId) != null);
	        boolean imageSearch = module.getLabel().equalsIgnoreCase("image search") || 
	                              module.getModuleIdx() == TSServerInfo.IMG_MODULE_IDX;
	        
	        // create the message
	        StringBuilder sb = new StringBuilder();
	        SearchLogFactory sharedInstance = SearchLogFactory.getSharedInstance();
	        SearchLogPage searchLogPage = sharedInstance.getSearchLogPage(searchId);
	        sb.append("</div>");
	        
	        Object additional = GetAttribute("additional");
			if(Boolean.TRUE != additional){
	        	searchLogPage.addHR();
	        	sb.append("<hr/>");	
	        }
			int fromRemoveForDB = sb.length();
	        
			//searchLogPage.
	        sb.append("<span class='serverName'>");
	        String serverName = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID).getName();
			sb.append(serverName);
	        sb.append("</span> ");
	
	       	sb.append(automatic? "automatic":"manual");
	       	Object info = module.getExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION);
	       	if(StringUtils.isNotEmpty(module.getLabel())) {
		        
		        if(info!=null){
		        	sb.append(" - " + info + "<br>");
		        }
		        sb.append(" <span class='searchName'>");
		        sb.append(module.getLabel());
	       	} else {
	       		sb.append(" <span class='searchName'>");
		        if(info!=null){
		        	sb.append(" - " + info + "<br>");
		        }
	       	}
	        sb.append("</span> by ");
	        
	        int groupOrTypeCode = 0;
	        for(Entry<String,String> entry : moduleParams.entrySet() ){
	        	String key = entry.getKey();
	        	String value = entry.getValue();
	        	value = value.replaceAll("(, )+$","");
	        	if ("Group or Type".equals(key)) {
	        		if ("group".equals(value)) {
	        			groupOrTypeCode = 1;
	        		} else if ("type".equals(value)) {
	        			groupOrTypeCode = 2;
	        		}
	        		break;
	        	}
	        }
	        
	        boolean firstTime = true;
	        for(Entry<String,String> entry : moduleParams.entrySet() ){
	        	boolean doNotWrite = false;
	        	String key = entry.getKey();
	        	String value = entry.getValue();
	        	value = value.replaceAll("(, )+$","");
	        	if ("Book Type".equals(key)) {
	        		value = getBookTypeFromCode(search.getSearchID(), value);
	        	} else if ("Document Type".equals(key)) {
	        		if ("please select item".equalsIgnoreCase(value) || "-1".equalsIgnoreCase(value)) {
	        			doNotWrite = true;
	        		} else {
	        			if (groupOrTypeCode==2) {
	        				value = getDocumentTypeFromCode(value.toUpperCase());
	        			} else {
	        				doNotWrite = true;
	        			}
	        		}
	        	} else if ("Subdivision".equals(key)) {
	        		if ("please select item".equalsIgnoreCase(value) || "-1".equalsIgnoreCase(value)) {
	        			doNotWrite = true;
	        		} else {
	        			value = getNameFromSubdivisionCode(value.toUpperCase());
	        		}
	        	} else if ("Name Type".equals(key)) {
	        		doNotWrite = true;
	        	} else if ("Results Per Page".equals(key)) {
	        		doNotWrite = true;
	        	} else if ("Group or Type".equals(key)) {
	        		doNotWrite = true;
	        	} else if ("Document Group".equals(key)) {
	        		if (hasDocumentGroup(getStateCounty()) && groupOrTypeCode==1) {
	        			value = getDocumentGroupFromCode(value.toUpperCase(), getStateCounty());
	        		} else {
	        			doNotWrite = true;
	        		}
	        	} 
	        	if(!firstTime){
	        		if (!doNotWrite) {
	        			sb.append(", ");
	        		}
	        	} else {
	        		if (!doNotWrite) {
	        			firstTime = false;
	        		}
	        	}
	        	if (!doNotWrite) {
	        		sb.append(entry.getKey().replaceAll("&lt;br&gt;", "") + " = <b>" + value + "</b>");
	        	}
	        } 
	        int toRemoveForDB = sb.length();
	        //log time when manual is starting        
	        if (!automatic || imageSearch){
	        	sb.append(" ");
	        	sb.append(SearchLogger.getTimeStamp(searchId));
	        }
	        sb.append(":<br/>");
	        
	        // log the message
	        SearchLogger.info(sb.toString(),searchId);   
	        ModuleShortDescription moduleShortDescription = new ModuleShortDescription();
	        moduleShortDescription.setDescription(sb.substring(fromRemoveForDB, toRemoveForDB));
	        moduleShortDescription.setSearchModuleId(module.getModuleIdx());
	        search.setAdditionalInfo(TSServerInfoConstants.TS_SERVER_INFO_MODULE_DESCRIPTION, moduleShortDescription);
	        String user=InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getAttribute(1).toString();
	        SearchLogger.info(StringUtils.createCollapsibleHeader(),searchId);
	        searchLogPage.addModuleSearchParameters(serverName,additional, info, moduleParams,module.getLabel(), automatic, imageSearch,user);
    	}  
    }
	
	public static boolean hasDocumentGroup(String stateCounty) {
		
		SelectLists sl = getCachedSelectLists().get(stateCounty);
		if (sl!=null) {
			return sl.hasDocumentGroup;
		}
		
		return false;
	}
	
	@Override
	public String getIncludedScript() {
		StringBuilder sb1 = new StringBuilder();
		SelectLists sl = getCachedSelectLists().get(getStateCounty());
		if (sl!=null) {
			LinkedHashMap<String,LinkedHashMap<String,ArrayList<String>>> typeBookPage = sl.getTypeBookPageMap();
			if (typeBookPage!=null) {
				sb1.append("<SCRIPT language=\"JavaScript\">\n");
				sb1.append("\n");
				sb1.append("function updateBookNumber(comboBox) {\n");
				sb1.append("\n");
				sb1.append("var myindex = comboBox.selectedIndex;\n");
				sb1.append("var selectedValue = comboBox.options[myindex].value;\n");
				sb1.append("\n");
				
				Iterator<Entry<String, LinkedHashMap<String, ArrayList<String>>>> it1 = typeBookPage.entrySet().iterator();
				if (it1.hasNext()) {
					StringBuilder sb2 = new StringBuilder();
					sb1.append("if (");
					int i=0;
					while (it1.hasNext()) {
						Entry<String, LinkedHashMap<String, ArrayList<String>>> entry1 = it1.next();
						sb1.append("selectedValue == \"").append(entry1.getKey()).append("\"");
						if (it1.hasNext()) {
							sb1.append(" || ");
						} else {
							sb1.append("){\n");
						}
						if (i==0) {
							sb2.append("var ");
						} else {
							if (i>1){
								sb2.append("else ");
							}
							sb2.append("if (selectedValue == \"").append(entry1.getKey()).append("\") {\n");
						}
						LinkedHashMap<String, ArrayList<String>> bookNumberList = entry1.getValue();
						Iterator<Entry<String, ArrayList<String>>> it2 = bookNumberList.entrySet().iterator();
						if (it2.hasNext()) {
							sb2.append("items = {");
							while (it2.hasNext()) {
								Entry<String, ArrayList<String>> entry2 = it2.next();
								sb2.append("\"").append(entry2.getKey()).append("\":\"").append(entry2.getKey()).append("\"");
								if (it2.hasNext()) {
									sb2.append(", ");
								}
							}
							sb2.append("};\n");
						} else {
							sb2.append("items = {\"\":\"\"};\n");
						}
						if (i>0) {
							sb2.append("}\n");
						}
						i++;
					}
					sb1.append(sb2);
					sb1.append("\n");
					sb1.append("var select = document.createElement(\"select\");\n");
					sb1.append("select.setAttribute(\"size\", \"1\");\n");
					sb1.append("select.setAttribute(\"id\", \"").append(ro.cst.tsearch.connection.http2.GenericCountyRecorderRO.BOOK_NUMBER_PARAM).append("\");\n");
					sb1.append("select.setAttribute(\"name\", \"").append(ro.cst.tsearch.connection.http2.GenericCountyRecorderRO.BOOK_NUMBER_PARAM).append("\");\n");
					sb1.append("\n");
					sb1.append("for (var key in items) {\n");
					sb1.append("var op = document.createElement(\"option\");\n");
					sb1.append("op.value = key;\n");
					sb1.append("op.appendChild(document.createTextNode(items[key]));\n");
					sb1.append("select.appendChild(op);\n");
					sb1.append("}\n");
					sb1.append("\n");
					sb1.append("var bookNumberObj = document.getElementById(\"")
						.append(ro.cst.tsearch.connection.http2.GenericCountyRecorderRO.BOOK_NUMBER_PARAM).append("\");\n");
					sb1.append("var parent = bookNumberObj.parentNode;\n");
					sb1.append("parent.appendChild(select);\n");
					sb1.append("parent.removeChild(bookNumberObj);\n");
					sb1.append("}\n");
				}
				sb1.append("\n");
				sb1.append("}\n");
			}
		}
		sb1.append("\n");
		sb1.append("</SCRIPT>");
		return sb1.toString();
	}
	
}
