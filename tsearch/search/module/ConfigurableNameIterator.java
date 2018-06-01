/**
 * 
 */
package ro.cst.tsearch.search.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.filter.newfilters.name.SynonimNameFilter;
import ro.cst.tsearch.search.name.CompanyNameExceptions;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.SynonymManager;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.MostCommonName;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.Party;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
 * @author radu bacrau
 * 
 */ 
public class ConfigurableNameIterator extends NameModuleStatesIterator {
	
	public static enum SEARCH_WITH_TYPE {
		PERSON_NAME,
		FEMALE_NAME,
		MALE_NAME,
		COMPANY_NAME,
		BOTH_NAMES
	}

	public static final long serialVersionUID = 1000L;
	private long searchId = -1;
	private static Hashtable<String, String> companyNameDerivationStrings = new Hashtable<String, String>();
	private static Hashtable<String, String> companyNameReplacements = new Hashtable<String, String>();
	private static List<String> scottishNames = new ArrayList<String>();
	
	private boolean treatAsCorporate=false;
	private boolean doScottishNamesDerivations = false;
	private boolean derivateWithSynonims = false;
	private boolean nameDerrivation = true;
	
	private SEARCH_WITH_TYPE searchWithType = SEARCH_WITH_TYPE.BOTH_NAMES;
	
	static {
		companyNameDerivationStrings.put("DEV", "DEVELOPMENT");
		companyNameDerivationStrings.put("BCH", "BEACH"); 
		companyNameDerivationStrings.put("TR", "TRUST");
		
		companyNameReplacements.put("\\-"," ");
		
		scottishNames.add("O'Donnell");
		scottishNames.add("O'Callaghan");
		scottishNames.add("O'Sullivan");
		scottishNames.add("O'Connor");
		scottishNames.add("O'Neill");
		scottishNames.add("O'Neil");
		scottishNames.add("O'Reilly");
		scottishNames.add("O'Doherty");
		scottishNames.add("O'Carroll");
		scottishNames.add("O'Connell");
		scottishNames.add("O'Donnell");
		scottishNames.add("O'Brien");
		scottishNames.add("O'Flynn");
		scottishNames.add("O'Donovan");
		scottishNames.add("O'Hara");
		scottishNames.add("O'Toole");
		scottishNames.add("McDonald");
		scottishNames.add("McArdle");
		scottishNames.add("McCardle");
		scottishNames.add("McCree");
		scottishNames.add("McLeod");
		scottishNames.add("McCleod");
		scottishNames.add("McDowell");
		scottishNames.add("McDonnough");
		scottishNames.add("McDuff");
		scottishNames.add("McKensie");
		scottishNames.add("McNamara");
		scottishNames.add("McCurdy");
		scottishNames.add("McNulty");
		scottishNames.add("McPherson");
		scottishNames.add("McQueen");
		scottishNames.add("McIntosh");
		scottishNames.add("McGuire");
		scottishNames.add("McGrath");
		scottishNames.add("McGraw");
		scottishNames.add("McCoy");
		scottishNames.add("McGregor");
	}
	
	/**
	 * @param searchId
	 * @param derrivPatterns
	 */
	public ConfigurableNameIterator(long searchId, String[] derrivPatterns) {
		super(searchId);
		this.searchId = searchId;
		this.derrivPatterns = derrivPatterns;
	}
	public ConfigurableNameIterator(long searchId, String[] derrivPatterns,boolean treatAsCorporate) {
		super(searchId);
		this.searchId = searchId;
		this.derrivPatterns = derrivPatterns;
		this.treatAsCorporate=treatAsCorporate;
	}
	/**
	 * @param searchId
	 * @param derrivPatterns
	 * @param useNickNames - using nickname for search
	 */
	public ConfigurableNameIterator(long searchId, boolean useNickNames, String[] derrivPatterns) {
		super(searchId);
		this.searchId = searchId;
		this.derrivPatterns = derrivPatterns;
		this.useNickNames = useNickNames;
	}
	/**
	 * @param searchId
	 * @param derrivPatterns
	 * @param lastNameLength - for split long company names
	 */
	public ConfigurableNameIterator(long searchId, String[] derrivPatterns, int lastNameLength) {
		super(searchId);
		this.searchId = searchId;
		this.derrivPatterns = derrivPatterns;
		this.lastNameLength = lastNameLength;
	}
	/**
	 * @param searchId
	 */
	public ConfigurableNameIterator(long searchId) {
		super(searchId);
		this.searchId = searchId;
	}
	
