package ro.cst.tsearch.servlet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.generic.IOUtil;
import ro.cst.tsearch.jsp.utils.RepTopBar;
import ro.cst.tsearch.jsp.utils.TopBar;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servlet.download.DownloadFile;
import ro.cst.tsearch.templates.AddDocsTemplates;
import ro.cst.tsearch.templates.GlobalTemplateFactory;
import ro.cst.tsearch.templates.Template;
import ro.cst.tsearch.templates.TemplateBuilder;
import ro.cst.tsearch.templates.TemplateUtils;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

public class FileServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	public static final int VIEW_ORDER = 1;
	public static final int VIEW_LOG = 2;
	public static final int VIEW_INDEX = 3;
	public static final int VIEW_LOG_OLD_STYLE = 6;
	
	public static final Pattern productPattern = Pattern.compile( "(?is)<TR.*?frametitlerow.*?Product: <I>(.*?)</I>.*?</TR" );
	
	private static String extractExtension( String fileName ){
		
		int poz = fileName.lastIndexOf(".");
		String ext ="";
		
		if( poz>0 && poz+1<fileName.length()-1 ){
			ext = fileName.substring(poz+1);
		}
		
		return ext;
		
	}
	
	private static Pattern pat = Pattern.compile("(?is)<a[^/]+href=['\"]/title-search/URLConnectionReader?[^'\"]*searchId=([0-9]+)[^'\"]*['\"][^>]*>");
	
	
	public final void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String showLog = request.getParameter("showLog");

		String searchIdStr = request.getParameter( "searchId" );
		String viewOption = request.getParameter( "view" );
		request.getQueryString();
		String fileName = request.getParameter("f");
		
		//System.err.println("FILE SERVLET:  " + searchIdStr);
		if( fileName != null ){
			if(fileName.contains("?searchId=")&&fileName.contains("logo.gif")){
				searchIdStr = fileName.substring(fileName.indexOf("?searchId=")+"?searchId=".length());
				fileName = fileName.substring(0,fileName.indexOf("?searchId="));
			}
		}
		
		String viewDescription = request.getParameter("viewDescription");
		String viewOrder = request.getParameter( "viewOrder" );
		String forceDownload = request.getParameter(RequestParams.FORCE_DOWNLOAD);
		
			
		if(searchIdStr==null){
			int poz=fileName.indexOf(File.separator);
			int pozStart = 0;
			int pozStop = 0;
			if(poz == -1) {
				poz = fileName.indexOf("/");
				pozStart = fileName.indexOf("/",poz+1);
				pozStop = fileName.indexOf("/",pozStart+1);
			} else {
				pozStart = fileName.indexOf(File.separator,poz+1);
				pozStop = fileName.indexOf(File.separator,pozStart+1);
			}
			searchIdStr = fileName.substring(pozStart+1,pozStop);
		}
		
		long searchId = Long.parseLong(searchIdStr);
		
		if("true".equals(request.getParameter(com.stewart.ats.tsrindex.client.shared.RequestParams.COMPILE_DOC_TEMPLATE))) {
			try {
				long userId = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getID().longValue();
				Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				
				Map<String,String> boilerPlatesTSR = TemplateUtils.getBoilerText(searchId, userId, request);
				TemplateUtils.compileTemplate( searchId, userId, fileName, false, null, null, false,boilerPlatesTSR);
				fileName = (search.getSearchDir()+ File.separator + Search.TEMP_DIR_FOR_TEMPLATES + File.separator + fileName).replaceAll("//", "/");
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if ("true".equals(request.getParameter(com.stewart.ats.tsrindex.client.shared.RequestParams.PREVIEW_TSRI))) {
			try {
				Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				long userId = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getID().longValue();
				
				SearchAttributes sa = search.getSa();
				
				
				Template templateByName = GlobalTemplateFactory.getInstance().getTemplateByName(ServerConfig.getPreviewTSRIndexName());
				
				State state = State.getState(new BigDecimal(sa.getAtribute(SearchAttributes.P_STATE)));
				County county = County.getCounty(new BigDecimal(sa.getAtribute(SearchAttributes.P_COUNTY)));
				CommunityAttributes ca = InstanceManager.getManager().getCurrentInstance(sa.getSearchId()).getCurrentCommunity();
				
				//on preview fill all codes for docs
				TemplateUtils.fillDocumentBPCodes(searchId,userId);
				
				HashMap<String, Object> templateParams = TemplateBuilder.fillTemplateParameters(search, ca, county, state, false, false, null);
							
				
				fileName = search.getSearchDir() + File.separator + Search.TEMP_DIR_FOR_TEMPLATES + File.separator + ServerConfig.getPreviewTSRIndexName();
				fileName = fileName.replaceAll("//", "/");
				
				
				AddDocsTemplates.completeNewTemplatesV2New(
						templateParams,
						fileName,
						templateByName.getTemplateAsCommunityTemplatesMapper(),
						search,
						true, null, null, new HashMap<String, String>(), false);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		} else if(showLog!=null && searchIdStr!=null){
			try {
				
				Search crtSearchContext = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
				
				if(crtSearchContext.getSa().isLogInDatabase() && ServerConfig.isEnableLogInSamba()) {
					//force database log
					fileName = null;
					viewOption = VIEW_LOG+"";
				} else {
					fileName = crtSearchContext.getSearchDir()+"logFile.html";
					if(!new File(fileName).exists()) {
						fileName = null;
						viewOption = VIEW_LOG+"";
					}
				}
			}catch(Exception e) {
				fileName = null;
				viewOption = VIEW_LOG+"";
			}
		}
		
		HttpSession session = request.getSession(true);
		
		boolean fileFound = false;
		String favIco = "<link rel='shortcut icon' href='/title-search/favicon.ico' type='image/x-icon'>" + 
		"<link rel='icon' href='/title-search/favicon.ico' type='image/x-icon'>";
		
		if (fileName != null) {
			
			fileName = fileName.replaceAll("\\.\\.", "");
			
			String ext = extractExtension( fileName );
			
			final boolean isImage = ext.equalsIgnoreCase("pdf") || 
				ext.equalsIgnoreCase("tif")  || 
				ext.equalsIgnoreCase("tiff") || 
				ext.equalsIgnoreCase("doc") ||
				ext.equalsIgnoreCase("gif") ||
				ext.equalsIgnoreCase("jpg") ;
			
			File file = new File(BaseServlet.FILES_PATH + File.separator + fileName);
			
			if(!file.exists() && fileName.startsWith(BaseServlet.FILES_PATH)){
				file = new File(fileName);
			}
            
			
			
			if (file.exists()) {
				if (fileName.contains("logFile.html"))
					showLog = "true";
                
                response.setContentType(DownloadFile.getSpecificExt(file.getName()));
                if (forceDownload != null && !forceDownload.equals("0")){
                	response.setHeader(
                            "Content-Disposition",
                            " attachment; filename="
                                + file.getName());
                } else {
                	response.setHeader(
                        "Content-Disposition",
                        " inline; filename=\""
                            + file.getName()
                            + "\"");
                }
                int length = 0;
                InputStream source = null;
                
                
				if( 			viewDescription == null && 
                				viewOrder == null && 
                				!isImage && !fileName.contains("logFile.html")		) {
                	
    				FileInputStream fis = new FileInputStream(file);
    				Long oldSearchId =  -1l;
    				String fileContents = "";
    				try {
    					fileContents = org.apache.commons.io.FileUtils.readFileToString(file);
    					
    					boolean addedLinks = false;
    					
    					//bug 7163
    					int indexOfIcoSquareHome = fileContents.indexOf("ico_square_home.gif");
    					
    					if(indexOfIcoSquareHome==-1)
	    					try {System.err.println(">>>>>>>>>>>>>>>>>>>>>>>> indexOfIcoSquareHome==-1");
	    			  			Search search = SearchManager.getSearch(searchId,false);
	    			  			
	    			  			if(search == null ){
	    			  				search = SearchManager.getSearchFromDisk(searchId);
	    			  			}
	    			  			
	    			  			if (search != null){
	    			  				String part = "Product:";
	    			  				
	    			  				String[] parts = fileContents.split(part);
	    			  				
	    			  				if(parts.length==2){
		    			  				TopBar top = new RepTopBar( search ,"File View", "", "chapter00","about.htm", true, true, "", request);
		    			  				
		    			  				String links = top.getLinks(false, true, true, false, true, false);
		    			  				
		    			  				String first_part = parts[0];
		    			  				String sec_part = parts[1];
		    			  				
		    			  				first_part = first_part.replaceAll("(?ism)(<TD class=\"frameTitleRow\" colspan=\"2\">)",
		    			  								"$1<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\"><tr class=\"frameTitleRow\"><td align=\"left\">");
		    			  				sec_part = sec_part.replaceFirst("(?ism)</TD>", "</td><td align=\"right\">"+links + "</td></tr></table></td>");
		    			  				
		    			  				fileContents = first_part + part + sec_part;
		    			  				
		    			  				fileContents = fileContents.replaceFirst("(?ims)(</table></td>)<TD align=\"right\">.*<img src='/title-search/web-resources/images/spacer.gif' width='2' height='17' border='0' align='absmiddle'></TD>", 
		    			  									"$1");
		    			  				
		    			  				addedLinks = true;
		    			  				System.err.println(">>>>>>>> Added Links for Old Order");
	    			  				}	
	    			  				else
									{
										Pattern p = Pattern.compile("(?is)emaill.jsp[^()]*(curpageurl=.*?)&");
										Matcher m = p.matcher(fileContents);
										if (m.find() && m.group(1).equalsIgnoreCase("CurPageURL="))
										{
											TopBar top = new RepTopBar(search, "File View", "", "chapter00", "about.htm", true, true, "", request);
											String emailLink = top.getLinks(false, false, true, false, false, false);
											p = Pattern.compile("(?is)<a[^>]*emailSwapImage[^>]*>.*?</a><img[^>]*>");
											m = p.matcher(fileContents);
											if (m.find())
											{
												fileContents = fileContents.replaceFirst(p.toString(), emailLink);
												addedLinks = true;
											}
										}
									}	
	    			  			}
	    					} catch (Exception e) {
	    						e.printStackTrace();
	    					}
	    					else {
	    						System.err.println(">>>>>>>>>>>>>>>>>>>>>>>> indexOfIcoSquareHome!=-1");
	    						String firstPart = fileContents.substring(0,indexOfIcoSquareHome); 
	    						String secondPart = fileContents.substring(indexOfIcoSquareHome); 
	    						
	    						try{
		    						firstPart = firstPart.substring(0,firstPart.lastIndexOf("<a"));
		    						secondPart = secondPart.substring(secondPart.indexOf("</a>")+4);
		    						fileContents = firstPart + secondPart;
		    						addedLinks = true;
	    						} catch (Exception e) {
	    							e.printStackTrace();
								}
	    					}
    					
    					
	    				Matcher mat1 = pat.matcher(fileContents);
	    				boolean shouldBeOrder = fileContents.contains("Reopening this search will delete the existing TSR.");
	    				boolean foundOldSearchId = mat1.find();
	    				fileContents.replaceAll("<head>", "<head>" +
						favIco );
	    				if( shouldBeOrder || foundOldSearchId ){
	                     	//hope it is order :)
	                     	//we must replace old searchIds with the new one
	     					try{
	     						Pattern pat = Pattern.compile("searchId=([1-9][0-9]*)");
	     						Matcher mat = pat.matcher(fileContents);
	     						if(mat.find()){
	     							String idStr=mat.group(1);
	     							oldSearchId = Long.parseLong(idStr);
	     						}
	     					}
	     					catch(Exception e){
	     						e.printStackTrace();
	     					}
	     					if(oldSearchId != -1 && oldSearchId != searchId){
	     						fileContents = fileContents.replaceAll("searchId="+oldSearchId, "searchId="+searchIdStr);
	     						System.err.println("FILESERVLET: replacing " + oldSearchId + " with " + searchIdStr);
	     						if(shouldBeOrder){
	     							//do not delete this... again
	     							fileContents = fileContents.replaceAll(String.valueOf(oldSearchId), searchIdStr);
	     						}
	     					}
	    				 } else {
	    					 if(!addedLinks)
	    						 fileContents = "";
	    				 }

    				} catch (Exception e) {
						e.printStackTrace();
					} finally {
						
					}
    				
    				if(StringUtils.isEmpty(fileContents)){
    					length = (int) file.length();
	                    source = fis;
    				} else {
    					length = fileContents.length();
                        
                        ByteArrayInputStream bais = new ByteArrayInputStream( fileContents.getBytes() );
                        
                        source = bais;
    				}
                    
                }
                else if( viewOrder != null ){
                	
                    String fileContents = FileUtils.readFile( file.getAbsolutePath() );
                    
                    Matcher productTypeMatcher = productPattern.matcher( fileContents );
                    String productType = "";
                    
                    if( productTypeMatcher.find() ){
                    	productType = productTypeMatcher.group( 1 );
                    }
                    
                    long userId = -1;
                    try{
                    	userId = Long.parseLong( request.getParameter( "userId" ) );
                    }catch( Exception e ){
                    	e.printStackTrace();
                    }
                    
                    try{	
	                    	//topBar and reptopbar require stuff set on current instance
	                    	User currentUser = new User(UserValidation.getDirPath(BaseServlet.FILES_PATH));
	                    	currentUser.setUserAttributes( UserUtils.getUserFromId( userId ) );
	                    	
	                    	CurrentInstance ci =InstanceManager.getManager().getCurrentInstance(searchId);
	                    	synchronized (ci) {
	                    		if( ci.getCurrentUser()==null&& ci.getCrtSearchContext()==null ){
	                    			//User currentUser = new User(UserValidation.getDirPath(BaseServlet.FILES_PATH));
			                    	currentUser.setUserAttributes( UserUtils.getUserFromId( userId ) );
			                    	ci.setup( currentUser , request, response, session);
			                    	Search search = new Search(searchId);
			                    	ci.setCrtSearchContext(search);
	                    		}
							}
	                    	
	                    	
                    }catch( Exception e ){
                    	e.printStackTrace();
                    }

                    fileContents = fileContents.replaceAll( "<head>" , "<head>" +
                    										favIco +
                    										"<SCRIPT language=\"JavaScript\" src=\"" + URLMaping.JAVASCRIPTS_DIR + "/menucode.js\"></SCRIPT>"+ 
                    										"<SCRIPT language=\"JavaScript\" src=\"" + URLMaping.JAVASCRIPTS_DIR + "/frameRollOver.js\"></SCRIPT>" +
                    										"<SCRIPT language=\"JavaScript\" src=\"" + URLMaping.JAVASCRIPTS_DIR + "/random.js\"></SCRIPT>" +
                    										"<SCRIPT language=\"JavaScript\" src=\"" + URLMaping.JAVASCRIPTS_DIR + "/validate.js\"></SCRIPT>" +
                    										"<SCRIPT language=\"JavaScript\" src=\"" + URLMaping.JAVASCRIPTS_DIR + "/bw_info.js\"></SCRIPT>" +
                    										"<SCRIPT language=\"JavaScript\" src=\"" + URLMaping.JAVASCRIPTS_DIR + "/calendarPopup.js\"></SCRIPT>" + 
                    										"<SCRIPT language=\"JavaScript1.2\" src=\"/title-search/web-resources/javascripts/tooltip.js\" type=\"text/javascript\"></SCRIPT>" +
                    										"<SCRIPT language=\"JavaScript1.2\" src=\"/title-search/web-resources/javascripts/tooltipstyle.js\" type=\"text/javascript\"></SCRIPT>" +
                    										"<SCRIPT language=JavaScript src=\"/title-search/web-resources/javascripts/mm_menu.js\"></SCRIPT>" +
                    										"<SCRIPT language=JavaScript src=\"/title-search/web-resources/javascripts/bw_info.js\"></SCRIPT>");
                    
                    fileContents = fileContents.replaceAll("(?is)<TR.*?frametitlerow.*?</TR>", "<tr class=frameTitleRow><td>" +
                    										(new RepTopBar( "View Order Transaction Type: " + productType, "", "chapter03","helpfile04.htm#23", true, true,request)).toString(searchId) + 
                    										"</td></tr>");
                    if(fileContents.contains("Reopening this search will delete the existing TSR.")){
                    	//hope it is order :)
                    	//we must replace old searchIds with the new one
    					Long oldSearchId =  -1l;
    					try{
    						Pattern pat = Pattern.compile("searchId=([1-9][0-9]*)");
    						Matcher mat = pat.matcher(fileContents);
    						if(mat.find()){
    							String idStr=mat.group(1);
    							oldSearchId = Long.parseLong(idStr);
    						}
    					}
    					catch(Exception e){
    						e.printStackTrace();
    					}
    					if(oldSearchId != -1 && oldSearchId != searchId){
    						fileContents = fileContents.replaceAll(String.valueOf(oldSearchId), searchIdStr);
    					}
                    }
                    
                    length = fileContents.length();
                    ByteArrayInputStream bais = new ByteArrayInputStream( fileContents.getBytes() );
                    
                    source = bais;
                }
                else {
                	if ( 	!isImage	) 
                    {
                		String fileContents = "";                		
                		if(file.getAbsolutePath().contains("logFile")){
                			fileContents = FileUtils.readFilePreserveNewLines( file.getAbsolutePath() );                			
                		} else {
                			fileContents = FileUtils.readFile( file.getAbsolutePath() );
                		}
                        
                		
                        fileContents = fileContents.replaceAll( "(?is)</?html>", "" );
                        fileContents = fileContents.replaceAll( "(?is)</?body>", "" );
                        
                        // fix bug# 750
                        fileContents = fileContents.replaceAll( "<hr>", "");
                        
                        fileContents = fileContents.replaceAll( "\"\\./fs\\?", "\"../../fs?" );
                        
                        fileContents = "<div>" + fileContents + "</div>";
                        	
                        fileContents = fileContents.replaceAll("<head>", "<head>" +  favIco);
                        length = fileContents.length();
                        
                        ByteArrayInputStream bais = new ByteArrayInputStream( fileContents.getBytes() );
                        
                        source = bais;
                    }
                	else{
                		length = (int) file.length();
                        source = new FileInputStream(file);
                	}
                }
                
                
//                create
				if (!isImage){ 
	                String readInputStream = FileUtils.readInputStream(source);
	                readInputStream = readInputStream.replaceAll("<head>", "<head>" +
	                    										favIco 
	                    										);
	                
	                source = new ByteArrayInputStream(readInputStream.getBytes("UTF-8"));
	                length = readInputStream.length();
				}
				
				if("true".equals(showLog)){
					//bug 7235
					String contents = org.apache.commons.io.IOUtils.toString(source);
					source = new ByteArrayInputStream(SearchLogger.updateLogHeader(searchId, new String(contents)).getBytes());
				}
				
                response.setContentLength( length);
                IOUtil.copy(source, response.getOutputStream());
                fileFound = true;
			}
			
		}
		else if ( viewOption != null ){
			int view = 0;
			try{
				view = Integer.parseInt( viewOption );
				
				byte[] dataToDisplay;
				long startTMS = System.currentTimeMillis();
				dataToDisplay = DBManager.getSearchOrderLogs(searchId, view, true);
				long endTMS = System.currentTimeMillis();
				System.err.println("timeSpent in getSearchOrderLogs miliseconds " + ((endTMS - startTMS)));
				if(view == VIEW_ORDER){
					//we must replace old searchIds with the new one
					String fileString = new String(dataToDisplay);
					
					Long oldSearchId =  -1l;
					try{
						Pattern pat = Pattern.compile("searchId=([1-9][0-9]*)");
						Matcher mat = pat.matcher(fileString);
						if(mat.find()){
							String idStr=mat.group(1);
							oldSearchId = Long.parseLong(idStr);
						} 
					}
					catch(Exception e){
						e.printStackTrace();
					}
					if(oldSearchId != -1 && oldSearchId != searchId){
						fileString = fileString.replaceAll(String.valueOf(oldSearchId), searchIdStr);
					}
					fileString = fileString.replaceFirst("(?ims)<a href=\"#\"  onMouseOut=\"swapImgRestore()[^>]*><img src='/title-search/web-resources/images/ico_square_home.gif'[^>]+></a>", "");
					fileString = fileString.replaceFirst("(?ims)<a href=\"#\"  onMouseOut=\"swapImgRestore()[^>]*><img src='/title-search/web-resources/images/ico_square_home2.gif'[^>]+></a>", "");
					fileString = fileString.replaceFirst("(?ims)<a href=\"javascript[^>]*\"[^>]*onMouseOut=\"swapImgRestore()[^>]*><img src='/title-search/web-resources/images/ico_order_1.gif'[^>]+></a>", "");
					fileString=fileString.replaceFirst("(?ims)<a\\s*[^>]*emailSwapImage[^>]+>.*?</a>", "");
					dataToDisplay = fileString.getBytes();
				}
				
				if(view == VIEW_LOG){
					//bug 7235
					if(dataToDisplay!=null)
					dataToDisplay = SearchLogger.updateLogHeader(searchId, new String(dataToDisplay)).getBytes();
				}
				
				response.setContentType( "text/html" );
				
				if( dataToDisplay == null ){
					dataToDisplay = "No log available!".getBytes();
				}
				
				IOUtil.copy( new ByteArrayInputStream( dataToDisplay ) , response.getOutputStream());
				fileFound = true;
			}
			catch( Exception e ){
				e.printStackTrace();
			}
		}
		
		if(!fileFound){
			try{
			//file not found
			response.setContentType( "text/html" );
			String string = "<html><head>" + favIco + "</head><body>" +  
					"File Not Found!</body></html>";
			byte [] dataToDisplay = string.getBytes();
			IOUtil.copy( new ByteArrayInputStream( dataToDisplay ) , response.getOutputStream());
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
}
