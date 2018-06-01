package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;

public class COGenericSOS {
	public static ResultMap parseIntermediaryRow(TableRow row, Integer additionalInfo) {
		ResultMap m = new ResultMap();

		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "CC");
		m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "UCC");

		TableColumn[] cols = row.getColumns();
		
		int nameIndex = -1;
		boolean isGood = false;
		if (cols.length==8) {
			nameIndex = additionalInfo;
			isGood = true;
		} else if (cols.length==7 || cols.length==6) {
			nameIndex = 2;
			isGood = true;
		}

		if (isGood) {
			// ucc no
			m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), cols[1].toPlainTextString().replace("&nbsp;", "").trim());
			String name = cols[nameIndex].toPlainTextString().replace("&nbsp;", " ").replace("&amp;", "&").trim();
			name = name.replaceAll("(?ism), Delinquent \\w+ \\d+, \\d+", "");
			name = name.replaceAll("(?ism), Disolved.*$", "");
			m.put(SaleDataSetKey.GRANTOR.getKeyName(), name);

			ArrayList<String> all_names = new ArrayList<String>();
			all_names.add(name);
			parseNames(m, all_names, "GrantorSet");

			// switch (additionalInfo) {
			// case 0:
			// case 1:
			// String name0 = cols[3].toPlainTextString().replace("&nbsp;", " ").trim();
			// m.put(SaleDataSetKey.GRANTOR.getKeyName(), name0);
			//
			// ArrayList<String> all_names0 = new ArrayList<String>();
			// all_names0.add(name0);
			// parseNames(m, all_names0, "GrantorSet");
			// break;
			// case 2:
			// case 3:
			// case 4:
			// String name4 = cols[2].toPlainTextString().replace("&nbsp;", " ").trim().replaceAll("\\s+", " ");
			// name4 = name4.replaceAll("(?ism), Delinquent \\w+ \\d+, \\d+", "");
			// m.put(SaleDataSetKey.GRANTOR.getKeyName(), name4);
			//
			// ArrayList<String> all_names4 = new ArrayList<String>();
			// all_names4.add(name4);
			// parseNames(m, all_names4, "GrantorSet");
			// break;
			// }
		}
		m.removeTempDef();
		return m;
	}

	public static void parseNames(ResultMap m, List<String> all_names, String setName) {
		try {
			@SuppressWarnings("rawtypes")
			ArrayList<List> body = new ArrayList<List>();

			for (String n : all_names) {
				n = StringUtils.strip(n);
				if (ro.cst.tsearch.utils.StringUtils.isNotEmpty(n)) {
					String[] names = StringFormats.parseNameNashville(n, true);
					String[] type = GenericFunctions.extractAllNamesType(names);
					String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
					String[] suffixes = GenericFunctions.extractNameSuffixes(names);

					GenericFunctions.addOwnerNames(n, names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), body);
				}
			}
			if (body.size() > 0)
				m.put(setName, GenericFunctions.storeOwnerInSet(body, true));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseAddress(ResultMap m, String addr) {
		addr = StringUtils.strip(StringUtils.defaultString(addr));

		if (ro.cst.tsearch.utils.StringUtils.isEmpty(addr))
			return;

		m.put(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(), addr);

		m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), StringUtils.defaultString(StringFormats.StreetNo(addr)));
		m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), StringUtils.defaultString(StringFormats.StreetName(addr)));
	}
}
