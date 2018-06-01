package ro.cst.tsearch.datatrace;

import static ro.cst.tsearch.datatrace.Utils.notNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.address2.Normalize;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.types.TSServersFactory;

import com.stewart.ats.base.document.TaxDocumentI;

public class DTTaxRecord {

	private Map<String,String> parcelInfo = null;

	private Map<String,String> currentAssessmentInfo = null;
	private Map<String,String> taxDueInfo = null;			
	private Set<List<Map<String,String>>> partyList = null;
	private Set<Map<String,String>> taxInstallmentList = null;
	private Set<Map<String,String>> taxInstallmentSupplementalList = null;
	private Set<Map<String,String>> specialAssessmentList = null;
	private Set<Map<String,String>> priorYearsDelinqList = null;
	private Set<Map<String,String>> priorYearsRedemptionList = null;

	
	public DTTaxRecord (
			Map<String,String> parcelInfo,
			Map<String,String> currentAssessmentInfo,
			Map<String,String> taxDueInfo,
			Set<List<Map<String,String>>> partyList,
			Set<Map<String,String>> taxInstallmentList,
			Set<Map<String,String>> specialAssessmentList
	){
		this.parcelInfo = parcelInfo;
		this.setCurrentAssessmentInfo(currentAssessmentInfo);
		this.setTaxDueInfo(taxDueInfo);
		this.setPartyList(partyList);
		this.setTaxInstallmentList(taxInstallmentList);
		this.setSpecialAssessmentList(specialAssessmentList);
	}
	public DTTaxRecord (
			Set<Map<String,String>> priorYearsDelinqList,
			Set<Map<String,String>> priorYearsRedemptionList
	){
		this.setPriorYearsDelinqList(priorYearsDelinqList);
		this.setPriorYearsRedemptionList(priorYearsRedemptionList);
	}
	
	public Map<String, String> getParcelInfo() {
		return parcelInfo;
	}

	public void setParcelInfo(Map<String, String> parcelInfo) {
		this.parcelInfo = parcelInfo;
	}
	
	public String getId(){
		String id = getAPN();
		
		if (id != null){ 
			return id;
		}
		
		return "";
	}
	
	/**
	 * return APN
	 * @return
	 */
	public String getAPN(){
		if (parcelInfo == null){ return null; }
		String apn = notNull(parcelInfo.get("apn.freeform"));
		
		apn = apn.replaceAll("\\s+", "");
		
		return apn;
	}
	
	/**
	 * returns image info
	 * @return String[2] containing id,description of image. null if no image exists
	 */
	public String [] getDTImageInfo(){
		String id = null;
		String description = null;
		
		if (parcelInfo != null){
			id = parcelInfo.get("image.image_params.document_index_id");
			if (id != null){
				description = parcelInfo.get("image.image_params.description");
			}
		}
		
		if (id != null && description != null){
			return new String[] {id, description};
		} else {
			return null;
		}
	}
	
