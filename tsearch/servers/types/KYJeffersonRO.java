package ro.cst.tsearch.servers.types;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.time.DateFormatUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPManager;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.connection.http.HTTPSite;
import ro.cst.tsearch.connection.http.HTTPSiteInterface;
import ro.cst.tsearch.connection.http.HTTPSiteManager;
import ro.cst.tsearch.connection.http2.HttpManager;
import ro.cst.tsearch.connection.http2.HttpSite;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.filter.BetweenDatesFilterResponse;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.address.AddressFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.DateFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.date.LastTransferDateFilter;
import ro.cst.tsearch.search.filter.newfilters.doctype.DoctypeFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.legal.LegalFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.name.NameFilterFactory;
import ro.cst.tsearch.search.filter.newfilters.pin.PINFilterFactory;
import ro.cst.tsearch.search.iterator.ModuleStatesIteratorFactory;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.validator.DocsValidator;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoConstants;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.info.spitHTML.HTMLControl;
import ro.cst.tsearch.servers.info.spitHTML.HTMLObject;
import ro.cst.tsearch.servers.info.spitHTML.PageZone;
import ro.cst.tsearch.servers.response.CrossRefSet;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.Parser;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.servlet.URLConnectionReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.GBManager;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.name.NameI;

public class KYJeffersonRO extends TSServer implements TSServerROLikeI {

	private static final long serialVersionUID = -4450683706152064923L;

	private boolean downloadingForSave;

	private static final int ID_SEARCH_BY_BOOK_PAGE_ADV = 102;

	public static final Pattern hiddenPageNamePattern = Pattern.compile("hidPageName=(\\w+)");
	public static final Pattern hiddenCurrentRecordPattern = Pattern.compile("hidCurrentRec=(\\d+)");
	public static final Pattern totalRecsPattern = Pattern.compile("hidTotalNumRecs=(\\d+)");

	public static final Pattern imageLinkPattern = Pattern.compile("<a.*?href=\"javascript:openimagewin\\('(.*?)'\\).*?>\\s*View</a>");
	public static final Pattern detailsImageLinkPattern = Pattern
			.compile("(?s)<a.*?href=\"javascript:openimagewin\\('(.*?)'\\).*?>\\s*<!--.*?-->\\s*VIEW</a>");
	public static final Pattern hrefPattern = Pattern.compile("href=\"(.*?)\"");

	public static final Pattern nextControlLinkPattern = Pattern.compile("moveNext\\(document\\.S1Resultsform,(\\d+)\\)");
	public static final Pattern nextLinkPattern = Pattern.compile("<a .*?href=.*?>Next page</a>");
	public static final Pattern prevLinkPattern = Pattern.compile("<a .*?href=.*?>Previous page</a>");

	public static final Pattern instrumentNumberPattern = Pattern.compile("(?s)Reference Number:.*?<td.*?<a href.*?>(.*?)</a");
	public static final Pattern bookPageRefPattern = Pattern.compile("(?s)Book/Page:</th>\\s+<td.*?>\\s+(.*?)</td");
	
	private static final Pattern certDatePattern = Pattern.compile("(?is)All records from August 15, 1984 to present are indexed on this system");

	//changed default from 25 to 30 because all configs had 30
	public static int recordsPerPage = 30;

	/**
	 * this represents the first year for which the server will retrieve images
	 */
	private static int MIN_YEAR_FOR_IMAGE = 1992; // this is default on server

