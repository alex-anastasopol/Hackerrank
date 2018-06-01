package ro.cst.tsearch.servers.bean.tims;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;

public class Payload {
	
	private Search search;
	
	private String InternalAccountIdentifier = "ST";
	private String OfficeIdentifier = "";
	private String ProcessorIdentifier = "";
	private String VendorOrderIdentifier;
	private String OrderType = "OO";
	private String VendorTransactionIdentifier = "";
	private String Underwriter = "";
	private String Examiner = "";
	private String Priority = "1";
	private String CreateStarter = "Y";
	
	private List<PayloadName> sellers = new ArrayList<PayloadName>();
	private List<PayloadName> borrowers = new ArrayList<PayloadName>();
	
	private String LOAN_PURPOSE_Type = "";

	private String SUBMITTING_PARTY_Name = "";
	
	private String TAX_INFOOrderTaxes = "N";
	private String TAX_INFOTaxServiceName = "";
	private String TAX_INFOTAX_CODECode = "";
	
	public Payload(Search search) {
		this.search = search;
		VendorOrderIdentifier = calculateVendorOrderIdentifier();
	}
	
	private PayloadProperty property = new PayloadProperty();
	
	public void loadDataFromSearch(boolean useInitialOrderOnly) {
		
		
		
		for (NameI name : search.getSa().getOwners().getNames()) {
			if(useInitialOrderOnly) {
				if(name.getNameFlags().isNewFromOrder()) {
					sellers.add(new PayloadName(name));	
				}
			} else {
				sellers.add(new PayloadName(name));	
			}
		}
		for (NameI name : search.getSa().getBuyers().getNames()) {
			if(useInitialOrderOnly) {
				if(name.getNameFlags().isNewFromOrder()) {
					sellers.add(new PayloadName(name, false));
				}
			} else {
				sellers.add(new PayloadName(name, false));
			}
		}
		
		property.loadDataFromSearch(search, useInitialOrderOnly);

		UserAttributes agent = search.getAgent();
		if(agent != null) {
			SUBMITTING_PARTY_Name = (agent.getFIRSTNAME() + " " + agent.getLASTNAME()).trim();
		}
		
	}
	
	public void loadDataFromRequest(HTTPRequest request) {
		
		LinkedHashSet<NameI> owners = new LinkedHashSet<NameI>();
		for (int i = 0; i < 3; i++) {
			String lastName = request.getPostFirstParameter("oLast" + i);
			String options = request.getPostFirstParameter("oOptions" + i);
			if("Yes".equals(options)) {
				if(StringUtils.isNotEmpty(lastName)) {
					NameI name = new Name();
					name.setCompany(true);
					name.setLastName(lastName);
					owners.add(name);
				}
			} else {
				if(StringUtils.isNotEmpty(lastName)) {
					NameI name = new Name();
					name.setLastName(lastName);
					String firstName = request.getPostFirstParameter("oFirst" + i);
					String middleName = request.getPostFirstParameter("oFirst" + i);
					if(StringUtils.isNotEmpty(firstName)) {
						name.setFirstName(firstName.trim());
					}
					if(StringUtils.isNotEmpty(middleName)) {
						name.setMiddleName(middleName);
					}
					owners.add(name);
				}
			}
		}
		for (NameI name : owners) {
			sellers.add(new PayloadName(name));
		}
		
		LinkedHashSet<NameI> buyers = new LinkedHashSet<NameI>();
		for (int i = 0; i < 3; i++) {
			String lastName = request.getPostFirstParameter("bLast" + i);
			String options = request.getPostFirstParameter("bOptions" + i);
			if("Yes".equals(options)) {
				if(StringUtils.isNotEmpty(lastName)) {
					NameI name = new Name();
					name.setCompany(true);
					name.setLastName(lastName);
					buyers.add(name);
				}
			} else {
				if(StringUtils.isNotEmpty(lastName)) {
					NameI name = new Name();
					name.setLastName(lastName);
					String firstName = request.getPostFirstParameter("bFirst" + i);
					String middleName = request.getPostFirstParameter("bFirst" + i);
					if(StringUtils.isNotEmpty(firstName)) {
						name.setFirstName(firstName.trim());
					}
					if(StringUtils.isNotEmpty(middleName)) {
						name.setMiddleName(middleName);
					}
					buyers.add(name);
				}
			}
		}
		for (NameI name : buyers) {
			sellers.add(new PayloadName(name, false));
		}
		
		property.loadDataFromRequest(search, request);
		
		UserAttributes agent = search.getAgent();
		if(agent != null) {
			SUBMITTING_PARTY_Name = (agent.getFIRSTNAME() + " " + agent.getLASTNAME()).trim();
		}
	}

