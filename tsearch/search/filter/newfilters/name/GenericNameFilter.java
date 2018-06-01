package ro.cst.tsearch.search.filter.newfilters.name;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.codec.language.DoubleMetaphone;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.name.SynonymManager;
import ro.cst.tsearch.search.util.AdditionalInfoKeys;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.NameSet;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PartyNameSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.MatchEquivalents;
import ro.cst.tsearch.utils.NameEquivalents;
import ro.cst.tsearch.utils.StringCleaner;
import ro.cst.tsearch.utils.StringUtils;

import com.gwt.utils.client.base.NameG.NameType;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;


/**
 * @author Cristi Stochina
 */
public class GenericNameFilter extends FilterResponse {

	protected static class Result{
		public double score ;
		public boolean candInitialToWordReferenceApplied = false;
		public boolean emptyToWordApplied = false;
		public Result(){}
		public Result(double score){ this.score = score;}
	}
	
	private static final int MIN_COMP_LENGHT = 6;
	
	protected double	pondereLast		= 1.0d;
	protected double	pondereMiddle	= 1.0d;
	protected double	pondereFirst	= 1.0d;
	protected boolean	ignoreSufix 	= false;
	private double 	matchRefInitialWithCandidatWeightFromThreshold	= 0.85d;
	protected boolean useSubdivisionNameAsReference	= false;
	protected int stringCleaner						= StringCleaner.NO_CLEAN;
	protected boolean useSubdivisionNameAsCandidat	= false;
	protected boolean useArrangements = true;
	protected boolean useDoubleMetaphoneForLast = false;
	protected transient DoubleMetaphone doubleMetaphone = null;
	
	/** by default we ignore empty middle on candidates and also ignore empty middle on reference when ignoreMiddleOnEmpty is set true*/
	private boolean ignoreMiddleOnEmpty				= false;
	
	/** by default we ignore empty middle on candidates and also ignore empty middle on reference when ignoreMiddleOnEmpty is set true*/
	private boolean ignoreEmptyMiddleOnCandidat			= true;
	
	/** for B 4441*/
	private boolean useNameEquivalence				= false;
	
	private boolean useSynonymsForCandidates		= true;
	private boolean useSynonymsBothWays				= false;
	
	private boolean dontRequireCharArraySort			= false;
	
	// protected static final Logger logger = Logger.getLogger(GenericNameFilter.class);
	protected double myDoubleThreshold	= NameFilterFactory.NAME_FILTER_THRESHOLD;
	protected Set<String> companyNameRef	= new HashSet<String>() ;
	protected Set<NameI> setRef	= new HashSet<NameI>();
	private Set<NameI> setRefNicknames	= new HashSet<NameI>();
	
	protected TSServerInfoModule module = null;
	
	protected static final String[] suffixList = { "JR", "SR", "III","II","IV" };
	private static final long serialVersionUID = -19874320023423L;
	protected static int DEFAULT_SEARCH_ID = -10;
	private static final GenericNameFilter defaultNameFilter = new GenericNameFilter(DEFAULT_SEARCH_ID);
	private static final GenericNameFilter defaultNameFilterIgnoreMiddleOnEmpty = new GenericNameFilter(DEFAULT_SEARCH_ID);
	static {
		defaultNameFilter.myDoubleThreshold =1.0d;
		defaultNameFilterIgnoreMiddleOnEmpty.myDoubleThreshold =1.0d;
		defaultNameFilterIgnoreMiddleOnEmpty.setIgnoreMiddleOnEmpty(true);
	}
	protected boolean markIfCandidatesAreEmpty = false;

	protected boolean enableTrusteeCheck = false;
	protected boolean enableTrusteeDoctype = true;

	/**
	 * If null we filter both parties, else only grantor or grantee depending on the type
	 */
	private PType filterPartyType = null;
	
	public GenericNameFilter(long searchId) {
		super("", searchId);
	}
	

	public GenericNameFilter(String key, long searchId,boolean useSubdivisionName,TSServerInfoModule module, boolean ignoreSuffix) {
		super(key, searchId);
		super.setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
		super.setThreshold(new BigDecimal(NameFilterFactory.NAME_FILTER_THRESHOLD));
		this.useSubdivisionNameAsReference = useSubdivisionName;
		this.ignoreSufix = ignoreSuffix;
		this.module = module;
		init();
	}
	
	public GenericNameFilter(String key, long searchId,boolean useSubdivisionName,TSServerInfoModule module, 
								boolean ignoreSuffix, int stringCleaner) {
		super(key, searchId);
		super.setStrategyType(FilterResponse.STRATEGY_TYPE_HIGH_PASS);
		super.setThreshold(new BigDecimal(NameFilterFactory.NAME_FILTER_THRESHOLD));
		this.useSubdivisionNameAsReference = useSubdivisionName;
		this.ignoreSufix = ignoreSuffix;
		this.module = module;
		this.stringCleaner = stringCleaner;
		init();
	}
	
	public GenericNameFilter(String key, long searchId) {
		this(key,searchId,false,null, true);
	}

	public void setUseArrangements(boolean useArrangements){
		this.useArrangements = useArrangements;
	}
	
	public void setUseNameEquivalenceForFilter(boolean useNameEquivalence){
		this.useNameEquivalence = useNameEquivalence;
	}
	
	public void setDontRequireCharArraySort(boolean dontRequireCharArraySort){
		this.dontRequireCharArraySort = dontRequireCharArraySort;
	}

	public boolean isMarkIfCandidatesAreEmpty() {
		return markIfCandidatesAreEmpty;
	}

	public void setMarkIfCandidatesAreEmpty(boolean markIfCandidatesAreEmpty) {
		this.markIfCandidatesAreEmpty = markIfCandidatesAreEmpty;
	}

	public boolean isUseSynonymsForCandidates() {
		return useSynonymsForCandidates;
	}

	public void setUseSynonymsForCandidates(boolean useSynonymsForCandidates) {
		this.useSynonymsForCandidates = useSynonymsForCandidates;
	}

	/**
	 * Example:<br>
	 * <b>ANTHONY</b> has the following synonyms <i>TONY, ANTONIA, ANTONY, TONI, TONIA, TONYA, ANTONETTE, LATONYA</i><br>
	 * If useSynonymsBothWays is false only <b>ANTHONY</b> will be expanded to all synonyms<br>
	 * If useSynonymsBothWays is true each word like <b>TONY</b> will be expanded to all synonyms
	 * @return current setting for useSynonymsBothWays
	 */
	public boolean isUseSynonymsBothWays() {
		return useSynonymsBothWays;
	}

	/**
	 * Example:<br>
	 * <b>ANTHONY</b> has the following synonyms <i>TONY, ANTONIA, ANTONY, TONI, TONIA, TONYA, ANTONETTE, LATONYA</i><br>
	 * If useSynonymsBothWays is false only <b>ANTHONY</b> will be expanded to all synonyms<br>
	 * If useSynonymsBothWays is true each word like <b>TONY</b> will be expanded to all synonyms
	 * @param useSynonymsBothWays
	 */
	public void setUseSynonymsBothWays(boolean useSynonymsBothWays) {
		this.useSynonymsBothWays = useSynonymsBothWays;
	}

	public boolean isUseDoubleMetaphoneForLast() {
		return this.useDoubleMetaphoneForLast;
	}

	public void setUseDoubleMetaphoneForLast(boolean useDoubleMetaphoneForLast) {
		this.useDoubleMetaphoneForLast = useDoubleMetaphoneForLast;
	}
	
	private DoubleMetaphone getDoubleMetaphone() {
		if (doubleMetaphone == null) {
			doubleMetaphone = new DoubleMetaphone();
		}
		return doubleMetaphone;
	}

