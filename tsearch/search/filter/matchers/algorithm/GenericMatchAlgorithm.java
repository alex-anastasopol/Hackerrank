package ro.cst.tsearch.search.filter.matchers.algorithm;

import java.math.BigDecimal;

import org.apache.log4j.Category;

import ro.cst.tsearch.utils.StringUtils;

public class GenericMatchAlgorithm extends MatchAlgorithm{

	protected static final Category logger = Category.getInstance(LotMatchAlgorithm.class.getName());

	
	public GenericMatchAlgorithm  (long searchId){
		super(searchId);
	}

	public void init(String ref, String cand){	
		super.init( ref, cand);

		refTokens = StringUtils.parseIntegers(StringUtils.join(refTokens, " "), new String[]{" ", "-"});
		candTokens = StringUtils.parseIntegers(StringUtils.join(candTokens, " "), new String[]{" ", "-"});
	}
	
	protected BigDecimal computeFinalScore() {
		return maxNo(getLocalScores());
	}

}
