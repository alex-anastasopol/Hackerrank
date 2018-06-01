package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.CrossRefSet.CrossRefSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class GenericParsingOR {

	@SuppressWarnings("rawtypes")
	public static String concatenateNames(ArrayList<List> nameList) {
		String result = "";
		
		StringBuilder resultSb = new StringBuilder();
		for (List list: nameList) {
			if (list.size()>3) {
				resultSb.append(list.get(3)).append(", ").append(list.get(1)).append(" ").append(list.get(2)).append(" / ");
			}
		}
		result = resultSb.toString().replaceAll("/\\s*,\\s*/", " / ").replaceAll(",\\s*/", " /").
			replaceAll("\\s{2,}", " ").replaceAll("/\\s*$", "").trim();
		return result;
	}

	@SuppressWarnings("rawtypes")
	public static void parseGrantorGranteeSetOR(ResultMap m, long searchId)
			throws Exception {

		String grantor = (String) m.get("SaleDataSet.Grantor");
		String grantee = (String) m.get("SaleDataSet.Grantee");

		if (StringUtils.isEmpty(grantee)
				&& StringUtils.isNotEmpty((String) m
						.get("SaleDataSet.GranteeLander"))) {
			grantee = (String) m.get("SaleDataSet.GranteeLander");
		}
		grantor = StringUtils.prepareStringForHTML(grantor);
		grantee = StringUtils.prepareStringForHTML(grantee);
		grantor = NameCleaner.cleanFreeformName(grantor);
		grantee = NameCleaner.cleanFreeformName(grantee);

		ArrayList<List> grantorList = new ArrayList<List>();
		ArrayList<List> granteeList = new ArrayList<List>();

		parseNameInner(m, grantor, grantorList, searchId, false);
		parseNameInner(m, grantee, granteeList, searchId, true);

		m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantorList, true));
		m.put("GranteeSet", GenericFunctions.storeOwnerInSet(granteeList, true));
		
		GenericFunctions1.setGranteeLanderTrustee2(m, searchId,true);
		GenericFunctions.checkTNCountyROForMERSForMortgage(m, searchId);
		
		m.put(SaleDataSetKey.GRANTOR.getKeyName(), concatenateNames(grantorList));
		m.put(SaleDataSetKey.GRANTEE.getKeyName(), concatenateNames(granteeList));
	}

	@SuppressWarnings("rawtypes")
	public static void parseNameInner(ResultMap m, String name,
			ArrayList<List> namesList, long searchId, boolean isGrantee) {

		if (StringUtils.isEmpty(name))
			return;
		
		name = name.replaceAll("(?is)\\b(Trustee) Of\\b", "$1 / ");
		name = name.replaceAll("(?is)\\s*, \\s*NOMINEE\\s+FOR\\b", " / ");
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;

		String[] nameItems = name.split("/");
		for (int i = 0; i < nameItems.length; i++) {
			String n = nameItems[i];
			if (n.contains(",")) {
				names = StringFormats.parseNameNashville(n, true);
			} else {
				names = StringFormats.parseNameWilliamson(n, true);
			}
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(n, names, suffixes[0],
					suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), namesList);
		}
	}
	
	// if Ref Document ID is like 733/1608 clear it and put these values at Book and Page, respectively
	// ses also Task 8023
	public static void correctBookPage(ResultMap m, long searchId)	throws Exception {
		String instNo = (String) m.get(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName());
    	String book = (String) m.get(SaleDataSetKey.BOOK.getKeyName());
    	String prefix = "";
    	
    	if(StringUtils.isNotEmpty(instNo) && StringUtils.isNotEmpty(book)) {
    		if(instNo.matches("\\d{4}[A-Z]\\d+")) {
    			prefix = instNo.replaceAll("\\d+", "");
    			m.put(SaleDataSetKey.BOOK.getKeyName(), prefix + book);
    		}
    	}
    	
		ResultTable resultTable = (ResultTable)m.get("CrossRefSet");
		if (resultTable!=null) {
			ResultTable crossRef = new ResultTable();	
			String[] header = {CrossRefSetKey.INSTRUMENT_NUMBER.getShortKeyName(), CrossRefSetKey.BOOK.getShortKeyName(), CrossRefSetKey.PAGE.getShortKeyName()};
			@SuppressWarnings("rawtypes")
			List<List> body = new ArrayList<List>();
			List<String> list;
			String refValue, bookValue, pageValue;
			for (int i=0;i<resultTable.getLength();i++) {
				refValue = resultTable.getItem(CrossRefSetKey.INSTRUMENT_NUMBER.getShortKeyName(), "icw", i);
				bookValue = resultTable.getItem(CrossRefSetKey.BOOK.getShortKeyName(), "icw", i);
				pageValue = resultTable.getItem(CrossRefSetKey.PAGE.getShortKeyName(), "icw", i);
				list = new ArrayList<String>();
				Matcher matcher = Pattern.compile("(?s)\\A(\\d+)/(\\d+)\\z").matcher(refValue);
				if (matcher.find() && "".equals(bookValue) && "".equals(pageValue)) {
					list.add("");
					list.add(matcher.group(1));
					list.add(matcher.group(2));
					body.add(list);
				} else {
					if(StringUtils.isNotEmpty(refValue) && StringUtils.isNotEmpty(bookValue)) {
						if(refValue.matches("\\d{4}[A-Z]\\d+")) {
							prefix = refValue.replaceAll("\\d+", "");
							bookValue = prefix + bookValue;
						}
					}
					list.add(refValue);
					list.add(bookValue);
					list.add(pageValue);
					body.add(list);
				}
			}
			crossRef = GenericFunctions2.createResultTable(body, header);
			if (crossRef != null){
				m.put("CrossRefSet", crossRef);
			}
		}
	}	
}
