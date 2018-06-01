package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;

import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;

public class TNShelbyEP {
    public static void taxShelbyEP(ResultMap m,long searchId) throws Exception {//city
    	String totalDueFromM = (String) m.get("tmpTotalDue");
        String tmpCurrentYearDue = GenericFunctions.sum(totalDueFromM.replaceAll("(.*?)\\+.*", "$1"),searchId);// TOTAL DUE CURRENT YEAR
        String tmpDelinquentAmt = totalDueFromM.indexOf("+") != -1 ? GenericFunctions.sum(totalDueFromM.replaceAll(".*?\\+(.*)", "$1"),searchId) : "0.00";
        String baseAmount = (String) m.get("TaxHistorySet.BaseAmount"); // Base
        String tmpPeanalty = (String) m.get("tmpPenalty");
        String tmpOtherChargers = (String) m.get("tmpOtherCharges");
        
        BigDecimal penalty = new BigDecimal(tmpPeanalty).add(new BigDecimal(tmpOtherChargers));        
        BigDecimal amountPaid = new BigDecimal(baseAmount).add(penalty).subtract(new BigDecimal(tmpCurrentYearDue));
        if (amountPaid.compareTo(new BigDecimal(0)) < 0){
        	amountPaid = new BigDecimal(0);
        }
      
        m.put("TaxHistorySet.AmountPaid", amountPaid.toString());
        m.put("TaxHistorySet.PenaltyCurrentYear", penalty.toString());
        m.put("TaxHistorySet.PriorDelinquent", tmpDelinquentAmt);
        m.put("TaxHistorySet.CurrentYearDue", tmpCurrentYearDue);
        m.put("TaxHistorySet.TotalDue", tmpCurrentYearDue);
    }

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "YA");
		
		TableColumn[] cols = row.getColumns();
		if(cols.length == 3) {
			resultMap.put("PropertyIdentificationSet.ParcelID",cols[0].toPlainTextString().trim());
			resultMap.put("tmpOwnerFullName",cols[1].toPlainTextString().trim());
			String[] address = StringFormats.parseAddressShelbyAO(cols[2].toPlainTextString().trim());
			resultMap.put("PropertyIdentificationSet.StreetNo", address[0]);
			resultMap.put("PropertyIdentificationSet.StreetName", address[1]);
			try {
				GenericFunctions.stdPisWilliamsonAO(resultMap, searchId);
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				GenericFunctions.partyNamesTNShelbyTR(resultMap, searchId);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		resultMap.removeTempDef();
		return resultMap;
	}
}
