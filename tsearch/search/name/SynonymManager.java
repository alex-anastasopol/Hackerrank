package ro.cst.tsearch.search.name;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.DBConstants;

import com.Ostermiller.util.ExcelCSVParser;



public class SynonymManager {
	
	protected static final Category logger = Logger.getLogger(SynonymManager.class);
	
	private HashMap<String, HashSet<String>> synonyms = null;
	private HashMap<String, HashSet<String>> synonymsBothWays = null;
	
	private static class SingletonHolder {
		private static SynonymManager instance = new SynonymManager();
	}
	
	public static SynonymManager getInstance(){
		return SingletonHolder.instance;
	}
	
	private SynonymManager() {
		synonyms = new HashMap<String, HashSet<String>>();
		synonymsBothWays = new HashMap<String, HashSet<String>>();
		loadSynonymFromDatabase();
	}

	private static final String SQL_SELECT_ALL_SYNONYMS = 
		"SELECT  " + DBConstants.FIELD_NAME_SYNONYMS_SYNONYM_KEY + ", " + DBConstants.FIELD_NAME_SYNONYMS_SYNONYM_VALUE + 
		" FROM " + DBConstants.TABLE_NAME_SYNONYMS;
	
	/*
	private static final String SQL_INSERT_KEY_VALUE = 
		"INSERT INTO " + DBConstants.TABLE_NAME_SYNONYMS + 
			" (" + DBConstants.FIELD_NAME_SYNONYMS_SYNONYM_KEY + 
			", " + DBConstants.FIELD_NAME_SYNONYMS_SYNONYM_VALUE + ") VALUES (?,?)";
	*/ 
	
	private void loadSynonymFromDatabase() {
		List<Map<String, Object>> synonymsList = DBManager.getSimpleTemplate().queryForList(SQL_SELECT_ALL_SYNONYMS);
		for (Map<String, Object> map : synonymsList) {
			String key = ((String) map.get(DBConstants.FIELD_NAME_SYNONYMS_SYNONYM_KEY)).toUpperCase();
			String value = ((String) map.get(DBConstants.FIELD_NAME_SYNONYMS_SYNONYM_VALUE)).toUpperCase();
			String[] entries = value.split("\\s*,\\s*");
			HashSet<String> entriesMap = new HashSet<String>();
			entriesMap.add(key);		//add me so I'm sure I'll test as least me
			for (String entry : entries) {
				entry = entry.trim();
				entriesMap.add(entry);
			}
			synonyms.put(key, entriesMap);
			
			for (String singleKey : entriesMap) {
				HashSet<String> alreadyAdded = synonymsBothWays.get(singleKey);
				if(alreadyAdded == null) {
					alreadyAdded = new HashSet<String>();
					synonymsBothWays.put(singleKey, alreadyAdded);
				}
				alreadyAdded.addAll(entriesMap);
			}
			
		}
	}
	
	public HashSet<String> getSynonymsFor(String name) {
		return getSynonymsFor(name, false);
	}
	
	/**
	 * Example:<br>
	 * <b>ANTHONY</b> has the following synonyms <i>TONY, ANTONIA, ANTONY, TONI, TONIA, TONYA, ANTONETTE, LATONYA</i><br>
	 * If bothWays is false only <b>ANTHONY</b> will be expanded to all synonyms<br>
	 * If bothWays is true each word like <b>TONY</b> will be expanded to all synonyms
	 * @param name the name to be searched
	 * @param bothWays
	 * @return set containing all synonyms
	 */
	@SuppressWarnings("unchecked")
	public HashSet<String> getSynonymsFor(String name, boolean bothWays) {
		HashSet<String> result = null;
		if(bothWays) {
			result = synonymsBothWays.get(name.trim().toUpperCase());
		} else {
			result = synonyms.get(name.trim().toUpperCase());
		}
		if(result == null) {
			return null;
		}
		return (HashSet<String>) result.clone();
	}
	
	private static boolean importFileIntoDatabase(File f) {
		
		if(f == null || !f.isFile() || !f.exists()) {
			return false;
		}
		try {
			String[][] rawData = ExcelCSVParser.parse(new FileReader(f));
			SimpleJdbcInsert insert = DBManager.getSimpleJdbcInsert().
				withTableName(DBConstants.TABLE_NAME_SYNONYMS);
			for (String[] row : rawData) {
				if(row.length == 2) {
					Map<String, Object> params = new HashMap<String, Object>();
					params.put(DBConstants.FIELD_NAME_SYNONYMS_SYNONYM_KEY, row[0].trim());
					params.put(DBConstants.FIELD_NAME_SYNONYMS_SYNONYM_VALUE, row[1].trim());
					insert.execute(params);
				}
			}
			
		} catch (Exception e) {
			logger.error("Parsing error for csv file " + f.getAbsolutePath(), e);
			return false;
		} 
		
		return true;
	}
	
	public static void main(String[] args) {
		//SynonymManager.importFileIntoDatabase(new File("D:\\workspace\\TS_trunk\\src\\resource\\names\\ATS-name-nickname.csv"));
		SynonymManager.getInstance().getSynonymsFor("STEPHANIE", true);
	}
}
