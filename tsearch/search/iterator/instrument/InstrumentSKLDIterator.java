package ro.cst.tsearch.search.iterator.instrument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterWithDoctype;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.types.GenericSKLD;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.SKLDInstrument;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

public class InstrumentSKLDIterator extends InstrumentGenericIterator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected Map<String, RegisterDocumentI> referenceParentMapper;
	
	public InstrumentSKLDIterator(long searchId) {
		super(searchId);
	}
	public InstrumentSKLDIterator(long searchId, boolean enableBookPage) {
		super(searchId);
		if(enableBookPage) {
			enableBookPage();
		} else {
			enableInstrumentNumber();
		}
		
	}

	@Override
	public List<InstrumentI> createDerrivations() {
		
		Search global = getSearch();
		int countyId = Integer.parseInt(global.getCountyId());
		
		List<InstrumentI> result = new Vector<InstrumentI>();
		HashSet<String> listsForNow = new HashSet<String>();
		DocumentsManagerI manager = global.getDocManager();
		boolean addReferences = true;
		try{
			manager.getAccess();
			List<DocumentI> list = null;
			if(!isLoadFromRoLike()) {
				list = manager.getDocumentsWithType( true, DType.ASSESOR, DType.TAX );
			} else {
				list = new ArrayList<DocumentI>();
				list.addAll(manager.getDocumentsWithDataSource(true, "SK"));
				referenceParentMapper = new HashMap<String, RegisterDocumentI>();
				
			}
			for(DocumentI documentI:list){
				addReferences = true;
				if (documentI.isOneOf(DType.ASSESOR, DType.TAX) && !HashCountyToIndex.isLegalBootstrapEnabled(global.getCommId(), documentI.getSiteId())) {
					addReferences = false;
				}
				if (addReferences) {
					for(RegisterDocumentI reg : documentI.getReferences()){
						try {
							InstrumentI instrumentIClone = reg.getInstrument().clone();
							
							if(instrumentIClone instanceof SKLDInstrument) {
								SKLDInstrument instrumentI = (SKLDInstrument)instrumentIClone;
					    		if(!isEnableBookPage()) {
					    			String instrumentNo = instrumentI.getOriginalInstrumentNo();
						    		String instrumentYear = Integer.toString(instrumentI.getYear());
									if(instrumentI.getYear() == SimpleChapterUtils.UNDEFINED_YEAR) {
										instrumentYear = "*";
									}
						    		if(StringUtils.isNotEmpty(instrumentNo)) {
						    			if(instrumentNo.matches("\\d+-\\d{4}") && instrumentI.getYear() == SimpleChapterUtils.UNDEFINED_YEAR) {
						    				int indexLastPos = instrumentNo.indexOf("-");
						    				instrumentYear = instrumentNo.substring(indexLastPos + 1);
						    				instrumentNo = instrumentNo.substring(0,indexLastPos);
						    				instrumentI.setYear(Integer.parseInt(instrumentYear));
						    			}
					    				instrumentNo = cleanInstrumentNo(instrumentNo, instrumentYear, countyId);
					    				String key = "Instrument=" + instrumentNo + "___year=" + instrumentYear;
					    				if(StringUtils.isNotEmpty(instrumentNo) && !listsForNow.contains(key)) {
					    					if("*".equals(instrumentYear)) {
					    						instrumentI.setInstno(instrumentNo);
					    					} else {
					    						instrumentI.setInstno(instrumentNo + "-" + instrumentYear);
					    					}
					    					List<DocumentI> almostLike = manager.getDocumentsWithInstrumentsFlexible(false, instrumentI);
					    	    			if(almostLike.isEmpty()) {
					    	    				if(isLoadFromRoLike() && referenceParentMapper != null) {
					    	    					if(documentI instanceof RegisterDocumentI) {
					    	    						referenceParentMapper.put(key, (RegisterDocumentI)documentI);
					    	    					}
					    	    				}
					    	    				listsForNow.add(key);
					    	    				instrumentI.setInstno(instrumentNo);
					    	    				result.add(instrumentI);
					    	    			}
					    				}
						    		}
					    		} else {
					    			String book = instrumentI.getBook();
					    			String page = instrumentI.getPage();
					    			if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
					    				String key = "Book=" + book + "_Page=" + page;
					    				if(!listsForNow.contains(key)) {
					    					//if has the same instrumentNumber we will not save it
					    					List<DocumentI> almostLike = manager.getDocumentsWithInstrumentsFlexible(false, instrumentI);
					    	    			if(almostLike.isEmpty()) {
					    	    				if(isLoadFromRoLike() && referenceParentMapper != null) {
					    	    					if(documentI instanceof RegisterDocumentI) {
					    	    						referenceParentMapper.put(key, (RegisterDocumentI)documentI);
					    	    					}
					    	    				}
					    	    				listsForNow.add(key);
					    	    				result.add(instrumentI);
					    	    			}
					    				}
					    			}
					    		}
							
							} else {
								InstrumentI instrumentI = instrumentIClone;
					    		if(!isEnableBookPage()) {
					    			String instrumentNo = instrumentI.getInstno();
						    		String instrumentYear = Integer.toString(instrumentI.getYear());
									if(instrumentI.getYear() == SimpleChapterUtils.UNDEFINED_YEAR) {
										instrumentYear = "*";
									}
						    		if(StringUtils.isNotEmpty(instrumentNo)) {
						    			if(instrumentNo.matches("\\d+-\\d{4}") && instrumentI.getYear() == SimpleChapterUtils.UNDEFINED_YEAR) {
						    				int indexLastPos = instrumentNo.indexOf("-");
						    				instrumentYear = instrumentNo.substring(indexLastPos + 1);
						    				instrumentNo = instrumentNo.substring(0,indexLastPos);
						    				instrumentI.setYear(Integer.parseInt(instrumentYear));
						    			}
					    				instrumentNo = cleanInstrumentNo(instrumentNo, instrumentYear, countyId);
					    				String key = "Instrument=" + instrumentNo + "___year=" + instrumentYear;
					    				if(StringUtils.isNotEmpty(instrumentNo) && !listsForNow.contains(key)) {
					    					if("*".equals(instrumentYear)) {
					    						instrumentI.setInstno(instrumentNo);
					    					} else {
					    						instrumentI.setInstno(instrumentNo + "-" + instrumentYear);
					    					}
					    					List<DocumentI> almostLike = manager.getDocumentsWithInstrumentsFlexible(false, instrumentI);
					    	    			if(almostLike.isEmpty()) {
					    	    				if(isLoadFromRoLike() && referenceParentMapper != null) {
					    	    					if(documentI instanceof RegisterDocumentI) {
					    	    						referenceParentMapper.put(key, (RegisterDocumentI)documentI);
					    	    					}
					    	    				}
					    	    				listsForNow.add(key);
					    	    				instrumentI.setInstno(instrumentNo);
					    	    				result.add(instrumentI);
					    	    			}
					    				}
						    		}
					    		} else {
					    			String book = instrumentI.getBook();
					    			String page = instrumentI.getPage();
					    			if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
					    				String key = "Book=" + book + "_Page=" + page;
					    				if(!listsForNow.contains(key)) {
					    					//if has the same instrumentNumber we will not save it
					    					List<DocumentI> almostLike = manager.getDocumentsWithInstrumentsFlexible(false, instrumentI);
					    	    			if(almostLike.isEmpty()) {
					    	    				if(isLoadFromRoLike() && referenceParentMapper != null) {
					    	    					if(documentI instanceof RegisterDocumentI) {
					    	    						referenceParentMapper.put(key, (RegisterDocumentI)documentI);
					    	    					}
					    	    				}
					    	    				listsForNow.add(key);
					    	    				result.add(instrumentI);
					    	    			}
					    				}
					    			}
					    		}
							}
							
						} catch (Exception e) {
							GenericSKLD.getLogger().error("Error while processing References for searchId: " + 
										global.getID() + " from document " + documentI, e);
						}
			    		
					}
					for (InstrumentI instrumentIToIterate : documentI.getParsedReferences()) {
						try {
							InstrumentI instrumentIClone = instrumentIToIterate.clone();
							if(instrumentIClone instanceof SKLDInstrument) {
								SKLDInstrument instrumentI = (SKLDInstrument)instrumentIClone;
								if(!isEnableBookPage()) {
									String instrumentNo = instrumentI.getOriginalInstrumentNo();
						    		String instrumentYear = Integer.toString(instrumentI.getYear());
									if(instrumentI.getYear() == SimpleChapterUtils.UNDEFINED_YEAR) {
										instrumentYear = "*";
									}
						    		if(StringUtils.isNotEmpty(instrumentNo)) {
						    			if(instrumentNo.matches("\\d+-\\d{4}") && instrumentI.getYear() == SimpleChapterUtils.UNDEFINED_YEAR) {
						    				int indexLastPos = instrumentNo.indexOf("-");
						    				instrumentYear = instrumentNo.substring(indexLastPos + 1);
						    				instrumentNo = instrumentNo.substring(0,indexLastPos);
						    				instrumentI.setYear(Integer.parseInt(instrumentYear));
						    				
						    			}
					    				instrumentNo = cleanInstrumentNo(instrumentNo, instrumentYear, countyId);
					    				String key = "Instrument=" + instrumentNo + "___year=" + instrumentYear;
					    				if(StringUtils.isNotEmpty(instrumentNo) && !listsForNow.contains(key)) {
					    					if("*".equals(instrumentYear)) {
					    						instrumentI.setInstno(instrumentNo);
					    					} else {
					    						instrumentI.setInstno(instrumentNo + "-" + instrumentYear);
					    					}
					    					List<DocumentI> almostLike = manager.getDocumentsWithInstrumentsFlexible(false, instrumentI);
					    	    			if(almostLike.isEmpty()) {
					    	    				if(isLoadFromRoLike() && referenceParentMapper != null) {
					    	    					if(documentI instanceof RegisterDocumentI) {
					    	    						referenceParentMapper.put(key, (RegisterDocumentI)documentI);
					    	    					}
					    	    				}
					    	    				listsForNow.add(key);
					    	    				instrumentI.setInstno(instrumentNo);
					    	    				result.add(instrumentI);
					    	    			}
					    				}
						    		} 
						    	} else {
					    			String book = instrumentI.getBook();
					    			String page = instrumentI.getPage();
					    			if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
					    				String key = "Book=" + book + "_Page=" + page;
					    				if(!listsForNow.contains(key)) {
					    					//if has the same instrumentNumber we will not save it
					    					List<DocumentI> almostLike = manager.getDocumentsWithInstrumentsFlexible(false, instrumentI);
					    	    			if(almostLike.isEmpty()) {
					    	    				if(isLoadFromRoLike() && referenceParentMapper != null) {
					    	    					if(documentI instanceof RegisterDocumentI) {
					    	    						referenceParentMapper.put(key, (RegisterDocumentI)documentI);
					    	    					}
					    	    				}
					    	    				listsForNow.add(key);
					    	    				result.add(instrumentI);
					    	    			}
					    				}
						    		}
								}
							} else {
								InstrumentI instrumentI = instrumentIClone;
								if(!isEnableBookPage()) {
									String instrumentNo = instrumentI.getInstno();
						    		String instrumentYear = Integer.toString(instrumentI.getYear());
									if(instrumentI.getYear() == SimpleChapterUtils.UNDEFINED_YEAR) {
										instrumentYear = "*";
									}
						    		if(StringUtils.isNotEmpty(instrumentNo)) {
						    			if(instrumentNo.matches("\\d+-\\d{4}") && instrumentI.getYear() == SimpleChapterUtils.UNDEFINED_YEAR) {
						    				int indexLastPos = instrumentNo.indexOf("-");
						    				instrumentYear = instrumentNo.substring(indexLastPos + 1);
						    				instrumentNo = instrumentNo.substring(0,indexLastPos);
						    				instrumentI.setYear(Integer.parseInt(instrumentYear));
						    				
						    			}
					    				instrumentNo = cleanInstrumentNo(instrumentNo, instrumentYear, countyId);
					    				String key = "Instrument=" + instrumentNo + "___year=" + instrumentYear;
					    				if(StringUtils.isNotEmpty(instrumentNo) && !listsForNow.contains(key)) {
					    					if("*".equals(instrumentYear)) {
					    						instrumentI.setInstno(instrumentNo);
					    					} else {
					    						instrumentI.setInstno(instrumentNo + "-" + instrumentYear);
					    					}
					    					List<DocumentI> almostLike = manager.getDocumentsWithInstrumentsFlexible(false, instrumentI);
					    	    			if(almostLike.isEmpty()) {
					    	    				if(isLoadFromRoLike() && referenceParentMapper != null) {
					    	    					if(documentI instanceof RegisterDocumentI) {
					    	    						referenceParentMapper.put(key, (RegisterDocumentI)documentI);
					    	    					}
					    	    				}
					    	    				listsForNow.add(key);
					    	    				instrumentI.setInstno(instrumentNo);
					    	    				result.add(instrumentI);
					    	    			}
					    				}
						    		} 
						    	} else {
					    			String book = instrumentI.getBook();
					    			String page = instrumentI.getPage();
					    			if(StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
					    				String key = "Book=" + book + "_Page=" + page;
					    				if(!listsForNow.contains(key)) {
					    					//if has the same instrumentNumber we will not save it
					    					List<DocumentI> almostLike = manager.getDocumentsWithInstrumentsFlexible(false, instrumentI);
					    	    			if(almostLike.isEmpty()) {
					    	    				if(isLoadFromRoLike() && referenceParentMapper != null) {
					    	    					if(documentI instanceof RegisterDocumentI) {
					    	    						referenceParentMapper.put(key, (RegisterDocumentI)documentI);
					    	    					}
					    	    				}
					    	    				listsForNow.add(key);
					    	    				result.add(instrumentI);
					    	    			}
					    				}
						    		}
								}
							}
						} catch (Exception e) {
							GenericSKLD.getLogger().error("Error while processing Parsed References for searchId: " + 
										global.getID() + " from document " + documentI, e);
						}
					}
				}
			}
		}
		finally {
			manager.releaseAccess();
		}
		
		return result;
	}
	
	private static String cleanInstrumentNo(String instrumentNo, String instrumentYear, int countyId) {
		if(StringUtils.isNotEmpty(instrumentNo)  && StringUtils.isNotEmpty(instrumentYear)) {
			if (countyId==CountyConstants.CO_Adams) {
				//20060202000116600 -> 116600 (remove year, month, and day)
				instrumentNo = instrumentNo.replaceFirst("^\\d{8}(\\d{9})$", "$1");
				//remove four-digits-year
				instrumentNo = instrumentNo.replaceFirst("^\\d{4}(\\d{9})$", "$1");
				//06000116590 -> 116590 (remove two-digits-year)
				instrumentNo = instrumentNo.replaceFirst("^\\d{2}(\\d{9})$", "$1");
			} else if (countyId==CountyConstants.CO_Eagle) {
				//201121282 -> 21282 (remove year)
				instrumentNo = instrumentNo.replaceFirst("^\\d{4}(\\d{5})", "$1");
			} else if (countyId==CountyConstants.CO_El_Paso) {
				//213150810 -> 150810 (remove year)
				instrumentNo = instrumentNo.replaceFirst("^[02]?\\d{2}(\\d{6})", "$1");
			} else {
				instrumentNo = instrumentNo.replaceAll("(?i)^[A-Z]+0*", "");
				if(instrumentNo.startsWith(instrumentYear)) {
					instrumentNo = instrumentNo.replaceFirst(instrumentYear, "");
				} else if (instrumentNo.length() > 7 && instrumentYear.length() == 4 && 
						instrumentNo.startsWith(instrumentYear.substring(2))) {
					instrumentNo = instrumentNo.substring(2);	//remove just the year hopefully
				} else if (instrumentYear.length() == 4) {
					// Transform 2012 to 212
					String year = instrumentYear.charAt(0) + instrumentYear.substring(2);
					// Transform 212001226 to 001226
					instrumentNo = instrumentNo.replaceFirst("^"+year, "");
				}
			}
			instrumentNo = instrumentNo.replaceFirst("^0+", "");
			instrumentNo = instrumentNo.replaceAll("(?i)^[A-Z]+0*", "");
			instrumentNo = instrumentNo.replaceFirst("^0+", "");
		}
		return instrumentNo;
	}

	@Override
	protected void loadDerrivation(TSServerInfoModule module, InstrumentI state) {
		
		for (Object functionObject : module.getFunctionList()) {
			if (functionObject instanceof TSServerInfoFunction) {
				TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
				if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH) {
					function.setParamValue(state.getInstno());
				} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_YEAR) {
					if(state.getYear() != SimpleChapterUtils.UNDEFINED_YEAR) {
						function.setParamValue(Integer.toString(state.getYear()));
					} else {
						function.setParamValue("*");
					}
					
					if(isLoadFromRoLike()) {
						String key = "Instrument=" + state.getInstno() + "___year=" + function.getParamValue();
						addDateFilterWithDoctype(module, key);
					}
				} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE) {
					function.setParamValue(state.getBook());
				} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE) {
					function.setParamValue(state.getPage());
					if(isLoadFromRoLike()) {
						String key = "Book=" + state.getBook() + "_Page=" + state.getPage();
						addDateFilterWithDoctype(module, key);
					}
				}
			}
		}
	}
	
	protected void addDateFilterWithDoctype(TSServerInfoModule module,
			String key) {
		List<FilterResponse> allFilters = module.getFilterList();
		DateFilterWithDoctype dateFilterWithDoctype = null;
		if(allFilters != null) {
			for (FilterResponse filterResponse : allFilters) {
				if (filterResponse instanceof DateFilterWithDoctype) {
					dateFilterWithDoctype = (DateFilterWithDoctype) filterResponse;
					dateFilterWithDoctype.setReferenceDate(null);
				}
			}
		}
		if(referenceParentMapper != null) {
			RegisterDocumentI instrumentI = referenceParentMapper.get(key);
			if(instrumentI != null) {
				if(instrumentI.getDocType().equals("RELEASE") || 
						instrumentI.getDocType().equals("ASSIGNMENT") ||
						instrumentI.getDocType().equals("SUBORDINATION") ||
						instrumentI.getDocType().equals("MODIFICATION")) {
					
					
					
					
					if(dateFilterWithDoctype != null) {
						Set<String> applyToDoctypes = new HashSet<String>();
						applyToDoctypes.add("MORTGAGE");
						applyToDoctypes.add("LIEN");
						dateFilterWithDoctype
							.setAllowNewerAndForbidOlder(false)
							.setApplyToDoctypes(applyToDoctypes)
							.setReferenceDate(instrumentI.getRecordedDate());
					}
				}
			}
		}
	}
	
	public static void main(String[] args) {
		String[] rows = {"JsT0123131","R2132","T21000","F21312"};
		for (int i = 0; i < rows.length; i++) {
			System.out.println(rows[i] + " --- " + rows[i].replaceAll("(?i)^[A-Z]+0*", ""));
		}
		
		System.out.println("C0361951 - 1998 ==> " + cleanInstrumentNo("C0361951", "1998", -1));
		System.out.println("C1004235 - 2002 ==> " + cleanInstrumentNo("C1004235", "2002", -1));
		System.out.println("08000062082 - 2008 ==> " + cleanInstrumentNo("08000062082", "2008", -1));
		
		
	}

}
