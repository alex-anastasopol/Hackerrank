package ro.cst.tsearch.servers.types;

import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.iterator.instrument.InstrumentTSTwoFormsIterator;

public class COGrandTS extends COGenericDASLTS {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public COGrandTS(long searchId) {
		super(searchId);
	}

	public COGrandTS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	public InstrumentGenericIterator getInstrumentIterator() {
		return new InstrumentTSTwoFormsIterator(searchId);
	}
}
