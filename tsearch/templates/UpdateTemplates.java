package ro.cst.tsearch.templates;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.processExecutor.server.ServerExecutor;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servlet.DBFileView;
import ro.cst.tsearch.servlet.community.UploadPolicyDoc;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.CommunityTemplatesMapper;
import ro.cst.tsearch.database.rowmapper.CountyDefaultLegalMapper;
import ro.cst.tsearch.exceptions.BaseException;

public class UpdateTemplates {
	
//	public static Map<String, Set<Long>> MISSING_TAGS = new HashMap<String, Set<Long>>();
	
	private static 				FileOutputStream fileOut = null;
	private static Object 		accessToLog = new Object();
	
	static{
		File file =new File (ServerConfig.getTemplatesPath() + "/UpdateTemplates.log");
		if (file.exists()){
			file.renameTo(new File(ServerConfig.getTemplatesPath() + "/UpdateTemplates_bak.log"));
		}
		 
		try{
			fileOut = new FileOutputStream (ServerConfig.getTemplatesPath() + "/UpdateTemplates.log");
		}
		catch(Exception e){
			//e.printStackTrace();
			System.err.println("  --- UpdateTemplates:: Nu pot crea stream pe fisierul " + ServerConfig.getTemplatesPath() + "/UpdateTemplates.log");
			try{
				fileOut = new FileOutputStream ("UpdateTemplates.log");
			}
			catch(Exception e1){
				e1.printStackTrace(System.err); 
				fileOut=null;
			}
		}
	}
	
	public static void log(String mes){
		String date = ServerExecutor.getDate();
		synchronized (accessToLog){
			if(fileOut!=null){
				try{
					fileOut.write((date+"\t"+mes+"\n").getBytes());
				}
				catch(Exception e){
					e.printStackTrace(System.err);
				}
			}
		} 
	}
	
		public static final String FILE_NAME_EXPRESION="fileName=\".*?\"";
		public static final String POLICYID_EXPRESION="policyID=\".*?\"";
		public static final String EXPR_QUOTES="\".*\"";
		
		private static final Logger logger = Logger.getLogger(UpdateTemplates.class);
		
		public static  boolean updateTemplates(String templatesDir){
			return updateTemplates(templatesDir, null, null);
		}
		
