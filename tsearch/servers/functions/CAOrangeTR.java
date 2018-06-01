package ro.cst.tsearch.servers.functions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;

public class CAOrangeTR {
	public static void parseLegalSummary(ResultMap resultMap, String legalDescription)
	{
		Pattern LEGAL_TRACT_PATTERN =
				Pattern.compile("(?is)[\\w-]{0,2}TRACT:?\\s*([\\d]+)");
		Pattern LEGAL_RANGE_PATTERN =
				Pattern.compile("RANGE:?\\s+([^\\s]+)");
		Pattern LEGAL_TOWNSHIP_PATTERN =
				Pattern.compile("[\\w-]{0,2}TOWNSHIP:?\\s+([^\\s]+)");
		Pattern LEGAL_PLAT_BOOK_PATTERN =
				Pattern.compile("(?is)\\w?-?{0,2}BOOK: ([^\\s]+)");
		Pattern LEGAL_PLAT_PAGE_PATTERN =
				Pattern.compile("(?is)\\w?-?{0,2}PAGE:\\s*([^\\s]+)");
		Pattern LEGAL_LOT_PATTERN =
				Pattern.compile("(?is)LO?T?:\\s?([^\\s]+)");

		Matcher mTract = LEGAL_TRACT_PATTERN.matcher(legalDescription);
		if (mTract.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), mTract.group(1));
			legalDescription = legalDescription.replace(mTract.group(), " ");
		}
		String block = ro.cst.tsearch.extractor.legal.LegalDescription.extractBlockFromText(legalDescription, false, false);
		if (block.length() > 0)
			if (!block.contains("LO"))
				resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(), block);

		Matcher mLot = LEGAL_LOT_PATTERN.matcher(legalDescription);
		if (mLot.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(), mLot.group(1));
			legalDescription = legalDescription.replace(mLot.group(), " ");
		}

		Matcher mRange = LEGAL_RANGE_PATTERN.matcher(legalDescription);
		if (mRange.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_RANGE.getKeyName(), mRange.group(1));
			legalDescription = legalDescription.replace(mRange.group(), " ");
		}
		Matcher mTownship = LEGAL_TOWNSHIP_PATTERN.matcher(legalDescription);
		if (mTownship.find()) {
			resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_TOWNSHIP.getKeyName(), mTownship.group(1));
			legalDescription = legalDescription.replace(mTownship.group(), " ");
		}

		Matcher mBook = LEGAL_PLAT_BOOK_PATTERN.matcher(legalDescription);
		if (mBook.find()) {
			resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), mBook.group(1));
			legalDescription = legalDescription.replace(mBook.group(), " ");
		}
		Matcher mPage = LEGAL_PLAT_PAGE_PATTERN.matcher(legalDescription);
		if (mPage.find()) {
			resultMap.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), mPage.group(1));
			legalDescription = legalDescription.replace(mPage.group(), " ");
		}
	}
}
