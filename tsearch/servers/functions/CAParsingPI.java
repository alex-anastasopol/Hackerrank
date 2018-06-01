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
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;

/**
 * @author mihaib
 * parsing functions for California PI data source
  */

public class CAParsingPI {
	
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
					
					if (gt.contains(" IJT")){// || (NameUtils.isNotCompany(gt) && !gt.contains(","))){
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
						
					if (gtor.contains(" IJT")){// || (NameUtils.isNotCompany(gtor) && !gtor.contains(","))){
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
					
					if (gt.contains(" IJT")){// || (NameUtils.isNotCompany(gt) && !gt.contains(","))){
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
					if (gtee.contains(" IJT")){// || (NameUtils.isNotCompany(gtee) && !gtee.contains(","))
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
		//DATED NOVEMBER 2 2006
		name = name.replaceAll("(?is)(DTD|DATED(\\s+\\w+\\s+\\d+)?)\\s*\\d+", "");
		
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
		name = name.replaceAll("(?is)\\(\\\"MERS\\\"\\)", "");
		
		//WILSON, JOHN P JR P J
		name = name.replaceAll("(?is)\\b([A-Z]\\s+(?:JR|SR))\\s+\\1", "$1");
		name = name.replaceAll("(?is)\\bLAH\\s*\\d+[A-Z]\\b", "");
		
		//01-753751 LP
		name = name.replaceAll("(?is)\\b\\d{2}-\\d+\\s*LP\\b", "");
		name = name.replaceAll("(?is)[\\)\\(]+", "");
		
		//MALICK, ELLEN G. NKA ROBINSON,
		if (name.trim().matches("(?is)([A-Z]+,)\\s+([A-Z]+\\s+[A-Z]\\.)\\s+NKA\\s+([A-Z]+,)")){
			name = name.replaceAll("(?is)([A-Z]+,)\\s+([A-Z]+\\s+[A-Z]\\.)\\s+NKA\\s+([A-Z]+,)", "$1 $2##@@##$3 $2");
		}
		
		List<List> body = new ArrayList<List>();
		List<String> line = new ArrayList<String>();
		
		//BookPage Date
		Pattern p = Pattern.compile("(?is)\\b(\\d+)\\s+(\\d+)\\s+(\\d{6,8})\\b");//Orange: 164245:1982
		Matcher ma = p.matcher(name);
		
		if (ma.find()){
			String book = StringUtils.stripStart(ma.group(1), "0");
			String page = StringUtils.stripStart(ma.group(2), "0");
			
			String date = ma.group(3);
			String year = "", month = "", day = "";
			
			if (date.length() > 5){
				month = date.substring(0, 2);
				day = date.substring(2, 4);
				year = date.substring(4);
				
				if (year.length() == 2){
					if (year.startsWith("0") || year.startsWith("1")){
						year = "20" + year;
					} else{
						year = "19" + year;
					}
				}
			}
			
			line.add("");
			line.add(book);
			line.add(page);
			line.add(year);
			line.add(month);
			line.add(day);
			body.add(line);
			
			name = name.replaceAll(ma.group(0), "");
		} else{
			//InstrumentNo Date
			p = Pattern.compile("(?is)\\b(\\d+)\\s+(\\d{6,8})\\b");//Orange: 632165:1996
			ma = p.matcher(name);
			if (ma.find()){
				
				String instr = StringUtils.stripStart(ma.group(1), "0");
				String date = ma.group(2);
				String year = "", month = "", day = "";
				
				if (date.length() > 5){
					month = date.substring(0, 2);
					day = date.substring(2, 4);
					year = date.substring(4);
					
					if (year.length() == 2){
						if (year.startsWith("0") || year.startsWith("1")){
							year = "20" + year;
						} else{
							year = "19" + year;
						}
					}
				}
				
				if (StringUtils.isNotEmpty(instr) && StringUtils.isNotEmpty(year)){
					instr = year + "-" + instr;
				}
				line.add(instr);
				line.add("");
				line.add("");
				line.add(year);
				line.add(month);
				line.add(day);
				body.add(line);
				
				name = name.replaceAll(ma.group(0), "");
			} else{
				//Book Page
				p = Pattern.compile("(?is)\\b(\\d+)\\s+(\\d{2,5})\\b");//Orange: 43840:1977
				ma = p.matcher(name);
				if (ma.find()){
					
					String book = StringUtils.stripStart(ma.group(1), "0");
					String page = StringUtils.stripStart(ma.group(2), "0");
					
					line.add("");
					line.add(book);
					line.add(page);
					line.add("");
					line.add("");
					line.add("");
					body.add(line);
					name = name.replaceAll(ma.group(0), "");
				} else{
					//Year-Instrument
					p = Pattern.compile("(?is)\\b(\\d{2,4})\\s*-\\s*(\\d+)\\b");//Orange: 9578:1985
					ma = p.matcher(name);
					if (ma.find()){
						
						String year = ma.group(1);
						if (year.length() == 2){
							if (year.startsWith("0") || year.startsWith("1")){
								year = "20" + year;
							} else{
								year = "19" + year;
							}
						}
						
						String instrument = year + "-" + StringUtils.stripStart(ma.group(2), "0");
						
						line.add(instrument);
						line.add("");
						line.add("");
						line.add(year);
						line.add("");
						line.add("");
						body.add(line);
						name = name.replaceAll(ma.group(0), "");
					}
				}
			}
		}
		
		if (body != null && body.size() > 0) {
			
			ResultTable rt = (ResultTable) resultMap.get("CrossRefSet");
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
						if (!body.contains(line)){
							body.add(line);
						}
					}
				}
			}
			
			rt = new ResultTable();
			String[] header = {"InstrumentNumber", "Book", "Page", "Year", "Month", "Day"};
			rt = GenericFunctions2.createResultTable(body, header);
			resultMap.put("CrossRefSet", rt);
		}
		
		name = name.replaceAll("(?is)\\s*&\\s*$", "");
		if (name.matches("(?is)[\\d\\s]+")){
			name = "";
		}
		
		return name.trim();
	}

