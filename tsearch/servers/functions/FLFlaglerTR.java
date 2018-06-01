
package ro.cst.tsearch.servers.functions;

import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.tags.TableRow;
import org.htmlparser.util.NodeList;
import org.htmlparser.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.utils.Roman;

public class FLFlaglerTR {
	
	public static ResultMap parseIntermediaryRow(String recordHtml, long searchId) {
		
		ResultMap resultMap = new ResultMap();
		resultMap.put("OtherInformationSet.SrcType", "TR");
		
		try {
			org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(recordHtml, null);
			NodeList nodeList = htmlParser.parse(null);
			
			Node table = nodeList
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "98%"), true)
				.elementAt(0);
			Node link = nodeList
				.extractAllNodesThatMatch(new TagNameFilter("a"), true)
				.elementAt(0);
			Node legal = nodeList
				.extractAllNodesThatMatch(new TagNameFilter("table"), true)
				.extractAllNodesThatMatch(new HasAttributeFilter("width", "98%"), true)
				.elementAt(1);
			
			String pid = link.toPlainTextString().split("\\s+")[0];
			if(pid.length() < 19) {
				resultMap.put("PropertyIdentificationSet.PropertyType", "Tangible");
			} else {
				resultMap.put("PropertyIdentificationSet.PropertyType", "Real Estate");
			}
			resultMap.put("PropertyIdentificationSet.ParcelID", pid);
			
			String ownerInfo = table.toPlainTextString()
				.replaceAll("&nbsp;", "")
				.replaceAll("\\s*\n\\s*", "\n")
				.trim();
			parseNames(resultMap, ownerInfo, searchId);
			
			String[] ownerArray = ownerInfo.split("\n");
			for(String eachLine:ownerArray) {
				if(eachLine.matches("\\d+\\s+.*") && eachLine.indexOf("2 BROTHERS LAWN SERVICE") < 0) {
					parseAddress(resultMap, eachLine, searchId);
					break;
				}
			}
			
