/*
 * Created on Sep 4, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.filter.matchers.algorithm;

import java.math.BigDecimal;
import java.util.Comparator;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class BestScoreMatchTokenComparator implements Comparator {

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Object o1, Object o2) {
		MatchToken mt1 = (MatchToken) o1;
		MatchToken mt2 = (MatchToken) o2;
		BigDecimal score1 = mt1.getBestScore();
		BigDecimal score2 = mt2.getBestScore();
		return (score2.compareTo(score1));
	}

}
