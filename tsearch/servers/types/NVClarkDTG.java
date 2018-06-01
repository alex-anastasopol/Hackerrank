package ro.cst.tsearch.servers.types;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
 * @author cristian stochina
 */
public class NVClarkDTG extends NVGenericDTG {

	private static final long serialVersionUID = 3024488184071095461L;

	public NVClarkDTG(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) throws FileNotFoundException, IOException {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	public NVClarkDTG(long searchId) {
		super(searchId);
	}
	
	private boolean dontMakeTheSearch(TSServerInfoModule module){
		
		if(module.getModuleIdx()==TSServerInfo.INSTR_NO_MODULE_IDX){
			String date = StringUtils.defaultString(module.getParamValue(1));
			return StringUtils.isEmpty(module.getParamValue(0)) || date.length()>8 || date.length()<6; 
		}
		
		return false;
	}
	
	@Override
	public ServerResponse SearchBy(TSServerInfoModule module, Object sd) throws ServerResponseException {
		if(mSearch==null){
			mSearch = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		}
		
		if(isParentSite()){
			mSearch.setAdditionalInfo("RESAVE_DOCUMENT",Boolean.FALSE);
		}
		
		if( !isParentSite() && (dontMakeTheSearch(module) || "true".equalsIgnoreCase(mSearch.getSa().getAtribute(SearchAttributes.ATS_MULTIPLE_LEGALS_FOUND)) ) ){
			return new ServerResponse();
		}
		
		return super.SearchBy(module, sd);
	}
	

	@Override
    protected  boolean addInstNoSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, Set<String> searched, boolean isUpdate){
		
    	if(super.addInstNoSearch(inst, serverInfo, modules, searchId, searched, isUpdate)){
    		String instr = inst.getInstno().replaceFirst("^0+", "");
			String year = "";
			
			if(instr.length()==12){//970121000258
				String first = instr.substring(0,6);
				year = first;
				if(Integer.parseInt(year.substring(0,2))>=10 && Integer.parseInt(year.substring(0,2))<=30){
					year = "20"+year;
				}	
				instr = instr.substring(6).replaceAll("^0+", "");
			}else if(instr.length()==11){
				String first = instr.substring(0,1);
				String middle = instr.substring(1,5);
				year = "200"+first+middle;
				instr = instr.substring(5).replaceAll("^0+", "");
			}else if(instr.length()==9){ //321001500
				String first = instr.substring(0,3);
				year = "20000"+first;
				instr = instr.substring(5).replaceAll("^0+", "");
			}else if(instr.length()==15){//200003210001500 AO docs
				String first = instr.substring(0,8);
				year = first;
				if(!"20".equals(year.substring(0,2))){
					year = year.substring(2);
				}
				instr = instr.substring(8).replaceAll("^0+", "");
			} else if (instr.length() < 7){
				if (inst.getDate() != null){
					year = FormatDate.getDateFormat(FormatDate.PATTERN_yyyyMMdd).format(inst.getDate());
					if(!"20".equals(year.substring(0,2))){
						year = year.substring(2);
					}
				}
			}
			
			TSServerInfoModule module = modules.get(modules.size()-1);
			module.setData(0, instr);
			if(StringUtils.isNotBlank(year)){
				module.setData(1, year);
			}
			
			return true;
    	}
			
		return false;
	} 

	@Override
	protected String prepareInstNoForReferenceSearch(InstrumentI inst) {
		return inst.getInstno();
	}


	@Override
	protected String prepareInstrumentYearForReferenceSearch(InstrumentI inst) {
		int year = inst.getYear();
		
		if(year<0){
			return "";
		}
		
		String yearStr =  String.valueOf(year);
		
		SimpleDateFormat format = new SimpleDateFormat("MMdd");
		Date date = inst.getDate();
		if(date!=null){
			if(year<=1999){
				if(yearStr.length()==4){
					return yearStr.substring(2,4)+format.format(date);
				}
			}
			else{
				return yearStr+format.format(date);
			}
		}
		return yearStr;
	}
	
	@Override
	public void processOcredInstrument(InstrumentI in, boolean ocrReportedBookPage) {
		if(ocrReportedBookPage){
			String book = StringUtils.defaultString(in.getBook()).replaceAll("^0+", "");
			String page = StringUtils.defaultString(in.getPage()).replaceAll("^0+", "");
			
			if(book.length()==6 && page.length()>=1 &&  page.length()<=6){
				String yearStr = book.substring(0,2);
				String monthStr = book.substring(2,4);
				String dayStr = book.substring(4,6);
				if(super.validYear(yearStr)&&super.validMonth(monthStr)&&super.validDay(dayStr)){
					Date date = Util.dateParser3(monthStr+"/"+dayStr+"/"+yearStr);
					if(date!=null){
						Calendar cal = Calendar.getInstance();
						cal.setTime(date);
						in.setBook("");
						in.setPage("");
						in.setInstno(page);
						in.setYear(cal.get(Calendar.YEAR));
						in.setDate(date);
					}
				}
			}
		}else{
			String inst = StringUtils.defaultString(in.getInstno()).replaceAll("^0+", "");
			if(inst.length()==15){				
				String yearStr = inst.substring(0,4);
				String monthStr = inst.substring(4,6);
				String dayStr = inst.substring(6,8);
				
				if(super.validYear(yearStr)&&super.validMonth(monthStr)&&super.validDay(dayStr)){
					Date date = Util.dateParser3(monthStr+"/"+dayStr+"/"+yearStr);
					if(date!=null){
						Calendar cal = Calendar.getInstance();
						cal.setTime(date);
						in.setBook("");
						in.setPage("");
						in.setInstno(inst.substring(9).replaceAll("^0+", ""));
						in.setYear(cal.get(Calendar.YEAR));
						in.setDate(date);
					}
				}
			}
		}
		
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
	
}
