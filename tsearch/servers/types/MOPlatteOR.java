package ro.cst.tsearch.servers.types;

import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;

/**
 * @author Radu Bacrau
 */
public class MOPlatteOR extends GenericOrbit {

	private static final long serialVersionUID = 224346344606092458L;

	public MOPlatteOR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	public MOPlatteOR(long searchId) {
		super(searchId);
	}

	@Override
	protected String retrieveImage(String para, String docNo, String fileName){
	      return super.retrieveImage(para, docNo, fileName);
    }
	
	@Override
	protected DownloadImageResult searchForImage(DocumentI doc) throws ServerResponseException {
	 
		return MOPlatteImageRetriever.getSingletonObject().retrieveImage(dataSite, doc, searchId);
	}
	
	@Override
	public InstrumentGenericIterator getInstrumentNumberIterator() {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId, getDataSite()) {

			private static final long	serialVersionUID	= 1291084821123247074L;

			@Override
			public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				String instrNo = state.getInstno().trim();
				if (StringUtils.isNotEmpty(instrNo) && instrNo.matches("\\d{4,5}")) {
					int year = state.getYear();
					if (year!=SimpleChapterUtils.UNDEFINED_YEAR) {
						instrNo = year + instrNo;
					}
				}
				return instrNo;
			}
			
		};
				
		return instrumentGenericIterator;
	}
}
