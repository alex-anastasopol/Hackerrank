package ro.cst.tsearch.servers.types;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.datatrace.DTRecord;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.iterator.data.LegalStructDTG;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
 * @author cristian stochina
 */
public class OHFranklinDTG extends OHGenericDTG{

	private static final long serialVersionUID = 3024488184071095461L;

	public OHFranklinDTG(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid)
			throws FileNotFoundException, IOException {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

	public OHFranklinDTG(long searchId) {
		super(searchId);
	}
	
	@Override
	protected boolean isCompleteLegal(LegalStructDTG ret1, int qo, String qv, int siteType, String stateAbbrev){
		return (StringUtils.isNotEmpty(ret1.getArbDtrct()) 
				&& StringUtils.isNotEmpty(ret1.getArbParcel()) 
				&& StringUtils.isNotEmpty(ret1.getArbParcelSplit()));
	}
	
	@Override
	protected boolean testIfExist(Set<LegalStructDTG> legalStruct2, LegalStructDTG l,long searchId) {
		
		if (ARB_TYPE.equalsIgnoreCase(l.getType())){
			for (LegalStructDTG p : legalStruct2){
				if (StringUtils.isNotEmpty(p.getArbDtrct()) && StringUtils.isNotEmpty(p.getArbParcel())){
					if (l.getArbDtrct().equals(p.getArbDtrct()) && l.getArbParcel().equals(p.getArbParcel())){
						return true;
					}
				}
			}
		} else if(SUBDIVIDED_TYPE.equalsIgnoreCase(l.getType())){
			for (LegalStructDTG p:legalStruct2){
				if (p.isPlated()){
					if (l.equalsSubdivided(p)){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean isAlreadySaved(String instrumentNo, DocumentI doc, DTRecord record){
		
		String instrNo = DTRecord.formatInstNoForOHFranklin(instrumentNo, record.getRecordedDate(), "MM/dd/yyyy");
		
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("type", doc.getDocType());
		
		DocumentI docToCheck = null;
		if (doc != null){
			docToCheck = doc.clone();
			docToCheck.setDocSubType(null);
		}

		return isInstrumentSaved(instrNo, docToCheck, data, false);
	}
	
	@Override
	public InstrumentGenericIterator getInstrumentOrBookTypeIterator(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId, dataSite) {

			private static final long serialVersionUID = 5399351945130601258L;
			@Override
			protected String cleanInstrumentNo(String instr, int year) {
				if (instr.length() == 15) { // e.g. 200704160065511 (Task 8683)
					instr = instr.substring(8);
				}
				instr = StringUtils.stripStart(instr, "0");
				
				return instr;
			}
			
		};
		if (instrumentType) {
			instrumentGenericIterator.enableInstrumentNumber();
		} else {
			instrumentGenericIterator.enableBookPage();
			instrumentGenericIterator.setRemoveLeadingZerosBP(true);
		}

		return instrumentGenericIterator;
	}
	
	@Override
	protected String prepareInstNoForReferenceSearch(InstrumentI inst) {
		String instrumentNumber = inst.getInstno();
		if (instrumentNumber.length() == 15) { // e.g. 200704160065511 (Task 8683)
			instrumentNumber = instrumentNumber.substring(8);
		}
		instrumentNumber = instrumentNumber.replaceFirst("^0+", "");
		
		return instrumentNumber;
	}

	@Override
	protected String preparePageForReferenceSearch(InstrumentI inst) {
		
		String page = inst.getPage();
		if (org.apache.commons.lang.StringUtils.isNotEmpty(page)){
			if (page.matches("(?is)[A-Z]0\\d")){
				page = page.replaceFirst("(?is)\\A([A-Z])0(\\d)$", "$1$2");
				return page;
			}
		}
		return page;
	}
	
	@Override
	protected  boolean addInstNoSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, Set<String> searched, boolean isUpdate){
		if ( inst.hasInstrNo() ){ 
			String instr = StringUtils.defaultString(inst.getInstno());
			
			if(instr.length() == 15) { // e.g. 200704160065511 (Task 8683)
				instr = instr.substring(8);
				inst.setEnableInstrNoTailMatch(true);
			}
			instr = instr.replaceFirst("^0+", "");
			/*if(instr.length()>0 && instr.length()==7){
				instr = "0"+instr;
			}*/
			
			
			String year1 = String.valueOf(inst.getYear());
			if(!searched.contains(instr+year1)){
				searched.add(instr+year1);
			}else{
				return false;
			}
			
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.INSTR_NO_MODULE_IDX));
			module.setData(0, instr);
			module.setData(1, inst.getYear()+"");
			if (isUpdate) {
				module.addFilter(new BetweenDatesFilterResponse(searchId));
			}
			modules.add(module);
			return true;
		}
		return false;
	}
	

	@Override
	public void processOcredInstrument(InstrumentI in, boolean ocrReportedBookPage) {
		if(!ocrReportedBookPage){
			
			String inst = StringUtils.defaultString(in.getInstno()).replaceAll("^0+", "");
			if(inst.length()>=15){				
				String yearStr = inst.substring(0,4);
				inst = inst.substring(8);
				
				if(super.validYear(yearStr)){
					in.setBook("");
					in.setPage("");
					in.setInstno(inst.replaceAll("^0+", ""));
					in.setYear(Integer.parseInt(yearStr));
				}
			}
		}
		
	}
	
	 public static void processAllReferencedInstruments(DocumentI doc) {
	    	try {
	    		Set<InstrumentI> parsedRefs = doc.getParsedReferences();
	    		for (InstrumentI instr : parsedRefs) {
	    			processInstrumentNo(instr);
				}
			
	    	} catch(Exception e) {
				e.printStackTrace();
			}
	    }
	 
	 public static void processInstrumentNo(InstrumentI instr) {
			try {
				String instrNo = instr.getInstno();
				Matcher m = Pattern.compile("(\\d{4})(\\d{4})(\\d+)").matcher(instrNo);
				if (m.find()) {
					int instYear = Integer.parseInt(m.group(1));
					if (instYear <=  Calendar.getInstance().get(Calendar.YEAR)) {
						if ( Util.isValidDate(m.group(1))) {
							if (m.group(3).trim().length() == 7)
								instr.setEnableInstrNoTailMatch(true);
						}
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	
	/**
	 * avoid saving docs that were already saved from RO (Task 8683)
	 */
	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
		if(StringUtils.isEmpty(instrumentNo))
			return false;

		DocumentsManagerI documentManager = getSearch().getDocManager();
		try {
			documentManager.getAccess();
			if(documentToCheck != null) {
				processInstrumentNo(documentToCheck.getInstrument());
				processAllReferencedInstruments(documentToCheck);
				if(documentManager.getDocument(documentToCheck.getInstrument()) != null) {
					if (GWTDataSite.isRealRoLike(dataSite.getSiteTypeInt())){
	    				RegisterDocumentI docFound = (RegisterDocumentI) documentManager.getDocument(documentToCheck.getInstrument());
	    				RegisterDocumentI docToCheck = (RegisterDocumentI) documentToCheck;
	    				
	    				docToCheck.mergeDocumentsInformation(docFound, searchId, true, false);
    				}
					return true;
				} else if(!checkMiServerId) {
					List<DocumentI> almostLike = documentManager.getDocumentsWithInstrumentsFlexible(false, documentToCheck.getInstrument());
					if(almostLike != null && !almostLike.isEmpty()) {
						return true;
					}
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
						for (DocumentI documentI : almostLike) {
							if( (!checkMiServerId || miServerID==documentI.getSiteId()) && documentI.getDocType().equals(docCateg)){
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
		return false;
	}
	
	/**
	 * avoid saving docs that were already saved from RO (Task 8683)
	 */
	@Override
	protected DocumentI getAlreadySavedDocument(DocumentsManagerI manager, DocumentI doc) {
		DocumentI _doc = super.getAlreadySavedDocument(manager, doc);
		if(_doc == null) {
			HashMap<String, String> data = new HashMap<String, String>();
            data.put("type", doc.getDocType());
			if(isInstrumentSaved(doc.getInstno(), doc, data, false)) {
				try {
					manager.getAccess();
					InstrumentI instr = doc.getInstrument().clone();
					instr.setDocSubType("MISCELLANEOUS");
					List<DocumentI> almostLike = manager.getDocumentsWithInstrumentsFlexible(false, instr);
					for (DocumentI documentI : almostLike) {
						if(documentI.getDocType().equals(doc.getDocType())){
							_doc = documentI;
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					manager.releaseAccess();
				}
			}
		}
		
		return _doc;
	}
	
	@Override
	public DownloadImageResult downloadedFromOtherSite(ImageI image, InstrumentI instr){
		
		if (image != null && image.getLinks().size() > 0){
			long siteId = TSServersFactory.getSiteId(
					getSearch().getSa().getAtribute(SearchAttributes.P_STATE_ABREV), 
					getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY_NAME), 
					"RO");
    		OHFranklinRO ohFranklinRo = (OHFranklinRO)TSServersFactory.GetServerInstance((int)siteId, getSearch().getID());
    		
    		if (ohFranklinRo.getDataSite().isEnableSite(getSearch().getCommId())) {
    			return ohFranklinRo.downloadImageFromRO(image, instr);
    		}
		}

		return new DownloadImageResult();
	}
	
	@Override
	protected String preparePid(String pin) {
		if (pin.contains("-") || pin.contains(",")){
			return pin;
		}
		if (pin.length() == 11){
			pin = pin.replaceAll("(\\d{3})(\\d{6})(\\d{2})", "$1-$2-$3");
		}
		
		return pin;
	}
	
	@Override
	protected String formatInstrumentNumber(String instNo){
		if (instNo.length() > 8){
			instNo = StringUtils.stripStart(instNo.substring(8), "0");
		}
		
		return instNo;
	}
}
