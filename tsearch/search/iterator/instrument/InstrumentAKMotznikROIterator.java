package ro.cst.tsearch.search.iterator.instrument;

import java.util.HashMap;

import com.stewart.ats.base.document.InstrumentI;

public class InstrumentAKMotznikROIterator extends InstrumentAKROIterator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	public InstrumentAKMotznikROIterator(long searchId) {
		super(searchId);
	}
	/**
	 * Constructor use to enable/disable BookPage while disabling/enabling InstrumentSearch 
	 * @param searchId
	 * @param enableBookPage
	 */
	public InstrumentAKMotznikROIterator(long searchId, boolean enableBookPage) {
		super(searchId,enableBookPage);
	}
	
	@Override
	public String getInstrumentNoFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
		if(state.getInstno().matches("\\d+-\\d+-\\d")) {
			String result = state.getInstno();
			
			String suffix = state.getInstno().replaceAll("\\d+-\\d+-(\\d)", "$1");
			if(filterCriteria != null) {
				if("0".equals(suffix)) {
					filterCriteria.put("InstrumentNumber", state.getInstno().replaceAll("(\\d+)-(\\d+)-.*", "$1_$2"));
				} else {
					filterCriteria.put("InstrumentNumber", state.getInstno().replaceAll("(\\d+)-(\\d+)-(\\d+)", "$1_$2$3"));
				}
			}
			return result;
		}
		return "";
	}
		
	@Override
	public String getSuffixFrom(InstrumentI state, HashMap<String, String> filterCriteria) {
		if(state.getInstno().matches("\\d+-\\d+-\\d")) {
			return state.getInstno().replaceAll("\\d+-\\d+-(\\d)", "$1");
		}
		return "";
	}
	
}
