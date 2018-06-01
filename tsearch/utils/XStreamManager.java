package ro.cst.tsearch.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FileUtils;

import ro.cst.tsearch.ChapterSavedData;
import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchFlags;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.bean.TSDIndexPage;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.StateContants;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.propertyInformation.Address;
import ro.cst.tsearch.propertyInformation.Family;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.propertyInformation.Instrument.DocTypes;
import ro.cst.tsearch.propertyInformation.Owner;
import ro.cst.tsearch.propertyInformation.Person;
import ro.cst.tsearch.search.filter.newfilters.name.ExactNameFilter;
import ro.cst.tsearch.searchsites.client.GWTDataSite;
import ro.cst.tsearch.servers.ServletServerComm;
import ro.cst.tsearch.servers.ServletServerComm.CompareInstrumentsAfterRecordedDate;
import ro.cst.tsearch.servers.response.CrossRefSet;
import ro.cst.tsearch.servers.response.GranteeSet;
import ro.cst.tsearch.servers.response.GrantorSet;
import ro.cst.tsearch.servers.response.ImageLinkInPage;
import ro.cst.tsearch.servers.response.LinkInPage;
import ro.cst.tsearch.servers.response.OtherInformationSet;
import ro.cst.tsearch.servers.response.OwnerDetailsSet;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PartyNameSet;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.SpecialAssessmentSet;
import ro.cst.tsearch.servers.response.TaxHistorySet;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.servlet.FileServlet;
import ro.cst.tsearch.threads.AsynchSearchLogSaverThread;
import ro.cst.tsearch.titledocument.abstracts.Chapter;
import ro.cst.tsearch.titledocument.abstracts.Chapter.Types;
import ro.cst.tsearch.titledocument.abstracts.ChapterUtils;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.user.MyAtsAttributes;
import ro.cst.tsearch.user.UserAttributes;

import com.stewart.ats.base.document.AssessorDocument;
import com.stewart.ats.base.document.Document;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.Image;
import com.stewart.ats.base.document.ImageI;
import com.stewart.ats.base.document.Lien;
import com.stewart.ats.base.document.Mortgage;
import com.stewart.ats.base.document.Patriots;
import com.stewart.ats.base.document.PriorFileDocument;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.SKLDInstrument;
import com.stewart.ats.base.document.TaxDocument;
import com.stewart.ats.base.document.Transfer;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.TownShip;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.name.NameMortgageGrantee;
import com.stewart.ats.base.parties.Party;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.Pin;
import com.stewart.ats.base.property.PinI.PinType;
import com.stewart.ats.base.property.Property;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.tsrindex.client.CertificationDate;
import com.stewart.ats.tsrindex.client.Receipt;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;
import com.stewart.ats.tsrindex.client.ocrelements.LegalDescription;
import com.stewart.ats.tsrindex.client.ocrelements.OcredEditableElement;
import com.stewart.ats.tsrindex.client.ocrelements.VestingInfo;
import com.stewart.ats.tsrindex.server.UploadImage;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XStream11XmlFriendlyReplacer;
import com.thoughtworks.xstream.io.xml.XppDriver;

public class XStreamManager {
	
	private XStream xstream = null;
	private XStream xstreamCompatible = null;
	private XStream xstreamNoAlias = null;
	
	private XStreamManager(){
		
		xstream = new XStream();
		xstreamNoAlias = new XStream();
		xstreamCompatible = new XStream(new XppDriver(new XStream11XmlFriendlyReplacer())) {
			protected boolean useXStream11XmlFriendlyMapper() {
				return true;
			}
		};
		
		aliasAllClasses();
		//aliasAttribute("c", "class");
		//aliasAttribute("e", "enum-type");
		//aliasAttribute("r", "reference");
		
		aliasSearchFields();
		aliasSearchAttributeFields();
		aliasSearchFlagsFields();
		
		aliasDocumentManagerFields();
		aliasDocumentFields();
		aliasAssessorDocumentFields();
		aliasTaxDocumentFields();
		aliasRegisterDocumentFields();
		
		
		
		aliasInstrumentFields();
		aliasPartyFields();
		aliasNameFields();
		aliasChapterFields();
		aliasParsedResponseFields();
		aliasInstrumentPropertyFields();
		aliasTSDIndexPageFields();
		
		aliasPropertyFields();
		aliasAddressFields();
		aliasLegalFields();
		aliasSubdivisionFields();
		aliasTownshipFields();
		aliasLegalDescriptionFields();
		aliasReceiptFields();
		aliasImageFields();
		aliasMyAtsAttributes();
		
				
		aliasField("d", CertificationDate.class, "date");
		aliasField("e", CertificationDate.class, "edited");
		
		aliasField("a1", UserAttributes.class, "attrValues");
		aliasField("a", UserAttributes.class, "allowedCountyList");
		aliasField("A", UserAttributes.class, "ats2CommRates");
		aliasField("c", UserAttributes.class, "comm2AgentRates");
		aliasField("m", UserAttributes.class, "myAtsAttributes");
		aliasField("u", UserAttributes.class, "userLoginIp");
		
		aliasField("m", DocTypes.class, "mName");
		
		aliasField("o", Family.class, "mOwner");
		aliasField("s", Family.class, "mSpouse");
		aliasField("a", Family.class, "mAddress");
		
		aliasField("f", Person.class, "msFirstName");
		aliasField("m", Person.class, "msMiddleName");
		aliasField("l", Person.class, "msLastName");
		aliasField("p", Person.class, "msWorkPhone");
		aliasField("s", Person.class, "msSSN");
		
		aliasField("o", Address.class, "miStNo");
		aliasField("d", Address.class, "msStDirection");
		aliasField("n", Address.class, "msStName");
		aliasField("f", Address.class, "msStSuffix");
		aliasField("c", Address.class, "msCity");
		aliasField("s", Address.class, "msStateAbbr");
		aliasField("z", Address.class, "msZip");
		aliasField("u", Address.class, "miUnit");
		aliasField("p", Address.class, "msStPostDirection");
		
		
		aliasField("c", ChapterUtils.class, "chapters");
		aliasField("r", ChapterUtils.class, "registerChapters");
		
		aliasField("c", ImageLinkInPage.class, "contentType");
		aliasField("d", ImageLinkInPage.class, "downloadStatus");
		aliasField("f", ImageLinkInPage.class, "fakeLink");
		aliasField("i", ImageLinkInPage.class, "imageFileName");
		aliasField("j", ImageLinkInPage.class, "justImageLookUp");
		aliasField("l", ImageLinkInPage.class, "link");
		aliasField("p", ImageLinkInPage.class, "path");
		aliasField("s", ImageLinkInPage.class, "solved");
		aliasField("S", ImageLinkInPage.class, "ssfLink");
		
		aliasField("c", ChapterSavedData.class, "chapterChecked");
		aliasField("i", ChapterSavedData.class, "chapterImageChecked");
		
		aliasField("t", Types.class, "mType");
		
		aliasField("a", LinkInPage.class, "actionType");
		aliasField("l", LinkInPage.class, "link");
		aliasField("o", LinkInPage.class, "originalLink");
		
	}
	
	private void aliasSearchFlagsFields() {
		aliasField("i", SearchFlags.class, "isClosed");
		aliasField("if", SearchFlags.class, "isForReview");
		aliasField("io", SearchFlags.class, "isOld");
		aliasField("I", SearchFlags.class, "isTsrCreated");
		aliasField("o", SearchFlags.class, "objectVersionNumber");
		aliasField("p", SearchFlags.class, "permanentWarnings");
		aliasField("s", SearchFlags.class, "starter");
		aliasField("S", SearchFlags.class, "status");
		aliasField("w", SearchFlags.class, "warnings");
	}

