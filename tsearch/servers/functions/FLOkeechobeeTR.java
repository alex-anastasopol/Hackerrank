package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.search.name.LastNameUtils;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameFactory;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

public class FLOkeechobeeTR 
  {
    public static void legalTokenizerFLOkeechobeeTR(ResultMap m, String legal) throws Exception {
    	
    	   //initial cleanup and correction of the legal description
      	legal = legal.replaceAll("(?<=[A-Z])\\.(?!\\d)", " ");
       	legal = legal.replaceAll("(\\d+)(?:ST|ND|RD|TH) SEC\\b", "SEC $1");
       	legal = legal.replaceAll("\\b(COR|\\d+)(BLK)(?= \\d)", "$1 $2");
       	legal = legal.replaceAll("([A-Z])(SUBD(?:IV)?)\\b", "$1 $2");
       	legal = legal.replaceAll("\\b(?:US )?GOVT? (LOTS?)\\b", "$1");
       	legal = legal.replaceAll("\\b(BLK \\d+)([A-Z]{2,})", "$1 $2");
       	legal = legal.replaceAll("\\bCOTS(?= \\d+\\b)", "LOTS");
       	legal = legal.replaceAll("(?<![A-Z])#\\s*(?=\\d|[A-Z]-?\\d)", "");
       	legal = legal.replaceAll("\\bINC\\b", "");
       	
       	legal = GenericFunctions.replaceNumbers(legal);
       	String origLegal = legal;
       	String[] exceptionTokens = {"I", "M", "C", "L", "D"};
       	legal = legal.replace("-", "__");
     	   	legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert roman numbers
     	   	legal = legal.replace("__", "-");
     	   	
       	legal = legal.replaceAll("\\sTHRU\\s", "-");
       	legal = legal.replaceAll("(?<=\\d) TO (?=\\d)", "-");
       	legal = legal.replaceAll("\\s{2,}", " ");
     	   	
     	   	// extract and remove sec-twn-rng from legal description
     	   	List<List> body = new ArrayList<List>();
     	   	Pattern p = Pattern.compile("^(\\d+(?:/\\d+)*)[-\\s](\\d+[SWEN]?)[-\\s](\\d+[SWEN]?)\\b(?:[A-Z\\s]*(?:\\s*-\\s*)?[\\d\\.]+[A-Z]{0,2}\\s*-\\s*\\(?(?:\\bMAP )?\\d+[A-Z]?\\d*\\)?)?");  	   	
     	   	Matcher ma = p.matcher(legal);	   
    	    if (ma.find()){
    	    	List<String> line = new ArrayList<String>();
    	    	line.add(ma.group(1).replaceAll("/", " "));
    	    	line.add(ma.group(2));
    	    	line.add(ma.group(3));
    	    	body.add(line);
    	    	legal = legal.replace(ma.group(0), ""); 
    	    }
    	    legal = legal.replaceFirst("\\s*-\\s*\\d+\\.\\d+[A-Z]?\\s*-", "");
    	    // extract and replace section from legal description
  		   String section = "";
  		   String township = "",township_aux="";
  		   String range = "",range_aux="";
  		   String ParcelID = (String)m.get("PropertyIdentificationSet.ParcelID");
 		   Pattern pPID = Pattern.compile("\\d-(\\d\\d)-(\\d\\d)-(\\d\\d)-");
  		   Matcher maPID = pPID.matcher(ParcelID);
  		   if (maPID.find()){
  			   //section = maPID.group(1) ;
  			   township_aux = maPID.group(2) + " ";
  			   range_aux = maPID.group(3) + " ";
  		   }  
    	   
    	    p = Pattern.compile("\\bSEC(?:TION)? (\\d+|[A-Z])\\b(?![\\.'/])");
    	    ma = p.matcher(legal);
   	    while (ma.find()){   
  		   section += " " + ma.group(1);  
  		  legal = legal.replace(ma.group(0), "SEC ");
  	    }
   	    section = LegalDescription.cleanValues(section, false, true);
   	    if (!section.equals(""))
   	    	m.put("PropertyIdentificationSet.SubdivisionSection", section);	
  		
   	    
   	    // extract and replace township from legal description
  	    p = Pattern.compile("\\bTOWN(?:SHIP)?\\s*(\\d+)\\s*(S(?:OUTH)?|N(?:ORTH)?|E(?:AST)?|W(?:EST)?)\\b");
  	    ma = p.matcher(legal);
  	    if (ma.find()) {
  	    	String dir = ma.group(2);
  	    	if (dir.equals("SOUTH")) dir = "S"; 
  	    	else
  	    		if (dir.equals("NORTH")) dir = "N";
  	    		else
  	    			if (dir.equals("EAST")) dir = "E";
  	    			else
  	    				if (dir.equals("WEST")) dir = "W"; 
  	       	
  	       township += township_aux + ma.group(1);
  	       township = LegalDescription.cleanValues(township, false, true) ; 
  	       township += " " + dir;
  	       m.put("PropertyIdentificationSet.SubdivisionTownship", township);
  	       legal = legal.replace(ma.group(0), "TOWN ");
  	    }   
  	    p = Pattern.compile("\\bR(?:ANGE)?\\s*(\\d+)\\s*(S(?:OUTH)?|N(?:ORTH)?|E(?:AST)?|W(?:EST)?)\\b");
  	    ma = p.matcher(legal);
  	    if (ma.find()) {
  	    	String dir = ma.group(2);
  	    	if (dir.equals("SOUTH")) dir = "S"; 
  	    	else
  	    		if (dir.equals("NORTH")) dir = "N";
  	    		else
  	    			if (dir.equals("EAST")) dir = "E";
  	    			else
  	    				if (dir.equals("WEST")) dir = "W"; 
  	       range += range_aux + ma.group(1);
  	       range = LegalDescription.cleanValues(range, false, true) ;
  	       range += " " + dir ;
  	       m.put("PropertyIdentificationSet.SubdivisionRange", range);
  	       legal = legal.replace(ma.group(0), "RANGE ");
  	    }
  	 //extragerea section township range din PID (in caz ca nu am LOT si/sau BLOCK
  	    if (section.equals("")){
  	    	section = maPID.group(1) ;
  	    	m.put("PropertyIdentificationSet.SubdivisionSection", section);
  	    }
  	    if (township.equals(""))
  	    {
  	    	m.put("PropertyIdentificationSet.SubdivisionTownship", township_aux);
  	    }
  	    if (range.equals(""))
  	    {
  	    	m.put("PropertyIdentificationSet.SubdivisionRange", range_aux);
  	    }
  	    
        legal = legal.replaceAll("\\s{2,}", " ").trim();
    	    // extract and remove cross refs from legal description
     	   	List<List> bodyCR = new ArrayList<List>();
     	   	p = Pattern.compile("\\b(ORB|DB)?[\\s\\.]?(\\d+)\\s?P(?:GS?)?[\\s\\.]?(\\d+(?:\\s*[&,/-]\\s*\\d+)*)\\b");
     	   	ma = p.matcher(legal);
     	   	Pattern pTemp = Pattern.compile("((\\d+)\\d)\\s*&\\s*(\\d)");
     	   	Matcher maTemp;
     	   	while (ma.find()){
     	   		List<String> line = new ArrayList<String>();			   
      			line.add(ma.group(2)); 		// book
      			String page = ma.group(3); 	// page
      			maTemp = pTemp.matcher(page);			// ORB 1473 P 1378 & 9 must be transformed in ORB 1473 P 1378 & 1379 (PID 03281-000-000)
      			if (maTemp.matches()){
      				page = maTemp.group(1) + " " + maTemp.group(2) + maTemp.group(3);
      			}
      			page = page.replaceAll("\\s*[&,/]\\s*", " ");
      			line.add(page);
      			String bType = ma.group(1);
      			if (bType == null) 
      				bType = "ORB";
      			line.add(bType);		// book type
      			bodyCR.add(line);
      			legal = legal.replace(ma.group(0), ""); 
     	   	}
     	   	p = Pattern.compile("\\b(ORB|DB)\\s?(\\d+) (\\d+(?:\\s*[&,/-]\\s*\\d+)*)\\b");
   	   	ma = p.matcher(legal);
   	   	while (ma.find()){
   	   		List<String> line = new ArrayList<String>();			   
    			line.add(ma.group(2)); 		// book
    			String page = ma.group(3); 	// page
    			maTemp = pTemp.matcher(page);			
    			if (maTemp.matches()){
    				page = maTemp.group(1) + " " + maTemp.group(2) + maTemp.group(3);
    			}
    			page = page.replaceAll("\\s*[&,/]\\s*", " ");
    			line.add(page);
    			line.add(ma.group(1));		// book type
    			bodyCR.add(line);
    			legal = legal.replace(ma.group(0), ""); 
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
     	   	}
     	   	legal = legal.replaceFirst("\\s*\\bORB( \\d+)?\\s*$", "");
     	   	legal = legal.replaceAll("\\s{2,}", " ");
    	   	   	   
     	   	// extract and replace lot from legal description
     	   	String lot = ""; // can have multiple occurrences
     	   	// \bLOT(?:S)?\s+([A-Z]+(?:\s*&\s*[A-Z]+)?|[\d&\s,-]+)\b
     	   	// p = Pattern.compile("\\bL(?:OTS? |-)(\\d+[A-Z]?(?:\\s*[,&-]\\s*\\d+)*|[A-Z](?:\\d+|[IVX]+)?(?:-\\d+)?(?:\\s*[&,]\\s*[A-Z])*)\\b(?!')");
     	   	p = Pattern.compile("\\bLOT(?:S)?\\s+([A-Z]+(?:\\s*&\\s*[A-Z]+)?|[\\d&\\s,-]+)\\b");
     	   	ma = p.matcher(legal);
     	   	while (ma.find()){
     	   		String lotTemp = ma.group(1);
     	   		lotTemp = lotTemp.replaceAll("\\s*[,&]\\s*", " ");
     	   		lot = lot + " " + lotTemp;
     	   		legal = legal.replace(ma.group(0), "LOT ");
     	   	}
     	   	lot = lot.trim();
     	   	if (lot.length() != 0){
     	   		lot = LegalDescription.cleanValues(lot, false, true);
     	   		m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
     	   	}
     	   	legal = legal.trim().replaceAll("\\s{2,}", " ");
    	   	   	   	   
    	   // extract and replace block from legal description - extract from original legal description
    	   String block = "";
    	   String blkPattern = "\\bB(?:LK?|LOCK)\\s(\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?)\\b(?!')";
    	   p = Pattern.compile(blkPattern);
    	   ma = p.matcher(origLegal);
    	   while (ma.find()){
    		   block = block + " " + ma.group(1);
    		   legal = legal.replaceFirst(blkPattern, "BLK ");
    	   } 
    	   p = Pattern.compile("\\bB(\\d+)\\b");
   	   ma = p.matcher(legal);
   	   while (ma.find()){
   		   block = block + " " + ma.group(1);
   		   legal = legal.replace(ma.group(0), "BLK ");
   	   } 
    	   block = block.trim();
    	   if (block.length() != 0){
    		   block = LegalDescription.cleanValues(block, false, true);
    		   m.put("PropertyIdentificationSet.SubdivisionBlock", block);
    		   legal = legal.trim().replaceAll("\\s{2,}", " ");
    	   }	   
    	   
    	   // extract and replace unit from legal description
    	   String unit = "";
    	   p = Pattern.compile("\\b(?:U(?:NIT |-|#)|APT )(\\d+(?:-?[A-Z])?(?:-\\d+[A-Z]?)?|[A-Z](?:-?\\d+)?(\\s*&\\s*[A-Z](?:-?\\d+)?)*(?:-\\d+)?)\\b");
    	   ma = p.matcher(legal);
    	   while (ma.find()){
    		   unit = unit + " " + ma.group(1).replaceAll("\\s*&\\s", " ");
    		   legal = legal.replace(ma.group(0), "UNIT "); 		   
    	   }
    	   unit = unit.trim();
    	   if (unit.length() != 0){
    		  unit = LegalDescription.cleanValues(unit, false, true);
    		  m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
   		  legal = legal.trim().replaceAll("\\s{2,}", " ");
    	   }
    	   	   
    	   // extract and replace building # from legal description
    	   p = Pattern.compile("\\b(?:BLD(?:G)?|BUILDING)\\s(\\d+|[A-Z](?:-?\\d+)?)\\b");
    	   ma = p.matcher(legal);
    	   if (ma.find()){
    		   m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
    		   legal = legal.replace(ma.group(0), "BLDG ");
    		   legal = legal.trim().replaceAll("\\s{2,}", " ");
    	   }
    	   
    	   // extract and replace phase from legal description
    	   p = Pattern.compile("\\bPH(?:ASE)? (\\d+(?:-?[A-Z])?|I)\\b");
    	   ma = p.matcher(legal);
    	   if (ma.find()){
    		   m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(1).replaceFirst("\\bI", "1"));
    		   legal = legal.replace(ma.group(0), "PHASE ");
    		   legal = legal.trim().replaceAll("\\s{2,}", " ");
    	   }
    	   
    	   // extract and replace tract from legal description
    	   String tract = "";
    	   p = Pattern.compile("\\bTRACT (\\d+)\\b");
    	   ma = p.matcher(legal);
    	   while (ma.find()){ 		
    		   tract = tract + " " + ma.group(1); 		   
    		   legal = legal.replace(ma.group(0), "TRACT ");
    	   }
    	   tract = tract.trim();
    	   tract = LegalDescription.cleanValues(tract, true, true);
    	   if (tract.length() != 0){
    		   tract = LegalDescription.cleanValues(tract, true, true);
    		   m.put("PropertyIdentificationSet.SubdivisionTract", tract);
   		   legal = legal.trim().replaceAll("\\s{2,}", " ");
    	   } 	    	   
    	   	   	 	   	    	
    	   legal = legal.replaceAll("\\bRE[\\s-]?PLAT\\b", "PLAT");
    	   legal = legal.replaceAll("\\b(RE[-\\s]?)?SUB(-?D(IV(ISION)?)?)?\\b", "SUBD");
    	   legal = legal.replaceAll("\\bCONDOMINIUMS?\\b", "CONDO");
    	   legal = legal.replaceAll("\\bUNR(E?C(ORDED)?)?\\b", "UNREC");
    	   
    	   // extract subdivision name - only if lot, block or unit was extracted, or the legal contains PLAT, SUBD or CONDO token
    	   if (lot.length() != 0 || block.length() != 0 || unit.length() != 0 || legal.matches(".*\\b(PLAT|SUBD|CONDO|CO BEG)\\b.*")){
    	   
   	 	   // first perform additional cleaning 
   	 	   legal = legal.replaceAll("\\s*\\b(FOR|TO) PO(B|R)\\b", "");
   	 	   legal = legal.replaceAll("\\bCORRECTED\\b\\s*", "");
   	 	   legal = legal.replaceAll("\\b(BEG|COM|COR)\\b.*?(\\s*\\b(LOT|BLK|UNIT))+\\b", "");
   	 	   legal = legal.replaceAll(".*\\b(KNOWN|KWN) AS\\b.*?(\\s*\\b(LOT|BLK))+\\b", "");
   	 	   legal = legal.replaceAll("(\\s*& )?(LESS )?(?<!/)(?<!\\bTH )\\b([SWEN] )?[SWEN]{1,2}(LY)?\\s?[\\d\\.]+'( & [\\d\\.]+')?( TO [SWEN] R/W\\b.*)?", "");
   	 	   legal = legal.replaceAll("\\b(TH (CONT )?[SWEN]{1,2})\\s?[\\d\\.]+'", "$1");
   	 	   legal = legal.replaceAll("\\b[SWEN]{1,2}\\s*\\d+[/\\.]\\d+( OF)?\\b", "");
   	 	   legal = legal.replaceAll("^\\d+(?:ST|ND|RD|TH) (ADD(ITION)?|AMENDED( PLAT)?)( TO)?\\s+", "");
   	 	   legal = legal.replaceAll("^(PLAT|SUBD)( OF)?\\s+", "");	 	   
   	 	   legal = legal.replaceAll("\\s{2,}", " ").trim();
   	 	   
   	 	   String subdiv = "";
   	 	   p = Pattern.compile("(.*?)\\s*(?:(\\bOF )?\\b(?:LOT|BLK|(?:IN )?SEC|UNIT|PLAT|SUBD|(?:\\d+(?:ST|ND|RD|TH) )?ADD(?:ITION)?|TH\\s*(?:\\bCONT\\b\\s*)?(?:\\b[SWEN](?:LY)?|\\bLFT \\d+|$)|UNREC|(?:(?<!-)A )?CONDO|PHASE|TRACT)\\b.*|\\d+'|$)");
   	 	   ma = p.matcher(legal);
   	 	   if (ma.find()){
   	 		   subdiv = ma.group(1);		   
   	 	   } 
   	 	   if (subdiv.length() == 0) {	  
   	 		   p = Pattern.compile("LOT (.*) UNREC");  //PID 07365-010-000, 32788-000-000
   	 		   ma.usePattern(p);
   	 		   ma.reset();
   	 		   if (ma.matches()){
   	 			   subdiv = ma.group(1);
   	 		   }   
   	 	   }
   	 	   if (subdiv.length() == 0) {	  
   	 		   p = Pattern.compile("LOT BLK (.*)");  //PID 0195-000-000
   	 		   ma.usePattern(p);
   	 		   ma.reset();
   	 		   if (ma.matches()){
   	 			   subdiv = ma.group(1);
   	 		   }   
   	 	   }
   	 	   if (subdiv.length() == 0) {	  
   	 		   p = Pattern.compile("(.+ CO) BEG\\b.*");  //PID 08461-000-000, 08468-000-000
   	 		   ma.usePattern(p);
   	 		   ma.reset();
   	 		   if (ma.matches()){
   	 			   subdiv = ma.group(1);
   	 		   }   
   	 	   }
   	 	   subdiv = subdiv.trim();
   	 	   
   	 	   if (subdiv.length() != 0){
   	 		   subdiv = subdiv.replaceFirst("\\s*\\b[SWEN]{1,2}LY\\b.*", "");
   	 		   subdiv = subdiv.replaceFirst("\\s*\\b\\d+(?:ST|ND|RD|TH) AD\\s*$", "");
   	 		   subdiv = subdiv.replaceFirst("\\s*\\bM(AP)?[\\s-]*\\d+\\s*$", "");
   	 		   subdiv = subdiv.replaceFirst("\\s*\\b(M(AP)?[\\s-]*)?\\d+-?[A-Z]\\d*-?\\s*$", "");
   	 		   subdiv = subdiv.replaceFirst("\\s*\\(?[A-Z]\\.\\d+\\)?\\s*,?$", "");
   	 		   subdiv = subdiv.replaceFirst("\\s*\\(?\\d+(\\.\\d+)?(-[A-Z])?\\)?\\s*,?$", "");
   	 		   subdiv = subdiv.replaceFirst("\\s*\\b(M(AP)?[\\s-]*)?\\d+-?[A-Z]\\d*-?\\s*$", "");
   	 		   subdiv = subdiv.replaceFirst("^\\s*THE\\s*$", "");
   	 		   subdiv = subdiv.replaceFirst("^\\s*-\\s*", "");
   	 		   subdiv = subdiv.replaceFirst("\\s*(\\b(I|ORIG|BEG|TH)\\b|[-,\\(])\\s*$", "");	 		   
   	 		   subdiv = subdiv.trim();
   	 		   if (subdiv.length() != 0){
   	 			   m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
   	 			   if (legal.matches(".*\\bCONDO\\b.*"))
   	 				   m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
   	 		   }
   	 	   }
   	 	   //else  m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
    	   }
       }    
    
    protected static String cleanupNamesFLOkeechobeeTR(String s)
    {
  	s = s.replaceAll("\\(([A-Z\\s]+)\\)", "$1");
  	s = s.replaceAll("[\\.\\(\\)]", ", ");
  	s = s.replaceAll("\\bC/P\\b", "C/O");    	
  	s = s.replaceFirst("([A-Z\\s-]+, [A-Z\\s-]+)\\s*(?:\\bAND\\b|\\bOR\\b|/)\\s*([A-Z\\s-]+)", "$1 & $2");
  	s = s.replaceFirst(",? EST\\.?(ATE)?\\s*$", "ESTATE");
  	s = s.replaceFirst(",? ESQUIRE\\s*$", "");
  	s = s.replaceFirst("\\bMRS\\s*$", "");
  	s = s.replaceFirst(" WIFE\\b", "");
  	if (!s.contains("ESTATE"))
  		s = s.replaceFirst("\\bLIFE\\b","LIFE ESTATE");
  	s = StringFormats.unifyNameDelim(s);
  	
  	 s = s.replaceFirst("@@((?:P\\s*O )?BOX?)?\\s*\\d+.*","");	
  	 		//PIN: 1-06-36-34-0010-00400-0190  -> SMITH ROSALIND GAIL, AVENA-LYN | P O BO 785
  	 		//PIN: 1-15-37-35-0030-00030-0070  -> WILLIAMS ENID |BOX 374									
  	 s = s.replaceFirst("\\s*\\b(AS )?(CO-)?TRUSTEES?\\s*$", "");
       s = s.replaceFirst("\\s*,\\s*$", ""); 
       s = s.replaceFirst("\\A([A-Z,\\s*]+)\\bTR\\b(\\s*[A-Z\\s&-]+)\\Z","$1,$2"); // pt cazuri de genul BROWN LOIS E TR WILLIAMS JOHN (2 nume despartite prin TR)
       s = s.replaceFirst("[,\\s*]+(TR(?:U|USTEE|USTEES)?)\\b"," $1");
       //******* cazuri particulare ********
       s = s.replaceAll("([A-Z]),([A-Z])\\s*,","$1 $2 ,");	//1-27-34-36-0A00-00005-0000		SCHIRARD J,L, SMITH V,SMITH C, |	LINGLE G
       s = s.replaceAll("([A-Z])\\s*\\.\\s*([A-Z]+)","$1 , $2");   //1-06-38-35-0A00-00002-0000		TAYLOR V, PURVIS M. SMITH B 
       //******* cazuri particulare ********
       //s = s.replaceAll("&@@"," & ");
       s = s.replaceAll("(?:,@@|&@@)"," , ");
       s = s.replaceAll("@@"," "); 
       s = s.replaceFirst("C/O\\s([-A-Z]+(?:\\s*[A-Z])?)\\s([-A-Z]+)",", $2 $1"); 
       		//PIN: 1-29-37-35-0A00-00024-0000    WILLIAMS EVA MAE TRUST | C/O WANDA WOLFORD TRUSTEE   (WOLFORD = Last se interschimba cu First (si Midle dc exista)
       		//PIN: 1-33-34-36-0A00-00002-0000    SMITH RANCH & GROVE LC | C/O VERNON D SMITH			- || -           - || -
       if (!s.contains("LC"))		//caz de exceptie -> PIN:1-33-34-36-0A00-00002-0000		SMITH RANCH & GROVE LC
       {
    	 //B3628, PID: 1-11-34-33-0A00-00003-K000  RAPHAEL VICTOR & ANNA MARIE
    	   String s1 = s, p1 = "", p2 = "";
    	   s1 = s1.replaceFirst("(?is).*&\\s(([-A-Z]+)\\s([A-Z][A-Z]+(?:[A-Z])?)).*", "$1");
    	   p1 = s1;  p2 = s1;
    	   p1 = p1.replaceFirst("([-A-Z]+)\\s([A-Z][A-Z]+(?:[A-Z])?)", "$1");
    	   p2 = p2.replaceFirst("([-A-Z]+)\\s([A-Z][A-Z]+(?:[A-Z])?)", "$2");
    	   if (!(FirstNameUtils.isFirstName(p1)) && (FirstNameUtils.isFirstName(p2)))
    		   s = s.replaceFirst("&\\s([-A-Z]+\\s[A-Z][A-Z]+(?:[A-Z])?)",", $1");    // PIN:1-09-34-33-0A00-00027-B000	JOHNSON BRUCE & SMITH-JOHNSON PAULETTE
    	   else
    		   s = s.replaceFirst("(?is)([A-Z-]{3,})(.*)&\\s([-A-Z]+\\s[A-Z][A-Z]+(?:[A-Z])?)(.*)", "$1 $2, $1 $3"); 
       }
       
       s= s.replaceAll("\\s{2,}", " ");
      	//b4287
       s= s.replaceFirst("(\\d+.*AVEN?U?E?)|(\\d+.*CT)", "");
   	   s= s.replaceFirst("(?i)(PANAMA CITY|OKEECHOBEE),\\sFL\\s\\d*-?\\d*", "");
  	 return s.trim();
    }
  
  public static void ownerNameTokenizerFLOkeechobeeTR (ResultMap m, String owner) throws Exception {

	       String s = owner;
	       s = cleanupNamesFLOkeechobeeTR(s);  

	       if (!NameUtils.isCompany(s)) 
	       {
	    	if (s.matches("\\A([A-Z]{2,})\\s*[A-Z]+(?:[A-Z])?\\s*&\\s*([A-Z]+)\\s*&\\s*([A-Z]+)\\Z"))  
	    		  //PIN: 1-31-37-36-0010-00070-0090 -> EDWARDS LUTHER & JOYCE & SMITH
	    	  {
	    		 String s1 = s;
	    		 s1 = s1.replaceFirst("\\A([A-Z]{2,})\\s*[A-Z]+(?:[A-Z])?\\s*&\\s*([A-Z]+)\\s*&\\s*([A-Z]+)\\Z", "$3");
	    		 if (LastNameUtils.isLastName(s1))  
	    			 s = s.replaceFirst("\\A([A-Z]{2,}\\s*[A-Z]+(?:[A-Z])?\\s*&\\s*[A-Z]+)\\s*&\\s*([A-Z]+)\\Z","$1, $2");
	    	  }
	    	else
	    	if (s.matches("\\A([A-Z]{2,})\\s*[A-Z]+(?:[A-Z])?\\s*,\\s*([A-Z]+)\\s*&\\s*([A-Z]+)\\Z"))
	    	{
	    		String s1 = s;
	    		s1 = s1.replaceFirst("\\A([A-Z]{2,}\\s*[A-Z]+(?:[A-Z])?)\\s*,\\s*([A-Z]+)\\s*&\\s*([A-Z]+)\\Z","$3");
	    		if (FirstNameUtils.isFirstName(s1))
	    			s = s.replaceFirst("\\A([A-Z]{2,})\\s*([A-Z]+(?:[A-Z])?)\\s*,\\s*([A-Z]+\\s*&\\s*[A-Z]+)\\Z","$1 $2, $1 $3");
	    	}
	    	else
	    	if (s.matches("\\A[A-Z'-]+\\s*[A-Z]+\\s*(?:[A-Z]+\\s*(?:JR|SR)?)?\\s*&\\s*([A-Z-]{3,}\\s*)([A-Z]+\\s*(?:[A-Z]+)?)?\\Z") && (!(s.contains(" JR") || s.contains(" SR"))))
	    	{
	    		String s1 = s;
	    		s1 = s1.replaceFirst(".*\\s*&\\s*([A-Z-]{3,}\\s*)([A-Z]+\\s*(?:[A-Z]+)?)?","$1");
	    		if (!FirstNameUtils.isFirstName(s1))
	    			s = s.replaceFirst("(.*)\\s*&\\s*(.*)","$1 , $2");
	    	}
	       }
	       String[] owners = s.split(",");
	      
		   List<List> body = new ArrayList<List>();
		   String[] names = {"", "", "", "", "", ""};
		   String[] suffixes;
		    
		  	for (int i=0; i<owners.length; i++)
		  	  {
		  		boolean lifeEst = false;
		  		String ow = owners[i], aux="";
		  		if (ow.matches(".*\\b((?:LIFE\\s*)?ESTATE)\\b\\s*"))
		  		{
		  			aux = ow;
		  			aux = aux.replaceAll(".*\\b((?:LIFE\\s*)?ESTATE)\\b","$1");
		  			lifeEst = true;
		  		}
		  		if (lifeEst) 				// in parseNameNashville se stergea ESTATE sau LIFE ESTATE
		  			names[2] = owners[i];
		  		else
		  			names = StringFormats.parseNameNashville(ow);
		  			
		  		suffixes = GenericFunctions2.extractNameSuffixes(names);        
		        GenericFunctions2.addOwnerNames(names, suffixes[0], suffixes[1], NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		  	  }
		  	GenericFunctions2.storeOwnerInPartyNames(m, body);    	 
  }    
  }