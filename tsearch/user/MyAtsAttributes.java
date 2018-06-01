package ro.cst.tsearch.user;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.name.NameFormater;
import com.stewart.ats.base.name.NameFormaterI;
import com.stewart.ats.base.name.NameFormaterI.PosType;
import com.stewart.ats.base.name.NameFormaterI.TitleType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.SortBy;

public class MyAtsAttributes implements ParameterizedRowMapper<MyAtsAttributes>, Serializable {
	
	public static final int DEFAULT_AGENTS_SELECT_WIDTH	=	180;
	
	
	public static enum USER_START_VIEW_DATE_TYPE {
		USER_START_VIEW_DATE_DEFAULT (-1),
		USER_START_VIEW_DATE_CD_PRIOR (0);
		
		private int type;
		
		private USER_START_VIEW_DATE_TYPE(int type) {
			this.type = type;
		}
		public int getType() {
			return type;
		}
	}
	
	public BigDecimal user_id				= BigDecimal.valueOf(-1);
	
	public BigDecimal SEARCH_PAGE_STATE		= BigDecimal.valueOf(-1);
	public BigDecimal SEARCH_PAGE_COUNTY	= BigDecimal.valueOf(-1);
	public BigDecimal SEARCH_PAGE_AGENT		= BigDecimal.valueOf(-1);
	
	public String TSR_SORTBY				= "SORTBY_INST";
	public Integer TSR_UPPER_LOWER			= DBConstants.NAMES_TITLECASE;
	public Integer TSR_NAME_FORMAT			= DBConstants.NAMES_FORMAT_FML;
	public BigDecimal TSR_COLORING			= BigDecimal.valueOf(-1);
	public BigDecimal paginate_tsrindex     = BigDecimal.valueOf(150);
	 
	public String reportState				= "-1";
	public String reportCounty				= "-1";
	public String reportAbstractor			= "-1";
	public String reportCompanyAgent		= "-1";
	public String reportAgent				= "-1";
	public String reportStatus				= "20";
	
	public String reportDefaultView			= URLMaping.REPORTS_INTERVAL;
	public String reportSortBy				= "TSR";
	public String reportSortDir				= "desc";
	public String DASHBOARD_START_INTERVAL	= "now-30";
	public String DASHBOARD_END_INTERVAL	= "now";
	public BigDecimal DASHBOARD_ROWS_PER_PAGE= BigDecimal.valueOf(100);
	
	public String DEFAULT_HOMEPAGE			= URLMaping.REPORTS_INTERVAL;
	public Integer MY_ATS_READ_ONLY			= 0;
	
	public Integer receive_notification		= 1;
	public Integer search_log_link			= 0;
	public Integer invoiceEditEmail			= 0;
	
	public BigDecimal pages					= BigDecimal.valueOf(10);
	public Timestamp timestamp 				= null;
	public BigDecimal default_state			= BigDecimal.valueOf(-1);
	public BigDecimal default_county		= BigDecimal.valueOf(-1);

	public Integer legalCase				= DBConstants.NAMES_TITLECASE;
	public Integer vestingCase				= DBConstants.NAMES_NO_CHANGE;
	public Integer addressCase				= DBConstants.NAMES_TITLECASE;
	private int agentsSelectWidth			= DEFAULT_AGENTS_SELECT_WIDTH;
	public Integer startViewDateValue		= USER_START_VIEW_DATE_TYPE.USER_START_VIEW_DATE_DEFAULT.getType();

	/* Fields not used anymore but kept for old search compatibility issues */
	@SuppressWarnings("unused")
	@Deprecated
	private transient Integer	reportNameFormat			= null;
	@SuppressWarnings("unused")
	@Deprecated
	private transient Integer	reportNameCase				= null;
	@SuppressWarnings("unused")
	@Deprecated
	private transient Integer	ocrNamesFormat				= null;
	@SuppressWarnings("unused")
	@Deprecated
	private transient Integer	ocrLegalDescriptionFormat	= null;
	@SuppressWarnings("unused")
	@Deprecated
	private transient String	defaultLD					= null;
	@SuppressWarnings("unused")
	@Deprecated
	private transient Integer	USE_OCR_LEGAL				= null;
	
	/* fields that are not stored in the database */
	public UserAttributes ua;

