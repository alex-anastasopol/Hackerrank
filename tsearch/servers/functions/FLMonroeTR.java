package ro.cst.tsearch.servers.functions;

import java.util.Vector;

import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameCleaner;

public class FLMonroeTR {
	
	public static void parseNames(ResultMap resultMap, String ownerCell) {
				
		Vector<String> companyExpr = new Vector<String>();
		Vector<String> excludeWords = new Vector<String>();
		
		String properInfo = ownerCell.trim().replaceFirst("(?is)\n\\s*\n.*", "").trim(); // fix for the case when "prior taxes due" is present in the owner info
		String[] nameLines = GenericFunctions.removeAddressFLTR(properInfo.split("\n"), excludeWords, new Vector<String>(), 2, Integer.MAX_VALUE);
		
		for(int i = 0; i < nameLines.length; i++) {
			nameLines[i] = cleanName(nameLines[i]);
		}
		nameLines = FLFlaglerTR.processName(nameLines, companyExpr, "&|C/O|\\bAND\\b");
		for(int i = 0; i < nameLines.length; i++) {
			nameLines[i] = nameLines[i].replaceAll("\\A\\s*&\\s*", "").replaceAll("\\s*&\\s*\\z", "").trim();
		}
		
		COFremontAO.genericParseNames(resultMap, nameLines, null, companyExpr, 
				false, COFremontAO.ALL_NAMES_LF, -1);
	}

	public static String cleanName(String name) {
		
		String ownerInfo = name;
		
		ownerInfo = ownerInfo.replaceAll("-?JTWROS", ""); // Joint Tenants with Right of Survivorship
		ownerInfo = ownerInfo.replaceAll("H&W|W&H|H/W", "");
		ownerInfo = ownerInfo.replaceAll("[(].*[)]", "");
//		ownerInfo = ownerInfo.replaceAll("\\bATTN:?", "");
		ownerInfo = ownerInfo.replaceAll("\\bT/C\\b", "");
		ownerInfo = ownerInfo.replaceAll("\\bR/S\\b", "");
		ownerInfo = ownerInfo.replaceAll("(?is)\\bPOA\\b", "");
		ownerInfo = ownerInfo.replaceFirst("\\bP/R\\s*$", "");
		
		ownerInfo = ownerInfo.replaceAll("\\bC/O\\b", "&");
//		ownerInfo = ownerInfo.replaceAll("\\bAND\\b", "&");
		ownerInfo = ownerInfo.replaceAll("[0-9]*%", "&");
		ownerInfo = ownerInfo.replaceAll("\\bTRE\\b", "TRUSTEE");
		
		ownerInfo = NameCleaner.cleanNameNew(ownerInfo);
		
		return ownerInfo;
	}
}
