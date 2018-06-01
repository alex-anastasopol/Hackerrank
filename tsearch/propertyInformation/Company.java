package ro.cst.tsearch.propertyInformation;

/**
 * Kept only for compatibility with older orders
 */
@Deprecated
public class Company implements Owner {
	@SuppressWarnings("unused")			// Kept only for compatibility with older orders
	private transient String	msName		= null;
	@SuppressWarnings("unused")			// Kept only for compatibility with older orders
	private transient Address	mAddress	= null;
}
