package ro.cst.tsearch.log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Map.Entry;

import javax.servlet.ServletException;

import org.apache.log4j.Category;
import org.apache.struts.util.MessageResources;
import org.html.parser.HtmlHelper;
import org.htmlparser.Attribute;
import org.htmlparser.Tag;
import org.htmlparser.tags.BodyTag;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.name.Name;

public class SearchLogPage implements SearchLogPageLogicalLayout {

	private static final Category logger = Category.getInstance(SearchLogFactory.class.getName());

	// private static final String RESOURCE_LOCATION = BaseServlet.REAL_PATH +
	// File.separator +
	// "WEB-INF" + File.separator +
	// "classes" + File.separator +
	// "resource" + File.separator +
	// "utils" + File.separator;
	//	

	// resource
	private static final String CSS_FILE_NAME = 
		BaseServlet.REAL_PATH +
		 File.separator +
		 "WEB-INF" + File.separator +"classes"+ File.separatorChar+  "resource" + File.separatorChar
		 + "utils" + File.separatorChar + "search_log_css";
		
//		SearchLogPage.class.getResource(File.separatorChar
//			+"resource" + File.separatorChar
//			+ "utils" + File.separatorChar + "search_log_css").toString(); 
		
//		new File("src" + File.separatorChar + "resource" + File.separatorChar
//			+ "utils" + File.separatorChar + "search_log_css").getAbsolutePath();

	// FileUtilities.findFileOnClassPath("search_log_css").getAbsolutePath();

	private static final String JS_FILE_NAME = BaseServlet.REAL_PATH +
	 File.separator +
	 "WEB-INF" + File.separator +
	 "classes" + File.separator +
	 "resource" + File.separatorChar+ 
	 "utils" + File.separatorChar + "search_log_js";	
//		new File("src" + File.separatorChar + "resource" + File.separatorChar
//			+ "utils" + File.separatorChar + "search_log_js").getAbsolutePath();
	// FileUtilities.findFileOnClassPath("search_log_js").getAbsolutePath();

	private NodeList rootOfPage;

	private static final MessageResources messageResources = MessageResources
			.getMessageResources("ro.cst.tsearch.log.SearchLogPage");