	public MyAtsAttributes() {}

	public MyAtsAttributes(BigDecimal user_id) {
		this.user_id = user_id;
	}
	
	public MyAtsAttributes mapRow(ResultSet rs, int rowNum) throws SQLException {
        MyAtsAttributes my = new MyAtsAttributes();
    	if(rs.getBigDecimal(UserAttributes.USER_ID)!=null) my.setUser_id(rs.getBigDecimal(UserAttributes.USER_ID));
    	if(rs.getBigDecimal(UserAttributes.USER_SEARCH_PAGE_STATE)!=null) my.setSEARCH_PAGE_STATE(rs.getBigDecimal(UserAttributes.USER_SEARCH_PAGE_STATE));
    	if(rs.getBigDecimal(UserAttributes.USER_SEARCH_PAGE_COUNTY)!=null) my.setSEARCH_PAGE_COUNTY(rs.getBigDecimal(UserAttributes.USER_SEARCH_PAGE_COUNTY));
    	if(rs.getBigDecimal(UserAttributes.USER_SEARCH_PAGE_AGENT)!=null) my.setSEARCH_PAGE_AGENT(rs.getBigDecimal(UserAttributes.USER_SEARCH_PAGE_AGENT));
    	if(rs.getString(UserAttributes.USER_TSR_SORTBY)!=null) my.setTSR_SORTBY(rs.getString(UserAttributes.USER_TSR_SORTBY));
    	if(rs.getObject(UserAttributes.USER_TSR_UPPER_LOWER)!=null) my.setTSR_UPPER_LOWER(rs.getInt(UserAttributes.USER_TSR_UPPER_LOWER));
    	if(rs.getObject(UserAttributes.USER_TSR_NAME_FORMAT)!=null) my.setTSR_NAME_FORMAT(rs.getInt(UserAttributes.USER_TSR_NAME_FORMAT));
    	if(rs.getBigDecimal(UserAttributes.USER_TSR_COLORING)!=null) my.setTSR_COLORING(rs.getBigDecimal(UserAttributes.USER_TSR_COLORING));
    	if(rs.getString(UserAttributes.USER_DASHBOARD_STATE)!=null) my.setReportState(rs.getString(UserAttributes.USER_DASHBOARD_STATE));
    	if(rs.getString(UserAttributes.USER_DASHBOARD_COUNTY)!=null) my.setReportCounty(rs.getString(UserAttributes.USER_DASHBOARD_COUNTY));
    	if(rs.getString(UserAttributes.USER_DASHBOARD_ABSTRACTOR)!=null) my.setReportAbstractor(rs.getString(UserAttributes.USER_DASHBOARD_ABSTRACTOR));
    	if(rs.getString(UserAttributes.USER_DASHBOARD_AGENCY)!=null) my.setReportCompanyAgent(rs.getString(UserAttributes.USER_DASHBOARD_AGENCY));
    	if(rs.getString(UserAttributes.USER_DASHBOARD_AGENT)!=null) my.setReportAgent(rs.getString(UserAttributes.USER_DASHBOARD_AGENT));
    	if(rs.getString(UserAttributes.USER_DASHBOARD_STATUS)!=null) my.setReportStatus(rs.getString(UserAttributes.USER_DASHBOARD_STATUS));
    	if(rs.getString(UserAttributes.USER_DASHBOARD_VIEW)!=null) my.setReportDefaultView(rs.getString(UserAttributes.USER_DASHBOARD_VIEW));
    	if(rs.getString(UserAttributes.USER_DASHBOARD_SORTBY)!=null) my.setReportSortBy(rs.getString(UserAttributes.USER_DASHBOARD_SORTBY));
    	if(rs.getString(UserAttributes.USER_DASHBOARD_SORTDIR)!=null) my.setReportSortDir(rs.getString(UserAttributes.USER_DASHBOARD_SORTDIR));
    	try {
        	if(rs.getString(UserAttributes.USER_DASHBOARD_START_INTERVAL)!=null) 
        		my.setDASHBOARD_START_INTERVAL(rs.getString(UserAttributes.USER_DASHBOARD_START_INTERVAL));
        	if(rs.getString(UserAttributes.USER_DASHBOARD_END_INTERVAL)!=null) 
        		my.setDASHBOARD_END_INTERVAL(rs.getString(UserAttributes.USER_DASHBOARD_END_INTERVAL));
    	} catch(Exception e) {}
    	if(rs.getBigDecimal(UserAttributes.USER_DASHBOARD_ROWS_PER_PAGE)!=null) 
    		my.setDASHBOARD_ROWS_PER_PAGE(rs.getBigDecimal(UserAttributes.USER_DASHBOARD_ROWS_PER_PAGE));
    	if(rs.getString(UserAttributes.USER_DEFAULT_HOMEPAGE)!=null) 
    		my.setDEFAULT_HOMEPAGE(rs.getString(UserAttributes.USER_DEFAULT_HOMEPAGE));
    	if(rs.getObject(UserAttributes.USER_MY_ATS_READ_ONLY)!=null) 
    		my.setMY_ATS_READ_ONLY(rs.getInt(UserAttributes.USER_MY_ATS_READ_ONLY));
    	if(rs.getObject(UserAttributes.USER_RECEIVE_NOTIFICATION)!=null) 
    		my.setReceive_notification(rs.getInt(UserAttributes.USER_RECEIVE_NOTIFICATION));
    	if(rs.getObject(UserAttributes.USER_SEARCH_LOG_LINK)!=null) my.setSearch_log_link(rs.getInt(UserAttributes.USER_SEARCH_LOG_LINK));
    	if(rs.getObject(UserAttributes.USER_INVOICE_EDIT_EMAIL)!=null) my.setInvoiceEditEmail(rs.getInt(UserAttributes.USER_INVOICE_EDIT_EMAIL));
    	if(rs.getBigDecimal("pages")!=null) my.setPages(rs.getBigDecimal("pages"));
    	my.setTimestamp(rs.getTimestamp("timestamp"));
    	if(rs.getBigDecimal("default_state")!=null) my.setDefault_state(rs.getBigDecimal("default_state"));
    	if(rs.getBigDecimal("default_county")!=null) my.setDefault_county(rs.getBigDecimal("default_county"));
    	if(rs.getBigDecimal("default_county")!=null) my.setDefault_county(rs.getBigDecimal("default_county"));
        if(rs.getObject(UserAttributes.USER_TSR_PAGINATE)!=null)my.setPaginate_tsrindex(rs.getBigDecimal(UserAttributes.USER_TSR_PAGINATE));
        if(rs.getObject(UserAttributes.USER_LEGAL_CASE)!=null)my.setLegalCase(rs.getInt(UserAttributes.USER_LEGAL_CASE));
        if(rs.getObject(UserAttributes.USER_VESTING_CASE)!=null)my.setVestingCase(rs.getInt(UserAttributes.USER_VESTING_CASE));
        if(rs.getObject(UserAttributes.USER_ADDRESS_CASE)!=null)my.setAddressCase(rs.getInt(UserAttributes.USER_ADDRESS_CASE));
        try {
        	my.setAgentsSelectWidth(rs.getInt(UserAttributes.USER_DASHBOARD_AGENT_SELECT_WIDTH));
		} catch (Exception ignored) {
			
		} finally {
			if(my.getAgentsSelectWidth() <= 0 ) {
				my.setAgentsSelectWidth(DEFAULT_AGENTS_SELECT_WIDTH);
			}
		}
        if(rs.getObject(UserAttributes.USER_START_VIEW_DATE_VALUE) != null){
        	my.setStartViewDateValue(rs.getInt(UserAttributes.USER_START_VIEW_DATE_VALUE));
        }
        /* 	//do not read anymore the allowed products
        try {
        	if(rs.getString(UserAttributes.USER_ALLOWED_PRODUCTS)!=null) 
        		my.setAllowedProducts(rs.getString(UserAttributes.USER_ALLOWED_PRODUCTS));
        } catch (Exception e) {}
        */
    	return my;
    }
	
