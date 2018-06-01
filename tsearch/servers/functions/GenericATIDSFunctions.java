package ro.cst.tsearch.servers.functions;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.servers.TSConnectionURL;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.bean.DocumentParsedResponse;
import ro.cst.tsearch.servers.info.TSServerInfo;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servers.types.GenericATIDS;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.StringUtils;

import com.atids.services.AtidsS2SServiceStub.ArrayOfDocumentReference;
import com.atids.services.AtidsS2SServiceStub.ArrayOfLegalEntityName;
import com.atids.services.AtidsS2SServiceStub.ArrayOfParty;
import com.atids.services.AtidsS2SServiceStub.ArrayOfPlattedLegal;
import com.atids.services.AtidsS2SServiceStub.ArrayOfRecordingNumberReference;
import com.atids.services.AtidsS2SServiceStub.ArrayOfSubdivisionLevels;
import com.atids.services.AtidsS2SServiceStub.ArrayOfTaxFolioReference;
import com.atids.services.AtidsS2SServiceStub.ArrayOfUnplattedLegal;
import com.atids.services.AtidsS2SServiceStub.AuthorizedPlatLevel;
import com.atids.services.AtidsS2SServiceStub.BookPageReference;
import com.atids.services.AtidsS2SServiceStub.DocumentReference;
import com.atids.services.AtidsS2SServiceStub.GetConveyanceDocumentInformationResponse;
import com.atids.services.AtidsS2SServiceStub.GetDocumentImageAvailabilityResponse;
import com.atids.services.AtidsS2SServiceStub.LegalEntityName;
import com.atids.services.AtidsS2SServiceStub.LegalEntityNameType;
import com.atids.services.AtidsS2SServiceStub.OfficialRecordDocument;
import com.atids.services.AtidsS2SServiceStub.Party;
import com.atids.services.AtidsS2SServiceStub.PartyRoleType;
import com.atids.services.AtidsS2SServiceStub.PlatInformation;
import com.atids.services.AtidsS2SServiceStub.PlatReference;
import com.atids.services.AtidsS2SServiceStub.PlattedLegal;
import com.atids.services.AtidsS2SServiceStub.RecordingNumberReference;
import com.atids.services.AtidsS2SServiceStub.SearchMatch;
import com.atids.services.AtidsS2SServiceStub.SectionBreakdownCode;
import com.atids.services.AtidsS2SServiceStub.SubdivisionLevels;
import com.atids.services.AtidsS2SServiceStub.TaxFolioReference;
import com.atids.services.AtidsS2SServiceStub.UnplattedLegal;
import com.stewart.ats.base.document.Ccer;
import com.stewart.ats.base.document.Corporation;
import com.stewart.ats.base.document.Court;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.Lien;
import com.stewart.ats.base.document.Mortgage;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.Transfer;
import com.stewart.ats.base.legal.SubdivisionDetailed;
import com.stewart.ats.base.legal.SubdivisionDetailedI;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.name.NameMortgageGrantee;
import com.stewart.ats.base.name.NameMortgageGranteeI;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.PinI;
import com.stewart.ats.base.property.PinI.PinType;
import com.stewart.ats.base.property.Property;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.SavedFromType;

public class GenericATIDSFunctions {
	
public static Map<String, String[]> SOURCES = new HashMap<String, String[]>();
	
	static {
		SOURCES.put("AJ", new String[]{DocumentTypes.ASSIGNMENT, "Assignment"});
		SOURCES.put("AM", new String[]{DocumentTypes.ASSIGNMENT, "Assignment"});
		SOURCES.put("AS", new String[]{DocumentTypes.ASSIGNMENT, "Assignment"});
		
		SOURCES.put("I", new String[]{DocumentTypes.CORPORATION, "Corporation"});
		SOURCES.put("ON", new String[]{DocumentTypes.CORPORATION, "Corporation"});
		
		SOURCES.put("CA", new String[]{DocumentTypes.COURT, "Court"});
		SOURCES.put("CC", new String[]{DocumentTypes.COURT, "Court"});
		SOURCES.put("CM", new String[]{DocumentTypes.COURT, "Court"});
		SOURCES.put("CO", new String[]{DocumentTypes.COURT, "Court"});
		SOURCES.put("CR", new String[]{DocumentTypes.COURT, "Court"});
		SOURCES.put("FC", new String[]{DocumentTypes.COURT, "Court"});
		SOURCES.put("FR", new String[]{DocumentTypes.COURT, "Court"});
		SOURCES.put("JC", new String[]{DocumentTypes.COURT, "Court"});
		SOURCES.put("LA", new String[]{DocumentTypes.COURT, "Court"});
		SOURCES.put("OJ", new String[]{DocumentTypes.COURT, "Court"});
		SOURCES.put("SC", new String[]{DocumentTypes.COURT, "Court"});
		SOURCES.put("AC", new String[]{DocumentTypes.COURT, "Court"});
		SOURCES.put("SU", new String[]{DocumentTypes.COURT, "Court"});
		
		SOURCES.put("CP", new String[]{DocumentTypes.COURT, "Probate"});
		SOURCES.put("PG", new String[]{DocumentTypes.COURT, "Probate"});
		
		SOURCES.put("FB", new String[]{DocumentTypes.COURT, "Bankrupcy"});
		
		SOURCES.put("FJ", new String[]{DocumentTypes.COURT, "Judgment"});
		SOURCES.put("JF", new String[]{DocumentTypes.COURT, "Judgment"});
		
		SOURCES.put("GS", new String[]{DocumentTypes.COURT, "Guardianship"});
		
		SOURCES.put("FL", new String[]{DocumentTypes.LIEN, "Federal Tax Lien"});
		SOURCES.put("HL", new String[]{DocumentTypes.LIEN, "Lien"});
		SOURCES.put("JL", new String[]{DocumentTypes.LIEN, "Lien"});
		SOURCES.put("L", new String[]{DocumentTypes.LIEN, "Lien"});
		SOURCES.put("NC", new String[]{DocumentTypes.LIEN, "Notice of Commencement"});
		SOURCES.put("UC", new String[]{DocumentTypes.LIEN, "UCC"});
		
		SOURCES.put("M", new String[]{DocumentTypes.MORTGAGE, "Mortgage"});
		SOURCES.put("MB", new String[]{DocumentTypes.MORTGAGE, "Mortgage"});
		
		SOURCES.put("CB", new String[]{DocumentTypes.PLAT, "Condominium_map"});
		SOURCES.put("MP", new String[]{DocumentTypes.PLAT, "Plat"});
		SOURCES.put("PB", new String[]{DocumentTypes.PLAT, "Plat_map"});
		SOURCES.put("NP", new String[]{DocumentTypes.PLAT, "Other"});
		SOURCES.put("RP", new String[]{DocumentTypes.PLAT, "Other"});
		SOURCES.put("SB", new String[]{DocumentTypes.PLAT, "Other"});
		SOURCES.put("UN", new String[]{DocumentTypes.PLAT, "Other"});
		
		SOURCES.put("SJ", new String[]{DocumentTypes.RELEASE, "Release"});
		SOURCES.put("SM", new String[]{DocumentTypes.RELEASE, "Release"});
		
		SOURCES.put("D", new String[]{DocumentTypes.TRANSFER, "Transfer"});
		SOURCES.put("DB", new String[]{DocumentTypes.TRANSFER, "Transfer"});
		SOURCES.put("WB", new String[]{DocumentTypes.TRANSFER, "Will"});
		SOURCES.put("TC", new String[]{DocumentTypes.TRANSFER, "Other Deeds"});
		SOURCES.put("TS", new String[]{DocumentTypes.TRANSFER, "Other Deeds"});
		
	}

