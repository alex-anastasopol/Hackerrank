package ro.cst.tsearch.settings;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.data.CountyCommunityManager;
import ro.cst.tsearch.data.DueDate;
import ro.cst.tsearch.data.GenericCounty;
import ro.cst.tsearch.data.GenericState;
import ro.cst.tsearch.data.Payrate;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.templates.AddDocsTemplates;
import ro.cst.tsearch.templates.Template;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.MultipartParameterParser;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;

public class AdminSettingsServlet extends HttpServlet {

	/**
	 * Generated serial UID
	 */
	private static final long serialVersionUID = 1L;
	private static final Category logger = Logger.getLogger(DBManager.class.getName());
	public static final String TIMESTAMP_EXPORT = "dd-MMM-yyyy HH:mm:ss";

	private class CountyNotSelected extends RuntimeException {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		CountyNotSelected(String e) {
			super(e);
		}
	}

	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		HttpSession session = request.getSession(true);
		User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		Search global;
		String operation = "";
		String idList = "";
		int yearReport, monthReport, dayReport, checkbox = 0, ii = 0, i = 0, j;
		long searchId;
		long stateId, ids[] = {}, commId = 0;
		try {
			commId = Integer.parseInt(session.getAttribute("commId") + "");
		} catch (Exception e) {
			e.printStackTrace();
		}
		double fullSearchPrice, updateSearchPrice, currentOwnerPrice, refinancePrice, fvsPrice;
		double constructionPrice, commercialPrice, oePrice, liensPrice, acreagePrice, sublotPrice;
		double indexPrice;
		boolean overwriteOCRLegal = "on".equalsIgnoreCase(request.getParameter("overwriteOCRLegal"));
		boolean overwriteOCRLegalCondo = "on".equalsIgnoreCase(request.getParameter("overwriteOCRLegalCondo"));
		int intOpCode = -1;

