package ro.cst.tsearch.threads;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.a2pdf.AdjustHTMLFile;
import ro.cst.tsearch.LoadConfig;
import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.NdexPackage;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.connection.FTPWriterNdex;
import ro.cst.tsearch.connection.ftp.FtpClient;
import ro.cst.tsearch.connection.ftp.FtpClient.FTPFileStruct;
import ro.cst.tsearch.connection.gateway.EcorGatewayClient;
import ro.cst.tsearch.connection.titledesk.TitleDesk;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.CommunityTemplatesMapper;
import ro.cst.tsearch.database.rowmapper.CommunityUserTemplatesMapper;
import ro.cst.tsearch.database.rowmapper.RequestCount;
import ro.cst.tsearch.database.transactions.SaveSearchTransaction;
import ro.cst.tsearch.monitor.ConvertToPdfTime;
import ro.cst.tsearch.pdftiff.util.Util;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servlet.FileServlet;
import ro.cst.tsearch.templates.TemplateBuilder;
import ro.cst.tsearch.templates.TemplateUtils;
import ro.cst.tsearch.titledocument.abstracts.PrivacyStatement;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.FileCopy;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.MemoryAllocation;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.Tidy;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.webservices.PlaceOrderService;

import com.gwt.utils.client.UtilsAtsGwt;
import com.netdirector.titleresponse.ResponseCreator;
import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.CourtI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.ImageI.IType;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.TaxDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.base.search.SsfDocumentMapper;
import com.stewart.ats.base.warning.WarningUtils;
import com.stewart.ats.connection.stewarorders.StewartOrders;
import com.stewart.ats.connection.sureclose.SureCloseConn;
import com.stewart.ats.connection.sureclose.SureCloseConn.CallResult;
import com.stewart.ats.connection.sureclose.SureCloseConn.CallStatus;
import com.stewart.ats.tsrcreation.DocumentWithContent;
import com.stewart.ats.tsrcreation.TsrCreator;
import com.stewart.ats.tsrindex.client.ReviewChecker;
import com.stewart.ats.tsrindex.client.SimpleChapter;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.server.UploadTemplate;
import com.stewart.ats.tsrindex.server.UtilForGwtServer;
import com.stewartworkplace.starters.ssf.services.connection.DocAdminConn;
import com.stewartworkplace.starters.ssf.services.docadmin.DocumentInfoResultType;
import com.stewartworkplace.starters.ssf.services.docadmin.DocumentInfoType;
import com.stewartworkplace.starters.ssf.services.docadmin.ErrorContainerType;
import com.stewartworkplace.starters.ssf.services.docadmin.SuccesType;

public class GPThread extends Thread {
	
	private static final int  WAIT_GENERATE_TEMPLATE_MAX=350000;
	private static final Pattern patternCod=Pattern.compile(TemplateBuilder.NOT_EXIST_COD);
    protected static final Category logger = Logger.getLogger(GPThread.class);

    private static final String lbURL = LoadConfig.getLoadBalancingUrl();
	private static final int lbPort = LoadConfig.getLoadBalancingPort();
	private static final String appBcc = MailConfig.getMailBcc();
	private static final String appFrom = MailConfig.getMailFrom();
	private static final String mailHost = MailConfig.getMailSmtpHost();
	    
    private Search global = null;
    private User user = null;
    
    private CommunityAttributes ca = null;
    private UserAttributes currentAgent = null;
    boolean hasPolicyFiles = false;
    private long searchId=-1;
    protected GPThread(Search search, User user, CommunityAttributes ca) {
        this.global = search;
        this.user = user;
        this.ca = ca;
        setName("GPThread - " + search.getSearchID());
        searchId = search.getID();
        createTime = System.currentTimeMillis();
    }

    public Object notifier = new Object();
    public boolean conversionFinished = false;
    private long startTime = 0, finishTime = 0;
    protected long createTime = 0;

    public void run() {

    	MemoryAllocation.getInstance().addTsrsStarted();
        startTime = System.currentTimeMillis();
        
        InstanceManager.getManager().getCurrentInstance(searchId).setCurrentUser(user.getUserAttributes());
        InstanceManager.getManager().getCurrentInstance(searchId).setCurrentCommunity(ca);

        if(ServerConfig.getBoolean("create.tsr.force.parent.site.flag", true)) {
        	global.setSearchType(Search.PARENT_SITE_SEARCH);
        }
        
        String userLogin = "";
        UserAttributes uaLogin = user.getUserAttributes();
        if (uaLogin != null)
        	userLogin = uaLogin.getNiceName();
        
        try {

            setPriority(Thread.MIN_PRIORITY);
            Search.saveSearch(global);
            AsynchSearchSaverThread.getInstance().saveSearchContext( global );
            
            global.deleteAllSsfDocument();	//delete previous documents if any
            
            int result = createTSR();
            
            if(result != GP_SUCCESS) {
            	SearchLogger.infoUpdateToDB("\n</div><div><BR><B>TSR creation FAILED</B> on: " + SearchLogger.getTimeStampFormat1(userLogin, "&#44; ") + " - Retrying...<BR></div>\n", global.getID());
        		logger.error("TSR creation FAILED first time for SearchId " + searchId + " retrying...");
            	try {
            		TimeUnit.SECONDS.sleep(ServerConfig.getTsrFailRetrySeconds());
            	} catch (Exception e) {
            		
            	}
            	result = createTSR();
            }
            
            if (result == GP_SUCCESS) {
            	
                SearchLogger.info("\n</div><div><BR><B>TSR created</B> on: " + SearchLogger.getTimeStampFormat1(userLogin, "&#44; ") + "<BR></div>\n", global.getID());
            
	            //save search information to database
                global.getSearchFlags().setForReview(false);
                DBManager.setDbSearchForReview(global.getID(), 0);
                
                SearchManager.removeSearch(searchId, false);
                
	            int savedToDB = DBManager.saveCurrentSearch(user, global, Search.SEARCH_TSR_CREATED, 
	                   notificationFile, contextPath, notificationSendTo);
	            if(savedToDB != SaveSearchTransaction.STATUS_SUCCES) {
	            	if(savedToDB == SaveSearchTransaction.STATUS_FAIL_TIMEOUT) {
	            		
	            		SaveTsrRetryer retryer = new SaveTsrRetryer(
	            				user, 
	            				global,
	            				notificationFile,
	            				contextPath,
	            				notificationSendTo);
	            		
	            		retryer.setSleepInterval(120);
	            		retryer.setMaxInterval(1800);
	            		
	            		
	            		Thread threadRetryer = new Thread(retryer);
	            		threadRetryer.setName("SaveTsrRetryer for searchId: " + searchId + ", ca: " + (ca!=null?ca.getNAME():"NoCommunity"));
	            		threadRetryer.start();
	            		
	            		//let's sleep one more time, this is not seen by the user :)
	            		try {
	            			threadRetryer.join();
	            		} catch (Exception e) {
	            			e.printStackTrace();
	            		}
	            		savedToDB = retryer.getSaveStatus();
	            		
	            	}
	            	if(savedToDB != SaveSearchTransaction.STATUS_SUCCES) {
	            		SearchLogger.info("\n</div><div><BR><B>Search was not correctly autosaved after TSR creation</B> on: " + SearchLogger.getTimeStampFormat1(userLogin, "&#44; ") + "<BR></div>\n", global.getID());
	            		//this shouldn't be happening 
	        	    	Log.sendEmail2("Search was not correctly autosaved after TSR creation", 
	        	    			"Search was not correctly autosaved after TSR creation with status " + savedToDB + " " + 
	        	    			SearchLogger.getTimeStampFormat1(userLogin, "&#44; ") +
	        	    			"<br>SearchId: " + searchId + ", ca: " + (ca!=null?ca.getNAME():"NoCommunity"));
	            	}
	            }
	            
	            AsynchSearchSaverThread.getInstance().saveSearchContext( global );
	            DBManager.setSearchOwner(global.getID(), 0);
	            SearchLogger.moveLogToDatabase(global.getID());
	            // send message to abstractor if the TSR had warnings
	            if(!"".equals(warningMessages)){
	                String abstractorEmail = user.getUserAttributes().getEMAIL();
	                String abstractorAlternateMail = user.getUserAttributes().getALTEMAIL();
	                
	                if (abstractorAlternateMail.trim().equals("") || abstractorAlternateMail.trim().equals("N/A"))
	                    abstractorAlternateMail = null;
	                
	                if ((user != null) && (user.getUserAttributes().getMyAtsAttributes().getReceive_notification() == 1)) {
		                sendMailMessage(
		                		appFrom,
		                		abstractorEmail,
		                		abstractorAlternateMail,
		                		appBcc, 
		                        global.getSa().getAbstractorFileName() + ": TSR created with warning",		                    
		                        "Your TSR " + global.getSa().getAbstractorFileName() + " was created with the following warnings: \n" + warningMessages, 
			                    null, false, null, false);
		               }
	            	
	            }
	            
            } else {
            	
            	global.deleteAllSsfDocument();
            	
            	SearchLogger.infoUpdateToDB("\n</div><div><BR><B>TSR creation FAILED</B> on: " + SearchLogger.getTimeStampFormat1(userLogin, "&#44; ") + "<BR></div>\n", global.getID());
                String abstractorEmail = user.getUserAttributes().getEMAIL();
                String abstractorAlternateMail = user.getUserAttributes().getALTEMAIL();
                
                if (abstractorAlternateMail.trim().equals("") || abstractorAlternateMail.trim().equals("N/A"))
                    abstractorAlternateMail = null;
                
                if ((user != null) && (user.getUserAttributes().getMyAtsAttributes().getReceive_notification() == 1)) {
	                sendMailMessage(appFrom, abstractorEmail, abstractorAlternateMail, appBcc, 
	                        global.getSa().getAbstractorFileName() + ": Unable to create a valid TSR. Please try again later.",
		                    (conversionError != null ? conversionError : "") +
		                    "Your search " + global.getSa().getAbstractorFileName() + " was saved and you can open it from dashboard section.", 
		                    null, false, null, false);
                }
                
                DBManager.setSearchOwner(global.getID(), user.getUserAttributes().getID().intValue());
                //SaveSearchTransaction.saveImageCount(DBManager.getSimpleTemplate(), searchId);
                Search.saveSearch(global);
                SearchLogger.moveLogToDatabase(global.getID());
            }

        } catch (Exception e) {
        	
        	global.deleteAllSsfDocument();
        	
        	logger.error("Problem while saving search " + searchId + " in GPThread", e);
            
            SearchLogger.infoUpdateToDB("\n</div><div><BR><B>TSR creation FAILED</B> on: " + SearchLogger.getTimeStampFormat1(userLogin, "&#44; ") + "<BR></div>\n", global.getID());
            String abstractorEmail = user.getUserAttributes().getEMAIL();
            String abstractorAlternateMail = user.getUserAttributes().getALTEMAIL();
            
            if (abstractorAlternateMail.trim().equals("") || abstractorAlternateMail.trim().equals("N/A"))
                abstractorAlternateMail = null;
            
            if ((user != null) && (user.getUserAttributes().getMyAtsAttributes().getReceive_notification() == 1)) {
                try {
					sendMailMessage(appFrom, abstractorEmail, abstractorAlternateMail, appBcc, 
					        global.getSa().getAbstractorFileName() + ": Unable to create a valid TSR. Please try again later.",
					        (conversionError != null ? conversionError : "") +
					        "Your search " + global.getSa().getAbstractorFileName() + " was saved and you can open it from dashboard section.", 
					        null, false, null, false);
				} catch (Exception e1) {
					logger.error("Error sending email for searchId " + searchId , e1);
					e1.printStackTrace();
				}
            }
            
            DBManager.setSearchOwner(global.getID(), user.getUserAttributes().getID().intValue());
            //SaveSearchTransaction.saveImageCount(DBManager.getSimpleTemplate(), searchId);
            try {
				Search.saveSearch(global);
			} catch (Exception e1) {
				logger.error("Error saving search for searchId " + searchId , e1);
				e1.printStackTrace();
			}
            SearchLogger.moveLogToDatabase(global.getID());
            
        }
        
        GPMaster.removeThread(this);

        synchronized (notifier) {
            conversionFinished = true;
            notifier.notify();
        }
        
        //InstanceManager.getCurrentInstance().setCrtSearchContext(null);
        //InstanceManager.setCurrentInstance(null);

        finishTime = System.currentTimeMillis();
        
    	MemoryAllocation.getInstance().addTsrsEnded();
    }

    public Search getOriginalSearch() {
        return global;
    }

    public Search getoriginalSearch() {
    	return global;
    }
    
    public long getStartTime() {
        return startTime;
    }

    public long getFinishTime() {
        return finishTime;
    }

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    private static SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy_HHmmss");
    static {        
        sdf.setLenient(false);
    }
    
    public String TSRFolder;
    public String TSRFileName;
    public String COMFileName;
    public String TSDFileName;
    public String contextPath;
    
    public String notificationSendTo = null;
    public String notificationFile = null;
    
    public ro.cst.tsearch.servers.parentsite.State currentState = null;
    public County currentCounty = null;
    
    //public String forwardTo = null;
    
    public static final int GP_SUCCESS						=	0;
    
    public static final int GP_CLONE_ERROR					=	1;
    public static final int GP_DOWNLOAD_IMAGE_ERROR			=	2;
    public static final int GP_FILE_NOT_FOUND_ERROR			=	3;
    public static final int GP_CREATE_TSR_ERROR				=	4;
    
    public String conversionError = null;
    private String warningMessages = "";
    private boolean removeInvoicePage = false;
    
