package ro.cst.tsearch.servers.types;

import java.util.List;
import java.util.Set;

import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.ExactDateFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;

public class TXMidlandTS extends TXGenericConcatNameDASLTS {

	private static final long serialVersionUID = 1L;

	public TXMidlandTS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	public LegalDescriptionIterator getLegalDescriptionIterator(long searchId,
			boolean lookUpWasWithNames, boolean legalFromLastTransferOnly) {
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, lookUpWasWithNames, legalFromLastTransferOnly, getDataSite());
		it.setEnableTownshipLegal(false);
		return it;
	}
	
	protected boolean addAoLookUpSearches(TSServerInfo serverInfo, List<TSServerInfoModule> modules,
			Set<InstrumentI> allAoRef, long searchId, boolean isUpdate) {

		boolean atLeastOne = false;
		BetweenDatesFilterResponse betweenDatesFilterResponse = BetweenDatesFilterResponse
				.getDefaultIntervalFilter(searchId);

		InstrumentGenericIterator instrumentBPInterator = getInstrumentIterator();
		instrumentBPInterator.enableBookPage();
		TSServerInfoModule m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
				TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_BP);
		m.clearSaKeys();
		m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
		m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
		m.addIterator(instrumentBPInterator);
		if (isUpdate) {
			m.addFilter(betweenDatesFilterResponse);
		}
		if (instrumentBPInterator.createDerrivations().size() > 0) {
			atLeastOne = true;
			modules.add(m);
		}

		InstrumentGenericIterator instrumentInstrumentInterator = getInstrumentIterator();
		instrumentInstrumentInterator.enableInstrumentNumber();
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
				TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);
		m.clearSaKeys();
		m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
		m.addIterator(instrumentInstrumentInterator);
		m.addFilter(new ExactDateFilterResponse(searchId));
		if (isUpdate) {
			m.addFilter(betweenDatesFilterResponse);
		}
		if (instrumentInstrumentInterator.createDerrivations().size() > 0) {
			atLeastOne = true;
			modules.add(m);
		}

		InstrumentGenericIterator instrumentDocNoInterator = getInstrumentIterator();
		instrumentDocNoInterator.enableDocumentNumber();
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
				TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_DOCNO);
		m.clearSaKeys();
		m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_DOCNO_LIST_FAKE);
		m.addIterator(instrumentDocNoInterator);
		m.addFilter(new ExactDateFilterResponse(searchId));
		if (isUpdate) {
			m.addFilter(betweenDatesFilterResponse);
		}
		if (instrumentDocNoInterator.createDerrivations().size() > 0) {
			atLeastOne = true;
			modules.add(m);
		}
		return atLeastOne;

	}
	
	@Override
	public InstrumentGenericIterator getInstrumentIterator() {
		return new InstrumentIterator(searchId);
	}
	
	public class InstrumentIterator extends InstrumentGenericIterator {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public InstrumentIterator(long searchId) {
			super(searchId);
		}
		
		protected String cleanInstrumentNo(String instno, int year) {
			return instno.replaceFirst("^0+", "");
		}
		
		@Override
		protected void loadDerrivation(TSServerInfoModule module, InstrumentI inst) {
			super.loadDerrivation(module, inst);
			
			List<FilterResponse> allFilters = module.getFilterList();
			ExactDateFilterResponse dateFilter = null;
			if(allFilters != null) {
				for (FilterResponse filterResponse : allFilters) {
					if (filterResponse instanceof ExactDateFilterResponse) {
						dateFilter = (ExactDateFilterResponse) filterResponse;
						dateFilter.getFilterDates().clear();
						if(inst instanceof RegisterDocument) {
							dateFilter.addFilterDate(((RegisterDocumentI)inst).getRecordedDate());
							dateFilter.addFilterDate(((RegisterDocumentI)inst).getInstrumentDate());
						} else {
							dateFilter.addFilterDate(inst.getDate());
						}
					}
				}
			}
		}

	}

	
}