			parseLegal(resultMap, legal.toPlainTextString().trim());
		} catch(Exception e) {
			
		}
		
		return resultMap;
	}
	
	public static void parseNames(ResultMap resultMap, String ownerInfo, long searchId) throws Exception {
		
		if(ownerInfo == null || ownerInfo.matches("\\s*")) {
			return;
		}
		
		Vector<String> companyExpr = new Vector<String>();
		companyExpr.add("ADAMS & ADAMS");
		
		Vector<String> excludeWords = new Vector<String>();
		
		ownerInfo = fixLines(ownerInfo);
		String[] ownerLines = ownerInfo.split("\\s*\n\\s*");
		String[] nameLines = GenericFunctions.removeAddressFLTR(ownerLines,
				excludeWords, new Vector<String>(), 2, Integer.MAX_VALUE);
		
		for(int i = 0; i < nameLines.length; i++) {
			nameLines[i] = cleanName(nameLines[i]);
		}
		nameLines = processName(nameLines, companyExpr, "(&)|(C/O)");
		for(int i = 0; i < nameLines.length; i++) {
			nameLines[i] = nameLines[i].replaceAll("\\A\\s*&\\s*", "").replaceAll("\\s*&\\s*\\z", "").trim();
		}
		
		COFremontAO.genericParseNames(resultMap, nameLines, null, companyExpr, 
				false, COFremontAO.FIRST_NAME_LF, searchId);
		
	}		
	
	//merge people names split on 2 lines, split company names placed on the same line
	public static String[] processName(String[] lines, Vector<String> companyExpr, String separatorRegex) {

		ArrayList<String> newLines = new ArrayList<String>();
		String crtLine = "";
		
		for(String line:lines) {
			line = " " + line + " ";
			String[] parts = line.split(separatorRegex);
			if(COFremontAO.isCompany(line, companyExpr)) {
				if(crtLine.length() > 0) {
					newLines.add(crtLine);
				}
				boolean allAreCompanies = true;
				for(String part:parts) {
					if(part.trim().split("\\s+").length <= 1 || !COFremontAO.isCompany(part, companyExpr)) {
						allAreCompanies = false;
						break;
					}
				}
				if(allAreCompanies) {
					for(String part:parts) {
						if(part.length() > 0) {
							newLines.add(part);
						}
					}
				} else {
					newLines.add(line);
				}
				crtLine = "";
			} else {
				String firstPart = parts[0].trim();
				if(firstPart.matches("\\w{2,}(\\s+\\w)*") || crtLine.matches("\\w{2,}(\\s+\\w)*")) {
					newLines.add(crtLine + " " + firstPart);
					crtLine = "";
				} else {
					if(crtLine.length() > 0) {
						newLines.add(crtLine);
					}
					crtLine = firstPart;
				}
				for(int i = 1; i < parts.length; i++) {
					if(crtLine.length() > 0) {
						newLines.add(crtLine);
					}
					crtLine = parts[i];
				}			
			}
		}
		if(crtLine.length() > 0) {
			newLines.add(crtLine);
		}
		
		return newLines.toArray(new String[newLines.size()]);
	}
	
	private static String cleanName(String name) {
		
		String ownerInfo = name;
		
		if(ownerInfo.indexOf("BOARD OF TRUSTEES") < 0) {  // BOARD OF TRUSTEES OF THE INTERNAL IMPROVEMENT TRUST
			ownerInfo = ownerInfo.replaceAll("(CO)?-?TRUSTEES?(\\s*OF)?", "");
		}
		ownerInfo = ownerInfo.replaceAll("-?JTWROS", ""); // Joint Tenants with Right of Survivorship
		ownerInfo = ownerInfo.replaceAll("H&W|W&H", "");
		ownerInfo = ownerInfo.replaceAll("[(].*[)]", "");
		ownerInfo = ownerInfo.replaceAll("\\bDR\\b", "");
		ownerInfo = ownerInfo.replaceAll("\\bTTEE\\b", "");
		ownerInfo = ownerInfo.replaceAll("\\bATTN:?", "");
		ownerInfo = ownerInfo.replaceAll("\\bFOR\\b", "");
		ownerInfo = ownerInfo.replaceAll(",?\\s*HIS\\s+WIFE", "");
		ownerInfo = ownerInfo.replaceAll("\\bET\\s*AL\\b", "");
		
		ownerInfo = ownerInfo.replaceAll("\\b*C/O\\b", "&");
		ownerInfo = ownerInfo.replaceAll("\\bAND\\b", "&");
		ownerInfo = ownerInfo.replaceAll("[0-9]*%", "&");
		
		ownerInfo = NameCleaner.cleanName(ownerInfo);
		
		return ownerInfo;
	}
	
	private static String fixLines(String ownerInfo) {
		
		ownerInfo = ownerInfo.replaceAll("(CONTEMPORARY\\s+MACH\\s+&)\\s*", "$1 ");  // CONTEMPORARY MACH & ENG SERVICE INC 
		ownerInfo = ownerInfo.replaceAll("\\b(MUNOS)\\s+(ESTELLA\\s+MUNOZ)\\b", "$1 & $2");
		ownerInfo = ownerInfo.replaceAll("\\bINTERNAL\\s*\n\\s*MEDICINE\\b", "INTERNAL MEDICINE");
//		ownerInfo = ownerInfo.replaceAll("\\bOF\\s*\n", "OF ");
		ownerInfo = ownerInfo.replaceAll("(BENNETT JR)\\s+(LIVING TRUST)", "$1 $2"); 
		ownerInfo = ownerInfo.replaceAll("\n\\s*TEMPORARY ADMINISTRATOR\\s*\n", "\n");
		ownerInfo = ownerInfo.replaceAll("(CALLE\\s+JOSE\\s+A)\\s+(MELIDA\\s+M)", "$1 & $2");
		ownerInfo = ownerInfo.replaceAll("CALLE\\s+(?!FATIMA|JOSE|MILTON).*\n", "CALLE 1 ");
		ownerInfo = ownerInfo.replaceAll("\n(\\s*LIFE\\s+ESTATE\\s*\n)", " $1");
		
		return ownerInfo;
	}
	
	public static void parseAddress(ResultMap resultMap, String address, long searchId) {
	
		if(address == null || address.matches("\\s*")) {
			return;
		}
		
		resultMap.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address.trim()));
		resultMap.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address.trim()));
	}
	
	public static void parseLegal(ResultMap resultMap, String legal) {
		
		Matcher m = Pattern.compile("(LOTS[\\s\\d-,]*\\d+\\s*-\\s*(\\d+))s*(-\\s*\\d+)").matcher(legal); // LOTS 17-18,18-19-20 
		while(m.find()) {
			legal = m.replaceFirst("$1,$2$3");
			m.reset(legal);
		}
		
		Matcher m1 = Pattern.compile("(LOTS[\\s\\d,]*\\d+)\\s*&\\s*(\\d+)").matcher(legal); // LOTS 15 & 16
		while(m1.find()) {
			legal = m1.replaceFirst("$1,$2");
			m1.reset(legal);
		}
		
		legal = legal.replaceAll("\\bSECTION\\b", "SEC");
		legal = legal.replaceAll("\\bBL-(\\d+)\\b", "BLK $1");
		legal = legal.replaceAll("\\bBL\\b", "BLK");
		legal = legal.replaceAll("(\\d+\\s*-\\s*(\\d+))s*(-\\s*\\d+)", "$1,$2$3");
		legal = legal.replaceAll("(\\d+)\\s*-\\s*(\\d+)", "$1 - $2");
		legal = legal.replaceAll("\\b(\\d+)OR\\b", "$1 OR");
		legal = legal.replaceAll("\\bUNIT\\s+#\\s*(\\d+)\\b", "UNIT $1");
		legal = legal.replaceAll("\\b0+([1-9]\\d*)\\b", "$1");  // LOT 00010
		legal = legal.replaceAll("\\b\\d+\\s*/\\s*\\d+\\s+INT\\b", "");
		legal = legal.replaceAll("\\A\\s*-\\s*", "");
		try {
			m = Pattern.compile("\\bPHASE\\s+([A-Z]+)\\b").matcher(legal);
			while(m.find()) {
				legal = m.replaceFirst("PHASE " + Roman.parseRoman(m.group(1)));
				m.reset(legal);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		try {
			// extract cross refs from legal description
		   List<List> bodyCR = new ArrayList<List>();
		   Pattern p = Pattern.compile("(OR)\\s+(?:BOOK\\s+)?(\\d+)\\s*(?:PG|PAGE|/)\\s*(\\d+)( - (\\d+))?\\b");
		   Matcher ma = p.matcher(legal);	      	   
		   while (ma.find()){
			   int fromPage = Integer.valueOf(ma.group(3));
			   int toPage = fromPage;
			   if(ma.group(5) != null) {  // e.g. OR 1391 PG 1646-1650
				   toPage = Integer.valueOf(ma.group(5));
			   }
			   if(toPage < fromPage) {
				   int aux = toPage;
				   toPage = fromPage;
				   fromPage = aux;
			   }
			   for(int i = fromPage; i <= toPage; i++) {
				   List<String> line = new ArrayList<String>();		   
				   line.add(ma.group(2));
				   line.add(String.valueOf(i));
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
			   resultMap.put("CrossRefSet", cr);
		   }
		
		   FLDuvalTR.legalFLDuvalTR(resultMap, legal);
		   
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}