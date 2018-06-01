package ro.cst.tsearch.search.filter.testnamefilter;

import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.Vector;

import ro.cst.tsearch.data.DataAttribute;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;


public class GenericNameFilterTestFiles extends DataAttribute {
	
	//this are the name of the attributes, where are them used?
	public static final String FILE_NAME	="NAME";
	public static final String FILE_ID		="ID";
	
	//attributes section: the column position in our data structure
	public static final int NAME	= 0;
	public static final int ID	= 1;
//why do I need 2 structures of the same data?
    private static Hashtable fileList = null;
    private static Vector files = null;
	@Override
	protected int getAttrCount() {
		//the number of columns in our data table
		return ID +1;
	}
	
	public void getFileList(){
	       DBConnection conn = null;
	       String stm = "select id, name from ts_filter_test_files";
	       fileList  = new Hashtable();
	       files = new Vector();
	       try {
	            
	            conn = ConnectionPool.getInstance().requestConnection();
	            DatabaseData data = conn.executeSQL(stm);
	            
	            int rownum = data.getRowNumber();
	            for(int i=0;i<rownum;i++){
	                GenericNameFilterTestFiles testFile = new GenericNameFilterTestFiles();
	                testFile.setFileId(new BigDecimal(data.getValue(1,i).toString()));
	                testFile.setName((String)data.getValue(2,i));
	                fileList.put(testFile.getFileId(), testFile );
	                files.addElement(testFile);
	            }
	            
	        }catch(Exception e){
	            e.printStackTrace();
	        }finally{
	            try{
	                ConnectionPool.getInstance().releaseConnection(conn);
	            }catch(BaseException e){
	                e.printStackTrace();
	            }           
	        }
	}
	
	public void setFileId(DatabaseData data, int row) {
		setAttribute(ID, data, FILE_ID, row);
	}
	public void setFileId(BigDecimal fileId) {
		setAttribute(ID, fileId);
	}
	public BigDecimal getFileId() {
		return (BigDecimal) getAttribute(ID);
	}
	
	public void setName(DatabaseData data, int row) {
		setAttribute(NAME, data, FILE_NAME, row);
	}
	public void setName(String fileName) {
		setAttribute(NAME, fileName);
	}
	public String getName() {
		return (String) getAttribute(NAME);
	}	
}
