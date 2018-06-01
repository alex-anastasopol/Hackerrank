package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLOsceolaTR {
	
	public static void legalFLOsceolaTR(ResultMap m, long searchId) throws Exception {		   
		   String legal = (String)m.get("tmpPropAddrLegalDesc");

		   if (StringUtils.isEmpty(legal))
			   return;
		   String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		   if ("Citrus".equals(crtCounty)){
			   legal = legal.replaceAll("(?is).*?\r\n(.*?)", "$1");
		   }
		   legal = GenericFunctions.replaceNumbers(legal);
		   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
		   legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
		   
		   //legal = legal.replaceFirst("UNKNOWN", "");
		   legal = legal.replaceAll("\\s+THRU\\s+", "-");
		   legal = legal.replaceAll("\\s+&\\s+", "&");
		   legal = legal.replaceAll("\\bA\\s*REPLAT\\s*OF\\b", "");
		   legal = legal.replaceAll("\\b\\d+(ST|ND|RD|TH) ([ADD|ADDITION|ADDN]+)\\b", "");
		   legal = legal.replaceAll("\\bLESS\\s+[NWSE\\s&]+[\\d\\./\\s]+\\b", "");
		   legal = legal.replaceAll("\\b(FT) (&)?([OF|FOR]+)?\\b", "");
		   legal = legal.replaceAll("\\s*SUB (OF)?", " ");
		   legal = legal.replaceAll("\\b(\\d+)(PB)\\b", "$1" + " " + "$2");
		   legal = legal.replaceAll("\\b(\\d/\\d+/\\d+)", " " + "$1");
		   legal = legal.replaceAll("\\b\\d+\\s+ST", "");
		   legal = legal.replaceAll("&amp;", "&");
		   legal = legal.replaceAll("(?i)\\b(BK\\s*\\d+)(PG\\s*\\d+)", "$1 $2");
		   legal = legal.replaceAll("(?i)\\bPLAT BK\\b", "PB");
		   
		   
		   String legalTemp = legal;
		   
		   // extract section from legal description
		   String sec = "";
		   Pattern p = Pattern.compile("\\b(SEC)\\s*([&0-9]+)\\b");
		   Matcher ma = p.matcher(legal);
		   if (ma.find()){
			   sec = sec + " " + ma.group(2);
			   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			   m.put("PropertyIdentificationSet.SubdivisionSection", sec.trim());
			   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			   legal = legalTemp;
		   }
		   
		   String section = "";
		   String township = "";
		   String range = "";
		   p = Pattern.compile("(?is)\\b(\\d+)\\s*/\\s*(\\d+)\\s*/\\s*(\\d+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()) {
			   section = ma.group(1);
			   township = ma.group(2);
			   range = ma.group(3);
			   m.put("PropertyIdentificationSet.SubdivisionTownship", township);
			   m.put("PropertyIdentificationSet.SubdivisionRange", range);
			   if (StringUtils.isEmpty(sec))
				   m.put("PropertyIdentificationSet.SubdivisionSection", section);
			   legalTemp = legalTemp.replaceFirst(ma.group(0), " ");
		   }
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
			
		   String lot = ""; // can have multiple occurrences
		   p = Pattern.compile("\\b(LO?T)S?\\s*([-\\d,&\\s]+|[A-Z]?|[\\dA-Z]+)([\\s|,])?\\b");
		   ma = p.matcher(legal);
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
		   p = Pattern.compile("\\b(BLK)S?\\s*([\\dA-Z&]+)\\b");
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
	   p = Pattern.compile("\\b(UNIT)\\s*([\\d]+[A-Z]?|[A-Z])\\b");
	   ma = p.matcher(legal);
	   if (ma.find()){
		   unit = unit +" " + ma.group(2);
		   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		   m.put("PropertyIdentificationSet.SubdivisionUnit", unit.trim());//ma.group(2));
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
	   }
	   
	   // extract phase from legal description
	   String phase = "";
	   p = Pattern.compile("\\b(PH)A?S?E?\\s*([,\\dA-Z-]+)\\b");
	   ma = p.matcher(legal);
	   if (ma.find()){
		   phase = phase + " " + ma.group(2);
		   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		   m.put("PropertyIdentificationSet.SubdivisionPhase", phase.trim()); 
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
	   }
	   	   	 	   
	   String tract = "";
	   p = Pattern.compile("\\b(TRACT)\\s*([A-Z-\\d]+)\\b");
	   ma = p.matcher(legal);
	   if (ma.find()){
		   tract = tract + " " + ma.group(2);
		   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		   m.put("PropertyIdentificationSet.SubdivisionTract", tract.trim());
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
	   }
	   
	   //	 extract building #
	   p = Pattern.compile("\\b(BLDG) ([-A-Z\\d]+)\\b");
	   ma = p.matcher(legal);
	   if (ma.find()){
		   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		   m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2).trim());
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
	   }
	   
	   legal = legal.replaceAll("(OR\\s+)", " " + "$1");
	   // extract cross refs from legal description
	   @SuppressWarnings("rawtypes")
	   List<List> bodyCR = new ArrayList<List>();
	   p = Pattern.compile("\\b(OR)\\s*(\\d+)/(\\d+)\\b");
	   ma = p.matcher(legal);	      	   
	   while (ma.find()){
		   List<String> line = new ArrayList<String>();		   
		   line.add(ma.group(2).trim());
		   line.add(ma.group(3).trim());
		   line.add("");
		   bodyCR.add(line);
		   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));		   
	   }
	   legal = legalTemp;
	   legal = legal.replaceAll("(OR\\s+)", " " + "$1");
	   p = Pattern.compile("\\b(O\\s*R)B?\\s*(?:BK\\s*)?(\\d+)\\s*(PG)\\s*(\\d+)\\b");
	   ma.reset();
	   ma = p.matcher(legal); 	      	   
	   while (ma.find()){
		   List<String> line = new ArrayList<String>();		   
		   line.add(ma.group(2).trim().replaceAll("\\A0+", ""));
		   line.add(ma.group(4).trim().replaceAll("\\A0+", ""));
		   line.add("");
		   bodyCR.add(line);
		   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));		   
	   }
	   legal = legalTemp;
	   p = Pattern.compile("\\b(O\\s*R)B?\\s*(?:BK\\s*)?(\\d+)\\b");
	   ma.reset();
	   ma = p.matcher(legal); 
	   while (ma.find()){
		   List<String> line = new ArrayList<String>();		   
		   line.add(ma.group(2).trim().replaceAll("\\A0+", ""));
		   line.add("");
		   line.add("");
		   bodyCR.add(line);
		   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));		   
	   }
	   legal = legalTemp; 
		   
	   ResultTable sds = (ResultTable) m.get("SaleDataSet");
	   if (sds != null && !bodyCR.isEmpty()){
		   String[][] bodyH = sds.getBodyRef();
		   for(String[] str : bodyH){
			   for (int i = 0; i < bodyCR.size(); i++){
				   if (str[0].trim().equals(bodyCR.get(i).get(0).toString().trim()) 
						   	&& str[1].trim().equals(bodyCR.get(i).get(1).toString().trim())){
					   bodyCR.remove(i);
				   }
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
	   	  
	   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
	   legal = legalTemp; 
	   
	   String pb = "";
	   p = Pattern.compile("\\b(PB(?:\\s+BK)?|BK|CB)\\s*([#\\dA-Z]+)\\b");
	   ma = p.matcher(legal);
	   while (ma.find()){
		   pb = pb + " " + ma.group(2);
		   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		   m.put("PropertyIdentificationSet.PlatBook",pb.trim());
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
	   }
	   legal = legal.replaceAll("((PG)S?\\s*([\\d-\\s/]+))", "$1" + " ");
	   String pg = "";
	   p = Pattern.compile("\\b(PG)S?\\s*([\\d-\\s/]+)\\s+");
	   ma = p.matcher(legal);
	   while (ma.find()){
		   pg = pg + " " + ma.group(2);
		   legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		   m.put("PropertyIdentificationSet.PlatNo",pg.trim());
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
	   }
	   
	   
	   legal = legal.replaceFirst("(A) (CONDO.*)", "$2");
	   
	   String subdiv = "";
	   p = Pattern.compile("\\s+(?:CIR|WAY|AVE|DR|HWY|AV|LA|UNKNOWN|UNK|ST|RD|LN|CT|TER|BLVD|ROAD|LOOP|TR) (.+?)(?:(\\d|PH|UNIT|LOTS?|BLK|SEC|TRACT|PB|BK|CB|PG))");
	   ma = p.matcher(legal);
	   if (ma.find()){
		   subdiv = ma.group(1);		   
	   } else {
		   p = Pattern.compile(".*\\b(?:TRACT ) (.+?) (?:\\d|PH|UNIT|LOT|BLK|SEC|TRACT|PB|BK|CB|PG)\\b.*");
		   ma.usePattern(p);
		   ma.reset();
		   if (ma.find()){
			   subdiv = ma.group(1);
		   } else {
			   p = Pattern.compile("(.+?) (?:\\bPB|LOTS?|UNREC)\\b.*");
			   ma.usePattern(p);
			   ma.reset();
			   if (ma.find()){
				   subdiv = ma.group(1);
			   }   
		   } 
	   }
	   if (subdiv.length() > 4){
		 
		   subdiv = subdiv.replaceFirst("TRACT(.+)", "$1");
		   subdiv = subdiv.replaceFirst("(.+) CONDO(MINIUM)?.*", "$1");
		   subdiv = subdiv.replaceFirst("(.+) SUB (OF)?.*", "$1");
		   subdiv = subdiv.replaceFirst("(.+)\\s+(ADD|ADDITION|ADDN).*", "$1");
		   subdiv = subdiv.replaceFirst("(.+) CON.*", "$1");
		   subdiv = subdiv.replaceFirst("(.+) UNIT\\b.*", "$1");
		   subdiv = subdiv.replaceFirst("(.+) EX.*", "$1");
		   subdiv = subdiv.replaceFirst("[NWSE]? \\s+", "");
		   subdiv = subdiv.replaceFirst("\\bUNREC.*", "");
		   subdiv = subdiv.replaceFirst("(?is)\\A\\s*UNKNOWN\\s+(.+)", "$1");
		   //subdiv = subdiv.replaceFirst("(.+) (BLK|BLOCK(S)?).*", "$1");
		   //subdiv = subdiv.replaceFirst("(.+) SUBD.*", "$1");
		   
		   if (subdiv.matches(".*\\d+/\\d+.*")){
			   subdiv = "";
		   }
		   
		   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
		   if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
			   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
	   }	
	   
	}
	
}