	private HashMap<String,String> fillTemplates() throws Exception{
        // fill template parameters
        final HashMap<String,Object> templateParams = TemplateBuilder.fillTemplateParameters(global, ca, currentCounty, currentState, false, false, null);
        final String fakeTSRFolder = TSRFolder;
        class CPDThread extends Thread{
        	
        	private CurrentInstance ci;
        	
        	public CPDThread(CurrentInstance ci) {
        		this.ci = ci;
        	}
        	private HashMap<String,String> attFiles = new HashMap<String,String>();
        	public boolean finishTemplates = false;
        	public Object notifier = new Object();
        	public void run() {
        		InstanceManager.getManager().setCurrentInstance(searchId,ci);
        		
        		//on preview fill all codes for docs
				TemplateUtils.fillDocumentBPCodes(searchId, user.getUserAttributes().getID().longValue());
        		
        		// create template files
	            if (getCurrentAgent() != null) {
	                try {
	                	attFiles = TemplateBuilder.addTemaplteFilesV2(global, templateParams, getCurrentAgent(), ca, fakeTSRFolder,removeInvoicePage);
	                	logger.info("Am templateurile in attFiles on SearchId " + global.getID());
	                } catch (Exception e) {
	                    e.printStackTrace();
	                    logger.error("CPDThread - error for " + searchId, e);
	                }
	            }
	            COMFileName = null;		            
	            synchronized (notifier) {
	            	finishTemplates = true;
	                notifier.notify();
	            }
        	}
        	public HashMap<String,String> getAttFiles() {
        		return attFiles;
        	}
        }
        CPDThread cpd = new CPDThread(InstanceManager.getManager().getCurrentInstance(global.getID()));
        logger.info("Starting template generation thread! on SearchId " + global.getID());
        cpd.start();
        cpd.join(WAIT_GENERATE_TEMPLATE_MAX);
        hasPolicyFiles = cpd.finishTemplates; 
        return cpd.getAttFiles();
    }
    
	private int createTSR() {

		String searchFolder = null;
		
		final UserAttributes ua = global.getAgent();
		
		synchronized (global) {
			try {
				searchFolder = global.getSearchDir();

				// copy current agent
				this.setCurrentAgent(ua);
				InstanceManager.getManager().getCurrentInstance(global.getID())
						.setCrtSearchContext(global);

				// saving context for the cloned search
				global.setUpdate(false);
				DBManager.saveCurrentSearch(user, global, Search.SEARCH_TSR_NOT_CREATED, null, global, false);
				DBManager.setSearchOwner(global.getID(), -2);

			} catch (Throwable e) {
				e.printStackTrace();

				conversionError = "Unable to clone search context, TSR generation aborted.";
				SearchLogger.info("<b>Unable to save search context</b>", searchId);
				return GP_CLONE_ERROR;
			}
		}
        
		SearchLogger.infoUpdateToDB("\n</div><div><BR><B>Start creating TSR</B> on: " + SearchLogger.getTimeStampFormat1(user.getUserAttributes().getNiceName(), "&#44; ") 
				+ (user.getBrowserVersion()!=null?" using browser: <span class=\"timestamp\">" + user.getBrowserVersion() + "</span>":"")
				+ "<BR></div>\n", global.getID());
		
		// Add remaining warnings.
		Set<String> warnings = WarningUtils.calculateWarnings(searchId);
		if (!warnings.isEmpty()) {
			StringBuilder warningsHtml = new StringBuilder("<ul>\n<font color=\"red\"><b>Remaining warnings:</b></font>\n");
			for(String warning: warnings) {
				warningsHtml.append("<li><font color=\"red\">").append(warning).append("</font></li>\n");
			}
			warningsHtml.append("</ul>");
			SearchLogger.infoUpdateToDB(warningsHtml.toString(), global.getID());
		}
		
        int stateFips = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateFips();
		int countyFips = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getCountyFips();
        
        // TSR distribution type
        ImageI.IType distributionType = ImageI.IType.TIFF;
        if (this.getCurrentAgent() != null) {
            distributionType = this.getCurrentAgent().getDistributionType();
        }
        
        String originalValueOfTheMemberFieldTSRFolder = TSRFolder;
        /**
		 * "Perverted" means that some idiot decided to replace the "_" in fileId and I had to 
		 * fix the effect fast and send notifications according to the real file id
		 */
		//HashMap pervertedGoodAttachedFiles = null;
		String pervertedFileId = null;
		
		//Set<String> notAnotherSetWithFileNames = new HashSet<String>();
		
		// list of created files in the temporary folder that will be copied to tsrFileNames
		List<String> createdFileNames = new ArrayList<String>(); 
        List<String> createdFileNamesTemplates = new ArrayList<String>(); // list of created files in the temporary folder
        String fileId = global.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO);
		if ("".equals(fileId)){
			fileId = "UnknownFileId";
		}
		String displayFileName = TSRFileName;
        
