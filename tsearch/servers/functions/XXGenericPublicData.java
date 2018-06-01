package ro.cst.tsearch.servers.functions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.Text;
import org.htmlparser.tags.TableColumn;

import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.parser.HtmlParser3;
import ro.cst.tsearch.parser.StringParentTagVisitorList;
import ro.cst.tsearch.servers.response.CourtDocumentIdentificationSet.CourtDocumentIdentificationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PartyNameSet.PartyNameSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.types.XXGenericPublicDataParentSiteConfiguration;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.RegExUtils;

import com.stewart.ats.base.address.Address;
import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.CourtDocumentPublicData;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DriverDataDocument;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.OffenderInformationDocument;
import com.stewart.ats.base.document.PersonIdentificationDocument;
import com.stewart.ats.base.document.PersonIdentificationDocumentI;
import com.stewart.ats.base.document.ProfessionalLicenseDocument;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.SexOffenderDocument;
import com.stewart.ats.base.document.TaxDocument;
import com.stewart.ats.base.document.Transfer;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.parties.Party;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.Pin;
import com.stewart.ats.base.property.Property;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

/**
 * 
 * @author Oprina George
 * 
 *         Aug 12, 2011
 */

public class XXGenericPublicData {

	private static final String INPUT_TYPE_HIDDEN_NAME_VALUE = "<input type='hidden' name='(.*?)'  value='(.*?)'/>";
	public static final String SOURCE_DOC_REG_EX = "(?is)<h4>.*?bullet5.gif.*?/>(.*)</h4>";

	private static final String REGEX_PREFIX = "REGEX";

	// private static final String REGEX_PATTERN = REGEX_PREFIX + "@%s@";

	public static void parseDetails(String response, long searchId,
			ResultMap resultMap) {
		extract(searchId, response);
	}

	public static DocumentI parseDetailsToDocument(String response, long searchId) {
		DocumentI document = extract(searchId, response);
		return document;
	}

	public static Vector<ParsedResponse> parseIntermediary(
			ServerResponse serverResponse, String response, long searchId,
			String partial_link) {
		return null;
	}

	public static void createDocument(long searchId,
			ParsedResponse currentResponse, PersonBean personBean) {
		ResultMap resultMap = new ResultMap();
		String id = personBean.getUniqueID();
		resultMap.put(SaleDataSetKey.INSTRUMENT_NUMBER.getKeyName(), id);

		RegisterDocument regDoc = new RegisterDocument(DocumentsManager.generateDocumentUniqueId(searchId, new Instrument()));
		PersonIdentificationDocumentI document = new PersonIdentificationDocument(
				(RegisterDocument) regDoc);
		document.setGrantorFreeForm(personBean.getName());
		PropertyI prop = Property.createEmptyProperty();
		prop.setOwner(setName(personBean.getName(),
				SimpleChapterUtils.PType.GRANTOR));
		prop.setPin(new Pin());

		Set<PropertyI> s = new HashSet<PropertyI>();
		s.add(prop);
		document.setProperties(s);

		document.setDocType(DocumentTypes.MISCELLANEOUS);
		document.setDocSubType(DocumentTypes.MISCELLANEOUS);

		currentResponse.setDocument(document);
	}

