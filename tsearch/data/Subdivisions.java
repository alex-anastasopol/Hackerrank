package ro.cst.tsearch.data;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;

public class Subdivisions implements ParameterizedRowMapper<Subdivisions>{

	
	private long    Id;
	private String  Code;
	private String  Area;
	private String  Phase;
	private String  Name;
	private int   TypeId;
	
	public static final int  DB_MACOMB_SUBDIVISION = 3;
	public static final int  DB_MACOMB_CONDOMINIUM = 4;
	
	
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
	
	public int getTypeId(){
		return TypeId;
	}
	
	public String getPhase(){
		return Phase;
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
	
	public void setTypeId(int value){
		this.TypeId = value;
	}
	
	public void setPhase( String phase ){
		this.Phase = phase;
	}

	@Override
	public Subdivisions mapRow(ResultSet rs, int rowNum) throws SQLException {
		Subdivisions sbd = new Subdivisions();
		sbd.setId(rs.getLong(DBConstants.FIELD_SUBDIVISIONS_MACOMB_ID));
		sbd.setCode(rs.getString(DBConstants.FIELD_SUBDIVISIONS_MACOMB_CODE));
		sbd.setArea(rs.getString(DBConstants.FIELD_SUBDIVISIONS_MACOMB_AREA));
		sbd.setTypeId(rs.getInt(DBConstants.FIELD_SUBDIVISIONS_MACOMB_TYPEID));
		sbd.setName(rs.getString(DBConstants.FIELD_SUBDIVISIONS_MACOMB_NAME));
		sbd.setPhase(rs.getString(DBConstants.FIELD_SUBDIVISIONS_MACOMB_PHASE));
		return sbd;
	}
}
 