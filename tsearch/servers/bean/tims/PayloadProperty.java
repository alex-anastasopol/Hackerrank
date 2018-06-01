package ro.cst.tsearch.servers.bean.tims;

import org.apache.commons.lang.StringEscapeUtils;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.property.PropertyI;

public class PayloadProperty {
	
	private String _City = "";
	private String _State = "";
	private String _County = "";
	private String _PostalCode = "";
	
	private String _TextDescription = "";
	private String LegalStructure = "";
	private String _Type = "";
	private String _Identifier = "";
	private String TaxID = "";
	private String PropertyLotIdentifier_LOW = "";
	private String PropertyBlockIdentifier_LOW = "";
	private String PropertyLotIdentifier_HIGH = "";
	private String PropertyBlockIdentifier_HIGH = "";
	private String PropertySectionIdentifier = "";
	private String PropertySubdivisionIdentifier = "";
	private String PlatName = "";
	private String RecordedDocumentBook = "";
	private String RecordedDocumentPage = "";
	private String _DescriptionType = "";
	private String DescriptionTypeOtherDescription = "";
	private String _MetesAndBoundsRemainingDescription = "";
	private String OrderNumber = "";
	private String DocNumber = "";
	private String FilmCode = "";
	
	private String _StreetName = "";
	private String _HouseNumber = "";
	private String _ApartmentOrUnit = "";
	
	public String toXml() {
		StringBuilder sb = new StringBuilder();
		sb
			.append("<PROPERTY _City=\"").append(StringEscapeUtils.escapeXml(_City)).append("\" ")
			.append("_State=\"").append(StringEscapeUtils.escapeXml(_State)).append("\" ")
			.append("_County=\"").append(StringEscapeUtils.escapeXml(_County)).append("\" ")
			.append("_PostalCode=\"").append(StringEscapeUtils.escapeXml(_PostalCode)).append("\">\n")
			.append("<_LEGAL_DESCRIPTION _TextDescription=\"").append(StringEscapeUtils.escapeXml(_TextDescription)).append("\" ")
			.append("LegalStructure=\"").append(StringEscapeUtils.escapeXml(LegalStructure)).append("\">")
			.append("<PARCEL_IDENTIFICATION _Type=\"").append(StringEscapeUtils.escapeXml(_Type)).append("\" ")
			.append("_Identifier=\"").append(StringEscapeUtils.escapeXml(_Identifier)).append("\" ")
			.append("TaxID=\"").append(StringEscapeUtils.escapeXml(TaxID)).append("\"/>")
			.append("<PLATTED_LAND PropertyLotIdentifier_LOW=\"").append(StringEscapeUtils.escapeXml(PropertyLotIdentifier_LOW)).append("\" ")
			.append("PropertyBlockIdentifier_LOW=\"").append(StringEscapeUtils.escapeXml(PropertyBlockIdentifier_LOW)).append("\" ")
			.append("PropertyLotIdentifier_HIGH=\"").append(StringEscapeUtils.escapeXml(PropertyLotIdentifier_HIGH)).append("\" ")
			.append("PropertyBlockIdentifier_HIGH=\"").append(StringEscapeUtils.escapeXml(PropertyBlockIdentifier_HIGH)).append("\" ")
			.append("PropertySectionIdentifier=\"").append(StringEscapeUtils.escapeXml(PropertySectionIdentifier)).append("\" ")
			.append("PropertySubdivisionIdentifier=\"").append(StringEscapeUtils.escapeXml(PropertySubdivisionIdentifier)).append("\" ")
			.append("PlatName=\"").append(StringEscapeUtils.escapeXml(PlatName)).append("\" ")
			.append("RecordedDocumentBook=\"").append(StringEscapeUtils.escapeXml(RecordedDocumentBook)).append("\" ")
			.append("RecordedDocumentPage=\"").append(StringEscapeUtils.escapeXml(RecordedDocumentPage)).append("\"/>")
			.append("<UNPLATTED-LAND _DescriptionType=\"").append(StringEscapeUtils.escapeXml(_DescriptionType)).append("\" ")
			.append("DescriptionTypeOtherDescription=\"").append(StringEscapeUtils.escapeXml(DescriptionTypeOtherDescription)).append("\" ")
			.append("_MetesAndBoundsRemainingDescription=\"").append(StringEscapeUtils.escapeXml(_MetesAndBoundsRemainingDescription)).append("\"/>")
			.append("<PRIOR OrderNumber=\"").append(StringEscapeUtils.escapeXml(OrderNumber)).append("\" ")
			.append("DocNumber=\"").append(StringEscapeUtils.escapeXml(DocNumber)).append("\" ")
			.append("FilmCode=\"").append(StringEscapeUtils.escapeXml(FilmCode)).append("\"/>")
			.append("</_LEGAL_DESCRIPTION>\n")
			.append("<PARSED_STREET_ADDRESS _StreetName=\"").append(StringEscapeUtils.escapeXml(_StreetName)).append("\" ")
			.append("_HouseNumber=\"").append(StringEscapeUtils.escapeXml(_HouseNumber)).append("\" ")
			.append("_ApartmentOrUnit=\"").append(StringEscapeUtils.escapeXml(_ApartmentOrUnit)).append("\"/>\n")
			.append("</PROPERTY>\n");
			
		return sb.toString();
	}

