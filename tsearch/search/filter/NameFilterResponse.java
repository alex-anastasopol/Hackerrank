/*
 * Created on Jun 30, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Category;

import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.PartyI;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.propertyInformation.Family;
import ro.cst.tsearch.propertyInformation.Owner;
import ro.cst.tsearch.search.filter.matchers.name.NameMatcher;
import ro.cst.tsearch.search.tokenlist.NameTokenList;
import ro.cst.tsearch.servers.response.NameSet;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;

import static ro.cst.tsearch.utils.StringUtils.*;


/**
 * @author elmarie
 */
public class NameFilterResponse extends FilterResponse{
	
	private static final long serialVersionUID = 1232143245435432L;
	
	@SuppressWarnings("unused")
	private static final Category logger = Category.getInstance(NameFilterResponse.class.getName());
	private static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + NameFilterResponse.class.getName());
	
  	protected static final String NA_STRING ="__String not available__";

	
	protected NameMatcher nameMatcher;
	
	// flag indicating whether to try removing all MI from both reference and candidate
	protected boolean tryIgnoreMI = false; // bug #826
	protected boolean tryIgnoreMICond = false; // bug #826
	protected boolean alwaysIgnoreMI = false; // required on ILCookLA
	
	// sets flag tryIgnoreMI
	public void setTryIgnoreMI(boolean tryIgnoreMI){
		this.tryIgnoreMI = tryIgnoreMI;
	}
	
	// sets flag tryIgnoreMICond
	public void setTryIgnoreMICond(boolean tryIgnoreMICond){
		this.tryIgnoreMICond = tryIgnoreMICond;
	}

	public void setAlwaysIgnoreMI(boolean alwaysIgnoreMI){
		this.alwaysIgnoreMI = alwaysIgnoreMI;
	}
	
	public NameFilterResponse(String key, NameMatcher nm,long searchId) {
		super(key,searchId);
		this.nameMatcher = nm;
	}

	/**
     * checks whether all names have the same number of initials and returns it 
     * @param ntl
     * @return 0: no initials, 2: 2 initials, -1: 1 initial
     */
    private int computeMI(NameTokenList [] ntl){    	
    	if(ntl == null){ 
    		return 0; 
    	}
    	// compute number of initials
    	int retVal = 0;
    	int totalNames = 0;
    	for(int i=0; i<ntl.length; i++){
    		boolean emptyLast   = ntl[i].getLastNameAsString().equals("");
    		boolean emptyFirst  = ntl[i].getFirstNameAsString().equals("");
    		boolean emptyMiddle = ntl[i].getMiddleNameAsString().equals("");
    		if(!emptyLast && (!emptyFirst || !emptyMiddle)){
    			totalNames += 1;
	    		if(!emptyMiddle){
	    			retVal++;
	    		}
    		}
    	}
    	if((retVal != 0) && (retVal != totalNames)){
    	    return -1;
    	} else if(retVal == totalNames){
    		return 2;
    	} else {
    		return 0;
    	}
    }
    
    private boolean emptyFirst(NameTokenList ntl){
    	return ntl.getFirstNameAsString().trim().equals("");
    }
    
    private boolean emptyMiddle(NameTokenList ntl){
    	return ntl.getMiddleNameAsString().trim().equals("");
    }
    
    private boolean emptyLast(NameTokenList ntl){
    	return ntl.getLastNameAsString().trim().equals("");
    }
    
    private boolean emptyName(NameTokenList ntl){
    	return emptyLast(ntl) || emptyFirst(ntl);
    }    
    
    /**
     * get list of candidate names that either have or do not have middle name
     * @param candList initial list of candidates
     * @param middle with or without middle initial/name
     * @return list of candidates
     */
    private List<NameTokenList[]> getCanidates(List<NameTokenList[]> candList, boolean middle){
    	List<NameTokenList[]> newCandList = new ArrayList<NameTokenList[]>();
    	for(NameTokenList[] cand : candList){
    		NameTokenList[] newCand =  new NameTokenList[2];
    		int index = 0;
			for(NameTokenList cnd: cand){
    			if(!emptyName(cnd)){
    				if(emptyMiddle(cnd) ^ middle){
    					newCand[index] = new NameTokenList(cnd);
    					newCand[index].clearMiddleName();
    					index++;
    				}	    				
    			}
			}
    		if(index > 0){
    			if(index == 1){
    				newCand[1] = new NameTokenList(newCand[0].getLastNameAsString(),"","");
    				//newCand[1] = new NameTokenList();
    			}
    			newCandList.add(newCand);
    		}
    	}
    	return newCandList;    	
    }
    
	public BigDecimal getScoreOneRow( ParsedResponse row) {
		
		// if we are instructed to ignore MI, deactivate the tryIgnoreMI and tryIgnoreMICond
		if(alwaysIgnoreMI){
			tryIgnoreMICond = false;
			tryIgnoreMI = false;			
		}
						
		// if tryIgnoreMICond enabled, then activate tryIgnoreMI, but only for liens and judgements
		if(tryIgnoreMICond){
			tryIgnoreMI = false;
			for(int i=0; i<row.getSaleDataSetsCount(); i++){
				String docType = row.getSaleDataSet(i).getAtribute("DocumentType");
				if(!"".equals(docType)){
					if(DocumentTypes.isLienDocType(docType,searchId) || DocumentTypes.isCourtDocType(docType,searchId)){
						tryIgnoreMI = true;
						break;
					}
				}
			}
		}
		
		List<NameTokenList[]> candNames = getCandNames(row);
		
		// no candidates lit -> maximum score
		if (candNames == null){
			IndividualLogger.info("No candidate names (null list). Score = 1.00",searchId);
			return ATSDecimalNumberFormat.ONE;
		}
		
		// empty candidates lit -> maximum score
		if(candNames.size() == 0){
			IndividualLogger.info("No candidate names (empty list). Score = 1.00",searchId);
			return ATSDecimalNumberFormat.ONE;
		}

		NameTokenList[] refNames = getRefNames(); 
		
		if(alwaysIgnoreMI){
			
			// clear middle name of reference names
			for(NameTokenList ntl : refNames){
				ntl.clearMiddleName();
			}
			
			// clear middle name of candidates
			for(NameTokenList[] ntls : candNames){
				for(NameTokenList ntl : ntls){
					ntl.clearMiddleName();
				}
			}
		}
		
		BigDecimal score = matchOwnerName(candNames, refNames);
		
		if((!(score.compareTo(ATSDecimalNumberFormat.ONE) == 0)) && tryIgnoreMI){
			
			/*
			 * Determine whether candidates/references have all MI
			 * or all do not have MI
			 */
			
			// assume we have to compute using new algorithm
			boolean computeWithoutMI = true;
			// compute number of MI for reference	
			int refMICount = computeMI(refNames);
			// if not mixed, search on, maybe all have same number of MI
			if(refMICount != -1){
				// assume we don't have to compute with new algorithm
				computeWithoutMI = false;
				for(Iterator it = candNames.iterator(); it.hasNext();){
					int candMICount = computeMI((NameTokenList[])it.next());
					// if mixed, stop search, we'll do it
					if(candMICount == -1){
						computeWithoutMI = true;
						break;
					}
					// if not mixed, but different from reference, stop search, we'll do it
					if(refMICount != candMICount ){
						computeWithoutMI = true;
						break;
					}
				}
			}
			
			/*
			 * if reference and candiates either all have mi or none have mi
			 * then try removing middle initials and see what score we get
			 */
			if(computeWithoutMI){
				
				// split reference with middles and reference without middles
				List<NameTokenList> refWithMiddle = new ArrayList<NameTokenList>();
				List<NameTokenList> refWithoutMiddle = new ArrayList<NameTokenList>();
				for(NameTokenList refName : refNames){
					if(!emptyName(refName)){
						NameTokenList ntl = new NameTokenList(refName);
						ntl.clearMiddleName();						
						if(!emptyMiddle(refName)){
							refWithMiddle.add(ntl);
						}else{
							refWithoutMiddle.add(ntl);
						}
					}
				}
				
				// compare reference with middles with candidates without middles
				if(refWithMiddle.size() != 0){
					// reconstruct NameTokenList[2]
					NameTokenList[] newRefNames = new NameTokenList[refWithMiddle.size()>1?refWithMiddle.size():2];
					for(int i=0; i<refWithMiddle.size(); i++){
						newRefNames[i] = refWithMiddle.get(i); 
					}
					if(newRefNames[1] == null){
						//newRefNames[1] = new NameTokenList(newRefNames[0].getLastNameAsString(),"","");
						newRefNames[1] = new NameTokenList();
					}
					// get only candidates without a middle name
					List<NameTokenList[]> candWithoutMiddle = getCanidates(candNames, false);
					// compare them
					if(candWithoutMiddle.size() != 0){
						IndividualLogger.info("Compare reference with MI with candidates without MI:",searchId);
						BigDecimal newScore = matchOwnerName(candWithoutMiddle, newRefNames);  
						if(newScore.compareTo(ATSDecimalNumberFormat.ONE) == 0){
					        IndividualLogger.info( "Final Score = " + newScore,searchId );
					        IndividualLogger.info(" ",searchId);						
							return ATSDecimalNumberFormat.ONE;
						}					
						score = score.max(newScore);
					}
				}
				
				if(refWithoutMiddle.size() != 0){
					// reconstruct NameTokenList[2]
					NameTokenList[] newRefNames = new NameTokenList[refWithoutMiddle.size()>1?refWithoutMiddle.size():2];
					for(int i=0; i<refWithoutMiddle.size(); i++){
						newRefNames[i] = refWithoutMiddle.get(i); 
					}
					if(newRefNames[1] == null){
						//newRefNames[1] = new NameTokenList(newRefNames[0].getLastNameAsString(),"","");
						newRefNames[1] = new NameTokenList();
					}					
					// get only candidates with a middle
					List<NameTokenList[]> candWithMiddle = getCanidates(candNames, true);
					// compare them
					if(candWithMiddle.size() != 0){
						IndividualLogger.info("Compare reference without MI with candidates with MI:",searchId);
						BigDecimal newScore = matchOwnerName(candWithMiddle, newRefNames);
						if(newScore.compareTo(ATSDecimalNumberFormat.ONE) == 0){
					        IndividualLogger.info( "Final Score = " + newScore ,searchId);
					        IndividualLogger.info(" ",searchId);						
							return ATSDecimalNumberFormat.ONE;
						}					
						score = score.max(newScore);	
					}
				}
			}else{
				IndividualLogger.info("Did NOT try removing MI.",searchId);
			}
		}
        
        IndividualLogger.info( "Final Score = " + score ,searchId);
        IndividualLogger.info(" ",searchId);
        return score;
	}

	protected NameTokenList[] getRefNames() {
		return getOwners(sa, saKey);
	}

	protected List getCandNames(ParsedResponse row) {
		return null;
	}

	protected BigDecimal matchOwnerName(List candNames, NameTokenList[] owners){
		return matchName(candNames, owners);
	}

	
	protected BigDecimal matchName(List candNames, NameTokenList[] owners) {
		BigDecimal rez = ATSDecimalNumberFormat.ZERO;
		for (Iterator iter = candNames.iterator(); iter.hasNext();) {
			NameTokenList[] candName  = (NameTokenList[]) iter.next();
			rez = rez.max(matchName(candName, owners));
			if(rez.compareTo(ATSDecimalNumberFormat.ONE) == 0){
				return ATSDecimalNumberFormat.ONE;
			}
		}
		return rez;
	}



	protected BigDecimal matchName(NameTokenList[] candName, NameTokenList[] owners) {
	
		if (matchInvalidPattern(candName)){
			return ATSDecimalNumberFormat.ZERO;
		}

		BigDecimal score = nameMatcher.getScore(owners[0], owners[1], candName[0], candName[1]);
		loggerDetails.debug("MATCH  ref = [" + owners[0].getString() + " & " + owners[1].getString() + "] vs cand = [" + candName[0].getString() + " & " + candName[1].getString() + "]");
		loggerDetails.debug("SCORE = " + score);

        IndividualLogger.info("MATCH  ref = [" + owners[0].getString() + " & " + owners[1].getString() + "] vs cand = [" + candName[0].getString() + " & " + candName[1].getString() + "]",searchId);
        IndividualLogger.info("SCORE = " + score,searchId);
        
        return score;
	}	
	
	protected static NameTokenList[] getOwners(SearchAttributes sa1, String key) {
		NameTokenList[] names = null ;
		
		if (key.equals(SearchAttributes.NO_KEY) || "".equals( key )) {
			key = SearchAttributes.OWNER_OBJECT;
		}
		PartyI party=null;
		if(key.equals(SearchAttributes.OWNER_OBJECT))
		   party = sa1.getOwners();
	    if (key.equals(SearchAttributes.BUYER_OBJECT))
	    	party=sa1.getBuyers();
			
		if( party==null || party.size()==0 ){
			names = new NameTokenList[]{new NameTokenList("","",""), new NameTokenList("","","")};  
		}
		else{
			List<NameTokenList> list = new ArrayList<NameTokenList>();
			for( NameI name: party.getNames() ){
				NameTokenList nameToken  = new NameTokenList(name.getLastName(),name.getFirstName(),name.getMiddleName());
				list.add( nameToken );
			}
			if( list.size()<2 ){
				list .add(new NameTokenList("","",""));
			}
			names = list.toArray( new NameTokenList[list.size()]);
		}
	
		
		return names;
	}