		try {
			intOpCode = Integer.parseInt(request.getParameter(TSOpCode.OPCODE));
			if (intOpCode == TSOpCode.SET_DEFAULT_LEGAL_DESCRIPTION) {
				String countyIds = request.getParameter(RequestParams.SETTINGS_COUNTY_IDS);
				String defaultLd = request.getParameter(RequestParams.SETTINGS_DEFAULT_LD);
				defaultLd = StringEscapeUtils.unescapeHtml(StringEscapeUtils.unescapeHtml(defaultLd)); // Task 8720 
				List<Long> templateIds = new ArrayList<Long>();
				List<String> defaultCondoLds = new ArrayList<String>();
				int nr = DBManager.setDefaultLdTemplates(countyIds, defaultLd, overwriteOCRLegal, (int) commId, templateIds, defaultCondoLds);

				for (int k=0;k<nr;k++) {
					Template template = new Template((long)(templateIds.get(k)), AddDocsTemplates.LEGAL_TEMPLATE_NAME, AddDocsTemplates.LEGAL_TEMPLATE_NAME,
							Long.toString(new Date().getTime()), AddDocsTemplates.LEGAL_TEMPLATE_NAME, defaultLd,
							AddDocsTemplates.LEGAL_TEMPLATE_NAME, AddDocsTemplates.LEGAL_COMMID);
					template.compile();
					Template template_condo = new Template((long)(templateIds.get(k)), AddDocsTemplates.LEGAL_CONDO_TEMPLATE_NAME, 
							AddDocsTemplates.LEGAL_CONDO_TEMPLATE_NAME,	Long.toString(new Date().getTime()), 
							AddDocsTemplates.LEGAL_CONDO_TEMPLATE_NAME, defaultCondoLds.get(k),
							AddDocsTemplates.LEGAL_CONDO_TEMPLATE_NAME, AddDocsTemplates.LEGAL_COMMID);
					template_condo.compile();
				}
				
				request.setAttribute("close", Boolean.TRUE);
				request.getRequestDispatcher(URLMaping.SET_DEFAULT_LD).forward(request, response);

				return;
			} else if (intOpCode == TSOpCode.SET_DEFAULT_LEGAL_DESCRIPTION_CONDO) {
				String countyIds = request.getParameter(RequestParams.SETTINGS_COUNTY_IDS);
				String defaultLdCondo = request.getParameter(RequestParams.SETTINGS_DEFAULT_LD_CONDO);
				defaultLdCondo = StringEscapeUtils.unescapeHtml(StringEscapeUtils.unescapeHtml(defaultLdCondo)); // Task 8720 
				List<Long> templateIds = new ArrayList<Long>();
				List<String> defaultLds = new ArrayList<String>();
				int nr = DBManager.setDefaultLdTemplatesCondo(countyIds, defaultLdCondo, overwriteOCRLegalCondo, (int) commId, templateIds, defaultLds);
				
				for (int k=0;k<nr;k++) {
					Template template = new Template((long)(templateIds.get(k)), AddDocsTemplates.LEGAL_TEMPLATE_NAME, AddDocsTemplates.LEGAL_TEMPLATE_NAME,
							Long.toString(new Date().getTime()), AddDocsTemplates.LEGAL_TEMPLATE_NAME, defaultLds.get(k),
							AddDocsTemplates.LEGAL_TEMPLATE_NAME, AddDocsTemplates.LEGAL_COMMID);
					template.compile();
					Template template_condo = new Template((long)(templateIds.get(k)), AddDocsTemplates.LEGAL_CONDO_TEMPLATE_NAME, 
							AddDocsTemplates.LEGAL_CONDO_TEMPLATE_NAME,	Long.toString(new Date().getTime()), 
							AddDocsTemplates.LEGAL_CONDO_TEMPLATE_NAME, defaultLdCondo,
							AddDocsTemplates.LEGAL_CONDO_TEMPLATE_NAME, AddDocsTemplates.LEGAL_COMMID);
					template_condo.compile();
				}
				
				request.setAttribute("close", Boolean.TRUE);
				request.getRequestDispatcher(URLMaping.SET_DEFAULT_LD).forward(request, response);

				return;
			}else if (intOpCode == TSOpCode.SET_CERTIFICATION_DATE_OFFSET) {
				String countyIds = request.getParameter(RequestParams.SETTINGS_COUNTY_IDS);
				if (StringUtils.isEmpty(countyIds)) {
					throw new CountyNotSelected("Before modifying Certification date, please select the desired counties");
				}
				String offset = request.getParameter(RequestParams.SETTINGS_CERTIFICATION_DATE_OFFSET);
				if ("".equals(offset)){
					offset = null;
				}
				String search_id = request.getParameter(RequestParams.SEARCH_ID);
				DBManager.setDefaultCertificationDateOffset(countyIds, offset,
						Integer.parseInt(request.getParameter(RequestParams.COMM_ID)));
				request.getRequestDispatcher(
						URLMaping.SETTINGS_PAGE + "?" + RequestParams.SEARCH_ID + "=" + search_id + "&" + TSOpCode.OPCODE + "="
								+ TSOpCode.SETTINGS_VIEW).forward(request, response);
				CountyCommunityManager.getInstance().reloadCounties();
				return;
			} else if (intOpCode == TSOpCode.SET_OFFICIAL_START_DATE_OFFSET) {
				String countyIds = request.getParameter(RequestParams.SETTINGS_COUNTY_IDS);
				if (StringUtils.isEmpty(countyIds)) {
					throw new CountyNotSelected("Before modifying Official Start Date, please select the desired counties");
				}
				String offset = request.getParameter(RequestParams.SETTINGS_OFFICIAL_START_DATE_OFFSET);
				String search_id = request.getParameter(RequestParams.SEARCH_ID);
				DBManager.setDefaultOfficialStartDateOffset(countyIds, offset,
						Integer.parseInt(request.getParameter(RequestParams.COMM_ID)));
				request.getRequestDispatcher(
						URLMaping.SETTINGS_PAGE + "?" + RequestParams.SEARCH_ID + "=" + search_id + "&" + TSOpCode.OPCODE + "="
								+ TSOpCode.SETTINGS_VIEW).forward(request, response);
				CountyCommunityManager.getInstance().reloadCounties();
				return;
			}
		} catch (NumberFormatException nfe) {
		} catch (CountyNotSelected cns) {
			logger.error("Some error here", cns);
			throw cns;
		} catch (Exception e) {
			logger.error("Some error here", e);
		}

