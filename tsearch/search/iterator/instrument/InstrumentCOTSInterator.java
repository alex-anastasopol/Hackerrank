package ro.cst.tsearch.search.iterator.instrument;


public class InstrumentCOTSInterator extends InstrumentGenericIterator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public InstrumentCOTSInterator(long searchId) {
		super(searchId);
	}

	protected String cleanInstrumentNo(String instno, int year) {
		return instno.replaceFirst("^0+", "");
	}

}
