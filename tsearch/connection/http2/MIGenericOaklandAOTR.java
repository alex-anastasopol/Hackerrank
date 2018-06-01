package ro.cst.tsearch.connection.http2;

import static ro.cst.tsearch.connection.http.HTTPRequest.GET;
import static ro.cst.tsearch.connection.http.HTTPRequest.POST;
import static ro.cst.tsearch.utils.StringUtils.extractParameter;
import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.parser.SimpleHtmlParser;
import ro.cst.tsearch.parser.SimpleHtmlParser.Form;
import ro.cst.tsearch.parser.SimpleHtmlParser.Input;
import ro.cst.tsearch.parser.SimpleHtmlParser.Option;
import ro.cst.tsearch.parser.SimpleHtmlParser.Select;
import ro.cst.tsearch.utils.StringUtils;

public abstract class MIGenericOaklandAOTR extends HttpSite {

	/*
	 * Attributes used during login: 
	 * 	  prodSelParams
	 *    COM_PRODUCT - Product
	 * 	  RES_PRODUCT - Product
	 * 	  TAX_PRODUCT - Product
	 * 
	 * Attributes used during search:
	 * 	  COM_PRODUCT_1 - template parameters
	 *    COM_PRODUCT_2 - template parameters
	 * 	  RES_PRODUCT_1 - template parameters
	 *    RES_PRODUCT_2 - template parameters
	 *    TAX_PRODUCT_1 - template parameters
	 * 	  TAX_PRODUCT_2 - template parameters
	 */
	
	private static final String MAIN_SERVLET = "http://www.oakgov.com/aofs0001/MainServlet";
	
	public static final String SRC_PRODUCT  = "Search Product";
	public static final String TAX_PRODUCT  = "T_Current Tax Profile";
	public static final String RES_PRODUCT  = "S_Residential Property Profile";
	public static final String COM_PRODUCT  = "S_Commercial and Industrial Property Profile";
				
	protected class Product {
		
		public final String name;
		public final String type;
		public final int index;
		public final String id;
		
		public Product(String name, String type, int index, String id){
			this.name = name;
			this.type = type;
			this.index = index;
			this.id = id;
		}
		
		public String toString(){
			return "Product(name=" + name + ",type=" + type + ",index=" + index + ",id=" + id + ")";
		}
	}
	
	@SuppressWarnings("unchecked")
	protected boolean selectProduct(String productString){

		// get product ids
		Product product = (Product)getAttribute(productString);
		if(product == null){
			return false;
		}
		
		// get product details
		HTTPRequest req = new HTTPRequest(MAIN_SERVLET, POST);		
		Map<String,String> prodSelParams = (Map<String,String>)getAttribute("prodSelParams");
		for(Map.Entry<String, String> entry: prodSelParams.entrySet()){
			String name = entry.getKey();
			String value = entry.getValue();
			if(product.type.equals("S") && name.equals("sel_SI_prod_usage_id")){
				value = product.id;
			} else if(product.type.equals("T") && name.equals("sel_TI_prod_usage_id")){
				value = product.id;
			} else if(name.equals("sel_prod_usage_typ")){
				value = product.type;
			} else if(name.equals("sel_prod_index")){
				value = "" + product.index;
			} else if(name.equals("action")){
				value = "DisplayProductPage";
			}
			req.setPostParameter(name, value);
		}		
		String prodPage = execute(req);
		dbgWriteFile(product.type + product.id + ".html", prodPage);
		
		// parse product parameters for all modules
		SimpleHtmlParser parser = new SimpleHtmlParser(prodPage);		
		int idx = 1;
		for(Form form: parser.forms){
			if(form.action.equals("findit.cfm")){
				Map<String,String> params = new LinkedHashMap<String,String>();
				for(Input input: form.inputs){
					if(input.type.equals("image")){
						params.put(input.name + ".x", "48");
						params.put(input.name + ".y", "18");
					} else {
						params.put(input.name, input.value);
					}
				}
				for(Select select: form.selects){
					params.put(select.name, "");
				}
				setAttribute(productString + "_" + idx, params);
				idx++;
			}
		}
		
		return true;
	}
	
	/**
	 * Used for debugging
	 * @param fileName
	 * @param text
	 */
	protected void dbgWriteFile(String fileName, String text){
		//FileUtils.writeTextFile("d:/" + fileName, text);
	}

	/**
	 * Setup the products that will be used
	 * @return
	 */
	abstract boolean setupProducts();
	
