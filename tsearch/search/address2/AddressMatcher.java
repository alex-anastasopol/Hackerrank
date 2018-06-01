package ro.cst.tsearch.search.address2;

//import ro.cst.tsearch.utils.LowPriorityFileLogger;
import ro.cst.tsearch.search.address.AddressInterface;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericLegal;

/**
 * Class to match a set of candidates against a reference address, and return the scores.
 * The weights for each address token, or the default values for missing tokens can also be modified.
 *
 * TODO:
 * - make classes of identifiers that are interchangable (such as unit - apt) for better accuracy
 * - not checking for enough "nulls" expecially at splits
 * - maybe check for the common range not just at the end; read the code for the secondary identifiers for details
 * - the altToken system can be cleaned up (and the code reduced, made more clear) if one creates an AltToken interface
 *   with getScore() of its own`
 */
public class AddressMatcher {
	
	/**
	 * Store the parsed reference address so we don't waste time for each candidate
	 */
	private AddressInterface referenceAddress;

	/**
	 * array of the reference tokens to make things faster
	 */
	private String [] referenceTokens = null;

	/**
	 * If the candidate is a standard type, store to help future matching
	 */
	private int [] tokenTypes = null;

	/**
	 * If we have a standard type token, store abbreviated form
	 */
	private String [] altToken = null;			// if it is, then store the alternate (Abreviated) rep

	/**
	 * Store the weight for score of each token in the match process (apt-apt, number-number, etc)
	 * Defaults to 1 for all tokens
	 */
	private double [] tokenWeight = null;

	/**
	 * Store the default value for score between missing tokens.
	 * Defaults to 0.8
	 */
	private double [] missingValue = null;

	/**
	 * Parse the reference trying to determine token types. Store reference tokens for future matching
	 */
	public AddressMatcher(AddressInterface reference) {
		
		referenceAddress = reference;
		referenceTokens = referenceAddress.getAddressElements();
		// compute the masks for the token types
		tokenTypes = new int[7];
		altToken = new String[7];
		tokenWeight = new double[7];
		missingValue = new double[7];
		for (int i = 0; i < 7; i++) {
			if (referenceTokens[i] == null) {
				tokenTypes[i] = Normalize.ABREV_NONE;
				altToken[i] = null;
			} else {
				// since checking if this is a number throws an exception, we compute this outside if/else
				int intValue = 0;
				boolean isInt = false;
				try {
					intValue = Integer.parseInt(referenceTokens[i]);
					isInt = true;
				} catch (NumberFormatException e) {
					// completely ignored
				}

				if (Normalize.isSuffix(referenceTokens[i])) {
					tokenTypes[i] = Normalize.ABREV_SUFFIX;
					altToken[i] = (String)Normalize.translateSuffix(referenceTokens[i]);
				} else if (Normalize.isState(referenceTokens[i])) {
					tokenTypes[i] = Normalize.ABREV_STATE;
					altToken[i] = (String)Normalize.translateState(referenceTokens[i]);
				} else if (Normalize.isIdentifier(referenceTokens[i])) {
					tokenTypes[i] = Normalize.ABREV_IDENT;
					altToken[i] = (String)Normalize.translateIdentifier(referenceTokens[i]);
				} else if (Normalize.isDirectional(referenceTokens[i])) {
					tokenTypes[i] = Normalize.ABREV_DIR;
					altToken[i] = (String)Normalize.translateDirection(referenceTokens[i]);
				} else if (isInt) {
					// this gets rid of leading 0s
					tokenTypes[i] = Normalize.ABREV_NUMBER;
					altToken[i] = Integer.toString(intValue);
				} else if (Normalize.isCompositeNumber(referenceTokens[i])) {
					tokenTypes[i] = Normalize.ABREV_NUMBER;
					altToken[i] = referenceTokens[i];
				} else if (Ranges.isNumberRange(referenceTokens[i])) {
					tokenTypes[i] = Normalize.ABREV_RANGE;
					// we don't have an extra representation
					altToken[i] = referenceTokens[i];
				} else if (Ranges.isLetterRange(referenceTokens[i])) {	// This is actually very unlikely in reference
					tokenTypes[i] = Normalize.ABREV_LETTERS;
					// we don't have an extra representation
					altToken[i] = referenceTokens[i];
				} else if (Ranges.isNumberList(referenceTokens[i])) {
					tokenTypes[i] = Normalize.ABREV_LIST;
					// we don't have an extra representation
					altToken[i] = referenceTokens[i];
				} else {
					tokenTypes[i] = Normalize.ABREV_NONE;
					altToken[i] = referenceTokens[i];
				}
			}
			tokenWeight[i] = 1;
			//missingValue[i] = 0.8;
			missingValue[i] = 0.0;
		}
	}

	/**
	 * Override the default token weights, applied when calculating the final score of the token
	 */
	public void setTokenWeights(double [] newWeights) {
		for (int i = 0; i < 7; i++) {
			tokenWeight[i] = newWeights[i];
		}
	}

