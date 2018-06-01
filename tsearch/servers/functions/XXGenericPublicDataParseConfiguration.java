package ro.cst.tsearch.servers.functions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ro.cst.tsearch.servers.response.CourtDocumentIdentificationSet.CourtDocumentIdentificationSetKey;
import ro.cst.tsearch.servers.response.PartyNameSet.PartyNameSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;

public class XXGenericPublicDataParseConfiguration {

	public static Map<String, List<String>> driverDataPropertyToLabels = new HashMap<String, List<String>>();
	public static Map<String, List<String>> voterDataPropertyToLabels = new HashMap<String, List<String>>();
	public static Map<String, List<String>> licensePropertyToLabels = new HashMap<String, List<String>>();
	public static Map<String, List<String>> unclaimedPropertyToLabels = new HashMap<String, List<String>>();
	public static Map<String, List<String>> civilCourtPropertyToLabels = new HashMap<String, List<String>>();
	public static Map<String, List<String>> sexOffenderPropertyToLabels = new HashMap<String, List<String>>();
	public static Map<String, List<String>> criminalPropertyToLabels = new HashMap<String, List<String>>();
	public static Map<String, List<String>> taxPropertyToLabels = new HashMap<String, List<String>>();

	static {
		setDriverData();
		setUnclaimedProperty();
		setCivilCourt();
		setSexOffender();
		setCriminal();
		setVoterData();
		setLicenseData();
		setTaxPropertyLabels();
	}

	private static void setCivilCourt() {
		civilCourtPropertyToLabels.put("court",
				Arrays.asList(new String[] { "Court" }));
		civilCourtPropertyToLabels.put(
				CourtDocumentIdentificationSetKey.CASE_NUMBER.getKeyName(),
				Arrays.asList(new String[] { "Case Number", "Case",
						"File Number", "Case ID" }));
		civilCourtPropertyToLabels.put(
				CourtDocumentIdentificationSetKey.CASE_TYPE.getKeyName(),
				Arrays.asList(new String[] { "Case Description" }));

		civilCourtPropertyToLabels.put("sex",
				Arrays.asList(new String[] { "Gender" }));

		civilCourtPropertyToLabels.put("race",
				Arrays.asList(new String[] { "Race" }));

		civilCourtPropertyToLabels.put(
				"fillingDate",
				Arrays.asList(new String[] { "File Date", "Marriage Date",
						"Date of Marriage", "Divorce Date", "Filing Date" }));
		civilCourtPropertyToLabels.put(
				SaleDataSetKey.GRANTOR.getKeyName(),
				Arrays.asList(new String[] { "Defendant Name", "Defendant 1",
						"Defendant's Name", "Husband's Name (Last, First MI)",
						"Husband's Name", "Full Name", "Name" }));
		civilCourtPropertyToLabels.put(
				SaleDataSetKey.GRANTEE.getKeyName(),
				Arrays.asList(new String[] { "Wife's Name (Last, First MI)",
						"Name of Spouse", "Wife's Name (First MI)" }));
	}

	private static void setTaxPropertyLabels() {
		taxPropertyToLabels.put("accountNumber",
				Arrays.asList(new String[] { "Account Number" }));
		taxPropertyToLabels.put(SaleDataSetKey.GRANTOR.getKeyName(),
				Arrays.asList(new String[] { "Name and Address line 1" }));
		taxPropertyToLabels.put("saleDate",
				Arrays.asList(new String[] { "Date of Sale" }));
		taxPropertyToLabels.put("legal",
				Arrays.asList(new String[] { "Legal Description" }));
		taxPropertyToLabels
				.put("address",
						Arrays.asList(new String[] { "Address (click to find others here)" }));

	}

