package ro.cst.tsearch.loadBalServ;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;

public class ServerSourceEntry implements ParameterizedRowMapper<ServerSourceEntry>{
	private int id = -1;
	private String address = "";
	private int netmask = 32;
	private int enable = 1;
	private String redirectAddress = "";
	private String clientUsername = "";
	private String clientCommname = "";
	
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public int getEnable() {
		return enable;
	}
	public void setEnable(int enable) {
		this.enable = enable;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getNetmask() {
		return netmask;
	}
	public void setNetmask(int netmask) {
		this.netmask = netmask;
	}
	public String getRedirectAddress() {
		/*String ip = redirectAddress;
		int pos1 = ip.indexOf("://");
		if(pos1>0){
			//this means that we have http:// in front of the ip
			ip = ip.substring(pos1 + 3);
		}
		pos1 = ip.indexOf(":");	//this means that we have port after the address
		if(pos1>0){
			ip = ip.substring(0,pos1);
		}*/
		return redirectAddress;
	}
	public void setRedirectAddress(String redirectAddress) {
		this.redirectAddress = redirectAddress;
	}
	public String getClientCommname() {
		if(clientCommname==null)
			return "";
		return clientCommname;
	}
	public void setClientCommname(String clientCommname) {
		this.clientCommname = clientCommname;
	}
	public String getClientUsername() {
		if(clientUsername==null)
			return "";
		return clientUsername;
	}
	public void setClientUsername(String clientUsername) {
		this.clientUsername = clientUsername;
	}
	public String getChecked(){
		if(getEnable()!=0)
			return "checked";
		return "";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ServerSourceEntry other = (ServerSourceEntry) obj;
		if (id != other.id)
			return false;
		return true;
	}
	@Override
	public ServerSourceEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
		ServerSourceEntry sse = new ServerSourceEntry();
		sse.setId(rs.getInt(DBConstants.FIELD_LBS_SOURCES_ID));
		sse.setAddress(rs.getString(DBConstants.FIELD_LBS_SOURCES_ADDRESS));
		sse.setNetmask(rs.getInt(DBConstants.FIELD_LBS_SOURCES_NETMASK));
		sse.setEnable(rs.getInt(DBConstants.FIELD_LBS_SOURCES_ENABLE));
		sse.setRedirectAddress(rs.getString(DBConstants.FIELD_LBS_SOURCES_REDIRECT_ADDRESS));
		return sse;
	}
	
}
