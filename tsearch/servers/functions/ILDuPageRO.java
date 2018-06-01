package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;

public class ILDuPageRO {
	public static Pattern crossRefRemarks = Pattern
		.compile("\\b(\\d{2}[A-Z](-|\\s)\\d+)\\b");
	public static Pattern crossRefRemarks2 = Pattern
		.compile("\\b([A-Z]\\d{2,4}-?\\d{3,})\\b");
	
	public static ArrayList<Pattern> CRPatterns = new ArrayList<Pattern>();
	static{
		CRPatterns.add(crossRefRemarks);
		CRPatterns.add(crossRefRemarks2);
	}
	public static ArrayList<Pattern> plat = new ArrayList<Pattern>();
	static{
		plat.add(Pattern.compile("BK (\\d+) PG (\\d+)"));
	}
	
	
	public static Pattern lotRemarks = Pattern.compile("\\bL\\-(\\d+)");
	public static Pattern blockRemarks = Pattern
		.compile("\\bB\\-(\\d+|\\w{1,2})\\b");
	public static Pattern unitRemarks = Pattern
		.compile("\\bUNI?T?S? ?(?:NO |# ?)?(\\d[\\d\\w]{0,2}\\s*-?\\s*[A-Z]?\\d*(,\\d*-\\d*)*)\\b");
	public static Pattern ordRemarks = Pattern
		.compile("\\bORD\\s+#?([\\w-]+)\\b");
	
	public static ArrayList<String> grantorPartyType = new ArrayList<String>();
	static{
		grantorPartyType.add("MISC 1");
		grantorPartyType.add("GRNTOR");
		grantorPartyType.add("CORP 1");
		grantorPartyType.add("MRTGOR");
		grantorPartyType.add("SECPTY");
		grantorPartyType.add("CLMANT");
		grantorPartyType.add("RLSE 1");
		grantorPartyType.add("PLNTIF");
		grantorPartyType.add("ASINOR");
		grantorPartyType.add("ASMT 1");
		grantorPartyType.add("SELLER");
	}

	public static ArrayList<String> granteePartyType = new ArrayList<String>();
	static{
		granteePartyType.add("MISC 2");
		granteePartyType.add("GRNTEE");
		granteePartyType.add("MRTGEE");
		granteePartyType.add("DEFNDT");
		granteePartyType.add("DEBTOR");
		granteePartyType.add("ASINEE");
		granteePartyType.add("ASMT 2");
		granteePartyType.add("BUYER");

	}
	
	private static final String[] trustList = {
		"\\bLIVING TRUST\\b.*",
		"\\bLIVING TR\\b.*",
		"\\bLIV TRUST\\b.*",
		"\\bLIV TR\\b.*",
		
		"\\bFAMILY TRUST\\b.*",
		"\\bFAMILY TR\\b.*",
		"\\bFAM TRUST\\b.*",
		"\\bFAM TR\\b.*",
		
		"\\bMARITAL TRUST\\b.*",
		"\\bMARITAL TR\\b.*",
		
		"\\bREVOCABLE TRUST\\b.*",
		"\\bREVOCABLE TR\\b.*",
		"\\bREV DECL TR\\b.*",
		
		"\\bIRREV TRUST\\b.*",
		"\\bIRRV TRUST\\b.*",
		"\\bIRREV TR\\b.*",
		"\\bIRRV TR\\b.*",
		
		
		"\\bJOINT TRUST\\b.*",
		"\\bJOINT TR\\b.*",
		"\\bJNT TRUST\\b.*",
		"\\bJNT TR\\b.*",
		"\\bJT TRUST\\b.*",
		"\\bJT TR\\b.*",
		
		"\\DECL TRUST\\b.*",
		"\\DECL TR\\b.*",
		
		"\\bTRUST\\b.*",
		
		
	};
	
