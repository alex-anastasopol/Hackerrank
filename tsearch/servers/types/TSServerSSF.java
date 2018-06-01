package ro.cst.tsearch.servers.types;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.databinding.types.Token;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.PrototypicalNodeFactory;
import org.htmlparser.filters.TagNameFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.name.RomanNumber;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.threads.ASMaster;
import ro.cst.tsearch.threads.ASThread;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.XmlUtils;
import ro.cst.tsearch.utils.tags.StatementTag;
import ro.cst.tsearch.utils.tags.TextTag;
import ro.cst.tsearch.utils.tags.TypeTag;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.PriorFileDocument;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.SSFPriorFileDocument;
import com.stewart.ats.base.misc.SelectableStatement;
import com.stewart.ats.base.property.PinI;
import com.stewart.ats.base.property.Property;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewartworkplace.starters.ssf.services.connection.DocAdminConn;
import com.stewartworkplace.starters.ssf.services.docadmin.AddressType;
import com.stewartworkplace.starters.ssf.services.docadmin.ApnType;
import com.stewartworkplace.starters.ssf.services.docadmin.BlockType;
import com.stewartworkplace.starters.ssf.services.docadmin.BookOrPageOrEmptyType;
import com.stewartworkplace.starters.ssf.services.docadmin.BookPageType;
import com.stewartworkplace.starters.ssf.services.docadmin.DocumentIndexType;
import com.stewartworkplace.starters.ssf.services.docadmin.DocumentInfoResultType;
import com.stewartworkplace.starters.ssf.services.docadmin.DocumentInfoType;
import com.stewartworkplace.starters.ssf.services.docadmin.ErrorContainerType;
import com.stewartworkplace.starters.ssf.services.docadmin.ErrorType;
import com.stewartworkplace.starters.ssf.services.docadmin.FindStartersResponse;
import com.stewartworkplace.starters.ssf.services.docadmin.FindStartersResultType;
import com.stewartworkplace.starters.ssf.services.docadmin.FullStateType;
import com.stewartworkplace.starters.ssf.services.docadmin.GrantorGranteeType;
import com.stewartworkplace.starters.ssf.services.docadmin.LabelsPropertyType;
import com.stewartworkplace.starters.ssf.services.docadmin.LegalType;
import com.stewartworkplace.starters.ssf.services.docadmin.LetterAndNumberTokenType;
import com.stewartworkplace.starters.ssf.services.docadmin.LotFreeFormType;
import com.stewartworkplace.starters.ssf.services.docadmin.LotType;
import com.stewartworkplace.starters.ssf.services.docadmin.MatchModeType;
import com.stewartworkplace.starters.ssf.services.docadmin.NameType;
import com.stewartworkplace.starters.ssf.services.docadmin.NameTypeChoice_type0;
import com.stewartworkplace.starters.ssf.services.docadmin.PartyType;
import com.stewartworkplace.starters.ssf.services.docadmin.PinType;
import com.stewartworkplace.starters.ssf.services.docadmin.Properties_type0;
import com.stewartworkplace.starters.ssf.services.docadmin.PropertyType;
import com.stewartworkplace.starters.ssf.services.docadmin.SearchCriteriaType;
import com.stewartworkplace.starters.ssf.services.docadmin.StarterType;
import com.stewartworkplace.starters.ssf.services.docadmin.StateAbbreviationType;
import com.stewartworkplace.starters.ssf.services.docadmin.StateType;
import com.stewartworkplace.starters.ssf.services.docadmin.SubdivisionType;
import com.stewartworkplace.starters.ssf.services.docadmin.SuccesType;
import com.stewartworkplace.starters.ssf.services.docadmin.TextStratersType;
import com.stewartworkplace.starters.ssf.services.docadmin.TownshipType;
import com.stewartworkplace.starters.ssf.services.docadmin.TxtStarterAndIndexType;
import com.stewartworkplace.starters.ssf.services.docadmin.ZipType;
import com.stewartworkplace.starters.ssf.services.util.ConvertUtil;

/**
 * @author cristi stochina
 */
public abstract class TSServerSSF extends TSServer implements TSServerROLikeI {
	
	public static final long serialVersionUID = -23423322;
	
	transient private DocAdminConn conn;

	protected static String PREFIX_FINAL_LINK = "SSF___";
	
	private static Pattern finalDocLinkPattern = Pattern.compile(PREFIX_FINAL_LINK + "([^&]+)&?");


	/* --------- start abstracts methods ----------------- */
	protected abstract void ParseResponse(String moduleIdx,ServerResponse Response, int viParseID)throws ServerResponseException;
	
	protected abstract ServerResponse performRequest(String page,int methodType, String action, int parserId, String imageLink, String vbRequest, Map<String, Object> extraParams)throws ServerResponseException;
	/* ------------ end abstracts methods ------------------- */

	/**
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 * @param searchId
	 */
	public TSServerSSF(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	/**
	 * @param searchId
	 */
	public TSServerSSF(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	/**
	 * Remove formatting from html row displayed in intermediate results
	 * @param html
	 * @return
	 */
	protected static String removeFormatting(String html) {
		int istart = html.indexOf("<td");
		if (istart == -1) {
			return html;
		}
		istart = html.indexOf("<td", istart + 1);
		if (istart == -1) {
			return html;
		}
		istart = html.indexOf("<td", istart + 1);
		if (istart == -1) {
			return html;
		}
		istart = html.indexOf(">", istart);
		if (istart == -1) {
			return html;
		}
		istart += 1;
		int iend = html.lastIndexOf("</td");
		if (iend == -1) {
			return html;
		}
		if (istart > iend) {
			return html;
		}
		return html.substring(istart, iend);
	}
 
	/**
	 * get file name from link
	 */
	protected String getFileNameFromLink(String link) {
		// try the normal link pattern
		Matcher ssfLinkMatcher = finalDocLinkPattern.matcher(link);
		serverTypeDirectoryOverride = null;
		
		if (ssfLinkMatcher.find()) {
			return ssfLinkMatcher.group(1) + ".html";
		}
		
		throw new RuntimeException("Unknown Link Type: " + link);
	}

	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		return searchBy(module, sd);
	}

	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = super.getDefaultServerInfo();
		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}

