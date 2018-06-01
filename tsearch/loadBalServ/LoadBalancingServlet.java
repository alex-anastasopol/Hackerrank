package ro.cst.tsearch.loadBalServ;

import java.net.InetAddress;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Category;

import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;

public class LoadBalancingServlet extends BaseServlet {
	private static final Category logger = Category.getInstance(LoadBalancingServlet.class.getName());
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void doRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpSession session = request.getSession(true);
		User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		
		String address = URLMaping.loadBalancingPage;
		int opCode = -1;
		try {
			opCode = Integer.parseInt(request.getParameter(TSOpCode.OPCODE));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String idList = request.getParameter( RequestParams.INVOICE_LIST_CHK );
		if(opCode==TSOpCode.LBS_ADD_SOURCE){
			String client = request.getParameter(RequestParams.LBS_SOURCE_CLIENT);
			String community = request.getParameter(RequestParams.LBS_SOURCE_COMMNAME);
			String sourceIp = request.getParameter(RequestParams.LBS_SOURCE_IP_SRC);
			String destIp = request.getParameter(RequestParams.LBS_SOURCE_IP_DEST);
			String enabled = request.getParameter(RequestParams.LBS_SOURCE_ENABLE);
			String sourceNetmask = request.getParameter(RequestParams.LBS_SOURCE_IP_MASK);	
			try {
				int mask = Integer.parseInt(sourceNetmask);
				if(mask<0 || mask>32)
					sourceNetmask = "32";
			}catch (Exception e) {
				logger.error("Netmask not recognized: " + sourceNetmask);
				logger.error("Using default value for mask: 32");
				e.printStackTrace();
				sourceNetmask = "32";
			}
			DBConnection conn = null;
			String sql = null;
			
			
				
			sql = "INSERT INTO " + DBConstants.TABLE_LBS_SOURCES + " ( " + 
				DBConstants.FIELD_LBS_SOURCES_ADDRESS + ", " + 
				DBConstants.FIELD_LBS_SOURCES_NETMASK + ", " + 
				DBConstants.FIELD_LBS_SOURCES_REDIRECT_ADDRESS + ", " + 
				DBConstants.FIELD_LBS_SOURCES_SERVER_NAME + ", " +
				((client==null)?"":(DBConstants.FIELD_LBS_SOURCES_CLNT_USERNAME + ", ")) + 
				((community==null)?"":(DBConstants.FIELD_LBS_SOURCES_CLNT_COMMNAME + ", ")) + 
				DBConstants.FIELD_LBS_SOURCES_ENABLE + ") VALUES ( ?, ?, ?, ?, ?, ?, ? ) ";
					
				try {
					logger.info("Add Source: " + sql);
					
					conn  = ConnectionPool.getInstance().requestConnection();
					
					PreparedStatement pstmt = conn.prepareStatement( sql );
					
					pstmt.setString( 1, sourceIp);
					pstmt.setInt( 2, Integer.parseInt(sourceNetmask) );
					pstmt.setString( 3, destIp);
					pstmt.setString( 4, URLMaping.INSTANCE_DIR);
					pstmt.setString( 5, ((client==null)?"":client));
					pstmt.setString( 6, ((community==null)?"":community));
					pstmt.setInt( 7, Integer.parseInt(((enabled==null)?"0":"1")) );

					
					pstmt.executeUpdate();
					pstmt.close();
					
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						ConnectionPool.getInstance().releaseConnection(conn);
					} catch (BaseException e) {
						logger.error(e);
					}
				}
		} else if(opCode==TSOpCode.LBS_DELETE_SOURCE){
			if(idList.length()>0){
				DBConnection conn = null;
				String sql = "DELETE FROM " + DBConstants.TABLE_LBS_SOURCES + 
					" WHERE " + DBConstants.FIELD_LBS_SOURCES_ID + 
					" IN (" + StringUtils.makeValidNumberList(idList) + ")";
				try {
					conn  = ConnectionPool.getInstance().requestConnection();
					logger.info("Delete Sources: " + sql);
					conn.executeSQL(sql);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						ConnectionPool.getInstance().releaseConnection(conn);
					} catch (BaseException e) {
						logger.error(e);
					}
				}
			}
		} else if(opCode==TSOpCode.LBS_UPDATE_SOURCE){
			doUpdateQueries(request);
		} else if(opCode==TSOpCode.LBS_SET_DEFAULT_SOURCE){
			String defaultDestination = request.getParameter(RequestParams.LBS_SOURCE_DEFAULT_DEST);
			boolean ok = true;
			try {
				InetAddress.getByName(getShortIp(defaultDestination));
			} catch (Exception e) {
				ok = false;
			}
			if(ok){
				ServerInfoSingleton.getInstance().setDefaultDestination(defaultDestination);
				String sql = "UPDATE " + DBConstants.TABLE_SERVER + " SET " +
					DBConstants.FIELD_SERVER_DEFAULT + " = ? ";
				
				try {
					DBManager.getSimpleTemplate().update(sql,defaultDestination);
					logger.info("Set default address: " + sql);
				} catch (Exception e) {
					e.printStackTrace();
					 logger.error(e);
				}

			} else {
				request.setAttribute("ret", 
						"Address: " + defaultDestination + 
						" is not valid. Please enter a valid ip address.");
			}
			
		} else if(opCode==TSOpCode.LBS_ADD_SERVER){
			address = URLMaping.ATSInstances;
			String idServer = request.getParameter(RequestParams.LBS_SERVER_ID);
			String ipAddress = request.getParameter(RequestParams.LBS_SERVER_IP_ADDR);
			String ipMask = request.getParameter(RequestParams.LBS_SERVER_IP_MASK);
			String hostName = request.getParameter(RequestParams.LBS_SERVER_HOST_NAME);
			String alias = request.getParameter(RequestParams.LBS_SERVER_ALIAS);
			String enable = request.getParameter(RequestParams.LBS_SERVER_ENABLE);
			String checkSearchAccess = request.getParameter(RequestParams.LBS_SERVER_CHECK_SEARCH_ACCESS);
			String path = request.getParameter(RequestParams.LBS_SERVER_PATH);
			boolean ok = true;
			try {
				Integer.parseInt(idServer);	//check to see if it is an integer
			} catch (Exception e) {
				e.printStackTrace();
				ok = false;
			}
			
			if(ok){
				ServerInfoSingleton serverInfo = ServerInfoSingleton.getInstance();
				
				if(ipMask==null || ipMask.length()==0)
					ipMask = "32";
				else {
					try {
						int maskInt = Integer.parseInt(ipMask);
						if(maskInt<=0 || maskInt>32)
							ipMask = "32";
					} catch (Exception e) {
						ipMask = "32";
					}
				}
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DATE, -1);
				String sql = "INSERT INTO " + DBConstants.TABLE_SERVER + " ( " +
					DBConstants.FIELD_SERVER_ID + ", " +
					DBConstants.FIELD_SERVER_IP_ADDRESS + ", " + 
					DBConstants.FIELD_SERVER_IP_MASK + ", " + 
					DBConstants.FIELD_SERVER_NAME + ", " + 
					DBConstants.FIELD_SERVER_ALIAS + ", " +
					DBConstants.FIELD_SERVER_ENABLED + ", "  +
					DBConstants.FIELD_SERVER_CHECK_SEARCH_ACCESS + ", "  +
					DBConstants.FIELD_SERVER_TIMESTAMP + ", " +
					DBConstants.FIELD_SERVER_PATH + ", " +
					DBConstants.FIELD_SERVER_DEFAULT + " " + 
					" ) VALUES ( ?, ?, ?, ?, ?, ?, ?, str_to_date( '" + 
						new FormatDate(FormatDate.TIMESTAMP).getDate(cal.getTime()) + "' , '" +  
						FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' ) " + ", ?, ? )"; 

				ServerInfoEntry sie = new ServerInfoEntry();
				sie.setAlias(alias);
				sie.setEnabled(((enable==null)?0:1));
				sie.setCheckSearchAccess(((checkSearchAccess!=null)));
				sie.setHostName(hostName);
				sie.setIp(ipAddress);
				sie.setTimestamp(cal.getTime());
				sie.setIpMask(Integer.parseInt(ipMask));
				sie.setTryAgain(true);
				sie.setPath(path);
				DBConnection conn = null;
				try {
					conn  = ConnectionPool.getInstance().requestConnection();
					logger.info("Add ATS instance: " + sql);
					
					PreparedStatement pstmt = conn.prepareStatement( sql );
					
					pstmt.setInt( 1, Integer.parseInt(idServer));
					pstmt.setString( 2, ipAddress);
					pstmt.setInt( 3, Integer.parseInt(ipMask));
					pstmt.setString( 4, hostName);
					pstmt.setString( 5, ((alias==null)?"":alias) );
					pstmt.setInt( 6, ((enable==null)?0:1));
					pstmt.setBoolean( 7, checkSearchAccess!=null);
					pstmt.setString( 8, path);
					pstmt.setString( 9, serverInfo.getDefaultDestination());
					
					pstmt.executeUpdate();
					pstmt.close();
					
					sie.setId((int)DBManager.getLastId(conn));
					if(LBNotification.getEnable())
						LBNotification.sendNotification(LBNotification.SERVER_ADD, currentUser.getUserAttributes(), sie);
				} catch (Exception e) {
					e.printStackTrace();
					request.setAttribute("ret", 
							"There was a problem with your data, please check it again.");
				} finally {
					try {
						ConnectionPool.getInstance().releaseConnection(conn);
					} catch (BaseException e) {
						logger.error(e);
					}
				}
			} else {
				request.setAttribute("ret", 
					"ID Server: " + idServer + 
					" is not valid. Please enter a valid id.");
			}
			
		} else if(opCode==TSOpCode.LBS_UPDATE_SERVER){
			address = URLMaping.ATSInstances;
			Vector<ServerInfoEntry> serversUpdates = new Vector<ServerInfoEntry>();
			
			try {
				int nr = doServersUpdateQueries(request, serversUpdates);
				if(LBNotification.getEnable() && nr>0){
					LBNotification.sendNotification(LBNotification.SERVER_UPDATE, currentUser.getUserAttributes(), serversUpdates);
				}
			}catch(Exception e) {
				e.printStackTrace();
				logger.error(e);
			}
			
		} else if(opCode==TSOpCode.LBS_DELETE_SERVER){
			address = URLMaping.ATSInstances;
			if(idList.length()>0){
				DBConnection conn = null;
				String sql = "DELETE FROM " + DBConstants.TABLE_SERVER + 
					" WHERE " + DBConstants.FIELD_SERVER_ID + 
					" IN (" + StringUtils.makeValidNumberList(idList) + ")";
				try {
					conn  = ConnectionPool.getInstance().requestConnection();
					logger.info("Delete ATS Server: " + sql);
					conn.executeSQL(sql);
										
					if(LBNotification.getEnable()){
						Vector<String> deleted = new Vector<String>();
						String[] ids = idList.split(",");
						for (int i = 0; i < ids.length; i++) {
							String hostName = RequestParams.LBS_SERVER_FIELD_HOST_NAME + "_" + ids[i];
							hostName = request.getParameter(hostName);
							deleted.add(hostName);
						}
						LBNotification.sendNotification(LBNotification.SERVER_DEL, currentUser.getUserAttributes(), deleted);
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						ConnectionPool.getInstance().releaseConnection(conn);
					} catch (BaseException e) {
						logger.error(e);
					}
				}
			}
		} else if(opCode==TSOpCode.LBS_ENABLE_LOAD){
			DBConnection conn = null;
			String temp = ""; 
			ServerInfoSingleton sis = ServerInfoSingleton.getInstance();
			if(sis.isEnabledLoadAlgorithm())
				temp += "0";
			else 
				temp += "1";
			
			String sql = "UPDATE " + DBConstants.TABLE_CONFIGS + " SET "  +
				DBConstants.FIELD_CONFIGS_VALUE + " = " + temp + " WHERE " +
				DBConstants.FIELD_CONFIGS_NAME + " = " + "\"lbs.enable.load.alg\"";
			try {
				conn  = ConnectionPool.getInstance().requestConnection();
				logger.info("LBS_ENABLE_LOAD algorithm: " + sql);
				int nr = conn.executeUpdate(sql);
				if(nr==0){
					sql = "INSERT INTO " + DBConstants.TABLE_CONFIGS + " (" + 
						DBConstants.FIELD_CONFIGS_NAME + ", " + 
						DBConstants.FIELD_CONFIGS_VALUE + ") VALUES (" + 
						"\"lbs.enable.load.alg\", " + temp +")";
					conn.executeUpdate(sql);
				}
				sis.setEnabledLoadAlgorithm(!sis.isEnabledLoadAlgorithm());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					ConnectionPool.getInstance().releaseConnection(conn);
				} catch (BaseException e) {
					logger.error(e);
				}
			}

		} else if(opCode==TSOpCode.LBS_ENABLE_SOURCE){
			String temp = ""; 
			ServerInfoSingleton sis = ServerInfoSingleton.getInstance();
			if(sis.isEnabledSourceAlgorithm())
				temp += "0";
			else 
				temp += "1";
			
			DBConnection conn = null;
			String sql = "UPDATE " + DBConstants.TABLE_CONFIGS + " SET "  +
				DBConstants.FIELD_CONFIGS_VALUE + " = " + temp + " WHERE " +
				DBConstants.FIELD_CONFIGS_NAME + " = " + "\"lbs.enable.source.alg\"";
			try {
				conn  = ConnectionPool.getInstance().requestConnection();
				logger.info("LBS_ENABLE Source algorithm: " + sql);
				int nr = conn.executeUpdate(sql);
				if(nr==0){
					sql = "INSERT INTO " + DBConstants.TABLE_CONFIGS + " (" + 
						DBConstants.FIELD_CONFIGS_NAME + ", " + 
						DBConstants.FIELD_CONFIGS_VALUE + ") VALUES (" + 
						"\"lbs.enable.source.alg\", " + temp + ")";
					conn.executeUpdate(sql);
				}
				sis.setEnabledSourceAlgorithm(!sis.isEnabledSourceAlgorithm());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					ConnectionPool.getInstance().releaseConnection(conn);
				} catch (BaseException e) {
					logger.error(e);
				}
			}
		} else if(opCode==TSOpCode.LBS_SET_DEFAULT_EMAIL){
			address = URLMaping.ATSInstances;
			String emails = request.getParameter(RequestParams.LBS_SOURCE_NOTIF_EMAILS);
			LBNotification.setEmails(emails);
		} else if(opCode==TSOpCode.LBS_ENABLE_NOTIFICATION){
			address = URLMaping.ATSInstances;
			LBNotification.changeNotificationStatus();
		} else if(opCode==TSOpCode.LBS_ENABLE_OVERRIDE_DESTINATION){
			String sqls = "UPDATE " + DBConstants.TABLE_SERVER + " SET "  +
				DBConstants.FIELD_SERVER_OVERRIDE_DESTINATION + " =? ";
			LoadBalancingStatus loadBalancingStatus = new LoadBalancingStatus();
			DBManager.getSimpleTemplate().update(sqls,((loadBalancingStatus.isEnableOverrideDestination())?"0":"1"));
		} else {
			
		}
		
		forward(request, response, address);
		
	}

	private int doServersUpdateQueries(HttpServletRequest request, Vector<ServerInfoEntry> serversUpdates) throws Exception{
		String idList = request.getParameter( RequestParams.INVOICE_LIST_CHK );
		int nr = 0;
		if(idList==null || idList.length()==0)
			return 0;
		String[] ids = idList.split(",");
		for (int i = 0; i < ids.length; i++) {
			String idServer = RequestParams.LBS_SERVER_FIELD_ID + "_" + ids[i];
			idServer = request.getParameter(idServer);
			
			String ipAddress = RequestParams.LBS_SERVER_FIELD_IP_ADDR + "_" + ids[i];
			ipAddress = request.getParameter(ipAddress);
			
			String ipMask = RequestParams.LBS_SERVER_FIELD_IP_MASK + "_" + ids[i];
			ipMask = request.getParameter(ipMask);
			
			String hostName = RequestParams.LBS_SERVER_FIELD_HOST_NAME + "_" + ids[i];
			hostName = request.getParameter(hostName);
			
			String alias = RequestParams.LBS_SERVER_FIELD_ALIAS + "_" + ids[i];
			alias = request.getParameter(alias);
			
			String enable = RequestParams.LBS_SERVER_FIELD_ENABLE + "_" + ids[i];
			enable = request.getParameter(enable);
			
			String checkServer = RequestParams.LBS_SERVER_FIELD_CHECK_SEARCH_ACCESS + "_" + ids[i];
			checkServer = request.getParameter(checkServer);
			
			String path = RequestParams.LBS_SERVER_FIELD_PATH + "_" + ids[i];
			path = request.getParameter(path);
			
			try{
				Integer.parseInt(idServer);
				if(Integer.parseInt(ipMask)<0 || Integer.parseInt(ipMask)>32){
					ipMask = "32";
				}
			} catch (Exception e) {
				continue;
			}
			
			String sqls = "UPDATE " + DBConstants.TABLE_SERVER + " SET "  +
				DBConstants.FIELD_SERVER_ID + " =?, " +
				DBConstants.FIELD_SERVER_ALIAS + " =?, "+ 
				DBConstants.FIELD_SERVER_IP_ADDRESS + " =?, "+
				DBConstants.FIELD_SERVER_NAME + " =?, " +
				DBConstants.FIELD_SERVER_PATH + " =?, " +
				DBConstants.FIELD_SERVER_ENABLED + " =?, " +
				DBConstants.FIELD_SERVER_CHECK_SEARCH_ACCESS + " =?, " + 
				DBConstants.FIELD_SERVER_IP_MASK + " =? " +  
				" WHERE " + DBConstants.FIELD_SERVER_ID + "=?";
			
			ServerInfoEntry sie = new ServerInfoEntry();
			sie.setId(Integer.parseInt(ids[i]));
			sie.setAlias(alias);
			sie.setEnabled(((enable==null)?0:1));
			sie.setCheckSearchAccess(checkServer!=null);
			sie.setHostName(hostName);
			sie.setIp(ipAddress);
			sie.setIpMask(Integer.parseInt(ipMask));
			sie.setTryAgain(true);
			serversUpdates.add(sie);
			
			DBManager.getSimpleTemplate().update(sqls,idServer,alias,ipAddress,hostName,path,((enable==null)?"0":"1"),checkServer!=null,ipMask,ids[i]);

			logger.info("Update ATS Servers: " + sqls);

			nr++;
		}
		return nr;
	}
	
	
	

	/**
	 * Executes the sql queries that will update the selected entries
	 * @param request
	 * @param sourceUpdates 
	 * @return
	 */
	private void doUpdateQueries(HttpServletRequest request) {
		String idList = request.getParameter( RequestParams.INVOICE_LIST_CHK );
		if(idList==null || idList.length()==0)
			return;
		String[] ids = idList.split(",");
		
		for (int i = 0; i < ids.length; i++) {
			String address = RequestParams.LBS_SOURCE_ADDRESS + "_" + ids[i];
			address = request.getParameter(address);
			
			String clientUserName = RequestParams.LBS_SOURCE_CLNT_USERNAME + "_" + ids[i];
			clientUserName = request.getParameter(clientUserName);
			
			String clientCommName = RequestParams.LBS_SOURCE_CLNT_COMMNAME + "_" + ids[i];
			clientCommName = request.getParameter(clientCommName);
			
			String redirIp = RequestParams.LBS_SOURCE_REDIRADDRESS + "_" + ids[i];
			redirIp = request.getParameter(redirIp);
			
			String netmask = RequestParams.LBS_SOURCE_NETMASK + "_" + ids[i];
			netmask = request.getParameter(netmask);
			
			String enable = RequestParams.LBS_SOURCE_ENBL + "_" + ids[i];
			enable = request.getParameter(enable);
			
			String sqls = "UPDATE " + DBConstants.TABLE_LBS_SOURCES + " SET "  +
				DBConstants.FIELD_LBS_SOURCES_ADDRESS + " =?, " + 
				DBConstants.FIELD_LBS_SOURCES_CLNT_COMMNAME + " =?, " +
				DBConstants.FIELD_LBS_SOURCES_CLNT_USERNAME + " =?, " +
				DBConstants.FIELD_LBS_SOURCES_ENABLE + " =?, " + 
				DBConstants.FIELD_LBS_SOURCES_NETMASK + " =?, " +
				DBConstants.FIELD_LBS_SOURCES_REDIRECT_ADDRESS + " =?, " + 
				DBConstants.FIELD_LBS_SOURCES_SERVER_NAME + " =? " +
				" WHERE " + DBConstants.FIELD_LBS_SOURCES_ID + "=? ";
			
			try {
				DBManager.getSimpleTemplate().update(sqls,address,clientCommName,clientUserName,
																			((enable==null)?"0":"1"),netmask,redirIp,URLMaping.INSTANCE_DIR,ids[i]);
			} catch (Exception e) {
				e.printStackTrace();
				 logger.error(e);
				 continue;
			}
			logger.info("Update Sources: " + sqls);
		}		
	}

	public String getShortIp(String ip) {
		int pos1 = ip.indexOf("://");
		if(pos1>0){
			//this means that we have http:// in front of the ip
			ip = ip.substring(pos1 + 3);
		}
		pos1 = ip.indexOf(":");	//this means that we have port after the address
		if(pos1>0){
			ip = ip.substring(0,pos1);
		}
		return ip;
	}
	
	
}
