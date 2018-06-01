package ro.cst.tsearch.search.iterator.instrument;

import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.search.DocumentsManagerI;

public class InstrumentTXTSInterator extends InstrumentGenericIterator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private boolean above2000 = false; 
	
	public InstrumentTXTSInterator(long searchId) {
		super(searchId);
	}

	@Override
	protected String cleanInstrumentNo(String inst, int year) {
		String instNo = inst.replaceFirst("^0+", "");
		if(instNo.isEmpty()) {
			return instNo;
		}
		instNo = StringUtils.leftPad(instNo, 6, "0");
		if(year>=2000){
			instNo = year + StringUtils.leftPad(instNo, 8, "0");
		}
		instNo = instNo.replaceFirst("^0+", "");
		return instNo;
	}
	
	@Override
	protected void useInstrumentI(List<InstrumentI> result,
			HashSet<String> listsForNow, DocumentsManagerI manager,
			InstrumentI instrumentI) {
		
		int originalSize = result.size();
		super.useInstrumentI(result, listsForNow, manager, instrumentI);
		if( instrumentI.hasYear() && instrumentI.getYear() > 2000 && originalSize < result.size()) {
			above2000 = true;
		}
	}

	public boolean isAbove2000() {
		return above2000;
	}

	public void setAbove2000(boolean above2000) {
		this.above2000 = above2000;
	}

}
