package ro.cst.tsearch.servers.types;

import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;

/**
 * @author Radu Bacrau
 */
public class MOClayOR extends GenericOrbit {

	private static final long serialVersionUID = 224346344606092458L;

	public MOClayOR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	public MOClayOR(long searchId) {
		super(searchId);
	}

	@Override
	protected String retrieveImage(String para, String docNo, String fileName){
		// TODO: Add code to first try to get image from MOClayRO
		// then revert to super.retrieveImage(...) if not succeeded		
		/*if(MOClayRO.retrieveImageFromLink("",fileName,searchId)){
			return true;
		} 
		else 
		{*/
			//B4996
	      return super.retrieveImage(para, docNo, fileName);
		//} 
	
	}

	@Override
	public InstrumentGenericIterator getInstrumentNumberIterator() {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId, getDataSite()) {

			private static final long	serialVersionUID	= -2477680486918849905L;
			
			@Override
			public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				String instrNo = state.getInstno().trim();
				if (StringUtils.isNotEmpty(instrNo) && instrNo.matches("\\d{4,5}")) {
					int year = state.getYear();
					if (year!=SimpleChapterUtils.UNDEFINED_YEAR) {
						instrNo = year + StringUtils.leftPad(instrNo, 6, "0");
					}
				}
				return instrNo;
			}
			
		};
				
		return instrumentGenericIterator;
	}
	
}
