package ro.cst.tsearch.search.address;

import java.io.Serializable;
import java.util.ResourceBundle;

import org.apache.log4j.Category;

import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.URLMaping;

/**
 * Address matching class
 */
public class AddressMatcher implements Serializable {

	static final long serialVersionUID = 10000000;
	private static final Category logger =	Category.getInstance(AddressMatcher.class.getName());
	private static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + AddressMatcher.class.getName());

	private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	private static boolean useNewVersionFlag = Boolean.parseBoolean(rbc.getString("use.new.address.matcher").trim());	
	private static boolean useNewVersion(){ return useNewVersionFlag; }	

	/**
	 * Reference address for matching.
	 */
	private AddressInterface referenceAddress;
	private ro.cst.tsearch.search.address2.AddressMatcher addressMatcher2;
	
	/**
	 * Address tokens.
	 * Array of the reference tokens to make things faster.
	 */
	private String[] referenceTokens = null;
	/**
	 * Token types.
	 * Maybe this is a standard entity such as suffix.
	 */
	private int[] tokenTypes = null;
	/**
	 * Abrev token.
	 * If it is, then store the alternate (Abreviated) rep.
	 */
	private String[] altToken = null;
	/**
	 * Token matcher.
	 * Basic string matcher
	 */
	private BasicFilter matcher = null;
	/**
	 * Used to normalize input address.
	 * Make sense of abreviated tokens.
	 */
	private Normalize stdTokens = null;
	/**
	 * Token array for "" escaped strings (fixed strings)
	 */
	private String[] escapedStrings = null;

	/**
	 * Dummy ;-)
	 *
	 */
	/*
	public AddressMatcher2() {
	};
	*/
	/**
	 * Constructor.
	 * Build tokens and alt tokens for reference address.
	 * 
	 * @param reference
	 */
	public AddressMatcher(AddressInterface reference) {
		if(useNewVersion()){
			addressMatcher2 = new ro.cst.tsearch.search.address2.AddressMatcher(reference);
			return;
		}
		stdTokens = new Normalize();
		referenceAddress = reference;
		referenceTokens = referenceAddress.getAddressElements();
		matcher = new BasicFilter();
		// compute the masks for the token types
		tokenTypes = new int[7];
		altToken = new String[7];
		for (int i = 0; i < 7; i++) {
			if (referenceTokens[i] == null) {
				tokenTypes[i] = Normalize.ABREV_NONE;
				altToken[i] = null;
			} else {
				if (stdTokens.isSuffix(referenceTokens[i])) {
					tokenTypes[i] = Normalize.ABREV_SUFFIX;
					altToken[i] =
						new String(Normalize.translateSuffix(referenceTokens[i]));
				} else if (stdTokens.isState(referenceTokens[i])) {
					tokenTypes[i] = Normalize.ABREV_STATE;
					altToken[i] = new String(Normalize.translateState(referenceTokens[i]));
				} else if (stdTokens.isIdentifier(referenceTokens[i])) {
					tokenTypes[i] = Normalize.ABREV_IDENT;
					altToken[i] =
						new String(Normalize.translateIdentifier(referenceTokens[i]));
				} else if (stdTokens.isDirectional(referenceTokens[i])) {
					tokenTypes[i] = Normalize.ABREV_DIR;
					altToken[i] = new String(Normalize.translateDirection(referenceTokens[i]));
				} else {
					tokenTypes[i] = Normalize.ABREV_NONE;
					altToken[i] = referenceTokens[i];
				}
			}
		}

		// build escaped strings array
		escapedStrings =
			AddressStringUtils.getEscapedStrings(
				reference.getAddrInputString());
	}

	public void setTokenWeights(double [] newWeights) {
		if(useNewVersion()){
			addressMatcher2.setTokenWeights(newWeights);
		}		
	}

	public void setMissingValues(double [] newValues) {
		if(useNewVersion()){
			addressMatcher2.setMissingValues(newValues);
		}
	}

	//private static LowPriorityFileLogger addressLogger =  new LowPriorityFileLogger("tslogs/compared-addresses-old.txt",100);
	
	/**
	 * Computes scores between reference address and candidate.
	 * 
	 * @param candidate
	 * 
	 * @return Matching score
	 */
	public double matches(AddressInterface candidate) {
		if(useNewVersion()){
			return addressMatcher2.matches(candidate)[7];
		}
		// escape candidate address only if necessary
		if(escapedStrings.length != 0){
			candidate =	new StandardAddress(
					AddressStringUtils.escapeString(
						candidate.getAddrInputString(),
						escapedStrings));
		}
		String[] candTokens = candidate.getAddressElements();

		// basic scheme, for common tokens add the score, return scaled at the end
		double common = 0;
		double score = 0;
		for (int i = 0; i < 7; i++) {
			// skip cases
			// if the reference does not contain a street number, then don't count it on the reference
			// either even if it exists
			if (i == 0 && referenceTokens[i] == null)
				continue;
			// add 1 to the common tokens if at least one of the adresses contains a non-null token
			// in order to get the max number of tokens at the end
			if (candTokens[i] != null || referenceTokens[i] != null)
				common += 1;
			if (candTokens[i] != null && referenceTokens[i] != null) {
				// before sending them to the matcher, see if we can get an exact match by running them
				// through abreviation filters; fortunately they have to be in the same class to match
				String tt = null;
				switch (tokenTypes[i]) {
					case Normalize.ABREV_SUFFIX :
						tt = (String) Normalize.translateSuffix(candTokens[i]);
						if (tt != null && altToken[i].compareTo(tt) == 0)
							score += 1;
						else
							score
								+= matcher.score(
									candTokens[i],
									referenceTokens[i]);
						break;
					case Normalize.ABREV_STATE :
						tt = (String) Normalize.translateState(candTokens[i]);
						if (tt != null && altToken[i].compareTo(tt) == 0)
							score += 1;
						else
							score
								+= matcher.score(
									candTokens[i],
									referenceTokens[i]);
						break;
					case Normalize.ABREV_IDENT :
						tt = (String) Normalize.translateIdentifier(candTokens[i]);
						if (tt != null && altToken[i].compareTo(tt) == 0)
							score += 1;
						else
							score
								+= matcher.score(
									candTokens[i],
									referenceTokens[i]);
						break;
					case Normalize.ABREV_DIR :
						tt = (String) Normalize.translateDirection(candTokens[i]);
						if (tt != null && altToken[i].compareTo(tt) == 0)
							score += 1;
						else
							score
								+= matcher.score(
									candTokens[i],
									referenceTokens[i]);
						break;
					case Normalize.ABREV_NONE :
						score
							+= matcher.score(candTokens[i], referenceTokens[i]);
						break;
				}
			}
		}
		double sc = score / common;
		sc =  (sc > 1.0 ? 1.0 : sc);				
		//addressLogger.logString("Comparing ref |" + referenceAddress + "| vs |" + candidate +"|=" + sc);		
		return sc;
	}
	
	
}
