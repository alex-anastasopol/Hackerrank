package ro.cst.tsearch.servers.functions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLCollierRV {

	public static Pattern crossRefRemarks = Pattern.compile("ORB? (\\d+)\\s*PA?GE? (\\d+)");
	public static Pattern lotRemarks = Pattern.compile("\\bL\\-(\\d+)");
	public static Pattern blockRemarks = Pattern.compile("\\bB\\-(\\d+|\\w{1,2})\\b");
	public static Pattern unitRemarks = Pattern.compile("\\bUNI?T? (?:NO |# ?)?([\\d\\w\\-]+)\\b");
	
	public static void legalFLCollierDASLRV(ResultMap m, long searchId)
			throws Exception {

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0){
			return;
		}
		String remarks = (String)m.get("SaleDataSet.Remarks");
		if (remarks == null || !remarks.equals(legal)){
			//legal = legal.replaceAll("App Date:", "");
			String[] exceptionTokens = { "I", "M", "C", "L", "D" };
			legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers to arabics
			legal = GenericFunctions.replaceNumbers(legal);
			//legal = legal.replaceAll("\\bOR BK\\b", "ORB");
			//legal = legal.replaceAll("\\bLO\\b", "LOT");
			//legal = legal.replaceAll("\\bUNIT LOT\\b", "LOT");
			//legal = legal.replaceAll("(WD|QCD)", "");
			legal = legal.replaceAll("\\bLOT #(\\d+)\\b", "LOT $1");
			legal = legal.replaceAll("\\b(SECT|TRACT) BLK\\b", "BLK");
			legal = legal.replaceAll("\\bUNIT(\\d+)\\b", "UNIT $1");
			legal = legal.replaceAll("(\\d+)\\s*(?:THROUGH|TO)\\s*(\\d+)", "$1-$2");
			legal = legal.replaceAll("\\s*(&|\\bAND\\b)\\s*", " ");
			legal = legal.replaceAll("[WSNE] [\\.\\d]+FT", "");
			legal = legal.replaceAll("\\s*\\+\\s*", " ");
			legal = legal.replaceAll("&", "");
			legal = legal.trim();
			Pattern p;
			Matcher ma;
			// extract and replace tract from legal description
			String tract = (String) m.get("PropertyIdentificationSet.SubdivisionTract");
			if (tract == null) {
				tract = "";
			}
			String patt = "([A-Z0-9-]|\\d+ )";
			p = Pattern.compile("\\bTR(?:ACTS?)? (" + patt + "+)\\b");
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

			//section-township-range
			// extract section, township and range from legal description
			List<List> body = new ArrayList<List>();
			body = GenericFunctions.goToGetSTRFromMapFromPIS(m, body);
			ArrayList<Pattern> secs = new ArrayList<Pattern>();
			//secs.add(Pattern.compile("((?:\\d+ )+\\d+)/(\\d+)/(\\d+)"));
			secs.add(Pattern.compile("\\bSEC ((?:\\d+ )+\\d+)/(\\d+)/(\\d+)"));
			secs.add(Pattern.compile("\\bSEC ((?:\\d+ )+)(\\d+)/(\\d+)"));
			secs.add(Pattern.compile("\\b((?:\\d+-\\d+)? \\d+-\\d+)/(\\d+)/(\\d+)\\b"));
			secs.add(Pattern.compile("\\bSEC (\\d+[NWSE]?)(?:-)(\\d+[NWSE]?)(?:-)(\\d+[NWSE]?)\\b"));
			secs.add(Pattern.compile("\\b(\\d+[NWSE]*?)(?:/)(\\d+[NWSE]*?)(?:/)(\\d+[NWSE]*?)\\b"));
			secs.add(Pattern.compile("\\bSECT?(?:ION)? ([\\d\\w]+)()()"));
			secs.add(Pattern.compile("^(\\d+) (\\d+) (\\d+)\\b"));

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
			GenericFunctions2.goToSaveSTRInMap(m, body);

			//book and page
			Matcher bpFinal = crossRefRemarks.matcher(legal);
			;
			if (bpFinal.find()) {
				bpFinal.reset();
				List<List> bodyCR = new ArrayList<List>();
				while (bpFinal.find()) {
					List<String> line = new ArrayList<String>();
					line.add(bpFinal.group(1));
					line.add(bpFinal.group(2));
					line.add("");
					bodyCR.add(line);
				}
				ResultTable cr = (ResultTable) m.get("CrossRefSet");
				if (cr == null) {
					String[] header = { "Book", "Page", "InstrumentNumber" };
					Map<String, String[]> map = new HashMap<String, String[]>();
					map.put("Book", new String[] { "Book", "" });
					map.put("Page", new String[] { "Page", "" });
					map.put("InstrumentNumber", new String[] { "InstrumentNumber",
							"" });
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
				bpFinal.reset();
				legal = bpFinal.replaceAll("|");
			}
			// extract and remove building # from legal description
			p = Pattern.compile("\\bB(?:UI)?LD(?:IN)?G? ([#A-Z\\-0-9]+)\\b");
			String building = (String) m.get("PropertyIdentificationSet.SubdivisionBldg");
			if (building == null) {
				building = "";
			}
			ma = p.matcher(legal);
			if (ma.find()) {
				building += " " + ma.group(1);
				building = building.replaceAll("#", "");
				m.put("PropertyIdentificationSet.SubdivisionBldg", building);
			}
			ma.reset();
			legal = ma.replaceAll("");

			//unit
			ma = unitRemarks.matcher(legal);
			String unit = (String) m.get("PropertyIdentificationSet.SubdivisionUnit");
			if (unit == null) {
				unit = "";
			}
			while (ma.find()) {
				unit += " " + ma.group(1);
			}
			unit = unit.replaceAll("NO", "");
			unit = LegalDescription.cleanValues(unit, false, true);
			if (!"".equals(unit)) {
				m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
			}
			ma.reset();
			legal = ma.replaceAll("");

			//lot

			p = Pattern.compile("\\bL(?:O?TS?\\s*)?(?:NO )?([-\\d\\s,&+]+([A-Z])?)\\b");
			ma = p.matcher(legal);
			String lot = (String) m
					.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (lot == null) {
				lot = "";
			}
			while (ma.find()) {
				lot += " " + ma.group(1);
			}
			lot = lot.replaceAll("\\+", "");
			//lot = lot.replaceAll("^-", "");
			lot = lot.replaceAll("-$", "");
			lot = LegalDescription.cleanValues(lot, false, true);
			if (!"".equals(lot)) {
				m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			}
			ma.reset();
			legal = ma.replaceAll("");

			//block
			p = Pattern.compile("\\bB(?:L?(?:OC)?KS?\\s*)?((\\d+,?|\\b[A-Z]{1,2}\\b|[A-Z]\\b|-|\\s|\\+)+)");
			ma = p.matcher(legal);
			String block = (String) m
					.get("PropertyIdentificationSet.SubdivisionBlock");
			if (block == null) {
				block = "";
			}
			while (ma.find()) {
				block += " " + ma.group(1).replaceAll("^-", "").replaceAll("-$", "").replaceAll("\\s*\\+\\s*", " ").trim();
			}
			block = block.replaceAll("\\b(OF|EL)\\b", "");


			block = LegalDescription.cleanValues(block, false, true);
			if (!"".equals(block)) {

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
			p = Pattern.compile("\\bPH(?:ASES?)?(( (\\d+\\w?|\\b[A-Z]\\b))+)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				phase += " " + ma.group(1).trim();
				phase = LegalDescription.cleanValues(phase, false, true);
				m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			}
		}
		//done saving
		legalRemarksFLCollierRV(m, searchId);

	}
	
	public static void legalRemarksFLCollierRV(ResultMap m, long searchId) throws Exception {
		   
		   String legal = (String) m.get("SaleDataSet.Remarks");

		   if (legal == null || legal.length() == 0)
			   return;
		   
		   legal = legal.replaceAll("(\\d+\\s*(?:ND|RD|TH|ST))", " $1");
		   legal = legal.replaceAll("(\\d+/\\d+)", "$1 ");
		   legal = legal.replaceAll("\\bUNS SEE\\b", "UNS");
		   legal = legal.replaceAll("\\bUNS & TRS\\b", "TRS");
		   legal = legal.replaceAll("(\\d+)\\s*THRU\\s*(\\d+)", "$1-$2");
		   legal = legal.replaceAll("(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");
		   
		   	//initial corrections and cleanup of legal description
		   legal = GenericFunctions.replaceNumbers(legal);
		   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
		   legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); 
		   
		   String legalTemp = legal;
		   			   
		   // extract lot from legal description
		   String lot = ""; // can have multiple occurrences
		   Pattern p = Pattern.compile("(?is)\\b(LO?TS?=?)\\*?\\s*(\\d+ & \\d+|[\\d&-\\.\\s]+|(?:\\d+[A-Z]\\d?))\\b");
		   Matcher ma = p.matcher(legalTemp);
		   while (ma.find()){
			   lot = lot + " " + ma.group(2);
		   }
		   lot = lot.replaceAll("\\.", " ");
		   lot = lot.replaceAll("&", " ");
		   lot = lot.trim();
		   if (lot.length() != 0){
			   String lotFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			   if (!StringUtils.isEmpty(lotFromSet)){
				   lot = lotFromSet + " " + lot;
			   }
			   lot = LegalDescription.cleanValues(lot, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot.trim());
		   }
		   ma.reset();
		   legalTemp = ma.replaceAll("");
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
		   
		   // extract tract from legal description
		   String tract = (String)m.get("PropertyIdentificationSet.SubdivisionTract");
		   if (tract == null){
			   tract = "";
		   }
		   p = Pattern.compile("(?is)\\b(TR(?:ACT)?S?=?)\\s*(\\d+ & \\d+|\\d+| [A-Z])\\b");
		   ma = p.matcher(legalTemp);
		   if (ma.find()){
			   tract = tract + " " + ma.group(2);
			   tract = tract.replaceAll("&", " ");
		   }
		   ma.reset();
		   legalTemp = ma.replaceAll(""); 
		   p = Pattern.compile("(?is)\\bTRACT=([A-Z])\\b");
		   ma = p.matcher(legalTemp);
		   if (ma.find()){
			   tract = tract + " " + ma.group(1);
			   tract = tract.replaceAll("&", " ");
		   }
		   ma.reset();
		   legalTemp = ma.replaceAll(""); 
		   tract = tract.trim();
		   if (tract.length() != 0){
			   tract = LegalDescription.cleanValues(tract, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionTract", tract.trim());
		   }
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
		   
		   // extract block from legal description
		   String block = "";
		   p = Pattern.compile("(?is)\\b(BL(?:OC)?K?S?)=?\\s*(\\d+|(?:[A-Z]&[A-Z]?)|(?:\\d+[A-Z])|(?:[A-Z]))\\b");
		   ma = p.matcher(legalTemp);
		   if (ma.find()){
			   block = ma.group(2).replaceAll("\\s*,\\s*", " ");
			   if (block.trim().equals("CH"))
				   block = block.replaceAll("CH", "");// for BCH that means BEACH
			   String blockFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			   if (!StringUtils.isEmpty(blockFromSet)){
				   block = blockFromSet + " " + block;
				   block = LegalDescription.cleanValues(block, false, true);
			   }
			   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		   }
		   ma.reset();
		   legalTemp = ma.replaceAll("");
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
		   
		   // extract cross refs from legal description
		   p = Pattern.compile("(?is)\\b(OR)\\s*(\\d+)\\s*(PG)\\s*(\\d+)");
		   ma = p.matcher(legalTemp);
		   List<List> bodyCR = new ArrayList<List>();
	   	   
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();
			   if(ma.group(2)==null)
				   line.add("");
			   else
				   line.add(ma.group(2));
			   if(ma.group(4)==null)
				   line.add("");
			   else
				   line.add(ma.group(4));
			   line.add("");
			   line.add("");
			   bodyCR.add(line);
		   } 
		   GenericFunctions.saveCRInMap(m, bodyCR);
		   ma.reset();
		   legalTemp = ma.replaceAll("");
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
		   	   
		   // extract phase from legal description
		   String phase = (String)m.get("PropertyIdentificationSet.SubdivisionPhase");
		   if (phase == null){
			   phase = ""; 
		   }		   
		   p = Pattern.compile("(?is)\\b(PH(?:ASE=)?)\\s*([\\dA-Z&]+)\\b");
		   ma = p.matcher(legalTemp);
		   if (ma.find()){
			   phase += " " + ma.group(2).replaceAll("\\s*,\\s*", " ");
			   phase = LegalDescription.cleanValues(phase, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionPhase", phase.trim());
			   ma.reset();
			   legalTemp = ma.replaceAll("");
		   }
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
		  
		   // extract unit from legal description
		   String unit = (String) m.get("PropertyIdentificationSet.SubdivisionUnit");
		   if (unit == null){
			   unit = "";
		   }
		   p = Pattern.compile("(?is)\\b((UN(?:IT)?S?(?: WEEK or WEEK| WKS?)?=?)\\s*([\\d\\w]+(\\s*[&-.,]\\s*\\w+)*))\\b");
		   ma = p.matcher(legalTemp);
		   if (ma.find()){
			   unit += " " + ma.group(3).replaceAll("\\s*,\\s*", " ");
			   unit = unit.replaceAll("&", " ");
			   unit = unit.replaceAll("\\.", " ");
			   unit = LegalDescription.cleanValues(unit, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionUnit", unit.trim());
			   ma.reset();
			   legalTemp = ma.replaceAll("");
		   }
		   
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
		      
		   // extract building #
		   p = Pattern.compile("(\\bB(?:UI)?L?D(?:IN)?G?=?\\s*(\\w+)\\b)");
		   ma = p.matcher(legalTemp);
		   String bldg = (String) m.get("PropertyIdentificationSet.SubdivisionBldg");
		   if (bldg == null){
			   bldg = "";
		   }
		   if (ma.find()){
			   bldg += " " + ma.group(2);
			   bldg = LegalDescription.cleanValues(bldg, false, true);
			   m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			   ma.reset();
			   legalTemp = ma.replaceAll("");
		   }
		   legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		   legal = legalTemp;
		  
		   // extract plat b&p
		   p = Pattern.compile("(?is)\\bPB\\s*(\\d+)\\s*PGS?\\s*([\\d+&-]+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.PlatBook", ma.group(1).trim());
			   m.put("PropertyIdentificationSet.PlatNo", ma.group(2).trim());
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "");
			   legal = legal.trim().replaceAll("\\s{2,}", " ");
		   }
		   
		   //extract section, township, range
		   List<List> body = GenericFunctions.getSTRFromMap(m);
		   List<String> line;
		   //String sec="", twp="", rng="";
		   ArrayList<Pattern> secs = new ArrayList<Pattern>();
		   secs.add(Pattern.compile("(?is)\\b(\\d+)\\s*-\\s*(\\d+)\\s*-\\s*(\\d+)"));
		   secs.add(Pattern.compile("(?is)Section=(\\d+[A-Z]?)\\s*Township=(\\d+[A-Z]?)\\s*Range=(\\d+[A-Z]?)"));
		   for(int i = 0; i<secs.size(); i++){
			   ma = secs.get(i).matcher(legalTemp);
			   while (ma.find()){		   
				   line = new ArrayList<String>(); 		   
				   line.add(ma.group(1));
				   line.add(ma.group(2));
				   line.add(ma.group(3));
				   body.add(line);
			   }
			   ma.reset();
			   legalTemp = ma.replaceAll("");
		   }
		   GenericFunctions.saveSTRInMap(m, body);
	   }
}
