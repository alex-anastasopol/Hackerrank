package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

public class HoaInfoMapper implements ParameterizedRowMapper<HoaInfoMapper> {

	/**
	 * class for new style HOA updated on november 2011
	 */
	
	public static final String TABLE_HOA_INFO = "hoa_info";
	
	
	public static final String FIELD_SUBDIVISION_NAME = "subdivisionName";
	public static final String FIELD_PLAT_BOOK = "platBook";
	public static final String FIELD_PLAT_PAGE = "platPage";
	public static final String FIELD_CCR_DEC_BOOK = "ccrDecBook";
	public static final String FIELD_CCR_DEC_PAGE = "ccrDecPage";
	public static final String FIELD_HOA_NAME = "hoaName";
	public static final String FIELD_MASTER_HOA = "masterHoa";
	public static final String FIELD_ADD_HOA = "additionalHoa";
	public static final String FIELD_LIEN_JDG_NOC = "lienJdgNoc";
	public static final String FIELD_NOTES = "notes";
	public static final String FIELD_COUNTY = "county";
	public static final String FIELD_COUNTYFIPS = "countyFIPS";
	
	
	private String subdivisionName;
	private String platBook;
	private String platPage;
	private String ccrDecBook;
	private String ccrDecPage;
	private String hoaName;
	private String masterHoa;
	private String additionalHoa;
	private String lienJdgNoc;
	private String notes;
	private String county;
	private String countyFIPS;
	
	public static final String SQL_SELECT_HOA_INFO_DATA = 
		"SELECT * FROM " + TABLE_HOA_INFO + " WHERE ";
	
	public static final String SQL_INSERT_HOA_INFO_DATA = 
			"INSERT INTO " + TABLE_HOA_INFO + " (" 
			+ FIELD_SUBDIVISION_NAME + ","
			+ FIELD_PLAT_BOOK + ","
			+ FIELD_PLAT_PAGE + ","
			+ FIELD_CCR_DEC_BOOK + ","
			+ FIELD_CCR_DEC_PAGE + ","
			+ FIELD_HOA_NAME + ","
			+ FIELD_MASTER_HOA + ","
			+ FIELD_ADD_HOA + ","
			+ FIELD_LIEN_JDG_NOC + ","
			+ FIELD_NOTES + ","
			+ FIELD_COUNTY + ","
			+ FIELD_COUNTYFIPS
			+ ") "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	@Override
	public HoaInfoMapper mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		HoaInfoMapper hoaInfoMapper = new HoaInfoMapper();
		hoaInfoMapper.setSubdivisionName(resultSet.getString(FIELD_SUBDIVISION_NAME));
		hoaInfoMapper.setPlatBook(resultSet.getString(FIELD_PLAT_BOOK));
		hoaInfoMapper.setPlatPage(resultSet.getString(FIELD_PLAT_PAGE));
		hoaInfoMapper.setCcrDecBook(resultSet.getString(FIELD_CCR_DEC_BOOK));
		hoaInfoMapper.setCcrDecPage(resultSet.getString(FIELD_CCR_DEC_PAGE));
		hoaInfoMapper.setHoaName(resultSet.getString(FIELD_HOA_NAME));
		hoaInfoMapper.setMasterHoa(resultSet.getString(FIELD_MASTER_HOA));
		hoaInfoMapper.setAdditionalHoa(resultSet.getString(FIELD_ADD_HOA));
		hoaInfoMapper.setLienJdgNoc(resultSet.getString(FIELD_LIEN_JDG_NOC));
		hoaInfoMapper.setNotes(resultSet.getString(FIELD_NOTES));
		hoaInfoMapper.setCounty(resultSet.getString(FIELD_COUNTY));
		hoaInfoMapper.setCountyFIPS(resultSet.getString(FIELD_COUNTYFIPS));

		return hoaInfoMapper;
	}
	
	public String getSubdivisionName() {
		return subdivisionName;
	}

	public void setSubdivisionName(String subdivisionName) {
		this.subdivisionName = subdivisionName;
	}

	public String getPlatBook() {
		return platBook;
	}

	public void setPlatBook(String platBook) {
		this.platBook = platBook;
	}

	public String getPlatPage() {
		return platPage;
	}

	public void setPlatPage(String platPage) {
		this.platPage = platPage;
	}

	public String getCcrDecBook() {
		return ccrDecBook;
	}

	public void setCcrDecBook(String ccrDecBook) {
		this.ccrDecBook = ccrDecBook;
	}

	public String getCcrDecPage() {
		return ccrDecPage;
	}

	public void setCcrDecPage(String ccrDecPage) {
		this.ccrDecPage = ccrDecPage;
	}
	
	public String getHoaName() {
		return hoaName;
	}
	
	public void setHoaName(String hoaName) {
		this.hoaName = hoaName;
	}
	
	public String getMasterHoa() {
		return masterHoa;
	}

	public void setMasterHoa(String masterHoa) {
		this.masterHoa = masterHoa;
	}

	public String getAdditionalHoa() {
		return additionalHoa;
	}

	public void setAdditionalHoa(String additionalHoa) {
		this.additionalHoa = additionalHoa;
	}

	public String getLienJdgNoc() {
		return lienJdgNoc;
	}

	public void setLienJdgNoc(String lienJdgNoc) {
		this.lienJdgNoc = lienJdgNoc;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
	
	public String getCounty() {
		return county;
	}

	public void setCounty(String county) {
		this.county = county;
	}
	
	public String getCountyFIPS() {
		return countyFIPS;
	}

	public void setCountyFIPS(String countyFIPS) {
		this.countyFIPS = countyFIPS;
	}
	
}
