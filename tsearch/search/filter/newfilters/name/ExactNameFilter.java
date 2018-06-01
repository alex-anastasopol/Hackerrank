package ro.cst.tsearch.search.filter.newfilters.name;

import java.util.Set;

import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.name.NameI;

/**
 * @author Cristi Stochina
 */
public class ExactNameFilter extends GenericNameFilter{

	private static final ExactNameFilter defaultNameFilter = new ExactNameFilter(DEFAULT_SEARCH_ID);
	static {
		defaultNameFilter.myDoubleThreshold =1.0d;
	}
	
	private static final long serialVersionUID = 346468834269546094L;

	public ExactNameFilter(long searchId) {
		super("", searchId);
	}
	
	public ExactNameFilter(String key, long searchId,boolean useSubdivisionName,TSServerInfoModule module, boolean ignoreSuffix) {
		super(key, searchId, useSubdivisionName, module, ignoreSuffix);
		
	}
	
	public ExactNameFilter(String key, long searchId) {
		super(key, searchId);
	}
	
	@Override
	protected Result calculateMatchForFirstOrMiddle(String refToken, String candToken) {
		Result a = new Result();
		a.candInitialToWordReferenceApplied = false;
		if( refToken.trim().equals( candToken.trim() ) ){
			a.score = 1.0d;
		}
		else{
			a.score = 0.0d;
		}
		return a;
	}
	
	@Override
	protected  double calculateMatchForLast( String refToken, String candToken) {
		if( refToken.trim().equals( candToken.trim() ) ){
			return 1.0d;
		}
		else{
			return 0.0d;
		}
	}
	
	/**
	 * Compare the candList and famCand with the defaultMatcher and 
	 * @return true is score grater then matchScore
	 */
	public static boolean isMatchGreaterThenScore(Set<NameI> refList, NameI famCand, double matchScore){
        double maxscore = 0;
        double score = 0;
        
        if ( refList==null || refList.size()==0 )return false;	
        String []cand=	{ 
        		famCand.getFirstName(), 
        		cleanTokenOfPunctuation(famCand.getMiddleName()), 
        		famCand.getLastName() };
	
        
        if(!StringUtils.isEmpty(famCand.getLastName()) && 
        		StringUtils.isEmpty(famCand.getFirstName()) && 
        		StringUtils.isEmpty(cleanTokenOfPunctuation(famCand.getMiddleName())) ){
        	for(NameI n:refList){
        		if(n.getLastName().toUpperCase().equals(famCand.getLastName().toUpperCase())){
        			return true;
        		}
        	}
        }
        
		for ( NameI element : refList ) {
			String ref[]={ 
					element.getFirstName(), 
					cleanTokenOfPunctuation(element.getMiddleName()), 
					element.getLastName() };
			score=defaultNameFilter.calculateScore( ref, cand, true );
			Result resSuffix = defaultNameFilter.calculateMatchForFirstOrMiddle(
					cleanTokenOfPunctuation(element.getSufix().toUpperCase()),
					cleanTokenOfPunctuation(famCand.getSufix().toUpperCase())
					);
			
			if(resSuffix.score < matchScore){
				score *= 0.9;
			}
			
			maxscore = Math.max(maxscore, score);
		}
		
		
		
		return ( maxscore > matchScore );
	}
	
}
