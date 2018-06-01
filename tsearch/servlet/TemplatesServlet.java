package ro.cst.tsearch.servlet;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.CommunityTemplatesMapper;
import ro.cst.tsearch.templates.AddDocsTemplates;
import ro.cst.tsearch.templates.CodeBookStatementObject;
import ro.cst.tsearch.templates.CompileTemplateResult;
import ro.cst.tsearch.templates.TemplateBuilder;
import ro.cst.tsearch.templates.TemplateUtils;
import ro.cst.tsearch.templates.edit.client.InstrumentStructForUndefined;
import ro.cst.tsearch.templates.edit.client.TemplateEditService;
import ro.cst.tsearch.templates.edit.client.TemplateInfoResultG;
import ro.cst.tsearch.templates.edit.client.TemplateSearchPermisionException;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.PriorFileDocument;
import com.stewart.ats.base.document.SSFPriorFileDocument;
import com.stewart.ats.base.reuse.ClipboardAtsI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.template.TemplatesInitUtils;
import com.stewart.ats.tsrindex.server.TsdIndexPageServer;
import com.stewartworkplace.starters.ssf.services.connection.DocAdminConn;
import com.stewartworkplace.starters.ssf.services.connection.DocAdminConn.TransactionResponse;

/**
 *The class implements  TemplateEditService and respond to RPC calls from client
 * @author Cristian Stochina
 */
public class TemplatesServlet extends RemoteServiceServlet  implements TemplateEditService{
    
	private static final long serialVersionUID = 5254303027139652880L;
	protected static final Category logger = Logger.getLogger(AddDocsTemplates.class);
	public static String TSD_TEMPLATE_UNCHECKED_MESSAGE = "Please check the TSD Template in Agent's profile!";
	
	public static final String LABEL_NAME_DELIM     = "_";
	public static Pattern previewPatternAts = Pattern.compile("(?m)(<(.*?)(\\s[^>]*)?>([^<]*?)</\\2>)|(<[^<>\\s\"/]*\\s*[^>]*/>)");
	public static Pattern previewPatternPxt = Pattern.compile("(?m)^(.*?)=(.*)$");
	public static Pattern previewAttributesPattern = Pattern.compile("(?m)([^\\s]*?)=\"([^\"]*?)\"");
	
	public static final String ATS_CURRENT_SERVER_LINK_FOR_DOC_RETREIVER = 
			ServerConfig.getAppUrl() + "/title-search/DocumentDataRetreiver" ;

	
	@Override
	public String  getState(long searchId,String templateName, int templateId){
		return TemplateUtils.getState(this.getThreadLocalRequest(), searchId, templateName,(long)templateId);
	}
	
	
	public Boolean saveTemplate(long searchId,long userId,String templateName, int templateId, String templateContent, String buttonLabel, boolean makeBackup) throws TemplateSearchPermisionException{
		return saveTemplate(searchId, userId, templateName, templateId, templateContent, buttonLabel, makeBackup, null);
	}
	