	private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);

	public static final String groupSelect = "<select name=\"group\">" + "<option value=\"  \" selected>   - none</option>"
			+ "<option value=\"1\">1</option>" + "<option value=\"2\">2</option>" + "<option value=\"3\">3</option>"
			+ "<option value=\"4\">4</option>" + "<option value=\"5\">5</option>" + "<option value=\"6\">6</option>"
			+ "<option value=\"7\">7</option>" + "<option value=\"21\">21</option>" + "</select>";

	public static final String docTypeSelect = "<select name=\"doctype\">" + "<option value=\"  \" selected>     - none</option>"
			+ "<option value=\"AAA\">AAA - AGRE AMEND ARL</option>" + "<option value=\"AAM\">AAM - AGRE AMEND MTG</option>"
			+ "<option value=\"AAT\">AAT - AffidToAidTitle</option>" + "<option value=\"ACD\">ACD - Amend Cont Deed</option>"
			+ "<option value=\"AF\">AF - Affidavit</option>" + "<option value=\"AFC\">AFC - Aff of Consider</option>"
			+ "<option value=\"AFD\">AFD - Aff of Descent</option>" + "<option value=\"AG\">AG - Agreement</option>"
			+ "<option value=\"AGM\">AGM - Mod Mtg Ag & AS</option>" + "<option value=\"AL\">AL - Attorneys Lien</option>"
			+ "<option value=\"AMF\">AMF - Amended Fixture</option>" + "<option value=\"APT\">APT - Amend/Rev Plat</option>"
			+ "<option value=\"ARL\">ARL - Asgn Rent/Lease</option>" + "<option value=\"AS\">AS - Assignment</option>"
			+ "<option value=\"ASF\">ASF - Assgn Fix File</option>" + "<option value=\"ASJ\">ASJ - Assign of Judgm</option>"
			+ "<option value=\"ASL\">ASL - ASGN LIS PENDEN</option>" + "<option value=\"BAS\">BAS - BLANKET ASSIGN</option>"
			+ "<option value=\"BB\">BB - Bail Bond</option>" + "<option value=\"BH\">BH - Bldg & Housing</option>"
			+ "<option value=\"BL\">BL - Boarding Lien</option>" + "<option value=\"BLU\">BLU - City Blue</option>"
			+ "<option value=\"BR\">BR - Blanket Release</option>" + "<option value=\"CCP\">CCP - Certified Copy</option>"
			+ "<option value=\"CFF\">CFF - Cont of Fixture</option>" + "<option value=\"CL\">CL - City Lien</option>"
			+ "<option value=\"CM\">CM - Cond Mylar</option>" + "<option value=\"CND\">CND - Cond Master Ded</option>"
			+ "<option value=\"COD\">COD - Cert of Delinqu</option>" + "<option value=\"CPY\">CPY - Regular Copy</option>"
			+ "<option value=\"CR\">CR - Check Refund</option>" + "<option value=\"CRE\">CRE - Conversion R E</option>"
			+ "<option value=\"CRP\">CRP - Corporation</option>" + "<option value=\"CS\">CS - CHILD SUPPORT</option>"
			+ "<option value=\"CSE\">CSE - Con Scenic Ease</option>" + "<option value=\"CSR\">CSR - REL CHILD SPT L</option>"
			+ "<option value=\"DBA\">DBA - Doing Bus. As</option>" + "<option value=\"DBN\">DBN - Discontinu Name</option>"
			+ "<option value=\"DCN\">DCN - Consolidation D</option>" + "<option value=\"DCR\">DCR - Deed Corr W Rel</option>"
			+ "<option value=\"DEC\">DEC - Decrease Drw/Ac</option>" + "<option value=\"DED\">DED - deed</option>"
			+ "<option value=\"DFC\">DFC - Ded of Contract</option>" + "<option value=\"DNF\">DNF - Ded St Fee Exmp</option>"
			+ "<option value=\"DOC\">DOC - Deed of Corr</option>" + "<option value=\"DOR\">DOR - Ded of Restrict</option>"
			+ "<option value=\"DOT\">DOT - Decl of Trust</option>" + "<option value=\"DRN\">DRN - Ded Rest N St F</option>"
			+ "<option value=\"DVL\">DVL - DEED W/VENDLIEN</option>" + "<option value=\"DWR\">DWR - Ded with a Rel</option>"
			+ "<option value=\"EMA\">EMA - Mtg Ext Agre</option>" + "<option value=\"EMS\">EMS - Esmt St Fee Exm</option>"
			+ "<option value=\"EST\">EST - Easement</option>" + "<option value=\"FAL\">FAL - FALSE ALRM LIEN</option>"
			+ "<option value=\"FAR\">FAR - RELEASE OF FAL</option>" + "<option value=\"FAS\">FAS - Floor Assigment</option>"
			+ "<option value=\"FDS\">FDS - Fed Lien Subord</option>" + "<option value=\"FED\">FED - Federal Lien</option>"
			+ "<option value=\"FF\">FF - Fixture Filing</option>" + "<option value=\"FM\">FM - Fire Minutes</option>"
			+ "<option value=\"FRA\">FRA - Rel Fl Atty Ln</option>" + "<option value=\"FRD\">FRD - Rel Fl Mtg/Ded</option>"
			+ "<option value=\"FWA\">FWA - Fixture W/ Asgn</option>" + "<option value=\"INC\">INC - IncreaseDrwr/Ac</option>"
			+ "<option value=\"INR\">INR - Inher Tx w/Rel</option>" + "<option value=\"JUD\">JUD - Judgment Lien</option>"
			+ "<option value=\"LCS\">LCS - CHILD SUPPORT</option>" + "<option value=\"LL\">LL - Lottery Lien</option>"
			+ "<option value=\"LMV\">LMV - Liens Motor Veh</option>" + "<option value=\"LP\">LP - Lis Pendens</option>"
			+ "<option value=\"LUR\">LUR - Land Use Restr</option>" + "<option value=\"MDL\">MDL - Maint. Lien</option>"
			+ "<option value=\"MER\">MER - Mtg Elec Regist</option>" + "<option value=\"MI\">MI - Mental Inquest</option>"
			+ "<option value=\"ML\">ML - Mechanics Lien</option>" + "<option value=\"MLO\">MLO - ML-OTHER</option>"
			+ "<option value=\"MM\">MM - Master Mortgage</option>" + "<option value=\"MMA\">MMA - Mtg Modify Agre</option>"
			+ "<option value=\"MMD\">MMD - Misc M Discharg</option>" + "<option value=\"MNF\">MNF - Mtg St Fee Exmp</option>"
			+ "<option value=\"MOC\">MOC - Mtg of Correct</option>" + "<option value=\"MRC\">MRC - MER Correction</option>"
			+ "<option value=\"MSA\">MSA - Subord Agre</option>" + "<option value=\"MSC\">MSC - Miscellaneous</option>"
			+ "<option value=\"MTA\">MTA - Mtg With Assign</option>" + "<option value=\"MTG\">MTG - Mortgage</option>"
			+ "<option value=\"MTS\">MTS - Mtg With Subord</option>" + "<option value=\"MVL\">MVL - Modify Vends Ln</option>"
			+ "<option value=\"NC\">NC - Name Change</option>" + "<option value=\"OTH\">OTH - Other</option>"
			+ "<option value=\"PFL\">PFL - Partial Fed Rel</option>" + "<option value=\"PLP\">PLP - Partial Rel LP</option>"
			+ "<option value=\"PLT\">PLT - Plat</option>" + "<option value=\"POA\">POA - Power of Atty</option>"
			+ "<option value=\"POR\">POR - POA WITH REVOC</option>" + "<option value=\"PR\">PR - Partial Release</option>"
			+ "<option value=\"PRF\">PRF - Partial Rel Fix</option>" + "<option value=\"PRI\">PRI - Partial MI Rel</option>"
			+ "<option value=\"PRT\">PRT - Partnership</option>" + "<option value=\"PST\">PST - Partial St Rel</option>"
			+ "<option value=\"RAL\">RAL - Rel/Asgn Atty L</option>" + "<option value=\"RBB\">RBB - Rel Bail Bond</option>"
			+ "<option value=\"RBH\">RBH - Rel Bldg & Hous</option>" + "<option value=\"RBL\">RBL - Rel Boarding l</option>"
			+ "<option value=\"RBN\">RBN - Grant Esmt/Rel</option>" + "<option value=\"RCD\">RCD - Rel of Cert Del</option>"
			+ "<option value=\"RCL\">RCL - Rel City Lien</option>" + "<option value=\"RCS\">RCS - REL CHILD SPT L</option>"
			+ "<option value=\"RED\">RED - Rel Easement</option>" + "<option value=\"REL\">REL - Release</option>"
			+ "<option value=\"RFA\">RFA - Rel Fl Maint Ln</option>" + "<option value=\"RFB\">RFB - Rel Fl B & H</option>"
			+ "<option value=\"RFC\">RFC - Rel Fl City Ln</option>" + "<option value=\"RFD\">RFD - Rel Fed Lien</option>"
			+ "<option value=\"RFJ\">RFJ - Rel Floor Judg</option>" + "<option value=\"RFL\">RFL - Rel Floor L P</option>"
			+ "<option value=\"RFM\">RFM - Rel FL Mech Ln</option>" + "<option value=\"RFP\">RFP - Revoke Fl POA</option>"
			+ "<option value=\"RJD\">RJD - Rel Judg Lien</option>" + "<option value=\"RLB\">RLB - Rel ML By Bond</option>"
			+ "<option value=\"RLF\">RLF - Rel Fix Filing</option>" + "<option value=\"RLL\">RLL - Rel Lottery Ln</option>"
			+ "<option value=\"RLP\">RLP - Rel Lis Pendens</option>" + "<option value=\"RMD\">RMD - Rel Maint Lien</option>"
			+ "<option value=\"RMI\">RMI - Restoration MI</option>" + "<option value=\"RML\">RML - Rel Mech Lien</option>"
			+ "<option value=\"RMS\">RMS - Release of Misc</option>" + "<option value=\"RPO\">RPO - Revoke POA</option>"
			+ "<option value=\"RST\">RST - Rel State Lien</option>" + "<option value=\"RUT\">RUT - Rel US Tax Lien</option>"
			+ "<option value=\"SC\">SC - Street Closing</option>" + "<option value=\"ST\">ST - State Lien</option>"
			+ "<option value=\"STS\">STS - St Lien Subord</option>" + "<option value=\"TCF\">TCF - Inqiry Fax</option>"
			+ "<option value=\"TCP\">TCP - Inquiry Copy</option>" + "<option value=\"TCV\">TCV - Vendacard</option>"
			+ "<option value=\"TFI\">TFI - FAX IN</option>" + "<option value=\"UST\">UST - US Tax Lien</option>"
			+ "<option value=\"VOI\">VOI - Voided Document</option>" + "<option value=\"WIL\">WIL - Will</option>"
			+ "<option value=\"ZZZ\">ZZZ - Last Doc Type</option>" + "</select>";

	public KYJeffersonRO(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
		resultType = MULTIPLE_RESULT_TYPE;
	}

	public TSServerInfo getDefaultServerInfo() {
		TSServerInfo msiServerInfoDefault = null;
		TSServerInfoModule simTmp = null;

		msiServerInfoDefault = new TSServerInfo(5);

		msiServerInfoDefault.setServerAddress("www.landrecords.jcc.ky.gov");

		msiServerInfoDefault.setServerLink("http://www.landrecords.jcc.ky.gov/");

		msiServerInfoDefault.setServerIP("www.landrecords.jcc.ky.gov");

		// build start date and end date formatted strings
		String startDate = "", endDate = "";
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
			SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
			Date start = sdf.parse(sa.getAtribute(SearchAttributes.FROMDATE));
			Date end = sdf.parse(sa.getAtribute(SearchAttributes.TODATE));
			sdf.applyPattern("MMddyyyy");
			startDate = sdf.format(start);
			endDate = sdf.format(end);
		} catch (ParseException e) {
		}

		// control number search
		{

			simTmp = msiServerInfoDefault.ActivateModule(TSServerInfo.PROP_NO_IDX, 3);
			simTmp.setName("Control Number");
			simTmp.setDestinationPage("/records/S1DataLKUP.jsp");
			simTmp.setRequestMethod(TSConnectionURL.idPOST);
			simTmp.setParserID(ID_SEARCH_BY_PROP_NO);

			try {
				PageZone searchByRefNo = new PageZone("searchByCtrlfNo", "Control Number search", HTMLObject.ORIENTATION_HORIZONTAL, null,
						new Integer(600), new Integer(250), HTMLObject.PIXELS, true);
				searchByRefNo.setBorder(true);

				HTMLControl docCode = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "controlnumber", "Control Number:", 1, 1, 1, 1, 30, "",
						simTmp.getFunction(0), searchId);
				docCode.setJustifyField(true);
				docCode.setRequiredCritical(true);
				searchByRefNo.addHTMLObject(docCode);

				HTMLControl search = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "DoItButton", "", 1, 1, 1, 1, 10, "Search", simTmp
						.getFunction(1), searchId);
				search.setHiddenParam(true);
				searchByRefNo.addHTMLObject(search);

				HTMLControl hiddenPageName = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "hidPageName", "", 1, 1, 1, 1, 10, "S1Search",
						simTmp.getFunction(2), searchId);
				hiddenPageName.setHiddenParam(true);
				searchByRefNo.addHTMLObject(hiddenPageName);

				simTmp.setModuleParentSiteLayout(searchByRefNo);
			} catch (Exception e) {
				e.printStackTrace();
			}

			simTmp.getFunction(0).setDefaultValue("");
			simTmp.getFunction(1).setHiddenParam("DoItButton", "Search");
			simTmp.getFunction(2).setHiddenParam("hidPageName", "S1Search");
		}

		// search by party name
		{
			simTmp = SetModuleSearchByName(10, msiServerInfoDefault, TSServerInfo.NAME_MODULE_IDX, "/records/S2DataLKUP.jsp",
					TSConnectionURL.idPOST, "surname", "givenname");

			try {
				PageZone searchByName = new PageZone("searchByName", "Search by Party Name", HTMLObject.ORIENTATION_HORIZONTAL, null,
						new Integer(600), new Integer(250), HTMLObject.PIXELS, true);
				searchByName.setBorder(true);

				HTMLControl lastName = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "surname", "Surname:", 1, 1, 1, 1, 30, null, simTmp
						.getFunction(0), searchId);
				lastName.setJustifyField(true);
				lastName.setRequiredExcl(true);
				searchByName.addHTMLObject(lastName);

				HTMLControl firstName = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "givenname", "Given Name:", 1, 1, 2, 2, 30, null,
						simTmp.getFunction(1), searchId);
				firstName.setJustifyField(true);
				firstName.setRequiredExcl(true);
				searchByName.addHTMLObject(firstName);

				HTMLControl middleName = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "middlename", "Middle Name:", 1, 1, 3, 3, 30, null,
						simTmp.getFunction(2), searchId);
				middleName.setJustifyField(true);
				middleName.setRequiredExcl(true);
				searchByName.addHTMLObject(middleName);

				HTMLControl dateFrom = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "fromdate", "Date Filed From:", 1, 1, 4, 4, 30,
						startDate, simTmp.getFunction(3), searchId);
				dateFrom.setJustifyField(true);
				dateFrom.setRequiredExcl(true);
				dateFrom.setFieldNote("MMDDYYYY");
				searchByName.addHTMLObject(dateFrom);

				HTMLControl dateTo = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "todate", "Date Filed To:", 1, 1, 5, 5, 30, endDate,
						simTmp.getFunction(4), searchId);
				dateTo.setJustifyField(true);
				dateTo.setRequiredExcl(true);
				dateTo.setFieldNote("MMDDYYYY");
				searchByName.addHTMLObject(dateTo);

				HTMLControl docType = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "doctype", "Doc Type:", 1, 1, 6, 6, 30, "  ", simTmp
						.getFunction(5), searchId);
				docType.setJustifyField(true);
				searchByName.addHTMLObject(docType);

				HTMLControl group = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "group", "Group:", 1, 1, 7, 7, 30, "  ", simTmp
						.getFunction(6), searchId);
				group.setJustifyField(true);
				searchByName.addHTMLObject(group);

				HTMLControl partyType = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "partytype", "Party Type:", 1, 1, 8, 8, 30, null,
						simTmp.getFunction(7), searchId);
				partyType.setJustifyField(true);
				searchByName.addHTMLObject(partyType);

				HTMLControl search = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "DoItButton", "", 1, 1, 9, 9, 30, "Search", simTmp
						.getFunction(8), searchId);
				search.setHiddenParam(true);
				searchByName.addHTMLObject(search);

				HTMLControl hiddenPageName = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "hidPageName", "", 1, 1, 9, 9, 30, "S2Search",
						simTmp.getFunction(9), searchId);
				hiddenPageName.setHiddenParam(true);
				searchByName.addHTMLObject(hiddenPageName);

				simTmp.setModuleParentSiteLayout(searchByName);
			} catch (Exception e) {
				e.printStackTrace();
			}

			simTmp.getFunction(5).setDefaultValue("  ");
			simTmp.getFunction(5).setHtmlformat(docTypeSelect);

			simTmp.getFunction(6).setDefaultValue("  ");
			simTmp.getFunction(6).setHtmlformat(groupSelect);

			simTmp.getFunction(8).setHiddenParam("DoItButton", "Search");
			simTmp.getFunction(9).setHiddenParam("hidPageName", "S2Search");

		}

		// book page search
		{
			simTmp = SetModuleSearchByBookAndPage(6, msiServerInfoDefault, TSServerInfo.BOOK_AND_PAGE_MODULE_IDX,
					"/records/S3DataLKUP.jsp", TSConnectionURL.idPOST, "book", "page");
			try {
				PageZone searchByBookPage = new PageZone("searchByBookPage", "Book-Page search", HTMLObject.ORIENTATION_HORIZONTAL, null,
						new Integer(600), new Integer(250), HTMLObject.PIXELS, true);
				searchByBookPage.setBorder(true);

				HTMLControl book = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "book", "Book:", 1, 1, 1, 1, 30, null, simTmp
						.getFunction(0), searchId);
				book.setJustifyField(true);
				book.setRequiredExcl(true);
				searchByBookPage.addHTMLObject(book);

				HTMLControl page = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "page", "Page:", 1, 1, 2, 2, 30, null, simTmp
						.getFunction(1), searchId);
				page.setJustifyField(true);
				page.setRequiredExcl(true);
				searchByBookPage.addHTMLObject(page);

				HTMLControl bookType = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "booktype", "Book Type:", 1, 1, 3, 3, 30, "M", simTmp
						.getFunction(2), searchId);
				bookType.setJustifyField(true);
				searchByBookPage.addHTMLObject(bookType);

				HTMLControl search = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "DoItButton", "", 1, 1, 1, 1, 30, "Search", simTmp
						.getFunction(3), searchId);
				search.setHiddenParam(true);
				searchByBookPage.addHTMLObject(search);

				HTMLControl hiddenPageName = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "hidPageName", "", 1, 1, 1, 1, 30, "S3Search",
						simTmp.getFunction(4), searchId);
				hiddenPageName.setHiddenParam(true);
				searchByBookPage.addHTMLObject(hiddenPageName);

				HTMLControl info = new HTMLControl(HTMLControl.HTML_LABEL, "SearchInstructions",
						"No leading Zeros are required.  The Page number is an optional field.", 1, 1, 4, 4, 30, null, simTmp
								.getFunction(5), searchId);
				searchByBookPage.addHTMLObject(info);

				simTmp.setModuleParentSiteLayout(searchByBookPage);
			} catch (Exception e) {
				e.printStackTrace();
			}

			simTmp.getFunction(2).setHtmlformat(
					"<select name=\"booktype\">" + "<option value=\"M\" selected>M - Mortgage</option>"
							+ "<option value=\"C\">C - Corporation</option>" + "<option value=\"D\">D - Deed</option>"
							+ "<option value=\"F\">F - Fixture filing</option>" + "<option value=\"L\">L - Lien</option>"
							+ "<option value=\"N\">N - Name Change</option>" + "<option value=\"P\">P - Plat</option>"
							+ "<option value=\"W\">W - Will</option>" + "<option value=\"X\">X - Condominium</option>"
							+ "<option value=\"Z\">Z - Miscellaneous</option>"
							+ "<option value=\"B\">B - Rel of Mech Lien by Bond</option>" + "</select>");
			simTmp.getFunction(3).setHiddenParam("DoItButton", "Search");
			simTmp.getFunction(4).setHiddenParam("hidPageName", "S3Search");
		}

		// reference number search
		{
			simTmp = SetModuleSearchByInstrumentNo(5, msiServerInfoDefault, TSServerInfo.INSTR_NO_MODULE_IDX, "/records/S4DataLKUP.jsp",
					TSConnectionURL.idPOST, "seqnumber");

			try {
				PageZone searchByRefNo = new PageZone("searchByRefNo", "Reference Number search", HTMLObject.ORIENTATION_HORIZONTAL, null,
						new Integer(600), new Integer(250), HTMLObject.PIXELS, true);
				searchByRefNo.setBorder(true);

				HTMLControl docCode = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "doccode", "Reference Number:", 1, 1, 1, 1, 10, "DN",
						simTmp.getFunction(1), searchId);
				docCode.setJustifyField(true);
				docCode.setRequiredExcl(true);
				searchByRefNo.addHTMLObject(docCode);

				HTMLControl year = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "year", "Year:", 2, 2, 1, 1, 10, null, simTmp
						.getFunction(2), searchId);
				year.setRequiredExcl(true);
				searchByRefNo.addHTMLObject(year);

				HTMLControl seqNumber = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "seqnumber", "", 3, 3, 1, 1, 10, null, simTmp
						.getFunction(0), searchId);
				seqNumber.setRequiredExcl(true);
				searchByRefNo.addHTMLObject(seqNumber);

				HTMLControl search = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "DoItButton", "", 1, 1, 1, 1, 10, "Search", simTmp
						.getFunction(3), searchId);
				search.setHiddenParam(true);
				searchByRefNo.addHTMLObject(search);

				HTMLControl hiddenPageName = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "hidPageName", "", 1, 1, 1, 1, 10, "S4Search",
						simTmp.getFunction(4), searchId);
				hiddenPageName.setHiddenParam(true);
				searchByRefNo.addHTMLObject(hiddenPageName);

				simTmp.setModuleParentSiteLayout(searchByRefNo);
			} catch (Exception e) {
				e.printStackTrace();
			}

			simTmp.getFunction(1).setDefaultValue("DN");
			simTmp.getFunction(3).setHiddenParam("DoItButton", "Search");
			simTmp.getFunction(4).setHiddenParam("hidPageName", "S4Search");
		}

		// Refers To Search
		{
			simTmp = SetModuleSearchByBookAndPage(5, msiServerInfoDefault, TSServerInfo.ADV_SEARCH_MODULE_IDX, "/records/S5DataLKUP.jsp",
					TSConnectionURL.idPOST, "book", "page");
			simTmp.setParserID(ID_SEARCH_BY_BOOK_PAGE_ADV);
			try {
				PageZone refersToSearch = new PageZone("refersToSearch", "Refers To Search", HTMLObject.ORIENTATION_HORIZONTAL, null,
						new Integer(600), new Integer(250), HTMLObject.PIXELS, true);
				refersToSearch.setBorder(true);

				HTMLControl book = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "book", "Book:", 1, 1, 1, 1, 30, null, simTmp
						.getFunction(0), searchId);
				book.setJustifyField(true);
				book.setRequiredExcl(true);
				refersToSearch.addHTMLObject(book);

				HTMLControl page = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "page", "Page:", 1, 1, 2, 2, 30, null, simTmp
						.getFunction(1), searchId);
				page.setJustifyField(true);
				page.setRequiredExcl(true);
				refersToSearch.addHTMLObject(page);

				HTMLControl bookType = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "booktype", "Book Type:", 1, 1, 3, 3, 30, "M", simTmp
						.getFunction(2), searchId);
				bookType.setJustifyField(true);
				refersToSearch.addHTMLObject(bookType);

				HTMLControl search = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "DoItButton", "", 1, 1, 1, 1, 30, "Search", simTmp
						.getFunction(3), searchId);
				search.setHiddenParam(true);
				refersToSearch.addHTMLObject(search);

				HTMLControl hiddenPageName = new HTMLControl(HTMLControl.HTML_TEXT_FIELD, "hidPageName", "", 1, 1, 1, 1, 30, "S5Search",
						simTmp.getFunction(4), searchId);
				hiddenPageName.setHiddenParam(true);
				refersToSearch.addHTMLObject(hiddenPageName);

				simTmp.setModuleParentSiteLayout(refersToSearch);
			} catch (Exception e) {
				e.printStackTrace();
			}

			simTmp.getFunction(2).setDefaultValue("M");
			simTmp.getFunction(2).setHtmlformat(
					"<select name=\"booktype\">" + "<option value=\"M\" selected>M - Mortgage</option>"
							+ "<option value=\"C\">C - Corporation</option>" + "<option value=\"D\">D - Deed</option>"
							+ "<option value=\"F\">F - Fixture filing</option>" + "<option value=\"L\">L - Lien</option>"
							+ "<option value=\"N\">N - Name Change</option>" + "<option value=\"P\">P - Plat</option>"
							+ "<option value=\"W\">W - Will</option>" + "<option value=\"X\">X - Condominium</option>"
							+ "<option value=\"Z\">Z - Miscellaneous</option>"
							+ "<option value=\"B\">B - Rel of Mech Lien by Bond</option>" + "</select>");
			simTmp.getFunction(3).setHiddenParam("DoItButton", "Search");
			simTmp.getFunction(4).setHiddenParam("hidPageName", "S5Search");
		}
		msiServerInfoDefault.setupParameterAliases();
		setModulesForAutoSearch(msiServerInfoDefault);
		setModulesForGoBackOneLevelSearch(msiServerInfoDefault);

		return msiServerInfoDefault;
	}

	public static void splitResultRows(Parser p, ParsedResponse pr, String htmlString, int pageId, String linkStart, int action)
			throws ro.cst.tsearch.exceptions.ServerResponseException {
		p.splitResultRows(pr, htmlString, pageId, "<tr><td class=\"FieldValue\"", "</table>", linkStart, action);
	}

	/*
	 * - workaround pt calc radu - nu stergeti deocamdata please private String
	 * getResponseFromStream(HTTPResponse res) { InputStream is =
	 * res.getResponseAsStream(); String body = null; if ( body != null ) return
	 * body;
	 * 
	 * ByteArrayOutputStream baos = new ByteArrayOutputStream();
	 * 
	 * byte[] buff = new byte[1024];
	 * 
	 * try{ int length; while ( (length = is.read(buff)) != -1 ) {
	 * baos.write(buff, 0, length); } }catch(IOException ioe){
	 * //ioe.printStackTrace(System.out); System.err.println("jakarta commons
	 * httpclint problem appeared!"); }
	 * 
	 * body = new String(baos.toByteArray());
	 * 
	 * return body; }
	 */

	private String getImageLink(String initialImageUrl) {
		return "http://www.landrecords.jcc.ky.gov/records/" + initialImageUrl;
	}

	/*
	private String getFinalImageLink(String intermediateImageUrl) {
		String newImageLink = null;
		String result = "";

		try {
			HTTPRequest req = new HTTPRequest(intermediateImageUrl);
			req.noRedirects = true;

			HTTPResponse res = HTTPSiteManager.pairHTTPSiteForTSServer("KYJeffersonRO", searchId, miServerID).process(req);

			result = res.getResponseAsString();
			// result = getResponseFromStream(res); // workaround calc radu - nu
			// stergeti deocamdata please

			if (result.indexOf("The requested document is not available") >= 0) {
				// image for this document not available
				return null;
			}

			Matcher m = hrefPattern.matcher(result);

			if (m.find()) {
				newImageLink = m.group(1);
				newImageLink = newImageLink.replace("&amp;", "&");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return newImageLink;
	}
	*/

	protected String getFileNameFromLink(String link) {
		String fileName = "";
		if (link.indexOf("dummy") != -1) {
			fileName = link.replaceAll(".*dummy=([^&\"\' ]*).*", "$1");
		} else {
			fileName = link.replaceAll(".*cntrlnum=([^&\"\' ]*).*", "$1");
		}
		return fileName + ".html";
	}

	// get instrument number
	public String getInstNo(String rsResponse) {
		Matcher instNoMatcher = instrumentNumberPattern.matcher(rsResponse);
		if (instNoMatcher.find()) {
			String instNo = instNoMatcher.group(1);
			instNo = instNo.replaceAll("<!--.*?-->", "");
			instNo = instNo.replaceAll("\\s+", "");
			return instNo.trim();
		}

		return "none";
	}

	private static Pattern bptPattern = Pattern.compile("(.)\\s+(\\d+)\\s+(\\d+)");

	public String[] getBookPageType(String rsResponse) {
		Matcher instNoMatcher = bookPageRefPattern.matcher(rsResponse);
		if (instNoMatcher.find()) {
			String instNo = instNoMatcher.group(1);
			instNo = instNo.replaceAll("<!--.*?-->", "");
			// instNo = instNo.replaceAll( "\\s+", "" );
			String bpt = instNo.trim();
			Matcher bptMatcher = bptPattern.matcher(bpt);
			if (bptMatcher.matches()) {
				return new String[] { bptMatcher.group(2), bptMatcher.group(3), bptMatcher.group(1) };
			}
		}
		return null;
	}

	// get doctype string, e.g. MRTG
	public String getDocTypeString(String rsResponse) {
		int istart, iend;
		istart = rsResponse.indexOf("Document Type:</th>");
		if (istart == -1) {
			return "none";
		}
		istart = rsResponse.indexOf("<td", istart);
		if (istart == -1) {
			return "none";
		}
		istart = rsResponse.indexOf(">", istart);
		if (istart == -1) {
			return "none";
		}
		istart++;
		iend = rsResponse.indexOf("<", istart);
		String retVal = rsResponse.substring(istart, iend);
		return retVal.trim();
	}

	// used to trick engine to get next pages with same link
	private static int unqDmyNmbr = 0;

	public boolean fakeImage(String imgLink) {
		boolean fake = true;

		try {
			HTTPRequest req = new HTTPRequest(imgLink);
			req.noRedirects = true;
			HTTPResponse res = HTTPSiteManager.pairHTTPSiteForTSServer("KYJeffersonRO", searchId, miServerID).process(req);

			if (res.getReturnCode() == 302) {
				// redirect, not fake
				fake = false;
			}
		} catch (Exception e) {

		}

		return fake;
	}