		public static  boolean updateTemplates(String templatesDir, Long communityId, List<String> templatesToBeCompiled){
						
			/* Recompile legal description templates */
			if(communityId == null) {
				recompileDefaultLegalDescriptionTemplates();
				recompileBaseFileTemplate();
			}
				  
			   boolean isSucces = true;			   
			    logger.debug("TemplatesDir " + templatesDir);
			    log("******** ------------ TEMPLATES_DIR: " + templatesDir +"--------- *********\n");
			   
			    String tableName = DBConstants.TABLE_COMMUNITY_TEMPLATES;
//			    tableName = "ts_community_templates_for_ro4";
			    String where 		= DBConstants.FIELD_COMMUNITY_TEMPLATES_CONTENT + " is not null ";
				if(communityId!=null) {
					where += " AND " + DBConstants.FIELD_COMMUNITY_TEMPLATES_COMM_ID + " = " + communityId.longValue();
				}
				// select only specified templates to be compiled
				if(templatesToBeCompiled != null && !templatesToBeCompiled.isEmpty()) {
					where += " AND " + DBConstants.FIELD_COMMUNITY_TEMPLATES_NAME + " IN (";
					for(String template : templatesToBeCompiled) {
						where += "'" + template + "', ";
					}
					where = where.replaceFirst(", $", ""); // remove last comma
					where += ") ";
				}
				
//				where += " AND year(" + DBConstants.FIELD_COMMUNITY_TEMPLATES_LAST_UPDATE + ") = 2014 and comm_id not in (3,4,10,73) ";
			    String orderBy	= DBConstants.FIELD_COMMUNITY_TEMPLATES_COMM_ID +" DESC";
			    
			    Vector data   		= DBManager.fetchData(tableName, where, orderBy);
			    
			    HashMap map 	=			 null;
			    String	fileName;
			    int 	   fileExtPos;
			    String  fileExt;
			    String content;
			    int      policyId;
			    int 	   commId;
			    InputStream streamContent;
			    
//			    MISSING_TAGS.clear();
//			    
//			    CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(4452962);
//			    Search search = currentInstance.getCrtSearchContext();
//			    SearchAttributes sa = search.getSa();
//			    
//			    long userId = InstanceManager.getManager().getCurrentInstance(search.getID()).getCurrentUser().getID().longValue();
//			    
//			    State state = null;
//			    County county = null;
//				try {
//					state = State.getState(new BigDecimal(sa.getAtribute(SearchAttributes.P_STATE)));
//					county = County.getCounty(new BigDecimal(sa.getAtribute(SearchAttributes.P_COUNTY)));
//				} catch (BaseException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
//				CommunityAttributes ca = InstanceManager.getManager().getCurrentInstance(sa.getSearchId()).getCurrentCommunity();
//				
//				//on preview fill all codes for docs
//				TemplateUtils.fillDocumentBPCodes(search.getID(), userId);
//				
//				HashMap<String, Object> templateParams = TemplateBuilder.fillTemplateParameters(search, ca, county, state, false, false, null);
//			    
//				Map<Long, String> allTemplates = new HashMap<Long, String>();
				
			      for (int i=0;i<data.size();i++)
			      {
			    	   map 			  =  ((HashMap)data.get(i));
			    	   
			    	   fileName    = map.get(DBConstants.FIELD_COMMUNITY_TEMPLATES_FILENAME).toString();
			    	   fileExtPos	  = fileName.lastIndexOf(".");
			    	   fileExt 	      = fileName.substring(fileExtPos+1);
			    	   

			    	   
			    	   policyId	  = Integer.parseInt(map.get(DBConstants.FIELD_COMMUNITY_TEMPLATES_ID).toString());			    	   
			    	   commId	  = Integer.parseInt(map.get(DBConstants.FIELD_COMMUNITY_TEMPLATES_COMM_ID).toString());
			    	   
//			    	   allTemplates.put((long) policyId, fileName + "(comm_id " + commId + ")");
			    	   
			    	   //get filenames used with AddDoc
			    	   String currentDir				= templatesDir + File.separator +commId;
			    	   
			    	   logger.debug("---------- Fisier curent: <<<<" + currentDir+"/"+fileName+" >>>>");
			    	   log("---------- Fisier curent: <<<<" + currentDir+"/"+fileName+" >>>>");
			    	   
			    	   // if the specifed dir does not exists, creates it
			    	   File fileDir=new File(currentDir);
			    	    if (!fileDir.isDirectory())
			    	         fileDir.mkdir();
			    	   
			    	    String xPolicyTempFile				= UploadPolicyDoc.getGeneratedTemplateFileName(fileName, policyId, commId);			    	    			    	  
			    	    String xJavaFile							= UploadPolicyDoc.getJavaFile(commId, policyId);
						

				    try{	   
				      //retrieves file content
				    	DBFileView.setBlobFileInfo(policyId,tableName);
			    	   
			    	   //creates java,temp, and class files

				    	   try{				    		 				    		  
					    	   if( AddDocsTemplates.docDocumentsExtensions.get(fileExt)!=null ){
					    		   
					    		   
					    		   //creates a temporary file for doc file to store content
					    		   streamContent  	= 	DBFileView.getBlobContentStream();
					    		   byte []b 				= 	new byte[(int)streamContent.available()];
					    		   streamContent.read(b);
					    		   streamContent.close();
					    		   String tempFile = xPolicyTempFile.replaceAll("temp", "").replaceAll("lates", "templates").replaceAll("//","/");
						    		FileOutputStream out = new FileOutputStream (tempFile);
						    		out.write(b);
						    		out.close();
					    		   		
						    		OfficeDocumentContents doc = null;
						    		try {
						    			doc =new OfficeDocumentContents(tempFile);
						    			AddDocsTemplates.addTempFilesNew(doc , xPolicyTempFile, xJavaFile, new Long(policyId).intValue(),fileExt);
						    		}catch(Exception e) {
						    			e.printStackTrace();
						    		}
						    		finally {
						    			if(doc != null) {
						    				OfficeDocumentContents.closeOO(doc.getXComponent());
						    			}
						    		}
					    		   //AddDocsTemplates.addTempFilesNew(tempFile, xPolicyTempFile, xJavaFile, policyId, fileExt);
					    		   
									//removes the temporarly created file
									 new File (tempFile).delete();
					    		   
					    	   } else {
					    		   
					    		   	//get file content
						    	   content	= DBFileView.getBlobContentAsString();					    		   
					    		   AddDocsTemplates.addTempFilesNew(new StringBufferContents(content), xPolicyTempFile,xJavaFile, policyId, fileExt);
					    	   }
					    	   
//					    	   CommunityTemplatesMapper templateById = DBManager.getSimpleTemplate().queryForObject(
//										"SELECT * FROM " + tableName + " WHERE " + DBConstants.FIELD_COMMUNITY_TEMPLATES_ID + " = ?",
//										new CommunityTemplatesMapper(),
//										policyId);
//					    	   
//					    	   AddDocsTemplates.completeNewTemplatesV2New(
//										templateParams,
//										fileName,
//										templateById,
//										search,
//										true, null, null, new HashMap<String, String>(), false);
					    	   
				    	   }catch (TemplatesException te) {
				    		    te.printStackTrace(System.err);
				    		    logger.debug("----- Eroare la parsarea documentului <<<"+fileName+">>>");
								log(ExceptionUtils.getStackTrace(te));							
				    	   } catch (IOException io ){
				    		    io.printStackTrace();
				    	   } 
				    	}catch (Exception e) {				    		 
				    		 logger.debug("----Eroare neidentificata la fisierul <<< "+fileName+ " >>>");
				    		 e.printStackTrace();
				    	}
 
			      }	
			      
//			      StringBuilder result = new StringBuilder("Missing tags:\n");
//	for (String tag : MISSING_TAGS.keySet()) {
//		if(!tag.contains("BlocComponent")) {
//		result.append(tag + " in " + MISSING_TAGS.get(tag).size() + " files:\n");
//		for (Long templateErr : MISSING_TAGS.get(tag)) {
//			result.append("\t").append(allTemplates.get(templateErr)).append("\n");
//		}
//		result.append("\n");
//		}
//	}
//	result.append("-----------");
//	System.err.println(result);
			      
			return isSucces;
		}
		
		
		