	public static void partyNamesILDuPageRO(ResultMap m,long searchId) throws Exception {
		String owner = (String)m.get("tmpPeople");
		if (StringUtils.isEmpty(owner)){
			return;
		}
		String splitOwner[] = owner.split("/");
		String partyType = (String)m.get("tmpPartyType");
		String splitPartyType[] = null;
		if (StringUtils.isEmpty(partyType)){
			splitPartyType = new String[splitOwner.length];
			for (int i=0; i<splitPartyType.length; i++){
				splitPartyType[i] = granteePartyType.get(0);
			}
		} else {
			splitPartyType = partyType.split("/");
			if (splitOwner.length != splitPartyType.length){
				return;
			}
		}
		ArrayList<List> grantor = new ArrayList<List>();
		ArrayList<List> grantee = new ArrayList<List>();
		
		String grantorAsString = "";
		String granteeAsString = "";
		
		for(int i =0; i<splitOwner.length; i++){
			if(!splitOwner[i].isEmpty()) {
				String names[] = {"", "", "", "", "", ""};
				String namesTrustCleaned[] = {"", "", "", "", "", ""};
				String temporaryName = cleanTrust(splitOwner[i],"").trim();
				
				namesTrustCleaned = StringFormats.parseNameNashville(cleanOwnerILDuPageRO(temporaryName));
				
				names = StringFormats.parseNameNashville(cleanOwnerILDuPageRO(splitOwner[i]));
				
				
				if (granteePartyType.contains(splitPartyType[i])){
					granteeAsString += splitOwner[i] + "/";
					if(!temporaryName.equalsIgnoreCase(splitOwner[i]) && 
							StringUtils.isNotEmpty(namesTrustCleaned[0])) {
						GenericFunctions.addOwnerNames(splitOwner[i], namesTrustCleaned, grantee);	
					}
					GenericFunctions.addOwnerNames(splitOwner[i], names, grantee);
				} else {
					grantorAsString += splitOwner[i] + "/";
					if(!temporaryName.equalsIgnoreCase(splitOwner[i]) && 
							StringUtils.isNotEmpty(namesTrustCleaned[0])) {
						GenericFunctions.addOwnerNames(splitOwner[i], namesTrustCleaned, grantee);	
					}
					GenericFunctions.addOwnerNames(splitOwner[i], names, grantor);
				}
			}
		}
		if(StringUtils.isNotEmpty(granteeAsString)) {
			m.put("SaleDataSet.Grantee", granteeAsString.substring(0,granteeAsString.length() - 1));
		}
		if(StringUtils.isNotEmpty(grantorAsString)) {
			m.put("SaleDataSet.Grantor", grantorAsString.substring(0,grantorAsString.length() - 1));
		} 
		m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee));
		m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor));
		
		GenericFunctions1.setGranteeLanderTrustee2(m, searchId,true);
		

	}
	
	public static String cleanTrust(String initialOwnerName, String prefix) {
		String newName = initialOwnerName.toUpperCase();
		for(String trustAlias : trustList) {
			if(newName.matches( ".*" + prefix + trustAlias)) {
				newName = newName.replaceAll(prefix + trustAlias, "");
			}
		}
		return newName;
	}

	public static void partyNamesILDuPageROinter(ResultMap m,long searchId) throws Exception {
		String owner = (String)m.get("tmpParty");
		if (StringUtils.isEmpty(owner)){
			return;
		}
		owner = cleanOwnerILDuPageRO(owner);
		m.put("GrantorSet", StringFormats.parseGrantorGranteeSetFromString(owner, "aaaaaaaaaaaaaaaaaaaaaaaaaaa"));
		

	}	
	
	public static String cleanOwnerILDuPageRO(String s){
		s = s.replaceAll("\\*", "");
		s = s.replaceAll("\\s{2,}", "\\s");
		return s;
	}
	
	public static void reparseDocTypeILDuPageRO(ResultMap m, long searchId) throws Exception{
		
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		String docType = (String) m.get("SaleDataSet.DocumentType");
		if (StringUtils.isNotEmpty(legal)){
			if (legal.matches("(?is)\\ARR\\s+.*")){
				if (StringUtils.isNotEmpty(docType)){
					if (docType.matches("MORTG")){
						m.put("SaleDataSet.DocumentType", "MORTG + RR");
					} else if (docType.matches("MORTGAGE")){
						m.put("SaleDataSet.DocumentType", "MORTGAGE + RR");
					} else if (docType.matches("DEED")){
						m.put("SaleDataSet.DocumentType", "DEED + RR");
					} else if (docType.matches("DEEDS")){
						m.put("SaleDataSet.DocumentType", "DEEDS + RR");
					} else if (docType.matches("ASSIGN")){
						m.put("SaleDataSet.DocumentType", "A M + RR");
					}
				}
			}
		}
		
	}
	
	public static void legalILDuPageRO(ResultMap m, long searchId) throws Exception{
		String legal = (String)m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(legal)){
			return;
		}
		legalTokenizerILDuPageRO(m, legal);
	}
	
	public static void legalTokenizerILDuPageRO(ResultMap m, String legal) throws Exception{
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers to arabics
		legal = GenericFunctions.replaceNumbers(legal);
		
		legal = legal.replaceAll("\\b[NEWS]{1,2}\\s?\\d/\\d\\b", "");
		legal = legal.replaceAll("\\b\\d+-\\d+-\\d{4,}\\b", "");
		legal = legal.replaceAll("\\b\\d{4,}\\d+-\\d+\\b", "");
		legal = legal.replaceAll("\\b\\d+-\\d{4,}-\\d+\\b", "");

		legal = legal.replaceAll("\\s{2,}", " ");
		legal = legal.trim();
		
		Pattern p;
		Matcher ma = ordRemarks.matcher(legal);
		if(ma.find()) {
			String ord = ma.group(1);
			if(StringUtils.isNotEmpty(ord)) {
				m.put("OtherInformationSet.Remarks", "ORD #" + ord );
			}
		}
		
		legal = legal.replaceAll("\\bORD #?\\d+(-\\d+)?", "");
		legal = legal.replaceAll("\\s{2,}", " ");
		legal = legal.trim();
		
		
		//platDescription          
		Pattern pattern = Pattern.compile("(ORD #?)?((\\d{1,3})-)?\\b(\\d{1,3})-(\\d{1,3})\\b");
		Matcher matcher = pattern.matcher(legal);
		while (matcher.find()){
			String group = matcher.group();
			if(!group.startsWith("ORD")){
				String platDesc = (String) (m.get("PropertyIdentificationSet.PlatDesc"));
				platDesc= platDesc==null?"":platDesc;
				if (!platDesc.contains(group)){
					m.put("PropertyIdentificationSet.PlatDesc", (StringUtils.isEmpty(platDesc)?group:platDesc+ " "+group) );
				}
				
			}
		}
		// extract and replace tract from legal description
		String tract = (String) m
				.get("PropertyIdentificationSet.SubdivisionTract");
		if (tract == null) {
			tract = "";
		}
		String patt = "([A-Z0-9-]|\\d+ )";
		p = Pattern.compile("\\bTR(?:A?CTS?)? (" + patt + "+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract += " " + ma.group(1);
		}
		tract = LegalDescription.cleanValues(tract, false, true);
		if (tract.length() > 0) {
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}
		ma.reset();
		legal = ma.replaceAll("");




		// extract and remove building # from legal description
		p = Pattern.compile("\\bB(?:UI)?LD(?:IN)?G ([A-Z0-9])\\b");
		String building = (String) m
				.get("PropertyIdentificationSet.SubdivisionBldg");
		if (building == null) {
			building = "";
		}
		ma = p.matcher(legal);
		if (ma.find()) {
			building += " " + ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionBldg", building);
		}
		ma.reset();
		legal = ma.replaceAll("");

		//unit
		ma = unitRemarks.matcher(legal);
		String unit = (String) m
				.get("PropertyIdentificationSet.SubdivisionUnit");
		if (unit == null) {
			unit = "";
		}
		while (ma.find()) {
			unit += " " + ma.group(1);
		}
		unit = unit.replaceAll("NO", "");
		unit = LegalDescription.cleanValues(unit.trim(), false, true);
		if (!"".equals(unit)) {
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
		}
		ma.reset();
		legal = ma.replaceAll("");

		//lot

		p = Pattern
				.compile("\\bL(?:S?)?\\s?([\\d-,]+(?: ?& ?)?)+\\b");
		ma = p.matcher(legal);
		String lot = (String) m
				.get("PropertyIdentificationSet.SubdivisionLotNumber");
		if (lot == null) {
			lot = "";
		}
		while (ma.find()) {
			lot += " " + ma.group(0).replaceAll("&|L(?:S?)", " ").replaceFirst("-$", "");
		}
		lot = LegalDescription.cleanValues(lot, false, true);
		if (!"".equals(lot)) {
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		ma.reset();
		legal = ma.replaceAll("");

		//block
		p = Pattern
				.compile("\\bB(?:L?(?:OC)?S?\\s*)?((\\d+,?|-|\\s|&)+)");
		ma = p.matcher(legal);
		String block = (String) m
				.get("PropertyIdentificationSet.SubdivisionBlock");
		if (block == null) {
			block = "";
		}
		while (ma.find()) {
			block += " " + ma.group(1).replaceAll("&", " ");
		}
		block = block.replaceAll("\\b(OF|EL)\\b", "");
		block = LegalDescription.cleanValues(block, false, true);
		if (!"".equals(block)) {
			if(block.startsWith("-")) {
				block = block.replaceFirst("\\-", "");
			}
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		ma.reset();
		legal = ma.replaceAll("");

		//phase
		// extract phase from legal description
		String phase = (String) m
				.get("PropertyIdentificationSet.SubdivisionPhase");
		if (phase == null) {
			phase = "";
		}
		p = Pattern.compile("\\bPH(?:ASES?)? (((\\d+\\w?|\\b[A-Z]\\b))+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase += " " + ma.group(1).trim();
			phase = LegalDescription.cleanValues(phase, false, true);
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
		}
		
		//crossref
		
		List<List> bodyCR = new ArrayList<List>();
		String tmpCross = (String) m.get("tmpCrossRef");
		if (!StringUtils.isEmpty(tmpCross)){
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(fixInstrNumber(tmpCross));
			bodyCR.add(line);
		}
		
		for (int j=0; j<CRPatterns.size(); j++){
			Matcher bpFinal = CRPatterns.get(j).matcher(legal);
			while (bpFinal.find()) {
				List<String> line = new ArrayList<String>();
				line.add("");
				line.add("");
				line.add(fixInstrNumber(bpFinal.group(1)));
				bodyCR.add(line);
				
			}
			bpFinal.reset();
			legal = bpFinal.replaceAll("");
		}
		
		if (bodyCR.size() > 0){
			ResultTable cr = (ResultTable) m.get("CrossRefSet");
			if (cr == null) {
				String[] header = { "Book", "Page", "InstrumentNumber" };
				Map<String, String[]> map = new HashMap<String, String[]>();
				map.put("Book", new String[] { "Book", "" });
				map.put("Page", new String[] { "Page", "" });
				map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
				cr = new ResultTable();
				cr.setHead(header);
				cr.setBody(bodyCR);
				cr.setMap(map);
	
			} else {
				String[][] b = cr.getBodyRef();
				for (int i = 0; i < b.length; i++) {
					List<String> line = new ArrayList<String>();
					line.add(b[i][0]);
					line.add(b[i][1]);
					line.add(b[i][2]);
					bodyCR.add(line);
				}
				cr.setBody(bodyCR);
			}
			m.put("CrossRefSet", cr);
		}

		//section-township-range
		// extract section, township and range from legal description

		List<List> body = new ArrayList<List>();
		//body = GenericFunctions.goToGetSTRFromMapFromPIS(m, body);

		//20 & 28 & 29-40-10
		Pattern pstr = Pattern.compile("\\b((?:\\d+ ?(?:& ?)?)+(?:\\d+))-(\\d{1,3})-(\\d{1,3})\\b");
		ma = pstr.matcher(legal);
		if (ma.find()){
			String str = ma.group(1);
			String tw = ma.group(2);
			String rg = ma.group(3);
			String[] sections = str.split("(\\s*&\\s*|\\s)");
			for (int i=0; i<sections.length; i++){
				List<String> line = new ArrayList<String>();
				line.add(sections[i]);
				line.add(tw);
				line.add(rg);
				body.add(line);
			}
			legal = ma.replaceAll("");
			
			
		}					
		ArrayList<Pattern> secs = new ArrayList<Pattern>();
		secs.add(Pattern.compile("\\bSEC (\\d+) (\\d+)-(\\d+)\\b"));
		secs.add(Pattern.compile("\\b(\\d{1,3})-(\\d{1,3})-(\\d{1,3})\\b"));
		secs.add(Pattern.compile("\\b()(\\d{1,3})-(\\d{1,3})\\b"));
		for (int i = 0; i < secs.size(); i++) {
			ma = secs.get(i).matcher(legal);
			while (ma.find()) {
				List<String> line = new ArrayList<String>();
				if (ma.group(1).equals("BLK")) {
					continue;
				}
				line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
				line.add(ma.group(2).replaceFirst("^0+(\\d+)", "$1"));
				line.add(ma.group(3).replaceFirst("^0+(\\d+)", "$1"));
				body.add(line);
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
			}
			ma.reset();
			legal = ma.replaceAll("");
		}
		GenericFunctions.joinResultTableInMap(m, body, new String[]{"SubdivisionSection", "SubdivisionTownship", "SubdivisionRange"},
							"PropertyIdentificationSet");				
		//plat book and plat page
		body.clear();
		for(int i=0; i<plat.size(); i++){
			ma = plat.get(i).matcher(legal);
			while(ma.find()){
				List<String> line = new ArrayList<String>();
				line.add(ma.group(1));
				line.add(ma.group(2));
				body.add(line);
				
			}
		}
		if (body.size() > 0){
			String pb = (String) m.get("PropertyIdentificationSet.PlatBook");
			String pp = (String) m.get("PropertyIdentificationSet.PlatNo");
			if (!StringUtils.isEmpty(pb)){
				List<String> line = new ArrayList<String>();
				line.add(pb);
				line.add(pp);
				body.add(line);
			}
			GenericFunctions.joinResultTableInMap(m, body, new String[]{"PlatBook", "PlatNo"},
				"PropertyIdentificationSet");
		}
			
	}
	
	/**
	 * takes instrument numbers and normalize then to the format [A-Z]YYYY-\d{6}.<br>
	 * We need to do that to be able to search with these instrument numbers 
	 * @param inst - the instrument number parsed. YY[A-Z](-| )\d{1,6} and [A-Z]YY(-| )\d{1,6} formats are supported
	 * @return the normalized intrument number
	 */
	public static String fixInstrNumber(String inst){
		inst = inst.toUpperCase();
		String[] s = inst.split("(-| )");
		if (s.length == 2){
			String year = s[0];
			String ino = s[1];
			String code = "";
			for(int i=0; i<6-s[1].length(); i++){
				ino = "0" + ino;
			}
			Matcher ma = Pattern.compile("([A-Z])").matcher(year);
			if (ma.find()){
				code = ma.group(1);
				year = year.replaceFirst(code, "");
				try{
					int y = Integer.parseInt(year);
					if (y < 100 || y > 1000){
						if (y<25){
							y += 2000;
						} else if (y >= 25 && y<100){
							y += 1900;
						}
						//return code + y + "-" + ino;
						return code + y + ino;
					}
				} catch (NumberFormatException e){}
			}
			
		}
		inst.replaceAll("[-\\s]", "");
		return inst;
	}
	
	public static void fixInstrumentForOcr(Instrument instrument){
		String inst = fixInstrumentForOcr(instrument.toInstrumet());
		instrument.setInstrumentNo(inst);
	}
	
	public static String fixInstrumentForOcr(InstrumentI instrument) {
		String inst = instrument.getInstno();
		inst = fixInstrNumber(inst);
		if(inst.length() != 11) {
			int year = instrument.getYear();
    		String instrumentNumber = instrument.getInstno();
    		if( !StringUtils.isEmpty(instrumentNumber) && year != SimpleChapterUtils.UNDEFINED_YEAR && instrumentNumber.length() <= 6) {
    			inst = "R" + year + instrumentNumber;
    		}
		}
		instrument.setInstno(inst);
		return inst;
	}
	
	public static void main(String[] args) {
		/*String[] intrString = new String[] {"R78-37628","R78-44157","R78-562424"};
		for (String string : intrString) {
			System.out.println(string + " -> " + fixInstrNumber(string));
		}*/
		
		String[] stringArray = new String[] {
				"ORD #90-7 L 2 SMITH ASSMT PL & L 2 WARNER ASSMT PL 16-40-9",
				"ORD 95-3 L 2 SMITH ASSMT PL 40-9 & L 2 WARNER ASSMT PL 40-9",
				"ORD #0-6-82 PTS SMITH & ALBEN AVE BATES ADD 39-11, YK-2B & P",
				"ORD #413 VAC WALKWAY",
				"ORD 1302 OUTL 1 HURLINGHAM UN 1 39-9",
				"ORD #84-05-22 AMND ORD ETC SHINING WATERS UN 10 40-9, WA-25A",
				"ORD 0-93-12 APPR FINAL PL RESUB BACHI DIV 19-40-11",
				"ORD #81-21 AUTHORIZE FINAL PLAT APPROVAL WHISPERING OKS UN II",
				"ORD O-07-142 PT LS 113-117 BOESKE KLOCK ADD ADDSN UN 2 40-11"
		};
		
		ResultMap map = new ResultMap();
		for (int i = 0; i < stringArray.length; i++) {
			map.put("OtherInformationSet.Remarks",null);
			try {
				ILDuPageRO.legalTokenizerILDuPageRO(map, stringArray[i]);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(map.get("OtherInformationSet.Remarks"));
		}
		
		
		
	}
	
	
}