        try {
        	if(fileId.contains("_")) {
				//pervertedGoodAttachedFiles = new HashMap();
				pervertedFileId = fileId.replaceAll("_", "");
				TSRFolder = TSRFolder.replace("-" + pervertedFileId + "_", "-" + fileId + "_");
				displayFileName = displayFileName.replace("-" + pervertedFileId + "_", "-" + fileId + "_");
			}
        	
        	
	        // image download was here
	        String tsrFolder = ""; // tsr folder
	        String tmpFolder = ""; // temporary folder        
	        String file = ""; // file
	        
	        // TSR Folder and File name
	    	tsrFolder = TSRFolder.substring(0, TSRFolder.lastIndexOf(File.separator));
	    	tmpFolder = tsrFolder;
	        file = TSRFolder.substring(TSRFolder.lastIndexOf(File.separator) + 1, TSRFolder.lastIndexOf("."));
	        
	        /* --------------------------------------------------------
	         * DOWNLOAD IMAGES 
	         * -------------------------------------------------------- */
	        // download images before conversion process
			try {
			    warningMessages = global.downloadImages(false);
			    //just close the last opened <div> tag
			    SearchLogger.info("</div>\n", global.getID());
			} catch (Throwable e) {   
			    e.printStackTrace();
			    conversionError = "Unable to download image: " + e.getMessage() + " Document Server. ";
			    SearchLogger.info("<b>Unable to download images: " + e.getMessage() + ".</b>", searchId);
			    return GP_DOWNLOAD_IMAGE_ERROR;
			}
			
			
			
			if( UtilForGwtServer.uploadImagesToSSF(searchId,false, true)<0 ){
				SearchLogger.info("<b>Could NOT RESERVE SSF TRANSACTION ID</b>", searchId);
	   		 	conversionError = "Could NOT RESERVE SSF TRANSACTION ID, TSR generation aborted.";
	   		 	return GP_CREATE_TSR_ERROR;
			}
			
			String apn = "";
			String taxId = "";
		      
			try{
				  String apnVec[] = global.getSa().getAtribute(SearchAttributes.LD_PARCELNO).replaceAll("\\s", "").split("[&,]");;
			      String taxIdVec[] = global.getSa().getAtribute(SearchAttributes.LD_PARCELNO2).replaceAll("\\s", "").split("[&,]");
				  
			      if(apnVec.length>0){
			    	  apn = apnVec[0];
			      }
			       
			      if(taxIdVec.length>0){
			    	  taxId = taxIdVec[0];
			      }
			      
			      if(!apn.matches("[a-zA-z0-9-]*")){
			    	  apn = "";
			      }
			      
			      if(!taxId.matches("[a-zA-z0-9-]*")){
			    	  taxId = "";
			      }
			 }catch(Exception e){e.printStackTrace();}
			
			 
	       
	       //UPLOAD THE BLANK TSR FILE ON SSF
			int imageTransactionId = global.getImageTransactionId();
			DocAdminConn docAdminConn = new DocAdminConn(global.getCommId());
			try{
	    	   String tsrFileName = ServerConfig.getBlankTsrPath();
	    	   if(distributionType==IType.PDF){
	    		   tsrFileName =  ServerConfig.getBlankPdfTsrPath();
	    	   }
	    	   
			   DocumentInfoResultType result = docAdminConn.uploadSearchResult(tsrFileName,
						searchId+"", apn, taxId, imageTransactionId, stateFips, countyFips, Calendar.getInstance().get(Calendar.YEAR),
						DocAdminConn.TSR_INDEX_TYPE,true);
				boolean success = (SuccesType.SUCCESS == result.getStatus());
				if(success){
					
					DocumentInfoType[]  docInfos = result.getDocInfo();
					if(docInfos!=null && docInfos.length>0){
						DocumentInfoType docInfo = docInfos[0];
						String link = docInfo.getLink().toString();	
						global.setTsrLink(link);
						SearchLogger.info("<br/>TSR link <a href='"+link+"'>"+fileId+"</a>"+" ", searchId);
					}else{
						success = false;
						SearchLogger.info("<br/>Couldn't reserve TSR link for  "+searchId +" on SSF. Please check APN in Search Page", searchId);
						return GP_CREATE_TSR_ERROR; 
					}
				}else{
					ErrorContainerType erorContainer = result.getErrors();
					if(erorContainer!=null && erorContainer.getError()!=null && erorContainer.getError().length>0){
						/*ErrorType []allerr = erorContainer.getError();
						for(int i=0;i<allerr.length;i++){
							errorsString += "<br/>" + "Code: "+allerr[i].getCode()+ " Messsage:"+allerr[i].getText();
						}*/
					}
					SearchLogger.info("<br/>Couldn't reserve TSR link for  "+searchId +" on SSF ", searchId);
					return GP_CREATE_TSR_ERROR; 
				}
			}catch (Exception e) {
				logger.error("Couldn't reserve TSR link for "+searchId +" on SSF, " + 
						"] apn = [" + apn +
						"] taxId = [" + taxId + 
						"] imageTransactionId = [" + imageTransactionId + 
						"] stateFips = [" + stateFips + 
						"] countyFips = [" + countyFips + "]"
						, e);
				SearchLogger.info("<br/>Couldn't reserve TSR link for "+searchId +" on SSF "+"<br/><b>Reason: </b>"+e.getMessage(), searchId);
				conversionError = "TSR generation: Exception while uploading Blank TSR on SSF!";
				return GP_CREATE_TSR_ERROR; 
			}
			
			
	       
			// create the TSR index html file
		    String tsrIHtmlIndexPath =  createTsrIndexHtmlFile(tmpFolder, file, new HashMap<String, String>(), 
		    		createdFileNames, createdFileNamesTemplates,new HashMap<String, String>());
		    if(uploadTsriToSssf(docAdminConn, tsrIHtmlIndexPath, apn, taxId, imageTransactionId, stateFips, countyFips)<0){
		    	conversionError = "Could NOT Upload TSRI to SSF, TSR generation aborted.";   
		    	SearchLogger.info("<b>Could NOT Upload TSRI to SSF</b>", searchId);
		    	return GP_CREATE_TSR_ERROR;  
		    }
			
	        /* --------------------------------------------------------
	         * CREATE TEMPLATES 
	         * -------------------------------------------------------- */
			HashMap<String, String> templatesToBeValidated = new HashMap<String, String>();
			HashMap<String, String> goodAttachedFiles = new HashMap<String, String>();
			HashMap<String, String> templatesOnSsf = new HashMap<String, String>();
			
			String stewartOrdersFileName = null;
			String searchOriginUppercase = global.getSa().getAtribute(SearchAttributes.SEARCH_ORIGIN).toUpperCase();
			if(searchOriginUppercase.startsWith("STEWARTORDERS")){
				try {
	        		List<CommunityTemplatesMapper>userTemplates = UserUtils.getUserTemplates(currentAgent.getID().longValue(), global.getProductId());
					for(CommunityTemplatesMapper template:userTemplates){
						if(template.getPath().toUpperCase().contains("NETDIRECTOR")){
							ResponseCreator creator = new ResponseCreator(global, ua);
							
							String netDirectorFileContent =  creator.buildResponse();
							String templateDiretory = UploadTemplate.getTemplatesDirectory(global);
							stewartOrdersFileName = templateDiretory + File.separator + "NETDIRECTOR.xml";
							org.apache.commons.io.FileUtils.writeStringToFile(new File(stewartOrdersFileName), netDirectorFileContent);
							TemplateUtils.setTemplateModified(searchId, template.getId()+"", stewartOrdersFileName);
							//stewartOrdersFileNames.add(stewartOrdersFileName);
							break;
						}
					}
	        	} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			try {
	        	 templatesToBeValidated = fillTemplates();   
	        } catch (Throwable e) {
	            e.printStackTrace();
	            conversionError = "Cannot generate template files.";
	            SearchLogger.info("<b>Cannot generate template files.</b>", searchId);
	            return GP_CREATE_TSR_ERROR;
	        }
	        
	        /* --------------------------------------------------------
	         * Validate templates before uploading to SSF - they must really exist on hard disk
	         * -------------------------------------------------------- */
	        for(String key: (Collection<String>)templatesToBeValidated.keySet()){
	        	String fileName = templatesToBeValidated.get(key).toString();
	        	if(!org.apache.commons.lang.StringUtils.isBlank(fileName)
	        			&& FileUtils.existPath(fileName)){
	        		goodAttachedFiles.put(key, fileName);	        		
	        	}
	        }

	        
	        /* --------------------------------------------------------
	         * upload templates to SSF 
	         * -------------------------------------------------------- */
	        for(String key: (Collection<String>)goodAttachedFiles.keySet()){
	        	String path =  (String)goodAttachedFiles.get(key);
	        	DocumentInfoResultType result = docAdminConn.uploadSearchResult(
	        			path,
	 					searchId + FilenameUtils.getExtension(path)+path.hashCode()%100000, 
	 					apn, 
	 					taxId, 
	 					imageTransactionId, 
	 					stateFips, 
	 					countyFips, 
	 					Calendar.getInstance().get(Calendar.YEAR),
	 					DocAdminConn.TEMPLATE_INDEX_TYPE,
	 					false);
	        	//boolean success = (SuccesType.SUCCESS == result.getStatus());
	        	SearchLogger.info("<br/>",searchId);
	        	
	        	// Task 7724
				String linkText = FilenameUtils.getName(path);
				try {
					linkText = (String) UserUtils.getTemplate(Integer.parseInt(key)).get(UserUtils.TEMPLATE_NAME);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
	        	
	        	if(SuccesType.SUCCESS == result.getStatus() && result.getDocInfo().length>0){
 					DocumentInfoType docInfo = result.getDocInfo()[0];
 					String link = docInfo.getLink().toString();	
 					templatesOnSsf.put(key, link);
 					
 					SsfDocumentMapper ssfDocumentMapper = new SsfDocumentMapper();
					ssfDocumentMapper.loadFrom(docInfo);
					ssfDocumentMapper.setAtsFilePath(path);
					try {
						ssfDocumentMapper.setAtsId(Long.parseLong(key));
					} catch (Exception e) {
						logger.error("Error while trying to parse template ID [" + key + "] SearchId " + searchId, e);
					}
					global.addSsfDocument(ssfDocumentMapper);
					
					SearchLogger.info("<br/>Template: <b><a href='"+link+"'>"+linkText+"</a></b> uploaded on SSF.", searchId);
	 			} else {
	 				ErrorContainerType erorContainer = result.getErrors();
	 				if(erorContainer!=null && erorContainer.getError()!=null && erorContainer.getError().length>0){
	 					/*ErrorType []allerr = erorContainer.getError();
	 					for(int i=0;i<allerr.length;i++){
	 						errorsString += "<br/>" + "Code: "+allerr[i].getCode()+ " Messsage:"+allerr[i].getText();
	 					}*/
	 				}
	 				logger.error("Couldn't upload template: <b>"+linkText+"</b> to SSF SearchId " + searchId);
	 				SearchLogger.info("<br/>Couldn't upload template: <b>"+linkText+"</b> to SSF", searchId);
	 				return GP_CREATE_TSR_ERROR; 
	 			}
	        }
	        
	        /* --------------------------------------------------------
	         * upload templates to ECOR if enabled 
	         * -------------------------------------------------------- */
	        String additionalText = "";
	        if(ServerConfig.isEcoreIntegrationEnabled() 
	        		&& !PlaceOrderService.isStewartOrders(global.getSa()) 
	        		&& !PlaceOrderService.isTitleDesk(global.getSa())){
	        	additionalText += "<br/>Automatic AIM upload status: " + EcorGatewayClient.upload(global, goodAttachedFiles.values());
	        }
	        
	        String searchLogFileName = ServerConfig.getFilePath() + global.getRelativePath() + "logFile.html";   
	        String newLogFileName = tmpFolder + File.separator + file + ".log.html";
	        
	        /* --------------------------------------------------------
	         * SEND AGENT 1ST NOTIFICATION EMAIL
	         * -------------------------------------------------------- */
	        sendNotificationEmail(
	        		displayFileName, 
	        		contextPath, 
	                FileUtils.removeFileExtention(TSRFolder) + "." + distributionType.toString().toLowerCase(), 
	                COMFileName, 
	                goodAttachedFiles, 
	                global, 
	                hasPolicyFiles, 
	                additionalText,
	                NotificationType.AGENT_FIRST,
	                newLogFileName, 
	                templatesOnSsf
	         );
			
	        // creating the list that will contain all included files
	        Vector<DocumentWithContent> allDocumentsWithContext = null;
	        try {
	        	allDocumentsWithContext = getAllTSRFiles(searchFolder, global);
	        } catch (Throwable e) {
			    e.printStackTrace();
			    conversionError = "Cannot get file list from local server.";
			    SearchLogger.info("<b>Cannot get file list from local server.</b>", searchId);
			    return GP_CREATE_TSR_ERROR;
			}
	                
	        synchronized (global) {
	        	TSDFileName = global.getTSDFileName();
			}
	        
	        if(org.apache.commons.lang.StringUtils.isBlank(TSDFileName)) {
	        	logger.error("HUGE error on CreateTSR for SearchID " + searchId + " because TSDFileName is " + TSDFileName);
	        }
	
	        boolean linkedCommitment  = ua.getDISTRIB_LINK()==1 ;
	        
	        /* --------------------------------------------------------
	         * CREATE THE TSR 
	         * -------------------------------------------------------- */ 
	        String tsrFileName = "";
	        try {
	        	// TSR Folder and File name
	        	tsrFolder = TSRFolder.substring(0, TSRFolder.lastIndexOf(File.separator));
	        	tmpFolder = tsrFolder;
	        	file = TSRFolder.substring(TSRFolder.lastIndexOf(File.separator) + 1, TSRFolder.lastIndexOf("."));
	            
	            // TSR FileId
	            String titleDocumentContext = org.apache.commons.io.FileUtils.readFileToString(new File(TSDFileName));
	            PrivacyStatement privacyStatement = new PrivacyStatement(global, ca);
	            
	            TsrCreator tsrCreator = new TsrCreator(
	                		titleDocumentContext, 
	                		allDocumentsWithContext, 
	                		distributionType, 
	                		privacyStatement.getPrivacyStatement(), fileId,searchId,linkedCommitment);
	                        
	            // add tsr to the list of created files
	            tsrFileName = tmpFolder + File.separator + file + "." + distributionType.toString().toLowerCase();
	            org.apache.commons.io.FileUtils.writeByteArrayToFile(new File(tsrFileName), tsrCreator.getCreatedTsr());
	            createdFileNames.add(tsrFileName);
	    		
	            
	        } catch (Throwable e) {
			    e.printStackTrace();
			    logger.error("Cannot initialize graphics api for TSR generation on SearchId " + searchId, e);
			    conversionError = "Cannot initialize graphics api. TSR generation aborted. (internal exception received " + e.getClass().getName() + ")";
			    SearchLogger.info("<b><br>Cannot initialize graphics api for TSR generation.</b> (internal exception received " + e.getClass().getName() + ")", searchId);
			    return GP_CREATE_TSR_ERROR;
			}
	
	        logger.debug( "\nNEW Conversion Time = " + (System.currentTimeMillis() - startTime) / 1000 + " sec for SearchId " + searchId + "\n");
	
	        global.setNormalStopConvert(TSRFolder);
	        
	        /* --------------------------------------------------------
	         * CREATE THE TSR - ENDED 
	         * -------------------------------------------------------- */
	        
	                
	        // copy the original search log file - for now I need this to be uploaded to TitleDesk
//	        String logFileName = BaseServlet.FILES_PATH + global.getRelativePath() + "logFile.html";
//	        String newLogFileName = tmpFolder + File.separator + file + ".log.html";        
	        try
	        {
	        	File logFile = new File(searchLogFileName);
	        	File newLogFile = new File(newLogFileName);
	        	
	        	if(global.getSa().isLogInDatabase()) {
	        		org.apache.commons.io.FileUtils.writeByteArrayToFile(newLogFile, DBManager.getSearchOrderLogs(searchId, FileServlet.VIEW_LOG, false));
	        	} else {
		            FileCopy.copy(logFile, newLogFile);
		            if(newLogFile.exists()){
		            	createdFileNames.add(newLogFileName);
		            }
	        	}
	        }
	        catch( Exception e ) {}
	        
			// create the TSR index html file
			tsrIHtmlIndexPath = createTsrIndexHtmlFile(tmpFolder, file,
					goodAttachedFiles, createdFileNames,
					createdFileNamesTemplates, templatesOnSsf);

			if (uploadTsriToSssf(docAdminConn, tsrIHtmlIndexPath, apn, taxId,
					imageTransactionId, stateFips, countyFips) < 0) {
				conversionError = "Could NOT Upload TSRI to SSF, TSR generation aborted.";
				SearchLogger.info("<b>Could NOT Upload TSRI to SSF.</b>", searchId);
				return GP_CREATE_TSR_ERROR;
			}
			
			try {
				AsynchSearchLogSaverThread.writeTsrILogFile(global, 
						org.apache.commons.io.FileUtils.readFileToString(new File(tsrIHtmlIndexPath)));
			} catch (Exception e) {
				logger.error("Cannot write TsrILog for search " + global.getID(), e);
			}
			
//			String tsrIndexFileName = global.getSearchDir() + File.separator + "tsrIndexFile.html";
//			File tsrIndexFile = new File(tsrIndexFileName);
//			if(tsrIndexFile.exists()) {
//				tsrIndexFile.delete();
//			}
//			try {
//				org.apache.commons.io.FileUtils.copyFile(new File(tsrIHtmlIndexPath), tsrIndexFile);
//			} catch (Exception e) {
//				logger.error("Error on copying tsrindex html first try", e);
//				try {
//					org.apache.commons.io.FileUtils.copyFile(new File(tsrIHtmlIndexPath), tsrIndexFile);
//				} catch (Exception e1) {
//					logger.error("Error on copying tsrindex html second try", e1);
//				}
//			}
			
			
	       
	        // copy into tsrFileNames all the created files
	        HashSet<String> tsrFileNames = new HashSet<String>();         
	        for(String fileName: createdFileNames){
	        	if((new File(fileName)).exists()){
	        		tsrFileNames.add(fileName);
	        		if(!tsrFileName.equals(fileName)){
	        			if(!originalValueOfTheMemberFieldTSRFolder.equals(TSRFolder)) {
		        			String toAdvertiseFileName = fileName.replace("-" + fileId + "_", "-" + pervertedFileId + "_");
		        			try {
								File toAdvertiseFile = new File(toAdvertiseFileName);
								File presentFile = new File(fileName);
								if(toAdvertiseFile.exists()) {
									org.apache.commons.io.FileUtils.deleteQuietly(toAdvertiseFile);
								}
								org.apache.commons.io.FileUtils.copyFile(presentFile, toAdvertiseFile);
		        			} catch (Exception e) {
		        				logger.error("Error while replicating files 2 on SearchId " + searchId, e);
		        				
							}
		        		}
	        		}
	        	} else {        		
	        		// radu: we ignore this, as requested by CM, to preserve existing functionality
	        		logger.warn("Following TSR-related file does not actually exist: " + fileName + " on SearchId " + searchId);
	        	}
	        }
	        for(String fileName: createdFileNamesTemplates){
	        	if((new File(fileName)).exists()){
	        		tsrFileNames.add(fileName);
	        	} else {        		
	        		// radu: we ignore this, as requested by CM, to preserve existing functionality
	        		logger.warn("Following TSR-related file does not actually exist: " + fileName + " on SearchId " + searchId);
	        	}        	
	        }
	        
	        //UPLOAD THE TSR FILE ON SSF
	        try{
	        	DocumentInfoResultType result = docAdminConn.uploadSearchResult(tsrFileName,
						searchId+"", apn, taxId, imageTransactionId, stateFips, countyFips, Calendar.getInstance().get(Calendar.YEAR), 
						DocAdminConn.TSR_INDEX_TYPE,true);
				boolean success = (SuccesType.SUCCESS == result.getStatus());
				if(success){
					if(result.getDocInfo().length>0){
						DocumentInfoType docInfo = result.getDocInfo()[0];
						String link =docInfo.getLink().toString();	
						global.setTsrLink(link);
						
						SsfDocumentMapper ssfDocumentMapper = new SsfDocumentMapper();
						ssfDocumentMapper.loadFrom(docInfo);
						ssfDocumentMapper.setAtsFilePath(tsrFileName);
						global.addSsfDocument(ssfDocumentMapper);
						
						SearchLogger.info("<br/><br/>TSR: <a href='"+link+"'>"+fileId+"</a> uploaded to SSF. ", searchId);
					}else{
						SearchLogger.info("<br/>Couldn't upload TSR "+searchId +" to SSF ", searchId);
						success = false;
					}
				}else{
					ErrorContainerType erorContainer = result.getErrors();
					if(erorContainer!=null && erorContainer.getError()!=null && erorContainer.getError().length>0){
						/*ErrorType []allerr = erorContainer.getError();
						for(int i=0;i<allerr.length;i++){
							errorsString += "<br/>" + "Code: "+allerr[i].getCode()+ " Messsage:"+allerr[i].getText();
						}*/
					}
					SearchLogger.info("<br/>Couldn't upload TSR "+searchId +" to SSF ", searchId);
					return GP_CREATE_TSR_ERROR; 
				}
			}catch (Throwable e) {
				logger.error("Couldn't upload TSR "+searchId +" on SSF, " +
						"tsrFileName = [" + tsrFileName + 
						"] apn = [" + apn +
						"] taxId = [" + taxId + 
						"] imageTransactionId = [" + imageTransactionId + 
						"] stateFips = [" + stateFips + 
						"] countyFips = [" + countyFips + "]"
						, e);
				SearchLogger.info("<br/>Couldn't upload TSR "+searchId +" on SSF "+"<br/><b>Reason: </b>"+e.getMessage(), searchId);
				conversionError = "TSR generation: Exception while uploading files on SSF!";
				return GP_CREATE_TSR_ERROR; 
			}
	       
	        boolean isUpdate = Products.UPDATE_PRODUCT == global.getSearchProduct();
	        
	        if(searchOriginUppercase.startsWith("TITLEDESK")){
	        	String orderId = global.getSa().getAtribute(SearchAttributes.TITLEDESK_ORDER_ID);
	        	logger.debug(global.getID() + ": Trying to upload to Title Desk for orderId = " + orderId);	        	
	        	SearchLogger.infoUpdateToDB("\n</div><div><BR><B>Trying to upload to Title Desk</B> on: " + 
	        			SearchLogger.getTimeStampFormat1(user.getUserAttributes().getNiceName(), "&#44; ") + "<BR></div>\n", global.getID());
	        	String newText = TitleDesk.sendFiles(orderId, tsrFileNames, isUpdate);
	        	additionalText += newText;
	        	SearchLogger.info("<div>"+newText+"</div>", searchId);
	        } else if( searchOriginUppercase.startsWith("STEWARTORDERS")){
	        	
	        	String orderId = global.getSa().getAtribute(SearchAttributes.STEWARTORDERS_ORDER_ID);
	        	CommunityAttributes  ca = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity();
	        	Set<String> stewartOrdersFileNames = new LinkedHashSet<String>();
	        	stewartOrdersFileNames.addAll(tsrFileNames);
	        	
	        	if("IL".equalsIgnoreCase(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv())){
		        	try {
		    			addSearchDocFileForIL(global, stewartOrdersFileNames);
		    		}catch (IOException e) {
		    			SearchLogger.info("<br> Error reading TP3 doc files <br>", searchId);
		    		}
	        	}
	        	
	        	List<CommunityUserTemplatesMapper> commUserTemplates = null;
	        	try {
	        		commUserTemplates = DBManager .getSimpleTemplate().query( 
							UserUtils.SQL_TEMPLATES_LIST_FOR_USER_PROFILE,
							new CommunityUserTemplatesMapper(), currentAgent.getID().longValue(), ca.getID().intValue());
	        		
	        	} catch (Exception e) {
					e.printStackTrace();
				}
	        	
	        	List<SsfDocumentMapper> allSSfDocs = global.getSffDocumentWithType(DocAdminConn.TEMPLATE_INDEX_TYPE);
	        	allSSfDocs.addAll(global.getSffDocumentWithType(DocAdminConn.TSR_INDEX_TYPE));
	        	
	        	String newText = StewartOrders.sendFiles(ca.getID().intValue(),orderId, stewartOrdersFileNames ,isUpdate, global.isStarterStewartOrders(),
	        			global.getSa().getAtribute(SearchAttributes.AGENT_USER),
	        			global.getSa().getAtribute(SearchAttributes.AGENT_PASSWORD), 
	        			global.getAllImagesSsfLink()+"&includeSearchResults=true",
	        			commUserTemplates, allSSfDocs);
	        	if(newText.contains("<font color=\"red\">")||newText.contains("<font color='red'>")){
	        		//conversionError = "Could not send the results to OrdersGateway ";
	        		SearchLogger.info("<br><br><font color=\"red\">Could not send the results to OrdersGateway!</font><br><br>", searchId);
	        		//return GP_CREATE_TSR_ERROR;
	        		additionalText += "<font color=\"red\">Could not send the results to OrdersGateway!</font>";
	        	} else {
	        		additionalText +=newText;
	        	}
	        	
	        	if(stewartOrdersFileName!=null && (new File(stewartOrdersFileName)).exists()){
	        		
	        		// upload the files
	        		FtpClient ftp = new FtpClient();
	        		try{
	        			boolean success = false;
	        			for(int i=0; i<3 && !success; i++){
	        				success = ftp.connect(ServerConfig.getNetDirectorFtpSite(), ServerConfig.getNetDirectorFtpUser(), ServerConfig.getNetDirectorFtpPassword());
	        			}
	        			if(success){
	        				List<FTPFileStruct> uploadFiles = new ArrayList<FtpClient.FTPFileStruct>();
		        			uploadFiles.add(new FTPFileStruct(new File(stewartOrdersFileName), global.getAbstractorFileNo()+"_"+searchId+".xml", null));
		        			
		        			String message =null;
		        			try{
		        				message = ftp.uploadFiles(uploadFiles, "/");
		        			}catch(RuntimeException e){
		        				message = "<font color=\"red\">Error occured. Upload not performed!</font>";
		        			}
		        			newText+= "\n<br/><b><u>NETDIRECTOR Upload Status</u>:</b> " + message; 
	        			}else{
	        				newText+= "\n<font color=\"red\">ERROR: Could not connect to NETDIRECTOR FTP site drive !</font><br/>";
	        			}
	        		}finally{
	        			ftp.disconnect();
	        		}
	        	}
	        	
	        	SearchLogger.info("<div>"+newText+"</div>", searchId);
	        } else if( searchOriginUppercase.equals("NDEX")){
	        	NdexPackage ndexPackage = new NdexPackage();
	        	ndexPackage.setSearch(global);
	        	ndexPackage.setDistributionType(distributionType);
	        	ndexPackage.setRunSheet(new File(tsrIHtmlIndexPath));
	        	ndexPackage.setTitleSearch(new File(tsrFileName));
	        	
	        	StringBuilder imagesLog = new StringBuilder("<div>Included Files in the TSC package:<br>");
	        	
	        	for (DocumentWithContent documentWithContent : allDocumentsWithContext) {
					DocumentI document = documentWithContent.getDocument();
					
					if("AO".equals(document.getDataSource())) {
						ndexPackage.getTitleSearchCopies().add(document);
						imagesLog.append("AO Data Sheet<br>");
					} else if(document.hasImage() 
							&& document.isIncludeImage() 
							&& StringUtils.isNotEmpty(document.getImage().getSsfLink())) {
						
						ndexPackage.getTitleSearchCopies().add(document);
						imagesLog.append("Image ").append(document.prettyPrint()).append("<br>");
						/*
						if(document.is(DocumentTypes.ASSIGNMENT)) {
							ndexPackage.getAssigments().add(document);
						} else if(document.is(DocumentTypes.LIEN, DocumentTypes.SUBCATEGORY_FEDERAL_TAX_LIEN)) {
							ndexPackage.getFederalTaxLiens().add(document);
						} else {
							ndexPackage.getTitleSearchCopies().add(document);
						}
						*/
					}
				}
	        	imagesLog.append("</div>");
	        	
	        	String message = null;
	        	try {
	        		message = FTPWriterNdex.writePackage(ndexPackage);
	        	} catch(RuntimeException e){
    				message = "<font color=\"red\">Error occured. Upload not performed on Ndex FTP!</font>";
    			}
	        	SearchLogger.info("<div>"+message+"</div>" + imagesLog, searchId);
	        	
			} else {// upload info on TitleDesk or TP3 if on ILCook
	        	additionalText += uploadFilesLdrive(allDocumentsWithContext);
	        	if(additionalText.contains("<font color=\"red\">")){
	        		//conversionError = "Could not upload results to L drive!";
	        		//return GP_CREATE_TSR_ERROR;
	        		SearchLogger.info("<font color=\"red\">Could not upload results to L drive!</font>", searchId);
	        		additionalText += "<font color=\"red\">Could not upload results to L drive!</font>";
	        	}        	
	        }
	        
	        String surecloseFileId = global.getSa().getAtribute(SearchAttributes.SURECLOSE_FILE_ID);
	        logger.info("SURECLOSE ------  SureClose fileId: "+surecloseFileId + " on SearchId " + searchId);
	        if(PlaceOrderService.isStewartOrders(global.getSa())){
	        	SearchLogger.info("SureClose received UTI: <b> "+surecloseFileId+" </b><br/> ", searchId);
	        }

	        if(StringUtils.isNotEmpty(surecloseFileId)){
	        	String additionalInformation = global.getSa().getAtribute(SearchAttributes.ADDITIONAL_INFORMATION);
	        	//upload images to sureclose
				try { 
					UserAttributes currentUA = this.getCurrentAgent();
					
					String communityId = ca.getID().toString();
					String user = SitesPasswords.getInstance().getPasswordValue(communityId, "SureClose","user");
					String password = SitesPasswords.getInstance().getPasswordValue(communityId, "SureClose","password");
					SureCloseConn surecloseConn = new SureCloseConn( user, password, ServerConfig.getSureCloseLink() );
					
					boolean enableReportUpload = true;

					if(currentUA!=null){
						enableReportUpload = currentUA.getSEND_REPORT_FORCLOSURE() == 1;
					}
					
					if(enableReportUpload){
						
						String placeHolder = org.apache.commons.lang.StringUtils.defaultString(ca.getPLACEHOLDER());
						
						if(StringUtils.isEmpty(placeHolder) || "N/A".equals(placeHolder))
							placeHolder = FilenameUtils.getBaseName(tsrFileName);
						
						CallResult resultTSRExport = surecloseConn.importDocument(FilenameUtils.getName(tsrFileName), org.apache.commons.io.FileUtils.readFileToByteArray(new File(tsrFileName)),
								placeHolder, surecloseFileId,  distributionType==IType.PDF);
						if( resultTSRExport==null || (resultTSRExport!=null && resultTSRExport.status != CallStatus.SUCCESS) ){
							SearchLogger.info("<font color=\"red\">Could not upload TSR "+FilenameUtils.getName(tsrFileName)+" to SureClose !</font><br/>", searchId);
			        		additionalText += "<font color=\"red\">Could not upload TSR "+FilenameUtils.getName(tsrFileName)+" to SureClose !</font>\n";
			        		additionalInformation += "Could not upload TSR "+FilenameUtils.getName(tsrFileName)+" to SureClose !<br/>";
						}else{
							SearchLogger.info("<font color=\"green\">Success uploading TSR "+FilenameUtils.getName(tsrFileName)+" to SureClose. </font><br/>", searchId);
						}
					}
					
					boolean enableImageUpload = true;
	
					if(currentUA!=null){
						enableImageUpload = currentUA.getSEND_IMAGES_FORCLOSURE() == 1;
					}
							
					if(enableImageUpload){
						for( DocumentWithContent doc:allDocumentsWithContext ){
			        		if(doc.getImageContent()!=null && doc.getImageContent().length>0){
			        			boolean exception = false;
			        			CallResult result = null;
			        			// Task 8778
			        			String name = doc.getDocument().getDocType() + "_" + doc.getDocument().getDocSubType() + "_" + doc.getDocument().prettyPrint();
			        			try {
									result = surecloseConn.importDocument(name+"."+ FilenameUtils.getExtension(doc.getFileName()), doc.getImageContent(), name, surecloseFileId,  distributionType==IType.PDF);
								}catch (Exception e){
									exception = true;
									e.printStackTrace();
								}
								if( exception || result==null || (result!=null && result.status != CallStatus.SUCCESS) ){
									SearchLogger.info("<font color=\"red\">Could not upload image "+name+" to SureClose !</font><br/>", searchId);
					        		additionalText += "<font color=\"red\">Could not upload image "+name+" to SureClose !</font>\n";
					        		additionalInformation += "Could not upload image "+name+" to SureClose !<br/>";
								}else{
									SearchLogger.info("<font color=\"green\">Success uploading image "+name+" to SureClose. </font><br/>", searchId);
								}
			        		}
			        	}	
					}
				}catch (Throwable e) {
					e.printStackTrace();
					SearchLogger.info("<font color=\"red\">Could not upload results to SureClose !</font><br/>", searchId);
	        		additionalText += "<font color=\"red\">Could not upload results to SureClose !</font>\n";
	        		additionalInformation+= "Could not upload results to SureClose !<br/>";
				}
				global.getSa().setAtribute(SearchAttributes.ADDITIONAL_INFORMATION, additionalInformation);
	        }
	        
	        /* --------------------------------------------------------
	         * SEND AGENT 2ND NOTIFICATION EMAIL 
	         * -------------------------------------------------------- */
	        sendNotificationEmail(
	        		displayFileName, 
	        		contextPath, 
	                FileUtils.removeFileExtention(TSRFolder) + "." + distributionType.toString().toLowerCase(), 
	                COMFileName, 
	                null,  // does not have templates
	                global, 
	                false, // does not have templates 
	                additionalText,
	                NotificationType.AGENT_SECOND,
	                newLogFileName, null
	        );
	        
	        /* --------------------------------------------------------
	         * SEND THE USUAL NOTIFICATION EMAILS
	         * -------------------------------------------------------- */
	        sendNotificationEmail(
	        		displayFileName, 
	        		contextPath, 
	                FileUtils.removeFileExtention(TSRFolder) + "." + distributionType.toString().toLowerCase(), 
	                COMFileName, 
	                goodAttachedFiles, 
	                global, 
	                hasPolicyFiles, 
	                additionalText,
	                NotificationType.USUAL,
	                newLogFileName, templatesOnSsf
	        );
	        
	        if(notificationFile != null && pervertedFileId != null) {
	        	notificationFile = notificationFile.replace("-" + fileId + "_", "-" + pervertedFileId + "_");
	        }
	        
	        //update time statistics
	        ConvertToPdfTime.update(System.currentTimeMillis() - startTime);
	        
	        UtilForGwtServer.deleteImages(searchId, linkedCommitment);
	        
	        return GP_SUCCESS;
        
        } finally {
        	Set<String> allCreatedFileNames = new HashSet<String>();
    		allCreatedFileNames.addAll(createdFileNames);
    		allCreatedFileNames.addAll(createdFileNamesTemplates);
    		allCreatedFileNames.add(TSDFileName);
    		
        	if(!originalValueOfTheMemberFieldTSRFolder.equals(TSRFolder)) {
        		
        		// remove reference to list of temporary files 
        		System.out.println(allCreatedFileNames);
        		
        		for (String createdFileName : allCreatedFileNames) {
        			String toKeepFileName = createdFileName.replace("-" + fileId + "_", "-" + pervertedFileId + "_");
        			try {
						File createdFile = new File(createdFileName);
						File toKeepFile = new File(toKeepFileName);
						if(toKeepFile.exists()) {
							org.apache.commons.io.FileUtils.deleteQuietly(toKeepFile);
						}
						//org.apache.commons.io.FileUtils.copyFile(createdFile, toKeepFile);
						org.apache.commons.io.FileUtils.deleteQuietly(createdFile);

        			} catch (Exception e) {
        				logger.error("SearchId " + searchId + ": Error while deleting " + createdFileName + " and/or " + toKeepFileName, e);
					}
				}
        		TSRFolder = originalValueOfTheMemberFieldTSRFolder;
        	} else {
        		for (String createdFileName : allCreatedFileNames) {
        			try {
						File createdFile = new File(createdFileName);
						org.apache.commons.io.FileUtils.deleteQuietly(createdFile);
						logger.debug("SearchId " + searchId + ": Deleted " + createdFileName);
        			} catch (Exception e) {
						logger.error("SearchId " + searchId + ": Error while deleting " + createdFileName , e);
					}
        		}
        	}
        	
        	
        	
        	
        }
    }
    
    private int uploadTsriToSssf(DocAdminConn docAdminConn, String tsrIHtmlIndexPath, String apn, String taxId, int imageTransactionId, int stateFips, int countyFips ){
    	if(!StringUtils.isEmpty(tsrIHtmlIndexPath)){
 	       // upload the TSR index html file to SSF
 	       DocumentInfoResultType resultTsriUpload = docAdminConn.uploadSearchResult(tsrIHtmlIndexPath,searchId+"", 
 	    		   apn, taxId, imageTransactionId, stateFips, countyFips, 
 	    		   Calendar.getInstance().get(Calendar.YEAR),DocAdminConn.TEMPLATE_INDEX_TYPE, true);
 	       boolean successTsrI = (SuccesType.SUCCESS == resultTsriUpload.getStatus());
 	       SearchLogger.info("<br/>",searchId);
 	       if(successTsrI){
 				if(resultTsriUpload.getDocInfo().length>0){
 					DocumentInfoType docInfo = resultTsriUpload.getDocInfo()[0];
 					String link =docInfo.getLink().toString();	
 					global.setTsriLink(link);
 					SearchLogger.info("<br/>TSRI Html file: <b><a href='"+link+"'>"+FilenameUtils.getName(tsrIHtmlIndexPath)+"</a></b> uploaded on SSF.", searchId);
 				}else{
 					successTsrI = false;
 				}
 			}else{
 				ErrorContainerType erorContainer = resultTsriUpload.getErrors();
 				if(erorContainer!=null && erorContainer.getError()!=null && erorContainer.getError().length>0){
 					/*ErrorType []allerr = erorContainer.getError();
 					for(int i=0;i<allerr.length;i++){
 						errorsString += "<br/>" + "Code: "+allerr[i].getCode()+ " Messsage:"+allerr[i].getText();
 					}*/
 				}
 				SearchLogger.info("<br/>Couldn't upload TSRI Html file: <b>"+FilenameUtils.getName(tsrIHtmlIndexPath)+" </b> to SSF", searchId);
 				
 				return -1; 
 			}
        }
    	return 0;
    }

    public static String createTsrIndexHtmlContents(boolean showTemplatesAsLinks, 
    				Search global, String contextPath, 
    				HashMap<String, String>attachedFiles, List<String> createdFileNamesTemplates, HashMap<String,String> newTemplates ) {
    	
    	StringBuilder str = new StringBuilder();
    	
    	String fileNo = "";
		String soOrderId = "";
		long searchId = 0;
		if (global != null) {
			SearchAttributes sa = global.getSa();
			fileNo = sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO);
			soOrderId = sa.getAtribute(SearchAttributes.STEWARTORDERS_ORDER_ID);
			searchId = global.getID();
		}
		StringBuilder fileSearchSourceId = new StringBuilder();
		if (fileNo.length() > 0) {
			fileSearchSourceId.append("[" +
					StringUtils.HTMLEntityEncode(fileNo)
					+ "]");
		} else {
			fileSearchSourceId.append("[]");
		}
		fileSearchSourceId.append("[" + Long.toString(searchId) + "]");
		if (!StringUtils.isEmpty(soOrderId)) {
			fileSearchSourceId.append("[" + soOrderId + "]");
		}
		fileSearchSourceId.append("</font>  ");
		str.append(fileSearchSourceId.toString());
		
    	SimpleDateFormat dateFormat = new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY);
    	
