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
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLLakeTR {
	   @SuppressWarnings("rawtypes")
	public static void partyNamesFLLakeTR(ResultMap m,long searchId) throws Exception {
		   
		   String owner = (String) m.get("tmpOwner");
		  
		   if (StringUtils.isEmpty(owner))
			   return;

		   String ownerCopy = owner;
		   owner = owner.replaceAll("(?is)(.*)(?:\\s+|\n)(R[A-Z]\\s+\\d+\\s+BOX|STE.*)", "$1");
		   owner = owner.replaceAll("(?is)(.*)(?:\\s+|\n)(P\\s*O\\s*BOX.*)", "$1");
		   owner = owner.replaceAll("(?is)(.*)(?:\\s+|\n)([\\d-]+\\s+.*)", "$1");
		   owner = owner.replaceAll("(?is)(.*)(?:\\s+|\n)([\\d-]+\\s+.*)", "$1");
		   owner = owner.replaceAll("(NE) (SMITH WAYNE C)", "$1$2");
		   owner = owner.replaceFirst("ATTNY", "");
		   owner = owner.replaceAll("CO-", "CO");
		   owner = owner.replaceAll("(?is)C/O", "\n");
		   owner = GenericFunctions.cleanOwnerFLOsceolaTR(owner);
		   
		   String[] owners = owner.split("\n");
		   
		   List<List> body = new ArrayList<List>();
		   String[] names = {"", "", "", "", "", ""};
		   String[] suffixes, types, otherTypes;
		   String[] lines = {"", "", "", "", ""};
		   String ln="";
			
			try {
				if (ownerCopy.contains("UNITED KINGDOM")) {// for this kind of examples  1119260900-000-00Q01
					for (int j = 0; j < owners.length; j++) {
						if (!LastNameUtils.isNoNameOwner(owners[j])) {
							lines[j] = owners[j];
						} else
							break;
					}
					for (int k = 0; k < owners.length; k++){
							owners[k] = "";
					}
					for (int i = 0; i < lines.length; i++) {
						if (!"".equals(lines[i])) {
							owners[i] = lines[i];
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			for (int i=0; i<owners.length; i++){
				String ow = owners[i];
					if (i == 0) {
						names = StringFormats.parseNameNashville(ow, true);	
						ln = names[2];
						if (names[3].contains("-") && "".equals(names[4])) {
							String aux = names[5];
							names[5] = names[3];
							names[3] = aux;
						}
						if (names[5].contains("-")) {
							names[5] = names[5] + ln;
						}
					} else {
						names = StringFormats.parseNameDesotoRO(ow, true);
					}
					
				names[0] = names[0].replaceFirst("(MC)-(\\w+)", "$1 $2");
				types = GenericFunctions1.extractAllNamesType(names);
				otherTypes = GenericFunctions1.extractAllNamesOtherType(names);   
				suffixes = GenericFunctions1.extractNameSuffixes(names);        
				GenericFunctions1.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes,
											NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
			}
			GenericFunctions1.storeOwnerInPartyNames(m, body, true);
			m.put("PropertyIdentificationSet.NameOnServer", owner);
			
			String[] a = StringFormats.parseNameNashville(owner, true);
			m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
			m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
			m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
			m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
			m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
			m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
	   }
	   
		@SuppressWarnings("rawtypes")
		public static void legalFLLakeTR(ResultMap m, long searchId) throws Exception {	
			   
			   String legal = (String)m.get("PropertyIdentificationSet.PropertyDescription");

			   if (StringUtils.isEmpty(legal))
				   return;
			   			   
			   legal = GenericFunctions.replaceNumbers(legal);
			   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
			   legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
			   
			   legal = legal.replaceFirst("FROM S'LY COR OF", "");
			   legal = legal.replaceAll("\\s+THRU\\s+", "-");
			   //legal = legal.replaceAll("BEG\\s+SE\\s+COR", " ");
			   legal = legal.replaceAll("\\s+&\\s+", "&");
			   
			   legal = legal.replaceAll("\\bA\\s*REPLAT\\s*OF\\b", "");
			   legal = legal.replaceAll("\\b\\d+(ST|ND|RD|TH) ([ADD|ADDITION|ADDN]+)\\b", "");
			   legal = legal.replaceAll("\\b[NWSE]+\\s+[\\d\\.]+\\s+FT\\s+OF\\b", "");
			   legal = legal.replaceAll("\\b[NWSE]+\\s+[\\d/]+\\s+OF\\b", "");
			   legal = legal.replaceAll("GOV", " ");
			   legal = legal.replaceAll("(\\d+)(PB)\\b", "$1" + " " + "$2");
			   legal = legal.replaceAll("(.*)(ORB)", "$1" + " " + "$2");
			   legal = legal.replaceAll("UNTI", "UNIT");
			   legal = legal.replaceAll("(\\d/\\d+/\\d+)", " " + "$1");
			   legal = legal.replaceAll("\\d+\\s+ST", "");
			   legal = legal.replaceAll("\\s+TO\\s*[A-Z]{3}(\\s+|,)", " ");
			   legal = legal.replaceAll("(?is)--.*--", "");
			   legal = legal.replaceAll("(?is)TO(.*)ST", "");
			   legal = legal.replaceAll("(?is)TO\\s+([A-Z'/]+)\\s+([A-Z'/]+) ?([A-Z'/]+) OF", "");
			   legal = legal.replaceAll("(?is)OF\\s*(SE)","$1");
			   
			   String legalTemp = legal;
			   
			   String lot = ""; // can have multiple occurrences
			   Pattern p = Pattern.compile("\\b(LOT)S?\\s*([\\s\\d,]+|[\\d]+|[A-Z])\\b");
			   Matcher ma = p.matcher(legal);
			   while (ma.find()){
				   lot = lot + " " + ma.group(2);
				   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1)+" ");
			   }
			   lot = lot.replaceAll("\\s*&\\s*", " ").trim();
			   if (lot.length() != 0){
				   lot = LegalDescription.cleanValues(lot, false, true);
				   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			   }
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
			   
			   String block = "";
			   p = Pattern.compile("\\b(BLK)\\s*([\\dA-Z-]+)\\b");
			   ma = p.matcher(legal);
			   while (ma.find()){
				   block = block + " " + ma.group(2);
				   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1)+" ");
			   }
			   block = block.replaceAll("\\s*&\\s*", " ").trim();
			   if (block.length() != 0){
				   block = LegalDescription.cleanValues(block, false, true);
				   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
			   }	   
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		  
		   // extract unit from legal description
		   String unit = "";
		   p = Pattern.compile("\\b(UNIT)\\s*(?:NO)?\\s*([\\dA-Z]+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   unit = unit +" " + ma.group(2);
			   unit = unit.trim();
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			   m.put("PropertyIdentificationSet.SubdivisionUnit", unit);//ma.group(2));
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		   }
		   
		   // extract phase from legal description
		   String phase = "";
		   p = Pattern.compile("\\b(PHASE)S?\\s*([,\\dA-Z&-]+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   phase = phase + " " + ma.group(2);
			   phase = phase.trim();
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
			   m.put("PropertyIdentificationSet.SubdivisionPhase", phase); 
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		   }
		   	   	 
		   // extract section from legal description
		   String sec = "";
		   p = Pattern.compile("\\b(SEC)(?:TION)?\\s*([&\\d]+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   sec = sec + " " + ma.group(2);
			   sec = sec.trim();
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			   m.put("PropertyIdentificationSet.SubdivisionSection", sec);
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		   }
		   
		   String tract = "";
		    p = Pattern.compile("\\b(TRACT)(?:S)?\\s*([\\d&\\s]+)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   tract = tract + " " + ma.group(2);
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1)+" ");
		   }
		   tract = tract.replaceAll("\\s*&\\s*", " ").trim();
		   if (tract.length() != 0){
			   tract = LegalDescription.cleanValues(tract, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		   }
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
		   
		   ResultTable saleHistory = (ResultTable) m.get("SaleDataSet");
		   
		   // extract cross refs from legal description
		   List<List> bodyCR = new ArrayList<List>();
		   p = Pattern.compile("\\b(ORB)\\s*(\\d+)\\s*(PG)(?:S)?\\s*([&\\d\\s-]+)\\b");
		   ma = p.matcher(legal);	      	   
		   while (ma.find()){
			   boolean isAlreadyInSDS = false;
			   List<String> line = new ArrayList<String>();		   
			   line.add(ma.group(2));
			   line.add(ma.group(4));
			   line.add("");
			   if (saleHistory == null) {
				   bodyCR.add(line);
				   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			   } else {
				   String[][] body = saleHistory.getBody();
				   for (int i = 0; i < body.length; i++){
					   if (ma.group(2).replaceAll("(?is)\\A0+", "").equals(body[i][0]) && ma.group(4).replaceAll("(?is)\\A0+", "").equals(body[i][1])){
						   isAlreadyInSDS = true;
							break;
					   }
				   }
				   if (!isAlreadyInSDS){
						bodyCR.add(line);
					}
			   }
		   }	       
		   
		   if (!bodyCR.isEmpty()){		  		   		   
			   String [] header = {"Book", "Page", "InstrumentNumber"};		   
			   Map<String,String[]> map = new HashMap<String,String[]>();		   
			   map.put("Book", new String[]{"Book", ""});
			   map.put("Page", new String[]{"Page", ""});
			   map.put("InstrumentNumber", new String[]{"InstrumentNumber", ""});
			   
			   ResultTable cr = new ResultTable();	
			   cr.setHead(header);
			   cr.setBody(bodyCR);
			   cr.setMap(map);		   
			   m.put("CrossRefSet", cr);
		   }	  
		   
		   String pb = "";
		   String pg = "";
		   p = Pattern.compile("\\b(PB|CB)\\s*([\\d]+)\\s+(PG)(?:S)?\\s*([\\d-\\s,&]+)");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   pb = pb + " " + ma.group(2);
			   pg = pg + " " + ma.group(4);
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			   pb = pb.trim();
			   pg = pg.trim();
			   m.put("PropertyIdentificationSet.PlatBook",pb);
			   m.put("PropertyIdentificationSet.PlatNo",pg);
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		   }
		  
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;  
		   
		   String subdiv = "";
		   p = Pattern.compile("\\b(TO|OF)(\\s*)(.+)(\\s*,)");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   subdiv = ma.group(3);		   
			   subdiv = subdiv.replaceFirst("(.+)&.*", "$1");
			   subdiv = subdiv.replaceFirst("(.+)&.*", "$1");
			   if (subdiv.matches("(?is)(?:LOT|BLK|UNIT|PHASE|SEC|TRACT|ORB|PB|CB)\\b.*")) {
				   p = Pattern.compile("\\b(?:[,\\s]+)? (.+?) (?:FROM|\\d+|,|SUB |ADD |NO |PH |PHASE|UNIT |SEC|TRACT |PB |ORB )");
				   ma.usePattern(p);
				   ma.reset();
				   if (ma.find()) {
					   subdiv = ma.group(1).trim();
				   }
			   }
		   } /*else {
			   p = Pattern.compile("\\b(?:[,\\s]+|OF |LOT |BLK |SEC |TO )? (.+?) (?:(\\d+|,|SUB |ADD |NO |PH |PHASE|UNIT |SEC|TRACT |PB |ORB ))");
			   ma.usePattern(p);
			   ma.reset();
			   if (ma.find()){
				   subdiv = ma.group(1);
			   }*/ 
			   else {	   
				   p = Pattern.compile("(.+?) (\\d+|BEG |UNIT |ADD|LOT|PH |PHASE|BLK|SEC|TRACT |ORB|PB|SUB|PLAT)");
				   ma.usePattern(p);
				   ma.reset();
				   if (ma.find()){
					   subdiv = " " + ma.group(1);
					   subdiv = subdiv.replaceFirst("&", " & ");
					   subdiv = subdiv.replaceFirst("\\s(NE|SE|NW|SW|N|S|E|W)\\s*$", "");
					   subdiv = subdiv.replaceFirst("(?:.+),", "");
					   subdiv = subdiv.trim();
				   }   
			   }
		   //}
		   if (subdiv.length() != 0){
			 
			   subdiv = subdiv.replaceAll("(?is)([^,]+),.*", "$1");
			   //subdiv = subdiv.replaceFirst("'S", "");
			   subdiv = subdiv.replaceAll("LOT", "");
			   subdiv = subdiv.replaceFirst("(REVISED\\s+PLAT\\s+OF) (.+)", "$2");
			   //subdiv = subdiv.replaceFirst(".*(?: CIR | WAY | AVE | DR | HWY | AV | LA | UNKNOWN | UNK | ST | RD | LN | CT | TER | BLVD | ROAD | LOOP | TR | PL ) (.+)", "$1");
			   subdiv = subdiv.replaceFirst("(.+) CONDO(MINIUM)?.*", "$1");
			   subdiv = subdiv.replaceFirst("(.+) SUB (OF)?.*", "$1");
			   subdiv = subdiv.replaceFirst("(.+)\\s+(ADD|ADDITION|ADDN).*", "$1");
			   subdiv = subdiv.replaceFirst("(.+) CON.*", "$1");
			   
			   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			   if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
				   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
		   }	
		   
		   String parcelId = (String) m.get("PropertyIdentificationSet.ParcelID");
		   if (StringUtils.isEmpty(parcelId))
			   return;
		   
		   parcelId = parcelId.replaceAll("[-]+", "").trim();
		   String section = "";
		   String township = "";
		   String range = "";
		   p = Pattern.compile("(?is)\\A(\\d{2})(\\d{2})(\\d{2}).*");
		   Matcher mp = p.matcher(parcelId);
		   if (mp.find()){
			   section = mp.group(1).trim();
			   township = mp.group(2).trim();
			   range = mp.group(3).trim();
			   m.put("PropertyIdentificationSet.SubdivisionTownship", township.replaceAll("(?is)\\A0+", ""));
			   m.put("PropertyIdentificationSet.SubdivisionRange", range.replaceAll("(?is)\\A0+", ""));
			   if (StringUtils.isEmpty(sec))
				   m.put("PropertyIdentificationSet.SubdivisionSection", section.replaceAll("(?is)\\A0+", ""));
		   }
		   
		}	

}
