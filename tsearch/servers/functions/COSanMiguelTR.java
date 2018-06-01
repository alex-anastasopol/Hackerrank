package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;


import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;

public class COSanMiguelTR {
	
	public static Pattern  nameFMFMLPattern = Pattern.compile("(?ism)\\w+\\s+\\w\\s+&\\s+(?:\\w\\s?)*\\s(\\w+)");
	
	public static Pattern  addressPattern = Pattern.compile("(?is)(.+?)\\s+((?:[A-Z]\\s+)?[\\d-]+)(\\s+[\\d-]+)?\\s*$");
	
	private static final String[] CITIES = {
		"MTN VIL",
		"MOUNTAIN VILLAGE",
		"NORWOOD",
		"OPHIR",
		"PLACERVILLE",
		"SAWPIT",
		"TELLURIDE"
	};

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "TR");
		
		TableColumn[] cols = row.getColumns();
		if(cols.length == 3) {
			String[] col0 = cols[0].toPlainTextString().trim().split("\\s+");
			resultMap.put("PropertyIdentificationSet.ParcelID", col0[1]);
			resultMap.put("TaxHistorySet.Year", col0[2].replaceAll("[()]", ""));
			resultMap.put("PropertyIdentificationSet.AddressOnServer", cols[2].toPlainTextString().trim());
			
			String ownerName = cols[1].toPlainTextString().trim().replace("&", "\n");
			resultMap.put("PropertyIdentificationSet.NameOnServer", ownerName);
			try {
				parseNames(resultMap, searchId);
				parseAddress(resultMap, searchId);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		return resultMap;
	}
	
	@SuppressWarnings("rawtypes")
	public static void parseNames(ResultMap resultMap, long searchId) throws Exception {
	
		String ownerName = (String) resultMap.get("PropertyIdentificationSet.NameOnServer");
		
		if(StringUtils.isEmpty(ownerName)) {
			return;
		}
		
		ownerName = ownerName.replaceAll("\\bAN INDIVIDUAL(\\s+PERSON)?\\b", "").trim();
		ownerName = ownerName.replaceAll("\\b(ET)\\s+(AL|UX|VIR)\\b", "$1$2");
		ownerName = ownerName.replaceAll("`+", "");
		//ownerName = ownerName.replaceAll("\\bTRUSTEE UNDER?\\s*\n", " AND ");
		ownerName = ownerName.replaceAll("\\b(TRUSTEE?S?)(\\s+UNDER?(?:\\s+THE?)?)?\\b", "$1");
		ownerName = ownerName.replaceAll("\\b(?:AS\\s+)(T(RU?S?)?TEE?S?)(?:\\s+OF(?:\\s+THE)?(?:\\s+[A-Z]{1,3})?)?\\s*$", " $1");
		ownerName = ownerName.replaceAll("\\b(?:CO\\s+)?(T(RU?S?)?TEE?S?)(?:\\s+OF)?\\b", " $1");
		ownerName = ownerName.replaceAll("\\bDTD\\s+[\\d\\s-]+", "");
		ownerName = ownerName.replaceAll("\\bAS\\b", "");
		
		ownerName = ownerName.replaceAll("(?:-|\\b|AS\\s+)?\\s*JTS?(WROS)?\\b", "");
		ownerName = ownerName.replaceAll("[0-9]+%", "");
		ownerName = ownerName.replaceAll("\\s*\n\\s*([A-Z]\\s+[A-Z])\\s*$", " and $1");
		ownerName = ownerName.replaceAll("\\bCARE\\s+OF\\b", "C/O ");
		ownerName = ownerName.replaceAll("\\bC\\s+O\\b", "C/O ");
		ownerName = ownerName.replaceAll("\n\\s*%", "\nC/O ");
		ownerName = ownerName.replaceAll("\\s+C/O", "\nC/O");

		//2030022110
		ownerName = ownerName.replaceAll("\\b(THE)\\s*\n", "\n$1 ");
		ownerName = ownerName.replaceAll("\\b(TRT)\\s+(PO BOX)", "$1\n$2");
		
		String[] ownerRows = ownerName.split("\n");
		StringBuffer stringOwnerBuff = new StringBuffer();
		for (String row : ownerRows){
			if (row.trim().matches("\\A\\s*\\d+.*")){
				break;
			} else if (row.toLowerCase().contains("box") || row.toLowerCase().contains("rfd")){
				break;
			} else if (LastNameUtils.isNoNameOwner(row)) {
			   break;
			} else {
				if (row.trim().endsWith(" AND") || row.trim().matches("\\w+\\s+[A-Z]")){
					stringOwnerBuff.append(row.replaceAll("(?is)\\A\\s*-", "").trim() + " ");
				} else {
					if (row.trim().endsWith(" TC")){
						stringOwnerBuff.append("\n" + row.replaceAll("\\s+TC\\s*$", "").trim() + "\n");
					} else {
						stringOwnerBuff.append(row.trim() + "\n");
					}
				}
			}
		}
		String stringOwner = stringOwnerBuff.toString();
		stringOwner = stringOwner.replaceAll("\n$", "");
		stringOwner = stringOwner.replaceAll("\\bOR\\s*\n\\s*HER SUCCESSORS IN TRUST\\s*$", "");
		stringOwner = stringOwner.replaceAll("\\bAND\\s+OR\\b", " AND ");
		stringOwner = stringOwner.replaceAll("\n\\s*-?\\s*((?:TR(?:US)?TE?E?|DA?TE?D|REVOCABLE|FAMILY|LIVING|RES\\s+TRU?S?T).*)", " $1");
		stringOwner = stringOwner.replaceAll("\n\\s*-", " ");
		stringOwner = stringOwner.replaceAll("\\b(MICHELL)\\s+(LEE)", "$1E $2");
		stringOwner = stringOwner.replaceAll("\\b(WILLIAM)\n(LILA)", "$1S $2");
		stringOwner = stringOwner.replaceAll("\\b(AKA)\\s*\n", "\n$1 ");
		stringOwner = stringOwner.replaceAll("\\b,", "\nC/O ");//1060060345
		stringOwner = stringOwner.replaceAll("\\b(AND\\s+\\w+)\\s*\n\\s*(\\w+\\s+[A-Z])\\s*$", "$1 $2 ");//2010099003      
		stringOwner = stringOwner.replaceAll("\\bDR AND MRS\\b", "");
		
		//1030008920
		stringOwner = stringOwner.replaceAll("\\b(TRUST)\\s+", "$1\n").replaceAll("\n\\s*(AND)\\b", " $1");
		
		
		String[] nameLines = stringOwner.split("\n");

		List<List> body = new ArrayList<List>();
		String[] names = {"", "", "", "", "", ""};
		String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
		StringBuffer nameOnServerBuff = new StringBuffer();
		for (int i=0; i < nameLines.length; i++){
			String ow = nameLines[i];
			//1030008920   ARLIN B CO AND DELPHINE COOPER  TRUSTEES
			ow = ow.replaceAll("(?is)([A-Z\\s]+)(CO)\\s+(AND\\s+[A-Z]+)\\s+(\\2[A-Z]+)", "$4 $1 $3 ");
			
			names = StringFormats.parseNameNashville(ow, true);
			if (ow.trim().startsWith("AKA") || ow.trim().startsWith("C/O") || ow.trim().startsWith("ATTN")){
				ow = ow.replaceAll("\\bAKA\\b", "").replaceAll("\\bC/O\\b", "").replaceAll("\\bATTN:?\\b", "");
				names = StringFormats.parseNameDesotoRO(ow, true);
			}
			if (StringUtils.isNotEmpty(names[5]) && 
					LastNameUtils.isNotLastName(names[5]) && 
					NameUtils.isNotCompany(names[5])){
				names[4] = names[3];
				names[3] = names[5];
				names[5] = names[2];
			}
			if (StringUtils.isNotEmpty(names[3]) && 
					LastNameUtils.isLastName(names[3]) && !FirstNameUtils.isFirstName(names[3]) && 
					StringUtils.isEmpty(names[4]) &&
					StringUtils.isNotEmpty(names[5]) &&	FirstNameUtils.isFirstName(names[5])){
				String aux = names[3];
				names[3] = names[5];
				names[5] = aux;
			}
			names[1] = names[1].replaceAll("\\s+DA?TE?D.*", "");
			names[4] = names[4].replaceAll("\\s+DA?TE?D.*", "");

			types = GenericFunctions.extractAllNamesType(names);
			otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractAllNamesSufixes(names);
			
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
					NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			nameOnServerBuff.append("/").append(ow);
		}
		String nameOnServer = nameOnServerBuff.toString();
		nameOnServer = nameOnServer.replaceFirst("/", "");
		resultMap.put("PropertyIdentificationSet.NameOnServer", nameOnServer);
		GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		
		String[] a = StringFormats.parseNameNashville(ownerName, true);
		resultMap.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
		resultMap.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
		resultMap.put("PropertyIdentificationSet.OwnerLastName", a[2]);
		resultMap.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
		resultMap.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
		resultMap.put("PropertyIdentificationSet.SpouseLastName", a[5]);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void parseLegalSummary(ResultMap resultMap, long searchId) throws Exception {
		String legal = (String) resultMap.get("PropertyIdentificationSet.PropertyDescription");
		
		if(StringUtils.isEmpty(legal)) {
			return;
		}
		
		legal = legal.replaceAll("(?s)\\bA\\s+(REPLAT\\s+OF)\\b", "$1");
		legal = legal.replaceAll("\\b(REAL|PERSONAL)\\s+PROPERTY\\b", "");
		legal = legal.replaceAll("(CONT)\\s", " $1");
		legal = legal.replaceAll("\"", "");
    	legal = legal.replaceAll("\\s+THR?U\\s+", "-");
    	legal = legal.replaceAll("\\b(\\d+|[A-Z])\\s*AND\\s*(\\d+|[A-Z])\\b", "$1&$2");
    	legal = legal.replaceAll("\\b([A-Z])\\s+", "$1,");
    	legal = legal.replaceAll("\\b(IS\\s+A)\\s*,\\s*", "$1 ");
    	legal = legal.replaceAll("\\bPT\\b", "");
    	legal = legal.replace("+", ",");

    	//legal = GenericFunctions.replaceNumbers(legal);
    	String[] exceptionTokens = {"I", "M", "C", "L", "D"};
  	   	legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers  	   	
    	legal = legal.replaceAll("\\s{2,}", " ").trim();
    	    	
    	List<String> line = new ArrayList<String>();
    	    	
    	// extract and replace cross refs from legal description
  	   	List<List> bodyCR = new ArrayList<List>();
  	   	Pattern p = Pattern.compile("(?is)\\bBOOK\\s*(\\d+)\\s*PA?GE?\\s*(\\d+)\\b");
  	   	Matcher ma = p.matcher(legal);
	  	while (ma.find()){
	  		line = new ArrayList<String>();
	  		line.add(ma.group(1).trim().replaceAll("\\A0+", ""));
	  		line.add(ma.group(2).trim().replaceAll("\\A0+", ""));
	  		legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", " ORBKPG ");		   
	  	} 
	  	p = Pattern.compile("(?is)\\bRECE?PT(?:(?:ION)?\\s+NO)?\\s*(\\d+)");
  	   	ma = p.matcher(legal);
	  	while (ma.find()){
	  		line = new ArrayList<String>();
	  		line.add(ma.group(1).trim().replaceAll("\\A0+", ""));
	  		line.add("");
	  		line.add("");
	  		legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", " ORBKPG ");		   
	  	} 
  	   	if (!bodyCR.isEmpty()){		  		   		   
		   String [] header = {"InstrumentNumber", "Book", "Page"};		   
		   Map<String,String[]> map = new HashMap<String,String[]>();
		   map.put("InstrumentNumber", new String[]{"InstrumentNumber", ""});
		   map.put("Book", new String[]{"Book", ""});
		   map.put("Page", new String[]{"Page", ""});		   
		   ResultTable cr = new ResultTable();	
		   cr.setHead(header);
		   cr.setBody(bodyCR);
		   cr.setMap(map);		   
		   resultMap.put("CrossRefSet", cr);
		   legal = legal.replaceAll("\\s{2,}", " ");
  	   	}
  	   	
  	  List<List> bodyPIS = new ArrayList<List>();
  	  String strpatt = "\\b(\\d+[NSEW]?)\\s+(\\d+[NSEW]?)\\s+(\\d+[NSEW]?)\\s+\\d+\\.\\d+\\s*AC?(?:(?:RE|ER)S)?";
	  p = Pattern.compile(strpatt);
	  ma.reset();
	  ma = p.matcher(legal);
	  while (ma.find()) {
		  line = new ArrayList<String>();
		  line.add(ma.group(1).replaceAll("\\A0+", ""));
		  line.add(ma.group(2).replaceAll("\\A0+", ""));
		  line.add(ma.group(3).replaceAll("\\A0+", ""));
		  line.add("");
		  line.add("");
		  	bodyPIS.add(line);
	    }
	    legal = legal.replaceAll(strpatt, " STR ");
  	   	
  	   	// extract and replace lot from legal description
  	   	String lot = ""; // can have multiple occurrences
  	   	p = Pattern.compile("(?is)\\b(LOTS?)\\s*([A-Z,&]+|[\\d\\s&-]+(?:\\s*[A-Z])?|[A-Z]?\\d+(?:\\s*[A-Z]{1,2})?)\\b");
  	   	ma = p.matcher(legal);
  	   	while (ma.find()){
  	   		lot = lot + " " + ma.group(2).replaceAll("\\s*[,&]\\s*", " ").replaceAll("\\bLINE\\b", "").replaceAll("\\bLOCATED\\b", "")
  	   				.replaceAll("(?is)\\b(NORTH|SOUTH)?(EAST|WEST)?\\b", "").replaceAll("(?is)\\b[A-Z]{3,}\\b", "");
  	   		legal = legal.replace(ma.group(0), " LOT ");
  	   	}
  	   	lot = lot.trim();
  	   	if (lot.length() != 0){
  	   		lot = LegalDescription.cleanValues(lot, false, true);
  	   		resultMap.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
  	  	   	legal = legal.trim().replaceAll("\\s{2,}", " ");
  	   	}  	
  	   	
  	   	// extract and replace block from legal description - extract from original legal description
  	   	String block = "";
  	   	p = Pattern.compile("(?is)\\b(BLO?C?KS?)\\s*([\\d\\s]+|[A-Z]|\\d+[A-Z]?)\\b");
  	   	ma = p.matcher(legal);
  	   	while (ma.find()){
  	   		block = block + " " + ma.group(2).replaceAll("\\s*[&,]\\s*", " ");
  	   		legal = legal.replace(ma.group(0), " BLK ");
  	   	}   	   	
  	   	block = block.trim();
  	   	if (block.length() != 0){
  	   		block = LegalDescription.cleanValues(block, false, true);
  	   		resultMap.put("PropertyIdentificationSet.SubdivisionBlock", block);
  	   		legal = legal.trim().replaceAll("\\s{2,}", " ");
  	   	}
  	   	
  	   	// extract and replace unit from legal description
  	   	String unit = "";
  	   	p = Pattern.compile("(?is)\\b(UNIT)\\s*([A-Z]|((?:[A-Z]{1,2})?\\d+(?:[A-Z]{1,2})?))\\b");
  	   	ma = p.matcher(legal);
  	   	while (ma.find()){
  	   		unit = unit + " " + ma.group(2);
  	   		legal = legal.replace(ma.group(0), " UNIT ");; 		   
  	   	}
  	   	unit = unit.trim();
  	   	if (unit.length() != 0){
  	   		unit = LegalDescription.cleanValues(unit, false, true);
  	   		resultMap.put("PropertyIdentificationSet.SubdivisionUnit", unit);
  	   		legal = legal.trim().replaceAll("\\s{2,}", " ");
  	   	}
  	   	  	   
  	   	// extract and replace section, township and range from legal description
  	   	p = Pattern.compile("(?is)\\bS(?:EC)?T?\\s*(\\d+[A-Z]?)(?:\\s+IN)?\\s*T?(\\d+[A-Z]?)\\s*R?(\\d+[A-Z]?)\\b");
	    ma = p.matcher(legal);
	    if (ma.find()){
	    	line = new ArrayList<String>();
	    	line.add(ma.group(1).replaceAll("\\A0+", ""));
	    	line.add(ma.group(2).replaceAll("\\A0+", ""));
	    	line.add(ma.group(3).replaceAll("\\A0+", ""));
	    	line.add("");
	    	line.add("");
	    	bodyPIS.add(line);		   
	    	legal = legal.replace(ma.group(0), " SEC ");
	    } else {
	    	p = Pattern.compile("(?is)\\bT(\\d+[A-Z]?)\\s*R(\\d+[A-Z]?)\\b");
	    	ma.reset();
	    	ma = p.matcher(legal);
	    	if (ma.find()){
	    		String twp = ma.group(1).replaceAll("\\A0+", ""); 
	    		String rng = ma.group(2).replaceAll("\\A0+", "");
	    		p = Pattern.compile("(?is)\\bSEC\\s*(\\d+)\\b");
		    	ma.reset();
		    	ma = p.matcher(legal);
		    	while (ma.find()){
			    	line = new ArrayList<String>();
			    	line.add(ma.group(1).replaceAll("\\A0+", ""));
			    	line.add(twp);
			    	line.add(rng);
			    	line.add("");
			    	line.add("");
			    	bodyPIS.add(line);		   
		    	}
		    } else {
		    	p = Pattern.compile("(?is)\\bIN\\s+(\\d+[A-Z]?)\\s*T?(\\d+[A-Z]?)\\s*R?(\\d+[A-Z]?)\\b");
		    	ma.reset();
		    	ma = p.matcher(legal);
		    	if (ma.find()){
		    		line = new ArrayList<String>();
			    	line.add(ma.group(1).replaceAll("\\A0+", ""));
			    	line.add(ma.group(2).replaceAll("\\A0+", ""));
			    	line.add(ma.group(3).replaceAll("\\A0+", ""));
			    	line.add("");
			    	line.add("");
			    	bodyPIS.add(line);		   
			    } 
		    }
	    }
	    
	    p = Pattern.compile("(?is)\\bSECTIONS\\s+(\\d+[A-Z]?(?:\\s*(?:AND|,|&)\\s*\\d+[A-Z]?)+)\\s*T(\\d+[A-Z]?)\\s*R(\\d+[A-Z]?)");
	    ma.reset();
	    ma = p.matcher(legal);
	    while (ma.find()) {
	    	String sec = ma.group(1).replaceAll("[&,]", " ").replaceAll("(?is)\\bAND\\b", " ");
	    	String[] split = sec.split("\\s+");
	    	for (String s:split) {
	    		line = new ArrayList<String>();
			  	line.add(s.replaceAll("\\A0+", ""));
			   	line.add(ma.group(2).replaceAll("\\A0+", ""));
			   	line.add(ma.group(3).replaceAll("\\A0+", ""));
			   	line.add("");
			   	line.add("");
			   	bodyPIS.add(line);
	    	}
	    }
	    
	    // extract and replace building # from legal description
 	    p = Pattern.compile("(?is)\\b(BLDG|BUILDING)\\s*([A-Z]|\\d+)\\b");
 	    ma = p.matcher(legal);
 	    if (ma.find()){
 	    	resultMap.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2));
 	    	legal = legal.replace(ma.group(0), " BLDG ");
 	    	legal = legal.trim().replaceAll("\\s{2,}", " ");
 	    }
 	    	    	   
 	    // extract and replace tract from legal description
 	    String tract = "";
 	    p = Pattern.compile("(?is)\\b(TR(?:ACT)?)\\s*(\\d+[A-Z]?)\\b");
 	    ma = p.matcher(legal);
 	    if (ma.find()){ 	
 	    	tract = ma.group(2);
 	    	resultMap.put("PropertyIdentificationSet.SubdivisionTract", tract); 		   
 	    	legal = legal.replace(ma.group(0), " TRACT ");
 	    	legal = legal.trim().replaceAll("\\s{2,}", " ");
 	    } 	    	   
 	   	   	 	 
 	    // extract and replace phase from legal description
 	    String phase = "";
 	    legal = legal.replaceAll("(?is)\\b(PHASE\\s+[A-Z])\\s*,", "$1 ");
 	    p = Pattern.compile("(?is)\\b(Phase)\\s*(\\d[A-Z]\\b|\\d+|[A-Z])");
 	    ma = p.matcher(legal);
 	    if (ma.find()){
 	    	phase = ma.group(2);
 	    	resultMap.put("PropertyIdentificationSet.SubdivisionPhase", phase);
 	    	legal = legal.replace(ma.group(0), " PHASE ");
 	    	legal = legal.trim().replaceAll("\\s{2,}", " ");
 	    }
 	    
 	    String platBookPagePatt = "(?is)\\bBK\\s+(\\d+)\\s+PG\\s+(\\d+)";
 	    List<String> book = RegExUtils.getMatches(platBookPagePatt, legal, 1);
		List<String> page = RegExUtils.getMatches(platBookPagePatt, legal, 2);
		StringBuilder sb = new StringBuilder();
		String platBookPage = "";
		for (int i=0; i<book.size(); i++) {
			sb.append(" ").append(book.get(i)).append("&").append(page.get(i));
		} 
		platBookPage = sb.toString().trim();
		if (platBookPage.length() != 0)
		{
			platBookPage = LegalDescription.cleanValues(platBookPage, false, true);
			String[] values = platBookPage.split("\\s");
			
			for (int i=0; i<values.length; i++)			
			{
				String[] bookAndPage = values[i].split("&");
				line = new ArrayList<String>();
				String bk = bookAndPage[0];
				String pg = bookAndPage[1];
				boolean found = false;
				for (int j=0;j<bodyPIS.size();j++) {
					List l = bodyPIS.get(j);
					if (l.size()==5 && "".equals(l.get(3)) && "".equals(l.get(4))) {
						l.set(3, bk);
						l.set(4, pg);
						found = true;
						break;
					}
				}
				if (!found) {
					line = new ArrayList<String>();
			    	line.add("");
			    	line.add("");
			    	line.add("");
			    	line.add(bk);
			    	line.add(pg);
			    	bodyPIS.add(line); 
				}
			}
		}
		
		if (!bodyPIS.isEmpty()){
			   String [] header = {PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(),
					   PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(),
					   PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(),
					   PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(),
					   PropertyIdentificationSetKey.PLAT_NO.getShortKeyName()};
			   
			   Map<String,String[]> map = new HashMap<String,String[]>();
			   map.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), 
					   new String[]{PropertyIdentificationSetKey.SUBDIVISION_SECTION.getShortKeyName(), ""});
			   map.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), 
					   new String[]{PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getShortKeyName(), ""});
			   map.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), 
					   new String[]{PropertyIdentificationSetKey.SUBDIVISION_RANGE.getShortKeyName(), ""});
			   map.put(PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), 
					   new String[]{PropertyIdentificationSetKey.PLAT_BOOK.getShortKeyName(), ""});
			   map.put(PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), 
					   new String[]{PropertyIdentificationSetKey.PLAT_NO.getShortKeyName(), ""});
			   
			   ResultTable pis = new ResultTable();	
			   pis.setHead(header);
			   pis.setBody(bodyPIS);	
			   pis.setMap(map);		   
			   resultMap.put("PropertyIdentificationSet", pis);
			   legal = legal.replaceAll("\\s{2,}", " ").trim();
		    }
         	           	   	
 	   // extract subdivision name - only if lot or block or unit was extracted 	   
 	   if (lot.length() != 0 || block.length() != 0 || unit.length() != 0 || legal.matches(".*\\b(SUB-?DI?V?|ADD'?N?)\\b.*")){ 		   
	 	   // first perform additional cleaning 
 		   legal = legal.replaceAll("\\s+[A-Z]\\.{3}\\s*$", "");
 		   legal = legal.replaceAll("^(\\s*(ORBKPG|DB)\\b)+", "");
 	 	   legal = legal.replaceAll("(?<=(BLK|LOT|UNIT))\\s*,", " ");
 	 	   legal = legal.replaceAll("\\bLESS\\b", "");
 	 	   legal = legal.replaceFirst("\\s*\\b\\d+\\s*$", "");
 	 	   legal = legal.replaceFirst("\\bORBKPG\\b(\\sORBKPG\\b)+", "ORBKPG");
 	 	   legal = legal.replaceAll("(?<=[A-Z\\s])\\. ", " ");
	 	   legal = legal.replaceAll("\\s{2,}", " ").trim();
	 	   
	 	   String subdiv = "";	
	 	   String patt = "(?:\\b(?:LOTS?|BLK|THE(?: UNRECORDED)? SUBDI?V OF|UNRECORDED SUBDI?V)(?: PHASE| TRACT)*\\b\\s*(?:&\\s*|IN )?)++(.*?)\\s*(?:\\b(?:SUB(?:-?DI?V?)?|(?:\\d+(?:ST|ND|RD|TH) )?ADD'?(?:ITIO)?N?|UNIT|ORBKPG|DB|RUN(?:NING)?|SEC|UNRECORDED|PHASE|BEING)\\b|\\.{2,}|,|&(?= LOT\\b)).*";
	 	   p = Pattern.compile(".*"+patt);
	 	   ma = p.matcher(legal);
	 	   if (ma.find()){
	 		   subdiv = ma.group(1);		   
	 	   } 
	 	   if (subdiv.length() == 0){
	 		   p = Pattern.compile(".*(?:\\b(?:LOTS?|BLK|ORBKPG)\\s*(?:&\\s*|IN )?)++\\b(\\w+)$");
		 	   ma = p.matcher(legal);
		 	   if (ma.find()){
		 		   subdiv = ma.group(1);		   
		 	   }
		 	   if (subdiv.length() == 0){
		 		   p = Pattern.compile(".*\\bUNIT (.*?)\\s*(?:\\b(?:SUB(?:-?DI?V?)?|(?:CONDO)|(?:\\d+(?:ST|ND|RD|TH) )?ADD'?(?:ITIO)?N?|ORBKPG|DB|RUN(?:NING)?|SEC|UNRECORDED|PHASE|REPLAT)\\b|\\.{2,}|,|&(?= LOT\\b)).*");
			 	   ma = p.matcher(legal);
			 	   if (ma.find()){
			 		   subdiv = ma.group(1);		   
			 	   }
			 	   if (subdiv.length() == 0){
			 		   p = Pattern.compile(".*?\\b(?:(?:LOTS?|BLK|UNIT) )++(.*? (?:ESTATES?|LAKE|PARK|MANOR|FILING))\\b.*");  
			 		   ma.usePattern(p);
			 		   ma.reset();
			 		   if (ma.find()){
			 			   subdiv = ma.group(1);
			 		   } 
			 		   if (subdiv.length() == 0){
				 		   p = Pattern.compile("(?:.*\\b(?:ORBKPG|DB) )?(.+?)\\s*\\b(?:LOTS?|SUB(?:-?DI?V?)?|(?:\\d+(?:ST|ND|RD|TH) )?ADD'?(?:ITIO)?N?)\\b");  
				 		   ma.usePattern(p);
				 		   ma.reset();
				 		   if (ma.find()){
				 			   subdiv = ma.group(1);
				 		   } 
			 		   }
			 	   }
		 	   }
	 	   }
	 	   if (subdiv.length() == 0){
	 		   p = Pattern.compile(patt);
		 	   ma = p.matcher(legal);
		 	   if (ma.find()){
		 		   subdiv = ma.group(1);		   
		 	   }
	 	   }	 	   
	 	   subdiv = subdiv.trim();
	 	   if (subdiv.length() != 0){
	 		  subdiv = subdiv.replaceAll("\\bFILING\\s+\\d+\\b", "");
	 		  subdiv = subdiv.replaceAll("^\\s*TOWN\\s+OF\\b", "");
	 		  subdiv = subdiv.replaceAll("\\bLOCATED\\s+IN\\b", "");
              subdiv = subdiv.replaceAll("\\bUNREC(\\s*-\\s*)?ORDED PLAT( OF)?\\b", "");
              subdiv = subdiv.replaceAll("\\bPLAT UNRECORDED\\b", "");
              subdiv = subdiv.replaceAll("\\bREPLAT OF\\b", "");
	 		  subdiv = subdiv.replaceFirst("^\\s*(IN|OF|TO)\\b\\s*", "");
	 		  subdiv = subdiv.replaceFirst("\\s*\\b(BEING )?IN\\b.*", "");
	 		  subdiv = subdiv.replaceAll("\\s*-\\s*", "-");
	 		  subdiv = subdiv.replaceFirst("[,&;\\s-]+$", "");
	 		  subdiv = subdiv.replaceFirst("\\s*#?\\b\\d+\\s*$", "");
	 		  subdiv = subdiv.replaceFirst("\\s*\\b(BEG(IN(NING)?)?|COMM)( AT)?( [SWEN]{1,2}C?)?\\b.*", "");
	 		  subdiv = subdiv.replaceFirst("\\s*\\b(INCLUDING|FRONTING|LYING|ACC(ORDING\\s+TO)?)\\b.*", "");
	 		  subdiv = subdiv.replaceFirst("\\s*\\bSTRIP TO CITY\\b", "");
	 		  subdiv = subdiv.replaceFirst("^\\s*(\\b(LOT|BLK|ALSO:?|UNIT|TRACT)\\s*)+", "");
	 		  subdiv = subdiv.replaceFirst("\\d+\\.\\d+\\s+AC?\\b", "");	//acres
	 		  subdiv = subdiv.replaceAll("\\.", "");
	 		  subdiv = subdiv.replaceFirst("^[\\s-]+", "");
	 		  subdiv = subdiv.replaceFirst("(\\b(LOT|BLK|PHASE|ALSO:?|UNIT|TRACT)\\s*)+$", "");
	 		  subdiv = subdiv.replaceFirst("THE FOLLOWING DESCRIBED PROPERTY:?", "");
	 		  subdiv = subdiv.replaceFirst("^\\s*THAT\\b\\s*", "");
	 		  subdiv = subdiv.replaceFirst("(.+?)\\b[A-Z]+\\s+VILLAGE\\b", "$1");
	 		  subdiv = subdiv.replaceFirst("\\bAND\\s+LCE\\s+PARKING\\b", "");
	 		  subdiv = subdiv.replaceFirst("\\b[NSEW]{1,2}\\d+[NSEW]{1,2}\\d\\b.*", "");
	 		   subdiv = subdiv.replaceAll("\\s{2,}", " ").trim();
	 		   if (subdiv.length() != 0){ 
	 			   resultMap.put("PropertyIdentificationSet.SubdivisionName", subdiv);
	 			   if (legal.contains("CONDO")){
	 				  resultMap.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
	 			   }
	 		   }
	 	   }
 	   }
	}
	
	public static void parseAddress(ResultMap resultMap, long searchId) {

		String address = (String) resultMap.get("PropertyIdentificationSet.AddressOnServer");
			
		if (StringUtils.isEmpty(address))
			return;
		
		Matcher ma = Pattern.compile("(.+?)\\s+(\\d+)").matcher(address);
		if (ma.matches()) {
			address = ma.group(1);
			String zip = ma.group(2);
			if (zip.matches("\\d{5}0{4}")) {
				zip = zip.replaceFirst("0{4}$","");
			}
			if (zip.length()==5) {
				resultMap.put(PropertyIdentificationSetKey.ZIP.getKeyName(), zip);
			}
		}
			
		String city = "";
		String[] addressToks = address.split("\\s+");
		for (int i = 0; i < CITIES.length; i++){
			if (CITIES[i].toLowerCase().contains(addressToks[addressToks.length - 1].toLowerCase())){
				city = CITIES[i];
				if (city.matches("MTN VIL?")){
					address = address.replaceAll("\\bMTN\\s+V.*", "");
					city = "MOUNTAIN VILLAGE";
				} else {
					address = address.replaceAll(city.toUpperCase() + ".*", "");
				}
				resultMap.put("PropertyIdentificationSet.City", city);
				break;
			}
		}
		Matcher mat = addressPattern.matcher(address);
		if (mat.find()){
			if (mat.group(3) != null){
				address = mat.group(2) + " " + mat.group(1) + " " + mat.group(3);
			} else {
				address = mat.group(2) + " " + mat.group(1);
			}
		}
		address = address.replaceAll("(?is)\\A\\s*([NSWE]{1,2})\\s+(\\d+)", "$2 $1 ");
		address = address.replaceAll("(?is)\\s+", " ");
		address = address.replaceAll("(?is)\\A\\s*(\\d+)\\s+(\\d+)\\s+(HWY|HIGHWAY)\\s*$", "$1 $3 $2");
		resultMap.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address.trim()));
		resultMap.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address.trim()));
		
	}

}