	@Override
	public Boolean saveTemplate(long searchId,long userId,String templateName, int templateId, String templateContent, 
			String buttonLabel, boolean makeBackup, HashMap<String, HashMap<String, Boolean>> statementsMap) throws TemplateSearchPermisionException{
		try {
			boolean canSave = !makeBackup;
			if(makeBackup) {
				canSave = makeTempBackup(searchId, userId, templateName, templateId, templateContent);
			}
			if(canSave) {
				HashMap<String, Boolean> reqMap = statementsMap != null ? statementsMap.get(PriorFileDocument.REQUIREMENTS) : null;
				HashMap<String, Boolean> legalMap = statementsMap != null ? statementsMap.get(PriorFileDocument.LEGAL_DESC) : null;
				HashMap<String, Boolean> excMap = statementsMap != null ? statementsMap.get(PriorFileDocument.EXCEPTIONS) : null;
				
				if (reqMap != null){
					HashMap<String, Boolean> tempMap = new HashMap<String, Boolean>();
					for(String key : reqMap.keySet()){
						 tempMap.put(StringEscapeUtils.escapeXml(StringEscapeUtils.unescapeXml(key)), reqMap.get(key));
					}
					reqMap = tempMap;
					statementsMap.put(PriorFileDocument.REQUIREMENTS, reqMap);
				}
				if (legalMap != null){
					HashMap<String, Boolean> tempMap = new HashMap<String, Boolean>();
					for(String key : legalMap.keySet()){
						 tempMap.put(StringEscapeUtils.escapeXml(StringEscapeUtils.unescapeXml(key)), legalMap.get(key));
					}
					legalMap = tempMap;
					statementsMap.put(PriorFileDocument.LEGAL_DESC, legalMap);
				}
				if (excMap != null){
					HashMap<String, Boolean> tempMap = new HashMap<String, Boolean>();
					for(String key : excMap.keySet()){
						 tempMap.put(StringEscapeUtils.escapeXml(StringEscapeUtils.unescapeXml(key)), excMap.get(key));
					}
					excMap = tempMap;
					statementsMap.put(PriorFileDocument.EXCEPTIONS, excMap);
				}
				return TemplateUtils.saveTemplate(this.getThreadLocalRequest().getSession(), this.getThreadLocalResponse(),
						searchId, userId, templateName, (long) templateId, templateContent, true, buttonLabel, statementsMap);
			}
			return false;
		} catch (Exception e) {
			throw new TemplateSearchPermisionException(e.getMessage());
		}
	}

	@Override
	public Boolean deleteGeneratedTemplate(long searchId,long userId,String path, int templateId, boolean force) throws TemplateSearchPermisionException{
			try{return TemplateUtils.deleteGeneratedTemplate(this.getThreadLocalRequest(), searchId, userId, path, (long)templateId, force);}
			catch(Exception e){ throw new TemplateSearchPermisionException( e.getMessage() );}
	}
	
	@Override
	public int getMaxInactiveIntervalForTemplateEditing() {
		return 900;
	}
	
