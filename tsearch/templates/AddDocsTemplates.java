/*
 * Created on Apr 16, 2004
 *
 */
package ro.cst.tsearch.templates;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import ro.cst.tsearch.DataSources;
import ro.cst.tsearch.LoadConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.StateContants;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.CommunityTemplatesMapper;
import ro.cst.tsearch.database.rowmapper.SearchUserTimeMapper;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.reports.data.AbstractorWorkedTime;
import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servlet.TemplatesServlet;
import ro.cst.tsearch.servlet.community.UploadPolicyDoc;
import ro.cst.tsearch.templates.MultilineElementsMap.DocumentsTags;
import ro.cst.tsearch.templates.MultilineElementsMap.PartyType;
import ro.cst.tsearch.templates.edit.client.InstrumentStructForUndefined;
import ro.cst.tsearch.templates.edit.client.TemplateUtils;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.titledocument.abstracts.TaxUtilsNew;
import ro.cst.tsearch.user.MyAtsAttributes;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RegExUtils;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.document.AssessorDocumentI;
import com.stewart.ats.base.document.AssessorManagementDocumentI;
import com.stewart.ats.base.document.BoilerPlateObject.BPType;
import com.stewart.ats.base.document.CourtI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.HOACondoI;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.LienI;
import com.stewart.ats.base.document.MortgageI;
import com.stewart.ats.base.document.PriorFileDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.TaxDocumentI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.misc.SelectableStatement;
import com.stewart.ats.base.name.NameFormaterI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.base.taxutils.Installment;
import com.stewart.ats.base.taxutils.TaxRedemptionI;
import com.stewart.ats.base.warning.WarningUtils;
import com.stewart.ats.tsrindex.client.HOAInfo;
import com.stewart.ats.tsrindex.client.Receipt;
import com.stewart.ats.tsrindex.client.SimpleChapter;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;
import com.stewart.ats.tsrindex.client.template.TemplatesInitUtils;
import com.sun.star.lang.DisposedException;

public class AddDocsTemplates {
	
	private static final String $SEPARATOR_REG_EX = "(?s)%\\$SEPARATOR=(.*)\\$%";
	public static final String KEY_SEPARATOR_FOR_TAG = "?_?_?";
	protected static final Logger				logger								= Logger.getLogger(AddDocsTemplates.class);
	public static final HashMap<String,String> docDocumentsExtensions = new HashMap<String,String>();
	public static final int LEGAL_COMMID = -4;
	public static final String LEGAL_FOLDER_NAME = "legal";
	public static final String LEGAL_TEMPLATE_NAME = "defaultLd.txt";
	public static final String LEGAL_CONDO_TEMPLATE_NAME = "defaultLdCondo.txt";
	
	public static final long BASE_FILE_TEMPLATE_ID = Integer.MAX_VALUE-1;
	public static final int BASE_FILE_COMMID = -5;
	public static final String BASE_FILE_FOLDER_NAME = "legal";
	
	static{
		docDocumentsExtensions.put("doc", "Microsoft Office 97 document file");
	}
	
	private static Hashtable<String,String> daslCacheRequests = new Hashtable<String,String>();
	
	static final HashMap<String,String> aimEscapeCharacters=new HashMap <String,String>();
	
	static{
		aimEscapeCharacters.put( "'", "&apos;" );
		
		aimEscapeCharacters.put( "<", "&lt;" );
		
		aimEscapeCharacters.put( ">", "&gt;" );
		
		aimEscapeCharacters.put( "\"", "&quot;" );
		
		aimEscapeCharacters.put( "" + ((char) 186) , "&#176;" );//"�"
		
		aimEscapeCharacters.put( "" + ((char) 176) , "&#176;" ); //'�'
		
		aimEscapeCharacters.put( "" + ((char) 146) , "&apos;");//�
	}
	
	static final String IF_EXPRESION="\\{ *IF *( *[^#{}\n\r]* *) *\\}";
	static final String ELSE_EXPRESION="\\{ *ELSE *\\}";
	static final String ENDIF_EXPRESION="\\{ *ENDIF *\\}";
	static final String IF_ELSE_ENDIF_EXPRESION="("+IF_EXPRESION +")|("+ELSE_EXPRESION+ ")|(" +ENDIF_EXPRESION+")";
	static final String EXPRESION_BLOCK="("+IF_EXPRESION+"[^{}]*)|("+
								ELSE_EXPRESION+"[^{}]*)|("+
								ENDIF_EXPRESION+"[^{}]*)";
	
	static final String IF_EXPR_DOCUMENTS ="\\{\\$IF *\\(([^)]*)\\) *\\$\\}";
	static final String ELSE_EXPR_DOCUMENTS ="\\{\\$ELSE\\$\\}";
	static final String ENDIF_EXPR_DOCUMENTS ="\\{\\$ENDIF\\$\\}";
	
//	private static Pattern pattIF_EXPR_DOCUMENTS = Pattern.compile(IF_EXPR_DOCUMENTS);
//	private static Pattern pattELSE_EXPR_DOCUMENTS = Pattern.compile(ELSE_EXPR_DOCUMENTS);
//	private static Pattern pattENDIF_EXPR_DOCUMENTS = Pattern.compile(ENDIF_EXPR_DOCUMENTS);
	//other patterns
//	private static Pattern pattIF_ELSE_DOCUMENTS = Pattern.compile("(?ism)"+IF_EXPR_DOCUMENTS+"((?:(?!"+ELSE_EXPR_DOCUMENTS+").)*)"+ELSE_EXPR_DOCUMENTS+"((?:(?!"+ENDIF_EXPR_DOCUMENTS+").)*)"+ENDIF_EXPR_DOCUMENTS);
//	private static Pattern pattIF_ENDIF_DOCUMENTS = Pattern.compile("(?ism)"+IF_EXPR_DOCUMENTS+"((?:(?!"+ENDIF_EXPR_DOCUMENTS+").)*)"+ENDIF_EXPR_DOCUMENTS);
	
	
	static final String EXPRESION = "<#[^#?=]*[-+*/][^#=]*/#>";
	static final String CONDITIONAL_EXPRESION = "<#[^#]*[?][^#]*[:][^#]*/#>";
	static final String STRING_EXPRESION = "<#[a-zA-Z0-9]+[.][a-zA-Z0-9]+[(][a-zA-Z0-9,]*[)][ \\t]*/#>";
	static final String TAG = "<#[^#]*?/#>";
	static final String DOCUMENT_UNDEFINED_TAG = "<#documents[0-9]*[\\s\\t]*=[\\s\\t]*\\(\\|"+ MultilineElementsMap.UNDEFINED +"\\|\\)[^#]*?/#>";
	
	public static final Pattern pattExpresionBlock = Pattern.compile(EXPRESION_BLOCK);
	private static Pattern pattExpression =  Pattern .compile(EXPRESION);
	private static Pattern pattConditionalExpression =  Pattern .compile(CONDITIONAL_EXPRESION);
	private static Pattern stringOperationExpression =  Pattern .compile(STRING_EXPRESION);
	private static Pattern pattIfElseEndif = Pattern.compile(IF_ELSE_ENDIF_EXPRESION);
	private static Pattern pattIf = Pattern.compile(IF_EXPRESION);
	private static Pattern pattElse = Pattern.compile(ELSE_EXPRESION);
	private static Pattern pattEndif = Pattern.compile(ENDIF_EXPRESION);
	private static Pattern pattTag = Pattern.compile(TAG);	
	public static Pattern undefinedTag = Pattern.compile(DOCUMENT_UNDEFINED_TAG,Pattern.CASE_INSENSITIVE);
	public static Pattern escapeNewLinesPattern = Pattern.compile("(?sm)<#.*?/#>");

	public static final int FULL_SEARCH = 1;
	public static final int UPDATE = 2;     //this product is the original app update
	public static final int REFINANCE = 3;	//for this product owner and buyer are the  same
	public static final int CONSTR_LOAN = 4;
	public static final int FORECLOSURE = 5;
	public static final int EQUITY_LOAN = 6;
	
	public static final long TO_SECCOND=1000;
	public static final long TO_MINUTE=1000*60;
	public static final long TO_HOUR=1000*3600;
	public static final long TO_DAY=TO_HOUR*24;
	public static final long TO_WEEK=TO_DAY*7;
	public static final long TO_MOUNTH=TO_DAY*30;
	public static final long TO_YEAR=TO_DAY*365;
	
	public static final int MONTHS_DERIVATION_TAG=24;  //used in <#derivation/#> tag implementation
	public static final int MONTHS_FINANCING_STATEMENTS_TAG=24;  //used in <#derivation/#> tag implementation
	
	public static final String viewerLink = "viewerLink";
	
	public static final String ssfAllImagesLink = "ssfAllImagesLink";
	public static final String communityDesc = "communityDesc";
	public static final String communityImageLink = "communityImageLink";
	public static final String communityAddress = "communityAddress";
	public static final String communityPhone = "communityPhone";
	
	public static final String taxBillNumber = "taxBillNumber";
	
	public static final String countyTaxFoundYear1 = "countyTaxFoundYear1";
	public static final String countyTaxFoundYear2 = "countyTaxFoundYear2";
	public static final String countyTaxFoundYear3 = "countyTaxFoundYear3";
	public static final String countyTaxFoundYear4 = "countyTaxFoundYear4";
	
	public static final String fileNo = "fileNo";
	public static final String abstrFileNo = "abstrFileNo";
	public static final String titleDeskOrderID ="titleDeskOrderID";
	public static final String searchId ="searchId";
	public static final String additionalLenderLanguage = "additionalLenderLanguage";
	
	public static final String communityAditionalInfo="communityAditionalInfo";
	
	//special multiline tag for taxes (has principal special element RECEIPTS )
	public static final String taxDocuments = "taxDocuments";
	
	public static final String documents = "documents";
	public static final String atsFileExceptions = "atsFileExceptions";
	public static final String atsFileRequirements = "atsFileRequirements";
	
	public static final String ssfStatements = "ssfStatements";

	public static final String tsrLink = "tsrLink";
	public static final String tsriLink = "tsriLink";
	public static final String abstractorFax = "abstractorFax";
	public static final String abstractorMPhone = "abstractorMPhone";
	public static final String abstractorHPhone = "abstractorHPhone";
	public static final String abstractorWPhone = "abstractorWPhone";
	public static final String abstractorEmail = "abstractorEmail";
	public static final String abstractorName = "abstractorName";
	
	public static final String abstractors = "abstractors";
	
	public static final String assessedValue = "assessedValue";
	public static final String assessedValueLand = "assessedValueLand";
	public static final String landValue = "landValue";
	public static final String assessedValueImprovements = "assessedValueImprovements";
	public static final String improvementsValue = "improvementsValue";
	
	public static final String ownerAmount = "ownerAmount";
	public static final String ownerAmountInt = "ownerAmountInt";
	public static final String lenderAmount = "lenderAmount";
	public static final String lenderAmountInt = "lenderAmountInt";
	
	public static final String hasJudgments = "hasJudgments";
	
	public static final String agentAddress  = "agentAddress";
	public static final String agentStateAbbr = "agentStateAbbr";
	public static final String agentUser = "agentUser";
	public static final String agentPassword = "agentPassword";
	public static final String agentState = "agentState";
	public static final String agentCity = "agentCity";
	public static final String agentZip = "agentZip";	
	public static final String agentOrderDate = "agentOrderDate";
	public static final String agentCompany = "agentCompany";
	public static final String agentName = "agentName";
	public static final String agent = "agent";
	public static final String agentFax = "agentFax";
	public static final String agentWPhone = "agentWPhone";
	public static final String agentEmail  = "agentEmail";

	public static final String parcels = "parcels";
	public static final String lender = "lender";
	public static final String propertyType = "propertyType";
	public static final String propAddress = "propAddress";
	public static final String streetDirPrefix = "streetDirPrefix"; 
	public static final String streetDirSuffix = "streetDirSuffix";
	public static final String streetName = "streetName";
	public static final String streetNum = "streetNum";
	public static final String streetSuffix = "streetSuffix";
	public static final String unitNumber = "unitNumber";
	public static final String unitType = "unitType";
	public static final String zipcode = "zipcode";
	public static final String orderStreetName = "orderStreetName";
	
	public static final String payRate = "payRate";
	public static final String legalDesc = "legalDesc";
	public static final String ocrLD = "ocrLD";
	public static final String additionalRequirements = "additionalRequirements";
    public static final String additionalExceptions = "additionalExceptions";
    public static final String additionalInformation = "additionalInformation";
    public static final String hasCityTax = "hasCityTax";
	public static final String county = "county";
	public static final String year = "year";
	public static final String bookPage = "bookPage";
	public static final String state = "state";
	public static final String stateShort = "stateShort";
//	public static final String zipCode = "zipCode";
	
	public static final String certificationDate = "certificationDate";
	public static final String certificationDateMMDDYYYY = "certificationDateMMDDYYYY";
	public static final String lastUpdateDate = "lastUpdateDate";
	public static final String updateNO = "updateNO";
	public static final String nowUpdateDate = "nowUpdateDate";
	
	//goodThru last date of the curent month
	public static final String goodThru = "goodThru";
	public static final String certificationDateInt = "certificationDateInt";
	
	public static final String certifDateFormat = "certifDateFormat";
	public static final String certifTimeFormat = "certifTimeFormat";
	public static final String commitmentDate = "commitmentDate";
//	public static final String currentOwner = "currentOwner";		//takes the names from last real transfer
	public static final String currentOwners = "currentOwners";
	public static final String currentBuyers = "currentBuyers";
	
	public static final String lot = "lot";
	public static final String sublot = "sublot";
	public static final String lotLetters = "lotLetters";
	
	public static final String unit = "unit";
	public static final String unitLetters = "unitLetters";
	
	public static final String block = "block";
	public static final String blockLetters = "blockLetters";
	
	public static final String phase = "phase";
	public static final String tract = "tract";
	public static final String platBook = "platBook";
	public static final String platPage = "platPage";
	public static final String platRecDate = "platRecDate";
	public static final String platRecDateInt = "platRecDateInt";
	public static final String section = "section";
	public static final String quarterOrder = "quarterOrder";
	public static final String township = "township";
	public static final String vestingInfoGrantee ="vestingInfoGrantee";
	public static final String vestingInfoGrantor ="vestingInfoGrantor";
	public static final String range = "range";
	public static final String subdivision = "subdivision";
	public static final String lastGrantee = "lastGrantee";
	public static final String lastGranteeAnyTransfer = "lastGranteeAnyTransfer";

	public static final String parcelId = "parcelId";
	
	public static final String countyTaxYear = "countyTaxYear";
	public static final String cityTaxYear = "cityTaxYear";
	
	public static final String taxAssesmentNo 			= "taxAssesmentNo";
	public static final String taxYearFormatedForCA		= "taxYearFormatedForCA"; 
	public static final String countyBaseAmount1 		= "countyBaseAmount1";
	public static final String countyAmountPaid1		= "countyAmountPaid1";
	public static final String countyAmountPaidOrDue1	= "countyAmountPaidOrDue1";
	public static final String countyAmountDue1 		= "countyAmountDue1";
	public static final String countyPenaltyAmount1		= "countyPenaltyAmount1";
	public static final String countyHomesteadExemption1= "countyHomesteadExemption1";
	public static final String countyInstallmentStatus1 = "countyInstallmentStatus1";
	
	public static final String countyBaseAmount2 		= "countyBaseAmount2";
	public static final String countyAmountPaid2		= "countyAmountPaid2";
	public static final String countyAmountPaidOrDue2	= "countyAmountPaidOrDue2";
	public static final String countyAmountDue2			= "countyAmountDue2";
	public static final String countyPenaltyAmount2		= "countyPenaltyAmount2";
	public static final String countyHomesteadExemption2= "countyHomesteadExemption2";
	public static final String countyInstallmentStatus2 = "countyInstallmentStatus2";
	
	public static final String countyBaseAmount3 		= "countyBaseAmount3";
	public static final String countyAmountPaid3		= "countyAmountPaid3";
	public static final String countyAmountPaidOrDue3	= "countyAmountPaidOrDue3";
	public static final String countyAmountDue3 		= "countyAmountDue3";
	public static final String countyPenaltyAmount3		= "countyPenaltyAmount3";
	public static final String countyHomesteadExemption3= "countyHomesteadExemption3";
	public static final String countyInstallmentStatus3 = "countyInstallmentStatus3";
	
	public static final String countyBaseAmount4 		= "countyBaseAmount4";
	public static final String countyAmountPaid4		= "countyAmountPaid4";
	public static final String countyAmountPaidOrDue4	= "countyAmountPaidOrDue4";
	public static final String countyAmountDue4 		= "countyAmountDue4";
	public static final String countyPenaltyAmount4		= "countyPenaltyAmount4";
	public static final String countyHomesteadExemption4= "countyHomesteadExemption4";
	public static final String countyInstallmentStatus4 = "countyInstallmentStatus4";
	
	public static final String countySABaseAmount1 			= "countySABaseAmount1";
	public static final String countySAAmountPaid1			= "countySAAmountPaid1";
	public static final String countySAAmountDue1 			= "countySAAmountDue1";
	public static final String countySAPenaltyAmount1		= "countySAPenaltyAmount1";
	public static final String countySAInstallmentStatus1	 = "countySAInstallmentStatus1";
	
	public static final String countySABaseAmount2 			= "countySABaseAmount2";
	public static final String countySAAmountPaid2			= "countySAAmountPaid2";
	public static final String countySAAmountDue2 			= "countySAAmountDue2";
	public static final String countySAPenaltyAmount2		= "countySAPenaltyAmount2";
	public static final String countySAInstallmentStatus2	 = "countySAInstallmentStatus2";
	
	public static final String boundRecordedDate = "boundRecordedDate";
	public static final String boundCityDistrict = "boundCityDistrict";
	public static final String boundEntityNumber = "boundEntityNumber";
	public static final String boundTreasurerSerDist = "boundTreasurerSerDist";
	public static final String boundImprovementOf = "boundImprovementOf";
	
	public static final String cityTaxFoundYear = "cityTaxFoundYear";
	public static final String countyTaxFoundYear = "countyTaxFoundYear";
	
	public static final String cityTaxFoundDeliq = "cityTaxFoundDeliq";
	public static final String countyTaxFoundDeliq = "countyTaxFoundDeliq";
	public static final String countyTaxDeliq = "countyTaxDeliq";
	public static final String cityTaxDeliq = "cityTaxDeliq";
	public static final String has_CityTax = "has_CityTax";
	public static final String has_CountyTax = "has_CountyTax";
	public static final String hasCountyHomesteadExemption = "hasCountyHomesteadExemption";
	
	public static final String hasCityTaxDocument = "hasCityTaxDocument";
	public static final String hasCountyTaxDocument = "hasCountyTaxDocument";
	
	public static final String countyTaxReq = "countyTaxReq";
	public static final String cityTaxReq = "cityTaxReq";
	public static final String countyDueDate = "countyDueDate";
	public static final String countyDueDateInt = "countyDueDateInt";
	public static final String cityDueDate = "cityDueDate";
	public static final String cityDueDateInt = "cityDueDateInt";
	public static final String cityTaxPaid = "cityTaxPaid";
	public static final String cityTaxDue = "cityTaxDue";
	public static final String countyTaxPaid = "countyTaxPaid"; 
	public static final String countyTaxDue = "countyTaxDue"; 
	public static final String amountsToRedeemFor = "amountsToRedeemFor";
	public static final String countyPayDate = "countyPayDate";
	public static final String countyLastReceiptDate = "countyLastReceiptDate";
	public static final String cityLastReceiptDate = "cityLastReceiptDate";
	
	
	public static final String countyPayDateInt = "countyPayDateInt";
	public static final String cityPayDate = "cityPayDate";
	public static final String cityPayDateInt = "cityPayDateInt";
	
	public static final String cityDueStatus = "cityDueStatus";
	public static final String countyDueStatus = "countyDueStatus";
	
	public static final String countyTaxVolume = "countyTaxVolume";
	public static final String city = "city";
	public static final String cityTitleCase = "cityTitleCase";
	public static final String has_buyer = "has_buyer";
	public static final String has_lender = "has_lender"; 
	public static final String has_bookPage = "has_bookPage";
	public static final String needByDate = "needByDate";
	
	public static final String currentDate = "currentDate";
	public static final String beginningDate = "beginningDate";
	public static final String officialStartDate = "officialStartDate";
	public static final String effectiveStartDate = "effectiveStartDate";
	public static final String effectiveStartDateString = "effectiveStartDateString";
	public static final String effectiveEndDateString = "effectiveEndDateString";
	public static final String viewStartDateString = "viewStartDateString";
	public static final String viewEndDateString = "viewEndDateString";
	public static final String currentDateInt = "currentDateInt";
	public static final String firstJullyDateInt = "firstJullyDateInt";
	
	public static final String dataSources = "dataSources";
	
	public static final String currentTime = "currentTime";
	
	public static final String productType = "productType";
	public static final String initialProductType = "initialProductType";
	
	public static final String searchFee="searchFee";
	public static final String TSDsearchFee="TSDsearchFee";
	
	public static final String TSD_NewPage = "TSD_NewPage";
	public static final String TSD_Patriots = "TSD_Patriots";
	public static final String TSD_StartInvoice = "TSD_StartInvoice";
	public static final String TSD_EndInvoice = "TSD_EndInvoice";
	
	public static final String  abstractorOperatingAccID= "abstractorOperatingAccID";
	
	public static final String  agentOperatingAccID = "agentOperatingAccID";
	public static final String  agentPersonalID = "agentPersonalID";
	
	public static final String  abstractorPersonalID= "abstractorPersonalID";
	
	public static final String  taxAreaCode= "taxAreaCode" ;
	
	public static final String 	taxAreaName = "taxAreaName" ; 
	
	public static final String 	taxYearOfSale = "taxYearOfSale" ;
	
	public static final String 	taxSaleNo = "taxSaleNo" ;
	
	public static final String 	taxSaleDate = "taxSaleDate" ;
	
	public static final String 	delinquentFirstYear = "delinquentFirstYear"; 
	
	public static final String 	taxExemption = "taxExemption";
	
	public static final String 	soOrderProductId = "soOrderProductId";
	
	public static final String DASLIndexOnly = "DASLIndexOnly";
	public static final String DASLStreetName = "DASLStreetName";
	public static final String DASLDocType = "DASLDocType";
	public static final String DASLDocThrough = "DASLDocThrough";
	public static final String DASLAddition = "DASLAddition";
	public static final String DASLAbstractNumber = "DASLAbstractNumber";
	public static final String DASLAbstractName = "DASLAbstractName";
	public static final String DASLPreviousParcel = "DASLPreviousParcel";
	public static final String DASLQuarterOrder1 = "DASLQuarterOrder1";
	public static final String DASLQuaterValue1 = "DASLQuaterValue1";
	public static final String DASLQuarterOrder2 = "DASLQuarterOrder2";
	public static final String DASLQuaterValue2 = "DASLQuaterValue2";
	public static final String DASLQuarterOrder3 = "DASLQuarterOrder3";
	public static final String DASLQuaterValue3 = "DASLQuaterValue3";
	public static final String DASLQuarterOrder4 = "DASLQuarterOrder4";
	public static final String DASLQuaterValue4 = "DASLQuaterValue4";
	
	public static final String DASLSubdivisionUnit ="DASLSubdivisionUnit";
	public static final String DASLPreviousARB = "DASLPreviousARB";
	public static final String DASLTitleOfficer = "DASLTitleOfficer";
	public static final String DASLSSN4 = "DASLSSN4";
	public static final String DASLYearFiled  = "DASLYearFiled";
	public static final String DASLMonthFiled  = "DASLMonthFiled";
	public static final String DASLDayFiled  = "DASLDayFiled";
	
	public static final String DASLStreetNumber = "DASLStreetNumber";
	public static final String DASLAddressUnitValue = "DASLAddressUnitValue";
	public static final String DASLStreetSuffix = "DASLStreetSuffix";
	public static final String DASLStreetFraction = "DASLStreetFraction";
	public static final String DASLID = "DASLID";
	public static final String DASLParcelId = "DASLParcelId";
	public static final String DASLPlatName = "DASLPlatName";
	public static final String DASLPlatLabel = "DASLPlatLabel";
	public static final String DASL_B_P_H = "DASL_B_P_H";
	public static final String DASL_TRACT = "DASL_TRACT";
	public static final String DASL_TRACT_THROUGH = "DASL_TRACT_THROUGH";
	public static final String DASL_NCB_NO = "DASL_NCB_NO";
	public static final String DASL_DIVISION_NO = "DASL_DIVISION_NO";
	public static final String DASL_LOT_THROUGH = "DASL_LOT_THROUGH";
	public static final String DASL_BLOCK_THROUGH = "DASL_BLOCK_THROUGH";
	public static final String DASL_FULL_STREET = "DASL_FULL_STREET";
	public static final String DASLProviderId = "DASLProviderId";

	public static final String DASLCountyFIPS = "DASLCountyFIPS";
	public static final String DASLStateFIPS = "DASLStateFIPS";
	
	public static final String DASLLastName = "DASLLastName";
	public static final String DASLFirstName = "DASLFirstName";
	public static final String DASLMiddleName = "DASLMiddleName";
	
	public static final String DASLClientTransactionReference = "DASLClientTransactionReference";
	public static final String DASLSearchType = "DASLSearchType";
	public static final String DASLIsPlat = "DASLIsPlat";
	
	public static final String DASLPropertySearchFromDate = "DASLPropertySearchFromDate";
	public static final String DASLPropertySearchToDate = "DASLPropertySearchToDate";
	public static final String DASLPropertySearchType = "DASLPropertySearchType";
	
	public static final String DASLAPN = "DASLAPN";
	public static final String DASLLot = "DASLLot";
	public static final String DASLLotThrough = "DASLLotThrough";
	public static final String DASLBuilding ="DASLBuilding";
	public static final String DASLUnit = "DASLUnit";
	public static final String DASLUnitPrefix = "DASLUnitPrefix";
	public static final String DASLCity = "DASLCity";
	public static final String DASLZip = "DASLZip";
	
	public static final String DASLSubLot = "DASLSubLot";
	public static final String DASLBlock = "DASLBlock";
	public static final String DASLPlatBook = "DASLPlatBook";
	public static final String DASLPlatPage = "DASLPlatPage";
	public static final String DASLPlatDocumentNumber = "DASLPlatDocumentNumber";
	public static final String DASLPlatDocumentYear = "DASLPlatDocumentYear";
	public static final String platBookPage ="platBookPage";

	public static final String DASLSection = "DASLSection";
	public static final String DASLTownship = "DASLTownship";
	public static final String DASLRange = "DASLRange";
	public static final String DASLQuarterOrder = "DASLQuarterOrder";
	public static final String DASLQuaterValue = "DASLQuaterValue";
	public static final String DASLARB = "DASLARB";
	
	public static final String DASLStreetDirection = "DASLStreetDirection";
	public static final String DASLStreetPostDirection = "DASLStreetPostDirection";
	
	public static final String DASLStateAbbreviation = "DASLStateAbbreviation";
	public static final String DASLDocumentSearchType = "DASLDocumentSearchType";
	public static final String DASLCounty = "DASLCounty";
	public static final String DASLIncludeTaxFlag = "DASLIncludeTaxFlag";
	public static final String DASLPropertyChainOption = "DASLPropertyChainOption";
	public static final String DASLPartySearchType = "DASLPartySearchType";
	public static final String DASLRealPartySearchType1 = "DASLRealPartySearchType1";
	public static final String DASLImageSearchType = "DASLImageSearchType";
	
	public static final String DASLPartySearchFromDate = "DASLPartySearchFromDate";
	public static final String DASLPartySearchToDate = "DASLPartySearchToDate";
	public static final String DASLNickName = "DASLNickName";
	public static final String DASLWithProperty = "DASLWithProperty";
	public static final String DASLSoundIndex = "DASLSoundIndex";
	public static final String DASLBook = "DASLBook";
	public static final String DASLPage = "DASLPage";
	public static final String DASLDocumentNumber = "DASLDocumentNumber";
	public static final String DASLClientReference = "DASLClientReference";
	
	public static final String DASLSubdivision	= "DASLSubdivision";
	public static final String DASLParcel		= "DASLParcel";
	public static final String DASLPcl		= "DASLPcl";
	public static final String DASLPcl1		= "DASLPcl1";
	public static final String DASLPcl2		= "DASLPcl2";
	public static final String DASLImEmail		= "DASLImEmail";
	
	public static final String DASLPartyRole_1 	= "DASLPartyRole_1";
	public static final String DASLFirstName_1 	= "DASLFirstName_1";
	public static final String DASLMiddleName_1 = "DASLMiddleName_1";
	public static final String DASLLastName_1 	= "DASLLastName_1";
	
	public static final String DASLPartyRole_2 	= "DASLPartyRole_2";
	public static final String DASLFirstName_2 	= "DASLFirstName_2";
	public static final String DASLMiddleName_2 = "DASLMiddleName_2";
	public static final String DASLLastName_2 	= "DASLLastName_2";
		
	public static final String DASLPartyRole_3 	= "DASLPartyRole_3";
	public static final String DASLFirstName_3 	= "DASLFirstName_3";
	public static final String DASLMiddleName_3 = "DASLMiddleName_3";
	public static final String DASLLastName_3 	= "DASLLastName_3";
	
	public static final String DASLPartyRole_4 	= "DASLPartyRole_4";
	public static final String DASLFirstName_4 	= "DASLFirstName_4";
	public static final String DASLMiddleName_4 = "DASLMiddleName_4";
	public static final String DASLLastName_4 	= "DASLLastName_4";

	public static final String DASLPartyRole_5 	= "DASLPartyRole_5";
	public static final String DASLFirstName_5 	= "DASLFirstName_5";
	public static final String DASLMiddleName_5 = "DASLMiddleName_5";
	public static final String DASLLastName_5 	= "DASLLastName_5";

	public static final String DASLPartyRole_6 	= "DASLPartyRole_6";
	public static final String DASLFirstName_6 	= "DASLFirstName_6";
	public static final String DASLMiddleName_6 = "DASLMiddleName_6";
	public static final String DASLLastName_6 	= "DASLLastName_6";
	
	public static final String DASLOwnerFirstName 	= "DASLOwnerFirstName";
	public static final String DASLOwnerLastName 	= "DASLOwnerLastName";
	public static final String DASLOwnerMiddleName 	= "DASLOwnerMiddleName";
	public static final String DASLOwnerFullName 	= "DASLOwnerFullName";
	
	public static final String DASLClientId = "DASLClientId";
	public static final String DASLimageId 	= "DASLimageId";
	
	public static final HashMap<String,Class> types = new HashMap<String,Class>(); 
	public static final String functionBaseName = "Component_";
	public static final String functionBlockBaseName = "BlocComponent_";
	public static final String functionBlockSecondLevelBaseName = "BlocComponentSecondLevel_";
	public static final String functionBlockThirdLevelBaseName = "BlocComponentThirdLevel_";
	public static final String searchWasReopen = "searchWasReopen";
	public static final String hasRestrictions	= "hasRestrictions";
	public static final String hasCcers	= "hasCcers";
	public static final String hasEasements		= "hasEasements";
	public static final String hasByLaws		= "hasByLaws";
	public static final String hasPlats			= "hasPlats";
	public static final String isSO			= "isSO";
	
	public static final String isCondo			= "isCondo";
	
	public static final String htmlOrder = "htmlOrder";
	public static final String searchPage = "searchPage";
	
	public static final String deleteLast = "deleteLast";
	public static final String  ATSExportDataPreview = "ATSExportDataPreview";
	public static final String updateFoundExtradata = "updateFoundExtradata";
	public static final String updateFoundOutstandingExtradata = "updateFoundOutstandingExtradata";
	public static final String otherResults = "otherResults";
	
	public static final String districtNo = "districtNo";
	public static final String districtCode = "districtCode";

	public static final String hasLiens = "hasLiens";
	public static final String ownerType = "ownerType";
	
	public static final String hasTransfers = "hasTransfers";
	
	public static final String hasMortgages = "hasMortgages";
	
	public static final String documentsNoGreaterThan = "documentsNoGreaterThan";
	
	public static final String OWNER_TYPE_SINGLE = "SINGLE";
	public static final String OWNER_TYPE_MARRIED = "MARRIED";
	public static final String OWNER_TYPE_MULTIPLE = "MULTIPLE";
	public static final String OWNER_TYPE_LLC = "LLC";
	public static final String OWNER_TYPE_LP = "LP";
	public static final String OWNER_TYPE_CORP = "CORP";
	public static final String OWNER_TYPE_PARTNERSHIP = "PARTNERSHIP";
	public static final String OWNER_TYPE_OTHER = "OTHER";
	
	// yes/no tags task 7652 
	// _YN -> this element can only be YES or NO
	public static final String LEGAL_CORRECT_YN = "LEGAL_CORRECT";
	public static final String TAX_SUIT_YN = "TAX_SUIT";
	public static final String DECEASED_DEBTOR_YN = "DECEASED_DEBTOR"; //under misc doctype
	public static final String BANKRUPTCY_YN = "BANKRUPTCY";
	public static final String STATE_TAX_LIEN_YN = "STATE_TAX_LIEN";
	public static final String GUARDIANSHIP_YN = "GUARDIANSHIP"; //under court doctype
	public static final String FEDERAL_TAX_LIEN_YN = "FEDERAL_TAX_LIEN"; // Federal Tax Lien/DOJ:
	public static final String LIS_PENDENS_YN = "LIS_PENDENS";
	public static final String MHU_EVIDENCE_YN = "MHU_EVIDENCE";
	public static final String HOME_EQUITY_YN = "HOME_EQUITY";
	public static final String AJ_YN = "AJ";
	public static final String DIVORCE_YN = "DIVORCE"; //under court doctype
	public static final String UNEMPLOYMENT_LIEN_YN = "UNEMPLOYMENT_LIEN"; // Texas Workforce/Unemployment Lien: 
																			// under lien doctype
	
