package ro.cst.tsearch.servers.types;

import java.util.List;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author Cristi Stochina
 */
public class FLLakeDT extends FLSubdividedBasedDASLDT{
	
	private static final long serialVersionUID = -1256127088901832521L;

	public FLLakeDT(long searchId) {
		super(searchId);
	}
	
	public FLLakeDT(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
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
		
		String apn = search.getSa().getAtribute( SearchAttributes.LD_PARCELNO ).replaceAll("[.-]", "");
		
		if(!StringUtils.isEmpty(apn) && StringUtils.isEmpty(tw)){
			apn = apn.replaceAll("[a-zA-Z]", "");
			if(apn.length()>=7){
				sec = apn.substring(0,2).replaceFirst("^0+", "");
				tw = apn.substring(2,4).replaceFirst("^0+", "");
				rg = apn.substring(4,6).replaceFirst("^0+", "");
			}
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
	public void addParcelMapSearch(TSServerInfoModule module, PersonalDataStruct str) {
		super.addDefaultPlatMapSearch(module, str, "PARCEL_MAP");
	}
	
	@Override
	public void addPlatMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
	@Override
	public void addSubdivisionMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
}
