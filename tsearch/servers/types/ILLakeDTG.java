package ro.cst.tsearch.servers.types;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.types.FLSubdividedBasedDASLDT.PropertyTypeFilter;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;

import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;

/**
 * @author cristian stochina
 */
public class ILLakeDTG extends TSServerDTG {

	private static final long serialVersionUID = 3024488184071095461L;

	public ILLakeDTG(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid)
			throws FileNotFoundException, IOException {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		setUsePlatInstrumentInsteadOfPlat(true);
	}

	public ILLakeDTG(long searchId) {
		super(searchId);
		setUsePlatInstrumentInsteadOfPlat(true);
	}
	
	public void addRelatedSearch(TSServerInfo serverInfo, List<TSServerInfoModule> modules) {
		
		TSServerInfoModule module = null;
		String[] relatedSourceDoctype = new String[]{
				DocumentTypes.MORTGAGE, 
				DocumentTypes.LIEN, 
				DocumentTypes.CCER
				};
		
		InstrumentGenericIterator instrumentRelatedBPIterator = getInstrumentIterator();
		instrumentRelatedBPIterator.enableBookPage();
		instrumentRelatedBPIterator.setLoadFromRoLike(true);
		instrumentRelatedBPIterator.setRoDoctypesToLoad(relatedSourceDoctype);
		instrumentRelatedBPIterator.setDoNotCheckIfItExists(true);
		
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX41));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				"Searching (related) with book/page list from all " + Arrays.toString(relatedSourceDoctype));
		module.clearSaKeys();
		module.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
		module.setIteratorType(1, FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
		module.addIterator(instrumentRelatedBPIterator);
		if (isUpdate()) {
			module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
		}
		module.addFilter(FLSubdividedBasedDASLDT.getLegalOrDocTypeFilter(searchId , true));
		module.addFilter(new PropertyTypeFilter(searchId));
		modules.add(module);
		
		
		addRelatedInstrumentNoSearch(serverInfo, modules, isUpdate(), relatedSourceDoctype);
		
	}

	public void addRelatedInstrumentNoSearch(TSServerInfo serverInfo, List<TSServerInfoModule> modules,
			boolean isUpdate, String[] relatedSourceDoctype) {
		TSServerInfoModule module;
		InstrumentGenericIterator instrumentRelatedNoIterator = getInstrumentOrBookTypeIterator(true);
		instrumentRelatedNoIterator.enableInstrumentNumber();
		instrumentRelatedNoIterator.setLoadFromRoLike(true);
		instrumentRelatedNoIterator.setRoDoctypesToLoad(relatedSourceDoctype);
		instrumentRelatedNoIterator.setDoNotCheckIfItExists(true);
		instrumentRelatedNoIterator.setInitAgain(true);
		
		module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.MODULE_IDX41));
		module.addExtraInformation(TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION, 
				"Searching (related) with Instrument Number list from all " + Arrays.toString(relatedSourceDoctype));
		module.clearSaKeys();
		module.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE);
		module.setIteratorType(4, FunctionStatesIterator.ITERATOR_TYPE_YEAR);
		module.addIterator(instrumentRelatedNoIterator);
		if (isUpdate) {
			module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
		}
		module.addFilter(FLSubdividedBasedDASLDT.getLegalOrDocTypeFilter(searchId , true));
		module.addFilter(new PropertyTypeFilter(searchId));
		modules.add(module);
	}
	
	protected InstrumentGenericIterator getInstrumentIterator() {
		InstrumentGenericIterator instrumentBPIterator = new InstrumentGenericIterator(searchId, dataSite);
		return instrumentBPIterator;
	}

	public InstrumentGenericIterator getInstrumentOrBookTypeIterator(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId, dataSite) {

			private static final long serialVersionUID = 5399351945130601258L;
			
		};
		if (instrumentType) {
			instrumentGenericIterator.enableInstrumentNumber();
		} else {
			instrumentGenericIterator.enableBookPage();
			instrumentGenericIterator.setRemoveLeadingZerosBP(true);
		}

		return instrumentGenericIterator;
	}
	
	@Override
	protected String prepareInstrumentYearForReferenceSearch(InstrumentI inst) {
		return String.valueOf(inst.getYear());
	}

	@Override
	protected  boolean addInstNoSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, Set<String> searched, boolean isUpdate){
		if ( inst.hasInstrNo() && inst.getYear() != SimpleChapterUtils.UNDEFINED_YEAR){
			String instr = inst.getInstno().replaceFirst("^0+", "");
			
			String year1 = String.valueOf(inst.getYear());
			if(!searched.contains(instr+year1)){
				searched.add(instr+year1);
			}else{
				return false;
			}
			
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.setData(0, instr);
			module.setData(1, inst.getYear()+"");
			if (isUpdate) {
				module.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			modules.add(module);
			return true;
		}
		return false;
	}
	
}
