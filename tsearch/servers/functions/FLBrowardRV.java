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
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLBrowardRV {
	   public static void legalRemarksFLBrowardRV(ResultMap m, long searchId) throws Exception {
		   String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		   if (legal == null || legal.length() == 0)
			   return;

		   legalTokenizerRemarksFLBrowardRV(m, legal);
	   }
	   
	   public static void legalTokenizerRemarksFLBrowardRV(ResultMap m, String legal) throws Exception{
		   
		   //initial corrections and cleanup of legal description
		   legal = GenericFunctions.replaceNumbers(legal);
		   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
		   legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers to arabics
		   	   	  
		   legal = legal.replaceAll("\\b(THAT )?P(AR)?T( (OF|IN))?\\b", "");
		   legal = legal.replaceAll("\\b[SWEN]\\d+[/\\.]\\d+\\s*('T?)?", "");	   
		   legal = legal.replaceAll("\\b[SWEN]\\s*\\d+\\b\\s*('T?)?( OF\\b)?", "");	   
		   legal = legal.replaceAll("\\b\\d+\\.\\d+\\b", "");
		   legal = legal.replaceAll("\\bGOV\\b", "");
		   legal = legal.replaceAll("\\bDESC( (IN|AS))?\\b", "");
		   legal = legal.replaceAll("\\bLESS\\b", "");
		   legal = legal.replaceAll("\\bCORR(?=(\\s|\\d+))", "");
		   legal = legal.replaceAll("\\bCORRECTED\\b", "");
		   legal = legal.replaceAll("\\b(A )?POR OF\\b", "");
		   legal = legal.replaceAll("\\bPOD \\d+( AT)?\\b", "");
		   legal = legal.replaceAll("\\bUNREC( Z)?\\b", "");
		   legal = legal.replaceAll("-?\\bREPLAT( OF)?\\b", "");
		   legal = legal.replaceAll("\\bPOR(TION)?( OF)?\\b", "");
		   legal = legal.replaceAll("\\bP(AR)?T( OF)?\\b", "");	   
		   legal = legal.replaceAll("\\bSITE \\d+\\b", "");
		   legal = legal.replaceAll("\\bAMENDED\\b", "");
		   legal = legal.replaceAll("\\bPLEDGE\\b", "");
		   legal = legal.replaceAll("\\bVLG \\d+,", "");
		   legal = legal.replaceFirst(" Z$", "");
		   legal = legal.replaceFirst(",(O|E)(?=,)", "");
		   legal = legal.replaceAll("\\bNOT SHOWN\\b", "");
		   legal = legal.replaceAll("[\\d+,]+ PNTS\\b,?", "");
		   legal = legal.replaceAll("^\\s*,\\s*", "");
		   
		   legal = legal.replaceAll("\\b(/\\d+),DCR\\b", "$1DCR,");
		   legal = legal.replaceAll("\\bTHRU\\b", "-");
		  	   
		   legal = legal.trim();
		   legal = legal.replaceAll("\\s{2,}", " ");	   
		   
		   // extract and remove unit from legal description	   
		   String unit = "";
		   String bldg = "";
		   boolean isCondo = false;
		   String patt = "((?:\\d+(?:-\\d+[A-Z]|-?[A-Z])?|[A-Z])(?:-\\d+)?)";
		   Pattern p = Pattern.compile("\\bU(?:NIT )?"+patt+"\\b");
		   Matcher ma = p.matcher(legal);	   	   
		   if (ma.find()){
			   unit = unit + " " + ma.group(1);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
		   }
		   p = Pattern.compile("^A((?:\\d+(?:-[A-Z])?|[A-Z])(?:-\\d+)?)\\b");
		   ma = p.matcher(legal);	   	   
		   if (ma.find()){
			   unit = unit + " " + ma.group(1);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
			   isCondo = true;
		   }
		   if (legal.matches(".*\\bCONDO\\b.*")){
			   p = Pattern.compile("\\bPARCEL (\\d+)\\b");
			   ma = p.matcher(legal);	   	   
			   if (ma.find()){
				   unit = unit + " " + ma.group(1);
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
				   isCondo = true;
			   }		   
		   }
		   p = Pattern.compile("\\bWK\\d+-(\\d+)\\b(?!,\\d)");
		   ma = p.matcher(legal);	   	   
		   if (ma.find()){
			   unit = unit + " " + ma.group(1);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
			   isCondo = true;
		   }
		   boolean isSubdiv = false;
		   p = Pattern.compile("\\bWK\\s*\\d+(?:[,\\s-]+\\d+)*\\b,?(?:U([A-Z]{0,2}\\d+),?)?");
		   ma = p.matcher(legal);	   	   
		   if (ma.find()){
			   legal = legal.replaceAll("\\bWK\\s*\\d+(?:[,\\s-]+\\d+)*\\b,?(?:U([A-Z]{0,2}\\d+),?)?", "");
			   isSubdiv = true;
			   if (ma.group(1) != null)
				   unit = unit + " " + ma.group(1);
		   }
		   p = Pattern.compile("^P(\\d+)([A-Z])?(,\\d+)*(?=,)");
		   ma = p.matcher(legal);	   	   
		   if (ma.find()){
			   unit = unit + " " + ma.group(1);
			   if (ma.group(3) != null){
				   unit = unit + ma.group(3);
				   unit = unit.replaceAll(",", " ");
			   }
			   bldg = ma.group(2);
			   if (bldg == null)
				   bldg = "";
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
			   isCondo = true;
		   }	  
		   unit = unit.trim();
		   if (unit.length() != 0){
			   unit = unit.replaceAll("-", "@");
			   String unit2 = (String) m.get("PropertyIdentificationSet.SubdivisionUnit");
			   if (!StringUtils.isEmpty(unit2)){
				   unit2 = unit2 + " " + unit;
			   } else {
				   unit2 = unit;
			   }		   
			   unit2 = LegalDescription.cleanValues(unit2, false, true);
			   unit2 = unit2.replaceAll("@", "@ ");
			   unit2 = Roman.normalizeRomanNumbersExceptTokens(unit2, exceptionTokens);
			   unit2 = unit2.replaceAll("@ ", "-");
			   m.put("PropertyIdentificationSet.SubdivisionUnit", unit2);		  
			   legal = legal.trim().replaceAll("\\s{2,}", " ");		   
		   }
		   	   
		   // extract and remove building from legal description
		   if (unit.length() != 0){
			   p = Pattern.compile("(?<=[\\s,])B(\\d+|[A-Z])(?=(?:,|$))");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   bldg = bldg + " " + ma.group(1);
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
			   }
		   }
		   p = Pattern.compile("\\bBLDG (\\d+|[A-Z])\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   bldg = ma.group(1);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
			   
		   }
		   bldg = bldg.trim();
		   if (bldg.length() != 0){
			   m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and replace lot from legal description
		   String lot = ""; 	  
		   p = Pattern.compile("\\bLOTS? ((?:\\d+(?:-?[A-Z])?|[A-Z]\\d+)(?:[\\s&,-]+\\d+)*)\\b");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   lot = lot + " " + ma.group(1);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "LOT ");
			   
		   }
		   p = Pattern.compile("\\bL((?:\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?)([,&-]\\d+)*)(?=(?:,|$))");
		   ma = p.matcher(legal);
		   while (ma.find()){
			   lot = lot + " " + ma.group(1);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "lot");		   
		   }
		   lot = lot.trim();
		   if (lot.length() != 0){
			   lot = lot.replace(',', ' ');
			   lot = lot.replace('&', ' ');
			   String lot2 = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			   if (!StringUtils.isEmpty(lot2)){
				   lot2 = lot2 + " " + lot;
			   } else {
				   lot2 = lot;
			   }
			   lot2 = LegalDescription.cleanValues(lot2, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot2);		   
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and replace block from legal description
		   String block = "";
		   p = Pattern.compile("\\bBLKS? ((?:\\d+|[A-Z](?:-\\d+)?)(?:[\\s,&-]+\\d+)?)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   block = ma.group(1).replaceAll("[,&]", " ");		   
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "BLK ");
		   }
		   p = Pattern.compile("\\bB(\\d+|[A-Z])(?=(?:,|$))");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   block = block + " " + ma.group(1);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "blk");
		   }
		   if (block.length() != 0){
			   String block2 = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			   if (!StringUtils.isEmpty(block2)){
				   block2 = block2 + " " + block;
			   } else {
				   block2 = block;
			   }
			   block2 = LegalDescription.cleanValues(block2, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionBlock", block2);		   
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and replace tract from legal description
		   String tract = "";
		   p = Pattern.compile("\\bTR\\s*(\\d+|[A-Z](?:-?\\d+)?)(?=(?:,|$))");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   tract = ma.group(1);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "tract");
		   }
		   p = Pattern.compile("\\bTR (\\d+|[A-Z](?:-?\\d+)?)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   tract = tract + " " + ma.group(1);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "TRACT ");
		   }
		   if (tract.length() != 0){
			   m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and replace phase from legal description
		   String phase = "";
		   p = Pattern.compile("\\bPH(?:ASE)? (\\d+[A-Z]?|I)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   phase = ma.group(1);
			   if ("I".equals(phase))
				   phase = "1";
			   m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "PHASE ");
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }	  
		   
		   // extract and remove plat b&p from legal description
		   legal = legal.replaceFirst("^P[A-Z](-?\\d+)?\\b\\s*", "parcel");
		   legal = legal.replaceFirst("^,+\\s*", "");
		   String platBk = "", platPg = "";
		   p = Pattern.compile("\\b(\\d+)[/-](\\d+) (?:B|D|PBC)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   platBk = ma.group(1);
			   platPg = ma.group(2);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "PLAT ");
		   } else {
			   p = Pattern.compile("(?:(?:RR)?\\d+/\\d+,)?.*((?:lot,(?:blk,)?|parcel|tract,).*\\s*\\b(\\d+|[A-Z])/(\\d+)(?:D(?:CR)?|PBC)?\\b)");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   platBk = ma.group(2);
				   platPg = ma.group(3);
				   legal = legal.replaceFirst("\\b"+ma.group(1)+"\\b", "");
			   } else {
				   p = Pattern.compile("(?<=,)\\s*(\\d+)/(\\d+),lot(?:,blk)?\\b");
				   ma = p.matcher(legal);
				   if (ma.find()){
					   platBk = ma.group(1);
					   platPg = ma.group(2);
					   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
				   } else {
					   p = Pattern.compile("\\b([A-Z])/(\\d+)\\b");
					   ma = p.matcher(legal);
					   if (ma.find()){
						   platBk = ma.group(1);
						   platPg = ma.group(2);
						   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
					   } else {
						   p = Pattern.compile("\\b(\\d+|[A-Z])/(\\d+)(?:D(?:CR)?|PBC)\\b");
						   ma = p.matcher(legal);
						   if (ma.find()){
							   platBk = ma.group(1);
							   platPg = ma.group(2);
							   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
						   } else {
							   p = Pattern.compile("(?<=\\bPLAT #?\\d{1,3},?)\\s*\\b(\\d+|[A-Z])/(\\d+)\\b(?!/)");
							   ma = p.matcher(legal);
							   if (ma.find()){
								   platBk = ma.group(1);
								   platPg = ma.group(2);
								   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
							   }
						   }
					   }
				   }
			   }
		   }		   
		   if (platBk.length() != 0){
			   m.put("PropertyIdentificationSet.PlatBook", platBk);
			   m.put("PropertyIdentificationSet.PlatNo", platPg);
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   // extract and remove cross refs from legal description
		   List<List> bodyCR = new ArrayList<List>();
		   p = Pattern.compile("\\b(\\d+\\s)?OR (\\d+)/(\\d+)\\b");
		   ma = p.matcher(legal);   	   
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();		
			   line.add(ma.group(2));
			   line.add(ma.group(3));
			   String instrNo = ma.group(1);
			   if (instrNo == null){
				   instrNo = "";
			   }
			   line.add(instrNo);
			   line.add("OR");
			   bodyCR.add(line);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");		   
		   }
		   p = Pattern.compile("\\b(?:RR)?(?<!/)(\\d+)/(\\d+)\\b(?!/)");
		   ma = p.matcher(legal);   	   
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();		
			   line.add(ma.group(1));
			   line.add(ma.group(2));
			   line.add("");
			   line.add("");
			   bodyCR.add(line);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");		   
		   }
		   p = Pattern.compile("\\bCFN\\s*(\\d+)\\b");
		   ma = p.matcher(legal);	      	   
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();		   
			   line.add("");
			   line.add("");
			   line.add(ma.group(1));
			   line.add("");
			   bodyCR.add(line);		   
		   }
		   p = Pattern.compile("^\\s*(\\d+)/(\\d+)\\s*$");
		   ma = p.matcher(legal);	      	   
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();		   
			   line.add(ma.group(1));
			   line.add(ma.group(2));
			   line.add("");
			   line.add("");
			   bodyCR.add(line);		   
		   } 
		   GenericFunctions.saveCRInMap(m, bodyCR);  
		   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   	   	   
		   // extract and replace section, township and range from legal description 
		    List<List> body = GenericFunctions.getSTRFromMap(m); //first add sec-twn-rng extracted from XML specific tags, if any (for DT use)
		   p = Pattern.compile("\\b(\\d+)-(\\d+)-(\\d+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){	
			   List<String> line = new ArrayList<String>();
			   line.add(ma.group(1));
			   line.add(ma.group(2));
			   line.add(ma.group(3));
			   body.add(line);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");		   		    
		   } 
		   p = Pattern.compile("(?<=\\bPLAT )(\\d+)/(\\d+)/(\\d+)");
		   ma = p.matcher(legal);
		   if (ma.find()){		 
			   List<String> line = new ArrayList<String>();
			   line.add(ma.group(1));
			   line.add(ma.group(2));
			   line.add(ma.group(3));
			   body.add(line);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");		   		    
		   } 
		   p = Pattern.compile("\\bSEC(?:TION)? ((?:\\d+|[A-Z])(?: \\d+)*)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   List<String> line = new ArrayList<String>();
			   line.add(ma.group(1));
			   line.add("");
			   line.add("");
			   body.add(line);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "SEC ");		  
		   } else {
			   p = Pattern.compile("\\b(\\d+)(?:ST|ND|RD|TH)? SEC(?:TION)?\\b");
			   ma = p.matcher(legal);
			   if (ma.find()){	
				   List<String> line = new ArrayList<String>();
				   line.add(ma.group(1));
				   line.add("");
				   line.add("");
				   body.add(line);
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "SEC ");		  
			   }
		   }
		   GenericFunctions.saveSTRInMap(m, body);
		   legal = legal.trim().replaceAll("\\s{2,}", " ");	    	   	  	  	   	 
		   
		   // extract subdivision name - only if lot or block or unit or tract or phase was extracted 
		   // or legal contains CONDO or SUB or PLAT or PARCEL; otherwise the text might be a doc type, a case number etc.
		   // additional cleaning before extracting subdivision name	   
		   String subdiv = "";
		   if (lot.length() != 0 || block.length() != 0 || unit.length() != 0 || phase.length() != 0 || tract.length() != 0 || platBk.length() != 0 || 
				   isCondo || isSubdiv || legal.matches("(?i).*\\b(CONDO|SUB|PARCEL|PLAT|WK \\d+)\\b.*")){
			   legal = legal.replaceAll("\\bWK\\s*\\d+[A-Z]?\\b", "");
			   legal = legal.replaceAll("\\b\\d+/\\d+/\\d+\\b", "");
			   legal = legal.replaceAll("(?i)\\bPARCEL( \\w+)?\\b", "");
			   legal = legal.replaceAll("^,?P[A-Z](-?\\d+)?,", "");
			   legal = legal.replaceAll("^,?P\\d+([,\\s-]+\\d+)*\\b", "");
			   legal = legal.replaceAll("\\blot\\b", "");
			   legal = legal.replaceAll("\\bblk\\b", "");
			   legal = legal.replaceAll("\\btract\\b", "");
			   legal = legal.replaceAll("^\\s*,\\s*", "");
			   legal = legal.replaceAll("\\s*,\\s*$", "");
			   legal = legal.replaceAll("\\([^\\)]*\\)", "");
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
			   subdiv = legal;
			   patt = "(?:PLAT|LOT|BLK|SEC|PHASE|UNIT|TRACT)";
			   p = Pattern.compile("(.*?)\\s*\\b(?:CONDO|SUB)\\b");		   
			   ma = p.matcher(legal);
			   if (ma.find()){
				   subdiv = ma.group(1);
				   subdiv = subdiv.replaceFirst("^(?:"+patt+" )+(.*)", "$1");
			   } 
			   p = Pattern.compile("(.*?)(?:\\b"+patt+"\\b|$)");
			   ma = p.matcher(subdiv);
			   if (ma.find()){
				   subdiv = ma.group(1);
			   }
			   
		   }
		   subdiv = subdiv.trim();
		   if (subdiv.length() != 0){
			   subdiv = subdiv.replaceFirst("^,\\s*", "");
			   if (!subdiv.matches("SECTORS.+,.+")){
				   subdiv = subdiv.replaceFirst("(.+),.*", "$1");
			   }
			   subdiv = subdiv.replaceFirst("(.+?) (\\d+(ST|ND|RD|TH)? )?ADD\\b.*", "$1");
			   subdiv = subdiv.replaceFirst("\\s*,\\s*$", "");
			   subdiv = subdiv.replaceFirst("^\\s*,\\s*", "");
			   subdiv = subdiv.replaceFirst("\\sZ$", "");
			   // remove last token from subdivision name if it is a number (as roman or arabic)
			   if (!subdiv.startsWith("SECTOR")){
				   p = Pattern.compile("(.*)\\b(\\w+)$");
				   ma = p.matcher(subdiv);
				   if (ma.find()){
					   String lastToken = ma.group(2); 
					   lastToken = Roman.normalizeRomanNumbersExceptTokens(lastToken, exceptionTokens); 
					   if (lastToken.matches("\\d+")){
						   subdiv = ma.group(1);				   
					   }
				   }
				   subdiv = subdiv.replaceFirst("\\s*\\b\\d+[\\s-]?[A-Z]\\s*$", ""); 
			   }
			   subdiv = subdiv.replaceFirst("\\s*(\\bNO|#)\\s*$", ""); 
			   subdiv = subdiv.trim();
		   }
		   if (subdiv.length() != 0){
			   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			   if (isCondo || legal.matches(".*\\bCONDO\\b.*")){
				   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			   }
		   }
	   }
	   
	   public static void legalFLBrowardDASLRV(ResultMap m, long searchId) throws Exception {
		   String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		   if (legal == null || legal.length() == 0)
			   return;

		   legalTokenizerFLBrowardDASLRV(m, legal);
	   }	   
	   
	   @SuppressWarnings("unchecked")
		public static void legalTokenizerFLBrowardDASLRV(ResultMap m, String legal) throws Exception {
			   
			   legal = legal.replaceAll("\\bORDN #?[\\d-]+\\s*", "");
			   legal = legal.replaceAll("\\bNO\\b\\s*", "");
			   legal = legal.replaceAll("\\b(\\d+)(?:ST|ND|RD|TH) (UNIT|SEC(?:TION)?)", "$2 $1");
			   legal = legal.replaceAll("\\b(A )?REPLAT( OF)?\\b\\s*", "");  
			   legal = legal.replaceAll("\\s*\\bTHRU\\b\\s*", "-");
			   legal = legal.replaceAll("(\\d)\\s+TO\\s+(\\d)", "$1-$2");
			   legal = legal.replaceAll("\\b(A )?POR( OF)?\\b", "");
			   legal = legal.replaceAll("\\bDESC( AS| IN)?\\b", "");
			   legal = legal.replaceAll("\\b\\d+/\\d+ INT\\b", "");
			   legal = legal.replaceAll("\\bLESS\\b", "");
			   legal = legal.replaceAll("\\bCORR\\b", "");
			   legal = legal.replaceAll("\\bPOD \\d+( AT)?\\b", "");
			   
			   // extract section, township and range from legal description
			   Pattern p = Pattern.compile("\\b(?:SEC )?(\\d+)-(\\d+)-(\\d+)\\b");
			   Matcher ma = p.matcher(legal);	   
			   List<List> bodySTR = new ArrayList<List>();	   	   
			   while (ma.find()){
				   List<String> line = new ArrayList<String>();
				   line.add(ma.group(1));
				   line.add(ma.group(2));
				   line.add(ma.group(3));
				   bodySTR.add(line);
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
			   } 
			   
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
			   String subdiv = legal;
			   String lotPatt = "\\bLOTS?\\s+(\\d+(?:-?[A-Z])?(?:[\\s/,&-]+\\d+)*|[A-Z]{1,2}(?:-?\\d+)?)\\b";
			   String platPatt = "\\b(\\d+|[A-Z])[/\\s-](\\d+(?:,\\d+)?)\\s*(?:B|D|PB)\\b"; //"\\b(\\d+)[/-](\\d+)\\s*(?:B|D|PB)\\b";
			   
			   //cleanup legal descr before extracting subdivision name
			   subdiv = subdiv.replaceAll("^TRACTS? (DESC AS )?BEG \\d+ ([SWNE]+ )?FT ([SENW]+ )?OF ([SENW]+ )?COR OF( (LOTS?|BL(OC)?K|UNIT) \\d+)*", "");
			   subdiv = subdiv.replaceAll("^SUBDIVISION OF TRACTS? \\d+( AND \\d+)? OF", "");
			   subdiv = subdiv.replaceAll("^C?OMM AT [SWNE]+ COR OF( (LOTS?|BL(OC)?K|UNIT) \\d+)*.", "");
			   subdiv = subdiv.replaceAll("^\\s*(\\d+(ST|ND|RD|TH) )?ADD(ITIO)?N?( TO)?\\b\\s*", "");
			   subdiv = subdiv.trim();
			   String subdivTemp = "";
			   	   	
			   if (!subdiv.matches("\\b[SWEN]{1,2}\\s*\\d+([/\\.]\\d+)? OF .*")){
				   p = Pattern.compile("(.*?)\\s*\\b(LOTS?|UNIT|PLAT|PB|A COOPERATIVE|(A )?CONDO(MINIUM)?|ADD(ITION)?|ADDN?|PARCELS? \\w+|PH(ASE)?|SEC(TION)?|(RE)?SUB(DIVISION)?|REV(ISED)?|THAT PART|(IN )?[SWNE]+ \\d+((\\.|/)\\d+)?|TRACTS?|EXTENSION|BL(?:OC)?KS?|BLDG|"+platPatt+"|APT|PER DECL)\\b.*");
				   ma = p.matcher(subdiv);
				   if (ma.find()){
					   subdivTemp = ma.group(1);			   		   			   
				   }
			   } else {
				   p = Pattern.compile(".*\\bAKA:\\s*"+lotPatt+"\\s+(.*)");
				   ma = p.matcher(subdiv);
				   if (ma.find()){
					   subdivTemp = ma.group(2);			   		   			   
				   }
			   }
			   if (subdivTemp.length() != 0){
				   subdivTemp = subdivTemp.replaceFirst("\\s+#\\d+\\s*$", "");
				   
				   // remove last token from subdivision name if it is a number (as roman or arabic)
				   if (!subdivTemp.matches("SECTOR \\w+")){
					   p = Pattern.compile("(.*)\\s*\\b(\\w+)\\s*$");
					   ma = p.matcher(subdivTemp);
					   if (ma.find()){
						   String lastToken = ma.group(2); 
						   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
						   lastToken = Roman.normalizeRomanNumbersExceptTokens(lastToken, exceptionTokens); 
						   if (lastToken.matches("\\d+")){
							   subdivTemp = ma.group(1);				   
						   } else {
							   lastToken = GenericFunctions.replaceNumbers(lastToken);
							   if (lastToken.matches("\\d+(ST|ND|RD|TH)?")){
								   subdivTemp = ma.group(1);
							   }
						   }
					   }
				   }
				   subdivTemp = subdivTemp.replaceAll("\\s*\\d+(ST|ND|RD|TH) REVISION\\s*$", "");
				   subdivTemp = subdivTemp.replaceAll("\\s+[ANWSE]$", "");
				   subdivTemp = subdivTemp.replaceFirst("\\s+SEE$", "");		   
				   subdivTemp = subdivTemp.replaceFirst("^OF\\s+", "");
				   subdivTemp = subdivTemp.replaceFirst("\\bCOMM .*", "");
				   subdivTemp = subdivTemp.replaceAll("\\bAMENDED\\b", "");
				   subdivTemp = subdivTemp.replaceFirst("\\s*-\\s*$", "");
				   if (!subdivTemp.startsWith("SECTOR"))
					   subdivTemp = subdivTemp.replaceFirst("\\s*&?\\s+\\d+\\s*$", "");
				   subdivTemp = subdivTemp.replaceFirst("\\bBOUNDARY\\b", "");
				   subdivTemp = subdivTemp.replaceFirst("\\s*\\(\\s*$", "");
				   subdivTemp = subdivTemp.trim().replaceAll("\\s{2,}", "");
		       
				   if (subdivTemp.length() != 0){
					   m.put("PropertyIdentificationSet.SubdivisionName", subdivTemp);
					   if (legal.contains("CONDO"))
						   m.put("PropertyIdentificationSet.SubdivisionCond", subdivTemp);
				   }
			   }
				   	  
			   legal = legal.replaceAll("#(?=\\d)", "");
			   legal = legal.replaceAll("\\s*\\b[SWNE]{1,2}\\s?\\d+((\\.|/)\\d+)?\\b(?!-)", "");
			   legal = legal.replaceAll("\\s\\d+\\.\\d+\\b", "");	  
			   legal = legal.replaceAll("\\bTO\\b", "-");
			   String origLegal = legal;
			   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
			   legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);  // convert roman numbers to arabics 
			   legal = GenericFunctions.replaceNumbers(legal);	   

			   // extract section from legal description
			   p = Pattern.compile("\\bSEC(?:TION)? (\\d+) TWN (\\d+) RANGE (\\d+)\\b");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   List<String> line = new ArrayList<String>();
				   line.add(ma.group(1));
				   line.add(ma.group(2));
				   line.add(ma.group(3));
				   bodySTR.add(line);
			   } else {
				   p = Pattern.compile("\\bSEC(?:TION)? (\\d+(?:-?[A-Z])?|[A-Z]\\d*)\\b(?!-\\d)");
				   ma = p.matcher(legal);
				   boolean foundSec = false;
				   while (ma.find()){
					   List<String> line = new ArrayList<String>();
					   line.add(ma.group(1));
					   line.add("");
					   line.add("");
					   bodySTR.add(line);
					   foundSec = true;
				   }
				   if (!foundSec){
					   p = Pattern.compile("\\b(\\d+)(?:ST|ND|RD|TH)? SEC(?:TION)?\\b");
					   ma = p.matcher(legal);
					   while (ma.find()){
						   List<String> line = new ArrayList<String>();
						   line.add(ma.group(1));
						   line.add("");
						   line.add("");
						   bodySTR.add(line);
					   }
				   }		   
			   }
			   
			   if (!bodySTR.isEmpty()){
				   String [] header = {"SubdivisionSection", "SubdivisionTownship", "SubdivisionRange"};
				   
				   Map<String,String[]> map = new HashMap<String,String[]>();
				   map.put("SubdivisionSection", new String[]{"SubdivisionSection", ""});
				   map.put("SubdivisionTownship", new String[]{"SubdivisionTownship", ""});
				   map.put("SubdivisionRange", new String[]{"SubdivisionRange", ""});
				   
				   ResultTable pis = new ResultTable();	
				   pis.setHead(header);
				   pis.setBody(bodySTR);
				   pis.setMap(map);
				   m.put("PropertyIdentificationSet", pis);
			   }
			   
			   // extract plat book & page from legal description
			   List<List> bodyPlat = new ArrayList<List>();
			   p = Pattern.compile("\\bPB (\\d+) PG (\\d+)\\b");
			   ma.usePattern(p);
			   ma.reset();
			   if (ma.find()){
				   List<String> line = new ArrayList<String>();
				   line.add(ma.group(1));
				   line.add(ma.group(2));
				   bodyPlat.add(line);
			   } 
			   p = Pattern.compile("\\b(?:PLAT|PB|REC) (\\d+)-(\\d+)\\b(?:\\s*(?:B|D|PB)\\b)?");
			   ma.usePattern(p);
			   ma.reset();
			   if (ma.find()){
				   List<String> line = new ArrayList<String>();
				   line.add(ma.group(1));
				   line.add(ma.group(2));
				   bodyPlat.add(line);
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "").replaceAll("\\s{2,}", " ");
				   origLegal = origLegal.replaceFirst("\\b"+ma.group(0)+"\\b", "").replaceAll("\\s{2,}", " ");
			   } 
			   p = Pattern.compile(platPatt);
			   ma = p.matcher(legal);
			   while (ma.find()){
				   List<String> line = new ArrayList<String>();
				   line.add(ma.group(1));
				   line.add(ma.group(2));
				   bodyPlat.add(line);
				   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "").replaceAll("\\s{2,}", " ");
				   origLegal = origLegal.replaceFirst("\\b"+ma.group(0)+"\\b", "").replaceAll("\\s{2,}", " ");
			   }
			   if (!bodyPlat.isEmpty()){
				   String [] header = {"PlatBook", "PlatNo"};
				   
				   Map<String,String[]> map = new HashMap<String,String[]>();
				   map.put("PlatBook", new String[]{"PlatBook", ""});
				   map.put("PlatNo", new String[]{"PlatNo", ""});
				   
				   ResultTable pis = new ResultTable();	
				   pis.setHead(header);
				   pis.setBody(bodyPlat);		  
				   
				   ResultTable pisSTR = (ResultTable) m.get("PropertyIdentificationSet");
				   if (pisSTR != null){
					   pis = ResultTable.joinHorizontal(pis, pisSTR);
					   map.putAll(pisSTR.getMapRefference());
				   }
				   pis.setMap(map);
				   
				   m.put("PropertyIdentificationSet", pis);
			   }
			   
			   //	 extract cross refs from legal description
			   List<List> bodyCR = new ArrayList<List>();
			   p = Pattern.compile("\\b(OR)\\s+(\\d+)\\s*[/-]\\s*(\\d+)\\b");
			   ma = p.matcher(legal);	      	   
			   while (ma.find()){
				   List<String> line = new ArrayList<String>();		   
				   line.add(ma.group(2));
				   line.add(ma.group(3));
				   line.add("");
				   bodyCR.add(line);		   
			   } 
			   p = Pattern.compile("\\b(OR)\\s+(\\d+) PGS? (\\d+(?:-\\d+)?)\\b");
			   ma = p.matcher(legal);	      	   
			   while (ma.find()){
				   List<String> line = new ArrayList<String>();		   
				   line.add(ma.group(2));
				   line.add(ma.group(3));
				   line.add("");
				   bodyCR.add(line);		   
			   } 
			   if (!StringUtils.isEmpty((String)m.get("SaleDataSet.Remarks"))){
				   bodyCR = new ArrayList<List>(); //because we don't parse crossrefs from legal
				   String legal2 = (String)m.get("SaleDataSet.Remarks");
				   p = Pattern.compile("\\bCFN\\s*(\\d+)\\b");
				   ma = p.matcher(legal2);	      	   
				   while (ma.find()){
					   List<String> line = new ArrayList<String>();		   
					   line.add("");
					   line.add("");
					   line.add(ma.group(1));
					   bodyCR.add(line);		   
				   }
				   p = Pattern.compile("^\\s*(\\d+)/(\\d+)\\s*$");
				   ma = p.matcher(legal2);	      	   
				   while (ma.find()){
					   List<String> line = new ArrayList<String>();		   
					   line.add(ma.group(1));
					   line.add(ma.group(2));
					   line.add("");
					   bodyCR.add(line);		   
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
				   m.put("CrossRefSet", cr); //- DO NOT PARSE CROSSREF FROM LEGAL
			   }	  
			   
			   // extract tract 
			   String tract = ""; // can have multiple occurrences 
			   p = Pattern.compile("\\bTR(?:ACT)?S? (\\d+(\\s*[&,\\s]\\s*\\d+)*|\\w(\\s*[&,\\s-]\\s*\\w)*|\\w\\w)\\b");
			   ma = p.matcher(legal);
			   while (ma.find()){
				   tract = tract + " " + ma.group(1);		   
			   }
			   tract = tract.trim();
			   tract = tract.replaceAll("[&,]", " ");
			   tract = tract.replaceAll("\\s{2,}", " ");
			   if (tract.length() != 0){
				   tract = LegalDescription.cleanValues(tract, false, true);
				   m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			   }
			   
			   // extract building #
			   String bldg = "";
			   p = Pattern.compile("\\b(?:BLDG|BUILDING) (\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?)\\b");
			   ma.usePattern(p);
			   ma.reset();
			   while (ma.find()){
				   bldg = bldg + " " + ma.group(1);		   
			   }
			   bldg = bldg.trim();
			   if (bldg.length() != 0){
				   bldg = LegalDescription.cleanValues(bldg, false, true);
				   m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			   }
			   	   	   
			   
			   // extract phase from legal description
			   p = Pattern.compile("\\bPH(?:ASE)?((?:\\s*[&,\\s-]\\s*(?:\\d+\\w?(?!-)|\\b[A-Z]\\b))+)\\b");
			   ma.usePattern(p);
			   ma.reset();
			   if (ma.find()){
				   String phase = ma.group(1).trim();
				   if ("I".equals(phase)){
					   phase = "1";
				   }
				   phase = phase.replaceAll("[&,]", " ").trim().replaceAll("\\s{2,}", " ");
				   phase = phase.replaceFirst("^0+(\\d+)", "$1");
				   m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			   }
			   
			   // extract lot from legal description
			   String lot = ""; // can have multiple occurrences	   
			   p = Pattern.compile(lotPatt);
			   ma.usePattern(p);
			   ma.reset();
			   while (ma.find()){
				   lot = lot + " " + ma.group(1);
			   }
			   lot = lot.trim();
			   lot = lot.replaceFirst("\\-$", "");
			   lot = lot.replace('/', '-');
			   lot = lot.replaceAll("[&,]", " ");	   
			   if (lot.length() != 0){
				   lot = LegalDescription.cleanValues(lot, false, true);
				   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			   }
			   
		       // extract unit from legal description
			   String unit = "";		   
			   p = Pattern.compile("\\b(?:UNITS?|APT)\\s+(\\d+(?:-?\\d*[A-Z])?(?:\\s*&\\s*\\d+)*(?:[/-]\\d+)?|[A-Z](?:-?\\d+[A-Z]?)?)\\b");
			   ma.usePattern(p);
			   ma.reset();
			   while (ma.find()){
				   unit = unit + " " +  ma.group(1).replaceFirst("^0+(\\w.*)", "$1").replaceFirst("-(?=\\d+[A-Z])", "@");		   
			   }
			   unit = unit.replaceAll("/", "-");
			   unit = unit.replaceAll("&", " ");
			   unit = unit.trim().replaceAll("\\s{2,}", " ");
			   if (unit.length() != 0){		  
				   unit = LegalDescription.cleanValues(unit, false, true);
				   unit = unit.replaceAll("@", "-");
				   m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
			   }
			   
			   // extract block from legal description
			   String block = ""; // can have multiple occurrences
			   p = Pattern.compile("\\bBL(?:OC)?KS?\\s+(\\d+[A-Z]?(?:\\s*[&,\\s-]\\s*\\d+)*)\\b(?![/-]\\d)");
			   ma = p.matcher(origLegal);
			   while (ma.find()){
				   block = block + " " + ma.group(1).replaceAll("[&,]", " ");
			   }
			   p = Pattern.compile("\\bBL(?:OC)?KS?\\s+([A-Z]{1,2}(?:\\s*[&,-]\\s*[A-Z]{1,2})*)\\b(?![/-]\\d)");
			   ma.usePattern(p);
			   ma.reset();
			   while (ma.find()){
				   block = block + " " + ma.group(1).replaceAll("[&,]", " ");
			   }
			   block = block.trim().replaceAll("\\s{2,}", " ");	   
			   if (block.length() != 0){
				   block = LegalDescription.cleanValues(block, false, true);
				   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
			   }
		   }	   
	   
}