	private void aliasMyAtsAttributes() {
		aliasField("a", MyAtsAttributes.class, "addressCase");
		aliasField("d", MyAtsAttributes.class, "DASHBOARD_START_INTERVAL");
		aliasField("de", MyAtsAttributes.class, "DASHBOARD_END_INTERVAL");
		aliasField("dr", MyAtsAttributes.class, "DASHBOARD_ROWS_PER_PAGE");
		aliasField("dh", MyAtsAttributes.class, "DEFAULT_HOMEPAGE");
		aliasField("ds", MyAtsAttributes.class, "default_state");
		aliasField("D", MyAtsAttributes.class, "default_county");
		aliasField("i", MyAtsAttributes.class, "invoiceEditEmail");
		aliasField("l", MyAtsAttributes.class, "legalCase");
		aliasField("m", MyAtsAttributes.class, "MY_ATS_READ_ONLY");
		aliasField("p", MyAtsAttributes.class, "paginate_tsrindex");
		aliasField("P", MyAtsAttributes.class, "pages");
		aliasField("r", MyAtsAttributes.class, "reportState");
		aliasField("rc", MyAtsAttributes.class, "reportCounty");
		aliasField("ra", MyAtsAttributes.class, "reportAbstractor");
		aliasField("rb", MyAtsAttributes.class, "reportCompanyAgent");
		aliasField("rg", MyAtsAttributes.class, "reportAgent");
		aliasField("rs", MyAtsAttributes.class, "reportStatus");
		aliasField("rd", MyAtsAttributes.class, "reportDefaultView");
		aliasField("ry", MyAtsAttributes.class, "reportSortBy");
		aliasField("tS", MyAtsAttributes.class, "reportSortDir");
		aliasField("rn", MyAtsAttributes.class, "receive_notification");		
		aliasField("rf", MyAtsAttributes.class, "reportNameFormat");
		aliasField("rN", MyAtsAttributes.class, "reportNameCase");
		aliasField("s", MyAtsAttributes.class, "SEARCH_PAGE_STATE");
		aliasField("sc", MyAtsAttributes.class, "SEARCH_PAGE_COUNTY");
		aliasField("sa", MyAtsAttributes.class, "SEARCH_PAGE_AGENT");
		aliasField("S", MyAtsAttributes.class, "search_log_link");
		aliasField("t", MyAtsAttributes.class, "TSR_SORTBY");
		aliasField("tu", MyAtsAttributes.class, "TSR_UPPER_LOWER");
		aliasField("tn", MyAtsAttributes.class, "TSR_NAME_FORMAT");
		aliasField("T", MyAtsAttributes.class, "TSR_COLORING");
		aliasField("u", MyAtsAttributes.class, "user_id");
		aliasField("v", MyAtsAttributes.class, "vestingCase");
		
	}

	private void aliasSearchAttributeFields() {
		aliasField("a", SearchAttributes.class, "abstrFileName");
		aliasField("c", SearchAttributes.class, "certification");
		aliasField("C", SearchAttributes.class, "commId");
		aliasField("e", SearchAttributes.class, "extraHashSearch");
		aliasField("i", SearchAttributes.class, "isSet");
		aliasField("l", SearchAttributes.class, "legalDescription");
		aliasField("o", SearchAttributes.class, "ownerSearchForGBList");
		aliasField("r", SearchAttributes.class, "reopenSearch");
		aliasField("H", SearchAttributes.class, "hashSearch");
		aliasField("s", SearchAttributes.class, "searchId");
		aliasField("S", SearchAttributes.class, "searchIdSKLD");
		aliasField("v", SearchAttributes.class, "vestingInfoGrantee");
		
	}

	private void aliasImageFields() {
		aliasField("c", Image.class, "contentType");
		aliasField("e", Image.class, "extension");
		aliasField("f", Image.class, "fileName");
		aliasField("l", Image.class, "links");
		aliasField("o", Image.class, "ocrInProgress");
		aliasField("O", Image.class, "ocrDone");
		aliasField("p", Image.class, "path");
		aliasField("P", Image.class, "planedForOCR");
		aliasField("s", Image.class, "saved");
		aliasField("S", Image.class, "ssfLink");
		aliasField("u", Image.class, "uploaded");
		aliasField("v", Image.class, "viewed");
	}

	private void aliasReceiptFields() {
		aliasField("a", Receipt.class, "receiptAmount");
		aliasField("d", Receipt.class, "receiptDate");
		aliasField("n", Receipt.class, "receiptNumber");
	}

	private void aliasLegalDescriptionFields() {
		aliasField("e", OcredEditableElement.class, "edited");
		aliasField("f", OcredEditableElement.class, "filledByOcr");
		aliasField("v", OcredEditableElement.class, "value");
		
		aliasField("i", LegalDescription.class, "imageLink");
	}

	private void aliasTownshipFields() {
		aliasField("a", TownShip.class, "addition");
		aliasField("ab", TownShip.class, "arb");
		aliasField("ar", TownShip.class, "area");
		aliasField("A", TownShip.class, "absNumber");
		aliasField("f", TownShip.class, "firstFractio");
		aliasField("F", TownShip.class, "firstDirection");
		aliasField("p", TownShip.class, "parcel");
		aliasField("q", TownShip.class, "quarterOrder");
		aliasField("Q", TownShip.class, "quarterValue");
		aliasField("r", TownShip.class, "range");
		aliasField("s", TownShip.class, "section");
		aliasField("sf", TownShip.class, "secondFractio");
		aliasField("S", TownShip.class, "secondDirection");
		aliasField("t", TownShip.class, "township");
		aliasField("tt", TownShip.class, "thruTownship");
		aliasField("ts", TownShip.class, "thruSection");
		aliasField("tr", TownShip.class, "thruRange");
		aliasField("tq", TownShip.class, "thruQuarterOrder");
		aliasField("tQ", TownShip.class, "thruQuarterValue");
		aliasField("T", TownShip.class, "thruParcel");
	}

	private void aliasSubdivisionFields() {
		
		aliasField("a", Subdivision.class, "acreage");
		aliasField("b", Subdivision.class, "block");
		aliasField("B", Subdivision.class, "blockThrough");
		aliasField("bu", Subdivision.class, "building");
		aliasField("c", Subdivision.class, "code");
		aliasField("l", Subdivision.class, "lot");
		aliasField("L", Subdivision.class, "lotThrough");
		aliasField("n", Subdivision.class, "name");
		aliasField("N", Subdivision.class, "ncbNumber");
		aliasField("nu", Subdivision.class, "number");
		aliasField("p", Subdivision.class, "platDescription");
		aliasField("pb", Subdivision.class, "platBook");
		aliasField("pp", Subdivision.class, "platPage");
		aliasField("P", Subdivision.class, "platInstrument");
		aliasField("ph", Subdivision.class, "phase");
		aliasField("t", Subdivision.class, "tract");
		aliasField("u", Subdivision.class, "unit");
		
	}

	private void aliasLegalFields() {
		aliasField("f", Legal.class, "freeForm");
		aliasField("p", Legal.class, "partialLegal");
		aliasField("s", Legal.class, "subdivision");
		aliasField("t", Legal.class, "townShip");
	}