	public static RegisterDocumentI getDocument(SearchMatch searchMatch, Search search, DataSite dataSite, TSServerInfoModule module){
		if(searchMatch == null) {
			return null;
		}
		
		return getDocument(searchMatch.getOfficialRecordDocument(), search, dataSite, module);
	}
	
	
	public static RegisterDocumentI getDocument(OfficialRecordDocument officialRecordDocument,
			Search search, DataSite dataSite, TSServerInfoModule module) {
		
		long searchId = search.getID();
		
		if(officialRecordDocument == null) {
			return null;
		}
		InstrumentI instr = getInstrumentFrom(officialRecordDocument.getDocumentReference(), dataSite);
		
		String serverDocType = officialRecordDocument.getTypeOfInstrument().replaceAll("\\s+", "");
		String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId); 
    	instr.setDocType(docCateg);
    	String stype = DocumentTypes.getDocumentSubcategory(serverDocType, searchId);
    	if("MISCELLANEOUS".equals(stype)&&!"MISCELLANEOUS".equals(docCateg)){
    		stype = docCateg;
    	}
    	instr.setDocSubType(stype);
		
    	RegisterDocument docR = new RegisterDocument( DocumentsManager.generateDocumentUniqueId(searchId, instr) );
    	
    	if(module != null && org.apache.commons.lang.StringUtils.isNotBlank(module.getSearchType())) {
    		docR.setSearchType(SearchType.valueOf(module.getSearchType()));	
		}
    	
		docR.setInstrument(instr);
		
		docR.setServerDocType(serverDocType);
    	docR.setType(SimpleChapterUtils.DType.ROLIKE);
    	
    	if(StringUtils.isNotEmpty(officialRecordDocument.getToiAdditionalInformation())) {
    		docR.setShortServerDocType(officialRecordDocument.getToiAdditionalInformation());
    	}
    	
    	docR.setDataSource(dataSite.getSiteTypeAbrev());
    	docR.setSiteId((int)dataSite.getServerId());
    	
		if(officialRecordDocument.getDateOfFile() != null) {
			docR.setRecordedDate(officialRecordDocument.getDateOfFile().getTime());
		}
		if(officialRecordDocument.getDateOfInstrument() != null && officialRecordDocument.getDateOfInstrument().get(Calendar.YEAR) != 1900) {
			docR.setInstrumentDate(officialRecordDocument.getDateOfInstrument().getTime());
		}
		
		
		instr.setInstno(generateSpecificInstrument(docR, dataSite));
		
		String legalDescription = officialRecordDocument.getLegalDescription();
		GenericATIDS.getLogger().debug("Found legal description: " + legalDescription);

		Set<InstrumentI> crossRefs = new HashSet<InstrumentI>();
		
		PropertyI prop = Property.createEmptyProperty();
		
