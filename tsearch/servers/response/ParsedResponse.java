package ro.cst.tsearch.servers.response;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;
import static ro.cst.tsearch.utils.StringUtils.formatList;
import static ro.cst.tsearch.utils.StringUtils.isEmpty;
import static ro.cst.tsearch.utils.StringUtils.join;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.bean.SearchLogRow;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.types.TSInterface;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.Fields;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;

public class ParsedResponse extends ParsedResponseData implements Serializable, Cloneable {

	private static final String CITYTAX_DOC_TYPE = "CITYTAX";

	private static final String CNTYTAX_DOC_TYPE = "CNTYTAX";

	static final long serialVersionUID = 10000000;

	private static final Category logger = Logger.getLogger(ParsedResponse.class);
	public static final int UNKNOW_RESULTS_COUNT = -1;

	private boolean mbSolved = false;

	//atributes
	public static final String DASL_RECORD = "DASL_RECORD";
	public static final String REAL_PI = "REAL_PI";
	public static final String SSF_CONTENT = "SSF_CONTENT";
	public static final String DASL_TYPE = "DASL_TYPE";
	public static final String DASL_DOCNO = "DASL_DOCNO";
	public static final String DT_RECORD = "DT_RECORD";

	public static final String SKIP_BOOTSTRAP = "SKIP_BOOTSTRAP";
	
	public static final String ASK_RECORD = "ASK_RECORD";
	public static final String ASK_TYPE = "ASK_TYPE";
	public static final String ASK_FNAME = "ASK_FNAME";
	public static final String ASK_IMGEXT = "ASK_IMGEXT";

	public static final String SERVER_ROW_RESPONSE = "SERVER_ROW_RESPONSE";
	public static final String SERVER_NAVIGATION_LINK = "SERVER_NAVIGATION_LINK";
	public static final String SERVER_RECURSIVE_ANALYZE = "SERVER_RECURSIVE_ANALYZE";
	public static final String SERVER_RESPONSE_OBJECT = "SERVER_RESPONSE_OBJECT";

	/**
	 * if set "true" shows only the message error set on this ParsedResponse without ServletServerComm.INTERNAL_ERR_MSG before it
	 */
	public static final String SHOW_ONLY_ERROR_MESSAGE = "SHOW_ONLY_ERROR_MESSAGE";  
	
	private transient Hashtable<String, Object> attributes = new Hashtable<String, Object>();
	private transient long searchId;

	private Vector<ParsedResponse> mResultRow = new Vector<>();
	private Vector mSkippedResultRow = new Vector();
	private Vector imageLinks = new Vector();

	private LinkInPage pageLink = null;

	private transient String msResponse = "";
	private String msError = null;
	private String msWarning = null;
	private String nextLink = "";
	private String header = "";
	private String footer = "";
	private String fileName;
	private boolean isParentSite = false;

	private int resultsCount = 0;
	private int resultsSkippedCount = 0;
	private int initialResultsCount = 0;

	private boolean useDocumentForSearchLogRow = false;
	// //////////////////////////////////////////////////////////////

	private transient TSInterface tsInterface;

	public ParsedResponse() {
		// TODO Auto-generated constructor stub
	}

	public void setFileName(String s) {
		fileName = s;
	}

	public String getFileName() {
		return fileName;
	}

	public void setResponse(String response) {
		msResponse = response;
		mResultRow = new Vector();
		resultsCount = 1;
		// logger.debug("setResponse : resultsCount = " + resultsCount);
	}

	public void setOnlyResponse(String s) {
		msResponse = s;
	}

	public void setOnlyResultRows(Vector rows) {
		if(rows == null) {
			mResultRow = new Vector<>();
		} else {
			mResultRow = new Vector(rows);
		}
	}

	public void addOneResultRowOnly(ParsedResponse pr) {
		mResultRow.add(pr);
	}

	public void setResultRows(Vector rows) {
		setOnlyResultRows(rows);
		resultsCount = mResultRow.size();
		// logger.debug("setResultRows : resultsCount = " + resultsCount);
	}

	public void setSkippedRows(Vector rows) {
		// setOnlyResultRows(rows) ;
		mSkippedResultRow = new Vector(rows);
		resultsSkippedCount = mSkippedResultRow.size();
		// logger.debug("setSkippedResultRows : skippedResultsCount = " +
		// resultsSkippedCount);
	}

	public String getResponse() {
		return msResponse;
	}

	public Vector getResultRowsAsStrings() {
		Vector rez = new Vector();
		for (Iterator iter = mResultRow.iterator(); iter.hasNext();) {
			ParsedResponse pr = (ParsedResponse) iter.next();
			rez.add(pr.getResponse());
		}
		// nu se mai afiseaza si rez care au picat la validare - numai cele cu
		// scor maxim ATS - doar daca nu am nici un rez

		if (mResultRow.size() == 0) {
			for (Iterator iter = mSkippedResultRow.iterator(); iter.hasNext();) {
				ParsedResponse pr = (ParsedResponse) iter.next();
				rez.add(pr.getResponse());
			}
		}
		return rez;
	}

	public Vector<ParsedResponse> getResultRows() {
		return new Vector<ParsedResponse>(mResultRow);
	}

	public Vector getSkippedResultRows() {
		return new Vector(mSkippedResultRow);
	}

	public String getFooter() {
		return footer;
	}

	public String getHeader() {
		return header;
	}

