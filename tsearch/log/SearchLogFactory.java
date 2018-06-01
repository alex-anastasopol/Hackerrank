package ro.cst.tsearch.log;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Category;

import ro.cst.tsearch.servlet.ValidateInputs;

import com.stewart.ats.base.name.Name;

/**
 * This is a implementation of Singleton and Factory design patterns. This class
 * contains a collection of SearchLogPage objects that are organized using the
 * searchId.
 * 
 * @author l
 */
public class SearchLogFactory {
	private static final Category logger = Category.getInstance(SearchLogFactory.class.getName());
	
	private static SearchLogFactory INSTANCE;

	private  Map<Long,SearchLogPage> pages; 
	
	private SearchLogFactory() {
		pages = new HashMap<Long, SearchLogPage>();
	};

	/**
	 * Retrieves an instance of {@link SearchLogFactory}.
	 * 
	 * @return
	 */
	public static SearchLogFactory getSharedInstance() {
		if (INSTANCE == null) {
			INSTANCE = new SearchLogFactory();
		}
		return INSTANCE;
	}
	
	/**
	 * Given a searchId this method returns a {@link SearchLogPage} instance. 
	 * @param searchId
	 * @return
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public SearchLogPage getSearchLogPage(Long searchId) {
		SearchLogPage searchLogPage = pages.get(searchId);
		if (searchLogPage==null){
			try {
				searchLogPage = new SearchLogPage();
			} catch (Exception e) {
				logger.error(e);
			}
			pages.put(searchId, searchLogPage);
		}
		return searchLogPage;
	}
	

	
}
