package ro.cst.tsearch.servers.types;

import static ro.cst.tsearch.datatrace.Utils.setupSelectBox;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.html.parser.HtmlHelper;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.StateCountyManager;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadySavedDocumentsFilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotInterval;
import ro.cst.tsearch.search.filter.matchers.algorithm.LotMatchAlgorithm;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericMultipleLegalFilter;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.SubdivisionFilter;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.iterator.data.LegalStruct;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIteratorI;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.OcrOrBootStraperIterator;
import ro.cst.tsearch.search.name.NameUtils;
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
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.ModuleWrapperManager;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.templates.MultilineElementsMap;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
 * 
 * @author Oprina George
 * 
 *         Oct 26, 2012
 */


public class TNGenericTitleSearcherSRC extends TSServerROLike {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	
	protected String instr_type_select = "";
	protected String party_type_select = "";
	protected String search_type_select = "";
	protected String index_type_select = "";
	protected String mortgage_op_select = "";
	protected String transfer_op_select = "";
	
	protected String[] DOCTYPES_NOT_ALLOWED_FOR_SAVE_WITH_CROSSREFS = {"PLAT"};
	
	protected static final String PARTY_TYPE_SELECT = 
			"<select NAME=\"nameType\" size=\"3\">" + 
				"<option value=\"0\">Grantor - Seller</option>" +                            
				"<option value=\"1\">Grantee - Buyer</option>" +                            
				"<option value=\"2\" SELECTED>Both</option>" +                            
			"</select>";
	
	protected static final String SEARCH_TYPE_SELECT = 
			"<select NAME=\"searchType\" size=\"2\">" + 
				"<option value=\"PA\" SELECTED>Pure Alpha Search</option>" +                            
				"<option value=\"SS\">Standard Search</option>" +                                                       
			"</select>";
	
	protected static final String INDEX_TYPE_SELECT = 
			"<select NAME=\"indexType\" size=\"3\">" + 
				"<option value=\"LR\">Land Records</option>" +                            
				"<option value=\"FS\">Financing Statements</option>" +                            
				"<option value=\"BOTH\" SELECTED>Both</option>" +                            
			"</select>";
	
	protected static final String MORTGAGE_OPERATOR_SELECT = 
			"<select NAME=\"mortOp\" size=\"5\">" + 
				"<option value=\">=\" SELECTED>Greater than or equal to</option>" +                            
				"<option value=\">\">Greater than</option>" +                            
				"<option value=\"<=\">Less than or equal to</option>" +        
				"<option value=\"<\">Less than</option>" +                            
				"<option value=\"=\">Equal to</option>" +         
			"</select>";
	
	protected static final String TRANSFER_OPERATOR_SELECT = 
			"<select NAME=\"transOp\" size=\"5\">" + 
				"<option value=\">=\" SELECTED>Greater than or equal to</option>" +                            
				"<option value=\">\">Greater than</option>" +                            
				"<option value=\"<=\">Less than or equal to</option>" +        
				"<option value=\"<\">Less than</option>" +                            
				"<option value=\"=\">Equal to</option>" +         
			"</select>";

	public TNGenericTitleSearcherSRC(long searchId) {
		super(searchId);
		initFields();
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public TNGenericTitleSearcherSRC(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
		initFields();
		resultType = MULTIPLE_RESULT_TYPE;
	}

	@Override
	public TSServerInfo getDefaultServerInfo(){
		
		initFields();
		
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		ModuleWrapperManager moduleWrapperManager = ModuleWrapperManager.getInstance();
		DataSite dataSite = HashCountyToIndex.getDateSiteForMIServerID(getCommunityId(), miServerID);
		String siteName = StateCountyManager.getInstance().getSTCounty(dataSite.getCountyId()) + dataSite.getSiteType();
		
		TSServerInfoModule tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.NAME_MODULE_IDX);
		if (tsServerInfoModule != null) {
			setupSelectBox(tsServerInfoModule.getFunction(4), instr_type_select);
			setupSelectBox(tsServerInfoModule.getFunction(5), party_type_select);
	        setupSelectBox(tsServerInfoModule.getFunction(6), search_type_select);
	        setupSelectBox(tsServerInfoModule.getFunction(7), index_type_select);
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
		if (tsServerInfoModule != null) {

			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.BOOK_AND_PAGE_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX);
		if (tsServerInfoModule != null) {

			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.SUBDIVISION_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
				
				if ("District".equalsIgnoreCase(functionName)){
					if (dataSite.getCountyId() == CountyConstants.TN_Shelby){
						htmlControl.setHiddenParam(true);
					} else{
						htmlControl.setHiddenParam(false);
					}
				} else if ("Subdivision Name".equalsIgnoreCase(functionName)){
					if (dataSite.getCountyId() == CountyConstants.TN_Johnson){
						htmlControl.setHiddenParam(true);
					} else{
						htmlControl.setHiddenParam(false);
					}
				} else if ("Acres".equalsIgnoreCase(functionName)){
					if (dataSite.getCountyId() == CountyConstants.TN_Shelby){
						htmlControl.setHiddenParam(true);
					} else{
						htmlControl.setHiddenParam(false);
					}
				} else if ("Block".equalsIgnoreCase(functionName)){
					switch (dataSite.getCountyId()) {
						case CountyConstants.TN_Fentress:
						case CountyConstants.TN_Polk:
						case CountyConstants.TN_Shelby:
						case CountyConstants.TN_Weakley:
						
						htmlControl.setHiddenParam(true);
						break;
					default:
						htmlControl.setHiddenParam(false);
						break;
					}
				} else if ("Section".equalsIgnoreCase(functionName)){
					switch (dataSite.getCountyId()) {
						case CountyConstants.TN_Decatur:
						case CountyConstants.TN_Fentress:
						case CountyConstants.TN_Polk:
						case CountyConstants.TN_Sequatchie:
						case CountyConstants.TN_Shelby:
						case CountyConstants.TN_Weakley:
							
						htmlControl.setHiddenParam(true);
						break;
					default:
						htmlControl.setHiddenParam(false);
						break;
					}
				} else if ("Phase".equalsIgnoreCase(functionName)){
					switch (dataSite.getCountyId()) {
						case CountyConstants.TN_Anderson:
						case CountyConstants.TN_Bedford:
						case CountyConstants.TN_Bradley:
						case CountyConstants.TN_Cocke:
						case CountyConstants.TN_Coffee:
						case CountyConstants.TN_Cumberland:
						case CountyConstants.TN_Giles:
						case CountyConstants.TN_Hickman:
						case CountyConstants.TN_Humphreys:
						case CountyConstants.TN_Jackson:
						case CountyConstants.TN_Jefferson:
						case CountyConstants.TN_Johnson:
						case CountyConstants.TN_Lawrence:
						case CountyConstants.TN_Macon:
						case CountyConstants.TN_Madison:
						case CountyConstants.TN_Marion:
						case CountyConstants.TN_Maury:
						case CountyConstants.TN_Moore:
						case CountyConstants.TN_Washington:
						case CountyConstants.TN_Polk:
						case CountyConstants.TN_Sequatchie:
						case CountyConstants.TN_Shelby:
						case CountyConstants.TN_Weakley:
						case CountyConstants.TN_White:
						case CountyConstants.TN_Williamson:
						case CountyConstants.TN_Wilson:
							
						htmlControl.setHiddenParam(true);
						break;
					default:
						htmlControl.setHiddenParam(false);
						break;
					}
				} else if ("Map".equalsIgnoreCase(functionName) || "Parcel".equalsIgnoreCase(functionName)){
					switch (dataSite.getCountyId()) {
						case CountyConstants.TN_Campbell:
						case CountyConstants.TN_Claiborne:
						case CountyConstants.TN_Hawkins:
						case CountyConstants.TN_Johnson:
						case CountyConstants.TN_Loudon:
						case CountyConstants.TN_Maury:
						case CountyConstants.TN_Rhea:
						case CountyConstants.TN_Roane:
						case CountyConstants.TN_Shelby:
						case CountyConstants.TN_Sullivan:
						case CountyConstants.TN_Williamson:
							
						htmlControl.setHiddenParam(true);
						break;
					default:
						htmlControl.setHiddenParam(false);
						break;
					}
				} else if ("Group".equalsIgnoreCase(functionName)){
					switch (dataSite.getCountyId()) {
						case CountyConstants.TN_Bledsoe:
						case CountyConstants.TN_Campbell:
						case CountyConstants.TN_Claiborne:
						case CountyConstants.TN_Clay:
						case CountyConstants.TN_Grainger:
						case CountyConstants.TN_Hawkins:
						case CountyConstants.TN_Humphreys:
						case CountyConstants.TN_Johnson:
						case CountyConstants.TN_Loudon:
						case CountyConstants.TN_Maury:
						case CountyConstants.TN_Moore:
						case CountyConstants.TN_Perry:
						case CountyConstants.TN_Pickett:
						case CountyConstants.TN_Rhea:
						case CountyConstants.TN_Roane:
						case CountyConstants.TN_Shelby:
						case CountyConstants.TN_Sullivan:
						case CountyConstants.TN_Van_Buren:
						case CountyConstants.TN_White:
						case CountyConstants.TN_Williamson:
							
						htmlControl.setHiddenParam(true);
						break;
					default:
						htmlControl.setHiddenParam(false);
						break;
					}
				} else if ("ParcelID".equalsIgnoreCase(functionName)){
					if (dataSite.getCountyId() == CountyConstants.TN_Shelby){
						htmlControl.setHiddenParam(false);
					} else{
						htmlControl.setHiddenParam(true);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
		if (tsServerInfoModule != null) {

			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.INSTR_NO_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.SUBDIVISION_PLAT_MODULE_IDX);
		if (tsServerInfoModule != null) {

			HashMap<String, Integer> nameToIndex = new HashMap<String, Integer>();
			for (int i = 0; i < tsServerInfoModule.getFunctionCount(); i++) {
				nameToIndex.put(tsServerInfoModule.getFunction(i).getName(), i);
			}
			PageZone pageZone = (PageZone) tsServerInfoModule.getModuleParentSiteLayout();
			for (HTMLControl htmlControl : pageZone.getHtmlControls()) {
				String functionName = htmlControl.getCurrentTSSiFunc().getName();
				if (StringUtils.isNotEmpty(functionName)) {
					String comment = moduleWrapperManager.getCommentForSiteAndFunction(siteName, TSServerInfo.SUBDIVISION_PLAT_MODULE_IDX, nameToIndex.get(functionName));
					if (comment != null) {
						htmlControl.setFieldNote(comment);
					}
				}
			}
		}
		
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.CONDOMIN_MODULE_IDX);
		if (tsServerInfoModule != null) {
	         setupSelectBox(tsServerInfoModule.getFunction(0), instr_type_select.replaceAll("\\bitype\\b", "instType1"));
	         setupSelectBox(tsServerInfoModule.getFunction(1), instr_type_select.replaceAll("\\bitype\\b", "instType2"));
	         setupSelectBox(tsServerInfoModule.getFunction(2), instr_type_select.replaceAll("\\bitype\\b", "instType3"));
	         setupSelectBox(tsServerInfoModule.getFunction(5), mortgage_op_select);
	         setupSelectBox(tsServerInfoModule.getFunction(7), transfer_op_select);
		}
		
//		View Daily Notebook 
//		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.ARCHIVE_DOCS_MODULE_IDX);
//		if (tsServerInfoModule != null){
//			tsServerInfoModule.setVisible(false);
//		}
		
		//View Backscanned Plats 
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.MODULE_IDX38);
		if (tsServerInfoModule != null){
			switch (dataSite.getCountyId()) {
				case CountyConstants.TN_Bedford:
				case CountyConstants.TN_Bledsoe:
				case CountyConstants.TN_Bradley:
				case CountyConstants.TN_Coffee:
				case CountyConstants.TN_Decatur:
				case CountyConstants.TN_Hamblen:
				case CountyConstants.TN_Hawkins:
				case CountyConstants.TN_Hickman:
				case CountyConstants.TN_Lincoln:
				case CountyConstants.TN_Loudon:
				case CountyConstants.TN_Macon:
				case CountyConstants.TN_Madison:
				case CountyConstants.TN_Monroe:
				case CountyConstants.TN_Moore:
				case CountyConstants.TN_Sequatchie:
				case CountyConstants.TN_Smith:
				case CountyConstants.TN_Unicoi:
				case CountyConstants.TN_Union:
				case CountyConstants.TN_Weakley:
				case CountyConstants.TN_White:
				case CountyConstants.TN_Wilson:
					tsServerInfoModule.setVisible(true);
					break;
	
				default:
					tsServerInfoModule.setVisible(false);
					break;
			}
		}
		
		//View Backscanned Deeds 
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.MODULE_IDX39);
		if (tsServerInfoModule != null){
			switch (dataSite.getCountyId()) {
				case CountyConstants.TN_Bledsoe:
				case CountyConstants.TN_Bradley:
				case CountyConstants.TN_Campbell:
				case CountyConstants.TN_Carter:
				case CountyConstants.TN_Coffee:
				case CountyConstants.TN_Decatur:
				case CountyConstants.TN_Giles:
				case CountyConstants.TN_Greene:
				case CountyConstants.TN_Hamblen:
				case CountyConstants.TN_Hickman:
				case CountyConstants.TN_Humphreys:
				case CountyConstants.TN_Jackson:
				case CountyConstants.TN_Johnson:
				case CountyConstants.TN_Lawrence:
				case CountyConstants.TN_Lincoln:
				case CountyConstants.TN_Loudon:
				case CountyConstants.TN_Macon:
				case CountyConstants.TN_Madison:
				case CountyConstants.TN_Marion:
				case CountyConstants.TN_Maury:
				case CountyConstants.TN_Monroe:
				case CountyConstants.TN_Moore:
				case CountyConstants.TN_Perry:
				case CountyConstants.TN_Polk:
				case CountyConstants.TN_Rhea:
				case CountyConstants.TN_Roane:
				case CountyConstants.TN_Sequatchie:
				case CountyConstants.TN_Sevier:
				case CountyConstants.TN_Smith:
				case CountyConstants.TN_Sullivan:
				case CountyConstants.TN_Unicoi:
				case CountyConstants.TN_Union:
				case CountyConstants.TN_Washington:
				case CountyConstants.TN_Weakley:
				case CountyConstants.TN_White:
				case CountyConstants.TN_Williamson:
				case CountyConstants.TN_Wilson:
					tsServerInfoModule.setVisible(true);
					break;
	
				default:
					tsServerInfoModule.setVisible(false);
					break;
			}
		}
		
		//View Indexed Book Images
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.MODULE_IDX40);
		if (tsServerInfoModule != null){
			switch (dataSite.getCountyId()) {
				case CountyConstants.TN_Decatur:
				case CountyConstants.TN_Jackson:
					tsServerInfoModule.setVisible(true);
					break;
	
				default:
					tsServerInfoModule.setVisible(false);
					break;
			}
		}
		
//		/View Old-Indexed Books
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.MODULE_IDX41);
		if (tsServerInfoModule != null){
			switch (dataSite.getCountyId()) {
				case CountyConstants.TN_Bledsoe:
				case CountyConstants.TN_Bradley:
				case CountyConstants.TN_Campbell:
				case CountyConstants.TN_Carter:
				case CountyConstants.TN_Coffee:
				case CountyConstants.TN_Cumberland:
				case CountyConstants.TN_Grainger:
				case CountyConstants.TN_Greene:
				case CountyConstants.TN_Hamblen:
				case CountyConstants.TN_Humphreys:
				case CountyConstants.TN_Johnson:
				case CountyConstants.TN_Lawrence:
				case CountyConstants.TN_Lincoln:
				case CountyConstants.TN_Loudon:
				case CountyConstants.TN_Macon:
				case CountyConstants.TN_Madison:
				case CountyConstants.TN_Marion:
				case CountyConstants.TN_Monroe:
				case CountyConstants.TN_Moore:
				case CountyConstants.TN_Rhea:
				case CountyConstants.TN_Roane:
				case CountyConstants.TN_Sequatchie:
				case CountyConstants.TN_Unicoi:
				case CountyConstants.TN_Union:
				case CountyConstants.TN_Weakley:
				case CountyConstants.TN_White:
				case CountyConstants.TN_Williamson:
				case CountyConstants.TN_Wilson:
					tsServerInfoModule.setVisible(true);
					break;
	
				default:
					tsServerInfoModule.setVisible(false);
					break;
			}
		}
		
