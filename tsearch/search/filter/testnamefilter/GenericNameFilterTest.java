package ro.cst.tsearch.search.filter.testnamefilter;

import ro.cst.tsearch.search.filter.newfilters.name.SynonimNameFilter;

public class GenericNameFilterTest extends SynonimNameFilter {

	
	private static final long serialVersionUID = 4078415212560745728L;

	public GenericNameFilterTest(long seachId){
		super(seachId);
	}
	
	public GenericNameFilterTest(String key, long searchId,
			boolean useSubdivisionName) {
		super(key, searchId, useSubdivisionName,null, true);
	}

	public GenericNameFilterTest(String key, long searchId) {
		super(key, searchId);
	}
	
	public double calculateScore(String ref[], String cand[],
			boolean generateAranjaments) {
		return super.calculateScore(ref, cand, generateAranjaments);
	}

}
