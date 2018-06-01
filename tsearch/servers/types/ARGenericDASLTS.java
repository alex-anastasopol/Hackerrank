package ro.cst.tsearch.servers.types;

import java.util.HashMap;
import java.util.List;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;

public class ARGenericDASLTS extends GenericDASLTS {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ARGenericDASLTS(long searchId) {
		super(searchId);
	}

	public ARGenericDASLTS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	protected void addIteratorModule( TSServerInfo serverInfo, List<TSServerInfoModule> modules,int code, long searchId, boolean isUpdate, boolean lookUpWasWithNames, boolean legalFromLastTransferOnly){
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
		module.clearSaKeys();
		LegalDescriptionIterator it = getLegalDescriptionIterator(searchId, lookUpWasWithNames, legalFromLastTransferOnly);
		it.setEnableTownshipLegal(false);
		it.setCheckAlreadyFilledKeyWithDocuments(AdditionalInfoKeys.AR_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR);
		module.addIterator(it);
		if (isUpdate) {
			module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
		}

		//module.addFilter( new SubdivisionFilter(searchId) );

		modules.add(module);
	}
	
	protected String prepareInstrumentNoForCounty(InstrumentI inst){
		String instNo = inst.getInstno().replaceFirst("^0+", "");
		instNo = appendBeginingZero(6-instNo.length(),instNo);
		if(inst.hasYear()){
			if(inst.getYear()>=2000 && instNo.length()==6){
				instNo = inst.getYear() + instNo;
			}else if(inst.getYear()<2000 && instNo.length()==6){
				instNo = (inst.getYear() + instNo).substring(2);
			}else if(inst.getYear()>=2000 && instNo.length()==7){
				instNo = instNo.substring(1);
				instNo = appendBeginingZero(6-instNo.length(),instNo);
				instNo = inst.getYear()+instNo;
			}
			else if(instNo.length()==7){
				String year = instNo.substring(0,2);
				instNo = instNo.substring(2);
				instNo = appendBeginingZero(6-instNo.length(),instNo);
				instNo = year+instNo;
			}
		}
		instNo  = instNo .replaceFirst("^0+", "");
		return instNo;
	}
	
	public boolean isInstrumentSaved(String instrumentNo,
			DocumentI documentToCheck, HashMap<String, String> data) {
		
		boolean firstTry = super.isInstrumentSaved(instrumentNo, documentToCheck, data);
		
		if(firstTry) {
			return true;
		}
		
		if(documentToCheck == null) {
			return false;
		}
		
		if (mSearch.getCountyId().equals(CountyConstants.AR_Pulaski_STRING)){
			
			DocumentsManagerI documentManager = getSearch().getDocManager();
	    	try {
	    		documentManager.getAccess();
	    		InstrumentI instToCheck = documentToCheck.getInstrument();
	    		for(DocumentI e: documentManager.getDocumentsWithDataSource(false, "RO")){
	    			InstrumentI savedInst = e.getInstrument();
	    			
	    			String instrNo = e.getInstno();
	    			if (instrNo.startsWith("19")){
	    				instrNo = instrNo.substring(2, instrNo.length());
	    			}
	    			String savedDocCateg = DocumentTypes.getDocumentCategory(e.getServerDocType(), searchId); 
	    			if( (savedInst.getInstno().equals(instToCheck.getInstno()) || instToCheck.getInstno().equals(instrNo))  
	    					&& (savedInst.getBook().equals(instToCheck.getBook()) && savedInst.getPage().equals(instToCheck.getPage()))  
	    					&& savedInst.getDocno().equals(instToCheck.getDocno())
	    					&& savedDocCateg.equals(documentToCheck.getServerDocType())
	    					&& savedInst.getYear() == instToCheck.getYear()
	    			){
	    				return true;
	    			}
	    		}
	    	} finally {
	    		documentManager.releaseAccess();
	    	}
		}
		return false;
	}
}
