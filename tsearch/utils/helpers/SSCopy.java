package ro.cst.tsearch.utils.helpers;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.LinkedHashSet;
import java.util.Set;

import ro.cst.tsearch.searchsites.client.GWTDataSite;

public class SSCopy {

	private String sourceServerName = null;
	private Set<String> countyNames = new LinkedHashSet<String>();
	private String stateAbv = null;
	private int siteType = -1;
	private long enableStatus = 0;
	private long defaultEnableStatus = 0;
	private Set<Integer> commIds = new LinkedHashSet<Integer>();
	public String getSourceServerName() {
		return sourceServerName;
	}
	public void setSourceServerName(String sourceServerName) {
		this.sourceServerName = sourceServerName;
	}
	public Set<String> getCountyNames() {
		return countyNames;
	}
	public void setCountyNames(Set<String> countyNames) {
		this.countyNames = countyNames;
	}
	public String getStateAbv() {
		return stateAbv;
	}
	public void setStateAbv(String stateAbv) {
		this.stateAbv = stateAbv;
	}
	public int getSiteType() {
		return siteType;
	}
	public void setSiteType(int siteType) {
		this.siteType = siteType;
	}	
	public long getEnableStatus() {
		return enableStatus;
	}
	public void setEnableStatus(long enableStatus) {
		this.enableStatus = enableStatus;
	}
	public long getDefaultEnableStatus() {
		return defaultEnableStatus;
	}
	public void setDefaultEnableStatus(long defaultEnableStatus) {
		this.defaultEnableStatus = defaultEnableStatus;
	}
	public Set<Integer> getCommIds() {
		return commIds;
	}
	public void setCommIds(Set<Integer> commIds) {
		this.commIds = commIds;
	}
	public static void main(String[] args) {
		SSCopy copy = new SSCopy();
		
		copy.getCountyNames().add("Brazoria");
		copy.getCountyNames().add("Harris");
		copy.getCountyNames().add("Fort Bend");
		copy.getCountyNames().add("Galveston");
		copy.getCountyNames().add("Liberty");
		
		
		copy.setStateAbv("TX");
		copy.setSiteType(GWTDataSite.ADI_TYPE);
		copy.setSourceServerName("local");
		
		copy.setDefaultEnableStatus(65519);
		//copy.setEnableStatus(65519);
		
		copy.getCommIds().add(3);
		copy.getCommIds().add(4);
		copy.setEnableStatus(300);
		
		System.out.println(copy.getInsertSql());
	}
	
	public String getInsertSql(){
		
		Connection conn = null;
		Statement stm = null;
		try {
			conn = SyncTool.getConnectionServer(sourceServerName);
			stm = conn.createStatement();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(conn == null || stm == null) {
			throw new RuntimeException("Could not initialize connection to " + sourceServerName);
		}
		
		String countyName = "";
		
		if(countyNames != null && !countyNames.isEmpty()) {
			
			for (String cnt : countyNames) {
				if(countyName.isEmpty()) {
					countyName = " and c.name in (\"" + cnt + "\"";		
				} else {
					countyName += ", \"" + cnt + "\"";
				}
			}
			
			countyName += " ) ";
		}
		
		
		String sql = "select distinct s.* " +
				"from ts_sites s " +
				"join ts_county c on s.id_county = c.id " +
				"join ts_state st ON c.state_id = st.id " +
				" where 1=1 " + countyName + 
				" and st.stateabv = \"" + stateAbv + 
				"\" and s.site_type = \"" + siteType + "\"";
		
		StringBuilder result = new StringBuilder();
		
		String p2 = null;
		
		try {
			ResultSet rs = stm.executeQuery(sql);
			
			while(rs.next()) {
			
				result.append("Insert into ts_sites values (");
				
				ResultSetMetaData metaData = rs.getMetaData();
				
				for (int i = 1; i <= metaData.getColumnCount(); i++) {
					
					if(i > 1) {
						result.append(", ");
					}
					
					switch (metaData.getColumnType(i)) {
					case Types.VARCHAR:
						String string = rs.getString(i);
						if(string == null) {
							result.append("null");
						} else {
							result.append("\"").append(string).append("\"");
						}
						break;
					case Types.DATE:
						Date date = rs.getDate(i);
						if(date == null) {
							result.append("null");
						} else {
							System.err.println("Date column " + i + " name " + metaData.getColumnName(i));
						}
						break;
					case Types.BIGINT:
                    case Types.BIT:
                    case Types.BOOLEAN:
                    case Types.DECIMAL:
                    case Types.DOUBLE:
                    case Types.FLOAT:
                    case Types.INTEGER:
                    case Types.SMALLINT:
                    case Types.TINYINT:
                    	result.append(rs.getString(i));
                    	break;
					default:
						System.err.println("Unknown column type for column " + i + " name " + metaData.getColumnName(i) + " type " + metaData.getColumnType(i));
						break;
					}
					
					if("P2".equals(metaData.getColumnName(i))) {
						p2 = rs.getString(i);
					}
					
				}
				
				result.append(");\n");
				
				long countyId = rs.getLong("id_county");
				
				result.append("INSERT into ts_community_sites SELECT comm.comm_id, ").append(countyId).append(", ")
					.append(siteType).append(", ")
					.append(p2).append(", ")
					.append(defaultEnableStatus)
					.append(" FROM ts_community comm;\n");
				if(commIds != null && !commIds.isEmpty()) {
					
					String commId = "";
					
					for (Integer cnt : commIds) {
						if(commId.isEmpty()) {
							commId = " and community_id in (" + cnt;		
						} else {
							commId += ", " + cnt;
						}
					}
					
					commId += ") ";
					
					result.append("update ts_community_sites set enableStatus = ")
						.append(enableStatus)
						.append(" where site_type = ")
						.append(siteType)
						.append(commId)
						.append("and county_id = ").append(countyId).append(";\n");
					
					
				}
				result.append("\n");
				
			}
			
			
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		
		
		
		return result.toString();
	}
	
}
