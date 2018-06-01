package ro.cst.tsearch.search.filter;

import static ro.cst.tsearch.bean.SearchAttributes.LD_LOTNO;
import static ro.cst.tsearch.bean.SearchAttributes.LD_SUBDIV_NAME;
import static ro.cst.tsearch.bean.SearchAttributes.LD_SUBDIV_PHASE;
import static ro.cst.tsearch.bean.SearchAttributes.LD_SUBDIV_SEC;
import static ro.cst.tsearch.utils.StringUtils.isEmpty;

import java.math.BigDecimal;

import org.apache.log4j.Category;

import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.filter.matchers.subdiv.SubdivMatcher;
import ro.cst.tsearch.search.validator.RegisterDocsValidator;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringCleaner;

public class SubdivisionNameFilterResponse extends FilterResponse {

    protected static final Category logger = Category.getInstance(SubdivisionNameFilterResponse.class.getName());
    protected static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + SubdivisionNameFilterResponse.class.getName());
    
    SubdivMatcher  subdivMatcher = null;//

    public SubdivisionNameFilterResponse(long searchId){
        super(searchId);
        subdivMatcher= new SubdivMatcher(searchId);
        threshold = new BigDecimal("0.5");
    }

    public BigDecimal getScoreOneRow(ParsedResponse row) {
//        PropertyIdentificationSet pisCand = getCandidates(row);
        PropertyIdentificationSet pisCand = row.getPropertyIdentificationSet(0);
        
        pisCand.setAtribute("SubdivisionName", StringCleaner.cleanString( stringCleanerId, pisCand.getAtribute("SubdivisionName")));
        
        PropertyIdentificationSet pisRef = getReference();

        BigDecimal score = subdivMatcher.getScore(pisRef,pisCand); 
        if (logger.isDebugEnabled())
            logger.debug(" score match subdiv " + getSubdivToString(pisCand) + " is valid = " + score);
        IndividualLogger.info( " score match subdiv " + getSubdivToString(pisCand) + " is valid = " + score ,searchId);
        return score;
        
    }


    protected PropertyIdentificationSet getCandidates(ParsedResponse row){
        
        //se pare ca denumirile de subdivizie se gasesc doar la grantee
        String candSubdiv = row.getSaleDataSet(0).getAtribute("Grantee");
        
        candSubdiv = StringCleaner.cleanString( stringCleanerId, candSubdiv );
        
        PropertyIdentificationSet pisCand =  new PropertyIdentificationSet();
        
        pisCand.setAtribute("SubdivisionLotNumber",StringFormats.LotNashvilleAO(candSubdiv));
        pisCand.setAtribute("SubdivisionName",StringFormats.SubdivisionNashvilleAO(candSubdiv));
        pisCand.setAtribute("SubdivisionSection",StringFormats.SectionNashvilleAO(candSubdiv));
        pisCand.setAtribute("SubdivisionPhase",StringFormats.PhaseNashvilleAO(candSubdiv));
        
        return pisCand;
    }
    
    protected PropertyIdentificationSet getReference(){
        return RegisterDocsValidator.FillPisFromSa4Register(sa);
    }
        

    
    public void setSubdivMatcher(SubdivMatcher matcher) {
        subdivMatcher = matcher;
    }

    public SubdivMatcher getSubdivMatcher() {
        return subdivMatcher ;
    }
    
    public String getSubdivToString(PropertyIdentificationSet pis){
        String s ="";
        s += "(" + pis.getAtribute("SubdivisionLotNumber") + ")";
        s += "(" + pis.getAtribute("SubdivisionName") + ")";
        s += "(" + pis.getAtribute("SubdivisionSection") + ")";
        s += "(" + pis.getAtribute("SubdivisionPhase") + ")";
        return s;

    }
    
    @Override
    public String getFilterName(){
    	return "Filter by Subdivision, Lot, Section, Phase";
    }
    
	@Override
	public String getFilterCriteria(){
		
		String sub = sa.getAtribute(LD_SUBDIV_NAME);
		String lot = sa.getAtribute(LD_LOTNO); 		
		String sec = sa.getAtribute(LD_SUBDIV_SEC);
		String ph  = sa.getAtribute(LD_SUBDIV_PHASE);
		
		String retVal = "";
		if(!isEmpty(sub)){
			retVal += "Sub: " + sub + " ";
		}
		if(!isEmpty(lot)){
			retVal += "Lot: " + lot + " ";
		}
		if(!isEmpty(sec)){
			retVal += "Sec: " + sec + " ";
		}
		if(!isEmpty(ph)){
			retVal += "Ph: " + ph + " ";
		}
		
		return "Legal='" + retVal.trim() + "'";
	}
    
}