		if(!StringUtils.isEmpty(legalDescription)) {
			
			SubdivisionDetailedI subdivisionDetailedI = new SubdivisionDetailed();
			prop.getLegal().setSubdivision(subdivisionDetailedI);
			prop.getLegal().setFreeForm(legalDescription);
			
//			if(legalDescription.length() < 150) {
			
				Matcher matcher = null;
				
				Pattern APT_PATTERN = Pattern.compile("\\bAPT\\s+([A-Z\\d]+)\\b");
				matcher = APT_PATTERN.matcher(legalDescription);
				
				if(matcher.find()) {
					prop.getLegal().getSubdivision().setUnit(matcher.group(1));
				} else {
					Pattern UNIT_PATTERN = Pattern.compile("\\bUNIT\\s+(\\d+)\\b");
					matcher = UNIT_PATTERN.matcher(legalDescription);
					
					if(matcher.find()) {
						prop.getLegal().getSubdivision().setUnit(matcher.group(1));
					}
				}
				
				Pattern BLDG_PATTERN = Pattern.compile("\\bBLDG\\s+([A-Z\\d]+)\\b");
				matcher = BLDG_PATTERN.matcher(legalDescription);
				
				if(matcher.find()) {
					prop.getLegal().getSubdivision().setBuilding(matcher.group(1));
				}
				
				
//				Pattern OR_BP_PATTERN = Pattern.compile("\\b(OR|CN)\\s+(\\d+)\\s*[/-]\\s*(\\d+)\\b");
//				matcher = OR_BP_PATTERN.matcher(legalDescription);
//				if(matcher.find()) {
//					
//					Instrument instrCrossRef = new Instrument();
//					String group2 = matcher.group(2);
//					String group3 = matcher.group(3);
//					if(group2.length() == 4 && group3.length() >= 6) {
//						instrCrossRef.setYear(Integer.parseInt(group2));
//						instrCrossRef.setInstno(group3);
//					} else {
//						instrCrossRef.setBook(group2);
//						instrCrossRef.setPage(org.apache.commons.lang.StringUtils.stripStart(group3, "0"));
//					}
//					instrCrossRef.setBookType(matcher.group(1));
//					crossRefs.add(instrCrossRef);
//				}
				
				Pattern LT_PATTERN = Pattern.compile("\\bLO?TS?\\s+(\\d+)(-(\\d+))?\\b");
				matcher = LT_PATTERN.matcher(legalDescription);
				if(matcher.find()) {
					prop.getLegal().getSubdivision().setLot(matcher.group(1));
					if(StringUtils.isNotEmpty(matcher.group(3))) {
						prop.getLegal().getSubdivision().setLotThrough(matcher.group(3));	
					}
				}
				
				Pattern BLK_PATTERN = Pattern.compile("\\bBLK?\\s+([A-Z\\d]+)\\b");
				matcher = BLK_PATTERN.matcher(legalDescription);
				if(matcher.find()) {
					prop.getLegal().getSubdivision().setBlock(matcher.group(1));
				} else {
				
					Pattern BLKS_PATTERN = Pattern.compile("\\bBLK?S\\s+(([A-Z\\d]+)(\\s*&\\s*([A-Z\\d]+))?)\\b");
					matcher = BLKS_PATTERN.matcher(legalDescription);
					if(matcher.find()) {
						prop.getLegal().getSubdivision().setBlock(matcher.group(1));
					}
				}
				Pattern PB_BP_PATTERN = Pattern.compile("\\b(OR|CN|PB|CB)\\s+([A-Z\\d]+)\\s*[/-]\\s*(\\d+)(-([A-Z]))?\\b");
				matcher = PB_BP_PATTERN.matcher(legalDescription);
				if(matcher.find()) {
					subdivisionDetailedI.setPlatBookType(matcher.group(1));
					subdivisionDetailedI.setPlatBook(matcher.group(2));
					subdivisionDetailedI.setPlatPage(matcher.group(3));
					if(org.apache.commons.lang.StringUtils.isNotBlank(matcher.group(5))) {
						subdivisionDetailedI.setPlatPageSuffix(matcher.group(5));
					}
				}
				
				Pattern SEC_PATTERN = Pattern.compile("\\bSEC\\s+([A-Z\\d]+)\\s*-\\s*([A-Z\\d]+)\\s*-\\s*([A-Z\\d]+)\\b");
				matcher = SEC_PATTERN.matcher(legalDescription);
				if(matcher.find()) {
					prop.getLegal().getTownShip().setSection(matcher.group(1));
					prop.getLegal().getTownShip().setTownship(matcher.group(2));
					prop.getLegal().getTownShip().setRange(matcher.group(3));
				}
				Pattern TR_PATTERN = Pattern.compile("\\bTR\\s+([A-Z-\\d]+)\\b");
				matcher = TR_PATTERN.matcher(legalDescription);
				if(matcher.find()) {
					String tract = matcher.group(1);
					tract = tract.replaceFirst("(?is)\\bDESC(RIPTION)?\\b", "");
					prop.getLegal().getSubdivision().setTract(tract.trim());
				}
				Pattern PHASE_PATTERN = Pattern.compile("\\bPHASE\\s+([A-Z\\d]+)\\b");
				matcher = PHASE_PATTERN.matcher(legalDescription);
				if(matcher.find()) {
					prop.getLegal().getSubdivision().setPhase(matcher.group(1));
				}
			
//			}
			
		}
		
		String recordComments = officialRecordDocument.getComments();
		if(org.apache.commons.lang.StringUtils.isNotBlank(recordComments)) {
			docR.setInfoForSearchLog(recordComments);
		}
		
		ArrayOfDocumentReference relatedDocumentReferences = officialRecordDocument.getRelatedDocumentReferences();
		if(relatedDocumentReferences != null) {
			for (DocumentReference documentReference : relatedDocumentReferences.getDocumentReference()) {
				InstrumentI instrCrossRef = getInstrumentFrom(documentReference, dataSite);
				crossRefs.add(instrCrossRef);
			}
		}
		
		ArrayOfRecordingNumberReference relatedCaseNumberReferences = officialRecordDocument.getRelatedCaseNumberReferences();
		if(relatedCaseNumberReferences != null) {
			for (RecordingNumberReference recordingNumberReference : relatedCaseNumberReferences.getRecordingNumberReference()) {
				Instrument instrCrossRef = new Instrument();
				instrCrossRef.setInstno(Integer.toString(recordingNumberReference.getNumber()));
				instrCrossRef.setYear(recordingNumberReference.getYear());
				
				instrCrossRef.setInstno(generateSpecificInstrument(instrCrossRef, dataSite));
				
				instrCrossRef.setBookType(recordingNumberReference.getSource());
				crossRefs.add(instrCrossRef);
			}
		}
		
		
		docR.setParsedReferences(crossRefs);
		
		ArrayOfTaxFolioReference relatedTaxFolioReferences = officialRecordDocument.getRelatedTaxFolioReferences();
		if(relatedTaxFolioReferences != null && relatedTaxFolioReferences.getTaxFolioReference() != null) {
			PinI pin = prop.getPin();
			for (TaxFolioReference taxFolioReference : relatedTaxFolioReferences.getTaxFolioReference()) {
				for (PinType pinType : PinType.values()) {
					if(org.apache.commons.lang.StringUtils.isBlank(pin.getPin(pinType))) {
						pin.addPin(pinType, taxFolioReference.getTaxFolioNumber());
						break;
					}
				}
			}
		}
		
		docR.addProperty(prop);
		
		PartyI grantors = new com.stewart.ats.base.parties.Party(PType.GRANTOR);
		PartyI grantees = new com.stewart.ats.base.parties.Party(PType.GRANTEE);
		