	public void init(){
		
		if(useSubdivisionNameAsReference){
			String subdivName = sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
			if(!StringUtils.isEmpty(subdivName)){
				setRef.add(new Name("","", StringCleaner.cleanString(stringCleaner, subdivName )));
				
				
				try {
					DataSite dataSite = HashCountyToIndex.getCrtServer(searchId, false);
					long miServerId = dataSite.getServerId();
					for (LegalI legal : sa.getForUpdateSearchLegalsNotNull(miServerId)) {
						setRef.add(new Name("","", StringCleaner.cleanString(stringCleaner, legal.getSubdivision().getName() )));
					}
					
					
				} catch (Exception e) {
					logger.error("Error loading names for Update saved from Parent Site", e);
				}
				
			}
		}
		else{
			if ( saKey.equals(SearchAttributes.BUYER_OBJECT) ){
				setRef=sa.getBuyers().getNames();

			}
			else if( saKey.equals(SearchAttributes.OWNER_OBJECT) ){
				setRef = sa.getOwners().getNames();
				//if(sa.isUpdate()) {
					try {
						DataSite dataSite = HashCountyToIndex.getCrtServer(searchId, false);
						long miServerId = dataSite.getServerId();
						setRef.addAll(sa.getForUpdateSearchGrantorNamesNotNull(miServerId));
						setRef.addAll(sa.getForUpdateSearchGranteeNamesNotNull(miServerId));
					} catch (Exception e) {
						logger.error("Error loading names for Update saved from Parent Site", e);
					}
				//}
			}
			else if ( saKey.equals(SearchAttributes.GB_MANAGER_OBJECT) )
			  {
				DocumentsManagerI documentsManager= InstanceManager.getManager().getCurrentInstance(
						searchId).getCrtSearchContext().getDocManager();
			    try{
					documentsManager.getAccess();
					GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

						if("grantor".equals(module.getTypeSearchGB())){
							setRef.addAll(gbm.getNamesForSearch(module.getIndexInGB(), searchId));
							if( searchId > 0 && InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().isIgnoreOwnerMiddleName() ){
								this.pondereMiddle = 0.0;
							}
						}
						else{
						  setRef.addAll(gbm.getNamesForBrokenChain(module.getIndexInGB(), searchId));
						  if(searchId > 0 && InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().isIgnoreBuyerMiddleName()){
								this.pondereMiddle = 0.0;
							}
						}
					  }
				
				    finally{
						  documentsManager.releaseAccess();
					    }
			}
			else if (saKey.equals(AdditionalInfoKeys.ADDITIONAL_NAMES_LIST)) {
				Object additionalInfo = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getAdditionalInfo(AdditionalInfoKeys.ADDITIONAL_NAMES_LIST);
				if(additionalInfo != null && additionalInfo instanceof List) {
					@SuppressWarnings("unchecked")
					List<NameI> additionalNames = (List<NameI>) additionalInfo;
					setRef.addAll(additionalNames);
				}
			}
			else if(setRef.size()==0){
				System.err.println(">>>>>>>>> Incorect GenericNameFilter use >>>>>>>>>>");
			}
		}
		if( setRef!=null && setRef.size()>0){
			
			if(!useSubdivisionNameAsReference && enableTrusteeCheck) {
				Set<NameI> newRefs = new HashSet<NameI>();
				for (NameI name : setRef) {
					if(name.getNameType().equals(NameType.TRUSTEE)) {
						newRefs.add(name);
					} else {
						String[] mihai = GenericFunctions1.extractType(name.getFullName());
						if(StringUtils.isNotEmpty(mihai[1])) {
							NameI clone = name.clone();
							clone.setNameType(NameType.TRUSTEE);
							newRefs.add(clone);
						} else {
							newRefs.add(name);	
						}
					}
				}
				setRef.clear();
				setRef.addAll(newRefs);
			}
			
			boolean canBeCompanyNameOrSubdivision = false;
			for(NameI name : setRef){
				if( StringUtils.isEmpty(name.getFirstName()) && StringUtils.isEmpty(name.getMiddleName()) && !StringUtils.isEmpty(name.getLastName()) ){
					companyNameRef.add( name.getLastName() );
					canBeCompanyNameOrSubdivision = true;
				}
			}
			if ( saKey.equals(SearchAttributes.OWNER_OBJECT) ){
				if( searchId>0 && InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().isIgnoreOwnerMiddleName() ){
					this.pondereMiddle = 0.0;
				}
			} else if ( saKey.equals(SearchAttributes.BUYER_OBJECT) ){
				if(searchId > 0 && InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa().isIgnoreBuyerMiddleName()){
					this.pondereMiddle = 0.0;
				}
			}
			if( this.ignoreSufix && !canBeCompanyNameOrSubdivision ){
				for(NameI name : setRef){
					for(int k=0; k < suffixList.length; k++){
						name.setFirstName	( name.getFirstName().replaceAll("(?i)\\b"+suffixList[k]+"\\b", "").trim() 	);
						name.setLastName	( name.getLastName().replaceAll("(?i)\\b"+suffixList[k]+"\\b", "").trim() 	);
						name.setMiddleName	( name.getMiddleName().replaceAll("(?i)\\b"+suffixList[k]+"\\b", "").trim() );
					}
				}
			}
			
			if(!useSubdivisionNameAsReference && enableTrusteeCheck) {
				Set<NameI> newRefs = new HashSet<NameI>();
				for (NameI name : setRef) {
					if(name.getNameType().equals(NameType.TRUSTEE)) {
						newRefs.add(name);
					} else {
						String[] mihai = GenericFunctions1.extractType(name.getFullName());
						if(StringUtils.isNotEmpty(mihai[1])) {
							NameI clone = name.clone();
							clone.setNameType(NameType.TRUSTEE);
							newRefs.add(clone);
						} else {
							newRefs.add(name);	
						}
					}
				}
				setRef.clear();
				setRef.addAll(newRefs);
			}
			
			if (useNameEquivalence){ //B 4441
				Set<NameI> setEquivRef	= new HashSet<NameI>();
				for(NameI name : setRef){
					String firstName = name.getFirstName();
					if (StringUtils.isNotEmpty(firstName)){
						String[] equivNames = NameEquivalents.getInstance(searchId).getEquivalent(firstName);
						if (equivNames != null){
							for (String nam:equivNames){
								if (!nam.equalsIgnoreCase(firstName)){
									NameI newName = new Name();
									newName.setFirstName(nam);
									newName.setLastName(name.getLastName());
									newName.setMiddleName(name.getMiddleName());
									newName.setSufix(name.getSufix());
									newName.setPrefix(name.getPrefix());
									newName.setNameType(name.getNameType());
									setEquivRef.add(newName);
								}
							}
							
						}
					}
				}
				for(NameI name : setEquivRef){
					setRef.add(name);
				}
			}
			
			
			
			if(isUseSynonymsForCandidates()) {
				setRefNicknames = new HashSet<NameI>();
				for (NameI originalName : setRef) {
					if(originalName.isCompany()) {
						continue;
					}
					String firstName = originalName.getFirstName();
					
					Set<String> firstSynonyms = null;
					if(isUseSynonymsBothWays()) {
						firstSynonyms = SynonymManager.getInstance().getSynonymsFor(firstName, true);
					} else {
						firstSynonyms = SynonymManager.getInstance().getSynonymsFor(firstName);
					}
					if(firstSynonyms != null && firstSynonyms.size() > 0) {
						for (String nameSynonym : firstSynonyms) {
							if(!firstName.equalsIgnoreCase(nameSynonym)) {
								NameI nickName = originalName.clone();
								nickName.setFirstName(nameSynonym);
								setRefNicknames.add(nickName);
							}
						}
					}
					
				}
			}
			
			for(NameI name : setRef){
				name.setMiddleName(cleanTokenOfPunctuation(name.getMiddleName()));
			}
			
		}		
	}

	public void setThreshold(BigDecimal big) {
		super.setThreshold(big);
		myDoubleThreshold = big.doubleValue();
	}
	
	/**
	 * calculate match using  LevenshteinDistance  
	 * optimization for Initials (e.g: Name and NameInitial return threshold)
	 * ignore spaces and "-" that may appear between two middle names
	 * */
	protected Result calculateMatchForFirstOrMiddle(String refToken, String candToken) {
		
		
		/*
		Result result = new Result();
		if(false && isUseSynonymsForCandidates()) {
			candToken = candToken.replaceAll("[ -]", "");
			result.score = -1;
			Set<String> firstSynonyms = SynonymManager.getInstance().getSynonymsFor(candToken);
			if(firstSynonyms != null && firstSynonyms.size() > 0) {
				for (String nameSynonym : firstSynonyms) {
					Result tempResult = calculateMatchForFirstOrMiddleInternal(refToken, nameSynonym);
					if(tempResult.score > result.score) {
						result = tempResult;
					}
					if(result.score == 1) {
						break;
					}
				}
			} else {
				result = calculateMatchForFirstOrMiddleInternal(refToken, candToken);	
			}
			
			
				
		} else {
			result = calculateMatchForFirstOrMiddleInternal(refToken, candToken);
		}*/
		return calculateMatchForFirstOrMiddleInternal(refToken, candToken);
	}