		//View Non-Indexed Images 
		tsServerInfoModule = msiServerInfoDefault.getModule(TSServerInfo.MODULE_IDX42);
		if (tsServerInfoModule != null){
			switch (dataSite.getCountyId()) {
				case CountyConstants.TN_Williamson:
					tsServerInfoModule.setVisible(true);
					break;
			
				default:
					tsServerInfoModule.setVisible(false);
					break;
			}
		}
		
		return msiServerInfoDefault;
		
	}
	
	@Override
	public Object getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		if(restoreDocumentDataI == null) {
			return null;
		}
		
		List<TSServerInfoModule> list = new ArrayList<TSServerInfoModule>();
		
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		TSServerInfoModule module = null;
		
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(book) && ro.cst.tsearch.utils.StringUtils.isNotEmpty(page)) {
			module = getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX);
			module.forceValue(0, book);
			module.forceValue(1, page);
			module.forceValue(3, "1");
			list.add(module);
		}
		
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(restoreDocumentDataI.getInstrumentNumber())) {
			
			module = getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX);
			module.forceValue(0, restoreDocumentDataI.getInstrumentNumber());
			list.add(module);
		}
			
		return list;
	}

	@Override
	 protected NameI getNameFromModule(TSServerInfoModule module){
		 NameI name = new Name();
		 if (module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX && module.getFunctionCount() > 0){
			 String allName = module.getFunction(0).getParamValue();
			 			 
			 if (StringUtils.isEmpty(allName)){
				 return null;
			 }

			 String[] names = null;
			 if (NameUtils.isCompany(allName)) {
				 names = new String[]{"", "", allName, "", "", ""};
			 } else {
				 names = StringFormats.parseNameNashville(allName, true);
			 }
			 
			 name.setLastName(names[2]);
			 name.setFirstName(names[0]);
			 name.setMiddleName(names[1]);

			 return name;
		 }
		 
		 return null;
	}
		
	 @Override
	 protected LegalI getLegalFromModule(TSServerInfoModule module){
		 LegalI legal = null;
		 SubdivisionI subdivision = null;
			
		 if (module.getModuleIdx() == TSServerInfo.SUBDIVISION_MODULE_IDX && module.getFunctionCount() > 4){
			 subdivision = new Subdivision();
				
			 String subdivisionName = module.getFunction(1).getParamValue().trim();
			 subdivision.setName(subdivisionName);
			 subdivision.setLot(module.getFunction(2).getParamValue().trim());
			 subdivision.setBlock(module.getFunction(4).getParamValue().trim());
		 }
		 if (subdivision != null){
			 legal = new Legal();
			 legal.setSubdivision(subdivision);
		 }
		 
		 return legal;
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
					html = ((ro.cst.tsearch.connection.http2.TNGenericTitleSearcherSRC) site).getCertDate();
				} catch (RuntimeException e) {
					e.printStackTrace();
				} finally {
					HttpManager.releaseSite(site);
				}
		
				if (StringUtils.isNotEmpty(html)) {
					try {
						NodeList nodes = new HtmlParser3(html).getNodeList();
		
						String date = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(nodes, "last update:"), "", true)
								.replaceAll("<[^>]*>", "").replaceAll("last update:", "").replaceAll("\\s+", " ").split("\\d{2}:\\d{2}:\\d{2}")[0].trim();
						
						date = DateFormatUtils.format(Util.dateParser3(date), "MM/dd/yyyy");
						
						CertificationDateManager.cacheCertificationDate(dataSite, date);
						getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
					} catch (Exception e) {
						CertificationDateManager.getLogger().error("Cannot parse certification date on " + getDataSite().getName() + " because pattern not found");
					}
				} else {
					CertificationDateManager.getLogger().error("Cannot parse certification date on " + getDataSite().getName() + " because html response is empty");
				}
			}
		} catch (Exception e) {
			CertificationDateManager.getLogger().error("Error setting certification date on " + getDataSite().getName(), e);
		}
	}
	
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		String rsResponse = Response.getResult();
		ParsedResponse parsedResponse = Response.getParsedResponse();

		mSearch.setAdditionalInfo("viParseID", viParseID);

		if (rsResponse.contains("No records selected")) {
			Response.getParsedResponse().setError("No Results Found!");
			Response.getParsedResponse().setResponse("");
			return;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_BOOK_AND_PAGE:
		case ID_SEARCH_BY_SUBDIVISION_NAME:
		case ID_SEARCH_BY_INSTRUMENT_NO:
		case ID_SEARCH_BY_SUBDIVISION_PLAT:
		case ID_SEARCH_BY_CONDO_NAME:
		case ID_INTERMEDIARY:

			StringBuilder outputTable = new StringBuilder();
			Collection<ParsedResponse> smartParsedResponses = smartParseIntermediary(Response, rsResponse, outputTable);

			if (smartParsedResponses.size() == 0) {
				return;
			}

			parsedResponse.setResultRows(new Vector<ParsedResponse>(smartParsedResponses));
			parsedResponse.setOnlyResponse(outputTable.toString());
			parsedResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, outputTable.toString());

			break;

		case ID_DETAILS:
		case ID_SAVE_TO_TSD:

			StringBuilder accountId = new StringBuilder();
			HashMap<String, String> data = new HashMap<String, String>();
			String details = getDetails(Response, rsResponse, accountId, data);
			String accountName = accountId.toString();

			if (viParseID != ID_SAVE_TO_TSD) {
				String originalLink = sAction.replace("?", "&") + "&" + Response.getQuerry().replaceFirst("(?is)(\\.php)\\?restore.*", "$1");
				String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idPOST) + originalLink;

				if (isInstrumentSaved("", null, data)) {
					details += CreateFileAlreadyInTSD();
				} else {
					mSearch.addInMemoryDoc(sSave2TSDLink, details);
					details = addSaveToTsdButton(details, sSave2TSDLink, ID_DETAILS);
				}
				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				Response.getParsedResponse().setResponse(details);

			} else {
				RegisterDocumentI doc = (RegisterDocumentI) smartParseDetails(Response, details);

				msSaveToTSDFileName = accountName + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				Response.getParsedResponse().setResponse(details.replaceAll("(?ism)</?a[^>]*>", ""));

				msSaveToTSDResponce = details + CreateFileAlreadyInTSD();
				
				if (doc.isNotOneOf(DocumentTypes.PLAT)){
					Pattern crossRefLinkPattern = Pattern.compile("(?is)HREF=\\\"([^\\\"]+)\\\"[^>]*>\\s*Details");
	                Matcher crossRefLinkMatcher = crossRefLinkPattern.matcher(details);
	                while (crossRefLinkMatcher.find()) {
						String link = crossRefLinkMatcher.group(1) + "&isSubResult=true";
						LinkInPage pl = new LinkInPage(link, link,TSServer.REQUEST_SAVE_TO_TSD);
						ParsedResponse prChild = new ParsedResponse();
						prChild.setPageLink(pl);
						Response.getParsedResponse().addOneResultRowOnly(prChild);
	                }
				}
			}

			break;
		case ID_GET_LINK:
			if (rsResponse.contains("Detail Information for Instrument")){
				ParseResponse(sAction, Response, ID_DETAILS);
			} else if (sAction.contains("/plathold.php") || rsResponse.contains("platholdViewer.php")){
				ParseResponse(sAction, Response, ID_BROWSE_BACKSCANNED_PLATS);
			} else if (sAction.contains("/deedhold.php") || rsResponse.contains("deedholdViewer.php")){
				ParseResponse(sAction, Response, ID_BROWSE_BACKSCANNED_DEEDS);
			} else if (sAction.contains("/indexedbooks.php")){
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE40);
			} else if (sAction.contains("/oldIndexed.php")){
				ParseResponse(sAction, Response, ID_SEARCH_BY_MODULE41);
			} else {
				ParseResponse(sAction, Response, ID_INTERMEDIARY);
			}
			break;
			
		case ID_BROWSE_BACKSCANNED_PLATS:
			
			int iStart = rsResponse.indexOf("Backscanned Plat Viewer");
			if (iStart < 0)
				return;
	            
			//rsResponce = rsResponce.replaceFirst("(?is)\\b(name=\\\"type\\\")", "$1 disabled");
			String bookNo = "";
			rsResponse = "<center><h2>" + rsResponse.substring(iStart, rsResponse.indexOf("<center><input"));
			rsResponse = rsResponse.replaceAll("<script language=[^<]+</script>", ""); 
			rsResponse = rsResponse.replaceAll("<form action=\"plathold.php\" method=\"GET\">", "");
			rsResponse = rsResponse.replaceAll("(?is)<\\s*form\\s+action\\s*=\\s*\\\"\\\"\\s+method\\s*=\\s*\\\"GET\\\"\\s*>", "");
			String sTmp = CreatePartialLink(TSConnectionURL.idGET);
			int iEnd = rsResponse.indexOf("SELECTED");
			if (iEnd != -1) {
				bookNo = rsResponse.substring(iEnd - 12, iEnd + 1);
				bookNo = ro.cst.tsearch.utils.StringUtils.getTextBetweenDelimiters("=\"", "\" S", bookNo);
			}
	
			iStart = rsResponse.indexOf("<select onChange=\"this.form.submit()\" name=\"book\">");
			iEnd = rsResponse.indexOf("</select>" , iStart);
			if (iStart != -1 && iEnd != -1){
				String bookSelect = rsResponse.substring(iStart, iEnd);
				Matcher optionMatcher = Pattern.compile("(?is)<option value=\"([^\"]*)\"[^>]*>([^<]*)</option>").matcher(bookSelect);
				while (optionMatcher.find()){
					if( "-1".equals(optionMatcher.group(1))){
						bookSelect = bookSelect.replace(optionMatcher.group(0) , "");            		
					} else{
						bookSelect = bookSelect.replace(optionMatcher.group(0), 
								"<A HREF=\"" + sTmp + "/plathold.php?book=" + optionMatcher.group(1) + "&page=-1\">" + optionMatcher.group(2) + "</A>&nbsp;&nbsp;");
					}
				}
		            
				rsResponse = rsResponse.replaceFirst("(?is)<select onChange=\\\"this\\.form\\.submit\\(\\)\\\" name=\\\"book\\\">.*?</select>", bookSelect);
			}
			rsResponse = rsResponse.replaceAll("<select[^>]*>" , "");
			rsResponse = rsResponse.replaceAll("</select>" , "");            
	            
			rsResponse = rsResponse.replaceAll("<select\\s*name=\"page\" onChange=.*>\\s*<option[^<]+</option>", "");
			rsResponse = rsResponse.replaceAll("(?is)<\\s*[/]?form[^>]*>", "");
			
			rsResponse = rsResponse.replaceAll("(?is)<option value=\"[^\"]*\">Please Select</option>", "");
	
			rsResponse = rsResponse.replaceAll("(?is)<option value=\\\"(/?[^\\\"]*)\\\"\\s*>[^<]*</option>", 
					"&nbsp;<input type=\"checkbox\" name=\"" + bookNo + "-$1" + "\" " 
									+ "value=\"" + CreatePartialLink(TSConnectionURL.idGET) + "/platholdImage.php?imgtype=&book=" + bookNo + "&page=$1.pdf\">"
					+ "<A target=\"NEW\" HREF=\"" + CreatePartialLink(TSConnectionURL.idGET) + "/platholdImage.php?imgtype=&book=" + bookNo + "&page=$1.pdf\">" + bookNo + "-$1</A><BR>");
	        
			if (StringUtils.isNotEmpty(bookNo)){
				rsResponse = "<form name=\"savepdf\" action=\"/title-search/MultiDocSave\" method=\"POST\">" + rsResponse.substring(0, rsResponse.indexOf("</table>") + 8)
					+ "<input name=\"Button\" type=\"button\" class=\"button\" value=\"" + SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL + "\" onClick=\"window.document.forms[0].submit();\">" 
					+ "<input type=\"hidden\" name=\"searchId\" value=\"" + searchId +"\"/>"
					+ "<input type=\"hidden\" name=\"serverId\" value=\"65\"/>"
					+ "<input type=\"hidden\" name=\"searchType\" value=\"PLAT\"/>"
					+ "<input type=\"hidden\" name=\"searchTypeModule\" value=\"IM\"/>"
					+ "</form>";
			}
			
			rsResponse += "</body></html>";
	            
            parser.Parse(Response.getParsedResponse(), rsResponse, ro.cst.tsearch.servers.response.Parser.NO_PARSE);
			
			break;
			
		case ID_BROWSE_BACKSCANNED_DEEDS:
			
			iStart = rsResponse.indexOf("View Backscanned Deeds");
			if (iStart < 0)
				return;
	        
			rsResponse = rsResponse.replaceAll("(?is)(<center>)\\s*(<table)", "$1$2");
			bookNo = "";
			rsResponse = "<center><h2>" + rsResponse.substring(iStart, rsResponse.indexOf("<center><table"));
			rsResponse = rsResponse.replaceAll("<script language=[^<]+</script>", ""); 
			rsResponse = rsResponse.replaceAll("<form action=\"deedhold.php\" method=\"GET\">", "");
			rsResponse = rsResponse.replaceAll("(?is)<\\s*form\\s+action\\s*=\\s*\\\"\\\"\\s+method\\s*=\\s*\\\"GET\\\"\\s*>", "");
			sTmp = CreatePartialLink(TSConnectionURL.idGET);
			
			String bookCode = "";
			iEnd = rsResponse.indexOf("selected");
			if (iEnd != -1) {
				bookCode = rsResponse.substring(iEnd - 12, iEnd + 1);
				bookCode = ro.cst.tsearch.utils.StringUtils.getTextBetweenDelimiters("=\"", "\" s", bookCode);
			}
			
			iStart = rsResponse.indexOf("<select name=\"dir\">");
			iEnd = rsResponse.indexOf("</select>" , iStart);
			if (iStart != -1 && iEnd != -1){
				String bookCodeSelect = rsResponse.substring(iStart, iEnd);
				Matcher optionBookCodeMatcher = Pattern.compile("(?is)<option\\s+label=\\\"([^\\\"]*)\\\"\\s+value=\\\"([^\\\"]*)\\\"[^>]*>([^<]*)</option>").matcher(bookCodeSelect);
				while (optionBookCodeMatcher.find()){
						bookCodeSelect = bookCodeSelect.replace(optionBookCodeMatcher.group(0), 
														"<A HREF=\"" + sTmp + "/deedhold.php?dir=" + optionBookCodeMatcher.group(2) + "&max=&min=\">" 
															+ optionBookCodeMatcher.group(1) + "</A>&nbsp;&nbsp;");
				}
				
				rsResponse = rsResponse.replaceFirst("(?is)<select name=\\\"dir\\\">.*?</select>", bookCodeSelect);
			}
            			
			iEnd = rsResponse.indexOf("SELECTED");
			if (iEnd != -1) {
				bookNo = rsResponse.substring(iEnd - 12, iEnd + 1);
				bookNo = ro.cst.tsearch.utils.StringUtils.getTextBetweenDelimiters("=\"", "\" S", bookNo);
			}
	
			iStart = rsResponse.indexOf("<select name=\"book\" onchange=\"this.form.submit();\">");
			iEnd = rsResponse.indexOf("</select>" , iStart);
			if (iStart != -1 && iEnd != -1){
				String bookSelect = rsResponse.substring(iStart, iEnd);
				Matcher optionMatcher = Pattern.compile("(?is)<option value=\"([^\"]*)\"[^>]*>([^<]*)</option>").matcher(bookSelect);
				while (optionMatcher.find()){
					if( "-1".equals(optionMatcher.group(1))){
						bookSelect = bookSelect.replace(optionMatcher.group(0) , "");            		
					} else{
						bookSelect = bookSelect.replace(optionMatcher.group(0), 
								"<A HREF=\"" + sTmp + "/deedhold.php?max=&min=&dir=" + bookCode + "&book=" + optionMatcher.group(1) + "\">" + optionMatcher.group(2) + "</A>&nbsp;&nbsp;");
					}
				}
				rsResponse = rsResponse.replaceFirst("(?is)<select name=\\\"book\\\" onchange=\\\"this\\.form\\.submit\\(\\);\\\">.*?</select>", bookSelect);
			}
			
			rsResponse = rsResponse.replaceAll("<select[^>]*>" , "");
			rsResponse = rsResponse.replaceAll("</select>" , "");  
			
			rsResponse = rsResponse.replaceAll("<select\\s*name=\"page\" onChange=.*>\\s*<option[^<]+</option>", "");
			rsResponse = rsResponse.replaceAll("(?is)<\\s*[/]?form[^>]*>", "");
	
			rsResponse = rsResponse.replaceAll("(?is)<option value=\\\"([^\\\"]*)\\\"\\s*>[^<]*</option>", 
					"&nbsp;<input type=\"checkbox\" name=\"" + bookNo + "-$1" + "\" " 
								+ "value=\"" + CreatePartialLink(TSConnectionURL.idGET) + "/deedholdImage.php?max=&min=&dir=" + bookCode + "&book=" + bookNo + "&page=$1.pdf\">"
					+ "<A target=\"NEW\" HREF=\"" + CreatePartialLink(TSConnectionURL.idGET) + "/deedholdImage.php?max=&min=&dir=" + bookCode + "&book=" + bookNo + "&page=$1.pdf\">" + bookNo + "-$1</A><BR>");

			if (StringUtils.isNotEmpty(bookNo)){
				rsResponse = "<form name=\"savepdf\" action=\"/title-search/MultiDocSave\" method=\"POST\">" + rsResponse.substring(0, rsResponse.indexOf("</table>") + 8)
					+ "<input name=\"Button\" type=\"button\" class=\"button\" value=\"" + SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL + "\" onClick=\"window.document.forms[0].submit();\">" 
					+ "<input type=\"hidden\" name=\"searchId\" value=\"" + searchId +"\"/>"
					+ "<input type=\"hidden\" name=\"serverId\" value=\"65\"/>"
					+ "<input type=\"hidden\" name=\"searchType\" value=\"DEED\"/>"
					+ "<input type=\"hidden\" name=\"searchTypeModule\" value=\"IM\"/>"
					+ "</form>";
			}
			rsResponse += "</body></html>";
	            
            parser.Parse(Response.getParsedResponse(), rsResponse, ro.cst.tsearch.servers.response.Parser.NO_PARSE);
			
			break;
			
		case ID_SEARCH_BY_MODULE40:
			
			iStart = rsResponse.indexOf("Index Book Image Browser");
			if (iStart < 0)
				return;
	        
			rsResponse = rsResponse.replaceAll("(?is)(<center>)\\s*(<table)", "$1$2");
			
			rsResponse = "<center><h2>" + rsResponse.substring(iStart, rsResponse.indexOf("</select>Image Index") + 20);
			rsResponse = rsResponse.replaceAll("<script language=[^<]+</script>", "");
			rsResponse = rsResponse.replaceAll("(?is)(</h2>)", "$1</center>");
			rsResponse = rsResponse.replaceAll("(?is)</center>\\s*</center>", "</center>");
			rsResponse = rsResponse.replaceAll("(?is)<form action=\"indexedbooks.php\" method=\"post\"[^>]*>", "");
			rsResponse = rsResponse.replaceAll("(?is)<\\s*form\\s+action\\s*=\\s*\\\"\\\"\\s+method\\s*=\\s*\\\"GET\\\"\\s*>", "");
			sTmp = CreatePartialLink(TSConnectionURL.idPOST);
			
			String bookIndex = "";
			iStart = rsResponse.indexOf("<select onChange=\"submitform('index')\" name=\"indexes\"");
			iEnd = rsResponse.indexOf("</select>" , iStart);
			if (iStart != -1 && iEnd != -1){
				String bookIndexSelect = rsResponse.substring(iStart, iEnd);
				
				Matcher optionSelectedBookIndexMatcher = Pattern.compile("(?is)<select onChange=\\\"submitform\\('index'\\)\\\" name=\\\"indexes\\\"[^>]*>\\s*(<option\\s+value=\\\"[^\\\"]+\\\"\\s*>([^<]+)</option>)").matcher(bookIndexSelect);
				if (optionSelectedBookIndexMatcher.find()){
					bookIndex = optionSelectedBookIndexMatcher.group(2);
					bookIndexSelect = bookIndexSelect.replaceFirst(optionSelectedBookIndexMatcher.group(1), "");
				}
				
				Matcher optionBookIndexMatcher = Pattern.compile("(?is)<option value=\\'([^\\']*)\\'[^>]*>([^<]*)</option>").matcher(bookIndexSelect);
				while (optionBookIndexMatcher.find()){
						bookIndexSelect = bookIndexSelect.replace(optionBookIndexMatcher.group(0), 
														"<A HREF=\"" + sTmp + "/indexedbooks.php?indexes=" + optionBookIndexMatcher.group(1) + "\">" 
															+ optionBookIndexMatcher.group(2) + "</A>&nbsp;&nbsp;");
				}
				
				rsResponse = rsResponse.replaceFirst("(?is)<select onChange=\\\"submitform\\('index'\\)\\\" name=\\\"indexes\\\"[^>]*>.*?</select>\\s*(Index)", "<b>$1:</b> " + bookIndexSelect);
			}		
			iStart = rsResponse.indexOf("<select onChange=\"submitform('book')\" name=\"books\"");
			iEnd = rsResponse.indexOf("</select>" , iStart);
			bookNo = "";
			if (iStart != -1 && iEnd != -1){
				String bookSelect = rsResponse.substring(iStart, iEnd);
				
				Matcher optionSelectedBookMatcher = Pattern.compile("(?is)<select onChange=\\\"submitform\\('book'\\)\\\" name=\\\"books\\\"[^>]*>\\s*(<option\\s+value=\\\"[^\\\"]+\\\"\\s*>([^<]+)</option>)").matcher(bookSelect);
				if (optionSelectedBookMatcher.find()){
					bookNo = optionSelectedBookMatcher.group(2);
					bookSelect = bookSelect.replaceFirst(optionSelectedBookMatcher.group(1), "");
				}
				
				Matcher optionMatcher = Pattern.compile("<option value=\\'([^\\']*)\\'[^>]*>([^<]*)</option>").matcher(bookSelect);
				while (optionMatcher.find()){
						bookSelect = bookSelect.replace(optionMatcher.group(0), 
												"<A HREF=\"" + sTmp + "/indexedbooks.php?indexes=" + bookIndex + "&books=" + optionMatcher.group(1) + "\">" 
														+ optionMatcher.group(2) + "</A>&nbsp;&nbsp;");
				}
				rsResponse = rsResponse.replaceFirst("(?is)<select onChange=\\\"submitform\\('book'\\)\\\" name=\\\"books\\\"[^>]*>.*?</select>\\s*(Book)", "<br><b>$1:</b> " + bookSelect);
			}
			
			rsResponse = rsResponse.replaceAll("(?is)<select name=\\\"images\\\" onChange=.*>\\s*<option[^>]+></option>(.*?</select>)\\s*(Image Index)", "<br><b>$2:</b>$1");
			rsResponse = rsResponse.replaceAll("(?is)<\\s*[/]?form[^>]*>", "");
			rsResponse = rsResponse.replaceAll("<select[^>]*>" , "");
			rsResponse = rsResponse.replaceAll("</select>" , "");
			rsResponse = rsResponse.replaceAll("<input[^>]*>" , "");
			rsResponse = rsResponse.replaceAll("(?is)<option value=\"\">Please Select</option>", "");
	
			rsResponse = rsResponse.replaceAll("(?is)<option value=\\'([^\\']*)\\'[^>]*>([^<]*)</option>", 
					"&nbsp;<input type=\"checkbox\" name=\"" + bookNo + "-$1" + "\" "
								+ "value=\"" + CreatePartialLink(TSConnectionURL.idPOST) + "/indexedbooks.php?indexes=" + bookIndex + "&books=" + bookNo + "&images=$1.pdf\">"
					+ "<A target=\"NEW\" HREF=\"" + CreatePartialLink(TSConnectionURL.idPOST) + "/indexedbooks.php?indexes=" + bookIndex + "&books=" + bookNo + "&images=$1.pdf\">" + bookNo + "-$1</A><BR>");
	        
			if (StringUtils.isNotEmpty(bookNo)){
				rsResponse = "<form name=\"savepdf\" action=\"/title-search/MultiDocSave\" method=\"POST\">" + rsResponse + "<BR><BR>"
					+ "<input name=\"Button\" type=\"button\" class=\"button\" value=\"" + SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL + "\" onClick=\"window.document.forms[0].submit();\">" 
					+ "<input type=\"hidden\" name=\"searchId\" value=\"" + searchId +"\"/>"
					+ "<input type=\"hidden\" name=\"serverId\" value=\"65\"/>"
					+ "<input type=\"hidden\" name=\"searchType\" value=\"MISC\"/>"
					+ "<input type=\"hidden\" name=\"searchTypeModule\" value=\"IM\"/>"
					+ "</form>";
				}
			rsResponse += "</body></html>";
	            
            parser.Parse(Response.getParsedResponse(), rsResponse, ro.cst.tsearch.servers.response.Parser.NO_PARSE);
			
			break;
			
		case ID_SEARCH_BY_MODULE41:
			
			iStart = rsResponse.indexOf("Old Indexed Book Search");
			if (iStart < 0)
				return;
	        
			HtmlParser3 htmlParser = new HtmlParser3(rsResponse);
			NodeList tableList = htmlParser.getNodeList().extractAllNodesThatMatch(new TagNameFilter("table"), true);
			if (tableList != null && tableList.size() > 0){
				for (int i = 0; i < tableList.size(); i++){
					String myTable = tableList.elementAt(i).toHtml();
					if (myTable.indexOf("Index Page Viewer") > 0 && myTable.indexOf("Old Indexed Book Search") < 1){
						rsResponse = tableList.elementAt(i).toHtml();
						break;
					}
				}
			}
			
			rsResponse = rsResponse.replaceAll("(?is)\\b(href=')([^']+)", "$1" + CreatePartialLink(TSConnectionURL.idGET) + "/$2");
			rsResponse = rsResponse.replaceAll("(?is)<input[^>]+>", "");
			String imageLink = rsResponse.replaceFirst("(?is).*<iframe\\s+src\\s*=\\s*\\\"([^\\\"]+)[^>]+>\\s*</iframe>.*", "$1");
			
			String path = StringUtils.substringBetween(imageLink, "datekey=", "&");
			path += "_" + StringUtils.substringBetween(imageLink, "path=", "&");
			path += "_" + StringUtils.substringBetween(imageLink, "bookpath=", "&");
			
			StringBuffer indexNum = new StringBuffer();
			Matcher mat = Pattern.compile("(?is)indexNum=(\\d+)").matcher(rsResponse);
			while (mat.find()){
				indexNum.append(mat.group(1)).append("_");
			}
			
			rsResponse = rsResponse.replaceAll("(?is)<iframe\\s+src\\s*=\\s*\\\"([^\\\"]+)[^>]+>\\s*</iframe>", 
					"</div><div>&nbsp;<input type=\"checkbox\" name=\"" + path + "\" value=\"" + CreatePartialLink(TSConnectionURL.idGET) + "$1.pdf\">"
					+ "<A target=\"NEW\" HREF=\"" + CreatePartialLink(TSConnectionURL.idGET) + "$1.pdf\">" + path + "</A><BR>");
	        
			if (indexNum.length() > 0){
				rsResponse = rsResponse.replaceAll("(?is)\\b(indexNum=&)", "$1allPages=" + indexNum.toString() + "&");
			}
			rsResponse = "<form name=\"savepdf\" action=\"/title-search/MultiDocSave\" method=\"POST\">" + rsResponse + "<BR><BR>"
				+ "<input name=\"Button\" type=\"button\" class=\"button\" value=\"" + SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL + "\" onClick=\"window.document.forms[0].submit();\">" 
				+ "<input type=\"hidden\" name=\"searchId\" value=\"" + searchId +"\"/>"
				+ "<input type=\"hidden\" name=\"serverId\" value=\"65\"/>"
				+ "<input type=\"hidden\" name=\"searchType\" value=\"MISC\"/>"
				+ "<input type=\"hidden\" name=\"searchTypeModule\" value=\"IM\"/>"
				+ "</form>";
			
			rsResponse += "</body></html>";
	            
            parser.Parse(Response.getParsedResponse(), rsResponse, ro.cst.tsearch.servers.response.Parser.NO_PARSE);
            
            break;
		default:
			break;
		}
	}

	protected String getDetails(ServerResponse response, String rsResponse, StringBuilder accountId, HashMap<String, String> data) {
		try {
			StringBuilder details = new StringBuilder();

			NodeList nodes = new HtmlParser3(rsResponse.replaceAll("(?ism)&nbsp;", "")).getNodeList();
			NodeList tables = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true);

			NodeList tds = tables.extractAllNodesThatMatch(new HasAttributeFilter("style", "background-color: #FFFFFF;"), true);
			
			//when saving and document index is loaded from memory
			if (tds.size() == 0){
				tds = tables.extractAllNodesThatMatch(new HasAttributeFilter("id", "summaryTable"), true);
			}

			if (tds.size() > 0) {
				String bookPage = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(tds.elementAt(0).getChildren(), "Book#"), "", true)
						.replaceAll("<[^>]*>", "").trim();
				String instr = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(tds.elementAt(0).getChildren(), "Detail Information for Instrument #"), "", false)
						.replaceAll("<[^>]*>", "").replaceAll("\\s+"," ").replaceAll("(?sim).*Detail Information for Instrument #([^\\s]+) In Year.*", "$1").trim();

				String type = HtmlParser3.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(tds.elementAt(0).getChildren(), "Doc Type:"), "", true)
						.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").replaceAll("Doc Type:", "").trim();
				String year = HtmlParser3
						.getValueFromAbsoluteCell(0, 0, HtmlParser3.findNode(tds.elementAt(0).getChildren(), "Detail Information for Instrument"), "", true)
						.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").replaceAll(".*In Year (\\d{4}).*", "$1").trim();

				if (year.length() > 4) {
					year = "";
				}

				bookPage = bookPage.replaceAll("\\s+", " ").trim();

				String book = "";
				String page = "";

				if (bookPage.contains("Page#")) {
					book = bookPage.split("Page#")[0].replaceAll("(?sim)Book#", "").trim();
					page = bookPage.split("Page#")[1].replace("Monitor Book / Page", "").trim();
				}

				if (book.matches("[A-Z]+")) {
					book = "";
				}

				if (page.matches("0+")) {
					page = "";
				}

				data.put("type", type);
				data.put("book", book);
				data.put("page", page);
				data.put("year", year);
				data.put("docno", instr);

				accountId.append(instr + "_" + book + "_" + page);

				Node n = HtmlParser3.findNode(tds.elementAt(0).getChildren(), "View Image");

				if (n != null && n.getParent() instanceof LinkTag) {
					LinkTag l = (LinkTag) n.getParent();
					if(StringUtils.isNotEmpty(instr)){
						l.setLink(CreatePartialLink(TSConnectionURL.idGET) + "/imgview.php?imgMode=GS&instNum="+ instr + "&year="  +  year);
					} else {
						l.setLink(CreatePartialLink(TSConnectionURL.idGET) + "/imgview.php?" + 
								l.getLink().replaceAll("(?ism)[^\']*\'([^\']*)\'.*", "$1").replaceAll("(?ism)PHPSESSID=[^&]*&","") + "&imgtype=pdf&ACCTID=20113");
					}
					l.setAttribute("target", "_blank");

					response.getParsedResponse().addImageLink(new ImageLinkInPage(l.getLink(), accountId.toString() + ".pdf"));
				}
			} else
				return null;

			/* If from memory - use it as is */
			if (!org.apache.commons.lang.StringUtils.containsIgnoreCase(rsResponse, "<html")) {
				return rsResponse;
			}

			NodeList crossrefs = tds.elementAt(0).getChildren()
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "40%"), true)
					.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"));

			if (crossrefs.size() > 0) {
				TableTag t = (TableTag) crossrefs.elementAt(0);

				TableRow[] rows = t.getRows();

				for (int i = 1; i < rows.length; i++) {
					TableRow r = rows[i];

					TableColumn[] cols = r.getColumns();

					if (r.getColumnCount() == 6 && cols[0].getChildCount() > 1 && cols[0].getChild(1) instanceof FormTag) {
						FormTag f = (FormTag) cols[0].getChild(1);
						InputTag instNum = f.getInputTag("instNum");
						InputTag year = f.getInputTag("year");

						if (year != null && instNum != null) {
							LinkTag l = new LinkTag();
							l.setLink(CreatePartialLink(TSConnectionURL.idGET) + "/" + f.getAttribute("action") + "?instNum=" + instNum.getAttribute("value")
									+ "&year=" + year.getAttribute("value"));

							HtmlHelper.addTagToTag(l, HtmlHelper.createPlainText("Details"));

							NodeList children = new NodeList();
							children.add(l);

							cols[0].removeChild(1);
							cols[0].setChildren(children);
						}
					}
				}
			}

			if (tds.size() > 0) {
				details.append("<table align=\"center\" width=\"95%\" id=\"summaryTable\"><tr><td>" + tds.elementAt(0).getChildren().toHtml() + "</td></tr></table>");
			}

			return details.toString().replaceAll("(?ism)bgcolor=\"#140270\"", "")
					.replaceAll("(?ism)<a[^>]*>[^<]*<-Prev[^<]*</a>", "")
					.replaceAll("(?ism)<a[^>]*>[^<]*Book->[^<]*</a>", "")
					.replaceAll("(?ism)<a[^>]*>Click[^<]*</a>", "")
					.replaceAll("(?ism)<a[^>]*>Monitor[^<]*</a>", "");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private String makeHeader(TableTag resultsTable, int viParseID) {
		if (resultsTable == null)
			return "";

		String header = "<table id=\"intermediaryResults\" align=\"center\" cellspacing=\"0\" cellpadding=\"3\" border=\"1\" width=\"95%\">";

		TableRow r = resultsTable.getRow(0);

		header += r.toHtml().replaceFirst("(?ism)(<tr[^>]*>)", "$1<td>" + SELECT_ALL_CHECKBOXES + "</td>");

		return header;
	}

	@Override
	public String getPrettyFollowedLink (String initialFollowedLnk){	
		if (initialFollowedLnk.contains("instNum=")){
    		String retStr =  "Instrument " + StringUtils.substringBetween(initialFollowedLnk, "instNum=", "&");
    		
    		if (initialFollowedLnk.contains("year=")){
    			retStr += ":" + StringUtils.substringBetween(initialFollowedLnk, "year=", "&");
    		}
    		
    		retStr += " has already been processed from a previous search in the log file.";
    		
    		return  "<br/><span class='followed'>" + retStr + "</span><br/>";
    	}
    	return "<br/><span class='followed'>Link already followed: </span>" + preProcessLink(initialFollowedLnk) + "<br/>";
    }
	
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		Vector<ParsedResponse> intermediaryResponse = new Vector<ParsedResponse>();
		try {
			int numberOfUncheckedElements = 0;
			Integer viParseId = (Integer) mSearch.getAdditionalInfo("viParseID");

			/**
			 * We need to find what was the original search module in case we need some info from it like in the new PS interface
			 */
			TSServerInfoModule moduleSource = null;
			Object objectModuleSource = response.getParsedResponse().getAttribute(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE);
			if (objectModuleSource != null) {
				if (objectModuleSource instanceof TSServerInfoModule) {
					moduleSource = (TSServerInfoModule) objectModuleSource;
				}
			} else {
				objectModuleSource = getSearch().getAdditionalInfo(this.getKeyForSavingInIntermediaryNextLink(response.getQuerry()));
				if (objectModuleSource instanceof TSServerInfoModule) {
					moduleSource = (TSServerInfoModule) objectModuleSource;
				}
			}

			ParsedResponse parsedResponse = response.getParsedResponse();
			String rsResponse = response.getResult();
			rsResponse = rsResponse.replaceAll("(?is)</tr>", "</td></tr>");
			rsResponse = rsResponse.replaceAll("(?is)</td>\\s*</td>", "</td>").replaceAll("(?is)<!--.*?-->?", "");
			rsResponse = rsResponse.replaceAll("(?is)</td>\\s*</td>", "</td>");
			rsResponse = rsResponse.replaceAll("(?is)<div[^>]*>[^<]*\\s*<a[^>]*>\\s*<img[^>]*>\\s*</a>\\s*</div>", "");
//System.out.println(rsResponse);
			org.htmlparser.Parser tableParser = org.htmlparser.Parser.createParser(rsResponse.replaceAll("(?ism)&nbsp;", " "), null);
			NodeList nodes = tableParser.parse(null);
			//NodeList nodes = new HtmlParser3(rsResponse.replaceAll("(?ism)&nbsp;", " ")).getNodeList();
//			rsResponse = Tidy.tidyParse(rsResponse, null);

			NodeList tableList = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("border", "0"), true)
					.extractAllNodesThatMatch(new HasAttributeFilter("width", "95%"), true);

			TableTag intermediary = null;

			boolean multipleTables = true;

			if (tableList == null || tableList.size() == 0) {
				multipleTables = false;
				try {
					tableList = new NodeList(nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("width", "95%"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "4"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"), true).elementAt(0));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (response.getLastURI().toString().contains("dailyNotebook.php")){
				tableList = nodes.extractAllNodesThatMatch(new TagNameFilter("table"), true)
									.extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"))
									.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "4"))
									.extractAllNodesThatMatch(new HasAttributeFilter("border", "1"), true);
				multipleTables = false;
			}
			if (tableList != null && tableList.size() > 0) {

				outputTable.append(tableList.elementAt(0).toHtml());

				if (multipleTables) {
					tableList = tableList.elementAt(0).getChildren().extractAllNodesThatMatch(new HasAttributeFilter("width", "100%"), true)
							.extractAllNodesThatMatch(new HasAttributeFilter("cellpadding", "4"));
				}

				if (tableList != null && tableList.size() > 0) {

					for (int j = 0; j < tableList.size(); j++) {

						intermediary = (TableTag) tableList.elementAt(j);

						TableRow[] rows = intermediary.getRows();
						for (int i = 1; i < rows.length; i++) {
							TableRow row = rows[i];
							if (row.getColumnCount() >= 7) {
								String link = "";
								String docLink = "";

								TableColumn c = row.getColumns()[0];
								if (c.getChildCount() > 1 && c.childAt(1) instanceof LinkTag) {
									LinkTag linkTag = (LinkTag) c.childAt(1);
									docLink = linkTag.getLink();
									link = CreatePartialLink(TSConnectionURL.idGET) + linkTag.getLink();
									link = link.replace("&amp;", "&");
									linkTag.setLink(link);
								}

								if (row.getColumns().length > 7){
									c = row.getColumns()[7];
								} else{
									c = row.getColumns()[6];
								}

								NodeList auxNodes = c.getChildren().extractAllNodesThatMatch(new TagNameFilter("a"), true);
								if (auxNodes.size() > 0) {
									LinkTag linkTag = (LinkTag) auxNodes.elementAt(0);
									String lk = linkTag.getLink();
									if (lk.matches("#_\\d+")){
										lk = linkTag.getAttribute("onclick");
										if (StringUtils.isEmpty(lk)){
											lk = linkTag.getAttribute("onClick");
										}
									}
									lk = lk.replaceAll("(?ism)[^\']*\'([^\']*)\'.*", "$1").replaceAll("(?ism)[^\"]*\"([^\"]*)\".*", "$1");
									linkTag.setLink(CreatePartialLink(TSConnectionURL.idGET) + "/imgview.php?" + lk);
									linkTag.setAttribute("target", "_blank");
								}

								ParsedResponse currentResponse = new ParsedResponse();

								ResultMap resultMap = new ResultMap();
								resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), dataSite.getSiteTypeAbrev());
								
								ro.cst.tsearch.servers.functions.TNGenericTitleSearcherSRC.parseIntermediaryRow(row, viParseId, resultMap);
								GenericFunctions1.setGranteeLanderTrustee2(resultMap, searchId,true);
								GenericFunctions.checkTNCountyROForMERSForMortgage(resultMap, searchId);
								
								Bridge bridge = new Bridge(currentResponse, resultMap, searchId);

								DocumentI document = (RegisterDocumentI) bridge.importData();
								currentResponse.setDocument(document);

								String checkBox = "checked";

								if (isInstrumentSaved("", document, null) && !Boolean.TRUE.equals(getSearch().getAdditionalInfo("RESAVE_DOCUMENT"))) {
									checkBox = "saved";
								} else {
									numberOfUncheckedElements++;
									checkBox = "<input type='checkbox' name='docLink' value='" + link + "'>";
									if (getSearch().getInMemoryDoc(docLink) == null) {
										getSearch().addInMemoryDoc(docLink, currentResponse);
									}
									currentResponse.setPageLink(new LinkInPage(link, link, TSServer.REQUEST_SAVE_TO_TSD));

									/**
									 * Save module in key in additional info. The key is instrument number that should be always available.
									 */
									String keyForSavingModules = this
											.getKeyForSavingInIntermediary(getInstrumentNumberForSavingInFinalResults(document));
									getSearch().setAdditionalInfo(keyForSavingModules, moduleSource);
								}

								String cleanRow = row.toHtml().replaceAll("(?ism)<a href=\"viewDetails.php[^\"]*\">([^<]*)</a>", "$1")
										.replaceAll("(?ism)</?font[^>]*>", "")
										.replaceAll("(?ism)<a name=[^>]*>(.*?)</a>", "$1");
								String rowHtml = "<tr><td>" + checkBox + "</td>" + cleanRow.replaceAll("(?ism)<tr[^>]*>", "").replaceAll("(?ism)</tr>", "")
										+ "</tr>";

								currentResponse.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, rowHtml.replaceAll("(?ism)<a [^>]*>([^<]*)<[^>]*>", "$1"));
								currentResponse.setOnlyResponse(rowHtml);

								intermediaryResponse.add(currentResponse);

							}
						}
					}
				}
			}

			if (getSearch().getSearchType() == Search.PARENT_SITE_SEARCH) {
				parsedResponse.setHeader(CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST")
						+ makeHeader(intermediary, viParseId));
				parsedResponse.setFooter("\n</table><br>" + CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseId, (Integer) numberOfUncheckedElements));
			} else {
				parsedResponse.setHeader("<table border=\"1\">");
				parsedResponse.setFooter("</table>");
			}

			SetAttribute(NUMBER_OF_UNSAVED_DOCUMENTS, numberOfUncheckedElements);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return intermediaryResponse;
	}

	@Override
	public String addRestrictionToolTip(){
		return "<b><font color=\"red\"> Not Applicable for " + Arrays.toString(DOCTYPES_NOT_ALLOWED_FOR_SAVE_WITH_CROSSREFS) + "</font></b>";
	}
	
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap resultMap) {
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), dataSite.getSiteTypeAbrev());
		
		ro.cst.tsearch.servers.functions.TNGenericTitleSearcherSRC.parseAndFillResultMap(response, detailsHtml, resultMap, searchId);
		return null;
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
			DocsValidator[] docsValidatorsCrossref, TSServerInfoModule m) {

		if (m == null) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
		}
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, extraInformation);
		m.setSaObjKey(key);
		m.clearSaKeys();
		m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LFM_NAME_FAKE);
		m.setSaKey(2, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
		m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
		m.setSaKey(3, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);

		for (FilterResponse filterResponse : filters) {
			m.addFilter(filterResponse);
		}
		addFilterForUpdate(m, true);
		for (DocsValidator docsValidator : docsValidators) {
			m.addValidator(docsValidator);
		}
		for (DocsValidator docsValidator : docsValidatorsCrossref) {
			m.addCrossRefValidator(docsValidator);
		}

		m.addCrossRefValidator(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator());

		ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory
				.getConfigurableNameIterator(m, false, searchId, new String[] { "L;F;" });
		nameIterator.setInitAgain(true); // initialize again after all parameters are set
		if (searchedNames != null) {
			nameIterator.setSearchedNames(searchedNames);
		}
		searchedNames = nameIterator.getSearchedNames();

		m.addIterator(nameIterator);
		modules.add(m);

		return searchedNames;
	}

	private LegalDescriptionIterator getLegalDescriptionIterator(boolean lookUpWasDoneWithNames) {
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, dataSite); 

		it.setCheckAlreadyFilledKeyWithDocuments(AdditionalInfoKeys.RO_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR);
		it.setAdditionalInfoKey(AdditionalInfoKeys.RO_LOOK_UP_DATA);
		it.setLoadFromSearchPageIfNoLookup(true);
		it.setLoadFromSearchPage(false);
		it.setEnableTownshipLegal(false);
		it.setEnableSubdividedLegal(true);
		it.setEnableSubdivision(true);

		return it;
	}

	class LegalDescriptionIterator extends GenericRuntimeIterator<LegalStruct> implements LegalDescriptionIteratorI{

		public LegalDescriptionIterator(long searchId, DataSite dataSite) {
			super(searchId);
			allSubdivisionNames = new HashSet<String>();
			setDataSite(dataSite);
		}

		private static final long serialVersionUID = -475674567472109L;
		protected Set<LegalStruct> legalStruct;
		private Set<String> allSubdivisionNames = null;
		private String subdivisionSetKey = AdditionalInfoKeys.SUBDIVISION_NAME_SET;
		
		private String checkAlreadyFilledKeyWithDocuments = null;
		private String additionalInfoKey = AdditionalInfoKeys.RO_LOOK_UP_DATA;
		private boolean loadFromSearchPage = true;
		private boolean loadFromSearchPageIfNoLookup = false;
		
		private boolean enableSubdividedLegal = true;
		private boolean enableTownshipLegal = true;
		private boolean enableSubdivision = true;
		
		
		
		@Override
		public boolean isTransferAllowed(RegisterDocumentI doc) {
			return doc != null && doc.isOneOf(DocumentTypes.TRANSFER);
		}
		
		public List<RegisterDocumentI> getGoodDocumentsOrForCurrentOwner(LegalDescriptionIteratorI legalDescriptionIteratorI, DocumentsManagerI m, 
																		Search search, boolean applyNameMatch){
			final List<RegisterDocumentI> ret = new ArrayList<RegisterDocumentI>();
			
			List<RegisterDocumentI> listRodocs = m.getRealRoLikeDocumentList();
			DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);
			
			SearchAttributes sa	= search.getSa();
			PartyI owner 		= sa.getOwners();
			
			
			for (RegisterDocumentI doc : listRodocs){
				boolean found = false;
				for (PropertyI prop : doc.getProperties()){
					if (((doc.isOneOf("MORTGAGE", "RELEASE") || legalDescriptionIteratorI.isTransferAllowed(doc)) && applyNameMatch)
							|| ((doc.isOneOf("MORTGAGE") || legalDescriptionIteratorI.isTransferAllowed(doc)) && !applyNameMatch)){
						if (prop.hasSubdividedLegal()){
							SubdivisionI sub = prop.getLegal().getSubdivision();
							LegalStruct ret1 = new LegalStruct(false);
							
							ret1.setAddition(sub.getName());
							ret1.setDistrict(sub.getDistrict());
							ret1.setLot(sub.getLot());
							ret1.setBlock(sub.getBlock());
							
							ret1.setPlatBook(sub.getPlatBook());
							ret1.setPlatPage(sub.getPlatPage());
							
							legalDescriptionIteratorI.loadSecondaryPlattedLegal(prop.getLegal(), ret1);
							
							boolean nameMatched = false;
							
							if (applyNameMatch){
								if (GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantor(), NameFilterFactory.NAME_FILTER_THRESHOLD) 
										|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantor(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)
										|| GenericNameFilter.isMatchGreaterThenScore(owner, doc.getGrantee(), NameFilterFactory.NAME_FILTER_THRESHOLD) 
										|| GenericNameFilter.isMatchGreaterThenScore(doc.getGrantee(), owner, NameFilterFactory.NAME_FILTER_THRESHOLD)){
										nameMatched = true;
								}
							}
							boolean isDistrictNeeded = isDistrictSearchCounty(dataSite);
							
							if ((nameMatched || !applyNameMatch) 
									&& (StringUtils.isNotEmpty(ret1.getAddition()) || (isDistrictNeeded && StringUtils.isNotEmpty(ret1.getDistrict())))){
								ret.add(doc);
								found = true;
								break;
							}
						}
					}
				}
				if (found){
					break;
				}
			}
			
			return ret;
		}
		
		public boolean isDistrictSearchCounty(DataSite dataSite){
			if (dataSite.getCountyId() == CountyConstants.TN_Johnson){
				return true;
			}
			return false;
		}
		
		protected List<LegalStruct> createDerrivations() {

			Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			DocumentsManagerI m = global.getDocManager();
			legalStruct = (Set<LegalStruct>) global.getAdditionalInfo(AdditionalInfoKeys.RO_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR);

			String aoAndTrLots = global.getSa().getAtribute(SearchAttributes.LD_LOTNO);
			HashSet<String> allAoAndTrlots = new HashSet<String>();

			String aoAndTrBloks = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
			HashSet<String> allAoAndTrBloks = new HashSet<String>();

			
			if (aoAndTrLots.contains(",") || aoAndTrLots.contains(" ") || aoAndTrLots.contains("-") || aoAndTrLots.contains(";")) {
				if (!StringUtils.isEmpty(aoAndTrLots)) {
					for (LotInterval interval : LotMatchAlgorithm.prepareLotInterval(aoAndTrLots)) {
						allAoAndTrlots.addAll(interval.getLotList());
					}
				}
			} else {
				if (!StringUtils.isEmpty(aoAndTrLots)) {
					allAoAndTrlots.add(aoAndTrLots);
				}
			}
			if (!StringUtils.isEmpty(aoAndTrBloks)) {
				for (LotInterval interval : LotMatchAlgorithm.prepareLotInterval(aoAndTrBloks)){
					allAoAndTrBloks.addAll(interval.getLotList());
				}
			}

			if (legalStruct == null){
				legalStruct = new HashSet<LegalStruct>();
				try {
					m.getAccess();
					List<RegisterDocumentI> listRodocs = getGoodDocumentsOrForCurrentOwner(this, m, global, true);

					if (listRodocs == null || listRodocs.size() == 0){
						listRodocs = getGoodDocumentsOrForCurrentOwner(this, m, global, false);
					}
					if (listRodocs == null || listRodocs.size() == 0){
						for (DocumentI doc : m.getDocumentsWithDataSource(true, dataSite.getSiteTypeAbrev())){
							if (doc instanceof RegisterDocumentI){
								listRodocs.add((RegisterDocumentI) doc);
							}
						}
					}
					DocumentUtils.sortDocuments(listRodocs, MultilineElementsMap.DATE_ORDER_DESC);

					for (RegisterDocumentI reg : listRodocs){
						if (!reg.isOneOf(DocumentTypes.PLAT, DocumentTypes.RESTRICTION, DocumentTypes.EASEMENT, DocumentTypes.MASTERDEED,
								DocumentTypes.COURT, DocumentTypes.LIEN, DocumentTypes.CORPORATION, DocumentTypes.AFFIDAVIT, DocumentTypes.CCER)){

							List<LegalStruct> tempLegalStructListPerDocument = new ArrayList<LegalStruct>();
							for (PropertyI prop : reg.getProperties()){
								if (prop.hasLegal()){
									LegalI legal = prop.getLegal();
									if (legal.hasSubdividedLegal()){

										SubdivisionI subdiv = legal.getSubdivision();
										
										String block = subdiv.getBlock();
										
										String subdivName = subdiv.getName();
										String platBook = subdiv.getPlatBook();
										String platPage = subdiv.getPlatPage();
										
										saveSubdivisionName(subdivName);

										String district = subdiv.getDistrict();
										
										String lot = subdiv.getLot();
										if (StringUtils.isNotEmpty(lot)){
											HashSet<String> lotsExpanded = new HashSet<String>();
											for (LotInterval interval : LotMatchAlgorithm.prepareLotInterval(lot)) {
												lotsExpanded.addAll(interval.getLotList());
											}
											
											for (String lt : lotsExpanded) {
												LegalStruct legalStructItem = new LegalStruct(false);
												legalStructItem.setDistrict(StringUtils.defaultString(district));
												legalStructItem.setBlock(StringUtils.defaultString(block));
												legalStructItem.setPlatBook(StringUtils.defaultString(platBook));
												legalStructItem.setPlatPage(StringUtils.defaultString(platPage));
												legalStructItem.setAddition(StringUtils.defaultString(subdivName));

												lt = StringUtils.stripStart(lt, "0");
												legalStructItem.setLot(lt);	

												legalStruct.add(legalStructItem);
											}
										} else{
											LegalStruct legalStructItem = new LegalStruct(false);
											legalStructItem.setDistrict(StringUtils.defaultString(district));
											legalStructItem.setBlock(StringUtils.defaultString(block));
											legalStructItem.setPlatBook(StringUtils.defaultString(platBook));
											legalStructItem.setPlatPage(StringUtils.defaultString(platPage));
											legalStructItem.setAddition(StringUtils.defaultString(subdivName));

											legalStruct.add(legalStructItem);
										}
									}
								}
							}
							if (allAoAndTrlots.size() > 0) {
								List<LegalStruct> aoAndTrMatches = new ArrayList<LegalStruct>();
								for (LegalStruct item : tempLegalStructListPerDocument) {
									if (!StringUtils.isEmpty(item.getLot())) {
										for (String lt : allAoAndTrlots) {
											lt = StringUtils.stripStart(lt, "0");
											if (GenericLegal.computeScoreInternal("lot", lt, item.getLot(), false, false) >= 0.8
													|| GenericLegal.computeScoreInternal("lot", item.getLot(), lt, false, false) >= 0.8) {
												aoAndTrMatches.add(item);
											}
										}
									}
								}
								if (aoAndTrMatches.size() > 0) {
									legalStruct.addAll(aoAndTrMatches);
								}
							}
						}
					}

					legalStruct = keepOnlyGoodLegals(legalStruct);

					try {
						DataSite dataSite = HashCountyToIndex.getCrtServer(searchId, false);
						long miServerId = dataSite.getServerId();

						for (LegalI legal : global.getSa().getForUpdateSearchLegalsNotNull(miServerId)) {
							if (legal.hasSubdividedLegal()) {

								LegalStruct legalStructItem = new LegalStruct(false);
								SubdivisionI subdiv = legal.getSubdivision();

								String block = subdiv.getBlock();
								String lot = subdiv.getLot();
								String subdivision = subdiv.getName();
								String district = subdiv.getDistrict();
								String platBook = subdiv.getPlatBook();
								String platPage = subdiv.getPlatPage();

								legalStructItem.setBlock(StringUtils.defaultString(block));
								legalStructItem.setAddition(StringUtils.defaultString(subdivision));
								legalStructItem.setDistrict(StringUtils.defaultString(district));
								legalStructItem.setPlatBook(StringUtils.isEmpty(platBook) ? "" : platBook);
								legalStructItem.setPlatPage(StringUtils.isEmpty(platPage) ? "" : platPage);

								if (StringUtils.isNotEmpty(lot)) {
									lot = StringUtils.stripStart(lot, "0");
									legalStructItem.setLot(lot);
								}

								legalStruct.add(legalStructItem);
							}
						}
					} catch (Exception e) {
						logger.error("Error loading names for Update saved from Parent Site", e);
					}
					legalStruct = keepOnlyGoodLegals(legalStruct);

					HashSet<LegalStruct> tempLegalStruct = new HashSet<LegalStruct>();

					if (isLoadFromSearchPage() || (legalStruct.isEmpty() && isLoadFromSearchPageIfNoLookup())){

						String subdivisionBoots = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_NAME);
						if (StringUtils.isNotEmpty(subdivisionBoots)){
							StringBuilder sb = new StringBuilder();
							for (String lot : allAoAndTrlots) {
								lot = StringUtils.stripStart(lot, "0");
								sb.append(lot).append(" ");
							}
							if (sb.length() > 0){
								for (LotInterval interval : LotMatchAlgorithm.prepareLotInterval(sb.toString())) {
									int lot = interval.getLow();
									if (lot == 0) {
										continue;
									}
									LegalStruct struct = new LegalStruct(false);
									struct.setLot(Integer.toString(lot));
		
									struct.setAddition(subdivisionBoots);
									struct.setBlock(aoAndTrBloks);
									
									tempLegalStruct.add(struct);
								}
							} else{
								LegalStruct struct = new LegalStruct(false);
	
								struct.setAddition(subdivisionBoots);
								struct.setBlock(aoAndTrBloks);
								
								tempLegalStruct.add(struct);
							}
							saveSubdivisionName(subdivisionBoots);
							legalStruct.addAll(tempLegalStruct);
						}
				}				
				legalStruct = keepOnlyGoodLegals(legalStruct);

				global.setAdditionalInfo(AdditionalInfoKeys.RO_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR, legalStruct);
				global.setAdditionalInfo(this.getAdditionalInfoKey(), legalStruct);
				
				if (allSubdivisionNames != null && allSubdivisionNames.size() > 0) {
					global.setAdditionalInfo(subdivisionSetKey, allSubdivisionNames);
				}

				} finally {
					m.releaseAccess();
				}
			}
			return new ArrayList<LegalStruct>(legalStruct);
		}	

		protected void loadDerrivation(TSServerInfoModule module, LegalStruct str) {
			for (Object functionObject : module.getFunctionList()) {
				if (functionObject instanceof TSServerInfoFunction) {
					TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
					switch (function.getIteratorType()) {
						case FunctionStatesIterator.ITERATOR_TYPE_NAME_TYPE:
							function.setParamValue(str.getAddition());
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_LOT:
							function.setParamValue(str.getLot());
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_BLOCK:
							function.setParamValue(str.getBlock());
							break;
						case FunctionStatesIterator.ITERATOR_TYPE_GENERIC_67:
							if (isDistrictSearchCounty(getDataSite())){
								function.setParamValue(str.getDistrict());
							}
							break;
					}
				}
			}
		}
		
		private Set<LegalStruct> keepOnlyGoodLegals(Set<LegalStruct> legals){
			Set<LegalStruct> good = new HashSet<LegalStruct>();
			for (LegalStruct str : legals){
				if (!incompleteData(str)){
					good.add(str);
				}
			}
			return good;
		}
		
		private boolean incompleteData(LegalStruct str) {
			if (str == null){
				return true;
			}
				
			boolean emptyAddition = StringUtils.isEmpty(str.getAddition());
			boolean emptyDistrict = StringUtils.isEmpty(str.getDistrict());
				
			if (isDistrictSearchCounty(dataSite)){
				return emptyDistrict;
			}
				
			return emptyAddition;
		}
		
		public void saveSubdivisionName(String addiction) {
			if (addiction != null) {
				allSubdivisionNames.add(addiction.toUpperCase().trim());
			}
		}
		
		@Override
		public void loadSecondaryPlattedLegal(LegalI legal, LegalStruct legalStruct) {
			
		}
		
		public String getCheckAlreadyFilledKeyWithDocuments() {
			return checkAlreadyFilledKeyWithDocuments;
		}

		public void setCheckAlreadyFilledKeyWithDocuments(
				String checkAlreadyFilledKeyWithDocuments) {
			this.checkAlreadyFilledKeyWithDocuments = checkAlreadyFilledKeyWithDocuments;
		}
		
		public String getAdditionalInfoKey() {
			return additionalInfoKey;
		}

		public void setAdditionalInfoKey(String additionalInfoKey) {
			this.additionalInfoKey = additionalInfoKey;
		}
		
		public boolean isEnableSubdivision() {
			return enableSubdivision;
		}
		
		public void setEnableSubdivision(boolean enableSubdivision) {
			this.enableSubdivision = enableSubdivision;
		}
		
		public boolean isEnableSubdividedLegal() {
			return enableSubdividedLegal;
		}

		public void setEnableSubdividedLegal(boolean enableSubdividedLegal) {
			this.enableSubdividedLegal = enableSubdividedLegal;
		}

		public boolean isEnableTownshipLegal() {
			return enableTownshipLegal;
		}

		public void setEnableTownshipLegal(boolean enableTownshipLegal) {
			this.enableTownshipLegal = enableTownshipLegal;
		}
		
		public boolean isLoadFromSearchPage() {
			return loadFromSearchPage;
		}

		/**
		 * Load information from search page not just from lookup<br>
		 * Default is to load
		 * @param loadFromSearchPage
		 */
		public void setLoadFromSearchPage(boolean loadFromSearchPage) {
			this.loadFromSearchPage = loadFromSearchPage;
		}

		public boolean isLoadFromSearchPageIfNoLookup() {
			return loadFromSearchPageIfNoLookup;
		}
		
		public void setLoadFromSearchPageIfNoLookup(boolean loadFromSearchPageIfNoLookup) {
			this.loadFromSearchPageIfNoLookup = loadFromSearchPageIfNoLookup;
		}
	}
		
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		Search global = getSearch();
		int searchType = global.getSearchType();
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();

		if (searchType == Search.AUTOMATIC_SEARCH) {
			TSServerInfoModule m = null;

			FilterResponse defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m);
			DocsValidator rejectSavedDocuments = (new RejectAlreadySavedDocumentsFilterResponse(searchId)).getValidator();
			DocsValidator betweenDatesValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId).getValidator();
			
			GenericMultipleLegalFilter genericMultipleLegalFilter = new GenericMultipleLegalFilter(searchId);
			genericMultipleLegalFilter.setAdditionalInfoKey(AdditionalInfoKeys.RO_LOOK_UP_DATA);
			genericMultipleLegalFilter.setUseLegalFromSearchPage(true);
			genericMultipleLegalFilter.setEnableDistrict(true);
			DocsValidator genericMultipleLegalValidator = genericMultipleLegalFilter.getValidator();
			
			SubdivisionFilter subdivisionFilter = new SubdivisionFilter(searchId);
			DocsValidator subdivisionValidator = subdivisionFilter.getValidator();
			
			FilterResponse defaultSingleLegalFilter = LegalFilterFactory.getDefaultLegalFilter(searchId);
			((GenericLegal)defaultSingleLegalFilter).setEnableDistrict(true);
			
			DocsValidator defaultSingleLegalValidator = defaultSingleLegalFilter.getValidator();
			DocsValidator lastTransferDateFilter = new LastTransferDateFilter(searchId).getValidator();

			GenericInstrumentFilter gnif = new GenericInstrumentFilter(searchId) {
				/**
				 * 
				 */
				private static final long	serialVersionUID	= 1L;

				@Override
				public String getCandidateBook(String book, String refBook) {
					
					if (dataSite.getCountyId() == CountyConstants.TN_Lincoln){
						return book;
					} else if (dataSite.getCountyId() == CountyConstants.TN_Anderson
							|| dataSite.getCountyId() == CountyConstants.TN_Bradley
							|| dataSite.getCountyId() == CountyConstants.TN_Coffee
							|| dataSite.getCountyId() == CountyConstants.TN_Greene
							|| dataSite.getCountyId() == CountyConstants.TN_Hawkins
							|| dataSite.getCountyId() == CountyConstants.TN_Jefferson
							|| dataSite.getCountyId() == CountyConstants.TN_Marion
							|| dataSite.getCountyId() == CountyConstants.TN_Maury
							|| dataSite.getCountyId() == CountyConstants.TN_Shelby
							|| dataSite.getCountyId() == CountyConstants.TN_Smith
							|| dataSite.getCountyId() == CountyConstants.TN_Sullivan
							|| dataSite.getCountyId() == CountyConstants.TN_Union
							|| dataSite.getCountyId() == CountyConstants.TN_Van_Buren
							|| dataSite.getCountyId() == CountyConstants.TN_Williamson

							){
						return book;
					} else if (dataSite.getCountyId() == CountyConstants.TN_Fentress){
						if (book.matches("(?is)\\AMISC\\d+")){
							return book;
						} else if (book.matches("(?is)\\A[A-Z]{2}[A-Z]\\d+")){
							return book.replaceFirst("\\A[A-Z]{2}", "");
						}
					} else if (dataSite.getCountyId() == CountyConstants.TN_Roane){
						if (book.matches("(?is)\\A[A-Z][A-Z]\\d+")){
							return book.replaceFirst("\\A[A-Z]", "");
						}
					} else{
						return book.replaceFirst("(?is)\\A[A-Z]+", "");
					}
					
					return book;
				}
				
				@Override
				public String getFilterBook(String book) {
					book = StringUtils.defaultString(book);
//					if (book.matches("(?is)\\A[A-Z]\\d+")){
//						return book.replaceFirst("\\A[A-Z]", "");
//					} else if (book.matches("(?is)\\A[A-Z]{2}[A-Z]\\d+")){
//						return book.replaceFirst("\\A[A-Z]{2}", "");
//					} else if (book.matches("(?is)\\AMISC\\d+")){
//						return book;
//					}
					
					if (dataSite.getCountyId() == CountyConstants.TN_Jackson 
							|| dataSite.getCountyId() == CountyConstants.TN_Lawrence
							|| dataSite.getCountyId() == CountyConstants.TN_Unicoi
							
							){
						return book.replaceFirst("\\A[A-Z]{2}", "");
					} if (dataSite.getCountyId() == CountyConstants.TN_Claiborne
							){
						return book.replaceFirst("\\A[A-Z]", "");
					} else{
						return book;
					}
				}
			};

			boolean lookUpWasDoneWithNames = true;
			{
				/**
				 * Searching with instruments extracted from AO-like, TR-like
				 */
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);
				m.clearSaKeys();

				InstrumentGenericIterator instrumentNoInterator = new InstrumentGenericIterator(searchId, dataSite){
					
					/**
					 * 
					 */
					private static final long serialVersionUID = 5245053541331792792L;

					@Override
					protected String cleanInstrumentNo(String instno, int year) {
						if (instno.length() > 0){
							if (getDataSite().getCountyId() == CountyConstants.TN_Shelby && StringUtils.isAlpha(instno.substring(0, 1))){
								return instno;
							}
							return StringUtils.leftPad(instno, 8, "0");
						}
						
						return instno;
					}
					
					@Override
					public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
						String instno = state.getInstno();
						
						if (instno.length() > 0){
							if (getDataSite().getCountyId() == CountyConstants.TN_Shelby && StringUtils.isAlpha(instno.substring(0, 1))){
								return instno;
							}
							return StringUtils.leftPad(instno, 8, "0");
						}
						
						return instno;
					}
				};
				m.addIterator(instrumentNoInterator);
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);

				// add module only if we have something to search with
				if (!instrumentNoInterator.createDerrivations().isEmpty()) {
					m.addFilter(gnif);
					m.addValidator(defaultSingleLegalValidator);
					m.addValidator(subdivisionValidator);
					m.addValidator(betweenDatesValidator);
					m.addCrossRefValidator(defaultSingleLegalValidator);
					m.addCrossRefValidator(subdivisionValidator);
					m.addCrossRefValidator(betweenDatesValidator);
					modules.add(m);
					lookUpWasDoneWithNames = false;
				}

				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_BP);
				m.clearSaKeys();

				InstrumentGenericIterator bookPageIterator = new InstrumentGenericIterator(searchId, getDataSite());
				bookPageIterator.enableBookPage();
				m.addIterator(bookPageIterator);
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);

				if (!bookPageIterator.createDerrivations().isEmpty()) {
					m.addFilter(gnif);
					m.addValidator(defaultSingleLegalValidator);
					m.addValidator(subdivisionValidator);
					m.addValidator(betweenDatesValidator);
					m.addCrossRefValidator(defaultSingleLegalValidator);
					m.addCrossRefValidator(subdivisionValidator);
					m.addCrossRefValidator(betweenDatesValidator);
					modules.add(m);
					lookUpWasDoneWithNames = false;
				}
				if (lookUpWasDoneWithNames) {
					SearchLogger.info("<font color='red'><b> No valid transaction History Detected and Not enough info for Subdivision Search. We must perform Name Look Up.</b></font></br>",
							searchId);
				}
				
				if (lookUpWasDoneWithNames) {
					addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS, null,
							new FilterResponse[] { defaultNameFilter },
							new DocsValidator[] { defaultSingleLegalValidator, lastTransferDateFilter, subdivisionValidator, rejectSavedDocuments },
							new DocsValidator[] { defaultSingleLegalValidator, subdivisionValidator, lastTransferDateFilter, rejectSavedDocuments });
				}
			}

			{
				/**
				 * Searching with plated legal. We must have subdivision name and at least lot or block
				 */
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_MODULE_IDX));
				m.clearSaKeys();

				LegalDescriptionIterator it = getLegalDescriptionIterator(lookUpWasDoneWithNames);
				m.addIterator(it);

				m.setSaKey(6, SearchAttributes.FROMDATE_MM_SLASH_DD_SLASH_YYYY);
				m.setSaKey(7, SearchAttributes.TODATE_MM_SLASH_DD_SLASH_YYYY);
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_GENERIC_67);//District
				m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_NAME_TYPE);
				m.setIteratorType(2, FunctionStatesIterator.ITERATOR_TYPE_LOT);
				m.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_BLOCK);

				m.addFilter(genericMultipleLegalFilter);
				
				m.addValidator(genericMultipleLegalValidator);
				m.addValidator(subdivisionValidator);
				m.addValidator(lastTransferDateFilter);
				m.addValidator(rejectSavedDocuments);
				
				m.addCrossRefValidator(genericMultipleLegalValidator);
				m.addCrossRefValidator(lastTransferDateFilter);
				m.addCrossRefValidator(betweenDatesValidator);
				modules.add(m);
			}

			ArrayList<NameI> searchedNames = null;
			{
				/**
				 * Owner search by name
				 */
				if (hasOwner()) {
					searchedNames = addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS, null,
							new FilterResponse[] { defaultNameFilter },
							new DocsValidator[] { genericMultipleLegalValidator, lastTransferDateFilter, subdivisionValidator, rejectSavedDocuments },
							new DocsValidator[] { genericMultipleLegalValidator, subdivisionValidator, lastTransferDateFilter, rejectSavedDocuments });
				}
			}

			{
				/**
				 * OCR last transfer - instrument number search (no Book-Page yet)
				 */

				// P6 OCR last transfer - instrument search
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_INST);
				m.clearSaKeys();
				m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
				m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
				m.addFilter(new GenericInstrumentFilter(searchId));
				m.addFilter(genericMultipleLegalFilter);
				m.addValidator(genericMultipleLegalValidator);
				m.addValidator(subdivisionValidator);
				m.addCrossRefValidator(defaultSingleLegalValidator);
				m.addCrossRefValidator(subdivisionValidator);
				modules.add(m);

				GenericInstrumentFilter gnifPlat = new GenericInstrumentFilter(searchId) {
					/**
					 * 
					 */
					private static final long	serialVersionUID	= 1L;

					@Override
					public String getCandidateBook(String book, String refBook) {
						
						if (StringUtils.isNotEmpty(refBook) && refBook.trim().equalsIgnoreCase(book.trim())){
							return book;
						}
						return book.replaceFirst("(?is)\\A\\s*[A-Z]+", "");
					}
				};
				gnifPlat.setCheckForDoctype(true);
				
				// OCR last transfer - book page search
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_BP);
				m.clearSaKeys();
				m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
				m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
				m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
				
				OcrOrBootStraperIterator ocrBPIteratoriterator = new OcrOrBootStraperIterator(searchId, getDataSite()) {
					private static final long serialVersionUID = 1L;

					@Override
					public Object current() {
						Instrument instr = ((Instrument) getStrategy().current());
				        
				        TSServerInfoModule crtState = new TSServerInfoModule(initialState);
				        
				        List<FilterResponse> allFilters = crtState.getFilterList();
						GenericInstrumentFilter gif = null;
						HashMap<String, String> filterCriteria = null;
						if (allFilters != null) {
							for (FilterResponse filterResponse : allFilters) {
								if (filterResponse instanceof GenericInstrumentFilter) {
									gif = (GenericInstrumentFilter) filterResponse;
									filterCriteria = new HashMap<String, String>();
									gif.clearFilters();
								}
							}
						}
				        
				        for (int i =0; i< crtState.getFunctionCount(); i++){
				            TSServerInfoFunction fct = crtState.getFunction(i);
				            if (StringUtils.isEmpty(instr.getInstrumentNo())){
				            	filterCriteria.put(GenericInstrumentFilter.GENERIC_INSTRUMENT_FILTER_KEY_DOCTYPE, instr.getRealdoctype());
				            	
					            if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH) {
					                
					                fct.setParamValue( instr.getBookNo() );
					                if (filterCriteria != null) {
										filterCriteria.put(GenericInstrumentFilter.GENERIC_INSTRUMENT_FILTER_KEY_BOOK, instr.getBookNo());
									}
					            } else if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH) {
					                fct.setParamValue(instr.getPageNo());
					                if (filterCriteria != null) {
										filterCriteria.put(GenericInstrumentFilter.GENERIC_INSTRUMENT_FILTER_KEY_PAGE, instr.getPageNo());
									}
					            } else if(fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_DOCTYPE_SEARCH) {
					            	fct.setParamValue(instr.getRealdoctype());
					            }
				            } else{
					            if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH) {
					                
					                fct.setParamValue(instr.getInstrumentNo());
					                if (filterCriteria != null) {
										filterCriteria.put(GenericInstrumentFilter.GENERIC_INSTRUMENT_FILTER_KEY_INSTRUMENT_NUMBER, instr.getInstrumentNo());
									}
					            }
				            }
				        }
				        if (gif != null) {
							gif.addDocumentCriteria(filterCriteria);
						}
				        return  crtState ;
					}
				};

				ocrBPIteratoriterator.setInitAgain(true);
				m.addIterator(ocrBPIteratoriterator);

				m.addFilter(gnifPlat);
				m.addFilter(genericMultipleLegalFilter);
				m.addValidator(genericMultipleLegalValidator);
				m.addValidator(subdivisionValidator);
				m.addCrossRefValidator(defaultSingleLegalValidator);
				m.addCrossRefValidator(subdivisionValidator);
				modules.add(m);
			}

			{
				/**
				 * Owner name module with extra names from search page (for example added by OCR)
				 */

				searchedNames = addNameSearch(modules, serverInfo, SearchAttributes.OWNER_OBJECT, TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS, searchedNames,
						new FilterResponse[] { defaultNameFilter },
						new DocsValidator[] { genericMultipleLegalValidator, lastTransferDateFilter, subdivisionValidator, rejectSavedDocuments },
						new DocsValidator[] { genericMultipleLegalValidator, subdivisionValidator, lastTransferDateFilter, rejectSavedDocuments });

			}

			/**
			 * Buyer Search
			 */
			if (hasBuyer()) {

				FilterResponse nameFilterBuyer = NameFilterFactory.getDefaultNameFilter(SearchAttributes.BUYER_OBJECT, getSearch().getID(), null);

				addNameSearch(modules, serverInfo, SearchAttributes.BUYER_OBJECT, TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS, null,
						new FilterResponse[] { nameFilterBuyer },
						new DocsValidator[] { DoctypeFilterFactory.getDoctypeBuyerFilter(searchId).getValidator(), lastTransferDateFilter,
								nameFilterBuyer.getValidator(), rejectSavedDocuments },
						new DocsValidator[] { genericMultipleLegalValidator, lastTransferDateFilter, rejectSavedDocuments });
			}

			{
				
				GenericInstrumentFilter gnifr = new GenericInstrumentFilter(searchId);
				
				/**
				 * Ro docs references
				 */
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_INSTR);
				m.clearSaKeys();
	
				InstrumentGenericIterator instrumentNoInterator = new InstrumentGenericIterator(searchId, getDataSite()){
					
					/**
					 * 
					 */
					private static final long serialVersionUID = 5245435645451792792L;

					@Override
					protected String cleanInstrumentNo(String instno, int year) {
						if (instno.length() > 0){
							if (dataSite.getCountyId() == CountyConstants.TN_Shelby && StringUtils.isAlpha(instno.substring(0, 1))){
								return instno;
							}
							
							return StringUtils.leftPad(instno, 8, "0");
						}
						
						return instno;
					}
					
					@Override
					public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
						String instno = state.getInstno();
						
						if (instno.length() > 0){
							if (dataSite.getCountyId() == CountyConstants.TN_Shelby && StringUtils.isAlpha(instno.substring(0, 1))){
								return instno;
							}
							return StringUtils.leftPad(instno, 8, "0");
						}
						
						return instno;
					}
				};
				instrumentNoInterator.setLoadFromRoLike(true);
				instrumentNoInterator.setDsToLoad(new String[]{dataSite.getSiteTypeAbrev()});
				m.addIterator(instrumentNoInterator);
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
	
				m.addFilter(gnifr);
				m.addFilter(genericMultipleLegalFilter);
				
				m.addValidator(genericMultipleLegalValidator);
				m.addValidator(subdivisionValidator);
				m.addValidator(betweenDatesValidator);
				m.addCrossRefValidator(defaultSingleLegalValidator);
				m.addCrossRefValidator(subdivisionValidator);
				m.addCrossRefValidator(betweenDatesValidator);
				modules.add(m);
	
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, TSServerInfoConstants.VALUE_PARAM_LIST_RO_CROSSREF_BP);
				m.clearSaKeys();
	
				InstrumentGenericIterator bookPageIterator = new InstrumentGenericIterator(searchId, getDataSite());
				bookPageIterator.enableBookPage();
				bookPageIterator.setLoadFromRoLike(true);
				bookPageIterator.setDsToLoad(new String[]{dataSite.getSiteTypeAbrev()});
				m.addIterator(bookPageIterator);
				m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
				m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
	
				m.addFilter(gnifr);
				m.addFilter(genericMultipleLegalFilter);
				
				m.addValidator(genericMultipleLegalValidator);
				m.addValidator(subdivisionValidator);
				m.addValidator(betweenDatesValidator);
				m.addCrossRefValidator(defaultSingleLegalValidator);
				m.addCrossRefValidator(subdivisionValidator);
				m.addCrossRefValidator(betweenDatesValidator);
				modules.add(m);
			}
		}
		serverInfo.setModulesForAutoSearch(modules);
	}

	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data) {
		return isInstrumentSaved(instrumentNo, documentToCheck, data, false);
	}

	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId) {

		DocumentsManagerI documentManager = getSearch().getDocManager();
		try {
			documentManager.getAccess();
			if (documentToCheck != null) {
				if (documentManager.getDocument(documentToCheck.getInstrument()) != null)
					return true;
			} else {
				InstrumentI instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
				if (data != null) {
					if (!StringUtils.isEmpty(data.get("type"))) {
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
				} catch (Exception e) {
				}

				if (documentManager.getDocument(instr) != null) {
					return true;
				} else {
					List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);

					if (data != null) {
						if (!StringUtils.isEmpty(data.get("type"))) {
							String serverDocType = data.get("type");
							String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
							for (DocumentI documentI : almostLike) {
								if (documentI.getDocType().equals(docCateg)) {
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

		if (documentToCheck == null) {
			return false;
		}
		try {
			documentManager.getAccess();
			InstrumentI instToCheck = documentToCheck.getInstrument();
			for (DocumentI e : documentManager.getDocumentsWithDataSource(false, "SRC", "ST", "RO")) {
				InstrumentI savedInst = e.getInstrument();
				boolean savedInstNo = savedInst.getInstno().equals(instToCheck.getInstno());
				if ((org.apache.commons.lang.StringUtils.isEmpty(savedInst.getInstno()) 
						&& org.apache.commons.lang.StringUtils.isNotEmpty(instToCheck.getInstno()))
					|| (org.apache.commons.lang.StringUtils.isNotEmpty(savedInst.getInstno()) 
							&& org.apache.commons.lang.StringUtils.isEmpty(instToCheck.getInstno()))){
					
					savedInstNo = true;
				}
				if (savedInstNo
						&& (savedInst.getBook().equals(instToCheck.getBook()) && savedInst.getPage().equals(instToCheck.getPage()))
						&& savedInst.getDocno().equals(instToCheck.getDocno())
						&& e.getDocType().equals(documentToCheck.getDocType())
						&& savedInst.getYear() == instToCheck.getYear()) {
					return true;
				}
			}
		} finally {
			documentManager.releaseAccess();
		}

		return false;
	}

	
	@Override
	protected DownloadImageResult saveImage(ImageI image) throws ServerResponseException {
		String link = "";
		if (image.getLink(0).split("&Link=").length == 2) {
			link = image.getLink(0).split("&Link=")[1];
		} else
			link = image.getLink(0).split("&Link=")[0];

		byte[] imageBytes = null;

		HttpSite site = HttpManager.getSite(getCurrentServerName(), searchId);
		try {
			imageBytes = ((ro.cst.tsearch.connection.http2.TNGenericTitleSearcherSRC) site)
					.process(new HTTPRequest(getDataSite().getServerHomeLink() + link)).getResponseAsByte();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			HttpManager.releaseSite(site);
		}

		ServerResponse resp = new ServerResponse();

		if (imageBytes != null) {
			afterDownloadImage(true);
		} else {
			return new DownloadImageResult(DownloadImageResult.Status.ERROR, new byte[0], image.getContentType());
		}

		String imageName = image.getPath();
		if (ro.cst.tsearch.utils.FileUtils.existPath(imageName)) {
			imageBytes = ro.cst.tsearch.utils.FileUtils.readBinaryFile(imageName);
			return new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, image.getContentType());
		}

		resp.setImageResult(new DownloadImageResult(DownloadImageResult.Status.OK, imageBytes, image.getContentType()));

		if (!ro.cst.tsearch.utils.FileUtils.existPath(imageName)) {
			FileUtils.writeByteArrayToFile(resp.getImageResult().getImageContent(), image.getPath());
		}

		DownloadImageResult dres = resp.getImageResult();

		return dres;
	}

	protected static Map<String,Map<CountySpecificInfo,String>> parentSiteInfo = new Hashtable<String,Map<CountySpecificInfo,String>>();
	protected static enum CountySpecificInfo {
		INSTR_TYPE_SELECT,
	}
	protected String[] platTypeIndexes = new String[0];
	protected String[] easementTypeIndexes = new String[0];
	protected String[] restrictionTypeIndexes = new String[0];
	protected String[] bookPageTypeIndexes = new String[0];
	
	protected void initFields() {
		
//		downloadParentSiteData();
//		checkoutInputsForSubdivisionSearch();
		
		String countyId = getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY);
		County county;
		try {
			county = County.getCounty(Integer.parseInt(countyId));
			String countyName = county.getName();
			
			if (parentSiteInfo.containsKey(countyName)) {
				instr_type_select = parentSiteInfo.get(countyName).get(CountySpecificInfo.INSTR_TYPE_SELECT);
				
			}
			party_type_select 	= PARTY_TYPE_SELECT;
			search_type_select 	= SEARCH_TYPE_SELECT;
			index_type_select 	= INDEX_TYPE_SELECT;
			mortgage_op_select 	= MORTGAGE_OPERATOR_SELECT;
			transfer_op_select 	= TRANSFER_OPERATOR_SELECT;
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	static {
		loadParentSiteData();
	}
	
	public static void loadParentSiteData() {
		String folderPath = ServerConfig.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath + "] does not exist. Module Information not loaded!");
		}
		try {
			String xml = org.apache.commons.io.FileUtils.readFileToString(new File(folderPath + File.separator + TNGenericTitleSearcherSRC.class.getSimpleName() + ".xml"));
			Pattern countyPattern = Pattern.compile("(?ism)<county id=\"(.*?)\">(.*?)</county>");
			Matcher countyM = countyPattern.matcher(xml);
			while (countyM.find()) {
				String countyName = countyM.group(1);
				String controls = countyM.group(2);
				String instrumentTypeSelect = ro.cst.tsearch.utils.StringUtils.extractParameter(controls, "(?ism)(<select name=\"itype\".*?</select>)");			
				
				Map<CountySpecificInfo,String> info = new HashMap<CountySpecificInfo, String>();
				info.put(CountySpecificInfo.INSTR_TYPE_SELECT,instrumentTypeSelect);

	    		parentSiteInfo.put(countyName, info);
			}
		} catch (Exception e) {
			e.printStackTrace();	
		}
	}
	
	protected static String getTypeIndexes(String controls, String selectBox, String type, boolean tryAbbreviationAlso) {
		
		String values  = ro.cst.tsearch.utils.StringUtils.extractParameter(controls, "(?ism)<"+type+"Types>(.*?)</"+type+"Types>");
		String indexes = "";
		if(values.isEmpty()) {
			return ro.cst.tsearch.utils.StringUtils.extractParameter(selectBox, "(?im)<option.*?value=\"(.*?)\".*?>[^<]*?\\["+type.toUpperCase()+"\\]");
		}
		
		for(String value :  values.split(";")) {
			String index = "";
			value = value.trim();
			if(tryAbbreviationAlso) {
				index = ro.cst.tsearch.utils.StringUtils.extractParameter(selectBox, "(?im)<option.*?value=\"(.*?)\".*?>[^<]*?\\["+value+"\\]");
			}
			if(index.isEmpty()) {
				index = ro.cst.tsearch.utils.StringUtils.extractParameter(selectBox, "(?im)<option.*?value=\"(.*?)\".*?>\\s*[^<]*?"+value+"");
			}
			if(!index.isEmpty()) {
				indexes += index + ";";
			}
		}
		
		return indexes;
	}
	protected void downloadParentSiteData() {
		String html = "";
		ro.cst.tsearch.connection.http2.TNGenericTitleSearcherSRC site = (ro.cst.tsearch.connection.http2.TNGenericTitleSearcherSRC)HttpManager.getSite(getCurrentServerName(), searchId);
//    	String server = dataSite.getLink();
    	
    	String folderPath = ServerConfig.getModuleDescriptionFolder(BaseServlet.REAL_PATH + "WEB-INF/classes/resource/module/comments/");
		File folder = new File(folderPath);
		if(!folder.exists() || !folder.isDirectory()) {
			throw new RuntimeException("The folder [" + folderPath + "] does not exist. Module Information not loaded!");
		}
		File f = new File(folderPath + File.separator + TNGenericTitleSearcherSRC.class.getSimpleName() + ".xml");
    	
    	String allParentSiteData = "<state id=\"TN\">";
    	try{
    			String link = ""; 
    			
    	        html = site.process(new HTTPRequest("http://www.titlesearcher.com/countySelect.php")).getResponseAsString();
    	        Parser parser = Parser.createParser(html, null);
    	    	try {
    	    		NodeList nodeList = parser.parse(null);
    	    		NodeList alist = nodeList.extractAllNodesThatMatch(new TagNameFilter("a"), true);
    	    		if (alist != null){
    	    			for(int i =0; i < alist.size(); i++){
    	    				LinkTag linnk = (LinkTag) alist.elementAt(i);
    	    				if (linnk.getLink().contains("countySearchPage.php?cnum=")){
    	    					link = "http://www.titlesearcher.com/" + linnk.getLink();
    	    					html = site.process(new HTTPRequest(link)).getResponseAsString();
    	        				html = site.process(new HTTPRequest("http://www.titlesearcher.com/nameSearch.php")).getResponseAsString();
    	        				String instrumentTypeSelect = ro.cst.tsearch.utils.StringUtils.extractParameter(html, "(?ism)(<select name=\\\"itype\\\".*?</select>)");
    	        				
    	        				System.out.println(instrumentTypeSelect);
    	        	    		
    	        	    		allParentSiteData += "<county id=\"" + org.apache.commons.lang.StringUtils.capitalize(linnk.getLinkText().toLowerCase()) + "\">\n";
    	        	    		allParentSiteData += instrumentTypeSelect.replaceAll("(?ism)\\s*label=\\\"[^\\\"]*\\\"\\s*", " ").replaceAll("(?is)\\s*(<option[^>]*>)", "\n$1");
    	        	    		allParentSiteData += "</county>\n";
    	    				}
    	    			}
    	    		}
    	    	} catch (ParserException e) {
    	    		e.printStackTrace();
    	    	}


	    		
    		allParentSiteData += "</state>\n";
    		allParentSiteData = allParentSiteData.replaceAll("</option>\n\n", "</option>\n").replaceAll("\\s*</select>", "\n</select>");
    		org.apache.commons.io.FileUtils.writeStringToFile(f, allParentSiteData);
    	} catch(Exception e){
    		e.printStackTrace();
    	} finally {
    		HttpManager.releaseSite(site);
    	}  
	}
	
	protected void checkoutInputsForSubdivisionSearch() {
		String html = "";
		ro.cst.tsearch.connection.http2.TNGenericTitleSearcherSRC site = (ro.cst.tsearch.connection.http2.TNGenericTitleSearcherSRC)HttpManager.getSite(getCurrentServerName(), searchId);
    	
    	String allParentSiteData = "<state id=\"TN\">";
    	try{
    			String link = ""; 
    			
    	        html = site.process(new HTTPRequest("http://www.titlesearcher.com/countySelect.php")).getResponseAsString();
    	        Parser parser = Parser.createParser(html, null);
    	    	try {
    	    		NodeList nodeList = parser.parse(null);
    	    		NodeList alist = nodeList.extractAllNodesThatMatch(new TagNameFilter("a"), true);
    	    		if (alist != null){
    	    			for(int i =0; i < alist.size(); i++){
    	    				LinkTag linnk = (LinkTag) alist.elementAt(i);
    	    				StringBuffer inputs = new StringBuffer();
    	    				if (linnk.getLink().contains("countySearchPage.php?cnum=")){
    	    					link = "http://www.titlesearcher.com/" + linnk.getLink();
    	    					html = site.process(new HTTPRequest(link)).getResponseAsString();
    	        				html = site.process(new HTTPRequest("http://www.titlesearcher.com/subdivisionSearch.php")).getResponseAsString();

    	        				html = html.replaceAll("(?is)<script[^>]*>.*?</script>", "");
    	        				HtmlParser3 prs = new HtmlParser3(html);
    	        				NodeList nl = prs.getNodeList().extractAllNodesThatMatch(new TagNameFilter("form"), true).extractAllNodesThatMatch(new HasAttributeFilter("action", "subdivisionSearch.php"), true);
    	        				if (nl != null && nl.size() > 0){
    	        					for (int j = 0; j < nl.size(); j++){
    	        						String tabel = nl.elementAt(j).toHtml();
    	        						if (tabel.toLowerCase().contains("subdivision")){
    	        							NodeList inplist =  nl.elementAt(j).getChildren().extractAllNodesThatMatch(new TagNameFilter("input"), true);
    	        							
    	        							if (inplist != null && inplist.size() > 0){
    	        								for (int k = 0; k < inplist.size(); k++){
    	        									InputTag inp = (InputTag) inplist.elementAt(k);
    	        									inputs.append(inp.getAttribute("name")).append(", ");
    	        								}
    	        							}
    	        						}
    	        					}
    	        				}
    	        	    		
    	        	    		allParentSiteData += "<county id=\"" + org.apache.commons.lang.StringUtils.capitalize(linnk.getLinkText().toLowerCase()) + "\">\n";
    	        	    		allParentSiteData += inputs;
    	        	    		allParentSiteData += "</county>\n";
    	    				}
    	    			}
    	    		}
    	    	} catch (ParserException e) {
    	    		e.printStackTrace();
    	    	}


	    		
    		allParentSiteData += "</state>\n";
    		System.out.println(allParentSiteData);
    	} catch(Exception e){
    		e.printStackTrace();
    	} finally {
    		HttpManager.releaseSite(site);
    	}  
	}
}