	public BigDecimal getUser_id() {
		return user_id;
	}

	public void setUser_id(BigDecimal user_id) {
		this.user_id = user_id;
	}

	public BigDecimal getSEARCH_PAGE_STATE() {
		return SEARCH_PAGE_STATE;
	}

	public void setSEARCH_PAGE_STATE(BigDecimal search_page_state) {
		SEARCH_PAGE_STATE = search_page_state;
	}

	public BigDecimal getSEARCH_PAGE_COUNTY() {
		return SEARCH_PAGE_COUNTY;
	}

	public void setSEARCH_PAGE_COUNTY(BigDecimal search_page_county) {
		SEARCH_PAGE_COUNTY = search_page_county;
	}

	public BigDecimal getSEARCH_PAGE_AGENT() {
		return SEARCH_PAGE_AGENT;
	}

	public void setSEARCH_PAGE_AGENT(BigDecimal search_page_agent) {
		SEARCH_PAGE_AGENT = search_page_agent;
	}

	public String getTSR_SORTBY() {
		return TSR_SORTBY;
	}

	public void setTSR_SORTBY(String tsr_sortby) {
		TSR_SORTBY = tsr_sortby;
	}

	public Integer getTSR_UPPER_LOWER() {
		return TSR_UPPER_LOWER;
	}

