package ro.cst.tsearch.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;

public class OaklandSubdivisions implements ParameterizedRowMapper<OaklandSubdivisions>{

	
	private long    Id;
	private String  Code;
	private String  Area;
	private String  Name;
	private short   TypeId;
	
	public static final int  DB_OAKLAND_SUBDIVISION = 1;
	public static final int  DB_OAKLAND_CONDOMINIUM = 2;
	
	
	//GET METHODS DECLARATION
	public long getId(){
		return Id;
	}
	
	public String getCode(){
		return Code;
	}
	
	public String getArea(){
	    return Area; 	
	}
	
	public String getName(){
		return Name;
	}
	
	public short getTypeId(){
		return TypeId;
	}
	
	//SET METHODS DECLARATION	
	public void setId(long value){
		this.Id = value;
	}
	
	public void setCode(String value){
		this.Code = value;
	}
	
	public void setArea(String value){
		this.Area = value;		
	}
	
	public void setName(String value){
		this.Name = value;
	}
	
	public void setTypeId(short value){
		this.TypeId = value;
	}

	@Override
	public OaklandSubdivisions mapRow(ResultSet rs, int rowNum)
			throws SQLException {
		OaklandSubdivisions sbd = new OaklandSubdivisions();
		sbd.setId(rs.getLong(DBConstants.FIELD_SUBDIVISIONS_OAKLAND_ID));
		sbd.setCode(rs.getString(DBConstants.FIELD_SUBDIVISIONS_OAKLAND_CODE));
		sbd.setArea(rs.getString(DBConstants.FIELD_SUBDIVISIONS_OAKLAND_AREA));
		sbd.setTypeId(rs.getShort(DBConstants.FIELD_SUBDIVISIONS_OAKLAND_TYPEID));
		sbd.setName(rs.getString(DBConstants.FIELD_SUBDIVISIONS_OAKLAND_NAME));
		return sbd;
	}
	
}
 