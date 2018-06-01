package ro.cst.tsearch.servers.types;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.datatrace.DTRecord;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.HttpUtils;
import ro.cst.tsearch.utils.SearchLogger;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.search.DocumentsManager;

/**
 * @author cristian stochina
 */
public class OHCuyahogaDTG extends OHGenericDTG {

	private static final long serialVersionUID = 3024488184071095461L;

	public OHCuyahogaDTG(String rsRequestSolverName, String rsSitePath,
			String rsServerID, String rsPrmNameLink, long searchId, int mid)
			throws FileNotFoundException, IOException {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink,
				searchId, mid);
	}

	public OHCuyahogaDTG(long searchId) {
		super(searchId);
	}
	
	@Override
	public boolean isAlreadySaved(String instrumentNo, DocumentI doc, DTRecord record){
		
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("instrno", instrumentNo);
		data.put("book", record.getBook());
		data.put("page", record.getPage());
		data.put("year", record.getInstYear());
		data.put("type", doc.getServerDocType());
        
		return isInstrumentSaved(instrumentNo, doc, data, false);
	}

	@Override
	protected String prepareInstNoForReferenceSearch(InstrumentI inst) {
		String instNo = inst.getInstno();
		
		if (StringUtils.isNotEmpty(instNo) && instNo.length() == 12){
			instNo = instNo.substring(4);
		}
		return instNo;
	}

	@Override
	protected String prepareInstrumentYearForReferenceSearch(InstrumentI inst) {
		if (inst.getYear() < 0){
			if (inst.getInstno().length() == 12){
				return inst.getInstno().substring(0, 4);
			}
			return "";
		}
		return String.valueOf(inst.getYear());
	}
	
	@Override
	public InstrumentGenericIterator getInstrumentOrBookTypeIterator(boolean instrumentType) {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId, dataSite) {

			private static final long serialVersionUID = 5399351945130601258L;
			@Override
			protected String cleanInstrumentNo(String instno, int year) {
				if (instno.length() > 0 && instno.length() == 7){
					instno = StringUtils.leftPad(instno, 8, "0");
				}
				return instno;
			}
			@Override
			public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				return state.getInstno().trim();
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
	protected boolean addBookPageSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, Set<String> searched,boolean isUpdate){
		if(inst.hasBookPage()){
			String book = inst.getBook().replaceFirst("^0+", "");
			String page = inst.getPage().replaceFirst("^0+", "");
			
			RegisterDocument document = new RegisterDocument(DocumentsManager.generateDocumentUniqueId(searchId, inst));
			document.setInstrument(inst);
			HashMap<String, String> data = new HashMap<String, String>();
			data.put("year", Integer.toString(inst.getYear()));
			if (!isInstrumentSaved(book + "_" + page, document, data, false)) {
				if(!searched.contains(book+"_"+page)){
					searched.add(book+"_"+page);
				}else{
					return false;
				}
				TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
				module.setData(0, book);
				module.setData(1, page);
				if (isUpdate) {
					module.addFilter(new BetweenDatesFilterResponse(searchId));
				}
				modules.add(module);
				return true;
			}
			
		}
		return false;
	}
	
	@Override
	protected  boolean addInstNoSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, Set<String> searched, boolean isUpdate){
		if ( inst.hasInstrNo() ){ 
			String instr = StringUtils.defaultString(inst.getInstno());
			
			RegisterDocument document = new RegisterDocument(DocumentsManager.generateDocumentUniqueId(searchId, inst));
			document.setInstrument(inst);
			HashMap<String, String> data = new HashMap<String, String>();
			data.put("year", Integer.toString(inst.getYear()));
			if (!isInstrumentSaved(instr, document, data, false)) {
				if(instr.length()>0 && instr.length()==7){
					instr = "0"+instr;
				}
				
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
			
			
		}
		return false;
	}
	
	@Override
	protected String prepareDocumentNoForImageSearch(String docNo, String yearStr, String monthStr, String dayStr) {
		int year = -1;
		int day = -1;
		int month = -1;
		
		try{year = Integer.parseInt(yearStr);}catch(Exception e){}
		try{day = Integer.parseInt(dayStr);}catch(Exception e){}
		try{month = Integer.parseInt(monthStr);}catch(Exception e){}
		
		
		//1998.11.09.1
		if(year>1998 || (year==1998 && month>11) || (year==1998 && month==11 && day>=9)){
			if(docNo.length()>=4){
				docNo = docNo.substring(docNo.length()-4, docNo.length());
				docNo = docNo.replaceAll("^0+", "");
			}
		}
		
		return docNo;
	}

	@Override
	void setParsedData(DTRecord record2, ParsedResponse item2){
		/*String book = "";
		String page = "";
		String year = "";
		Map<String,String> instInfo = record2.getInstrumentInfo();
		
		if(instInfo!=null && "YEAR.BOOK.PAGE".equalsIgnoreCase(instInfo.get("image.image_params.index_type"))){
			String description = instInfo.get("image.image_params.description");
			if(StringUtils.isNotBlank(description)){
				int lastPointPoz = description.lastIndexOf('.');
				if(lastPointPoz>0){
					page = description.substring(lastPointPoz+1,description.length());
					description = description.substring(0, lastPointPoz);
					
					if(lastPointPoz>0){
						lastPointPoz = description.lastIndexOf('.');	
						book = description.substring(lastPointPoz+1,description.length());
						year = description.substring(0, lastPointPoz);
					}
				}
			}
		}
		
		if( StringUtils.isNotBlank(book) && StringUtils.isNotBlank(page) && StringUtils.isNotBlank(year)){
			instInfo.remove("number");
			instInfo.put("book", book);
			instInfo.put("page", page);
		}*/
		
		record2.setParsedData(item2, searchId, getDataSite());	
		
	}
	
	@Override
	public boolean isInstrumentSaved(String instrumentNo, DocumentI documentToCheck, HashMap<String, String> data, boolean checkMiServerId){
    	
		if (mSearch == null){
			if (searchId > 0){
				mSearch = getSearch();
			} else{
				throw new RuntimeException("Incomplete TSServer mSearch == " + mSearch + " searchId = " + searchId);
			}
		}
		
		boolean found = super.isInstrumentSaved(instrumentNo, documentToCheck, data, checkMiServerId);
		
		if (found) {
			return true;
		} else {
			if (instrumentNo.length()<8) {
				instrumentNo = StringUtils.leftPad(instrumentNo, 8, "0");
			}
			String year = data.get("year");
			if (!StringUtils.isEmpty(year)) {
				instrumentNo = year + instrumentNo;
			}
			DocumentI clone = documentToCheck.clone();
			clone.setInstno(instrumentNo);
			return super.isInstrumentSaved(instrumentNo, clone, data, checkMiServerId);
		}
		
    }
	
	@Override
	public DownloadImageResult downloadedFromOtherSite(ImageI image, InstrumentI instr){
		
		if (image != null && image.getLinks().size() > 0){
			HashMap<String, String> params = HttpUtils.getParamsFromLink(image.getLink(0));
			if (params != null){

				String instrno = params.get("instr");
				String book = params.get("book");
				String page = params.get("page");
				String year = params.get("year");	
		    	String month = params.get("month");
		    	String day = params.get("day");
		    	
		    	String date = StringUtils.leftPad(month, 2, "0") + "-" + StringUtils.leftPad(day, 2, "0") + "-" + year;
		    	String instrument = year + StringUtils.leftPad(instrno, 8, "0");
		    	
		    	String imageLink = ro.cst.tsearch.connection.http3.OHCuyahogaRO.IMAGE_ADDRESS + "?instrno=" + instrument + "&book=" + book + "&page=" + page + 
		    		"&date=" + date + "&ignoreType=true";
		    	HashSet<String> linkRO = new HashSet<String>();
		    	linkRO.add(imageLink);
		    	
		    	ImageI imageForRo = image.clone();
		    	if (imageForRo.getLinks().size() > 0){
		    		imageForRo.setLinks(linkRO);
		    		
		    		long siteId = TSServersFactory.getSiteId(
							getSearch().getSa().getAtribute(SearchAttributes.P_STATE_ABREV), 
							getSearch().getSa().getAtribute(SearchAttributes.P_COUNTY_NAME), 
							"RO");
		    		OHCuyahogaRO ohCuyahogaRo = (OHCuyahogaRO)TSServersFactory.GetServerInstance((int)siteId, getSearch().getID());
					
		    		try {
		    			DownloadImageResult dir = ohCuyahogaRo.saveImage(imageForRo);
		    			if (dir == null){
		    				SearchLogger.info("<br/>Image(searchId=" + searchId + ") book=" 
		    						+ instr.getBook() + " page=" + instr.getPage() + "inst=" + instr.getInstno()
		    						+ " failed to be taken from RO<br/>", searchId);
		    			}
		    			if (dir != null && TSInterface.DownloadImageResult.Status.OK.equals(dir.getStatus())){
		    				byte[] b = new byte[0];
		    	    		try {
		    	    			b = FileUtils.readFileToByteArray(new File(image.getPath()));
		    	    		} catch (IOException e) {e.printStackTrace();}
		    	    		
		    	    		if (b.length > 0){
		    	    			afterDownloadImage(true, GWTDataSite.RO_TYPE);
		    	    			SearchLogger.info("<br/>Image(searchId=" + searchId + ") book=" 
										+ instr.getBook() + " page=" + instr.getPage() + "inst=" + instr.getInstno()
										+ " was taken from RO<br/>", searchId);
		    	    			return dir;
		    	    		}
		    			}
					} catch (ServerResponseException e) {
						e.printStackTrace();
					}
		    	}
			}
		}

		return new DownloadImageResult();
	}
	
}
