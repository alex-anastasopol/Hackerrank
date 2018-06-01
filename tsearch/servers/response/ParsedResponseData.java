package ro.cst.tsearch.servers.response;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import org.w3c.dom.*;
import org.apache.log4j.Category;
import ro.cst.tsearch.search.tokenlist.AddressTokenList;

public class ParsedResponseData implements Serializable {

    static final long serialVersionUID = 10000000;

    private static final Category logger = Category.getInstance(ParsedResponseData.class.getName());

    protected PropertyAppraisalSet mPAS = new PropertyAppraisalSet();

    protected OwnerDetailsSet mODS = null;

    protected Vector mTHS = new Vector();
    
    protected Vector mTIS = new Vector();
    
    protected Vector mTRS = new Vector();
    
    protected Vector mPYDS = new Vector();
    
    protected Vector mBAS = new Vector();
    
    protected Vector mBMS = new Vector();
    
    protected Vector mSAS = new Vector();

    protected Vector mPIS = new Vector();
    
    protected Vector mPNS = new Vector();
    
    protected Vector mCDIS = new Vector();

    protected Vector mSDS = new Vector(5, 1);

    protected OtherInformationSet mOIS = new OtherInformationSet();
    
    protected Vector mGrantor = new Vector();

    protected Vector mGrantee = new Vector();

    protected Vector mCrossRef = new Vector();

    protected AddressTokenList atl = null;  // not actually used. remained here so that searches can be de-serialized
    
    protected String addressString = null; 

    ////////////////////////////////////////////////////////////////
    public Map infSets = new HashMap();
    {
        infSets.put("PropertyAppraisalSet", mPAS);
        infSets.put("OtherInformationSet", mOIS);
    }

    public Map infVectorSets = new HashMap();
    {
        infVectorSets.put("PropertyIdentificationSet", mPIS);
        infVectorSets.put("PartyNameSet", mPNS);
        infVectorSets.put("CourtDocumentIdentificationSet", mCDIS);
        infVectorSets.put("SaleDataSet", mSDS);
        infVectorSets.put("GrantorSet", mGrantor);
        infVectorSets.put("GranteeSet", mGrantee);
        infVectorSets.put("CrossRefSet", mCrossRef);
        infVectorSets.put("TaxHistorySet", mTHS);
        infVectorSets.put("TaxInstallmentSet", mTIS);
        infVectorSets.put("TaxRedemptionSet", mTRS);
        infVectorSets.put("PriorYearsDelinquentSet", mPYDS);
        infVectorSets.put("BondsAndAssessmentsSet", mBAS);
        infVectorSets.put("BoardMemberSet", mBMS);
        infVectorSets.put("SpecialAssessmentSet", mSAS);
    }

    public void setCrossRefSet(Vector v) {
        mCrossRef=v;
        infVectorSets.put("CrossRefSet", mCrossRef);
    }
    
    public InfSet createInfSet(String s) throws Exception {
        Class c = Class.forName("ro.cst.tsearch.servers.response." + s);
        return (InfSet) c.newInstance();
    }

    public static boolean vectorCompareOrderInsensitive(Vector v1, Vector v2) {
        if (v1.size() != v2.size())
            return false;
        int n = v1.size();
        boolean[] idx = new boolean[n];
        for (int i = 0; i < n; i++) {
            Object o1 = v1.get(i);
            boolean gast = false;
            for (int j = 0; j < n; j++) {
                if (idx[j])
                    continue;
                if (o1.equals(v2.get(j))) {
                    gast = true;
                    idx[j] = true;
                    break;
                }
            }
            if (!gast)
                return false;
        }
        return true;
    }

    public boolean equals(Object o) {
        if (!(o instanceof ParsedResponseData))
            return false;
        ParsedResponseData p = (ParsedResponseData) o;
        if (!infSets.equals(p.infSets))
            return false;
        Iterator it = infVectorSets.keySet().iterator();
        while (it.hasNext()) {
            String k = (String) it.next();
            if (!vectorCompareOrderInsensitive((Vector) infVectorSets.get(k),
                    (Vector) p.infVectorSets.get(k)))
                return false;
        }
        return true;
    }

    public static void main(String args[]) {
        Vector v1 = new Vector(), v2 = new Vector();
        Integer i1 = new Integer(1), i2 = new Integer(2);
        v1.add(i1);
        v1.add(i1);
        v1.add(i2);
        v2.add(i2);
        v2.add(i1);
        v2.add(i1);
        logger.info(" = " + vectorCompareOrderInsensitive(v1, v2));
    }
    
    /**
     * This function is only used, in conjunction with setAddressString on some of the sites
     * For the oher sites, the address string is composed from PropertyIdentificationSet fields "StreetNo" and "StretName"
     * @return address string of the current response
     */
    public String getAddressString() {
        return addressString;
    }
    
    /**
     * This function is only used, in conjunction with getAddressString on some of the sites
     * For the oher sites, the address string is composed from PropertyIdentificationSet fields "StreetNo" and "StretName"
     * @param address address string of the current response
     */
    public void setAddressString(String address) {
        addressString = address;
    }
    
    /**
     * display infsets, each on a separate line
     * @param all display all infvector set contents (false - display only first element of vector)
     * @param html insert &lt;br&gt; after each line if true
     * @return
     */
    public String display(boolean all, boolean html){
    	
    	StringBuilder sb = new StringBuilder();
    	String eol = html ? "<br/>" : "\n";
    	
    	// display standalone infsets
    	for(Object keyObj : infSets.keySet()){
    		Object valObj = infSets.get(keyObj);    		
    		if((keyObj instanceof String) && (valObj instanceof InfSet)){
    			String name = (String)keyObj;
    			InfSet infSet = (InfSet)valObj;
    			String val = infSet.display();
    			if(!"".equals(val)){
    				sb.append(name); sb.append(" = "); sb.append(val); sb.append(eol);    				
    			}    			
    		}
    	}
    	
    	//display inf vector sets - only first item of each
    	for(Object keyObj : infVectorSets.keySet()){

    		Object valObj = infVectorSets.get(keyObj);    		
    		if((keyObj instanceof String) && (valObj instanceof Vector)){
    			String name = (String)keyObj;
    			Vector infVectorSet = (Vector)valObj;
    			if(all){
	    			for(int i=0; i<infVectorSet.size(); i++ ){	    				
	    				String val = ((InfSet)infVectorSet.get(i)).display();
	    				if(!"".equals(val)){
	    					sb.append(name); sb.append(" ("+i+")"); sb.append(" = "); sb.append(val); sb.append(eol);
	    				}    				
	    			}       	
    			} else {
	    			if(infVectorSet.size() > 0){	    				
	    				String val = ((InfSet)infVectorSet.get(0)).display();
	    				if(!"".equals(val)){
	    					sb.append(name); sb.append(" = "); sb.append(val); sb.append(eol);
	    				}    				
	    			}       	    				
    			}
    		}
    	}
    	if(sb.length() == 0){
    		sb.append(eol);
    	}
    	return sb.toString();
    }

}