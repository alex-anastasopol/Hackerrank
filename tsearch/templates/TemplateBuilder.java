package ro.cst.tsearch.templates;

import java.io.File;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mailtemplate.MailTemplateUtils;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

import ro.cst.a2pdf.ConvertPdf2Tiff;
import ro.cst.tsearch.LoadConfig;
import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchFlags.CREATION_SOURCE_TYPES;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityProducts;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.data.CountyCommunityManager;
import ro.cst.tsearch.data.GenericState;
import ro.cst.tsearch.data.Payrate;
import ro.cst.tsearch.data.StateContants;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.CommunityTemplatesMapper;
import ro.cst.tsearch.database.rowmapper.CommunityUserTemplatesMapper;
import ro.cst.tsearch.database.rowmapper.CountyDefaultLegalMapper;
import ro.cst.tsearch.database.rowmapper.SearchUpdateMapper;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.search.name.FirstNameUtils;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servers.types.XXStewartPA;
import ro.cst.tsearch.servlet.FileServlet;
import ro.cst.tsearch.servlet.TemplatesServlet;
import ro.cst.tsearch.templates.edit.client.TemplateUtils;
import ro.cst.tsearch.titledocument.abstracts.DirectoryException;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.titledocument.abstracts.FidelityTSD;
import ro.cst.tsearch.titledocument.abstracts.FormatSa;
import ro.cst.tsearch.titledocument.abstracts.TaxUtilsNew;
import ro.cst.tsearch.tsr.UpdateFileCheckFilter;
import ro.cst.tsearch.user.MyAtsAttributes;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.user.UserRates;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.FileCopy;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.PDFUtils;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.webservices.PlaceOrderService;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentUtils;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.TaxDocumentI;
import com.stewart.ats.base.document.TransferI;
import com.stewart.ats.base.name.NameFormater;
import com.stewart.ats.base.name.NameFormaterI;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManagerI;
import com.stewart.ats.base.taxutils.BondAndAssessmentI;
import com.stewart.ats.base.taxutils.InstallmentI;
import com.stewart.ats.tsrindex.client.Receipt;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;
import com.stewart.ats.tsrindex.client.ocrelements.VestingInfoI;
import com.stewart.ats.tsrindex.client.template.TemplateInfo;
import com.stewart.ats.tsrindex.client.template.TemplatesInitUtils;
import com.stewart.ats.tsrindex.server.TsdIndexPageServer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.DisposedException;
import com.sun.star.text.XTextRange;

public class TemplateBuilder {
	
	private static final String TSRI_LINK_CODE = "http://34523tsriLink352345";
	private static final String ALL_IMAGES_LINK_CODE = "http://123ssfAllImagesLink345";
	private static final String TSR_LINK_CODE = "http://678tsrLink991";
	
    public static final String NOT_EXIST_COD="1a2c4d5dg45645ytutygfnbgopqaq";
    protected static final Logger logger = Logger.getLogger(TemplateBuilder.class);
    static DecimalFormat df = new DecimalFormat("#,###.##");
    static String defaultValue = "";
    
    public static final int NO_CHANGE_EXPORT_FORMAT = 0;
    public static final int TIFF_EXPORT_FORMAT = 1;
    public static final int PDF_EXPORT_FORMAT = 2;
    
//	public static String smallSpace = "__________";
//	public static String largeSpace = "_______________";
    
    private static String getProductType(int productType, CommunityAttributes ca){
         try{
 	       Products currentProduct = CommunityProducts.getProduct(ca.getID().longValue());
 	       return currentProduct.getProductName(productType);
         }catch(Exception e){
        	 e.printStackTrace();
        	 return "<#ERR: productType/#>";
         }
    }
    

    private static String getProductType(SearchAttributes sa, CommunityAttributes ca){
    	if(sa==null){
    		return "";
    	}
    	String service = sa.getAtribute(SearchAttributes.SEARCH_PRODUCT);
        int nr=-1;
         try{
 	        nr=Integer.parseInt(service);
 	        return getProductType(nr, ca);
         }catch(Exception e){
        	e.printStackTrace();
        	return "<#ERR: productType/#>";
         }
    }
    
