package ro.cst.tsearch.servers.types;

import java.util.HashMap;
import java.util.List;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.ExactDateFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.doctype.DocTypeSimpleFilter;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.RestoreDocumentDataI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class ARBentonTS extends ARGenericDASLTS {
	
	private static final long serialVersionUID = 1L;

	public ARBentonTS(long searchId) {
		super(searchId);
	}

	public ARBentonTS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data){
  	  return isInstrumentSaved(instrumentNo, documentToCheck, data, false);
    }
	
	@Override
	public LegalDescriptionIterator getLegalDescriptionIterator(long searchId,
			boolean lookUpWasWithNames, boolean legalFromLastTransferOnly) {
		
		LegalDescriptionIterator it = super.getLegalDescriptionIterator(searchId, lookUpWasWithNames, legalFromLastTransferOnly);
		it.setUseAddictionInsteadOfSubdivision(true);
		return it;
	}
	
	/**
     * Looks for the a document having the same instrumentNo
     * @param instrumentNo
     * @param documentToCheck if not null will only compare its instrument with saved documents
     * @param data
     * @return
     */
	@Override
    public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
    	
    	DocumentsManagerI documentManager = getSearch().getDocManager();
    	try {
    		documentManager.getAccess();
    		if(documentToCheck != null) {
    			if(documentManager.getDocument(documentToCheck.getInstrument()) != null){
    				if (GWTDataSite.isRealRoLike(dataSite.getSiteTypeInt())){
	    				RegisterDocumentI docFound = (RegisterDocumentI) documentManager.getDocument(documentToCheck.getInstrument());
	    				RegisterDocumentI docToCheck = (RegisterDocumentI) documentToCheck;
	    				
	    				docToCheck.mergeDocumentsInformation(docFound, searchId, true, false);
    				}
    				
    				return true;
    			}
    		} else {
	    		InstrumentI instr = new com.stewart.ats.base.document.Instrument(instrumentNo);
	    		if(data != null) {
		    		if(!StringUtils.isEmpty(data.get("type"))) {
		        		String serverDocType = data.get("type");
		    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId); 
		            	instr.setDocType(docCateg);
		            	instr.setDocSubType("MISCELLANEOUS");
		    		}
		    		
		    		instr.setBook(data.get("book"));
		    		instr.setPage(data.get("page"));
		    		instr.setDocno(data.get("docno"));
	    		}
	    		
	    		try {
	    			instr.setYear(Integer.parseInt(data.get("year")));
	    		} catch (Exception e) {}
	    		
	    		if(documentManager.getDocument(instr) != null) {
	    			return true;
	    		} else {
	    			List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, instr);
	    			
    				if(data!=null) {
    					if(!StringUtils.isEmpty(data.get("type"))){
			        		String serverDocType = data.get("type"); 
			    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
			    	    	for (DocumentI documentI : almostLike) {
			    	    		if(documentI.getDocType().equals(docCateg)){
									return true;
			    	    		}
							}	
    					}
		    		} else {
		    			EmailClient email = new EmailClient();
		    			email.addTo(MailConfig.getExceptionEmail());
		    			email.setSubject("isInstrumentNoSaved problem on " + URLMaping.INSTANCE_DIR + this.getClass().getName());
		    			email.addContent("We should at least have type!!!!\nSearchId=" + searchId);
		    			email.sendAsynchronous();
		    		}
	    		}
    		}
    		
    	} catch (Exception e) {
			e.printStackTrace();
		} finally {
			documentManager.releaseAccess();
		}
    	
    	if(documentToCheck == null) {
			return false;
		}
		try {
    		documentManager.getAccess();
    		InstrumentI instToCheck = documentToCheck.getInstrument();
    		for(DocumentI e: documentManager.getDocumentsWithDataSource(false, "RO")){
    			InstrumentI savedInst = e.getInstrument();
    			if( savedInst.getInstno().equals(instToCheck.getInstno())  
    					&& (savedInst.getBook().equals(instToCheck.getBook()) && savedInst.getPage().equals(instToCheck.getPage()))  
    					&& savedInst.getDocno().equals(instToCheck.getDocno())
    					&& e.getDocType().equals(documentToCheck.getDocType())
    					&& savedInst.getYear() == instToCheck.getYear()
    			){
    				return true;
    			}
    		}
    	} finally {
    		documentManager.releaseAccess();
    	}
    	
    	return false;
    }
	
	protected boolean addBookPageSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId,  boolean isUpdate){
		if(inst.hasBookPage()){
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
			module.setData(0, inst.getBook().replaceFirst("^0+", ""));
			module.setData(1, inst.getPage().replaceFirst("^0+", ""));
			if (isUpdate) {
				module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId)); 
			}
			
			if(!DocumentTypes.MISCELLANEOUS.equals(inst.getDocType())){
				DocTypeSimpleFilter docTypefilter = new DocTypeSimpleFilter(searchId);
				docTypefilter.setDocTypes(new String[]{inst.getDocType()});
				module.addFilter(docTypefilter);
			}
			
			ExactDateFilterResponse dateFilter = new ExactDateFilterResponse(searchId);
			if(inst instanceof RegisterDocument) {
				dateFilter.addFilterDate(((RegisterDocumentI)inst).getRecordedDate());
				dateFilter.addFilterDate(((RegisterDocumentI)inst).getInstrumentDate());
			} else {
				dateFilter.addFilterDate(inst.getDate());
			}
			if(!dateFilter.getFilterDates().isEmpty()) {
				module.addFilter(dateFilter);
			}
			
			modules.add(module);
			return true;
		}
		return false;
	}
	
	@Override
	public TSServerInfoModule getRecoverModuleFrom(RestoreDocumentDataI restoreDocumentDataI) {
		TSServerInfoModule module = super.getRecoverModuleFrom(restoreDocumentDataI);
		
		if(module != null && restoreDocumentDataI != null) {
			ExactDateFilterResponse dateFilter = new ExactDateFilterResponse(searchId);
			
			dateFilter.addFilterDate(restoreDocumentDataI.getRecordedDate());
			module.getFilterList().clear();
			
			if(!dateFilter.getFilterDates().isEmpty()) {
				module.addFilter(dateFilter);
			}
		}
		
		return module;
	}

}