	/** Do not allow MCN to be derived */
	private boolean allowMcnPersons = false;
	
	/** Do not allow search by MCN Companies */
	private boolean allowMcnCompanies = false;
	
	/** Do not allow nicknames by default */
	private boolean useNickNames = false;
	
	/** Last names length */
	private int lastNameLength = 0;
	
	/** Default derive pattern is identity derivation */
	private String [] derrivPatterns = new String[]{"L;F;M"};
	
	/** Default name separator is ; */
	private String nameSeparator = ";";

	/** Do not derive company names by default */
	private boolean derrivateCompanies = false;
	
	/** skip derivation that needs F/M but they are only one letter **/
	private boolean skipInitial = false;
	
	/** Ignore person names whose last name is most common */
	private boolean ignoreMCLast = false;
	
	/** Ignore company names */
	private boolean ignoreCompanies = false;
	
	private transient ArrayList<NameI> searchedNames = new  ArrayList<NameI> ();
	
	/**
	 * If set to <code>true</code> will create a derivation that has on first name an element from companyForceFirstNameList<br>
	 * Can be used for example with "%" character 
	 */
	private boolean enableCompanyForceFirstName = false;
	private List<String> companyForceFirstNameList = null;
	
	
	/**
	 * Split the three dispatching patterns 
	 * @param config three dispatching patterns separated by <code>separator</code>
	 * @param separator separator between the three patterns
	 * @return array of 3 patterns
	 * @throws IllegalArgumentException in case there are not exactly 2 occurrences of <code>separator</code>
	 */
	private String [] splitPatterns(String config, String separator){
		
		int pos1 = config.indexOf(separator); 
		int pos2 = config.indexOf(separator, pos1 + separator.length());
		int pos3 = config.indexOf(separator, pos2 + separator.length());
		
		if(pos1 == -1 || pos2 == -1 || pos3 != -1){
			throw new IllegalArgumentException("Invalid config:{" + config + "} separator: {" + separator + "}");
		}
		
		String lPat = config.substring(0, pos1);
		String fPat = config.substring(pos1 + separator.length(), pos2);
		String mPat = "";
		if(pos2 + separator.length() < config.length()){
			mPat = config.substring(pos2 + separator.length());
		}
		
		return new String[]{lPat, fPat, mPat};
		
	}
	