	public void setTSR_UPPER_LOWER(Integer tsr_upper_lower) {
		TSR_UPPER_LOWER = tsr_upper_lower;
	}

	public Integer getTSR_NAME_FORMAT() {
		return TSR_NAME_FORMAT;
	}

	public void setTSR_NAME_FORMAT(Integer tsr_name_format) {
		TSR_NAME_FORMAT = tsr_name_format;
	}

	public BigDecimal getTSR_COLORING() {
		return TSR_COLORING;
	}

	public void setTSR_COLORING(BigDecimal tsr_coloring) {
		TSR_COLORING = tsr_coloring;
	}

	public String getReportState() {
		return reportState;
	}

	public void setReportState(String reportState) {
		this.reportState = reportState;
	}

	public String getReportCounty() {
		return reportCounty;
	}

	public void setReportCounty(String reportCounty) {
		this.reportCounty = reportCounty;
	}

	public String getReportAbstractor() {
		return reportAbstractor;
	}

	public void setReportAbstractor(String reportAbstractor) {
		this.reportAbstractor = reportAbstractor;
	}

	public String getReportCompanyAgent() {
		return reportCompanyAgent;
	}

	public void setReportCompanyAgent(String reportCompanyAgent) {
		this.reportCompanyAgent = reportCompanyAgent;
	}

	public String getReportAgent() {
		return reportAgent;
	}

	public void setReportAgent(String reportAgent) {
		this.reportAgent = reportAgent;
	}

	public String getReportStatus() {
		return reportStatus;
	}

	public void setReportStatus(String reportStatus) {
		this.reportStatus = reportStatus;
	}

	public String getReportDefaultView() {
		return reportDefaultView;
	}

	public void setReportDefaultView(String reportDefaultView) {
		this.reportDefaultView = reportDefaultView;
	}

	public String getReportSortBy() {
		return reportSortBy;
	}

	public void setReportSortBy(String reportSortBy) {
		this.reportSortBy = reportSortBy;
	}

	public String getReportSortDir() {
		return reportSortDir;
	}

	public void setReportSortDir(String reportSortDir) {
		this.reportSortDir = reportSortDir;
	}

	public String getDASHBOARD_START_INTERVAL() {
		return DASHBOARD_START_INTERVAL;
	}

	public void setDASHBOARD_START_INTERVAL(String dashboard_start_interval) throws BaseException{
		if(!Util.isValidForStartInterval(dashboard_start_interval)) 
			throw new BaseException("Invalid Format for dashboard start interval ");
		DASHBOARD_START_INTERVAL = dashboard_start_interval;
	}

	public String getDASHBOARD_END_INTERVAL() {
		return DASHBOARD_END_INTERVAL;
	}

	public void setDASHBOARD_END_INTERVAL(String dashboard_end_interval) throws BaseException{
		if(!Util.isValidForStartInterval(dashboard_end_interval)) 
			throw new BaseException("Invalid Format for dashboard end interval ");
		DASHBOARD_END_INTERVAL = dashboard_end_interval;
	}

	public BigDecimal getDASHBOARD_ROWS_PER_PAGE() {
		return DASHBOARD_ROWS_PER_PAGE;
	}

	public void setDASHBOARD_ROWS_PER_PAGE(BigDecimal dashboard_rows_per_page) {
		DASHBOARD_ROWS_PER_PAGE = dashboard_rows_per_page;
	}