	private FindStartersResponse  performSearch(Map<String, String> params, int moduleIDX) throws RemoteException{

		CurrentInstance ci = InstanceManager.getManager().getCurrentInstance(mSearch.getID());
		County county = ci.getCurrentCounty();
		State state = ci.getCurrentState();
		int stateFips = state.getStateFips();
		int countyFips = county.getCountyFips();
		String countyName = county.getName(); 
		String stateName = state.getName();
		String stateAbrev = state.getStateAbv().toUpperCase();
		
		SearchCriteriaType criteria = new SearchCriteriaType();
		
		String matchMode = params.get("matchMode");
		
		if(!StringUtils.isBlank(matchMode)){
			criteria.setMatchMode(MatchModeType.Factory.fromValue(getValidToken(matchMode)));
		}
		
		StateType stateT = null;
		try{stateT = ConvertUtil.toStateType(stateName, stateAbrev);}catch(RuntimeException e){};
		String agentId = params.get("agentId");
		
		//unit=234, direction=dir, unitPrefix=apt, name=MIRABELLE, state=tx, number=2806, suffix=suffix, zipp=77494, city=katy
		if(moduleIDX==TSServerInfo.ADDRESS_MODULE_IDX){
			ZipType zip = null;
			
			LetterAndNumberTokenType from = null;
			LetterAndNumberTokenType to = null;
			
			String unitPrefix = params.get("unitPrefix");
			String unit = params.get("unit");
			String city = params.get("city");
			String streetName = params.get("name");
			
			
			try{from = ConvertUtil.toLetterAndNumberTokenType(params.get("number"));}catch(RuntimeException e){};
			try{to = ConvertUtil.toLetterAndNumberTokenType(params.get("toNumber"));}catch(RuntimeException e){};
			try{zip = ConvertUtil.toZipType(params.get("zipp"));}catch(RuntimeException e){};
			
			AddressType add = new AddressType();
			if(streetName!=null){
				add.setStreetName( getValidToken(streetName));
			}
			if(city!=null){
				add.setCity(getValidToken(city));
			}
			if(unitPrefix!=null){
				add.setIdentifierType(getValidToken(unitPrefix));
			}
			if(unit!=null){
				add.setIdentifierNumber(getValidToken(unit));
			}
			if(from!=null){
				add.setNumber(from);
			}
			if(to!=null){
				add.setThruNumber(to);
			}
			if(stateT!=null){
				add.setState(stateT);
			}
			if(countyName!=null){
				add.setCounty(getValidToken(countyName));
			}
			if(zip!=null){
				add.setZip(zip);
			}
			
			String fileType = params.get("fileType");
			
			if("ALL".equalsIgnoreCase(fileType)){
				fileType = "";
			}
			if(StringUtils.isNotBlank(fileType)){
				criteria.setFileType(getValidToken(fileType));
			}
			
			criteria.setDescription(new Token("Address Search"));
			criteria.addAddress(add);
			if(StringUtils.isNotBlank(agentId)){
				criteria.setAgentId(getValidToken(agentId));
			}
			return getConn().searchByCriteria(criteria, stateFips, countyFips);
		}else if( moduleIDX==TSServerInfo.PARCEL_ID_MODULE_IDX ){

			PinType pin = createPin(countyName, stateT, params.get("pid"),"parcelId");
			criteria.addPin(pin);
			
			pin = createPin(countyName, stateT, params.get("pid"),"apn");
			criteria.addPin(pin);
			
			pin = createPin(countyName, stateT, params.get("pid"),"folio");
			criteria.addPin(pin);
			
			pin = createPin(countyName, stateT, params.get("pid"),"propId");
			criteria.addPin(pin);
			
			pin = createPin(countyName, stateT, params.get("pid"),"geo");
			criteria.addPin(pin);
			
			pin = createPin(countyName, stateT, params.get("pid"),"parentId");
			criteria.addPin(pin);
			
			String fileType = params.get("fileType");
			
			if("ALL".equalsIgnoreCase(fileType)){
				fileType = "";
			}
			if(StringUtils.isNotBlank(fileType)){
				criteria.setFileType(getValidToken(fileType));
			}
			
			criteria.setDescription(new Token("PID Search"));
			
			if(StringUtils.isNotBlank(agentId)){
				criteria.setAgentId(getValidToken(agentId));
			}
			if(StringUtils.isNotBlank(agentId)){
				criteria.setAgentId(getValidToken(agentId));
			}
			if(StringUtils.isNotBlank(agentId)){
				criteria.setAgentId(getValidToken(agentId));
			}
			return getConn().searchByCriteria(criteria, stateFips, countyFips);
		}else if( moduleIDX==TSServerInfo.PROP_NO_IDX ){	//Search By FileId
			String fileId = params.get("fileId");
			if(StringUtils.isNotBlank(fileId)){
				criteria.setFileId(getValidToken(fileId));
				criteria.setDescription(new Token("File ID Search"));
				if(StringUtils.isNotBlank(agentId)){
					criteria.setAgentId(getValidToken(agentId));
				}
				String fileType = params.get("fileType");
				
				if("ALL".equalsIgnoreCase(fileType)){
					fileType = "";
				}
				if(StringUtils.isNotBlank(fileType)){
					criteria.setFileType(getValidToken(fileType));
				}
				return getConn().searchByCriteria(criteria, stateFips, countyFips);
			}
		}else if( moduleIDX==53){	//Search by AgentId or FileType
			String fileType = params.get("fileType");
			
			if("ALL".equalsIgnoreCase(fileType)){
				fileType = "";
			}

			criteria.setDescription(new Token("File ID Search"));
			if(StringUtils.isNotBlank(fileType)){
				criteria.setFileType(getValidToken(fileType));
			}
			if(StringUtils.isNotBlank(agentId)){
				criteria.setAgentId(getValidToken(agentId));
			}
			return getConn().searchByCriteria(criteria, stateFips, countyFips);
			
		}else if( moduleIDX==TSServerInfo.SUBDIVISION_MODULE_IDX ){
			SubdivisionType subdiv = new SubdivisionType();
			
			String lot		= params.get("lot");
			String block	= params.get("block");
			String platBook	= params.get("platBook");
			String platPage	= params.get("platPage");
			String platDocNo= params.get("platDocNo");
			String name		= params.get("name");
			
			LotFreeFormType lotFree = null;
			LotFreeFormType blockFree = null;
			LetterAndNumberTokenType platBookL = null;
			LetterAndNumberTokenType platPageL = null;
			ApnType platInstrA = null;
			BlockType blockT = null;
			LotType lotT = null;
			
			try{lotFree = ConvertUtil.toLotFreeFormType(lot);}catch(RuntimeException e){};
			try{blockFree = ConvertUtil.toLotFreeFormType(block);}catch(RuntimeException e){};
			try{platBookL = ConvertUtil.toLetterAndNumberTokenType(platBook);}catch(RuntimeException e){};
			try{platPageL = ConvertUtil.toLetterAndNumberTokenType(platPage);}catch(RuntimeException e){};
			try{platInstrA = ConvertUtil.toApnType(platDocNo);}catch(RuntimeException e){};
			try{lotT = ConvertUtil.toLotType(lot);}catch(RuntimeException e){};
			try{
				blockT = new BlockType();
				blockT.setValue(ConvertUtil.toLetterAndNumberTokenType(block));
				if(lotT!=null){
					blockT.addLot(lotT);
				}
			}catch(RuntimeException e){};
			
			if(countyName!=null){
				subdiv.setCounty(getValidToken(countyName));
			}
			if(stateT!=null){
				subdiv.setState(stateT);
			}
			if(lotFree!=null){
				subdiv.setLotFreeForm(lotFree);
			}
			if(platBookL!=null){
				subdiv.setPlatBook(platBookL);
			}
			if(platPageL!=null){
				subdiv.setPlatPage(platPageL);
			}
			if(platInstrA!=null){
				subdiv.setPlatInstrument(platInstrA);
			}
			if(name!=null){
				subdiv.setName(getValidToken(name));
			}
			if(		(blockT!=null&&blockT.getValue()!=null)
				  ||(blockT.getLot()!=null&&blockT.getLot().length>0)){
				subdiv.addBlock(blockT);
			}
			if(blockFree!=null){
				subdiv.setBlockFreeForm(blockFree);
			}
			
			if(subdiv.getName()==null){
				subdiv.setName(new Token(""));
			}
			
			String fileType = params.get("fileType");
			
			if("ALL".equalsIgnoreCase(fileType)){
				fileType = "";
			}
			if(StringUtils.isNotBlank(fileType)){
				criteria.setFileType(getValidToken(fileType));
			}
			
			criteria.addSubdivision(subdiv);
			criteria.setDescription(new Token("Subdivision Search"));
			if(StringUtils.isNotBlank(agentId)){
				criteria.setAgentId(getValidToken(agentId));
			}
			return getConn().searchByCriteria(criteria, stateFips, countyFips);
		}else if( moduleIDX==TSServerInfo.ARB_MODULE_IDX ){		//Sectional and Abstract Search
			TownshipType townshipType = new TownshipType();
			
			String sec = params.get("section");
			String tw = params.get("township");
			String rg = params.get("range");
			String qo = params.get("quarterOrder");
			String qv = params.get("quarterValue");
			String abs = params.get("abs");
			
			BookOrPageOrEmptyType    secT  = null;
			LetterAndNumberTokenType twT = null;
			LetterAndNumberTokenType rgT = null;
			LetterAndNumberTokenType qoT = null;
			LetterAndNumberTokenType qvT = null;
			
			try{secT = ConvertUtil.toBookOrPageOrEmptyType(sec);}catch(Exception e){}
			try{twT = ConvertUtil.toLetterAndNumberTokenType(tw);}catch(Exception e){}
			try{rgT = ConvertUtil.toLetterAndNumberTokenType(rg);}catch(Exception e){}
			try{qoT = ConvertUtil.toLetterAndNumberTokenType(qo);}catch(Exception e){}
			try{qvT = ConvertUtil.toLetterAndNumberTokenType(qv);}catch(Exception e){}
			
			if(secT!=null){
				townshipType.setSection(secT);
			}
			if(twT!=null){
				townshipType.setTownship(twT);
			}
			if(rgT!=null){
				townshipType.setRange(rgT);
			}
			if(qoT!=null){
				townshipType.setQuarterOrder(qoT);
			}
			if(qvT!=null){
				townshipType.setQuarterValue(qvT);
			}
			if(!StringUtils.isEmpty(abs)){
				townshipType.setArb(getValidToken(abs));
			}
			if(!StringUtils.isBlank(sec)){
				townshipType.setSectionFreeForm(getValidToken(sec));
			}
			
			String fileType = params.get("fileType");
			
			if("ALL".equalsIgnoreCase(fileType)){
				fileType = "";
			}
			if(StringUtils.isNotBlank(fileType)){
				criteria.setFileType(getValidToken(fileType));
			}
			
			criteria.addTownship(townshipType);
			criteria.setDescription(new Token("Sectional Search"));
			if(StringUtils.isNotBlank(agentId)){
				criteria.setAgentId(getValidToken(agentId));
			}
			return getConn().searchByCriteria(criteria, stateFips, countyFips);
		}
		
		return null;
	}

