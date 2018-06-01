package ro.cst.tsearch.search.iterator;

import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.GenericRuntimeIterator;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.InstanceManager;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class DocumentResaverIterator extends GenericRuntimeIterator<InstrumentI> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(DocumentResaverIterator.class);
	
	private String[] roDoctypesToLoad = null;

	public DocumentResaverIterator(long searchId) {
		super(searchId);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected List<InstrumentI> createDerrivations() {
		return createDerrivationsInternal();
	}
	

	@Override
	public boolean hasNext(long searchId) {
		boolean hasNext = super.hasNext(searchId);
		if(!hasNext) {
			InstanceManager.getManager().getCurrentInstance(searchId).
				getCrtSearchContext().removeAdditionalInfo("RESAVE_DOCUMENT");
		}
		return hasNext;
	}
	
	private List<InstrumentI> createDerrivationsInternal(){
		List<InstrumentI> derivationList = new Vector<InstrumentI>();
		
		Search global = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		//need to check documents
		global.applyQARules();
		global.setAdditionalInfo("RESAVE_DOCUMENT", true);
		global.clearVisitedLinks();
		global.clearValidatedLinks();
		DocumentsManagerI manager = global.getDocManager();
		try {
			manager.getAccess();
			List<DocumentI> documents = manager.getDocumentsWithDocType(true, roDoctypesToLoad);
			for (DocumentI documentI : documents) {
				derivationList.add(documentI.getInstrument());
			}
		} catch (Throwable t) {
			logger.error("Error while creating derivation list", t);
		} finally {
			manager.releaseAccess();
		}
		
		return derivationList;
	}

	@Override
	protected void loadDerrivation(TSServerInfoModule module, InstrumentI instrument) {
		for (Object functionObject : module.getFunctionList()) {
			if (functionObject instanceof TSServerInfoFunction) {
				TSServerInfoFunction function = (TSServerInfoFunction) functionObject;
				if(function.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_INSTRUMENT_LIST_FAKE) {
					function.setParamValue(instrument.getInstno());
				}
			}
		}
		
	}
	
	public String[] getRoDoctypesToLoad() {
		return roDoctypesToLoad;
	}

	public void setRoDoctypesToLoad(String[] roDoctypesToLoad) {
		this.roDoctypesToLoad = roDoctypesToLoad;
	}

}
