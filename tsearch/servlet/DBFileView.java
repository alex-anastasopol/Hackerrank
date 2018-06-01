package ro.cst.tsearch.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Category;

import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.servlet.community.UploadPolicyDoc;
import ro.cst.tsearch.templates.AddDocsTemplates;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.exceptions.BaseException;

import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 *   Retrieves blob files stored in database
 *   Or it can can be used as download servlet for blob data
 * @author bogdan popa
 * 
 */
public class DBFileView extends HttpServlet {


	private static final long serialVersionUID = 1L;
	protected static final Category logger= Category.getInstance(UploadPolicyDoc.class.getName());
	
	private static Blob    blobFileContent;
	private static String blobFileContentAsString;
	private static InputStream blobStreamContent;
	private static String  blobFileName;
	private static String blobFileExtension;
	private static String  blobMimeType;
	
	private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws  ServletException,IOException
	{
		
		int fileId 					= Integer.parseInt(request.getParameter("fileId"));
		String tableName	= request.getParameter("tableName");        
		//------------------------------------------------------------------
		// fill global variables with blob content info
		//------------------------------------------------------------------
		setBlobFileInfo (fileId,tableName);
		
		//------------------------------------------------------------------
		//set file parameters
		//------------------------------------------------------------------		
		response.setContentType(blobMimeType);
		response.setHeader(
					"Content-Disposition",
					" attachment; filename=\""
						+ blobFileName
						+ "\"");
		
		
		//------------------------------------------------------------------		
		//display content
		//------------------------------------------------------------------		
		try{
		OutputStream out	= response.getOutputStream();				
		InputStream in=new BufferedInputStream(blobFileContent.getBinaryStream()); 
		 byte[] buff=new byte[100];
		 int n;
		 while((n=in.read(buff))>0)
		 {
		 	 out.write(buff, 0, n);
		 }
		 in.close();					 
		out.close();	
		}
		catch (SQLException se) {
			se.printStackTrace();
		}
	}
	
	/**
	 * SET all info regarding blob.It's class 'constructor' 
	 * @param fileId
	 * @param tableName
	 */
	
	public static void setBlobFileInfo(int fileId,String tableName) 
	{
		
		//------------------------------------------------------------------		
		///init variables
		//------------------------------------------------------------------				
				
		String columnFileName    = "";
		String columnFileContent = "";
		String columnFileId           = "";
		
		String sql      = "";
		DBConnection conn = null;
		Statement stmt;
		ResultSet rs;
		
		//------------------------------------------------------------------
		//retrieves current table columns.
		//If other tables will deal with blob,in this if sequence, can be done the process 		
		//------------------------------------------------------------------		
//        if (tableName.equals(DBConstants.TABLE_COMMUNITY_TEMPLATES))
        {
        		columnFileName    = DBConstants.FIELD_COMMUNITY_TEMPLATES_FILENAME;
        		columnFileContent  = DBConstants.FIELD_COMMUNITY_TEMPLATES_CONTENT;
        		columnFileId  			= DBConstants.FIELD_COMMUNITY_TEMPLATES_ID;
        }
        
        try{  
	        sql 	= 	" SELECT " +
	        						columnFileName +"," +
	        						columnFileContent      +
	        				" FROM " + DBManager.sqlTableName(tableName) +
	       	  				" WHERE " +columnFileId  +"=" +fileId;
	       	  						;
	       	//------------------------------------------------------------------	       	  							       	 
	       	//init db connection
	       //------------------------------------------------------------------	       	  						
			conn = ConnectionPool.getInstance().requestConnection();			
			stmt   =  conn.createStatement();
			rs      = stmt.executeQuery(sql);
						
			//------------------------------------------------------------------			
			//retrieves file name and content
			//------------------------------------------------------------------			
			if (rs.next()) {
				blobFileName	         	    = rs.getString(columnFileName);
				blobFileContent		 	    = rs.getBlob(columnFileContent);
				blobFileExtension            = blobFileName.substring(blobFileName.lastIndexOf(".")+1).toLowerCase();
				blobMimeType   		 	    = AddDocsTemplates.knownExtensions.get(blobFileExtension);		
				blobFileContentAsString  = convertFromBlobToString(blobFileContent);
				blobStreamContent          = blobFileContent.getBinaryStream();
			}	
			else{
				blobFileName     			 	= "";
				blobFileContent			 	= null;
				blobFileExtension		 	= "";
				blobFileExtension		 	= "";
				blobMimeType			 	= "";
				blobFileContentAsString	= "";
			}
				
        }
        catch (Exception e ){
        	logger.error("Error!!File could not be opened from the database!!");
        	e.printStackTrace();        	
        } finally {
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);			
			}catch(BaseException e1){
			    e1.printStackTrace();
			}			
		}
	}
	
	
	
	/**
	 * Receives a blob parameter.Return string content of the blob 
	 * @param Blob content
	 * @return String content
	 */
	public static String convertFromBlobToString(Blob content) {

		String stringContent = "";
		try {
//			InputStream is = new BufferedInputStream(content.getBinaryStream());
//			BufferedReader in = new BufferedReader(new InputStreamReader(is));
//			StringBuffer buffer = new StringBuffer();
//			String line;
//
//			if ((line = in.readLine()) != null)
//				buffer.append(line);
//			while ((line = in.readLine()) != null) {
//				buffer.append("\n");
//				buffer.append(line);
//			}
//
//			in.close();
//			stringContent = buffer.toString();
			
			stringContent = IOUtils.toString(content.getBinaryStream());
			
		} catch (Exception se) {
			se.printStackTrace();
			logger.error("Error in convertFromBlobToString", se);
		}
		return stringContent;
	}
	
	/**
	 * Retrieves file url
	 * @param policyId
	 * @return String file url 
	 */
	public static String getFileUrl(long policyId)
	{
		String serverName =  rbc.getString("app.url");
		String url 				= URLMaping.COMMUNITY_VIEW_FILE;
		String[] params		= {"policyId",String.valueOf(policyId)};
		String link 			    = serverName+URLMaping.getAbsoluteURL(url, params);
		
	return link;
	}
	
	/**
	 * GET method for blob content as string
	 * @return
	 */
	public static String getBlobContentAsString(){		
	return blobFileContentAsString;
	}
	
	/**
	 * GET method for blob file name
	 * @return
	 */
	public static String getBlobFileName(){
		return blobFileName;
	}
	
	/**
	 * GET method for blob file extension
	 * @return
	 */
	public static String getBlobFileExtension(){
		return blobFileExtension;
	}
	
	/**
	 * GET method for blob mime type
	 */
	public static String getBlobMimeType(){
		return blobMimeType;
	}
	
	/**
	 * GET method for blob content
	 */
	public static Blob getBlobContent(){
		return blobFileContent;
	}
	
	/**
	 * GET file content as stream
	 */
	public static InputStream getBlobContentStream(){
		
	return blobStreamContent;
	}
	
}