	private static void setLicenseData() {
		licensePropertyToLabels.put(
				"licenseNumber",
				Arrays.asList(new String[] { "Certificate ID",
						"License Number", "Lic. No.", }));

		licensePropertyToLabels.put(
				"expirationDate",
				Arrays.asList(new String[] { "Certificate Expiration Date",
						"Expiration Date", "Expire Date" }));
		licensePropertyToLabels.put(SaleDataSetKey.GRANTOR.getKeyName(),
				Arrays.asList(new String[] { "Name" }));

		licensePropertyToLabels.put(PartyNameSetKey.FIRST_NAME.getKeyName(),
				Arrays.asList(new String[] { "First Name" }));
		licensePropertyToLabels.put(PartyNameSetKey.MIDDLE_NAME.getKeyName(),
				Arrays.asList(new String[] { "Middle Name" }));
		licensePropertyToLabels.put(PartyNameSetKey.LAST_NAME.getKeyName(),
				Arrays.asList(new String[] { "Last Name" }));

		// licensePropertyToLabels.put(,
		// Arrays.asList(new String[] { "" }));
	}

	private static void setCriminal() {
		criminalPropertyToLabels.putAll(civilCourtPropertyToLabels);
		List<String> list = new LinkedList<String>(
				civilCourtPropertyToLabels
						.get(CourtDocumentIdentificationSetKey.CASE_NUMBER
								.getKeyName()));
		list.add("Cause Number");
		list.add("SID Number");
		list.add("Person ID No.");
		list.add("Case ID");
		list.add("Case No.");
		list.add("Warrant Number");
		list.add("Warrant No.");

		criminalPropertyToLabels.put(
				CourtDocumentIdentificationSetKey.CASE_NUMBER.getKeyName(),
				list);
		criminalPropertyToLabels.put(
				SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), list);

		list = new LinkedList<String>(
				criminalPropertyToLabels.get("fillingDate"));
		list.add("Conviction Date");
		list.add("Arrest Date");

		criminalPropertyToLabels.put("fillingDate", list);