	@Override
	public LoginResponse onLogin(){

		// get username and password from database
		String userName = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), getClass().getSimpleName(), "user_" + sid);
		String password = SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(), getClass().getSimpleName(), "password_" + sid);

		HTTPRequest req = new HTTPRequest("https://www.oakgov.com/aofs0001/index.jsp", GET);
		String p1 = execute(req);
		dbgWriteFile("p1.html", p1);
		
				
		req = new HTTPRequest("https://www.oakgov.com/aofs0001/MainServlet", POST);
		req.setPostParameter("currpage", "login");
		req.setPostParameter("nextpage", "MainMenu");
		req.setPostParameter("username", userName);
		req.setPostParameter("password", password);
		String p2 = execute(req);
		dbgWriteFile("p2.html", p2);
		
		String mainMenu = "";
		
		if (p2.contains("Concurrent use of a user name is not allowed")) {
			
			req = new HTTPRequest(MAIN_SERVLET, POST);
			req.setPostParameter("currpage", "ConcurrentUser");
			req.setPostParameter("nextpage", "ConcurrentUser");
			req.setPostParameter("process", "process");
			req.setPostParameter("action", "CCU");
			req.setPostParameter("help_page", "/accessok/help/aofs0001.html");
			String p3 = execute(req);
			dbgWriteFile("p3.html", p3);
			if(!p3.contains("You can click the 'Purge' button below to remove the active session for your user name")){
				logger.error("Could not log in: purge - after CCU");
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not log in: purge - after CCU");
			}
			
			req = new HTTPRequest(MAIN_SERVLET, POST);
			req.setPostParameter("currpage", "ConcurrentUser");
			req.setPostParameter("nextpage", "ConcurrentUser");
			req.setPostParameter("process", "process");
			req.setPostParameter("action", "PCU");
			req.setPostParameter("help_page", "/accessok/help/aofs0001.html");
			String p4 = execute(req);
			dbgWriteFile("p4.html", p4);
			if(!p4.contains("Concurrent User has been purged. Click 'Continue' to continue with this account.")){
				logger.error("Could not log in: purge - after PCU");
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not log in: purge - after PCU");
			}
			
			req = new HTTPRequest(MAIN_SERVLET, POST);
			req.setPostParameter("currpage","ConcurrentUser");
			req.setPostParameter("nextpage","MainMenu");
			req.setPostParameter("process","");
			req.setPostParameter("action","");
			req.setPostParameter("help_page","/accessok/help/aofs0001.html");
			String p5 = execute(req);
			dbgWriteFile("p5.html", p5);
			mainMenu = p5;
									
		} else {			
			
			String link = extractParameter(p2, "<meta http-equiv=\"refresh\" content=\"0;url=([^\t\r\n\"]+)\\s*\">");						
			if(isEmpty(link)){
				logger.error("Could not log in!");
				return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Invalid page");
			}
			
			req = new HTTPRequest(link, GET);
			String p6 = execute(req);
			dbgWriteFile("p6.html", p6);
			mainMenu = p6;
		}
		
		if(!mainMenu.contains("Home Page</title>")){
			logger.error("Could not get main menu!");
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not get main menu!");
		}

		// go to my products
		SimpleHtmlParser parser = new SimpleHtmlParser(mainMenu);
		Form form = parser.getForm("process_form");
		if(form == null){
			logger.error("Could not find the process_form in main menu page");
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not find the process_form in main menu page");
		}
		req = new HTTPRequest(MAIN_SERVLET, POST);
		for(Input input: form.inputs){
			if(!StringUtils.isEmpty(input.name)){
				if(input.name.equals("nextpage") && input.value.equals("")){
					input.value = "USERACCTPRODUCTS";
				}
				req.setPostParameter(input.name, input.value);
			}
		}
		String p7 = execute(req);
		dbgWriteFile("p7.html", p7);
		String myProducts = p7;
		
		/*
		 * parse my products
		 */ 
		parser = new SimpleHtmlParser(myProducts);
		form = parser.getForm("process_form");
		if (form == null) {
			logger.error("Could not find the process_form in main menu page");
			return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, "Could not find the process_form in main menu page");
		}
		
		// isolate tax profile id
		Select selectTi = form.getSelect("sel_TI_prod_usage_id");
		if(selectTi != null){
			int index = 0;
			for(Option option: selectTi.options){
				if(!option.value.equals("")){
					Product product = new Product(option.text, "T", index, option.value);
					setAttribute("T_" + option.text, product);
				}
				index++;
			}
		}
		
		// isolate residential profile id
		Select selectSi = form.getSelect("sel_SI_prod_usage_id");
		if(selectSi != null){
			int index = 0;
			for(Option option: selectSi.options){
				if(!option.value.equals("")){
					Product product = new Product(option.text, "S", index, option.value);
					setAttribute("S_" + option.text, product);
				}				
				index ++;
			}
		}
		
		// isolate params for product selection
		Map<String,String> prodSelParams = new LinkedHashMap<String,String>();
		for(Input input: form.inputs){
			prodSelParams.put(input.name, input.value);			
		}
		for(Select select: form.selects){
			prodSelParams.put(select.name, "");
		}
		setAttribute("prodSelParams", prodSelParams);
		
		if(setupProducts()) {
			return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
		}
		
		return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBeforeRequest(HTTPRequest req){
		
		// don't do anything during login
		if(status != STATUS_LOGGED_IN){
			return;
		}
		
		// don't do anything while we're already inside a onBeforeRequest call
		if(getAttribute("onBeforeRequest") == Boolean.TRUE){
			return;
		}
		
		// mark that we're treating onBeforeRequest
		setAttribute("onBeforeRequest", Boolean.TRUE);
		try{
			
			// add the template parameters, obtained during login
			String productId = req.getPostFirstParameter(SRC_PRODUCT); 
			if(productId != null){
				req.removePostParameters(SRC_PRODUCT);
				Map<String,String> templateParams = (Map<String,String>)getAttribute(productId);
				if(templateParams == null){
					throw new RuntimeException("Cannot find product template: " + productId);
				}
				for(Map.Entry<String, String> entry: templateParams.entrySet()){
					String name = entry.getKey();
					String value = entry.getValue();
					if(!req.hasPostParameter(name)){
						req.setPostParameter(name, value);
					}					
				}
			}
			
			HTTPResponse httpResponse = process(req);
			String htmlResponse = httpResponse.getResponseAsString();
			if(htmlResponse.contains("this is data source aof")){
				setDestroy(true);
				throw new RuntimeException("Session is no longer valid on OAKLAND AO/TR");
			} else {
				httpResponse.is = IOUtils.toInputStream(htmlResponse);
				req.setBypassResponse(httpResponse);				
			}
			
		} finally {
			setAttribute("onBeforeRequest", Boolean.FALSE);
		}
	}
}
