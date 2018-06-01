package ro.cst.tsearch.servers.types;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

import com.stewart.ats.base.document.InstrumentI;

import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;


/**
 * @author Cristian Stochina
 */
public class FLGilchristDT extends FLSubdividedBasedDASLDT{

	private static final long serialVersionUID = 3477834L;

	public FLGilchristDT(long searchId) {
		super(searchId);
	}
	
	public FLGilchristDT(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	public void addAssesorMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}

	@Override
	public void addPlatMapSearch(TSServerInfoModule module,PersonalDataStruct str) {}
	
	@Override
	public void addParcelMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
	@Override
	public void addSubdivisionMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
	@Override
	public void addCondoMapSearch(TSServerInfoModule module, PersonalDataStruct str) {}
	
	@Override
	protected boolean addBookPageSearch(InstrumentI inst,TSServerInfo serverInfo, List<TSServerInfoModule> modules,long searchId, Set<String> searched,boolean isUpdate){
		
		if(inst.hasBookPage()){
			String book = inst.getBook().replaceFirst("^0+", "");
			String page = inst.getPage().replaceFirst("^0+", "");
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
			
			int year=-1;
			try{year = Integer.parseInt(book);}catch(Exception e){}
			
			int currentYear = Calendar.getInstance().get(Calendar.YEAR);
			
			if(year>=2000 && year<=currentYear){
				InstrumentI inst1 = inst.clone();
				inst1.setBook("");
				inst1.setPage("");
				inst1.setYear(year);
				inst1.setInstno(page);
				addInstNoSearch(inst1, serverInfo, modules, searchId, searched, isUpdate);
			}
			
			return true;
			
		}
		
		return false;
	}
}
