package ro.cst.tsearch.utils.helpers;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.XStreamManager;
import ro.cst.tsearch.utils.ZipUtils;

public class SearchOpener {
	
	private static class ResultRow {
		public long id;
		public String fileId; 
		private Date sdate;
		private String atiFileReference;
		private long parentSearchId;
		private String countyName;
		private int commId;
	}
	
	private static class DateType {
		public Date  sdate;
		public int type;
	}
	
	public static void main(String[] args) {
		PrintWriter writer = null;
		try {
			/*
			Connection connection = DatabaseSync.getAts02DBConn();
			Statement stmt = connection.createStatement();
			
			ResultSet ret1 = null;
			*/
			int counter = 0;
			
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
			String sql = "select s.id, s.file_id, date_sub(s.sdate, interval 6 hour) sdate " +
					"from ts_search s join ts_search_flags sf on sf.search_id = s.id " +
					"join ts_property p on p.id = s.property_id " + 
					"where s.search_type = ? and year(s.sdate) = ? and month(s.sdate) = ? and sf.tsr_created = ? " +
					" and s.comm_id = ?  and p.state_id = ? " +
					"order by id ";		
			
			
			sql = "select s.*, c.name cname, sef.ati_file_reference " +
					"from ts_search s  " +
					"join ts_property p on s.property_id = p.id " +
					"join ts_county c on p.county_id = c.id " +
					"join ts_community_sites cs on c.id = cs.county_id " +
					"left join search_external_flags sef on sef.search_id = s.id " +
					"where c.state_id = 10  " +
					"and s.comm_id in (3,4,71) " +
					"and cs.community_id = 71 and cs.site_type = 58 " + 
					"and cs.enableStatus > 0 " +
					"and search_type = 10 " +
					"and (STR_TO_DATE('3/1/2013','%m/%d/%Y') <= sdate or STR_TO_DATE('3/1/2013','%m/%d/%Y') <= tsr_date) " +
					"and STR_TO_DATE('4/1/2013','%m/%d/%Y') >= sdate  " +
					"order by s.id " ;
			
			
			writer = new PrintWriter("D:\\bugs\\outUpdateRep1.csv");
			
			List<ResultRow> list = DBManager.getSimpleTemplate().query(sql, new ParameterizedRowMapper<ResultRow>() {

				@Override
				public ResultRow mapRow(ResultSet resultSet, int arg1) throws SQLException {
					ResultRow row = new ResultRow();
					row.id = resultSet.getLong("id");
					row.fileId = resultSet.getString("file_id");
					row.sdate = resultSet.getTimestamp("sdate");
					row.commId = resultSet.getInt("id");
					row.countyName = resultSet.getString("cname");
					try {
						row.atiFileReference = resultSet.getString("ati_file_reference");
					} catch (Exception e) {
					}
					return row;
				}
			});
//			}, 10, 2012, 2, 1, 71, 10);
			for (ResultRow resultRow : list) {
				
				counter ++;
				
				System.out.print(counter + ", " + resultRow.id + ", " + resultRow.fileId + ", " + sdf.format(resultRow.sdate) + ", "+ resultRow.countyName + ", "+ resultRow.commId + ", ");
				writer.append(counter + ", " + resultRow.id + ", " + resultRow.fileId + ", " + sdf.format(resultRow.sdate) + ", "+ resultRow.countyName + ", "+ resultRow.commId + ", ");
				
				try {
				
					Search searchFromDisk = SearchManager.getSearchFromDisk(resultRow.id);
					long parentSearchId = searchFromDisk.getParentSearchId();
	//				Search updateFromDisk = loadSearchFrom(DBManager.loadSearchDataFromDB(parentSearchId, false), parentSearchId); 
					//SearchManager.getSearchFromDisk(parentSearchId);
	//				while(updateFromDisk != null && updateFromDisk.isProductType(Products.UPDATE_PRODUCT)) {
	//					parentSearchId = updateFromDisk.getParentSearchId();
	//					//updateFromDisk = SearchManager.getSearchFromDisk(parentSearchId);
	//					updateFromDisk = loadSearchFrom(DBManager.loadSearchDataFromDB(parentSearchId, false), parentSearchId); 
	//				}
					
	//				if(updateFromDisk == null) {
	//					//System.out.println("nuuuuuuuuuuuuuull");
	//					DateType dateType = DBManager.getSimpleTemplate().queryForObject(SQL_LOAD_SEARCH_DATA_INFO, new ParameterizedRowMapper<DateType>() {
	//
	//						@Override
	//						public DateType mapRow(ResultSet arg0, int arg1) throws SQLException {
	//							DateType dt = new DateType();
	//							dt.sdate = arg0.getTimestamp("sdate");
	//							dt.type = arg0.getInt("search_type");
	//							return dt;
	//						}
	//					}, parentSearchId);
	//					
	//					
	//					System.out.println((dateType.type != 10) + ", " + sdf.format(dateType.sdate) + ", " + parentSearchId);
	//					writer.append((dateType.type != 10) + ", " + sdf.format(dateType.sdate) + ", " + parentSearchId);
	//					
	//					
	//				} else {
	//					System.out.println("true" + ", " + sdf.format(updateFromDisk.getTSROrderDate()) + ", " + parentSearchId);
	//					writer.append("true" + ", " + sdf.format(updateFromDisk.getTSROrderDate()) + ", " + parentSearchId);
	//				}
					
					String atiFileRef = searchFromDisk.getSa().getAtribute(SearchAttributes.ATIDS_FILE_REFERENCE_ID);
					
					boolean updateCreatedNew = (resultRow.id + "").equalsIgnoreCase(atiFileRef) || (atiFileRef != null && atiFileRef.matches("U\\d+"));
							
					
					System.out.println(searchFromDisk.getParentSearchId() + ", " + atiFileRef + ", " + updateCreatedNew);
					writer.append(searchFromDisk.getParentSearchId() + ", " + atiFileRef + ", " + updateCreatedNew);
				
				} catch (Exception e) {
					e.printStackTrace();
					writer.append(" " + ", " + " " + ", " + " ");
				}
				
				writer.append("\n");
				writer.flush();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(writer != null) {
				writer.close();
			}
		}
		
	}
	
	private static final String SQL_LOAD_SEARCH_DATA_INFO = "SELECT  date_sub(s.sdate, interval 6 hour) sdate " + 
	    	", search_type FROM " + 
	    	DBConstants.TABLE_SEARCH + " s JOIN " + 
			DBConstants.TABLE_SEARCH_FLAGS + " f ON s." + DBConstants.FIELD_SEARCH_ID + " = f." + DBConstants.FIELD_SEARCH_FLAGS_ID +  
			" WHERE s." + DBConstants.FIELD_SEARCH_ID + " = ?";
	
	public static Search loadSearchFrom(byte[] bytes, long searchId) {
		if(bytes == null) {
			return null;
		}
		Search search = null;
		String tsrFolder = DBManager.getSearchTSRFolder(searchId);
        
        XStreamManager xstream = XStreamManager.getInstance();
        try {
            
        	//REPLICATION
        	//check if context exists on disk, if not try to take it from database
        	String contextPath = tsrFolder + File.separator;
        	contextPath = contextPath .replace("//", "/");
        	//File contextPathFile = new File( tsrFolder + File.separator );
        	
        	ZipUtils.unzipContext( bytes, contextPath, searchId );
        	
        	File file =new File(tsrFolder + File.separator + "__search.xml");
        	
        	if( file.length()< 40 * 1024 * 1024){
        		String path = tsrFolder + File.separator + "__search.xml";
        		path = path.replaceAll("//", "/");
            	Reader inputReader = new FileReader(path);
                
                StringBuffer sb = new StringBuffer();
                char[] buffer= new char[1024];
    			while (true)
    			{
    				int bytes_read= inputReader.read(buffer);
    				if (bytes_read == -1)
    					break;
    				
    				sb.append(buffer, 0, bytes_read);
    			}
    			inputReader.close();
    				
				String searchString = sb.toString().replaceAll("/opt/resin/webapps/title-search/TSD", "/opt/TSD").replace( "<readyToProcess>false</readyToProcess>", "<readyToProcess>true</readyToProcess>" );
				
				search = (Search)xstream.fromXML( searchString );
    				
    			
        	}
			else{
				
				System.err.println("#####################################################");
				System.err.println("SIZE = bigger then " + 40 * 1024 * 1024+ " FILE_NAME = ");
				System.err.println("##########------- BLANK PAGE  -------########");
				System.err.println("#####################################################");
				
				return null;
			}                    
        } catch(Exception e) {
            e.printStackTrace();
        }        					
        return search;
	}
}