	/**
	 * Build a single derivation
	 * @param seed initial name
	 * @param config 3 patterns, corresponding to L, F, M derivations
	 * @return derivation
	 * @throws IllegalArgumentException in case <code>config</code> does not have exactly 3 elements
	 */
	private Collection<NameI> buildDerrivation(NameI seed, NameI origName, String config){
		
		String [] patterns = splitPatterns(config, nameSeparator); 
		
		if(patterns.length != 3){
			throw new IllegalArgumentException("patterns length should be 3!");
		}
		
		boolean nL = config.contains("L") || config.contains("l");
		boolean nF = config.contains("F") || config.contains("f");
		boolean nM = config.contains("M") || config.contains("m");
		
		String last = seed.getLastName().toUpperCase();
		String first = seed.getFirstName().toUpperCase();
		String middle = seed.getMiddleName().toUpperCase();
		String suffix = seed.getSufix();
		
		String l = (last.length() > 0) ? last.substring(0,1) : "";
		String f = (first.length() > 0) ? first.substring(0,1) : "";
		String m = (middle.length() > 0) ? middle.substring(0,1) : "";
		
		// synonyms become Non MCN Persons and then will search with initial of the synonym that are not MCN
		boolean origFirstNameIsMCN = false;
		boolean origMiddleNameIsMCN = false;
		boolean origLastNameIsMCN = false;
		if (origName != null){
			String firstName = origName.getFirstName().toUpperCase();
			String middleName = origName.getMiddleName().toUpperCase();
			String lastName = origName.getLastName().toUpperCase();
			if(MostCommonName.isMCFirstName(firstName)){
				origFirstNameIsMCN = true;
			}
			if(MostCommonName.isMCFirstName(middleName)){
				origMiddleNameIsMCN = true;
			}
			if(MostCommonName.isMCLastName(lastName)){
				origLastNameIsMCN = true;
			}
		}
		
		
		// if MCNs not allowed, then replace initials with full
		boolean abort = false;
		if(!allowMcnPersons){
			if(MostCommonName.isMCLastName(last) || origLastNameIsMCN){
				l = last;
				f = first;
				m = middle;
				//B3467 - I am not allowed to search with Last first/middle initial if last is MCN
				char[] c = config.toUpperCase().replaceAll(";", "").replaceAll(" ", "").toCharArray();
				Arrays.sort(c);
				String cleanConfig = new String(c);
				if ((cleanConfig.equals("LM") && middle.length() < 2)
					|| (cleanConfig.equals("FL") && first.length() <2)
					|| (cleanConfig.equals("FLM") && (first+middle).length() <2)){
						abort = true;
				}
			}
			if(MostCommonName.isMCFirstName(first) || origFirstNameIsMCN){
				l = last;
				f = first;
			}
			if(MostCommonName.isMCFirstName(middle) || origMiddleNameIsMCN){
				l = last;
				m = middle;
			}
		}
		
		// create return list
		List<NameI> derrivations = new LinkedList<NameI>();
		
		// abort if we need middle and we do not have it
		abort = abort ||
			(nL && StringUtils.isEmpty(last)) ||
			(nF && StringUtils.isEmpty(first)  && (!nM || StringUtils.isEmpty(middle))) ||
		    (nM && StringUtils.isEmpty(middle) && (!nF || StringUtils.isEmpty(first)));
		if(StringUtils.isEmpty(first) && StringUtils.isEmpty(middle) && !StringUtils.isEmpty(last) && nL){
			abort = abort || false;
		} 
		
		// abort if we need a name but we only have an initial		
		if(skipInitial){
			if(config.contains("M") && middle.length() < 2){ 
				abort = true; 
			} else if(config.contains("F") && first.length() < 2){ 
				abort = true; 
			} else if(config.contains("L") && last.length() < 2){ 
				abort = true; 
			}
		}
		
		if(abort){  
			return derrivations; 
		}
		
		String [] deriv = new String [3];
		for(int i=0; i<3; i++){
			String crt = "";
			for(int k=0; k<patterns[i].length(); k++){
				char c = patterns[i].charAt(k);
				switch(c){
				case 'L': crt += last; break;
				case 'l': crt += l; break;
				case 'F': crt += first; break;
				case 'f': crt += f; break;
				case 'M': crt += middle; break;
				case 'm': crt += m; break;
				case 'S': 
				case 's': crt += suffix; break;
				default: crt += c;						
				}
			}
			// cleanup
			if (crt.endsWith(",")) {
				crt = crt.substring(0, crt.lastIndexOf(","));
			}
			crt = crt.replaceAll("\\s{2,}", " ").trim();
			deriv[i] = crt;
		}

		String dL = deriv[0];
		String dF = deriv[1];
		String dM = deriv[2];
		
		NameI current =  new Name(dF, dM, dL) ;
		current.setSsn4Encoded(seed.getSsn4Encoded());
		current.getNameFlags().setSourceTypes(seed.getNameFlags().getSourceTypes());
		// add the main derivation
		if( searchedNames == null)
			searchedNames = new ArrayList<NameI>();
		if( !searchedNames.contains( current ) ){
			derrivations.add( current );
			searchedNames.add( current ) ;
		}
		
		// add nicknames
		if(useNickNames && StringUtils.isNotEmpty(dF)){
			
			try {
				Set<String> firstNicks = SynonymManager.getInstance().getSynonymsFor(dF);
				
				if(firstNicks != null && firstNicks.size() != 0){
					for(String firstNick: firstNicks){
						current = new Name(firstNick, dM, dL) ;
						current.setSsn4Encoded(seed.getSsn4Encoded());
						current.getNameFlags().setSourceTypes(seed.getNameFlags().getSourceTypes());
						if( !searchedNames.contains( current ) ){
							derrivations.add( current ) ;
							searchedNames.add( current ) ;
						}
					}
				}
			} catch (Exception e) {
				logger.error("Crapelnitare when adding name derivation ", e);
			}
			/*
			Collection<String> middleNicks = NameAliases.getAliases(dM);
			if(middleNicks.size() != 0){
				for(String middleNick: middleNicks){
					current = new Name(dF, middleNick, dL) ;
					current.setSsn4Encoded(seed.getSsn4Encoded());
					if( !searchedNames.contains( current ) ){
						derrivations.add( current );
						searchedNames.add( current ) ;
					}
				}
			}*/			
		}
		
		// manage composed last names (with format last1-last2) --> Task 7026
		if(nameDerrivation) {
			List<NameI> _derrivations = new LinkedList<NameI>(derrivations);
			for(NameI name : _derrivations) {
				String lastName = name.getLastName();
				String firstName = name.getFirstName();
				String middleName = name.getMiddleName();
				
				String[] parts = lastName.split("\\s*-\\s*");
				if(parts.length == 2) {
					if(!StringUtils.isEmpty(parts[0]) && !StringUtils.isEmpty(parts[1])) {
						current = new Name(firstName, middleName, parts[0] + parts[1]) ;
						current.setSsn4Encoded(seed.getSsn4Encoded());
						current.getNameFlags().setSourceTypes(seed.getNameFlags().getSourceTypes());
						if( !searchedNames.contains( current ) ){
							derrivations.add( current ) ;
							searchedNames.add( current ) ;
						}
						
						for(String part : parts) {
							current = new Name(firstName, middleName, part) ;
							current.setSsn4Encoded(seed.getSsn4Encoded());
							current.getNameFlags().setSourceTypes(seed.getNameFlags().getSourceTypes());
							if( !searchedNames.contains( current ) ){
								derrivations.add( current ) ;
								searchedNames.add( current ) ;
							}
						}
					}
				}
			}
		}
		
		return derrivations;
	}
	