	private void aliasAddressFields() {
		aliasField("b", com.stewart.ats.base.address.Address.class, "building");
		aliasField("c", com.stewart.ats.base.address.Address.class, "city");
		aliasField("C", com.stewart.ats.base.address.Address.class, "county");
		aliasField("f", com.stewart.ats.base.address.Address.class, "freeform");
		aliasField("F", com.stewart.ats.base.address.Address.class, "fractio");
		aliasField("n", com.stewart.ats.base.address.Address.class, "number");
		aliasField("i", com.stewart.ats.base.address.Address.class, "identifierNumber");
		aliasField("I", com.stewart.ats.base.address.Address.class, "identifierType");
		aliasField("p", com.stewart.ats.base.address.Address.class, "postDirection");
		aliasField("P", com.stewart.ats.base.address.Address.class, "preDiretion");
		aliasField("s", com.stewart.ats.base.address.Address.class, "suffix");
		aliasField("S", com.stewart.ats.base.address.Address.class, "streetName");
		aliasField("st", com.stewart.ats.base.address.Address.class, "state");
		aliasField("t", com.stewart.ats.base.address.Address.class, "thruNumber");
		aliasField("z", com.stewart.ats.base.address.Address.class, "zip");
	}

	private void aliasPropertyFields() {
		aliasField("a", Property.class, "address");
		aliasField("ac", Property.class, "acres");
		aliasField("ar", Property.class, "areaCode");
		aliasField("A", Property.class, "areaName");
		aliasField("d", Property.class, "district");
		aliasField("le", Property.class, "legal");
		aliasField("m", Property.class, "municipalJurisdiction");
		aliasField("o", Property.class, "owner");
		aliasField("pi", Property.class, "pin");
		aliasField("pt", Property.class, "propertyType");
		aliasField("t", Property.class, "thruAreaCode");
		aliasField("T", Property.class, "type");
		aliasField("y", Property.class, "yearBuilt");
	}

	/**
	 * Aliases fields for class <b>ro.cst.tsearch.bean.TSDIndexPage</b>
	 */
	@SuppressWarnings("deprecation")
	private void aliasTSDIndexPageFields() {
		aliasField("a", TSDIndexPage.class, "mAssesor");
		aliasField("y", TSDIndexPage.class, "mCity");
		aliasField("c", TSDIndexPage.class, "mChapters");
		aliasField("C", TSDIndexPage.class, "mCounty");
		aliasField("o", TSDIndexPage.class, "mOther");
		aliasField("r", TSDIndexPage.class, "mRegister");
		aliasField("s", TSDIndexPage.class, "searchId");
		aliasField("t", TSDIndexPage.class, "tsdColorsMap");
	}

	private void aliasInstrumentPropertyFields() {
		aliasField("b", Instrument.class, "bookPageType");
		aliasField("B", Instrument.class, "bookNo");
		aliasField("e", Instrument.class, "extraInfoMap");
		aliasField("E", Instrument.class, "extraInstrType");
		aliasField("g", Instrument.class, "grantees");
		aliasField("G", Instrument.class, "grantors");
		aliasField("i", Instrument.class, "instrumentRefType");
		aliasField("I", Instrument.class, "instrType");
		aliasField("m1", Instrument.class, "mPIS");
		aliasField("m", Instrument.class, "mCrossReferences");
		aliasField("m2", Instrument.class, "msGrantor");
		aliasField("m3", Instrument.class, "msGrantee");
		aliasField("m4", Instrument.class, "msInstrumentNo");
		aliasField("M", Instrument.class, "mdMortgageValue");
		aliasField("m5", Instrument.class, "mdTaxValue");
		aliasField("m6", Instrument.class, "mDocType");
		aliasField("m7", Instrument.class, "mdTransValue");
		aliasField("m8", Instrument.class, "mFileDate");
		aliasField("m9", Instrument.class, "mInstrumentDate");
		aliasField("o", Instrument.class, "origin");
		aliasField("O", Instrument.class, "overwrite");
		aliasField("p", Instrument.class, "parcelID");
		aliasField("P", Instrument.class, "pageNo");
		aliasField("r", Instrument.class, "realdoctype");
	}

	private void aliasParsedResponseFields() {
		aliasField("f", ParsedResponse.class, "footer");
		aliasField("F", ParsedResponse.class, "fileName");
		aliasField("i1", ParsedResponse.class, "infSets");	//TO EXPAND
		aliasField("i2", ParsedResponse.class, "imageLinks");	//TO EXPAND
		aliasField("i", ParsedResponse.class, "infVectorSets");	//TO EXPAND
		aliasField("I1", ParsedResponse.class, "isParentSite");
		aliasField("I", ParsedResponse.class, "initialResultsCount");
		aliasField("h", ParsedResponse.class, "header");
		aliasField("m1", ParsedResponse.class, "mPAS");
		aliasField("m2", ParsedResponse.class, "mODS");
		aliasField("m3", ParsedResponse.class, "mTHS");
		aliasField("m4", ParsedResponse.class, "mTIS");
		aliasField("m5", ParsedResponse.class, "mSAS");
		aliasField("m6", ParsedResponse.class, "mPIS");
		aliasField("m7", ParsedResponse.class, "mCDIS");
		aliasField("m8", ParsedResponse.class, "mSDS");
		aliasField("m9", ParsedResponse.class, "mOIS");
		aliasField("m0", ParsedResponse.class, "mPAS");
		aliasField("M1", ParsedResponse.class, "mGrantor");
		aliasField("M2", ParsedResponse.class, "mGrantee");
		aliasField("M", ParsedResponse.class, "mCrossRef");
		aliasField("m", ParsedResponse.class, "mSkippedResultRow");
		aliasField("M3", ParsedResponse.class, "mbSolved");
		aliasField("M4", ParsedResponse.class, "mResultRow");
		aliasField("n", ParsedResponse.class, "nextLink");
		aliasField("p", ParsedResponse.class, "pageLink");
		aliasField("r", ParsedResponse.class, "resultsCount");
		aliasField("R", ParsedResponse.class, "resultsSkippedCount");
	}

	@SuppressWarnings("deprecation")
	private void aliasChapterFields() {
		aliasField("a", Chapter.class, "AMOUNTPAID");
		aliasField("b", Chapter.class, "BOOK");
		aliasField("B", Chapter.class, "BASEAMOUNT");
		aliasField("c", Chapter.class, "CONSIDERATIONAMOUNT");
		aliasField("c1", Chapter.class, "CHECKBOX");
		aliasField("C", Chapter.class, "CHECKED");
		aliasField("d", Chapter.class, "DOCTYPEABBREV");
		aliasField("D", Chapter.class, "DELIQUENTAMOUNT");
		aliasField("d1", Chapter.class, "DOCUMENTNUMBER");
		aliasField("f", Chapter.class, "FILLED");
		aliasField("F", Chapter.class, "FILLEDTIME");
		aliasField("g1", Chapter.class, "GRANTOR");
		aliasField("G1", Chapter.class, "GRANTEE");
		aliasField("g", Chapter.class, "GRANTEETR");
		aliasField("G", Chapter.class, "GRANTEELANDER");
		aliasField("i4", Chapter.class, "INSTTYPE");
		aliasField("i3", Chapter.class, "INSTNO");
		aliasField("I1", Chapter.class, "isTRANSFER");
		aliasField("i", Chapter.class, "INCLUDEIMAGE");
		aliasField("I", Chapter.class, "INSTRUMENTDATE");
		aliasField("i1", Chapter.class, "isCHANGED");
		aliasField("i2", Chapter.class, "isDONE");
		aliasField("i5", Chapter.class, "isTransfer");
		aliasField("i6", Chapter.class, "IMAGEPATH");
		aliasField("l", Chapter.class, "LINK");
		aliasField("m", Chapter.class, "MORTGAGEAMOUNT");
		aliasField("M", Chapter.class, "manualCheck");
		aliasField("n", Chapter.class, "NAME");
		aliasField("p", Chapter.class, "PAGE");
		aliasField("P", Chapter.class, "params");
		aliasField("r", Chapter.class, "RECEIPTNUMBER");
		aliasField("r1", Chapter.class, "RECEIPTDATE");
		aliasField("R", Chapter.class, "RECEIPTAMOUNT");
		aliasField("r2", Chapter.class, "REMARKS");
		aliasField("r3", Chapter.class, "REFERENCES");
		aliasField("s", Chapter.class, "SERVERINSTTYPE");
		aliasField("S", Chapter.class, "SRCTYPE");
		aliasField("t", Chapter.class, "TAXYEAR");
		aliasField("T", Chapter.class, "TYPE");
		aliasField("u", Chapter.class, "uploaded");
	}