	public void loadDataFromSearch(Search search, boolean useInitialOrderOnly) {
		
		SearchAttributes sa = search.getSa();
		
		
		_State = sa.getAtribute(SearchAttributes.P_STATE_ABREV);
		_County = sa.getAtribute(SearchAttributes.P_COUNTY_FIPS);
		
		_Type = "TaxParcelIdentifier";
		PropertyI orderProperty = sa.getOrderProperty();
		if(useInitialOrderOnly && orderProperty!=null) {
			
			AddressI address = orderProperty.getAddress();
			if(address != null) {
				_StreetName = org.apache.commons.lang.StringUtils.defaultString(address.getStreetName());
				_HouseNumber = org.apache.commons.lang.StringUtils.defaultString(address.getNumber());
				_City = org.apache.commons.lang.StringUtils.defaultString(address.getCity());
				_PostalCode = org.apache.commons.lang.StringUtils.defaultString(address.getZip());
			}
			
			LegalI legal = orderProperty.getLegal();
			if(legal != null) {
				SubdivisionI subdivision = legal.getSubdivision();
				if(subdivision != null) {
					PlatName = org.apache.commons.lang.StringUtils.defaultString(subdivision.getName());
					PropertyLotIdentifier_LOW = org.apache.commons.lang.StringUtils.defaultString(subdivision.getLot());
					PropertyLotIdentifier_HIGH = org.apache.commons.lang.StringUtils.defaultString(subdivision.getLotThrough());
					PropertyBlockIdentifier_LOW = org.apache.commons.lang.StringUtils.defaultString(subdivision.getBlock());
					PropertyBlockIdentifier_HIGH = org.apache.commons.lang.StringUtils.defaultString(subdivision.getBlockThrough());
					RecordedDocumentBook = org.apache.commons.lang.StringUtils.defaultString(subdivision.getPlatBook());
					RecordedDocumentPage = org.apache.commons.lang.StringUtils.defaultString(subdivision.getPlatPage());
				}
				TownShipI townShip = legal.getTownShip();
				if(townShip != null) {
					PropertySectionIdentifier = org.apache.commons.lang.StringUtils.defaultString(townShip.getSection());
				}
			
			}

			
		} else {
		
			_City = sa.getAtribute(SearchAttributes.P_CITY);
			_PostalCode = sa.getAtribute(SearchAttributes.P_ZIP);
			
			
			
			boolean hasSubdividedLegal = false;
			
			PlatName = sa.getAtribute(SearchAttributes.LD_SUBDIV_NAME);
			if (StringUtils.isNotEmpty(PlatName)) {
				hasSubdividedLegal = true;
			}
			
			String lot = sa.getAtribute(SearchAttributes.LD_LOTNO);
			if(StringUtils.isNotEmpty(lot)) {
				hasSubdividedLegal = true;
				if(lot.matches("\\s*\\d+\\s*-\\s*\\d+\\s*")) {
					PropertyLotIdentifier_LOW = lot.replaceAll("\\s*(\\d+)\\s*-\\s*\\d+\\s*", "$1");
					PropertyLotIdentifier_HIGH = lot.replaceAll("\\s*\\d+\\s*-\\s*(\\d+)\\s*", "$1");
				} else {
					PropertyLotIdentifier_LOW = lot;
				}
			}
			
			String block = sa.getAtribute(SearchAttributes.LD_SUBDIV_BLOCK);
			if(StringUtils.isNotEmpty(lot)) {
				hasSubdividedLegal = true;
				if(block.matches("\\s*\\d+\\s*-\\s*\\d+\\s*")) {
					PropertyBlockIdentifier_LOW = block.replaceAll("\\s*(\\d+)\\s*-\\s*\\d+\\s*", "$1");
					PropertyBlockIdentifier_HIGH = block.replaceAll("\\s*\\d+\\s*-\\s*(\\d+)\\s*", "$1");
				} else {
					PropertyBlockIdentifier_LOW = block;
				}
			}
			
			PropertySectionIdentifier = sa.getAtribute(SearchAttributes.LD_SUBDIV_SEC);
			if(!hasSubdividedLegal && StringUtils.isNotEmpty(PropertySectionIdentifier)) {
				hasSubdividedLegal = true;
			}
			RecordedDocumentBook = sa.getAtribute(SearchAttributes.LD_BOOKNO);
			if(!hasSubdividedLegal && StringUtils.isNotEmpty(RecordedDocumentBook)) {
				hasSubdividedLegal = true;
			}
			RecordedDocumentPage = sa.getAtribute(SearchAttributes.LD_PAGENO);
			if(!hasSubdividedLegal && StringUtils.isNotEmpty(RecordedDocumentPage)) {
				hasSubdividedLegal = true;
			}
			
			_StreetName = sa.getAtribute(SearchAttributes.P_STREETNAME);
			_HouseNumber = sa.getAtribute(SearchAttributes.P_STREETNO);
		}
		
		//if(hasSubdividedLegal) {
			LegalStructure = "S";
		//}
			
		
		
	}

