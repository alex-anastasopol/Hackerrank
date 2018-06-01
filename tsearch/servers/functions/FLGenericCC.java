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

/**
 * 
 * @author Oprina George
 * 
 *         Oct 25, 2011
 */

public class FLGenericCC {

	public static ResultMap parseIntermediaryRow(TableRow row, Integer additionalInfo) {
		ResultMap m = new ResultMap();

		m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "CC");
//		m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "UCC");

		TableColumn[] cols = row.getColumns();

		if (cols.length == 8) {
			// name
			String name = cols[0].toPlainTextString();
			m.put(SaleDataSetKey.GRANTOR.getKeyName(), name);
			ArrayList<String> all_names = new ArrayList<String>();
			all_names.add(name);
			parseNames(m, all_names, "GrantorSet");

			// ucc no
			m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), StringUtils.defaultString(cols[1].toPlainTextString()));

			// addr
			parseAddress(m, cols[2].toPlainTextString());

			// city
			m.put(PropertyIdentificationSetKey.CITY.getKeyName(), StringUtils.defaultString(cols[3].toPlainTextString()));

			// zip
			m.put(PropertyIdentificationSetKey.ZIP.getKeyName(), StringUtils.defaultString(cols[5].toPlainTextString()));
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
