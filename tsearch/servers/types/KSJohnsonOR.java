package ro.cst.tsearch.servers.types;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.search.iterator.instrument.InstrumentGenericIterator;

import com.stewart.ats.base.document.InstrumentI;

/**
 * @author Radu Bacrau
 */
public class KSJohnsonOR extends GenericOrbit {

	private static final long serialVersionUID = 224346344606092458L;

	public KSJohnsonOR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, miServerID);
	}
	
	public KSJohnsonOR(long searchId) {
		super(searchId);
	}

	@Override
	protected String retrieveImage(String para, String docNo, String fileName){
		// TODO: Add code to first try to get image from KSJohnsonRO
		// then revert to super.retrieveImage(...) if not succeeded		
		String newFileName = KSJohnsonRO.retrieveImageFromLink(docNo,fileName,searchId); 
		if(newFileName!=null){
			return newFileName;
		} else {
			return super.retrieveImage(para, docNo, fileName);
		} 
	
	}
	
	@Override
	public InstrumentGenericIterator getInstrumentNumberIterator() {
		InstrumentGenericIterator instrumentGenericIterator = new InstrumentGenericIterator(searchId, getDataSite()) {

			private static final long	serialVersionUID	= 9192626185094643751L;

			@Override
			public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
				String instrNo = state.getInstno().trim();
				if (StringUtils.isNotEmpty(instrNo) && instrNo.matches("\\d{4,5}")) {
					Date date = state.getDate();
					if (date!=null) {
						String dateStr = new SimpleDateFormat("yyyyMMdd").format(date);
						instrNo = dateStr + StringUtils.leftPad(instrNo, 7, "0");
					}
				}
				return instrNo;
			}
			
		};
				
		return instrumentGenericIterator;
	}

}