    public static HashMap<String,Object> fillTemplateParameters(Search search, CommunityAttributes ca, 
            County currentCounty, State currentState, boolean doNotFillLegalDesc, boolean fillAtsExceptionTag, HashMap<String, Object> templateParamsPreloaded) {
    	SimpleDateFormat sdfMMddyyyy = new SimpleDateFormat("MM/dd/yyyy");
        HashMap <String,Object> templateParams = new HashMap<String,Object>();
        
        if(templateParamsPreloaded != null) {
        	templateParams.putAll(templateParamsPreloaded);
        } else {
        
        GregorianCalendar calendar = new GregorianCalendar();
        DocumentsManagerI docManager = search.getDocManager();
        SearchAttributes sa = search.getSa();
        
        NameFormaterI.TitleType legalFormat = NameFormaterI.TitleType.TITLE_CASE;
        NameFormaterI.TitleType addressFormat = NameFormaterI.TitleType.TITLE_CASE;
//        NameFormaterI nameFormat = new NameFormater(NameFormaterI.PosType.NO_CHANGE,NameFormaterI.TitleType.NO_CHANGE);
		try {
			legalFormat = MyAtsAttributes.getLegalFormatterForSearch(search.getID());
			addressFormat = MyAtsAttributes.getAddressFormatterForSearch(search.getID());
//			nameFormat = MyAtsAttributes.getNameFormatterForSearch(search.getID());
		}catch(Exception e) {}
		
        String has_buyer = sa.getBuyers().size() == 0 ? " " : "x"; 
        String has_lender = sa.getAtribute(SearchAttributes.BM1_LENDERNAME);
        if (StringUtils.isStringBlank(has_lender)
        		|| has_lender.toUpperCase().indexOf("N/A") != -1
        		|| has_lender.toUpperCase().indexOf("___") != -1) {
        	has_lender = " ";
        } else { has_lender = "x";}
        
        String has_bookPage = org.apache.commons.lang.StringUtils.isBlank(sa.getAtribute( SearchAttributes.LD_BOOKPAGE)) ? " " : "x";
        
       
        
        templateParams.put(AddDocsTemplates.updateFoundExtradata, false);
        if(search.getSa().isUpdate()) {
        	try {
        		
				docManager.getAccess();
				
				DocumentsManagerI manager = search.getDocManager();
				boolean foundRoDocs = ro.cst.tsearch.templates.TemplateUtils.foundRoDocs(manager, false);
				templateParams.put(AddDocsTemplates.updateFoundExtradata, foundRoDocs);
				
				/* Bug 4990 */
				foundRoDocs = ro.cst.tsearch.templates.TemplateUtils.foundRoDocs(manager, true);
				templateParams.put(AddDocsTemplates.updateFoundOutstandingExtradata, foundRoDocs);
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				docManager.releaseAccess();
			}
        }
        
        String productType = getProductType(sa, ca);
        String initialProductType = "";
        UserAttributes agent= search.getAgent();
        
		if (agent != null) {
			try {

				String soParentGuid = sa.getAtribute(SearchAttributes.STEWARTORDERS_PARENT_ORDER_GUID);
				if (org.apache.commons.lang.StringUtils.isNotBlank(soParentGuid)) {
					List<SearchUpdateMapper> searches = DBManager.getSearchIdsForUpdate(
							soParentGuid,
							ca.getID().intValue(),
							sa.getAtribute(SearchAttributes.P_COUNTY));

					for (SearchUpdateMapper mapper : searches) {
						if (!mapper.isUpdate()) {
							initialProductType = getProductType(mapper.getSearchType(), ca);
							break;
						}
					}
				}

				if (org.apache.commons.lang.StringUtils.isBlank(initialProductType)) {
					List<SearchUpdateMapper> searches = DBManager.getSearchIdsForUpdate(
							"___-" + sa.getAtribute(SearchAttributes.ABSTRACTOR_FILENO).replaceAll("[_]*", "") + "\\_",
							ca.getID().intValue(),
							sa.getAtribute(SearchAttributes.P_COUNTY),
							sa.getAtribute(SearchAttributes.ORDERBY_FILENO));

					if (searches != null) {
						for (SearchUpdateMapper mapper : searches) {
							if (!mapper.isUpdate()) {
								initialProductType = getProductType(mapper.getSearchType(), ca);
								break;
							}
						}
					}
				}
			} catch (Exception e) {
				logger.error("Something happened while loading initialProductType", e);
			}
		}
        
        int nr=-1;
         try{
 	        nr=Integer.parseInt(sa.getAtribute(SearchAttributes.SEARCH_PRODUCT));
         }catch (Exception e) {}
        
        boolean isCA = "CA".equals( currentState.getStateAbv() );
        double TSDsearchFee = Double.MIN_VALUE;
        TSDsearchFee = calculateTSDSearchFee(ca,currentCounty,search,nr);
        
        templateParams.put(AddDocsTemplates.orderStreetName, sa.getAtribute(SearchAttributes.P_ORDER_STREETNAME));
        templateParams.put(AddDocsTemplates.productType, productType);
        templateParams.put(AddDocsTemplates.initialProductType, initialProductType);
        
        templateParams.put(AddDocsTemplates.searchFee, new Double(TSDsearchFee));
        templateParams.put(AddDocsTemplates.TSDsearchFee, new Double(TSDsearchFee));
        
        templateParams.put(AddDocsTemplates.has_buyer, has_buyer);
        templateParams.put(AddDocsTemplates.has_lender, has_lender);
        templateParams.put(AddDocsTemplates.searchWasReopen, search.getSa().isReopenSearch());        
        templateParams.put(AddDocsTemplates.has_bookPage, has_bookPage);
        
        if(fillAtsExceptionTag){
        	templateParams.put(AddDocsTemplates.atsFileExceptions, calculateATSFileExceptions(search.getID()));
        }else{
        	templateParams.put(AddDocsTemplates.atsFileExceptions, "*");
        }
        templateParams.put(AddDocsTemplates.atsFileRequirements, "*");
        
       	byte[] tmpName = DBManager.getSearchOrderLogs(search.getID(), FileServlet.VIEW_ORDER, false);
       	if (tmpName != null){
       		String searchOrder = new String(tmpName);
       		searchOrder = searchOrder.replaceAll("(?ism)<input.*?type=\"button\".*?>","");
       		searchOrder = searchOrder.replaceFirst("(?ism).*<body.*?>(.*?)</body>.*","$1");
       		templateParams.put(AddDocsTemplates.htmlOrder, searchOrder);
       	}
       	
       	try {
	       	UserAttributes userAttr = InstanceManager.getManager().getCurrentInstance(search.getID()).getCurrentUser();
			String searchPage = MailTemplateUtils.fillAndSaveSearchOrder(search, search.getSa(), userAttr, true, null);
			searchPage = MailTemplateUtils.cleanSearchPageForTemplates(searchPage);
			
			if(StringUtils.isNotEmpty(searchPage)) {
				templateParams.put(AddDocsTemplates.searchPage, searchPage);
			} else {
				templateParams.put(AddDocsTemplates.searchPage, "");
			}
		} catch (Exception e) {
			templateParams.put(AddDocsTemplates.searchPage, "");
			e.printStackTrace();
		}

        TaxUtilsNew countyTax = new TaxUtilsNew(search.getID() , DType.TAX, currentState.getStateAbv() , currentCounty.getName(), null/*choose first checked*/);        
        double 	countyTaxReq	 		= 	countyTax.getBaseAmount();
        Date 	countyDueDate 			= 	countyTax.getDueDate();
        int 	countyStatus 			= 	countyTax.getStatus();
        Date 	countyPayDate 			=	countyTax.getPayDate();
        double  countyFoundDelinquent 	=	countyTax.getFoundDelinguent();
        double 	copuntyDue				= 	countyTax.getAmountDue();
        int 	countyTaxYear 			=	countyTax.getCurrentTaxYear();
        int 	countyTaxFoundYear		= 	countyTax.getFoundYear();
        double 	countyTaxPaid			= 	countyTax.getAmountPaid();
        int 	countyTaxVolume 		=	countyTax.getCountyTaxVolume();
        double 	countyTaxDeliq			= 	countyTax.getTotalDelinquent();
        int		installmentsSize 		= 	countyTax.getInstalmentsSize();
        String  taxAreaCode				= 	countyTax.getAreaCode();
        String  taxAreaName				=	countyTax.getAreaName();
        String  taxSaleNo				=	countyTax.getSaleNo();
        String	taxSaleDate				=	countyTax.getSaleDate();
		int    	boundSize 				= 	countyTax.getBondsSize(); 
		int 	priorYearsSize 			= 	countyTax.getPriorYearsSize();
		String taxYearFormatedForCA     = 	countyTax.getTaxYearDescription();
		double taxExemption 			= 	countyTax.getExemptionAmount();
		double assessedValueLand		= 	countyTax.getAssessedValueLand();
		double assessedValueImprovements= 	countyTax.getAssessedValueImprovements();
		int		installmentsSASize 		= 	countyTax.getSpecialAssessmentInstallmentsSize();
		boolean	hasCountyHomesteadExemption 	= 	false;
		TaxDocumentI taxDocumentI = countyTax.getDoc();
		
		String districtNo = "";
		String districtCode = "";
		if (taxDocumentI!=null) {
			Set<PropertyI> prop = taxDocumentI.getProperties();
			if (prop!=null) {
				for (PropertyI propertyI : prop) {
					districtNo = propertyI.getDistrict();
					if (StringUtils.isNotEmpty(districtNo)) break;
				}
				if (StringUtils.isNotEmpty(districtNo))
					districtCode = DBManager.getNVClarkDistrictName(districtNo);
				
				if (districtCode==null) 
					districtCode = "";
			}
		}
		templateParams.put(AddDocsTemplates.districtNo, districtNo);
		templateParams.put(AddDocsTemplates.districtCode, districtCode);
		
		String billNumber = taxDocumentI == null ?  "" :(org.apache.commons.lang.StringUtils.defaultIfEmpty(taxDocumentI.getBillNumber(),""));
		
		String delinquentFirstYear 		=  	"" ;
		if ( priorYearsSize>0 ){
			delinquentFirstYear = countyTax.getPriorYear(0).getTaxPeriod() ;
		}
		String taxAssesmentNo = countyTax.getParcelId();
		
		if( boundSize > 0  ){
			BondAndAssessmentI bond = countyTax.getBond(0);
			String  boundRecordedDate = bond.getRecordedDate();
			if(isCA){
				boundRecordedDate  = getDateFormatedForCA(boundRecordedDate);
			}
			templateParams.put(AddDocsTemplates.boundRecordedDate, boundRecordedDate);
	        templateParams.put(AddDocsTemplates.boundCityDistrict, bond.getCityDistrict());
	        templateParams.put(AddDocsTemplates.boundEntityNumber, bond.getEntityNo());
	        templateParams.put(AddDocsTemplates.boundTreasurerSerDist, bond.getTreasurerSerDist());
	        templateParams.put(AddDocsTemplates.boundImprovementOf, bond.getImprovementOf());
		}
		
		templateParams.put(AddDocsTemplates.assessedValueLand, assessedValueLand );
		templateParams.put(AddDocsTemplates.landValue, assessedValueLand );
		templateParams.put(AddDocsTemplates.assessedValueImprovements, assessedValueImprovements );
		templateParams.put(AddDocsTemplates.improvementsValue, assessedValueImprovements );
		templateParams.put(AddDocsTemplates.taxExemption, taxExemption );
		templateParams.put(AddDocsTemplates.delinquentFirstYear, delinquentFirstYear );
		templateParams.put(AddDocsTemplates.taxYearFormatedForCA, taxYearFormatedForCA );
        templateParams.put(AddDocsTemplates.taxAreaCode, taxAreaCode);
        templateParams.put(AddDocsTemplates.taxAreaName, taxAreaName);
        templateParams.put(AddDocsTemplates.taxSaleNo, taxSaleNo);
        templateParams.put(AddDocsTemplates.taxSaleDate, taxSaleDate);
        if(	taxSaleDate!=null && taxSaleDate.length()>=4){
        	templateParams.put( AddDocsTemplates.taxYearOfSale, taxSaleDate.substring( taxSaleDate.length()-4, taxSaleDate.length() ));
        }
        templateParams.put(AddDocsTemplates.countyTaxVolume, countyTaxVolume);        
        templateParams.put(AddDocsTemplates.countyTaxReq	,countyTaxReq);
        String countyDueDateStr = sdfMMddyyyy.format(countyDueDate);
        if(isCA){
        	countyDueDateStr = getDateFormatedForCA(countyDueDateStr);
        }
        templateParams.put(AddDocsTemplates.countyDueDate	, countyDueDateStr);
        templateParams.put(AddDocsTemplates.countyDueDateInt , countyDueDate.getTime());
        String countyPayDateStr = sdfMMddyyyy.format(countyPayDate);
        if(isCA){
        	countyPayDateStr = getDateFormatedForCA(countyPayDateStr);
        }
        templateParams.put(AddDocsTemplates.countyPayDate , countyPayDateStr); 
		templateParams.put(AddDocsTemplates.countyPayDateInt , countyPayDate.getTime());
		templateParams.put(AddDocsTemplates.countyTaxFoundDeliq , countyFoundDelinquent );
		templateParams.put(AddDocsTemplates.countyTaxDeliq,countyTaxDeliq);
		templateParams.put(AddDocsTemplates.taxBillNumber, billNumber);
		templateParams.put(AddDocsTemplates.countyTaxDue,    copuntyDue );
		templateParams.put(AddDocsTemplates.countyTaxYear, countyTaxYear );
	    templateParams.put(AddDocsTemplates.countyTaxFoundYear, countyTaxFoundYear ); 
        templateParams.put(AddDocsTemplates.countyTaxPaid,  countyTaxPaid);
        templateParams.put(AddDocsTemplates.countyDueStatus ,countyStatus);
        templateParams.put(AddDocsTemplates.has_CountyTax, (!(countyStatus==TaxUtilsNew.TAX_NOT_APLICABE))+"");
        templateParams.put(AddDocsTemplates.taxAssesmentNo, taxAssesmentNo); 
        
        if( installmentsSize > 0 ){
        	InstallmentI inst = countyTax.getInstalment(0);
        	templateParams.put( AddDocsTemplates.countyTaxFoundYear1, inst.getYearDescription() );
	        templateParams.put( AddDocsTemplates.countyBaseAmount1, inst.getBaseAmount() );
	        templateParams.put( AddDocsTemplates.countyAmountDue1,  inst.getAmountDue() );
	        templateParams.put( AddDocsTemplates.countyAmountPaid1, inst.getAmountPaid() );
	        templateParams.put( AddDocsTemplates.countyPenaltyAmount1, inst.getPenaltyAmount() );
	        templateParams.put( AddDocsTemplates.countyInstallmentStatus1, inst.getStatus() );
	        
	        double d = inst.getHomesteadExemption();
	        templateParams.put( AddDocsTemplates.countyHomesteadExemption1,  d);
	        if (d>0.0d) {
	        	hasCountyHomesteadExemption = true;
	        }
	        
	        if("PAID".equals(inst.getStatus())) {
	        	templateParams.put( AddDocsTemplates.countyAmountPaidOrDue1, inst.getAmountPaid() );
	        } else {
	        	templateParams.put( AddDocsTemplates.countyAmountPaidOrDue1, inst.getAmountDue() );
	        }
        } else {
        	templateParams.put( AddDocsTemplates.countyTaxFoundYear1, "" );
	        templateParams.put( AddDocsTemplates.countyBaseAmount1, 0d );
	        templateParams.put( AddDocsTemplates.countyAmountDue1,  0d );
	        templateParams.put( AddDocsTemplates.countyAmountPaid1, 0d );
	        templateParams.put( AddDocsTemplates.countyPenaltyAmount1, 0d );
	        templateParams.put( AddDocsTemplates.countyInstallmentStatus1, "" );
	        templateParams.put( AddDocsTemplates.countyAmountPaidOrDue1, 0d );
        }
        
        if( installmentsSize > 1 ){
        	InstallmentI inst = countyTax.getInstalment(1);
        	templateParams.put(AddDocsTemplates.countyTaxFoundYear2, inst.getYearDescription() );
	        templateParams.put( AddDocsTemplates.countyBaseAmount2, inst.getBaseAmount() );
	        templateParams.put( AddDocsTemplates.countyAmountDue2,  inst.getAmountDue() );
	        templateParams.put( AddDocsTemplates.countyAmountPaid2, inst.getAmountPaid() );
	        templateParams.put( AddDocsTemplates.countyPenaltyAmount2, inst.getPenaltyAmount() );
	        templateParams.put( AddDocsTemplates.countyInstallmentStatus2, inst.getStatus() );
	        
	        double d = inst.getHomesteadExemption();
	        templateParams.put( AddDocsTemplates.countyHomesteadExemption2,  d);
	        if (d>0.0d) {
	        	hasCountyHomesteadExemption = true;
	        }
	        
	        if("PAID".equals(inst.getStatus())) {
	        	templateParams.put( AddDocsTemplates.countyAmountPaidOrDue2, inst.getAmountPaid() );
	        } else {
	        	templateParams.put( AddDocsTemplates.countyAmountPaidOrDue2, inst.getAmountDue() );
	        }
        } else {
        	templateParams.put( AddDocsTemplates.countyTaxFoundYear2, "" );
	        templateParams.put( AddDocsTemplates.countyBaseAmount2, 0d );
	        templateParams.put( AddDocsTemplates.countyAmountDue2,  0d );
	        templateParams.put( AddDocsTemplates.countyAmountPaid2, 0d );
	        templateParams.put( AddDocsTemplates.countyPenaltyAmount2, 0d );
	        templateParams.put( AddDocsTemplates.countyInstallmentStatus2, "" );
	        templateParams.put( AddDocsTemplates.countyAmountPaidOrDue2, 0d );
        }
        
        if( installmentsSize > 2 ){
        	InstallmentI inst = countyTax.getInstalment(2);
        	templateParams.put( AddDocsTemplates.countyTaxFoundYear3, inst.getYearDescription() );
	        templateParams.put( AddDocsTemplates.countyBaseAmount3, inst.getBaseAmount() );
	        templateParams.put( AddDocsTemplates.countyAmountDue3,  inst.getAmountDue() );
	        templateParams.put( AddDocsTemplates.countyAmountPaid3, inst.getAmountPaid() );
	        templateParams.put( AddDocsTemplates.countyPenaltyAmount3, inst.getPenaltyAmount() );
	        templateParams.put( AddDocsTemplates.countyInstallmentStatus3, inst.getStatus() );
	        
	        double d = inst.getHomesteadExemption();
	        templateParams.put( AddDocsTemplates.countyHomesteadExemption3,  d);
	        if (d>0.0d) {
	        	hasCountyHomesteadExemption = true;
	        }
	        
	        if("PAID".equals(inst.getStatus())) {
	        	templateParams.put( AddDocsTemplates.countyAmountPaidOrDue3, inst.getAmountPaid() );
	        } else {
	        	templateParams.put( AddDocsTemplates.countyAmountPaidOrDue3, inst.getAmountDue() );
	        }
        } else {
        	templateParams.put( AddDocsTemplates.countyTaxFoundYear3, "" );
	        templateParams.put( AddDocsTemplates.countyBaseAmount3, 0d );
	        templateParams.put( AddDocsTemplates.countyAmountDue3,  0d );
	        templateParams.put( AddDocsTemplates.countyAmountPaid3, 0d );
	        templateParams.put( AddDocsTemplates.countyPenaltyAmount3, 0d );
	        templateParams.put( AddDocsTemplates.countyInstallmentStatus3, "" );
	        templateParams.put( AddDocsTemplates.countyAmountPaidOrDue3, 0d );  
        }
        
        if( installmentsSize > 3 ){
        	InstallmentI inst = countyTax.getInstalment(3);
        	templateParams.put( AddDocsTemplates.countyTaxFoundYear4, inst.getYearDescription() );
	        templateParams.put( AddDocsTemplates.countyBaseAmount4, inst.getBaseAmount() );
	        templateParams.put( AddDocsTemplates.countyAmountDue4,  inst.getAmountDue() );
	        templateParams.put( AddDocsTemplates.countyAmountPaid4, inst.getAmountPaid() );
	        templateParams.put( AddDocsTemplates.countyPenaltyAmount4, inst.getPenaltyAmount() );
	        templateParams.put( AddDocsTemplates.countyInstallmentStatus4, inst.getStatus() );
	        
	        double d = inst.getHomesteadExemption();
	        templateParams.put( AddDocsTemplates.countyHomesteadExemption4,  d);
	        if (d>0.0d) {
	        	hasCountyHomesteadExemption = true;
	        }
	        
	        if("PAID".equals(inst.getStatus())) {
	        	templateParams.put( AddDocsTemplates.countyAmountPaidOrDue4, inst.getAmountPaid() );
	        } else {
	        	templateParams.put( AddDocsTemplates.countyAmountPaidOrDue4, inst.getAmountDue() );
	        }
        } else {
        	templateParams.put( AddDocsTemplates.countyTaxFoundYear4, "" );
	        templateParams.put( AddDocsTemplates.countyBaseAmount4, 0d );
	        templateParams.put( AddDocsTemplates.countyAmountDue4,  0d );
	        templateParams.put( AddDocsTemplates.countyAmountPaid4, 0d );
	        templateParams.put( AddDocsTemplates.countyPenaltyAmount4, 0d );
	        templateParams.put( AddDocsTemplates.countyInstallmentStatus4, "" );
	        templateParams.put( AddDocsTemplates.countyAmountPaidOrDue4, 0d );  
        }

        if (installmentsSASize > 0){
        	InstallmentI inst = countyTax.getSpecialAssessmentInstalment(0);
	        templateParams.put(AddDocsTemplates.countySABaseAmount1, 		inst.getBaseAmount());
	        templateParams.put(AddDocsTemplates.countySAAmountDue1,  		inst.getAmountDue());
	        templateParams.put(AddDocsTemplates.countySAAmountPaid1, 		inst.getAmountPaid());
	        templateParams.put(AddDocsTemplates.countySAPenaltyAmount1, 	inst.getPenaltyAmount());
	        templateParams.put(AddDocsTemplates.countySAInstallmentStatus1, inst.getStatus());
        } else {
        	templateParams.put(AddDocsTemplates.countySABaseAmount1, 		0d);
	        templateParams.put(AddDocsTemplates.countySAAmountDue1,  		0d);
	        templateParams.put(AddDocsTemplates.countySAAmountPaid1, 		0d);
	        templateParams.put(AddDocsTemplates.countySAPenaltyAmount1, 	0d);
	        templateParams.put(AddDocsTemplates.countySAInstallmentStatus1, "");
        }
        if (installmentsSASize > 1){
        	InstallmentI inst = countyTax.getSpecialAssessmentInstalment(1);
	        templateParams.put(AddDocsTemplates.countySABaseAmount2, 		inst.getBaseAmount());
	        templateParams.put(AddDocsTemplates.countySAAmountDue2,  		inst.getAmountDue());
	        templateParams.put(AddDocsTemplates.countySAAmountPaid2, 		inst.getAmountPaid());
	        templateParams.put(AddDocsTemplates.countySAPenaltyAmount2, 	inst.getPenaltyAmount());
	        templateParams.put(AddDocsTemplates.countySAInstallmentStatus2, inst.getStatus());
        } else {
        	templateParams.put(AddDocsTemplates.countySABaseAmount2, 		0d);
	        templateParams.put(AddDocsTemplates.countySAAmountDue2,  		0d);
	        templateParams.put(AddDocsTemplates.countySAAmountPaid2, 		0d);
	        templateParams.put(AddDocsTemplates.countySAPenaltyAmount2, 	0d);
	        templateParams.put(AddDocsTemplates.countySAInstallmentStatus2, "");
        }
        
        templateParams.put(AddDocsTemplates.countyLastReceiptDate, getLastReceiptDate(search,isCA, false));
        templateParams.put(AddDocsTemplates.cityLastReceiptDate, getLastReceiptDate(search,isCA, true));
        templateParams.put(AddDocsTemplates.hasCountyHomesteadExemption, hasCountyHomesteadExemption);

      //B3285
       	boolean interpretTRDocAsEP = false;
       	if ("TN".equals(currentState.getStateAbv()) && "Shelby".equals(currentCounty.getName())){
       		interpretTRDocAsEP = true;
       	}
       	
        TaxUtilsNew cityTax = new TaxUtilsNew(search.getID() , DType.CITYTAX , currentState.getStateAbv() , currentCounty.getName(), null);    
        
        if (cityTax.getDoc() == null && taxDocumentI != null){
        	cityTax = new TaxUtilsNew(search.getID() , DType.CITYTAX , currentState.getStateAbv() , currentCounty.getName(), 
        			taxDocumentI.getAdditionalCityDoc(), interpretTRDocAsEP);    
        }
        
        double 	cityTaxReq	 			= 	cityTax.getBaseAmount();
        Date 	cityDueDate 			= 	cityTax.getDueDate();
        int 	cityStatus 				= 	cityTax.getStatus();
        Date 	cityPayDate 			=	cityTax.getPayDate();
        double  cityFoundDelinquent 	=	cityTax.getFoundDelinguent();
        double 	cityDue					= 	cityTax.getAmountDue();
        int 	cityTaxYear 			=	cityTax.getCurrentTaxYear();
        int 	cityTaxFoundYear		= 	cityTax.getFoundYear();
        double 	cityTaxPaid				= 	cityTax.getAmountPaid();
        double  cityTaxDeliq			=   cityTax.getTotalDelinquent();
                
        templateParams.put(AddDocsTemplates.cityTaxReq, cityTaxReq);
        String cityDueDateStr = sdfMMddyyyy.format(cityDueDate);
        if( isCA ){
        	cityDueDateStr = getDateFormatedForCA(cityDueDateStr );
        }
        templateParams.put(AddDocsTemplates.cityDueDate, cityDueDateStr );
        templateParams.put(AddDocsTemplates.cityDueDateInt, cityDueDate.getTime());
        templateParams.put(AddDocsTemplates.cityDueStatus, cityStatus);
        String cityPayDateStr = sdfMMddyyyy.format(cityPayDate);
        if(isCA){
        	cityPayDateStr  = getDateFormatedForCA(cityPayDateStr);
        }
        if(cityTaxFoundYear == -1) cityTaxFoundYear = Integer.MIN_VALUE;
        templateParams.put(AddDocsTemplates.cityPayDate, cityPayDateStr);
		templateParams.put(AddDocsTemplates.cityPayDateInt , cityPayDate.getTime());
        templateParams.put(AddDocsTemplates.cityTaxYear, cityTaxYear);
        templateParams.put(AddDocsTemplates.cityTaxFoundYear, cityTaxFoundYear );
        templateParams.put(AddDocsTemplates.cityTaxFoundDeliq,cityFoundDelinquent);
        templateParams.put(AddDocsTemplates.cityTaxDeliq,cityTaxDeliq);
        templateParams.put(AddDocsTemplates.cityTaxPaid, cityTaxPaid );
		templateParams.put(AddDocsTemplates.cityTaxDue, cityDue );
		
		
		
		
		
		String spacedLD_PARCEL = StringUtils.insertSpaceToSplit(search.getSa().getAtribute(SearchAttributes.LD_PARCELNO), ";,", 50);
        templateParams.put(AddDocsTemplates.parcelId, spacedLD_PARCEL);
        templateParams.put(AddDocsTemplates.has_CityTax, (!(cityStatus==TaxUtilsNew.TAX_NOT_APLICABE))+"");
        
        
        String city = search.getSa().getAtribute(SearchAttributes.P_CITY);
        if (city == null){
        	city = "";
        }
        
        templateParams.put(AddDocsTemplates.city, StringUtils.formatString(city,addressFormat));
        templateParams.put(AddDocsTemplates.cityTitleCase, StringUtils.toTitleCase(city));
        
        String communityDesc= ca.getDESCRIPTION();
        
        
        String communityAditionalInfo  = ca.getSEE_ALSO();
        templateParams.put(AddDocsTemplates.communityAditionalInfo,(communityAditionalInfo==null)?"":communityAditionalInfo );
        
        FidelityTSD.writeLogo(search.getSearchID());
        String communityImageLink = makeImageCommunityLink(search);
        String communityPhone=ca.getPHONE();
        String communityAddress=ca.getADDRESS();
        
        if(communityDesc==null){
        	communityDesc="";
        }
        if(communityPhone==null){
        	communityPhone="";
        }
        if(communityAddress==null){
        	communityAddress="";
        }
        
        templateParams.put(AddDocsTemplates.communityImageLink, communityImageLink);
        templateParams.put(AddDocsTemplates.communityDesc, communityDesc);
        templateParams.put(AddDocsTemplates.communityPhone, communityPhone);
        templateParams.put(AddDocsTemplates.communityAddress, communityAddress);
        
        templateParams.put(AddDocsTemplates.fileNo, search.getSa().getAtribute(SearchAttributes.ORDERBY_FILENO));
        templateParams.put(AddDocsTemplates.abstrFileNo, search.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO));
        templateParams.put(AddDocsTemplates.searchId, String.valueOf(search.getID()));
        templateParams.put(AddDocsTemplates.additionalLenderLanguage, search.getSa().getAtribute(SearchAttributes.ADDITIONAL_LENDER_LANGUAGE));
        templateParams.put(AddDocsTemplates.abstractorEmail, search.getSa().getAtribute(SearchAttributes.ABSTRACTOR_EMAIL));
        
        templateParams.put(AddDocsTemplates.agentOrderDate,sdfMMddyyyy.format(search.getStartDate()));
        
        InstanceManager.getManager().getCurrentInstance(search.getID());
        
        templateParams.put(AddDocsTemplates.soOrderProductId, sa.getAtribute(SearchAttributes.STEWARTORDERS_ORDER_PRODUCT_ID));
        templateParams.put(AddDocsTemplates.agentUser, "$AGENT_USER$");
        templateParams.put(AddDocsTemplates.agentPassword, "$AGENT_PASSWORD$");
        
        templateParams.put(AddDocsTemplates.taxDocuments, "");
        templateParams.put(AddDocsTemplates.amountsToRedeemFor, "");
        
        String defaultStr="";
            	
    	if (agent!=null){
    		templateParams.put(AddDocsTemplates.agentWPhone, org.apache.commons.lang.StringUtils.defaultIfEmpty(agent.getPHONE(), defaultStr));
    		templateParams.put(AddDocsTemplates.agentFax, org.apache.commons.lang.StringUtils.defaultIfEmpty(agent.getALTPHONE(), defaultStr));
    		if (UserAttributes.TIFF_TYPE.equalsIgnoreCase(agent.getDISTRIBUTION_TYPE())){
            	templateParams.put(AddDocsTemplates.viewerLink, "Pdf Viewer&nbsp;&nbsp;http://get.adobe.com/reader/&nbsp;&nbsp;&nbsp;Tiff Viewer&nbsp;http://www.alternatiff.com/</br>" );
            } else if (UserAttributes.PDF_TYPE.equalsIgnoreCase(agent.getDISTRIBUTION_TYPE())){
            	templateParams.put(AddDocsTemplates.viewerLink, "<a href=\"http://get.adobe.com/reader/\">Pdf Viewer</a>&nbsp;&nbsp;<a href=\"http://www.alternatiff.com/\">Tiff Viewer</a></br>" );
            }
    		
    		templateParams.put(AddDocsTemplates.agentEmail, org.apache.commons.lang.StringUtils.defaultString(agent.getEMAIL()));   
    	}
    	
    	try{
	    	Hashtable agentInfo = UserUtils.getUserInfo(agent);
	    	templateParams.put(AddDocsTemplates.agentOperatingAccID, (agentInfo==null)?"":agentInfo.get(UserAttributes.USER_WCARD_ID)); 
	    	templateParams.put(AddDocsTemplates.agentPersonalID, (agentInfo==null)?"":agentInfo.get(UserAttributes.USER_PCARD_ID)); 
    	}
    	catch(Exception e){
    		templateParams.put(AddDocsTemplates.agentPersonalID, ""); 
    		templateParams.put(AddDocsTemplates.agentOperatingAccID, "" ); 
    	}
    	
        
        
        setAbstractorTags(search, templateParams);
        
        String assessedValue=search.getSa().getAtribute(SearchAttributes.ASSESSED_VALUE).replaceAll("[ $]", "");
        if(assessedValue==null || "0".equals(assessedValue)){
        	assessedValue = defaultValue;
        }
        try {
        	NumberFormat nf = NumberFormat.getInstance(Locale.US); 
        	nf.setGroupingUsed(true);
        	nf.setMinimumFractionDigits(2);
        	nf.setMaximumFractionDigits(2);
        	assessedValue = nf.format(Double.parseDouble(assessedValue));
        }catch(Exception ignored) {}
        templateParams.put(AddDocsTemplates.assessedValue, assessedValue);
        String needByDate = DBManager.getNeedByDate(search);
        if(isCA){
        	needByDate  = getDateFormatedForCA(needByDate);
        }
        templateParams.put(AddDocsTemplates.needByDate,needByDate);
        
        String quarterOrder = search.getSa().getAtribute(SearchAttributes.QUARTER_ORDER);
        templateParams.put(AddDocsTemplates.quarterOrder,quarterOrder!=null ?quarterOrder :"");
        
        String township = search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_TWN);
        templateParams.put(AddDocsTemplates.township,township!=null?township :"");
        
