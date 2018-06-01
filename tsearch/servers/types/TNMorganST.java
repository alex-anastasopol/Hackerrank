package ro.cst.tsearch.servers.types;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.search.iterator.ConfigurableBookPageIterator;

class TNMorganST extends TNGenericUsTitleSearchDefaultRO {

	private static final long serialVersionUID = 1L;

	public TNMorganST(long searchId) {
		super(searchId);
	}

	public TNMorganST(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
	}
	
	protected ConfigurableBookPageIterator getBookPageIterator() {
		return super.getBookPageIterator().setRemoveBookPrefixes("RB");
	}
	
	protected ConfigurableBookPageIterator getBookPageIteratorAfterOcr() {
		return super.getBookPageIterator().setRemoveBookPrefixes("RB");
	}
	
	@Override
	protected String getBookForBPSearch(String book) {
		return StringUtils.removeStart(super.getBookForBPSearch(book), "RB");
	}

	@Override
	protected String getPageForBPSearch(String page) {
		return super.getBookForBPSearch(page);
	}
	

}