	/**
	 * Build all needed derivations
	 * @param initialNames names to be derived
	 * @param configs list of derivations to be used (named configurations)
	 * @param separator separates between L,F,M inside a configuration
	 * @return derivations
	 */
	private Set<NameI> buildDerrivations(Collection<NameI> initialNames){

		Set<NameI> derrivations = new LinkedHashSet<NameI>();
		Collection<NameI> derivedNames = new ArrayList<NameI>();
		if(getSearchWithType().equals(SEARCH_WITH_TYPE.BOTH_NAMES)) {
			derivedNames.addAll(initialNames);
		} else {
			for (NameI nameI : initialNames) {
				if(nameI.isCompany() && getSearchWithType().equals(SEARCH_WITH_TYPE.COMPANY_NAME) ) {
					derivedNames.add(nameI);
				}
				if(!nameI.isCompany() && getSearchWithType().equals(SEARCH_WITH_TYPE.PERSON_NAME) ) {
					derivedNames.add(nameI);
					
				}
				if(!nameI.isCompany() && getSearchWithType().equals(SEARCH_WITH_TYPE.FEMALE_NAME) ) {
					if (FirstNameUtils.isFemaleName(nameI.getFirstName()) && !FirstNameUtils.isMaleName(nameI.getFirstName())) {
						derivedNames.add(nameI);
					}
				}
				if(!nameI.isCompany() && getSearchWithType().equals(SEARCH_WITH_TYPE.MALE_NAME) ) {
					if (!(FirstNameUtils.isFemaleName(nameI.getFirstName()) && !FirstNameUtils.isMaleName(nameI.getFirstName()))) {
						derivedNames.add(nameI);
					}
				}
			}
		}
		
		
		if(doScottishNamesDerivations)  {
			for(NameI seed: initialNames){
				if(getSearchWithType().equals(SEARCH_WITH_TYPE.BOTH_NAMES) || 
						(seed.isCompany() && getSearchWithType().equals(SEARCH_WITH_TYPE.COMPANY_NAME) ) ||
						(!seed.isCompany() && getSearchWithType().equals(SEARCH_WITH_TYPE.PERSON_NAME)) ) {
					derivedNames.addAll(buildScottishNameDerivations(seed));
					if(isDerivateWithSynonims()) {
						for (NameI synonim : getSynonimsFor(seed)) {
							derivedNames.addAll(buildScottishNameDerivations(synonim));
						}
					}
				}
			}
		}
		for(NameI seed: derivedNames){
			for(String config: derrivPatterns){								
				derrivations.addAll(buildDerrivation(seed, null, config));
				if(isDerivateWithSynonims()) {
					for (NameI synonim : getSynonimsFor(seed)) {
						derrivations.addAll(buildDerrivation(synonim, seed, config));
					}
					
				}
			}
		}
		
		return derrivations;		
	}
	
	private Collection<NameI> getSynonimsFor(NameI name){
		String firstName = name.getFirstName().toUpperCase();
		HashMap<String, String> synonims = SynonimNameFilter.getAllSynonims();
		Collection<NameI> result = new Vector<NameI>();
		if(synonims.containsKey(firstName)) {
			NameI clone = name.clone();
			clone.setFirstName(synonims.get(firstName));
			result.add(clone);
		} else if(synonims.containsValue(firstName)) {
			for (Entry<String, String> entry : synonims.entrySet()) {
				if(firstName.equals(entry.getValue())){
					NameI clone = name.clone();
					clone.setFirstName(entry.getKey());
					result.add(clone);
				}
			}
		}
		return result;
	}
	
