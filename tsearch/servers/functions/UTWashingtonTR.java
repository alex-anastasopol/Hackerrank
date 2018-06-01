package ro.cst.tsearch.servers.functions;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.ResultBodyUtils;
import ro.cst.tsearch.utils.StringUtils;

public class UTWashingtonTR extends ParseClass {

	private static UTWashingtonTR _instance = null;

	private UTWashingtonTR() {

	}

	public static UTWashingtonTR getInstance() {
		if (_instance == null) {
			_instance = new UTWashingtonTR();
		}
		return _instance;
	}

	@Override
	public void parseDetails(String response, long searchId, ResultMap resultMap) {
		String label = "Net Total Due:";
		String netTotalDue = extractValueForLabel(response, label);
		resultMap.put( TaxHistorySetKey.TOTAL_DUE.getKeyName(), netTotalDue);
		
		label = "Serial Number:";
		String serialNumber = extractValueForLabel(response, label);
		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID2.getKeyName(), serialNumber);
		
		label = "Account Number:";
		String accountNumber = extractValueForLabel(response, label);
		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), accountNumber);
		
		label = "Primary Owner:";
		String primaryOwner = extractValueForLabel(response, label);
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), primaryOwner);
		
		setTaxData(response,resultMap);
//		setTaxData(resultMap, tableAsListMap);
		
		parseName("", resultMap);
		
	}

	@Override
	public void setTaxData(String text, ResultMap resultMap) {
		String tbl = RegExUtils.getFirstMatch("(?is)<table.*</table>", text, 0);
		List<HashMap<String,String>> tableAsListMap = HtmlParser3.getTableAsListMap(tbl);
		parseTaxData(resultMap, tableAsListMap);
	}
	
	private void parseTaxData(ResultMap resultMap, List<HashMap<String, String>> tableAsListMap) {
		String[] header= new String[]{ TaxHistorySetKey.YEAR.getShortKeyName(), 
				TaxHistorySetKey.BASE_AMOUNT.getShortKeyName(), 
				TaxHistorySetKey.TOTAL_DUE.getShortKeyName(), TaxHistorySetKey.AMOUNT_PAID.getShortKeyName()};
		
		Map<String, String> resultBodyHeaderToSourceTableHeader = new HashMap<String,String>();
		String yearHtmlKey = "Year";
		resultBodyHeaderToSourceTableHeader.put(TaxHistorySetKey.YEAR.getShortKeyName(), yearHtmlKey);
		
		String baseAmountHtmlKey = "Original Tax";
		resultBodyHeaderToSourceTableHeader.put(TaxHistorySetKey.BASE_AMOUNT.getShortKeyName(), baseAmountHtmlKey);
		
		String totalDueHtmlKey = "Total Due";
		resultBodyHeaderToSourceTableHeader.put(TaxHistorySetKey.TOTAL_DUE.getShortKeyName(), totalDueHtmlKey);
		
		resultBodyHeaderToSourceTableHeader.put(TaxHistorySetKey.AMOUNT_PAID.getShortKeyName(), TaxHistorySetKey.AMOUNT_PAID.getShortKeyName());
		
		//get last record from table;		
		HashMap<String, String> lastYearRecord = tableAsListMap.get(tableAsListMap.size()-1);
		
		//total due
		String totalDue = StringUtils.cleanAmount(lastYearRecord.get(totalDueHtmlKey));
		resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
		
		String baseAmnt = StringUtils.cleanAmount(lastYearRecord.get(baseAmountHtmlKey));
		if (StringUtils.isEmpty(baseAmnt)){
			baseAmnt = "0.00";
		}
		if ("0.00".equals(totalDue)){
			//amount paid
			resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), baseAmnt);
		} else {
			String taxDue = StringUtils.cleanAmount(lastYearRecord.get("Tax Due"));
			if (NumberUtils.isNumber(taxDue) && NumberUtils.isNumber(baseAmnt)){
				BigDecimal td = new BigDecimal(taxDue);
				BigDecimal ba = new BigDecimal(baseAmnt);
				resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), ba.subtract(td).toPlainString());
			}else{
				resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), "0");
			}
		}

		//tax year
		String currentYear = lastYearRecord.get(yearHtmlKey);
		resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), currentYear);
		
		//clean table from Receipt Number 0
		BigDecimal priorDeliq = new BigDecimal(0);
		for (Map<String,String> row: tableAsListMap) {
			String baseAmount = row.get(baseAmountHtmlKey);
			row.put(baseAmountHtmlKey,StringUtils.cleanAmount(baseAmount));
			totalDue = row.get(totalDueHtmlKey); 
			if ("0.00".equals(totalDue)){
				row.put(totalDueHtmlKey,StringUtils.cleanAmount(totalDue));
				row.put(TaxHistorySetKey.AMOUNT_PAID.getShortKeyName(),StringUtils.cleanAmount(baseAmount));
			}
			
			//prior delinquent
			String y = row.get(yearHtmlKey);
			boolean isLessThanCurrentYear = StringUtils.isNotEmpty(y) && StringUtils.isNotEmpty(currentYear)  && (!y.equals(currentYear));
			String totalDueAmount = StringUtils.cleanAmount(row.get(totalDueHtmlKey)); 
			if (isLessThanCurrentYear){
				boolean number = NumberUtils.isNumber(totalDueAmount);
				if (number){
					priorDeliq = priorDeliq.add(new BigDecimal(totalDueAmount));
				}
			}
		}
		
		//prior delinquent
		resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), "" + priorDeliq.doubleValue());
 
		//base amount 
		resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmnt);
		
		ResultBodyUtils.buildInfSet(resultMap, tableAsListMap, header, resultBodyHeaderToSourceTableHeader , TaxHistorySet.class);
	}

	public String extractValueForLabel(String response, String label) {
		String labelRegEx = label + " <strong>(.*?)</strong>";
		String value = RegExUtils.getFirstMatch(labelRegEx, response, 1);
		return value;
	}

	@Override
	public void parseName(String name, ResultMap resultMap) {
		String nameOnServer = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		if (StringUtils.isNotEmpty(nameOnServer)){
			ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, nameOnServer, null);
		}
	}

}