        String range = search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_RNG);
        templateParams.put(AddDocsTemplates.range,range!=null?range :"");
        
        
        int ownerAmountInt=Integer.MIN_VALUE;
        try{
        	String str=search.getSa().getAtribute(SearchAttributes.BM2_LOADACCOUNTNO).replaceAll("[ ,$]", "");
        	
        	if(str!=null)
        		if(!str.equals("") && !str.equals("TBD")){
        			try{
        			str=str.substring(0, str.indexOf("."));
        			}
        			catch(Exception e){
        				logger.error("ownerAmountInt= " + str+ " doesn't contains dot (.) character , could be wrong" );
        			}
        			ownerAmountInt=Integer.parseInt(str);
        		}
        }
        catch(NumberFormatException e){
        	e.printStackTrace();
        	logger.error(e.getCause() +"\n"+ e.getMessage());
        }
       
        double ownerAmount =Double.MIN_VALUE ;
        try{
        	String str=search.getSa().getAtribute(SearchAttributes.BM2_LOADACCOUNTNO).trim().replaceAll("[, $]","");
        	if(str!=null)if(!str.equals(""))
        		ownerAmount = Double.valueOf(str).doubleValue();
        }
        catch(NumberFormatException e){e.printStackTrace();}
      
        
        templateParams.put(AddDocsTemplates.ownerAmount, new Double(ownerAmount));
        templateParams.put(AddDocsTemplates.ownerAmountInt, new Integer(ownerAmountInt));
        
        java.text.SimpleDateFormat smartSdf = TemplateBuilder.getSimpleDateFormatForTemplates(currentState.getStateId().intValue());

        int lenderAmountInt=Integer.MIN_VALUE;
        try{
        	String str=search.getSa().getAtribute(SearchAttributes.BM1_LOADACCOUNTNO).trim();
        	if(str!=null)if(!str.equals("") && !str.equals("TBD")){
        		str = str.replaceAll("[, $]", "");
        		try{
        		str = str.substring(0, str.indexOf("."));
        		}
        		catch (Exception e){
        			logger.error("ownerAmountInt= " + str+ " doesn't contains dot (.) character , could be wrong" );
        		}
        		lenderAmountInt=Integer.parseInt(str);
        	}
        }catch(NumberFormatException ignored){/*e.printStackTrace();*/}
       
        double lenderAmount =Double.MIN_VALUE;
        try{String str=search.getSa().getAtribute(SearchAttributes.BM1_LOADACCOUNTNO).trim().replaceAll("[, $]","");
        	if(str!=null)if(!str.equals("")){
        		lenderAmount = Double.valueOf(str).doubleValue();
        	}
        }catch(NumberFormatException e){
        	/*e.printStackTrace();*/
        }

        templateParams.put(AddDocsTemplates.lenderAmount, new Double(lenderAmount));
        
        templateParams.put(AddDocsTemplates.lenderAmountInt, new Integer(lenderAmountInt));
        templateParams.put(AddDocsTemplates.lender, search.getSa().getAtribute(SearchAttributes.BM1_LENDERNAME).replaceAll("\n", ""));
        templateParams.put(AddDocsTemplates.payRate, getSearchFee(search));
        
		templateParams.put(AddDocsTemplates.streetDirPrefix, search.getSa().getAtribute(SearchAttributes.P_STREETDIRECTION));
		templateParams.put(AddDocsTemplates.streetDirSuffix, search.getSa().getAtribute(SearchAttributes.P_STREET_POST_DIRECTION));
		templateParams.put(AddDocsTemplates.streetName, StringUtils.formatString(search.getSa().getAtribute(SearchAttributes.P_STREETNAME),addressFormat));
		templateParams.put(AddDocsTemplates.streetNum, StringUtils.formatString(search.getSa().getAtribute(SearchAttributes.P_STREETNO),addressFormat));
		templateParams.put(AddDocsTemplates.streetSuffix, StringUtils.formatString(search.getSa().getAtribute(SearchAttributes.P_STREETSUFIX),addressFormat));
		templateParams.put(AddDocsTemplates.unitNumber, StringUtils.formatString(search.getSa().getAtribute(SearchAttributes.P_STREETUNIT),addressFormat));
		templateParams.put(AddDocsTemplates.unitType, "");
		templateParams.put(AddDocsTemplates.zipcode, StringUtils.formatString(search.getSa().getAtribute(SearchAttributes.P_ZIP),addressFormat));
		
		String isCondo = search.getSa().getAtribute(SearchAttributes.IS_CONDO);
		if(StringUtils.isEmpty(isCondo)){
			isCondo = "false";
		}
		templateParams.put(AddDocsTemplates.isCondo, Boolean.parseBoolean(isCondo));
		
		String titleDeskOrderId = search.getSa().getAtribute(SearchAttributes.TITLEDESK_ORDER_ID);
		templateParams.put(AddDocsTemplates.titleDeskOrderID, titleDeskOrderId==null?"":titleDeskOrderId);
        templateParams.put(AddDocsTemplates.propAddress, StringUtils.formatString(FormatSa.getPropertyAddress(search.getSa()),addressFormat));
        String propertyType = sa.getAtribute(SearchAttributes.PROPERTY_TYPE);
		templateParams.put(AddDocsTemplates.propertyType, propertyType==null?"":propertyType);
        templateParams.put(AddDocsTemplates.agent, FormatSa.getAgentCompanyName(search.getSa()));
        templateParams.put(AddDocsTemplates.ssfAllImagesLink, "<a href=\""+ALL_IMAGES_LINK_CODE+"\">TSR Images</a>" ); 
               
        templateParams.put(AddDocsTemplates.tsrLink, "<a href=\""+TSR_LINK_CODE+"\">TSR file</a>"); 
        templateParams.put(AddDocsTemplates.tsriLink, "<a href=\""+TSRI_LINK_CODE+"\">TSR index file</a>");

        String lot = search.getSa().getAtribute(SearchAttributes.LD_LOTNO);
        if(lot==null) lot =defaultValue;
        lot = lot.replaceAll("(?>\\s+|^)0(?:\\s+|$)", "");
        templateParams.put(AddDocsTemplates.lot, replaceEmptyString(lot));
        
        String sublot = search.getSa().getAtribute(SearchAttributes.LD_SUBLOT);
        if(sublot==null) sublot =defaultValue;
        templateParams.put(AddDocsTemplates.sublot, replaceEmptyString(sublot));
        
        String lotLetters = ro.cst.tsearch.utils.NumberUtils.convertNumberToEnglishWordsList(lot, "\\s");
        templateParams.put(AddDocsTemplates.lotLetters, lotLetters);
        
        
        
        
        String legalOCR = org.apache.commons.lang.StringUtils.defaultIfEmpty(TsdIndexPageServer.getLegalDescriptionOCR(search).getLegal(), defaultStr);
        legalOCR = TemplateUtils.revertStringFromHtmlEncoding(legalOCR);
        templateParams.put(AddDocsTemplates.ocrLD, legalOCR);
        
        String otherResults = search.getSa().getAtribute(SearchAttributes.OTHER_RESULTS);
        if(StringUtils.isEmpty(otherResults)) {
        	templateParams.put(AddDocsTemplates.otherResults, "");	
        }else {
        	templateParams.put(AddDocsTemplates.otherResults, otherResults);
        }
        
        String additionalRequirements = search.getSa().getAtribute(SearchAttributes.ADDITIONAL_REQUIREMENTS);
       // additionalRequirements = AddDocsTemplates.cleanStringForAIM(additionalRequirements, false);
        templateParams.put(AddDocsTemplates.additionalRequirements, additionalRequirements);

        String additionalExceptions = search.getSa().getAtribute( SearchAttributes.ADDITIONAL_EXCEPTIONS );
       // additionalExceptions = AddDocsTemplates.cleanStringForAIM(additionalExceptions, false);
        templateParams.put( AddDocsTemplates.additionalExceptions, additionalExceptions );
        
        String additionalInformation = search.getSa().getAtribute( SearchAttributes.ADDITIONAL_INFORMATION );
       // additionalInformation = AddDocsTemplates.cleanStringForAIM(additionalInformation, false);
        templateParams.put( AddDocsTemplates.additionalInformation, additionalInformation );
        
        String county = cleanCountyName(StringUtils.formatString(currentCounty.getName(),addressFormat));
        templateParams.put(AddDocsTemplates.county, county);
        
