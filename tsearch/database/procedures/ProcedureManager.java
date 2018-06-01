package ro.cst.tsearch.database.procedures;

import java.util.HashMap;

import org.springframework.jdbc.object.StoredProcedure;

import ro.cst.tsearch.database.DBManager;

public class ProcedureManager {
	
	private HashMap<String, StoredProcedure> procedures = null;
	
	private ProcedureManager(){
		procedures = new HashMap<String, StoredProcedure>();
	}
	
	private static class SingletonHolder {
		private static ProcedureManager instance = new ProcedureManager();
	} 
	
	public static ProcedureManager getInstance() {
		return SingletonHolder.instance;
	}
	
	public StoredProcedure getProcedure(String name) {
		StoredProcedure result = procedures.get(name);
		if(result == null) {
			if(TableReportProcedure.SP_NAME.equals(name)){
				result = new TableReportProcedure(DBManager.getJdbcTemplate());
				procedures.put(name, result);
			} else if(GraphicReportProcedure.SP_NAME.equals(name)) {
				result = new GraphicReportProcedure(DBManager.getJdbcTemplate());
				procedures.put(name, result);
			} else if(SearchReportAllInOneProcedure.SP_NAME.equals(name)) {
				result = new SearchReportAllInOneProcedure(DBManager.getJdbcTemplate());
				procedures.put(name, result);
			}
		}
		return result;
	}

}