	/**
	 * Override the default value to which the score is set if the token is missing in either the candidate or the reference
	 */
	public void setMissingValues(double [] newValues) {
		for (int i = 0; i < 7; i++) {
			missingValue[i] = newValues[i] > 1 ? 1 : newValues[i];
		}
	}
	
	//private static LowPriorityFileLogger addressLogger =  new LowPriorityFileLogger("tslogs/compared-addresses-new.txt",100);
	
	public double[] matches(AddressInterface candidate) {		
		String [] candTokens = candidate.getAddressElements();
		
		double[] scores1 = matches_internal(candTokens);
		if (scores1[7] >= 0.9)
			return scores1;
		
		// if reference has a pre-direction and candidate has a post-direction or vice-versa 
		// interchange pre-direction with post-direction at candidate and compute the new score	- fix for bug #1447
		boolean interchanged = false;				 		
		if (referenceTokens[1] != null && referenceTokens[4] == null && candTokens[1] == null && candTokens[4] != null){
			candTokens[1] = candTokens[4];
			candTokens[4] = null;
			interchanged = true;
		} else if (referenceTokens[1] == null && referenceTokens[4] != null && candTokens[1] != null && candTokens[4] == null){
			candTokens[4] = candTokens[1];
			candTokens[1] = null;
			interchanged = true;
		}
					
		if (!interchanged)
			return scores1;
		
		double[] scores2 = matches_internal(candTokens);
		if (scores2[7] < 0.8)
			return scores1;
		else if (scores2[7] == 1)
			return scores2;
		else if (scores1[7] > scores2[7] - 0.02)
			return scores1;
		else {
			scores2[7] -= 0.02; 
			return scores2;
		}
	}
	/**
	 * Return the score with which the reference address matches with the candidate
	 */
	private double[] matches_internal(String [] candTokens) {		
		// basic scheme, for common tokens add the score, return scaled at the end
		double common = 0;
		double score = 0;
		
		double[] scores = new double[8];
		
		for  (int i = 0; i < 7; i++) {
			double indScore = 0.0;

			// skip cases

			// add 1 to the common tokens if at least one of the adresses contains a non-null token
			// in order to get the max number of tokens at the end
			if (candTokens[i] != null || referenceTokens[i] != null) {
				common += tokenWeight[i];
				indScore = missingValue[i]; // if token exists both places this will get overwritten				
			}
			// # matches anything on token 5
			if (i == 5) {
				if("#".equals(referenceTokens[i]) || "#".equals(candTokens[i])) {
					score += tokenWeight[i];
					scores[i] = tokenWeight[i];
					continue;
				}
				if (referenceTokens[i] == null && candTokens[i] == null) {
					common += tokenWeight[i];
					score += tokenWeight[i];
					scores[i] = tokenWeight[i];
					continue;
				}
			}
			
			if (candTokens[i] != null && referenceTokens[i] != null) {
				// TODO: this enumeration can be generalized but the code would be as long
				
				if (candTokens[i].equals(referenceTokens[i])) {
					scores[i] = tokenWeight[i];
					score += scores[i];
					continue;
				}
				
				// number-range
				if (Ranges.isNumberRange(candTokens[i])) {
					if(tokenTypes[i] == Normalize.ABREV_NUMBER) {
						scores[i] = Ranges.isNumberInRange(candTokens[i], referenceTokens[i]) ? tokenWeight[i] : 0;
					} else if (tokenTypes[i] == Normalize.ABREV_RANGE) {
						scores[i] = Ranges.isRangeInRange(candTokens[i], referenceTokens[i]) ? tokenWeight[i] : 0;
					}
					score += scores[i];
					continue;
					
				} else if (Normalize.isCompositeNumber(candTokens[i]) && (tokenTypes[i] == Normalize.ABREV_RANGE)) {
					scores[i] = Ranges.isNumberInRange(referenceTokens[i], candTokens[i]) ? tokenWeight[i] : 0;
					score += scores[i];
					continue;
				}

				// number-list
				if (Ranges.isNumberList(candTokens[i]) && (tokenTypes[i] == Normalize.ABREV_NUMBER)) {
					scores[i] = Ranges.isNumberInList(candTokens[i], referenceTokens[i]) ? tokenWeight[i] : 0;
					score += scores[i];
					continue;
				} else if (Normalize.isCompositeNumber(candTokens[i]) && (tokenTypes[i] == Normalize.ABREV_LIST)) {
					scores[i] = Ranges.isNumberInList(referenceTokens[i], candTokens[i]) ? tokenWeight[i] : 0;
					score += scores[i];
					continue;
				}

				// list-list
				if (Ranges.isNumberList(candTokens[i]) && (tokenTypes[i] == Normalize.ABREV_LIST)) {
					scores[i] = Ranges.isListInList(candTokens[i], referenceTokens[i]) ? tokenWeight[i] : 0;
					score += scores[i];
					continue;
				}

				// range-range not implemented yet

				// list in range
				if (Ranges.isNumberList(candTokens[i]) && (tokenTypes[i] == Normalize.ABREV_RANGE)) {
					scores[i] = Ranges.isListInRange(candTokens[i], referenceTokens[i]) ? tokenWeight[i] : 0;
					score += scores[i];
					continue;
				}

				if (Ranges.isLetterRange(candTokens[i])) {
					scores[i] = Ranges.isLetterInRange(candTokens[i], referenceTokens[i]) ? tokenWeight[i] : 0;
					score += scores[i];
					continue;
				} else if (tokenTypes[i] == Normalize.ABREV_LETTERS) {
					scores[i] = Ranges.isLetterInRange(referenceTokens[i], candTokens[i]) ? tokenWeight[i] : 0;
					score += scores[i];
					continue;
				}

				if(i==6 && candTokens[i].matches("(?i)\\d+[A-Z]*") && referenceTokens[i].matches("(?i)\\d+[A-Z]*")){
					indScore = GenericLegal.computeScoreInternal("", candTokens[i], referenceTokens[i], true, false);					
				} else {
					// before sending them to the matcher, see if we can get an exact match by running them
					// through abreviation filters; fortunately they have to be in the same class to match
					String tt = null;
					switch (tokenTypes[i]) {
						case Normalize.ABREV_SUFFIX :
							// falls down, treated the same as direction
							tt = (String)Normalize.translateSuffix(candTokens[i]);
							indScore = altToken[i].equals(tt) ? 1 : 0;
							break;
						case Normalize.ABREV_STATE :
							// falls down, treated the same as direction
							tt = (String)Normalize.translateState(candTokens[i]);
							indScore = altToken[i].equals(tt) ? 1 : 0;
							break;
						case Normalize.ABREV_IDENT :
							// falls down, treated the same as direction
							tt = (String)Normalize.translateIdentifier(candTokens[i]);
							indScore = altToken[i].equals(tt) ? 1 : 0;
							break;
						case Normalize.ABREV_DIR :
							tt = (String)Normalize.translateDirection(candTokens[i]);
							indScore = altToken[i].equals(tt) ? 1 : 0;
							break;
						case Normalize.ABREV_NUMBER :
							// try to get a number out of candidate. if also number, look for identical
							// else go for string matching
							try {
								String cleaned = Integer.toString(Integer.parseInt(candTokens[i]));
								if (cleaned.equals(altToken[i])) {
									indScore = 1;
								} else if ("0".equals(cleaned) || "0".equals(altToken[i])) {
									indScore = missingValue[i];
								} else {
									if(cleaned.equals(altToken[i].replaceAll("^(\\d+)\\s*(TH|RD|ST|ND)\\s*$", "$1"))) {
										indScore = 1;
									} else {
										indScore = 0;
									}
								}
								break;
							} catch (NumberFormatException e) {
								// fall in the normal string matching case
							}
						case Normalize.ABREV_NONE :
							if(i == 6){
								indScore = GenericLegal.computeScoreInternal("", candTokens[i], referenceTokens[i], true, false);				
							} else {
								indScore = BasicFilter.score(candTokens[i], referenceTokens[i]);
							}
							// when comparing street names, check if street name + suffix from candidate is matching the street name from referance or viceversa 
							// with a bigger score than comparing only the street names  (fix for bug #1324)
							if (i == 2){
								double indScoreNameSuffix1 = 0.0;
								double indScoreNameSuffix2 = 0.0;
								if (referenceTokens[3] != null){
									indScoreNameSuffix1 = BasicFilter.score(candTokens[2], referenceTokens[2] + " " + referenceTokens[3]);
								}
								if (candTokens[3] != null){
									indScoreNameSuffix2 = BasicFilter.score(candTokens[2] + " " + candTokens[3], referenceTokens[2]);
								}
								if (indScore < indScoreNameSuffix1)
									indScore = indScoreNameSuffix1;
								if (indScore < indScoreNameSuffix2)
									indScore = indScoreNameSuffix2;
							}
							break;
					}
				}
			}/* else {
				if (i == 6 && candTokens[i] == null && referenceTokens[i] != null){
					indScore = 1.00;
				}
			}*/
			score += (indScore * tokenWeight[i]);
			scores[i] = indScore;
		}
		scores[7] = (score/common);

		/*
		 * if street name score is greater than overall score, then make the overall score to be street name score
		 */
		/*
		if((candTokens[2]!= null) && (referenceTokens[2]!= null)){
			double nameWeight =  (tokenWeight[2] * commonTokens) / common;
			double newScore = scores[2] / nameWeight;
			if(scores[7] > newScore){
 				scores[7] = newScore;
			}
		}
		*/
		
		//addressLogger.logString("Comparing ref |" + referenceAddress + "| vs |" + candidate +"|=" + scores[7]);				
		return scores;
	}
	
	public static void main(String[] args) {
		AddressMatcher addressMatcher = new AddressMatcher(new StandardAddress("65 BEACH"));
		
		System.out.println(addressMatcher.matches(new StandardAddress("65 BEACH LN #BB")));
	}
}