	/**
	 * Build scottish name derivations - Bug 4193
	 * @param seed
	 * @return
	 */
	private Collection<NameI> buildScottishNameDerivations(NameI seed) {
		ArrayList<NameI> list = new ArrayList<NameI>();
		for(String name : scottishNames) {
			String last = seed.getLastName().replaceAll("'|\\s","").toLowerCase();
			if(last.startsWith(name.replaceAll("'|\\s","").toLowerCase())) {
				Name deriv1 = new Name(seed);
				deriv1.setLastName(name);
				Name deriv2 = new Name(seed);
				deriv2.setLastName(name.replaceAll("'",""));
				Name deriv3 = new Name(seed);
				deriv3.setLastName(name.replaceAll("'"," "));
				Name deriv4 = new Name(seed);
				deriv4.setLastName(name.replaceAll("(?i)Mc","Mc "));
				Name deriv5 = new Name(seed);
				deriv5.setLastName(name.replaceAll("(?i)Mc ","Mc"));
				list.add(deriv1);
				list.add(deriv2);
				list.add(deriv3);
				list.add(deriv4);
				list.add(deriv5);
				break;
			}
		}
		return list;
	}

	public void init(TSServerInfoModule initial) {
		
		initFilter(initial);
		initInitialState(initial);
		
		Set<NameI> derivNames = new LinkedHashSet<NameI>();
		Set<NameI> setRef	= new HashSet<NameI>();
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance( searchId ).getCrtSearchContext().getSa();
				
		DocumentsManagerI documentsManager=InstanceManager.getManager().getCurrentInstance(
				searchId).getCrtSearchContext().getDocManager();
		
		boolean copyNameFlags = false;
		
		
		// create set of initial names to be taken into consideration
		Set<NameI> initialNames = new LinkedHashSet<NameI>();
		String saKey=initial.getSaObjKey(); 
		
		if ( saKey.equals(SearchAttributes.BUYER_OBJECT) ){
			setRef=sa.getBuyers().getNames();
			copyNameFlags = sa.isUpdate();
		} 
		else if( saKey.equals(SearchAttributes.GB_MANAGER_OBJECT) ){
		    try{
		    	GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);
		    	documentsManager.getAccess();
				if("grantor".equals(initial.getTypeSearchGB())){
				    setRef.clear();;
				    setRef.addAll(gbm.getNamesForSearch(initial.getIndexInGB(), searchId));
				    if(setRef==null||setRef.size()==0)
				    {
				    	gbm.setErr(initial.getIndexInGB(),GBManager.ERR_INVALID_SEARCH_NAME);				      
				    }
				} else{
					setRef.clear();
					setRef.addAll(gbm.getNamesForBrokenChain(initial.getIndexInGB(), searchId));
				}
		    } finally{
		    	documentsManager.releaseAccess();
		    }
		}
		else if( saKey.equals(SearchAttributes.OWNER_OBJECT) ){
			setRef = sa.getOwners().getNames();
			copyNameFlags = sa.isUpdate();
			//if(sa.isUpdate()) {
				try {
					DataSite dataSite = HashCountyToIndex.getCrtServer(searchId, false);
					long miServerId = dataSite.getServerId();
					setRef.addAll(sa.getForUpdateSearchGrantorNamesNotNull(miServerId));
					setRef.addAll(sa.getForUpdateSearchGranteeNamesNotNull(miServerId));
				} catch (Exception e) {
					logger.error("Error loading names for Update saved from Parent Site", e);
				}
		} else if (saKey.equals(AdditionalInfoKeys.ADDITIONAL_NAMES_LIST)) {
			Object additionalInfo = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getAdditionalInfo(AdditionalInfoKeys.ADDITIONAL_NAMES_LIST);
			
			if(additionalInfo != null && additionalInfo instanceof List) {
				setRef = new LinkedHashSet((List<NameI>) additionalInfo);
			}
		}

