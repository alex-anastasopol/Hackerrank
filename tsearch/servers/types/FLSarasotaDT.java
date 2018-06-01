package ro.cst.tsearch.servers.types;

import java.util.List;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

/**
 * @author Cristi Stochina
 */
public class FLSarasotaDT extends FLSubdividedBasedDASLDT{
	
	private static final long serialVersionUID = -1256127088901832521L;
	
	/*
	public final Date MINIMUM_INSTRUMENT_RECORDED_DATE; 
	{
	Calendar cal = Calendar.getInstance();
	cal.set(Calendar.YEAR, 1998);
	cal.set(Calendar.MONTH, 5);
	cal.set(Calendar.DAY_OF_MONTH, 1);
	MINIMUM_INSTRUMENT_RECORDED_DATE= cal.getTime();
	}
	*/
	
	public FLSarasotaDT(long searchId) {
		super(searchId);
	}
	
	public FLSarasotaDT(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	
	public void addCondoMapSearch(TSServerInfoModule module, PersonalDataStruct str) {
		String isCondo = getSearchAttribute(SearchAttributes.IS_CONDO);
	
		if( "true".equalsIgnoreCase(isCondo) || !StringUtils.isEmpty(getSearchAttribute(SearchAttributes.P_STREETUNIT))){
			CurrentInstance ci 	= InstanceManager.getManager().getCurrentInstance(searchId);
			Search search 		= ci.getCrtSearchContext();
		
			String book  = str.getPlatBook();
			String page  = str.getPlatPage();
			
			DocumentsManagerI m = search.getDocManager();
			
			uncheckPlats(m);
			
			if( StringUtils.isEmpty(book)||StringUtils.isEmpty(page) ){
				try{
					m.getAccess();
					String[] subdivVector = getSubdivisionVector(m);
					if(StringUtils.isEmpty(book)){
						book = subdivVector[0];
					}
					if(StringUtils.isEmpty(page)){
						page = subdivVector[1];
					}
				}finally{
					m.releaseAccess();
				}
			}
			
			if(StringUtils.isEmpty(book)||StringUtils.isEmpty(page)){
				try{
					m.getAccess();
					List<DocumentI> list = m.getDocumentsWithType(true, DType.ASSESOR, DType.TAX, DType.CITYTAX);
					boolean found = false;
					for(DocumentI doc:list){
						for(PropertyI prop:doc.getProperties()){
							if(prop.hasSubdividedLegal()){
								String book1 = prop.getLegal().getSubdivision().getPlatBook();
								String page1 =  prop.getLegal().getSubdivision().getPlatPage();
								if(!StringUtils.isEmpty(book1)&&!StringUtils.isEmpty(page1)){
									book  = book1;
									page = page1;
									found = true;
									break;
								}
							}
						}
						if(found){
							break;
						}
					}
				}finally{
					m.releaseAccess();
				}
			}
			
			if(!StringUtils.isEmpty(book)&&!StringUtils.isEmpty(page)){
				try{
					m.getAccess();
					List<DocumentI> list = m.getDocuments("PLAT");
					if(list!=null){
						for(DocumentI doc:list){
							if(doc.hasBookPage()){
								if(doc.getBook().equals(book)&&doc.getPage().equals(page)&&doc.isFake()){
									return;
								}
							}
						}
					}
				}
				finally{
					m.releaseAccess();
				}
			}
			
			if(!StringUtils.isEmpty(book) && book.length()>=2){
				book = book.replaceFirst("[a-zA-Z]", "");
			}
			
			if(!StringUtils.isEmpty(page) && page.length()>=2){
				page = page.replaceAll("[a-zA-Z]", "");
			}
			if (StringUtils.isEmpty(page)){
				page ="";
			}
			
			deletePlat(m, book, page.split("[ -]")[0]);
			
			module.forceValue(0,book);
			module.forceValue(1,page.split("[ -]")[0]);
			module.forceValue(4,"PLAT");
			module.forceValue(5,"CONDOMINIUM_MAP");
		}
		
	}
	
	
	@Override
	protected void addAssesorMapSearch(TSServerInfoModule module, PersonalDataStruct str) {
		CurrentInstance ci 	= InstanceManager.getManager().getCurrentInstance(searchId);
		Search search 		= ci.getCrtSearchContext();
	
		String tw  = str.getTownship();
		String rg  = str.getRange();
		String sec = str.getSection();
		
		DocumentsManagerI m = search.getDocManager();
		uncheckPlats(m);
		if(StringUtils.isEmpty(tw)||StringUtils.isEmpty(rg)||StringUtils.isEmpty(sec)){
			try{
				m.getAccess();
				String[] subdivVector = getSubdivisionVector(m);
				if(StringUtils.isEmpty(tw)){
					tw = subdivVector[2];
				}
				if(StringUtils.isEmpty(rg)){
					rg = subdivVector[3];
				}
				if(StringUtils.isEmpty(sec)){
					sec = subdivVector[3];
				}
			}finally{
				m.releaseAccess();
			}
		}
		if(StringUtils.isEmpty(tw)){
			tw = search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_TWN);
		}
		if(StringUtils.isEmpty(rg)){
			rg = search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_RNG);
		}
		if(StringUtils.isEmpty(sec)){
			sec = search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_SEC);
		}
		
		if(!StringUtils.isEmpty(tw) && tw.length()>=2){
			tw = tw.replaceAll("[a-zA-Z]", "").replaceFirst("^0+", "");
		}
		
		if(!StringUtils.isEmpty(rg) && rg.length()>=2){
			rg = rg.replaceAll("[a-zA-Z]", "").replaceFirst("^0+", "");
		}
		
		if(!StringUtils.isEmpty(sec) && sec.length()>=2){
			sec = sec.replaceAll("[a-zA-Z]", "").replaceFirst("^0+", "");
		}
		deletePlat(m, tw, rg);
		module.forceValue(0,tw);
		module.forceValue(1,rg);
		module.forceValue(2,sec);
		module.forceValue(4,"PLAT");
		module.forceValue(5,"ASSESSOR_MAP");
		
		return;
	}
	
	@Override 
	protected ServerResponse searchBy(TSServerInfoModule module, Object sd, String fakeResponse) throws ServerResponseException {
		if( module.getModuleIdx() == TSServerInfo.IMG_MODULE_IDX){
			return super.searchBy(module, sd, "");
		}
		return super.searchBy(module, sd, fakeResponse);
	}
	
	@Override
	public void addParcelMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
	@Override
	public void addSubdivisionMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
	protected boolean downloadImageFromOtherSiteImpl(InstrumentI instrument, ImageLinkInPage image) {
		
		boolean useDoctypeInImageSearch = true;
		if(DocumentTypes.PLAT.equalsIgnoreCase(instrument.getDocType())) {
			useDoctypeInImageSearch = false;
		}
		return downloadImageFromRV(makeInstrumentForRV(instrument), image, useDoctypeInImageSearch);
	}
	
	private InstrumentI makeInstrumentForRV(InstrumentI dtInstrument) {
		InstrumentI rvInstrument = dtInstrument.clone();
		rvInstrument.setInstno(dtInstrument.getYear() + dtInstrument.getInstno());
		return rvInstrument;
	}

	public static void processInstrumentNo(InstrumentI instr) {
		try {
			if (instr.hasYear()) {
				String instNo = instr.getInstno();
				if (instNo.length() < 6) {
					instNo = "000000".substring(instr.getInstno().length()) + instNo;
				}
				instr.setInstno(instr.getYear() + instNo);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void processInstrumentBeforeAdd(DocumentI doc) {
		
		try {
//			if(doc.hasYear()) {
//				String instNo = doc.getInstno();
//				if(instNo.length()<6) {
//					instNo = "000000".substring(doc.getInstno().length()) + instNo;
//				}
//				doc.setInstno(doc.getYear() + instNo);
//			}
			
			processInstrumentNo(doc.getInstrument());
			
			/*
			if((doc instanceof RegisterDocumentI) && ((RegisterDocumentI)doc).getRecordedDate() != null) {
				if(((RegisterDocumentI)doc).getRecordedDate().before(MINIMUM_INSTRUMENT_RECORDED_DATE)) {
					doc.setInstno("");
				}
			}
			*/
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public int getImageDataSource() {
		return HashCountyToIndex.getServer(CountyConstants.FL_Sarasota, "RV").getCityCheckedInt();	
	}
	
}
