/*
 * Created on Jan 6, 2004
 */
package ro.cst.tsearch.servers.types;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.functions.ForwardingParseClass;
import ro.cst.tsearch.servers.functions.ParseInterface;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servers.response.TaxHistorySet;
import ro.cst.tsearch.servers.response.TaxHistorySet.TaxHistorySetKey;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.XMLDocument;
import ro.cst.tsearch.utils.XMLRecord;

public class TNKnoxTR extends TemplatedServer {

	static final long serialVersionUID = 10000000;

	public static final String TEMPLATES_PATH = ServerConfig.getRealPath() + "WEB-INF" + File.separator + "classes" + File.separator
			+ "resource" + File.separator + "knox" + File.separator + "template" + File.separator;

	public static final String INTERMEDIATE_RESULTS_TEMPLATE_PATH = TEMPLATES_PATH + "intermediateResultsKnox.txt";
	public static final String FINAL_RESULTS_TEMPLATE_PATH = TEMPLATES_PATH + "finalResultsKnox.txt";
	public static final String INTERMEDIATE_RESULTS_TEMPLATE = FileUtils.readFile(INTERMEDIATE_RESULTS_TEMPLATE_PATH);
	public static final String FINAL_RESULTS_TEMPLATE = FileUtils.readFile(FINAL_RESULTS_TEMPLATE_PATH);

	private boolean downloadingForSave;

	protected String getAccountNumber(String serverResult) {
		return "";
	}

