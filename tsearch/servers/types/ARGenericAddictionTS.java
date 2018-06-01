package ro.cst.tsearch.servers.types;

import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator;

public class ARGenericAddictionTS extends ARGenericDASLTS {

	private static final long serialVersionUID = 1L;

	public ARGenericAddictionTS(long searchId) {
		super(searchId);
	}

	public ARGenericAddictionTS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	public LegalDescriptionIterator getLegalDescriptionIterator(long searchId,
			boolean lookUpWasWithNames, boolean legalFromLastTransferOnly) {
		
		LegalDescriptionIterator it = super.getLegalDescriptionIterator(searchId, lookUpWasWithNames, legalFromLastTransferOnly);
		it.setUseAddictionInsteadOfSubdivision(true);
		return it;
	}
}