//	////////////////////////////////////////////////////////////

	protected boolean matchInvalidPattern(NameTokenList[] candName) {
		  boolean rez = false;
		  for (int i = 0; i < candName.length; i++) {
			  rez = rez || matchInvalidPattern(candName[i]); 
		  }
		  return rez;
	}

	protected boolean matchInvalidPattern(NameTokenList candName) {
		  boolean rez = matchInvalidPattern(candName.toString());
		  loggerDetails.debug("Match Invalid pattern name"  + candName + " = " +rez ); 
		  return rez;
	  }

	public List nameSetVector2NameTokenListList(Vector src){
		List<NameTokenList[]> l = new ArrayList<NameTokenList[]>();
		for (Iterator iter = src.iterator(); iter.hasNext();) {
			NameSet ns = (NameSet) iter.next();
			NameTokenList[] rez = new NameTokenList[2];
			rez[0] = new NameTokenList (
							ns.getAtribute("OwnerLastName"),
							ns.getAtribute("OwnerFirstName"),
							ns.getAtribute("OwnerMiddleName")
							);
			rez[1] = new NameTokenList (
							ns.getAtribute("SpouseLastName"),
							ns.getAtribute("SpouseFirstName"),
							ns.getAtribute("SpouseMiddleName")
							);
			l.add(rez);
		}
		return l;
	}

	public static Vector nameTokenListList2NameSetVector(List ntll){
		
		Vector<NameSet>  rez = new Vector<NameSet>();
		
		for (Iterator iter = ntll.iterator(); iter.hasNext();) {
			NameTokenList[] fam = (NameTokenList[]) iter.next();
			NameSet ns = new NameSet();
			ns.setAtribute("OwnerLastName", fam[0].getLastNameAsString());
			ns.setAtribute("OwnerFirstName", fam[0].getFirstNameAsString());
			ns.setAtribute("OwnerMiddleName", fam[0].getMiddleNameAsString());
			ns.setAtribute("SpouseLastName", fam[1].getLastNameAsString());
			ns.setAtribute("SpouseFirstName", fam[1].getFirstNameAsString());
			ns.setAtribute("SpouseMiddleName", fam[1].getMiddleNameAsString());
			
			rez.add(ns);
		}
		
		return rez;
	}


	
	public void setNameMatcher(NameMatcher nm)
	{
		this.nameMatcher = nm;
	}
	
	/*
	private String ntlArray2String(List l){
		List<String> rez = new ArrayList<String>();
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			NameTokenList[] a = (NameTokenList[]) iter.next();
			rez.add(a[0] + " & " + a[1]);
		}
		return StringUtils.join(rez,"; ");
		
	}
	*/
	
	@Override
    public String getFilterName(){
		Search src = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		
		boolean ignoreMiddleName = false;
		if ( saKey.equals(SearchAttributes.OWNER_OBJECT) ){
			if(searchId > 0 && src.getSa().isIgnoreOwnerMiddleName()){
				ignoreMiddleName = true;
			}
		}
		if ( saKey.equals(SearchAttributes.BUYER_OBJECT) ){
			if(searchId > 0 && src.getSa().isIgnoreBuyerMiddleName()){
				ignoreMiddleName = true;
			}
		}
		
    	return "Filter " + (ignoreMiddleName?"(ignoring middle) ":"") + "by Name";
    }
	
	@Override
	public String getFilterCriteria(){
		Search src = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		StringBuilder sb = new StringBuilder();
		
		NameTokenList [] refNames = getRefNames();
		String prevLast = "";
		
		for(NameTokenList ntl: refNames){

			String first = ntl.getFirstNameAsString();
			String last = ntl.getLastNameAsString();
			String middle = ntl.getMiddleNameAsString();
			
			boolean emptyFirst = isEmpty(first);
			boolean emptyMiddle = isEmpty(middle); 
			boolean emptyLast = isEmpty(last);

			// skip empty sets
			if(emptyLast && emptyMiddle && emptyFirst){
				continue;
			}
			
			// skip wife only last name and empty names
			if((emptyLast || last.equals(prevLast))  && emptyFirst && emptyMiddle){
				continue;
			}
			
			if(sb.length() != 0){
				sb.append("; ");
			}
			String crt = "";
			if(!emptyLast){
				crt += last + ", "; 				
			}
			if(!emptyFirst){
				crt += first + " "; 
			}
			if(!emptyMiddle){
				crt += middle + " "; 
			}
			crt = crt.trim();
			crt = crt.replaceFirst(",$", "");
			sb.append(crt);
			prevLast = last;
		}
		boolean ignoreMiddleName = false;
		if ( saKey.equals(SearchAttributes.OWNER_OBJECT) ){
			if(searchId > 0 && src.getSa().isIgnoreOwnerMiddleName()){
				ignoreMiddleName = true;
			}
		}
		if ( saKey.equals(SearchAttributes.BUYER_OBJECT) ){
			if(searchId > 0 && src.getSa().isIgnoreBuyerMiddleName()){
				ignoreMiddleName = true;
			}
		}
		return "Names='" + sb.toString() + "'" + (ignoreMiddleName?"(ignoring middle name) ":"");
    }
	
}
