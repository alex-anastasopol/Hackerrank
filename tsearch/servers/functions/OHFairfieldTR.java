package ro.cst.tsearch.servers.functions;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.Node;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.servers.response.TaxInstallmentSet.TaxInstallmentSetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

public class OHFairfieldTR {

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
				
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "TR");
				
		TableColumn[] cols = row.getColumns();
		if (cols.length==5) {
			String parcelID;
			String owner;
			String address;
					
			parcelID = cols[0].toPlainTextString().trim(); 
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(), parcelID);
			owner = cols[1].toPlainTextString().trim().replaceAll("&amp;", "&");
			resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), owner);
			address= cols[2].toPlainTextString().trim();
			resultMap.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), address);
			
			parseNames(resultMap, searchId);
			parseAddress(resultMap);
		}
				
		return resultMap;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) {
		
		String owner = (String) resultMap.get(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName());
		if (StringUtils.isEmpty(owner))
			   return;
		
		boolean moreThanTwoNames = false;
		boolean done = false;
		
		String trPatt = "\\b(T(?:(?:RU?)?S?)?(?:TE)?E?S?)\\b";					//trustee pattern
		
		owner = owner.replaceAll("(?is)\\bSURV\\b(\\s+SUB\\b)?", "");
		owner = owner.replaceAll(",", "&");
		owner = owner.replaceAll("T\\.?O\\.?D\\.?\\s*\\z", "");
		owner = owner.replaceAll("(?is)-LE-", " LE ");
		owner = owner.replaceAll("(?is)" + trPatt + "(?: THE\\b)?", "$1 OF THE");
		owner = owner.replaceAll("(?is) OF THE\\s*\\z", "");
		owner = owner.replaceAll("(?is)\\bLE\\s+OF\\b", "LE @@@ OF");
		owner = owner.replaceAll("(?is)\\b(LI?VI?N?G|FAM(?:ILY)?)\\s+TRS?\\b", "$1 TRUST");
		owner = owner.replaceAll("(?is)\\bOR\\b", "&");
		owner = owner.replaceAll("(?is)(?:\\bCO[-\\s]?\\b)" + trPatt, "$1");
		owner = owner.replaceAll("(?is)\\bLE\\s*NKA\\b", "LE AKA");
		owner = owner.replaceAll("(?is)\\bAS\\s+" + trPatt, "$1");
		
		owner += " &";
		owner = owner.replaceAll("(?is)\\b(AKA.*?)\\s*&", "($1) &");
		owner = owner.replaceFirst("\\s*&\\s*\\z", "");
		owner = owner.replaceFirst("\\({2,}", "(");
		owner = owner.replaceFirst("\\){2,}", ")");
		
		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = {"", ""} , type, otherType;
		
		String[] names_later = {"", "", "", "", "", ""};
		String[] suffixes_later = {"", ""} ;
		String[] type_later = {"", ""};
		String[] otherType_later = {"", ""};
		boolean later = false;
		
		List<String> newOwner = new ArrayList<String>(); 
		
		String[] split;
		
		if (!done) {										//KILBARGER LINDA M % RICK BABCOCK
			split = owner.split("%");									
			if (split.length==2) {									
				newOwner.add(split[0]);
				newOwner.add(split[1] + " @@@FML@@@");
				done = true;
			}
		}
		
		if (!done) {										//BROWN REGINA R NKA GOTTLIEBSON REGINA RUTH
			split = owner.split("\\b(F|N)KA\\b");
			if (split.length>1) {
				for (int i=0;i<split.length;i++) {
					String s = split[i].trim();
					if (s.length()>0) {
						if (s.split("\\s").length>1) {
							newOwner.add(s.replaceFirst("\\A\\s*&", ""));
						}
					}
				}
				done = true;
			}
		}
		
		if (!done) {									//INSER CHARLES JR & EVA M TRUSTEES OF THE CHAS & EVA KINSER REVOCABLE TRUST
			String ofUnderThePatt = "(?:OF|UNDER)\\b(?:\\sTHE\\b)?";
			Matcher matcher = Pattern.compile("(?is)(.+?)" + trPatt + "\\s" + ofUnderThePatt + "(.+)").matcher(owner);
			if (matcher.find()) {
				newOwner.add((matcher.group(1) + " " + matcher.group(2)).trim());
				newOwner.add(matcher.group(3).trim().replaceFirst("\\s*" + ofUnderThePatt, ""));
				done = true;
			}
		}
		
		if (!done) {									//SMITH JOHNATHON A & SMITH DAVID A SURV SUB LE OF CURTICE EMMA JEAN
			Matcher matcher = Pattern.compile("(?is)(.+?)@@@\\sOF\\b(.+)").matcher(owner);
			if (matcher.find()) {
				newOwner.add(matcher.group(1).trim());
				newOwner.add(matcher.group(2).trim());
				done = true;
			}
		}
		
		if (!done) {
			split = owner.split("&");
			if (split.length>2) {									//more than two names
				for (int i=0;i<split.length-1;i++) {
					newOwner.add(split[i]+ " & " + split[i+1]);		//take the names two by two in order not to lose last names
				}													//e.g. SMITH JOHN & MARY & DAVID		
				moreThanTwoNames = true;
				done = true;
			}
		}
		
		if (!done) {								//one or two names
			newOwner.add(owner);
		}
		
		for (int i=0;i<newOwner.size();i++) {
			String s = newOwner.get(i).trim();
			
			String akaExpression = "\\(\\s*AKA(.*?)\\s*\\)";
			
			int j ;
			if (moreThanTwoNames && i>0) {
				j=5;
				if (names[j].length()==0)
					j = 2;
				if (!s.startsWith(names[j] + " "))
					s = names[j] + " " + s;
			}
			
			Matcher matcher1 = Pattern.compile("(?is)(.*?)" + akaExpression + "(\\s*" + 
					akaExpression + ")*").matcher(s);
			if (matcher1.find()) {
				String baseName = s.replaceAll("(?is)" + akaExpression, "");
				names = StringFormats.parseNameNashville(baseName, true);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				if (LastNameUtils.isLastName(names[3]) && !FirstNameUtils.isFirstName(names[3]) &&
					FirstNameUtils.isFirstName(names[5]) && !LastNameUtils.isLastName(names[5])) {
						String aux = names[3];
						names[3] = names[5];
						names[5] = aux;
				}
				
				later = false;
				int ampersandIndex = s.indexOf("&");
				if (ampersandIndex>-1 && s.indexOf("(AKA ")<ampersandIndex) {	//aka name before the second owner:
					names_later[0] = names[3];									//keep the second owner in order
					names_later[1] = names[4];									//to be added later
					names_later[2] = names[5];									//(after aka name is added)
					names_later[3] = names_later[4] = names_later[5] = "";
					names[3] = names[4] = names[5] = "";
					suffixes_later[0] = suffixes[1]; 
					suffixes_later[1] = "";
					suffixes[1] = "";
					type_later[0] = type[1]; 
					type_later[1] = "";
					type[1] = "";
					otherType_later[0] = otherType[1]; 
					otherType_later[1] = "";
					otherType[1] = "";
					later = true;
				}
				
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
				
				Matcher matcher2 = Pattern.compile("(?is)" + akaExpression).matcher(s);
				while (matcher2.find()) {
					String akaName = matcher2.group(1).trim();
					split = akaName.split("\\s");
					j=5;
					if (names[j].length()==0)
						j = 2;
					if (names[j].equals(split[0])) {	//aka name begins with last name
						akaName += "@@@LFM@@@";
					} else  {
						boolean withoutLastName = false; 
						if (split.length==2) {
							withoutLastName = FirstNameUtils.isFirstName(split[0]);
							withoutLastName = withoutLastName && 
								(split[1].matches("[A-Z]") || FirstNameUtils.isFirstName(split[1])); 
						}  
						if (withoutLastName) {
							akaName = akaName + " " + names[j];
						}	
					}
					if (akaName.endsWith("@@@LFM@@@")) {
						akaName = akaName.replaceFirst("@@@LFM@@@", "").trim();
						names = StringFormats.parseNameNashville(akaName, true);
					}	
					else {
						names = StringFormats.parseNameDesotoRO(akaName, true);
					}
					suffixes = GenericFunctions.extractNameSuffixes(names);
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
					if (later) {
						GenericFunctions.addOwnerNames(names_later, suffixes_later[0], suffixes_later[1], 
								type_later, otherType_later,
								NameUtils.isCompany(names_later[2]), NameUtils.isCompany(names_later[5]), body);
					}
					
				}
			} else {
				if (moreThanTwoNames && i>0 && !s.startsWith(names[5] + " "))
					s = names[5] + " " + s;
				if (s.endsWith("@@@FML@@@")) {
					s = s.replaceFirst("@@@FML@@@", "").trim();
					names = StringFormats.parseNameDesotoRO(s, true);
				}	
				else {
					names = StringFormats.parseNameNashville(s, true);
				}
				suffixes = GenericFunctions.extractNameSuffixes(names);
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				if (moreThanTwoNames && i>0) {									//the first name was already added
					names[0] = names[3];										//so only the second will be added now
					names[1] = names[4];
					names[2] = names[5];
					suffixes[0] = suffixes[1];
					type[0] = type[1];
					otherType[0] = otherType[1];
					names[3] = names[4] = names[5] = "";
					suffixes[1] = "";
					type[1] = "";
					otherType[1] = "";
				}
				GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			}
		}
		
		body = removeDuplicates(body);
		
		try {
			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static List<List> removeDuplicates(List<List> body) {
		List<List> result = new ArrayList<List>();
		for (List l: body) {
			if (!result.contains(l))
				result.add(l);
		}
		return result;
	} 
	
	public static void parseAddress(ResultMap resultMap)  {
		
		String address = (String) resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (address==null) 
			return;
		if (StringUtils.isEmpty(address))
			return;
		
		String streetName = StringFormats.StreetName(address).trim();
		String streetNo = StringFormats.StreetNo(address);
		if (streetNo.matches("0+"))
			streetNo = "";

		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
	}
		
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void parseLegalSummary(ResultMap resultMap)
	{
		
	String legalDescription = (String) resultMap.get(PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER.getKeyName());
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
		
		List<List> bodySTR = new ArrayList<List>();
		
		String strSubexpr = "(?:\\d+(?:\\s(?:NE|NW|SE|SW|N|S|E|W)\\b)?)";
		String strExpr = "(?is)\\bR\\s(\\d+)\\sT\\s(\\d+)\\sS\\s(" + strSubexpr +  
						 "(?:\\s&\\s" + strSubexpr + ")*)";
		Matcher matcher1 = Pattern.compile(strExpr).matcher(legalDescription);
		if (matcher1.find()) {
			String range = matcher1.group(1);
			String township = matcher1.group(2);
			String section = matcher1.group(3);
			
			Matcher mQVal = Pattern.compile("(?is)(\\d+)(?:\\s(NE|NW|SE|SW|N|S|E|W)\\b)").matcher(section);
			if (mQVal.find()){
				resultMap.put(PropertyIdentificationSetKey.QUARTER_VALUE.getKeyName(), mQVal.group(2));
				section = mQVal.group(1);
			}
			
			String[] split = section.split("&");
			for (int i=0;i<split.length;i++) {
				List list = new ArrayList<String>();
				list.add(split[i].trim());
				list.add(township);
				list.add(range);
				bodySTR.add(list);
			}
		}
		
		String lotSubexpr = "(?:[0-9]+(?:[NE|NW|SE|SW|N|S|E|W])?)";
		String lotExpr = "(?is)\\bLOT\\s(" + lotSubexpr + "(?:[-\\s]" + lotSubexpr + ")*)";
		List<String> lot = RegExUtils.getMatches(lotExpr, legalDescription, 1);
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<lot.size(); i++) {
			String[] split = lot.get(i).split("[-\\s]");
			for (int j=0;j<split.length;j++)
				sb.append(split[j]).append(" ");
		} 
		String subdivisionLot = sb.toString().trim();
		if (subdivisionLot.length() != 0) {
			subdivisionLot = LegalDescription.cleanValues(subdivisionLot, false, true);
			subdivisionLot = sortValues(subdivisionLot);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), subdivisionLot);
		}
		
		String subdivisionexpr = "(?is)(.*?)\\s*\\b(P(?:AR)?T|ADD|SUB|PH|SEC|LOT|OF)\\b";
		Matcher matcher2 = Pattern.compile(subdivisionexpr).matcher(legalDescription);
		if (matcher2.find()) {
			String subd = matcher2.group(1).trim();
			if (subd.matches(".*?\\bCONDO\\z")) {
				subd = subd.replaceFirst("\\bCONDO\\z", "");
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_COND.getKeyName(), subd);
			}
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subd);
		}
		
		List<String> unit = RegExUtils.getMatches("(?is)\\bUNIT\\s([^\\s]+)\\s", legalDescription, 1);
		sb = new StringBuilder();
		for (int i=0; i<unit.size(); i++) 
			sb.append(unit.get(i)).append(" ");
		String subdivisionUnit = sb.toString().trim();
		if (subdivisionUnit.length() != 0) {
			subdivisionUnit = LegalDescription.cleanValues(subdivisionUnit, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), subdivisionUnit);
		}
		
		List<String> phase = RegExUtils.getMatches("(?is)\\bPH\\s(\\d+)", legalDescription, 1);
		sb = new StringBuilder();
		for (int i=0; i<phase.size(); i++) 
			sb.append(phase.get(i)).append(" ");
		String subdivisionPhase = sb.toString().trim();
		if (subdivisionPhase.length() != 0) {
			subdivisionPhase = LegalDescription.cleanValues(subdivisionPhase, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(), subdivisionPhase);
		}
		
		List<String> section = RegExUtils.getMatches("(?is)\\bSEC\\s(\\d+(?:\\s*(?:NE|NW|SE|SW|N|S|E|W)\\b)?)", legalDescription, 1);
		for (int i=0; i<section.size(); i++) {
			List list = new ArrayList<String>();
			
			String secVal = section.get(i);
			Matcher mQVal = Pattern.compile("(?is)(\\d+)(?:\\s(NE|NW|SE|SW|N|S|E|W)\\b)").matcher(secVal);
			if (mQVal.find()){
				resultMap.put(PropertyIdentificationSetKey.QUARTER_VALUE.getKeyName(), mQVal.group(2));
				secVal = mQVal.group(1);
			}
			
			//list.add(section.get(i));
			list.add(secVal);
			list.add("");
			list.add("");
			bodySTR.add(list);
		}
		try {
			GenericFunctions2.saveSTRInMap(resultMap, bodySTR);
		} catch (Exception e) {
			e.printStackTrace();
		}  
	}
	
	public static String sortValues(String values) {
		String[] split = values.split("\\s");
		Arrays.sort(split);
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<split.length;i++)
			sb.append(split[i]).append(" ");
		return sb.toString().trim();
	}
	
	public static void parseTaxes(NodeList nodeList, ResultMap resultMap, long searchId) {
		
		String baseAmount = "0.0";
		String totalDue = "0.0";
		String priorDelinquent = "0.0";
		String amountPaid = "0.0";
		
		String year = (String)resultMap.get(TaxHistorySetKey.YEAR.getKeyName());
		if (year!=null) {
			
			NodeList taxYearList = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Tax_fvDataTax"))
				.extractAllNodesThatMatch(new HasAttributeFilter("year", year));
			
			NodeList baseAmountList1 = taxYearList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Tax_fvDataTax_FirstHalfSubtotalLabel"));
			if (baseAmountList1	.size()>0) {
				baseAmount += "+" + baseAmountList1.elementAt(0).toPlainTextString().replaceAll("[\\$\\(\\),]", "");
			}
			NodeList baseAmountList2 = taxYearList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Tax_fvDataTax_SecondHalfSubtotalLabel"));
			if (baseAmountList2.size()>0) {
				baseAmount += "+" + baseAmountList2.elementAt(0).toPlainTextString().replaceAll("[\\$\\(\\),]", "");
			}
			baseAmount = GenericFunctions.sum(baseAmount, searchId);
			
			NodeList priorDelinquentList = taxYearList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Tax_fvDataTax_PriorDelinquencies"));
			if (priorDelinquentList.size()>0) {
				priorDelinquent = priorDelinquentList.elementAt(0).toPlainTextString().replaceAll("[\\$\\(\\),]", "");
			}
			
			NodeList amountPaidList = taxYearList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Tax_fvDataTax_CollectedLabel"));
			if (amountPaidList.size()>0) {
				amountPaid = amountPaidList.elementAt(0).toPlainTextString().replaceAll("[\\$\\(\\),]", "");
			}
			
			NodeList totalDueList = taxYearList.extractAllNodesThatMatch(new TagNameFilter("span"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Tax_fvDataTax_DueLabel"));
			if (totalDueList.size()>0) {
				totalDue += "+" + totalDueList.elementAt(0).toPlainTextString().replaceAll("[\\$\\(\\),]", "");
			}
			totalDue += "+-" + amountPaid + "+-" + priorDelinquent;
			totalDue = GenericFunctions.sum(totalDue, searchId);
			
//			Date dueDate = HashCountyToIndex.getDueDate(InstanceManager.getManager()
//					.getCurrentInstance(searchId).getCurrentCommunity().getID().intValue(), 
//					"OH", "Fairfield", DType.TAX); 
//			Date now = new Date();
//	        if (now.after(dueDate)) {
//	        	priorDelinquent += "+" + totalDue;
//	        	totalDue = "0.0";
//	        	priorDelinquent = GenericFunctions.sum(priorDelinquent, searchId);
//	        }
		}
				
		resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), baseAmount);
		resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), totalDue);
		resultMap.put(TaxHistorySetKey.PRIOR_DELINQUENT.getKeyName(), priorDelinquent);
		resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), amountPaid);
	}
	
	public static void parsePropertyAppraisalSet(NodeList nodeList, ResultMap resultMap) {

		NodeList propertyAppraisal = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Base_fvDataValuation"), true);
		Node land = propertyAppraisal
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Base_fvDataValuation_AppraisedLandValueLabel"), true)
				.elementAt(0);

		if (land != null) {
			resultMap.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), land.toPlainTextString().replaceAll("[$,\\s]", ""));
		}

		Node improvements = propertyAppraisal
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Base_fvDataValuation_AppraisedBuildingValueLabel"), true)
				.elementAt(0);

		if (improvements != null) {
			resultMap.put(PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(), improvements.toPlainTextString().replaceAll("[$,\\s]", ""));
		}

		Node appraisal = propertyAppraisal
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Base_fvDataValuation_AppraisedTotalValueLabel"), true)
				.elementAt(0);

		if (appraisal != null) {
			resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(), appraisal.toPlainTextString().replaceAll("[$,\\s]", ""));
		}

		Node assessment = propertyAppraisal
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Base_fvDataValuation_AssessedTotalValueLabel"), true)
				.elementAt(0);

		if (assessment != null) {
			resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(), assessment.toPlainTextString().replaceAll("[$,\\s]", ""));
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void parseTaxInstallments(NodeList nodeList, ResultMap resultMap) {
		NodeList taxInstallmentSet = nodeList.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("id", "ctl00_ContentPlaceHolder1_Tax_fvDataTax"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("class", "formview"), true);

		Map<String, String[]> installmentsMap = new HashMap<String, String[]>();
		TableTag installmentsTable = (TableTag) taxInstallmentSet.elementAt(0);
		TableRow[] installmentsRows = installmentsTable.getRows();
		List<List> installmentsBody = new ArrayList<List>();
		List installment1Row = new ArrayList<String>();
		List installment2Row = new ArrayList<String>();
		ResultTable resultTable = new ResultTable();

		String[] installmentsHeader =
		{ TaxInstallmentSetKey.HOMESTEAD_EXEMPTION.getShortKeyName(),
				TaxInstallmentSetKey.PENALTY_AMOUNT.getShortKeyName(),
				TaxInstallmentSetKey.BASE_AMOUNT.getShortKeyName(),
				TaxInstallmentSetKey.TOTAL_DUE.getShortKeyName(),
				TaxInstallmentSetKey.STATUS.getShortKeyName(),
				TaxInstallmentSetKey.AMOUNT_PAID.getShortKeyName()
		};

		for (String s : installmentsHeader)
		{
			installmentsMap.put(s, new String[] { s, "" });
		}
		String priorCharges = "";
		String halfYearDue = "";
		String fullYearDue = "";
		String baseAmount1 = "";
		String baseAmount2 = "";
		Double priorChg =0.0;
		Double halfYearDueD = 0.0;
		Double fullYearDueD = 0.0;
		String homesteadReduction1 = "";
		String homesteadReduction2 = "";
		
		NumberFormat numberFormat = NumberFormat.getInstance();
		numberFormat.setMaximumFractionDigits(2);
		
			for (int i = 1; i < installmentsRows.length; i++) {

				TableRow r = installmentsRows[i];
				TableColumn[] cols = r.getColumns();
				if (cols.length == 2) {
					if (r.getHeaders()[0].toPlainTextString().contains("Penalties"))
					{
						String penalties1 = cols[0].toPlainTextString().replaceAll("[$,\\s]", "").replace("&nbsp;", "").trim();
						installment1Row.add(penalties1);
						
						String penalties2 = cols[1].toPlainTextString().replaceAll("[$,\\s]", "").replace("&nbsp;", "").trim();
						installment2Row.add(penalties2);
					}
					if (r.getHeaders()[0].toPlainTextString().contains("Subtotals"))
					{
						baseAmount1 = cols[0].toPlainTextString().replaceAll("[$,\\s]", "").replace("&nbsp;", "").trim();
						installment1Row.add(baseAmount1);
						
						baseAmount2 = cols[1].toPlainTextString().replaceAll("[$,\\s]", "").replace("&nbsp;", "").trim();
						installment2Row.add(baseAmount2);
					}
					
					if (r.getHeaders()[0].toPlainTextString().contains("Homestead Reduction"))
					{
						homesteadReduction1 = cols[0].toPlainTextString().replaceAll("[$,\\s()]", "").replace("&nbsp;", "").trim();
						installment1Row.add(homesteadReduction1);
						
						homesteadReduction2 = cols[1].toPlainTextString().replaceAll("[$,\\s()]", "").replace("&nbsp;", "").trim();
						installment2Row.add(homesteadReduction2);
					}

				} else //if (inst == 0)
				{
					if (r.getHeaders()[0].toPlainTextString().contains("Prior Charges"))
					{
						priorCharges = cols[0].toPlainTextString().replaceAll("[$,\\s]", "").replace("&nbsp;", "").trim();
						priorChg = Double.parseDouble(priorCharges);
					}

					if (r.getHeaders()[0].toPlainTextString().contains("Half Year Due"))
					{
						halfYearDue = cols[0].toPlainTextString().replaceAll("[$,\\s]", "").replace("&nbsp;", "").trim();
						halfYearDueD = Double.parseDouble(halfYearDue);
					}

					if (r.getHeaders()[0].toPlainTextString().contains("Full Year Due"))
					{
						fullYearDue = cols[0].toPlainTextString().replaceAll("[$,\\s]", "").replace("&nbsp;", "").trim();
						fullYearDueD = Double.parseDouble(fullYearDue);
					}
				}

				if (!halfYearDue.isEmpty() && !priorCharges.isEmpty() && installment1Row.size() <= 3) {
					Double amountDue1 = halfYearDueD - Double.parseDouble(priorCharges);
					installment1Row.add(numberFormat.format(amountDue1));

					String status = "PAID";
					if (halfYearDueD > 0.0) {
						status = "UNPAID";
					}
					installment1Row.add(status);

					if (!baseAmount1.isEmpty()) {
						Double ba1 = Double.parseDouble(baseAmount1);
						Double amountPaid1 = Math.abs(halfYearDueD - ba1 - priorChg);
						installment1Row.add(numberFormat.format(amountPaid1));
					}
				}

				if (!fullYearDue.isEmpty() && !priorCharges.isEmpty() && installment2Row.size() <= 3) {
					Double amountDue2 = fullYearDueD - priorChg - halfYearDueD;
					installment2Row.add(numberFormat.format(amountDue2));

					String status = "PAID";
					if (fullYearDueD > 0.0) {
						status = "UNPAID";
					}
					installment2Row.add(status);

					if (!baseAmount2.isEmpty()) {
						Double ba2 = Double.parseDouble(baseAmount2);
						Double amountPaid2 = Math.abs(fullYearDueD - ba2 - priorChg - halfYearDueD);
						installment2Row.add(numberFormat.format(amountPaid2));
					}
				}
			}

			if (installment1Row.size() == 6) {
				installmentsBody.add(installment1Row);
			}
			if (installment2Row.size() == 6) {
				installmentsBody.add(installment2Row);
			}

		if (!installmentsBody.isEmpty()) {
			try {
				resultTable.setHead(installmentsHeader);
				resultTable.setMap(installmentsMap);
				resultTable.setBody(installmentsBody);
			} catch (Exception e) {
				e.printStackTrace();
			}
			resultTable.setReadOnly();
			resultMap.put("TaxInstallmentSet", resultTable);
		}
	}
}
