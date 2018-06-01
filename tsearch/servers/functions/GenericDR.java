package ro.cst.tsearch.servers.functions;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.utils.StringUtils;

public class GenericDR {
	
	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "NR");
		resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), "DEATH CERTIFICATE");
				
		TableColumn[] cols = row.getColumns();
		
		String last = cols[0].toPlainTextString().trim();
		String first = cols[1].toPlainTextString().trim();
		String middle = cols[2].toPlainTextString().trim();
		String name = last + " " + first + " " + middle;
		name = name.trim();
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(), name);
		resultMap.put(SaleDataSetKey.GRANTOR.getKeyName(), name);
		
		String birthDate = cols[3].toPlainTextString().trim();
		String deathDate = cols[4].toPlainTextString().trim();
		deathDate = deathDate.replaceAll("\\s\\([VP]\\)", "");
		if (!deathDate.substring(0,1).matches("\\d"))
			deathDate = "01 " + deathDate;
		DateFormat df1 = new SimpleDateFormat("dd MMM yyyy");
		DateFormat df2 = new SimpleDateFormat("MM/dd/yyyy");
		try {
			Date birth = df1.parse(birthDate);
			Date death = df1.parse(deathDate);
			birthDate = df2.format(birth);
			deathDate = df2.format(death);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), birthDate);
		resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), deathDate);
		
		resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), cols[5].toPlainTextString().trim());
		resultMap.put(SaleDataSetKey.GRANTEE.getKeyName(), cols[6].toPlainTextString().trim());
		
		parseNames(resultMap, searchId);
				
		return resultMap;
	}

	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap m, long searchId){
		
		String names[] = {"", "", "", "", "", ""};
		String[] suffixes, type, otherType;
		
		ArrayList<List> grantor = new ArrayList<List>();
		
		String tmpPartyGtor = (String)m.get(SaleDataSetKey.GRANTOR.getKeyName());
		if (StringUtils.isNotEmpty(tmpPartyGtor)){
			names = StringFormats.parseNameNashville(tmpPartyGtor, true);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
				
			GenericFunctions.addOwnerNames(tmpPartyGtor, names, suffixes[0],
						suffixes[1], type, otherType,
						NameUtils.isCompany(names[2]),
						NameUtils.isCompany(names[5]), grantor);
		}
			
		try {
			m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ArrayList<List> grantee = new ArrayList<List>();
		String tmpGtee = (String)m.get(SaleDataSetKey.GRANTEE.getKeyName());
		GenericFunctions.addOwnerNames(new String[] {tmpGtee, "", ""} , "", false, grantee);
		try {
			m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void parseAddress(ResultMap m, long searchId) throws Exception {
		
		String tmpAddress = (String)m.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
		if (StringUtils.isNotEmpty(tmpAddress)){
			//zip, city, county, state abbreviation
			Matcher matcher1 = Pattern.compile("(\\d\\d\\d\\d\\d)\\s(.*?),\\s(.*?),\\s(\\w\\w)").matcher(tmpAddress);
			if (matcher1.find())
			{
				m.put(PropertyIdentificationSetKey.ZIP.getKeyName(), matcher1.group(1));
				m.put(PropertyIdentificationSetKey.CITY.getKeyName(), matcher1.group(2));
				m.put(PropertyIdentificationSetKey.COUNTY.getKeyName(), matcher1.group(3));
				m.put(PropertyIdentificationSetKey.STATE.getKeyName(), matcher1.group(4));
			} else {
				//state name
				Matcher matcher2 = Pattern.compile("((?i)[A-Z\\s])").matcher(tmpAddress);	
				if (matcher2.find())
				{
					m.put(PropertyIdentificationSetKey.STATE.getKeyName(), matcher2.group(1));
				}
			}
		}
	}
	
}