	public void setFooter(String footer) {
		this.footer = footer;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public boolean isMultiple() {
		return (resultsCount > 1);
	}

	/**
	 * Returns the none.
	 * 
	 * @return boolean
	 */
	public boolean isNone() {
		return (resultsCount == 0);
	}

	/**
	 * Returns the unique.
	 * 
	 * @return boolean
	 */
	public boolean isUnique() {
		return (resultsCount == 1);
	}

	/**
	 * Returns the mlResultsCount.
	 * 
	 * @return long
	 */
	public int getResultsCount() {
		return resultsCount;
	}

	// ////////////////////////////////////////////////

	public void setSolved(boolean solved) {
		mbSolved = solved;
	}

	public boolean isSolved() {
		return mbSolved;
	}

	/**
	 * Returns the error.
	 * 
	 * @return String
	 */
	public String getError() {
		return msError;
	}

	/**
	 * Sets the error.
	 * 
	 * @param error
	 *            The error to set
	 */
	public void setError(String sError) {
		msError = sError;
	}

	/**
	 * clear the error
	 */
	public void clearError() {
		msError = null;
	}

	/**
	 * Returns the error.
	 * 
	 * @return boolean
	 */
	public boolean isError() {
		return msError != null;
	}

	// ////////////////////////////////////////////////////

	/**
	 * Returns the oIS.
	 * 
	 * @return OtherInformationSet
	 */
	/*
	 * public OtherInformationSet getOtherInformationSet() { return mOIS; }
	 */

	/**
	 * Returns the pAS.
	 * 
	 * @return PropertyAppraisalSet
	 */
	public PropertyAppraisalSet getPropertyAppraisalSet() {
		return mPAS;
	}

	/**
	 * Returns the pIS.
	 * 
	 * @return PropertyIdentificationSet
	 */
	/*
	 * public PropertyIdentificationSet getPropertyIdentificationSet() { return
	 * mPIS; }
	 */

	public Vector getPropertyIdentificationSet() {
		return mPIS;
	}

	public Vector getPartyNameSet() {
		return mPNS;
	}

	public int getPropertyIdentificationSetCount() {
		return mPIS.size();
	}

	public PropertyIdentificationSet getPropertyIdentificationSet(int index) {
		if (index >= mPIS.size())
			return new PropertyIdentificationSet();

		return (PropertyIdentificationSet) mPIS.get(index);
	}

	public void addPropertyIdentificationSet(PropertyIdentificationSet obj) {
		mPIS.add(obj);
	}

	/**
	 * 
	 * @return
	 * 
	 *         CourtDocumentIdentificationSet
	 */

	public Vector getCourtDocumentIdentificationSet() {
		return mCDIS;
	}

	public int getCourtDocumentIdentificationSetCount() {
		return mCDIS.size();
	}

	public CourtDocumentIdentificationSet getCourtDocumentIdentificationSet(int index) {
		return (CourtDocumentIdentificationSet) mCDIS.get(index);
	}

	public void addCourtDocumentIdentificationSet(CourtDocumentIdentificationSet obj) {
		mCDIS.add(obj);
	}

	/**
	 * Returns the sDS.
	 * 
	 * @return SaleDataSet
	 */
	public int getSaleDataSetsCount() {
		return mSDS.size();
	}

	public SaleDataSet getSaleDataSet(int index) {
		return (SaleDataSet) mSDS.get(index);
	}

	public Vector getSaleDataSet() {
		return mSDS;
	}

	public void addSaleDataSet(SaleDataSet obj) {
		mSDS.add(obj);
	}

	public Vector getGrantorNameSet() {
		return mGrantor;
	}

	public int getGrantorNameSetCount() {
		return mGrantor.size();
	}

	public NameSet getGrantorNameSet(int index) {
		return (NameSet) mGrantor.get(index);
	}

	public void setGrantorNameSet(Vector newGrantor) {
		mGrantor = newGrantor;
	}

	public Vector getGranteeNameSet() {
		return mGrantee;
	}

	public int getGranteeNameSetCount() {
		return mGrantee.size();
	}

	public NameSet getGranteeNameSet(int index) {
		return (NameSet) mGrantee.get(index);
	}

	public void setGranteeNameSet(Vector newGrantee) {
		mGrantee = newGrantee;
	}

	public Vector getBoardNameSet() {
		return mBMS;
	}

	public int getBoardNameSetCount() {
		return mBMS.size();
	}

	public BoardNameSet getBoardNameSet(int index) {
		return (BoardNameSet) mBMS.get(index);
	}

	public void setBoardNameSet(Vector newBMS) {
		mBMS = newBMS;
	}

	public int getTaxHistorySetsCount() {
		return mTHS.size();
	}

	public TaxHistorySet getTaxHistorySet(int index) {
		if (index >= mTHS.size())
			return new TaxHistorySet();

		return (TaxHistorySet) mTHS.get(index);
	}

	public void addTaxHistorySet(TaxHistorySet obj) {
		mTHS.add(obj);
	}

	/**
	 * Returns the tHS.
	 * 
	 * @return Vector
	 */
	public Vector getTaxHistorySet() {
		return mTHS;
	}

	public int getTaxInstallmentSetsCount() {
		return mTIS.size();
	}

	public TaxInstallmentSet getTaxInstallmentSet(int index) {
		if (index >= mTIS.size())
			return new TaxInstallmentSet();

		return (TaxInstallmentSet) mTIS.get(index);
	}

	public void addTaxInstallmentSet(TaxInstallmentSet obj) {
		mTIS.add(obj);
	}

	public Vector getTaxInstallmentSet() {
		return mTIS;
	}

	public int getSpecialAssessmentSetsCount() {
		return mSAS.size();
	}

	public SpecialAssessmentSet getSpecialAssessmentSet(int index) {
		if (index >= mSAS.size())
			return new SpecialAssessmentSet();

		return (SpecialAssessmentSet) mSAS.get(index);
	}

	public void addSpecialAssessmentSet(SpecialAssessmentSet obj) {
		mTIS.add(obj);
	}
	
	public Vector getSpecialAssessmentSet() {
		return mSAS;
	}
	
	public OtherInformationSet getOtherInformationSet() {
		return mOIS;
	}

	// //////////////////////////////////////////////

	public String toString() {
		return "ParsedResponse(resultsCount=" + getResultsCount() + "){\n" + join(mResultRow, "\n") + "}" + "\n page  ";
	}

	public String toStringDetails() {
		return "ParsedResponse(resultsCount=" + getResultsCount() + ")\n" + "PIS = " + new ArrayList(mPIS) + "\n" + "CDIS = "
				+ new ArrayList(mCDIS) + "\n" + "SDS = " + new ArrayList(mSDS) + "\n";
	}

	public ImageLinkInPage getImageLink(int i) {
		return (ImageLinkInPage) imageLinks.get(i);
	}

	public int getImageLinksCount() {
		return imageLinks.size();
	}

	public void addImageLink(ImageLinkInPage page) {
		imageLinks.add(page);
	}

	/**
	 * @return
	 */
	public LinkInPage getPageLink() {
		return pageLink;
	}

	/**
	 * @param page
	 */
	public void setPageLink(LinkInPage link) {
		pageLink = link;
	}

	/**
	 * @return
	 */
	public String getWarning() {
		return msWarning;
	}

	/**
	 * @param string
	 */
	public void setWarning(String string) {
		msWarning = string;
	}

	public void setInitialResultsCount(int initialCount) {
		initialResultsCount = initialCount;
	}

	public int getInitialResultsCount() {
		return initialResultsCount;
	}

	public boolean isWarning() {
		return msWarning != null;
	}

	/**
	 * @return
	 */
	public String getNextLink() {
		return nextLink;
	}

	/**
	 * @param string
	 */
	public void setNextLink(String string) {
		nextLink = string;
	}

	public int getCrossRefSetCount() {
		return mCrossRef.size();
	}

	public Vector getCrossRefSets() {
		return mCrossRef;
	}

	public CrossRefSet getCrossRefSet(int i) {
		return (CrossRefSet) mCrossRef.get(i);
	}

	public void setAttribute(String key, Object attribute) {
		if (key != null && attribute != null)
			attributes.put(key, attribute);
	}

	public Object getAttribute(String key) {
		return attributes.get(key);
	}

	public void clearAttributes() {
		attributes = new Hashtable<String, Object>();
	}

	public synchronized Object clone() {
		try {
			ParsedResponse pr = (ParsedResponse) super.clone();
			try {
				pr.msError = msError;
			} catch (Exception e) {
			}
			try {
				pr.msWarning = msWarning;
			} catch (Exception e) {
			}
			try {
				pr.msResponse = msResponse;
			} catch (Exception e) {
			}
			try {
				pr.nextLink = nextLink;
			} catch (Exception e) {
			}
			try {
				pr.header = header;
			} catch (Exception e) {
			}
			try {
				pr.footer = footer;
			} catch (Exception e) {
			}
			try {
				pr.fileName = fileName;
			} catch (Exception e) {
			}
			try {
				pr.mResultRow = new Vector(mResultRow);
			} catch (Exception e) {
			}
			try {
				pr.imageLinks = new Vector(imageLinks);
			} catch (Exception e) {
			}
			try {
				pr.pageLink = (LinkInPage) ((LinkInPage) pageLink).clone();
			} catch (Exception e) {
			}
			try {
				pr.isParentSite = isParentSite;
			} catch (Exception e) {
			}

			return pr;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("clone() not supported in ParsedResponse");
		}
	}

	public void resetImages() {
		imageLinks = new Vector();
	}

	public boolean isParentSite() {
		return isParentSite;
	}

	public void setParentSite(boolean isParentSite) {
		this.isParentSite = isParentSite;
	}

	/**
	 * Create a simple text representation
	 * 
	 * @return
	 */
	public String getTextRepresentation() {

		String html = getResponse();

		// remove all tags
		html = html.replaceAll("<[^<>]*>", " ");

		// remove leftover from previous tag
		int index = html.indexOf(">");
		if (index != -1) {
			html = html.substring(index);
		}

		// remove leftover from next tag
		index = html.indexOf("<");
		if (index != -1) {
			html = html.substring(index);
		}

		// reduce size
		html = html.replaceAll("\\s{2,}", " ");

		return html;
	}

	/**
	 * Create a set of HTML TD tags with all columns from TSR index
	 * 
	 * @return
	 */
	public String getTsrIndexRepresentation() {

		SearchLogRow searchLogRow = getSearchLogRow();

		if (searchLogRow.isEmpty()) {
			return "<td colspan='8'>&nbsp;" + getTextRepresentation() + "&nbsp;</td>";
		} else {
			return "<td>&nbsp;" + searchLogRow.getDs() + "&nbsp;<br>&nbsp;" + searchLogRow.getSearchType() + "&nbsp;</td><td halign='left'>" + searchLogRow.getDesc() + "</td><td>&nbsp;"
					+ searchLogRow.getDate() + "&nbsp;</td><td>&nbsp;" + searchLogRow.getGtors() + "&nbsp;</td><td>&nbsp;"
					+ searchLogRow.getGtees() + "&nbsp;</td><td>&nbsp;" + searchLogRow.getDocType() + "&nbsp;</td><td>&nbsp;"
					+ searchLogRow.getDocNo() + "&nbsp;</td><td>&nbsp;" + searchLogRow.getComments() + "&nbsp;</td>";
		}
	}

	public SearchLogRow getSearchLogRow() {
		String ds = "";
		String desc = "";
		String date = "";
		String gtors = "";
		String gtees = "";
		String docType = "";
		String docNo = "";
		String comments = "";

		String onePin = "";
		String pins = "";
		String legals = "";
		String plegals = "";
		String addresses = "";
		String muns = "";
		String districts = "";
		
		String searchType = "";

		SearchLogRow searchLogRow = new SearchLogRow();
		if (isUseDocumentForSearchLogRow()) {
			if(!getDocument().isFieldModified(Fields.GRANTOR)) {
				TSServer.calculateAndSetFreeForm(getDocument(),com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType.GRANTOR, searchId);	
			}
			if(!getDocument().isFieldModified(Fields.GRANTEE)) {
				TSServer.calculateAndSetFreeForm(getDocument(),com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType.GRANTEE, searchId);
			}
			if (getDocument().getSearchType().equals(SearchType.NA) && !this.getSearchType().equals(SearchType.NA)){
				getDocument().setSearchType(this.getSearchType());
			}
			searchLogRow = getDocument().getSearchLogRow();
		} else {

			InfSet ois = (InfSet) getOtherInformationSet();
			ds = ois.getAtribute("SrcType");
			String amount = ois.getAtribute("Amount");

			List<String> owners = new ArrayList<String>();
			if (getPropertyIdentificationSetCount() > 0) {
				for (int i = 0; i < getPropertyIdentificationSetCount(); i++) {
					InfSet pis = (InfSet) getPropertyIdentificationSet(i);
					String plat = pis.getAtribute("PlatInstr");
					String pb = pis.getAtribute("PlatBook");
					String pp = pis.getAtribute("PlatNo");
					if (StringUtils.isNotEmpty(pb)){
						plat = pb;
						if (StringUtils.isNotEmpty(pp)){
							plat +=  "_" + pp;
						}
					} 
					String pin = pis.getAtribute("ParcelID");
					String lot = pis.getAtribute("SubdivisionLotNumber");
					String lotTh = pis.getAtribute("SubdivisionLotThrough");
					String subLot = pis.getAtribute(PropertyIdentificationSetKey.SUB_LOT.getShortKeyName());
					String subd = pis.getAtribute("SubdivisionName");
					String subdSec = pis.getAtribute(PropertyIdentificationSetKey.SECTION.getShortKeyName());
					String sec = pis.getAtribute("SubdivisionSection");
					String twn = pis.getAtribute("SubdivisionTownship");
					String rng = pis.getAtribute("SubdivisionRange");
					String ph = pis.getAtribute("SubdivisionPhase");
					String unit = pis.getAtribute("SubdivisionUnit");
					String blk = pis.getAtribute("SubdivisionBlock");
					String blkTh = pis.getAtribute("SubdivisionBlockThrough");
					String tr = pis.getAtribute("SubdivisionTract");
					// String pDsc = pis.getAtribute("PropertyDescription");
					String pLeg = pis.getAtribute("PartialLegal");
					String stNo = pis.getAtribute("StreetNo");
					String stNm = pis.getAtribute("StreetName");
					String pType = pis.getAtribute("PropertyType");
					String zip = pis.getAtribute("Zip");
					String ownerZip = pis.getAtribute("OwnerZipCode");
					String city = pis.getAtribute("City");
					String mun = pis.getAtribute("MunicipalJurisdiction");
					String district = pis.getAtribute("District");

					String qo = pis.getAtribute("QuarterOrder");
					String qv = pis.getAtribute("QuarterValue");
					String arb = pis.getAtribute("ARB");
					String acres = pis.getAtribute("Acres");
					String ncb = pis.getAtribute("NcbNo");
					
					// create legal
					String legal = "";

					// if(!isEmpty(pDsc)){ legal += " <b>LEGAL FULL</b>:" +
					// escapeHtml(pDsc) + ""; }
					if (!isEmpty(subd)) {
						legal += " <b>Subd</b>:" + escapeHtml(subd) + "";
					}
					if (!isEmpty(subdSec)) {
						legal += " <b>SubdSec</b>:" + escapeHtml(subdSec) + "";
					}
					if (!isEmpty(plat)) {
						legal += " <b>Plat</b>: " + escapeHtml(plat) + "";
					}
					if (!isEmpty(ph)) {
						legal += " <b>Ph</b>: " + escapeHtml(ph) + "";
					}
					if (!isEmpty(tr)) {
						legal += " <b>Tr</b>: " + escapeHtml(tr) + "";
					}
					if (!isEmpty(sec)) {
						legal += " <b>Sec</b>: " + escapeHtml(sec);
					}
					if (!isEmpty(twn)) {
						legal += " <b>Twn</b>: " + escapeHtml(twn);
					}
					if (!isEmpty(rng)) {
						legal += " <b>Rng</b>: " + escapeHtml(rng);
					}
					if (!isEmpty(qo)) {
						legal += " <b>QuarterO</b>: " + escapeHtml(qo);
					}
					if (!isEmpty(qv)) {
						legal += " <b>QuarterV</b>: " + escapeHtml(qv);
					}
					if (!isEmpty(arb)) {
						legal += " <b>ARB</b>: " + escapeHtml(arb);
					}
					if (!isEmpty(ncb)) {
						legal += " <b>Ncb</b>: " + escapeHtml(ncb);
					}
					if (!isEmpty(lot) && !"-".equals(lot)) {
						legal += " <b>Lot</b>: " + escapeHtml(lot) + "";
					}
					if (!isEmpty(lotTh) && !"-".equals(lotTh)) {
						legal += " <b>LotThrough</b>: " + escapeHtml(lotTh) + "";
					}
					if (!isEmpty(subLot) && !"-".equals(subLot)) {
						legal += " <b>SubLot</b>: " + escapeHtml(subLot) + "";
					}
					if (!isEmpty(blk)) {
						legal += " <b>Blk</b>: " + escapeHtml(blk) + "";
					}
					if (!isEmpty(blkTh)) {
						legal += " <b>BlkThrough</b>: " + escapeHtml(blkTh) + "";
					}
					if (!isEmpty(unit)) {
						legal += " <b>Unit</b>: " + escapeHtml(unit) + "";
					}
					if (!isEmpty(acres)) {
						legal += " <b>Acres</b>: " + escapeHtml(acres) + "";
					}

					// create address
					String address = (stNo + " " + stNm + " " + city + " " + zip).trim();
					// this need for cleaning the quotes from street name.
					// the quotes are introduced in street name for a good split
					// for ugly address in StandardAddress
					address = address.replaceAll("\\\"+", "");
					if ("WP".equals(ds)) {
						//for WP site, ownerZip code should appear at Desc on Addr area
						address += ("; Zip: " + ownerZip).trim();
					}

					// gather results
					if (!isEmpty(pin)) {
						pins += "<b>Pin</b>: " + pin + "<br/>";
						onePin = pin;
					}
					if (!isEmpty(legal)) {
						legals += "" + legal + "<br/>";
					}
					if (!isEmpty(pLeg)) {
						plegals += "<b>Part</b>: " + pLeg + "<br/>";
					}
					if (!isEmpty(address)) {
						addresses += "<b>Addr</b>: " + address + "<br/>";
					}
					if (!isEmpty(mun)) {
						muns += "<b>Mun</b>:" + mun + "<br/>";
					}
					if (!isEmpty(district)) {
						districts += "<b>Dtrct</b>:" + district + "<br/>";
					}

					if (!isEmpty(pType)) {
						comments += "<b>Type</b>:" + pType + "<br/>";
					}
					String partyNameSetString = pis.displayNameSet();
					if (!partyNameSetString.isEmpty()) {
						owners.add(partyNameSetString);
					}

				}
				if (owners.isEmpty()) {

					for (PartyNameSet partyNameSet : (Vector<PartyNameSet>) getPartyNameSet()) {
						String partyNameSetString = (partyNameSet.getAtribute("FirstName") + " " + partyNameSet.getAtribute("MiddleName")
								+ " " + partyNameSet.getAtribute("LastName")).trim();
						if (!partyNameSetString.isEmpty()) {
							owners.add(partyNameSetString);
						}

					}
				}
			} else {
				try {
					for (InfSet pis : (Vector<InfSet>) infVectorSets.get("PropertyIdentificationSet")) {

						String plat = pis.getAtribute("PlatInstr");
						String pb = pis.getAtribute("PlatBook");
						String pp = pis.getAtribute("PlatNo");
						if (!isEmpty(pb) && !isEmpty(pp)) {
							plat = pb + "_" + pp;
						}
						String pin = pis.getAtribute("ParcelID");
						String lot = pis.getAtribute("SubdivisionLotNumber");
						String lotTh = pis.getAtribute("SubdivisionLotThrough");
						String subLot = pis.getAtribute(PropertyIdentificationSetKey.SUB_LOT.getShortKeyName());
						String subd = pis.getAtribute("SubdivisionName");
						String subdSec = pis.getAtribute(PropertyIdentificationSetKey.SECTION.getShortKeyName());
						String sec = pis.getAtribute("SubdivisionSection");
						String twn = pis.getAtribute("SubdivisionTownship");
						String rng = pis.getAtribute("SubdivisionRange");
						String ph = pis.getAtribute("SubdivisionPhase");
						String unit = pis.getAtribute("SubdivisionUnit");
						String blk = pis.getAtribute("SubdivisionBlock");
						String blkTh = pis.getAtribute("SubdivisionBlockThrough");
						String tr = pis.getAtribute("SubdivisionTract");
						// String pDsc = pis.getAtribute("PropertyDescription");
						String pLeg = pis.getAtribute("PartialLegal");
						String stNo = pis.getAtribute("StreetNo");
						String stNm = pis.getAtribute("StreetName");
						String pType = pis.getAtribute("PropertyType");
						String zip = pis.getAtribute("Zip");
						String city = pis.getAtribute("City");
						String mun = pis.getAtribute("MunicipalJurisdiction");
						String district = pis.getAtribute("District");

						String qo = pis.getAtribute("QuarterOrder");
						String qv = pis.getAtribute("QuarterValue");
						String arb = pis.getAtribute("ARB");
						
						String ncb = pis.getAtribute("NcbNo");
						// create legal
						String legal = "";

						// if(!isEmpty(pDsc)){ legal += " <b>LEGAL FULL</b>:" +
						// escapeHtml(pDsc) + ""; }
						if (!isEmpty(subd)) {
							legal += " <b>Subd</b>:" + escapeHtml(subd) + "";
						}
						if (!isEmpty(subdSec)) {
							legal += " <b>SubdSec</b>:" + escapeHtml(subdSec) + "";
						}
						if (!isEmpty(plat)) {
							legal += " <b>Plat</b>: " + escapeHtml(plat) + "";
						}
						if (!isEmpty(ph)) {
							legal += " <b>Ph</b>: " + escapeHtml(ph) + "";
						}
						if (!isEmpty(tr)) {
							legal += " <b>Tr</b>: " + escapeHtml(tr) + "";
						}
						if (!isEmpty(sec)) {
							legal += " <b>Sec</b>: " + escapeHtml(sec);
						}
						if (!isEmpty(twn)) {
							legal += " <b>Twn</b>: " + escapeHtml(twn);
						}
						if (!isEmpty(rng)) {
							legal += " <b>Rng</b>: " + escapeHtml(rng);
						}
						if (!isEmpty(qo)) {
							legal += " <b>QuarterO</b>: " + escapeHtml(qo);
						}
						if (!isEmpty(qv)) {
							legal += " <b>QuarterV</b>: " + escapeHtml(qv);
						}
						if (!isEmpty(arb)) {
							legal += " <b>ARB</b>: " + escapeHtml(arb);
						}
						if (!isEmpty(ncb)) {
							legal += " <b>Ncb</b>: " + escapeHtml(ncb);
						}
						if (!isEmpty(lot) && !"-".equals(lot)) {
							legal += " <b>Lot</b>: " + escapeHtml(lot) + "";
						}
						if (!isEmpty(lotTh) && !"-".equals(lotTh)) {
							legal += " <b>LotThrough</b>: " + escapeHtml(lotTh) + "";
						}
						if (!isEmpty(subLot) && !"-".equals(subLot)) {
							legal += " <b>SubLot</b>: " + escapeHtml(subLot) + "";
						}
						if (!isEmpty(blk)) {
							legal += " <b>Blk</b>: " + escapeHtml(blk) + "";
						}
						if (!isEmpty(blkTh)) {
							legal += " <b>BlkThrough</b>: " + escapeHtml(blkTh) + "";
						}
						if (!isEmpty(unit)) {
							legal += " <b>Unit</b>: " + escapeHtml(unit) + "";
						}

						// create address
						String address = (stNo + " " + stNm + " " + city + " " + zip).trim();

						// gather results
						if (!isEmpty(pin)) {
							pins += "<b>Pin</b>: " + pin + "<br/>";
							onePin = pin;
						}
						if (!isEmpty(legal)) {
							legals += "" + legal + "<br/>";
						}
						if (!isEmpty(pLeg)) {
							plegals += "<b>Part</b>: " + pLeg + "<br/>";
						}
						if (!isEmpty(address)) {
							addresses += "<b>Addr</b>: " + address + "<br/>";
						}
						if (!isEmpty(mun)) {
							muns += "<b>Mun</b>:" + mun + "<br/>";
						}
						if (!isEmpty(district)) {
							districts += "<b>Dtrct</b>:" + district + "<br/>";
						}

						if (!isEmpty(pType)) {
							comments += "<b>Type</b>:" + pType + "<br/>";
						}

						owners.add(pis.displayNameSet());
					}
				} catch (Exception e) {
					logger.error("Error while creating log!", e);
				}

			}

			String book = "";
			String page = "";
			String instNo = "";

			if (getSaleDataSetsCount() > 0) {
				for (int i = 0; i < getSaleDataSetsCount(); i++) {

					InfSet sds = (InfSet) getSaleDataSet(i);

					// treat amount
					String mAmount = sds.getAtribute("MortgageAmount");
					String cAmount = sds.getAtribute("ConsiderationAmount");
					if (!"0.00".equals(mAmount) && !isEmpty(mAmount)) {
						amount = mAmount;
					}
					if (!"0.00".equals(cAmount) && !isEmpty(cAmount)) {
						amount = cAmount;
					}

					// treat grantors & grantees
					String grantor = sds.getAtribute("Grantor");
					String grantee = sds.getAtribute("Grantee");
					String granteeLander = sds.getAtribute("GranteeLander");
					if (!isEmpty(grantor)) {
						gtors += " " + grantor;
					}
					if (!isEmpty(grantee)) {
						gtees += " " + grantee;
					}
					if (!isEmpty(granteeLander)) {
						gtees += " / " + granteeLander;
					}

					// treat book, page
					String bk = sds.getAtribute("Book");
					String pg = sds.getAtribute("Page");
					if (!isEmpty(bk) && !isEmpty(pg)) {
						book = bk;
						page = pg;
					}

					// treat instrument, doc number
					String in = sds.getAtribute("InstrumentNumber");
					String dn = sds.getAtribute("DocumentNumber");
					if (!isEmpty(in) && !"0".equals(in)) {
						instNo = in;
					}
					if (!isEmpty(dn) && !"0".equals(dn)) {
						instNo = dn;
					}

					// treat doc type
					String dt = sds.getAtribute("DocumentType");
					if (!isEmpty(dt)) {
						docType = dt;
					}

					// treat date
					String d2 = sds.getAtribute("PreparedDate");
					if (!isEmpty(d2) && !"*".equals(d2)) {
						date = "PrepD: " + d2;
					}
					String d1 = sds.getAtribute("RecordedDate");
					if (!isEmpty(d1) && !"*".equals(d1)) {
						if (org.apache.commons.lang.StringUtils.isNotBlank(date)){
							date += "<br>";
						}
						date += "RecD: " + d1;
					}
					String d3 = sds.getAtribute("InstrumentDate");
					if (!isEmpty(d3) && !"*".equals(d3)) {
						if (org.apache.commons.lang.StringUtils.isNotBlank(date)){
							date += "<br>";
						}
						date += "InsD: " + d3;
					}
				}
			}
			
			 if (Bridge.isAssessorSite(ds)) {
				 date = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
			 }

			// create docNo column
			if (!isEmpty(book) && !isEmpty(page)) {
				docNo = book + "_" + page;
			}
			if (!isEmpty(instNo) && !docNo.equals(instNo)) {
				if (!isEmpty(docNo)) {
					docNo += "<br/>";
				}
				docNo += instNo;
			}

			for (int i = 0; i < getCourtDocumentIdentificationSetCount(); i++) {
				InfSet cis = (InfSet) getCourtDocumentIdentificationSet(i);
			}

			LinkedHashSet<String> crossRefs = new LinkedHashSet<String>();
			for (int i = 0; i < getCrossRefSetCount(); i++) {
				InfSet cr = (InfSet) getCrossRefSet(i);
				String crs = cr.displayCrossRefSet();
				if (!isEmpty(crs)) {
					crossRefs.add(crs);
				}
			}

			if (!isEmpty(amount)) {
				amount = "<b>Amount</b>: " + amount + "	<br/>";
			}

			desc = pins + legals + plegals + addresses + muns + amount + districts;
			if (desc.endsWith("<br/>")) {
				desc = desc.substring(0, desc.lastIndexOf("<br/>"));
			}
			if (comments.endsWith("<br/>")) {
				comments = comments.substring(0, comments.lastIndexOf("<br/>"));
			}

			// fix the AO sites
			if (Bridge.isAssessorSite(ds) | Bridge.isTaxSite(ds)) {
				if (Bridge.isCityTaxSite(ds)) {
					gtees = "City";
				} else {
					gtees = "County";
				}
				if (Bridge.isAssessorSite(ds)) {
					docType = "Appraisal & Assessment";
				} else if (Bridge.isCountyTaxSite(ds)) {
					docType = CNTYTAX_DOC_TYPE;
				} else if (Bridge.isCityTaxSite(ds)) {
					docType = CITYTAX_DOC_TYPE;
				}
				// gtors = "__________";
				gtors = formatList(owners, " / ");

				docNo = onePin;

				// 5306, 5404
				for (int i = 0; i < getSaleDataSetsCount(); i++) {
					InfSet cr = (InfSet) getSaleDataSet(i);
					String crs = cr.displayCrossRefSet();
					if (!isEmpty(crs)) {
						crossRefs.add(crs);
					}
				}
			}

			// if the names were not gathered in SaleDataSet,
			// try to get them from GrantorSet
			if (isEmpty(gtors)) {
				List<String> grantors = new ArrayList<String>();

				for (int i = 0; i < getGrantorNameSetCount(); i++) {
					NameSet name = (NameSet) getGrantorNameSet(i);
					grantors.add(name.displayNameSet());
				}
				gtors = formatList(grantors, " / ");
			}

			// if the names were not gathered in SaleDataSet,
			// try to get them from GranteeSet
			if (isEmpty(gtees)) {
				List<String> grantees = new ArrayList<String>();

				for (int i = 0; i < getGranteeNameSetCount(); i++) {
					NameSet name = (NameSet) getGranteeNameSet(i);
					grantees.add(name.displayNameSet());
				}
				gtees = formatList(grantees, " / ");
			}

			// make sure the cells will not have empty values
			if (isEmpty(desc)) {
				desc = "&nbsp;";
			}

			// temporar
			// comments += display(true, true);

			if (crossRefs.size() != 0) {
				if (!isEmpty(comments)) {
					comments += "<br/>";
				}
				comments += "<b>Ref</b>: " + formatList(crossRefs, "; ");
			}

			if (getTaxHistorySetsCount() > 0) {
				String year = getTaxHistorySet(0).getAtribute("Year");
				if (!StringUtils.isEmpty(year)) {
					if (!isEmpty(comments)) {
						comments += "<br/>";
					}
					comments += "<b>Year</b>: " + year;
					if (CITYTAX_DOC_TYPE.equals(docType) || CNTYTAX_DOC_TYPE.equals(docType)) {
						date = year;
					}
				} else {
					year = getTaxHistorySet(0).getAtribute("ReceiptDate");
					if (!StringUtils.isEmpty(year)) {
						if (!isEmpty(comments)) {
							comments += "<br/>";
						}
						comments += "<b>Date</b>: " + year;
					} else {
						date = "*";
					}
				}
				String billNumber = getTaxHistorySet(0).getAtribute("TaxBillNumber");
				if (!StringUtils.isEmpty(billNumber)) {
					if (!isEmpty(comments)) {
						comments += "<br/>";
					}
					comments += "<b>Bill Number</b>: " + billNumber;
				}
			}

			String remarks = getOtherInformationSet().getAtribute("Remarks");
			if (StringUtils.isNotEmpty(remarks)) {
				if (!isEmpty(comments)) {
					comments += "<br/>";
				}
				comments += remarks;
			}

			for (int i = 0; i < getCourtDocumentIdentificationSetCount(); i++) {
				InfSet cis = (InfSet) getCourtDocumentIdentificationSet(i);
			}

			searchType = getSearchType().toString().toLowerCase(); // get search type from search Module
			
			
			DocumentI doc = getDocument();
			if (doc != null){
				if (!SearchType.NA.toString().equalsIgnoreCase(doc.getSearchType().toString())){
					searchType = doc.getSearchType().toString().toLowerCase();
				}
			}
			if (doc != null && doc instanceof RegisterDocument){
				String propertyRemarks = doc.getInfoForSearchLog();
				if (org.apache.commons.lang.StringUtils.isNotEmpty(propertyRemarks)){
					if (this.getAttribute(ParsedResponse.REAL_PI) != null && "true".equals(this.getAttribute(ParsedResponse.REAL_PI))){
						if (!isEmpty(comments)) {
							comments += "<br/>";
						}
						comments += "<b>Add info</b>: " + propertyRemarks; 
					}
				}
			}
			
			gtees = gtees.replaceFirst("^\\s*/", "");
			gtees = gtees.replaceFirst("/\\s*$", "");
			gtees = gtees.trim();
			gtors = gtors.trim();
			
			searchLogRow.setDesc(desc);
			searchLogRow.setDate(date);
			searchLogRow.setGtees(gtees);
			searchLogRow.setGtors(gtors);
			searchLogRow.setDocType(docType);
			searchLogRow.setDocNo(docNo);
			searchLogRow.setComments(comments);
			searchLogRow.setDs(ds);
			searchLogRow.setSearchType(searchType);
		}

		return searchLogRow;
	}
	
	/**
	 * Get search type from search module for the current document
	 * 
	 * @return Search Type 
	 */
	public SearchType getSearchType() {
		try {
			long searchId = getSearchId();
			
			if(searchId == 0)
				return SearchType.NA;
			
			Search search = SearchManager.getSearch(searchId, false);

			if (search != null) {

				// if (search.getSearchIterator() != null) { // automatic
				// try {
				// TSServerInfoModule module = (TSServerInfoModule) search.getSearchIterator().current();
				//
				// if (module != null) {
				// String searchType = module.getSearchType();
				// if (StringUtils.isEmpty(searchType)) {
				// return TSServerInfo.getDefaultSearchTypeForModule(module.getModuleIdx(), getDocument());
				// } else {
				// return SearchType.valueOf(searchType);
				// }
				// }
				//
				// } catch (Exception e) {
				// logger.error("Error getting when extracting SearchType module on searchId: " + searchId, e);
				// }
				// } else {
				
				TSServerInfoModule module = search.getSearchRecord().getModule();

				if (module != null) {
					if(module.getIteratorType() == ModuleStatesIterator.TYPE_OCR_FULL_OR_BOOTSTRAPER 
							|| module.getIteratorType() == ModuleStatesIterator.TYPE_OCR){
						return SearchType.IP;
					} else if (StringUtils.isNotEmpty(module.getSearchType())) {
						return SearchType.valueOf(module.getSearchType());
					}

					return TSServerInfo.getDefaultSearchTypeForModule(module.getModuleIdx(), getDocument());
				}
			}
//			}
		} catch (Exception e) {
			logger.error("Error getting when extracting SearchType module on searchId: " + searchId);
		}
		
		return SearchType.NA;
	}
	

	@SuppressWarnings("unchecked")
	public String getFirstPin() {
		if (getPropertyIdentificationSet() == null) {
			return "";
		}
		for (PropertyIdentificationSet pis : (Vector<PropertyIdentificationSet>) getPropertyIdentificationSet()) {
			String pin = pis.getAtribute("ParcelID");
			if (!isEmpty(pin)) {
				return pin;
			}
		}
		return "";
	}

	public long getSearchId() {
		return searchId;
	}

	public void setSearchId(long searchId) {
		this.searchId = searchId;
	}

	public void setDocument(DocumentI doc) {
		if (attributes == null)
			attributes = new Hashtable<String, Object>();
		attributes.put("9999_DOCUMENT_99999", doc);
	}

	public DocumentI getDocument() {
		if (attributes == null)
			attributes = new Hashtable<String, Object>();
		return (DocumentI) attributes.get("9999_DOCUMENT_99999");
	}

	/**
	 * Tries to get the "Book" value from the document if already parsed<br>
	 * If no document, then the method goes to the origin and reads the parsed
	 * vector
	 * 
	 * @return the book value (the value will <b>never</b> be null)
	 */
	public String getBook() {
		DocumentI document = getDocument();
		String result = null;
		if (document != null) {
			result = document.getBook();
		} else {
			result = Bridge.getFieldFromVector((Vector) infVectorSets.get("SaleDataSet"), "Book");
		}
		return org.apache.commons.lang.StringUtils.defaultString(result).trim();
	}

	/**
	 * Tries to get the "Page" value from the document if already parsed<br>
	 * If no document, then the method goes to the origin and reads the parsed
	 * vector
	 * 
	 * @return the page value (the value will <b>never</b> be null)
	 */
	public String getPage() {
		DocumentI document = getDocument();
		String result = null;
		if (document != null) {
			result = document.getPage();
		} else {
			result = Bridge.getFieldFromVector((Vector) infVectorSets.get("SaleDataSet"), "Page");
		}
		return org.apache.commons.lang.StringUtils.defaultString(result).trim();
	}

	/**
	 * Tries to get the "InstrumentNumber" value from the document if already
	 * parsed<br>
	 * If no document, then the method goes to the origin and reads the parsed
	 * vector
	 * 
	 * @return the page value (the value will <b>never</b> be null)
	 */
	public String getInstrumentNumber() {
		DocumentI document = getDocument();
		String result = null;
		if (document != null) {
			result = document.getInstno();
		} else {
			result = Bridge.getFieldFromVector((Vector) infVectorSets.get("SaleDataSet"), "InstrumentNumber");
		}
		return org.apache.commons.lang.StringUtils.defaultString(result).trim();
	}

	/**
	 * Tries to get the "DocumentNumber" value from the document if already
	 * parsed<br>
	 * If no document, then the method goes to the origin and reads the parsed
	 * vector
	 * 
	 * @return the page value (the value will <b>never</b> be null)
	 */
	public String getDocumentNumber() {
		DocumentI document = getDocument();
		String result = null;
		if (document != null) {
			result = document.getDocno();
		} else {
			result = Bridge.getFieldFromVector((Vector) infVectorSets.get("SaleDataSet"), "DocumentNumber");
		}
		return org.apache.commons.lang.StringUtils.defaultString(result).trim();
	}

	/**
	 * Tries to get the "Year" value from the document if already parsed<br>
	 * If no document, then the method goes to the origin and reads the parsed
	 * vector
	 * 
	 * @return the page value (the value will <b>never</b> be null)
	 */
	public int getYear() {
		DocumentI document = getDocument();
		if (document != null) {
			return document.getYear();
		} else {
			String srcType = ((OtherInformationSet) infSets.get("OtherInformationSet")).getAtribute("SrcType");
			if (srcType == null) {
				srcType = "";
			}
			if (Bridge.isAssessorSite(srcType)) {
				return Calendar.getInstance().get(Calendar.YEAR);
			} else if (Bridge.isTaxSite(srcType)
					|| Bridge.isTaxFromDT(srcType, Bridge.getFieldFromVector((Vector) infVectorSets.get("SaleDataSet"), "DocumentType"))) {
				Vector<TaxHistorySet> vectorTHS = (Vector<TaxHistorySet>) infVectorSets.get("TaxHistorySet");
				// set tax year
				String tmp = Bridge.getFieldFromVector((Vector) vectorTHS, "Year");
				int result = SimpleChapterUtils.UNDEFINED_YEAR;
				if (!StringUtils.isEmpty(tmp)) {
					try {
						result = Integer.parseInt(tmp);
					} catch (NumberFormatException e) {
					}
				}
				if (result == SimpleChapterUtils.UNDEFINED_YEAR) {
					CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
					// B3787
					String crtCounty = currentInstance.getCurrentCounty().getName();
					String crtState = currentInstance.getCurrentState().getStateAbv();
					if (crtState.equals("MI") && crtCounty.equals("Wayne")) {
						// get the tax year from search sites
						Date payDate = HashCountyToIndex.getPayDate(currentInstance.getCommunityId(), crtState, crtCounty, DType.CITYTAX);
						return payDate.getYear() + 1900;
					}
				}
			} else {
				// set recordedYear
				String recDate = Bridge.getFieldFromVector((Vector) infVectorSets.get("SaleDataSet"), "RecordedDate");
				if (recDate.length() != 0 && !recDate.trim().equalsIgnoreCase("N/A")) {
					Date date = Util.dateParser3(recDate);
					if (date != null) {
						return 1900 + date.getYear();
					}
				}
			}

		}

		return SimpleChapterUtils.UNDEFINED_YEAR;
	}

	/**
	 * Tries to get the "ServerDocType" value from the document if already
	 * parsed<br>
	 * If no document, then the method goes to the origin and reads the parsed
	 * vector
	 * 
	 * @return the page value (the value will <b>never</b> be null)
	 */
	public String getServerDocType() {
		DocumentI document = getDocument();
		String result = null;
		if (document != null) {
			result = document.getServerDocType();
		} else {
			String srcType = ((OtherInformationSet) infSets.get("OtherInformationSet")).getAtribute("SrcType");
			if (srcType == null) {
				srcType = "";
			}
			if (Bridge.isAssessorSite(srcType)) {

			} else if (Bridge.isTaxSite(srcType)
					|| Bridge.isTaxFromDT(srcType, Bridge.getFieldFromVector((Vector) infVectorSets.get("SaleDataSet"), "DocumentType"))) {

			} else {
				result = Bridge.getFieldFromVector((Vector) infVectorSets.get("SaleDataSet"), "DocumentType");
			}

		}
		return org.apache.commons.lang.StringUtils.defaultString(result).trim();
	}

	public void setTsInterface(TSInterface tsInterface) {
		this.tsInterface = tsInterface;
	}

	public TSInterface getTsInterface() {
		return tsInterface;
	}

	public void setAttributes(Map<String, Object> extraParams) {
		if (extraParams != null) {
			Set<Entry<String, Object>> entrySet = extraParams.entrySet();
			for (Entry<String, Object> entry : entrySet) {
				setAttribute(entry.getKey(), entry.getValue());
			}
		}
	}

	public boolean isUseDocumentForSearchLogRow() {
		return useDocumentForSearchLogRow;
	}

	public void setUseDocumentForSearchLogRow(boolean useDocumentForSearchLogRow) {
		this.useDocumentForSearchLogRow = useDocumentForSearchLogRow;
	}
	
	public int getTaxYear(){
		if(this.getTaxHistorySetsCount() == 0){
			return 0;
		}
		String yearStr = this.getTaxHistorySet(0).getAtribute("ReceiptDate");
		if(!yearStr.matches("\\d+")){
			yearStr = this.getTaxHistorySet(0).getAtribute("Year");
			if(!yearStr.matches("\\d+")){
				return 0;
			}
		}
		return Integer.parseInt(yearStr);		
	}
}
