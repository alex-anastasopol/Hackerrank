/*
 * Created on Aug 18, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.wrapper2;

/**
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class action_constants {
	/** skips to the first occurence of the pattern **/
		public static final int ACT_SKIPTO=0;
		/** skips while tokens match pattern **/
		public static final int ACT_SKIPWHILE=1;		
	/** skips until tokens match pattern **/	
	public static final int ACT_SKIPUNTIL=2;	
	/** skips a number of tokens **/
	public static final int ACT_SKIPNR=3;
	/** skips a number of tokens (for forward rule **/
		public static final int ACT_SKIPNRFW=4;
}
