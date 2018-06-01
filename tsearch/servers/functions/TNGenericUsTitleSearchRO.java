package ro.cst.tsearch.servers.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.WordUtils;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.ResultTable;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.address.StandardAddress;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

public class TNGenericUsTitleSearchRO {
	
	public static String[] CHEATHAM_CITIES = {"ASHLAND CITY", "KINGSTON SPRINGS", "PEGRAM", "PLEASANT VIEW", "JOELTON"};
	
	public static String[] DICKSON_CITIES = {"BURNS", "CHARLOTTE", "DICKSON", "SLAYDEN", "VANLEER", "WHITE BLUFF",	//incorporated
											 "BELLSBURG", "CUMBERLAND FURNACE",										//unincorporated
	                                         "BON AQUA"};
	
	public static String[] OBION_CITIES = {"South Fulton", "Union City", "Woodland Mills",							//cities
										   "Hornbeak", "Kenton", "Obion", "Rives", "Samburg", "Trimble", "Troy",	//towns
										   "Cunningham", "Midway"};													//unincorporated
	
	//they can be addresses, but are subdivision names 
	public static String[] POSSIBLE_ADDRESSES_EXCEPTIONS = {"FORREST PARK ESTATES"};
	
	public static ResultMap parseIntermediaryRow(TableTag row, ro.cst.tsearch.servers.types.TNGenericUsTitleSearchRO tnGenericUsTitleSearchRO) {
		
		ResultMap resultMap = new ResultMap();
		
		resultMap.put("OtherInformationSet.SrcType", tnGenericUsTitleSearchRO.getDataSite().getSiteTypeAbrev());
		
		TableTag innerTable = (TableTag)row.getRow(0).getColumns()[0].getFirstChild();
		TableRow mainRow = (TableRow) innerTable.getRow(0);
		TableColumn[] cols = mainRow.getColumns();
		Pattern bookPagePattern = Pattern.compile("(.*?)\\-(.*?)$");
		
		for(int columnIndex=0; columnIndex<mainRow.getColumnCount(); columnIndex++) {
			TableColumn col = cols[columnIndex];
//			String contents = col.getStringText().trim();
			String plainTextContents = StringUtils.prepareStringForHTML(col.toPlainTextString()).trim();
			switch(columnIndex) {
				case 0:
//					Matcher instrumentNumberMatcher = instrumentNumberPattern.matcher(contents);
//					if(instrumentNumberMatcher.find()) {
//						if(!("TNRobertson".equals(stateCounty))){
//							resultMap.put("SaleDataSet.InstrumentNumber",instrumentNumberMatcher.group(2));
//						}
//					}
					break;
				case 1:
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_NAME.getKeyName(),plainTextContents);
					break;
				case 2:
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName(),plainTextContents);
					break;
				case 3:
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_PHASE.getKeyName(),plainTextContents);
					break;
				case 4:
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName(),plainTextContents);
					break;
				case 5:
					//building
					break;
				case 6:
					resultMap.put(PropertyIdentificationSetKey.SUBDIVISION_UNIT.getKeyName(),plainTextContents);
					break;
				case 7:
					//acres
					break;
				case 8:
					//district
					break;
				case 9:
					resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(),plainTextContents);
					break;
				case 10:
					resultMap.put(SaleDataSetKey.RECORDED_DATE.getKeyName(),plainTextContents);
					break;
				case 11:
					//class
					break;
				case 12:
					String book,page;
					Matcher bookPageMatcher = bookPagePattern.matcher(plainTextContents);
					if(bookPageMatcher.find()) {
						book = bookPageMatcher.group(1);
						page = bookPageMatcher.group(2);
						resultMap.put(SaleDataSetKey.BOOK.getKeyName(),book);
						resultMap.put(SaleDataSetKey.PAGE.getKeyName(),page);	
					}
					break;
			}
			
		}
		
		return resultMap;
	}
	
	public static void newParsingTNGenericUsTitleSearchRO(ResultMap m, long searchId) throws Exception {
		if ("/".equals(m.get("SaleDataSet.Grantee"))) {
			m.put("SaleDataSet.Grantee", "");
		}
		List<String> allSubdivisions = new ArrayList<String>();
		try {
			String county = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
			ResultTable pis = (ResultTable) m.get("PropertyIdentificationSet");
			String[][] pisBody = pis.getBody();
			List<String[]> newBody = new ArrayList<String[]>(pis.getBody().length);
			for (int i = 0; i < pis.getLength(); i++) {
				String name = org.apache.commons.lang.StringUtils.defaultString(pisBody[i][0]);
				String section = org.apache.commons.lang.StringUtils.defaultString(pisBody[i][1]);
				String phase = org.apache.commons.lang.StringUtils.defaultString(pisBody[i][2]);
				String lot = org.apache.commons.lang.StringUtils.defaultString(pisBody[i][3]);
				String building = org.apache.commons.lang.StringUtils.defaultString(pisBody[i][4]);
				String unit = org.apache.commons.lang.StringUtils.defaultString(pisBody[i][5]);
				String acres = org.apache.commons.lang.StringUtils.defaultString(pisBody[i][6]);
				String district = org.apache.commons.lang.StringUtils.defaultString(pisBody[i][7]);

				if (!StringUtils.isEmpty(lot)) {
					
					String cleanedLotLowerCase = lot.replaceAll(" ", "").toLowerCase();
					
					if (cleanedLotLowerCase.equals("tract")
							|| cleanedLotLowerCase.equals("lot")
							|| cleanedLotLowerCase.equals("ease")
							|| cleanedLotLowerCase.equals("row")
							|| cleanedLotLowerCase.equals("tracts")) {
						pisBody[i][3] = "";
						lot = "";
					}
				}

				/* Try to see if we have a subdivision or an address */
				boolean isAddress = false;

				if (StringUtils.areAllEmpty(section, phase, lot, building, unit)) {
					ro.cst.tsearch.search.address2.StandardAddress tokAddr = new ro.cst.tsearch.search.address2.StandardAddress(
							name);
					if (StringUtils.isNotEmpty(tokAddr.getAddressElement(StandardAddress.STREET_NUMBER))
							|| StringUtils.isNotEmpty(tokAddr.getAddressElement(StandardAddress.STREET_SUFFIX))) {
						isAddress = true;
						for (int j=0;j<POSSIBLE_ADDRESSES_EXCEPTIONS.length;j++) {
							if (name.equalsIgnoreCase(POSSIBLE_ADDRESSES_EXCEPTIONS[j])) {
								isAddress = false;
								break;
							}
						}
					}
				}
				
				if ("Obion".equalsIgnoreCase(county)) {
					String[] addressAndCity = StringFormats.parseCityFromAddress(name, OBION_CITIES);
					m.put(PropertyIdentificationSetKey.CITY.getKeyName(), addressAndCity[0]);
					pisBody[i][0] = name = addressAndCity[1].trim();
				}
				
				if (isAddress || name.matches("(?is).*\\bHIGHWAY\\b.*")) {
					parsePossibleAddress(m, name, county);
					m.put("PropertyIdentificationSet.Acres", acres);
					
					if (StringUtils.isNotEmpty(district)) {
						district = district.replaceFirst("(?is)0+(\\d+)", "$1");
						if (!"0".equals(district)) {
							m.put("PropertyIdentificationSet.District", district);
						}
					}
				} else {
					allSubdivisions.add(name);
					newBody.add(pisBody[i]);
				}
			}
			String[][] newBodyArray = new String[newBody.size()][pisBody[0].length];
			for (int i = 0; i < newBody.size(); i++) {
				newBodyArray[i] = newBody.get(i);
				if (newBodyArray[i] != null) {
					for (int j = 0; j < newBodyArray[i].length; j++) {
						if (newBodyArray[i][j] == null) {
							newBodyArray[i][j] = "";
						}
					}
				}
			}
			if (newBody.size() > 0) {
				pis.setReadOnly(false);
				pis.setBody(newBodyArray);
				pis.setReadOnly(true);
			}

			String legalRef = (String) m.get("PropertyIdentificationSet.LegalRef");

			String s[] = GenericFunctions1.SubdivparseRuthRO(legalRef, searchId);
			if (s != null) {
				legalRef = s[0];
			} else {
				legalRef = "";
			}
			// legalRef = legalRef.replaceAll("LO?TS?:?\\s\\d+(\\-\\d+)?","").trim();
			if (StringUtils.isNotEmpty(legalRef)) {
				ro.cst.tsearch.search.address2.StandardAddress tokAddr = new ro.cst.tsearch.search.address2.StandardAddress(legalRef);
				if (StringUtils.isNotEmpty(tokAddr.getAddressElement(StandardAddress.STREET_NUMBER))
						|| StringUtils.isNotEmpty(tokAddr.getAddressElement(StandardAddress.STREET_SUFFIX))) {
					boolean isAddress = true;
					for (String subdiv : allSubdivisions) {
						if (legalRef.contains(subdiv)) {
							isAddress = false;
							break;
						}
					}
					if (isAddress) {
						parsePossibleAddress(m, legalRef, county);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			EmailClient email = new EmailClient();
			email.addTo(MailConfig.getExceptionEmail());
			email.setSubject("Error ro.cst.tsearch.servers.functions.TNGenericUsTitleSearchRO.newParsingTNGenericUsTitleSearchRO(ResultMap, "
					+ searchId + ") on " + URLMaping.INSTANCE_DIR);
			email.addContent("Error ro.cst.tsearch.servers.functions.TNGenericUsTitleSearchRO.newParsingTNGenericUsTitleSearchRO(ResultMap, "
					+ searchId
					+ ")\n\n Stack Trace: "
					+ e.getMessage()
					+ " \n\n "
					+ ServerResponseException.getExceptionStackTrace(e, "\n"));
			email.sendAsynchronous();
		}
	}
	
	public static void parseCrossRefSet(ResultMap m, long searchId) {
		if (m.get("CrossRefSet") != null) {
			ResultTable crossrefs = (ResultTable) m.get("CrossRefSet");

			String[][] body = crossrefs.getBody();

			// get grantor and parse it
			String grantor = (String) (m.get(SaleDataSetKey.GRANTOR.getKeyName()) != null ? m.get(SaleDataSetKey.GRANTOR.getKeyName()) : "");

			if (StringUtils.isNotEmpty(grantor) && grantor.matches(".*?\\d+/\\d+")) {
				// get the reference \\d+/\\d+
				String ref = grantor.replaceAll(".*?(\\d+/\\d+)", "$1");

				String book = "";
				String page = "";

				if (ref.matches("\\d+/\\d+")) {
					book = ref.split("/")[0];
					page = ref.split("/")[1];
				}

				if (StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)) {
					// add ref to body and set body
					String[] refToAdd = new String[crossrefs.getHead().length];
					
					if (refToAdd.length < 6)
						return;

					for (int i = 0; i < refToAdd.length; i++) {
						refToAdd[i] = "";
					}

					refToAdd[5] = book;
					refToAdd[6] = page;

					List<List<String>> newBody = new ArrayList<List<String>>();
					
					//copy old body
					for(String[] line: body){
						newBody.add(Arrays.asList(line));
					}
					
					newBody.add(Arrays.asList(refToAdd));
					try {
						crossrefs.setReadOnly(false);
						crossrefs.setBody(newBody);
					} catch (Exception e) {
						e.printStackTrace();
					}
					m.put("CrossRefSet", crossrefs);
				}
			}
		}
	}
	 
	 private static void parsePossibleAddress(ResultMap m,String name,String county) {
		 	String address = name, cityStateZip = "";
		 	if(address.contains(",")) {
				address = address.substring(0, address.indexOf(","));
				cityStateZip = name.substring(name.indexOf(","));
			}
			
		 	String city = "", streetNo = "";
		 	streetNo = StringFormats.StreetNo(address);
			
			if(StringUtils.isNotEmpty(streetNo)) {
				address = address.replaceAll(streetNo, "");	
			}
			
			if(address.toUpperCase().contains(county.toUpperCase())) {
				address = address.replaceAll("(?i)"+county.toUpperCase(), "");
				city = county.toUpperCase();
			}
			
			Pattern addressCityZipPattern = Pattern.compile(",(.*?),(.*?)");
			Matcher matcher = addressCityZipPattern.matcher(cityStateZip);
			if(matcher.find()) {
				city = matcher.group(1).trim();
			}
			
			if(!StringUtils.isEmpty(city)) {
				address = address.replace(city, "");
			}
			
			if ("Cheatham".equalsIgnoreCase(county)) {
				String[] addressAndCity = StringFormats.parseCityFromAddress(address, CHEATHAM_CITIES);
				city = addressAndCity[0];
				address = addressAndCity[1];
			} else if ("Dickson".equalsIgnoreCase(county)) {
				address = address.replaceFirst("(?i)\\bCUMBERLAND FURANCE\\s*$", "CUMBERLAND FURNACE");	//Volume V779-383
				String[] addressAndCity = StringFormats.parseCityFromAddress(address, DICKSON_CITIES);
				city = addressAndCity[0];
				address = addressAndCity[1];
			} 
			
			/* is address */
			m.put("PropertyIdentificationSet.StreetName",address.trim());
			m.put("PropertyIdentificationSet.StreetNo",streetNo);
			m.put("PropertyIdentificationSet.City",city);
			m.put("PropertyIdentificationSet.AddressOnServer",name);
			ResultTable pis = (ResultTable) m.get("PropertyIdentificationSet");
			String[][] pisBody = pis.getBody();
			for (int i = 0; i < pis.getLength(); i++) {
				pisBody[i][0] = pisBody[i][0].replaceFirst(Pattern.quote(name), "");
			}
			try {
				pis.setReadOnly(false);
				pis.setBody(pisBody);
				pis.setReadOnly(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
			m.put("PropertyIdentificationSet", pis);
	 }
	 
	@SuppressWarnings("rawtypes")
	public static void parseGrantorGranteeSetTNRO(ResultMap m, long searchId)
			throws Exception {

		String grantor = (String) m.get("SaleDataSet.Grantor");
		String grantee = (String) m.get("SaleDataSet.Grantee");
		
		if (!StringUtils.isEmpty(grantor)) {
			grantor = grantor.replaceAll("(?is)\\bS/D\\b", "SD");
			m.put("SaleDataSet.Grantor", grantor);
		}
		if (!StringUtils.isEmpty(grantee)) {
			grantee = grantee.replaceAll("(?is)\\bS/D\\b", "SD");
			m.put("SaleDataSet.Grantee", grantee);
		}

		if (StringUtils.isEmpty(grantee)
				&& StringUtils.isNotEmpty((String) m
						.get("SaleDataSet.GranteeLander"))) {
			grantee = (String) m.get("SaleDataSet.GranteeLander");
		}
		
		grantor = org.apache.commons.lang.StringUtils.defaultString(grantor);
		grantee = org.apache.commons.lang.StringUtils.defaultString(grantee);
		
		String docType = (String) m.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName());
		if (docType != null){
			if (DocumentTypes.checkDocumentType(docType, DocumentTypes.MORTGAGE_INT,null,searchId)){	//3846
				grantor = grantor.replaceAll("(?i)\\bTR(U?(STEE))?\\b", "");
				grantee = grantee.replaceAll("(?i)\\bTR(U?(STEE))?\\b", "");
			}
		}
		
		grantor = StringUtils.prepareStringForHTML(grantor);
		grantee = StringUtils.prepareStringForHTML(grantee);
		grantor = NameCleaner.cleanFreeformName(grantor);
		grantee = NameCleaner.cleanFreeformName(grantee);

		ArrayList<List> grantorList = new ArrayList<List>();
		ArrayList<List> granteeList = new ArrayList<List>();

		parseNameInner(m, grantor, grantorList, searchId, false);
		parseNameInner(m, grantee, granteeList, searchId, true);

		m.put("GrantorSet", GenericFunctions.storeOwnerInSet(grantorList, true));
		m.put("GranteeSet", GenericFunctions.storeOwnerInSet(granteeList, true));
		GenericFunctions1.setGranteeLanderTrustee2(m, searchId,true);
		GenericFunctions.checkTNCountyROForMERSForMortgage(m, searchId);
	}

	@SuppressWarnings("rawtypes")
	public static void parseNameInner(ResultMap m, String name,
			ArrayList<List> namesList, long searchId, boolean isGrantee) {

		String[] names = { "", "", "", "", "", "" };
		String[] suffixes, type, otherType;

		name = NameCleaner.cleanNameAndFix(name, new Vector<String>(), true);
		name = name.replaceAll("(?is)\\bEXECUTRIX\\b", "");
		name = name.replaceAll("(?is)\\bCO[\\s-]*EXECUTORS?\\b", "");
		name = name.replaceAll("(?is),?\\s+SUB\\s+(TR)", " $1");
		name = name.replaceAll("\\s{2,}", ", ");
		
		/**@see ro.cst.tsearch.search.name.NameCleaner#fixScotishLikeNames(String)  */
		name = name.replaceAll("(?is)_{4}", "-");
		name = name.replaceAll("(?is)_{2}", "'");
		

		String[] nameItems = name.split("/");
		for (int i = 0; i < nameItems.length; i++) {
			
			names = StringFormats.parseNameNashville(nameItems[i], true);
			NameCleaner.removeUnderscore(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);
			suffixes = GenericFunctions.extractNameSuffixes(names);
			GenericFunctions.addOwnerNames(nameItems[i], names, suffixes[0],
					suffixes[1], type, otherType,
					NameUtils.isCompany(names[2]),
					NameUtils.isCompany(names[5]), namesList);

		}

	}

	public static void sdsTNGenericUsTitleSearch(ResultMap m, long searchId) {
		String originalBookType = (String) m.get(SaleDataSetKey.BOOK_TYPE.getKeyName());
		if(org.apache.commons.lang.StringUtils.isNotBlank(originalBookType)) {
			originalBookType = WordUtils.capitalizeFully(originalBookType);
			if(originalBookType.startsWith("Greenbelt")) {
				m.put(SaleDataSetKey.BOOK_TYPE.getKeyName(), "GB");
			} else if (originalBookType.startsWith("Deed Of Trust")) {
				m.put(SaleDataSetKey.BOOK_TYPE.getKeyName(), "TD");
			} else {
				m.put(SaleDataSetKey.BOOK_TYPE.getKeyName(), originalBookType);
			}
			
		}
		
	}

	
}