		MultipartParameterParser mpp = null;
		String contentType = request.getContentType();
		boolean isMultipart = contentType != null && contentType.indexOf("multipart/form-data") > -1;
		if (isMultipart) {
			mpp = new MultipartParameterParser(request);
			stateId = 1; // some default, it will not be used if we have
							// multipart
			operation = mpp.getMultipartStringParameter(RequestParams.SETTINGS_OPERATION);
			
			String dateFromNew = request.getParameter(RequestParams.DATE_FROM_NEW);
			if (StringUtils.isNotEmpty(dateFromNew) && 
					RegExUtils.matches("\\d{1,2}/\\d{1,2}/\\d{4}", dateFromNew)) {
				String[] split = dateFromNew.split("/");
				dayReport = Integer.parseInt(StringUtils.removeLeadingZeroes(split[1]));
				monthReport = Integer.parseInt(StringUtils.removeLeadingZeroes(split[0]));
				yearReport = Integer.parseInt(StringUtils.removeLeadingZeroes(split[2]));
			} else {
				dayReport = mpp.getMultipartIntParameter("dayReport");
                monthReport = mpp.getMultipartIntParameter("monthReport");
                yearReport = mpp.getMultipartIntParameter("yearReport");
			}
			
			global = currentUser.getSearch(mpp.getMultipartLongParameter(RequestParams.SEARCH_ID));

		} else {
			global = (Search) currentUser.getSearch(request);
			// reading request params
			operation = request.getParameter(RequestParams.SETTINGS_OPERATION);
			idList = request.getParameter(RequestParams.INVOICE_LIST_CHK);
			stateId = Long.parseLong(request.getParameter(RequestParams.REPORTS_STATE));
			// long cityID = Long.parseLong( request.getParameter(
			// RequestParams.REPORTS_CITY ));
			if (logger.isDebugEnabled())
				logger.debug("AdminSettingsServlet parameters: state-" + stateId + " / Ids:" + idList);
			String dateFromNew = request.getParameter(RequestParams.DATE_FROM_NEW);
			if (StringUtils.isNotEmpty(dateFromNew) && 
					RegExUtils.matches("\\d{1,2}/\\d{1,2}/\\d{4}", dateFromNew)) {
				String[] split = dateFromNew.split("/");
				dayReport = Integer.parseInt(StringUtils.removeLeadingZeroes(split[1]));
				monthReport = Integer.parseInt(StringUtils.removeLeadingZeroes(split[0]));
				yearReport = Integer.parseInt(StringUtils.removeLeadingZeroes(split[2]));
			} else {
				dayReport = Integer.parseInt(request.getParameter("dayreport"));
				monthReport = Integer.parseInt(request.getParameter("monthReport"));
				yearReport = Integer.parseInt(request.getParameter("yearReport"));
			}
		}

		searchId = global.getSearchID();

