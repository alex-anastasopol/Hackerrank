package ro.cst.tsearch.search.filter.newfilters.name;

import java.util.HashMap;
import java.util.Map.Entry;

import ro.cst.tsearch.servers.info.TSServerInfoModule;

public class SynonimNameFilter extends GenericNameFilter {

	private static final HashMap<String, String> synonimsSingleWord = new HashMap<String, String>();
	private static final HashMap<String, String> synonimsMultipleWord = new HashMap<String, String>();
	static {
		
		synonimsSingleWord.put("ALEXR", "ALEXANDER");
		synonimsSingleWord.put("ALEX", "ALEXANDER");
		synonimsSingleWord.put("BENJ", "BENJAMIN");
		synonimsSingleWord.put("BERND", "BERNARD");
		synonimsSingleWord.put("CHAS", "CHARLES");
		synonimsSingleWord.put("DANL", "DANIEL");
		synonimsSingleWord.put("DY", "DOROTHY");
		synonimsSingleWord.put("ELIZ", "ELIZBETH");
		synonimsSingleWord.put("EDW", "EDWARD");
		synonimsSingleWord.put("FREDK", "FREDERICK");
		synonimsSingleWord.put("GEO", "GEORGE");
		synonimsSingleWord.put("JON", "JONATHAN");
		synonimsSingleWord.put("JOE", "JOSEPH");
		synonimsSingleWord.put("JOSH", "JOSHUA");
		synonimsSingleWord.put("MATTW", "MATTHEW");
		synonimsSingleWord.put("RICH", "RICHARD");
		synonimsSingleWord.put("RICHD", "RICHARD");
		synonimsSingleWord.put("ROBT", "ROBERT");
		synonimsSingleWord.put("SAM", "SAMANTHA");
		synonimsSingleWord.put("SAML", "SAMUEL");
		synonimsSingleWord.put("THEO", "THEODORE");
		synonimsSingleWord.put("THOS", "THOMAS");
		synonimsSingleWord.put("TOM", "THOMAS");
		synonimsSingleWord.put("XIAN", "CHRISTIAN");
		synonimsSingleWord.put("XPR", "CHRISTOPHER");
		synonimsSingleWord.put("WM", "WILLIAM");
		
		synonimsMultipleWord.put("LA VONNE", "LAVONNE");
		synonimsMultipleWord.put("LA V", "LAVONNE");
		synonimsMultipleWord.put("LIV TR", "LIVING TRUST");
		synonimsMultipleWord.put("REV TR", "REVOCABLE TRUST");
		
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SynonimNameFilter(String key, long searchId,
			boolean useSubdivisionName, TSServerInfoModule module,
			boolean ignoreSuffix, int stringCleaner) {
		super(key, searchId, useSubdivisionName, module, ignoreSuffix, stringCleaner);
	}
	
	public SynonimNameFilter(String key, long searchId,
			boolean useSubdivisionName, TSServerInfoModule module,
			boolean ignoreSuffix) {
		super(key, searchId, useSubdivisionName, module, ignoreSuffix);
	}
	
	public SynonimNameFilter(long searchId){
		super(searchId);
	}
	
	public SynonimNameFilter(String key, long searchId) {
		super(key, searchId);
	}

	

	/* (non-Javadoc)
	 * @see ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter#calculateScore(java.lang.String[], java.lang.String[], boolean)
	 */
	@Override
	protected double calculateScore(String[] ref, String[] cand,
			boolean generateAranjaments) {
		toUpperCase(ref);
		for (int i = 0; i < ref.length; i++) {
			if(ref[i] != null) {
				String[] littleTokens = ref[i].split("[ ]+");
				String newRef = "";
				for (String littleToken : littleTokens) {
					if(synonimsSingleWord.containsKey(littleToken)){
						newRef += synonimsSingleWord.get(littleToken) + " "; 
					} else {
						newRef += littleToken + " ";
					}
				}
				ref[i] = newRef;
				for (Entry<String, String> entry : synonimsMultipleWord.entrySet()) {
					if(ref[i].matches("(?is)(^|.*\\s)(" + entry.getKey() + ")($|\\s+.*)")) {
						ref[i] = ref[i].replaceAll("(?is)(^|.*\\s)(" + entry.getKey() + ")($|\\s+.*)", 
								"$1" + entry.getValue() + "$3");
					}
				}
				ref[i] = ref[i].trim();
			}
		}
		
		toUpperCase(cand);
		for (int i = 0; i < cand.length; i++) {
			if(cand[i] != null) {
				String[] littleTokens = cand[i].split("[ ]+");
				String newCand = "";
				for (String littleToken : littleTokens) {
					if(synonimsSingleWord.containsKey(littleToken)){
						newCand += synonimsSingleWord.get(littleToken) + " "; 
					} else {
						newCand += littleToken + " ";
					}
				}
				cand[i] = newCand;
				for (Entry<String, String> entry : synonimsMultipleWord.entrySet()) {
					if(cand[i].matches("(?is)(^|.*\\s)(" + entry.getKey() + ")($|\\s+.*)")) {
						cand[i] = cand[i].replaceAll("(?is)(^|.*\\s)(" + entry.getKey() + ")($|\\s+.*)", 
								"$1" + entry.getValue() + "$3");
					}
				}
				cand[i] = cand[i].trim();
			}
		}
			
		
		
		return super.calculateScore(ref, cand, generateAranjaments);
	}
	
	public static HashMap<String, String> getAllSynonims () {
		HashMap<String, String> synonims = new HashMap<String, String>();
		synonims.putAll(synonimsSingleWord);
		synonims.putAll(synonimsMultipleWord);
		return synonims;
	}
	

}
