package ro.cst.tsearch.search.name;

/**
 * INameMatcher
 *
 * @author catalinc
 */
public interface INameMatcher {
	/**
	 * Matches given names.
	 * 
	 * @param n1 Name 1
	 * @param n2 Name 2
	 * @return Match score.
	 */
	public double match(Name n1, Name n2);
	/**
	 * Matches given name strings.
	 * 
	 * Strings are parsed used specified name parser.
	 * 
	 * @param n1 Name 1 as string.
	 * @param n2 Name 2 as string.
	 * @param np Name parser.
	 * @param weight Name tokens weight.
	 * @return Match score.
	 */
	public double match(String n1,String n2,INameParser np);	
	/**
	 * Sets name tokens score weight array.
	 *  
	 * @param weight tokens score weight array.
	 */
	public void setWeightArray(double[] weight);	
}