		for (NameI element : setRef) {
			if (element.getLastName().length()<=1) {
				sa.setAtribute(SearchAttributes.POOR_SEARCH_DATA,element.getLastName()+" ");
			} else {
				//B6140
				element.setMiddleName(element.getMiddleName().replaceAll("[\\.,]+", ""));
				
				try {
					if(element.isCompany()) {
						// ignore companies, if the flag is set
						if(ignoreCompanies) {
							continue;
						}
						
						for(String deriv : companyNameDerivationStrings.keySet()) {
							if( element.getLastName().matches("(?is)(^|.*\\s)(" + deriv + ")($|\\s+.*)")  ) {
								NameI tempName = new Name(element);
								String newLastName = tempName.getLastName().replaceAll("(?is)(^|.*\\s)(" + deriv + ")($|\\s+.*)", 
										"$1" + companyNameDerivationStrings.get(deriv) + "$3");
								tempName.setLastName(newLastName);
								if( !searchedNames.contains( tempName ) ){
									if(allowMcnCompanies || CompanyNameExceptions.allowed(tempName.getLastName(), searchId)){//without this condition will generate B4371
										
										for(String derivRepl : companyNameReplacements.keySet()) {	
											NameI tempNameRepl = new Name(tempName);
											String newLastNameRepl = tempNameRepl.getLastName().replaceAll(derivRepl, Matcher.quoteReplacement(companyNameReplacements.get(derivRepl)));
											tempNameRepl.setLastName(newLastNameRepl);
											if( !searchedNames.contains( tempNameRepl ) ){
												derivNames.add( tempNameRepl );
												searchedNames.add( tempNameRepl ) ;
											}
										}
										if( !searchedNames.contains( tempName ) ){
											derivNames.add( tempName );
											searchedNames.add( tempName ) ;
										}
										
									}
								}
							}
						}
						if(allowMcnCompanies || CompanyNameExceptions.allowed(element.getLastName(), searchId)){//without this condition will generate B4371
							for(String deriv : companyNameReplacements.keySet()) {	
								NameI tempName = new Name(element);
								String newLastName = tempName.getLastName().replaceAll(deriv, Matcher.quoteReplacement(companyNameReplacements.get(deriv)));
								tempName.setLastName(newLastName);
								if( !searchedNames.contains( tempName ) ){
									derivNames.add( tempName );
									searchedNames.add( tempName ) ;
								}
							}
						}
					}
				} catch(Exception caught) { 
					caught.printStackTrace(); 
				}
				
				// ignore names whose last name is most common, if the flag is set
				if(ignoreMCLast) {
					String lastName = element.getLastName();
					if(MostCommonName.isMCLastName(lastName)) {
						continue;
					}
				}
				
				//must eliminate variable treatasorporate soon as possibile.used it for michigan .but added the functionality	on iterator part that does it more better.
				if (element.isCompany() || this.treatAsCorporate) {
					if (this.treatAsCorporate) {
						if (allowMcnCompanies || CompanyNameExceptions.allowed(element.getFullName(), searchId)) {
							NameI current =  new Name("", "", element.getFullName());
							current.setCompany(true);
							if(copyNameFlags) {
								current.getNameFlags().setSourceTypes(element.getNameFlags().getSourceTypes());
							}
							// add the main derivation
							if( searchedNames == null)
								searchedNames = new ArrayList<NameI>();
							if( !searchedNames.contains( current ) ){
								derivNames.add( current );
								searchedNames.add( current ) ;
							}
							if(derrivateCompanies){
								List<String> companyNames = CompanyNameDerrivator.buldCompanyNameList(element.getFullName(), searchId);
								for(String company: companyNames){
									current = new Name("", "", company);
									if(copyNameFlags) {
										current.getNameFlags().setSourceTypes(element.getNameFlags().getSourceTypes());
									}
									if( !searchedNames.contains( current ) ){
										derivNames.add( current );
										searchedNames.add( current ) ;
									}
								}
							}
						}	
					} else {
						if(allowMcnCompanies || CompanyNameExceptions.allowed(element.getLastName(), searchId)){
							NameI current =  new Name("", "", element.getLastName());
							current.setCompany(true);
							if(copyNameFlags) {
								current.getNameFlags().setSourceTypes(element.getNameFlags().getSourceTypes());
							}
							// add the main derivation
							if( searchedNames == null)
								searchedNames = new ArrayList<NameI>();
							if( !searchedNames.contains( current ) ){
								derivNames.add( current );
								searchedNames.add( current ) ;
							}
							if (derrivateCompanies) {
								List<String> companyNames = CompanyNameDerrivator.buldCompanyNameList(element.getLastName(), searchId);
								for (String company : companyNames) {
									current = new Name("", "", company);
									if (copyNameFlags) {
										current.getNameFlags().setSourceTypes(element.getNameFlags().getSourceTypes());
									}
									current.setCompany(true);
									if (!searchedNames.contains(current)) {
										derivNames.add(current);
										searchedNames.add(current);
									}
								}
							}
						}
					}
				} else {
					// check that name derivations are enabled
					boolean isCaseNet = false;
					try{
						DataSite server = HashCountyToIndex.getCrtServer(searchId, false);
						nameDerrivation = server.isEnabledNameDerivation(sa.getCommId());
						isCaseNet = GWTDataSite.CO_TYPE == server.getSiteTypeInt() && 
								(server.getCountyId() == CountyConstants.MO_Jackson 
								|| server.getCountyId() == CountyConstants.MO_Clay);  
					}catch(Exception e){
						//e.printStackTrace();
					}
					
					if(isCaseNet){
						if(nameDerrivation){
							derrivPatterns = new String[]{"L;f;", "L;m;"};
						} else {
							derrivPatterns = new String[]{"L;F;", "L;M;"};
							skipInitial = true;
						}
					} else 	if(!nameDerrivation){
						derrivPatterns = new String[]{"L;F;M"};
					}			 	
					initialNames.add(new Name(element));
						
							
					// add derrivations
					derivNames.addAll(buildDerrivations(initialNames));
				}
			}
			
		}	
			
