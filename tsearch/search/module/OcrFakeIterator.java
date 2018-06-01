package ro.cst.tsearch.search.module;

import java.util.List;

import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

public class OcrFakeIterator extends OcrOrBootStraperIterator {

	private static final long serialVersionUID = -1110598193537657882L;

	public OcrFakeIterator(boolean removeTrailingZeroes, long searchId) {
		super(removeTrailingZeroes, searchId);
	}

	public OcrFakeIterator(long searchId) {
		super(searchId);
	}

	@Override
	public List<Instrument> extractInstrumentNoList(TSServerInfoModule initial) {
		List<Instrument> extractInstrumentNoList = super.extractInstrumentNoList(initial);
		extractInstrumentNoList.clear();
		return extractInstrumentNoList;
	}

}
