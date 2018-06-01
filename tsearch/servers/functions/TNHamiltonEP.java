package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class TNHamiltonEP {
	@SuppressWarnings("unchecked")
	public static void partyNamesTNHamiltonEP(ResultMap m, long searchId) throws Exception {

		String owner = (String) m.get("PropertyIdentificationSet.OwnerLastName");

		if (StringUtils.isEmpty(owner))
			return;

		if ((owner.contains("TR") || owner.contains("TRS"))
				&& owner.contains("TRUST")) {
			owner = owner.replaceAll("\\bTRS?\\b", "");
		}
		owner = owner.replaceAll("\\s+CO-(TRS)", " $1");
		owner = owner.replaceAll("&\\s+(\\w+\\s+\\w+)\n(MC CRONE)", "& $2 $1");
		owner = owner.replaceAll("(\\w+)\\s+(C/O)", "$1\n$2");
		owner = owner.replaceAll("\n(REVOC.*)", " $1");
		owner = owner.replaceAll("\\bATTY\\b", "").trim();
		owner = owner.replaceAll("(?is)\\s*&\\s*(ETAL)\\b", " $1").trim();
		owner = owner.replaceAll("\n(CO INC)", " $1");
		owner = owner.replaceAll("&\n(\\w+(?:\\s+\\w+)?)\\z", "& $1");
		owner = owner.replaceAll("\n(\\w+)\\z", " $1");
		owner = owner.replaceAll("\n", "   ");
		owner = owner.replaceAll("\\s{2,}([A-Z]+)\\s*$", " $1");

		String[] owners = owner.split("\\s{2,}");

		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		//String[] namesAux = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;

		for (int i = 0; i < owners.length; i++) {
			String ow = owners[i];
			//String own = ow;
			names = StringFormats.parseNameNashville(ow, true);
			if (ow.contains("C/O")) {
				ow = ow.replaceAll("\\bC/O\\b", "");
				names = StringFormats.parseNameDesotoRO(ow, true);
			}
			if (ow.matches("(.+)\\s+&\\s+\\w+\\s+[A-Z]{3,}") && !(ow.trim().toLowerCase()).endsWith("etal")) {
				names[4] = names[3];
				names[3] = names[5];
				names[5] = names[2];
			}
			names[0] = names[0].replaceAll("#", "");

			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, 
											NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
		
		String[] a = StringFormats.parseNameNashville(owner, true);
        m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
        m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
        m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
        m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
        m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
        m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
	}
	
	public static void taxHamiltonEP(ResultMap m,long searchId) throws Exception{
        String baseAmount = (String) m.get("TaxHistorySet.BaseAmount");
        if (baseAmount==null || "".equals(baseAmount)) {
            baseAmount = "0.00";
        }
        
        String totalDue = (String) m.get("TaxHistorySet.TotalDue");
        if (totalDue==null || "".equals(totalDue)) {
            totalDue = "0.00";
        }
        
        String amountPaid = (String) m.get("TaxHistorySet.AmountPaid");        
        if (amountPaid==null || "".equals(amountPaid)) {
            amountPaid = "0.00";
        }
        
        String stormAmount = (String) m.get("tmpStormAssesed");
        if (stormAmount==null || "".equals(stormAmount)) {
            stormAmount = "0.00";
        }
        
        String stormDue = (String) m.get("tmpStormDue");
        if (stormDue==null || "".equals(stormDue)) {
            stormDue = "0.00";
        }
        
        String stormPaid = (String) m.get("tmpStormPaid");
        if (stormPaid==null || "".equals(stormPaid)) {
            stormPaid = "0.00";
        }
        
        baseAmount = new BigDecimal (baseAmount).add(new BigDecimal (stormAmount)).toString();
        
        amountPaid = new BigDecimal (amountPaid).add(new BigDecimal (stormPaid)).toString(); //
        
        BigDecimal totalDueBD = new BigDecimal (totalDue).add(new BigDecimal (stormDue));
        totalDue = totalDueBD.toString();
        
        m.put("TaxHistorySet.BaseAmount", baseAmount);
        m.put("TaxHistorySet.TotalDue", totalDue);
        m.put("TaxHistorySet.CurrentYearDue", totalDue);
        m.put("TaxHistorySet.AmountPaid", amountPaid);
          
        String priorDelinq = GenericFunctions.sum((String) m.get("tmpPriorDue"), searchId);
        m.put("TaxHistorySet.PriorDelinquent", priorDelinq);
        
        BigDecimal delinquentAmount = new BigDecimal(priorDelinq);
               
        try {        
        	Date dueDate = DBManager.getDueDateForCity(
                InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity()
                        .getID().longValue(), InstanceManager.getManager()
                        .getCurrentInstance(searchId).getCurrentCounty().getCountyId()
                        .longValue());
        
	        Date current = new Date();
	        
	        if (current.after(dueDate)) {
	            delinquentAmount = delinquentAmount.add(totalDueBD);
	        }	        	        	        
        } catch (Exception e) {
        	e.printStackTrace();
        }
        if (delinquentAmount.compareTo(new BigDecimal(0)) < 0){
        	delinquentAmount = new BigDecimal(0);        	
        }
        m.put("TaxHistorySet.DelinquentAmount", delinquentAmount.toString());
    }
	
public static void setReceiptsTNHamiltonEP(ResultMap m,long searchId) throws Exception {
    	
    	ResultTable ths = (ResultTable) m.get("TaxHistorySet");
    	
    	if (ths == null)
    		return;
    	
    	int len = ths.getLength();
    	
    	if (len == 0)
    		return;
    	
    	String [][] body = ths.getBody();
    	BigDecimal taxPaid;
    	BigDecimal swPaid;
    	
    	for (int i=0; i<len; i++){
    		body[i][2] = StringFormats.DateYearLast(body[i][2]);
    		if (body[i][6] == null || body[i][6].length()==0){
    			taxPaid = new BigDecimal("0.0");
    		} else {
    			taxPaid = new BigDecimal(body[i][6]);
    		}
    		if (body[i][9] == null || body[i][9].length()==0){
    			swPaid = new BigDecimal("0.0");
    		} else {
    			swPaid = new BigDecimal(body[i][9]);
    		}

    		body[i][10] = taxPaid.add(swPaid).toString();
    	}
    	
    	m.put("TaxHistorySet", ths);
   }
}
