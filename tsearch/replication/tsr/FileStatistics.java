package ro.cst.tsearch.replication.tsr;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

/**
 * Immutable class holding file statistics info
 * @author radu bacrau
 */
public class FileStatistics implements ParameterizedRowMapper<FileStatistics>{
	
	public final FileInfo info;
	public final int retries;
	
	public FileStatistics(FileInfo info, int retries){
		this.info = info;
		this.retries = retries;
	}
	
	public FileStatistics(){
		this.info = null;
		this.retries = 0;
	}

	@Override
	public FileStatistics mapRow(ResultSet rs, int rowNum) throws SQLException {
		int fid = rs.getInt("file_id");
    	String name = rs.getString("file_name");
    	int size = rs.getInt("file_size");
    	int retries = rs.getInt("file_status");
    	String timestamp = rs.getString("timestamp");
    	return new FileStatistics(new FileInfo(fid, name, size, timestamp), retries);
	}
}
