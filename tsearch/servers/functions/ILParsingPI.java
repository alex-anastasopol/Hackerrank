package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.InstanceManager;

/**
 * @author mihaib
 * parsing functions for Illinois PI data source
  */

public class ILParsingPI {
	
	public static void tokenizeNames(ResultMap m, ArrayList<String> grantor, ArrayList<String> grantee, long searchId) throws Exception{

		String grantorString = "";
		String[] suffixes, type, otherType;
		ArrayList<List> allGrantors = new ArrayList<List>();
		
		String onlyOneGtor = "";
		String previousGrantor = "";
		for (String gtor : grantor) {

			String first = "";
			String middle = "";
			String last = "";
			gtor = repairString(gtor, m);
			
			//ANDERSON, PETER K ANDERSON, DENISE A
			if (NameUtils.isNotCompany(gtor) && gtor.matches("(?is)[A-Z]+,\\s+[A-Z]+\\s+[A-Z]\\s+[A-Z]+,\\s+[A-Z]+\\s+[A-Z]")){
				gtor = gtor.replaceAll("(?is)([A-Z]+,\\s+[A-Z]+\\s+[A-Z])\\s+([A-Z]+,\\s+[A-Z]+\\s+[A-Z])", "$1 & $2");
			}
			
			if (StringUtils.isNotEmpty(previousGrantor) && NameUtils.isNotCompany(gtor) && gtor.matches("(?is)[A-Z]+(\\s+[A-Z])?")){
				gtor = previousGrantor + " & " + gtor;
			} else if (NameUtils.isNotCompany(gtor) && gtor.matches("(?is)[A-Z]+\\s+[A-Z]")){
				onlyOneGtor = gtor;
				continue;
			}

			if (StringUtils.isNotEmpty(onlyOneGtor)){
				gtor += " & " + onlyOneGtor;
				onlyOneGtor = "";
			}
			
			if (NameUtils.isCompany(gtor)){
				gtor = gtor.replaceAll(",\\s*$", "");
				gtor = gtor.replaceAll("^,\\s*", "");
				last = gtor.trim();
			} else {
				previousGrantor = gtor;
				gtor = gtor.replaceAll("(?is)\\s+([IV]{1,3}),(.*)", ", $2 $1");
				int pos = 0;
				if ((pos = gtor.indexOf(',')) > 0){
					last = gtor.substring(0, pos);
					first = gtor.substring(pos + 1).trim();
					String words[] = first.split(" ");
					if (words.length > 1) {
						first = words[0];
						for (int i = 1; i < words.length; i++){
							middle += words[i] + " ";
						}
						middle = middle.trim();
					}
				} else{
					last = gtor;
				}
			}

			String names[] = { first, middle, last, "", "", "" };
			if (NameUtils.isNotCompany(gtor) && gtor.matches("(?is)[A-Z]+\\s+[A-Z]\\.\\s+[A-Z]+\\s+AND\\s+[A-Z]+\\s+[A-Z]\\.\\s+[A-Z]+")){
				gtor = gtor.replaceAll("\\s+AND\\s+", "##@@##");
			}
			if (gtor.contains("##@@##")){
				String[] gtors = gtor.split("##@@##");
				for (String gt : gtors) {
					names = StringFormats.parseNameNashville(gt, true);
					
					if (gt.contains(" IJT") || (NameUtils.isNotCompany(gt) && !gt.contains(","))){
						gt = gt.replaceAll("(?is)\\bIJT\\b", "");
						names = StringFormats.parseNameDesotoRO(gt, true);
					}
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
					
					GenericFunctions.addOwnerNames(gt, names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
								NameUtils.isCompany(names[5]), allGrantors);
				}
			} else if (NameUtils.isNotCompany(gtor)) {
					names = StringFormats.parseNameNashville(gtor, true);
						
					if (gtor.contains(" IJT") || (NameUtils.isNotCompany(gtor) && !gtor.contains(","))){
						gtor = gtor.replaceAll("(?is)\\bIJT\\b", "");
						names = StringFormats.parseNameDesotoRO(gtor, true);
					}
					
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
					
					GenericFunctions.addOwnerNames(gtor, names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
								NameUtils.isCompany(names[5]), allGrantors);
				} else if (NameUtils.isCompany(gtor)) {
					names = StringFormats.parseNameNashville(gtor, true);
						
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
					
					GenericFunctions.addOwnerNames(gtor, names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
								NameUtils.isCompany(names[5]), allGrantors);
				}

			grantorString += first + " " + middle + " " + last + " / ";
		}
		m.put(SaleDataSetKey.GRANTOR.getKeyName(), grantorString.replaceAll("\\s*##@@##\\s*", " and ").replaceAll("\\s*/\\s*$", ""));
		m.put("GrantorSet", GenericFunctions.storeOwnerInSet(allGrantors, true));
			
		String granteeString = "";
		
		String onlyOneGtee = "";
		String previousGrantee = "";
		
		ArrayList<List> allGrantees = new ArrayList<List>();
		for (String gtee : grantee) {
			String first = "";
			String middle = "";
			String last = "";
			gtee = repairString(gtee, m);
			
			//ANDERSON, PETER K ANDERSON, DENISE A
			if (NameUtils.isNotCompany(gtee) && gtee.matches("(?is)[A-Z]+,\\s+[A-Z]+\\s+[A-Z]\\s+[A-Z]+,\\s+[A-Z]+\\s+[A-Z]")){
				gtee = gtee.replaceAll("(?is)([A-Z]+,\\s+[A-Z]+\\s+[A-Z])\\s+([A-Z]+,\\s+[A-Z]+\\s+[A-Z])", "$1 & $2");
			}
			if (StringUtils.isNotEmpty(previousGrantee) && NameUtils.isNotCompany(gtee) && gtee.matches("(?is)[A-Z]+(\\s+[A-Z])?")){
				gtee = previousGrantee + " & " + gtee;
			} else if (NameUtils.isNotCompany(gtee) && gtee.matches("(?is)[A-Z]+\\s+[A-Z]")){
				onlyOneGtee = gtee;
				continue;
			}

			if (StringUtils.isNotEmpty(onlyOneGtee)){
				gtee += " & " + onlyOneGtee;
				onlyOneGtee = "";
			}
			
			if (NameUtils.isCompany(gtee)){
				gtee = gtee.replaceAll(",\\s*$", "");
				gtee = gtee.replaceAll("^,\\s*", "");
				last = gtee.trim();
			} else {
				previousGrantee = gtee;
				int pos = 0;
				if ((pos = gtee.indexOf(',')) > 0){
					last = gtee.substring(0, pos);
					first = gtee.substring(pos + 1).trim();
					String words[] = first.split(" ");
					if (words.length > 1){
						first = words[0];
						for (int i = 1; i < words.length; i++){
							middle += words[i] + " ";
						}
						middle = middle.trim();
					}
				} else{
					last = gtee;
				}
			}

			String names[] = { first, middle, last, "", "", "" };
			
			if (NameUtils.isNotCompany(gtee) && gtee.matches("(?is)[A-Z]+\\s+[A-Z]\\.\\s+[A-Z]+\\s+AND\\s+[A-Z]+\\s+[A-Z]\\.\\s+[A-Z]+")){
				gtee = gtee.replaceAll("\\s+AND\\s+", "##@@##");
			}
			
			if (gtee.contains("##@@##")){
				String[] gtees = gtee.split("##@@##");
				for (String gt : gtees) {
					names = StringFormats.parseNameNashville(gt, true);
					
					if (gt.contains(" IJT") || (NameUtils.isNotCompany(gt) && !gt.contains(","))){
						gt = gt.replaceAll("(?is)\\bIJT\\b", "");
						names = StringFormats.parseNameDesotoRO(gt, true);
					}
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
					
					GenericFunctions.addOwnerNames(gt, names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
								NameUtils.isCompany(names[5]), allGrantees);
				}
			} else if (NameUtils.isNotCompany(gtee)) {
					names = StringFormats.parseNameNashville(gtee, true);
					if (gtee.contains(" IJT") || (NameUtils.isNotCompany(gtee) && !gtee.contains(","))){
						gtee = gtee.replaceAll("(?is)\\bIJT\\b", "");
						names = StringFormats.parseNameDesotoRO(gtee, true);
					}
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);

					GenericFunctions.addOwnerNames(gtee, names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), allGrantees);
				} else if (NameUtils.isCompany(gtee)) {
					names = StringFormats.parseNameNashville(gtee, true);
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);