	public static final String searchProduct = "searchProduct";
	
	@Deprecated
	public static final String bpCode = "bpCode";
	public static final String cbCode = "cbCode";
	
	public static final Set<String> tagsWithImageLinks = new HashSet<String>(); 
	
    private static String DELETE_LAST_REGEX = AddDocsTemplates.deleteLast + "\\(([0-9]+)\\)";
    private static Pattern DELETE_LAST_PATTERN = Pattern.compile(DELETE_LAST_REGEX,Pattern.CASE_INSENSITIVE);
    
    private static String PARCEL_ID_FORMAT_REGEX =  AddDocsTemplates.parcelId + "\\(([x\\-]+)\\)";
    private static Pattern PARCEL_ID_FORMAT_PATTERN =  Pattern.compile(PARCEL_ID_FORMAT_REGEX);
    
    private static final String PARAMETERS_FOR_TAG =  "\\(((?:(?:\"[^\"]+\")|(?:[A-Za-z0-9]+))(?:,(?:(?:\"[^\"]+\")|(?:[A-Za-z0-9]+)))*)\\)";
    public static String TAG_WITH_PARAMETERS =  "([A-Za-z0-9]+)\\s*" + PARAMETERS_FOR_TAG;
    private static final String	PARAMETERS_FOR_TAG_ESCAPE	= "[\"|,|\\(\\)%\\$\\[\\]]+";
    
	static {
		types.put(DASLIndexOnly, String.class );
		types.put( hasPlats, boolean.class );
		types.put( isSO, boolean.class );
		types.put( searchWasReopen, boolean.class );
		types.put( hasRestrictions, boolean.class );
		types.put( hasCcers, boolean.class );
		types.put( hasByLaws, boolean.class );
		types.put( hasEasements, boolean.class );
		types.put( amountsToRedeemFor, String.class );
		types.put( countyBaseAmount1, double.class );
		types.put( countyAmountDue1,  double.class );
		types.put( countyAmountPaid1, double.class );
		types.put( countyAmountPaidOrDue1, double.class );
		types.put( countyPenaltyAmount1, double.class );
		types.put( countyHomesteadExemption1, double.class );
		types.put( countyInstallmentStatus1, String.class);
		types.put( taxAssesmentNo, String.class);
		types.put( taxYearFormatedForCA, String.class);
		types.put( delinquentFirstYear, String.class );
		
		types.put( atsFileExceptions, String.class);
		types.put( atsFileRequirements, String.class );
		
		types.put( taxExemption, double.class );
		
		types.put( countyBaseAmount2, double.class );
		types.put( countyAmountDue2,  double.class );
		types.put( countyAmountPaid2, double.class );
		types.put( countyAmountPaidOrDue2, double.class );
		types.put( countyPenaltyAmount2, double.class );
		types.put( countyHomesteadExemption2, double.class );
		types.put( countyInstallmentStatus2, String.class);
		
		types.put( countyBaseAmount3, double.class );
		types.put( countyAmountDue3,  double.class );
		types.put( countyAmountPaid3, double.class );
		types.put( countyAmountPaidOrDue3, double.class );
		types.put( countyPenaltyAmount3, double.class );
		types.put( countyHomesteadExemption3, double.class );
		types.put( countyInstallmentStatus3, String.class);
		
		types.put( countyBaseAmount4, double.class );
		types.put( countyAmountDue4,  double.class );
		types.put( countyAmountPaid4, double.class );
		types.put( countyAmountPaidOrDue4, double.class );
		types.put( countyPenaltyAmount4, double.class );
		types.put( countyHomesteadExemption4, double.class );
		types.put( countyInstallmentStatus4, String.class);
		
		types.put( countySABaseAmount1, double.class );
		types.put( countySAAmountDue1,  double.class );
		types.put( countySAAmountPaid1, double.class );
		types.put( countySAPenaltyAmount1, double.class );
		types.put( countySAInstallmentStatus1, String.class);
		
		types.put( countySABaseAmount2, double.class );
		types.put( countySAAmountDue2,  double.class );
		types.put( countySAAmountPaid2, double.class );
		types.put( countySAPenaltyAmount2, double.class );
		types.put( countySAInstallmentStatus2, String.class);
		
		types.put( boundRecordedDate, String.class );
		types.put( boundCityDistrict, String.class );
		types.put( boundEntityNumber, String.class );
		types.put( boundTreasurerSerDist, String.class );
		types.put( boundImprovementOf, String.class );
		types.put( soOrderProductId, String.class );
		
		types.put( taxAreaCode, String.class );
		types.put( taxAreaName, String.class );
		
		types.put( taxYearOfSale, String.class );
		types.put( taxSaleNo, String.class );
		types.put( taxSaleDate, String.class );
		
		types.put( agentOperatingAccID, String.class );
		types.put( abstractorOperatingAccID, String.class );
		
		types.put( agentPersonalID, String.class );
		types.put( abstractorPersonalID, String.class );
		
		types.put( certificationDateMMDDYYYY, String.class );
		types.put( titleDeskOrderID, String.class );
		types.put( parcels, String.class );
		types.put( documents , Vector.class );
		
		types.put(ssfStatements, Vector.class );
		
		types.put(communityDesc, String.class);
		types.put(ssfAllImagesLink, String.class);
		types.put(tsrLink, String.class);
		types.put(tsriLink, String.class);
		types.put(communityImageLink, String.class);
		types.put(communityAditionalInfo,String.class);
		types.put(viewerLink, String.class);
		
		types.put(communityPhone, String.class);
		types.put(communityAddress, String.class);
		
		types.put(fileNo, String.class);
		types.put(updateNO, int.class);
		types.put(abstrFileNo, String.class);
		types.put(searchId, String.class);
		types.put(additionalLenderLanguage, String.class);
		
		types.put(abstractorHPhone, String.class);
		types.put(abstractorWPhone, String.class);
		types.put(abstractorFax, String.class);
		types.put(abstractorMPhone, String.class);
		types.put(abstractorEmail, String.class);
		
		types.put(abstractors, String.class);
		
		types.put(agentFax, String.class);
		types.put(agentWPhone, String.class);
		
		types.put(taxDocuments, String.class);
		types.put(abstractorName, String.class);
		
		types.put( assessedValue, String.class );
		types.put( assessedValueLand, double.class );
		types.put( landValue, double.class );
		types.put( assessedValueImprovements, double.class );
		types.put( improvementsValue, double.class );
		
		types.put(ownerAmount,double.class);
		types.put(ownerAmountInt, int.class);
		types.put(lenderAmount, double.class);
		types.put(lenderAmountInt, int.class);

		types.put(agentAddress, String.class);
		types.put(agentEmail, String.class);
		types.put(hasJudgments, boolean.class);
		
		types.put(agentOrderDate, String.class);
		types.put(agentCity, String.class);
		types.put(agentZip, String.class);
		types.put(agentStateAbbr, String.class);
		types.put(agentUser, String.class);
		types.put(agentPassword, String.class);
		types.put(agentState, String.class);

		types.put(lender, String.class);
		types.put(propAddress, String.class);
		types.put(propertyType, String.class);
		types.put(streetDirPrefix , String.class);
		types.put(streetDirSuffix, String.class);
		types.put(streetName, String.class);
		types.put(streetNum, String.class);
		types.put(streetSuffix, String.class);
		types.put(unitNumber, String.class);
		types.put(unitType, String.class);
		types.put(zipcode, String.class);
//		types.put(zipCode, String.class);
		types.put(orderStreetName, String.class);
		types.put(agent, String.class);
		types.put(agentCompany, String.class);
		types.put(agentName, String.class);

		types.put(legalDesc, String.class);
		types.put(ocrLD, String.class);
		types.put(additionalRequirements, String.class);
        types.put( additionalExceptions, String.class );
        types.put( additionalInformation, String.class );
        types.put( hasCityTax, boolean.class );
		types.put(county, String.class);
		types.put(year, int.class);
		types.put(bookPage, String.class);
		types.put(state, String.class);
		types.put(stateShort, String.class);

		types.put(certificationDate, String.class);
		types.put(lastUpdateDate, String.class);
		types.put(nowUpdateDate, String.class);
		
		types.put(goodThru, String.class);
		types.put(certificationDateInt, long.class);
		
		types.put(certifDateFormat, String.class);
		types.put(certifTimeFormat, String.class);
		types.put(commitmentDate, String.class);
//		types.put(currentOwner, String.class);
		types.put(currentOwners, Vector.class);
		types.put(currentBuyers, Vector.class);

		types.put(lot, String.class);
		types.put(sublot, String.class);
		types.put(lotLetters, String.class);
		types.put(unit, String.class);
		types.put(unitLetters, String.class);
		types.put(block, String.class);
		types.put(blockLetters, String.class);
		types.put(phase, String.class);
		types.put(tract, String.class);
		types.put(platBook, String.class);
		types.put(platPage, String.class);
		types.put(platRecDate, String.class);
		types.put(platRecDateInt, long.class);
		types.put(section, String.class);
		types.put(quarterOrder, String.class);
		types.put(township,String.class);
		types.put(vestingInfoGrantee, String.class);
		types.put(vestingInfoGrantor, String.class);
		types.put(range,String.class);
		types.put(subdivision, String.class);
		types.put(lastGrantee, String.class);
		types.put(lastGranteeAnyTransfer, String.class);
		types.put(parcelId, String.class);
		types.put(countyTaxYear, int.class);
		types.put(cityTaxYear, int.class);
		types.put(cityTaxFoundYear, int.class);
		
		types.put(taxBillNumber, String.class);
		
		types.put(countyTaxFoundYear, int.class);
		types.put(countyTaxFoundYear1, String.class);
		types.put(countyTaxFoundYear2, String.class);
		types.put(countyTaxFoundYear3, String.class);
		types.put(countyTaxFoundYear4, String.class);
		
		types.put(cityTaxFoundDeliq, double.class);
		types.put(countyTaxFoundDeliq, double.class);
		types.put(countyTaxDeliq,double.class);
		types.put(cityTaxDeliq,double.class);
		types.put(has_CityTax,String.class);
		types.put(has_CountyTax,String.class);
		types.put(hasCityTaxDocument,boolean.class);
		types.put(hasCountyTaxDocument,boolean.class);
		types.put(hasCountyHomesteadExemption,boolean.class);
		
		types.put(countyTaxReq, double.class);
		types.put(cityTaxReq, double.class);
		types.put(countyDueDate, String.class);
		types.put(countyDueDateInt, long.class);
		types.put(cityDueDate, String.class);
		types.put(cityDueDateInt, long.class);
		types.put(cityTaxPaid, double.class);
		types.put(cityTaxDue, double.class);
		types.put(countyTaxPaid , double.class);
		types.put(countyTaxDue , double.class);
		types.put(countyPayDate, String.class);
		types.put(countyLastReceiptDate,String.class);
		types.put(cityLastReceiptDate,String.class);
		types.put(countyPayDateInt, long.class);
		types.put(cityPayDate, String.class);
		types.put(cityPayDateInt, long.class);
		types.put(cityDueStatus, int.class);
		types.put(countyDueStatus, int.class);
		types.put(countyTaxVolume, int.class);
		types.put(city, String.class);
		types.put(cityTitleCase, String.class);
		types.put(has_buyer, String.class);
		types.put(has_lender , String.class);
		types.put(has_bookPage, String.class);
		types.put(needByDate, String.class);
		
		types.put(currentDate, String.class);
		types.put(beginningDate,String.class);
		types.put(officialStartDate,String.class);
		types.put(effectiveStartDate,String.class);
		types.put(effectiveStartDateString,String.class);
		types.put(effectiveEndDateString,String.class);
		types.put(viewStartDateString,String.class);
		types.put(viewEndDateString,String.class);
		
		types.put(currentDateInt, long.class);
		types.put(firstJullyDateInt, long.class);
		types.put(currentTime, String.class);
		types.put(payRate, String.class);
		
		types.put(productType, String.class);
		types.put(initialProductType, String.class);
		types.put(searchFee,double.class);
		types.put(TSDsearchFee,double.class);
		types.put(TSD_NewPage,String.class);
		types.put(TSD_Patriots,String.class);
		types.put(TSD_StartInvoice,String.class);
		types.put(TSD_EndInvoice,String.class);
		
		types.put(DASLSSN4, String.class);
		types.put(DASLSubdivision,String.class);
		types.put(DASLStreetName, String.class);
		types.put(DASLDocType, String.class);
		types.put(DASLDocThrough, String.class);
		
		types.put(DASLSubdivisionUnit, String.class);
		types.put(DASLTitleOfficer, String.class);
		types.put(DASLYearFiled, String.class);
		types.put(DASLMonthFiled, String.class);
		types.put(DASLDayFiled, String.class);
		
		types.put(DASLAddition, String.class);
		types.put(DASLAbstractNumber,String.class);
		types.put(DASLAbstractName, String.class);
		types.put(DASLPreviousParcel, String.class);
		types.put(DASLQuarterOrder1, String.class);
		types.put(DASLQuaterValue1, String.class);
		types.put(DASLQuarterOrder2, String.class);
		types.put(DASLQuaterValue2, String.class);
		types.put(DASLQuarterOrder3, String.class);
		types.put(DASLQuaterValue3, String.class);
		types.put(DASLQuarterOrder4, String.class);
		types.put(DASLQuaterValue4, String.class);
		
		types.put(DASLStreetNumber, String.class);
		types.put(DASLAddressUnitValue, String.class);
		
		types.put(DASLStreetFraction, String.class);
		types.put(DASLStreetPostDirection, String.class);
		
		types.put(DASLStreetSuffix, String.class);
		types.put(DASLID, String.class);
		types.put(DASLParcelId, String.class);
		
		types.put(DASLPcl , String.class);
		types.put(DASLPcl1 , String.class);
		types.put(DASLImEmail , String.class);
		types.put(DASLPcl2 , String.class);
		
		types.put(DASLCountyFIPS, String.class);
		types.put(DASLStateFIPS , String.class);
		types.put(DASLLastName ,String.class);
		types.put(DASLFirstName ,String.class);
		types.put(DASLMiddleName ,String.class);
		types.put(DASLParcel , String.class);
		types.put(DASLPlatName , String.class);
		types.put(DASLPlatLabel , String.class);
		types.put(DASL_B_P_H , String.class);
		types.put(DASLCounty , String.class);
		types.put(DASL_TRACT , String.class);
		types.put(DASL_LOT_THROUGH , String.class);
		types.put(DASL_TRACT_THROUGH , String.class);
		types.put(DASL_NCB_NO , String.class);
		types.put(DASL_DIVISION_NO , String.class);
		
		types.put(DASL_BLOCK_THROUGH , String.class);
		types.put(DASL_FULL_STREET , String.class);
		types.put(DASLProviderId , String.class);
		types.put(DASLPreviousARB, String.class);
		types.put(DASLClientTransactionReference,String.class);
		types.put(DASLSearchType,String.class);
		types.put(DASLIsPlat,String.class);
		types.put(DASLPropertySearchFromDate,String.class);
		types.put(DASLPropertySearchToDate,String.class);
		types.put(DASLAPN,String.class);
		types.put(DASLStreetDirection,String.class);
		types.put(DASLStateAbbreviation,String.class);
		types.put(DASLDocumentSearchType,String.class);
		types.put(DASLIncludeTaxFlag,String.class);
		types.put(DASLPropertyChainOption,String.class);
		types.put(DASLPartySearchType,String.class);
		types.put(DASLImageSearchType ,String.class);
		types.put(DASLPartySearchFromDate,String.class);
		types.put(DASLPartySearchToDate,String.class);
		types.put(DASLPartyRole_1,String.class);
		types.put(DASLRealPartySearchType1,String.class);
		types.put(DASLFirstName_1,String.class);
		types.put(DASLMiddleName_1,String.class);
		types.put(DASLLastName_1,String.class);
		types.put(DASLPartyRole_2,String.class);
		types.put(DASLFirstName_2,String.class);
		types.put(DASLMiddleName_2,String.class);
		types.put(DASLLastName_2,String.class);
		types.put(DASLPartyRole_3,String.class);
		types.put(DASLFirstName_3,String.class);
		types.put(DASLMiddleName_3,String.class);
		types.put(DASLLastName_3,String.class);
		
		types.put(DASLPartyRole_4,String.class);
		types.put(DASLFirstName_4,String.class);
		types.put(DASLMiddleName_4,String.class);
		types.put(DASLLastName_4,String.class);
		
		types.put(DASLPartyRole_5,String.class);
		types.put(DASLFirstName_5,String.class);
		types.put(DASLMiddleName_5,String.class);
		types.put(DASLLastName_5,String.class);
		
		types.put(DASLPartyRole_6,String.class);
		types.put(DASLFirstName_6,String.class);
		types.put(DASLMiddleName_6,String.class);
		types.put(DASLLastName_6,String.class);
		types.put(DASLOwnerFirstName,String.class);
		types.put(DASLOwnerLastName,String.class);
		types.put(DASLOwnerMiddleName,String.class);
		types.put(DASLOwnerFullName,String.class);
		types.put(DASLClientId,String.class);
		
		types.put(DASLNickName,String.class);
		types.put(DASLWithProperty,String.class);
		types.put(DASLSoundIndex,String.class);
		types.put(DASLBook,String.class);
		types.put(DASLPage,String.class);
		types.put(DASLDocumentNumber,String.class);
		types.put(DASLClientReference, String.class);
		types.put(DASLPropertySearchType, String.class);
		
		types.put(DASLLot, String.class);
		types.put(DASLLotThrough, String.class);
		types.put(DASLBuilding, String.class);
		types.put(DASLUnit, String.class);
		types.put(DASLUnitPrefix, String.class);
		types.put(DASLCity, String.class);
		types.put(DASLZip, String.class);
		types.put(DASLSubLot,String.class);
		types.put(DASLBlock,String.class);
		types.put(DASLPlatBook,String.class);
		types.put(DASLPlatPage,String.class);
		types.put(DASLPlatDocumentYear,String.class);
		types.put(DASLPlatDocumentNumber,String.class);
		types.put(platBookPage, String.class);
	
		types.put(DASLSection,String.class);
		types.put(DASLTownship,String.class);
		types.put(DASLRange,String.class);
		types.put(DASLQuarterOrder,String.class);
		types.put(DASLQuaterValue,String.class);
		types.put(DASLARB,String.class);
		
		types.put(DASLimageId, String.class);
		
		types.put(htmlOrder, String.class);
		types.put(searchPage, String.class);

		types.put(ATSExportDataPreview, String.class);
		types.put(updateFoundExtradata, boolean.class);
		types.put(updateFoundOutstandingExtradata, boolean.class); 
		types.put(otherResults, String.class);
		
		types.put(isCondo, boolean.class);
		
		types.put(districtNo, String.class);
		types.put(districtCode, String.class);
		
		types.put(hasLiens, boolean.class);
		types.put(ownerType, String.class);
		
		types.put(hasTransfers, boolean.class);
		
		types.put(hasMortgages, boolean.class);
		
		types.put(documentsNoGreaterThan, boolean.class);
		
		// task 7652 yes/no elements
		types.put(LEGAL_CORRECT_YN, String.class);
		types.put(TAX_SUIT_YN, String.class);
		types.put(DECEASED_DEBTOR_YN, String.class);
		types.put(BANKRUPTCY_YN, String.class);
		types.put(STATE_TAX_LIEN_YN, String.class);
		types.put(GUARDIANSHIP_YN, String.class);
		types.put(FEDERAL_TAX_LIEN_YN, String.class);
		types.put(LIS_PENDENS_YN, String.class);
		types.put(MHU_EVIDENCE_YN, String.class);
		types.put(HOME_EQUITY_YN, String.class);
		types.put(AJ_YN, String.class);
		types.put(DIVORCE_YN, String.class);
		types.put(UNEMPLOYMENT_LIEN_YN, String.class);
		
		types.put(searchProduct, String.class);
		types.put(dataSources, String.class);
		types.put(bpCode, String.class);
		types.put(cbCode, String.class);
		
		tagsWithImageLinks.add(platBook);
		tagsWithImageLinks.add(platPage);
		tagsWithImageLinks.add(platBookPage);
		
		
		for (String tagWithImageLink : tagsWithImageLinks) {
			types.put(tagWithImageLink + MultilineElementsMap.DOCUMENT_IMAGE_MARKER, String.class);
			types.put(tagWithImageLink + MultilineElementsMap.DOCUMENT_PDF_MARKER, String.class);
		}
		
		
		
		
	}
	
	public static final HashMap<String,String> knownExtensions = new HashMap<String,String>();
	
	static 
	{
		knownExtensions.put("pxt", "text/plain");
		knownExtensions.put("doc", "MS Word 97");
		knownExtensions.put("xml", "text/xml ");
		knownExtensions.put("ats", "text/plain");
		knownExtensions.put("html", "text/html");
		knownExtensions.put("htm", "text/html");
		knownExtensions.put("tsd", "text/plain");
		knownExtensions.put("txt", "text/plain");
	}

	/**
	 * Regex for BPCode what will match "BPCode_ProductID=", for example "E12_1="
	 */
	public static final String	REGEX_BPCODE_PRODUCT	= "(?i)(([a-zA-Z0-9]+)_([0-9]+))\\s*=";
	public static final Pattern PATTERN_BPCODE_PRODUCT = Pattern.compile(AddDocsTemplates.REGEX_BPCODE_PRODUCT);
	
	public AddDocsTemplates(){
	}
	
