package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class KSWyandotteOR {
	@SuppressWarnings("rawtypes")
	public static void parseGrantorGranteeSetOR(ResultMap m, long searchId) throws Exception {

		String grantor = (String) m.get("SaleDataSet.Grantor");
		String grantee = (String) m.get("SaleDataSet.Grantee");

		if (StringUtils.isEmpty(grantee)
				&& StringUtils.isNotEmpty((String) m.get("SaleDataSet.GranteeLander"))) {
			grantee = (String) m.get("SaleDataSet.GranteeLander");
		}
		
		if(StringUtils.isEmpty(grantor) && StringUtils.isEmpty(grantee)){
			return;
		}
		
		grantor = StringUtils.prepareStringForHTML(grantor);
		grantee = StringUtils.prepareStringForHTML(grantee);
		grantor = NameCleaner.cleanFreeformName(grantor);
		grantee = NameCleaner.cleanFreeformName(grantee);

		// clean grantor
		grantor = grantor
				.replaceAll("\\w+/\\w*", "")
				.replaceAll("\\bAS\\b", "")
				.replaceAll("\\bMARRIED\\b", "")
				.replaceAll("RIGHTS OF SURVIVORSHIP", "")
				.replaceAll("\\bJT\\b", "")
				.replaceAll("\\bNKA\\b", "")
				.replaceAll("\\bNTIC\\b", "")
				.replaceAll("\\bSGL\\b", "")
				.replaceAll(",\\s*,", ",")
				.replaceAll("&\\s*$", "")
				.replaceAll(",\\s*$", "")
				.replaceAll("\\s+", " ").trim();

		// clean grantee
		grantee = grantee
				.replaceAll("\\w+/\\w*", "")
				.replaceAll("\\bAS\\b", "")
				.replaceAll("\\bMARRIED\\b", "")
				.replaceAll("RIGHTS OF SURVIVORSHIP", "")
				.replaceAll("\\bJT\\b", "")
				.replaceAll("\\bNKA\\b", "")
				.replaceAll("\\bNTIC\\b", "")
				.replaceAll("\\bSGL\\b", "")
				.replaceAll(",\\s*,", ",")
				.replaceAll("&\\s*$", "")
				.replaceAll(",\\s*$", "")
				.replaceAll("\\s+", " ").trim();

		ArrayList<List> grantorList = new ArrayList<List>();
		ArrayList<List> granteeList = new ArrayList<List>();

		parseNameInner(m, grantor, grantorList, searchId, false);
		parseNameInner(m, grantee, granteeList, searchId, true);

		m.put("SaleDataSet.Grantor", grantor);
		m.put("SaleDataSet.Grantee", grantee);

		m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantorList, true));
		m.put("GranteeSet", GenericFunctions.storeOwnerInSet(granteeList, true));

		GenericFunctions1.setGranteeLanderTrustee2(m, searchId, true);
		GenericFunctions.checkTNCountyROForMERSForMortgage(m, searchId);
		
		m.put(SaleDataSetKey.GRANTOR.getKeyName(), GenericParsingOR.concatenateNames(grantorList));
		m.put(SaleDataSetKey.GRANTEE.getKeyName(), GenericParsingOR.concatenateNames(granteeList));
	}

	@SuppressWarnings("rawtypes")
	public static void parseNameInner(ResultMap m, String name,
			ArrayList<List> namesList, long searchId, boolean isGrantee) {

		if (StringUtils.isEmpty(name))
			return;

		name = name.replaceAll("(?is)\\b(Trustee) Of\\b", "$1 / ");
		name = name.replaceAll("(?is)\\s*,\\s*DEC'?D\\s*", "");
		name = name.replaceAll("(?is)\\s*,\\s*SVR\\s*", "");
		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;

		String[] nameItems = name.split("/");

		if (name.split(" ").length >= 4 && org.apache.commons.lang.StringUtils.countMatches(name, ",") >= 3) {
			name = name.replaceAll("(\\w+, \\w+), ", "$1&&");
			nameItems = name.split("&&");
		}

		if (name.matches("\\w+, \\w+ & \\w+, \\w+")) {
			nameItems = name.split("&");
		}

		for (int i = 0; i < nameItems.length; i++) {

			names = StringFormats.parseNameNashville(nameItems[i], true);

			if (nameItems[i].matches("\\w+\\s+\\w+\\s+\\w+\\s+\\w")
					&& names[1].contains(" ")) {
				names[2] = names[2] + " " + names[0];
				names[0] = names[1].split(" ")[0];
				names[1] = names[1].split(" ")[1];
			}

			for (int j = 0; j < names.length; j++) {
				names[j] = names[j].replace(",", "");
			}

			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(nameItems[i], names, suffixes[0],
					suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), namesList);
		}
	}
}