	/**
	 * Creates the basic HtmlPage <html> <head>
	 * 
	 * </head> <body> </body> </html>
	 * 
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public SearchLogPage() throws InstantiationException, IllegalAccessException {
		NodeList root = HtmlHelper.initiateHtml();
		HtmlHelper.addHead(root);
		HtmlHelper.addBody(root);
		this.setRootOfPage(root);
		createBasicSearchPage(JS_FILE_NAME,CSS_FILE_NAME);
		
	}
	
	SearchLogPage(String jsFile,String cssFile ) throws InstantiationException, IllegalAccessException{
		NodeList root = HtmlHelper.initiateHtml();
		HtmlHelper.addHead(root);
		HtmlHelper.addBody(root);
		this.setRootOfPage(root);
		createBasicSearchPage(jsFile,cssFile);
	}

	private NodeList createBasicSearchPage(String jsFile,String cssFile) {

		try {
			HtmlHelper.createCSSTagFromFile(getRootOfPage(), cssFile);
			HtmlHelper.createJSTagFromFile(getRootOfPage(), jsFile);

			List<Attribute> showDetailsAttributes = new ArrayList<Attribute>();
			showDetailsAttributes.add(HtmlHelper.createAttribute("id", "showHideAllDetails"));
			showDetailsAttributes.add(HtmlHelper.createAttribute("class", "submitLinkBlue"));
			showDetailsAttributes.add(HtmlHelper.createAttribute("onClick", "JavaScript:toggleAllDetails();"));
			HtmlHelper.addSpan(getRootOfPage(), "Show Details", showDetailsAttributes);

			Tag spaces = HtmlHelper.addNumberOfSpace(rootOfPage, 3);

			HtmlHelper.addTagToTag(getRootOfPage(), spaces, BodyTag.class);

			List<Attribute> showPassedAttributes = new ArrayList<Attribute>();
			showPassedAttributes.add(HtmlHelper.createAttribute("id", "showHideAllFailed"));
			showPassedAttributes.add(HtmlHelper.createAttribute("class", "submitLinkBlue"));
			showPassedAttributes.add(HtmlHelper.createAttribute("onClick", "JavaScript:toggleAllFailed();"));
			HtmlHelper.addSpan(getRootOfPage(), "Show Passed", showPassedAttributes);

			HtmlHelper.addTagToTag(getRootOfPage(), spaces, BodyTag.class);

			List<Attribute> showExtraInfoAttributes = new ArrayList<Attribute>();
			showExtraInfoAttributes.add(HtmlHelper.createAttribute("id", "showHideExtraInfo"));
			showExtraInfoAttributes.add(HtmlHelper.createAttribute("class", "submitLinkBlue"));
			showExtraInfoAttributes.add(HtmlHelper.createAttribute("onClick", "JavaScript:toggleAllExtraInfo();"));
			HtmlHelper.addSpan(getRootOfPage(), "Show Extra info", showExtraInfoAttributes);

			HtmlHelper.addTagToTag(getRootOfPage(), spaces, BodyTag.class);

		} catch (Exception e) {
			error(e.getStackTrace().toString());
			logger.error(e);
		}

		return getRootOfPage();
	}

	public synchronized void addNewGrantor(Name name, String currentUser) {

		NodeList appendPlainText;
		try {
			appendPlainText = HtmlHelper.createPlainText("In Search Page, was added a new grantor: ");

			Tag divToBody = HtmlHelper.appendDivToBody(getRootOfPage(), appendPlainText);

			HtmlHelper.appendPlainText(divToBody, " First= ");
			HtmlHelper.addBoldTag(divToBody, name.getFirstName());

			HtmlHelper.appendPlainText(divToBody, ", Middle= ");
			HtmlHelper.addBoldTag(divToBody, name.getMiddleName());

			HtmlHelper.appendPlainText(divToBody, ", Last= ");
			HtmlHelper.addBoldTag(divToBody, name.getLastName());

			HtmlHelper.appendPlainText(divToBody, ",by ");
			HtmlHelper.addBoldTag(divToBody, currentUser);

			HtmlHelper.appendPlainText(divToBody, " at " + SearchLogger.getCurDateTimeCST() + ".");

			HtmlHelper.addTagToTag(divToBody, HtmlHelper.createBR());
		} catch (Exception e) {
			error(e.getStackTrace().toString());
			logger.error(e);
		}
	}

	public synchronized void addChangePageMessage(String currentUser, String fromPage, String toPage) {
		String idDiv = String.valueOf(System.nanoTime()) + "_extraInfo";
		try {
			NodeList appendPlainText = HtmlHelper.createPlainText(messageResources.getMessage("change.location",
					currentUser, fromPage, toPage, getCurDateTimeCST()));
			// removed this because it has no effect on ie and on chrome it made
			// de div content invisible {"style", "display:none"}
			List<Attribute> attributes = HtmlHelper.retrieveAttributeList(new String[][] { { "id", idDiv }, });
			Tag divToBody = HtmlHelper.appendDivToBody(getRootOfPage(), appendPlainText, attributes);
			HtmlHelper.addTagToTag(divToBody, HtmlHelper.createBR());
		} catch (Exception e) {
			error(e.getStackTrace().toString());
			logger.error(e);
		}
	}

	public synchronized void changeValueOperation(String page, String variable, String initialValue, String afterValue,
			String operationAuthor) {
		String[] messageParameters = new String[] { page, variable, initialValue, afterValue, operationAuthor,
				getCurDateTimeCST() };
		String messageKey = "change.value";
		logOperation(messageKey, messageParameters);
	}

	public synchronized void changeValueOperation(String element, String elementProperty, String initialValue,
			String afterValue) {
		String[] messageParameters = new String[] { element, elementProperty, initialValue, afterValue,
				getCurDateTimeCST() };
		String messageKey = "change.variable.property.value";
		logOperation(messageKey, messageParameters);
	}

	public synchronized void setValueOperation(String element, String elementProperty, String initialValue) {
		String[] messageParameters = new String[] { element, elementProperty, initialValue, getCurDateTimeCST() };
		String messageKey = "set.variable.property.value";
		logOperation(messageKey, messageParameters);
	}

	public synchronized void deleteValueOperation(String element, String elementProperty) {
		String[] messageParameters = new String[] { element, elementProperty, getCurDateTimeCST() };
		String messageKey = "delete.variable.property.value";
		logOperation(messageKey, messageParameters);
	}

	public synchronized void deleteAllOperation(String where) {
		String messageKey = "general.delete";
		String[] messageParameters = { where, getCurDateTimeCST() };
		logOperation(messageKey, messageParameters);
	}

	public synchronized void logOperation(String messageKey, String[] messageParameters) {
		try {
			NodeList appendPlainText = HtmlHelper.createPlainText(messageResources.getMessage(messageKey,
					messageParameters));
			Tag divToBody = HtmlHelper.appendDivToBody(getRootOfPage(), appendPlainText);
			HtmlHelper.addTagToTag(divToBody, HtmlHelper.createBR());
		} catch (Exception e) {
			error(e.getStackTrace().toString());
			logger.error(e);
		}

	}

	public synchronized void addValueOperation(String page, String variable, String value, String operationAuthor) {
		String[] messageParameters = new String[] { page, variable, value, operationAuthor, getCurDateTimeCST() };
		String messageKey = "add.value";
		logOperation(messageKey, messageParameters);
	}

	public synchronized void addNewGrantee(String page, Name name, String operationAuthor) {
		String value = "First= " + "<b>" + name.getFirstName() + "</b>, Middle= <b>" + name.getMiddleName()
				+ "</b>, Last= <b>" + name.getLastName() + "</b>";
		addValueOperation(page, AtsStandardNames.VARIABLE_GRANTEE, value, operationAuthor);
	}

	public synchronized void addErrorMessage(String messageKey) {
		try {
			String message = messageResources.getMessage(messageKey);
			List<Attribute> attributeList = HtmlHelper.retrieveAttributeList(new String[][] { { "class", "error" } });
			Tag addSpan = HtmlHelper.addSpan(getRootOfPage(), message, attributeList);
			HtmlHelper.appendTagToBody(getRootOfPage(), HtmlHelper.createBR());
		} catch (Exception e) {
			error(e.getStackTrace().toString());
			logger.error(e);
		}
	}

	public synchronized void assignSearchToSmbd(String role, String identity) {
		String[] messageParameters = { role, identity };
		String message = messageResources.getMessage("assign.search", messageParameters);
		try {
			NodeList createPlainText = HtmlHelper.createPlainText(message);
			HtmlHelper.appendTagToBody(getRootOfPage(), createPlainText);
		} catch (Exception e) {
			e.printStackTrace();
			error(e.getStackTrace().toString());
			logger.error(e);
		}
	}

	public synchronized void addInfoMessage(String messageKey) {
		String message = messageResources.getMessage(messageKey);
		try {
			Tag createBR = HtmlHelper.createBR();
			HtmlHelper.appendTagToBody(getRootOfPage(), createBR );
			Tag createBoldTag = HtmlHelper.createBoldTag(message);
			HtmlHelper.appendTagToBody(getRootOfPage(), createBoldTag);
			HtmlHelper.appendTagToBody(getRootOfPage(), HtmlHelper.createBR());
		} catch (Exception e) {
			e.printStackTrace();
			error(e.getStackTrace().toString());
			logger.error(e);
		}
		
	}

	/*
	 * if (request.getHeader("Referer").contains("property1.jsp")) {
	 * SearchLogger.info("</div><div id='" + idDiv +
	 * "' style=\"display:none\">The user <b>" +
	 * InstanceManager.getManager().getCurrentInstance
	 * (searchId).getCurrentUser().getAttribute(1).toString() +
	 * "</b> was passing from Parent Site to Search Page at " +
	 * SearchLogger.getCurDateTimeCST() +".<BR></div><div>", searchId); } else
	 * if (request.getHeader("Referer").contains("tsdindexpage.jsp")) {
	 * SearchLogger.info("</div><div id='" + idDiv +
	 * "' style=\"display:none\">The user <b>" +
	 * InstanceManager.getManager().getCurrentInstance
	 * (searchId).getCurrentUser().getAttribute(1).toString() +
	 * "</b> was passing from TSR Index to Search Page at " +
	 * SearchLogger.getCurDateTimeCST() +".<BR></div><div>", searchId); }
	 */