	public static int findBlocksNew(TemplateContents doc,StringBuffer javaData)throws TemplatesException{
			
		int startBloc=0,stopBloc=0,nrBlocuri=0;
		Vector<String> blokvec=new Vector<String> ();
		ArrayList<ArrayList<String>> secondLevelTagsList = new ArrayList<ArrayList<String>>();
		
		String sursa = doc.toString();	
		
		StringBuffer expresieCurentaBuf =new StringBuffer("true");
		StringBuffer functionBlockBaseNameBuf=new StringBuffer("");
		String expresieCurenta = "";
		
		Matcher mIfElseEndif=pattIfElseEndif.matcher(sursa);
		
		
		while(mIfElseEndif.find()){
			String  stemp=mIfElseEndif.group();
			
			String name = "";
			Matcher matcher = pattIf.matcher(stemp);
			if (matcher.matches()) {													//add field, getter and setter  (if they are not already present)
				name = matcher.group(1);												//for tags with parameters
				name = name.replaceFirst("\\A\\(", "");
				name = name.replaceFirst("\\)\\z", "");
				if (name.length()!=0)
					if (name.matches(TAG_WITH_PARAMETERS)) {
						@SuppressWarnings("rawtypes")
						Class classType = (Class) types.get(name.replaceAll(PARAMETERS_FOR_TAG, ""));
						String className = classType.getName();
						className = className.substring(className.lastIndexOf(".") + 1);
						name = name.replaceAll(PARAMETERS_FOR_TAG, "$1").replaceAll("\\s", "").replaceAll(PARAMETERS_FOR_TAG_ESCAPE, "_");
						types.put(name, classType);
						//add field
						String str = "\tpublic " + className + " " + name + ";" + "\n";
						if (!javaData.toString().contains(str))
							javaData.append(str);
						//add getter
						str = "\tpublic " + className + " get" + name + "(){" + "\n" +
						       "\t\treturn " + name + ";" + "\n" + "\t}" + "\n\n"; 
						if (!javaData.toString().contains(str))
							javaData.append(str);
						//add setter
						str = "\tpublic void set" + name + "(" + className + " " + name + "){" + "\n" + 
						       "\t\tthis." + name + "=" + name + ";" + "\n" + "\t}" + "\n\n"; 
						if (!javaData.toString().contains(str))
									javaData.append(str);
					}
			}
			
			Matcher iFMatcher = Pattern.compile(IF_EXPRESION).matcher(stemp);
			if (iFMatcher.matches()) {
				String content = iFMatcher.group(1);
				content = content.replaceFirst("\\A\\(", "");
				content = content.replaceFirst("\\)\\z", "");
				if (content.matches(TAG_WITH_PARAMETERS)) {
					String newContent = content.replaceAll(PARAMETERS_FOR_TAG, "$1").replaceAll("\\s", "").replaceAll(PARAMETERS_FOR_TAG_ESCAPE, "_");
					stemp = stemp.replace(content, newContent);
				}
			}
			
			Matcher mIf=pattIf.matcher(stemp);
			Matcher mElse=pattElse.matcher(stemp);
			Matcher mEndif=pattEndif.matcher(stemp);
			
			stopBloc=mIfElseEndif.start();
			
			if((!(stopBloc==0&&startBloc==0))/*(stopBloc!=startBloc)*/&&(!expresieCurentaBuf.toString().equals("true"))){
				//adauga blocul
				String sbloc=sursa.substring(startBloc,stopBloc);
				blokvec.add(sbloc);
				ArrayList<String> list = new ArrayList<String>();
				
				boolean foundLevelTwoOrThree = false;
				
				List<String> elementInnerTags = RegExUtils.getMatches(ELEMENT_INNER_TAG_REGEX, expresieCurenta, 1);
				String multilineTag = isSecondLevel(sursa, mIfElseEndif.end(), expresieCurenta, elementInnerTags, false);
				if (!"".equals(multilineTag)) {
					for (String field: elementInnerTags) {
						addFieldGetterSetter(javaData, multilineTag + "$" + field);
						list.add(field);
					}
					addBlockFunctionToJavaFile(javaData, expresieCurentaBuf.toString().replaceAll(ELEMENT_INNER_TAG_REGEX, multilineTag + "\\$$1"), 
							nrBlocuri, functionBlockSecondLevelBaseName);
					nrBlocuri++;
					foundLevelTwoOrThree = true;
				}
				
				List<String> subelementInnerTags = RegExUtils.getMatches(SUBELEMENT_INNER_TAG_REGEX, expresieCurenta, 1);
				String subElement = isThirdLevel(sursa, mIfElseEndif.end(), expresieCurenta, subelementInnerTags);
				if (!"".equals(subElement)) {
					for (String field: subelementInnerTags) {
						addFieldGetterSetter(javaData, subElement + "$" + field);
					}
					addBlockFunctionToJavaFile(javaData, expresieCurentaBuf.toString().replaceAll(SUBELEMENT_INNER_TAG_REGEX, Matcher.quoteReplacement(subElement) + "\\$$1"), 
							nrBlocuri, functionBlockThirdLevelBaseName);
					nrBlocuri++;
					foundLevelTwoOrThree = true;
				}
				
				if (!foundLevelTwoOrThree) {
					addBlockFunctionToJavaFile(javaData, expresieCurentaBuf.toString(), nrBlocuri, getBlockBaseName(expresieCurentaBuf));
					nrBlocuri++;
				}
				
				secondLevelTagsList.add(list);
				
				//System.out.println(expresieCurentaBuf.toString().replaceAll("!!!","&&"));
			}
			startBloc=mIfElseEndif.end();
			if (mIf.find()){
				String  sIf=mIf.group();
//				sIf=sIf.replaceAll(" ","");
				
				List<String> elementInnerTags = RegExUtils.getMatches(ELEMENT_INNER_TAG_REGEX, stemp, 1);
				String multilineTag = isSecondLevel(sursa, mIfElseEndif.end(), stemp, elementInnerTags, false);
				if (!"".equals(multilineTag)) {
					for (String field: elementInnerTags) {
						addFieldGetterSetter(javaData, multilineTag + "$" + field);
					}
				}
				
				List<String> subelementInnerTags = RegExUtils.getMatches(SUBELEMENT_INNER_TAG_REGEX, stemp, 1);
				String subElement = isThirdLevel(sursa, mIfElseEndif.end(), stemp, subelementInnerTags);
				if (!"".equals(subElement)) {
					for (String field: subelementInnerTags) {
						addFieldGetterSetter(javaData, subElement + "$" + field);
					}
				}
				
				expresieCurentaBuf.append("!!!");
				expresieCurenta = sIf.replaceFirst("^\\{ *IF", "").replaceFirst("}$", ""); //sIf.substring(3,sIf.length()-1);
				expresieCurentaBuf.append(expresieCurenta);
			}
			else if(mElse.find()){
				String fost=expresieCurentaBuf.substring(expresieCurentaBuf.lastIndexOf("!!!")+3);
				expresieCurentaBuf.delete(expresieCurentaBuf.lastIndexOf("!!!"),expresieCurentaBuf.length());
				expresieCurentaBuf.append("!!!");
				expresieCurentaBuf.append("(!");
				expresieCurentaBuf.append(fost);
				expresieCurentaBuf.append(")");
			}
			else if(mEndif.find()){
				int poz=expresieCurentaBuf.lastIndexOf("!!!");
				if(poz>0)
					expresieCurentaBuf.delete(poz,expresieCurentaBuf.length());
				else 
					throw new TemplatesException("INCORECT USE OF IF ,ELSE or ENDIF 1");
			}
		}
		if(!expresieCurentaBuf.toString().equals("true")) {
			//System.out.println(expresieCurentaBuf.toString());
			throw new TemplatesException("INCORECT USE OF IF ,ELSE or ENDIF");
		}
		if(nrBlocuri>0){
			
			addBasicBlocksToJavaFile(javaData,blokvec,nrBlocuri);
			addSecondLevelTagsListToJavaFile(javaData, secondLevelTagsList, nrBlocuri);

			int level=0;
			int skip=0;
			int j=0;
			
			doc.findAll(pattExpresionBlock);
						
			try {
				while(doc.find()){
					functionBlockBaseNameBuf.delete(0,functionBlockBaseNameBuf.length());			
					
					String stemp = doc.group();
					Matcher mIf=pattIf.matcher(stemp);
					Matcher mElse=pattElse.matcher(stemp);
					
					boolean foundElse = mElse.find();
					
					String expresion = "";					
					if(mIf.find()){
						level++;
						expresion = mIf.group(1);
					} else if(!foundElse) {
						level--;
						if(level==0 && doc instanceof StringBufferContents){
							stemp = stemp.substring(stemp.indexOf("}")+1);
							doc.replaceCurrentMatchEscape(stemp);
							doc.resetFind();
						}
					} else if(foundElse){
						expresion = mElse.group();
					}
					//System.out.println("*** nivel = "+level +" stemp= "+ stemp);
					if(level>0){
						functionBlockBaseNameBuf.append( "<#" ); 
						functionBlockBaseNameBuf.append( getBlockBaseName(sursa, new StringBuffer(expresion), level) );
						functionBlockBaseNameBuf.append( j-skip ); 
						functionBlockBaseNameBuf.append( "/#>" );
						doc.replaceCurrentMatch(functionBlockBaseNameBuf.toString());
						if(doc instanceof StringBufferContents) {
							doc.resetFind();
						}
					}
					else skip++;
					j++;
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		doc.replaceAll(IF_ELSE_ENDIF_EXPRESION,"");

		return nrBlocuri;
	}
	
	public static String isSecondLevel(String doc, int index, String expression, List<String> elementInnerTags, boolean isForThirdLevel) {
		String multilineTag = "";
		boolean found = false;
		if (index>0 && index<doc.length()) {
			if (elementInnerTags.size()>0) {		//found elements in IF condition
				int index0 = doc.substring(0, index).lastIndexOf(expression);
				if (index0!=-1) {
					int index1 = doc.substring(0, index0).lastIndexOf("<#");			//start index of the possible multiline tag
					while (index1!=-1 && "".equals(multilineTag)) {
						int index2 = doc.indexOf("#>", index1+expression.length());		//end index of the possible multiline tag
						if (index2!=-1) {
							int index3 = doc.indexOf("=", index1);						//index of '=' inside possible multiline tag
							if (index3!=-1 && index3<index2 && index1+2<index3) {
								multilineTag = doc.substring(index1+2, index3).replaceFirst("\\d+$", "").trim();
								if (!MultilineElementsMap.isDefinedAsMultilineTag(multilineTag)) {
									multilineTag = "";
								}
							}
						}
						index1 = doc.substring(0, index1).lastIndexOf("<#");
					}
				}
			}
		}
		if (!"".equals(multilineTag)) {
			found = true;
			for (String field: elementInnerTags) {
				if (!MultilineElementsMap.multilineTagContainsElement(multilineTag, field, isForThirdLevel)) {
					found = false;
					break;
				}
			}
		}
		if (!found) {
			return "";
		}
		return multilineTag;
	}
	
	public static String isThirdLevel(String doc, int index, String expression, List<String> subelementInnerTags) {
		String multilineTagSubElement = "";
		String element = "";
		boolean found = false;
		if (index>0 && index<doc.length()) {
			if (subelementInnerTags.size()>0) {		//found sub-elements in IF condition
				int index0 = doc.substring(0, index).lastIndexOf(expression);
				if (index0!=-1) {
					int index1 = doc.substring(0, index0).lastIndexOf("%$");
					if (index1!=-1) {
						int index2 = doc.indexOf("$%", index1+expression.length());
						if (index2!=-1) {
							int index3 = doc.indexOf("=", index1);
							if (index3!=-1 && index3<index2) {
								element = doc.substring(index1, index3).replaceFirst("^%\\$", "").trim();
								if (!"".equals(element)) {
									String multilineTag = isSecondLevel(doc, index, expression, Arrays.asList(new String[]{element}), true);
									if (!"".equals(multilineTag)) {
										multilineTagSubElement = multilineTag + "$" + element; 
										found = true;
										for (String field: subelementInnerTags) {
											if (!MultilineElementsMap.multilineTagContainsElementSubElement(multilineTag, element, field)) {
												found = false;
												break;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		if (!found) {
			return "";
		}
		return multilineTagSubElement;
	}
	
	private static boolean isTagWithParameters(String s) {
		boolean result = false;
		if (!StringUtils.isEmpty(s)) {
			s = s.replaceFirst("\\A\\(", "");
			s = s.replaceFirst("\\)\\z", "");
			if (s.length()!=0) {
				if (s.matches(TAG_WITH_PARAMETERS)) {
					s = s.replaceAll(PARAMETERS_FOR_TAG, "$1").replaceAll("\\s", "").replaceAll(PARAMETERS_FOR_TAG_ESCAPE, "_");
					if (null!=types.get(s)) {
						result = true;
					}
				}
			}	
		}
		return result;	
	}

	private static String lastBlockName = "";
	private static int lastBlockLevel = -1;
	
	private static String getBlockBaseName(String doc, StringBuffer expresieCurentaBuf, int level) {
		String expresie = expresieCurentaBuf.toString();				
		
		if(level == lastBlockLevel && org.apache.commons.lang.StringUtils.isNotBlank(lastBlockName) && expresie.contains("ELSE")){
			//ELSE block
			lastBlockLevel--;
			return lastBlockName;
		}
		
		if(org.apache.commons.lang.StringUtils.isBlank(expresie)){
			lastBlockLevel = level;
			return lastBlockName;
		}
		
		if (!isTagWithParameters(expresieCurentaBuf.toString())) {
			List<String> elementInnerTags = RegExUtils.getMatches(ELEMENT_INNER_TAG_REGEX, expresieCurentaBuf.toString(), 1);
			if (elementInnerTags.size()>0) {
				lastBlockName = functionBlockSecondLevelBaseName;
				lastBlockLevel = level;
				return functionBlockSecondLevelBaseName;
			}
			
			List<String> subelementInnerTags = RegExUtils.getMatches(SUBELEMENT_INNER_TAG_REGEX, expresieCurentaBuf.toString(), 1);
			if (subelementInnerTags.size()>0) {
				lastBlockName = functionBlockThirdLevelBaseName;
				lastBlockLevel = level;
				return functionBlockThirdLevelBaseName;
			}
		}
		
		//see if it is second level block
		for(DocumentsTags dts : DocumentsTags.values()){
			if(expresie.contains(dts.getName())){
				lastBlockName = functionBlockSecondLevelBaseName;
				lastBlockLevel = level;
				return functionBlockSecondLevelBaseName;
			}
		}
		
		lastBlockName = functionBlockBaseName;
		lastBlockLevel = level;
		return functionBlockBaseName;
	}
	
	private static String getBlockBaseName(StringBuffer expresieCurentaBuf) {
		String expresie = expresieCurentaBuf.toString();				
		
		if(org.apache.commons.lang.StringUtils.isBlank(expresie)){
			return functionBlockBaseName;
		}
		
		//see if it is second level block
		for(DocumentsTags dts : DocumentsTags.values()){
			if(expresie.contains(dts.getName())){
				return functionBlockSecondLevelBaseName;
			}
		}
		
		return functionBlockBaseName;
	}

	private static String clean(String str){
		
		try {
			Matcher m = escapeNewLinesPattern.matcher(str);
			StringBuffer escaped = new StringBuffer(); 
			//Bug 4183
			while(m.find()) {
				try {
					String esc = m.group().replace("\\n","\\\\n").replace("\\\"","\\\\\\\"");
					esc = esc.replaceAll("(?ism)(?<!\\\\)\\\\(?![btnfr\"'\\\\])","\\\\\\\\");
					m.appendReplacement(escaped, Matcher.quoteReplacement(esc));
				}catch(Exception e) {
					/* e.printStackTrace(); */	
				}
			}
			m.appendTail(escaped);
			str = escaped.toString();
		}catch(Exception e) {
			e.printStackTrace();
		}
		str = str.replaceAll("[\t]","\\\\t").replaceAll("[\r]","\\\\r" ).replaceAll("[\n]", "\\\\n");
		str = str.replaceAll( "([^\\\\])[\"]", "$1\\\\\"" );
		return str;
	}
	
	private static void addBasicBlocksToJavaFile(StringBuffer javaData, Vector blokvec,int nrBlocuri){
		javaData.append("\n");
		javaData.append("\tpublic String bloc[] ={");
		for(int i=0;i<nrBlocuri-1;i++){
			javaData.append("\"");
			javaData.append( clean( (String)blokvec.get(i) ) );
			javaData.append("\",\n");
		}
		javaData.append("\"");
		javaData.append( clean(( String)( blokvec.get(nrBlocuri-1)) ));
		javaData.append("\"");
		javaData.append("};");
		javaData.append("\n");
	}
	
	private static void addSecondLevelTagsListToJavaFile(StringBuffer javaData, ArrayList<ArrayList<String>> secondLevelTagsList, int nrBlocuri){
		javaData.append("\n");
		javaData.append("public String secondLevelTagsList[][] ={");
		for(int i=0;i<nrBlocuri;i++){
			ArrayList<String> list = secondLevelTagsList.get(i);
			if (list.size()==0) {
				javaData.append("{},\n");
			} else {
				javaData.append("{");
				for (int j=0;j<list.size()-1;j++) {
					javaData.append("\"");
					javaData.append( clean( (String)list.get(j) ) );
					javaData.append("\",");
				}
				javaData.append("\"");
				javaData.append( clean( (String)list.get(list.size()-1) ) );
				javaData.append("\"},\n");
			}
		}
		javaData.delete(javaData.length()-2, javaData.length());	//last comma
		javaData.append("};");
		javaData.append("\n\n");
	}
	
	private static void  addHeaderConstantsGetersAndSetersToJavaFile(StringBuffer javaData, int template_id){
		
		if(javaData==null){
			javaData = new StringBuffer();
		}
	
		javaData.append("\n");
		javaData.append("import java.lang.reflect.Method;" + "\n");
		javaData.append("import java.util.HashMap;" + "\n");
		javaData.append("import java.util.*;" + "\n");
		javaData.append("\n");
		javaData.append("public class temp" + template_id + "\n");
		javaData.append("{\n");
		
		//introducem constantele
		javaData.append("\tpublic static final long TO_SECCOND=1000;\n");
		javaData.append("\tpublic static final long TO_HOUR=1000*3600;\n");
		javaData.append("\tpublic static final long TO_DAY=TO_HOUR*24;\n");
		javaData.append("\tpublic static final long TO_WEEK=TO_DAY*7;\n");
		javaData.append("\tpublic static final long TO_MOUNTH=TO_DAY*30;\n");
		javaData.append("\tpublic static final long TO_YEAR=TO_DAY*365;\n");
	
		//	introducem membrele
		Iterator i = types.keySet().iterator();
		while ( i.hasNext() )
		{
			String key = i.next().toString();
			String className = ((Class) types.get(key)).getName();
			className = className.substring(className.lastIndexOf(".") + 1);	
			javaData.append("\tpublic " + className + " " + key + ";" + "\n"); 
		}
		
		//	introducem getters and setters
		i = types.keySet().iterator();
		while ( i.hasNext() )
		{
			String key = i.next().toString();
			
			String className = ((Class) types.get(key)).getName();
			className = className.substring(className.lastIndexOf(".") + 1);	
			
			//	getter
			javaData.append("\tpublic " + className + " get" + key + "(){" + "\n");
			javaData.append("\t\treturn " + key + ";" + "\n");
			javaData.append("\t}" + "\n\n");
			
			//	setter
			javaData.append("\tpublic void set" + key + "(" + className + " " + key + "){" + "\n");
			javaData.append("\t\tthis." + key + "=" + key + ";" + "\n");
			javaData.append("\t}" + "\n\n");
		}
	
		//add other header fields, getter and setter
		javaData.append("\n\n\n");
		
		DocumentsTags[] documentsTags = MultilineElementsMap.DocumentsTags.values();
		for(DocumentsTags dT : documentsTags){
			String key = dT.getName();
			String className = dT.getClassType().getName();
			className = className.substring(className.lastIndexOf(".") + 1);	
			javaData.append("\tpublic " + className + " " + key + ";" + "\n"); 
			
			//	getter
			javaData.append("\tpublic " + className + " get" + key + "(){" + "\n");
			javaData.append("\t\treturn " + key + ";" + "\n");
			javaData.append("\t}" + "\n\n");
			
			//	setter
			javaData.append("\tpublic void set" + key + "(" + className + " " + key + "){" + "\n");
			javaData.append("\t\tthis." + key + "=" + key + ";" + "\n");
			javaData.append("\t}" + "\n\n");
		}
		
		//add getBoolean function
		javaData.append("\tpublic boolean getBoolean(String s) {\n");
		javaData.append("\t\tboolean b = false;\n");
		javaData.append("\t\tif (s==null) {\n");
		javaData.append("\t\t\treturn b;\n");
		javaData.append("\t\t}\n");
		javaData.append("\t\ts = s.trim();\n");
		javaData.append("\t\tif (s.equals(\"\")) {\n");
		javaData.append("\t\t\treturn b;\n");
		javaData.append("\t\t}\n");
		javaData.append("\t\ttry {\n");
		javaData.append("\t\t\tb = Boolean.parseBoolean(s);\n");
		javaData.append("\t\t} catch (Exception e) {}\n");
		javaData.append("\t\treturn b;\n");
		javaData.append("\t}" + "\n\n");
				
		//add getInt function
		javaData.append("\tpublic int getInt(String s) {\n");
		javaData.append("\t\tint i = 0;\n");
		javaData.append("\t\tif (s==null) {\n");
		javaData.append("\t\t\treturn i;\n");
		javaData.append("\t\t}\n");
		javaData.append("\t\ts = s.replaceAll(\"[,$]\", \"\").trim();\n");
		javaData.append("\t\tif (s.equals(\"\")) {\n");
		javaData.append("\t\t\treturn i;\n");
		javaData.append("\t\t}\n");
		javaData.append("\t\ttry {\n");
		javaData.append("\t\t\ti = Integer.parseInt(s);\n");
		javaData.append("\t\t} catch (Exception e) {}\n");
		javaData.append("\t\treturn i;\n");
		javaData.append("\t}" + "\n\n");		
		
		//add getDouble function
		javaData.append("\tpublic double getDouble(String s) {\n");
		javaData.append("\t\tdouble d = 0.0d;\n");
		javaData.append("\t\tif (s==null) {\n");
		javaData.append("\t\t\treturn d;\n");
		javaData.append("\t\t}\n");
		javaData.append("\t\ts = s.replaceAll(\"[,$]\", \"\").trim();\n");
		javaData.append("\t\tif (s.equals(\"\")) {\n");
		javaData.append("\t\t\treturn d;\n");
		javaData.append("\t\t}\n");
		javaData.append("\t\ttry {\n");
		javaData.append("\t\t\td = Double.parseDouble(s);\n");
		javaData.append("\t\t} catch (Exception e) {}\n");
		javaData.append("\t\treturn d;\n");
		javaData.append("\t}" + "\n");
		
	}
	
	public static void addFieldGetterSetter(StringBuffer javaData, String key) {
		String s = "\tpublic " + "String" + " " + key + " = \"\";" + "\n";
		
		if (javaData.indexOf(s)==-1) {		//if not already added
			javaData.append(s);
			//	getter
			javaData.append("\tpublic " + "String" + " get" + key + "(){" + "\n");
			javaData.append("\t\treturn " + key + ";" + "\n");
			javaData.append("\t}" + "\n\n");
			
			//	setter
			javaData.append("\tpublic void set" + key + "(" + "String" + " " + key + "){" + "\n");
			javaData.append("\t\tthis." + key + "=" + key + ";" + "\n");
			javaData.append("\t}" + "\n\n");
		}
		
	}
	
	private static void  addConstructorToJavaFile(StringBuffer javaData, int template_id){
		javaData.append("\tpublic temp" + template_id + "(HashMap tags, HashMap types)" + "\n");
		javaData.append("\tthrows Exception" + "\n");
		javaData.append("\t{" + "\n");
		javaData.append("\t\tString key = \"\";" + "\n");
		javaData.append("\t\ttry" + "\n");
		javaData.append("\t\t{" + "\n");
		javaData.append("\t\t\tClass thisClass = this.getClass();" + "\n");
		javaData.append("\t\t\tjava.util.Iterator i = tags.keySet().iterator();" + "\n");
		javaData.append("\t\t\t" + "\n");	
		javaData.append("\t\t\twhile ( i.hasNext() )" + "\n");
		javaData.append("\t\t\t{" + "\n");
		javaData.append("\t\t\t\tkey = i.next().toString();" + "\n");
		javaData.append("\t\t\t\tMethod setValue = thisClass.getMethod(\"set\" + key, new Class[]{(Class) types.get(key)});" + "\n");
		javaData.append("\t\t\t\tsetValue.invoke(this, new Object[]{tags.get(key)});" + "\n");
		javaData.append("\t\t\t}" + "\n");
		javaData.append("\t\t} catch (Exception e){" + "\n");
		javaData.append("\t\t\tthrow new Exception(\"There is a problem with >>>>> \" + key + \" <<<<<  tag. Error msg: \" + e.getMessage());" + "\n");
		javaData.append("\t\t}" + "\n");
		javaData.append("\t}" + "\n");
	}
	
	private static void  addMethod_setLevel1Tags_ToJavaFile(StringBuffer javaData, int template_id){
		javaData.append("\tpublic void setLevel1Tags(HashMap tags, HashMap types)" + "\n");
		javaData.append("\tthrows Exception" + "\n");
		javaData.append("\t{" + "\n");
		javaData.append("\t\tString key = \"\";" + "\n");
		javaData.append("\t\ttry" + "\n");
		javaData.append("\t\t{" + "\n");
		javaData.append("\t\t\tClass thisClass = this.getClass();" + "\n");
		javaData.append("\t\t\tjava.util.Iterator i = tags.keySet().iterator();" + "\n");
		javaData.append("\t\t\t" + "\n");	
		javaData.append("\t\t\twhile ( i.hasNext() )" + "\n");
		javaData.append("\t\t\t{" + "\n");
		javaData.append("\t\t\t\tkey = i.next().toString();" + "\n");
		javaData.append("\t\t\t\tMethod setValue = thisClass.getMethod(\"set\" + key, new Class[]{(Class) types.get(key)});" + "\n");
		javaData.append("\t\t\t\tsetValue.invoke(this, new Object[]{tags.get(key)});" + "\n");
		javaData.append("\t\t\t}" + "\n");
		javaData.append("\t\t} catch (Exception e){" + "\n");
		javaData.append("\t\t\tthrow new Exception(\"There is a problem with >>>>> \" + key + \" <<<<<  tag. Error msg: \" + e.getMessage());" + "\n");
		javaData.append("\t\t}" + "\n");
		javaData.append("\t}" + "\n");
	}
	
	private static void  addMethod_setLevel2Tags_ToJavaFile(StringBuffer javaData){
		javaData.append("\tpublic void setLevel2Tags(String element, ArrayList list, HashMap tags)" + "\n");
		javaData.append("\tthrows Exception" + "\n");
		javaData.append("\t{" + "\n");
		javaData.append("\t\tif (list.size()>0) {" + "\n");
		javaData.append("\t\t\tString key = \"\";" + "\n");
		javaData.append("\t\t\ttry" + "\n");
		javaData.append("\t\t\t{" + "\n");
		
		javaData.append("\t\t\t\tClass thisClass = this.getClass();" + "\n");
		javaData.append("\t\t\t\tfor (int i=0;i<list.size();i++) {" + "\n");
		javaData.append("\t\t\t\t\tObject o = list.get(i);" + "\n");
		javaData.append("\t\t\t\t\tif (o instanceof String) {" + "\n");
		javaData.append("\t\t\t\t\t\tint elem = Integer.parseInt((String)o);" + "\n");
		javaData.append("\t\t\t\t\t\tif (elem<secondLevelTagsList.length) {" + "\n");
		javaData.append("\t\t\t\t\t\t\tString[] values = secondLevelTagsList[elem];" + "\n");
		javaData.append("\t\t\t\t\t\t\tfor (int j=0;j<values.length;j++) {" + "\n");
		javaData.append("\t\t\t\t\t\t\t\tkey = values[j];" + "\n");
		javaData.append("\t\t\t\t\t\t\t\ttry {" + "\n");
		javaData.append("\t\t\t\t\t\t\t\t\tMethod setValue = thisClass.getMethod(\"set\" + element + \"$\" + key, new Class[]{String.class});" + "\n");
		javaData.append("\t\t\t\t\t\t\t\t\tsetValue.invoke(this, new Object[]{tags.get(key)});" + "\n");
		javaData.append("\t\t\t\t\t\t\t\t} catch (Exception e){}" + "\n");
		javaData.append("\t\t\t\t\t\t\t}" + "\n");
		javaData.append("\t\t\t\t\t\t}" + "\n");
		javaData.append("\t\t\t\t\t}" + "\n");
		javaData.append("\t\t\t\t}" + "\n");
		javaData.append("\t\t\t} catch (Exception e){" + "\n");
		javaData.append("\t\t\t\tthrow new Exception(\"There is a problem with >>>>> \" + key + \" <<<<<  tag. Error msg: \" + e.getMessage());" + "\n");
		javaData.append("\t\t\t}" + "\n");
		javaData.append("\t\t}" + "\n");
		javaData.append("\t}" + "\n");
	}
	
	private static void  addMethod_setLevel3Tags_ToJavaFile(StringBuffer javaData){
		javaData.append("\tpublic void setLevel3Tags(HashMap tags)" + "\n");
		javaData.append("\tthrows Exception" + "\n");
		javaData.append("\t{" + "\n");
		javaData.append("\t\tString key = \"\";" + "\n");
		javaData.append("\t\ttry" + "\n");
		javaData.append("\t\t{" + "\n");
		javaData.append("\t\t\tClass thisClass = this.getClass();" + "\n");
		javaData.append("\t\t\tjava.util.Iterator i = tags.keySet().iterator();" + "\n");
		javaData.append("\t\t\t" + "\n");	
		javaData.append("\t\t\twhile ( i.hasNext() )" + "\n");
		javaData.append("\t\t\t{" + "\n");
		javaData.append("\t\t\t\tkey = i.next().toString();" + "\n");
		javaData.append("\t\t\t\ttry {" + "\n");
		javaData.append("\t\t\t\t\tMethod setValue = thisClass.getMethod(\"set\" + key, new Class[]{String.class});" + "\n");
		javaData.append("\t\t\t\t\tsetValue.invoke(this, new Object[]{tags.get(key)});" + "\n");
		javaData.append("\t\t\t\t} catch (Exception e){}" + "\n");
		javaData.append("\t\t\t}" + "\n");
		javaData.append("\t\t} catch (Exception e){" + "\n");
		javaData.append("\t\t\tthrow new Exception(\"There is a problem with >>>>> \" + key + \" <<<<<  tag. Error msg: \" + e.getMessage());" + "\n");
		javaData.append("\t\t}" + "\n");
		javaData.append("\t}" + "\n");
	}
	
	private static void  addConstructorSecondLevelToJavaFile(StringBuffer javaData, int template_id){
		javaData.append("\tpublic temp" + template_id + "(HashMap tags, HashMap values, boolean docTags)" + "\n");
		javaData.append("\tthrows Exception" + "\n");
		javaData.append("\t{" + "\n");
		javaData.append("\t\tString key = \"\";" + "\n");
		javaData.append("\t\ttry" + "\n");
		javaData.append("\t\t{" + "\n");
		javaData.append("\t\t\tClass thisClass = this.getClass();" + "\n");
		javaData.append("\t\t\tjava.util.Iterator i = tags.keySet().iterator();" + "\n");
		javaData.append("\t\t\t" + "\n");	
		javaData.append("\t\t\twhile ( i.hasNext() )" + "\n");
		javaData.append("\t\t\t{" + "\n");
		javaData.append("\t\t\t\tkey = i.next().toString();" + "\n");
		javaData.append("\t\t\t\tMethod setValue = thisClass.getMethod(\"set\" + key, new Class[]{(Class) tags.get(key)});" + "\n");
		javaData.append("\t\t\t\tsetValue.invoke(this, new Object[]{values.get(key)});" + "\n");
		javaData.append("\t\t\t}" + "\n");
		javaData.append("\t\t} catch (Exception e){" + "\n");
		javaData.append("\t\t\tthrow new Exception(\"There is a problem with >>>>> \" + key + \" <<<<<  tag. Error msg: \" + e.getMessage());" + "\n");
		javaData.append("\t\t}" + "\n");
		javaData.append("\t}" + "\n");
	}
	
	private static void addFunctionToJavaFile(StringBuffer javaData,String toEvalCond,String returnType,int functionId ){
		javaData.append("\n");
		javaData.append("\tpublic "+ returnType +" get" + functionBaseName + functionId + "()" + "\n");
		javaData.append("\t{" + "\n");
		javaData.append("\t\treturn (" + ((!"String".equals(returnType))?toEvalCond:"\"\"+("+(toEvalCond)+")") + ");" + "\n");
		javaData.append("\t}" + "\n");
		javaData.append("\n");
	}

	private static void addBlockFunctionToJavaFile(StringBuffer javaData,String expresieCurentaBuf,int  nrBloc, String blockName){
		javaData.append("\n");
		javaData.append("\tpublic Object get" + blockName + nrBloc + "() throws Exception" + "\n");
		javaData.append("\t{" + "\n");
		javaData.append("\t\tif("+expresieCurentaBuf.replaceAll("!!!","&&")+") return bloc["+nrBloc+"] ;" + "\n");
		//System.out.println(expresieCurentaBuf.replaceAll("!!!","&&"));
		javaData.append("\t\treturn " + "\"\"" /*" null "*/ + ";" + "\n");
		javaData.append("\t}" + "\n");
		javaData.append("\n");
	}
	
	public static void addTempFilesNew(TemplateContents doc,
			String outTemplate,
			String javaFile,
			int template_id,String inFileExt) throws TemplatesException
	{

		if(daslCacheRequests.size()>0){
			daslCacheRequests.clear();
		}
		
		int functionId = 0;
		StringBuffer javaData = new StringBuffer();
		addHeaderConstantsGetersAndSetersToJavaFile(javaData,template_id);
		int contor = 0;
		
		if(doc instanceof StringBufferContents 
				&& outTemplate != null && (outTemplate.contains(TemplatesInitUtils.TEMPLATE_BP_CONTAINS) 
						||outTemplate.contains(TemplatesInitUtils.TEMPLATE_CB_CONTAINS))) {
			boolean hasMoreMatches = true;
			String fileContent = doc.toString();
			StringBuilder newFileContent = new StringBuilder();
			Matcher mat = null;
			while (hasMoreMatches) {
				mat = AddDocsTemplates.PATTERN_BPCODE_PRODUCT.matcher(fileContent);
				if (mat.find()) {
					
					String fullLabel = mat.group();
					
					fileContent = fileContent.substring(mat.end());
					mat = AddDocsTemplates.PATTERN_BPCODE_PRODUCT.matcher(fileContent);
					
					String content;
					if(mat.find()) {
						content = fileContent.substring(0, mat.start());
					} else {
						content = fileContent;
					}

					if(newFileContent.length() > 0) {
						newFileContent.append("\n");
					}
					newFileContent.append(fullLabel).append(content.trim());
					
				} else {
					hasMoreMatches = false;
				}
			}
			if(newFileContent.length() > 0) {
				doc = new StringBufferContents(newFileContent.toString().replaceAll("\\\\n", "\n"));
			}
			
		}
		
		for (int i = 0; i < MultilineElementsMap.allSpecialElements.length; i++) {
			String key = MultilineElementsMap.allSpecialElements[i];
			contor = 0;
//			if(doc instanceof StringBufferContents) {
//				doc.findAll("(?s)(<#)(" + key + "[0-9]*)(\\s*=.*?/#>)");
//			} else {
				doc.findAll("(<#)(" + key + "[0-9]*)(\\s*=[^#]*/#>)");
//			}
			while(doc.find()) {
				doc.replaceCurrentMatch("$1" + key + (contor++) + "$3" );
			}
		}
		
		/* Find all the <#documentsX = ... #> tags, and number them like: <# documents1= , <# documents2=, ... */
//		doc.findAll("(<#)(documents[0-9]*)(\\s*=[^#]*/#>)");
//		while(doc.find()) {
//			doc.replaceCurrentMatch("$1" + "documents" + (contor++) + "$3" );
//		}
		/* all the <#documents= ... > tags are numbered properly now */
		
		
		
		
			 
		doc.findAll(pattExpression);
				
		while(doc.find()){
			String found = "";
			try {
				found = doc.group();
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			String toEvalCond = found.replaceAll("<# *", "").replaceAll(" */#>", "")
									 .replaceAll("" + ((char) 148), "\"").replaceAll("" + ((char) 147),"\""); 
			
			StringTokenizer stok=new StringTokenizer(toEvalCond ,"+-*/");
			String tip="";
			Vector<String> tipuri=new Vector<String>();
			boolean gasit=false;
			while(stok.hasMoreTokens()){
				String key=stok.nextToken().replaceAll("[ \t()]*","");
				Class cl =(Class)types.get(key);
				if(cl !=null)
				if((tip=cl.getName())!=null) {
					gasit=true; 
					tipuri.add(tip);
					break;
				}
			}
			if (gasit){
				if( tipuri.contains("java.lang.String")) tip="String";
				else if(tipuri.contains("double")) tip="double";
				else if(tipuri.contains("float")) tip="float";
				else if(tipuri.contains("long")) tip="long";
				else tip="int";
				
				addFunctionToJavaFile(javaData, toEvalCond, tip, functionId);
				doc.replaceCurrentMatch("<#" + functionBaseName + functionId + "/#>");
				doc.resetFind();	/* useless? */
				functionId ++;
			}
			else{
				//throw new TemplatesException("Eror at <#"+toEvalCond+"#>");
			}
		}
		
		doc.findAll(pattConditionalExpression);

		while(doc.find()){
			String found = "";
			try {
				found = doc.group();
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			String toEvalCond = found.replaceAll("<# *", "").replaceAll(" */#>", "")
									 .replaceAll("" + ((char) 148), "\"").replaceAll("" + ((char) 147),"\"");
			addFunctionToJavaFile(javaData,toEvalCond,"String",functionId);
			doc.replaceCurrentMatch("<#" + functionBaseName + functionId + "/#>");
			doc.resetFind();	/* useless? */
			functionId ++;
		}
		
		doc.findAll(stringOperationExpression);
		
		while(doc.find()){
			
			String found = "";
			try {
				found = doc.group();
			}catch(Exception e) {
				e.printStackTrace();
			}
			
			String toEvalCond = found.replaceAll("<# *", "").replaceAll(" */#>", "")
									 .replaceAll("" + ((char) 148), "\"").replaceAll("" + ((char) 147),"\"");
			
			addFunctionToJavaFile(javaData,toEvalCond,"String",functionId);
			doc.replaceCurrentMatch("<#" + functionBaseName + functionId + "/#>");
			doc.resetFind();
			functionId ++;
		}
		
		int numberOfBlocks = findBlocksNew(doc,javaData);
		
		addConstructorToJavaFile(javaData,template_id);
		addConstructorSecondLevelToJavaFile(javaData,template_id);
		addMethod_setLevel1Tags_ToJavaFile(javaData,template_id);
		
		if (numberOfBlocks>0) {
			addMethod_setLevel2Tags_ToJavaFile(javaData);
		}
		
		addMethod_setLevel3Tags_ToJavaFile(javaData);
		
		javaData.append("}");
		//	java write / compile

		//compile templates separately for LD and LD condo 
		if (outTemplate.substring(outTemplate.lastIndexOf(File.separator) + 1).startsWith("defaultLd_temp")){
			javaFile = javaFile.replaceFirst("(temp)(\\d+)", "$1ld$2");
			javaData = new StringBuffer(javaData.toString().replaceAll("(temp)(\\d+)", "$1ld$2"));
		} else if (outTemplate.substring(outTemplate.lastIndexOf(File.separator) + 1).startsWith("defaultLdCondo_temp")){
			javaFile = javaFile.replaceFirst("(temp)(\\d+)", "$1ldc$2");
			javaData = new StringBuffer(javaData.toString().replaceAll("(temp)(\\d+)", "$1ldc$2"));
		}
		try {
			
			//if the dir not exists, attemp to create it
			String fileDir = javaFile.substring(0,javaFile.lastIndexOf(File.separator));
			  if (!(new File(fileDir)).isDirectory())
				    (new File(fileDir)).mkdir();
			  			  
			StringUtils.toFile(javaFile, javaData.toString());
			String classFile = javaFile.replaceAll("\\.java", ".class");
			if ( new File(classFile).exists() )
				new File(classFile).delete();
		} catch (Exception e) {e.printStackTrace();}
		try 
		{
			compileTempClass(javaFile);
		} catch (TemplatesException te) {
			te.printStackTrace(System.err);
			throw te;
		}
		
		if ( !knownExtensions.containsKey(inFileExt))
			throw new TemplatesException("No Format found for this extension");
		
		try {
			doc.saveToFile(outTemplate, inFileExt);
		}catch(Exception e) {
			System.err.println("Problem writing file " + outTemplate + " (extension): " + inFileExt);
			e.printStackTrace();
		}
	}

	public static void compileTempClass(String name) throws TemplatesException
	{
		
		try {

			ByteArrayOutputStream logstr = new ByteArrayOutputStream();
			
			Class c = Class.forName("sun.tools.javac.Main");
            Constructor cons = c.getConstructor(new Class[] { OutputStream.class, String.class });
            Object compiler = cons.newInstance(new Object[] { logstr, "javac" });
            
            Method compile = c.getMethod("compile", new Class [] { String[].class });
            Boolean ok = (Boolean)compile.invoke(compiler, new Object[] {new String[]{name}});
            logger.info("Name: " + name + " generated exception " + logstr);
            if ( !ok.booleanValue() )
            	throw new TemplatesException("File contains unparsable commands. Please re your template and try again.");
			
		} catch (ClassNotFoundException ex) {
			System.err.println(ex.getMessage());
			throw new TemplatesException("Class sun.tools.javac.Main couldn't be found.");
		} catch (Exception exc) {
			System.err.println(exc.getMessage());
			throw new TemplatesException("File contains unparsable commands. Please review your template and try again.");
		} 
  
	}
	
	private static Object replaceSubElementsWithTags(Object dataElement,HashMap<String, String> hm){
		if (dataElement instanceof Vector) {
			Vector datavec = (Vector) dataElement;
			Vector newVec = new Vector();
			
			for(int i=0;i<datavec.size();i++){
				String str = (String)datavec.get(i);
				str = ro.cst.tsearch.templates.TemplateUtils.replaceSpecialElements(str, "%$", "$%", hm);
				str = ro.cst.tsearch.templates.TemplateUtils.replaceSpecialElements(str, "[$", "$]", hm);
				newVec.add(str);
			}
			
			dataElement = newVec;
		}
		else if(dataElement instanceof String){			
			String str = (String)dataElement ;
			str = ro.cst.tsearch.templates.TemplateUtils.replaceSpecialElements(str, "%$", "$%", hm);
			str = ro.cst.tsearch.templates.TemplateUtils.replaceSpecialElements(str, "[$", "$]", hm);
			dataElement = str;
		}
		return dataElement;
	}
	
	public static String completeNewTemplatesV2ForTextFilesOnly(HashMap<String,Object> inputs,
			String outputFile,
			CommunityTemplatesMapper curentTemplate,
			Search search,
			boolean finalCreation, 
			String undefinedContents, 
			List instrumentListForUndefined,
			Map<String,String> bolilerPlatesTSR) throws TemplatesException {
		return completeNewTemplatesV2ForTextFilesOnly(inputs, outputFile, curentTemplate, search, finalCreation, undefinedContents, instrumentListForUndefined, bolilerPlatesTSR, false);
	}
	
	public static String completeNewTemplatesV2ForTextFilesOnly(HashMap<String,Object> inputs,
			String outputFile,
			CommunityTemplatesMapper curentTemplate,
			Search search,
			boolean finalCreation, 
			String undefinedContents, 
			List instrumentListForUndefined,
			Map<String,String> bolilerPlatesTSR, boolean doNotSave) throws TemplatesException {
		try {
			return completeNewTemplatesV2New(inputs, outputFile, curentTemplate, search, finalCreation, undefinedContents, instrumentListForUndefined, bolilerPlatesTSR, doNotSave);
		}catch(Exception e) {
			throw new TemplatesException(e);
		}
	}
	
	
	public static String completeNewTemplatesV2New(HashMap<String,Object> inputs,
			String outputFile,
			CommunityTemplatesMapper template,
			Search search,
			boolean finalCreation, 
			String undefinedContents, 
			List instrumentListForUndefined,
			Map<String,String> bolilerPlatesTSR, 
			boolean doNotSave) throws Exception
	{
		return completeNewTemplatesV2New(
				inputs, 
				outputFile, 
				template, 
				search, 
				finalCreation, 
				undefinedContents, 
				instrumentListForUndefined, 
				bolilerPlatesTSR,
				doNotSave, 
				false, 
				null, 
				null);
	}
	
	/**
	 * completeNewTemplatesV2New
	 * @param finalCreation
	 * @param undefinedContents
	 * @param instrumentListForUndefined
	 * @param doNotSave TODO
	 * @param bpCodeToFill 
	 * @param HashMap inputs
	 * @param String outputFile
	 * @param HashMap templateInfo
	 * @param Search search1
	 * @param int documentType
    */	
	public static String completeNewTemplatesV2New(HashMap<String,Object> inputs,
			String outputFile,
			CommunityTemplatesMapper template,
			Search search,
			boolean finalCreation, 
			String undefinedContents, 
			List instrumentListForUndefined,
			Map<String,String> bolilerPlatesTSR, 
			boolean doNotSave, 
			boolean getBoilerMap, 
			String bpCodeToFill, 
			String documentIdToFill) 
					throws Exception
	{
		try {
			return _completeNewTemplatesV2New(inputs, outputFile, template, search, finalCreation, undefinedContents, instrumentListForUndefined,
					bolilerPlatesTSR, doNotSave, getBoilerMap, bpCodeToFill, documentIdToFill);
		} catch (DisposedException de) {
			logger.error("Received DisposedException - retrying", de);
			//sleep random - max 30 seconds
			try {
				Thread.sleep((long)(Math.random() * 1000 * 30));
			} catch (InterruptedException ie) {
			}
			return _completeNewTemplatesV2New(inputs, outputFile, template, search, finalCreation, undefinedContents, instrumentListForUndefined,
					bolilerPlatesTSR, doNotSave, getBoilerMap, bpCodeToFill, documentIdToFill);
		}
	}
	
	/**
	 * Internal method that should only be called by completeNewTemplatesV2New in order to treat part of the exceptions and retry the method
	 * @param inputs
	 * @param outputFile
	 * @param template
	 * @param search
	 * @param finalCreation
	 * @param undefinedContents
	 * @param instrumentListForUndefined
	 * @param bolilerPlatesTSR
	 * @param doNotSave
	 * @param getBoilerMap
	 * @param bpCodeToFill
	 * @param documentIdToFill
	 * @return
	 * @throws Exception
	 */
	private static String _completeNewTemplatesV2New(HashMap<String,Object> inputs,
																	String outputFile,
																	CommunityTemplatesMapper template,
																	Search search,
																	boolean finalCreation, 
																	String undefinedContents, 
																	List instrumentListForUndefined,
																	Map<String,String> bolilerPlatesTSR, 
																	boolean doNotSave, 
																	boolean getBoilerMap, 
																	String bpCodeToFill, 
																	String documentIdToFill) 
																			throws Exception
	{
		int iterationNr = 2;
		
		HashMap<String,String> inputsForMultiline = null;
		
		String fileName 		= template.getPath();				
		int templateId	  		= (int)template.getId();
		int commId				= template.getCommunityId();
		
		if(instrumentListForUndefined==null) {
			instrumentListForUndefined = new ArrayList();
		}
		
		//add values for tags with parameters
		if (template!=null) {
			String contents = template.getFileContent();
			Matcher matcher = Pattern.compile(AddDocsTemplates.TAG_WITH_PARAMETERS).matcher(contents);
			Set<String> tagsWithParams = new HashSet<String>();				//we use a set in order to retain a tag only once
			while (matcher.find()) {
				String tag = (matcher.group(1) + "(" + matcher.group(2) + ")");
				tagsWithParams.add(tag);
			}
			Map<String, String> cbMap = null;
			for (String tag: tagsWithParams) {	
				Object dataElement = null;
				
				if(cbMap == null && (tag.startsWith(AddDocsTemplates.bpCode + "(") || tag.startsWith(AddDocsTemplates.cbCode + "("))) {
					try{
						long userId = search.getSa().getAbstractorObject().getID().longValue();
						cbMap = new CaseInsensitiveMap<>(TemplatesServlet.getBoilerPlatesMap(search.getID(), userId, false, true, null, null));
					}catch (Exception e){
						e.printStackTrace();
					}
				}
				
				dataElement = getValueOfTagWithParameters(search, tag, cbMap);
				if (dataElement!=null && !(tag.startsWith(AddDocsTemplates.bpCode + "(") || tag.startsWith(AddDocsTemplates.cbCode + "("))) {
					inputs.remove(tag);
					String cleanedTag = tag.replaceAll(PARAMETERS_FOR_TAG, "$1").replaceAll("\\s", "").replaceAll(PARAMETERS_FOR_TAG_ESCAPE, "_");
					inputs.put(cleanedTag, dataElement);
					Class type = types.get(tag.replaceAll(PARAMETERS_FOR_TAG, ""));
					types.put(cleanedTag,type);
				}
			}
		}
		
		TemplateType templateType = TemplateType.NA;
		
		boolean isDasl = fileName.startsWith("DASL");
		boolean isLegal = fileName.startsWith(LEGAL_TEMPLATE_NAME) || fileName.startsWith(LEGAL_CONDO_TEMPLATE_NAME);
		boolean isBaseFile = fileName.startsWith(ro.cst.tsearch.templates.TemplateUtils.BASE_FILE_TEMPLATE_NAME);
		boolean isUndefinedElementCompilation = (undefinedContents!=null && undefinedContents.length()>0);
		boolean isDoc = fileName.toLowerCase().endsWith(".doc") /*|| fileName.toLowerCase().endsWith(".docx")*/;
		boolean isAts = fileName.toLowerCase().endsWith(".ats");
		
		boolean hasAlternateVals = false;
		Vector <String>newKeys = new Vector<String>();

		String inFile 		    = fileName;
		
		boolean isCodeBookLibrary = (fileName.contains(TemplatesInitUtils.TEMPLATE_BP_CONTAINS) 
				|| fileName.contains(TemplatesInitUtils.TEMPLATE_CB_CONTAINS));
		if(isCodeBookLibrary) {
			templateType = TemplateType.CBLibrary;
		}
		String[] codeBookRows = null;
		String initialCodeToFill = bpCodeToFill;
		
		String productId = "0";
		
//		boolean singleBPRequested = (fileName.contains(TemplatesInitUtils.TEMPLATE_BP_CONTAINS) 
//					|| fileName.contains(TemplatesInitUtils.TEMPLATE_CB_CONTAINS))
//				&& org.apache.commons.lang.StringUtils.isNotBlank(bpCodeToFill);
		
		//String inFileExt 		= fileName.substring(fileName.lastIndexOf(".") + 1);
			
		Class<?> templateAttachedClass = null;
		Object templateAttachedObject = null;
		//SymbolTable st = null;
		
		String className = "temp" + templateId;
		
		//templates compiled separately for LD and LD condo 
		if (fileName.startsWith("defaultLd.")){
			className = "templd" + templateId;
		} else if (fileName.startsWith("defaultLdCondo.")){
			className = "templdc" + templateId;
		}
		
		try {
			String classURI = UploadPolicyDoc.getTemplatesPath(commId);
			ClassLoader cl = new URLClassLoader( new URL[] { (new File(classURI)).toURI().toURL() });
			templateAttachedClass = cl.loadClass(className);
			Constructor<?> cons = templateAttachedClass.getConstructor(new Class[] {HashMap.class, HashMap.class});
			templateAttachedObject = cons.newInstance(new Object[] { prepareInputs(inputs), types });
			//st = (SymbolTable) templateAttachedClass.getMethod("getSymbolTable").invoke(templateAttachedObject);
			
			hasAlternateVals = true;
			inFile = UploadPolicyDoc.getGeneratedTemplateFileName(inFile,templateId, commId);
			
		} catch(ClassNotFoundException cnfe) {
			
			hasAlternateVals = false;
			inputs = prepareInputs( inputs ) ;
			
		}  catch (Exception e) {
			e.printStackTrace();
			throw new TemplatesException(e.getCause().getMessage());
		}
			
		
		
		try{
			productId = search.getSa().getAtribute(SearchAttributes.SEARCH_PRODUCT);
		} catch (Exception e){
			if(!isDasl) {
				logger.error("Cannot get search product:");
				e.printStackTrace();
			}
		}
			
		TemplateContents doc = null;
		
		if(isDasl){
			String str = daslCacheRequests .get(inFile);
			if(str!=null){
				doc = new StringBufferContents(str );
			}
			if(doc  == null ){
				doc = new StringBufferContents(StringUtils.fileToString(inFile));
				daslCacheRequests.put(inFile, doc.toString());
			}
		}else{
			inputsForMultiline = getInputsForMultiline(inputs);
			if(isDoc) {
				doc = new OfficeDocumentContents(inFile);
			}else {
				
				String fileToString = StringUtils.fileToString(inFile);
				
				if(isCodeBookLibrary) {
					fileToString = ro.cst.tsearch.templates.TemplateUtils.fixAllRecursiveStaticCodes(Integer.parseInt(productId), fileToString);
				}
				
				if(isCodeBookLibrary && org.apache.commons.lang.StringUtils.isNotBlank(initialCodeToFill)) {
					
					//had to change and complete the entire library because some components might use other codes that would have been deleted in the cleaning process
					
					//go for exact codes
//					Matcher mat = Pattern.compile("(?i)(?:\\A|\\n)(" + Matcher.quoteReplacement(bpCodeToFill) + "_([0-9]+))\\s*=").matcher(fileToString);
//					String fileJustForByCode = null;
//					while(mat.find()) {
//						String label = mat.group(1);
//						// take just boilers for product id or default TemplateUtils.BOILER_MAP_DEFAULT_PRODUCT
//						boolean perfectMatch = label.equalsIgnoreCase(bpCodeToFill + "_" + productId);
//						if (perfectMatch
//								|| (label.equalsIgnoreCase(bpCodeToFill + "_" + ro.cst.tsearch.templates.TemplateUtils.BOILER_MAP_DEFAULT_PRODUCT) 
//										&& fileJustForByCode == null)) {
////							label = label.split(TemplatesServlet.LABEL_NAME_DELIM)[0];
//							
//							String fileToStringFragment = fileToString.substring(mat.end());
//							Matcher matcherInner = AddDocsTemplates.PATTERN_BPCODE_PRODUCT.matcher(fileToStringFragment);
////							String content = matcherInner.find() ? fileToStringFragment.substring(0, matcherInner.start()) : fileToStringFragment;
//							String content = null;
//							if(matcherInner.find()) {
//								content = fileToStringFragment.substring(0, matcherInner.start() - 1);
//							} else {
//								content = fileToStringFragment;
//							}
//							fileJustForByCode = mat.group() + content;
//							
//							if(perfectMatch) {
//								break;
//							}
//							
//						}
//					}
//					
//					if(fileJustForByCode == null) {
//						fileJustForByCode = "";
//						
//						doc = new StringBufferContents(fileJustForByCode);
//						if (!isDasl && !isUndefinedElementCompilation && !isLegal && !isBaseFile && !doNotSave){
//							doc.saveToFile(outputFile);
//						}
//						
//						return doc.toString() ;
//					}
//					
//					doc = new StringBufferContents(fileJustForByCode);
					
					doc = new StringBufferContents(fileToString);
					
//					doc = new StringBufferContents("PLDEF_1=<#documents=(|PLAT|)(|GoogleMap|)Plat PLDEF Product 1 from %$GRANTOR$% to %$GRANTEE$% conveying <#BlocComponent_4/#><#BlocComponent_5/#> the land described <#BlocComponent_6/#><#BlocComponent_7/#> in Schedule \"A\".  The purpose of this Plat is to TEST. Recorded in Official Records %$BP_INST_DOC1$%, of the Public Records of <#county/#> County, OR./#>");
				} else {
					if(getBoilerMap && isCodeBookLibrary) {
						codeBookRows = fileToString.split("\\r?\\n", -1);
					}
					
					doc = new StringBufferContents(fileToString);
				}
			}
			iterationNr =3;
		}
		
		try {
			if(isUndefinedElementCompilation)
				doc = new StringBufferContents(undefinedContents);
			
			Map<String, String> cbMap = null;
			
			for(int ki=0; ki<iterationNr; ki++){
			
				int codeBookRowIndex = 0;
				
				//we don't want to modify the content
//				if(doc instanceof StringBufferContents) {
//					doc.findAll(Pattern.compile("(?s)<#.*?/#>"));
//				} else {
					doc.findAll(pattTag);
//				}
				
				newKeys.clear();
				while(doc.find()){
					String keyName = doc.group().replaceAll("<#[ \t]*", "").replaceAll("[ \t]*/#>", "");
					newKeys.add(keyName);
				}
				
				Iterator<String> i;			
				if ( hasAlternateVals )
					i = newKeys.iterator();
				else 
					i = inputs.keySet().iterator();
				while (i.hasNext())
				{
					Object dataElement = null;
					String key = i.next().toString();
					
					int pozEqual=-1;
					boolean isMultiline = ((pozEqual=key.indexOf("="))>0);
					String elementTag="";
					if (hasAlternateVals)
					{
						try
						{
							
							Matcher parcelIdFormatMatcher = PARCEL_ID_FORMAT_PATTERN.matcher(key);
							if(parcelIdFormatMatcher.find()) {
								Method getValue = templateAttachedClass.getMethod("get" + parcelId, new Class[]{});
								if (templateAttachedObject != null)
									dataElement = getValue.invoke(templateAttachedObject, new Object[]{});
								dataElement = formatParcelId( (String)dataElement, parcelIdFormatMatcher.group(1));
							}else {
								Method getValue = templateAttachedClass.getMethod("get" + key, new Class[]{});
								if (templateAttachedObject != null)
									dataElement = getValue.invoke(templateAttachedObject, new Object[]{});
							}

							if("crash".equals(key)) throw new TemplatesException("A fost generat un crash intentionat");

							if("ATSExportDataPreview".equals(key)) {
								dataElement = TemplateBuilder.getAtsExportDataPreviewTag(inFile, search);
							}

						} catch (NoSuchMethodException nsme) {
							if("crash".equals(key)) throw new TemplatesException("A fost generat un crash intentionat");
							//	tagul cautat nu are corespondenta, de aceea se va inlocui cu el insusi in template
							//	pentru a semnala acest lucru.
							boolean elemGasit=false;
							
							if(isMultiline){//new multiline element
								elementTag = key.substring(0,pozEqual).replaceAll("[ \t]","");
								String formItem   = key.substring(pozEqual+1);
								String elementTagCleaned = elementTag.replaceAll("([^0-9]*)([0-9]*)", "$1");
								elemGasit = MultilineElementsMap.isDefinedAsMultilineTag( elementTagCleaned );
								if(elemGasit ){
									
									//I need to tell the compile which CB is filling only when compiling the entire CB Library
									if(getBoilerMap && isCodeBookLibrary && AddDocsTemplates.documents.equals(elementTagCleaned)
											&& doc instanceof StringBufferContents && codeBookRows != null) {
										
										bpCodeToFill = null;
										documentIdToFill = null;
										
										String[] keyParts = key.split("\\r?\\n", -1);
										
										for (int codeBookIndex = codeBookRowIndex; codeBookIndex < codeBookRows.length; codeBookIndex++) {
											
											int indexOf = -1;
											
											for (int keyPartIndex = 0; keyPartIndex < keyParts.length; keyPartIndex++) {
												
												String normalizedKeyPart = keyParts[keyPartIndex].replaceAll("==_==(.*?)==_==", "<#$1/#>");
												
												if(keyPartIndex == 0) {
													indexOf = codeBookRows[codeBookIndex].indexOf(normalizedKeyPart);
													if(indexOf < 0) {
														//no need 
														break;
													}
												} else {
													if(codeBookIndex + keyPartIndex < codeBookRows.length) {
														if(!codeBookRows[codeBookIndex + keyPartIndex].equals(normalizedKeyPart)) {
															if((keyPartIndex == keyParts.length - 1) && normalizedKeyPart.isEmpty() && codeBookRows[codeBookIndex + keyPartIndex].equals("/#>")) {
																
															} else {
																indexOf = -1;
																break;
															}
														}
													} else {
														indexOf = -1;
														break;
													}
												}
											}
											
											if(indexOf > 0 && keyParts.length > 0) {
												codeBookRowIndex = codeBookIndex + keyParts.length; //continue with next
												
												String firstPart = codeBookRows[codeBookIndex].substring(0, indexOf).replaceAll("<#[ \t]*", "").trim();
												String lastPart = codeBookRows[codeBookIndex].substring(indexOf + keyParts[0].replaceAll("==_==(.*?)==_==", "<#$1/#>").length()).replaceAll("[ \t]*/#>", "");
												
												if(org.apache.commons.lang.StringUtils.isBlank(lastPart)) {
													Matcher mat = AddDocsTemplates.PATTERN_BPCODE_PRODUCT.matcher(firstPart);
													if(mat.matches()) {
														String label = mat.group(1);
														// take just boilers for product id or default TemplateUtils.BOILER_MAP_DEFAULT_PRODUCT
														if (mat.group(3).equals(productId)) {
															bpCodeToFill = mat.group(2);
														} else if(mat.group(3).equals(ro.cst.tsearch.templates.TemplateUtils.BOILER_MAP_DEFAULT_PRODUCT)){
															if(((StringBufferContents)doc).getStringBuffer().indexOf(label) < 0) {
																bpCodeToFill = mat.group(2);
															}
														}
																
														
													}
												}
												
												break;
											}
										}
										
									}
									
									dataElement = resolveMultiLineTagContent(elementTagCleaned, formItem,search,instrumentListForUndefined,bolilerPlatesTSR,templateAttachedClass, templateType, inputs, getBoilerMap, bpCodeToFill, documentIdToFill);
									dataElement = replaceSubElementsWithTags(dataElement,inputsForMultiline );
								}
							}
							if (key.matches(TAG_WITH_PARAMETERS)) {		
								if(cbMap == null && (
										key.startsWith(AddDocsTemplates.bpCode + "(") 
										|| key.startsWith(AddDocsTemplates.cbCode + "("))) {
									
									Object o = search.getAdditionalInfo(ro.cst.tsearch.templates.TemplateUtils.BOILER_MAP);
									if (o != null && o instanceof Map) {
										cbMap =  new CaseInsensitiveMap<>((Map<String, String>) o);
									}
									
									if(cbMap == null) {
									
										try{
											long userId = search.getSa().getAbstractorObject().getID().longValue();
											cbMap = new CaseInsensitiveMap<>(TemplatesServlet.getBoilerPlatesMap(search.getID(), userId, false, true, null, null));
											
										}catch (Exception e){
											e.printStackTrace();
										}
									}
								}
								dataElement = getValueOfTagWithParameters(search, key, cbMap);
								if (dataElement!=null && !(
										key.startsWith(AddDocsTemplates.bpCode + "(")
										|| key.startsWith(AddDocsTemplates.cbCode + "("))) {
									inputs.remove(key);
									String cleanedKey = key.replaceAll(PARAMETERS_FOR_TAG, "$1").replaceAll("\\s", "").replaceAll(PARAMETERS_FOR_TAG_ESCAPE, "_");
									inputs.put(cleanedKey, dataElement);
									Class type = types.get(key.replaceAll(PARAMETERS_FOR_TAG, ""));
									types.put(cleanedKey,type);
									Constructor<?> cons = templateAttachedClass.getConstructor(new Class[] {HashMap.class, HashMap.class});
									templateAttachedObject = cons.newInstance(new Object[] { prepareInputs(inputs), types });
									elemGasit = true;
								} else if (dataElement!=null && (key.startsWith(AddDocsTemplates.bpCode + "(") || key.startsWith(AddDocsTemplates.cbCode + "("))){
									elemGasit = true;
								}
									
							}
							if(!elemGasit ){
								if(!DELETE_LAST_PATTERN.matcher(key).find()) {
									dataElement = "<#" + key + "/#>";
									System.err.println("tagul cautat < "+key +" >nu are corespondenta, de aceea se va inlocui cu el insusi in template " + fileName);
									
//									Set<Long> set = UpdateTemplates.MISSING_TAGS.get(key);
//									if(set == null) {
//										set = new LinkedHashSet<>();
//										UpdateTemplates.MISSING_TAGS.put(key, set);
//									}
//									set.add(new Long(templateId));
									continue;
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
							if("crash".equals(key)) throw new TemplatesException("A fost generat un crash intentionat");
							dataElement = "<# Err: " + key + "/#>";
							e.printStackTrace();
						}
						
					}
					else
					{
						dataElement = inputs.get(key).toString();
					}
					
					if ( dataElement == null ||dataElement.toString().equals(Double.MIN_VALUE+"")||dataElement.toString().equals(Integer.MIN_VALUE+"")||dataElement.toString().equals(Integer.MAX_VALUE+"")
											||dataElement.toString().equals(Long.MIN_VALUE+"")||dataElement.toString().equals(Float.MIN_VALUE+"")){
						dataElement = "";
					}
				
					Pattern patKey=null;
					if(isMultiline){
//						if(doc instanceof StringBufferContents) {
//							patKey = Pattern.compile("(?s)<#[ \t]*"+ elementTag+"[ \t]*"+".*?"+"/#>");
//						} else {
							patKey = Pattern.compile("<#[ \t]*"+ elementTag+ "[ \t]*=" + "[ \t]*"+"[^#]*"+"[ \t]*/#>");
//						}
					}else{
						patKey = Pattern.compile("<#[ \t]*"+ key.replace("(", "\\(").replace(")", "\\)") +"[ \t]*/#>");
					}
					
					doc.findAll(patKey);
									
					//temporary patch
					if(key!=null && isAts && dataElement instanceof String){
						if(requiresCleaning(key)){ 
							if(dataElement instanceof String){
								dataElement = TemplateUtils.cleanStringForAIM(dataElement.toString(),true, false);
							}
						}
					}
					 
					if (dataElement instanceof Vector) 
					{
						Vector data= (Vector) dataElement;
						if(data.size()>0 && !isUndefinedElementCompilation) {
							Matcher m = undefinedTag.matcher(data.elementAt(0).toString());
							if(m.matches()) continue;
						}
						if(!isMultiline){
							while( doc.find()){	
								String element = doc.group();
								Matcher m = undefinedTag.matcher(element);
								if(m.matches() && !isUndefinedElementCompilation) continue;
								
								//String xLocalText = matKey.group();
								Vector paragraphData = (Vector) dataElement;
								StringBuffer  strVecContent = new StringBuffer("");
								if (isAts){
									for (int k = 0; k < paragraphData.size(); k++){
										String stringParagraph=TemplateUtils.cleanStringForAIM(paragraphData.elementAt(k).toString(),true, false);
										if (k != paragraphData.size() - 1){
											strVecContent.append(stringParagraph);
											strVecContent.append("\n");
										}
										else{
											strVecContent.append(TemplateUtils.cleanStringForAIM(paragraphData.elementAt(k).toString(),true, false));
										}
									}
								}
								else{
									for (int k = 0; k < paragraphData.size(); k++){
										if (k != paragraphData.size() - 1){
											String text = paragraphData.elementAt(k).toString();
											if(isDoc && doc instanceof OfficeDocumentContents) {
												text=text.replaceAll("\\|\\|",(char)13+"");
											}
											strVecContent.append(text);
											if(doc instanceof OfficeDocumentContents) {
											//	strVecContent.append((char)13);
											}else {
												strVecContent.append("\n");
											}
										}
										else{
											String text = paragraphData.elementAt(k).toString();
											if(isDoc && doc instanceof OfficeDocumentContents) {
												text=text.replaceAll("\\|\\|",(char)13+"");
											}
											strVecContent.append(text);
										}
									}
								}
								doc.replaceCurrentMatchEscape(strVecContent.toString());
								doc.resetFind();
							}
						}
						else{
							while( doc.find()){	
								String element = doc.group();
								Matcher m = undefinedTag.matcher(element);
								if(m.matches() && !isUndefinedElementCompilation) continue;
								
								Vector<String> paragraphData = (Vector<String>) dataElement;
								StringBuffer  strVecContent = new StringBuffer("");
								if (isAts){
									for (int k = 0; k < paragraphData.size(); k++){
										strVecContent.append(TemplateUtils.cleanStringForAIMNew(paragraphData.elementAt(k).toString(),true, true));
									}
								}
								else{
									for (int k = 0; k < paragraphData.size(); k++){
										if (isDoc && k != paragraphData.size() - 1){
											String text = paragraphData.elementAt(k).toString();
											if(isDoc && doc instanceof OfficeDocumentContents) {
												text=text.replaceAll("\\|\\|",(char)13+"");
											}
											strVecContent.append(text);
											if(doc instanceof OfficeDocumentContents) {
												//strVecContent.append((char)13);
											}else {
												strVecContent.append("\n");
											}
										}else{
											String text = paragraphData.elementAt(k).toString();
											if(isDoc && doc instanceof OfficeDocumentContents) {
												text=text.replaceAll("\\|\\|",(char)13+"");
											}
											strVecContent.append(text);
										}
										//strVecContent.append(paragraphData.elementAt(k).toString());
									}
									
								}
								doc.replaceCurrentMatchEscape(strVecContent.toString() );
								doc.resetFind();
								break;
							}
						}
					} else {
						String replData = "";
						if ((dataElement instanceof Double)) {
							DecimalFormat format = new DecimalFormat("#,##0.00");
							replData = format.format(((Double) dataElement).doubleValue());
							if (isAts && ki == 1) {
								replData = AddDocsTemplates.cleanStringForAIM(replData, true);
							}
						} else if ((dataElement instanceof Float)) {
							DecimalFormat format = new DecimalFormat("#,##0.00");
							replData = format.format(((Float) dataElement).doubleValue());
							if (isAts && ki == 1) {
								replData = AddDocsTemplates.cleanStringForAIM(replData, true);
							}
						} else {
							replData = dataElement.toString();
							Matcher m = undefinedTag.matcher(replData);
							if (m.matches() && !isUndefinedElementCompilation)
								continue;
							if (isAts && !key.startsWith(functionBlockBaseName)) {
								replData = AddDocsTemplates.cleanStringForAIM(replData, true);
							}
						}
						// avoid infinite loops
						String regex = "";
						if (isMultiline) {
							regex = "<#[ \t]*" + elementTag + "[ \t]*" + "[^#]*" + "[ \t]*/#>";
						} else {
							regex = "<#[ \t]*" + key.replace("(", "\\(").replace(")", "\\)") + "[ \t]*/#>";
						}
						if (!isMultiline) {
							
							// we need to keep second/third level conditions for later evaluation, at this moment we don't have the info
							if(key.startsWith(functionBlockSecondLevelBaseName)){
								replData = secondLevelBlockPrefix+key+secondLevelBlockPrefix;
							} else if(key.startsWith(functionBlockThirdLevelBaseName)){
								replData = thirdLevelBlockPrefix+key+thirdLevelBlockPrefix;
							} 							
							
							while (doc.find()) {
								if (replData.matches(regex)) {
									replData = replData.replaceAll("#", "");
								}
								if (isDoc && doc instanceof OfficeDocumentContents) {
									if (key.startsWith(functionBlockBaseName)) {
										Matcher m = escapeNewLinesPattern.matcher(replData);
										StringBuffer completed = new StringBuffer();
										while (m.find()) {
											try {
												String tag = m.group().replaceAll("\\|\\|",
														TemplateBuilder.NOT_EXIST_COD);
												m.appendReplacement(completed, Matcher.quoteReplacement(tag));
											} catch (Exception e) {
												/* e.printStackTrace(); */
											}
										}
										m.appendTail(completed);
										replData = completed.toString();
										replData = replData.replaceAll("\\|\\|", (char) 13 + "");
										replData = replData.replaceAll(TemplateBuilder.NOT_EXIST_COD, "||");

									} else {
										replData = replData.replaceAll("\\|\\|", (char) 13 + "");
									}
								}
								if(replData.matches("[\\s]+/#>") && isDoc){
									replData = replData.replaceAll("[\\s]+/#>", "\\\\n || /#>");
								}
								
								if(key.equals("TSD_Patriots")) {
									doc.replaceCurrentMatchEscapeHtml(replData);
								} else {
									doc.replaceCurrentMatchEscape(replData);
								}
								try {
									Matcher deleteLastMatcher = DELETE_LAST_PATTERN.matcher(key);
									if (deleteLastMatcher.find()) {
										int charsToDelete = Integer.parseInt(deleteLastMatcher.group(1));
										/*
										 * We do not want to delete over some
										 * special ('breaking') characters. If
										 * we detect we are about to delete
										 * them, make the charsToDelete smaller
										 */
										if (doc instanceof StringBufferContents) {
											StringBufferContents sbc = (StringBufferContents) doc;
											Matcher matKey = sbc.getMatcher();
											for (String br : new String[] { "<!--" }) {
												charsToDelete = Math.min(charsToDelete, matKey.start()
														- (sbc.getStringBuffer().substring(0, matKey.start())
																.lastIndexOf(br) + br.length()));
											}
											sbc.getStringBuffer()
													.delete(matKey.start() - charsToDelete, matKey.start());
										}
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
								doc.resetFind();
							}
						} else {
							if (doc.find()) {
								if (replData.matches(regex)) {
									replData = replData.replaceAll("#", "");
								}
								doc.replaceCurrentMatchEscape(replData);
								doc.resetFind();
							}
						}
					}
				}
			}
			
			//remove second level uncompleted tags
			if(doc instanceof OfficeDocumentContents){
				
				// list of regular expresions for open office 
				// http://wiki.openoffice.org/wiki/Documentation/How_Tos/Regular_Expressions_in_Writer
				doc.resetFind();
				doc.findAll(secondLevelBlockPrefix + functionBlockSecondLevelBaseName + "[:digit:]+" + secondLevelBlockPrefix);
				while(doc.find()){
					((OfficeDocumentContents)doc).replaceCurrentMatch("");
				}
				doc.resetFind();
				doc.findAll(thirdLevelBlockPrefix + functionBlockThirdLevelBaseName + "[:digit:]+" + thirdLevelBlockPrefix);
				while(doc.find()){
					((OfficeDocumentContents)doc).replaceCurrentMatch("");
				}
			} else {
				doc.findAll(secondLevelBlockPattern);
				while(doc.find()){
					doc.replaceCurrentMatchEscape("");
					doc.resetFind();
				}
				doc.findAll(thirdLevelBlockPattern);
				while(doc.find()){
					doc.replaceCurrentMatchEscape("");
					doc.resetFind();
				}
			}
			
			String url = LoadConfig.getLoadBalancingUrl();
			int port = LoadConfig.getLoadBalancingPort();
			
			String fullUrl = url.replaceFirst("ats(?:prdinet|stginet|preinet)?", "ats(?:prdinet|stginet|preinet)?[0-9]*") + ":" + port;
	
			String templateContent = doc.toString();
			
			//the next code is because open office does not have NON-GREEDY functions on RE
			
			Pattern pa1 = Pattern.compile( "[<][ ]*a[ ]+href=\"([^\"]+)\"[ ]*[>]([^<]*)[<].{3}" );
			Pattern pa2 = Pattern.compile( "[<][ ]*a[ ]+href=\"([^\"]+)\"[ ]*[>]([^/]+)[/].{2}" );
			Pattern pa3 = Pattern.compile( "[<][ ]*a[ ]+href=\"([^\"]+)\"[ ]*[>]([^a]+)a.{1}" );
			Pattern pa4 = Pattern.compile( "[<][ ]*a[ ]+href=\"([^\"]+)\"[ ]*[>]([^>]+)[>]" );
			
			
			
			if(doc  instanceof OfficeDocumentContents){
				
				
				int contor = 0;
				
				List<AbstractMap.SimpleEntry> listOfReplacements = new ArrayList<AbstractMap.SimpleEntry>();
				
				doc.resetFind();
				doc.findAll(pa1);
				while(doc.find()) {
					String groupFound = doc.group();
					if(groupFound.endsWith("</a>")) {
						Matcher mat = pa1.matcher(groupFound);	
						if(mat.find()){
							String link = mat.group(1);
							String name = mat.group(2);
							link = link.replaceFirst(fullUrl, ServerConfig.getAppUrl() );
							((OfficeDocumentContents)doc).replaceCurrentMatch("Gogu_are_mere_si_rachita_micsunele_" + contor);
							
							listOfReplacements.add(new AbstractMap.SimpleEntry(name, link));
							
							contor ++;
						}
					}
					
				}
				
				doc.resetFind();
				doc.findAll(pa2);
				while(doc.find()) {
					String groupFound = doc.group();
					if(groupFound.endsWith("</a>")) {
						Matcher mat = pa2.matcher(groupFound);	
						if(mat.find()){
							String link = mat.group(1);
							String name = mat.group(2).substring(0, mat.group(2).length() - 1);
							link = link.replaceFirst(fullUrl, ServerConfig.getAppUrl() );
							((OfficeDocumentContents)doc).replaceCurrentMatch("Gogu_are_mere_si_rachita_micsunele_" + contor);
							
							listOfReplacements.add(new AbstractMap.SimpleEntry(name, link));
							
							contor ++;
						}
					}
				}
				
				doc.resetFind();
				doc.findAll(pa3);
				while(doc.find()) {
					String groupFound = doc.group();
					if(groupFound.endsWith("</a>")) {
						Matcher mat = pa3.matcher(groupFound);	
						if(mat.find()){
							String link = mat.group(1);
							String name = mat.group(2).substring(0, mat.group(2).length() - 2);
							link = link.replaceFirst(fullUrl, ServerConfig.getAppUrl() );
							((OfficeDocumentContents)doc).replaceCurrentMatch("Gogu_are_mere_si_rachita_micsunele_" + contor);
							
							listOfReplacements.add(new AbstractMap.SimpleEntry(name, link));
							
							contor ++;
						}
					}
				}
				
				doc.resetFind();
				doc.findAll(pa4);
				while(doc.find()) {
					String groupFound = doc.group();
					if(groupFound.endsWith("</a>")) {
						Matcher mat = pa4.matcher(groupFound);	
						if(mat.find()){
							String link = mat.group(1);
							String name = mat.group(2).substring(0, mat.group(2).length() - 3);
							link = link.replaceFirst(fullUrl, ServerConfig.getAppUrl() );
							((OfficeDocumentContents)doc).replaceCurrentMatch("Gogu_are_mere_si_rachita_micsunele_" + contor);
							
							listOfReplacements.add(new AbstractMap.SimpleEntry(name, link));
							
							contor ++;
						}
					}
				}
				
				for (int i = listOfReplacements.size() - 1; i >= 0; i--) {
				
					AbstractMap.SimpleEntry simpleEntry = listOfReplacements.get(i);
					doc.resetFind();
					Pattern pLast = Pattern.compile( "Gogu_are_mere_si_rachita_micsunele_" + i );
					doc.findAll(pLast);
					while(doc.find()) {
						((OfficeDocumentContents)doc).replaceCurrentMatchWithProperties(simpleEntry.getKey().toString(), "HyperLinkURL", simpleEntry.getValue().toString());
					}	
				}
				
				
				
				/*
				private static Pattern PAT_LINK = Pattern.compile( "[<][ ]*a[ ]+href=\"([^\"]+)\"[ ]*[>]([^<]*)[<][/]a[>]" );
				doc.resetFind();
				doc.findAll( PAT_LINK );
				while(doc.find()){
					Matcher mat = PAT_LINK.matcher(doc.group());
					if(mat.find()){
						String link = mat.group(1);
						String name = mat.group(2);
						link = link.replaceFirst(fullUrl, ServerConfig.getAppUrl() );						
						((OfficeDocumentContents)doc).replaceCurrentMatchWithProperties(name, "HyperLinkURL", link);
					}
				}
				
				*/
				
				
				
				
			}else {
				templateContent = templateContent.replaceAll("(?ism)"+fullUrl, ServerConfig.getAppUrl());
			}
			
			
			if(finalCreation && TemplateUtils.isForEditorElement(outputFile) ){
				templateContent = templateContent.replaceAll("<!--", "").replaceAll("-->","");
			}
			
			if(isCodeBookLibrary) {
				//fix codes in blocks that were replaced in the meantime
				templateContent = ro.cst.tsearch.templates.TemplateUtils.fixAllRecursiveStaticCodes(Integer.parseInt(productId), templateContent);
				
				//moved this part here in order to fill recursive codes
				if(org.apache.commons.lang.StringUtils.isNotBlank(initialCodeToFill)) {
					//go for exact codes
					Matcher mat = Pattern.compile("(?i)(?:\\A|\\n)(" + Matcher.quoteReplacement(bpCodeToFill) + "_([0-9]+))\\s*=").matcher(templateContent);
					String fileJustForByCode = null;
					while(mat.find()) {
						String label = mat.group(1);
						// take just boilers for product id or default TemplateUtils.BOILER_MAP_DEFAULT_PRODUCT
						boolean perfectMatch = label.equalsIgnoreCase(bpCodeToFill + "_" + productId);
						if (perfectMatch
								|| (label.equalsIgnoreCase(bpCodeToFill + "_" + ro.cst.tsearch.templates.TemplateUtils.BOILER_MAP_DEFAULT_PRODUCT) 
										&& fileJustForByCode == null)) {
//							label = label.split(TemplatesServlet.LABEL_NAME_DELIM)[0];
							
							String fileToStringFragment = templateContent.substring(mat.end());
							Matcher matcherInner = AddDocsTemplates.PATTERN_BPCODE_PRODUCT.matcher(fileToStringFragment);
//							String content = matcherInner.find() ? fileToStringFragment.substring(0, matcherInner.start()) : fileToStringFragment;
							String content = null;
							if(matcherInner.find()) {
								content = fileToStringFragment.substring(0, matcherInner.start() - 1);
							} else {
								content = fileToStringFragment;
							}
							fileJustForByCode = mat.group() + content;
							
							if(perfectMatch) {
								break;
							}
							
						}
					}
					templateContent = fileJustForByCode;
					
				}
				
			}
			
			if(finalCreation){
				templateContent=templateContent.replaceAll("\\[&quot;\\]","\"");
				templateContent=templateContent.replaceAll("\\[&gt;\\]",">");
				templateContent=templateContent.replaceAll("\\[&lt;\\]","<");
			}
			
			if (!isDasl && !isUndefinedElementCompilation && !isLegal && !isBaseFile && !doNotSave){
				doc.saveToFile(outputFile);
			}
			
			return templateContent ;
		}
		finally {
			if(doc instanceof OfficeDocumentContents) {
				OfficeDocumentContents.closeOO(((OfficeDocumentContents)doc).getXComponent());
			}
		}
		
	}

	/**
	 * @param key
	 * @return
	 */
	public static boolean requiresCleaning(String key) {
		return key.equals(subdivision)
			|| key.equals(legalDesc)
			|| key.equals(ocrLD)
			|| key.equals(additionalInformation)
			|| key.equals(additionalExceptions)
			|| key.equals(additionalRequirements)
			|| key.equals(vestingInfoGrantee)
			|| key.equals(vestingInfoGrantor)
//			|| key.equals(currentOwner)
			;
	}
	

	
	private static HashMap<String,String> getInputsForMultiline(HashMap<String,Object> inputs){
		HashMap<String, String> ret = new HashMap<String, String> ();
		Set<Entry<String, Object>> set = inputs.entrySet();
		
		for(Entry<String, Object> entry:set){
			Object value = entry.getValue();
			if((value instanceof Vector)){
				//in the future we may expand the vector
			}
			else{
				if(value != null) {
					String valueString = value.toString();
					if ((value instanceof Double)) {
						DecimalFormat format = new DecimalFormat("#,##0.00");
						valueString = format.format(((Double) value).doubleValue());
					} else if ((value instanceof Float)) {
						DecimalFormat format = new DecimalFormat("#,##0.00");
						valueString = format.format(((Float) value).doubleValue());
					}
					ret.put(entry.getKey(), valueString);
				}
				else
					ret.put(entry.getKey(), "");
			}
		}
		
		return ret;
	}
	
	public static HashMap<String, Object> prepareInputs ( HashMap<String,Object> inputs )
	{
		HashMap<String,Object> m = new HashMap<String,Object>();
		
		if(inputs == null){
			return m;
		}
		
		Iterator i = inputs.keySet().iterator();
		String key = "";
		try
		{
			while ( i.hasNext() )
			{
				key = (String) i.next();
				Object o = inputs.get(key);
				if ( o != null )
				{
					//	il punem neschimbat in hashul rezultat
					m.put(key, o);
				}
				else
				{
					if ( ((Class) types.get(key)).equals(String.class) )
						m.put(key, "");
					else if ( ((Class) types.get(key)).equals(Vector.class) )
						m.put(key, new Vector());
				}
			}
		} catch (Exception e) {
			logger.error(e);
			//	daca se arunca vreo exceptie, se va returna hashul initial
			return inputs;
		}
		return m;
		
	}
	
	public static String cleanStringForAIM(String str,boolean andTest){
		
		HashMap<String,String> map=AddDocsTemplates.aimEscapeCharacters;
		if(andTest) {
			//str=str.replaceAll("&","&amp;");
		}
		Iterator it=map.keySet().iterator();
		if(str!=null){
			while(it.hasNext()){
				String strnext=(String)it.next();
				str=str.replaceAll(strnext,map.get(strnext));
			}
		}
		return str;
	}
	
	
	public static String corectStringForAIM(String str){
		str=str.replaceAll("" + ((char) 186) , "&#176;").replaceAll( "" + ((char) 176) , "&#176;" ); 
		return replaceAmpForAIM(str);
	}
	
	public static String replaceAmpForAIM(String input){
		StringBuffer ret = new StringBuffer(input);
		
		String aux ="";
		int i=-1,len=0;
		while(true){
			i++;
			i = ret.indexOf("&",i);
			if(i==-1)
				break;
			len = ret.length();
			if(i+1+3 > len){
				ret.insert(i+1,"amp;");
				continue;
			}
			aux = ret.substring(i+1, i+1+3);
			if(aux.equalsIgnoreCase("lt;") || aux.equalsIgnoreCase("gt;")){
				continue;
			}
			if(i+1+4 > len){
				ret.insert(i+1,"amp;");
				continue;
			}
			aux = ret.substring(i+1, i+1+4);
			if(aux.equalsIgnoreCase("amp;"))
				continue;
			if(i+1+5 > len){
				ret.insert(i+1,"amp;");
				continue;
			}
			aux = ret.substring(i+1, i+1+5);
			if(aux.equalsIgnoreCase("quot;") || aux.equalsIgnoreCase("apos;") || aux.equalsIgnoreCase("#176;"))
				continue;
			//daca s-a ajuns aici nu s-a facut match
			ret.insert(i+1,"amp;");
		}
		String str="";
		str=ret.toString();
		return  str;
	}

	private static Object resolveMultiLineTagContent(
			String tagName,
			String itemForm,
			Search global,
			List/*<InstrumentStructForUndefined>*/ instrumentListForUndefined, 
			Map<String,String> bolilerPlatesTSR, 
			Class<?> templateAttachedClass,
			TemplateType templateType,
			HashMap<String, Object> level1Inputs, 
			boolean getBoilerMap, 
			String cbCodeToFill, 
			String documentIdToFill){
		Class  tipClass = types.get(tagName);
		Object o = null;
		try{
			if(tipClass !=null){
				o=tipClass.newInstance();
			}
		}
		catch(Exception e){
			e.printStackTrace(System.err);
		}
		if( o!=null ){
			
			if(AddDocsTemplates.documents.equals(tagName)){
				o = getDocuments(itemForm,global,instrumentListForUndefined,bolilerPlatesTSR, templateAttachedClass, templateType, level1Inputs, getBoilerMap, documentIdToFill, cbCodeToFill);
			}
			else if(AddDocsTemplates.parcels.equals(tagName)){
				o = getParcels(itemForm,global);
			}
			else if(AddDocsTemplates.amountsToRedeemFor.equals(tagName)){
				o = getAmountsToRedeemFor(itemForm,global);
			}
			else if(AddDocsTemplates.currentOwners.equals(tagName)){
				o = getCurrentOwners(itemForm,global);
			}
			else if(AddDocsTemplates.currentBuyers.equals(tagName)){
				o = getCurrentBuyers(itemForm,global);
			}else if(AddDocsTemplates.ssfStatements.equals(tagName)){
				o = getSSFStatements(itemForm,global);
			} else if(AddDocsTemplates.dataSources.equals(tagName)){
				o = getCurrentDataSources(itemForm, global);
			} else if(AddDocsTemplates.abstractors.equals(tagName)){
				o = getAbstractors(itemForm, global);
			}
			
		}
		return o;
	}
	
	private static Vector<String> getSSFStatements(String itemForm, Search global) {
		Vector<String> ret = new Vector<String>();
		String separator = "\n";
		String separatorPattern = "\\%\\$SEPARATOR=\\((.*)\\)\\$\\%";
		if (global == null) {
			return ret;
		}

		if (true) {
			String[] statementTypes = null;
			Map<String, LinkedHashSet<String>> statementsFromSSF = global.getSa().getStatementsFromSSF();
			String[] keySet = statementsFromSSF.keySet().toArray(new String[] {});
			
			if (!StringUtils.isEmpty(itemForm)){
				
				Pattern compile = Pattern.compile(separatorPattern);
				Matcher matcher = compile.matcher(itemForm);
				
				if (matcher.find()){
					separator = matcher.group(1);
					separator = separator.replace("\\n","\n");
					itemForm = itemForm.replaceAll( separatorPattern ,"");
				}
				
				itemForm = itemForm.replaceAll("\\(\\||\\|\\)", "");
				statementTypes = itemForm.split("\\|");
				keySet = statementTypes;
			}
			
			for (String statementsType : keySet) {
				HashSet<String> hashSet = statementsFromSSF.get(statementsType);
				if (hashSet != null) {
					
					for (String statement : hashSet) {
						// the statements come formatted with <br> inside them
						statement = statement.replaceAll("<br>",separator);
						statement = statement.replaceAll("(?i)<a href='([^']+)'>(.*?)</a>", "<a href=\"$1\">$2</a>");
						
						ret.add(statement);
						ret.add(separator);
					}
					ret.add(separator);
				}
			}
		}
		return ret;
	}
	

	private static Vector<String> getParcels(String itemForm,Search global){
		Vector<String> ret = new Vector<String>();
		String temp="";
		if(global==null){
			return ret;
		}
		Vector<String> parcels = getParcels(global);
		if(parcels == null ) {
    		return ret;
    	}
		for(int i=0;i<parcels.size();i++){
			MultilineElementsMap map = new MultilineElementsMap(MultilineElementsMap.ELEMENT_TYPE,AddDocsTemplates.parcels);
			String PARCEL_NO = parcels.get(i);
			map.put(MultilineElementsMap.PARCEL_NO, PARCEL_NO);
			if(itemForm!=null){
				temp=ro.cst.tsearch.templates.TemplateUtils.replaceSpecialElements(map, null, itemForm, true);
				temp=temp.replaceAll("\\n", " ").replaceAll("\\r", " ");
				temp=temp.replaceAll("\\\\r", "\r").replaceAll("\\\\n", "\n");
				ret.add(temp);
			}
		}
		return ret;
	}
	
	private static String getAmountsToRedeemFor(String itemForm,Search global){
		StringBuilder ret= new StringBuilder();
		if(global == null){
			return "";
		}
		long searchId = global.getID();
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
		State currentState = currentInstance.getCurrentState();
		County currentCounty = currentInstance.getCurrentCounty();
		TaxUtilsNew countyTax = new TaxUtilsNew( searchId, DType.TAX, currentState.getStateAbv() , currentCounty.getName(), null);        
	    
		int redemtionSize = countyTax.getRedemtionsSize();
		for( int i=0; i<redemtionSize; i++ ){
			MultilineElementsMap map = new MultilineElementsMap(MultilineElementsMap.ELEMENT_TYPE,AddDocsTemplates.amountsToRedeemFor);
			TaxRedemptionI redem = countyTax.getRedemtion(i);
			
			String MONTH  = redem.getMonth() ;
			String YEAR   = redem.getYear()+"" ;
			String AMOUNT = redem.getAmount()+"" ;
			
			MONTH = ((StringUtils.isEmpty(MONTH))?"":MONTH);
			AMOUNT = ((StringUtils.isEmpty(AMOUNT))?"":AMOUNT);
			YEAR = ((StringUtils.isEmpty(YEAR))?"":YEAR);
			
			map.put(MultilineElementsMap.MONTH, MONTH);
			map.put(MultilineElementsMap.YEAR, YEAR);
			map.put(MultilineElementsMap.AMOUNT, AMOUNT);
			
			if(itemForm!=null){
				String temp=ro.cst.tsearch.templates.TemplateUtils.replaceSpecialElements(map, null, itemForm, true);
				temp=temp.replaceAll("\\r\\n", "\n");
				temp=temp.replaceAll("\\\\r", "\r").replaceAll("\\\\n", "\n");
				ret.append(temp);
			}
		}
		
		return ret.toString();
	}
	
	private static Vector<String> getParcels(Search global){
		Vector<String>  ret = new Vector<String>();
		
		SearchAttributes sa = global.getSa();
		if(sa==null){
			return ret;
		}
		String parcelID = sa.getAtribute(SearchAttributes.LD_PARCELNO);
		if(!StringUtils.isEmpty(parcelID)){
			String[] split = parcelID.split("\\s*,\\s*");
			for (String s: split) {
				if (!"".equals(s)) {
					boolean found = false;
					for (String each: ret) {
						if (each.replaceAll("[-.\\s]", "").equals(s.replaceAll("[-.\\s]", ""))) {
							found = true;
							break;
						}
					}
					if (!found) {
						ret.add(s);
					}
				}
			}
		}
		if (ret.size()==0) {
			ret.add("");
		}
		return ret;
	}
	
	private static Vector<String> getCurrentOwners(String itemForm, Search global) {
		String currentowners2 = AddDocsTemplates.currentOwners;
		
		Set<NameI> names = global.getSa().getOwners().getNames();
		return getNames(itemForm, global, currentowners2, names);
	}
	
	private static Vector<String> getCurrentBuyers(String itemForm,Search global){
		String currentbuyers2 = AddDocsTemplates.currentBuyers;
		Set<NameI> names = global.getSa().getBuyers().getNames();
		return getNames(itemForm, global, currentbuyers2, names);
	}

	private static Vector<String> getNames(String itemForm, Search global, String currentowners2, Set<NameI> names) {
		Vector<String> ret = new Vector<String>();
		
		if (global == null) {
			return ret;
		}

		NameFormaterI nf = MyAtsAttributes.getNameFormatterForSearch(global.getID());

		String separatorRegEx = $SEPARATOR_REG_EX;
		String separator = "";

		if (RegExUtils.matches(separatorRegEx, itemForm)) {
			String match = RegExUtils.getFirstMatch(separatorRegEx, itemForm, 1);
			separator = match;
		}
		
		boolean justBusinessName = false;
		if (itemForm.contains(MultilineElementsMap.FIRST_BUSINESS_NAME) || itemForm.contains(MultilineElementsMap.BUSINESS_NAME)){
			//look just for companies
			justBusinessName = true;
			Set<NameI> companies = new LinkedHashSet<NameI>();
			
			for(NameI name : names){
				if(name.isCompany())
					companies.add(name);
			}
			
			names = companies;
		}
		
		int i = 0;
		for (NameI name : names) {
			if(!name.isValidated()) {
				continue;
			}
			NameI element = nf.setCaseForName(name);

			String first = element.getFirstName();
			String midle = element.getMiddleName();
			String midle_initial = element.getMiddleInitial();
			String last = element.getLastName();
			String suffix = element.getSufix();
			MultilineElementsMap map = new MultilineElementsMap(MultilineElementsMap.ELEMENT_TYPE, currentowners2);

			map.put(MultilineElementsMap.FIRST, first);
			map.put(MultilineElementsMap.MIDDLE, midle);
			map.put(MultilineElementsMap.MIDDLE_INITIAL, midle_initial);
			map.put(MultilineElementsMap.LAST, last);
			map.put(MultilineElementsMap.SUFFIX, suffix);
			map.put(MultilineElementsMap.COUNTER, String.valueOf((i+1)));
			map.put(MultilineElementsMap.GUID, org.apache.commons.lang.StringUtils.defaultString(element.getGuid()));
			map.put(MultilineElementsMap.PARTY_TYPE, PartyType.getPartyTypeString(element.isCompany()));

			boolean breakNow = false;
			
			if (itemForm != null) {
				if(element.isCompany()){
					if(justBusinessName){
						map.put(MultilineElementsMap.FIRST, "");
						map.put(MultilineElementsMap.MIDDLE, "");
						map.put(MultilineElementsMap.MIDDLE_INITIAL, "");
						map.put(MultilineElementsMap.LAST, "");
					}
					map.put(MultilineElementsMap.BUSINESS_NAME, element.getFullName());
					
					if(itemForm.contains(MultilineElementsMap.FIRST_BUSINESS_NAME)){
						map.put(MultilineElementsMap.FIRST_BUSINESS_NAME, element.getFullName());
						breakNow = true;
					}
				} 
				
				String tagValue = replaceTags(itemForm, map).replaceAll("\r", "\n").replaceAll("\n", "").replaceAll("\\\\r", "\r").replaceAll("\\\\n", "\n");
				tagValue = tagValue.replaceAll(separatorRegEx, "");
				if (StringUtils.isEmpty(tagValue.trim())) {
					tagValue = "";
				} else if (i < names.size() - 1) {
					tagValue += " " + separator;
				}
				i++;
				ret.add(tagValue);
			}
			
			if(breakNow)
				break;
		}
		return ret;
	}
	
	/**
	 *  Regular expression that matches the document tag contents.
	 * 
	 * Example of expression matched:
	 * <#documents= %$TSR_INDEX_ORDER$% 
	 * (|CAT1|CAT3|)(|SUBCAT1|SUBCAT3|) string for cat1 %$TAG$%  adadada $%asdsda%$   
	 * (|CAT2|)(|SUBCAT2|) string for cat2 %$APPOINTMENTS= Text for appointments [$...$] ... $% 
	 * (|CAT3|) string for cat3 ....  
	 * #/>
	 *
	 */
	private static final String DOCUMENTS_CONTENTS_REGEX = 	"(?i)(?s)(<#\\s*documents[0-9]*\\s*=\\s*)?(\\(\\|.+?\\|\\)\\s*(\\(\\|.*?\\|\\))?(.+?))(?=((\\(\\|.+?\\|\\))|(\\z)|(/#>)))";
	private static final Pattern DOCUMENTS_CONTENTS_PATTERN = Pattern.compile(DOCUMENTS_CONTENTS_REGEX);
	
	private static final String STATEMENTS_CONTENTS_REGEX = "(?s)(<#\\s*ssfStatements[0-9]*\\s*=?\\s*)(\\(\\|.+?\\|\\)\\s*(\\(\\|.*?\\|\\))?(.+?))?(?=((/#>)?))";
	private static final Pattern STATEMENTS_CONTENTS_PATTERN = Pattern.compile(STATEMENTS_CONTENTS_REGEX);

	/** Regular expresion that finds: (|category|)(|subcategory|) .... */
	private static final String CATEGORY_CONTENTS_REGEX = "(?s)(\\(\\|([^)]*)\\|\\))(\\(\\|([^)]*)\\|\\))?(\\[\\|([^\\]]*)\\|\\])?((.)*)";
	private static final Pattern CATEGORY_CONTENTS_PATTERN = Pattern.compile(CATEGORY_CONTENTS_REGEX);
	
	/** Regular expression that matches  %$ SUBELEMENT(SUBCATEGORY) =  ........ $% */
	private static String DOCUMENTS_SUBELEMENT_REGEX = "(?s)%\\$\\s*(SUBELEMENTS)\\s*(\\(([^)]*)\\))?\\s*=((.)*?)\\$%";
	static {
		String subelements = "";
		for(String sub : MultilineElementsMap.allSpecialSubElements.keySet()) {
			subelements += "|" + sub + "";
		}
		subelements = subelements.replaceFirst("\\|", "");
		DOCUMENTS_SUBELEMENT_REGEX = DOCUMENTS_SUBELEMENT_REGEX.replaceAll("SUBELEMENTS", subelements);
	}
	private static final Pattern DOCUMENTS_SUBELEMENT_PATTERN = Pattern.compile(DOCUMENTS_SUBELEMENT_REGEX);
	
	/** Regular expression that matches the [$ ... $] tags*/
	private static String SUBELEMENT_INNER_TAG_REGEX = "\\[\\$(.+?)\\$\\]";
	private static Pattern SUBELEMENT_INNER_TAG_PATTERN = Pattern.compile(SUBELEMENT_INNER_TAG_REGEX);
	
	/** Regular expression that matches the %$ ... $% tags*/
	private static String ELEMENT_INNER_TAG_REGEX = "%\\$(.+?)\\$%";
	private static Pattern ELEMENT_INNER_TAG_PATTERN = Pattern.compile(ELEMENT_INNER_TAG_REGEX);

	private static String DOCUMENT_ORDER_REGEX = "%\\$("+MultilineElementsMap.DATE_ORDER_ASC
											      + "|"+MultilineElementsMap.DATE_ORDER_DESC
											      + "|"+MultilineElementsMap.TSR_INDEX_ORDER
											      + "|"+MultilineElementsMap.LAST_ORDER
											      + "|"+MultilineElementsMap.FIRST_ORDER
											      + "|"+MultilineElementsMap.LAST_OWNER_VALID
											      +")\\$%"; 
	private static Pattern DOCUMENT_ORDER_PATTERN = Pattern.compile(DOCUMENT_ORDER_REGEX);
	
	private static String DISABLE_BOILER_REGEX = "%\\$("+MultilineElementsMap.DISABLE_BOILER_FEATURE+")\\$%"; 
	
	private static Pattern DISABLE_BOILER_PATTERN = Pattern.compile(DISABLE_BOILER_REGEX);
	
	private static String MONTHS_BACK = "%\\$("+MultilineElementsMap.MONTHS_BACK +")_"+"([1-9][0-9]+)"+"\\$%"; 
	private static Pattern MONTHS_BACK_PATTERN = Pattern.compile(MONTHS_BACK);

	
	
	/**
	 * Parses a <#documents= ... > element and completes the types/contents Map/sortBy and other fields
	 * @param item
	 * @param fillCodeBookSet to fill or not the code book codes
	 * @param boilerPlatesTSR 
	 * @param overrideSubcategory 
	 * @param overrideCategory 
	 * @return
	 */
	private static ParseDocumentsTagResponse parseDocumentsElement(String item, boolean fillCodeBookSet, Map<String,String> boilerPlatesTSR, String overrideCategory, String overrideSubcategory) {
		
		ParseDocumentsTagResponse response = new ParseDocumentsTagResponse();
		
		if (boilerPlatesTSR == null) {
			boilerPlatesTSR = new HashMap<String, String>();
		}
		
		try {
			
			Matcher documentMatcher = DOCUMENTS_CONTENTS_PATTERN.matcher(item);
			boolean oneEmpty = Pattern.matches("\\(\\s*\\|[^)]*\\|\\s*\\)\\s*(\\(\\s*\\|[^)]*\\|\\s*\\))?\\s*", item);
			while(documentMatcher.find()||oneEmpty) {
				try {
					Matcher categoryMatcher = CATEGORY_CONTENTS_PATTERN.matcher(oneEmpty?item:documentMatcher.group());
						
					if(categoryMatcher.find()) {
						String[] doctypes,subtypes,bpCodes;
						if(org.apache.commons.lang.StringUtils.isNotBlank(overrideCategory)) {
							doctypes = new String[] {overrideCategory};
							if(org.apache.commons.lang.StringUtils.isNotBlank(overrideSubcategory)) {
								subtypes = new String[] {overrideSubcategory};
							} else {
								subtypes = new String[] {};
							}
						} else {
							doctypes = categoryMatcher.group(2).split("\\|");
						
							try {
								subtypes =  categoryMatcher.group(4).split("\\|");
							}catch(Exception ignored) { subtypes = new String[] {}; } /* The subtypes aren't mandatory */
						}
						
						if(fillCodeBookSet) {	
							try{
								bpCodes = categoryMatcher.group(6).split("\\|");
							}catch (Exception ignored){
								bpCodes = new String[] {};
							}
							
							response.getCodeBookCodes().addAll(Arrays.asList(bpCodes));
						}
						
						String innerContents = categoryMatcher.group(7);
														
						for(String doctype : doctypes) {
							
							/* Set the doctypes desired */
							if(subtypes.length == 0) {
								response.getTypes().put(doctype, new ArrayList<String>() );
							}
							if(response.getTypes().containsKey(doctype)) {
								if(!response.getTypes().get(doctype).isEmpty()) {
									response.getTypes().get(doctype).addAll(Arrays.asList(subtypes));
								}
							}
							else {
								Collection<String> subtypesCollection = new ArrayList<String>();
								subtypesCollection.addAll(Arrays.asList(subtypes));
								response.getTypes().put(doctype, subtypesCollection );
							}
							
							List<String> l = new ArrayList<String>();
							l.add(doctype);

							/* Set the contents for each doctype/subtype */
							if (subtypes.length == 0) {
								response.getContents().put(doctype, innerContents);
							} else {
								l.addAll(Arrays.asList(subtypes));
							}

							response.getContentsDocTypesAndSubTypes().add(l);

							for (String subtype : subtypes) {
								response.getContents().put(doctype + "_" + subtype.toUpperCase(), innerContents);
							}
						}
					}
				}
				
				catch (StackOverflowError e) {
						System.err.println("StackOverflowError " + e.getMessage());
						logger.error("StackOverflowError: " + e.getMessage());
				}catch(Exception ignored) {} /* We couldn't parse a category. Move on to the next one */
				finally{
					if(oneEmpty){
						break;
					}
				}
			}
			
			if(response.getTypes().isEmpty())  {
				response.setParseError(true);
			}
			
			Matcher orderMatcher = DOCUMENT_ORDER_PATTERN.matcher(item);
			if(orderMatcher.find()) {
				response.setSortBy(orderMatcher.group(1));
			}
			
			Matcher disableBoilerMatcher = DISABLE_BOILER_PATTERN.matcher(item);
			if(disableBoilerMatcher.find()) {
				response.setDisableBoilerPlatesText(disableBoilerMatcher.group(1));
			}
			
			Matcher monthsBackMatcher = MONTHS_BACK_PATTERN.matcher(item);
			if(monthsBackMatcher.find()){
				response.setMonthsBack(Integer.parseInt(monthsBackMatcher.group(2)));
			}
			
			response.setParsedItem(item.replaceAll(DOCUMENT_ORDER_REGEX, "").replaceAll(DISABLE_BOILER_REGEX, ""));
			
			/* Also add the contents for the documents which have a boilerplate set in the TSR index*/
			for(Entry<String,String> e : boilerPlatesTSR.entrySet()) {
				
				String boilerText = e.getValue();
				ParseDocumentsTagResponse parsePossible = parseDocumentsElement(boilerText, false , null, null, null);
				if(!parsePossible.isParseError()) {
					if(parsePossible.getContents().containsKey(MultilineElementsMap.UNDEFINED)) { 
						boilerText =  parsePossible.getContents().get(MultilineElementsMap.UNDEFINED);
					}
				}
				response.getContentsDocTypesAndSubTypes().addAll(parsePossible.getContentsDocTypesAndSubTypes());
				response.getContents().put(e.getKey(), boilerText);
			}
		} catch (StackOverflowError e) {
			response.setParseError(true); 
			System.err.println("StackOverflowError " + e.getMessage());
			logger.error("StackOverflowError: " + e.getMessage());
		} catch(Exception e) { 
			response.setParseError(true);
		}
		
		return response;
	}
	
	public static String createDocimageLink(String link, String name){
		return DOC_IMAGE_LINK.replace("!", name).replace("@", link);
	}
	
	public static final String	secondLevelBlockPrefix	= "==_==";
	public static final String	thirdLevelBlockPrefix	= "===_===";
	public static final String	secondLevelBlockRegex	= "(?ism)" + secondLevelBlockPrefix + "(" +functionBlockSecondLevelBaseName + "\\d+"
																+ ")" + secondLevelBlockPrefix;
	public static final Pattern	secondLevelBlockPattern	= Pattern.compile(secondLevelBlockRegex);
	public static final String	thirdLevelBlockRegex	= "(?ism)" + thirdLevelBlockPrefix + "(" +functionBlockThirdLevelBaseName + "\\d+"
																+ ")" + thirdLevelBlockPrefix;
	public static final Pattern	thirdLevelBlockPattern	= Pattern.compile(thirdLevelBlockRegex);

	/**
	 * Resolve a <#document =  ... > element.
	 * @param item (|TRANSFER|)%$GRANTOR$%, having no record interest in said by .....
	 * @param search
	 * @param instrumentList
	 * @param templateAttachedClass class attached to the template for secondLevel conditions
	 * @param level1Inputs used with the attached class
	 * @param getBoilerMap - if true return a String vector with docId preceding every doc statement
	 * @param singleBPRequested 
	 * @return
	 */
	public static Vector<String> getDocuments( String item,
												Search search, 
												Collection<InstrumentStructForUndefined> instrumentList,
												Map<String,String> bolilerPlatesTSR, 
												Class<?> templateAttachedClass, 
												TemplateType templateType,
												HashMap<String, Object> level1Inputs, 
												boolean getBoilerMap, 
												String singleCBDocumentId,
												String cbBodeToFill) {
		
		Vector<String> returnValue = new Vector<String>();
		
		/* because keys from contents hash are made by taking all combinations of docType and subType
		e.g. for (|LIEN|)(|FLN|Federal Tax Lien|) we'll have 2 keys: LIEN_FLN and LIEN_FEDERAL TAX LIEN
		
		this structure keeps the doctype and subtypes in a List<List<String>> (Matrix) as follows
		
		docType1 docSubType11 docSubType12 ...
		docType2 docSubType21 docSubType22 ...
		...
		
		*/
		
		if(getBoilerMap) {
			
			if(org.apache.commons.lang.StringUtils.isNotBlank(singleCBDocumentId)) {
				DocumentsManagerI docManager = search.getDocManager();
				try {
					docManager.getAccess();
					DocumentI document = docManager.getDocument(singleCBDocumentId);
					if(document != null 
							&& DocumentTypes.OTHERFILES.equals(document.getDocType())
							&& DocumentTypes.OTHER_FILE_SPECIAL_SUBCATEGORIES.contains(document.getDocSubType()) ) {
						getBoilerMap = false;
					}
				} catch (Exception e) {
					// TODO: handle exception
				} finally {
					docManager.releaseAccess();
				}
			}
			
			//do the original code
			/* Parse the <#documents= ... (|category|)(|subcategory|) ... /#> element */
			ParseDocumentsTagResponse parseDocumentsTagResponse = parseDocumentsElement(item, true, bolilerPlatesTSR, null, null);
			
			
			FilledDocumentsInternalResponse documentsInternal = getDocumentsInternal(
					search, 
					instrumentList, 
					templateAttachedClass, 
					templateType,
					level1Inputs, 
					getBoilerMap, 
					null,		//first let's not force any document to allow all correct documents to be iterated upon 
					null,
					parseDocumentsTagResponse);
			returnValue.addAll(documentsInternal.getFilledData());
			
			//let's to it the other way ;)
			
			if(getBoilerMap && org.apache.commons.lang.StringUtils.isNotBlank(cbBodeToFill)) {
			
				List<String> docIds = new ArrayList<String>(); 
				
				//first let's get all documents that have this code and were not designed to
				DocumentsManagerI docManager = search.getDocManager();
				try {
					docManager.getAccess();
					
					List<DocumentI> documentsList = docManager.getDocumentsList(true);
					for (DocumentI documentI : documentsList) {
						if(documentI.hasCodeBookCode(cbBodeToFill, true) 
								&& !documentsInternal.getFilledDocumentsIds().contains(documentI.getId())) {
							docIds.add(documentI.getId());
						}
					}
					
				} catch (Exception e) {
					// TODO: handle exception
				} finally {
					docManager.releaseAccess();
				}
				
				for (String docId : docIds) {
	
					String overrideCategory = null;
					String overrideSubcategory = null;
					if(org.apache.commons.lang.StringUtils.isNotBlank(docId)) {
						try {
							docManager.getAccess();
							DocumentI document = docManager.getDocument(docId);
							if(document != null) {
								overrideCategory = document.getDocType();
								overrideSubcategory = document.getDocSubType();
							}
						} catch (Exception e) {
							// TODO: handle exception
						} finally {
							docManager.releaseAccess();
						}
					}
					
					parseDocumentsTagResponse = parseDocumentsElement(item, true, bolilerPlatesTSR, overrideCategory, overrideSubcategory);
					
					documentsInternal = getDocumentsInternal(
							search, 
							instrumentList, 
							templateAttachedClass, 
							templateType,
							level1Inputs, 
							getBoilerMap, 
							docId, 
							cbBodeToFill,
							parseDocumentsTagResponse);
					returnValue.addAll(documentsInternal.getFilledData());
					
				}
			
			}
			
		} else {
		
		
			String overrideCategory = null;
			String overrideSubcategory = null;
			if(org.apache.commons.lang.StringUtils.isNotBlank(cbBodeToFill)) {
				if(org.apache.commons.lang.StringUtils.isNotBlank(singleCBDocumentId)) {
					DocumentsManagerI docManager = search.getDocManager();
					try {
						docManager.getAccess();
						DocumentI document = docManager.getDocument(singleCBDocumentId);
						if(document != null) {
							overrideCategory = document.getDocType();
							overrideSubcategory = document.getDocSubType();
						}
					} catch (Exception e) {
						// TODO: handle exception
					} finally {
						docManager.releaseAccess();
					}
				}
			} else {
				singleCBDocumentId = null;
				
				
			}
			
			/* Parse the <#documents= ... (|category|)(|subcategory|) ... /#> element */
			ParseDocumentsTagResponse parseDocumentsTagResponse = parseDocumentsElement(item, true, bolilerPlatesTSR, overrideCategory, overrideSubcategory);
			
			FilledDocumentsInternalResponse documentsInternal = getDocumentsInternal(
					search, 
					instrumentList, 
					templateAttachedClass,
					templateType,
					level1Inputs, 
					getBoilerMap, 
					singleCBDocumentId, 
					cbBodeToFill,
					parseDocumentsTagResponse);
			returnValue.addAll(documentsInternal.getFilledData());
			
		}
		
		return returnValue;
	}

	private static FilledDocumentsInternalResponse getDocumentsInternal(Search search, Collection<InstrumentStructForUndefined> instrumentList, 
			Class<?> templateAttachedClass,
			TemplateType templateType,
			HashMap<String, Object> level1Inputs, boolean getBoilerMap, String singleCBDocumentId, String cbBodeToFill, ParseDocumentsTagResponse parseDocumentsTagResponse) {
		
		FilledDocumentsInternalResponse internalResponse = new FilledDocumentsInternalResponse();
		
		if(parseDocumentsTagResponse.isParseError()) {
			return internalResponse;
		}
		
		boolean singleBPRequested = org.apache.commons.lang.StringUtils.isNotBlank(cbBodeToFill);
		
		Boolean resolveUndefined = (instrumentList != null && !instrumentList.isEmpty());
		
		/* Table with the categories and subcategories used in the documents element */
		Map<String,Collection<String>> types = parseDocumentsTagResponse.getTypes();
		Map<String, String> contents = parseDocumentsTagResponse.getContents();
		Set<String> codeBookCodes = parseDocumentsTagResponse.getCodeBookCodes();
		boolean disableBoilerFeature = MultilineElementsMap.DISABLE_BOILER_FEATURE.equals(parseDocumentsTagResponse.getDisableBoilerPlatesText());
		
		if(types.containsKey(MultilineElementsMap.UNDEFINED) && !resolveUndefined) {
			/* We  have an undefined element, but don't have the instrument list yet; return the original item */
			internalResponse.getFilledData().addAll(Arrays.asList(new String[] { parseDocumentsTagResponse.getParsedItem() } ));
			return internalResponse;
		}

		/* Get the documents */
		
		
						
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(search.getID());
		int currentState = currentInstance.getCurrentState().getStateId().intValue();
		int currentCountyId = currentInstance.getCurrentCounty().getCountyId().intValue();
		
		java.text.SimpleDateFormat	 sdf = TemplateBuilder.getSimpleDateFormatForTemplates(currentState);
		DocumentsManagerI docManager = search.getDocManager();
		try{
			docManager.getAccess();
			Collection<DocumentI> documents = new ArrayList<DocumentI>();
				
			/* Get the document list */
			
			List<InstrumentI> instruments = new ArrayList<InstrumentI>();
			
			if(resolveUndefined) {
				/* If we complete an undefined tag, we need to get only the documents with the specified instruments */
								
				for(InstrumentStructForUndefined i : instrumentList) {
					/* create some instruments */
					InstrumentI instr = new Instrument();
					
					/* set their data */
					instr.setInstno(i.instNo);
					instr.setBook(i.book);
					instr.setPage(i.page);
					instr.setDocno(i.docNo);
					
					instruments.add(instr);
				}
				
				documents = docManager.getDocumentsWithInstrumentsFlexible(false,instruments.toArray(new InstrumentI[instruments.size()]));
			}
			else {  
				DocumentI document = null;
				if(org.apache.commons.lang.StringUtils.isNotBlank(singleCBDocumentId)) {
					document = docManager.getDocument(singleCBDocumentId);
				}
				if(document != null) {
					documents.add(document);
				} else {
					documents = docManager.getDocuments(!singleBPRequested,false,types);
				}
				
			}
			if(types.containsKey(MultilineElementsMap.UNRELATED)) {
				for(DocumentI unrelated : docManager.getDocumentsWithDocType(true,
										  DocumentTypes.ASSIGNMENT,DocumentTypes.APPOINTMENT,DocumentTypes.RELEASE,DocumentTypes.MODIFICATION) ) {
					if(unrelated.getReferences().isEmpty()  && !documents.contains(unrelated)) {
						documents.add(unrelated);
					}
				}
			}
			
			//keep just documents with boiler plates requested in template
			if(!codeBookCodes.isEmpty()){
				ArrayList<DocumentI> newDocs = new ArrayList<DocumentI>();
				
				for(DocumentI doc : documents){
					for(String s : codeBookCodes){
						boolean flag = false;
						if(s.endsWith("_")){
							flag = doc.hasCodeBookCode(s, false);
						} else {
							flag = doc.hasCodeBookCode(s, true);
						}
						
						if(flag){
							newDocs.add(doc);
							break;
						}
					}
				}
				
				documents.clear();
				documents.addAll(newDocs);
			}
			
			int lastMonths = parseDocumentsTagResponse.getMonthsBack();
			if(lastMonths>0){
				documents = DocumentUtils.keepRecentDocuments(documents, lastMonths);
			}
			
			/* Sort the documents if sorting tags are present, ex: %$DATE_ORDER_ASC$% */
			List<String> idList = new ArrayList<String>();
			for (SimpleChapter simpleChapter : docManager.getTsrIndexOrder()) {
				idList.add(simpleChapter.getDocumentId());
			}
					
			DocumentUtils.sortDocuments(documents, parseDocumentsTagResponse.getSortBy(), idList);
						
			if(MultilineElementsMap.LAST_OWNER_VALID.equals(parseDocumentsTagResponse.getSortBy())){
				keepTheFirstOneValidateByOwner(search, documents);
			} else if (MultilineElementsMap.FIRST_ORDER.equals(parseDocumentsTagResponse.getSortBy())
					|| MultilineElementsMap.LAST_ORDER.equals(parseDocumentsTagResponse.getSortBy())) {
					keepTheFirstOne(documents);
			}
			
			int contor = -1;
			
			internalResponse.setAppliedOverDocuments(documents.size());
			
			for(DocumentI doc : documents) {
				
				/* Determine what's the key by which we find the contents; The key was completed in parseDocumentsElement(...) */
				String key = getKey(doc,types,contents,instruments,resolveUndefined, disableBoilerFeature);
				
				if(!contents.containsKey(key)) 
					continue;
				
				/* Get the contents corresponding to this document type */
				String innerContents = contents.get(key);
								
				/* Fill in the tags */
				MultilineElementsMap map= new MultilineElementsMap(MultilineElementsMap.ELEMENT_TYPE,AddDocsTemplates.documents,""/*'*'*/);
				map.setAutoConvertEmpty(true);
				
				contor++;
				
				InstrumentI instrument = doc.getInstrument();
	 			
				map.put(MultilineElementsMap.CONTOR_TYPE_1, getContorType1(contor) );
				map.put(MultilineElementsMap.CONTOR_TYPE_2, getContorType2(contor) );

				map.put(MultilineElementsMap.DOCUMENT_TYPE, doc.getDocType());
				map.put(MultilineElementsMap.DOCUMENT_SUBTYPE, doc.getDocSubType());
	 			map.put(MultilineElementsMap.SERVER_DOCUMENT_TYPE, doc.getServerDocType());
	 			map.put(MultilineElementsMap.DOCUMENT_ID, doc.getId());
				map.put(MultilineElementsMap.GRANTEE,starToEmpty(doc.getGranteeFreeForm()));
				map.put(MultilineElementsMap.GRANTOR,starToEmpty(doc.getGrantorFreeForm()));
				map.put(MultilineElementsMap.DOCUMENT_REMARKS, doc.getRemarks(true));
				map.put(MultilineElementsMap.NOTE, doc.getNote());
				map.put(MultilineElementsMap.SSN4, doc.getAllSSN());
				map.put(MultilineElementsMap.CONTOR_TYPE_3, Integer.toString(contor+1));
				
//				if(!boilerPlateCodes.isEmpty()){
				Collection<String> collection = types.get(doc.getDocType());
				if(collection != null) {
					if(collection.isEmpty()) {
						map.put(MultilineElementsMap.BPCODE, doc.getCodeBookObjectStringValue(codeBookCodes, BPType.CATEGORY).replaceAll("\\\n", "\\\\n"));
					} else {
						map.put(MultilineElementsMap.BPCODE, doc.getCodeBookObjectStringValue(codeBookCodes, BPType.SUBCATEGORY ).replaceAll("\\\n", "\\\\n"));	
					}
						
				}
				
//				}
				
				putDateTags(map,doc,sdf,innerContents,false);
				
				
				putInstrumentTags(map,instrument, currentState, currentCountyId, search.getSa().isCondo());
				
				DecimalFormat df = new DecimalFormat( "#,###,###,##0.00" );
				if(doc.is(DocumentTypes.MORTGAGE) && (doc instanceof MortgageI)) {
					MortgageI mortgageDoc = (MortgageI) doc;
					
					String mortgageAmount = "";
					double mortgageAmountdouble = mortgageDoc.getMortgageAmount();
					if(mortgageAmountdouble!=SimpleChapterUtils.UNDEFINED_DOUBLE_VALUE){
						mortgageAmount = df.format(mortgageAmountdouble);
					}
					
					map.put( MultilineElementsMap.MORTGAGE_AMOUNT, mortgageAmount );
					map.put( MultilineElementsMap.MORTGAGE_AMOUNT_FREE_FORM, mortgageDoc.getMortgageAmountFreeForm() );
					map.put(MultilineElementsMap.LOAN_NO, mortgageDoc.getLoanNo() );
					
					map.put(MultilineElementsMap.GRANTEE_LANDER,starToEmpty(mortgageDoc.getGranteeLenderFreeForm()));
					map.put(MultilineElementsMap.GRANTEE_LENDER,starToEmpty(mortgageDoc.getGranteeLenderFreeForm()));
					
					map.put(MultilineElementsMap.GRANTEE_TRUSTEE,starToEmpty(mortgageDoc.getGranteeTrusteeFreeForm()));
					map.put(MultilineElementsMap.GRANTEE_TR,starToEmpty(mortgageDoc.getGranteeTrusteeFreeForm()));
				}
				else if(doc.is(DocumentTypes.LIEN)) {
					if(doc instanceof LienI) {
						LienI lienDoc = (LienI) doc;
						String lienAmount = "";
						double lienAmountdouble = lienDoc.getConsiderationAmount();
						if(lienAmountdouble!=SimpleChapterUtils.UNDEFINED_DOUBLE_VALUE){
							lienAmount = df.format(lienAmountdouble);
						}
						map.put(MultilineElementsMap.CONSIDERATION_AMOUNT,lienAmount) ;					
					}
				} else if(doc.is(DocumentTypes.COURT)) {
					if(doc instanceof CourtI) {
						CourtI courtDoc = (CourtI) doc;
						String courtAmount = "";
						double courtAmountdouble = courtDoc.getConsiderationAmount();
						if(courtAmountdouble != SimpleChapterUtils.UNDEFINED_DOUBLE_VALUE){
							courtAmount = df.format(courtAmountdouble);
						}
						map.put(MultilineElementsMap.CONSIDERATION_AMOUNT, courtAmount);					
					}
				} else if(doc.is(DocumentTypes.TRANSFER)){
					if(doc instanceof TransferI){
						TransferI transfertDoc = (TransferI) doc;
						String transferAmount = formatDouble(transfertDoc.getConsiderationAmount(), df);
						map.put(MultilineElementsMap.CONSIDERATION_AMOUNT, transferAmount);					
					}
				} else if (doc instanceof TaxDocumentI) {
					TaxDocumentI taxDoc = (TaxDocumentI)doc;
					
					TaxUtilsNew.Struct1 str1 = TaxUtilsNew.calculateCurrentTaxYear(taxDoc.getPayDate(), taxDoc.getDueDate(), taxDoc.getTaxYearMode(), taxDoc);
					int currentTaxYear = str1.getCurrentTaxYear();
					TaxUtilsNew.Struct2 str2 = TaxUtilsNew.calculateTotalDelinquent(-1, taxDoc.getDueDate(), taxDoc.getPayDate(), taxDoc, currentTaxYear);
					double totalDelinquent = str2.getTotalDelinquent();
					
					map.put(MultilineElementsMap.APPRAISED_VALUE_LAND, formatDouble(taxDoc.getAppraisedValueLand(), df));
					map.put(MultilineElementsMap.APPRAISED_VALUE_IMPROVEMENTS, formatDouble(taxDoc.getAppraisedValueImprovements(), df));
					map.put(MultilineElementsMap.APPRAISED_VALUE_TOTAL, formatDouble(taxDoc.getAppraisedValueTotal(), df));
					map.put(MultilineElementsMap.TOTAL_ASSESSMENT, formatDouble(taxDoc.getTotalAssessment(), df));
					map.put(MultilineElementsMap.EXEMPTION_AMOUNT, formatDouble(taxDoc.getExemptionAmount(), df));
					map.put(MultilineElementsMap.BASE_AMOUNT, formatDouble(taxDoc.getBaseAmount(), df));
					map.put(MultilineElementsMap.BASE_AMOUNT_CITY, formatDouble(taxDoc.getBaseAmountEP(), df));
					map.put(MultilineElementsMap.FOUND_DELINQUENT, formatDouble(taxDoc.getFoundDelinquent(), df));
					map.put(MultilineElementsMap.TOTAL_DELINQUENT, formatDouble(totalDelinquent, df));
					map.put(MultilineElementsMap.AMOUNT_PAID, formatDouble(taxDoc.getAmountPaid(), df));
					map.put(MultilineElementsMap.AMOUNT_DUE, formatDouble(taxDoc.getAmountDue(), df));
					map.put(MultilineElementsMap.DATE_PAID, org.apache.commons.lang.StringUtils.defaultString(taxDoc.getDatePaid()));
					map.put(MultilineElementsMap.TAX_VOLUME, formatInt(taxDoc.getTaxVolume()));
					map.put(MultilineElementsMap.SALE_DATE, org.apache.commons.lang.StringUtils.defaultString(taxDoc.getSaleDate()));
					map.put(MultilineElementsMap.SALE_NO, org.apache.commons.lang.StringUtils.defaultString(taxDoc.getSaleNo()));
					map.put(MultilineElementsMap.BILL_NUMBER, org.apache.commons.lang.StringUtils.defaultString(taxDoc.getBillNumber()));
					map.put(MultilineElementsMap.RESEARCH_REQUIRED, Boolean.toString(taxDoc.isResearchRequired()));
					map.put(MultilineElementsMap.SPLIT_PAYMENT_AMOUNT, formatDouble(taxDoc.getSplitPaymentAmount(), df));
					map.put(MultilineElementsMap.PAY_DATE, formatDate(taxDoc.getPayDate(), sdf));
					map.put(MultilineElementsMap.DUE_DATE, formatDate(taxDoc.getDueDate(), sdf));
					
					boolean hasHomesteadExemption = false;
					ArrayList<Installment> installments = taxDoc.getInstallments();
					if (installments!=null) {
						for (Installment installment: installments) {
							double d = installment.getHomesteadExemption();
							if (d>0.0d) {
								hasHomesteadExemption = true;
								break;
							}
						}
					}
					ArrayList<Installment> sa_installments = taxDoc.getSpecialAssessmentInstallments();
					map.put(MultilineElementsMap.HAS_HOMESTEAD_EXEMPTION, Boolean.toString(hasHomesteadExemption));
					map.put(MultilineElementsMap.TOTAL_INSTALL, Integer.toString(installments==null?0:installments.size()));
					map.put(MultilineElementsMap.SA_TOTAL_INSTALL, Integer.toString(sa_installments==null?0:sa_installments.size()));
					
				} else if (doc instanceof AssessorDocumentI) {
					
					AssessorDocumentI assDoc = (AssessorDocumentI)doc;
					map.put(MultilineElementsMap.TOTAL_ASSESSMENT, formatDouble(assDoc.getTotalAssessement(), df));
					map.put(MultilineElementsMap.TOTAL_ESTIMATED_TAXES, formatDouble(assDoc.getTotalEstimatedTaxes(), df));
					
					if (doc instanceof AssessorManagementDocumentI){
						AssessorManagementDocumentI assMgmtDoc = (AssessorManagementDocumentI) doc;
						
						map.put(MultilineElementsMap.DISTRICT, assMgmtDoc.getDistrict());
						map.put(MultilineElementsMap.FINAL_PAYMENT, assMgmtDoc.getFinalPayment());
						map.put(MultilineElementsMap.PREPAID_PRINCIPAL, formatDouble(assMgmtDoc.getPrepaidPrincipal(), df));
						map.put(MultilineElementsMap.CURRENT_DUE, formatDouble(assMgmtDoc.getCurrentDue(), df));
						map.put(MultilineElementsMap.TOTAL_PAYOFF, formatDouble(assMgmtDoc.getTotalPayoff(), df));
						map.put(MultilineElementsMap.DUE_DATES_AM, assMgmtDoc.getDueDatesAM());
						map.put(MultilineElementsMap.INITIAL_PRINCIPAL, formatDouble(assMgmtDoc.getInitialPrincipal(), df));
					}
					
				} else if(doc.isOneOf(DocumentTypes.PRIORFILE)){
					if(doc instanceof PriorFileDocument){
						PriorFileDocument pfDoc = (PriorFileDocument) doc;
						
						List<SelectableStatement> reqList = pfDoc.getRequirements();
						String req = "";
						if(reqList != null) {
							for(SelectableStatement stmt : reqList) {
								if(stmt.isSelected()) {
									String stmtText = stmt.getText().replaceAll("\\\n", "\\\\n");//.replace("\n", "<br>");
									stmtText = StringEscapeUtils.unescapeXml(stmtText);
									//req += stmtText + "<br><br>";
									req += stmtText + "\\n\\n";
								}
							}
						}
						map.put(MultilineElementsMap.PRIOR_REQUIREMENTS, req);
						
						List<SelectableStatement> ldList = pfDoc.getLegalDescriptions();
						String ld = "";
						if(ldList != null) {
							for(SelectableStatement stmt : ldList) {
								if(stmt.isSelected()) {
									String stmtText = stmt.getText().replaceAll("\\\n", "\\\\n"); //replace("\n", "<br>");
									stmtText = StringEscapeUtils.unescapeXml(stmtText);
									//ld += stmtText + "<br><br>";
									ld += stmtText + "\\n\\n";
								}
							}
						}
						map.put(MultilineElementsMap.PRIOR_LD, ld);
						
						List<SelectableStatement> exList = pfDoc.getExceptionsList();
						String ex = "";
						if(exList != null) {
							for(SelectableStatement stmt : exList) {
								if(stmt.isSelected()) {
									String stmtText = stmt.getText().replaceAll("\\\n", "\\\\n");//.replace("\n", "<br>");
									stmtText = StringEscapeUtils.unescapeXml(stmtText);
//									ex += stmtText + "<br><br>";
									ex += stmtText + "\\n\\n";
								}
							}
						}
						map.put(MultilineElementsMap.PRIOR_EXCEPTIONS, ex);
					}
				}
				
				applyCommunityQARules(map, doc, search);
				updateMapWithImageLinks(map,doc,search, singleBPRequested);
				
				//complete second level conditions
				Object templateAttachedObject = null;
				if(templateAttachedClass != null){	
					try{
						//get object 
						Constructor<?> cons = templateAttachedClass.getConstructor(new Class[] {HashMap.class, HashMap.class, boolean.class});
						
						//NOTE: !!! DOCUMENTS_TAGS_MAP and prepareSecondLevelValues(MultilineElementsMap.DOCUMENTS_TAGS_MAP, map) must have same size
						templateAttachedObject = cons.newInstance(new Object[] { MultilineElementsMap.DOCUMENTS_TAGS_MAP, prepareSecondLevelValues(MultilineElementsMap.DOCUMENTS_TAGS_MAP, map), true });
						
						//set level1 inputs ...
						Method setLevel1Tags = templateAttachedClass.getMethod("setLevel1Tags", new Class[]{HashMap.class, HashMap.class});
						
						if(templateAttachedObject != null){
							setLevel1Tags.invoke(templateAttachedObject, new Object[] { prepareInputs(level1Inputs), AddDocsTemplates.types });
							
							if(innerContents.contains(functionBlockSecondLevelBaseName)){
								putDateTags(map,doc,sdf,"",true);		//date tags inside IF condition
								List<String> list = RegExUtils.getMatches(secondLevelBlockPrefix + functionBlockSecondLevelBaseName + "([\\d]+)" + secondLevelBlockPrefix, innerContents, 1);
								Method setLevel2Tags = templateAttachedClass.getMethod("setLevel2Tags", new Class[]{String.class, ArrayList.class, HashMap.class});
								HashMap<String, String> newMap = new HashMap<String, String>();
								Iterator<Entry<String, String>> it = map.entrySet().iterator();
								while (it.hasNext()) {
									Map.Entry<String, String> entry = it.next();
									newMap.put(entry.getKey(), entry.getValue());
								}
								setLevel2Tags.invoke(templateAttachedObject, new Object[] { AddDocsTemplates.documents, list, newMap });
							}
						}
						
						// now we should have all the info we need
						
						// find all second level blocks
						Matcher ma = secondLevelBlockPattern.matcher(innerContents);
						while(ma.find()){
							String blockKey = ma.group(1);
														
							Method getValue = templateAttachedClass.getMethod("get" + blockKey, new Class[]{});
							if (templateAttachedObject != null){
								String dataElement = (String) getValue.invoke(templateAttachedObject, new Object[]{});
								innerContents = innerContents.replace(ma.group(), dataElement);
							}
						}
					} catch (Exception e){
						e.printStackTrace();
					}
				}
				
				if(innerContents.contains(functionBlockSecondLevelBaseName)){
					Matcher ma = secondLevelBlockPattern.matcher(innerContents);
					while(ma.find()){
						innerContents = innerContents.replaceAll(ma.group(), "");
					}
				}
				
				/* Resolve the second-level elements */
				String contentsWithoutSubelements = completeDocumentsSecondLevelElements(doc,innerContents,currentState,currentCountyId,parseDocumentsTagResponse.getSortBy(), search, singleBPRequested, 
						templateAttachedClass, templateAttachedObject);
	    		
				putDateTags(map,doc,sdf,innerContents,false);		//some date tags may have appeared after expanding the second level blocks
				
	    		/* Replace the tags in the contents using the map we just filled */
				String content = replaceTags(contentsWithoutSubelements,map);
				
				if(getBoilerMap){
					content = content.replaceAll("\\\\r", "\r")
							.replaceAll("\\\\n", "\n");
					
					internalResponse.getFilledData().add(
							ro.cst.tsearch.templates.TemplateUtils.BOILER_MAP_SEPARATOR +
							doc.getId() +
							ro.cst.tsearch.templates.TemplateUtils.BOILER_MAP_SEPARATOR_EQUAL +
							content +
							ro.cst.tsearch.templates.TemplateUtils.BOILER_MAP_SEPARATOR
								);
					internalResponse.getFilledDocumentsIds().add(doc.getId());
				} else {
					
					if(TemplateType.CBLibrary.equals(templateType)) {
						content = content.replaceAll("\\\\r", "\r")
								.replaceAll("\\\\n", "\n");
					} else {
						content = content.replaceAll("\r", "\n")
								.replaceAll("\n", "")
								.replaceAll("\\\\r", "\r")
								.replaceAll("\\\\n", "\n");	
					}
					
					internalResponse.getFilledData().add(content);
				}
				
				
				internalResponse.getFilledData().add("");		
			}
			
			//for all keys place NO doc ....
			Set<String> keySet = contents.keySet();
			List<List<String>> contentsDocTypesAndSubTypes = parseDocumentsTagResponse.getContentsDocTypesAndSubTypes();
			if (!keySet.isEmpty() && contentsDocTypesAndSubTypes.size() > 0) {
				for (List<String> l : contentsDocTypesAndSubTypes) {
					boolean found = false; // if we find at least one doc for a given doctype and subtype do not complete the defaultifnomatch tag

					String content = "";

					if (l.size() == 1) {
						// just doctype
						for (DocumentI doc : documents) {
							if (l.get(0).equalsIgnoreCase(doc.getDocType())) {
								// we have at least one doc
								found = true;
								break;
							}
						}
						content = contents.get(l.get(0).toUpperCase());
					} else if (l.size() > 1) {
						// doctype and docsubtypes
						for (DocumentI doc : documents) {
							for (int i = 1; i < l.size(); i++) {
								if (l.get(0).equalsIgnoreCase(doc.getDocType()) && l.get(i).equalsIgnoreCase(doc.getDocSubType())) {
									// we have at least one doc
									found = true;
									break;
								}
							}
							if (found)
								break;
						}
						content = contents.get((l.get(0)+ "_" + l.get(1)).toUpperCase());
					}

					if (found || content == null || !content.contains(MultilineElementsMap.DEFAULTIFNOMATCH) || l.size() == 0)
						continue;

					// no doc found for this key, replace the key with it's default value
					String contentsWithoutSubelements = completeDocumentsSecondLevelElements(null, content, currentState, currentCountyId, parseDocumentsTagResponse.getSortBy(), search, singleBPRequested, 
							templateAttachedClass, null);

					MultilineElementsMap map = new MultilineElementsMap(MultilineElementsMap.ELEMENT_TYPE, AddDocsTemplates.documents, ""/* '*' */);
					map.setAutoConvertEmpty(true);

					String retVal = replaceTags(contentsWithoutSubelements, map)
							.replaceAll("\r", "\n")
							.replaceAll("\n", "")
							.replaceAll("\\\\r", "\r")
							.replaceAll("\\\\n", "\n");

					internalResponse.getFilledData().add(retVal);
					internalResponse.getFilledData().add("");
				}
			}
			
		}
		catch(Exception e) {
			e.printStackTrace();
			logger.error("Error while processing getDocuments with original tag [" + parseDocumentsTagResponse.getParsedItem() + "]", e);
		}
		finally {
			docManager.releaseAccess();
		}
		
		return internalResponse;
	}

	private static Object prepareSecondLevelValues(HashMap<String, Class<?>> types, HashMap<String, String> values) {
		try {
			HashMap<String, Object> newMap = new HashMap<String, Object>();
			//convert values to their class
			for (Entry<String, Class<?>> e : types.entrySet()) {
				try {
					String value = values.get(e.getKey());
					
					if(org.apache.commons.lang.StringUtils.isBlank(value) || "*".equals(value)){
						if (e.getValue().equals(String.class)) {
							newMap.put(e.getKey(), "");
						} else if (e.getValue().equals(int.class)) {
							newMap.put(e.getKey(), Integer.MIN_VALUE);
						} else if (e.getValue().equals(double.class)) {
							newMap.put(e.getKey(), Double.MIN_VALUE);
						} else if (e.getValue().equals(Date.class)) {
							newMap.put(e.getKey(), null);
						} else if (e.getValue().equals(boolean.class)) {
							newMap.put(e.getKey(), false);
						}
						
						continue;
					}
					
					if (e.getValue().equals(String.class)) {
						newMap.put(e.getKey(), value);
					} else if (e.getValue().equals(int.class)) {
						newMap.put(e.getKey(), Integer.parseInt(value.replaceAll("[,]","")));
					} else if (e.getValue().equals(double.class)) {
						newMap.put(e.getKey(), Double.parseDouble(value.replaceAll("[,]","")));
					} else if (e.getValue().equals(Date.class)) {
						newMap.put(e.getKey(), Util.dateParser3(value));
					} else if (e.getValue().equals(boolean.class)) {
						newMap.put(e.getKey(), Boolean.parseBoolean(value));
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					return values;
				}

			}
			
			return newMap;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return values;
	}

	private static String formatDate(Date date, SimpleDateFormat sdf) {
		if(date!=null) {
			return sdf.format(date);
		}
		return "";
	}

	public static String formatDouble(double doubleValue, DecimalFormat format) {
		String formattedValue = "";
		if(doubleValue != SimpleChapterUtils.UNDEFINED_DOUBLE_VALUE){
			formattedValue = format.format(doubleValue);
		}
		return formattedValue;
	}
	
	public static String formatInt(int intValue) {
		if(intValue == Integer.MIN_VALUE){
			return "";
		}
		return Integer.toString(intValue);
	}
	
	
	private static void applyCommunityQARules(MultilineElementsMap map, DocumentI doc, Search search) {
		// apply QA rules for community 
		int commID = search.getCommId();
		
		int ndexCommunityId = ro.cst.tsearch.ServerConfig.getNdexCommunityId();
		
		if(commID == ndexCommunityId){
			//task 8097
			if (doc.getDocType().equalsIgnoreCase("COURT") &&
					(doc.getDocSubType().equalsIgnoreCase("Judgment") ||
					doc.getDocSubType().equalsIgnoreCase("Court") ||
					doc.getDocSubType().equalsIgnoreCase("Bankruptcy") ||
					doc.getDocSubType().equalsIgnoreCase("Divorce")
					)) {
				map.remove(MultilineElementsMap.BOOK_NO);
				map.remove(MultilineElementsMap.PAGE_NO);
			}
		}
	}

	private static void keepTheFirstOne(Collection<DocumentI> documents) {
		
		if(documents==null){
			return ;
		}
		
		DocumentI first = null;
		for(DocumentI current:documents){
			first = current;
			break;
		}
		
		documents.clear();
		
		if(first!=null){
			documents.add(first);
		}
	}
	
	/**
	 * Only treats transfers or mortgages
	 * @param search
	 * @param documents
	 */
	private static void keepTheFirstOneValidateByOwner(
			Search search, Collection<DocumentI> documents) {
		
		if(search == null) {
			return;
		}
		PartyI owners = search.getSa().getOwners();
		if (owners == null || owners.size()==0) {
			return;
		}
		
		List<TransferI> transfers = new ArrayList<TransferI>();
		for (DocumentI documentI : documents) {
			if(documentI instanceof TransferI) {
				transfers.add((TransferI) documentI);
			}
		}
		
		List<MortgageI> mortgages = new ArrayList<MortgageI>();
		for (DocumentI documentI : documents) {
			if(documentI instanceof MortgageI) {
				mortgages.add((MortgageI) documentI);
			}
		}

		documents.clear();
		
		if(transfers.size() > 0) {
			
			String[] realTransferSubcategories = DocumentTypes.getRealTransferSubcategories(
					Integer.parseInt(search.getSa().getAtribute(SearchAttributes.P_STATE)),
					Integer.parseInt(search.getCountyId()));
			
			for (DocumentI d : transfers) {
				TransferI transfer = (TransferI) d;
				boolean foundSubcategory = false;
				for (String subcategory : realTransferSubcategories) {
					if(subcategory.equalsIgnoreCase(transfer.getDocSubType())) {
						foundSubcategory = true;
						break;
					}
				}
				
				if (!foundSubcategory || transfer.getGrantee().size() == 0) { 
					continue;
				}
				if (GenericNameFilter.isMatchGreaterThenScore(owners,transfer.getGrantee(), 0.79)
						|| GenericNameFilter.isMatchGreaterThenScore(transfer.getGrantee(), owners, 0.79)) {
					documents.add(transfer);
					return;
				}
			}
		} else if(mortgages.size() > 0) {
				
			DocumentsManagerI docManager = search.getDocManager();
			try {
				docManager.getAccess();
				MortgageI lastMortgageForCurrentOwner = WarningUtils.getLastMortgageForCurrentOwner(search, mortgages, docManager.getLastTransfer(), false);
				if(lastMortgageForCurrentOwner != null) {
					documents.add(lastMortgageForCurrentOwner);
				}
				return;
			} finally {
				docManager.releaseAccess();
			}
				
		}
	}

	private static void updateMapWithImageLinks(MultilineElementsMap map, DocumentI doc, Search search, boolean singleBPRequested) {
		Map<String, String> newMap = new HashMap<String, String>();
		
		String linkForImage = DocumentUtils.createImageLinkWithDummyParameter(doc, search);
		String linkForPDF = DocumentUtils.createImageLinkForImageAsPDF(doc, search);
		
		for (Entry<String, String> entry : map.entrySet()) {
			String key1 = entry.getKey();
			String value1 = entry.getValue();
			if (!key1.endsWith(MultilineElementsMap.DOCUMENT_IMAGE_MARKER) && !key1.endsWith(MultilineElementsMap.DOCUMENT_PDF_MARKER)) {
				if (!StringUtils.isEmpty(value1)) {
					if (doc.hasImage() && (doc.isIncludeImage() || singleBPRequested)) {
						newMap.put(key1 + MultilineElementsMap.DOCUMENT_IMAGE_MARKER, createDocimageLink(linkForImage, value1));
						newMap.put(key1 + MultilineElementsMap.DOCUMENT_PDF_MARKER, createDocimageLink(linkForPDF, value1));
					} else {
						newMap.put(key1 + MultilineElementsMap.DOCUMENT_IMAGE_MARKER, value1);
						newMap.put(key1 + MultilineElementsMap.DOCUMENT_PDF_MARKER, value1);
					}
				} else {
					newMap.put(key1 + MultilineElementsMap.DOCUMENT_IMAGE_MARKER, "");
					newMap.put(key1 + MultilineElementsMap.DOCUMENT_PDF_MARKER, "");					
				}
			}
		}
		map.putAll(newMap);
		
		//TASK 7869
		String[] tagsToModify = new String[]{MultilineElementsMap.INSTRUMENT_STCKC};
		
		for (String key : tagsToModify) {
			String imageKey = key + MultilineElementsMap.DOCUMENT_IMAGE_MARKER;
			if (map.containsKey(imageKey) && doc.hasImage() && (doc.isIncludeImage() || singleBPRequested)) {
				map.put(imageKey, getValueForImageLinks(doc, imageKey, map.get(imageKey), linkForImage));
			}

			String pdfKey = key + MultilineElementsMap.DOCUMENT_PDF_MARKER;
			if (map.containsKey(pdfKey) && doc.hasImage() && (doc.isIncludeImage() || singleBPRequested)) {
				map.put(pdfKey, getValueForImageLinks(doc, pdfKey, map.get(pdfKey), linkForPDF));
			}
		}
	}
	
	//by TASK 7869
	private static String getValueForImageLinks(DocumentI doc, String key, String value, String link){
		
		if (key.contains(MultilineElementsMap.INSTRUMENT_STCKC)) {

			// TASK 7869
			String book = cleanInstrument(doc.getBook());
			String page = cleanInstrument(doc.getPage());
			String instrNo = cleanInstrument(doc.getInstno());
			String docNo = cleanInstrument(doc.getDocno());

			String newValue = "";

			boolean justBP = true;

			Calendar cal = Calendar.getInstance();
			cal.set(2003, Calendar.DECEMBER, 1);
			
			// Task 8089
			if(doc.getDate() != null && doc.getDate().compareTo(cal.getTime()) < 0) {
				if (doc.hasDocNo()) {
					newValue = " as " + createDocimageLink(link,"Document No. " + docNo);
					justBP = false;
				} else if (doc.hasInstrNo()) {
					newValue = " as " + createDocimageLink(link,"Document No. " + instrNo);
					justBP = false;
				}
			} else {
				if (doc.hasInstrNo()) {
					newValue = " as " + createDocimageLink(link,"Document No. " + instrNo);
					justBP = false;
				} else if (doc.hasDocNo()) {
					newValue = " as " + createDocimageLink(link,"Document No. " + docNo);
					justBP = false;
				}
			}

			if (doc.hasBookPage()) {
				if (justBP) {
					newValue = " in " + createDocimageLink(link, "Book " + book + " at Page " + page);
				} else
					newValue += " in Book " + book + " at Page " + page;
			}
			
			return newValue.trim();
		}
		
		return "";
	}

	private static boolean subelementsEquals(String cand, String ref){
		
		if(cand.length()==ref.length()+1){
			return cand.equals(ref+"S");
		}else if(cand.length()+1==ref.length()){
			return ref.equals(cand+"S");
		}
			
		return cand.equals(ref);
	}
	
	/**
	 * Resolves/completes the contents of a second level element (sub-element) 
	 * @param doc
	 * @param contents
	 * @param currentState
	 * @param searchId
	 * @return
	 */
	public static String completeDocumentsSecondLevelElements(DocumentI doc, String contents, int currentState, int currentCountyId, String sortBy, Search search, boolean singleBPRequested, 
			Class<?> templateAttachedClass, Object templateAttachedObject) {
		
		java.text.SimpleDateFormat sdf = TemplateBuilder.getSimpleDateFormatForTemplates(currentState);
		
		Matcher subElementMatcher = DOCUMENTS_SUBELEMENT_PATTERN.matcher(contents);
		
		StringBuffer completedContents = new StringBuffer();
		
		/* Loops through the list of sublements */
		while (subElementMatcher.find()) {	
			try {
				String replacement = ""; 
				String subelement = subElementMatcher.group(1).trim();
				String subcategory = "";
				try { subcategory = subElementMatcher.group(3).trim(); } catch(Exception ignored) {} 
				String elementContents = subElementMatcher.group(4); 
				
				/* Fill the tags for %$ LEGAL= ... $%	 */
				if( subelementsEquals(subelement, MultilineElementsMap.LEGAL) && doc!=null) {
					
					Collection<PropertyI> properties = doc.getProperties();                                        
					
					for(PropertyI property : properties) {
						
						String subElementContents = elementContents;
						
		 				MultilineElementsMap mapCurent = new MultilineElementsMap(MultilineElementsMap.SUBELEMENT_TYPE,MultilineElementsMap.LEGAL);
		 				mapCurent.setAutoConvertEmpty(true);
		 				mapCurent.setAutoConvertNull(true);
		 				
		 				mapCurent.put(MultilineElementsMap.LOT,property.getLegal().getSubdivision().getLot().replaceAll("(?>\\s+|^)0(?:\\s+|$)", ""));
		 				mapCurent.put(MultilineElementsMap.SUBLOT, property.getLegal().getSubdivision().getSubLot());
		 				mapCurent.put(MultilineElementsMap.UNIT,property.getLegal().getSubdivision().getUnit());
		 				mapCurent.put(MultilineElementsMap.BLOCK,property.getLegal().getSubdivision().getBlock());
		 				mapCurent.put(MultilineElementsMap.PHASE,property.getLegal().getSubdivision().getPhase());
		 				mapCurent.put(MultilineElementsMap.SECTION,property.getLegal().getTownShip().getSection());
		 				mapCurent.put(MultilineElementsMap.SUBDIVISION,property.getLegal().getSubdivision().getName());
		 				
		 				if(property.getType() == PropertyI.PType.CONDO ) {
		 					mapCurent.put(MultilineElementsMap.CONDOMINIUM,property.getLegal().getSubdivision().getName());	
		 				}
		 				
		 				subElementContents = replaceThirdLevelElements(templateAttachedClass, templateAttachedObject, doc, sdf, mapCurent, documents, MultilineElementsMap.LEGAL, subElementContents);
		 				
		 				replacement += replaceTags(subElementContents, mapCurent);
					}
				}
				
				/* Fill the tags for %$ ADDRESS= ... $%	 */
				if( subelementsEquals(subelement, MultilineElementsMap.ADDRESS) && doc!=null) {
					
					Collection<PropertyI> properties = doc.getProperties();                                        
					
					for(PropertyI property : properties) {
						
						if(property.getAddress() != null) {
							
							String subElementContents = elementContents;
						
			 				MultilineElementsMap mapCurent = new MultilineElementsMap(MultilineElementsMap.SUBELEMENT_TYPE,MultilineElementsMap.ADDRESS);
			 				mapCurent.setAutoConvertEmpty(true);
			 				mapCurent.setAutoConvertNull(true);
			 				
			 				mapCurent.put(MultilineElementsMap.CITY, property.getAddress().getCity());
			 				
			 				subElementContents = replaceThirdLevelElements(templateAttachedClass, templateAttachedObject, doc, sdf, mapCurent, documents, MultilineElementsMap.ADDRESS, subElementContents);
			 				
			 				replacement += replaceTags(subElementContents, mapCurent);
		 				
						}
					}
				}
				
				/* Fill the tags for %$ RECEIPTS = ... $%	 */
				if( subelementsEquals(subelement, MultilineElementsMap.RECEIPTS) && doc!=null) {
					
					if(doc.isOneOf(DocumentTypes.CITYTAX, DocumentTypes.COUNTYTAX)) {
						
						if(doc instanceof TaxDocumentI ) {
							Collection<Receipt> receipts = new ArrayList<Receipt>();
							
							TaxDocumentI taxDoc = (TaxDocumentI) doc;	
							if(taxDoc.getReceipts() != null) {
								receipts.addAll( taxDoc.getReceipts() );
							}
							
							for(Receipt receipt : receipts) {
								
								String subElementContents = elementContents;
								
				 				MultilineElementsMap mapCurent = new MultilineElementsMap(MultilineElementsMap.SUBELEMENT_TYPE,MultilineElementsMap.RECEIPTS);
				 				mapCurent.setAutoConvertEmpty(true);
				 				mapCurent.setAutoConvertNull(true);
				 				
				 				mapCurent.put(MultilineElementsMap.RECEIPT_NUMBER,receipt.getReceiptNumber());
				 				mapCurent.put(MultilineElementsMap.RECEIPT_AMOUNT,receipt.getReceiptAmount());
				 				mapCurent.put(MultilineElementsMap.RECEIPT_DATE,receipt.getReceiptDate());
				 				putInstrumentTags(mapCurent,taxDoc.getInstrument(), -1, -1, search.getSa().isCondo());
				 				
				 				subElementContents = replaceThirdLevelElements(templateAttachedClass, templateAttachedObject, doc, sdf, mapCurent, documents, MultilineElementsMap.RECEIPTS, subElementContents);
				 				
				 				replacement += replaceTags(subElementContents, mapCurent);
							}
						}
					}
				}
				
				/* Fill the tags for %$ INSTALLMENTS = ... $%	 */
				if( subelementsEquals(subelement, MultilineElementsMap.INSTALLMENTS) && doc!=null) {
					
					if(doc.isOneOf(DocumentTypes.CITYTAX, DocumentTypes.COUNTYTAX)) {
						
						if(doc instanceof TaxDocumentI ) {
							
							DecimalFormat df = new DecimalFormat( "#,###,###,##0.00" );
							
							Collection<Installment> installments = new ArrayList<Installment>();
							
							TaxDocumentI taxDoc = (TaxDocumentI) doc;	
							if(taxDoc.getInstallments() != null) {
								installments.addAll( taxDoc.getInstallments() );
							}
							
							int count = 1;
							for(Installment installment : installments) {
								
								String subElementContents = elementContents;
								
				 				MultilineElementsMap mapCurent = new MultilineElementsMap(MultilineElementsMap.SUBELEMENT_TYPE, MultilineElementsMap.INSTALLMENTS);
				 				mapCurent.setAutoConvertEmpty(true);
				 				mapCurent.setAutoConvertNull(true);
				 				
				 				mapCurent.put(MultilineElementsMap.INSTALL_NO, Integer.toString(count++));
				 				mapCurent.put(MultilineElementsMap.BASE_AMOUNT, formatDouble(installment.getBaseAmount(), df));
				 				mapCurent.put(MultilineElementsMap.AMOUNT_PAID, formatDouble(installment.getAmountPaid(), df));
				 				mapCurent.put(MultilineElementsMap.AMOUNT_DUE, formatDouble(installment.getAmountDue(), df));
				 				mapCurent.put(MultilineElementsMap.PENALTY_AMOUNT, formatDouble(installment.getPenaltyAmount(), df));
				 				mapCurent.put(MultilineElementsMap.HOMESTEAD_EXEMPTION, formatDouble(installment.getHomesteadExemption(), df));
				 				mapCurent.put(MultilineElementsMap.STATUS, installment.getStatus());
				 				mapCurent.put(MultilineElementsMap.YEAR_DESCRIPTION, installment.getYearDescription());
				 				mapCurent.put(MultilineElementsMap.BILL_TYPE, installment.getBillType());
				 				
				 				subElementContents = replaceThirdLevelElements(templateAttachedClass, templateAttachedObject, doc, sdf, mapCurent, documents, MultilineElementsMap.INSTALLMENTS, subElementContents);
				 				
				 				replacement += replaceTags(subElementContents, mapCurent);
							}
						}
					}
				}
				
				/* Fill the tags for %$ SA_INSTALLMENTS = ... $% (Special Assessment Installments)	 */
				if( subelementsEquals(subelement, MultilineElementsMap.SA_INSTALLMENTS) && doc!=null) {
					
					if(doc.isOneOf(DocumentTypes.CITYTAX, DocumentTypes.COUNTYTAX)) {
						
						if(doc instanceof TaxDocumentI ) {
							
							DecimalFormat df = new DecimalFormat( "#,###,###,##0.00" );
							
							Collection<Installment> sa_installments = new ArrayList<Installment>();
							
							TaxDocumentI taxDoc = (TaxDocumentI) doc;	
							if(taxDoc.getSpecialAssessmentInstallments() != null) {
								sa_installments.addAll( taxDoc.getSpecialAssessmentInstallments() );
							}
							
							int count = 1;
							for(Installment installment : sa_installments) {
								
								String subElementContents = elementContents;
								
				 				MultilineElementsMap mapCurent = new MultilineElementsMap(MultilineElementsMap.SUBELEMENT_TYPE, MultilineElementsMap.SA_INSTALLMENTS);
				 				mapCurent.setAutoConvertEmpty(true);
				 				mapCurent.setAutoConvertNull(true);
				 				
				 				mapCurent.put(MultilineElementsMap.INSTALL_NO, Integer.toString(count++));
				 				mapCurent.put(MultilineElementsMap.BASE_AMOUNT, formatDouble(installment.getBaseAmount(), df));
				 				mapCurent.put(MultilineElementsMap.AMOUNT_PAID, formatDouble(installment.getAmountPaid(), df));
				 				mapCurent.put(MultilineElementsMap.AMOUNT_DUE, formatDouble(installment.getAmountDue(), df));
				 				mapCurent.put(MultilineElementsMap.PENALTY_AMOUNT, formatDouble(installment.getPenaltyAmount(), df));
				 				mapCurent.put(MultilineElementsMap.HOMESTEAD_EXEMPTION, formatDouble(installment.getHomesteadExemption(), df));
				 				mapCurent.put(MultilineElementsMap.STATUS, installment.getStatus());
				 				mapCurent.put(MultilineElementsMap.YEAR_DESCRIPTION, installment.getYearDescription());
				 				mapCurent.put(MultilineElementsMap.BILL_TYPE, installment.getBillType());
				 				
				 				subElementContents = replaceThirdLevelElements(templateAttachedClass, templateAttachedObject, doc, sdf, mapCurent, documents, MultilineElementsMap.SA_INSTALLMENTS, subElementContents);
				 				
				 				replacement += replaceTags(subElementContents, mapCurent);
							}
						}
					}
				}
				
				/* Fill the tags for %$ HOA = ... $%	 */
				if( subelementsEquals(subelement, MultilineElementsMap.HOAS) && doc!=null) {
					
					if(doc.isOneOf(DocumentTypes.HOA)) {
						
						if(doc instanceof HOACondoI ) {
							Collection<HOAInfo> hoaInfos = new ArrayList<HOAInfo>();
							
							HOACondoI hoaDoc = (HOACondoI) doc;	
							if(hoaDoc!= null) {
								hoaInfos.addAll( hoaDoc.getHOAInfo());
							}
							
							for(HOAInfo hoaInfo : hoaInfos) {
								
								String subElementContents = elementContents;
								
				 				MultilineElementsMap mapCurent = new MultilineElementsMap(MultilineElementsMap.SUBELEMENT_TYPE, MultilineElementsMap.HOAS);
				 				mapCurent.setAutoConvertEmpty(true);
				 				mapCurent.setAutoConvertNull(true);
				 				
				 				mapCurent.put(MultilineElementsMap.ASSOCIATION_NAME, hoaInfo.getAssociationName());
				 				mapCurent.put(MultilineElementsMap.HOA_NAME, hoaInfo.getHOAName());
				 				mapCurent.put(MultilineElementsMap.MASTER_HOA, hoaInfo.getMasterHOA());
				 				mapCurent.put(MultilineElementsMap.ADD_HOA, hoaInfo.getAddHOA());
				 				mapCurent.put(MultilineElementsMap.HOA_PLAT_BOOK, hoaInfo.getHoaPlatBook());
				 				mapCurent.put(MultilineElementsMap.HOA_PLAT_PAGE, hoaInfo.getHoaPlatPage());
				 				mapCurent.put(MultilineElementsMap.HOA_DECL_BOOK, hoaInfo.getHoaDeclBook());
				 				mapCurent.put(MultilineElementsMap.HOA_DECL_PAGE, hoaInfo.getHoaDeclPage());
				 				
				 				subElementContents = replaceThirdLevelElements(templateAttachedClass, templateAttachedObject, doc, sdf, mapCurent, documents, MultilineElementsMap.HOAS, subElementContents);
				 				
				 				replacement += replaceTags(subElementContents, mapCurent);
							}
						}
					}
				}
				
				/* Fill the tags for %$ AMENDMENTS .... = ... $%	 */
				if(  subelementsEquals(subelement, MultilineElementsMap.RESTRICTIONS) && doc!=null ) {

					if(doc.isOneOf(DocumentTypes.RESTRICTION)) {
						
						Collection<RegisterDocumentI> references = new ArrayList<RegisterDocumentI>();
						if( doc.getReferences() != null ) {
							references.addAll( doc.getReferences() );
						}
						DocumentUtils.sortDocuments(references, sortBy);
						
						for(RegisterDocumentI reference : references ) {
							
							/* Keep only the desired documents */
							String category = MultilineElementsMap.allSpecialSubElements.get(subelement);
														
							if(reference.isChecked() && reference.is(category) ) {
								
								/* If we have a subcategory, keep only documents with that subcategory */
								
								if(subcategory.isEmpty() || reference.getDocSubType().equalsIgnoreCase(subcategory)) {
									
									 String subElementContents = elementContents;
																		
									 MultilineElementsMap mapCurent = new MultilineElementsMap(MultilineElementsMap.SUBELEMENT_TYPE,MultilineElementsMap.RESTRICTIONS);
									 mapCurent.setAutoConvertEmpty(true);
									 mapCurent.setAutoConvertNull(true);
			
									 mapCurent.put(MultilineElementsMap.DOCUMENT_TYPE, reference.getDocType());
									 mapCurent.put(MultilineElementsMap.DOCUMENT_SUBTYPE, reference.getDocSubType());
									 mapCurent.put(MultilineElementsMap.SERVER_DOCUMENT_TYPE, reference.getServerDocType());
									 mapCurent.put(MultilineElementsMap.DOCUMENT_ID, reference.getId());
									 mapCurent.put(MultilineElementsMap.GRANTEE,reference.getGranteeFreeForm());
									 	
									 mapCurent.put(MultilineElementsMap.GRANTOR,reference.getGrantorFreeForm());
									 mapCurent.put(MultilineElementsMap.DOCUMENT_REMARKS, reference.getRemarks(true));
									 mapCurent.put(MultilineElementsMap.NOTE, reference.getNote());
									 mapCurent.put(MultilineElementsMap.SSN4, reference.getAllSSN());	
									 
									 putDateTags(mapCurent,reference,sdf,contents,false);									 
									 putInstrumentTags(mapCurent,reference.getInstrument(), -1, -1, search.getSa().isCondo());
									 updateMapWithImageLinks(mapCurent,reference,search, singleBPRequested);
									 
									 subElementContents = replaceThirdLevelElements(templateAttachedClass, templateAttachedObject, reference, sdf, mapCurent, documents, MultilineElementsMap.RESTRICTIONS, subElementContents);
									 
									 replacement += replaceTags(subElementContents, mapCurent);
								}
							}
						}
					}
				}
				
				/* Fill the tags for %$ AMENDMENTS .... = ... $%	 */	//B5850
				if( subelementsEquals(subelement, MultilineElementsMap.CCERS)  && doc!=null  ) {

					if(doc.isOneOf(DocumentTypes.CCER)) {
						
						Collection<RegisterDocumentI> references = new ArrayList<RegisterDocumentI>();
						if( doc.getReferences() != null ) {
							references.addAll( doc.getReferences() );
						}
						DocumentUtils.sortDocuments(references, sortBy);
						
						for(RegisterDocumentI reference : references ) {
							
							/* Keep only the desired documents */
							String category = MultilineElementsMap.allSpecialSubElements.get(subelement);
														
							if(reference.isChecked() && reference.is(category) ) {
								
								/* If we have a subcategory, keep only documents with that subcategory */
								
								if(subcategory.isEmpty() || reference.getDocSubType().equalsIgnoreCase(subcategory)) {
									
									 String subElementContents = elementContents;
																		
									 MultilineElementsMap mapCurent = new MultilineElementsMap(MultilineElementsMap.SUBELEMENT_TYPE,MultilineElementsMap.CCERS);
									 mapCurent.setAutoConvertEmpty(true);
									 mapCurent.setAutoConvertNull(true);
			
									 mapCurent.put(MultilineElementsMap.DOCUMENT_TYPE, reference.getDocType());
									 mapCurent.put(MultilineElementsMap.DOCUMENT_SUBTYPE, reference.getDocSubType());
									 mapCurent.put(MultilineElementsMap.SERVER_DOCUMENT_TYPE, reference.getServerDocType());
									 mapCurent.put(MultilineElementsMap.DOCUMENT_ID, reference.getId());
									 mapCurent.put(MultilineElementsMap.GRANTEE,reference.getGranteeFreeForm());
									 	
									 mapCurent.put(MultilineElementsMap.GRANTOR,reference.getGrantorFreeForm());
									 mapCurent.put(MultilineElementsMap.DOCUMENT_REMARKS, reference.getRemarks(true));
									 mapCurent.put(MultilineElementsMap.NOTE, reference.getNote());
									 mapCurent.put(MultilineElementsMap.SSN4, reference.getAllSSN());	
									 
									 putDateTags(mapCurent,reference,sdf,contents,false);									 
									 putInstrumentTags(mapCurent,reference.getInstrument(), -1, -1, search.getSa().isCondo());
									 updateMapWithImageLinks(mapCurent,reference,search, singleBPRequested);
									 
									 subElementContents = replaceThirdLevelElements(templateAttachedClass, templateAttachedObject, reference, sdf, mapCurent, documents, MultilineElementsMap.CCERS, subElementContents);
									 
									 replacement += replaceTags(subElementContents, mapCurent);
								}
							}
						}
					}
				}
				
				
				/* Fill the tags for %$ ASSIGNMENTS, APOINTMENTS, .... = ... $%	 */
				if( subelementsEquals(subelement, MultilineElementsMap.ASSIGNMENTS)
					|| subelementsEquals(subelement, MultilineElementsMap.APPOINTMENTS)							
					|| subelementsEquals(subelement, MultilineElementsMap.SUBORDINATIONS)
					|| subelementsEquals(subelement, MultilineElementsMap.MODIFICATIONS)								
					|| subelementsEquals(subelement, MultilineElementsMap.RQNOTICE)
					|| subelementsEquals(subelement, MultilineElementsMap.MISCELLANEOUS)
					|| subelementsEquals(subelement, MultilineElementsMap.SUBSTITUTION)
					|| subelementsEquals(subelement, MultilineElementsMap.ASSUMPTION)
					|| subelementsEquals(subelement, MultilineElementsMap.RELEASES)
					|| subelementsEquals(subelement, MultilineElementsMap.ALL_REFERENCES)
					
					|| subelementsEquals(subelement, MultilineElementsMap.ASSESSOR)
					|| subelementsEquals(subelement, MultilineElementsMap.COUNTYTAX)
					|| subelementsEquals(subelement, MultilineElementsMap.CITYTAX)
					
					) {

					if(doc!=null && doc.isOneOf(DocumentTypes.MORTGAGE, DocumentTypes.LIEN, DocumentTypes.CCER, DocumentTypes.RESTRICTION)) {
						
						Collection<RegisterDocumentI> references = new ArrayList<RegisterDocumentI>();
						if( doc.getReferences() != null ) {
							references.addAll( doc.getReferences() );
						}
						DocumentUtils.sortDocuments(references, sortBy);
						
						for(RegisterDocumentI reference : references ) {
							
							/* Keep only the desired documents */
							String category = MultilineElementsMap.allSpecialSubElements.get(subelement);
							boolean isAllReferences = subelementsEquals(subelement, MultilineElementsMap.ALL_REFERENCES);
							
							if(reference.isChecked() && 
									 ( reference.is(category) 
									 || (isAllReferences && DocumentTypes.isMortgageRelated(reference.getDocType())))
								) {
								
								/* If we have a subcategory, keep only documents with that subcategory */
								
								if(subcategory.isEmpty() || 
										reference.getDocSubType().equalsIgnoreCase(subcategory) ||
										checkSubcategory(subcategory, reference.getDocSubType())) {
									
									 String subElementContents = elementContents;
																		
									 MultilineElementsMap mapCurent = new MultilineElementsMap(MultilineElementsMap.SUBELEMENT_TYPE,MultilineElementsMap.ASSIGNMENTS);
									 mapCurent.setAutoConvertEmpty(true);
									 mapCurent.setAutoConvertNull(true);
			
									 mapCurent.put(MultilineElementsMap.DOCUMENT_TYPE, reference.getDocType());
									 mapCurent.put(MultilineElementsMap.DOCUMENT_SUBTYPE, reference.getDocSubType());
									 mapCurent.put(MultilineElementsMap.SERVER_DOCUMENT_TYPE, reference.getServerDocType());
									 mapCurent.put(MultilineElementsMap.DOCUMENT_ID, reference.getId());
									 mapCurent.put(MultilineElementsMap.GRANTEE,reference.getGranteeFreeForm());
									 	
									 mapCurent.put(MultilineElementsMap.GRANTOR,reference.getGrantorFreeForm());
									 mapCurent.put(MultilineElementsMap.DOCUMENT_REMARKS, reference.getRemarks(true));
									 mapCurent.put(MultilineElementsMap.NOTE, reference.getNote());
									 mapCurent.put(MultilineElementsMap.SSN4, reference.getAllSSN());
									 
									 putDateTags(mapCurent,reference,sdf,contents,false);									 
									 putInstrumentTags(mapCurent,reference.getInstrument(), currentState, currentCountyId, search.getSa().isCondo());
									 updateMapWithImageLinks(mapCurent,reference,search, singleBPRequested);
									 
									 subElementContents = replaceThirdLevelElements(templateAttachedClass, templateAttachedObject, reference, sdf, mapCurent, documents, MultilineElementsMap.ASSIGNMENTS, subElementContents);
									 
									 replacement += replaceTags(subElementContents, mapCurent);
								}
							}
						}
					}
				}
				
				/* Fill %$DEFAULTIFNOMATCH= ... $%		always the last one */
				if (subelementsEquals(subelement, MultilineElementsMap.DEFAULTIFNOMATCH)) {
					if (doc == null) {
						// place it for this doc 
						// replacement = elementContents;
						return elementContents; //ignore other content of this item
					} 
//					else {
//						// erase it for this doc (do nothing / don't add it)
//					}
				}
				
				subElementMatcher.appendReplacement(completedContents, Matcher.quoteReplacement(replacement));
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		subElementMatcher.appendTail(completedContents);

		return completedContents.toString();
	}
	
	public static String replaceThirdLevelElements(Class<?> templateAttachedClass, Object templateAttachedObject, DocumentI doc, SimpleDateFormat sdf, 
			MultilineElementsMap map, String prefix1, String prefix2, String content) {
		//complete third level conditions
		Matcher ma = thirdLevelBlockPattern.matcher(content);
		if(ma.find() && templateAttachedClass != null){
			try{
				
				HashMap<String, String> tmpMap = new HashMap<String, String>();
				putDateTags(tmpMap, doc, sdf, "", true);		//date tags inside IF condition
				
				//set level3 inputs ...
				Method setLevel3Tags = templateAttachedClass.getMethod("setLevel3Tags", new Class[]{HashMap.class});
				
				HashMap<String, String> newMap = new HashMap<String, String>();
				Iterator<Entry<String, String>> it = map.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<String, String> entry = it.next();
					newMap.put(prefix1 + "$" + prefix2 + "$" + entry.getKey(), entry.getValue());
				}
				it = tmpMap.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<String, String> entry = it.next();
					newMap.put(prefix1 + "$" + prefix2 + "$" + entry.getKey(), entry.getValue());
				}
				
				if(templateAttachedObject != null){
					setLevel3Tags.invoke(templateAttachedObject, new Object[] {newMap});
				}
				
				// find all third level blocks
				ma.reset();
				while(ma.find()){
					String blockKey = ma.group(1);
												
					Method getValue = templateAttachedClass.getMethod("get" + blockKey, new Class[]{});
					if (templateAttachedObject != null){
						String dataElement = (String) getValue.invoke(templateAttachedObject, new Object[]{});
						content = content.replace(ma.group(), dataElement);
					}
				}
				
				putDateTags(map,doc,sdf,content,false);		//some date tags may have appeared after expanding the third level blocks
				
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		
		return content;
	}
	
	/**
	 * Checks the condition for subcategory, if the doc has one of the requested subcategory 
	 * 
	 * 
	 * e.g. %$ASSIGNMENTS(ASSIGNMENT OF RENTS AND/OR LEASES|ASSIGNEMENT)$%
	 * doc should be an ASSIGNMENT with subcategory ASSIGNMENT OF RENTS AND/OR LEASES or ASSIGNEMENT
	 * 
	 * %$ASSIGNMENTS(~ASSIGNEMENT)$%
	 * any doc that is an ASSIGNEMENT but not with subcategory ASSIGNEMENT
	 * 
	 * @param subcategoryReference <- subcategory parsed from template
	 * @param docSubcategory <- document subcategory (docsubtype)
	 * @return true if doc has one of the subcategories and is not contained ina any other 
	 */
	private static boolean checkSubcategory(String subcategoryReference, String docSubcategory){
		boolean isOneOf = false; //is in one of the subcategories 
		boolean isNoneOf = true; //is is in none of the negated subcategories 
		
		boolean atLeastOneOf = false; //we have at least one subcategory in which our document must be
		boolean atLeastNoneOf = false; //we have at least one subcategory in which our document should not be
		
		if(StringUtils.isEmpty(subcategoryReference))
			return true;
		
		if(subcategoryReference.contains("|") || subcategoryReference.contains("~")){
			String[] subcategories = subcategoryReference.split("\\|");
			for(String s: subcategories){
				if(s.startsWith("~")){
					atLeastNoneOf = true;
					if(s.replaceFirst("~", "").equalsIgnoreCase(docSubcategory)){
						isNoneOf = false;
					}
				} else {
					atLeastOneOf = true;
					if(s.equalsIgnoreCase(docSubcategory))
						isOneOf = true;
				}
			}
			
			if(atLeastOneOf && atLeastNoneOf)
				return isOneOf && isNoneOf;
			else if(!atLeastOneOf && atLeastNoneOf)
				return isNoneOf;
			else if(atLeastOneOf && !atLeastNoneOf)
				return isOneOf;
		}
		
		return false;
	}
	
	/**
	 * Replaces the  %$ ... $% or [$ ... $] tags (depending on the map type) 
	 * with their corresponding entries from the map
	 * @param contents
	 * @param map
	 * @return
	 */
	public static String replaceTags(String contents, Map<String,String> map) {
			
		Matcher tagMatcher = ELEMENT_INNER_TAG_PATTERN.matcher(contents);   
		
		if(map instanceof MultilineElementsMap) {
			int type = ((MultilineElementsMap) map).getCurentType();
			
			if(type == MultilineElementsMap.SUBELEMENT_TYPE) {
				tagMatcher = SUBELEMENT_INNER_TAG_PATTERN.matcher(contents);  
			}
		}

		StringBuffer completed = new StringBuffer();
				
		/* Loop through the list of tags inside the subelement */
		while(tagMatcher.find()) {
			try {
				String tag = tagMatcher.group(1).trim();
				if (tag.contains("($")){
					tag = extractParameter(tag);
				}
				tagMatcher.appendReplacement(completed, Matcher.quoteReplacement(map.get(tag)));
			}catch(Exception e) {
				/* e.printStackTrace(); */	
			}
		}
		tagMatcher.appendTail(completed);
				
		return completed.toString();
	}

	private static final String DOC_IMAGE_LINK = "<a href=\"@\">!</a>";
	
	public static void putInstrumentTags(Map<String,String> map, InstrumentI instrument, int currentState, int currentCountyId, boolean isCondo) {
		String str = TemplateBuilder.defaultValue;
		String book = cleanInstrument(instrument.getBook());
		String page = cleanInstrument(instrument.getPage());
		String instrNo = cleanInstrument(instrument.getInstno());
		String docNo = cleanInstrument(instrument.getDocno());
		String bookType = cleanInstrument(instrument.getBookType());
		
		map.put(MultilineElementsMap.BOOK_NO, book);
     	map.put(MultilineElementsMap.PAGE_NO, page);
     	map.put(MultilineElementsMap.BOOK_TYPE, bookType+" "); //4172
		map.put(MultilineElementsMap.DOCUMENT_NO, docNo);
 		map.put(MultilineElementsMap.INSTRUMENT_NO,instrNo);
 		
 		map.put(MultilineElementsMap.BOOK_PAGE_INSTNO_AK,convertToAKInstrument(instrument, currentState));
 		
		map.put(MultilineElementsMap.BP_INST_DOC,  book.equals(str)?(instrNo.equals(str)?"Document No "+docNo:"Instrument "+instrNo):"Book "+book+" and Page "+page); 
		map.put(MultilineElementsMap.BP_DOC_INST,  book.equals(str)?(docNo.equals(str)?"Instrument "+instrNo:"Document No "+docNo):"Book "+book+" and Page "+page);   
		map.put(MultilineElementsMap.INST_BP_DOC,  instrNo.equals(str)?(book.equals(str)?"Document No "+docNo:"Book "+book+" and Page "+page):"Instrument "+instrNo); 
		map.put(MultilineElementsMap.INST_DOC_BP,  instrNo.equals(str)?(docNo.equals(str)?"Book "+book+" and Page "+page:"Document No "+docNo):"Instrument "+instrNo);
		map.put(MultilineElementsMap.DOC_BP_INST,  docNo.equals(str)?(book.equals(str)?"Instrument "+instrNo:"Book "+book+" and Page "+page):"Document No "+docNo);   
		map.put(MultilineElementsMap.DOC_INST_BP,  docNo.equals(str)?(instrNo.equals(str)?"Book "+book+" and Page "+page:"Instrument "+instrNo):"Document No "+docNo);
     	                                                                                                                                                              
		map.put(MultilineElementsMap.BP_INST_DOC1, book.equals(str)?(instrNo.equals(str)?"Document No "+docNo:"Instrument "+instrNo):"Book "+book+", Page "+page);    
		map.put(MultilineElementsMap.BP_INST_DOC2, book.equals(str)?(instrNo.equals(str)?"Document No "+docNo:"INSTRUMENT "+instrNo):"BOOK "+book+", PAGE "+page);
		
		map.put(MultilineElementsMap.BOOK_NV, convertToNVBookFormat(map, instrument, currentState));
				
		map.put(MultilineElementsMap.INSTRUMENT_NV, convertToNVInstrumentFormat(map, instrument, currentState));
		
		map.put(MultilineElementsMap.INSTRUMENT_STCKC, getSTCKCInstrumentFormat(instrument));
		
		boolean hasBook = !book.equals(str);
		boolean hasInst = !instrNo.equals(str);
		
		String bpInstCO = "in Book * at Page * as Reception No. *";
		
		if(hasBook && !hasInst){
			 bpInstCO = "in Book "+book+" at Page "+page;
		}else if(hasInst && !hasBook){
			bpInstCO = "as Reception No. "+instrNo;
		}else if(hasBook && hasInst){
			bpInstCO = "in Book "+book+" at Page "+page+" as Reception No. "+instrNo;;
		}
		
		map.put(MultilineElementsMap.BP_INST_CO, bpInstCO);    
		map.put(MultilineElementsMap.BP_INST_TX, getBP_INST_TX(map, instrument, currentState, currentCountyId));
		map.put(MultilineElementsMap.BP_INST_OH, getBP_INST_OH(map, instrument, currentState, currentCountyId, isCondo));
		
		//Book/Page = "in Book #### at Page ####"
		//Instrument number = "as Reception No. #####"
		//Book/Page and Instrument = "in Book #### at Page #### as Reception No. ####"
		
		map.put(MultilineElementsMap.BP_DOC_INST1, book.equals(str)?(docNo.equals(str)?"Instrument "+instrNo:"Document No "+docNo):"Book "+book+", Page "+page);      
		map.put(MultilineElementsMap.INST_BP_DOC1, instrNo.equals(str)?(book.equals(str)?"Document No "+docNo:"Book "+book+", Page "+page):"Instrument "+instrNo);    
		map.put(MultilineElementsMap.INST_DOC_BP1, instrNo.equals(str)?(docNo.equals(str)?"Book "+book+", Page "+page:"Document No "+docNo):"Instrument "+instrNo);   
		map.put(MultilineElementsMap.DOC_BP_INST1, docNo.equals(str)?(book.equals(str)?"Instrument "+instrNo:"Book "+book+", Page "+page):"Document No "+docNo);      
		map.put(MultilineElementsMap.DOC_INST_BP1, docNo.equals(str)?(instrNo.equals(str)?"Book "+book+", Page "+page:"Instrument "+instrNo):"Document No "+docNo);   

		map.put(MultilineElementsMap.BP_INST_DOC_ALL, 
 						(book.equals(str) && instrNo.equals(str) && docNo.equals(str))?"Document Number "+docNo:
						((!book.equals(str)?", Book "+book+", Page "+page + "":"")
						+(!instrNo.equals(str)?", Instrument Number "+instrNo:"")
						+(!docNo.equals(str)?", Document Number "+docNo:"")).replaceFirst(",","").trim()
 						);
	}
	
	
	private static String getSTCKCInstrumentFormat(InstrumentI instrument) {
		String book = cleanInstrument(instrument.getBook());
		String page = cleanInstrument(instrument.getPage());
		String instrNo = cleanInstrument(instrument.getInstno());
		String docNo = cleanInstrument(instrument.getDocno());
		
		String toDisplay = "";
		
		boolean justBP = true;
		
		Calendar cal = Calendar.getInstance();
		cal.set(2003, Calendar.DECEMBER, 1);
		
		// Task 8089
		if(instrument.getDate() != null && instrument.getDate().compareTo(cal.getTime()) < 0) {
			if(instrument.hasDocNo()) {
				toDisplay += " as Document No. "+docNo;
				justBP=false;
			} else if(instrument.hasInstrNo()){
				toDisplay += " as Document No. "+instrNo;
				justBP=false;
			}
		} else {
			if(instrument.hasInstrNo()){
				toDisplay += " as Document No. "+instrNo;
				justBP=false;
			} else if(instrument.hasDocNo()) {
				toDisplay += " as Document No. "+docNo;
				justBP=false;
			}
		}
		
		if(instrument.hasBookPage()){
			if(justBP)
				toDisplay+=" in Book "+book+" at Page "+page;
			else
				toDisplay+=" in Book "+book+" at Page "+page;
		}
		return toDisplay.trim();
	}

	private static final Calendar CAL_2000_01_01 = Calendar.getInstance();
	private static final Calendar CAL_2009_08_04 = Calendar.getInstance();
	static{
		CAL_2000_01_01.set(Calendar.YEAR, 2000);
		CAL_2000_01_01.set(Calendar.MONTH, Calendar.JANUARY);
		CAL_2000_01_01.set(Calendar.DAY_OF_MONTH, 1);
		CAL_2009_08_04.set(Calendar.YEAR, 2009);
		CAL_2009_08_04.set(Calendar.MONTH, Calendar.SEPTEMBER);
		CAL_2009_08_04.set(Calendar.DAY_OF_MONTH, 4);
	}
	
	private static String convertToNVBookFormat(Map<String, String> map, InstrumentI instrument, int currentState) {
		if (StateContants.NV==currentState) {
			Date recDate = instrument.getDate(); // recorded date
			
			if(recDate!=null){
				final Calendar cal = Calendar.getInstance();
				cal.setTime(recDate);
				
				String MM = String.valueOf(cal.get(Calendar.MONTH)+1);
				String DD = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
				DD = DD.length()<2?"0"+DD:DD;
				MM = MM.length()<2?"0"+MM:MM;
				
				if(recDate.before(CAL_2000_01_01.getTime())){
					String YY = String.valueOf(cal.get(Calendar.YEAR)).substring(2);	
					return YY+MM+DD;
				}else if ((recDate.equals(CAL_2000_01_01.getTime())||recDate.after(CAL_2000_01_01.getTime()))){
					String YYYY = String.valueOf(cal.get(Calendar.YEAR));
					return YYYY+MM+DD;
				}
			}
		}
			
		return "*";
	}

	private static String convertToNVInstrumentFormat(Map<String, String> map, InstrumentI instrument, int currentState) {
		if (StateContants.NV==currentState) {
			Date recDate = instrument.getDate(); // recorded date
			
			if(recDate!=null){
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
				final Calendar cal = Calendar.getInstance();
				cal.setTime(recDate);
				String instNo = instrument.getInstno();
				
				if(org.apache.commons.lang.StringUtils.isNotBlank(instNo)){
					instNo = instNo.replaceAll("^0+", "");
					if ( recDate.after(CAL_2009_08_04.getTime()) || recDate.equals(CAL_2009_08_04.getTime()) ){
						if(instNo.length()<=7){
							return sdf.format(recDate) + org.apache.commons.lang.StringUtils.leftPad(instNo, 7, "0");
						}else{
							return sdf.format(recDate)+instNo;
						}
					}
				}
			}
		}
		
		String instNo = instrument.getInstno();
		if(org.apache.commons.lang.StringUtils.isNotBlank(instNo)){
			return instNo;
		}
		
		return "*";
	}
	
	private static String convertToAKInstrument(InstrumentI instrument, int state) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (StateContants.AK==state) {
			String book = instrument.getBook();
			String page = instrument.getPage();
			String instno = instrument.getInstno();

			stringBuilder.append(StringUtils.isNotEmpty(book) ? "//Book: " + book : "");
			stringBuilder.append(StringUtils.isNotEmpty(page) ? "//Page: " + page : "");
			String instrNoRegEx =  "(\\d+)_(\\d{6})(\\d?)";
			if (RegExUtils.matches(instrNoRegEx, instno)){
				 List<String> matches = RegExUtils.getMatches(instrNoRegEx, instno);
				 if ( matches.size() == 3 ){
					 instno = matches.get(0) + "-" + matches.get(1) + "-" + org.apache.commons.lang.StringUtils.defaultIfEmpty(matches.get(2), "0");
				 }
				 
			}
			stringBuilder.append(StringUtils.isNotEmpty(instno) ? "//Serial Number: " + instno : "");
		}

		return stringBuilder.toString();
	}
	
	private static String getBP_INST_TX(Map<String, String> map, InstrumentI instrument, int currentState, int currentCountyId) {
		if (StateContants.TX == currentState) {
			String book = map.get(MultilineElementsMap.BOOK_NO);
			String page = map.get(MultilineElementsMap.PAGE_NO);
			String instrNo = map.get(MultilineElementsMap.INSTRUMENT_NO);
			String doctype = instrument.getDocType();
			
			String bpBaseWithoutComma = "Volume " + book + ", Page " + page;
			String bpBase = bpBaseWithoutComma +  ", ";
			String instrNoBase = "Document No. " + instrNo + ", ";
			String suffix1 = "Deed and Plat Records";
			String suffix2 = "Map and Plat Records";
			String suffix3 = "Real Property Records";
			String suffix4 = "Official Public Records";
			String suffix5 = "Official Records";
			String suffix6 = "Deed Records";
			
			String result = "";
			
			if (DocumentTypes.PLAT.equals(doctype)) {
				if (CountyConstants.TX_Bexar==currentCountyId) {
					result = bpBase + suffix1;
				} else {
					result = bpBase + suffix2;
				}
			} else {
				if (TemplateBuilder.defaultValue.equals(book)||TemplateBuilder.defaultValue.equals(page)) {
					
					switch (currentCountyId) {
					case CountyConstants.TX_Bexar:
						result = instrNoBase + suffix3;
						break;
					case CountyConstants.TX_Kendall:
					case CountyConstants.TX_Guadalupe:
						result = instrNoBase + suffix5;
						break;
					default:
						result = instrNoBase + suffix4;
						break;
					}
					
					
				} else {
					switch (currentCountyId) {
					case CountyConstants.TX_Bexar:
					{
						int year = instrument.getYear();
						if (year<1978) {
							result = bpBase + suffix6;
						} else {
							result = bpBase  + suffix3;
						}
					}
						break;

						
					case CountyConstants.TX_Comal:
					{
						int bookInt = -1;
						try {
							bookInt = Integer.parseInt(book);
						} catch (NumberFormatException nfe) {}
						if (bookInt<=352) {
							result = bpBase  + suffix6;
						} else {
							result = bpBase  + suffix4;
						}
					}
						break;
					
					case CountyConstants.TX_Guadalupe:
					{
						int bookInt = -1;
						try {
							bookInt = Integer.parseInt(book);
						} catch (NumberFormatException nfe) {}
						if (bookInt<=657) {
							result = bpBase  + suffix6;
						} else {
							result = bpBase  + suffix5;
						}
					}
						break;
						
					case CountyConstants.TX_Kendall:
					{
						Date date = instrument.getDate();
						if (date!=null) {
							Calendar calendar = Calendar.getInstance();
							calendar.setTime(date);
							calendar.set(Calendar.HOUR_OF_DAY, 0);
							calendar.set(Calendar.MINUTE, 0);
							calendar.set(Calendar.SECOND, 0);
							calendar.set(Calendar.MILLISECOND, 0);
							Calendar limit = Calendar.getInstance();
							limit.set(1982, Calendar.DECEMBER, 30, 0, 0, 0);
							limit.set(Calendar.MILLISECOND, 0);
							if (calendar.before(limit)) {
								result = bpBase  + suffix6;
							} else {
								result = bpBase  + suffix5;
							}
						} else {
							result = bpBaseWithoutComma;
						}
					}
						break;
						
					case CountyConstants.TX_Medina:
					{
						int year = instrument.getYear();
						if (year<1985) {
							result = bpBase  + suffix6;
						} else {
							result = bpBase  + suffix4;
						}
					}
						break;
						
					case CountyConstants.TX_Nueces:
					{
						int year = instrument.getYear();
						if (year<1993) {
							result = bpBase + suffix6;
						} else {
							result = bpBase  + suffix4;
						}
					}
						break;
						
					case CountyConstants.TX_San_Patricio:
					{
						Date date = instrument.getDate();
						if (date!=null) {
							Calendar calendar = Calendar.getInstance();
							calendar.setTime(date);
							calendar.set(Calendar.HOUR_OF_DAY, 0);
							calendar.set(Calendar.MINUTE, 0);
							calendar.set(Calendar.SECOND, 0);
							calendar.set(Calendar.MILLISECOND, 0);
							Calendar limit = Calendar.getInstance();
							limit.set(1983, Calendar.MAY, 19, 0, 0, 0);
							limit.set(Calendar.MILLISECOND, 0);
							if (calendar.before(limit)) {
								result = bpBase  + suffix6;
							} else {
								result = bpBase  + suffix4;
							}
						} else {
							result = bpBaseWithoutComma;
						}
					}
						break;
						
					default:
						result = bpBase  + suffix4;
						break;
					}
					
				}
			}
			
			return result;
			
		}	
		
		return "*";
	}
	
	private static String getBP_INST_OH(Map<String, String> map, InstrumentI instrument, int currentState, int currentCountyId, boolean isCondo) {
		if (StateContants.OH==currentState) {
			String book = map.get(MultilineElementsMap.BOOK_NO);
			String page = map.get(MultilineElementsMap.PAGE_NO);
			String instrNo = map.get(MultilineElementsMap.INSTRUMENT_NO);
			String doctype = instrument.getDocType();
			
			String condominiumPrefix = "Condominium ";
			String platPrefix = "Plat ";
			String instrumentPrefix = "Instrument ";
			String bookPrefix1 = "Volume ";
			String bookPrefix2 = "Plat Book ";
			String bookPrefix3 = "Book ";
			String bookPrefix4 = "Official Record ";
			String bookPrefix5 = "Deed Book ";
			String pagePrefix1 = " of Maps, Page ";
			String pagePrefix2 = ", Page ";
			String pagePrefix3 = " Page ";
			String pagePrefix4 = " and Page ";
			
			String instrumentBase = instrumentPrefix + instrNo; 
			
			String result = "";
			
			if (DocumentTypes.PLAT.equals(doctype)) {
				if (TemplateBuilder.defaultValue.equals(book)||TemplateBuilder.defaultValue.equals(page)) {			//has only Instrument Number
					result = platPrefix + instrumentBase;
					if (isCondo) {
						result = condominiumPrefix + result;
					}
				} else {																							//has Book and Page
					if (CountyConstants.OH_Cuyahoga==currentCountyId) {
						result = bookPrefix1 + book + pagePrefix1 + page;
					} else {
						result = bookPrefix2 + book + pagePrefix2 + page;
						if (isCondo) {
							result = condominiumPrefix + result;
						}
					}
				}
			} else {
				if (TemplateBuilder.defaultValue.equals(book)||TemplateBuilder.defaultValue.equals(page)) {			//has only Instrument Number
					if (CountyConstants.OH_Cuyahoga==currentCountyId) {
						Date date = instrument.getDate();
						if (date!=null) {
							Calendar calendar = Calendar.getInstance();
							calendar.setTime(date);
							calendar.set(Calendar.HOUR_OF_DAY, 0);
							calendar.set(Calendar.MINUTE, 0);
							calendar.set(Calendar.SECOND, 0);
							calendar.set(Calendar.MILLISECOND, 0);
							int year = calendar.get(Calendar.YEAR);
							Calendar limit = Calendar.getInstance();
							limit.set(1998, Calendar.NOVEMBER, 9, 0, 0, 0);
							limit.set(Calendar.MILLISECOND, 0);
							if (calendar.before(limit)) {
								int len = instrNo.length();
								if (len>2) {
									result = bookPrefix1 + new SimpleDateFormat("yy").format(date) + "-" + instrNo.substring(0, len-2) + pagePrefix3 + instrNo.substring(len-2);
								} else {
									result = instrumentBase;
								}
							} else {
								result = instrumentPrefix + year + instrNo;
							}
						} else {
							result = instrumentBase;
						}
					} else {
						result = instrumentBase;
					}
				} else {																						//has Book and Page
					if (CountyConstants.OH_Cuyahoga==currentCountyId) {
						result = bookPrefix1 + book + pagePrefix4 + page;
					} else if (CountyConstants.OH_Franklin==currentCountyId) {
						if (page.matches("(?i)^[A-Z].+")) {
							result = bookPrefix4 + book + pagePrefix4 + page;
						} else {
							result = bookPrefix5 + book.replaceFirst("(?i)^[A-Z]", "") + pagePrefix4 + page;
						}
					} else {
						result = bookPrefix3 + book + pagePrefix4 + page;
					}
				}
			}
			
			return result;
			
		}	
		
		return "*";
	}

	/**
	 * Fills tags like:<br>
	 * MultilineElementsMap.INSTRUMENT_DATE<br>
	 * MultilineElementsMap.FILLED_DATE
	 * @param map
	 * @param doc
	 * @param sdf
	 * @param contents
	 */
	public static void putDateTags(Map<String,String> map, DocumentI doc, SimpleDateFormat sdf, String contents, boolean putDefaultIfNotFound) {
		try{
			if(doc instanceof RegisterDocumentI) {
				RegisterDocumentI registerDoc = (RegisterDocumentI)doc;
				if(registerDoc.getInstrumentDate() == null) {
					map.put(MultilineElementsMap.INSTRUMENT_DATE, "");
				}
				else {
					putTagInMapForValue(MultilineElementsMap.INSTRUMENT_DATE, contents, map, sdf, registerDoc.getInstrumentDate(), putDefaultIfNotFound);
				}
				
				if(registerDoc.getRecordedDate() == null) {
					map.put(MultilineElementsMap.FILLED_DATE,"");
				}
				else {
					putTagInMapForValue(MultilineElementsMap.FILLED_DATE, contents, map, sdf, registerDoc.getRecordedDate(), putDefaultIfNotFound);
				}
			} else {
				if(doc.getYear() == SimpleChapterUtils.UNDEFINED_YEAR) {
					map.put(MultilineElementsMap.INSTRUMENT_DATE, "");
					map.put(MultilineElementsMap.FILLED_DATE,"");
				} else {
					map.put(MultilineElementsMap.INSTRUMENT_DATE, Integer.toString(doc.getYear()));
					map.put(MultilineElementsMap.FILLED_DATE, Integer.toString(doc.getYear()));
				}
				
				
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	/**
	 * This is not a general method. It is used only for putting data parameters for INSTRUMENT_DATE and FILLED_DATE.
	 * @param keyTag
	 * @param contents
	 * @param map
	 * @param sdf
	 * @param value
	 */
	private   static  void putTagInMapForValue(String keyTag, String contents, Map<String,String> map,SimpleDateFormat sdf, java.util.Date value, boolean putDefaultIfNotFound){
		SimpleDateFormat localDF;
		Map<String, String> dateFormat = extractDateFormat(keyTag, contents);
		for (String key : dateFormat.keySet()) {
		try{
			if (dateFormat.get(key).length()>0){
				localDF = new SimpleDateFormat(dateFormat.get(key));
			}else{
				localDF = sdf;
			}
			}catch(IllegalArgumentException ex){
				localDF = sdf;
			}
		map.put(key,localDF.format(value));
		}
		if (putDefaultIfNotFound && dateFormat.size()==0) {
			map.put(keyTag,sdf.format(value));
		}
	}
    /**
     * This method extract a date format parameter if it exists from a tag( for now just INSTRUMENT_DATE and FILLED_DATE).
     * At tag replacement you should use {@link AddDocsTemplates.KEY_SEPARATOR_FOR_TAG} for determining that the tag has parameters.
     * @param tagName
     * @param tagContents
     * @return
     */
	private static Map<String,String> extractDateFormat(String tagName, String tagContents){
		Map<String,String> tags = new HashMap<String,String>();
		StringBuffer outTagFormat = new StringBuffer("");
		String filledDateKey = tagName;
		Pattern allPattern = Pattern.compile(tagName);
		Matcher matcherAll = allPattern.matcher( tagContents);
		while(matcherAll.find()){
			int startMatch = matcherAll.start();
			String tempContents = tagContents.substring(startMatch); 
			if (tempContents.startsWith(tagName+"($")){
				Pattern pattern = Pattern.compile(tagName+"\\(\\$.*\\$\\)");
				Matcher matcher = pattern.matcher(tagContents);
				filledDateKey = tagName;
				if (matcher.find()){
					outTagFormat.delete(0, outTagFormat.length());
					String group = matcher.group();
					group = group.subSequence(group.indexOf("($")+2, group.indexOf("$)")).toString();
					outTagFormat.insert(0, group);
					filledDateKey+= KEY_SEPARATOR_FOR_TAG + outTagFormat.toString();
					tags.put(filledDateKey, outTagFormat.toString());
				}
			}else{
				tags.put(filledDateKey, "");
			}
			filledDateKey = tagName;
		}
		return tags;
	}
	
	private static String extractParameter(String tagContents){
		String parameterTagKey="";
		if (tagContents.contains("($")&& tagContents.endsWith("$)")){
			String tag = tagContents.substring(0, tagContents.indexOf("($"));
            String parameter = tagContents.substring( tagContents.indexOf("($")+2, tagContents.indexOf("$)") );
			
			parameterTagKey = tag + KEY_SEPARATOR_FOR_TAG + parameter;
			}
	 	return parameterTagKey;
	}
	
	public static String getKey(DocumentI doc, Map<String,Collection<String>> types, Map<String,String> contents, Collection<InstrumentI> instruments, boolean resolveUndefined, boolean disableBoilerFeature) {
		String key = "";
		
		String docType = doc.getDocType();
		String subType = doc.getDocSubType().toUpperCase();
		
		if(contents.get(docType + "_" + subType) == null) {
			key = docType;
		}
		
		if(contents.get(docType + "_" + subType) != null) {
			key = docType + "_" + subType;
		}
		
		if(resolveUndefined) {
			for(InstrumentI instr : instruments) {
				if(instr.flexibleEquals(doc.getInstrument())) {
					key = MultilineElementsMap.UNDEFINED;
					break;
				}
			}
		}
		
		if(types.containsKey(MultilineElementsMap.UNRELATED) 
				&& doc.getReferences().isEmpty() 
				&& doc.isOneOf(DocumentTypes.ASSIGNMENT,DocumentTypes.APPOINTMENT,DocumentTypes.RELEASE,DocumentTypes.MODIFICATION)) {
			key = MultilineElementsMap.UNRELATED;
		}
		
		if(!disableBoilerFeature && contents.containsKey(doc.getId())) {
			key = doc.getId();
		}
		
		return key;
	}
	
	public static String cleanInstrument(String str) {
		try {
			 str = str.replaceAll("&nbsp;", " ").replaceAll("^0$","").trim();
			 if(str.isEmpty()) str = TemplateBuilder.defaultValue;
			 return str;
		}catch(NullPointerException e) {
			return "";
		}
	}
	
	/**
	 *  CONTOR_TYPE_1 starts at 'a', and restarts after 'z' 
	 */
	public static String getContorType1(int contor) {
		return "("+(char)('a'+ (contor % ('z'-'a')))+")";
	}
	
	/**
	 *  CONTOR_TYPE_2 is seems to be like:  RE 03, 04, 05 ... 99 ... TEXT= 
	 */
	public static String getContorType2(int contor) {
		return "RE"+((contor+3)<10?"0":"")+(contor+3)+"TEXT= ";
	}
	
	private static String starToEmpty(String str) {
		return "*".equals(str)?"":str;
	}
	
	//Bug 5002
	private static String formatParcelId(String parcelId, String format) {
		if(StringUtils.isEmpty(parcelId)) {
			return "";
		}
		StringBuilder ret = new StringBuilder();;
		try {
			int j = 0;
			for (int i = 0; i < format.length(); i++) {
				switch (format.charAt(i)) {
				case 'x':
					if (parcelId.length() > j) {
						ret.append(parcelId.charAt(j++));
					}
					break;
				case '-':
					ret.append("-");
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return parcelId;
		}
		return ret.toString();
	}
	
	private static Object getValueOfTagWithParameters(Search search, String tag, Map<String, String> cbMap) {
		tag = tag.trim();
		Matcher matcher = Pattern.compile(TAG_WITH_PARAMETERS).matcher(tag);
		if (matcher.find()) {
			String tagName = matcher.group(1).trim();
			String tagParameters = matcher.group(2).trim();
			if (tagName.length()!=0 && tagParameters.length()!=0) {
				if((AddDocsTemplates.bpCode.equals(tagName) || AddDocsTemplates.cbCode.equals(tagName)) && org.apache.commons.lang.StringUtils.isNotBlank(tagParameters)){
					try{
						
						if(cbMap == null) {
							long userId = search.getSa().getAbstractorObject().getID().longValue();
							cbMap = new CaseInsensitiveMap<>(TemplatesServlet.getBoilerPlatesMap(search.getID(), userId, false, true, null, null));
						}
						return org.apache.commons.lang.StringUtils.defaultIfBlank(cbMap.get(tagParameters), "");
					}catch (Exception e){
						e.printStackTrace();
					}
				} else if (hasTransfers.equals(tagName)) {				
					int months = 0;
					try {
						months = Integer.parseInt(tagParameters);
					} catch (NumberFormatException e) {}
					DocumentsManagerI docManager = search.getDocManager();
					try {
		        		docManager.getAccess();
		        		List<DocumentI> transfers = docManager.getDocuments(true, "TRANSFER");
		    	        boolean result = false;
		    	        Calendar referenceDate = Calendar.getInstance();
		            	referenceDate.add(Calendar.MONTH, 0-months);
		            	for (DocumentI doc: transfers) {
		    	        	Date recordedDate = ((TransferI)doc).getRecordedDate();
		    	        	if (recordedDate!=null) {
		    	        		Calendar docDate = Calendar.getInstance();
		    		        	docDate.setTime(recordedDate);
		    		        	result = docDate.after(referenceDate);
		    		        	if (result)
		    		        		break;
		    	        	}
		            	}
		    	        return result;
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						docManager.releaseAccess();
					}
				} else if (documentsNoGreaterThan.equals(tagName)) {
					
					int noToCompare = 0;
					boolean onlyChecked = true;
					int lastIndexOf = tagParameters.lastIndexOf(",");
					if(lastIndexOf > 0) {
						noToCompare = Integer.parseInt(tagParameters.substring(lastIndexOf+1).trim());
						tagParameters = tagParameters.substring(0, lastIndexOf).trim();
						
						lastIndexOf = tagParameters.lastIndexOf(",");
						if(lastIndexOf > 0) {
							onlyChecked = Boolean.valueOf(tagParameters.substring(lastIndexOf + 1).trim());
							tagParameters = tagParameters.substring(0, lastIndexOf).trim();
							
						}
					}
					
					if(tagParameters.matches("\".*\"")) {
						tagParameters = "<#documents=" + tagParameters.substring(1, tagParameters.length() - 1) + "/#>";
						
						/* Parse the <#documents= ... (|category|)(|subcategory|) ... /#> element */
						ParseDocumentsTagResponse parseDocumentsTagResponse = parseDocumentsElement(tagParameters, true, null, null, null);
						
						FilledDocumentsInternalResponse documentsInternal = getDocumentsInternal(
								search, 
								new ArrayList<InstrumentStructForUndefined>(), 
								null,
								null,
								null, 
								false, 
								null, 
								onlyChecked?null:"fake",
								parseDocumentsTagResponse);
						
						return documentsInternal.getAppliedOverDocuments() > noToCompare;
						
					}
				}
			}
		}
		return null;
	}
	
	private static Vector<String> getCurrentDataSources(String itemForm, Search global){
		
		Vector<String> ret = new Vector<String>();
		
		if (global == null){
			return ret;
		}

		HashSet<DataSources> dataSourcesAsSet = global.getSa().getDataSourcesOnSearch();
		
		String separatorRegEx = $SEPARATOR_REG_EX;
		String separator = "";

		if (RegExUtils.matches(separatorRegEx, itemForm)){
			String match = RegExUtils.getFirstMatch(separatorRegEx, itemForm, 1);
			separator = match;
		}
		
		List<DataSources> dataSources = new ArrayList<DataSources>();
		try {
			Vector<DataSite> vec1 = HashCountyToIndex.getAllDataSites(global.getCommId(),
					ro.cst.tsearch.servers.parentsite.State.getState(Integer.parseInt(global.getStateId())).getStateAbv(),
					County.getCounty(Integer.parseInt(global.getCountyId())).getName(),
					false,
					global.getProductId());
			Map<Integer, DataSources> dataSourcesAsMap = new HashMap<Integer, DataSources>();
			for (DataSources dataSource : dataSourcesAsSet) {
				dataSourcesAsMap.put(dataSource.getSiteTypeInt(), dataSource);
			}
			for (DataSite dataSite : vec1) {
				DataSources dataSource = dataSourcesAsMap.get(dataSite.getSiteTypeInt());
				if(dataSource != null) {
					dataSources.add(dataSource);
				}
			}
			if(dataSources.size() < dataSourcesAsSet.size()) {
				logger.error("Found only a part of sites in getCurrentDataSources");
				for (DataSources dataSource : dataSourcesAsSet) {
					if(!dataSources.contains(dataSource)) {
						dataSources.add(dataSource);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Cannot determine correct order datasources", e);
			dataSources.clear();
			dataSources.addAll(dataSourcesAsSet);
		}
			
		
		
		int i = 0;
		for (DataSources dataSource : dataSources){

			String siteType = dataSource.getSiteType();
			String description = dataSource.getDescription();

			MultilineElementsMap map = new MultilineElementsMap(MultilineElementsMap.ELEMENT_TYPE, AddDocsTemplates.dataSources);

			map.put(MultilineElementsMap.DATASOURCE, siteType);
			map.put(MultilineElementsMap.DATASOURCE_DESCRIPTION, description);
			if (dataSource.getEffectiveStartDate() != null){
				map.put(MultilineElementsMap.DATASOURCE_EFFECTIVE_START_DATE, new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).format(dataSource.getEffectiveStartDate()));
			} else{
				map.put(MultilineElementsMap.DATASOURCE_EFFECTIVE_START_DATE, "");
			}
			map.put(MultilineElementsMap.DATASOURCE_RUN_TYPE, dataSource.getRunType());
			
			if (itemForm != null){
				String tagValue = replaceTags(itemForm, map).replaceAll("\r", "\n").replaceAll("\n", "").replaceAll("\\\\r", "\r").replaceAll("\\\\n", "\n");
				tagValue = tagValue.replaceAll(separatorRegEx, "");
				if (StringUtils.isEmpty(tagValue.trim())){
					tagValue = "";
				} else if (i < dataSources.size() - 1){
					tagValue += " " + separator;
				}
				i++;
				ret.add(tagValue);
			}
		}
		return ret;
	}
	
	/**
	 * Takes care of the "abstractors" multiline tag
	 * @param itemForm
	 * @param global
	 * @return
	 */
	private static Vector<String> getAbstractors(String itemForm, Search global) {
		
		Vector<String> ret = new Vector<String>();
		
		if (global == null || org.apache.commons.lang.StringUtils.isBlank(itemForm)){
			return ret;
		}
		
		String separatorRegEx = $SEPARATOR_REG_EX;
		String separator = "";

		if (RegExUtils.matches(separatorRegEx, itemForm)){
			String match = RegExUtils.getFirstMatch(separatorRegEx, itemForm, 1);
			separator = match;
		}
		
		List<AbstractorWorkedTime> abstractors = DBManager.getSimpleTemplate().query(
				"SELECT sut.*, u." + DBConstants.FIELD_USER_FIRST_NAME + ", u." + DBConstants.FIELD_USER_LAST_NAME + ", u." + DBConstants.FIELD_USER_LOGIN + 
				" FROM " + SearchUserTimeMapper.TABLE_SEARCH_USER_TIME + 
				" sut join " + DBConstants.TABLE_USER + " u on sut." + SearchUserTimeMapper.FIELD_USER_ID + " = u." + DBConstants.FIELD_USER_ID + 
				" WHERE sut." + SearchUserTimeMapper.FIELD_SEARCH_ID + " = ?", new AbstractorWorkedTime(), global.getID());
		boolean foundMainAbstractor = false;
		boolean foundSecondaryAbstractor = global.getSecondaryAbstractorId() == null;
		for (AbstractorWorkedTime abstractorWorkedTime : abstractors) {
			if(!foundMainAbstractor && abstractorWorkedTime.getUserId() == global.getSa().getAbstractorObject().getID().longValue()) {
				foundMainAbstractor = true;
			} else if (!foundSecondaryAbstractor && abstractorWorkedTime.getUserId() == global.getSecondaryAbstractorId()) {
				foundSecondaryAbstractor = true;
			}
		}
		
		if(!foundMainAbstractor) {
			UserAttributes abstractorObject = global.getSa().getAbstractorObject();
			abstractors.add(0, new AbstractorWorkedTime(abstractorObject));
		}
		
		if(!foundSecondaryAbstractor) {
	    	UserAttributes ua = UserManager.getUser(new BigDecimal(global.getSecondaryAbstractorId()));
	    	abstractors.add(new AbstractorWorkedTime(ua));
		}
		
		for (int i = 0; i < abstractors.size(); i++) {
			AbstractorWorkedTime workedTime = abstractors.get(i);
			
			MultilineElementsMap map = new MultilineElementsMap(MultilineElementsMap.ELEMENT_TYPE, AddDocsTemplates.abstractors);

			map.put(MultilineElementsMap.ABSTRACTORS.FIRST_NAME.toString(), workedTime.getFirstName());
			map.put(MultilineElementsMap.ABSTRACTORS.LAST_NAME.toString(), workedTime.getLastName());
			map.put(MultilineElementsMap.ABSTRACTORS.LOGIN.toString(), workedTime.getLoginName());
			
			if(workedTime.getWorkedTime() > 0) {
				map.put(MultilineElementsMap.ABSTRACTORS.PROCESSED_TIME_SECONDS.toString(), Long.toString(workedTime.getWorkedTime()));
				map.put(MultilineElementsMap.ABSTRACTORS.PROCESSED_TIME_FORMATTED.toString(), workedTime.getUserTime().getWorkedTimeFormatted());
			} else {
				map.put(MultilineElementsMap.ABSTRACTORS.PROCESSED_TIME_SECONDS.toString(), "0");
				map.put(MultilineElementsMap.ABSTRACTORS.PROCESSED_TIME_FORMATTED.toString(), "");
			}
			
			String tagValue = replaceTags(itemForm, map)
					.replaceAll("\r", "\n")
					.replaceAll("\n", "")
					.replaceAll("\\\\r", "\r")
					.replaceAll("\\\\n", "\n");
			tagValue = tagValue.replaceAll(separatorRegEx, "");
			if (StringUtils.isEmpty(tagValue.trim())){
				tagValue = "";
			} else if (i < abstractors.size() - 1){
				tagValue += " " + separator;
			}
			ret.add(tagValue);
			
		}
		
		return ret;
	}
}
