package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.DBConstants;

@Deprecated
public class HoaAndCondoMapper implements ParameterizedRowMapper<HoaAndCondoMapper> {

	/**
	 * class for old style HOA from 2010
	 */
	
	
	public static final String TABLE_HOA_CONDO = "hoa_condo";
	
	public static final String FIELD_ORIGINAL_ID = "original_id";
	public static final String FIELD_ASSOC_NAME = "assoc_name";
	public static final String FIELD_NO_OF_UNITS = "no_of_units";
	public static final String FIELD_ASSOC_ADDR_LINE1 = "assoc_addr_line1";
	public static final String FIELD_ASSOC_ADDR_LINE2 = "assoc_addr_line2";
	public static final String FIELD_ASSOC_CITY = "assoc_city";
	public static final String FIELD_ASSOC_STATE = "assoc_state";
	public static final String FIELD_ASSOC_ZIP = "assoc_zip";
	public static final String FIELD_ASSOC_CREATION_DATE = "assoc_creat_date";
	public static final String FIELD_REGISTERED_AGENT_NAME = "reg_agent_name";
	public static final String FIELD_REGISTERED_AGENT_STREET_NUMBER = "reg_agent_str_no";
	public static final String FIELD_REGISTERED_AGENT_STREET_PREFIX_DIR = "reg_agent_str_pref_dir";
	public static final String FIELD_REGISTERED_AGENT_STREET_NAME = "reg_agent_str_name";
	public static final String FIELD_REGISTERED_AGENT_STREET_SUFFIX = "reg_agent_str_suff";
	public static final String FIELD_REGISTERED_AGENT_STREET_SUFFIX_DIR = "reg_agent_str_suff_dir";
	public static final String FIELD_REGISTERED_AGENT_APT_TYPE = "reg_agent_apt_type";
	public static final String FIELD_REGISTERED_AGENT_APT_NUMBER = "reg_agent_apt_no";
	public static final String FIELD_REGISTERED_AGENT_CITY = "reg_agent_city";
	public static final String FIELD_REGISTERED_AGENT_STATE = "reg_agent_state";
	public static final String FIELD_REGISTERED_AGENT_ZIP = "reg_agent_zip";
	public static final String FIELD_REGISTERED_AGENT_ZIP4 = "reg_agent_zip4";
	public static final String FIELD_REGISTERED_AGENT_PHONE = "reg_agent_phone";
	public static final String FIELD_BOARD_MEMBER_TITLE = "board_memb_title";
	public static final String FIELD_PRESIDENT = "president";
	public static final String FIELD_VICE_PRESIDENT = "vice_president";
	public static final String FIELD_SECRETARY = "secretary";
	public static final String FIELD_TREASURER = "treasurer";
	public static final String FIELD_BOARD_MEMBER_NAME = "board_memb_name";
	public static final String FIELD_PARSED_FIRST_NAME = "parsed_fname";
	public static final String FIELD_PARSED_LAST_NAME = "parsed_lname";
	public static final String FIELD_BOARD_MEMBER_ADDRESS_PREFIX_NUMBER = "board_memb_addr_pref_no";
	public static final String FIELD_BOARD_MEMBER_ADDRESS_PREFIX_DIR = "board_memb_addr_pref_dir";
	public static final String FIELD_BOARD_MEMBER_ADDRESS_STREET = "board_memb_addr_street";
	public static final String FIELD_BOARD_MEMBER_ADDRESS_STREET_SUFFIX = "board_memb_addr_street_suff";
	public static final String FIELD_BOARD_MEMBER_ADDRESS_STREET_SUFFIX_DIR = "board_memb_addr_street_suff_dir";
	public static final String FIELD_BOARD_MEMBER_ADDRESS_APT_TYPE = "board_memb_addr_apt_type";
	public static final String FIELD_BOARD_MEMBER_ADDRESS_APT_NUMBER = "board_memb_addr_apt_no";
	public static final String FIELD_BOARD_MEMBER_CITY = "board_memb_city";
	public static final String FIELD_BOARD_MEMBER_STATE = "board_memb_state";
	public static final String FIELD_BOARD_MEMBER_ZIP = "board_memb_zip";
	public static final String FIELD_BOARD_MEMBER_ZIP_PLUS4 = "board_memb_zip_plus4";
	public static final String FIELD_BOARD_MEMBER_PHONE = "board_memb_phone";
	public static final String FIELD_ADDR_SCORE = "addr_score";
	public static final String FIELD_ESTIMATED_INCOME = "estimated_income";
	public static final String FIELD_ESTIMATED_HOME_VALUE = "estimated_home_value";
	public static final String FIELD_COUNTY = "county";
	public static final String FIELD_COUNTYFIPS = "countyFIPS";
	
	
	private String original_id;
	private String assoc_name;
	private String no_of_units;
	private String assoc_addr_line1;
	private String assoc_addr_line2;
	private String assoc_city;
	private String assoc_state;
	private String assoc_zip;
	private String assoc_creat_date;
	private String reg_agent_name;
	private String reg_agent_str_no;
	private String reg_agent_str_pref_dir;
	private String reg_agent_str_name;
	private String reg_agent_str_suff;
	private String reg_agent_str_suff_dir;
	private String reg_agent_apt_type;
	private String reg_agent_apt_no;
	private String reg_agent_city;
	private String reg_agent_state;
	private String reg_agent_zip;
	private String reg_agent_zip4;
	private String reg_agent_phone;
	private String board_memb_title;
	private String president;
	private String vice_president;
	private String secretary;
	private String treasurer;
	private String board_memb_name;
	private String parsed_fname;
	private String parsed_lname;
	private String board_memb_addr_pref_no;
	private String board_memb_addr_pref_dir;
	private String board_memb_addr_street;
	private String board_memb_addr_street_suff;
	private String board_memb_addr_street_suff_dir;
	private String board_memb_addr_apt_type;
	private String board_memb_addr_apt_no;
	private String board_memb_city;
	private String board_memb_state;
	private String board_memb_zip;
	private String board_memb_zip_plus4;
	private String board_memb_phone;
	private String addr_score;
	private String estimated_income;
	private String estimated_home_value;
	private String county;
	private String countyFIPS;
	