/* Pretty prints a link that was already followed when creating TSRIndex
 * (non-Javadoc)
 * @see ro.cst.tsearch.servers.types.TSServer#getPrettyFollowedLink(java.lang.String)
 */	
    public String getPrettyFollowedLink (String initialFollowedLnk)
    {
    	if (initialFollowedLnk.matches("(?i).*[^a-z]+dummy[=]([a-z0-9]+)[^a-z0-9]*.*"))
    	{
    		String retStr = initialFollowedLnk.replaceFirst(
    				"(?i).*[^a-z]+dummy[=]([a-z0-9]+)[^a-z0-9]*.*", 
    				"Instrument " + "$1" +
    				" has already been processed from a previous search in the log file.");
    		retStr =  "<br/><span class='followed'>"+retStr+"</span><br/>";
    		
    		return retStr;
    	}
    	
    	return "<br/><span class='followed'>Link already followed</span>:" + initialFollowedLnk + "<br/>";
    }
	
	protected void ParseResponse(String sAction, ServerResponse Response, int viParseID) throws ServerResponseException {
		// enable bookPageType only if we have a subresult / we just got a
		// crossreference
		if (Response.getQuerry().indexOf("isSubResult") != -1) {
			InstanceManager.getManager().getCurrentInstance(searchId).enableBookPageType();
		} else {
			InstanceManager.getManager().getCurrentInstance(searchId).disableBookPageType();
		}

		String rsResponse = Response.getResult();
		unqDmyNmbr++;

		Matcher hidPageNameMatcher = hiddenPageNamePattern.matcher(rsResponse);
		String hidPageName = "S2Result";
		int currentRecord = 1;
		int totalRecs = 1;

		if (hidPageNameMatcher.find()) {
			hidPageName = hidPageNameMatcher.group(1);
		}

		Matcher currentRecordMatcher = hiddenCurrentRecordPattern.matcher(rsResponse);
		if (currentRecordMatcher.find()) {
			try {
				currentRecord = Integer.parseInt(currentRecordMatcher.group(1));
			} catch (NumberFormatException nfe) {
			}
		}

		Matcher totalRecsMatcher = totalRecsPattern.matcher(rsResponse);
		if (totalRecsMatcher.find()) {
			try {
				totalRecs = Integer.parseInt(totalRecsMatcher.group(1));
			} catch (NumberFormatException nfe) {
			}
		}

		rsResponse = rsResponse.replaceAll("&nbsp", " ");
		rsResponse = rsResponse.replaceAll("</?INIT>", " ");

		String initialResponse = rsResponse;
		int iStart = -1, iEnd = -1;

		String tableIndex = "<HR>";
		String tableEnd = "</table>";

		String detailsStartToken = "<!-- Display general information -->";
		String detailsEndToken = "<form";

		String sTmp = CreatePartialLink(TSConnectionURL.idGET);

		String prefix = null;

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
			prefix = "S2DataLKUP.jsp";
			hidPageName = "S2Result";
			break;
		case ID_SEARCH_BY_BOOK_AND_PAGE:
			prefix = "S3DataLKUP.jsp";
			hidPageName = "S3Result";
			break;
		case ID_SEARCH_BY_INSTRUMENT_NO:
			prefix = "S4DataLKUP.jsp";
			hidPageName = "S4Result";
			break;
		case ID_SEARCH_BY_BOOK_PAGE_ADV:
			prefix = "S5DataLKUP.jsp";
			hidPageName = "S5Result";
			break;
		case ID_SEARCH_BY_PROP_NO:
			prefix = "S1DataLKUP.jsp";
			hidPageName = "S1Result";
			break;
		}

		switch (viParseID) {
		case ID_SEARCH_BY_NAME:
		case ID_SEARCH_BY_BOOK_AND_PAGE:
		case ID_SEARCH_BY_INSTRUMENT_NO:
		case ID_SEARCH_BY_BOOK_PAGE_ADV:
		case ID_SEARCH_BY_PROP_NO:

			Matcher nextLinkMatcher = nextLinkPattern.matcher(rsResponse);
			String linkNext = "";
			String linkPrev = "";

			String nextPages = "";

			if (nextLinkMatcher.find()) {
				linkNext = sTmp + "/records/" + prefix + "&hidPageName=" + hidPageName;
				String lastControlNumber = "";

				if (viParseID == ID_SEARCH_BY_PROP_NO) {
					// "moveNext(document.S1Resultsform,198402690524)"
					Matcher m = nextControlLinkPattern.matcher(rsResponse);
					if (m.find()) {
						lastControlNumber = m.group(1);
					}
					linkNext += "&controlnumber=" + lastControlNumber;
				}
				// add the dummy parameter so that the mechanism in
				// ModulesStatesIterator will allow
				// us the same link more than once
				if (InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchType() != Search.PARENT_SITE_SEARCH) {
					linkNext = linkNext + "&unqDmyNmbr=" + unqDmyNmbr;
				}

				if (InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchType() == Search.PARENT_SITE_SEARCH) {
					// append all next results

					String nextUrl = "http://www.landrecords.jcc.ky.gov/records/" + prefix + "?hidPageName=" + hidPageName;
					if (viParseID == ID_SEARCH_BY_PROP_NO) {
						nextUrl += "&controlnumber=" + lastControlNumber;
					}
					int totalResults = 5;

					while (nextUrl != null && totalResults < recordsPerPage) {
						try {
							HTTPRequest req = new HTTPRequest(nextUrl);
							HTTPResponse res = HTTPSiteManager.pairHTTPSiteForTSServer("KYJeffersonRO", searchId, miServerID).process(req);
							String result = res.getResponseAsString();

							// give up retrieving more pages if error occurs
							if (result.contains("<title>Error Page</title>")) {
								break;
							}

							Matcher findNextLink = nextLinkPattern.matcher(result);
							if (findNextLink.find()) {
								nextUrl = "http://www.landrecords.jcc.ky.gov/records/" + prefix + "?hidPageName=" + hidPageName;
								if (viParseID == ID_SEARCH_BY_PROP_NO) {
									Matcher m = nextControlLinkPattern.matcher(result);
									if (m.find()) {
										lastControlNumber = m.group(1);
									}
									nextUrl += "&controlnumber=" + lastControlNumber;
								}
							} else {
								nextUrl = null;
							}

							currentRecordMatcher = hiddenCurrentRecordPattern.matcher(result);
							if (currentRecordMatcher.find()) {
								try {
									currentRecord = Integer.parseInt(currentRecordMatcher.group(1));
								} catch (NumberFormatException nfe) {
								}
							}

							result = getTable(tableIndex, tableEnd, result);
							result = result.replaceAll("(?i)<table.*?>", "");
							result = result.replaceAll("(?i)</table>", "");
							result = result.replaceFirst("(?is)<tr>.*?</tr>", "");
							result = result.replaceAll("&nbsp", "&nbsp;");

							nextPages += result;

							totalResults += 5;
						} catch (Exception e) {

						}
					}
					if (viParseID == ID_SEARCH_BY_PROP_NO) {
						linkNext = sTmp + "/records/" + prefix + "&hidPageName=" + hidPageName + "&controlnumber=" + lastControlNumber;
					}
				}
			}

			Matcher prevLinkMatcher = prevLinkPattern.matcher(rsResponse);
			if (prevLinkMatcher.find()) {
				currentRecord = currentRecord + 4;
				int remainder = currentRecord % recordsPerPage;

				if (remainder == 0) {
					linkPrev = sTmp + "/records/" + hidPageName + "s.jsp&hidRecPerPage=5&hidCurrentRec="
							+ (currentRecord - (2 * recordsPerPage) + 1) + "&hidTotalNumRecs=" + totalRecs + "&hidPageName=" + hidPageName;
				} else {
					linkPrev = sTmp + "/records/" + hidPageName + "s.jsp&hidRecPerPage=5&hidCurrentRec="
							+ (currentRecord - (remainder + recordsPerPage) + 1) + "&hidTotalNumRecs=" + totalRecs + "&hidPageName="
							+ hidPageName;
				}
			}

			rsResponse = getTable(tableIndex, tableEnd, rsResponse);

			rsResponse = rsResponse.replace("</table>", nextPages + "</table>");

			rsResponse = rsResponse.replaceAll("<th.*?>Option</th>", "");
			rsResponse = rsResponse.replaceAll("<td.*?><input.*?type=\"text\".*?</td>", "");

			rsResponse = rsResponse.replaceAll("<a.*?href=\"(.*?)\".*?>Info</a>", "<a href=\"" + sTmp + "/records/$1\">Info</a>");
			rsResponse = rsResponse.replaceAll("DocInfo.jsp\\?", "DocInfo.jsp&");
			rsResponse = rsResponse.replaceAll("<tr>\\s+<td class=\"FieldValue\"", "<tr><td class=\"FieldValue\"");
			rsResponse = rsResponse.replaceAll("(?s)(openimagewin\\(.*?refnum=(.*?)'\\).*?)<a href=\"(.*?)\">Info",
					"$1<a href=\"$3&dummy=$2\">Info");

			// if not on parent sites it does not make sense to get the image
			// link
			// since it will be brought again on details

			// if
			// (InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSearchType()
			// == Search.PARENT_SITE_SEARCH ){
			Matcher imageMatcher = imageLinkPattern.matcher(rsResponse);
			while (imageMatcher.find()) {
				String firstImageLink = imageMatcher.group(1);

				String finalImageLink = getImageLink(firstImageLink);

				if (firstImageLink != null) {
					// I will try to determin the refnum parameter to get the
					// year for the image
					// if the year will be before 1992 the image will not be
					// available
					int refnumPosition = finalImageLink.indexOf("refnum=");

					if (refnumPosition != -1) {
						String refnum = finalImageLink.substring(refnumPosition + "refnum=".length());
						// if there is another parameter in the link i will
						// remove it
						// in the string refnum i will have something like this:
						// DNxxxxyyyyyy where xxxx is the year
						if (refnum.indexOf("&") > 0) {
							refnum = refnum.substring(0, refnum.indexOf("&"));
						}
						String refnumYear = "";
						if (refnum.length()>6){
							refnumYear = refnum.substring(2, 6);
						}
						try {
							if (!refnumYear.equals("") && Integer.parseInt(refnumYear) < MIN_YEAR_FOR_IMAGE) {
								// the image will not be available
								rsResponse = rsResponse.replaceAll("<a.*?href=\"javascript:openimagewin\\('"
										+ firstImageLink.replace("?", "\\?") + "'\\).*?>\\s*View</a>", "NO IMAGE");
							} else {
								// the image should be available
								rsResponse = rsResponse.replaceAll("<a.*?href=\"javascript:openimagewin\\('"
										+ firstImageLink.replace("?", "\\?") + "'\\).*?>", "<a href=\"" + sTmp + finalImageLink + "\">");
							}
						} catch (Exception e) {
							e.printStackTrace();
							logger.error("The year is wrong ... pls check this problem");
							logger.info("Using default link that should get an image");
						}

					} else {
						// if I cannot determine the year this way I will use
						// the default link
						rsResponse = rsResponse.replaceAll("<a.*?href=\"javascript:openimagewin\\('" + firstImageLink.replace("?", "\\?")
								+ "'\\).*?>", "<a href=\"" + sTmp + finalImageLink + "\">");

					}
				} else {
					//rsResponse = rsResponse.replaceAll("<a.*?href=\"javascript:openimagewin\\('" + firstImageLink.replace("?", "\\?")
					//		+ "'\\).*?>\\s*View</a>", "NO IMAGE");
				}
			}
			// }

			if (!"".equals(linkPrev)) {
				rsResponse += "<br><a href=\"" + linkPrev + "\">Previous</a>";
			}

			if (!"".equals(linkNext)) {
				rsResponse += " <a href=\"" + linkNext + "\">Next</a>";
			}

			rsResponse = rsResponse.replaceFirst("<th class=\"ColumnHeader\"", "$0 width = \"1%\">" + SELECT_ALL_CHECKBOXES + "</th>$0");
			rsResponse = rsResponse.replaceAll("(?i)(?s)(?s)<tr><td class=\"FieldValue\"(.*?)</td>(.*?)<a href=\"(.*?)\">Info",
						"<tr><td class=\"FieldValue\"><input type=\"checkbox\" name=\"docLink\" value=\"$3\"></td><td class=\"FieldValue\"$1</td>$2<a href=\"$3\">Info");

//			rsResponse = addRememberDocLinks(rsResponse, "dummy=");
			rsResponse = CreateSaveToTSDFormHeader(URLConnectionReader.MULTIPLE_SAVE_TO_TSD_ACTION, "POST") + rsResponse
					+ CreateSaveToTSDFormEnd(SAVE_SELECTED_DOCUMENTS_BUTTON_LABEL, viParseID, -1);

			parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_ROWS, getLinkPrefix(TSConnectionURL.idGET),
					TSServer.REQUEST_SAVE_TO_TSD);

			if (linkNext.trim().length() != 0) {
				linkNext = "<a href=\"" + linkNext + "\">Next</a>";
			}

			Response.getParsedResponse().setNextLink(linkNext);

			break;
		case ID_DETAILS:
			String imgLink = null;

			Matcher detailsImageMatcher = detailsImageLinkPattern.matcher(rsResponse);
			if (detailsImageMatcher.find()) {
				imgLink = getImageLink(detailsImageMatcher.group(1));
			}

			iStart = rsResponse.indexOf(detailsStartToken);
			if (iStart >= 0) {
				iStart += detailsStartToken.length();

				iEnd = rsResponse.indexOf(detailsEndToken, iStart);

				if (iEnd >= 0) {
					rsResponse = rsResponse.substring(iStart, iEnd);
				}
			}

			String fileName = "none";

			String docTypeString = getDocTypeString(rsResponse);
			String bookPageType[] = getBookPageType(rsResponse);

			String instNo = getInstNo(rsResponse);
			fileName = instNo + ".html";

			if (!downloadingForSave) {
				rsResponse = rsResponse.replaceAll("<a .*?>", "");
				rsResponse = rsResponse.replaceAll("</a>", "");
			}

			String originalLink = sAction + "&" + "dummy=" + fileName.replace(".html", "") + "&" + Response.getQuerry();
			String sSave2TSDLink = getLinkPrefix(TSConnectionURL.idGET) + originalLink;

			String bookPageRef = "";

			Matcher selfReferenceMatcher = bookPageRefPattern.matcher(rsResponse);
			if (selfReferenceMatcher.find()) {
				bookPageRef = selfReferenceMatcher.group(1);

				int isReferenceIndex = rsResponse.indexOf("Doc Refers To");
				int position = rsResponse.indexOf(bookPageRef);

				while (position >= 0) {
					if (isReferenceIndex < position) {
						rsResponse = rsResponse.substring(0, position) + " " + rsResponse.substring(position + bookPageRef.length());
					}

					position = rsResponse.indexOf(bookPageRef, position + 1);
				}

				// rsResponse = rsResponse.replace( bookPageRef, " " );
			}

			iStart = rsResponse.indexOf("View Image</a>");
			if (iStart >= 0 && imgLink == null) {
				iStart = rsResponse.indexOf("<a href=\"");
				iStart += 9;
				imgLink = rsResponse.substring(iStart, rsResponse.indexOf("\"", iStart));
			} else if (imgLink != null) {
				// imgLink = getFinalImageLink(imgLink);
				// if(imgLink != null){
				// imgLink = sTmp + imgLink;
				// }
			}

			boolean fakeImageLink = true;

			if (imgLink != null) {
				fakeImageLink = fakeImage(imgLink);

				if (!fakeImageLink) {
					Response.getParsedResponse().addImageLink(new ImageLinkInPage(sTmp + imgLink, fileName.replace(".html", "") + ".pdf"));
				}
			}

			if (!downloadingForSave) {
				if (imgLink != null && !fakeImageLink) {
					rsResponse += "<a href=\"" + sTmp + imgLink + "\">View Image</a>";
				}

				if (FileAlreadyExist(fileName) ) {
					rsResponse += CreateFileAlreadyInTSD();
				} else {
					rsResponse = addSaveToTsdButton(rsResponse, sSave2TSDLink, viParseID);
					if (imgLink != null) {
						initialResponse += "<a href=\"" + sTmp + imgLink + "\">View Image</a>";
					}
					mSearch.addInMemoryDoc(sSave2TSDLink, initialResponse);
				}
				rsResponse = rsResponse.replace("Click to Display Search Results for Refers To", "");
				Response.getParsedResponse().setPageLink(new LinkInPage(sSave2TSDLink, originalLink, TSServer.REQUEST_SAVE_TO_TSD));
				if (Response.getQuerry().indexOf("lts") >= 0) {
					parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_DETAILS);
				} else {
					parser.Parse(Response.getParsedResponse(), rsResponse, Parser.NO_PARSE);
				}
			} else {

				rsResponse = rsResponse.replaceAll("(?si)<a .*?>", "");
				rsResponse = rsResponse.replaceAll("(?i)</a>", "");
				rsResponse = rsResponse.replaceAll("(?i)View Image", "");

				if (imgLink != null && !fakeImageLink) {
					if (imgLink.indexOf(sTmp) >= 0) {
						rsResponse += "<a href=\"" + imgLink + "\">View Image</a>";
					} else {
						rsResponse += "<a href=\"" + sTmp + imgLink + "\">View Image</a>";
					}
				}

				msSaveToTSDFileName = fileName;
				Response.getParsedResponse().setFileName(getServerTypeDirectory() + msSaveToTSDFileName);
				msSaveToTSDResponce = rsResponse + CreateFileAlreadyInTSD();

				// get crossreferences, but only if we have a mortgage or lien
				if (DocumentTypes.isMortgageDocType(docTypeString, searchId) || DocumentTypes.isLienDocType(docTypeString, searchId) /*
																																		 * &&
																																		 * isReleasedDoc(initialResponse)
																																		 */) {
					// set book, page, typeti
					InstanceManager.getManager().getCurrentInstance(searchId).setBookPageType(bookPageType[0], bookPageType[1],
							bookPageType[2]);
					// get cross references
					HashSet<String> links = new HashSet<String>();
					// <a href="JavaScript:void
					// window.open('S5_A_DataLKUP.jsp?booktype5_A=L&book5_A=216&page5_A=877&hidPageName5_A=DocInfo','RefSearch','scrollbars=yes,status=yes,resizable=yes,toolbar=yes');"
					// >
					Pattern p = Pattern.compile("'(S5_A_DataLKUP[^']+)'");
					Matcher m = p.matcher(initialResponse);
					String references = "";
					
					if (m.find()) {
						String link = m.group(1);
						link = "http://www.landrecords.jcc.ky.gov/records/" + link;
						System.err.println(link);
						// get crossreferences
						String crossRefPage = getPage(link);
						crossRefPage = removeSelfReferencedLinks(crossRefPage); // fix
						// for
						// bug
						// #1009

						// <a class="FieldValue"
						// href="DocInfo.jsp?cntrlnum=199002210058&hidRecPerPage=5&hidCurrentRec=1&hidTotalNumRecs=1&moreAvailableRecs=No&hidPageName=S5_A_Result&backPage=S5_A_Results.jsp"
						// tabindex=-1>Info</a></td>
						Pattern docInfoPattern = Pattern.compile("href=\"DocInfo\\.jsp\\?([^\"]+)\"");
						Matcher docInfoMatcher = docInfoPattern.matcher(crossRefPage);
						
						
						Pattern docNrPattern = Pattern.compile("href=\"JCCOGetImage.jsp\\?refnum=(\\w+)");
						Matcher docNrMatcher = docNrPattern.matcher(crossRefPage);
						while (docNrMatcher.find()) {
							if(!references.contains(docNrMatcher.group(1))) {
								references += docNrMatcher.group(1) + " ";
							}
						}
								
						while (docInfoMatcher.find()) {
							String crossRefLink = docInfoMatcher.group(1);
							crossRefLink = "<a HREF='" + CreatePartialLink(TSConnectionURL.idGET) + "/records/DocInfo.jsp&"
									+ docInfoMatcher.group(1) + "'></a>";
							if (!links.contains(crossRefLink)) {
								links.add(crossRefLink);
							}
						}
					}
					// add crossreferences to the current document
					for (Iterator<String> it = links.iterator(); it.hasNext();) {
						rsResponse += it.next() + "<br>";
					}
					
					
					rsResponse = rsResponse.replaceAll("(?ism)(Doc Refers To:.*?)</td>", "$1" + references + "</td>");
				}
				
				rsResponse = rsResponse.replace("Click to Display Search Results for Refers To", "");
				
				// put crossreferences
				String[] bpt = InstanceManager.getManager().getCurrentInstance(searchId).getBookPageType();
				if (bpt != null) {
					System.err.println("Adding crossreferences !");

					ParsedResponse pr = Response.getParsedResponse();
					Vector<CrossRefSet> crossReferences = new Vector<CrossRefSet>();

					CrossRefSet crs = new CrossRefSet();
					crs.setAtribute("Book_Page_Type", bpt[2]);
					crs.setAtribute("Book", bpt[0]);
					crs.setAtribute("Page", bpt[1]);
					crossReferences.add(crs);

					pr.setCrossRefSet(crossReferences);
				}

				parser.Parse(Response.getParsedResponse(), rsResponse, Parser.PAGE_DETAILS, getLinkPrefix(TSConnectionURL.idGET),
						TSServer.REQUEST_SAVE_TO_TSD);
			}
			break;

		case ID_GET_LINK:
		case ID_SAVE_TO_TSD:

			if (viParseID == ID_GET_LINK) {
				if (sAction.indexOf("DocInfo.jsp") >= 0) {
					ParseResponse(sAction, Response, ID_DETAILS);
				} else {
					int newId = ID_SEARCH_BY_NAME;
					if (sAction.indexOf("S3") != -1) {
						newId = ID_SEARCH_BY_BOOK_AND_PAGE;
					} else if (sAction.indexOf("S4") != -1) {
						newId = ID_SEARCH_BY_INSTRUMENT_NO;
					} else if (sAction.indexOf("S5") != -1) {
						newId = ID_SEARCH_BY_BOOK_PAGE_ADV;
					} else if (sAction.indexOf("S1") != -1) {
						newId = ID_SEARCH_BY_PROP_NO;
					}
					ParseResponse(sAction, Response, newId);
				}
			} else if (viParseID == ID_SAVE_TO_TSD) {
				downloadingForSave = true;
				ParseResponse(sAction, Response, ID_DETAILS);
				downloadingForSave = false;
			}
			break;
		}

		logger.info(sAction);
	}

	/**
	 * check if document is released
	 * 
	 * @param doc
	 * @return
	 */
	/*private boolean isReleasedDoc(String doc) {
		// check if doc is released
		// <th class="FieldName" style="width: 69px" align="left"
		// valign="middle" >
		// Released:</th>
		// <td class="FieldValue" style="width: 35px" align="left"
		// valign="middle" >
		// R</td>
		int istart, iend;
		istart = doc.indexOf("Released:</th>");
		if (istart == -1) {
			return false;
		}
		istart += "Released:</th>".length() + 1;
		istart = doc.indexOf("<td", istart);
		if (istart == -1) {
			return false;
		}
		istart = doc.indexOf(">", istart);
		if (istart == -1) {
			return false;
		}
		istart += ">".length();
		iend = doc.indexOf("</td>", istart);
		if (iend == -1) {
			return false;
		}
		doc = doc.substring(istart, iend).replaceAll("[\\s\n]", "");
		return "R".equals(doc);
	}*/

	
	protected void setModulesForGoBackOneLevelSearch(TSServerInfo serverInfo) {

	   	ConfigurableNameIterator nameIterator = null;
		DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId,true,true).getValidator(); 
		DocsValidator addressHighPassValidator = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d).getValidator();
		
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();	
		List<TSServerInfoModule> modules = new ArrayList<TSServerInfoModule>();
	    TSServerInfoModule module;	
	    GBManager gbm=(GBManager)sa.getObjectAtribute(SearchAttributes.GB_MANAGER_OBJECT);

  for (String id : gbm.getGbTransfers()) {
			  		   	    	 
	         module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
	         module.setIndexInGB(id);
	         module.setTypeSearchGB("grantor");
	         module.clearSaKeys();
		     module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
		     module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
		     String date=gbm.getDateForSearch(id,"MMddyyyy", searchId);
		     if (date!=null) 
		    	 module.getFunction(3).forceValue(date);
		     nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;f;", "L;m;"} );
		 	 module.addIterator(nameIterator);
		 	 module.addValidator( defaultLegalValidator );
			 module.addValidator( addressHighPassValidator );
	         module.addValidator( pinValidator );
             module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
		  	 module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());
		
			 modules.add(module);
		    
		     
		     if(gbm.getNamesForBrokenChain(id, searchId).size()>0){
			     module = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			     module.setIndexInGB(id);
			     module.setTypeSearchGB("grantee");
			     module.clearSaKeys();
				 module.setSaObjKey(SearchAttributes.GB_MANAGER_OBJECT);
				 module.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
				 date=gbm.getDateForSearchBrokenChain(id,"MMddyyyy", searchId);
				 if (date!=null) 
					 module.getFunction(3).forceValue(date);
				 nameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(module, searchId, new String[]{"L;f;", "L;m;"} );
				 module.addIterator(nameIterator);
				 module.addValidator( defaultLegalValidator );
				 module.addValidator( addressHighPassValidator );
				 module.addValidator( pinValidator );
			     module.addValidator(NameFilterFactory.getDefaultTransferNameFilter(searchId, 0.90d, module).getValidator());
				 module.addValidator(DateFilterFactory.getDateFilterForGoBack(SearchAttributes.GB_MANAGER_OBJECT, searchId, module).getValidator());
			
	
			 modules.add(module);
			 
		     }

	    }	 
  serverInfo.setModulesForGoBackOneLevelSearch(modules);	
	}

	protected void setModulesForAutoSearch(TSServerInfo serverInfo) {
		List<TSServerInfoModule> l = new ArrayList<TSServerInfoModule>();
		TSServerInfoModule m;
		SearchAttributes sa = getSearchAttributes();
		//String[] mortgageModules = new String[] { "1", "2" };
		String[] liensModules = new String[] { "3", "6" };
		String[] somePlatsTypes = new String[] { "PLT", "CM" };
		String[] someRestrictionTypes = new String[]  { "DOR", "DRN", "LUR" }; // B 3926

		DocsValidator defaultLegalValidator = LegalFilterFactory.getDefaultLegalFilter(searchId).getValidator();
		DocsValidator pinValidator = PINFilterFactory.getPinFilter(searchId,true,true).getValidator(); 
		DocsValidator lastTransferDateValidator = (new LastTransferDateFilter(searchId)).getValidator();
		DocsValidator recordedDateValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId).getValidator();
		DocsValidator buyerDoctypeValidator = DoctypeFilterFactory.getDoctypeFilter(
					searchId, 0.8, new String[] {"LIEN","COURT"}, FilterResponse.STRATEGY_TYPE_HIGH_PASS).getValidator();
		DocsValidator addressHighPassValidator = AddressFilterFactory.getAddressHighPassFilter(searchId, 0.8d).getValidator();
		boolean validateWithDates = applyDateFilter();
		
		// p1.1, 1.2 search by owner
		ConfigurableNameIterator ownerNameIterator = null;
		if (hasOwner()) {
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
			m.addExtraInformation(
					TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			
			m.setSaKey(3, SearchAttributes.FROMDATE_MM_DD_YYYY);
			m.setSaKey(4, SearchAttributes.TODATE_MM_DD_YYYY);
			
			m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			
			m.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
		    m.addFilter( NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m) );
		    m.addValidator( defaultLegalValidator );
			m.addValidator( addressHighPassValidator );
	        m.addValidator( pinValidator );
	        m.addValidator( recordedDateNameValidator );
	        m.addValidator( lastTransferDateValidator );
	        addFilterForUpdate(m, false);
	        
	        m.addCrossRefValidator( defaultLegalValidator );
			m.addCrossRefValidator( addressHighPassValidator );
	        m.addCrossRefValidator( pinValidator );
	        m.addCrossRefValidator( recordedDateNameValidator );
		    ownerNameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L;f;", "L;m;"});
			m.addIterator( ownerNameIterator);
			l.add(m);
		}

		// p2 search for buyer
		if (hasBuyer()) {
			// p2.2, p2.4 search for buyer - liens
			for (int i = 0; i < liensModules.length; i++) {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
				m.addExtraInformation(
						TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_NAME_BUYERS_LIEN);
				m.setSaObjKey(SearchAttributes.BUYER_OBJECT);
				m.setSaKey(3, SearchAttributes.FROMDATE_MM_DD_YYYY);
				m.setSaKey(4, SearchAttributes.TODATE_MM_DD_YYYY);
				m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
				m.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
				m.getFunction(6).setParamValue(liensModules[i]);
	
			    m.addFilter( NameFilterFactory.getNameFilterIgnoreMiddleOnEmpty(SearchAttributes.BUYER_OBJECT, searchId, m));			    
			    m.addValidator(buyerDoctypeValidator);
			    m.addValidator( defaultLegalValidator );
				m.addValidator( addressHighPassValidator );
		        m.addValidator( pinValidator );
		        m.addValidator( recordedDateNameValidator );
		        m.addValidator( lastTransferDateValidator );
		        addFilterForUpdate(m, true);
		        m.addCrossRefValidator( defaultLegalValidator );
				m.addCrossRefValidator( addressHighPassValidator );
		        m.addCrossRefValidator( pinValidator );
		        m.addCrossRefValidator( recordedDateNameValidator );
				m.addIterator( (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L;f;", "L;m;"}));
			    l.add(m);
			}
		}

		// p4 + p5 - search for some plats surname = subdivision
		if (!StringUtils.isEmpty(sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME)) ) {
			for (int i = 0; i < somePlatsTypes.length; i++) {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.addExtraInformation(
						TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_PLAT);
				m.getFunction(0).setSaKey("");
				m.getFunction(0).setParamValue(getSearchAttribute(SearchAttributes.LD_SUBDIV_NAME));
				m.getFunction(1).setParamValue("");
				m.getFunction(1).setSaKey("");
				m.setSaKey(3, (validateWithDates) ? SearchAttributes.FROMDATE : SearchAttributes.START_HISTORY_DATE);
				m.getFunction(5).setParamValue(somePlatsTypes[i]);
				m.addFilterForNextType(FilterResponse.TYPE_SUBDIV_NAME_FOR_NEXT);
				m.addFilter(NameFilterFactory
						.getDefaultNameFilterForSubdivision(searchId));
				
				l.add(m);
			}
			// B 3926
			for (int i = 0; i < someRestrictionTypes.length; i++) {
				m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
				m.addExtraInformation(
						TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
						TSServerInfoConstants.VALUE_PARAM_NAME_SUBDIVISION_RESTRICTION);
				m.getFunction(0).setSaKey("");
				m.getFunction(0).setParamValue(getSearchAttribute(SearchAttributes.LD_SUBDIV_NAME));
				m.getFunction(1).setParamValue("");
				m.getFunction(1).setSaKey("");
				m.getFunction(5).setParamValue(someRestrictionTypes[i]);
				m.addFilterForNextType(FilterResponse.TYPE_SUBDIV_NAME_FOR_NEXT);
				//m.addFilterType(FilterResponse.TYPE_REGISTER_NAME);
				m.addFilter(NameFilterFactory
						.getDefaultNameFilterForSubdivision(searchId));

				l.add(m);
			}
		} else {
			printSubdivisionException();
		}

		// p6 - restrictions, easements and agreements
		// - the results of this module are included in the name search results
		/*
		 * if(!emptyOwner){ m = new
		 * TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
		 * m.setSaObjKey(SearchAttributes.OWNER_OBJECT); // for filter
		 * m.getFunction(0).setSaKey(""); m.getFunction(1).setSaKey("");
		 * m.getFunction(0).setParamValue
		 * (searchAttributes.getAtribute(SearchAttributes.OWNER_LNAME));
		 * m.getFunction(1).setParamValue
		 * (searchAttributes.getAtribute(SearchAttributes.OWNER_FNAME));
		 * m.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
		 * m.addFilterType(FilterResponse.TYPE_REGISTER_NAME);
		 * m.addValidatorType(DocsValidator.TYPE_SUBDIVISION_AVAILABLE);
		 * m.setValidatorType(DocsValidator.TYPE_KYJEFFERSONRO_REA);
		 * m.addCrossRefValidatorType(DocsValidator.TYPE_REGISTER_SUBDIV_LOT);
		 * l.add(m); }
		 */

		// p7 - search for transfers from AO - by book page stored in instrument
		// list
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		m.addExtraInformation(
				TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
				TSServerInfoConstants.VALUE_PARAM_LIST_AO_BP_TRANSFERS);
		m.setSaObjKey(SearchAttributes.INSTR_LIST);
		m.setIteratorType(ModuleStatesIterator.TYPE_BOOK_PAGE_LIST);
		m.addFilterForNextType(FilterResponse.TYPE_BOOK_PAGE_FOR_NEXT_CROSREF);
		m.getFunction(0).setSaKey("");
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_FAKE);
		m.getFunction(1).setSaKey("");
		m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_FAKE);
		m.getFunction(2).setParamValue("D");
		m.addCrossRefValidator( defaultLegalValidator );
		m.addCrossRefValidator( addressHighPassValidator );
		m.addCrossRefValidator( recordedDateValidator );
        if (validateWithDates) {
        	m.addValidator(recordedDateValidator);
		}
		l.add(m);

		// p8 - search for plats by Book_Page taken from AO, 3rd line of legal
		// description
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		m.addExtraInformation(
				TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
				TSServerInfoConstants.VALUE_PARAM_LIST_AO_BP_PLAT);
		m.addFilterForNextType(FilterResponse.TYPE_BOOK_PAGE_FOR_NEXT_CROSREF);
		m.setSaObjKey(SearchAttributes.LD_BOOKPAGE);
		m.setIteratorType(ModuleStatesIterator.TYPE_BOOK_PAGE_LIST_SEARCH);
		m.getFunction(0).setSaKey("");
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
		m.getFunction(1).setSaKey("");
		m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
		m.getFunction(2).setParamValue("P");
		m.addCrossRefValidator( defaultLegalValidator );
		m.addCrossRefValidator( addressHighPassValidator );
		m.addCrossRefValidator( recordedDateValidator );
        if (validateWithDates) {
        	m.addValidator(recordedDateValidator);
		}
		l.add(m);

		// p9 - search for transfer - specifications missing

		// p10 - search for cross-references: not needed anymore

		// p11 - search by book/page

		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));		
		m.getFunction(0).setSaKey("");
		m.getFunction(1).setSaKey("");
		m.setIteratorType(ModuleStatesIterator.TYPE_DEED_BOOK_PAGE_ITERATOR);
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
		m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
		m.getFunction(2).setParamValue("D");
		m.addFilterForNextType(FilterResponse.TYPE_BOOK_PAGE_FOR_NEXT_CROSREF);
		m.addCrossRefValidator( defaultLegalValidator );
		m.addCrossRefValidator( addressHighPassValidator );
		m.addCrossRefValidator( recordedDateValidator );
        if (validateWithDates) {
        	m.addValidator(recordedDateValidator);
		}
		l.add(m);
		
		m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));
		m.addExtraInformation(
				TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
				TSServerInfoConstants.VALUE_PARAM_OCR_SEARCH_BP);
		m.clearSaKeys();
		m.setIteratorType(ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER);
		m.getFunction(0).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH);
		m.getFunction(1).setIteratorType(FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH);
		m.getFunction(2).setParamValue("D");
		m.addFilterForNextType(FilterResponse.TYPE_BOOK_PAGE_FOR_NEXT_CROSREF);
		m.addCrossRefValidator( defaultLegalValidator );
		m.addCrossRefValidator( addressHighPassValidator );
        m.addCrossRefValidator( pinValidator );
        m.addCrossRefValidator( recordedDateValidator );
        if (validateWithDates) {
			m.addValidator(recordedDateValidator);
		}
		l.add(m);
		
		{
			//search by owner boostrapped from last transfer and ocr
			m = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.NAME_MODULE_IDX));
			DocsValidator recordedDateNameValidator = BetweenDatesFilterResponse.getDefaultIntervalFilter(searchId, m).getValidator();
			m.addExtraInformation(
					TSServerInfoConstants.EXTRA_PARAM_MODULE_DESCRIPTION,
					TSServerInfoConstants.VALUE_PARAM_NAME_OWNERS);
			m.setSaObjKey(SearchAttributes.OWNER_OBJECT);
			m.setSaKey(3, SearchAttributes.FROMDATE_MM_DD_YYYY);
			m.setSaKey(4, SearchAttributes.TODATE_MM_DD_YYYY);
			m.setIteratorType(3, FunctionStatesIterator.ITERATOR_TYPE_FROM_DATE);
			m.addFilterForNextType(FilterResponse.TYPE_REGISTER_NAME_FOR_NEXT);
		    m.addFilter( NameFilterFactory.getDefaultNameFilter(SearchAttributes.OWNER_OBJECT, searchId, m) );
	        m.addValidator( defaultLegalValidator );
			m.addValidator( addressHighPassValidator );
	        m.addValidator( pinValidator );
	        m.addValidator( recordedDateNameValidator );
	        m.addValidator( lastTransferDateValidator );
	        m.addCrossRefValidator( defaultLegalValidator );
			m.addCrossRefValidator( addressHighPassValidator );
	        m.addCrossRefValidator( pinValidator );
	        m.addCrossRefValidator( recordedDateNameValidator );
	        ArrayList<NameI> searchedNames = (ownerNameIterator != null)?ownerNameIterator.getSearchedNames():new ArrayList<NameI>();
		    ownerNameIterator =  (ConfigurableNameIterator)ModuleStatesIteratorFactory.getConfigurableNameIterator(m, searchId, new String[]{"L;f;", "L;m;"});
		    // get your values at runtime
		    ownerNameIterator.setInitAgain(true);
		    ownerNameIterator.setSearchedNames(searchedNames);
			m.addIterator( ownerNameIterator);
			l.add(m);
		}
		

		serverInfo.setModulesForAutoSearch(l);

	}

	public TSServerInfoModule getSearchByBookPageModule(TSServerInfo serverInfo, String book, String page, String instrumentNo) {
		// we don't need the instrument number
		if ("".equals(book) && "".equals(page)) {
			return null;
		}

		TSServerInfoModule retVal = new TSServerInfoModule(serverInfo.getModule(TSServerInfo.BOOK_AND_PAGE_MODULE_IDX));

		retVal.getFunction(0).setSaKey("");
		retVal.getFunction(1).setSaKey("");

		retVal.getFunction(0).setParamValue(book);
		retVal.getFunction(1).setParamValue(page);
		retVal.getFunction(2).setParamValue("D");
		retVal.getFunction(3).setParamValue("Search");
		retVal.getFunction(4).setParamValue("S3Search");

		return retVal;
	}

	private String removeSelfReferencedLinks(String page) {
		StringBuffer retVal = new StringBuffer();
		int istart, iend;

		// extracts the table with referenced documents links
		istart = page.indexOf("<table width=\"100%\">");
		if (istart == -1) {
			return retVal.toString();
		}
		istart = page.indexOf("<tr>", istart);
		if (istart == -1) {
			return retVal.toString();
		}
		istart += "<tr>".length();
		istart = page.indexOf("<tr>", istart);
		if (istart == -1) {
			return retVal.toString();
		}
		iend = page.indexOf("</table>", istart);
		if (iend == -1) {
			return retVal.toString();
		}
		if (iend < istart) {
			return retVal.toString();
		}
		page = page.substring(istart, iend);

		// builds an array with all the rows from links table
		String[] rows = page.split("</tr>");
		int len = rows.length;
		if (len > 0) {
			// extracts the book, page and doc type abrev for the reference
			// document - it's enough to extract it from the first row, because
			// it's the same on all rows
			String book_page = rows[0].replaceFirst("(?s).*href=\"DocInfo\\.jsp\\?.*?<td class=\"FieldValue\" >([^<]+)</td>.*", "$1");
			if (!book_page.equals(rows[0])) {
				String ref;
				for (int i = 0; i < len; i++) {
					if (rows[i].contains("class=\"FieldValue\"")) {
						// extracts the book, page and doc type abrev for the
						// referenced document
						ref = rows[i].replaceFirst("(?s).*<td class=\"FieldValue\" >(.+)</td>.*<td class=\"FieldValue\" > &nbsp.*", "$1");
						// keeps only the rows that don't refers to themselves
						if (!ref.equals(rows[i]) && !ref.equals(book_page)) {
							retVal.append(rows[i]);
						}
					}
				}
			}
		}

		return retVal.toString();
	}

	private String getTable(String tableStart, String tableEnd, String response) {
		String table = response;

		int iStart = response.indexOf(tableStart);
		if (iStart < 0) {
			return table;
		}

		iStart += tableStart.length();
		int iEnd = response.indexOf(tableEnd, iStart);
		if (iEnd < 0) {
			return table;
		}

		iEnd += tableEnd.length();
		table = response.substring(iStart, iEnd);

		return table;
	}

	/**
	 * get a page from web
	 * 
	 * @param link
	 *            link of the page
	 * @param query
	 *            querry for the page
	 * @return the page
	 */
	private String getPage(String link) {
		HTTPSiteInterface httpSite = (HTTPSiteInterface) HTTPSiteManager.pairHTTPSiteForTSServer("KYJeffersonRO", searchId, miServerID);
		HTTPRequest req = new HTTPRequest(link);
		req.setMethod(HTTPRequest.GET);
		req.noRedirects = false;
		HTTPResponse res = httpSite.process(req);
		String strdata = res.getResponseAsString();
		return strdata;
	}

	@Override
	protected void setCertificationDate() {
		try {
			if (CertificationDateManager.isCertificationDateInCache(dataSite)){
				String date = CertificationDateManager.getCertificationDateFromCache(dataSite);
				getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
			} else{
				HTTPSite site = HTTPManager.getSite(getCurrentServerName(), searchId, miServerID);
				HTTPRequest reqP = new HTTPRequest(dataSite.getLink() + (dataSite.getLink().endsWith("/") ? "" : "/") + "userguide.html");
		    	reqP.setMethod(HTTPRequest.GET);
		    	String page = "";
	        	
		    	try {
	        		HTTPResponse resP = site.process(reqP);
	        		page = resP.getResponseAsString();
	        	} finally {
	        		HTTPManager.releaseSite(site);
				} 
				
				if (StringUtils.isNotEmpty(page)){
					Matcher certDateMatcher = certDatePattern.matcher(page);
					
					if(certDateMatcher.find()) {
						String date = DateFormatUtils.format(Calendar.getInstance().getTime(), "MM/dd/yyyy");
						
						CertificationDateManager.cacheCertificationDate(dataSite, date);
						getSearch().getSa().updateCertificationDateObject(dataSite, Util.dateParser3(date));
					} else {
						CertificationDateManager.getLogger().error("Cannot parse certification date on " + getDataSite().getName() + " because pattern not found");
					}
				} else {
					CertificationDateManager.getLogger().error("Cannot parse certification date on " + getDataSite().getName() + " because html response is empty");
				}
			}
        } catch (Exception e) {
        	CertificationDateManager.getLogger().error("Error setting certification date on " + getDataSite().getName(), e);
        }
	}
}