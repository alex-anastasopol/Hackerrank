package ro.cst.tsearch.servers.types;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.search.module.OcrOrBootStraperIterator;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.utils.SearchLogger;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.datatree.DataTreeImageException;

public class COLarimerDT extends FLSubdividedBasedDASLDT{

	private static final long serialVersionUID = 3428089738835211318L;
	
	
	public COLarimerDT(long searchId) {
		super(searchId);
	}
	
	public COLarimerDT(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	public void addAssesorMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}

	@Override
	public void addPlatMapSearch(TSServerInfoModule module,PersonalDataStruct str) {}
	
	@Override
	public void addParcelMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
	@Override
	public void addSubdivisionMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
	@Override
	public void addCondoMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
	@Override
	public boolean addAoLookUpSearches(TSServerInfo serverInfo, List<TSServerInfoModule> modules, Set<InstrumentI> allAoRef,long searchId,  boolean isUpdate, boolean isTimeShare){
		boolean atLeastOne = false;
		final Set<String> searched = new HashSet<String>();
		
		for(InstrumentI inst:allAoRef){
			if( inst.hasInstrNo() ){
				boolean temp = addInstNoSearch(inst, serverInfo, modules, searchId, searched, isUpdate);
				atLeastOne = atLeastOne || temp;
			}
		}
		
		return atLeastOne;
	}
	
	@Override
	protected boolean addInstNoSearch(InstrumentI inst, TSServerInfo serverInfo, List<TSServerInfoModule> modules, long searchId,Set<String> searched, boolean isUpdate) {
		inst = inst.clone();
		if(inst.hasInstrNo()){
			int year = -1;
			String instNo = inst.getInstno();
			if(instNo.length()>=10){
				try{year = Integer.parseInt(instNo.substring(0,4));}catch(Exception e){}
				if(year>=2000){
					instNo = instNo.substring(4).replaceFirst("^0+", "");
					inst.setInstno(instNo);
				}
			}else if(instNo.length()>=8){
				try{year = Integer.parseInt(instNo.substring(0,4));}catch(Exception e){}
				if(year>=80){
					instNo = instNo.substring(2).replaceFirst("^0+", "");
					inst.setInstno(instNo);
				}
			}
		}
		return super.addInstNoSearch(inst, serverInfo, modules, searchId, searched, isUpdate);
	}
	
	protected InstrumentGenericIterator getInstrumentIterator() {
		InstrumentGenericIterator instrumentBPIterator = new InstrumentGenericIterator(searchId){
			private static final long serialVersionUID = -524770912234911421L;

			@Override
			protected String cleanInstrumentNo(String instno, int year) {
				
				if(instno.length() <= 6) {
					return instno.replaceFirst("^0+", "");
				}
				String yearAsString = Integer.toString(year);
				if(instno.startsWith(yearAsString)) {
					return instno.substring(4).replaceFirst("^0+", "");
				} else if(instno.startsWith(yearAsString.substring(2))) {
					return instno.substring(2).replaceFirst("^0+", "");
				}
				
				return instno.replaceFirst("^0+", "");
			}
		};
		
		return instrumentBPIterator;
	}
	
	
	protected boolean downloadImageFromOtherSiteImpl(InstrumentI instrument, ImageLinkInPage image, String commId) {

		if (datTreeList == null) {
			datTreeList = initDataTreeStruct();
		}

		boolean status = false;

		try {
			status = downloadImageFromDataTree(instrument, datTreeList, image.getPath(), commId, null, null);
			if (status) {
				SearchLogger.info("<br/>Image(searchId=" + searchId + " )book=" + instrument.getBook() + "page="
						+ instrument.getPage() + "inst=" + instrument.getInstno() + " was taken from DataTree<br/>",
						searchId);
			}
		} catch (DataTreeImageException e) {
			logger.error("Error while getting image ", e);
			SearchLogger.info(
					"<br/>FAILED to take Image(searchId="+searchId+" ) book=" +
					instrument.getBook()+" page="+instrument.getPage()+" inst="+
					instrument.getInstno()+" from DataTree. "+
					"Official Server Message: [" + e.getLocalizedMessage() + " (" + e.getStatus() + ") ]<br/>", searchId);
		}
		afterDownloadImage(status);
		return status;

	}	
	
