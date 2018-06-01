package ro.cst.tsearch.search.iterator;

import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.connection.clarkimages.NvClarkAOMConn.MapType;

public class MapTypeBookPageIterator extends GenericRuntimeIterator<InstrumentI> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected int initialNumberOfDocuments = -1;

	
	public MapTypeBookPageIterator(long searchId) {
		super(searchId);
	}
	
	

	@Override
	protected List<InstrumentI> createDerrivations() {

		List<InstrumentI> derivations = new Vector<InstrumentI>();
		HashSet<String> listsForNow = new HashSet<String>();
		
		DocumentsManagerI manager = getSearch().getDocManager();
		try {
			manager.getAccess();
			List<DocumentI> documents = manager.getDocumentsWithDataSource(false, "AOM");
			
			initialNumberOfDocuments = documents.size();
		}
		finally{
			manager.releaseAccess();
		}

			
		for (InstrumentI instrumentI : derivations) {
			String key = "Book_"  + instrumentI.getBook() + "**Page_" + instrumentI.getPage();
			listsForNow.add(key);
		}
		
		String mapBook = getSearchAttribute(SearchAttributes.LD_BOOKNO);
		String mapPage = getSearchAttribute(SearchAttributes.LD_PAGENO);
		
		if(StringUtils.isNotBlank(mapBook) && StringUtils.isNotBlank(mapPage)) {
		
			MapType mapTypeFromAo = null;
			DocumentsManagerI docManager = null;
			try {
				docManager = search.getDocManager();
				docManager.getAccess();
				
				List<DocumentI> aoDocs = docManager.getDocumentsWithDataSource(true, "AO");
				for (DocumentI documentI : aoDocs) {
					if(documentI.getProperties() != null) {
						for (PropertyI property : documentI.getProperties()) {
							
							if(property.getLegal() != null
									&& property.getLegal().getSubdivision() != null
									&& ro.cst.tsearch.utils.StringUtils.isNotEmpty(property.getLegal().getSubdivision().getPlatDescription())) {
								try {
									mapTypeFromAo = MapType.valueOf(property.getLegal().getSubdivision().getPlatDescription().trim());
								} catch (IllegalArgumentException illegalArgumentException) {
									//do nothing, go to next step
								}
								if(mapTypeFromAo != null) {
									break;
								}
							}
						}
					}
					if(mapTypeFromAo != null) {
						break;
					}
 				}				
			} catch(Exception e) {
				logger.error("Error reading maptype from AO doc", e);
			} finally {
				if(docManager != null) {
					docManager.releaseAccess();
				}
			}
			
			MapType mapTypes[] = null;
			if(mapTypeFromAo != null) {
				mapTypes = new MapType[] {mapTypeFromAo};
			} else {
				mapTypes = new MapType[] {MapType.PL, MapType.PM};
			}
			
			for (MapType mapType : mapTypes) {
				
				InstrumentI instrument = new Instrument();
				
				instrument.setBook(mapBook);
				instrument.setPage(mapPage);	
				instrument.setBookType(mapType.toString());
				
				String key = "Book_"  + instrument.getBook() + "**Page_" + instrument.getPage() + "**MapType_" + instrument.getBookType();
				listsForNow.add(key);
				derivations.add(instrument);
				
				if (mapBook.matches("[A-Z]{2}\\d+")){
					instrument = new Instrument();
					instrument.setBook(mapBook.replaceAll("(?is)\\A[A-Z]{1,2}", ""));
					instrument.setPage(mapPage);	
					instrument.setBookType(mapType.toString());
					
					key = "Book_"  + instrument.getBook() + "**Page_" + instrument.getPage() + "**MapType_" + instrument.getBookType();
					listsForNow.add(key);
					derivations.add(instrument);
				}
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
				} else if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BP_TYPE) {
					function.setParamValue(state.getBookType());
				}
			}
		}
	}
	
	@Override
	public boolean hasNext(long searchId) {
		boolean hasNext = super.hasNext(searchId);
		
		if (hasNext){
			DocumentsManagerI docManager = getSearch().getDocManager();
			
			try{
				docManager.getAccess();
				if(initialNumberOfDocuments < docManager.getDocumentsWithDataSource(false, "AOM").size()) {
					return false;
				}
			}
			finally{
				docManager.releaseAccess();
			}
		}
		
		return hasNext;
	}
}
