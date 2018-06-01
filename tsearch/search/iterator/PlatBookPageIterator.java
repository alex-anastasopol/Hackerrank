package ro.cst.tsearch.search.iterator;

import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class PlatBookPageIterator extends GenericRuntimeIterator<InstrumentI> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private boolean loadFromSearchAttributes = true;
	private boolean loadFromPlatDocuments = true;
	private boolean alsoSearchWithoutLeadingZeroes = true;
	
	public PlatBookPageIterator(long searchId) {
		super(searchId);
	}
	
	

	@Override
	protected List<InstrumentI> createDerrivations() {
		Search global = getSearch();
		List<InstrumentI> derivations = new Vector<InstrumentI>();
		HashSet<String> listsForNow = new HashSet<String>();
		for (InstrumentI instrumentI : derivations) {
			String key = "Book_"  + instrumentI.getBook() + "**Page_" + instrumentI.getPage();
			listsForNow.add(key);
		}
		
		if(loadFromSearchAttributes) {
			String platBook = global.getSa().getAtribute(SearchAttributes.LD_BOOKNO_1);
			String platPage = global.getSa().getAtribute(SearchAttributes.LD_PAGENO_1);
			if(StringUtils.isNotEmpty(platBook) && StringUtils.isNotEmpty(platPage)) {
				platPage = platPage.replaceFirst("^(\\d+)-\\d+$", "$1");
				InstrumentI instrument = new Instrument();
				instrument.setBook(platBook);
				instrument.setPage(platPage);
				String key = "Book_"  + instrument.getBook() + "**Page_" + instrument.getPage();
				if(!listsForNow.contains(key)) {
					listsForNow.add(key);
					derivations.add(instrument);
				}
				
				if(alsoSearchWithoutLeadingZeroes) {
					platBook = platBook.replaceAll("^0+", "");
					platPage = platPage.replaceAll("^0+", "");
					instrument = new Instrument();
					instrument.setBook(platBook);
					instrument.setPage(platPage);
					key = "Book_"  + instrument.getBook() + "**Page_" + instrument.getPage();
					if(!listsForNow.contains(key)) {
						listsForNow.add(key);
						derivations.add(instrument);
					}
				}
				
				
			}
		}
		if(loadFromPlatDocuments) {
			DocumentsManagerI documentsManagerI = global.getDocManager();
			try {
				documentsManagerI.getAccess();
				List<DocumentI> availablePlats = documentsManagerI.getDocumentsWithDocType("PLAT");
				for (DocumentI documentI : availablePlats) {
					if(documentI.hasBookPage()) {
						String key = "Book_"  + documentI.getBook() + "**Page_" + documentI.getPage();
						if(!listsForNow.contains(key)) {
							listsForNow.add(key);
							derivations.add(documentI);
						}
						if(alsoSearchWithoutLeadingZeroes) {
							String platBook = documentI.getBook().replaceAll("^0+", "");
							String platPage = documentI.getPage().replaceAll("^0+", "");
							InstrumentI instrument = new Instrument();
							instrument.setBook(platBook);
							instrument.setPage(platPage);
							key = "Book_"  + instrument.getBook() + "**Page_" + instrument.getPage();
							if(!listsForNow.contains(key)) {
								listsForNow.add(key);
								derivations.add(instrument);
							}
						}
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				documentsManagerI.releaseAccess();
			}
		}
		return derivations;
	}

	@Override
	protected void loadDerrivation(TSServerInfoModule module, InstrumentI state) {
		for (Object functionObject : module.getFunctionList()) {
			if (functionObject instanceof TSServerInfoFunction) {
				TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
				if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE) {
					function.setParamValue(state.getBook());
				} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE) {
					function.setParamValue(state.getPage());
				}
			}
		}
	}

}
