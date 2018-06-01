package ro.cst.tsearch.search.module;

import java.util.LinkedHashSet;
import java.util.Set;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class ConfigurableSkipModuleNameIterator extends
		ConfigurableNameIterator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String[] skipModuleIfSourceTypesAvailable = null;

	public ConfigurableSkipModuleNameIterator(long searchId,
			String[] derivPatern, String ... skipModuleIfSourceTypesAvailable) {
		super(searchId,derivPatern);
		this.skipModuleIfSourceTypesAvailable = skipModuleIfSourceTypesAvailable;
		setInitAgain(true);
	}
	
	@Override
	public void init(TSServerInfoModule initial) {
		Search search = getSearch();
		DocumentsManagerI managerI = search.getDocManager();
		try {
			managerI.getAccess();
			if(managerI.getDocumentsWithDataSource(false, skipModuleIfSourceTypesAvailable).size() == 0) {
				super.init(initial);
			} else {
				Set<NameI> derivNames = new LinkedHashSet<NameI>();
				StatesIterator si = new DefaultStatesIterator(derivNames);
				setStrategy(si);
			}
		} catch (Throwable t) {
			logger.error("Error while initializing ConfigurableSkipModuleNameIterator!", t);
		} finally {
			if(managerI != null) {
				managerI.releaseAccess();
			}
		}
		
	}

}