	public static final String SQL_SELECT_HOA_CONDO_DATA = 
		"SELECT * FROM " + TABLE_HOA_CONDO + " WHERE ";

	
	@Override
	public HoaAndCondoMapper mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		HoaAndCondoMapper hoaAndCondoMapper = new HoaAndCondoMapper();
		hoaAndCondoMapper.setOriginal_id(resultSet.getString(FIELD_ORIGINAL_ID));
		hoaAndCondoMapper.setAssoc_name(resultSet.getString(FIELD_ASSOC_NAME));
		hoaAndCondoMapper.setNo_of_units(resultSet.getString(FIELD_NO_OF_UNITS));
		hoaAndCondoMapper.setAssoc_addr_line1(resultSet.getString(FIELD_ASSOC_ADDR_LINE1));
		hoaAndCondoMapper.setAssoc_addr_line2(resultSet.getString(FIELD_ASSOC_ADDR_LINE2));
		hoaAndCondoMapper.setAssoc_city(resultSet.getString(FIELD_ASSOC_CITY));
		hoaAndCondoMapper.setAssoc_state(resultSet.getString(FIELD_ASSOC_STATE));
		hoaAndCondoMapper.setAssoc_zip(resultSet.getString(FIELD_ASSOC_ZIP));
		hoaAndCondoMapper.setAssoc_creat_date(resultSet.getString(FIELD_ASSOC_CREATION_DATE));
		hoaAndCondoMapper.setReg_agent_name(resultSet.getString(FIELD_REGISTERED_AGENT_NAME));
		hoaAndCondoMapper.setReg_agent_str_no(resultSet.getString(FIELD_REGISTERED_AGENT_STREET_NUMBER));
		hoaAndCondoMapper.setReg_agent_str_pref_dir(resultSet.getString(FIELD_REGISTERED_AGENT_STREET_PREFIX_DIR));
		hoaAndCondoMapper.setReg_agent_str_name(resultSet.getString(FIELD_REGISTERED_AGENT_STREET_NAME));
		hoaAndCondoMapper.setReg_agent_str_suff(resultSet.getString(FIELD_REGISTERED_AGENT_STREET_SUFFIX));
		hoaAndCondoMapper.setReg_agent_str_suff_dir(resultSet.getString(FIELD_REGISTERED_AGENT_STREET_SUFFIX_DIR));
		hoaAndCondoMapper.setReg_agent_apt_type(resultSet.getString(FIELD_REGISTERED_AGENT_APT_TYPE));
		hoaAndCondoMapper.setReg_agent_apt_no(resultSet.getString(FIELD_REGISTERED_AGENT_APT_NUMBER));
		hoaAndCondoMapper.setReg_agent_city(resultSet.getString(FIELD_REGISTERED_AGENT_CITY));
		hoaAndCondoMapper.setReg_agent_state(resultSet.getString(FIELD_REGISTERED_AGENT_STATE));
		hoaAndCondoMapper.setReg_agent_zip(resultSet.getString(FIELD_REGISTERED_AGENT_ZIP));
		hoaAndCondoMapper.setReg_agent_zip4(resultSet.getString(FIELD_REGISTERED_AGENT_ZIP4));
		hoaAndCondoMapper.setReg_agent_phone(resultSet.getString(FIELD_REGISTERED_AGENT_PHONE));
		hoaAndCondoMapper.setBoard_memb_title(resultSet.getString(FIELD_BOARD_MEMBER_TITLE));
		hoaAndCondoMapper.setPresident(resultSet.getString(FIELD_PRESIDENT));
		hoaAndCondoMapper.setVice_president(resultSet.getString(FIELD_VICE_PRESIDENT));
		hoaAndCondoMapper.setSecretary(resultSet.getString(FIELD_SECRETARY));
		hoaAndCondoMapper.setTreasurer(resultSet.getString(FIELD_TREASURER));
		hoaAndCondoMapper.setBoard_memb_name(resultSet.getString(FIELD_BOARD_MEMBER_NAME));
		hoaAndCondoMapper.setParsed_fname(resultSet.getString(FIELD_PARSED_FIRST_NAME));
		hoaAndCondoMapper.setParsed_lname(resultSet.getString(FIELD_PARSED_LAST_NAME));
		hoaAndCondoMapper.setBoard_memb_addr_pref_no(resultSet.getString(FIELD_BOARD_MEMBER_ADDRESS_PREFIX_NUMBER));
		hoaAndCondoMapper.setBoard_memb_addr_pref_dir(resultSet.getString(FIELD_BOARD_MEMBER_ADDRESS_PREFIX_DIR));
		hoaAndCondoMapper.setBoard_memb_addr_street(resultSet.getString(FIELD_BOARD_MEMBER_ADDRESS_STREET));
		hoaAndCondoMapper.setBoard_memb_addr_street_suff(resultSet.getString(FIELD_BOARD_MEMBER_ADDRESS_STREET_SUFFIX));
		hoaAndCondoMapper.setBoard_memb_addr_street_suff_dir(resultSet.getString(FIELD_BOARD_MEMBER_ADDRESS_STREET_SUFFIX_DIR));
		hoaAndCondoMapper.setBoard_memb_addr_apt_type(resultSet.getString(FIELD_BOARD_MEMBER_ADDRESS_APT_TYPE));
		hoaAndCondoMapper.setBoard_memb_addr_apt_no(resultSet.getString(FIELD_BOARD_MEMBER_ADDRESS_APT_NUMBER));
		hoaAndCondoMapper.setBoard_memb_city(resultSet.getString(FIELD_BOARD_MEMBER_CITY));
		hoaAndCondoMapper.setBoard_memb_state(resultSet.getString(FIELD_BOARD_MEMBER_STATE));
		hoaAndCondoMapper.setBoard_memb_zip(resultSet.getString(FIELD_BOARD_MEMBER_ZIP));
		hoaAndCondoMapper.setBoard_memb_zip_plus4(resultSet.getString(FIELD_BOARD_MEMBER_ZIP_PLUS4));
		hoaAndCondoMapper.setBoard_memb_phone(resultSet.getString(FIELD_BOARD_MEMBER_PHONE));
		hoaAndCondoMapper.setAddr_score(resultSet.getString(FIELD_ADDR_SCORE));
		hoaAndCondoMapper.setEstimated_income(resultSet.getString(FIELD_ESTIMATED_INCOME));
		hoaAndCondoMapper.setEstimated_home_value(resultSet.getString(FIELD_ESTIMATED_HOME_VALUE));
		hoaAndCondoMapper.setCounty(resultSet.getString(FIELD_COUNTY));
		hoaAndCondoMapper.setCountyFIPS(resultSet.getString(FIELD_COUNTYFIPS));

