package ro.cst.tsearch.servers.types;

import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;

import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;

/**
 * @author Radu Bacrau
 */
public class KSWyandotteOR extends GenericOrbit {

	private static final long serialVersionUID = 224346344606092458L;

	public KSWyandotteOR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	public KSWyandotteOR(long searchId) {
		super(searchId);
	}

	@Override
	protected String retrieveImage(String para, String docNo, String fileName){
	      return super.retrieveImage(para, docNo, fileName);
    }
	
	@Override
	public InstrumentGenericIterator getInstrumentNumberIterator() {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId, getDataSite()) {

			private static final long	serialVersionUID	= -6770069157494357188L;

			@Override
			public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				String instrNo = state.getInstno().trim();
				if (StringUtils.isNotEmpty(instrNo) && instrNo.matches("\\d{4,5}")) {
					int year = state.getYear();
					if (year!=SimpleChapterUtils.UNDEFINED_YEAR) {
						instrNo = year + StringUtils.leftPad(instrNo, 5, "0");
					}
				}
				return instrNo;
			}
			
		};
				
		return instrumentGenericIterator;
	}
	
}
