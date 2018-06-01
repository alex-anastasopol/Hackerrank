package ro.cst.tsearch.propertyInformation;


/**
 * Kept only for compatibility with older orders
 */
@Deprecated
public class Family implements Owner
{
	@SuppressWarnings("unused")				//Kept only for compatibility with older orders
	private transient Person mOwner = null;
	@SuppressWarnings("unused")				//Kept only for compatibility with older orders
	private transient Person mSpouse = null;
	@SuppressWarnings("unused")				//Kept only for compatibility with older orders
	private transient Address mAddress = null;

}