    	StringBuilder datesTable = new StringBuilder();
    	datesTable.append("<table><tr>");
    	String effectiveStartDateString = global.getSa().getEffectiveStartDateAsString();
    	if (effectiveStartDateString==null) {
    		effectiveStartDateString = "*";
    	}
    	datesTable.append("<td>Effective Start Date: </td>");
    	datesTable.append("<td>" + effectiveStartDateString + "&nbsp;&nbsp;</td>");
    	Date effectiveEndDate = global.getSa().getCertificationDate().getDate();
    	String effectiveEndDateString = "*";
    	if (effectiveEndDate!=null) {
    		effectiveEndDateString = dateFormat.format(effectiveEndDate);
    	}
    	datesTable.append("<td>Effective End Date: </td>");
    	datesTable.append("<td>" + effectiveEndDateString + "&nbsp;&nbsp;</td>");
    	Date startViewDate = global.getDocManager().getStartViewDate();
    	String startViewDateString = "*";
    	if (startViewDate!=null) {
    		startViewDateString = dateFormat.format(startViewDate);
    	}
    	datesTable.append("<td>Start View Date: </td>");
    	datesTable.append("<td>" + startViewDateString + "&nbsp;&nbsp;</td>");
    	Date endViewDate = global.getDocManager().getEndViewDate();
    	String endViewDateString = "*";
    	if (endViewDate!=null) {
    		endViewDateString = dateFormat.format(endViewDate);
    	}
    	datesTable.append("<td>End View Date: </td>");
    	datesTable.append("<td>" + endViewDateString + "&nbsp;&nbsp;</td>");
    	datesTable.append("</tr></table>");
    	str.append(datesTable);
    	str.append(   /*  put here the TSRIndex representation  */global.getAsHTML());
		