		/**
		 * This area forces derivations for companies to use a default first name
		 * For example this is used to add "%" to first name when searching with company name at last name
		 * First needed on Nevada TS
		 */
		if(isEnableCompanyForceFirstName() && 
				(getCompanyForceFirstNameList() != null || !getCompanyForceFirstNameList().isEmpty())) {
			Set<NameI> temporaryNames = new LinkedHashSet<NameI>();
			for (NameI nameI : derivNames) {
				temporaryNames.add(nameI);
				if(nameI.isCompany()) {
					for (String forcedFirstName : getCompanyForceFirstNameList()) {
						NameI nameClone = nameI.clone();
						nameClone.setFirstName(forcedFirstName);
						if (!searchedNames.contains(nameClone)) {
							temporaryNames.add(nameClone);
							searchedNames.add(nameClone);
						}
					}
				}
			}
			derivNames = temporaryNames;
			
		}
		
		if(lastNameLength > 0) {
			for (NameI element : derivNames) {
				if(element.isCompany()) {
					//if last name is longer than X characters, then split in last = X characters and first = size - X. B 4382
					if (element.getLastName().length() > lastNameLength){
						String temp = element.getLastName().substring(0, lastNameLength);
						element.setFirstName(element.getLastName().substring(lastNameLength, element.getLastName().length()));
						element.setLastName(temp);
					}
				}
			}
		}
		