//        templateParams.put(AddDocsTemplates.zipCode, StringUtils.formatString(search.getSa().getAtribute(SearchAttributes.P_ZIP),addressFormat));
        templateParams.put(AddDocsTemplates.year, new Integer(calendar.get(Calendar.YEAR)) );
                    
        putLastPlatTags(templateParams,search,smartSdf); 	
        
        templateParams.put(AddDocsTemplates.bookPage, 
        		ro.cst.tsearch.templates.TemplateUtils.getBookAndPage(search, DocumentTypes.PLAT, true));
        templateParams.put(AddDocsTemplates.state, StringUtils.formatString(currentState.getName(),addressFormat));
        templateParams.put(AddDocsTemplates.stateShort, currentState.getStateAbv().toUpperCase());
       
        templateParams.put(AddDocsTemplates.currentOwners, new Vector());
        templateParams.put(AddDocsTemplates.currentBuyers, new Vector());
        
        String certificationDate = "";
        
        if (sa.getCertificationDate().getDate() != null){
        	certificationDate = sdfMMddyyyy.format(sa.getCertificationDate().getDate());
        } else{
        	certificationDate = search.getSa().getAtribute(SearchAttributes.CERTICICATION_DATE);
        }
        
        String lastUpdateDate = defaultValue;
        String nowUpdateDate = defaultValue;
        int updateNO = Integer.MIN_VALUE;
        
        updateNO = calculateUpdateNO(search);
        lastUpdateDate = calculateLastUpdateDate(search);
        nowUpdateDate = calculateNowUpdateDate(search);
        if(isCA){
        	lastUpdateDate  = getDateFormatedForCA(lastUpdateDate);
        	nowUpdateDate = getDateFormatedForCA(nowUpdateDate);
        }
        templateParams.put(AddDocsTemplates.lastUpdateDate, lastUpdateDate);
        templateParams.put(AddDocsTemplates.nowUpdateDate, nowUpdateDate);
        templateParams.put(AddDocsTemplates.updateNO , updateNO );
        
        
        String certifDateMMddyyyy = "";
        String certificationDateStr = "";
        
        SimpleDateFormat longFormat = new SimpleDateFormat("MMMM d, yyyy");
        
        try {
        	DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
    		Date d = df.parse(certificationDate);
    		certifDateMMddyyyy = sdfMMddyyyy.format(d);
    		
    		
    		certificationDateStr = longFormat.format(d) + " @ 08:00 A.M.";
        } catch (Exception e) {}
        
        if(isCA){
        	if(!"".equals(certificationDateStr)){
        		certificationDateStr = getDateFormatedForCA(certificationDateStr);
        	}
        }

        templateParams.put(AddDocsTemplates.certificationDate, certificationDateStr);
        templateParams.put(AddDocsTemplates.effectiveEndDateString, certificationDateStr);
        templateParams.put(AddDocsTemplates.certificationDateMMDDYYYY, certificationDate.equals("") ? "" : (certifDateMMddyyyy + " 08:00 AM"));
        
        
        templateParams.put(AddDocsTemplates.certifDateFormat, certifDateMMddyyyy);
        if(!certifDateMMddyyyy.isEmpty())
        	templateParams.put(AddDocsTemplates.certifTimeFormat, "08:00 AM");
        long t13=Long.MIN_VALUE;
        try{
        	t13=Util.dateParser(certificationDate).getTime() ;
        }
        catch(Exception e){
        	e.printStackTrace();
        }
        templateParams.put(AddDocsTemplates.certificationDateInt,new Long(t13)); 
        
	    String tsrCreationDate;
	    if (!isCA) {
	    	tsrCreationDate = new SimpleDateFormat("MMM dd, yyyy").format(new Date());
	    } else {
	    	tsrCreationDate = new SimpleDateFormat("MMMMM dd, yyyy").format(new Date());
	    }
	    templateParams.put(AddDocsTemplates.commitmentDate, tsrCreationDate);
        
	    String goodThru ="";//sdf
	    Calendar cal = Calendar.getInstance();
	    cal.getActualMaximum(Calendar.MONTH);
	    cal.set(Calendar.DAY_OF_MONTH,cal.getActualMaximum(Calendar.DAY_OF_MONTH));
	    
	    Date date= cal.getTime();
	    goodThru  = sdfMMddyyyy.format(date);
	    
	    templateParams.put(AddDocsTemplates.goodThru, goodThru);
       
        boolean hasCityTaxDocument = false;

        try { 
        	
        	docManager.getAccess();
        	templateParams.put( AddDocsTemplates.hasCityTaxDocument, (hasCityTaxDocument=(docManager.getDocumentsWithType(true,DType.CITYTAX).size()>0) ) );
        	templateParams.put( AddDocsTemplates.hasCountyTaxDocument, docManager.getDocumentsWithType(true,DType.TAX).size()>0 );
        	
//        	templateParams.put(AddDocsTemplates.currentOwner, smallSpace);
        	
	        templateParams.put(AddDocsTemplates.hasEasements, docManager.getDocuments(true,"EASEMENT").size()>0);
	        templateParams.put(AddDocsTemplates.hasPlats, docManager.getDocuments(true,"PLAT").size()>0);
	        templateParams.put(AddDocsTemplates.isSO, PlaceOrderService.isStewartOrders(sa));
	        
	        templateParams.put(AddDocsTemplates.hasRestrictions, docManager.getDocuments(true,"RESTRICTION").size()>0);
	        templateParams.put(AddDocsTemplates.hasCcers, docManager.getDocuments(true,"CCER").size()>0);
	        templateParams.put(AddDocsTemplates.hasByLaws, docManager.getDocuments(true,"BY-LAWS","By-Laws").size()>0);
	        templateParams.put(AddDocsTemplates.hasJudgments, docManager.getDocuments(true,"COURT","JUDGMENT").size()>0);
	        templateParams.put(AddDocsTemplates.hasLiens, docManager.getDocuments(true,"LIEN").size()>0);
	        templateParams.put(AddDocsTemplates.hasMortgages, docManager.getDocuments(true, "MORTGAGE").size()>0);
	        
//	        TransferI lastRealTransfer = docManager.getLastRealTransfer();
//	        if(lastRealTransfer!=null) {
//	        	String granteeFreeFormLastRealTransfer = lastRealTransfer.getGranteeFreeForm();
//	        	if(!StringUtils.isStringBlank(granteeFreeFormLastRealTransfer)){
//	        		templateParams.put(AddDocsTemplates.currentOwner, nameFormat.setCase(granteeFreeFormLastRealTransfer));
//	        	}
//	        }else {
//	        	if (sa.hasOwner()) { 
//	        		templateParams.put(AddDocsTemplates.currentOwner, 
//	        				nameFormat.setCase(StringUtils.getOwnersAsString(sa, "<F> <Mi> <L>", -1)));
//	        	}
//	        }
	        
	        templateParams.put(AddDocsTemplates.ownerType, getOwnerType(sa));
	        
	        String vestingInfoGrantee = TsdIndexPageServer.getVestingInfo(search).getVesting();
	        vestingInfoGrantee = TemplateUtils.revertStringFromHtmlEncoding(vestingInfoGrantee);
	        String vestingInfoGrantor = getVestingGrantor(search).getVesting();
	        vestingInfoGrantor = TemplateUtils.revertStringFromHtmlEncoding(vestingInfoGrantor);
	        	
	        templateParams.put(AddDocsTemplates.vestingInfoGrantee,vestingInfoGrantee!=null?vestingInfoGrantee :"");
	        templateParams.put(AddDocsTemplates.vestingInfoGrantor,vestingInfoGrantor!=null?vestingInfoGrantor :"");

	        
	        String viewStartDateString = "*";
	        if(docManager.getStartViewDate() != null) {
	        	viewStartDateString = sdfMMddyyyy.format(docManager.getStartViewDate());
	        }
			templateParams.put(AddDocsTemplates.viewStartDateString, viewStartDateString);
			
			String viewEndDateString = "*";
	        if(docManager.getEndViewDate() != null) {
	        	viewEndDateString = sdfMMddyyyy.format(docManager.getEndViewDate());
	        }
			templateParams.put(AddDocsTemplates.viewEndDateString, viewEndDateString);
	        
	        
        }finally {
        	docManager.releaseAccess();
        }
        
        //fill yes/no tags task 7652
        fillYesNoTags(templateParams, search, docManager);
        
        boolean hasCityTax = false;
        List<DataSite> allSites = HashCountyToIndex.getAllSites(sa.getCommId(), Integer.valueOf(search.getCountyId()).intValue());
        
        for (DataSite dataSite : allSites) {
        	String searchPageCity = sa.getAtribute(SearchAttributes.P_CITY);
        	searchPageCity = searchPageCity.replaceAll("[^a-zA-Z\\s]+", "");
        	String searchSiteCity = dataSite.getCityName();
        	if(searchSiteCity!=null){
        		searchSiteCity = searchSiteCity.replaceAll("[^a-zA-Z\\s]+", "");
	        	if (searchPageCity.contains(searchSiteCity)){
	        		hasCityTax = true;
	        	}
        	}
		}
        templateParams.put(AddDocsTemplates.hasCityTax, hasCityTax||hasCityTaxDocument);
        
        String unit = search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_UNIT);
		templateParams.put(AddDocsTemplates.unit, 
        		 replaceEmptyString(unit));
        
        String unitLetters = ro.cst.tsearch.utils.NumberUtils.convertNumberToEnglishWordsList(unit, "\\s");
        templateParams.put(AddDocsTemplates.unitLetters, unitLetters);
        
        templateParams.put(AddDocsTemplates.section, 
                replaceEmptyString(search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_SEC)));
        String subdivision = search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_NAME);
       // if(subdivision !=null){
        //	subdivision = subdivision .replaceAll("&", "&amp;");
        //}
        if(subdivision == null) subdivision=defaultValue;
        templateParams.put(AddDocsTemplates.subdivision,replaceEmptyString(StringUtils.formatString(subdivision,legalFormat)) );
               
        String block = search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
		templateParams.put(AddDocsTemplates.block, 
        		replaceEmptyString(block));
		
        String blockLetters = ro.cst.tsearch.utils.NumberUtils.convertNumberToEnglishWordsList(block, "\\s");
        templateParams.put(AddDocsTemplates.blockLetters, blockLetters);
        
        templateParams.put(AddDocsTemplates.phase, 
        		replaceEmptyString(search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_PHASE)));
        templateParams.put(AddDocsTemplates.tract, 
        		replaceEmptyString(search.getSa().getAtribute(SearchAttributes.LD_SUBDIV_TRACT)));
                       
        String agentAddress =FormatSa.getAgentWorkAddressForCurrentSearch(search.getID());
        String agentCity =FormatSa.getAgentWorkCityForCurrentSearch(search.getID());
        String agentZip =FormatSa.getAgentWorkZipCodeForCurrentSearch(search.getID());
        String stateName =FormatSa.getAgentWorkStateForCurrentSearch(search.getID());
        
        GenericState allSates[]=DBManager.getAllStates(); 
        String agentStateAbbr="";
   
        if(stateName!=null){
        	if (stateName.length()>=2){
        		agentStateAbbr = stateName.substring(0,2);
        	}
        }
        else{
        	stateName="";
        }
        
        for(int pi=0;pi<allSates.length;pi++){
        	GenericState curentState=null;
        	if(stateName.equalsIgnoreCase((curentState=allSates[pi]).getName())){
        		agentStateAbbr = curentState.getStateAbv();
        	}
        }
        if(agentStateAbbr == null){
        	agentStateAbbr ="";
        }
        templateParams.put(AddDocsTemplates.agentCompany, FormatSa.getAgentCompanyName(search.getSa()));
        templateParams.put(AddDocsTemplates.agentAddress, agentAddress);
        templateParams.put(AddDocsTemplates.agentCity, agentCity);
        templateParams.put(AddDocsTemplates.agentZip , agentZip);
        templateParams.put(AddDocsTemplates.agentStateAbbr, agentStateAbbr);
        templateParams.put(AddDocsTemplates.agentState,stateName);
        
        templateParams.put(AddDocsTemplates.agentName, FormatSa.getAgentName(search.getSa()));
        
        Date currDate = calendar.getTime();
        String currDateString = sdfMMddyyyy.format(currDate);
        if( isCA ){
        	currDateString = getDateFormatedForCA(currDateString);
        }
        templateParams.put(AddDocsTemplates.currentDate, currDateString);
        
        Calendar calBegin = (Calendar )calendar.clone();
        
        int offset = SearchAttributes.YEARS_BACK;
        try {
        	offset = CountyCommunityManager.getInstance()
				.getCountyCommunityMapper(Integer.parseInt(sa.getCountyId()), sa.getCommId())
				.getDefaultStartDateOffset();
        } catch (Exception e) {
			logger.error("Error while trying to read official start offset", e);
		}
                
        calBegin.set(calBegin.get(Calendar.YEAR)-offset, calBegin.get(Calendar.MONTH), calBegin.get(Calendar.DATE));
        
        Date begginningDate = calBegin.getTime();
        
        String begginningDateStr="";
        
        begginningDateStr = sdfMMddyyyy.format(begginningDate );
        
        if(isCA){
        	begginningDateStr = getDateFormatedForCA(begginningDateStr);
        }
        
        templateParams.put(AddDocsTemplates.beginningDate, begginningDateStr);
        templateParams.put(AddDocsTemplates.officialStartDate, begginningDateStr);

        String effectiveStartDateString = org.apache.commons.lang.StringUtils.defaultString(sa.getEffectiveStartDateAsString(), "*");
		templateParams.put(AddDocsTemplates.effectiveStartDate, effectiveStartDateString);
		templateParams.put(AddDocsTemplates.effectiveStartDateString, effectiveStartDateString);

		sa.getCertificationDate().getDate();
		
        
			long lcurrentDate = calendar.getTime().getTime();

        templateParams.put(AddDocsTemplates.currentDateInt,new Long( lcurrentDate));
        templateParams.put(AddDocsTemplates.currentTime, 
                calendar.get(Calendar.HOUR_OF_DAY) + 
                ":" + 
                (calendar.get(Calendar.MINUTE) < 10 ? "0" : "") + 
                calendar.get(Calendar.MINUTE));
        
			templateParams.put(AddDocsTemplates.TSD_NewPage, getTSDNewPage());
			templateParams.put(AddDocsTemplates.TSD_Patriots, constructTSDPatriots(search));
			templateParams.put(AddDocsTemplates.TSD_StartInvoice, "<!-- START INVOICE -->");
			templateParams.put(AddDocsTemplates.TSD_EndInvoice, "<!-- END INVOICE -->");
        
			try {
				templateParams.put(AddDocsTemplates.searchProduct, search.getSearchProductTemplateTag());
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("Error filling searchProduct tag", e);
			}
        
        }
        
        String legalDesc = defaultValue;
        
        if(!doNotFillLegalDesc){
        	legalDesc = TsdIndexPageServer.getLegalDescription(search, templateParams).getLegal();
        	legalDesc = TemplateUtils.revertStringFromHtmlEncoding(legalDesc);
        }
        
        templateParams.put(AddDocsTemplates.legalDesc, legalDesc);
        
        return templateParams;
    }

	private static void fillYesNoTags(HashMap<String, Object> templateParams, Search search, DocumentsManagerI docManager) {
    	String YES ="YES";
    	String NO = "NO";
    	
    	try{
    		SearchAttributes sa = search.getSa();
    		
    		if(sa.getValidatedProperty() != null && sa.getValidatedProperty().hasLegal()){
    			templateParams.put(AddDocsTemplates.LEGAL_CORRECT_YN, YES);
    		} else {
    			templateParams.put(AddDocsTemplates.LEGAL_CORRECT_YN, NO);
    		}
    		
    		docManager.getAccess();
    		
    		//List<DocumentI> documents = docManager.getDocumentsWithDocType(true, "COUNTYTAX");
    		List<DocumentI> documents = docManager.getDocuments(true, "COURT", "Tax Suit"); //task 8144
    		
    		if(documents.size()>0){
    			templateParams.put(AddDocsTemplates.TAX_SUIT_YN, YES);
    		} else {
    			templateParams.put(AddDocsTemplates.TAX_SUIT_YN, NO);
    		}
    		
    		documents = docManager.getDocuments(true, "MISCELLANEOUS", "Death Certificate");
    		
    		if(documents.size()>0){
    			templateParams.put(AddDocsTemplates.DECEASED_DEBTOR_YN, YES);
    		} else {
    			templateParams.put(AddDocsTemplates.DECEASED_DEBTOR_YN, NO);
    		}
    		
    		documents = docManager.getDocuments(true, "COURT", "Bankruptcy", "Bky", "Bk", "By");
    		
    		if(documents.size()>0){
    			templateParams.put(AddDocsTemplates.BANKRUPTCY_YN, YES);
    		} else {
    			templateParams.put(AddDocsTemplates.BANKRUPTCY_YN, NO);
    		}
    		
    		documents = docManager.getDocuments(true, "LIEN", "State Tax Lien");
    		
    		if(documents.size()>0){
    			templateParams.put(AddDocsTemplates.STATE_TAX_LIEN_YN, YES);
    		} else {
    			templateParams.put(AddDocsTemplates.STATE_TAX_LIEN_YN, NO);
    		}
    		
    		documents = docManager.getDocuments(true, "COURT", "Guardianship");
    		
    		if(documents.size()>0){
    			templateParams.put(AddDocsTemplates.GUARDIANSHIP_YN, YES);
    		} else {
    			templateParams.put(AddDocsTemplates.GUARDIANSHIP_YN, NO);
    		}
    		
    		documents = docManager.getDocuments(true, "LIEN", "Federal Tax Lien");
    		
    		if(documents.size()>0){
    			templateParams.put(AddDocsTemplates.FEDERAL_TAX_LIEN_YN, YES);
    		} else {
    			templateParams.put(AddDocsTemplates.FEDERAL_TAX_LIEN_YN, NO);
    		}
    		
    		documents = docManager.getDocuments(true, "LIEN", "Lis Pendens");
    		
    		if(documents.size()>0){
    			templateParams.put(AddDocsTemplates.LIS_PENDENS_YN, YES);
    		} else {
    			templateParams.put(AddDocsTemplates.LIS_PENDENS_YN, NO);
    		}
    		
    		templateParams.put(AddDocsTemplates.MHU_EVIDENCE_YN, NO);
    		
//    		if(search.isProductType(SearchAttributes.SEARCH_PROD_OE)){
//    			templateParams.put(AddDocsTemplates.HOME_EQUITY_YN, YES);
//    		} else {
    			templateParams.put(AddDocsTemplates.HOME_EQUITY_YN, NO);
//    		}
    		
    		documents = docManager.getDocuments(true, "COURT", "Court", "Judgement", "Judgment");
    		if(documents.size()>0){
    			templateParams.put(AddDocsTemplates.AJ_YN, YES);
    		} else {
    			templateParams.put(AddDocsTemplates.AJ_YN, NO);
    		}
    		
    		String[] divorceSubtypes = new String[]{"Divorce Decree", "Div", "Divorce"};
    		documents = docManager.getDocuments(true, "COURT", divorceSubtypes);
    		documents.addAll(docManager.getDocuments(true, "TRANSFER", divorceSubtypes));
    		
    		if(documents.size()>0){
    			templateParams.put(AddDocsTemplates.DIVORCE_YN, YES);
    		} else {
    			templateParams.put(AddDocsTemplates.DIVORCE_YN, NO);
    		}
    		
    		documents = docManager.getDocuments(true, "LIEN", "Unemployment Lien");
    		
    		if(documents.size()>0){
    			templateParams.put(AddDocsTemplates.UNEMPLOYMENT_LIEN_YN, YES);
    		} else {
    			templateParams.put(AddDocsTemplates.UNEMPLOYMENT_LIEN_YN, NO);
    		}
    		
    	} catch (Exception e) {
    		e.printStackTrace();
    	} finally {
    		docManager.releaseAccess();
    	}
	}


	/**
     * Groups all tags related to abstractor (as user) and fills them
     * @param search current search 
     * @param templateParams destination of tags
     */
    private static void setAbstractorTags(Search search,
			HashMap<String, Object> templateParams) {
    	
    	BigDecimal userId = search.getSa().getAbstractorObject().getID();
    	if(CREATION_SOURCE_TYPES.REOPENED.equals(search.getSearchFlags().getCreationSourceType())) {
    		if(search.getSecondaryAbstractorId() != null) {
    			userId = new BigDecimal(search.getSecondaryAbstractorId());
    		}
    	}
    	
    	UserAttributes ua = UserManager.getUser(userId);
    	
    	String defaultStr = "";
    	
    	templateParams.put(AddDocsTemplates.abstractorName, 
    			ua.getFIRSTNAME() + " " + ua.getMIDDLENAME()+ " " + ua.getLASTNAME());
    	templateParams.put(AddDocsTemplates.abstractorFax, 
    			org.apache.commons.lang.StringUtils.defaultIfEmpty(ua.getALTPHONE(), defaultStr));
    	templateParams.put(AddDocsTemplates.abstractorHPhone, 
    			org.apache.commons.lang.StringUtils.defaultIfEmpty(ua.getHPHONE(), defaultStr));
    	templateParams.put(AddDocsTemplates.abstractorWPhone,
    			org.apache.commons.lang.StringUtils.defaultIfEmpty(ua.getPHONE(), defaultStr));
    	templateParams.put(AddDocsTemplates.abstractorMPhone, 
    			org.apache.commons.lang.StringUtils.defaultIfEmpty(ua.getMPHONE(), defaultStr)); 
    	templateParams.put(AddDocsTemplates.abstractorOperatingAccID, 
    			org.apache.commons.lang.StringUtils.defaultIfEmpty(ua.getWCARD_ID(), defaultStr)); 
    	templateParams.put(AddDocsTemplates.abstractorPersonalID, 
    			org.apache.commons.lang.StringUtils.defaultIfEmpty(ua.getPCARD_ID(), defaultStr));

		
	}

	private static String calculateATSFileExceptions(long searchId) {
    	List<TemplateInfo> templatesInfo = ro.cst.tsearch.templates.TemplateUtils.getTemplatesInfo(searchId, false);
    	List<TemplateInfo> atsTemplates = new ArrayList<TemplateInfo>();
    	
    	Search global  = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
    	int commId = InstanceManager.getManager().getCommunityId(searchId);
    	
    	for(TemplateInfo info:templatesInfo){
    		if(isATSFileName(info.getPath())){
    			atsTemplates.add(info);
    		}
    	}
    	
    	if(atsTemplates.size()==1){
    		TemplateInfo atsInfo = atsTemplates.get(0);
    		
    		String newSourceName = ro.cst.tsearch.templates.TemplateUtils.isTemplateModified(global,
    				atsInfo.getPath(), atsInfo.getId());
    		
    		try {
	    		if(org.apache.commons.lang.StringUtils.isNotBlank(newSourceName)){
						String content = org.apache.commons.io.FileUtils.readFileToString(new File(newSourceName));
						return parseATSFileExceptions(content);
	    		}else{	
	    			SearchAttributes sa = global.getSa();
					State state = State.getState(new BigDecimal(sa.getAtribute(SearchAttributes.P_STATE)));
					County county = County.getCounty(new BigDecimal(sa.getAtribute(SearchAttributes.P_COUNTY)));
					CommunityAttributes ca = InstanceManager.getManager().getCurrentInstance(sa.getSearchId()).getCurrentCommunity();
					HashMap<String,Object> templateParams = TemplateBuilder.fillTemplateParameters(global, ca,county, state, false, false, null);
					
					CommunityTemplatesMapper templateMapper = new CommunityTemplatesMapper();
					templateMapper.setId( atsInfo.getId() );
					templateMapper.setName(atsInfo.getName());
					templateMapper.setShortName(atsInfo.getShortName());
					templateMapper.setLastUpdate(Long.toString(new Date().getTime()));
					templateMapper.setPath(atsInfo.getPath());
					templateMapper.setFileContent("");
					templateMapper.setCommunityId(commId);
					return parseATSFileExceptions(AddDocsTemplates.completeNewTemplatesV2ForTextFilesOnly(templateParams, "", templateMapper, global, true, null, null, null, true));
	    		}
    		} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    	
    	return "*";
	}
    
    private static final Pattern PAT_QUOTE_EXCEPTION = Pattern.compile("\\s+[^= ]+=\"([^\"]+)\"");
    
    private static String parseATSFileExceptions(String content) {
    	content = content.replace("<!--", "").replace("-->", "");
    	
    	int start = content.indexOf("<Exceptions>");
    	int end = content.indexOf("</Exceptions>");
    	
    	if(start>0 && end>start+12){
    		content = content.substring(start+12,end);
    		
    		Matcher matQuoteExceptions = PAT_QUOTE_EXCEPTION.matcher(content);
    		String temp = "";
    		
    		while(matQuoteExceptions.find()){
    			temp+=matQuoteExceptions.group(1);
    			temp+="\r\n\r\n";
    		}
    		
    		if(temp.length()>0){
    			content = temp;
    		}
    		
    		if(content.indexOf('>')>0||content.indexOf('<')>0||content.indexOf('\"')>0){
    			content = "*";
    		}
    		
    		return content;
    	}
    	
		return "*";
	}

	private static boolean isATSFileName(String fileName){
    	return (fileName!=null && fileName.toUpperCase().endsWith(".ATS"));
    }

	public static String getDateLastPaid(TaxDocumentI doc, double baseAmount) {
		String dateLastPaid = "";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy");
		Date datePaid = doc.getDatePaidAsDate();

		if (doc.getAmountPaid() >= doc.getBaseAmount() && datePaid != null) {
			dateLastPaid = simpleDateFormat.format(datePaid);
		} else {
			if (datePaid ==null){
				Date payDate = doc.getPayDate();
				if (payDate!=null) {
					Calendar payDatePlusOneYearCalendar = Calendar.getInstance();
					payDatePlusOneYearCalendar.setTime(payDate);
					payDatePlusOneYearCalendar.add(Calendar.YEAR, 1);
					Date payDatePlusOneYear = payDatePlusOneYearCalendar.getTime();
					
					ArrayList<Receipt> receipts = doc.getReceipts();
					BigDecimal paidAmount = NumberUtils.createBigDecimal("0");
					Date mostRecentDate = null;
					if (receipts!=null){
					String receiptAmount = "";
					String receiptDate = "";
					
					for (int i = 0; i < receipts.size(); i++) {
						Receipt receipt = receipts.get(i);
						receiptDate = receipt.getReceiptDate();
						Date d = Util.dateParser3(receiptDate);
						if (d != null && d.after(payDate) && d.before(payDatePlusOneYear)) {
							receiptAmount = receipt.getReceiptAmount();						
							if(NumberUtils.isNumber(receiptAmount)){
								paidAmount = paidAmount.add(new BigDecimal(receiptAmount));
							}
							
							if (mostRecentDate == null) {
								mostRecentDate = d;
							}
							if (d.after(mostRecentDate)) {
								mostRecentDate = d;
							}
						}
					}
					if(mostRecentDate!=null){
						if (paidAmount.doubleValue() >= baseAmount) {
							dateLastPaid = simpleDateFormat.format(mostRecentDate);
						} else {
		
							Collections.sort(receipts, new ReceiptDateComparator());
							String countyLastReceiptDate = receipts.get(
									receipts.size() - 1).getReceiptDate();
							if (!countyLastReceiptDate.isEmpty()) {
								dateLastPaid = countyLastReceiptDate;
							}
						}
					}
				}
			}}else{
				dateLastPaid = simpleDateFormat.format(datePaid);
			}
		}
		return dateLastPaid;
	}
    
    
    public static VestingInfoI  getVestingGrantor(Search global){
		VestingInfoI legal = global.getSa().getVestingInfoGrantee();
		if( legal.isEdited() ){
			return legal;
		}
		DocumentsManagerI manager = global.getDocManager();
		try{
			manager.getAccess();
			TransferI tr = manager.getLastTransfer();
			//4004
			/*
			TransferI tr1 = manager.getLastRealTransfer();
			*/
			if(tr!=null && tr.getVestingInfoGrantor().isFilledByOcr()){
				return tr.getVestingInfoGrantor();
			}/*
			else if(tr1!=null && tr1.getVestingInfoGrantor().isFilledByOcr()){
				return tr1.getVestingInfoGrantor();
			}*/
			else if(tr!=null){
				String freeform = tr.getGrantorFreeForm();
				if(!StringUtils.isEmpty(freeform)){
					legal.setVesting(freeform);
				}
			}
		}
		finally{
			manager.releaseAccess();
		}
		return legal;
	}
    
    static Pattern pat = Pattern.compile("=\\s*\"([^\"]+)\""); 
    static String cleanForCa(String templateContent){
		StringBuilder build = new StringBuilder(templateContent);
		Matcher mat = pat.matcher(build);
		try{
		while( mat.find() ){
			if(mat.groupCount()>=1){
			String before = build.substring(0,mat.start());
			String after = build.substring(mat.end());
			String str = "=AXXAAXXA"+mat.group(1).replaceAll("[\n]", "&#11;")+"AXXAAXXA";
			build.setLength(0);
			build.append(before);
			build.append(str);
			build.append(after);
			mat.reset();
			}
		}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return build.toString().replaceAll("AXXAAXXA", "\"");
	}
    
    private static String cleanAfterLinkEditor(String txt){
		//txt = txt.replaceAll("(?i)&lt;a href=&quot;", "<a href=\"");
		//txt = txt.replaceAll("(?i)&lt;/a&gt;", "</a>");
    	//txt = txt.replaceAll("(?i)&quot;&gt;", "\">");
    	txt = txt.replaceAll("(?i)&lt;/p&gt;", "\n");
		txt = txt.replaceAll("(?i)&lt;p&gt;", "\n");
		txt = txt.replaceAll("(?i)&amp;nbsp;", "&nbsp;");
		txt = txt.replaceAll("(?i)&nbsp;", " ");
		txt = txt.replaceAll("(?i)&lt;br/&gt;", "\n");
		txt = txt.replaceAll("(?i)&lt;br&gt;", "\n");
		txt = txt.replaceAll("(?i)&lt;/div&gt;", " ");
		txt = txt.replaceAll("(?i)&lt;div&gt;", " ");
		txt = txt.replaceAll("(?i)&lt;/span&gt;", " ");
		txt = txt.replaceAll("(?i)&lt;span&gt;", " ");
		return txt;
	} 
    
    private static void cleanAfterLinkEditor(TemplateContents txt){
		//txt = txt.replaceAll("(?i)&lt;a href=&quot;", "<a href=\"");
		//txt = txt.replaceAll("(?i)&lt;/a&gt;", "</a>");
    	//txt = txt.replaceAll("(?i)&quot;&gt;", "\">");
    	txt.replaceAll("(?i)&lt;/p&gt;", "\n");
		txt.replaceAll("(?i)&lt;p&gt;", "\n");
		txt.replaceAll("(?i)&amp;nbsp;", "&nbsp;");
		txt.replaceAll("(?i)&nbsp;", " ");
		txt.replaceAll("(?i)&lt;br/&gt;", "\n");
		txt.replaceAll("(?i)&lt;br&gt;", "\n");
		txt.replaceAll("(?i)&lt;/div&gt;", " ");
		txt.replaceAll("(?i)&lt;div&gt;", " ");
		txt.replaceAll("(?i)&lt;/span&gt;", " ");
		txt.replaceAll("(?i)&lt;span&gt;", " ");
	} 
    
    public static void cleanJustLink(String fileName, Search search) {

		String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
    	
    	boolean isDoc = "doc".equalsIgnoreCase(extension);
    	boolean isAts = "ats".equalsIgnoreCase(extension);
    	boolean isPxt = "pxt".equalsIgnoreCase(extension);
    	boolean isHtml = "htm".equalsIgnoreCase(extension) || "html".equalsIgnoreCase(extension);
    	if(isDoc) {
    		cleanAndReplaceImageLinksInTemplateDoc(fileName, search);
    		return;
    	}
    	if(isAts || isPxt || isHtml) {
    		String template = StringUtils.fileToString(fileName);
    		template = replaceImageLinksInTemplate(template, search, false, isAts);
    		StringUtils.toFile(fileName, template);
    	}
    }
    
    /**
     * Cleans and prepares a template for the export process removing any ATS specific data
     * @param fileName
     * @param templateName
     * @param extension
     * @param global
     * @param isModified
     * @param removeInvoicePage
     * @param commId
     * @param agentId
     * @param templateId
     * @param replaceImageLinksInTemplate if <code>true</code> only links uploaded to SSF are kept on this file
     * @return
     */
	public static String processTemplateForExport(String fileName, String templateName, String extension, Search global,
			boolean isModified, boolean removeInvoicePage, long commId, long agentId, long templateId, boolean replaceImageLinksInTemplate) {
		try {
			return _processTemplateForExport(fileName, templateName, extension, global, isModified, removeInvoicePage, commId, agentId, templateId,
					replaceImageLinksInTemplate);
		} catch (DisposedException de) {
			logger.error("First try on processTemplateForExport failed", de);
			boolean isDoc = "doc".equalsIgnoreCase(extension);
			if (isDoc) {
				//OpenOffice might fail and we need to retry and since this is TSR creation we can do it more than once
				for (int i = 1; i <= 2; i++) {
					try {
						Thread.sleep((long)(Math.random() * 1000 * i * 60));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					try {
						return _processTemplateForExport(fileName, templateName, extension, global, isModified, removeInvoicePage, commId, agentId, templateId,
								replaceImageLinksInTemplate);
					} catch (DisposedException innerDE) {
						logger.debug("processTemplateForExport try " + i + " failed", innerDE);
					}
				}
				
			}
			throw de;
		}
	}
    
    /**
     * Cleans and prepares a template for the export process removing any ATS specific data
     * @param fileName
     * @param templateName
     * @param extension
     * @param global
     * @param isModified
     * @param removeInvoicePage
     * @param commId
     * @param agentId
     * @param templateId
     * @param replaceImageLinksInTemplate if <code>true</code> only links uploaded to SSF are kept on this file
     * @return
     */
	private static String _processTemplateForExport(String fileName, String templateName, String extension, Search global, 
			boolean isModified, boolean removeInvoicePage, long commId, long agentId, long templateId, boolean replaceImageLinksInTemplate) {
    	    	   	
    	String xmlHeader = "<?xml version=\"1.0\" encoding=\"ASCII\"?>";
    	String stateAbv = InstanceManager.getManager().getCurrentInstance(global.getID()).getCurrentState().getStateAbv().toUpperCase();
    	boolean isDoc = "doc".equalsIgnoreCase(extension);
    	boolean isAts = "ats".equalsIgnoreCase(extension);
    	boolean isPxt = "pxt".equalsIgnoreCase(extension);
    	boolean isHtml = "htm".equalsIgnoreCase(extension) || "html".equalsIgnoreCase(extension);
    	boolean isCa = "CA".equals(stateAbv);
		boolean isFl = "FL".equals(stateAbv);
		
    	if(isDoc) {
    		cleanAndReplaceImageLinksInTemplateDoc(fileName, global);
    		
    		int exportFormat = 0;
			List<CommunityUserTemplatesMapper> templates = DBManager
					.getSimpleTemplate().query(UserUtils.SQL_TEMPLATE_FOR_USER_PROFILE,
							new CommunityUserTemplatesMapper(), agentId, commId, templateId);
			if (templates.size()==1) {
				exportFormat = templates.get(0).getExportFormat();
			}
    		if (PDF_EXPORT_FORMAT==exportFormat || TIFF_EXPORT_FORMAT==exportFormat) {
    			OfficeDocumentContents odc = null;
    	    	try {
    				odc = new OfficeDocumentContents(fileName);
    				String newExtension = "pdf";
        			String pdfName = fileName.replaceFirst("\\." + extension + "$", "." + newExtension);
        			odc.saveToFile(pdfName, newExtension);
        			odc.closeOO();
        			FileUtils.deleteFile(fileName);
        			fileName = pdfName;
        			extension = newExtension;
        			if (TIFF_EXPORT_FORMAT==exportFormat) {
        				String tiffname = ConvertPdf2Tiff.convertPdf2Tiff(fileName);
        				FileUtils.deleteFile(fileName);
            			fileName = tiffname;
        			}
        		} catch (Exception e) {
    				e.printStackTrace();
    				try{if(odc!=null)odc.closeOO();}catch(Exception e1){e1.printStackTrace();}
    			}	
    		}
    		return fileName;
    	}
    	
    	String template = StringUtils.fileToString(fileName);
    	
    	String allImagesSsfLink = global.getAllImagesSsfLink();
    	if (isAts){//must encode the ampersands (&) from link for xml-like templates
    		allImagesSsfLink = allImagesSsfLink.replaceAll("(?s)&", "&amp;");
    	}
    	template = template.replace(ALL_IMAGES_LINK_CODE, allImagesSsfLink);
    	template = template.replace(TSR_LINK_CODE, global.getTsrLink());
    	template = template.replace(TSRI_LINK_CODE, global.getTsriLink());
    	if( isPxt ) {
    		/* I hate this stuff: Bug 4579 */
    		template = template.replace((char)8220,'\"')
    						   .replace((char)8221,'\"')
    						   .replace((char)8217,'\'')
    						   .replaceAll("" + (char)176, "deg.");
    	}
    	if(isAts || isHtml){
        	/*I feel the same. I hate this stuff: Bug 6834 */
        	template = template.replace((char)8217, '\'')
        						.replaceAll("" + (char)8220, "&quot;")
        						.replaceAll("" + (char)8221, "&quot;")
        						.replaceAll("" + (char)176, "&#176;");
    	}
    	
    	if(isAts || isPxt) {
    		template = template.replaceAll("<!--", "").replaceAll("-->", "");
    	}
    	
    	if(isPxt){
    		template = template.replaceAll("(?i)&nbsp;", " ").replaceAll("(?i)&amp;nbsp;", " ");
    		template = template.replaceAll("(?i)&amp;", "&");
    		template = template.replaceAll("(?i)&apos;", "'");
    		template = template.replaceAll("(?i)&quot;", "\"");
    	}
    	
    	if(isAts){
    		template = template.replaceAll("[\\[]([^#])[\\]]", "$1");
    		
			if(template.indexOf("<?xml")!=0) {
				template = xmlHeader + template;
			}		
			
			if( (isFl||isCa) && (!templateName.toLowerCase().contains("titledesk"))) {
				template = cleanForCa(template);
			}
			
			template = template.replaceAll("[/][/]", "&#11;");
			template = template.replaceAll("(?i)&lt;A href=", "&lt;a href=");
			template = template.replaceAll("&lt;/A&gt;", "&lt;/a&gt;");
			
			//clean empty links Task 7686
			
			// <a>text</a>
			// <a></a>
			// <a .... href="" ...>text</a>
			// <a .... href="" ...></a>
			// <a .... href="link" ...></a>
	    	template = template
	    			.replaceAll("(?ism)(?:&lt;)a(?:(?!href).)+href=(?:&quot;)(?:(?!&quot;).)+(?:&quot;)(?:(?!&gt;).)*(?:&gt;)(?:&lt;)/a(?:&gt;)", "")
	    			.replaceAll("(?ism)(?:&lt;)a(?:&gt;)((?:(?!&lt;).)*)(?:&lt;)/a(?:&gt;)", "$1")
	    			.replaceAll("(?ism)(?:&lt;)a(?:(?!href).)+href=(?:&quot;)(?:&quot;)(?:(?!&gt;).)*(?:&gt;)((?:(?!&lt;).)*)(?:&lt;)/a(?:&gt;)", "$1");
		}
    	
    	if(isAts || isPxt || isHtml) {
    		if(replaceImageLinksInTemplate) {
    			template = replaceImageLinksInTemplate(template, global, false, isAts);
    		} else {
    			template = template.replaceAll("(https?:)&#11;", "$1//");
    		}
    	}
    	
    	if(isHtml) {
    		if(removeInvoicePage) {
    			template = template.replaceAll("(?ism)<!-- START INVOICE -->.*?<!-- END INVOICE -->", "");
    		}else {
    			template = template.replaceAll("(?ism)<!-- (?:START|END) INVOICE -->","");
    		}
    	}
    	
    	
    	template = template.replaceAll("\\[HARD_BREAK\\]", "\n").replaceAll("\\|\\|", "\n");
    	template = cleanAfterLinkEditor(template);
    	
    	if(!isModified) {
			if(isHtml && fileName.toLowerCase().contains(TemplatesInitUtils.TEMPLATE_TSD_START.toLowerCase())){
				boolean succeded = corectTSDTemplate(fileName);
				if(!succeded){
					logger.error("%%%%%%%%%%%%%%  Can't  corect the TSD template    %%%%%%%%%%%%%%\n"+fileName);
				}
			}
    	}
    			
    	StringUtils.toFile(fileName, template);
    	
    	if (isHtml) {
    		int exportFormat = 0;
			List<CommunityUserTemplatesMapper> templates = DBManager
					.getSimpleTemplate().query(UserUtils.SQL_TEMPLATE_FOR_USER_PROFILE,
							new CommunityUserTemplatesMapper(), agentId, commId, templateId);
			if (templates.size()==1) {
				exportFormat = templates.get(0).getExportFormat();
			}
    		if (PDF_EXPORT_FORMAT==exportFormat || TIFF_EXPORT_FORMAT==exportFormat) {
    			String pdfName = PDFUtils.convertToPDFFile(template.getBytes());
    			String newExtension = "pdf";
    			fileName = fileName.replaceFirst("\\." + extension + "$", "." + newExtension);
    			extension = newExtension;
    			FileUtils.copy(pdfName, fileName);
    			FileUtils.deleteFile(pdfName);
    			if (TIFF_EXPORT_FORMAT==exportFormat) {
    				String tiffname = ConvertPdf2Tiff.convertPdf2Tiff(fileName);
    				FileUtils.deleteFile(fileName);
        			fileName = tiffname;
    			}
    		}
    	}
    	
    	return fileName;
    	
    	/*try {
			if(isAts) {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder documentBuilder = factory.newDocumentBuilder();
				documentBuilder.parse(fileName);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}*/
    }
    
    
    public static void cleanAndReplaceImageLinksInTemplateDoc(String filename, Search global) {
    	
    	DocumentsManagerI docManager = global.getDocManager();
    	docManager.getAccess();
    	OfficeDocumentContents odc = null;
    	try {
			odc = new OfficeDocumentContents(filename);
			cleanAfterLinkEditor(odc);
			for(Entry<XTextRange,XPropertySet> xps : odc.getLinks().entrySet()) {
				try {
					String fakeLink = (String) xps.getValue().getPropertyValue("HyperLinkURL");
					String realLink = "";
					
					if(fakeLink.contains(TSR_LINK_CODE)){
						realLink = global.getTsrLink();
					}else if(fakeLink.contains(ALL_IMAGES_LINK_CODE)){
						realLink = global.getAllImagesSsfLink();
					}else if(fakeLink.contains(TSRI_LINK_CODE)){
						realLink = global.getTsriLink();
					}
					
					if(org.apache.commons.lang.StringUtils.isBlank(realLink)){
						String documentId = null;
						try {
							documentId = DocumentUtils.makeLongDocumentId(DocumentUtils.extractDocumentIdFromDummyLink(fakeLink), global.getID());
						} catch (Exception e) {
							logger.error("Something happened when tring to get documentId from " + fakeLink, e);
							continue;
						}
						DocumentI doc = docManager.getDocument(documentId);
						if(doc == null ) {
							continue;
						}
						realLink = DocumentUtils.createRealImageLink( doc.getId() , doc.getSiteId() , global );
						if(!doc.hasImage() || !doc.isIncludeImage() 
								|| (doc.hasImage() && !doc.getImage().isSaved())) {
							realLink = "";
							String str = xps.getKey().getString();
							odc.getDoc().getText().insertString(xps.getKey(), str, false);
							odc.getDoc().getText().insertString(xps.getKey(), "", true);
							continue;
						}	
					}
					xps.getValue().setPropertyValue("HyperLinkURL", realLink);
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
			odc.save();
			odc.closeOO();
		} catch (Exception e) {
			e.printStackTrace();
			try{if(odc!=null)odc.closeOO();}catch(Exception e1){e1.printStackTrace();}
		}finally {
			docManager.releaseAccess();
		}
    }
    
    /*public static String replaceImageLinksInTemplate(String template, long searchId) {
    	return replaceImageLinksInTemplate(template,searchId, false);
    }*/
    
    public static String replaceImageLinksInTemplate(String template, Search search, boolean doNotPutRealLink, boolean isAts) {
    	if(StringUtils.isEmpty(template)) {
    		return template;
    	}
    	
    	template = template.replaceAll("(https?:)&#11;", "$1//");
    	DocumentsManagerI docManager = search.getDocManager();
    	try {
			docManager.getAccess();
			for(DocumentI document : docManager.getDocumentsList()) {
				if(document.hasImage() && document.isIncludeImage()  
						&& (document.getImage().isSaved() || StringUtils.isNotEmpty(document.getImage().getSsfLink() ))) {
					String documentId = document.getId();
					String imageLink = doNotPutRealLink?"#":StringEscapeUtils.escapeHtml(DocumentUtils.createRealImageLink(documentId, document.getSiteId(), search));
					if(!isAts){
						imageLink = imageLink.replace("&amp;", "&");
					}
					template = replaceLinkInternal(template, document, search, imageLink);
				}else{
					template = removeLinkInternal(template, document, search);
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			docManager.releaseAccess();
		}
    	
    	if(!doNotPutRealLink) {
	    	template = template.replaceAll("<a href=\\\"[^\\\"]+DocumentDataRetreiver[^\\\"]+\\\"\\s*>([^<]+)</a>","$1");
	    	template = template.replaceAll("(?:&lt;)a href=(?:&quot;)(?:[^&])+DocumentDataRetreiver(?:.*?)(?:&quot;)\\s*(?:&gt;)(.*?)(?:&lt;)/a(?:&gt;)","$1");
    	}

		return template;
    }
    
    private static String replaceLinkInternal(String template,DocumentI document,Search search, String imageLink){
    	template = template.replaceAll(DocumentUtils.createImageLinkWithDummyParameter(document, search ).replace("&", "&amp;").replaceFirst("ats(?:prdinet|stginet|preinet)?[0-9]*", "ats(?:prdinet|stginet|preinet)?[0-9]*"), imageLink);
		template = template.replaceAll(DocumentUtils.createATSImageLinkWithDummyParameter(document, search.getID() ).replace("&", "&amp;").replaceFirst("ats(?:prdinet|stginet|preinet)?[0-9]*", "ats(?:prdinet|stginet|preinet)?[0-9]*"), imageLink);
		template = template.replaceAll(DocumentUtils.createImageLinkWithDummyParameter(document, search ).replaceFirst("ats(?:prdinet|stginet|preinet)?[0-9]*", "ats(?:prdinet|stginet|preinet)?[0-9]*"), imageLink);
		template = template.replaceAll(DocumentUtils.createATSImageLinkWithDummyParameter(document, search.getID() ).replaceFirst("ats(?:prdinet|stginet|preinet)?[0-9]*", "ats(?:prdinet|stginet|preinet)?[0-9]*"), imageLink);
		
		try {
			String lbUrl = LoadConfig.getLoadBalancingUrl();
			int port = LoadConfig.getLoadBalancingPort();
			String fullUrl = lbUrl.replaceFirst("ats(?:prdinet|stginet|preinet)?[0-9]*", "ats(?:prdinet|stginet|preinet)?[0-9]*") + ":" + port;
			String crtServerAtsImageLink = DocumentUtils.createATSImageLinkWithDummyParameter(document, search.getID() ).replaceFirst("(?ism)"+fullUrl, ServerConfig.getAppUrl());
			template = template.replaceAll(crtServerAtsImageLink.replace("?","\\?").replace("&","\\&"), imageLink);
			template = template.replaceAll(crtServerAtsImageLink.replace("&", "&amp;").replace("?","\\?").replace("&","\\&"), imageLink);
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return template;
    }
    
    private static String removeLinkInternal(String template,DocumentI document,Search search){
    	final String TEMP_CODE= "2T0B3D1SQBLED";
    	template = template.replaceAll(DocumentUtils.createImageLinkWithDummyParameter(document, search ).replace("&", "&amp;").replaceFirst("ats(?:prdinet|stginet|preinet)?[0-9]*", "ats(?:prdinet|stginet|preinet)?[0-9]*"), TEMP_CODE);
		template = template.replaceAll(DocumentUtils.createATSImageLinkWithDummyParameter(document, search.getID() ).replace("&", "&amp;").replaceFirst("ats(?:prdinet|stginet|preinet)?[0-9]*", "ats(?:prdinet|stginet|preinet)?[0-9]*"), TEMP_CODE);
		template = template.replaceAll(DocumentUtils.createImageLinkWithDummyParameter(document, search ).replaceFirst("ats(?:prdinet|stginet|preinet)?[0-9]*", "ats(?:prdinet|stginet|preinet)?[0-9]*"), TEMP_CODE);
		template = template.replaceAll(DocumentUtils.createATSImageLinkWithDummyParameter(document, search.getID() ).replaceFirst("ats(?:prdinet|stginet|preinet)?[0-9]*", "ats(?:prdinet|stginet|preinet)?[0-9]*"), TEMP_CODE);
				
		try {
			String lbUrl = LoadConfig.getLoadBalancingUrl();
			int port = LoadConfig.getLoadBalancingPort();
			String fullUrl = lbUrl.replaceFirst("ats(?:prdinet|stginet|preinet)?[0-9]*", "ats(?:prdinet|stginet|preinet)?[0-9]*") + ":" + port;
			String crtServerAtsImageLink = DocumentUtils.createATSImageLinkWithDummyParameter(document, search.getID() ).replaceFirst("(?ism)"+fullUrl, ServerConfig.getAppUrl());
			template = template.replaceAll(crtServerAtsImageLink.replace("?","\\?").replace("&","\\&"), TEMP_CODE);
			template = template.replaceAll(crtServerAtsImageLink.replace("&", "&amp;").replace("?","\\?").replace("&","\\&"), TEMP_CODE);
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		template = template.replaceAll("(?i)(?:<|&lt;)a href=(?:\"|&quot;)"+TEMP_CODE+"(?:\"|&quot;)(?:>|&gt;)(.*?)(?:<|&lt;)/a(?:>|&gt;)", "$1");
		template = template.replaceAll("(?i)(?:<|&lt;)a href=(?:\"|&quot;)"+TEMP_CODE+"(?:&|&amp;)forceFormat=PDF(?:\"|&quot;)(?:>|&gt;)(.*?)(?:<|&lt;)/a(?:>|&gt;)", "$1");
		
		return template;
    }
    
    public static HashMap<String,String> addTemaplteFilesV2(Search search, HashMap<String,Object> templateParams, 
            UserAttributes agent, CommunityAttributes ca, String TSRFileName, boolean removeInvoicePage) throws DirectoryException, TemplatesException {
    	
    	Search globalsearch= InstanceManager.getManager().getCurrentInstance(search.getID()).getCrtSearchContext();
	
    	HashMap<String,String> returnedFiles 		= new HashMap<String,String>();
     	List<CommunityTemplatesMapper> userTemplates = null;
		try {					
			userTemplates = UserUtils.getUserTemplates(agent.getID().longValue(),-1, UserUtils.FILTER_BOILER_PLATES_EXCLUDE, globalsearch.getProductId());					
		} catch (Exception e1) {
			logger.error(e1.toString());
			return null;
		}
		
		for (CommunityTemplatesMapper communityTemplatesMapper : userTemplates) {
			
				String templateName	= communityTemplatesMapper.getPath();		
				String fileExtension = templateName.substring(templateName.lastIndexOf(".") + 1);
				
				long templateId	= communityTemplatesMapper.getId();
				final String shorttemplate = communityTemplatesMapper.getShortName();
				final String longtemplate = communityTemplatesMapper.getPath();
	    		final String templateNewPath = makeTemapltesNameV2(TSRFileName, shorttemplate,templateId, fileExtension);
	    		//String sourceName=globalsearch.getSearchDir()+File.separator+Search.TEMP_DIR_FOR_TEMPLATES+File.separator+templateName;
				
				String newSourceName = ro.cst.tsearch.templates.TemplateUtils.isTemplateModified(search,templateName,templateId);
				
				if(longtemplate.startsWith(TemplatesInitUtils.TEMPLATE_TSD_START) && !longtemplate.contains("Invoice")){
					synchronized (search) {
						search.setTSDFileName(templateNewPath);
					}
				}
				
				try {
					
					if(newSourceName != null ){
						FileCopy.copy(new File(newSourceName),templateNewPath,!FileUtils.getFileExtension(newSourceName).equalsIgnoreCase(".doc"));						
					}else {
						AddDocsTemplates.completeNewTemplatesV2New(
									templateParams, templateNewPath, 
									communityTemplatesMapper,search,true, null, null, new HashMap<String,String>(), false);																					
					}
					
					long commId = ca.getID().longValue();
					long agentId = agent.getID().longValue();
					
					String templateNewPathAfterProcess = processTemplateForExport(templateNewPath,templateName, fileExtension, globalsearch, 
							newSourceName != null,removeInvoicePage, commId, agentId, templateId, true);
					returnedFiles.put(String.valueOf(communityTemplatesMapper.getId()), templateNewPathAfterProcess);
					
				}catch(Exception e) {
					
					returnedFiles.put(String.valueOf(communityTemplatesMapper.getId()), templateNewPath+NOT_EXIST_COD);
					
					EmailClient email = new EmailClient();
					email.addTo(MailConfig.getExceptionEmail());
					email.setSubject("Template error on " + URLMaping.INSTANCE_DIR + " for search " + globalsearch.getID());
					email.addContent("[" + globalsearch.getID() +"] Problems when generate template short: "+ shorttemplate +",   "+"long name: "+longtemplate	
							+"\n"+"ERROR: \n"+Log.exceptionToString(e));
					email.sendAsynchronous();

					e.printStackTrace();
					
				}
			
    	}
    	return returnedFiles;
    }
    
    //if we want some clean after TSD editor 
    public static boolean corectTSDTemplate(String templateNewPath){
    	return true;
    }
        
    public static String makeTemapltesNameV2(String tsrFile, String prefix,long templateId, String fileExtension) {
    	String nameFile = "";
    	 int lastSeparator = tsrFile.lastIndexOf(File.separator);
        if (lastSeparator >= 0) {
            lastSeparator += File.separator.length();
            int lastPeriod = tsrFile.lastIndexOf(".");
            if (lastPeriod >= lastSeparator) {
                nameFile = tsrFile.substring(0, lastSeparator) + prefix + templateId
                        + tsrFile.substring(lastSeparator + UpdateFileCheckFilter.TSR_TSU_LEN, lastPeriod + 1) + fileExtension;
            }
        }
        return nameFile;
    }
       
    public static String getSearchFee(Search search)
    {
    	
    	String payRate = defaultValue;
    	
    	Payrate pr = DBManager.getCurrentPayrateForCounty(
    			InstanceManager.getManager().getCurrentInstance(search.getID()).getCurrentCommunity().getID().longValue(), 
    			InstanceManager.getManager().getCurrentInstance(search.getID()).getCurrentCounty().getCountyId().longValue(), 
    			new Date(System.currentTimeMillis()),
    			InstanceManager.getManager().getCurrentInstance(search.getID()).getCurrentUser());
    	//finding current user rating id		
    	double rate = 0.0;
    	UserRates ur = UserManager.getUserRates(InstanceManager.getManager().getCurrentInstance(search.getID()).getCurrentUser().getID().longValue(), 
    			InstanceManager.getManager().getCurrentInstance(search.getID()).getCurrentCounty().getCountyId().longValue(),
    			new Date(System.currentTimeMillis()));
    	if (ur != null) 
    		rate = ur.getC2ARATEINDEX();
    	else
    		rate = 1;
    	
    	
    	BigDecimal payRateBD = null;
    	if (pr != null){
    		/*
    		if ( isSearchUpdate )
    			payRateBD = new BigDecimal(pr.getUpdateValue()*rate);
    		else
    			payRateBD = new BigDecimal(pr.getSearchValue()*rate);
    		*/
    		int service = 1;
    		try{
    			service= Integer.parseInt(search.getSa().getAtribute(SearchAttributes.SEARCH_PRODUCT));
    		}catch(Exception e){
    			//e.printStackTrace();
    		}
    		payRateBD = new BigDecimal(pr.getProductValue(service) * rate);
    		//long commID = InstanceManager.getManager().getCurrentInstance(search.getID()).getCurrentCommunity().getID().longValue();
    	}
    	if (payRateBD != null)
    		payRate = ATSDecimalNumberFormat.format( payRateBD );
    	String alternativePayrate = "$";
    	try {
    		DecimalFormat format = new DecimalFormat("#,##0.00");
    		alternativePayrate += format.format(Double.parseDouble(search.getSa().getAtribute(SearchAttributes.PAYRATE_NEW_VALUE)));
    		/*alternativePayrate = ATSDecimalNumberFormat.format( 
    		 new BigDecimal (
    		 Double.parseDouble(searchAttributes.getAtribute(SearchAttributes.PAYRATE_NEW_VALUE))));
    		 */
    		return alternativePayrate;
    	} catch (NumberFormatException e) {
    		alternativePayrate = payRate;
    		return payRate;
    	} 
    }
      
  //calculate searchFee using comunity to agent value
  public static double calculateTSDSearchFee(CommunityAttributes ca, County currentCounty, Search global, int service){
	  
	 double payRate1=0.0;
	  
	  Payrate pr = DBManager.getCurrentPayrateForCounty(
      		ca.getID().longValue(),
      		currentCounty.getCountyId().longValue(), 
      		new Date(System.currentTimeMillis()),
      		InstanceManager.getManager().getCurrentInstance(global.getID()).getCurrentUser());
	  
	  // finding current user rating id --> current agent rating		
	  double rate = 0.0;
      
      UserRates ur = null;
      
      try
      {
          ur = UserManager.getUserRates( global.getAgent().getID().longValue() , currentCounty.getCountyId().longValue(), new Date(System.currentTimeMillis()));
      }
      catch( Exception e )
      {
          ur = UserManager.getUserRates(
        		  InstanceManager.getManager().getCurrentInstance(global.getID()).getCurrentUser().getID().longValue(), 
        		  currentCounty.getCountyId().longValue(), 
        		  new Date(System.currentTimeMillis()));
      }
      
		if (ur != null) 
			rate = ur.getC2ARATEINDEX();
		else
			rate = 1;
		
		BigDecimal payRateBD = null;
		if (pr != null){
			
			payRateBD = new BigDecimal(pr.getProductValue(service) * rate);
		}
      if (payRateBD != null){
          payRate1 = payRateBD.doubleValue();
      }
      
      double value=0.0;
      
      try {
      	value = Double.parseDouble(global.getSa().getAtribute(SearchAttributes.PAYRATE_NEW_VALUE));
      	
      } catch (NumberFormatException e) {
    	  value = payRate1;      	
      } 
  
	  return value;
  }
  
  	//CAlifornia wants Dates in other format
	public static String getDateFormatedForCA( String dateStr ){
		SimpleDateFormat simpleDF1 = new SimpleDateFormat("MMMM d, yyyy");
		Date date = Util.dateParser3( dateStr );
		if( date==null ){
			return simpleDF1.format(new Date());
		}
		return simpleDF1.format( date );
	}
	
	public static SimpleDateFormat getSimpleDateFormatForTemplates(int stateId){
	  	//CAlifornia wants Dates in other format
		if(StateContants.CA==stateId
			||StateContants.CO==stateId){ 
			return new SimpleDateFormat("MMMM d, yyyy");
		}
		else{
			return new SimpleDateFormat("MM/dd/yyyy");
		}
	}
  
  public static String calculateLastUpdateDate(Search global){
	  SimpleDateFormat simpleDF1 = new SimpleDateFormat("MMM d, yyyy");
	  SimpleDateFormat simpleDF2 = new SimpleDateFormat("MMMM d, yyyy");
	  String fromDate ="";
	  if(global==null){
		  return "";
	  }
	  
	  SearchAttributes searchAttributes = global.getSa();
	  
	  if(searchAttributes!=null){
		  fromDate = searchAttributes.getAtribute( SearchAttributes.FROMDATE );
	  }
	  if(fromDate==null){
		  fromDate="";
	  }
	    if ( !fromDate.trim().equals("") ) {
	        Date dFromDate = simpleDF1.parse(fromDate, new ParsePosition(0));
	        if (dFromDate != null) {
	            fromDate = simpleDF2.format(dFromDate);
	            fromDate += " @ 08:00 A.M.";
	        } else {
	            fromDate = "";
	        }
	    }
	    
	   return fromDate;
  }
  
  public static String getLastReceiptDate(Search search, boolean isCA, boolean forCity) {
	  
	  DocumentsManagerI docManager = search.getDocManager();
	  String lastReceiptDate = defaultValue;
	  
	  try{
	         docManager.getAccess();
	        	        	
	         Collection<DocumentI> taxDocuments = null;
	         if (!forCity)
	         {
	        	 taxDocuments = docManager.getDocumentsWithDocType(true, /*DocumentTypes.CITYTAX,*/ DocumentTypes.COUNTYTAX);
	         }
	         else
	         {
	        	 taxDocuments = docManager.getDocumentsWithDocType(true,DocumentTypes.CITYTAX);
	         }
	         
	         DocumentUtils.sortDocuments(taxDocuments, MultilineElementsMap.DATE_ORDER_DESC);
	         
	         if(!taxDocuments.isEmpty()) {
	        	 for(DocumentI doc : taxDocuments ) {	         
		        	 if(doc instanceof TaxDocumentI ) {
		        		 
							TaxDocumentI taxDoc = (TaxDocumentI) doc;	
							
							try {
								String countyLastReceiptDate = new String("");
								if (taxDoc.getReceipts()!=null)
								{
									if (taxDoc.getReceipts().size()>=1)
									{
										ArrayList<Receipt> l = new ArrayList<Receipt>();
										l.addAll(taxDoc.getReceipts());
										Collections.sort(l, new ReceiptDateComparator());
										
										countyLastReceiptDate = l.get(l.size()-1).getReceiptDate();
									}
								}
								if(!countyLastReceiptDate.isEmpty()) {
									if(isCA){
										lastReceiptDate = getDateFormatedForCA(countyLastReceiptDate);
									}
									lastReceiptDate = countyLastReceiptDate;
									break;
								}else{
									String datePaid = taxDoc.getDatePaid();
									lastReceiptDate = datePaid;
								}
							}
							catch(Exception ignored) {}
		        	 }
	        	 }
	         }
	         
     } catch(Exception e){
     	e.printStackTrace(); 
     } finally {
     	docManager.releaseAccess();
     }
     
     return lastReceiptDate;
      
  }
  
  public static String calculateNowUpdateDate(Search global){
	  SimpleDateFormat simpleDF1 = new SimpleDateFormat("MMM d, yyyy");
	  SimpleDateFormat simpleDF2 = new SimpleDateFormat("MMMM d, yyyy");
	  String toDate ="";
	  if(global==null){
		 return "";
	  }
	  
	  SearchAttributes searchAttributes = global.getSa();
	  
	  if(searchAttributes!=null){
		  toDate = searchAttributes.getAtribute( SearchAttributes.TODATE );
	  }
	  if(toDate==null){
		  toDate="";
	  }
	    if ( !toDate.trim().equals("") ) {
	        Date dtoDate = simpleDF1.parse(toDate, new ParsePosition(0));
	        if (dtoDate != null) {
	            toDate = simpleDF2.format(dtoDate);
	            toDate += " @ 08:00 A.M.";
	        } else {
	            toDate = "";
	        }
	    }
	    
	   return toDate;
  }
  
  public static int calculateUpdateNO(Search global){
	  int nr=0;
	  
	  if(global==null){
		  return 0;
	  }
	  nr = global.getTsuNo();
	  return nr;
	  
  }
  
  public static String makeImageCommunityLink(Search search){
	  String searchPath = search.getSearchDir();
	  
	  String imgLink = ServerConfig.getAppUrl() + "/title-search/fs?f=" 
		+ searchPath.substring(searchPath.indexOf("TSD")).replaceAll("\\\\","/") 
		+  "Title%20Document/logo.gif?searchId="+search.getID()+"\"";  
	  imgLink = "<img src=\"" + imgLink + "></img>";
	  
	  return imgLink;
  }
  
	public static String constructTSDPatriots(Search search) {

		DocumentsManagerI docManager = search.getDocManager();
		StringBuilder sb = new StringBuilder();
		try {
			docManager.getAccess();
			Collection<DocumentI> patriots = docManager.getDocumentsWithDocType(true,"PATRIOTS");
			
			if(!patriots.isEmpty()) {
				sb.append("<BR><h3>Patriots act:</h3>");
			}
			
			String noResultsFoundDoc = "";
			for(DocumentI doc : patriots ) {
				
				String documentIndex = DBManager.getDocumentIndex(doc.getIndexId());
				documentIndex = documentIndex.replaceAll("(?is)</?html>", "");
				documentIndex = documentIndex.replaceAll("(?is)</?title>", "");
				documentIndex = documentIndex.replaceAll("(?is)</?head>", "");
				documentIndex = documentIndex.replaceAll("(?is)</?body>", "");
		    	 
				if(doc.hasImage() || XXStewartPA.getNumberOfResultsFound(documentIndex) > 0) {
					sb.append(documentIndex.trim());
				} else {
					if(noResultsFoundDoc.isEmpty()) {
						noResultsFoundDoc = documentIndex.trim();
					} else {
						noResultsFoundDoc = noResultsFoundDoc.replaceFirst("<td class=\"TdNameOdd\">(.*?)</td>", "<td class=\"TdNameOdd\">$1<br>"+doc.getGrantorFreeForm()+"</td>");
					}
				}
			}
			sb.append(noResultsFoundDoc);
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			docManager.releaseAccess();
		}
						
		return sb.toString().replaceAll("(?i)</?(html|body)>", ""); 
	}

		public static void putLastPlatTags(Map<String, Object> templateParams, Search search, SimpleDateFormat sdf) {

		templateParams.put(AddDocsTemplates.platRecDate, "");
		templateParams.put(AddDocsTemplates.platRecDateInt, Long.MIN_VALUE);

		String platBook = "", platPage = "";
		
		DocumentsManagerI docManager = search.getDocManager();
		try { 
			
			docManager.getAccess();
			RegisterDocumentI lastPlat = docManager.getLastPlat();
		
			boolean availableDocumentWithIncludeImage = false;
			try {
				if(lastPlat != null) {
					platBook = lastPlat.getBook().replaceAll("&nbsp;", " ");
					platPage = lastPlat.getPage().replaceAll("&nbsp;", " ");
					if (StringUtils.isEmpty(platBook.trim()) && StringUtils.isEmpty(platPage.trim())){
						if (StringUtils.isNotEmpty(lastPlat.getInstno().replaceAll("&nbsp;", " ").trim())){
							if (lastPlat.getInstno().replaceAll("&nbsp;", " ").trim().matches("\\d+[-|_]\\d+")){
								String instrNo = lastPlat.getInstno().replaceAll("&nbsp;"," ").trim();
								platBook = instrNo.replaceAll("(?is)(\\d+)[-|_]\\d+", "$1");
								platPage = instrNo.replaceAll("(?is)\\d+[-|_](\\d+)", "$1");
							}
						}
					}
					if (lastPlat.hasImage() && lastPlat.isIncludeImage()) {
						availableDocumentWithIncludeImage = true;
					}
				} else {
					String book = search.getSa().getAtribute(SearchAttributes.LD_BOOKNO);
					String page = search.getSa().getAtribute(SearchAttributes.LD_PAGENO);
					if (!StringUtils.isEmpty(book) && !StringUtils.isEmpty(page)) {
						platBook = book;
						platPage = page;
					}
				}
			} catch (Exception e) {
				String book = search.getSa().getAtribute(SearchAttributes.LD_BOOKNO);
				String page = search.getSa().getAtribute(SearchAttributes.LD_PAGENO);
				if (!StringUtils.isEmpty(book) && !StringUtils.isEmpty(page)) {
					platBook = book;
					platPage = page;
				}
			}
	
			try {
	
				templateParams.put(AddDocsTemplates.platBook,replaceEmptyString(platBook));
				templateParams.put(AddDocsTemplates.platPage,replaceEmptyString(platPage));
				String platBookPage = "";
				if(!StringUtils.isEmpty(platBook) && !StringUtils.isEmpty(platPage)) {
					platBookPage = platBook + "-" + platPage;
				}
				templateParams.put(AddDocsTemplates.platBookPage,replaceEmptyString(platBookPage));
				
				List<String> tagsWithImages = new ArrayList<String>();
				
			    tagsWithImages.add(AddDocsTemplates.platBook);
			    tagsWithImages.add(AddDocsTemplates.platPage);
			    tagsWithImages.add(AddDocsTemplates.platBookPage);
			    
			    for (String tagWithImages : tagsWithImages) {
					String valueOfTag = (String) templateParams.get(tagWithImages);
					
					if (!StringUtils.isEmpty(valueOfTag)) {
						if (availableDocumentWithIncludeImage) {
							String linkForImage = DocumentUtils.createImageLinkWithDummyParameter(lastPlat, search);
							String linkForPDF = DocumentUtils.createImageLinkForImageAsPDF(lastPlat, search);
							templateParams.put(tagWithImages + MultilineElementsMap.DOCUMENT_IMAGE_MARKER, AddDocsTemplates.createDocimageLink(linkForImage, valueOfTag));
							templateParams.put(tagWithImages + MultilineElementsMap.DOCUMENT_PDF_MARKER, AddDocsTemplates.createDocimageLink(linkForPDF, valueOfTag));
						} else {
							templateParams.put(tagWithImages + MultilineElementsMap.DOCUMENT_IMAGE_MARKER, valueOfTag);
							templateParams.put(tagWithImages + MultilineElementsMap.DOCUMENT_PDF_MARKER, valueOfTag);
						}
					} else {
						templateParams.put(tagWithImages + MultilineElementsMap.DOCUMENT_IMAGE_MARKER, "");
						templateParams.put(tagWithImages + MultilineElementsMap.DOCUMENT_PDF_MARKER, "");					
					}
				}
				
	
				if (lastPlat != null && lastPlat.getRecordedDate()!=null) {
					templateParams.put(AddDocsTemplates.platRecDate, sdf.format(lastPlat.getRecordedDate()));
					templateParams.put(AddDocsTemplates.platRecDateInt, new Long(lastPlat.getRecordedDate().getTime()));
				}
	
			} catch (Exception ex) {}
		} finally {
			docManager.releaseAccess();
		}
	}

	public static String cleanCountyName (String countyName){
		countyName = countyName.replaceAll("(?i)&nbsp;", " ");
		return countyName;
	}

	/**
	 * Return defaultValue if the string is empty, or the original string otherwise
	 * @param str String
	 */
	public static String replaceEmptyString(String str) {
		if(str == null) return str;
		return str.trim().isEmpty()?defaultValue:str;
	}

	public static String getAtsExportDataPreviewTag(String templateName, Search search) {
		String ATSExportDataPreview = "";
		if(!FileUtils.getFileExtension(templateName).equalsIgnoreCase(".ats") ) {
			try {
				long searchId = search.getID();
				long userId = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser().getID().longValue();
				Map<String,String> boilerPlatesTSR = ro.cst.tsearch.templates.TemplateUtils.getBoilerText(searchId, userId, null);
				List<CommunityTemplatesMapper> userTemplates = UserUtils.getUserTemplates(search.getAgent().getID().longValue(),-1, UserUtils.FILTER_BOILER_PLATES_EXCLUDE,search.getProductId());
				for (CommunityTemplatesMapper communityTemplatesMapper : userTemplates) {
					if(communityTemplatesMapper.getPath().toLowerCase().endsWith("ats") ) {
						if(ATSExportDataPreview.isEmpty()) { 
							String exporteTemplateContents = ro.cst.tsearch.templates.TemplateUtils.compileTemplate( searchId, userId, communityTemplatesMapper.getPath(), false, null, null, false,boilerPlatesTSR).getTemplateContent();
							exporteTemplateContents = exporteTemplateContents.replaceAll("<!--", "").replaceAll("-->", "");
							ATSExportDataPreview = TemplatesServlet.previewTemplateImpl(communityTemplatesMapper.getPath(),exporteTemplateContents,searchId);
						}else {
							ATSExportDataPreview = "Multiple .ats templates detected. Please have only one .ats template checked to use the ATSExportDataPreview tag";
							break;
						}
					}


				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return ATSExportDataPreview;	  
	}

	public static String getOwnerType(SearchAttributes sa) {
		String ownerType = AddDocsTemplates.OWNER_TYPE_OTHER;
        ArrayList<NameI> ownersNames = new ArrayList<NameI>(); 
        
        for(NameI name : sa.getOwners().getNames()) {
        	if (name.isValidated()) {
				ownersNames.add(name);
			}
        }
        
        if(ownersNames.size() == 1) {
        	NameI name = ownersNames.get(0);
        	if(name.isCompany()) {
        		String nameText = name.getLastName().toUpperCase();
        		if(nameText.matches(".*\\bLLC\\b.*")) {
        			ownerType = AddDocsTemplates.OWNER_TYPE_LLC;
        		} else if(nameText.matches(".*\\bLP\\b.*")) {
        			ownerType = AddDocsTemplates.OWNER_TYPE_LP;
        		} if(nameText.matches(".*\\bCORP(ORATION)?\\b.*")) {
        			ownerType = AddDocsTemplates.OWNER_TYPE_CORP;
        		} if(nameText.matches(".*\\bPARTNERSHIP\\b.*")) {
        			ownerType = AddDocsTemplates.OWNER_TYPE_PARTNERSHIP;
        		}
        	} else {
        		ownerType = AddDocsTemplates.OWNER_TYPE_SINGLE;
        	}
        } else if(ownersNames.size() == 2) {
        	ownerType = AddDocsTemplates.OWNER_TYPE_MULTIPLE;
        	
        	if(!ownersNames.get(0).isCompany() && !ownersNames.get(1).isCompany()) {
	        	String firstName1 = ownersNames.get(0).getFirstName();
	        	String firstName2 = ownersNames.get(1).getFirstName();
	        	String lastName1 = ownersNames.get(0).getLastName();
	        	String lastName2 = ownersNames.get(1).getLastName();
	        	
	        	if(lastName1.equals(lastName2)) {
	        		boolean foundMale = (FirstNameUtils.isMaleName(firstName1) && !FirstNameUtils.isFemaleName(firstName1)) ||
	        			(FirstNameUtils.isMaleName(firstName2) && !FirstNameUtils.isFemaleName(firstName2));
	        		boolean foundFemale = (FirstNameUtils.isFemaleName(firstName1) && !FirstNameUtils.isMaleName(firstName1)) ||
        				(FirstNameUtils.isFemaleName(firstName2) && !FirstNameUtils.isMaleName(firstName2));
		        	if(foundMale && foundFemale) {
		        		ownerType = AddDocsTemplates.OWNER_TYPE_MARRIED;
		        	}
	        	}
        	}
        } else if(ownersNames.size() > 2) {
        	ownerType = AddDocsTemplates.OWNER_TYPE_MULTIPLE;
        }
        
        return ownerType;
	}


	public static String getDefaultLegalDescription(Search search, HashMap<String, Object> templateParamsPreloaded) {
		
		String ld = "";
		try {
			
			SearchAttributes sa = search.getSa();
			if(sa.getAtribute(SearchAttributes.SEARCHUPDATE).trim().toLowerCase().equals("true")){
				return "";
			}
			
			State state = State.getState(new BigDecimal(sa.getAtribute(SearchAttributes.P_STATE)));
			County county = County.getCounty(new BigDecimal(sa.getAtribute(SearchAttributes.P_COUNTY)));
			CommunityAttributes ca = InstanceManager.getManager().getCurrentInstance(sa.getSearchId()).getCurrentCommunity();
			HashMap<String,Object> templateParams = fillTemplateParameters(search, ca,county, state, true, false, templateParamsPreloaded);
			
			List<CountyDefaultLegalMapper> l = DBManager.getCountyDefaultLegal(county.getCountyId()+"", ca.getID().intValue());
			
			CommunityTemplatesMapper templateMapper = new CommunityTemplatesMapper();
			templateMapper.setId( l.get(0).getTemplateId() );
			templateMapper.setLastUpdate(Long.toString(new Date().getTime()));
			templateMapper.setCommunityId(AddDocsTemplates.LEGAL_COMMID);
			if (search.getSa().isCondo()) {
				templateMapper.setName(AddDocsTemplates.LEGAL_CONDO_TEMPLATE_NAME);
				templateMapper.setShortName(AddDocsTemplates.LEGAL_CONDO_TEMPLATE_NAME);
				templateMapper.setPath(AddDocsTemplates.LEGAL_CONDO_TEMPLATE_NAME);
				templateMapper.setFileContent(l.get(0).getDefaultLdCondo());
			} else {
				templateMapper.setName(AddDocsTemplates.LEGAL_TEMPLATE_NAME);
				templateMapper.setShortName(AddDocsTemplates.LEGAL_TEMPLATE_NAME);
				templateMapper.setPath(AddDocsTemplates.LEGAL_TEMPLATE_NAME);
				templateMapper.setFileContent(l.get(0).getDefaultLd());
			}
			
			ld = AddDocsTemplates.completeNewTemplatesV2ForTextFilesOnly(templateParams, "", templateMapper,search, false, null, null, null);
		}catch(Exception e) {
			//e.printStackTrace();
		}
		
		return ld;
	}


	public static String getTSDNewPage() {
		return "\n\n<!-- NEW PAGE -->\n\n";
	}
}