package ro.cst.tsearch.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;

public class ILKaneSubdivisions implements ParameterizedRowMapper<ILKaneSubdivisions>{

	
	private String  Code;
	private String  Name;
	private String  Plat_doc;
	
	public static final int  DB_IL_KANE_SUBDIVISION = 5;
	
	
	//GET METHODS DECLARATION
	public String getCode(){
		return Code;
	}
	
	public String getName(){
		return Name;
	}
	
	public String getPlatDoc(){
	    return Plat_doc; 	
	}
	
	//SET METHODS DECLARATION	
	public void setCode(String value){
		this.Code = value;
	}
	
	public void setName(String value){
		this.Name = value;
	}
	
	public void setPlatDoc(String value){
		this.Plat_doc = value;		
	}

	@Override
	public ILKaneSubdivisions mapRow(ResultSet rs, int rowNum)
			throws SQLException {
		ILKaneSubdivisions sbd = new ILKaneSubdivisions();
		sbd.setCode(rs.getString(DBConstants.FIELD_SUBDIVISIONS_IL_KANE_CODE));
		sbd.setName(rs.getString(DBConstants.FIELD_SUBDIVISIONS_IL_KANE_NAME));
		sbd.setPlatDoc(rs.getString(DBConstants.FIELD_SUBDIVISIONS_IL_KANE_PLAT_DOC));
		return sbd;
	}
	
}
 