	protected Result calculateMatchForFirstOrMiddleInternal(String refToken,
			String candToken) {
		refToken = refToken.replaceAll("[ -]", "");
		candToken = candToken.replaceAll("[ -]", "");
		Result a = new Result();
		int refSize  = refToken.length();
		int candSize = candToken.length();
		int max = Math.max(refSize, candSize);
		
		/* both ref and cand empty  */
		if (max == 0) {
			a.score = 1.0d;
			return a;
		}
		
		/* be default we ignore empty middle on candidates and also ignore empty middle on reference when ignoreMiddleOnEmpty is set true*/
		if( (candSize==0 && ignoreEmptyMiddleOnCandidat) || ( ignoreMiddleOnEmpty && refSize==0 ) ){
			a.score = myDoubleThreshold;
			a.emptyToWordApplied = true;
			return a;
		}
		
		boolean oneLeterRef = refSize == 1;
		boolean oneLeterCand = candSize == 1;

		if ( oneLeterRef && oneLeterCand ) {
			if (refToken.equals(candToken)) {
				a.score = 1.0d;
				return a;
			} else {
				a.score = 0.0d;
				return a;
			}
		} else if (  ( oneLeterRef || oneLeterCand )  &&   refSize > 0  && candSize>0 ) {
			if ( refToken.startsWith(candToken)  ) {
				a.score = myDoubleThreshold;
				a.candInitialToWordReferenceApplied = true;
				return a;
			}
			else if( candToken.startsWith(refToken)  ){
				a.score = myDoubleThreshold * matchRefInitialWithCandidatWeightFromThreshold;
				a.candInitialToWordReferenceApplied = true;
				return  a;
			}
		}

		int distance = org.apache.commons.lang.StringUtils.getLevenshteinDistance(refToken, candToken);
		
		double score = 1 - ((double)distance) / max ;
		a.score = score ;
		return a;
	}

	/**
	 * Calculate match using LevenshteinDistance or, if the flag useDoubleMetaphoneForLast is true, compare reference with candidate using their double
	 * metaphone values; ignore spaces and "-" that may appear between two last names
	 * */
	public static double calculateMatchForLast(String refToken, String candToken, double myDoubleThreshold, DoubleMetaphone doubleMetaphone) {
		refToken = refToken.replaceAll("[ -]", "");
		candToken = candToken.replaceAll("[ -]", "");
		int refSize = refToken.length();
		int candSize = candToken.length();
		
		if(candSize ==0 && refSize>0){
			return myDoubleThreshold;
		}
		int max = Math.max(refSize, candSize);
		if (max == 0) {
			return 1.0d;
		}
		int distance = org.apache.commons.lang.StringUtils.getLevenshteinDistance(refToken, candToken);
		double score = 1 - ((double) distance) / max;

		if (doubleMetaphone != null && distance != 0 &&
				doubleMetaphone.isDoubleMetaphoneEqual(refToken, candToken)) {
			score = myDoubleThreshold;
		}

		return score;
	}
	
	protected double calculateMatchForLast(String refToken, String candToken) {
		return calculateMatchForLast(refToken, candToken, myDoubleThreshold, isUseDoubleMetaphoneForLast() ? getDoubleMetaphone() : null);
	}

	protected double calculateScore(String ref[], String cand[], boolean generateAranjaments) {
		
		toUpperCase(ref);
		toUpperCase(cand);
		double maxScore 			=	0.0d;
		int dif 					= 	-1	;
		boolean isEmptyFirstRef		=	StringUtils.isEmpty( ref[0] );
		boolean isEmptyMiddleRef	=	StringUtils.isEmpty( ref[1] );
		boolean isEmptyLastRef		=	StringUtils.isEmpty( ref[2] );
		
		if(isEmptyMiddleRef && pondereMiddle == 0){
			ref[1] ="*";
		}
		
		if ( (dif = ref.length - cand.length ) > 0) {
			String[] apend = new String[dif];
			for (int i = 0; i<apend.length; i++) {
				apend[i] = "";
			}
			cand = arrayMerge(cand, apend, cand.length - 1);
		}
		if( companyNameRef.size()>0 ){
			// -------- part that works best for subdivision Name and Company Names
			StringBuilder build =  new StringBuilder(cand[0]);
			for(int i=1;i<cand.length;i++){
				build.append(" " + cand[i]);
			}
			boolean requireSubdivisionClean = isUseSubdivisionNameAsCandidat();
			String candidate = MatchEquivalents.getInstance(searchId).getEquivalent(build.toString().trim(), requireSubdivisionClean).replaceAll("\\s", "").toUpperCase();
			
			maxScore = calculateMatchForCompanyOrSubdivision(companyNameRef, candidate, myDoubleThreshold, false, dontRequireCharArraySort,
					(isUseDoubleMetaphoneForLast() ? doubleMetaphone : null));
			if (maxScore >= myDoubleThreshold) {
				return maxScore;
			}
			//---------- end part for subdivision Name and Company Names
		}
		
		
		int candNonEmptySize = getNonEmptySize(cand);
		
		if ( generateAranjaments ) {
			int ar[] = null;
			
			//two last names, two middle name
			if( (cand.length==4 || cand.length==5) && (ref[1].split("[ -]+").length>1||ref[2].split("[ -]+").length>1) ){
				Aranjament aranj = new Aranjament(cand.length, cand.length);
				
				while ((ar = aranj.getNext()) != null) {
					
					ArrayList<String[]> all = new ArrayList<String[]>();
					//two last names or two middle names
					if(cand.length==4){
						String newCand1[] = {cand[ar[0]],cand[ar[1]],cand[ar[2]]+" "+cand[ar[3]]}; 
						String newCand2[] = {cand[ar[0]],cand[ar[1]]+" "+cand[ar[3]],cand[ar[2]]}; 
						all.add(newCand1);
						all.add(newCand2);
					}
					else{//two last names and two middle names
						String newCand3[] = {cand[ar[0]],cand[ar[1]]+" "+cand[ar[4]],cand[ar[2]]+" "+cand[ar[3]]}; 
						String newCand4[] = {cand[ar[0]],cand[ar[1]]+" "+cand[ar[3]],cand[ar[2]]+" "+cand[ar[4]]};
						String newCand5[] = {cand[ar[0]],cand[ar[1]],cand[ar[2]]+" "+cand[ar[3]]}; 
						String newCand6[] = {cand[ar[0]],cand[ar[1]]+" "+cand[ar[3]],cand[ar[2]]};
						String newCand7[] = {cand[ar[0]],cand[ar[1]],cand[ar[2]]+" "+cand[ar[4]]}; 
						String newCand8[] = {cand[ar[0]],cand[ar[1]]+" "+cand[ar[4]],cand[ar[2]]};
						all.add(newCand3);
						all.add(newCand4);
						all.add(newCand5);
						all.add(newCand6);
						all.add(newCand7);
						all.add(newCand8);
					}
					
					for(String[] newCand:all ){
						if(StringUtils.isEmpty(newCand[2])){
							continue;
						}
						boolean ignoreFirst		=	isEmptyFirstRef		&&	StringUtils.isEmpty(newCand[0]) ;
						boolean ignoreLast		= 	isEmptyLastRef;
						boolean ignoreMiddle	= 	isEmptyMiddleRef 	&&  StringUtils.isEmpty(newCand[1]) ;
						
						Result resFirst = calculateMatchForFirstOrMiddle( ref[0], newCand[0] );
						double firstScore  	= ignoreFirst 	? 0 : resFirst.score;
						
						String midleCandidat = newCand[1];
						Result resMiddle = calculateMatchForFirstOrMiddle( ref[1],  midleCandidat);
						double middleScore 	= ignoreMiddle 	? 0 : resMiddle.score ;
						double lastScore 	= ignoreLast 	? 0 : calculateMatchForLast( ref[2], newCand[2] );
						
						boolean oneMapped = resMiddle.candInitialToWordReferenceApplied||resFirst.candInitialToWordReferenceApplied;
						if(resMiddle.emptyToWordApplied&&resFirst.emptyToWordApplied){
							continue;
						}
						
						double numarator 	= pondereFirst*firstScore 
								+ ((pondereMiddle==0&&((oneMapped&&midleCandidat.length()>1)||candNonEmptySize <= 2)) ? pondereLast : pondereMiddle)*middleScore 
								+ pondereLast*lastScore ;
						double numitor 		= (ignoreFirst ? 0 : pondereFirst) + (ignoreLast ? 0 : pondereLast)
								+ (ignoreMiddle ? 0 : ((pondereMiddle==0&&((oneMapped&&midleCandidat.length()>1)||candNonEmptySize <= 2)) ? pondereLast : pondereMiddle) ) ;
						double curentScore	= numarator / numitor;
						maxScore = Math.max(curentScore, maxScore);
						if ( maxScore >= myDoubleThreshold ) {
							return maxScore;
						}
					}
				}
			}
			else{
				Aranjament aranj = new Aranjament(cand.length, ref.length);
				while ((ar = aranj.getNext()) != null) {
					if(StringUtils.isEmpty(cand[ar[2]])){
						continue;
					}
					if (doNotCompare(ref, cand, ar)) {
						continue;
					}
					boolean ignoreFirst		=	isEmptyFirstRef		&&	StringUtils.isEmpty(cand[ar[0]]) ;
					boolean ignoreLast		= 	isEmptyLastRef;
					boolean ignoreMiddle	= 	isEmptyMiddleRef 	&&  StringUtils.isEmpty(cand[ar[1]]) ;
					
					Result resFirst = calculateMatchForFirstOrMiddle( ref[0], cand[ar[0]] );
					double firstScore  	= ignoreFirst 	? 0 : resFirst.score;
					
					String midleCandidat = cand[ar[1]];
					Result resMiddle = calculateMatchForFirstOrMiddle( ref[1],  midleCandidat);
					double middleScore 	= ignoreMiddle 	? 0 : resMiddle.score ;
					double lastScore 	= ignoreLast 	? 0 : calculateMatchForLast( ref[2], cand[ar[2]] );
					
					boolean oneMapped = resMiddle.candInitialToWordReferenceApplied||resFirst.candInitialToWordReferenceApplied;
					//boolean twoMapped = resMiddle.candInitialToWordReferenceApplied&&resFirst.candInitialToWordReferenceApplied;
					
					if(resMiddle.emptyToWordApplied&&resFirst.emptyToWordApplied){
						continue;
					}
					
					double numarator 	= pondereFirst*firstScore 
							+ ((pondereMiddle==0&&((oneMapped&&midleCandidat.length()>1)||candNonEmptySize <= 2)) ? pondereLast : pondereMiddle)*middleScore 
							+ pondereLast*lastScore ;
					double numitor 		= (ignoreFirst ? 0 : pondereFirst) + (ignoreLast ? 0 : pondereLast)
							+ (ignoreMiddle ? 0 : ((pondereMiddle==0&&((oneMapped&&midleCandidat.length()>1)||candNonEmptySize <= 2)) ? pondereLast : pondereMiddle) ) ;
					double curentScore	= numarator / numitor;
					maxScore = Math.max(curentScore, maxScore);
					if ( maxScore >= myDoubleThreshold ) {
						return maxScore;
					}
				}
			}
		} else {
			boolean ignoreFirst		=	isEmptyFirstRef		&&	StringUtils.isEmpty(cand[0]) ;
			boolean ignoreLast		= 	isEmptyLastRef 		&&	StringUtils.isEmpty(cand[2]) ;
			boolean ignoreMiddle	= 	isEmptyMiddleRef 	&&  StringUtils.isEmpty(cand[1]) ;
			
			Result resFirst = calculateMatchForFirstOrMiddle( ref[0], cand[0] );
			double firstScore  	= ignoreFirst 	? 0 : resFirst.score;
			
			Result resMiddle = calculateMatchForFirstOrMiddle( ref[1], cand[1] );
			double middleScore 	= ignoreMiddle 	? 0 : resMiddle.score ;
			double lastScore 	= ignoreLast 	? 0 : calculateMatchForLast( ref[2], cand[2] );
			
			double numarator 	= pondereFirst*firstScore 
					+ ((pondereMiddle==0&&(resMiddle.candInitialToWordReferenceApplied||resFirst.candInitialToWordReferenceApplied||candNonEmptySize <= 2)) ? pondereLast : pondereMiddle)*middleScore 
					+ pondereLast*lastScore ;
			double numitor 		= (ignoreFirst ? 0 : pondereFirst) + (ignoreLast ? 0 : pondereLast)
					+ (ignoreMiddle ? 0 : ((pondereMiddle==0&&(resMiddle.candInitialToWordReferenceApplied||resFirst.candInitialToWordReferenceApplied||candNonEmptySize <= 2)) ? pondereLast : pondereMiddle) ) ;
			double curentScore	= numarator / numitor;
			maxScore = Math.max(curentScore, maxScore);
			if ( maxScore >= myDoubleThreshold ) {
				return maxScore;
			}
		}
		return maxScore;
	}
	
