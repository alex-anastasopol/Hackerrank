package ro.cst.tsearch.servers.types;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.iterator.legal.LegalDescriptionIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class AKGenericDASLTS extends GenericDASLTS  {
	
	private static final long serialVersionUID = -5993038028346379257L;

	public AKGenericDASLTS(long searchId) {
		super(searchId);
	}

	public AKGenericDASLTS(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	protected String prepareInstrumentNoForCounty(InstrumentI inst){
		String instNo = inst.getInstno().replaceFirst("^0+", "");
		
		
		if(inst.hasYear()){
			String start = inst.getYear()+"/";
			if(instNo.startsWith(start)){
				instNo = instNo.substring(start.length());
			}
		}
		instNo  = instNo.replaceFirst("^0+", "");
		
		if(instNo.length()<=6){
			instNo = appendBeginingZero(6-instNo.length(),instNo);
			if(inst.hasYear()){
				if(inst.getYear()>=2000){
					instNo = inst.getYear() + "_"+instNo;
				}
			}
		}
		instNo  = instNo .replaceFirst("^0+", "");
		return instNo;
	}


	protected boolean addBookPageSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId,  boolean isUpdate){
		boolean addSomething = super.addBookPageSearch(inst, serverInfo, modules, searchId, isUpdate);
		
		if(inst.hasInstrNo()){
			String instNo = inst.getInstno().replaceAll("^0+", "");
			if(instNo.length()>6) {
				String lastFour = instNo.substring(instNo.length()-4,instNo.length());
				if(lastFour.startsWith("0")){
					String book = instNo.substring(0,instNo.length()-4);
					String page = lastFour.replaceAll("^0+", "");
				
					TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
					module.setData(0, book);
					module.setData(1, page);
					if (isUpdate) {
						module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
					}
					modules.add(module);
					return true;
				}
			}
		}
		
		return addSomething;
	}
	
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data){
		return isInstrumentSaved(instrumentNo, documentToCheck, data, false);
	}
	
	/**
     * Looks for the a document having the same instrumentNo
     * @param instrumentNo
     * @param documentToCheck if not null will only compare its instrument with saved documents
     * @param data
     * @return
     */
    public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
    	if(StringUtils.isEmpty(instrumentNo))
    		return false;
    	
    	/* The AO,TR document must be re-saved in date down searches: Bug 4584 */
    	if(mSearch.getSa().isDateDown() && isAssessorOrTaxServer()) {
    		return false;
    	}
    	
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
	    			
	    			if(checkMiServerId) {
		    			boolean foundMssServerId = false;
	    				for (DocumentI documentI : almostLike) {
	    					if(miServerID==documentI.getSiteId()){
	    						foundMssServerId  = true;
	    						break;
	    					}
	    				}
		    			
	    				if(!foundMssServerId){
	    					return false;
	    				}
	    			}
	    			
    				if(data!=null) {
    					if(!StringUtils.isEmpty(data.get("type"))){
			        		String serverDocType = data.get("type"); 
			    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
			    	    	String dataSource = data.get("dataSource");
			    	    	for (DocumentI documentI : almostLike) {
			    	    		if (serverDocType.equals("ASSESSOR") && dataSource != null) {
									if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))//B 4435, must save NDB and ISI doc of the same instrNo
										return true;
			    	    		} else if (serverDocType.equals("CNTYTAX") && dataSource != null) {
			    	    			if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))
										return true;
			    	    		} else if (serverDocType.equals("CITYTAX") && dataSource != null) {
			    	    			if(documentI.getDocType().equals(docCateg) && documentI.getDataSource().equals(dataSource))
										return true;
			    	    		}else if( (!checkMiServerId || miServerID==documentI.getSiteId()) && documentI.getDocType().equals(docCateg)){
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
    		for(DocumentI e: documentManager.getDocumentsWithDataSource(false, "TS")){
    			InstrumentI savedInst = e.getInstrument();
    			if( savedInst.getInstno().equals(instToCheck.getInstno())  
    					&& (savedInst.getBook().equals(instToCheck.getBook()) && savedInst.getPage().equals(instToCheck.getPage()))  
    					&& savedInst.getDocno().equals(instToCheck.getDocno())
    					&& e.getServerDocType().equals(documentToCheck.getServerDocType())
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
    
    protected void addIteratorModule( TSServerInfo serverInfo, List<TSServerInfoModule> modules,int code, long searchId, boolean isUpdate, boolean lookUpWasWithNames, boolean legalFromLastTransferOnly){
		TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(code));
		module.clearSaKeys();
		LegalDescriptionIterator it = new LegalDescriptionIterator(searchId, lookUpWasWithNames, legalFromLastTransferOnly, getDataSite());
		it.setCheckAlreadyFilledKeyWithDocuments(AdditionalInfoKeys.AK_SAVED_DOCUMENTS_FOR_LEGAL_ITERATOR);
		it.setTreatTractAsArb(true);
		module.addIterator(it);
		if (isUpdate) {
			module.addFilter(BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId));
		}
		modules.add(module);
	}
}