		ArrayOfParty arrayOfParty = officialRecordDocument.getParties();
		if(arrayOfParty != null) {
			for (Party party : arrayOfParty.getParty()) {
				try{
				PartyI tempParty = new com.stewart.ats.base.parties.Party(PType.GRANTOR);
				
				ArrayOfLegalEntityName legalEntityNames = party.getLegalEntityNames();
				if (legalEntityNames == null){
					continue;
				}
				for (LegalEntityName legalEntityName : legalEntityNames.getLegalEntityName()) {
					
					NameI atsName = new Name();
					
					atsName.setCompany(LegalEntityNameType.Commercial.equals(legalEntityName.getLegalEntityNameType()));
					
					if(atsName.isCompany()) {
						
						String companyName = legalEntityName.getUnparsedLegalEntityName();
						if(companyName != null) {
							
							String comments = legalEntityName.getComments();
							if(org.apache.commons.lang.StringUtils.isNotBlank(comments)) {
								companyName = companyName.replace(comments, "");
							}
							
							companyName = companyName.replaceAll("\\bDATED\\s+[\\d/]+.*", "");
							companyName = companyName.replaceAll("\\bL L C\\b", "LLC");
							companyName = companyName.replaceAll("\\bTR\\b", "TRUST").trim();
						}
						
						atsName.setLastName(companyName);
					} else {
						atsName.setLastName(legalEntityName.getLastName());
						atsName.setFirstName(legalEntityName.getFirstName());
						atsName.setMiddleName(legalEntityName.getMiddleName());
						atsName.setSufix(legalEntityName.getSuffix());
						atsName.setPrefix(legalEntityName.getPrefix());
					}		
					
					tempParty.add(atsName);
					
				}
				
				
				if(party.getPartyRole().equals(PartyRoleType.Grantor)) {
					grantors.add(tempParty);
				} else if(party.getPartyRole().equals(PartyRoleType.Grantee)) {
					grantees.add(tempParty);
				} else if(party.getPartyRole().equals(PartyRoleType.General)) {
					grantees.add(tempParty);
				} else if(party.getPartyRole().equals(PartyRoleType.Unknown)) {
					grantees.add(tempParty);
				}
				
				} catch (Exception e){
					e.printStackTrace();
				}
			}
		}
		
		docR.setGrantor(grantors);
		docR.setGrantee(grantees);
		
		
		if(officialRecordDocument.getIsImageAvailable()) {
			String imageToken = officialRecordDocument.getImageToken();
			search.addImagesToDocument(docR, getShortImageLink(imageToken, false));
		}
		
		
		if (docCateg.equals(DocumentTypes.MORTGAGE)){
			Mortgage mortgage = new Mortgage(docR);
			BigDecimal amount = officialRecordDocument.getAmount();
			if(amount != null) {
				mortgage.setMortgageAmount(amount.doubleValue());
			}
			PartyI granteesLander = new com.stewart.ats.base.parties.Party(PType.GRANTEE);
			grantees = mortgage.getGrantee();
			for (NameI name : grantees.getNames()) {
				NameMortgageGranteeI nameGrantee = new NameMortgageGrantee(name);
				nameGrantee.setTrustee(false);
				granteesLander.add( nameGrantee );
			}
			mortgage.setGrantee(granteesLander);
			
			
			docR = mortgage;
		} else if (docCateg.equals(DocumentTypes.TRANSFER)) {
			Transfer transfer = new Transfer(docR);
			BigDecimal amount = officialRecordDocument.getAmount();
			if(amount != null) {
				transfer.setSalePrice(amount.doubleValue());
			}
			docR = transfer;
		} else if(docCateg.equals(DocumentTypes.COURT)){
			Court court = new Court(docR);
			BigDecimal amount = officialRecordDocument.getAmount();
			if(amount != null) {
				court.setConsiderationAmount(amount.doubleValue());
				court.setConsiderationAmountFreeForm(amount.toString());
			}
			docR = court;
		} else if(docCateg.equals(DocumentTypes.LIEN)){
			Lien lien = new Lien(docR);
			
			BigDecimal amount = officialRecordDocument.getAmount();
			if(amount != null) {
				lien.setConsiderationAmount(amount.doubleValue());
				lien.setConsiderationAmountFreeForm(amount.toString());
			}
			docR  = lien;
		} else if(docCateg.equals(DocumentTypes.CCER)){
			docR =  new Ccer(docR); 
		} else if (docCateg.equals(DocumentTypes.CORPORATION)) {
			docR = new Corporation(docR);
		}
		
