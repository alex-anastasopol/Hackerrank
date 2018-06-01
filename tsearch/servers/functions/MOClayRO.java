package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.GenericFunctions2;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author mihaib
*/

public class MOClayRO {
	
	private static final Pattern BOOK_PAGE_PAT = Pattern.compile("(?is)Book\\s*:\\s*(\\d+)\\s*Page\\s*:\\s*(\\d+)");
	
	
	public static void parseAndFillResultMap(ServerResponse response, String rsResponse, ResultMap m, long searchId) {
		
		try {
			m.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), "RO");
			
			rsResponse = rsResponse.replaceAll("&nbsp;", " ");
			
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(rsResponse, null);
			NodeList mainList = htmlParser.parse(null);
			
			String instrumentNo = HtmlParser3.getValueFromNextCell(mainList, "Document No.", "", false).trim();
			m.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), instrumentNo.trim());
			//m.put(SaleDataSetKey.DOCUMENT_NUMBER.getKeyName(), instrumentNo.trim());
			String docType = HtmlParser3.getValueFromAbsoluteCell(-1, 1, HtmlParser3.findNode(mainList, "Document No."), "", true).trim();
			//docType = docType.replaceAll("(?is)[^-]+-", "").trim();
			docType = docType.replaceAll("(?is)\\s+", " ").trim();
			m.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType);
			String instrDate = HtmlParser3.getValueFromNextCell(mainList, "Dated date", "", false).trim();
			instrDate = instrDate.replaceAll("(?is)([\\d/]+).*", "$1");
			m.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(), instrDate.trim());
			String recordedDate = HtmlParser3.getValueFromNextCell(mainList, "Recording Date", "", false).trim();
			recordedDate = recordedDate.replaceAll("(?is)([\\d/]+).*", "$1");
			m.put(SaleDataSetKey.RECORDED_DATE.getKeyName(), recordedDate.trim());
			
			String consAmount = HtmlParser3.getValueFromNextCell(mainList, "Referenced Amount", "", false).trim();//1993015558
			m.put(SaleDataSetKey.CONSIDERATION_AMOUNT.getKeyName(), consAmount.trim().replaceAll("[\\$,]+", ""));
			m.put(SaleDataSetKey.MORTGAGE_AMOUNT.getKeyName(), consAmount.trim().replaceAll("[\\$,]+", ""));
			
			String book = HtmlParser3.getValueFromAbsoluteCell(1, 1, HtmlParser3.findNode(mainList, "Document No."), "", true).trim();
			String page = HtmlParser3.getValueFromAbsoluteCell(2, 1, HtmlParser3.findNode(mainList, "Document No."), "", true).trim();
			
			m.put(SaleDataSetKey.BOOK.getKeyName(), book.trim());
			m.put(SaleDataSetKey.PAGE.getKeyName(), page.trim());
			
			String tableLegals = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Legal Description(s)"), "", true).trim();
			List<List<String>> listLegals = HtmlParser3.getTableAsList(tableLegals, true);
			String legal = "";
			for (List<String> list : listLegals){
				for (String item : list){
					if (StringUtils.isNotEmpty(item)){
							legal += "@@" + item.trim(); 
					}
				}
			}
			if (StringUtils.isNotEmpty(legal)){
				m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), legal.replaceAll("(?is)\\A@@", "").trim());
			}
			
			String refsdByThisDoc = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Referenced By This Document"), "", true).trim();
			String refsToThisDoc = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "References To This Document"), "", true).trim();
			
			@SuppressWarnings("rawtypes")
			List<List> bodyCR = new ArrayList<List>();
			List<String> line = new ArrayList<String>();
			
			if (StringUtils.isNotEmpty(refsdByThisDoc)){				
				String[] crBP = refsdByThisDoc.split("(?is)\\s*</?br\\s   * /?>\\s*");
				for (String bp : crBP){
					Matcher mat = BOOK_PAGE_PAT.matcher(bp);
					if (mat.find()){
						line = new ArrayList<String>();
						line.add(mat.group(1));
						line.add(mat.group(2));
						line.add("");
						bodyCR.add(line);
					}
				}
			}
			if (StringUtils.isNotEmpty(refsToThisDoc)){				
				String[] crBP = refsToThisDoc.split("(?is)\\s*</?br\\s*/?>\\s*");
				for (String bp : crBP){
					Matcher mat = BOOK_PAGE_PAT.matcher(bp);
					if (mat.find()){
						line = new ArrayList<String>();
						line.add(mat.group(1));
						line.add(mat.group(2));
						line.add("");
						bodyCR.add(line);
					}
				}
			}
			String comments = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Comments"), "", true).trim();
			if (StringUtils.isNotEmpty(comments)){
				m.put("tmpComments", comments);
				Pattern bookPageFromCommPat = Pattern.compile("(?is)\\bBK\\s*(\\d+)\\s*PG\\s*(\\d+)");
				Matcher mat = bookPageFromCommPat.matcher(comments);
				while (mat.find()){
					boolean isAlready = false;
					line = new ArrayList<String>();
					line.add(mat.group(1));
					line.add(mat.group(2));
					line.add("");
					if (bodyCR.isEmpty()){
						bodyCR.add(line);
					} else {
						for (List<String> lst : bodyCR){
							if (lst.equals(line)){
								isAlready = true;
								break;
							}
						}
						if (!isAlready){
							bodyCR.add(line);
						}
					}
				}
			}
			
			if (!bodyCR.isEmpty()){
				String[] header = { "Book", "Page", "InstrumentNumber" };
				ResultTable rt = GenericFunctions2.createResultTable(bodyCR, header);
				m.put("CrossRefSet", rt);
			}
			
			String names[] = {"", "", "", "", "", ""};
			String[] suffixes, type, otherType;
			@SuppressWarnings("rawtypes")
			ArrayList<List> grantor = new ArrayList<List>();
			@SuppressWarnings("rawtypes")
			ArrayList<List> grantee = new ArrayList<List>();
			
			String grantorString = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Grantor(s)"), "", true).trim();
			
			if (StringUtils.isNotEmpty(grantorString)){
				grantorString = grantorString.replaceAll("(?is)\\bDeceased\\b", "").trim();
				grantorString = grantorString.replaceAll("(?is)\\b(AS\\s+NOMINEE|FKA)\\b", "").trim();
				grantorString = grantorString.replaceAll("(?is)\\bDBA\\b", "      ").trim();
				grantorString = grantorString.replaceAll("(?is)\\bC/O\\b", "      ").trim();
				grantorString = grantorString.replaceAll("(?is)&nbsp;", " ").trim();
				grantorString = grantorString.replaceAll("&amp;", "&");
				grantorString = grantorString.replaceAll("(?is)\\bLINEAL DESCENDANTS? PER STIRPES?\\b", "&");
				
				String[] gtors = grantorString.split("(?is)\\s*</?br\\s*/?>\\s*");
				
				for (int i = 0; i < gtors.length; i++){
					gtors[i] = gtors[i].trim();
					
					gtors[i]=StringFormats.cleanNameMERS(gtors[i]);
					names = StringFormats.parseNameNashville(gtors[i], true);
					
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
					
					GenericFunctions.addOwnerNames(gtors[i], names, suffixes[0],
													suffixes[1], type, otherType,
													NameUtils.isCompany(names[2]),
													NameUtils.isCompany(names[5]), grantor);
				}
				grantorString = grantorString.replaceAll("(?is)\\s*</?br\\s*/?>\\s*$", "").replaceAll("(?is)\\s*</?br\\s*/?>\\s*", " / ");	
				m.put(SaleDataSetKey.GRANTOR.getKeyName(), grantorString);
				m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
			}
				
			String granteeString = HtmlParser3.getValueFromAbsoluteCell(1, 0, HtmlParser3.findNode(mainList, "Grantee(s)"), "", true).trim();
			
			if (StringUtils.isNotEmpty(granteeString)){
				granteeString = granteeString.replaceAll("(?is)\\bDeceased\\b", "").trim();
				granteeString = granteeString.replaceAll("(?is)\\b(AS\\s+NOMINEE|FKA)\\b", "").trim();
				granteeString = granteeString.replaceAll("(?is)\\bDBA\\b", "      ").trim();
				granteeString = granteeString.replaceAll("(?is)\\bC/O\\b", "      ").trim();
				granteeString = granteeString.replaceAll("(?is)&nbsp;", " ").trim();
				granteeString = granteeString.replaceAll("&amp;", "&");
				granteeString = granteeString.replaceAll("(?is)\\bLINEAL DESCENDANTS? PER STIRPES?\\b", "&");
				
				String[] gtee = granteeString.split("(?is)\\s*</?br\\s*/?>\\s*");
				
				for (int i = 0; i < gtee.length; i++){
					gtee[i] = gtee[i].trim();

					names = StringFormats.parseNameNashville(gtee[i], true);
					
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
					
					GenericFunctions.addOwnerNames(gtee[i], names, suffixes[0],
							suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), grantee);
				}
				
				granteeString = granteeString.replaceAll("(?is)\\s*</?br\\s*/?>\\s*$", "").replaceAll("(?is)\\s*</?br\\s*/?>\\s*", " / ");
				m.put(SaleDataSetKey.GRANTEE.getKeyName(), granteeString);
				m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
			}
			GenericFunctions1.setGranteeLanderTrustee2(m, searchId,true);

			
			try {
				legalMOClayRO(m, searchId);
				GenericFunctions1.setPlatBookPage(m, searchId);
				GenericFunctions1.differentDocNoMOClayRO(m, searchId);
			}catch(Exception e) {
				e.printStackTrace();
			}
						
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static void parseNameInterMOClayRO(ResultMap m, long searchId) throws Exception{
		
			String names[] = {"", "", "", "", "", ""};
			String[] suffixes, type, otherType;
			ArrayList<List> grantor = new ArrayList<List>();
			ArrayList<List> grantee = new ArrayList<List>();
			
			String tmpPartyGtor = (String)m.get("tmpPartyGtor");
			if (StringUtils.isNotEmpty(tmpPartyGtor)){
				tmpPartyGtor = tmpPartyGtor.replaceAll("\\sDBA\\s+", " / ");
				String[] gtors = tmpPartyGtor.split("/");
				for (String grantorName : gtors){
					grantorName = grantorName.replaceAll("\\bDECEASED\\b", "");
					
					names = StringFormats.parseNameNashville(grantorName, true);
					
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
					
					GenericFunctions.addOwnerNames(grantorName, names, suffixes[0],
													suffixes[1], type, otherType,
													NameUtils.isCompany(names[2]),
													NameUtils.isCompany(names[5]), grantor);
				}
				
				m.put(SaleDataSetKey.GRANTOR.getKeyName(), tmpPartyGtor);
				m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantor, true));
			}
			
			String tmpPartyGtee = (String)m.get("tmpPartyGtee");
			if (StringUtils.isNotEmpty(tmpPartyGtee)){
				tmpPartyGtee = tmpPartyGtee.replaceAll("\\sDBA\\s+", " / ");
				String[] gtee = tmpPartyGtee.split("/");
				for (String granteeName : gtee){
					granteeName = granteeName.replaceAll("\\bDECEASED\\b", "");
					
					names = StringFormats.parseNameNashville(granteeName, true);
					
					type = GenericFunctions.extractAllNamesType(names);
					otherType = GenericFunctions.extractAllNamesOtherType(names);
					suffixes = GenericFunctions.extractNameSuffixes(names);
					
					GenericFunctions.addOwnerNames(granteeName, names, suffixes[0],
							suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]),
							NameUtils.isCompany(names[5]), grantee);
				}
				
				m.put(SaleDataSetKey.GRANTEE.getKeyName(), tmpPartyGtee);
				m.put("GranteeSet", GenericFunctions.storeOwnerInSet(grantee, true));
				
			}
			
			GenericFunctions1.setGranteeLanderTrustee2(m, searchId,true);
		
	}
	
	public static void lotClayRO(ResultMap m,long searchId) throws Exception {
    	String subdivFullText = (String) m.get(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName());
    	String[] lotAndBlock = getLotAndBlock(subdivFullText);
    	m.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), lotAndBlock[0]);
    	m.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), lotAndBlock[1]);
    }
	
	public static String[] getLotAndBlock(String subdivFullText) {
		/*
    	 * parse lot / lots numbers from full subdivision string
    	 */
		if(subdivFullText == null){
    		subdivFullText = "";
    	}
    	
    	subdivFullText = subdivFullText.replaceAll("(?i)thru", "-");
    	
    	Matcher lotNumberMatcher = GenericFunctions1.clayROLotPattern.matcher(subdivFullText);
    	String commaSepparatedLotNumbers = "";
    	while(lotNumberMatcher.find()){
    		String possibleLotNumberString = lotNumberMatcher.group(1);
    		
    		possibleLotNumberString = possibleLotNumberString.replaceAll("&" , ",");
    		
    		String[] lotNumbers = possibleLotNumberString.split(",");
    		for(int i = 0; i < lotNumbers.length; i++){
    			String lotNumber = lotNumbers[i].trim();
    			if(lotNumber.contains("-")){
    				//group
    				//must determine the two boundaries and generate all possible variants
    				String[] range = lotNumber.split("-");
    				int rangeBegin = -1;
    				int rangeEnd = -1;
    				try{
    					rangeBegin = Integer.parseInt(range[0].trim());
    					rangeEnd = Integer.parseInt(range[1].trim());
    				}catch(Exception e){
    					
    				}
    				
    				if(rangeBegin != -1 && rangeEnd != -1){
    					for(; rangeBegin <= rangeEnd; rangeBegin ++){
    						commaSepparatedLotNumbers += rangeBegin + ",";
    					}
    				}
    			}
    			else {
    				commaSepparatedLotNumbers += lotNumber + ",";
    			}
    		}
    	}
    	
    	Matcher blockNumberMatcher = GenericFunctions1.clayROBlockPattern.matcher(subdivFullText);
    	String commaSepparatedBlockNumbers = "";
    	while(blockNumberMatcher.find()){
    		String possibleBlockNumberString = blockNumberMatcher.group(1);
    		
    		possibleBlockNumberString = possibleBlockNumberString.replaceAll("&" , ",");
    		
    		String[] blockNumbers = possibleBlockNumberString.split(",");
    		for(int i = 0; i < blockNumbers.length; i++){
    			String blockNumber = blockNumbers[i].trim();
    			if(blockNumber.contains("-")){
    				//group
    				//must determine the two boundaries and generate all possible variants
    				String[] range = blockNumber.split("-");
    				int rangeBegin = -1;
    				int rangeEnd = -1;
    				try{
    					rangeBegin = Integer.parseInt(range[0].trim());
    					rangeEnd = Integer.parseInt(range[1].trim());
    				}catch(Exception e){
    					
    				}
    				
    				if(rangeBegin != -1 && rangeEnd != -1){
    					for(; rangeBegin <= rangeEnd ; rangeBegin ++){
    						commaSepparatedBlockNumbers += rangeBegin + ",";
    					}
    				}
    			}
    			else {
    				commaSepparatedBlockNumbers += blockNumber + ",";
    			}
    		}
    	}    	
    	
    	String lotNr = LegalDescription.cleanValues(commaSepparatedLotNumbers, false, true);
    	String lotBlock = commaSepparatedBlockNumbers;
    	return new String[] { lotNr, lotBlock };
	}
	
	public static void legalMOClayRO(ResultMap m,long searchId) throws Exception{
        ResultTable pis = (ResultTable) m.get("PropertyIdentificationSet");
        if (pis == null)
        	return;
	
        String[][] body = pis.getBody();
        m.put(PropertyIdentificationSetKey.PROPERTY_DESCRIPTION.getKeyName(), body[0][0]);
        
        int len = pis.getLength();        
        int last = len-1;
    
        // try to identify if a legal description row from the legal description table contains 2 legals - ex. RO CLAY Instr# S92529
        String pattern = "(?:\\bLO?TS?\\b.+?)*(?:\\bBLO?KS?\\b.+?)?(?:\\bSEC\\b.+?)?\\b[\\w&,\\s-]+\\b";
        Pattern p = Pattern.compile("(.*?"+pattern+")\\s&\\s(\\D*"+pattern+".*)");
        Pattern pattStr = Pattern.compile("\\bSECS?\\s(\\d+(?:[&,\\s]+\\d+)*)-(\\d+)-(\\d+)"); 
        for (int i=0; i<len; i++) {        	
        	String subdiv = body[i][0];
        	Matcher ma = p.matcher(subdiv);
            // if there are 2 legals in one legal description table row, parse each of them and add them to legals table on separate rows        	
        	if(ma.find()) {
        		String legal1 = ma.group(1);
        		String legal2 = ma.group(2);
        		if (!legal1.matches(".*\\b\\d+(ST|ND|RD|TH)?") || !legal2.matches("\\d+(ST|ND|RD|TH)?\\b.*")){        		            	
	        		last ++;
	        		
	        		String[][] new_body = new String[last+1][7];
	        		System.arraycopy(body, 0, new_body, 0, last);
	        		body = new_body;
	
	        		body[i][0] = legal1;
	        		body[last][0] = legal2;
	        		body[i][1] = StringFormats.SubdivisionMOClayRO(legal1);	 	//subdivision name
	        		body[last][1] = StringFormats.SubdivisionMOClayRO(legal2);	
	        		body[i][2] = StringFormats.LotMOClayRO(legal1);				//lot
	        		body[last][2] = StringFormats.LotMOClayRO(legal2);
	        		body[i][3] = StringFormats.BlockMOClayRO(legal1);			//block
	        		body[last][3] = StringFormats.BlockMOClayRO(legal2);
	        		// section, township, range
	        		Matcher maStr1 = pattStr.matcher(legal1);
	        		Matcher maStr2 = pattStr.matcher(legal2);
	        		if (maStr1.find()){
	        			body[i][4] = maStr1.group(1).replaceAll("\\s*[&,]\\s*", " ");        			 
	        			body[i][5] = maStr1.group(2);
	        			body[i][6] = maStr1.group(3);
	        		} else {
	        			body[i][4] = "";
	        			body[i][5] = "";
	        			body[i][6] = "";
	        		}
	        		if (maStr2.find()){
	        			body[last][4] = maStr2.group(1).replaceAll("\\s*[&,]\\s*", " ");
	        			body[last][5] = maStr2.group(2);
	        			body[last][6] = maStr2.group(3);
	        		} else {
	        			body[last][4] = "";
	        			body[last][5] = "";
	        			body[last][6] = "";
	        		}
	        		pis.setReadOnly(false);
	        		pis.setBody(body);
	        		pis.setReadOnly();
        		}
        	}
        }	
        
	    m.put("PropertyIdentificationSet", pis);
    }  
	
    
}
