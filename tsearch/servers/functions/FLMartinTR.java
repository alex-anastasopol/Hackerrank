package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.token.AddressAbrev;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLMartinTR {
	 
	@SuppressWarnings("rawtypes")
	public static void ownerNameTokenizerIntermFLMartinTR (ResultMap m, String owner) throws Exception {
	    	owner = owner.replaceAll("\\s{2,}", " ");
	    	owner = owner.toUpperCase();
			owner = GenericFunctions2.cleanOwnerNameFLMartinTR(owner);
			owner = owner.replaceAll("\\s*#\\d+\\s*$", "");
			
			String names[] = {"", "", "", "", "", ""};
			//apply LFM name tokenizer
			if (NameUtils.isCompany(owner)){
				m.put("PropertyIdentificationSet.OwnerLastName", owner);
				names[2] = owner;
			} else {
				names = StringFormats.parseNameNashville(owner, true);
				m.put("PropertyIdentificationSet.OwnerFirstName", names[0]);
				m.put("PropertyIdentificationSet.OwnerMiddleName", names[1]);
				m.put("PropertyIdentificationSet.OwnerLastName", names[2]);
				m.put("PropertyIdentificationSet.SpouseFirstName", names[3]);
				m.put("PropertyIdentificationSet.SpouseMiddleName", names[4]);
				m.put("PropertyIdentificationSet.SpouseLastName", names[5]);
			}
			
			 List<List> body = new ArrayList<List>();
			 String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
			 types = GenericFunctions.extractAllNamesType(names);
			 otherTypes = GenericFunctions.extractAllNamesOtherType(names);
			 suffixes = GenericFunctions.extractAllNamesSufixes(names);
			   
			 GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
					 NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			
			 GenericFunctions.storeOwnerInPartyNames(m, body, true);
	    }
	    
	@SuppressWarnings("rawtypes")
	public static void ownerNameTokenizerFinalFLMartinTR (ResultMap m, String owner) throws Exception {
    	
    	owner = owner.replaceAll("\\s{2,}", " ");
    	owner = owner.toUpperCase();
    	String[] lines = owner.split("@@");
    	
    	List<List> body = new ArrayList<List>();
    	String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
    	
		for (int i=0; i<lines.length; i++){
			if (!lines[i].matches(".+\\(((\\d+|[A-Z])-)?\\d+\\)$")){
				lines[i] = GenericFunctions2.cleanOwnerNameFLMartinTR(lines[i]);
			}
		}
		lines[0] = lines[0].replaceAll("\\s*#\\d+\\s*$", "");
		
		//apply LFM name tokenizer on the first line
		String names[] = {"", "", "", "", "", ""};	
		if (NameUtils.isCompany(lines[0])){
			names[2] = lines[0];
		} else {
			names = StringFormats.parseNameNashville(lines[0], true);
		}
		// if co-owner was not extracted from the first line, try to extract it from the second line, if it's not an address 
		if (names[5].length() == 0 && lines.length >= 4){
			// first identify which is the first address line
			int addrIdx = -1;
			boolean foundState = false;
			Pattern p;
			Matcher ma;
			for (int i = lines.length-1; i>0; i--){
				if (!foundState){
					p = Pattern.compile("\\b[A-Z]{2} [\\d-]+?$");
					ma = p.matcher(lines[i]);
					if (ma.find()){
						foundState = true;
						continue;
					}
				}
				p = Pattern.compile("(^(S(UI)?TE|UNIT|APT|LOT|CT)|\\b(GARDENS|MBL))\\b");
				ma = p.matcher(lines[i]);
				if (ma.find()){
					addrIdx = i;
					continue;
				}
				p = Pattern.compile("\\(((\\d+|[A-Z])-)?\\d+\\)$");
				ma = p.matcher(lines[i]);
				if (ma.find()){
					addrIdx = i;
					continue;
				}
				if (AddressAbrev.containsAbbreviation(lines[i]) || lines[i].matches(".*\\bP\\.?O\\b\\.?.*")){
					addrIdx = i;
					break;
				}
				if (addrIdx != -1){
					if (lines[i].matches("\\d+\\b.*")){
						addrIdx = i;
					}
					break;
				}
			}
			
			if (addrIdx > 0 && (lines[addrIdx-1].matches(".*\\(((\\d+|[A-Z])-)?\\d+\\)$") || lines[addrIdx-1].matches(".*\\bP\\.?O\\b\\.?.*"))){
				addrIdx --;
			}
			if (addrIdx > 1 || addrIdx == -1){
				lines[1] = lines[1].replaceAll("\\s*#\\d+\\s*$", "");
				p = Pattern.compile("(?:C/O )+(.+)");
				ma = p.matcher(lines[1]);
				boolean isFML = false;
				String coowner[] = {"", "", "", "", "", ""}; 
				if (ma.matches()){
					lines[1] = ma.group(1);				
					isFML = true;
				} 
				if (NameUtils.isCompany(lines[1])){
					coowner[2] = lines[1];
				} else { 
					if (isFML){
						coowner = StringFormats.parseNameDesotoRO(lines[1], true);
					} else {
						coowner = StringFormats.parseNameNashville(lines[1], true);
					}
				}
				names[3] = coowner[0];
				names[4] = coowner[1];
				names[5] = coowner[2];
			}
		}

		types = GenericFunctions.extractAllNamesType(names);
		otherTypes = GenericFunctions.extractAllNamesOtherType(names);
		suffixes = GenericFunctions.extractAllNamesSufixes(names);


		GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, 
				NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);

		GenericFunctions.storeOwnerInPartyNames(m, body, true);
    }
	
	 @SuppressWarnings({ "rawtypes", "unchecked" })
	public static void legalTokenizerFLMartinTR(ResultMap m, String legal) throws Exception {

	    	//initial corrections and cleanup of legal description
	    	legal = legal.replaceAll("(?<=[A-Z]\\s?)\\.", "");
	    	legal = legal.replaceAll("#(?=\\s?\\d)", "");
	    	legal = legal.replaceAll("\\bBNO(?= \\d)", "");
	    	legal = legal.replaceAll("=", "");
	    	legal = legal.replaceFirst("([^\"]*)\"", "$1 \"");
	    	legal = legal.replaceAll("BUILDNG", "BUILDING");   
	    	legal = legal.replaceAll("\\bCONDO(?!MINIUM|\\s)", "CONDO ");
	    	legal = legal.replaceAll("\\bPG(?= \\d+ PG \\d+\\b)", "PB");
	    	legal = legal.replaceAll("([A-Z]{3,})(\\d)", "$1 $2");
	    	legal = legal.replaceAll("(?<!^\\w*)([A-Z]|\\d)(PUD|BLDG|UNITS?|LOTS?|BLKS?|PH(?:ASES?)?|TR(?:ACTS?)?|SEC(?:TION)?|AMD|LYING|OF)\\b", "$1 $2");
	    	legal = legal.replaceAll("([A-Z])(OR|PB|DB)(?= \\d)", "$1 $2");
	    	legal = legal.replaceAll("(\\d)([A-Z]{3,})", "$1 $2");
	    	legal = legal.replaceAll("(\\d)(OR|AS|PB|PG|PI)", "$1 $2");
	    	legal = legal.replaceAll("''", "'");
	    	legal = legal.replaceAll("'([A-Z])'", " $1 ");
	    	legal = legal.replaceAll("\\bGOVT?\\b", "");
	    	legal = legal.replaceAll("(\\d)(?: TO | THRU )(\\d)", "$1-$2");
	    	legal = legal.replaceAll("\\bNO(?= \\d)", "");
	    	legal = legal.replaceAll("\\b([SWEN]{1,2}|NORTH|SOUTH|WEST|EAST)(LY)?\\s*\\d+/\\d+( FT)?( OF)?\\b", "");
	    	legal = legal.replaceAll("(?<!')\\b([SWEN]{1,2}|NORTH|SOUTH|WEST|EAST)(LY)?\\s*\\d+(.\\d+)?'?( FT)?( OF)?\\b", "");
	    	legal = legal.replaceAll("\\b(\\d+-?[A-Z]?|[A-Z]) ([SWEN]{1,2}|NORTH|SOUTH|WEST|EAST)(LY)? OF\\b", "$1");
	    	legal = legal.replaceAll("\\b\\d+(.\\d+)?' ([SWEN]{1,2}|NORTH|SOUTH|WEST|EAST)(LY)?( OF)?\\b", "");
	    	legal = legal.replaceAll("\\b(& )?(A )?(UND )?\\d+/\\d+(ST|ND|RD|TH)? (UND )?(INT(EREST)?|SHARE)( IN)?\\b", "");
	    	legal = legal.replaceAll("\\b(REVISED )?PARCEL\\s+\\d+(\\s*[A-Z])?( AT)?\\b", "");
	    	legal = legal.replaceAll("\\b(THAT )?PART OF( THE)?\\b", "");
	    	legal = legal.replaceAll("\\bREPLAT\\b", "");
	    	legal = legal.replaceAll("\\bAMENDED\\b", "");
	    	legal = legal.replaceAll("\\bPAGE UNREC\\b", "");
	    	legal = legal.replaceAll("\\bALL (OF|IN)\\b", "");
	    	legal = legal.replaceAll("CLUSTER ([A-Z]|\\d+)\\b", "");
	    	legal = legal.replaceAll("\\d+' X \\d+'", "");
	    	legal = legal.replaceAll("\\d\\.\\d+ AM\\b", "");
	    	legal = legal.replaceAll("\\s{2,}", " ").trim();
	    	
	    	String legalOrig = legal;    	
	    	legal = GenericFunctions.replaceNumbers(legal);	   
	    	String[] exceptionTokens = {"I", "M", "C", "L", "D"};
	    	legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers to arabics
	    	legal = legal.replaceAll("\\s{2,}", " ").trim();
		   	   	
	    	// extract and remove cross refs from legal description  
	    	List<List> bodyCR = new ArrayList<List>();
	 	   	Pattern p = Pattern.compile("\\b(OR|DB)(?: BK)?\\s*(\\d+),? (?:PG|PAGE) (\\d+(?:[\\s&]+\\d+)*)\\b");
	 	   	Matcher ma = p.matcher(legal);	      	   
	 	   	while (ma.find()){
		 	   	List<String> line = new ArrayList<String>();
				line.add(ma.group(2).replaceFirst("^0+(.+)", "$1"));
				line.add(ma.group(3).replaceFirst("^0+(.+)", "$1").replaceAll("\\s*&\\s*", " "));
				line.add(ma.group(1));
				bodyCR.add(line);
	 	   		legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", ""); 	   		
	 	   	}
	 	   	String patt = "(\\d+)/(\\d+(?:[\\s&]+\\d+\\b(?!/))*)";
	 	   	Pattern p2 = Pattern.compile(patt); 
	 	   	p = Pattern.compile("\\b(OR|DB)(?: BK)?\\s*("+patt+"\\b(?:[\\s&]+"+patt+")*)");
		   	ma = p.matcher(legal);	      	   
		   	while (ma.find()){
		   		Matcher ma2 = p2.matcher(ma.group(2));
		   		while(ma2.find()){
			 	   	List<String> line = new ArrayList<String>();
					line.add(ma2.group(1).replaceFirst("^0+(.+)", "$1"));
					line.add(ma2.group(2).replaceFirst("^0+(.+)", "$1").replaceAll("\\s*&\\s*", " "));
					line.add(ma.group(1));
					bodyCR.add(line);
		   		}
		   		legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", ""); 	   		
		   	}
		   	p = Pattern.compile("(?<=\\b(?:PUD|AS IN) )(\\d+)/(\\d+)");
		   	ma = p.matcher(legal);	      	   
		   	while (ma.find()){
		 	   	List<String> line = new ArrayList<String>();
				line.add(ma.group(1).replaceFirst("^0+(.+)", "$1"));
				line.add(ma.group(2).replaceFirst("^0+(.+)", "$1").replaceAll("\\s*&\\s*", " "));
				line.add("");
				bodyCR.add(line);
		   		legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", ""); 	   		
		   	} 	   	
	 	   	if (!bodyCR.isEmpty()){	   		   
			   String [] header = {"Book", "Page", "Book_Page_Type"};		   
			   Map<String,String[]> map = new HashMap<String,String[]>();		   
			   map.put("Book", new String[]{"Book", ""});
			   map.put("Page", new String[]{"Page", ""});
			   map.put("Book_Page_Type", new String[]{"Book_Page_Type", ""});
			   
			   ResultTable cr = new ResultTable();	
			   cr.setHead(header);
			   cr.setBody(bodyCR);
			   cr.setMap(map);		   
			   m.put("CrossRefSet", cr);
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
	 	   	
	 	   //extract and remove plat book & page from legal description
	 	   List<List> bodyPIS = new ArrayList<List>();
	 	   p = Pattern.compile("\\bPB\\s*(\\d+) PG\\s*(\\d+)\\b");
	 	   ma = p.matcher(legal);
	 	   while (ma.find()){
	 		   List<String> line = new ArrayList<String>();
	 		   line.add(ma.group(1).replaceFirst("^0+(.+)", "$1"));
	 		   line.add(ma.group(2).replaceFirst("^0+(.+)", "$1"));
	 		   bodyPIS.add(line);
	 		   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "PB ");
	 	   } 
	 	   if (!bodyPIS.isEmpty()){	   		   
			   String [] header = {"PlatBook", "PlatNo"};		   
			   Map<String,String[]> map = new HashMap<String,String[]>();		   
			   map.put("PlatBook", new String[]{"PlatBook", ""});
			   map.put("PlatNo", new String[]{"PlatNo", ""});
			   
			   ResultTable pis = new ResultTable();	
			   pis.setHead(header);
			   pis.setBody(bodyPIS);
			   pis.setMap(map);		   
			   m.put("PropertyIdentificationSet", pis);
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
	 	   
		   // extract and replace sec-twn-rng from legal description
		   String sec = "", twn = "", rng = "";
		   String secFromIS = (String) m.get("PropertyIdentificationSet.SubdivisionSection");
		   String twnFromIS = (String) m.get("PropertyIdentificationSet.SubdivisionTownship");
		   String rngFromIS = (String) m.get("PropertyIdentificationSet.SubdivisionRange");
		   boolean strFromIS = !StringUtils.isEmpty(secFromIS) && !StringUtils.isEmpty(twnFromIS) && !StringUtils.isEmpty(rngFromIS);
		   List<List> bodySTR = new ArrayList<List>();
		   p = Pattern.compile("(?:^|\\bSEC(?:TION)?'?S? )(\\d+)[-\\s]+(\\d+)[-\\s]+(\\d+)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   sec = ma.group(1).replaceFirst("^0+(.+)", "$1");
			   twn = ma.group(2).replaceFirst("^0+(.+)", "$1");
			   rng = ma.group(3).replaceFirst("^0+(.+)", "$1");
			   if (!strFromIS || !sec.equals(secFromIS) || !twn.equals(twnFromIS) || !rng.equals(rngFromIS)){			   
				   List<String> line = new ArrayList<String>();
		 		   line.add(sec);
		 		   line.add(twn);
		 		   line.add(rng);
		 		   bodySTR.add(line);
			   }
	 		   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "SEC ");		   		   
		   }
		   p = Pattern.compile("(?<!-)\\b(\\d+)-(\\d+)-(\\d+)\\b(?!-)");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   sec = ma.group(1).replaceFirst("^0+(.+)", "$1");
			   twn = ma.group(2).replaceFirst("^0+(.+)", "$1");
			   rng = ma.group(3).replaceFirst("^0+(.+)", "$1");
			   if (!strFromIS || !sec.equals(secFromIS) || !twn.equals(twnFromIS) || !rng.equals(rngFromIS)){
		 		   List<String> line = new ArrayList<String>();
		 		   line.add(sec);
		 		   line.add(twn);
		 		   line.add(rng);
		 		   bodySTR.add(line);
			   }
	 		   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "SEC ");		   		   
		   }
		   p = Pattern.compile("\\bSEC(?:TION)?'?S? (\\d+/\\d+) (\\d+)/(\\d+)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   sec = ma.group(1).replaceFirst("^0+(.+)", "$1").replaceFirst("/", " ");
			   twn = ma.group(2).replaceFirst("^0+(.+)", "$1");
			   rng = ma.group(3).replaceFirst("^0+(.+)", "$1");
			   if (!strFromIS || !sec.equals(secFromIS) || !twn.equals(twnFromIS) || !rng.equals(rngFromIS)){
		 		   List<String> line = new ArrayList<String>();
		 		   line.add(sec);
		 		   line.add(twn);
		 		   line.add(rng);
		 		   bodySTR.add(line);
			   }
	 		   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "SEC ");		   		   
		   }
		   p = Pattern.compile("\\bSEC(?:TION)?'?S? (\\d+(?:[-\\s]*[A-Z])?(?:[\\s&]+\\d+)*|I)(?:(?:,? |-)T\\s*(\\d+\\s?[SWEN])(?:,? |-)?R\\s*(\\d+\\s?[SWEN]))?\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
	 		   List<String> line = new ArrayList<String>();
	 		   sec = ma.group(1).replaceFirst("^0+(.+)", "$1").replaceAll("\\s*&\\s*", " ").replaceAll("\\s(?=[A-Z])", "").replaceFirst("^I$", "1");
	 		   twn = ma.group(2);
	 		   twn = twn == null? "" : twn.replaceFirst("^0+(.+)", "$1").replaceFirst("\\s(?=[SWEN])", "");
	 		   rng = ma.group(3);
	 		   rng = rng == null? "" : rng.replaceFirst("^0+(.+)", "$1").replaceFirst("\\s(?=[SWEN])", "");
	 		   if (!strFromIS || !sec.equals(secFromIS) || (twn.length() !=0 && !twn.equals(twnFromIS)) || (rng.length() != 0 && !rng.equals(rngFromIS))){
		 		   line.add(sec);
		 		   line.add(twn);
		 		   line.add(rng);
		 		   bodySTR.add(line);
	 		   }
	 		   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "SEC ");		   		   
		   }
	 	   if (!bodySTR.isEmpty()){	 	
	 		   if (secFromIS != null && twnFromIS != null && rngFromIS != null){
		 		   List<String> line = new ArrayList<String>();
		 		   line.add(secFromIS);
				   line.add(twnFromIS);
				   line.add(rngFromIS);
				   bodySTR.add(line);
				   m.remove("PropertyIdentificationSet.SubdivisionSection");
				   m.remove("PropertyIdentificationSet.SubdivisionTownship");
				   m.remove("PropertyIdentificationSet.SubdivisionRange");
	 		   }
			   
			   String [] header = {"SubdivisionSection", "SubdivisionTownship", "SubdivisionRange"};		   
			   Map<String,String[]> map = new HashMap<String,String[]>();		   
			   map.put("SubdivisionSection", new String[]{"SubdivisionSection", ""});
			   map.put("SubdivisionTownship", new String[]{"SubdivisionTownship", ""});
			   map.put("SubdivisionRange", new String[]{"SubdivisionRange", ""});
			   
			   ResultTable pis = new ResultTable();	
			   pis.setHead(header);
			   pis.setBody(bodySTR);
			   ResultTable pisPlat = (ResultTable) m.get("PropertyIdentificationSet");
			   if (pisPlat != null){
				   pis = ResultTable.joinHorizontal(pis, pisPlat);
				   map.putAll(pisPlat.getMapRefference());
			   }
			   pis.setMap(map);		   
			   m.put("PropertyIdentificationSet", pis);
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }	
	 	   
	 	   //additional legal description cleaning before extracting the rest of the legal elements
	 	   legal = legal.replaceAll("\\b\\d+/\\d+(ST|ND|RD|TH)?( SHARE)?\\b", "");
	 	   legal = legal.replaceAll("\\b\\d+\\.\\d+\\b'?", "");
	 	   legal = legal.replaceAll("\\b\\d+'", "");
	 	   
		   // extract and replace lot from legal description	
	 	   patt = "\\d+(?:[-\\s]?[A-Z])?";
		   String lot = ""; // can have multiple occurrences	   
		   p = Pattern.compile("\\bLO?TS?\\s*("+patt+"(?:[\\s&,-]+"+patt+")*|\\b[A-Z](?:-\\d+)?(?:[\\s&,]+[A-Z])*)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   lot = lot + " " + ma.group(1).replaceAll("\\s*[&,]\\s*", " ").replaceAll("(?<![A-Z])\\s(?=[A-Z])", "");
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "LOT ");
		   }
		   lot = lot.trim().replaceAll("\\s{2,}", " ");
		   if (lot.length() != 0){
			   lot = LegalDescription.cleanValues(lot, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   	 
		   // extract and replace block from legal description
		   String blk = "";
		   p = Pattern.compile("\\bBL(?:OC)?KS?\\s*([A-Z]{1,2}(?:-?\\d+)?|\\d+(?:[-\\s]?[A-Z]?)?(?:[\\s&-]+\\d+)*)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   blk = blk + " " +  ma.group(1).replaceAll("\\s*[&,]\\s*", " ");		   
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "BLK ");		   
		   }
		   blk = blk.trim().replaceAll("\\s{2,}", " ");
		   if (blk.length() != 0){
			   blk = LegalDescription.cleanValues(blk, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionBlock", blk);
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   	   
		   // extract and replace unit from legal description
		   String unit = ""; 
		   p = Pattern.compile("\\b(?:UNIT|APT)\\s*(\\d+(?:[-\\s]*[A-Z])?(?:[\\s-]+\\d+)?|\\b[A-Z](?:[-\\s]?\\d+)?)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   unit = unit + " " + ma.group(1).replaceAll("\\s*-\\s*", "-");
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "UNIT ");
		   }
		   unit = unit.trim();
		   if (unit.length() != 0){
			   unit = LegalDescription.cleanValues(unit, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and replace building # from legal description
		   String bldg = "";
		   p = Pattern.compile("\\b(?:BLDG|BUILDI?NG)(?: PO )?\\s*\"?(\\d+|[A-Z]{1,2}(?:-?\\d+)?)\"?\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   bldg = ma.group(1);
			   m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "BLDG ");
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }	      
		   	   	   
		   // extract and replace phase from legal description
	 	   String phase = "";
		   p = Pattern.compile("\\bPH(?:ASES?)?\\s*\"?((?:\\d+|[IXV]+)(?:[-\\s]?[A-Z])?(?:[-&,\\s]+(?:\\d+|[IXV]+)[A-Z]?)*|\\b[A-Z](?:-?\\d+)?(?:[\\s&]+[A-Z])*)\"?\\b");
		   ma = p.matcher(legal);
		   Pattern p1 = Pattern.compile("([IXV]+)(-?[A-Z]+)");
		   Matcher ma1;
		   while (ma.find()){
			   String phaseTemp = ma.group(1).replaceAll("\\s*[&,]\\s*", " ");
			   String[] phases = phaseTemp.split(" ");
			   for (int i=0; i<phases.length; i++){
				   ma1 = p1.matcher(phases[i]);
				   if (ma1.find()){
					   phases[i] = Roman.normalizeRomanNumbersExceptTokens(ma1.group(1), exceptionTokens).replaceFirst("^I$", "1") + ma1.group(2);
				   }
				   phase = phase + " " + phases[i].replaceFirst("^I$", "1");
			   }		   
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "PHASE ");		   
		   }
		   phase = phase.trim();
		   if (phase.length() != 0){
			   phase = LegalDescription.cleanValues(phase, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and replace tract from legal description
		   String tract = "";
		   p = Pattern.compile("\\bTR(?:ACT)?S? (\\d+(?: & \\d+)*|[A-Z]\\d*(?:-\\d+)*(?:[\\s&]+[A-Z])*)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   tract = tract + " " + ma.group(1).replaceAll("\\s*&\\s*", " ");
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "TRACT ");		   
		   }	
		   tract = tract.trim();
		   if (tract.length() != 0){
			   tract = LegalDescription.cleanValues(tract, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   	   	 	   	   
		   // extract subdivision name	   
		   String subdiv = "";
		   if (lot.length() != 0 || blk.length() != 0 || tract.length() != 0 || unit.length() != 0 || phase.length() != 0 || !bodyPIS.isEmpty() ||
				   legal.matches(".*\\b(UNREC|CONDO?(?:MINIUM)?|BOATSLIP|PUD)\\b.*") || legalOrig.matches(".*\\s[A-Z]{2}(?<!(OR|PG))[-\\s]\\d+\\b(?!\\.).*")){
			   // cleanup the legal descr before extracting the subdivision name
			   legal = legal.replaceAll("\\bLANDSCAPE TRACTS?\\b", "");
			   legal = legal.replaceAll("\\bALSO BEING .* EASEMENT\\b", "");
			   legal = legal.replaceAll("\\bDEDICATED ALLEY\\b", "");
			   legal = legal.replaceAll("\\b(STORM)?WATER MANAGEMENT.* TRACTS?\\b", "");
			   legal = legal.replaceAll("\\b(PRIVATE )?STREETS? (-+ )?R/W'?(\\s?S)?", "");
			   legal = legal.replaceAll("\\bALG NEW\\b", "");
			   legal = legal.replaceAll("\\b[SWEN]/LN( OF)?\\b", "");
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
			   
			   subdiv = GenericFunctions2.extractSubdivFLMartinTR(legal);
			   if (subdiv.length() == 0){
				   p = Pattern.compile("(.*?)\\s*\\b[A-Z]{2}-\\d+\\b.*");		   
				   ma = p.matcher(legalOrig);
				   if (ma.find()){
					   subdiv = ma.group(1);
				   } else {
					   p = Pattern.compile("(.*?)\\s*\\b[A-Z]{2} \\d+\\s*$");		   
					   ma = p.matcher(legalOrig);
					   if (ma.find()){
						   subdiv = ma.group(1);
					   }
				   }
				   if (subdiv.length() != 0 && (subdiv.matches("^FROM\\b.+") || subdiv.matches(".*\\bCOR\\b.*"))){
					   subdiv = "";
				   }
			   }
			   
			   if (subdiv.length() == 0){
				   p = Pattern.compile("\\(([^\\)]+)\\)");	// for PID 2239380000000001000000
				   ma = p.matcher(legal);
				   if (ma.find()){
					   legal = ma.group(1);
					   if (!legal.contains("LESS PORTION") && legal.matches(".*\\b(LOT|TRACT|BLK)\\b.*")){
						   legal = legal.replaceAll("\\bBEG AT [SWEN]{1,2}\\b", "");
						   legal = legal.replaceAll("\\bCOR(R(ECTION)?)?\\b", "");
						   legal = legal.replaceAll("\\bLESS\\b:?", "");
						   legal = legal.replaceAll("\\s{2,}", " ").trim();
						   subdiv = GenericFunctions2.extractSubdivFLMartinTR(legal);
					   }
				   }
			   }

			   if (subdiv.length() != 0){	
				   subdiv = subdiv.replaceAll("\"", "");
				   subdiv = subdiv.replaceAll("\\b(A )?PUD\\b\\s*", "");
				   subdiv = subdiv.replaceAll("&?( [SWEN]{1,2}(LY)?)?( [\\d\\.]+'?)? TO POB\\b", "");
				   subdiv = subdiv.replaceFirst("\\s*\\bALL (THAT|OF)\\b.*", "");
				   subdiv = subdiv.replaceFirst("\\s*\\bTHE FOLLOWING\\b.*", "");
				   subdiv = subdiv.replaceFirst("\\s*\\bLYING\\b.*", "");
				   subdiv = subdiv.replaceFirst("\\s*\\b(THAT|[SWEN]{1,2}LY)?\\s*\\bPORTIONS?( OF)?\\b.*", "");
				   subdiv = subdiv.replaceFirst("\\s*\\bREVISED( PLAT)?.*", "");
				   subdiv = subdiv.replaceFirst("\\s*\\bB\\s*K PLAT\\b.*", "");
				   subdiv = subdiv.replaceFirst("\\s*\\bPER\\b.*", "");
				   subdiv = subdiv.replaceFirst("\\s*\\bAS IN DECL?\\b.*", "");
				   subdiv = subdiv.replaceFirst("\\s*\\bAMD TO\\b.*", "");
				   subdiv = subdiv.replaceFirst("\\s*\\bSR \\d+\\b.*", "");
				   subdiv = subdiv.replaceFirst("\\s*\\bPRIVATE STREET\\b.*", "");
				   subdiv = subdiv.replaceFirst("\\s*\\bM/L\\b.*", "");
				   subdiv = subdiv.replaceFirst("\\s*\\(.*", "");
				   subdiv = subdiv.replaceFirst("\\s*\\bAKA\\b.*", "");
				   subdiv = subdiv.replaceFirst("\\s*\\bBEING\\b.*", "");
				   subdiv = subdiv.replaceFirst("[,\\s&;'-]+$", "");
				   subdiv = subdiv.replaceFirst("\\bAREA \\d+\\s*$", "");
				   subdiv = subdiv.replaceFirst(".*\\bAREAS?\\s*-+\\s*", "");
				   subdiv = subdiv.replaceFirst("\\b[SWEN]{1,2} OF .*", "");
				   subdiv = subdiv.replaceFirst("^[\\.,\\s]+", "");
				   subdiv = subdiv.replaceFirst("[,\\s&;'-]+$", "");
				   subdiv = subdiv.replaceFirst("\\b\\d+ [A-Z]\\s*$", "");
				   subdiv = subdiv.replaceFirst("\\b(\\d+|I)(-\\d+)?\\s*$", "");
				   subdiv = subdiv.replaceFirst("\\s*\\b(\\d+(ST|ND|RD|TH) )?(ADD'?N?|MAP)\\b.*", "");
				   subdiv = subdiv.replaceFirst("\\s*\\bSUB-?\\s*$", "");
				   subdiv = subdiv.replaceFirst(",? THE\\s*$", "");
				   subdiv = subdiv.replaceFirst("[,\\s&-]+$", "");
				   subdiv = subdiv.replaceFirst("^[,\\s&-]+", "");
				   subdiv = subdiv.trim().replaceAll("\\s{2,}", " ");			   
			   }
		   }
		   if (subdiv.length() == 0){
			   p = Pattern.compile("[^\"]+\"([^\"]+)\".*");
			   ma = p.matcher(legalOrig);
			   if (ma.find()){
				   subdiv = ma.group(1);
				   subdiv = subdiv.replaceFirst("(.*?)\\bPUD\\b.*", "$1");
			   } else {
				   p = Pattern.compile("[^\\(]+\\(([^\\)]+)PUD\\b.*\\).*");
				   ma = p.matcher(legal);
				   if (ma.find()){
					   subdiv = ma.group(1);
				   }
			   }
		   }
		   if (subdiv.length() != 0){
			   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			   if (legal.matches(".*\\bCONDO?(?:MINIUM)?\\b.*"))
				   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
		   }
	    }
	 
	@SuppressWarnings("rawtypes")
	public static void taxFLMartinTR (ResultMap m, long searchId) throws Exception {
    	
    	String amtPaid = (String) m.get("TaxHistorySet.AmountPaid");
    	if (StringUtils.isEmpty(amtPaid)){
    		m.put("TaxHistorySet.AmountPaid", "0.00");
    	}
    	
    	String totalDue = (String) m.get("TaxHistorySet.TotalDue");
    	if (StringUtils.isEmpty(totalDue)){
    		m.put("TaxHistorySet.TotalDue", "0.00");
    	}
    	
    	String priorDelinq = (String) m.get("tmpPriorDelinquent"); 
    	priorDelinq = GenericFunctions.sum(priorDelinq, searchId);
    	if (priorDelinq.equals("0")){
    		priorDelinq = "0.00";
    	}
    	m.put("TaxHistorySet.PriorDelinquent", priorDelinq);	
    	
    	String baseAmt = (String) m.get("tmpBaseAmount"); 
    	baseAmt = GenericFunctions.sum(baseAmt, searchId);
    	if (baseAmt.equals("0")){
    		baseAmt = "0.00";
    	}
    	m.put("TaxHistorySet.BaseAmount", baseAmt);	
    	
    	// receipts
    	List<List> body = new ArrayList<List>();	 		   
    	String receipts = (String) m.get("tmpReceipts");
    	if (!StringUtils.isEmpty(receipts)){
    		Pattern p1 = Pattern.compile("(\\d+/\\d+/\\d+) (\\w+ \\d{4} [\\d\\.]+\\b)(?:[\\w\\s]*)([\\s\\d,\\.$-]*)");
    		Pattern p2 = Pattern.compile(".+\\s+.+\\s+(.+)");
    		Matcher ma;
    		List<String> line;
    		String[] recArray = receipts.split("@@");
    		for (int i=0; i<recArray.length; i++){
    			ma = p1.matcher(recArray[i]);
    			if (ma.matches()){
    				line = new ArrayList<String>();
    				line.add(ma.group(1));
    				line.add(ma.group(2));
    				String amts = ma.group(3);
    				String amt = "";
    				if (amts.length() != 0){
    					ma = p2.matcher(amts);
    					if (ma.matches()){
    						amt = ma.group(1).replaceAll("[$,]", "");
    					}
    				}
    				line.add(amt);
    				GenericFunctions.addIfNotAlreadyPresent(body, line);
    			}
    		}
	    	String [] header = {"ReceiptDate", "ReceiptNumber", "ReceiptAmount"};	 		   
	    	Map<String,String[]> map = new HashMap<String,String[]>();
 		   	map.put("ReceiptDate", new String[]{"ReceiptDate", ""});
 		   	map.put("ReceiptNumber", new String[]{"ReceiptNumber", ""});
 		   	map.put("ReceiptAmount", new String[]{"ReceiptAmount", ""});
 		   
 		   	ResultTable ths = new ResultTable();	
 		   	ths.setHead(header);
 		   	ths.setBody(body);
 		   	ths.setMap(map);
 		   	m.put("TaxHistorySet", ths);
    	}
	}
			  
}