		return docR;
	}
	
	public static DocumentParsedResponse getDocument(PlatInformation platInformation, GenericATIDS genericATIDS, TSServerInfoModule module)
			throws ParseException {
		
		Search search = genericATIDS.getSearch();
		DataSite dataSite = genericATIDS.getDataSite();
		
		InstrumentI instr = new Instrument();
		String serverDocType = "";
		
		PropertyI prop = Property.createEmptyProperty();
		SubdivisionDetailedI subdivisionDetailedI = new SubdivisionDetailed();
		prop.getLegal().setSubdivision(subdivisionDetailedI);
		
		String imageLink = null;
		String parentSiteImageLink = null;
		
		BookPageReference bookPageReference = platInformation.getPlatReference().getBookPageReference();
		if(bookPageReference != null) {
			
			instr.setBook(bookPageReference.getBook() );
			instr.setPage( org.apache.commons.lang.StringUtils.stripStart(bookPageReference.getPage(), "0") );
			instr.setBookType(bookPageReference.getSource());
			serverDocType = bookPageReference.getSource();
			
			subdivisionDetailedI.setPlatBook(org.apache.commons.lang.StringUtils.defaultString(bookPageReference.getBook()));
			subdivisionDetailedI.setPlatPage(org.apache.commons.lang.StringUtils.defaultString(bookPageReference.getPage()));
			subdivisionDetailedI.setPlatBookSuffix(org.apache.commons.lang.StringUtils.defaultString(bookPageReference.getBookSuffix()));
			subdivisionDetailedI.setPlatPageSuffix(org.apache.commons.lang.StringUtils.defaultString(bookPageReference.getPageSuffix()));
			subdivisionDetailedI.setPlatBookType(org.apache.commons.lang.StringUtils.defaultString(bookPageReference.getSource()));
			
			imageLink = getShortImageLink(bookPageReference);
			parentSiteImageLink = genericATIDS.createPartialLink(TSConnectionURL.idPOST, TSServerInfo.MODULE_IDX39) + imageLink;
			
		}
		
		RecordingNumberReference recordingNumberReference = platInformation.getPlatReference().getRecordingNumberReference();
		if(recordingNumberReference != null) {
			instr.setInstno(Integer.toString(recordingNumberReference.getNumber()));
			instr.setYear(recordingNumberReference.getYear());
			
			if(bookPageReference == null) {
				instr.setBookType(recordingNumberReference.getSource());
				serverDocType = recordingNumberReference.getSource();
			}
			
			subdivisionDetailedI.setPlatInstrument(recordingNumberReference.getNumber() + "");
			subdivisionDetailedI.setPlatInstrumentYear(recordingNumberReference.getYear() + "");
			subdivisionDetailedI.setPlatInstrumentSuffix(org.apache.commons.lang.StringUtils.defaultString(recordingNumberReference.getNumberSuffix()));
			subdivisionDetailedI.setPlatInstrumentCode(org.apache.commons.lang.StringUtils.defaultString(recordingNumberReference.getSeriesCode()));
			subdivisionDetailedI.setPlatInstrumentType(org.apache.commons.lang.StringUtils.defaultString(recordingNumberReference.getSource()));
			
			if(imageLink == null) {
				imageLink = getShortImageLink(recordingNumberReference);
				parentSiteImageLink = genericATIDS.createPartialLink(TSConnectionURL.idPOST, TSServerInfo.MODULE_IDX38) + imageLink;
			}
			
		}
		
		prop.getLegal().setFreeForm(platInformation.getPlatName());
		
		String docCateg = DocumentTypes.MISCELLANEOUS;
		instr.setDocType(docCateg);
		String stype = DocumentTypes.MISCELLANEOUS;
		instr.setDocSubType(stype);
		
		RegisterDocumentI registerDocumentI = new RegisterDocument( DocumentsManager.generateDocumentUniqueId(search.getID(), instr) );
		registerDocumentI.setInstrument(instr);
		
		
		if(platInformation.getPlatDate().getTime() == null) {
			//"FAKE" the result
			if(!instr.hasInstrNo()) {
				instr.setYear(1960);
			}
			registerDocumentI.setRecordedDate(new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).parse("01/01/1960"));
			registerDocumentI.setFake(true);
		} else {
			registerDocumentI.setRecordedDate(platInformation.getPlatDate().getTime());
		}
		
		if(instr.hasInstrNo()) {
			instr.setInstno(GenericATIDSFunctions.generateSpecificInstrument(instr, dataSite));
		}
		
		registerDocumentI.setServerDocType(serverDocType);
		
		String[] typePair = SOURCES.get(serverDocType);
		if(typePair != null) {
			instr.setDocType(typePair[0]);
			instr.setDocSubType(typePair[1]);
			
			registerDocumentI = DocumentsManager.createRegisterDocument(search.getID(), typePair[0], (RegisterDocument) registerDocumentI, null);	    		
		}
		
		registerDocumentI.setType(SimpleChapterUtils.DType.ROLIKE);
		registerDocumentI.setDataSource(dataSite.getSiteTypeAbrev());
		registerDocumentI.setSiteId((int)dataSite.getServerId());
		if(genericATIDS.isParentSite()) {
			registerDocumentI.setSavedFrom(SavedFromType.PARENT_SITE);
		} else {
			registerDocumentI.setSavedFrom(SavedFromType.AUTOMATIC);
		}
		if(module != null && org.apache.commons.lang.StringUtils.isNotBlank(module.getSearchType())) {
			registerDocumentI.setSearchType(SearchType.valueOf(module.getSearchType()));	
		}
		
		
		if(platInformation.getAuthorizedPlatLevels() != null) {
			AuthorizedPlatLevel platLevel = platInformation.getAuthorizedPlatLevels().getPrimary();
			if(platLevel != null) {
				if (org.apache.commons.lang.StringUtils.isNotEmpty(platLevel.getLevelType1())) {
					prop.getLegal().getSubdivision().setLot(platLevel.getLevelType1());
				}

				if (org.apache.commons.lang.StringUtils.isNotEmpty(platLevel.getLevelType2())) {
					prop.getLegal().getSubdivision().setBlock(platLevel.getLevelType2());
				}

				if (org.apache.commons.lang.StringUtils.isNotEmpty(platLevel.getLevelType3())) {
					prop.getLegal().getSubdivision().setSection(platLevel.getLevelType3());
				}

				registerDocumentI.addProperty(prop);
				
			}
			platLevel = platInformation.getAuthorizedPlatLevels().getAlternate1();
			if(platLevel != null) {
				PropertyI propAlternate1 = prop.clone();
				
				if (org.apache.commons.lang.StringUtils.isNotEmpty(platLevel.getLevelType1())) {
					propAlternate1.getLegal().getSubdivision().setLot(platLevel.getLevelType1());
				} else {
					propAlternate1.getLegal().getSubdivision().setLot(null);
				}

				if (org.apache.commons.lang.StringUtils.isNotEmpty(platLevel.getLevelType2())) {
					propAlternate1.getLegal().getSubdivision().setBlock(platLevel.getLevelType2());
				} else {
					propAlternate1.getLegal().getSubdivision().setBlock(null);
				}

				if (org.apache.commons.lang.StringUtils.isNotEmpty(platLevel.getLevelType3())) {
					propAlternate1.getLegal().getSubdivision().setSection(platLevel.getLevelType3());
				} else {
					propAlternate1.getLegal().getSubdivision().setSection(null);
				}

				registerDocumentI.addProperty(propAlternate1);
			}
			platLevel = platInformation.getAuthorizedPlatLevels().getAlternate2();
			if(platLevel != null) {
				PropertyI propAlternate2 = prop.clone();
				
				if (org.apache.commons.lang.StringUtils.isNotEmpty(platLevel.getLevelType1())) {
					propAlternate2.getLegal().getSubdivision().setLot(platLevel.getLevelType1());
				} else {
					propAlternate2.getLegal().getSubdivision().setLot(null);
				}

				if (org.apache.commons.lang.StringUtils.isNotEmpty(platLevel.getLevelType2())) {
					propAlternate2.getLegal().getSubdivision().setBlock(platLevel.getLevelType2());
				} else {
					propAlternate2.getLegal().getSubdivision().setBlock(null);
				}

				if (org.apache.commons.lang.StringUtils.isNotEmpty(platLevel.getLevelType3())) {
					propAlternate2.getLegal().getSubdivision().setSection(platLevel.getLevelType3());
				} else {
					propAlternate2.getLegal().getSubdivision().setSection(null);
				}

				registerDocumentI.addProperty(propAlternate2);
			}
		} else {
			registerDocumentI.addProperty(prop);
		}
		if(imageLink != null) {
			search.addImagesToDocument(registerDocumentI, imageLink);
		}
		
		DocumentParsedResponse response = new DocumentParsedResponse(
				registerDocumentI, imageLink, parentSiteImageLink);
		
		return response;
	}
	
	public static RegisterDocumentI getDocument(GetDocumentImageAvailabilityResponse response, Search search, DataSite dataSite, TSServerInfoModule module)
			throws ParseException {
		String serverDocType = "";
		
		InstrumentI instr = new Instrument();
		if (module.getModuleIdx() == TSServerInfo.MODULE_IDX39) {
			instr.setBook(module.getParamValue(0).trim());
			instr.setPage(module.getParamValue(2).trim());
			serverDocType = module.getParamValue(4).trim();
			instr.setBookType(serverDocType);
		} else if (module.getModuleIdx() == TSServerInfo.MODULE_IDX38) {
			instr.setInstno(module.getParamValue(0).trim());
			instr.setYear(Integer.parseInt(module.getParamValue(2).trim()));
			serverDocType = module.getParamValue(1).trim();
			instr.setBookType(serverDocType);
		}
		
		
		
		String docCateg = DocumentTypes.MISCELLANEOUS;
		instr.setDocType(docCateg);
		String stype = DocumentTypes.MISCELLANEOUS;
		instr.setDocSubType(stype);
		
		RegisterDocumentI registerDocumentI = new RegisterDocument( DocumentsManager.generateDocumentUniqueId(search.getID(), instr) );
		registerDocumentI.setInstrument(instr);
		
		//"FAKE" the result
		if(!instr.hasInstrNo()) {
			instr.setYear(1960);
		}
		registerDocumentI.setRecordedDate(new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).parse("01/01/1960"));
		registerDocumentI.setFake(true);
		
		if(instr.hasInstrNo()) {
			instr.setInstno(GenericATIDSFunctions.generateSpecificInstrument(instr, dataSite));
		}
		
		registerDocumentI.setServerDocType(serverDocType);
		
		String[] typePair = SOURCES.get(serverDocType);
		if(typePair != null) {
			instr.setDocType(typePair[0]);
			instr.setDocSubType(typePair[1]);
			
			registerDocumentI = DocumentsManager.createRegisterDocument(search.getID(), typePair[0], (RegisterDocument) registerDocumentI, null);	    		
		}
		
		registerDocumentI.setType(SimpleChapterUtils.DType.ROLIKE);
		registerDocumentI.setDataSource(dataSite.getSiteTypeAbrev());
		registerDocumentI.setSiteId((int)dataSite.getServerId());
		if(module != null && org.apache.commons.lang.StringUtils.isNotBlank(module.getSearchType())) {
			registerDocumentI.setSearchType(SearchType.valueOf(module.getSearchType()));	
		}
		
		String imageToken = response.getImageToken();
		search.addImagesToDocument(registerDocumentI, GenericATIDSFunctions.getShortImageLink(imageToken, false));
		return registerDocumentI;
	}
	
	public static void addPropertiesToRegisterDoc(RegisterDocumentI registerDoc, GetConveyanceDocumentInformationResponse response) {
		try {

			ArrayOfPlattedLegal plattedLegalArray = response.getPlattedLegalPostings();
			PropertyI propFreeForm = null;

			if (plattedLegalArray != null && plattedLegalArray.getPlattedLegal() != null && plattedLegalArray.getPlattedLegal().length > 0) {
				
				Set<PropertyI> initialProperties = registerDoc.getProperties();
				for (PropertyI propertyI : initialProperties) {
					if(org.apache.commons.lang.StringUtils.isNotBlank(propertyI.getLegal().getFreeForm())) {
						propFreeForm = Property.createEmptyProperty();
						propFreeForm.getLegal().setFreeForm(propertyI.getLegal().getFreeForm());
						break;
					}
				}
				if(registerDoc != null) {
					registerDoc.getProperties().clear();
				}
				
				PlattedLegal[] plattedLegal = plattedLegalArray.getPlattedLegal();
				for (PlattedLegal pl : plattedLegal) {
					PropertyI prop = Property.createEmptyProperty();
					SubdivisionDetailedI subdivisionDetailedI = new SubdivisionDetailed();
					prop.getLegal().setSubdivision(subdivisionDetailedI);
					
					PlatReference pr = pl.getPlatReference();

					if (pr != null) {
						BookPageReference bpr = pr.getBookPageReference();
						RecordingNumberReference rnr = pr.getRecordingNumberReference();

						if (bpr != null) {
							subdivisionDetailedI.setPlatBook(org.apache.commons.lang.StringUtils.defaultString(bpr.getBook()));
							subdivisionDetailedI.setPlatPage(org.apache.commons.lang.StringUtils.defaultString(bpr.getPage()));
							subdivisionDetailedI.setPlatBookSuffix(org.apache.commons.lang.StringUtils.defaultString(bpr.getBookSuffix()));
							subdivisionDetailedI.setPlatPageSuffix(org.apache.commons.lang.StringUtils.defaultString(bpr.getPageSuffix()));
							subdivisionDetailedI.setPlatBookType(org.apache.commons.lang.StringUtils.defaultString(bpr.getSource()));
						}

						if (rnr != null) {
							subdivisionDetailedI.setPlatInstrument(rnr.getNumber() + "");
							subdivisionDetailedI.setPlatInstrumentYear(rnr.getYear() + "");
							subdivisionDetailedI.setPlatInstrumentSuffix(org.apache.commons.lang.StringUtils.defaultString(rnr.getNumberSuffix()));
							subdivisionDetailedI.setPlatInstrumentCode(org.apache.commons.lang.StringUtils.defaultString(rnr.getSeriesCode()));
							subdivisionDetailedI.setPlatInstrumentType(org.apache.commons.lang.StringUtils.defaultString(rnr.getSource()));
							
						}
						
					}

					ArrayOfSubdivisionLevels subdLevels = pl.getSubdivisionLevels();

					if (subdLevels != null && subdLevels.getSubdivisionLevels() != null && subdLevels.getSubdivisionLevels().length > 0) {
						SubdivisionLevels[] levels = subdLevels.getSubdivisionLevels();

						try {
							for (SubdivisionLevels slvl : levels) {
								if (org.apache.commons.lang.StringUtils.isNotEmpty(slvl.getLevel1())) {
									prop.getLegal().getSubdivision().setLot(slvl.getLevel1());
								}

								if (org.apache.commons.lang.StringUtils.isNotEmpty(slvl.getLevel2())) {
									prop.getLegal().getSubdivision().setBlock(slvl.getLevel2());
								}

								if (org.apache.commons.lang.StringUtils.isNotEmpty(slvl.getLevel3())) {
									prop.getLegal().getSubdivision().setSection(slvl.getLevel3());
								}

								if (prop.hasLegal()) {
									Set<PropertyI> alreadyParsedProperties = registerDoc.getProperties();
									boolean alreadyParsed = false;
									for (PropertyI propertyI : alreadyParsedProperties) {
										if(propertyI.getLegal().hasSubdividedLegal() && propertyI.getLegal().getSubdivision().contains(prop.getLegal().getSubdivision())) {
											alreadyParsed = true;
											break;
										}
									}
									if(!alreadyParsed) {
										registerDoc.addProperty(prop);
									}
								}
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						if (prop.hasLegal()) {
							Set<PropertyI> alreadyParsedProperties = registerDoc.getProperties();
							boolean alreadyParsed = alreadyParsedProperties.isEmpty();
							for (PropertyI propertyI : alreadyParsedProperties) {
								if(propertyI.getLegal().hasSubdividedLegal() && propertyI.getLegal().getSubdivision().contains(prop.getLegal().getSubdivision())) {
									alreadyParsed = true;
									break;
								}
							}
							if(!alreadyParsed) {
								registerDoc.addProperty(prop);
							}
						}
					}
				}
			}

			ArrayOfUnplattedLegal unplattedLegalArray = response.getUnplattedLegalPostings();

			if (unplattedLegalArray != null && unplattedLegalArray.getUnplattedLegal() != null && unplattedLegalArray.getUnplattedLegal().length > 0) {
				UnplattedLegal[] unplattedLegal = unplattedLegalArray.getUnplattedLegal();
				for (UnplattedLegal upl : unplattedLegal) {
					PropertyI prop = Property.createEmptyProperty();

					if (StringUtils.isNotEmpty(upl.getSection())) {
						prop.getLegal().getTownShip().setSection(upl.getSection());
					}

					if (StringUtils.isNotEmpty(upl.getTownship())) {
						prop.getLegal().getTownShip().setTownship(upl.getTownship());
					}

					if (StringUtils.isNotEmpty(upl.getRange())) {
						prop.getLegal().getTownShip().setRange(upl.getRange());
					}

					if (StringUtils.isNotEmpty(upl.getTownshipDirection().getValue())) {
						prop.getLegal().getTownShip().setFirstDirection(upl.getTownshipDirection().getValue());
					}

					if (StringUtils.isNotEmpty(upl.getRangeDirection().getValue())) {
						prop.getLegal().getTownShip().setSecondDirection(upl.getRangeDirection().getValue());
					}

					if (upl.getSectionBreakdownCodes() != null &&
							upl.getSectionBreakdownCodes().getSectionBreakdownCode() != null &&
							upl.getSectionBreakdownCodes().getSectionBreakdownCode().length > 0) {

						SectionBreakdownCode[] codes = upl.getSectionBreakdownCodes().getSectionBreakdownCode();

						for (SectionBreakdownCode code : codes) {
							String cCode = org.apache.commons.lang.StringUtils.defaultString(code.getSection256Th()) +
									org.apache.commons.lang.StringUtils.defaultString(code.getSection64Th()) +
									org.apache.commons.lang.StringUtils.defaultString(code.getSection16Th()) +
									org.apache.commons.lang.StringUtils.defaultString(code.getSectionQuarter());

							if (StringUtils.isNotEmpty(cCode)) {
								prop.getLegal().getTownShip().setQuarterValue(cCode);
								if (prop.hasLegal()) {
									Set<PropertyI> alreadyParsedProperties = registerDoc.getProperties();
									boolean alreadyParsed = alreadyParsedProperties.isEmpty();
									for (PropertyI propertyI : alreadyParsedProperties) {
										if(propertyI.getLegal().hasTownshipLegal() && propertyI.getLegal().getTownShip().contains(prop.getLegal().getTownShip())) {
											alreadyParsed = true;
											break;
										}
									}
									if(!alreadyParsed) {
										registerDoc.addProperty(prop);
									}
								}
							}
						}

					} else {
						if (prop.hasLegal()) {
							Set<PropertyI> alreadyParsedProperties = registerDoc.getProperties();
							boolean alreadyParsed = alreadyParsedProperties.isEmpty();
							for (PropertyI propertyI : alreadyParsedProperties) {
								if(propertyI.getLegal().hasTownshipLegal() && propertyI.getLegal().getTownShip().contains(prop.getLegal().getTownShip())) {
									alreadyParsed = true;
									break;
								}
							}
							if(!alreadyParsed) {
								registerDoc.addProperty(prop);
							}
						}
					}
				}
			}
			if (propFreeForm != null) {
				if (registerDoc.getProperties().size() > 0) {
					for (PropertyI prop : registerDoc.getProperties()) {
						if (StringUtils.isEmpty(prop.getLegal().getFreeForm())) {
							prop.getLegal().setFreeForm(propFreeForm.getLegal().getFreeForm());
						}
						break;
					}
				} else {
					registerDoc.addProperty(propFreeForm);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
		
	public static String getShortImageLink(String imageToken, boolean encode) {
		if(encode) {
			try {
				return "&imageToken=" + URLEncoder.encode(imageToken,"UTF-8") + "&fakeName=fake.tiff";
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return  "&imageToken=" + imageToken + "&fakeName=fake.tiff";
			}
		} else {
			return "&imageToken=" + imageToken + "&fakeName=fake.tiff";
		}
	}
	
	public static String getShortImageLink(BookPageReference bookPageReference) {
		if(bookPageReference != null) {
			return "&Book=" + org.apache.commons.lang.StringUtils.defaultString(bookPageReference.getBook()) + 
					"&Page=" + org.apache.commons.lang.StringUtils.defaultString(bookPageReference.getPage()) +
					"&BookSuffix=" + org.apache.commons.lang.StringUtils.defaultString(bookPageReference.getBookSuffix()) +
					"&PageSuffix=" + org.apache.commons.lang.StringUtils.defaultString(bookPageReference.getPageSuffix()) +
					"&Source=" + org.apache.commons.lang.StringUtils.defaultString(bookPageReference.getSource()) +
					"&fakeName=fake.tiff";
		}
		return null;
	}
	
	private static String getShortImageLink(RecordingNumberReference recordingNumberReference) {
		if(recordingNumberReference != null) {
			return "&Number=" + recordingNumberReference.getNumber() + 
					"&NumberSuffix=" + org.apache.commons.lang.StringUtils.defaultString(recordingNumberReference.getNumberSuffix()) +
					"&Year=" + recordingNumberReference.getYear() +
					"&SeriesCode=" + org.apache.commons.lang.StringUtils.defaultString(recordingNumberReference.getSeriesCode()) +
					"&NumberSuffix=" + org.apache.commons.lang.StringUtils.defaultString(recordingNumberReference.getSource()) +
					"&fakeName=fake.tiff";
		}
		return null;
	}

	private static InstrumentI getInstrumentFrom(DocumentReference documentReference, DataSite dataSite) {
		InstrumentI instr = new Instrument();
		if(documentReference.getBookPageReference() != null) {
			instr.setBook(documentReference.getBookPageReference().getBook() );
			instr.setPage( org.apache.commons.lang.StringUtils.stripStart(documentReference.getBookPageReference().getPage(), "0") );
			instr.setBookType(documentReference.getBookPageReference().getSource());
		}
		if(documentReference.getRecordingNumberReference() != null) {
			if(documentReference.getRecordingNumberReference().getNumber() > 0) {
				instr.setInstno(Integer.toString(documentReference.getRecordingNumberReference().getNumber()));
			}
			if(documentReference.getRecordingNumberReference().getYear() > 0) {
				instr.setYear(documentReference.getRecordingNumberReference().getYear());
			}
			if(documentReference.getBookPageReference() == null) {
				instr.setBookType(documentReference.getRecordingNumberReference().getSource());
			}
		}
		return instr;
	}
	
	public static String generateSpecificInstrument(InstrumentI instr, DataSite dataSite) {
		
		String instrNo = instr.getInstno();
		String year = Integer.toString(instr.getYear());
		
		if(dataSite.getCountyId() == CountyConstants.FL_Sarasota) {
			if(instr.getYear() < 1000) {
				return instrNo;
			}
			return instr.getYear() + org.apache.commons.lang.StringUtils.leftPad(instrNo, 6, "0");
		} else if(dataSite.getCountyId() == CountyConstants.FL_Lee) {

			if (instr instanceof RegisterDocumentI) {
				Date date = ((RegisterDocumentI) instr).getRecordedDate();
				Calendar r = Calendar.getInstance();
				r.set(2005, Calendar.AUGUST, 28);
				Calendar c = Calendar.getInstance();
				c.setTime(date);

				if (c.before(r))
					return instrNo;
				else if (c.after(r)	|| 
							(StringUtils.isEmpty(instr.getBook()) && StringUtils.isEmpty(instr.getPage()))) {
					if (instrNo.length() < 13) {
						instrNo = year + org.apache.commons.lang.StringUtils.leftPad(instrNo, 13 - year.length(), "0");
					}
				}
			} else {
				if (instr.getYear() > 2005 || 
						(instr.getYear() == 2005 && StringUtils.isEmpty(instr.getBook()) && StringUtils.isEmpty(instr.getPage()))) {
					if (instrNo.length() < 13) {
						instrNo = year + org.apache.commons.lang.StringUtils.leftPad(instrNo, 13 - year.length(), "0");
					}
				}
					
			}
		} else if(dataSite.getCountyId() == CountyConstants.FL_Charlotte) {
			if(instr.getYear() >= 1980 && instr.getYear() <= 1987) {
				return year.substring(2) + org.apache.commons.lang.StringUtils.leftPad(instrNo, 6, "0");
			}
		} else if(dataSite.getCountyId() == CountyConstants.FL_Escambia) {
			if(instr.getYear() >= 2000 ) {
				return year + org.apache.commons.lang.StringUtils.leftPad(instrNo, 6, "0");
			} else if(instr.getYear() >= 1997 ) {
				return year.substring(2) + org.apache.commons.lang.StringUtils.leftPad(instrNo, 6, "0");
			}
		} else if(dataSite.getCountyId() == CountyConstants.FL_Lake) {
			if(instr.getYear() >= 2000 ) {
				return year + org.apache.commons.lang.StringUtils.leftPad(instrNo, 6, "0");
			} else {
				return year.substring(2) + org.apache.commons.lang.StringUtils.leftPad(instrNo, 6, "0");
			}
		} else if(dataSite.getCountyId() == CountyConstants.FL_DeSoto) {
			if(instr.getYear() >= 2000 ) {
				return year + org.apache.commons.lang.StringUtils.leftPad(instrNo, 6, "0");
			} else {
				return year.substring(2) + org.apache.commons.lang.StringUtils.leftPad(instrNo, 4, "0");
			}
		}
		return instrNo;
	}

}