	private void aliasAllClasses(){
		alias("a", AbstractQueuedSynchronizer.class);
		alias("a1", AssessorDocument.class);
		alias("A", com.stewart.ats.base.address.Address.class);
		alias("b", boolean.class);
		alias("B", Boolean.class);
		alias("ba", Boolean[].class);
		alias("bd", BigDecimal.class);
		alias("bi", BigInteger.class);
		
		alias("c", Chapter.class);
		alias("c1", Collections.class);
		alias("c2", CompareInstrumentsAfterRecordedDate.class);

		alias("co", Comparator.class);
		alias("C", CrossRefSet.class);
		alias("cs", ChapterSavedData.class);
		alias("d", DocTypes.class);
		alias("d1", Date.class);
		alias("d2", DocumentsManager.class);
		alias("d3", Document.class);
		alias("e", Entry.class);
		alias("f", Family.class);
		alias("g", GranteeSet.class);
		alias("g1", GBManager.class);
		alias("G", GrantorSet.class);
		alias("h", Hashtable.class);
		alias("i", Instrument.class);
		alias("i1", int.class);
		alias("i2", com.stewart.ats.base.document.Instrument.class);
		alias("im", Image.class);
		alias("I", ImageLinkInPage.class);
		alias("l", long.class);
		alias("l1", LinkedHashSet.class);
		alias("l2", LegalDescription.class);
		alias("le", Legal.class);
		alias("L", List.class);
		
		alias("Li", Lien.class);
		
		alias("ma", Map.class);
		alias("m", Mortgage.class);
		alias("m1", MyAtsAttributes.class);
		alias("n", com.stewart.ats.base.name.Name.class);
		alias("nm", NameMortgageGrantee.class);
		alias("o", OtherInformationSet.class);
		alias("od", OwnerDetailsSet.class);
		alias("O", Owner.class);
		alias("p", ParsedResponse.class);
		alias("p1", Party.class);
		alias("pa", Patriots.class);
		alias("pi", PropertyIdentificationSet.class);
		alias("pn", Pin.class);
		alias("pr", Property.class);
		alias("pt", PinType.class);
		alias("ps", PartyNameSet.class);
		alias("P", PropertyAppraisalSet.class);
		alias("Pr", PriorFileDocument.class);
		alias("r", RegisterDocument.class);
		alias("r1", ReentrantLock.class);
		alias("R", Receipt.class);
		alias("ua", UserAttributes.class);
		alias("s", String.class);
		//alias("s1", SynchronizedList.class);
		//alias("s2", SynchronizedCollection.class);
		alias("s3", ServletServerComm.class);
		alias("s4", SearchFlags.class);
		alias("sa", String[].class);
		alias("sd", SaleDataSet.class);
		alias("sr", Search.class);
		alias("ss", SpecialAssessmentSet.class);
		alias("su", Subdivision.class);
		alias("sk", SKLDInstrument.class);
		alias("t", Transfer.class);
		alias("t1", Timestamp.class);
		alias("tw", TownShip.class);
		alias("T", TaxDocument.class);
		alias("ts", TreeSet.class);
		alias("ta", TreeMap.class);
		alias("tx", TaxHistorySet.class);
		alias("v", Vector.class);
		alias("V", VestingInfo.class);
	}
	
	private void aliasSearchFields() {
		aliasField("a", Search.class, "allowGetAgentInfoFromDB");
		aliasField("a1", Search.class, "adrNoChanged");
		aliasField("A", Search.class, "adrNameChanged");
		aliasField("a2", Search.class, "agentId");
		aliasField("a3", Search.class, "allChapters");
		aliasField("b", Search.class, "backgroundSearch");
		aliasField("b1", Search.class, "buyerId");
		aliasField("B", Search.class, "buyerFNameChg");
		aliasField("B1", Search.class, "buyerMNameChg");
		aliasField("B2", Search.class, "buyerLNameChg");
		aliasField("c1", Search.class, "coOwnerFNameChg");
		aliasField("c2", Search.class, "coOwnerMNameChg");
		aliasField("c3", Search.class, "coOwnerLNameChg");
		aliasField("c4", Search.class, "coBuyerFNameChg");
		aliasField("c5", Search.class, "coBuyerMNameChg");
		aliasField("c6", Search.class, "coBuyerLNameChg");
		aliasField("c7", Search.class, "citychecked");
		aliasField("c", Search.class, "cityCheckedParentSite");
		aliasField("C", Search.class, "changesInTSDIndexPageMap");
		aliasField("c8", Search.class, "coDocs");
		aliasField("c9", Search.class, "clickedDocs");
		aliasField("c0", Search.class, "chapters");
		aliasField("C1", Search.class, "crossRefSetMap");
		aliasField("C2", Search.class, "crossRefMap");
		aliasField("C3", Search.class, "chapterMap");
		aliasField("d", Search.class, "disposeTime");
		aliasField("d1", Search.class, "dlDocs");
		aliasField("d2", Search.class, "dtDocs");
		aliasField("d3", Search.class, "docManager");
		aliasField("D", Search.class, "disabledChapters");
		aliasField("e", Search.class, "existingImages");
		aliasField("f", Search.class, "fileProcessed");
		aliasField("g", Search.class, "goBackType");
		aliasField("G", Search.class, "generatedTemp");
		aliasField("h", Search.class, "htmlIndexes");
		aliasField("i", Search.class, "isFakeSearch");
		aliasField("i1", Search.class, "inMemoryDocs");
		aliasField("i2", Search.class, "imagesMap");
		aliasField("ip", Search.class, "imagePath");
		aliasField("it", Search.class, "imageTransactionId");
		aliasField("I", Search.class, "instrumentListRef");
		aliasField("ID", Search.class, "ID");
		aliasField("j", Search.class, "jsSortChaptersBy");
		aliasField("l1", Search.class, "lastTransfer");
		aliasField("L", Search.class, "lastTransferDate");
		aliasField("l", Search.class, "lastTransferInformation");
		aliasField("l2", Search.class, "laDocs");
		aliasField("m2", Search.class, "mapImgs");
		aliasField("m1", Search.class, "mpts");
		aliasField("m", Search.class, "miSearchType");
		aliasField("m3", Search.class, "msP1");
		aliasField("m4", Search.class, "msP2");
		aliasField("m5", Search.class, "msUserPath");
		aliasField("m6", Search.class, "maxContextEvent");
		aliasField("M", Search.class, "mustSaveSearchHTML");
		aliasField("o", Search.class, "ocrVestingInfo");
		aliasField("o1", Search.class, "origSA");
		aliasField("o2", Search.class, "orDocs");
		aliasField("o3", Search.class, "ownerId");
		aliasField("o4", Search.class, "orderedSearch");
		aliasField("o5", Search.class, "ownerFNameChg");
		aliasField("o6", Search.class, "ownerMNameChg");
		aliasField("oc", Search.class, "openCount");
		aliasField("O", Search.class, "ownerLNameChg");
		aliasField("o7", Search.class, "orderNo");
		aliasField("p", Search.class, "patriotSearchName");
		aliasField("p0", Search.class, "propertyId");
		aliasField("p1", Search.class, "pcDocs");
		aliasField("p2", Search.class, "prDocs");
		aliasField("p3", Search.class, "patriotKeyNumber");
		aliasField("p4", Search.class, "parentSearchId");
		aliasField("P", Search.class, "patriotsAlertChapters");
		aliasField("r", Search.class, "removedInstr");
		aliasField("r1", Search.class, "registerData");
		aliasField("R", Search.class, "registerDataComp");
		aliasField("r2", Search.class, "rvDocs");
		aliasField("r3", Search.class, "roDocs");
		aliasField("s", Search.class, "sortAscendingChapters");
		aliasField("s0", Search.class, "searchFlags");
		aliasField("s1", Search.class, "searchStatus");
		aliasField("s2", Search.class, "searchID");
		aliasField("s3", Search.class, "searchStarted");
		aliasField("s4", Search.class, "searchState");
		aliasField("s5", Search.class, "showAllChapters");
		aliasField("s6", Search.class, "searchCycle");
		aliasField("s7", Search.class, "searchDirSuffix");
		aliasField("s8", Search.class, "savedInstruments");
		aliasField("s9", Search.class, "startDate");
		aliasField("S", Search.class, "savedTSDIndexState");
		aliasField("t", Search.class, "TS_SEARCH_STATUS");
		aliasField("t1", Search.class, "TEMP_DIR");
		aliasField("t2", Search.class, "TSDFileName");
		aliasField("t3", Search.class, "tsdIndexPage");
		aliasField("t4", Search.class, "tsuNo");
		aliasField("t5", Search.class, "timings");
		aliasField("t6", Search.class, "temp_dir");
		aliasField("t7", Search.class, "tsrLink");
		aliasField("T", Search.class, "TSROrderDate");
		aliasField("u", Search.class, "updatedTSDChapter");
		aliasField("U", Search.class, "uccDocs");
		aliasField("u1", Search.class, "update");
		aliasField("v1", Search.class, "visitedDocs");
		aliasField("v2", Search.class, "visitedLinks");
		aliasField("v", Search.class, "validatedLinks");
		aliasField("v3", Search.class, "versionSaveNo");
		aliasField("V", Search.class, "vecChaptKeysSorted");
		aliasField("w", Search.class, "wasOpened");
		
	}
	
