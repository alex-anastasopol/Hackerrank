package ro.cst.tsearch.loadBalServ;

import static ro.cst.tsearch.utils.DBConstants.FIELD_SERVER_ALIAS;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SERVER_ENABLED;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SERVER_ID;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SERVER_IP_ADDRESS;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SERVER_IP_MASK;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SERVER_NAME;
import static ro.cst.tsearch.utils.DBConstants.FIELD_SERVER_PATH;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatNumber;

public class ServerInfoEntry implements ParameterizedRowMapper<ServerInfoEntry>{
	private String ip = "";
	private float loadFactor = -1;
	private Date timestamp = null;
	private boolean reliable = false;
	private boolean tryAgain = true;
	private int id = 0;
	private int ipMask = 32;
	private String hostName = "";
	private String alias = "";
	private int enabled = 0;
	private boolean checkSearchAccess = false;
	private String path = "";
	private String ipBackup = "";
	private static float maxAvailability = ServerInfoSingleton.getMaxAvailability();
	
	public String getIp() {
		if(ip==null)
			ip = "";
		return ip;
	}
	public String getShortIp() {
		String ip = getIp();
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
	public void setIp(String ip) {
		this.ip = ip;
	}
	public float getLoadFactor() {
		return loadFactor;
	}
	public void setLoadFactor(float loadFactor) {
		this.loadFactor = loadFactor;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public boolean isReliable() {
		return reliable;
	}
	public void setReliable(boolean reliable) {
		this.reliable = reliable;
	}
	public boolean isTryAgain() {
		return tryAgain;
	}
	public void setTryAgain(boolean tryAgain) {
		this.tryAgain = tryAgain;
	}
	
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	/**
	 * If the timestamp is still valid (now-seconds < timestamp)
	 * we shouldn't update the entry
	 * @param seconds
	 * @return
	 */
	public boolean beforeSeconds(int seconds) {
		Calendar now = Calendar.getInstance();
		now.add(Calendar.SECOND, -seconds);
		if(timestamp.after(now.getTime()))
			return false;
		return true;
	}
	public String getAlias() {
		if(alias==null)
			alias = "";
		return alias;
	}
	public void setAlias(String alias) {
		this.alias = alias;
	}
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	
	public int getIpMask() {
		return ipMask;
	}
	public void setIpMask(int ipMask) {
		this.ipMask = ipMask;
	}
	public int getEnabled() {
		return enabled;
	}
	public void setEnabled(int enabled) {
		this.enabled = enabled;
	}
	public String getPath() {
		if(path==null)
			path = "";
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ServerInfoEntry other = (ServerInfoEntry) obj;
		if (id != other.id)
			return false;
		return true;
	}
	private static FormatNumber formatNumber = new FormatNumber(FormatNumber.TWO_DECIMALS);
	public String getStatus(){
		if(loadFactor==LoadBalancingStatus.BAD_LOAD)
			return "Unavailable";
		else 
			return formatNumber.getNumber(loadFactor/maxAvailability);
	}
	
	private static SimpleDateFormat sdf1 = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	//private static SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z");
	//private static SimpleDateFormat sdf3 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	private static int gmtOffset = (TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings())/3600000;
	private static String gmtOffsetStr = new String(((gmtOffset>=0)?"+":"") + Integer.toString(gmtOffset));
	
	public String getTime(){
		try {
			
			if (timestamp == null) {
				return "&nbsp;";
			}
			//return (sdf1.format(sdf2.parse((sdf3.format(timestamp))+" GMT"))).toString();
			return sdf1.format(timestamp).toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "N/A";
		}
	}
	
	public static String getTimeZone(){
		return gmtOffsetStr;
	}
	public String getIpBackup() {
		return ipBackup;
	}
	public String getShortIpBackup() {
		String ip = getIpBackup();
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
	public void setIpBackup(String ipBackup) {
		this.ipBackup = ipBackup;
	}
	public boolean isCheckSearchAccess() {
		return checkSearchAccess;
	}
	public boolean getCheckSearchAccess() {
		return checkSearchAccess;
	}
	public void setCheckSearchAccess(boolean checkSearchAccess) {
		this.checkSearchAccess = checkSearchAccess;
	}
	@Override
	public ServerInfoEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
		ServerInfoEntry entry = new ServerInfoEntry();
        entry.setId(rs.getInt(FIELD_SERVER_ID));
        entry.setIp(rs.getString(FIELD_SERVER_IP_ADDRESS));
        entry.setEnabled(rs.getInt(FIELD_SERVER_ENABLED));
        entry.setCheckSearchAccess(rs.getBoolean(DBConstants.FIELD_SERVER_CHECK_SEARCH_ACCESS));
        entry.setHostName(rs.getString(FIELD_SERVER_NAME));
        entry.setAlias(rs.getString(FIELD_SERVER_ALIAS));
        entry.setIpMask(rs.getInt(FIELD_SERVER_IP_MASK));
        entry.setPath(rs.getString(FIELD_SERVER_PATH));
        return entry;
	}
	
	
}