	/*
	 * task 7188
	 * when rearranging tokens, do not compare a token that has more than one character 
	 * and a token that has only one character, to avoid testing first name with middle initial
	 */
	public static boolean doNotCompare(String ref[], String cand[], int ar[]) {
		if (ref.length==cand.length && cand.length==ar.length) {
			int len = ref.length;
			boolean isRearrangement = false;
			for (int i=0;i<len;i++) {
				if (i!=ar[i]) {
					isRearrangement = true;
					break;
				}
			}
			if (isRearrangement) {
				for (int i=0;i<len;i++) {
					if ( (ref[i].length()==1 && cand[ar[i]].length()>1) || (ref[i].length()>1 && cand[ar[i]].length()==1) ) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private void clear(String []cand){
		for(int j=0;j<cand.length;j++ ){
			for(int k=0;k<suffixList.length;k++){
				cand[j]= cand[j].replaceAll("(?i)\\b"+suffixList[k]+"\\b", "").trim();
			}
		}
	}
	
	public BigDecimal getScoreForName(NameI candidate) {
		ParsedResponse pr = new ParsedResponse();
		Vector<NameSet> grantorSet = (Vector<NameSet>)pr.getGrantorNameSet();
		
		NameSet nameSet = new NameSet();
		nameSet.setAtribute("OwnerFirstName", candidate.getFirstName());
		nameSet.setAtribute("OwnerMiddleName", candidate.getMiddleName());
		nameSet.setAtribute("OwnerLastName", candidate.getLastName());
		nameSet.setAtribute("SpouseFirstName", "");
		nameSet.setAtribute("SpouseMiddleName", "");
		nameSet.setAtribute("SpouseLastName", "");
		
		grantorSet.add(nameSet);
		return getScoreOneRow(pr);
	}
	
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		if ( setRef==null || setRef.size()==0 ){
			if(markIfCandidatesAreEmpty) {
				return ATSDecimalNumberFormat.NA;
			} else {
				return new BigDecimal( myDoubleThreshold );
			}
		}
		double score 	= 0.0 ;
		double maxScore = 0.0 ;
		String cand[]	= new String[3] ;
		Vector<NameSet> candGrantors = null ;
		Vector<NameSet> candGrantees = null ;
		
		if( useSubdivisionNameAsCandidat ){
			candGrantees = new Vector<NameSet>();
			candGrantors = getSubdivisionNames( row, stringCleaner );
		} else {
			//for RO like sites 
			candGrantors = (Vector<NameSet>)row.getGrantorNameSet();
			candGrantees = (Vector<NameSet>)row.getGranteeNameSet();
			if( row.getPropertyIdentificationSetCount() > 0 ){
				// check if we need to take the candidate from the PIS (e.g: on TR or AO sites )
				if(candGrantors.size() == 0){			
					candGrantors.addAll( getOwnersFromPis(row) );
				}
				if(candGrantees.size() == 0){			
					candGrantees.addAll( getOwnersFromPis(row) );
				}
			}
			for (PartyNameSet nameSet : (Vector<PartyNameSet>)row.getPartyNameSet()) {
				candGrantors.add(nameSet.toNameSet());
			}
			
			try {
				if(candGrantors.isEmpty() && candGrantees.isEmpty() && row.getDocument() != null) {
					DocumentI documentI = row.getDocument();
					if(documentI instanceof RegisterDocumentI) {
						RegisterDocumentI regDoc = (RegisterDocumentI)documentI;
						PartyI grantor = regDoc.getGrantor();
						if(grantor != null && grantor.size() > 0) {
							for (NameI nameI : grantor.getNames()) {
								NameSet nameSet = new NameSet();
								nameSet.setAtribute("OwnerFirstName", nameI.getFirstName());
								nameSet.setAtribute("OwnerLastName", nameI.getLastName());
								nameSet.setAtribute("OwnerMiddleName", nameI.getMiddleName());
								candGrantors.add(nameSet);
							} 
						}
						
						PartyI grantee = regDoc.getGrantee();
						if(grantee != null && grantee.size() > 0) {
							for (NameI nameI : grantee.getNames()) {
								NameSet nameSet = new NameSet();
								nameSet.setAtribute("OwnerFirstName", nameI.getFirstName());
								nameSet.setAtribute("OwnerLastName", nameI.getLastName());
								nameSet.setAtribute("OwnerMiddleName", nameI.getMiddleName());
								candGrantees.add(nameSet);
							} 
						}
					} else {
						for(PropertyI prop:documentI.getProperties()){
		    				if(prop.hasOwners()){
		    					PartyI grantor = prop.getOwner();
		    					if(grantor != null && grantor.size() > 0) {
		    						for (NameI nameI : grantor.getNames()) {
		    							NameSet nameSet = new NameSet();
		    							nameSet.setAtribute("OwnerFirstName", nameI.getFirstName());
		    							nameSet.setAtribute("OwnerLastName", nameI.getLastName());
		    							nameSet.setAtribute("OwnerMiddleName", nameI.getMiddleName());
		    							candGrantors.add(nameSet);
		    						} 
		    					}	    					
		    				}
		    			}
					}
				}
			} catch (Exception e) {
				logger.error("Error while collecting names from document", e);
			}
			
			
		}
		
		boolean hasValidGrantor = false;
		boolean hasValidGrantee = false;
		
		
		Set<NameI> allNames = new HashSet<NameI>(setRef);
		if(isUseSynonymsForCandidates() && setRefNicknames != null) {
			allNames.addAll(setRefNicknames);
		}
		for (NameI element: allNames) { 
			String[]  ref = { element.getFirstName(), element.getMiddleName(), element.getLastName() };
			
			
			if(getFilterPartyType() == null || getFilterPartyType().equals(PType.GRANTOR)) {
			
				for (NameSet grantor : candGrantors) {
					String candFirst	= grantor.getAtribute("OwnerFirstName").trim();
					String candLast		= grantor.getAtribute("OwnerLastName").trim();
					String candMiddLe	= cleanTokenOfPunctuation(grantor.getAtribute("OwnerMiddleName"));
					
					String name			= candFirst + " " + candMiddLe + " " + candLast;
					//sometimes we receive bad information from parser
					if( StringUtils.isEmpty(name) || ( StringUtils.isEmpty(candFirst) && StringUtils.isEmpty(candMiddLe) && candLast.length()<=2) ){
						continue;
					}
					hasValidGrantor = true;
					cand	= new String[3] ;
					//first we trust the parser
					cand[0] = candFirst;
					cand[1] = candMiddLe;
					cand[2] = candLast;
					if(ignoreSufix){
						clear(cand);
					}
					if(enableTrusteeCheck && 
							!enableTrusteeDoctype && 
							!element.getNameType().equals(NameType.TRUSTEE) &&
							StringUtils.isNotEmpty(GenericFunctions1.extractType(name)[1])) {
						//nothing happens if trustee filter is enabled and this ref is not trustee but the candidate is
						score = 0.0;
					} else {
						score = calculateScore(ref, cand, useArrangements);
					}
					maxScore = Math.max( maxScore, score );
					if (score >= myDoubleThreshold) {
						if(getStrategyType() != STRATEGY_TYPE_HYBRID) {
							return new BigDecimal(score);
						}
					}
					
					
					
					
					//we do not trust the parser tokeneizer
					cand = name.split("[ ,-]+");
					if(ignoreSufix){
						clear(cand);
					}
					if (cand.length > 3) {
						cand = removeEmptyStringsCand( cand );
					}
					if(enableTrusteeCheck && 
							!enableTrusteeDoctype && 
							!element.getNameType().equals(NameType.TRUSTEE) &&
							StringUtils.isNotEmpty(GenericFunctions1.extractType(name)[1])) {
						//nothing happens if trustee filter is enabled and this ref is not trustee but the candidate is
						score = 0.0;
					} else {
						score = calculateScore(ref, cand, useArrangements);
					}
					maxScore = Math.max( maxScore, score );
					if (score >= myDoubleThreshold) {
						if(getStrategyType() != STRATEGY_TYPE_HYBRID) {
							return new BigDecimal(score);
						}
						
					}
					
					//lets trust more the parser
					String candLastWithoutSpaces	= candLast.replaceAll("\\s+", "");
					String candFirstWithoutSpaces	= candFirst.replaceAll("\\s+", "");
					String candMiddleWithoutSpaces	= candMiddLe.replaceAll("\\s", "");
					if( !candLastWithoutSpaces.equals(candLast) || !candFirstWithoutSpaces.equals(candFirst) || !candMiddleWithoutSpaces.equals(candMiddLe)){
						String []temp = {candFirstWithoutSpaces,candMiddleWithoutSpaces,candLastWithoutSpaces};
						cand = temp;
						if(ignoreSufix){
							clear(cand);
						}
						if(enableTrusteeCheck && 
								!enableTrusteeDoctype && 
								!element.getNameType().equals(NameType.TRUSTEE) &&
								StringUtils.isNotEmpty(GenericFunctions1.extractType(name)[1])) {
							//nothing happens if trustee filter is enabled and this ref is not trustee but the candidate is
							score = 0.0;
						} else {
							score = calculateScore(ref, cand, useArrangements);
						}
						maxScore = Math.max( maxScore, score );
						if (score >= myDoubleThreshold) {
							if(getStrategyType() != STRATEGY_TYPE_HYBRID) {
								return new BigDecimal(score);
							}
						}
					}
					
					String candSpouseFirst 	= grantor.getAtribute("SpouseFirstName").trim();
					String candSpouseLast 	= grantor.getAtribute("SpouseLastName").trim();
					String candSpouseMiddLe = cleanTokenOfPunctuation(grantor.getAtribute("SpouseMiddleName"));
					name	=	candSpouseFirst + " " + candSpouseMiddLe + " " + candSpouseLast;
					if( StringUtils.isEmpty(name) || ( StringUtils.isEmpty(candSpouseFirst) && StringUtils.isEmpty(candSpouseMiddLe) && candSpouseLast.length()<=2) ){
						continue;
					}
					
					//first we trust the parser
					cand	= new String[3] ;
					cand[0] = candSpouseFirst;
					cand[1] = candSpouseLast;
					cand[2] = candSpouseMiddLe;
					if(ignoreSufix){
						clear(cand);
					}
					if(enableTrusteeCheck && 
							!enableTrusteeDoctype && 
							!element.getNameType().equals(NameType.TRUSTEE) &&
							StringUtils.isNotEmpty(GenericFunctions1.extractType(name)[1])) {
						//nothing happens if trustee filter is enabled and this ref is not trustee but the candidate is
						score = 0.0;
					} else {
						score = calculateScore(ref, cand, useArrangements);
					}
					maxScore = Math.max( maxScore, score );
					if (score >= myDoubleThreshold) {
						if(getStrategyType() != STRATEGY_TYPE_HYBRID) {
							return new BigDecimal(score);
						}
					}
					
					//we do not trust the parser tokeneizer
					cand = name.split("[ ,-]+");
					if( ignoreSufix ){
						clear(cand);
					}
					if (cand.length > 3) {
						cand = removeEmptyStringsCand(cand);
					}
					if(enableTrusteeCheck && 
							!enableTrusteeDoctype && 
							!element.getNameType().equals(NameType.TRUSTEE) &&
							StringUtils.isNotEmpty(GenericFunctions1.extractType(name)[1])) {
						//nothing happens if trustee filter is enabled and this ref is not trustee but the candidate is
						score = 0.0;
					} else {
						score 	= calculateScore(ref, cand, useArrangements);
					}
					maxScore= Math.max( maxScore, score );
					if (score >= myDoubleThreshold) {
						if(getStrategyType() != STRATEGY_TYPE_HYBRID) {
							return new BigDecimal(score);
						}
					}
					
					//lets trust more the parser
					candLastWithoutSpaces	= candSpouseLast.replaceAll("\\s+", "");
					candFirstWithoutSpaces	= candSpouseFirst.replaceAll("\\s+", "");
					candMiddleWithoutSpaces	= candSpouseMiddLe.replaceAll("\\s", "");
					if( !candLastWithoutSpaces.equals(candSpouseLast) || !candFirstWithoutSpaces.equals(candSpouseFirst) || !candMiddleWithoutSpaces.equals(candSpouseMiddLe)){
						String []temp = {candFirstWithoutSpaces,candMiddleWithoutSpaces,candLastWithoutSpaces};
						cand = temp;
						if(ignoreSufix){
							clear(cand);
						}
						if(enableTrusteeCheck && 
								!enableTrusteeDoctype && 
								!element.getNameType().equals(NameType.TRUSTEE) &&
								StringUtils.isNotEmpty(GenericFunctions1.extractType(name)[1])) {
							//nothing happens if trustee filter is enabled and this ref is not trustee but the candidate is
							score = 0.0;
						} else {
							score = calculateScore(ref, cand, useArrangements);
						}
						maxScore = Math.max( maxScore, score );
						if (score >= myDoubleThreshold) {
							if(getStrategyType() != STRATEGY_TYPE_HYBRID) {
								return new BigDecimal(score);
							}
						}
					}
				}
				
			}
			
			if(getFilterPartyType() == null || getFilterPartyType().equals(PType.GRANTEE)) {
			
				for (NameSet grantee:candGrantees) {
					String candFirst 	= grantee.getAtribute("OwnerFirstName").trim();
					String candLast 	= grantee.getAtribute("OwnerLastName").trim();
					String candMiddLe 	= cleanTokenOfPunctuation(grantee.getAtribute("OwnerMiddleName"));
					String name 		= candFirst + " " + candMiddLe + " " + candLast;
					//sometimes we receive bad information from parser
					if(StringUtils.isEmpty(name) || (StringUtils.isEmpty(candFirst) && StringUtils.isEmpty(candMiddLe) && candLast.length()<=2) ){
						continue;
					}
					
					hasValidGrantee = true;
					
					
					cand	= new String[3] ;
					//first we trust the parser
					cand[0] = candFirst;
					cand[1] = candMiddLe;
					cand[2] = candLast;
					if(ignoreSufix){
						clear(cand);
					}
					if(enableTrusteeCheck && 
							!enableTrusteeDoctype && 
							!element.getNameType().equals(NameType.TRUSTEE) &&
							StringUtils.isNotEmpty(GenericFunctions1.extractType(name)[1])) {
						//nothing happens if trustee filter is enabled and this ref is not trustee but the candidate is
						score = 0.0;
					} else {
						score = calculateScore(ref, cand, useArrangements);
					}
					maxScore = Math.max( maxScore, score );
					if (score >= myDoubleThreshold) {
						if(getStrategyType() != STRATEGY_TYPE_HYBRID) {
							return new BigDecimal(score);
						}
					}
					
					
					
					//we do not trust the parser tokeneizer
					cand = name.split("[ ,-]+");
					if(ignoreSufix){
						clear(cand);
					}
					
					if (cand.length > 3) {
						cand = removeEmptyStringsCand(cand);
					}
					if(enableTrusteeCheck && 
							!enableTrusteeDoctype && 
							!element.getNameType().equals(NameType.TRUSTEE) &&
							StringUtils.isNotEmpty(GenericFunctions1.extractType(name)[1])) {
						//nothing happens if trustee filter is enabled and this ref is not trustee but the candidate is
						score = 0.0;
					} else {
						score 	= calculateScore(ref, cand, useArrangements);
					}
					maxScore= Math.max(maxScore, score);
					if ( score >= myDoubleThreshold ) {
						if(getStrategyType() != STRATEGY_TYPE_HYBRID) {
							return new BigDecimal(score);
						}
					}
					
					String candLastWithoutSpaces	= candLast.replaceAll("\\s+", "");
					String candFirstWithoutSpaces	= candFirst.replaceAll("\\s+", "");
					String candMiddleWithoutSpaces	= candMiddLe.replaceAll("\\s", "");
					if( !candLastWithoutSpaces.equals(candLast) || !candFirstWithoutSpaces.equals(candFirst) || !candMiddleWithoutSpaces.equals(candMiddLe)){
						String []temp = {candFirstWithoutSpaces,candMiddleWithoutSpaces,candLastWithoutSpaces};
						cand = temp;
						if(ignoreSufix){
							clear(cand);
						}
						if(enableTrusteeCheck && 
								!enableTrusteeDoctype && 
								!element.getNameType().equals(NameType.TRUSTEE) &&
								StringUtils.isNotEmpty(GenericFunctions1.extractType(name)[1])) {
							//nothing happens if trustee filter is enabled and this ref is not trustee but the candidate is
							score = 0.0;
						} else {
							score = calculateScore(ref, cand, useArrangements);	
						}
						
						
						maxScore = Math.max( maxScore, score );
						if (score >= myDoubleThreshold) {
							if(getStrategyType() != STRATEGY_TYPE_HYBRID) {
								return new BigDecimal(score);
							}
						}
					}
					
					String candSpouseFirst 	= grantee.getAtribute("SpouseFirstName").trim();
					String candSpouseLast 	= grantee.getAtribute("SpouseLastName").trim();
					String candSpouseMiddLe = cleanTokenOfPunctuation(grantee.getAtribute("SpouseMiddleName"));
					name	=	candSpouseFirst + " " + candSpouseMiddLe + " " + candSpouseLast;
					if( StringUtils.isEmpty(name) || (StringUtils.isEmpty(candSpouseFirst) && StringUtils.isEmpty(candSpouseMiddLe) && candSpouseLast.length()<=2) ){
						continue;
					}
					cand = name.split("[ ,-]+");
					if(ignoreSufix){
						clear(cand);
					}
					if (cand.length > 3) {
						cand = removeEmptyStringsCand(cand);
					}
					if(enableTrusteeCheck && 
							!enableTrusteeDoctype && 
							!element.getNameType().equals(NameType.TRUSTEE) &&
							StringUtils.isNotEmpty(GenericFunctions1.extractType(name)[1])) {
						//nothing happens if trustee filter is enabled and this ref is not trustee but the candidate is
						score = 0.0;
					} else {
						score 	= calculateScore(ref, cand, useArrangements);
					}
					maxScore= Math.max(maxScore, score);
					if (score >= myDoubleThreshold) {
						if(getStrategyType() != STRATEGY_TYPE_HYBRID) {
							return new BigDecimal(score);
						}
					}
					
					candLastWithoutSpaces	= candSpouseLast.replaceAll("\\s+", "");
					candFirstWithoutSpaces	= candSpouseFirst.replaceAll("\\s+", "");
					candMiddleWithoutSpaces	= candSpouseMiddLe.replaceAll("\\s", "");
					if( !candLastWithoutSpaces.equals(candSpouseLast) || !candFirstWithoutSpaces.equals(candSpouseFirst) || !candMiddleWithoutSpaces.equals(candSpouseMiddLe)){
						String []temp = {candFirstWithoutSpaces,candMiddleWithoutSpaces,candLastWithoutSpaces};
						cand = temp;
						if(ignoreSufix){
							clear(cand);
						}
						if(enableTrusteeCheck && 
								!enableTrusteeDoctype && 
								!element.getNameType().equals(NameType.TRUSTEE) &&
								StringUtils.isNotEmpty(GenericFunctions1.extractType(name)[1])) {
							//nothing happens if trustee filter is enabled and this ref is not trustee but the candidate is
							score = 0.0;
						} else {
							score = calculateScore(ref, cand, useArrangements);
						}
						maxScore = Math.max( maxScore, score );
						if (score >= myDoubleThreshold) {
							if(getStrategyType() != STRATEGY_TYPE_HYBRID) {
								return new BigDecimal(score);
							}
						}
					}
				}
			
			}
		}
		//all are empty
		if ( maxScore==0.0  && (candGrantors.size()==0 || !hasValidGrantor) && (candGrantees.size()==0 || !hasValidGrantee)) {
			if(markIfCandidatesAreEmpty) {
				return ATSDecimalNumberFormat.NA;
			} else {
				maxScore = myDoubleThreshold;
			}
		}
		return new BigDecimal(maxScore);
	}

	public double getPondereFirst() {
		return pondereFirst;
	}

	public void setPondereFirst(double pondereFirst) {
		this.pondereFirst = pondereFirst;
	}

	public double getPondereLast() {
		return pondereLast;
	}

	public void setPondereLast(double pondereLast) {
		this.pondereLast = pondereLast;
	}

	public double getPondereMiddle() {
		return pondereMiddle;
	}

	public void setPondereMiddle(double pondereMiddle) {
		this.pondereMiddle = pondereMiddle;
	}

	public boolean isUseSubdivisionName() {
		return useSubdivisionNameAsReference;
	}
	
	public void setUseSubdivisionNameAsReference(
			boolean useSubdivisionNameAsReference) {
		this.useSubdivisionNameAsReference = useSubdivisionNameAsReference;
	}


	@Override
    public String getFilterName(){
		if(useSubdivisionNameAsCandidat){
			return "Filter by Subdiv Name";
		}
		
    	return "Filter " + ((pondereMiddle==0.0)?"(ignoring middle name)":"") + "by Name" + (enableTrusteeCheck?"(checking trustee match)":"");
    }
	
	@Override
	public String getFilterCriteria(){
		if(useSubdivisionNameAsCandidat){
			return "Subdiv Name '"+ getReferenceNameString() + "'";
		}
		String result = "Name='" + getReferenceNameString() + "'";
		String nickResult = getReferenceNickNameString();
		if(StringUtils.isNotEmpty(nickResult)) {
			result += " and NickNames='" + nickResult + "' ";
		}
		
		if(pondereMiddle==0.0) {
			result += "(ignoring middle name)";
		} else {
			if(ignoreMiddleOnEmpty && ignoreEmptyMiddleOnCandidat) {
				result += "(ignoring middle name when empty on reference or candidate)";	
			} else if (ignoreMiddleOnEmpty) {
				result += "(ignoring middle name when empty on reference)";
			} else if (ignoreEmptyMiddleOnCandidat) {
				result += "(ignoring middle name when empty on candidate)";
			}
		}
		return  result + (enableTrusteeCheck?"(checking trustee match)":"");
    }
	
	public String getReferenceNickNameString() {
		StringBuilder nameBuff= new StringBuilder();
		if(isUseSynonymsForCandidates() && setRefNicknames != null) {
			for ( NameI element : setRefNicknames ) {
				nameBuff.append( element.getFullName() );
				nameBuff.append("/");
			}
		}
		return nameBuff.toString();
	}
	
	public String getReferenceNameString() {
		StringBuilder nameBuff= new StringBuilder();
		for ( NameI element : setRef ) {
			nameBuff.append( element.getFullName() );
			nameBuff.append("/");
		}
		if(nameBuff.length() > 0) {
			nameBuff.deleteCharAt(nameBuff.length() - 1);
		}
		return nameBuff.toString();
	}
	
	public boolean isUseSubdivisionNameAsCandidat() {
		return useSubdivisionNameAsCandidat;
	}

	public void setUseSubdivisionNameAsCandidat(boolean useSubdivisionNameAsCandidat) {
		this.useSubdivisionNameAsCandidat = useSubdivisionNameAsCandidat;
	}
	
	public boolean isIgnoreSufix() {
		return ignoreSufix;
	}

	public void setIgnoreSufix(boolean ignoreSufix) {
		this.ignoreSufix = ignoreSufix;
	}


	public boolean isIgnoreMiddleOnEmpty() {
		return ignoreMiddleOnEmpty;
	}

	public void setIgnoreMiddleOnEmpty(boolean ignoreMiddleOnEmpty) {
		this.ignoreMiddleOnEmpty = ignoreMiddleOnEmpty;
	}

	public boolean isIgnoreEmptyMiddleOnCandidat() {
		return ignoreEmptyMiddleOnCandidat;
	}


	public void setIgnoreEmptyMiddleOnCandidat(boolean ignoreEmptyMiddleOnCandidat) {
		this.ignoreEmptyMiddleOnCandidat = ignoreEmptyMiddleOnCandidat;
	}
	
	public double getMatchRefInitialWithCandidatWeightFromThreshold() {
		return matchRefInitialWithCandidatWeightFromThreshold;
	}

	public void setMatchRefInitialWithCandidatWeightFromThreshold(double matchRefInitialWithCandidatWeightFromThreshold) {
		this.matchRefInitialWithCandidatWeightFromThreshold = matchRefInitialWithCandidatWeightFromThreshold;
	}
	
	private static int getNonEmptySize(String cand[]){
		int size= 0;
		for(int i=0; i<cand.length ;i++){
			if(!StringUtils.isEmpty(cand[i])){
				size++;
			}
		}
		return size;
	}
	
	private static String[] arrayMerge(String[] a, String b[], int startAtA) {
		
		List<String> result = new ArrayList<String>();
		boolean added = false;
		for (int i = 0; i < a.length; i++) {
			if(i < startAtA) {
				result.add(a[i]);
			} else {
				if(!added) {
					for (String string : b) {
						result.add(string);
					}
				}
				result.add(a[i]);
			}
		}
		
		return result.toArray(new String[result.size()]);
		
		// create new array
//		String[] res = new String[a.length + b.length];
//		int start = 0;
//		System.arraycopy(a, 0, res, start, a.length);
//		start += a.length;
//		System.arraycopy(b, 0, res, start, b.length);
//		return res;
	}
	
	public static String[] removeEmptyStringsCand(String a[]) {
		Vector<String> v = new Vector<String>();
		for (int i = 0; i < a.length; i++) {
			if (!StringUtils.isEmpty(a[i])) {
				v.add(a[i]);
			}
		}
		return (String[]) v.toArray(new String[0]);
	}

	protected static void toUpperCase( String a[] ) {
		for (int i = 0; i < a.length; i++) {
			a[i] = a[i].toUpperCase();
		}
	}
	
	private static Vector<NameSet> getOwnersFromPis(ParsedResponse row){
		PropertyIdentificationSet pis = row.getPropertyIdentificationSet(0);
		NameSet owner = new NameSet();
		owner.setAtribute("OwnerFirstName", pis.getAtribute("OwnerFirstName"));
		owner.setAtribute("OwnerMiddleName", pis.getAtribute("OwnerMiddleName"));
		owner.setAtribute("OwnerLastName", pis.getAtribute("OwnerLastName"));
		owner.setAtribute("SpouseFirstName", pis.getAtribute("SpouseFirstName"));
		owner.setAtribute("SpouseMiddleName", pis.getAtribute("SpouseMiddleName"));
		owner.setAtribute("SpouseLastName", pis.getAtribute("SpouseLastName"));
		Vector<NameSet> owners = new Vector<NameSet>();
		owners.add(owner);
		return owners;
	}
	
	private static Vector<NameSet> getSubdivisionNames(ParsedResponse row, int stringCleaner){
		Vector<NameSet> owners = new Vector<NameSet>();
		Vector<PropertyIdentificationSet> pisvec = (Vector<PropertyIdentificationSet>)row.getPropertyIdentificationSet();
		if( pisvec!=null ){
			for(int i=0;i<pisvec.size();i++){
				PropertyIdentificationSet pis = row.getPropertyIdentificationSet(i);
				if(pis!=null){
					String name = pis.getAtribute("SubdivisionName");
					if(!StringUtils.isEmpty(name)){
						NameSet owner = new NameSet();
						owner.setAtribute("OwnerFirstName", "");
						owner.setAtribute("OwnerMiddleName", "");
						owner.setAtribute("OwnerLastName", StringCleaner.cleanString(stringCleaner, name));
						owners.add(owner);
					}
				}
			}
		}
		if(owners.size() == 0) {
			DocumentI documentI = row.getDocument();
			if(documentI != null) {
				if(documentI instanceof RegisterDocumentI) {
					RegisterDocumentI regDoc = (RegisterDocumentI)documentI;
					for ( PropertyI property: regDoc.getProperties()) {
						String name = property.getLegal().getSubdivision().getName();
						if(!StringUtils.isEmpty(name)){
							NameSet owner = new NameSet();
							owner.setAtribute("OwnerFirstName", "");
							owner.setAtribute("OwnerMiddleName", "");
							owner.setAtribute("OwnerLastName", StringCleaner.cleanString(stringCleaner, name));
							owners.add(owner);
						}
					}
				}
			}
		}
		return owners;
	}
	
	public static  double calculateMatchForCompanyOrSubdivision( Set<String> refSet, String fullRef,
			double myDoubleThreshold, DoubleMetaphone doubleMetaphone) {
		return calculateMatchForCompanyOrSubdivision(refSet, fullRef, myDoubleThreshold, true, doubleMetaphone);
	}
	
	public static  double calculateMatchForCompanyOrSubdivision( Set<String> refSet, String fullRef,
															double myDoubleThreshold, boolean requireSubdivisionClean, DoubleMetaphone doubleMetaphone) {
		
		return calculateMatchForCompanyOrSubdivision(refSet, fullRef, myDoubleThreshold, requireSubdivisionClean, false, doubleMetaphone);
	}
	public static  double calculateMatchForCompanyOrSubdivision( Set<String> refSet, String fullRef,
			double myDoubleThreshold, boolean requireSubdivisionClean, boolean dontRequireCharArraySort, DoubleMetaphone doubleMetaphone) {
		
		double bestScore = 0;
		MatchEquivalents matchEquivalents = MatchEquivalents.getInstance(10);
		for(String refToken : refSet ){
			 
			String equivRefToken = matchEquivalents.getEquivalent(refToken.toUpperCase(), requireSubdivisionClean).replaceAll("\\s", "").replaceAll("\\p{Punct}", "");
			if(fullRef.startsWith(equivRefToken) && equivRefToken.length() > MIN_COMP_LENGHT){
				bestScore = myDoubleThreshold;
			} else if(equivRefToken.startsWith(fullRef) && fullRef.length() > MIN_COMP_LENGHT) {
				bestScore = myDoubleThreshold;
			}
			bestScore = Math.max(calculateMatchForLast(
						equivRefToken, 
						fullRef, 
						myDoubleThreshold, doubleMetaphone), bestScore);
		}
		if( bestScore>=myDoubleThreshold ){
			return bestScore ;
		}
		char b[] = fullRef.toCharArray();
		if (!dontRequireCharArraySort){
			Arrays.sort(b);
		}
		fullRef = new String( b );
		
		if( b.length>0 ){
			for(String refToken : refSet ){
				char a[] = matchEquivalents.getEquivalent(refToken,requireSubdivisionClean).toCharArray();
				if( a.length>0 ){
					if (!dontRequireCharArraySort){
						Arrays.sort(a);
					}
					bestScore = Math.max(calculateMatchForLast(new String(a), fullRef, myDoubleThreshold, doubleMetaphone), bestScore);
				}
			}
		}
		return bestScore;
	}
	
	
	/**
	 * Compare the candList and famCand with the defaultMatcher and 
	 * @param ignoreMiddleOnEmpty -  if reference middle is empty accepts all middles 
	 * @return true is score grater then matchScore
	 */
	public static boolean isMatchGreaterThenScore(Set<NameI> refSet, NameI famCand, double matchScore, boolean ignoreMiddleOnEmpty){
        double maxscore = 0;
        double score 	= 0;
        GenericNameFilter usedFilter = defaultNameFilter;
        if( ignoreMiddleOnEmpty ){
        	usedFilter = defaultNameFilterIgnoreMiddleOnEmpty;
        }
		String []cand		=	{	
				famCand.getFirstName(), 
				cleanTokenOfPunctuation(famCand.getMiddleName()), 
				famCand.getLastName() };
		
		
		for ( NameI element : refSet ) {
			String ref[]={	
					element.getFirstName(), 
					cleanTokenOfPunctuation(element.getMiddleName()), 
					element.getLastName() };
			
			score=usedFilter.calculateScore(ref, cand,true);
			maxscore = Math.max(maxscore, score);
		} 
		return ( maxscore>matchScore );
	}
	
	public static boolean isMatchGreaterThenScore(NameI candidat, NameI fererence, double matchScore){
		return ( calculateScore(candidat, fererence) > matchScore );
	}
	
	public static boolean isMatchGreaterThenScore(PartyI candidat, PartyI fererence, double matchScore){
		return ( calculateScore(candidat, fererence) > matchScore );
	}
	
	public static double calculateScore(PartyI candidate, PartyI reference){
		double maxScore = 0.0;
		for(NameI cand: candidate.getNames()){
			for(NameI ref: reference.getNames()){
				maxScore = Math.max( calculateScore(cand, ref), maxScore );
			}
		}
		return maxScore;
	}
	
	public static double calculateScore(NameI candidat, NameI reference){
        double score 	= 0;
        if( candidat==null || reference==null ){
        	return defaultNameFilter.myDoubleThreshold;
		}
		String []cand	=	{	
				candidat.getFirstName(), 
				cleanTokenOfPunctuation(candidat.getMiddleName()), 
				candidat.getLastName() };
		String []ref	=	{	
				reference.getFirstName(),
				cleanTokenOfPunctuation(reference.getMiddleName()),
				reference.getLastName() };
		if(candidat.isCompany()||reference.isCompany()){
			GenericNameFilter tempNameFilter = new GenericNameFilter(DEFAULT_SEARCH_ID);
			tempNameFilter.myDoubleThreshold = 1.0d;
			tempNameFilter.setRef.add(reference);
			tempNameFilter.init();
			score	=	tempNameFilter.calculateScore(ref, cand,true);
		}else{
			score	=	defaultNameFilter.calculateScore(ref, cand,true);
		}
		return score;
	}
	 
	/**
	 * Compare the candList and famCand with the defaultMatcher and 
	 * @return true is score grater then matchScore
	 */
	public static boolean isMatchGreaterThenScoreForSubdivision(String cand1, String ref1, double matchScore){
		if(StringUtils.isEmpty(cand1) || StringUtils.isEmpty(ref1)){
			return false;
		}		
		return ( computeScoreForStrings(cand1, ref1) > matchScore );
	}
	
	/**
	 * Compare 2 strings
	 * @param cand1
	 * @param ref1
	 * @return
	 */
	public static double computeScoreForStrings(String cand1, String ref1){
		return computeScoreForStrings(cand1, ref1, false);
	}
	public static double computeScoreForStrings(String cand1, String ref1, boolean dontRequireCharArraySort){
		if(StringUtils.isEmpty(cand1) || StringUtils.isEmpty(ref1)){
			return 1.0d;
		}
		String []cand	=	{	"",	"",	cand1	};
		String []ref	=	{	"",	"",	ref1	};
		GenericNameFilter filter = new GenericNameFilter(DEFAULT_SEARCH_ID);
		filter.companyNameRef.add( cand1 );
		if (dontRequireCharArraySort){
			filter.setDontRequireCharArraySort(true);
		}
		return filter.calculateScore(cand, ref, false);
	}
	
	public static double getScore (Set<NameI> cand, Vector<NameI> ref, long searchId){
		double maxScore = 0.0d;
		GenericNameFilter filter = new GenericNameFilter(searchId);
		filter.setMatchRefInitialWithCandidatWeightFromThreshold(0.85);
		
		for (NameI refName:ref) {
			if( refName.isCompany() ){
				filter.companyNameRef.add( refName.getLastName() );
			}
		}
		for (NameI refName:ref) {
			for (NameI candName:cand) {
				maxScore = Math.max(filter.calculateScore( refName.getNameAsStringArray(),  candName.getNameAsStringArray(), true), maxScore);
			}
		}
		return maxScore;
	}
	
	
	
	public Set<NameI> getRefNames(){
		return setRef;
	}
	

	public static final String[][] subdivisions = new String[][] { { "SIERRA EXCAVATING, LLC", "LOT A, LLC" }, 
		{ "MEADOW", "MEADOW LOT 17" }, { "MEADOWS OF SUNSHINE", "SECTION 4 MEADOWS OF SUNSHINE" }, { "LOT A, LLC", "LOT A, LLC" }, { " BATTLEMENT MESA LOT HOLDINGS LLC ", " BATTLEMENT MESA LOT HOLDINGS LLC " } };
	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		
		ro.cst.tsearch.servlet.BaseServlet.REAL_PATH = "D:\\workspace2\\TS_main\\web\\";
		/*GenericNameFilter filter = new GenericNameFilter(12);
		filter.setIgnoreMiddleOnEmpty(true);
		String []ref={ "Fahim", "" ,"HAKIM"};  
		String []cand = { "FRANCIS", "", "HAKIM" };
		System.err.println(filter.calculateScore(ref, cand, true));*/
		
		GenericNameFilter filter = new GenericNameFilter(12);
		
		////////-----------------------
		
//		filter.setIgnoreMiddleOnEmpty(true);
//		filter.setUseSynonymsBothWays(true);
		
		filter.setIgnoreMiddleOnEmpty(true);
		filter.setUseArrangements(false);
		
		
		Name refNameCompany = new Name("","", "Mary L. Baker Revocable Trust");
		refNameCompany.setCompany(true);
		filter.setRef.add(refNameCompany);
		filter.companyNameRef.add("Mary L. Baker Revocable Trust");
		
//		filter.setRef.add(new Name("Amanda", "", "Grant"));
//		filter.setRef.add(new Name("Theodore", "", "Grant"));
		
		filter.init();
		
		List<NameI> candidates = new ArrayList<NameI>();
		//candidates.add(new Name("Linda", "L", "Davis"));
		//DAVIS TIMMIE L / DAVIS LINDA F
		//candidates.add(new Name("Linda", "F", "Davis"));
		
//		Name companyCand = new Name("", "", "PUBLIC");
//		companyCand.setCompany(true);
//		candidates.add(companyCand);
//		
//		candidates.add(new Name("Lydell", "", "Grant"));
//		candidates.add(new Name("Andrea", "", "Grant"));
		
		candidates.add(new Name("Mary", "", "Baker"));
		
		
		System.out.println("Using filter: " + filter.getFilterCriteria());
		
		for (NameI nameI : candidates) {
			System.out.println("Cand: [" + nameI.toString() + "] score is " + filter.getScoreForName(nameI));
		}
		
		
		ParsedResponse parsedResponse = new ParsedResponse();
		Vector grantorNameSet = parsedResponse.getGrantorNameSet();
		NameSet nameSet = new NameSet();
		nameSet.setAtribute("OwnerFirstName", "");
		nameSet.setAtribute("OwnerLastName", "PUBLIC");
		nameSet.setAtribute("OwnerMiddleName", "");
		
		grantorNameSet.add(nameSet);
		nameSet = new NameSet();
		nameSet.setAtribute("OwnerFirstName", "");
		nameSet.setAtribute("OwnerLastName", "PUBLIC");
		nameSet.setAtribute("OwnerMiddleName", "");
		
		System.out.println("--------------------------");
		
		for (Object object : grantorNameSet) {
			System.out.println("Cand: [" + nameSet.toString() + "] score is " + filter.getScoreOneRow(parsedResponse));
		}
		
		
		/*
		
		String []ref={  "Enrique" ,"","Fernandez" };
		 
		String []cand = {  "carmen" ,"e", "fernandez"};  
		System.err.println(filter.calculateScore(ref, cand, true));
		
		GenericNameFilter genericNameFilter = new GenericNameFilter(0);

		String cand1 = "";
		String ref1 = "";
		double computeScoreForStrings = 0d;
		
		for (String []  testValue : subdivisions) {
			cand1 = testValue[1];
			ref1 = testValue[0];
			String [] ref2 = {"", "", testValue[0]};
			String [] cand2 = {"", "", testValue[1]};
			computeScoreForStrings = genericNameFilter.calculateScore(ref2, cand2, true);
			
			
			System.out.println(MessageFormat.format("For candidate {0} and reference {1} the score is {2} ", cand1, ref1, computeScoreForStrings ));
			
		}
		
		*/
	}


	public boolean isEnableTrusteeCheck() {
		return enableTrusteeCheck;
	}


	public void setEnableTrusteeCheck(boolean enableTrusteeCheck) {
		this.enableTrusteeCheck = enableTrusteeCheck;
	}


	public boolean isEnableTrusteeDoctype() {
		return enableTrusteeDoctype;
	}


	public void setEnableTrusteeDoctype(boolean enableTrusteeDoctype) {
		this.enableTrusteeDoctype = enableTrusteeDoctype;
	}
	
	public static String cleanTokenOfPunctuation(String token) {
		if(token != null) {
			return token.replaceAll("[\\.,;]+", "").trim();
		}
		return "";
	}

	public PType getFilterPartyType() {
		return filterPartyType;
	}

	public void setFilterPartyType(PType filterPartyType) {
		this.filterPartyType = filterPartyType;
	}
	
}
