package ro.cst.tsearch.servers.types;

import java.util.HashMap;
import java.util.List;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

public class TXBrazoriaTS extends TXDaslTS {
	
	private static final long serialVersionUID = 1L;

	public TXBrazoriaTS(long searchId) {
		super(searchId);
	}

	public TXBrazoriaTS(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}
	
	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		
		if (module.getModuleIdx() == TSServerInfo.BOOK_AND_PAGE_LOCAL_MODULE_IDX 
				|| module.getModuleIdx() == TSServerInfo.NAME_MODULE_IDX) {
			List<TSServerInfoModule> modules = getMultipleModules(module, sd);
			if (!modules.isEmpty()) {
				return super.searchByMultipleInstrument(modules, sd, null);
			}
		}
		
		ServerResponse response = super.searchByMultipleInstrument(module, sd);
		if (response!=null) {
			return response;
		}
		
		return super.SearchBy(module, sd);
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
    		} 
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
    		for(DocumentI e: documentManager.getDocumentsWithDataSource(false, "TDI")){
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
}
