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
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.utils.Roman;

public class FLJacksonRV {
	public static Pattern crossRefRemarks = Pattern.compile("B-(\\d+)\\s*P\\-(\\d+)");
	public static Pattern crossRefRemarks2 = Pattern.compile("(\\d{3,3})(\\s+|\\-)(\\d{2,3})");

	public static Pattern lotRemarks = Pattern.compile("\\bL\\-(\\d+)");
	public static Pattern blockRemarks = Pattern.compile("\\bB\\-(\\d+|\\w{1,2})\\b");
	public static Pattern unitRemarks = Pattern.compile("\\bU\\-(\\d+)\\b");
	public static void legalRemarksFLJacksonRV(ResultMap m, long searchId)
			throws Exception {
		
		String legal = (String) m.get("SaleDataSet.Remarks");
		if (legal == null || legal.length() == 0)
			return;

		legal = legal.replaceAll("(WD|QCD)", "");
		legal = legal.replaceAll("\\s{2,}", " ");
		legal = legal.trim();
		//section-township-range
		   // extract section, township and range from legal description
		   List<List> body = new ArrayList<List>();
		   Pattern p = Pattern.compile("\\b(\\d+[NWSE]*?)(\\-|\\s)(\\d+[NWSE]*?)(\\-|\\s)(\\d+[NWSE]*?)\\b");
		   Matcher ma = p.matcher(legal);	   	   	   	  
		   while (ma.find()){
			   List<String> line = new ArrayList<String>();
			   line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			   line.add(ma.group(3).replaceFirst("^0+(\\d+)", "$1"));
			   line.add(ma.group(5).replaceFirst("^0+(\\d+)", "$1"));
			   body.add(line);
			   legal = legal.replaceFirst("\\b"+ma.group(0)+"\\b", "SEC ");
		   } 
		   p = Pattern.compile("\\bSEC(?:TION)?S? (\\d+(?:-?[A-Z])?(?: \\d+)?)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   List<String> line = new ArrayList<String>();
			   line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			   line.add("");
			   line.add("");
			   body.add(line);
		   }
		   if (!body.isEmpty()){
			   String [] header = {"SubdivisionSection", "SubdivisionTownship", "SubdivisionRange"};
			   
			   Map<String,String[]> map = new HashMap<String,String[]>();
			   map.put("SubdivisionSection", new String[]{"SubdivisionSection", ""});
			   map.put("SubdivisionTownship", new String[]{"SubdivisionTownship", ""});
			   map.put("SubdivisionRange", new String[]{"SubdivisionRange", ""});
			   
			   ResultTable pis = new ResultTable();	
			   pis.setHead(header);
			   pis.setBody(body);
			   pis.setMap(map);
			   m.put("PropertyIdentificationSet", pis);
		   }
		   ma.reset();
		   legal = ma.replaceAll("");
		
		//book and page
		Matcher bp = crossRefRemarks.matcher(legal);
		Matcher bp2 = crossRefRemarks2.matcher(legal);
		String book = null;
		String page = null;
		if (bp.find()){
			book = bp.group(1);
			page = bp.group(2);
			
		} else if (bp2.find()){
			book = bp2.group(1);
			page = bp2.group(3);
		}
		if (book != null && page != null){
		   List<String> line = new ArrayList<String>();
		   line.add(book);
		   line.add(page);
		   line.add("");
		   ResultTable cr = (ResultTable)m.get("CrossRefSet");
		   List<List> bodyCR = new ArrayList<List>();
		   bodyCR.add(line);
		   if (cr == null){
			   String [] header = {"Book", "Page", "InstrumentNumber"};		   
			   Map<String,String[]> map = new HashMap<String,String[]>();		   
			   map.put("Book", new String[]{"Book", ""});
			   map.put("Page", new String[]{"Page", ""});
			   map.put("InstrumentNumber", new String[]{"InstrumentNumber", ""});
			   cr = new ResultTable();	
			   cr.setHead(header);
			   cr.setBody(bodyCR);
			   cr.setMap(map);		   
			   
		   } else {
			   String[][] b = cr.getBodyRef();
			   for (int i= 0; i< b.length; i++){
				   line = new ArrayList<String>();
				   line.add(b[i][0]);
				   line.add(b[i][1]);
				   line.add(b[i][2]);
				   bodyCR.add(line);
			   }
			   cr.setBody(bodyCR);
		   }
		   m.put("CrossRefSet", cr);
		}
		bp.reset();
		legal = bp.replaceAll("");
		
			int propInfNo = 0;
			   //lot
			   ma = lotRemarks.matcher(legal);
			   String lot = (String)m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			   if (lot == null){
				   lot = "";
			   }
			   while(ma.find()){
				   propInfNo++;
				   lot += " " + ma.group(1);
			   }
			   lot = LegalDescription.cleanValues(lot, false, true);
			   if (!"".equals(lot)){
				   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			   }
			   ma.reset();
			   legal = ma.replaceAll("");
			   
			   //block
		   ma = blockRemarks.matcher(legal);
		   String block = (String)m.get("PropertyIdentificationSet.SubdivisionBlock");
		   if (block == null){
			   block = "";
		   }
		   while(ma.find()){
			   propInfNo++;
			   block += " " + ma.group(1);
		   }
		   block = LegalDescription.cleanValues(block, false, true);
		   if (!"".equals(block)){
			   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		   }
		   ma.reset();
		   legal = ma.replaceAll("");
		   

		   
		   //unit
		   ma = unitRemarks.matcher(legal);
		   String unit = (String)m.get("PropertyIdentificationSet.SubdivisionUnit");
		   if (unit == null){
			   unit = "";
		   }
		   while(ma.find()){
			   propInfNo++;
			   unit += " " + ma.group(1);
		   }
		   unit = LegalDescription.cleanValues(unit, false, true);
		   if (!"".equals(unit)){
			   m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
		   }
		   ma.reset();
		   legal = ma.replaceAll("");
		   /*
		   //subdivision name
		   if ((String)m.get("PropertyIdentificationSet.SubdivisionName") == null){
			   ma = Pattern.compile("^(.*)(ADDITION|SUB|ADDIT)").matcher(legal);
			   if (ma.find()){
				   m.put("PropertyIdentificationSet.SubdivisionName", ma.group(1).trim());
			   } else if (propInfNo == 3){
				   //99% of remarks with L B U have the rest subDiv Name
				   m.put("PropertyIdentificationSet.SubdivisionName", legal.trim());
			   }
		   }
		   ma.reset();
		   legal = ma.replaceAll("");
		   */
		   //phase
		   // extract phase from legal description
		   p = Pattern.compile("\\bPH(?:ASES?)?(( (\\d+\\w?|\\b[A-Z]\\b))+)\\b");
		   ma = p.matcher(legal);
		   if (ma.find()){
			   m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(1).trim());
		   }
		   
	}

	/**
	 * easy way to save results
	 */
	public static void legalFLJacksonDASLRV(ResultMap m, long searchId, int a)
			throws Exception {

		if (m.get("tmpInte") != null) {
			return;
		}
		/*
		if (m.get("SaleDataSet.Remarks") != null){
			legalRemarksFLJacksonRV(m, searchId);
			return;
		}*/
		String pin = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (pin == null) {
			pin = (String) m.get("SaleDataSet.InstrumentNumber");
			if (pin==null){
				pin = "";
			}
		}
		String legal = (String) m.get("SaleDataSet.Remarks");
		if (legal == null || legal.length() == 0)
			return;
		boolean save = false;
		String o = legal;
		String filename = "fljacksonrv";
		String contain = ""; // if the same function is used for inter, then
								// what substring should be in o to save it
		if (m.get("saving") == null && save) {
			if (o.contains(contain)) {
				// String[] s = o.split("@@");
				// if (s.length>2 && (s[0].endsWith("&") || s[0].endsWith("AND")
				// || s[1].matches("(&)?( )?\\w+ \\w+( \\w+)?( \\w+)?"))){
				try {
					FileWriter fw = new FileWriter(new File(
							"/home/danut/Desktop/work/parsing/" + filename
									+ "/" + filename + "_legal.html"), true);
					fw.write(o + "\n");
					fw.write("\n=============================================================="
									+ pin + "\n");
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				//}
			}

		}
		//done saving
	}	
	
	public static void legalFLJacksonDASLRV(ResultMap m, long searchId)
			throws Exception { 
		   
		   String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		   String remarks = (String) m.get("SaleDataSet.Remarks");
		   if (legal == null || legal.length() == 0)
			   return;
		   if (!legal.equals(remarks)){
			   legal = legal.replaceAll("&", " ");
			   String origLegal = legal;
			   String[] exceptionTokens = {"I", "M", "C", "L", "D"};
			   legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers to arabics
			   legal = GenericFunctions.replaceNumbers(legal);
			   Matcher ma;
			   Pattern p;

			   // extract plat book & page from legal description
			   p = Pattern.compile("\\bPB (\\d+) PG (\\d+)\\b");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
				   m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
				   legal = ma.replaceAll("");
			   } else {
				   p = Pattern.compile("\\b(?:PLATS|PB|REC) (\\d+)-(\\d+)\\b");
				   ma = p.matcher(legal);
				   if (ma.find()){
					   m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
					   m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
					   legal = ma.replaceAll("");
				   } 
			   }
			  
			//	 extract cross refs from legal description
			   List<List> bodyCR = new ArrayList<List>();
			   p = Pattern.compile("\\b(OR|DB|BOOK)\\s+(\\d+)(\\s*P(AGE|G)?\\s*([\\d, \\-]+))?\\b");
			   ma = p.matcher(legal);
			   String docPage = (String)m.get("SaleDataSet.Page");
			   if (docPage == null){
				   docPage = "";
			   }
			   String docBook = (String)m.get("SaleDataSet.Book");
			   if (docBook == null){
				   docBook = "";
			   }
			   while (ma.find()){
				   String book = ma.group(2);
				   String page = ma.group(5);
				   if (page == null){
					   page = "";
				   }
				   //we might have OR 972 P 120,121
				   page = page.replaceAll(",$", "");
				   page = page.replaceAll("^,", "");
				   page = page.replaceAll("&", ",");
				   page = page.replaceAll("\\-.*", "");
				   page = page.trim();
				   page = page.replaceAll("\\s*,\\s*", ",");
				   String[] pages = page.split(",|\\s");
				   for (int jj = 0; jj< pages.length; jj++){
					   
					   if (!docBook.equals(book) && (pages[jj].equals("") || !docPage.equals(pages[jj]))){
						   List<String> line = new ArrayList<String>();
						   line.add(book);
					   	   line.add(pages[jj]);
					   	   line.add("");
					   	   bodyCR.add(line);
					   }					   
				   }
			   } 
			   ma.reset();
			   if (ma.find()){
				   legal = ma.replaceAll("");
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
			   
			   // extract lot from legal description
			   String lot = ""; // can have multiple occurrences
			   p = Pattern.compile("\\bLOTS?\\s+([-\\d\\s,&]+)");
			   ma = p.matcher(legal);
			   while (ma.find()){
				   lot = lot + " " + ma.group(1);

			   }
			   lot = lot.trim();
			   lot = lot.replaceFirst("\\-$", "");
			   if (lot.length() != 0){
				   lot = LegalDescription.cleanValues(lot, true, true);
				   m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			   }
			   ma.reset();
			   legal = ma.replaceAll("");

		       // extract unit from legal description
			   p = Pattern.compile("\\bUNITS?\\s+([-\\d\\s]+|\\d+-?[A-Z]|[A-Z\\d-]+)\\b");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(1).replaceFirst("^0+(\\w.*)", "$1"));
				   legal = ma.replaceAll("");
			   }
			   
			   // extract block from legal description
			   String block = ""; // can have multiple occurrences
			   p = Pattern.compile("\\bBL(?:OC)?KS?\\s+((\\d+,?|\\b[A-Z]{1,2}\\b|-|\\s)+)");
			   ma = p.matcher(legal);
			   String firstBlk = "";
			   while (ma.find()){
				   
				   String newBlk = ma.group(1).replaceAll("IN|OF.*", "");
				   if (block.length() == 0){
					   firstBlk = newBlk;
					   block = firstBlk;
				   }
				   else 
					   if (firstBlk.trim().matches("\\d+") && newBlk.matches(".*[A-Z].*")){
						   
					   } else {
						   block = block + " " + newBlk;
					   }
			   }
			   block = block.trim();
			   block = block.replaceFirst("\\-$", "");
			   if (block.length() != 0){
				   block = LegalDescription.cleanValues(block, false, true);
				   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
			   }
			   ma.reset();
			   legal = ma.replaceAll("");
			   /*
			   // extract subdvision name
			   String subdiv = "";
			   legal = legal.replaceAll("\\b(BEING|BEG(IN)?|COMM).*", "");
			   legal = legal.replaceAll("^\\s*([SWNE]+\\d+/\\d+)*\\s*(OF)?\\s*", "");
			   p = Pattern.compile("(?is)^(.*?)\\b(SUB\\-?DI?V|ADDN?|(UN)?RECORDED)(\\b|\\.)");
			   ma = p.matcher(legal);
			   if (ma.find()){
				   subdiv = ma.group(1);
				   if (subdiv == null || subdiv.contains("/")){
					   subdiv = "";
				   } 
			   } else {
				   p = Pattern.compile("\\bBLK [\\d\\w]+ (.*?)\\s*(UNIT|SUB|ADDN)(\\b)");
				   ma = p.matcher(origLegal);
				   if (ma.find()){
					   subdiv = ma.group(1);
				   } else {
					   p = Pattern.compile("\\bLOT [\\d]+(.*?) (UNIT|SUBD)\\b");
					   ma = p.matcher(origLegal);
					   if (ma.find()){
						   subdiv = ma.group(1);
						   subdiv = subdiv.replaceAll("BLK [\\d\\w]+", "");
					   }
				   }
			   }
				   
			   
			   subdiv = subdiv.trim();
			   if (subdiv.length() != 0){
				   subdiv = subdiv.replaceAll(",", "");
				   subdiv = subdiv.replaceAll(".*\\bIN\\s", "");
				   subdiv = subdiv.replaceAll("\\.", "");
				   subdiv = subdiv.trim();
			   }
			   if (subdiv.length() != 0){
				   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
				   if (legal.matches(".*\\bCO?NDO(MINIUM)?\\b.*")){
					   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
				   }
			   }*/
		   } 
		   legalRemarksFLJacksonRV(m, searchId);
	   }
	
	public static void writeNames(ResultTable rt, String pin, String type) {
		String filename = "fljacksonrv";

		try {
			FileWriter fw = new FileWriter(new File(
					"/home/danut/Desktop/work/parsing/" + filename + "/"
							+ filename + "_names.html"), true);
			String[][] b = rt.getBodyRef();
			for(int i = 0; i < b.length; i++){
				fw.write(type + "@@" +b[i][0] + "@@" + b[i][1] + "@@" + b[i][2] + "@@" + b[i][3] + "\n");
			}
			fw.write("\n===============================================" + pin + "\n");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public static String getPin(ResultMap m){
		String pin = (String)m.get("PropertyIdentificationSet.ParcelID");
		if (pin == null){
			pin = (String) m.get("SaleDataSet.InstrumentNumber");
			if (pin==null){
				pin = "";
			}
		}
		return pin;
	}

}
