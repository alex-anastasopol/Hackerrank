package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.List;

import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.util.NodeList;

import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet.PropertyAppraisalSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.RegExUtils;

/**
 * 
 * @author Oprina George
 * 
 *         Jun 29, 2011
 */

public class TNWilliamsonYA {
	public static void putSearchType(ResultMap resultMap, String type) {
		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), type);
	}

	public static ResultMap parseIntermediaryRow(TableRow row, long searchId) {
		ResultMap resultMap = new ResultMap();

		putSearchType(resultMap, "YA");

		TableColumn[] cols = row.getColumns();

		if (cols.length == 4) {
			// parcel id
			resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
					cols[3].toPlainTextString().trim());

			// year
			resultMap.put(TaxHistorySetKey.YEAR.getKeyName(), cols[0]
					.toPlainTextString().split("/")[0].trim());

			// bill no
			resultMap.put(TaxHistorySetKey.RECEIPT_NUMBER.getKeyName(), cols[0]
					.toPlainTextString().split("/").length > 1 ? cols[0]
					.toPlainTextString().split("/")[0].trim() : "");

			// owner
			parseNames(resultMap, cols[1].toPlainTextString().trim());

			// address
			parseAddress(resultMap, cols[2].toPlainTextString().trim());
		}

		return resultMap;
	}

	public static void parseAddress(ResultMap resultMap, String address) {
		String addr = address;

		resultMap.put(
				PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName(),
				addr);

		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
				StringFormats.StreetName(addr));
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(),
				StringFormats.StreetNo(addr));
	}

	public static void parseNames(ResultMap resultMap, String name) {
		resultMap.put(PropertyIdentificationSetKey.NAME_ON_SERVER.getKeyName(),
				name);

		try {
			@SuppressWarnings("rawtypes")
			List<List> body = new ArrayList<List>();
			String[] names = StringFormats.parseNameNashville(name, true);
			String[] type = GenericFunctions.extractAllNamesType(names);
			String[] otherType = GenericFunctions
					.extractAllNamesOtherType(names);
			String[] suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1],
					type, otherType, NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), body);

			GenericFunctions.storeOwnerInPartyNames(resultMap, body, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseTaxes(ResultMap resultMap, NodeList taxNodeTag) {
	}

	public static void parseLegalDescription(ResultMap resultMap,
			String legal_des) {
	}

	public static void parseDetails(String response, long searchId,
			ResultMap resultMap) {
		HtmlParser3 parser = new HtmlParser3(response
				.replaceAll("<b>Ownership and Property Description</b>", "")
				.replaceAll("<b>Appraised Value</b>", "")
				.replaceAll("<b>Assessed Value</b>", ""));
		String labelName = "Parcel ID";
		String keyName = PropertyIdentificationSetKey.PARCEL_ID.getKeyName();
		String parcelId = putValueInResultMap(resultMap, parser, labelName,
				keyName);

		parcelId = parcelId.replaceAll("\\s", "");
		resultMap.put(PropertyIdentificationSetKey.PARCEL_ID.getKeyName(),
				parcelId);

		labelName = "Tax Year/Bill Number";
		String valueFromNextCell = HtmlParser3.getValueFromNextCell(
				parser.getNodeList(), labelName, "", false);
		String firstMatch = RegExUtils.getFirstMatch("\\d{4,4}",
				valueFromNextCell, 0);
		keyName = TaxHistorySetKey.YEAR.getKeyName();
		resultMap.put(keyName, firstMatch);

		labelName = "Owner Name";
		valueFromNextCell = HtmlParser3.getValueFromNextCell(
				parser.getNodeList(), labelName, "", false);
		String secondNameCell = HtmlParser3.getValueFromAbsoluteCell(1, 1,
				HtmlParser3.findNode(parser.getNodeList(), labelName), "",
				false);

		keyName = PropertyIdentificationSetKey.OWNER_LAST_NAME.getKeyName();
		resultMap.put(keyName,
				(valueFromNextCell + " & " + secondNameCell).trim());

		try {
			TNGenericTR.ownerTNGenericTR(resultMap, searchId);
		} catch (Exception e) {
			e.printStackTrace();
		}

		labelName = "Property Location";
		keyName = "tmpPropertyLocation";
		putValueInResultMap(resultMap, parser, labelName, keyName);

		String streetNo = StringFormats.StreetNo((String) resultMap
				.get("tmpPropertyLocation"));
		resultMap.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(),
				streetNo);

		String streetName = StringFormats.StreetName((String) resultMap
				.get("tmpPropertyLocation"));
		resultMap.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(),
				streetName);

		labelName = "Subdivision";
		keyName = PropertyIdentificationSetKey.LEGAL_DESCRIPTION_ON_SERVER
				.getKeyName();
		String propertyDesc = putValueInResultMap(resultMap, parser, labelName,
				keyName);

		resultMap.put(
				PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(),
				StringFormats.SubdivisionNashvilleAO(propertyDesc));
		resultMap.put(
				PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(),
				StringFormats.SectionNashvilleAO(propertyDesc));
		resultMap.put(
				PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(),
				StringFormats.PhaseNashvilleAO(propertyDesc));
		resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER
				.getKeyName(), StringFormats.LotNashvilleAO(propertyDesc));
		resultMap.put(
				PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName(),
				StringFormats.BlockNashvilleAO(propertyDesc));

		labelName = "Description";
		keyName = PropertyIdentificationSetKey.PROPERTY_DESCRIPTION
				.getKeyName();
		String description = putValueInResultMap(resultMap, parser, labelName,
				keyName);

		resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(),
				RegExUtils.getFirstMatch("(\\d*).", description, 0));
		resultMap.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(),
				RegExUtils.getFirstMatch("\\d*-(\\d*)", description, 1));

		labelName = "Tax Amount";
		keyName = TaxHistorySetKey.BASE_AMOUNT.getKeyName();
		String taxAmount = putValueInResultMap(resultMap, parser, labelName,
				keyName);
		taxAmount = ro.cst.tsearch.utils.StringUtils.cleanAmount(taxAmount);
		resultMap.put(TaxHistorySetKey.BASE_AMOUNT.getKeyName(), taxAmount);

		labelName = "Total Due";
		keyName = TaxHistorySetKey.TOTAL_DUE.getKeyName();
		String amountDue = ro.cst.tsearch.utils.StringUtils
				.cleanAmount(putValueInResultMap(resultMap, parser, labelName,
						keyName));
		resultMap.put(TaxHistorySetKey.TOTAL_DUE.getKeyName(), amountDue);

		if ("0.00".equals(amountDue) && !"0.00".equals(taxAmount)) {
			resultMap.put(TaxHistorySetKey.AMOUNT_PAID.getKeyName(), taxAmount);
		}

		// book-page instrument date
		labelName = "Deed";
		keyName = "tmpDeed";
		String bookPage = ro.cst.tsearch.utils.StringUtils
				.cleanAmount(putValueInResultMap(resultMap, parser, labelName,
						keyName));
		List<String> matches = RegExUtils.getMatches(
				"(\\d*)-\\s*(\\d*)\\s*(\\d{4,4}-\\d{2,2}-\\d{2,2})", bookPage);
		if (matches.size() == 3) {
			if (!matches.get(0).matches("0+") && !matches.get(1).matches("0+")) {
				resultMap.put(SaleDataSetKey.BOOK.getKeyName(), matches.get(0));
				resultMap.put(SaleDataSetKey.PAGE.getKeyName(), matches.get(1));
				resultMap.put(SaleDataSetKey.INSTRUMENT_DATE.getKeyName(),
						matches.get(2));
			}
		}

		labelName = "Land Value";
		keyName = PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName();
		String land = putValueInResultMap(resultMap, parser, labelName, keyName);
		land = ro.cst.tsearch.utils.StringUtils.cleanAmount(land);
		resultMap
				.put(PropertyAppraisalSetKey.LAND_APPRAISAL.getKeyName(), land);

		labelName = "Personal Property";
		keyName = PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName();
		String totalappraisal = HtmlParser3.getValueFromAbsoluteCell(1, 1,
				HtmlParser3.findNode(parser.getNodeList(), labelName), "",
				false);
		totalappraisal = ro.cst.tsearch.utils.StringUtils
				.cleanAmount(totalappraisal);
		resultMap.put(PropertyAppraisalSetKey.TOTAL_APPRAISAL.getKeyName(),
				totalappraisal);

		labelName = "Improvements";
		keyName = PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName();
		String improvements = putValueInResultMap(resultMap, parser, labelName,
				keyName);
		improvements = ro.cst.tsearch.utils.StringUtils
				.cleanAmount(improvements);
		resultMap.put(
				PropertyAppraisalSetKey.IMPROVEMENT_APPRAISAL.getKeyName(),
				improvements);

		labelName = "Assessed Value";
		keyName = PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName();
		String totalAssessmet = putValueInResultMap(resultMap, parser,
				labelName, keyName);
		totalAssessmet = ro.cst.tsearch.utils.StringUtils
				.cleanAmount(totalAssessmet);
		resultMap.put(PropertyAppraisalSetKey.TOTAL_ASSESSMENT.getKeyName(),
				totalAssessmet);

	}

	/**
	 * @param resultMap
	 * @param parser
	 * @param labelName
	 * @param keyName
	 */
	public static String putValueInResultMap(ResultMap resultMap,
			HtmlParser3 parser, String labelName, String keyName) {
		String valueFromNextCell = HtmlParser3.getValueFromNextCell(
				parser.getNodeList(), labelName, "", false);
		resultMap.put(keyName, valueFromNextCell);
		return valueFromNextCell;
	}
}
