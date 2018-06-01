package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class TNShelbyPR {

	public static ResultMap parseIntermediaryRow(String row) {
		ResultMap resultMap = new ResultMap();
		row = row.replaceAll("&nbsp;", " ");
		String[] cols = row.replaceAll("</?tr[^>]*>", "").split("(?is)</td>");

		if (cols.length >= 4) {
			String caseNo = cols[0].replaceAll("<[^>]*>", "").replaceAll("\\s+", "");
			String grantor = cols[1].replaceAll("<[^>]*>", "").trim();
			String caseType = cols[2].replaceAll("<[^>]*>", "").trim();
			String filed = cols[3].replaceAll("<[^>]*>", "").trim();
			resultMap.put("tmpGrantor", grantor);

			if (!StringUtils.isEmpty(filed)) {
				resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), filed);
			}

			if (!StringUtils.isEmpty(caseNo)) {
				resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), caseNo);
			}

			if (!StringUtils.isEmpty(caseType)) {
				resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), caseType);
			}
			parseNames(resultMap);
		}
		resultMap.removeTempDef();
		return resultMap;
	}

	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap) {

		ArrayList<List> grantor = new ArrayList<List>();
		String tmpGrantor = (String) resultMap.get("tmpGrantor");
		if (StringUtils.isNotEmpty(tmpGrantor)) {
			parseName(tmpGrantor, grantor);
		}
		if (grantor.size() > 0) {
			try {
				resultMap.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), concatenateNames(grantor));
	}

	@SuppressWarnings("rawtypes")
	public static void parseName(String rawNames, ArrayList<List> list) {

		String names[] = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;

		String split[] = rawNames.split(" / ");

		for (int i = 0; i < split.length; i++) {
			String name = split[i];

			boolean isDeSoto = false;
			Matcher ma1 = Pattern.compile("^(.*?),\\s*Guardian\\b").matcher(name);
			if (ma1.find()) {
				name = ma1.group(1);
				isDeSoto = true;
			} else {
				Matcher ma2 = Pattern.compile("^(.*?),\\s*.*\\s*County\\s*$").matcher(name);
				if (ma2.find()) {
					name = ma2.group(1);
					isDeSoto = true;
				}
			}

			if (!StringUtils.isEmpty(name)) {
				if (isDeSoto) {
					names = StringFormats.parseNameDesotoRO(name, true);
				} else {
					names = StringFormats.parseNameNashville(name, true);
				}
				type = GenericFunctions.extractAllNamesType(names);
				otherType = GenericFunctions.extractAllNamesOtherType(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);

				GenericFunctions.addOwnerNames(split[i], names, suffixes[0], suffixes[1], type, otherType,
						NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), list);
			}

		}
	}

	@SuppressWarnings("rawtypes")
	public static String concatenateNames(ArrayList<List> nameList) {
		String result = "";

		StringBuilder resultSb = new StringBuilder();
		for (List list : nameList) {
			if (list.size() > 3) {
				resultSb.append(list.get(3)).append(", ").append(list.get(1)).append(" ").append(list.get(2)).append(" / ");
			}
		}
		result = resultSb.toString().replaceAll("/\\s*,\\s*/", " / ").replaceAll(",\\s*/", " /").
				replaceAll("\\s{2,}", " ").replaceAll("/\\s*$", "").trim();
		return result;
	}

}
