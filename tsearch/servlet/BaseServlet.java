package ro.cst.tsearch.servlet;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ResourceBundle;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.HttpJspPage;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.LoadConfig;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.exceptions.NotImplFeatureException;
import ro.cst.tsearch.monitor.Counter;
import ro.cst.tsearch.monitor.Time;
import ro.cst.tsearch.templates.UpdateTemplates;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

/**
 * @author nae
 * @see  ro.cst.tsearch.servlet.package-info to see how the authorization process is done. 
 * @deprecated  
 */
public class BaseServlet extends HttpServlet implements HttpJspPage
{
	
	
	protected static final Logger logger = Logger.getLogger(BaseServlet.class);
	
	public volatile static String REAL_PATH = "";
	public static String FILES_PATH = "";
	
	public static volatile Counter hitCounter = new Counter();
	public static volatile Time hitTime = new Time();
	
	public static void incrementHitCounter(){
		hitCounter.increment();
	}
	
	public static void updateHitTime(long hitTime1){
		hitTime.update(hitTime1);
	}
	
	/**
	 * Url Encode the query string parameters
	 * @param parameters
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String encodeParams(String parameters) throws UnsupportedEncodingException {
		String new_params = "?";

		try {
			parameters=parameters.replaceAll("&#([0-9]{2});", "##$1;");
	
		    String[] psplit = (parameters.substring(1)).split("&");
		    for (int i = 0; i < psplit.length; i++) {
				String param= psplit[i];
				String param_contents[] = param.split("=");
				for (int j = 0; j < param_contents.length; j++) {
					String string = param_contents[j];
					string = string.replaceAll("##([0-9]{2});", "&#$1;");
					new_params += URLEncoder.encode(URLDecoder.decode(string,"UTF-8"),"UTF-8") + "=";
				}
				new_params= new_params.substring(0,new_params.length()-1);
				new_params+="&";
			}
		}catch(Exception e) {
			System.err.println("Exception encoding request parameters ");
			e.printStackTrace();
		}
	    return new_params;
	}
	
	/**
	 * Url-encode an url
	 * @param url
	 * @return
	 */
	public static String encodeUrl(String fullUrl) {
		try {
			fullUrl = fullUrl.substring(0,fullUrl.indexOf('?')) +
					  encodeParams(fullUrl.substring(fullUrl.indexOf('?'),fullUrl.length()));
		}catch(Exception e) {
			e.printStackTrace();
		}
		return fullUrl;
	}
	
