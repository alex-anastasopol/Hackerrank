package ro.cst.tsearch.search.filter.newfilters.name;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.NameSet;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringCleaner;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.search.DocumentsManagerI;

/**
 * With this filter should pass only documents that contains exact the reference names as form and number
 * e.g.  refNames SERAFIN RODRIQUEZ / MARIA RODRIQUEZ, then should pass only documents that contains: (exactly SERAFIN RODRIQUEZ / MARIA RODRIQUEZ) + other names
 * B 4555
 * 
 * @author mihaib
 */
public class PerfectlyMatchNamesFilter extends GenericNameFilter{

	private static final long serialVersionUID = 346468834269546094L;
	
	private static final PerfectlyMatchNamesFilter defaultNameFilter = new PerfectlyMatchNamesFilter(DEFAULT_SEARCH_ID);
	
	static {
		defaultNameFilter.myDoubleThreshold =1.0d;
	}
	
	

	public PerfectlyMatchNamesFilter(long searchId) {
		super("", searchId);
	}
	
	public PerfectlyMatchNamesFilter(String key, long searchId, boolean useSubdivisionName, TSServerInfoModule module, boolean ignoreSuffix) {
		super(key, searchId, useSubdivisionName, module, ignoreSuffix);
		init();
	}
	
	
	public PerfectlyMatchNamesFilter(String key, long searchId) {
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
        String []cand=	{ famCand.getFirstName(), famCand.getMiddleName(), famCand.getLastName() };
	
        
        if(!StringUtils.isEmpty(famCand.getLastName())&& StringUtils.isEmpty(famCand.getFirstName()) && StringUtils.isEmpty(famCand.getMiddleName()) ){
        	for(NameI n:refList){
        		if(n.getLastName().toUpperCase().equals(famCand.getLastName().toUpperCase())){
        			return true;
        		}
        	}
        }
        
		for ( NameI element : refList ) {
			String ref[]={ element.getFirstName(), element.getMiddleName(), element.getLastName() };
			score=defaultNameFilter.calculateScore( ref, cand, true );
			Result resSuffix = defaultNameFilter.calculateMatchForFirstOrMiddle( element.getSufix().toUpperCase(),  famCand.getSufix().toUpperCase());
			
			if(resSuffix.score < matchScore){
				score *= 0.9;
			}
			
			maxscore = Math.max(maxscore, score);
		}
		
		
		
		return ( maxscore > matchScore );
	}
	
