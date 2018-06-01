package ro.cst.tsearch.servers.types;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.HoaAndCondoMapper;
import ro.cst.tsearch.database.rowmapper.HoaInfoMapper;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.SearchDataWrapper;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.SearchManager;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.RawResponseWrapper;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SearchLogger;

import com.stewart.ats.base.document.DocumentI;


/**
 * @author mihaib
*/

@SuppressWarnings("deprecation")
public class GenericSearchHO extends TSServer{

	private static final long serialVersionUID = 1L;
	private boolean downloadingForSave;

	public GenericSearchHO(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public GenericSearchHO(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,	int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,	miServerID);
		resultType = MULTIPLE_RESULT_TYPE;
	}

		
	@Override
	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		int searchType = global.getSearchType();
		
		String subdivision = global.getSa().getAtribute(SearchAttributes.LD_SUBDIV_NAME);
		if(searchType == Search.AUTOMATIC_SEARCH) {
        
	        TSServerInfoModule m = null;	        
	        //subdivision search
	        {
	        	if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(subdivision) && subdivision.length() > 4){
				    m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.SUBDIVISION_PLAT_MODULE_IDX));
				    m.clearSaKeys();
				    m.forceValue(0, subdivision);
					m.forceValue(1, global.getSa().getAtribute(SearchAttributes.P_COUNTY_FIPS));
					modules.add(m);
	        	} else {
				     SearchLogger.logWithServerName("I will not search with subdivision = " + subdivision + " because must have at least 4 characters."
				    		 , searchId, SearchLogger.ERROR_MESSAGE, getDataSite());
	        	}
	        }
		}
	    serverInfo.setModulesForAutoSearch(modules);
	}
	
	@Override
	 protected ServerResponse performRequest(String page, int methodType,String action, int parserId, String imagePath, String vbRequest, Map<String, Object> extraParams)throws ServerResponseException {

		String query = getTSConnection().getQuery();

		int count = 1;
		ServerResponse response = null;

		try { 
			if("GetLink".equalsIgnoreCase(action) || "SaveToTSD".equalsIgnoreCase(action)){
				response = new ServerResponse();
			}else{
				Map<String, String> allParams = new HashMap<String, String>();
				List<TSServerInfoFunction> allParameters = ((TSServerInfoModule)extraParams.get(TSServerInfoConstants.EXTRA_PARAM_MODULE_OBJECT_SOURCE)).getFunctionList();
				for(TSServerInfoFunction param : allParameters){
					allParams.put(param.getParamName(), param.getParamValue());
				}
				response = searchByInternal(allParams);
			}
			if (query.indexOf("parentSite=true") >= 0 || isParentSite()){
				response.setParentSiteSearch(true);
			}
						
			response.setQuerry(query);
						
			RawResponseWrapper rrw = new RawResponseWrapper(response.getResult());
			solveResponse(page, parserId, action, response,rrw, imagePath);

			} catch (Exception th) {
				logger.error("Unexpected Error...count=" + count + "\n"+ th.getMessage());
				SearchLogger.info("</div>", searchId); // for Bug 2652
				th.printStackTrace(System.err);
			}

		return response;
	}
	
	protected  ServerResponse searchByInternal(Map<String, String> allParams) throws UnsupportedEncodingException{
		
		String subdivisionName = URLDecoder.decode(StringUtils.defaultString(allParams.get("subdivisionName")), "UTF-8");
		String platBook = URLDecoder.decode(StringUtils.defaultString(allParams.get("platBook")), "UTF-8");
		String platPage = URLDecoder.decode(StringUtils.defaultString(allParams.get("platPage")), "UTF-8");
		String countyFips = URLDecoder.decode(StringUtils.defaultString(allParams.get("countyFIPS")), "UTF-8");
		
		ArrayList<String> querry_params = new ArrayList<String>();		
		
		List<Map<String, Object>> data = null;
		String query = "";
		ServerResponse resp = new ServerResponse();
		
		//boolean platBookPageSearch = false;
		if (StringUtils.isNotEmpty(subdivisionName)){
			query += HoaInfoMapper.FIELD_SUBDIVISION_NAME + " LIKE ? ";
			querry_params.add(subdivisionName + "%");
		} else if (StringUtils.isNotEmpty(platBook) && StringUtils.isNotEmpty(platPage)){
			query += HoaInfoMapper.FIELD_PLAT_BOOK + " = ? ";
			querry_params.add(platBook);
			query += " AND " + HoaInfoMapper.FIELD_PLAT_PAGE + " = ? ";
			querry_params.add(platPage);
			//platBookPageSearch = true;
		}	
		
		String additionalQuery = "";
		
		additionalQuery += " AND " + HoaInfoMapper.FIELD_COUNTYFIPS + " = ?";
		querry_params.add(countyFips);
		
		data = DBManager.getHoaCondoData(HoaInfoMapper.SQL_SELECT_HOA_INFO_DATA, query + additionalQuery, querry_params.toArray());
		
		if (data != null){
			if (data.isEmpty()){
				resp.setResult("No data found.");
			} else {
				StringBuffer resultBuff = new StringBuffer("<table id=\"result\" border=\"1\"><tr><th>ID</th><th>Subdivision Name</th><th>Plat Book</th><th>Plat Page</th><th>CCR/DEC Book</th>");
				resultBuff.append("<th>CCR/DEC Page</th><th>HOA Name</th><th>Master HOA</th><th>Additional HOA</th><th>Lien/JDG/NOC</th><th>County</th></tr>");
			
				String instrNo = "";
				for (Map<String, Object> asa : data){
					if (ro.cst.tsearch.utils.StringUtils.isEmpty(instrNo)){
						instrNo = asa.get("id").toString();
					}
					resultBuff.append("<tr><td>").append(asa.get("id")).append("</td><td>").append(asa.get(HoaInfoMapper.FIELD_SUBDIVISION_NAME)).append("</td><td>")
								.append(asa.get(HoaInfoMapper.FIELD_PLAT_BOOK)).append("</td><td>")
								.append(asa.get(HoaInfoMapper.FIELD_PLAT_PAGE)).append("</td><td>")
								.append(asa.get(HoaInfoMapper.FIELD_CCR_DEC_BOOK)).append("</td><td>")
								.append(asa.get(HoaInfoMapper.FIELD_CCR_DEC_PAGE)).append("</td><td>")
								.append(asa.get(HoaInfoMapper.FIELD_HOA_NAME)).append("</td><td>")
								.append(asa.get(HoaInfoMapper.FIELD_MASTER_HOA)).append("</td><td>")
								.append(asa.get(HoaInfoMapper.FIELD_ADD_HOA)).append("</td><td>")
								.append(asa.get(HoaInfoMapper.FIELD_LIEN_JDG_NOC)).append("</td><td>")
								.append(asa.get(HoaInfoMapper.FIELD_COUNTY)).append("</td></tr>");
				}
				resultBuff.append("</table>");
				String result = resultBuff.toString();
				result = result.replaceAll("(?is)(<td[^>]*>)(</td>)", "$1&nbsp;$2");
				resp.setResult(result);
			}
		} else {
			resp.setResult("Error when loading from database.");
		}
		return  resp;
	}
		
	@Override
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String initialResponse = Response.getResult();
		
		String rsResponse = initialResponse;
	
		switch (viParseID) {
			case ID_DETAILS :
				
				if (rsResponse.indexOf("Error when loading from database") != -1){
					Response.getParsedResponse().setError("Error when loading info");
					return;
				} else if (rsResponse.indexOf("No data found") != -1){
					Response.getParsedResponse().setError("No results found");
					return;
				}
		        
				String originalID = ro.cst.tsearch.utils.StringUtils.extractParameter(rsResponse, "(?is)County</th></tr><tr><td>([^<]+)</td>");
		        String keyNumber = originalID;					
				
				if ((!downloadingForSave))
				{	
	                String qry_aux = Response.getRawQuerry();
	               
					qry_aux = "dummy=" + keyNumber + "&" + qry_aux;
					
					String originalLink = getLinkPrefix(TSConnectionURL.idGET) + sAction + "&" + qry_aux;
					String sSave2TSDLink = originalLink;
					
					Pattern p = Pattern.compile("subdivisionName=([^&]*)");
	                Matcher m = p.matcher(originalLink);
					if(Response.isParentSiteSearch() && m.find()){
						String subName = m.group(1);
						try {
							originalLink = originalLink.replace(subName, URLEncoder.encode(subName, "UTF-8"));
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
						try {
							sSave2TSDLink = sSave2TSDLink.replace(subName, URLEncoder.encode(URLEncoder.encode(subName, "UTF-8"), "UTF-8"));
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
						
	                }
					
					HashMap<String, String> data = new HashMap<String, String>();
    				data.put("type", "HOA");
	    				
					if (isInstrumentSaved(keyNumber, null, data)){
						rsResponse += CreateFileAlreadyInTSD();
					} else {
						mSearch.addInMemoryDoc(sSave2TSDLink, rsResponse);
						rsResponse = addSaveToTsdButton(rsResponse, originalLink, viParseID);
						
					}
					LinkInPage lip = new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD);
	                Response.getParsedResponse().setPageLink(lip);
	                Response.getParsedResponse().setResponse(rsResponse);
	                Response.setResult(rsResponse);
	            } else {      					
					smartParseDetails(Response, rsResponse);
	                msSaveToTSDFileName = keyNumber + ".html";
	                Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
	                msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();	                
				}
				break;	
			case ID_SAVE_TO_TSD :
				downloadingForSave = true;
					ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
				break;

			
		}
	}
	
	private ServerResponse insertIntoDatabase(List<TSServerInfoModule> modules){
		
		StringBuilder statusMessage = new StringBuilder("Status:<br>");
		
		Map<String, String> allParams = new HashMap<String, String>();
		Map<String, Object> query_params = new HashMap<String, Object>();
		
		String countyName = "", countyFIPS = "";
		
		for (TSServerInfoModule module : modules){
			List<TSServerInfoFunction> allParameters = module.getFunctionList();
			for(TSServerInfoFunction param : allParameters){
				allParams.put(param.getParamName(), param.getParamValue());
			}
			
			String subdivisionName = StringUtils.defaultString(allParams.get("subdivisionName")).toUpperCase();
			
			if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(subdivisionName)){
				query_params.put(HoaInfoMapper.FIELD_SUBDIVISION_NAME, 	subdivisionName);
				query_params.put(HoaInfoMapper.FIELD_PLAT_BOOK, 		StringUtils.defaultString(allParams.get("platBook")).toUpperCase());
				query_params.put(HoaInfoMapper.FIELD_PLAT_PAGE, 		StringUtils.defaultString(allParams.get("platPage")).toUpperCase());
				query_params.put(HoaInfoMapper.FIELD_CCR_DEC_BOOK, 		StringUtils.defaultString(allParams.get("ccrDecBook")).toUpperCase());
				query_params.put(HoaInfoMapper.FIELD_CCR_DEC_PAGE, 		StringUtils.defaultString(allParams.get("ccrDecPage")).toUpperCase());
				query_params.put(HoaInfoMapper.FIELD_HOA_NAME, 			StringUtils.defaultString(allParams.get("hoaName")).toUpperCase());
				query_params.put(HoaInfoMapper.FIELD_MASTER_HOA, 		StringUtils.defaultString(allParams.get("masterHoa")).toUpperCase());
				query_params.put(HoaInfoMapper.FIELD_ADD_HOA, 			StringUtils.defaultString(allParams.get("additionalHoa")).toUpperCase());
				query_params.put(HoaInfoMapper.FIELD_LIEN_JDG_NOC, 		StringUtils.defaultString(allParams.get("lienJdgNoc")).toUpperCase());
				query_params.put(HoaInfoMapper.FIELD_NOTES, 			StringUtils.defaultString(allParams.get("notes")).toUpperCase());
				
				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(allParams.get("county"))){
					countyName = StringUtils.defaultString(allParams.get("county")).toUpperCase();
					countyFIPS = StringUtils.defaultString(allParams.get("countyFIPS"));
				}
				
				query_params.put(HoaInfoMapper.FIELD_COUNTY, 			countyName);
				query_params.put(HoaInfoMapper.FIELD_COUNTYFIPS, 		countyFIPS);
				
				boolean updated = DBManager.insertHOAData(query_params);
				if (updated){
					statusMessage.append("The info for Subdivision: " + subdivisionName + " was succesfully inserted in the database!<br>");
				} else {
					statusMessage.append("The info for Subdivision: " + subdivisionName + " was not inserted in database!<br>");
				}
			}
		}

		ServerResponse serverReponse = new ServerResponse();
		serverReponse.getParsedResponse().setResponse(statusMessage.toString());

		return serverReponse;
	}
	@SuppressWarnings("unused")
	@Deprecated
	private List<Map<String, Object>> getDataForIds(List<Map<String, Object>> data, String additionalQuery, Object additionalQuerry_params){
		List<Map<String, Object>> newData = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> asa : data){
			if (StringUtils.isNotEmpty(asa.get(HoaAndCondoMapper.FIELD_ORIGINAL_ID).toString())){
				String query = HoaAndCondoMapper.FIELD_ORIGINAL_ID + " = '" + asa.get(HoaAndCondoMapper.FIELD_ORIGINAL_ID) + "'";
				List<Map<String, Object>> tempData = DBManager.getHoaCondoData(HoaAndCondoMapper.SQL_SELECT_HOA_CONDO_DATA, 
																			query + additionalQuery, additionalQuerry_params);
				if (tempData != null && !tempData.isEmpty()){
					newData.addAll(tempData);
				}
			}
		}
		return newData;
	}

	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException{
		
		if(isParentSite() && !module.isVisible() && module.getVisibleFor().equals(SearchManager.ALL_ADMIN)
				&& TSServerInfo.DATABASE_SEARCH_MODULE_IDX == module.getModuleIdx()){
			ServerResponse ret = new ServerResponse();
				
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
				
			if(!modules.isEmpty()){
				ret = insertIntoDatabase(modules);
			}
			return ret;
		} else {
			return super.SearchBy(module, sd);
		}
	}
		
	public List<TSServerInfoModule> getMultipleModules(TSServerInfoModule module,Object sd){
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		final HttpServletRequest originalRequest = ((SearchDataWrapper) sd).getRequest();
			
		if (sd instanceof SearchDataWrapper){

			int cnt = 0;

			try {
				cnt = Integer.parseInt(originalRequest.getParameter(RequestParams.PARENT_SITE_ADDITIONAL_CNT + module.getMsName()));
				} 
			catch (Exception e) {	}

			if (cnt == 0){
				modules.add(module);
				return modules;
			}

			for (int i = 0; i <= cnt; i++){

				final TSServerInfoModule mod = (TSServerInfoModule) module.clone();
				final int index = i;
					
				HttpServletRequest req = new HttpServletRequestWrapper(originalRequest){
					@Override
					public String getParameter(String name){
						if (originalRequest.getParameter(name + "_" + index) == null){
							if(index!=0){
								return "";
							}
							return originalRequest.getParameter(name);
						} else {
							return originalRequest.getParameter(name + "_" + index);
						}
					}
				};

				mod.setData(new SearchDataWrapper(req));
				modules.add(mod);
			}
		}
		
		return modules;
	}
		
	@Override
	protected Object parseAndFillResultMap(ServerResponse response,String detailsHtml, ResultMap resultMap) {
		try {
			resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "HO");
			
			SimpleDateFormat formatter = new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY);
	        String sDate = formatter.format(Calendar.getInstance().getTime());
			resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), sDate);
			resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), sDate);
			
			detailsHtml = detailsHtml.replaceAll("(?is)&nbsp;", " ");
			
			String instrNo = "";
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(detailsHtml, null);
			NodeList mainList = htmlParser.parse(null);
			
			TableTag table = (TableTag) HtmlParser3.getNodeByID("result", mainList, true);
			if (table != null){
				TableRow[] rows =  table.getRows();
				
				Vector<PropertyIdentificationSet> pisVector = new Vector<PropertyIdentificationSet>();
				
				for (TableRow row : rows){
					if (row.getColumnCount() > 2){
						PropertyIdentificationSet newPis = new PropertyIdentificationSet();
						newPis = new PropertyIdentificationSet();
						instrNo += row.getColumns()[0].toPlainTextString() + "+";
						newPis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_NAME.getShortKeyName(),  row.getColumns()[1].toPlainTextString());
						newPis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(),  row.getColumns()[2].toPlainTextString());
						newPis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(),  row.getColumns()[3].toPlainTextString());
						newPis.setAtribute("tmpPlatBook",  row.getColumns()[2].toPlainTextString());
						newPis.setAtribute("tmpPlatPage",  row.getColumns()[3].toPlainTextString());
						newPis.setAtribute("tmpDeclBook",  row.getColumns()[4].toPlainTextString());
						newPis.setAtribute("tmpDeclPage",  row.getColumns()[5].toPlainTextString());
						newPis.setAtribute("tmpHOAName",  row.getColumns()[6].toPlainTextString());
						newPis.setAtribute("tmpMasterHOA",  row.getColumns()[7].toPlainTextString());
						newPis.setAtribute("tmpAddHOA",  row.getColumns()[8].toPlainTextString());
						pisVector.add(newPis);
					}
				}

				if (!pisVector.isEmpty()){
					resultMap.put("PropertyIdentificationSet", pisVector);
				}
				if (!instrNo.isEmpty()){
					resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), GenericFunctions.sum(instrNo + "1", searchId));
				}

			}
			resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "HOA");
			resultMap.put(SaleDataSetKey.DOC_SUBTYPE.getKeyName(), "HOA");
			resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), "Home Owner Association");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	 public DocumentI smartParseDetails(ServerResponse response, String detailsHtml, boolean fillServerResponse){
			DocumentI document = null;
			StringBuilder justResponse = new StringBuilder(detailsHtml);
			try {
				ResultMap map = new ResultMap();
								
				parseAndFillResultMap(response, detailsHtml, map);
				
				map.removeTempDef();
				
				Bridge bridge = new Bridge(response.getParsedResponse(), map, searchId);
				
				document = bridge.importData();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			if(fillServerResponse) {
				response.getParsedResponse().setOnlyResponse(justResponse.toString());
				if(document!=null) {
					response.getParsedResponse().setDocument(document);
				}
			}
			
			return document;
		}
	 
}
		