		StringBuilder sb_status = new StringBuilder();
		Vector<String[]> ss = global.getSearchStatus();
		boolean addNbsp = true;
		for(int ii = 0; ii < ss.size(); ii++) {
			String[] entry = ss.get(ii);
			if (!"".equals(entry[0])) {
				addNbsp = false;
				String statusColor  = entry[1].startsWith("done") ? "green" : "red";
	            String statusServer = new String(entry[1]);
	            if(entry[1].startsWith("done")) {
	               statusServer  = entry[1].substring(4);
	            }
				sb_status.append("<li>" + entry[0] 
					+ "&nbsp;:&nbsp;<font color=\"" + statusColor + "\">" + statusServer + "</font></li>");
			}
		}
		
		String requests = RequestCount.getReportForTSRI(global.getID());
		if(org.apache.commons.lang.StringUtils.isNotBlank(requests)) {
			sb_status.append("<br/>"+requests);
		}
		
		String certificationDates = global.getSa().getCertificationDateManager().toHtml();
		if(org.apache.commons.lang.StringUtils.isNotBlank(certificationDates)) {
			sb_status.append("<br/>").append(certificationDates);
		}
		
		if (addNbsp) {
			sb_status.append("&nbsp;");
		}
    	
		String warnings = "";
		
		try {
			for(String warning: WarningUtils.calculateWarnings(global.getID())){
					warnings += "<tr><td><li><font color=\"red\">"+warning+"</font></li></td></tr>";
			}
		}catch(Exception e) { e.printStackTrace(); }
		if (!warnings.isEmpty()) {//9366 - WP checkboxes on two columns and underneath them the Warnings
			warnings = "<tr><td colspan=\"2\" align=\"center\"><table>" + warnings + "</table></td></tr>";
		}
		
		StringBuffer rcList = new StringBuffer();
		LinkedHashMap<String, ReviewChecker> reviewCheckList = global.getSa().getReviewCheckList();
		int reviewCheckListIndex = 0;
    	if (reviewCheckList != null){
    		for (Entry<String, ReviewChecker> entry : (Set<Entry<String, ReviewChecker>>) reviewCheckList.entrySet()) {
    			ReviewChecker rc = entry.getValue();
    			if (rc != null){
					rcList.append((reviewCheckListIndex % 2 == 0 ? "<tr>" : ""));
					rcList.append("<td><input type=\"checkbox\" ");
    				if (rc.isReviewFlagValue()){
    					rcList.append("checked");
    				}
    				rcList.append(" disabled>").append(rc.getReviewFlagTooltip());
    				if (org.apache.commons.lang.StringUtils.isNotEmpty(rc.getReviewFlagChangedDateAsString())){
    					rcList.append(" @ ").append(rc.getReviewFlagChangedDateAsString());
    				}
    				rcList.append("</td>");
					rcList.append((reviewCheckListIndex % 2 == 0 ? "" : "</tr>"));
					reviewCheckListIndex++;
    			}
    		}
    	}
		String rcListAndWarnings = rcList + warnings;
		if (!rcListAndWarnings.isEmpty()) {
			rcListAndWarnings = "<table>" + rcListAndWarnings + "</table>";
		}
		str.append(START_TABEL_TEMPLATE.replace(ERRORS_TAG, rcListAndWarnings).replace(STATUS_TAG, sb_status.toString()));
    	
    	Object[] tempaltesIds = attachedFiles.keySet().toArray();
    	int i=0;
    	//        	 add the templates to the createdFileNames         
        for(Entry<String, String> entry : (Set<Entry<String, String>>)attachedFiles.entrySet()){
        	String fileName = entry.getValue();
        	createdFileNamesTemplates.add(fileName);
        	
        	int poz = fileName.lastIndexOf(File.separator);
        	String shString = fileName;
        	if(poz>0){
        		shString = fileName.substring(poz+1);
        	}
        	String policyName = UserUtils.getTemplatePolicyName(global.getCommId(), shString);
        	if (!StringUtils.isEmpty(policyName)) {
        		shString = policyName;
        	} else { 
        		String key = entry.getKey();
        		policyName = "";
        		if (key.matches("\\d+")) {
        			try {
            			//when TSR is created, the key is the template id 
            			policyName = (String) UserUtils.getTemplate(Integer.parseInt(key)).get(UserUtils.TEMPLATE_NAME);
    				} catch (NumberFormatException e) {
    					e.printStackTrace();
    				}
    			}
        		if (StringUtils.isEmpty(policyName)) {
        			shString = key;
        		} else {
        			shString = policyName;
        		}
        	}
        	String link ="";
        	if(showTemplatesAsLinks) {
        		try {
		        	String eolPolicyFile = contextPath
							+ FileUtils.ReplaceAllFileSeparators(
									((String) attachedFiles.get(tempaltesIds[i].toString())).
										substring(((String) attachedFiles.get(tempaltesIds[i].toString())).indexOf(
												File.separator	+ "TSD" + File.separator)),
									"/");
		        	Matcher m = patternCod.matcher(eolPolicyFile);
		        	boolean templateNegenerat = false;
					if(m.find()) templateNegenerat=true;
					String displayEolPolicyFile = attachedFiles.get(tempaltesIds[i].toString()).toString();
					
					if(!templateNegenerat ){
						if(newTemplates==null||newTemplates.size()==0){
							link= "<A href=\""+ lbURL+":"+ lbPort
				                + contextPath
				                + URLMaping.PDF_SHOW
				                + "?pdfFile="
				                + eolPolicyFile
				                + "&displayPdfFile="
				                + displayEolPolicyFile
				                + "&agentId="
				                + global.getAgent().getID()
				                + "\" target=\"_blank\"><B>" + shString + "</B></A><br>";
						}else{
							link= "<A href=\""
								+newTemplates.get(tempaltesIds[i].toString())+
								"\" target=\"_blank\"><B>" + shString + "</B></A><br>";
						}
					} 
	        	} catch (Exception e) {
					e.printStackTrace();
					link = shString;
				}
        	}else {
        		link = shString;
        	}
        	str.append("<tr><td></td><td></td>");
        	str.append("<td id=\"slot0\"> <div class=\"gwt-Hyperlink\">"
        			+
        			link
        			+
        			"</div></td>");
        	
        	str.append("<td> </td><td> </td>");
        	str.append("<td id=\"state0\">");
        	str.append("<div class=\"gwt-labelState\">&nbsp;</div></td></tr>");
        	i++;
        }
        
    	str.append(END_TABEL_TEMPLATE);
    	
    	str.append("</table>");
    	