	protected static Token getValidToken(String input) {
		return new Token(StringUtils.normalizeSpace(input));
	}
	
	private static PinType createPin(String countyName, StateType stateT, String pin1, final String type){
		PinType pin = new PinType();
		ApnType apn = null;
		
		if(countyName!=null){
			pin.setCounty(getValidToken(countyName));
		}
		if(stateT!=null){
			pin.setState(stateT);
		}
		
		try{apn = ConvertUtil.toApnType(pin1);}catch(RuntimeException e){};
		if(apn!=null){
			if("parcelId".equalsIgnoreCase(type)){
				pin.setParcelId(apn);
			}else if("apn".equalsIgnoreCase(type)){
				pin.setApn(apn);
			}else if("folio".equalsIgnoreCase(type)){
				pin.setFolio(apn);
			}else if("propId".equalsIgnoreCase(type)){
				pin.setPropertyId(apn);
			}else if("geo".equalsIgnoreCase(type)){
				pin.setGeoNumber(apn);
			}else if("parentId".equalsIgnoreCase(type)){
				pin.setParentParcelId(apn);
			}else{
				pin.setOtherParcelId(apn);
			}
		}else{
			throw new RuntimeException("Please provide a corect PID");
		}
		
		return pin;
	}
	
	protected ServerResponse searchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		global.removeAllInMemoryDocs();
		global.clearClickedDocuments();
		
		logSearchBy(module);
		
		int moduleIDX = module.getModuleIdx();
		int parserID = module.getParserID();
		
		Map<String, String> params = getNonEmptyParams( module, null );
		
		if (moduleIDX==TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX) {	//Image Search
			return processSsfImageResponse(params, moduleIDX, parserID, true);
		}
		
		FindStartersResponse response;
		try {
			response = performSearch( params, module.getModuleIdx() );
		} catch (Exception e) {
			e.printStackTrace();
			return ServerResponse.createErrorResponse("Eception while calling SSF Web service !" +"\n Exception: "+e.getMessage()); 
		}
		
