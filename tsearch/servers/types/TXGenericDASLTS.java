package ro.cst.tsearch.servers.types;
import java.util.List;
import java.util.Set;

import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.iterator.instrument.InstrumentTXTSInterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

import com.stewart.ats.base.document.InstrumentI;


public class TXGenericDASLTS extends TXDaslTS {
	
	private static final long serialVersionUID = -4452448341765544186L;

	public TXGenericDASLTS(long searchId) {
		super(searchId);
	}

	public TXGenericDASLTS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	
	protected  boolean addAoLookUpSearches(TSServerInfo serverInfo, List<TSServerInfoModule> modules, Set<InstrumentI> allAoRef,long searchId,  boolean isUpdate){
		/*boolean atLeastOne = super.addAoLookUpSearches(serverInfo, modules, allAoRef, searchId, isUpdate);
		boolean oneafter2000 = false;
		for(InstrumentI inst:allAoRef){
			if( inst.hasYear() && inst.getYear()>2000){
				oneafter2000 = true;
			}
		}*/
		
		boolean atLeastOne = false; 
		boolean oneafter2000 = false;
		BetweenDatesFilterResponse betweenDatesFilterResponse = new BetweenDatesFilterResponse(searchId);
		TSServerInfoModule m = null;
		InstrumentTXTSInterator instrumentInstrumentInterator = new InstrumentTXTSInterator(searchId);
		instrumentInstrumentInterator.enableInstrumentNumber();
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_INSTR);
		m.clearSaKeys();
		m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
		m.addIterator(instrumentInstrumentInterator);
		if (isUpdate) {
			m.addFilter(betweenDatesFilterResponse);
		}
		if(instrumentInstrumentInterator.createDerrivations().size() > 0) {
			atLeastOne = true;
			oneafter2000 |= instrumentInstrumentInterator.isAbove2000();
			modules.add(m);
		}
		
		InstrumentTXTSInterator instrumentBPInterator = new InstrumentTXTSInterator(searchId);
		instrumentBPInterator.enableBookPage();
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_BP);
		m.clearSaKeys();
		m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
		m.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
		m.addIterator(instrumentBPInterator);
		if (isUpdate) {
			m.addFilter(betweenDatesFilterResponse);
		}
		if(instrumentBPInterator.createDerrivations().size() > 0) {
			atLeastOne = true;
			oneafter2000 |= instrumentBPInterator.isAbove2000();
			modules.add(m);
		}
		
		InstrumentTXTSInterator instrumentDocNoInterator = new InstrumentTXTSInterator(searchId);
		instrumentDocNoInterator.enableDocumentNumber();
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
		m.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
    			TSServerInfoConstants.VALUE_PARAM_LIST_AO_NDB_TR_DOCNO);
		m.clearSaKeys();
		m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_DOCNO_LIST_FAKE);
		m.addIterator(instrumentDocNoInterator);
		if (isUpdate) {
			m.addFilter(betweenDatesFilterResponse);
		}
		if(instrumentDocNoInterator.createDerrivations().size() > 0) {
			atLeastOne = true;
			oneafter2000 |= instrumentDocNoInterator.isAbove2000();
			modules.add(m);
		}
		return atLeastOne && oneafter2000;
	}
	
	protected String prepareInstrumentNoForCounty(InstrumentI inst){
		String instNo = inst.getInstno().replaceFirst("^0+", "");
		instNo = appendBeginingZero(6-instNo.length(),instNo);
		if(inst.hasYear()){
			if(inst.getYear()>=2000){
				instNo = inst.getYear() + appendBeginingZero(8-instNo.length(),instNo);
			}
		}
		instNo  = instNo .replaceFirst("^0+", "");
		return instNo;
	}
	
}