		criminalPropertyToLabels.put(
				PartyNameSetKey.FIRST_NAME.getKeyName(),
				Arrays.asList(new String[] { "Defendant's First Name",
						"First Name" }));
		criminalPropertyToLabels.put(
				PartyNameSetKey.LAST_NAME.getKeyName(),
				Arrays.asList(new String[] { "Defendant's Last Name",
						"Last Name" }));
	}

	private static void setSexOffender() {
		sexOffenderPropertyToLabels.put("personId",
				Arrays.asList(new String[] { "Person ID" }));
		sexOffenderPropertyToLabels.put("sex",
				Arrays.asList(new String[] { "Sex" }));
		sexOffenderPropertyToLabels.put(SaleDataSetKey.GRANTOR.getKeyName(),
				Arrays.asList(new String[] { "Name" }));
		sexOffenderPropertyToLabels.put("DOB",
				Arrays.asList(new String[] { "Date of Birth" }));
		sexOffenderPropertyToLabels.put("eventDate",
				Arrays.asList(new String[] { "Event Date" }));
	}

	private static void setDriverData() {
		driverDataPropertyToLabels.put(
				SaleDataSetKey.GRANTOR.getKeyName(),
				Arrays.asList(new String[] { "Name", "Owner Full Name",
						"Owner Name", "Owner 1" }));

		driverDataPropertyToLabels.put(PartyNameSetKey.FIRST_NAME.getKeyName(),
				Arrays.asList(new String[] { "First Name" }));
		driverDataPropertyToLabels.put(
				PartyNameSetKey.MIDDLE_NAME.getKeyName(),
				Arrays.asList(new String[] { "Middle Name" }));
		driverDataPropertyToLabels.put(PartyNameSetKey.LAST_NAME.getKeyName(),
				Arrays.asList(new String[] { "Last Name" }));
		driverDataPropertyToLabels.put(PartyNameSetKey.SUFFIX.getKeyName(),
				Arrays.asList(new String[] { "Name Suffix" }));

		// Idaho +Iowa doesn't have Driver License => instrumentNumber a
		// combination of Name+ DOB + address
		driverDataPropertyToLabels.put(
				"driverLicense",
				Arrays.asList(new String[] { "DL Number",
						"DL Number (New Format)", "Driver License Number",
						"License number", "VUID" }));

		driverDataPropertyToLabels.put(
				"vin",
				Arrays.asList(new String[] { "VIN", "Vin Number",
						"Vehicle ID Number" }));

		driverDataPropertyToLabels.put(
				"licensePlate",
				Arrays.asList(new String[] { "License Plate Number",
						"Plate Number", "Tagnum", "License Plate 1" }));

		driverDataPropertyToLabels.put("driverLicenseOld",
				Arrays.asList(new String[] { "Old DL Number" }));

		driverDataPropertyToLabels.put("dateOfBirth",
				Arrays.asList(new String[] { "Date of Birth", "DOB" }));
		driverDataPropertyToLabels.put("sex",
				Arrays.asList(new String[] { "Gender", "Sex" }));
	}

	private static void setVoterData() {
		// voterDataPropertyToLabels.put(SaleDataSetKey.GRANTOR.getKeyName(),
		// Arrays.asList(new String[] { "Name" }));

		voterDataPropertyToLabels.put(PartyNameSetKey.FIRST_NAME.getKeyName(),
				Arrays.asList(new String[] { "First Name" }));
		voterDataPropertyToLabels.put(PartyNameSetKey.MIDDLE_NAME.getKeyName(),
				Arrays.asList(new String[] { "Middle Name" }));
		voterDataPropertyToLabels.put(PartyNameSetKey.LAST_NAME.getKeyName(),
				Arrays.asList(new String[] { "Last Name" }));
		voterDataPropertyToLabels.put(PartyNameSetKey.SUFFIX.getKeyName(),
				Arrays.asList(new String[] { "Name Suffix" }));

		// Idaho +Iowa doesn't have Driver License => instrumentNumber a
		// combination of Name+ DOB + address
		voterDataPropertyToLabels.put("voterId",
				Arrays.asList(new String[] { "VUID" }));
		voterDataPropertyToLabels.put("dateOfBirth",
				Arrays.asList(new String[] { "Date of Birth", "DOB" }));
		voterDataPropertyToLabels.put("sex",
				Arrays.asList(new String[] { "Gender", "Sex" }));
	}

	private static void setUnclaimedProperty() {
		unclaimedPropertyToLabels.put("propertyId",
				Arrays.asList(new String[] { "Property ID" }));
		unclaimedPropertyToLabels
				.put("accountNumber",
						Arrays.asList(new String[] { "Account No.",
								"Account Number" }));

		unclaimedPropertyToLabels.put(
				SaleDataSetKey.GRANTOR.getKeyName(),
				Arrays.asList(new String[] { "Owner Name", "Full Name", "Name",
						"Owner" }));
		unclaimedPropertyToLabels.put(
				PartyNameSetKey.LAST_NAME.getKeyName(),
				Arrays.asList(new String[] { "Owner Last Name",
						"Former Owner Last Name / Company Name" }));
		unclaimedPropertyToLabels.put(
				PartyNameSetKey.FIRST_NAME.getKeyName(),
				Arrays.asList(new String[] { "Owner First Name",
						"Former Owner First Name" }));
		unclaimedPropertyToLabels.put(PartyNameSetKey.MIDDLE_NAME.getKeyName(),
				Arrays.asList(new String[] { "Owner Middle Name" }));

		unclaimedPropertyToLabels.put(
				"address",
				Arrays.asList(new String[] { "Former Owner Address line 1",
						"Owner Address line 1", "Street Address line 1",
						"Owner Street Address", "Address line 1", "Address" }));
		unclaimedPropertyToLabels.put(
				"city",
				Arrays.asList(new String[] { "Former Owner City", "Owner City",
						"City" }));
		unclaimedPropertyToLabels.put("zip",
				Arrays.asList(new String[] { "ZIP Code" }));
		unclaimedPropertyToLabels.put(
				SaleDataSetKey.GRANTEE.getKeyName(),
				Arrays.asList(new String[] { "Holder / Property Description",
						"Holder Company Name", "Holder Name" }));
		unclaimedPropertyToLabels.put("coOwner",
				Arrays.asList(new String[] { "Co-Owners" }));

		// unclaimedPropertyToLabels.put(, Arrays.asList(new String[] { }));
	}

}
