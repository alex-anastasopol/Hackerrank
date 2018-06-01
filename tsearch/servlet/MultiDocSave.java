/*
 * Created on May 6, 2005
 */
package ro.cst.tsearch.servlet;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.titledocument.FakeDocumentsCreator;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.Image;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.Mortgage;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.Transfer;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.SavedFromType;
import com.stewart.ats.tsrindex.server.TsdIndexPageServer;

@Deprecated
public class MultiDocSave extends BaseServlet {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Pattern bookPagePattern = Pattern.compile("p?(\\d+)\\-(\\d+[a-z]?)");
	
	  
    public void doRequest(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
        
        HttpSession session = request.getSession();
        String sName, sFile, images[], serverId;
		ImageLinkInPage iLip = null;
        // UploadDocType udt;

		User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		long searchId=0;
		try {
			searchId = Long.parseLong(request.getParameter(RequestParams.SEARCH_ID));
		} catch (Exception e) {
			searchId = 0;
		}
		try {
			serverId = request.getParameter("serverId");
			if(serverId==null){
				serverId = request.getParameter("ServerID");//ILKaneRO
			}
		} catch (Exception e) {
		    serverId = "68";
		}
		
		if (currentUser == null) {
			//nu e logat....			
			try {
				getServletContext().getRequestDispatcher("login.jsp").forward(request, response);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		String referer = request.getHeader("Referer");
		Search global= currentUser.getSearch( request);
		int initialNumberOfDocs = global.getNumberOfDocs();
//		get all chapters
		if(serverId.equals("19") || serverId.equals("70")) {
		
			String bookType = request.getParameter("BookType");
		    String realPage = request.getParameter("realPage");
		    
		    //		  for Index Book Page Search KSJohnsonRO
		    String indexBookType = request.getParameter("IndexBookType");
		    String indexDateRange = request.getParameter("IndexDateRange");
		    String indexAlphaRange = request.getParameter("IndexAlphaRange");
		    String indexPageRange = request.getParameter("IndexPageRange");
		    
		    String desc = "MISC";
		    String book, page, page2 = "";
		    String searchType = "MISCELLANEOUS";
		    
		    if (bookType != null) {
		    	if (serverId.equals("19")) {
		    		book = request.getParameter("Book");
		    		page2 = request.getParameter("Page");
		    		page = realPage.replaceAll("\\.tif", "");
		    		sName = bookType + "_" + book + "_" + realPage.replaceAll("\\.tif", "");
		    		desc = bookType;
		    		
		    		String refererLink = referer.substring(referer.indexOf("Link=") + "Link=".length());
			    	refererLink = refererLink.substring(refererLink.indexOf("&") + 1);
			    	Map<String,String> linkParams = StringUtils.extractParametersFromQuery(refererLink);
		    		
			    	referer = "/URLConnectionReader?p1=019&p2=1&searchId=" + searchId + "&ActionType=2&Link=/recording/SPVBookSearch.asp&BookType=" + 
			    		bookType.replaceAll("\\s", "%20") + "&Book=" + 
			    		book.replaceAll("\\s", "%20") + "&Page=" + 
			    		page2.replaceAll("\\s", "%20");
		    		if(linkParams.containsKey("IndexBookType")) {
		    			referer += "&IndexBookType=" + linkParams.get("IndexBookType").replaceAll("\\s", "%20");
		    		} else {
		    			if(indexBookType != null) {
		    				referer += "&IndexBookType=" + indexBookType.replaceAll("\\s", "%20");
		    			}
		    		}
		    		if(linkParams.containsKey("IndexDateRange")) {
		    			referer += "&IndexDateRange=" + linkParams.get("IndexDateRange").replaceAll("\\s", "%20");
		    		}
		    		if(linkParams.containsKey("IndexAlphaRange")) {
		    			referer += "&IndexAlphaRange=" + linkParams.get("IndexAlphaRange").replaceAll("\\s", "%20");
		    		}
		    		if(linkParams.containsKey("IndexPageRange")) {
		    			referer += "&IndexPageRange=" + linkParams.get("IndexPageRange").replaceAll("\\s", "%20");
		    		}
		    		
		    	} else {
		    		book = request.getParameter("Book");
		    		page = request.getParameter("Page");  
		    		sName = bookType.replaceAll("\\s", "+") + "_" + book.replaceAll("\\s", "+") + "_" + realPage.replaceAll("\\.tif", "");

		    	}
		    } else {
			    book = indexAlphaRange;
			    page = indexPageRange;
		    	Matcher type = Pattern.compile("General Index (\\w+)").matcher(indexBookType);
		    	if (type.find()){
		    		searchType = type.group(1).toUpperCase();
		    	}
		    	referer += "&IndexPageRange=" + page;
		    	desc = searchType;
		    	sName = indexBookType.replaceAll("\\s", "+") + "_" + indexDateRange.replaceAll("\\s", "+") + "_" + indexAlphaRange + "_" + realPage;
		    	
		    	String refererLink = referer.substring(referer.indexOf("Link=") + "Link=".length());
		    	refererLink = refererLink.substring(refererLink.indexOf("&") + 1);
		    	Map<String,String> linkParams = StringUtils.extractParametersFromQuery(refererLink);
		    	
		    	if(serverId.equals("19")) {
		    		referer = "/URLConnectionReader?p1=019&p2=1&searchId=" + searchId + "&ActionType=2&Link=/recording/SPVIndexSearch.asp&IndexBookType=" + 
		    			indexBookType.replaceAll("\\s", "%20") + "&IndexDateRange=" + 
		    			indexDateRange.replaceAll("\\s", "%20") + "&IndexAlphaRange=" + 
		    			indexAlphaRange.replaceAll("\\s", "%20") + "&IndexPageRange=" + 
		    			indexPageRange.replaceAll("\\s", "%20");
		    		if(linkParams.containsKey("BookType")) {
		    			referer += "&BookType=" + linkParams.get("BookType").replaceAll("\\s", "%20");
		    		}
		    		if(linkParams.containsKey("Book")) {
		    			referer += "&Book=" + linkParams.get("Book").replaceAll("\\s", "%20");
		    		}
		    	} else {
		    		referer = null;
		    	}
		    }
		    
		    if (serverId.equals("70")) { 
		    	if (bookType != null)
			    	sFile = "/title-search/URLConnectionReader?p1=070&p2=1&searchId=" + searchId 
					 + "&ActionType=2&Link=/LoadImage.asp&BookType=" + bookType
					 + "&Book=" + book + "&Page=" + page;
		    	else
		    		sFile = "/title-search/URLConnectionReader?p1=070&p2=1&searchId=" + searchId 
					 + "&ActionType=2&Link=/LoadImage.asp&IndexBookType=" + indexBookType
					 + "&DateRange=" + indexDateRange + "&AlphaRange=" + indexAlphaRange + "&IndexPage=" + page;
		    } else {
		    	if (bookType != null) {
				    sFile = "/title-search/URLConnectionReader?p1=019&p2=1&searchId=" + searchId 
			    		+ "&ActionType=2&Link=/recording/LoadImage.asp&BookType=" + bookType
			    		+ "&Book=" + book + "&Page=" + page2;
		    	} else {
		    		sFile = "/title-search/URLConnectionReader?p1=019&p2=1&searchId=" + searchId 
			    		+ "&ActionType=2&Link=/recording/LoadImage.asp&IndexBookType=" + indexBookType
			    		+ "&IndexDateRange=" + indexDateRange + "&IndexAlphaRange=" + indexAlphaRange + "&IndexPageRange=" + request.getParameter("IndexPageRange");
		    	}
		    }
		    
			if (serverId.equals("70"))
				
				//
				
					iLip = new ImageLinkInPage("searchId=" + searchId + "&ActionType=2&Link=" + sFile.substring(sFile.indexOf("/LoadImage")).replaceAll("\\?", "&"), sName + ".tif");
			else
					iLip = new ImageLinkInPage("searchId=" + searchId + "&ActionType=2&Link=" + sFile.substring(sFile.indexOf("/recording")).replaceAll("\\?", "&"), sName + ".tif");		    
		    //
		    Instrument instr = new Instrument();
			instr.setBook(book);
			instr.setPage(page);
		    instr.setDocType(searchType.equals("DEED")? "TRANSFER":searchType);
		    instr.setDocSubType(searchType.equals("DEED")? "TRANSFER":searchType);
			instr.setInstno(book + "_" + page);
			instr.setYear((new Date()).getYear()+1900);
			
		    RegisterDocument docR = new RegisterDocument( DocumentsManager.generateDocumentUniqueId(searchId, instr) );
		    docR.setInstrument(instr);
		    docR.setServerDocType(desc);
		    docR.setType(DType.ROLIKE);
		    docR.setFake(true);
		    docR.setRecordedDate(new Date());
		    docR.setInstrumentDate(new Date());
		    docR.setDataSource("RO");
		    docR.updateDescription();
		    if (searchType.equals("DEED")){
			    docR = new Transfer(docR);		    	
		    } else if (searchType.equals("MORTGAGE")){
		    	docR = new Mortgage(docR);
		    }
		    TSServer.calculateAndSetFreeForm(docR, PType.GRANTEE, searchId);
		    TSServer.calculateAndSetFreeForm(docR, PType.GRANTOR, searchId);
		    docR.setSavedFrom(SavedFromType.PARENT_SITE);
		    try {
		    	docR.setSiteId(((TSServer)TSServersFactory.GetServerInstance(global.getP1ParentSite(), global.getP2ParentSite(), searchId)).getServerID());
		    } catch (Exception e) {
		    	e.printStackTrace();
			}

			
			images = new String[1];
			images[0] = new String(sFile);

    		SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd");
			String sFileOld = sFile;
			sFileOld = URLDecoder.decode(sFileOld);
			ImageI image = new Image();
			Set<String> list = new HashSet<String>();
			list.add(sFileOld);
        	image.setLinks( list );
    		String extension = "";
    		String oldImageFileName = iLip.getImageFileName();
    		int poz = oldImageFileName .lastIndexOf(".");
    		if(poz>0 && (poz+1 < oldImageFileName.length()) ){
    			extension = oldImageFileName.substring(poz+1);
    		}
    		if(extension.equalsIgnoreCase("tif")){
    			extension = "tiff";
    		}		            	
        	image.setExtension( extension );
        	if("tiff".equalsIgnoreCase(extension)){
        		image.setContentType("image/tiff");
        	}
        	else if("pdf".equalsIgnoreCase(extension)){
        		image.setContentType("application/pdf");
        	}
			Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();	
    		Date sdate = search.getStartDate();
    		String basePath = ServerConfig.getImageDirectory()+File.separator+format.format(sdate)+File.separator+searchId;
    		File file= new File(basePath);
    		if(!file.exists()){
    			file.mkdirs();
    		}
    		
    		boolean alreadySaved = false;;
    		try {
         		File fakeFile = new File(search.getSearchDir() + File.separator + "Register" + File.separator + sName + searchType + ".html");
         		if(fakeFile.exists()){
         			alreadySaved = true;
         		}
    		 }catch (Exception e) {
    				e.printStackTrace();
    			} 
    		 
        	String fileName = docR.getId()+"."+extension;
        	String path 	= basePath+File.separator+fileName;
			String html = FakeDocumentsCreator.createTNWilsonBackScannedRegisterDoc(
                    sName, // Instrument Number
                    String.valueOf((new Date()).getYear()+1900), // Instrument Year
                    "", // File date
                    searchType, // Instrument Type
                    book, // Book
                    page, // Page
                    "", // Grantor
                    "", // Grantee
                    FakeDocumentsCreator.ViewImage, // View (?)
                    search.getSearchDir() + File.separator + "Register" + File.separator + sName + searchType + ".html", // File path
                    sFile, // Tiff href
                    "html" // File extention
            );
		
			Parser parser = new Parser(BaseServlet.REAL_PATH,
					(int)TSServersFactory.getSiteId("TN", "Shelby", "RO"),
					global,
					global.getID());
			ParsedResponse pr = new ParsedResponse();
			pr.setFileName(sName); 

			pr.addImageLink( new ImageLinkInPage(sFileOld, sName));
			
			pr.setParentSite(true);
			try {
			    parser.Parse(pr,html, Parser.PAGE_DETAILS,"",0, "", "");
			} catch( ServerResponseException e) {
			    e.printStackTrace();
			}
            pr.setDocument(docR);
            docR.setChecked(true);
            docR.setIncludeImage(true);
            
            logDisplay(alreadySaved, pr, instr.getInstno(), docR, searchId);
            
			DocumentsManagerI manager = search.getDocManager();
			try {
				manager.getAccess();
				manager.add(docR);
				docR.setIndexId( DBManager.addDocumentIndex(Tidy.tidyParse(html, null), search ) );
            	image.setFileName(fileName);
            	image.setPath(path);
            	image.setSaved(false);
            	image.setUploaded(false);
            	docR.setImage(image);
            	docR.setIncludeImage(true);
            	docR.setSavedFrom(SavedFromType.PARENT_SITE);
			} catch(Exception e){  
	        	e.printStackTrace(); 
	        } finally{
	        	manager.releaseAccess();
	        }
		}

		else if (serverId.equals("68") || "65".equals(serverId) || serverId.equals("p1=06282&p2=1")) {//'p1=06282&p2=1' is ILKaneRO
			
			Enumeration<String> parameterNames = request.getParameterNames();
			
			String searchType = null;
			try {
				searchType = request.getParameter("searchType");
			} catch ( Exception e ) {e.printStackTrace();}
			if ( searchType == null ) {
				searchType = "MISC";
			}
				
			//there are checked chapters
			String extension = request.getParameter("image_type");
			
			while (parameterNames.hasMoreElements()) {
				//get file path and name
				sName = (String) parameterNames.nextElement();
				Matcher ma = bookPagePattern.matcher(sName);

				String book = "";
				String page = "";
				String instrument = sName;
				
				if (ma.matches()){
					book = ma.group(1);
					page = ma.group(2);
				} else {
					if ("65".equals(serverId)) {
						ma = Pattern.compile("(?is)(\\w+)-/?([\\w\\.]+)").matcher(sName);
						if (ma.find()){
							book = ma.group(1);
							page = ma.group(2).replaceAll("(?is)\\p{Punct}", "");
						} else {
							if(!sName.matches("\\d+\\w*\\d+")) {
								continue;
							}
						}
					} else if (serverId.equals("p1=06282&p2=1")) {
						if (!sName.equals("bookPageILKaneRO")) {
							continue;
						}
					} else {
						if (!sName.matches("\\d+\\w*\\d+") && !serverId.equals("p1=06282&p2=1")) {
							continue;
						}
					}
				}
				String[] values = request.getParameterValues( sName );
				
				for (int i = 0; i < values.length; i++) {
					sFile = values[i];
					String imageLinkILKaneRO = "";
					if(serverId.equals("p1=06282&p2=1")){//ILKaneRO
						String bookPageParam = values[i].replaceAll("(?i)(?:PLAT\\s*)?(?:BOOK|PAGE|DOCS)", "");
						
						ma = Pattern.compile("(?is)(?:^\\s*\\d+-)?(\\w+)\\b\\s*-?/?\\b([\\w\\.]+)").matcher(bookPageParam);
						if (ma.find()) {
							book = ma.group(1);
							page = ma.group(2).replaceAll("(?is)\\p{Punct}", "");
							instrument = book + "-" + page;
							sName = instrument;
							imageLinkILKaneRO = org.apache.commons.lang.StringUtils.defaultString(request.getParameterValues( "imageLink" )[i]);
							imageLinkILKaneRO = imageLinkILKaneRO.substring(imageLinkILKaneRO.indexOf("Link=")+5);
						} 
					} else if (values.length > 1) {
						instrument += "_" + i;
					}
					if(extension == null) {
		        		int poz = sFile .lastIndexOf(".");
		        		if(poz>0 && (poz+1 < sFile.length()) ){
		        			extension = sFile.substring(poz+1);
		        		}
					}

				    Instrument instr = new Instrument();
					instr.setBook(book);
					instr.setPage(page);
				    instr.setDocType(searchType.equals("DEED")? "TRANSFER":searchType);
				    instr.setDocSubType(searchType.equals("DEED")? "TRANSFER":searchType);
					instr.setInstno(instrument);
					instr.setYear((new Date()).getYear()+1900);
			    	
					String dataSource = "RO";
					try {
						dataSource = HashCountyToIndex.getServerAbbreviationByType(Integer.parseInt(serverId));
					} catch (Exception e) {
					}
					RegisterDocument docR = new RegisterDocument( DocumentsManager.generateDocumentUniqueId(searchId, instr) );
				    docR.setInstrument(instr);
				    docR.setServerDocType(searchType);
				    docR.setType(DType.ROLIKE);
				    
				    String srcType = null;
					try {
						srcType = request.getParameter("searchTypeModule");
					} catch ( Exception e ) {e.printStackTrace();}
					if (org.apache.commons.lang.StringUtils.isNotEmpty(srcType) && SearchType.IM.toString().equalsIgnoreCase(srcType)){
						docR.setSearchType(SearchType.IM);
					}
				    
				    docR.setRecordedDate(new Date());
				    docR.setInstrumentDate(new Date());
				    docR.setDataSource(dataSource);
				    if (searchType.equals("DEED")){
				    	docR = new Transfer(docR);
				    }
				    docR.updateDescription();
				    TSServer.calculateAndSetFreeForm(docR, PType.GRANTEE, searchId);
				    TSServer.calculateAndSetFreeForm(docR, PType.GRANTOR, searchId);
				    docR.setSavedFrom(SavedFromType.PARENT_SITE);
				    try {
				    	docR.setSiteId(((TSServer)TSServersFactory.GetServerInstance(global.getP1ParentSite(), global.getP2ParentSite(), searchId)).getServerID());
				    } catch (Exception e) {
				    	e.printStackTrace();
					}

					
					images = new String[1];
					if("pdf".equalsIgnoreCase(extension)) {
						sFile = sFile.replaceAll("type=$", "type=pdf");
					}
					images[0] = new String(sFile);
					
					SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd");
					String sFileOld = sFile;
					sFileOld = URLDecoder.decode(sFileOld);
					ImageI image = new Image();
					Set<String> list = new HashSet<String>();
					if (serverId.equals("p1=06282&p2=1")) {// ILKaneRO
						list.add(imageLinkILKaneRO);
					} else {
						list.add(sFileOld);
					}
	            	image.setLinks( list );

	            	if(extension.equalsIgnoreCase("tif")){
            			extension = "tiff";
            		}		            	
	            	image.setExtension( extension );
	            	if("tiff".equalsIgnoreCase(extension)){
	            		image.setContentType("image/tiff");
	            	}
	            	else if("pdf".equalsIgnoreCase(extension)){
	            		image.setContentType("application/pdf");
	            	}
					Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();	
            		Date sdate = search.getStartDate();
            		String basePath = ServerConfig.getImageDirectory()+File.separator+format.format(sdate)+File.separator+searchId;
            		File file= new File(basePath);
            		if(!file.exists()){
            			file.mkdirs();
            		}
            		
            		boolean alreadySaved = false;;
            		try {
                 		File fakeFile = new File(search.getSearchDir() + File.separator + "Register" + File.separator + sName.replaceAll("\\p{Punct}", "") + searchType + ".html");
                 		if(fakeFile.exists()){
                 			alreadySaved = true;
                 		}
            		 }catch (Exception e) {
            				e.printStackTrace();
            			} 
            		
                	String fileName = docR.getId()+"."+extension;
                	String path 	= basePath+File.separator+fileName;
					String html = FakeDocumentsCreator.createTNWilsonBackScannedRegisterDoc(
		                    instrument, // Instrument Number
		                    String.valueOf((new Date()).getYear()+1900), // Instrument Year
		                    "", // File date
		                    searchType, // Instrument Type
		                    book, // Book
		                    page, // Page
		                    "", // Grantor
		                    "", // Grantee
		                    FakeDocumentsCreator.ViewImage, // View (?)
		                    search.getSearchDir() + File.separator + "Register" + File.separator + instrument.replaceAll("\\p{Punct}", "") + searchType + ".html", // File path
		                    sFile, // Tiff href
		                    "html" // File extention
		            );
				
					Parser parser = new Parser(BaseServlet.REAL_PATH,
							(int)TSServersFactory.getSiteId("TN", "Shelby", "RO"),
							global,
							global.getID());
					ParsedResponse pr = new ParsedResponse();
					pr.setFileName(sName); 

					pr.addImageLink( new ImageLinkInPage(sFileOld, instrument));
					
					pr.setParentSite(true);
					try {
					    parser.Parse(pr,html, Parser.PAGE_DETAILS,"",0, "", "");
					} catch( ServerResponseException e) {
					    e.printStackTrace();
					}
	                pr.setDocument(docR);
	                docR.setChecked(true);
	                docR.setIncludeImage(true);
	                
	                logDisplay(alreadySaved, pr, instr.getInstno(), docR, searchId);
	                
					DocumentsManagerI manager = search.getDocManager();
					try {
						manager.getAccess();
						manager.add(docR);
						docR.setIndexId( DBManager.addDocumentIndex(Tidy.tidyParse(html, null), search ) );
		            	image.setFileName(fileName);
		            	image.setPath(path);
		            	image.setSaved(false);
		            	image.setUploaded(false);
		            	docR.setImage(image);
		            	docR.setIncludeImage(true);
					} catch(Exception e){  
			        	e.printStackTrace(); 
			        } finally{
			        	manager.releaseAccess();
			        }
				}
			}
		}
		if(serverId.equals("19") && referer != null) {
			request.setAttribute("DocumentSaved", true);
			forward(request, response, referer);
		} else if (serverId.equals("p1=06282&p2=1") && referer != null) {//ILKaneRO
			referer = referer.substring(referer.indexOf("/URLConnectionReader"));
			referer += (referer.endsWith("/URLConnectionReader") ? "?" : "&");
			referer += "initialNumberOfDocsMultiDocSave=" + initialNumberOfDocs;

			forward(request, response, referer);
		} else {
			forward(request, response, URLMaping.TSD);
		}
    }
    
    private void logDisplay(boolean alreadySaved, ParsedResponse pr, String instrNo, RegisterDocument docR, long searchId){
    	
    	String type = "details";
        String id = String.valueOf(System.nanoTime()) + "_details"; 
        String searchType = "";
        if (org.apache.commons.lang.StringUtils.isNotEmpty(docR.getSearchType().toString())){
        	searchType = "<br>" + docR.getSearchType().toString();
        }
        SearchLogger.info("<div id='" + id + "'>", searchId);
        SearchLogger.info("<br/>Retrieved <span class='rtype'>" + type + "</span> document:<br/>",searchId);
        SearchLogger.info(
            	"<table border='1' cellspacing='0' width='99%'>" +
            	"<tr><th width='2%'>DS</th><th width='24%' align='left'>Desc</th><th width='7%'>Date</th><th width='17%'>Grantor</th><th width='17%'>Grantee</th><th width='10%'>Instr Type</th><th width='7%'>Instr</th><th width='16%'>Remarks</th></tr>" +
            	"<tr><td width='2%'>" + docR.getDataSource() + searchType + "</td><td width='24%' align='left'>&nbsp;</td><td width='7%'>&nbsp;</td><td width='17%'>&nbsp;</td><td width='17%'>&nbsp;</td><td width='10%'>&nbsp;</td><td width='7%'>" + instrNo + "</td><td width='16%'>&nbsp;</td></tr>" +
            	"</table>", searchId);

   		if(alreadySaved){
   			SearchLogger.info("<span class='overwrite'>Document overwritten.</span>&nbsp;&nbsp;" + SearchLogger.getTimeStamp(searchId) + "</div>", searchId);
   		}else{
   			SearchLogger.info("<span class='saved'>Document saved.</span><br/></div>", searchId);
   			if (pr.isParentSite()){
   				TsdIndexPageServer.logAction("Parent site add ", searchId, docR);
   			}
         }
    }
}