	public void setParsedData(ParsedResponse pr,long searchId, DataSite dataSite){
		
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), dataSite.getSiteTypeAbrev());

		parseAndFillResultMap(pr, resultMap, dataSite);
				
    	Bridge bridge = new Bridge(pr, resultMap, searchId);
		try {
			TaxDocumentI doc = (TaxDocumentI) bridge.importData();
			doc.setDataSource(dataSite.getSiteTypeAbrev());
			
			String county  = dataSite.getCountyName();
			String state = dataSite.getStateAbbreviation();
			long siteId = TSServersFactory.getSiteId(state, county, doc.getDataSource());
			doc.setSiteId((int)siteId);
			pr.setDocument(doc);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void parseAndFillResultMap(ParsedResponse pr, ResultMap resultMap, DataSite dataSite){
	     
        Vector<PropertyIdentificationSet> pisVector = (Vector<PropertyIdentificationSet>) pr.infVectorSets.get("PropertyIdentificationSet");
        if (pisVector == null) {
        	pisVector = new Vector<PropertyIdentificationSet>();
        	pr.infVectorSets.put("PropertyIdentificationSet", pisVector);
        }
        pisVector.clear();  
        
        if (parcelInfo == null){
        	return;
        }
        
        PropertyIdentificationSet pis = new PropertyIdentificationSet();
        	
        	
       	String apn = notNull(parcelInfo.get("apn.freeform"));
       	String legal = notNull(parcelInfo.get("property.property_legal.freeform"));
        	
		String property_legal 		= notNull(parcelInfo.get("property.property_legal.freeform"));
		if (StringUtils.isNotEmpty(property_legal)){
			pis.setAtribute(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getShortKeyName(), legal.replaceAll("#/#", "; "));
			parseLegal(pis, legal);
		}
//		if (StringUtils.isEmpty(pis.getAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName()))
//				&& StringUtils.isEmpty(pis.getAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName()))){
//			
//			String pbk 		= notNull(parcelInfo.get("apn.book"));
//			String ppg 		= notNull(parcelInfo.get("apn.page"));
//				
//			if (pbk.matches("(?is)[A-Z]") && ppg.matches("(?is)[A-Z]")){
//				pbk = "";
//				ppg = "";
//			}
//			pis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), pbk);
//	        pis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), StringUtils.stripStart(ppg, "0"));
//		}
		
		String siteAddress			= notNull(parcelInfo.get("situs.freeform"));
		if (StringUtils.isNotEmpty(apn)){
			apn = apn.replaceAll("\\s+", "");
		}
		pis.setAtribute(PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(), apn);

        pis.setAtribute(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getShortKeyName(), siteAddress);
        
        String tractName = notNull(parcelInfo.get("tra.name"));
        if (StringUtils.isNotEmpty(tractName)){
        	pis.setAtribute(PropertyIdentificationSetKey.AREA_NAME.getShortKeyName(), 
        			tractName.replaceAll("(?is)\\A([^\\-]+)\\s*-.*", "$1").trim());
        }
        String areaCode = notNull(parcelInfo.get("tra.id"));
        if (StringUtils.isNotEmpty(areaCode)){
        	pis.setAtribute(PropertyIdentificationSetKey.AREA_CODE.getShortKeyName(), areaCode.trim());
        }
        
        if (StringUtils.isNotEmpty(siteAddress)){
        	if (StringUtils.isNotEmpty(tractName)){
        		tractName = tractName.replaceAll("(?is)\\A\\s*CITY OF\\s+([^\\-]+)\\s*-.*", "$1");
        		tractName = tractName.replaceAll("(?is)\\A\\s*(.*?)\\s+CITY\\s*-.*", "$1");
        		tractName = tractName.replaceAll("(?is)\\A\\s*CITY OF\\s+(.*)", "$1");
        		siteAddress = siteAddress.replaceFirst("(?is)" + tractName + ".*", "").trim();
        	} 
//        	else{
        		String[] tokens = siteAddress.split("\\s+");
        		StringBuffer addr = new StringBuffer();
        		int tokencounter = 0;
        		boolean suffixFound = false;
        		for (String token : tokens){
        			tokencounter++;
        			if (Normalize.isSuffix(token) && tokencounter > 2) {
        				addr.append(token).append(" ");
        				suffixFound = true;
        			} else{
        				if (suffixFound){
        					break;
        				}
        				addr.append(token).append(" ");
        			}
        		}
        		siteAddress = addr.toString().trim();
//        	}
        	//San diego: 487-291-20-00
        	siteAddress = siteAddress.replaceFirst("(?is)\\A(\\d+)\\s*-\\d+\\s+", "$1 ");
        	//Sacramaento: 132-1810-072-0000
        	siteAddress = siteAddress.replaceFirst("(?is)(\\d{5,})\\s*$", "");
        	
        	pis.setAtribute(PropertyIdentificationSetKey.STREET_NO.getShortKeyName(), StringFormats.StreetNo(siteAddress));
        	pis.setAtribute(PropertyIdentificationSetKey.STREET_NAME.getShortKeyName(), StringFormats.StreetName(siteAddress));
        }
       
        if (!pisVector.contains(pis)){
        	pisVector.add(pis);
        }
        
        String currentTaxPeriod = notNull(getTaxDueInfo().get("current_assessment.tax_period"));
    	if (StringUtils.isNotEmpty(currentTaxPeriod)){
    		String taxYear = currentTaxPeriod.replaceFirst("(?is)\\A\\s*(\\d{4})\\s*-\\s*\\d{2}\\s*$", "$1");
    		resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), taxYear);
    	}
        if (getCurrentAssessmentInfo() != null){
        	String assessedLand = notNull(getCurrentAssessmentInfo().get("current_assessment.tax_assessment.land_value"));
        	if (StringUtils.isNotEmpty(assessedLand)){
        		assessedLand = assessedLand.replaceAll("[\\$,]+", "").trim();
        		resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), assessedLand);
        	}
        	String assessedImprovements = notNull(getCurrentAssessmentInfo().get("current_assessment.tax_assessment.improvement_value"));
        	if (StringUtils.isNotEmpty(assessedImprovements)){
        		assessedImprovements = assessedImprovements.replaceAll("[\\$,]+", "").trim();
        		resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), assessedImprovements);
        	}
        	
        	String exemption = notNull(getCurrentAssessmentInfo().get("current_assessment.tax_assessment.homeowner_exemption"));
        	if (StringUtils.isNotEmpty(exemption)){
        		exemption = exemption.replaceAll("[\\$,]+", "").trim();
        		resultMap.put(TaxHistorySetKey.TAX_EXEMPTION_AMOUNT.getKeyName(), exemption);
        	}
        }
        
        String instNumber = notNull(parcelInfo.get("property.ownership.acquired.inst.number"));
        String instYear = notNull(parcelInfo.get("property.ownership.acquired.inst.year"));
        
        String instDay = notNull(parcelInfo.get("property.ownership.acquired.date.day"));
        String instMonth = notNull(parcelInfo.get("property.ownership.acquired.date.month"));
        String instYr = notNull(parcelInfo.get("property.ownership.acquired.date.year"));
        
        if (StringUtils.isNotEmpty(instNumber) && StringUtils.isNotEmpty(instYear)){
        	List<List> bodyHistory = new ArrayList<List>();
        	List<String> transaction = new ArrayList<String>();
        	
        	if (instYear.startsWith("19") || instYear.startsWith("20")){
        		if (!instNumber.contains("-")){
            		instNumber = instYear + "-" + instNumber;
            	}
	        	transaction.add(instYear);
	        	transaction.add(instNumber);
	        	transaction.add("");
	        	transaction.add("");
	        	transaction.add(instMonth);
	        	transaction.add(instDay);
        	} else{//this means is Book-Page
        		if (instNumber.contains("-")){
            		instNumber = instNumber.replaceFirst("(?is)\\A" + instYear + "-", "");
            	}
        		transaction.add(instYr);
	        	transaction.add("");
        		transaction.add(instYear);
	        	transaction.add(instNumber);
	        	transaction.add(instMonth);
	        	transaction.add(instDay);
        	}
        	
        	bodyHistory.add(transaction);
        	if (!bodyHistory.isEmpty()) {

				ResultTable newRT = new ResultTable();
				String[] header = {"Year", "InstrumentNumber", "Book", "Page", "Month", "Day"};
				Map<String, String[]> map = new HashMap<String, String[]>();
				map.put("Year", 			new String[] {"Year", ""});
				map.put("InstrumentNumber", new String[] {"InstrumentNumber", ""});
				map.put("Book", 			new String[] {"Book", ""});
				map.put("Page", 			new String[] {"Page", ""});
				map.put("Month", 			new String[] {"Month", ""});
				map.put("Day", 				new String[] {"Day", ""});
				
				try {
					newRT.setHead(header);
					newRT.setMap(map);
					newRT.setBody(bodyHistory);
					resultMap.put("CrossRefSet", newRT);
				} catch (Exception e) {
				}
			}
        }
        
        if (getTaxInstallmentList() != null && getTaxInstallmentList().size() > 0){
        	List<List> bodyInstallments = new ArrayList<List>();
			List<List> bodyTaxes = new ArrayList<List>();
			StringBuffer amPaid = new StringBuffer("0.00");
        	for (Map<String,String> install : getTaxInstallmentList()){
        		List<String> installRow = new ArrayList<String>();
        		String type = notNull(install.get("tax_installment.type"));
        		
        		String status = notNull(install.get("tax_installment.status"));
        		String paymentDate = notNull(install.get("payment_date.month")) 
        							+ "/" + notNull(install.get("payment_date.day")) 
        							+ "/" + notNull(install.get("payment_date.year"));
        		String delinqDate = notNull(install.get("delinquent_date.month")) 
									+ "/" + notNull(install.get("delinquent_date.day")) 
									+ "/" + notNull(install.get("delinquent_date.year"));
        		String installAmount = notNull(install.get("installment_amount"));
        		if (StringUtils.isNotEmpty(installAmount)){
        			installAmount = installAmount.replaceAll("[\\$,]+", "").trim();
        		}
        		String penaltyAmount = notNull(install.get("penalty_amount"));
        		if (StringUtils.isNotEmpty(penaltyAmount)){
        			penaltyAmount = penaltyAmount.replaceAll("[\\$,]+", "").trim();
        		}
        		String balanceDue = notNull(install.get("balance_due"));
        		if (StringUtils.isNotEmpty(balanceDue)){
        			balanceDue = balanceDue.replaceAll("[\\$,]+", "").trim();
        		}
        		if (StringUtils.isNotEmpty(paymentDate) && paymentDate.matches("(?is)//")){
        			paymentDate = "";
        		}
        		if ("PARTIAL".equalsIgnoreCase(type)){
        			
        			installRow.add(installAmount);
        			
        			if (StringUtils.isNotEmpty(paymentDate)){
            			List<String> line = new ArrayList<String>();
            			line.add(paymentDate);
            			if (StringUtils.isNotEmpty(installAmount) && StringUtils.isNotEmpty(balanceDue)){      				
            				if (status.contains("DELINQUENT") || status.contains("PAID")){
    	        				String amtPaid = GenericFunctions.sum(installAmount + "+" + penaltyAmount + "+-" + balanceDue, -1);
    	        				amPaid.append("+").append(amtPaid);
    	        				line.add(amtPaid);
    	        				installRow.add(amtPaid);
            				} else if (status.contains("OPEN")){
    	        				String amtPaid = GenericFunctions.sum(installAmount + "+-" + balanceDue, -1);
    	        				amPaid.append("+").append(amtPaid);
    	        				line.add(amtPaid);
    	        				installRow.add(amtPaid);
            				}
            			}
            			bodyTaxes.add(line);
            		} else{
            			installRow.add("0.00");
            		}

        			installRow.add(balanceDue);
        			installRow.add(penaltyAmount);
        			if ("PAID".equalsIgnoreCase(status)){
        				installRow.add("PAID");
        			} else{
        				installRow.add("UNPAID");
        			}
        			bodyInstallments.add(installRow);
        		} else if ("TOTAL".equalsIgnoreCase(type)){
        			if (StringUtils.isNotEmpty(installAmount)){
        				resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), installAmount);
        			}
        			if (StringUtils.isNotEmpty(balanceDue)){
        				resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), balanceDue);
        			}
        		}
        	}
        	if (!bodyInstallments.isEmpty()){
				String[] header = {"BaseAmount", "AmountPaid", "TotalDue", "PenaltyAmount", "Status"};
				Map<String, String[]> map = new HashMap<String, String[]>();
				map.put("BaseAmount", new String[] { "BaseAmount", "" });
				map.put("AmountPaid", new String[] { "AmountPaid", "" });
				map.put("TotalDue", new String[] { "TotalDue", "" });
				map.put("PenaltyAmount", new String[] { "PenaltyAmount", "" });
				map.put("Status", new String[] { "Status", "" });

				ResultTable installments = new ResultTable();
				try {
					installments.setHead(header);
					installments.setBody(bodyInstallments);
					installments.setMap(map);
					resultMap.put("TaxInstallmentSet", installments);
				} catch (Exception e) {
				}
			}
        	if (!bodyTaxes.isEmpty()) {

        		String amtPaid = GenericFunctions.sum(amPaid.toString(), -1);
        		if (StringUtils.isNotEmpty(amtPaid)){
        			resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amtPaid);
        		}
        		
				ResultTable newRT = new ResultTable();
				String[] header = {"ReceiptDate", "ReceiptAmount"};
				Map<String, String[]> map = new HashMap<String, String[]>();
				map.put("ReceiptDate", new String[] {"ReceiptDate", ""});
				map.put("ReceiptAmount", new String[] {"ReceiptAmount", ""});
				
				try {
					newRT.setHead(header);
					newRT.setMap(map);
					newRT.setBody(bodyTaxes);
					resultMap.put("TaxHistorySet", newRT);
				} catch (Exception e) {
				}
			}
        }
        //parse prior years delinquent
        StringBuffer totalPrior = new StringBuffer();
        if (getPriorYearsDelinqList() != null && getPriorYearsDelinqList().size() > 0){
        	
        	for (Map<String,String> eachRedemp : getPriorYearsDelinqList()){
        		String amount = eachRedemp.get("amount_due");
        		if (StringUtils.isNotEmpty(amount)){
        			amount = amount.replaceAll("[\\$,]+", "").trim();
        			if (amount.matches("(?is)[\\d\\.]+")){
	        			if (totalPrior.length() == 0){
	        				totalPrior.append(amount);
	        			} else{
	        				totalPrior.append("+").append(amount);
	        			}
        			}
        		}
        	}
        }
        if (getTaxInstallmentSupplementalList() != null && getTaxInstallmentSupplementalList().size() > 0){
        	boolean isPriorDelinq = false; //this is from supplemental bill
        	for (Map<String,String> eachInstall : getTaxInstallmentSupplementalList()){
        		if (StringUtils.isNotEmpty(eachInstall.get("tax_period"))){
        			String taxPeriod = eachInstall.get("tax_period");
        			if (StringUtils.isNotEmpty(taxPeriod)){
        				taxPeriod = taxPeriod.replaceFirst("(?is)\\A\\s*(\\d{4})\\s*-\\s*\\d{2}\\s*$", "$1");
        				int taxPeriodInt = -1;
        				try {
							taxPeriodInt = Integer.parseInt(taxPeriod);
						} catch (Exception e) {
						}
        				int currentTaxYearInt = -1;
        				try {
        					currentTaxYearInt = Integer.parseInt((String) resultMap.get(TaxHistorySetKey.YEAR.getKeyName()));
						} catch (Exception e) {
						}
        				if (taxPeriodInt > 0 && currentTaxYearInt > 0 && taxPeriodInt < currentTaxYearInt){
        					isPriorDelinq = true;
        					break;
        				}
        			}
        		}
        		
        	}
        	if (isPriorDelinq){
        		for (Map<String,String> eachInstall : getTaxInstallmentSupplementalList()){
        			String status = eachInstall.get("tax_installment.status");
        			if (StringUtils.isNotEmpty(status)){
        				if (status.toLowerCase().contains("delinquent")){
        					String amount = eachInstall.get("balance_due");
        					if (StringUtils.isNotEmpty(amount)){
        						amount = amount.replaceAll("[\\$,]+", "").trim();
        						if (amount.matches("(?is)[\\d\\.]+")){
        							totalPrior.append("+").append(amount);
        						}
        					}
        				}
        			}
        		}
        	}
        }
        if (totalPrior.length() > 0){
			try {
				resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), GenericFunctions.sum(totalPrior.toString(), -1));
			} catch (Exception e) {
			}
		}
        
        if (getPartyList() != null && getPartyList().size() > 0){
        	ArrayList<List> body = new ArrayList<List>();
        	StringBuffer nameOnServer = new StringBuffer();
        	for (List<Map<String,String>> party : getPartyList()){
        		if (party != null && party.size() > 0){
	        		for (Map<String,String> prt : party){
	        			if (prt != null){
		        			String name = notNull(prt.get("freeform"));
		        			if (StringUtils.isNotEmpty(name)){
		        				
		        				if (nameOnServer.length() == 0){
		        					nameOnServer.append(name);
		        				} else{
		        					nameOnServer.append(" / ").append(name);
		        				}
		        				
		        				name = name.replaceFirst("(?is)\\bAND\\b\\s*$", "");
		        				name = name.replaceAll("(?is)\\b(DECD|DECEASED)\\b", "");
		        				name = name.replaceAll("(?is)\\bEST(ATE)?\\s+OF\\b", "");
		        				name = name.replaceFirst("(?is)\\bFAMI\\b\\s*$", "");
		        				name = name.replaceFirst("(?is)\\bJT\\b", "");
		        				name = name.replaceFirst("(?is)\\bJOINT\\s+TENANTS\\b", "");
		        				name = name.replaceFirst("(?is)\\bREV(\\s+TR)\\b", "$1");
		        				
		        				String[] names = StringFormats.parseNameNashville(name, true);
		        				
		        				if (name.trim().startsWith("C/O ") || name.trim().startsWith("C O ") || name.trim().startsWith("(N)")){
		        					name = name.replaceFirst("(?is)\\A\\s*\\(N\\)", "");
		        					name = name.replaceFirst("(?is)\\bC O\\b", "");
			        				name = name.replaceFirst("(?is)\\bC/O\\b", "");
			        				names = StringFormats.parseNameDesotoRO(name.trim(), true);
		        				}
		
		        				String[] type = GenericFunctions.extractAllNamesType(names);
		        				String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
		        				String[] suffixes = GenericFunctions.extractNameSuffixes(names);
		
		        				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1],
								        						type, otherType, NameUtils.isCompany(names[2]),
								        						NameUtils.isCompany(names[5]), body);
		        			}
	        			}
	        		}
        		}
        	}
        	if (nameOnServer.length() > 0){
        		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), nameOnServer.toString());
        	}
        	if (body.size() > 0){
	        	try {
	    			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
        	}
        }
	
	}
	
	public static void parseLegal(PropertyIdentificationSet pis, String legal){
		
		Pattern STR = Pattern.compile("(?is)\\b(?:LOT/SECT)\\s+(\\w+)\\s+(?:BLK/DIV/TWN)\\s+(\\w+)\\s+(?:REG/RNG)\\s+(\\w+)");
		Matcher mat = STR.matcher(legal);
		if (mat.find()){
			pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), mat.group(1));
			pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), mat.group(2));
			pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), mat.group(3));
			legal = legal.replaceFirst(mat.group(0), "");
		} else{
			STR = Pattern.compile("(?is)\\b(?:SECTION(?:AL)?:?)\\s+(\\w+)\\s+(?:TOWNSHIP\\s*:?)\\s+(\\w+)\\s+(?:RANGE\\s*:)\\s+(\\w+)");
			mat = STR.matcher(legal);
			if (mat.find()){
				pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), mat.group(1));
				pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), mat.group(2));
				pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), mat.group(3));
				legal = legal.replaceFirst(mat.group(0), "");
			} else {
				STR = Pattern.compile("(?is)\\b(?:SEC:?)\\s*(\\w+)\\s+(?:TP?\\s*:?)\\s*(\\w+)\\s+(?:R\\s*:?)\\s*(\\w+)");
				mat = STR.matcher(legal);
				if (mat.find()){
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), mat.group(1));
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), mat.group(2));
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), mat.group(3));
					legal = legal.replaceFirst(mat.group(0), "");
				}
			}
		}
		
		//CASacramento:  207-0250-043-0000
		Pattern ARB_B_PG_PAT = Pattern.compile("(?is)\\b(AR\\s*:)\\s*([\\w+]+)\\s+(PAGE|PG)\\s*:\\s*([\\w+]+)\\b");
		mat = ARB_B_PG_PAT.matcher(legal);
		if (mat.find()){
			String arb = StringUtils.stripStart(mat.group(2), "0") + "-" + StringUtils.stripStart(mat.group(4), "0");
			pis.setAtribute(PropertyIdentificationSetKey.ARB.getShortKeyName(), arb);
			legal = legal.replaceAll(mat.group(0), "");
		} else{
			//CAContra Costa: 182-090-010-6
			ARB_B_PG_PAT = Pattern.compile("(?is)\\b(BK\\s*:)\\s*A(\\d+)\\s+(PAGE|PG)\\s*:\\s*([\\w+]+)\\b");
			mat = ARB_B_PG_PAT.matcher(legal);
			if (mat.find()){
				String arb = StringUtils.stripStart(mat.group(2), "0") + "-" + StringUtils.stripStart(mat.group(4), "0");
				pis.setAtribute(PropertyIdentificationSetKey.ARB.getShortKeyName(), arb);
				legal = legal.replaceAll(mat.group(0), "");
			}
		}
				
		Pattern PB_PG_PAT = Pattern.compile("(?is)\\b(MP\\s*:|BK\\s*:|RS\\s*:)\\s*([\\w+]+)\\s+(PAGE|PG)\\s*:\\s*([\\w+]+)\\b");
		mat = PB_PG_PAT.matcher(legal);
		if (mat.find()){
			pis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), StringUtils.stripStart(mat.group(2), "0"));
			pis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), StringUtils.stripStart(mat.group(4), "0"));
			legal = legal.replaceFirst(mat.group(0), "");
		} else{
			Pattern PBPG_PAT = Pattern.compile("(?is)\\b(MB)\\s*(\\d+)\\s*/\\s*(\\d+)\\b");
			mat = PBPG_PAT.matcher(legal);
			if (mat.find()){
				pis.setAtribute(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), StringUtils.stripStart(mat.group(2), "0"));
				pis.setAtribute(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), StringUtils.stripStart(mat.group(3), "0"));
				legal = legal.replaceFirst(mat.group(0), "");
			}
		}
		
		Pattern UNIT_NO = Pattern.compile("(?is)\\b(UN(?:IT)?(?:\\s*[#:]*)?)\\s*(\\d+)");
		mat.reset();
		mat = UNIT_NO.matcher(legal);
		if (mat.find()){
			pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getShortKeyName(), mat.group(2));
			legal = legal.replaceFirst(mat.group(0), "");
		} else{
			legal = legal.replaceAll("(?is)\\bUNIT:", "");
		}
		
		Pattern BLDG_NO = Pattern.compile("(?is)\\b(BLDG(?:\\s*#)?)\\s*(\\d+)");
		mat.reset();
		mat = BLDG_NO.matcher(legal);
		if (mat.find()){
			pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLDG.getShortKeyName(), mat.group(2));
			legal = legal.replaceFirst(mat.group(0), "");
		} else{
			legal = legal.replaceAll("(?is)\\bBLDG:", "");
		}
		
		Pattern LOT_SECT = Pattern.compile("(?is)\\b(LOT/SECT)\\s+(\\w+)");
		mat = LOT_SECT.matcher(legal);
		if (mat.find()){
			pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), mat.group(2));
			legal = legal.replaceFirst(mat.group(0), "");
		} else{
			Pattern LOT_PAT = Pattern.compile("(?is)\\b(LO?TS?\\s*:?)\\s+([\\d\\s&]+)");
			mat = LOT_PAT.matcher(legal);
			String lot = "";
			while (mat.find()){
				lot += StringUtils.stripStart(mat.group(2), "0") + " ";
			}
			if (StringUtils.isNotEmpty(lot)){
				lot = lot.replaceAll("\\s*&\\s*", " ");
				lot = lot.replaceAll("\\s+", " ");
				lot = LegalDescription.cleanValues(lot, false, true);
				pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getShortKeyName(), lot.trim());
			}
		}
		
		Pattern TRACT_NO = Pattern.compile("(?is)\\b(TRACT(?:\\s+NO|\\s*#|\\s*=?)|TR\\s*(?:=|:)?|T)\\s*([\\d]+(?:\\s*-\\s*\\w)?)");
		mat.reset();
		mat = TRACT_NO.matcher(legal);
		if (mat.find()){
			String tract = StringUtils.stripStart(mat.group(2), "0");
			tract = tract.replaceFirst("(?is)\\s*-\\s*\\w\\s*$", "");
			pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getShortKeyName(), tract);
			legal = legal.replaceFirst(mat.group(0), "");
		}
		
		Pattern BLK_DIV_TWN = Pattern.compile("(?is)\\b(BLK/DIV/TWN)\\s+(\\d+|\\w)\\b");
		mat.reset();
		mat = BLK_DIV_TWN.matcher(legal);
		if (mat.find()){
			pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), mat.group(2));
			legal = legal.replaceFirst(mat.group(0), "");
		} else{
			Pattern BLK_PAT = Pattern.compile("(?is)\\b(BLK\\s*:?)\\s+(\\d+|\\w)\\b");
			mat = BLK_PAT.matcher(legal);
			String blk = "";
			if (mat.find()){
				blk += StringUtils.stripStart(mat.group(2), "0") + " ";
				if (StringUtils.isNotEmpty(blk)){
					pis.setAtribute(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getShortKeyName(), blk);
					legal = legal.replaceFirst(mat.group(0), "");
				}
			} else{
				legal = legal.replaceAll("(?is)\\bBLK:", "");
			}
		}
	}

	/**
	 * @return the currentAssessmentInfo
	 */
	public Map<String,String> getCurrentAssessmentInfo() {
		return currentAssessmentInfo;
	}

	/**
	 * @param currentAssessmentInfo the currentAssessmentInfo to set
	 */
	public void setCurrentAssessmentInfo(Map<String,String> currentAssessmentInfo) {
		this.currentAssessmentInfo = currentAssessmentInfo;
	}

	/**
	 * @return the taxDueInfo
	 */
	public Map<String,String> getTaxDueInfo() {
		return taxDueInfo;
	}

	/**
	 * @param taxDueInfo the taxDueInfo to set
	 */
	public void setTaxDueInfo(Map<String,String> taxDueInfo) {
		this.taxDueInfo = taxDueInfo;
	}

	/**
	 * @return the partyList
	 */
	public Set<List<Map<String,String>>> getPartyList() {
		return partyList;
	}

	/**
	 * @param partyList the partyList to set
	 */
	private void setPartyList(Set<List<Map<String,String>>> partyList) {
		this.partyList = partyList;
	}

	/**
	 * @return the taxInstallmentList
	 */
	public Set<Map<String,String>> getTaxInstallmentList() {
		return taxInstallmentList;
	}

	/**
	 * @param taxInstallmentList the taxInstallmentList to set
	 */
	public void setTaxInstallmentList(Set<Map<String,String>> taxInstallmentList) {
		this.taxInstallmentList = taxInstallmentList;
	}

	/**
	 * @return the taxInstallmentSupplementalList
	 */
	public Set<Map<String,String>> getTaxInstallmentSupplementalList() {
		return taxInstallmentSupplementalList;
	}
	/**
	 * @param taxInstallmentSupplementalList the taxInstallmentSupplementalList to set
	 */
	public void setTaxInstallmentSupplementalList(Set<Map<String,String>> taxInstallmentSupplementalList) {
		this.taxInstallmentSupplementalList = taxInstallmentSupplementalList;
	}
	/**
	 * @return the specialAssessmentList
	 */
	public Set<Map<String,String>> getSpecialAssessmentList() {
		return specialAssessmentList;
	}

	/**
	 * @param specialAssessmentList the specialAssessmentList to set
	 */
	public void setSpecialAssessmentList(Set<Map<String,String>> specialAssessmentList) {
		this.specialAssessmentList = specialAssessmentList;
	}

	/**
	 * @return the priorYearsDelinqList
	 */
	public Set<Map<String,String>> getPriorYearsDelinqList() {
		return priorYearsDelinqList;
	}

	/**
	 * @param priorYearsDelinqList the priorYearsDelinqList to set
	 */
	public void setPriorYearsDelinqList(Set<Map<String,String>> priorYearsDelinqList) {
		this.priorYearsDelinqList = priorYearsDelinqList;
	}

	/**
	 * @return the priorYearsRedemptionList
	 */
	public Set<Map<String,String>> getPriorYearsRedemptionList() {
		return priorYearsRedemptionList;
	}

	/**
	 * @param priorYearsRedemptionList the priorYearsRedemptionList to set
	 */
	public void setPriorYearsRedemptionList(Set<Map<String,String>> priorYearsRedemptionList) {
		this.priorYearsRedemptionList = priorYearsRedemptionList;
	}	
	
}