    	return str.toString();
    }
    
    private String createTsrIndexHtmlFile(String tmpFolder, String file, 
    		HashMap<String, String> attachedFiles, 
    		List<String> createdFileNames, List<String> createdFileNamesTemplates,HashMap<String, String> newTemplates) {
    	try{
        	String tsrIndexFileName = tmpFolder + File.separator + file + ".tsr.html";  
        	PrintWriter pw = new PrintWriter(tsrIndexFileName);
        	String str = createTsrIndexHtmlContents(true, global, contextPath, attachedFiles, createdFileNamesTemplates, newTemplates);
        	pw.print("<html><head><title>TSR Index Page</title></head><body>\n");
        	pw.print(str);
        	pw.print("</body></html>");
        	pw.flush();
        	pw.close();
        	createdFileNames.add(tsrIndexFileName);
        	return tsrIndexFileName;
        }catch(Exception ignored){
        	ignored.printStackTrace();
        }
		return null;
	}

	public class FileInfo {
        public String fileName;
        public String instType;
        public FileInfo(String fileName, String instType) {
            this.fileName = fileName;
            this.instType = instType;
        }
    }

	private Vector<DocumentWithContent> getAllTSRFiles(String searchFolder, Search search) {
    	
    	Vector<DocumentWithContent> allDocumentsWithContext = new Vector<DocumentWithContent>();
    	final DocumentsManagerI docManager = search.getDocManager();
    	
    	
    	if(docManager != null) {
	    	try {
	    		docManager.getAccess();
	    		for (DocumentI document : docManager.getDocumentsList()) {
	    			if(document.isChecked()) {
		    			DocumentWithContent docWithContext = new DocumentWithContent();
		    			docWithContext.setDocument(document);
		    			DataSite dataSite = null;
		    			try {
		    				if(document.getSiteId()!=-1) {
				    			dataSite = HashCountyToIndex.getDateSiteForMIServerID(String.valueOf(document.getSiteId()));
								if(!dataSite.isEnabledIncludeInTsr(search.getCommId())) {
				    				continue;
				    			}
		    				}
		    			}catch(Exception e) {
		    				e.printStackTrace();
		    			}
		    			if(document.isChecked() && 
		    					document.hasImage() &&
		    					document.isIncludeImage() && 
		    					document.getImage().isSaved()) {
		    				if(dataSite != null && dataSite.isEnabledIncludeImageInTsr(search.getCommId())) {
			    				byte[] imageContent = document.getImage().getContent();
			    				docWithContext.setImageContent(imageContent);
		    				}
		    			}
		    			//B3744	Never include documents indexes in the final TSR
		    			boolean isSBDoc = TsrCreator.isSBDoc(docWithContext);
		    			boolean isHODoc = TsrCreator.isHODoc(docWithContext);
		    			if (document instanceof AssessorDocumentI || 
		    					document instanceof TaxDocumentI ||
		    					document instanceof CourtI || isSBDoc|| isHODoc) {
		    				try {
			    				if(document.isUploaded() || document.isFake()) {
			    					docWithContext.setIndexContent( GPThread.getIndexForUploaded(search, document.getId()) );
			    				} else {
					    			docWithContext.setIndexContent(Tidy.tidyParse(DBManager.getDocumentIndex(document.getIndexId()), null));
					    			AdjustHTMLFile.adjustContent(docWithContext);
			    				}

			    			} catch (Exception e) {
								e.printStackTrace();
							}
	    				}
		    			allDocumentsWithContext.add(docWithContext);
	    			}
				}
	    	
	    	try {
	    		if(true/*search.getCrtState(false).equalsIgnoreCase("CA")*/) {
	    			List<SimpleChapter> tsrIndexOrder = docManager.getTsrIndexOrder();
	    			final List<String> tsrIndexOrderDocIds = new ArrayList<String>();
	    			for (SimpleChapter simpleChapter : tsrIndexOrder) {
	    				tsrIndexOrderDocIds.add(simpleChapter.getDocumentId());
					}
	    			
		    		Collections.sort(allDocumentsWithContext,
		    				new Comparator<DocumentWithContent>() {
		    					@Override
		    					public int compare(DocumentWithContent o1, DocumentWithContent o2) {
		    						return DocumentUtils.getTsrIndexOrderComparator(tsrIndexOrderDocIds).compare(o1.getDocument(), o2.getDocument());	    						 
		    					}
		    				} 
		            );
	    		}
	    	}
	    	catch(Exception notCritical) { notCritical.printStackTrace(); }
	    		
	    	} catch (Exception e) {
				e.printStackTrace();
			} finally {
				docManager.releaseAccess();
			}
    	}
    	        
    	return allDocumentsWithContext;
    }
    
    private String cleanEmail(String email){
    	if(StringUtils.isEmpty(email)){
    		return null;
    	}
    	email = email.trim();
    	if("N/A".equalsIgnoreCase(email)){
    		return null;
    	}
    	return email;
    }
    
    public enum NotificationType {AGENT_FIRST, AGENT_SECOND, USUAL }
    
    private boolean sendNotificationEmail(
            String displayFileName, 
			String contextPath, 
			String TSRFileName, 
			String sCOMPDFFile, 
			HashMap attachedFiles,
			Search search,
			boolean hasPolicyFiles,
			String additionalText,
			NotificationType type,
			String searchLogFileName, HashMap<String,String> newTemplates) {
    	
    	// no need to send separate emails when not in single seat
        if(!getCurrentAgent().isSINGLE_SEAT() && type != NotificationType.USUAL){
        	return true;
        }    	

        boolean sendAttachment = true;
        if (this.getCurrentAgent() != null)
            sendAttachment = this.getCurrentAgent().hasDistributionMode(UserAttributes.ATTACH_MODE);

        if (displayFileName == null || displayFileName.trim().equals("")) {
            //will rename file to user's permanent directory below:
            displayFileName = search.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO);
            if (displayFileName.trim().equals(""))
                displayFileName = "UnknownFileNo";
            //append date/time to file name
            displayFileName += "_" + sdf.format(new Date()) + ".pdf";
        }

        if (this.getCurrentAgent() == null) {
            displayFileName = displayFileName.replaceAll("\\.pdf", ".tif");
        } else {
        	String distribExt = (Integer.parseInt(
        			this.getCurrentAgent().
					getDISTRIBUTION_TYPE()) != 
						Integer.parseInt(UserAttributes.TIFF_TYPE) 
						? ".pdf":".tif");
        	displayFileName = displayFileName.replaceAll("\\.pdf", distribExt);
        }

        try {

//            String pdfFile = contextPath
//                    + FileUtils.ReplaceAllFileSeparators(TSRFileName.substring(TSRFileName.indexOf(File.separator + "TSD"
//                            + File.separator)), "/");

            String displayCOMPDFFile = null;
            String pdfCOMFile = null;
            if (sCOMPDFFile != null) {
                displayCOMPDFFile = sCOMPDFFile;
                int pos = displayCOMPDFFile.lastIndexOf(File.separator);
                if (pos >= 0) {
                    displayCOMPDFFile = displayCOMPDFFile.substring(pos + File.separator.length());
                }
                pos = sCOMPDFFile.indexOf(File.separator + "TSD" + File.separator);
                if (pos >= 0)
                    pdfCOMFile = contextPath + FileUtils.ReplaceAllFileSeparators(sCOMPDFFile.substring(pos), "/");
                logger.info("*****************************>>>> " + sCOMPDFFile + " " + pdfCOMFile + " on SearchId " + searchId);
            }

            //send email with links included
            String appUrl = lbURL+":"+ lbPort;
            String txtMessageText = "<HTML><HEAD><TITLE>Title Search Results</TITLE></HEAD><BODY>\r\n";
            txtMessageText += "<TABLE>\r\n";
            txtMessageText += "<TR><TD COLSPAN=\"2\"><A name=\"goToTop\">&nbsp;</A>&nbsp;</TD></TR>\r\n";
            txtMessageText += "<TR><TD WIDTH=\"10\">&nbsp;</TD>\r\n";
            if(type == NotificationType.AGENT_FIRST){
            	txtMessageText += "<TD><B>Your Title Search Report is being generated. A separate email will be sent to you when it is available for download/view/save/print.";
            } else {
            	txtMessageText += "<TD><B>Your Title Search Report is available for you to download/view/save/print.";
            }
            txtMessageText += "</B></TD></TR>\r\n";
            txtMessageText += "<TR><TD COLSPAN=\"2\">&nbsp;</TD></TR>\r\n";
            if(type != NotificationType.AGENT_FIRST){
	            txtMessageText += "<TR><TD WIDTH=\"10\">&nbsp;</TD>\r\n";
	            txtMessageText += "<TD><A href=\"" + search.getTsrLink() + "\" target=\"_blank\"><B>" + displayFileName + "</B></A></TD></TR>\r\n";
	            txtMessageText += "<TR><TD COLSPAN=\"2\">&nbsp;</TD></TR>\r\n";
	            
	            txtMessageText += "<TR><TD WIDTH=\"10\">&nbsp;</TD>\r\n";
	            txtMessageText += "<TD><A href=\"" + search.getAllImagesSsfLink() + "\" target=\"_blank\"><B> All images </B></A></TD></TR>\r\n";
	            txtMessageText += "<TR><TD COLSPAN=\"2\">&nbsp;</TD></TR>\r\n";
            }
            if(type == NotificationType.AGENT_SECOND){
            	txtMessageText += "<TR><TD WIDTH=\"10\">&nbsp;</TD><TD>The Policy Documents were sent in a previous email.</TD></TR>\r\n";
            }

            UserAttributes agent = this.getCurrentAgent();
            boolean hasPolicy = false;
            hasPolicy = (agent != null)
                    && (agent.hasDistributionMode(UserAttributes.EOL_POLICY_MODE)
                            || agent.hasDistributionMode(UserAttributes.OWNERS_POLICY_MODE)
                            || agent.hasDistributionMode(UserAttributes.SHORT_POLICY_MODE) || agent
                            .hasDistributionMode(UserAttributes.FCOT_MODE));
            
           
                try {
                    hasPolicy = UserUtils.hasPolicy(agent,search.getProductId());
                } catch (Exception e) {
                    hasPolicy = false;
                }
           
            if (sCOMPDFFile != null || hasPolicy) {
                //Commitment part of the email notification
                txtMessageText += "<TR><TD COLSPAN=\"2\"><hr size=\"2\"></TD></TR>\r\n";
                txtMessageText += "<TR><TD COLSPAN=\"2\">&nbsp;</TD></TR>\r\n";
                txtMessageText += "<TR><TD WIDTH=\"10\">&nbsp;</TD>\r\n";
                txtMessageText += "<TD><B>Included in the \"Title Search Report\" service is data converted from the \"Title Search Report\" and from order entry information you provided for the purpose of ";
                txtMessageText += "assisting in completing the Commitment/Policy/Title Documents.</B></TD></TR>\r\n";
                txtMessageText += "<TR><TD WIDTH=\"10\">&nbsp;</TD>\r\n";
                txtMessageText += "<TD><hr size=\"1\"></TD></TR>\r\n";
                txtMessageText += "<TR><TD WIDTH=\"10\">&nbsp;</TD>\r\n";
                txtMessageText += "<TD>This conversion is intended to be used to facilitate the electronic transmission of certain standard information, ";
                txtMessageText += "provided in the order entry information and title search process, into a draft form of the Commitment/Policy/Title Documents.</TD></TR>\r\n";
                txtMessageText += "<TR><TD COLSPAN=\"2\">&nbsp;</TD></TR>\r\n";
                txtMessageText += "<TR><TD WIDTH=\"10\">&nbsp;</TD>\r\n";
                txtMessageText += "<TD><B>The conversion is to be used <U>solely</U> as a convenience and does not modify, alter or amend your responsibility as an agent/attorney for ";
                txtMessageText += ca.getDESCRIPTION()
                        + " (\"the Company\") to examine the \"Title Search Report\" in accordance with the terms ";
                txtMessageText += "and conditions set forth in the Issuing Agency Agreement.";
                txtMessageText += "<BR>You must also comply with all requirements of the Real Estate Settlement Procedures Act (\"RESPA\") and all regulations promulgated thereunder. ";
                txtMessageText += "Further, any matters relevant in determining insurability that become known to you must also be included in these Policy Documents.</B></TD></TR>\r\n";
                txtMessageText += "<TR><TD COLSPAN=\"2\">&nbsp;</TD></TR>\r\n";
                txtMessageText += "<TR><TD WIDTH=\"10\">&nbsp;</TD>\r\n";
                txtMessageText += "<TD>I acknowledge and accept the responsibility to examine the \"Title Search Report\" in accordance with the terms and conditions set forth in ";
                txtMessageText += "the Issuing Agency Agreement. I further acknowledge and agree that I am using this conversion package solely as a convenience in preparing these Policy Documents.</TD></TR>\r\n";
                txtMessageText += "<TR><TD WIDTH=\"10\">&nbsp;</TD>\r\n";
                txtMessageText += "<TD ALIGN=\"RIGHT\">";
            }

            // Do not include the templates in the second agent email
            if(type != NotificationType.AGENT_SECOND) {            
	            boolean templateNegenerat=false;
	            if (hasPolicy && hasPolicyFiles) {
	            	//int templateId = -1;
	            	
	            		Object[] tempaltesIds = attachedFiles.keySet().toArray();
	            		String eolPolicyFile = "", displayEolPolicyFile = "", out = "";
	            		int pos = 0;
	            		//HashMap currentTemplate = new HashMap();
	            		for (int i = 0; i < tempaltesIds.length; i++) {
	            			try {templateNegenerat=false;
	            				//templateId = Integer.parseInt(tempaltesIds[i].toString());
	            			
		            			//currentTemplate = UserUtils.getTemplate(templateId);
		            			//policyName = currentTemplate.get(UserUtils.TEMPLATE_NAME).toString();
		            			eolPolicyFile = contextPath
		                        		+ FileUtils.ReplaceAllFileSeparators(((String) attachedFiles.get(tempaltesIds[i].toString()))
		                                .substring(((String) attachedFiles.get(tempaltesIds[i].toString())).indexOf(File.separator + "TSD"
		                                        + File.separator)), "/");
		            			Matcher m=patternCod.matcher(eolPolicyFile);
		            			if(m.find()) templateNegenerat=true;
		            			displayEolPolicyFile = attachedFiles.get(tempaltesIds[i].toString()).toString();
		            			out = displayEolPolicyFile;
		            			
	            			} catch (NumberFormatException nfe) {
	            				//policyName = "XMLedATSDFields";
	            				String xmlTemplateId = "XMLFileds";
	            				eolPolicyFile = contextPath
		                    		+ FileUtils.ReplaceAllFileSeparators(((String) attachedFiles.get(xmlTemplateId))
		                            .substring(((String) attachedFiles.get(xmlTemplateId)).indexOf(File.separator + "TSD"
		                                    + File.separator)), "/");
	            				displayEolPolicyFile = attachedFiles.get(xmlTemplateId).toString();
	            			}
	            			if (displayEolPolicyFile != null) {
		            			pos = displayEolPolicyFile.lastIndexOf(File.separator);
			                    if (pos >= 0) {
			                        displayEolPolicyFile = displayEolPolicyFile.substring(pos + File.separator.length());
			                    }
			                    
			                    if (displayEolPolicyFile.matches("(?is).*TSDIn.*")){
			                    	String distribExt = ".tif";
		            				
			                    	if (this.getCurrentAgent() == null) {
		            					distribExt = ".tif";
		            		        } else {
		            		        	distribExt = (Integer.parseInt(
		            		        			this.getCurrentAgent().
		            							getDISTRIBUTION_TYPE()) != 
		            								Integer.parseInt(UserAttributes.TIFF_TYPE) 
		            								? ".pdf":".tif");
		            		        }
		            		        
			                    	if ((out != null) && !(displayEolPolicyFile.endsWith(".tif") || displayEolPolicyFile.endsWith(".pdf"))) {
		            		        	String outputfolder = out.substring(0, pos+1);
		            		        	String newFile = Util.convertToPDF(outputfolder, outputfolder + displayEolPolicyFile, 3, displayEolPolicyFile.replaceAll("\\.html?", ""));
			            		        	
		            		        	if (distribExt.equals(".tif")){
		            		        		newFile = Util.convertPDFToTIFF(newFile, "pass", outputfolder + displayEolPolicyFile.replaceAll("\\.html?", ".tif"));
			            		        }
		            		        	
		            		        	displayEolPolicyFile = displayEolPolicyFile.replaceAll("\\.html?", distribExt);
		            		        	eolPolicyFile = eolPolicyFile.replaceAll("\\.html?", distribExt);
		            		        	attachedFiles.remove(tempaltesIds[i]);
		            		        	attachedFiles.put(tempaltesIds[i], newFile);
		            		        }
		            			}
			                    
			                    // Task 7724
			    				String linkText = displayEolPolicyFile;
			    				try {
			    					linkText = (String) UserUtils.getTemplate(Integer.parseInt(tempaltesIds[i].toString())).get(UserUtils.TEMPLATE_NAME);
			    				} catch (NumberFormatException e) {
			    					e.printStackTrace();
			    				}
			                    
			                    if(templateNegenerat==false){
			                    	if(newTemplates==null||newTemplates.size()==0){
				                    	txtMessageText += "<A href=\""
			                            + appUrl
			                            + contextPath
			                            + URLMaping.PDF_SHOW
			                            + "?pdfFile="
			                            + eolPolicyFile
			                            + "&displayPdfFile="
			                            + displayEolPolicyFile
			                            + "&agentId="
			                            + search.getAgent().getID()
			                            + "\" target=\"_blank\"><B>I Accept. Download the " + linkText + "</B></A><br>";
			                    	}else{
			                    		txtMessageText += "<A href=\""
			                    			+ newTemplates.get(tempaltesIds[i].toString())
				                            + "\" target=\"_blank\"><B>I Accept. Download the " + linkText + "</B></A><br>";
			                    	}
		            			}
			                    else{
			            			txtMessageText += linkText +"<br>";
			            		}
	            			}
	            		}
	            }
	            
	            boolean sendSearchLogLink = true;
	            sendSearchLogLink = ((agent != null) && (agent.getMyAtsAttributes().getSearch_log_link()==1));
	            if (sendSearchLogLink) {
	            	if(!StringUtils.isEmpty(searchLogFileName)){
	                	File searchLogFile = new File(searchLogFileName);
	                	if(searchLogFile.exists()){
	                		String fileName = searchLogFile.getName();
	                		int idx = searchLogFileName.indexOf(File.separator + "TSD" + File.separator);
	                		if(idx != -1){
	                			String disFileName = searchLogFileName.substring(idx);
	    	            		disFileName = contextPath + FileUtils.ReplaceAllFileSeparators(disFileName, "/");
	    	            		txtMessageText += "</TD></TR>\r\n<TR><TD WIDTH=\"10\">&nbsp;</TD>\r\n";
	    	 	                txtMessageText += "<TR><TD COLSPAN=\"2\">&nbsp;</TD></TR>\r\n";
	    	 	                txtMessageText += "<TR><TD WIDTH=\"10\">&nbsp;</TD>\r\n";
	    	            		txtMessageText += "<TD><A href=\""
	    	                        + appUrl
	    	                        + contextPath
	    	                        + URLMaping.PDF_SHOW
	    	                        + "?pdfFile="
	    	                        + disFileName
	    	                        + "&displayPdfFile="
	    	                        + fileName
	    	                        + "&agentId="
	    	                        + search.getAgent().getID()
	    	                        + "\" target=\"_blank\"><B>I Accept. Download the " + fileName + "</B></A><br>";
	    	            		
	                		}
	                	}
	                }
	            }
	            
            }  //  type != NotificationType.AGENT_SECOND
            
            txtMessageText += "</TD>";
            if (sCOMPDFFile != null || hasPolicy) {
                txtMessageText += "</TR>\r\n";
                if(type != NotificationType.AGENT_SECOND){
	                txtMessageText += "<TR><TD WIDTH=\"10\">&nbsp;</TD>\r\n";
	                txtMessageText += "<TR><TD COLSPAN=\"2\">&nbsp;</TD></TR>\r\n";
	                txtMessageText += "<TR><TD WIDTH=\"10\">&nbsp;</TD>\r\n";
	                txtMessageText += "<TD>I am not interested in using the preliminary form of these Policy Documents provided with the \"Title Search Report\" Service.</TD></TR>\r\n";
	                txtMessageText += "<TR><TD WIDTH=\"10\">&nbsp;</TD>\r\n";
	                txtMessageText += "<TD ALIGN=\"RIGHT\"><A href=\"#goToTop\"><B>Not interested. Go to TOP.</B></A></TD></TR>\r\n";
                }
                txtMessageText += "<TR><TD COLSPAN=\"2\">&nbsp;If clicking the URL links in this message does not work, copy and paste them";
                txtMessageText += " into the address bar of your browser</TD></TR>\r\n";
                txtMessageText += "<TR><TD COLSPAN=\"2\">&nbsp;</TD></TR>\r\n";
                txtMessageText += "<TR><TD COLSPAN=\"2\"><hr size=\"2\"></TD></TR>\r\n";
                txtMessageText += "<TR><TD COLSPAN=\"2\">&nbsp;</TD></TR>\r\n";
                
            }
             
            // Add the content of Additional Information field from Search Page (CR #1428) 
            String addInfo = search.getSa().getAtribute(SearchAttributes.ADDITIONAL_INFORMATION);
            if (addInfo != null && addInfo.length() > 0){   							
            	addInfo = addInfo.replaceAll("\\r\\n?", "<BR>");
            	txtMessageText += "<TR><TD WIDTH=\"10\">&nbsp;</TD>\r\n";
            	txtMessageText += "<TD><B><FONT color=\"brown\">Additional Information</FONT></B></TD></TR>\r\n";
            	txtMessageText += "<TR><TD WIDTH=\"10\">&nbsp;</TD>\r\n";
            	txtMessageText += "<TD><FONT color=\"brown\">&nbsp;" + addInfo + "</FONT></TD></TR>\r\n";
            	txtMessageText += "<TR><TD COLSPAN=\"2\">&nbsp;</TD></TR>\r\n";
                txtMessageText += "<TR><TD COLSPAN=\"2\"><hr size=\"2\"></TD></TR>\r\n";
                txtMessageText += "<TR><TD COLSPAN=\"2\">&nbsp;</TD></TR>\r\n";	
            }
            
            //add links to PDF and TIFF viewers
            
            txtMessageText += "<TR><TD WIDTH=\"10\">&nbsp;</TD>\r\n";
        	txtMessageText += "<TD><B><FONT color=\"blue\">Links to PDF and TIFF viewers</FONT></B></TD></TR>\r\n";
        	txtMessageText += "<TR><TD WIDTH=\"10\">&nbsp;</TD>\r\n";
        	txtMessageText += "<TD><FONT color=\"blue\">&nbsp;<a href=\"http://www.alternatiff.com/\">TIFF Viewer</a></FONT></TD></TR>\r\n";
        	txtMessageText += "<TR><TD WIDTH=\"10\">&nbsp;</TD>\r\n";
        	txtMessageText += "<TD><FONT color=\"blue\">&nbsp;<a href=\"http://get.adobe.com/reader/\">PDF Viewer</a></FONT></TD></TR>\r\n";
        	txtMessageText += "<TR><TD COLSPAN=\"2\">&nbsp;</TD></TR>\r\n";
            txtMessageText += "<TR><TD COLSPAN=\"2\"><hr size=\"2\"></TD></TR>\r\n";
            txtMessageText += "<TR><TD COLSPAN=\"2\">&nbsp;</TD></TR>\r\n";	
            
           
            txtMessageText += "</TABLE>" + additionalText + "</BODY></HTML>\r\n";

            UserAttributes agentAttrib = this.getCurrentAgent();

            String orderByEmail = null, orderByEmailAlternate = null;
            if (agentAttrib != null) {
                orderByEmail = cleanEmail(agentAttrib.getEMAIL());
                orderByEmailAlternate = cleanEmail(agentAttrib.getALTEMAIL());
            }
            
            String abstractorEmail = cleanEmail(user.getUserAttributes().getEMAIL());
            String abstractorAlternateMail = cleanEmail(user.getUserAttributes().getALTEMAIL());
            
            //the CurrentCommunity
            CommunityAttributes ca = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity();
            UserAttributes commAdmin = UserUtils.getUserFromId(CommunityUtils.getCommunityAdministrator(ca));
            
            String commAdmMail = cleanEmail(commAdmin.getEMAIL());
            String commAdmAlternateMail = cleanEmail(commAdmin.getALTEMAIL());

            String agentDetails = (agent.getFIRSTNAME().equals("")?"":agent.getFIRSTNAME()) + " "
								+ (agent.getMIDDLENAME().equals("")?"":(agent.getMIDDLENAME().substring(0,1)+ "."))
								+ " " + (agent.getLASTNAME().equals("")?"":agent.getLASTNAME())
								+ ( ( agent.getCOMPANY().equals("") || agent.getCOMPANY().length() < 3 ) ? 
										agent.getCOMPANY() :
											(" (" + agent.getCOMPANY().substring(0,3) + ")"));
								
            String mailSubject = displayFileName + "  " + agentDetails;
            
            if(type == NotificationType.AGENT_FIRST){
            	mailSubject += " - policies only";
            } else if (type == NotificationType.AGENT_SECOND){
            	mailSubject += " - tsr only";
            }
            
            String emailSentTo = appFrom;
            
            boolean userDeliveryNotification = false;
            if (user != null){
            	if  (user.getUserAttributes().getMyAtsAttributes().getReceive_notification() == 1){
            		userDeliveryNotification = true;
            	}
            }

            boolean deliveryAgentNotification = true;
            deliveryAgentNotification = ((agent != null) && (agent.getMyAtsAttributes().getReceive_notification()==1));

	            try {
	            	if(type != NotificationType.USUAL){
	                	// send ot to agent
	                	if (orderByEmail != null && abstractorEmail != null) {
	                		if(deliveryAgentNotification) {
		                		boolean attachTsr = type == NotificationType.AGENT_SECOND; // try to attach tsr only at the second try 
		                		sendMailMessage(abstractorEmail, orderByEmail, null, appBcc, mailSubject, txtMessageText, TSRFileName, sendAttachment, attachedFiles, true, attachTsr);
	                		}
	                	}            		
	            	} else {
		                if (orderByEmail != null && abstractorEmail != null) {
		                	
		                	// send it to agent only for non single seat
		                	if(!getCurrentAgent().isSINGLE_SEAT() && deliveryAgentNotification) {
		                		sendMailMessage(abstractorEmail, orderByEmail, null, appBcc, mailSubject, txtMessageText, TSRFileName, sendAttachment, attachedFiles, deliveryAgentNotification);
		                	}
		                    emailSentTo = orderByEmail;
		                    
		                    if (userDeliveryNotification){
			                    sendMailMessage(abstractorEmail, abstractorEmail, null, appBcc, mailSubject, txtMessageText, TSRFileName, false, attachedFiles, false);
			                    if (orderByEmailAlternate != null) {                    	
				                    sendMailMessage(abstractorEmail, orderByEmailAlternate, null, appBcc, mailSubject, txtMessageText, TSRFileName, true, attachedFiles, deliveryAgentNotification);
			                    }                    
			                    if (abstractorAlternateMail != null) {                    	
				                    sendMailMessage(abstractorEmail, abstractorAlternateMail, null, appBcc, mailSubject, txtMessageText, TSRFileName, true, attachedFiles, false);
			                    }
			                }
		                } else {
		                    if (abstractorEmail != null) {
		                    	if (userDeliveryNotification){
			                        sendMailMessage(appFrom, abstractorEmail, abstractorAlternateMail, appBcc, mailSubject, txtMessageText, TSRFileName, true, attachedFiles, true);
			                        emailSentTo = abstractorEmail;
		                    	}
		                    } else {
		                        if (orderByEmail != null) {
		                            // this case should never happen
		                            sendMailMessage(appFrom, orderByEmail, null, appBcc, mailSubject, txtMessageText, TSRFileName,  sendAttachment, attachedFiles, deliveryAgentNotification);
		                            emailSentTo = orderByEmail;
		
		                        } else {
		                            //this case should never happen
		                            sendMailMessage(appFrom, appBcc, null, appBcc, mailSubject, txtMessageText, TSRFileName, sendAttachment, attachedFiles, true);
		                        }
		                    }
		                }
		                
		                boolean deliveryNotificationToCommAdmin = false;//B 4939
		                if (commAdmin != null){
		                	if  (commAdmin.getMyAtsAttributes().getReceive_notification() == 1){
		                		deliveryNotificationToCommAdmin = true;
		                	}
		                }
		                //send mail to community admin
		                if (commAdmMail != null && !commAdmMail.equals("") && !commAdmMail.equals("N/A")) {
		                	if (deliveryNotificationToCommAdmin){
		                		sendMailMessage(abstractorEmail, commAdmMail, null, appBcc, mailSubject, txtMessageText, TSRFileName, false, attachedFiles, false);
		                	}
		                }
		                
		                if (commAdmAlternateMail != null && !commAdmAlternateMail.equals("") && !commAdmAlternateMail.equals("N/A")) {
		                	if (deliveryNotificationToCommAdmin){
		                		sendMailMessage(abstractorEmail, commAdmAlternateMail, null, appBcc, mailSubject, txtMessageText, TSRFileName, true, attachedFiles, false);
		                	}
		                }
	            	}
	            } catch (Exception e0) {
	                e0.printStackTrace();
	                logger.error("Could not send mail notification on SearchId " +searchId, e0);
	
	                try {
	
	                    sendMailMessage(appFrom, appBcc + "," + MailConfig.getSupportEmailAddress(), null, appBcc, mailSubject, txtMessageText, TSRFileName, sendAttachment,  attachedFiles, true);
	
	                } catch (Exception e1) {
	                    e1.printStackTrace();
	                    logger.error("AGAIN! Could not send mail notification on SearchId " + searchId, e1);
	                }
	            }
            
	            // don't set any values for AGENT_FIRST
	            if(type != NotificationType.AGENT_FIRST){
	            		
		            notificationSendTo = emailSentTo;
		            notificationFile = displayFileName;
		            /*
		            forwardTo = URLMaping.CONVERT_TO_PDF_SHOW 
		            			+ "?pdfFile=" + pdfFile 
		            			+ "&displayPdfFile=" + displayFileName 
		            			+ "&emailSentTo=" + emailSentTo 
		            			+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.CONVERT_TO_PDF;
		            */
	            }   

        } catch (Exception e) {
            e.printStackTrace();
            
            return false;
        }

        return true;
    }

    private void sendMailMessage(String parFrom, String parTo, String parCc, String parBcc, String parSubject,
            String parMessageText, String TSRFileName, boolean withAttach, HashMap attachedFiles, boolean requestReceipt)
            throws AddressException, MessagingException {
    	sendMailMessage(parFrom, parTo, parCc, parBcc, parSubject, parMessageText, TSRFileName, withAttach, attachedFiles, requestReceipt, true);
    }

    
    private void sendMailMessage(String parFrom, String parTo, String parCc, String parBcc, String parSubject,
            String parMessageText, String TSRFileName, boolean withAttach, HashMap attachedFiles, boolean requestReceipt, final boolean attachTsr)
            throws AddressException, MessagingException {

    	Properties props = System.getProperties();
        props.put("mail.smtp.host", mailHost);
        
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage msg = new MimeMessage(session);
                
        // set From
        InternetAddress[] from = null;
    	try {
    		from = InternetAddress.parse(parFrom);
    	} catch (Exception ignored) {}
    	if (from == null || from.length == 0) {
    		from = new InternetAddress[1];
    		from[0] = new InternetAddress(MailConfig.getMailFrom());
    	}
    	msg.setFrom(from[0]);

    	// set Subject
        if (parSubject == null || "".equals(parSubject))
        	parSubject = "No Subject";
        msg.setSubject(parSubject);
        
        // set To
        msg.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(parTo));

        // set Cc
        if (parCc != null) {
            msg.setRecipients(javax.mail.Message.RecipientType.CC, InternetAddress.parse(parCc));
        }

        // set Bcc
        if (parBcc != null) {
            msg.setRecipients(javax.mail.Message.RecipientType.BCC, InternetAddress.parse(parBcc));
        }

        // requestReceipt
