package ro.cst.tsearch.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.utils.DBConstants;

public class StateCountyManager {
	
	private HashMap<Long, HashMap<Long, County>> allCountiesByStateId = null;
	private HashMap<Long, County> allCounties = null;
	private HashSet<String> allStateAbrev = null;
	
	private StateCountyManager() {
		allCountiesByStateId = new HashMap<Long, HashMap<Long,County>>();
		allCounties = new HashMap<Long,County>();
		allStateAbrev = new HashSet<String>();
		reloadCounties();
	}
	
	private static class SingletonHolder {
		private static StateCountyManager instance = new StateCountyManager();
	}
	
	public static StateCountyManager getInstance(){
		return SingletonHolder.instance;
	}

	private static final String SQL_GET_COUNTIES = "SELECT c." + 
		DBConstants.FIELD_COUNTY_ID + ", c." + 
		DBConstants.FIELD_COUNTY_NAME + ", c." +
		DBConstants.FIELD_COUNTY_STATE_ID + ", s." +
		DBConstants.FIELD_STATE_ABV + ", c." +
		DBConstants.FIELD_COUNTY_FIPS_ID + " FROM " + 
		DBConstants.TABLE_COUNTY + " c JOIN " + 
		DBConstants.TABLE_STATE + " s ON c." + 
		DBConstants.FIELD_COUNTY_STATE_ID + " = s." + 
		DBConstants.FIELD_STATE_ID;
	
	public synchronized void reloadCounties() {
		List<County> counties = DBManager.getSimpleTemplate().query(SQL_GET_COUNTIES, new County());
		/*
		 *  State.getStateFromAbv(currentState)
		 */
		/*int i = 1010000;
		
		Scanner sc = null;;
		try {
			sc = new Scanner(new File("e:/a.txt"));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
       
		sc.useDelimiter("\\s+"); // this means whitespace or comma
        while(sc.hasNext()) {
            String stateFips = sc.next();
           
            if(stateFips.length()==1){
				stateFips ="0"+stateFips;
			}
            
            String countyFips = sc.next();
            if(countyFips.length()==1){
				countyFips = "00"+countyFips;
			}else if(countyFips.length()==2){
				countyFips ="0"+countyFips;
			}
            
            if(StringUtils.isNotBlank(stateFips)&&StringUtils.isNotBlank(countyFips)){
				i++;
				String str1 ="INSERT INTO indexProfile(id,subCountyId,indexType,beginDate,endDate,firstDoc,lastDoc,docType,docFormat,docFiledNames,rangeInfo,idDescr,stateFips,areaFips,companyType) values ("+i+",'','Year.Docid.Doctype',NULL,NULL,'','','SearchRes','','','','General Index with Year.DocId.Doctype',"+stateFips+","+countyFips+",'');";
	
				i++;
				String str2 ="INSERT INTO indexProfile(id,subCountyId,indexType,beginDate,endDate,firstDoc,lastDoc,docType,docFormat,docFiledNames,rangeInfo,idDescr,stateFips,areaFips,companyType ) values ("+i+",'','Book.Page.Doctype',NULL,NULL,'','','SearchRes','','','','General Index with Book.Page.Doctype',"+stateFips+","+countyFips+",'');";
	
				try {
					FileUtils.write(new File("E:/ada.sql"), str1+"\n"+str2+"\n", true);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
        }*/
		
		for (County county : counties) {	
			HashMap<Long, County> countiesOnState = allCountiesByStateId.get(county.getStateId());
			if(countiesOnState == null) {
				countiesOnState = new HashMap<Long, County>();
				allCountiesByStateId.put(county.getStateId(), countiesOnState);
			}
			countiesOnState.put(county.getId(), county);
			allCounties.put(county.getId(), county);
			allStateAbrev.add(county.getStateAbv());
		}
	}
	
	public String getCountyFipsForCountyId(long stateId, long countyId) {
		HashMap<Long, County> countiesOnState = allCountiesByStateId.get(stateId);
		return countiesOnState.get(countyId).getIdFips();
	}
	
	public List<County> getCountiesForStateIds(int ... stateIds) {
		List<County> counties = new Vector<County>();
		for (int stateId : stateIds) {
			HashMap<Long, County> countiesOnState = allCountiesByStateId.get(new Long(stateId));
			if(countiesOnState != null) {
				counties.addAll(countiesOnState.values());
			}
		}
		return counties;
	}
	
	public County getCounty(long countyId) {
		return allCounties.get(countyId);
	}
	
	public boolean isStateAbrevValid(String stateAbrev) {
		if(stateAbrev == null) {
			return false;
		}
		return allStateAbrev.contains(stateAbrev);
	}
	/**
	 * Returns a string formated like STCountyName where ST is State Abbreviation and CountyName is the name of the county
	 * @param countyId id of the county
	 * @return the formatted STCountyName or empty if countyId is not valid
	 */
	public String getSTCounty(long countyId) {
		County county = allCounties.get(countyId);
		if(county != null) {
			return county.getStateAbv() + county.getName();
		}
		return "";
	}
}