	public static String getCurDateTimeCST() {
		FormatDate ASDate = new FormatDate(FormatDate.DISC_FORMAT_2);
		return ASDate.getDate(FormatDate.currentTimeMillis(), TimeZone.getDefault());
	}

	public synchronized void addModuleSearchParameters(String serverName, Object additional, Object info,
			Map<String, String> parameters, String module, boolean typeOfSearch, boolean imageSearch,String user) {
        
        if(Boolean.TRUE != additional){
        	this.addHR();
        }
        Attribute attribute = HtmlHelper.createAttribute("class", "serverName");
        ArrayList<Attribute> attributes = new ArrayList<Attribute>(1);
        attributes.add(attribute);
		try {
			HtmlHelper.addSpan(getRootOfPage(), serverName, attributes);
			HtmlHelper.appendPlainText(getRootOfPage(), BodyTag.class, typeOfSearch? "automatic":"manual");
			if(info!=null){
				HtmlHelper.appendPlainText(getRootOfPage(), BodyTag.class, " - " + info);
				addBR();
			}
			
	        Attribute attribute1 = HtmlHelper.createAttribute("class", "searchName");
	        
	        attributes = new ArrayList<Attribute>(1);
	        attributes.add(attribute1);
			HtmlHelper.addSpan(getRootOfPage(), module, attributes);
			boolean firstTime = true;
			StringBuffer sb = new StringBuffer();
	        for(Entry<String,String> entry : parameters.entrySet() ){
	        	String value = entry.getValue();
	        	value = value.replaceAll("(, )+$",""); 
	        	if(!firstTime){
	        		sb.append(", ");
	        	} else {
	        		firstTime = false;
	        	}
	        	sb.append(entry.getKey() + " = <b>" + value + "</b>");
	        }      
	        HtmlHelper.appendPlainText(getRootOfPage(), BodyTag.class, " by " + sb.toString());
	        if (!typeOfSearch || imageSearch){
	        	addTimeStampLocationUser(user);
	        }
	        HtmlHelper.appendPlainText(getRootOfPage(), BodyTag.class, ":");
	        addBR();
		} catch (Exception e) {
			e.printStackTrace();
			error(e.getStackTrace().toString());
			logger.error(e);
		}
	}
	
	
	 private synchronized void createColapsibleHeader(){
		 String id = String.valueOf(System.nanoTime());
		 String[][] attributePairs =  {{"id",id + "_header"},
				 					   {"class","submitLinkBlue"}, 
				 						{"onClick","JavaScript:toggle('" + id + "');"}};
		List<Attribute> attributeList = HtmlHelper.retrieveAttributeList(attributePairs);
		try {
			
			HtmlHelper.appendDivToBody(getRootOfPage(), messageResources.getMessage("general.show_details"),attributeList);
			
			String[][] attributePairs1 = {{"id",id + "_contents"},
					  {"style","display:none"}};
			
			
//			HtmlHelper.a
//			
//			HtmlHelper.appendDivToBody(getRootOfPage());
			
		} catch (Exception e) {
			e.printStackTrace();
			error(e.getStackTrace().toString());
			logger.error(e);
		}
		
		
//		
//		 "<div id=\"" + id + "_header\" class=\"submitLinkBlue\" onClick=\"JavaScript:toggle('" + id + "');\">Show Details</div>" +
//         "<div id=\"" + id + "_contents\" style=\"display:none\">"
	 }
	 
