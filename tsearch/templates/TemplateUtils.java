package ro.cst.tsearch.templates;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.CommunityTemplatesMapper;
import ro.cst.tsearch.performanceMonitor.LoggedUsersMonitor;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servers.types.GenericATS;
import ro.cst.tsearch.servers.types.TSServerSSF;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.TemplatesServlet;
import ro.cst.tsearch.servlet.UserValidation;
import ro.cst.tsearch.templates.edit.client.InstrumentStructForUndefined;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.titledocument.abstracts.FidelityTSD;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.BoilerPlateObject;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.PriorFileAtsDocument;
import com.stewart.ats.base.document.PriorFileAtsDocumentI;
import com.stewart.ats.base.document.PriorFileDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.SSFPriorFileDocument;
import com.stewart.ats.base.misc.SelectableStatement;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.template.TemplateInfo;
import com.stewart.ats.tsrindex.client.template.TemplatesInitUtils;

public class TemplateUtils {
	
	private static final Logger logger = Logger.getLogger(TemplateUtils.class);

	public static final String CLASSES_FOLDER = BaseServlet.REAL_PATH + File.separator + "WEB-INF" + File.separator + "classes" + File.separator;
	public static final String RESOURCE_SF_FOLDER = (CLASSES_FOLDER	+ "resource" + File.separator + "SF" + File.separator ).replaceAll("//", "/");

	public static final String BASE_FILE_TEMPLATE_NAME = 	"baseFileTemplate.xml";
	
	public static final String BASE_FILE_TEMPLATE_PATH = 	RESOURCE_SF_FOLDER + BASE_FILE_TEMPLATE_NAME;
	
	public static final String TSD_TEMPLATE_XML_NAME 	= 	"TSDtemplate.xml";
	public static final String TSD_TEMPLATE_DTD_NAME 	= 	"TSDtemplate.dtd";
	public static final String TSD_TEMPLATE_CODE 		= 	"";

	public static final Hashtable<Long,String>		tempTSDContent		= new Hashtable<Long,String>();
	public static final Hashtable<Long, CompileTemplateResult>	tempTemplateInfo		= new Hashtable<Long, CompileTemplateResult>();
	
	//replacement to the ATS template right before saving of the template
	public static final  java.util.Hashtable<String, String> replacements = new java.util.Hashtable<String, String>();
	static {
		replacements.put("\u2018", "&apos;"); // 2018 == (hex)'‘'
		replacements.put("\u2019", "&apos;"); // 2019 == (hex)'’'
		replacements.put("\u201C", "&quot;"); //"&#8220;"  Left Double Quotation Mark
		replacements.put("\u201D", "&quot;"); //"&#8221;"  Right Double Quotation Mark		
		replacements.put("&ldquo;", "&quot;"); //"&#8220;"  Left Double Quotation Mark
		replacements.put("&rdquo;", "&quot;"); //"&#8221;"  Right Double Quotation Mark
	}

	public static class TestAvailable {

		public final boolean available;
		public final String errorBody;

		public TestAvailable(boolean available, String body) {
			super();
			this.available = available;
			errorBody = body;
		}
	}

	public static TestAvailable isSearchAvailable(long searchId, long userId) {
		DBManager.SearchAvailabitily searchAvailable = DBManager
				.checkAvailability(searchId, userId, DBManager.CHECK_OWNER, false);
		String errorBody = "";
		if (searchAvailable.status != DBManager.SEARCH_AVAILABLE) {
			errorBody = searchAvailable.getErrorMessage();
			return new TemplateUtils.TestAvailable(false, errorBody);
		}
		return new TemplateUtils.TestAvailable(true, errorBody);
	}

	public static Vector<String> getTemplateNames(long searchId, HttpServletRequest request, boolean giveDASLTemplates) {
		return getTemplateNames(searchId, giveDASLTemplates);
	}
	
	public static Vector<String> getTemplateNames(long searchId, boolean giveDASLTemplates) {
		Vector<String> v = new Vector<String>();
		try {
			
			Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			if(search.getAgent() == null) {
				return v;
			}
			v = getTemplateNames(search.getAgent().getID().longValue(), search.getProductId(), giveDASLTemplates);
		}
		catch(Exception e) {
			e.printStackTrace();		
		}
		return v;
	}
	
