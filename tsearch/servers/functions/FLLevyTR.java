package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

public class FLLevyTR {
	
	@SuppressWarnings("rawtypes")
	public static void parseName(String name, ResultMap m) {
		String owner = (String) m.get("tmpOwner");

		if (StringUtils.isEmpty(owner))
			return;

		owner = owner.trim();
		owner = owner.replaceAll("(?is)\\s*\\.\\.\\.\\z", "");
		owner = owner.replaceAll("(?is)\\s*-?\\s*\\bESTATE\\s*-?\\s*", "");
		owner = owner.replaceAll("(?is)\\s*-\\z", "");
		owner = owner.replaceAll("(?is)[\r\t\n]", "");
		owner = owner.replaceAll("(?is)<br>\\s*C\\s*O\\b", " C/O<br>");
		owner = owner.replaceAll("(?is)<br>\\s*CONT\\s*", "");
		owner = owner.replaceAll("(?is)\\bES\\s*<br>\\s*ESTATE\\s*<br>", "ESTATE<br>");
		owner = owner.replaceAll("(?is)\\bCO[-\\s]*TRS\\b", "TRS");
		owner = owner.replaceAll("(?is)-\\s*(TRS?)\\s*-", " $1 ");
		owner = owner.replaceAll("(?is)\\bTRS\\b", "TRS<br>");
		owner = owner.replaceAll("(?is)-?\\s*<br>\\s*TRS\\s*\\b", " TRS");
		owner = owner.replaceAll("(?is)&\\s*<br>", "& ");
		String[] ownerRows = owner.split("<br>");
		String stringOwner;
		StringBuilder sb = new StringBuilder();
		for (String row : ownerRows) {
			if (row.trim().matches("(?is)(?:\\s*\\bC/O\\b\\s*)?\\d+\\s+.*")) {
				break;
			} else if (row.toLowerCase().contains("box")) {
				break;
			} else {
				sb.append(row).append("<br>");
			}
		}
		stringOwner = sb.toString();
		Matcher matcher = Pattern.compile("(?is)(.*?)<br>\\s*([A-Za-z])\\b(.*)").matcher(stringOwner);
		if (matcher.find()) {
			String after = matcher.group(2) + matcher.group(3);
			if (NameUtils.isNotCompany(after)) 
				stringOwner = matcher.group(1) + " " + after;
			
		}
		stringOwner = stringOwner.replaceAll("(?is)-\\s*(LIFE ES?T?A?T?E?)(-|\\b)", " $1");
		stringOwner = stringOwner.replaceAll("(?is)(?:(?:\\bCO\\s*)|-)(?:\\s*<br>\\s*)?((T(?:(?:RU?)?S?)?(?:TE)?E?S?))(-|\\b)", " $1");
		stringOwner = stringOwner.replaceAll("(?is)-CO?N?T?(-|\\b)", "");
		stringOwner = stringOwner.replaceAll("(?is)-EST(-|\\b)", " EST");
		stringOwner = stringOwner.replaceAll("\\d+(/|\\s)\\d+(\\s*\\w+)?", "");
		stringOwner = stringOwner.replaceAll(";", "");
		stringOwner = stringOwner.replaceAll("(?is)<br>\\s*DBA\\b", "<br>");
		stringOwner = stringOwner.replaceAll("(?is)\\b(C[\\s/]O)\\s*(<br>)", " $2$1 ");
		
		List<List> body = new ArrayList<List>();
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;

		stringOwner = stringOwner.replaceAll("(?is)\\s*&\\s*$", "");
		String[] owners = stringOwner.split("<br>");

		String[] prevName = {"", "", "" };
		boolean thirdName = false;
		boolean firstNameisCompany = false;
		
		for (int i = 0; i < owners.length; i++) {
			owners[i] = owners[i].replaceAll("\\A\\s*&", "");
			if (NameUtils.isCompany(owners[i].replaceAll("(?is)\\bC[\\s/]O\\b", ""))) {
				for (int j=0;j<names.length;j++)
					names[j] = "";
				names[2] = owners[i].replaceAll("(?is)\\bC[\\s/]O\\b", "");
				firstNameisCompany = true;
			} else {
				
				if (owners[i].matches("(?is)\\bC[\\s/]O\\b.+")) {
					owners[i] = owners[i].replaceAll("(?is)\\bC[\\s/]O\\b", "");
					names = StringFormats.parseNameDesotoRO(owners[i], true);
				} else {
					//add the first name before the third in order to take the last name
	                //e.g. SMITH JOHN & MARY <br> DAVID
					if (i>0 && owners[i].indexOf("&")==-1 && !StringUtils.isEmpty(prevName)) {		 
						owners[i] = prevName[2] + " " + prevName[0] + " " + prevName[1] + " & " + owners[i];
						thirdName = true;
					}
					names = StringFormats.parseNameNashville(owners[i], true);
					if (names[3].length() == 0 && names[4].length() == 0 && names[4].length() == 0  &&			//EARL JONES
							FirstNameUtils.isFirstName(names[2]) && !LastNameUtils.isLastName(names[2]) &&
							!FirstNameUtils.isFirstName(names[0]) && LastNameUtils.isLastName(names[0])) {
						String aux = names[0];
						names[0] = names[2];
						names[2] = aux;
					}
				}
				
				if (thirdName) {
					names[0] = names[3];
					names[1] = names[4];
					names[2] = names[5];
					names[3] = "";
					names[4] = "";
					names[5] = "";
				}
				
				names[0] = names[0].replaceAll("\\s*-\\s*\\z", "");
			} 
				
			suffixes = GenericFunctions.extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			
			if (!firstNameisCompany) {
				prevName[0] = names[0];
				prevName[1] = names[1];
				prevName[2] = names[2];
			}
		}
		try {
			GenericFunctions.storeOwnerInPartyNames(m, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("rawtypes")
	protected static void parseSaleData(ResultMap m, NodeList mainList, String detailsHtml) {
		List<String> line;
		NodeList saleTablesList = HtmlParser3.getNodesByID("saleTable", mainList, true);
		if (saleTablesList != null && saleTablesList.size()>0) {
			List<List<String>> taxTable = HtmlParser3.getTableAsList(saleTablesList.elementAt(0).toHtml(), false);
			List<List> body = new ArrayList<List>();
			for (List lst : taxTable) {
				line = new ArrayList<String>();
				if (lst.size()==9 && !lst.get(0).toString().trim().equalsIgnoreCase("Sale Date")) {
					line.add(lst.get(0).toString().trim());
					line.add(lst.get(1).toString().replaceAll("[\\$,]", "").trim());
					line.add(lst.get(2).toString().trim());
					line.add(lst.get(3).toString().replaceFirst("\\A0+", "").trim());
					line.add(lst.get(4).toString().replaceFirst("\\A0+", "").trim());
					line.add(lst.get(7).toString().trim());
					line.add(lst.get(8).toString().trim());
					body.add(line);
				}
			}
			
			if (body != null) {
				if (!body.isEmpty()) {
					ResultTable rt = new ResultTable();
					String[] header = { "InstrumentDate", "SalesPrice", "DocumentType", "Book", "Page", "Grantor", "Grantee",  };
					rt = GenericFunctions2.createResultTable(body, header);
					m.put("SaleDataSet", rt);
				}
			}
		}
	}
	
	public static void parseLegalDescription(ResultMap resultMap)
	{
		
	    String legalDescription = (String) resultMap.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
		if(StringUtils.isEmpty(legalDescription)) {
			return;
		}
		
		legalDescription = legalDescription.replaceAll("\\s{2,}", " ");
		
		Matcher strMatcher = Pattern.compile("(?is)(\\d{2})-(\\d{2})-(\\d{2})").matcher(legalDescription);
		if (strMatcher.find()) {
			String section = strMatcher.group(1).replaceAll("\\A0+", "");
			String township = strMatcher.group(2).replaceAll("\\A0+", "");
			String range = strMatcher.group(3).replaceAll("\\A0+", "");
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(), section);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), township);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), range);
		}
		legalDescription = legalDescription.replaceFirst("(?is)\\A\\s*\\d{2}-\\d{2}-\\d{2}", "").trim();
		legalDescription = legalDescription.replaceFirst("(?is)\\ALEG\\b", "").trim();
		legalDescription = legalDescription.replaceFirst("(?is)\\A[\\d\\.]+\\sACRES\\b", "").trim();
		
		legalDescription = " " + legalDescription; 
		Matcher subdivisionMatcher = Pattern.compile("(?is)(.+?)(S/D|((N|S|E|W|NE|NW|SE|SW)\\d+/\\d+)|UNIT|TRACT|BLK|(-?(UN)?REC)|-?REPLAT)")
			.matcher(legalDescription);
		if (subdivisionMatcher.find()) {
			String subdivision = subdivisionMatcher.group(1).trim();
			if (subdivision.length()>0)
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(), subdivision);
		}
		
		String lotexpr = "(?is)\\bLOTS?\\s(((\\d+(\\s*THRU\\s*\\d+)?)[\\s,&]*)+)";
		List<String> lot = RegExUtils.getMatches(lotexpr, legalDescription, 1);
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<lot.size();i++) {
			String[] temp = lot.get(i).trim().split("[&,]");
			for (int j=0;j<temp.length;j++)
				sb.append(temp[j].trim().replaceAll("(\\d+)\\s*THRU\\s*(\\d+)", "$1-$2")).append(" ");
		}
		String subdivisionLot = sb.toString().trim();
		if (subdivisionLot.length() != 0) {
			subdivisionLot = LegalDescription.cleanValues(subdivisionLot, true, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), subdivisionLot);
		}
		
		List<String> blk = RegExUtils.getMatches("(?is)\\bBL(?:OC)?K\\s(\\w+)", legalDescription, 1);
		StringBuilder sb_blk = new StringBuilder();
		for (int i=0; i<blk.size(); i++) sb_blk.append(" ").append(blk.get(i));
		String subdivisionBlock = sb_blk.toString();
		subdivisionBlock = subdivisionBlock.trim();
		if (subdivisionBlock.length() != 0) {
			subdivisionBlock = LegalDescription.cleanValues(subdivisionBlock, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), subdivisionBlock);
		}
		
		List<String> unit = RegExUtils.getMatches("(?is)\\bUNIT\\s([^\\s]+)", legalDescription, 1);
		String subdivisionUnit = "";
		sb = new StringBuilder();
		for (int i=0; i<unit.size(); i++) 
			sb.append(" ").append(unit.get(i));
		subdivisionUnit = sb.toString().trim();
		if (subdivisionUnit.length() != 0) {
			subdivisionUnit = LegalDescription.cleanValues(subdivisionUnit, false, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(), subdivisionUnit);
		}
		
		List<String> tract1 = RegExUtils.getMatches("(?is)\\bTRACT\\s(\\d+)\\s*(..?)", legalDescription, 1);
		List<String> tract2 = RegExUtils.getMatches("(?is)\\bTRACT\\s(\\d+)\\s*(..?)", legalDescription, 2);
		String subdivisionTract = "";
		sb = new StringBuilder();
		for (int i=0; i<tract1.size(); i++) {
			String tr2 = tract2.get(i).trim();
			if (!"X".equals(tr2) && !"FT".equals(tr2))
				sb.append(" ").append(tract1.get(i));
		}
		subdivisionTract = sb.toString().trim();
		if (subdivisionTract.length() != 0) {
			subdivisionTract = LegalDescription.cleanValues(subdivisionTract, true, true);
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), subdivisionTract);
		}
		
		ResultTable rt = (ResultTable)resultMap.get("SaleDataSet");
		
		List<String> book = RegExUtils.getMatches("(?is)\\bOR\\sBOOK\\s(\\d+)\\sPAGES?(\\s(\\d+(?:\\sTHRU\\s\\d+)?(\\s*[,&]\\s*)?)+)", legalDescription, 1);
		List<String> page = RegExUtils.getMatches("(?is)\\bOR\\sBOOK\\s(\\d+)\\sPAGES?(\\s(\\d+(?:\\sTHRU\\s\\d+)?(\\s*[,&]\\s*)?)+)", legalDescription, 2);
		StringBuilder sb_bookpage = new StringBuilder();
		for (int i=0; i<book.size(); i++) {
			List<String> pages = new ArrayList<String>();
			String[] split = page.get(i).split("[&,]");
			for (int j=0;j<split.length;j++) {
				String current = split[j].trim();
				if (current.length()>0) {
					Matcher pageMatcher = Pattern.compile("(\\d+)\\sTHRU\\s(\\d+)").matcher(current);
					if (pageMatcher.find()) {
						int low = Integer.parseInt(pageMatcher.group(1));
						int high = Integer.parseInt(pageMatcher.group(2));
						for (int k=low;k<=high;k++)
							pages.add(k+"");
					} else {
						pages.add(current);
					}
				}
			}
			for (int j=0;j<pages.size();j++)
				sb_bookpage.append(book.get(i)).append("&").append(pages.get(j)).append(" ");
		}
		String crossBookPage = sb_bookpage.toString().trim();
		if (crossBookPage.length() != 0)
		{
			crossBookPage = LegalDescription.cleanValues(crossBookPage, false, true);
			String[] values = crossBookPage.split("\\s");
			
			ResultTable crossRef = new ResultTable();			//cross references table
			@SuppressWarnings("rawtypes")
			List<List> tablebodyRef = new ArrayList<List>();
			List<String> list;
			for (int i=0; i<values.length; i++)			
			{
				String[] bookAndPage = values[i].split("&");
				if (bookAndPage.length==2) {
					if (!alreadyInSaleDataSet(bookAndPage[0],bookAndPage[1],rt)) {
						list = new ArrayList<String>();
						list.add(bookAndPage[0]);
						list.add(bookAndPage[1]);
						tablebodyRef.add(list);
					}	
				}
			}
			String[] headerRef = {CrossRefSetKey.BOOK.getShortKeyName(), CrossRefSetKey.PAGE.getShortKeyName()};
			crossRef = GenericFunctions2.createResultTable(tablebodyRef, headerRef);
			if (crossRef != null && tablebodyRef.size()!=0){
				resultMap.put("CrossRefSet", crossRef);
			}
		}
	}
	
	public static boolean alreadyInSaleDataSet(String book, String page, ResultTable rt) {
		try {
			for (int i=0;i<rt.getLength();i++) {
				String bookValue = rt.getItem("book", "icw", i);
				String pageValue = rt.getItem("page", "icw", i);
				if (book.equals(bookValue) && page.equals(pageValue)) {
					return true;
				}
			}
		} catch (Exception e) {
			
		}
			
		return false;
	}
	
}
