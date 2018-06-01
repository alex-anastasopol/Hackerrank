package ro.cst.tsearch.servers.types;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.Vector;
import java.util.regex.Pattern;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPSiteInterface;
import ro.cst.tsearch.connection.http.HTTPSiteManager;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.propertyInformation.Family;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.RejectAlreadyPresentFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.doctype.PacerDoctypeFilterResponse;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.HTMLObject;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.titledocument.abstracts.Chapter;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class MIWaynePR extends TSServer {

	private static final long serialVersionUID = 4760453061875693625L;
	private boolean downloadingForSave;

	public static final Pattern partiesSummaryLink = Pattern.compile("(?is)pamw2000-o_party_sum\\?([^>]*)");
	public static final Pattern dispositionLink = Pattern.compile("(?is)pamw2000-o_casedsp_sum\\?([^>]*)");

	public MIWaynePR(long searchId) {
		super(searchId);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	/**
	 * @param rsRequestSolverName
	 * @param rsSitePath
	 * @param rsServerID
	 * @param rsPrmNameLink
	 */
	public MIWaynePR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public TSServerInfo getDefaultServerInfo() {

		TSServerInfo msiServerInfoDefault = null;
		TSServerInfoModule simTmp = null;

		msiServerInfoDefault = new TSServerInfo(1);
		msiServerInfoDefault.setServerAddress("public.wcpc.us");
		msiServerInfoDefault.setServerLink("http://public.wcpc.us/pa/");
		msiServerInfoDefault.setServerIP("public.wcpc.us");

		// name search
		{

			simTmp = SetModuleSearchByName(39, msiServerInfoDefault, TSServerInfo.NAME_MODULE_IDX, "/pa/pa.urd/PAMW6500", TSConnectionURL.idPOST, "LAST_NAME.PAPROFILE.PAM", "FIRST_NAME.PAPROFILE.PAM");

			try {
				PageZone searchByName = new PageZone("searchByAddress", "General Index Search", HTMLObject.ORIENTATION_HORIZONTAL, null, new Integer(600), new Integer(250), HTMLObject.PIXELS, true);
				searchByName.setBorder(true);

				HTMLControl lastName = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "LAST_NAME.PAPROFILE.PAM", "Last Name", 1, 2, 1, 1, 35, null, simTmp.getFunction(0), searchId);
				lastName.setJustifyField(true);
				lastName.setRequiredExcl(true);
				lastName.setFieldNote("(at least two characters)");
				searchByName.addHTMLObject(lastName);

				HTMLControl firstName = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "FIRST_NAME.PAPROFILE.PAM", "First Name", 1, 1, 2, 2, 20, null, simTmp.getFunction(1), searchId);
				firstName.setJustifyField(true);
				firstName.setRequiredExcl(true);
				firstName.setFieldNote("(required when searching by last name)");
				searchByName.addHTMLObject(firstName);

				HTMLControl middleName = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "MIDDLE_NAME.PAPROFILE.PAM", "Middle", 2, 2, 2, 2, 10, null, simTmp.getFunction(2), searchId);
				//middleName.setJustifyField(true);
				searchByName.addHTMLObject(middleName);

				HTMLControl actionCode = new HTMLControl(HTMLControl.HTML_SELECT_BOX, "ACTN_CD.PAPROFILE.PAM", "Action Code", 1, 2, 9, 9, 10, null, simTmp.getFunction(3), searchId);
				//actionCode.setHiddenParam(true);
				actionCode.setJustifyField(true);
				searchByName.addHTMLObject(actionCode);				
				simTmp.getFunction(3).setHtmlformat(ACTION_SELECT);
				simTmp.getFunction(3).setParamType(TSServerInfoFunction.idSingleselectcombo);

				HTMLControl companyName = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "COMPANY_NAME.PAPROFILE.PAM", "Company Name", 1, 2, 3, 3, 35, null, simTmp.getFunction(4), searchId);
				companyName.setRequiredExcl(true);
				companyName.setJustifyField(true);
				companyName.setFieldNote("(at least two characters)");
				searchByName.addHTMLObject(companyName);

				HTMLControl beginDate = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "BEGIN_DT.PAPROFILE.PAM", "Begin Date", 1, 1, 4, 4, 10, null, simTmp.getFunction(5), searchId);
				beginDate.setJustifyField(true);
				searchByName.addHTMLObject(beginDate);

				HTMLControl endDate = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "END_DT.PAPROFILE.PAM", "End Date", 2, 2, 4, 4, 10, null, simTmp.getFunction(6), searchId);
				//endDate.setJustifyField(true);
				searchByName.addHTMLObject(endDate);

				HTMLControl partyType = new HTMLControl(HTMLControl.HTML_SELECT_BOX, "PTY_CD.PAPROFILE.PAM", "Party type", 1, 2, 5, 5, 35, null, simTmp.getFunction(7), searchId);
				partyType.setJustifyField(true);
				searchByName.addHTMLObject(partyType);
				simTmp.getFunction(7).setHtmlformat(PARTY_TYPE_SELECT);
				simTmp.getFunction(7).setParamType(TSServerInfoFunction.idSingleselectcombo);				

				HTMLControl caseType = new HTMLControl(HTMLControl.HTML_SELECT_BOX, "CASE_CD.PAPROFILE.PAM", "Case type", 1, 2, 6, 6, 35, null, simTmp.getFunction(8), searchId);
				caseType.setJustifyField(true);
				searchByName.addHTMLObject(caseType);
				simTmp.getFunction(8).setHtmlformat(CASE_TYPE_SELECT);
				simTmp.getFunction(8).setParamType(TSServerInfoFunction.idSingleselectcombo);				

				HTMLControl caseNumber = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "CASE_NBR.PAPROFILE.PAM", "Case number", 1, 1, 7, 7, 20, null, simTmp.getFunction(9), searchId);
				caseNumber.setJustifyField(true);
				caseNumber.setRequiredExcl(true);
				searchByName.addHTMLObject(caseNumber);

				HTMLControl status = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "STAT_CD.PAPROFILE.PAM", "Status", 1, 1, 8, 8, 35, null, simTmp.getFunction(10), searchId);
				status.setJustifyField(true);
				searchByName.addHTMLObject(status);
				simTmp.getFunction(10).setHtmlformat(STATUS_SELECT);
				simTmp.getFunction(10).setParamType(TSServerInfoFunction.idSingleselectcombo);				

				HTMLControl paprofilePam = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "%.PAPROFILE.PAM.1.", "", 1, 1, 1, 1, 10, null, simTmp.getFunction(11), searchId);
				paprofilePam.setHiddenParam(true);
				searchByName.addHTMLObject(paprofilePam);

				HTMLControl lastNameLabel = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "LAST_NAME_LBL.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, "Last Name", simTmp.getFunction(12), searchId);
				lastNameLabel.setHiddenParam(true);
				searchByName.addHTMLObject(lastNameLabel);

				HTMLControl companyNameLabel = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "COMPANY_NAME_LBL.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, "Company Name", simTmp.getFunction(13), searchId);
				companyNameLabel.setHiddenParam(true);
				searchByName.addHTMLObject(companyNameLabel);

				HTMLControl firstNameLabel = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "FIRST_NAME_LBL.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, "First Name", simTmp.getFunction(14), searchId);
				firstNameLabel.setHiddenParam(true);
				searchByName.addHTMLObject(firstNameLabel);

				HTMLControl actionCodeLabel = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "ACTN_CD_LBL.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, "Action Code", simTmp.getFunction(15), searchId);
				actionCodeLabel.setHiddenParam(true);
				searchByName.addHTMLObject(actionCodeLabel);

				HTMLControl beginDateLabel = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "BEGIN_DT_LBL.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, "Begin Date", simTmp.getFunction(16), searchId);
				beginDateLabel.setHiddenParam(true);
				searchByName.addHTMLObject(beginDateLabel);

				HTMLControl partyTypeLabel = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "PTY_CD_LBL.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, "Party Type", simTmp.getFunction(17), searchId);
				partyTypeLabel.setHiddenParam(true);
				searchByName.addHTMLObject(partyTypeLabel);

				HTMLControl endDateLabel = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "END_DT_LBL.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, "End Date", simTmp.getFunction(18), searchId);
				endDateLabel.setHiddenParam(true);
				searchByName.addHTMLObject(endDateLabel);

				HTMLControl ssnLabel = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "SSN_LBL.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, "SSN", simTmp.getFunction(19), searchId);
				ssnLabel.setHiddenParam(true);
				searchByName.addHTMLObject(ssnLabel);

				HTMLControl ssnValue = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "SSN.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, null, simTmp.getFunction(20), searchId);
				ssnValue.setHiddenParam(true);
				searchByName.addHTMLObject(ssnValue);

				HTMLControl dobLabel = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "DOB_LBL.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, "D.O.B.", simTmp.getFunction(21), searchId);
				dobLabel.setHiddenParam(true);
				searchByName.addHTMLObject(dobLabel);

				HTMLControl dobValue = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "DOB.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, null, simTmp.getFunction(22), searchId);
				dobValue.setHiddenParam(true);
				searchByName.addHTMLObject(dobValue);

				HTMLControl caseTypeValue = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "CASE_CD_LBL.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, "Case Type", simTmp.getFunction(23), searchId);
				caseTypeValue.setHiddenParam(true);
				searchByName.addHTMLObject(caseTypeValue);

				HTMLControl dodLabel = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "DOD_LBL.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, "D.O.D", simTmp.getFunction(24), searchId);
				dodLabel.setHiddenParam(true);
				searchByName.addHTMLObject(dodLabel);

				HTMLControl dodValue = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "DOD.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, null, simTmp.getFunction(25), searchId);
				dodValue.setHiddenParam(true);
				searchByName.addHTMLObject(dodValue);

				HTMLControl ticketNumberLabel = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "TICKET_NBR_LBL.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, "Ticket Nbr", simTmp.getFunction(26), searchId);
				ticketNumberLabel.setHiddenParam(true);
				searchByName.addHTMLObject(ticketNumberLabel);

				HTMLControl ticketNumberValue = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "TICKET_NBR.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, null, simTmp.getFunction(27), searchId);
				ticketNumberValue.setHiddenParam(true);
				searchByName.addHTMLObject(ticketNumberValue);

				HTMLControl caseNumberLabel = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "CASE_NBR_LBL.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, "Case Nbr", simTmp.getFunction(28), searchId);
				caseNumberLabel.setHiddenParam(true);
				searchByName.addHTMLObject(caseNumberLabel);

				HTMLControl statusLabel = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "STAT_CD_LBL.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, "Status", simTmp.getFunction(29), searchId);
				statusLabel.setHiddenParam(true);
				searchByName.addHTMLObject(statusLabel);

				HTMLControl driversLicenseLabel = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "DRIVERS_LIC_NO_LBL.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, "Drivers License No.", simTmp.getFunction(30), searchId);
				driversLicenseLabel.setHiddenParam(true);
				searchByName.addHTMLObject(driversLicenseLabel);

				HTMLControl driversLicenseValue = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "DRIVERS_LIC_NO.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, null, simTmp.getFunction(31), searchId);
				driversLicenseValue.setHiddenParam(true);
				searchByName.addHTMLObject(driversLicenseValue);

				HTMLControl profileCrtv = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "%.PROFILE.CRTV.1.", "", 1, 1, 1, 1, 10, null, simTmp.getFunction(32), searchId);
				profileCrtv.setHiddenParam(true);
				searchByName.addHTMLObject(profileCrtv);

				HTMLControl currentYearProfile = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "CURRENT_YEAR.PROFILE.CRTV", "", 1, 1, 1, 1, 10, null, simTmp.getFunction(33), searchId);
				currentYearProfile.setHiddenParam(true);
				searchByName.addHTMLObject(currentYearProfile);

				HTMLControl resultMsg = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "RESULT_MSG.PROFILE.CRTV", "", 1, 1, 1, 1, 10, null, simTmp.getFunction(34), searchId);
				resultMsg.setHiddenParam(true);
				searchByName.addHTMLObject(resultMsg);

				HTMLControl searchButton = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "SEARCH_BUTTON.PAPROFILE.PAM", "", 1, 1, 1, 1, 10, "Searching", simTmp.getFunction(35), searchId);
				searchButton.setHiddenParam(true);
				searchByName.addHTMLObject(searchButton);

				HTMLControl srchPr02 = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "#.SRCHPR02.PAM.1-1-1.", "", 1, 1, 1, 1, 10, "fS9JQzgxMDIyNDAz", simTmp.getFunction(36), searchId);
				srchPr02.setHiddenParam(true);
				searchByName.addHTMLObject(srchPr02);

				HTMLControl crcsrch = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "#CRC.SRCHPR02.PAM.1-1-1.", "", 1, 1, 1, 1, 10, "00000021", simTmp.getFunction(37), searchId);
				crcsrch.setHiddenParam(true);
				searchByName.addHTMLObject(crcsrch);

				HTMLControl actnCdPrf = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "ACTN_CD_PRF.SRCHPR02.PAM", "", 1, 1, 1, 1, 10, "1", simTmp.getFunction(38), searchId);
				actnCdPrf.setHiddenParam(true);
				searchByName.addHTMLObject(actnCdPrf);

				simTmp.setModuleParentSiteLayout(searchByName);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		msiServerInfoDefault.setupParameterAliases();
		setModulesForAutoSearch(msiServerInfoDefault);
        setModulesForGoBackOneLevelSearch(msiServerInfoDefault);
		return msiServerInfoDefault;
	}

	protected String getFileNameFromLink(String link) {
		String parcelId = StringUtils.getTextBetweenDelimiters("caseNum=", "&", link);
		return parcelId + ".html";
	}

	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		
		String rsResponse = Response.getResult();

		// check if we have received an error
		String initialResponse = rsResponse;
		String keyNumber = "";
		String sTmp;
		int istart, iend;

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
			
			String message = StringUtils.extractParameter(rsResponse, "<INPUT TYPE=HIDDEN NAME=\"RESULT_MSG.PROFILE.CRTV\" VALUE=\"([^\"]+)\"");
			if(!StringUtils.isEmpty(message) && !message.contains("record(s) found")){				
				Response.getParsedResponse().setWarning(message);
				return;
			}

			istart = rsResponse.indexOf("<TABLE WIDTH=\"100%\">");
			iend = rsResponse.indexOf("</TABLE>");

			if (istart < 0 || iend < 0) {
				return;
			}

			if (rsResponse.contains("General Index Search Criteria")) {
				return;
			}

			sTmp = CreatePartialLink(TSConnectionURL.idGET);
			rsResponse = rsResponse.substring(istart, iend + 8);
			rsResponse = rsResponse.replaceAll("(?is)<input[^>]*>", "");
			rsResponse = rsResponse.replaceAll("(?is) href=pamw2000-o_case_sum\\?([^>]*)", " href=\"" + sTmp + "/pa/pa.urd/pamw2000-o_case_sum&caseNum=$1\"");
			rsResponse = rsResponse.replaceAll("<FONT COLOR=\"#FFFFFF\">", "<FONT COLOR=\"#000000\">");
			
			rsResponse = rsResponse.replaceFirst("<TABLE", "<TABLE border=1 cellpadding=0 cellspacing=0");
			rsResponse = rsResponse.replaceAll("<TD WIDTH=\"12%\">", "<TD WIDTH=\"12%\">&nbsp;");
			rsResponse = rsResponse.replaceAll("<TD WIDTH=\"15%\">\\s+", "<TD WIDTH=\"15%\">&nbsp;"); 
			
			rsResponse = rsResponse.replaceFirst("<TR", "<TR bgcolor=\"#DEDEDE\"");
			parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET), TSServer.REQUEST_SAVE_TO_TSD);			
			break;
			
		case ID_DETAILS:

			keyNumber = StringUtils.getTextBetweenDelimiters("caseNum=", "&", Response.getQuerry());

			// we have summary from the detailed page
			istart = rsResponse.indexOf("<FORM");
			iend = rsResponse.indexOf("</FORM>", istart + 1);

			if (istart < 0 || iend < 0) {
				return;
			}

			rsResponse = rsResponse.substring(istart, iend);
			rsResponse = rsResponse.replaceAll("(?is)<form[^>]*>", "");
			rsResponse = rsResponse.replaceAll("(?is)</form>", "");
			rsResponse = rsResponse.replaceAll("(?is)<input[^>]*>", "");
			rsResponse = rsResponse.replaceAll("(?is)<input[^>]*>", "");
			rsResponse = rsResponse.replaceAll("(?is)<map.*?</map>", "");
			rsResponse = rsResponse.replaceAll("(?is)<img[^>]*>", "");			

			// get parties page
			HTTPSiteInterface site = HTTPSiteManager.pairHTTPSiteForTSServer("MIWaynePR", searchId, miServerID);

			try {
				String partiesPage = "";
				HTTPRequest req = new HTTPRequest("http://public.wcpc.us/pa/pa.urd/pamw2000-party_lst?" + keyNumber);
				HTTPResponse res = site.process(req);

				partiesPage = res.getResponseAsString();

				partiesPage += "";

				String appendSummaryPage = partiesPage;

				istart = appendSummaryPage.indexOf("<FORM");
				iend = appendSummaryPage.indexOf("</FORM>", istart + 1);

				if (istart < 0 || iend < 0) {
					appendSummaryPage = "";
				} else {
					appendSummaryPage = appendSummaryPage.substring(istart, iend);
					appendSummaryPage = appendSummaryPage.replaceAll("(?is)<form[^>]*>", "");
					appendSummaryPage = appendSummaryPage.replaceAll("(?is)</form>", "");
					appendSummaryPage = appendSummaryPage.replaceAll("(?is)<input[^>]*>", "");
					appendSummaryPage = appendSummaryPage.replaceAll("(?is)<map.*?</map>", "");
					appendSummaryPage = appendSummaryPage.replaceAll("(?is)<img[^>]*>", "");
					appendSummaryPage = appendSummaryPage.replaceAll("(?is)<a[^>]*>", "");
					appendSummaryPage = appendSummaryPage.replaceAll("(?is)</a>", "");

					rsResponse += appendSummaryPage;
				}

				/*
				// get parties summary and addresses by drilling down on name
				// links
				Matcher partiesSummaryMatcher = partiesSummaryLink.matcher(partiesPage);
				while (partiesSummaryMatcher.find()) {
					String partiesSummaryPage = "http://public.wcpc.us/pa/pa.urd/pamw2000-o_party_sum?" + partiesSummaryMatcher.group(1);

					req = new HTTPRequest(partiesSummaryPage);
					res = site.process(req);

					appendSummaryPage = res.getResponseAsString();

					istart = appendSummaryPage.indexOf("<FORM");
					iend = appendSummaryPage.indexOf("</FORM>", istart + 1);

					if (istart < 0 || iend < 0) {
						appendSummaryPage = "";
					} else {
						appendSummaryPage = appendSummaryPage.substring(istart, iend);
						appendSummaryPage = appendSummaryPage.replaceAll("(?is)<form[^>]*>", "");
						appendSummaryPage = appendSummaryPage.replaceAll("(?is)</form>", "");

						appendSummaryPage = appendSummaryPage.replaceAll("(?is)<input[^>]*>", "");
						appendSummaryPage = appendSummaryPage.replaceAll("(?is)<map.*?</map>", "");
						appendSummaryPage = appendSummaryPage.replaceAll("(?is)<img[^>]*>", "");

						rsResponse += appendSummaryPage;
					}

					String partiesAddressPage = "http://public.wcpc.us/pa/pa.urd/pamw2000-o_ptyaddr_lst?" + partiesSummaryMatcher.group(1);
					req = new HTTPRequest(partiesAddressPage);
					res = site.process(req);

					appendSummaryPage = res.getResponseAsString();

					istart = appendSummaryPage.indexOf("</html>");
					iend = appendSummaryPage.indexOf("<!DOCTYPE", istart + 1);

					if (istart < 0 || iend < 0) {
						appendSummaryPage = "";
					} else {
						appendSummaryPage = appendSummaryPage.substring(istart, iend);
						appendSummaryPage = appendSummaryPage.replaceAll("(?is)<html>", "");
						appendSummaryPage = appendSummaryPage.replaceAll("(?is)</html>", "");
						appendSummaryPage = appendSummaryPage.replaceAll("(?is)<input[^>]*>", "");
						appendSummaryPage = appendSummaryPage.replaceAll("(?is)<map.*?</map>", "");
						appendSummaryPage = appendSummaryPage.replaceAll("(?is)<img[^>]*>", "");

						rsResponse += appendSummaryPage;
					}
				}
				*/
			} catch (Exception e) {
				e.printStackTrace();
			}

			// get events
			/*
			try {
				String eventsPage = "";
				HTTPRequest req = new HTTPRequest("http://public.wcpc.us/pa/pa.urd/pamw2000-event_lst?" + keyNumber);
				HTTPResponse res = site.process(req);

				eventsPage = res.getResponseAsString();

				istart = eventsPage.indexOf("<FORM");
				iend = eventsPage.indexOf("</FORM>", istart + 1);

				if (istart < 0 || iend < 0) {
					eventsPage = "";
				} else {
					eventsPage = eventsPage.substring(istart, iend);
					eventsPage = eventsPage.replaceAll("(?is)<form[^>]*>", "");
					eventsPage = eventsPage.replaceAll("(?is)</form>", "");

					eventsPage = eventsPage.replaceAll("(?is)<input[^>]*>", "");
					eventsPage = eventsPage.replaceAll("(?is)<map.*?</map>", "");
					eventsPage = eventsPage.replaceAll("(?is)<select.*?</select>", "");
					eventsPage = eventsPage.replaceAll("(?is)<img[^>]*>", "");

					rsResponse += eventsPage;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			*/
			
			// get dockets tab
			/*
			try {
				String docketsPage = "";
				HTTPRequest req = new HTTPRequest("http://public.wcpc.us/pa/pa.urd/pamw2000-docket_lst?" + keyNumber);
				HTTPResponse res = site.process(req);

				docketsPage = res.getResponseAsString();

				istart = docketsPage.indexOf("<FORM");
				iend = docketsPage.indexOf("</FORM>", istart + 1);

				if (istart < 0 || iend < 0) {
					docketsPage = "";
				} else {
					docketsPage = docketsPage.substring(istart, iend);
					docketsPage = docketsPage.replaceAll("(?is)<form[^>]*>", "");
					docketsPage = docketsPage.replaceAll("(?is)</form>", "");

					docketsPage = docketsPage.replaceAll("(?is)<input[^>]*>", "");
					docketsPage = docketsPage.replaceAll("(?is)<map.*?</map>", "");
					docketsPage = docketsPage.replaceAll("(?is)<select.*?</select>", "");
					docketsPage = docketsPage.replaceAll("(?is)<img[^>]*>", "");

					rsResponse += docketsPage;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			*/
			
			/*
			// get case dispositions
			try {
				String caseDispositionList = "";
				HTTPRequest req = new HTTPRequest("http://public.wcpc.us/pa/pa.urd/pamw2000-o_casedsp_lst?" + keyNumber);
				HTTPResponse res = site.process(req);

				caseDispositionList = res.getResponseAsString();

				String appendDispListPage = caseDispositionList;

				istart = appendDispListPage.indexOf("<FORM");
				iend = appendDispListPage.indexOf("</FORM>", istart + 1);

				if (istart < 0 || iend < 0) {
					appendDispListPage = "";
				} else {
					appendDispListPage = appendDispListPage.substring(istart, iend);
					appendDispListPage = appendDispListPage.replaceAll("(?is)<form[^>]*>", "");
					appendDispListPage = appendDispListPage.replaceAll("(?is)</form>", "");
					appendDispListPage = appendDispListPage.replaceAll("(?is)<input[^>]*>", "");
					appendDispListPage = appendDispListPage.replaceAll("(?is)<map.*?</map>", "");
					appendDispListPage = appendDispListPage.replaceAll("(?is)<img[^>]*>", "");
					appendDispListPage = appendDispListPage.replaceAll("(?is)<a[^>]*>", "");
					appendDispListPage = appendDispListPage.replaceAll("(?is)</a>", "");

					rsResponse += appendDispListPage;
				}

				// drill down on links
				Matcher dispositionMatcher = dispositionLink.matcher(caseDispositionList);
				while (dispositionMatcher.find()) {
					String dispositionPage = "http://public.wcpc.us/pa/pa.urd/pamw2000-o_casedsp_sum?" + dispositionMatcher.group(1);

					req = new HTTPRequest(dispositionPage);
					res = site.process(req);

					appendDispListPage = res.getResponseAsString();

					istart = appendDispListPage.indexOf("<FORM");
					iend = appendDispListPage.indexOf("</FORM>", istart + 1);

					if (istart < 0 || iend < 0) {
						appendDispListPage = "";
					} else {
						appendDispListPage = appendDispListPage.substring(istart, iend);
						appendDispListPage = appendDispListPage.replaceAll("(?is)<form[^>]*>", "");
						appendDispListPage = appendDispListPage.replaceAll("(?is)</form>", "");

						appendDispListPage = appendDispListPage.replaceAll("(?is)<input[^>]*>", "");
						appendDispListPage = appendDispListPage.replaceAll("(?is)<map.*?</map>", "");
						appendDispListPage = appendDispListPage.replaceAll("(?is)<img[^>]*>", "");

						rsResponse += appendDispListPage;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			*/
			/*
			// get costs
			try {
				String costsPage = "";
				HTTPRequest req = new HTTPRequest("http://public.wcpc.us/pa/pa.urd/pamw2000-o_casecst_sum?" + keyNumber);
				HTTPResponse res = site.process(req);

				costsPage = res.getResponseAsString();

				istart = costsPage.indexOf("<FORM");
				iend = costsPage.indexOf("</FORM>", istart + 1);

				if (istart < 0 || iend < 0) {
					costsPage = "";
				} else {
					costsPage = costsPage.substring(istart, iend);
					costsPage = costsPage.replaceAll("(?is)<form[^>]*>", "");
					costsPage = costsPage.replaceAll("(?is)</form>", "");

					costsPage = costsPage.replaceAll("(?is)<input[^>]*>", "");
					costsPage = costsPage.replaceAll("(?is)<map.*?</map>", "");
					costsPage = costsPage.replaceAll("(?is)<select.*?</select>", "");
					costsPage = costsPage.replaceAll("(?is)<img[^>]*>", "");

					rsResponse += costsPage;
				}

				// get checks list
				req = new HTTPRequest("http://public.wcpc.us/pa/pa.urd/pamw2010-o_checks_lst?" + keyNumber);
				res = site.process(req);

				costsPage = res.getResponseAsString();

				istart = costsPage.indexOf("</html>");
				iend = costsPage.indexOf("<!DOCTYPE", istart + 1);

				if (istart < 0 || iend < 0) {
					costsPage = "";
				} else {
					costsPage = costsPage.substring(istart, iend);
					costsPage = costsPage.replaceAll("(?is)<html>", "");
					costsPage = costsPage.replaceAll("(?is)</html>", "");

					costsPage = costsPage.replaceAll("(?is)<input[^>]*>", "");
					costsPage = costsPage.replaceAll("(?is)<map.*?</map>", "");
					costsPage = costsPage.replaceAll("(?is)<select.*?</select>", "");
					costsPage = costsPage.replaceAll("(?is)<img[^>]*>", "");

					rsResponse += costsPage;
				}

				// get receipts list
				req = new HTTPRequest("http://public.wcpc.us/pa/pa.urd/pamw2010-o_receipt_lst?" + keyNumber);
				res = site.process(req);

				costsPage = res.getResponseAsString();

				istart = costsPage.indexOf("<FORM");
				iend = costsPage.indexOf("</FORM>", istart + 1);

				if (istart < 0 || iend < 0) {
					costsPage = "";
				} else {
					costsPage = costsPage.substring(istart, iend);
					costsPage = costsPage.replaceAll("(?is)<form[^>]*>", "");
					costsPage = costsPage.replaceAll("(?is)</form>", "");

					costsPage = costsPage.replaceAll("(?is)<input[^>]*>", "");
					costsPage = costsPage.replaceAll("(?is)<map.*?</map>", "");
					costsPage = costsPage.replaceAll("(?is)<select.*?</select>", "");
					costsPage = costsPage.replaceAll("(?is)<img[^>]*>", "");

					rsResponse += costsPage;
				}

				// get $dockets
				req = new HTTPRequest("http://public.wcpc.us/pa/pa.urd/pamw2010-o_fin_docket_sum?" + keyNumber);
				res = site.process(req);

				costsPage = res.getResponseAsString();

				istart = costsPage.indexOf("<FORM");
				iend = costsPage.indexOf("</FORM>", istart + 1);

				if (istart < 0 || iend < 0) {
					costsPage = "";
				} else {
					costsPage = costsPage.substring(istart, iend);
					costsPage = costsPage.replaceAll("(?is)<form[^>]*>", "");
					costsPage = costsPage.replaceAll("(?is)</form>", "");

					costsPage = costsPage.replaceAll("(?is)<input[^>]*>", "");
					costsPage = costsPage.replaceAll("(?is)<map.*?</map>", "");
					costsPage = costsPage.replaceAll("(?is)<select.*?</select>", "");
					costsPage = costsPage.replaceAll("(?is)<img[^>]*>", "");

					rsResponse += costsPage;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			*/

			rsResponse = rsResponse.replaceAll("(?is)<br>", "");
			rsResponse = rsResponse.replaceAll("(?is)<h\\d*>", "");
			rsResponse = rsResponse.replaceAll("(?is)</h\\d*>", "");
			rsResponse = rsResponse.replaceAll("(?is)<font[^>]*>", "");
			rsResponse = rsResponse.replaceAll("(?is)</font>", "");
			rsResponse = rsResponse.replaceAll("(?is)<b>", "");
			rsResponse = rsResponse.replaceAll("(?is)</b>", "");
			rsResponse = rsResponse.replaceAll("(?is)<![^>]*>", "");

			if (!downloadingForSave) {
				// not saving to TSR
				String qry = Response.getQuerry();
				Response.setQuerry(qry);
				String originalLink = sAction + "&" + qry;
				String sSave2TSDLink = CreatePartialLink(TSConnectionURL.idPOST) + originalLink;
				if (FileAlreadyExist(keyNumber + ".html") ) {
					rsResponse += CreateFileAlreadyInTSD();
				} else {
					rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink, viParseID);
					mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
				}
				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				parser.Parse(Response.getParsedResponse(), rsResponse, Parser.NO_PARSE);
			} else {
				// saving
				msSaveToTSDFileName = keyNumber + ".html";
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();
				parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_DETAILS, getLinkPrefix(TSConnectionURL.idPOST), TSServer.REQUEST_SAVE_TO_TSD);
			}
			break;
			
		case ID_SAVE_TO_TSD:			
			downloadingForSave = true;
			ParseResponse(sAction, Response, ID_DETAILS);
			downloadingForSave = false;
			break;
			
		case ID_GET_LINK:
			if (sAction.contains("pamw2000-o_case_sum")) {
				ParseResponse(sAction, Response, ID_DETAILS);
			} else {
				ParseResponse(sAction, Response, ID_SEARCH_BY_NAME);
			}
			break;
		}
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		
		FilterResponse rejectAlreadyPresentFilter = new RejectAlreadyPresentFilterResponse(searchId);
		
		
		for(String key: new String[]{SearchAttributes.OWNER_OBJECT, SearchAttributes.BUYER_OBJECT}){
			
			TSServerInfoModule module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			DocsValidator intervalValidator = new BetweenDatesFilterResponse(searchId, module).getValidator();
			module.clearSaKeys();
			module.clearIteratorTypes();
			module.setSaObjKey(key);
			module.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_LAST_NAME_FAKE);
			module.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_FIRST_NAME_FAKE);
					
			GenericNameFilter nameFilter = (GenericNameFilter)NameFilterFactory.getDefaultNameFilter(key, searchId, module);
	        nameFilter.setIgnoreMiddleOnEmpty(true);
	        module.addFilter(nameFilter);
	        module.addFilter(rejectAlreadyPresentFilter);
	        module.addValidator(intervalValidator);	        
			ConfigurableNameIterator iterator = new ConfigurableNameIterator(searchId, new String[] {"L;F;"});
			iterator.setInitAgain(true);
			module.addIterator(iterator);						
	        
			modules.add(module);
		}

		serverInfo.setModulesForAutoSearch(modules);

	}
	
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
		serverInfo.setModulesForGoBackOneLevelSearch(modules);
    }

	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action) throws ro.cst.tsearch.exceptions.ServerResponseException {
		p.splitResultRows(pr, htmlString, pageId, "<TR", "</TABLE>", linkStart, action);
		// remove table header
		Vector rows = pr.getResultRows();
		if (rows.size() > 0) {
			ParsedResponse firstRow = (ParsedResponse) rows.remove(0);
			pr.setResultRows(rows);
			pr.setHeader(pr.getHeader() + firstRow.getResponse());
		}
	}
	
    protected ServerResponse SearchBy(boolean bResetQuery, TSServerInfoModule module, Object sd) throws ServerResponseException {
    	    	
    	// use company name search while in automatic and only last name available
    	String last = module.getParamValue(0);
    	String first = module.getParamValue(1);
    	String middle = module.getParamValue(2);    	
    	if(!StringUtils.isEmpty(last) && StringUtils.isEmpty(first) && StringUtils.isEmpty(middle)){
    		module.forceValue(0, "");
    		module.forceValue(4, last);    		
    	}
    	    	
    	// search as usual
    	return super.SearchBy(bResetQuery, module, sd);
    }

	private static final String PARTY_TYPE_SELECT = 
		"<SELECT NAME=\"PTY_CD.PAPROFILE.PAM\">" +
		"<OPTION VALUE=\"\" SELECTED>ALL" +
		"<OPTION VALUE=\"ADE\">Plenary Guardian Estate" +
		"<OPTION VALUE=\"ADM\">*Administrator" +
		"<OPTION VALUE=\"ADP\">Plenary Guardian Person" +
		"<OPTION VALUE=\"AGY\">Agency" +
		"<OPTION VALUE=\"CLM\">Claimant" +
		"<OPTION VALUE=\"CNSLR\">Court Appointed Attorney" +
		"<OPTION VALUE=\"CNSVTR\">Conservator" +
		"<OPTION VALUE=\"COR\">Correctional Facility" +
		"<OPTION VALUE=\"DBN\">*Administrator De Bonis Non" +
		"<OPTION VALUE=\"DCDNT\">Decedent" +
		"<OPTION VALUE=\"DFNDT\">Defendant" +
		"<OPTION VALUE=\"ESC\">*Escheats" +
		"<OPTION VALUE=\"EX\">*Executor" +
		"<OPTION VALUE=\"FAT\">Father" +
		"<OPTION VALUE=\"GMR\">*Guardian Mentally Retarded" +
		"<OPTION VALUE=\"GRANTOR\">Grantor" +
		"<OPTION VALUE=\"GRDN\">Guardian" +
		"<OPTION VALUE=\"HSP\">Hospital" +
		"<OPTION VALUE=\"IDD\">An Individual with a Developmental Disability" +
		"<OPTION VALUE=\"IMO\">In The Matter Of" +
		"<OPTION VALUE=\"INC\">Incarcerated Individual" +
		"<OPTION VALUE=\"IPI\">*Ind Pers Rep Intestate" +
		"<OPTION VALUE=\"IPT\">*Ind Pers Rep Testate" +
		"<OPTION VALUE=\"IPY\">Interested Person" +
		"<OPTION VALUE=\"IVT\">Interactive Video" +
		"<OPTION VALUE=\"MISC\">Misc." +
		"<OPTION VALUE=\"MN\">Minor" +
		"<OPTION VALUE=\"MOM\">Mother" +
		"<OPTION VALUE=\"NEWNM\">*New Name" +
		"<OPTION VALUE=\"NIP\">No Longer An Interested Person" +
		"<OPTION VALUE=\"NMP\">Nominee Party" +
		"<OPTION VALUE=\"NSF\">Nominated Special Fiduciary" +
		"<OPTION VALUE=\"OLD NAME\">*Original Name" +
		"<OPTION VALUE=\"PDE\">Partial Guardian Estate" +
		"<OPTION VALUE=\"PDP\">Partial Guardian Person" +
		"<OPTION VALUE=\"PDW\">Person Depositing Will" +
		"<OPTION VALUE=\"PET\">Petitioner" +
		"<OPTION VALUE=\"PLNTF\">Plaintiff" +
		"<OPTION VALUE=\"PLO\">Preliminary Order" +
		"<OPTION VALUE=\"PR\">Personal Representative" +
		"<OPTION VALUE=\"PREV\">Previous Attorney" +
		"<OPTION VALUE=\"PRO\">Protective Order" +
		"<OPTION VALUE=\"PTI\">Protected Individual" +
		"<OPTION VALUE=\"PWW\">Person Withdrawing Will" +
		"<OPTION VALUE=\"RSPND\">Subject of a Petition" +
		"<OPTION VALUE=\"SBGRDN\">Stand-By Guardian" +
		"<OPTION VALUE=\"SME\">*Small Estate MCLA 700.102" +
		"<OPTION VALUE=\"SPF\">Special Fiduciary" +
		"<OPTION VALUE=\"SPS\">Spouse" +
		"<OPTION VALUE=\"SUR\">Surety" +
		"<OPTION VALUE=\"TESTATOR\">Testator (Will)" +
		"<OPTION VALUE=\"TRA\">Trustee" +
		"<OPTION VALUE=\"TRT\">Trustee, Intervivos (Supervised)" +
		"<OPTION VALUE=\"TRU\">Trust" +
		"<OPTION VALUE=\"TRV\">Trustee, Intervivos" +
		"<OPTION VALUE=\"WARD\">Legally Incapacitated Individual" +
		"<OPTION VALUE=\"WPG\">*Spec Admin W/ Powers General" +
		"<OPTION VALUE=\"WWA\">*Adm Will Annexed" +
		"</SELECT>";
	
	private static final String CASE_TYPE_SELECT = 
		"<SELECT NAME=\"CASE_CD.PAPROFILE.PAM\">" +
		"<OPTION VALUE=\"\" SELECTED>ALL" +
		"<OPTION VALUE=\"AK\">z*AK-Acknowledgement of Parantage (Obsolete)" +
		"<OPTION VALUE=\"AT\">AT-Appeals to Probate" +
		"<OPTION VALUE=\"BR\">BR-Delayed Reg Of Foreign Birth" +
		"<OPTION VALUE=\"BX\">BX-Safe Deposit Box" +
		"<OPTION VALUE=\"CA\">CA-Conservators, Adult" +
		"<OPTION VALUE=\"CG\">z*CG-Guardian / Conservator (Obsolete)" +
		"<OPTION VALUE=\"CY\">CY-Conservators, Minor" +
		"<OPTION VALUE=\"CZ\">CZ - Civil" +
		"<OPTION VALUE=\"DA\">DA-Dec Estate-Supervised" +
		"<OPTION VALUE=\"DD\">DD-Developmentally Disabled" +
		"<OPTION VALUE=\"DDT\">DDT-Developmentally Disabled (Estate)" +
		"<OPTION VALUE=\"DE\">DE-Dec Estate Non-Supervised" +
		"<OPTION VALUE=\"DH\">DH-Determination Of Heirs" +
		"<OPTION VALUE=\"DN\">DN-Demand for Notice" +
		"<OPTION VALUE=\"EM\">z*EM-Emancipation (Obsolete)" +
		"<OPTION VALUE=\"ES\">z*ES-Escheats (Obsolete)" +
		"<OPTION VALUE=\"GA\">GA-Guardians, Adult Full" +
		"<OPTION VALUE=\"GL\">GL-Guardians, Adult Limited" +
		"<OPTION VALUE=\"GM\">GM-Guardians, Minor Full" +
		"<OPTION VALUE=\"JA\">JA-Judicial Admission" +
		"<OPTION VALUE=\"LG\">LG-Limited Guardian, Minor" +
		"<OPTION VALUE=\"MI\">MI-Mentally Ill Petition" +
		"<OPTION VALUE=\"ML\">ML-Miscellaneous" +
		"<OPTION VALUE=\"MW\">z*MW-Marriage Waiver (Obsolete)" +
		"<OPTION VALUE=\"NC\">z*NC-Name Change (Obsolete)" +
		"<OPTION VALUE=\"PE\">PE-Assignment of Property" +
		"<OPTION VALUE=\"PO\">PO-Protective Orders" +
		"<OPTION VALUE=\"PW\">z*PW-Waiver of Parental Consent (Obsolete)" +
		"<OPTION VALUE=\"TR\">TR-Trust Registration" +
		"<OPTION VALUE=\"TT\">TT-Trusts, Testamentary" +
		"<OPTION VALUE=\"TV\">TV-Trust, Inter Vivos" +
		"<OPTION VALUE=\"TX\">TX-Tax File" +
		"<OPTION VALUE=\"WSD\">WSD-Wills For Safekeeping - Deceased" +
		"</SELECT>";
	
	private static final String STATUS_SELECT = 
		"<SELECT NAME=\"STAT_CD.PAPROFILE.PAM\">" +
		"<OPTION VALUE=\"A\">*Do not use" +
		"<OPTION VALUE=\"C\">Closed" +
		"<OPTION VALUE=\"N\">N/A" +
		"<OPTION VALUE=\"O\">Open" +
		"<OPTION VALUE=\"RO\">Reopened" +
		"<OPTION VALUE=\"W\">*Do not use" +
		"<OPTION VALUE=\"\" SELECTED>ALL" +
		"</SELECT>";
	
	private static final String ACTION_SELECT = 
		"<SELECT name=\"ACTN_CD.PAPROFILE.PAM\">" +
		"<OPTION VALUE=\"\" SELECTED>ALL" +
		"<OPTION VALUE=\"AK\">*Acknowledgement of Parantage" +
		"<OPTION VALUE=\"AT\">Appeals to Probate Court" +
		"<OPTION VALUE=\"BR\">Delayed Registration of Foreign Birth" +
		"<OPTION VALUE=\"BX\">Safe Deposit Box" +
		"<OPTION VALUE=\"CA\">Conservatorship for an Adult" +
		"<OPTION VALUE=\"CG\">*Guardian / Conservator" +
		"<OPTION VALUE=\"CY\">Conservatorship for a Minor" +
		"<OPTION VALUE=\"CZ\">Civil" +
		"<OPTION VALUE=\"DAI\">Intestate" +
		"<OPTION VALUE=\"DAT\">Testate" +
		"<OPTION VALUE=\"DD\">DD-Partial" +
		"<OPTION VALUE=\"DDP\">DD-Plenary" +
		"<OPTION VALUE=\"DDPP\">DD-Plenary and Partial" +
		"<OPTION VALUE=\"DEIF\">Intestate Formal" +
		"<OPTION VALUE=\"DEII\">Intestate Informal" +
		"<OPTION VALUE=\"DETF\">Testate Formal" +
		"<OPTION VALUE=\"DETI\">Testate Informal" +
		"<OPTION VALUE=\"DH\">Determination of Heirs" +
		"<OPTION VALUE=\"DN\">Demand for Notice" +
		"<OPTION VALUE=\"EM\">*Emancipation" +
		"<OPTION VALUE=\"ES\">*Escheats" +
		"<OPTION VALUE=\"GA\">Guardianship of an Adult" +
		"<OPTION VALUE=\"GL\">Guardian Adult Limited" +
		"<OPTION VALUE=\"GM\">Guardianship of a Minor" +
		"<OPTION VALUE=\"GMJ\">*Minor Guardianship Juvenile" +
		"<OPTION VALUE=\"JA\">Judicial Admissions" +
		"<OPTION VALUE=\"LG\">Guardian Minor Limited" +
		"<OPTION VALUE=\"LGJ\">*Limited Guardianship of a Minor Juvenile" +
		"<OPTION VALUE=\"MI\">Mental Illness" +
		"<OPTION VALUE=\"ML\">Miscellaneous" +
		"<OPTION VALUE=\"MS\">Secret Marriage" +
		"<OPTION VALUE=\"MW\">*Marriage Waiver" +
		"<OPTION VALUE=\"NC\">*Name Change" +
		"<OPTION VALUE=\"OCV\">Order for Change of Venue" +
		"<OPTION VALUE=\"OSV\">Objection to Hospitalization of a Minor" +
		"<OPTION VALUE=\"OT\">Other" +
		"<OPTION VALUE=\"PE\">Assignment of Property" +
		"<OPTION VALUE=\"PFH\">Petition for Hospitalization (C60) (pcm210)" +
		"<OPTION VALUE=\"PO\">Protective Orders" +
		"<OPTION VALUE=\"POT\">POT Petition and Order to Transport a Minor (ETO) (pcm240)" +
		"<OPTION VALUE=\"PW\">*Waiver of Parental Consent" +
		"<OPTION VALUE=\"SPA\">SPA Supplemental Petition/Application for Hospitalization & Order for Examination" +
		"<OPTION VALUE=\"TR\">Trust Registration" +
		"<OPTION VALUE=\"TT\">Trusts, Testamentary" +
		"<OPTION VALUE=\"TV\">Trust, Inter Vivos" +
		"<OPTION VALUE=\"TX\">Tax File" +
		"<OPTION VALUE=\"VD\">VD Voided Case" +
		"<OPTION VALUE=\"WSD\">Wills for Safekeeping - Deceased" +
		"<OPTION VALUE=\"WSK\">Wills for Safekeeping" +
		"</SELECT>";

}
