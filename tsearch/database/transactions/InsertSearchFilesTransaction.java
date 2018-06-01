package ro.cst.tsearch.database.transactions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.rowmapper.CommunityTemplatesMapper;
import ro.cst.tsearch.generic.IOUtil;
import ro.cst.tsearch.threads.GPThread;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.utils.ZipUtils;

public class InsertSearchFilesTransaction implements TransactionCallback {

	private Search search = null;
	
	public InsertSearchFilesTransaction(Search search) {
		super();
		this.search = search;
	}

	public Search getSearch() {
		return search;
	}

	public void setSearch(Search search) {
		this.search = search;
	}

	public Object doInTransaction(TransactionStatus status) {
		String sql = null;
		String sqlFlags = null;
        //insert search order, search log and search index
        try{
        	SimpleJdbcTemplate sjt = ConnectionPool.getInstance().getSimpleTemplate();
        	
        	File zipSearchOrder = null;
        	File zipLogFile = null;
        	File zipIndexFile = null;
        	
        	String sourceZipOrderLink = search.getSearchDir() + "orderFile.html";
        	String sourceZipLogLink = search.getSearchDir() + "logFile.html";
        	String sourceZipTsrIndexLink = search.getSearchDir()+"tsrIndexFile.html";
        	
        	String destZipOrderLink =search.getID()+"orderFileZip.zip";
        	String destZipLogLink =search.getID()+"logFileZip.zip";
        	String destZipTsrIndexLink =search.getID()+"tsrIndexFileZip.zip";
        		
        	ByteArrayOutputStream searchOrderBaos = null;
        	//File searchOrder = new File( sourceZipOrderLink );
        	File searchLogFile = new File( sourceZipLogLink );
        	File tsrIndexFile = new File( sourceZipTsrIndexLink );
        	
        	if(ServerConfig.isEnableOrderOldField()) {
	            if(sourceZipOrderLink!=null && sourceZipOrderLink.length()>0){
	            	ZipUtils.zipFile(sourceZipOrderLink,destZipOrderLink);
	            	zipSearchOrder = new File( destZipOrderLink );
	            }
	            searchOrderBaos = new ByteArrayOutputStream();
	        	if (zipSearchOrder.exists()){
	        		IOUtil.copy( new FileInputStream( zipSearchOrder ) , searchOrderBaos);
	        	}
        	}
            if(ServerConfig.isEnableLogOldField()) {
	            if(sourceZipLogLink!=null && sourceZipLogLink.length()>0){
	            	ZipUtils.zipFile(sourceZipLogLink,destZipLogLink);
	            	zipLogFile = new File( destZipLogLink );
	            }
            }
            ByteArrayOutputStream logFileBaos = new ByteArrayOutputStream();
        	if(zipLogFile != null && zipLogFile.exists()) {
        		IOUtil.copy( new FileInputStream( zipLogFile ) , logFileBaos);
        	}
            
        	
        	//I must create it because I am the one that deletes it when saving :)
        	if(ServerConfig.isEnableTsrLogOldField() && (!tsrIndexFile.exists() || tsrIndexFile.length() == 0)) {
        		
        		System.err.println("Error When saving T link Log, no file to save" + search.getID());
        		System.err.println("Error When saving T link Log, no file to save" + search.getID());
        		System.err.println("Error When saving T link Log, no file to save" + search.getID());
        		System.err.println("Error When saving T link Log, no file to save" + search.getID());
        		
        		try{
                	PrintWriter pw = new PrintWriter(sourceZipTsrIndexLink);
                	
                	HashMap<String,String> templatesMap = new HashMap<String,String>();
                	try {
        	        	List<CommunityTemplatesMapper> templ = UserUtils.getUserTemplates(
        	        			search.getAgent().getID().longValue(),-1, UserUtils.FILTER_BOILER_PLATES_EXCLUDE, search.getProductId());
        	        	for(CommunityTemplatesMapper cmt : templ) {
        	        		templatesMap.put(cmt.getName(), cmt.getPath());
        	        	}
                	}catch(Exception ignored) {}
                	
    	        	String str = GPThread.createTsrIndexHtmlContents(search.getSearchFlags().isTsrCreated(), search, URLMaping.path, templatesMap, new ArrayList<String>(), null);
    	
    	        	pw.print("<html><head><title>TSR Index Page</title></head><body>\n");
    	        	pw.print(str);
    	        	pw.print("</body></html>");
    	        	pw.flush();
    	        	pw.close();
                	
                }catch(Exception ignored){
                	ignored.printStackTrace();
                }
        	}
        	
        	ByteArrayOutputStream indexFileBaos = new ByteArrayOutputStream();
        	if(ServerConfig.isEnableTsrLogOldField()) {
	            if(sourceZipTsrIndexLink!=null && tsrIndexFile.length()>0){
	            	ZipUtils.zipFile(sourceZipTsrIndexLink,destZipTsrIndexLink);
	            	zipIndexFile = new File( destZipTsrIndexLink );
	            }
	        	if( zipIndexFile != null && zipIndexFile.exists()){
		        	IOUtil.copy( new FileInputStream( zipIndexFile ) , indexFileBaos);
	        	}
        	}
            
        	int paramIndexO = 0;
    		int paramIndexS = 1;
    		int paramIndexT = 2;
    		int paramSize = 3;
    	
    		
    		if (!ServerConfig.isEnableOrderOldField()) {
    			paramIndexO = -1;
    			paramIndexS --;
    			paramIndexT --;
    			paramSize --;
    		}
    		
    		if(!(searchLogFile.exists() && ServerConfig.isEnableLogOldField())) {
    			paramIndexS = -1;
    			paramIndexT --;
    			paramSize --;
    		}
    		if(!ServerConfig.isEnableTsrLogOldField()) {
    			paramIndexT --;
    			paramSize --;
    		}
        	
        	
        	PreparedStatementCreatorFactory pstmt = null;
        	Object[] params = null;
        	
    		sql = "UPDATE " + DBConstants.TABLE_SEARCH_DATA_BLOB + " SET ";
    		if(paramIndexO >= 0) {
    			sql += "`searchOrder` = ?, ";
    		}
    		if(paramIndexS >= 0) {
    			sql +="`searchLog` = ?, ";
    		}
    		if(paramIndexT >= 0) {
    			sql += "`searchIndex` = ? ";
    		}
    		sql += " WHERE " + DBConstants.FIELD_SEARCH_DATA_BLOB_ID + 
    			" = " + search.getSearchID();
    		sqlFlags = "UPDATE " + DBConstants.TABLE_SEARCH_FLAGS + " SET " + 
    			DBConstants.FIELD_SEARCH_FLAGS_ORDER_STATUS + " = 1 , ";
    		sqlFlags += DBConstants.FIELD_SEARCH_FLAGS_LOG_STATUS + " = 1, ";
    		sqlFlags += DBConstants.FIELD_SEARCH_FLAGS_INDEX_STATUS + " = 1 WHERE " + 
        			DBConstants.FIELD_SEARCH_FLAGS_ID + " = " + search.getSearchID();
    		
    		if(searchLogFile.exists() && ServerConfig.isEnableLogOldField()) {
    			params = new Object[paramSize];
    			if(paramIndexS >= 0) {
    				params[paramIndexS] = logFileBaos.toByteArray();		//search log
    				
    			}
    			if(paramIndexT >= 0) {
    				params[paramIndexT] = indexFileBaos.toByteArray();	//tsr index
    			}
    			
    		} else {
    			params = new Object[paramSize];
    			if(paramIndexT >= 0) {
    				params[paramIndexT] = indexFileBaos.toByteArray();	//just tsr index -> create tsr without Automatic
    			}
    		}
    		
    		
    		pstmt = new PreparedStatementCreatorFactory(sql);
    		if(paramIndexT >= 0) {
    			pstmt.addParameter(new SqlParameter(DBConstants.FIELD_SEARCH_DATA_BLOB_INDEX, Types.BLOB));
    		}
        	if(paramIndexO >= 0 && searchOrderBaos != null) {
        		pstmt.addParameter(new SqlParameter(DBConstants.FIELD_SEARCH_DATA_BLOB_ORDER,Types.BLOB));
        		params[paramIndexO] = searchOrderBaos.toByteArray();	//we always have order
        	}
        	
        	if(paramIndexS >= 0) {
        		pstmt.addParameter(new SqlParameter(DBConstants.FIELD_SEARCH_DATA_BLOB_LOG,Types.BLOB));
        	}
        	
        	sjt.update(sqlFlags);
        	if(params != null && params.length > 0) {
        		sjt.getJdbcOperations().update(pstmt.newPreparedStatementCreator(params));
        	}
        	
        	if (zipSearchOrder != null && zipSearchOrder.exists()){
        		zipSearchOrder.delete();
        		//searchOrder.delete();	//do not delete, it will be recreated again and again and again
        	}
        	if(zipLogFile != null && zipLogFile.exists()) {
        		zipLogFile.delete();
        	}
        	if( zipIndexFile != null && zipIndexFile.exists()){
        		zipIndexFile.delete();
        		//tsrIndexFile.delete();
        	}
        	
        	if(searchLogFile != null && searchLogFile.exists() && ServerConfig.isEnableLogOldField()) {
        		SearchLogger.finish(search.getID());
        		searchLogFile.delete();
        	}
        		
        } catch (Exception e) {
        	e.printStackTrace();
            status.setRollbackOnly();
            return false;
        } 
        return true;
	}

}
