package ro.cst.tsearch.extractor.xml;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.xerces.dom.DeferredElementImpl;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.country.Countries;
import ro.cst.tsearch.data.StateContants;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.extractor.legal.LegalDescription;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.name.NameCleaner;
import ro.cst.tsearch.search.name.NameFactory;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.token.AddressAbrev;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.functions.CAGenericDT;
import ro.cst.tsearch.servers.functions.FLBayTR;
import ro.cst.tsearch.servers.functions.FLBrevardTR;
import ro.cst.tsearch.servers.functions.FLBrowardRV;
import ro.cst.tsearch.servers.functions.FLCollierRV;
import ro.cst.tsearch.servers.functions.FLCollierTR;
import ro.cst.tsearch.servers.functions.FLDeSotoTR;
import ro.cst.tsearch.servers.functions.FLDuvalTR;
import ro.cst.tsearch.servers.functions.FLEscambiaTR;
import ro.cst.tsearch.servers.functions.FLFranklinTR;
import ro.cst.tsearch.servers.functions.FLGulfTR;
import ro.cst.tsearch.servers.functions.FLHendryTR;
import ro.cst.tsearch.servers.functions.FLHernandoTR;
import ro.cst.tsearch.servers.functions.FLJacksonRV;
import ro.cst.tsearch.servers.functions.FLLeeTR;
import ro.cst.tsearch.servers.functions.FLLevyTR;
import ro.cst.tsearch.servers.functions.FLMartinTR;
import ro.cst.tsearch.servers.functions.FLNassauTR;
import ro.cst.tsearch.servers.functions.FLOkeechobeeRV;
import ro.cst.tsearch.servers.functions.FLOkeechobeeTR;
import ro.cst.tsearch.servers.functions.FLOrangeTR;
import ro.cst.tsearch.servers.functions.FLPascoTR;
import ro.cst.tsearch.servers.functions.FLPolkTR;
import ro.cst.tsearch.servers.functions.FLRVnames;
import ro.cst.tsearch.servers.functions.FLSantaRosaTR;
import ro.cst.tsearch.servers.functions.FLSarasotaTR;
import ro.cst.tsearch.servers.functions.FLSeminoleTR;
import ro.cst.tsearch.servers.functions.FLStJohnsTR;
import ro.cst.tsearch.servers.functions.FLSumterTR;
import ro.cst.tsearch.servers.functions.FLWakullaTR;
import ro.cst.tsearch.servers.functions.FLWaltonTR;
import ro.cst.tsearch.servers.functions.GenericDASLNDBFunctions;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.Roman;
import ro.cst.tsearch.utils.StringUtils;

/* This file should contain the GenericFunctions methods related only to Florida counties. 
 * The methods for Tennessee, Missouri, Kansas, Kentucky, Ohio, Illinois, Michigan are stored in  GenericFunctions1.
 */

public class GenericFunctions2 extends GenericFunctions1 {

	public static final Pattern NCB_NO_PATTERN = Pattern.compile("((?is)(?<=((NCB\\s?)|(NEW CITY BL )|(CITY BLOCK )))(\\d+[A-Z]?))");
	public static final String ROMAN_NUMERAL_PATTERN = "M{0,4}(?:CM|CD|D?C{0,3})(?:XC|XL|L?X{0,3})(?:IX|IV|V?I{0,3})";		//matches Roman numerals up to 4000
	public static final String FLOAT_NUMBER_PATTERN = "[0-9]*\\.?[0-9]+";
	
	public static String cleanNameFLHillsboroughTR(String s) {
		s = s.replaceFirst("^XXX*\\s+", "");
		s = s.replaceFirst("\\s+(?:AS )?((?:TTEEE?|TR|TRUSTEEE?)S?)\\s*$", " $1");
		s = s.replaceFirst("\\s+(?:AS )?(P\\s?A)\\s*$", "");
		s = s.replaceFirst("\\s+ESQ\\s*$", "");
		s = s.replaceAll("\\s{2,}", " ").trim();
		return s;
	}

	public static void partyNamesFLHillsboroughTR(ResultMap m, long searchId) throws Exception {
		String crtOwner = (String) m.get("tmpCrtOwner");
		String lastOwner = (String) m.get("tmpLastOwnerTxt");
		if (lastOwner == null) {
			lastOwner = (String) m.get("tmpLastOwnerOptAll");
		}

		String owner = crtOwner;
		if (lastOwner != null && crtOwner != null) {
			if (lastOwner.contains(crtOwner)) {
				owner = lastOwner;
			}
		}
		partyNamesTokenizerFLHillsboroughTR(m, owner);
	}

	public static void partyNamesTokenizerFLHillsboroughTR(ResultMap m, String owner) throws Exception {

		String[] owners = owner.split("@@");
		List<List> body = new ArrayList<List>();
		String[] names;
		String[] suffixes = { "", "" }, type = { "", "" }, otherType = { "", "" };
		Pattern p1 = Pattern.compile(".+ AND (([A-Z]+ ([A-Z]{2,}( [A-Z])?|[A-Z] [A-Z]))|[A-Z] [A-Z]+)");
		Pattern p2 = Pattern.compile(".+ AND ([A-Z]+ [A-Z]+ [A-Z'-]+)");
		Pattern p3 = Pattern.compile("(.+) C/O (.+)");
		Matcher ma;
		boolean parserCoownerFML = false;

		for (int i = 0; i < owners.length; i++) {
			String ow = owners[i];
			ow = cleanNameFLHillsboroughTR(ow);
			parserCoownerFML = false;
			String coowner = "";
			ma = p3.matcher(ow);
			if (ma.matches()) {
				parserCoownerFML = true;
				coowner = ma.group(2);
				names = StringFormats.parseNameNashville(ma.group(1), true);
			} else {
				names = StringFormats.parseNameNashville(ow, true);
				ma = p1.matcher(ow);
				if (ma.matches()) {
					// correction
					if ((!names[5].contains(names[2]) && !names[2].contains(names[5])) || names[5].length() == 1) {
						// for SMITH ALFRED JAMES 2ND AND JUANITA SUE
						// or SMITH JAMES N AND JILL A B
						// or SMITH LONZO I AND M DEANNA

						names[4] = (names[4] + " " + names[3]).trim();
						names[3] = names[5];
						names[5] = names[2];
						if (names[3].length() == 1 && names[4].length() > 1) {
							String temp = names[3];
							names[3] = names[4];
							names[4] = temp;
						}
					}
				} else {
					ma = p2.matcher(ow);
					if (ma.matches()) {
						parserCoownerFML = true;
						coowner = ma.group(1);
					}
				}
			}
			if (parserCoownerFML) {
				String[] names2 = StringFormats.parseNameDesotoRO(coowner, true); // SMITH
																					// CLIFFORD
																					// AND
																					// THELMA
																					// P
																					// MATTHEWS
				names[3] = names2[0]; // or SMITH TOBIAS P C/O MICHAEL COFFEE
										// ESQ
				names[4] = names2[1];
				names[5] = names2[2];
			}
			suffixes = extractNameSuffixes(names);
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);

			addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);
		}
		storeOwnerInPartyNames(m, body, true);
	}

	public static void pisFLHillsoboroughTR(ResultMap m, long searchId) throws Exception {

		String crtOwner = (String) m.get("tmpCrtOwner");
		String lastOwner1 = (String) m.get("tmpLastOwnerTxt");
		String lastOwner2 = "";
		if (lastOwner1 == null) {
			lastOwner1 = (String) m.get("tmpLastOwnerOpt1");
			lastOwner2 = (String) m.get("tmpLastOwnerOpt2");
		}

		String owner = crtOwner;
		String co_owner = "";
		if (lastOwner1 != null && crtOwner != null) {
			if (lastOwner1.contains(crtOwner)) {
				owner = lastOwner1;
				if (lastOwner2 != null && lastOwner2.length() != 0)
					co_owner = lastOwner2;
			}
		}

		if (owner != null && owner.length() != 0) {
			owner = owner.replaceFirst("^XXX*\\s+", "");

			// apply LFM name tokenizer
			String names[] = StringFormats.parseNameNashville(owner);
			if (names[5].length() == 0) {
				String names_co_owner[] = StringFormats.parseNameNashville(co_owner);
				names[5] = names_co_owner[2];
				names[3] = names_co_owner[0];
				names[4] = names_co_owner[1];
			}

			m.put("PropertyIdentificationSet.OwnerFirstName", names[0]);
			m.put("PropertyIdentificationSet.OwnerMiddleName", names[1]);
			m.put("PropertyIdentificationSet.OwnerLastName", names[2]);
			m.put("PropertyIdentificationSet.SpouseFirstName", names[3]);
			m.put("PropertyIdentificationSet.SpouseMiddleName", names[4]);
			m.put("PropertyIdentificationSet.SpouseLastName", names[5]);
		}

		String streetNo = org.apache.commons.lang.StringUtils.defaultString((String) m.get(PropertyIdentificationSetKey.STREET_NO.getKeyName())).trim()
				.replaceAll("^0+", "");
		m.remove(PropertyIdentificationSetKey.STREET_NO.getKeyName());
		if (!streetNo.isEmpty()) {
			m.put(PropertyIdentificationSetKey.STREET_NO.getKeyName(), streetNo);
		}

		String streetName = org.apache.commons.lang.StringUtils.defaultString((String) m.get(PropertyIdentificationSetKey.STREET_NAME.getKeyName())).trim();
		m.remove(PropertyIdentificationSetKey.STREET_NAME.getKeyName());
		if (!RegExUtils.getFirstMatch("(?i)\\b(CONFIDENTIAL)\\b", streetName, 1).isEmpty()) {
			streetName = streetName.replaceAll("(?i)\\s*(\\bXX\\b|\\bSITE\\b|\\*+)\\s*", "");
		}
		if (!streetName.isEmpty()) {
			m.put(PropertyIdentificationSetKey.STREET_NAME.getKeyName(), streetName);
		}
	}

	public static void pisGenericDASLRV(ResultMap m, long searchId) throws Exception {

		String fullName = (String) m.get("tmpFullName");
		if (fullName == null || fullName.length() == 0)
			return;

		if (NameUtils.isCompany(fullName)) {
			m.put("PropertyIdentificationSet.OwnerLastName", fullName);

		} else {
			// apply LFM name tokenizer
			String names[] = StringFormats.parseNameNashville(fullName);

			m.put("PropertyIdentificationSet.OwnerFirstName", names[0]);
			m.put("PropertyIdentificationSet.OwnerMiddleName", names[1]);
			m.put("PropertyIdentificationSet.OwnerLastName", names[2]);
			m.put("PropertyIdentificationSet.SpouseFirstName", names[3]);
			m.put("PropertyIdentificationSet.SpouseMiddleName", names[4]);
			m.put("PropertyIdentificationSet.SpouseLastName", names[5]);
		}
	}

	public static void legalFLHillsoboroughTR(ResultMap m, long searchId) throws Exception {

		String pid = (String) m.get("PropertyIdentificationSet.ParcelID3");
		String sec = "";
		String twn = "";
		String rng = "";
		String lot = "";
		String blk = "";
		if (pid != null || pid.length() != 0) {
			Pattern p = Pattern.compile("^[A-Z]-(\\d+)-(\\d+)-(\\d+)-.+-(\\w+)-(\\d+).*");
			Matcher ma = p.matcher(pid);
			if (ma.find()) {
				sec = ma.group(1).replaceFirst("^0*(.+)", "$1");
				twn = ma.group(2).replaceFirst("^0*(.+)", "$1");
				rng = ma.group(3).replaceFirst("^0*(.+)", "$1");

				String lotFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
				if (StringUtils.isEmpty(lotFromSet)) {
					lot = ma.group(5).replaceFirst("^0*(.+)", "$1");
					m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
				}

				String blkFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
				if (StringUtils.isEmpty(blkFromSet)) {
					blk = ma.group(4).replaceFirst("^0*(.*)", "$1").replaceAll("^([A-Z])0*", "$1");
					m.put("PropertyIdentificationSet.SubdivisionBlock", blk);
				}

				ResultTable pis = (ResultTable) m.get("PropertyIdentificationSet");
				if (pis == null) {
					if (sec.length() != 0 && twn.length() != 0 && rng.length() != 0) {
						String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };
						String[][] body = { { sec, twn, rng } };

						Map<String, String[]> map = new HashMap<String, String[]>();
						map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
						map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
						map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

						ResultTable newPis = new ResultTable();
						newPis.setHead(header);
						newPis.setBody(body);
						newPis.setMap(map);
						m.put("PropertyIdentificationSet", newPis);
					}
				} else {
					String[][] body = pis.body;
					int len = body.length;
					List<List> newBody = new ArrayList<List>();
					boolean addSTR = true;
					for (int i = 0; i < len; i++) {
						boolean isSame = (body[i].length == 3) && body[i][1].equals(twn) && body[i][2].equals(rng);
						if (isSame && !body[i][0].equals(sec)) {
							body[i][0] = LegalDescription.cleanValues(body[i][0], true, true);
							isSame = false;
							addSTR = false;
						}
						if (!isSame)
							newBody.add(Arrays.asList(body[i]));
					}
					if (addSTR) {
						List<String> str = new ArrayList<String>();
						str.add(sec);
						str.add(twn);
						str.add(rng);
						newBody.add(str);
					}
					pis.setBody(newBody);
				}
			}
		}

		String pid3 = pid.replaceAll("[.-]", "");
		m.put("PropertyIdentificationSet.ParcelID3", pid3);
		pid = pid.replaceFirst("^([A-Z])-(.+)", "$2$1").replaceAll("\\W", ""); // prepared
																				// for
																				// NDB
		m.put("PropertyIdentificationSet.ParcelID2", pid);
	}

	public static void taxFLHillsboroughTR(ResultMap m, long searchId) throws Exception {

		String priorDelinq = sum((String) m.get("tmpPriorDelinqSum"), searchId);
		m.put("TaxHistorySet.PriorDelinquent", priorDelinq);

		String totalDue = (String) m.get("TaxHistorySet.TotalDue");
		if (StringUtils.isEmpty(totalDue)) {
			String certificateDueTotal = (String) m.get("tmpTotalDueCertificate");
			if (!StringUtils.isEmpty(certificateDueTotal)) {
				totalDue = certificateDueTotal;
				m.put("TaxHistorySet.CurrentYearDue", totalDue);
			} else {
				totalDue = "0.00";
			}
		}
		String totalDelinq = priorDelinq;
		Date dueDate = DBManager.getDueDateForCountyAndCommunity(InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity().getID()
				.longValue(), InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getCountyId().longValue());
		Date dateNow = new Date();
		if (dateNow.after(dueDate)) {
			totalDelinq = new BigDecimal(priorDelinq).add(new BigDecimal(totalDue)).toString();
		}
		m.put("TaxHistorySet.DelinquentAmount", totalDelinq);

		String amtPaid = "0.00";
		String crtYearStatus = (String) m.get("tmpStatusCrtYear");
		String crtYearPaiment = sum((String) m.get("tmpCrtYearPayment"), searchId);
		if ("Installment".equalsIgnoreCase(crtYearStatus)) {
			String installment = sum((String) m.get("tmpCrtYearInstallment"), searchId);
			if ("0.00".equals(installment)) {
				amtPaid = crtYearPaiment;
			} else {
				amtPaid = installment;
			}
		} else {
			amtPaid = crtYearPaiment;
		}
		m.put("TaxHistorySet.AmountPaid", amtPaid);
	}

	public static void instrumentNoFormatNDB(ResultMap m, long searchId) throws Exception {

		String instr = (String) m.get("SaleDataSet.InstrumentNumber");
		if (StringUtils.isEmpty(instr))
			return;

		String recDate = (String) m.get("SaleDataSet.RecordedDate");

		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();

		if ("TN".equals(crtState)) { // fix for Davidson, Knox, Rutherford and
										// Hamilton - bug # 2228

			if ("Davidson".equals(crtCounty) || "Knox".equals(crtCounty)) {

				if (StringUtils.isEmpty(recDate))
					return;

				try {
					Date date = Util.dateParser3(recDate);
					String newInstr = String.format("%1$tY%1$tm%1$td", date)
							+ String.format("%1$7s", instr.replaceFirst("^0+(.+)", "$1")).replaceAll("\\s", "0");
					m.put("SaleDataSet.InstrumentNumber", newInstr);
				} catch (Exception e) {
					m.remove("SaleDataSet.InstrumentNumber");
					return;
				}

			} else if ("Rutherford".equals(crtCounty)) {
				if (!instr.matches("RB\\d{2,7}") && !instr.matches("\\d+")) {
					m.remove("SaleDataSet.InstrumentNumber");
					return;
				}
				if (instr.matches("\\d{8,}")) {
					return;
				}
				m.remove("SaleDataSet.InstrumentNumber");

				if (StringUtils.isEmpty(recDate)) {
					String instrNoRaw = "";
					if (m.get("tmpInstrumentNumberRawTNRutherford") != null
							&& StringUtils.isNotEmpty((instrNoRaw = (String) m.get("tmpInstrumentNumberRawTNRutherford")))) {
						if (instrNoRaw.contains("-")) {
							String book = instrNoRaw.split("-")[0];
							String page = instrNoRaw.split("-")[1];

							m.put("SaleDataSet.Book", book);
							m.put("SaleDataSet.Page", page.replaceFirst("^0+(.+)", "$1"));
						}
					}
					return;
				}

				Date d = Util.dateParser3(recDate);
				if (d == null)
					return;

				Calendar c = Calendar.getInstance();
				c.setTime(d);

				int year = c.get(Calendar.YEAR);

				if (instr.matches("RB\\d{2,7}")) {
					m.put("SaleDataSet.BookType", "RB");
					instr = instr.substring(2);
				}
				String book = "";
				String page = "";
				if (year <= 2001 && instr.length() < 7) {
					book = instr.substring(0, 2);
					page = instr.substring(2);
				} else {
					book = instr.substring(0, 3);
					page = instr.substring(3);
				}
				m.put("SaleDataSet.Book", book);
				m.put("SaleDataSet.Page", page.replaceFirst("^0+(.+)", "$1"));

			} else if ("Hamilton".equals(crtCounty)) {
				if ((instr.length() > 5) || (instr.length() == 5 && !instr.endsWith("0"))) {
					m.remove("SaleDataSet.InstrumentNumber");
					String book = instr.substring(0, 4);
					String page = instr.substring(4);
					m.put("SaleDataSet.Book", book);
					m.put("SaleDataSet.Page", page.replaceFirst("^0+(.+)", "$1"));
				}
			}
		}

	}

	public static void legalGenericDASLNDB(ResultMap m, long searchId) throws Exception {
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		State currentState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState();
		String crtState = currentState.getStateAbv();

		String addressName = (String) m.get("PropertyIdentificationSet.StreetName");
		if (addressName != null) {
			addressName = addressName.replaceAll("([NSWE]{1,2})\\s+OF\\s+(STATE)", "$1 $2").replaceAll("\\A\\s*([NSWE]{1,2})\\s+OFF\\s+([NSWE]{1,2})", "")
					.trim();// FLGilchristNB 060715000000030010
			if ("TX".equals(crtState) && "Lubbock".equals(crtCounty)) {
				// bug 6917
				if (addressName.matches("(?ism)([NSWE]{1,2}\\s+COUNTY\\s+ROAD\\s+\\d+)\\s+[#\\d]+")) {
					addressName = addressName.replaceAll("(?ism)([NSWE]{1,2}\\s+COUNTY\\s+ROAD\\s+\\d+)\\s+[#\\d]+", "$1");
				}
			} else if ("IL".equals(crtState) && "will".equalsIgnoreCase(crtCounty)) {
				addressName = addressName.replaceAll("(?is)\\bFIRST\\b", "1ST");// 401
																				// FIRST
																				// ST;
																				// ILWill
				addressName = addressName.replaceAll("(?is)\\bSECOND\\b", "2ND");
				addressName = addressName.replaceAll("(?is)\\bTHIRD\\b", "3RD");
			} else if ("CA".equals(crtState) && "san benito".equalsIgnoreCase(crtCounty)) {
				addressName = addressName.replaceFirst("[NSWE]{1,2}\\b\\s+OF\\s+ROAD TO [A-Z]+\\s*$", ""); // CA
																											// San
																											// Benito,
																											// APN
																											// 030090005000
			}

			m.put("PropertyIdentificationSet.StreetName", addressName);

			// String addressNo = (String)
			// m.get("PropertyIdentificationSet.StreetNo");
			// if(addressNo != null ) {
			// Matcher addressNoMatcher =
			// Pattern.compile("(\\d+)([NSWE]{1,2})(\\d+)").matcher(addressNo);
			// if(addressNoMatcher.find()) {
			// addressNo = addressNoMatcher.group(1);
			// addressName = addressNoMatcher.group(2) + " " +
			// addressNoMatcher.group(3) + " " + addressName;
			// m.put("PropertyIdentificationSet.StreetNo", addressNo);
			// m.put("PropertyIdentificationSet.StreetName", addressName);
			// }
			// }

		}
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legal = legal.replaceFirst("\\bNOT PART OF A SUBDIVISION\\b", "");
		legal = legal.replaceFirst("\\bMAPPLATB\\s+\\d+", "");

		if (legal.matches("\\d+") || legal.matches("\\d+\\s+\\d+")) {
			// m.put("PropertyIdentificationSet.SubdivisionNo", legal);
			m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), legal.replaceAll("\\s+", ""));
			return;
		} else if (!org.apache.commons.lang.StringUtils.defaultString(HashCountyToIndex.getServerAbbreviationByType(GWTDataSite.PRI_TYPE)).equals(m.get(OtherInformationSetKey.SRC_TYPE.getKeyName()))) {
			Matcher matcher = Pattern.compile("(\\d+(?:\\s+\\d+)?)\\s+(PH(?:ASE)?.*)").matcher(legal);
			if (matcher.find()) {
				m.put(PropertyIdentificationSetKey.SUBDIVISION_TRACT.getKeyName(), matcher.group(1).replaceAll("\\s+", ""));
				legal = matcher.group(2).trim();
			}
		}

		Matcher matcher = NCB_NO_PATTERN.matcher(legal);

		if (matcher.find()) {
			String group = matcher.group();
			m.put("PropertyIdentificationSet.NcbNo", group);
			// clean the legal from the things we parsed
			legal = legal.replaceAll(group, "");
			legal = legal.replace(matcher.group(2), "");
		}

		// it is unclear if CB means City BLock. will be extracted to parse
		// correctly sub name
		String cbNoPattern = "((?is)(?<=((CB\\s?)))(\\d+[A-Z]?))";
		Pattern cbNoPatternCompiled = Pattern.compile(cbNoPattern);
		matcher = cbNoPatternCompiled.matcher(legal);

		if (matcher.find()) {
			String cb = matcher.group();
			// clean the legal from the things we parsed
			legal = legal.replaceAll(cb, "");
			legal = legal.replace(matcher.group(2), "");
		}

		SimpleParseTokenResult absNoFromLegal = getAbsNoFromLegal(currentState.getStateId().intValue(), legal);
		if(absNoFromLegal != null && org.apache.commons.lang.StringUtils.isNotBlank(absNoFromLegal.getTokenParsed())) {
			m.put(PropertyIdentificationSetKey.ABS_NO.getKeyName(), absNoFromLegal.getTokenParsed());
			legal = absNoFromLegal.getFinalSource();
		}

		legal = legal.replaceAll("(?<=[A-Z]*\\d[A-Z]*\\s?)&(?=\\s?[A-Z]*\\d[A-Z]*)", " ");
		if (!"true".equals(m.get("tmpDoNotReplaceNumbers"))) {
			legal = replaceNumbers(legal);
		}
		legal = legal.replaceFirst("7 OAKS", "SEVEN OAKS"); // B3266
		legal = legal.replaceAll("\\bNO\\b\\s*", "");
		legal = legal.replaceAll("(\\d+)(ST|ND|RD|TH) UNIT", "UNIT $1");

		String subdiv = legal;
		// cleanup legal descr before extracting subdivision name
		subdiv = subdiv.replaceAll("^ACREAGE(\\s+&amp;|\\s+&)?\\s+UNREC(\\s+PLATS?)?$", "");
		subdiv = subdiv.trim();

		// keep forms like - SURVEY 73 A RAY
		String surveyForms = RegExUtils.getFirstMatch("SURV(EY?)\\s\\d+", subdiv, 0);
		if (StringUtils.isNotEmpty(surveyForms)) {
			subdiv = subdiv.replaceAll(surveyForms, "");
		}

		// R040644

		String filling = "";
		Pattern p1 = Pattern.compile("(?i)(.*?)\\s+FI?LI?N?G\\s+([0-9]+)\\s*");
		Matcher ma1 = p1.matcher(subdiv);
		if (ma1.find()) {
			subdiv = ma1.group(1);
			filling = ma1.group(2);
		}

		Pattern p = Pattern.compile("(?is)\\b(SEC\\.?T?I?O?N?)\\s*((?:\\d+|[A-Z]|" + ROMAN_NUMERAL_PATTERN + ")(?:(?:\\s+(?:AND|&))?\\s+(?:\\d+|[A-Z]|" + ROMAN_NUMERAL_PATTERN + "))*)\\s+");
		Matcher ma = p.matcher(subdiv + " ");
		String subdSection = "";
		String subdSections = "";
		while (ma.find()) {
			subdiv = subdiv.replace(ma.group(0).trim(), "");
			subdSection = ma.group(2).replaceAll("(?is)\\bAND\\b", "").replaceAll("&", "");
			subdSection = subdSection.replaceAll("\\s{2,}", " ").trim();
			if (Roman.isRoman(subdSection)) {
				subdSection = Roman.transformToArabic(subdSection);
			}
			subdSections += subdSection + " ";
		}
		subdSections = subdSections.trim();
		if (StringUtils.isNotEmpty(subdSections)) {
			m.put(PropertyIdentificationSetKey.SECTION.getKeyName(), subdSections);
		}

		p = Pattern.compile("(\\d+)-(\\d+)\\s+B\\s*$");
		ma = p.matcher(subdiv);
		if (ma.find()) {
			m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), ma.group(1).replaceFirst("^0+", ""));
			m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), ma.group(2).replaceFirst("^0+", ""));
		} else {
			// TNLauderdale 094OC003.00
			p = Pattern.compile("(?is)\\bPB\\s*(\\d+)\\s+P(\\d+)\\s*$");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), ma.group(1).replaceFirst("^0+", ""));
				m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), ma.group(2).replaceFirst("^0+", ""));
				subdiv = subdiv.replaceFirst(ma.group(0), "");
			} else {
				//TN Putnam 005M-A-017.00
				p = Pattern.compile("(?is)\\bPB\\s+(\\d+|[A-Z])\\s+PG\\s+(\\d+)\\b");
				ma = p.matcher(subdiv);
				if (ma.find()) {
					m.put(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName(), ma.group(1).replaceFirst("^0+", ""));
					m.put(PropertyIdentificationSetKey.PLAT_NO.getKeyName(), ma.group(2).replaceFirst("^0+", ""));
					subdiv = subdiv.replaceFirst(ma.group(0), "");
				}
			}
		}
		
		// extract phase from legal description
		String phasePattern = "\\d+\\w?|[A-Z]\\b|" + ROMAN_NUMERAL_PATTERN + "\\b";
		p = Pattern.compile("\\bPH(?:ASE)?(( ?(" + phasePattern + ")(-" + phasePattern + ")?)+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			if (!ma.group(1).trim().equals("0")) {
				String phase = ma.group(1).replaceAll("-0*", " ");
				phase = phase.trim().replaceFirst("^0+(\\w.*)", "$1");
				if (Roman.isRoman(phase)) {
					phase = Roman.transformToArabic(phase);
				}
				m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			}
			subdiv = subdiv.replace(ma.group(0).trim(), "PHASE");
		}
		
		subdiv = subdiv.replaceFirst("(?is)\\bMRD\\s*[A-Z]-\\d+-\\d+\\s*$", "");	//TREFFER ACRES MRD S-67-89 (CO Larimer PRI R1320009)
		p = Pattern.compile("(\\d+\\s+\\w+)\\s+SUB(?:DIVISION)?");					//1107 JUNIPER SUB (CO Boulder PRI R0108582)
		ma = p.matcher(subdiv);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile("(.*?)\\s*\\b(ADD(ITIO)?N?|UN(ITS?)?|LO?TS?|PH(ASE)?|PA?RCE?L|TR(ACTS?)?|REV(ISION)?|"
					+ "REP(L(AT)?)?|APT|CONDO(M?INIUM)?S?|S/D|(RE\\s*)?SUB(DIVISION)?|BL(OC)?KS?|\\d+\\w+?|PART|PT|[NSEW]\\s*\\d+|[NS][EW]|ANNEX)\\b.*");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		subdiv = subdiv.replaceAll("\\s*SUBDIVISION\\s*(?:NAME)?\\s*|\\s*\\bSUB\\b\\s*", "");
		subdiv = subdiv.replaceFirst("- ", "").trim();
		subdiv = surveyForms + " " + subdiv;
		subdiv = subdiv.replaceFirst("\\([^\\)]*\\)?", ""); // fix for bug #3060
		subdiv = subdiv.replaceAll("[-#,]$", "");
		subdiv = subdiv.replaceFirst("\\s*\\*\\s*", " ");
		subdiv = subdiv.replaceFirst(",?\\s*\\bSEC(?:TION|\\.)?\\s*$", "").trim();// B9286
																					// -
																					// issue
																					// 4
		subdiv = subdiv.replaceFirst("(?is)\\bCO-OP.*$", "").trim();	//SUGAR CREEK ESTATES CO-OP UR0/0 (FL Manatee PRI 14021-2430-7)
		subdiv = subdiv.replaceFirst("(?is)\\bRESUBD\\s+OF\\s*$", "");	//WESTMOORLAND RESUBD OF (CO Adams PRI R0104679)
		subdiv = subdiv.replaceFirst("(?is)\\s*&\\s*$", "");
		subdiv = subdiv.replaceFirst("(?is)(.+[NSEW])\\s+S$", "$1");	//CUMBERLAND PLACE N S (TN Sumner 148I-A-020.00)
		subdiv = subdiv.replaceFirst("(?is)^\\s*BEG\\s*$", "");			//MO Clay PRI 07-80-34-000-000-009-000
		subdiv = subdiv.replaceFirst("(?is)\\b+MISC\\s*$", "");			//RIVERSIDE MISC (MO Clay PRI 23-20-04-200-006-004-001)
		subdiv = subdiv.replaceFirst("(?is)\\b+OR\\s+UNKNOWN\\s*$", "");//M & B OR UNKNOWN (CO Fremont 9300001741)
		if (subdiv.endsWith("/")) {
			subdiv = subdiv.substring(0, subdiv.length() - 1);
		}

		if (subdiv.length() != 0) {
			if (filling.length() != 0) {
				subdiv = (subdiv.trim() + " " + "Filing " + filling.replaceAll("^0+", "")).trim();
			}

			if (crtState.equals("CO") && crtCounty.equals("Summit")) {
				subdiv = cleanSubdivNameCOSummitAOLike(subdiv);
			}

			m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			if (legal.contains("CONDO"))
				m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
		}

		String a[] = { "X", "L", "C", "D", "M" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, a); // convert
																	// roman
																	// numbers
																	// to
																	// arabics

		// extract unit from legal description
		p = Pattern.compile("\\bUN(?:ITS?)?\\s+(\\d+\\s*-?\\s*[A-Z]|[-\\d\\s]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(1).replaceFirst("^0+(\\w.*)", "$1"));
		}

		// extract tract
		p = Pattern.compile("\\bTR(?:ACTS?)? (\\d+|\\w( \\w)*|\\w\\w)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionTract", ma.group(1).replaceFirst("^0+(\\w+)", "1"));
		}

		// extract section from legal description
		p = Pattern.compile("\\bSEC(?:TION)? (\\d+|\\w( \\w)*|\\w\\w)\\b");
		ma.usePattern(p);
		ma.reset();

		List<List> body = new ArrayList<List>();

		if (ma.find()) {
			String sec = (String) m.get("PropertyIdentificationSet.SubdivisionSection");
			String twn = (String) m.get("PropertyIdentificationSet.SubdivisionTownship");
			String rgn = (String) m.get("PropertyIdentificationSet.SubdivisionRange");

			if (sec != null && sec.length() != 0) {
				List<String> line = new ArrayList<String>();
				line.add(sec);
				if (twn == null)
					twn = "";
				line.add(twn);
				if (rgn == null)
					rgn = "";
				line.add(rgn);
				body.add(line);

				Map<String, String[]> map = new HashMap<String, String[]>();
				map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
				map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
				map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

				String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };
				ResultTable pis = new ResultTable();
				pis.setHead(header);
				pis.setBody(body);
				pis.setMap(map);
				m.put("PropertyIdentificationSet", pis);
				m.remove(PropertyIdentificationSetKey.SUBDIVISION_SECTION.getKeyName());
				m.remove("PropertyIdentificationSet.SubdivisionTownship");
				m.remove("PropertyIdentificationSet.SubdivisionRange");
			}

			// m.put("PropertyIdentificationSet.SubdivisionSection",
			// ma.group(1).trim().replaceFirst("\\A0+", ""));
		}

		// extract section-township-range from legal description // B 4212
		p = Pattern.compile("(?is)\\b(\\d+)\\s*-\\s*(\\d+)\\s*-\\s*(\\d+)\\b");
		ma.usePattern(p);
		if (ma.find()) {
			ResultTable pis = (ResultTable) m.get("PropertyIdentificationSet");
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\w.*)", "$1"));
			line.add(ma.group(2).replaceFirst("^0+(\\w.*)", "$1"));
			line.add(ma.group(3).replaceFirst("^0+(\\w.*)", "$1"));
			List<List> bodyN = new ArrayList<List>();
			bodyN.add(line);

			if (pis != null) {
				String bodyPIS[][] = pis.getBodyRef();
				if (bodyPIS.length != 0) {
					for (int i = 0; i < bodyPIS.length; i++) {
						line = new ArrayList<String>();
						for (int j = 0; j < pis.getHead().length; j++) {
							line.add(bodyPIS[i][j]);
						}
						bodyN.add(line);
					}
				}
			}
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
			map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
			map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

			String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };
			ResultTable pisN = new ResultTable();
			pisN.setHead(header);
			pisN.setBody(bodyN);
			pisN.setMap(map);
			m.put("PropertyIdentificationSet", pisN);
		}

		String lot = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
		if (lot != null) {
			lot = lot.replaceAll("(?i)([a-z]{1,2})-([0-9]+)", "$1$2");
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}

		String block = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
		if (block != null) {
			block = block.replaceAll("(?i)([a-z]{1,2})-([0-9]+)", "$1$2");
			block = StringUtils.removeLeadingZeroes(block);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		if (crtState.equals("TN") && crtCounty.equals("Sumner")) {
			cleanLegalTNSumnerNB(m);
		}
	}
	
	/**
	 * Tries to get abstract number and cleans the legal if found
	 * @param stateId
	 * @param legal
	 */
	public static SimpleParseTokenResult getAbsNoFromLegal(int stateId, String legal) {
		SimpleParseTokenResult simpleParseTokenResult = new SimpleParseTokenResult();
		simpleParseTokenResult.setInitialSource(legal);
		Matcher matcher;
		// extract abstract from legal description
		String absNo = "";
		Pattern absNoPatternCompiled = Pattern.compile("(?is)\\b(ABST?(?:RACT)?|(?:(?<!C/)A))\\s*[:-]?\\s*(\\d+)(?:\\s+SUR\\s*[:-]?\\s*\\d+)?\\b");
		matcher = absNoPatternCompiled.matcher(legal);
		if (matcher.find()) {
			absNo = matcher.group(2).replaceAll("(?is)\\A0+(\\d+)", "$1");
			// clean the legal from the things we parsed
			legal = legal.replaceAll(matcher.group(), "");
			legal = legal.replace(matcher.group(2), "");
		} else if (stateId == StateContants.TX) {
			absNoPatternCompiled = Pattern.compile("(?is)\\bAW0*(\\d+)\\b");
			matcher = absNoPatternCompiled.matcher(legal);
			if (matcher.find()) {
				absNo = matcher.group(1);
				legal = legal.replaceAll(matcher.group(), "");
			}
		}
		absNo = absNo.replaceAll("\\s*&\\s*", " ").trim();
		if (absNo.length() != 0) {
			absNo = LegalDescription.cleanValues(absNo, false, true);
		}
		simpleParseTokenResult.setTokenParsed(absNo);
		simpleParseTokenResult.setFinalSource(legal);
		return simpleParseTokenResult;
	}
	
	public static SimpleParseTokenResult getAcresFromLegal(int stateId, String legal) {
		SimpleParseTokenResult simpleParseTokenResult = new SimpleParseTokenResult();
		simpleParseTokenResult.setInitialSource(legal);
		
		Pattern pt = Pattern.compile("ACRES?\\s*(" + FLOAT_NUMBER_PATTERN + ")");
        Matcher m = pt.matcher(legal);
        
        String acres = null;
        if (m.find()) {
        	acres = m.group(1).replaceFirst("^0+", "");
        	legal = legal.replace(m.group(), "");
        } else {
        	pt = Pattern.compile("(" + FLOAT_NUMBER_PATTERN + ")\\s*ACRES?");
            m = pt.matcher(legal);
            if (m.find()) {
            	acres = m.group(1).replaceFirst("^0+", "");
            	legal = legal.replace(m.group(), "");
            }
        }
        
		simpleParseTokenResult.setTokenParsed(acres);
		simpleParseTokenResult.setFinalSource(legal);
		return simpleParseTokenResult;
	}
	
	public static SimpleParseTokenResult getTractFromLegal(int stateId, String legal) {
		SimpleParseTokenResult simpleParseTokenResult = new SimpleParseTokenResult();
		simpleParseTokenResult.setInitialSource(legal);
		
		Pattern pt = Pattern.compile("(?is)\\bTR(?:ACTS?)? (\\d+(?!-))\\b");
        Matcher m = pt.matcher(legal);
        
        String tract = "";
        if (m.find()) {
        	tract = m.group(1).replaceFirst("^0+", "");
        	legal = legal.replace(m.group(), "");
        } 

        if (org.apache.commons.lang.StringUtils.isNotEmpty(tract)) {
        	tract = LegalDescription.cleanValues(tract, false, true);
		}
        
		simpleParseTokenResult.setTokenParsed(tract);
		simpleParseTokenResult.setFinalSource(legal);
		return simpleParseTokenResult;
	}

	public static String cleanSubdivNameCOSummitAOLike(String subdivisionName) {
		subdivisionName = subdivisionName.replaceFirst("\\s*#\\s*\\d*\\s*$", "");
		return subdivisionName;
	}

	public static void cleanLegalTNSumnerNB(ResultMap m) {
		String notAvailablePattern = "(?i)N\\s*/?\\s*(?:A|R)|/(?:A|R)?";

		String lot = org.apache.commons.lang.StringUtils.defaultString((String) m.get(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName())).trim();
		if (lot.matches("(?i)LIST")) {
			m.remove(PropertyIdentificationSetKey.SUBDIVISION_LOT_NUMBER.getKeyName());
		}

		String block = org.apache.commons.lang.StringUtils.defaultString((String) m.get(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName())).trim();
		if (block.matches(notAvailablePattern)) {
			m.remove(PropertyIdentificationSetKey.SUBDIVISION_BLOCK.getKeyName());
		}
		String platBook = org.apache.commons.lang.StringUtils.defaultString((String) m.get(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName())).trim();
		if (platBook.matches(notAvailablePattern)) {
			m.remove(PropertyIdentificationSetKey.PLAT_BOOK.getKeyName());
		}
		String platNo = org.apache.commons.lang.StringUtils.defaultString((String) m.get(PropertyIdentificationSetKey.PLAT_NO.getKeyName())).trim();
		if (platNo.matches(notAvailablePattern)) {
			m.remove(PropertyIdentificationSetKey.PLAT_NO.getKeyName());
		}
	}

	public static String cleanLegalGenericDASLRV(String legal) {
		legal = legal.replaceAll("\\s+AND\\s+", " ");
		legal = legal.replaceAll("(?<=\\d)\\s+TO\\s+(?=\\d)", "-");
		legal = legal.replaceAll(",\\s*", " ");
		legal = legal.replaceAll("\\s*\\b[SWNE]+ \\d+((\\.|/)\\d+)?", "");
		legal = legal.replaceAll("\\s*\\b\\d+(\\.|/)\\d+\\b", "");
		return legal;
	}

	protected static String cleanLegalFLOkaloosaDASLRV(String legal) {
		legal = legal.replaceAll("\\s+AND\\s+", " ");
		legal = legal.replaceAll("\\s+TO\\s+", "-");
		legal = legal.replaceAll(",\\s*", " ");
		return legal;
	}

	public static void legalFLOkaloosaDASLRV(ResultMap m, long searchId, boolean parseCross) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		String parcelID = (String) m.get("PropertyIdentificationSet.ParcelID");

		String subdivName = ""; // always will be empty
		String section = "";
		String township = "";
		String range = "";
		String lot = "";
		String block = "";
		String unit = "";
		String phase = "";
		String platBook = "";
		String platPage = "";

		if (parcelID != null) {
			int length = parcelID.length();
			if (length >= 2) {
				section = parcelID.substring(0, 2).replaceAll("00", "0");
			}
			if (length >= 4) {
				township = parcelID.substring(2, 4);
			}
			if (length >= 6) {
				range = parcelID.substring(4, 6);
			}
			if (length >= 14) {
				block = parcelID.substring(10, 14).replaceAll("0", "");
			}
			if (length >= 18) {
				lot = parcelID.substring(14, 17);
				lot = lot.replaceAll("[0]*(.*)", "$1");
			}
		}
		if (StringUtils.isEmpty(legal)) {
			return;
		}
		legal = legal.replaceAll("&", " ");
		legal = legal.replaceAll("\\bORDN #?[\\d-]+\\s*", "");
		legal = legal.replaceAll("\\bNO\\b\\s*", "");
		legal = legal.replaceAll("(\\d+)(ST|ND|RD|TH) UNIT", "UNIT $1");
		legal = legal.replaceAll("\\b(A )?REPLAT( OF)?\\b\\s*", "");
		legal = legal.replaceAll("\\bTHRU\\b", "-");
		legal = cleanLegalFLOkaloosaDASLRV(legal);
		String origLegal = legal;
		String a[] = { "X", "L", "C", "D", "M" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, a); // convert
																	// roman
																	// numbers
																	// to
																	// arabics
		legal = replaceNumbers(legal);

		// try to extract section township range
		if (StringUtils.isEmpty(parcelID)) {
			String secTwnShipRange1 = "(?i)\\bS?E?C?(?:TION)?\\s*([\\d]+-?\\s*\\d*)\\s*(T(?:OWNSHIP)?[0-9]+[NSEW]*)?\\s*(R(?:ANGE)?[0-9]+[NSEW]*)";
			String secTwnShipRange2 = "(?i)\\bS?E?C?(?:TION)?\\s*([\\d]+-?\\s*\\d*)\\s*(T(?:OWNSHIP)?[0-9]+[NSEW]*)\\s*(R(?:ANGE)?[0-9]+[NSEW]*)?";
			String secTwnShipRange3 = "(?i)\\bT(?:OWNSHIP)?([0-9]+[NSEW]*)\\s*(R(?:ANGE)?[0-9]+[NSEW]*)?\\s*(SE?C?(?:TION)?\\s*[\\d]+-?\\s*\\d*)?";
			String secTwnShipRange4 = "(?i)\\bR(?:ANGE)?([0-9]+[NSEW]*)\\s*(T(?:OWNSHIP)?[0-9]+[NSEW]*)?\\s*(SE?C?(?:TION)?\\s*[\\d]+-?\\s*\\d*)?";
			String secTwnShipRange5 = "(?i)\\bSEC(?:TION)?\\s*([\\d][\\d ]+-?\\s*\\d*)";

			Pattern pat1 = Pattern.compile(secTwnShipRange1);
			Pattern pat2 = Pattern.compile(secTwnShipRange2);
			Pattern pat3 = Pattern.compile(secTwnShipRange3);
			Pattern pat4 = Pattern.compile(secTwnShipRange4);
			Pattern pat5 = Pattern.compile(secTwnShipRange5);

			Matcher mat1 = pat1.matcher(legal);
			Matcher mat2 = pat2.matcher(legal);
			Matcher mat3 = pat3.matcher(legal);
			Matcher mat4 = pat4.matcher(legal);
			Matcher mat5 = pat5.matcher(legal);

			if (mat1.find()) {
				section = StringUtils.isEmpty(section) ? mat1.group(1) : section;
				township = StringUtils.isEmpty(township) ? mat1.group(2) : township;
				range = StringUtils.isEmpty(range) ? mat1.group(3) : range;
			}
			if (mat2.find()) {
				section = StringUtils.isEmpty(section) ? mat2.group(1) : section;
				township = StringUtils.isEmpty(township) ? mat2.group(2) : township;
				range = StringUtils.isEmpty(range) ? mat2.group(3) : range;
			}
			if (mat3.find()) {
				section = StringUtils.isEmpty(section) ? mat3.group(3) : section;
				township = StringUtils.isEmpty(township) ? mat3.group(1) : township;
				range = StringUtils.isEmpty(range) ? mat3.group(2) : range;
			}
			if (mat4.find()) {
				section = StringUtils.isEmpty(section) ? mat4.group(3) : section;
				township = StringUtils.isEmpty(township) ? mat4.group(2) : township;
				range = StringUtils.isEmpty(range) ? mat4.group(1) : range;
			}
			if (mat5.find()) {
				section = StringUtils.isEmpty(section) ? mat5.group(1) : section;
				township = "";
				range = "";
			}
			section = section == null ? "" : section;
			township = township == null ? "" : township;
			range = range == null ? "" : range;

			section = section.replaceFirst("(?i)\\bSE?C?(?:TION)?\\s*", "");
			section = section.trim();
			section = section.replaceFirst("\\-$", "");

			township = township.replaceFirst("TOWNSHIP", "");
			township = township.replaceFirst("township", "");
			township = township.replaceFirst("[Tt]", "");

			range = range.replaceFirst("RANGE", "");
			range = range.replaceFirst("range", "");
			range = range.replaceFirst("[Rr]", "");

		}
		List<List> body = getSTRFromMap(m); // first add sec-twn-rng extracted
											// from XML specific tags, if any
											// (for DT use)
		if (!StringUtils.isEmpty(section) || (!StringUtils.isEmpty(range) && !StringUtils.isEmpty(township))) {
			List<String> line = new ArrayList<String>();
			line.add(section);
			line.add(township);
			line.add(range);
			body.add(line);
		}
		saveSTRInMap(m, body);

		{ // parsing lot from legal also
			lot = lot == null ? "" : lot;
			Pattern p = Pattern.compile("\\bLO?T?S?\\s*([\\d][\\d\t ]*-?[\\s\\d]*)($|[ \n\t\r])");
			Matcher ma = p.matcher(legal);
			ma.reset();
			String lotRef = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			lotRef = lotRef == null ? "" : lotRef;
			lot = lot + " " + lotRef;
			while (ma.find()) {
				lot = lot + " " + ma.group(1).replace("&", "-");
			}
			lot = lot.trim();
			lot = lot.replaceFirst("-$", "");
			if (lot.length() != 0) {
				lot = LegalDescription.cleanValues(lot, true, true);
				m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			} else {
				Pattern p1 = Pattern.compile("^\\b([1-9][0-9 -]*)\\sBL(?:OC)?K?S?\\s+((\\d+|\\b[A-Z]\\b|-|\\s)+)");
				Matcher ma1 = p1.matcher(legal);
				if (ma1.find()) {
					String lot1 = ma1.group(1);
					if (!StringUtils.isEmpty(lot1)) {
						lot1 = LegalDescription.cleanValues(lot1, true, true);
						lot1 = lot1.trim();
						lot1 = lot1.replaceFirst("-$", "");
						m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot1);
					}
				}
			}
		}

		if (StringUtils.isEmpty(block)) {
			Pattern p = Pattern.compile("\\bBL(?:OC)?K?S?\\s+((\\d+|\\b[A-Z]\\b|-|\\s)+)");
			Matcher ma = p.matcher(origLegal);
			String firstBlk = "";
			block = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			block = block == null ? "" : block;
			while (ma.find()) {
				String newBlk = ma.group(1);
				if (block.length() == 0) {
					firstBlk = newBlk;
					block = firstBlk;
				} else if (firstBlk.trim().matches("\\d+") && newBlk.matches(".*[A-Z].*")) {

				} else {
					block = block + " " + newBlk;
				}
			}
			block = block.trim();
			block = block.replaceFirst("\\-$", "");
			if (block.length() != 0) {
				block = LegalDescription.cleanValues(block, false, true);
				m.put("PropertyIdentificationSet.SubdivisionBlock", block);
			}
		}

		{
			// extract phase from legal description
			Pattern p = Pattern.compile("\\bPH(?:ASE)?(( (\\d+\\w?|\\b[A-Z]\\b))+)\\b");
			Matcher ma = p.matcher(origLegal);
			if (ma.find()) {
				phase = ma.group(1).trim();
				m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			}
		}

		{ // extract tract
			String tract = ""; // can have multiple occurrences
			Pattern p = Pattern.compile("\\bTRACTS? (\\d+( \\d+)*|\\w( \\w)*|\\w\\w)\\b");
			Matcher ma = p.matcher(origLegal);
			while (ma.find()) {
				tract = tract + " " + ma.group(1);
			}
			tract = tract.trim();
			if (tract.length() != 0) {
				tract = LegalDescription.cleanValues(tract, true, true);
				m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			}
		}

		{ // extract building #
			Pattern p = Pattern.compile("\\b(?:BLDG|BUILDING) (?:NO )?(\\d+)\\b");
			Matcher ma = p.matcher(origLegal);
			if (ma.find()) {
				m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
			}
		}

		{
			// extract unit from legal description
			Pattern p = Pattern.compile("\\bUNITS?\\s*[#]?([-\\d\\s]+|\\d+-?[A-Z]|[A-Z\\d-]+)\\b");

			String tokens[] = { "X", "L", "C", "D", "M" };
			String tempLegal = Roman.normalizeRomanNumbersExceptTokens(origLegal, tokens);
			Matcher ma = p.matcher(tempLegal);

			if (ma.find()) {
				m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(1).replaceFirst("^0+(\\w.*)", "$1"));
			}
		}

		{
			// extract cross refs from legal description
			List<List> bodyCR = new ArrayList<List>();
			Pattern p = Pattern.compile("\\b(OR|COC|COR|[A-Z]EC)\\s+(\\d+)\\s*[/-]\\s*(\\d+)\\b");
			Matcher ma = p.matcher(legal);
			while (ma.find()) {
				List<String> line = new ArrayList<String>();
				line.add(ma.group(2));
				line.add(ma.group(3));
				line.add("");
				line.add("");
				bodyCR.add(line);
			}

			// 1278 PG 1050/1060
			p = Pattern.compile("\\b(\\d+)\\s*PG\\s*(\\d+)((\\s*/\\s*\\d+)+)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				List<String> line = new ArrayList<String>();
				line.add(ma.group(1));
				line.add(ma.group(2));
				line.add("");
				line.add("");
				bodyCR.add(line);
				String[] vecStr = ma.group(3).split("/");

				for (int i = 0; i < vecStr.length; i++) {
					if (!StringUtils.isEmpty(vecStr[i])) {
						line = new ArrayList<String>();
						line.add(ma.group(1));
						line.add(vecStr[i]);
						line.add("");
						line.add("");
						bodyCR.add(line);
					}
				}
			} else {
				// 1211 PG 1646
				p = Pattern.compile("\\b(\\d+)\\s*PG\\s*(\\d+)\\s*\\b");
				ma = p.matcher(legal);
				while (ma.find()) {
					List<String> line = new ArrayList<String>();
					line.add(ma.group(1));
					line.add(ma.group(2));
					line.add("");
					line.add("");
					bodyCR.add(line);
				}
			}

			// 1275 P 1642 B 1297 P 1472
			p = Pattern.compile("(?:B|\\b)(\\d+)\\s*P\\s*(\\d+)\\s*B?\\b");
			ma = p.matcher("A " + legal);
			while (ma.find()) {
				List<String> line = new ArrayList<String>();
				line.add(ma.group(1));
				line.add(ma.group(2));
				line.add("");
				line.add("");
				bodyCR.add(line);
			}

			// 2474/182 or 2474-182
			p = Pattern.compile("^\\s*(\\d+)\\s*[-/]\\s*(\\d+)\\s*\\b");
			ma = p.matcher(legal);
			while (ma.find()) {
				List<String> line = new ArrayList<String>();
				line.add(ma.group(1));
				line.add(ma.group(2));
				line.add("");
				line.add("");
				bodyCR.add(line);
			}
			if (parseCross) {
				saveCRInMap(m, bodyCR);
			}
		}

		// extract plat book & page from legal description
		// do not have yet examples

		if (!StringUtils.isEmpty(block)) {
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		if (!StringUtils.isEmpty(lot)) {
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		if (!StringUtils.isEmpty(unit)) {
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
		}
		if (!StringUtils.isEmpty(phase)) {
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
		}
		if (!StringUtils.isEmpty(platBook)) {
			m.put("PropertyIdentificationSet.PlatBook", platBook);
		}
		if (!StringUtils.isEmpty(platPage)) {
			m.put("PropertyIdentificationSet.PlatNo", platPage);
		}
		if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.SubdivisionName"))) {
			m.put("PropertyIdentificationSet.SubdivisionName", "");
		}

	}

	public static void legalTokenizerFLHillsboroughTR(ResultMap m, String legal) throws Exception {

		legalTokenizerFLHillsboroughDASLRV(m, legal);

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		Pattern p = Pattern.compile("\\b(OR|COC|[A-Z]EC)\\s+(\\d+)\\s*-\\s*(\\d+)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add(ma.group(3));
			line.add("");
			bodyCR.add(line);
		}

		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "InstrumentNumber" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });

			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			m.put("CrossRefSet", cr);
		}
	}

	public static void legalFLHillsboroughTR(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerFLHillsboroughTR(m, legal);
	}

	public static void legalGenericDASLRV(ResultMap m, long searchId) throws Exception {

		// TestParser.printSample(m);

		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();

		if ("FL".equals(crtState)) {
			if ("Walton".equals(crtCounty)) {
				legalFLWaltonDASLRV(m, searchId);
			} else if ("Broward".equals(crtCounty)) {
				legalFLBrowardDASLRV(m, searchId);
			} else if ("Pinellas".equals(crtCounty)) {
				legalFLPinellasDASLRV(m, searchId);
			} else if ("Palm Beach".equals(crtCounty)) {
				legalFLPalmBeachDASLRV(m, searchId);
			} else if ("Seminole".equals(crtCounty)) {
				legalFLSeminoleDASLRV(m, searchId);
			} else if ("Hernando".equals(crtCounty)) {
				legalFLHernandoDASLRV(m, searchId);
			} else if ("Brevard".equals(crtCounty)) {
				legalFLBrevardDASLRV(m, searchId);
			} else if ("Duval".equals(crtCounty)) {
				legalFLDuvalDASLRV(m, searchId);
			} else if ("Nassau".equals(crtCounty)) {
				legalFLNassauDASLRV(m, searchId);
			} else if ("Charlotte".equals(crtCounty)) {
				legalFLCharlotteDASLRV(m, searchId);
			} else if ("Volusia".equals(crtCounty)) {
				legalFLVolusiaDASLRV(m, searchId);
			} else if ("Sarasota".equals(crtCounty)) {
				legalFLSarasotaDASLRV(m, searchId);
			} else if ("Bay".equals(crtCounty)) {
				legalFLBayDASLRV(m, searchId);
			} else if ("Franklin".equals(crtCounty)) {
				legalFLFranklinDASLRV(m, searchId);
			} else if ("Polk".equals(crtCounty)) {
				legalFLPolkDASLRV(m, searchId);
			} else if ("Santa Rosa".equals(crtCounty)) {
				legalFLSantaRosaDASLRV(m, searchId);
			} else if ("St. Johns".equals(crtCounty)) {
				legalFLStJohnsDASLRV(m, searchId);
			} else if ("Escambia".equals(crtCounty)) {
				legalFLEscambiaDASLRV(m, searchId);
			} else if ("Indian River".equals(crtCounty)) {
				legalFLIndianRiverDASLRV(m, searchId);
			} else if ("Lee".equals(crtCounty)) {
				legalFLLeeDASLRV(m, searchId);
			} else if ("Okaloosa".equals(crtCounty)) {
				legalFLOkaloosaDASLRV(m, searchId, false);
			} else if ("Sumter".equals(crtCounty)) {
				legalFLSumterDASLRV(m, searchId);
			} else if ("Jackson".equals(crtCounty)) {
				FLJacksonRV.legalFLJacksonDASLRV(m, searchId);
			} else if ("Alachua".equals(crtCounty)) {
				legalFLAlachuaDASLRV(m, searchId);
			} else if ("Okeechobee".equals(crtCounty)) {
				FLOkeechobeeRV.legalFLOkeechobeeDASLRV(m, searchId);
			} else if ("Collier".equals(crtCounty)) {
				FLCollierRV.legalFLCollierDASLRV(m, searchId);
			} else if ("Levy".equals(crtCounty)) {
				FLLevyTR.parseLegalDescription(m);
			} else if ("Martin".equals(crtCounty)) {
				legalFLMartinTR(m, searchId);
			} else if ("Hendry".equals(crtCounty)) {
				FLHendryTR.parseLegalFLHendryTR(m, searchId);
			} else {
				legalFLHillsboroughDASLRV(m, searchId, false);
			}
		}
	}

	public static void legalFLBrowardDASLRV(ResultMap m, long searchId) throws Exception {
		FLBrowardRV.legalFLBrowardDASLRV(m, searchId);
	}

	public static void legalFLPinellasDASLRV(ResultMap m, long searchId) throws Exception {
		// it's a copy-paste from Hillsborough RV, except the subdiv name is not
		// extracted

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legal = legal.replaceAll("&", " ");

		legal = legal.replaceAll("\\bORDN #?[\\d-]+\\s*", "");
		legal = legal.replaceAll("\\bNO\\b\\s*", "");
		legal = legal.replaceAll("(\\d+)(ST|ND|RD|TH) UNIT", "UNIT $1");
		legal = legal.replaceAll("\\b(A )?REPLAT( OF)?\\b\\s*", ""); // fix for
																		// bug
																		// #1998
		legal = legal.replaceAll("\\bTHRU\\b", "-");

		// String subdiv = legal; - don't extract subdivision name

		String origLegal = legal;
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
																					// to
																					// arabics
		legal = replaceNumbers(legal);
		// extract section, township and range from legal description
		Pattern p = Pattern.compile("\\bSEC (\\d+(?:-\\d+)?)(?:-|\\s)(\\d+)(?:-|\\s)(\\d+)\\b");
		Matcher ma = p.matcher(legal);

		List<List> body = new ArrayList<List>();

		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add(ma.group(3));
			body.add(line);
		}
		if (body.isEmpty()) {
			p = Pattern.compile("\\bSEC(?:TION)? (\\d+|\\w( \\w)*|\\w\\w)\\b");
			ma.usePattern(p);
			ma.reset();
			if (ma.find()) {
				m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(1));
			}
		} else {
			String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
			map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
			map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(body);
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
		}

		legal = cleanLegalGenericDASLRV(legal);
		origLegal = cleanLegalGenericDASLRV(origLegal);

		// extract tract
		String tract = ""; // can have multiple occurrences
		p = Pattern.compile("\\bTRACTS? (\\d+( \\d+)*|\\w( \\w)*|\\w\\w)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(1);
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, true, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}

		// extract building #
		p = Pattern.compile("\\b(?:BLDG|BUILDING) (?:NO )?(\\d+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
		}

		// extract plat book & page from legal description
		p = Pattern.compile("\\bPB (\\d+) PG (\\d+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
			m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
		} else {
			p = Pattern.compile("\\b(?:PLAT|PB|REC) (\\d+)-(\\d+)\\b");
			ma.usePattern(p);
			ma.reset();
			if (ma.find()) {
				m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
				m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
			}
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("\\b(OR|COC|[A-Z]EC)\\s+(\\d+)\\s*-\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add(ma.group(3));
			line.add("");
			bodyCR.add(line);
		}

		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "InstrumentNumber" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });

			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			m.put("CrossRefSet", cr);
		}

		// extract phase from legal description
		p = Pattern.compile("\\bPH(?:ASE)?(( (\\d+\\w?|\\b[A-Z]\\b))+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(1).trim());
		}

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		p = Pattern.compile("\\bLOTS?\\s+([-\\d\\s]+)");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
		}
		lot = lot.trim();
		lot = lot.replaceFirst("\\-$", "");
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, true, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}

		// extract unit from legal description
		p = Pattern.compile("\\bUNITS?\\s+([-\\d\\s]+|\\d+-?[A-Z]|[A-Z\\d-]+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(1).replaceFirst("^0+(\\w.*)", "$1"));
		}

		// extract block from legal description
		String block = ""; // can have multiple occurrences
		p = Pattern.compile("\\bBL(?:OC)?KS?\\s+((\\d+|\\b[A-Z]\\b|-|\\s)+)");
		ma = p.matcher(origLegal);
		String firstBlk = "";
		while (ma.find()) {
			String newBlk = ma.group(1);
			if (block.length() == 0) {
				firstBlk = newBlk;
				block = firstBlk;
			} else if (firstBlk.trim().matches("\\d+") && newBlk.matches(".*[A-Z].*")) {

			} else {
				block = block + " " + newBlk;
			}
		}
		block = block.trim();
		block = block.replaceFirst("\\-$", "");
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
	}

	@SuppressWarnings("unchecked")
	public static void legalTokenizerFLPalmBeachDASLRV(ResultMap m, String legal) throws Exception {

		// legal corrections
		legal = legal.replaceAll("(\\w)(?<!SE)COND\\b", "$1 COND");
		legal = legal.replaceAll("\\bCOND(?:NO )?(\\d+)\\b", "COND $1");
		legal = legal.replaceAll("\\bCONDSEC\\b", "COND SEC");
		legal = legal.replaceAll("(\\d)(LO?T|BLK)S?\\b", "$1 $2");
		legal = legal.replaceAll("\\b([A-Z]+)(LO?TS? \\d+)\\b", "$1 $2");
		legal = legal.replaceAll("\\b(SEC|PH)(\\d+)\\b", "$1 $2");
		legal = legal.replaceAll("(\\d)UNREC\\b", "$1 UNREC");
		legal = legal.replaceAll("\\b([A-Z]+)([SWEN]{2}LY \\d+)", "$1 $2");

		legal = legal.replaceAll("\\b(\\d+)(?:ST|ND|RD|TH) (UNIT|SEC(?:TION)?)", "$2 $1");
		legal = legal.replaceAll("\\b(A )?REPL(AT)? OF( A)?\\b\\s*", "");
		legal = legal.replaceAll("\\b?REPL(AT)?\\b\\s*", "");
		legal = legal.replaceAll("(\\w)(UNIT|PL)\\b", "$1 $2"); // CHANTECLAIR
																// VILLAS COND
																// NO ONEUNIT
																// 12-A; PGA
																// NATIONAL GOLF
																// CLUB
																// ESTATESPL 2
																// LT 20 BLK 51
		legal = legal.replaceAll("\\s*\\bTHRU\\b\\s*", "-");
		legal = legal.replaceAll("(\\d)\\s+TO\\s+(\\d)", "$1-$2");
		legal = legal.replaceAll("\\b(A )?POR( OF)?\\b", "");
		legal = legal.replaceAll("\\bDESC( AS| IN)?\\b", "");
		legal = legal.replaceAll("\\b\\d+/\\d+ INT\\b", "");
		legal = legal.replaceAll("\\bLESS\\b", "");
		legal = legal.replaceAll("\\bCORR\\b", "");
		legal = legal.replaceAll("\\b(THAT )?PART( OF)?\\b", "SUB");
		legal = legal.replaceAll("\\b(TH )?PT( OF)?\\b", "SUB");
		legal = legal.replaceAll("\\bGOV\\b", "");
		legal = legal.replaceAll("\\bPOD \\d+[A-Z]?( AT)?\\b", "");
		legal = legal.replaceAll("\\bBLDG (LT \\d)", "$1"); // RAINBERRY BAY
															// SECTION 6 UNIT D
															// BLDG LT 105

		// extract section, township and range from legal description
		Pattern p = Pattern.compile("(?<!UNIT )\\b(\\d+)-(\\d+)-(\\d+)\\b");
		Matcher ma = p.matcher(legal);
		List<List> bodySTR = new ArrayList<List>();
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add(ma.group(3));
			bodySTR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}

		legal = legal.trim().replaceAll("\\s{2,}", " ");
		String subdiv = legal;
		String lotPatt = "\\bLO?TS?\\s+(\\d+(?:-?[A-Z])?(?:[\\s/,&-]+\\d+(?:-?[A-Z])?)*|[A-Z]{1,2}(?:-?\\d+)?(?:[\\s/,&-]+[A-Z])*)\\b";
		String blkPatt = "\\b\\d+(?:-?[A-Z]\\b)?";
		String blkPatt1 = "\\bBL(?:OC)?KS?\\s+(" + blkPatt + "(?:\\s*[&,\\s-]\\s*" + blkPatt + ")*)\\b(?![/-]\\d)";
		String blkPatt2 = "\\bBL(?:OC)?KS?\\s+([A-Z](?:\\s*[&,-]\\s*[A-Z]{1,2})*|[A-Z]{1,2}(?:-?\\d+)?|[A-Z]\\d+)\\b(?![/-]\\d)";
		String unitPatt = "\\d+(?:-?\\d*[A-Z](?:-[A-Z])?)?";

		// cleanup legal descr before extracting subdivision name
		subdiv = subdiv.replaceAll("^\\s*?ADD(ITIO)?N? \\d+( TO)?\\b\\s*", "");
		subdiv = subdiv.replaceAll("^\\s*(\\d+(ST|ND|RD|TH) )?ADD(ITIO)?N?( TO)?\\b\\s*", "");
		subdiv = subdiv.replaceFirst("^\\s*(OF )?PL(ATS?)?\\s*(\\bNO )?\\d*\\b\\s*(OF )?", "");
		subdiv = subdiv.replaceFirst("^\\s*(RE)?SUB\\b\\s*(OF )?", "");
		subdiv = subdiv.replaceFirst("^\\s*" + blkPatt1, "");
		subdiv = subdiv.replaceFirst("^\\s*" + blkPatt2, "");
		subdiv = subdiv.replaceFirst("^\\s*UNIT( NO)?\\s+" + unitPatt, "");
		subdiv = subdiv.replaceFirst("^\\s*(OF )?PL(ATS?)?\\s*(\\bNO )?\\d*\\b\\s*(OF )?", "");
		subdiv = subdiv.trim();
		String subdivTemp = "";

		p = Pattern.compile(".*\\b(?:UNREC|AMENDED|AMND) PL(?:AT)? OF (.*)");
		ma = p.matcher(subdiv);
		boolean foundPl = false;
		if (ma.find()) {
			subdiv = ma.group(1);
			foundPl = true;
		}

		p = Pattern.compile(".*\\bCOND(?:OMINIUM)? \\d+ OF (.*)");
		ma = p.matcher(subdiv);
		boolean foundCondo = false;
		if (ma.find()) {
			subdiv = ma.group(1);
			foundCondo = true;
		}

		p = Pattern
				.compile("(.*?)\\s*\\b(LO?TS?|UNIT|PL(ATS?)?\\d*|(A )?COND(O(MINIUM)?)?|ADD(ITION)?|ADDN?|PAR(CEL)?S? \\w+|PH(ASE)?|SEC(TION)?S?|(RE)?SUB(DIVISION)?|REV(ISED)?|(IN )?[SWNE]{1,2} \\d+((\\.|/)\\d+)?|[SWEN]{1,2}LY \\d+|TR(ACT)?S?|BL(?:OC)?KS?|BLDG|APT|PB|PB?\\d+P\\d+)\\b.*");
		ma = p.matcher(subdiv);
		if (ma.find()) {
			subdivTemp = ma.group(1);
		} else if (foundPl || foundCondo) {
			subdivTemp = subdiv;
		}

		if (subdivTemp.length() != 0) {
			subdivTemp = subdivTemp.replaceFirst("\\s+#\\d+\\s*$", "");

			// remove last token from subdivision name if it is a number (as
			// roman or arabic)
			p = Pattern.compile("(.*)\\s*\\b(\\w+)\\s*$");
			ma = p.matcher(subdivTemp);
			if (ma.find()) {
				String lastToken = ma.group(2);
				String[] exceptionTokens = { "I", "M", "C", "L", "D" };
				lastToken = Roman.normalizeRomanNumbersExceptTokens(lastToken, exceptionTokens);
				if (lastToken.matches("\\d+")) {
					subdivTemp = ma.group(1);
				} else {
					lastToken = replaceNumbers(lastToken);
					if (lastToken.matches("\\d+(ST|ND|RD|TH)?")) {
						subdivTemp = ma.group(1);
					}
				}
			}

			subdivTemp = subdivTemp.replaceFirst("\\s*\\bIN\\s*$", "");
			subdivTemp = subdivTemp.replaceAll("\\s*\\d+(ST|ND|RD|TH) REVISION\\s*$", "");
			subdivTemp = subdivTemp.replaceFirst("^OF\\s+", "");
			subdivTemp = subdivTemp.replaceFirst("\\bCOMM .*", "");
			subdivTemp = subdivTemp.replaceAll("\\b(AMENDED|AMND)\\b", "");
			subdivTemp = subdivTemp.replaceFirst("\\s*-\\s*$", "");
			subdivTemp = subdivTemp.replaceFirst("\\s*\\,\\s*$", "");
			subdivTemp = subdivTemp.replaceFirst("\\s*\\([^\\)]*\\)?\\s*$", "");
			subdivTemp = subdivTemp.replaceFirst("\\s*/[^/]*/\\s*$", "");
			subdivTemp = subdivTemp.replaceFirst("\\s*&?\\s+\\d+[A-Z]?\\s*$", "");
			subdivTemp = subdivTemp.replaceFirst("^\\s*\\,\\s*", "");
			subdivTemp = subdivTemp.replaceFirst("\\bUNREC\\s*", "");
			subdivTemp = subdivTemp.replaceFirst("\\bNO\\s*$", "");
			subdivTemp = subdivTemp.trim().replaceAll("\\s{2,}", "");

			if (subdivTemp.length() != 0) {
				m.put("PropertyIdentificationSet.SubdivisionName", subdivTemp);
				if (legal.matches(".*\\bCOND(O(MINIUM)?)?\\b.*"))
					m.put("PropertyIdentificationSet.SubdivisionCond", subdivTemp);
			}
		}

		legal = legal.replaceAll("\\bNO\\b\\s*", "");
		legal = legal.replaceAll("#(?=\\d)", "");
		legal = legal.replaceAll("\\s*\\b[SWNE]{1,2}\\s?\\d+((\\.|/)\\d+)?\\b(?!-)", "");
		legal = legal.replaceAll("\\s\\d+\\.\\d+\\b", "");
		legal = legal.replaceAll("\\bTO\\b", "-");
		String origLegal = legal;
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
																					// to
																					// arabics
		legal = replaceNumbers(legal);

		// extract section from legal description
		String patt = "(?:\\d+(?:-?[A-Z])?|[A-Z]\\d*)";
		p = Pattern.compile("\\bSEC(?:TION)?S? (" + patt + "(?:\\s*[&,\\s-]\\s*" + patt + ")*)\\b(?!/)");
		ma = p.matcher(legal);
		boolean foundSec = false;
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			String sec = ma.group(1);
			sec = sec.replaceAll("[&,]", " ").replaceAll("\\s{2,}", " ");
			line.add(sec);
			line.add("");
			line.add("");
			bodySTR.add(line);
			foundSec = true;
		}
		if (!foundSec) {
			p = Pattern.compile("\\b(\\d+)(?:ST|ND|RD|TH)? SEC(?:TION)?\\b");
			ma = p.matcher(legal);
			while (ma.find()) {
				List<String> line = new ArrayList<String>();
				line.add(ma.group(1));
				line.add("");
				line.add("");
				bodySTR.add(line);
			}
		}

		if (!bodySTR.isEmpty()) {
			String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
			map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
			map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(bodySTR);
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
		}

		// extract plat book & page from legal description
		List<List> bodyPlat = new ArrayList<List>();
		p = Pattern.compile("\\bPB?(\\d+)P(\\d+)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			bodyPlat.add(line);
		}
		p = Pattern.compile("\\bPB\\s*(\\d+) PG\\s*(\\d+)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			bodyPlat.add(line);
		}
		if (!bodyPlat.isEmpty()) {
			String[] header = { "PlatBook", "PlatNo" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("PlatBook", new String[] { "PlatBook", "" });
			map.put("PlatNo", new String[] { "PlatNo", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(bodyPlat);

			ResultTable pisSTR = (ResultTable) m.get("PropertyIdentificationSet");
			if (pisSTR != null) {
				pis = ResultTable.joinHorizontal(pis, pisSTR);
				map.putAll(pisSTR.map);
			}
			pis.setMap(map);

			m.put("PropertyIdentificationSet", pis);
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("\\b(OR)(\\d+)\\s*P(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add("");
			line.add(ma.group(3));
			bodyCR.add(line);
		}

		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "InstrumentNumber" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });

			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			// m.put("CrossRefSet", cr); - DO NOT PARSE CROSSREF FROM LEGAL
		}

		// extract tract
		String tract = ""; // can have multiple occurrences
		p = Pattern.compile("\\bTR(?:ACT)?S? (\\d+(?:-?[A-Z]?)(\\s*[&,\\s]\\s*\\d+)*|[A-Z]-?\\d+|\\w(\\s*[&,\\s-]\\s*\\w)*|\\w\\w|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(1);
		}
		tract = tract.trim();
		tract = tract.replaceAll("[&,]", " ");
		tract = tract.replaceAll("\\s{2,}", " ");
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}

		// extract building #
		String bldg = "";
		p = Pattern.compile("\\b(?:BLDG|BUILDING) (\\d+(?:-?[A-Z])?|[A-Z]{1,2}(?:-?\\d+)?)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			bldg = bldg + " " + ma.group(1);
		}
		bldg = bldg.trim();
		if (bldg.length() != 0) {
			bldg = LegalDescription.cleanValues(bldg, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
		}

		// extract phase from legal description
		p = Pattern.compile("\\bPH(?:ASE)?((?:\\s*[&,\\s-]\\s*(?:\\d+(?:-?[A-Z])?(?!-)|\\b[A-Z]\\b))+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			String phase = ma.group(1).trim();
			phase = phase.replaceAll("\\bI\\b", "1");
			phase = phase.replaceAll("[&,]", " ").trim().replaceAll("\\s{2,}", " ");
			phase = phase.replaceFirst("^0+(\\d+)", "$1");
			phase = phase.replaceFirst("^-", "");
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
		}

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		p = Pattern.compile(lotPatt);
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
		}
		lot = lot.trim();
		lot = lot.replaceFirst("\\-$", "");
		lot = lot.replace('/', '-');
		lot = lot.replaceAll("[&,]", " ");
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("\\b(?:UNITS?|APT|DWELLING)\\s+(" + unitPatt + "(?:\\s*&\\s*" + unitPatt
				+ ")*(?:[/-]\\d+(?:-?[A-Z])?)*|[A-Z](?:-?\\d+(?:-?[A-Z])?)?)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			String unitTemp = ma.group(1);
			unitTemp = unitTemp.replaceAll("(\\d+)-(\\d+)-(\\d+)", "$1@$2@$3");
			unitTemp = unitTemp.replaceFirst("^0+(\\w.*)", "$1");
			unit = unit + " " + unitTemp;
		}
		unit = unit.replaceAll("/", "-");
		unit = unit.replaceAll("&", " ");
		unit = unit.trim().replaceAll("\\s{2,}", " ");
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			unit = unit.replaceAll("@", "-");
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
		}

		// extract block from legal description
		String block = ""; // can have multiple occurrences
		p = Pattern.compile(blkPatt1);
		ma = p.matcher(origLegal);
		while (ma.find()) {
			block = block + " " + ma.group(1).replaceAll("[&,]", " ");
		}
		p = Pattern.compile(blkPatt2);
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			block = block + " " + ma.group(1).replaceAll("[&,]", " ");
		}
		block = block.trim().replaceAll("\\s{2,}", " ");
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
	}

	public static void legalFLPalmBeachDASLRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerFLPalmBeachDASLRV(m, legal);
	}

	public static void legalTokenizerFLSeminoleDASLRV(ResultMap m, String legal) throws Exception {

		// legal corrections
		legal = legal.replaceAll("\\bPG (\\d+ PGS? \\d+)\\b", "PB $1");
		legal = legal.replaceAll("\\b(PB \\d+) PB(S? \\d+)\\b", "$1 PG$2");
		legal = legal.replaceAll("\\bTWP TWP\\b", "TWP");
		// legal cleanup
		legal = legal.replaceAll("[&\\+]", " ");
		legal = legal.replaceAll("\\bLEG\\b", "");
		legal = legal.replaceAll("(?<=\\d)LEG\\b", "");
		legal = legal.replaceAll("\\bNO\\b\\s*", "");
		legal = legal.replaceAll("(\\d+)(?:ST|ND|RD|TH) (SEC(?:TION)?)\\b", "$2 $1");
		legal = legal.replaceAll("\\bREPLAT( OF)?\\b\\s*", "");
		legal = legal.replaceAll("(?<=\\d)(\\s*-\\s*| THRU | TO )(?=\\d)", "-");
		legal = legal.replaceAll("\\b\\w+-\\w+-\\w+(-\\w+)+\\b", "");
		legal = legal.replaceAll("\\b[SWEN]{1,2} \\d+([/\\.]\\d+)?( FT)? OF\\b", "");
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// subdivision name - not requested

		legal = legal.replaceAll("\\bA CONDO\\b", "");
		legal = legal.replaceAll("\\sAND\\s", " ");
		String origLegal = legal;
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
																					// to
																					// arabics
		legal = replaceNumbers(legal);
		// extract section, township and range from legal description
		List<List> body = new ArrayList<List>();
		Pattern p = Pattern.compile("\\bSEC (\\d+(?:-\\d+)?)(?:-| TWP |\\s)(\\d+[SWEN]?)(?:-| RGE |\\s)(\\d+[SWEN]?)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(2));
			line.add(ma.group(3));
			body.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}
		p = Pattern.compile("\\bSEC(?:TION)?S? (\\d+(?:-?[A-Z])?(?: \\d+)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			line.add("");
			line.add("");
			body.add(line);
		}
		if (!body.isEmpty()) {
			String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
			map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
			map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(body);
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
		}

		legal = cleanLegalGenericDASLRV(legal);
		origLegal = cleanLegalGenericDASLRV(origLegal);

		// extract tract
		String tract = ""; // can have multiple occurrences
		p = Pattern.compile("\\bTR(?:ACT)?S? (\\d+( \\d+)*|\\w( \\w)*|\\w\\w)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(1);
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}

		// extract building #
		p = Pattern.compile("\\b(?:BLDG|BUILDING) (?:NO )?(\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
		}

		// extract plat book & page from legal description
		p = Pattern.compile("\\bPB (\\d+) PGS? (\\d+(?:[\\s-]+\\d+[A-Z]?)*)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
			m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("\\b(ORB|DB)\\s+(\\d+) PG (\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add(ma.group(3));
			line.add(ma.group(1));
			bodyCR.add(line);
		}

		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "Book_Page_Type" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("Book_Page_Type", new String[] { "Book_Page_Type", "" });

			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			// m.put("CrossRefSet", cr); - DO NOT PARSE CROSSREF FROM LEGA
		}

		// extract phase from legal description
		p = Pattern.compile("\\bPH(?:ASES?)?(( (\\d+\\w?|\\b[A-Z]\\b))+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(1).trim());
		}

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		p = Pattern.compile("\\bLOTS?\\s+(\\d+[A-Z]?(?:[-\\s]+\\d+[A-Z]?)*|[A-Z])\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("\\bU(?:NITS?)?\\s+((?:\\d+|[A-Z])[-\\s]?(?:[A-Z]|\\d+[A-Z]?)|[A-Z\\d-]+)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			unit = unit + " " + ma.group(1).replaceFirst("^0+(\\w.*)", "$1");
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
		}

		// extract block from legal description
		String block = ""; // can have multiple occurrences
		p = Pattern.compile("\\bBL(?:OC)?KS?\\s+((\\d+\\b|\\b[A-Z]\\b|-|\\s)+)");
		ma = p.matcher(origLegal);
		String firstBlk = "";
		while (ma.find()) {
			String newBlk = ma.group(1);
			if (block.length() == 0) {
				firstBlk = newBlk;
				block = firstBlk;
			} else if (firstBlk.trim().matches("\\d+") && newBlk.matches(".*[A-Z].*")) {

			} else {
				block = block + " " + newBlk;
			}
		}
		block = block.trim();
		block = block.replaceFirst("\\-$", "");
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
	}

	public static void legalFLSeminoleDASLRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerFLSeminoleDASLRV(m, legal);
		// String parcelId = (String)
		// m.get("PropertyIdentificationSet.ParcelID");
		// if (parcelId == null) parcelId = "";
		// ro.cst.tsearch.test.TestParser.append("C:/SeminoleRVLegal_LEGAL_raw.txt",
		// legal + "  ::: " + parcelId);
	}

	public static void legalTokenizerFLHernandoDASLRV(ResultMap m, String legal) throws Exception {

		// legal cleanup
		legal = legal.replaceAll("SEC\\s*[NSEW]\\s*[NSEW]\\d+/\\d+\\s*OF\\s*", "");
		legal = legal.replaceAll("(SEC(?:TION)?)\\s*-([A-Z&&[^ENSW]])", "$1 $2"); // APN:221836311000000352
		legal = legal.replaceAll("(?:A?\\s*LOT\\s*(?:IN|\\d+\\.[\\d\\.X]+\\s*FT\\s*IN))", "");
		legal = legal.replaceAll("(\\b(?:N|S|E|W){1,2}\\s*\\d+/\\d+\\s*(?:OF|AKA|DES IN|[\\dA-Z]+\\s*FT|\\*\\* CONTINUED \\*\\* |FRAC)?|"
				+ "(?:LESS\\s*)?\\b[NSEW]\\b\\s*\\d+\\s*FT\\s*(?:(?:THERE)?OF(?:\\s*GOV|\\s*\\*\\* CONTINUED \\*\\*)?)?)", "");
		legal = legal.replaceAll("AND SUBJECT TO AN EASEMENT OVER THE \\*\\* CONTINUED \\*\\* ", "");
		legal = legal.replaceAll("\\b([\\d-]+)\\s*\\bAKA\\b\\s*", "$1 ");
		legal = legal.replaceAll("\\*\\* CONTINUED \\*\\*", "");
		legal = legal.replaceAll("\\bA TR [\\dX]+\\s*(?:FT)?\\s*MOL\\s*IN(?:\\s*THAT PT OF )?", "");
		legal = legal.replaceAll("\\+ A STRIP ADJ TO ", "");
		legal = legal.replaceAll("THAT PART OF", "");
		legal = legal.replaceAll("\\bCLASS\\s*\\d+\\s*SUB\\s*[\\d\\.]+\\s*AC MOL IN", "");
		legal = legal.replaceAll("\\bNO\\b\\s", "");
		legal = legal.replaceAll("(?is)\\b(\\d+)\\s+THRU\\s+(\\d+)\\s+AND\\s+(\\d+)\\s+THRU\\s+(\\d+)\\b", "$1-$2 & $3-$4");
		legal = legal.replaceAll("(?is)\\b(\\d+)\\s+THRU\\s+(\\d+)\\b", "$1-$2");
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// subdivision name - not requested
		legal = legal.replaceAll("\\bA CONDO\\b", "");
		legal = legal.replaceAll("\\sAND\\s", " ");
		Pattern p = Pattern.compile("([A-Z\\s]+)\\bSUB\\b");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionName", ma.group(1));
		}
		String origLegal = legal;
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
																					// to
																					// arabics
		legal = replaceNumbers(legal);

		// extract section, township and range from legal description
		List<List> body = new ArrayList<List>();
		p = Pattern.compile("\\bSEC (\\d+(?:-\\d+)?)(?:-| TWP |\\s)(\\d+[SWEN]?)(?:-| RGE |\\s)(\\d+[SWEN]?)\\b");
		ma.reset();
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(2));
			line.add(ma.group(3));
			body.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}

		p = Pattern.compile("\\bSEC(?:TION)?\\b\\s*([\\d&]+|[A-Z])");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add("");
			line.add("");
			body.add(line);
		}
		if (!body.isEmpty()) {
			String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
			map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
			map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(body);
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
		}

		legal = cleanLegalGenericDASLRV(legal);
		origLegal = cleanLegalGenericDASLRV(origLegal);

		// extract tract
		String tract = ""; // can have multiple occurrences
		p = Pattern.compile("\\bTR(?:ACT)?\\b\\s*([A-Z]|\\d+)");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(1);
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}

		// extract building #
		/*
		 * p = Pattern.compile(
		 * "\\b(?:BLDG|BUILDING) (?:NO )?(\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?)\\b"
		 * ); ma.usePattern(p); ma.reset(); if (ma.find()){
		 * m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1)); }
		 */

		// extract plat book & page from legal description
		p = Pattern.compile("\\bPB (\\d+) PGS? (\\d+(?:[\\s-]+\\d+[A-Z]?)*)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
			m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("\\b(ORB?)\\b\\s*(\\d+)\\s*PGS?\\s*([\\d\\s&]+)");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add(ma.group(3));
			line.add(ma.group(1));
			bodyCR.add(line);
		}

		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "Book_Page_Type" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("Book_Page_Type", new String[] { "Book_Page_Type", "" });

			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			m.put("CrossRefSet", cr);
		}

		// extract phase from legal description
		p = Pattern.compile("\\bPH(?:ASE)?\\b\\s*(\\d+|IV|III|II|I)");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(1).trim());
		}

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		p = Pattern.compile("\\bLOTS?\\s*([\\d-&\\s]+|[A-Z]-\\d+)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
		}
		lot = lot.replaceAll("&", "");
		lot = lot.trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("\\bUN(?:IT)?\\b\\s*(\\d+[A-Z]?)");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			unit = unit + " " + ma.group(1);
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
		}

		// extract block from legal description
		String block = ""; // can have multiple occurrences
		p = Pattern.compile("\\bBL(?:K|OCK)\\s*([A-Z]|\\d+|)");
		ma = p.matcher(origLegal);
		String firstBlk = "";
		while (ma.find()) {
			String newBlk = ma.group(1);
			if (block.length() == 0) {
				firstBlk = newBlk;
				block = firstBlk;
			} else if (firstBlk.trim().matches("\\d+") && newBlk.matches(".*[A-Z].*")) {

			} else {
				block = block + " " + newBlk;
			}
		}
		block = block.trim();
		block = block.replaceFirst("\\-$", "");
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}

		String remarks = (String) m.get("SaleDataSet.Remarks");

		if (remarks != null)
			legalTokenizerRemarksFLHernandoRV(m, remarks);
	}

	public static void legalRemarksFLHernandoRV(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;
		legalTokenizerRemarksFLHernandoRV(m, legal);
	}

	public static void legalTokenizerRemarksFLHernandoRV(ResultMap m, String remarks) throws Exception {
		// corrections
		remarks = remarks.replaceAll("\\bSH\\b", "SPRING HILL");
		remarks = remarks.replaceAll("\\bBCH\\b", "BEACH");
		remarks = remarks.replaceAll("\\bRDG\\b", "RIDGE");
		remarks = remarks.replaceAll("\\bMNR\\b", "MANOR");
		remarks = remarks.replaceAll("\\bRYL\\b", "ROYAL");
		remarks = remarks.replaceAll("\\bPK\\b", "PARK");
		remarks = remarks.replaceAll("\\bPTRFLD\\b", "POTTERFIELD");
		remarks = remarks.replaceAll("\\bRVR\\b", "RIVER");
		remarks = remarks.replaceAll("\\bCNTRY\\b", "COUNTRY");
		remarks = remarks.replaceAll("GRDN", "GARDEN");
		remarks = remarks.replaceAll("GRDNS", "GARDENS");
		remarks = remarks.replaceAll("ACRS?", "ACRES");
		remarks = remarks.replaceAll("W/W", "WEEKIWACHEE");
		remarks = remarks.replaceAll("RYL HGHLNDS", "ROYAL HIGHLANDS");
		remarks = remarks.replaceAll("HERN BCH", "HERNANDO BEACH");
		remarks = remarks.replaceAll("COAST RETS?", "COAST RETREATS");
		remarks = remarks.replaceAll("BRKRDG COMM?", "BROOKRIDGE COMMUNITY");
		remarks = remarks.replaceAll("HGH PNT MHS?", "HIGH POINT ");
		remarks = remarks.replaceAll("THE HTHR", "THE HEATHER");

		// cleanup remarks
		remarks = remarks.replaceAll("\\d+/\\d+\\s*INT\\s*", " ");
		remarks = remarks.replaceAll("\\b(?:NORTH|EAST|WEST|SOUTH)\\b", "");
		remarks = remarks.replaceAll("\\bSTATUS\\b", "");
		remarks = remarks.replaceAll("\\bMULTI? (?:LOTS?\\s*(?:[\\dA-Z]{4,}|SEE INSTRUMENT)?|PCL|PARCELS)\\b", "");
		remarks = remarks.replaceAll("&?SEE INSTR(?:UMENT)?", "");
		// remarks = remarks.replaceAll("\\d+-\\d+-CP-\\d+(?:-RT)?", "");
		remarks = remarks.replaceAll("(?:[\\d-A-Z]+\\s*)?\\bORD(?:ER)?(?:\\s*OF)?\\b", "");
		remarks = remarks.replaceAll("\\b[A-Z]&[A-Z]\\b", "");
		remarks = remarks.replaceAll("\\bCORRECTIVE\\s*\\b", "");
		remarks = remarks.replaceAll("\\bMULTI?\\s*TRACTS?\\b", "");
		remarks = remarks.replaceAll("\\b\\s*(?:PARCEL|PRCL|PARC)\\s*-?(?:[A-Z]\\s*|[\\d&-]+)\\b", " ");
		remarks = remarks.replaceAll("\\bVIL(?:LAGES)? AT HILL? N DALE\\s*", "");
		remarks = remarks.replaceAll("\\b(SEC(?:TION)?)\\b\\s*-([^NSEW])", "$1 $2");
		remarks = remarks.replaceAll("\\bSEC(?:TION)?\\b\\s*-?[NSEW]", "");
		remarks = remarks.replaceAll("\\b[NSEW]\\s*\\d+\\s*FT\\b", "");
		remarks = remarks.replaceAll("\\b(SEC\\s*\\d+)\\s+(T\\s*(?:\\d+[NSEW]))\\s+T(\\d+[NSEW])", "$1 $2 R$3"); // SEC24
																													// T23S
																													// T17E
		remarks = remarks.replaceAll("--", "-");
		remarks = remarks.replaceAll("(\\d+)(BL\\d+)", "$1 $2");
		remarks = remarks.replaceAll("\\bL-", "L ");
		remarks = remarks.replaceAll("\\bPRT\\b", "");
		remarks = remarks.replaceAll("\\b&OR\\b", " OR");

		remarks = replaceNumbers(remarks);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		remarks = Roman.normalizeRomanNumbersExceptTokens(remarks, exceptionTokens); // convert
																						// roman
																						// numbers
																						// to
																						// arabics

		remarks = remarks.trim();
		remarks = remarks.replaceAll("\\s{2,}", " ");
		String legalTemp = remarks;

		// extract and remove section, township and range from legal description
		List<List> body = getSTRFromMap(m); // first add sec-twn-rng extracted
											// from XML specific tags, if any
											// (for DT use)
		Pattern p = Pattern.compile("\\b(?:SEC(?:TION)?)\\s*([\\d&-]+)\\s*T\\s*(\\d+[NSEW])\\s*(?:R\\s*(\\d+[NSEW]))?");
		Matcher ma = p.matcher(remarks);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			String sec = ma.group(1);
			String twn = ma.group(2);
			String rng = ma.group(3);
			if (rng == null)
				rng = "";
			line.add(sec);
			line.add(twn);
			line.add(rng);
			body.add(line);
			remarks = remarks.replace(ma.group(0), " ");
		}
		saveSTRInMap(m, body);
		remarks = remarks.trim().replaceAll("\\s{2,}", " ");
		// extract section without township and range
		p = Pattern.compile("-?\\s*\\bSEC(?:TION)?\\s*([\\d&-]+|[A-Z])\\s*");
		ma = p.matcher(remarks);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			String sec = ma.group(1);
			line.add(sec);
			line.add("");
			line.add("");
			body.add(line);
			remarks = remarks.replace(ma.group(0), " ");
		}
		saveSTRInMap(m, body);
		remarks = remarks.trim().replaceAll("\\s{2,}", " ");

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		p = Pattern.compile("(?is)\\b(LS?|LTS?|LOTS?)\\s*([\\d&-]+|[A-Z][\\d]+(?:&[A-Z][\\d]+)?)\\b");
		ma = p.matcher(remarks);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			String lotFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (!StringUtils.isEmpty(lotFromSet)) {
				lot = lotFromSet + " " + lot;
			}
			lot = LegalDescription.cleanValues(lot, false, true);
			if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.SubdivisionLotNumber")))
				m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		remarks = legalTemp;

		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BLS?|BLKS?)\\s*([\\d&-]+)\\b");
		ma = p.matcher(remarks);
		while (ma.find()) {
			block = ma.group(2).replaceAll("\\s*,\\s*", " ");
			String blockFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(blockFromSet)) {
				block = blockFromSet + " " + block;
				block = LegalDescription.cleanValues(block, false, true);
			}
			if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.SubdivisionBlock")))
				m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		remarks = legalTemp;

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();

		remarks = remarks.replaceAll("(?is)\\b&(\\d{3,}\\s*-)", " OR$1");

		p = Pattern.compile("(?is)\\b(OR)\\s*(\\d+)\\s*(?:-|PG|/)\\s*([\\d&]+)\\b");
		ma = p.matcher(remarks);

		while (ma.find()) {
			String book = ma.group(2);
			String page = ma.group(3);
			if (page == null) {
				page = "";
			}
			// we might have OR895-2078&2083;
			page = page.trim();
			page = page.replaceAll("\\s*&\\s*", "&");
			String[] pages = page.split("&");
			ResultTable crossRefs = (ResultTable) m.get("CrossRefSet");
			String[][] cbody = null;
			if (crossRefs != null) {
				cbody = crossRefs.body;

				for (int jj = 0; jj < pages.length; jj++) {
					for (int i = 0; i < cbody.length; i++) {
						if (!cbody[i][0].equals(book) && (pages[jj].equals("") || !cbody[i][1].equals(pages[jj]))) {
							List<String> line = new ArrayList<String>();
							line.add(book);
							line.add(pages[jj]);
							line.add("");
							bodyCR.add(line);
						}
					}
				}
			} else {
				for (int jj = 0; jj < pages.length; jj++) {
					List<String> line = new ArrayList<String>();
					line.add(book);
					line.add(pages[jj]);
					line.add("");
					bodyCR.add(line);
				}
			}
		}
		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "InstrumentNumber" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });

			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			m.put("CrossRefSet", cr);
		}

		// extract phase from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH|PHASE)\\s*([\\d-]+[A-Z]?)\\b");
		ma = p.matcher(remarks);
		if (ma.find()) {
			phase = ma.group(2).replaceAll("\\s*,\\s*", " ");
			if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.SubdivisionPhase")))
				m.put("PropertyIdentificationSet.SubdivisionPhase", phase.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		remarks = legalTemp;

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(U|UNITS?)\\s*([\\d-&]+[A-B]?)\\b");
		ma = p.matcher(remarks);
		if (ma.find()) {
			unit = ma.group(2).replaceAll("\\s*,\\s*", " ");
			unit = unit.replaceAll("#", "");
			if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.SubdivisionUnit")))
				m.put("PropertyIdentificationSet.SubdivisionUnit", unit.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		remarks = legalTemp;

		// extract tract #
		p = Pattern.compile("(?is)\\b(TR|TRACT)\\s*([\\d&-]+[A-Z]?)\\b");
		ma = p.matcher(remarks);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionTract", ma.group(2).replaceAll("-", " ").trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		remarks = legalTemp;

		// extract plat b&p
		p = Pattern.compile("(?is)\\b(PLAT|CONDOMINIUM|DEED)\\s+(?:BOOK)\\s+([\\dA-Z]+)\\s+PAGES?\\s+([\\dA-Z-]+)\\b");
		ma = p.matcher(remarks);
		if (ma.find()) {
			if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.PlatBook"))
					&& StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.PlatNo"))) {
				m.put("PropertyIdentificationSet.PlatBook", ma.group(2).trim());
				m.put("PropertyIdentificationSet.PlatNo", ma.group(3).trim());
			}
			remarks = remarks.replaceFirst("\\b" + ma.group(0) + "\\b", "");
			remarks = remarks.trim().replaceAll("\\s{2,}", " ");
		}

	}

	public static void legalFLHernandoDASLRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerFLHernandoDASLRV(m, legal);
	}

	public static void legalTokenizerFLBrevardDASLRV(ResultMap m, String legal) throws Exception {

		// corrections
		legal = legal.replaceAll("\\bUNTI(?= \\d)", "UNIT");

		// legal cleanup
		legal = legal.replaceAll("[&\\+]", " ");
		legal = legal.replaceAll("\\bNO\\b\\.?\\s*", "");
		legal = legal.replaceAll("#(?=\\d)*", "");
		legal = legal.replaceAll("(\\d+)(?:ST|ND|RD|TH) (SEC(?:TION)?)\\b", "$2 $1");
		legal = legal.replaceAll("\\b(A )?(REPLAT|PORT)( OF)?\\b\\s*", "");
		legal = legal.replaceAll("(?<=\\d)(\\s*-\\s*| THRU | TO )(?=\\d)", "-");
		legal = legal.replaceAll("\\b\\w+-\\w+-\\w+(-\\w+)+\\b", "");
		legal = legal.replaceAll("\\b[SWEN]{1,2} \\d+([/\\.]\\d+)?( FT)? OF\\b", "");
		legal = legal.replaceAll("\\b\\d+ FT OF\\b", "");
		legal = legal.replaceAll("\\s(?<!ORB? )\\d+(\\.|/)\\d+\\b", "");
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// subdivision name - not requested

		legal = legal.replaceAll("\\bA CONDO\\b", "");
		legal = legal.replaceAll("\\sAND\\s", " ");
		legal = legal.replaceAll(",\\s*", " ");
		legal = legal.replaceAll("\\bP[\\s\\.]?U[\\s\\.]?D\\b", "");
		legal = legal.replaceAll("\\bAS DESC? IN\\b", "");
		legal = legal.replaceAll("\\bALL AMENDMENTS THERETO\\b\\.?", "");
		String origLegal = legal;
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
																					// to
																					// arabics
		legal = replaceNumbers(legal);
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract section, township and range from legal description
		List<List> body = new ArrayList<List>();
		Pattern p = Pattern.compile("\\bSEC (\\d+(?:-\\d+)?)(?:-| TWP |\\s)(\\d+[SWEN]?)(?:-| RGE |\\s)(\\d+[SWEN]?)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(2));
			line.add(ma.group(3));
			// body.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}
		p = Pattern.compile("\\bSEC(?:TION)?S? (\\d+(?:-?[A-Z])?(?: \\d+)?|[A-Z](?:[\\s-]\\d+)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			line.add("");
			line.add("");
			body.add(line);
		}
		if (!body.isEmpty()) {
			String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
			map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
			map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(body);
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
		}

		// extract tract
		String tract = ""; // can have multiple occurrences
		p = Pattern.compile("\\bTR(?:ACT)?S? (?!OF )(\\d+(?:-?[A-Z])?( \\d+)*|\\w([\\s\\.]\\w)*|\\w\\w)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(1);
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}

		// extract building #
		p = Pattern.compile("\\b(?:BLDG|BUILDING) (\\d+(?:-?[A-Z])?(?: \\d+)*|[A-Z](?:-?\\d+)?)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
		}

		// extract plat book & page from legal description
		p = Pattern.compile("\\bPB (\\d+) (?:PGS? )?(\\d+(?:[\\s-]+\\d+[A-Z]?)*)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
			m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("\\b(ORB|DB|SB)?(?<! PB)\\s+(\\d+) P(?:G|AGE)S? (\\d+(?:[\\s-]\\d+(?! PG))*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2).replaceFirst("^0+(\\d*)", "$1"));
			line.add(ma.group(3).replaceFirst("^0+(\\d*)", "$1"));
			String bpType = ma.group(1);
			if (bpType == null)
				bpType = "";
			line.add(bpType);
			bodyCR.add(line);
		}
		p = Pattern.compile("\\b(ORB|DB|SB)\\s+(\\d+)/(\\d+(?:[\\s-]\\d+)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2).replaceFirst("^0+(\\d*)", "$1"));
			line.add(ma.group(3).replaceFirst("^0+(\\d*)", "$1"));
			String bpType = ma.group(1);
			if (bpType == null)
				bpType = "";
			line.add(bpType);
			bodyCR.add(line);
		}

		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "Book_Page_Type" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("Book_Page_Type", new String[] { "Book_Page_Type", "" });

			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			// m.put("CrossRefSet", cr); - DO NOT PARSE CROSSREF FROM LEGAL
		}

		// extract phase from legal description
		p = Pattern.compile("\\bPH(?:ASES?)?(( (\\d+\\w?|\\b[A-Z]\\b))+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			String phase = ma.group(1).trim();
			phase = phase.replaceFirst("^I$", "1");
			phase = LegalDescription.cleanValues(phase, false, true);
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
		}

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		p = Pattern.compile("\\bLOTS?\\s+(\\d+[A-Z]?(?:[-\\s]+\\d+[A-Z]?)*|[A-Z](?:\\s[A-Z])*)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("\\b(?:U(?:NITS?)?|APT)\\s+((?:\\d+|[A-Z])-?(?:[A-Z]\\d*|\\d+[A-Z]?)?)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			unit = unit + " " + ma.group(1).replaceFirst("^0+(\\w.*)", "$1");
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
		}

		// extract block from legal description
		String block = ""; // can have multiple occurrences
		p = Pattern.compile("\\bBL(?:OC)?KS?\\s+((\\d+(?:[\\s-]?[A-Z])?(?:[-\\s]+\\d+)*|\\b[A-Z]{1,2}\\d*)\\b)");
		ma = p.matcher(origLegal);
		String firstBlk = "";
		while (ma.find()) {
			String newBlk = ma.group(1);
			if (block.length() == 0) {
				firstBlk = newBlk;
				block = firstBlk;
			} else if (firstBlk.trim().matches("\\d+") && newBlk.matches(".*[A-Z].*")) {

			} else {
				block = block + " " + newBlk;
			}
		}
		block = block.trim();
		block = block.replaceFirst("\\-$", "");
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
	}

	public static void legalFLBrevardDASLRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerFLBrevardDASLRV(m, legal);
	}

	public static void legalTokenizerFLDuvalDASLRV(ResultMap m, String legal) throws Exception {

		// initial legal descr corrections and cleanup
		legal = legal.replaceAll("\\b(UNI) (T \\d)", "$1$2");
		legal = legal.replaceAll("\\b(PHASE)(\\d)", "$1 $2");
		legal = legal.replaceAll("[&\\+]", " ");
		legal = legal.replaceAll("\\bNO\\b\\.?\\s*", "");
		legal = legal.replaceAll("#(?=\\d)*", "");
		legal = legal.replaceAll("(?<=\\d)(\\s*-\\s*| THRU | TO )(?=\\d)", "-");
		legal = legal.replaceAll("\\b[SWEN]{1,2} \\d+([/\\.]\\d+)?(\\s*FT)?( OF)?\\b", "");
		legal = legal.replaceAll("\\b\\d+ FT OF\\b", "");
		legal = legal.replaceAll("\\b(\\d*\\.|\\d+/)\\d+(ST|ND|RD|TH)?\\b", "");
		legal = legal.replaceAll("\\b\\d+ HEIRS?\\b", "");
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// subdivision name - not requested

		legal = legal.replaceAll("\\bA CONDO(MINIUM)?\\b", "");
		legal = legal.replaceAll("\\sAND\\s", " ");
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
																					// to
																					// arabics
		legal = replaceNumbers(legal);
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract section, township and range from legal description
		List<List> body = new ArrayList<List>();
		Pattern p = Pattern.compile("\\b(?:SEC )?((?<!-)(?:\\b\\d+,)*\\d+(?:-\\d+)?)-(\\d+[SWEN])[-\\s]?(\\d+[SWEN])\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1").replaceAll(",", " ").replaceAll("\\s{2,}", " "));
			line.add(ma.group(2).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(3).replaceFirst("^0+(\\d+)", "$1"));
			body.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}
		p = Pattern.compile("\\b(\\d+)-(\\d+([SWEN])?)-(\\d+([SWEN])?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			if (ma.group(3) != null || ma.group(5) != null) {
				List<String> line = new ArrayList<String>();
				line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
				line.add(ma.group(2).replaceFirst("^0+(\\d+)", "$1"));
				line.add(ma.group(4).replaceFirst("^0+(\\d+)", "$1"));
				body.add(line);
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
			}
		}
		p = Pattern.compile("\\bSEC(?:TION)?S? (\\d+(?:-?[A-Z])?(?: \\d+)?|[A-Z](?:[\\s-]\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			line.add("");
			line.add("");
			body.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}
		if (!body.isEmpty()) {
			String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
			map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
			map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(body);
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
		}

		// extract tract
		String tract = ""; // can have multiple occurrences
		p = Pattern.compile("\\bTR(?:ACT)?S? (?!OF )(\\d+(?:-?[A-Z])?([\\s,-]+\\d+)*|\\w([\\s\\.]\\w)*|\\w\\w)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(1);
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}

		// extract building #
		p = Pattern.compile("\\b(?:BLD?G|BUILDING) (\\d+(?:-?[A-Z])?(?: \\d+)*|[A-Z](?:-?\\d+)?)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("\\b(O/?RS?|D|LIFE EST(?:ATE)?|RECD)(?: BKS?)?((?:[\\s,]+\\d+-\\d+(?:[\\s,]+\\d+\\b(?!-))*)+)\\b");
		ma = p.matcher(legal);
		Pattern p2 = Pattern.compile("\\b(\\d+)-(\\d+(?:[\\s,]+\\d+\\b(?!-))*)\\b");
		while (ma.find()) {
			Matcher ma2 = p2.matcher(ma.group(2));
			String bpType = ma.group(1);
			if (bpType == null)
				bpType = "";
			bpType = bpType.replaceFirst("/", "").replaceAll("RECD", "OR");
			while (ma2.find()) {
				List<String> line = new ArrayList<String>();
				line.add(ma2.group(1).replaceFirst("^0+(\\d*)", "$1"));
				line.add(ma2.group(2).replaceFirst("^0+(\\d*)", "$1").replaceAll(",", " ").replaceAll("\\s{2,}", " "));
				line.add(bpType);
				bodyCR.add(line);
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "OR ");
		}
		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "Book_Page_Type" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("Book_Page_Type", new String[] { "Book_Page_Type", "" });

			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			// m.put("CrossRefSet", cr); - DO NOT PARSE CROSSREF FROM LEGAL
		}

		// extract plat book & page from legal description
		List<List> bodyPlat = new ArrayList<List>();
		String pbPatt = "\\b(\\d+)-(\\d+[A-Z]?)\\b";
		p = Pattern.compile("^(?<!-)(" + pbPatt + "(?: " + pbPatt + ")*)(?!-)");
		ma = p.matcher(legal);
		p2 = Pattern.compile(pbPatt);
		while (ma.find()) {
			Matcher ma2 = p2.matcher(ma.group(1));
			while (ma2.find()) {
				List<String> line = new ArrayList<String>();
				line.add(ma2.group(1).replaceFirst("^0+(\\d*)", "$1"));
				line.add(ma2.group(2).replaceFirst("^0+(\\d*)", "$1"));
				bodyPlat.add(line);
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PLAT ");
		}
		p = Pattern.compile("(?<!-)(" + pbPatt + "(?: " + pbPatt + ")*)(?= SEC\\b)");
		ma = p.matcher(legal);
		while (ma.find()) {
			Matcher ma2 = p2.matcher(ma.group(1));
			while (ma2.find()) {
				List<String> line = new ArrayList<String>();
				line.add(ma2.group(1).replaceFirst("^0+(\\d*)", "$1"));
				line.add(ma2.group(2).replaceFirst("^0+(\\d*)", "$1"));
				bodyPlat.add(line);
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PLAT ");
		}
		if (!bodyPlat.isEmpty()) {
			String[] header = { "PlatBook", "PlatNo" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("PlatBook", new String[] { "PlatBook", "" });
			map.put("PlatNo", new String[] { "PlatNo", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(bodyPlat);

			ResultTable pisSTR = (ResultTable) m.get("PropertyIdentificationSet");
			if (pisSTR != null) {
				pis = ResultTable.joinHorizontal(pis, pisSTR);
				map.putAll(pisSTR.map);
			}
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
		}

		// extract phase from legal description
		p = Pattern.compile("\\bPH(?:ASES?)?((?: (?:\\d+\\w?|\\b[A-Z]\\b))+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			String phase = ma.group(1).trim();
			phase = phase.replaceFirst("^I$", "1");
			phase = LegalDescription.cleanValues(phase, false, true);
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
		}

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		String lotPatt = "\\d+(?:-?[A-Z])?(?:[-\\s,]+\\d+(?:-?[A-Z])?)*";
		p = Pattern.compile("\\bLO?TS?[\\s,]+(" + lotPatt + "|[A-Z](?:\\s[A-Z])*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(1).replaceAll(",", " ").replaceAll("\\s{2,}", " ");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		p = Pattern.compile("\\bL (" + lotPatt + ")\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("\\b(?:U(?:NITS?)?|APT)\\s+((?:\\d+|[A-Z])-?(?:[A-Z]\\d*|\\d+[A-Z]?)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1).replaceFirst("^0+(\\w.*)", "$1");
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
		}

		// extract block from legal description
		String block = ""; // can have multiple occurrences
		String blkPatt = "\\d+(?:[\\s-]?[A-Z])?(?:[-\\s,]+\\d+)*";
		p = Pattern.compile("\\bBL(?:OC)?KS? (" + blkPatt + "|\\b[A-Z]{1,2}\\d*)\\b(?!/)");
		ma = p.matcher(legal);
		while (ma.find()) {
			String newBlk = ma.group(1).replaceAll(",", " ").replaceAll("\\s{2,}", " ");
			block = block + " " + newBlk;
		}
		p = Pattern.compile("\\bB (" + blkPatt + ")\\b(?!/)");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1).replaceAll(",", " ").replaceAll("\\s{2,}", " ");
		}
		block = block.trim();
		block = block.replaceFirst("\\-$", "");
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
	}

	public static void legalFLDuvalDASLRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerFLDuvalDASLRV(m, legal);
	}

	public static void legalTokenizerFLVolusiaDASLRV(ResultMap m, String legal) throws Exception {

		// initial legal descr corrections and cleanup
		legal = legal.replaceAll("T HRU\\b", "THRU");
		legal = legal.replaceAll("TH RU\\b", "THRU");
		legal = legal.replaceAll("THR U\\b", "THRU");
		legal = legal.replaceAll("BL K(?= \\d)", "BLK");
		legal = legal.replaceAll("B LK\\b", "BLK");
		legal = legal.replaceAll("LO T\\b", "LOT");
		legal = legal.replaceAll("UN REC\\b", "UNREC");
		legal = legal.replaceAll("\\bO F\\b", "OF");
		legal = legal.replaceAll("P ER\\b", "PER");
		legal = legal.replaceAll("PE R\\b", "PER");
		legal = legal.replaceAll("P\\s+(GS?)(?= \\d)", "P$1");
		legal = legal.replaceAll("PG\\s+S(?= \\d)", "PG");
		legal = legal.replaceAll("(?<=\\d )GP(?= \\d)", "PG");
		legal = legal.replaceAll("(\\d)(PG)\\b", "$1 $2");
		legal = legal.replaceAll("(\\d)([A-Z]{3,})", "$1 $2");
		legal = legal.replaceAll("T O(?=\\s*\\d)", " TO");
		legal = legal.replaceAll("(?<=\\d)(\\s*-\\s*|\\s+(THRU|TO)\\s+)(?=\\d)", "-");
		legal = legal.replaceAll("(?<=\\b|\\d)IN C\\b", " INC");
		legal = legal.replaceAll("(?<=\\b|\\d)I NC\\b", " INC");
		legal = legal.replaceAll("(\\d+) (\\d+)(?= PGS?)", "$1$2");
		legal = legal.replaceAll("(\\d-\\d+) (\\d+)\\b", "$1$2");
		legal = legal.replaceAll("\\b(PGS? \\d{2,}) (\\d)\\b", "$1$2");
		legal = legal.replaceAll("\\b(PGS? \\d) (\\d{3,4})\\b", "$1$2");
		legal = legal.replaceAll("\\b(PGS? 0) (\\d+)\\b", "$1$2");
		legal = legal.replaceAll("\\b(PGS? \\d+) (\\d+)(?=\\s*[-&])", "$1$2");
		legal = legal.replaceAll("O R(?=\\s*\\d)", " OR");
		legal = legal.replaceAll("M B(?=\\s*\\d)", " MB");
		legal = legal.replaceAll("\\b(OF)(BLK)\\b", "$1 $2");
		legal = legal.replaceAll("\\b(GOVT?)(LOT)", "$1 $2");
		legal = legal.replaceAll("\\b(PHASE \\d+)([A-Z]{2,})\\b", "$1 $2");
		legal = legal.replaceAll("(\\w)M B(?= \\d+ PG)", "$1 MB");
		legal = legal.replaceAll("\\b(\\w) (/\\w)", " $1$2");
		legal = legal.replaceAll("\\b((?:MB|OR|DB|D/C) \\d+) (PGF|PER)(?= \\d)", "$1 PGS");
		legal = legal.replaceAll("\\bNO\\b\\.?\\s*", "");
		legal = legal.replaceAll("#(?=\\d)*", "");
		legal = legal.replaceAll("\\bA PORTION OF\\b", "");
		legal = legal.replaceAll("\\bA QUIET PLACE\\b", "");
		legal = legal.replaceFirst("^\\s*&\\s*", "");
		legal = legal.replaceAll("\\bINC\\b", "");
		legal = legal.replaceAll("\\bPAGES?\\b", "PG");
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// subdivision name - not requested

		legal = legal.replaceAll("\\sAND\\s", " ");
		legal = legal.replaceAll("\\b[SWEN]{1,2} \\d*[/\\.]?\\d+(\\s*FT)?( OF)?\\b", "");
		legal = legal.replaceAll("\\b[SWEN]{1,2}(/L)? OF\\b", "");
		legal = legal.replaceAll("\\b\\d+(\\.\\d+)? FT( OF)?\\b", "");
		legal = legal.replaceAll("\\b\\d+/\\d+( INT)?\\b", "");
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
																					// to
																					// arabics
		legal = replaceNumbers(legal);
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract section, township and range from legal description
		List<List> body = new ArrayList<List>();
		Pattern p = Pattern.compile("(?:\\bSEC(?:TION)?\\s*|^)(\\d+(?:\\s*&\\s*\\d+)*)[\\s-](\\d+[SWEN]?)[\\s-](\\d+[SWEN]?)\\b(?!-| \\d)");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1").replaceAll("\\s*&\\s*", " "));
			line.add(ma.group(2).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(3).replaceFirst("^0+(\\d+)", "$1"));
			body.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}
		p = Pattern.compile("(?<!-)\\b(\\d+)-(\\d+([SWEN])?)-(\\d+([SWEN])?)\\b(?!-| \\d)");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(2).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(4).replaceFirst("^0+(\\d+)", "$1"));
			body.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}
		p = Pattern.compile("\\bSEC(?:TION)?S?\\s*(\\d+(?:-?[A-Z](?:-[A-Z]|[-\\s]\\d+)?)?(?:\\s*&\\s*\\d+(?![-/]))*|\\b[A-Z]{1,2}-\\d+|\\b[A-Z]\\d*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1").replaceAll("\\s*&\\s*", " "));
			line.add("");
			line.add("");
			body.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}
		if (!body.isEmpty()) {
			legal = legal.replaceAll("\\s{2,}", " ");
			String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
			map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
			map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(body);
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
		}

		// extract tract
		String tract = ""; // can have multiple occurrences
		p = Pattern.compile("\\bTR(?:ACT)?S? (?!OF )(\\d+(?:-?[A-Z])?([\\s,-]+\\d+)*|\\w([\\s\\.]\\w)*|\\w\\w)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(1);
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}

		// extract building #
		p = Pattern.compile("\\b(?:BLD?G|BUILDING) (\\d+(?:-?[A-Z])?(?: \\d+)*|[A-Z](?:-?\\d+)?)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
		}

		// extract plat book & page from legal description
		List<List> bodyPlat = new ArrayList<List>();
		p = Pattern.compile("\\bMB\\s*(\\d+)(?:/| PGS?\\s?)(\\d+[A-Z]?(?:[\\s/&,-]+\\d+)*)\\b");
		ma = p.matcher(legal);
		Pattern pPage = Pattern.compile("(\\d+) (\\d+)");
		Matcher maPage;
		while (ma.find()) {
			String book = ma.group(1).replaceFirst("^0+(\\d+)", "$1");
			String page = ma.group(2).replaceAll("\\b0+(\\d+)", "$1").replaceAll("\\s*[&/,]\\s*", " ");
			maPage = pPage.matcher(page);
			if (maPage.matches()) { // correct typing errors, e.g. MB 29 PG 8 1,
									// where page should be extracted as "81",
									// not "8 1"
				int l1 = Integer.parseInt(maPage.group(1));
				int l2 = Integer.parseInt(maPage.group(2));
				if (l1 >= l2)
					page = page.replace(" ", "");
			}
			List<String> line = new ArrayList<String>();
			line.add(book);
			line.add(page);
			bodyPlat.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PB ");
			legal = legal.replaceAll("\\s{2,}", " ");
		}
		if (!bodyPlat.isEmpty()) {
			String[] header = { "PlatBook", "PlatNo" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("PlatBook", new String[] { "PlatBook", "" });
			map.put("PlatNo", new String[] { "PlatNo", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(bodyPlat);

			ResultTable pisSTR = (ResultTable) m.get("PropertyIdentificationSet");
			if (pisSTR != null) {
				pis = ResultTable.joinHorizontal(pis, pisSTR);
				map.putAll(pisSTR.map);
			}
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		String pgPatt = "(\\d+(?:\\s*[\\s&,-]\\s*\\d+(?! PGS?\\b))*)\\b";
		String bpPatt = "(\\d+) PGS?\\s?" + pgPatt;
		p = Pattern.compile("\\b(OR|DB|D/?C)?\\s*(" + bpPatt + "(?:\\s*[,&]\\s*" + bpPatt + ")*)\\b");
		ma = p.matcher(legal);
		Pattern p2 = Pattern.compile(bpPatt);
		Matcher ma2;
		while (ma.find()) {
			ma2 = p2.matcher(ma.group(2));
			while (ma2.find()) {
				String book = ma2.group(1).replaceFirst("^0+(\\d+)", "$1");
				String page = ma2.group(2);
				maPage = pPage.matcher(page);
				if (maPage.matches()) { // correct typing errors, e.g. OR 29 PG
										// 8 1, where page should be extracted
										// as "81", not "8 1"
					int l1 = Integer.parseInt(maPage.group(1));
					int l2 = Integer.parseInt(maPage.group(2));
					if (l1 >= l2)
						page = page.replace(" ", "");
				}
				// correct typing errors, e.g. OR 5084 PG 3496-99, where page
				// should be extracted as "3496-3499", not "3496-99"
				page = page.replaceFirst("^(\\d{1,})(\\d{2})-(\\d{2})$", "$1$2-$1$3");
				page = page.replaceAll("\\b0+(\\d+)", "$1").replaceAll("\\s*[,&]\\s*", " ");
				String bpType = ma.group(1);
				if (bpType == null)
					bpType = "";
				List<String> line = new ArrayList<String>();
				line.add(book);
				line.add(page);
				line.add(bpType.replaceAll("/", ""));
				bodyCR.add(line);
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "OR ");
		}
		if (!bodyCR.isEmpty()) {
			legal = legal.replaceAll("\\s{2,}", " ");
			String[] header = { "Book", "Page", "Book_Page_Type" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("Book_Page_Type", new String[] { "Book_Page_Type", "" });

			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			// m.put("CrossRefSet", cr); - DO NOT PARSE CROSSREF FROM LEGAL
		}

		// extract phase from legal description
		p = Pattern.compile("\\bPH(?:ASES?)?((?:[\\s&]+(?:\\d+(?:[\\s-]?[A-Z])?|I))+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			String phase = ma.group(1).trim();
			phase = phase.replaceAll("\\s*&\\s*", " ");
			phase = phase.replaceAll("\\bI\\b", "1");
			phase = LegalDescription.cleanValues(phase, false, true);
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
		}

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		String lotPatt = "\\d+(?:[\\s-]?[A-Z])?";
		String lotPattLetter = "\\b[A-Z]";
		p = Pattern.compile("\\bLO?TS?\\s?((?:" + lotPatt + "|" + lotPattLetter + ")(?:\\s*[-,&\\s]\\s*(?:" + lotPatt + "|" + lotPattLetter + "))*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			String lotTmp = ma.group(1);
			lotTmp = lotTmp.replaceAll("\\b(\\d+) ([A-Z])\\b", "$1$2");
			lotTmp = lotTmp.replaceAll("\\s*[,&]\\s*", " ");
			lot = lot + " " + lotTmp;
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "LOT ");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			legal = legal.replaceAll("\\s{2,}", " ");
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("\\b(?:U(?:NI)?TS?|APT|UNI)\\s*((?:\\d+(?:-?[A-Z]\\d*)?|\\b[A-Z](?:-?\\d+)?)(?:[\\s&-]+(?:\\b[A-Z]\\d*|\\d+[A-Z]?))*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1).replaceFirst("^0+(\\w.*)", "$1").replaceAll("\\s*&\\s*", " ");
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
		}

		// extract block from legal description
		String block = ""; // can have multiple occurrences
		String blkPatt = "\\d+(?:[\\s-]?[A-Z](?! &))?(?:[-\\s,&]+\\d+)*";
		p = Pattern.compile("\\bBL(?:OC)?KS?\\s?(" + blkPatt + "|\\b[A-Z]{1,2}\\d*)\\b(?!/)");
		ma = p.matcher(legal);
		while (ma.find()) {
			String newBlk = ma.group(1).replaceAll("\\s*[,&]\\s*", " ");
			block = block + " " + newBlk;
		}
		block = block.trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
	}

	public static void legalFLVolusiaDASLRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerFLVolusiaDASLRV(m, legal);
	}

	public static void legalTokenizerFLNassauDASLRV(ResultMap m, String legal) throws Exception {

		// initial legal description corrections and cleanup
		legal = legal.replaceAll("(\\d)(L/E|PB)", "$1 $2");
		legal = legal.replaceAll("\\bNO\\b\\.?\\s*", "");
		legal = legal.replaceAll("\\bALL\\b\\.?\\s*", "");
		legal = legal.replaceAll("#(?=\\d)*", "");
		legal = legal.replaceAll("(?<=\\d)(\\s*-\\s*| THRU | TO )(?=\\d)", "-");
		legal = legal.replaceAll("(?<!\\bUNIT )\\b[SWEN]{1,2}(LY)?(\\s*\\d+([/\\.]\\d+)?| COR)(\\s*FT\\b)?(\\s*OF)?\\b", "");
		legal = legal.replaceAll("\\b[SWEN]{1,2}(LY)? OF\\b", "");
		legal = legal.replaceAll("\\b\\d+ FT OF\\b", "");
		legal = legal.replaceAll("\\bR\\s*\\d+( & \\d+)*\\b", "");
		legal = legal.replaceAll("\\b\\d+/\\d+ INT\\b", "");
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// subdivision name - not requested

		legal = legal.replaceAll("\\sAND\\s", " ");
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract section, township and range from legal description
		List<List> body = new ArrayList<List>();
		Pattern p = Pattern.compile("(?:\\bSE?CT?\\s*|^)(\\d+(?:\\s*&\\s*\\d+)*)-(\\d+[SWEN]?)-(\\d+[SWEN]?)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1").replace('&', ' ').replaceAll("\\s{2,}", " "));
			line.add(ma.group(2).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(3).replaceFirst("^0+(\\d+)", "$1"));
			body.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}
		p = Pattern.compile("\\b(\\d+)-(\\d+([SWEN])?)-(\\d+([SWEN])?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			if (ma.group(3) != null || ma.group(5) != null) {
				List<String> line = new ArrayList<String>();
				line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
				line.add(ma.group(2).replaceFirst("^0+(\\d+)", "$1"));
				line.add(ma.group(4).replaceFirst("^0+(\\d+)", "$1"));
				body.add(line);
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
			}
		}
		p = Pattern.compile("\\bSE?C(?:T(?:ION)?)?S?\\s*(\\d+(?:-?[A-Z])?(?:\\s*&\\s*\\d+(?![-/]))*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1").replace('&', ' ').replaceAll("\\s{2,}", " "));
			line.add("");
			line.add("");
			body.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}
		if (!body.isEmpty()) {
			legal = legal.replaceAll("\\s{2,}", " ");
			String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
			map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
			map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(body);
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
		}

		// extract tract
		String tract = ""; // can have multiple occurrences
		p = Pattern.compile("\\bTR(?:ACT)?S? (?!OF )(\\d+(?:-?[A-Z])?([\\s,-]+\\d+)*|\\w([\\s\\.]\\w)*|\\w\\w)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(1);
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}

		// extract building #
		p = Pattern.compile("\\b(?:BLD?G|BUILDING) (\\d+(?:-?[A-Z])?(?: \\d+)*|[A-Z](?:-?\\d+)?)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
		}

		// extract plat book & page from legal description
		List<List> bodyPlat = new ArrayList<List>();
		p = Pattern.compile("\\bPB\\s*(\\d+)(?:/| PGS? )(\\d+[A-Z]?(?:[\\s/&,-]+\\d+[A-Z]?)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(2).replaceFirst("^0+(\\d+)", "$1").replaceAll("[&/,]", " ").replaceAll("\\s{2,}", " "));
			bodyPlat.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PB ");
			legal = legal.replaceAll("\\s{2,}", " ");
		}
		if (!bodyPlat.isEmpty()) {
			String[] header = { "PlatBook", "PlatNo" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("PlatBook", new String[] { "PlatBook", "" });
			map.put("PlatNo", new String[] { "PlatNo", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(bodyPlat);

			ResultTable pisSTR = (ResultTable) m.get("PropertyIdentificationSet");
			if (pisSTR != null) {
				pis = ResultTable.joinHorizontal(pis, pisSTR);
				map.putAll(pisSTR.map);
			}
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		String pgPatt = "(\\d+(?:\\s*[&,-]\\s*\\d+(?!/))*)\\b";
		p = Pattern.compile("\\b(OR|DB) (\\d+) PGS? " + pgPatt + "\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(3).replaceFirst("^0+(\\d+)", "$1").replaceAll("[,&]", " ").replaceAll("\\s{2,}", " "));
			line.add(ma.group(1));
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "OR ");
		}
		String bpPatt = "(\\d+)/" + pgPatt;
		p = Pattern.compile("\\b(OR|DB)?\\s*(" + bpPatt + "(?:[\\s&]+" + bpPatt + ")*)\\b");
		ma = p.matcher(legal);
		Pattern p2 = Pattern.compile(bpPatt);
		while (ma.find()) {
			Matcher ma2 = p2.matcher(ma.group(2));
			while (ma2.find()) {
				List<String> line = new ArrayList<String>();
				line.add(ma2.group(1).replaceFirst("^0+(\\d+)", "$1"));
				line.add(ma2.group(2).replaceFirst("^0+(\\d+)", "$1").replaceAll("[,&]", " ").replaceAll("\\s{2,}", " "));
				String bpType = ma.group(1);
				if (bpType == null)
					bpType = "";
				line.add(bpType);
				bodyCR.add(line);
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "OR ");
		}
		if (!bodyCR.isEmpty()) {
			legal = legal.replaceAll("\\s{2,}", " ");
			String[] header = { "Book", "Page", "Book_Page_Type" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("Book_Page_Type", new String[] { "Book_Page_Type", "" });

			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			// m.put("CrossRefSet", cr); - DO NOT PARSE CROSSREF FROM LEGAL
		}

		// extract phase from legal description
		p = Pattern.compile("\\bPH(?:ASES?)?((?:[\\s&]+(?:\\d+\\w?|\\b[A-Z]\\b))+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			String phase = ma.group(1).trim();
			phase = phase.replace('&', ' ');
			phase = LegalDescription.cleanValues(phase, false, true);
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
		}

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		String lotPatt = "\\d+(?:-?[A-Z])?";
		String lotPattLetter = "[A-Z](?!/)(?: OF \\d+)?";
		p = Pattern
				.compile("\\b(?:LO?T|SUBLOT)S? ((?:" + lotPatt + "|" + lotPattLetter + ")(?:\\s*[-,&\\s]\\s*(?:" + lotPatt + "|" + lotPattLetter + "))*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			String lotTmp = ma.group(1).replaceAll("[,&]", " ");
			lotTmp = lotTmp.replaceAll("\\bOF\\b", "");
			lotTmp = lotTmp.replaceAll("\\b[A-Z]-\\d+\\b", "");
			lotTmp = lotTmp.replaceAll("\\b(\\d+)( [A-Z])+\\b", "$1");
			lotTmp = lotTmp.replaceAll("\\s{2,}", " ");
			lot = lot + " " + lotTmp;
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "LOT ");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			legal = legal.replaceAll("\\s{2,}", " ");
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("\\b(?:U(?:NITS?)?|APT)\\s+((?:\\d+|[A-Z])-?(?:[A-Z]\\d*|\\d+[A-Z]?)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1).replaceFirst("^0+(\\w.*)", "$1");
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
		}

		// extract block from legal description
		String block = ""; // can have multiple occurrences
		String blkPatt = "\\d+(?:[\\s-]?[A-Z])?(?:[-\\s,]+\\d+)*";
		p = Pattern.compile("\\bBL(?:OC)?KS? (" + blkPatt + "|\\b[A-Z]{1,2}\\d*)\\b(?!/)");
		ma = p.matcher(legal);
		while (ma.find()) {
			String newBlk = ma.group(1).replaceAll(",", " ").replaceAll("\\s{2,}", " ");
			block = block + " " + newBlk;
		}
		block = block.trim();
		block = block.replaceFirst("\\-$", "");
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
	}

	public static void legalFLNassauDASLRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerFLNassauDASLRV(m, legal);
	}

	public static void legalTokenizerFLHillsboroughDASLRV(ResultMap m, String legal) throws Exception {
		legal = legal.replaceAll("&", " ");

		legal = replaceNumbers(legal);
		legal = legal.replaceAll("\\bORDN #?[\\d-]+\\s*", "");
		legal = legal.replaceAll("\\bNO\\b\\s*", "");
		legal = legal.replaceAll("(\\d+)(ST|ND|RD|TH) UNIT", "UNIT $1");
		legal = legal.replaceAll("\\b(A )?REPLAT( OF)?\\b\\s*", ""); // fix for
																		// bug
																		// #1998
		legal = legal.replaceAll("\\bTHRU\\b", "-");

		String subdiv = legal;
		// cleanup legal descr before extracting subdivision name
		subdiv = subdiv.replaceAll("^TRACTS? (DESC AS )?BEG \\d+ ([SWNE]+ )?FT ([SENW]+ )?OF ([SENW]+ )?COR OF( (LOTS?|BL(OC)?K|UNIT) \\d+)*", "");
		subdiv = subdiv.replaceAll("^SUBDIVISION OF TRACTS? \\d+( AND \\d+)? OF", "");
		subdiv = subdiv.replaceAll("^C?OMM AT [SWNE]+ COR OF( (LOTS?|BL(OC)?K|UNIT) \\d+)*.", "");
		subdiv = subdiv.replaceAll("^\\s*(\\d+(ST|ND|RD|TH) )?ADD(ITIO)?N?( TO)?\\b\\s*", "");
		subdiv = subdiv.trim();

		Pattern p = Pattern
				.compile("(.*?)\\s*\\b(LOTS?|UNIT|PLAT|PB|A COOPERATIVE|(A )?CONDOMINIUM|CONDO|ADD(ITION)?|ADDN?|PARCEL \\w+|PH(ASE)?|SEC(TION)?|(RE)?SUB(DIVISION)?|REV(ISED)?|THAT PART|(IN )?[SWNE]+ \\d+((\\.|/)\\d+)?|TRACTS?|EXTENSION|BL(?:OC)?KS?)\\b.*");
		Matcher ma = p.matcher(subdiv);
		if (ma.find()) {
			subdiv = ma.group(1);

			subdiv = subdiv.replaceAll("\\s*\\d+(ST|ND|RD|TH)$", "");
			subdiv = subdiv.replaceAll("^\\d+( \\d+)*\\b(.+)", " $2");
			subdiv = subdiv.replaceAll("\\s+[ANWSE]$", "");
			subdiv = subdiv.replaceFirst("\\s+SEE$", "");

			if (subdiv.length() != 0) {
				m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
				if (legal.contains("CONDO"))
					m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			}
		}

		String origLegal = legal;
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
																					// to
																					// arabics
		// extract section, township and range from legal description
		p = Pattern.compile("\\bSEC (\\d+(?:-\\d+)?)(?:-|\\s)(\\d+)(?:-|\\s)(\\d+)\\b");
		ma = p.matcher(legal);

		List<List> body = new ArrayList<List>();

		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add(ma.group(3));
			body.add(line);
		}
		if (body.isEmpty()) {
			p = Pattern.compile("\\bSEC(?:TION)? (\\d+|\\w( \\w)*|\\w\\w)\\b");
			ma.usePattern(p);
			ma.reset();
			if (ma.find()) {
				m.put(PropertyIdentificationSetKey.SECTION.getKeyName(), ma.group(1));
			}
		} else {
			String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
			map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
			map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(body);
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
		}

		legal = cleanLegalGenericDASLRV(legal);
		origLegal = cleanLegalGenericDASLRV(origLegal);

		// extract tract
		String tract = ""; // can have multiple occurrences
		p = Pattern.compile("\\bTRACTS? (\\d+( \\d+)*|\\w( \\w)*|\\w\\w)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(1);
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, true, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}

		// extract building #
		p = Pattern.compile("\\b(?:BLDG|BUILDING) (?:NO )?(\\d+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
		}

		// extract plat book & page from legal description
		p = Pattern.compile("\\bPB (\\d+) PG (\\d+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
			m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
		} else {
			p = Pattern.compile("\\b(?:PLAT|PB|REC) (\\d+)-(\\d+)\\b");
			ma.usePattern(p);
			ma.reset();
			if (ma.find()) {
				m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
				m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
			}
		}

		// extract phase from legal description
		p = Pattern.compile("\\bPH(?:ASE)?(( (\\d+\\w?|\\b[A-Z]\\b))+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(1).trim());
		}

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		p = Pattern.compile("\\bLOTS?\\s+([-\\d\\s]+)");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
		}
		lot = lot.trim();
		lot = lot.replaceFirst("\\-$", "");
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, true, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}

		// extract unit from legal description
		p = Pattern.compile("\\bUNITS?\\s+([-\\d\\s]+|\\d+-?[A-Z]|[A-Z\\d-]+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(1).replaceFirst("^0+(\\w.*)", "$1"));
		}

		// extract block from legal description
		String block = ""; // can have multiple occurrences
		p = Pattern.compile("\\bBL(?:OC)?KS?\\s+((\\d+|\\b[A-Z]\\b|-|\\s)+)");
		ma = p.matcher(origLegal);
		String firstBlk = "";
		while (ma.find()) {
			String newBlk = ma.group(1);
			if (block.length() == 0) {
				firstBlk = newBlk;
				block = firstBlk;
			} else if (firstBlk.trim().matches("\\d+") && newBlk.matches(".*[A-Z].*")) {

			} else {
				block = block + " " + newBlk;
			}
		}
		block = block.trim();
		block = block.replaceFirst("\\-$", "");
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
	}

	public static void legalFLHillsboroughDASLRV(ResultMap m, long searchId, boolean crossRef) throws Exception {

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerFLHillsboroughDASLRV(m, legal);
	}

	public static void legalFLCharlotteDASLRV(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legal = legal.replaceAll("&", " ");

		legal = legal.replaceAll("(N|W|S|E|SE|SW|NE|NW)?\\s*[\\d\\.]+\\s*FT\\s*(OF)?\\b", "");
		legal = legal.replaceAll("\\bORDN #?[\\d-]+\\s*", "");
		legal = legal.replaceAll("\\bNO\\b\\s*", "");
		legal = legal.replaceAll("(\\d+)(ST|ND|RD|TH) UNIT", "UNIT $1");
		legal = legal.replaceAll("\\b(A )?REPLAT( OF)?\\b\\s*", "");
		legal = legal.replaceAll("\\bTHRU\\b", "-");
		legal = legal.replaceAll("(\\d+)([A-Z]{2})", "$1" + " " + "$2");
		legal = legal.replaceAll("(\\d{2,})(\\d{4}/\\d{4} )", "$1" + " $2");
		legal = legal.replaceAll("(\\d{2,})(\\d{3}/\\d{3} )", "$1" + " $2");
		legal = legal.replaceAll("UNREC", "");

		// String subdiv = legal;
		// //cleanup legal descr before extracting subdivision name
		// subdiv =
		// subdiv.replaceAll("^TRACTS? (DESC AS )?BEG \\d+ ([SWNE]+ )?FT ([SENW]+ )?OF ([SENW]+ )?COR OF( (LOTS?|BL(OC)?K|UNIT) \\d+)*",
		// "");
		// subdiv =
		// subdiv.replaceAll("^SUBDIVISION OF TRACTS? \\d+( AND \\d+)? OF", "");
		// subdiv =
		// subdiv.replaceAll("^C?OMM AT [SWNE]+ COR OF( (LOTS?|BL(OC)?K|UNIT) \\d+)*.",
		// "");
		// subdiv =
		// subdiv.replaceAll("(.+)\\s*(\\d+(ST|ND|RD|TH) )?ADD(ITIO)?N?( TO)?\\b\\s*.*",
		// "$1");
		// subdiv = subdiv.replaceAll("(.+)\\s*(ST|ND|RD|TH)", "$1");
		// subdiv = subdiv.trim();
		//
		// Pattern p =
		// Pattern.compile(".*\\b(?:LOT|BLK|BLOCK|UNIT|PHASE|BLDG) (.+?) (?:UNIT|PHASE|SEC(?! OF\\b)|ORI?)\\b.*");
		// Matcher ma = p.matcher(subdiv);
		// if (ma.find()){
		// subdiv = ma.group(1);
		// } else {
		// p =
		// Pattern.compile("(.+?) (?:SEC|LT|BLK|PH|UN|UNIT|BLDG|TRACT|A POR|PLAN)");
		// ma.usePattern(p);
		// ma.reset();
		// if (ma.find()){
		// subdiv = ma.group(1);
		// }
		// }
		// if (subdiv.length() != 0){
		//
		// subdiv = subdiv.replaceAll("(.+) (\\d+)", "$1");
		// subdiv = subdiv.replaceFirst("(.+) CONDO(MINIUM)?.*", "$1");
		// subdiv = subdiv.replaceFirst("(.+) SUB(DIVISION)?.*", "$1");
		// m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
		// if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
		// m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
		// }

		String origLegal = legal;
		String tokens[] = { "X", "L", "C", "D", "M" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, tokens); // convert
																		// roman
																		// numbers
																		// to
																		// arabics
		legal = replaceNumbers(legal);

		// extract section from legal description
		String sec = "";
		Pattern p = Pattern.compile("\\bSEC(?:TION)?\\s*(\\d+|\\w( \\w)*|\\w\\w)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			sec = sec + " " + ma.group(1);
		}
		sec = sec.trim();
		if (sec.length() != 0) {
			sec = LegalDescription.cleanValues(sec, true, true);
			m.put("PropertyIdentificationSet.SubdivisionSection", sec);
		}

		// legal = cleanLegalGenericDASLRV(legal);
		// origLegal = cleanLegalGenericDASLRV(origLegal);

		// extract tract
		String tract = ""; // can have multiple occurrences
		p = Pattern.compile("\\bTRACTS?\\s*(\\d+( \\d+)*|\\w( \\w)*|\\w\\w)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(1);
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, true, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}

		// extract building #
		p = Pattern.compile("\\b(?:BLDG)\\s*([A-Z\\d]+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
		}

		// extract plat book & page from legal description
		p = Pattern.compile("\\bPB(\\d+)\\s*/\\s*(\\d+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
			m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("\\b(?:[A-Z/]{2,3})?(\\d{2,})/([\\d&]+)(?:-[A-Z]{2})?\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add("");
			line.add(ma.group(2));
			bodyCR.add(line);
		}

		p = Pattern.compile("\\b(?:[#A-Z]{1,3})?(\\d{2,})-([\\d&]+)(?:-[A-Z]{2})?\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add("");
			bodyCR.add(line);
		}

		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "InstrumentNumber" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });

			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			// m.put("CrossRefSet", cr); - DO NOT PARSE CROSSREF FROM LEGAL
		}

		// extract phase from legal description
		p = Pattern.compile("\\bPH(?:ASE)?(( (\\d+\\w?|\\b[A-Z]\\b))+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(1).trim());
		}

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		p = Pattern.compile("\\b(LT)S?\\s*([\\d&\\s]+\\s+|(?:\\d+-[A-Z]))\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
		}
		lot = lot.trim();
		lot = lot.replaceFirst("\\-$", "");
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, true, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}

		// extract unit from legal description
		p = Pattern.compile("\\b(UN)I?T?\\s*([\\d-A-Z]+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(2));
		}

		// extract block from legal description
		String block = ""; // can have multiple occurrences
		p = Pattern.compile("\\b(BLK)S?\\s*([A-Z\\d]+)\\b");
		ma = p.matcher(origLegal);
		String firstBlk = "";
		while (ma.find()) {
			String newBlk = ma.group(2);
			if (block.length() == 0) {
				firstBlk = newBlk;
				block = firstBlk;
			} else if (firstBlk.trim().matches("\\d+") && newBlk.matches(".*[A-Z].*")) {

			} else {
				block = block + " " + newBlk;
			}
		}
		block = block.trim();
		block = block.replaceFirst("\\-$", "");
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
	}

	public static void legalFLSarasotaDASLRV(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legal = legal.replaceAll("&", " ");

		legal = legal.replaceAll("\\bORDN #?[\\d-]+\\s*", "");
		legal = legal.replaceAll("\\bNO\\b\\s*", "");
		legal = legal.replaceAll("(\\d+)(ST|ND|RD|TH) UNIT", "UNIT $1");
		legal = legal.replaceAll("\\b(A )?REPLAT( OF)?\\b\\s*", ""); // fix for
																		// bug
																		// #1998
		legal = legal.replaceAll("\\bTHRU\\b", "-");
		legal = legal.replaceAll("\\s*\\+\\s*", "");
		legal = legal.replaceAll(",", " ");

		// String subdiv = legal;
		// //cleanup legal descr before extracting subdivision name
		// subdiv =
		// subdiv.replaceAll("^TRACTS? (DESC AS )?BEG \\d+ ([SWNE]+ )?FT ([SENW]+ )?OF ([SENW]+ )?COR OF( (LOTS?|BL(OC)?K|UNIT) \\d+)*",
		// "");
		// subdiv =
		// subdiv.replaceAll("^SUBDIVISION OF TRACTS? \\d+( AND \\d+)? OF", "");
		// subdiv =
		// subdiv.replaceAll("^C?OMM AT [SWNE]+ COR OF( (LOTS?|BL(OC)?K|UNIT) \\d+)*.",
		// "");
		// subdiv =
		// subdiv.replaceAll("^\\s*(\\d+(ST|ND|RD|TH) )?ADD(ITIO)?N?( TO)?\\b\\s*",
		// "");
		// subdiv = subdiv.replaceAll("([\\d\\s]+)(.+)", " $2");
		// subdiv = subdiv.trim();
		//
		//
		// Pattern p =
		// Pattern.compile(".*\\b(?:LOTS?|BLKS?|PH|BLDG|UNIT) (.+?) (?:UNIT|PH|PHASE|SEC(?! OF\\b)|ORI?)\\b.*");
		// Matcher ma = p.matcher(legal);
		// if (ma.find()){
		// subdiv = ma.group(1);
		// } else {
		// p = Pattern.compile(".*\\b(?:LOTS?|BLKS?|UNIT|PH|BLDG|ORI) (.+)");
		// ma.usePattern(p);
		// ma.reset();
		// if (ma.find()){
		// subdiv = ma.group(1);
		// }
		// }
		// subdiv =
		// subdiv.replaceAll("\\s*\\d+\\s*(ST|ND|RD|TH)\\s*(ADD)? (TO)?", "");
		// subdiv = subdiv.replaceAll("\\s+[ANWSE]$", "");
		// subdiv = subdiv.replaceFirst("\\s+SEE$", "");
		// subdiv = subdiv.replaceFirst("\\s*SFT\\s+OF\\s*", "");
		// subdiv = subdiv.replaceFirst("(^[A-Z]) (.+)", "$2");
		// subdiv = subdiv.replaceFirst("([\\d-]+|[A-Z][\\d-]+) (.+)", "$2");
		// subdiv = subdiv.replaceFirst("([\\d-]+)(.+)", "$2");
		// subdiv = subdiv.replaceFirst("(.+) A?\\s*(COND.*)", "$1");
		//
		// if (subdiv.length() != 0){
		// m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
		// if (legal.contains("CONDO"))
		// m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
		// }

		String origLegal = legal;
		String tokens[] = { "X", "L", "C", "D", "M" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, tokens); // convert
																		// roman
																		// numbers
																		// to
																		// arabics
		legal = replaceNumbers(legal);

		// extract section from legal description
		String sec = "";
		Pattern p = Pattern.compile("\\bSEC(?:TION)?\\s*(\\d+|\\w( \\w)*|\\w\\w)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			sec = sec + " " + ma.group(1);
		}
		sec = sec.trim();
		if (sec.length() != 0) {
			sec = LegalDescription.cleanValues(sec, true, true);
			m.put("PropertyIdentificationSet.SubdivisionSection", sec);
		}

		legal = cleanLegalGenericDASLRV(legal);
		origLegal = cleanLegalGenericDASLRV(origLegal);

		// extract tract
		String tract = ""; // can have multiple occurrences
		p = Pattern.compile("\\bTRACTS? (\\d+( \\d+)*|\\w( \\w)*|\\w\\w)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(1);
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, true, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}

		// extract building #
		p = Pattern.compile("\\bBLDG ([A-Z]|\\d+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("\\b(OR) (\\d+)/(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add(ma.group(3));
			line.add("");
			bodyCR.add(line);
		}
		p = Pattern.compile("\\b(ORI)? (\\d{10})\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(2));
			bodyCR.add(line);
		}
		/*
		 * p = Pattern.compile("\\b(\\d{10})\\b"); ma.usePattern(p); ma.reset();
		 * while (ma.find()){ List<String> line = new ArrayList<String>();
		 * line.add(""); line.add(""); line.add(ma.group(1)); bodyCR.add(line);
		 * }
		 */
		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "InstrumentNumber" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });

			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			// m.put("CrossRefSet", cr); - DO NOT PARSE CROSSREF FROM LEGAL
		}

		// extract phase from legal description
		p = Pattern.compile("\\bPH(?:ASE)?(( (\\d+\\w?|\\b[A-Z]\\b))+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(1).trim());
		}

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		p = Pattern.compile("\\bLOTS? ([\\d\\s&-]+|\\d+[A-Z])\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
		}
		lot = lot.trim();
		lot = lot.replaceFirst("\\s*&\\s*", "");
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, true, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}

		// extract unit from legal description
		p = Pattern.compile("\\bUNITS?\\s+([A-Z\\d-]+|[-\\d\\s]+|\\d+-?[A-Z])\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(1).replaceFirst("^0+(\\w.*)", "$1"));
		}

		// extract block from legal description
		String block = ""; // can have multiple occurrences
		p = Pattern.compile("\\bBLKS? ((?:\\b[A-Z]\\b|\\d+|&|\\s)+)\\b");
		ma = p.matcher(origLegal);
		String firstBlk = "";
		while (ma.find()) {
			String newBlk = ma.group(1);
			if (block.length() == 0) {
				firstBlk = newBlk;
				block = firstBlk;
			} else if (firstBlk.trim().matches("\\d+") && newBlk.matches(".*[A-Z].*")) {

			} else {
				block = block + " " + newBlk;
			}
		}
		block = block.trim();
		block = block.replaceFirst("\\s*&\\s*", "");
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
	}

	public static void legalFLStJohnsDASLRV(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legal = legal.replaceAll("\\bORDN #?[\\d-]+\\s*", "");
		legal = legal.replaceAll("\\bNO\\b\\s*", "");
		legal = legal.replaceAll("(\\d+)(ST|ND|RD|TH) UNIT", "UNIT $1");
		legal = legal.replaceAll("\\b(A )?REPLAT( OF)?\\b\\s*", "");
		legal = legal.replaceAll("[\\d\\.]+\\s*FT\\s*(OF|ON)?\\s*(N|W|S|E|SE|SW|NE|NW)?\\b", "");
		legal = legal.replaceAll("\\bTHRU\\b", "-");
		legal = legal.replaceAll("&\\s*(\\d{2,})\\s*/\\s*(\\d{2,})", "& OR$1/$2");
		legal = legal.replaceAll("OR\\s*(\\d+)\\s*/\\s*(\\d+)\\s&(\\d+)", "OR$1/$2 & OR$1/$3");
		legal = legal.replaceAll("(TRACT)\\s+OF", " $1 ");
		legal = legal.replaceAll("&", " ");
		legal = legal.replaceAll("\\(", "");
		legal = legal.replaceAll("\\)", "");

		// String subdiv = legal;
		// //cleanup legal descr before extracting subdivision name
		// /*subdiv =
		// subdiv.replaceAll("^TRACTS? (DESC AS )?BEG \\d+ ([SWNE]+ )?FT ([SENW]+ )?OF ([SENW]+ )?COR OF( (LOTS?|BL(OC)?K|UNIT) \\d+)*",
		// "");
		// subdiv =
		// subdiv.replaceAll("^SUBDIVISION OF TRACTS? \\d+( AND \\d+)? OF", "");
		// subdiv =
		// subdiv.replaceAll("^C?OMM AT [SWNE]+ COR OF( (LOTS?|BL(OC)?K|UNIT) \\d+)*.",
		// "");
		// subdiv =
		// subdiv.replaceAll("^\\s*(\\d+(ST|ND|RD|TH) )?ADD(ITIO)?N?( TO)?\\b\\s*",
		// "");*/
		// subdiv = subdiv.replaceAll("([\\d\\s]+)(.+)", " $2");
		// subdiv = subdiv.replaceAll("PT\\s*OF", "");
		// subdiv = subdiv.replaceAll("SEC(.*)(SEC)", "$1 $2");
		// subdiv = subdiv.replaceAll("(.+) TRACT.*", "$1");
		// subdiv = subdiv.trim();
		//
		// Pattern p =
		// Pattern.compile("^(?:[\\d/\\s,&-]+[A-Z]?\\s)?\\b(.*?)\\s*-?\\s*\\b(?:U(?:NI)?T|SEC|BLDG|LOTS?|PHASES?|CONDO|SUBD?|PARCELS?|(?:\\d+(?:ST|ND|RD|TH) )?ADDN|RESUB|BLK|GL|SECS?|LYING|BOUNDED)\\b.*");
		// Matcher ma = p.matcher(subdiv);
		// if (ma.find()){
		// subdiv = ma.group(1);
		// subdiv = subdiv.replaceAll("\\bUNREC\\b", "");
		// subdiv = subdiv.replaceAll("\\bPLAT\\b", "");
		// subdiv = subdiv.replaceAll("\\bMAP\\b", "");
		// subdiv = subdiv.replaceAll("\\b[\\d,\\s&-]+$", "");
		// subdiv = subdiv.replaceAll("^[\\d,\\s&-]+\\b", "");
		// subdiv = subdiv.replaceAll("\\s*&\\s*$", "");
		// if (subdiv.matches(".+ TR(ACT)?")){
		// subdiv = "";
		// }
		// subdiv = subdiv.replaceFirst("(.*) OR\\s*\\d+.*", "$1");
		// }
		// if (subdiv.length() == 0){
		// p =
		// Pattern.compile(".*\\b(?:U(?:NI)?T|TRACT|PARCEL|LOT|SEC)S? [&\\s\\d]+[A-Z]?(?: OF)? (.*?)(?: OF)? UNREC\\b.*");
		// ma = p.matcher(legal);
		// if (ma.find()){
		// subdiv = ma.group(1);
		// if (subdiv.contains("LYING") || subdiv.trim().equals("OF"))
		// subdiv = "";
		// }
		// }
		// if (subdiv.length() != 0){
		// subdiv = subdiv.replaceFirst("(.+) CONDO(MINIUM)?", "$1");
		// m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
		// if (legal.matches(".*\\bCONDO\\b.*")){
		// m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
		// }
		// }

		String origLegal = legal;
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
																					// to
																					// arabics
		legal = replaceNumbers(legal);

		// extract section from legal description
		String sec = "";
		Pattern p = Pattern.compile("\\bSEC(?:TION)?\\s*(\\d+|\\w( \\w)*|\\w\\w)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			sec = sec + " " + ma.group(1);
		}
		sec = sec.trim();
		if (sec.length() != 0) {
			sec = LegalDescription.cleanValues(sec, true, true);
			m.put("PropertyIdentificationSet.SubdivisionSection", sec);
		}

		legal = cleanLegalGenericDASLRV(legal);
		origLegal = cleanLegalGenericDASLRV(origLegal);

		// extract tract
		String tract = ""; // can have multiple occurrences
		p = Pattern.compile("\\bTRACTS? (\\d+( \\d+)*|\\w( \\w)*|\\w\\w)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(1);
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, true, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}

		// extract building #
		p = Pattern.compile("\\bBLDG (\\d+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
		}

		// extract plat book & page from legal description
		p = Pattern.compile("^\\(?(\\d+)(/\\d+[A-Z]?(?:[\\s-]+\\d+[A-Z]?)*|\\s*-\\s*\\d+[A-Z]?(?:\\s\\d+[A-Z]?)*)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.PlatBook", ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			m.put("PropertyIdentificationSet.PlatNo", ma.group(2).replaceFirst("^[/-]", "").replaceFirst("^0+(\\d+)", "$1"));
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("\\b(OR)\\s*(\\d+)\\s*/\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add(ma.group(3));
			line.add("");
			bodyCR.add(line);
		}

		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "InstrumentNumber" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });

			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			// m.put("CrossRefSet", cr); - DO NOT PARSE CROSSREF FROM LEGAL
		}

		// extract phase from legal description
		p = Pattern.compile("\\bPHASES?(( ([\\d&]+\\w?|\\b[A-Z]\\b))+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(1).trim());
		}

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		p = Pattern.compile("\\bLOTS? ((?:[A-Z])?|\\d+(\\-?[A-Z])?(?:[\\s-]+\\d+(\\-?[A-Z])?)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(1).replaceFirst("[\\s-]+$", "");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}

		// extract unit from legal description
		p = Pattern.compile("\\b(?:UNIT|UT)\\s+([A-Z\\d-]+|[-\\d\\s]+|\\d+-?[A-Z])\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(1).replaceFirst("^0+(\\w.*)", "$1"));
		}

		// extract block from legal description
		String block = ""; // can have multiple occurrences
		p = Pattern.compile("\\b(?:BLK|BLOCK)\\s+((\\d+|\\b[A-Z]\\b|-|\\s)+)");
		ma = p.matcher(origLegal);
		String firstBlk = "";
		while (ma.find()) {
			String newBlk = ma.group(1);
			if (block.length() == 0) {
				firstBlk = newBlk;
				block = firstBlk;
			} else if (firstBlk.trim().matches("\\d+") && newBlk.matches(".*[A-Z].*")) {

			} else {
				block = block + " " + newBlk;
			}
		}
		block = block.trim();
		block = block.replaceFirst("\\-$", "");
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
	}

	public static void legalFLBayDASLRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerLEGALFLBayRV(m, legal);
	}

	public static void legalTokenizerLEGALFLBayRV(ResultMap m, String legal) throws Exception {

		// corrections
		legal = legal.replaceAll("\\b(L-\\d+)(B-([A-Z]|\\d+))\\b", "$1 $2");
		legal = legal.replaceAll("\\bOR B\\b", "ORB");
		legal = legal.replaceAll("\\b0RB(?= \\d)", "ORB");
		legal = legal.replaceAll("\\b(ORB \\d+) (\\d+)(?= P\\s*\\d)", "$1$2");
		legal = legal.replaceAll("(?<=[A-Z]|\\d)(ORB|LOT)(?= \\d)", " $1");
		legal = legal.replaceAll("(?<=[A-Z]|\\d)(BLK)(?= [A-Z\\d])", " $1");
		legal = legal.replaceAll("\\bBL K(?= ([A-Z]|\\d))", "BLK");
		legal = legal.replaceAll("(\\d+)(?:ST|ND|RD|TH) (SEC)", "$2 $1");

		// cleanup
		legal = legal.replaceAll("&", " ");
		legal = legal.replaceAll("\\bNO\\b\\s*", "");
		legal = legal.replaceAll("#\\s*(?=[A-Z\\d])", " ");
		legal = legal.replaceAll("\\sTHRU\\s", "-");
		legal = legal.replaceAll(",", " ");
		legal = legal.replaceAll("(?<=[A-Z])\\.", " ");
		legal = legal.replaceAll("\\bTYPE [\\d[A-Z]-]+\\b", "@");
		legal = legal.replaceAll("\\b\\d+\\s*BR([\\s/]\\d+\\s*BA\\b)?", "@");
		legal = legal.replaceAll("\\d*(\\s*\\.\\s*)?\\d+ SQ FT\\b", " ");
		legal = replaceNumbers(legal);

		legal = legal.replaceAll("\\s+AND\\s+", " ");
		legal = legal.replaceAll("(?<=\\d)\\s+TO\\s+(?=\\d)", "-");
		legal = legal.replaceAll("\\s*\\b[SWNE]+ \\d+((\\.|/)\\d+)?", "");
		legal = legal.replaceAll("\\s*\\b\\d+\\.\\d+\\b", "");

		legal = legal.replaceAll("\\s{2,}", " ");

		String origLegal = legal;
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = legal.replaceAll("(?<=\\b[VX])-", "__");
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
		legal = legal.replace("__", "-");

		// extract and remove sec-twn-rng from legal description
		List<List> body = new ArrayList<List>();
		Pattern p = Pattern.compile("^(\\d+(?:/\\d+)*)[-\\s](\\d+[SWEN]?)[-\\s](\\d+[SWEN]?)\\b");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceAll("/", " "));
			line.add(ma.group(2));
			line.add(ma.group(3));
			body.add(line);
			legal = legal.replace(ma.group(0), "");
		}
		p = Pattern.compile("(\\d+(?:/\\d+)*)[-\\s](\\d+[SWEN])[-\\s](\\d+[SWEN])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceAll("/", " "));
			line.add(ma.group(2));
			line.add(ma.group(3));
			body.add(line);
			legal = legal.replace(ma.group(0), "");
		}
		legal = legal.replaceFirst("\\s*-\\s*\\d+\\.\\d+[A-Z]?\\s*-", " ").replaceAll("\\s{2,}", " ");
		p = Pattern.compile("\\bSEC(?:TION)? (\\d+)\\b(?![\\.'/])");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add("");
			line.add("");
			body.add(line);
			legal = legal.replace(ma.group(0), "SEC ");
		}
		if (!body.isEmpty()) {
			String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
			map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
			map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(body);
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);

		}
		legal = legal.replaceAll("-{2,}", " ");
		legal = legal.replaceAll("\\s{2,}", " ").trim();

		// extract tract
		String tract = ""; // can have multiple occurrences
		p = Pattern.compile("\\bTRACTS? (\\d+( \\d+)*|\\w( \\w)*|\\w\\w)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(1);
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, true, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}

		// extract building #
		p = Pattern.compile("\\b(?:BLDG|BUILDING) ([A-Z]{1,2}(?:-?\\d+)?|\\d+(?:-?[A-Z])?)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("\\b(OR(?:BM?)?|OB|DB)\\s?(\\d+)(?:\\s(?:N?PG?\\s?)?|N?PG?\\s)(\\d+(?:[\\s-]+(?:PG?\\s?)?\\d+(?! P |\\s*[-\\.]))*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			String page = ma.group(3).replaceAll("\\s*-\\s*", "-").replaceAll("P\\s?", "");
			page = page.replaceFirst("^(\\d{2,}) \\d\\b.*", "$1");
			line.add(page);
			line.add(ma.group(1));
			bodyCR.add(line);
			legal = legal.replace(ma.group(0), "");
		}
		p = Pattern.compile("\\b(\\d+)(?:\\sN?PG?\\s?|N?PG?\\s)(\\d+(?:[\\s-]+\\d+)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			String page = ma.group(2).replaceAll("\\s*-\\s*", "-");
			page = page.replaceFirst("^(\\d{2,}) \\d\\b.*", "$1");
			line.add(page);
			line.add("");
			bodyCR.add(line);
		}
		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "InstrumentNumber" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });

			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			// m.put("CrossRefSet", cr); - DO NOT PARSE CROSSREF FROM LEGAL
		}

		// extract phase from legal description
		p = Pattern.compile("\\bPH(?:ASE)?\\s*\\b(\\d+(?:-?[A-Z])?(?: \\d+)*|[A-Z])\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(1).trim().replaceFirst("\\bI\\b", "1"));
		}

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("\\b(?:UNITS?|APT)\\s+([A-Z]{1,2}(?:[-\\s\\d]+)?(?:\\b[A-Z]-\\d+)*|\\d+[\\s-][A-Z](?!-)|\\d+-?\\d*[A-Z]\\d*|[-/\\d\\s]+)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			unit = unit + " " + ma.group(1).replaceFirst("^0+(\\w.*)", "$1").replace('/', ' ');
			legal = legal.replace(ma.group(0), "@");
		}
		p = Pattern.compile("\\bU-([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replace(ma.group(0), "@");
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
		}

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		p = Pattern.compile("\\bLO?TS? (\\d+-?[A-Z]|[\\d\\s-]+)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
		}
		p = Pattern.compile("\\bLO?TS? ([A-Z]\\d*(?:-\\d+)?(?: [A-Z])?)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
		}
		p = Pattern.compile("\\bL[-\\s](\\d+[A-Z]?(?: \\d+[A-Z]?)*)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}

		// extract block from legal description
		String block = ""; // can have multiple occurrences
		p = Pattern.compile("\\bBLKS?[-\\s](\\d+(?:-?[A-Z])?(?:\\s\\d+)*|[A-Z](?:-?\\d+)?)\\b");
		ma = p.matcher(origLegal);
		while (ma.find()) {
			block = block + " " + ma.group(1);
		}
		p = Pattern.compile("\\bB-([A-Z]|\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1);
		}
		block = block.trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
	}

	public static void legalFLFranklinDASLRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerLEGALFLFranklinRV(m, legal);
	}

	public static void legalTokenizerLEGALFLFranklinRV(ResultMap m, String legal) throws Exception {

		// initial cleanup and correction of the legal description
		legal = legal.replaceAll("\\b((RE)?PLAT)T", "$1");
		legal = legal.replaceAll("\\bCONATINING\\b", "CONTAINING");
		legal = legal.replaceAll("\\b0RB?(?= \\d)", "ORB");
		legal = legal.replaceAll("\\b(\\d+)(ORB?)(?=/\\d)\\b", "$1 $2");
		legal = legal.replaceAll("(?<=[A-Z])\\.", " ");
		legal = legal.replaceAll("\\bNO\\s+(?=\\d)", " ");
		legal = legal.replaceAll("[\"\\(\\)#]", "");
		legal = legal.replaceAll("\\bNUMBER(?= \\d)", "");
		legal = legal.replaceAll("\\bCASE\\s+\\d+\\w*(-\\w+)?\\b", "");
		legal = legal.replaceAll("\\+", "&");
		legal = legal.replaceAll("\\sTHRU\\s", "-");
		legal = legal.replaceAll("\\s?\\bALL( OF)?\\b", "");
		legal = legal.replaceAll("\\b([SWEN] )?PART OF\\b", "");
		legal = legal.replaceAll("(?<=\\d,? )AND(?=\\s+\\d)", "&");
		legal = legal.replaceAll("\\b(PROBATE (FILE|CASE)|BOUNDARY SURVEY)\\s+[\\d\\s-]*\\b(?!/)", "");
		legal = legal.replaceAll("\\bCASE [\\dA-Z-]+\\b", "");
		legal = legal.replaceAll("\\bTAX (CERT(IFICATE)?|DEED) [\\d\\s-]*", "");
		legal = legal.replaceAll("\\bCONTRACT FOR (DEED|SALE)\\b", "");
		legal = legal.replaceAll("\\bUS (LOTS?)\\b", "$1");
		legal = legal.replaceAll("\\bM/L\\b", "");
		legal = legal.replaceAll("\\bORIGINAL ACRES\\b", "");
		legal = legal.replaceAll("(\\bAPP(ROX)? )?(\\bA )?\\.?\\b(?<![-/])[\\d\\./]+\\s*AC(RES?)?([/ ]ML)?( RR/RW)?(( RECD)? IN)?\\b", "");
		legal = legal.replaceAll("\\b(AN )?UN[\\s'-]?(RECR?('?D)?|RECORD(ED)?|RCR?D|REED|R)( (PL(AT)?|SUB))?\\b", "");
		legal = legal.replaceAll("\\b(THE )?([SWEN]{1,2} )?[\\d\\.]+(\\s*X\\s*[\\d\\.]+)+\\s?(F(EE)?T\\b|(DEG\\b|')( \\d+)?)?(\\s?\\bOF )?", "");
		legal = legal
				.replaceAll(
						"(& |\\bAND )?\\b(THE )?((EAST|WEST|NORTH?|SOU(TH)?)(ERLY)? |[SWEN]{1,2} )[\\d\\.\\s/]+\\s?(F(EE)?T|(DEG\\b|')( \\d+)?)(\\s?X\\s?[\\d\\.]+\\s?FT)*(\\s?X)?( IN( THE)? | OF )?",
						" ");
		legal = legal
				.replaceAll(
						"(& |\\bAND )?\\b(THE )?((EAST|WEST|NORTH?|SOU(TH)?)(ERLY)? |[SWEN]{1,2} )?[\\d\\./]+\\s?(F(EE)?T|(DEG\\b|')( \\d+)?)(\\s?X\\s?[\\d\\.]+\\s?FT)*(\\s?X)?( IN( THE)? | OF )?",
						" ");
		legal = legal.replaceAll(
				"(& |\\bAND )?\\b(THE )?((EAST|WEST|NORTH?|SOU(TH)?)(ERLY)? |[SWEN]{1,2} )?[\\d\\./]+\\s?(\\s?X\\s?[\\d\\.]+\\s?)+(\\s?X)?( IN( THE)? | OF )?",
				" ");
		legal = legal.replaceAll("(& |\\bAND )?\\b(THE )?(EAST|WEST|NORTH|SOU(TH)?|(?<!\\bORB? )[SWEN]{1,2})\\s?\\d[\\d/]*( F(EE)?T)?( OF)?\\b", "");
		legal = legal.replaceAll("(\\b\\d+)?\\.\\d+\\b", "");
		legal = legal.replaceAll("\\bINC\\b", "");
		legal = legal.replaceAll("\\bA[ /]?K[ /]?A( NOW)?\\b", "AKA");
		legal = legal.replaceAll("\\b(ALSO )?(NOW )?KNOWN AS( NOW)?\\b", "AKA");
		legal = legal.replaceAll("(?<=\\bAKA )\\d+/\\d+\\b", "");
		legal = legal.replaceAll("\\bORB? ORB?\\b", "ORB");
		legal = legal.replaceAll("(?<!\\b(PB|PLAT|ORB?)\\s?)\\b\\d+/\\d+(?= LOTS?\\b)", "");
		// legal = legal.replaceAll("\\bORB?/?\\d+/\\d+(?:-\\d+)?\\b", "");
		legal = legal.replaceAll("\\s{2,}", " ").trim();

		List<String> line = new ArrayList<String>();

		// remove cross ref book & page from legal
		String pagePatt = "\\d+(\\s?[&\\s,/-]\\s?\\d+(?!/))*";
		Pattern p = Pattern.compile("\\bORB?[\\s/]?(\\d+|\\b[A-Z]{1,2})[\\s/-]+(" + pagePatt + ")\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "ORB ");
		}
		p = Pattern.compile("(?<!\\b(?:PB|PLAT|UNDIVIDED) )\\b(\\d+)(?<!OR)/(" + pagePatt + ")\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "ORB ");
		}
		p = Pattern.compile("\\bORB? (\\d+) (?:PAGE|PG) (\\d+(?:\\s*[&,-]\\s*\\d+)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "ORB ");
		}
		legal = legal.replaceAll("\\s{2,}", " ");

		// extract and replace plat b&p from legal description
		List<List> bodyPlat = new ArrayList<List>();
		p = Pattern.compile("\\bPB\\s?0*(\\d+)/0*(\\d+(?:-\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			bodyPlat.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PLAT ");
		}
		p = Pattern.compile("\\bPLAT(( (\\d+|[A-Z]{2})-\\d+)+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			Matcher ma2 = Pattern.compile("\\b(\\d+|[A-Z]{2})-(\\d+)\\b").matcher(ma.group(1));
			while (ma2.find()) {
				line = new ArrayList<String>();
				line.add(ma2.group(1));
				line.add(ma2.group(2));
				bodyPlat.add(line);
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PLAT ");
		}
		p = Pattern.compile("\\bPLAT(( (\\d+|[A-Z]{2})/\\d+)+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			Matcher ma2 = Pattern.compile("\\b(\\d+|[A-Z]{2})/(\\d+)\\b").matcher(ma.group(1));
			while (ma2.find()) {
				line = new ArrayList<String>();
				line.add(ma2.group(1));
				line.add(ma2.group(2));
				bodyPlat.add(line);
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PLAT ");
		}
		legal = legal.replaceAll("\\bORB? \\d+(?= [A-Z])", "ORB ");
		if (bodyPlat.isEmpty()) {
			p = Pattern.compile("\\b(LOTS? \\d+(?:(?:[ &]+\\d+)*|(?:-\\d+){2,}))((?: (?:\\d+|[A-Z]{2})[-/]\\d+)+)\\b");
			ma = p.matcher(legal);
			while (ma.find()) {
				Matcher ma2 = Pattern.compile("\\b(\\d+|[A-Z]{2})-(\\d+)\\b").matcher(ma.group(2));
				while (ma2.find()) {
					line = new ArrayList<String>();
					line.add(ma2.group(1));
					line.add(ma2.group(2));
					bodyPlat.add(line);
				}
				ma2 = Pattern.compile("\\b([A-Z]{2})/(\\d+)\\b").matcher(ma.group(2));
				while (ma2.find()) {
					line = new ArrayList<String>();
					line.add(ma2.group(1));
					line.add(ma2.group(2));
					bodyPlat.add(line);
				}
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", ma.group(1) + " PLAT ");
			}
			p = Pattern.compile("\\b([A-Z]{2})-(\\d+)\\b");
			ma = p.matcher(legal);
			while (ma.find()) {
				line = new ArrayList<String>();
				line.add(ma.group(1));
				line.add(ma.group(2));
				bodyPlat.add(line);
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PLAT ");
			}
		}
		if (!bodyPlat.isEmpty()) {
			String[] header = { "PlatBook", "PlatNo" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("PlatBook", new String[] { "PlatBook", "" });
			map.put("PlatNo", new String[] { "PlatNo", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(bodyPlat);
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
			legal = legal.replaceAll("\\s{2,}", " ");
		}

		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = legal.replaceAll("(?<=\\b[VX])-", "__");
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
		legal = legal.replace("__", "-");

		legal = legal.replaceAll("\\b\\d+/\\d+(?:-\\d+)?\\b", "");

		// extract and replace lot from legal description
		String lot = ""; // can have multiple occurrences
		p = Pattern.compile("\\bLOTS?\\s?(\\d+[A-Z]?(?:-[A-Z])?(?:[-&,\\s]+\\d+[A-Z]?)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			String lotTemp = ma.group(1);
			lotTemp = lotTemp.replaceAll("[,&\\s]", " ");
			if (lotTemp.matches("\\d+(-\\d+){2,}")) {
				lotTemp = lotTemp.replace('-', ' ');
			}
			lotTemp = lotTemp.replaceAll("\\s*-\\s*", "-");
			lot = lot + " " + lotTemp;
			legal = legal.replace(ma.group(0), "LOT ");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace phase from legal description
		String phase = "";
		p = Pattern.compile("\\bPH(?:ASES?)? (\\d+(?:-?[A-Z])?( & \\d+)*|I)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = ma.group(1).replaceFirst("\\bI", "1");
			phase = phase.replaceAll("\\s?&\\s?", " ");
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legal = legal.replace(ma.group(0), "PHASE ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace sec-twn-rng from legal description
		List<List> bodySTR = new ArrayList<List>();
		p = Pattern.compile("\\bSEC(?:TION)?S? ((?:\\d+ & )?\\d+)[- ]+T?0*(\\d+[SWEN])[- ]+R?0*(\\d+[SWEN])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1).replaceAll("\\s?&\\s?", " ").replaceFirst("^0+(\\d)", "$1"));
			line.add(ma.group(2));
			line.add(ma.group(3));
			bodySTR.add(line);
			legal = legal.replace(ma.group(0), "SEC ");
		}
		p = Pattern.compile("\\b0*(\\d+)-0*(\\d+[SWEN])-0*(\\d+[SWEN])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add(ma.group(3));
			bodySTR.add(line);
			legal = legal.replace(ma.group(0), "SEC ");
		}
		p = Pattern.compile("\\bSEC (\\d+) TWN (\\d+[SWEN]) RA?N(?:GE)? (\\d+[SWEN])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add(ma.group(3));
			bodySTR.add(line);
			legal = legal.replace(ma.group(0), "SEC ");
		}
		p = Pattern.compile("\\bSEC(?:TIONS?)? (\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add("");
			line.add("");
			bodySTR.add(line);
			legal = legal.replace(ma.group(0), "SEC ");
		}
		if (!bodySTR.isEmpty()) {
			String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
			map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
			map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(bodySTR);

			ResultTable pisPLAT = (ResultTable) m.get("PropertyIdentificationSet");
			if (pisPLAT != null) {
				pis = ResultTable.joinHorizontal(pis, pisPLAT);
				map.putAll(pisPLAT.map);
			}
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
			legal = legal.replaceAll("\\s{2,}", " ").trim();
		}
		p = Pattern.compile("\\bR(?:(?:AN)?GE)?\\s?((?:\\d+|[A-Z]-\\d+)(?: \\d+)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionRange", ma.group(1));
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "RNG ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace block from legal description
		String block = "";
		p = Pattern.compile("\\bBLOCKS ([A-Z](-[A-Z]){2,})\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1).replace('-', ' ');
			legal = legal.replace(ma.group(0), "BLK ");
		}
		p = Pattern
				.compile("\\bBL(?:(?:OC)?KS?)?[ -]([A-Z](?:[- ]\\d+)*(?:\\s*&\\s*[A-Z])*|\\d+(?:(?:/|\\s*&\\s*)?[A-Z]|\\s?[SWEN])?(?:[\\s-]\\d+)*)(?: R(?:(?:AN)?GE)?\\s?\\d+(?: \\d+)?)?\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1).replace('/', '-').replaceAll("\\s*&\\s*", " ").replaceAll("\\b(\\d+) ([SWEN])\\b", "$1$2");
			legal = legal.replace(ma.group(0), "BLK ");
		}
		block = block.trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace unit from legal description
		String unit = "";
		p = Pattern.compile("\\bUNIT (\\d+|[A-Z](?:-\\d+)?)\\b");
		ma = p.matcher(replaceNumbers(legal));
		while (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\bUNIT (\\d+|[A-Z](?:-\\d+)?)\\b", "UNIT ");
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace building # from legal description
		p = Pattern.compile("\\b(?:BUILDING|BLDG|BLDING) (\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
			legal = legal.replace(ma.group(0), "BLDG ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace tract from legal description
		String tract = "";
		p = Pattern.compile("\\bTR(?:ACTS?)? (\\d+(?:\\s?[A-Z])?(?:[\\s&,-]+\\d+[A-Z]?)*|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(1).replaceAll("\\s?[&,]\\s?", " ");
			legal = legal.replace(ma.group(0), "TRACT ");
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// if no plat was find, check if the legal doesn't end with \\d+-\\d+,
		// which may be the pla b&p
		// (e.g. PID 20070442300A020090, BL A R2 64 THE SOUTH 40 FT OF LOT 9 AND
		// ALL LOT 10 PICKETTS ADD 96-150 97-417)
		if (bodyPlat.isEmpty()) {
			legal = legal.replaceFirst("( ORB?)+$", "");
			p = Pattern.compile("((?: (\\d+|[A-Z]{2})-(\\d+)\\b)+)$");
			ma = p.matcher(legal);
			if (ma.find()) {
				Matcher ma2 = Pattern.compile("(\\d+|[A-Z]{2})-(\\d+)\\b").matcher(ma.group(1));
				while (ma2.find()) {
					line = new ArrayList<String>();
					line.add(ma2.group(1));
					line.add(ma2.group(2));
					bodyPlat.add(line);
				}
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PLAT ");
			}
			if (!bodyPlat.isEmpty()) {
				String[] header = { "PlatBook", "PlatNo" };
				Map<String, String[]> map = new HashMap<String, String[]>();
				map.put("PlatBook", new String[] { "PlatBook", "" });
				map.put("PlatNo", new String[] { "PlatNo", "" });

				ResultTable pis = new ResultTable();
				pis.setHead(header);
				pis.setBody(bodyPlat);

				ResultTable pisSTR = (ResultTable) m.get("PropertyIdentificationSet");
				if (pisSTR != null) {
					pis = ResultTable.joinHorizontal(pis, pisSTR);
					map.putAll(pisSTR.map);
				}
				pis.setMap(map);
				m.put("PropertyIdentificationSet", pis);
				legal = legal.replaceAll("\\s{2,}", " ");
			}
		}
	}

	public static void legalFLLeeDASLRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerLEGALFLLeeRV(m, legal);
	}

	public static void legalTokenizerLEGALFLLeeRV(ResultMap m, String legal) throws Exception {

		// corrections
		legal = legal.replaceAll("\\bPG (\\d+ PG \\d+)\\b", "PB $1");
		legal = legal.replaceAll("\\b(OR \\d+) (G \\d+)\\b", "$1 P$2");
		legal = legal.replaceAll("\\b(\\d+)(UNIT|BLDG)\\b", "$1 $2");

		// cleanup
		legal = legal.replaceAll("[&\\+]", " ");
		legal = legal.replaceAll("\\bNO\\b\\s*", "");
		legal = legal.replaceAll("#\\s*(?=[A-Z\\d])", " ");
		legal = legal.replaceAll("\\sTHRU\\s", "-");
		legal = legal.replaceAll("\\bPT(?=\\d|\\b)", "");
		legal = legal.replaceAll("\\bDESC(?= \\d+/\\d+\\b)", "OR");
		legal = legal.replaceAll(",", " ");
		legal = legal.replaceAll("(?<=[A-Z]\\s?)\\.", " ");
		legal = replaceNumbers(legal);

		legal = legal.replaceAll("\\s+AND\\s+", " ");
		legal = legal.replaceAll("(?<=\\d)\\s+TO\\s+(?=\\d)", "-");
		legal = legal.replaceAll("\\s*(?<!APT )\\b[SWNE]{1,2}\\s*\\d+((\\.|/)\\d+)?( FT)?\\b", "");
		legal = legal.replaceAll("\\s*\\b[SWNE]{1,2}\\d+FT\\b", "");
		legal = legal.replaceAll("\\s*\\b\\d+\\.\\d+\\b", "");
		legal = legal.replaceAll("\\s*-\\s*", "-");
		legal = legal.replaceAll("\\bALL(?= \\d)", "");

		legal = legal.replaceAll("\\s{2,}", " ").trim();

		String origLegal = legal;
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = legal.replaceAll("(?<=\\b[VX])-", "__");
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
		legal = legal.replace("__", "-");

		// extract and remove sec-twn-rng from legal description
		List<List> body = new ArrayList<List>();
		Pattern p = Pattern.compile("\\bSEC(?:T(?:ION)?)? (\\d+) T(?:W[NP])?\\s?(\\d+) R(?:GE)?\\s?(\\d+)\\b");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add(ma.group(3));
			body.add(line);
			legal = legal.replace(ma.group(0), "");
		}

		p = Pattern.compile("\\bSEC(?:T(?:ION)?)?\\s?(\\d+(?:-\\d+)?|[A-Z])\\b(?![\\.'/])");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceAll("\\bI\\b", "1"));
			line.add("");
			line.add("");
			body.add(line);
			legal = legal.replace(ma.group(0), "SEC ");
		}
		saveSTRInMap(m, body);
		legal = legal.replaceAll("\\s{2,}", " ").trim();

		// extract building #
		String bldg = "";
		p = Pattern.compile("\\b(?:BLDG|BUILDING)[\\s-]([A-Z]{1,2}(?:-?\\d+)?|\\d+(?:-?[A-Z])?)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			bldg = bldg + " " + ma.group(1);
			legal = legal.replace(ma.group(0), "@");
		}
		bldg = bldg.trim();
		if (bldg.length() != 0) {
			bldg = LegalDescription.cleanValues(bldg, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legal = legal.replaceAll("\\s{2,}", " ").trim();
		}

		// extract plat book & page from legal description
		List<List> bodyPlat = new ArrayList<List>();
		p = Pattern.compile("\\bPB\\s?0*(\\d+)(?: PGS?\\s?|/)0*(\\d+[A-Z]?(?:[-\\s]\\d+)*)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			bodyPlat.add(line);
			legal = legal.replace(ma.group(0), "");
		}
		if (!bodyPlat.isEmpty()) {
			String[] header = { "PlatBook", "PlatNo" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("PlatBook", new String[] { "PlatBook", "" });
			map.put("PlatNo", new String[] { "PlatNo", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(bodyPlat);

			ResultTable pisSTR = (ResultTable) m.get("PropertyIdentificationSet");
			if (pisSTR != null) {
				pis = ResultTable.joinHorizontal(pis, pisSTR);
				map.putAll(pisSTR.map);
			}
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
		}
		p = Pattern.compile("\\bCPB\\s?(\\d+) PGS?\\s?(\\d+[A-Z]?(?:[-\\s]\\d+)*)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.CondominiumPlatBook", ma.group(1));
			m.put("PropertyIdentificationSet.CondominiumPlatPage", ma.group(2));
			legal = legal.replace(ma.group(0), "");
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		String page = "0*(\\d+)/0*(\\d+(?: \\d+(?!/))?)\\b";
		Pattern p2 = Pattern.compile("\\b" + page);
		Matcher ma2;
		p = Pattern.compile("\\b(OR|DB|MISC|MB|AMEND)((?:\\s?" + page + ")+)");
		ma = p.matcher(legal);
		while (ma.find()) {
			ma2 = p2.matcher(ma.group(2));
			while (ma2.find()) {
				List<String> line = new ArrayList<String>();
				line.add(ma2.group(1));
				line.add(ma2.group(2));
				line.add("");
				line.add(ma.group(1));
				bodyCR.add(line);
			}
			legal = legal.replace(ma.group(0), "");
		}
		p = Pattern.compile("\\b(OR|DB|MISC|MB|AMEND)(?: BK)?\\s?0*(\\d+)\\s?P(?:GS?)?\\s?0*(\\d+(?:[\\s-]\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add(ma.group(3));
			line.add("");
			line.add(ma.group(1));
			bodyCR.add(line);
			legal = legal.replace(ma.group(0), "");
		}
		p = Pattern.compile("\\bINST\\s?([\\d-]+( \\d{5,})*)\\b");
		ma = p.matcher(legal);
		p2 = Pattern.compile("\\b\\d{5,}\\b");
		Pattern p3 = Pattern.compile("^(\\d{4})-(\\d+)\\b");
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			String instr = ma.group(1);
			String prefix = "";
			String instr1 = "";
			ma2 = p3.matcher(instr);
			if (ma2.find()) {
				prefix = ma2.group(1);
				instr1 = ma2.group(2);
				int len = 9 - instr1.length();
				for (int i = 0; i < len; i++) {
					instr1 = "0" + instr1;
				}
				instr1 = prefix + instr1;
			} else {
				instr1 = instr.replaceFirst("^(\\d+)\\b.+", "$1");
			}
			line.add(instr1);
			line.add("");
			bodyCR.add(line);

			String addInstr = ma.group(2);
			if (addInstr != null) {
				ma2 = p2.matcher(addInstr);
				while (ma2.find()) {
					instr1 = ma2.group(0);
					int len = 9 - instr1.length();
					for (int i = 0; i < len; i++) {
						instr1 = "0" + instr1;
					}
					instr1 = prefix + instr1;
					line = new ArrayList<String>();
					line.add("");
					line.add("");
					line.add(instr1);
					line.add("");
					bodyCR.add(line);
				}
			}
			legal = legal.replace(ma.group(0), "");
		}
		// saveCRInMap(m, bodyCR); - DO NOT PARSE CROSSREF FROM LEGAL

		// extract tract
		String tract = ""; // can have multiple occurrences
		p = Pattern.compile("\\bTR(?:ACT)?S? (\\d+([\\s-]\\d+)*|\\w( \\w)*|\\w\\w)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(1);
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("\\b(?:U(?:NI)?TS?|UN|APT)\\s+((?:\\d|[IVX])+(?:-?[A-Z])?(?:-\\d+)?|[A-Z]{1,2}(?:[\\s-]?\\d+)?)\\b");
		p2 = Pattern.compile("\\b([IVX]+)([A-Z]?)");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			String crtUnit = ma.group(1);
			ma2 = p2.matcher(crtUnit);
			if (ma2.find()) {
				String nr = Roman.normalizeRomanNumbers(ma2.group(1));
				crtUnit = nr + ma2.group(2);
			}
			unit = unit + " " + crtUnit;
			legal = legal.replace(ma.group(0), "@");
		}
		p = Pattern.compile("\\bU[-\\s](\\d+[A-Z]?|[A-Z]\\d*)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replace(ma.group(0), "@");
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
		}

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		p = Pattern.compile("\\bLO?TS?\\s?(\\d+-?[A-Z]|[\\d\\s-]+(?<!\\s)[A-Z]?(?!/))\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
		}
		p = Pattern.compile("\\bLO?TS? ([A-Z]\\d*(?:[-\\s]\\d+)?(?: [A-Z])?)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
		}
		p = Pattern.compile("\\bL[-\\s](\\d+[A-Z]?(?: \\d+[A-Z]?)*)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}

		// extract block from legal description
		String block = ""; // can have multiple occurrences
		p = Pattern.compile("\\bBL(?:OC)?KS?[-\\s](\\d+(?:-?[A-Z])?(?:\\s\\d+)*|[A-Z]{1,2}(?:-?\\d+)?)\\b");
		ma = p.matcher(origLegal);
		while (ma.find()) {
			block = block + " " + ma.group(1);
		}
		p = Pattern.compile("\\bB-([A-Z]|\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1);
		}
		block = block.trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}

		// extract phase from legal description
		String phase = "";
		p = Pattern.compile("\\bPH(?:A?SE)?S?[-\\s]*((?:\\d+|\\bI)(?:-?[A-Z])?(?: (?:\\d+|\\bI)(?:-?[A-Z])?)*|\\b[A-Z]{1,2}(?:-\\d+)?)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			phase = phase + " " + ma.group(1).trim().replaceAll("\\bI\\b", "1");
		}
		phase = phase.trim();
		if (phase.length() != 0) {
			phase = LegalDescription.cleanValues(phase, false, true);
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
		}
	}

	public static void stdPisFLBrowardTR(ResultMap m, long searchId) throws Exception {
		String s = (String) m.get("PropertyIdentificationSet.OwnerLastName");
		s = s.replaceFirst("\\s+\\d+/\\d+ INT\\b.*", "");
		s = s.replaceFirst("\\s*&$", "");

		String[] a = StringFormats.parseNameNashville(s);
		m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
		m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
		m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
		m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
		m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
		m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
	}

	public static void stdPisFLBrowardTR2(ResultMap m, long searchId) throws Exception {
		// FLBrowardTR.stdPisFLBrowardTR2(m, searchId);
	}

	public static void legalFLBrowardTR(ResultMap m, long searchId) throws Exception {

	}

	public static void taxFLBrowardTR(ResultMap m, long searchId) throws Exception {
		// FLBrowardTR.taxFLBrowardTR(m, searchId);
	}

	public static void fixGrantorGranteeSetDT(ResultMap m, long searchId) throws Exception {
		CAGenericDT.fixGrantorGranteeSetDT(m, searchId);
	}

	public static void setGrantorGranteeDT(ResultMap m, long searchId) throws Exception {

		ResultTable grantorSet = (ResultTable) m.get("GrantorSet");
		if (grantorSet != null) {
			String body[][] = grantorSet.body;
			int len = body.length;
			StringBuilder grantor = new StringBuilder();
			if (len != 0) {
				for (int i = 0; i < len; i++) {
					if ("-".equals(body[i][0]))
						grantor.append(" / ").append(body[i][1]).append(" ").append(body[i][2]).append(" ").append(body[i][3]);
					else
						grantor.append(" / ").append(body[i][0].replaceFirst("\\s*&\\s*$", ""));
				}
			}

			String grantorStr = grantor.toString();
			grantorStr = grantorStr.replaceFirst("^ / ", "");
			if (grantorStr.length() > 0) {
				m.put("SaleDataSet.Grantor", grantorStr);
			}
		}

		ResultTable granteeSet = (ResultTable) m.get("GranteeSet");
		if (granteeSet != null) {
			String body[][] = granteeSet.body;
			int len = body.length;
			StringBuilder grantee = new StringBuilder();
			if (len != 0) {
				for (int i = 0; i < len; i++) {
					if ("-".equals(body[i][0]))
						grantee.append(" / ").append(body[i][1]).append(" ").append(body[i][2]).append(" ").append(body[i][3]);
					else
						grantee.append(" / ").append(body[i][0].replaceFirst("\\s*&\\s*$", ""));
				}
			}

			String granteeStr = grantee.toString();
			granteeStr = granteeStr.replaceFirst("^ / ", "");
			if (granteeStr.length() > 0) {
				m.put("SaleDataSet.Grantee", granteeStr);
			}
		}
	}

	public static void setGrantorFromCommentDT(ResultMap m, long searchId) throws Exception {

		String comment = (String) m.get("tmpCommentForGrantor");
		if (comment == null || comment.length() == 0)
			return;

		// the comment will be set as grantor for mortgage and transfer
		// documents that don't have grantor&grantee already set
		ResultTable grantor = (ResultTable) m.get("GrantorSet");
		ResultTable grantee = (ResultTable) m.get("GranteeSet");
		// String docType = (String) m.get("SaleDataSet.DocumentType");

		boolean grantorSetFromComment = false;
		if ((grantor == null || grantor.getLength() == 0) && (grantee == null || grantee.getLength() == 0)) {

			comment = comment.replaceAll("[,]?[^ ,]*[0-9][^ ,]*[,]?", " ").replaceAll("[ ][ ]+", " ");
			comment = " " + comment + " ";
			comment = comment.replaceAll("([ \t]*[&][ \t]*)((SON)|(WIFE)|(ETUX)|(CO)|(TRS))", "&$3");
			comment = comment.replaceFirst("^[\\d\\s]+$", "");
			if (comment.length() != 0) {
				String comp[] = NameUtils.getCompanyExpressions();
				int poz = 0;
				int split = 0;
				int size = comment.length();
				String granteeStr = "";

				int virgPoz = comment.indexOf(",");

				if (virgPoz > 0) {
					String part1 = comment.substring(0, virgPoz);
					String part2 = comment.substring(virgPoz + 1, size);
					double d = GenericNameFilter.calculateMatchForLast(part1.trim(), part2.trim(), 0.90d, null);
					if (d > 0.90) {
						comment = part1;
						size = comment.length();
					}
				}

				Matcher mat = NameUtils.patLastInitialLastInitial.matcher(comment); // Name
																					// [Name]
																					// Initial
																					// Name
																					// Initial
																					// [Initial
																					// Initial
																					// ...]

				if (mat.find()) {
					if (mat.start() < 3 && mat.end() > size - 4) {
						poz = mat.start(2);
						split = mat.end(2);
						if (split < size - 4 && poz > 3) {
							granteeStr = comment.substring(split, size);
							comment = comment.substring(0, split - 1);
						}
					}
				}

				if (granteeStr.length() < 1) { // company names rules
					// try with company names
					for (int j = 0; j < comp.length; j++) {
						String temp1 = " " + comp[j] + " ";
						if (((poz = comment.toLowerCase().indexOf(temp1)) > 0) || ((poz = comment.toUpperCase().indexOf(temp1)) > 0)) {
							split = poz + comp[j].length() + 1;
							if (split < size - 6 && split > 5) {
								try {
									granteeStr = comment.substring(split, size);
								} catch (Exception e) {
									e.printStackTrace();
								}
								String temp = granteeStr.replaceAll("[ \t]", "");
								boolean testBreak = NameUtils.isInCompanyList(temp);
								if (testBreak) {
									granteeStr = "";
								} else {
									comment = comment.substring(0, split);
									j = comp.length;
								}
							}
						}
					}
				}

				/*
				 * Matcher mat =
				 * NameUtils.firstAndMiddleInitialsWithSeparator.matcher
				 * (comment);
				 * 
				 * if(mat.find()){ poz = mat.start(); split = mat.end(); if(
				 * split<size-3 && poz > 3){ granteeStr =
				 * comment.substring(split,size); comment =
				 * comment.substring(0,split-1).replaceAll(",", ""); } }
				 */

				if (granteeStr.length() < 1) {
					mat = NameUtils.firstAndMiddleInitials.matcher(comment);

					if (mat.find()) { // two initial rule
						poz = mat.start();
						split = mat.end();
						if (split < size - 5 && poz > 4) {
							granteeStr = comment.substring(split, size);
							comment = comment.substring(0, split - 1);
						} else if (split >= size - 3) { // two initial at end
							Matcher matToken = NameUtils.posiblleNameTokenWithFirstAndMiddleInitials.matcher(comment);
							if (matToken.find()) {
								split = matToken.start();
								if (split > 4 && split < size - 5) {
									granteeStr = comment.substring(split, size);
									comment = comment.substring(0, split - 1);
								}
							}
						} else if (poz <= 3) { // two initial at start
							Matcher matToken = NameUtils.posiblleNameToken.matcher(comment);
							if (matToken.find()) {
								poz = matToken.start();
								split = matToken.end();
								if (split < size - 5 && poz > 4) {
									granteeStr = comment.substring(split, size);
									String temp = granteeStr.replaceAll("[ \t]", "");
									boolean testBreak = NameUtils.isInCompanyList(temp);
									if (testBreak) {
										granteeStr = "";
									} else {
										comment = comment.substring(0, split);
									}
								}
							}
						}
					}
				}

				m.put("SaleDataSet.Grantor", comment);
				m.put("SaleDataSet.Grantee", granteeStr);
				grantorSetFromComment = true;
			}
		}

		if (!grantorSetFromComment) {
			m.remove("tmpCommentForGrantor");
		}
	}

	protected static void fixLotBlockDASLDT(ResultMap m, long searchId) throws Exception {

		String lot = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
		String blk = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
		String unit = (String) m.get("PropertyIdentificationSet.SubdivisionUnit");
		if (lot == null) {
			lot = "";
		}
		if (blk == null) {
			blk = "";
		}
		if (unit == null) {
			unit = "";
		}
		if (lot.matches("W\\d+[OEW]?") && blk.matches("U[\\dA-Z-]+")) { // e.g.
																		// b&p
																		// 8345_3865
																		// on FL
																		// Orange
																		// DT
																		// (bug
																		// #2533)
			blk = blk.substring(1);
			unit = unit + " " + blk;
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit.trim());
			m.remove("PropertyIdentificationSet.SubdivisionLotNumber");
			m.remove("PropertyIdentificationSet.SubdivisionBlock");
		} else if (lot.matches("U\\d+")) {
			if (blk.matches("X[A-Z]+")) { // e.g. BP 1711_2482, 3074_958, FL
											// Osceola DT (see bug #2585)
				String lotStr = lot.substring(1);
				lotStr = lotStr.length() > 1 ? lotStr : "0" + lotStr;
				unit = unit + " " + blk.substring(1) + "-" + lotStr;
				m.remove("PropertyIdentificationSet.SubdivisionBlock");
			} else { // e.g. BP 2023_1911, FL Osceola DT (see bug #2585)
				unit = unit + " " + lot.substring(1);
			}
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit.trim());
			m.remove("PropertyIdentificationSet.SubdivisionLotNumber");
		} else {
			if (lot.matches("L(\\d+|[A-Z])")) {
				lot = lot.substring(1);
				m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			}
			if (blk.matches("B([A-Z]|\\d).*")) {
				blk = blk.substring(1);
				m.put("PropertyIdentificationSet.SubdivisionBlock", blk);
			}
		}
	}

	protected static void fixTownshipDASLDT(ResultMap m, long searchId) throws Exception {

		String twn = (String) m.get("tmpSubdivisionTownship");
		if (!StringUtils.isEmpty(twn)) {
			if (NameUtils.isCompany(twn)) {
				m.put("tmpSubdivisionTownship", "");
				twn = (String) m.get("PropertyIdentificationSet.SubdivisionTownship");
				if (!StringUtils.isEmpty(twn)) {
					m.put("PropertyIdentificationSet.SubdivisionTownship", "");
				}
			}
		}
	}

	public static void fixLegalFieldsFLOrangeDASLDT(ResultMap m, long searchId) throws Exception {

		fixLotBlockDASLDT(m, searchId);
		fixTownshipDASLDT(m, searchId);
	}

	public static void fixLegalFieldsFLSeminoleDASLDT(ResultMap m, long searchId) throws Exception {

		fixLotBlockDASLDT(m, searchId);
	}

	public static void fixLegalFieldsFLPinellasDASLDT(ResultMap m, long searchId) throws Exception {

		// fix township
		String twn = (String) m.get("tmpSubdivisionTownship"); // can contain
																// "PTLT"
																// (13828_343),
																// "LESS W55FT LT11"
																// (9982_1924),
																// "PTLT,XF103116511560010110"
																// (13973_667)
																// etc.
		if (!StringUtils.isEmpty(twn)) {
			if (!twn.matches("\\d+[SWEN]?")) {
				m.put("tmpSubdivisionTownship", "");
				twn = (String) m.get("PropertyIdentificationSet.SubdivisionTownship");
				if (!StringUtils.isEmpty(twn)) {
					m.put("PropertyIdentificationSet.SubdivisionTownship", "");
				}
			}
		}
	}

	public static void fixLegalFieldsFLOsceolaDASLDT(ResultMap m, long searchId) throws Exception {

		fixLotBlockDASLDT(m, searchId);
		fixTownshipDASLDT(m, searchId);
	}

	public static void fixLegalFieldsDASLDT(ResultMap m, long searchId) throws Exception {

		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();

		if ("FL".equals(crtState)) {
			if ("Orange".equals(crtCounty)) {
				fixLegalFieldsFLOrangeDASLDT(m, searchId);
			} else if ("Seminole".equals(crtCounty)) {
				fixLegalFieldsFLSeminoleDASLDT(m, searchId);
			} else if ("Pinellas".equals(crtCounty)) {
				fixLegalFieldsFLPinellasDASLDT(m, searchId);
			} else if ("Osceola".equals(crtCounty)) {
				fixLegalFieldsFLOsceolaDASLDT(m, searchId);
			}
		}
	}

	public static void legalRemarksFLOrangeRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		FLOrangeTR.legalTokenizerRemaksFLOrangeRV(m, legal);
		// ro.cst.tsearch.test.TestParser.append("C:/OrangeRVLegal_raw.txt",
		// legal);
	}

	public static void legalTokenizerRemarksFLPinellasRV(ResultMap m, String legal) throws Exception {

		// initial corrections and cleanup of legal description
		legal = replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
																					// to
																					// arabics

		legal = legal.replaceAll("\\*", "");
		legal = legal.replaceAll("\\bETC\\b", "");
		legal = legal.replaceAll("\\bDESC\\b\\.?", "");
		legal = legal.replaceAll("\\bPT\\b", "");
		legal = legal.replaceAll("\\bCORR?( BLK)?\\b", "");
		legal = legal.replaceAll("\\bCOMM( [SWEN]{1,2})?\\b", "");
		legal = legal.replaceAll("\\b(RE-)?REV(ISED)?( (MAP|PLAT))?\\b", "");
		legal = legal.replaceAll("\\b(AMD|AMEND)\\b", "");
		legal = legal.replaceAll("\\b(TERM|TERMINATION)\\b", "");
		legal = legal.replaceAll("(\\bLESS )?\\b[SWEN]\\s*\\d+(\\.\\d+|/\\d+| \\d+/\\d+)?\\s*FT\\b", "");
		legal = legal.replaceAll("\\b[SWEN]{1,2}/\\d+\\b", "");
		legal = legal.replaceAll("\\bALL(?= \\d+)", "");
		legal = legal.replaceAll("\\bANX \\d+\\b", "");
		legal = legal.replaceAll("\\bNO\\b", "");
		legal = legal.replaceAll("\\bPAR(CEL)? \\d+\\b", "PARCEL");
		legal = legal.replaceAll("\\bREP(LAT)?\\b", "PLAT");
		legal = legal.replaceAll("\\b\\d+(ST|ND|RD|TH)? (ADD(ITION)?|EDITION)\\b", "ADD");
		legal = legal.replaceAll("\\bWK\\s+\\d+([\\s,-]+\\d+)*\\b", "");
		legal = legal.replaceAll(" (\\d+) SEC\\b(?! \\d+(?:\\s|$))", " SEC $1");

		legal = legal.trim();
		legal = legal.replaceAll("\\s{2,}", " ");

		// extract and remove ParcelID from legal description
		Pattern p = Pattern.compile("\\b(\\d+[/-]\\d+[/-]\\d+[/-]\\d+[/-]\\d+[/-]\\d+)\\b");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			String pid = ma.group(1);
			p = Pattern.compile("([^/-]+)[/-]([^/-]+)[/-]([^/-]+)[/-][^/-]+[/-][^/-]+[/-][^/-]+");
			ma = p.matcher(pid);
			if (ma.find()) {
				m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(1).replaceFirst("^0+(.+)", "$1"));
				m.put("PropertyIdentificationSet.SubdivisionTownship", ma.group(2).replaceFirst("^0+(.+)", "$1"));
				m.put("PropertyIdentificationSet.SubdivisionRange", ma.group(3).replaceFirst("^0+(.+)", "$1"));
			}
			pid = pid.replaceAll("[/-]", "");
			m.put("PropertyIdentificationSet.ParcelID", pid);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and remove plat b&p from legal description
		p = Pattern.compile("\\bPL\\s*(\\d+)/(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
			m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and remove cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("\\b(?:(?:OR|BK) )?(?<!/)(\\d+)/(\\d+)(?=\\s|$)");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add("");
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		saveCRInMap(m, bodyCR);
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract and replace section, township and range from legal
		// description
		List<List> body = getSTRFromMap(m); // first add sec-twn-rng extracted
											// from XML specific tags, if any
											// (for DT use)
		p = Pattern.compile("\\bSEC (\\d+|[A-Z])(?=(?:\\s|$))");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add("");
			line.add("");
			body.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
		p = Pattern.compile("\\b(\\d+)-(\\d+)-(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add(ma.group(3));
			body.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
		saveSTRInMap(m, body);

		// additional cleanup
		legal = legal.replaceAll("\\bCASE#? \\d+[A-Z]*\\b", "");
		legal = legal.replaceAll("\\bSEE IMAGE( FOR)?\\b", "");
		legal = legal.replaceAll("\\bCASE NUMBER\\b", "");
		legal = legal.replaceAll("^\\d+[A-Z]*$", "");
		legal = legal.replaceAll("^\\d+[A-Z]{2,}\\b\\s*", "");
		legal = legal.replaceAll("\\b\\d+(-\\d+[A-Z]*)?\\s*((CT|M|SM) )?(CIV?|CR(IM)?|SP|CL|CO|PR)\\b", "");
		legal = legal.replaceAll("\\b\\d+(-\\d+)?-\\d+[A-Z]*(\\s|$)", "");
		legal = legal.replaceAll("\\bC[RT]C\\s*\\d+[\\s-]\\d+\\s*[A-Z]+[\\s-][A-Z]\\b", "");
		legal = legal.trim().replaceAll("\\s{2,}", " ");
		if (legal.length() == 0)
			return;

		// extract and replace unit from legal description
		String unit = "";
		p = Pattern.compile("\\b(?:(?:[SWEN] )?UN(?:IT)?|APT)[\\s-](\\d+(?:[-\\s]?[A-Z])?|[A-Z](?:-?\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1).replaceAll(" ", "@").replaceFirst("^0+(.+)", "$1");
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			String unit2 = (String) m.get("PropertyIdentificationSet.SubdivisionUnit");
			if (!StringUtils.isEmpty(unit2)) {
				unit2 = unit2 + " " + unit;
			} else {
				unit2 = unit;
			}
			unit2 = LegalDescription.cleanValues(unit2, false, true);
			unit2 = unit2.replaceAll("@", " ");
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace building # from legal description
		String bldg = "";
		p = Pattern.compile("\\bBLDG? (\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLDG ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace block from legal description
		String block = "";
		p = Pattern.compile("\\bBLK (\\d+|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLK ");
		}
		block = block.trim();
		if (block.length() != 0) {
			String block2 = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(block2)) {
				block2 = block2 + " " + block;
			} else {
				block2 = block;
			}
			block2 = LegalDescription.cleanValues(block2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace tract from legal description
		String tract = "";
		p = Pattern.compile("\\bTR (\\d+|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "TRACT ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace phase from legal description
		String phase = "";
		p = Pattern.compile("\\bPH(?:ASE)? (\\d+[A-Z]?|I)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = ma.group(1);
			if ("I".equals(phase))
				phase = "1";
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PHASE ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace lot from legal description
		String lot = "";
		String patt = "\\d{1,4}[A-Z]?"; // limit to 4 digits for legals like
										// 67209 CH (bk=4565, pg=1679)
		String headerPatt = "^(?:\\b(?:LOT|BLK|UNIT|BLDG|TRACT|PLAT|PARCEL|PHASE|LAND)\\b\\s*)";
		p = Pattern.compile("\\bLT (" + patt + "(?: " + patt + ")*)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			lot = ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "LOT ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		} else {
			p = Pattern.compile("^(" + patt + "(?: " + patt + ")*)(?= [A-Z])");
			boolean foundLot = true;
			String legalTmp = legal;
			// iteration for legals like: 1 2 3 BLK 106 1 2 3 14 15 16 BLK 93
			// SUTHERLAND with Lot = 1-3 14-16 and Block = 93 106 (bk=3901
			// pg=951)
			while (foundLot) {
				ma = p.matcher(legalTmp);
				if (ma.find()) {
					lot = lot + " " + ma.group(1);
					legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "LOT ");
					legal = legal.trim().replaceAll("\\s{2,}", " ");
					legalTmp = legalTmp.replaceFirst("\\b" + ma.group(0) + "\\b", "LOT ");
					legalTmp = legal.trim().replaceAll("\\s{2,}", " ");
					legalTmp = legalTmp.replaceFirst(headerPatt + "+", "");
				} else {
					foundLot = false;
				}
			}
		}
		String lot2 = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
		if (!StringUtils.isEmpty(lot2)) {
			lot2 = lot2 + " " + lot;
		} else {
			lot2 = lot;
		}
		if (lot2.length() != 0) {
			lot2 = LegalDescription.cleanValues(lot2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot2);
		}

		// extract subdivision name - only if lot or block or unit or tract or
		// phase was extracted
		// or legal contains CO?NDO or SUB or PARCEL; otherwise the text might
		// be a doc type
		// additional cleaning before extracting subdivision name
		String subdiv = "";
		String endPatt = "\\b(?:TRACT|PLAT|UNIT|PHASE|SEC)\\b";
		p = Pattern.compile("(.*?)(?:" + endPatt + "\\s*)*\\s*\\b(?:SUB|CO?NDO(?:MINIUM)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
			subdiv = subdiv.replaceFirst(headerPatt + "+", "");
		} else if (lot.length() != 0 || block.length() != 0 || unit.length() != 0 || phase.length() != 0 || tract.length() != 0
				|| legal.matches(".*\\bPARCEL\\b.*")) {
			p = Pattern.compile(headerPatt + "*(.*?)\\s*(?:-?" + endPatt + "|$)");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		subdiv = subdiv.trim();
		if (subdiv.length() != 0) {
			subdiv = subdiv.replaceFirst("^ADD(ITION)?\\b", "");
			subdiv = subdiv.replaceFirst("\\b(ADD(ITION)?|EDITION)\\b.*", "");
			subdiv = subdiv.replaceFirst("\\s+\\d+(ST|ND|RD|TH)?\\s*$", "");
			subdiv = subdiv.replaceFirst("\\s*-\\s*$", "");
			subdiv = subdiv.replaceFirst("^\\s*OF\\b\\s*", "");
			subdiv = subdiv.trim();
		}
		if (subdiv.length() != 0) {
			m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			if (legal.matches(".*\\bCO?NDO(MINIUM)?\\b.*")) {
				m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			}
		}
	}

	public static void legalRemarksFLPinellasRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerRemarksFLPinellasRV(m, legal);
		// String book = (String) m.get("SaleDataSet.Book");
		// if (book == null) book = "";
		// String page = (String) m.get("SaleDataSet.Page");
		// if (page == null) page = "";
		// ro.cst.tsearch.test.TestParser.append("C:/PinellasRVLegal_raw.txt",
		// legal + " ::: " + book + " " + page);
	}

	public static void legalTokenizerRemarksFLLeeRV(ResultMap m, String legal) throws Exception {

		// initial corrections and cleanup of legal description
		legal = legal.replaceAll("(\\w)(L\\d+([-,]\\d+)*,B\\d+)\\b", "$1,$2");
		legal = legal.replaceAll("\\b(L\\d+)(B(\\d+|[A-Z]))\\b", "$1,$2");
		legal = legal.replaceAll("\\b(\\d+)(PT)\\b", "$1 $2");
		legal = legal.replaceAll("\\b(P(?:AR)?T|PAR)(\\d+)\\b", "$1 $2");
		legal = legal.replaceAll("\\b(\\d+)(ETC)\\b", "$1 $2");
		legal = legal.replaceAll("#\\d+-\\d+,", " ");
		legal = legal.replaceAll("\\bCO \\w*( BK)?\\s?\\d+/\\d+\\b", "");
		legal = legal.replaceAll("\\b[SWEN]{1,2}\\s?\\d+/\\d+", "");
		legal = legal.replaceAll("\\bRESOLUTION NO \\d+-\\d+-\\d+\\b", "");
		legal = legal.replaceAll("\\bP(AR)?T\\b", "");
		legal = legal.replaceAll("#", " ");
		legal = legal.replaceAll("\\bUS (DIST|LIFE)[A-Z]*\\b", "");
		legal = legal.replaceAll("\\bPAR((CE)?L)?\\s?\\d+(,\\d+)*( IN)?\\b", "");
		legal = legal.replaceAll("\\bUNITS(?! ([A-Z]|\\d+)\\b)", "");
		legal = legal.replaceAll("\\b[SWEN]{1,2}\\d+'", "");

		legal = replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
																					// to
																					// arabics
		legal = legal.replaceAll("\\s{2,}", " ").trim();

		// extract and remove ParcelID from legal description
		String pid = "";
		Pattern p = Pattern.compile("(\\d+)-(\\d+)-(\\d+)-[\\dA-Z\\.-]+(?: \\d+[A-Z]?)?");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			pid = ma.group(0).replaceAll("[\\.\\s-]", "");
			m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(1).replaceFirst("^0+(.+)", "$1"));
			m.put("PropertyIdentificationSet.SubdivisionTownship", ma.group(2).replaceFirst("^0+(.+)", "$1"));
			m.put("PropertyIdentificationSet.SubdivisionRange", ma.group(3).replaceFirst("^0+(.+)", "$1"));

		} else {
			p = Pattern.compile("\\bST(?:RAP)?\\s?(\\d{6}[\\dA-Z]{9,})\\b");
			ma.usePattern(p);
			ma.reset();
			if (ma.find()) {
				pid = ma.group(1);
			}
		}
		if (pid.length() != 0) {
			pid = pid.replaceAll("[-,\\s]", "");
			m.put("PropertyIdentificationSet.ParcelID", pid);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace lot from legal description
		String lot = "";
		p = Pattern.compile("(?<!APT )\\b(?:PTG?|GOV|P?G|U)?L\\s?(\\d+(?:-?[A-Z])?(?:\\s?[-,&]\\s?\\d+(?!-\\d+[SWEN]?-\\d+)(?:-?[A-Z])?)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "LOT ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
		p = Pattern.compile("(?<=(?:,\\s?|^))(?:P?L|G)\\s?([A-Z](?:-?\\d+)?|\\d+(?:,\\d+)*)(?=,)");
		ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(1).replace(',', ' ');
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "LOT ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			lot = lot.replaceAll("\\s?[,&]\\s?", " ");
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}

		// extract and replace block from legal description
		String block = "";
		p = Pattern.compile("(?<!APT )\\bB\\s?(\\d+(?:-[A-Z])?(?:,\\d+)?)\\b(?!/)");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLK ");
		}
		p = Pattern.compile("(?<=,\\s?)P?B\\s?([A-Z]{1,2}|\\d+)(?=,|$)");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLK ");
		}
		p = Pattern.compile("\\bBLK (\\d+|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLK ");
		}
		block = block.trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace unit from legal description
		String unit = "";
		p = Pattern.compile("(?<!APT )\\bU[\\s-]?(\\d+(?:-?[A-Z])?(?:[,-]\\s?\\d+(?!-))*)\\b(?=,|$)");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		p = Pattern.compile("(?<!APT )\\bU[\\s-]?(\\d+(?:-?[A-Z])?(?:-\\d+(?!-))*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		p = Pattern.compile("(?<!APT )\\bU[\\s-]?(\\d+(?:-?[A-Z])?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		p = Pattern.compile("(?<=,\\s?|^|FAM )U\\s?([A-Z](?:-?\\d+)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		p = Pattern.compile("\\bU ([A-Z]{1,2}\\d+-\\d+[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		p = Pattern.compile("^,?APT\\s?(\\d+(?:-?[A-Z])?|[A-Z]-?\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		p = Pattern.compile("\\bUNIT\\s?(\\d+|[A-Z](?:-?\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace building # from legal description
		String bldg = "";
		p = Pattern.compile("\\bBLDG?\\s?(\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLDG ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace phase from legal description
		String phase = "";
		p = Pattern.compile("\\bPH(?:ASE)?[\\s-]?((?:(?:(?:\\d|[IVX])+(?:-?[A-Z])?)\\b,?)+)");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = ma.group(1);
		} else {
			p = Pattern.compile("(?<!APT )\\bP((?:(?:(?:\\d|[IVX])+(?:-?[A-Z])?)\\b,?)+(?!-))");
			ma = p.matcher(legal);
			if (ma.find() && !ma.group(0).equals("PIN")) {
				phase = ma.group(1);
			}
		}
		if (phase.length() != 0) {
			phase = phase.replace(',', ' ');
			Pattern p2 = Pattern.compile("\\b([IVX]+)([A-Z]?)");
			Matcher ma2 = p2.matcher(phase);
			if (ma2.find()) {
				String nr = Roman.normalizeRomanNumbers(ma2.group(1));
				phase = nr + ma2.group(2);
			}
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PHASE ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace tract from legal description
		String tract = "";
		p = Pattern.compile("\\bTR(?:A(?:CT)?\\b)?\\s?([A-Z]|\\d+(?:,\\d+(?!-\\d+[SWEN]?-\\d+))*)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = ma.group(1).replace(',', ' ');
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "TRACT ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace section, township and range from legal
		// description
		List<List> body = getSTRFromMap(m); // first add sec-twn-rng extracted
											// from XML specific tags, if any
											// (for DT use)
		p = Pattern
				.compile("\\b(?:(?:PART?|PT)\\s?)?(?:S(?:E(?:C(?:T(?:ION)?)?)?)?)?(?<![-/]|RSL\\s?)(\\d+(?:,\\d+)*)[,-]T?(\\d+(?:[SWEN]|\\s?(?:SOUTH|EAST|WEsT|NORTH))?)-R?(\\d+(?:[SWEN]|\\s?(?:SOUTH|EAST|WEsT|NORTH))?)\\b(?!\\.)");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replace(',', ' '));
			line.add(ma.group(2).replaceFirst("(\\d+)\\s?([SWEN])[A-Z]+", "$1$2"));
			line.add(ma.group(3).replaceFirst("(\\d+)\\s?([SWEN])[A-Z]+", "$1$2"));
			body.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
		p = Pattern.compile("\\b(?:PAR|PT)?(?:S(?:E(?:C(?:T(?:ION)?)?)?)?)?(\\d+(?:,\\d+)*),(\\d+[SWEN]?),(\\d+[SWEN]?)\\b(?!\\.)");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replace(',', ' '));
			line.add(ma.group(2));
			line.add(ma.group(3));
			body.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
		p = Pattern.compile("\\bSECT? (\\d+) TWN (\\d+(?:[SWEN]|\\s?(?:SOUTH|NORTH|EAST|WEST))?) RNG (\\d+(?:[SWEN]|\\s?(?:SOUTH|NORTH|EAST|WEST))?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replace(',', ' '));
			line.add(ma.group(2).replaceFirst("(\\d+)\\s?([SWEN])[A-Z]+", "$1$2"));
			line.add(ma.group(3).replaceFirst("(\\d+)\\s?([SWEN])[A-Z]+", "$1$2"));
			body.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
		p = Pattern.compile("(?<!APT )\\bS(\\d+)(?:,(\\d+[SWEN]?),(\\d+[SWEN]?))?\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			String twn = ma.group(2);
			if (twn == null)
				twn = "";
			line.add(twn);
			String rng = ma.group(3);
			if (rng == null)
				rng = "";
			line.add(rng);
			body.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
		p = Pattern.compile("\\bSEC(?:T(?:ION)?)?\\s?(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add("");
			line.add("");
			body.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
		saveSTRInMap(m, body);

		// extract and remove plat b&p from legal description
		p = Pattern.compile("\\bPL(?:A?T)?(?: BK )?(\\d+)/(\\d+(?:-\\d+)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
			m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and remove cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("\\b(\\d{6,})[ ,]+OR\\s?(\\d+)[/\\-](\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2).replaceFirst("^0", ""));
			line.add(ma.group(3).replaceFirst("^0", ""));
			line.add(ma.group(1));
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		p = Pattern.compile("\\bOR\\s?(\\d+)[/\\\\-](\\d+(?:[-,]\\d{1,5})?)\\b(?![/-])(?:,\\s?(?:INST )?(\\d{6,})\\b)?");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0", ""));
			String page = ma.group(2).replaceFirst("^0", "");
			page = page.replaceFirst("^(\\d+)(\\d[,-])(\\d)$", "$1$2$1$3"); // modify
																			// 120-3
																			// into
																			// 120-123
			page = page.replaceFirst("^(\\d+)(\\d{2}[,-])(\\d{2})$", "$1$2$1$3"); // modify
																					// 120-31
																					// into
																					// 129-131
			line.add(page);
			String instr = ma.group(3);
			if (instr == null)
				instr = "";
			line.add(instr);
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		p = Pattern.compile("\\b(?:OR|CORR?|REREC)?(\\d+)/(\\d+(?:[-,]\\d{1,5})?)\\b(?!/)(?:,\\s?(?:INST )?(\\d{6,})\\b)?");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0", ""));
			String page = ma.group(2).replaceFirst("\\b0", "");
			page = page.replaceFirst("^(\\d+)(\\d[,-])(\\d)$", "$1$2$1$3");
			page = page.replaceFirst("^(\\d+)(\\d{2}[,-])(\\d{2})$", "$1$2$1$3");
			line.add(page);
			String instr = ma.group(3);
			if (instr == null)
				instr = "";
			line.add(instr);
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		p = Pattern.compile("^(\\d{6,})(?=,)");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(1));
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		p = Pattern.compile("\\bINST (\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(1));
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		p = Pattern.compile("^[A-Z]+,\\s?(?:FILE\\s?)?(\\d+)$");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(1));
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		saveCRInMap(m, bodyCR);
		legal = legal.trim().replaceAll("\\s{2,}", " ");
	}

	public static void legalRemarksFLLeeRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerRemarksFLLeeRV(m, legal);
	}

	public static void legalTokenizerRemarksFLFranklinRV(ResultMap m, String legal) throws Exception {

		// initial corrections and cleanup of legal description
		legal = legal.replaceAll("\\b(\\w+)(UN \\d+)\\b", "$1 $2");
		legal = legal.replaceAll("\\b[SWEN]{1,2} \\d+/\\d+( INT)?\\b", "");
		legal = legal.replaceAll("\\b\\d+/\\d+ INT\\b", "");
		legal = legal.replaceAll("\\b[SWEN]{1,2} \\d+'", "");
		legal = legal.replaceAll("#", "");
		legal = legal.replaceAll("\\bAND\\b", "&");
		legal = legal.replaceAll("\\bEAST\\b", "E");
		legal = legal.replaceAll("\\bWEST\\b", "W");
		legal = legal.replaceAll("\\bNORTH\\b", "N");
		legal = legal.replaceAll("\\bSOUTH\\b", "S");

		legal = replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = legal.replace("-", "__");
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
																					// to
																					// arabics
		legal = legal.replace("__", "-");

		legal = legal.replaceAll("\\sTHRU\\s", "-");
		legal = legal.replaceAll("\\s{2,}", " ").trim();

		// extract and remove cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		Pattern p = Pattern.compile("^(\\d{9,})$");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		p = Pattern.compile("\\b(?:BK|BOOK|OR) (\\d+) (?:PG|PAGE) (\\d+(?:-\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(2).replaceAll("\\b0+(\\d+)", "$1"));
			line.add("");
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		p = Pattern.compile("\\bOR/((?:\\s?\\b\\d+/\\d+)*)\\b");
		ma = p.matcher(legal);
		Pattern p2 = Pattern.compile("\\b(\\d+)/(\\d+)\\b");
		Matcher ma2;
		while (ma.find()) {
			ma2 = p2.matcher(ma.group(1));
			while (ma2.find()) {
				List<String> line = new ArrayList<String>();
				line.add(ma2.group(1).replaceFirst("^0+(\\d+)", "$1"));
				line.add(ma2.group(2).replaceAll("\\b0+(\\d+)", "$1"));
				line.add("");
				line.add("");
				bodyCR.add(line);
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		p = Pattern.compile("\\bB-?(\\d+) P-?(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(2).replaceFirst("^0+(\\d+)", "$1"));
			line.add("");
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		saveCRInMap(m, bodyCR);
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		legal = legal.replaceAll("\\b\\d+/\\d+\\b", "");

		// extract and replace block from legal description
		String block = "";
		String lot = "";
		String unit = "";
		p = Pattern.compile("^B([A-Z]) L(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			block = block + " " + ma.group(1);
			lot = lot + " " + ma.group(2);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "LOT ");
		}
		p = Pattern.compile("\\bB([A-Z]) U(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1);
			unit = unit + " " + ma.group(2);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "LOT ");
		}
		while (ma.find()) {
			block = block + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLOCK ");
		}
		p = Pattern.compile("\\b(?:BLOCK|BLK?|BK) ((?:\\d+(?:[A-Z]| [SWEN])?|[A-Z](?:-?\\d+)?)\\b(?:\\s?\\(\\d+\\))?)");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1);
			legal = legal.replace(ma.group(0), "BLOCK ");
		}
		if (block.length() == 0) {
			p = Pattern.compile("\\bB(\\d+[A-Z]?|[A-Z](?:-\\d+)?)\\b");
			ma = p.matcher(legal);
			while (ma.find()) {
				block = block + " " + ma.group(1);
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLOCK ");
			}
		}
		block = block.trim();
		if (block.length() != 0) {
			block = block.replaceAll("[\\(\\)]", " ");
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace lot from legal description
		p = Pattern.compile("\\b(?:(?<!(?:BLK?|BK) )L\\s?|S?LO?TS?\\s?|LTO |LO )(\\d+(?:-?[A-Z])?(?:\\s?[&,\\s-]\\s?\\d+(?:-?[A-Z])?)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(1).replaceAll("\\s?-\\s?", "-");
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "LOT ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			lot = lot.replaceAll("\\s?[,&]\\s?", " ");
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace unit from legal description
		p = Pattern.compile("\\b(?:UN(?:IT?)?|APT)\\s?(\\d+(?:-?[A-Z])?|\\b[A-Z](?:-?\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		p = Pattern.compile("\\bU[-\\s]?(\\d+[A-Z]?(?:\\s?&\\s?\\d+)*|[A-Z](?:-?\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1).replaceAll("\\s?&\\s?", " ");
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		p = Pattern.compile("^\\s?CONDO(?:MINIUM)? ([A-Z]-\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			unit = unit.replaceAll("\\bI\\b", "1");
			unit = LegalDescription.cleanValues(unit, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace building # from legal description
		String bldg = "";
		p = Pattern.compile("\\bBLDG?\\s?(\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLDG ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace phase from legal description
		String phase = "";
		p = Pattern.compile("\\bPH(?:ASE)?\\s?(\\d+|I)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = ma.group(1).replaceFirst("\\bI\\b", "1");
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PHASE ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and remove ParcelID from legal description
		String pid = "";
		String sec = "", twn = "", rng = "";
		p = Pattern.compile("^(\\d+[A-Z]?)[-\\s](\\d+[A-Z]?)[-\\s](\\d+[A-Z]?)[-\\s]\\d+[A-Z]?[-\\s]\\d+[A-Z]?[-\\s]\\d+[A-Z]?\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			pid = ma.group(0).replaceAll("\\s", "-");
			sec = ma.group(1).replaceFirst("^0+(.+)", "$1");
			twn = ma.group(2).replaceFirst("^0+(.+)", "$1");
			rng = ma.group(3).replaceFirst("^0+(.+)", "$1");
			m.put("PropertyIdentificationSet.ParcelID", pid);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace section, township and range from legal
		// description
		List<List> body = getSTRFromMap(m); // first add sec-twn-rng extracted
											// from XML specific tags, if any
											// (for DT use)
		String patt = "(\\d+(?:\\s?[&,-]\\s?\\d+)*) (?:TOWNSHIP |T)(\\d+\\s?[SWEN]?)\\b";
		p = Pattern.compile("\\bSEC(?:TION)?S?((?:\\s?&?\\s?" + patt + ")*)(?: [RT](\\d+\\s?[SWEN]?)\\d*)?\\b");
		p2 = Pattern.compile(patt);
		ma = p.matcher(legal);
		if (ma.find()) {
			String rng2 = ma.group(4);
			if (rng2 == null)
				rng2 = "";
			else
				rng2 = rng2.replaceFirst("\\b(\\d+) ([SWEN])\\b", "$1$2");
			ma2 = p2.matcher(ma.group(1));
			while (ma2.find()) {
				List<String> line = new ArrayList<String>();
				line.add(ma2.group(1).replaceAll("\\s?[,&]\\s?", " "));
				line.add(ma2.group(2).replaceFirst("\\b(\\d+) ([SWEN])\\b", "$1$2"));
				line.add(rng2);
				body.add(line);
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
		p = Pattern.compile("\\bT\\s?(\\d+[SWEN]) R\\s?(\\d+[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add(ma.group(1));
			line.add(ma.group(2));
			body.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
		if (sec.length() != 0) {
			List<String> line = new ArrayList<String>();
			line.add(sec);
			line.add(twn);
			line.add(rng);
			body.add(line);
		}
		saveSTRInMap(m, body);
		p = Pattern.compile("\\bR(?:GE?|A?NGE)?\\s?(\\d+|[A-Z]-\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionRange", ma.group(1));
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
		p = Pattern.compile("\\bSEC(?:TION)?\\s?(\\d+|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(1));
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace tract from legal description
		String tract = "";
		p = Pattern.compile("\\bTR(?:ACT)? (\\d+(?:-?[A-Z])?(?:\\s?&\\s?\\d+(?:-?[A-Z])?)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = ma.group(1).replaceAll("\\s?[,&]\\s?", " ");
		} else {
			p = Pattern.compile("\\bT(\\d+[SWEN]?(?:-\\d+[SWEN]?)?)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				tract = ma.group(1);
			}
		}
		if (tract.length() != 0) {
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "TRACT ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
	}

	public static void legalRemarksFLFranklinRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerRemarksFLFranklinRV(m, legal);
	}

	public static void legalTokenizerRemarksFLPolkRV(ResultMap m, String legal) throws Exception {

		// initial corrections and cleanup of legal description
		legal = legal.replaceAll("\\bETC\\b", "");
		legal = legal.replaceAll("^\\w+ [SWNE\\s]{1,3} COR\\b", "");
		legal = legal.replaceAll("\\b[SWNE]{1,2} \\d+/\\d+\\b", "");
		legal = legal.replaceAll("\\b(U\\s?S)? GOV(?= LO?TS?\\b)", "");
		legal = legal.replaceAll("\\bNO(?= \\d+\\b)", "");
		legal = legal.replaceAll("\\bPT\\b", "");
		legal = legal.replaceAll("\\bTIER \\d+\\b", "");
		legal = legal.replaceAll("\\b(\\d+)(?:ST|ND|RD|TH) (UN(?:IT)?)\\b(?! \\d)", "$2 $1");
		legal = replaceNumbers(legal);
		legal = legal.replaceAll("\\s{2,}", " ").trim();

		// extract and remove ParcelID from legal description
		String pid = "";
		Pattern p = Pattern.compile("\\b\\d{2}-\\d{2}-\\d{2}-\\d{6}-\\d+\\b");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			pid = ma.group(0).replaceAll("-", "");
			m.put("PropertyIdentificationSet.ParcelID", pid);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and remove cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("\\bO\\s?[RB] BK\\s?(\\d+) PG\\s?(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add("");
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		saveCRInMap(m, bodyCR);
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract and remove section, township and range from legal description
		List<List> body = getSTRFromMap(m); // first add sec-twn-rng extracted
											// from XML specific tags, if any
											// (for DT use)
		p = Pattern.compile("\\b(\\d+)\\s*-\\s*(\\d+)\\s*-\\s*(\\d+)\\b(?!\\s*-)");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(2).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(3).replaceFirst("^0+(\\d+)", "$1"));
			body.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
		saveSTRInMap(m, body);
		p = Pattern.compile("\\bSEC (\\d+|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(1));
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace lot from legal description
		String lot = "";
		p = Pattern.compile("\\bLO?TS?\\s?(\\d+(\\s*[&,\\s-]\\s*\\d+)*(?:[A-Z]\\d*)?|[A-Z]\\d*)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			lot = ma.group(1);
			lot = lot.replaceAll("\\s*[&,]\\s*", " ");
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " LOT ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace block from legal description
		String block = "";
		p = Pattern.compile("\\b(?:BL?K|BKL) ([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			block = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " BLK ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace unit from legal description
		String unit = "";
		p = Pattern.compile("\\bUN(?:IT)? (\\d+|[A-Z]\\d*)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			unit = ma.group(1);
			unit = unit.replaceFirst("^0+(\\d+)", "$1");
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " UNIT ");
		}
		p = Pattern.compile("\\bAPT (\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " UNIT ");
		}
		if (unit.length() == 0) {
			p = Pattern.compile(" #([A-Z]?\\d+)$");
			ma = p.matcher(legal);
			if (ma.find()) {
				unit = unit + " " + ma.group(1);
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " UNIT ");
			}
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace building # from legal description
		String bldg = "";
		p = Pattern.compile("\\bBLDG (\\d+|[A-Z]{1,2}(?: \\d+)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " BLDG ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace phase from legal description
		String phase = "";
		p = Pattern.compile("\\bPH(?:ASES?)? (\\d+(?:\\s*[A-Z])?(?:\\s*&\\s*\\d+)?|([IVX]+[A-Z]?(?:\\s*&\\s*[IVX]+)*))\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = ma.group(1);
			phase = phase.replaceAll("\\s*&\\s*", " ");
			phase = phase.replaceAll("\\b([IVX]+)", "$1 --");
			phase = Roman.normalizeRomanNumbers(phase);
			phase = phase.replaceAll(" --", "");
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " PHASE ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace tract from legal description
		String tract = "";
		p = Pattern.compile("\\bTR(?:ACT)?S? (\\d+[A-Z]?|[A-Z](?:\\s*&\\s*[A-Z])?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = ma.group(1).replaceAll("\\s*&\\s*", " ");
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " TRACT ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace plat b&p from legal description
		p = Pattern.compile("\\bPB (\\d+) PG\\s?(\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
			m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " PB ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace condo plat b&p from legal description
		p = Pattern.compile("\\bCB (\\d+) PG\\s?(\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.CondominiumPlatBook", ma.group(1));
			m.put("PropertyIdentificationSet.CondominiumPlatPage", ma.group(2));
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " CB ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
	}

	public static void legalRemarksFLPolkRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerRemarksFLPolkRV(m, legal);
	}

	public static void legalRemarksFLBrowardRV(ResultMap m, long searchId) throws Exception {
		FLBrowardRV.legalRemarksFLBrowardRV(m, searchId);
	}

	public static void legalTokenizerRemarksFLPalmBeachRV(ResultMap m, String legal) throws Exception {

		// initial corrections and cleanup of legal description
		legal = legal.replaceAll("\\bUNIDENTIFIABLE LAND DESC\\b", "");
		legal = legal.replaceAll("\\bPT(\\s*\\d+)\\b", "");
		legal = legal.replaceAll("\\*", "");
		legal = legal.replaceAll("(\\d+)OL\\b", "$1");
		legal = legal.replaceAll("\\bOL\\b", "");
		legal = legal.replaceAll("\\b(U [A-Z]\\d+)([A-Z]{3,})\\b", "$1 $2");
		legal = legal.replaceAll("\\b(\\d+[A-Z]?)(PH\\d+)\\b", "$1 $2");
		legal = legal.replaceAll("(\\d)\\s*THRU\\s*(\\d)", "$1-$2");
		legal = legal.replaceAll("(?<=\\bOF )LT\\b", "");
		legal = legal.replaceAll("^(TH)(L\\d+[A-Z]?\\b)", "$1 $2");
		legal = legal.replaceAll("\\b(WK|UW)\\d+(&\\d+)?\\b", "");
		legal = legal.replaceFirst("\\s*\\bSE$", "");
		legal = legal.replaceFirst("\\s*\\bTW$", "");

		boolean hasPlat = false;
		Pattern p = Pattern.compile("(.*)\\b(?:(?:RE?)?PL(?:AT)?|RE?PL?)\\d*\\b(.*)");
		Matcher ma = p.matcher(legal);
		if (ma.find()) {
			legal = ma.group(1) + ma.group(2);
			hasPlat = true;
		}

		legal = legal.trim();
		legal = legal.replaceAll("\\s{2,}", " ");
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };

		// extract and remove cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("\\b(\\d{11})\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(1));
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		p = Pattern.compile("\\bORB\\s*(\\d+) P\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add("");
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		p = Pattern.compile("\\bORB:(\\d+) ORP:(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add("");
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		saveCRInMap(m, bodyCR);
		legal = legal.trim().replaceAll("\\s{2,}", " ");
		legal = legal.replaceFirst("^\\s*;\\s*", "");

		// extract and remove section, township and range from legal description
		List<List> body = getSTRFromMap(m); // first add sec-twn-rng extracted
											// from XML specific tags, if any
											// (for DT use)
		String sec = "", twn = "", rng = "";
		p = Pattern.compile("\\b(?:ACREAGE )?(\\d+(?:&\\d+)?) (\\d+) (\\d+) PORT?\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			sec = ma.group(1).replace('&', ' ').replaceFirst("^0+(\\d*)", "$1");
			twn = ma.group(2).replaceFirst("^0+(\\d*)", "$1");
			rng = ma.group(3).replaceFirst("^0+(\\d*)", "$1");
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		} else {
			p = Pattern.compile("^\\s*(\\d+(?:&\\d+)?) (\\d+) (\\d+)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				sec = ma.group(1).replace('&', ' ').replaceFirst("^0+(\\d*)", "$1");
				twn = ma.group(2).replaceFirst("^0+(\\d*)", "$1");
				rng = ma.group(3).replaceFirst("^0+(\\d*)", "$1");
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
			} else {
				p = Pattern.compile("\\bTOWNSHIP: (\\d+) RANGE: (\\d+) SECTION: (\\d+)\\b");
				ma.usePattern(p);
				ma.reset();
				if (ma.find()) {
					sec = ma.group(3).replaceFirst("^0+(\\d*)", "$1");
					twn = ma.group(1).replaceFirst("^0+(\\d*)", "$1");
					rng = ma.group(2).replaceFirst("^0+(\\d*)", "$1");
					legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
				} else {
					p = Pattern.compile("\\bSEC(?:TION)? ([A-Z]?\\d+)\\b");
					ma.usePattern(p);
					ma.reset();
					if (ma.find()) {
						sec = ma.group(1).replaceFirst("^0+(\\d*)", "$1");
						legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " SEC ");
					}
					p = Pattern.compile("\\bS(\\d+|[IVX]+)\\b");
					ma.usePattern(p);
					ma.reset();
					if (ma.find()) {
						sec = sec + " " + ma.group(1).replaceFirst("^0+(\\d*)", "$1");
						sec = Roman.normalizeRomanNumbersExceptTokens(sec, exceptionTokens); // convert
																								// roman
																								// numbers
																								// to
																								// arabics
						legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " SEC ");
					}
					p = Pattern.compile("SEC:([A-Z]?\\d+)");
					ma.usePattern(p);
					ma.reset();
					if (ma.find()) {
						sec = sec + " " + ma.group(1).replaceFirst("^0+(\\d*)", "$1");
						legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
					}
					sec = sec.trim();
					p = Pattern.compile("TWN[:\\s](\\d+)");
					ma.usePattern(p);
					ma.reset();
					if (ma.find()) {
						twn = ma.group(1).replaceFirst("^0+(\\d*)", "$1");
						legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
					}
					p = Pattern.compile("RNG[:\\s](\\d+)");
					ma.usePattern(p);
					ma.reset();
					if (ma.find()) {
						rng = ma.group(1).replaceFirst("^0+(\\d*)", "$1");
						legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
					}
				}
			}
		}
		if (sec.length() != 0) {
			sec = LegalDescription.cleanValues(sec, false, true);
		}
		if (sec.length() != 0 || twn.length() != 0 || rng.length() != 0) {
			List<String> line = new ArrayList<String>();
			line.add(sec);
			line.add(twn);
			line.add(rng);
			body.add(line);
		}
		saveSTRInMap(m, body);
		legal = legal.replaceAll("\\s{2,}", " ");

		// extract and remove unit from legal description
		String unit = "";
		String bldg = "";
		p = Pattern.compile("\\bUNIT #\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		p = Pattern.compile("\\bA(\\d+) BLDG ([A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			unit = unit + " " + ma.group(1);
			bldg = ma.group(2);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		} else {
			p = Pattern.compile("\\bBD(\\d+) A(\\d+)\\b");
			ma.usePattern(p);
			ma.reset();
			if (ma.find()) {
				unit = unit + " " + ma.group(2);
				bldg = ma.group(1);
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
			} else {
				p = Pattern.compile("\\bA\\s*(\\d+ \\d+)\\b");
				ma.usePattern(p);
				ma.reset();
				if (ma.find()) {
					unit = unit + " " + ma.group(1);
					legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
				}
			}
		}
		p = Pattern.compile("\\bU[\\s-]?((?:\\d+|[IVX]+)(?:-?[A-Z]{1,2}\\d*)?(?:[\\s-]\\d+)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			unit = unit + " " + Roman.normalizeRomanNumbersExceptTokens(ma.group(1), exceptionTokens);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		p = Pattern.compile("\\bU ([A-Z]{1,2}\\d*)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		p = Pattern.compile("\\bU([A-Z]{1,2}-?\\d+)\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		p = Pattern.compile("(\\bBL\\d+ )(\\bU([A-Z])\\b)");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			unit = unit + " " + ma.group(3);
			legal = legal.replaceFirst("\\b" + ma.group(2) + "\\b", "UNIT ");
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			String unit2 = (String) m.get("PropertyIdentificationSet.SubdivisionUnit");
			if (!StringUtils.isEmpty(unit2)) {
				unit2 = unit2 + " " + unit;
			} else {
				unit2 = unit;
			}
			unit2 = LegalDescription.cleanValues(unit2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and remove building from legal description
		p = Pattern.compile("\\bBLDG(\\s*\\d+[A-Z]?|\\s*[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(1).trim();
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLDG ");
		}
		p = Pattern.compile("\\bBL\\s*(\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLDG ");
		}
		p = Pattern.compile("\\bBL\\s*([A-Z])(?= UNIT\\b)");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = bldg + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLDG ");
		}
		bldg = bldg.trim();
		if (bldg.length() != 0) {
			bldg = LegalDescription.cleanValues(bldg, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace block from legal description
		String block = "";
		p = Pattern.compile("\\bB\\s*(\\d+[A-Z]?(?:[&,-]\\d+)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1).replaceAll("[&,]", " ");
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLOCK ");
		}
		p = Pattern.compile("\\bB\\s*([A-Z]) (?=L\\d)");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			block = block + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLOCK ");
		}
		block = block.trim();
		if (block.length() == 0) {
			p = Pattern.compile("(\\bL\\d+ )(\\bB\\s*([A-Z]))\\b");
			ma.usePattern(p);
			ma.reset();
			while (ma.find()) {
				block = block + " " + ma.group(3);
				legal = legal.replaceFirst("\\b" + ma.group(2) + "\\b", "BLOCK ");
			}
			block = block.trim();
		}
		if (block.length() != 0) {
			String block2 = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(block2)) {
				block2 = block2 + " " + block;
			} else {
				block2 = block;
			}
			block2 = LegalDescription.cleanValues(block2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace lot from legal description
		String lot = "";
		p = Pattern.compile("LOT:(\\d+)");
		ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
			legal = legal.replace(ma.group(0), "");

		}
		p = Pattern.compile("\\bL((?:\\d+[A-Z]?|[A-Z]-?\\d+)(?:[&,\\s-]\\d+)*)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			lot = lot + " " + ma.group(1).replaceAll("[&,]", " ");
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "LOT ");
		}
		if (block.length() != 0) {
			p = Pattern.compile("(?<!^)\\bL([A-Z])\\b(?!$)");
			ma = p.matcher(legal);
			String lotTemp = "";
			while (ma.find()) {
				lotTemp = ma.group(1);
				if (!"K".equals(lotTemp)) {
					lot = lot + " " + lotTemp;
					legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "LOT ");
				}
			}
			if (lotTemp.length() == 0) {
				p = Pattern.compile("(?<=\\bBLOCK )L([A-Z])\\b");
				ma = p.matcher(legal);
				if (ma.find()) {
					lot = lot + " " + ma.group(1);
					legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "LOT ");
				}
			}
		}
		p = Pattern.compile("(?<!\\d CA )\\bL ([A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "LOT ");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			String lot2 = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (!StringUtils.isEmpty(lot2)) {
				lot2 = lot2 + " " + lot;
			} else {
				lot2 = lot;
			}
			lot2 = LegalDescription.cleanValues(lot2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		legal = legal.replaceAll("\\bP(CL)?\\s*\\d+\\b", "PARCEL");
		legal = legal.replaceAll("\\bP [A-Z]\\d+\\b", "PARCEL");
		legal = legal.replaceAll("\\bP[\\s-]?\\d+\\b", "PARCEL");
		legal = legal.replaceAll("\\bPAR \\w+\\b", "PARCEL");
		legal = legal.replaceAll("\\s{2,}", " ");

		// extract and replace tract from legal description
		String tract = "";
		p = Pattern.compile("\\bTRACT( [A-Z]\\d*|\\s*\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = ma.group(1).trim();
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "TRACT ");
		}
		p = Pattern.compile("\\bTR\\s*([A-Z]\\d+(?:-[A-Z]?\\d+)?|\\d+(?:-\\d+)?|\\s[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = tract + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "TRACT ");
		}
		p = Pattern.compile("\\bT(\\d+(?:-\\d+)?|[A-Z]\\d+(?:-[A-Z]\\d+)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = tract + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "TRACT ");
		}
		p = Pattern.compile("\\bT(\\s*[A-Z]-?\\d+|\\s[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = tract + " " + ma.group(1).trim();
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "TRACT ");
		}
		p = Pattern.compile("\\bTR?([A-Z])\\b(?! (CO?N(DO)?|LOT|PARCEL|[A-Z]?\\d+|CRIM)\\b)");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = tract + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "TRACT ");
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace phase from legal description
		String phase = "";
		p = Pattern.compile("\\bPH(?:ASE)?\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PHASE ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and remove plat b&p from legal description
		String platBk = "", platPg = "";
		p = Pattern.compile("\\bMB (\\d+) MP (\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			platBk = ma.group(1);
			platPg = ma.group(2);
		} else {
			p = Pattern.compile("\\bM_BK:(\\d+); M_PG:(\\d+)\\b");
			ma.usePattern(p);
			ma.reset();
			if (ma.find()) {
				platBk = ma.group(1);
				platPg = ma.group(2);
			}
		}
		if (platBk.length() != 0) {
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "MB ");
			m.put("PropertyIdentificationSet.PlatBook", platBk.replaceFirst("^0+(\\d+)", "$1"));
			m.put("PropertyIdentificationSet.PlatNo", platPg.replaceFirst("^0+(\\d+)", "$1"));
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract subdivision name - only if lot or block or unit or building
		// or tract or phase was extracted
		// or legal contains CONDO or POR or APT or hasPlat; otherwise the text
		// might be a doc type, a case number etc.
		// additional cleaning before extracting subdivision name
		String subdiv = "";
		if (lot.length() != 0 || block.length() != 0 || unit.length() != 0 || bldg.length() != 0 || phase.length() != 0 || tract.length() != 0
				|| platBk.length() != 0 || sec.length() != 0 || legal.matches("(?i).*\\b(COND(O(MINIUM)?)?|CN(DO)?)\\b.*") || legal.matches(".+\\bPOR\\b.*")) {
			p = Pattern.compile("^(?:(?:UNIT|BLOCK|LOT|BLDG|PARCEL|TH|TRACT) )+(.+)");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
			if (subdiv.length() != 0) {
				legal = subdiv;
			}
			p = Pattern
					.compile("(.*?)\\s*\\b(?:LOT|BLOCK|UNIT|ADD|MB|COND(?:O(?:MINIUM)?)?|CN(DO)?|SEC|PH(?:ASE)?|POR|TRACT|BLDG|(?:RE)?SUB|POR|RNG|TWN)\\b.*");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		} else {
			p = Pattern.compile("(.+) #\\s*\\d+\\b.*");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else if (legal.matches(".+\\bAPT\\b.*") || hasPlat || legal.matches("^PARCEL .+")) {
				subdiv = legal;
			}
		}

		subdiv = subdiv.trim();
		if (subdiv.length() != 0) {
			subdiv = subdiv.replaceFirst("^PARCEL\\b", "");
			subdiv = subdiv.replaceFirst("\\bPARCEL\\b.*", "");
			subdiv = subdiv.replaceFirst("\\b[SWEN]{1,2} \\d+ FT\\b.*", "");
			subdiv = subdiv.replaceFirst("\\bINCOMPLETE\\b", "");
			subdiv = subdiv.replaceAll("\\bAMENDED\\b", "");
			subdiv = subdiv.replaceAll("\\bPOR\\b", "");
			subdiv = subdiv.replaceAll("^\\s*PT( (\\d+|[A-Z]))?\\b", "");
			subdiv = subdiv.replaceAll("\\bPT( (\\d+|[A-Z]))?\\b.*", "");
			subdiv = subdiv.replaceFirst("^\\s*;\\s*", "");
			subdiv = subdiv.replaceFirst("\\s*;\\s*$", "");
			subdiv = subdiv.replaceFirst("\\s*\\b\\d+[A-Z]?([&-]\\d+)?\\s*$", "");
			subdiv = subdiv.replaceFirst("\\s*\\b[A-Z]\\d+\\s*$", "");
			subdiv = subdiv.replaceFirst("\\s*(\\bNO|#)\\s*(\\b[\\w&-]+\\s*)?$", "");
			subdiv = subdiv.replaceFirst("\\s*\\.\\s*$", "");

			// remove last token from subdivision name if it is a number (as
			// roman or arabic)
			p = Pattern.compile("(.*)\\b(\\w+)$");
			ma = p.matcher(subdiv);
			if (ma.find()) {
				String lastToken = ma.group(2);
				lastToken = Roman.normalizeRomanNumbersExceptTokens(lastToken, exceptionTokens);
				if (lastToken.matches("\\d+")) {
					subdiv = ma.group(1);
				}
			}
			subdiv = subdiv.trim().replaceAll("\\s{2,}", " ");
		}
		if (subdiv.length() != 0) {
			m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			if (legal.matches(".*\\b(COND(O(MINIUM)?)?|CN(DO)?)\\b.*")) {
				m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			}
		}
	}

	public static void legalRemarksFLPalmBeachRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerRemarksFLPalmBeachRV(m, legal);
	}

	public static void legalTokenizerRemarksFLSeminoleRV(ResultMap m, String legal) throws Exception {

		// initial corrections and cleanup of legal description
		legal = legal.replaceAll("\\bETC\\b", "");
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);
		legal = replaceNumbers(legal);
		legal = legal.replaceAll("#\\s*(\\d+)", "$1");
		legal = legal.replaceAll("(\\d+)(?:ST|ND|RD|TH) SEC\\b", "SEC $1");
		legal = legal.replaceAll("\\b\\d+/\\d+/\\d+\\b", "");
		legal = legal.replaceAll("\\b([SWEN]{1,2}) (\\d+(?:[\\./]\\d+)?)\\b", "$1$2");

		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract and replace cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		Pattern p = Pattern.compile("\\b(\\d{10,})\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(1));
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "OR ");
		}
		p = Pattern.compile("(?:\\bORB )?\\b(\\d+)/(\\d+(?:\\s&\\s\\d+)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2).replace('&', ' ').replaceAll("\\s{2,}", " "));
			line.add("");
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "ORB ");
		}
		p = Pattern.compile("\\bORB (\\d+) (?:PG|PAGE) (\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add("");
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "ORB ");
		}
		saveCRInMap(m, bodyCR);
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract and replace section, township and range from legal
		// description
		List<List> bodyPIS = getSTRFromMap(m); // first add sec-twn-rng
												// extracted from XML specific
												// tags, if any (for DT use)
		String sec = "", twn = "", rng = "";
		p = Pattern.compile("\\bSEC (\\d+(?:\\s*[,&]\\s*\\d+)*)(?:-(\\d+)-(\\d+))?\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			sec = ma.group(1).replaceAll("[&,]", " ").replaceAll("\\s{2,}", " ");
			twn = ma.group(2);
			if (twn == null) {
				twn = "";
			}
			rng = ma.group(3);
			if (rng == null) {
				rng = "";
			}
			List<String> line = new ArrayList<String>();
			line.add(sec);
			line.add(twn);
			line.add(rng);
			bodyPIS.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}
		if (legal.matches(".*\\b[SWEN]{1,2}\\d+([/\\.]\\d+)?\\b.*")) {
			p = Pattern.compile("\\b(\\d+(?:\\s*[,&]\\s*\\d+)*)(?:-(\\d+)-(\\d+))\\b");
			ma = p.matcher(legal);
			while (ma.find()) {
				sec = ma.group(1).replaceAll("[&,]", " ").replaceAll("\\s{2,}", " ");
				twn = ma.group(2);
				if (twn == null) {
					twn = "";
				}
				rng = ma.group(3);
				if (rng == null) {
					rng = "";
				}
				List<String> line = new ArrayList<String>();
				line.add(sec);
				line.add(twn);
				line.add(rng);
				bodyPIS.add(line);
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
			}
		}
		saveSTRInMap(m, bodyPIS);
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract and replace unit from legal description
		String unit = "";
		p = Pattern.compile("\\bUN(?:IT)? ((?:\\d+|[IXV]+)(?:[\\s-]?[A-Z])?|[A-Z](?:[\\s-]?\\d+)?)\\b(?!/)");
		ma = p.matcher(legal);
		while (ma.find()) {
			String unitTemp = ma.group(1);
			if (unitTemp.matches("([IXZ]+)([A-HJ-UWYZ])")) {
				unitTemp = unitTemp.replaceFirst("([IXZ]+)([A-HJ-UWYZ])", "$1,$2");
				unitTemp = Roman.normalizeRomanNumbersExceptTokens(unitTemp, exceptionTokens);
				unitTemp = unitTemp.replaceFirst(",", "");
				unitTemp = unitTemp.replaceAll("\\bI([A-Z])\\b", "1$1");
			}
			unit = unit + " " + unitTemp;
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			String unit2 = (String) m.get("PropertyIdentificationSet.SubdivisionUnit");
			if (!StringUtils.isEmpty(unit2)) {
				unit2 = unit2 + " " + unit;
			} else {
				unit2 = unit;
			}
			unit2 = LegalDescription.cleanValues(unit2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace building from legal description
		String bldg = "";
		p = Pattern.compile("\\bBLDG (\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLDG ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace block from legal description
		String block = "";
		p = Pattern.compile("\\bB(\\d+|[IXV]+|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			String blkTemp = ma.group(1);
			if (!"Y".equals(blkTemp)) {
				blkTemp = Roman.normalizeRomanNumbersExceptTokens(blkTemp, exceptionTokens);
				block = block + " " + blkTemp;
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLK ");
			}
		}
		block = block.trim();
		if (block.length() != 0) {
			String block2 = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(block2)) {
				block2 = block2 + " " + block;
			} else {
				block2 = block;
			}
			block2 = LegalDescription.cleanValues(block2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace lot from legal description
		String lot = "";
		p = Pattern.compile("\\bL,?(\\d+[A-Z]?(?:\\s*[&,\\s-]\\s*\\d+)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			String lotTemp = ma.group(1);
			lotTemp = lotTemp.replaceFirst("^0+(\\d+)", "$1");
			lotTemp = lotTemp.replaceAll("[&,]", " ").replaceAll("\\s{2,}", " ");
			lotTemp = lotTemp.replaceAll("\\s*-\\s*", "-");
			lot = lot + " " + lotTemp;
			legal = legal.replace(ma.group(0), "LOT ");

		}
		p = Pattern.compile("\\bL([A-Z](?:\\s*\\d+)?(?:\\s*&\\s*[A-Z])*)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			String lotTemp = ma.group(1);
			if (!"K".equals(lotTemp)) {
				lotTemp = lotTemp.replaceAll("[&,]", " ").replaceAll("\\s{2,}", " ");
				lotTemp = lotTemp.replaceAll("\\s*-\\s*", "-");
				lot = lot + " " + lotTemp;
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "LOT ");
			}
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			String lot2 = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (!StringUtils.isEmpty(lot2)) {
				lot2 = lot2 + " " + lot;
			} else {
				lot2 = lot;
			}
			lot2 = LegalDescription.cleanValues(lot2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace tract from legal description
		String tract = "";
		p = Pattern.compile("\\bTR(?:ACT)? (\\d+|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "TRACT ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace phase from legal description
		String phase = "";
		p = Pattern.compile("\\bPH(?:ASE)? ((?:\\d+|I)(?:[\\s-]?[A-Z])?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = ma.group(1);
			phase = phase.replaceFirst("\\bI\\b", "1");
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PHASE ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace plat b&p from legal description
		String platBk = "", platPg = "";
		p = Pattern.compile("\\bPB (\\d+) PG (\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			platBk = ma.group(1);
			platPg = ma.group(2);
			m.put("PropertyIdentificationSet.PlatBook", platBk);
			m.put("PropertyIdentificationSet.PlatNo", platPg);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PB ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract subdivision name - not requested
	}

	public static void legalRemarksFLSeminoleRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerRemarksFLSeminoleRV(m, legal);
	}

	public static void legalTokenizerRemarksFLBrevardRV(ResultMap m, String legal) throws Exception {

		// initial corrections and cleanup of legal description
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);
		legal = replaceNumbers(legal);
		legal = legal.replaceAll("\\bNO\\b\\s*", "");
		legal = legal.replaceAll("\\bPRT\\b\\s*", "");
		legal = legal.replaceAll("(?<=\\d[A-Z]?)(\\s*-\\s*| THRU | TO )(?=\\d)", "-");
		legal = legal.replaceAll("#\\s*(\\d+)", "$1");
		legal = legal.replaceAll("\\b\\d+/\\d+/\\d+\\b", "");
		legal = legal.replaceAll("(?<!\\bBLK )\\b([SWEN]{1,2}|NORTH|SOUTH|WEST|EAST) \\d+([\\./]\\d+)?(?! T \\d+)( FT)?( OF)?\\b", "");
		legal = legal.replaceAll("\\bTOTAL LOTS \\d+\\b", "");
		legal = legal.replaceAll("\\bUS GOV(ERNMENT)?\\b", "");
		boolean hasUnit = false;
		Pattern p = Pattern.compile("\\bUW \\d+(?:[-,]\\d+)*\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
			hasUnit = true;
		}
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract and replace unit from legal description
		String unit = "";
		p = Pattern.compile("\\bU[-\\s]?((?:\\d+[A-Z]{0,2}|[A-Z]{1,2}(?:-?\\d+)?)(?:[,-]\\d+)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			String unitTemp = ma.group(1).replaceFirst("^0+(\\d+)", "$1").replace(',', ' ');
			unit = unit + " " + unitTemp;
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		p = Pattern.compile("\\bUNIT (\\d+|[A-Z](?:-?\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			String unitTemp = ma.group(1).replaceFirst("^0+(\\d+)", "$1");
			unit = unit + " " + unitTemp;
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		if (hasUnit) {
			p = Pattern.compile("\\bPRCL ([A-Z]-?\\d+)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				String unitTemp = ma.group(1).replaceFirst("^0+(\\d+)", "$1");
				unit = unit + " " + unitTemp;
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
			}
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			String unit2 = (String) m.get("PropertyIdentificationSet.SubdivisionUnit");
			if (!StringUtils.isEmpty(unit2)) {
				unit2 = unit2 + " " + unit;
			} else {
				unit2 = unit;
			}
			unit2 = LegalDescription.cleanValues(unit2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace building from legal description
		String bldg = "";
		p = Pattern.compile("\\b(?:BUILDING|BLDG) ([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLDG ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace tract from legal description
		String tract = "";
		p = Pattern.compile("\\bTR(?:ACT)?S? (\\d+|[A-Z]{1,2}(?:[-\\.]\\d+)?(?:[,-][A-Z])*)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = ma.group(1).replace(',', ' ').replaceAll("\\s{2,}", " ");
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "TRACT ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace block from legal description
		String block = "";
		p = Pattern.compile("\\bBL(?:OC)?K?S?\\s*((?:[\\d\\.]+[A-Z]?|[A-Z])(?:[\\s,;-]\\d+(?:-\\d+)*)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			String matched = ma.group(0);
			if (!matched.matches("BL(OC)?KS?")) {
				String blkTemp = ma.group(1).replaceAll("[;,]", " ").replaceAll("\\s{2,}", " ");
				block = block + " " + blkTemp;
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLK ");
			}
		}
		p = Pattern.compile("\\bB([\\d\\.]+[A-Z]?|[A-Z]\\d*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			String blkTemp = ma.group(1);
			if (!"Y".equals(blkTemp)) {
				block = block + " " + blkTemp;
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLK ");
			}
		}
		block = block.trim();
		if (block.length() != 0) {
			String block2 = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(block2)) {
				block2 = block2 + " " + block;
			} else {
				block2 = block;
			}
			block2 = LegalDescription.cleanValues(block2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace lot from legal description
		String lot = "";
		p = Pattern.compile("\\bLO?TS? (\\d+(?:[A-Z]\\d*)?(?:\\.\\d+)?(?:[\\s,&-]+\\d+[A-Z]?)*)");
		ma = p.matcher(legal);
		while (ma.find()) {
			String lotTemp = ma.group(1).replaceAll("\\d+-\\d+-\\d+", "");
			lotTemp = lotTemp.replaceFirst("^0+(\\d+)", "$1");
			lotTemp = lotTemp.replaceAll("[,&]", " ").replaceAll("\\s{2,}", " ");
			lot = lot + " " + lotTemp;
			legal = legal.replace(ma.group(0), "LOT ");

		}
		p = Pattern.compile("\\bL(\\d+(?:-?[A-Z]\\d*)?(?:\\.\\d+)?|[A-Z]\\.\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			String lotTemp = ma.group(1);
			if (!"K".equals(lotTemp)) {
				lot = lot + " " + lotTemp;
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "LOT ");
			}
		}
		p = Pattern.compile("^\\s*L([A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			lot = lot + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "LOT ");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			String lot2 = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (!StringUtils.isEmpty(lot2)) {
				lot2 = lot2 + " " + lot;
			} else {
				lot2 = lot;
			}
			lot2 = LegalDescription.cleanValues(lot2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace phase from legal description
		String phase = "";
		p = Pattern.compile("\\bPH(?:ASE)?\\s*(\\d+(?:-?[A-Z])?(?:-\\d+)?|I)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = ma.group(1).replaceFirst("^0+(\\d+)", "$1");
			phase = phase.replaceFirst("\\bI\\b", "1");
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PHASE ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace section, township and range from legal
		// description
		String sec = "";
		List<List> bodyPIS = getSTRFromMap(m); // first add sec-twn-rng
												// extracted from XML specific
												// tags, if any (for DT use)
		p = Pattern.compile("\\bS\\s*(\\d+) T\\s*(\\d+)[A-Z]? R\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(2).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(3).replaceFirst("^0+(\\d+)", "$1"));
			bodyPIS.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}
		p = Pattern.compile("\\bSEC (\\d+)-(\\d+)-(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(2).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(3).replaceFirst("^0+(\\d+)", "$1"));
			bodyPIS.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}
		p = Pattern.compile("\\bSEC(?:TION)?\\s*(\\d+(?:-?[A-Z])?|\\s[A-Z](?:[\\s-]?\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1").trim());
			line.add("");
			line.add("");
			bodyPIS.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}
		saveSTRInMap(m, bodyPIS);
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract and replace plat b&p from legal description
		String platBk = "", platPg = "";
		// extract plat book & page from legal description
		List<List> bodyPlat = new ArrayList<List>();
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("\\bPB (\\d+)/(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(2).replaceFirst("^0+(\\d+)", "$1"));
			bodyPlat.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PB ");
		}
		p = Pattern.compile("\\bPB\\s*(\\d+) P(?:P|G)\\s*(\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(2).replaceFirst("^0+(\\d+)", "$1"));
			bodyPlat.add(line);
			// sometimes the book and page are not a plat => the b&p should be
			// added also to cross ref set (e.g. the legal from b&p 4057_1845
			// and 5692_8520)
			List<String> line2 = new ArrayList<String>(line);
			line2.add("");
			line2.add("PB");
			bodyCR.add(line2);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PB ");
		}
		if (!bodyPlat.isEmpty()) {
			String[] header = { "PlatBook", "PlatNo" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("PlatBook", new String[] { "PlatBook", "" });
			map.put("PlatNo", new String[] { "PlatNo", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(bodyPlat);

			ResultTable pisSTR = (ResultTable) m.get("PropertyIdentificationSet");
			if (pisSTR != null) {
				pis = ResultTable.joinHorizontal(pis, pisSTR);
				map.putAll(pisSTR.map);
			}
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
		}

		// extract and replace cross refs from legal description
		String pagePatt = "\\d+(?:-\\d+)?";
		p = Pattern.compile("\\b(\\d+) (ORB?) (\\d+)/(" + pagePatt + "(?:," + pagePatt + "(?!/))*)\\b");
		ma = p.matcher(legal);
		String crb = "", crp = "";
		while (ma.find()) {
			crb = ma.group(3).replaceFirst("^0+(\\d*)", "$1");
			crp = ma.group(4).replaceFirst("^0+(\\d*)", "$1").replace(',', ' ');
			if (crb.length() != 0) {
				List<String> line = new ArrayList<String>();
				line.add(crb);
				line.add(crp);
				line.add(ma.group(1));
				line.add(ma.group(2));
				bodyCR.add(line);
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "OR ");
		}
		p = Pattern.compile("(\\bORB?)?\\s*(?<!-)\\b(\\d+)/(" + pagePatt + "(?:," + pagePatt + "(?!/))*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			crb = ma.group(2).replaceFirst("^0+(\\d*)", "$1");
			crp = ma.group(3).replaceFirst("^0+(\\d*)", "$1").replace(',', ' ');
			if (crb.length() != 0) {
				List<String> line = new ArrayList<String>();
				String bpType = ma.group(1);
				if (bpType == null)
					bpType = "";
				line.add(crb);
				line.add(crp);
				line.add("");
				line.add(bpType);
				bodyCR.add(line);
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "OR ");
		}
		p = Pattern.compile("\\b(ORB?) (\\d+) PG (" + pagePatt + "(?:," + pagePatt + "(?!/))*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			crb = ma.group(2).replaceFirst("^0+(\\d*)", "$1");
			crp = ma.group(3).replaceFirst("^0+(\\d*)", "$1");
			if (crb.length() != 0) {
				List<String> line = new ArrayList<String>();
				line.add(crb);
				line.add(crp);
				line.add("");
				line.add(ma.group(1));
				bodyCR.add(line);
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "ORB ");
		}
		p = Pattern.compile("\\b(DB) (\\d*)-(\\d*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			crb = ma.group(2).replaceFirst("^0+(\\d+)", "$1");
			crp = ma.group(3).replaceFirst("^0+(\\d+)", "$1");
			if (crb.length() != 0) {
				List<String> line = new ArrayList<String>();
				line.add(crb);
				line.add(crp);
				line.add("");
				line.add(ma.group(1));
				bodyCR.add(line);
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "ORB ");
		}
		p = Pattern.compile("\\b(SURVEY) BOOK (\\d*) PAGE (\\d*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			crb = ma.group(2).replaceFirst("^0+(\\d+)", "$1");
			crp = ma.group(3).replaceFirst("^0+(\\d+)", "$1");
			if (crb.length() != 0) {
				List<String> line = new ArrayList<String>();
				line.add(crb);
				line.add(crp);
				line.add("");
				line.add(ma.group(1));
				bodyCR.add(line);
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "ORB ");
		}
		p = Pattern.compile("\\b(\\d+) CLERKS FILE");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(1));
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "ORB ");
		}
		saveCRInMap(m, bodyCR);
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract subdivision name - not requested
	}

	public static void legalRemarksFLBrevardRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerRemarksFLBrevardRV(m, legal);
	}

	public static void legalTokenizerRemarksFLDuvalRV(ResultMap m, String legal) throws Exception {

		// initial corrections and cleanup of legal description
		legal = legal.replaceAll("(\\d+)(THE)\\b", "$1 $2");
		legal = legal.replaceAll("\\b(GOVT?|PT)(L\\d+)", "$1 $2");

		legal = replaceNumbers(legal);
		legal = legal.replaceAll("(\\bNO |#\\s*)(?=\\d)", " ");
		legal = legal.replaceAll("(?<=\\d[A-Z]?)(\\s*-\\s*| THRU | TO )(?=\\d)", "-");
		legal = legal.replaceAll("\\bPT\\b\\s*", "");
		legal = legal.replaceAll("\\bGOVT?\\b\\s*", "");
		legal = legal.replaceAll("\\b([SWEN]{1,2}|NORTH|SOUTH|WEST|EAST) \\d+([\\./]\\d+)?\\b", "");
		legal = legal.replaceAll(" &C(HTS)?\\b", "");

		// extract and replace unit from legal description
		String unit = "";
		Pattern p = Pattern.compile("\\bUN\\s*([A-Z]{1,2})\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		p = Pattern.compile("\\bUN?\\s*(\\d+[A-Z]?(?: \\d+)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			String unit2 = (String) m.get("PropertyIdentificationSet.SubdivisionUnit");
			if (!StringUtils.isEmpty(unit2)) {
				unit2 = unit2 + " " + unit;
			} else {
				unit2 = unit;
			}
			unit2 = LegalDescription.cleanValues(unit2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace building from legal description
		String bldg = "";
		p = Pattern.compile("\\bBLDG (\\d+[A-Z]?|[A-Z]\\d*)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLDG ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace lot from legal description
		String lotPatt = "L([A-Z]\\d*(?: [A-Z]\\d*)*)";
		String bPatt1 = "\\d+[A-Z]?";
		String bPatt2 = "[A-Z]";
		String blkPatt = "(?:" + bPatt1 + "|" + bPatt2 + ")";
		String lot = "";
		p = Pattern.compile("\\bL(?:OTS?)?\\s*(\\d+(?:-?[A-Z])?(?:[\\s-]\\d+[A-Z]?)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
			legal = legal.replace(ma.group(0), "LOT ");
		}
		p = Pattern.compile("^" + lotPatt + "\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "LOT ");
		}
		p = Pattern.compile("\\b" + lotPatt + "(?= B" + blkPatt + "\\b)");
		ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "LOT ");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			String lot2 = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (!StringUtils.isEmpty(lot2)) {
				lot2 = lot2 + " " + lot;
			} else {
				lot2 = lot;
			}
			lot2 = LegalDescription.cleanValues(lot2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace block from legal description
		String block = "";
		p = Pattern.compile("\\bB(?:L|K)?(" + bPatt1 + ")\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLK ");
		}
		p = Pattern.compile("\\bBL?KS? (" + blkPatt + "(?: " + blkPatt + ")*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLK ");
		}
		p = Pattern.compile("(?<=\\bLOT )B(" + bPatt2 + ")\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLK ");
		}
		block = block.trim();
		if (block.length() != 0) {
			String block2 = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(block2)) {
				block2 = block2 + " " + block;
			} else {
				block2 = block;
			}
			block2 = LegalDescription.cleanValues(block2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace tract from legal description
		String tract = "";
		p = Pattern.compile("\\bTR(?:ACT)?S? (\\d+(?: \\d+)*|[A-Z](?: [A-Z])*)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "TRACT ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace phase from legal description
		String phase = "";
		p = Pattern.compile("\\bPH(?:ASE)? (\\d+(?:-?[A-Z])?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PHASE ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and remove cross refs from legal description
		p = Pattern.compile("\\b(\\d{8,} )?(?:OR )?(\\d+)/(\\d+)\\b");
		ma = p.matcher(legal);
		List<List> bodyCR = new ArrayList<List>();
		List<String> line;
		while (ma.find()) {
			String bk = ma.group(2).replaceFirst("^0+(\\d*)", "$1");
			if (bk.length() != 0) {
				String instrNo = ma.group(1);
				if (instrNo == null)
					instrNo = "";
				line = new ArrayList<String>();
				line.add(bk);
				line.add(ma.group(3).replaceFirst("^0+(\\d*)", "$1"));
				line.add(instrNo);
				line.add("");
				bodyCR.add(line);
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		p = Pattern.compile("\\b(\\d{8,} )?O\\s*R((?: (\\d+) (\\d+))+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			String instrNo = ma.group(1);
			if (instrNo != null) {
				String bk = ma.group(3).replaceFirst("^0+(\\d*)", "$1");
				if (bk.length() != 0) {
					line = new ArrayList<String>();
					line.add(bk);
					line.add(ma.group(4).replaceFirst("^0+(\\d*)", "$1"));
					line.add(instrNo);
					line.add("");
					bodyCR.add(line);
				}
			} else {
				Pattern p1 = Pattern.compile("\\b(\\d+) (\\d+)\\b");
				Matcher ma2 = p1.matcher(ma.group(2));
				while (ma2.find()) {
					String bk = ma2.group(1).replaceFirst("^0+(\\d*)", "$1");
					if (bk.length() != 0) {
						line = new ArrayList<String>();
						line.add(bk);
						line.add(ma2.group(2).replaceFirst("^0+(\\d*)", "$1"));
						line.add("");
						line.add("");
						bodyCR.add(line);
					}
				}
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		p = Pattern.compile("^(\\d+) (\\d+)$");
		ma = p.matcher(legal);
		if (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d*)", "$1"));
			line.add(ma.group(2).replaceFirst("^0+(\\d*)", "$1"));
			line.add("");
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		p = Pattern.compile("(?:^|CLKS? )?(\\d{8,})");
		ma = p.matcher(legal);
		if (ma.find()) {
			line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(1));
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		saveCRInMap(m, bodyCR);
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract and replace section, township and range from legal
		// description
		String sec = "";
		List<List> bodyPIS = getSTRFromMap(m); // first add sec-twn-rng
												// extracted from XML specific
												// tags, if any (for DT use)
		p = Pattern.compile("\\bSEC\\s*(\\d+) (\\d+[SWEN]?) (\\d+[SWEN]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add(ma.group(3));
			bodyPIS.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}
		p = Pattern.compile("\\bSEC(?:TION)?\\s*(\\d+|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add("");
			line.add("");
			bodyPIS.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}
		p = Pattern.compile("\\b(\\d+) (\\d+([SWEN])?) (\\d+([SWEN])?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			if (ma.group(3) != null || ma.group(5) != null) {
				line = new ArrayList<String>();
				line.add(ma.group(1));
				line.add(ma.group(2));
				line.add(ma.group(4));
				bodyPIS.add(line);
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
			}
		}
		p = Pattern.compile("((\\s*\\b\\d+)+)\\s*$");
		ma = p.matcher(legal);
		if (ma.find()) {
			Pattern p2 = Pattern.compile("^\\s*(\\d+) (\\d+) (\\d+)$");
			Matcher ma2 = p2.matcher(ma.group(1));
			if (ma2.find()) {
				line = new ArrayList<String>();
				line.add(ma2.group(1));
				line.add(ma2.group(2));
				line.add(ma2.group(3));
				bodyPIS.add(line);
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
			}
		}
		saveSTRInMap(m, bodyPIS);
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract subdivision name - not requested
	}

	public static void legalRemarksFLDuvalRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerRemarksFLDuvalRV(m, legal);
	}

	public static void legalTokenizerRemarksFLVolusiaRV(ResultMap m, String legal) throws Exception {

		// initial corrections and cleanup of legal description
		legal = legal.replaceAll("#(?=\\s*\\d)", "");
		legal = legal.replaceAll("\\bPT\\b\\s*", "");
		legal = legal.replaceAll("\\b(\\d+)(ST|ND|RD|TH) SEC\\b", "SEC $1");
		legal = legal.replaceAll("\\b(PHS?)([IVX]+)\\b", "$1 $2");

		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);

		// extract and replace lot from legal description
		String lot = "";
		Pattern p = Pattern.compile("\\bLO?TS? (\\d+(?:-?[A-Z](?:-?[A-Z])?|(?:([,-]|\\s*&\\s*|\\s)\\d+)*)|[A-Z]{1,2}\\d*)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(1).replaceAll("[&,]", " ");
			legal = legal.replaceAll("\\b" + ma.group(0) + "\\b", "LOT ");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			String lot2 = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (!StringUtils.isEmpty(lot2)) {
				lot2 = lot2 + " " + lot;
			} else {
				lot2 = lot;
			}
			lot2 = LegalDescription.cleanValues(lot2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace block from legal description
		String block = "";
		p = Pattern.compile("\\bBLK?S? (\\d+|[A-Z](?:[,&-][A-Z])*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1).replaceAll("[&,]", " ");
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLK ");
		}
		block = block.trim();
		if (block.length() != 0) {
			String block2 = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(block2)) {
				block2 = block2 + " " + block;
			} else {
				block2 = block;
			}
			block2 = LegalDescription.cleanValues(block2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace unit from legal description
		String unit = "";
		p = Pattern.compile("^([A-Z]\\d+)(?=.+ CONDO$)");
		ma = p.matcher(legal);
		if (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		p = Pattern.compile("\\bUN(?:IT)?S? (\\d+(?:-?[A-Z]{1,2}|\\s[A-Z])?(?:[,/\\s-]+\\d+)*|[A-Z](?:[\\s-]?\\d+[A-Z]?)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1).replaceAll("[,/]", " ");
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		p = Pattern.compile("\\bUN/WK (\\d+)/\\d+\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			String unit2 = (String) m.get("PropertyIdentificationSet.SubdivisionUnit");
			if (!StringUtils.isEmpty(unit2)) {
				unit2 = unit2 + " " + unit;
			} else {
				unit2 = unit;
			}
			unit2 = LegalDescription.cleanValues(unit2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace building from legal description
		String bldg = "";
		p = Pattern.compile("\\bBLDG (\\d+|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLDG ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace tract from legal description
		String tract = "";
		p = Pattern.compile("\\bTR(?:ACT)? ([A-Z](?:[\\s&]+[A-Z])*|\\d+(?:[,-]\\d+)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(1).replaceAll("[,&]", " ");
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "TRACT ");
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace phase from legal description
		String phase = "";
		p = Pattern.compile("\\bPH(?:ASE)?S?\\s*((?:\\d+|I)(?:[-\\s]?[A-Z])?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = ma.group(1).replaceFirst("\\bI\\b", "1");
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PHASE ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and remove cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		List<String> line;
		p = Pattern.compile("\\b(O\\s?R|DB|DEED)(?: BK)?\\s?(\\d+)(?:\\s*\\?)? PG\\s?(\\d+(?:-\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(2).replaceFirst("^0+(\\d*)", "$1"));
			line.add(ma.group(3).replaceFirst("^0+(\\d*)", "$1"));
			line.add(ma.group(1).replaceAll(" ", ""));
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		saveCRInMap(m, bodyCR);
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract and replace section, township and range from legal
		// description
		String sec = "";
		List<List> bodyPIS = getSTRFromMap(m); // first add sec-twn-rng
												// extracted from XML specific
												// tags, if any (for DT use)
		p = Pattern.compile("\\bSEC (\\d+(?:\\s*[&,]\\s*\\d+)*)(?:[\\s-](\\d+)(?:[\\s-](\\d+))?)?");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			sec = ma.group(1).replaceAll("\\s*[&,]\\s*", " ");
			String twn = ma.group(2);
			if (twn == null)
				twn = "";
			String rng = ma.group(3);
			if (rng == null)
				rng = "";
			line.add(sec);
			line.add(twn);
			line.add(rng);
			bodyPIS.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}
		p = Pattern.compile("\\bSEC ([A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add("");
			line.add("");
			bodyPIS.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}
		p = Pattern.compile("\\bSHARE (\\d+) TWP (\\d+) (?:RNG|RANGE) (\\d+(?: & \\d+)?)");
		ma = p.matcher(legal);
		if (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add(ma.group(3).replaceAll("\\s*&\\s*", " "));
			bodyPIS.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}
		saveSTRInMap(m, bodyPIS);
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract subdivision name - not requested
	}

	public static void legalRemarksFLVolusiaRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerRemarksFLVolusiaRV(m, legal);
	}

	public static void legalTokenizerRemarksFLNassauRV(ResultMap m, String legal) throws Exception {

		// initial corrections and cleanup of legal description
		legal = legal.replaceAll("\\bUNNIT\\b", "UNIT");
		legal = legal.replaceAll("\\bEC(?= \\d+ TWN \\d+)", "SEC");
		legal = legal.replaceAll("\\b(TWN \\d+ [SWEN])\\s*R (\\d+)", "$1 RGE $2");
		legal = legal.replaceAll("\\bPT( OF)?\\b", "");
		legal = legal.replaceAll("\\bPART\\b", "");
		legal = legal.replaceAll("\\bCORR(ECTION)?\\b", "");
		legal = legal.replaceAll("\\b([SWEN]{1,2}|NORTH|SOUTH|WEST|EAST)\\s*\\d+([\\./]\\d+)?\\b", "");

		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);
		legal = replaceNumbers(legal);

		// extract and replace unit from legal description
		String unit = "";
		Pattern p = Pattern.compile("\\bUNIT (\\d+(?:-[A-Z])?|[A-Z](?:[\\s-]?\\d+)?)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "UNIT ");
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			String unit2 = (String) m.get("PropertyIdentificationSet.SubdivisionUnit");
			if (!StringUtils.isEmpty(unit2)) {
				unit2 = unit2 + " " + unit;
			} else {
				unit2 = unit;
			}
			unit2 = LegalDescription.cleanValues(unit2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace building from legal description
		String bldg = "";
		p = Pattern.compile("\\bBLDG (\\d+[A-Z]?|[A-Z]\\d*)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			bldg = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLDG ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace lot from legal description
		String lot = "";
		p = Pattern.compile("\\b(?:SUB)?LOTS? (\\d+(?:-?[A-Z])?(?:\\s*[-&\\s]\\s*\\d+)*|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(1).replaceAll("\\s*&\\s*", " ");
			legal = legal.replace(ma.group(0), "LOT ");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			String lot2 = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (!StringUtils.isEmpty(lot2)) {
				lot2 = lot2 + " " + lot;
			} else {
				lot2 = lot;
			}
			lot2 = LegalDescription.cleanValues(lot2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace block from legal description
		String block = "";
		p = Pattern.compile("\\bBL(?:OC)?K\\s*(\\d+|\\b[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "BLK ");
		}
		block = block.trim();
		if (block.length() != 0) {
			String block2 = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(block2)) {
				block2 = block2 + " " + block;
			} else {
				block2 = block;
			}
			block2 = LegalDescription.cleanValues(block2, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block2);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace tract from legal description
		String tract = "";
		p = Pattern.compile("\\bTRACTS? ((?:\\d+|[A-Z])(?:\\s*[-&\\s]\\s*\\d+)*)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "TRACT ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace phase from legal description
		String phase = "";
		p = Pattern.compile("\\bPH(?:ASE)? (\\d+(?:-?[A-Z])?|I)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = ma.group(1);
			phase = phase.replaceFirst("^I$", "1");
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PHASE ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and remove cross refs from legal description
		p = Pattern.compile("\\bOR (?:BK )?(\\d+) (?:PG|PAGE) (\\d+(?:-\\d+)?)\\b");
		ma = p.matcher(legal);
		List<List> bodyCR = new ArrayList<List>();
		List<String> line;
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(2).replaceFirst("^0+(\\d+)", "$1"));
			line.add("");
			line.add("");
			bodyCR.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
		}
		saveCRInMap(m, bodyCR);
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract and replace section, township and range from legal
		// description
		List<List> bodyPIS = getSTRFromMap(m); // first add sec-twn-rng
												// extracted from XML specific
												// tags, if any (for DT use)
		String secPatt = "SEC(?:TION)? (\\d+(?:\\s*&\\s*\\d+)*)";
		String twnPatt = "(?:TW?N|TW) (\\d+(?:\\s*[SWEN])?)";
		String rngPatt = "(?:RGE|RANGE|REG)\\s*(\\d+(?:\\s*[SWEN])?(?:\\s*&\\s*\\d+(?:\\s*[SWEN])?)*)";
		p = Pattern.compile("\\b" + secPatt + "(?: " + twnPatt + "(?: " + rngPatt + ")?)?\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			String sec = ma.group(1);
			sec = sec.replaceAll("\\s*&\\s*", " ");
			line.add(sec);
			String twn = ma.group(2);
			if (twn == null)
				twn = "";
			twn = twn.replaceAll(" (?=[SWEN])", "");
			line.add(twn);
			String rng = ma.group(3);
			if (rng == null)
				rng = "";
			rng = rng.replaceAll("\\s*&\\s*", " ");
			rng = rng.replaceAll(" (?=[SWEN])", "");
			line.add(rng);
			bodyPIS.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC@ ").replaceAll("\\s{2,}", " ");
		}
		p = Pattern.compile("\\bSEC(?:TION)? (\\d+(?:\\s*&\\s*\\d+)*|[A-Z])\\b(?! (TW?N|TW))");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add("");
			line.add("");
			bodyPIS.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
		}
		saveSTRInMap(m, bodyPIS);
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract and replace plat b&p from legal description
		List<List> bodyPlat = new ArrayList<List>();
		p = Pattern.compile("\\bPLAT BOOK (\\d+) PAGES? (\\d+(?:-\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d+)", "$1"));
			line.add(ma.group(2).replaceFirst("^0+(\\d+)", "$1"));
			bodyPlat.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PB ");
		}
		if (!bodyPlat.isEmpty()) {
			String[] header = { "PlatBook", "PlatNo" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("PlatBook", new String[] { "PlatBook", "" });
			map.put("PlatNo", new String[] { "PlatNo", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(bodyPlat);

			ResultTable pisSTR = (ResultTable) m.get("PropertyIdentificationSet");
			if (pisSTR != null) {
				pis = ResultTable.joinHorizontal(pis, pisSTR);
				map.putAll(pisSTR.map);
			}
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
		}

		// extract subdivision name - not requested
	}

	public static void legalRemarksFLNassauRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerRemarksFLNassauRV(m, legal);
	}

	public static void legalRemarksGenericDASLRV(ResultMap m, long searchId) throws Exception {

		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();

		if ("FL".equals(crtState)) {
			if ("Orange".equals(crtCounty)) {
				legalRemarksFLOrangeRV(m, searchId);
			} else if ("Walton".equals(crtCounty)) {
				legalRemarksFLWaltonRV(m, searchId);
			} else if ("Pinellas".equals(crtCounty)) {
				legalRemarksFLPinellasRV(m, searchId);
			} else if ("Broward".equals(crtCounty)) {
				legalRemarksFLBrowardRV(m, searchId);
			} else if ("Palm Beach".equals(crtCounty)) {
				legalRemarksFLPalmBeachRV(m, searchId);
			} else if ("Seminole".equals(crtCounty)) {
				legalRemarksFLSeminoleRV(m, searchId);
			} else if ("Brevard".equals(crtCounty)) {
				legalRemarksFLBrevardRV(m, searchId);
			} else if ("Duval".equals(crtCounty)) {
				legalRemarksFLDuvalRV(m, searchId);
			} else if ("Nassau".equals(crtCounty)) {
				legalRemarksFLNassauRV(m, searchId);
			} else if ("Charlotte".equals(crtCounty)) {
				legalRemarksFLCharlotteRV(m, searchId);
			} else if ("Volusia".equals(crtCounty)) {
				legalRemarksFLVolusiaRV(m, searchId);
			} else if ("Sarasota".equals(crtCounty)) {
				legalRemarksFLSarasotaRV(m, searchId);
			} else if ("St. Johns".equals(crtCounty)) {
				legalRemarksFLStJohnsRV(m, searchId);
			} else if ("Escambia".equals(crtCounty)) {
				legalRemarksFLEscambiaRV(m, searchId);
			} else if ("Indian River".equals(crtCounty)) {
				legalRemarksFLIndianRiverRV(m, searchId);
			} else if ("Okaloosa".equals(crtCounty)) {
				legalFLOkaloosaDASLRV(m, searchId, true);
			} else if ("Lee".equals(crtCounty)) {
				legalRemarksFLLeeRV(m, searchId);
			} else if ("Bay".equals(crtCounty)) {
				legalRemarksFLBayRV(m, searchId);
			} else if ("Franklin".equals(crtCounty)) {
				legalRemarksFLFranklinRV(m, searchId);
			} else if ("Polk".equals(crtCounty)) {
				legalRemarksFLPolkRV(m, searchId);
			} else if ("Santa Rosa".equals(crtCounty)) {
				legalRemarksFLSantaRosaRV(m, searchId);
			} else if ("Lake".equals(crtCounty)) {
				legalRemarksFLLakeRV(m, searchId);
			} else if ("Osceola".equals(crtCounty)) {
				legalRemarksFLOsceolaRV(m, searchId);
			} else if ("Pasco".equals(crtCounty)) {
				legalRemarksFLPascoRV(m, searchId);
			} else if ("Collier".equals(crtCounty)) {
				FLCollierRV.legalRemarksFLCollierRV(m, searchId);
			} else if ("Hernando".equals(crtCounty)) {
				legalRemarksFLHernandoRV(m, searchId);
			} else if ("Sumter".equals(crtCounty)) {
				legalRemarksFLSumterRV(m, searchId);
			} else if ("Jackson".equals(crtCounty)) {
				// I have to parse is after legal parsing
				FLJacksonRV.legalRemarksFLJacksonRV(m, searchId);
			} else if ("Alachua".equals(crtCounty)) {
				legalRemarksFLAlachuaRV(m, searchId);
			} else if ("Okeechobee".equals(crtCounty)) {
				FLOkeechobeeRV.legalRemarksFLOkeechobeeRV(m, searchId);
				// the remarks will be parsed after the legal description is
				// parsed
			} else if ("Levy".equals(crtCounty)) {
				FLLevyTR.parseLegalDescription(m);
			} else if ("Martin".equals(crtCounty)) {
				legalFLMartinTR(m, searchId);
			} else if ("Hendry".equals(crtCounty)) {
				FLHendryTR.parseLegalFLHendryTR(m, searchId);
			} else {
				legalFLHillsboroughRO(m, searchId);
			}
		}
	}

	public static void nameParsingFLRV(ResultMap m, long searchId) throws Exception {
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
		if ("FL".equals(crtState)) {
			FLRVnames.nameParseFLRV(m, searchId);
		}
	}

	public static List<List> goToGetSTRFromMap(ResultMap m) throws Exception {
		List<List> body = new ArrayList<List>();
		String secFromSet = (String) m.get("tmpSubdivisionSection");
		String twnFromSet = (String) m.get("tmpSubdivisionTownship");
		String rngFromSet = (String) m.get("tmpSubdivisionRange");
		if (!StringUtils.isEmpty(secFromSet) || !StringUtils.isEmpty(twnFromSet) || !StringUtils.isEmpty(rngFromSet)) {
			body = getSTRFromMap(m);
		}
		return body;
	}

	public static List<List> goToGetSTRFromMapFromPIS(ResultMap m, List<List> body) {
		ResultTable pis = (ResultTable) m.get("PropertyIdentificationSet");
		if (pis != null) {
			String[][] bodyRef = pis.getBodyRef();
			for (int i = 0; i < bodyRef.length; i++) {
				List<String> line = new ArrayList<String>();
				line.add(bodyRef[i][0]);
				line.add(bodyRef[i][1]);
				line.add(bodyRef[i][2]);
				body.add(line);
			}
		}
		return body;
	}

	public static List<List> getSTRFromMap(ResultMap m) throws Exception {
		List<List> body = new ArrayList<List>();
		String secFromSet = (String) m.get("tmpSubdivisionSection");
		if (secFromSet == null)
			secFromSet = "";
		String twnFromSet = (String) m.get("tmpSubdivisionTownship");
		if (twnFromSet == null)
			twnFromSet = "";
		String rngFromSet = (String) m.get("tmpSubdivisionRange");
		if (rngFromSet == null)
			rngFromSet = "";
		if (!StringUtils.isEmpty(secFromSet) || !StringUtils.isEmpty(twnFromSet) || !StringUtils.isEmpty(rngFromSet)) {
			List<String> line = new ArrayList<String>();
			line.add(secFromSet);
			line.add(twnFromSet);
			line.add(rngFromSet);
			body.add(line);
		}
		return body;
	}

	public static void goToSaveSTRInMap(ResultMap m, List<List> body) throws Exception {
		if (!body.isEmpty()) {
			saveSTRInMap(m, body);
		}
	}

	public static void saveSTRInMap(ResultMap m, List<List> body) throws Exception {
		if (!body.isEmpty()) {
			String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
			map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
			map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(body);
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
		}
	}

	public static void saveSTRQInMap(ResultMap m, List<List> body) throws Exception {
		if (!body.isEmpty()) {
			String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange", "QuarterValue" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
			map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
			map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });
			map.put("QuarterValue", new String[] { "QuarterValue", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(body);
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
		}
	}

	/**
	 * Takes an existing infSet from resultMap m and appends the values from
	 * body to it.<bR>
	 * It works only for result tables with no common column<br>
	 * If infSet does not exist in resultMap we create a new one.<br>
	 * If infSet is already in resultMap we will modify its headers to contain
	 * all the colums from mapping
	 * 
	 * @param m
	 *            - The resultMap containing the result table
	 * @param body
	 *            - the values to be appended to result table
	 * @param header
	 *            - The header associated with body
	 * @param infSet
	 *            - The result table to which we add
	 */
	public static void joinResultTableInMap(ResultMap m, List<List> body, String[] header, String infSet) throws Exception {
		if (!body.isEmpty()) {
			ResultTable is = (ResultTable) m.get(infSet);
			if (is == null) {
				is = new ResultTable();
				is.setHead(new String[] {});
				is.setMap(ResultTable.createMapFromHead(new String[] {}));
				is.setBody(new ArrayList<List>());

			}
			ResultTable newIS = createResultTable(body, header);
			m.put(infSet, is.joinHorizontalWithMap(is, newIS));
		}
	}

	/**
	 * Creates a new resultTable with the given body and header
	 * 
	 * @param body
	 *            - body of the result table
	 * @param header
	 *            - header of the result table
	 * @return new result table
	 * 
	 */
	public static ResultTable createResultTable(List<List> body, String[] header) {
		ResultTable is = new ResultTable();
		if (body.size() > 0 && body.get(0).size() != header.length) {
			return null;
		}

		try {
			is.setHead(header);
			is.setBody(body);
			is.setMap(ResultTable.createMapFromHead(header));
		} catch (Exception e) {
			return null;
		}
		return is;

	}

	public static void goToSaveCRInMap(ResultMap m, List<List> bodyCR) throws Exception {
		if (!bodyCR.isEmpty()) {
			saveCRInMap(m, bodyCR);
		}
	}

	public static void saveCRInMap(ResultMap m, List<List> bodyCR) throws Exception {
		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "InstrumentNumber", "Book_Page_Type" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			map.put("Book_Page_Type", new String[] { "Book_Page_Type", "" });

			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);

			// add also the cross ref b&p extracted from XML specific tags, if
			// any (if tokenizer is used also on DT)
			ResultTable crFromSet = (ResultTable) m.get("CrossRefSet");
			if (crFromSet != null && crFromSet.body.length >= 1) {
				cr = ResultTable.joinVertical(cr, crFromSet, true);
			}
			m.put("CrossRefSet", cr);
		}
	}

	public static void legalFLHillsboroughRO(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;
		legalTokenizerFLHillsboroughRO(m, legal);
	}

	public static void legalTokenizerFLHillsboroughRO(ResultMap m, String legal) throws Exception {

		// if legal descr contains an address, then don't attempt to extract any
		// legal elements
		if (legal.matches(".+ FLA? [\\d-]+"))
			return;

		// initial corrections and cleanup of legal description
		legal = replaceNumbers(legal);
		legal = legal.replaceAll("\\bORDN #?[\\d-]+\\s*", "");
		legal = legal.replaceAll("(\\d)(ETAL)\\b", "$1 $2");
		legal = legal.replaceAll("(\\d+)(?:ST|ND|RD|TH) (SEC(?:TION)?|UNIT)\\b", "$2 $1");
		legal = legal.replaceAll("\\bUIT (\\d)", "UNIT $1");
		legal = legal.replaceAll("\\bL(\\d)", "L $1");
		legal = legal.replaceAll("\\s+THRU\\s+", "-");
		legal = legal.replaceAll("(\\d+[A-Z]?|\\b[A-Z])\\s+TO\\s+", "$1-");

		legal = legal.replaceAll("\\b(\\s*)(GOVT?|CORR?(ECTION|ECTIVE)?|ETAL|REV|(RE)?SUB(D(IVISION)?)?|ETC|AMEND|SUBORD|&\\s+SA|MTG)\\b\\s*", "$1");
		legal = legal.replaceAll("\\b(\\s*)(AS )?RENTS & LEASES\\b\\s*", "$1");
		legal = legal.replaceAll("\\b(\\s*)(AS )?RTS & LE\\s*", "$1");
		legal = legal.replaceAll("\\b(\\s*)(REFER TO THE INSTRUMENT|PAT ACRES|MULTIPLE LEGALS)\\b\\s*", "$1");
		legal = legal.replaceAll("^RE .*$", "");
		legal = legal.replaceAll("^RECEIPT .*$", "");
		legal = legal.replaceAll("^DISMISSAL .*$", "");

		legal = legal.replaceAll("\\s+PT (\\d+)\\b", ",$1");
		legal = legal.replaceAll(",\\s+(\\d+|[A-Z])\\b", ",$1");
		legal = legal.replaceAll("\\bL(\\d)", "L $1");
		legal = legal.replaceAll("\\b(\\d+) (\\d+)\\b", "$1,$2");
		legal = legal.replaceAll("\\b(\\d+) (\\d+)\\b", "$1,$2");
		legal = legal.replaceAll("\\b(\\s*)(PTE/)?PTS?\\b\\s*", "$1");

		legal = legal.trim();
		legal = legal.replaceAll("\\s{2,}", " ");

		String legalTemp = legal;

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		Pattern p = Pattern.compile("(\\b(?:L|LOT)\\s((\\d+|[A-Z]\\b)([A-Z]\\b|,|\\d+|-)*))");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			String lotFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (!StringUtils.isEmpty(lotFromSet)) {
				lot = lotFromSet + " " + lot;
			}
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract tract from legal description
		String tract = ""; // can have multiple occurrences
		p = Pattern.compile("(\\bTR(?:ACT)? ((\\b(\\d+|\\d+[A-Z]|[A-Z]\\d+|[A-Z]|,|-)\\b)+))");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(2);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract block from legal description
		String block = "";
		String pat = "\\b([A-Z]|\\d+[A-Z]?)\\b";
		p = Pattern.compile("(\\b(?:B|BLK) (" + pat + "([,-]" + pat + ")*))");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			block = ma.group(2).replaceAll("\\s*,\\s*", " ");
			String blockFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(blockFromSet)) {
				block = blockFromSet + " " + block;
				block = LegalDescription.cleanValues(block, false, true);
			}
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract section, township and range from legal description
		List<List> body = getSTRFromMap(m); // first add sec-twn-rng extracted
											// from XML specific tags, if any
											// (for DT use)

		p = Pattern.compile("(\\bS(\\d+(?:[,-]\\d+)?) T(\\d+) R(\\d+)\\b)");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			String sec = ma.group(2).replaceAll("\\s*,\\s*", " ");
			String twn = ma.group(3);
			String rng = ma.group(4);
			line.add(sec);
			line.add(twn);
			line.add(rng);
			body.add(line);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract section from legal description
		p = Pattern.compile("(\\bSEC (\\d+|[A-Z]{1,2})\\b)");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add("");
			line.add("");
			body.add(line);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		saveSTRInMap(m, body);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract plat book & page from legal description for Miami-Dade
		p = Pattern.compile("\\bPB (\\d+) PP (\\d+)(:?0)?\\b");
		ma.usePattern(p);
		ma.reset();
		if (ma.find()) {
			m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
			m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
		} else {
			p = Pattern.compile("\\b(?:PB) (\\d+)-(\\d+)(?:0)?\\b");
			ma.usePattern(p);
			ma.reset();
			if (ma.find()) {
				m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
				m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
			}
		}

		// extract cross refs from legal description
		p = Pattern.compile("(\\bOR BK (\\d+) PG (\\d+)\\b)");
		ma = p.matcher(legal);
		List<List> bodyCR = new ArrayList<List>();

		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add(ma.group(3));
			line.add("");
			line.add("O");
			bodyCR.add(line);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}

		// extract cross refs from legal description for Miami-Dade
		p = Pattern.compile("(\\bORB (\\d+)/(\\d+)\\b)");
		ma = p.matcher(legal);

		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add(ma.group(3));
			line.add("");
			line.add("O");
			bodyCR.add(line);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		saveCRInMap(m, bodyCR);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		String tokens[] = { "X", "L", "C", "D", "M" };
		// extract phase from legal description
		pat = "\\b(\\d+[A-Z]?|[IVX]+)\\b";
		p = Pattern.compile("(\\bPH(?:ASE)? (" + pat + "([,-]" + pat + ")*))");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionPhase", Roman.normalizeRomanNumbersExceptTokens(ma.group(2), tokens).replaceAll("\\s*,\\s*", " "));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract unit from legal description
		p = Pattern.compile("((?:#|UNIT #?)(\\w+([-,]\\w+)*))(?! ADD)");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionUnit", Roman.normalizeRomanNumbersExceptTokens(ma.group(2), tokens).replaceAll("\\s*,\\s*", " "));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract building #
		p = Pattern.compile("(\\bBLDG #?(\\w+)\\b)");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// additional cleaning before extracting subdivision name
		legal = legal.replaceAll("^REPL(A?T)?\\b\\s*", "");
		legal = legal.replaceAll("^\\s*(\\d+(ST|ND|RD|TH) )?ADD(ITIO)?N?( TO)?\\b\\s*", "");
		legal = legal.replaceAll("^\\s*SUBD?\\b\\s*", "");

		// extract subdivision name
		String subdiv = legal.replaceFirst("(.*?)\\s*\\b(SUBD?|REPL(A?T)?|SEC|CONDO|(\\d+(ST|ND|RD|TH)? )?ADD(ITIO)?N?|PH(ASE)?|PAR(CEL)?)\\b.*", "$1");
		subdiv = subdiv.replaceFirst("\\s*#$", "");
		subdiv = subdiv.replaceFirst("^-\\s*", "");
		subdiv = subdiv.replaceFirst("^\\s*\\d+\\s*$", "");
		subdiv = subdiv.replaceFirst("([^;]+;\\s*)(.+)", "$2");
		subdiv = subdiv.replaceFirst(".*\\bCASE #.*", "");
		subdiv = subdiv.replaceFirst(".*\\b\\w+-\\w+-\\w+\\b.*", "");

		if (subdiv.length() != 0) {
			m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
				m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
		}
	}

	public static void legalRemarksFLCharlotteRV(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		// if legal descr contains an address, then don't attempt to extract any
		// legal elements
		if (legal.matches(".+ FLA? [\\d-]+"))
			return;

		// initial corrections and cleanup of legal description
		legal = replaceNumbers(legal);
		legal = legal.replaceAll("\\bORDN #?[\\d-]+\\s*", "");
		legal = legal.replaceAll("(\\d)(ETAL)\\b", "$1 $2");
		legal = legal.replaceAll("(\\d+)(?:ST|ND|RD|TH) (SEC(?:TION)?|UNIT)\\b", "$2 $1");
		legal = legal.replaceAll("\\bUIT (\\d)", "UNIT $1");
		legal = legal.replaceAll("\\bL(\\d)", "L $1");
		legal = legal.replaceAll("\\s+THRU\\s+", "-");
		legal = legal.replaceAll("(\\d+[A-Z]?|\\b[A-Z])\\s+TO\\s+", "$1-");

		legal = legal.replaceAll("\\b(\\s*)(GOVT?|CORR?(ECTION|ECTIVE)?|ETAL|REV|(RE)?SUB(D(IVISION)?)?|ETC|AMEND|SUBORD|&\\s+SA|MTG)\\b\\s*", "$1");
		legal = legal.replaceAll("\\b(\\s*)(AS )?RENTS & LEASES\\b\\s*", "$1");
		legal = legal.replaceAll("\\b(\\s*)(AS )?RTS & LE\\s*", "$1");
		legal = legal.replaceAll("\\b(\\s*)(REFER TO THE INSTRUMENT|PAT ACRES|MULTIPLE LEGALS)\\b\\s*", "$1");
		legal = legal.replaceAll("^RE .*$", "");
		legal = legal.replaceAll("^RECEIPT .*$", "");
		legal = legal.replaceAll("^DISMISSAL .*$", "");

		legal = legal.replaceAll("\\s+PT (\\d+)\\b", ",$1");
		legal = legal.replaceAll(",\\s+(\\d+|[A-Z])\\b", ",$1");
		legal = legal.replaceAll("\\bL(\\d)", "L $1");
		legal = legal.replaceAll("\\b(\\d+) (\\d+)\\b", "$1,$2");
		legal = legal.replaceAll("\\b(\\d+) (\\d+)\\b", "$1,$2");
		legal = legal.replaceAll("\\b(\\s*)(PTE/)?PTS?\\b\\s*", "$1");
		legal = legal.replaceAll(" BK ", " B ");
		legal = legal.replaceAll("BLK-", "B ");
		legal = legal.replaceAll("BLK", "B");
		legal = legal.replaceAll("PH", "PH ");
		legal = legal.replaceAll("SEC ", "S");
		legal = legal.replaceAll("TWP ", "T");
		legal = legal.replaceAll("RG ", "R");

		legal = legal.trim();
		legal = legal.replaceAll("\\s{2,}", " ");

		String legalTemp = legal;

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		Pattern p = Pattern.compile("\\b(L|LT|LOT)(?:S)?\\s*([\\d&\\s]+\\s+|(?:\\d+-[A-Z]))");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			String lotFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (!StringUtils.isEmpty(lotFromSet)) {
				lot = lotFromSet + " " + lot;
			}
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract tract from legal description
		String tract = ""; // can have multiple occurrences
		p = Pattern.compile("(\\bTR(?:ACT)? ((\\b(\\d+|\\d+[A-Z]|[A-Z]\\d+|[A-Z]|,|-)\\b)+))");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(2);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract block from legal description
		String block = "";
		String pat = "([A-Z]|\\d+[A-Z]?)\\b";
		p = Pattern.compile("(\\b(?:B)\\s*(" + pat + "([,-]" + pat + ")*))");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			block = ma.group(2).replaceAll("\\s*,\\s*", " ");
			String blockFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(blockFromSet)) {
				block = blockFromSet + " " + block;
				block = LegalDescription.cleanValues(block, false, true);
			}
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract section, township and range from legal description
		List<List> body = getSTRFromMap(m); // first add sec-twn-rng extracted
											// from XML specific tags, if any
											// (for DT use)
		p = Pattern.compile("(\\bS(\\d+(?:[,-]\\d+)?) T([A-Z\\d]+) R([A-Z\\d]+)\\b)");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			String sec = ma.group(2).replaceAll("\\s*,\\s*", " ");
			String twn = ma.group(3);
			String rng = ma.group(4);
			line.add(sec);
			line.add(twn);
			line.add(rng);
			body.add(line);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract section from legal description
		p = Pattern.compile("(\\b(?:S|SEC)\\s*(\\d+|[A-Z]{1,2})\\b)");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add("");
			line.add("");
			body.add(line);
		}
		saveSTRInMap(m, body);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract cross refs from legal description
		p = Pattern.compile("(\\bOR\\s*(\\d+)\\s*PG\\s*(\\d+)\\b)");
		ma = p.matcher(legal);
		List<List> bodyCR = new ArrayList<List>();

		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			if (ma.group(2) == null)
				line.add("");
			else
				line.add(ma.group(2));
			if (ma.group(3) == null)
				line.add("");
			else
				line.add(ma.group(3));
			line.add("");
			line.add("O");
			bodyCR.add(line);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}

		p = Pattern.compile("(\\b(\\d+)?\\s*OR (\\d+)/(\\d+)\\b)");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			if (ma.group(3) == null)
				line.add("");
			else
				line.add(ma.group(3));
			if (ma.group(4) == null)
				line.add("");
			else
				line.add(ma.group(4));
			if (ma.group(2) == null)
				line.add("");
			else
				line.add(ma.group(2));
			line.add("O");
			bodyCR.add(line);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		saveCRInMap(m, bodyCR);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		String tokens[] = { "X", "L", "C", "D", "M" };
		// extract phase from legal description
		pat = "\\b(\\d+[A-Z]?|[IVX]+)\\b";
		p = Pattern.compile("(\\bPH(?:ASE)? (" + pat + "([,-]" + pat + ")*))");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionPhase", Roman.normalizeRomanNumbersExceptTokens(ma.group(2), tokens).replaceAll("\\s*,\\s*", " "));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract unit from legal description
		p = Pattern.compile("((?:#|UNIT #?)(\\w+([-,]\\w+)*))(?! ADD)");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionUnit", Roman.normalizeRomanNumbersExceptTokens(ma.group(2), tokens).replaceAll("\\s*,\\s*", " "));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract building #
		p = Pattern.compile("(\\bBLDG #?(\\w+)\\b)");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// additional cleaning before extracting subdivision name
		legal = legal.replaceAll("^REPL(A?T)?\\b\\s*", "");
		legal = legal.replaceAll("\\s*(\\d+(ST|ND|RD|TH) )?ADD(ITIO)?N?( TO)?\\b\\s*", "");
		legal = legal.replaceAll("^\\s*SUBD?\\b\\s*", "");

		// extract subdivision name
		// String subdiv =
		// legal.replaceFirst("(.*?)\\s*\\b(SUBD?|REPL(A?T)?|SEC|CONDO|(\\d+(ST|ND|RD|TH)? )?ADD(ITIO)?N?|PH(ASE)?|PAR(CEL)?)\\b.*",
		// "$1");
		// subdiv = subdiv.replaceFirst("\\s*#$", "");
		// subdiv = subdiv.replaceFirst("^-\\s*", "");
		// subdiv = subdiv.replaceFirst("^\\s*\\d+\\s*$", "");
		//
		// if (subdiv.length() != 0){
		// m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
		// if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
		// m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
		// }
	}

	public static void legalRemarksFLSarasotaRV(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		// if legal descr contains an address, then don't attempt to extract any
		// legal elements
		if (legal.matches(".+ FLA? [\\d-]+"))
			return;

		// initial corrections and cleanup of legal description
		legal = replaceNumbers(legal);
		legal = legal.replaceAll("\\bORDN #?[\\d-]+\\s*", "");
		legal = legal.replaceAll("(\\d)(ETAL)\\b", "$1 $2");
		legal = legal.replaceAll("(\\d+)(?:ST|ND|RD|TH) (SEC(?:TION)?|UNIT)\\b", "$2 $1");
		legal = legal.replaceAll("\\bUIT (\\d)", "UNIT $1");
		legal = legal.replaceAll("\\bL(\\d)", "L $1");
		legal = legal.replaceAll("\\s+THRU\\s+", "-");
		legal = legal.replaceAll("(\\d+[A-Z]?|\\b[A-Z])\\s+TO\\s+", "$1-");
		legal = legal.replaceAll("\\s*\\+\\s*", " ");

		legal = legal.replaceAll("\\b(\\s*)(GOVT?|CORR?(ECTION|ECTIVE)?|ETAL|REV|(RE)?SUB(D(IVISION)?)?|ETC|AMEND|SUBORD|&\\s+SA|MTG)\\b\\s*", "$1");
		legal = legal.replaceAll("\\b(\\s*)(AS )?RENTS & LEASES\\b\\s*", "$1");
		legal = legal.replaceAll("\\b(\\s*)(AS )?RTS & LE\\s*", "$1");
		legal = legal.replaceAll("\\b(\\s*)(REFER TO THE INSTRUMENT|PAT ACRES|MULTIPLE LEGALS)\\b\\s*", "$1");
		legal = legal.replaceAll("^RE .*$", "");
		legal = legal.replaceAll("^RECEIPT .*$", "");
		legal = legal.replaceAll("^DISMISSAL .*$", "");

		legal = legal.replaceAll("\\s+PT (\\d+)\\b", ",$1");
		legal = legal.replaceAll(",\\s+(\\d+|[A-Z])\\b", ",$1");
		legal = legal.replaceAll("\\bL(\\d)", "L $1");
		legal = legal.replaceAll("\\b(\\d+) (\\d+)\\b", "$1,$2");
		legal = legal.replaceAll("\\b(\\d+) (\\d+)\\b", "$1,$2");
		legal = legal.replaceAll("\\b(\\s*)(PTE/)?PTS?\\b\\s*", "$1");

		legal = legal.trim();
		legal = legal.replaceAll("\\s{2,}", " ");

		String legalTemp = legal;

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		Pattern p = Pattern.compile("(\\b(?:LT) ([\\d\\s&-]+|\\d+[A-Z])\\b)");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			String lotFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (!StringUtils.isEmpty(lotFromSet)) {
				lot = lotFromSet + " " + lot;
			}
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract tract from legal description
		String tract = ""; // can have multiple occurrences
		p = Pattern.compile("(\\bTR(?:ACT)? ((\\b(\\d+|\\d+[A-Z]|[A-Z]\\d+|[A-Z]|,|-)\\b)+))");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(2);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract block from legal description
		String block = "";
		String pat = "\\b([A-Z]|\\d+[A-Z]?)\\b";
		p = Pattern.compile("(\\b(?:BL) (" + pat + "([,-]" + pat + ")*))");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			block = ma.group(2).replaceAll("\\s*,\\s*", " ");
			String blockFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(blockFromSet)) {
				block = blockFromSet + " " + block;
				block = LegalDescription.cleanValues(block, false, true);
			}
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract section from legal description
		List<List> bodyPIS = getSTRFromMap(m); // first add sec-twn-rng
												// extracted from XML specific
												// tags, if any (for DT use)
		p = Pattern.compile("(\\bSC (\\d+|[A-Z]{1,2})\\b)");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add("");
			line.add("");
			bodyPIS.add(line);
		}
		saveSTRInMap(m, bodyPIS);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract cross refs from legal description
		p = Pattern.compile("(\\bOR (\\d+) PG (\\d+)\\b)");
		ma = p.matcher(legal);
		List<List> bodyCR = new ArrayList<List>();

		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add(ma.group(3));
			line.add("");
			line.add("OR");
			bodyCR.add(line);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		p = Pattern.compile("\\b(\\d{10})\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(1));
			line.add("");
			bodyCR.add(line);
		}
		saveCRInMap(m, bodyCR);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		String tokens[] = { "X", "L", "C", "D", "M" };
		// extract phase from legal description
		pat = "\\b(\\d+[A-Z]?|[IVX]+)\\b";
		p = Pattern.compile("(\\bPH(?:ASE)? (" + pat + "([,-]" + pat + ")*))");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionPhase", Roman.normalizeRomanNumbersExceptTokens(ma.group(2), tokens).replaceAll("\\s*,\\s*", " "));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract unit from legal description
		p = Pattern.compile("((?:#|UN #?)(\\w+([-,]\\w+)*))(?! ADD)");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionUnit", Roman.normalizeRomanNumbersExceptTokens(ma.group(2), tokens).replaceAll("\\s*,\\s*", " "));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract building #
		p = Pattern.compile("(\\bBD #?(\\w+)\\b)");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// additional cleaning before extracting subdivision name
		legal = legal.replaceAll("^REPL(A?T)?\\b\\s*", "");
		legal = legal.replaceAll("^\\s*(\\d+(ST|ND|RD|TH) )?ADD(ITIO)?N?( TO)?\\b\\s*", "");
		legal = legal.replaceAll("^\\s*SUBD?\\b\\s*", "");

		// extract subdivision name
		// String subdiv =
		// legal.replaceFirst("(.*?)\\s*\\b(SUBD?|REPL(A?T)?|SEC|CONDO|(\\d+(ST|ND|RD|TH)? )?ADD(ITIO)?N?|PH(ASE)?|PAR(CEL)?)\\b.*",
		// "$1");
		// subdiv = subdiv.replaceFirst("\\s*#$", "");
		// subdiv = subdiv.replaceFirst("^-\\s*", "");
		// subdiv = subdiv.replaceFirst("^\\s*\\d+\\s*$", "");
		// subdiv = subdiv.replaceFirst("([^;]+;\\s*)(.+)", "$2");
		// subdiv = subdiv.replaceFirst("(.+) (\\d+)", "$1");
		// subdiv = subdiv.replaceFirst("(^[A-Z]) (.+)", "$2");
		//
		// if (subdiv.length() != 0){
		// m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
		// if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
		// m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
		// }
	}

	public static void legalRemarksFLStJohnsRV(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		// if legal descr contains an address, then don't attempt to extract any
		// legal elements
		if (legal.matches(".+ FLA? [\\d-]+"))
			return;

		// initial corrections and cleanup of legal description

		legal = legal.replaceAll("\\bORDN #?[\\d-]+\\s*", "");
		legal = legal.replaceAll("(\\d)(ETAL)\\b", "$1 $2");
		legal = legal.replaceAll("(\\d+)(?:ST|ND|RD|TH) (SEC(?:TION)?|UNIT)\\b", "$2 $1");
		legal = legal.replaceAll("\\bUIT (\\d)", "UNIT $1");
		legal = legal.replaceAll("\\bL(\\d)", "L $1");
		legal = legal.replaceAll("\\s+THRU\\s+", "-");
		legal = legal.replaceAll("(\\d+[A-Z]?|\\b[A-Z])\\s+TO\\s+", "$1-");

		legal = legal.replaceAll("\\b(\\s*)(GOVT?|CORR?(ECTION|ECTIVE)?|ETAL|REV|(RE)?SUB(D(IVISION)?)?|ETC|AMEND|SUBORD|&\\s+SA|MTG)\\b\\s*", "$1");
		legal = legal.replaceAll("\\b(\\s*)(AS )?RENTS & LEASES\\b\\s*", "$1");
		legal = legal.replaceAll("\\b(\\s*)(AS )?RTS & LE\\s*", "$1");
		legal = legal.replaceAll("\\b(\\s*)(REFER TO THE INSTRUMENT|PAT ACRES|MULTIPLE LEGALS)\\b\\s*", "$1");
		legal = legal.replaceAll("^RE .*$", "");
		legal = legal.replaceAll("^RECEIPT .*$", "");
		legal = legal.replaceAll("^DISMISSAL .*$", "");

		legal = legal.replaceAll("\\s+PT (\\d+)\\b", ",$1");
		legal = legal.replaceAll(",\\s+(\\d+|[A-Z])\\b", ",$1");
		legal = legal.replaceAll("\\bL(\\d)", "L $1");
		legal = legal.replaceAll("\\b(\\d+) (\\d+)\\b", "$1,$2");
		legal = legal.replaceAll("\\b(\\d+) (\\d+)\\b", "$1,$2");
		legal = legal.replaceAll("\\b(\\s*)(PTE/)?PTS?\\b\\s*", "$1");

		legal = legal.replaceAll("(UT\\s*\\d+) (\\d{3})", " $1-$2 ");

		legal = legal.trim();
		legal = legal.replaceAll("\\s{2,}", " ");

		// additional cleaning before extracting subdivision name
		legal = legal.replaceAll("^REPL(A?T)?\\b\\s*", "");
		legal = legal.replaceAll("^\\s*(\\d+(ST|ND|RD|TH) )?ADD(ITIO)?N?( TO)?\\b\\s*", "");
		legal = legal.replaceAll("^\\s*SUBD?\\b\\s*", "");
		legal = legal.replaceFirst("\\s+CO$", " CONDO");

		// extract subdivision name
		// String subdiv =
		// legal.replaceFirst("(.*?)\\s*\\b(SUBD?|REPL(A?T)?|SEC|CONDO|(\\d+(ST|ND|RD|TH)? )?ADD(ITIO)?N?|PH(ASE)?|PAR(CEL)?)\\b.*",
		// "$1");
		// subdiv = subdiv.replaceFirst("\\s*#$", "");
		// subdiv = subdiv.replaceFirst("^-\\s*", "");
		// subdiv = subdiv.replaceFirst("^\\s*\\d+\\s*$", "");
		// subdiv = subdiv.replaceFirst("([^;]+;\\s*)(.+)", "$2");
		// subdiv = subdiv.replaceFirst("([\\d/]+) (.+)", "$2");
		// subdiv = subdiv.replaceFirst("(LTS?) (.+)", "$2");
		// subdiv = subdiv.replaceFirst("(.+) (UT .*)", "$1");
		//
		// if (subdiv.length() != 0){
		// m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
		// if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
		// m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
		// }

		String legalTemp = legal;

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		Pattern p = Pattern.compile("(\\b(?:LOTS|LTS?)\\s((\\d+|[A-Z]\\b)([A-Z]\\b|,|[\\s\\d&]+|-)*))");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		lot = lot.replaceAll("&", "");
		lot = lot.trim();
		if (lot.length() != 0) {
			String lotFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (!StringUtils.isEmpty(lotFromSet)) {
				lot = lotFromSet + " " + lot;
			}
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		legal = replaceNumbers(legal);
		// extract tract from legal description
		String tract = ""; // can have multiple occurrences
		p = Pattern.compile("(\\bTR(?:ACT)? ((\\b([\\d\\s&]+|\\d+[A-Z]|[A-Z]\\d+|[A-Z]|,|-)\\b)+))");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(2);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract block from legal description
		String block = "";
		String pat = "\\b([A-Z]|\\d+[A-Z]?)\\b";
		p = Pattern.compile("(\\b(?:BLK) (" + pat + "([,-]" + pat + ")*))");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			block = ma.group(2).replaceAll("\\s*,\\s*", " ");
			String blockFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(blockFromSet)) {
				block = blockFromSet + " " + block;
				block = LegalDescription.cleanValues(block, false, true);
			}
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract section, township and range from legal description
		List<List> body = getSTRFromMap(m); // first add sec-twn-rng extracted
											// from XML specific tags, if any
											// (for DT use)
		p = Pattern.compile("(\\bSEC\\s*([\\d\\s&]+)\\s*-\\s*(\\d+)\\s*-\\s*(\\d+)\\b)");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			String sec = ma.group(2).replaceAll("\\s*,\\s*", " ");
			String twn = ma.group(3);
			String rng = ma.group(4);
			line.add(sec);
			line.add(twn);
			line.add(rng);
			body.add(line);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		saveSTRInMap(m, body);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract cross refs from legal description
		p = Pattern.compile("(\\bOR\\s*(\\d+)\\s*PG\\s*(\\d+)\\b)");
		ma = p.matcher(legal);
		List<List> bodyCR = new ArrayList<List>();

		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add(ma.group(3));
			line.add("");
			line.add("OR");
			bodyCR.add(line);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		p = Pattern.compile("\\b(\\d{8,10})\\s+OR\\s*(\\d+)\\s*/\\s*(\\d+)\\b");
		ma.usePattern(p);
		ma.reset();
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add(ma.group(3));
			line.add(ma.group(1));
			line.add("");
			bodyCR.add(line);
		}
		saveCRInMap(m, bodyCR);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		String tokens[] = { "X", "L", "C", "D", "M" };
		// extract phase from legal description
		pat = "\\b(\\d+[A-Z]?|[IVX]+)\\b";
		p = Pattern.compile("(\\bPH(?:ASE)? (" + pat + "([,-]" + pat + ")*))");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionPhase", Roman.normalizeRomanNumbersExceptTokens(ma.group(2), tokens).replaceAll("\\s*,\\s*", " "));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract unit from legal description
		p = Pattern.compile("((?:#|UT #?)(\\w+([-,]\\w+)*))(?! ADD)");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionUnit", Roman.normalizeRomanNumbersExceptTokens(ma.group(2), tokens).replaceAll("\\s*,\\s*", " "));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract building #
		p = Pattern.compile("(\\bBLDG #?(\\w+)\\b)");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

	}

	public static String resolveOtherTypes(String owner) {
		owner = owner.replaceAll("(?is)\\bET\\s+UX\\b", "ETUX");
		owner = owner.replaceAll("(?is)\\bET\\s+AL\\b", "ETAL");
		owner = owner.replaceAll("(?is)\\bET\\s+VIR\\b", "ETVIR");
		return owner.trim();
	}

	public static String cleanOwnerNameFromPrefix(String owner) {
		owner = owner.replaceAll("(?is)\\bMRS\\b", "");
		return owner.trim();
	}

	public static String cleanOwnerNameFromContractTitles(String owner) throws Exception {
		owner = owner.replaceAll("(?is)TRUSTEE(\\s+OF|\\s+FOR)?", "");
		return owner.trim();
	}

	public static String cleanOwnerNameFromMarriageStatus(String owner) throws Exception {
		owner = owner.replaceAll("(?is)(\\b(WFE?)|(HUSBAND)\\b)", "");
		return owner.trim();
	}

	public static void setCrossRefsFromCommentDT(ResultMap m, long searchId) throws Exception {

		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();

		if ("CA".equals(crtState)) {

			if ("San Francisco".equals(crtCounty)) { // B 3527
				ResultTable crossRefs = (ResultTable) m.get("CrossRefSet");
				String crossRefBook = "";
				if (crossRefs != null) {
					String[][] body = crossRefs.body;
					for (int i = 0; i < body.length; i++) {
						crossRefBook = body[i][0];
						crossRefBook = crossRefBook.replaceAll("([A-Z])0+(\\d+)", "$1$2");
						body[i][0] = crossRefBook;
					}
				}
				String saleDataSetBook = (String) m.get("SaleDataSet.Book");
				if (saleDataSetBook != null) {
					saleDataSetBook = saleDataSetBook.replaceAll("([A-Z])0+(\\d+)", "$1$2");
					m.put("SaleDataSet.Book", saleDataSetBook);
				}
				String comment = (String) m.get("tmpComment");
				if (StringUtils.isEmpty(comment))
					return;

				if (comment.matches("[A-Z]\\d+\\s+\\d+")) {
					if (crossRefs == null) {
						m.put("CrossRefSet.Book", comment.replaceFirst("(.+)\\s+(.+)", "$1"));
						m.put("CrossRefSet.Page", comment.replaceFirst("(.+)\\s+(.+)", "$2"));
					}
				}
			} else if ("Riverside".equals(crtCounty)) { // B 3858
				String comment = (String) m.get("tmpComment");
				if (StringUtils.isEmpty(comment))
					return;
				if (comment.matches("\\d{2}\\s+\\d+")) {
					m.put("CrossRefSet.InstrumentNumber", comment.replaceAll("\\d{2}\\s+(\\d+)", "$1"));
				}
			}
		} else {

			String docType = (String) m.get("SaleDataSet.DocumentType");
			if (docType == null || docType.length() == 0 || !DocumentTypes.checkDocumentType(docType, DocumentTypes.RELEASE_INT, null, searchId))
				return;

			String comment = (String) m.get("tmpComment");
			if (comment == null || comment.length() == 0)
				return;

			Pattern p = Pattern.compile("^(\\d{3,5}) (\\d{3,5})\\b");
			Matcher ma = p.matcher(comment);
			if (ma.find()) {
				m.put("CrossRefSet.Book", ma.group(1));
				m.put("CrossRefSet.Page", ma.group(2));
			}

			p = Pattern.compile("ORB\\s*(\\d+)\\s+(\\d+)"); // B3340
			ma.reset();
			ma = p.matcher(comment);
			if (ma.find()) {
				m.put("CrossRefSet.Book", ma.group(1));
				m.put("CrossRefSet.Page", ma.group(2));
			}
		}
	}

	public static void legalFLOrangeTR(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get("tmpLegal");
		if (StringUtils.isEmpty(legal)) {
			return;
		}
		FLOrangeTR.legalTokenizerFLOrangeTR(m, legal);
	}

	public static void taxFLOrangeTR(ResultMap m, long searchId) throws Exception {
		FLOrangeTR.taxFLOrangeTR(m, searchId);
	}

	public static void parseAddressFLOrangeTR(ResultMap m, long searchId) throws Exception {

		String s = (String) m.get("tmpAddress");
		if (StringUtils.isEmpty(s))
			return;

		FLOrangeTR.parseAddressFLOrangeTR(m, s);
	}

	public static void stdPisFLOrangeTR(ResultMap m, long searchId) throws Exception {

		String s = (String) m.get("tmpOwnerNameAddr");
		s = FLOrangeTR.goToPartyNameTokenizerFLOrangeTR(s);
		if (s == null || s.length() == 0)
			return;
		FLOrangeTR.partyNamesFinalFLOrangeTR(m, s);
	}

	public static void stdFLSarasotaTR(ResultMap m, long searchId) throws Exception {

		String s = (String) m.get("tmpOwnerName");
		if (s == null || s.length() == 0)
			return;

		FLSarasotaTR.stdFLSarasotaTR(m, s);
	}

	public static void partyNamesFLSarasotaTR(ResultMap m, long searchId) throws Exception {

		String owner = (String) m.get("tmpOwnerAddress");
		if (StringUtils.isEmpty(owner))
			return;

		FLSarasotaTR.partyNamesFLSarasotaTR(m, owner);
	}

	public static void legalFinalFLSarasotaTR(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(legal))
			return;

		FLSarasotaTR.legalFLSarasotaTR(m, legal);
	}

	public static void legalIntermFLSarasotaTR(ResultMap m, long searchId) throws Exception {
		String descr = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(descr))
			return;

		legalTokenizerIntermFLSarasotaTR(m, descr);
	}

	public static void legalTokenizerIntermFLSarasotaTR(ResultMap m, String descr) throws Exception {
		Pattern p = Pattern.compile("(.*?)\\s+((?:LOTS?|UNIT|[EWNS] \\d+|COM AT|SEC)\\b.*)");
		Matcher ma = p.matcher(descr);
		if (ma.find()) {
			String address = ma.group(1);
			if (address.length() != 0) {
				m.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address));
				m.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address));
			}

			FLSarasotaTR.legalFLSarasotaTR(m, ma.group(2));
		}
	}

	public static void taxFLSarasotaTR(ResultMap m, long searchId) throws Exception {
		FLSarasotaTR.taxFLSarasotaTR(m, searchId);
	}

	public static void legalFinalFLDuvalTR(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(legal))
			return;

		FLDuvalTR.legalFLDuvalTR(m, legal);
	}

	public static void legalIntermFLDuvalTR(ResultMap m, long searchId) throws Exception {
		String descr = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(descr))
			return;
		legalTokenizerIntermFLDuvalTR(m, descr);
	}

	public static void legalTokenizerIntermFLDuvalTR(ResultMap m, String descr) throws Exception {
		Pattern p = Pattern.compile("^[\\dNSEW\\s-]{11}\\s+(.*)");
		Matcher ma = p.matcher(descr);
		if (ma.find()) {
			if (!ma.group(1).startsWith("-")) {
				descr = ma.group(1);
				p = Pattern.compile("(.+?)\\s((?:\\d+-\\d+|[\\d,]+-[\\dNSEW]+-[\\dNSEW]+)\\b.*)");
				ma = p.matcher(descr);
				if (ma.find()) {
					descr = ma.group(2);
					String address = ma.group(1);
					if (address.length() != 0) {
						m.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(address));
						m.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(address));
					}
				}
			}
		}
		FLDuvalTR.legalFLDuvalTR(m, descr);
	}

	public static void stdFLDuvalTR(ResultMap m, long searchId) throws Exception {
		FLDuvalTR.stdFLDuvalTR(m, searchId);
	}

	public static void legalFLPolkDASLRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		FLPolkTR.legalTokenizerLEGALFLPolkRV(m, legal);
	}

	public static void legalFLPolkTR(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(legal))
			return;
		FLPolkTR.legalTokenizerFinalFLPolkTR(m, legal);
	}

	public static void addressFLPolkTR(ResultMap m, long searchId) throws Exception {
		String adr = (String) m.get("tmpAddress");
		if (StringUtils.isEmpty(adr))
			return;
		FLPolkTR.parseAddress(m, adr);
	}

	public static void legalIntermFLPolkTR(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(legal))
			return;
		FLPolkTR.legalTokenizerIntermediateFLPolkTR(m, legal);
	}

	protected static String cleanOwnerFLPolkTR(String s) {
		s = s.toUpperCase();
		s = s.replaceAll("\\bC/O\\b", "");
		s = s.replace(',', ' ');
		s = s.replaceAll("\\b(ET)\\s+(AL|UX|VIR)", "$1$2");
		s = s.replaceAll("\\s{2,}", " ").trim();
		return s;
	}

	public static void stdFinalFLPolkTR(ResultMap m, long searchId) throws Exception {

		String ownerAddr = (String) m.get("tmpOwnerAddress");
		if (StringUtils.isEmpty(ownerAddr))
			return;
		int lineForMailAddress = 1;
		String[] lines = ownerAddr.split("\\s{2,}");
		// the owner and co-owner name are stored in the first 1 or 2 lines and
		// the last 2 lines contains the mailing address
		if (lines.length <= 2)
			return;

		String[] a = StringFormats.parseNameNashville(cleanOwnerFLPolkTR(lines[0]));
		if (lines[1].startsWith("C/O ")) {
			String[] b = StringFormats.parseNameDesotoRO(cleanOwnerFLPolkTR(lines[1]));
			a[3] = b[0];
			a[4] = b[1];
			a[5] = b[2];
			lineForMailAddress++;
		}
	}

	public static void stdFLPolkTR(ResultMap m, long searchId) throws Exception {
		String ownerAddr = (String) m.get("tmpOwnerName");
		if (StringUtils.isEmpty(ownerAddr))
			return;

		ownerAddr = FLSantaRosaTR.cleanOwnerFLSantaRosaTR(ownerAddr);

		// 342723-013009-000750 - WILEN CESAR WILEN VERONICA
		// 342723-013009-000760 - GITTENS EZRA R GITTENS CAMILLA
		// 342723-013009-000240 - HERRERA ROBERT W HERRERA EVELY
		// 342723-013009-000300 - DEVICTORIA JOELSON L DEVICTORIA MAGDA L
		// 342723-013009-001520 - MABE HORACE EUGENE JR MABE AUDREY
		if (NameUtils.isNotCompany(ownerAddr)) {
			ownerAddr = ownerAddr.replaceAll("(?is)([A-Z]+)\\s+([A-Z]+(?:\\s+[A-Z]+)?(?:\\s+[A-Z]{1,2})?)\\s+(\\1)\\s*", "$1 $2 & $3 ");
			if (!ownerAddr.contains("&")) {
				ownerAddr = ownerAddr.replaceFirst("(?is)([A-Z]+)\\s+([A-Z]+(?:\\s+[A-Z])?)\\s+([A-Z]{2,}\\s+[A-Z]+)", "$1 $2 & $3");
			}
		}
		// separate owner of co-owner
		String[] lines = ownerAddr.split("\\bC/O\\b\\s+");

		String[] a = StringFormats.parseNameNashville(cleanOwnerFLPolkTR(lines[0]), true);
		if (lines.length == 2) {
			String[] b = StringFormats.parseNameDesotoRO(cleanOwnerFLPolkTR(lines[1]), true);
			a[3] = b[0];
			a[4] = b[1];
			a[5] = b[2];
		}

		List<List> body = new ArrayList<List>();
		String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
		types = GenericFunctions.extractAllNamesType(a);
		otherTypes = GenericFunctions.extractAllNamesOtherType(a);
		suffixes = GenericFunctions.extractAllNamesSufixes(a);

		GenericFunctions.addOwnerNames(a, suffixes[0], suffixes[1], types, otherTypes, NameUtils.isCompany(a[2]), NameUtils.isCompany(a[5]), body);

		GenericFunctions.storeOwnerInPartyNames(m, body, true);
	}

	public static void legalFLStJohnTRFinal(ResultMap m, long searchId) throws Exception {
		FLStJohnsTR.legalFLStJohnTRFinal(m, searchId);
	}

	public static void legalFLStJohnTRInterm(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(legal))
			return;
		legalTokenizerFLStJohnTRInterm(m, legal);
	}

	public static void legalTokenizerFLStJohnTRInterm(ResultMap m, String legal) throws Exception {
		FLStJohnsTR.legalTokenizerFLStJohnTRInterm(m, legal);
	}

	public static void stdFLStJohnsTR(ResultMap m, long searchId) throws Exception {
		FLStJohnsTR.stdFLStJohnsTR(m, searchId);
	}

	public static void partyNamesFLStJohnsTR(ResultMap m, long searchId) throws Exception {
		FLStJohnsTR.partyNamesFLStJohnsTR(m, searchId);
	}

	public static void stdFLNassauTR(ResultMap m, long searchId) throws Exception {

		String s = (String) m.get("tmpOwnerName");

		if (StringUtils.isEmpty(s))
			return;

		FLNassauTR.stdFLNassauTR(m, searchId);
	}

	public static void partyNamesFLNassauTR(ResultMap m, long searchId) throws Exception {

		String owner = (String) m.get("tmpOwnerAddress");
		if (StringUtils.isEmpty(owner))
			return;

		FLNassauTR.partyNamesFLNassauTR(m, searchId);
	}

	public static void parseAddressFLNassauTR(ResultMap m, long searchId) throws Exception {

		String propertyAddress = (String) m.get("tmpAddress");
		if (StringUtils.isEmpty(propertyAddress))
			return;

		FLNassauTR.parseAddressFLNassauTR(m, searchId);
	}

	public static void legalFLNassauTR(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(legal))
			return;

		FLNassauTR.legalFLNassauTR(m, searchId);
	}

	public static void taxFLNassauTR(ResultMap m, long searchId) throws Exception {

		FLNassauTR.taxFLNassauTR(m, searchId);
	}

	public static void legalFLVolusiaTR(ResultMap m, long searchId) throws Exception {
		// TODO FLVolusiaTR.legalFLVolusiaTR(m, searchId);
	}

	public static void partyNamesFLVolusiaTR(ResultMap m, long searchId) throws Exception {
		// FLVolusiaTR.partyNamesFLVolusiaTR(m, searchId);
	}

	public static void stdFLVolusiaTR(ResultMap m, long searchId) throws Exception {
		// FLVolusiaTR.stdFLVolusiaTR(m, searchId);
	}

	public static void stdFLSeminoleTR(ResultMap m, long searchId) throws Exception {

		String owners = (String) m.get("tmpOwnerName");
		if (StringUtils.isEmpty(owners))
			return;

		FLSeminoleTR.stdFLSeminoleTR(m, searchId);
	}

	public static void partyNamesFLSeminoleTR(ResultMap m, long searchId) throws Exception {

		String ownName = (String) m.get("tmpOwnerName");

		if (StringUtils.isEmpty(ownName))
			return;

		FLSeminoleTR.partyNamesFLSeminoleTR(m, searchId);
	}

	public static void legalFLSeminoleTR(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(legal))
			return;

		FLSeminoleTR.legalFLSeminoleTR(m, searchId);
	}

	public static void taxFLSeminoleTR(ResultMap m, long searchId) throws Exception {

		FLSeminoleTR.taxFLSeminoleTR(m, searchId);
	}

	public static void stdFLLeeTR(ResultMap m, long searchId) throws Exception {

		String owners = (String) m.get("tmpOwnerName");
		if (StringUtils.isEmpty(owners))
			return;

		FLLeeTR.partyNamesInterFLLeeTR(m, searchId);
	}

	public static void partyNamesFLLeeTR(ResultMap m, long searchId) throws Exception {

		String owners = (String) m.get("tmpOwnerName");
		if (StringUtils.isEmpty(owners))
			return;

		FLLeeTR.partyNamesFLLeeTR(m, searchId);
	}

	public static void legalFLLeeTR(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(legal))
			return;

		FLLeeTR.legalTokenizerFLLeeTR(m, searchId);
	}

	public static void taxFLLeeTR(ResultMap m, long searchId) throws Exception {

		FLLeeTR.taxFLLeeTR(m, searchId);
	}

	public static void partyNamesFLPascoTR(ResultMap m, long searchId) throws Exception {
		String ownerName = (String) m.get("tmpOwnerName");
		String address = (String) m.get("tmpAddress");
		if (StringUtils.isNotEmpty(address)) {
			String[] split = address.split("\\b\\w+\\b");
			List<Integer> stringContainsPositions = StringUtils.stringContainsComponentsFromOtherString(ownerName, address);
			List<String> splitString = StringUtils.splitString(address);
			if (stringContainsPositions.size() >= 2) {
				ownerName = ownerName.substring(0, ownerName.indexOf(splitString.get(0)));
			} else {
				Pattern p = Pattern.compile("((?=(INC|PO)).*|\\d.*)");
				Matcher ma = p.matcher(ownerName);
				if (ma.find()) {
					ownerName = ownerName.replaceAll("((?=(INC|PO)).*|\\d.*)", "");
				}
			}
		}
		if (StringUtils.isEmpty(ownerName))
			return;

		FLPascoTR.partyNamesTokenizerTR(m, ownerName);
	}

	public static void stdFLPascoTR(ResultMap m, long searchId) throws Exception {

		String s = (String) m.get("tmpOwnerName");
		if (StringUtils.isEmpty(s))
			return;

		FLPascoTR.stdFLPascoTR(m, s);
	}

	public static void legalFLPascoTR(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(legal))
			return;
		FLPascoTR.legalFLPascoTR(m, searchId);
	}

	public static void taxFLPascoTR(ResultMap m, long searchId) throws Exception {
		FLPascoTR.taxFLPascoTR(m, searchId);
	}

	public static void stdFLBrevardTR(ResultMap m, long searchId) throws Exception {
		FLBrevardTR.stdFLBrevardTR(m, searchId);
	}

	public static void stdFinalFLBrevardTR(ResultMap m, long searchId) throws Exception {
		FLBrevardTR.stdFinalFLBrevardTR(m, searchId);
	}

	public static Vector<String> wordsFLBrevardTR = new Vector<String>();
	static {
		wordsFLBrevardTR.add("FLORIDA");
		wordsFLBrevardTR.add("COMPLIANCE");
		wordsFLBrevardTR.add("OF");
		wordsFLBrevardTR.add("GOD");
		wordsFLBrevardTR.add("TRUSTEE");
		wordsFLBrevardTR.add("AND");
	}

	public static void addresslFLBrevardTR(ResultMap m, long searchId) throws Exception {
		FLBrevardTR.parseAddresslFLBrevardTR(m, searchId);
	}

	public static void pisFLBrevardTR(ResultMap m, long searchId) throws Exception {
		FLBrevardTR.pisFLBrevardTR(m, searchId);
	}

	public static void pisIntermFLBrevardTR(ResultMap m, long searchId) throws Exception {
		FLBrevardTR.pisIntermFLBrevardTR(m, searchId);
	}

	public static Vector<String> excludeWordsFLBrevardTR = new Vector<String>();
	static {
		excludeWordsFLBrevardTR.add("ST");
	}

	public static String[] removeAddressFLTR(String[] a, Vector<String> excludeWords, Vector<String> extraWords, int minAddressLines, int lastLineLength) {

		return removeAddressFLTR(a, excludeWords, extraWords, minAddressLines, lastLineLength, false);
	}

	/**
	 * removed the addres from some counties that display address as<br>
	 * Name<br>
	 * Address
	 * 
	 * @param a
	 *            - the mailing addreses split on rows
	 * @param excludeWords
	 *            - words to exclude from the company Name words list
	 * @param extraWords
	 *            - words to include in the company name words list used when we
	 *            want to merge names split on 2 rows
	 * @param minAddressLines
	 *            - what's the min number of lines used to store address
	 * @param lastLineLength
	 *            - if the last line is longer then this, then we dec
	 *            minAddressLines<br>
	 *            user Integer.MAX_VALUE if you want this feature "disabled"
	 */
	public static String[] removeAddressFLTR(String[] a, Vector<String> excludeWords, Vector<String> extraWords, int minAddressLines, int lastLineLength,
			boolean removeOnlyAddress) {
		int i = 0, j = 0;
		String[] tempStr, tempStr2;
		String tmpLine;
		if (Countries.isCountry(a[a.length - 1]) || a[a.length - 1].matches("\\d+\\-\\d+")) {
			i++;
		}
		// there are at least minAddressLines lines of addresses
		if (a[a.length - 1].length() > lastLineLength) {
			minAddressLines--;
		}
		i += minAddressLines;
		// check the last lines if it's a street name or company name.
		// tmpLine = a[a.length - i - 1];
		for (int jj = a.length - i - 1; jj >= 1; jj--) {
			if (!NameUtils.isCompany(a[jj], excludeWords, true) && a[jj].matches(".*?[0-9]+.*") && !a[jj].matches("(?i).*?c/o.*")) {
				i++;
			} else {
				break;
			}
		}
		tempStr = new String[a.length - i];

		if (removeOnlyAddress) {
			for (i = 0; i < tempStr.length; i++) {
				tempStr[i] = a[i];
			}
			return tempStr;
		} else {
			// remove ATTN
			// merge split names
			for (i = 0; i < tempStr.length; i++) {
				if (!NameCleaner.isValidName(a[i])) {
					j++;
				} else {
					boolean merge = false;
					if (i >= 1) {
						merge = NameUtils.isCompanyNamesOnly(a[i], extraWords);// company
																				// names
																				// split
																				// on
																				// multiple
																				// rows;
						if (!merge && a[i - 1].matches("\\w+,\\s+\\w+\\s+\\w+\\s+&")) { // SMITH,
																						// WESLEY
																						// LOUIS
																						// &
																						// on
																						// the
																						// first
																						// row
							if (a[i].matches("(\\w+)\\s(\\w){1}")) { // SHANNON
																		// L on
																		// the
																		// second
																		// row
								merge = true;
							} else if (a[i].matches("(\\w+)\\s(\\w)+")) { // ELIZABETH
																			// ANN
																			// on
																			// the
																			// second
																			// row
								String[] n = a[i].split("\\s");
								if (NameFactory.getInstance().isFemale(n[0]) && NameFactory.getInstance().isFemale(n[1])
										|| NameFactory.getInstance().isMale(n[0]) && !NameFactory.getInstance().isMale(n[1])) {
									// ELIZABETH ANN
									// CATHERINE CRAIG
									merge = true;
								}
							}
						}

					}
					if (merge) {
						tempStr[i - j - 1] += " " + a[i];
						j++;
					} else {
						tempStr[i - j] = a[i];
					}
				}
			}
			// return an array without null components
			if (j > 0) {
				tempStr2 = new String[tempStr.length - j];
				for (i = 0; i < tempStr2.length; i++) {
					tempStr2[i] = tempStr[i];
				}
				return tempStr2;
			}
		}
		return tempStr;
	}

	/**
	 * removed the addres from some counties that display address as<br>
	 * Name<br>
	 * Address<br>
	 * Improved with ArrayList
	 * 
	 * @param a
	 *            - the mailing addreses split on rows
	 * @param excludeWords
	 *            - words to exclude from the company Name words list
	 * @param extraWords
	 *            - words to include in the company name words list used when we
	 *            want to merge names split on 2 rows
	 * @param extraInvalid
	 *            - extra invalid patterns
	 * @param minAddressLines
	 *            - what's the min number of lines used to store address
	 * @param lastLineLength
	 *            - if the last line is longer then this, then we dec
	 *            minAddressLines<br>
	 *            user Integer.MAX_VALUE if you want this feature "disabled"
	 */
	public static ArrayList<String> removeAddressFLTR2(ArrayList<String> a, Vector<String> excludeWords, Vector<String> extraWords,
			Vector<String> extraInvalid, int minAddressLines, int lastLineLength) {
		int n = 0, j = 0;
		ArrayList<String> tempStr;
		String tmpLine;
		if (Countries.isCountry(a.get(a.size() - 1)) || a.get(a.size() - 1).matches("\\d+\\-\\d+") || a.get(a.size() - 1).matches(",.*-")) {
			n++;
		}
		// HICKAM TRUST P O BOX 335 on second line
		if (a.size() > 1 && a.get(a.size() - 2).contains("!!!!!")) {
			a.set(a.size() - 2, a.get(a.size() - 2).replaceAll("!!!!!.*", ""));
			n--;
		}
		// there are at least minAddressLines lines of addresses
		if (a.get(a.size() - 1).length() > lastLineLength) {
			minAddressLines--;

		}
		n += minAddressLines;
		// check the last lines if it's a street name or company name.
		// tmpLine = a[a.length - i - 1];
		for (int jj = a.size() - n - 1; jj >= 1; jj--) {
			if (!NameUtils.isCompany(a.get(jj), excludeWords, true) && a.get(jj).matches(".*?[0-9]+.*") && !a.get(jj).matches("(?i).*?c/(o|0).*")) {
				n++;
			} else {
				break;
			}
		}
		tempStr = new ArrayList<String>();

		// remove ATTN
		// merge split names
		for (int i = 0; i < a.size() - n; i++) {
			if (!NameCleaner.isValidName(a.get(i), extraInvalid)) {
				j++;
			} else {
				boolean merge = false;
				// the merge needs improvements
				if (i >= 1) {
					merge = NameUtils.isCompanyNamesOnly(a.get(i), extraWords);// company
																				// names
																				// split
																				// on
																				// multiple
																				// rows
					if (!merge && a.get(i - 1).matches("\\w+,\\s+\\w+\\s+\\w+\\s+&")) { // SMITH,
																						// WESLEY
																						// LOUIS
																						// &
																						// on
																						// the
																						// first
																						// row
						if (a.get(i).matches("(\\w+)\\s(\\w){1}")) { // SHANNON
																		// L on
																		// the
																		// second
																		// row
							merge = true;
						} else if (a.get(i).matches("(\\w+)\\s(\\w)+")) { // ELIZABETH
																			// ANN
																			// on
																			// the
																			// second
																			// row
							String[] nsplit = a.get(i).split("\\s");
							if (NameFactory.getInstance().isFemale(nsplit[0]) && NameFactory.getInstance().isFemale(nsplit[1])
									|| NameFactory.getInstance().isMale(nsplit[0]) && !NameFactory.getInstance().isMale(nsplit[1])) {
								// ELIZABETH ANN
								// CATHERINE CRAIG
								merge = true;
							}
						}
					}

				}
				if (merge) {
					tempStr.set(i - j - 1, tempStr.get(i - j - 1) + " " + a.get(i));
					j++;
				} else {
					tempStr.add(i - j, a.get(i));
				}
			}
		}
		// return an array without null components
		/*
		 * if (j > 0){ tempStr2 = new String[tempStr.length - j]; for (i=0;
		 * i<tempStr2.length; i++){ tempStr2[i] = tempStr[i]; } return tempStr2;
		 * }
		 */
		return tempStr;
	}

	public static void legalIntermFLBrevardTR(ResultMap m, long searchId) throws Exception {
		FLBrevardTR.legalIntermFLBrevardTR(m, searchId);
	}

	public static void legalFinalFLBrevardTR(ResultMap m, long searchId) throws Exception {
		FLBrevardTR.legalFinalFLBrevardTR(m, searchId);
	}

	public static void legalIntermFLEscambiaTR(ResultMap m, long searchId) throws Exception {

		String initialDescr = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		String descr = initialDescr;
		if (StringUtils.isEmpty(descr)) {
			return;
		}

		// remove PIN
		descr = descr.replaceFirst("[A-Z0-9-]+\\s*", "");

		// find end of address
		Pattern pat = Pattern.compile("(?i)\\b(BEG|LT|LTS|LOT|LOTS|[NSEW] FT OF|[NSEW]\\s*\\d*/?\\d* OF)\\b");
		Matcher mat = pat.matcher(descr);
		if (!mat.find()) {
			return;
		}
		int idx = descr.indexOf(mat.group(1));
		if (idx == -1) {
			return;
		}
		String address = descr.substring(0, idx).trim();
		if (address.matches("\\(\\d+\\)") || address.length() < 6) {
			return;
		}

		String legal = descr.substring(idx).trim();
		String streetName = StringFormats.StreetName(address);
		String streetNo = StringFormats.StreetNo(address);

		if (StringUtils.isEmpty(legal) || StringUtils.isEmpty(streetName) || StringUtils.isEmpty(streetNo) || address.contains("BLK") || address.contains("LT")) {
			return;
		}

		m.put("PropertyIdentificationSet.StreetName", streetName);
		m.put("PropertyIdentificationSet.StreetNo", streetNo);

		FLEscambiaTR.legalFLEscambiaTR(m, legal);
	}

	public static void legalFinalFLEscambiaTR(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("tmpLegal");
		if (legal == null) {
			return;
		}

		legal = legal.replaceFirst("(?is).*Legal\\s*Description(?:\\s*\\(click[^\\)]+\\)\\s*)?([^<]+).*", "$1");
		if (StringUtils.isNotEmpty(legal)) {
			FLEscambiaTR.legalFLEscambiaTR(m, legal);
		}
	}

	public static void partyNamesFLEscambiaTR(ResultMap m, long searchId) throws Exception {
		FLEscambiaTR.partyNamesFLEscambiaTR(m, searchId);
	}

	public static void legalFLSantaRosaDASLRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerLEGALFLSantaRosaRV(m, legal);
	}

	public static void legalRemarksFLSantaRosaRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		FLSantaRosaTR.legalTokenizerRemarksFLSantaRosaRV(m, legal);
	}

	public static void legalTokenizerLEGALFLSantaRosaRV(ResultMap m, String legal) throws Exception {

		FLSantaRosaTR.extractLegalElemsFLSantaRosa(m, legal);
	}

	public static void legalIntermFLSantaRosaTR(ResultMap m, long searchId) throws Exception {
		String descr = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(descr)) {
			return;
		}
		String legal = descr;
		FLSantaRosaTR.legalFLSantaRosaTR(m, legal);
	}

	public static void legalFinalFLSantaRosaTR(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(legal)) {
			return;
		}
		FLSantaRosaTR.legalFLSantaRosaTR(m, legal);
	}

	public static void partyNamesFLSantaRosaTR(ResultMap m, long searchId) throws Exception {
		String own = (String) m.get("tmpOwnerAddress");
		if (StringUtils.isEmpty(own))
			return;
		own = FLSantaRosaTR.cleanOwnerFLSantaRosaTR(own);
		FLSantaRosaTR.partyNamesTokenizerFLSantaRosaTR(m, own);
	}

	public static String cleanOwnerFLOsceolaTR(String s) {
		s = s.toUpperCase();
		s = s.replaceFirst("\\b\\s*\\(H&W\\)", "");
		s = s.replaceFirst("\\b\\s*\\(F/D\\)", "");
		s = s.replaceFirst("\\bC/O\\b", "");
		s = s.replaceAll("-\\bCO\\b", "");
		s = s.replaceAll("HEIRS OF ", "");
		s = s.replace("%", "&");
		s = s.replaceAll("\\bMRS\\b", "");
		s = s.replaceAll("\\bRLE\\b", "");
		s = s.replaceAll("\\(", "");
		s = s.replaceAll("\\)", "");
		// s = s.replaceAll("\\s{2,}", " ").trim();
		return s;
	}

	public static void taxFLHernandoTR(ResultMap m, long searchId) throws Exception {
		FLHernandoTR.taxFLHernandoTR(m, searchId);
	}

	public static void stdFLWaltonTR(ResultMap m, long searchId) throws Exception {
		FLWaltonTR.stdFLWaltonTR(m, searchId);
	}

	public static void partyNamesFLWaltonTR(ResultMap m, long searchId) throws Exception {
		FLWaltonTR.partyNamesFLWaltonTR(m, searchId);
	}

//	public static void partyNamesFLSumterTR(ResultMap m, long searchId) throws Exception {
//		String owner = (String) m.get("tmpOwnerName");// tmpOwnerAddress
//		if (StringUtils.isEmpty(owner))
//			return;
//
//		owner = FLSumterTR.cleanOwnerFLSumterTR(owner);
//		owner = FLSumterTR.partyNamesTokenizerFLSumterTR(owner);
//		FLSumterTR.partyNamesFLSumterTR(m, owner);
//	}

	/*
	 * public static void stdFLSumterTR(ResultMap m, long searchId) throws
	 * Exception { // the owner and co-owner name are stored in this string
	 * String owner = (String) m.get("tmpOwnerName");//tmpOwnerAddress if
	 * (StringUtils.isEmpty(owner)) return;
	 * 
	 * owner = owner.replaceFirst("(?s)(.*)\\s*\\bUNKNOWN\\b.*", "$1"); String
	 * a[] = FLWaltonTR.ownerNameFLWaltonTR(owner);
	 * 
	 * m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
	 * m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
	 * m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
	 * m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
	 * m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
	 * m.put("PropertyIdentificationSet.SpouseLastName", a[5]); }
	 */
	public static void legalRemarksFLWaltonRV(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;
		legalTokenizerRemarksFLWaltonRV(m, legal);
	}

	public static void legalTokenizerRemarksFLWaltonRV(ResultMap m, String legal) throws Exception {
		// initial corrections and cleanup of legal description
		legal = legal.replaceAll("\\bADDITIO N\\b", "ADDITION");
		legal = legal.replaceAll("\\bII I(\\s|$)", "3");
		legal = legal.replaceAll("\\bPHAS E\\b", "PHASE");

		legal = legal.replaceAll("\\b[SWEN]{1,2}\\s*(\\d+ )?\\d*[\\./]?\\d+(\\s*FT)?(\\s*OF)?\\b", "");
		legal = legal.replaceAll("\\b[SWEN]{1,2}/C\\b", "");
		legal = legal.replaceAll("\\bETC\\b\\.?", "");
		legal = legal.replaceAll("\\bINCOMPLETE LEGAL\\b", "");
		legal = legal.replaceAll("^.*:\\s*", "");
		legal = legal.replaceAll("\\b\\d+/\\d+/\\d+\\b", "");
		legal = legal.replaceAll("\\bTO CORRECT\\b", "");
		legal = legal.replaceAll("\\bNO\\b\\.?", "");
		legal = legal.replaceAll("\\bREPLAT\\b", "");
		legal = legal.replaceAll("\\bGOV('?T)?\\b", "");
		legal = legal.replaceAll("\\bU.S. GOVERNMENT\\b", "");
		legal = legal.replaceAll("\\b(\\d+-\\d+[SWEN]-\\d+) ([SWEN])\\b", "$1$2");

		legal = replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
																					// to
																					// arabics

		legal = legal.trim();
		legal = legal.replaceAll("\\s{2,}", " ");

		// extract and remove section, township and range from legal description
		List<List> body = getSTRFromMap(m); // first add sec-twn-rng extracted
											// from XML specific tags, if any
											// (for DT use)
		Pattern p = Pattern.compile("\\bSEC\\.? (\\d+)-(\\d+[SWEN])(?:-(\\d+[SWEN]))?\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			String sec = ma.group(1);
			String twn = ma.group(2);
			String rng = ma.group(3);
			if (rng == null)
				rng = "";
			line.add(sec);
			line.add(twn);
			line.add(rng);
			body.add(line);
			legal = legal.replace(ma.group(0), "");
		}
		saveSTRInMap(m, body);
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract and replace lot from legal description
		String lot = ""; // can have multiple occurrences
		String patt = "(?:\\d+|[A-Z](?=[\\s&,$-]))";
		p = Pattern.compile("\\bLO?TS? (" + patt + "(?:\\s*[&,-]\\s*" + patt + ")*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
			legal = legal.replace(ma.group(0), "LOT ");
		}
		p = Pattern.compile("\\bL\\s*(\\d+(?:\\s*[&-]\\s*\\d+)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(1);
			legal = legal.replace(ma.group(0), "LOT ");
		}
		lot = lot.replaceAll("[&,]", " ").trim();
		String lotFromLegal = lot;
		if (lot.length() != 0) {
			String lotFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (!StringUtils.isEmpty(lotFromSet)) {
				lot = lotFromSet + " " + lot;
			}
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace block from legal description
		String block = "";
		p = Pattern.compile("\\bBL(?:OC)?K?\\.? (\\d+|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1);
			legal = legal.replaceFirst(ma.group(0) + "\\b", "BLK ");
		}
		block = block.trim();
		String blockFromLegal = block;
		if (block.length() != 0) {
			String blockFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(blockFromSet)) {
				block = blockFromSet + " " + block;
			}
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace phase from legal description
		String phase = "";
		patt = "(?:\\d+[A-Z]?|I|[IVX]+[A-Z])";
		p = Pattern.compile("\\bPH(?:ASE)? (" + patt + "(?:\\s*[&,-]\\s*" + patt + ")*)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = ma.group(1).replaceAll("\\b(\\w+)([A-HJ-UWYZ])\\b", "$1 _ $2");
			String[] exceptionTokens1 = { "M", "C", "L", "D" };
			phase = Roman.normalizeRomanNumbersExceptTokens(phase, exceptionTokens1); // convert
																						// roman
																						// numbers
																						// to
																						// arabics
			phase = phase.replaceFirst(" _ ", "");
			phase = phase.replaceAll("\\s*[&,\\s-]\\s*", " ");
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legal = legal.replace(ma.group(0), "PHASE ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		legal = legal.replaceAll("\\bOR 0/0\\b", "OR");
		p = Pattern.compile("(?<!PLAT) BK (\\d+) PG\\.? (\\d+(?:-\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add("");
			line.add("");
			bodyCR.add(line);
			legal = legal.replace(ma.group(0), "OR ");
		}

		p = Pattern.compile("^(\\d+)/(\\d+)\\b(?!/)");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add("");
			line.add("");
			bodyCR.add(line);
			legal = legal.replace(ma.group(0), "OR ");
		}

		p = Pattern.compile("^(\\d+) OR (\\d+)/(\\d+)");
		ma = p.matcher(legal);
		if (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add(ma.group(3));
			line.add(ma.group(1));
			line.add("");
			bodyCR.add(line);
			legal = legal.replace(ma.group(0), "OR ");
		}
		saveCRInMap(m, bodyCR);
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		// extract and replace unit from legal description
		String unitFromLegal = "";
		patt = "(\\d+(?:-?[A-Z])?|[A-Z])";
		p = Pattern.compile("(?:\\bUNITS? #?|\\bAPT\\.? #?|#|^)" + patt + "\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			unitFromLegal = ma.group(1).replaceFirst("^0+(.+)", "$1");
			m.put("PropertyIdentificationSet.SubdivisionUnit", unitFromLegal);
			legal = legal.replace(ma.group(0), "UNIT ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace building #
		p = Pattern.compile("\\bBLDG (\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
			legal = legal.replace(ma.group(0), "BLDG ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace plat b&p
		p = Pattern.compile("\\bPLAT B(?:OO)?K (\\d+) (?:PAGE|PG) (\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.PlatBook", ma.group(1));
			m.put("PropertyIdentificationSet.PlatNo", ma.group(2));
			legal = legal.replace(ma.group(0), "PB ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract subdivision name - only if lot or block or unit was extracted
		// or legal contains S/D or CONDO
		// additional cleaning before extracting subdivision name
		legal = legal.replaceFirst("[,;\\s]+$", "");
		legal = legal.replaceAll("^[,;\\s]+", "");
		boolean unlistedLots = legal.contains("LOTS LISTED");
		legal = legal.replaceAll("\\bLOTS LISTED\\b\\s*-?\\s*", "LOT ");
		String subdiv = "";
		if (lotFromLegal.length() == 0 && !unlistedLots && blockFromLegal.length() == 0 && unitFromLegal.length() == 0) {
			p = Pattern.compile("(.*)\\s*\\b(?:S/?D|CONDO(?:MINIUM)?)");
			ma = p.matcher(legal);
		} else {
			p = Pattern.compile(".*?\\b(?:(?:LOT|BLK|UNIT|BLDG)\\b(?: OF\\b|[,;\\s&-]+)?)+\\s*(.*?)\\s*(?:S/?D|LOT|UNIT|CONDO(?:MINIUM)?|PHASE|$)");
			ma = p.matcher(legal);
		}
		if (ma.find()) {
			subdiv = ma.group(1).trim();
		}
		if (subdiv.length() != 0) {
			subdiv = subdiv.replaceFirst("\\s*\\b\\d+(ST|ND|RD|TH) ADD(ITION)?\\b.*", "");
			subdiv = subdiv.replaceFirst("\\s+\\d+$", "");
			subdiv = subdiv.replaceFirst("\\s*[,\\(]$", "");
			subdiv = subdiv.replaceFirst("^\\d+\\b\\s*", "");
		}
		if (subdiv.length() != 0) {
			m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			if (legal.matches(".*\\bCONDO(MINIUM)?\\b.*")) {
				m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			}
		}
	}

	public static void legalFLWaltonDASLRV(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;
		legalTokenizerFLWaltonDASLRV(m, legal);
	}

	public static void legalTokenizerFLWaltonDASLRV(ResultMap m, String legal) throws Exception {
		// initial correction and cleanup of legal description
		legal = legal.replaceAll("\\b(OR \\d+-\\d+) (\\d+(?:\\s|$))", "$1$2");
		legal = legal.replaceAll("\\b([SWEN])(\\d+)", "$1 $2");
		legal = legal.replaceAll("(\\d+)(DEG)\\b", "$1 $2");
		legal = legal.replaceAll("\\b[SWEN]{1,2}\\s*(\\d+ )?\\d*[\\./]?\\d+(\\s*FT)?(\\s*OF)?\\b", "");
		legal = legal.replaceAll("\\b[SWEN]{1,2} OF\\b", "");
		legal = legal.replaceAll("\\bLONG DESC\\b", "");
		legal = legal.replaceAll("\\bRECD( IN)?\\b", "");
		legal = legal.replaceAll("\\bDESC IN\\b", "");
		legal = legal.replaceAll("\\bBEING\\b", "");
		legal = legal.replaceAll("\\bINCL\\b", "");
		legal = legal.replaceAll("\\bNO\\b\\.?", "");
		legal = legal.replaceAll("\\bTHE\\s+ABANDONED\\s+ALLEY\\b", "");
		legal = legal.replaceAll("\\bADJACENT\\s+TO\\b", "");

		legal = replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
																					// to
																					// arabics

		// extract and replace lot from legal description
		String lot = ""; // can have multiple occurrences
		String patt = "(?:\\d+|[A-Z](?=[\\s&$-]))";
		Pattern p = Pattern.compile("\\bLOTS? (" + patt + "(?:[&+,\\s-]+" + patt + ")*)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			String lotTemp = ma.group(1);
			if (lotTemp.matches("\\d+(-\\d+){2,}")) {
				lotTemp = lotTemp.replace('-', ' ');
			}
			lotTemp = lotTemp.replaceAll("[&,+]", " ");
			if (lotTemp.matches("^\\d+.*")) {
				lotTemp = lotTemp.replaceAll(" [A-Z]", "");
			}
			lot = lot + " " + lotTemp;
			legal = legal.replace(ma.group(0), "LOT ");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace block from legal description
		String block = ""; // can have multiple occurrences
		p = Pattern.compile("\\bBLK ([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1);
			legal = legal.replace(ma.group(0), "BLK ");
		}
		block = block.trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace unit from legal description
		p = Pattern.compile("\\b(?:UNIT|APT\\.?) #?(\\d+(?:-?[A-Z])?|[A-Z]\\d*)\\b");
		ma = p.matcher(legal);
		String unit = "";
		if (ma.find()) {
			unit = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
			legal = legal.replace(ma.group(0), "UNIT ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace building #
		p = Pattern.compile("\\bBLDG (\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
			legal = legal.replace(ma.group(0), "BLDG ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and remove section from legal description
		String sec = "";
		p = Pattern.compile("\\bSEC (\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			sec = sec + " " + ma.group(1);
			legal = legal.replace(ma.group(0), "");
		}
		sec = sec.trim();
		if (sec.length() != 0) {
			sec = LegalDescription.cleanValues(sec, true, true);
			m.put("PropertyIdentificationSet.SubdivisionSection", sec);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace phase from legal description
		p = Pattern.compile("\\bPH(?:ASE)? (\\d+[A-Z]?|I)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(1).replaceFirst("^I$", "1"));
			legal = legal.replace(ma.group(0), "PHASE ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace plat book & page from legal description
		List<List> bodyPIS = new ArrayList<List>();
		p = Pattern.compile("\\bPB (\\d+)\\s*(?:\\bPG\\b|-|\\s)\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			bodyPIS.add(line);
			legal = legal.replace(ma.group(0), "PB ");
		}
		p = Pattern.compile("\\bPLAT BK\\s*(\\d+) PG (\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			bodyPIS.add(line);
			legal = legal.replace(ma.group(0), "PB ");
		}
		if (!bodyPIS.isEmpty()) {
			String[] header = { "PlatBook", "PlatNo" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("PlatBook", new String[] { "PlatBook", "" });
			map.put("PlatNo", new String[] { "PlatNo", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(bodyPIS);
			pis.setMap(map);

			m.put("PropertyIdentificationSet", pis);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		patt = "(\\d+)\\s*[-\\s]\\s*(\\d+)";
		p = Pattern.compile("\\bORB?\\s*(" + patt + "(?: & " + patt + ")*)\\b");
		Pattern p2 = Pattern.compile(patt);
		ma = p.matcher(legal);
		while (ma.find()) {
			String orb = ma.group(1);
			Matcher ma2 = p2.matcher(orb);
			while (ma2.find()) {
				List<String> line = new ArrayList<String>();
				line.add(ma2.group(1));
				line.add(ma2.group(2));
				line.add("");
				bodyCR.add(line);
			}
			legal = legal.replace(ma.group(0), "OR ");
		}

		p = Pattern.compile("\\bOR\\s*(\\d+) P\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add("");
			bodyCR.add(line);
			legal = legal.replace(ma.group(0), "OR ");
		}

		if (!bodyCR.isEmpty()) {
			// add also the cross ref b&p extracted from XML specific tags, if
			// any
			String crb = (String) m.get("CrossRefSet.Book");
			if (crb == null) {
				crb = "";
			}
			String crp = (String) m.get("CrossRefSet.Page");
			if (crp == null) {
				crp = "";
			}
			String cri = (String) m.get("CrossRefSet.InstrumentNumber");
			if (cri == null) {
				cri = "";
			}
			if ((crb.length() != 0 && crp.length() != 0) || cri.length() != 0) {
				List<String> line = new ArrayList<String>();
				line.add(crb);
				line.add(crp);
				line.add(cri);
				bodyCR.add(line);
			}

			String[] header = { "Book", "Page", "InstrumentNumber" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });

			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);

			// m.put("CrossRefSet", cr); - DO NOT PARSE CROSSREF FROM LEGAL
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract subdivision name - only if lot or block or unit was extracted
		// or legal contains S/D
		String subdiv = "";
		boolean foundSubdiv = false;
		if (lot.length() == 0 && block.length() == 0 && unit.length() == 0) {
			p = Pattern.compile("(.*)\\s*\\bS/D\\b.*");
			ma = p.matcher(legal);
			foundSubdiv = ma.find();
		} else {
			patt = "(?:(?:LOT|BLK|UNIT)\\s*(?:\\s*[,;&\\s-]\\s*| OF )?)+\\s*(.*?)\\s*\\b(?:S/D|OR|PB|UNIT|UNRECD?|PHASE|CONDO(MINIUM)?)\\b.*";
			p = Pattern.compile("^" + patt);
			ma = p.matcher(legal);
			foundSubdiv = ma.find();
			if (!foundSubdiv) {
				p = Pattern.compile(".*\\b" + patt);
				ma = p.matcher(legal);
				foundSubdiv = ma.find();
			}
		}
		if (foundSubdiv) {
			subdiv = ma.group(1).trim();
		}
		if (subdiv.length() != 0) {
			subdiv = subdiv.replaceFirst("\\s*\\b\\d+(ST|ND|RD|TH) ADD(ITION)?\\b.*", "");
			subdiv = subdiv.replaceFirst("(\\bAND|&) A FRACTIONAL .*", "");
			subdiv = subdiv.replaceFirst("(\\bAND|&) INTEREST .*", "");
			subdiv = subdiv.replaceFirst(",.*", "");
			subdiv = subdiv.replaceFirst("\\bREVISED PLAT\\b.*", "");
			subdiv = subdiv.replaceFirst("\\bALLEY ABANDONED.*", "");
			subdiv = subdiv.replaceFirst("\\bDEG \\d+[SWEN]? .*", "");
			subdiv = subdiv.replaceFirst("\\b(FOR )?BEG\\b.*", "");
			subdiv = subdiv.replaceFirst("\\s*(\\bAND\\s*|&)$", "");
		}
		if (subdiv.length() != 0) {
			m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
			if (legal.matches(".*\\bCONDO(MINIUM)?\\b.*")) {
				m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			}
		}
	}

	public static void legalFLWaltonTR(ResultMap m, long searchId) throws Exception {
		FLWaltonTR.legalFLWaltonTR(m, searchId);
	}

	public static void legalFLSumterTR(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(legal))
			return;

		// initial corrections and cleanup of legal description
		legal = replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers

		legal = legal.replaceAll("\\s*\\bTHRU\\b\\s*", "-");
		// legal = legal.replaceAll("\\bESTATES?\\b", "");
		legal = legal.replaceAll("\\bCO-OP\\b", "");
		legal = legal.replaceAll("\\b\\d+(ST|ND|RD|TH) ADD( TO)?\\b", "");
		legal = legal.replaceAll("\\b(THE )?[NWSE]{1,2}(LY)? [\\d\\./\\s]+(\\s*\\bOF)?\\b", "");
		legal = legal.replaceAll("\\bFT( OF)?\\b", "");
		legal = legal.replaceAll("\\bRESUB( OF)?\\b", "");
		legal = legal.replaceAll("\\b(\\d+(ST|ND|RD|TH) )?REPLAT( OF)?\\b", "");
		legal = legal.replaceAll("\\b(REVISED|AMENDED) PLAT( OF)?\\b", "");
		legal = legal.replaceAll(",\\s*SUBJ TO [^,]+,?", "");
		legal = legal.replace(",", " ");
		legal = legal.replaceAll("\\s{2,}", " ").trim();

		String legalTemp = legal;

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		Pattern p = Pattern.compile("\\b(LOT)S? ([\\d\\s,&-]+|\\d+[A-Z])\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract block from legal description
		String block = "";
		p = Pattern.compile("\\b(BLK)S? ((?:\\b[A-Z]\\b|\\d+|&|\\s)+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(2);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " ");
		}
		block = block.replaceAll("\\s*&\\s*", " ").trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract unit from legal description
		p = Pattern.compile("\\b(UNIT) ((?:[A-Z]-?)?\\d+[A-Z]?(?:-\\d+)?)");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			m.put("PropertyIdentificationSet.SubdivisionUnit", ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract building #
		p = Pattern.compile("\\b(BLDG) ([A-Z]|\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract phase from legal description
		p = Pattern.compile("\\b(PH)(?:ASES?)? ([\\d\\s&-]+|\\d+[A-Z]?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			m.put("PropertyIdentificationSet.SubdivisionPhase", ma.group(2).replaceAll("\\s*&\\s*", " "));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// extract section from legal description
		p = Pattern.compile("\\b(SEC) (\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
			m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(2));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		// extract plat book and page
		p = Pattern.compile("\\b(PB)\\s*(\\d+)\\s*(PGS?)\\s*(\\d+(?:-\\d+[A-Z]?)?)\\b");
		ma.usePattern(p);
		// ma.reset();
		while (ma.find()) {
			/*
			 * List<String> line = new ArrayList<String>(); //line.add("");
			 * //line.add(""); line.add(ma.group(2)); line.add(ma.group(4));
			 * line.add(""); bodyCR.add(line);
			 */
			m.put("PropertyIdentificationSet.PlatBook", ma.group(2));
			m.put("PropertyIdentificationSet.PlatNo", ma.group(4));
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1) + " " + ma.group(3));
		}

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("\\b(OR) (\\d+)/(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add(ma.group(2));
			line.add(ma.group(3));
			line.add("");
			bodyCR.add(line);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		p = Pattern.compile("\\b(ORI) ([A-Z\\d]+)\\b");
		ma.usePattern(p);
		// ma.reset();
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(2));
			bodyCR.add(line);
			legalTemp = legalTemp.replaceFirst(ma.group(0), ma.group(1));
		}
		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "InstrumentNumber" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });

			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			m.put("CrossRefSet", cr);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		String subdiv = "";
		p = Pattern.compile(".*\\b(?:LOT|BLK|UNIT|PH|BLDG) (.+?) (?:UNIT|PB|PH|SEC(?! OF\\b)|ORI?)\\b.*");
		ma = p.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		} else {
			p = Pattern.compile(".*\\b(?:LOT|BLK|UNIT|PH|BLDG) (.+)");
			ma.usePattern(p);
			ma.reset();
			if (ma.find()) {
				subdiv = ma.group(1);
			}
		}
		if (subdiv.length() != 0) {
			if (subdiv.matches(".*\\b[SEWN]-\\d+-\\d+-\\d+.*")) {
				subdiv = ""; // 0072-14-0027 for intermediate results parser
			}
			subdiv = subdiv.replaceFirst("^\\s*OF\\b\\s*", "");
			subdiv = subdiv.replaceFirst("(.+) CONDO(MINIUM)?.*", "$1");
			subdiv = subdiv.replaceFirst("(.+) SUB(DIVISION)?.*", "$1");
			subdiv = subdiv.replaceFirst("^\\d+\\s", "");
			subdiv = subdiv.replaceAll("\\bPB\\b", "");
			subdiv = subdiv.replaceAll("\\bPG\\b", "");
			// subdiv = subdiv.replaceFirst("\\s\\d+$", "");
			subdiv = subdiv.trim();
			if (subdiv.length() != 0) {
				m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
				if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
					m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
			}
		}
	}

	public static void stdFLLakeTR(ResultMap m, long searchId) throws Exception {

		String s = (String) m.get("tmpOwnerName");

		if (s == null || s.length() == 0)
			return;

		s = cleanOwnerFLOsceolaTR(s);

		if (!s.matches("[A-Z]{2,}(-[A-Z]{2,})?( (JR|SR|II|III))? [A-Z]+( [A-Z]+)?( [A-Z])?")) {

			if (!s.contains("&")) {
				s = s.replaceFirst("^(([A-Z]{2,})\\b.*)\\b([A-Z]{2,}\\-\\2)\\b", "$1& $3");
			}
			if (!s.contains("&")) {
				s = s.replaceFirst("^(([A-Z]{2,})\\b.*)\\b(\\2)\\b", "$1& $3");
			}
			if (!s.contains("&")) {
				s = s.replaceFirst("\b([A-Z]) ([A-Z]) ([A-Z]+)", "$3" + "$1" + "$2");
			}
			if (!s.contains("&")) {
				s = s.replaceFirst("([A-Z]+(?: (?:JR|SR|II|III))? [A-Z]+(?: [A-Z])?(?: [A-Z])?) ([A-Z]{2,}(?: (?:JR|SR|II|III))? [A-Z]+.*)", "$1 & $2");
			}
		}

		String[] a = StringFormats.parseNameNashville(s);

		a[4] = a[4].replaceFirst("^([A-Z]) [A-Z]{2,} [A-Z]{2,}( [A-Z])?.+", "$1");

		m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
		m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
		m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
		m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
		m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
		m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
		s = (String) m.get("PropertyIdentificationSet.ParcelID");
		if (s != null) {
			m.put("PropertyIdentificationSet.ParcelID", s.replaceAll("(?is)[^0-9]+", ""));
		}
	}

	public static void stdFLOkaloosaTR(ResultMap m, long searchId) throws Exception {

		String s = (String) m.get("tmpOwnerName");

		if (s == null || s.length() == 0)
			return;

		s = cleanOwnerFLOsceolaTR(s);

		if (!s.matches("[A-Z]{2,}(-[A-Z]{2,})?( (JR|SR|II|III))? [A-Z]+( [A-Z]+)?( [A-Z])?")) {

			if (!s.contains("&")) {
				s = s.replaceFirst("^(([A-Z]{2,})\\b.*)\\b([A-Z]{2,}\\-\\2)\\b", "$1& $3");
			}
			if (!s.contains("&")) {
				s = s.replaceFirst("^(([A-Z]{2,})\\b.*)\\b(\\2)\\b", "$1& $3");
			}
			if (!s.contains("&")) {
				s = s.replaceFirst("\b([A-Z]) ([A-Z]) ([A-Z]+)", "$3" + "$1" + "$2");
			}
			if (!s.contains("&")) {
				s = s.replaceFirst("([A-Z]+(?: (?:JR|SR|II|III))? [A-Z]+(?: [A-Z])?(?: [A-Z])?) ([A-Z]{2,}(?: (?:JR|SR|II|III))? [A-Z]+.*)", "$1 & $2");
			}
		}

		String[] a = StringFormats.parseNameNashville(s);

		a[4] = a[4].replaceFirst("^([A-Z]) [A-Z]{2,} [A-Z]{2,}( [A-Z])?.+", "$1");

		m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
		m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
		m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
		m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
		m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
		m.put("PropertyIdentificationSet.SpouseLastName", a[5]);
	}

	protected static String tokenizeSuffixFLOkaloosaTR(String s) {
		s = s.replaceAll("\\A(\\d+)(.+)", "$1 $2");
		s = s.replaceAll("HWY", "HIGHWAY");
		s = s.replaceAll(",", "");
		s = s.replaceAll("CI\\s+R", "CIR");
		s = s.replaceAll("BL\\s+VD", "BLVD");
		s = s.replaceAll("AV\\s+E", "AVE");
		s = s.replaceAll("WA\\s+Y", "WAY");
		s = s.replaceAll("LO\\s+OP", "LOOP");
		s = s.replaceAll("PA\\s+RK", "PARK");
		return s;
	}

	public static void pidForRVGenericDASLNDB(ResultMap m, long searchId) throws Exception {

		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
		if (crtState.equals("FL")) {
			if (crtCounty.equals("Volusia")) {
				// FIX for B2440
				String pid2 = (String) m.get("PropertyIdentificationSet.ParcelID");
				if (pid2 == null || pid2.length() == 0 || pid2.length() < 12)
					return;
				pid2 = pid2.substring(4);
				String tw = (String) m.get("PropertyIdentificationSet.SubdivisionTownship");
				if (tw == null || tw.length() == 0 || tw.length() < 2)
					return;
				String rg = (String) m.get("PropertyIdentificationSet.SubdivisionRange");
				if (rg == null || rg.length() == 0 || rg.length() < 2)
					return;
				String sc = (String) m.get("PropertyIdentificationSet.SubdivisionSection");
				if (sc == null || sc.length() == 0 || sc.length() < 2)
					return;
				pid2 = rg.substring(0, 2) + tw.substring(0, 2) + sc + pid2;
				m.put("PropertyIdentificationSet.ParcelID2", pid2);
			} else {
				if (crtCounty.equals("Hernando")) {
					String pid = (String) m.get("PropertyIdentificationSet.ParcelID");
					if (pid == null) {
						return;
					}
					pid = pid.replaceFirst("^R", "");
					if (pid.length() != 19) {
						return;
					}
					m.put("PropertyIdentificationSet.ParcelID2", pid.substring(3, 7) + pid.substring(0, 2) + pid.substring(7));
				}
			}
		}
	}

	public static void formatPIDforNB(ResultMap m, long searchId) {
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
		String pid = org.apache.commons.lang.StringUtils.defaultString((String) m.get("PropertyIdentificationSet.ParcelID"));
		if (crtState.equals("OH")) {
			if (crtCounty.equals("Franklin")) {
				pid = formatPIDforNB_OHFranklin(pid);
				if (StringUtils.isNotEmpty(pid)) {
					m.put("PropertyIdentificationSet.ParcelID", pid);
				}
			}
		}
	}

	public static String formatPIDforNB_OHFranklin(String pid) {
		String formattedPID = new String();
		Pattern pidPattern = Pattern.compile("^(\\d{3})-?(\\d{6})-?(\\d{2})?");
		Matcher pidMatcher = pidPattern.matcher(pid);
		if (pidMatcher.find()) {
			formattedPID = pidMatcher.group(1) + "-" + pidMatcher.group(2) + (pidMatcher.group(3) == null ? "-00" : "-" + pidMatcher.group(3));
		}
		return formattedPID;
	}

	public static void interFLHernandoTR(ResultMap m, long searchId) throws Exception {

		FLHernandoTR.interFLHernandoTR(m, searchId);
	}

	/**
	 * Adds information to a vector infset Intended to be used when the vector
	 * is not already attached to map, oly separate elements
	 * 
	 * @param m
	 * @param name
	 * @param fields
	 * @param body
	 * @throws Exception
	 */
	protected static void addVectInfo2Map(ResultMap m, String name, String[] fields, List<List> body) throws Exception {

		if (body.size() == 0) {
			return;
		}

		if (m.get(name) != null) {
			// throw new
			// RuntimeException("This method does not overwrite an existing vector");
			logger.error("This method does not overwrite an existing vector");
			return;
		}

		// add the initial values to the list
		boolean empty = true;
		for (String field : fields) {
			if (!StringUtils.isEmpty((String) m.get(name + "." + field))) {
				empty = false;
				break;
			}
		}
		if (!empty) {
			List<String> line = new ArrayList<String>();
			for (String field : fields) {
				String val = (String) m.get(field);
				if (val == null) {
					val = "";
				}
				line.add(val);
			}
			body.add(line);
		}

		// upload the list into map
		if (body.size() == 1) {
			for (int i = 0; i < fields.length; i++) {
				m.put(name + "." + fields[i], (String) body.get(0).get(i));
			}
		} else {
			Map<String, String[]> map = new HashMap<String, String[]>();
			for (String field : fields) {
				map.put(field, new String[] { field, "" });
			}
			ResultTable rt = new ResultTable();
			rt.setHead(fields);
			rt.setBody(body);
			rt.setMap(map);
			m.put(name, rt);
		}
	}

	public static String cleanLegalFLEscambiaDASLRV(String legal) {

		// A LOT 60 FT WIDE BY 580 FT LONG
		legal = legal.replaceAll("A LOT \\d+ FT WIDE BY \\d+ FT LONG", "");
		// distances
		legal = legal.replaceAll("(\\b[ESNW] )?\\d+( \\d+/\\d+)? FT( \\d+ IN)?( TO)?\\b", " DISTANCE ");
		// 1 TO 3
		legal = legal.replaceAll("\\b(\\d+) TO (\\d+)\\b", "$1-$2");
		// 86/A/B/C
		legal = legal.replaceAll("\\b(\\d+)/A/B/C\\b", "$1A $1B $1C");
		// 45A&B
		legal = legal.replaceAll("(\\d+)A[&/]+B\\b", "$1A $1B");
		// UNIT B-602 | B 602
		legal = legal.replaceAll("\\bUNIT ([A-Z])[- ](\\d+)\\b", "UNIT $1$2");
		// UNIT 602-B | 602 B
		legal = legal.replaceAll("\\bUNIT (\\d+)[- ]([A-Z])\\b", "UNIT $1$2");
		// AND
		legal = legal.replaceAll("\\b(\\d+) AND (\\d+)\\b", "$1&$2");
		// unuseful at the end
		// legal = legal.replaceAll("\\b(CA|SHEET) ([A-Z0-9/&-]+){1,2}$", "");
		// S/D
		legal = legal.replaceAll("S/D ", "");
		legal = legal.replaceAll(" S/D", "");
		//
		legal = legal.replace("}", "-");
		//
		legal = legal.replaceAll("\\b([0-9]+)-([A-Z])\\b", "$1$2");
		legal = legal.replaceAll("\\b([A-Z])-([0-9]+)\\b", "$1$2");

		//
		legal = legal.replaceAll("\\b(1ST|2ND|3RD|4TH) ADDN\\b", "");

		// numbers
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, new String[] { "I", "M", "C", "L", "D" });
		legal = replaceNumbers(legal);

		// S 74 DEG 8 MIN N
		legal = legal.replaceAll("(([NESW] )?\\d+ (DEG|MIN)( \\d+ SEC)?( [NESW])?)+( \\d+( \\d+/\\d+)?(?! (MIN|SEC|DEG)))?", " @ ");

		// LOT 3LESS
		// LOT22
		legal = legal.replaceAll("LOTS? ?(\\d+)([A-Z])", "LOT $1 $2");
		legal = legal.replaceAll("LOTS?(\\d+)\\b", "LOT $1 ");

		// trim and implode spaces
		legal = legal.replaceAll("\\bDISTANCE\\b", " @ ");
		legal = legal.replaceAll("\\s{2,}", " ").trim();

		return legal;
	}

	@SuppressWarnings("unchecked")
	public static void legalFLEscambiaDASLRV(ResultMap m, long searchId) throws Exception {

		String NUMBER_SEQ = "([0-9]+[A-Z]?([ &-]+[0-9]+[A-Z]?)*\\b)";
		String LETTER_SEQ = "([A-Z][0-9]*([ &-]+[A-Z][0-9]*)*\\b)";

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(legal)) {
			return;
		}

		// cleanup the legal string
		legal = cleanLegalFLEscambiaDASLRV(legal);

		/*
		 * LOT parsing
		 */
		String lot = "";
		Pattern pat = Pattern.compile("\\b(LOTS?|LTS?) (" + NUMBER_SEQ + "|" + LETTER_SEQ + ")");
		Matcher mat = pat.matcher(legal);
		while (mat.find()) {
			lot = lot + " " + mat.group(2);
			legal = legal.replaceFirst(mat.group(0), " @ ");
		}
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		String legalTemp = legal.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		/*
		 * PLAT parsing
		 */
		// Single book case
		pat = Pattern.compile("PB (\\d+) P ((?:\\d+[A-Z]?[& /-]*)+)(?:[^A-Z]|\\b)\\b");
		mat = pat.matcher(legal);
		List<List> bodyPis = new ArrayList<List>();
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");

			String book = mat.group(1);
			String page = mat.group(2).replaceAll("[/&]", " ").replaceAll("\\s{2,}", " ").trim();
			page = page.replaceAll(" -", "").trim();

			List<String> line = new ArrayList<String>();
			line.add(book);
			line.add(page);
			bodyPis.add(line);

			legalTemp = legalTemp.replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// PBI case
		pat = Pattern.compile("PB[IS] (\\d+)[-} ]((?:\\d+[A-Z]?[& /-]*)+)(?:[^A-Z]|\\b)\\b");
		mat = pat.matcher(legal);
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");

			String book = mat.group(1);
			String page = mat.group(2).replaceAll("[/&]", " ").replaceAll("\\s{2,}", " ").trim();
			page = page.replaceAll(" -", "").trim();

			List<String> line = new ArrayList<String>();
			line.add(book);
			line.add(page);
			bodyPis.add(line);

			legalTemp = legalTemp.replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// Double book case
		pat = Pattern.compile("PB (\\d+)\\s*/\\s*(\\d+) P\\s*(\\d+)\\s*/\\s*(\\d+)\\b");
		mat = pat.matcher(legal);
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");

			// add lines to map

			List<String> line1 = new ArrayList<String>();
			line1.add(mat.group(1));
			line1.add(mat.group(3));
			bodyPis.add(line1);
			List<String> line2 = new ArrayList<String>();
			line2.add(mat.group(2));
			line2.add(mat.group(4));
			bodyPis.add(line2);

			legalTemp = legalTemp.replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		// upload into map
		addVectInfo2Map(m, "PropertyIdentificationSet", new String[] { "PlatBook", "PlatNo" }, bodyPis);

		/*
		 * Cross reference parsing
		 */
		List<List> bodyCr = new ArrayList<List>();
		// single book case
		pat = Pattern.compile("(DB|OR|MB|BK|D[ ]?BK) (?:BK? ?)?(\\d+) ?[,]? ?(?:PP?|PG|PAGE) ?(\\d+(?:/\\d+)*)");
		mat = pat.matcher(legal);
		while (mat.find()) {

			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");

			String type = mat.group(1);
			if (type == null || "BK".equals(type) || "DBK".equals(type.replaceAll("\\s+", ""))) {
				type = "";
			}
			String book = mat.group(2);
			String page = mat.group(3).replaceAll("[&/]", "").replaceAll("\\s{2,}", " ").trim();
			List<String> line = new ArrayList<String>();
			line.add(book);
			line.add(page);
			line.add("");
			line.add(type);
			bodyCr.add(line);
		}
		legalTemp = legalTemp.replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// double book case
		pat = Pattern.compile("(DB|OR) (\\d+)/(\\d+) P (\\d+)/(\\d+)");
		mat = pat.matcher(legal);
		while (mat.find()) {

			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");

			String type = mat.group(1);
			String book1 = mat.group(2);
			String book2 = mat.group(3);
			String page1 = mat.group(4);
			String page2 = mat.group(5);
			List<String> line = new ArrayList<String>();
			line.add(book1);
			line.add(page1);
			line.add("");
			line.add(type);
			bodyCr.add(line);
			line = new ArrayList<String>();
			line.add(book2);
			line.add(page2);
			line.add("");
			line.add(type);
			bodyCr.add(line);

		}
		legalTemp = legalTemp.replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// upload into map
		// addVectInfo2Map(m, "CrossRefSet", new String[]{"Book", "Page",
		// "InstrumentNumber", "Book_Page_Type"}, bodyCr); - DO NOT PARSE
		// CROSSREF FROM LEGAL

		/*
		 * BLOCK parsing
		 */
		String block = "";
		String LETTER_SEQ2 = "([A-Z]{1,2}[0-9]*([ &-]+[A-Z][0-9]*)*\\b)";
		pat = Pattern.compile("(?:BLOCKS?|BLKS?) ?(" + NUMBER_SEQ + "|" + LETTER_SEQ + "|" + LETTER_SEQ2 + ")");
		mat = pat.matcher(legal);
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");
			block = block + " " + mat.group(1);

			legalTemp = legalTemp.replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		block = block.trim().replaceAll("&", "").replaceAll("\\s{2,}", " ").trim();
		block = LegalDescription.cleanValues(block, false, true);
		m.put("PropertyIdentificationSet.SubdivisionBlock", block);

		/*
		 * UNIT parsing
		 */
		String unit = "";

		pat = Pattern.compile("(?:UNITS?) (?:NO |# ?)? ?(" + NUMBER_SEQ + "|" + LETTER_SEQ + ")");
		mat = pat.matcher(legal);
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");
			unit = unit + " " + mat.group(1);
			legalTemp = legalTemp.replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		unit = unit.trim().replaceAll("&", "").replaceAll("\\s{2,}", " ").trim();
		unit = LegalDescription.cleanValues(unit, false, true);
		m.put("PropertyIdentificationSet.SubdivisionUnit", unit);

		/*
		 * S/T/R parsing
		 */
		pat = Pattern.compile("SEC ((?:\\d+)(?:/\\d+)*) T? ?((?:\\d+)(?:/\\d+)* ?[NESW]) R? ?((?:\\d+)(?:/\\d+)* ?[NESW]?)");
		mat = pat.matcher(legal);
		if (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");

			String s = mat.group(1);
			s = s.replaceAll("\\s", "").replaceAll("/", " ");
			String t = mat.group(2);
			t = t.replaceAll("\\s", "").replaceAll("/", " ");
			String r = mat.group(3);
			r = r.replaceAll("\\s", "").replaceAll("/", " ");
			m.put("PropertyIdentificationSet.SubdivisionSection", s);
			m.put("PropertyIdentificationSet.SubdivisionTownship", t);
			m.put("PropertyIdentificationSet.SubdivisionRange", r);
			legalTemp = legalTemp.replaceAll("\\s{2,}", " ").trim();
			legal = legalTemp;
		}

		// SEC 8-31-39
		pat = Pattern.compile("SEC ?(\\d+)-(\\d+)-(\\d+)");
		mat = pat.matcher(legal);
		if (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");

			String s = mat.group(1);
			s = s.replaceAll("\\s", "").replaceAll("/", " ");
			String t = mat.group(2);
			t = t.replaceAll("\\s", "").replaceAll("/", " ");
			String r = mat.group(3);
			r = r.replaceAll("\\s", "").replaceAll("/", " ");
			m.put("PropertyIdentificationSet.SubdivisionSection", s);
			m.put("PropertyIdentificationSet.SubdivisionTownship", t);
			m.put("PropertyIdentificationSet.SubdivisionRange", r);
			legalTemp = legalTemp.replaceAll("\\s{2,}", " ").trim();
			legal = legalTemp;
		}

		/*
		 * PHASE parsing
		 */
		pat = Pattern.compile("\\b(PH)(?:ASES?)? (" + NUMBER_SEQ + "|" + LETTER_SEQ + ")");
		mat = pat.matcher(legal);
		if (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0).trim(), " @ ");
			m.put("PropertyIdentificationSet.SubdivisionPhase", mat.group(2).replaceAll("\\s*&\\s*", " "));
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		/*
		 * BLDG parsing
		 */
		legal = legal.replaceFirst("BLK/BLDG$", "");
		pat = Pattern.compile("BLDGS? (" + NUMBER_SEQ + "|" + LETTER_SEQ + ")");
		mat = pat.matcher(legal);
		String bldg = "";
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0).trim(), " @ ");
			bldg = bldg + " " + mat.group(1).replaceAll("\\s*&\\s*", " ");
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		if (!StringUtils.isEmpty(bldg)) {
			bldg = LegalDescription.cleanValues(bldg, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
		}

		/*
		 * TRACT parsing
		 */
		pat = Pattern.compile("(?:TRACTS?|\\bTRS?) (" + NUMBER_SEQ + "|" + LETTER_SEQ + ")");
		mat = pat.matcher(legal);
		String tract = "";
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0).trim(), " @ ");
			tract = tract + " " + mat.group(1).replaceAll("\\s*&\\s*", " ");
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		if (!StringUtils.isEmpty(tract)) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}

		m.put("tmpRemainingLegal", legal);
	}

	public static String cleanRemarksFLEscambiaDASLRV(String legal) {

		// BLDG U-1
		legal = legal.replaceAll("\\b(LOTS?|BLOCKS?|BLKS?|APTS?|BLDGS?|PH|PHASES?|UNITS?) ([A-Z])-([0-9]+)\\b", "$1 $2$3");
		legal = legal.replaceAll("\\b(LOTS?|BLOCKS?|BLKS?|APTS?|BLDGS?|PH|PHASES?|UNITS?) ([0-9]+)-([A-Z])\\b", "$1 $2$3");
		// 01 306 CP 03
		// 05 3110 000 000/$176.00
		legal = legal.replaceAll("  \\d{2} \\d+ CP \\d+( [A-Z])?$", "");
		// 2003 CA 921
		legal = legal.replaceAll("  (\\d+ )?\\d+ (CA|OF)( \\d+)?$", "");
		//
		legal = legal.replaceAll("  F/S$", "");

		legal = legal.replaceAll("\\d+ \\d+ \\d+ \\d+/\\$\\d+(\\.\\d+)?", "");
		// numbers
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, new String[] { "I", "M", "C", "L", "D" });
		legal = replaceNumbers(legal);
		// OR 3152/3180/3185/3315 PG 592/915/771/857
		legal = legal.replaceAll("OR (\\d+)/(\\d+)/(\\d+)/(\\d+) (?:PG?|PAGES?) (\\d+)/(\\d+)/(\\d+)/(\\d+)", "OR $1/$2 PG $5/$6 OR $3/$4 PG $7/$8");
		// OR 3152/3180/3185 PG 592/915/771
		legal = legal.replaceAll("OR (\\d+/\\d+)/(\\d+) (?:PG?|PAGES?) (\\d+/\\d+)/(\\d+)", "OR $1 PG $3 OR $2 PG $4");
		// OR 3152/3180 PG 592/915
		legal = legal.replaceAll("OR (\\d+)/(\\d+) (?:PG?|PAGES?) (\\d+)/(\\d+)", "OR $1 PG $3 OR $2 PG $4");
		// 1 THU 5
		legal = legal.replaceAll("\\b(\\d+) (?:THRU|-) (\\d+)\\b", "$1-$2");
		// 97-0278-CA
		legal = legal.replaceAll("\\b\\d+-\\d+-CA(-\\d+)?\\b", "");
		// UNIT j-19
		legal = legal.replaceAll("\\bUNIT ([A-Z])-(\\d+)\\b", "UNIT $1$2");
		// PH-7
		legal = legal.replaceAll("(PH|UN)-(\\d+)", "$1 $2");
		// trim and implode spaces
		legal = legal.replaceAll("\\s{2,}", " ").trim();
		// 16 AND 17
		legal = legal.replaceAll("(\\d+)\\s*AND\\s*(\\d+)", "$1 $2");

		return legal;
	}

	@SuppressWarnings("unchecked")
	public static void legalRemarksFLEscambiaRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(legal)) {
			return;
		}
		legal = cleanRemarksFLEscambiaDASLRV(legal);
		String legalTemp = legal;

		/*
		 * S/T/R Parsing
		 */
		List<List> body = getSTRFromMap(m); // first add sec-twn-rng extracted
											// from XML specific tags, if any
											// (for DT use)
		Pattern pat = Pattern.compile("\\b(?:SEC )?(\\d{1,2})(?: TW[NP] |-)(\\d{1,2}[NESW]?)(?: RNG |-)(\\d{1,2}[NESW]?)");
		Matcher mat = pat.matcher(legal);
		if (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");
			List<String> line = new ArrayList<String>();
			line.add(mat.group(1));
			line.add(mat.group(2));
			line.add(mat.group(3));
			body.add(line);
			legalTemp = legalTemp.replaceAll("\\s{2,}", " ").trim();
			legal = legalTemp;
		}
		saveSTRInMap(m, body);
		/*
		 * LOT parsing
		 */
		String lot = "";
		String NUMBER_SEQ = "([0-9]+[A-Z]?([ &,-]+[0-9]+[A-Z]?)*\\b)";
		String LETTER_SEQ = "([A-Z][0-9]*([ &,-]+[A-Z][0-9]*)*\\b)";
		pat = Pattern.compile("\\b(LOTS?|LTS?) (" + NUMBER_SEQ + "|" + LETTER_SEQ + ")");
		mat = pat.matcher(legal);
		while (mat.find()) {
			lot = lot + " " + mat.group(2);
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");
		}
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			String prevLot = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (prevLot != null) {
				lot = prevLot + " " + lot;
			}
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		/*
		 * CR parsing
		 */
		// OR576 551, 868 2370, 905 2414, 912 1486, 915 2128,
		pat = Pattern.compile("OR(\\d+ \\d+(?:, *\\d+ \\d+)+)");
		mat = pat.matcher(legal);
		while (mat.find()) {
			String cr = "OR " + mat.group(1).replaceAll(",", " OR ");
			cr = cr.replaceAll("\\s{2,}", " ").trim();
			legalTemp = legalTemp.replaceFirst(mat.group(0), cr);
		}
		legal = legalTemp;

		List<List> bodyCr = new ArrayList<List>();
		pat = Pattern.compile("(\\d{10} ?)?(OR|DB) ?(?:BK )?(\\d+)\\s*(?:PG?|PAGES?|PGS?|/| )\\s*(\\d+)\\b");
		mat = pat.matcher(legal);
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");
			String nr = mat.group(1);
			if (nr == null) {
				nr = "";
			}
			String type = mat.group(2);
			String book = mat.group(3);
			String page = mat.group(4);
			List<String> line = new ArrayList<String>();
			line.add(book);
			line.add(page);
			line.add(nr);
			line.add(type);
			bodyCr.add(line);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		pat = Pattern.compile("\\b(\\d{10})\\b");
		mat = pat.matcher(legal);
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");
			String nr = mat.group(1);
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(nr);
			line.add("");
			bodyCr.add(line);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// upload into map
		// addVectInfo2Map(m, "CrossRefSet", new String[]{"Book", "Page",
		// "InstrumentNumber", "Book_Page_Type"}, bodyCr);
		saveCRInMap(m, bodyCr);

		/*
		 * BLOCK parsing
		 */
		pat = Pattern.compile("(?:BLOCKS?|BLKS?|BL) (" + NUMBER_SEQ + "|" + LETTER_SEQ + ")");
		mat = pat.matcher(legal);
		String block = "";
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");
			block += mat.group(1) + " ";
		}
		legalTemp = legalTemp.replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		block = LegalDescription.cleanValues(block, false, true);
		if (!StringUtils.isEmpty(block)) {
			String prevBlk = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (prevBlk != null) {
				block = prevBlk + " " + block;
			}
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block.trim());
		}

		/*
		 * UNIT parsing
		 */
		pat = Pattern.compile("(?:UNITS?|\\bUN|APTS?) (?:NO? |# ?)?(" + NUMBER_SEQ + "|" + LETTER_SEQ + ")");
		mat = pat.matcher(legal);
		String unit = "";
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");
			unit = unit + " " + mat.group(1);
			legalTemp = legalTemp.replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		unit = LegalDescription.cleanValues(unit, false, true);
		if (!StringUtils.isEmpty(unit)) {
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
		}

		/*
		 * PHASE parsing
		 */
		String phase = "";
		pat = Pattern.compile("\\b(PH)(?:ASES?)? (" + NUMBER_SEQ + "|" + LETTER_SEQ + ")");
		// pat =
		// Pattern.compile("\\b(PH)(?:ASES?)? ([\\d\\s&-]+|\\d+[A-Z]?|[A-Z])\\b");
		mat = pat.matcher(legal);
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0).trim(), " @ ");
			phase = phase + " " + mat.group(2).replaceAll("\\s*&\\s*", " ");
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		phase = LegalDescription.cleanValues(phase, false, true);
		if (!StringUtils.isEmpty(phase)) {
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
		}
		/*
		 * PLAT parsing
		 */
		// Single book case
		pat = Pattern.compile("PB\\s*(\\d+) PG?\\s*((?:\\d+[A-Z]?[& /-]*)+)(?:[^A-Z]|\\b)\\b");
		mat = pat.matcher(legal);
		if (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");

			m.put("PropertyIdentificationSet.PlatBook", mat.group(1));
			String no = mat.group(2).replaceAll("[/&]", " ").replaceAll("\\s{2,}", " ").trim();
			m.put("PropertyIdentificationSet.PlatNo", no);

			legalTemp = legalTemp.replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

		/*
		 * BLDG parsing
		 */
		pat = Pattern.compile("BLDGS? (" + NUMBER_SEQ + "|" + LETTER_SEQ + ")");
		mat = pat.matcher(legal);
		String bldg = "";
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0).trim(), " @ ");
			bldg = bldg + " " + mat.group(1).replaceAll("\\s*&\\s*", " ");
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		if (!StringUtils.isEmpty(bldg)) {
			bldg = LegalDescription.cleanValues(bldg, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
		}

		/*
		 * TRACT parsing
		 */
		pat = Pattern.compile("(?:TRACTS?|\\bTRS?) (" + NUMBER_SEQ + "|" + LETTER_SEQ + ")");
		mat = pat.matcher(legal);
		String tract = "";
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0).trim(), " @ ");
			tract = tract + " " + mat.group(1).replaceAll("\\s*&\\s*", " ");
			legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		if (!StringUtils.isEmpty(tract)) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}

		m.put("tmpRemainingLegal", legal);
	}

	public static void stdFLStLucieTR(ResultMap m, long searchId) throws Exception {
		String s = (String) m.get("tmpOwner");
		String[] tmpName = null;

		if (s != null) {
			s = s.replaceAll(",", "");
			if ((s != null) && (s.length() > 0)) {
				tmpName = s.split(" ");
				if (tmpName.length > 1) {
					m.put("PropertyIdentificationSet.OwnerFirstName", tmpName[1]);
				}
				if (tmpName.length > 2) {
					m.put("PropertyIdentificationSet.OwnerMiddleName", tmpName[2]);
				}
				if (tmpName.length > 0) {
					m.put("PropertyIdentificationSet.OwnerLastName", tmpName[0]);
				}
			}
		}
		s = (String) m.get("tmpPropAddress");
		if (s != null) {
			if (s.length() > 0) {
				s = s.replaceAll(",", "");
				tmpName = s.split(" ");
				// m.put("PropertyIdentificationSet.StreetNo", adresa[0]);
				// m.put("PropertyIdentificationSet.StreetName",
				// adresa[1].trim());

				if (tmpName.length > 1) {
					if (tmpName[1].length() > 2) {
						m.put("PropertyIdentificationSet.StreetName", tmpName[1]);
					}
				}
				if (tmpName.length > 2) {
					if (tmpName[1].length() < 3) {
						m.put("PropertyIdentificationSet.StreetName", tmpName[2]);
					}
				}
				if (tmpName.length > 0) {
					m.put("PropertyIdentificationSet.StreetNo", tmpName[0].replaceAll("(?is)[^0-9]+", ""));
				}
			}
		}

	}

	// applied for this kind of string: Nov 30, 2008 9,657.53@@Dec 31, 2008
	// 9,758.13@@Jan 31, 2009 9,858.73@@Feb 28, 2009 9,959.33
	public static String extractAmountFromArray(String amount) throws Exception {

		if (amount == null) {
			return "";
		}

		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
		Calendar now = Calendar.getInstance();
		String t = sdf.format(now.getTime()).toString();
		now.setTime(sdf.parse(t));
		Calendar dat = Calendar.getInstance();

		amount = amount.replaceAll("(?is)([a-z]+)([\\s]+)([0-9,]+)([\\s]+)([0-9]+)([\\s]+)([\\s])([\\d,.]+)", "$1" + "$2" + "$3" + "$4" + "$5" + "&" + "$8");
		String[] lines = amount.split("@@");
		String correctAmount = "";

		for (int i = 0; i < lines.length; i++) {
			String partDat = lines[i].replaceFirst("(?is)([^&]+)(.*)", "$1").trim();
			if (!partDat.matches("(?i)^.*?(tax\\s+Deed|Date\\s+\\d+/\\d+/d+|\\bBidder|#\\s*\\d{3,}).*$")) {
				dat.setTime(sdf.parse(partDat));
				if (dat.after(now) || dat.equals(now)) {
					correctAmount = lines[i].replaceFirst("(?is)([^&]+)([&])(.*)", "$3");
					correctAmount = correctAmount.replaceAll("[,]", "");
					break;
				}
			}
		}
		return correctAmount;
	}

	public static void updateInstrumentNumberRV(ResultMap m, long searchId) throws Exception {
		String instrNo = (String) m.get("tmpWholeInstrumentNumber");
		if (!org.apache.commons.lang.StringUtils.isEmpty(instrNo)) {
			String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
			String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
			if ("FL".equals(crtState))
				if ("Seminole".equalsIgnoreCase(crtCounty))
					m.put("SaleDataSet.InstrumentNumber", instrNo);
		}
	}

	public static void interFLOsceolaTR(ResultMap m, long searchId) throws Exception {
		String s = (String) m.get("tmpowner");
		String[] tmpName = null;

		if (s != null) {
			s = s.replaceAll(",", "");
			if ((s != null) && (s.length() > 0)) {
				tmpName = s.split(" ");
				if (tmpName.length > 1) {
					m.put("PropertyIdentificationSet.OwnerFirstName", tmpName[1]);
				}
				if (tmpName.length > 2) {
					m.put("PropertyIdentificationSet.OwnerMiddleName", tmpName[2]);
				}
				if (tmpName.length > 0) {
					m.put("PropertyIdentificationSet.OwnerLastName", tmpName[0]);
				}
			}
		}
		s = (String) m.get("tmpPropAddress");
		if (s != null) {
			if (s.length() > 0) {
				s = s.replaceAll(",", "");
				tmpName = s.split(" ");
				// m.put("PropertyIdentificationSet.StreetNo", adresa[0]);
				// m.put("PropertyIdentificationSet.StreetName",
				// adresa[1].trim());

				if (tmpName.length > 1) {
					if (tmpName[1].length() > 2) {
						m.put("PropertyIdentificationSet.StreetName", tmpName[1]);
					}
				}
				if (tmpName.length > 2) {
					if (tmpName[1].length() < 3) {
						m.put("PropertyIdentificationSet.StreetName", tmpName[2]);
					}
				}
				if (tmpName.length > 0) {
					m.put("PropertyIdentificationSet.StreetNo", tmpName[0].replaceAll("(?is)[^0-9]+", ""));
				}
			}
		}
		s = (String) m.get("PropertyIdentificationSet.ParcelID");
		if (s != null) {
			String s1 = s.substring(0, 1).replaceAll("(?is)[^a-zA-Z]+", "");
			if ("".equals(s1)) {
				m.put("PropertyIdentificationSet.ParcelID", s.replaceAll("(?is)[^\\W]+", ""));
			} else {
				m.put("PropertyIdentificationSet.ParcelID", s.substring(1).replaceAll("(?is)[^0-9A-Za-z]+", ""));
			}
		}
	}

	public static void legalFLCollierTR(ResultMap m, long searchId) throws Exception {
		FLCollierTR.legalFLCollierTR(m, searchId);
	}

	public static void taxFLCollierTR(ResultMap m, long searchId) throws Exception {
		FLCollierTR.taxFLCollierTR(m, searchId);
	}

	public static void stdFinalFLCollierTR(ResultMap m, long searchId) throws Exception {
		FLCollierTR.stdFinalFLCollierTR(m, searchId);
	}

	public static void partyNamesFLCollierTR(ResultMap m, long searchId) throws Exception {
		FLCollierTR.partyNamesFLCollierTR(m, searchId);
	}

	public static void interFLLakeTR(ResultMap m, long searchId) throws Exception {
		String s = (String) m.get("tmpOwnerName");
		if (s != null) {
			s = s.replaceAll("(?is)\\s+", " ");
			String[] ownerName = s.split("&");
			String[] name = null;
			if (ownerName.length > 0) {
				name = ownerName[0].split(" ");
				if (name.length > 1) {
					m.put("PropertyIdentificationSet.OwnerFirstName", name[1]);
				}
				if (name.length > 2) {
					m.put("PropertyIdentificationSet.OwnerMiddleName", name[2]);
				}
				if (name.length > 0) {
					m.put("PropertyIdentificationSet.OwnerLastName", name[0]);
				}
				// m.put("PropertyIdentificationSet.SpouseFirstName", names[3]);
				// m.put("PropertyIdentificationSet.SpouseMiddleName",
				// names[4]);
				// m.put("PropertyIdentificationSet.SpouseLastName", names[5]);
			}
		}
	}

	private static Pattern p1MartinTR = Pattern
			.compile("^(?:(?:\\b(?:LO?TS?|PLAT (?:\\d+[A-Z]?|[A-Z])|PHASE|BL(?:OC)?KS?|UNIT|BLDG|PHASE|TRACT|SEC)\\b|\\([^\\)]+\\)|\"[^\"]+\")\\s*(?:-|&|OF\\b|\\s)*)+(.*)");
	private static Pattern p2MartinTR = Pattern
			.compile("(.*?)\\s*\\b(LO?TS?|BL(?:OC)?KS?|BEG|PH(?:ASES?)?|TR(?:ACT)?S?|PL(?:ATS?)? (?:\\d+[A-Z]?|[A-Z])|CONDO?(?:MINIUM)?|SEC|BLDG|UNIT|UNREC|S/D|BOATSLIP \\d+|PB|[A-Z]{2}[-\\s]\\d+)\\b.*");

	public static String extractSubdivFLMartinTR(String legal) {
		Matcher ma = p1MartinTR.matcher(legal);
		String subdiv = "";
		if (ma.find()) {
			subdiv = ma.group(1);
			legal = subdiv;
		}
		ma = p2MartinTR.matcher(legal);
		if (ma.find()) {
			subdiv = ma.group(1);
		}
		return subdiv;
	}

	public static void legalFLMartinTR(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(legal)) {
			return;
		}
		FLMartinTR.legalTokenizerFLMartinTR(m, legal);
	}

	public static String cleanOwnerNameFLMartinTR(String name) {
		if (name.contains("ATTN: "))
			return "";
		name = name.replaceAll("[\\(\\)]+", "");
		name = name.replaceAll("\\bDEC'D\\b", "");
		name = name.replaceFirst("'\\s*$", "");
		name = name.replaceAll("\\^", "");
		name = name.replaceFirst("-?\\b(JTRS|ESQ)\\s*$", "");
		Pattern p = Pattern.compile("(.+, .+) OR (.+)");
		Matcher ma = p.matcher(name);
		if (ma.matches()) {
			name = ma.group(1) + " & " + ma.group(2);
		}
		name = name.replaceAll("\\s{2,}", " ");
		return name;
	}

	public static void ownerNameIntermFLMartinTR(ResultMap m, long searchId) throws Exception {
		String owner = (String) m.get("tmpOwnerInfo");
		if (StringUtils.isEmpty(owner)) {
			return;
		}
		FLMartinTR.ownerNameTokenizerIntermFLMartinTR(m, owner);

	}

	public static void ownerNameFinalFLMartinTR(ResultMap m, long searchId) throws Exception {
		String owner = (String) m.get("tmpOwnerInfo");
		if (StringUtils.isEmpty(owner)) {
			return;
		}
		FLMartinTR.ownerNameTokenizerFinalFLMartinTR(m, owner);
	}

	public static void ownerNameTokenizerIntermFLBayTR(ResultMap m, String owner) throws Exception {
		owner = FLBayTR.cleanOwnerNameFLBayTR(owner);
		owner = owner.replaceFirst("\\s*\\b(?:AS )?(?:CO-)?(TRUSTEES?)\\s*$", "$1");
		owner = owner.replaceFirst("\\s*,\\s*$", "");
		String names[] = { "", "", "", "", "", "" };
		// apply LFM name tokenizer
		if (NameUtils.isCompany(owner)) {
			m.put("PropertyIdentificationSet.OwnerLastName", owner);
			names[2] = owner;
		} else {
			names = StringFormats.parseNameNashville(owner, true);
			m.put("PropertyIdentificationSet.OwnerFirstName", names[0]);
			m.put("PropertyIdentificationSet.OwnerMiddleName", names[1]);
			m.put("PropertyIdentificationSet.OwnerLastName", names[2]);
			m.put("PropertyIdentificationSet.SpouseFirstName", names[3]);
			m.put("PropertyIdentificationSet.SpouseMiddleName", names[4]);
			m.put("PropertyIdentificationSet.SpouseLastName", names[5]);
		}

		List<List> body = new ArrayList<List>();
		String[] suffixes = { "", "" }, types = { "", "" }, otherTypes = { "", "" };
		types = GenericFunctions.extractAllNamesType(names);
		otherTypes = GenericFunctions.extractAllNamesOtherType(names);
		suffixes = GenericFunctions.extractAllNamesSufixes(names);

		GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], types, otherTypes, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), body);

		GenericFunctions.storeOwnerInPartyNames(m, body, true);
	}

	public static void taxFLMartinTR(ResultMap m, long searchId) throws Exception {

		FLMartinTR.taxFLMartinTR(m, searchId);
	}

	public static void ownerNameIntermFLBayTR(ResultMap m, long searchId) throws Exception {
		String owner = (String) m.get("tmpOwnerInfo");
		if (StringUtils.isEmpty(owner)) {
			return;
		}
		ownerNameTokenizerIntermFLBayTR(m, owner);
	}

	static Pattern poBoxPat = Pattern.compile("\\b((P[\\s\\.]*O)?|\\d+) BOX\\b"); // RT
																					// \\d+
																					// BOX
	static Pattern numberPat = Pattern.compile("\\b\\d+");

	public static boolean isStreetAddress(String line) {
		return (numberPat.matcher(line).find() && AddressAbrev.containsAbbreviation(line))
		// checked for the presence of a number to avoid interpretation of
		// strings like DANNY ALLEMAN DR as street instead of name
				|| poBoxPat.matcher(line).find();
	}

	public static void ownerNameFinalFLBayTR(ResultMap m, long searchId) throws Exception {
		String owner = (String) m.get("tmpOwnerInfo");
		if (StringUtils.isEmpty(owner)) {
			return;
		}
		FLBayTR.ownerNameTokenizerFLBayTR(m, owner);
	}

	public static void taxFLBayTR(ResultMap m, long searchId) throws Exception {
		String delinq = (String) m.get("tmpDelinq");
		if (delinq == null) {
			delinq = "0.00";
		} else {
			delinq = sum(delinq, searchId);
		}
		m.put("TaxHistorySet.PriorDelinquent", delinq);
	}

	public static void taxFLGenericVisualGovTR(ResultMap m, long searchId) throws Exception {
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();

		if ("FL".equals(crtState)) {
			if ("Franklin".equals(crtCounty)) {
				FLFranklinTR.taxFLFranklinTR(m, searchId);
			}
		}
	}

	public static void partyNamesFLVisualGovTR(ResultMap m, long searchId) throws Exception {
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();

		if ("FL".equals(crtState)) {
			if ("Calhoun".equals(crtCounty)) {
				partyNamesFLCalhounTR(m, searchId);
			} else if ("Hardee".equals(crtCounty)) {
				partyNamesFLHardeeTR(m, searchId);
			} else if ("Jackson".equals(crtCounty)) {
				partyNamesFLJacksonTR(m, searchId);
			} else if ("Bay".equals(crtCounty)) {
				FLBayTR.partyNamesFLBayTR(m, searchId);
			} else if ("Okeechobee".equals(crtCounty)) {
				ownerNameFinalFLOkeechobeeTR(m, searchId);
			} else if ("DeSoto".equals(crtCounty)) {
				FLDeSotoTR.partyNamesFLDeSotoTR(m, searchId);
			} else if ("Gulf".equals(crtCounty)) {
				FLGulfTR.parseNames(m, true, searchId);
			} else if ("Wakulla".equals(crtCounty)) {
				FLWakullaTR.parseNames(m, true, searchId);
			}
		}
	}

	public static void parseAddressFLVisualGovTR(ResultMap m, long searchId) throws Exception {
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();

		if ("FL".equals(crtState)) {
			if ("Gulf".equals(crtCounty)) {
				FLGulfTR.parseAddressGulf(m, searchId);
			}
		}
	}

	public static void parseAddressFLEscambiaTR(ResultMap m, long searchId) throws Exception {
		String tmpAdr = (String) m.get("tmpAddress");
		if (tmpAdr.contains("BLK") || tmpAdr.contains("BLOCK")) {
			tmpAdr = tmpAdr.replaceFirst("(?is)\\s*\\bBL(?:OC)?K\\b", " ").trim();
		}
		if (StringUtils.isNotEmpty(tmpAdr)) {
			if (tmpAdr.matches("(?is)(.+) \\d+$")) {
				if (!tmpAdr.matches("(?is)(.+)\\bUN(?:IT)?\\s*(\\d+)$")) {
					tmpAdr = tmpAdr.replaceFirst("(?is)(.+) \\d+$", "$1").trim();
				}
			}
			m.put("PropertyIdentificationSet.StreetNo", StringFormats.StreetNo(tmpAdr.trim()));
			m.put("PropertyIdentificationSet.StreetName", StringFormats.StreetName(tmpAdr.trim()));
		}

		return;
	}

	public static void partyNamesFLHardeeTR(ResultMap m, long searchId) throws Exception {
		String owner = (String) m.get("tmpOwnerInfo");
		if (StringUtils.isEmpty(owner)) {
			return;
		}
		partyNamesTokenizerFLHardeeTR(m, owner);
	}

	public static void partyNamesTokenizerFLHardeeTR(ResultMap m, String s) throws Exception {

		// initial corrections and cleanup
		s = s.replaceAll("\\bPOB OX\\b", "PO BOX");
		s = s.replaceAll("\\([^\\)]*\\)?", "");
		s = s.replaceAll("\\b(?:CO-)?(TRUSTEE) OF(\\s|@@)REVOCABLE LIVING TRUST\\b", " $1");
		s = s.replaceAll("(?:,\\s?)?\\b(?:AS )?(?:CO-)?(T(?:RUS)?TEES?|TR)(?: OF)?\\b(?:\\s?(&|\\bAND\\b))?", " $1 ");
		s = s.replaceAll("\\bA/K/A\\b", "&");
		s = s.replaceAll("(&|\\bAND\\b)\\s?OTHERS( AS)?\\b", "");
		s = s.replaceAll("\\bATTN?:\\s?.+?@@", "@@");
		s = s.replaceAll("\\bHEIRS?( OF)?\\b", "");
		s = s.replaceAll("\\b(ET) (AL|UX|VIR)\\b", " $1$2");
		s = s.replaceAll("\\bOR\\b", "&");
		s = s.replaceAll(" / ", " & ");
		s = s.replaceAll("%", " C/O ");
		s = s.replaceAll("\\s{2,}", " ").trim();

		String entities[] = s.split("\\s*@@\\s*");
		int addrIdx = -1;
		for (int i = 1; i < entities.length; i++) {
			if (entities[i].matches("^(#?\\d+|(P\\.?\\s?O\\.?|POST OFFICE|RT \\d+) BOX|UNIT)\\b.+") || entities[i].matches(".*\\bFLOOR\\b.*")
					|| entities[i].matches(".+\\b[A-Z]{2} \\d+(-\\d+)?")) {
				addrIdx = i;
				break;
			}
		}
		if (addrIdx == -1)
			addrIdx = 1;
		s = s.replaceAll("\\s*\\bC[/\\\\]O\\s*@@", "@@C/O ");
		s = s.replaceAll("\\bC[/\\\\]O(\\s+C[/\\\\]O)+\\b", "C/O");
		s = s.replaceAll("(@@C/O )(&|AND\\b)\\s*", "$1");
		if (entities.length == 1) {
			s = s.replaceFirst("\\s*\\bC[/\\\\]O\\b", "");
		}

		entities = s.split("\\s*@@\\s*");
		for (int i = 0; i < addrIdx; i++) {
			entities[i] = entities[i].replaceFirst("\\s*\\bAS\\s*$", "");
		}
		// concatenate successive lines, if needed - first remove the name
		// suffixes from each line in order to be able to determine the name
		// format
		Matcher ma1, ma2, ma3;
		entities[addrIdx - 1] = entities[addrIdx - 1].replaceFirst(" (AND|&)\\s*$", "");
		String[] entities2 = new String[addrIdx];
		for (int i = 0; i < addrIdx; i++) {
			ma1 = nameSuffix.matcher(entities[i]);
			if (ma1.matches()) {
				entities2[i] = ma1.group(1).trim();
			} else {
				entities2[i] = entities[i];
			}
		}
		for (int i = 1; i < addrIdx; i++) {
			// when the name format is one of the following, don't concatenate
			boolean concatenate = entities2[i].matches("^OF .+");
			if (!concatenate) {
				if ((entities2[i - 1].matches(".+&.+") && (entities2[i - 1].matches(" (AND|&)\\s*$") && entities2[i].matches("[A-Z'-]{2,} [A-Z]{2,}") || NameUtils
						.isCompany(entities2[i])))
						|| entities2[i].matches("[A-Z'-]{2,} [A-Z'-]+ [A-Z'-]{2,}")
						|| entities2[i].matches("[A-Z'-]{2,} [A-Z]{2,} (&|AND) [A-Z]{2,}.*")
						|| entities2[i].matches("^\\s*C[/\\\\]O\\b.+")
						|| (entities2[i - 1].matches(".+ (AND|&) .+") && entities2[i].matches("[A-Z'-]{2,} [A-Z]{2,}( [A-Z]+) & [A-Z]+"))
						|| (entities2[i - 1].replaceFirst("^(.+?)\\s.*", "$1").equals(entities2[i].replaceFirst("^(.+?)\\s.*", "$1")))) {
					concatenate = false;
				} else {
					concatenate = true;
				}
			}
			if (concatenate) {
				entities[i - 1] = entities[i - 1] + " " + entities[i];
				entities[i] = "";
			}
		}
		String[] a = new String[6];
		String[] b = new String[6];
		String[] tokens;
		String[] addOwners;
		String[] suffixes = { "", "" }, type = { "", "" }, otherType = { "", "" };
		List<List> body = new ArrayList<List>();
		Pattern p = Pattern.compile("(.+?)\\s*&\\s*([^&]+)&?(.*)");
		String coownerPatt = "^\\s*C[/\\\\]O\\s*";
		Pattern p2 = Pattern.compile(coownerPatt + "(.+)");
		Pattern p3 = Pattern.compile(coownerPatt + "[A-Z]{2,}(?: [A-Z])? & [A-Z]{2,}(?: [A-Z])? ([A-Z'-]{2,})");
		String prevRowLastName = "";
		for (int i = 0; i < addrIdx; i++) {
			entities[i] = entities[i].replaceAll("(,|\\bAND\\b)", "&");
			entities[i] = entities[i].replaceAll("\\s*&\\s*$", "");
			entities[i] = entities[i].trim();
			if (entities[i].length() == 0)
				continue;

			String owner, spouse = "", others = "";
			ma1 = p.matcher(entities[i]);
			if (ma1.matches()) {
				owner = ma1.group(1);
				spouse = ma1.group(2);
				others = ma1.group(3);
			} else {
				owner = entities[i];
			}
			boolean ownerIsCompany = false;
			if (NameUtils.isCompany(owner)) {
				a[0] = "";
				a[1] = "";
				a[3] = "";
				a[4] = "";
				a[5] = "";
				if (NameUtils.isCompany(spouse)) {
					a[2] = entities[i].replaceFirst(coownerPatt, "");

					type = GenericFunctions.extractAllNamesType(a);
					otherType = GenericFunctions.extractAllNamesOtherType(a);

					addOwnerNames(a, "", "", type, otherType, true, false, body);
					continue;
				} else {
					a[2] = owner.replaceFirst(coownerPatt, "");

					type = GenericFunctions.extractAllNamesType(a);
					otherType = GenericFunctions.extractAllNamesOtherType(a);

					addOwnerNames(a, "", "", type, otherType, true, false, body);
					ownerIsCompany = true;
				}
			} else if (NameUtils.isCompany(entities[i])) {
				a[2] = entities[i].replaceFirst(coownerPatt, "");
				a[0] = "";
				a[1] = "";
				a[3] = "";
				a[4] = "";
				a[5] = "";

				type = GenericFunctions.extractAllNamesType(a);
				otherType = GenericFunctions.extractAllNamesOtherType(a);

				addOwnerNames(a, "", "", type, otherType, true, false, body);
				continue;
			}
			if (!ownerIsCompany) {
				boolean ownerIsFML = false;
				ma2 = p2.matcher(owner);
				if (ma2.matches()) {
					owner = ma2.group(1);
					ma3 = p3.matcher(entities[i]);
					if (ma3.matches()) {
						owner = owner + " " + ma3.group(1);
					}
					ownerIsFML = true;
				}
				if (i == 1 && owner.replaceFirst("^(.+?)\\s.*", "$1").equals(prevRowLastName)) {
					a = StringFormats.parseNameNashville(owner, true);
				} else if (ownerIsFML || (i > 0 && owner.matches("[A-Z]{2,} [A-Z] [A-Z'-]{2,}.*"))) {
					a = StringFormats.parseNameDesotoRO(owner, true);
					if (a[2].length() == 1 && a[1].length() > 1) {
						String temp = a[1];
						a[1] = a[2];
						a[2] = temp;
					}
				} else {
					a = StringFormats.parseNameNashville(owner, true);
				}
			}
			prevRowLastName = a[2];
			String spouseSuffix = "";
			if (spouse.length() != 0) {
				ma2 = nameSuffix.matcher(spouse);
				if (ma2.matches()) { // spouse has suffix => remove it
					spouse = ma2.group(1).trim();
					spouseSuffix = ma2.group(2);
				}
				spouse = spouse.replaceFirst("^([A-Z]+)-([A-Z]+)$", "$1 $2");
				if (!ownerIsCompany) {
					tokens = spouse.split(" ");
					if (tokens.length == 2) {
						if ((tokens[0].length() > 1)
								&& (tokens[0].contains(a[2] + "-") || tokens[0].contains("-" + a[2]) || tokens[0].equals(a[2])
										|| a[2].contains("-" + tokens[0]) || a[2].contains(tokens[0] + "-") || tokens[0].contains("-") || tokens[0]
											.startsWith("MC"))) {
							b = StringFormats.parseNameNashville(spouse, true);
						} else if ((tokens[1].length() > 1)
								&& (tokens[1].contains(a[2] + "-") || tokens[1].contains("-" + a[2]) || tokens[1].equals(a[2])
										|| a[2].contains(tokens[1] + "-") || a[2].contains("-" + tokens[1]) || tokens[1].startsWith("MC"))) {
							b = StringFormats.parseNameDesotoRO(spouse, true);
						} else {
							b = StringFormats.parseNameNashville(a[2] + " " + spouse, true);
						}
					} else {
						if (tokens.length == 1) {
							spouse = a[2] + " " + spouse;
						}
						b = StringFormats.parseNameNashville(spouse, true);
					}
					a[3] = b[0];
					a[4] = b[1];
					a[5] = b[2];
					suffixes = extractNameSuffixes(a);
					type = GenericFunctions.extractAllNamesType(a);
					otherType = GenericFunctions.extractAllNamesOtherType(a);
					if (spouseSuffix.length() != 0) {
						suffixes[1] = spouseSuffix;
					}
					addOwnerNames(a, suffixes[0], suffixes[1], type, otherType, false, NameUtils.isCompany(spouse), body);
				} else {
					a = StringFormats.parseNameNashville(spouse);
					type = GenericFunctions.extractAllNamesType(a);
					otherType = GenericFunctions.extractAllNamesOtherType(a);
					addOwnerNames(a, spouseSuffix, "", type, otherType, NameUtils.isCompany(spouse), false, body);
				}
			} else if (!ownerIsCompany) {
				suffixes = extractNameSuffixes(a);
				type = GenericFunctions.extractAllNamesType(a);
				otherType = GenericFunctions.extractAllNamesOtherType(a);
				addOwnerNames(a, suffixes[0], "", type, otherType, false, false, body);
			}

			String prevLast = a[5];
			if (others.length() != 0) {
				addOwners = others.trim().split("\\s?&\\s?");
				for (int j = 0; j < addOwners.length; j++) {
					if (!addOwners[j].matches("[A-Z'-]+ [A-Z]+ [A-Z'-]+") && !addOwners[j].contains(prevLast)) {
						addOwners[j] = prevLast + " " + addOwners[j];
					}
					if (addOwners[j].matches("[A-Z]{2,} [A-Z] [A-Z'-]{2,}.*")) {
						a = StringFormats.parseNameDesotoRO(addOwners[j], true);
					} else {
						a = StringFormats.parseNameNashville(addOwners[j], true);
					}
					prevLast = a[2];
					suffixes = extractNameSuffixes(a);
					type = GenericFunctions.extractAllNamesType(a);
					otherType = GenericFunctions.extractAllNamesOtherType(a);
					addOwnerNames(a, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(addOwners[j]), false, body);
				}
			}
		}
		storeOwnerInPartyNames(m, body, true);
	}

	public static void partyNamesFLJacksonTR(ResultMap m, long searchId) throws Exception {
		String owner = (String) m.get("tmpOwnerInfo");
		if (StringUtils.isEmpty(owner)) {
			return;
		}
		partyNamesTokenizerFLJacksonTR(m, owner);
	}

	public static void partyNamesTokenizerFLJacksonTR(ResultMap m, String s) throws Exception {

		// initial corrections and cleanup

		s = s.replaceAll("\\b(?:CO-)?(TRUSTEE?S?)\\b", "$1");
		s = s.replaceAll("\\b(TIEE|MRS|DR)@@\\b", "@@");
		s = s.replaceAll("\\b(\\d+ )?HEIRS?( OF)?\\b", "");
		s = s.replaceAll("\\b(ET) (AL|UX|VIR)\\b", "$1$2");
		s = s.replaceAll("(.+, [A-Z]+)@@((?:[A-Z]+ )?[A-Z'-]+, .+)", "$1 $2");
		s = s.replaceAll("(.+, [A-Z]+(?: [A-Z]+)?)@@([A-Z'-]+, .+)", "$1 $2");
		s = s.replaceAll("&@@([A-Z]+(?: [A-Z])?)@@", "& $1 @@");
		s = s.replaceAll("@@([A-Z'-]+@@)", " $1@@");
		s = s.replaceAll(",( [A-Z]+(?: [A-Z])?(?: " + nameSuffixString + ")?)(?= AND\\b|,|@@[^@]+&[^@]+)", "&$1");
		s = s.replaceAll("\\b(AND [A-Z]+)@@([A-Z]+ [A-Z'-]+@@)", " $1 $2");
		s = s.replaceAll("\\b(AND [A-Z]+(?: [A-Z])?)@@([A-Z'-]+ AND .+)", " $1 $2");
		s = s.replaceAll("\\b(OF)@@", "$1 ");
		s = s.replaceAll("@@(OF|TRUST)\\b", " $1");
		s = s.replaceAll("\\b(FAMILY)@@(.*\\bTRUST@@)", "$1 $2");
		s = s.replaceAll("\\bNO \\d+@@", "@@");
		s = s.replaceAll("\\s{2,}", " ").trim();

		String entities[] = s.split("\\s*@@\\s*");
		int addrIdx = -1;
		Pattern addrPatt = Pattern.compile("(C/O )?((#\\s*)?\\d+|P\\.?\\s?O\\.?|POST OFFICE|RT \\d+|UNIT|BOX)\\b.+");
		for (int i = 1; i < entities.length; i++) {
			if (addrPatt.matcher(entities[i]).matches() || entities[i].matches(".*\\bFLOOR\\b.*")) {
				addrIdx = i;
				break;
			}
		}
		if (addrIdx == -1) {
			Matcher ma = addrPatt.matcher(entities[entities.length - 1]);
			if (ma.find()) {
				entities[entities.length - 1] = entities[entities.length - 1].replace(ma.group(0), "").trim();
				addrIdx = entities.length;
				if (entities[entities.length - 1].matches("[A-Z'-]+") && entities.length > 1) {
					entities[entities.length - 2] = entities[entities.length - 2] + " " + entities[entities.length - 1];
					addrIdx = entities.length - 1;
				}
			} else {
				addrIdx = 1;
			}
		}

		String[] a = new String[6];
		String[] b = new String[6];
		String[] suffixes = { "", "" }, type = { "", "" }, otherType = { "", "" };
		List<List> body = new ArrayList<List>();
		Pattern p = Pattern.compile("(.+?)\\s*&\\s*(.+)");
		Matcher ma1, ma2;
		String prevLastName = "";
		String patt = "\\s?(?:\\bAND\\b|\\bC/O\\b|,|%)\\s?";
		for (int i = 0; i < addrIdx; i++) {
			entities[i] = entities[i].replaceAll("\\s*(&|\\bAND)\\s*$", "");
			entities[i] = entities[i].trim();
			if (entities[i].length() == 0)
				continue;

			String owners[] = entities[i].split(patt);
			for (int j = 0; j < owners.length; j++) {
				if (owners[j].length() == 0)
					continue;
				String owner, spouse = "";
				if (i != 0) {
					if (i == addrIdx - 1 && owners[j].matches("[A-Z]+( [A-Z])?( & [A-Z]+( [A-Z])?)?")) {
						owners[j] = owners[j] + " " + prevLastName;
					}
					owners[j] = owners[j].replaceAll("^([A-Z]+(?: [A-Z]+)?) & ([A-Z]+(?: [A-Z]+)?) ([A-Z'-]{2,})\\b", "$1 $3 & $2 $3");
					owners[j] = owners[j].replaceAll("^([A-Z]+(?: [A-Z]+)?) ([A-Z'-]{2,}) & ([A-Z]+(?: [A-Z])?)$", "$1 $2 & $3 $2");
				}
				ma1 = p.matcher(owners[j]);
				if (ma1.matches()) {
					owner = ma1.group(1);
					spouse = ma1.group(2);
				} else {
					owner = owners[j];
				}
				boolean ownerIsCompany = false;
				if (NameUtils.isCompany(owner)) {
					a[0] = "";
					a[1] = "";
					a[3] = "";
					a[4] = "";
					a[5] = "";
					if (NameUtils.isCompany(spouse)) {
						a[2] = owners[j];

						type = GenericFunctions.extractAllNamesType(a);
						otherType = GenericFunctions.extractAllNamesOtherType(a);

						addOwnerNames(a, "", "", type, otherType, true, false, body);
						ownerIsCompany = true;
						spouse = "";
					} else {
						a[2] = owner;

						type = GenericFunctions.extractAllNamesType(a);
						otherType = GenericFunctions.extractAllNamesOtherType(a);

						addOwnerNames(a, "", "", type, otherType, true, false, body);
						ownerIsCompany = true;
					}
				} else if (NameUtils.isCompany(owners[j])) {
					a[2] = owners[j];
					a[0] = "";
					a[1] = "";
					a[3] = "";
					a[4] = "";
					a[5] = "";

					type = GenericFunctions.extractAllNamesType(a);
					otherType = GenericFunctions.extractAllNamesOtherType(a);

					addOwnerNames(a, "", "", type, otherType, true, false, body);
					ownerIsCompany = true;
					spouse = "";
				} else if (j == 0 && NameUtils.isCompany(entities[i])) {
					a[2] = entities[i].replaceFirst("^" + patt, "");
					a[0] = "";
					a[1] = "";
					a[3] = "";
					a[4] = "";
					a[5] = "";

					type = GenericFunctions.extractAllNamesType(a);
					otherType = GenericFunctions.extractAllNamesOtherType(a);

					addOwnerNames(a, "", "", type, otherType, true, false, body);
					continue;
				}

				boolean isLF = false;
				if (!ownerIsCompany) {
					if (i == 0 && j == 0) {
						owner = owner.replaceFirst("\\s*\\d+\\s*$", "");
						a = StringFormats.parseNameNashville(owner, true);
						a[1] = a[1].replaceFirst("\\s*\\bREV$", "");
						isLF = true;
					} else {
						a = StringFormats.parseNameDesotoRO(owner, true);
					}
					suffixes = extractNameSuffixes(a);
					type = GenericFunctions.extractAllNamesType(a);
					otherType = GenericFunctions.extractAllNamesOtherType(a);

					addOwnerNames(a, suffixes[0], "", type, otherType, false, false, body);
				}
				prevLastName = a[2];
				String spouseSuffix = "";
				if (spouse.length() != 0) {
					ma2 = nameSuffix.matcher(spouse);
					if (ma2.matches()) { // spouse has suffix => remove it
						spouse = ma2.group(1).trim();
						spouseSuffix = ma2.group(2);
					}
					if (isLF) {
						if (spouse.matches("[A-Z]+( [A-Z]+)?") && !spouse.contains(prevLastName)) {
							spouse = spouse + " " + prevLastName;
						}
					}
					a = StringFormats.parseNameDesotoRO(spouse, true);

					type = GenericFunctions.extractAllNamesType(a);
					otherType = GenericFunctions.extractAllNamesOtherType(a);

					addOwnerNames(a, spouseSuffix, "", type, otherType, false, NameUtils.isCompany(spouse), body);
				}
			}
		}
		storeOwnerInPartyNames(m, body, true);
	}

	public static void partyNamesFLDuvalTR(ResultMap m, long searchId) throws Exception {
		FLDuvalTR.partyNamesFLDuvalTR(m, searchId);
	}

	public static void partyNamesFLCalhounTR(ResultMap m, long searchId) throws Exception {
		String owner = (String) m.get("tmpOwnerInfo");
		if (StringUtils.isEmpty(owner)) {
			return;
		}
		partyNamesTokenizerFLCalhounTR(m, owner);
	}

	public static String tokenizeOneOwnerFLCalhounTR(String entity, List<List> body) throws Exception {
		Matcher ma1 = Pattern.compile("(.+?)\\s*&\\s*(.+)").matcher(entity);
		Matcher ma2;
		String owner = "", spouse = "";
		String[] a = { "", "", "", "", "", "" };
		String[] b = { "", "", "", "", "", "" };
		String[] tokens;
		String[] suffixes, type, otherType;
		String enitityCleaned = StringFormats.unifyNameDelim(entity, true);
		String ownerCleaned = "";
		if (ma1.matches()) {
			owner = ma1.group(1);
			spouse = ma1.group(2);
		} else {
			owner = entity;
			ownerCleaned = enitityCleaned;
		}
		boolean ownerIsCompany = false, parseSpouse = true;
		if (NameUtils.isCompany(ownerCleaned)) {
			if (NameUtils.isCompany(spouse)) {
				a[2] = enitityCleaned;
				parseSpouse = false;
			} else {
				a[2] = ownerCleaned;
			}
			type = GenericFunctions.extractAllNamesType(a);
			otherType = GenericFunctions.extractAllNamesOtherType(a);

			addOwnerNames(a, "", "", type, otherType, true, false, body);
			ownerIsCompany = true;
		} else if (NameUtils.isCompany(enitityCleaned)) {
			a[2] = enitityCleaned;
			type = GenericFunctions.extractAllNamesType(a);
			otherType = GenericFunctions.extractAllNamesOtherType(a);

			addOwnerNames(a, "", "", type, otherType, true, false, body);
			parseSpouse = false;
			ownerIsCompany = true;
		}
		String spouseSuffix = "", ownerSuffix = "";
		if (parseSpouse && spouse.length() != 0) {
			boolean spouseIsFL = false;
			// if spouse is F MI? L then parse it separately
			if (spouse.matches("[A-Z]{2,} [A-Z]+ [A-Z'-]{2,}")) {
				spouseIsFL = true;
			}
			ma2 = nameSuffix.matcher(spouse);
			if (ma2.matches()) { // spouse has suffix => remove it
				spouse = ma2.group(1).trim();
				spouseSuffix = ma2.group(2);
				entity = ma1.group(1) + " & " + spouse;
			}
			tokens = spouse.split(" ");

			if (!ownerIsCompany) {
				a = StringFormats.parseNameNashville(entity, true);
				if (spouse.matches("[A-Z]{2,} [A-Z]{2,}")) {
					spouseIsFL = true;
					spouse = spouse + " " + a[2];
				}
				if (spouseIsFL) {
					b = StringFormats.parseNameDesotoRO(spouse, true);
					a[3] = b[0];
					a[4] = b[1];
					a[5] = b[2];
				}
			} else {
				a = StringFormats.parseNameNashville(spouse, true);
				type = GenericFunctions.extractAllNamesType(a);
				otherType = GenericFunctions.extractAllNamesOtherType(a);

				addOwnerNames(a, spouseSuffix, "", type, otherType, NameUtils.isCompany(a[2]), false, body);
			}
		} else if (!ownerIsCompany) {
			ma2 = nameSuffix.matcher(owner);
			if (ma2.matches()) {
				owner = ma2.group(1).trim();
				ownerSuffix = ma2.group(2);
			}
			a = StringFormats.parseNameNashville(owner, true);
		}
		if (!ownerIsCompany) {
			suffixes = extractNameSuffixes(a);
			type = GenericFunctions.extractAllNamesType(a);
			otherType = GenericFunctions.extractAllNamesOtherType(a);
			if (ownerSuffix.length() != 0) {
				suffixes[0] = ownerSuffix;
			}
			if (spouseSuffix.length() != 0) {
				suffixes[1] = spouseSuffix;
			}
			addOwnerNames(a, suffixes[0], suffixes[1], type, otherType, false, NameUtils.isCompany(a[5]), body);
		}
		return a[2];
	}

	public static void partyNamesTokenizerFLCalhounTR(ResultMap m, String s) throws Exception {

		// cleanup
		s = s.replaceAll("\\([^\\)]*\\)?", "");
		s = s.replaceAll("\\bDEC\\b", "");
		s = s.replaceAll("\\b(ET)\\s*(AL|UX|VIR)\\b", "$1$2");
		s = s.replaceAll("(?:,\\s?)?\\b(EST(ATE)?( OF)?)", "$1");
		s = s.replaceAll("\\b(TRUSTEES?):", "$1");
		s = s.replaceAll("\\bOR\\s*(?=@@|$)", "");
		s = s.replaceAll("(?:,\\s?)?\\b(TRUSTEES?)\\b:?( OF\\b)?", "$1");
		s = s.replaceAll("\\b[A-Z]*\\d+(-\\d+){2,}\\b", "");
		s = s.replaceAll("\\b[CA]/O\\b", "@");
		s = s.replaceAll("(?<=@@)%", "@");
		s = s.replaceAll("\\b\\d{1,2}/\\d{1,2}/\\d{2,4}\\b", "");
		s = s.replaceAll("['\\.](?=\\s|$)", "");
		s = s.replaceAll("ATTN: .*?(?=@@|$)", "");
		s = s.replaceAll("\\s*@@\\s*", "@@");
		s = s.replaceAll("\\s{2,}", " ").trim();

		String entities[] = s.split("@@");
		if (entities.length == 4) {
			if (entities[1].matches("[A-Z]+( FAMILY)?") && entities[2].matches(".*\\bTRUST\\b.*")) {
				entities[1] = entities[1] + " " + entities[2];
				entities[2] = "";
			} else if (entities[1].matches("[A-Z]+")
					|| (entities[1].matches("[A-Z]+ [A-Z]+") && NameUtils.isCompany(entities[1]) && entities[1].matches(".*\\b[A-Z]{2,3}\\b.*"))
					|| entities[0].endsWith("&") && entities[1].matches("[A-Z]{2,} [A-Z]")) { // PID
																								// 23-1N-10-0000-0016-0000
				entities[0] = entities[0] + " " + entities[1];
				entities[1] = "";
			}
		}
		for (int i = 0; i < entities.length; i++) {
			entities[i] = entities[i].replaceAll("^\\s*@\\s*", "");
		}

		List<List> body = new ArrayList<List>();
		// tokenize the first line, which can have the format L F M (& SF SMI?)?
		// or L F MI? & SF SMI SL
		String lastName = tokenizeOneOwnerFLCalhounTR(entities[0], body);

		// coowners may be on line2 or line3 only when the owner info array has
		// 4 lines
		if (entities.length == 4) {
			Pattern p = Pattern.compile("[A-Z]+(?: [A-Z]+)+ ([A-Z'-]{2,})");
			Pattern p2 = Pattern.compile("[A-Z]{2,} ([A-Z'-]{2,})");
			Matcher ma1, ma2;
			String[] a = new String[6];
			String[] suffixes, type, otherType;
			for (int i = 1; i < 3; i++) {
				// check if line is not actually an address
				if (!entities[i].matches("\\d+\\b.+") && !entities[i].matches(".*\\bP\\s?O BOX\\b.*") && !entities[i].matches(".*\\bRT \\d+ BOX\\b.*")
						&& !entities[i].matches(".*\\bLODGE (# )?\\d+\\b.*")) {
					if (entities[i].length() != 0) {
						if (entities[i].startsWith("&") || entities[i].matches("[A-Z'-]{2,} [A-Z]+ [A-Z]") // PIN
																											// 01-1S-09-0000-0039-0000
								|| ((i == 1) && entities[i].startsWith(lastName))) {
							tokenizeOneOwnerFLCalhounTR(entities[i], body);
							continue;
						}
						entities[i] = entities[i].replace(',', '&');
						String n[] = entities[i].split("\\s*&\\s*");
						boolean ownerIsCompany = false;
						if (n.length == 2) {
							if (NameUtils.isCompany(n[0])) {
								a[0] = "";
								a[1] = "";
								a[3] = "";
								a[4] = "";
								a[5] = "";
								if (NameUtils.isCompany(n[1])) {
									a[2] = entities[i];

									continue;
								} else {
									a[2] = n[0];
									ownerIsCompany = true;
								}
								type = GenericFunctions.extractAllNamesType(a);
								otherType = GenericFunctions.extractAllNamesOtherType(a);

								addOwnerNames(a, "", "", type, otherType, true, false, body);
							} else if (NameUtils.isCompany(entities[i])) {
								a[2] = entities[i];
								a[0] = "";
								a[1] = "";
								a[3] = "";
								a[4] = "";
								a[5] = "";
								type = GenericFunctions.extractAllNamesType(a);
								otherType = GenericFunctions.extractAllNamesOtherType(a);

								addOwnerNames(a, "", "", type, otherType, true, false, body);
								continue;
							}
						}
						suffixes = new String[n.length];
						String ln = "";
						for (int j = 0; j < n.length; j++) {
							if (j == 0 && ownerIsCompany)
								continue;
							ma2 = nameSuffix.matcher(n[j]);
							if (ma2.matches()) { // has suffix => remove it
								n[j] = ma2.group(1).trim();
								suffixes[j] = ma2.group(2);
							} else {
								suffixes[j] = "";
							}
							ma1 = p.matcher(n[j]);
							if (ma1.matches()) {
								if (ln.length() == 0) {
									ln = ma1.group(1);
									for (int k = 0; k < j; k++) {
										n[k] = n[k] + " " + ln;
									}
								}
							} else {
								ma1 = p2.matcher(n[j]);
								if (ma1.matches()) {
									if (j == n.length - 1) {
										if (ln.length() == 0) {
											ln = ma1.group(1);
											for (int k = 0; k < j; k++) {
												n[k] = n[k] + " " + ln;
											}
										}
									}
								} else if (ln.length() != 0) {
									n[j] = n[j] + " " + ln;
								}
							}
						}
						for (int j = 0; j < n.length; j++) {
							a = StringFormats.parseNameDesotoRO(n[j], true);
							type = GenericFunctions.extractAllNamesType(a);
							otherType = GenericFunctions.extractAllNamesOtherType(a);

							addOwnerNames(a, suffixes[j], "", type, otherType, NameUtils.isCompany(n[j]), false, body);
						}
					}
				} else {
					break;
				}
			}
		}
		storeOwnerInPartyNames(m, body, true);
	}

	public static void legalFLVisualGovTR(ResultMap m, long searchId) throws Exception {
		String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
		String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();

		if ("FL".equals(crtState)) {
			if ("Franklin".equals(crtCounty)) {
				legalFLFranklinTR(m);
			} else if ("Calhoun".equals(crtCounty)) {
				legalFLCalhounTR(m, searchId);
			} else if ("Hardee".equals(crtCounty)) {
				legalFLHardeeTR(m, searchId);
			} else if ("Jackson".equals(crtCounty)) {
				legalFLJacksonTR(m, searchId);
			} else if ("Okeechobee".equals(crtCounty)) {
				legalFLOkeechobeeTR(m, searchId);
			} else if ("Gulf".equals(crtCounty)) {
				FLGulfTR.parseLegalSummary(m, searchId);
			} else if ("Wakulla".equals(crtCounty)) {
				FLWakullaTR.parseLegalSummary(m, searchId);
			} else {
				legalFLBayTR(m, searchId);
			}
		}
	}

	public static void legalFLOkeechobeeTR(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;
		FLOkeechobeeTR.legalTokenizerFLOkeechobeeTR(m, legal);
	}

	public static void ownerNameFinalFLOkeechobeeTR(ResultMap m, long searchId) throws Exception {
		String owner = (String) m.get("tmpOwnerInfo");
		if (StringUtils.isEmpty(owner)) {
			return;
		}
		FLOkeechobeeTR.ownerNameTokenizerFLOkeechobeeTR(m, owner);
	}

	public static void legalFLBayTR(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		FLBayTR.legalTokenizerFLBayTR(m, legal);
	}

	public static void legalTokenizerFLFranklinTR(ResultMap m, String legal) throws Exception {

		// initial cleanup and correction of the legal description
		legal = legal.replaceAll("(?is)\\bBLOCK\\s+\\d+\\((\\d+)\\)", "BLOCK $1"); // PIN
																					// 20-07S-04W-1000-0155-0030:
																					// "BLOCK 1(55)"
		legal = legal.replaceAll("\\b((RE)?PLAT)T", "$1");
		legal = legal.replaceAll("\\bCONATINING\\b", "CONTAINING");
		legal = legal.replaceAll("\\b0RB?(?= \\d)", "ORB");
		legal = legal.replaceAll("(?<=[A-Z])\\.", " ");
		legal = legal.replaceAll("\\bNO\\s+(?=\\d)", " ");
		legal = legal.replaceAll("[\"\\(\\)#]", "");
		legal = legal.replaceAll("\\bNUMBER(?= \\d)", "");
		legal = legal.replaceAll("\\+", "&");
		legal = legal.replaceAll("\\sTHRU\\s", "-");
		legal = legal.replaceAll("(?<=\\d )AND(?= \\d)", "&");
		legal = legal.replaceAll("\\b(PROBATE (FILE|CASE)|BOUNDARY SURVEY)\\s+[\\d\\s-]*\\b(?!/)", "");
		legal = legal.replaceAll("\\bCASE [\\dA-Z-]+\\b", "");
		legal = legal.replaceAll("\\bTAX (CERT(IFICATE)?|DEED) [\\d\\s-]*", "");
		legal = legal.replaceAll("\\bCONTRACT FOR (DEED|SALE)\\b", "");
		legal = legal.replaceAll("\\bUS (LOTS?)\\b", "$1");
		legal = legal.replaceAll("\\bM/L\\b", "");
		legal = legal.replaceAll("\\bORIGINAL ACRES\\b", "");
		legal = legal.replaceAll("(\\bAPP(ROX)? )?(\\bA )?\\.?\\b[\\d\\./]+\\s*AC(RES?)?([/ ]ML)?( RR/RW)?(( RECD)? IN)?\\b", "");
		legal = legal.replaceAll("\\b(AN )?UN[\\s'-]?(RECR?('?D)?|RECORD(ED)?|RCR?D|REED|R)( (PL(AT)?|SUB))?\\b", "");
		legal = legal.replaceAll("\\b(THE )?([SWEN]{1,2} )?[\\d\\.]+(\\s*X\\s*[\\d\\.]+)+\\s?(F(EE)?T\\b|(DEG\\b|')( \\d+)?)?( OF )?", "");
		legal = legal
				.replaceAll(
						"(& |\\bAND )?\\b(THE )?((EAST|WEST|NORTH?|SOU(TH)?)(ERLY)? |[SWEN]{1,2} )?[\\d\\.\\s/]+\\s?(F(EE)?T|(DEG\\b|')( \\d+)?)(\\s?X\\s?[\\d\\.]+\\s?FT)*(\\s?X)?( IN( THE)? | OF )?",
						" ");
		legal = legal.replaceAll("(& |\\bAND )?\\b(THE )?(EAST|WEST|NORTH|SOU(TH)?|(?<!\\bORB? )[SWEN]{1,2})\\s?[\\d/]+( F(EE)?T)?( OF)?\\b", "");
		legal = legal.replaceAll("(\\b\\d+)?\\.\\d+\\b", "");
		legal = legal.replaceAll("\\bINC\\b", "");
		legal = legal.replaceAll("\\bA[ /]?K[ /]?A( NOW)?\\b", "AKA");
		legal = legal.replaceAll("\\b(ALSO )?(NOW )?KNOWN AS( NOW)?\\b", "AKA");
		legal = legal.replaceAll("(?<=\\bAKA )\\d+/\\d+\\b", "");
		legal = legal.replaceAll("\\bORB? ORB?\\b", "ORB");
		legal = legal.replaceAll("(?<!\\b(PB|PLAT|ORB?)\\s?)\\b\\d+/\\d+(?= LOTS?\\b)", "");
		legal = legal.replaceAll("\\s{2,}", " ").trim();

		List<String> line = new ArrayList<String>();
		// extract and replace cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		String pagePatt = "\\d+(\\s?[&\\s,/-]\\s?\\d+(?!/))*";
		Pattern p = Pattern.compile("\\bORB?[\\s/]?(\\d+|\\b[A-Z]{1,2})[\\s/-]+0*(" + pagePatt + ")\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			String page = ma.group(2);
			page = page.replaceFirst("^(\\d+)(\\d[ -])(\\d)$", "$1$2$1$3"); // modify
																			// 120-3
																			// into
																			// 120-123
			page = page.replaceFirst("^(\\d+)(\\d{2}[ -])(\\d{2})$", "$1$2$1$3"); // modify
																					// 120-31
																					// into
																					// 129-131
			page = page.replaceAll("\\s*[&,]\\s*", " ").replaceAll("/", " ");
			line.add(page);
			addIfNotAlreadyPresent(bodyCR, line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "ORB ");
		}
		p = Pattern.compile("(?<!\\b(?:PB|PLAT|UNDIVIDED) )\\b(\\d+|[A-Z]{2})(?<!OR)/(" + pagePatt + ")\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			String page = ma.group(2);
			page = page.replaceAll("\\s*[&,]\\s*", " ").replaceAll("\\s*-\\s*", "-");
			page = page.replaceFirst("^(\\d+)(\\d[ -])(\\d)$", "$1$2$1$3"); // modify
																			// 120-3
																			// into
																			// 120-123
			page = page.replaceFirst("^(\\d+)(\\d{2}[ -])(\\d{2})$", "$1$2$1$3");
			line.add(page);
			addIfNotAlreadyPresent(bodyCR, line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "ORB ");
		}
		p = Pattern.compile("\\bORB? 0*(\\d+) (?:PAGE|PG) 0*(\\d+(?:\\s*[&,-]\\s*\\d+)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2).replaceAll("\\s*[&,]\\s*", " "));
			addIfNotAlreadyPresent(bodyCR, line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "ORB ");
		}
		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			m.put("CrossRefSet", cr);
			legal = legal.replaceAll("\\s{2,}", " ");
		}

		// extract and replace plat b&p from legal description
		List<List> bodyPlat = new ArrayList<List>();
		p = Pattern.compile("\\bPB\\s?0*(\\d+)/0*(\\d+(?:-\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			bodyPlat.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PLAT ");
		}
		p = Pattern.compile("\\bPLAT(( (\\d+|[A-Z]{2})[-/]\\d+)+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			Matcher ma2 = Pattern.compile("\\b(\\d+|[A-Z]{2})[-/](\\d+)\\b").matcher(ma.group(1));
			while (ma2.find()) {
				line = new ArrayList<String>();
				line.add(ma2.group(1));
				line.add(ma2.group(2));
				bodyPlat.add(line);
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PLAT ");
		}
		legal = legal.replaceAll("\\bORB? \\d+(?= [A-Z])", "ORB ");
		if (bodyPlat.isEmpty()) {
			p = Pattern.compile("\\b(LOTS? \\d+(?:(?:[ &]+\\d+)*|(?:-\\d+){2,}))((?: (?:\\d+|[A-Z]{2})-\\d+)+)\\b");
			ma = p.matcher(legal);
			while (ma.find()) {
				Matcher ma2 = Pattern.compile("\\b(\\d+|[A-Z]{2})-(\\d+)\\b").matcher(ma.group(2));
				while (ma2.find()) {
					line = new ArrayList<String>();
					line.add(ma2.group(1));
					line.add(ma2.group(2));
					bodyPlat.add(line);
				}
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", ma.group(1) + " PLAT ");
			}
			p = Pattern.compile("\\b([A-Z]{2})-(\\d+)+\\b");
			ma = p.matcher(legal);
			while (ma.find()) {
				line = new ArrayList<String>();
				line.add(ma.group(1));
				line.add(ma.group(2));
				bodyPlat.add(line);
				legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "PLAT ");
			}
		}
		if (!bodyPlat.isEmpty()) {
			String[] header = { "PlatBook", "PlatNo" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("PlatBook", new String[] { "PlatBook", "" });
			map.put("PlatNo", new String[] { "PlatNo", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(bodyPlat);
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
			legal = legal.replaceAll("\\s{2,}", " ");
		}

		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = legal.replace("-", "__");
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
		legal = legal.replace("__", "-");

		// extract and replace lot from legal description
		String lot = ""; // can have multiple occurrences
		p = Pattern.compile("\\bLOTS?\\s?(\\d+[A-Z]?(?:-[A-Z])?(?:[-&,\\s]+\\d+[A-Z]?)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			String lotTemp = ma.group(1);
			lotTemp = lotTemp.replaceAll("[,&\\s]", " ");
			if (lotTemp.matches("\\d+(-\\d+){2,}")) {
				lotTemp = lotTemp.replace('-', ' ');
			}
			lotTemp = lotTemp.replaceAll("\\s*-\\s*", "-");
			lot = lot + " " + lotTemp;
			legal = legal.replace(ma.group(0), "LOT ");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace phase from legal description
		String phase = "";
		p = Pattern.compile("\\bPH(?:ASES?)? (\\d+(?:-?[A-Z])?( & \\d+)*|I)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = ma.group(1).replaceFirst("\\bI", "1");
			phase = phase.replaceAll("\\s?&\\s?", " ");
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legal = legal.replace(ma.group(0), "PHASE ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace sec-twn-rng from legal description
		List<List> bodySTR = new ArrayList<List>();
		p = Pattern.compile("\\bSEC(?:TION)?S? ((?:\\d+ & )?\\d+)[- ]+T?0*(\\d+[SWEN])[- ]+R?0*(\\d+[SWEN])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1).replaceAll("\\s?&\\s?", " ").replaceFirst("^0+(\\d)", "$1"));
			line.add(ma.group(2));
			line.add(ma.group(3));
			bodySTR.add(line);
			legal = legal.replace(ma.group(0), "SEC ");
		}
		p = Pattern.compile("\\b0*(\\d+)-0*(\\d+[SWEN])-0*(\\d+[SWEN])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add(ma.group(3));
			bodySTR.add(line);
			legal = legal.replace(ma.group(0), "SEC ");
		}
		p = Pattern.compile("\\bSEC (\\d+) TWN (\\d+[SWEN]) RA?NGE (\\d+[SWEN])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add(ma.group(3));
			bodySTR.add(line);
			legal = legal.replace(ma.group(0), "SEC ");
		}
		if (!bodySTR.isEmpty()) {
			String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
			map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
			map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(bodySTR);

			ResultTable pisPLAT = (ResultTable) m.get("PropertyIdentificationSet");
			if (pisPLAT != null) {
				pis = ResultTable.joinHorizontal(pis, pisPLAT);
				map.putAll(pisPLAT.map);
			}
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
			legal = legal.replaceAll("\\s{2,}", " ").trim();
		}
		p = Pattern.compile("\\bR(?:ANGE)?\\s?((?:\\d+|[A-Z]-\\d+)(?: \\d+)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionRange", ma.group(1));
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "RNG ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
		p = Pattern.compile("\\bSEC(?:TION)?\\s?(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionSection", ma.group(1));
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "SEC ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace block from legal description - extract from
		// original legal description
		String block = "";
		p = Pattern.compile("\\bBLOCKS ([A-Z](-[A-Z]){2,})\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1).replace('-', ' ');
			legal = legal.replace(ma.group(0), "BLK ");
		}
		p = Pattern
				.compile("\\bBL(?:(?:OC)?KS?)?[ -]([A-Z](?:[- ]\\d+)*(?:\\s*&\\s*[A-Z])*|\\d+(?:(?:/|\\s*&\\s*)?[A-Z]|\\s?[SWEN])?(?:[\\s-]\\d+)*)(?: R(?:(?:AN)?GE)?\\s?\\d+(?: \\d+)?)?\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1).replace('/', '-').replaceAll("\\s*&\\s*", " ").replaceAll("\\b(\\d+) ([SWEN])\\b", "$1$2");
			legal = legal.replace(ma.group(0), "BLK ");
		}
		block = block.trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace unit from legal description
		String unit = "";
		p = Pattern.compile("\\bUNIT (\\d+|[A-Z](?:-\\d+)?)\\b");
		ma = p.matcher(replaceNumbers(legal));
		while (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replaceFirst("\\bUNIT (\\d+|[A-Z](?:-\\d+)?)\\b", "UNIT ");
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace building # from legal description
		p = Pattern.compile("\\b(?:BUILDING|BLDG) (\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
			legal = legal.replace(ma.group(0), "BLDG ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace tract from legal description
		String tract = "";
		p = Pattern.compile("\\bTR(?:ACTS?)? (\\d+(?:\\s?[A-Z])?(?:[\\s&,-]+\\d+)*|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(1).replaceAll("\\s?[&,]\\s?", " ");
			legal = legal.replace(ma.group(0), "TRACT ");
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract subdivision name - only if lot or unit or section or tract
		// was extracted
		boolean isSubdivOrCondo = legal.matches(".*\\b(SUBD?(IVISION)?|CONDOS?)\\b.*");
		if (lot.length() != 0 || unit.length() != 0 || !bodySTR.isEmpty() || tract.length() != 0 || phase.length() != 0 || isSubdivOrCondo) {
			// first perform additional cleaning
			legal = legal.replaceFirst("^IN\\b", "");
			legal = legal.replaceFirst("\\bAKA\\s*$", "");
			legal = legal.replaceAll("\\bBEG AT .* (LOT|TRACT|UNIT)\\b", "$1");
			legal = legal.replaceAll("\\b(EAST|WEST|NORTH|SOUTH) OF HWY( \\d+)?\\b", "");
			legal = legal.replaceAll("\\bALSO\\s*\\-", "");
			legal = legal.replaceAll("\\b(OF )?FRACT(IONAL)?( [SWEN]{1,2})?\\b", "");
			legal = legal.replaceAll("\\b([SWEN]{1,2} )?COR( OF)?\\b", "");
			legal = legal.replaceAll("\\b(THE )?((EAST|WEST|NORTH?|SOUTH)(ERLY)? )+(ONE[ -])?(SIDE|END|HALF|PART|PORTION)( OF( ISLAND)?)?\\b", "");
			legal = legal.replaceAll("\\b(A )?(PORTION|PART|REPLAT)( OF)+( THE( OLD)?)?\\b", "");
			legal = legal.replaceAll("\\bUN-NUMBERED\\b", "");
			legal = legal.replaceAll("(& )?\\b(ALSO )?(A )?PARCE?L( \\d+| I)?( OF LAND)?( FROM)?\\b", "");
			legal = legal.replaceAll("\\b(ALSO )?(A |ONE )?TRACTS? OF LAND( IN)?\\b", "");
			legal = legal.replaceAll("\\bADJ TO\\b", "");
			legal = legal.replaceAll("\\b(EACH )?CONT(AINING)?\\b", "");
			legal = legal.replaceAll("\\bCONTAINS\\b", "");
			if (!legal.matches("^\\s*(LOT|BLK|TRACT)\\b.*"))
				legal = legal.replaceFirst("( AND |&)?.*?\\bAKA\\b", "");
			legal = legal.replaceFirst("\\b(LYING )?(NORTH|SOUTH|EAST|WEST|[NWSE]{1,2}) OF( \\w+ R(OA)?D| HWY)\\s*$", "");
			legal = legal.replaceFirst(".*\\b(LYING )?(NORTH|SOUTH|EAST|WEST|[NWSE]{1,2}) OF( \\w+ R(OA)?D| HWY)?\\b", "");
			legal = legal.replaceAll("\\bLYING (NORTH|SOUTH|EAST|WEST|[NWSE]{1,2})\\b", "");
			legal = legal.replaceFirst("\\bALONG\\b.*", "");
			legal = legal.replaceFirst("\\bOUT OF\\b.*", "");
			legal = legal.replaceAll("\\b(ALL|MOST) OF\\b", "");
			legal = legal.replaceAll("\\bALL (BL|LOT)\\b", "LOT");
			legal = legal.replaceAll("\\bCOUNTY ROAD\\b", "");
			legal = legal.replaceAll("\\bABANDD?ONED ST\\b", "");
			legal = legal.replaceAll("\\bORB ORDER OF .*\\bORB\\b", "ORB");
			legal = legal.replaceAll("\\bAND ADDITIONAL LAND\\b", "");
			legal = legal.replaceAll("\\bLYING BETWEEN\\b", "");
			legal = legal.replaceAll("\\bRE-?RECORDED\\b", "");
			legal = legal.replaceFirst("\\bUNDIVIDED [\\d/-]+\\b.*", "");
			legal = legal.replaceAll("\\bLIFE ESTATE\\b", "");
			legal = legal.replaceAll("\\b(?:\\d+(?:ST|ND|RD|TH) )?AD(?:DI?(?:(?:TIO)?N)?)?( TO)?\\b", "");
			legal = legal.replaceAll("\\b\\d+/\\d+\\b", "");
			legal = legal.replaceFirst("^\\s*AKA\\b", "");
			legal = legal.replaceAll("\\s{2,}", " ").trim();

			String subdiv = "";
			String prefix = "^(?:(?:\\s?(?:[&\\s]|\\b(?:IN(?: OLD)?|AND|ALSO|OF|LESS|REVISED)\\b)\\s?)?(?:LOTS?|SEC|RNG(?: \\d+)?|(?:LAND )?BEING|BLK?|TRACT|UNIT|ORB|PL(?:AT)?)(?: OF)?)+\\b";
			String suffix = "(?:\\bALL )?(?:\\b(?:SUBD?(?:IVISION)?|CONDOS?|UNIT|LOT|LESS|PHASE|ORB?|(?:(?:A )?RE-?)?PL(?:AT)?|AKA|BEING|BLK|BLDG)\\b|$)";
			if (lot.length() != 0 || unit.length() != 0 || phase.length() != 0) {
				p = Pattern.compile("(.*?)\\s?\\b(LOT|UNIT|BEING|BLK?|PLAT|PHASE|SEC|RNG|SUBD?(IVISION)?|CONDOS?|TRACT|BLDG)\\b.*");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				}
				if (subdiv.length() == 0) {
					p = Pattern.compile(prefix + "\\s*(.*?)\\s?" + suffix);
					ma.usePattern(p);
					ma.reset();
					if (ma.find()) {
						subdiv = ma.group(1);
					}
				}
			} else if (tract.length() != 0) {
				p = Pattern.compile("^TRACT(?: (?:BEING|ORB))*+ (.*?)\\s?(?:\\b(?:TRACT|UNIT|SUBD?(IVISION)?|CONDOS?|(?:AND )?AKA|ORB)\\b|$)");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			} else if (!bodySTR.isEmpty()) {
				p = Pattern.compile("^SEC(?: (?:BEING|LESS|ORB))++ (.*)");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1).replaceFirst(" SUBD?(IVISION)$", "");
				}
			} else if (isSubdivOrCondo) {
				p = Pattern.compile(prefix + "\\s*(.*?)\\s?" + suffix);
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}

			subdiv = subdiv.trim();
			if (subdiv.length() != 0) {
				subdiv = subdiv.replaceFirst("\\b[A-Z]\\d+[-/]\\d+\\s*$", "");
				subdiv = subdiv.replaceFirst("(?<!-)\\b(\\d+|[IVX]+)\\s*$", "");
				subdiv = subdiv.replaceFirst("^\\s*&\\s*", "");
				subdiv = subdiv.replaceFirst("^\\s*[\\d\\./]+\\b\\s*", "");
				subdiv = subdiv.replaceFirst("\\s*&\\s*$", "");
				subdiv = subdiv.replaceFirst("^\\s*IN(( OLD)? PL(AT)?)?\\b\\s*", "");
				subdiv = subdiv.replaceFirst("^\\s*IS\\s*$", "");
				subdiv = subdiv.replaceFirst("^\\s*ON THE\\b", "");
				subdiv = subdiv.replaceFirst("\\bPUBLIC PARK\\b", "");
				subdiv = subdiv.replaceFirst("\\bTHE [SWEN]{1,2}\\s*$", "");
				subdiv = subdiv.replaceFirst(".*\\bHWY\\b.*", "");
				subdiv = subdiv.replaceFirst("\\bEXCEPT .*", "");
				subdiv = subdiv.replaceFirst("\\b(SAID|ALSO) PROPERTY\\b", "");
				subdiv = subdiv.replaceFirst(".*SURVEY LINE.*", "");
				subdiv = subdiv.replaceFirst("\\bAND\\s*$", "");
				subdiv = subdiv.replaceFirst("^\\s*ALSO\\b", "");
				subdiv = subdiv.replaceFirst("\\bALSO\\s*$", "");
				subdiv = subdiv.replaceFirst("\\bAKA\\s*$", "");
				subdiv = subdiv.replaceAll("\\bTO POB\\b", "");
				subdiv = subdiv.replaceFirst("-\\s*$", "");
				subdiv = subdiv.replaceFirst("(?i)^PROBATE.*", "");
				subdiv = subdiv.trim();
				// remove last token if it is a number word
				int idx = subdiv.lastIndexOf(" ");
				if (idx + 2 <= subdiv.length()) {
					String lastToken = subdiv.substring(idx + 1);
					lastToken = replaceNumbers(lastToken);
					if (lastToken.matches("\\d+(ST|ND|RD|TH)?")) {
						subdiv = subdiv.substring(0, idx - 1);
					}
				}

				subdiv = subdiv.replaceAll("\\s{2,}", " ").trim();

				if (subdiv.length() != 0) {
					if (bodyPlat.isEmpty()) {
						ma = Pattern.compile("(.*?)(( \\d+-\\d+)+)$").matcher(subdiv);
						if (ma.matches()) {
							subdiv = ma.group(1);
							Matcher ma2 = Pattern.compile("(\\d+)-(\\d+)").matcher(ma.group(2));
							while (ma2.find()) {
								line = new ArrayList<String>();
								line.add(ma2.group(1));
								line.add(ma2.group(2));
								bodyPlat.add(line);
							}
						}
						if (!bodyPlat.isEmpty()) {
							String[] header = { "PlatBook", "PlatNo" };
							Map<String, String[]> map = new HashMap<String, String[]>();
							map.put("PlatBook", new String[] { "PlatBook", "" });
							map.put("PlatNo", new String[] { "PlatNo", "" });

							ResultTable pis = new ResultTable();
							pis.setHead(header);
							pis.setBody(bodyPlat);
							pis.setMap(map);
							m.put("PropertyIdentificationSet", pis);
						}
					}
				}
				if (subdiv.length() != 0) {
					m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
					if (legal.matches(".*\\bCONDO\\b.*"))
						m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
				}
			}
		}
	}

	public static void legalFLFranklinTR(ResultMap m) {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;
		try {
			legalTokenizerFLFranklinTR(m, legal);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void legalTokenizerFLCalhounTR(ResultMap m, String legal) throws Exception {

		// initial cleanup and correction of the legal description
		legal = legal.replaceAll("(?<=[&,-])(\\d{1,2}) (\\d{1,2})(?=,\\d+)", "$1$2");
		legal = legal.replaceAll("(?<=[&,])(\\d+) (\\d+)(?=-\\d+)", "$1$2");
		legal = legal.replaceAll("-\\s*-", "-");
		legal = legal.replaceAll("(\\d+)(BLK)\\b", "$1 $2");
		legal = legal.replaceAll("\\b(S/D)(OR)\\b", "$1 $2");
		legal = legal.replaceAll("\\bBKL(?= [A-Z]\\b|\\d)", "BLK");
		legal = legal.replaceFirst("\\s*,\\s*$", "");
		legal = legal.replaceAll("\\b(OR \\d+) (\\d{2}-\\d{2}\\d+)\\b", "$1$2");
		legal = legal.replaceAll("\\b(OR \\d+) (\\d-\\d\\d+)\\b", "$1$2");
		legal = legal.replaceAll("\\b(OR \\d+-\\d+) (\\d+)(?=$|\\s)", "$1$2");
		legal = legal.replaceAll("\\b(BN) (DY)\\b", "$1$2");
		legal = legal.replaceAll("\\b(A) (DN)\\b", "$1$2");
		legal = legal.replaceAll("\\b(DES) (C IN)\\b", "$1$2");
		legal = legal.replaceAll("\\b(PA) (RK)\\b", "$1$2");
		legal = legal.replaceAll("\\b(FA) (RMS)\\b", "$1$2");
		legal = legal.replaceAll("\\b(LES) (S)\\b", "$1$2");
		legal = legal.replaceAll("\\b(LE) (SS)\\b", "$1$2");
		legal = legal.replaceAll("\\b(MIRRO) (R)\\b", "$1$2");
		legal = legal.replaceAll("\\b(MIR) (ROR)\\b", "$1$2");
		legal = legal.replaceAll("\\b(HO) (MESITES)\\b", "$1$2");
		legal = legal.replaceAll("\\b(HAL) (LEY)\\b", "$1$2");
		legal = legal.replaceAll("\\(LESS[^\\)]+\\)?", ",");
		legal = legal.replaceAll("\\b([A-Z]+)- ([A-Z]+)\\b", "$1$2");
		legal = legal.replaceAll("\\b(\\d+-\\d{2,}) (\\d)\\b", "$1$2");
		legal = legal.replaceAll("(?<!OR )\\b(\\d+-\\d+, \\d+-\\d+)\\b", "OR $1");
		legal = legal.replaceAll("^\\*?[\\d\\.]+(AC?|M\\\\?A)\\*?", "");
		legal = legal.replaceAll("\\b\\d[\\d/\\.\\s]+AC(RES?)?\\b", "");
		legal = legal.replaceAll("\\b[SWEN]\\s?\\d+\\s?D\\d+\\s?(M|'\\d+\\s?\"\\s?[SWEN])\\b", "@@");
		legal = legal.replaceAll("\\b[SWEN]{1,2} \\d+/\\d+( OF)?( INT)?\\b", "");
		legal = legal.replaceAll("\\b[SWEN]{1,2} \\\\d?YD\\b", "");
		legal = legal.replaceAll("\\bGOV LOTS? IN\\b", "");
		legal = legal.replaceAll("\\b((BEG|COM) (AT )?)?[SWEN]{1,2}\\s?/C\\b", "");
		legal = legal.replaceAll("\\bCOR( [SWEN]{1,2})? OF\\b", "");
		legal = legal.replaceAll("\\b[SWEN]{2} OF\\b", "");
		legal = legal.replaceAll("\\bSTRIP [\\d\\.]+\\s?FT( WD)?( & [\\d\\.]+\\s?FT LNG)?\\b", "");
		legal = legal.replaceAll("\\b(LESS )?([SWEN]{1,2}\\s?)?[\\d\\.]+\\s?FT\\b", "");
		legal = legal.replaceAll("\\bALN?G [SWEN]{1,2} BNDY?\\b", "@@");
		legal = legal.replaceAll("\\b[SWEN]{1,2} (ON|TO)\\b( [SWEN])?( BNDY?| ST)?\\b", "@@");
		legal = legal.replaceAll("\\b[SWEN]{1,2}/\\d+ OF\\b", "");
		legal = legal.replaceAll("\\b(LO?TS? )?(AS )?DESC (IN|AS)\\b", "@@");
		legal = legal.replaceAll("\\b(ORIG )?INTERS\\b", "@@");
		legal = legal.replaceAll("\\bPT( OF)?\\b", "");
		legal = legal.replaceAll("\\bFRACT\\b", "");
		legal = legal.replaceAll("\\bPRT( IN)?\\b", "");
		legal = legal.replaceAll("\\bRE-?PL(AT)? OF\\b", "");
		legal = legal.replaceAll("\\bU\\s*/\\s*R\\b", "");
		legal = legal.replaceAll("\\bUNRECORDED PLAT\\b", "");
		legal = legal.replaceAll("\\bPRCL( #?\\s?\\d+-?[A-Z]?)?\\b", "");
		legal = legal.replaceAll("\\bPARTLY LOC( IN)?\\b", "");
		legal = legal.replaceAll("\\b\\d+/\\d+ (OF|INT)\\b", "");
		legal = legal.replaceAll("\\(\\w{2}\\),?", "");
		legal = legal.replaceAll("-\\s?,", ",");
		legal = legal.replaceAll("\\s{2,}", " ").trim();

		List<String> line = new ArrayList<String>();
		// extract and replace cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		Pattern p = Pattern.compile("\\b(OR|DB)\\s?(\\d+)\\s?/\\s?0*(\\d+)\\b");
		Matcher ma = p.matcher(legal);
		Matcher ma2;
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add(ma.group(3));
			addIfNotAlreadyPresent(bodyCR, line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " OR ");
		}
		p = Pattern.compile("\\b(OR|DB)\\s?,?(((\\d+)\\s*(?:[-&\\s]|PG)\\s*(\\d+(?:\\s*[&/-]\\s*\\d+\\b(?!\\s?[-/]))*)[,&\\s]*)+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			ma2 = Pattern.compile("\\b(\\d+)\\s*(?:[-&\\s]|PG)\\s*(\\d+(?:\\s*[&/-]\\s*\\d+\\b(?!\\s?[-/]))*)").matcher(ma.group(2));
			while (ma2.find()) {
				line = new ArrayList<String>();
				line.add(ma.group(1));
				line.add(ma2.group(1));
				String page = ma2.group(2);
				page = page.replaceAll("\\s*-\\s*", "-");
				page = page.replaceAll("\\b0+(\\d+)\\b", "$1");
				page = page.replaceFirst("^(\\d+)(\\d[&/-])(\\d)$", "$1$2$1$3"); // modify
																					// 120-3
																					// into
																					// 120-123
				page = page.replaceFirst("^(\\d+)(\\d{2}[&/-])(\\d{2})$", "$1$2$1$3");
				page = page.replaceAll("\\s*[&/]\\s*", " ");
				line.add(page);
				addIfNotAlreadyPresent(bodyCR, line);
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " OR ");
		}
		p = Pattern.compile("\\b(OR|DB)\\s?(\\d+) (\\d+(?:[-&]\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			String page = ma.group(3);
			page = page.replaceAll("\\b0+(\\d+)\\b", "$1");
			page = page.replaceFirst("^(\\d+)(\\d-)(\\d)$", "$1$2$1$3"); // modify
																			// 120-3
																			// into
																			// 120-123
			page = page.replaceFirst("^(\\d+)(\\d{2}-)(\\d{2})$", "$1$2$1$3"); // modify
																				// 120-31
																				// into
																				// 129-131
			page = page.replaceAll("&", " ");
			line.add(page);
			addIfNotAlreadyPresent(bodyCR, line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " OR ");
		}
		if (!bodyCR.isEmpty()) {
			String[] header = { "Book_Page_Type", "Book", "Page" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book_Page_Type", new String[] { "Book_Page_Type", "" });
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			m.put("CrossRefSet", cr);
			legal = legal.replaceAll("\\s{2,}", " ");
		}
		legal = legal.replaceFirst("\\s*\\bOR\\s?\\d[\\d,&\\s-]*$", "");

		// extract and replace plat b&p from legal description
		List<List> bodyPlat = new ArrayList<List>();
		p = Pattern.compile("\\bPB\\s?(\\d+)\\s*(?:-|PG?-?|\\s)\\s*(\\d+(?:\\s*[ /&]\\s*\\d+\\b(?!-))?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2).replaceAll("\\s*[/&]\\s*", " "));
			bodyPlat.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " PLAT ");
		}
		if (!bodyPlat.isEmpty()) {
			String[] header = { "PlatBook", "PlatNo" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("PlatBook", new String[] { "PlatBook", "" });
			map.put("PlatNo", new String[] { "PlatNo", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(bodyPlat);
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
			legal = legal.replaceAll("\\s{2,}", " ");
		}

		// extract and replace lot from legal description
		String lot = ""; // can have multiple occurrences
		p = Pattern.compile("\\bLO?TS? (?:[SWEN]{1,2} )?(\\d+(?:[-\\s]?[A-Z])?(?:\\s*[-&,]\\s*\\d+)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			String lotTemp = ma.group(1);
			lotTemp = lotTemp.replaceAll("\\s*[,&]\\s*", " ");
			lotTemp = lotTemp.replaceAll("(\\d+) ([A-Z])", "$1-$2");
			lot = lot + " " + lotTemp;
			legal = legal.replace(ma.group(0), " LT ");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace block from legal description - extract from
		// original legal description
		String block = "";
		p = Pattern.compile("\\bBLK\\s?(\\d+|\\b[A-Z](?:-\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1);
			legal = legal.replace(ma.group(0), " BLK ");
		}
		block = block.trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace section, township and range from legal
		// description
		List<List> bodySTR = new ArrayList<List>();
		p = Pattern.compile("\\bSECT? 0*(\\d+)-0*(\\d+[SWEN]?)-0*(\\d+[SWEN]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add(ma.group(3));
			bodySTR.add(line);
			legal = legal.replace(ma.group(0), "");
		}
		if (!bodySTR.isEmpty()) {
			String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
			map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
			map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(bodySTR);

			ResultTable pisPLAT = (ResultTable) m.get("PropertyIdentificationSet");
			if (pisPLAT != null) {
				pis = ResultTable.joinHorizontal(pis, pisPLAT);
				map.putAll(pisPLAT.map);
			}
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
			legal = legal.replaceAll("\\s{2,}", " ").trim();
		}
		String sec = "";
		p = Pattern.compile("\\bSECT? (\\d+(?:\\s?&\\s?\\d+)?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			sec = sec + " " + ma.group(1).replaceAll("\\s*&\\s*", " ");
			legal = legal.replace(ma.group(0), "");
		}
		sec = sec.trim();
		if (sec.length() != 0) {
			sec = LegalDescription.cleanValues(sec, true, true);
			m.put("PropertyIdentificationSet.SubdivisionSection", sec);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace unit from legal description
		String unit = "";
		p = Pattern.compile("\\bUN(?:IT)? (\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replace(ma.group(0), " UNIT ");
			;
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace building # from legal description
		p = Pattern.compile("\\b(?:BUILDING|BLDG) (\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
			legal = legal.replace(ma.group(0), " BLDG ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace tract from legal description
		String tract = "";
		p = Pattern.compile("\\bTRACTS? (\\d+[A-Z]?(?:\\s*[-\\s]\\s*\\d+)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(1).replaceAll("\\s*-\\s*", "-");
			legal = legal.replace(ma.group(0), " TRACT ");
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract subdivision name - only if lot or block or unit or tract was
		// extracted
		if (lot.length() != 0 || unit.length() != 0 || block.length() != 0 || tract.length() != 0) {
			// first perform additional cleaning
			legal = legal.replaceAll("\\bAS BEG AT\\b.*", "");
			legal = legal.replaceAll("(?<=\\b(LT|BLK|UNIT) )RESUB OF\\b", "");
			legal = legal.replaceAll("\\b((?:LT|BLK|UNIT) .+) UNIT\\b", "$1 @@");
			legal = legal.replaceAll("\\([^\\)]*\\)?", "");
			legal = legal.replaceAll("((\\b(?:LT|BLK|UNIT)|@@)[&\\s]*)+( OR)*\\s*$", "");
			legal = legal.replaceAll("\\s{2,}", " ").trim();

			String subdiv = "";
			p = Pattern
					.compile(".*\\b(?:(?:LT|BLK|UNIT|TRACT)\\s*[&\\s]\\s*)++(.*?)\\s*(?:\\b(?:IN|OF) )?(?:\\b(?:BLK|LO?TS?|UNIT|(?:\\d+(?:ST|ND|RD|TH) )?ADD?N|OR|S\\s*/\\s*[DE]|(?:RE\\s?)?SUBD?|PLAT|PB|UN\\s?REC|TAX)\\b|@@)");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile(".*(?:\\b(?:LT|BLK|UNIT|TRACT)[&\\s]*)++\\b(.+)$");
				ma.usePattern(p);
				ma.reset();
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}
			subdiv = subdiv.trim();
			if (subdiv.length() != 0) {
				subdiv = subdiv.replaceFirst("\\s*:.*", "");
				subdiv = subdiv.replaceFirst("^\\s*,\\s*", "");
				subdiv = subdiv.replaceFirst(",?\\s*\\b\\d[\\d\\s,&-]*$", "");
				subdiv = subdiv.replaceFirst("\\bREPLAT\\b", "");
				subdiv = subdiv.replaceAll("\\s{2,}", " ").trim();
				if (subdiv.length() != 0) {
					m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
				}
			}
		}
	}

	public static void legalFLCalhounTR(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerFLCalhounTR(m, legal);
	}

	public static void legalTokenizerFLHardeeTR(ResultMap m, String legal) throws Exception {

		// initial cleanup of the legal description
		legal = legal.replaceAll("\\bINC\\b", "");
		legal = legal.replaceAll("\"", "");
		legal = legal.replaceAll("\\b[SWEN]{1,2} \\d+/\\d+\\b", "");
		legal = legal.replaceAll("&?\\s*\\bLESS [SWEN]{1,2} \\d+([\\./]\\d+)?\\s?FT\\b", "");
		legal = legal.replaceAll("\\bAJOINING SAID (BLK|LOT)\\b", "");
		legal = legal.replaceAll("&?\\s*\\bLESS RD?/R?W( [SWEN]{1,2} SIDE)?\\b", "");
		legal = legal.replaceAll("(?<=LOT )\\b([SWEN]|NORTH|SOUTH|WEST|EAST) (\\d+)", "$1");
		legal = legal.replaceAll("\\b(\\d+)\\s?(?:TO|T0|THRU)\\s?(\\d+)\\b", "$1-$2");
		legal = legal.replaceAll("\\b(\\d+)\\s?-\\s?(\\d+)\\b", "$1-$2");
		legal = legal.replaceAll("\\b(\\d+)\\s?AND\\s?(\\d+)\\b", "$1&$2");
		legal = legal.replaceAll("\\b\\d+\\.\\d+\\b", "");
		legal = replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
		legal = legal.replaceAll("\\s{2,}", " ").trim();

		List<String> line = new ArrayList<String>();

		// extract and replace plat b&p from legal description
		List<List> bodyPlat = new ArrayList<List>();
		Pattern p = Pattern.compile("\\bPBN?(?:[-\\s]B-?)?(\\d+)\\s?P(\\d+(?:&\\d+)*)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2).replace('&', ' '));
			bodyPlat.add(line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " PLAT ");
		}
		if (!bodyPlat.isEmpty()) {
			String[] header = { "PlatBook", "PlatNo" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("PlatBook", new String[] { "PlatBook", "" });
			map.put("PlatNo", new String[] { "PlatNo", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(bodyPlat);
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
			legal = legal.replaceAll("\\s{2,}", " ");
		}

		// extract and replace cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("\\b(?:[A-Z/]{2,4})?(\\d+)((?:PP?\\d+(?:[-&]\\d+)?)+)([A-Z/]{2,4})?\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2).replaceAll("(PP?|&)", " ").trim());
			addIfNotAlreadyPresent(bodyCR, line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " OR ");
		}
		p = Pattern.compile("\\bOR BOOK (\\d+) PAGE (\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			addIfNotAlreadyPresent(bodyCR, line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " OR ");
		}
		p = Pattern.compile("\\b(?:[A-Z/]{2,4})\\s?(\\d+) ((?:PP?\\d+(?:[-&]\\d+)?)+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2).replaceAll("(PP?|&)", " ").trim());
			addIfNotAlreadyPresent(bodyCR, line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " OR ");
		}
		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			m.put("CrossRefSet", cr);
			legal = legal.replaceAll("\\s{2,}", " ");
		}

		// extract and replace lot from legal description
		String lot = ""; // can have multiple occurrences
		p = Pattern.compile("\\bLOTS? (\\d+[A-Z]?(?:\\s?[-&,\\s]+\\s?\\d+[A-Z]?)*|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(1).replaceAll("\\s*[,&]\\s*", " ");
			legal = legal.replace(ma.group(0), " LOT ");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace block from legal description - extract from
		// original legal description
		String block = "";
		p = Pattern.compile("\\bBL(?:OC)?KS?\\s?(\\d+(?:\\s*&\\s*\\d+)*|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1).replaceAll("\\s*&\\s*", " ");
			legal = legal.replace(ma.group(0), " BLK ");
		}
		block = block.trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace unit from legal description
		String unit = "";
		p = Pattern.compile("\\bUNIT (\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replace(ma.group(0), " UNIT ");
			;
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace section, township and range from legal
		// description
		List<List> bodySTR = new ArrayList<List>();
		p = Pattern.compile("\\bS?(\\d+)[-\\s]T?(\\d+[SWEN])[-\\s]R?(\\d+[SWEN])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+", ""));
			line.add(ma.group(2).replaceFirst("^0+", ""));
			line.add(ma.group(3).replaceFirst("^0+", ""));
			bodySTR.add(line);
			legal = legal.replace(ma.group(0), " SEC ");
		}
		p = Pattern.compile("\\bSEC (\\d+)[-\\s](\\d+)[-\\s](\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+", ""));
			line.add(ma.group(2).replaceFirst("^0+", ""));
			line.add(ma.group(3).replaceFirst("^0+", ""));
			bodySTR.add(line);
			legal = legal.replace(ma.group(0), " SEC ");
		}
		p = Pattern.compile("\\bSEC\\s?(\\d+) TWN\\s?(\\d+[SWEN]?)(?: (?:RGE|RNG)\\s?(\\d+[SWEN]?))?\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+", ""));
			line.add(ma.group(2).replaceFirst("^0+", ""));
			String rng = ma.group(3);
			if (rng != null) {
				rng = rng.replaceFirst("^0+", "");
			} else {
				rng = "";
			}
			line.add(rng);
			bodySTR.add(line);
			legal = legal.replace(ma.group(0), " SEC ");
		}
		if (!bodySTR.isEmpty()) {
			String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
			map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
			map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(bodySTR);
			ResultTable pisPLAT = (ResultTable) m.get("PropertyIdentificationSet");
			if (pisPLAT != null) {
				pis = ResultTable.joinHorizontal(pis, pisPLAT);
				map.putAll(pisPLAT.map);
			}
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
			legal = legal.replaceAll("\\s{2,}", " ").trim();
		}
		String sec = "";
		p = Pattern.compile("\\bSEC(?:TION)? (\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			sec = sec + " " + ma.group(1).replaceFirst("^0+", "");
			legal = legal.replace(ma.group(0), " SEC ");
		}
		sec = sec.trim();
		if (sec.length() != 0) {
			sec = LegalDescription.cleanValues(sec, true, true);
			m.put("PropertyIdentificationSet.SubdivisionSection", sec);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace building # from legal description
		p = Pattern.compile("\\b(?:BUILDING|BLDG) (\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
			legal = legal.replace(ma.group(0), " BLDG ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace tract from legal description
		String tract = "";
		p = Pattern.compile("\\bTRACTS? (\\d+(?:[\\s-]?[A-Z]\\b)?(?:\\s?[&\\s]\\s?\\d+(?:[\\s-]?[A-Z]\\b)?)*)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = ma.group(1).replaceAll("\\s?&\\s?", " ").replaceAll("(\\d+) ([A-Z]\\b)", "$1$2");
			m.put("PropertyIdentificationSet.SubdivisionTract", LegalDescription.cleanValues(tract, false, true));
			legal = legal.replace(ma.group(0), " TRACT ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace phase from legal description
		String phase = "";
		p = Pattern.compile("\\bPHASE (\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legal = legal.replace(ma.group(0), " PHASE ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract subdivision name - only if lot or block or tract or unit was
		// extracted
		if (lot.length() != 0 || block.length() != 0 || tract.length() != 0 || unit.length() != 0) {
			// first perform additional cleaning
			legal = legal.replaceAll("\\([^\\)]*\\)*", "");
			legal = legal.replaceAll("\\s{2,}", " ").trim();

			String subdiv = "";
			p = Pattern
					.compile(".*\\b(?:LOT|BLK|TRACT)(?: SEC| REPLAT OF)? (.*?)\\s*\\b(?:SEC|(?:\\d+(?:ST|ND|RD|TH) )?ADD(?:ITIO)?N?|(?:RE[\\s-]?)?SUBD?(?:IVISION)?S?|S/D|OR|DB|PHASE|RR SURVEY|UNIT|BLDG|(?:RE)?PLAT|CONDOMINIUM)\\b");
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			} else {
				p = Pattern.compile(".*\\b(?:LOT|BLK|TRACT)(?: SEC| REPLAT OF)? (.+?\\s*\\b(?:ESTATE|FARM|HOMESITE|OR|ACRE|HEIGHT|MANOR|PLACE|END)S?)\\b.*");
				ma.usePattern(p);
				ma.reset();
				if (ma.find()) {
					subdiv = ma.group(1);
				} else {
					p = Pattern.compile(".*\\b(?:LOT|BLK|TRACT)(?: SEC| REPLAT OF)? (TOWN OF .+)");
					ma.usePattern(p);
					ma.reset();
					if (ma.find()) {
						subdiv = ma.group(1);
					} else {
						p = Pattern.compile(".*\\bUNIT (.+) CONDOMINIUM\\b");
						ma.usePattern(p);
						ma.reset();
						if (ma.find()) {
							subdiv = ma.group(1);
						}
					}
				}
			}
			subdiv = subdiv.trim();
			if (subdiv.length() != 0) {
				subdiv = subdiv.replaceAll(".*\\bLYING ON [SWEN]{1,2} SIDE\\b", "");
				subdiv = subdiv.replaceFirst("\\bRESURVEY OF\\b", "");
				subdiv = subdiv.replaceFirst("^\\s*(THERE)?OF\\b\\s*", "");
				subdiv = subdiv.replaceFirst("\\bLINE TO R/W\\b.*", "");
				subdiv = subdiv.replaceFirst("\\bRUN [SWEN]{1,2}(/LY)? \\d+.+", "");
				subdiv = subdiv.replaceFirst("\\bRUN ALONG .*", "");
				subdiv = subdiv.replaceFirst("&?\\s*\\bCOMM? (AT )?.*", "");
				subdiv = subdiv.replaceFirst("&?\\s*\\bBEG AT\\b.*", "");
				subdiv = subdiv.replaceFirst("&?\\s*\\b[SWEN] \\d+(\\.\\d+)? FT\\b.*", "");
				subdiv = subdiv.replaceFirst("\\b(BEING )?LOCATED IN\\b", "");
				subdiv = subdiv.replaceFirst("\\bSUBJECT TO\\b.*", "");
				subdiv = subdiv.replaceFirst("^\\s*LESS RD\\s?/\\s?RW\\b", "");
				subdiv = subdiv.replaceFirst("\\bLESS R/?[A-Z]\\b.*", "");
				subdiv = subdiv.replaceFirst("\\bPBN?(-B)?\\d+.*", "");
				subdiv = subdiv.replaceFirst("\\b[A-Z]{2,3}(&[A-Z]{2,3})*-\\s?$", "");
				subdiv = subdiv.replaceFirst("\\s*\\bIN\\s*$", "");
				subdiv = subdiv.replaceFirst("\\s*&\\s*$", "");
				subdiv = subdiv.replaceFirst("\\sPR-?B-?\\d+ P-?\\d+\\s*$", "");
				subdiv = subdiv.replaceFirst("\\s*\\b\\d+\\s*$", "");
				subdiv = subdiv.replaceAll("\\s{2,}", " ").trim();
				if (subdiv.length() != 0) {
					m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
					if (legal.contains("CONDOMINIUM")) {
						m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
					}
				}
			}
		}
	}

	public static void legalFLHardeeTR(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerFLHardeeTR(m, legal);
	}

	public static void legalTokenizerFLJacksonTR(ResultMap m, String legal) throws Exception {

		// initial cleanup of the legal description
		legal = legal.replaceAll("\"", "");
		legal = legal.replaceAll("\\b(BEING )?[\\d\\./]+\\s?AC(RES)?\\b", "");
		legal = legal.replaceAll("\\b[\\d/\\.]+ BY ?[\\d/\\.\\s]+ FT( [SWEN&]+)?( OF)?\\b", "");
		legal = legal.replaceAll("\\b(BEING )?([SWEN]{1,2} )?[\\d/\\.\\s]+ FT( [SWEN&]+)?( BY ?[\\d/\\.\\s]+ FT)?( [SWEN&]+)?( OF)?\\b", "");
		legal = legal.replaceAll("\\b\\d+/\\d+ INTEREST( IN)?\\b", "");
		legal = legal.replaceAll("\\bNO\\.?(?= \\d+)", "");
		legal = legal.replaceAll("\\b(BEING )?(A )?PART OF\\b", "");
		legal = legal.replaceAll("\\bALL OF\\b", "");
		legal = legal.replaceAll("\\b(AS )?PER\\b", "");
		legal = legal.replaceAll("\\bA (STRIP|TRACT) OF LAND\\b", "");
		legal = legal.replaceAll("\\b[SWEN]{1,2}\\d+/\\d+( OF)?\\b", "");
		legal = legal.replaceAll("\\b[SWEN]{1,2}C? OF\\b", "");
		legal = legal.replaceAll("\\b(LOTS \\d+ BLK \\w+ &) (\\d+ BLK \\w+)\\b", "$1 LOT $2");
		legal = legal.replaceAll("\\bCASE [\\d#-]+\\b", "");
		legal = legal.replaceAll("\\s*\\bTHRU\\b\\s*", "-");
		legal = legal.replaceAll("\\bPT\\b", "");
		legal = legal.replace("+", ",");

		legal = replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens); // convert
																					// roman
																					// numbers
		legal = legal.replaceAll("\\s{2,}", " ").trim();

		List<String> line = new ArrayList<String>();

		// extract and replace cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		Pattern p = Pattern.compile("\\b(?:OR|DB|BOOK|BK?)\\.?\\s?(\\d+|[A-Z]{2}),?\\s?P(?:G|AGE)?S?\\.?\\s?(\\d+(\\s*[&,-]\\s*\\d+)*)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d)", "$1"));
			line.add(ma.group(2).replaceAll("\\s*[&,]\\s*", " ").replaceAll("\\s*-\\s*", "-").replaceAll("\\b0+(\\d)", "$1").trim());
			addIfNotAlreadyPresent(bodyCR, line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " OR ");
		}
		p = Pattern.compile("\\b(?:OR|DB|BOOK)\\.? (\\d+|[A-Z]{2}) (\\d+(\\s*[&,-]\\s*\\d+)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1).replaceFirst("^0+(\\d)", "$1"));
			line.add(ma.group(2).replaceAll("\\s*[&,]\\s*", " ").replaceAll("\\s*-\\s*", "-").replaceAll("\\b0+(\\d)", "$1").trim());
			addIfNotAlreadyPresent(bodyCR, line);
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", " OR ");
		}
		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			m.put("CrossRefSet", cr);
			legal = legal.replaceAll("\\s{2,}", " ");
		}

		// extract and replace lot from legal description
		String lot = ""; // can have multiple occurrences
		p = Pattern.compile("\\bLO?TS? (\\d+(?:-?[A-Z])?(?:\\s*[,&\\s-]\\s*\\d+(?:-?[A-Z])?)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(1).replaceAll("\\s*[,&]\\s*", " ");
			legal = legal.replace(ma.group(0), " LOT ");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace block from legal description - extract from
		// original legal description
		String block = "";
		p = Pattern.compile("\\bBL(?:OC)?KS? (?!IN\\b)([A-Z]{1,2}|\\d+(?:\\s*[&,]\\s*\\d+)*)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = block + " " + ma.group(1).replaceAll("\\s*[&,]\\s*", " ");
			legal = legal.replace(ma.group(0), " BLK ");
		}
		block = block.trim();
		if (block.length() != 0) {
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace unit from legal description
		String unit = "";
		p = Pattern.compile("\\bUNIT (\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			unit = unit + " " + ma.group(1);
			legal = legal.replace(ma.group(0), " UNIT ");
			;
		}
		unit = unit.trim();
		if (unit.length() != 0) {
			unit = LegalDescription.cleanValues(unit, false, true);
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace section, township and range from legal
		// description
		List<List> bodySTR = new ArrayList<List>();
		p = Pattern.compile("\\b(\\d+(?:\\s*[&,]\\s*\\d+)*)[-\\s](\\d+[SWEN]?)-(\\d+[SWEN]?)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1).replaceAll("\\b0(\\d+)", "$1").replaceAll("\\s*[&,]\\s*", " "));
			line.add(ma.group(2).replaceFirst("^0+", ""));
			line.add(ma.group(3).replaceFirst("^0+", ""));
			bodySTR.add(line);
			legal = legal.replace(ma.group(0), " SEC ");
		}
		if (!bodySTR.isEmpty()) {
			String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };

			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
			map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
			map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

			ResultTable pis = new ResultTable();
			pis.setHead(header);
			pis.setBody(bodySTR);
			pis.setMap(map);
			m.put("PropertyIdentificationSet", pis);
			legal = legal.replaceAll("\\s{2,}", " ").trim();
		}
		String sec = "";
		p = Pattern.compile("\\bSECT?(?:ION)?S? (\\d+(?:\\s*[,&]\\s*\\d+)*)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			sec = ma.group(1).replaceAll("\\s*[,&]\\s*", " ");
			m.put("PropertyIdentificationSet.SubdivisionSection", sec);
			legal = legal.replace(ma.group(0), " SEC ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace building # from legal description
		p = Pattern.compile("\\b(?:BUILDING|BLDG) (\\d+(?:-?[A-Z])?|[A-Z](?:-?\\d+)?)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(1));
			legal = legal.replace(ma.group(0), " BLDG ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace tract from legal description
		String tract = "";
		p = Pattern.compile("\\bTRACT ([A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = ma.group(1);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
			legal = legal.replace(ma.group(0), " TRACT ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract and replace phase from legal description
		String phase = "";
		p = Pattern.compile("\\bPHASE (\\d+|I)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = ma.group(1).replace('I', '1');
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase);
			legal = legal.replace(ma.group(0), " PHASE ");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract subdivision name - only if lot or block or unit was extracted
		if (lot.length() != 0 || block.length() != 0 || unit.length() != 0 || legal.matches(".*\\b(SUB-?DI?V?|ADD'?N?)\\b.*")) {
			// first perform additional cleaning
			legal = legal.replaceAll("^(\\s*(OR|DB)\\b)+", "");
			legal = legal.replaceAll("(?<=(BLK|LOT|UNIT))\\s*,", " ");
			legal = legal.replaceAll("\\bLESS\\b", "");
			legal = legal.replaceFirst("\\s*\\b\\d+\\s*$", "");
			legal = legal.replaceFirst("\\bOR\\b(\\sOR\\b)+", "OR");
			legal = legal.replaceAll("(?<=[A-Z\\s])\\. ", " ");
			legal = legal.replaceAll("\\s{2,}", " ").trim();

			String subdiv = "";
			String patt = "(?:\\b(?:LOTS?|BLK|THE(?: UNRECORDED)? SUBDI?V OF|UNRECORDED SUBDI?V)(?: PHASE| TRACT)*\\b\\s*(?:&\\s*|IN )?)++(.*?)\\s*(?:\\b(?:SUB(?:-?DI?V?)?|(?:\\d+(?:ST|ND|RD|TH) )?ADD'?(?:ITIO)?N?|UNIT|OR|DB|RUN(?:NING)?|SEC|UNRECORDED|PHASE|BEING)\\b|\\.{2,}|,|&(?= LOT\\b)).*";
			p = Pattern.compile(".*" + patt);
			ma = p.matcher(legal);
			if (ma.find()) {
				subdiv = ma.group(1);
			}
			if (subdiv.length() == 0) {
				p = Pattern.compile(".*(?:\\b(?:LOTS?|BLK|OR)\\s*(?:&\\s*|IN )?)++\\b(\\w+)$");
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				}
				if (subdiv.length() == 0) {
					p = Pattern
							.compile(".*\\bUNIT (.*?)\\s*(?:\\b(?:SUB(?:-?DI?V?)?|(?:\\d+(?:ST|ND|RD|TH) )?ADD'?(?:ITIO)?N?|OR|DB|RUN(?:NING)?|SEC|UNRECORDED|PHASE)\\b|\\.{2,}|,|&(?= LOT\\b)).*");
					ma = p.matcher(legal);
					if (ma.find()) {
						subdiv = ma.group(1);
					}
					if (subdiv.length() == 0) {
						p = Pattern.compile(".*?\\b(?:(?:LOTS?|BLK|UNIT) )++(.*? (?:ESTATES?|LAKE|PARK|MANOR))\\b.*");
						ma.usePattern(p);
						ma.reset();
						if (ma.find()) {
							subdiv = ma.group(1);
						}
						if (subdiv.length() == 0) {
							p = Pattern.compile("(?:.*\\b(?:OR|DB) )?(.+?)\\s*\\b(?:LOTS?|SUB(?:-?DI?V?)?|(?:\\d+(?:ST|ND|RD|TH) )?ADD'?(?:ITIO)?N?)\\b");
							ma.usePattern(p);
							ma.reset();
							if (ma.find()) {
								subdiv = ma.group(1);
							}
						}
					}
				}
			}
			if (subdiv.length() == 0) {
				p = Pattern.compile(patt);
				ma = p.matcher(legal);
				if (ma.find()) {
					subdiv = ma.group(1);
				}
			}
			subdiv = subdiv.trim();
			if (subdiv.length() != 0) {
				subdiv = subdiv.replaceAll("\\bUNREC(\\s*-\\s*)?ORDED PLAT( OF)?\\b", "");
				subdiv = subdiv.replaceAll("\\bPLAT UNRECORDED\\b", "");
				subdiv = subdiv.replaceAll("\\bREPLAT OF\\b", "");
				subdiv = subdiv.replaceFirst("^\\s*(IN|OF|TO)\\b\\s*", "");
				subdiv = subdiv.replaceFirst("\\s*\\b(BEING )?IN\\b.*", "");
				subdiv = subdiv.replaceAll("\\s*-\\s*", "-");
				subdiv = subdiv.replaceFirst("[,&;\\s-]+$", "");
				subdiv = subdiv.replaceFirst("\\s*#?\\b\\d+\\s*$", "");
				subdiv = subdiv.replaceFirst("\\s*\\b(BEG(IN(NING)?)?|COMM)( AT)?( [SWEN]{1,2}C?)?\\b.*", "");
				subdiv = subdiv.replaceFirst("\\s*\\b(INCLUDING|FRONTING|LYING)\\b.*", "");
				subdiv = subdiv.replaceFirst("\\s*\\bSTRIP TO CITY\\b", "");
				subdiv = subdiv.replaceFirst("^\\s*(\\b(LOT|BLK|ALSO:?|UNIT|TRACT)\\s*)+", "");
				subdiv = subdiv.replaceAll("\\.", "");
				subdiv = subdiv.replaceFirst("^[\\s-]+", "");
				subdiv = subdiv.replaceFirst("(\\b(LOT|BLK|ALSO:?|UNIT|TRACT)\\s*)+$", "");
				subdiv = subdiv.replaceFirst("THE FOLLOWING DESCRIBED PROPERTY:?", "");
				subdiv = subdiv.replaceFirst("^\\s*THAT\\b\\s*", "");
				subdiv = subdiv.replaceAll("\\s{2,}", " ").trim();
				if (subdiv.length() != 0) {
					m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
					if (legal.contains("CONDOMINIUM")) {
						m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
					}
				}
			}
		}
	}

	public static void legalFLJacksonTR(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerFLJacksonTR(m, legal);

		// add section, township and range extracted from PID
		String pin = (String) m.get("PropertyIdentificationSet.ParcelID");
		if (!StringUtils.isEmpty(pin)) {
			Matcher ma = Pattern.compile("^(\\d+)-(\\d+[A-Z]?)-(\\d+[A-Z]?)\\b").matcher(pin);
			if (ma.find()) {
				List<List> bodySTR = new ArrayList<List>();
				ArrayList line = new ArrayList<String>();
				line.add(ma.group(1).replaceFirst("^0+", ""));
				line.add(ma.group(2).replaceFirst("^0+", ""));
				line.add(ma.group(3).replaceFirst("^0+", ""));
				bodySTR.add(line);

				String[] header = { "SubdivisionSection", "SubdivisionTownship", "SubdivisionRange" };

				Map<String, String[]> map = new HashMap<String, String[]>();
				map.put("SubdivisionSection", new String[] { "SubdivisionSection", "" });
				map.put("SubdivisionTownship", new String[] { "SubdivisionTownship", "" });
				map.put("SubdivisionRange", new String[] { "SubdivisionRange", "" });

				ResultTable pis = new ResultTable();
				pis.setHead(header);
				pis.setBody(bodySTR);

				ResultTable pisSTR = (ResultTable) m.get("PropertyIdentificationSet");
				if (pisSTR != null) {
					pis = ResultTable.joinVertical(pis, pisSTR, false);
				}
				pis.setMap(map);
				m.put("PropertyIdentificationSet", pis);
			}
		}
	}

	public static String cleanLegalFLIndianRiverDASLRV(String legal) {
		return cleanLegalFLEscambiaDASLRV(legal);
	}

	@SuppressWarnings("unchecked")
	public static void legalFLIndianRiverDASLRV(ResultMap m, long searchId) throws Exception {
		legalFLEscambiaDASLRV(m, searchId);
	}

	public static String cleanRemarksFLIndianRiverDASLRV(String legal) {
		return cleanRemarksFLEscambiaDASLRV(legal);
	}

	@SuppressWarnings("unchecked")
	public static void legalRemarksFLIndianRiverRV(ResultMap m, long searchId) throws Exception {
		legalRemarksFLEscambiaRV(m, searchId);
	}

	public static String cleanRemarksFLBayDASLRV(String legal) {
		legal = legal.replaceAll("SEC (\\d+-\\d+-\\d+) & (\\d+-\\d+-\\d+)", "SEC $1 SEC $2");

		String prevLegal;
		do {
			prevLegal = legal;
			legal = legal.replaceAll("(OR|O R|ORB)\\s*(\\d+)\\s*(?:PG?)\\s*(\\d+)\\s*&\\s*(\\d+)\\s*(?:PG?)\\s*(\\d+)", "OR $2 P $3 OR $4 P $5");
		} while (!legal.equals(prevLegal));

		// numbers
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, new String[] { "I", "M", "C", "L", "D" });
		legal = replaceNumbers(legal);

		// APT#120
		legal = legal.replaceAll("#(\\d+)", "$1");

		// BLK A-93
		// UNIT D-4
		legal = legal.replaceAll("(?:\\s|^)([A-Z])-(\\d+)(?:\\s|$)", " $1$2 ");

		// PTL8B20
		legal = legal.replaceAll("L(\\d+)B(\\d+)", " L $1 B $2 ");

		// duplicated spaces
		legal = legal.trim().replaceAll("\\s{2,}", " ");

		return legal;
	}

	public static void legalRemarksFLBayRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (StringUtils.isEmpty(legal)) {
			return;
		}
		legal = cleanRemarksFLBayDASLRV(legal);
		String legalTemp = legal;

		/*
		 * S/T/R Parsing
		 */
		// PCL SEC 18-3-16 & 19-3-16

		List<List> body = getSTRFromMap(m); // first add sec-twn-rng extracted
											// from XML specific tags, if any
											// (for DT use)
		Pattern pat = Pattern.compile("\\bSEC (\\d{1,2})(?: TW[NP] | T|-)(\\d{1,2}[NESW]?)(?: RNG | R|-)(\\d{1,2}[NESW]?)");
		Matcher mat = pat.matcher(legal);
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");
			List<String> line = new ArrayList<String>();
			line.add(mat.group(1));
			line.add(mat.group(2));
			line.add(mat.group(3));
			body.add(line);
			legalTemp = legalTemp.replaceAll("\\s{2,}", " ").trim();
			legal = legalTemp;
		}
		saveSTRInMap(m, body);

		String NUMBER_SEQ = "([0-9]+[A-Z]?([ &,-]+[0-9]+[A-Z]?)*\\b)";
		String LETTER_SEQ = "([A-Z][0-9]*([ &,-]+[A-Z][0-9]*)*\\b)";

		/*
		 * CR parsing
		 */
		// OR576 551, 868 2370, 905 2414, 912 1486, 915 2128,
		pat = Pattern.compile("OR(\\d+ \\d+(?:, *\\d+ \\d+)+)");
		mat = pat.matcher(legal);
		while (mat.find()) {
			String cr = "OR " + mat.group(1).replaceAll(",", " OR ");
			cr = cr.replaceAll("\\s{2,}", " ").trim();
			legalTemp = legalTemp.replaceFirst(mat.group(0), cr);
		}
		legal = legalTemp;

		List<List> bodyCr = new ArrayList<List>();
		pat = Pattern.compile("(\\d{10} ?)?(ORB?|DB|O R|&) ?(?:BK )?(\\d+)\\s*(?:PG?|PAGES?|PGS?|/| )\\s*(" + NUMBER_SEQ + ")\\b");
		mat = pat.matcher(legal);
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");
			String nr = mat.group(1);
			if (nr == null) {
				nr = "";
			}
			String type = mat.group(2).replace(" ", "").replace("&", "");
			String book = mat.group(3);
			String page = mat.group(4).replaceAll("&", " ");
			List<String> line = new ArrayList<String>();
			line.add(book);
			line.add(page);
			line.add(nr);
			line.add(type);
			bodyCr.add(line);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		pat = Pattern.compile("(?:\\s|^)(\\d+)/(\\d+)(?:\\s|$)");
		mat = pat.matcher(legal);
		while (mat.find()) {

			String nr = "";
			String type = "";
			String book = mat.group(1);
			String page = mat.group(2);
			if (book.length() < 2 && page.length() < 2 || (page.length() == 2 && book.length() == 2)) {
				continue;
			}
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");
			List<String> line = new ArrayList<String>();
			line.add(book);
			line.add(page);
			line.add(nr);
			line.add(type);
			bodyCr.add(line);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// upload into map
		saveCRInMap(m, bodyCr);

		/*
		 * LOT parsing
		 */
		String lot = "";
		pat = Pattern.compile("\\b(?:LT?|LOT)\\s*([0-9]+[A-Z]?(?:[ &,-]+[0-9]+[A-Z]?)*)");
		mat = pat.matcher(legal);
		while (mat.find()) {
			lot = lot + " " + mat.group(1);
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");
		}
		lot = lot.replaceAll("\\s*&\\s*", " ").trim();
		if (lot.length() != 0) {
			String prevLot = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (prevLot != null) {
				lot = prevLot + " " + lot;
			}
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		/*
		 * UNIT parsing
		 */
		pat = Pattern.compile("(?:UNITS?|\\bUN?|APTS?)\\s*(?:NO? |# ?)?(" + NUMBER_SEQ + "|" + LETTER_SEQ + ")");
		mat = pat.matcher(legal);
		String unit = "";
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");
			unit = unit + " " + mat.group(1);
			legalTemp = legalTemp.replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		pat = Pattern.compile("\\bU\\s*(\\d+)");
		mat = pat.matcher(legal);
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");
			unit = unit + " " + mat.group(1);
			legalTemp = legalTemp.replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		unit = LegalDescription.cleanValues(unit, false, true);
		if (!StringUtils.isEmpty(unit)) {
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit);
		}

		/*
		 * BUILDING parsing
		 */
		pat = Pattern.compile("\\b(?:BLDG?|BUILDING)\\s*(?:NO )?(" + NUMBER_SEQ + "|" + LETTER_SEQ + ")");
		mat = pat.matcher(legal);
		String bldg = "";
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");
			bldg = bldg + " " + mat.group(1);
			legalTemp = legalTemp.replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}
		bldg = LegalDescription.cleanValues(bldg, false, true);
		if (!StringUtils.isEmpty(bldg)) {
			m.put("PropertyIdentificationSet.SubdivisionBldg", bldg);
		}

		/*
		 * BLOCK parsing
		 */
		pat = Pattern.compile("\\b(?:BLOCKS?|BLKS?|BL?)\\s*(" + NUMBER_SEQ + "|" + LETTER_SEQ + ")");
		mat = pat.matcher(legal);
		String block = "";
		while (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");
			block += mat.group(1) + " ";
		}
		legalTemp = legalTemp.replaceAll("\\s{2,}", " ");
		legal = legalTemp;
		block = LegalDescription.cleanValues(block.replaceAll("&", " "), false, true);
		if (!StringUtils.isEmpty(block)) {
			String prevBlk = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (prevBlk != null) {
				block = prevBlk + " " + block;
			}
			block = LegalDescription.cleanValues(block, false, true);
			m.put("PropertyIdentificationSet.SubdivisionBlock", block.trim());
		}

		/*
		 * PHASE parsing
		 */
		pat = Pattern.compile("\\bPH(?:ASE)?(( (\\d+\\w?|\\b[A-Z]\\b))+)\\b");
		mat = pat.matcher(legal);
		if (mat.find()) {
			legalTemp = legalTemp.replaceFirst(mat.group(0), " @ ");
			m.put("PropertyIdentificationSet.SubdivisionPhase", mat.group(1).trim().replaceFirst("^0+(\\w.*)", "$1"));
			legalTemp = legalTemp.replaceAll("\\s{2,}", " ");
			legal = legalTemp;
		}

	}

	public static void legalRemarksFLLakeRV(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		// if legal descr contains an address, then don't attempt to extract any
		// legal elements
		if (legal.matches(".+ FLA? [\\d-]+"))
			return;

		// initial corrections and cleanup of legal description
		legal = replaceNumbers(legal);

		legal = legal.replaceAll("\\s+PT (\\d+)\\b", ",$1");
		legal = legal.replaceAll(",\\s+(\\d+|[A-Z])\\b", ",$1");
		legal = legal.replaceAll("\\bL(\\d)", "L $1");
		legal = legal.replaceAll("\\b(\\d+) (\\d+)\\b", "$1,$2");
		legal = legal.replaceAll("\\b(\\d+) (\\d+)\\b", "$1,$2");
		legal = legal.replaceAll("\\b(\\s*)(PTE/)?PTS?\\b\\s*", "$1");

		legal = legal.trim();
		legal = legal.replaceAll("\\s{2,}", " ");

		String legalTemp = legal;

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		Pattern p = Pattern.compile("\\b(LTS?)\\s*([\\d&\\s-,]+[,\\s]+|(?:\\d+-[A-Z])|[A-Z,\\s]+\\s+)");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			String lotFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (!StringUtils.isEmpty(lotFromSet)) {
				lot = lotFromSet + " " + lot;
			}
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract tract from legal description
		String tract = ""; // can have multiple occurrences
		p = Pattern.compile("(\\bTRA?CT ((\\b(\\d+|\\d+[A-Z]|[A-Z]\\d+|[A-Z]|,|-)\\b)+))");
		ma = p.matcher(legal);
		while (ma.find()) {
			tract = tract + " " + ma.group(2);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract block from legal description
		String block = "";
		String pat = "([A-Z]|\\d+[A-Z]?)\\b";
		p = Pattern.compile("(\\b(?:BLKS?)\\s*(" + pat + "([,-]" + pat + ")*))");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			block = ma.group(2).replaceAll("\\s*,\\s*", " ");
			String blockFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(blockFromSet)) {
				block = blockFromSet + " " + block;
				block = LegalDescription.cleanValues(block, false, true);
			}
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract cross refs from legal description
		p = Pattern.compile("(\\bOR\\s*(\\d+)\\s*PG\\s*(\\d+)\\b)");
		ma = p.matcher(legal);
		List<List> bodyCR = new ArrayList<List>();

		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			if (ma.group(2) == null)
				line.add("");
			else
				line.add(ma.group(2));
			if (ma.group(3) == null)
				line.add("");
			else
				line.add(ma.group(3));
			line.add("");
			line.add("O");
			bodyCR.add(line);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}

		p = Pattern.compile("(\\b(\\d+)?\\s*OR (\\d+)/(\\d+)\\b)");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			if (ma.group(3) == null)
				line.add("");
			else
				line.add(ma.group(3));
			if (ma.group(4) == null)
				line.add("");
			else
				line.add(ma.group(4));
			if (ma.group(2) == null)
				line.add("");
			else
				line.add(ma.group(2));
			line.add("O");
			bodyCR.add(line);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		saveCRInMap(m, bodyCR);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		String tokens[] = { "X", "L", "C", "D", "M" };
		// extract phase from legal description
		pat = "\\b(\\d+[A-Z]?|[IVX]+)\\b";
		p = Pattern.compile("(\\bPH(?:ASE)? (" + pat + "([,-]" + pat + ")*))");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionPhase", Roman.normalizeRomanNumbersExceptTokens(ma.group(2), tokens).replaceAll("\\s*,\\s*", " "));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract unit from legal description
		p = Pattern.compile("((?:#|UN #?)(\\w+([-.,]\\w+)*))(?! ADD)");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionUnit", Roman.normalizeRomanNumbersExceptTokens(ma.group(2), tokens).replaceAll("\\s*,\\s*", " "));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract building #
		p = Pattern.compile("(\\bBLDG #?(\\w+)\\b)");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// additional cleaning before extracting subdivision name
		legal = legal.replaceAll("^REPL(A?T)?\\b\\s*", "");
		legal = legal.replaceAll("\\s*(\\d+(ST|ND|RD|TH) )?ADD(ITIO)?N?( TO)?\\b\\s*", "");
		legal = legal.replaceAll("^\\s*SUBD?\\b\\s*", "");

		// extract subdivision name
		// String subdiv =
		// legal.replaceFirst("(.*?)\\s*\\b(SUBD?|REPL(A?T)?|SEC|CONDO|(\\d+(ST|ND|RD|TH)? )?ADD(ITIO)?N?|PH(ASE)?|PAR(CEL)?)\\b.*",
		// "$1");
		// subdiv = subdiv.replaceFirst("\\s*#$", "");
		// subdiv = subdiv.replaceFirst("^-\\s*", "");
		// subdiv = subdiv.replaceFirst("^\\s*\\d+\\s*$", "");
		//
		// if (subdiv.length() != 0){
		// m.put("PropertyIdentificationSet.SubdivisionName", subdiv);
		// if (legal.matches(".*\\b(CONDO(MINIUM)?)\\b.*"))
		// m.put("PropertyIdentificationSet.SubdivisionCond", subdiv);
		// }
	}

	public static void legalRemarksFLOsceolaRV(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");

		if (legal == null || legal.length() == 0)
			return;
		legal = legal.replaceAll("\\s*&\\s*", "-");
		legal = legal.replaceAll("(\\d+)\\s*,\\s*(\\d+)", "$1,$2");
		legal = legal.replaceAll("(\\d+)\\s*-\\s*(\\d+)", "$1-$2");
		legal = legal.replaceAll("-(\\D)", "- $1");
		legal = legal.replaceAll(",(\\D)", ", $1");
		legal = legal.replaceAll("(\\d+\\s*(?:ND|RD|TH|ST))", " $1");

		// initial corrections and cleanup of legal description
		legal = replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);

		String legalTemp = legal;

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		Pattern p = Pattern.compile("(?is)\\b(LTS?|LOTS?:?)\\s*((?:[A-Z]+)?[0-9-,]+[A-Z]?)");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			String lotFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (!StringUtils.isEmpty(lotFromSet)) {
				lot = lotFromSet + " " + lot;
			}
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract tract from legal description
		String tract = "";
		p = Pattern.compile("(?is)\\b(TRACTS?) ([\\d-]+|[A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = tract + " " + ma.group(2);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BL |BLKS?|B|BLOCK):? ([A-Z]?[0-9-]+[A-Z]?|[A-Z]\\s?|$)");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			block = ma.group(2).replaceAll("\\s*,\\s*", " ");
			String blockFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(blockFromSet)) {
				block = blockFromSet + " " + block;
				block = LegalDescription.cleanValues(block, false, true);
			}
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract cross refs from legal description
		p = Pattern.compile("\\b((?:OR)?\\s*(\\d{3,})\\s*/\\s*(\\d{3,})\\b)");
		ma = p.matcher(legal);
		List<List> bodyCR = new ArrayList<List>();

		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			if (ma.group(2) == null)
				line.add("");
			else
				line.add(ma.group(2));
			if (ma.group(3) == null)
				line.add("");
			else
				line.add(ma.group(3));
			line.add("");
			line.add("O");
			bodyCR.add(line);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}

		p = Pattern.compile("(\\b(\\d+)?\\s*OR (\\d+)/(\\d+)\\b)");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			if (ma.group(3) == null)
				line.add("");
			else
				line.add(ma.group(3));
			if (ma.group(4) == null)
				line.add("");
			else
				line.add(ma.group(4));
			if (ma.group(2) == null)
				line.add("");
			else
				line.add(ma.group(2));
			line.add("O");
			bodyCR.add(line);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		saveCRInMap(m, bodyCR);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract phase from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH|PHASES?)(( ([\\d-]+\\w?|\\b[A-Z]\\b))+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			phase = ma.group(2).replaceAll("\\s*,\\s*", " ");
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b((UNI?T?:? )(\\w+(\\s*[&-.,]\\s*\\w+)*))\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			unit = ma.group(3).replaceAll("\\s*,\\s*", " ");
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract building #
		p = Pattern.compile("(\\bBLDG #?(\\w+)\\b)");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2));
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract section, township, range
		List<List> body = getSTRFromMap(m);
		List<String> line;
		String sec = "";
		p = Pattern.compile("(?is)\\b(SEC|SECT(?:ION)?) ([\\d-]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			sec = ma.group(1).replaceAll("\\s*[&,]\\s*", " ");
			line.add(sec);
			line.add("");
			line.add("");
			body.add(line);
		}
		p = Pattern.compile("(?is)\\bTWP (\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			String twn = ma.group(1);
			line.add("");
			line.add(twn);
			line.add("");
			body.add(line);
		}

		p = Pattern.compile("\\b(?is)SECT: ([0-9A-Z]+)\\s+TSHP: ([0-9A-Z]+)\\s+RNG: ([0-9A-Z]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add(ma.group(3));
			body.add(line);
		}
		saveSTRInMap(m, body);
	}

	public static void legalRemarksFLPascoRV(ResultMap m, long searchId) throws Exception {

		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");

		if (legal == null || legal.length() == 0)
			return;

		legal = legal.replaceAll("(\\d+\\s*(?:ND|RD|TH|ST))", " $1");
		legal = legal.replaceAll("(\\d+/\\d+)", "$1 ");
		legal = legal.replace("BDIXIE", "B DIXIE");
		legal = legal.replaceAll("(\\d+)\\s*THRU\\s*(\\d+)", "$1-$2");
		legal = legal.replaceAll("(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");

		// initial corrections and cleanup of legal description
		legal = replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);

		String legalTemp = legal;

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		Pattern p = Pattern.compile("(?is)\\b(LS?|LTS?)\\s*([\\d&-]+|(?:\\d+[A-Z]\\d?))\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			String lotFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (!StringUtils.isEmpty(lotFromSet)) {
				lot = lotFromSet + " " + lot;
			}
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract tract from legal description
		String tract = "";
		p = Pattern.compile("(?is)\\b(TR)\\s*(\\d+| [A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = tract + " " + ma.group(2);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			m.put("PropertyIdentificationSet.SubdivisionTract", tract.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BLS?|B)\\s*(\\d+|(?:[A-Z]&?[A-Z]?)|(?:\\d+[A-Z]))\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			block = ma.group(2).replaceAll("\\s*,\\s*", " ");
			if (block.trim().equals("CH"))
				block = block.replaceAll("CH", "");// for BCH that means BEACH
			String blockFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(blockFromSet)) {
				block = blockFromSet + " " + block;
				block = LegalDescription.cleanValues(block, false, true);
			}
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract cross refs from legal description
		p = Pattern.compile("(?is)\\b(O(?:R|B)|PG)\\s*(\\d+)\\s*(P(?:G|B))\\s*(\\d+)");
		ma = p.matcher(legal);
		List<List> bodyCR = new ArrayList<List>();

		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			if (ma.group(2) == null)
				line.add("");
			else
				line.add(ma.group(2));
			if (ma.group(4) == null)
				line.add("");
			else
				line.add(ma.group(4));
			line.add("");
			line.add("O");
			bodyCR.add(line);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		saveCRInMap(m, bodyCR);
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract phase from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH)\\s*([\\dA-Z&]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			phase = ma.group(2).replaceAll("\\s*,\\s*", " ");
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b((U)\\s*(\\w+(\\s*[&-.,]\\s*\\w+)*))\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			unit = ma.group(3).replaceAll("\\s*,\\s*", " ");
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract building #
		p = Pattern.compile("(\\bBD\\s*(\\w+)\\b)");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2).trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract plat b&p
		p = Pattern.compile("(?is)\\bPB\\s*(\\d+)\\s*PGS?\\s*([\\d+&-]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.PlatBook", ma.group(1).trim());
			m.put("PropertyIdentificationSet.PlatNo", ma.group(2).trim());
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract section, township, range
		List<List> body = getSTRFromMap(m);
		List<String> line;
		// String sec="", twp="", rng="";
		p = Pattern.compile("(?is)\\b(\\d+)\\s*-\\s*(\\d+)\\s*-\\s*(\\d+)");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add(ma.group(1));
			line.add(ma.group(2));
			line.add(ma.group(3));
			body.add(line);
		}
		saveSTRInMap(m, body);

	}

	public static void legalTokenizerRemarksFLSumterRV(ResultMap m, String legal) throws Exception {

		legal = legal.replaceAll("(\\d+\\s*(?:ND|RD|TH|ST))", " $1");
		legal = legal.replaceAll("(\\d+/\\d+)", "$1 ");

		legal = legal.replaceAll("(\\d+)\\s*THRU\\s*(\\d+)", "$1-$2");
		legal = legal.replaceAll("(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");
		legal = legal.replaceAll("Sub_Lot=", "LOT ");
		legal = legal.replaceAll("(UNIT) NO", "$1 ");
		legal = legal.replaceAll("\\-,", " ");

		// initial corrections and cleanup of legal description
		legal = replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);

		String legalTemp = legal;

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		Pattern p = Pattern.compile("(?is)\\b(LO?TS?)\\*?\\s*([\\s\\d&-]+|(?:\\d+[A-Z]\\d?))\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			String lotFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (!StringUtils.isEmpty(lotFromSet)) {
				lot = lotFromSet + " " + lot;
			}
			lot = lot.replaceAll("&", "");
			lot = LegalDescription.cleanValues(lot, false, true);
			if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.SubdivisionLotNumber")))
				m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract tract from legal description
		String tract = "";
		p = Pattern.compile("(?is)\\b(TR)\\s*(\\d+| [A-Z])\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			tract = tract + " " + ma.group(2);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		tract = tract.trim();
		if (tract.length() != 0) {
			tract = LegalDescription.cleanValues(tract, false, true);
			if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.SubdivisionTract")))
				m.put("PropertyIdentificationSet.SubdivisionTract", tract.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BLKS?)\\s*(\\d+|(?:[A-Z]&?[A-Z]?)|(?:\\d+[A-Z]))\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			block = ma.group(2).replaceAll("\\s*,\\s*", " ");
			if (block.trim().equals("CH"))
				block = block.replaceAll("CH", "");// for BCH that means BEACH
			String blockFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(blockFromSet)) {
				block = blockFromSet + " " + block;
				block = LegalDescription.cleanValues(block, false, true);
			}
			if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.SubdivisionBlock")))
				m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract cross refs from legal description

		String crossRefSetInstr = (String) m.get("CrossRefSet.InstrumentNumber");
		if (crossRefSetInstr == null)
			crossRefSetInstr = "";
		String crossRefSetBook = (String) m.get("CrossRefSet.Book");
		if (crossRefSetBook == null)
			crossRefSetBook = "";
		String crossRefSetPage = (String) m.get("CrossRefSet.Page");
		if (crossRefSetPage == null)
			crossRefSetPage = "";

		List<List> bodyCR = new ArrayList<List>();

		p = Pattern.compile("(?is)\\bAff_Bk=([^\\s]+)\\s+Aff_Pg=([^\\s]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			if (ma.group(1) == null)
				line.add("");
			else if (!crossRefSetBook.equals(ma.group(1)))
				line.add(ma.group(1));
			else
				line.add("");
			if (ma.group(2) == null)
				line.add("");
			else if (!crossRefSetPage.equals(ma.group(1)))
				line.add(ma.group(2));
			else
				line.add("");
			line.add("");
			line.add("O");
			bodyCR.add(line);
		}
		ma.reset();
		p = Pattern.compile("(?is)\\bOR\\s+(\\d+)\\s+(?:PAGE|PG\\b)\\s+([\\d-]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			if (ma.group(1) == null)
				line.add("");
			else if (!crossRefSetBook.equals(ma.group(1)))
				line.add(ma.group(1));
			else
				line.add("");
			if (ma.group(2) == null)
				line.add("");
			else if (!crossRefSetPage.equals(ma.group(1)))
				line.add(ma.group(2));
			else
				line.add("");
			line.add("");
			line.add("O");
			bodyCR.add(line);
		}
		ma.reset();
		p = Pattern.compile("(?is)\\bAff_Year=([^\\s]+)\\s+Aff_Num=([^\\s]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			if (ma.group(1) == null && ma.group(2) == null)
				line.add("");
			else if (!crossRefSetInstr.equals(ma.group(1) + " " + ma.group(2)))
				line.add(ma.group(1) + " " + ma.group(2));
			else
				line.add("");
			line.add("O");
			bodyCR.add(line);
		}
		ma.reset();
		p = Pattern.compile("(?is)\\bCaseNo=([\\dA-Z-]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			if (ma.group(1) == null)
				line.add("");
			else
				line.add(ma.group(1));
			line.add("O");
			bodyCR.add(line);
		}
		saveCRInMap(m, bodyCR);

		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract phase from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH)\\s*([\\dA-Z&]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			phase = ma.group(2).replaceAll("\\s*,\\s*", " ");
			if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.SubdivisionPhase")))
				m.put("PropertyIdentificationSet.SubdivisionPhase", phase.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b((UN(?:IT)?)\\s+(\\w+(\\s*[&.,]\\s*\\w+)*))\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			unit = ma.group(3).replaceAll("\\s*,\\s*", " ");
			if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.SubdivisionUnit")))
				m.put("PropertyIdentificationSet.SubdivisionUnit", unit.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract building #
		p = Pattern.compile("(\\bBD\\b\\s*(\\w+)\\b)");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.SubdivisionBldg")))
				m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2).trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract plat b&p
		p = Pattern.compile("(?is)\\bPB\\s*(\\d+)\\s*P(?:G|B)S?\\s*([\\d+A-Z&-]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.PlatBook"))
					&& StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.PlatNo"))) {
				m.put("PropertyIdentificationSet.PlatBook", ma.group(1).trim());
				m.put("PropertyIdentificationSet.PlatNo", ma.group(2).trim());
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract section, township, range
		List<List> body = getSTRFromMap(m);
		List<String> line;
		// String sec="", twp="", rng="";
		p = Pattern.compile("(?is)\\bSectn=(\\d+)\\s+TwnShp=([\\dA-Z]+)\\s+Range=([\\dA-Z]+)\\b");
		String sect = "";
		String twnshp = "";
		String range = "";
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			if (!sect.equals(ma.group(1)) || !twnshp.equals(ma.group(2)) || !range.equals(ma.group(3))) {
				line.add(ma.group(1));
				line.add(ma.group(2));
				line.add(ma.group(3));
				sect = ma.group(1);
				twnshp = ma.group(2);
				range = ma.group(3);
				body.add(line);
			}
		}
		saveSTRInMap(m, body);

	}

	public static void legalRemarksFLSumterRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerRemarksFLSumterRV(m, legal);
	}

	public static void legalFLSumterDASLRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerFLSumterDASLRV(m, legal);
	}

	public static void legalTokenizerFLSumterDASLRV(ResultMap m, String legal) throws Exception {

		if (StringUtils.isEmpty(legal))
			return;

		legal = legal.replaceAll("(?is)\\b(\\d+)(PB)\\b", "$1 $2");
		legal = legal.replaceAll("UNIT NO\\.?", "UNIT ");
		legal = legal.replaceAll("(?is)\\b(\\d+)([A-Z]{3,})", "$1 $2");
		legal = legal.replaceAll("(?is)(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");

		legal = replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);

		String legalTemp = legal;

		String lot = ""; // can have multiple occurrences
		Pattern p = Pattern.compile("(?is)\\b(LOTS?)\\s+([\\s\\d&,-]+|(?:\\d+[A-Z]\\d?))\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			String lotFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (!StringUtils.isEmpty(lotFromSet)) {
				lot = lotFromSet + " " + lot;
			}
			lot = lot.replaceAll("&", "");
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BLKS?|BLOCKS?)\\s+(\\d+|[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			block = ma.group(2).replaceAll("\\s*,\\s*", " ");
			if (block.trim().equals("CH"))
				block = block.replaceAll("CH", "");// for BCH that means BEACH
			String blockFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(blockFromSet)) {
				block = blockFromSet + " " + block;
				block = LegalDescription.cleanValues(block, false, true);
			}
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b((UN(?:IT)?)\\s+(\\w+(\\s*[&.,]\\s*\\w+)*))\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			unit = ma.group(3).replaceAll("\\s*,\\s*", " ");
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract tract #
		p = Pattern.compile("(?is)\\b(TRACT)\\s+(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionTract", ma.group(2).trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract building #
		p = Pattern.compile("(?is)\\b(BLDG)\\s+(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2).trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract plat b&p
		p = Pattern.compile("(?is)\\b(?:PB|PLAT\\s+BOOK)\\s*(\\d+)\\s*(?:P(?:G|B)S?|PAGES?)\\s*([\\d+A-Z&-]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.PlatBook", ma.group(1).trim());
			m.put("PropertyIdentificationSet.PlatNo", ma.group(2).trim());
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();
		p = Pattern.compile("(?is)\\bOR\\s+(\\d+)\\s+PG\\s+(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			if (ma.group(1) == null)
				line.add("");
			else
				line.add(ma.group(1));
			if (ma.group(2) == null)
				line.add("");
			else
				line.add(ma.group(2));
			line.add("");
			line.add("O");
			bodyCR.add(line);
		}
		saveCRInMap(m, bodyCR);

		String remarks = (String) m.get("SaleDataSet.Remarks");
		if (remarks != null)
			legalTokenizerRemarksFLSumterRV(m, remarks);
	}

	public static void legalTokenizerRemarksFLAlachuaRV(ResultMap m, String legal) throws Exception {

		legal = legal.replaceAll("(\\d+\\s*(?:ND|RD|TH|ST))", " $1");
		legal = legal.replaceAll("(\\d+/\\d+)", "$1 ");

		legal = legal.replaceAll("(\\d+)\\s*THRU\\s*(\\d+)", "$1-$2");
		legal = legal.replaceAll("(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");
		legal = legal.replaceAll("(UNIT) NO", "$1 ");
		legal = legal.replaceAll("\\-,", " ");
		legal = legal.replaceAll("ETC", "");
		legal = legal.replaceAll("&amp;", "&");
		legal = legal.replaceAll("CONDOMONIUM", "CONDOMINIUM");
		legal = legal.replaceAll("PLATA BOOK", "PLAT BOOK");

		// initial corrections and cleanup of legal description
		legal = replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);

		String legalTemp = legal;

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		Pattern p = Pattern.compile("(?is)\\b(LOT|LT)\\s+([\\dA-Z-&]+)\\b");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			String lotFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (!StringUtils.isEmpty(lotFromSet)) {
				lot = lotFromSet + " " + lot;
			}
			lot = LegalDescription.cleanValues(lot, false, true);
			if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.SubdivisionLotNumber")))
				m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BLOCK|BLK)\\s+([\\dA-Z&]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			block = ma.group(2).replaceAll("\\s*,\\s*", " ");
			if (block.trim().equals("CH"))
				block = block.replaceAll("CH", "");// for BCH that means BEACH
			String blockFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(blockFromSet)) {
				block = blockFromSet + " " + block;
				block = LegalDescription.cleanValues(block, false, true);
			}
			if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.SubdivisionBlock")))
				m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();

		p = Pattern.compile("(?is)\\bBK\\s+(\\d+)\\s+PG\\s+(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			if (ma.group(1) == null)
				line.add("");
			else
				line.add(ma.group(1));
			if (ma.group(2) == null)
				line.add("");
			else
				line.add(ma.group(2));
			line.add("");
			line.add("O");
			bodyCR.add(line);
		}
		ma.reset();
		p = Pattern.compile("(?is)\\b(OR\\s+)?(\\d+)\\s*/\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			if (ma.group(2) == null)
				line.add("");
			else
				line.add(ma.group(2));
			if (ma.group(3) == null)
				line.add("");
			else
				line.add(ma.group(3));
			line.add("");
			line.add("O");
			bodyCR.add(line);
		}
		ma.reset();
		p = Pattern.compile("(?is)\\b(OFFICIAL\\s+RECORDS|OR)\\s+BOOK\\s+(\\d+)\\s+PAGE\\s+(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			if (ma.group(2) == null)
				line.add("");
			else
				line.add(ma.group(2));
			if (ma.group(3) == null)
				line.add("");
			else
				line.add(ma.group(3));
			line.add("");
			line.add("O");
			bodyCR.add(line);
		}
		ma.reset();
		p = Pattern.compile("(?is)\\b(CONDOMINIUM|DEED\\s+BOOK)\\s+([\\dA-Z]+)\\s+PAGE\\s+([\\dA-Z-]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			if (ma.group(2) == null)
				line.add("");
			else
				line.add(ma.group(2));
			if (ma.group(3) == null)
				line.add("");
			else
				line.add(ma.group(3));
			line.add("");
			line.add("O");
			bodyCR.add(line);
		}
		ma.reset();
		p = Pattern.compile("(?is)\\b(CFN)\\s+(\\d+)\\b");
		ma = p.matcher(legal);
		String relDocNo = (String) m.get("CrossRefSet.InstrumentNumber");
		if (relDocNo == null)
			relDocNo = "";
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			line.add("");
			line.add("");
			if (ma.group(2) == null)
				line.add("");
			else if (!ma.group(2).equals(relDocNo))
				line.add(ma.group(2));
			else
				line.add("");
			line.add("O");
			bodyCR.add(line);
		}
		saveCRInMap(m, bodyCR);

		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract phase from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PHASE|PH)\\s+([\\dA-Z]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			phase = ma.group(2).replaceAll("\\s*,\\s*", " ");
			if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.SubdivisionPhase")))
				m.put("PropertyIdentificationSet.SubdivisionPhase", phase.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(UNIT)\\s+([\\dA-Z-#]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			unit = ma.group(2).replaceAll("\\s*,\\s*", " ");
			unit = unit.replaceAll("#", "");
			if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.SubdivisionUnit")))
				m.put("PropertyIdentificationSet.SubdivisionUnit", unit.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract building #
		p = Pattern.compile("(?is)\\b(BLD|BUILDING)\\s+(\\w+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.SubdivisionBldg")))
				m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2).trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract plat b&p
		p = Pattern.compile("(?is)\\b(PLAT|CONDOMINIUM|DEED)\\s+(?:BOOK)\\s+([\\dA-Z]+)\\s+PAGES?\\s+([\\dA-Z-]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			if (StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.PlatBook"))
					&& StringUtils.isEmpty((String) m.get("PropertyIdentificationSet.PlatNo"))) {
				m.put("PropertyIdentificationSet.PlatBook", ma.group(2).trim());
				m.put("PropertyIdentificationSet.PlatNo", ma.group(3).trim());
			}
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract section, township, range
		List<List> body = getSTRFromMap(m);
		List<String> line;
		// String sec="", twp="", rng="";
		p = Pattern.compile("(?is)\\b(?:SEC)\\s+(\\d+)\\s*;?\\s*(?:TOWN|TWN)\\s+([\\dA-Z]+)\\s*;?\\s*(?:RANGE|RNG)\\s+([\\dA-Z]+)\\b");
		String sect = "";
		String twnshp = "";
		String range = "";
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			if (!sect.equals(ma.group(1)) || !twnshp.equals(ma.group(2)) || !range.equals(ma.group(3))) {
				line.add(ma.group(1));
				line.add(ma.group(2));
				line.add(ma.group(3));
				sect = ma.group(1);
				twnshp = ma.group(2);
				range = ma.group(3);
				body.add(line);
			}
		}
		saveSTRInMap(m, body);

	}

	public static void legalRemarksFLAlachuaRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerRemarksFLAlachuaRV(m, legal);
	}

	public static void legalFLAlachuaDASLRV(ResultMap m, long searchId) throws Exception {
		String legal = (String) m.get("PropertyIdentificationSet.PropertyDescription");
		if (legal == null || legal.length() == 0)
			return;

		legalTokenizerFLAlachuaDASLRV(m, legal);
	}

	public static void legalTokenizerFLAlachuaDASLRV(ResultMap m, String legal) throws Exception {

		legal = legal.replaceAll("(\\d+\\s*(?:ND|RD|TH|ST))", " $1");
		legal = legal.replaceAll("(\\d+/\\d+)", "$1 ");
		legal = legal.replaceAll("\\b(\\d+)\\s*&\\s+(\\d+)\\s+", "$1&$2");
		legal = legal.replaceAll("\\b(-\\d+[A-Z]?)\\s+&\\s+(\\d+[A-Z])\\s+", "$1&$2 ");
		legal = legal.replaceAll("\\b&\\s+\\d+\\s+FT\\b", "");
		legal = legal.replaceAll("(\\d+)(OR|AKA|PB|LOT)\\b", "$1 $2");
		legal = legal.replaceAll("(?is)(OR\\s*\\d+\\s*/\\s*\\d+)", " $1");
		legal = legal.replaceAll("(\\d+)\\s*THRU\\s*(\\d+)", "$1-$2");
		legal = legal.replaceAll("(\\d+)\\s*TO\\s*(\\d+)", "$1-$2");
		legal = legal.replaceAll("(UNIT) NO", "$1 ");
		legal = legal.replaceAll("\\-,", " ");
		legal = legal.replaceAll("ETC", "");
		legal = legal.replaceAll("&amp;", "&");
		legal = legal.replaceAll("CONDOMONIUM", "CONDOMINIUM");
		legal = legal.replaceAll("PLATA BOOK", "PLAT BOOK");

		// initial corrections and cleanup of legal description
		legal = replaceNumbers(legal);
		String[] exceptionTokens = { "I", "M", "C", "L", "D" };
		legal = Roman.normalizeRomanNumbersExceptTokens(legal, exceptionTokens);

		String legalTemp = legal;

		// extract lot from legal description
		String lot = ""; // can have multiple occurrences
		Pattern p = Pattern.compile("(?is)\\b(LOTS?)\\s+([\\d\\s&]+|[\\d-]+[A-Z])(?:\\s|\\z)");
		Matcher ma = p.matcher(legal);
		while (ma.find()) {
			lot = lot + " " + ma.group(2);
			legalTemp = legalTemp.replace(ma.group(1), "");
		}
		lot = lot.trim();
		if (lot.length() != 0) {
			String lotFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionLotNumber");
			if (!StringUtils.isEmpty(lotFromSet)) {
				lot = lotFromSet + " " + lot;
			}
			lot = lot.replaceAll("\\s*&\\s*", " ");
			lot = LegalDescription.cleanValues(lot, false, true);
			m.put("PropertyIdentificationSet.SubdivisionLotNumber", lot.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract block from legal description
		String block = "";
		p = Pattern.compile("(?is)\\b(BK|BLK)\\s+([\\dA-Z]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			block = ma.group(2).replaceAll("\\s*,\\s*", " ");
			if (block.trim().equals("CH"))
				block = block.replaceAll("CH", "");// for BCH that means BEACH
			String blockFromSet = (String) m.get("PropertyIdentificationSet.SubdivisionBlock");
			if (!StringUtils.isEmpty(blockFromSet)) {
				block = blockFromSet + " " + block;
				block = LegalDescription.cleanValues(block, false, true);
			}
			m.put("PropertyIdentificationSet.SubdivisionBlock", block);
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract cross refs from legal description
		List<List> bodyCR = new ArrayList<List>();

		p = Pattern.compile("(?is)\\b(OR)\\s*(\\d+)\\s*/\\s*(\\d+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			List<String> line = new ArrayList<String>();
			if (ma.group(2) == null)
				line.add("");
			else
				line.add(ma.group(2).replaceAll("\\b0+(\\d+)", "$1"));
			if (ma.group(3) == null)
				line.add("");
			else
				line.add(ma.group(3).replaceAll("\\b0+(\\d+)", "$1"));
			line.add("");
			line.add("O");
			bodyCR.add(line);
		}
		saveCRInMap(m, bodyCR);

		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract phase from legal description
		String phase = "";
		p = Pattern.compile("(?is)\\b(PH)(?:ASE)?\\s+(\\d+|\\d?[A-Z])\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			phase = ma.group(2).replaceAll("\\s*,\\s*", " ");
			m.put("PropertyIdentificationSet.SubdivisionPhase", phase.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract unit from legal description
		String unit = "";
		p = Pattern.compile("(?is)\\b(UNIT)\\s+(\\d+|[-\\dA-Z]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			unit = ma.group(2).replaceAll("\\s*,\\s*", " ");
			unit = unit.replaceAll("#", "");
			m.put("PropertyIdentificationSet.SubdivisionUnit", unit.trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract building #
		p = Pattern.compile("(?is)\\b(BLDG)\\s+(\\d+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionBldg", ma.group(2).trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract tract #
		p = Pattern.compile("(?is)\\b(TRACTS?)\\s+([\\d\\s&]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			legalTemp = legalTemp.replace(ma.group(1), "");
			m.put("PropertyIdentificationSet.SubdivisionTract", ma.group(2).trim());
		}
		legalTemp = legalTemp.trim().replaceAll("\\s{2,}", " ");
		legal = legalTemp;

		// extract plat b&p
		p = Pattern.compile("(?is)\\b((?:P|D)B)\\s+([\\dA-Z]+)\\s*-\\s*([\\dA-Z&-]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.PlatBook", ma.group(2).trim());
			m.put("PropertyIdentificationSet.PlatNo", ma.group(3).trim());
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}
		ma.reset();
		p = Pattern.compile("(?is)\\b(?:CONDO\\s+)?(BK|PB-?)\\s*([\\dA-Z]+)\\s+PGS?-?\\s*([\\dA-Z-]+)\\b");
		ma = p.matcher(legal);
		if (ma.find()) {
			m.put("PropertyIdentificationSet.PlatBook", ma.group(2).trim());
			m.put("PropertyIdentificationSet.PlatNo", ma.group(3).trim());
			legal = legal.replaceFirst("\\b" + ma.group(0) + "\\b", "");
			legal = legal.trim().replaceAll("\\s{2,}", " ");
		}

		// extract section, township, range
		List<List> body = getSTRFromMap(m);
		List<String> line;
		// String sec="", twp="", rng="";
		p = Pattern.compile("(?is)\\b(?:RG)\\s+([\\dA-Z]+)\\b");
		ma = p.matcher(legal);
		while (ma.find()) {
			line = new ArrayList<String>();
			line.add("");
			line.add("");
			line.add(ma.group(1));
			body.add(line);
		}
		saveSTRInMap(m, body);

		String remarks = (String) m.get("SaleDataSet.Remarks");

		if (remarks != null)
			legalTokenizerRemarksFLAlachuaRV(m, remarks);
	}

	public static void correctInstNoAfterCoreLogicUpgrade(ResultMap m, long searchId) {
		try {
			String crtState = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
			String crtCounty = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();

			String instrNumber = (String) m.get("SaleDataSet.InstrumentNumber");

			String financeBook = "";
			String financePage = "";
			String financeInstr = "";
			String documentNumberRaw = "";

			String financeRecordedate = "";
			DeferredElementImpl el = (DeferredElementImpl) m.get("tmpInstrument");

			if (el != null) {
				NodeList list = el.getChildNodes();

				for (int i = 0; i < list.getLength(); i++) {
					Node node = list.item(i);
					if ("FinanceBookPage".equalsIgnoreCase(node.getNodeName())) {
						String value = node.getTextContent();

						int poz = value.indexOf("-");

						if (poz > 0) {
							financeBook = value.substring(0, poz);
							if (poz < value.length() - 1) {
								financePage = value.substring(poz + 1);
							}
						}

					} else if ("FinanceInstrumentNumber".equalsIgnoreCase(node.getNodeName())) {
						financeInstr = node.getTextContent();
					} else if ("FinanceRecordedDate".equalsIgnoreCase(node.getNodeName())) {
						financeRecordedate = node.getTextContent();
					} else if ("DocumentNumberRaw".equalsIgnoreCase(node.getNodeName())) {
						documentNumberRaw = node.getTextContent();
					}
				}
			}

			boolean findFinanceInformation = false;
			if (StringUtils.isNotEmpty(financeBook) && StringUtils.isNotEmpty(financePage)) {
				findFinanceInformation = true;
				m.put("SaleDataSet.FinanceBook", financeBook.trim().replaceAll("\\A0+", ""));
				m.put("SaleDataSet.FinancePage", financePage.trim().replaceAll("\\A0+", ""));
			}
			if (StringUtils.isNotEmpty(financeInstr)) {
				findFinanceInformation = true;
				m.put("SaleDataSet.FinanceInstrumentNumber", financeInstr.trim().replaceAll("\\A0+", ""));
			}
			if (StringUtils.isNotEmpty(financeRecordedate)) {
				findFinanceInformation = true;
				m.put("SaleDataSet.FinanceRecordedDate", financeRecordedate.trim());
			}
			if (!findFinanceInformation && StringUtils.isEmpty(instrNumber)) {
				return;
			}

			if (StringUtils.isNotEmpty(documentNumberRaw)) {
				int poz = documentNumberRaw.indexOf('-');
				if (poz > 0 && poz < documentNumberRaw.length() - 1) {
					m.put("SaleDataSet.Book", documentNumberRaw.substring(0, poz).trim().replaceAll("\\A0+", ""));
					m.put("SaleDataSet.Page", documentNumberRaw.substring(poz + 1).trim().replaceAll("\\A0+", ""));
					m.put("SaleDataSet.InstrumentNumber", "");
				} else {
					m.put("SaleDataSet.InstrumentNumber", documentNumberRaw.trim().replaceAll("[^a-zA-Z0-9]*", "").replaceAll("\\A0+", ""));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// B3746. if recorded date is missing or is like this form 1/1/0001 then
	// recorded date become 01/01/1970
	public static void transfomRecordedDate(ResultMap m, long searchId) throws Exception {
		String recordedDate = (String) m.get("SaleDataSet.RecordedDate");
		if (StringUtils.isEmpty(recordedDate) || "1/1/0001".equals(recordedDate)) {
			recordedDate = "01/02/1970";
			m.put("SaleDataSet.RecordedDate", recordedDate);
		}
	}

	public static void parseName(ResultMap m, String name, List<List>/* String */additionalNames) throws Exception {
		parseName(m, name, additionalNames, 0);
	}

	/**
	 * 
	 * @param m
	 * @param name
	 * @param additionalNames
	 * @param format
	 *            0 = lmf ; 1 = fml
	 * @throws Exception
	 */
	public static void parseName(ResultMap m, String name, List<List>/* String */additionalNames, int format) throws Exception {
		if (StringUtils.isNotEmpty(name)) {
			name = name.replaceAll("&amp;", "&");
			m.put("PropertyIdentificationSet.NameOnServer", name);

			name = name.replaceAll("(\\s)(AND|OR)(\\s)", "$1&$3").replaceAll("/", " & ").replaceFirst("\\s*&$", "")
					.replaceAll("(?is)\\b(ET)\\s+(AL|UX|VIR)\\b", "$1$2");

			List<List/* <String> */> namesList = new ArrayList<List/* <String> */>();
			if (additionalNames != null) {
				namesList.addAll(additionalNames);
			}

			String[] names = { "", "", "", "", "", "" };
			String[] suffixes = { "", "" }, type = { "", "" }, otherType = { "", "" };

			if (NameUtils.isCompany(name, new Vector<String>(), true)) {
				name = name.replaceAll("(?is)\\([^^\\)s]*\\)", "");
				names[2] = name.trim();
			} else {
				name = NameCleaner.cleanNameAndFix(name, new Vector<String>(), true);
				switch (format) {
				case 0:
					names = StringFormats.parseNameNashville(name, true);
					break;
				case 1:
					names = StringFormats.parseNameDesotoRO(name, true);
					break;
				default:
					names = StringFormats.parseNameNashville(name, true);
					break;
				}
				names = NameCleaner.tokenNameAdjustment(names);
				names = NameCleaner.removeUnderscore(names);
				suffixes = GenericFunctions.extractNameSuffixes(names);
				name = NameCleaner.removeUnderscore(name);
			}
			type = GenericFunctions.extractAllNamesType(names);
			otherType = GenericFunctions.extractAllNamesOtherType(names);

			GenericFunctions.addOwnerNames(names, suffixes[0], suffixes[1], type, otherType, NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]),
					namesList);

			GenericFunctions.storeOwnerInPartyNames(m, namesList, true);
			String[] a = new String[6];
			switch (format) {
			case 0:
				a = StringFormats.parseNameNashville(name, true);
				break;
			case 1:
				a = StringFormats.parseNameDesotoRO(name, true);
				break;
			default:
				a = StringFormats.parseNameNashville(name, true);
				break;
			}
			m.put("PropertyIdentificationSet.OwnerFirstName", a[0]);
			m.put("PropertyIdentificationSet.OwnerMiddleName", a[1]);
			m.put("PropertyIdentificationSet.OwnerLastName", a[2]);
			m.put("PropertyIdentificationSet.SpouseFirstName", a[3]);
			m.put("PropertyIdentificationSet.SpouseMiddleName", a[4]);
			m.put("PropertyIdentificationSet.SpouseLastName", a[5]);

		}
	}

	public static void parseName(ResultMap m, String name) throws Exception {
		parseName(m, name, null);
	}

	public static void removeCityAndZip(ResultMap resultMap, long searchId) throws Exception {

		GenericDASLNDBFunctions.removeCityAndZip(resultMap, searchId);
	}

}