	public String getDEFAULT_HOMEPAGE() {
		return DEFAULT_HOMEPAGE;
	}

	public void setDEFAULT_HOMEPAGE(String default_homepage) {
		DEFAULT_HOMEPAGE = default_homepage;
	}

	public Integer getMY_ATS_READ_ONLY() {
		return MY_ATS_READ_ONLY;
	}

	public void setMY_ATS_READ_ONLY(Integer my_ats_read_only) {
		MY_ATS_READ_ONLY = my_ats_read_only;
	}

	public Integer getReceive_notification() {
		return receive_notification;
	}

	public void setReceive_notification(Integer receive_notification) {
		this.receive_notification = receive_notification;
	}

	public Integer getSearch_log_link() {
		return search_log_link;
	}

	public void setSearch_log_link(Integer search_log_link) {
		this.search_log_link = search_log_link;
	}

	
	public Integer getInvoiceEditEmail() {
		return invoiceEditEmail;
	}

	public void setInvoiceEditEmail(Integer invoiceEditEmail) {
		this.invoiceEditEmail = invoiceEditEmail;
	}
	
	public BigDecimal getPages() {
		return pages;
	}

	public void setPages(BigDecimal pages) {
		this.pages = pages;
	}

	public Timestamp getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}

	public BigDecimal getDefault_state() {
		return default_state;
	}

	public void setDefault_state(BigDecimal state) {
		this.default_state = state;
	}

	public BigDecimal getDefault_county() {
		return default_county;
	}

	public void setDefault_county(BigDecimal county) {
		this.default_county = county;
	}

	public UserAttributes getUa() {
		return ua;
	}

	public void setUa(UserAttributes ua) {
		this.ua = ua;
	}
	
	
	public NameFormaterI getTSRNameFormater() { 
		return getNameFormater(getTSR_UPPER_LOWER(),getTSR_NAME_FORMAT());
	}
	
	public static NameFormaterI getNameFormater(int nameCase, int nameFormat) { 
		NameFormater nf = new NameFormater(); 
	
		if(nameCase == DBConstants.NAMES_UPPERCASE) {
			nf.setTtype(TitleType.UPPER_CASE);
		}
		if(nameCase == DBConstants.NAMES_LOWERCASE) {
			nf.setTtype(TitleType.LOWER_CASE);
		}
		if(nameCase == DBConstants.NAMES_TITLECASE) {
			nf.setTtype(TitleType.TITLE_CASE);
		}
		if(nameCase == DBConstants.NAMES_NO_CHANGE) {
			nf.setTtype(TitleType.NO_CHANGE);
		}
		
		if(nameFormat == DBConstants.NAMES_FORMAT_FML) {
			nf.setPtype(PosType.FML);
		}
		if(nameFormat == DBConstants.NAMES_FORMAT_LFM) {
			nf.setPtype(PosType.LFM);
		}
		if(nameFormat == DBConstants.NAMES_NO_CHANGE) {
			nf.setPtype(PosType.NO_CHANGE);
		}
		
		return nf;
	}

	public SortBy getTSR_SORTBY_NEW() {
		if(TSR_SORTBY.equalsIgnoreCase("SORTBY_SRCTYPE")) 	return SortBy.SORT_BY_DATA_SOURCE;
		if(TSR_SORTBY.equalsIgnoreCase("SORTBY_DATE")) 		return SortBy.SORT_BY_DATE;
		if(TSR_SORTBY.equalsIgnoreCase("SORTBY_DATE_DESC")) return SortBy.SORT_BY_DATE_DESC;
		if(TSR_SORTBY.equalsIgnoreCase("SORTBY_GRANTOR")) 	return SortBy.SORT_BY_GRANTOR;
		if(TSR_SORTBY.equalsIgnoreCase("SORTBY_GRANTEE")) 	return SortBy.SORT_BY_GRANTEE;
		if(TSR_SORTBY.equalsIgnoreCase("SORTBY_INSTTYPE")) 	return SortBy.SORT_BY_TYPE;
		if(TSR_SORTBY.equalsIgnoreCase("SORTBY_INST")) 		return SortBy.SORT_BY_INSTRUMENT;
		
		return SortBy.SORT_BY_INSTRUMENT;
	}

	

	public BigDecimal getPaginate_tsrindex() {
		return paginate_tsrindex;
	}

	public void setPaginate_tsrindex(BigDecimal paginate_tsrindex) {
		this.paginate_tsrindex = paginate_tsrindex;
	}

	public Integer getLegalCase() {
		return legalCase;
	}

	public NameFormaterI.TitleType getLegalTitleType() {
		if(legalCase == DBConstants.NAMES_LOWERCASE) return NameFormaterI.TitleType.LOWER_CASE;
		if(legalCase == DBConstants.NAMES_UPPERCASE) return NameFormaterI.TitleType.UPPER_CASE;
		if(legalCase == DBConstants.NAMES_TITLECASE) return NameFormaterI.TitleType.TITLE_CASE;
		return NameFormaterI.TitleType.NO_CHANGE;
	}
	
	public void setLegalCase(Integer legalCase) {
		this.legalCase = legalCase;
	}
	
	public Integer getVestingCase() {
		return vestingCase;
	}

	public NameFormaterI.TitleType getVestingTitleType() {
		if(vestingCase == DBConstants.NAMES_LOWERCASE) return NameFormaterI.TitleType.LOWER_CASE;
		if(vestingCase == DBConstants.NAMES_UPPERCASE) return NameFormaterI.TitleType.UPPER_CASE;
		if(vestingCase == DBConstants.NAMES_TITLECASE) return NameFormaterI.TitleType.TITLE_CASE;
		return NameFormaterI.TitleType.NO_CHANGE;
	}
	
	public void setVestingCase(Integer vestingCase) {
		this.vestingCase = vestingCase;
	}

	public Integer getStartViewDateValue() {
		return startViewDateValue;
	}
	
	public void setStartViewDateValue(Integer startViewDateValue) {
		this.startViewDateValue = startViewDateValue;
	}
	
	public NameFormaterI.TitleType getAddressTitleType() {
		if(addressCase == DBConstants.NAMES_LOWERCASE) return NameFormaterI.TitleType.LOWER_CASE;
		if(addressCase == DBConstants.NAMES_UPPERCASE) return NameFormaterI.TitleType.UPPER_CASE;
		if(addressCase == DBConstants.NAMES_TITLECASE) return NameFormaterI.TitleType.TITLE_CASE;
		return NameFormaterI.TitleType.NO_CHANGE;
	}
	
	public Integer getAddressCase() {
		return addressCase;
	}

	public void setAddressCase(Integer addressCase) {
		this.addressCase = addressCase;
	}	
	
	public static NameFormaterI getNameFormatterForSearch(long searchId) {
		return getNameFormatterForSearch(searchId, null);
	}
	
	/**
	 * @see Bug 4989
	 * @param searchId
	 * @return
	 */
	public static NameFormaterI getNameFormatterForSearch(long searchId, UserAttributes agent) {
		
		try {
			Search s = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			UserAttributes abstractor = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
			
			if(agent==null){
				agent = s.getAgent();
			}
			
			NameFormaterI agentNf = null;
			if(agent!=null) {
				agentNf = agent.getMyAtsAttributes().getTSRNameFormater();
			}
			
			NameFormaterI abstractorNf = null; 
			if(abstractor!=null) {
				abstractorNf =abstractor.getMyAtsAttributes().getTSRNameFormater();
			}
			
			return getNameFormatterForSearch(searchId,abstractorNf,agentNf);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return new NameFormater(PosType.NO_CHANGE, TitleType.NO_CHANGE);
	}
	
	public static NameFormaterI getNameFormatterForDashboard(long searchId,UserAttributes abstractor,NameFormaterI agentNf) {
		NameFormaterI abstractorNf = null; 
		if(abstractor!=null) {
			abstractorNf =abstractor.getMyAtsAttributes().getTSRNameFormater();
		}
		return getNameFormatterForSearch(searchId,abstractorNf,agentNf);
	}
	
	/**
	 * @see Bug 4989
	 * @param searchId
	 * @return
	 */
	public static NameFormaterI getNameFormatterForSearch(long searchId, NameFormaterI abstractorNf,NameFormaterI agentNf) {
	
		NameFormater nf = new NameFormater(PosType.NO_CHANGE, TitleType.NO_CHANGE);
		
		try {						
	    	if(agentNf != null) {
	    		 if(agentNf.getPtype()!= NameFormaterI.PosType.NO_CHANGE) {
	    			nf.setPtype(agentNf.getPtype());
	    		 }
	    		 if(agentNf.getTtype()!= NameFormaterI.TitleType.NO_CHANGE) {
	    			nf.setTtype(agentNf.getTtype());
	    		 }
	    	}
	    	
	    	if(abstractorNf != null) {
		    	if(nf.getPtype()==NameFormaterI.PosType.NO_CHANGE) {
		    		nf.setPtype(abstractorNf.getPtype());
		    	}
		    	
		    	if(nf.getTtype()==NameFormaterI.TitleType.NO_CHANGE) {
		    		nf.setTtype(abstractorNf.getTtype());
		    	}
	    	}
	    		    	
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	return nf;
	}

	public int getAgentsSelectWidth() {
		return agentsSelectWidth;
	}

	public void setAgentsSelectWidth(int agentsSelectWidth) {
		this.agentsSelectWidth = agentsSelectWidth;
	}
	
	public static NameFormaterI.TitleType getLegalFormatterForSearch(long searchId) {
		return getLegalFormatterForSearch(searchId, null);
	}
	

	public static NameFormaterI.TitleType getLegalFormatterForSearch(long searchId, UserAttributes agent) {
		
		try {
			Search s = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			UserAttributes abstractor = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
			
			if(agent==null){
				agent = s.getAgent();
			}
			
			NameFormaterI.TitleType agentNf = null;
			if(agent!=null) {
				agentNf = agent.getMyAtsAttributes().getLegalTitleType();
				if(agentNf!=TitleType.NO_CHANGE) {
					return agentNf;
				}
			}
			
			NameFormaterI.TitleType abstractorNf = null; 
			if(abstractor!=null) {
				abstractorNf =abstractor.getMyAtsAttributes().getLegalTitleType();
				if(abstractorNf!=TitleType.NO_CHANGE) {
					return abstractorNf;
				}
			}

		}catch(Exception e) {
			e.printStackTrace();
		}
		return TitleType.NO_CHANGE;
	}
	
	public static NameFormaterI.TitleType getVestingFormatterForSearch(long searchId) {
		return getVestingFormatterForSearch(searchId, null);
	}
	

	public static NameFormaterI.TitleType getVestingFormatterForSearch(long searchId, UserAttributes agent) {
		
		try {
			Search s = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			UserAttributes abstractor = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
			
			if(agent==null){
				agent = s.getAgent();
			}
			
			NameFormaterI.TitleType agentNf = null;
			if(agent!=null) {
				agentNf = agent.getMyAtsAttributes().getVestingTitleType();
				if(agentNf!=TitleType.NO_CHANGE) {
					return agentNf;
				}
			}
			
			NameFormaterI.TitleType abstractorNf = null; 
			if(abstractor!=null) {
				abstractorNf =abstractor.getMyAtsAttributes().getVestingTitleType();
				if(abstractorNf!=TitleType.NO_CHANGE) {
					return abstractorNf;
				}
			}

		}catch(Exception e) {
			e.printStackTrace();
		}
		return TitleType.NO_CHANGE;
	}
	
	public static NameFormaterI.TitleType getAddressFormatterForSearch(long searchId) {
		return getAddressFormatterForSearch(searchId, null);
	}
	

	public static NameFormaterI.TitleType getAddressFormatterForSearch(long searchId, UserAttributes agent) {
		
		try {
			Search s = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
			UserAttributes abstractor = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
			
			if(agent==null){
				agent = s.getAgent();
			}
			
			NameFormaterI.TitleType agentNf = null;
			if(agent!=null) {
				agentNf = agent.getMyAtsAttributes().getAddressTitleType();
				if(agentNf!=TitleType.NO_CHANGE) {
					return agentNf;
				}
			}
			
			NameFormaterI.TitleType abstractorNf = null; 
			if(abstractor!=null) {
				abstractorNf =abstractor.getMyAtsAttributes().getAddressTitleType();
				if(abstractorNf!=TitleType.NO_CHANGE) {
					return abstractorNf;
				}
			}

		}catch(Exception e) {
			e.printStackTrace();
		}
		return TitleType.NO_CHANGE;
	}
	
}