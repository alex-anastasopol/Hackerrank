package ro.cst.tsearch.search.iterator;

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
import com.stewart.ats.base.search.DocumentsManagerI;

public class ParcelIdIterator extends GenericRuntimeIterator<String> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int initialSiteDocuments = -1;
	private String[] checkDocumentSource = null;
	private boolean firstTime = true;

	public ParcelIdIterator(long searchId) {
		super(searchId);
	}

	public String[] getCheckDocumentSource() {
		return checkDocumentSource;
	}

	public void setCheckDocumentSource(String ... checkDocumentSource) {
		this.checkDocumentSource = checkDocumentSource;
	}

	@Override
	protected List<String> createDerrivations() {
		List<String> derivationList = new Vector<String>();
		
		Search global = getSearch();
		
		DocumentsManagerI manager = global.getDocManager();
		try {
			manager.getAccess();
			List<DocumentI> documents = manager.getDocumentsWithDocType("ASSESSOR", "COUNTYTAX");
			for (DocumentI documentI : documents) {
				if(!derivationList.contains(documentI.getInstno())) {
					derivationList.add(documentI.getInstno());
				}
			}
			
			if(checkDocumentSource != null) {
				initialSiteDocuments = manager.size();
			}
			
		} catch (Throwable t) {
			logger.error("Error while creating derivation list", t);
		} finally {
			manager.releaseAccess();
		}
		String parcelSearchPage = preparePin(search.getSa().getAtribute(SearchAttributes.LD_PARCELNO));
		
		if(StringUtils.isNotEmpty(parcelSearchPage) && !derivationList.contains(parcelSearchPage)) {
			if (parcelSearchPage.contains(",")){
				String[] pins = parcelSearchPage.split("\\s*,\\s*");
				for (String pin : pins) {
					if(StringUtils.isNotEmpty(pin)){
						pin = preparePin(pin);
						if (!derivationList.contains(pin)){
							derivationList.add(pin);
						}
					}
				}
			} else {
				derivationList.add(parcelSearchPage);
			}
		}
		firstTime = true;
		return derivationList;
	}

	protected String preparePin(String pin){
		
		return pin;
	}
	
	@Override
	public boolean hasNext(long searchId) {
		boolean hasNext = super.hasNext(searchId);
		if(hasNext && !firstTime && initialSiteDocuments >= 0) {
			if(initialSiteDocuments <= getSearch().getDocManager().size()) {
				return false;
			}
		}
		
		return hasNext;
	}
	
	@Override
	protected void loadDerrivation(TSServerInfoModule module, String instrumentNumber) {
		firstTime = false;
		for (Object functionObject : module.getFunctionList()) {
			if (functionObject instanceof TSServerInfoFunction) {
				TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
				if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_PARCELID_FAKE) {
					function.setParamValue(instrumentNumber);
				}
			}
		}

	}

}