		if (numberOfYearsAllowed > 1){
			CombinedNameIterator si = new CombinedNameIterator(derivNames, numberOfYearsAllowed);
			si.init();
			setStrategy(si);
			
		} else if(getSearchWithType().equals(SEARCH_WITH_TYPE.BOTH_NAMES)) {
			StatesIterator si = new DefaultStatesIterator(derivNames);
			setStrategy(si);
		} else {
			
			Set<NameI> temporaryNames = new LinkedHashSet<NameI>();
			
			for (NameI nameI : derivNames) {
				if(nameI.isCompany() && getSearchWithType().equals(SEARCH_WITH_TYPE.COMPANY_NAME) ) {
					temporaryNames.add(nameI);
				}
				if(!nameI.isCompany() && getSearchWithType().equals(SEARCH_WITH_TYPE.PERSON_NAME) /*|| 
						getSearchWithType().equals(SEARCH_WITH_TYPE.FEMALE_NAME) ||
						getSearchWithType().equals(SEARCH_WITH_TYPE.MALE_NAME)*/) {
					temporaryNames.add(nameI);
				}
				if(!nameI.isCompany() && getSearchWithType().equals(SEARCH_WITH_TYPE.FEMALE_NAME)) {
					if (FirstNameUtils.isFemaleName(nameI.getFirstName()) && !FirstNameUtils.isMaleName(nameI.getFirstName())) {
						temporaryNames.add(nameI);
					}
				}
				if(!nameI.isCompany() && getSearchWithType().equals(SEARCH_WITH_TYPE.MALE_NAME)) {
					if (!(FirstNameUtils.isFemaleName(nameI.getFirstName()) && !FirstNameUtils.isMaleName(nameI.getFirstName()))) {
						temporaryNames.add(nameI);
					}
				}
			}
			
			
			StatesIterator si = new DefaultStatesIterator(temporaryNames);
			setStrategy(si);
			
		}
	}
	
	/**
	 * Instruct to allow MCN derivations for person names  or not
	 * @param allowMcnPersons
	 */
	public void setAllowMcnPersons(boolean allowMcnPersons) {
		this.allowMcnPersons = allowMcnPersons;
	}

	/**
	 * Instruct to split last name longer than X characters
	 * @param lastNameLength
	 */
	//public void setSplitLastNameLongerThan(int lastNameLength){
	//	this.lastNameLength = lastNameLength;
	//}
	
	/**
	 * Instruct to allow MCN companies derrivations  or not
	 * @param allowMcnCompanies
	 */
	public void setAllowMcnCompanies(boolean allowMcnCompanies) {
		this.allowMcnCompanies = allowMcnCompanies;
	}

	/**
	 * Set which derivations to use
	 * @param derrivationPatterns
	 */
	public void setDerivPatterns(String [] derivPatterns) {
		this.derrivPatterns = derivPatterns;
	}

	/**
	 * Instruct to use nicknames or not
	 * @param useNickNames
	 */
	public void setUseNicknames(boolean useNickNames) {
		this.useNickNames = useNickNames;
	}

	/**
	 * Set the separator between LFM used by derriv patterns
	 * @param nameSeparator
	 */
	public void setNameSeparator(String nameSeparator) {
		this.nameSeparator = nameSeparator;
	}

	/**
	 * Instruct whether to derive company names or not
	 * @param derrivateCompanies
	 */
	public void setDerrivateCompanies(boolean derrivateCompanies){
		this.derrivateCompanies = derrivateCompanies;
	}
	
	public void  clearSearchedNames(){
		searchedNames.clear() ;
	}

	public ArrayList<NameI> getSearchedNames() {
		return searchedNames;
	}

	public void setSearchedNames(ArrayList<NameI> searchedNames) {
		this.searchedNames = searchedNames;
	}

	public boolean isSkipInitial() {
		return skipInitial;
	}

	public void setSkipInitial(boolean skipInitial) {
		this.skipInitial = skipInitial;
	}
	public boolean isDoScottishNamesDerivations() {
		return doScottishNamesDerivations;
	}
	public void setDoScottishNamesDerivations(boolean doScottishNamesDerivations) {
		this.doScottishNamesDerivations = doScottishNamesDerivations;
	}
	/**
	 * @return the derivateWithSynonims
	 */
	public boolean isDerivateWithSynonims() {
		return derivateWithSynonims;
	}
	/**
	 * @param derivateWithSynonims the derivateWithSynonims to set
	 */
	public void setDerivateWithSynonims(boolean derivateWithSynonims) {
		this.derivateWithSynonims = derivateWithSynonims;
	}
	public SEARCH_WITH_TYPE getSearchWithType() {
		return searchWithType;
	}
	public void setSearchWithType(SEARCH_WITH_TYPE searchWithType) {
		this.searchWithType = searchWithType;
	}
	
	public void setIgnoreMCLast(boolean value) {
		ignoreMCLast = value;
	}
	
	public void setIgnoreCompanies(boolean value) {
		ignoreCompanies = value;
	}
	public boolean isEnableCompanyForceFirstName() {
		return enableCompanyForceFirstName;
	}
	public void setEnableCompanyForceFirstName(boolean enableCompanyForceFirstName) {
		this.enableCompanyForceFirstName = enableCompanyForceFirstName;
	}
	public List<String> getCompanyForceFirstNameList() {
		return companyForceFirstNameList;
	}
	public void setCompanyForceFirstNameList(List<String> companyForceFirstNameList) {
		this.companyForceFirstNameList = companyForceFirstNameList;
	}
	
	@Override
	public void setFirstNameOnFunction(String firstName, boolean isCompany,
			TSServerInfoFunction fct) {
		if(!isCompany || 
				(isEnableCompanyForceFirstName() 
						&& getCompanyForceFirstNameList() != null 
						&& getCompanyForceFirstNameList().contains(firstName)) || 
				(lastNameLength > 0)){
			fct.setParamValue(firstName);
		} else {
			fct.setParamValue("");
		}
	}
	
}