	public static void parseName(String name, ResultMap resultMap) {
		List<String> body = new ArrayList<String>();
		ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap, name,
				body);
	}

	public static DocumentI extract(long searchId, String resultTable) {
		String dataSource = getDataSource(resultTable);
		DocumentI bean = null;
		PersonIdentificationEnum enum1 = PersonIdentificationEnum
				.valueOfByDataSource(dataSource);
		// System.out.println("dataSource " + dataSource);
		if (enum1 != null) {
			bean = enum1.getPopulatedDocument(searchId, resultTable);
		} else {
			bean = new RegisterDocument(DocumentsManager.generateDocumentUniqueId(searchId, new Instrument()));
		}

		return bean;
	}

	/**
	 * @param resultTable
	 * @return
	 */
	public static String getDataSource(String resultTable) {
		byte keyIndex = 1, valueIndex = 2;
		Map<String, List<String>> documentIdentification = RegExUtils
				.getMatchesAsMap(INPUT_TYPE_HIDDEN_NAME_VALUE, resultTable,
						keyIndex, valueIndex);

		List<String> dbCase = documentIdentification.get("db");
		List<String> oCase = documentIdentification.get("o");

		String source = "";
		if (dbCase != null && dbCase.size() > 0) {
			source = dbCase.get(0);
		} else if (oCase != null && oCase.size() > 0) {
			source = oCase.get(0);
		}

		String dataSource = source.toUpperCase();
		return dataSource;
	}

	private static List<String> getDataSourcefromOptions(String[][] options) {
		List<String> list = new ArrayList<String>();

		for (String[] s : options) {
			list.add(s[0].toUpperCase());
		}
		return list;
	}

	private static List<String> DRIVER_DATA_DATA_SOURCE = new ArrayList<String>();
	private static List<String> COURT_DOCUMENT_DATA_SOURCE = new ArrayList<String>();
	private static List<String> SEX_OFFENDER_DOCUMENT_DATA_SOURCE = new ArrayList<String>();
	private static List<String> CRIMINAL_DOCUMENT_DATA_SOURCE = new ArrayList<String>();
	private static List<String> VOTER_DOCUMENT_DATA_SOURCE = new ArrayList<String>();
	private static List<String> PROFESSIONAL_LICENSE_DOCUMENT_DATA_SOURCE = new ArrayList<String>();
	private static List<String> TAX_DOCUMENT_DATA_SOURCE = new ArrayList<String>();

	static {
		DRIVER_DATA_DATA_SOURCE
				.addAll(getDataSourcefromOptions(XXGenericPublicDataParentSiteConfiguration.DRIVERS_LICENCE_NAME_SEARCH_OPTIONS));
		DRIVER_DATA_DATA_SOURCE
				.addAll(getDataSourcefromOptions(XXGenericPublicDataParentSiteConfiguration.DMV_PLATE_OPTIONS));
		DRIVER_DATA_DATA_SOURCE
				.addAll(getDataSourcefromOptions(XXGenericPublicDataParentSiteConfiguration.DMV_VIN_OPTIONS));

		COURT_DOCUMENT_DATA_SOURCE
				.addAll(getDataSourcefromOptions(XXGenericPublicDataParentSiteConfiguration.CIVIL_COURT_NAME_SEARCH_TX));
		SEX_OFFENDER_DOCUMENT_DATA_SOURCE
				.addAll(getDataSourcefromOptions(XXGenericPublicDataParentSiteConfiguration.SEX_OFFENDER_NAME_SEARCH_TX));
		CRIMINAL_DOCUMENT_DATA_SOURCE
				.addAll(getDataSourcefromOptions(XXGenericPublicDataParentSiteConfiguration.CRIMINAL_NAME_SEARCH_TX));
		VOTER_DOCUMENT_DATA_SOURCE
				.addAll(getDataSourcefromOptions(XXGenericPublicDataParentSiteConfiguration.VOTER_NAME_SEARCH_TX));
		PROFESSIONAL_LICENSE_DOCUMENT_DATA_SOURCE
				.addAll(getDataSourcefromOptions(XXGenericPublicDataParentSiteConfiguration.PROFESSIONAL_LICENSE_NAME_SEARCH_TX));
		TAX_DOCUMENT_DATA_SOURCE
				.addAll(getDataSourcefromOptions(XXGenericPublicDataParentSiteConfiguration.TAX_SEARCH_NAME_SEARCH_TX));
	}

	public enum PersonIdentificationEnum {
		DRIVER_DATA(
				new HashSet<String>(DRIVER_DATA_DATA_SOURCE),
				XXGenericPublicDataParseConfiguration.driverDataPropertyToLabels,
				new HashSet<String>(Arrays.asList(new String[] {
						"driverLicense", "vin", "licensePlate" }))),

		UNCLAIMED_PROP(
				new HashSet<String>(
						Arrays.asList(new String[] { "UNCLAIMED_PROP" })),
				XXGenericPublicDataParseConfiguration.unclaimedPropertyToLabels,
				new HashSet<String>(Arrays.asList(new String[] { "propertyId",
						"accountNumber" }))),

		COURT_DOCUMENT(
				new HashSet<String>(COURT_DOCUMENT_DATA_SOURCE),
				XXGenericPublicDataParseConfiguration.civilCourtPropertyToLabels,
				new HashSet<String>(Arrays.asList(new String[] {
						"fileNumber",
						CourtDocumentIdentificationSetKey.CASE_NUMBER
								.getKeyName() }))),

		SEX_OFFENDER_DOCUMENT(
				new HashSet<String>(SEX_OFFENDER_DOCUMENT_DATA_SOURCE),
				XXGenericPublicDataParseConfiguration.sexOffenderPropertyToLabels,
				new HashSet<String>(Arrays.asList(new String[] { "personId" }))),

		CRIMINAL_DOCUMENT(
				new HashSet<String>(CRIMINAL_DOCUMENT_DATA_SOURCE),
				XXGenericPublicDataParseConfiguration.criminalPropertyToLabels,
				new HashSet<String>(
						Arrays.asList(new String[] { CourtDocumentIdentificationSetKey.CASE_NUMBER
								.getKeyName() })), new HashSet<String>(
						Arrays.asList(new String[] { "fillingDate" }))),

		VOTER_DOCUMENT(
				new HashSet<String>(VOTER_DOCUMENT_DATA_SOURCE),
				XXGenericPublicDataParseConfiguration.voterDataPropertyToLabels,
				new HashSet<String>(Arrays.asList(new String[] { "voterId" })),
				new HashSet<String>(
						Arrays.asList(new String[] { "dateOfBirth" }))),

		PROFESSIONAL_LICENSE_DOCUMENT(new HashSet<String>(
				PROFESSIONAL_LICENSE_DOCUMENT_DATA_SOURCE),
				XXGenericPublicDataParseConfiguration.licensePropertyToLabels,
				new HashSet<String>(
						Arrays.asList(new String[] { "licenseNumber" })),
				new HashSet<String>(
						Arrays.asList(new String[] { "expirationDate" }))),

		TAX_DOCUMENT(new HashSet<String>(TAX_DOCUMENT_DATA_SOURCE),
				XXGenericPublicDataParseConfiguration.taxPropertyToLabels,
				new HashSet<String>(
						Arrays.asList(new String[] { "accountNumber" })),
				new HashSet<String>(
						Arrays.asList(new String[] { "dateOfSale" }))),

		DEFAULT_DOCUMENT(new HashSet<String>(Arrays.asList(new String[] {})),
				new HashMap<String, List<String>>(), new HashSet<String>(
						Arrays.asList(new String[] {})), new HashSet<String>(
						Arrays.asList(new String[] {})));

		private Map<String, List<String>> propertyToLabels;
		private Set<String> propertyAsId;
		private Set<String> propertyAsRecordedDate;

		private PersonIdentificationEnum(Set<String> dataSourceAbreviation,
				Map<String, List<String>> propertyToLabels,
				Set<String> propertyAsID, Set<String> propertyAsRecordedDate) {
			this.dataSourceAbreviation = dataSourceAbreviation;
			this.propertyToLabels = propertyToLabels;
			this.propertyAsId = propertyAsID;
			this.propertyAsRecordedDate = propertyAsRecordedDate;
		}

		private PersonIdentificationEnum(Set<String> dataSourceAbreviation,
				Map<String, List<String>> propertyToLabels,
				Set<String> propertyAsID) {
			this.dataSourceAbreviation = dataSourceAbreviation;
			this.propertyToLabels = propertyToLabels;
			this.propertyAsId = propertyAsID;
			this.propertyAsRecordedDate = Collections.emptySet();

		}

		public Collection<List<String>> getIdLabels() {
			Collection<List<String>> list = this.propertyToLabels.values();
			return list;
		}

		private DocumentI getDocument(long searchId, Instrument instrument) {
			String id = DocumentsManager.generateDocumentUniqueId(searchId, instrument);
			DocumentI doc = new RegisterDocument(id);
			switch (this) {
			case DRIVER_DATA:
				doc = new DriverDataDocument((RegisterDocument) doc) {
					@Override
					public void setGranteeFreeForm(String granteeFreeForm) {
						super.setGranteeFreeForm(granteeFreeForm);
						// ResultMap resultMap = new ResultMap();
						// String[] nameParsed =
						// ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap,
						// granteeFreeForm, null);
						//
						// PartyI grantee1 = new
						// Party(SimpleChapterUtils.PType.GRANTEE);
						// // if nameParsed.length> 0
						// grantee1.add(new Name(nameParsed[0], nameParsed[1],
						// nameParsed[2]));

						PartyI setName = XXGenericPublicData.setName(
								granteeFreeForm,
								SimpleChapterUtils.PType.GRANTEE);
						setGrantee(setName);

					}

					public void setGrantorFreeForm(String granteeFreeForm) {
						super.setGrantorFreeForm(granteeFreeForm);
					}
				};
				break;
			case UNCLAIMED_PROP:
				doc = new Transfer((RegisterDocument) doc) {
					@Override
					public void setGrantorFreeForm(String grantorFreeFrorm) {
						super.setGrantorFreeForm(grantorFreeFrorm);
						// ResultMap resultMap = new ResultMap();
						// String[] nameParsed =
						// ParseNameUtil.putNamesInResultMapFromNashvilleParse(resultMap,
						// grantorFreeFrorm, null);
						//
						// PartyI grantee1 = new Party();
						// grantee1.add(new Name(nameParsed[0], nameParsed[1],
						// nameParsed[2]));
						PartyI setName = XXGenericPublicData.setName(
								grantorFreeFrorm,
								SimpleChapterUtils.PType.GRANTOR);
						setGrantor(setName);
					}

					public void setGranteeFreeForm(String grantorFreeFrorm) {
						super.setGrantorFreeForm(grantorFreeFrorm);
						PartyI setName = XXGenericPublicData.setName(
								grantorFreeFrorm,
								SimpleChapterUtils.PType.GRANTEE);
						setGrantee(setName);
					}
				};
				break;
			case COURT_DOCUMENT:
				doc = new CourtDocumentPublicData((RegisterDocument) doc);
				break;
			case SEX_OFFENDER_DOCUMENT:
				doc = new SexOffenderDocument((RegisterDocument) doc);
				break;
			case CRIMINAL_DOCUMENT:
				doc = new OffenderInformationDocument((RegisterDocument) doc);
				break;
			case VOTER_DOCUMENT:
				doc = new PersonIdentificationDocument((RegisterDocument) doc);
				break;
			case PROFESSIONAL_LICENSE_DOCUMENT:
				doc = new ProfessionalLicenseDocument((RegisterDocument) doc);
				break;
			case TAX_DOCUMENT:
				doc = new TaxDocument(id, DType.TAX);
				break;
			case DEFAULT_DOCUMENT:
				doc = new RegisterDocument((RegisterDocument) doc);
				break;
			}

			return doc;
		}

		private Instrument treatExceptionInstrNo(DocumentI bean,
				Map<String, String> labelToValue, Instrument instrument) {
			switch (this) {
			case DRIVER_DATA:
				if (StringUtils.isEmpty(labelToValue.get("driverLicense"))) {
					// probably search by vin
					String newInstrNo = "";

					if (StringUtils.isNotEmpty(labelToValue.get("vin")))
						newInstrNo += labelToValue.get("vin");
					if (StringUtils
							.isNotEmpty(labelToValue.get("licensePlate")))
						newInstrNo += labelToValue.get("licensePlate");

					instrument.setInstno(newInstrNo);
				}
				break;
			}

			return instrument;
		}

		public DocumentI getPopulatedDocument(long searchId, String resultTable) {
			DocumentI bean;
			HtmlParser3 parser3 = new HtmlParser3(resultTable);
			Map<String, String> labelToValue = getLabelToValueMap(parser3);

			String id2 = getDocumentIDCandidate(resultTable, labelToValue);
			com.stewart.ats.base.document.Instrument instrument2 = new com.stewart.ats.base.document.Instrument(
					id2);
			bean = getDocument(searchId, instrument2);
			if (!propertyAsRecordedDate.isEmpty()) {
				if (bean instanceof RegisterDocument) {
					((RegisterDocument) bean)
							.setRecordedDate(getDocumentRecordedDateCandidate(labelToValue));
				} else if (bean instanceof TaxDocument) {
					((TaxDocument) bean)
							.setSaleDate(new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY)
									.format(getDocumentRecordedDateCandidate(labelToValue)));
					labelToValue.remove("saleDate");
				}
			}
			// instrument2 = treatExceptionInstrNo(bean, labelToValue,
			// instrument2);

			bean.setInstrument(instrument2);
			bean.setDataSource("PD");

			String serverDocType = StringUtils
					.defaultString(
							RegExUtils.getFirstMatch(SOURCE_DOC_REG_EX,
									resultTable, 1)).trim();
			bean.setServerDocType(serverDocType);
			// Bridge.fillDocument(resultMap, parsedResponse, searchId)

			bean.setDocType(DocumentTypes.MISCELLANEOUS);
			bean.setDocSubType(DocumentTypes.MISCELLANEOUS);

			try {
				BeanUtils.populate(bean, labelToValue);

				// set the names
				String grantor = labelToValue.get(SaleDataSetKey.GRANTOR
						.getKeyName());
				String grantee = StringUtils.defaultString(labelToValue
						.get(SaleDataSetKey.GRANTEE.getKeyName()));
				String format = String.format(
						"%s %s %s",
						StringUtils.defaultIfEmpty(labelToValue
								.get(PartyNameSetKey.LAST_NAME.getKeyName()),
								""),
						StringUtils.defaultIfEmpty(labelToValue
								.get(PartyNameSetKey.FIRST_NAME.getKeyName()),
								""),
						StringUtils.defaultIfEmpty(labelToValue
								.get(PartyNameSetKey.MIDDLE_NAME.getKeyName()),
								"")).trim();

				if (StringUtils.isNotEmpty(format)) {
					bean.setGrantorFreeForm(format);
					PartyI setName = setName(format,
							SimpleChapterUtils.PType.GRANTOR);
					if (bean instanceof RegisterDocument) {
						((RegisterDocument) bean).setGrantor(setName);
						PropertyI prop = Property.createEmptyProperty();
						prop.setOwner(setName);
						prop.setPin(new Pin());
						Set<PropertyI> s = new HashSet<PropertyI>();
						s.add(prop);
						((RegisterDocument) bean).setProperties(s);
					} else if (bean instanceof TaxDocument) {
						((TaxDocument) bean).setGrantorFreeForm(format);
						PropertyI prop = Property.createEmptyProperty();
						prop.setOwner(setName);
						prop.setPin(new Pin());

						AddressI adr = new Address();
						adr.setNumber(StringFormats.StreetNo(labelToValue
								.get("address")));
						adr.setStreetName(StringFormats.StreetName(labelToValue
								.get("address")));
						prop.setAddress(adr);

						LegalI legal = new Legal();
						legal.setFreeForm(labelToValue.get("legal"));
						prop.setLegal(legal);

						Set<PropertyI> s = new HashSet<PropertyI>();
						s.add(prop);
						((TaxDocument) bean).setProperties(s);
					}
				} else if (StringUtils.isNotEmpty(grantor)) {
					bean.setGrantorFreeForm(grantor);
					PartyI setName = setName(grantor,
							SimpleChapterUtils.PType.GRANTOR);
					if (bean instanceof RegisterDocument) {
						((RegisterDocument) bean).setGrantor(setName);
						PropertyI prop = Property.createEmptyProperty();
						prop.setOwner(setName);
						prop.setPin(new Pin());
						Set<PropertyI> s = new HashSet<PropertyI>();
						s.add(prop);
						((RegisterDocument) bean).setProperties(s);
					} else if (bean instanceof TaxDocument) {
						((TaxDocument) bean).setGrantorFreeForm(grantor);
						PropertyI prop = Property.createEmptyProperty();
						prop.setOwner(setName);
						prop.setPin(new Pin());

						AddressI adr = new Address();
						adr.setNumber(StringFormats.StreetNo(labelToValue
								.get("address")));
						adr.setStreetName(StringFormats.StreetName(labelToValue
								.get("address")));
						prop.setAddress(adr);

						LegalI legal = new Legal();
						legal.setFreeForm(labelToValue.get("legal"));
						prop.setLegal(legal);

						Set<PropertyI> s = new HashSet<PropertyI>();
						s.add(prop);
						((TaxDocument) bean).setProperties(s);
					}
				}

				if (StringUtils.isNotEmpty(grantee)) {
					bean.setGranteeFreeForm(grantee);
					PartyI setName = setName(grantee,
							SimpleChapterUtils.PType.GRANTEE);
					if (bean instanceof RegisterDocument) {
						((RegisterDocument) bean).setGrantee(setName);
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			return bean;
		}

		/**
		 * @param resultTable
		 * @param labelToValue
		 * @return
		 */
		public String getDocumentIDCandidate(String resultTable,
				Map<String, String> labelToValue) {
			byte keyIndex = 1, valueIndex = 2;
			Map<String, List<String>> documentIdentification = RegExUtils
					.getMatchesAsMap(INPUT_TYPE_HIDDEN_NAME_VALUE, resultTable,
							keyIndex, valueIndex);

			String id2 = documentIdentification.get("rec") != null
					&& documentIdentification.get("rec").size() > 0 ? documentIdentification
					.get("rec").get(0) : "";

			String db = documentIdentification.get("db") != null
					&& documentIdentification.get("db").size() > 0 ? documentIdentification
					.get("db").get(0) : "";
			id2 = id2 + db;
			Set<String> propertyIDs = this.propertyAsId;

			String id3 = "";

			for (String id : propertyIDs) {
				String tempID = labelToValue.get(id);
				if (StringUtils.isNotEmpty(tempID)) {
					id3 += tempID;
				}
			}
			if (StringUtils.isEmpty(id3))
				return id2.toUpperCase();
			else
				return id3.toUpperCase();
		}

		public Date getDocumentRecordedDateCandidate(
				Map<String, String> labelToValue) {
			Set<String> props = this.propertyAsRecordedDate;
			Date recordedDate = Calendar.getInstance().getTime();
			for (String p : props) {
				String val = labelToValue.get(p);
				if (StringUtils.isNotEmpty(val)) {
					Date dateParser3 = Util.dateParser3(val);
					recordedDate = dateParser3 == null ? recordedDate
							: dateParser3;
				}
			}
			return recordedDate;
		}

		/**
		 * @param parser3
		 * @return
		 */
		public Map<String, String> getLabelToValueMap(HtmlParser3 parser3) {
			Set<Entry<String, List<String>>> propertyValuesEntrySet = propertyToLabels
					.entrySet();
			Map<String, String> labelToValue = new HashMap<String, String>();

			for (Entry<String, List<String>> entry : propertyValuesEntrySet) {
				Iterator<String> labels = entry.getValue().iterator();
				boolean isFound = false;
				while (labels.hasNext() && !isFound) {
					String label = labels.next();
					Text nodeToParse = HtmlParser3.findNode(
							parser3.getNodeList(), label,
							new StringParentTagVisitorList(label, "i"), true);

					// NodeList findNodeList =
					// HtmlParser3.findNodeList(parser3.getNodeList(), label);
					// System.out.println("Label " + label);
					TableColumn cell = HtmlParser3.getAbsoluteCell(0, 0,
							nodeToParse);
					if (cell != null) {
						String match = RegExUtils.getFirstMatch(
								"(?is)<br>(.*?)</td>", cell.toHtml(), 1);
						if (StringUtils.isNotEmpty(match)) {
							labelToValue.put(entry.getKey(), match.trim());
							isFound = true;
						}
					}
				}
			}
			return labelToValue;
		}

		public static PersonIdentificationEnum valueOfByDataSource(
				String dataSource) {
			PersonIdentificationEnum pi = DEFAULT_DOCUMENT;
			PersonIdentificationEnum[] typeOfDocumentsAccepted = values();
			for (PersonIdentificationEnum val : typeOfDocumentsAccepted) {
				Set<String> expectedDataSource = val.getDataSourceAbreviation();
				if (expectedDataSource.contains(dataSource.toUpperCase())) {
					pi = val;
				} else {
					for (String ds : expectedDataSource) {
						if (ds.startsWith(REGEX_PREFIX)) {
							String firstLimit = REGEX_PREFIX + "@";
							String dataSourcePattern = ds.substring(
									ds.indexOf(firstLimit)
											+ firstLimit.length(),
									ds.lastIndexOf("@"));
							if (dataSource.matches(dataSourcePattern)) {
								pi = val;
							}
						}
					}
				}
			}
			return pi;
		}

		private Set<String> dataSourceAbreviation;

		public Set<String> getDataSourceAbreviation() {
			return dataSourceAbreviation;
		}
	}

	public static String cleanDetails(String response) {
		if (!response.contains("<html"))
			return response;

		String match = RegExUtils.getFirstMatch(
				"(?is)<div id='details_results'>.*</div>", response, 0);
		String typeOfDocument = RegExUtils
				.getFirstMatch(
						ro.cst.tsearch.servers.functions.XXGenericPublicData.SOURCE_DOC_REG_EX,
						response, 0);
		String inputs = RegExUtils
				.getFirstMatch("(?ism)</html>.*", response, 0);

		match = typeOfDocument + "\n" + match + "\n"
				+ inputs.replace("</html>", "");
		match = match.replaceAll("(?is)<div class='pdfooters'>.*?</div>", "");
		match = match.replaceAll("(?is)<div class='footerNav'>.*?</div>", "");
		match = match.replaceAll("(?is)<div id='footerNav'>.*?</div>", "");
		match = match.replaceAll("(?is)<div id=\"footerNav\">.*?</div>", "");

		match = match.replaceAll("(?ism)<a [^>]*>(.*)</a>", "$1");
		match = match.replaceAll("(?ism)<a [^>]*>", "").replaceAll("</a>", "");

		return match;
	}

	/**
	 * @param grantorFreeFrorm
	 */
	public static PartyI setName(String grantorFreeFrorm,
			SimpleChapterUtils.PType granteeType) {
		ResultMap resultMap = new ResultMap();
		String[] nameParsed = ParseNameUtil
				.putNamesInResultMapFromNashvilleParse(resultMap,
						grantorFreeFrorm, null);

		PartyI grantee1 = new Party(granteeType);
		grantee1.add(new Name(nameParsed[0], nameParsed[1], nameParsed[2]));
		return grantee1;
	}

}