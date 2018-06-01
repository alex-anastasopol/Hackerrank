/*
 * Created on Oct 12, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ro.cst.tsearch.servlet.community;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.data.DataException;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.database.rowmapper.CommunityTemplatesMapper;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.exceptions.UploadPolicyDocException;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.DistributedMutex;
import ro.cst.tsearch.templates.AddDocsTemplates;
import ro.cst.tsearch.templates.OfficeDocumentContents;
import ro.cst.tsearch.templates.StringBufferContents;
import ro.cst.tsearch.templates.TemplatesException;
import ro.cst.tsearch.templates.edit.client.TemplateUtils;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.MultipartParameterParser;
import ro.cst.tsearch.utils.ParameterNotFoundException;
import ro.cst.tsearch.utils.StrUtil;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.utils.ZipUtils;
import de.schlichtherle.util.zip.ZipOutputStream;



/**
 * @author george
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class UploadPolicyDoc extends BaseServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected static final Category logger= Category.getInstance(UploadPolicyDoc.class.getName());
	
	// XML constants
	public static final String X_POLICY_FILE = "fileName";
	public static final String X_POLICY_NAME = "policyName";
	public static final String X_POLICY_SHORT_NAME = "shortPolicyName";
	public static final String X_POLICY_ID = "policyID";
	public static final String X_POLICY_TEMP_FILE = "policyTempFile";
	public static final String X_POLICY_CLASS_FILE = "policyClassFile";
	
	public static final int X_NO_ACTION = -1;
	public static final int X_ADD_DOC = 1;
	public static final int X_REMOVE_DOC = 2;
	public static final int X_EDIT_DOC = 3;
	
	public static final String fSep = File.separator;
	public static final String RET_VAR 					= "ret";
	public static final String RET_MSG 					= "msg";
	public static final String FILE_NAME 				= "fileName";
	public static final String FILE_POLICY_NAME	= "policyName";
	public static final String FILE_SHORT_NAME    = "shortFileName";
	public static final String FILE_COMM_ID 					= "commId";
	public static final String FILE_POLICY_ID					= "policyId";
	
	public static final int COMMUNITY_NEW = 1;
	public static final int COMMUNITY_EXISTS = 2;
	
	public static final int OUT_OPTIONS_HTML = 1;
	public static final int OUT_CLEAR_TEXT_HTML = 2;
	
	public static final String TEMP_SUFIX = "_temp";
	
	private static final String SQL_GET_TEMPLATES_FROM_COMM = "SELECT * FROM " + 
			DBConstants.TABLE_COMMUNITY_TEMPLATES + " WHERE " + 
			DBConstants.FIELD_COMMUNITY_TEMPLATES_COMM_ID + "= ? AND " + 
			DBConstants.FIELD_COMMUNITY_TEMPLATES_CONTENT + " is not NULL";
	
	/*
	 * ==== NOT TRUE ====
	 * this dispatcherization is taken by TSOPcode
	 * ==== /NOT TRUE ==== 
	 */ 
	// actions that are taken. This dispatcher is used for knowing where templates should be found:
	// in TMP directory or TEMPLATES directory
	public static final String COMM_ACTION = "COMM_ACTION";
	
	public static final int COMM_ADD_ACTION    = 1;
	public static final int COMM_EDIT_ACTION   = 2;
	public static final int COMM_VIEW_ACTION   = 3;
	public static final int COMM_SAVE_ACTION   = 4;
	public static final int COMM_DELETE_ACTION = 5;
	
	public static final String RMV_FILE_VAR = "rmvFile";
	public static final String EDIT_POLICY_NAME_VAR = "policyName";
	public static final String EDIT_POLICY_SHORT_NAME_VAR = "shortPolicyName";
	public static final String EDIT_FILE_VAR = "editFileVar";
	public static final String EDIT_POLICY_ID_VAR = "editPolicyId";
	
			
	public static final String UPLOAD_OK = "Your file was succesfully uploaded!";
	public static final String UPLOAD_BAD = "A problem occurs while trying to apload your file";
	public static final String REMOVE_OK = "Your file was succesfully removed from server.";
	public static final String UPDATE_DATE = "Your data was succesfully updated.";
	public static final String RET_OK			 = "ok";
	public static final String RET_BAD		 = "bad";
	
	public static final String SESSION_ID = "sessionId";
	public static final String POLICY_FILE = "policyFile";
	public static final String NAME_OF_POLICY = "nameOfPolicy";
	public static final String SHORT_NAME_POL = "shortNameOfPol";
	public static final String CURRENT_POLICY_NAME = "policyName";
	public static final String CURRENT_POLICY_FILE = "currentPolicyFile";
	
	
	public static final String TEMPL_META_TILE = "tempMeta.xml";
	public static final String TEMPL_DTD_FILE = "templates.dtd";
	
	public static final String ERR_TEXT_1 = "The file you are trying to upload doesn't appear to be a doc file!";
	public static final String ERR_TEXT_2 = "A file with the name of file that you are trying to upload already exists in your template directory";
	public static final String ERR_TEXT_3 = "We could'n delete the file you requested to.";
	public static final String ERR_TEXT_4 = "The file couldn't be found on server!";
	public static final String ERR_TEXT_5 = "The templates directory couldn't be created!";
	public static final String ERR_TEXT_6 = "The community directory already exists!";
	public static final String ERR_TEXT_7 = "We couldn't find your file in our temporary directory. Please upload it again!";
	public static final String ERR_TEXT_8 = "The copy process failed!";
	public static final String ERR_TEXT_9 = "For the file that you are trying to upload, you must specify a Policy name!";
	public static final String ERR_TEXT_10 = "The file you are trying to upload is empty or doesn't exist";
	public static final String TMP_TEMPLATES_WEB_PATH = URLMaping.path + fSep + "tmp";
	public static final String FILES_PATH = BaseServlet.FILES_PATH;
	public static final String TMP_TEMPLATES = FILES_PATH + fSep + "tmp"; 
	public static final String FINAL_TEMPLATES = FILES_PATH + fSep + "templates";
	//public static final String TMP_TEMPLATES_DTD_PATH = BaseServlet.REAL_PATH + fSep + "resources" + fSep + "XML";
	public static final String TEMPLATES_WEB_PATH = URLMaping.path + fSep + "templates";
	public static final String TEMPLATES_PATH = FILES_PATH + fSep + "templates"; 
	
	
	private static boolean deletedTempl = false;
		
	public void doRequest(HttpServletRequest request, HttpServletResponse response) 
			throws IOException, ServletException{
	
		//HttpSession session = request.getSession();
		int opCode = 0;
		if (request.getParameter(TSOpCode.OPCODE) != null) {
			opCode = Integer.parseInt(request.getParameter(TSOpCode.OPCODE));
			if (opCode == TSOpCode.DWNL_ALL_TEMPLATES) {
	        	downloadAllFiles(request, response);
			}
        	
		} else {
		    MultipartParameterParser mpp = new  MultipartParameterParser(request);
		    
	        opCode = mpp.getMultipartIntParameter(TSOpCode.OPCODE);
	        
	        int commId =  mpp.getMultipartIntParameter(CommunityAttributes.COMMUNITY_ID);
	        
	        String sessionId = request.getSession().getId()/*mpp.getMultipartStringParameter(SESSION_ID)*/;
	                    
	        File tempDir = null;
	        if (commId == -1) {
		        if ( !(new File(TMP_TEMPLATES)).exists() ) {
		        	(new File(TMP_TEMPLATES)).mkdir(); 
		        }
		        tempDir = new File(TMP_TEMPLATES + fSep + sessionId);
	        } else {
	        	if ( !(new File(FINAL_TEMPLATES)).exists() ) {
		        	(new File(FINAL_TEMPLATES)).mkdir(); 
		        }
		        tempDir = new File(FINAL_TEMPLATES + fSep + commId);
	        }
	
	        StringBuilder returnStatus = new StringBuilder();
	    	StringBuilder returnMessage = new StringBuilder();
	    	
	        switch(opCode) {        
	        case TSOpCode.COMM_UPLOAD_POLICY:
	        	try {
	        		String policyName				 = 	mpp.getMultipartStringParameter(NAME_OF_POLICY);
	        		String shortName 				 =    mpp.getMultipartStringParameter(SHORT_NAME_POL);
	                File fileToUpload 				 =	mpp.getFileParameter(POLICY_FILE);
	                FileInputStream	fis			 =    new FileInputStream(fileToUpload);
	                String fileName					 =    fileToUpload.getName();                                
	                long policyId						 = 	-1;
	                /**
	                 * Uploads file to database
	                 */                
	               //redirect page 
	        		String encondingType = "UTF-8";
	        		String forwardQueryError = "?" + RET_VAR + "="+RET_BAD
					 + "&" + RET_MSG + "=";
	        		if (UserUtils.isTemplatePolicyFileNameAlreadySaved(commId, fileName)) {
	    				forwardQueryError += URLEncoder.encode("Policy File Name already exists!",encondingType);
	    				forward(request, response, URLMaping.UPLOAD_POLICY + forwardQueryError);
	    			} else {
	    				policyId    = uploadFile(policyId, policyName, shortName, fileName,fis,commId, tempDir, returnStatus, returnMessage);
	    				String forwardQuery = null;
	            		try {
	            			
	            			forwardQuery = "?" + RET_VAR + "="+returnStatus
	            									+ "&" + RET_MSG + "=" + URLEncoder.encode(returnMessage.toString(),encondingType)
	            									+ "&" + FILE_NAME + "=" + URLEncoder.encode(fileToUpload.getName(),encondingType)
	            									+ "&" + FILE_POLICY_NAME + "=" + URLEncoder.encode(policyName,encondingType)
	            									+ "&" + FILE_SHORT_NAME + "=" + URLEncoder.encode(shortName,encondingType)
	            									+ "&" + FILE_POLICY_ID + "=" 
	            									+ policyId;
	            			
	            		} catch (UnsupportedEncodingException e) {
	            			e.printStackTrace();
	            		}
	            		try {
	            			forward(request, response, URLMaping.UPLOAD_POLICY + forwardQuery);
	            		} catch (ServletException e1) {
	            			e1.printStackTrace();
	            		} catch (IOException e2) {
	            			e2.printStackTrace();
	            		}
	    			}
	        	 } catch (UploadPolicyDocException upde) {                                        	 
	        		            upde.printStackTrace();
	        		            forward(request, response, URLMaping.UPLOAD_POLICY + formatExceptionQuery(upde));
	        	}            
	        break;
	        
//	        case TSOpCode.DWNL_ALL_TEMPLATES:
//	        	downloadAllFiles(request, response);
//	//        	downloadAllFiles(mpp.getMultipartIntParameter(CommunityAttributes.COMMUNITY_ID), request, response);
//	//        	downloadAllFiles(request);
//	        	break;
	        
	        case TSOpCode.COMM_REMOVE_POLICY:
	        	try {
	        		
	        		int policyId = mpp.getMultipartIntParameter(FILE_POLICY_ID);        		        		
	        		/**
	        		 * Removes selected file
	        		 */
	        		removeFile(tempDir, policyId);
	        		
	        		String encondingType = "UTF-8";
	        		String forwardQuery = "?" + RET_VAR + "="+RET_OK
							+ "&" + RET_MSG + "=" + URLEncoder.encode(REMOVE_OK,encondingType)
	        				+ "&" + TSOpCode.OPCODE + "=" + TSOpCode.COMM_REMOVE_POLICY
	        				+ "&" + FILE_POLICY_ID	+ "=" + policyId
	        			    + "&" + CommunityAttributes.COMMUNITY_ID + "=" + commId;
	
	        		forward(request, response, URLMaping.UPLOAD_POLICY + forwardQuery);
	        		
	        	} catch (UploadPolicyDocException upde) {
	        		forward(request, response, URLMaping.UPLOAD_POLICY + formatExceptionQuery(upde));
	        	}
	        	catch (ParameterNotFoundException pnfe) {pnfe.printStackTrace();}
	        	break;
	        	
	        case TSOpCode.COMM_EDIT_POLICY:
	        	String policyName = null, shortName = null, editedFile = null;
	        	Long policyId = null;
	        	File uploadedFile = null;        	
	        	String fileName      = null;
	        	String currentPolicyName = null;
	        	try {
	        		policyName		 =  mpp.getMultipartStringParameter(NAME_OF_POLICY);
					shortName 			 =  mpp.getMultipartStringParameter(SHORT_NAME_POL);
					editedFile 			 =  mpp.getMultipartStringParameter(FILE_NAME);
					policyId 				 = Long.parseLong(mpp.getMultipartStringParameter(FILE_POLICY_ID));
					currentPolicyName = mpp.getMultipartStringParameter(CURRENT_POLICY_NAME);
				} catch (ParameterNotFoundException e1) {
					try {
						throw new UploadPolicyDocException("This two parameters are requiered!");
					} catch (UploadPolicyDocException upde){
						forward(request, response, URLMaping.UPLOAD_POLICY + formatExceptionQuery(upde));
					}
				}
				
				String encondingType = "UTF-8";
				String forwardQueryError = "?" + RET_VAR + "="+RET_BAD
										 + "&" + RET_MSG + "=";
				String forwardQuery = "?" + RET_VAR + "="+RET_OK
				+ "&" + RET_MSG + "=" 
				+ URLEncoder.encode(UPDATE_DATE,encondingType)
				+ "&" + UploadPolicyDoc.FILE_NAME + "="
				+ editedFile
				+ "&" + UploadPolicyDoc.FILE_POLICY_NAME + "="
				+ policyName
				+ "&" + UploadPolicyDoc.FILE_SHORT_NAME + "="
				+ shortName
				+ "&" + TSOpCode.OPCODE + "="
				+ TSOpCode.COMM_EDIT_POLICY;
			    try {
			    	uploadedFile		 =	 mpp.getFileParameter(POLICY_FILE);
			        FileInputStream fis	= new FileInputStream(uploadedFile);
			    	fileName				 	=     uploadedFile.getName();        		
			    	
			    	if (editedFile==null) {
			    		editedFile = "";
			    	}
			    	if (currentPolicyName==null) {
			    		currentPolicyName = "";
			    	}
			    	if (!currentPolicyName.equals(policyName) && UserUtils.isTemplatePolicyNameAlreadySaved(commId, policyName)) {
						forwardQueryError += URLEncoder.encode("Policy Name already exists!",encondingType);
						forward(request, response, URLMaping.UPLOAD_POLICY + forwardQueryError);
					} else if (!editedFile.equals(fileName) && UserUtils.isTemplatePolicyFileNameAlreadySaved(commId, fileName)) {
						forwardQueryError += URLEncoder.encode("Policy File Name already exists!",encondingType);
						forward(request, response, URLMaping.UPLOAD_POLICY + forwardQueryError);
					}
			    	
			    	//removes existing files from disk
			    	removeFileFromDisk(tempDir, policyId);        		
			    	//updload  new file
			    	policyId = uploadFile(policyId, policyName, shortName, fileName, fis,  commId, tempDir, returnStatus, returnMessage);
			    		
			    }catch (ParameterNotFoundException e) {
			    	// if File parameter was not found, it means that only other info was modified
			    	// and only the referred data will be modified.
			    	try {
			       		
			       		if (editedFile == null || editedFile.equals("")) {
			       			throw new UploadPolicyDocException("The remove process failed!");
			       		} else {
			       			//updates file info
			       			String where = DBConstants.FIELD_COMMUNITY_TEMPLATES_ID+"="+policyId;
			       			HashMap<String, String> fieldsAndValues = new HashMap<String, String>();
			           			fieldsAndValues.put(DBConstants.FIELD_COMMUNITY_TEMPLATES_NAME, policyName);
			           			fieldsAndValues.put(DBConstants.FIELD_COMMUNITY_TEMPLATES_SHORT_NAME, shortName);
			           			
			           		//execute update	
			           		DBManager.executeUpdate(DBConstants.TABLE_COMMUNITY_TEMPLATES, where, fieldsAndValues);
			       		}            		            		
			       	} catch (UploadPolicyDocException upde) {
			       		forward(request, response, URLMaping.UPLOAD_POLICY + formatExceptionQuery(upde));
			       	}
			    } catch (UploadPolicyDocException upde) {
			    	forward(request, response, URLMaping.UPLOAD_POLICY + formatExceptionQuery(upde));
			    }
			    
				forwardQuery += "&" + UploadPolicyDoc.FILE_NAME + "=" + editedFile
									   + "&" + UploadPolicyDoc.FILE_POLICY_ID + "=" + policyId;								           		
				forward(request, response, URLMaping.UPLOAD_POLICY + forwardQuery);
					
				break;
	        }
		}
	}
	
	/**
	 * 
	 * @param fileDesc
	 * @param shortName
	 * @param fileToUpload
	 * @param tempDir
	 * @param request
	 * @param response
	 * @param forwardLink
	 * @throws UploadPolicyDocException
	 */
	
	public static long uploadFile(  long policyId,
											  String policyName,
											  String shortPolicyName,										  
											  String fileName,
											  InputStream inputStream,											  
											  int commId,
											  File	tempDir	, 
											  StringBuilder returnStatus,
											  StringBuilder returnMessage
										  ) throws UploadPolicyDocException {
		
		//if the directory does not exists, create it
		if (!tempDir.isDirectory())
			          tempDir.mkdir();
		
		//get file extension
    	String fileExtension = (fileName.substring(fileName.lastIndexOf(".") + 1,
    			fileName.length())).toLowerCase();
				
    	//test if file extension is within defined file extensions
    	if (!AddDocsTemplates.knownExtensions.containsKey(fileExtension))
    		//	daca nu avem extensia acceptata..nu face uploar
    		throw new UploadPolicyDocException(ERR_TEXT_1);
		
		
    	//test to see if other files exists with this name
  	  String sql = "select count(*) as total from "+DBConstants.TABLE_COMMUNITY_TEMPLATES +
  	  					  " where "+DBConstants.FIELD_COMMUNITY_TEMPLATES_COMM_ID+ "=" + commId +
  	  					  " and " +DBConstants.FIELD_COMMUNITY_TEMPLATES_NAME + "= ?";
  	  
		try {
			int checkForSameFile = DBManager.getSimpleTemplate().queryForInt(sql,policyName);
			if(checkForSameFile > 0 && policyId < 0)
				throw new UploadPolicyDocException(ERR_TEXT_2);
	     }catch (Exception e) {
				e.printStackTrace();
				throw new UploadPolicyDocException(ERR_TEXT_2);
			}
		
  	  
		 int inputStreamLength = 0;
		//test if  the file is empty
		 try{
			 inputStreamLength = inputStream.available();
		 } catch (IOException io) {
		        	throw new UploadPolicyDocException(ERR_TEXT_10);		        
		 }
  	  
		 
		 
		 
		 //initialize variables based on edit/insert mode
		boolean isEdit;
		 if (policyId == -1){
			 isEdit      = false;
			 policyId  = DBManager.getNextId(DBConstants.TABLE_COMMUNITY_TEMPLATES, commId+"");
		 } else {			 
			 isEdit = true;			 
		 }
		 			  		
		DBConnection conn = null;      
		PreparedStatement ps = null;
		returnMessage.append(UPLOAD_OK);
		returnStatus.append(RET_OK);


    	//-----------------------------------------------------------------------------//
    	// insert file into database
    	// creates temp , java, class files
    	//-----------------------------------------------------------------------------//
        try
        {

        	StrUtil strUtil						= new StrUtil(inputStream);              	         	            
            String xJavaFile 			        = getJavaFile(commId, policyId); 			            
            String xPolicyTempFile       = getGeneratedTemplateFileName(fileName, policyId, commId);

	
			if( AddDocsTemplates.docDocumentsExtensions.get(fileExtension)!=null ){
	            
	            //creates a temporary file for doc file to store content
	            ByteArrayInputStream in = strUtil.getStreamCopy();
	            byte []b = new byte[(int)in.available()];
	    		in.read(b);
	    		in.close();

	    		String tempFile = xPolicyTempFile.replaceAll("temp", "").replaceAll("lates", "templates").replaceAll("//","/");
	    		FileOutputStream out = new FileOutputStream (tempFile);
	    		out.write(b);
	    		out.close();
	            
	    		//compile doc file
	    		OfficeDocumentContents doc = null;
	    		try {
	    			doc = new OfficeDocumentContents(tempFile);
	    			AddDocsTemplates.addTempFilesNew( doc , xPolicyTempFile, xJavaFile, new Long(policyId).intValue(),fileExtension);
	    		}catch(Exception e) {
	    			e.printStackTrace();
	    		}finally {
	    			if(doc!=null) {
	    				OfficeDocumentContents.closeOO(doc.getXComponent());
	    			}
	    		}
				//AddDocsTemplates.addTempFilesNew(tempFile, xPolicyTempFile, xJavaFile, new Long(policyId).intValue(),fileExtension);
				
				//removes the temporarly created file
				 new File (tempFile).delete();
			}
			else{
	            //close the input stream            	
	            String templateContent  		= StrUtil.getStreamAsString(strUtil.getStreamCopy());	
				AddDocsTemplates.addTempFilesNew(new StringBufferContents(templateContent), xPolicyTempFile, xJavaFile, new Long(policyId).intValue(),fileExtension);			
			}
			
			
			
			
			///executes an update on the templates table
	       	String sqlStm = null;
        	conn = ConnectionPool.getInstance().requestConnection();
        	        	
          
                  	
        	Date date 							= new Date();
			long time 							= date.getTime();
			String dataCalen 				= new FormatDate(FormatDate.TIMESTAMP).getDate(time);
			String sDate 						= "str_to_date( '" + dataCalen + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
			
			
			//get file content		
									
			sqlStm = "UPDATE " + DBConstants.TABLE_COMMUNITY_TEMPLATES + 
			 " SET " 
			+ DBConstants.FIELD_COMMUNITY_TEMPLATES_COMM_ID + "= ?,"
			+ DBConstants.FIELD_COMMUNITY_TEMPLATES_NAME +  "=?,"
			+ DBConstants.FIELD_COMMUNITY_TEMPLATES_SHORT_NAME + "=?,"
			+ DBConstants.FIELD_COMMUNITY_TEMPLATES_FILENAME + "=?,"
			+ DBConstants.FIELD_COMMUNITY_TEMPLATES_LAST_UPDATE + "=" + sDate +","
			+ DBConstants.FIELD_COMMUNITY_TEMPLATES_CONTENT+ "= ? " 
			+ " WHERE "+DBConstants.FIELD_COMMUNITY_TEMPLATES_ID + "=?";
			
											
        	ps  = conn.prepareStatement(sqlStm);
        	ps.setObject( 1 , commId ,java.sql.Types.BIGINT);
        	ps.setString( 2 , policyName);
        	ps.setString( 3 , shortPolicyName);
        	ps.setString( 4 , fileName);
        	ps.setBinaryStream(5, strUtil.getStreamCopy() , inputStreamLength);
        	ps.setObject( 6 , policyId ,java.sql.Types.BIGINT);
        	
        	ps.executeUpdate();			
			ps.close();
			
			//closes the stream
			inputStream.close();
			
			// propagate template
			final String pName = policyName;
			final String dCalen = dataCalen;
			final int cId = commId;
			
			Thread t = new Thread() {
				@Override
				public void run() {
					DistributedMutex.propagateTemplate(pName, dCalen, cId);
				}
			};
			t.start();
        }
        catch (TemplatesException te) {
        	te.printStackTrace();
        	returnMessage.delete(0, returnMessage.length());
        	returnStatus.delete(0, returnStatus.length());
        	returnMessage.append("Your file contains errors.Please review it and try again!");        	
        	returnStatus.append(RET_BAD);
        	throw  new UploadPolicyDocException(returnMessage.toString());
        }
        catch (IOException ie) {            			
        	ie.printStackTrace();
        	returnMessage.delete(0, returnMessage.length());
        	returnStatus.delete(0, returnStatus.length());
        	returnMessage.append("There was a problem creating files on the system!");
        	returnStatus.append(RET_BAD);
        }         
        catch (BaseException be) {
        	be.printStackTrace();
        	returnMessage.delete(0, returnMessage.length());
        	returnStatus.delete(0, returnStatus.length());
        	returnMessage.append("Sql connection could not be established!");
        	returnStatus.append(RET_BAD);
        }
        catch (SQLException se) {
        	se.printStackTrace();
        	returnMessage.delete(0, returnMessage.length());
        	returnStatus.delete(0, returnStatus.length());
        	returnMessage.append("There was a problem with the sql query");
        	returnStatus.append(RET_BAD);
        } finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);			    
				//deletes record on bad files.only when insert new file.When delete it restore the old file
				if (returnStatus.equals(RET_BAD) && !isEdit )
					   removeFile(tempDir,policyId);
			}catch(BaseException e1){
			    throw new UploadPolicyDocException("SQLException:" + e1.getMessage());
			}			
		}
         

  return policyId; 
  }
	
	
	/**
	 * 
	 * @param tempDir  --file location
	 * @param policyId  -- tempalte id
	 * @throws UploadPolicyDocException
	 */
	
	
	public static void removeFileFromDisk (File tempDir, long policyId) throws UploadPolicyDocException{
		//deletes all this policyId info relateds  on the filesystem
		if ( !tempDir.isDirectory() ) {
			throw new UploadPolicyDocException("args[0] must be a directory!");
		}
		
		File[] filesToDel = tempDir.listFiles();		
		for (int i = 0; i < filesToDel.length; i++) {
			if ( ((File) filesToDel[i]).getName().contains(String.valueOf(policyId))) {
				 filesToDel[i].delete();				
			}
		}
	}
	
	public static void removeFile(File tempDir, long policyId) throws UploadPolicyDocException {

		
		//removes files from disk
		removeFileFromDisk(tempDir,policyId);
		
		//deletes file from the database		
		String where = DBConstants.FIELD_COMMUNITY_TEMPLATES_ID + "='" + policyId+"'";
		if (!DBManager.executeDelete(DBConstants.TABLE_COMMUNITY_TEMPLATES, where))
			throw  new UploadPolicyDocException (ERR_TEXT_3);
	}
	
	
	/**
	 * 
	 * @param commId 			- integer value, represents the value of community ID from database
	 * @param templateDocName 	- the name of .doc template
	 * @return byteArray		- content of .doc template in byte[] format
	 */
	public byte[] getContentOfDocTemplate (int commId, String templateDocName) {
		byte[] byteArray =  null;
		DBConnection conn = null;
		
		try {
    		PreparedStatement pstmt;    
    		conn  = ConnectionPool.getInstance().requestConnection();
            String sqlPhrase = "SELECT " + DBConstants.FIELD_COMMUNITY_TEMPLATES_CONTENT  + " FROM " + 
        			DBConstants.TABLE_COMMUNITY_TEMPLATES + " WHERE " + 
        			DBConstants.FIELD_COMMUNITY_TEMPLATES_COMM_ID + "= ? AND " + 
        			DBConstants.FIELD_COMMUNITY_TEMPLATES_NAME + "= ?";
            
           	pstmt = conn.prepareStatement(sqlPhrase);
           	pstmt.setInt(1, commId);
           	pstmt.setString(2, templateDocName);
           	
   			ResultSet rs = pstmt.executeQuery();

   			if(rs.next()) {
   				byteArray = rs.getBytes(1);
   			}  
   			
    		pstmt.close();
    		
        } catch (Exception e) {
        	e.printStackTrace();
        	
        } finally {
            if (conn != null) {
                try {
                    ConnectionPool.getInstance().releaseConnection(conn);
                } catch (Exception e) {
                	logger.error(e);
                }
            }
        }
		
		return byteArray;
	}
	
	
	/**
	 * 
	 * @param req - HttpServletRequest object
	 * @param resp - HttpServletResponse object
	 */
	public void downloadAllFiles(HttpServletRequest req, HttpServletResponse resp) {
		try {		
			int commId = Integer.parseInt(req.getParameter(CommunityAttributes.COMMUNITY_ID));
			String path = ServerConfig.getTsrCreationTempFolder();
			String directoryName = "templatesFolder" + Long.toString(System.currentTimeMillis());
			
			File tmpDir = new File("");
			if (StringUtils.isNotEmpty(path)) {
				path += File.separator + directoryName + File.separator;
				tmpDir = new File(path);
				if (!tmpDir.isDirectory())
					tmpDir.mkdir();				
			}
			
			List<CommunityTemplatesMapper> templates = new ArrayList<CommunityTemplatesMapper>();
				
			try {
				templates = DBManager.getSimpleTemplate().query(SQL_GET_TEMPLATES_FROM_COMM, new CommunityTemplatesMapper(), commId);
				
				for (CommunityTemplatesMapper temp : templates) {
					final String templatePath	= temp.getPath();
					final String templateName	= temp.getName();
					String fileContent = temp.getFileContent();
					String fileExtension = templatePath.substring(templatePath.lastIndexOf(".") + 1);
					
					if (AddDocsTemplates.docDocumentsExtensions.get(fileExtension)!=null) {
						byte[] byteArray = getContentOfDocTemplate(commId, templateName);
						if (byteArray != null)
							FileUtils.writeByteArrayToFile(new File(path + templatePath), byteArray);
						else 
							FileUtils.writeByteArrayToFile(new File(path + templatePath), new byte[0]);
					} else {
						FileUtils.writeStringToFile(new File(path + templatePath), fileContent);
					}
				}
				
			} catch (Exception e) {
				logger.error("Error on saving templates to local folder:" + e);
				e.printStackTrace();
			}

			String directoryPath = path.substring(0, path.length()-1);
			String zipFile = ServerConfig.getTsrCreationTempFolder() + File.separator + directoryName + ".zip";
			ZipOutputStream zos;
					
			try {
				//ZipUtils.zipFolder(directoryPath,"templatesFolder", zipFile);
				zos = new ZipOutputStream(new FileOutputStream(zipFile));
				ZipUtils.zipFolder(directoryPath,"templatesFolder", zos);
				zos.close();
				
				File f=new File(zipFile);
				resp.setHeader(	"Content-Disposition", " attachment; filename=\"" + FilenameUtils.getName(zipFile) + "\"");
				resp.setContentType("zip");
				resp.setContentLength((int)f.length());
				
				OutputStream out = resp.getOutputStream();							
				InputStream in=new BufferedInputStream(new FileInputStream(f));
				byte[] buff=new byte[100];
				int n;
				
				while((n=in.read(buff))>0) {
					 out.write(buff, 0, n);
				}
				
				in.close();					 
				out.close();				   
			} catch (Exception e) {
				logger.error("Error on creating archive of folder with all templates: " + e);
				e.printStackTrace();
			}
					
			if (tmpDir.exists()) {
				try {
					FileUtils.deleteDirectory(tmpDir);
				} catch (IOException e) {
					logger.error("Error on deleting temporary folder: " + e);
					e.printStackTrace();
				}
			}

		} catch (Exception e) {
			logger.error("Error appeared when downloading all templates from community: " + e);
			e.printStackTrace();
		} 
	}
	
	
	/**
	 * Set exception format
	 * @param exc
	 * @return
	 */
	
	private static String formatExceptionQuery(Exception exc) {
		try {
			String encondingType = "UTF-8";
			String forwardQuery = "?" + RET_VAR + "="+RET_BAD
							+ "&" + RET_MSG + "=" + URLEncoder.encode(exc.toString()
									.substring(exc.toString().indexOf(": ") + 2, exc.toString()
									.length()),encondingType);
			return forwardQuery;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Fill policy documents combo box with values
	 * @param sessionId
	 * @param commId
	 * @param outType
	 * @return
	 */
	public static String getHtmlOptionTemplates ( String sessionId, String commId, int outType,UserAttributes currentUser) {
		String retHTML = "";
		String fileName = "";
		String policyName = "";
		String shortName = "";
		String policyId = "";
		String rowStyle = "";
		
		boolean isTSAdmin = false;
		boolean isTSCadmin = false;
		boolean isCommAdmin = false;
		
		try{isTSAdmin  		= UserUtils.isTSAdmin (currentUser);	}catch(BaseException base){base.printStackTrace();}
		try{isTSCadmin  	= UserUtils.isTSCAdmin(currentUser);}catch(BaseException base){base.printStackTrace();}
		try{isCommAdmin  	= UserUtils.isCommAdmin(currentUser);}catch(BaseException base){base.printStackTrace();}
		
		//BigDecimal userIdBD = InstanceManager.getCurrentInstance().getCurrentUser().getID();
		//long user_id = userIdBD.longValue(); 
		 
		String stm = "SELECT "+
			DBConstants.FIELD_COMMUNITY_TEMPLATES_ID+ "," +
			DBConstants.FIELD_COMMUNITY_TEMPLATES_COMM_ID +"," +
			DBConstants.FIELD_COMMUNITY_TEMPLATES_NAME + "," +
			DBConstants.FIELD_COMMUNITY_TEMPLATES_SHORT_NAME +"," +
			DBConstants.FIELD_COMMUNITY_TEMPLATES_FILENAME + 
		    " FROM " +
			 DBConstants.TABLE_COMMUNITY_TEMPLATES + " WHERE COMM_ID=" +
		     Long.parseLong(commId);

		DBConnection conn = null;		
		try{
			conn = ConnectionPool.getInstance().requestConnection();
			DatabaseData data = conn.executeSQL(stm);			
			
			for (int i = 0; i<data.getRowNumber(); i++){
				try{
									
					 policyId 		 = data.getValue(DBConstants.FIELD_COMMUNITY_TEMPLATES_ID,i).toString();
					 if(data.getValue(DBConstants.FIELD_COMMUNITY_TEMPLATES_NAME,i)==null) {
						 continue;
					 }
					 policyName = data.getValue(DBConstants.FIELD_COMMUNITY_TEMPLATES_NAME,i).toString();
					 shortName   =  data.getValue(DBConstants.FIELD_COMMUNITY_TEMPLATES_SHORT_NAME,i).toString();
					 fileName      =  data.getValue(DBConstants.FIELD_COMMUNITY_TEMPLATES_FILENAME,i).toString();
					 
					 if( !fileName.startsWith(TemplateUtils.DASL_CODE) || isTSAdmin || isCommAdmin || isTSCadmin){
						if (outType == OUT_OPTIONS_HTML) {
							retHTML += "<option value=\"" +fileName+"#%%#" + policyId + "#%%#" + shortName											
										+ "\">" + policyName + "</option>\n";
						} else {
							rowStyle = (i%2 == 0) ? "row1" : "row2";
							retHTML += "<tr class=\"" + rowStyle + "\"><td>" + policyName
										+ "</td><td>" + shortName
										+ "</td><td>" + fileName + "</td></tr>";
							
						}
					 }
				}
				catch(Exception e){
					e.printStackTrace(System.err);					
				}//closed catch block
			}

		}//closed try
		catch(Exception e){
			e.printStackTrace(System.err);
		}
        finally {
            try{
                ConnectionPool.getInstance().releaseConnection(conn);
            }catch(BaseException e){
                e.printStackTrace();
            }           
        }
        
		return retHTML;
	}
	/*
	public static void updateDB(String sessionId, int operType, CommunityAttributes ca) {
		updateDB(sessionId, operType, ca.getID().longValue());
	}
	/*
	public static void updateDB(String sessionId, int operType, long commId) {
		String tmpMetaFile = "";
		if (operType == COMMUNITY_EXISTS) {
			tmpMetaFile = FINAL_TEMPLATES + fSep + commId + fSep + TEMPL_META_TILE;
		} else if (operType == COMMUNITY_NEW) {
			tmpMetaFile = TMP_TEMPLATES + fSep + sessionId + fSep + TEMPL_META_TILE;
		}
        //File templatesDtdFile = new File(TMP_TEMPLATES_DTD_PATH + fSep + "templates.dtd");
        HashMap templates = new HashMap();
        String xFileName = "";
        String xFileDesc = "";
        String xShortName = "";
        String xPolicyID = "";
        File currentFile = null;
        try {
			if ( !(new File(TEMPLATES_PATH)).exists() )
				if ( !(new File(TEMPLATES_PATH)).mkdir() )
					throw new UploadPolicyDocException(ERR_TEXT_5);
			File templatesDir = new File( TEMPLATES_PATH + fSep + commId );
			if ( !templatesDir.exists() )
				if ( !templatesDir.mkdir() )
					throw new UploadPolicyDocException(ERR_TEXT_5);
			Element e = null;
			Node t_1 = null;
			
			if ( (new File(tmpMetaFile)).exists() ) {
	    		try {
	    			DOMParser readDoc = new DOMParser();
					readDoc.parse(getURIStringFromPath(tmpMetaFile));
					Document xmldoc = readDoc.getDocument();
					Element rootElem = xmldoc.getDocumentElement();
					int nrNodes = rootElem.getChildNodes().getLength();
					for (int i = 0; i < nrNodes; i++) {
						t_1 = rootElem.getChildNodes().item(i);
						if ( t_1.getNodeName().equals("template")) { 
							xFileName = rootElem.getChildNodes().item(i).getAttributes().getNamedItem(X_POLICY_FILE).getNodeValue();
							xFileDesc = rootElem.getChildNodes().item(i).getAttributes().getNamedItem(X_POLICY_NAME).getNodeValue();
							xShortName = rootElem.getChildNodes().item(i).getAttributes().getNamedItem(X_POLICY_SHORT_NAME).getNodeValue();
							xPolicyID = rootElem.getChildNodes().item(i).getAttributes().getNamedItem(X_POLICY_ID).getNodeValue();
							templates.put(FILE_NAME, xFileName);
							templates.put(FILE_POLICY_NAME, xFileDesc);
							templates.put(SHORT_NAME_POL, xShortName);
							templates.put(FILE_COMM_ID, new BigDecimal(commId));
							templates.put(FILE_POLICY_ID, xPolicyID);
							if (operType == COMMUNITY_EXISTS) {
								currentFile = new File(FINAL_TEMPLATES + fSep + commId + fSep + xFileName);
							} else if (operType == COMMUNITY_NEW) {
								currentFile = new File(TMP_TEMPLATES + fSep + sessionId + fSep + xFileName);
							}
							if (currentFile.exists()) {
								if (operType == COMMUNITY_NEW) {
									FileCopy.copy(currentFile, templatesDir + fSep + xFileName);
								}
							} else {
								//throw new UploadPolicyDocException(ERR_TEXT_7);
							}
							if ( (new File(templatesDir + fSep + xFileName)).exists() ) {
								try {
									insertTemplateDB(templates, operType, new Long(commId).intValue());
								} catch (DataException e2) {
									e2.printStackTrace();
									//if the database couldn't be updated, then the file  should be deleted
									if ( (new File(templatesDir + fSep + xFileName)).exists() )
										(new File(templatesDir + fSep + xFileName)).delete();
								} catch (BaseException e2) {
									e2.printStackTrace();
									//if the database couldn't be updated, then the file  should be deleted
									if ( (new File(templatesDir + fSep + xFileName)).exists() )
										(new File(templatesDir + fSep + xFileName)).delete();
								}
							} else {
								//throw new UploadPolicyDocException(ERR_TEXT_8);
							}
						}
					}
					if (operType == COMMUNITY_NEW) {
						FileCopy.copy(new File(TMP_TEMPLATES + fSep + sessionId + fSep + TEMPL_META_TILE), 
								templatesDir + fSep + TEMPL_META_TILE);
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (SAXException e1) {
					e1.printStackTrace();
				} finally {
					if (operType == COMMUNITY_NEW) {
						FileUtils.deleteDir(new File(TMP_TEMPLATES));
					}
					deletedTempl = false;
				}
	    	}
			
//				temp directory must be deleted after the data has been updated
		} catch (UploadPolicyDocException e) {
			e.printStackTrace();
		}
	}
	*/
	/**
	 * 
	 * @param fileName
	 * @param sessionId
	 * @param commAction
	 * @param commId
	 * @return
	 */
	public static String getDocURL(String fileName, String sessionId, int commAction, int commId) {
		File templatesDir = null;
		String webPath = null;
		switch(commAction) {
			case COMM_ADD_ACTION:
				templatesDir = new File(TMP_TEMPLATES + fSep + sessionId);
				webPath = TMP_TEMPLATES_WEB_PATH + fSep + sessionId;
				break;
			case COMM_EDIT_ACTION:
				templatesDir = new File(TEMPLATES_PATH + fSep + commId);
				webPath = TEMPLATES_WEB_PATH + fSep + commId;
				break;
		}
		String URLtoFile = null;
		
		if (templatesDir.exists() && templatesDir.isDirectory()) {
			File[] filesInside = templatesDir.listFiles();
			for (int i = 0; i < filesInside.length; i++) {
				if (filesInside[i].getName().equals(fileName)) {
					URLtoFile = webPath + fSep + fileName;
				}
			}
		}
		return URLtoFile.replaceAll("\\\\", "/");
	}
	
	
	/**
	 * Insert template into database
	 * @param data
	 * @param operType
	 * @param commId
	 * @throws DataException
	 * @throws BaseException
	 */
	public static void insertTemplateDB(HashMap data, int operType, int commId) throws DataException, BaseException {
		Date date = new Date();
		long time = date.getTime();
		String dataCalen = new FormatDate(FormatDate.TIMESTAMP).getDate(time);
		String sDate = "str_to_date( '" + dataCalen + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
		String sqlRmvFiles = "DELETE FROM " + DBConstants.TABLE_COMMUNITY_TEMPLATES
							+ " WHERE " + DBConstants.FIELD_COMMUNITY_TEMPLATES_COMM_ID + "=" + commId;
		String sqlStm = "INSERT INTO " + DBConstants.TABLE_COMMUNITY_TEMPLATES + "("
						+ DBConstants.FIELD_COMMUNITY_TEMPLATES_ID + ","
						+ DBConstants.FIELD_COMMUNITY_TEMPLATES_COMM_ID + ","
						+ DBConstants.FIELD_COMMUNITY_TEMPLATES_NAME + ","
						+ DBConstants.FIELD_COMMUNITY_TEMPLATES_SHORT_NAME + ","
						+ DBConstants.FIELD_COMMUNITY_TEMPLATES_FILENAME + ","
						+ DBConstants.FIELD_COMMUNITY_TEMPLATES_LAST_UPDATE + ") VALUES ( ?, ?, ?, ?, ?, "
						+ sDate + ")";
		
			
		try {
			if (operType == COMMUNITY_EXISTS && deletedTempl == false) {
				DBManager.getSimpleTemplate().update(sqlRmvFiles);
            	deletedTempl = true;
            }
			DBManager.getSimpleTemplate().update(sqlStm,data.get(FILE_POLICY_ID),data.get(FILE_COMM_ID).toString(),data.get(FILE_POLICY_NAME).toString(),
					data.get(SHORT_NAME_POL).toString(),data.get(FILE_NAME).toString());
		} catch (Exception e) {
			e.printStackTrace();
            throw new BaseException("SQLException:" + e.getMessage());
		}
	}
	
	/**
	 * Generate temp file name
	 * @param fileName
	 * @param policyId
	 * @param commId
	 * @return String temp file
	 */
	public static String getGeneratedTemplateFileName(String fileName, long policyId, int commId){
		
		String tempFile  = getTemplatesPath(commId);
		 tempFile 		+= fileName.substring(0, fileName.lastIndexOf(".")) +TEMP_SUFIX + policyId +              			
									   fileName.substring(fileName.lastIndexOf(".") );		
	return tempFile;
	}
	
	/**
	 * Generate java file name
	 * @param commId
	 * @param policyId
	 * @return String javaFile
	 */
	public static String  getJavaFile(int commId,long policyId){
		
		String javaFile			  = getTemplatesPath(commId);
        		  javaFile 		   +=  "temp" +  policyId + ".java";
        		  
     return javaFile;
	}
	
	/**
	 * Generate templates path name
	 * @param commId
	 * @return String path name
	 */
	public static String getTemplatesPath(int commId){
		String comm = "";
		//global template will have commID -3
		if (commId == -3) {
			comm = "global";
		} 
		//legal templates have the commId = -4
		else if (commId == AddDocsTemplates.LEGAL_COMMID) {
			comm = AddDocsTemplates.LEGAL_FOLDER_NAME;
		}
		//base file templates have the commId = -5
		else if (commId == AddDocsTemplates.BASE_FILE_COMMID) {
			comm = AddDocsTemplates.BASE_FILE_FOLDER_NAME;
		}
		else {
			comm = Integer.toString(commId);
		}
		String templatesPath = FINAL_TEMPLATES+File.separator + comm + File.separator;
		
	 return templatesPath;
	}
	
 /**
  * Get root templates path
  * @return 
  */	
  public static String getPath(){
	  
	  String path   = FINAL_TEMPLATES;
	  
	return path;  
  }
	
	/**
	 * 
	 * @param inputStr
	 * @return
	 */
	public static String escapeORAStr(String inputStr) {
		String escapedStr = inputStr.replaceAll("'", "''");
		return escapedStr;
	}
	
}