		return hoaAndCondoMapper;
	}
	
	public String getOriginal_id() {
		return original_id;
	}

	public void setOriginal_id(String original_id) {
		this.original_id = original_id;
	}

	public String getAssoc_name() {
		return assoc_name;
	}

	public void setAssoc_name(String assoc_name) {
		this.assoc_name = assoc_name;
	}

	public String getNo_of_units() {
		return no_of_units;
	}

	public void setNo_of_units(String no_of_units) {
		this.no_of_units = no_of_units;
	}

	public String getAssoc_addr_line1() {
		return assoc_addr_line1;
	}

	public void setAssoc_addr_line1(String assoc_addr_line1) {
		this.assoc_addr_line1 = assoc_addr_line1;
	}

	public String getAssoc_addr_line2() {
		return assoc_addr_line2;
	}

	public void setAssoc_addr_line2(String assoc_addr_line2) {
		this.assoc_addr_line2 = assoc_addr_line2;
	}

	public String getAssoc_city() {
		return assoc_city;
	}

	public void setAssoc_city(String assoc_city) {
		this.assoc_city = assoc_city;
	}

	public String getAssoc_state() {
		return assoc_state;
	}

	public void setAssoc_state(String assoc_state) {
		this.assoc_state = assoc_state;
	}

	public String getAssoc_zip() {
		return assoc_zip;
	}

	public void setAssoc_zip(String assoc_zip) {
		this.assoc_zip = assoc_zip;
	}

	public String getAssoc_creat_date() {
		return assoc_creat_date;
	}

	public void setAssoc_creat_date(String assoc_creat_date) {
		this.assoc_creat_date = assoc_creat_date;
	}

	public String getReg_agent_name() {
		return reg_agent_name;
	}

	public void setReg_agent_name(String reg_agent_name) {
		this.reg_agent_name = reg_agent_name;
	}

	public String getReg_agent_str_no() {
		return reg_agent_str_no;
	}

	public void setReg_agent_str_no(String reg_agent_str_no) {
		this.reg_agent_str_no = reg_agent_str_no;
	}

	public String getReg_agent_str_pref_dir() {
		return reg_agent_str_pref_dir;
	}

	public void setReg_agent_str_pref_dir(String reg_agent_str_pref_dir) {
		this.reg_agent_str_pref_dir = reg_agent_str_pref_dir;
	}

	public String getReg_agent_str_name() {
		return reg_agent_str_name;
	}

	public void setReg_agent_str_name(String reg_agent_str_name) {
		this.reg_agent_str_name = reg_agent_str_name;
	}

	public String getReg_agent_str_suff() {
		return reg_agent_str_suff;
	}

	public void setReg_agent_str_suff(String reg_agent_str_suff) {
		this.reg_agent_str_suff = reg_agent_str_suff;
	}

	public String getReg_agent_str_suff_dir() {
		return reg_agent_str_suff_dir;
	}

	public void setReg_agent_str_suff_dir(String reg_agent_str_suff_dir) {
		this.reg_agent_str_suff_dir = reg_agent_str_suff_dir;
	}

	public String getReg_agent_apt_type() {
		return reg_agent_apt_type;
	}

	public void setReg_agent_apt_type(String reg_agent_apt_type) {
		this.reg_agent_apt_type = reg_agent_apt_type;
	}

	public String getReg_agent_apt_no() {
		return reg_agent_apt_no;
	}

	public void setReg_agent_apt_no(String reg_agent_apt_no) {
		this.reg_agent_apt_no = reg_agent_apt_no;
	}

	public String getReg_agent_city() {
		return reg_agent_city;
	}

	public void setReg_agent_city(String reg_agent_city) {
		this.reg_agent_city = reg_agent_city;
	}

	public String getReg_agent_state() {
		return reg_agent_state;
	}

	public void setReg_agent_state(String reg_agent_state) {
		this.reg_agent_state = reg_agent_state;
	}

	public String getReg_agent_zip() {
		return reg_agent_zip;
	}

	public void setReg_agent_zip(String reg_agent_zip) {
		this.reg_agent_zip = reg_agent_zip;
	}

	public String getReg_agent_zip4() {
		return reg_agent_zip4;
	}

	public void setReg_agent_zip4(String reg_agent_zip4) {
		this.reg_agent_zip4 = reg_agent_zip4;
	}

	public String getReg_agent_phone() {
		return reg_agent_phone;
	}

	public void setReg_agent_phone(String reg_agent_phone) {
		this.reg_agent_phone = reg_agent_phone;
	}

	public String getBoard_memb_title() {
		return board_memb_title;
	}

	public void setBoard_memb_title(String board_memb_title) {
		this.board_memb_title = board_memb_title;
	}

	public String getPresident() {
		return president;
	}

	public void setPresident(String president) {
		this.president = president;
	}

	public String getVice_president() {
		return vice_president;
	}

	public void setVice_president(String vice_president) {
		this.vice_president = vice_president;
	}

	public String getSecretary() {
		return secretary;
	}

	public void setSecretary(String secretary) {
		this.secretary = secretary;
	}

	public String getTreasurer() {
		return treasurer;
	}

	public void setTreasurer(String treasurer) {
		this.treasurer = treasurer;
	}

	public String getBoard_memb_name() {
		return board_memb_name;
	}

	public void setBoard_memb_name(String board_memb_name) {
		this.board_memb_name = board_memb_name;
	}

	public String getParsed_fname() {
		return parsed_fname;
	}

	public void setParsed_fname(String parsed_fname) {
		this.parsed_fname = parsed_fname;
	}

	public String getParsed_lname() {
		return parsed_lname;
	}

	public void setParsed_lname(String parsed_lname) {
		this.parsed_lname = parsed_lname;
	}

	public String getBoard_memb_addr_pref_no() {
		return board_memb_addr_pref_no;
	}

	public void setBoard_memb_addr_pref_no(String board_memb_addr_pref_no) {
		this.board_memb_addr_pref_no = board_memb_addr_pref_no;
	}

	public String getBoard_memb_addr_pref_dir() {
		return board_memb_addr_pref_dir;
	}

	public void setBoard_memb_addr_pref_dir(String board_memb_addr_pref_dir) {
		this.board_memb_addr_pref_dir = board_memb_addr_pref_dir;
	}

	public String getBoard_memb_addr_street() {
		return board_memb_addr_street;
	}

	public void setBoard_memb_addr_street(String board_memb_addr_street) {
		this.board_memb_addr_street = board_memb_addr_street;
	}

	public String getBoard_memb_addr_street_suff() {
		return board_memb_addr_street_suff;
	}

	public void setBoard_memb_addr_street_suff(String board_memb_addr_street_suff) {
		this.board_memb_addr_street_suff = board_memb_addr_street_suff;
	}

	public String getBoard_memb_addr_street_suff_dir() {
		return board_memb_addr_street_suff_dir;
	}

	public void setBoard_memb_addr_street_suff_dir(
			String board_memb_addr_street_suff_dir) {
		this.board_memb_addr_street_suff_dir = board_memb_addr_street_suff_dir;
	}

	public String getBoard_memb_addr_apt_type() {
		return board_memb_addr_apt_type;
	}

	public void setBoard_memb_addr_apt_type(String board_memb_addr_apt_type) {
		this.board_memb_addr_apt_type = board_memb_addr_apt_type;
	}

	public String getBoard_memb_addr_apt_no() {
		return board_memb_addr_apt_no;
	}

	public void setBoard_memb_addr_apt_no(String board_memb_addr_apt_no) {
		this.board_memb_addr_apt_no = board_memb_addr_apt_no;
	}

	public String getBoard_memb_city() {
		return board_memb_city;
	}

	public void setBoard_memb_city(String board_memb_city) {
		this.board_memb_city = board_memb_city;
	}

	public String getBoard_memb_state() {
		return board_memb_state;
	}

	public void setBoard_memb_state(String board_memb_state) {
		this.board_memb_state = board_memb_state;
	}

	public String getBoard_memb_zip() {
		return board_memb_zip;
	}

	public void setBoard_memb_zip(String board_memb_zip) {
		this.board_memb_zip = board_memb_zip;
	}

	public String getBoard_memb_zip_plus4() {
		return board_memb_zip_plus4;
	}

	public void setBoard_memb_zip_plus4(String board_memb_zip_plus4) {
		this.board_memb_zip_plus4 = board_memb_zip_plus4;
	}

	public String getBoard_memb_phone() {
		return board_memb_phone;
	}

	public void setBoard_memb_phone(String board_memb_phone) {
		this.board_memb_phone = board_memb_phone;
	}

	public String getAddr_score() {
		return addr_score;
	}

	public void setAddr_score(String addr_score) {
		this.addr_score = addr_score;
	}

	public String getEstimated_income() {
		return estimated_income;
	}

	public void setEstimated_income(String estimated_income) {
		this.estimated_income = estimated_income;
	}

	public String getEstimated_home_value() {
		return estimated_home_value;
	}

	public void setEstimated_home_value(String estimated_home_value) {
		this.estimated_home_value = estimated_home_value;
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