	public static void processInstrumentNo(InstrumentI instr) {
		try {
			String instNo = instr.getInstno();
			if (instr.hasYear()) {
				if (instr.getYear() >= 2003) {
					instNo = instr.getYear() + StringUtils.leftPad(instNo, 7, "0");
				} else if (instr.getYear() >= 2000) {
					instNo = instr.getYear() + StringUtils.leftPad(instNo, 6, "0");
				} else if (instr.getYear() >= 1981) {
					instNo = (instr.getYear() + StringUtils.leftPad(instNo, 6, "0")).substring(2);
				}
				instr.setInstno(instNo);
			}
			instr.setEnableInstrNoTailMatch(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void processInstrumentBeforeAdd(DocumentI doc) {
		try {
//			String instNo = doc.getInstno();
//			if(doc.hasYear()) {
//				if(doc.getYear() >= 2003) {
//					instNo = doc.getYear() + StringUtils.leftPad(instNo, 7, "0");
//				} else if(doc.getYear() >= 2000) {
//					instNo = doc.getYear() + StringUtils.leftPad(instNo, 6, "0");
//				} else if(doc.getYear() >= 1981) {
//					instNo = (doc.getYear() + StringUtils.leftPad(instNo, 6, "0")).substring(2);
//				}
//				doc.setInstno(instNo);
//			}
//			doc.getInstrument().setEnableInstrNoTailMatch(true);
			processInstrumentNo(doc.getInstrument());
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void addOCRSearch(List<TSServerInfoModule> modules,TSServerInfo serverInfo, FilterResponse ...filters){
		// OCR last transfer - book / page search
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
	    module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
	    module.clearSaKeys();
	    module.getFunction(0).setIteratorType( FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH );
	    module.getFunction(1).setIteratorType( FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH );
	    for(int i=0;i<filters.length;i++){
	    	module.addFilter(filters[i]);
	    }
	    addBetweenDateTest(module, false, false, false);
		modules.add(module);
		
	    // OCR last transfer - instrument search
	    module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
	    //module.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
	    module.clearSaKeys();
	    module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH);
	    module.getFunction(2).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_YEAR);
	    for(int i=0;i<filters.length;i++){
	    	module.addFilter(filters[i]);
	    }
	    
	    OcrOrBootStraperIterator ocrBPIteratoriterator = new OcrOrBootStraperIterator(searchId) {
	    	private static final long serialVersionUID = 1L;
	    	
	    	
	    	@SuppressWarnings("deprecation")
			@Override
	    	public Object current() {
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
	                else{
	    	            if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH) {
	    	            	String instno = instr.getInstrumentNo();
	    	            	if(instno.length() <= 6) {
	    	            		instno = instno.replaceFirst("^0+", "");
	    					} else if(instno.length() <= 8){
	    						instno = instno.substring(2).replaceFirst("^0+", "");
	    					} else {
	    						instno = instno.substring(4).replaceFirst("^0+", "");
	    					}
	    	                fct.setParamValue( instno );
	    	                if(filterCriteria != null) {
	    						filterCriteria.put("InstrumentNumber", instno);
	    					}
	    	            } else if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_YEAR) {
	    	            	String instno = instr.getInstrumentNo();
	    	            	String year = null;
	    	            	if(instno.length() <= 6) {
	    	            		year = "";
	    					} else if(instno.length() <= 8){
	    						year = "19" + instno.substring(0, 2);
	    					} else {
	    						year = instno.substring(0, 4);
	    					}
	    	                fct.setParamValue( year );
	    	            }
	                }
	            }
	            if(gif != null) {
	    			gif.addDocumentCriteria(filterCriteria);
	    		}
	            return  crtState ;
	    	}
	    	
	    };
	    ocrBPIteratoriterator.setSearchIfPresent(false);
	    ocrBPIteratoriterator.setInitAgain(true);
    	module.addIterator(ocrBPIteratoriterator);
    	
	    
	    
	    addBetweenDateTest(module, false, false, false);
		modules.add(module);
	}
	
}
