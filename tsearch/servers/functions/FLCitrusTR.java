/**
 * 
 */
package ro.cst.tsearch.servers.functions;

import java.util.Vector;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.name.NameCleaner;

/**
 * @author vladb
 *
 */
public class FLCitrusTR {

	public static void parseNames(ResultMap resultMap, String ownerCell) {
		
		Vector<String> companyExpr = new Vector<String>();
		Vector<String> excludeWords = new Vector<String>();
		
		String properInfo = ownerCell.trim()
			.replaceFirst("(?is)\n\\s*\n.*", "")
			.replaceFirst("\n\\s*RENTAL\\s*\n", "\n") // PID 3001613 
			.trim(); // fix for the case when "prior taxes due" is present in the owner info
		
		String[] nameLines = properInfo.split("\n");
		
		nameLines = GenericFunctions.removeAddressFLTR(nameLines , excludeWords, new Vector<String>(), 2, Integer.MAX_VALUE);
		;
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
		ownerInfo = FLMonroeTR.cleanName(ownerInfo);
		ownerInfo = NameCleaner.cleanNameNew(ownerInfo);
		
		return ownerInfo;
	}
}