	@Override
	public TemplateInfoResultG getTemplateInfo(long searchId, long userId, String templateName) throws TemplateSearchPermisionException{
		TemplateUtils.TestAvailable  test = TemplateUtils.isSearchAvailable(searchId,userId);
		if (!test.available) {
			throw new TemplateSearchPermisionException(test.errorBody);
		}
		CompileTemplateResult compileTemplateResult = TemplateUtils.tempTemplateInfo.get(searchId);
		if (compileTemplateResult ==null){
			try {
				compileTemplateResult = TsdIndexPageServer.compileTemplateImpl(searchId,userId,templateName, false, null);
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		TemplateUtils.tempTemplateInfo.remove(searchId);		
		TemplateInfoResultG templateInfoResultG = null;
		if(compileTemplateResult != null) {
			templateInfoResultG = compileTemplateResult.toTemplateInfoResultG();
			templateInfoResultG.setServerURL(ATS_CURRENT_SERVER_LINK_FOR_DOC_RETREIVER);
		}
		return templateInfoResultG;
	}
	
	@Override
	public Boolean makeTempBackup(long searchId, long userId, String templateName, int templateId,
			String templateContent) {
		try {
			this.getThreadLocalRequest()
					.getSession()
					.setAttribute("templateBackup" + searchId + File.separator + userId + File.separator + templateName,
							new Object[] { Long.valueOf(searchId), Long.valueOf(userId), templateName, templateId, templateContent });
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public Boolean restoreTempBackup( long searchId, long userId, String templateName, int templateId ) {
		try {
			if( getState(searchId,templateName,templateId).contains("not modified") ){
				return true; 
			}
			Object[] saved = (Object[]) this.getThreadLocalRequest().getSession().getAttribute("templateBackup"+searchId+File.separator+userId+File.separator+templateName);
			TemplateUtils.saveTemplate(
					this.getThreadLocalRequest().getSession(), 
					this.getThreadLocalResponse(),
					(Long)saved[0],(Long)saved[1],(String)saved[2],(Integer)saved[3],(String) saved[4],
					false, "");
		}catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	@Override
	public void logMessage( long searchId  , String message) {
		SearchLogger.info(message + SearchLogger.getTimeStamp(searchId) +".<BR><div>", searchId);
	}

	/**
	 * 
	 * @param searchId
	 * @param userId
	 * @param getBoilerPlateMapWithDocID
	 * @param getChachedCopy - if true it returns a cached copy of the map if available on the search
	 * @param bpCodeToFill 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getBoilerPlatesMap(long searchId, long userId, boolean getBoilerPlateMapWithDocID, boolean getChachedCopy, String bpCodeToFill, String documentIdToFill) {
		Search globalSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();

		if (getChachedCopy) {
			if (globalSearch != null) {
				if (getBoilerPlateMapWithDocID) {
					Object o = globalSearch.getAdditionalInfo(TemplateUtils.BOILER_MAP_WITH_DOCID);
					if (o != null && o instanceof Map) {
						return (Map<String, String>) o;
					}
				} else {
					Object o = globalSearch.getAdditionalInfo(TemplateUtils.BOILER_MAP);
					if (o != null && o instanceof Map) {
						return (Map<String, String>) o;
					}
				}
			}
		}

		// cache this map
		Map<String, String> map = getBoilerPlatesMap(searchId, userId, null, getBoilerPlateMapWithDocID, globalSearch.getProductId(), bpCodeToFill, documentIdToFill);

		if (globalSearch != null) {
			if (getBoilerPlateMapWithDocID) {
				globalSearch.setAdditionalInfo(TemplateUtils.BOILER_MAP_WITH_DOCID, map);
			} else {
				globalSearch.setAdditionalInfo(TemplateUtils.BOILER_MAP, map);
			}
		}

		return map;
	}
	
	
	/**
	 * 
	 * @param searchId
	 * @param userId
	 * @param getChachedCopy
	 * @param bpCodeToFill if filled only this code will be returned
	 * @param documentIdToFill the id of the document for which bpCodeToFill was requested
	 * @return Map of {@link CodeBookStatementObject}
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, CodeBookStatementObject> getBoilerPlateStatementObjectMap(long searchId, long userId, boolean getChachedCopy, String bpCodeToFill, String documentIdToFill) {
		
		Search globalSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		if(getChachedCopy){
			if(globalSearch != null){
				Object o = globalSearch.getAdditionalInfo(TemplateUtils.BOILER_MAP_WITH_DOCID_BPSOBJECT);
				if(o!=null && o instanceof Map){
					return (Map<String, CodeBookStatementObject>)o;
				}
			} 
		}
		
		Map<String, CodeBookStatementObject> map = new CaseInsensitiveMap<String, CodeBookStatementObject>();

		//get a boiler plate map with boiler documents separated by docId
		Map<String, String> mapString = getBoilerPlatesMap(searchId, userId, true, getChachedCopy, bpCodeToFill, documentIdToFill);

		for (Entry<String, String> e : mapString.entrySet()) {
			map.put(e.getKey(), new CodeBookStatementObject(e.getValue()));
		}
		
		if(globalSearch != null){
			globalSearch.setAdditionalInfo(TemplateUtils.BOILER_MAP_WITH_DOCID_BPSOBJECT, map);
		}

		return map;
	}
	
	public static Map<String,String> getBoilerPlatesMap(long searchId, long userId, HttpServletRequest request){
		return getBoilerPlatesMap(searchId, userId, request, false, null, null, null); 
	}
	
	/**
	 * 
	 * @param searchId
	 * @param userId
	 * @param request
	 * @param getBoilerPlateMapWithDocID
	 * @param productId - if not null it will return the boilerPlatesMap in respect with the productID of the boiler code
	 * @param bpCodeToFill 
	 * @return
	 */
	public static Map<String, String> getBoilerPlatesMap(long searchId, long userId, HttpServletRequest request, boolean getBoilerPlateMapWithDocID,
			Integer productId, String bpCodeToFill, String documentIdToFill) {
		
		String label = "";
		String content = "";
		String fileContent;
		Matcher mat = null;
		Map<String, String> hashMap = new HashMap<String, String>();
		HashMap<String, String> hashMapLowercase = new HashMap<String, String>();
		hashMap.put(" None ", "");
 		

		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();

		UserAttributes agent = search.getAgent();
		if(agent != null) {
			List<CommunityTemplatesMapper> templ = UserUtils.getUserTemplates(agent.getID().longValue(), -1, UserUtils.FILTER_BOILER_PLATES_ONLY,
					search.getProductId(), false);
			
			if(templ.size() > 0) {
				
				try {
					CompileTemplateResult compileTemplateResult = TemplateUtils.compileTemplate(
							searchId, userId, templ.get(0).getPath(), false, null, null, true, null, getBoilerPlateMapWithDocID, bpCodeToFill, documentIdToFill);
					fileContent = compileTemplateResult.getTemplateContent();
		
					boolean hasMoreMatches = fileContent != null;
					
					while (hasMoreMatches) {
						mat = AddDocsTemplates.PATTERN_BPCODE_PRODUCT.matcher(fileContent);
						if (mat.find()) {
							boolean addBoilerContent = false;
		
							if (productId == null) {
								label = mat.group(0);
								label = label.split(LABEL_NAME_DELIM)[0];
								addBoilerContent = true;
							} else {
								label = mat.group(1);
								// take just boilers for product id or default TemplateUtils.BOILER_MAP_DEFAULT_PRODUCT
								if (label.endsWith("_" + productId)
										|| (label.endsWith("_" + TemplateUtils.BOILER_MAP_DEFAULT_PRODUCT) && !hashMap.containsKey(label.split(LABEL_NAME_DELIM)[0]))) {
									label = label.split(LABEL_NAME_DELIM)[0];
									addBoilerContent = true;
								}
							}
		
							fileContent = fileContent.substring(mat.end());
							mat = AddDocsTemplates.PATTERN_BPCODE_PRODUCT.matcher(fileContent);
							if(mat.find()) {
								content = fileContent.substring(0, mat.start() - 1);
							} else {
								content = fileContent;
							}
//							content = mat.find() ? fileContent.substring(0, mat.start()) : fileContent;
		
							if (addBoilerContent) {
								hashMap.put(label, content/*.trim()*/);
								hashMapLowercase.put(label.toLowerCase(), content/*.trim()*/);
							}
						} else {
							hasMoreMatches = false;
						}
					}
					if (request != null) {
						request.getSession().setAttribute("boilerPlate" + searchId, hashMapLowercase);
					}
				} catch (Exception tsp) {
					logger.error("Caught a Template Search Permission Exception.Please make a check !!!", tsp);
				}
			}
				
		}

		return hashMap;
	}
	
	 @Override
	 public List<String> getLabelList(long searchId,long userId){
		 
//		 Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		 List<String> labelList = new ArrayList<String>();
		 labelList.add(" None ");
		 
//		 labelList.addAll(search.getCachedCodeList());
		 
		 Map<String,String> boiler = getBoilerPlatesMap(searchId, userId, this.getThreadLocalRequest());
		 for(Entry<String,String> e : boiler.entrySet()) {
			 labelList.add(e.getKey());
		 }
		 
		 return labelList;
	}
	
	/** 
	 * Function that returns the compiled information from the BoilerPlates, corresponding to the given label
	 * @param instrumentList List<InstrumentStructForUndefined> - instrument list for UNDEFINED elements
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public String getElement(long searchId,long userId,String label,List<InstrumentStructForUndefined> instrumentList){
		 HashMap<String, String> 	boilerPlateMap 	=	null;
		 String 	contents 		= 	""	;
		 HttpServletRequest request			= null;
		 Matcher undefinedDocumentsMatcher 	= null;
		 
		 try { 
			 if((request = this.getThreadLocalRequest())==null) return "";
			 if((boilerPlateMap =  (HashMap)request.getSession().getAttribute("boilerPlate"+searchId)) ==null){
				 return "";
		 	 }
			 if(boilerPlateMap.get(label.toLowerCase())==null) {
				 return "";
			 }
			 contents = (String)(boilerPlateMap.get(label.toLowerCase()));
			 /* if an undefined tag is found, we first compile it, using the instrumentList, and then return it to the client */
			 undefinedDocumentsMatcher = AddDocsTemplates.undefinedTag.matcher(contents);
			 if(undefinedDocumentsMatcher.find()) {
				 if( instrumentList==null || instrumentList.isEmpty() )  {
					 return "";
				 }
				 Vector<String> templatesNames = TemplateUtils.getTemplateNames(searchId, this.getThreadLocalRequest(), true);
				try {// get the boiler templates filename					 
					 
					for (String templateName : templatesNames) {
						if (templateName.contains(TemplatesInitUtils.TEMPLATE_BP_CONTAINS) || templateName.contains(TemplatesInitUtils.TEMPLATE_CB_CONTAINS)) {
							contents = (TemplateUtils.compileTemplate(searchId, userId, templateName, false, contents, instrumentList, true, null)).getTemplateContent().trim();
							break;	//not more templates in the list
						}
					}
					
				} catch (TemplateSearchPermisionException tsp) {
					logger.error("Caught a Template Search Permission Exception.Please make a check !!!");
					contents = "";
				}
			 }
		 }catch(Exception e) {
			 e.printStackTrace();
		 }
		 return contents;
	}

	@Override
	public String previewTemplate(String templateName, String templateContent, long searchId) {
		return previewTemplateImpl(templateName, templateContent, searchId);
	}
	
	public static String previewTemplateImpl(String templateName, String templateContent, long searchId) {
		
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		String preview = " ";
		boolean isPxt = templateName.toLowerCase().endsWith(".pxt");
		boolean isTxt = templateName.toLowerCase().endsWith(".txt");
		
		if(isPxt) {
			Matcher previewMatcher = previewPatternPxt.matcher(templateContent);
			while(previewMatcher.find()) {
				String name = previewMatcher.group(1);
				String value = previewMatcher.group(2);
				
				preview +=  name + "<br><br>";
				preview += "<blockquote><div><samp>"+  value.replaceAll("\n", "<br>")  + "</pre></samp></blockquote><br>";
			}
			return preview;
		}
		else if(isTxt) { 
			return templateContent;
		}
		
		//isAts
		templateContent = templateContent.replaceAll("\\[(<|>)\\]","$1" ).replaceAll("\n\n+", "\n\n");
		Matcher previewMatcher = previewPatternAts.matcher(templateContent);
		while(previewMatcher.find()) {
			try {
				String name = "", value ="";
				if(previewMatcher.group(5)!=null) {
					/* <exemplu name1="text1" name2="text2"/> */
					Matcher previewAttributesMatcher = previewAttributesPattern.matcher(previewMatcher.group(5));
					while(previewAttributesMatcher.find()) {
						name = previewAttributesMatcher.group(1);
						value = previewAttributesMatcher.group(2); 
						value = StringUtils.HTMLEntityEncode(ro.cst.tsearch.templates.edit.client.TemplateUtils.revertStringFromHtmlEncoding(value));
						value = value.replaceAll("&#"+((int)'\n')+";", "<br>");
						value = value.replaceAll("\n", "<br>");
						
						value = value.replaceAll("(?i)(https*&#58;)&#47;&#47;","$1&#92;&#92;");
						value = value.replaceAll("&#47;&#47;","<br>");
						value = value.replaceAll("(?i)(https*&#58;)&#92;&#92;","$1&#47;&#47;");
												
						preview +=  name + "<br><br>";
						preview += "<blockquote><div><samp>"+ value + "</samp></div></blockquote><br>";
					}
				} else {
					/*      <name3>Text3</name3>			  */
					name = previewMatcher.group(2);
					value = previewMatcher.group(4);
					value = StringUtils.HTMLEntityEncode(ro.cst.tsearch.templates.edit.client.TemplateUtils.revertStringFromHtmlEncoding(value));
					value = value.replaceAll("&#"+((int)'\n')+";", "<br>");
					value = value.replaceAll("\n", "<br>");
					
					value = value.replaceAll("(?i)(https*&#58;)&#47;&#47;","$1&#92;&#92;");
					value = value.replaceAll("&#47;&#47;","<br>");
					value = value.replaceAll("(?i)(https*&#58;)&#92;&#92;","$1&#47;&#47;");
										
					preview +=  name + "<br><br>";;
					preview += "<blockquote><div><samp>"+  value+ "</samp></div></blockquote><br>";
				}
				
			}catch(Exception e) {
				e.printStackTrace();
			} 
		}
		
		
		
		
		/* They want to see links to the images in the preview window :L */
		preview = preview.replace("&#60;a href&#61;&#34;http&#58;&#47;&#47;", "<a href=\"http://")
						 .replace("&#34;&#62;","\">")
						 .replace("&#60;&#47;a&#62;","</a>");
		
		Pattern pattern = Pattern.compile("<a [^>]+>");
		Matcher matcher = pattern.matcher(preview);
		StringBuffer sb = new StringBuffer();
		while(matcher.find()) {
			matcher.appendReplacement(sb, matcher.group().replace("&#34;", "\"").replace(" title&#61;", " title="));
		}
		matcher.appendTail(sb);
		
		preview = sb.toString();
		
		
		preview = TemplateBuilder.replaceImageLinksInTemplate(preview.replace("http&#58;&#47;&#47;", "http://"),search,true,false);
		preview = preview.replaceAll("(?i)&#60;br&#62;", "<br>").replace("(?i)&#60;br/&#62;", "<br/>");
		preview = preview.replaceAll("(?i)&#38;nbsp&#59;","&nbsp;");
		return preview;
	}
	
	
	public static String[] getLinkForLastCopiedChapter(long searchId,boolean forcePdf,HttpServletRequest req, HttpServletResponse resp) throws Exception{
		try{
			Search global = TsdIndexPageServer.testAvailability( searchId, req, resp );
			HttpSession session = req.getSession();
			ClipboardAtsI clipboard= null;
			
			synchronized(session){
				String str = (String)session.getAttribute("COPYDOC");
				str = org.apache.commons.lang.StringUtils.defaultString(str);
				if("true".equalsIgnoreCase(str)){
					throw new RuntimeException("COPY in progress. Please try again later !");
				}
				clipboard = (ClipboardAtsI)session.getAttribute(SessionParams.CLIPBOARD_ATS);
			}
			
			String documentId = clipboard.getCopiedDocument(searchId);
			if(!org.apache.commons.lang.StringUtils.isBlank(documentId)){
				DocumentsManagerI docManager = global.getDocManager();
				try{
					docManager.getAccess();
					DocumentI doc = docManager.getDocument(documentId);
					if(doc!=null)
						TsdIndexPageServer.logAction("Paste Link ", searchId, doc);
					return constructLink(doc, global,  forcePdf);
				}
				finally{
					docManager.releaseAccess();
				}
			}else{
				throw new RuntimeException("Please first use \"Copy Link\" from TSRIndex");
			}
		}catch(Exception e){
			SearchLogger.info("</div><div>Paste link failed; "+e.getMessage()+" "+SearchLogger.getTimeStamp(searchId)+"<BR></div>",searchId) ;
			throw e;
		}
	}
	
	@Override
	public String[] getLinkForLastCopiedChapter(long searchId, boolean forcePdf) throws Exception{
		return getLinkForLastCopiedChapter(searchId,forcePdf,getThreadLocalRequest(),getThreadLocalResponse());
	}
	
	private static String buildDocInfo(DocumentI doc){
		String ret = "";
		
		String book = doc.getBook();
		String page = doc.getPage();
		String instno = doc.getInstno();
		String docno = doc.getDocno();
		String type = doc.getDocType();
		String year = doc.getYear()+"";
		
		if( !StringUtils.isEmpty(book) && !StringUtils.isEmpty(page)){
			ret+= " Book: "+ book+" Page: "+page;
		}
		if( !StringUtils.isEmpty(instno) ){
			ret+= " Instrument: "+instno;
		}else  if(!StringUtils.isEmpty(docno)){
			ret+= " Instrument: "+docno;
		} 
		
		ret += " Type: " + type;
		
		if(doc.hasYear()){
			ret += " Year: " + year;
		}
		
		return ret;
	}
	
	public static String[] constructLink(DocumentI doc, Search search, boolean forcePDF){
		String []ret = {
				forcePDF?
						DocumentUtils.createImageLinkForImageAsPDF(doc, search) 
						:
						DocumentUtils.createImageLinkWithDummyParameter(doc, search), buildDocInfo(doc)
						};
		return ret;
	}

	@Override
	public Boolean uploadToSSf(long searchId, long userId, String ssfDocumentId) throws Exception{
		
		TemplateUtils.TestAvailable test = TemplateUtils.isSearchAvailable( searchId, userId );
		if (!test.available) {
			throw new RuntimeException(test.errorBody);
		}
		
		int stateFips = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateFips();
		int countyFips = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getCountyFips();
	    
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		DocumentsManagerI manager = global.getDocManager();
		
		SSFPriorFileDocument ssfDoc = null;
		try{
			manager.getAccess();
			DocumentI doc = manager.getDocument(ssfDocumentId);
			if(doc!=null && doc instanceof SSFPriorFileDocument){
				ssfDoc =(SSFPriorFileDocument)doc;
			}
		}finally{
			manager.releaseAccess();
		}
		
		if(ssfDoc ==null){
			throw new RuntimeException("Could not find the document for ID="+ssfDocumentId);
		}
		
		DocAdminConn docAdminConn = new DocAdminConn(global.getCommId());
    	int	imageTransactionId = -1;
        String imagesLink = "";
        imageTransactionId = global.getImageTransactionId();
        
        if(imageTransactionId<=0){
	        try{
		    	if(ServerConfig.isStewartSsfImagePostEnabled() /*&& !global.hasTransactionId()*/ ){
		    		TransactionResponse transactioResp = docAdminConn.reserveUniqueTransactionId(String.valueOf(searchId),stateFips,countyFips);
		    		imageTransactionId = transactioResp.id;
		    		imagesLink = transactioResp.link;
		    		global.setImageTransactionId(imageTransactionId);
		    		global.setAllImagesSsfLink(imagesLink);
		    	}
	    	}catch(Exception e){
	    		e.printStackTrace();
	    	}
        }
    	
    	if(imageTransactionId<=0){
    		throw new RuntimeException("Could not reserve SSF transaction ID");
    	}
		
    	String ssfIndesStr = DBManager.getSfDocumentIndex(ssfDoc.getSsfIdexId());
    	
    	String str = docAdminConn.updateStarterMlFile(ssfIndesStr, imageTransactionId, ssfDoc, true);
    	
    	if(!StringUtils.isEmpty(str)){
    		throw new RuntimeException(str);
    	}
    	
		return true;
	}
	
}
