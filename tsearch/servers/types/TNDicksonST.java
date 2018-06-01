package ro.cst.tsearch.servers.types;

import ro.cst.tsearch.search.iterator.ConfigurableBookPageIterator;

class TNDicksonST extends TNGenericUsTitleSearchDefaultRO {

	private static final long serialVersionUID = 1L;

	public TNDicksonST(long searchId) {
		super(searchId);
	}

	public TNDicksonST(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId,
			int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,
				miServerID);
	}
	
	protected ConfigurableBookPageIterator getBookPageIterator() {
		ConfigurableBookPageIterator bpIterator = super.getBookPageIterator();
		bpIterator.setBookPrefix("V");
		bpIterator.setOnlySearchWithPrefixesAdded(true);
		return bpIterator;
	}
	
	protected ConfigurableBookPageIterator getBookPageIteratorAfterOcr() {
		ConfigurableBookPageIterator bpIterator = super.getBookPageIterator();
		bpIterator.setBookPrefix("V");
		bpIterator.setOnlySearchWithPrefixesAdded(true);
		return bpIterator;
	}
	
	@Override
	protected String getBookForBPSearch(String book) {
		String res = super.getBookForBPSearch(book);
		
		if(!res.startsWith("V"))
			res = "V" + res;
		return res;
	}

	@Override
	protected String getPageForBPSearch(String page) {
		String res = super.getBookForBPSearch(page);
		return res;
	}
	

}