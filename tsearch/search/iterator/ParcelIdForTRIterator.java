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

public class ParcelIdForTRIterator extends GenericRuntimeIterator<String> {

	/**
	 * @author mihaiB
	 */
	private static final long serialVersionUID = 1L;
	
	private int initialSiteDocuments = -1;
	private String[] checkDocumentSource = null;
	private boolean firstTime = true;

	public ParcelIdForTRIterator(long searchId) {
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
		String parcel = "";
		try {
			manager.getAccess();
			List<DocumentI> documents = manager.getDocumentsWithDocType("ASSESSOR");
			for (DocumentI documentI : documents) {
				if(!derivationList.contains(documentI.getInstno())) {
					derivationList.add(documentI.getInstno());
					parcel = documentI.getInstno();
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
		
		String parcelCorresp = "";//FLBrowardTR
		if (StringUtils.isNotEmpty(parcel)) {
			for (int i = 0; i < 2; i++){
				if(StringUtils.isEmpty(parcelCorresp)) {
					parcelCorresp = "1" + parcel.substring(1, 2) + parcel.substring(3, 6) + "-" + parcel.substring(6, 8) + "-" + parcel.substring(8, parcel.length()) + "0";
				} else{
					parcelCorresp = "2" + parcel.substring(1, 2) + parcel.substring(3, 6) + "-" + parcel.substring(6, 8) + "-" + parcel.substring(8, parcel.length()) + "0";
				}
				derivationList.add(parcelCorresp);
			}
		}
		
		firstTime = true;
		return derivationList;
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
