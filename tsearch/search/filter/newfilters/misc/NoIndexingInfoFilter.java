package ro.cst.tsearch.search.filter.newfilters.misc;

import java.math.BigDecimal;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.servers.response.ParsedResponse;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.property.PropertyI;

public class NoIndexingInfoFilter extends FilterResponse {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -7931592640311741276L;

	/**
	 * removes instruments with no legal and not grantee or grantor from search page
	 */

	/**
	 * @param searchId
	 */

	//private TSServerInfoModule	module				= null;
	//private String				saKey				= "";
	private FilterResponse		defaultNameFilter	= null;

	public NoIndexingInfoFilter(long searchId) {
		super(searchId);
		setInitAgain(true);
		//this.module = null;
		//this.saKey = SearchAttributes.OWNER_OBJECT;
		setThreshold(new BigDecimal("0.95"));
		
	}

	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		DocumentI doc = row.getDocument();

		int score = 1;

		for (PropertyI prop : doc.getProperties()) {
			score = 1;
			if (!prop.hasLegal()) {
				if (defaultNameFilter == null) {
					defaultNameFilter = NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);
				}
				if (defaultNameFilter.getScoreOneRow(row).doubleValue() < NameFilterFactory.NAME_FILTER_THRESHOLD) {
					score = 0;
				}
			}
		}

		return new BigDecimal(score);
	}

	@Override
	public String getFilterCriteria() {
		return "Documents with no legal and different Grantor and Grantee";
	}

}