//        if (requestReceipt) {
//            String emailAddress = MailConfig.getMailPop3Address();
//            msg.setHeader("Disposition-Notification-To", emailAddress);
//        }
        
        logger.debug("Send mail message from " + from[0] + " to " + parTo + "; cc " + parCc + "; bcc " + parBcc + "; subject " + parSubject + "; SearchId " + searchId);

        if (!withAttach) {
            msg.setContent(parMessageText, "text/html");
            Transport.send(msg);
        } else {
            Multipart multipart = new MimeMultipart();
            BodyPart msgBodyPart = new MimeBodyPart();
            msgBodyPart.setContent(parMessageText, "text/html");
            multipart.addBodyPart(msgBodyPart);
            msg.setContent(multipart);
            //BodyPart attachPart = new MimeBodyPart();
            final MimeMessage msgf = msg;
            final String fileAttached = TSRFileName;
            final HashMap attachedHash = attachedFiles;
            Thread t = null;
            t = new Thread(new Runnable() {
                public void run() {
                    try {
                    	if(attachTsr){
                    		attachFile(fileAttached, msgf);
                    	}
                        if (attachedHash != null) {
                        	for (Object value : attachedHash.values()) {
                        		if (value != null /* && !value.toString().endsWith(".doc") */ )
	                                attachFile((String) value, msgf);
							}
                        }
                        Transport.send(msgf);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            if (t != null)
                t.start();
            try {
				t.join(1000 * 900);
			} catch (InterruptedException e) {
				logger.error("Error while waiting to send attachments on SearchId " + searchId, e);
			}
        }
    }

    private void attachFile(String pFileName, MimeMessage msg) throws MessagingException, IOException {

        BodyPart msgBodyPart;
        // attach the file to the message
        msgBodyPart = new MimeBodyPart();
        FileDataSource fds = new FileDataSource(pFileName);
        msgBodyPart.setDataHandler(new DataHandler(fds));
        msgBodyPart.setFileName(fds.getName().replaceAll(".tiff",".tif"));

        ((Multipart) msg.getContent()).addBodyPart(msgBodyPart);
    }
	/**
	 * @return Returns the currentAgent.
	 */
	public UserAttributes getCurrentAgent() {
		return currentAgent;
	}
	/**
	 * @param currentAgent The currentAgent to set.
	 */
	public void setCurrentAgent(UserAttributes currentAgent) {
		this.currentAgent = currentAgent;
	}
	
	/**
	 * Upload info on L drive
	 * @param tsrFileNames
	 * @return
	 */
	private String uploadFilesLdrive(Vector<DocumentWithContent> allDocumentsWithContext){
		
		boolean doUpload = false;
		
		String state="";
		String county ="";
		
		// check if we're on ILCook
		try{
			state = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
			county = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
			doUpload = "IL".equalsIgnoreCase(state);
		} catch(RuntimeException e) {
			doUpload = false;
			logger.error("Cannot find crt server on SearchId " + searchId, e);
		}
		if(!doUpload){
			return "";
		}
		
		// create the message
		String message;
		try{
			message = uploadFilesLdriveInternal(allDocumentsWithContext,county);
		}catch(RuntimeException e){
			message = "<font color=\"red\">Error occured. Upload not performed!</font>";
		}
		return "<br/><b><u>L Drive Upload Status</u>:</b> " + message; 
	}
    
	/**
	 * Upload info on L drive
	 * @param county 
	 * @param tsrFileNames
	 * @return
	 */
	private String uploadFilesLdriveInternal(Vector<DocumentWithContent> allDocumentsWithContext, String county){

		// obtain connection params to L drive
		// no connection params means the data upload is not enabled on this server
		String server, username, password;
		try{
			server = ServerConfig.getILCookLdriveServer(); 
			username = ServerConfig.getILCookLdriveUsername(); 
			password = ServerConfig.getILCookLdrivePassword();
		}catch(Exception e){
			logger.warn("Upload to L drive not enabled on this server! on SearchId " + searchId);
			return "<font color=\"blue\">WARNING:Upload to L drive not enabled on this ATS server!</font><br/>";
		}
		
		// obtain the folder on L drive where to upload docs
		String abstrFilename = global.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO); // e.g. AFWILG~543125
		String destFolder = abstrFilename.replaceFirst("[^~]*~", "").replaceAll("\\s+", "");
		if(StringUtils.isEmpty(destFolder)){		
			logger.error("Invalid ABSTRACTOR filename: " + abstrFilename + ". Gave up uploading! on SearchId " + searchId);
			return "<font color=\"red\">WARNING: Files not uploaded! Abstractor filename '" + abstrFilename + "' does not qualify for uploading to L drive. " +
			       "Use either 'something~number' or 'something' as abstractor file name. </font></br>";
		}
		// add all image files to upload list
		List <FTPFileStruct> uploadFiles = new ArrayList<FTPFileStruct>();
		for(DocumentWithContent	docWithContext : allDocumentsWithContext){
			if(docWithContext.getDocument().hasImage()) {
				String fileName = docWithContext.getDocument().getImage().getFileName().toUpperCase();
				if(fileName.endsWith(".PDF") || fileName.endsWith(".TIFF") || fileName.endsWith(".TIF")) {
					String ext = fileName.substring(fileName.lastIndexOf('.')+1);
					if(FileUtils.existPath(docWithContext.getDocument().getImage().getPath())){
						File file1 = new File(docWithContext.getDocument().getImage().getPath());
						uploadFiles.add(new FTPFileStruct(file1, docWithContext.getDocument().prettyPrint()+"."+ext, null));
					}
				}
			}
			if("cook".equalsIgnoreCase(county) && docWithContext.getDocument() instanceof TaxDocumentI && "TU".equals(docWithContext.getDocument().getDataSource()) &&
					docWithContext.getIndexContent() != null) {
				uploadFiles.add(new FTPFileStruct(null,docWithContext.getDocument().getInstno() + "_tu.html", docWithContext.getIndexContent().getBytes()));
			}else if(docWithContext.getDocument() instanceof TaxDocumentI && docWithContext.getIndexContent() != null){
				uploadFiles.add(new FTPFileStruct(null,docWithContext.getDocument().getInstno() + "_tr.html", docWithContext.getIndexContent().getBytes()));
			}
		}
		
		// search and add TP3 log files to the upload	
		try {
			addDocFiles(global, uploadFiles);
		} catch (IOException e) {
			SearchLogger.info("<br> Error reading TP3 doc files <br>", searchId);
		}
		
		if("du page".equalsIgnoreCase(county)||"kane".equalsIgnoreCase(county)) {
			String tmpFolder = TSRFolder.substring(0, TSRFolder.lastIndexOf(File.separator));
            String file = TSRFolder.substring(TSRFolder.lastIndexOf(File.separator) + 1, TSRFolder.lastIndexOf("."));
            String tsrIndexFileName = tmpFolder + File.separator + file + ".tsr.html";
            File tsrIndexFile = new File(tsrIndexFileName);
            if(tsrIndexFile.exists()) {
            	uploadFiles.add(new FTPFileStruct(tsrIndexFile,tsrIndexFile.getName(), null));
            }
            
            // TSR distribution type
            ImageI.IType distributionType = ImageI.IType.TIFF;
            if (this.getCurrentAgent() != null) {
                distributionType = this.getCurrentAgent().getDistributionType();
            }
            String tsrFileName = tmpFolder + File.separator + file + "." + distributionType.toString().toLowerCase();
            File tsrFile = new File(tsrFileName);
            if(tsrFile.exists()) {
            	uploadFiles.add(new FTPFileStruct(tsrFile,tsrFile.getName(), null));
            }
            
		}
		/*
		// add the Taxes Unlimited files
		files = new File(global.getSearchDir() + "City Tax" + File.separator).listFiles(new FilenameFilter(){
	        public boolean accept(File file, String name) {
	            return name.endsWith("_tu.html");
	        }
	    });
		if (files != null)
			for(File file: files){
				uploadFiles.add(new FTPFileStruct(file,file.getName(), null));
			}
		*/
		
		// upload the files
		FtpClient ftp = new FtpClient();
		try{
			boolean success = false;
			for(int i=0; i<3 && !success; i++){
				success = ftp.connect(server, username, password);
			}
			if(!success){
				return "<font color=\"red\">ERROR: Could not connect to L drive to upload files on folder: '" + destFolder + "'!</font><br/>";
			}
			return ftp.uploadFiles(uploadFiles, destFolder);
		}finally{
			ftp.disconnect();
		}
	}
	
	private static void addSearchDocFileForIL(Search global, Set <String> fileNames) throws IOException{
		
		if(global==null||fileNames==null){
			return;
		}
		
		File[] files = new File(global.getSearchDir() + "Register" + File.separator).listFiles(new FilenameFilter(){
	        public boolean accept(File file, String name) {
	            return name.endsWith(".doc");
	        }
	    });		
		
		ArrayList<String> fileNamesTemp = new ArrayList<String>();
		if(files != null) {
			for(File file: files){
				fileNamesTemp.add(file.getCanonicalPath());
			}
		}
		Collections.sort(fileNamesTemp);
		fileNames.addAll(fileNamesTemp);
	}
	
	private static void addDocFiles(Search global, List <FTPFileStruct> uploadFiles) throws IOException{
		Set <String> fileNames = new HashSet<String>();
		addSearchDocFileForIL(global, fileNames);
		
		for(String name:fileNames){
			File file = new File(name);
			uploadFiles.add(new FTPFileStruct(file, file.getName(), null));
		}
	}
	
	private static final String ERRORS_TAG			= "@@ats_errors@@";
	private static final String STATUS_TAG 			= "@@ats_status@@";
	private static final String START_TABEL_TEMPLATE = "<tr> <td colspan=\"3\" class=\"trteen\" id=\"searchStatus\" align=\"left\">"+STATUS_TAG+"</td>"+
    												 "<td colspan=\"3\" class=\"trteen\" align=\"left\">" + ERRORS_TAG + "</td>"+ 					
    												 "<td colspan=\"3\" class=\"trteen\" align=\"left\">"+					
    												 "<table>"+
    												 "<tbody>"+ "<tr><td></td><td></td>";
	
	private static final String END_TABEL_TEMPLATE ="</tbody></table></td></tr>";
	
	private static final String getIndexForUploaded(Search search, String documentId){
		DocumentsManagerI docManager = search.getDocManager();
		String result = SimpleChapterUtils.FAKE_DOC_TEMPLATE_FOR_CREATE_TSR;
				
		try {
			docManager.getAccess();
			DocumentI doc = docManager.getDocument(documentId);
			
			if(doc instanceof RegisterDocumentI && doc.isFake()) {
				result = result.replaceFirst("@@DS@@", ((RegisterDocumentI)doc).getDataSource());
			}else {
				result = result.replaceFirst("@@DS@@", "UP");
			}
			
			result = result.replaceFirst("@@DESCRIPTION@@", doc.getDescription());
			String instrument = doc.getInstno();
			if (doc instanceof RegisterDocument) {
				RegisterDocument docReg = (RegisterDocument) doc;
				String recDateStr = SearchAttributes.DATE_FORMAT_MMddyyyy.format(docReg.getRecordedDate());
		    	if(docReg.getRecordedDate()==null){
		    		recDateStr="*";
		    	}
		    	if(docReg.getInstrumentDate()==null){
		    		recDateStr += "<br>*";
		    	} else {
		    		recDateStr += "<br>" + SearchAttributes.DATE_FORMAT_MMddyyyy.format(docReg.getInstrumentDate());
		    	}
		    	result = result.replaceFirst("@@DATE@@", recDateStr);
		    	result = result.replaceFirst("@@TYPE@@", doc.getDocType() + "<br>" + doc.getDocSubType());
		    	String book = "*";
		    	if(!StringUtils.isEmpty(docReg.getBook())) {
		    		book = docReg.getBook();
		    	}
		    	String page = "*";
		    	if(!StringUtils.isEmpty(doc.getPage())) {
		    		page = doc.getPage();
		    	}
		    	instrument += "<br>" + book + "*" + page;
		    		
			} else {
				if(doc.getYear() == -1) {
					result = result.replaceFirst("@@DATE@@", "*");
				} else {
					result = result.replaceFirst("@@DATE@@", String.valueOf(doc.getYear()));
				}
				result = result.replaceFirst("@@TYPE@@", doc.getDocType());
			}
			
			if(StringUtils.isEmpty(doc.getGrantorFreeForm())) {
				 result.replaceFirst("@@GRANTOR@@", "*");
			}
			if(StringUtils.isEmpty(doc.getGranteeFreeForm())) {
				 result.replaceFirst("@@GRANTEE@@", "*");
			}
			result = result.replaceFirst("@@GRANTOR@@", doc.getGrantorFreeForm());
			result = result.replaceFirst("@@GRANTEE@@", doc.getGranteeFreeForm());
			result = result.replaceFirst("@@INSTRUMENT@@", instrument);
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			docManager.releaseAccess();
		}
		return result;
	}
	
	public boolean isRemoveInvoicePage() {
		return removeInvoicePage;
	}

	public void setRemoveInvoicePage(boolean removeInvoicePage) {
		this.removeInvoicePage = removeInvoicePage;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GPThread [global=");
		builder.append(global);
		builder.append(", user=");
		builder.append(user);
		builder.append(", ca=");
		builder.append(ca);
		builder.append(", currentAgent=");
		builder.append(currentAgent);
		builder.append(", hasPolicyFiles=");
		builder.append(hasPolicyFiles);
		builder.append(", searchId=");
		builder.append(searchId);
		builder.append(", notifier=");
		builder.append(notifier);
		builder.append(", conversionFinished=");
		builder.append(conversionFinished);
		builder.append(", startTime=");
		builder.append(startTime);
		builder.append(", finishTime=");
		builder.append(finishTime);
		builder.append(", createTime=");
		builder.append(createTime);
		builder.append(", TSRFolder=");
		builder.append(TSRFolder);
		builder.append(", TSRFileName=");
		builder.append(TSRFileName);
		builder.append(", COMFileName=");
		builder.append(COMFileName);
		builder.append(", TSDFileName=");
		builder.append(TSDFileName);
		builder.append(", contextPath=");
		builder.append(contextPath);
		builder.append(", notificationSendTo=");
		builder.append(notificationSendTo);
		builder.append(", notificationFile=");
		builder.append(notificationFile);
		builder.append(", currentState=");
		builder.append(currentState);
		builder.append(", currentCounty=");
		builder.append(currentCounty);
		builder.append(", conversionError=");
		builder.append(conversionError);
		builder.append(", warningMessages=");
		builder.append(warningMessages);
		builder.append(", removeInvoicePage=");
		builder.append(removeInvoicePage);
		builder.append("]");
		return builder.toString();
	}

	public static void main(String[] args) throws IOException {	
	    
	    String content = org.apache.commons.io.FileUtils.readFileToString(new File("e:/doctype.txt"));
	    
	    Matcher mat = Pattern.compile(
	    		"(?i)<[ ]*SUBCATEGORY[ ]+NAME[ ]*=[ ]*\"([^\"]+)\"[ ]*>"
	    		).matcher(content);
	    
	    while (mat.find()){
	    	content = content.replace(mat.group(), "<SUBCATEGORY NAME=\""+UtilsAtsGwt.toTitleCase(mat.group(1))+"\">");
	    }
	    
	    org.apache.commons.io.FileUtils.writeStringToFile(new File("e:/doctype_res.txt"), content);
	}
}