		public static boolean recompileBaseFileTemplate(){
			String content = "";
			try {
				content = FileUtils.readFileToString(new File(TemplateUtils.BASE_FILE_TEMPLATE_PATH));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			Template template = new Template(AddDocsTemplates.BASE_FILE_TEMPLATE_ID, 
					TemplateUtils.BASE_FILE_TEMPLATE_NAME,
					TemplateUtils.BASE_FILE_TEMPLATE_NAME,
					Long.toString(new Date().getTime()),
					TemplateUtils.BASE_FILE_TEMPLATE_NAME,
					content,
					TemplateUtils.BASE_FILE_TEMPLATE_NAME,
					AddDocsTemplates.BASE_FILE_COMMID);
			template.compile();
			return true;
		}
		
		public static boolean recompileDefaultLegalDescriptionTemplates() {
			try {
			
			/* Delete unused templates */	
			int count = DBManager.deleteUnusedDefaultLdTemplates();
			logger.info(" Deleted "+count+" unused default LD templates from the database");
			
			/* Empty ld templates folder */
			String path = UploadPolicyDoc.getTemplatesPath(AddDocsTemplates.LEGAL_COMMID);
			File dir = new File(path); 
			for(String s : dir.list()) {
				File del = new File(dir.getPath(),s);
				if(del.isDirectory()) continue;
				del.delete();
			}
			
			/* Recompile the templates */
			List<CountyDefaultLegalMapper> list = DBManager.getDefaultLegalTemplates();
			
			for(CountyDefaultLegalMapper cdm : list ) {
				Template template = new Template(cdm.getTemplateId(), 
						AddDocsTemplates.LEGAL_TEMPLATE_NAME,
						AddDocsTemplates.LEGAL_TEMPLATE_NAME,
						Long.toString(new Date().getTime()),
						AddDocsTemplates.LEGAL_TEMPLATE_NAME,
						cdm.getDefaultLd(),
						AddDocsTemplates.LEGAL_TEMPLATE_NAME,
						AddDocsTemplates.LEGAL_COMMID);
					template.compile();
				
					Template templateCondo = new Template(cdm.getTemplateId(), 
						AddDocsTemplates.LEGAL_CONDO_TEMPLATE_NAME,
						AddDocsTemplates.LEGAL_CONDO_TEMPLATE_NAME,
						Long.toString(new Date().getTime()),
						AddDocsTemplates.LEGAL_CONDO_TEMPLATE_NAME,
						cdm.getDefaultLdCondo(),
						AddDocsTemplates.LEGAL_CONDO_TEMPLATE_NAME,
						AddDocsTemplates.LEGAL_COMMID);
					templateCondo.compile();	
				}
			}catch(Exception e) {
				e.printStackTrace();
				return false;	
			}
			return true;
		}
}
