package ro.cst.tsearch.search.filter.matchers.algorithm;

import java.math.BigDecimal;
import java.util.Vector;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.StringUtils;

public class LotMatchAlgorithm extends MatchAlgorithm{

	protected static final Category logger = Logger.getLogger(LotMatchAlgorithm.class);

	public LotMatchAlgorithm  (long searchId){
		super(searchId);
	}

	Vector<LotInterval> refs = new Vector<LotInterval>();
	Vector<LotInterval> cands = new Vector<LotInterval>();
	
	public void init(String ref, String cand) {
	    
		super.init( ref, cand);
		
		this.refs = prepareLotInterval(ref);
		/*String[] refs = ref.split(",");
		for (int i = 0; i < refs.length; i++) {
		    this.refs.add(new LotInterval(refs[i]));
		}*/
		
		
		this.cands = prepareLotInterval(cand);
		/*String[] cands = cand.split(",");
		for (int i = 0; i < cands.length; i++) {
		    this.cands.add(new LotInterval(cands[i]));
		}*/
		
		/*refTokens = StringUtils.parseIntegers(StringUtils.join(refTokens, " "), new String[]{" ", "-"});
		candTokens = StringUtils.parseIntegers(StringUtils.join(candTokens, " "), new String[]{" ", "-"});*/
	}
	
	public static Vector<LotInterval> prepareLotInterval(String lotInterval) {
		
		Vector<LotInterval> v = new Vector<LotInterval>();
		
//		if(StringUtils.isEmpty(lotInterval)) {
//			return v;
//		}
	    
	    lotInterval = lotInterval.replaceAll("\\s*&\\s*", ",");
	    lotInterval = lotInterval.replaceAll("\\s*,\\s*", ",");
	    lotInterval = lotInterval.replaceAll("(?s)\\s*thru\\s*", "-");
	    lotInterval = lotInterval.replaceAll("(?s)\\s*to\\s*", "-");
	    lotInterval = lotInterval.replaceAll("\\s*-\\s*", "-");
		lotInterval = lotInterval.replaceAll(" ", ",");

		String[] refs = lotInterval.split(",");
		
		
		
		for (int i = 0; i < refs.length; i++) {
			
			LotInterval newInterval = new LotInterval(refs[i]);
			
			boolean added = false;
			for (int j = 0; j < v.size(); j++) {
				
				LotInterval interval = (LotInterval) v.elementAt(j);
				
				if (interval.related(newInterval)) {
					interval.add(newInterval);
					added = true;
					break;
				}
			}
			
			if (!added) {
				v.add(newInterval);
			}
		}
		
		return v;
	}
	
	protected BigDecimal computeFinalScore() {
		//return maxNo(getLocalScores());
	    
	    for (int i = 0; i < cands.size(); i++) {
	        
	        LotInterval candInterval = (LotInterval) cands.elementAt(i);
	        
	        for (int j = 0; j < refs.size(); j++) {
	            LotInterval refInterval = (LotInterval) refs.elementAt(j);
	            
	            if (refInterval.contains(candInterval))
	                return ATSDecimalNumberFormat.ONE;
	        }
	    }
	    
	    return ATSDecimalNumberFormat.ZERO;
	}
}