	public static String encode(String str) {
		try {
			str = URLEncoder.encode(str,"UTF-8");
		}catch(UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return str;
	}
	
	

	//so, after we make a request and got sesion ID expired the we delete the old seesion ID got a new one and repeat the request
	/***********************************************************************************
	 * Entry point into service. Do not modify
	 */
	static {
		logger.warn("Starting the TSearch system..");
		
		System.setProperty("javax.xml.xerces.DocumentBuilderFactory",  "org.apache.crimson.jaxp.DocumentBuilderFactoryImpl");

		System.setProperty( "javax.xml.parsers.DocumentBuilderFactory", "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
		//System.setProperty(parserName, parserImplementation);
    }
	public void doRequest(HttpServletRequest request, HttpServletResponse response) throws Exception
	{		
		try{
			_jspService(request, response);
		}catch(Exception e){
			e.printStackTrace();
			logger.error("Error in jsp page: " , e);
			throw(e);
		}
	}
	
	/**
	 * Notification daemon start code
	 */
	static{
		
		// decide if we start the notification daemon or not
		boolean startTemplateCompilation = ServerConfig.getBoolean("template.compile.daemon.start", true);
				
		// start the templates compile process
		if(startTemplateCompilation) {
			
			(new Thread(new Runnable(){
				private  ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
				final String templatesPath=rbc.getString("templates.path");
				public void run(){
					try{
						logger.warn("--> --> --> START compiling templates ...  "+ templatesPath );
						UpdateTemplates.updateTemplates(templatesPath);
						logger.warn("--> --> --> FINISH compiling templates ..." + templatesPath);
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}
			})).start();
			
		}
	
		try {
			FILES_PATH = ServerConfig.getFilePath();
		} catch (Exception e) {}
		
	}
	
	public final void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		//logger.debug("!START " + this.getClass().getName());
		
		hitCounter.increment();
		long startTime = System.currentTimeMillis();
		
		//init members
		REAL_PATH = getServletConfig().getServletContext().getRealPath("/");
		
		try {
			String searchID = request.getParameter( RequestParams.SEARCH_ID);
			Thread t = Thread.currentThread();
			String threadName = t.getName();
			//clean first
			threadName = threadName.replaceAll(" SearchId: \\-?[0-9]*", "");
			threadName = threadName.replaceAll(" URI: '[^']*'", "");
			
			if(!StringUtils.isEmpty(searchID)) {
				
				if(searchID.startsWith("-")) {
					String newSearchID = request.getParameter( "newSearchId" );
					if(org.apache.commons.lang.StringUtils.isNotBlank(newSearchID)) {
						searchID = newSearchID;
					}
				}
				//modify
				threadName += " SearchId: " + searchID;
			}
			//modify
			threadName += " URI: '" + request.getRequestURI() + "'";
			//rename
			t.setName(threadName);	
		}catch(Exception ignored) {}
		
		try {
			StringBuilder tempBuffer = new StringBuilder();
			if (UserValidation.validate(request, response, FILES_PATH, tempBuffer, (this instanceof CheckAvailabilityServlet))) {
			    
				doRequest(request, response);
				
			} else {	
				if (logger.isDebugEnabled()){
					logger.debug("User validation unsuccessfull");
				}
								
				String pdfFileParam = "";
				if ( request.getParameter( "pdfFile" ) != null ) {
					pdfFileParam = "pdfFile=" + request.getParameter( "pdfFile" );
				}
				
				String displayPdfFileParam = "";
				if ( request.getParameter( "displayPdfFile" ) != null ) {
					displayPdfFileParam = "displayPdfFile" + request.getParameter( "displayPdfFile" );
				}
				
				String pdfFileParameters = ( !"".equals( pdfFileParam ) ? "&" + pdfFileParam : "") + (!"".equals( displayPdfFileParam ) ? "&" + displayPdfFileParam : "");
				
				String commID = request.getParameter(RequestParams.USER_COMMUNITYID);
				String reason = tempBuffer.toString();
				if(StringUtils.isEmpty(reason))
					reason = RequestParamsValues.ERR_NO_INVALID_LOGIN;
				
				if(this instanceof CheckAvailabilityServlet) {
					response.getWriter().append("SESSION EXPIRED");
				}else {
					sendRedirect(request, response, 
							LoadConfig.getLoadBalancingUrl() +":" + LoadConfig.getLoadBalancingPort() + URLMaping.path + URLMaping.LOGIN_PAGE, 
							(commID != null && !"".equals(commID) 
									? "?" + RequestParams.USER_COMMUNITYID + "=" + commID + 
											pdfFileParameters : pdfFileParameters.replaceFirst( "&" , "?")) + "&" +
											RequestParams.PARAM_ERROR_MESSAGE + "=" + reason);
				}
			}
			
		}

        catch( InterruptedException ie )
        {
            //ignored
        }
        catch( javax.servlet.ServletException se )
        {
            se.printStackTrace();
        }
        catch (Exception e) {
			handleException(request, response, e);
		}
		
		hitTime.update(System.currentTimeMillis() - startTime);
		
		//logger.debug("!END " + this.getClass().getName());
	}

	public static void handleException(HttpServletRequest request, HttpServletResponse response, Exception e) throws ServletException, IOException {
		logger.error("ERROR!!!",e);
		
        String exceptionHandled = (String) request.getAttribute( "exceptionHandled" );
        //disable forwarding to error page if handleException flag set tot "1"
        
        if( exceptionHandled == null )
        {
    		if (e instanceof NotImplFeatureException){
    			request.setAttribute("msg", e.getMessage());
                request.setAttribute( "exceptionHandled", "1" );
    			forward(request, response, URLMaping.NOT_IMPLEMENTED_PAGE);
    		}else{
    			request.setAttribute("error", e);			
    			e.printStackTrace();
    			Log.sendExceptionViaEmail(e);
                request.setAttribute( "exceptionHandled", "1" );
    			forward(request, response, URLMaping.ERROR_PAGE);
    		}
        }
	}
    	
	protected BaseServlet()
	{}
	private ServletConfig config;
	public final void init(ServletConfig config) throws ServletException
	{
		this.config= config;
		jspInit();
	}
	public final ServletConfig getServletConfig()
	{
		return config;
	}
	/* (non-Javadoc)
	 * @see javax.servlet.jsp.JspPage#jspInit()
	 */
	public void jspInit()
	{
		// TODO Auto-generated method stub
	}
	/* (non-Javadoc)
	 * @see javax.servlet.jsp.HttpJspPage#_jspService(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void _jspService(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException
	{
		// TODO Auto-generated method stub
	}
	/* (non-Javadoc)
	 * @see javax.servlet.jsp.JspPage#jspDestroy()
	 */
	public void jspDestroy()
	{
		// TODO Auto-generated method stub
	}
	public void destroy()
	{
		jspDestroy();
		logger.info("Destroy..");
	}
	/*********************************************************************************************
	 * 
	 * @author nae
	 *
	 */
	public static void forward(HttpServletRequest request, HttpServletResponse response, String address) throws ServletException, IOException
	{
		
		//gets the RequestDispatcher of this servlet
		RequestDispatcher dispatcher= request.getRequestDispatcher(address);
		
		//forwards this request to retrived URL
		
		try {
		
			dispatcher.forward((ServletRequest) request, (ServletResponse) response);
		
		} catch (SocketException se) {
			se.printStackTrace();
		} catch (IllegalStateException ise) {
			ise.printStackTrace();
		}
	}
	public static void sendRedirect( HttpServletRequest request,HttpServletResponse response, String path,String parameters) throws IOException
	{
		if (logger.isDebugEnabled()){
			logger.debug ("enter send Redirect path" + path + " para=" +parameters);
		}


		if (parameters==null)
			parameters="?";
		else//not null
		{
			parameters=parameters.trim() ;
			if (parameters.length()==0) {
				if(path.contains("?")) {
					parameters="&";
				} else {
					parameters="?";
				}
				
			}
			else//has at least one parameter
			{
				if (parameters.indexOf("?")!=0)
					parameters= "?" + parameters;
				parameters+= "&";
			}
		}
		String searchID=request.getParameter( RequestParams.SEARCH_ID);
		if (searchID!=null) {
			if(!path.contains(RequestParams.SEARCH_ID) && !parameters.contains(RequestParams.SEARCH_ID)) {
				parameters += RequestParams.SEARCH_ID + "=" + searchID;
			}
		}
		
		if(path.contains("?")) {
			parameters = path.substring(path.indexOf("?")) + parameters;
			path = path.substring(0,path.indexOf("?"));
		}

		response.sendRedirect( path + encodeParams(parameters) );
	}
	
	public static void sendRedirect(HttpServletRequest request, HttpServletResponse response, String path) throws IOException
	{
		sendRedirect(request,response,path,"");
	}	
}
