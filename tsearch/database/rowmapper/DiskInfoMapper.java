package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;

public class DiskInfoMapper implements ParameterizedRowMapper<DiskInfoMapper> {
	
	private Float diskPercent;
	private Integer type;
	private String diskName;
	private Float diskMaxSpace;
	private Date timestamp;
	public Float getDiskPercent() {
		return diskPercent;
	}
	public void setDiskPercent(Float diskPercent) {
		this.diskPercent = diskPercent;
	}
	public Integer getType() {
		return type;
	}
	public void setType(Integer type) {
		this.type = type;
	}
	public String getDiskName() {
		return diskName;
	}
	public void setDiskName(String diskName) {
		this.diskName = diskName;
	}
	public Float getDiskMaxSpace() {
		return diskMaxSpace;
	}
	public void setDiskMaxSpace(Float diskMaxSpace) {
		this.diskMaxSpace = diskMaxSpace;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	@Override
	public DiskInfoMapper mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		DiskInfoMapper diskInfoMapper = new DiskInfoMapper();
		diskInfoMapper.setType(resultSet.getInt(DBConstants.FIELD_USAGE_DISK_TYPE));
		diskInfoMapper.setDiskMaxSpace(resultSet.getFloat(DBConstants.FIELD_USAGE_DISK_MAX_VALUE));
		diskInfoMapper.setDiskName(resultSet.getString(DBConstants.FIELD_USAGE_DISK_NAME));
		diskInfoMapper.setDiskPercent(resultSet.getFloat(DBConstants.FIELD_USAGE_DISK_DISK1));
		diskInfoMapper.setTimestamp(resultSet.getTimestamp(DBConstants.FIELD_USAGE_DISK_TIMESTAMP));
		return diskInfoMapper;
	}

}