		return processSsfResponse(response, moduleIDX, parserID);
	}
	
	protected ServerResponse processSsfResponse(FindStartersResponse ssfResponse, int moduleIDX , int parserID) throws ServerResponseException {
		return processSsfResponse(ssfResponse,moduleIDX,parserID,true);
	}
	
	protected ServerResponse processSsfResponse(FindStartersResponse response, int moduleIDX , int parserID, boolean log) throws ServerResponseException {
		
		if(response==null){
			logInSearchLogger("<font color=\"red\">SSF returned empty response ! </font>", searchId,log);
			return ServerResponse.createWarningResponse("SSF returned empty response !");
		}
		
		FindStartersResultType result = response.getFindStartersReturn();
		if(result==null){
			logInSearchLogger("<font color=\"red\">SSF returned empty response ! </font>", searchId,log);
			return ServerResponse.createWarningResponse("SSF returned empty response !");
		}
		
		if(SuccesType.SUCCESS != result.getStatus()){
			StringBuilder errBuilder = new StringBuilder();
			errBuilder.append("<font color=\"red\">");
			//errBuilder.append("Search operation return status: "+result.getStatus());
			//errBuilder.append("<br>");
			ErrorContainerType errContainer = null;
			if((errContainer = result.getErrors())!=null){
				ErrorType errors[] = null;
				if((errors = errContainer.getError())!=null){
					for(ErrorType er:errors){
						errBuilder.append(er.getText());
						errBuilder.append("<br>");
					}
				}
			}
			errBuilder.append("</font>");
			logInSearchLogger(errBuilder.toString(), searchId,log);
			return ServerResponse.createWarningResponse(errBuilder.toString());
		}
		
		// create & populate server response
		ServerResponse sr = new ServerResponse();
		ArrayList<ParsedResponse> parsedRowsList = new ArrayList<ParsedResponse>();
		String html = parseAndBuildHTML(result, parsedRowsList);
		
		Vector<ParsedResponse> parsedRows = new Vector<ParsedResponse>(parsedRowsList);
		sr.getParsedResponse().setResultRows(parsedRows);
		sr.setResult(html);
		
		String saction = moduleIDX + "";

		// sAction(link-ul) for SSF does not make any sense, but we can use it for transporting the module number
		solveHtmlResponse(saction, parserID, "SearchBy", sr, sr.getResult());
		
		// log number of results found
		logInSearchLogger("Found <span class='number'>"+ sr.getParsedResponse().getResultsCount()+ "</span> <span class='rtype'>intermediate</span> results.<br/>",searchId,log);

		return sr;
	}
	
	private void logInSearchLogger(String message, long searchId, boolean doLog) {
		if(doLog) {
			SearchLogger.info(message, searchId);
		}
	}
	
	protected ServerResponse processSsfImageResponse(Map<String, String> params, int moduleIDX , int parserID, boolean log) throws ServerResponseException {
		
		String book = params.get("book");
		String page = params.get("page");
		String instno = params.get("instno");
		String yearString = params.get("year");
		int year = -1;
		try {
			year = Integer.parseInt(yearString);
		} catch (NumberFormatException nfe) {}
		String docType = params.get("docType");
		
		InstrumentI instr = new Instrument();
		instr.setBook(book!=null?book:"");
		instr.setPage(page!=null?page:"");
		instr.setInstno(instno!=null?instno:"");
		instr.setYear(year);
		instr.setDocType(docType!=null?docType:"");
		instr.setDocSubType(StringUtils.defaultString(params.get("subcategory")));
    	
    	RegisterDocument docR = new RegisterDocument( DocumentsManager.generateDocumentUniqueId(searchId, instr) );
		docR.setInstrument(instr);
		docR.setSearchType(SearchType.IM);
		docR.setUploaded(true);
		
		String pin = params.get("pin");
		if(StringUtils.isNotBlank(pin)) {
			PropertyI createEmptyProperty = Property.createEmptyProperty();
			createEmptyProperty.getPin().addPin(com.stewart.ats.base.property.PinI.PinType.PID, pin);
			docR.getProperties().add(createEmptyProperty);
		}
		
		String pageOffsetString = StringUtils.defaultIfBlank(params.get("pageOffset"), "0");
		String pageSizeString = StringUtils.defaultIfBlank(params.get("pageSize"), "100");
		
		int pageOffset = 0;
		try {
			pageOffset = Integer.parseInt(pageOffsetString);
		} catch (Exception e) {
		}
		int pageSize = 0;
		try {
			pageSize = Integer.parseInt(pageSizeString);
		} catch (Exception e) {
		}
		
    	
//		List <DocumentInfoType> documents = findImagesOnSsf(getSearch(), docR);
		
		List <DocumentInfoType> documents = new ArrayList<DocumentInfoType>();
		String innerMessage = "Invalid Response!";
		try {
			County county=InstanceManager.getManager().getCurrentInstance(getSearch().getID()).getCurrentCounty();
			
			DocumentInfoResultType results = getConn().getDocumentInfo(docR, 
					county.getState().getStateFips(), 
					county.getCountyFips(),
					StringUtils.defaultIfBlank(params.get("toYear"), null),
					pageOffset,
					pageSize
					);
			
			if(results!=null){
				
				if(SuccesType.SUCCESS == results.getStatus()){
					DocumentInfoType docsInfo[] = results.getDocInfo();
					if(docsInfo!=null && docsInfo.length>0){
						
						for (int i=0;i<docsInfo.length;i++) {
							org.apache.axis2.databinding.types.URI uri = docsInfo[i].getLink();
							String link = "";
							if (uri!=null) {
								link = uri.toString();
							}
							if (org.apache.commons.lang.StringUtils.isNotBlank(link)) {
								documents.add(docsInfo[i]);
							}
						}
						
						ServerResponse sr = new ServerResponse();
						ArrayList<ParsedResponse> parsedRowsList = new ArrayList<ParsedResponse>();
						String html = parseAndBuildImageHTML(params, documents, parsedRowsList);
						
						Vector<ParsedResponse> parsedRows = new Vector<ParsedResponse>(parsedRowsList);
						sr.getParsedResponse().setResultRows(parsedRows);
						
						
						String linkPrev = null;
						String linkNext = null;
						if(pageOffset > 0) {
							linkPrev = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX);
							for (String paramKey : params.keySet()) {
								if(paramKey.equals("pageOffset")) {
									linkPrev += "&" + paramKey + "=" + (pageOffset - 1);
								} else {
									linkPrev += "&" + paramKey + "=" + URLEncoder.encode(params.get(paramKey).trim(), "UTF-8"); 
								}
							}
							if(!params.containsKey("pageOffset")){
								linkPrev += "&pageOffset=" + (pageOffset - 1);
							}
							linkPrev = "<a href=\"" + linkPrev + "\">Previous</a>";
						}
						if(pageSize == docsInfo.length) {
							linkNext = createPartialLink(TSConnectionURL.idPOST, TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX);
							for (String paramKey : params.keySet()) {
								if(paramKey.equals("pageOffset")) {
									linkNext += "&" + paramKey + "=" + (pageOffset + 1);
								} else {
									linkNext += "&" + paramKey + "=" + URLEncoder.encode(params.get(paramKey).trim(), "UTF-8"); 
								}
							}
							if(!params.containsKey("pageOffset")){
								linkNext += "&pageOffset=" + (pageOffset + 1);
							}
							linkNext = "<a href=\"" + linkNext + "\">Next</a>";
						}
						
						sr.getParsedResponse().setFooter( (linkPrev==null?"":linkPrev + "&nbsp;&nbsp;") + (linkNext==null?"":linkNext) );
						sr.setResult(html);
						
						String saction = moduleIDX + "";

						// sAction(link-ul) for SSF does not make any sense, but we can use it for transporting the module number
						solveHtmlResponse(saction, parserID, "SearchBy", sr, sr.getResult());
						
						// log number of results found
						logInSearchLogger("Found <span class='number'>"+ sr.getParsedResponse().getResultsCount()+ "</span> <span class='rtype'>intermediate</span> results.<br/>",searchId,log);
						
						return sr;
						
					} else {
						innerMessage = "No data found!";
					}
					
				} else {
					ErrorContainerType erorContainer = results.getErrors();
					if(erorContainer!=null && erorContainer.getError()!=null && erorContainer.getError().length>0){
						ErrorType []allerr = erorContainer.getError();
						
						StringBuilder sb = new StringBuilder();
						for(int i=0;i<allerr.length;i++){
							if(!"UNKNOWN".equals(allerr[i].getCode().toString())) {
								if(sb.length() == 0 ) {
									sb.append(allerr[i].getCode().toString());
								} else {
									sb.append(", ").append(allerr[i].getCode().toString());
								}
							}
							
						}
						
						if(sb.length() == 0) {
							sb.append("Invalid Response!");
						} else {
							sb.insert(0, "Invalid Response! (");
							sb.append(")");
						}
						
						innerMessage = sb.toString();
						
					}
				}
			}
		} catch (Exception e) {
			logger.error("Failed to get image results from SSF for document "+ instr.prettyPrint(), e);
		}
		
		logInSearchLogger("<font color=\"red\">" + innerMessage + "</font>", searchId, log);
		ServerResponse sr = new ServerResponse();
		sr.getParsedResponse().setError(innerMessage);
		sr.getParsedResponse().setAttribute(ParsedResponse.SHOW_ONLY_ERROR_MESSAGE,"true");
		return sr;
		
	}
	
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<Object> l = new ArrayList<Object>();
		serverInfo.setModulesForAutoSearch(l);
	}
	
	public String createPartialLink(int iActionType, int dispatcher) {
		StringBuilder build = new StringBuilder(msRequestSolverName);
		build.append("?");
		build.append(msServerID);
		build.append("&");
		build.append(RequestParams.SEARCH_ID);
		build.append("=");
		build.append(mSearch.getSearchID());
		build.append("&");
		build.append("dispatcher=");
		build.append(dispatcher);
		return build.toString();
	}

	private static StarterType parseCommMultiLineXmlFile(String fileContent){
		try {
			XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new ByteArrayInputStream(fileContent.getBytes("UTF-8")));
			while (!reader.isStartElement() && !reader.isEndElement())
				reader.next();
			reader.getName();
			reader.next();
			while (!reader.isStartElement() && !reader.isEndElement()){
				reader.next();
			}
			reader.getName();
			return StarterType.Factory.parse(reader);
		}catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public String parseAndBuildHTML(FindStartersResultType result, List<ParsedResponse> list){
		
		if(result==null){
			return "";
		}
		
		TextStratersType allStarters = result.getTxtStraters();
		
		if(allStarters==null){
			return "";
		}
		
		TxtStarterAndIndexType[] starters = allStarters.getTxtStarters();
		if(starters==null || starters.length==0){
			return "";
		}
		
		Search search = getSearch();
		
		StringBuilder fullHtml = new StringBuilder();
		
		for(TxtStarterAndIndexType starterFileContent:starters){
			StarterType starter = parseCommMultiLineXmlFile(starterFileContent.getTxtStarter());
			try{
				ParsedResponse item = new ParsedResponse();
				
				String starterFileContentStr = starterFileContent.getTxtStarter();
				
				String originalStr = starterFileContentStr;
				starterFileContentStr = starterFileContentStr.replaceAll("(?i)<link><url></url><name></name></link> ,?","");
				starterFileContentStr = starterFileContentStr.replaceAll("(?i)<url>([^>]+)</url>\\s*</link>","<url>$1</url> <name>Link</name></link>");
				starterFileContentStr = starterFileContentStr.replaceAll("(?i)<url>([^>]+)</url>\\s*<name/>","<url>$1</url> <name>Link</name>");
				starterFileContentStr = starterFileContentStr.replaceAll("(?i)<url>([^>]+)</url>\\s*<name>(//s*)</name>","<url>$1</url> <name>Link</name>");
				starterFileContentStr = starterFileContentStr.replaceAll("(?i)<url>([^>]+)</url>\\s*<name>([^>]+)</name>","<a href='$1'>$2</a>");
				starterFileContentStr = starterFileContentStr.replaceAll(">[ \t\r\n]+<","><");
				//content = content.replaceAll("(?i)\\s*<instrument>.*?</instrument>\\s*","");
				starterFileContentStr = starterFileContentStr.replaceAll("(?i)</link>","");
				starterFileContentStr = starterFileContentStr.replaceAll("(?i)<link>","");
				
				starterFileContentStr = starterFileContentStr.replaceAll("(?is)<br>\\s+<br>", "<br><br>");
				
				starterFileContent.setTxtStarter(starterFileContentStr);
				
				item.setAttribute(ParsedResponse.SSF_CONTENT, starterFileContent);
				//FileUtils.write(new File("D:\\delete1.txt"), starterFileContentStr);
				
				String html = buildHtml(null, originalStr);
				
				fullHtml.append( html  );
				
				item.setResponse(html);
				
				ResultMap resultMap = new ResultMap();
				parseAndFillResultMap(starter,resultMap,searchId);
				
				Bridge bridge = new Bridge(item, resultMap, searchId);
				SSFPriorFileDocument doc = (SSFPriorFileDocument) bridge.importData();
				
				ASThread thread = ASMaster.getSearch(search);
		        if (thread != null 
		        		&& thread.isBackgroundSearch() 
		        		&& DocumentTypes.PRIORFILE_BASE_FILE.equals(doc.getDocSubType()) 
		        		&& search.getAgent().isAutoExportTsr()) {
		        	parseStatementsAndSelect(doc, starterFileContent.getTxtStarter());
		        	doc.setManualChecked(true);
		        	doc.setChecked(true);
		        }
				
				//parseStatements(doc, starterFileContent.getTxtStarter(), null);
				doc.setServerDocType((String)resultMap.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName()));
		    	item.setDocument(doc);
				
				list.add(item);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		return fullHtml.toString();
	}
	
	public static void parseStatements(SSFPriorFileDocument doc, String starterText, HashMap<String, HashMap<String, Boolean>> statementsMap) {
		try {
			HashMap<String, Boolean> reqMap = statementsMap != null ? statementsMap.get(PriorFileDocument.REQUIREMENTS) : null;
			HashMap<String, Boolean> legalMap = statementsMap != null ? statementsMap.get(PriorFileDocument.LEGAL_DESC) : null;
			HashMap<String, Boolean> excMap = statementsMap != null ? statementsMap.get(PriorFileDocument.EXCEPTIONS) : null;
			
			if(doc.getRequirements() != null) { doc.getRequirements().clear(); }
			if(doc.getExceptionsList() != null) { doc.getExceptionsList().clear(); }
			if(doc.getLegalDescriptions() != null) { doc.getLegalDescriptions().clear(); }
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(starterText, null);

			PrototypicalNodeFactory nodeFactory = new PrototypicalNodeFactory();
			nodeFactory.registerTag(new TextTag());
			nodeFactory.registerTag(new TypeTag());
			nodeFactory.registerTag(new StatementTag());
			htmlParser.setNodeFactory(nodeFactory);

			org.htmlparser.util.NodeList mainList = htmlParser.parse(null);

			org.htmlparser.util.NodeList extractAllNodesThatMatch = mainList
					.extractAllNodesThatMatch(new TagNameFilter("statement"), true);
			for (int i = 0; i < extractAllNodesThatMatch.size(); i++) {
				org.htmlparser.util.NodeList typeNodeList = extractAllNodesThatMatch.elementAt(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("type"));
				org.htmlparser.util.NodeList textNodeList = extractAllNodesThatMatch.elementAt(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("text"));
				if(typeNodeList.size() > 0 && textNodeList.size() > 0) {
					String type = typeNodeList.elementAt(0).toPlainTextString();
					String text = textNodeList.elementAt(0).getChildren().toHtml().trim();
					if(!type.isEmpty() && !text.isEmpty()) {
						
						if(type.contains("Legal Description")) {
							doc.addLegalDescriptions(SelectableStatement.splitTextIntoStatements(StringEscapeUtils.escapeXml(StringEscapeUtils.unescapeXml(text)), legalMap));
						} else if(type.contains("Requirements")) {
							doc.addRequirements(SelectableStatement.splitTextIntoStatements(StringEscapeUtils.escapeXml(StringEscapeUtils.unescapeXml(text)), reqMap));
						} else if(type.contains("Exceptions")) {
							doc.addExceptionsList(SelectableStatement.splitTextIntoStatements(StringEscapeUtils.escapeXml(StringEscapeUtils.unescapeXml(text)), excMap));
						}
					}
				}
			}

		} catch (Exception e) {
			logger.error("Error while parsing statements", e);
		}
		
		
		/*
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse(new ByteArrayInputStream(starterText.getBytes("UTF-8")));
			Element docEle = dom.getDocumentElement();
			NodeList nl = docEle.getElementsByTagName("statement");
			
			
			
			
			
			if(nl != null && nl.getLength() > 0) {
				if(doc.getRequirements() != null) { doc.getRequirements().clear(); }
				if(doc.getExceptionsList() != null) { doc.getExceptionsList().clear(); }
				if(doc.getLegalDescriptions() != null) { doc.getLegalDescriptions().clear(); }
				
				for(int i = 0 ; i < nl.getLength();i++) {
					Element el = (Element)nl.item(i);
					String type = "";
					String text = "";
					
					NodeList _nl = el.getElementsByTagName("type");
					if(_nl != null && _nl.getLength() > 0) {
						type = _nl.item(0).getFirstChild().getNodeValue();
					}

					_nl = el.getElementsByTagName("text");
					if(_nl != null && _nl.getLength() > 0) {
						text = _nl.item(0).getFirstChild().getNodeValue();
					}
					
					if(!type.equals("") && !text.equals("")) {
						if(type.contains("Legal Description")) {
							doc.addLegalDescriptions(SelectableStatement.splitTextIntoStatements(text, true));
						} else if(type.contains("Requirements")) {
							doc.addRequirements(SelectableStatement.splitTextIntoStatements(text, true));
						} else if(type.contains("Exceptions")) {
							doc.addExceptionsList(SelectableStatement.splitTextIntoStatements(text, true));
						}
					}
				}
			}
		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}catch(SAXException se) {
			se.printStackTrace();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
		*/
	}
	
	private void parseStatementsAndSelect(SSFPriorFileDocument doc, String starterText) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse(new ByteArrayInputStream(starterText.getBytes("UTF-8")));
			Element docEle = dom.getDocumentElement();
			NodeList nl = docEle.getElementsByTagName("statement");
			
			if(nl != null && nl.getLength() > 0) {
				if(doc.getRequirements() != null) { doc.getRequirements().clear(); }
				if(doc.getExceptionsList() != null) { doc.getExceptionsList().clear(); }
				if(doc.getLegalDescriptions() != null) { doc.getLegalDescriptions().clear(); }
				
				for(int i = 0 ; i < nl.getLength();i++) {
					Element el = (Element)nl.item(i);
					String type = "";
					String text = "";
					
					NodeList _nl = el.getElementsByTagName("type");
					if(_nl != null && _nl.getLength() > 0) {
						type = _nl.item(0).getFirstChild().getNodeValue();
					}

					_nl = el.getElementsByTagName("text");
					if(_nl != null && _nl.getLength() > 0) {
						text = _nl.item(0).getFirstChild().getNodeValue();
					}
					
					if(!type.equals("") && !text.equals("")) {
						if(type.contains("Legal Description")) {
							doc.addLegalDescriptions(SelectableStatement.splitTextIntoStatements(text, null));
						} else if(type.contains("Requirements")) {
							doc.addRequirements(SelectableStatement.splitTextIntoStatements(text, null));
						} else if(type.contains("Exceptions")) {
							doc.addExceptionsList(SelectableStatement.splitTextIntoStatements(text, null));
						}
					}
				}
			}
		}catch(Exception e) {
			logger.error("Cannot parse and select statements on searchid " + searchId, e);
		}
	}

	@SuppressWarnings("unchecked")
	public static void parseAndFillResultMap(StarterType starter, ResultMap m, long searchId) {
		
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		//SimpleDateFormat format1 = new SimpleDateFormat("MMddyyyy");
		FullStateType stateT = starter.getState().getFullState();
		StateAbbreviationType stateAbrevT = starter.getState().getAbbreviation();
		//Token countyT = starter.getCounty();
		
		Token agentFIleIdT = starter.getAgentFileId();
		Token agentIdT = starter.getAgentId();
		
		//Token comunityFileIdT = starter.getCommunityFileId();
		Date effectiveDate = starter.getEffectiveDate();
		//Token estateOfInterestT = starter.getEstateOfInterest();
		Token fileTypeT = starter.getFileType();
		//Token underWriterT = starter.getUnderwriter();
		
		//GrantorGranteeType buyerT = starter.getBuyer();
		//GrantorGranteeType  sellerT = starter.getSeller();
		
		String fileType = "";
		if(fileTypeT!=null){
			fileType = fileTypeT.toString();
		}
		
		String state = ""; 
		try{ state = stateT.getValue().toString(); }catch( NullPointerException e){}
		if(StringUtils.isBlank(state)){
			try{ state = stateAbrevT.getValue().toString(); }catch( NullPointerException e){}
		}
		
		try {
			m.put("OtherInformationSet.SrcType","SF");
			
			String instrument = "";
			if(agentIdT!=null){
				instrument+=agentIdT;
			}
			if(agentFIleIdT!=null){
				if(instrument.length()>0){
					instrument+="_"+agentFIleIdT;
				}else{
					instrument+=agentFIleIdT;
				}
			}
			if(effectiveDate!=null){
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(effectiveDate);
				instrument+="_"+calendar.get(Calendar.YEAR);
			}
			
			instrument = instrument.replaceAll("[^a-zA-Z0-9_-]+", "");
			m.put("SaleDataSet.InstrumentNumber", instrument);
			
			if("BASE".equalsIgnoreCase(fileType)){
				m.put("SaleDataSet.DocumentType", "Base File");
			}else{
				m.put("SaleDataSet.DocumentType", "Prior File");
			}
			
			if(effectiveDate != null) {
				m.put("SaleDataSet.RecordedDate", format.format(effectiveDate));
			}
			//m.put("SaleDataSet.MortgageAmount", consAmount.trim().replaceAll("[\\$,]+", ""));
			
			Vector pisVector = new Vector();
			
			Properties_type0 allProps = starter.getProperties();
			if(allProps!=null){
				PropertyType[] props = allProps.getProperty();
				if(props !=null){
					for(PropertyType p:props){
						
						PropertyIdentificationSet pis = new PropertyIdentificationSet();
						LegalType[]legals = p.getLegal();
						if(legals !=null){
							for(LegalType l:legals){
								SubdivisionType[] subdivs = l.getSubdivision();
								if(subdivs !=null){
									for(SubdivisionType s:subdivs){
										String name ="";
										String lot = "";
										String acreage = "";
										String block = "";
										boolean isCondo = false;
										String phase = "";
										String platBook = "";
										String platPage = "";
										String platInst ="";
										String unit = "";
										
										try{name = s.getName().toString();}catch(RuntimeException e){}
										try{lot = s.getLotFreeForm().getLotFreeFormType().toString();}catch(RuntimeException e){}
										try{name = s.getName().toString();}catch(RuntimeException e){}
										try{acreage = s.getAcreage().getLetterAndNumberTokenType().toString();}catch(RuntimeException e){}
										try{block = s.getBlockFreeForm().getLotFreeFormType().toString();}catch(RuntimeException e){}
										try{isCondo = s.getIsCondo();}catch(RuntimeException e){}
										try{phase = s.getPhase().getLotFreeFormType().toString();
											phase = RomanNumber.isRomanNumber(phase) ? Integer.toString(RomanNumber.parse(phase)) : phase;}catch(RuntimeException e){}
										try{platBook = s.getPlatBook().getLetterAndNumberTokenType().toString();}catch(RuntimeException e){}
										try{platPage = s.getPlatPage().getLetterAndNumberTokenType().toString();}catch(RuntimeException e){}
										try{platInst = s.getPlatInstrument().getApnType().toString();}catch(RuntimeException e){}
										try{unit = s.getUnit().getLetterAndNumberTokenType().toString();}catch(RuntimeException e){}
										
										pis.setAtribute("SubdivisionName", name.replace("*", ""));
										pis.setAtribute("SubdivisionLotNumber", lot.replace("*", ""));
										pis.setAtribute("Acreage",acreage.replace("*", ""));
										pis.setAtribute("SubdivisionBlock",block.replace("*", ""));
										pis.setAtribute("SubdivisionCond",isCondo?name.replace("*", ""):"");
										pis.setAtribute("SubdivisionPhase",phase.replace("*", ""));
										pis.setAtribute("PlatBook",platBook.replace("*", ""));
										pis.setAtribute("PlatNo",platPage.replace("*", ""));
										pis.setAtribute("PlatInstr",platInst.replace("*", ""));
										pis.setAtribute("SubdivisionUnit",unit.replace("*", ""));
									}
								}
								TownshipType[] townships = l.getTownship();
								if(townships!=null){
									for(TownshipType t:townships){
										String sec = "";
										String tw = "";
										String rg = "";
										String qo = "";
										String qv = "";
										String arb = "";
										
										try{sec = t.getSection().getBookOrPageOrEmptyType().toString();}catch(RuntimeException e){}
										try{tw = t.getTownship().getLetterAndNumberTokenType().toString();}catch(RuntimeException e){}
										try{rg = t.getRange().getLetterAndNumberTokenType().toString();}catch(RuntimeException e){}
										try{qo = t.getQuarterOrder().getLetterAndNumberTokenType().toString();}catch(RuntimeException e){}
										try{qv = t.getQuarterValue().getLetterAndNumberTokenType().toString();}catch(RuntimeException e){}
										try{arb = t.getArb().toString();}catch(RuntimeException e){}
										
										pis.setAtribute("SubdivisionSection", sec.replace("*", ""));
										pis.setAtribute("SubdivisionTownship", tw.replace("*", ""));
										pis.setAtribute("SubdivisionRange", rg.replace("*", ""));
										pis.setAtribute("QuarterOrder", qo.replace("*", ""));
										pis.setAtribute("QuarterValue", qv.replace("*", ""));
										pis.setAtribute("ARB", arb.replace("*", ""));
									}
								}
							}
						}
						AddressType ad = p.getAddress();
						if(ad!=null){
							String no = "";
							String name = "";
							String unit = "";
							String city = "";
							
							try{unit = ad.getIdentifierNumber().toString();}catch(RuntimeException e){}
							try{no = ad.getNumber().getLetterAndNumberTokenType().toString();}catch(RuntimeException e){}
							try{name = ad.getStreetName().toString();}catch(RuntimeException e){}
							try{city = ad.getCity().toString();}catch(RuntimeException e){}
							
							pis.setAtribute("StreetName",name); 
							pis.setAtribute("StreetNo",no); 
							pis.setAtribute("City", city);
							if(pis.getAtribute("SubdivisionUnit")==null){
								pis.setAtribute("SubdivisionUnit",unit);
							}
						}
						
						LabelsPropertyType labels = p.getLabels();
						if(labels!=null){
							PinType[] pins = labels.getPin();
							if(pins!=null){
								for(PinType pin:pins){
									String apn = "";
									try{apn = pin.getApn().getApnType().toString();}catch(RuntimeException e){}
									if(StringUtils.isBlank(apn)){
										try{apn = pin.getParcelId().getApnType().toString();}catch(RuntimeException e){}
									}
									if(StringUtils.isBlank(apn)){
										try{apn = pin.getPropertyId().getApnType().toString();}catch(RuntimeException e){}
									}
									if(!StringUtils.isBlank(apn)){
										pis.setAtribute("ParcelID",apn);
										break;
									}
								}
							}
						}
						pisVector.add(pis);
					} 
				} 
			}
			m.put("PropertyIdentificationSet", pisVector);
			
			GrantorGranteeType seller = starter.getSeller();
			if(seller!=null){
				
				PartyType[] parties = seller.getParty();
				if(parties !=null){
					ArrayList<List> grantor = new ArrayList<List>();
					String grantorString = "";
					String[] suffixes, type, otherType;
					for(PartyType party:parties){
						NameType name = party.getName();
						if(name!=null){
							String first = "";
							String middle = "";
							String last = "";
							
							try{first = name.getFirst().toString();}catch(RuntimeException e){}
							try{middle = name.getMiddle().toString();}catch(RuntimeException e){}
							try{
								NameTypeChoice_type0[] last0 = name.getNameTypeChoice_type0();
								for(NameTypeChoice_type0 l:last0){
									last = l.getLast().toString();
									if(StringUtils.isBlank(last)){
										last = l.getFullName().toString();
									}
									if(!StringUtils.isBlank(last)){
										break;
									}
								}
							}catch(RuntimeException e){}
							
							first = ro.cst.tsearch.utils.StringUtils.prepareStringForHTML(first);
							middle = ro.cst.tsearch.utils.StringUtils.prepareStringForHTML(middle);
							last = ro.cst.tsearch.utils.StringUtils.prepareStringForHTML(last);
							
							String names[] = {first, middle, last, "", "", ""};
							grantorString += first + " "+ middle+ " "+last +" / ";
							
							suffixes = GenericFunctions.extractNameSuffixes(names);
							type = GenericFunctions.extractAllNamesType(names);
			    			otherType = GenericFunctions.extractAllNamesOtherType(names);
			    			
			    			GenericFunctions.addOwnerNames(grantorString, names, suffixes[0], suffixes[1], type, otherType, 
									NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), grantor);
							
						}
					}
					m.put("SaleDataSet.Grantor", grantorString);
					m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
				}
			}
			
			GrantorGranteeType buyer = starter.getBuyer();
			if(buyer!=null){
				PartyType[] parties = buyer.getParty();
				if(parties !=null){
					ArrayList<List> grantee = new ArrayList<List>();
					String granteeString = ""; 
					String[] suffixes, type, otherType;
					for(PartyType party:parties){
						NameType name = party.getName();
						if(name!=null){
							String first = "";
							String middle = "";
							String last = "";
							
							try{first = name.getFirst().toString();}catch(RuntimeException e){}
							try{middle = name.getMiddle().toString();}catch(RuntimeException e){}
							try{
								NameTypeChoice_type0[] last0 = name.getNameTypeChoice_type0();
								for(NameTypeChoice_type0 l:last0){
									last = l.getLast().toString();
									if(StringUtils.isBlank(last)){
										last = l.getFullName().toString();
									}
									if(!StringUtils.isBlank(last)){
										break;
									}
								}
							}catch(RuntimeException e){}
							
							first = ro.cst.tsearch.utils.StringUtils.prepareStringForHTML(first);
							middle = ro.cst.tsearch.utils.StringUtils.prepareStringForHTML(middle);
							last = ro.cst.tsearch.utils.StringUtils.prepareStringForHTML(last);
							
							if (last.toLowerCase().contains("trustees of")){
								last = last.replaceAll("(?is)\\b(?:CO-)?(Trustees)\\s+of\\s+the", " $1 @@");
								String[] moreNames = last.split("@@");
								for (String eachName : moreNames){
									eachName = eachName.replaceFirst("(?is)(?:&|\\bAND\\b)\\s*([A-Z]+\\s+[A-Z]+),?\\s+\\(?\\s*TR(?:USTEE)?S\\s*\\)?\\s*$", "TR & $1 TR");
									
									String[] names = StringFormats.parseNameDesotoRO(eachName, true);
									
									suffixes = GenericFunctions.extractNameSuffixes(names);
									type = GenericFunctions.extractAllNamesType(names);
					    			otherType = GenericFunctions.extractAllNamesOtherType(names);
					    			granteeString += names[0] + " "+ names[1] + " "+names[2] +" / ";
					    			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(names[5])){
					    				granteeString += names[3] + " "+ names[4] + " "+names[5] +" / ";
					    			}
					    			
					    			GenericFunctions.addOwnerNames(granteeString, names, suffixes[0], suffixes[1], type, otherType, 
											NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), grantee);
								}
							} else {
								String names[] = {first, middle, last, "", "", ""};
								granteeString += first + " "+ middle+ " "+last +" / ";
								
								suffixes = GenericFunctions.extractNameSuffixes(names);
								type = GenericFunctions.extractAllNamesType(names);
				    			otherType = GenericFunctions.extractAllNamesOtherType(names);
				    			
				    			GenericFunctions.addOwnerNames(granteeString, names, suffixes[0], suffixes[1], type, otherType, 
										NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), grantee);
							}
						}
					}
					m.put("SaleDataSet.Grantee", granteeString);
					m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
				}
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void fixSpecialNullValues(StarterType starter) {
		
		Properties_type0 allProps = starter.getProperties();
		if(allProps!=null){
			PropertyType[] props = allProps.getProperty();
			if(props!=null){
				for(PropertyType p:props){
					LegalType[] legals = p.getLegal();
					if(legals !=null){
						for(LegalType l:legals){
							TownshipType[] townships = l.getTownship();
							SubdivisionType[] subdivisions = l.getSubdivision();
							
							if(townships !=null){
								for(TownshipType t:townships){
									if(t.getSectionFreeForm()==null){
										t.setSectionFreeForm(new Token(""));
									}
								}
							}
							
							if(subdivisions!=null){
								for(SubdivisionType t:subdivisions){
									if(t.getName()==null){
										t.setName(new Token(""));
									}
								}
							}
							
						}
					}
				}
			}
		}
	}
	
	public String buildHtml(StarterType starter, String starterContent) throws XMLStreamException, FactoryConfigurationError, IOException {
		StringBuilder html = new StringBuilder();
		try{
			String content = starterContent;
			int start1 = content.indexOf("<starter>");
			int end1 = content.indexOf("</starter>");
			if(start1>0&&end1>0&&start1<end1){
				content = content.substring(start1, end1+"</starter>".length());
			}
			if(starter!=null){
				fixSpecialNullValues(starter);
				
				OMElement starterElement = starter.getOMElement(new javax.xml.namespace.QName("","starter"), OMAbstractFactory.getOMFactory());
				ByteArrayOutputStream byteStream  = new ByteArrayOutputStream();
				
				OMOutputFormat format = new OMOutputFormat();
				format.setIgnoreXMLDeclaration(true);
				format.setDoOptimize(true);
				format.setAutoCloseWriter(true);
				format.setCharSetEncoding("UTF-8");
				starterElement.serialize(byteStream,format);
				byteStream.flush();
				
				content = new String(byteStream.toByteArray(),"UTF-8");
			}
			
			content = content.replaceAll("(?i)[ ]+xmlns:ns[0-9]?=\"[^\"]+\"[ ]*", "");
			content = content.replaceAll("(?i)[ ]+xmlns=\"[^\"]+\"[ ]*", "");
			content = content.replaceAll("(?i)[ ]*ns[0-9]?:[ ]*", "");
			for(int i=0;i<10;i++){
				content = content.replaceAll("(?i)<[^>/]+>[ \n\r\t]*<[/][^>]+>", "");
			}
			content = content.replaceAll("(?i)[ \n\r\t]*<[^>]+[/]>[ \n\r\t]*", "");
			
			content = content.replaceAll("(?i)<url>([^>]+)</url>\\s*</link>","<url>$1</url> <name>Link</name></link>");
			content = content.replaceAll("(?i)<url>([^>]+)</url>\\s*<name/>","<url>$1</url> <name>Link</name>");
			content = content.replaceAll("(?i)<url>([^>]+)</url>\\s*<name>(//s*)</name>","<url>$1</url> <name>Link</name>");
			content = content.replaceAll("(?i)<url>([^>]+)</url>\\s*<name>([^>]+)</name>","&lt;a href='$1'&gt;$2&lt;/a&gt;");
			content = content.replaceAll(">[ \t\r\n]+<","><");
			content = content.replaceAll("(?i)\\s*<instrument>.*?</instrument>\\s*","");
			content = content.replaceAll("(?i)</link>","");
			content = content.replaceAll("(?i)<link>","");
			
			content =  htmlRepresentation(XmlUtils.parseXml(content,"UTF-8"), "");
			
			content.replaceAll("&lt;a\\s+href='([^']+)'\\s*&gt;(.*?)&lt;/a&gt;","<a href='$1'>$2</a>");
			content = content.replaceAll("(?i)<br><br>", "<br>");
			content = content.replaceAll("(?i)&gt;(&nbsp;)+&lt;","&gt;&lt;");
			content  = content.replace("![CDATA[", "");
			content  = content.replace("]]", "");
			content = content.replace("&lt;text&gt;", "&lt;text&gt;<table border=\"1\" width=\"98%\"><tr><td><font color=\"#00cccc\">");
			content = content.replace("&lt;/text&gt;", "</font></td></tr></table>&lt;/text&gt;");
			
			StringBuilder newContent = new StringBuilder();
			
			int start = 0;
			int stop = 0;
			int oldStart = 0;
			while( (start=content.indexOf("&lt;text&gt;",start))>0  
					&& 
					(stop=content.indexOf("&lt;/text&gt;",stop))>0 ){
				String beforeStrat = content.substring(oldStart,start+12);
				String between = content.substring(start+12,stop+13);
				newContent.append(beforeStrat);
				newContent.append(between.replace("\n", "<br>").replace("&nbsp;", ""));
				start = stop;
				oldStart = stop;
				stop++;
			}
			if(stop<content.length()){
				newContent.append(content.substring(stop));
			}
			
			content = newContent.toString();
			
			//content = content.replaceAll("’", "&apos;");
			content = content.replaceAll("(?is)<br>\\s+<br>", "<br><br>");
			
			html.append("<table border=\"1\">");
			html.append("<tr>");
			html.append("<td>");
			html.append(content);
			
			html.append("</td>");
			html.append("</tr>");
			html.append("</table>");
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return html.toString();
	}
	
	public String parseAndBuildImageHTML(Map<String, String> params, List<DocumentInfoType> documents, List<ParsedResponse> list){
		
		StringBuilder fullHtml = new StringBuilder();
		fullHtml.append("<table border=\"1\">");
		fullHtml.append("<tr>");
		fullHtml.append("<td>");
		
		for(DocumentInfoType document: documents){
			try{
				ParsedResponse item = new ParsedResponse();
				DocumentIndexType doc = document.getDocIndex();
				if(doc == null) {
					continue;
				}
				
				String book = null;
				String page = null;
				BookPageType bookPage = doc.getBookPage();
				if (bookPage!=null) {
					LetterAndNumberTokenType newbook = bookPage.getBook();
					if (newbook!=null) {
						book = newbook.toString();
					}
					LetterAndNumberTokenType newpage = bookPage.getPage();
					if (newpage!=null) {
						page = newpage.toString();
					}
				}
				
				String instno = null;
				ApnType newinst = doc.getInstNo();
				if (newinst!=null) {
					instno = newinst.toString();
				}
				
				int year = 0;
				if(doc.getYear() != null) {
					year = doc.getYear().getYear();
				}
				int month = -1;
				if(doc.getMonth() != null) {
					month = doc.getMonth().getMonth() - 1;
				}
				int day = -1;
				if(doc.getDay() != null) {
					day = doc.getDay().getDay();
				}
				
				Date recordedDate = null;
				
				if(year > 0 && month >= 0 && day >= 0) {
					Calendar recordedCalendar = Calendar.getInstance();
					try {
						recordedCalendar.set(year, month, day);
						recordedDate = recordedCalendar.getTime();
					} catch (Exception e) {
						logger.error("Error while trying to convert to date: " + year + "/" + month + "/" + day, e);
					}
				}
				
				String category = null;
				String subcategory = null;
				if(document.getDocIndex().getDataSourceType() != null) {
					category = document.getDocIndex().getDataSourceType().toString();
					if(StringUtils.isNotBlank(category)) {
						if(document.getDocIndex().getSubcategory() != null) {
							subcategory = document.getDocIndex().getSubcategory().toString();
						} else {
							subcategory = DocumentTypes.MISCELLANEOUS;
						}
					}
				} else {
					category = DocumentTypes.MISCELLANEOUS;
					subcategory = DocumentTypes.MISCELLANEOUS;
				}
				
				String ssfLink = document.getLink().toString();
				
				
//				String html = "<table cellspacing=\"6\"><tr><td><b> BOOK/PAGE: </b> " + StringUtils.defaultString(book) + 
//						"/" + StringUtils.defaultString(page) + "</td></tr><tr><td><b> INSTRUMENT NUMBER: </b>" + StringUtils.defaultString(instno) + 
//						"</td></tr><tr><td><b> YEAR: </b>" + (year <= 0?"":year) + "</td></tr>" +
//						"<tr><td><b> Recorded Date: </b>" + category +  "</td></tr>" +
//						"<tr><td><b> CATEGORY: </b>" + category +  "</td></tr>" +
//						"<tr><td><b> SUBCATEGORY: </b>" + subcategory +  "</td></tr>" +
//						"<tr><td><a href=\"" + ssfLink + "\">View image</a></td></tr></table>";
//				
//				fullHtml.append( html  );
//				
//				item.setResponse(html);
				
				
				InstrumentI instrument = new Instrument();
				instrument.setInstno(instno);
				instrument.setBook(book);
				instrument.setPage(page);
				instrument.setYear(year);
				instrument.setDocType(category);
				instrument.setDocSubType(subcategory);
				
				RegisterDocument defaultDocument = new RegisterDocument( DocumentsManager.generateDocumentUniqueId(searchId, instrument) );
				
				
				ApnType[] apnTypes = doc.getApn();
				if(apnTypes != null && apnTypes.length != 0) {
					for (ApnType apnType : apnTypes) {
						if(apnType != null && StringUtils.isNotBlank(apnType.toString())) {
							PropertyI property = Property.createEmptyProperty();
							PinI pin = property.getPin();
							pin.addPin(com.stewart.ats.base.property.PinI.PinType.PID, apnType.toString().trim());
							defaultDocument.addProperty(property);
						}
					}
				}
				
				
				RegisterDocumentI docR = DocumentsManager.createRegisterDocument(searchId, category, defaultDocument, null);
				
				
				docR.setInstrument(instrument);
				docR.setSearchType(SearchType.IM);
				docR.setDataSource(getDataSite().getSiteTypeAbrev());
				docR.setServerDocType(subcategory);
				docR.setSiteId(getServerID());
				if(recordedDate != null) {
					docR.setRecordedDate(recordedDate);
				}
				
				String html = docR.asHtml();
				fullHtml.append( html + "<br><a href=\"" + ssfLink + "\">View image</a>" );
				item.setResponse(html + "<br><a href=\"" + ssfLink + "\">View image</a>");
				
//				item.setOnlyResponse("<tr><td align=\"center\" >" + checkBox + "</td><td>" + asHtml + footer.toString()  + "</td></tr>");
				item.setAttribute(ParsedResponse.SERVER_ROW_RESPONSE, "<tr><td>" + html + "</td></tr>");
				
				
				Map<String, String> extractParametersFromQuery = ro.cst.tsearch.utils.StringUtils.extractParametersFromQuery(ssfLink);
				String ext = extractParametersFromQuery.get("ext");
				if(StringUtils.isBlank(ext)) {
					ext = "tiff";
				}
				
				
				getSearch().addImagesToDocument(docR, ssfLink + "&" + docR.prettyPrint() + "." + ext);
				
				ImageI image = docR.getImage();
				image.setSsfLink(ssfLink);
				image.getLinks().clear();
				
								
//				item.addImageLink(new ImageLinkInPage(document.getLink().toString(), (instno.length()!=0?instno:book+"-"+page) + ".tif"));
				
		    	item.setDocument(docR);
		    	
		    					
				list.add(item);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		fullHtml.append("</td>");
		fullHtml.append("</tr>");
		
		fullHtml.append("<tr>");
		fullHtml.append("<td>");
		fullHtml.append("NEXT");
		fullHtml.append("</td>");
		fullHtml.append("</tr>");
		fullHtml.append("</table>");
		
		return fullHtml.toString();
	}
	
	private static String htmlRepresentation(Node doc, String prefix){
		
		StringBuilder sb = new StringBuilder();
		String name = doc.getNodeName();
		String value = XmlUtils.getNodeValue(doc);
		
		if(!"".equals(value)){
			sb.append("<br>"); 
			sb.append(prefix); 
			sb.append("&lt;" + name + "&gt;");
			sb.append("<b>&nbsp;"+value+"&nbsp;</b>"); 
			sb.append("&lt;/" + name + "&gt;");
			sb.append("<br>");
		} else{
			if(!"#text".equalsIgnoreCase(name)){
				sb.append(prefix); 
				sb.append("&lt;" + name + "&gt;");
				for(Node child: XmlUtils.getChildren(doc)){							
					sb.append(htmlRepresentation(child, prefix + "&nbsp;&nbsp;&nbsp;&nbsp;"));
				}
				sb.append(prefix); 
				sb.append("&lt;/" + name + "&gt;");
			}
		}
		return sb.toString();
	}

	private DocAdminConn getConn() {
		if(conn == null) {
			conn = new DocAdminConn(getCommunityId(), getDataSite());
		}
		return conn;
	}
	
	@Override
	public void addDocumentAdditionalPostProcessing(DocumentI doc, ServerResponse response) {
		super.addDocumentAdditionalPostProcessing(doc, response);
//		if(doc instanceof SSFPriorFileDocument) {
//			((SSFPriorFileDocument)doc).updateStartViewDateAndReturn(getSearch().getDocManager());
//		}
	}
	
}