	public void init(){
		
		if(useSubdivisionNameAsReference){
			String subdivName = sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
			if(!StringUtils.isEmpty(subdivName)){
				setRef.add(new Name("","", StringCleaner.cleanString(stringCleaner, subdivName )));
			}
		}
		else{
			if ( saKey.equals(SearchAttributes.BUYER_OBJECT) ){
				setRef=sa.getBuyers().getNames();
			}
			else if( saKey.equals(SearchAttributes.OWNER_OBJECT) ){
				setRef = sa.getOwners().getNames();
			}
			else if ( saKey.equals(SearchAttributes.GB_MANAGER_OBJECT) )
			  {
				DocumentsManagerI documentsManager= InstanceManager.getManager().getCurrentInstance(
						searchId).getCrtSearchContext().getDocManager();
			    try{
					documentsManager.getAccess();
					GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

						if("grantor".equals(module.getTypeSearchGB()))
						  setRef.addAll(gbm.getNamesForSearch(module.getIndexInGB(), searchId));
						else
						  setRef.addAll(gbm.getNamesForBrokenChain(module.getIndexInGB(), searchId));	
					  }
				
				    finally{
						  documentsManager.releaseAccess();
					    }
			}
			else{
				System.err.println(">>>>>>>>> Incorect PerfectlyMatchNamesFilter use >>>>>>>>>>");
			}
		}
		if( setRef!=null && setRef.size()>0){
			for(NameI name : setRef){
				if( StringUtils.isEmpty(name.getFirstName()) && StringUtils.isEmpty(name.getMiddleName()) && !StringUtils.isEmpty(name.getLastName()) ){
					companyNameRef.add( name.getLastName() );
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
	
	@SuppressWarnings("unchecked")
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
		return owners;
	}
	
	private void clear(String []cand){
		for(int j=0;j<cand.length;j++ ){
			for(int k=0;k<suffixList.length;k++){
				cand[j]= cand[j].replaceAll("(?i)\\b"+suffixList[k]+"\\b", "").trim();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		if ( setRef==null || setRef.size()==0 ){
			if(markIfCandidatesAreEmpty) {
				return ATSDecimalNumberFormat.NA;
			} else {
				return new BigDecimal( myDoubleThreshold );
			}
		}

		double maxScore = 0.0 ;
		
		String cand[]	= new String[3] ;
		Vector<NameSet> candGrantors = null ;
		Vector<NameSet> candGrantees = null ;
		
		if( useSubdivisionNameAsCandidat ){
			candGrantees = new Vector<NameSet>();
			candGrantors = getSubdivisionNames( row, stringCleaner );
		}
		else {
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
		}
		boolean matchesGrantor = false;
		boolean matchesGrantee = false;
		List<String> bodyGtor = new ArrayList<String>();
		List<String> bodyGtee = new ArrayList<String>();
		
		for (NameI element:setRef) { 
			String[]  ref = { element.getFirstName(), element.getMiddleName(), element.getLastName() };
			String resultGtor = "", resultGtee = "";
			for (NameSet grantor : candGrantors) {
				
				String candFirst	= grantor.getAtribute("OwnerFirstName").trim();
				String candLast		= grantor.getAtribute("OwnerLastName").trim();
				String candMiddLe	= grantor.getAtribute("OwnerMiddleName").trim();
				
				String name			= candFirst + " " + candMiddLe + " " + candLast;
				//sometimes we receive bad information from parser
				if( StringUtils.isEmpty(name) || ( StringUtils.isEmpty(candFirst) && StringUtils.isEmpty(candMiddLe) && candLast.length()<=3) ){
					continue;
				}
				//we do not trust the parser tokeneizer
				cand = name.split("[ ,-]+");
				if(ignoreSufix){
					clear(cand);
				}
				if (cand.length > 3) {
					cand = removeEmptyStringsCand( cand );
				}
				
				Result resFirst = calculateMatchForFirstOrMiddle(ref[0].toUpperCase(), candFirst.toUpperCase());
				double firstScore  	= resFirst.score;
				
				double lastScore = calculateMatchForLast(ref[2].toUpperCase(), candLast.toUpperCase());;
				//double lastScore  	= resLast.score;
				
				if (firstScore == 1.0d && lastScore == 1.0d){
					resultGtor += "1";
				} else {
					resultGtor += "0";
				}

			}
			bodyGtor.add(resultGtor);

			for (String elementGtor : bodyGtor){
				if (elementGtor.matches("\\d*1\\d*")){
					matchesGrantor = true;
				} else {
					matchesGrantor = false;
					break;
				}
			}
		
		for (NameSet grantee : candGrantees) {
			
			String candFirst	= grantee.getAtribute("OwnerFirstName").trim();
			String candLast		= grantee.getAtribute("OwnerLastName").trim();
			String candMiddLe	= grantee.getAtribute("OwnerMiddleName").trim();
			
			String name			= candFirst + " " + candMiddLe + " " + candLast;
			//sometimes we receive bad information from parser
			if( StringUtils.isEmpty(name) || ( StringUtils.isEmpty(candFirst) && StringUtils.isEmpty(candMiddLe) && candLast.length()<=3) ){
				continue;
			}
			//we do not trust the parser tokeneizer
			cand = name.split("[ ,-]+");
			if(ignoreSufix){
				clear(cand);
			}
			if (cand.length > 3) {
				cand = removeEmptyStringsCand( cand );
			}
			
			Result resFirst = calculateMatchForFirstOrMiddle(ref[0].toUpperCase(), candFirst.toUpperCase());
			double firstScore  	= resFirst.score;
			
			double lastScore = calculateMatchForLast(ref[2].toUpperCase(), candLast.toUpperCase());;
			//double lastScore  	= resLast.score;
			
			if (firstScore == 1.0d && lastScore == 1.0d){
				resultGtee += "1";
			} else {
				resultGtee += "0";
			}

		}
		bodyGtee.add(resultGtee);

		for (String elementGtee : bodyGtee){
			if (elementGtee.matches("\\d*1\\d*")){
				matchesGrantee = true;
			} else {
				matchesGrantee = false;
				break;
			}
		}
	}
		
		
		if (matchesGrantor || matchesGrantee){
			maxScore = 1.0d;
		}
		
		return new BigDecimal(maxScore);
	}
	
	@Override
    public String getFilterName(){
		if(useSubdivisionNameAsCandidat){
			return "Filter Exactly by Subdiv Name";
		}
    	return "Filter Exactly by Name";
    }
	
	@Override
	public String getFilterCriteria(){
		if(useSubdivisionNameAsCandidat){
			return "Subdiv Name '"+ getReferenceNameString() + "'";
		}
    	return "Perfectly Match Name='" + getReferenceNameString() + "' (ignoring middle name):";
    }
	
	@Override
	public String getReferenceNameString() {
		StringBuilder nameBuff= new StringBuilder();
		for ( NameI element : setRef ) {
			nameBuff.append( element.getFullName() );
			nameBuff.append("/");
		}
		return nameBuff.toString();
	}

}
