package ro.cst.tsearch.utils.helpers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ro.cst.tsearch.searchsites.client.GWTDataSite;

public class SitePositioner {

	private String serverName = null;
	private List<Integer> justBefore = null;
	private List<Integer> justAfter = null;
	
	public int positionSite() {
		return -1;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SitePositioner positioner = new SitePositioner();
		List<Integer> justBefore = new ArrayList<Integer>();
		justBefore.add(GWTDataSite.DT_TYPE);
		positioner.setJustBefore(justBefore);
		positioner.setServerName("ats01db");
		positioner.positionSite(GWTDataSite.ATI_TYPE);
		
	}
	
	private class SmallDataSite {
		private int countyId;
		private int siteType;
		private int p2;
		private int autoPos;
		
		public String getUpdatePosSql(int pos) {
			return "update ts_sites set automatic_position = " + pos + " where id_county  = "  + countyId + " and site_type = " + siteType + " and P2 = " + p2;
 		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + countyId;
			result = prime * result + p2;
			result = prime * result + siteType;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SmallDataSite other = (SmallDataSite) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (countyId != other.countyId)
				return false;
			if (p2 != other.p2)
				return false;
			if (siteType != other.siteType)
				return false;
			return true;
		}

		private SitePositioner getOuterType() {
			return SitePositioner.this;
		}
		
	}


	private boolean positionSite(int siteType) {
		Connection conn = null;
		Statement stm = null;
		try {
			conn = SyncTool.getConnectionServer(serverName);
			stm = conn.createStatement();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(conn == null || stm == null) {
			throw new RuntimeException("Could not initialize connection to " + serverName);
		}
		
		HashMap<Integer, List<SmallDataSite>> hashedData = new HashMap<Integer, List<SmallDataSite>>();
		
		try {
			ResultSet rs = stm.executeQuery("select * from ts_sites order by id_county, automatic_position, site_type, P2");
			while(rs.next()) {
				SmallDataSite sds = new SmallDataSite();
				
				sds.p2 = rs.getInt("P2");
				sds.countyId = rs.getInt("id_county");
				sds.siteType = rs.getInt("site_type");
				sds.autoPos = rs.getInt("automatic_position");
				
				List<SmallDataSite> listData = hashedData.get(sds.countyId); 
				if(listData == null) {
					listData = new ArrayList<SitePositioner.SmallDataSite>();
					hashedData.put(sds.countyId, listData);
				}
				if(!listData.contains(sds)) {
					listData.add(sds);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		List<String> sqlToUpdate = new ArrayList<String>();
		
		for (List<SmallDataSite> listData : hashedData.values()) {
			
			SmallDataSite mySds = null;
			for (int i = 0; i < listData.size(); i++) {
				SmallDataSite sds = listData.get(i);
				if(sds.siteType == siteType) {
					mySds = sds;
					break;
				}
			}
			
			if(mySds == null) {	//my site is not in this county -> do nothing
				continue;		//go to next county
			}
			int currentPos = -1;
			for (Integer justBeforeSiteType : justBefore) {
				boolean added = false;
				for (int i = 0; i < listData.size(); i++) {
					SmallDataSite sds = listData.get(i);
					if(sds.siteType == justBeforeSiteType) {
						currentPos = sds.autoPos;
						added = true;
						sqlToUpdate.add(mySds.getUpdatePosSql(currentPos));
						sqlToUpdate.add(sds.getUpdatePosSql(sds.autoPos + 1));	//move it just one spot
					} else if(currentPos >= 0){
						if(mySds.siteType == sds.siteType && added) {
							
						} else {
						//this means I found the exact spot and I need to move all other site to the end
							sqlToUpdate.add(sds.getUpdatePosSql(sds.autoPos + 1));	//move it just one spot
						}
					}
				}
				if(currentPos >= 0) {
					//found correct position, i don't care about other possibilities
					break;
				}
			}
			if(currentPos == -1) {
				//couldn't find a correct spot, add it last
				//let's check if there 
				
				SmallDataSite sdsLast = listData.get(listData.size() - 1);
				sqlToUpdate.add(mySds.getUpdatePosSql(sdsLast.autoPos + 1));
			}
		}
		System.out.println("I will execute " + sqlToUpdate.size() + " updates ... REALLY????? OMG!");
		
		try {	
			for (String sql : sqlToUpdate) {
//				stm.addBatch(sql);
				System.out.println(sql + ";");
			}
//			stm.executeBatch();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}


	public List<Integer> getJustBefore() {
		return justBefore;
	}


	public void setJustBefore(List<Integer> justBefore) {
		this.justBefore = justBefore;
	}


	public List<Integer> getJustAfter() {
		return justAfter;
	}


	public void setJustAfter(List<Integer> justAfter) {
		this.justAfter = justAfter;
	}


	public String getServerName() {
		return serverName;
	}


	public void setServerName(String serverName) {
		this.serverName = serverName;
	}
	
	

}