	public String calculateVendorOrderIdentifier() {
		String searchName = search.getAbstractorFileNo().replace(".", "");
		if(searchName.length() > 15) {
			searchName = searchName.substring(0, 15);
		} else {
			int commId = search.getCommId();
			
			if(commId == 3 || commId == 4) {
				long originalSearchId = search.getSa().getOriginalSearchId();
				
				if(originalSearchId<=0){
					originalSearchId = search.getID();
				}
				String searchIdString = Long.toString(originalSearchId);
				
				searchName += "_";
				
				if( searchName.length() + searchIdString.length() > 15 ) {
					searchName += searchIdString.substring(searchIdString.length() - (15 - searchName.length()));
				} else {
					searchName += searchIdString;
				}
			}
		}
		return searchName;
	}
	
	public String toXm() {
		StringBuilder xml = new StringBuilder();
		xml
			.append("<REQUEST InternalAccountIdentifier=\"").append(StringEscapeUtils.escapeXml(InternalAccountIdentifier)).append("\">")
			
			.append("<TITLE_REQUEST OfficeIdentifier=\"").append(StringEscapeUtils.escapeXml(OfficeIdentifier)).append("\" ")
			.append("ProcessorIdentifier=\"").append(StringEscapeUtils.escapeXml(ProcessorIdentifier)).append("\" ")
			.append("VendorOrderIdentifier=\"").append(StringEscapeUtils.escapeXml(VendorOrderIdentifier)).append("\" ")
			.append("OrderType=\"").append(StringEscapeUtils.escapeXml(OrderType)).append("\" ")
			.append("VendorTransactionIdentifier=\"").append(StringEscapeUtils.escapeXml(VendorTransactionIdentifier)).append("\" ")
			.append("Underwriter=\"").append(StringEscapeUtils.escapeXml(Underwriter)).append("\" ")
			.append("Examiner=\"").append(StringEscapeUtils.escapeXml(Examiner)).append("\" ")
			.append("Priority=\"").append(StringEscapeUtils.escapeXml(Priority)).append("\" ")
			.append("CreateStarter=\"").append(StringEscapeUtils.escapeXml(CreateStarter)).append("\">");
			
		for (PayloadName name : borrowers) {
			xml.append(name.toXml());
		}
			
			
		xml
			.append("<LOAN_PURPOSE _Type=\"").append(StringEscapeUtils.escapeXml(LOAN_PURPOSE_Type)).append("\"/>")
			
			.append(property.toXml());
		for (PayloadName name : sellers) {
			xml.append(name.toXml());
		}
		xml	
			.append("<SUBMITTING_PARTY _Name=\"").append(StringEscapeUtils.escapeXml(SUBMITTING_PARTY_Name)).append("\"/>")
			
			.append("<TAX_INFO OrderTaxes=\"").append(StringEscapeUtils.escapeXml(TAX_INFOOrderTaxes)).append("\" ")
			.append("TaxServiceName=\"").append(StringEscapeUtils.escapeXml(TAX_INFOTaxServiceName)).append("\">")
			.append("<TAX_CODE Code=\"").append(StringEscapeUtils.escapeXml(TAX_INFOTAX_CODECode)).append("\"/>")
			.append("</TAX_INFO>")
			.append("</TITLE_REQUEST>")
			.append("</REQUEST>");
			
		return xml.toString();

	}

	public String getVendorOrderIdentifier() {
		return VendorOrderIdentifier;
	}

	
}