	public static void parsingRemarks(ResultMap resultMap, long searchId) throws Exception{
	
		String remarks = (String) resultMap.get("tmpRemarks");
		
		if (StringUtils.isEmpty(remarks)){
			return;
		}
		
		remarks = remarks.replaceAll("(?is)\\?", "");

		List<List> body = new ArrayList<List>();
		List<String> line = null;
		Pattern p = Pattern.compile("(?is)\\bRef:\\s*(.+)");
		Matcher ma = p.matcher(remarks);
		
		if (ma.find()){
			String[] references = ma.group(1).split("\\s*;\\s*");			
			for (String ref : references){
				if (ref.matches("(?is)\\b\\d+\\s*/\\s*\\d+")){
					String[] items = ref.trim().split("\\s*/\\s*");
					if (items.length == 2){
						line = new ArrayList<String>();
						line.add("");
						line.add(items[0]);
						line.add(items[1]);
						line.add("");
						line.add("");
						line.add("");
						body.add(line);
					}
				} else if (ref.matches("(?is)\\b(\\d{2,4})\\s*-\\s*(\\d+)\\b")){
					//Year-Instrument
					String[] items = ref.trim().split("\\s*-\\s*");
					if (items.length == 2){
						
						String year = items[0];
						if (year.length() == 2){
							if (year.startsWith("0") || year.startsWith("1")){
								year = "20" + year;
							} else{
								year = "19" + year;
							}
						}
						
						String instrument = year + "-" + StringUtils.stripStart(items[1], "0");
						line = new ArrayList<String>();
						line.add(instrument);
						line.add("");
						line.add("");
						line.add(year);
						line.add("");
						line.add("");
						body.add(line);
					}
				} else{
					ref = ref.replaceAll("(?is)\\A\\s*(\\d{2})\\s+(\\d{3})\\s+(\\d{2,3})\\s*$", "$1$2$3");
					ref = ref.replaceAll("(?is)\\A\\s*(\\d{2})\\s+(\\d+)\\s*$", "$1$2");
					ref = ref.replaceAll("(?is)\\A\\s*(\\d{4,6})\\s+(\\d{2})\\s*$", "$1$2");
					ref = ref.replaceAll("(?is)/", "");
					String[] items = ref.trim().split("\\s+");
					if (items.length == 2){
						line = new ArrayList<String>();
						if (items[1].length() == 8){//it's a date on second row 29827 05102004
							String instrNo = items[0];
							String year = items[1].substring(items[1].length() - 4);
							String month = items[1].substring(0, 2);
							String day = items[1].substring(2, 4);
									
							instrNo = year + "-" + instrNo;
							
							line.add(instrNo);
							line.add("");
							line.add("");
							line.add(year);
							line.add(month);
							line.add(day);
							body.add(line);
						} else if (items[0].length() == 8){//it's a date on first row 07161985 261581
							String instrNo = items[1];
							String year = items[0].substring(items[0].length() - 4);
							String month = items[0].substring(0, 2);
							String day = items[0].substring(2, 4);
									
							instrNo = year + "-" + instrNo;
							
							line.add(instrNo);
							line.add("");
							line.add("");
							line.add(year);
							line.add(month);
							line.add(day);
							body.add(line);
						}
					}
				}
			}
			remarks = remarks.replaceFirst(ma.group(0), "");
		}
				
		if (!body.isEmpty()) {
			String[] header = { "InstrumentNumber", "Book", "Page", "Year", "Month", "Day" };
			resultMap.put("CrossRefSet", GenericFunctions2.createResultTable(body, header));
		}
		
		resultMap.removeTempDef();
	}
}
