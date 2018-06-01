package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class GenericIL {

	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap m, long searchId) throws Exception {
		
		String names[] = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		
		ArrayList<List> grantor = new ArrayList<List>();
		
		String tmpPartyGtor = (String)m.get(SaleDataSetKey.GRANTOR.getKeyName());
		if (StringUtils.isNotEmpty(tmpPartyGtor)){
			
			names = StringFormats.parseNameFML(tmpPartyGtor, new Vector<String>(), true, true);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
				
			GenericFunctions.addOwnerNames(tmpPartyGtor, names, suffixes[0],
						suffixes[1], type, otherType,
						NameUtils.isCompany(names[2]),
						NameUtils.isCompany(names[5]), grantor);
		}
		m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
						
		ArrayList<List> grantee = new ArrayList<List>();
		String tmpGtee = (String)m.get(SaleDataSetKey.GRANTEE.getKeyName());
		GenericFunctions.addOwnerNames(new String[] {tmpGtee, "", ""} , "", false, grantee);
		m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
	}
}