	private void aliasDocumentManagerFields(){
		aliasField("c", DocumentsManager.class, "colored");
		aliasField("C", DocumentsManager.class, "curentUploadNumber");
		aliasField("d", DocumentsManager.class, "documents");
		aliasField("eV", DocumentsManager.class, "endViewDate");
		aliasField("i", DocumentsManager.class, "idList");
		aliasField("L", DocumentsManager.class, "lastMortgageForOwner");
		aliasField("n", DocumentsManager.class, "numberOfOcrInProgress");
		aliasField("o", DocumentsManager.class, "outOfRangeDocuments");
		aliasField("s", DocumentsManager.class, "searchId");
		aliasField("s1", DocumentsManager.class, "startViewDate");
		aliasField("S", DocumentsManager.class, "sortBy");
		aliasField("t", DocumentsManager.class, "tempDocuments");
	}
	
	private void aliasDocumentFields(){
		aliasField("a", Document.class, "allSSN");
		aliasField("b", Document.class, "boilerPlatesCode");
		aliasField("c", Document.class, "checked");
		aliasField("d", Document.class, "description");
		aliasField("D", Document.class, "dataSource");
		aliasField("f", Document.class, "fake");
		aliasField("g", Document.class, "granteeFreeForm");
		aliasField("G", Document.class, "grantorFreeFrorm");
		aliasField("id", Document.class, "id");
		aliasField("i", Document.class, "includeImage");
		aliasField("im", Document.class, "image");
		aliasField("I", Document.class, "indexId");
		aliasField("i1", Document.class, "instrument");
		aliasField("k", Document.class, "keptJustGoodReferences");
		aliasField("m", Document.class, "manualChecked");
		aliasField("M", Document.class, "marked");
		aliasField("n", Document.class, "note");
		aliasField("o", Document.class, "oldIndexPath");
		aliasField("p", Document.class, "parsedReferences");
		aliasField("P", Document.class, "properties");
		aliasField("r", Document.class, "references");
		aliasField("R", Document.class, "remarks");
		aliasField("s", Document.class, "serverDocType");
		aliasField("sa", Document.class, "savedFrom");
		aliasField("s1", Document.class, "siteId");
		aliasField("S", Document.class, "shortServerDocType");
		aliasField("T", Document.class, "tsrIndexColorClass");
		aliasField("t1", Document.class, "type");
		aliasField("u", Document.class, "uploaded");
		aliasField("st", Document.class, "searchType");
	}
	
	/**
	 * Must be careful not to use fields from Document
	 */
	private void aliasAssessorDocumentFields(){
		aliasField("t", Document.class, "totalAssessement");		//specific to AO field
	}
	
	private void aliasTaxDocumentFields() {
		aliasField("ac", TaxDocument.class, "additionalCityDoc");
		aliasField("ad", TaxDocument.class, "amountDue");
		aliasField("ae", TaxDocument.class, "amountDueEP");
		aliasField("ai", TaxDocument.class, "appraisedValueImprovements");
		aliasField("al", TaxDocument.class, "appraisedValueLand");
		aliasField("ap", TaxDocument.class, "amountPaid");
		aliasField("at", TaxDocument.class, "appraisedValueTotal");
		aliasField("ba", TaxDocument.class, "baseAmount");
		aliasField("bn", TaxDocument.class, "billNumber");
		aliasField("bo", TaxDocument.class, "bonds");
		aliasField("B", TaxDocument.class, "baseAmountEP");
		aliasField("dp", TaxDocument.class, "datePaid");
		aliasField("e", TaxDocument.class, "exemptionAmount");
		aliasField("F", TaxDocument.class, "foundDelinquent");
		aliasField("in", TaxDocument.class, "installments");
		aliasField("py", TaxDocument.class, "priorYears");
		aliasField("rc", TaxDocument.class, "receipts");
		aliasField("re", TaxDocument.class, "redemtions");
		aliasField("rr", TaxDocument.class, "researchRequired");
		aliasField("sd", TaxDocument.class, "saleDate");
		aliasField("sn", TaxDocument.class, "saleNo");
		aliasField("sp", TaxDocument.class, "splitPaymentAmount");
		aliasField("ta", TaxDocument.class, "totalAssessment");
		aliasField("td", TaxDocument.class, "totalDelinquentEP");
		aliasField("tt", TaxDocument.class, "taxYearEPfromTR");
		aliasField("tv", TaxDocument.class, "taxVolume");
		aliasField("ty", TaxDocument.class, "taxYearDescription");
	}
	
	private void aliasRegisterDocumentFields(){
		aliasField("dc", RegisterDocument.class, "documentClass");
		aliasField("gr", RegisterDocument.class, "grantor");
		aliasField("ge", RegisterDocument.class, "grantee");
		aliasField("is", RegisterDocument.class, "instrumentDate");
		aliasField("iy", RegisterDocument.class, "instrumentYear");
		aliasField("l", RegisterDocument.class, "legalDescription");
		aliasField("O", RegisterDocument.class, "ocrServerDocType");
	    aliasField("or", RegisterDocument.class, "ocrReferences");
	    aliasField("rd", RegisterDocument.class, "recordedDate");
	    
	    //transfer
	    aliasField("sp", Transfer.class, "salePrice");
		aliasField("ve", Transfer.class, "vestingInfoGrantee");
		aliasField("vr", Transfer.class, "vestingInfoGrantor");
		
		//mortgage
		aliasField("gl", Mortgage.class, "granteeLenderFreeForm");
		aliasField("gt", Mortgage.class, "granteeTrusteeFreeForm");
		aliasField("ln", Mortgage.class, "loanNo");
		aliasField("ma", Mortgage.class, "mortgageAmount");
		aliasField("mf", Mortgage.class, "mortgageAmountFreeForm");
		aliasField("re", Mortgage.class, "released");
		
		
		//lien
		aliasField("ca", Lien.class, "considerationAmount");
		aliasField("cf", Lien.class, "considerationAmountForm");
		
		//patriots
		aliasField("h", Patriots.class, "hit");
		
		//prior files
		aliasField("ex", PriorFileDocument.class, "exceptions");
		aliasField("ag", PriorFileDocument.class, "agency");
		
	}
	
	
	