	/**
	 * Get templates based on an agent and a product type, including or not DASL templates<br>
	 * Never throws an exception
	 * @param agentId the agent that holds the templates
	 * @param productId the product id for which the template are enabled
	 * @param giveDASLTemplates a flag used to include or not DASL templates
	 * @return a list of template names (string) or empty
	 */
	public static Vector<String> getTemplateNames(long agentId, int productId, boolean giveDASLTemplates) {
		Vector<String> v = new Vector<String>();
		try {

			List<CommunityTemplatesMapper> userTemplates = UserUtils.getUserTemplates(agentId, productId);

			for (CommunityTemplatesMapper map : userTemplates) {
				String path = (String) map.getPath();
				if (giveDASLTemplates || !path.startsWith(ro.cst.tsearch.templates.edit.client.TemplateUtils.DASL_CODE)) {
					v.add(path);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return v;
	}
	
	public static List<TemplateInfo> getTemplatesInfo(long searchId, boolean giveDASLTemplates) {
		
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		List<TemplateInfo>ret = new ArrayList<TemplateInfo>();
		
		try {
			if(search.getAgent()!=null) {
				List<CommunityTemplatesMapper> userTemplates = UserUtils.getUserTemplates(
						search.getAgent().getID().longValue(), 
						-1, -1 , search.getProductId(), false);
			
				for(CommunityTemplatesMapper map:userTemplates){
					String path = (String)map.getPath();
					
					String  templateNewPath = search.getSearchDir()+ File.separator + Search.TEMP_DIR_FOR_TEMPLATES + File.separator + path;
					templateNewPath = templateNewPath.replaceAll("//", "/");
					
					TemplateInfo ti = new TemplateInfo(map.getId(),map.getName(),map.getPath(),getState(searchId, map.getPath(), map.getId()),map.getShortName(), templateNewPath);
					if(giveDASLTemplates || !path.startsWith(ro.cst.tsearch.templates.edit.client.TemplateUtils.DASL_CODE)) {
						ret.add(ti);
					}
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return ret;
	}
	
	public static Boolean deleteGeneratedTemplate(HttpServletRequest request, long searchId , long userId , String path , long templateId, boolean force) throws Exception {
		Search 	globalSearch	= InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		boolean isTsd 			= false;
		
		
			if (TSD_TEMPLATE_CODE.equals(path)) {
				List<CommunityTemplatesMapper> userTemplates = UserUtils.getUserTemplates(
						globalSearch.getAgent().getID().longValue(),
						-1,
						UserUtils.FILTER_TSD_ONLY,
						globalSearch.getProductId());
				isTsd = true;
				if (userTemplates != null && !userTemplates.isEmpty()) {
					CommunityTemplatesMapper template = userTemplates.get(0);
					Object oby = null;
					if (template != null) {
						oby = template.getPath();
						templateId = template.getId();
					}
					if (oby != null)
						path = (String) oby;
				}
				
			}
		
		
		TemplateUtils.TestAvailable test = TemplateUtils.isSearchAvailable( searchId, userId );
		if (!test.available) {
			throw new Exception(test.errorBody);
		}
		
			if (isTsd) {
				path = globalSearch.getSearchDir() + File.separator + Search.TEMP_DIR_FOR_TEMPLATES + File.separator + path;
			}
			
			boolean isModified = false;
			synchronized (globalSearch.getGeneratedTemp()) {
				HashMap<String,String> map = globalSearch.getGeneratedTemp();
				isModified = map.containsValue(path);	
			}
			
			path = path.replaceAll("//", "/");
			if (!force) {
				if (!isModified) {
					File file = new File(path);
					if (file.exists() && file.isFile()){
						return Boolean.valueOf(file.delete());
					}
				}
			} else {
				int poz = path.lastIndexOf(File.separator);
				if ( poz<0 ) {
					poz = 0;
				}
				if ( poz < path.length()-1 ) {
					String key = path.substring(poz + 1);
					setTemplateNotModified(searchId,key,templateId);
				}
				File file = new File(path);
				if ( file.exists() && file.isFile() ){
					return Boolean.valueOf(file.delete());
				}
			}
		
		return Boolean.FALSE;
	}
	
	public static Boolean saveTemplate(HttpSession session, HttpServletResponse response, long searchId, long userId, String templateName, long templateId, String templateContent, String buttonLabel) throws Exception {
		return saveTemplate(session, response, searchId, userId, templateName, templateId, templateContent,true, buttonLabel);
	} 
	
	public static String cleanAfterLinkEditor(String html){
		if(html==null){
			return "";
		} 
		return html;
		/*html = html.replaceAll("(?i)&lt;a href=&quot;", "<a href=\"");
		html = html.replaceAll("(?i)&lt;/a&gt;", "</a>");
		html = html.replaceAll("(?i)&lt;/p&gt;", "</p>");
		html = html.replaceAll("(?i)&lt;p&gt;", "<p>");
		html = html.replaceAll("(?i)&quot;&gt;", "\">");
		html = html.replaceAll("(?i)&amp;nbsp;", "&nbsp;");
		html = html.replaceAll("(?i)&lt;br/&gt;", "<br/>");
		html = html.replaceAll("(?i)&lt;br&gt;", "<br>");
		html = html.replaceAll("(?i)&lt;/div&gt;", "</div>");
		html = html.replaceAll("(?i)&lt;div&gt;", "<div>");
		html = html.replaceAll("(?i)&lt;/span&gt;", "</span>");
		html = html.replaceAll("(?i)&lt;span&gt;", "<span>");
		return html;*/
	}
	
	public static Boolean saveTemplate(HttpSession session, HttpServletResponse response, long searchId, long userId, String templateName, 
			long templateId, String templateContent, boolean log, String buttonLabel) throws Exception {
		return saveTemplate(session, response, searchId, userId, templateName, templateId, templateContent, log, buttonLabel, null);
	}
	
	public static Boolean saveTemplate(HttpSession session, HttpServletResponse response, long searchId, long userId, String templateName, 
			long templateId, String templateContent, boolean log, String buttonLabel, HashMap<String, HashMap<String, Boolean>> statementsMap) throws Exception {
		
		Search globalSearch 	= InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();;
		String templateNewPath 	= "";
		String extension 		= "";
		Long start 				= System.currentTimeMillis();
		
		DocumentsManagerI manager = globalSearch.getDocManager();
		
		if(templateName.matches("[0-9]+_[0-9]+")){
			try{
				manager.getAccess();
				DocumentI docI = manager.getDocument(templateName);
				if(docI!=null && "SF".equalsIgnoreCase(docI.getDataSource())){
					return saveStarterFile(searchId, userId, templateName, templateId, templateContent, log, statementsMap);
				} else if(docI!=null && "ATS".equalsIgnoreCase(docI.getDataSource())){
					return saveATSFile(searchId, userId, templateName, templateId, templateContent, log, statementsMap);
				}
			}finally{
				manager.releaseAccess();
			}
		}
		
		if (TSD_TEMPLATE_CODE.equals(templateName)) {
			
			if(templateId > 0) {
				CommunityTemplatesMapper templateById = UserUtils.getTemplateById(templateId);
				if(templateById != null && templateById.getPath() != null) {
					templateName = templateById.getPath();
				}
			} else {
			
				List<CommunityTemplatesMapper> userTemplates = UserUtils.getUserTemplates(globalSearch.getAgent().getID().longValue(), globalSearch.getProductId());
				if (userTemplates != null) {
					CommunityTemplatesMapper template = FidelityTSD.getTSDTemplate(userTemplates);
					if (template != null) {
						Object oby = null;
						if ((oby=template.getPath()) != null){
							templateName = (String) oby;
						}
						templateId = template.getId();
					}
				}
			}
		}
		
		if (StringUtils.isEmpty(templateName)) {
			throw new Exception("Empty template name");
		}		
		try {extension = templateName.substring(templateName.indexOf('.') + 1);} catch (Exception e) {}

		if(!("html".equalsIgnoreCase(extension)||"htm".equalsIgnoreCase(extension))){
			templateContent = cleanAfterLinkEditor(templateContent);
		}
		if (extension.equalsIgnoreCase("ats")) {
			templateContent = AddDocsTemplates.corectStringForAIM(templateContent);
			for (String key : replacements.keySet()) {
				templateContent = templateContent.replaceAll(key, replacements.get(key));
			}
		}
		templateContent = templateContent.replaceAll("([^\r])\n", "$1\r\n");
		TemplateUtils.TestAvailable test = isSearchAvailable(searchId, userId);
		if (!test.available) {
			throw new Exception(test.errorBody);
		}
		
			String tempDirectory = globalSearch.getSearchDir() + File.separator + Search.TEMP_DIR_FOR_TEMPLATES.replaceAll("//", "/");
			try {
				File tmpFile = new File(tempDirectory);
				if (!tmpFile.exists()) {
					if(!tmpFile.mkdirs()){
						throw new Exception(" Could not create dirs");
					}
				}
			} catch (Exception e) { e.printStackTrace();}
			templateNewPath = tempDirectory + File.separator + templateName;
			templateNewPath = templateNewPath.replaceAll("//", "/").replaceAll("//", "/");
		
			if(StringUtils.toFile(templateNewPath, templateContent)){
				setTemplateModified(searchId, templateId+"", templateNewPath);
				
				// Task 7724
				String policyName = templateName;
				try {
					policyName = (String) UserUtils.getTemplate((int)templateId).get(UserUtils.TEMPLATE_NAME);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
				String withButton = "";
				if ("Apply".equals(buttonLabel)) {
					withButton = "(using Apply button) ";
				} else if ("Submit".equals(buttonLabel)) {
					withButton = "(using Submit button) ";
				}
				
				if(log) {
					SearchLogger.info("</div><div>The <b>Template " + 
							"&lt;" + policyName +
							"&gt;</b> was saved " + withButton
							+ SearchLogger.getTimeStamp(globalSearch.getSearchID())
							+".<BR><div>", globalSearch.getSearchID());
				}
			}
			else{
				throw new Exception("Can't save the file");
			}
		
			// just to avoid buttons changing states too fast
			try {
				long elapsed = System.currentTimeMillis() - start;
				if (elapsed < 2000){
					Thread.sleep(2000 - elapsed);
				}
			}catch(InterruptedException e) {}
	
			try {
				 
				session.setAttribute( UserValidation.LAST_ACCESS_PARAM, Long.valueOf(System.currentTimeMillis()) );
			
				LoggedUsersMonitor.getInstance().setMaxInactiveInterval( session.getMaxInactiveInterval() );
				User currentUser = (User) session.getAttribute( SessionParams.CURRENT_USER );
				
				DBManager.saveCurrentSearch( currentUser, globalSearch, Search.SEARCH_TSR_NOT_CREATED, null);
				
				if (searchId > 0) {
					InstanceManager.getManager().getCurrentInstance(searchId).setup( currentUser, searchId, response, session );
				}
				currentUser.updateLastAccess(null);
				
				DBManager.zipAndSaveSearchToDB( globalSearch );
				
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		return Boolean.TRUE;
	}
	

	private static Boolean saveATSFile(long searchId, long userId,
			String templateName, long templateId, String templateContent,
			boolean log, HashMap<String, HashMap<String, Boolean>> statementsMap) {
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		DocumentsManagerI manager = search.getDocManager();
		 
		try{
			manager.getAccess();
			
			DocumentI docI = manager.getDocument(templateName);
			
			if(docI!=null && docI instanceof PriorFileAtsDocument){
				PriorFileAtsDocumentI atsDoc= (PriorFileAtsDocumentI)docI ;
				
				GenericATS.parseStatements(atsDoc, templateContent, statementsMap, searchId);
				
				DBManager.updateDocumentIndex(atsDoc.getIndexId(), templateContent, search);
			}
			
		}finally{
			manager.releaseAccess();
		}
		
		return true;
	}

	private static Boolean saveStarterFile(long searchId, long userId, String templateName, long templateId, 
			String templateContent, boolean log, HashMap<String, HashMap<String, Boolean>> statementsMap) {
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		DocumentsManagerI manager = search.getDocManager();
		 
		try{
			manager.getAccess();
			
			DocumentI docI = manager.getDocument(templateName);
			
			if(docI!=null && docI instanceof SSFPriorFileDocument){
				SSFPriorFileDocument ssfDoc= (SSFPriorFileDocument)docI ;
				templateContent = templateContent.replace(" <!--", "<!--" ).replaceAll("-->[ \t\r\n]+", "-->");
				templateContent = templateContent.replace("<!--", "");
				templateContent = templateContent.replace("-->", "");
				
				templateContent = templateContent.replaceAll(">([^>]+?)[\r\n\t ]+<", ">$1<");
				
				String statementsText = templateContent.replaceFirst("(?is).*(<elements>.*</elements>).*", "$1");
				TSServerSSF.parseStatements((SSFPriorFileDocument)docI, statementsText, statementsMap);
				
				DBManager.updateSfDocumentIndex(ssfDoc.getSsfIdexId(), templateContent, search);
			}
			
		}finally{
			manager.releaseAccess();
		}
		
		return true;
	}

	public static  String  getState(HttpServletRequest 	request, long searchId,String templateName, long templateId){
		return (isTemplateModified(searchId,templateName,templateId)!=null) ? "modified" : "not modified";
	}
	
	public static  String  getState(HttpServletRequest 	request, long searchId,String templateName){
		return getState(searchId, templateName);
	}
		
	public static  String  getState(long searchId, String templateName){
		return isTemplateModified(searchId,templateName) ? "modified" : "not modified";
	}
	
	/**
	 * Gets the state of a template.
	 * We currently use the templateId to determine the state, 
	 * but we still check with the templateName ( for older searches )
	 * @param request
	 * @param searchId
	 * @param templateName
	 * @param templateId
	 * @return
	 */
	public static String getState(long searchId, String templateName, long templateId) {
		return (isTemplateModified(searchId,templateName,templateId) != null) ? "modified" : "not modified";
	}
	
	public static void setTemplateNotModified(long searchId, String templateName, long templateId){
		setTemplateState(searchId, templateName, "", false);
		setTemplateState(searchId, templateId+"", "", false);
	}
	
	public static void setTemplateModified(long searchId, String templateId, String modifiedTemplatePath){
		setTemplateState(searchId, templateId, modifiedTemplatePath, true);
	}
	
	public static void setTemplateState(long searchId, String templateName, String modifiedTemplatePath, boolean modified){
		
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		synchronized (search.getGeneratedTemp()) {
			if(modified) {
				search.getGeneratedTemp().put(templateName,modifiedTemplatePath);
			}else {
				search.getGeneratedTemp().remove(templateName);
			}
		}
	}
	
	public static boolean isTemplateModified(long searchId, String templateName){
		
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		synchronized (search.getGeneratedTemp()) {
			return search.getGeneratedTemp().containsKey(templateName);	
		}
	}
	
	public static String isTemplateModified(long searchId, String templateName, long templateId){
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		return isTemplateModified(search, templateName, templateId);
	}
	
	public static String isTemplateModified(Search search, String templateName, long templateId){
		
		synchronized (search.getGeneratedTemp()) {
			HashMap<String, String> generated = search.getGeneratedTemp();
			if(generated.containsKey(Long.toString(templateId))) {
				return generated.get(Long.toString(templateId));
			}
			if(generated.containsKey(templateName)) {
				return generated.get(templateName);
			}
			
		}
		return null;
	}

	private static  CompileTemplateResult compileStarterFile( long searchId, long userId, String starterId)throws Exception {
		
		CompileTemplateResult compileTemplateResult = new CompileTemplateResult();
		compileTemplateResult.setTemplateNewPath(starterId);
		

		TemplateUtils.TestAvailable test = TemplateUtils.isSearchAvailable( searchId, userId );
		if (!test.available) {
			throw new Exception(test.errorBody);
		}
		
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		DocumentsManagerI manager = search.getDocManager();
		DocumentI doc = null;
		
		
		try{
			manager.getAccess();
			doc = manager.getDocument(starterId);
		}
		finally{
			manager.releaseAccess();
		}
		
		String content = DBManager.getSfDocumentIndex(((SSFPriorFileDocument)doc).getSsfIdexId());
		content = content.replaceAll("></","> </");
		content = content.replaceAll("(?i)<text>([^Å]+?)</text>", "<text> <!--$1--> \n</text>");
		content = content.replaceAll("(?i)(?i)(<subdivision>[^Å]*?<name>)([^Å]*?)(</name>\\s+</subdivision>)", "$1<!--$2--> \n$3");
		content = content.replaceAll("(?i)<county>([^Å]+?)</county>", "<county> <!--$1--> \n</county>");
		content = content.replaceAll("(?i)<unit>([^Å]+?)</unit>", "<unit> <!--$1--> \n</unit>");
		content = content.replaceAll("(?i)<abbreviation>([^Å]+?)</abbreviation>", "<abbreviation> <!--$1--> \n</abbreviation>");
		content = content.replaceAll("(?i)<effectiveDate>([^Å]+?)</effectiveDate>", "<effectiveDate> <!--$1--> \n</effectiveDate>");
		content = content.replaceAll("(?i)<underwriter>([^Å]+?)</underwriter>", "<underwriter> <!--$1--> \n</underwriter>");
		content = content.replaceAll("(?i)<estateOfInterest>([^Å]+?)</estateOfInterest>", "<estateOfInterest> <!--$1--> \n</estateOfInterest>");
		content = content.replaceAll("(?i)<agentId>([^Å]+?)</agentId>", "<agentId> <!--$1--> \n</agentId>");
		content = content.replaceAll("(?i)<agentFileId>([^Å]+?)</agentFileId>", "<agentFileId> <!--$1--> \n</agentFileId>");
		//content = content.replaceAll("(?i)<fileType>([^Å]+?)</fileType>", "<fileType> <!--$1--> \n</fileType>");
		content = content.replaceAll("(?i)<communityFileId>([^Å]+?)</communityFileId>", "<communityFileId> <!--$1--> \n</communityFileId>");
		content = content.replaceAll("(?i)<geoCode>([^Å]+?)</geoCode >", "<geoCode> <!--$1--> \n</geoCode>");
		content = content.replaceAll("(?i)<first>([^Å]+?)</first>", "<first> <!--$1--> \n</first>");
		content = content.replaceAll("(?i)<middle>([^Å]+?)</middle>", "<middle> <!--$1--> \n</middle>");
		content = content.replaceAll("(?i)<last>([^Å]+?)</last>", "<last> <!--$1--> \n</last>");
		content = content.replaceAll("(?i)<fullName>([^Å]+?)</fullName>", "<fullName> <!--$1--> \n</fullName>");
		content = content.replaceAll("(?i)<freeForm>([^Å]+?)</freeForm>", "<freeForm> <!--$1--> \n</freeForm>");
		content = content.replaceAll("(?i)<type>([^Å]+?)</type>", "<type> <!--$1--> \n</type>");
		content = content.replaceAll("(?i)<lotFreeForm>([^Å]+?)</lotFreeForm>", "<lotFreeForm> <!--$1--> \n</lotFreeForm>");
		content = content.replaceAll("(?i)<blockFreeForm>([^Å]+?)</blockFreeForm>", "<blockFreeForm> <!--$1--> \n</blockFreeForm>");
		content = content.replaceAll("(?i)<censusTract>([^Å]+?)</censusTract>", "<censusTract> <!--$1--> \n</censusTract>");
		content = content.replaceAll("(?i)<phase>([^Å]+?)</phase>", "<phase> <!--$1--> \n</phase>");
		content = content.replaceAll("(?i)<zip>([^Å]+?)</zip>", "<zip> <!--$1--> \n</zip>");
		content = content.replaceAll("(?i)<city>([^Å]+?)</city>", "<city> <!--$1--> \n</city>");
		content = content.replaceAll("(?i)<streetName>([^Å]+?)</streetName>", "<streetName> <!--$1--> \n</streetName>");
		content = content.replaceAll("(?i)<isCompany>([^Å]+?)</isCompany>", "<isCompany> <!--$1--> \n</isCompany>");
		content = content.replaceAll("(?i)<number>([^Å]+?)</number>", "<number> <!--$1--> \n</number>");
		content = content.replaceAll("(?i)<suffix>([^Å]+?)</suffix>", "<suffix> <!--$1--> \n</suffix>");
		content = content.replaceAll("(?i)<platBook>([^Å]+?)</platBook>", "<platBook> <!--$1--> \n</platBook>");
		content = content.replaceAll("(?i)<platPage>([^Å]+?)</platPage>", "<platPage> <!--$1--> \n</platPage>");
		content = content.replaceAll("(?i)<parcelId>([^Å]+?)</parcelId>", "<parcelId> <!--$1--> \n</parcelId>");
		content = content.replaceAll("(?i)<preDirection>([^Å]+?)</preDirection>", "<preDirection> <!--$1--> \n</preDirection>");
		content = content.replaceAll("(?i)<apn>([^Å]+?)</apn>", "<apn> <!--$1--> \n</apn>");
		content = content.replaceAll("(?i)<sectionFreeForm>([^Å]+?)</sectionFreeForm>", "<sectionFreeForm> <!--$1--> \n</sectionFreeForm>");
		content = content.replaceAll("(?i)<section>([^Å]+?)</section>", "<section> <!--$1--> \n</section>");
		
		content = content.replaceAll("(?i)<block[ \t\r\n]+value=[ \t\r\n]*\"([^\"]+)\"", "<block value=\"<!--$1-->\"\n");	
		content = content.replaceAll(">([ \t\n\r]+)<", "><");
		//content = content.replaceAll(">([^<]+)<", "> <!--$1--> \n<");
		
		compileTemplateResult.setTemplateContent(content);
		compileTemplateResult.setServerURL(ServerConfig.getAppUrl());
		compileTemplateResult.setTemplateId("1");
		
		SSFPriorFileDocument ssfDoc = (SSFPriorFileDocument)doc;
		StringBuilder statementsText = new StringBuilder();
		statementsText.append(PriorFileDocument.REQUIREMENTS + "=");
		
		if(ssfDoc.getRequirements() != null) {
			for(SelectableStatement stmt : ssfDoc.getRequirements()) {
				statementsText.append(stmt.getText() + "=" + stmt.isSelected() + "%@%314zda%");
			}
			if(statementsText.toString().endsWith("%@%314zda%")) {
				statementsText.replace(statementsText.lastIndexOf("%@%314zda%"), statementsText.length(), "");
			}
		}
		
		statementsText.append("#@#&069%3#" + PriorFileDocument.EXCEPTIONS + "=");
		if(ssfDoc.getExceptionsList() != null) {
			for(SelectableStatement stmt : ssfDoc.getExceptionsList()) {
				statementsText.append(stmt.getText() + "=" + stmt.isSelected() + "%@%314zda%");
			}
			if(statementsText.toString().endsWith("%@%314zda%")) {
				statementsText.replace(statementsText.lastIndexOf("%@%314zda%"), statementsText.length(), "");
			}
		}
		
		statementsText.append("#@#&069%3#" + PriorFileDocument.LEGAL_DESC + "=");
		if(ssfDoc.getLegalDescriptions() != null) {
			for(SelectableStatement stmt : ssfDoc.getLegalDescriptions()) {
				statementsText.append(stmt.getText() + "=" + stmt.isSelected() + "%@%314zda%");
			}
			if(statementsText.toString().endsWith("%@%314zda%")) {
				statementsText.replace(statementsText.lastIndexOf("%@%314zda%"), statementsText.length(), "");
			}
		}
		
		compileTemplateResult.setStatementsText(statementsText.toString());
		
		return compileTemplateResult;
	}
	
	public static  CompileTemplateResult compileTemplate( 
			long searchId, 
			long userId, 
			String templateName, 
			boolean processTemplateForExport,
			String undefinedContents, 
			List<InstrumentStructForUndefined> instrumentListForUndefined, 
			boolean useUndefined, 
			java.util.Map<String,String> bolilerPlatesTSR ) throws Exception {
		return compileTemplate(
				searchId, 
				userId, 
				templateName, 
				processTemplateForExport, 
				undefinedContents, 
				instrumentListForUndefined, 
				useUndefined,
				bolilerPlatesTSR, 
				false, 
				null,
				null);
	}
	
	public static CompileTemplateResult compileTemplate( 
			long searchId, 
			long userId, 
			String templateName, 
			boolean processTemplateForExport,
			String undefinedContents, 
			List<InstrumentStructForUndefined> instrumentListForUndefined, 
			boolean useUndefined, 
			java.util.Map<String,String> bolilerPlatesTSR, 
			boolean getBoilerMap, String bpCodeToFill, String documentIdToFill) throws Exception {

		String ret = "";
		String templateNewPath = "";
		long templateId = -1;
		
		Search globalSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		CommunityAttributes ca = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity();

		TemplateUtils.TestAvailable test = TemplateUtils.isSearchAvailable(searchId, userId);
		if (!test.available) {
			throw new Exception(test.errorBody);
		}
		
		CompileTemplateResult compileTemplateResult = new CompileTemplateResult();

		DocumentsManagerI manager = globalSearch.getDocManager();
		
		

		if (templateName.matches("[0-9]+_[0-9]+")) {
			try {
				manager.getAccess();
				DocumentI docI = manager.getDocument(templateName);
				if (docI != null) {
					if("SF".equalsIgnoreCase(docI.getDataSource())) {
						return compileStarterFile(searchId, userId, templateName);
					} else if ("ATS".equalsIgnoreCase(docI.getDataSource()) && docI instanceof PriorFileAtsDocumentI) {
						return getPriorFileAts(globalSearch, (PriorFileAtsDocumentI)docI, templateName);
					}
				}	
			} finally {
				manager.releaseAccess();
			}
		}

		long agentId = globalSearch.getAgent().getID().longValue();
		int productId = globalSearch.getProductId();
		
//		List<CommunityTemplatesMapper> userTemplates;
		long startTMS = System.currentTimeMillis();
//		userTemplates = UserUtils.getUserTemplates(agentId, productId);
		CommunityTemplatesMapper currentTemplate = UserUtils.getTemplateByFileName(agentId, productId, templateName);	
		long endTMS = System.currentTimeMillis();
		System.err.println("timeSpent in miliseconds " + ((endTMS - startTMS)) + " UserUtils.getUserTemplates(" + agentId + ", " + productId + ")");
		County currentCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty();

		State currentState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState();
		HashMap<String, Object> templateParams = TemplateBuilder.fillTemplateParameters(globalSearch, ca,
				currentCounty, currentState, false, false, null);

		if (instrumentListForUndefined == null && !useUndefined) {
			instrumentListForUndefined = new ArrayList<InstrumentStructForUndefined>();
		}
		
//		CommunityTemplatesMapper currentTemplate = UserUtils.getTemplateByPath(agentId, productId, templateName);	
//		if ((currentTemplate = FidelityTSD.getTemplateObject(templateName, userTemplates)) != null) {
		if(currentTemplate != null) {
			
			if(currentTemplate.isCodeBookLibrary()) {
				String fixedContent = fixAllRecursiveStaticCodes(productId, currentTemplate.getFileContent());
				
				currentTemplate.setFileContent(fixedContent);
				
			}
			
			templateId = currentTemplate.getId();
			String fileExtension = null;
			
			String templateModifiedPath = isTemplateModified(globalSearch, templateName, templateId);
			if ((templateNewPath.equals(templateModifiedPath))) {
				templateNewPath = globalSearch.getSearchDir() + File.separator + Search.TEMP_DIR_FOR_TEMPLATES + File.separator + templateName;
				templateNewPath = templateNewPath.replaceAll("//", "/");
				try {
					fileExtension = templateNewPath.substring(templateNewPath.lastIndexOf(".") + 1);
					fileExtension = (fileExtension == null) ? "" : fileExtension;
					
					ret = AddDocsTemplates.completeNewTemplatesV2New(
									templateParams, 
									templateNewPath, 
									currentTemplate, 
									globalSearch,
									false, 
									undefinedContents, 
									instrumentListForUndefined, 
									bolilerPlatesTSR, 
									false, 
									getBoilerMap, 
									bpCodeToFill, 
									documentIdToFill);
					
					if ((templateNewPath.toLowerCase().contains("tsdtemplate"))
							&& (fileExtension.equals("htm") || fileExtension.equals("html"))) {
						boolean succeded = TemplateBuilder.corectTSDTemplate(templateNewPath);
						if (!succeded) {
							System.err.println("%%%%%%%%%%%%%%  Can't  corect the TSD template    %%%%%%%%%%%%%%\n"
									+ templateNewPath);
						}
					}
					
					

				} catch (Exception e) {
					System.err.println(">>>>>> Exception in  completeNewTemplatesV2(...) function\n" + e.getCause());
					e.printStackTrace();
				}
			} else {// nu este deja modificat
				
				templateNewPath = templateNewPath.replaceAll("//", "/");
				try {
					File f = new File(templateNewPath);
					if (!f.exists()) {
						templateNewPath = globalSearch.getGeneratedTemp().get(templateId + "");
					}
					
					fileExtension = templateNewPath.substring(templateNewPath.lastIndexOf(".") + 1);
					fileExtension = (fileExtension == null) ? "" : fileExtension;
					
					RandomAccessFile rand = new RandomAccessFile(templateNewPath, "rw");
					byte[] b = new byte[(int) rand.length()];
					rand.readFully(b);
					rand.close();
					ret = new String(b);

					ret = ret.replaceAll("https?://ats(?:prdinet|stginet|preinet)?[0-9]*\\.advantagetitlesearch\\.com(?::\\d+)?",
							ServerConfig.getAppUrl());

				} catch (Exception e) {
					templateNewPath = globalSearch.getSearchDir() + File.separator + Search.TEMP_DIR_FOR_TEMPLATES
							+ File.separator + templateName;
					templateNewPath = templateNewPath.replaceAll("//", "/");
					
					fileExtension = templateNewPath.substring(templateNewPath.lastIndexOf(".") + 1);
					fileExtension = (fileExtension == null) ? "" : fileExtension;
					
					try {
						long startTMS1 = System.currentTimeMillis();
						// if (isNotdocTemplate) {
						ret = AddDocsTemplates.completeNewTemplatesV2New(
								templateParams, 
								templateNewPath,
								currentTemplate, 
								globalSearch, 
								false, 
								undefinedContents, 
								instrumentListForUndefined,
								bolilerPlatesTSR, 
								false, 
								getBoilerMap, 
								bpCodeToFill, 
								documentIdToFill);
						// } else {
						// ret = AddDocsTemplates.completeNewTemplatesV2(
						// templateParams, templateNewPath, currentTemplate,
						// globalSearch,
						// instrumentListForUndefined,bolilerPlatesTSR);
						// }
						long endTMS1 = System.currentTimeMillis();
						System.err.println("timeSpent in miliseconds " + ((endTMS1 - startTMS1)) + " completeNewTemplatesV2New " + templateNewPath);
					} catch (Exception e1) {
						System.err
								.println(">>>>>> Exception in  completeNewTemplatesV2(...) function\n" + e1.getCause());
						e1.printStackTrace();
					}
				}
			}
			
			if(processTemplateForExport) {
				try {
					String templateNewPathAfterProcess = TemplateBuilder.processTemplateForExport(templateNewPath,templateName, fileExtension, globalSearch, 
							templateModifiedPath != null,false, ca.getID().intValue(), agentId, templateId, false);
					compileTemplateResult.setProcessedContent(FileUtils.readFileToString(new File(templateNewPathAfterProcess)));
				} catch (Exception e) {
					logger.error("Cannot process tempalte " + templateName + " for searchid " + searchId, e);
				}
				
				
			}
		}
		
		compileTemplateResult.setTemplateNewPath(templateNewPath);
		compileTemplateResult.setTemplateContent(ret);
		compileTemplateResult.setServerURL(ServerConfig.getAppUrl());
		compileTemplateResult.setTemplateId(templateId + "");
		
//		String[] retFinal 	 = new String[4];
//		
//		retFinal[0] 		 = templateNewPath;
//		retFinal[1] 		 = ret;
//		String parServerName = rbc.getString("app.url");
//		retFinal[2] 		 = parServerName;
//		retFinal[3]			 = templateId+"";
		
		if (templateName.contains(TemplatesInitUtils.TEMPLATE_TSD_START)) {
			TemplateUtils.tempTSDContent.put(searchId, ret);
			ret = "";
		} else {
			if( !useUndefined ){
				TemplateUtils.tempTemplateInfo.put(searchId, compileTemplateResult);
			}
		}
		return compileTemplateResult;
	}

	public static String fixAllRecursiveStaticCodes(int productId, String initialFileContent) {
		String fileContent = initialFileContent;
		
		for (int i = 0; i < 10; i++) {
			String lastFixedContent = fileContent;
			fileContent = fixFirstLevelRecursiveStaticCodes(productId, lastFixedContent);
			if(lastFixedContent.equals(fileContent)) {	
				//no changes
				break;
			}
			
		}
		
		return fileContent;
		
	}
	
	private static String fixFirstLevelRecursiveStaticCodes(int productId, String initialFileContent) {
		String fileContent = initialFileContent;
		boolean hasMoreMatches = true;
		Matcher mat = null;
		
		Map<String, String> hashMap = new CaseInsensitiveMap<String, String>();
		
		while (hasMoreMatches) {
			mat = AddDocsTemplates.PATTERN_BPCODE_PRODUCT.matcher(fileContent);
			if (mat.find()) {
				boolean addBoilerContent = false;

				
				String label = mat.group(1);
				// take just boilers for product id or default TemplateUtils.BOILER_MAP_DEFAULT_PRODUCT
				if (label.endsWith("_" + productId)
						|| (label.endsWith("_" + TemplateUtils.BOILER_MAP_DEFAULT_PRODUCT) && !hashMap.containsKey(label.split(TemplatesServlet.LABEL_NAME_DELIM)[0]))) {
					label = label.split(TemplatesServlet.LABEL_NAME_DELIM)[0];
					addBoilerContent = true;
				}
				

				fileContent = fileContent.substring(mat.end());
				mat = AddDocsTemplates.PATTERN_BPCODE_PRODUCT.matcher(fileContent);
				
				String content = null;
				
				if(mat.find()) {
					content = fileContent.substring(0, mat.start() - 1);
				} else {
					content = fileContent;
				}
//						content = mat.find() ? fileContent.substring(0, mat.start()) : fileContent;

				if (addBoilerContent) {
					hashMap.put(label, content/*.trim()*/);
				}
			} else {
				hasMoreMatches = false;
			}
		}
		
		StringBuffer completed = new StringBuffer();
		Matcher matcherK = Pattern.compile(AddDocsTemplates.TAG_WITH_PARAMETERS).matcher(initialFileContent);
		while (matcherK.find()) {
			
			String tagNameK = matcherK.group(1);
			String keyNameK = matcherK.group(2);
			
			String keyNameKValue = matcherK.group();
			if(tagNameK.equals(AddDocsTemplates.bpCode) || tagNameK.equals(AddDocsTemplates.cbCode)) {
				String tempValue = hashMap.get(keyNameK);
				
				if(tempValue != null && 
						(org.apache.commons.lang.StringUtils.isNotBlank(RegExUtils.getFirstMatch("\\b(" + AddDocsTemplates.cbCode + "\\(" + keyNameK + "\\))", tempValue, 1)) 
						|| org.apache.commons.lang.StringUtils.isNotBlank(RegExUtils.getFirstMatch("\\b(" + AddDocsTemplates.cbCode + "\\(" + keyNameK + "\\))", tempValue, 1)))) {
					//do not allow recursive codes
					keyNameKValue = "";
				} else {
					keyNameKValue = tempValue;
				}
				
			}
			
			matcherK.appendReplacement(completed, Matcher.quoteReplacement(org.apache.commons.lang.StringUtils.defaultString(keyNameKValue)));
			
		}
		
		matcherK.appendTail(completed);
		
		return completed.toString();
	}

	public static CompileTemplateResult getPriorFileAts(Search globalSearch, PriorFileAtsDocumentI priorFileAtsDocumentI,
			String templateName) throws Exception{
		String instno = priorFileAtsDocumentI.getDocno();
		long searchIdToOpen = Long.parseLong(instno);
		
		CompileTemplateResult compileTemplateResult = new CompileTemplateResult(); 
		//retFinal[0] 		 = templateName;
		
		String content = null;
		
		
		//we need this to force creation of files
		Search searchToOpenFromDisk = SearchManager.getSearchFromDisk(searchIdToOpen);
		
		if(searchToOpenFromDisk == null) {
			throw new Exception("Cannot open base file!");
		}
		
		if(searchToOpenFromDisk.getAgent() == null) {
			throw new Exception("There is no \".ats\" template saved for this base file!");
		}
		
		HashMap<String, String> generatedTemp = searchToOpenFromDisk.getGeneratedTemp();
		for (String string : generatedTemp.keySet()) {
			String	templateNewPath = searchToOpenFromDisk.getGeneratedTemp().get(string);
			
			if(!templateNewPath.toLowerCase().endsWith(".ats")) {
				continue;
			}
			
			File templateFile = new File(templateNewPath);
			if(templateFile.exists()) {
				RandomAccessFile rand = new RandomAccessFile(templateFile, "rw");
				byte[] b = new byte[(int) rand.length()];
				rand.readFully(b);
				rand.close();
				content = new String(b);
	
				content = content.replaceAll("https?://ats(?:prdinet|stginet|preinet)?[0-9]*\\.advantagetitlesearch\\.com(?::\\d+)?",
						ServerConfig.getAppUrl());
				
				try {
					HashMap template = UserUtils.getTemplate(Integer.parseInt(string));
					if(template != null) {
						compileTemplateResult.setTemplateNewPath((String) template.get(UserUtils.TEMPLATE_NAME));
					}
				} catch (Exception e) {
					logger.error("Error while tring to load old template from database. Using just filename and not template name", e);
				}
				
				if(StringUtils.isEmpty(compileTemplateResult.getTemplateNewPath())) {
					compileTemplateResult.setTemplateNewPath(FilenameUtils.getName(templateNewPath));
				}
				
				break;
				
			}
			
		}
		
		/*
		
		List<CommunityTemplatesMapper> userTemplates = UserUtils.getUserTemplates(
				searchToOpenFromDisk.getAgent().getID().longValue(),
				searchToOpenFromDisk.getProductId());
		
		for (CommunityTemplatesMapper communityTemplatesMapper : userTemplates) {
		
			if(!communityTemplatesMapper.getPath().toLowerCase().endsWith(".ats")) {
				continue;
			}
		
			String	templateNewPath = searchToOpenFromDisk.getGeneratedTemp().get(communityTemplatesMapper.getId() + "");
			if(StringUtils.isEmpty(templateNewPath)) {
				continue;
			}
			File templateFile = new File(templateNewPath);
			if(templateFile.exists()) {
				RandomAccessFile rand = new RandomAccessFile(templateFile, "rw");
				byte[] b = new byte[(int) rand.length()];
				rand.readFully(b);
				rand.close();
				content = new String(b);
	
				content = content.replaceAll("https?://ats(?:prdinet|stginet|preinet)?[0-9]*\\.advantagetitlesearch\\.com(?::\\d+)?",
						ServerConfig.getAppUrl());
				
				retFinal[0] = communityTemplatesMapper.getName();
				break;
				
			}
		}
		
		*/
		
		String _content = DBManager.getDocumentIndex(priorFileAtsDocumentI.getIndexId());
		if(StringUtils.isEmpty(_content) || _content.trim().toLowerCase().startsWith("<html>")) {
			if(StringUtils.isNotEmpty(content)) {
				int idx = DBManager.addDocumentIndex(content, globalSearch);
				priorFileAtsDocumentI.setIndexId(idx);
			}
		} else {
			content = _content;
		}
		
		if(content == null) {
			throw new Exception("There is no \".ats\" template saved for this base file!");
		} else {
			content = TemplateBuilder.replaceImageLinksInTemplate(content, searchToOpenFromDisk, false, true);
		}
		
		compileTemplateResult.setTemplateContent(content);
		compileTemplateResult.setServerURL(ServerConfig.getAppUrl());
		compileTemplateResult.setTemplateId("2"); //no idea what this is
		
		StringBuilder statementsText = new StringBuilder();
		statementsText.append(PriorFileDocument.REQUIREMENTS + "=");
		
		if(priorFileAtsDocumentI.getRequirements() != null) {
			for(SelectableStatement stmt : priorFileAtsDocumentI.getRequirements()) {
				statementsText.append(stmt.getText() + "=" + stmt.isSelected() + "%@%314zda%");
			}
			if(statementsText.toString().endsWith("%@%314zda%")) {
				statementsText.replace(statementsText.lastIndexOf("%@%314zda%"), statementsText.length(), "");
			}
		}
		
		statementsText.append("#@#&069%3#" + PriorFileDocument.EXCEPTIONS + "=");
		if(priorFileAtsDocumentI.getExceptionsList() != null) {
			for(SelectableStatement stmt : priorFileAtsDocumentI.getExceptionsList()) {
				statementsText.append(stmt.getText() + "=" + stmt.isSelected() + "%@%314zda%");
			}
			if(statementsText.toString().endsWith("%@%314zda%")) {
				statementsText.replace(statementsText.lastIndexOf("%@%314zda%"), statementsText.length(), "");
			}
		}
		
		statementsText.append("#@#&069%3#" + PriorFileDocument.LEGAL_DESC + "=");
		if(priorFileAtsDocumentI.getLegalDescriptions() != null) {
			for(SelectableStatement stmt : priorFileAtsDocumentI.getLegalDescriptions()) {
				statementsText.append(stmt.getText() + "=" + stmt.isSelected() + "%@%314zda%");
			}
			if(statementsText.toString().endsWith("%@%314zda%")) {
				statementsText.replace(statementsText.lastIndexOf("%@%314zda%"), statementsText.length(), "");
			}
		}
		
		compileTemplateResult.setStatementsText(statementsText.toString());
		
		return compileTemplateResult;
	}
 
	public static final String BOILER_MAP_SEPARATOR = "###@@@###";
	public static final String BOILER_MAP_SEPARATOR_EQUAL = "#@#@#@#@#";
	public static final String BOILER_MAP_WITH_DOCID = "BOILER_MAP_WITH_DOCID";
	public static final String BOILER_MAP = "BOILER_MAP";
	public static final String BOILER_MAP_WITH_DOCID_BPSOBJECT = "BOILER_MAP_WITH_DOCID_BPSOBJECT";
	
	public static final int BOILER_MAP_DEFAULT_PRODUCT = 1;
	
	public static Map<String,String> getBoilerText(long searchId,long userId, HttpServletRequest request) {
		 	
		HashMap<String,String> boilerPlatesTSR = new HashMap<String,String>();
//		DocumentsManagerI docManager= InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getDocManager();
			
//		try {
//			docManager.getAccess();
//			for(DocumentI doc : docManager.getDocumentsList(true) ) {
//				if(!doc.getBoilerPlatesCode().isEmpty() && !doc.getBoilerPlatesCode().equals("*")) {
//					boilerPlatesTSR.put(doc.getId(),doc.getBoilerPlatesCode());
//				}
//			}
//		}
//		finally{
//			docManager.releaseAccess();
//		}
			
		try {			
			if(!boilerPlatesTSR.isEmpty()) {
				Map<String,String> boiler = TemplatesServlet.getBoilerPlatesMap(searchId, userId, request);
					
				for(Entry<String,String> e : boilerPlatesTSR.entrySet()) {
					if(boiler.containsKey(e.getValue())) {
						e.setValue(boiler.get(e.getValue()));
					}
				}
			}
		}catch(Exception e) { e.printStackTrace(); }
			
		return  boilerPlatesTSR;
	 }
	
	/**
	 * 
	 * @param searchId
	 * @param userId
	 * @param doc
	 * @return get the boiler plate map using cached copies
	 */
	public static Map<String,String> getBoilerTextForDocument(long searchId,long userId, DocumentI doc) {
		return getBoilerTextForDocument(searchId, userId, doc, true, null, true);
	}
	
	/**
	 * 
	 * @param searchId
	 * @param userId
	 * @param doc
	 * @param getACachedCopyForText -> if false clears the cached maps for boiler plates on this search
	 * @param bpCodeToFill 
	 * @param matchDocument - forces the result to be used filled only according to the data stored on doc
	 * @return map[bpCode, value] for doc
	 */
	public static Map<String,String> getBoilerTextForDocument(long searchId,long userId, DocumentI doc, boolean getACachedCopyForText, String bpCodeToFill, boolean matchDocument) {
	 	
		if(!getACachedCopyForText){
			clearChachedBoilerPlateMaps(searchId);
		}
		
		Map<String, String> boilerPlatesTSR = new CaseInsensitiveMap<String, String>();

		try {			
			if(doc != null) {
				BoilerPlateObject boilerPlateObject = null;
				if(bpCodeToFill != null) {
					boilerPlateObject = doc.getCodeBookObject(bpCodeToFill);
				}
				if(boilerPlateObject != null) {
					if(boilerPlateObject.getModifiedStatement() != null) {
						boilerPlatesTSR.put(bpCodeToFill, boilerPlateObject.getModifiedStatement());
					} else {
						Map<String, CodeBookStatementObject> boiler = TemplatesServlet.getBoilerPlateStatementObjectMap(searchId, userId, true, bpCodeToFill, doc.getId());
						
						for(String bpCode : doc.getCodeBookCodes()) {
							
							BoilerPlateObject filledOnDocument = doc.getCodeBookObject(bpCode);
							if(filledOnDocument.hasModifiedStatement()) {
								boilerPlatesTSR.put(bpCode, filledOnDocument.getModifiedStatement());
							} else {
								if(boiler.containsKey(bpCode)) {
									boilerPlatesTSR.put(bpCode, 
											org.apache.commons.lang.StringUtils.defaultString(boiler.get(bpCode).getValueForDocId(doc.getId())));
								}
							}
						}
					
					}
				} else {
					Map<String, CodeBookStatementObject> boiler = TemplatesServlet.getBoilerPlateStatementObjectMap(searchId, userId, true, bpCodeToFill, doc.getId());
					
					if(matchDocument) {
						for(String bpCode : doc.getCodeBookCodes()) {
							
							BoilerPlateObject filledOnDocument = doc.getCodeBookObject(bpCode);
							if(filledOnDocument.hasModifiedStatement()) {
								boilerPlatesTSR.put(bpCode, filledOnDocument.getModifiedStatement());
							} else {
								if(boiler.containsKey(bpCode)) {
									
									if(doc != null 
											&& DocumentTypes.OTHERFILES.equals(doc.getDocType())
											&& DocumentTypes.OTHER_FILE_SPECIAL_SUBCATEGORIES.contains(doc.getDocSubType()) ) {
										boilerPlatesTSR.put(bpCode, 
												org.apache.commons.lang.StringUtils.defaultString(boiler.get(bpCode).getValue()));
									} else {
									
										boilerPlatesTSR.put(bpCode, 
											org.apache.commons.lang.StringUtils.defaultString(boiler.get(bpCode).getValueForDocId(doc.getId())));
									}
								}
							}
						}
					} else {
						if(boiler.containsKey(bpCodeToFill)) {
							if(doc != null 
									&& DocumentTypes.OTHERFILES.equals(doc.getDocType())
									&& DocumentTypes.OTHER_FILE_SPECIAL_SUBCATEGORIES.contains(doc.getDocSubType()) ) {
								boilerPlatesTSR.put(bpCodeToFill, 
										org.apache.commons.lang.StringUtils.defaultString(boiler.get(bpCodeToFill).getValue()));
							} else {
								boilerPlatesTSR.put(bpCodeToFill, 
									org.apache.commons.lang.StringUtils.defaultString(boiler.get(bpCodeToFill).getValueForDocId(doc.getId())));
							}
						}
					}
				}
			} else {
				Map<String, CodeBookStatementObject> boiler = TemplatesServlet.getBoilerPlateStatementObjectMap(searchId, userId, true, bpCodeToFill, null);
				if(boiler.containsKey(bpCodeToFill)) {
					boilerPlatesTSR.put(bpCodeToFill, 
							org.apache.commons.lang.StringUtils.defaultString(boiler.get(bpCodeToFill).getValue()));
				}
			}
		} catch (Exception e) {
			logger.error("Cannot get boilter text for searchId " + searchId + 
					", userId " + userId + ", bpcode " + bpCodeToFill + " and doc " + doc.prettyPrint(true), e);
			e.printStackTrace();
		}
		
		return  boilerPlatesTSR;
	 }
	
	/**
	 * Fills all CBCodes for all documents by product id of the search, it does a clear cache first, during the update of the documents it will use a cached copy<br>
	 * Then passes again and fills codes because some libraries might contains BPCODE that was filled in the first pass
	 * @param searchId
	 * @param userId - id of the current user that is in control of the order (the abstractor)
	 */
	public  static void fillDocumentBPCodes(long searchId, long userId) {
		fillDocumentBPCodes(searchId, userId, false);
	}
	
	/**
	 * Fills all CBCodes for all documents by product id of the search, it does a clear cache first, during the update of the documents it will use a cached copy<br>
	 * Depending of the flag <b>skipSecondPass</b> it will refill codes because some libraries might contains BPCODE that was filled in the first pass
	 * @param searchId
	 * @param userId - id of the current user that is in control of the order (the abstractor)
	 * @param skipSecondPass - if true the second pass will be skipped
	 */
	public  static void fillDocumentBPCodes(long searchId, long userId, boolean skipSecondPass) {
		try {
			clearChachedBoilerPlateMaps(searchId);
			
			DocumentsManagerI docManager= InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getDocManager();
			
			List<DocumentI> documents = new ArrayList<DocumentI>();
			try {
				docManager.getAccess();
				documents = new ArrayList<DocumentI>(docManager.getDocumentsList());
			} finally{
				docManager.releaseAccess();
			}
			
			if(!documents.isEmpty()){
				for(DocumentI doc : documents){
					if(doc.hasCodeBookCodes()){
						doc.setCodeBookCodesValuesFromMap(getBoilerTextForDocument(searchId, userId, doc));
//					} else {
						/*
						//load codes from doctype
						DocTypeNode subcategoryNode = DocTypeNode.getSubcategory(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty(),
								doc.getServerDocType());
						if (subcategoryNode != null) {
							subcategoryNode.fillDocument(doc);
						}
						
						if(doc.hasBoilerPlateCodes()){
							doc.setBoilerPlateCodesFromMap(getBoilerTextForDocument(searchId, userId, doc));
						}
						*/
					}
				}
				
				if(!skipSecondPass) {
					//we need to refill because there are case when some codes in library refer BPCODE which was set just now
					//those statements are not correct yet
					clearChachedBoilerPlateMaps(searchId);	//reset the library cache
					
					for(DocumentI doc : documents){
						if(doc.hasCodeBookCodes()){
							doc.setCodeBookCodesValuesFromMap(getBoilerTextForDocument(searchId, userId, doc));
						}
					}
				}
				
			}
			
			try {
				docManager.getAccess();
				//must update the values stored in documents manager
				for(DocumentI doc : documents){
					docManager.getDocument(doc.getId()).setCodeBookCodeMap(doc);
				}
			} finally{
				docManager.releaseAccess();
			}
			
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	
	/**
	 * clear the 3 hashmaps used in the caching process for boiler plate evaluation
	 * @param searchId
	 */
	private static void clearChachedBoilerPlateMaps(long searchId) {
		try {
			Search globalSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();

			globalSearch.removeAdditionalInfo(TemplateUtils.BOILER_MAP_WITH_DOCID);
			globalSearch.removeAdditionalInfo(TemplateUtils.BOILER_MAP);
			globalSearch.removeAdditionalInfo(TemplateUtils.BOILER_MAP_WITH_DOCID_BPSOBJECT);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public  static String replaceSpecialElements(String input, String strStart, String strEnd, HashMap<String, String> hm){
			StringBuffer ret = new StringBuffer(input);
			String 	subString="",	noSpaceString = "";
			int pozOpen = 0, pozClose = 0;
			
			while(true){
				pozOpen = ret.indexOf(strStart, pozClose);
				pozClose = ret.indexOf(strEnd, pozClose);
				if(pozOpen > pozClose){
					//daca am gasit intai strStart si apoi strEnd avem eroare
					return ret.toString();
				}
				if(pozOpen==-1){
					//nu s-a gasit strStart
					if(pozClose > -1){
						//daca s-a gasit strEnd avem eroare si returnam ce am corectat pana acum
						return ret.toString();
					}
					//nu s-a gasit nici strEnd deci e ok si pot sa ies
					break;
				}
				//	am gasit o pereche "{#" ... "#}"
				subString = ret.substring(pozOpen+2, pozClose);
				noSpaceString = subString.replaceAll("[ \t]*", "");
				
				if(noSpaceString.length() > 0){	//daca am gasit ceva
					String replStr = hm.get(noSpaceString);
					if(replStr != null) {
						//verific daca este o pereche simpla fara alte chei
						ret.delete(pozOpen, pozClose+2);
						ret.insert(pozOpen, replStr);
						pozClose = pozOpen + replStr.length();
					}
				}
				else{
					pozClose++;
				}	
			}
			
			return ret.toString();
		}
		
		public static String replaceSpecialElements (HashMap<String, String> hmPricipal,
													 HashMap<String, Vector<HashMap<String,String>>> hmSecundar,
													 String input, 
													 boolean test ) {
			final String CONTOR_TYPE_1 = "CONTOR_TYPE_1";
			StringBuffer ret = new StringBuffer(input);
			String subString="";
			String noSpaceString = "";
			String afterEqualsString= "";
			Vector <HashMap<String,String>>value = new Vector<HashMap<String,String>>();
			
			
			int pozOpen = 0, pozClose = 0;
			//int first = 0, last = 0;
			int pozEqual=0;
			while(true){
				pozOpen = ret.indexOf("%$", pozClose);
				pozClose = ret.indexOf("$%", pozClose);
				//System.err.println(">>>>>>  pozOpen = "+pozOpen +" pozClose = " +pozClose );
				
				//compar pozitia unde am gasit "{#" cu poz unde am gasit "#}"
				if(pozOpen > pozClose){
					//daca am gasit intai "#}" si apoi "{#" avem eroare
					return ret.toString();
				}
				if(pozOpen==-1){
					//nu s-a gasit "{#"
					if(pozClose > -1){
						//daca s-a gasit "#}" avem eroare si returnam ce am schimbat pana acum
						return ret.toString();
					}
					//nu s-a gasit nici "}" deci e ok si pot sa ies
					break;
				}
				//am gasit o pereche "{#" ... "#}"
				subString = ret.substring(pozOpen+2, pozClose);
				noSpaceString = subString.replaceAll("[ \t]*", "");
				if (!test && noSpaceString.indexOf(CONTOR_TYPE_1)==0){
					//System.err.println(">>> voi sterge "+aux);
					ret.delete(pozOpen, pozClose+2);
					pozClose = pozOpen;
					continue;
				}
				
				if(noSpaceString.length() > 0){	//daca am gasit ceva
					String local=hmPricipal.get(noSpaceString);
					if(local!= null) {
						//verific daca este o pereche simpla fara alte chei
						//System.err.println(aux);
						ret.delete(pozOpen, pozClose+2);
						ret.insert(pozOpen, local);
						pozClose = pozOpen + local.length();
					}
					else {
						if (hmSecundar == null){
							pozClose++;
							continue;
						}
						//intru aici daca nu am gasit cheie in hmPrincipal
						//trebuie sa gasesc cheia daca exista...
						pozEqual = subString.indexOf("=");
						
						if(pozEqual == -1){ //ne exista egal imi vad de treaba mai incolo
							pozClose++;
							continue;
						}
						
						noSpaceString = subString.substring(0, pozEqual).replaceAll("[ \t]*", "");
						afterEqualsString = subString.substring(pozEqual+1);
						//am selectat portiunea dintre "{#....=" pe care o caut in hmSecundar
						value = hmSecundar.get(noSpaceString);
						if(value !=null) {
							String curentReplaceStr =null; 
							String finalReplaceStr = null;
							//am gasit cheia, asa ca trebuie sa schimb tot ce gasesc
							for(int i=0;i<value.size();i++){
								HashMap<String,String> curentMap = value.get(i);
								curentReplaceStr = replaceSpecialElements(afterEqualsString,"[$","$]",curentMap);
								if (finalReplaceStr == null){
									finalReplaceStr = curentReplaceStr;
								}
								else {
									finalReplaceStr += curentReplaceStr;
								}
							}
							if(finalReplaceStr!=null){
								ret.delete(pozOpen, pozClose+2);
								ret.insert(pozOpen, finalReplaceStr);
								pozClose = pozOpen + finalReplaceStr.length();
							}
						}
						else{
							pozClose++;
						}
					}
				}
				else{
					pozClose++;
				}
			}
			return ret.toString();
		}
		
		
		public static String getBookPageInstrument(DocumentI doc, Search search) {
	    	return getBookPageInstrument(doc, false, search);
	    }
	         
	    public static String getBookPageInstrument(DocumentI doc, boolean shortFormat,Search search) {
	    	
	    	String instrument = "";  	
	    	
	    	if(!StringUtils.isEmpty(doc.getBook()) && !StringUtils.isEmpty(doc.getPage())) {
	    		if(shortFormat) 
	    			instrument = doc.getBook() + "_" + doc.getPage();
	    		else 
	    			instrument = "Book " + doc.getBook() + ", Page " + doc.getPage();
	    	}
	    	else {
	    		if(!StringUtils.isEmpty(doc.getInstno())) {
	    			instrument = doc.getInstno();    			
	    		}
	    		else {
	    			instrument = doc.getDocno();
	    		}
	    	}    	
	    	
	    	return instrument;
	    }
	    
	    public static String getInstNoBookPage(DocumentI doc, Search search, boolean reverseOrder){
	    	return getInstNoBookPage(doc, false, search, reverseOrder);
	    }
	    
	    public static String getInstNoBookPage(DocumentI doc, boolean shortFormat, Search search, boolean reverseOrder) {
	    	
	    	String display = "";  	
	    	
	    	if(!StringUtils.isEmpty(doc.getBook()) && !StringUtils.isEmpty(doc.getPage()) && !StringUtils.isEmpty(doc.getInstno())) {
	    		if (!reverseOrder){
		    		if(shortFormat) 
		    			display = doc.getInstno() + "_" + doc.getBook() + "_" + doc.getPage();
		    		else 
		    			display = "InstrNo " + doc.getInstno()+ " Book " + doc.getBook() + " and Page " + doc.getPage();
	    		} else {
	    			if(shortFormat) 
		    			display = doc.getBook() + "_" + doc.getPage() + "_" + doc.getInstno();
		    		else 
		    			display = "Book " + doc.getBook() + " and Page " + doc.getPage() + " InstrNo " + doc.getInstno();
	    		}
	    	}

	    	return display;
	    }
	    
	        
 
	    public static String getBookAndPage(Search search, String type, boolean onlyCheckedDocs) {
	    	
	    	String ret = "";
	    	DocumentsManagerI docManager = search.getDocManager();
	    	
	    	try {
	    		docManager.getAccess();
	    		
	    		Collection<DocumentI> documents = docManager.getDocumentsWithDocType(onlyCheckedDocs,type);
	    		
	    		for(DocumentI doc : documents) {
	    			try {
	    				if(!doc.getBook().replaceAll("&nbsp;","").trim().isEmpty()) {  
	    					ret += " ; Book " + doc.getBook() + " and Page " + doc.getPage();
	    				} else if(!doc.getInstno().replaceAll("&nbsp;","").trim().isEmpty()) {
	    					if (doc.getInstno().replaceAll("&nbsp;","").trim().matches("\\d+-\\d+")){
	    						String instrNo = doc.getInstno().replaceAll("&nbsp;","").trim();
	    						ret += " ; Book " + instrNo.replaceAll("(?is)(\\d+)-\\d+", "$1") + " and Page " + instrNo.replaceAll("(?is)\\d+-(\\d+)", "$1");
	    					}
	    				}
	    			}catch(Exception ignored) {}
	    		}
	    		
	    	} catch (Exception e){
	    		e.printStackTrace();
	    	} finally {
	    		docManager.releaseAccess();
	    	}
	    	return ret.replaceFirst(";", "").trim();
	    }
	    
	    public static boolean foundRoDocs(DocumentsManagerI manager, boolean onlyChecked) {
	    	
	    	try{
	    		boolean foundRoDocs = false;
	        	manager.getAccess();
//	            int roDocsSize = manager.getRoLikeDocumentList().size();
//	            
//	            if(roDocsSize>0){
//	            	List<RegisterDocumentI> regDocs = manager.getRoLikeDocumentList();
//	            	
//	            	for(RegisterDocumentI regDoc:regDocs){
//	            		if(	(onlyChecked && regDoc.isChecked()) || !onlyChecked ){
//	    	        		if( !"PA".equalsIgnoreCase(regDoc.getDataSource()) ){
//	    	        			return true;
//	    	        		}else{
//	    	        			if(regDoc instanceof PatriotsI){
//	    	        				PatriotsI patDoc = (PatriotsI)regDoc;
//	    	        				if(patDoc.isHit()){
//	    	        					return true;
//	    		            		}
//	    	        			}
//	    	        		}
//	            		}	
//	            	}
//	            }
	        	
			foundRoDocs = !manager.getRealRoLikeDocumentList(onlyChecked).isEmpty();
			
			//task 9265
			if (!foundRoDocs) {
				for (DocumentI doc : manager.getDocumentsWithDataSource(onlyChecked, "UP")) {
					if (doc instanceof RegisterDocumentI) {
						String docSubType = doc.getDocSubType();
						if (DocumentTypes.OTHERFILES.equalsIgnoreCase(doc.getDocType())
								&& (DocumentTypes.OTHER_FILE_EXC.equalsIgnoreCase(docSubType)
								|| DocumentTypes.OTHER_FILE_REQ.equalsIgnoreCase(docSubType)
								|| DocumentTypes.OTHER_FILE_ESTATE.equalsIgnoreCase(docSubType)
								|| DocumentTypes.OTHER_FILE_COMMENT.equalsIgnoreCase(docSubType))) {
							continue;
						} else {
							foundRoDocs = true;
							break;
						}
					}
				}
			}

			return foundRoDocs;
	        }finally{
	        	manager.releaseAccess();
	        }
	        
	    }
}