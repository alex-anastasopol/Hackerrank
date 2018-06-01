package ro.cst.tsearch.search.filter.newfilters.misc;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.emailOrder.MailOrder;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.FormatDate;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.PriorFileAtsDocument;

public class PriorFileAtsFilter extends FilterResponse {

	/**
	 * mihai b
	 * 
	 */
	private static final long serialVersionUID = 13434;
	
	private Long 	filteringAgentUserId;
	private String 	filteringAgentUserName 	= null;
	private String 	filteringState 			= null;
	private Date 	filteringDate 			= null;
	private String 	filteringDateString		= null;

	public PriorFileAtsFilter(long searchId) {
		super(searchId);
		setThreshold(ATSDecimalNumberFormat.ONE);
		
		filteringState = ServerConfig.getUsePriorAtsFilterForStates();
		filteringAgentUserName = ServerConfig.getPriorAtsFilterAgentRestriction();
		
		try {
			if (StringUtils.isNotEmpty(filteringAgentUserName)) {
				User user = MailOrder.getUser(filteringAgentUserName);
				if (user != null) {
					filteringAgentUserId = user.getUserAttributes().getID().longValue();
				}
			}
		} catch (Exception e) {}
		
		try {
			filteringDateString = ServerConfig.getPriorAtsFilterDateRestriction();
			if (StringUtils.isNotEmpty(filteringDateString)) {
				filteringDate = FormatDate.getDateFromFormatedString(filteringDateString, FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY);
			}
		} catch (Exception e) {}
		
	}

	@Override
	public BigDecimal getScoreOneRow(ParsedResponse row) {
		
		if (filteringAgentUserName == null || filteringDate == null || filteringState == null){
			return ATSDecimalNumberFormat.ONE; 
		}
		DocumentI document = row.getDocument();
		
		if (document != null) {
				
			boolean invalidByAgent = false, invalidByDate = false;
				
			if (document instanceof PriorFileAtsDocument){
				PriorFileAtsDocument pfaDocument = (PriorFileAtsDocument) document;
				Long docAgentId = pfaDocument.getAgentId();
				if (docAgentId != null && docAgentId.equals(filteringAgentUserId)){
					invalidByAgent = true;
				}
					
				if (pfaDocument.getTsrDate() != null){
					if (pfaDocument.getTsrDate().before(filteringDate)){
						invalidByDate = true;
					}
				} else if (pfaDocument.getRecordedDate() != null){
					if (pfaDocument.getRecordedDate().before(filteringDate)){
						invalidByDate = true;
					}
				} else if (pfaDocument.getInstrumentDate() != null){
					if (pfaDocument.getInstrumentDate().before(filteringDate)){
						invalidByDate = true;
					}
				}
					
				if (invalidByAgent && invalidByDate){
					return ATSDecimalNumberFormat.ZERO;
				} else{
					return ATSDecimalNumberFormat.ONE;
				}
			}
		}
		return ATSDecimalNumberFormat.ONE;
	}
	
	@Override
	public String getFilterCriteria() {
		return "PriorFileAtsFilter. Reject documents with Agent=" + filteringAgentUserName + "; prior to " + filteringDateString;
	}

	@Override
	public String getFilterName() {
		return "Filter by AgentId/Date";
	}

}