	private void aliasInstrumentFields(){
		aliasField("b", com.stewart.ats.base.document.Instrument.class, "book");
		aliasField("B", com.stewart.ats.base.document.Instrument.class, "bookType");
		aliasField("d", com.stewart.ats.base.document.Instrument.class, "disableBookPage");
		aliasField("D", com.stewart.ats.base.document.Instrument.class, "disableDocNo");
		aliasField("d1", com.stewart.ats.base.document.Instrument.class, "disableInstrNo");
		aliasField("d2", com.stewart.ats.base.document.Instrument.class, "docno");
		aliasField("d3", com.stewart.ats.base.document.Instrument.class, "docSubType");
		aliasField("d4", com.stewart.ats.base.document.Instrument.class, "docType");
		aliasField("i", com.stewart.ats.base.document.Instrument.class, "instrumentEdited");
		aliasField("I", com.stewart.ats.base.document.Instrument.class, "instno");
		aliasField("o", com.stewart.ats.base.document.Instrument.class, "overwritePermission");
		aliasField("p", com.stewart.ats.base.document.Instrument.class, "page");
		aliasField("y", com.stewart.ats.base.document.Instrument.class, "year");
	}
	
	private void aliasPartyFields(){
		aliasField("f", Party.class, "freeParsedForm");
		aliasField("n", Party.class, "nameSet");
		aliasField("t", Party.class, "type");
	}
	
	private void aliasNameFields(){
		aliasField("c", Name.class, "company");
		aliasField("f", Name.class, "firstName");
		aliasField("l", Name.class, "lastName");
		aliasField("m", Name.class, "middleName");
		aliasField("p", Name.class, "prefix");
		aliasField("s", Name.class, "sufix");
		aliasField("S", Name.class, "ssn4Encoded");
		aliasField("t", Name.class, "tokenized");
		
		aliasField("i", NameMortgageGrantee.class, "isTrustee");
	}
	
	
	
	private void alias(String name, Class c){
		xstream.alias(name, c);
		xstreamCompatible.alias(name, c);
	}
	
	private void aliasField(String alias, Class c, String field){
		xstream.aliasField(alias, c, field);
		xstreamCompatible.aliasField(alias, c, field);
	}
	/*
	private void aliasAttribute(String alias, String attributeName){
		xstream.aliasAttribute(alias, attributeName);
		xstreamCompatible.aliasAttribute(alias, attributeName);
	}
	*/
	private static class SingletonHolder {
		private static XStreamManager instance = new XStreamManager();
		
	} 
	
	public static XStreamManager getInstance() {
		XStreamManager crtInstance = SingletonHolder.instance;
		return crtInstance;
	}
	