		if (logger.isDebugEnabled())
			logger.debug("AdminSettingsServlet parameters: " + operation + " / Date:" + dayReport + "-" + monthReport + "-" + yearReport);
		// extracting the payrates ids from request string
		StringTokenizer st = new StringTokenizer(idList, ",");
		ii = st.countTokens();
		ids = new long[ii];
		while (st.hasMoreTokens()) {
			ids[i++] = Long.parseLong(st.nextToken().trim());
		}
		// computing the Date of the interval start
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DAY_OF_MONTH, dayReport);
		cal.set(Calendar.MONTH, monthReport - 1);
		cal.set(Calendar.YEAR, yearReport);
		if (operation.equals(RequestParamsValues.SETTINGS_OP_DUE_DATE)) {
			cal.set(Calendar.HOUR_OF_DAY, cal.getActualMaximum(Calendar.HOUR_OF_DAY));
			cal.set(Calendar.MINUTE, cal.getActualMaximum(Calendar.MINUTE));
			cal.set(Calendar.SECOND, cal.getActualMaximum(Calendar.SECOND));
		} else {
			cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
			cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
			cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
		}

		// getting the current payrates values
		commId = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity().getID().longValue();

		Payrate[] currentPayrate = new Payrate[0];
		if (!isMultipart)
			currentPayrate = DBManager.getCurrentPayratesForCommunityAndState(commId, stateId, cal.getTime());
		int payratesLength = currentPayrate.length;
		// setting modified fields in current payrates and saving changes
		boolean found = false;
		Payrate tempPayrate = new Payrate();
		String destinationAddress = URLMaping.SETTINGS_PAGE;
		if (!operation.equals("")) {
			if (operation.equals(RequestParamsValues.SETTINGS_OP_DUE_DATE)) {
				for (i = 0; i < ii; i++)
					DBManager.updateDueDateForCounty(commId, ids[i], cal.getTime());
			} else if (operation.equals(RequestParamsValues.SETTINGS_OP_EXPORT_TO_COMM)) {
				DBManager.exportPayrates((int) commId, Integer.parseInt(mpp.getMultipartStringParameter(RequestParams.REPORTS_COMMUNITY)),
						cal, currentUser.getUserAttributes());
				request.setAttribute("monthReport", monthReport + "");
				request.setAttribute("dayReport", dayReport + "");
				request.setAttribute("yearReport", yearReport + "");
				destinationAddress = URLMaping.SETTINGS_RATES_PAGE;
			} else if (operation.equals(RequestParamsValues.SETTINGS_OP_EXPORT_TO_FILE)) {
				Vector<Payrate> payrates = DBManager.getLatestPayratesForCommunity(commId, cal.getTime());
				// this will maintain corespondance between countyId and
				// position in the next two vectors
				HashMap<Long, Integer> countiesPosition = null;
				Vector<DueDate> countyDates = new Vector<DueDate>();
				Vector<DueDate> cityDates = new Vector<DueDate>();

				// countiesPosition = DBManager.getDueDatesForCommunity(commId,
				// countyDates, cityDates);

				String expFileName = "RATES_" + commId + "_" + new SimpleDateFormat(FormatDate.PATTERN_MMddyyyy_HHmmss).format(new Date());
				String fileFormat = "csv";
				String rootPath = BaseServlet.FILES_PATH + File.separator + "tempZipFolder" + File.separator;
				StringBuffer sb = writeToCSV(payrates, countiesPosition, countyDates, cityDates, session);

				PrintWriter pw = null;
				try {
					pw = new PrintWriter(rootPath + expFileName + "." + fileFormat);
					pw.print(sb.toString());
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						pw.close();
					} catch (Exception e) {
					}
				}

				destinationAddress = URLMaping.DownloadFileAs + "?" + "pdfFile=" + expFileName + "." + fileFormat;

			} else if (operation.equals(RequestParamsValues.SETTINGS_OP_IMPORT_FILE)) {
				destinationAddress = URLMaping.SETTINGS_RATES_PAGE;
				File importFile = mpp.getFileParameter(RequestParams.SETTINGS_IMPORT_FILE_NAME);
				HashMap<Long, Payrate> payrates = readFromCSV(importFile);
				DBManager.exportPayrates(payrates, commId, cal, currentUser.getUserAttributes());
				DBManager.importDueDates(payrates, commId);
				request.setAttribute("monthReport", monthReport + "");
				request.setAttribute("dayReport", dayReport + "");
				request.setAttribute("yearReport", yearReport + "");
			} else if (operation.equals(RequestParamsValues.SETTINGS_OP_CITY_DUE_DATE)) {
				for (i = 0; i < ii; i++)
					DBManager.updateCityDueDateForCounty(commId, ids[i], cal.getTime());
			} else if (operation.equals(RequestParamsValues.SETTINGS_OP_COMMADMIN)) {
				fullSearchPrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_TSC_FULLSEARCH));
				updateSearchPrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_TSC_UPDATESEARCH));
				currentOwnerPrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_TSC_CURRENTOWNER));
				refinancePrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_TSC_REFINANCE));
				constructionPrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_TSC_CONSTRUCTION));
				commercialPrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_TSC_COMMERCIAL));
				oePrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_TSC_OE));
				liensPrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_TSC_LIENS));
				acreagePrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_TSC_ACREAGE));
				sublotPrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_TSC_SUBLOT));
				indexPrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_C2A_INDEX));
				fvsPrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_TSC_FVS));

				for (i = 0; i < ii; i++) {
					found = false;
					for (j = 0; j < payratesLength; j++)
						if (ids[i] == currentPayrate[j].getCountyId() && currentPayrate[j].getId() > 0) {
							found = true;
							if (fullSearchPrice != currentPayrate[j].getSearchValue()
									|| updateSearchPrice != currentPayrate[j].getUpdateValue()
									|| currentOwnerPrice != currentPayrate[j].getCurrentOwnerCost()
									|| refinancePrice != currentPayrate[j].getRefinanceCost()
									|| constructionPrice != currentPayrate[j].getConstructionValue()
									|| commercialPrice != currentPayrate[j].getCommercialValue()
									|| oePrice != currentPayrate[j].getOEValue() || liensPrice != currentPayrate[j].getLiensValue()
									|| acreagePrice != currentPayrate[j].getAcreageValue()
									|| sublotPrice != currentPayrate[j].getSublotValue() || indexPrice != currentPayrate[j].getIndexC2A()
									|| fvsPrice != currentPayrate[j].getFvsValue()) {
								currentPayrate[j].setStartDate(cal.getTime());
								currentPayrate[j].setSearchValue(fullSearchPrice);
								currentPayrate[j].setUpdateValue(updateSearchPrice);
								currentPayrate[j].setCurrentOwnerValue(currentOwnerPrice);
								currentPayrate[j].setRefinanceValue(refinancePrice);
								currentPayrate[j].setConstructionValue(constructionPrice);
								currentPayrate[j].setCommercialValue(commercialPrice);
								currentPayrate[j].setOEValue(oePrice);
								currentPayrate[j].setLiensValue(liensPrice);
								currentPayrate[j].setAcreageValue(acreagePrice);
								currentPayrate[j].setSublotValue(sublotPrice);
								currentPayrate[j].setIndexC2A(indexPrice);
								currentPayrate[j].setFvsValue(fvsPrice);
								DBManager.insertPayrate(currentPayrate[j], currentUser.getUserAttributes());
								break;
							}
						}
					if (!found) {
						tempPayrate = new Payrate();
						tempPayrate.setStartDate(cal.getTime());
						tempPayrate.setSearchValue(fullSearchPrice);
						tempPayrate.setUpdateValue(updateSearchPrice);
						tempPayrate.setCurrentOwnerValue(currentOwnerPrice);
						tempPayrate.setRefinanceValue(refinancePrice);
						tempPayrate.setConstructionValue(constructionPrice);
						tempPayrate.setCommercialValue(commercialPrice);
						tempPayrate.setOEValue(oePrice);
						tempPayrate.setLiensValue(liensPrice);
						tempPayrate.setAcreageValue(acreagePrice);
						tempPayrate.setSublotValue(sublotPrice);
						tempPayrate.setIndexC2A(indexPrice);
						tempPayrate.setCountyId(ids[i]);
						tempPayrate.setCommId(commId);
						tempPayrate.setFvsValue(fvsPrice);
						DBManager.insertPayrate(tempPayrate, currentUser.getUserAttributes());
					}
				}
				checkbox = Integer.parseInt(request.getParameter(RequestParams.SETTINGS_COMMITMENT_DOC));
				if (logger.isDebugEnabled())
					logger.debug("AdminSettingsServlet parameters for commadmin: " + checkbox + " / Prices:" + fullSearchPrice + "-"
							+ updateSearchPrice + "-" + yearReport);
				InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity().setCOMMITMENT(new BigDecimal(checkbox));
			} else if (operation.equals(RequestParamsValues.SETTINGS_OP_TSADMIN)) {
				fullSearchPrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_CST_FULLSEARCH));
				updateSearchPrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_CST_UPDATESEARCH));
				currentOwnerPrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_CST_CURRENTOWNER));
				refinancePrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_CST_REFINANCE));
				constructionPrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_CST_CONSTRUCTION));
				commercialPrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_CST_COMMERCIAL));
				oePrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_CST_OE));
				liensPrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_CST_LIENS));
				acreagePrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_CST_ACREAGE));
				sublotPrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_CST_SUBLOT));
				indexPrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_A2C_INDEX));
				fvsPrice = Double.parseDouble(request.getParameter(RequestParams.SETTINGS_CST_FVS));

				boolean changesAdded = false;
				String changesTables = "\n";

				for (i = 0; i < ii; i++) {
					found = false;
					for (j = 0; j < payratesLength; j++)
						if (ids[i] == currentPayrate[j].getCountyId() && currentPayrate[j].getId() > 0) {
							found = true;
							if (fullSearchPrice != currentPayrate[j].getSearchCost()
									|| updateSearchPrice != currentPayrate[j].getUpdateCost()
									|| currentOwnerPrice != currentPayrate[j].getCurrentOwnerCost()
									|| refinancePrice != currentPayrate[j].getRefinanceCost()
									|| constructionPrice != currentPayrate[j].getConstructionCost()
									|| commercialPrice != currentPayrate[j].getCommercialCost() || oePrice != currentPayrate[j].getOECost()
									|| liensPrice != currentPayrate[j].getLiensCost() || acreagePrice != currentPayrate[j].getAcreageCost()
									|| sublotPrice != currentPayrate[j].getSublotCost() || indexPrice != currentPayrate[j].getIndexA2C()
									|| fvsPrice != currentPayrate[j].getFvsCost()) {
								currentPayrate[j].setStartDate(cal.getTime());
								currentPayrate[j].setSearchCost(fullSearchPrice);
								currentPayrate[j].setUpdateCost(updateSearchPrice);
								currentPayrate[j].setCurrentOwnerCost(currentOwnerPrice);
								currentPayrate[j].setRefinanceCost(refinancePrice);
								currentPayrate[j].setConstructionCost(constructionPrice);
								currentPayrate[j].setCommercialCost(commercialPrice);
								currentPayrate[j].setOECost(oePrice);
								currentPayrate[j].setLiensCost(liensPrice);
								currentPayrate[j].setAcreageCost(acreagePrice);
								currentPayrate[j].setSublotCost(sublotPrice);
								currentPayrate[j].setIndexA2C(indexPrice);
								currentPayrate[j].setFvsCost(fvsPrice);
								DBManager.insertPayrate(currentPayrate[j], currentUser.getUserAttributes());
								if (!changesAdded) {
									changesAdded = true;
									changesTables += "\nTime of update: " + SearchLogger.getCurDateTimeCST() + " by "
											+ currentUser.getUserAttributes().getLOGIN() + " from "
											+ currentUser.getUserAttributes().getUserLoginIp() + " on server " + URLMaping.INSTANCE_DIR
											+ ", Community "
											+ InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity().getNAME() // Community
																																		// ID
											+ "\n";
									String cntyName = "", stateAbv = "";
									try {
										cntyName = County.getCounty(new BigDecimal(currentPayrate[j].getCountyId())).getName();
										stateAbv = County.getCounty(new BigDecimal(currentPayrate[j].getCountyId())).getState()
												.getStateAbv();
									} catch (Exception e) {
										e.printStackTrace();
									}

									changesTables += currentPayrate[j].getPayrateContent(true) + "\n";
									changesTables += "FOR\n\n";// un tabel;
																// County, State
																// - alt tabel
									changesTables += "STATE | COUNTY\n";
									changesTables += stateAbv + " | " + cntyName + "\n";

								} else {
									String cntyName = "", stateAbv = "";
									try {
										cntyName = County.getCounty(new BigDecimal(currentPayrate[j].getCountyId())).getName();
										stateAbv = County.getCounty(new BigDecimal(currentPayrate[j].getCountyId())).getState()
												.getStateAbv();
									} catch (Exception e) {
										e.printStackTrace();
									}

									changesTables += stateAbv + " | " + cntyName + "\n";
								}
								break;
							}
						}
					if (!found) {
						tempPayrate = new Payrate();
						tempPayrate.setStartDate(cal.getTime());
						tempPayrate.setSearchCost(fullSearchPrice);
						tempPayrate.setUpdateCost(updateSearchPrice);
						tempPayrate.setCurrentOwnerCost(currentOwnerPrice);
						tempPayrate.setRefinanceCost(refinancePrice);
						tempPayrate.setConstructionCost(constructionPrice);
						tempPayrate.setCommercialCost(commercialPrice);
						tempPayrate.setOECost(oePrice);
						tempPayrate.setLiensCost(liensPrice);
						tempPayrate.setAcreageCost(acreagePrice);
						tempPayrate.setSublotCost(sublotPrice);
						tempPayrate.setIndexA2C(indexPrice);
						tempPayrate.setCountyId(ids[i]);
						tempPayrate.setCommId(commId);
						tempPayrate.setFvsCost(fvsPrice);
						DBManager.insertPayrate(tempPayrate, currentUser.getUserAttributes());
						if (!changesAdded) {
							changesAdded = true;
							changesTables += "\nTime of update: " + SearchLogger.getCurDateTimeCST() + " by "
									+ currentUser.getUserAttributes().getLOGIN() + " from "
									+ currentUser.getUserAttributes().getUserLoginIp() + " on server " + URLMaping.INSTANCE_DIR
									+ ", Community "
									+ InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCommunity().getNAME() // Community
																																// ID
									+ "\n";

							String cntyName = "", stateAbv = "";
							try {
								cntyName = County.getCounty(new BigDecimal(tempPayrate.getCountyId())).getName();
								stateAbv = County.getCounty(new BigDecimal(tempPayrate.getCountyId())).getState().getStateAbv();
							} catch (Exception e) {
								e.printStackTrace();
							}

							changesTables += tempPayrate.toString() + "\n";
							changesTables += "FOR\n\n";// un tabel; County,
														// State - alt tabel
							changesTables += "STATE | COUNTY\n";
							changesTables += stateAbv + " | " + cntyName + "\n";
						} else {
							String cntyName = "", stateAbv = "";
							try {
								cntyName = County.getCounty(new BigDecimal(tempPayrate.getCountyId())).getName();
								stateAbv = County.getCounty(new BigDecimal(tempPayrate.getCountyId())).getState().getStateAbv();
							} catch (Exception e) {
								e.printStackTrace();
							}

							changesTables += stateAbv + " | " + cntyName + "\n";
						}
					}
				}

				try {
					if (changesAdded) {
						EmailClient email = new EmailClient();
						email.addTo(DBManager.getConfigByName("ATSSettings.payrates.email"));
						email.setSubject("Changes of Payrates on " + URLMaping.INSTANCE_DIR);
						email.addContent(changesTables);
						email.sendNow();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (logger.isDebugEnabled())
					logger.debug("AdminSettingsServlet parameters for commadmin: " + checkbox + " / Prices:" + fullSearchPrice + "-"
							+ updateSearchPrice + "-" + yearReport);
				try {
					Settings.manipulateAttribute(Settings.SETTINGS_MODULE, Settings.INVOICE_DETAILS,
							new Integer(request.getParameter(RequestParams.SETTINGS_INV_DETAILS)).intValue(), InstanceManager.getManager()
									.getCurrentInstance(searchId).getCurrentUser().getID());
				} catch (Exception e) {
					e.printStackTrace();
					logger.error("AdminSettingsServlet exception: " + e.getMessage());
				}
			}
		}
		request.setAttribute(RequestParams.SEARCH_ID, searchId + "");
		// forwarding request back to the jsp page
		request.getRequestDispatcher(destinationAddress).forward(request, response);
	}

	/**
	 * Reads from a CSV file payrate data, and returns a hashmap with payrates
	 * read from the file
	 * 
	 * @param importFile
	 * @return
	 */
	private HashMap<Long, Payrate> readFromCSV(File importFile) {
		HashMap<Long, Payrate> payrates = new HashMap<Long, Payrate>();
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(importFile, "r");
			String line = null;
			String[] strArray = null;
			line = raf.readLine(); // reading table header
			while ((line = raf.readLine()) != null) {
				strArray = line.split(",");
				Payrate payr = new Payrate();
				payr.setCommId(Long.parseLong(strArray[2].replace("\"", "")));
				payr.setCountyId(Long.parseLong(strArray[3].replace("\"", "")));

				payr.setSearchCost(Double.parseDouble(strArray[6].replace("\"", "")));
				payr.setSearchValue(Double.parseDouble(strArray[7].replace("\"", "")));
				payr.setUpdateCost(Double.parseDouble(strArray[8].replace("\"", "")));
				payr.setUpdateValue(Double.parseDouble(strArray[9].replace("\"", "")));
				payr.setCurrentOwnerCost(Double.parseDouble(strArray[10].replace("\"", "")));
				payr.setCurrentOwnerValue(Double.parseDouble(strArray[11].replace("\"", "")));
				payr.setRefinanceCost(Double.parseDouble(strArray[12].replace("\"", "")));
				payr.setRefinanceValue(Double.parseDouble(strArray[13].replace("\"", "")));
				payr.setConstructionValue(Double.parseDouble(strArray[14].replace("\"", "")));
				payr.setConstructionCost(Double.parseDouble(strArray[15].replace("\"", "")));
				payr.setCommercialValue(Double.parseDouble(strArray[16].replace("\"", "")));
				payr.setCommercialCost(Double.parseDouble(strArray[17].replace("\"", "")));
				payr.setOEValue(Double.parseDouble(strArray[18].replace("\"", "")));
				payr.setOECost(Double.parseDouble(strArray[19].replace("\"", "")));
				payr.setLiensValue(Double.parseDouble(strArray[20].replace("\"", "")));
				payr.setLiensCost(Double.parseDouble(strArray[21].replace("\"", "")));
				payr.setAcreageValue(Double.parseDouble(strArray[22].replace("\"", "")));
				payr.setAcreageCost(Double.parseDouble(strArray[23].replace("\"", "")));
				payr.setSublotValue(Double.parseDouble(strArray[24].replace("\"", "")));
				payr.setSublotCost(Double.parseDouble(strArray[25].replace("\"", "")));

				payr.setDueDate(FormatDate.getDateFromFormatedString(strArray[27].replace("\"", ""), TIMESTAMP_EXPORT));
				payr.setCityDueDate(FormatDate.getDateFromFormatedString(strArray[26].replace("\"", ""), TIMESTAMP_EXPORT));
				if (strArray.length > 29) {
					payr.setIndexA2C(Double.parseDouble(strArray[28].replace("\"", "")));
					payr.setIndexC2A(Double.parseDouble(strArray[29].replace("\"", "")));
				}
				if (strArray.length > 31){
					payr.setFvsValue(Double.parseDouble(strArray[30].replace("\"", "")));
					payr.setFvsCost(Double.parseDouble(strArray[31].replace("\"", "")));
				}
				payrates.put(payr.getCountyId(), payr);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return payrates;
	}

	/**
	 * Returns a StringBuffer containing the content of a file to be written (if
	 * you modify this, you must modify readFromCSV method also)
	 * 
	 * @param payrates
	 * @param cityDates
	 * @param countyDates
	 * @param countiesPosition
	 * @param ses
	 * @return
	 */
	private StringBuffer writeToCSV(Vector<Payrate> payrates, HashMap<Long, Integer> countiesPosition, Vector<DueDate> countyDates,
			Vector<DueDate> cityDates, HttpSession ses) {
		HashMap<Long, String> communities = new HashMap<Long, String>();
		try {
			CommunityAttributes[] allCommunities = CommunityUtils.getAllCommunities(ses);
			for (int i = 0; i < allCommunities.length; i++) {
				communities.put(allCommunities[i].getID().longValue(), allCommunities[i].getNAME());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		GenericCounty[] counties = DBManager.getAllCounties();
		HashMap<Long, GenericCounty> countiesHM = new HashMap<Long, GenericCounty>();
		for (int i = 0; i < counties.length; i++) {
			countiesHM.put(counties[i].getId(), counties[i]);
		}
		GenericState[] states = DBManager.getAllStates();
		HashMap<Long, GenericState> statesHM = new HashMap<Long, GenericState>();
		for (int i = 0; i < states.length; i++) {
			statesHM.put(states[i].getId(), states[i]);
		}
		StringBuffer sb = new StringBuffer("");
		sb.append("\"");
		sb.append("ID");
		sb.append("\",\"");
		sb.append("Comm Name");
		sb.append("\",\"");
		sb.append("Comm Id");
		sb.append("\",\"");
		sb.append("County Id");
		sb.append("\",\"");
		sb.append("County Name");
		sb.append("\",\"");
		sb.append("State Name");
		sb.append("\",\"");
		sb.append("Search Cost");
		sb.append("\",\"");
		sb.append("Search Value");
		sb.append("\",\"");
		sb.append("Update Cost");
		sb.append("\",\"");
		sb.append("Update Value");
		sb.append("\",\"");
		sb.append("Current Owner Cost");
		sb.append("\",\"");
		sb.append("Current Owner Value");
		sb.append("\",\"");
		sb.append("Refinance Cost");
		sb.append("\",\"");
		sb.append("Refinance Value");
		sb.append("\",\"");
		sb.append("Construction Value");
		sb.append("\",\"");
		sb.append("Construction Cost");
		sb.append("\",\"");
		sb.append("Commercial Value");
		sb.append("\",\"");
		sb.append("Commercial Cost");
		sb.append("\",\"");
		sb.append("OE Value");
		sb.append("\",\"");
		sb.append("OE Cost");
		sb.append("\",\"");
		sb.append("Liens Value");
		sb.append("\",\"");
		sb.append("Liens Cost");
		sb.append("\",\"");
		sb.append("Acreage Value");
		sb.append("\",\"");
		sb.append("Acreage Cost");
		sb.append("\",\"");
		sb.append("Sublot Value");
		sb.append("\",\"");
		sb.append("Sublot Cost");
		sb.append("\",\"");
		sb.append("County Due Date");
		sb.append("\",\"");
		sb.append("City Due Date");
		sb.append("\",\"");
		sb.append("Index C2A");
		sb.append("\",\"");
		sb.append("Index A2C");
		sb.append("\",\"");
		sb.append("FVS Cost");
		sb.append("\",\"");
		sb.append("FVS Value");
		sb.append("\"");
		sb.append(System.getProperty("line.separator"));
		FormatDate formatDate = new FormatDate(TIMESTAMP_EXPORT);
		for (Payrate payr : payrates) {
			sb.append("\"");
			sb.append(payr.getId());
			sb.append("\",\"");
			String commName = communities.get(payr.getCommId());
			if (commName == null)
				commName = "N/A";
			sb.append(commName);
			sb.append("\",\"");
			sb.append(payr.getCommId());
			sb.append("\",\"");
			sb.append(payr.getCountyId());
			GenericCounty county = countiesHM.get(payr.getCountyId());
			String countyName = null;
			String stateName = null;
			if (county == null) {
				countyName = "N/A";
				stateName = "N/A";
			} else {
				countyName = county.getName();
				GenericState state = statesHM.get(county.getStateId());
				if (state == null)
					stateName = "N/A";
				else
					stateName = state.getName();
			}
			countyName = countyName.replaceAll("&nbsp;", " ");
			stateName = stateName.replaceAll("&nbsp;", " ");
			sb.append("\",\"");
			sb.append(countyName);
			sb.append("\",\"");
			sb.append(stateName);
			sb.append("\",\"");
			sb.append(payr.getSearchCost());
			sb.append("\",\"");
			sb.append(payr.getSearchValue());
			sb.append("\",\"");
			sb.append(payr.getUpdateCost());
			sb.append("\",\"");
			sb.append(payr.getUpdateValue());
			sb.append("\",\"");
			sb.append(payr.getFvsCost());
			sb.append("\",\"");
			sb.append(payr.getFvsValue());
			sb.append("\",\"");
			sb.append(payr.getCurrentOwnerCost());
			sb.append("\",\"");
			sb.append(payr.getCurrentOwnerValue());
			sb.append("\",\"");
			sb.append(payr.getRefinanceCost());
			sb.append("\",\"");
			sb.append(payr.getRefinanceValue());
			sb.append("\",\"");
			sb.append(payr.getConstructionValue());
			sb.append("\",\"");
			sb.append(payr.getConstructionCost());
			sb.append("\",\"");
			sb.append(payr.getCommercialValue());
			sb.append("\",\"");
			sb.append(payr.getCommercialCost());
			sb.append("\",\"");
			sb.append(payr.getOEValue());
			sb.append("\",\"");
			sb.append(payr.getOECost());
			sb.append("\",\"");
			sb.append(payr.getLiensValue());
			sb.append("\",\"");
			sb.append(payr.getLiensCost());
			sb.append("\",\"");
			sb.append(payr.getAcreageValue());
			sb.append("\",\"");
			sb.append(payr.getAcreageCost());
			sb.append("\",\"");
			sb.append(payr.getSublotValue());
			sb.append("\",\"");
			sb.append(payr.getSublotCost());
			sb.append("\",\"");

			if (payr.getDueDate() != null) {
				sb.append(formatDate.getDate(payr.getDueDate()));
			} else
				sb.append("N/A");
			sb.append("\",\"");

			if (payr.getCityDueDate() != null)

				sb.append(formatDate.getDate(payr.getCityDueDate()));
			else
				sb.append("N/A");
			sb.append("\",\"");
			sb.append(payr.getIndexC2A());
			sb.append("\",\"");
			sb.append(payr.getIndexA2C());
			sb.append("\"");
			sb.append(System.getProperty("line.separator"));
		}
		return sb;
	}

}
