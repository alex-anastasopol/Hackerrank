package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.module.OcrOrBootStraperIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;

@SuppressWarnings("deprecation")
public class COHuerfanoRO extends GenericCountyRecorderROImageTS {
	
	private static final long serialVersionUID = -3885460956713789709L;
	
	public COHuerfanoRO(long searchId) {
		super(searchId);
	}

	public COHuerfanoRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	@Override
	protected boolean hasBookTypeIterator() {
		return false;
	}
	
	@Override
	protected InstrumentGenericIterator getBookPageIterator() {
		
		InstrumentGenericIterator iterator = new InstrumentGenericIterator(searchId) {
			
			private static final long serialVersionUID = 1373318525077079932L;

			@Override
			protected void loadDerrivation(TSServerInfoModule module, InstrumentI state) {
				
				List<FilterResponse> allFilters = module.getFilterList();
				GenericInstrumentFilter gif = null;
				HashMap<String, String> filterCriteria = null;
				if(allFilters != null) {
					for (FilterResponse filterResponse : allFilters) {
						if (filterResponse instanceof GenericInstrumentFilter) {
							gif = (GenericInstrumentFilter) filterResponse;
							filterCriteria = new HashMap<String, String>();
							gif.clearFilters();
						}
					}
				}
				for (Object functionObject : module.getFunctionList()) {
					if (functionObject instanceof TSServerInfoFunction) {
						TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
						if (function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE) {
							String book = getBookFrom(state, filterCriteria);
							String bookType = "1";	//GENERAL
							if (book.matches("(?i)[A-Z]+")) {
								bookType = "8";		//MAPS
							}
							function.setParamValue(bookType);
						}
						if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE) {
							function.setParamValue(getBookFrom(state, filterCriteria));
						} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE) {
							function.setParamValue(getPageFrom(state, filterCriteria));
						} 
					}
				}
				if(gif != null) {
					gif.addDocumentCriteria(filterCriteria);
				}
			}
			
		};
		
		return iterator;
	}
	
	@Override
	protected OcrOrBootStraperIterator getOcrBookPageIterator() {
		
		OcrOrBootStraperIterator iterator = new OcrOrBootStraperIterator(searchId) {
			
			private static final long serialVersionUID = -2338882572249238814L;

			@Override
			public Object current(){
		        Instrument instr = ((Instrument) getStrategy().current());
		        
		        TSServerInfoModule crtState = new TSServerInfoModule(initialState);
		        
		        List<FilterResponse> allFilters = crtState.getFilterList();
				GenericInstrumentFilter gif = null;
				HashMap<String, String> filterCriteria = null;
				if(allFilters != null) {
					for (FilterResponse filterResponse : allFilters) {
						if (filterResponse instanceof GenericInstrumentFilter) {
							gif = (GenericInstrumentFilter) filterResponse;
							filterCriteria = new HashMap<String, String>();
							gif.clearFilters();
						}
					}
				}
		        
		        for (int i =0; i< crtState.getFunctionCount(); i++){
		            TSServerInfoFunction fct = crtState.getFunction(i);
		            if( "".equals( instr.getInstrumentNo() ) ){
		            	if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE) {
							String book = instr.getBookNo();
							String bookType = "1";	//GENERAL
							if (book.matches("(?i)[A-Z]+")) {
								bookType = "8";		//MAPS
							}
							fct.setParamValue(bookType);
						}
		            	if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH) {
			                
			                fct.setParamValue( instr.getBookNo() );
			                if(filterCriteria != null) {
								filterCriteria.put("Book", instr.getBookNo());
							}
			            }
			            else if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH) {
			                fct.setParamValue( instr.getPageNo() );
			                if(filterCriteria != null) {
								filterCriteria.put("Page", instr.getPageNo());
							}
			            }
			        }
		        }
		        if(gif != null) {
					gif.addDocumentCriteria(filterCriteria);
				}
		        return  crtState ;
		    }
			
		};
		
		return iterator;
	}
	
	@Override
	public List<TSServerInfoModule> getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		
		if(restoreDocumentDataI == null) {
			return null;
		}
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		TSServerInfoModule module = null;
		String instrumentNumber = restoreDocumentDataI.getInstrumentNumber();
		String book = restoreDocumentDataI.getBook();
		String page = restoreDocumentDataI.getPage();
		
		if (StringUtils.isNotEmpty(instrumentNumber)) {
			module = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.forceValue(0, instrumentNumber);
			module.getFilterList().clear();
			modules.add(module);
		}
		
		if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
			module = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
			String bookType = "1";	//GENERAL
			if (book.matches("(?i)[A-Z]+")) {
				bookType = "8";		//MAPS
			}
			module.forceValue(0, bookType);
			module.forceValue(1, book);
			module.forceValue(2, page);
			modules.add(module);
		} 
			
		//module for document with "Sensitive" instrument number
		//this type of documents are recovered using Tracking ID module 
		if (StringUtils.isNotEmpty(instrumentNumber)) {
			module = new TSServerInfoModule(getDefaultServerInfo().getModule(TSServerInfo.MODULE_IDX38));
			module.forceValue(0, instrumentNumber);
			module.getFilterList().clear();
			modules.add(module);
		}
		
		return modules;
	}
	
}