	/**
	 * Serialize an object to a pretty printed xml file and uses no aliases
	 * @param o the object to be serialized 
	 * @param outputFile the name of the output file
	 */
	public void prettyPrint(Object o, String outputFile){
		try {
			Writer writer = new BufferedWriter(new FileWriter(outputFile));	
			PrettyPrintWriter cw = new PrettyPrintWriter(writer);
			marshalFull(o, cw);
			cw.close();	   
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void marshalSearch(Search toSerialize, HierarchicalStreamWriter writer){
		if(toSerialize==null)
			return;
		//toSerialize.getSa().setObjectAtribute(SearchAttributes.RO_CROSS_REF_INSTR_LIST, new ArrayList<Instrument>());
		if(toSerialize.getOrigSA() != null){
			//toSerialize.getOrigSA().setObjectAtribute(SearchAttributes.RO_CROSS_REF_INSTR_LIST, new ArrayList<Instrument>());
		}
		
		Map allInMemoryDocCopy = toSerialize.getAllInMemoryDocCopy();
		boolean restoreCopy = allInMemoryDocCopy.size() > 0;
		if(restoreCopy) {
			toSerialize.removeAllInMemoryDocs();
		}
		marshal(toSerialize, writer);
		if(restoreCopy) {
			for (Object key : allInMemoryDocCopy.keySet()) {
				if(key instanceof String) {
					toSerialize.addInMemoryDoc((String)key, allInMemoryDocCopy.get(key));
				}
			}
		}
	}
	
	public void marshal(Object toSerialize, HierarchicalStreamWriter writer){
		xstream.marshal(toSerialize, writer);
	}
	
	/**
	 * Serializes an object without any aliases
	 * @param toSerialize
	 * @param writer
	 */
	public void marshalFull(Object toSerialize, HierarchicalStreamWriter writer){
		xstreamNoAlias.marshal(toSerialize, writer);
	}
	

	/**
	 * Tries to read an object from the specified xml
	 * @param xml
	 * @return
	 */
	public Object fromXML(String xml) {
		Object obj = null;
		try {
			obj = xstream.fromXML(xml);
		} catch (Throwable e) {
			if( obj == null ){
				System.err.println("Failed to open search...");
				e.printStackTrace();
				System.err.println("Trying old compatibility ...");
				try {
					obj = xstreamCompatible.fromXML(xml);
				} catch (Exception e1) {
					if(obj == null){
						System.err.println("Failed to open search completely...");
						e.printStackTrace();
						System.err.println("Bad error!!!");
					}
				}
				
			}
		}
		
		
		if(obj!=null && obj instanceof Search){
			Search global = (Search)obj;
			updateSearch(global);
		}
		
		return obj;
	}
	
	
	private void updateSearch(Search global) {
		
		//this is needed to initialize an unused field
		global.initOnDeserialization();
		
		long searchId = global.getID();
		SearchAttributes sa = global.getSa();
		PartyI ownerParty = sa.getOwners();
		PartyI buyerParty = sa.getBuyers();
		if(ownerParty.getType() == null)
			ownerParty.setType(PType.GRANTOR);
		if(buyerParty.getType() == null)
			buyerParty.setType(PType.GRANTEE);
		
		if (global.getDocManager() == null) {		
			//don't read old keys unless this order is so old that it doesn't even have docmanager
			String names[] = null;
			NameI name = null;
			if (StringUtils.isNotEmpty(sa.getAtribute(SearchAttributes.OWNER_LNAME))) {
				names = GenericFunctions.extractSuffix(sa.getAtribute(SearchAttributes.OWNER_MNAME).trim());
				name = new com.stewart.ats.base.name.Name(
						sa.getAtribute(SearchAttributes.OWNER_FNAME), 
						names[0], 
						sa.getAtribute(SearchAttributes.OWNER_LNAME));
				name.setSufix(names[1]);
				
				ownerParty.add(name);
			}
			
			if(StringUtils.isNotEmpty(sa.getAtribute("OWNER_SPOUSE_LNAME"))) {
				names = GenericFunctions.extractSuffix(sa.getAtribute("OWNER_SPOUSE_MNAME").trim());
				name = new com.stewart.ats.base.name.Name(
						sa.getAtribute("OWNER_SPOUSE_FNAME"), 
						names[0], 
						sa.getAtribute("OWNER_SPOUSE_LNAME"));
				name.setSufix( names[1] );
				if (!ExactNameFilter.isMatchGreaterThenScore(sa.getOwners().getNames(), name, 0.99))
					ownerParty.add(name);
			}
			if (StringUtils.isNotEmpty(sa.getAtribute(SearchAttributes.BUYER_LNAME))) {
				names = GenericFunctions.extractSuffix(sa.getAtribute(SearchAttributes.BUYER_MNAME).trim());
				name = new com.stewart.ats.base.name.Name(
						sa.getAtribute(SearchAttributes.BUYER_FNAME),
						names[0], 
						sa.getAtribute(SearchAttributes.BUYER_LNAME));
				name.setValidated(true);
				name.setSufix( names[1] );
				buyerParty.add(name);
			}
			if(StringUtils.isNotEmpty(sa.getAtribute("BUYER_SPOUSE_LNAME"))) {
				names = GenericFunctions.extractSuffix(sa.getAtribute("BUYER_SPOUSE_MNAME").trim());
				name = new com.stewart.ats.base.name.Name(
						sa.getAtribute("BUYER_SPOUSE_FNAME"), 
						names[0], 
						sa.getAtribute("BUYER_SPOUSE_LNAME"));
				name.setSufix( names[1] );
				name.setValidated(true);
				if (!ExactNameFilter.isMatchGreaterThenScore(sa.getBuyers().getNames(), name, 0.99)) {
					buyerParty.add(name);
				}
			}
		} else {
			
			boolean atLeasOneBuyer = false;
			Set<NameI> buyerNames = buyerParty.getNames();
			for (NameI name : buyerNames) {
				if (name instanceof Name) {
					Name nameObject = (Name) name;
					if(nameObject.isValidatedObject() == null) {
						nameObject.setValidated(true);
						atLeasOneBuyer = true;
					}
				}
			}
			if(atLeasOneBuyer) {
				buyerParty.setNames(buyerNames);
			}
			
		}
		
		//if this search is old we need to created the new document structures
		if( global.getDocManager() == null) {
			DocumentsManager docManager = new DocumentsManager(global.getID());
			CurrentInstance ci = new CurrentInstance();
			ci.setCrtSearchContext(global);
			InstanceManager.getManager().setCurrentInstance(searchId, ci);
			
			try { 
				docManager.getAccess();
			
				global.setDocManager(docManager);
				Map registerMap = global.getRegisterMap();
				ParsedResponse prd = null;
				
				if(registerMap != null) {
					Set<String> keys = registerMap.keySet();
					for (String chapterKey : keys) {
						if( (prd = (ParsedResponse)registerMap.get(chapterKey)) != null) {
							try {
								if ( chapterKey.contains("Other") && chapterKey.contains("patriots") ) {
									//do some shit....
									global.setPatriotKeyNumber(((Chapter)global.getChaptersMap().get(chapterKey)).INSTNO);
								} else {
									DocumentI doc = Bridge.fillDocument(null, prd, searchId);
									try {
										if(prd.getImageLinksCount() > 0) {
											if(global.getImagesMap().get(prd.getImageLink(0)) != null) {
												Object serverObject = global.getImagesMap().get(prd.getImageLink(0));
												if (serverObject instanceof Integer) {
													doc.setSiteId((Integer) serverObject);
												} else if (serverObject instanceof TSServer) {
													doc.setSiteId(((TSServer) serverObject).getServerID());
												}
											}
										}
										String[] chapterAsString = global.getFromChapterMap(chapterKey);
										if(chapterAsString != null && doc instanceof RegisterDocumentI) {
											RegisterDocumentI regDoc = (RegisterDocumentI)doc;
											if(chapterAsString.length > TSDIndexPage.CHAPTER_SRCTYPE) {
												String source = chapterAsString[TSDIndexPage.CHAPTER_SRCTYPE];
												if(source != null){
													if(source.equals("UP")){
														doc.setDataSource("UP");
														doc.setUploaded(true);
														String imagePath = chapterAsString[TSDIndexPage.CHAPTER_IMAGE_PATH];
														if(imagePath != null) {
															try {
																File srcFile = new File(imagePath);
																if(srcFile.exists()) {
																	String type = ro.cst.tsearch.utils.FileUtils.getFileExtension(imagePath);
																	String destFileName = global.getImageDirectory() + File.separator + 
																		doc.getId() + type;
																	File destFile = new File(destFileName);
																	FileUtils.copyFile(srcFile, destFile);
																	if(destFile.exists()) {
																		UploadImage.updateImage(
																				doc, 
																				destFileName, 
																				destFile.getName(), 
																				type.substring(1), 
																				searchId);
																		doc.setImageUploaded(true);
																	}
																}
															} catch (Exception e) {
																e.printStackTrace();
															}
														}
													} else if(source.equals("IP")){
														regDoc.setSearchType(SearchType.IP);
													}
												}
											}
										}
										
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
								File htmlFile = new File(chapterKey);
								String htmlContent = "No Content Available";
								if(htmlFile.exists())
									try {
										htmlContent = FileUtils.readFileToString(htmlFile);
									} catch (IOException e) {
										e.printStackTrace();
									}
								global.addDocument(chapterKey, (ParsedResponse)prd, htmlContent);
							} catch (Exception e) {
								System.err.println("Problem with chapter key = " + chapterKey);
								e.printStackTrace();
							}
						}
					}
				}
				
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					docManager.releaseAccess();
					InstanceManager.getManager().removeCurrentInstance(searchId);					
				}
		} else {
			DocumentsManagerI docManager = global.getDocManager();
			try { 
				docManager.getAccess();
				//before PRIORFILE_CHANGE we need to update old categories
				if(global.getVersionSearchNo() < Search.SEARCH_VERSION.PRIORFILE_CHANGE.getVersion()) {
					
					//keep the next 4 lines because we need compatibility with older searches before 6414
					List<DocumentI> documentsWithDataSource = docManager.getDocuments("PRIORFILES");
					for (DocumentI documentI : documentsWithDataSource) {
						documentI.setDocType("OTHER-FILE");
					}
					
					List<DocumentI> sfPriors = docManager.getDocuments("SFPRIORFILES");
					for (DocumentI documentI : sfPriors) {
						if(documentI.getDocSubType().startsWith("Prior")) {
							documentI.setDocSubType("Prior File");
						} else if (documentI.getDocSubType().startsWith("Base")) {
							documentI.setDocSubType("Base File");
						}
						documentI.setDocType(DocumentTypes.PRIORFILE);
					}
					
					List<DocumentI> otherFiles = docManager.getDocuments("OTHER-FILE");
					for (DocumentI documentI : otherFiles) {
						if(documentI.getDocSubType().startsWith("Prior")) {
							documentI.setDocSubType("Prior File");
							documentI.setDocType(DocumentTypes.PRIORFILE);
						} else if (documentI.getDocSubType().startsWith("Base")) {
							documentI.setDocSubType("Base File");
							documentI.setDocType(DocumentTypes.PRIORFILE);
						} else if (documentI.getDocSubType().startsWith("Mortgage")) {
							documentI.setDocSubType("Lender Policy");
							documentI.setDocType(DocumentTypes.PRIORFILE);
						} else if (documentI.getDocSubType().startsWith("Owner")
								|| documentI.getDocSubType().startsWith("Lender")
								|| documentI.getDocSubType().startsWith("Starter")) {
							documentI.setDocType(DocumentTypes.PRIORFILE);
						}
					}
				}
				
				if(global.getVersionSearchNo() < Search.SEARCH_VERSION.PRIOR_BASE_SERCH_CORRECTION.getVersion()) {
					List<DocumentI> atsDocs = docManager.getDocumentsWithDataSource(false, "ATS");
					for (DocumentI documentI : atsDocs) {
						if(documentI.getDocSubType().equals("Prior File")) {
							documentI.setDocSubType(DocumentTypes.PRIORFILE_PRIOR_SEARCH);
						} else if(documentI.getDocSubType().equals("Base File")) {
							documentI.setDocSubType(DocumentTypes.PRIORFILE_BASE_SEARCH);
						}
					}
				}
				
				if (global.getVersionSearchNo() < Search.SEARCH_VERSION.KS_JOHNSON_AOM_TO_AM.getVersion()) {
					List<DocumentI> aomDocs = docManager.getDocumentsWithDataSource(false, "AOM");
					int siteId = (int)TSServersFactory.getSiteIdfromCountyandServerTypeId(CountyConstants.KS_Johnson, GWTDataSite.AM_TYPE) + 1;
					for (DocumentI documentI : aomDocs) {
						documentI.setSiteId(siteId);
					}
				}
				
				for (DocumentI documentI : docManager.getDocumentsList()) {
					ImageI image = documentI.getImage();
					if(image != null && image.getViewCount() == null) {
						if(image.getSsfLink().isEmpty()) {
							documentI.getImage().setViewCount(new AtomicInteger());
						} else {
							documentI.getImage().setViewCount(new AtomicInteger(1));
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				docManager.releaseAccess();
			}
		}
		//be sure to clean unused data
		global.getRegisterMap().clear();
		if(global.getVersionSearchNo() < Search.SEARCH_VERSION.PRIORFILE_CHANGE.getVersion()) {
			if(StateContants.FL_STRING.equals(sa.getAtribute(SearchAttributes.P_STATE)) &&
					(sa.getAtribute(SearchAttributes.CERTICICATION_DATE) == null || 
							sa.getAtribute(SearchAttributes.CERTICICATION_DATE).equals("1/1/1"))){
				Calendar cal = Calendar.getInstance();
				if(DBManager.getTSRGenerationStatus(global.getID()) == Search.SEARCH_TSR_CREATED) {
					Date tsrDate = DBManager.getTSRCreationDate(global.getID());
					cal.setTime(tsrDate);
				}
				cal.add(Calendar.DAY_OF_MONTH, -14);
				sa.setAtribute(SearchAttributes.CERTICICATION_DATE, 
						SearchAttributes.DATE_FORMAT_MMddyyyy.format(cal.getTime()));
			}
		}
		boolean saLogInDatabase = sa.isLogInDatabase();
		if(!saLogInDatabase) {
			saLogInDatabase = AsynchSearchLogSaverThread.isLogOnSamba(searchId);
			if(saLogInDatabase) {
				sa.setObjectAtribute(SearchAttributes.INTERNAL_LOG_ORIGINAL_LOCATION, 
						(ServerConfig.isEnableLogInSamba())?
								new Integer(ServerConfig.getLogInTableVersion()):
								new Integer(0));
			}
		}
		if(!saLogInDatabase) {
			//now, we need to copy the log to the shared drive
			byte[] searchOrderLogs = DBManager.getSearchOrderLogs(searchId, FileServlet.VIEW_LOG_OLD_STYLE, true);
			String sambaFolderForSearch = AsynchSearchLogSaverThread.getSambaFolderForSearch(searchId);
			
			try {
			
				if(searchOrderLogs == null) {
					//nothing found, not good, but what can one do... except send an email with the error
					Log.sendEmail(
							MailConfig.getMailLoggerToEmailAddress(), 
							"Not log found on database or archive", 
							"SearchId used: " + searchId + ", folder checked: " + sambaFolderForSearch);
					
					sa.setObjectAtribute(SearchAttributes.INTERNAL_LOG_ORIGINAL_LOCATION, 
							(ServerConfig.isEnableLogInSamba())?
									new Integer(ServerConfig.getLogInTableVersion()):
									new Integer(0));
					DBManager.getSimpleTemplate().update(
							"UPDATE " + DBConstants.TABLE_SEARCH_FLAGS + " set " + DBConstants.FIELD_SEARCH_FLAGS_LOG_ORIGINAL_LOCATION + " = ? where " + DBConstants.FIELD_SEARCH_FLAGS_ID + " = ?", 
							sa.getLogOriginalLocation(), searchId);
					
				} else {
					
					String[] list = null;
					File folder = new File(sambaFolderForSearch);
					if(folder.isDirectory()) {
						list = folder.list();
					}
					if(list == null || list.length == 0) {
						//do nothing, this is expected
					} else {
						//cannot happen, show not happen, but if it does....
						//delete all of them :)
						for (String fileName : list) {
							try {
								FileUtils.deleteQuietly(new File(sambaFolderForSearch + fileName));
							} catch (Exception e) {
								Log.sendExceptionViaEmail(
										MailConfig.getMailLoggerToEmailAddress(), 
										"Cannot delete log path ", 
										e, 
										"SearchId used: " + searchId + ", path used: " + sambaFolderForSearch + fileName);
							}
						}
						
					}
					
					String fullPath = sambaFolderForSearch + "logFile_mixed.html";
					try {
						FileUtils.writeByteArrayToFile(new File(fullPath), searchOrderLogs);
						
						sa.setObjectAtribute(SearchAttributes.INTERNAL_LOG_ORIGINAL_LOCATION, 
								(ServerConfig.isEnableLogInSamba())?
										new Integer(ServerConfig.getLogInTableVersion()):
										new Integer(0));
						DBManager.getSimpleTemplate().update(
								"UPDATE " + DBConstants.TABLE_SEARCH_FLAGS + " set " + DBConstants.FIELD_SEARCH_FLAGS_LOG_ORIGINAL_LOCATION + " = ? where " + DBConstants.FIELD_SEARCH_FLAGS_ID + " = ?", 
								sa.getLogOriginalLocation(), searchId);
						//on error do not set flag, maybe next open will fix it
					} catch (Exception e) {
						AsynchSearchLogSaverThread.getLogger().error("Write to samba failed for path " + fullPath + " when opening search " + searchId, e);
						Log.sendExceptionViaEmail(
								MailConfig.getMailLoggerToEmailAddress(), 
								"Logging to Samba failed when opening search " + searchId, 
								e, 
								"SearchId used: " + searchId + ", path used: " + fullPath);
					}
				}
				
			} catch (Exception e) {
				
			}
			
		}
		
		
		//we finished with updates
		if(global.getVersionSearchNo() < Search.SEARCH_VERSION.getCurrentVersion()) {
			global.updateVersionSearchNo();					
		}
		
	}

	private void omitField(Class c, String fieldName){
		xstream.omitField(c, fieldName);
		xstreamCompatible.omitField(c, fieldName);
	}
	
	/**
	 * Takes a class and only serialize the given fields if the exist in that class
	 * @param c
	 * @param fieldsToBeSerialized 
	 */
	@SuppressWarnings("unused")
	private void serializeJustSomeFields(Class c, HashSet<String> fieldsToBeSerialized){
		Field[] fields = c.getFields();
		for (int i = 0; i < fields.length; i++) {
			if(!fieldsToBeSerialized.contains(fields[i].getName())){
				omitField(c, fields[i].getName());
			}
		}
		
	}
	

	public String toXML(Object o) {
		if(o==null)
			return "";
		String result = "";
		try {
			result = xstream.toXML(o);
		} catch (Exception e) {
			try {
				result = xstreamCompatible.toXML(o);
			}
			catch (Exception e1) {
				return "";
			}
		}
		return result;
	}
	
	public static void main(String[] args) {
		XStreamManager manager = XStreamManager.getInstance();
		Search fromXML = null;
		try {
			fromXML = (Search)manager.fromXML(FileUtils.readFileToString(new File("D:\\bugs\\search_context\\13553134 - Copy\\__search.xml")));
			
			CompactWriter cw = new CompactWriter(new BufferedWriter(new FileWriter("D:\\bugs\\search_context\\13553134 - Copy\\__search2.xml")));
			
			manager.marshalSearch(fromXML, cw);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(fromXML);
	}
	
}