					GenericFunctions.addOwnerNames(gtee, names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), allGrantees);
				}

			granteeString += first + " " + middle + " " + last + " / ";

		}
		m.put(SaleDataSetKey.GRANTEE.getKeyName(), granteeString.replaceAll("\\s*##@@##\\s*", " and ").replaceAll("\\s*/\\s*$", ""));
		m.put("GranteeSet", GenericFunctions.storeOwnerInSet(allGrantees, true));
		
		GenericFunctions1.setGranteeLanderTrustee2(m, searchId,true);
		
	}
	
	public static String repairString(String name, ResultMap resultMap){
		
		if (ro.cst.tsearch.utils.StringUtils.isEmpty(name))
			return "";
		
		name = name.replaceAll("(?is)\\bD\\s*/?\\s*B\\s*/?\\s*A\\b", "##@@##");
		name = name.replaceAll("(?is),? AS RECEIVER\\s*FOR\\b", "##@@##");
		
		if (name.matches("(?is)([^&]+)\\s*&\\s*([A-Z]+\\s*,).*")){
			name = name.replaceAll("(?is)([^&]+)\\s*&\\s*([A-Z]+\\s*,.*)", "$1##@@##$2");
		}

		name = name.replaceAll("(?is)\\bAKA\\s*$", "");
		name = name.replaceAll("(?is)\\bAKA\\b", "&");
		name = name.replaceAll("(?is)\\s+ET\\s+AL\\b", " ETAL ");
		name = name.replaceAll("(?is)\\s+ET\\s+UX\\b", " ETUX ");
		name = name.replaceAll("(?is)\\s+ET\\s+VIR\\b", " ETVIR ");
		
		//name = name.replaceAll("(?is)\\bTR(\\s*#)", "TRUST$1");
		name = name.replaceAll("(?is)\\b\\(TRS?\\)\\b", "TRUSTEE##@@##");
		name = name.replaceAll("(?is)TR\\s*#\\s*[\\d-]+", "");
		name = name.replaceAll("(?is)\\bTUT\\b", "TRUST");
		name = name.replaceAll("(?is)\\bT/U(/T/A)?\\b", "TRUSTEE");
		name = name.replaceAll("(?is)\\b(BK\\s*&)\\s*TR\\b", "$1 TRUST");
		name = name.replaceAll("(?is)\\b(BANKERS)\\s+TR\\b", "$1 TRUST");
		name = name.replaceAll("(?is)\\bDTD\\s+[A-Z]+\\s*/\\d+\\s*/\\s*\\d+", "");
		//DTD6/7/74
		name = name.replaceAll("(?is)DTD\\s*\\d+\\s*/\\d+\\s*/\\s*\\d+", "");
		
		name = name.replaceAll("(?is)&/OR\\s*$", "");
		name = name.replaceAll("(?is)\\.{3}", "");
		name = name.replaceAll("(?is), HIS WF\\b", "");
		name = name.replaceAll("(?is), S\\.C\\.", "");
		name = name.replaceAll("(?is), S\\.C\\b", "");
		name = name.replaceAll("(?is), M\\.D\\.", "");
		name = name.replaceAll("(?is)\\bWF\\b", "&");
		name = name.replaceAll("(?is)\\b[F|A]/?K/?A\\b", "##@@##");
		name = name.replaceAll("(?is)\\b(PE|RE|DIV|SINGLE|QUALIFIED|ATTY-IN-FACT)\\b", "");//for DIvorce, probably PEtitioner, REclamant
		name = name.replaceAll("(?is)\\b(ASSIGNEE FOR THE BENEFIT OF|HW (TEN|IJT)|HW)\\b", "");
		name = name.replaceAll("(?is)\\b((\\d+(ST|ND|RD|TH) PARTY )?NOT SHOWN|ETC|UNKNOWN OWNERS)\\b", "");
		
		name = name.replaceAll("(?is)\\bNKA\\s*$", "");
		
		//WILSON, JOHN P JR P J
		name = name.replaceAll("(?is)\\b([A-Z]\\s+(?:JR|SR))\\s+\\1", "$1");
		
		name = name.replaceAll("(?is)[\\)\\(]+", "");
		
		//MALICK, ELLEN G. NKA ROBINSON,
		if (name.trim().matches("(?is)([A-Z]+,)\\s+([A-Z]+\\s+[A-Z]\\.)\\s+NKA\\s+([A-Z]+,)")){
			name = name.replaceAll("(?is)([A-Z]+,)\\s+([A-Z]+\\s+[A-Z]\\.)\\s+NKA\\s+([A-Z]+,)", "$1 $2##@@##$3 $2");
		}
		
		Pattern p = Pattern.compile("(?is)\\b(\\d+)\\s*(CH)\\s*(\\d+)\\b");
		Matcher ma = p.matcher(name);
		if (ma.find()){
			List<List> body = new ArrayList<List>();
			
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1) + ma.group(2) + StringUtils.stripStart(ma.group(3), "0"));
			line.add("");
			line.add("");
			line.add("");
			body.add(line);
			
			name = name.replaceAll(ma.group(0), "");
			
			ResultTable rt = (ResultTable) resultMap.get("SaleDataSet");
			if (rt == null){
				rt = new ResultTable();
			} else {
				String[][] bodyRT = rt.getBody();
				if (bodyRT.length != 0) {
					for (int i = 0; i < bodyRT.length; i++) {
						line = new ArrayList<String>();
						for (int j = 0; j < rt.getHead().length; j++) {
							line.add(bodyRT[i][j]);
						}
						body.add(line);
					}
				}
			}
				
			if (body != null && body.size() > 0) {
				rt = new ResultTable();
				String[] header = {"InstrumentNumber", "Book", "Page", "Year"};
				rt = GenericFunctions2.createResultTable(body, header);
				resultMap.put("CrossRefSet", rt);
			}
		}
		
		name = name.replaceAll("(?is)\\s*&\\s*$", "");
		
		return name.trim();
	}

	public static void parsingRemarks(ResultMap resultMap, long searchId) throws Exception{
	
		String remarks = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		
		if (StringUtils.isEmpty(remarks)){
			return;
		}
		
		remarks = remarks.replaceAll("(?is)\\?", "");
		remarks = remarks.replaceAll("(?is)(\\d+)(TO)\\b", "$1 $2");
		remarks = remarks.replaceAll("(?is)(\\d+)\\s+(TO)\\s+(\\d+)", "$1, $3");
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
		
		boolean isCookCounty = false;
		boolean isDeKalbCounty = false;
		boolean isDuPageCounty = false;
		boolean isKaneCounty = false;
		boolean isKendallCounty = false;
		boolean isLakeCounty = false;
		boolean isMcHenryCounty = false;
		boolean isWillCounty = false;
		
		if ("IL".equalsIgnoreCase(crtState)){
			if ("cook".equalsIgnoreCase(crtCounty)){
				isCookCounty = true;
			} else if ("dekalb".equalsIgnoreCase(crtCounty)){
				isDeKalbCounty = true;
			} else if ("du page".equalsIgnoreCase(crtCounty)){
				isDuPageCounty = true;
			} else if ("kane".equalsIgnoreCase(crtCounty)){
				isKaneCounty = true;
			} else if ("kendall".equalsIgnoreCase(crtCounty)){
				isKendallCounty = true;
			} else if ("lake".equalsIgnoreCase(crtCounty)){
				isLakeCounty = true;
			} else if ("mchenry".equalsIgnoreCase(crtCounty)){
				isMcHenryCounty = true;
			} else if ("will".equalsIgnoreCase(crtCounty)){
				isWillCounty = true;
			}
		}
		List<List> body = new ArrayList<List>();
		List<String> line = null;
		Pattern p = Pattern.compile("(?is)\\bRef:\\s*(.+)");
		Matcher ma = p.matcher(remarks);
		
		if (ma.find()){
			String[] references = ma.group(1).split("\\s*;\\s*");			
			for (String ref : references){
				line = new ArrayList<String>();
				ref = ref.replaceAll("(?is)\\A\\s*(\\d{2})\\s+(\\d{3})\\s+(\\d{2,4})\\s*$", "$1$2$3");
				ref = ref.replaceAll("(?is)\\A\\s*(\\d{2})\\s+(\\d+)\\s*$", "$1$2");
				ref = ref.replaceAll("(?is)\\A\\s*(\\d{4,6})\\s+(\\d{2})\\s*$", "$1$2");
				String[] items = ref.trim().split("\\s+");
				if (items.length == 2){//6424400 01122009
					if (items[1].length() == 8){//it's a date
						String instrNo = items[0];
						String year = "";
						if (isDeKalbCounty) {
							if (items[1].length() > 4){
								year = items[1].substring(items[1].length() - 4);
								
								instrNo = year.concat(StringUtils.leftPad(instrNo, 6, "0"));

							}
						} else if (isDuPageCounty) {
							if (items[1].length() > 4){
								year = items[1].substring(items[1].length() - 4);
								
								instrNo = "R" + year + StringUtils.leftPad(instrNo, 6, "0");
							}
						} else if (isKaneCounty) {
							if (items[1].length() > 4){
								year = items[1].substring(items[1].length() - 4);					
								
								int yearInt = Integer.parseInt(year);
								
								if (yearInt >= 1999) {
									instrNo = year + "K" + StringUtils.leftPad(instrNo, 6, "0");
								} else if (yearInt > 1989 && yearInt < 1999){
									instrNo = year.substring(2) + "K" + StringUtils.leftPad(instrNo, 6, "0");
								}
							}
						} else if (isKendallCounty) {
							if (items[1].length() > 4){
								year = items[1].substring(items[1].length() - 4);						
								
								int yearInt = Integer.parseInt(year);
								
								if (yearInt > 1999) {
									instrNo = year + StringUtils.leftPad(instrNo, 8, "0");
								} else {
									instrNo = year.substring(2) + StringUtils.leftPad(instrNo, 5, "0");
								}
							}
						} else if (isCookCounty){
							if (instrNo.length() == 9 || instrNo.length() == 7){
								instrNo = "0" + instrNo;
							}
						} else if (isMcHenryCounty) {
							if (items[1].length() > 4){
								year = items[1].substring(items[1].length() - 4);
								
								instrNo = year.concat("R").concat(StringUtils.leftPad(instrNo, 7, "0"));

							}
						} else if (isWillCounty) {
							if (items[1].length() > 4){
								year = items[1].substring(items[1].length() - 4);
								if (year.startsWith("2")){
									instrNo = "R" + year + StringUtils.leftPad(instrNo, 6, "0");
								} else {
									instrNo = "R" + year.substring(year.length() - 2) + StringUtils.leftPad(instrNo, 6, "0");
								}
							}
						} else{
							if (instrNo.length() > 7 && instrNo.matches("\\d+")){
								instrNo = instrNo.substring(instrNo.length() - 6);
							}
							if (items[1].length() > 4){
								year = items[1].substring(items[1].length() - 4);
							}
						}
						
						line.add(instrNo);
						line.add("");
						line.add("");
						line.add(year);
						body.add(line);
					} else {
						String instrNo = items[0] + items[1];
						String year = "";
						
						if (isDuPageCounty) {
							if (instrNo.length() > 7 && instrNo.matches("\\d+")){
								year = instrNo.substring(0, 2);
							}
								
							if (StringUtils.isNotEmpty(year)){
								if (year.startsWith("0") || year.startsWith("1")){
									year = "20" + year;
									instrNo = "R20" + instrNo;
								} else {
									year = "19" + year;
									instrNo = "R19" + instrNo;
								}
							}
						} else if (isKaneCounty) {
							if (instrNo.length() > 7 && instrNo.matches("\\d+")){
								year = instrNo.substring(0, 2);
								instrNo = instrNo.substring(2);
							}						
							
							if (year.startsWith("0") || year.startsWith("1")){
								year = "20" + year;
							} else{
								year = "19" + year;
							}
							int yearInt = Integer.parseInt(year);
								
							if (yearInt >= 1999) {
								instrNo = year + "K" + StringUtils.leftPad(instrNo, 6, "0");
							} else if (yearInt > 1989 && yearInt < 1999){
								instrNo = year.substring(2) + "K" + StringUtils.leftPad(instrNo, 6, "0");
							}
						} else if (isKendallCounty){
							if (instrNo.length() > 7 && instrNo.matches("\\d+")){
								year = instrNo.substring(0, 2);
								instrNo = instrNo.substring(2);
							}
							if (StringUtils.isNotEmpty(year)){
								if (year.startsWith("0") || year.startsWith("1")){
									year = "20" + year;
									instrNo = year + StringUtils.leftPad(instrNo, 8, "0");
								} else {
									instrNo = year + StringUtils.leftPad(instrNo, 5, "0");
									year = "19" + year;
								}
							}
						} else if (isCookCounty){
							if (instrNo.length() == 9 || instrNo.length() == 7){
								instrNo = "0" + instrNo;
							}
						} else if (isWillCounty){
							if (instrNo.length() > 7 && instrNo.matches("\\d+")){
								year = instrNo.substring(0, 2);
							}
							if (StringUtils.isNotEmpty(year)){
								if (year.startsWith("0") || year.startsWith("1")){
									year = "20" + year;
									instrNo = "R20" + instrNo;
								} else {
									year = "19" + year;
									instrNo = "R" + instrNo;
								}
							}
						} else {
							if (instrNo.length() > 7 && instrNo.matches("\\d+")){
								year = instrNo.substring(0, 2);
								instrNo = instrNo.substring(instrNo.length() - 6);
							}
							if (StringUtils.isNotEmpty(year)){
								if (year.startsWith("0") || year.startsWith("1")){
									year = "20" + year;
								} else {
									year = "19" + year;
								}
							}
							instrNo = StringUtils.stripStart(instrNo, "0");
						}
						
						line.add(instrNo);
						line.add("");
						line.add("");
						line.add(year);
						body.add(line);
					}
					
				} else {//02230188
					String instrNo = items[0];
					String year = "";
					if (isDeKalbCounty){
						if (instrNo.length() > 7 && instrNo.matches("\\d+")){
							year = instrNo.substring(0, 2);
						}
						if (StringUtils.isNotEmpty(year)){
							if (year.startsWith("0") || year.startsWith("1")){
								year = "20" + year;
								instrNo = "20" + instrNo;
							} else {
								year = "19" + year;
								instrNo = "19" + instrNo;
							}
						}
					} else if (isDuPageCounty){
						if (instrNo.length() > 7 && instrNo.matches("\\d+")){
							year = instrNo.substring(0, 2);
						}
						if (StringUtils.isNotEmpty(year)){
							if (year.startsWith("0") || year.startsWith("1")){
								year = "20" + year;
								instrNo = "R20" + instrNo;
							} else {
								year = "19" + year;
								instrNo = "R19" + instrNo;
							}
						}
					} else if (isKaneCounty) {
						if (instrNo.length() > 6 && instrNo.matches("\\d+")){
							year = instrNo.substring(0, 2);
							instrNo = instrNo.substring(2);
						}						
						
						if (year.startsWith("0") || year.startsWith("1")){
							year = "20" + year;
						} else{
							year = "19" + year;
						}
						int yearInt = Integer.parseInt(year);
							
						if (yearInt >= 1999) {
							instrNo = year + "K" + StringUtils.leftPad(instrNo, 6, "0");
						} else if (yearInt > 1989 && yearInt < 1999){
							instrNo = year.substring(2) + "K" + StringUtils.leftPad(instrNo, 6, "0");
						}
					} else if (isKendallCounty){
						if (instrNo.length() > 7 && instrNo.matches("\\d+")){
							year = instrNo.substring(0, 2);
							instrNo = instrNo.substring(2);
						}
						if (StringUtils.isNotEmpty(year)){
							if (year.startsWith("0") || year.startsWith("1")){
								year = "20" + year;
								instrNo = year + StringUtils.leftPad(instrNo, 8, "0");
							} else {
								instrNo = year + StringUtils.leftPad(instrNo, 5, "0");
								year = "19" + year;
							}
						}
					} else if (isLakeCounty){
						if (instrNo.length() > 7 && instrNo.matches("\\d+")){
							instrNo = StringUtils.stripStart(instrNo, "0");
						}
					} else if (isCookCounty){
						if (instrNo.length() == 9 || instrNo.length() == 7){
							instrNo = "0" + instrNo;
						}
					} else if (isMcHenryCounty) {
						year = instrNo.substring(0, 2);
						instrNo = instrNo.substring(2);
						if (year.startsWith("0") || year.startsWith("1")){
							year = "20" + year;
							instrNo = year + StringUtils.leftPad(instrNo, 7, "0");
						} else {
							year = "19" + year;
							instrNo = year + StringUtils.leftPad(instrNo, 7, "0");
						}
						instrNo = instrNo.replaceFirst("(?is)(\\d{4})(\\d+)", "$1R$2");
					} else if (isWillCounty){
						if (instrNo.length() > 6 && instrNo.matches("\\d+")){
							year = instrNo.substring(0, 2);
						}
						if (StringUtils.isNotEmpty(year)){
							if (year.startsWith("0") || year.startsWith("1")){
								year = "20" + year;
								instrNo = "R20" + instrNo;
							} else {
								year = "19" + year;
								instrNo = "R" + instrNo;
							}
						}
					} else {
						if (instrNo.length() > 7 && instrNo.matches("\\d+")){
							year = instrNo.substring(0, 2);
							instrNo = instrNo.substring(instrNo.length() - 6);
							
						}
						if (StringUtils.isNotEmpty(year)){
							if (year.startsWith("0") || year.startsWith("1")){
								year = "20" + year;
							} else {
								year = "19" + year;
							}
						}
						instrNo = StringUtils.stripStart(instrNo, "0");
					}
					
					line.add(instrNo);
					line.add("");
					line.add("");
					line.add(year);
					body.add(line);

				}
			}
			remarks = remarks.replaceFirst(ma.group(0), "");
		}
		
		p = Pattern.compile("(?is)\\bDOC\\s*#?(?:\\s*S)?\\s*(\\d[^;]+)");
		ma = p.matcher(remarks);
		
		if (ma.find()){
			String[] references = ma.group(1).split("\\s*,\\s*");
			for (String ref : references){
				line = new ArrayList<String>();
			
				ref = ref.replaceAll("(?is)\\s+", "");
				String instrNo = ref.trim();
				String year = "";
				if (isDeKalbCounty){
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						year = instrNo.substring(0, 2);
					}
					if (StringUtils.isNotEmpty(year)){
						if (year.startsWith("0") || year.startsWith("1")){
							year = "20" + year;
							instrNo = "20" + instrNo;
						} else {
							year = "19" + year;
							instrNo = "19" + instrNo;
						}
					}
				} else if (isDuPageCounty){
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						year = instrNo.substring(0, 2);
					}
					if (StringUtils.isNotEmpty(year)){
						if (year.startsWith("0") || year.startsWith("1")){
							year = "20" + year;
							instrNo = "R20" + instrNo;
						} else {
							year = "19" + year;
							instrNo = "R19" + instrNo;
						}
					}
				} else if (isKaneCounty) {
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						year = instrNo.substring(0, 2);
						instrNo = instrNo.substring(2);
					}						
					
					if (year.startsWith("0") || year.startsWith("1")){
						year = "20" + year;
					} else{
						year = "19" + year;
					}
					
					int yearInt = Integer.parseInt(year);
						
					if (yearInt >= 1999) {
						instrNo = year + "K" + StringUtils.leftPad(instrNo, 6, "0");
					} else if (yearInt > 1989 && yearInt < 1999){
						instrNo = year.substring(2) + "K" + StringUtils.leftPad(instrNo, 6, "0");
					}
				} else if (isKendallCounty){
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						year = instrNo.substring(0, 2);
						instrNo = instrNo.substring(2);
					}
					if (StringUtils.isNotEmpty(year)){
						if (year.startsWith("0") || year.startsWith("1")){
							year = "20" + year;
							instrNo = year + StringUtils.leftPad(instrNo, 8, "0");
						} else {
							instrNo = year + StringUtils.leftPad(instrNo, 5, "0");
							year = "19" + year;
						}
					}
				} else if (isLakeCounty){
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						instrNo = StringUtils.stripStart(instrNo, "0");
					}
				} else if (isCookCounty){
					if (instrNo.length() == 9 || instrNo.length() == 7){
						instrNo = "0" + instrNo;
					}
				} else if (isMcHenryCounty) {
					year = instrNo.substring(0, 2);
					instrNo = instrNo.substring(2);
					
					if (year.startsWith("0") || year.startsWith("1")){
						year = "20" + year;
						instrNo = year + StringUtils.leftPad(instrNo, 7, "0");
					} else {
						year = "19" + year;
						instrNo = year + StringUtils.leftPad(instrNo, 7, "0");
					}
					instrNo = instrNo.replaceFirst("(?is)(\\d{4})(\\d+)", "$1R$2");
				} else if (isWillCounty){
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						year = instrNo.substring(0, 2);
					}
					if (StringUtils.isNotEmpty(year)){
						if (year.startsWith("0") || year.startsWith("1")){
							year = "20" + year;
							instrNo = "R20" + instrNo;
						} else {
							year = "19" + year;
							instrNo = "R" + instrNo;
						}
					}
				} else{
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						year = instrNo.substring(0, 2);
						instrNo = instrNo.substring(instrNo.length() - 6);
					}
					
					if (StringUtils.isNotEmpty(year)){
						if (year.startsWith("0") || year.startsWith("1")){
							year = "20" + year;
						} else {
							year = "19" + year;
						}
					}
				}
				line.add(instrNo);
				line.add("");
				line.add("");
				line.add(year);
				body.add(line);
			}
			remarks = remarks.replaceFirst(ma.group(0), "");
		}
		
		p = Pattern.compile("(?is)\\b(\\d{8});");
		ma = p.matcher(remarks);
		
		if (ma.find()){
			
			while (ma.find()){
				line = new ArrayList<String>();
			
				String instrNo = ma.group(1).trim();
				String year = "";
				if (isDeKalbCounty){
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						year = instrNo.substring(0, 2);
					}
					if (StringUtils.isNotEmpty(year)){
						if (year.startsWith("0") || year.startsWith("1")){
							year = "20" + year;
							instrNo = "20" + instrNo;
						} else {
							year = "19" + year;
							instrNo = "19" + instrNo;
						}
					}
				} else if (isDuPageCounty){
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						year = instrNo.substring(0, 2);
					}
					if (StringUtils.isNotEmpty(year)){
						if (year.startsWith("0") || year.startsWith("1")){
							year = "20" + year;
							instrNo = "R20" + instrNo;
						} else {
							year = "19" + year;
							instrNo = "R19" + instrNo;
						}
					}
				} else if (isKaneCounty) {
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						year = instrNo.substring(0, 2);
						instrNo = instrNo.substring(2);
					}						
					
					if (year.startsWith("0") || year.startsWith("1")){
						year = "20" + year;
					} else{
						year = "19" + year;
					}
					
					int yearInt = Integer.parseInt(year);
						
					if (yearInt >= 1999) {
						instrNo = year + "K" + StringUtils.leftPad(instrNo, 6, "0");
					} else if (yearInt > 1989 && yearInt < 1999){
						instrNo = year.substring(2) + "K" + StringUtils.leftPad(instrNo, 6, "0");
					}
				} else if (isKendallCounty){
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						year = instrNo.substring(0, 2);
						instrNo = instrNo.substring(2);
					}
					if (StringUtils.isNotEmpty(year)){
						if (year.startsWith("0") || year.startsWith("1")){
							year = "20" + year;
							instrNo = year + StringUtils.leftPad(instrNo, 8, "0");
						} else {
							instrNo = year + StringUtils.leftPad(instrNo, 5, "0");
							year = "19" + year;
						}
					}
				} else if (isLakeCounty){
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						instrNo = StringUtils.stripStart(instrNo, "0");
					}
				} else if (isCookCounty){
					if (instrNo.length() == 9 || instrNo.length() == 7){
						instrNo = "0" + instrNo;
					}
				} else if (isMcHenryCounty) {
					year = instrNo.substring(0, 2);
					instrNo = instrNo.substring(2);
					
					if (year.startsWith("0") || year.startsWith("1")){
						year = "20" + year;
						instrNo = year + StringUtils.leftPad(instrNo, 7, "0");
					} else {
						year = "19" + year;
						instrNo = year + StringUtils.leftPad(instrNo, 7, "0");
					}
					instrNo = instrNo.replaceFirst("(?is)(\\d{4})(\\d+)", "$1R$2");
				} else if (isWillCounty){
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						year = instrNo.substring(0, 2);
					}
					if (StringUtils.isNotEmpty(year)){
						if (year.startsWith("0") || year.startsWith("1")){
							year = "20" + year;
							instrNo = "R20" + instrNo;
						} else {
							year = "19" + year;
							instrNo = "R" + instrNo;
						}
					}
				} else{
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						year = instrNo.substring(0, 2);
						instrNo = instrNo.substring(instrNo.length() - 6);
					}
					
					if (StringUtils.isNotEmpty(year)){
						if (year.startsWith("0") || year.startsWith("1")){
							year = "20" + year;
						} else {
							year = "19" + year;
						}
					}
				}
				line.add(instrNo);
				line.add("");
				line.add("");
				line.add(year);
				body.add(line);
				remarks = remarks.replaceFirst(ma.group(0), "");
			}
		}
		remarks = remarks.replaceAll("(?is)&\\s*(TO)\\b", "$1");
		p = Pattern.compile("(?is)\\bSUBORDINATES\\s*([\\d\\s]+\\s*(?:TO\\s*[\\d\\s]+)?\\s*(?:TO\\s*[\\d\\s]+)?)");
		ma = p.matcher(remarks);
		
		if (ma.find()){
			String[] references = ma.group(1).split("\\s*TO\\s*");
			for (String ref : references){
				ref = ref.replaceAll("(?is)\\A\\s*(\\d{2})\\s+(\\d{3})\\s+(\\d{2,3})\\s*$", "$1$2$3");
				ref = ref.replaceAll("(?is)\\A\\s*(\\d{2})\\s+(\\d+)\\s*$", "$1$2");
				ref = ref.replaceAll("(?is)\\A\\s*(\\d{4,6})\\s+(\\d{2})\\s*$", "$1$2");
				line = new ArrayList<String>();
			
				String instrNo = ref.trim();
				String year = "";
				if (isDeKalbCounty){
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						year = instrNo.substring(0, 2);
					}
					if (StringUtils.isNotEmpty(year)){
						if (year.startsWith("0") || year.startsWith("1")){
							year = "20" + year;
							instrNo = "20" + instrNo;
						} else {
							year = "19" + year;
							instrNo = "19" + instrNo;
						}
					}
				} else if (isDuPageCounty){
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						year = instrNo.substring(0, 2);
					}
					if (StringUtils.isNotEmpty(year)){
						if (year.startsWith("0") || year.startsWith("1")){
							year = "20" + year;
							instrNo = "R20" + instrNo;
						} else {
							year = "19" + year;
							instrNo = "R19" + instrNo;
						}
					}
				} else if (isKaneCounty) {
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						year = instrNo.substring(0, 2);
						instrNo = instrNo.substring(2);
					}						
					
					if (year.startsWith("0") || year.startsWith("1")){
						year = "20" + year;
					} else{
						year = "19" + year;
					}
					
					int yearInt = Integer.parseInt(year);
						
					if (yearInt >= 1999) {
						instrNo = year + "K" + StringUtils.leftPad(instrNo, 6, "0");
					} else if (yearInt > 1989 && yearInt < 1999){
						instrNo = year.substring(2) + "K" + StringUtils.leftPad(instrNo, 6, "0");
					}
				} else if (isKendallCounty){
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						year = instrNo.substring(0, 2);
						instrNo = instrNo.substring(2);
					}
					if (StringUtils.isNotEmpty(year)){
						if (year.startsWith("0") || year.startsWith("1")){
							year = "20" + year;
							instrNo = year + StringUtils.leftPad(instrNo, 8, "0");
						} else {
							instrNo = year + StringUtils.leftPad(instrNo, 5, "0");
							year = "19" + year;
						}
					}
				} else if (isLakeCounty){
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						instrNo = StringUtils.stripStart(instrNo, "0");
					}
				} else if (isCookCounty){
					if (instrNo.length() == 9 || instrNo.length() == 7){
						instrNo = "0" + instrNo;
					}
				} else if (isMcHenryCounty) {
					year = instrNo.substring(0, 2);
					instrNo = instrNo.substring(2);
					
					if (year.startsWith("0") || year.startsWith("1")){
						year = "20" + year;
						instrNo = year + StringUtils.leftPad(instrNo, 7, "0");
					} else {
						year = "19" + year;
						instrNo = year + StringUtils.leftPad(instrNo, 7, "0");
					}
					instrNo = instrNo.replaceFirst("(?is)(\\d{4})(\\d+)", "$1R$2");
				} else if (isWillCounty){
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						year = instrNo.substring(0, 2);
					}
					if (StringUtils.isNotEmpty(year)){
						if (year.startsWith("0") || year.startsWith("1")){
							year = "20" + year;
							instrNo = "R20" + instrNo;
						} else {
							year = "19" + year;
							instrNo = "R" + instrNo;
						}
					}
				} else{
					if (instrNo.length() > 7 && instrNo.matches("\\d+")){
						year = instrNo.substring(0, 2);
						instrNo = instrNo.substring(instrNo.length() - 6);
					}
					
					if (StringUtils.isNotEmpty(year)){
						if (year.startsWith("0") || year.startsWith("1")){
							year = "20" + year;
						} else {
							year = "19" + year;
						}
					}
				}
				line.add(instrNo);
				line.add("");
				line.add("");
				line.add(year);
				body.add(line);
			}
			remarks = remarks.replaceFirst(ma.group(0), "");
		}
		
		p = Pattern.compile("(?is)\\b(\\d+)\\s*(CH)\\s*(\\d+)\\b");
		ma = p.matcher(remarks);
		if (ma.find()){
			line = new ArrayList<String>();
			line.add(ma.group(1) + ma.group(2) + StringUtils.stripStart(ma.group(3), "0"));
			line.add("");
			line.add("");
			line.add("");
			body.add(line);
			remarks = remarks.replaceFirst(ma.group(0), "");
		}
		
		if(!body.isEmpty()) {
			String[] header = { "InstrumentNumber", "Book", "Page", "Year" };
			resultMap.put("CrossRefSet", GenericFunctions2.createResultTable(body, header));
		}
		
		resultMap.removeTempDef();

	}
	
	
	 public static String correctInstrumentNumber(String crtState, String crtCounty, String serverDocType, String instrNo, String recordingDate){
		 
		 if ("IL".equalsIgnoreCase(crtState)){
				if (!"starter".equalsIgnoreCase(serverDocType) && !"open order".equalsIgnoreCase(serverDocType)
						&& StringUtils.isNumeric(instrNo)){
					if ("cook".equalsIgnoreCase(crtCounty)){
						if (/*StringUtils.isNotEmpty(recordingDate) &&*/ StringUtils.isNotEmpty(instrNo)){
	//						String recordYear = recordingDate.substring(0, 4);
	//						int year = Integer.parseInt(recordYear);
	//						
	//						if (year >=1999 && year < 2004) {
	//							instrNo = "00" + instrNo;
	//						} else if (year > 2004 && year < 2010){
	//							instrNo = "0" + instrNo;
	//						}
							if (instrNo.length() != 8){
								instrNo = "0" + instrNo;
							}
						}
					} else if ("dekalb".equalsIgnoreCase(crtCounty)){
						if (StringUtils.isNotEmpty(recordingDate) && StringUtils.isNotEmpty(instrNo)){
							String recordYear = recordingDate.substring(0, 4);
							
							if (recordYear.startsWith("1")){
								instrNo = recordYear + StringUtils.leftPad(instrNo, 5, '0');
							} else if (recordYear.startsWith("2")){
								instrNo = recordYear + StringUtils.leftPad(instrNo, 6, '0');
							}
						}
					} else if ("du page".equalsIgnoreCase(crtCounty)){
						if (StringUtils.isNotEmpty(recordingDate) && StringUtils.isNotEmpty(instrNo)){
							String recordYear = recordingDate.substring(0, 4);
							
							instrNo = "R" + recordYear + StringUtils.leftPad(instrNo, 6, '0');
						}
					} else if ("kane".equalsIgnoreCase(crtCounty)){
						if (StringUtils.isNotEmpty(recordingDate) && StringUtils.isNotEmpty(instrNo)){
							String recordYear = recordingDate.substring(0, 4);
							int year = Integer.parseInt(recordYear);
							
							if (year >= 1999) {
								instrNo = year + "K" + StringUtils.leftPad(instrNo, 6, "0");
							} else if (year > 1989 && year < 1999){
								instrNo = recordYear.substring(2) + "K" + StringUtils.leftPad(instrNo, 6, "0");
							}
							
							
	//						String recordMonth = recordingDate.trim().replaceAll("\\d{4}-([^-]+)-(\\d+)", "$1");
	//						String recordDay = recordingDate.trim().replaceAll("\\d{4}-([^-]+)-(\\d+)", "$2");
	//						
	//						int day = Integer.parseInt(recordDay);
	//						int month = Integer.parseInt(recordMonth);
							
						}
					} else if ("kendall".equalsIgnoreCase(crtCounty)){
						if (StringUtils.isNotEmpty(recordingDate) && StringUtils.isNotEmpty(instrNo)){
							String recordYear = recordingDate.substring(0, 4);
							
							if (recordYear.startsWith("1")){
								instrNo = recordYear.substring(2) + StringUtils.leftPad(instrNo, 5, '0');
							} else if (recordYear.startsWith("2")){
								instrNo = recordYear + StringUtils.leftPad(instrNo, 8, '0');
							}
						}
					} else if ("mchenry".equalsIgnoreCase(crtCounty)){
						if (StringUtils.isNotEmpty(recordingDate) && StringUtils.isNotEmpty(instrNo)){
							String recordYear = recordingDate.substring(0, 4);
							
							
							instrNo = recordYear + "R" + StringUtils.leftPad(instrNo, 7, '0');
							
						}
					} else if ("will".equalsIgnoreCase(crtCounty)){
						if (StringUtils.isNotEmpty(recordingDate) && StringUtils.isNotEmpty(instrNo)){
							String recordYear = recordingDate.substring(0, 4);
							if (!recordYear.startsWith("2")){
								recordYear = recordingDate.substring(2, 4);
							}
							
							instrNo = "R" + recordYear + StringUtils.leftPad(instrNo, 6, '0');
						}
					}
				}
			}
		 
		 return instrNo;
	 }
}
