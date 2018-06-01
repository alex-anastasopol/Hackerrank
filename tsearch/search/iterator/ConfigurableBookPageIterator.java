package ro.cst.tsearch.search.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.search.module.OcrOrBootStraperIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

public class ConfigurableBookPageIterator extends GenericRuntimeIterator<InstrumentI> {

	private static final long serialVersionUID = 1L;

	protected boolean loadFromSearchAttributes = true;
	protected boolean loadFromAoDocuments = true;
	protected boolean loadFromRoLikeDocuments = false;
	protected boolean loadFromOcr = false;
	protected boolean alsoSearchWithoutLeadingZeroes = false;
	protected boolean onlySearchWithPrefixesAdded = false;
	
	protected String bookPrefix = "";
	protected String pagePrefix = "";
	
	protected List<String> removeBookPrefixes = new ArrayList<String>();
	
	public ConfigurableBookPageIterator(long searchId) {
		super(searchId);
	}
	
	@Override
	protected List<InstrumentI> createDerrivations() {
		
		Search global = getSearch();
		
		LinkedHashSet<InstrumentI> baseDerivations = new LinkedHashSet<InstrumentI>();		
		LinkedHashSet<InstrumentI> finalDerivations = new LinkedHashSet<InstrumentI>();
			
		if(loadFromSearchAttributes) {
			List<InstrumentI> list = extractBookPageList(SearchAttributes.LD_BOOKPAGE);

			for(InstrumentI instrument : list) {
				if(StringUtils.isNotEmpty(instrument.getBook()) && StringUtils.isNotEmpty(instrument.getPage())) {
					baseDerivations.add(instrument);
				}
			}
		}
		
		
		if(loadFromAoDocuments) {
			DocumentsManagerI documentsManagerI = global.getDocManager();
			try {
				documentsManagerI.getAccess();
				List<DocumentI> assesorDocuments = documentsManagerI.getDocumentsWithType(DType.ASSESOR);
				for (DocumentI documentI : assesorDocuments) {
					if (HashCountyToIndex.isLegalBootstrapEnabled(global.getCommId(), documentI.getSiteId())) {
						for(InstrumentI ref : documentI.getReferences()) {
							baseDerivations.add(getBookPage(ref));
						}
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				documentsManagerI.releaseAccess();
			}
		}
		
		if(loadFromRoLikeDocuments){
			DocumentsManagerI documentsManagerI = global.getDocManager();
			try {
				documentsManagerI.getAccess();
				List<RegisterDocumentI> assesorDocuments = documentsManagerI.getRoLikeDocumentList();
				for (RegisterDocumentI regDocI : assesorDocuments) {
					for(InstrumentI ref : regDocI.getParsedReferences()) {
						baseDerivations.add(ref);
					}
				}
				
			}finally {
				documentsManagerI.releaseAccess();
			}
		}
		
		if(loadFromOcr) {
			OcrOrBootStraperIterator ocrOrBootStraperIterator = new OcrOrBootStraperIterator(alsoSearchWithoutLeadingZeroes, searchId);
			List<ro.cst.tsearch.propertyInformation.Instrument> ocrInstruments = 
				ocrOrBootStraperIterator.extractInstrumentNoList(initialState);
			for(ro.cst.tsearch.propertyInformation.Instrument instr : ocrInstruments ) {
				baseDerivations.add(instr.toInstrumet());
			}
		}
		
		finalDerivations.addAll(baseDerivations);
		
		if(!removeBookPrefixes.isEmpty()) { 
			for(InstrumentI instrument : baseDerivations) {
				if(instrument.hasBookPage()) {
					String book = instrument.getBook();
					if(!StringUtils.isEmpty(book)) {
						for(String removeBookPrefix : removeBookPrefixes) {
							if(book.startsWith(removeBookPrefix)) {
								book = book.replaceFirst(removeBookPrefix, "");
								instrument.setBook(book);
							}
						}
					}
				}
			}
		}
		
		if(!bookPrefix.isEmpty()) {
			for(InstrumentI instrument : baseDerivations) {
				if(instrument.hasBookPage()) {
					if(onlySearchWithPrefixesAdded) {
						finalDerivations.remove(instrument);	
					}
					finalDerivations.add(addBookPrefix(instrument));
				}
			}
		}
		
		if(alsoSearchWithoutLeadingZeroes) {
			for(InstrumentI instrument : baseDerivations) {	
				finalDerivations.add(removeLeadingZeros(instrument));
			}
		}
				
		if(!searchIfPresent){
			removeAlreadyRetrieved(finalDerivations, searchId);
		}
		
		return new ArrayList<InstrumentI>(finalDerivations);
	}

	protected  InstrumentI removeLeadingZeros(InstrumentI instrument) {
		InstrumentI newInstrument = instrument.clone();
		newInstrument.setBook(newInstrument.getBook().replaceAll("^0+", ""));
		newInstrument.setPage(newInstrument.getPage().replaceAll("^0+", ""));
		return newInstrument;
	}

	protected InstrumentI addBookPrefix(InstrumentI instrument) {
		InstrumentI newInstrument = instrument.clone();
		String book =  newInstrument.getBook();
		if(StringUtils.isNotEmpty(book) && !book.startsWith(bookPrefix)) {
			newInstrument.setBook(bookPrefix + book);	
		}
		return newInstrument;
	}
	
	protected InstrumentI getBookPage(InstrumentI instrument) {
		InstrumentI newInstrument = new Instrument();
		newInstrument.setBook(instrument.getBook());
		newInstrument.setPage(instrument.getPage());
		return newInstrument;
	}
	
	@Override
	protected void loadDerrivation(TSServerInfoModule module, InstrumentI state) {
		for (Object functionObject : module.getFunctionList()) {
			if (functionObject instanceof TSServerInfoFunction) {
				TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
				if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH) {
					function.setParamValue(state.getBook());
				} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH) {
					function.setParamValue(state.getPage());
				} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_SEARCH) {
					function.setParamValue(state.getInstno());
				}
			}
		}
	}

	protected void removeAlreadyRetrieved(Collection<InstrumentI> instruments, long searchId) {
		Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		DocumentsManagerI docManager = search.getDocManager();
		try {
			docManager.getAccess();
			for(DocumentI doc : docManager.getDocumentsWithType(DType.ROLIKE)) {
				InstrumentI instr = null;
				for(InstrumentI instrument : instruments) {
					if(doc.getBook().equalsIgnoreCase(instrument.getBook()) && 
							doc.getPage().equalsIgnoreCase(instrument.getPage())) {
						instr = instrument;
						break;
					}
				}
				if(instr!=null) {
					instruments.remove(instr);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			docManager.releaseAccess();
		}
	}
	
	public List<com.stewart.ats.base.document.InstrumentI> extractBookPageList(String saKey) {
		
		String bpList = getSearch().getSa().getAtribute(saKey);  
		List<com.stewart.ats.base.document.InstrumentI> instrList = new ArrayList<com.stewart.ats.base.document.InstrumentI>();
	
		if (StringUtils.isStringBlank(bpList)) {
			return instrList;
		}

		String[] bps = bpList.split(",");
		for (int i = 0; i < bps.length; i++) {
			if (!StringUtils.isStringBlank(bps[i])) {
				String[] bp = bps[i].split("[-,_]");
				if (bp != null && bp.length >= 2) {
					Instrument crtInst = new Instrument();
					crtInst.setBook(bp[0].trim());
					crtInst.setPage(bp[1].trim());
					instrList.add(crtInst);
				}
			}
		}
		
		
		return instrList;
	}

	public boolean isLoadFromSearchAttributes() {
		return loadFromSearchAttributes;
	}

	public ConfigurableBookPageIterator setLoadFromSearchAttributes(boolean loadFromSearchAttributes) {
		this.loadFromSearchAttributes = loadFromSearchAttributes;
		return this;
	}

	public boolean isLoadFromAoDocuments() {
		return loadFromAoDocuments;
	}

	public ConfigurableBookPageIterator setLoadFromAoDocuments(boolean loadFromAoDocuments) {
		this.loadFromAoDocuments = loadFromAoDocuments;
		return this;
	}

	public boolean isAlsoSearchWithoutLeadingZeroes() {
		return alsoSearchWithoutLeadingZeroes;
	}

	public ConfigurableBookPageIterator setAlsoSearchWithoutLeadingZeroes(
			boolean alsoSearchWithoutLeadingZeroes) {
		this.alsoSearchWithoutLeadingZeroes = alsoSearchWithoutLeadingZeroes;
		return this;
	}

	public boolean isOnlySearchWithPrefixesAdded() {
		return onlySearchWithPrefixesAdded;
	}

	public ConfigurableBookPageIterator setOnlySearchWithPrefixesAdded(boolean onlySearchWithPrefixesAdded) {
		this.onlySearchWithPrefixesAdded = onlySearchWithPrefixesAdded;
		return this;
	}

	public String getBookPrefix() {
		return bookPrefix;
	}

	public ConfigurableBookPageIterator setBookPrefix(String bookPrefix) {
		this.bookPrefix = bookPrefix;
		return this;
	}

	public boolean isLoadFromOcr() {
		return loadFromOcr;
	}

	public ConfigurableBookPageIterator setLoadFromOcr(boolean loadFromOcr) {
		this.loadFromOcr = loadFromOcr;
		return this;
	}

	public List<String> getRemoveBookPrefixes() {
		return removeBookPrefixes;
	}

	public ConfigurableBookPageIterator setRemoveBookPrefixes(String... removeBookPrefixes) {
		this.removeBookPrefixes = Arrays.asList(removeBookPrefixes);		
		return this;
	}
	
	public boolean isLoadFromRoLikeDocuments() {
		return loadFromRoLikeDocuments;
	}

	public void setLoadFromRoLikeDocuments(boolean loadFromRoLikeDocuments) {
		this.loadFromRoLikeDocuments = loadFromRoLikeDocuments;
	}
}