	 public synchronized void createContentsDiv(){
		 
	 }
	 
	 public synchronized void addTimeStampLocationUser(String user){
		 
	    	String a =" on " + getCurDateTimeCST()
	    			+ ", on server " + URLMaping.INSTANCE_DIR
	    			+", By User:"+user;
			List<Attribute> attributes = new ArrayList<Attribute>(1);
			attributes.add(HtmlHelper.createAttribute("class", "timestamp"));
			try {
				HtmlHelper.addSpan(getRootOfPage(), a , attributes);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
	    }
	
	
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ServletException, ClassNotFoundException, IOException {
//		for (int i = 0; i < 1000; i++) {
		String cssFile = new File("src" + File.separatorChar + "resource" + File.separatorChar
				+ "utils" + File.separatorChar + "search_log_css").getAbsolutePath();
		String jsFile = new File("src" + File.separatorChar + "resource" + File.separatorChar
		+ "utils" + File.separatorChar + "search_log_js").getAbsolutePath();
		SearchLogPage searchLogPage = new SearchLogPage(jsFile,cssFile);
		Name name = new Name("firstName", "middleName", "lastName");
		String currentUser = "Super user";
		searchLogPage.addNewGrantor(name, currentUser);
		searchLogPage.addChangePageMessage(currentUser, AtsStandardNames.PAGE_NAME_PARENT_SITE,
				AtsStandardNames.PAGE_NAME_TSR_INDEX);
		searchLogPage.addNewGrantee(AtsStandardNames.PAGE_NAME_SEARCH_PAGE, name, currentUser);
		searchLogPage.addValueOperation(AtsStandardNames.PAGE_NAME_SEARCH_PAGE, AtsStandardNames.VARIABLE_AGENT,
				"initial", currentUser);
		searchLogPage.changeValueOperation(AtsStandardNames.VARIABLE_ABSTRACTOR,
				AtsStandardNames.VARIABLE_PROPERTY_FILE_ID, "1", "222222222");
		searchLogPage.changeValueOperation(AtsStandardNames.PAGE_NAME_PARENT_SITE, AtsStandardNames.VARIABLE_BUYER,
				"Codrin", "Malin", currentUser);
		searchLogPage.deleteAllOperation(AtsStandardNames.PAGE_NAME_TSR_INDEX);
		searchLogPage.deleteValueOperation(AtsStandardNames.VARIABLE_AGENT, AtsStandardNames.VARIABLE_PROPERTY_FILE_ID);
		searchLogPage.setValueOperation(AtsStandardNames.VARIABLE_AGENT, AtsStandardNames.VARIABLE_PROPERTY_FILE_ID,
				"444444444");
		searchLogPage.addErrorMessage("results.skipped");
		searchLogPage.assignSearchToSmbd(AtsStandardNames.VARIABLE_ABSTRACTOR, "Cos ocs");
		searchLogPage.addInfoMessage("start.search");
		HashMap<String,String> map = new HashMap<String,String>();
		map.put("key1", "value1");
		map.put("key2", "value2");
		map.put("key3", "value3");
		map.put("key4", "value4");
		searchLogPage.addModuleSearchParameters("TNShelbyTR", true, "Info", map, "Nume modul", true, true, currentUser);
		System.out.println(searchLogPage.getRootOfPage().toHtml());
//		searchLogPage=null;
//		}
		System.out.println("END ----------------------------------------------------------------------------------------");
	}

	private synchronized void error(String message) {
		try {
			NodeList appendPlainText = HtmlHelper.createPlainText("An error has occured: " + message);
			Tag divToBody = HtmlHelper.appendDivToBody(getRootOfPage(), appendPlainText);
		} catch (Exception e) {
			logger.error(e);
		}
	}

	public synchronized void setRootOfPage(NodeList rootOfPage) {
		this.rootOfPage = rootOfPage;
	}

	public synchronized NodeList getRootOfPage() {
		return rootOfPage;
	}

	public synchronized void addHR() {
		Tag createHR = HtmlHelper.createHR();
		try {
			HtmlHelper.appendTagToBody(getRootOfPage(),createHR);
		} catch (Exception e) {
			e.printStackTrace();
			error(e.getStackTrace().toString());
			logger.error(e);
		}
	}
	
	public synchronized  void addBR() {
		Tag createHR = HtmlHelper.createBR();
		try {
			HtmlHelper.appendTagToBody(getRootOfPage(),createHR);
		} catch (Exception e) {
			e.printStackTrace();
			error(e.getStackTrace().toString());
			logger.error(e);
		}
	}
	

}