	protected void setMessages() {
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {

		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m;

		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();

		String parcelId = sa.getAtribute(SearchAttributes.LD_PARCELNO);
		parcelId = parcelId.replaceAll(" ", "-");
		parcelId = parcelId.replaceAll("([A-Za-z]{2})(\\d)", "$1-$2");
		boolean canSearchByPIN = false;
		if (parcelId.contains("-")){
			canSearchByPIN = true;
		}

		String streetName = sa.getAtribute(SearchAttributes.P_STREETNAME);
		String streetNumber = sa.getAtribute(SearchAttributes.P_STREETNO);

		FilterResponse addressFilter = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d);
		FilterResponse nameFilterHybrid = NameFilterFactory.getHybridNameFilter(SearchAttributes.OWNER_OBJECT, searchId, null);

		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.GENERIC_MODULE_IDX));
		if (hasPin() && canSearchByPIN) {
			m.getFunction(0).setSaKey("");
			m.getFunction(0).setParamValue(parcelId);
			l.add(m);
		} else if (hasStreet() || hasStreetNo()) {
			m.getFunction(0).setSaKey("");
			m.getFunction(0).setParamValue((streetNumber + " " + streetName).trim());
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			l.add(m);
		} else if (hasOwner()) {
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.addFilter(addressFilter);
			m.addFilter(nameFilterHybrid);
			m.getFunction(0).setSaKey("");
			m.setIteratorType(0, FunctionStatesIterator.ITERATOR_TYPE_LF_NAME_FAKE);
			ConfigurableNameIterator nameIterator = (ConfigurableNameIterator) ModuleStatesIteratorFactory.getConfigurableNameIterator(m,
					searchId, new String[] { "L;F;" });
			m.addIterator(nameIterator);
			l.add(m);
		}
		serverInfo.setModulesForAutoSearch(l);
	}

	public TNKnoxTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void ParseResponse(String sAction, ServerResponse serverResponse, int viParseID) throws ServerResponseException {

		String sTmp;
		String keyNumber = "";
		String responseAsHtml = serverResponse.getResult();
		String initialResponse = responseAsHtml;

		responseAsHtml = responseAsHtml.replaceAll("\\|t+", "");

		Hashtable<String, String> replacements = new Hashtable<String, String>();

		int totalResults = 0;

		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(responseAsHtml.getBytes());
			XMLDocument document = new XMLDocument(bais);

			XMLRecord totalRecords = document.getFirst("//tax_lookup//props//@amount");

			try {
				totalResults = Integer.parseInt(totalRecords.getTextContent());
			} catch (NumberFormatException nfe) {
			}

			if (totalResults == 1 && (viParseID == 106)) {
				viParseID = ID_DETAILS;
			}

			switch (viParseID) {
			case 106:
				if (totalResults == 0) {
					return;
				}

				sTmp = CreatePartialLink(TSConnectionURL.idGET);

				String prop_id = "";
				String owner_name = "";
				String address = "";
				String detail_info = "";

				XMLRecord[] results = document.getAll("//tax_lookup//prop_details//prop");

				String newResponse = "";

				for (int i = 0; i < results.length; i++) {
					prop_id = results[i].getText("prop_id");
					owner_name = results[i].getText("owner");
					address = results[i].getText("street");

					detail_info = sTmp + "/apps/tax_search/data/search2.php?search=" + prop_id + "&full=yes";

					replacements.put("prop_id", prop_id);
					replacements.put("owner_name", owner_name);
					replacements.put("address", address);
					replacements.put("detail_info", detail_info);

					newResponse += renderTemplate(INTERMEDIATE_RESULTS_TEMPLATE, replacements);
				}

				responseAsHtml = "<table>" + newResponse + "</table>";
				intermediary(serverResponse, responseAsHtml);
				// parser.Parse(Response.getParsedResponse(), rsResponse,
				// Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET),
				// TSServer.REQUEST_SAVE_TO_TSD);
				break;

			case ID_SEARCH_BY_PARCEL:
			case ID_DETAILS:

				XMLRecord finalResult = document.getFirst("//tax_lookup//prop_details//prop");

				replacements.put("owner", finalResult.getText("owner"));
				replacements.put("addr", finalResult.getText("addr"));
				replacements.put("mailing", finalResult.getText("mailing"));
				replacements.put("appr", finalResult.getText("appr"));
				replacements.put("asses", finalResult.getText("asses"));
				replacements.put("taxRate", finalResult.getText("taxRate"));
				String taxLevy = finalResult.getText("taxLevy");
				if (taxLevy != null)
					taxLevy = taxLevy.replaceAll(",", "");
				if ("".equals(taxLevy)) {
					taxLevy = "0";
				}

				replacements.put("taxLevy", taxLevy);

				replacements.put("prop_id", finalResult.getText("prop_id"));
				String PID = finalResult.getText("prop_id");
				replacements.put("deed_book", finalResult.getText("deed_book"));
				replacements.put("deed_page", finalResult.getText("deed_page"));
				replacements.put("deed_rec_num", finalResult.getText("deed_rec_num"));
				replacements.put("deed_sale_date", finalResult.getText("deed_sale_date"));
				replacements.put("deed_rec_date", finalResult.getText("deed_rec_date"));
				replacements.put("deed_type", finalResult.getText("deed_type"));

				XMLRecord taxPaymentInfo[] = document.getAll("//tax_lookup//prop_details//prop//details//year");

				String taxPaymentRows = "";

				// delinquent for prior years
				BigDecimal priorDelinquent = new BigDecimal(0);

				String currentYear = "";
				BigDecimal baseAmount = new BigDecimal(0);
				BigDecimal amountPaid = new BigDecimal(0);
				BigDecimal dueTotal = new BigDecimal(0);
				String paidDate = "";

				for (int i = 0; i < taxPaymentInfo.length; i++) {
					String year = taxPaymentInfo[i].getText("name");
					String status = taxPaymentInfo[i].getText("staatus");
					String amountDue = taxPaymentInfo[i].getText("net_billed").replaceAll(",", "");
					String datePaid = taxPaymentInfo[i].getText("datepaid");
					String totalPaid = taxPaymentInfo[i].getText("payments").replaceAll(",", "");
					String totalDue = taxPaymentInfo[i].getText("balancedue").replaceAll(",", "");

					if (i == 0) {
						currentYear = year;
						baseAmount = new BigDecimal(amountDue);
						amountPaid = new BigDecimal(totalPaid);
						dueTotal = new BigDecimal(totalDue);
						paidDate = datePaid;
					} else if ("UNPAID".equals(status)) {
						priorDelinquent = priorDelinquent.add(new BigDecimal(totalDue));
					}

					taxPaymentRows += "<tr>" + "<td>" + year + "</td>" + "<td>" + status + "</td>" + "<td>" + amountDue + "</td>" + "<td>" 
					        + datePaid + "</td>" + "<td>" + totalPaid + "</td>" + "<td>" + totalDue + "</td>" + "</tr>";
				}

				replacements.put("tax_payment_info_td", taxPaymentRows);
				String newResult = renderTemplate(FINAL_RESULTS_TEMPLATE, replacements);
				responseAsHtml = newResult;
				keyNumber = PID;

				if (!downloadingForSave) {

					String originalLink = serverResponse.getQuerry();
					String sSave2TSDLink = CreatePartialLink(TSConnectionURL.idGET) + sAction + "&" + originalLink + "&PID=" + PID
							+ "&full=yes";

					if (FileAlreadyExist(keyNumber + ".html")) {
						responseAsHtml += CreateFileAlreadyInTSD();
					} else {
						responseAsHtml = addSaveToTsdButton(responseAsHtml, sSave2TSDLink, viParseID);
						mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
					}
					serverResponse.getParsedResponse().setPageLink(
							new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
					parser.Parse(serverResponse.getParsedResponse(), responseAsHtml, Parser.NO_PARSE);

				} else {
					// for html
					msSaveToTSDFileName = keyNumber + ".html";
					serverResponse.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
					msSaveToTSDResponce = responseAsHtml + CreateFileAlreadyInTSD();
					// reparse the final answer (included all links)

					// set the delinquent data
					ParsedResponse pr = serverResponse.getParsedResponse();

					Vector taxHistorySetVector = (Vector) pr.infVectorSets.get("TaxHistorySet");
					if (taxHistorySetVector == null) {
						taxHistorySetVector = new Vector();
						pr.infVectorSets.put("TaxHistorySet", taxHistorySetVector);
					}
					if (taxHistorySetVector.size() == 0)
						taxHistorySetVector.add(new TaxHistorySet());
					TaxHistorySet taxHistorySet = (TaxHistorySet) taxHistorySetVector.get(0);
					BigDecimal due = baseAmount.subtract(amountPaid);
					taxHistorySet.setAtribute(TaxHistorySetKey.PRIOR_DELINQUENT.getShortKeyName(), priorDelinquent.toString());
					taxHistorySet.setAtribute(TaxHistorySetKey.BASE_AMOUNT.getShortKeyName(), baseAmount.toString());
					// fix for bug #918
					taxHistorySet.setAtribute(TaxHistorySetKey.CURRENT_YEAR_DUE.getShortKeyName(), due.toString());
					taxHistorySet.setAtribute(TaxHistorySetKey.AMOUNT_PAID.getShortKeyName(), amountPaid.toString());
					// fix for bug #1248
					taxHistorySet.setAtribute(TaxHistorySetKey.TOTAL_DUE.getShortKeyName(), dueTotal.toString());
					taxHistorySet.setAtribute(TaxHistorySetKey.YEAR.getShortKeyName(), currentYear);
					
					if (amountPaid.compareTo(BigDecimal.ZERO)>0){
						taxHistorySet.setAtribute(TaxHistorySetKey.DATE_PAID.getShortKeyName(), paidDate);
					}
					
					saveCasesParse(serverResponse, responseAsHtml, msSaveToTSDFileName);
				}
				break;

			case ID_SAVE_TO_TSD:
			case ID_GET_LINK:

				if (sAction.startsWith("/trustee/tax_search/owner.php"))
					ParseResponse(sAction, serverResponse, ID_SEARCH_BY_NAME);
				else if (viParseID == ID_GET_LINK)
					ParseResponse(sAction, serverResponse, ID_DETAILS);
				else {
					downloadingForSave = true;
					ParseResponse(sAction, serverResponse, ID_DETAILS);
					downloadingForSave = false;
				}
				break;

			default:
				break;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}

	@Override
	protected Object parseAndFillResultMap(ServerResponse response, String detailsHtml, ResultMap map) {
		getParserInstance().parseDetails(detailsHtml, searchId, map);
		return null;
	}

	@Override
	public Collection<ParsedResponse> smartParseIntermediary(ServerResponse response, String table, StringBuilder outputTable) {
		MessageFormat format = createPartialLinkFormat();
		Vector<ParsedResponse> parseIntermediary = getParserInstance().parseIntermediary(response, table, searchId, format);
		return parseIntermediary;
	}

	private ForwardingParseClass getParserInstance() {
		ParseInterface instance = ro.cst.tsearch.servers.functions.TNKnoxTR.getInstance();
		ForwardingParseClass forwardingParseClass = new ForwardingParseClass(instance);
		return forwardingParseClass;
	}

	@Override
	protected String getFileNameFromLink(String url) {
		String parcelId = StringUtils.getTextBetweenDelimiters("PID=", "&", url).trim();
		return parcelId + ".html";
	}

	private String renderTemplate(String template, Hashtable<String, String> replacements) {
		Iterator<String> keyIterator = replacements.keySet().iterator();

		while (keyIterator.hasNext()) {
			String keyName = (String) keyIterator.next();
			String value = replacements.get(keyName);

			template = template.replaceAll("@@" + keyName + "@@", value);
		}

		return template;
	}

}