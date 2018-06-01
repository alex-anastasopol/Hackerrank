package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.tags.TableRow;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.functions.COFremontAO;
import ro.cst.tsearch.servers.functions.COLarimerTR;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;

public class TNWilsonTR extends TNGenericEgovTR
{
	private static final long	serialVersionUID	= -8320698209773934272L;
	
	public TNWilsonTR(String rsRequestSolverName, String rsSitePath, String rsServerID, 
			String rsPrmNameLink, long searchId, int miServerID) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId,	miServerID);
	}
	
	@Override
	protected ResultMap parseSpecificIntermediaryRow(TableRow row) {
		ResultMap resultMap = new ResultMap();
		int columnCount = row.getColumnCount();
		if (columnCount > 1) {
			resultMap.put("tmpOwner", row.getColumns()[1].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());
			ro.cst.tsearch.servers.functions.TNMontgomeryTR.parseNames(resultMap);
		}
		if (columnCount > 3) {
			resultMap.put("tmpAddress", row.getColumns()[3].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());
			ro.cst.tsearch.servers.functions.TNMontgomeryTR.parseAddress(resultMap);
		}
		if (columnCount > 4) {
			resultMap.put(TaxHistorySetKey.TAX_BILL_NUMBER.getKeyName(),
					row.getColumns()[4].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());
		}
		if (columnCount > 5) {
			resultMap.put(TaxHistorySetKey.YEAR.getKeyName(),
					row.getColumns()[5].toPlainTextString().replaceAll("(?is)&nbsp;", "").trim());
		}

		return resultMap;
	}
	
	@Override
	protected void parseSpecificDetails(ResultMap resultMap) {
		try {
			ro.cst.tsearch.servers.functions.TNMontgomeryTR.parseAddress(resultMap);
			parseNames(resultMap);
		} catch (Exception e) {
			logger.error("Error while parsing details");
		}
	}
	
	public static void parseNames(ResultMap resultMap) {
		
		String owner = (String) resultMap.get("tmpOwner");
		if(StringUtils.isEmpty(owner)) {
			return;
		}
		Vector<String> companyExpr = new Vector<String>();
		companyExpr.add("PERSONAL");
		
 		owner = fixNames(owner);
		String[] lines = owner.split("\n");
		
		lines = splitAndFixOwner(lines);
		for(int i = 0; i < lines.length; i++) {
			lines[i] = cleanName(lines[i]);
		}
		COFremontAO.genericParseNames(resultMap, lines, null, companyExpr, 
				false, COFremontAO.FIRST_NAME_LF, -1);
	}
	
	private static String fixNames(String names) {
		String properNames = names;
		properNames = properNames.replace("%", "\n");
		properNames = properNames.replaceAll("\\bC/O\\b", "\n");
		properNames = properNames.replaceAll("\\bA/?K/?A\\b", "\n");
		properNames = properNames.replaceAll("\\bDRIVER:", "\n"); // S C JOHNSON & SON INC DRIVER: G SMITH
		properNames = properNames.replaceAll("\\bTRS\\b", "TRUSTEES");
		properNames = properNames.replaceAll("\\bCO-TR\\b", "TRUSTEE");
		properNames = properNames.replaceAll("\\bTR\\b", "TRUST");
		properNames = properNames.replaceAll("\\bCO\\s*-?\\s*(T(?:(?:RU?)?S)?TEE?S?)\\b", "$1"); // prevent parsing CO TRUSTEE as company
		properNames = properNames.replaceAll("(ET)\\s+(AL|UX|VIR)", "$1$2");
		properNames = properNames.replaceAll(",(\\s*ET\\s*(AL|UX|VIR))\\b", "$1,");
		properNames = properNames.replaceAll("\\b(ET\\s*(AL|UX|VIR))\\b", "\n$1\n");
		properNames = properNames.replaceAll("(?is),(\\s*\\n)", "$1");
		return properNames;
	}
	
	private static String cleanName(String name) {		
		String properName = name;
		properName = COLarimerTR.genericCleanName(properName, new Vector<String>(), "");
		return properName;
	}
	
	public static String[] splitAndFixOwner(String[] lines) {

		ArrayList<String> newLines = new ArrayList<String>();	
		for(String line:lines) {
			if(StringUtils.isEmpty(line)) {
				continue;
			}
			String[] parts = line.split("&|\\bAND\\b");
			if(NameUtils.isCompany(line)) {
				if(line.matches("(?is).*\\bBANK\\b.*")) {
					for(String part : parts) {
						if(!StringUtils.isEmpty(part)) {
							newLines.add(part);
						}
					}
				} else {
					newLines.add(line);
				}
			} else {
				for(String part : parts) {
					if(StringUtils.isNotEmpty(part)) {
						newLines.add(part);
					}
				}
			}
		}	
		return newLines.toArray(new String[newLines.size()]);
	}
}