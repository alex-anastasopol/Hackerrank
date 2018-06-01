package ro.cst.tsearch.search.filter.fornext;

import java.math.BigDecimal;
import java.util.List;

import org.apache.log4j.Category;

import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

/**
 * SubdivNameAndLotForNext
 *
 */
public class SubdivNameAndLotForNext extends FilterResponse {
	protected static final Category logger = Category.getInstance(SubdivNameAndLotForNext.class.getName());
	protected static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + SubdivNameAndLotForNext.class.getName());
	
	private String refSubdivName ="";
	private String refLot = "";

	private BigDecimal w0 = new BigDecimal("1.0");
	private BigDecimal w1 = new BigDecimal("1.0");

	public SubdivNameAndLotForNext(String refSubdiv,String refLot,long searchId){
		super(searchId);
		List tokens = StringUtils.splitString(refSubdiv);
		if (tokens.size()>0){
			String last = (String) tokens.get(tokens.size()-1);
			if ((tokens.size()>1)&&(last.length()<3)){//daca am cel putin doi 
				//tokeni din care ultimul este initiala
				//pot sa renunt la ultimul pt matchuire
				refSubdiv = StringUtils.join(tokens.subList(0, tokens.size()-1), " "); 
			}
		}
		this.refSubdivName=refSubdiv;
		this.refLot = refLot;
		threshold = new BigDecimal("0.90");
	}
	
	public BigDecimal getScoreOneRow(ParsedResponse row) {

		String[] candSubdiv = getCandidateSubdiv(row);
		String[] refSubdiv = new String[] {refSubdivName,refLot};
		
		BigDecimal sc0 =  getScoreOneSubdiv(MatchAlgorithm.TYPE_REGISTER_NAME_NA,refSubdiv[0],candSubdiv[0]);
		BigDecimal sc1 =  getScoreOneSubdiv(MatchAlgorithm.TYPE_LOT_ZERO_IGNORE,refSubdiv[1],candSubdiv[1]);
		
		if(betweenLot(refSubdiv[1],candSubdiv[1])) {
			sc1 = ATSDecimalNumberFormat.ONE;
		}
								
		return sc0.max(sc1);
	}


	private BigDecimal getScoreOneSubdiv(int t,String subdiv1, String subdiv2) {
		MatchAlgorithm matcher = MatchAlgorithm.getInstance(t, subdiv1, subdiv2,searchId);
		matcher.setPenilizeDiffNoTokensFlag(true);
		BigDecimal score = matcher.getScore();
		return score;
	}

	private String[] getCandidateSubdiv(ParsedResponse pr) {
		return new String[] {
				pr.getPropertyIdentificationSet(0).getAtribute("SubdivisionName"),
				pr.getPropertyIdentificationSet(0).getAtribute("SubdivisionLotNumber")
		};
	}	
	
	private boolean betweenLot(String ref, String cand) {
		if(!StringUtils.isStringBlank(ref) && 
			!StringUtils.isStringBlank(cand)) {
				try {
					int i0 = Integer.parseInt(ref.trim());
					String[] arr = cand.split("-");
					if(arr != null && arr.length ==2) {
						int i1 = Integer.parseInt(arr[0]);
						int i2 = Integer.parseInt(arr[1]);
						if(i0 >= i1 && i0 <= i2) return true;					
					}
				} catch(Exception e){}
		}
		return false;
	}


}

