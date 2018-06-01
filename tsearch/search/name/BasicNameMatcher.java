package ro.cst.tsearch.search.name;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Category;

/**
 * BasicNameMatcher
 *
 * @author catalinc
 */
public class BasicNameMatcher implements INameMatcher {

	protected static final Category logger= Category.getInstance(BasicNameMatcher.class.getName());

	/**
	 * Weights array.
	 */
	double[] weights = new double[]{1.0,1.0,1.0,1.0,1.0,1.0};
	
	/**
	 * Matcher used.
	 */
	SubSeqMatcher ssm = new SubSeqMatcher();

	/**
	 * @see ro.cst.tsearch.search.name.INameMatcher#match(ro.cst.tsearch.search.name.Name, ro.cst.tsearch.search.name.Name)
	 */
	public double match(Name n1, Name n2) {
		double score = 0.0;
		if(n1.isEmpty() || n2.isEmpty()) {
			return 0.0;
		}
		for(int i = 0; i < 6; i++) {
			String t1 = n1.getNameElement(i);
			String t2 = n2.getNameElement(i);
			score += (ssm.score(t1,t2)*weights[i]);
		}
		double wSum = 0.0;
		for(int j=0; j<weights.length; j++)
			wSum += weights[j];
		return score / wSum;
	}

	/**
	 * @see ro.cst.tsearch.search.name.INameMatcher#match(java.lang.String, java.lang.String, ro.cst.tsearch.search.name.INameParser)
	 */
	public double match(String s1, String s2, INameParser np) {
		double score = 0.0;

		List l1 = np.parseNames(s1);
		List l2 = np.parseNames(s2);

		for (Iterator it1 = l1.iterator(); it1.hasNext();) {
			Name n1 = (Name) it1.next();
			for (Iterator it2 = l2.iterator(); it2.hasNext();) {
				Name n2 = (Name) it2.next();
				double sc = match(n1,n2);
				score = score < sc ? sc : score;
			}
			
		}
		return score;
	}

	/**
	 * @see ro.cst.tsearch.search.name.INameMatcher#setWeightArray(double[])
	 */
	public void setWeightArray(double[] weight) {
		weights = weight;
	}
		 
	public static void main(String[] args) {
		String s1 = " ROBERTSON TERRY A ETUX";
		String s2 = "ROBERTON TERRY A ETUX";
		BasicNameParser bnp = new BasicNameParser("LFM");
		BasicNameMatcher bnm = new BasicNameMatcher();
		logger.info(""+bnm.match(s1,s2,bnp));
	}
}