	public void loadDataFromRequest(Search search, HTTPRequest request) {
		SearchAttributes sa = search.getSa();
		
		_State = sa.getAtribute(SearchAttributes.P_STATE_ABREV);
		_County = sa.getAtribute(SearchAttributes.P_COUNTY_FIPS);
		
		_City = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("City"));
		_PostalCode = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("PostalCode"));
		_StreetName = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("StreetName"));
		_HouseNumber = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("HouseNumber"));
		_ApartmentOrUnit = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("ApartmentOrUnit"));
		
		_Type = "TaxParcelIdentifier";
		TaxID = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("TaxID"));
		
		PlatName = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("PlatName"));
		PropertyLotIdentifier_LOW = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("PropertyLotIdentifier_LOW"));
		PropertyLotIdentifier_HIGH = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("PropertyLotIdentifier_HIGH"));
		PropertyBlockIdentifier_LOW = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("PropertyBlockIdentifier_LOW"));
		PropertyBlockIdentifier_HIGH = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("PropertyBlockIdentifier_HIGH"));
		PropertySectionIdentifier = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("PropertySectionIdentifier"));
		PropertySubdivisionIdentifier = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("PropertySubdivisionIdentifier"));
		RecordedDocumentBook = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("RecordedDocumentBook"));
		RecordedDocumentPage = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("RecordedDocumentPage"));
		LegalStructure = "S";
		
		_DescriptionType = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("DescriptionType"));
		DescriptionTypeOtherDescription = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("DescriptionTypeOtherDescription"));
		_MetesAndBoundsRemainingDescription = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("MetesAndBoundsRemainingDescription"));
		
		OrderNumber = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("OrderNumber"));
		DocNumber = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("DocNumber"));
		FilmCode = org.apache.commons.lang.StringUtils.defaultString(
				request.getPostFirstParameter("FilmCode"));
		
	}

}
