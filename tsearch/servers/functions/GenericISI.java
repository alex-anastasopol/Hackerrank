package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameFactory;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
*/

public class GenericISI {
	
		

	@SuppressWarnings("rawtypes")
	public static void partyNamesTokenizerILCookIS(ResultMap m, String s) throws Exception {

		String[] owners;
		m.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), s);
		m.remove(PropertyIdentificationSetKey.OWNER_FIRST_NAME.getKeyName());
		m.remove(PropertyIdentificationSetKey.OWNER_MIDDLE_NAME.getKeyName());
		m.remove(PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName());
		m.remove(PropertyIdentificationSetKey.SPOUSE_FIRST_NAME.getKeyName());
		m.remove(PropertyIdentificationSetKey.SPOUSE_MIDDLE_NAME.getKeyName());
		m.remove(PropertyIdentificationSetKey.SPOUSE_LAST_NAME.getKeyName());
		owners = s.split("&");
	  
		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, type = {"", ""}, otherType;

		if (s.matches("\\d+\\s*[A-Z]+")
				|| (owners.length == 2 && NameUtils.isCompany(owners[1]) && owners[1].endsWith("REV"))) {
			names[2] = s;
			//e.g. FORD JERRY J & SHALES-FORD RE (PIN 0127178007) restore RE, which was transformed in REV in order to be parsed as a company name
			names[2] = names[2].replaceFirst("(?is)REV\\z", "RE");
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			GenericFunctions.addOwnerNames(names, "", "", type, otherType, true, false, body);
			GenericFunctions.storeOwnerInPartyNames(m, body, true);
			return;
		}
		
		//try to see if the name should be parsed with DeSoto
		if (NameUtils.isNotCompany(s)) {
			boolean isDeSoto = false;
			String[] allNames = new String[]{s};
			
			String ss = s.replaceAll(ro.cst.tsearch.extractor.xml.GenericFunctions1.nameSuffixString, "")
					     .replaceAll(ro.cst.tsearch.extractor.xml.GenericFunctions1.nameTypeString, "")
					     .replaceAll(ro.cst.tsearch.extractor.xml.GenericFunctions1.nameOtherTypeString, "").trim(); 
			
			//e.g. JOHN W COX (Cook 01123030530000)
			Matcher ma1 = Pattern.compile("(?is)[A-Z]+\\s+(?:[A-Z]+\\s+)?([A-Z-]{2,})").matcher(ss);
			if (ma1.matches()) {
				if (NameFactory.getInstance().isLastOnly(ma1.group(1))) {
					isDeSoto = true;
					allNames = new String[]{s};
				}
			} else {
				//e.g. NORMAN & ELLEN ROBINSON (Lake 05041000330000), JOHN & ANN MARIE FITZGERALD (Lake 01131010050000)
				Matcher ma2 = Pattern.compile("(?is)[A-Z]+\\s+(?:[A-Z]\\s*)?\\s*&\\s*[A-Z]+\\s+(?:[A-Z]+\\s+)?([A-Z-]{2,})").matcher(ss);
				if (ma2.matches()) {
					isDeSoto = true;
					String[] split = s.split("\\s+");
					int index = -1;
					for (int i=0;i<split.length;i++) {
						if ("&".equals(split[i])) {
							index = i;
							break;
						}
					}
					if (index!=-1) {
						while (index>-1 && 
							(split[index].matches(ro.cst.tsearch.extractor.xml.GenericFunctions1.nameSuffixString)||
							 split[index].matches(ro.cst.tsearch.extractor.xml.GenericFunctions1.nameTypeString)||
							 split[index].matches(ro.cst.tsearch.extractor.xml.GenericFunctions1.nameOtherTypeString))) {
								index--;
						}
					}
					if (index!=-1) {
						StringBuilder sb = new StringBuilder();
						for (int i=0;i<index;i++) {
							sb.append(split[i]).append(" ");
						}
						sb.append(ma2.group(1)).append(" ");
						for (int i=index;i<split.length;i++) {
							sb.append(split[i]).append(" ");
						}
						s = sb.toString();
						allNames = new String[]{s};
					}
				} else {
					//e.g. STEPHEN KAUFMAN & TAMAR BEN-AMI (Lake 16261020310000)
					Matcher ma3 = Pattern.compile("(?is)[A-Z]+\\s(?:[A-Z]+\\s+)?([A-Z-]{2,})\\s*&\\s*[A-Z]+\\s(?:[A-Z]+\\s+)?[A-Z-]{2,}").matcher(ss);
					if (ma3.matches()) {
						if (NameFactory.getInstance().isLastOnly(ma3.group(1))) {
							isDeSoto = true;
							allNames = s.split("&");
						}
					}
				}
			}
			
			if (isDeSoto) {
				for (String each: allNames) {
					names = StringFormats.parseNameDesotoRO(each, true);
					
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
					GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1],
						type, otherType, NameUtils.isCompany(names[2]),	NameUtils.isCompany(names[5]), body);
				}
				GenericFunctions.storeOwnerInPartyNames(m, body, true);
				return;
			}
		}
		
		String prev_ow_last_name = "";
		for (int i = 0; i < owners.length; i++) {
			String next_ow = "";
			Pattern pa;
			Matcher ma;

			String ow = owners[i];
			if (i < owners.length - 1){
				next_ow = owners[i + 1];
			}
			names = StringFormats.parseNameNashville(ow, true);
			pa = Pattern.compile("\\bTR\\b");
			ma = pa.matcher(names[2]);
			if (i != owners.length - 1) {
				if (ma.find()){
					prev_ow_last_name = names[2].substring(0, names[2].indexOf(' '));
				} else {
					prev_ow_last_name = names[2];
				}
			}
			if (next_ow != "") {
				if (next_ow.contains(prev_ow_last_name)) // e.g.: PID:062800370000 -> SMITH JEFFREY C & ANNE STEINBOCK SMITH
				{
					// prev_ow_last_name = names[2];
					next_ow = next_ow.replaceFirst("([A-Z\\s]+)\\s" + prev_ow_last_name, prev_ow_last_name + "$1");
					owners[i + 1] = next_ow;
				} else {
					pa = Pattern.compile("[A-Z]+\\s[A-Z]\\sTR"); // cazul: SMITH HAROLD E TR & ELIZABETH D TR cand la
																 // ELIZABETH Last name ar trebui sa fie SMITH
					ma = pa.matcher(next_ow);
					if (ma.find()) {
						next_ow = prev_ow_last_name + next_ow;
						owners[i + 1] = next_ow;
					} else {
						pa = Pattern.compile("[A-Z]+\\s[A-Z][A-Z]+(?:\\s*([A-Z]|[A-Z]+))?"); // SMITH EDWARD JR & MITTEL MARIE H
																							 // & STACHURA CARLA MITTEL & PATRICAI M
						ma = pa.matcher(next_ow);
						if (!ma.find()) {
							pa = Pattern.compile("\\A\\s*[A-Z]+(:?\\s[A-Z])?\\s*(:?TR|III|JR|)?\\s*\\Z");
							ma = pa.matcher(next_ow);
							if (ma.find()) {
								// prev_ow_last_name = names[2];
								next_ow = prev_ow_last_name + next_ow;
								owners[i + 1] = next_ow;
							}
						}
					}
				}
			} else // suntem pe ultimul owner si putem avea un caz de genul:
				   // SMITH HAROLD E TR & ELIZABETH D TR
			{
				pa = Pattern.compile("\\A\\s*[A-Z]+\\s[A-Z]\\s*(:?TR|III|JR|)?\\s*\\Z");
				ma = pa.matcher(next_ow);
				if (ma.find()) {
					owners[i] = prev_ow_last_name + owners[i];
				}
			}
			if (!NameUtils.isCompany(ow)) {
				if (!LastNameUtils.isLastName(names[0])						// 1211128050	
					&& names[1].length() == 0
						&& !LastNameUtils.isLastName(names[2])
						&& !prev_ow_last_name.equalsIgnoreCase(names[2])) {
					names[1] = names[0];
					names[0] = names[2];
					names[2] = prev_ow_last_name;
				}
					type = GenericFunctions.extractAllNamesType(names);
			}
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1],
					type, otherType, NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), body);
		}
		
		GenericFunctions.storeOwnerInPartyNames(m, body, true);
	}
    
	
	public static void legalILCookIS(ResultMap m, String legal) throws Exception {
			   
		legal = legal.replaceAll("[\\(\\)]", " ").trim();
		legal = legal.replaceAll("\\s{2,}", " ");
		legal = legal.replaceFirst("\\s*\\d+\\s*(ST|ND|RD|TH)\\s*(ADD(N)?\\s+TO\\s+)", "");
		legal = legal.replaceFirst("(.*?) ((RE)?SUB|ADD|(NO|NUMBER|SECTION|UNIT)\\s?\\d+)\\b.*", "$1");
		legal = legal.replaceFirst(".*\\b(LO?TS?|BLK)\\s?\\d.*\\bIN (.+)", "$2"); // PID 05351170090000
			   
		if (legal.length() > 0){
			m.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), legal);
		}
		// fix the lot & block intervals
		String lot = (String)m.get(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName());
		String block = (String)m.get(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName());	   
		if(lot != null) {
			lot = org.apache.commons.lang.StringUtils.stripStart(lot, "0");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), GenericFunctions1.fixInterval(lot));
		}
		if(block != null){
			block = org.apache.commons.lang.StringUtils.stripStart(block, "0");
			m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), GenericFunctions1.fixInterval(block));
		}
	
		   
		String tmpSubdivision = (String)m.get("tmpSubdivision");
		if (tmpSubdivision != null && tmpSubdivision.length() != 0) {
			Matcher matcher = Pattern.compile("(?is)UNIT\\s*(\\d+)").matcher(tmpSubdivision);
			if (matcher.find()) {
				m.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), matcher.group(1));
			}
		}
	}
	   
	public static void parseSTR(ResultMap m) throws Exception {
		
		String section = (String)m.get(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName());
		String twp = (String)m.get(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName());
		   String range = (String)m.get(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName());
		   if (StringUtils.isNotEmpty(section)){
			   if ("0".equals(section.trim())){
				   m.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), "");
			   }
		   }
		   if (StringUtils.isNotEmpty(twp)){
			   if ("0".equals(twp.trim())){
				   m.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), "");
			   }
		   }
		   if (StringUtils.isNotEmpty(range)){
			   if ("0".equals(range.trim())){
				   m.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), "");
			   }
		   }
	}
	
	public static void parseAddressIS(ResultMap m) throws Exception {
		
		String address = (String) m.get("tmpPropAddr");
		
		if (StringUtils.isEmpty(address)){
			address = (String) m.get("tmpAddressLine1");
		}
		
		if (StringUtils.isNotEmpty(address)){
			address = address.replaceAll("(?is)\\bFIRST\\b", "1ST");//401 FIRST ST; ILWill
			address = address.replaceAll("(?is)\\bSECOND\\b", "2ND");
			address = address.replaceAll("(?is)\\bTHIRD\\b", "3RD");
			
			m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringFormats.StreetNo(address));
			m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringFormats.StreetName(address));
		}
	}
	
	public static void improveCrossRefsParsing(ResultMap resultMap, String src, long searchId){
				
		if ("IS".equals(src)){
			
			ResultTable saleSet = (ResultTable) resultMap.get("SaleDataSet");
			
			if (saleSet == null){
				return;
			}
			if (saleSet.getBodyRef().length == 0){
				return;
			}
			String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
			String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
			Calendar cal = Calendar.getInstance();
			int currentyear = cal.get(Calendar.YEAR);
			
			if ("IL".equals(crtState)){
				if ("Kane".equalsIgnoreCase(crtCounty)){
					String[][] body = saleSet.getBodyRef();
					for (int i = 0; i < body.length; i++) {
						if (StringUtils.isNotEmpty(body[i][0])){
							String instrNo = body[i][0].trim();

							if (instrNo.matches("(?is)\\A(\\d{2})K\\d+")){
								String year = instrNo.replaceFirst("\\A(\\d{2})K\\d+", "$1");
								try {
									int yearInt = Integer.parseInt(year);
									if (yearInt < 94){////e.g. 1223253004
										instrNo.replaceFirst("\\A(\\d{2}K)0+(\\d+)", "$1$2");
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
							body[i][0] = instrNo;
						}
					}
				} else if ("DeKalb".equalsIgnoreCase(crtCounty)){
					String[][] body = saleSet.getBodyRef();
					for (int i = 0; i < body.length; i++) {
						if (StringUtils.isNotEmpty(body[i][0])){
							String instrNo = body[i][0];
							if (instrNo.length() == 8){
								if (instrNo.startsWith("0") || instrNo.startsWith("1")){
									instrNo = "20" + instrNo;
								} else {
									instrNo = "19" + instrNo;
								}
							}
							body[i][0] = instrNo;
						}
					}
				} else if ("Du Page".equalsIgnoreCase(crtCounty)){
					
					String[][] body = saleSet.getBodyRef();
					for (int i = 0; i < body.length; i++) {
						if (StringUtils.isNotEmpty(body[i][0]) && body[i][0].startsWith("R")){
							String recDate = body[i][2];
							
							String instrNo = body[i][0];
							instrNo = instrNo.replaceFirst("\\AR", "");
							
							if (!recDate.trim().matches("00/00/0000")){
								String year = recDate.replaceAll("[^/]+/[^/]+/(\\d{4})", "$1");
								try {
									int intYear = Integer.parseInt(year);

									if (intYear > 2000) {

									} else if (intYear < 2000){
										if (year.length() == 4) {
											if (year.endsWith(instrNo.substring(0, 2))){
												instrNo = "19" + org.apache.commons.lang.StringUtils.leftPad(instrNo, 6, "0");
											}
										}
									}
									body[i][0] = "R" + instrNo;
									
								} catch (Exception e) {
									e.printStackTrace();
								}
							} else{
								if (instrNo.startsWith("2")){
									body[i][0] = "R" + instrNo;
								} else {
									body[i][0] = "R19" + instrNo;
								}
							}
						} else{
							String instrNo = body[i][0];
							if (StringUtils.isNotEmpty(instrNo)){
								instrNo = instrNo.replaceFirst("\\AR", "");
								if (instrNo.startsWith("2")){
									body[i][0] = "R" + instrNo;
								} else {
									body[i][0] = "R19" + instrNo;
								}
							}
						}
					}
				} else if ("Kendall".equalsIgnoreCase(crtCounty)){
					String[][] body = saleSet.getBodyRef();
					for (int i = 0; i < body.length; i++) {
						if (StringUtils.isNotEmpty(body[i][0])){
							String instrNo = body[i][0];
							String year = body[i][2];
							if (instrNo.contains("K")){
								instrNo = instrNo.replaceFirst("\\AK", "");
								String yearFromInstrNo = instrNo.substring(0, 2);
								
								if (StringUtils.isNotEmpty(year) && !year.matches("00/00/0000") && year.matches("\\d+/\\d+/\\d{4}")){
									year = year.replaceAll("\\d+/\\d+/(\\d{4})", "$1");
									
									instrNo = instrNo.substring(2);
									if (year.endsWith(yearFromInstrNo)){
										if (year.startsWith("1")){
											instrNo = year/*.substring(2)*/ +  org.apache.commons.lang.StringUtils.leftPad(instrNo, 5, "0");
										} else if (year.startsWith("2")){
											instrNo = year +  org.apache.commons.lang.StringUtils.leftPad(instrNo, 8, "0");
										}
									} 
								} else{
									if (yearFromInstrNo.startsWith("0") || yearFromInstrNo.startsWith("1")){
										instrNo = "20" + yearFromInstrNo +  org.apache.commons.lang.StringUtils.leftPad(instrNo, 8, "0");
									} else{
										instrNo = "19" + yearFromInstrNo +  org.apache.commons.lang.StringUtils.leftPad(instrNo, 5, "0");
									}
								}
								body[i][0] = instrNo;;
							} else if (instrNo.contains("-") && instrNo.matches("\\d+-\\d+")){
								String[] parts = instrNo.split("-");
								if (year.endsWith(parts[0])){
									if (year.startsWith("1")){
										instrNo = year/*.substring(2)*/ +  org.apache.commons.lang.StringUtils.leftPad(parts[1], 5, "0");
									} else if (year.startsWith("2")){
										instrNo = year +  org.apache.commons.lang.StringUtils.leftPad(parts[1], 8, "0");
									}
								} else {
									if (parts[0].length() == 4){
										if (parts[0].startsWith("1")){
											instrNo = parts[0]/*.substring(2)*/ +  org.apache.commons.lang.StringUtils.leftPad(parts[1], 5, "0");
										} else if (parts[0].startsWith("2")){
											instrNo = parts[0] +  org.apache.commons.lang.StringUtils.leftPad(parts[1], 8, "0");
										}
									}
								}
								body[i][0] = instrNo;
							}
						}
					}
				} else if ("McHenry".equalsIgnoreCase(crtCounty)){
					String[][] body = saleSet.getBodyRef();
					for (int i = 0; i < body.length; i++) {
						if (StringUtils.isNotEmpty(body[i][0])){
							String instrNo = body[i][0];
							String year = instrNo.substring(0, 2);
							
							instrNo = instrNo.replaceFirst("(?is)\\d+R", "");
							if (year.startsWith("0") || year.startsWith("1")){
								year = "20" + year;
							} else {
								year = "19" + year;
							}
							instrNo = year.concat("R").concat(org.apache.commons.lang.StringUtils.leftPad(instrNo, 7, "0"));
							body[i][0] = instrNo;
						}
					}
				} else if ("Will".equalsIgnoreCase(crtCounty)){
					
					String[][] body = saleSet.getBodyRef();
					for (int i = 0; i < body.length; i++) {
						if (StringUtils.isNotEmpty(body[i][0]) && body[i][0].startsWith("R")){
							String recDate = body[i][2];
							if (!recDate.trim().matches("00/00/0000")){
								String year = recDate.replaceAll("[^/]+/[^/]+/(\\d{4})", "$1");
								try {
									int intYear = Integer.parseInt(year);
									
									if (intYear > 2000) {
										if (year.length() == 4) {
											String instrNo = body[i][0];
											instrNo = instrNo.replaceFirst(instrNo.substring(1, 3), year);
											body[i][0] = instrNo;
										}
									} else if (intYear < 2000){//1202182020230000: R1999134524 must be R99134524 to get results on RO
										if (year.length() == 4) {
											String instrNo = body[i][0];
											if (year.equals(instrNo.substring(1, 5))){
												instrNo = instrNo.replaceFirst(instrNo.substring(1, 3), "");
												body[i][0] = instrNo;
											}
										}
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							} else {
								String instrNo = body[i][0];
								String year = "20" + instrNo.substring(1, 3);
								try {
									int intYear = Integer.parseInt(year);
									if (intYear == 2000){
										continue;
									} else if (intYear <= currentyear){
										instrNo = instrNo.replaceFirst(instrNo.substring(1, 3), year);
										body[i][0] = instrNo;
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						} else if (body[i][0].startsWith("0")){//02005005311, 00970105444
							String instr = body[i][0].replaceAll("\\A0+", "");
							if (instr.startsWith("9") && (instr.substring(2, instr.length())).length() > 6){
								String firstPart = instr.substring(0, 2);
								String secondPart = instr.substring(2, instr.length());
								secondPart = secondPart.replaceAll("(?is)\\A0+(\\d{6})", "$1");
								body[i][0] = "R" + firstPart + secondPart;
							} else {
								body[i][0] = "R" + instr;
							}
						}
					}
				} 
			}
		}
	}
		   
}
