package ro.cst.tsearch.servers.types;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.search.iterator.ConfigurableBookPageIterator;

class TNHancockST extends TNGenericUsTitleSearchDefaultRO {

	private static final long serialVersionUID = 1L;

	public TNHancockST(long searchId) {
		super(searchId);
	}

	public TNHancockST(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
	}
	
	@Override
	protected ConfigurableBookPageIterator getBookPageIterator() {
		return super.getBookPageIterator().setRemoveBookPrefixes("VOL","V");
	}
	
	@Override
	protected ConfigurableBookPageIterator getBookPageIteratorAfterOcr() {
		return super.getBookPageIterator().setRemoveBookPrefixes("VOL","V");
	}
	
	@Override
	protected String getBookForBPSearch(String book) {
		String res = StringUtils.removeStart(super.getBookForBPSearch(book), "VOL");
		res = StringUtils.removeStart(res, "V");
		return res;
	}

	@Override
	protected String getPageForBPSearch(String page) {
		return super.getPageForBPSearch(page);
	}
	

}