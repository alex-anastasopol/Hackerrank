package ro.cst.tsearch.log;

import com.stewart.ats.base.name.Name;

public interface SearchLogPageLogicalLayout {
	void addNewGrantor(Name name, String currentUser) throws InstantiationException,
	IllegalAccessException;
	
}
