package ro.cst.tsearch.extractor.xml;

import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.xerces.dom.DeferredTextImpl;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.search.address.StandardAddress;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.searchsites.client.TaxSiteData;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.response.BoardNameSet;
import ro.cst.tsearch.servers.response.BondsAndAssessmentsSet;
import ro.cst.tsearch.servers.response.CrossRefSet;
import ro.cst.tsearch.servers.response.GranteeSet;
import ro.cst.tsearch.servers.response.GrantorSet;
import ro.cst.tsearch.servers.response.InfSet;
import ro.cst.tsearch.servers.response.NameSet;
import ro.cst.tsearch.servers.response.OtherInformationSet;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.ParsedResponseData;
import ro.cst.tsearch.servers.response.PartyNameSet;
import ro.cst.tsearch.servers.response.PriorYearsDelinquentSet;
import ro.cst.tsearch.servers.response.PropertyAppraisalSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.SpecialAssessmentSet;
import ro.cst.tsearch.servers.response.TaxHistorySet;
import ro.cst.tsearch.servers.response.TaxInstallmentSet;
import ro.cst.tsearch.servers.response.TaxInstallmentSet.TaxInstallmentSetKey;
import ro.cst.tsearch.servers.response.TaxRedemptionSet;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.templates.TemplateBuilder;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.CurrentInstance;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.address.Address;
import com.stewart.ats.base.address.AddressI;
import com.stewart.ats.base.document.AssessorDocument;
import com.stewart.ats.base.document.BankSuccessorDocument;
import com.stewart.ats.base.document.BankSuccessorDocumentI;
import com.stewart.ats.base.document.Ccer;
import com.stewart.ats.base.document.Corporation;
import com.stewart.ats.base.document.Court;
import com.stewart.ats.base.document.DeathRecordsDocument;
import com.stewart.ats.base.document.DeathRecordsDocumentI;
import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.document.DocumentI.SearchType;
import com.stewart.ats.base.document.HOACondo;
import com.stewart.ats.base.document.Instrument;
import com.stewart.ats.base.document.InstrumentI;
import com.stewart.ats.base.document.LexisNexisDocument;
import com.stewart.ats.base.document.LexisNexisDocumentI;
import com.stewart.ats.base.document.Lien;
import com.stewart.ats.base.document.MERSDocument;
import com.stewart.ats.base.document.MERSDocumentI;
import com.stewart.ats.base.document.ManufacturedHousingDocument;
import com.stewart.ats.base.document.ManufacturedHousingDocumentI;
import com.stewart.ats.base.document.Mortgage;
import com.stewart.ats.base.document.OffenderInformationDocument;
import com.stewart.ats.base.document.OffenderInformationDocumentI;
import com.stewart.ats.base.document.Plat;
import com.stewart.ats.base.document.PriorFileDocument;
import com.stewart.ats.base.document.PriorFileDocumentI;
import com.stewart.ats.base.document.RegisterDocument;
import com.stewart.ats.base.document.RegisterDocumentI;
import com.stewart.ats.base.document.SKLDInstrument;
import com.stewart.ats.base.document.SSFPriorFileDocument;
import com.stewart.ats.base.document.StateBarDocument;
import com.stewart.ats.base.document.StateBarDocumentI;
import com.stewart.ats.base.document.TaxDocument;
import com.stewart.ats.base.document.Transfer;
import com.stewart.ats.base.document.WhitePagesDocument;
import com.stewart.ats.base.document.WhitePagesDocumentI;
import com.stewart.ats.base.legal.Legal;
import com.stewart.ats.base.legal.Subdivision;
import com.stewart.ats.base.legal.TownShip;
import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.name.NameMortgageGrantee;
import com.stewart.ats.base.name.NameMortgageGranteeI;
import com.stewart.ats.base.parties.Party;
import com.stewart.ats.base.parties.PartyI;
import com.stewart.ats.base.property.Pin;
import com.stewart.ats.base.property.PinI;
import com.stewart.ats.base.property.PinI.PinType;
import com.stewart.ats.base.property.Property;
import com.stewart.ats.base.property.PropertyI;
import com.stewart.ats.base.search.DocumentsManager;
import com.stewart.ats.base.taxutils.BondAndAssessment;
import com.stewart.ats.base.taxutils.Installment;
import com.stewart.ats.base.taxutils.PriorYearsDelinquent;
import com.stewart.ats.base.taxutils.TaxRedemption;
import com.stewart.ats.tsrindex.client.HOAInfo;
import com.stewart.ats.tsrindex.client.Receipt;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.DType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.PType;
import com.stewart.ats.tsrindex.client.SimpleChapterUtils.SavedFromType;
import com.sun.xml.bind.v2.TODO;

public class Bridge {

    protected ParsedResponseData parsedResponse;
    protected ResultMap resultMap;

    protected long searchId = -1;
    
    public Bridge(ParsedResponseData pr, ResultMap m, long searchId) {
        parsedResponse=pr;
        resultMap=m;
        this.searchId = searchId;
    }
    
    public Bridge(ParsedResponseData pr, ResultMap m) {
        parsedResponse=pr;
        resultMap=m;
    }

    public static boolean  isAssessorSite(String srcType){    	
    	return ("AO".equals(srcType) || "IS".equals(srcType) || "NB".equals(srcType) || "AM".equals(srcType));
    }
    
    public static boolean isCityTaxSite(String srcType){
    	return StringUtils.isNotEmpty(srcType) && srcType.startsWith("Y") ;    	
    }

    public static boolean isCountyTaxSite(String srcType){    	
    	return ("TR".equals(srcType) || "TR2".equals(srcType) || "TU".equals(srcType) || "TX".equals(srcType) || "NTN".equals(srcType));
    }
    
    public static boolean isTaxSite(String srcType){    	    	
    	return (isCountyTaxSite(srcType) || isCityTaxSite(srcType));
    }
    
    public static boolean isTaxFromDT(String srcType, String docType){    	    	
    	return srcType.equals("DT") && docType.equals(DocumentTypes.COUNTYTAX);
    }
    
    public static boolean isRegisterLikeSite(String srcType){
    	return (!isTaxSite(srcType) && !isAssessorSite(srcType));
    }
    
    
    public static  String getFieldFromVector(Vector<InfSet> vector, String fieldName){
		String fieldVal = "";
    	if(vector != null) {
    		int size = vector.size();
    		InfSet elem;
    		for (int i=0; i<size; i++){
    			elem = vector.get(i);
    			fieldVal = elem.getAtribute(fieldName);
    			if (!StringUtils.isEmpty(fieldVal)){
    				break;
    			}
    		}
    	}
    	return fieldVal;
    }
       
    public static void setNamesFromSet(Vector v, PartyI party){
    	if(v != null){
    		int size = v.size();
    		for (int i=0; i<size; i++){
    			NameSet elem = (NameSet)v.get(i);
    			// extract owner
    			String last = elem.getAtribute("OwnerLastName");
    			String first = elem.getAtribute("OwnerFirstName");
    			String middle = elem.getAtribute("OwnerMiddleName");
    			if (last.length() != 0){
	    			Name name = new Name();	    					    			
	    			name.setLastName(last);
	    			name.setFirstName(first);
	    			// extract suffix from middle name	    			
	    			String[] names = GenericFunctions.extractSuffix(middle);
	    			name.setMiddleName(names[0]);
	    			if (elem.hasAtribute("Suffix")){
	    				name.setSufix(elem.getAtribute("Suffix"));
	    			} else {
	    				name.setSufix(names[1]);
	    			}
	    			//extract type from middle name
	    			if (elem.hasAtribute("Type")){
	    				name.setNameType(elem.getAtribute("Type"));
	    			} else {
		    			String[] type = GenericFunctions.extractType(middle);
		    			name.setNameType(type[1]);
	    			}
	    			//extract otherType from middle name
	    			if (elem.hasAtribute("OtherType")){
	    				name.setNameOtherType(elem.getAtribute("OtherType"));
	    			} else {
		    			String[] otherType = GenericFunctions.extractOtherType(middle);
		    			name.setNameOtherType(otherType[1]);
	    			}
	    			name.setCompany((first.length() == 0) && (middle.length() == 0) && NameUtils.isCompany(last));
	    			party.add(name);
    			}
    			// extract spouse
    			last = elem.getAtribute("SpouseLastName");
    			first = elem.getAtribute("SpouseFirstName");
    			middle = elem.getAtribute("SpouseMiddleName");
    			if (last.length() != 0){
	    			Name name = new Name();	    					    			
	    			name.setLastName(last);
	    			name.setFirstName(first);
	    			// extract suffix from middle name	    			
	    			String[] names = GenericFunctions.extractSuffix(middle);
	    			name.setMiddleName(names[0]);
	    			if (elem.hasAtribute("Suffix")){
	    				name.setSufix(elem.getAtribute("Suffix"));
	    			} else {
	    				name.setSufix(names[1]);
	    			}
	    			//extract type from middle name
	    			if (elem.hasAtribute("Type")){
	    				name.setNameType(elem.getAtribute("Type"));
	    			} else {
		    			String[] type = GenericFunctions.extractType(middle);
		    			name.setNameType(type[1]);
	    			}
	    			//extract otherType from middle name
	    			if (elem.hasAtribute("OtherType")){
	    				name.setNameOtherType(elem.getAtribute("OtherType"));
	    			} else {
		    			String[] otherType = GenericFunctions.extractOtherType(middle);
		    			name.setNameOtherType(otherType[1]);
	    			}
	    			name.setCompany((first.length() == 0) && (middle.length() == 0) && NameUtils.isCompany(last));
	    			party.add(name);
    			}    			
    		}    		
    	}
    }
    	
	private static void setNamesTypeFromSet(Vector v, PartyI partyLander, PartyI partyTrustee){

    	if(v != null){
    		int size = v.size();
    		for (int i=0; i<size; i++){
    			NameSet elem = (NameSet)v.get(i);
    			// extract owner
    			String last = elem.getAtribute("OwnerLastName");
    			String first = elem.getAtribute("OwnerFirstName");
    			String middle = elem.getAtribute("OwnerMiddleName");
    			boolean isLander = "1".equals(elem.getAtribute("isLander")); 
    			if (last.length() != 0){
	    			Name name = new Name();	    					    			
	    			name.setLastName(last);
	    			name.setFirstName(first);
	    			// extract suffix from middle name	    			
	    			String[] names = GenericFunctions.extractSuffix(middle);
	    			name.setMiddleName(names[0]);
	    			if (elem.hasAtribute("Suffix")){
	    				name.setSufix(elem.getAtribute("Suffix"));
	    			} else {
	    				name.setSufix(names[1]);
	    			}
	    			//extract type from middle name
	    			if (elem.hasAtribute("Type")){
	    				name.setNameType(elem.getAtribute("Type"));
	    			} else {
		    			String[] type = GenericFunctions.extractType(middle);
		    			name.setNameType(type[1]);
	    			}
	    			//extract otherType from middle name
	    			if (elem.hasAtribute("OtherType")){
	    				name.setNameOtherType(elem.getAtribute("OtherType"));
	    			} else {
		    			String[] otherType = GenericFunctions.extractOtherType(middle);
		    			name.setNameOtherType(otherType[1]);
	    			}
	    			name.setCompany((first.length() == 0) && (middle.length() == 0) && NameUtils.isCompany(last));
	    			if (isLander){
	    				partyLander.add(name);
	    			} else {
	    				partyTrustee.add(name);
	    			}
    			}
    			// extract spouse
    			last = elem.getAtribute("SpouseLastName");
    			first = elem.getAtribute("SpouseFirstName");
    			middle = elem.getAtribute("SpouseMiddleName");
    			if (last.length() != 0){
	    			Name name = new Name();	    					    			
	    			name.setLastName(last);
	    			name.setFirstName(first);
	    			// extract suffix from middle name	    			
	    			String[] names = GenericFunctions.extractSuffix(middle);
	    			name.setMiddleName(names[0]);
	    			if (elem.hasAtribute("Suffix")){
	    				name.setSufix(elem.getAtribute("Suffix"));
	    			} else {
	    				name.setSufix(names[1]);
	    			}
	    			//extract type from middle name
	    			if (elem.hasAtribute("Type")){
	    				name.setNameType(elem.getAtribute("Type"));
	    			} else {
		    			String[] type = GenericFunctions.extractType(middle);
		    			name.setNameType(type[1]);
	    			}
	    			//extract otherType from middle name
	    			if (elem.hasAtribute("OtherType")){
	    				name.setNameOtherType(elem.getAtribute("OtherType"));
	    			} else {
		    			String[] otherType = GenericFunctions.extractOtherType(middle);
		    			name.setNameOtherType(otherType[1]);
	    			}
	    			name.setCompany((first.length() == 0) && (middle.length() == 0) && NameUtils.isCompany(last));
	    			if (isLander){
	    				partyLander.add(name);
	    			} else {
	    				partyTrustee.add(name);
	    			}
    			}    			
    		}    		
    	}
    }
	
	private static void setBoardNamesFromSet(Vector v, PartyI party){
    	if(v != null){
    		int size = v.size();
    		for (int i=0; i<size; i++){
    			BoardNameSet elem = (BoardNameSet)v.get(i);
    			// extract board member
    			String last = elem.getAtribute("MemberLastName");
    			String first = elem.getAtribute("MemberFirstName");
    			String middle = elem.getAtribute("MemberMiddleName");
    			if (last.length() != 0){
	    			Name name = new Name();	    					    			
	    			name.setLastName(last);
	    			name.setFirstName(first);
	    			name.setMiddleName(middle);
	    		
	    			party.add(name);
    			}   			
    		}    		
    	}
    }
	
	public void mergeInformation() throws Exception{
		// fill the OtherInformationSet.SrcType if empty
    	if(searchId > 0) {
    		try{
	    		String prevSrcType = (String)resultMap.get(OtherInformationSetKey.SRC_TYPE.getKeyName());
	    		if(StringUtils.isEmpty(prevSrcType)){
	    			boolean isParentSite = false;
	    			if (parsedResponse instanceof ParsedResponse) {
						isParentSite = ((ParsedResponse)parsedResponse).isParentSite();
					}
	    			
	    			String newSrcType = HashCountyToIndex.getCrtServer(searchId, isParentSite).getSiteTypeAbrev();
	    			resultMap.getMap().put(OtherInformationSetKey.SRC_TYPE.getKeyName(), newSrcType);
	    		}
			}catch(Exception e){
				e.printStackTrace();
			}    		
    	}
    	
        Iterator it=resultMap.entrySetIterator();
        while (it.hasNext()) {
            Entry e=(Entry)it.next();
            // get infSet
            boolean isVector=false;
            String setName=(String)e.getKey();
            String fieldName=null;
            int dotIdx=setName.indexOf('.');
            if (dotIdx>0) {
                fieldName=setName.substring(dotIdx+1);
                setName=setName.substring(0, dotIdx);
            }
            InfSet infSet=null;
            Vector infVectorSet=null;
            if (parsedResponse.infSets.containsKey(setName)) {
                isVector=false;
                infSet=(InfSet)parsedResponse.infSets.get(setName);
            } else if (parsedResponse.infVectorSets.containsKey(setName)) {
                isVector=true;
                infVectorSet=(Vector)parsedResponse.infVectorSets.get(setName);
            } else
                throw new Exception("Unknown InfSet : "+setName);
            // get mapping
            if (fieldName!=null) {
                
                String resultString;
                
                if (e.getValue() instanceof String)
                    resultString = (String) e.getValue();
                else if (e.getValue() instanceof DeferredTextImpl)
                    resultString = ((DeferredTextImpl) e.getValue()).getData();
                else if (e.getValue() == null)
                	continue;
                else
                    throw new Exception("The value for the key "+setName+" is not a string, it is a "+e.getValue().getClass()+" : "+e.getValue());
                
                if (isVector) {
                    if (infVectorSet.size()==0)
                        infVectorSet.add((InfSet)Class.forName(InfSet.class.getPackage().getName()+"."+setName).newInstance());
                    infSet=(InfSet)infVectorSet.get(0); //fix for bug #904
                }
                infSet.setAtribute(fieldName, resultString);
            } else {
            	if (e.getValue() == null)
            		continue;
            	if (e.getValue() instanceof Vector && setName.equals("PropertyIdentificationSet")){//for ILKaneRO multiple subdivisions/block and lots 1765950
            		for (int i = 0; i < ((Vector)e.getValue()).size(); i++){
            			if (!(((Vector)e.getValue()).elementAt(i) instanceof PropertyIdentificationSet)){
            				throw new Exception("Must be a Vector of ProperyIdentificationSet");
            			}
            		}
            		parsedResponse.infVectorSets.put(setName, e.getValue());
            		continue;
            	}
                if (!(e.getValue() instanceof ResultTable))
                    throw new Exception("The value for the key "+setName+" is not a table");
                ResultTable resultTable=(ResultTable)e.getValue();
                ResultMap mapping=resultTable.getMap();
                if (isVector) {
                    for (int i=0; i<resultTable.getLength(); i++) {
                    	boolean isGhertzoiala = false;
                    	//this test is very important
                    	//the order of the elements in resultMap is different localy than on alpha, beta....
                    	//why?.... I don't know...
                    	//if by any chance an attribute of a PropertyIdentificationSet appears before the full result table, 
                    		//...another pis will be added, which is not ok
                    	//I don't know what happens for the other sets 
                    	// Might not work always :D!!!!!
                    	if(infVectorSet.size()==1 && i==0 && setName.equals("PropertyIdentificationSet"))
                    		isGhertzoiala = true;
                    	InfSet infSeti = null;
                    	if(isGhertzoiala)
                    		infSeti = (InfSet)infVectorSet.get(0);
                    	else
                    		infSeti = (InfSet)Class.forName(InfSet.class.getPackage().getName()+"."+setName).newInstance();
                        // for each item in mapping
                        Iterator itm=mapping.entrySetIterator();
                        while (itm.hasNext()) {
                            Entry em=(Entry)itm.next();
                            String setColumnName=(String)em.getKey();
                            String[] mappingColumn=(String[])em.getValue();
                            String valOld = infSeti.getAtribute(setColumnName);
                            String valNew = getColumn(resultTable, mappingColumn [0], mappingColumn[1], i);
                            if("--".equals(valNew) && "ParcelID".equals(setColumnName))
                            	valNew = "";
                            if (!StringUtils.isEmpty(valOld) && !valOld.equals(valNew))
                            	valNew = (valNew + " " + valOld).trim();
                            infSeti.setAtribute(setColumnName, valNew);
                        }
                        if(!isGhertzoiala)	//if isGhertzoiala, the element is already in the infVectorSet
                        	infVectorSet.add(infSeti);
                    }
                } else {
                    if (resultTable.getLength()!=1)
                        throw new Exception("The lenght of the table for "+setName+" should be 1");
                    // for each item in mapping
                    Iterator itm=mapping.entrySetIterator();
                    while (itm.hasNext()) {
                        Entry em=(Entry)itm.next();
                        String setColumnName=(String)em.getKey();
                        String[] mappingColumn=(String[])em.getValue();
                        infSet.setAtribute(setColumnName, getColumn(resultTable, mappingColumn [0], mappingColumn[1], 0));
                    }
                }
            }
        }
	}
    
    public DocumentI importData() throws Exception {
        mergeInformation();
        return fillDocument(resultMap, parsedResponse, searchId);
    }
    
    public static DocumentI fillDocument (ResultMap resultMap, ParsedResponseData parsedResponse, long searchId){
	    DocumentI doc = fillDocumentInternal(resultMap, parsedResponse, searchId);
    	doc.setChecked(true);
		doc.setIncludeImage(true);
		doc.setManualChecked(false);
		doc.setManualIncludeImage(false);
	    
		try {
			long siteId = -1;
			boolean isParentSite = false;
			if (parsedResponse instanceof ParsedResponse) {
				isParentSite = ((ParsedResponse)parsedResponse).isParentSite();
				((ParsedResponse)parsedResponse).setDocument(doc);
				if(isParentSite) {
					doc.setSavedFrom(SavedFromType.PARENT_SITE);
				} else {
					doc.setSavedFrom(SavedFromType.AUTOMATIC);
				}
			}
			String county  = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentCounty().getName();
			String state = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
			
			if(resultMap != null) {	//i must be able to reopen old searches
			
				String type = (String)resultMap.get("OtherInformationSet.SrcType");
				
				if(StringUtils.isEmpty(type)){
					throw new RuntimeException("OtherInformationSet.SrcType should always be set from pasing");
				}
				//cook has wrong parsing rules and replace DS with search type
				if("IlCook".equalsIgnoreCase(state+county) && "PI".equalsIgnoreCase(type)
						||"GI".equalsIgnoreCase(type)||"SG".equalsIgnoreCase(type)){
					
					if(parsedResponse instanceof ParsedResponse){
						if(!"true".equals(  ((ParsedResponse)parsedResponse).getAttribute(ParsedResponse.REAL_PI)) ){
							type = "LA";
						}
					}else {
						type = "LA";
					}
				}
				siteId = TSServersFactory.getSiteId(state, county, type);
				doc.setSiteId((int)siteId);
				
				//task 7874
				if ("TR".equals(type) && ("KS".equals(state) || "MO".equals(state))) {
					String address = (String)resultMap.get(PropertyIdentificationSetKey.ADDRESS_ON_SERVER.getKeyName());
					if(address != null) {
						address = address.replaceAll("\n", " ");
						if (StringUtils.isNotEmpty(address)) {
							doc.setNote(address);
						}
					}
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return doc;
    }
    
    public static Set<RegisterDocumentI> getTransactioHistory( ParsedResponseData parsedResponse, Vector<SaleDataSet> vectorSDS, long searchId){
    	Set<RegisterDocumentI> tranHist =  new LinkedHashSet<RegisterDocumentI>();
    	// set transaction history
    	int size = vectorSDS.size();
    	if (vectorSDS != null && size  != 0){
    		for (int i=0; i<size; i++){
    			List<Instrument> allInst = new ArrayList<Instrument>(2);
    			
    			Instrument instr1 = new Instrument();
    			instr1.setBook(vectorSDS.get(i).getAtribute("Book"));
    			instr1.setPage(vectorSDS.get(i).getAtribute("Page"));
    			instr1.setInstno(vectorSDS.get(i).getAtribute("InstrumentNumber"));
    	    	instr1.setDocno(vectorSDS.get(i).getAtribute("DocumentNumber"));
    	    	String serverDocType = vectorSDS.get(i).getAtribute("DocumentType");
    	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
            	instr1.setDocType(docCateg);
            	String stype = DocumentTypes.getDocumentSubcategory(serverDocType, searchId);
            	if("MISCELLANEOUS".equals(stype)&&!"MISCELLANEOUS".equals(docCateg)){
            		stype = docCateg;
            	}
            	instr1.setDocSubType(stype);
            	allInst.add(instr1);
            	
            	Instrument instr2 = new Instrument();
    			instr2.setBook(vectorSDS.get(i).getAtribute("FinanceBook"));
    			instr2.setPage(vectorSDS.get(i).getAtribute("FinancePage"));
    			instr2.setInstno(vectorSDS.get(i).getAtribute("FinanceInstrumentNumber"));
    	    	instr2.setDocno(vectorSDS.get(i).getAtribute("FinanceDocumentNumber"));
    	    	if (!instr1.flexibleEquals(instr2, true)){//B 6972, when InstrumentNumber equals  FinanceInstrumentNumber and their RecordedDate is the same
	    	    	serverDocType = DocumentTypes.MORTGAGE;
	    	    	docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId);
	            	instr2.setDocType(docCateg);
	            	stype = DocumentTypes.getDocumentSubcategory(serverDocType, searchId);
	            	instr2.setDocSubType(stype);
	            	
	            	if(instr2.hasBookPage()||instr2.hasInstrNo()||instr2.hasDocNo()){
	            		allInst.add(instr2);
	            	}
    	    	}
            	
            	for(int j=0;j<allInst.size();j++){
            		boolean isFinance = (j==1);
            		Instrument instr = allInst.get(j);
	    			RegisterDocument roDoc = new RegisterDocument( DocumentsManager.generateDocumentUniqueId(searchId, instr) );
	    			roDoc.setInstrument(instr);
	    			roDoc.setServerDocType(serverDocType);
	            	roDoc.setShortServerDocType(vectorSDS.get(i).getAtribute("DocTypeAbbrev"));
	            	roDoc.setDocumentClass(vectorSDS.get(i).getAtribute("BookType"));
	            	roDoc.setType(SimpleChapterUtils.DType.ROLIKE);
	    			
	            	if(isFinance){
	            		String recDate = vectorSDS.get(i).getAtribute("FinanceRecordedDate"); 
	            		if (recDate.length() != 0){ 
		    				Date date = Util.dateParser3(recDate);
		    				if(date!=null){
			    				roDoc.setRecordedDate(date);
								roDoc.setYear(1900 + date.getYear());
		    				}
		    			}
	            	}else{
	            		String recDate = vectorSDS.get(i).getAtribute("RecordedDate"); 
		    			
		    			if (recDate.length() != 0){ 
		    				Date date = Util.dateParser3(recDate);
		    				if(date!=null){
			    				Util.setTime(date, vectorSDS.get(i).getAtribute("RecordedTime") );
			    				roDoc.setRecordedDate(date);
								roDoc.setYear(1900 + date.getYear());
		    				} else {
		    					try {
		    						if(recDate.matches("\\d{4}")) {
		    							roDoc.setYear(Integer.parseInt(recDate));
		    						} 
								} catch (Exception e) {
									e.printStackTrace();
								}
		    				}
		    			}
		    			String instrumentDate = vectorSDS.get(i).getAtribute("InstrumentDate");
		    			if (instrumentDate.matches("\\s+")) { //B 3324
		    				instrumentDate = "";
		    			}
		    			if (instrumentDate.length() != 0){ 
		    				Date date = Util.dateParser3(instrumentDate);
		    				if(date!=null){
			    				roDoc.setInstrumentDate(date);
			    				roDoc.setInstrumentYear(1900+date.getYear());
			    				if(roDoc.getRecordedDate()==null||roDoc.getYear()<=0){
			    					roDoc.setRecordedDate(date);
			    					roDoc.setYear(1900 + date.getYear());
			    				}
		    				} else {
		    					try {
		    						if(instrumentDate.matches("\\d{4}")) {
		    							int yearInt = Integer.parseInt(instrumentDate);
		    							roDoc.setInstrumentYear(yearInt);
		    							if(roDoc.getYear()<=0) {
		    								roDoc.setYear(yearInt);
		    							}
		    						} 
								} catch (Exception e) {
									e.printStackTrace();
								}
		    				}
		    			}
	            	}
	    			String grantor = vectorSDS.get(i).getAtribute("Grantor");
	    			if (!StringUtils.isEmpty(grantor)){
		    			Party grantors = new Party(PType.GRANTOR);
		    			grantors.setFreeParsedForm(grantor);
		    			Vector<GrantorSet> vectorGtorS = (Vector<GrantorSet>)parsedResponse.infVectorSets.get("GrantorSet");	
		    			if (vectorGtorS != null && vectorGtorS.size() != 0){
		    				setNamesFromSet(vectorGtorS, grantors);
		    			}
		    			roDoc.setGrantor(grantors);
	    			}
	    			
	    			String grantee = vectorSDS.get(i).getAtribute("Grantee");
	    			String granteeLander = vectorSDS.get(i).getAtribute("GranteeLander");
	    			if (!StringUtils.isEmpty(granteeLander)){
	    				if (!StringUtils.isEmpty(grantee)){
	    					grantee = grantee.concat("/").concat(granteeLander);
	    				} else {
	    					grantee = granteeLander;
	    				}
	    			}
	    			if (!StringUtils.isEmpty(grantee)){
		    			Party grantees = new Party(PType.GRANTEE);
		    			grantees.setFreeParsedForm(grantee);	
		    			Vector<GranteeSet> vectorGteeS = (Vector<GranteeSet>)parsedResponse.infVectorSets.get("GranteeSet");
		    			if (vectorGteeS != null && vectorGteeS.size() != 0){
		    				setNamesFromSet(vectorGteeS, grantees);
		    			}
		    			roDoc.setGrantee(grantees);
	    			}
	    			
	    			String salePrice = vectorSDS.get(i).getAtribute("SalesPrice"); 
	    			if (docCateg.equals(DocumentTypes.TRANSFER) || !StringUtils.isEmpty(salePrice)){
	    				Transfer tran = new Transfer(roDoc); 
						if (!StringUtils.isEmpty(salePrice)){
							try {
								tran.setSalePrice(Double.parseDouble(salePrice.replaceAll("[$,]", "")));
							} catch (NumberFormatException e){}
						}
	    				tranHist.add(tran);
	    			} else {
	    				String mtgAmt = vectorSDS.get(i).getAtribute("MortgageAmount"); 
		    			if (docCateg.equals(DocumentTypes.MORTGAGE)){
		    				Mortgage mtg = new Mortgage(roDoc); 
							if (!StringUtils.isEmpty(mtgAmt)){
								try {
									mtg.setMortgageAmount( Double.parseDouble(mtgAmt.replaceAll("[$,]", "")) );
								} catch (NumberFormatException e){}
								mtg.setMortgageAmountFreeForm( mtgAmt );
							}
		    				tranHist.add(mtg);
		    			} else {	    				
		    				tranHist.add(roDoc);
		    			}
	    			}
    			}
    		}
    	}
    	return tranHist;
    }
    
    private static DocumentI fillDocumentInternal (ResultMap resultMap, ParsedResponseData parsedResponse, long searchId){
    	
        //////////////// CR #2982
        // prepare data structures
    	Vector<PropertyIdentificationSet> vectorPIS = (Vector<PropertyIdentificationSet>)parsedResponse.infVectorSets.get("PropertyIdentificationSet");
        Vector<CrossRefSet> vectorCRS = (Vector<CrossRefSet>)parsedResponse.infVectorSets.get("CrossRefSet");
        Vector<SaleDataSet> vectorSDS = (Vector<SaleDataSet>)parsedResponse.infVectorSets.get("SaleDataSet");
        PropertyAppraisalSet pas = (PropertyAppraisalSet)parsedResponse.infSets.get("PropertyAppraisalSet");
        OtherInformationSet otherInformationSet = (OtherInformationSet)parsedResponse.infSets.get("OtherInformationSet");
        String tmp;
        try{
	        if( resultMap==null ){
	        	resultMap = new ResultMap();
				resultMap.put("OtherInformationSet.SrcType", otherInformationSet.getAtribute("SrcType"));
	        }
        }
        catch(Exception e){
        	e.printStackTrace();
        	resultMap = new ResultMap();
        }
        
        String srcType = (String)resultMap.get("OtherInformationSet.SrcType");
        if (srcType == null){
        	srcType = "";
        }
        
        // crossRefs
        Set<InstrumentI> crossRefs = extractCrossRefs(vectorCRS, srcType, searchId);
    	    	    	   		
    	// party
		PartyI party = new Party(PType.GRANTOR);
		party.setFreeParsedForm(getFieldFromVector((Vector)vectorPIS, "NameOnServer")); // needs to be added to all AO+ tax parsers
		if(StringUtils.isEmpty(party.getFreeParsedForm()) && !StringUtils.isEmpty(getFieldFromVector((Vector)vectorSDS, "Grantor"))) { 
			party.setFreeParsedForm(getFieldFromVector((Vector)vectorSDS, "Grantor"));
		}
			
		Vector<PartyNameSet> vectorPNS = (Vector<PartyNameSet>)parsedResponse.infVectorSets.get("PartyNameSet");
		// property.party 				
		// temporary:  if PartyNameSet was not set, then copy PropertyIdentificationSet owner and spouse names into PartyNameSet
		if (vectorPNS == null || vectorPNS.isEmpty()){    			
	    	if(vectorPIS != null){
	    		int size  = vectorPIS.size();
	    		String lastName = "";
	    		for (int i=0; i<size; i++){
	    			PropertyIdentificationSet elem = (PropertyIdentificationSet) vectorPIS.get(i);
	    			lastName = elem.getAtribute("OwnerLastName");
	    			if (lastName.length() != 0){
	    				vectorPNS = new Vector<PartyNameSet>();
	    				PartyNameSet pns = new PartyNameSet();
	    				pns.setAtribute("LastName", lastName);
	    				pns.setAtribute("FirstName", elem.getAtribute("OwnerFirstName"));
	    				pns.setAtribute("MiddleName", elem.getAtribute("OwnerMiddleName"));
	    				vectorPNS.add(pns);
	    				lastName = elem.getAtribute("SpouseLastName");
	    				if (lastName.length() != 0){
	    					pns = new PartyNameSet();
    	    				pns.setAtribute("LastName", lastName);
    	    				pns.setAtribute("FirstName", elem.getAtribute("SpouseFirstName"));
    	    				pns.setAtribute("MiddleName", elem.getAtribute("SpouseMiddleName"));
    	    				vectorPNS.add(pns);
	    				}
	    				break;
	    			}
	    		}
	    	}
		}		    	
    		
		if(vectorPNS != null){
    		int size = vectorPNS.size();
    		for (int i=0; i<size; i++){
    			PartyNameSet elem = (PartyNameSet)(vectorPNS.get(i));
    			Name name = new Name();
    			name.setLastName(elem.getAtribute("LastName"));
    			name.setFirstName(elem.getAtribute("FirstName"));
    			name.setMiddleName(elem.getAtribute("MiddleName"));
    			name.setSufix(elem.getAtribute("Suffix"));
    			name.setNameType(elem.getAtribute("Type"));
    			name.setNameOtherType(elem.getAtribute("OtherType"));
    			boolean isCompany = elem.getAtribute("isCompany").length() > 0; 
    			name.setCompany(isCompany);
    			party.add(name);
    		}    		
    	}
		
		Set<PropertyI> list = new  LinkedHashSet<PropertyI>();//ArrayList<PropertyI> list = new ArrayList<PropertyI>();
		// property.address and property.legal
		PropertyI prop1 = new Property();
		if (vectorPIS != null){
			int size = vectorPIS.size();
			for (int i=0; i<size; i++){      
		    	prop1 = new Property();
		    	if (i == 0){
		    		prop1.setOwner(party);
		    	} else {
		    		prop1.setOwner(new Party(PType.GRANTOR));
		    	}
		    	
				PropertyIdentificationSet elem = vectorPIS.get(i);
				
				// set address
				Address adr = new Address();
    			String street = elem.getAtribute("StreetName");
    			String addressOnServer = elem.getAtribute("AddressOnServer");
    			if (!StringUtils.isEmpty(street) || StringUtils.isNotEmpty(addressOnServer)){
    				String no = elem.getAtribute("StreetNo");
    				if (no.length() != 0){
    					street = no + " " + street;
    				}
    		    	fillAddressI(adr, street);
    				adr.setCity(elem.getAtribute("City"));
    				adr.setZip(elem.getAtribute("Zip"));
    				adr.setCounty(elem.getAtribute("County"));
    				adr.setState(elem.getAtribute("State"));
    				adr.setFreeform(elem.getAtribute("AddressOnServer")); // // needs to be added to all parsers	    		    	
    			}
    			prop1.setAddress(adr);
		    	
    			//set PINs
    			PinI pin = new Pin(); 
    			pin.addPin(PinI.PinType.PID, elem.getAtribute("ParcelID"));
    			pin.addPin(PinI.PinType.PID_ALT1, elem.getAtribute("ParcelIDParcel"));
    			pin.addPin(PinI.PinType.PID_ALT2, elem.getAtribute("ParcelID2_ALTERNATE"));
    			pin.addPin(PinI.PinType.PID_ALT3, elem.getAtribute("ParentParcelID"));
    			pin.addPin(PinI.PinType.GEO_NUMBER, elem.getAtribute("GeoNumber"));
    			prop1.setPin(pin);
    			
    			// set AreaCode and AreaName
    			prop1.setAreaCode(elem.getAtribute("AreaCode"));
    			prop1.setAreaName(elem.getAtribute("AreaName"));
    			
    			if (NumberUtils.isNumber( elem.getAtribute("Acres"))){
    				prop1.setAcres(Double.parseDouble(elem.getAtribute("Acres")));
    			}    			
    			prop1.setDistrict(elem.getAtribute("District"));
    			// set various fields
    			prop1.setPropertyType(elem.getAtribute("PropertyType"));
    			prop1.setMunicipalJurisdiction(elem.getAtribute("MunicipalJurisdiction"));
    			tmp=elem.getAtribute("ThruArea");
    			prop1.setThruAreaCode( StringUtils.isEmpty(tmp)? "": tmp);
    			
    			tmp = elem.getAtribute("YearBuilt");
    			if (!StringUtils.isEmpty(tmp)){
	    			try {
	    				prop1.setYearBuilt(Integer.parseInt(tmp));
	    			} catch (NumberFormatException e){}
    			}
    			
    			// set type
    			tmp = elem.getAtribute("SubdivisionCond");
    			if (!StringUtils.isEmpty(tmp)){
    				prop1.setType(PropertyI.PType.CONDO);
    			} else {
    				prop1.setType(PropertyI.PType.GENERIC);
    			}
    			
				// set legal    			
    			Legal legal = new Legal();
    			
    			// set legal free form
    			// temporary: for i=0, copy PIS.PropertyDescription into PIS.LegalDescriptionOnServer if LegalDescriptionOnServer in not set
    			String legalFreeForm = "";
    			if (i == 0){
    				legalFreeForm = elem.getAtribute("LegalDescriptionOnServer");  // needs to be added to all parsers
    				if (legalFreeForm.length() == 0){
    					legalFreeForm = elem.getAtribute("PropertyDescription");
    					elem.setAtribute("LegalDescriptionOnServer", legalFreeForm);
    				}    				
    			}
    			legal.setFreeForm(legalFreeForm);
    		 	legal.setPartialLegal(getFieldFromVector((Vector)vectorPIS, "PartialLegal"));
    			
    			 // set legal subdivision
    			Subdivision subdiv = new Subdivision();
    			subdiv.setName(elem.getAtribute("SubdivisionName"));
    			subdiv.setSection(elem.getAtribute(PropertyIdentificationSetKey.SECTION.getShortKeyName()));
    			subdiv.setCode(elem.getAtribute("SubdivisionCode"));
    			subdiv.setNumber(elem.getAtribute("SubdivisionNo"));
    			subdiv.setLot(elem.getAtribute("SubdivisionLotNumber"));
    			subdiv.setSubLot(elem.getAtribute("SubLot"));
    			subdiv.setLotThrough(elem.getAtribute("SubdivisionLotThrough"));
    			subdiv.setBlock(elem.getAtribute("SubdivisionBlock"));
    			subdiv.setBlockThrough(elem.getAtribute("SubdivisionBlockThrough"));
    			subdiv.setUnit(elem.getAtribute("SubdivisionUnit"));
    			subdiv.setBuilding(elem.getAtribute("SubdivisionBldg"));    			
    			subdiv.setPhase(elem.getAtribute("SubdivisionPhase"));
    			subdiv.setTract(elem.getAtribute("SubdivisionTract"));
    			subdiv.setAcreage(elem.getAtribute("Acreage"));
    			subdiv.setPlatBook(elem.getAtribute("PlatBook"));
    			subdiv.setPlatPage(elem.getAtribute("PlatNo"));  
    			subdiv.setPlatInstrument(elem.getAtribute("PlatInstr"));
    			subdiv.setPlatDescription(elem.getAtribute("PlatDesc"));
    			subdiv.setNcbNumber(elem.getAtribute("NcbNo"));
    			subdiv.setPlatInstrumentYear(elem.getAtribute("PlatInstrYear"));
    			subdiv.setDistrict(elem.getAtribute("District"));
    			legal.setSubdivision(subdiv);

    			// set legal township
    			TownShip twn =  new TownShip();
    			twn.setSection(elem.getAtribute("SubdivisionSection"));
    			twn.setTownship(elem.getAtribute("SubdivisionTownship"));
    			twn.setRange(elem.getAtribute("SubdivisionRange"));    			
    			twn.setArb(elem.getAtribute("ARB"));
    			twn.setAddition(elem.getAtribute("Addition"));
    			twn.setParcel(elem.getAtribute("SubdivisionParcel"));
    			twn.setArea(elem.getAtribute("Area"));
    			twn.setThruParcel(elem.getAtribute("ThruParcel"));
    			twn.setThruRange(elem.getAtribute("ThruRange"));
    			twn.setAbsNumber(elem.getAtribute("AbsNo"));
    			
    			tmp = elem.getAtribute("ThruQuarterOrder");
    			if (NumberUtils.isDigits(tmp)){
    				twn.setThruQuarterOrder(new Integer(tmp));
    			}
    			twn.setThruQuarterValue(elem.getAtribute("ThruQuarterValue"));
    			twn.setThruSection(elem.getAtribute("ThruSection"));
    			twn.setThruTownship(elem.getAtribute("ThruTownship"));
    			
    			tmp = elem.getAtribute("QuarterOrder");
    			if (!StringUtils.isEmpty(tmp)){
    				if (NumberUtils.isDigits(tmp) ){
    					twn.setQuarterOrder(Integer.parseInt(tmp));
    				}
    			}
    			
    			twn.setQuarterValue(elem.getAtribute("QuarterValue"));    			    		
    			legal.setTownShip(twn);
    			
    			prop1.setLegal(legal);  	
    			list.add(prop1);
			}
		} else {
			prop1.setOwner(party);
			list.add(prop1);
		}
		
		
		CurrentInstance currentInstance = InstanceManager.getManager().getCurrentInstance(searchId);
		County county = currentInstance.getCurrentCounty();
		
    	// set doc info for a AO site		
	    if (isAssessorSite(srcType)){
	    	
	    	InstrumentI instr = new Instrument();
	    	
	    	for(PropertyI prop:list){
	    		instr.setInstno(prop.getPin(PinType.PID));
	    		instr.setDocno(prop.getPin(PinType.PID_ALT1));
	    		break;
	    	}
	    	
	    	instr.setDocType("ASSESSOR");
	    	instr.setDocSubType("ASSESSOR");
	    	instr.setYear( Calendar.getInstance().get(Calendar.YEAR) );
	    	
	    	AssessorDocument assessDoc = new AssessorDocument(DocumentsManager.generateDocumentUniqueId(searchId, instr));
	    	assessDoc.setInstrument(instr);
	    	
	    	assessDoc.setServerDocType("ASSESSOR");        	
	    	assessDoc.setType(SimpleChapterUtils.DType.ASSESOR);
	    	assessDoc.setParsedReferences(crossRefs);
	    	for(PropertyI prop:list){
	    		assessDoc.addProperty(prop);
	    	}
	    	assessDoc.setDataSource(srcType);
	    	
	    	// set TotalAssessment field
	    	tmp = pas.getAtribute("TotalAssessment");
	    	if (!StringUtils.isEmpty(tmp)){
		    	try {
		    		assessDoc.setTotalAssessement(Double.parseDouble(tmp));	
		    	} catch (NumberFormatException e){}	
	    	}
	    	
	    	//set TotalEstimatedTaxes
	    	Vector<TaxHistorySet> vectorTHS = (Vector<TaxHistorySet>)parsedResponse.infVectorSets.get("TaxHistorySet");
	    	tmp = getFieldFromVector((Vector)vectorTHS, "BaseAmount");
	    	double totalEstimatedTaxes = 0d;
	    	if (!StringUtils.isEmpty(tmp)){
		    	try {
		    		totalEstimatedTaxes = Double.parseDouble(tmp);
		    		assessDoc.setTotalEstimatedTaxes(totalEstimatedTaxes);	
		    	} catch (NumberFormatException e){}
	    	}
	    	
	    	try{
		    	Set<RegisterDocumentI> trans = getTransactioHistory(parsedResponse, vectorSDS, searchId);
		    	if(trans!=null){
		    		assessDoc.setTransactionHistory(trans);
		    		
		    		//B 4757
		    		if ("AO".equals(srcType) || "IS".equals(srcType)){
				    		for(RegisterDocumentI c:trans){
				    			assessDoc.addParsedReference(c);
			    			}
		    		}
		    	}
	    	}catch(Exception e){
	    		e.printStackTrace();
	    	}
	    	
			return assessDoc;
			
		// set doc info for a tax site	and for DT site that have a single Tax Document			
	    } else if (isTaxSite(srcType)||isTaxFromDT(srcType, getFieldFromVector((Vector)vectorSDS, "DocumentType")) ) {	  
	    	
	    	InstrumentI instr = new Instrument();
	    	if (isCityTaxSite(srcType)){
	    		instr.setDocType("CITYTAX");
	    		instr.setDocSubType("Citytax");
	    	} else if (isCountyTaxSite(srcType) || isTaxFromDT(srcType, getFieldFromVector((Vector)vectorSDS, "DocumentType"))){
	    		instr.setDocType("COUNTYTAX");
	    		instr.setDocSubType("Countytax");
	        }
	    	
	    	for(PropertyI prop:list){
	    		instr.setInstno(prop.getPin(PinType.PID));
	    		instr.setDocno(prop.getPin(PinType.PID_ALT1));
	    		break;
	    	}
	    	
	    	Vector<TaxHistorySet> vectorTHS = (Vector<TaxHistorySet>)parsedResponse.infVectorSets.get("TaxHistorySet");
	    	// set tax year
	    	tmp = getFieldFromVector((Vector)vectorTHS, "Year");
	    	if (!StringUtils.isEmpty(tmp)){
		    	try {
		    		instr.setYear(Integer.parseInt(tmp));	
		    	} catch (NumberFormatException e){}
	    	} 
	    	if (instr.getYear() == SimpleChapterUtils.UNDEFINED_YEAR ){
	    		//B3787
	    	   	String crtCounty = county.getName();
	    		String crtState = currentInstance.getCurrentState().getStateAbv();
	    		if (crtState.equals("MI") && crtCounty.equals("Wayne")){
	    			//get the tax year from search sites
	    			Date payDate = HashCountyToIndex.getPayDate(currentInstance.getCommunityId(), crtState, crtCounty, DType.CITYTAX);
	    			instr.setYear(payDate.getYear()+1900);
	    		}
	    	}
	    	
	    	TaxDocument taxDoc = null;
	    	
	    	Date payDate = null;
	    	Date dueDate = null;
	    	int taxYearMode = -1;
	    	String crtState = currentInstance.getCurrentState().getStateAbv();
	    	String crtCounty = county.getName();
    		
    		if( isCityTaxSite(srcType) ){
	    		taxDoc = new TaxDocument(DocumentsManager.generateDocumentUniqueId(searchId, instr),DType.CITYTAX);
	    		taxDoc.setServerDocType("CITYTAX");
	    		payDate = HashCountyToIndex.getPayDate(currentInstance.getCommunityId(), crtState, crtCounty, DType.CITYTAX);
	    		dueDate = HashCountyToIndex.getDueDate(currentInstance.getCommunityId(), crtState, crtCounty, DType.CITYTAX);
	    		taxYearMode = HashCountyToIndex.getTaxYearMode(currentInstance.getCommunityId(), crtState, crtCounty, DType.CITYTAX);
	    	}
	    	else if (isCountyTaxSite(srcType) || srcType.equals("DT")){
	    		taxDoc = new TaxDocument(DocumentsManager.generateDocumentUniqueId(searchId, instr),DType.TAX);
	    		taxDoc.setServerDocType("COUNTYTAX");
	    		tmp = getFieldFromVector((Vector)vectorTHS, "TotalDueEP");
	    		if( !StringUtils.isEmpty(tmp) ){
	    			try { taxDoc.setAmountDueEP(Double.parseDouble(tmp)); }		catch (Exception e) {}
	    		}
	    		tmp = getFieldFromVector((Vector)vectorTHS, "PriorDelinquentEP");
	    		if( !StringUtils.isEmpty(tmp) ){
	    			try { taxDoc.setTotalDelinquentEP(Double.parseDouble(tmp)); }		catch (Exception e) {}
	    		}
	    		
	    		tmp = getFieldFromVector((Vector)vectorTHS, "BaseAmountEP");
	    		if( !StringUtils.isEmpty(tmp) ){
	    			try { taxDoc.setBaseAmountEP(Double.parseDouble(tmp)); }		catch (Exception e) {}
	    		}
	    		tmp = getFieldFromVector((Vector)vectorTHS, "TaxYearEPfromTR");
	    		if( !StringUtils.isEmpty(tmp) ){
	    			try { taxDoc.setTaxYearEPfromTR(tmp); }		catch (Exception e) {}
	    		}
	    		//B3285
	    		taxDoc.setAdditionalCityDoc(new TaxDocument(DocumentsManager.generateDocumentUniqueId(searchId, instr),DType.CITYTAX));
	    		payDate = HashCountyToIndex.getPayDate(currentInstance.getCommunityId(), crtState, crtCounty, DType.TAX);
	    		dueDate = HashCountyToIndex.getDueDate(currentInstance.getCommunityId(), crtState, crtCounty, DType.TAX);
	    		taxYearMode = HashCountyToIndex.getTaxYearMode(currentInstance.getCommunityId(), crtState, crtCounty, DType.TAX);
	    	}
    		
    		//update Pay Date and Due Date
    		setPayAndDueDates(instr, taxDoc, payDate, dueDate, taxYearMode);
    		
    		taxDoc.setTaxYearMode(taxYearMode);
    		
    		taxDoc.setInstrument(instr);
	    	    	
	    	taxDoc.setParsedReferences(crossRefs);	  
	    	for(PropertyI prop:list){
	    		taxDoc.addProperty(prop);
	    	}
	    	taxDoc.setDataSource(srcType);
	    	
	    	// set appraisal and assessment fields
	    	tmp = pas.getAtribute("LandAppraisal");
	    	if (!StringUtils.isEmpty(tmp)){
		    	try {
		    		taxDoc.setAppraisedValueLand(Double.parseDouble(tmp));	
		    	} catch (NumberFormatException e){}
	    	}
	    	tmp = pas.getAtribute("ImprovementAppraisal");
	    	if (!StringUtils.isEmpty(tmp)){
		    	try {
		    		taxDoc.setAppraisedValueImprovements(Double.parseDouble(tmp));	
		    	} catch (NumberFormatException e){}
	    	}
	    	tmp = pas.getAtribute("TotalAppraisal");
	    	if (!StringUtils.isEmpty(tmp)){
		    	try {
		    		taxDoc.setAppraisedValueTotal(Double.parseDouble(tmp));	
		    	} catch (NumberFormatException e){}
	    	}
	    	tmp = pas.getAtribute("TotalAssessment");
	    	if (!StringUtils.isEmpty(tmp)){
		    	try {
		    		taxDoc.setTotalAssessment(Double.parseDouble(tmp));	
		    	} catch (NumberFormatException e){}
	    	}
	    	tmp = getFieldFromVector((Vector)vectorTHS, "TaxExemptionAmount");
	    	if (!StringUtils.isEmpty(tmp)){
		    	try {
		    		taxDoc.setExemptionAmount(Double.parseDouble(tmp));	
		    	} catch (NumberFormatException e){}
	    	}
	    	tmp = getFieldFromVector((Vector)vectorTHS, "TaxYearDescription");
	    	if (!StringUtils.isEmpty(tmp)){
		    	try {
		    		taxDoc.setTaxYearDescription(tmp);	
		    	} catch (NumberFormatException e){}
	    	}
	    	// set tax amounts
	    	tmp = getFieldFromVector((Vector)vectorTHS, "SplitPaymentAmount");
	    	if (!StringUtils.isEmpty(tmp)){
		    	try {
		    		taxDoc.setSplitPaymentAmount(Double.parseDouble(tmp));	
		    	} catch (NumberFormatException e){}
	    	}
	    	tmp = getFieldFromVector((Vector)vectorTHS, "BaseAmount");
	    	double baseAmount = 0d;
	    	if (!StringUtils.isEmpty(tmp)){
		    	try {
		    		baseAmount = Double.parseDouble(tmp);
		    		taxDoc.setBaseAmount(baseAmount);	
		    	} catch (NumberFormatException e){}
	    	}
	    	tmp = getFieldFromVector((Vector)vectorTHS, "PriorDelinquent");
	    	if (!StringUtils.isEmpty(tmp)){
		    	try {
		    		taxDoc.setFoundDelinquent(Double.parseDouble(tmp));	
		    	} catch (NumberFormatException e){}
	    	}
	    	tmp = getFieldFromVector((Vector)vectorTHS, "AmountPaid");
	    	if (!StringUtils.isEmpty(tmp)){
		    	try {
		    		taxDoc.setAmountPaid(Double.parseDouble(tmp));	
		    	} catch (NumberFormatException e){}
	    	}
	    	tmp = getFieldFromVector((Vector)vectorTHS, "TotalDue");
	    	if (!StringUtils.isEmpty(tmp)){
		    	try {
		    		taxDoc.setAmountDue(Double.parseDouble(tmp));	
		    	} catch (NumberFormatException e){}
	    	}
	    	
	    	
	    	String datePaidStr = getFieldFromVector((Vector)vectorTHS, "DatePaid");
			taxDoc.setDatePaid(Util.dateParser3(datePaidStr));
	    	
	    	tmp = getFieldFromVector((Vector)vectorTHS, "TaxVolume");
	    	if (!StringUtils.isEmpty(tmp)){
		    	try {
		    		taxDoc.setTaxVolume(Integer.parseInt(tmp));	
		    	} catch (NumberFormatException e){}
	    	}
	    	taxDoc.setSaleDate(getFieldFromVector((Vector)vectorTHS, "TaxSaleDate"));
	    	taxDoc.setSaleNo(getFieldFromVector((Vector)vectorTHS, "TaxSaleNumber"));
	    	taxDoc.setBillNumber(getFieldFromVector((Vector)vectorTHS, "TaxBillNumber"));
	    	tmp = (String)resultMap.get("OtherInformationSet.ResearchRequired"); 
	    	if (!StringUtils.isEmpty(tmp)){
		    	boolean researchRequired = tmp.equalsIgnoreCase("True");
	    		taxDoc.setResearchRequired(researchRequired);	// set only on IL Cook TU
	    	}
	    	
	    	// set receipts	    	
	    	if (vectorTHS != null){
				int size = vectorTHS.size();
				ArrayList<Receipt> receipts = new ArrayList<Receipt>();				
				for (int i=0; i<size; i++){					    	
					TaxHistorySet elem = vectorTHS.get(i);
					String rcptAmt = elem.getAtribute("ReceiptAmount");
					if (!StringUtils.isEmpty(rcptAmt)){ 
						Receipt rcpt = new Receipt();
						rcpt.setReceiptAmount(rcptAmt);
						rcpt.setReceiptDate(elem.getAtribute("ReceiptDate"));
						rcpt.setReceiptNumber(elem.getAtribute("ReceiptNumber"));
						receipts.add(rcpt);
					}
				}
				if (receipts.size() != 0){
					taxDoc.setReceipts(receipts);
				}
	    	}
			//set datePaid from receipts if it is not filled at parse
	    	if (taxDoc.getAmountPaid()>0 && taxDoc.getDatePaid() ==null){
				String dateLastPaid = TemplateBuilder.getDateLastPaid(taxDoc, baseAmount);
				if (StringUtils.isNotEmpty(dateLastPaid)){
					taxDoc.setDatePaid(Util.dateParser3(dateLastPaid));
				}
	    	}
	    	
	    	// set installments
	    	Vector<TaxInstallmentSet> vectorTIS = (Vector<TaxInstallmentSet>)parsedResponse.infVectorSets.get("TaxInstallmentSet");
	    	if (vectorTIS != null){
				int size = vectorTIS.size();
				ArrayList<Installment> installments = new ArrayList<Installment>();				
				for (int i=0; i<size; i++){					    	
					TaxInstallmentSet elem = vectorTIS.get(i);
					Installment install = new Installment();
					tmp = elem.getAtribute("BaseAmount");
					if (!StringUtils.isEmpty(tmp)){
						try {
							install.setBaseAmount(Double.parseDouble(tmp));
						} catch (NumberFormatException e){}
					}
					tmp = elem.getAtribute("AmountPaid");
					if (!StringUtils.isEmpty(tmp)){
						try {
							install.setAmountPaid(Double.parseDouble(tmp));
						} catch (NumberFormatException e){}
					}
					tmp = elem.getAtribute("TotalDue");
					if (!StringUtils.isEmpty(tmp)){
						try {
							install.setAmountDue(Double.parseDouble(tmp));
						} catch (NumberFormatException e){}
					}
					tmp = elem.getAtribute("PenaltyAmount");
					if (!StringUtils.isEmpty(tmp)){
						try {
							install.setPenaltyAmount(Double.parseDouble(tmp));
						} catch (NumberFormatException e){}
					}
					tmp = elem.getAtribute(TaxInstallmentSetKey.HOMESTEAD_EXEMPTION.getShortKeyName());
					if (!StringUtils.isEmpty(tmp)){
						try {
							install.setHomesteadExemption(Double.parseDouble(tmp));
						} catch (NumberFormatException e){}
					}
					install.setStatus(elem.getAtribute("Status"));
					install.setYearDescription(elem.getAtribute("TaxYearDescription"));
					install.setBillType(elem.getAtribute("TaxBillType"));					
					installments.add(install);
				}
				if (installments.size() != 0){
					taxDoc.setInstallments(installments);
				}
	    	}
	    	
	    	// set special assessments installments
	    	Vector<SpecialAssessmentSet> vectorSAS = (Vector<SpecialAssessmentSet>)parsedResponse.infVectorSets.get("SpecialAssessmentSet");
	    	if (vectorSAS != null){
				int size = vectorSAS.size();
				ArrayList<Installment> specialAssessments = new ArrayList<Installment>();				
				for (int i=0; i<size; i++){					    	
					SpecialAssessmentSet elem = vectorSAS.get(i);
					Installment install = new Installment();
					tmp = elem.getAtribute("BaseAmount");
					if (!StringUtils.isEmpty(tmp)){
						try {
							install.setBaseAmount(Double.parseDouble(tmp));
						} catch (NumberFormatException e){}
					}
					tmp = elem.getAtribute("AmountPaid");
					if (!StringUtils.isEmpty(tmp)){
						try {
							install.setAmountPaid(Double.parseDouble(tmp));
						} catch (NumberFormatException e){}
					}
					tmp = elem.getAtribute("TotalDue");
					if (!StringUtils.isEmpty(tmp)){
						try {
							install.setAmountDue(Double.parseDouble(tmp));
						} catch (NumberFormatException e){}
					}
					tmp = elem.getAtribute("PenaltyAmount");
					if (!StringUtils.isEmpty(tmp)){
						try {
							install.setPenaltyAmount(Double.parseDouble(tmp));
						} catch (NumberFormatException e){}
					}
					tmp = elem.getAtribute(TaxInstallmentSetKey.HOMESTEAD_EXEMPTION.getShortKeyName());
					if (!StringUtils.isEmpty(tmp)){
						try {
							install.setHomesteadExemption(Double.parseDouble(tmp));
						} catch (NumberFormatException e){}
					}
					install.setStatus(elem.getAtribute("Status"));				
					specialAssessments.add(install);
				}
				if (specialAssessments.size() != 0){
					taxDoc.setSpecialAssessmentInstallments(specialAssessments);				
				}
	    	}
	    	
	    	// set bond and assessments details
	    	Vector<BondsAndAssessmentsSet> vectorBAS = (Vector<BondsAndAssessmentsSet>)parsedResponse.infVectorSets.get("BondsAndAssessmentsSet");
	    	if (vectorBAS != null){
				int size = vectorBAS.size();
				ArrayList<BondAndAssessment> bas = new ArrayList<BondAndAssessment>();				
				for (int i=0; i<size; i++){					    	
					BondsAndAssessmentsSet elem = vectorBAS.get(i);
					BondAndAssessment ba = new BondAndAssessment();
					ba.setEntityNo(elem.getAtribute("BondEntityNumber"));
					ba.setCityDistrict(elem.getAtribute("District"));						
					ba.setTreasurerSerDist(elem.getAtribute("BondSeries"));
					ba.setImprovementOf(elem.getAtribute("ImprovementOf"));
					ba.setRecordedDate(elem.getAtribute("RecordedFromDate"));
					bas.add(ba);
				}
				if (bas.size() != 0){
					taxDoc.setBonds(bas);
					
				}
	    	}
	    	
	    	// set prior years delinquent details
	    	Vector<PriorYearsDelinquentSet> vectorPYDS = (Vector<PriorYearsDelinquentSet>)parsedResponse.infVectorSets.get("PriorYearsDelinquentSet");
	    	if (vectorPYDS != null){
				int size = vectorPYDS.size();
				ArrayList<PriorYearsDelinquent> pyds = new ArrayList<PriorYearsDelinquent>();				
				for (int i=0; i<size; i++){					    	
					PriorYearsDelinquentSet elem = vectorPYDS.get(i);
					PriorYearsDelinquent pyd = new PriorYearsDelinquent();
					pyd.setInstallment(elem.getAtribute("Installment"));
					pyd.setTaxBillType(elem.getAtribute("TaxBillType"));
					pyd.setTaxPeriod(elem.getAtribute("TaxPeriod"));
					tmp = elem.getAtribute("AmountDue");
					if (!StringUtils.isEmpty(tmp)){
						try {
							pyd.setAmountDue(Double.parseDouble(tmp));
						} catch (NumberFormatException e){}
					}
					pyds.add(pyd);
				}
				if (pyds.size() != 0){
					taxDoc.setPriorYears(pyds);
				}
	    	}
	    	
	    	// set tax redemption
	    	Vector<TaxRedemptionSet> vectorTRS = (Vector<TaxRedemptionSet>)parsedResponse.infVectorSets.get("TaxRedemptionSet");
	    	if (vectorTRS != null){
				int size = vectorTRS.size();
				ArrayList<TaxRedemption> trs = new ArrayList<TaxRedemption>();				
				for (int i=0; i<size; i++){					    	
					TaxRedemptionSet elem = vectorTRS.get(i);
					TaxRedemption tr = new TaxRedemption();
					tr.setMonth(elem.getAtribute("Month"));
					tmp = elem.getAtribute("Year");
					if (!StringUtils.isEmpty(tmp)){
						try {
							tr.setYear(Integer.parseInt(tmp));
						} catch (NumberFormatException e){}
					}
					tmp = elem.getAtribute("Amount");
					if (!StringUtils.isEmpty(tmp)){
						try {
							tr.setAmount(Double.parseDouble(tmp));
						} catch (NumberFormatException e){}
					}
					trs.add(tr);
				}
				if (trs.size() != 0){
					taxDoc.setRedemtions(trs);
				}
	    	}
	    	
	    	try{
	    		Set<RegisterDocumentI> trans = getTransactioHistory(parsedResponse, vectorSDS, searchId);
	    		if(trans!=null){
	    			for(RegisterDocumentI c:trans){
	    				taxDoc.addParsedReference(c.getInstrument());
	    			}
	    		}
	    	}catch(Exception e){
	    		e.printStackTrace();
	    	}
	    	
	    	return taxDoc;
	    	
    	// set doc info for a RO like site	    	
	    } else {
	    	InstrumentI instr = new Instrument();
	    	if(srcType.equals("SK")) {
	    		instr = new SKLDInstrument();
			}
			String book  = getFieldFromVector((Vector)vectorSDS, "Book");
			String page = getFieldFromVector((Vector)vectorSDS, "Page"); 
			
			if("0".equals(book)){
				book="";
			}
			
			if("0".equals(page)){
				page="";
			}
			
	    	instr.setBook(book);
			instr.setPage(page);
			
			instr.setInstno(getFieldFromVector((Vector)vectorSDS, "InstrumentNumber"));
	    	instr.setDocno(getFieldFromVector((Vector)vectorSDS, "DocumentNumber"));
	    	
	    	instr.setBookType(getFieldFromVector((Vector)vectorSDS, "BookType"));
	    	
	    	String serverDocType = getFieldFromVector((Vector)vectorSDS, "DocumentType"); 
	    	
	    	serverDocType = serverDocType.replaceAll("\\s+", " ").toUpperCase();
	    	
	    	String docCateg = DocumentTypes.getDocumentCategory(serverDocType, searchId); 
        	instr.setDocType(docCateg);
        	String stype = DocumentTypes.getDocumentSubcategory(serverDocType, searchId);
        	if("MISCELLANEOUS".equals(stype)&&!"MISCELLANEOUS".equals(docCateg)){
        		stype = docCateg;
        	}
        	instr.setDocSubType(stype);
	    	
        	// set recordedYear
			String recDate = getFieldFromVector((Vector)vectorSDS, "RecordedDate"); 
			if (recDate.length() != 0 && !recDate.trim().equalsIgnoreCase("N/A")){
				Date date = Util.dateParser3(recDate);
				if(date!=null){
					instr.setYear(1900+date.getYear());
				}
			}
        	
			RegisterDocument docR = new RegisterDocument( DocumentsManager.generateDocumentUniqueId(searchId, instr) );
			
			docR.setInstrument(instr);
			
	        // set type, docType, subType, serverDocType, docTypeAbbrev and docClass
        	docR.setServerDocType(serverDocType);
        	docR.setShortServerDocType(getFieldFromVector((Vector)vectorSDS, "DocTypeAbbrev"));
        	docR.setDocumentClass(getFieldFromVector((Vector)vectorSDS, "BookType"));
        	docR.setType(SimpleChapterUtils.DType.ROLIKE);
        	
        	// set dataSource
        	String state = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentState().getStateAbv();
			if("IL".equals(state)){
				String ds = srcType;
				if(parsedResponse instanceof ParsedResponse && "PI".equals(ds)){
					if("true".equals(  ((ParsedResponse)parsedResponse).getAttribute(ParsedResponse.REAL_PI)) ){
						docR.setSearchType(SearchType.PI);
						docR.setDataSource("PI");
					}else{
						docR.setSearchType(SearchType.PI);
						docR.setDataSource("LA");
					}
				}else if("PI".equals(ds)){
					docR.setSearchType(SearchType.PI);
					docR.setDataSource("LA");
				}
				else if("GI".equals(ds)){
					docR.setSearchType(SearchType.GI);
					docR.setDataSource("LA");
				}
				else if("SG".equals(ds)){
					docR.setSearchType(SearchType.SG);
					docR.setDataSource("LA");
				}
				else{
					docR.setDataSource(srcType);
				}
			}
			else{
				docR.setDataSource(srcType);
			}
	        
	        docR.setParsedReferences(crossRefs);	
	        for(PropertyI prop:list){
	        	docR.addProperty(prop);
	        }
			
			// set instrumentDate
			String instrumentDate = getFieldFromVector((Vector)vectorSDS, "InstrumentDate"); 
			if (instrumentDate.length() != 0){ 
				Date date = Util.dateParser3(instrumentDate);
				docR.setInstrumentDate(date);
				if (date != null){
					docR.setInstrumentYear(1900+date.getYear());
				}
				else{
					docR.setInstrumentYear(1963);
				}
			}
			
			// set recordedDate
			if (recDate.length() != 0){ 
				Date date = Util.dateParser3(recDate);
				if(date!=null){
					Util.setTime(date, getFieldFromVector((Vector)vectorSDS, "RecordedTime") );
					docR.setRecordedDate(date);
				}
			}
			
			// set grantors
			Party grantors = new Party(PType.GRANTOR);
			String gtor = getFieldFromVector((Vector)vectorSDS, "Grantor");
			if(!StringUtils.isEmpty(gtor)) {
				gtor = gtor.replaceAll("\\s*[/]\\s*$","");
				grantors.setFreeParsedForm(gtor.replaceAll("[/]"," and "));	
			}
			
			List<String> allNames = new ArrayList<String>();
			if(gtor!=null){
				allNames = Arrays.asList(gtor.split("[/]"));
			}
			
			Vector<GrantorSet> vectorGtorS = (Vector<GrantorSet>)parsedResponse.infVectorSets.get("GrantorSet");	
			setNamesFromSet(vectorGtorS, grantors);
			
			/*if(docCateg.equals(DocumentTypes.HOA)){
				vectorGtorS = (Vector<GrantorSet>)parsedResponse.infVectorSets.get("BoardMemberSet");
				setBoardNamesFromSet(vectorGtorS, grantors);
			}*/
			
			//Bug 4079
			if(docR.isOneOf(DocumentTypes.PLAT,
					DocumentTypes.RESTRICTION,
					DocumentTypes.EASEMENT, 
					DocumentTypes.MASTERDEED,
					DocumentTypes.CCER)) {
				
				try {
					Search search = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
					search.setAdditionalInfo("Grantor_FIX_for" + docR.getId(), grantors.getNames());
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if("TN".equalsIgnoreCase(state) && allNames.size()>5) { 
					//B 4122
					allNames = allNames.subList(0, 5);
				}
				LinkedHashSet<NameI> names = new LinkedHashSet<NameI>();
				for(String t:allNames){
					Name name = new Name();
					name.setLastName(t);
					name.setTokenized(false);
					names.add(name);
				}
				grantors.setNames(names);
			}
			
			docR.setGrantor(grantors);
			String ssn4 = getFieldFromVector((Vector)vectorPIS, "SSN4");
			if( !StringUtils.isEmpty(ssn4) ) {
				if(grantors.getNames().size()==1) {
					Iterator<NameI> it = grantors.getNames().iterator();
					NameI name = it.next();
					name.setSsn4Decoded(ssn4);
					docR.setAllSSN(ssn4);
				}
			}	
			if (docCateg.equals(DocumentTypes.MORTGAGE)){
				Mortgage mtg = new Mortgage(docR);

				// set grantees lander and trustee
				Party granteesLander = new Party(PType.GRANTEE);
				Party granteesTrustee = new Party(PType.GRANTEE);
				
				//granteesLander.setFreeForm(getFieldFromVector((Vector)vectorSDS, "GranteeLander"));	
				//granteesTrustee.setFreeForm(getFieldFromVector((Vector)vectorSDS, "Grantee"));
				Vector<GranteeSet> vectorGteeS = (Vector<GranteeSet>)parsedResponse.infVectorSets.get("GranteeSet");
				setNamesTypeFromSet(vectorGteeS, granteesLander, granteesTrustee);
				
				Party grantee = new Party(PType.GRANTEE);
				for(NameI name:granteesLander.getNames()){
					NameMortgageGranteeI nameGrantee = new NameMortgageGrantee(name);
					nameGrantee.setTrustee(false);
					grantee.add( nameGrantee );
				}
				
				for(NameI name:granteesTrustee.getNames()){
					NameMortgageGranteeI nameGrantee = new NameMortgageGrantee(name);
					nameGrantee.setTrustee(true);
					grantee.add( nameGrantee );
				}
				
				String trusteeFreeForm = getFieldFromVector((Vector)vectorSDS, "Grantee");
				String lenderFreeForm = getFieldFromVector((Vector)vectorSDS, "GranteeLander");
				String freeForm = "";
				if(!StringUtils.isEmpty(trusteeFreeForm)) {
					freeForm += trusteeFreeForm;
				}
				if(!StringUtils.isEmpty(lenderFreeForm)) {
					if(!freeForm.isEmpty()) {
						freeForm += " / ";
					}
					freeForm += lenderFreeForm;
				}
				freeForm = freeForm.replaceAll("\\s*[/]\\s*$","");
				grantee.setFreeParsedForm(freeForm.replaceAll("[/]"," and "));	
				mtg.setGrantee(grantee);
				
				// set mortgage amount 
				tmp = getFieldFromVector((Vector)vectorSDS, "MortgageAmount");
				if (!StringUtils.isEmpty(tmp)){
					try {
						mtg.setMortgageAmount(Double.parseDouble(tmp));
					} catch (NumberFormatException e){
						e.printStackTrace();
					}
					mtg.setMortgageAmountFreeForm(tmp);
				}
				return mtg;
			}	
			
			// set grantees
			Party grantees = new Party(PType.GRANTEE);
			
			String gtee = getFieldFromVector((Vector)vectorSDS, "Grantee");
			if(!StringUtils.isEmpty(gtee)) {
				gtee = gtee.replaceAll("\\s*[/]\\s*$","");
				grantees.setFreeParsedForm(gtee.replaceAll("[/]"," and "));	
			}
			
			allNames = new ArrayList<String>();
			if(gtee!=null){
				allNames = Arrays.asList(gtee.split("[/]"));
			}
			
			Vector<GranteeSet> vectorGteeS = (Vector<GranteeSet>)parsedResponse.infVectorSets.get("GranteeSet");
			setNamesFromSet(vectorGteeS, grantees);
			
			if(docCateg.equals(DocumentTypes.HOA)){
				Name name = new Name();	    					    			
				name.setLastName(gtee);
				grantees.add(name);
			}
			
			//Bug 4079
			if(docR.isOneOf(DocumentTypes.PLAT,
					DocumentTypes.RESTRICTION,
					DocumentTypes.EASEMENT, 
					DocumentTypes.MASTERDEED,
					DocumentTypes.CCER)) {
				LinkedHashSet<NameI> names = new LinkedHashSet<NameI>();
				
				for(String t:allNames){
					Name name = new Name();
					name.setLastName(t);
					name.setTokenized(false);
					names.add(name);
				}
				
				grantees.setNames(names);
			} else if(docCateg.equals(DocumentTypes.WHITEPAGES) || docCateg.equals(DocumentTypes.LEXISNEXIS)){
				Name name = new Name();	    					    			
				name.setLastName(gtee);
				grantees.add(name);
			} else if(srcType.equals("NR")){
				Name name = new Name();	    					    			
				name.setLastName(gtee);
				grantees.add(name);
			} else if(srcType.equals("DD")){
				Name name = new Name();	    					    			
				name.setLastName(gtee);
				grantees.add(name);
			} else if(srcType.equals("DR")){
				Name name = new Name();	    					    			
				name.setLastName(gtee);
				grantees.add(name);
			} else if(srcType.equals("IL")){
				Name name = new Name();	    					    			
				name.setLastName(gtee);
				grantees.add(name);
			} else if("LW".equals(srcType)){
				Name name = new Name();	    					    			
				name.setLastName(gtee);
				grantees.add(name);
			}
			
			docR.setGrantee(grantees);
			
			if (docCateg.equals(DocumentTypes.TRANSFER)){
				Transfer tran = new Transfer(docR); 
				String salePrice = getFieldFromVector((Vector)vectorSDS, "SalesPrice");
				
				if (!StringUtils.isEmpty(salePrice)){
					try {
						tran.setSalePrice(Double.parseDouble(salePrice.replaceAll("[$,]", "")));
					} catch (NumberFormatException e){}
				}
				
				if(tran.getSalePrice()<=0){
					String amountT = (String)resultMap.get("OtherInformationSet.Amount");
			        if (amountT == null){
			        	amountT = "";
			        }
			        if (!StringUtils.isEmpty(amountT)){
						try {
							tran.setSalePrice(Double.parseDouble(amountT.replaceAll("[$,]", "")));
						} catch (NumberFormatException e){}
					}
				}
				
				String amountT = getFieldFromVector((Vector)vectorSDS, "ConsiderationAmount");
				if (!StringUtils.isEmpty(amountT)){
					if(tran.getSalePrice()<=0){
						try {
							tran.setSalePrice(Double.parseDouble(amountT.replaceAll("[$,]", "")));
						} catch (NumberFormatException e){
							e.printStackTrace();
						}
					}
					try {
						tran.setConsiderationAmount(Double.parseDouble(amountT.replaceAll("[$,]", "")));
					} catch (NumberFormatException e){
						e.printStackTrace();
					}
					tran.setConsiderationAmountFreeForm(amountT.replaceAll("[$,]", ""));
				}
				
				return tran;
			}
			if(docCateg.equals(DocumentTypes.COURT)){
				Court court = new Court(docR);
				tmp = getFieldFromVector((Vector)vectorSDS, "ConsiderationAmount");
				if (!StringUtils.isEmpty(tmp)){
					try {
						court.setConsiderationAmount(Double.parseDouble(tmp));
					} catch (NumberFormatException e){
						e.printStackTrace();
					}
					court.setConsiderationAmountFreeForm(tmp);
				}
				
				if ("JUDGMENT".equalsIgnoreCase(stype)){
					String remarks = (String) resultMap.get(OtherInformationSetKey.REMARKS.getKeyName());
					if (StringUtils.isNotEmpty(remarks)){
						court.setNote(remarks);
					}
				}
				
				return court;
			}
			if(docCateg.equals(DocumentTypes.LIEN)){
				Lien lien = new Lien(docR);
				// set mortgage amount 
				tmp = getFieldFromVector((Vector)vectorSDS, "ConsiderationAmount");
				if (!StringUtils.isEmpty(tmp)){
					try {
						lien.setConsiderationAmount(Double.parseDouble(tmp));
					} catch (NumberFormatException e){
						e.printStackTrace();
					}
					lien.setConsiderationAmountFreeForm(tmp);
				}
				String remarks = (String) resultMap.get(OtherInformationSetKey.REMARKS.getKeyName());
				if (StringUtils.isNotEmpty(remarks)) {
					lien.setNote(remarks);
				}
				return lien;
			}
			if (docCateg.equals(DocumentTypes.MISCELLANEOUS) && "Death Certificate".equals(docR.getDocSubType())){
				String remarks = (String) resultMap.get(OtherInformationSetKey.REMARKS.getKeyName());
				if (StringUtils.isNotEmpty(remarks)){
					docR.setNote(remarks);
				}
			}
			
			if("SF".equalsIgnoreCase(srcType)){
				SSFPriorFileDocument ssfPriorFileDocument = new SSFPriorFileDocument(docR);
				//ssfPriorFileDocument.setOriginalId(originalId);
				//ssfPriorFileDocument.setOriginalId(originalId);
				return ssfPriorFileDocument;
			}
			else if(srcType.equals("PF") || docCateg.equals("OTHER-FILE")) {
				PriorFileDocumentI priorFileDocumentI = new PriorFileDocument(docR);
				return priorFileDocumentI;
			} else if("WP".equals(srcType) || "WHITEPAGES".equals(docCateg)) {
				WhitePagesDocumentI whitePagesDocumentI = new WhitePagesDocument(docR);
				return whitePagesDocumentI;
			} else if(srcType.equals("NR")) {
				DeathRecordsDocumentI deathRecordsDocumentI = new DeathRecordsDocument(docR);
				return deathRecordsDocumentI;
			} else if("LN".equals(srcType) || "LEXISNEXIS".equals(docCateg)) {
				LexisNexisDocumentI lexisNexisDocumentI = new LexisNexisDocument(docR);
				return lexisNexisDocumentI;
			} else if(srcType.equals("DR")) {
				DeathRecordsDocumentI deathRecordsDocumentI = new DeathRecordsDocument(docR);
				return deathRecordsDocumentI;
			} else if(srcType.equals("IL")) {
				OffenderInformationDocumentI offenderInformationDocumentI = new OffenderInformationDocument(docR);
				return offenderInformationDocumentI;
			} else if("LW".equals(srcType) || "STATEBAR".equals(docCateg)) {
				StateBarDocumentI stateBarDocumentI = new StateBarDocument(docR);
				return stateBarDocumentI;
			} else if(srcType.equals("MH")) {
				ManufacturedHousingDocumentI manufacturedHousingDocumentI = new ManufacturedHousingDocument(docR);
				return manufacturedHousingDocumentI;
			} else if(srcType.equals("BS")) {
				BankSuccessorDocumentI bankSuccessorDocumentI = new BankSuccessorDocument(docR);
				return bankSuccessorDocumentI;
			} else if(srcType.equals("MERS")) {
				MERSDocumentI mersDocumentI = new MERSDocument(docR);
				return mersDocumentI;
			}		
			
			if(docCateg.equals(DocumentTypes.CCER)){
				Ccer convertedDoc = new Ccer(docR);
				return convertedDoc;
			}
			
			if (docCateg.equals(DocumentTypes.PLAT)){
				Plat convertedDoc = new Plat(docR);
				return convertedDoc;
			}
			
			if(docCateg.equals(DocumentTypes.HOA)){
				HOACondo hoaDoc = new HOACondo(docR);
				if (vectorPIS != null){
					int size = vectorPIS.size();
					ArrayList<HOAInfo> hoaInfos = new ArrayList<HOAInfo>();				
					for (int i=0; i<size; i++){					    	
						PropertyIdentificationSet elem = vectorPIS.get(i);
						String subName = elem.getAtribute("SubdivisionName");
						if (!StringUtils.isEmpty(subName)){ 
							HOAInfo hoainfo = new HOAInfo();
							hoainfo.setAssociationName(subName);
							hoainfo.setHOAName(elem.getAtribute("tmpHOAName"));
							hoainfo.setMasterHOA(elem.getAtribute("tmpMasterHOA"));
							hoainfo.setAddHOA(elem.getAtribute("tmpAddHOA"));
							hoainfo.setHoaPlatBook(elem.getAtribute("tmpPlatBook"));
							hoainfo.setHoaPlatPage(elem.getAtribute("tmpPlatPage"));
							hoainfo.setHoaDeclBook(elem.getAtribute("tmpDeclBook"));
							hoainfo.setHoaDeclPage(elem.getAtribute("tmpDeclPage"));
							hoaInfos.add(hoainfo);
						}
					}
					if (hoaInfos.size() != 0){
						hoaDoc.setHOAInfo(hoaInfos);
					}
		    	}
				return hoaDoc;
			}
			
			if (docCateg.equals(DocumentTypes.CORPORATION)) {
				Corporation corporation = new Corporation(docR);
				String status = org.apache.commons.lang.StringUtils.defaultIfEmpty(
						otherInformationSet.getAtribute(OtherInformationSetKey.REMARKS.getShortKeyName()), "");
				corporation.setStatus(status);
				docR = corporation;
			}
			return docR;
		}
    }

	/**
	 * @param instr
	 * @param taxDoc
	 * @param payDate
	 * @param dueDate
	 * @param taxYearMode
	 */
	public static void setPayAndDueDates(InstrumentI instr, TaxDocument taxDoc, Date payDate, Date dueDate, int taxYearMode) {
		int currentYear = instr.getYear();
		int payDateYear = -1;
		if (payDate!=null && currentYear!=SimpleChapterUtils.UNDEFINED_YEAR) {
			int taxYearOffset = 0;
			if (taxYearMode == TaxSiteData.TAX_YEAR_PD_YEAR_MINUS_1) {
				taxYearOffset = -1;
			} else if (taxYearMode == TaxSiteData.TAX_YEAR_PD_YEAR_PLUS_1) {
				taxYearOffset = 1;
			}
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
			String payDateYearString = sdf.format(payDate);
			payDateYear = Integer.parseInt(payDateYearString);
			payDateYear += taxYearOffset;
			Calendar payDateCalendar = Calendar.getInstance();
			payDateCalendar.setTime(payDate);
			payDateCalendar.add(Calendar.YEAR, currentYear - payDateYear);
			payDate = payDateCalendar.getTime();
			taxDoc.setPayDate(payDate);
		}
		if (dueDate!=null && payDateYear!=-1) {
			Calendar dueDateCalendar = Calendar.getInstance();
			dueDateCalendar.setTime(dueDate);
			dueDateCalendar.add(Calendar.YEAR, currentYear - payDateYear);
			dueDate = dueDateCalendar.getTime();
			taxDoc.setDueDate(dueDate);
		}
	}

    /**
     * Parses a full address into an AddressI object
     * @param address if null a new object will be created and returned
     * @param fullStreet the address to be tokenized
     * @return the address filled
     */
	public static AddressI fillAddressI(AddressI address, String fullStreet) {
		
		StandardAddress tokAddr = new StandardAddress(fullStreet);
		
		if(address == null) {
			address = new Address();
		}
		
		address.setNumber(tokAddr.getAddressElement(StandardAddress.STREET_NUMBER));
		address.setStreetName(tokAddr.getAddressElement(StandardAddress.STREET_NAME));
		address.setPostDirection(tokAddr.getAddressElement(StandardAddress.STREET_POSTDIRECTIONAL));
		address.setPreDiretion(tokAddr.getAddressElement(StandardAddress.STREET_PREDIRECTIONAL));			
		address.setIdentifierType(tokAddr.getAddressElement(StandardAddress.STREET_SEC_ADDR_IDENT));
		address.setIdentifierNumber(tokAddr.getAddressElement(StandardAddress.STREET_SEC_ADDR_RANGE));	
		address.setSuffix(tokAddr.getAddressElement(StandardAddress.STREET_SUFFIX));
		
		return address;
	}

	/**
	 * it extracts the referred instruments from cross ref vector populated in the parsing process.
	 * {@link TODO this method should be placed in a utility class or in a class that deal with instruments }
	 * @param vectorCRS
	 * @param srcType 
	 * @param searchId
	 * @return
	 */
	public static Set<InstrumentI> extractCrossRefs(Vector<CrossRefSet> vectorCRS, String srcType, long searchId) {
		Set<InstrumentI> crossRefs = new HashSet<InstrumentI>();
    	if(vectorCRS != null){
    		int size = vectorCRS.size();
    		for (int i=0; i<size; i++){
    			if(vectorCRS.get(i) instanceof CrossRefSet) {
    				CrossRefSet elem = (CrossRefSet)(vectorCRS.get(i));
        			InstrumentI instr = new Instrument();
        			if("SK".equals(srcType)) {
        				instr = new SKLDInstrument();
        			}
        			String page = elem.getAtribute("Page");
        			String book = elem.getAtribute("Book");
        			
        			if("0".equalsIgnoreCase(page)){
        				page = "";
        			}
        			
        			if("0".equalsIgnoreCase(book)){
        				book="";
        			}
        			 
        			instr.setBook(book);
        			instr.setPage(page);
        			
        			instr.setInstno(elem.getAtribute("InstrumentNumber"));
        			String docType = elem.getAtribute("DocumentType");
        			
        			if (org.apache.commons.lang.StringUtils.isNotEmpty(docType)){
	        			if ("PI".equals(srcType) || "DG".equals(srcType)) {
	        				instr.setServerDocType(docType);
	        				instr.setDocType(DocumentTypes.getDocumentCategory(docType, searchId));
	        				instr.setDocSubType(DocumentTypes.getDocumentSubcategory(docType, searchId));
	        			} else{
	        				instr.setDocType(docType);
	        			}
        			}
        			String subDocType = elem.getAtribute("DocSubtype");
        			if (org.apache.commons.lang.StringUtils.isNotEmpty(subDocType)){
        				instr.setDocSubType(subDocType);
        			}
        			String type = elem.getAtribute("Book_Page_Type");		// type not yet stored in Instrument
        			if (StringUtils.isEmpty(type)){
        				type = elem.getAtribute("Instrument_Ref_Type");
        			}
        			if (instr.hasNecessaryIdentification()){
        				crossRefs.add(instr);
        			}
        			String month = elem.getAtribute("Month");
        			String day = elem.getAtribute("Day");
        			String yearStr = elem.getAtribute("Year");
        			if(StringUtils.isNotEmpty(month)&&StringUtils.isNotEmpty(day)&&StringUtils.isNotEmpty(yearStr)){
        				instr.setDate(Util.dateParser3(month+"/"+day+"/"+yearStr));
        			}
        			int year = -1;
        			try{year = Integer.parseInt(yearStr);}catch(Exception e){}
        			instr.setYear(year);
				}
    		}    		
    	}
		return crossRefs;
	}

    protected String getColumn(ResultTable rt, String col, String pm, int index) throws Exception {
        col=col.trim();
        int pos=col.indexOf('+');
        if (pos!=-1) {
            return getColumn(rt, col.substring(0, pos), pm, index)+
                   getColumn(rt, col.substring(pos+1), pm, index);
        }
        if (col.startsWith("'")) {
            if (!col.endsWith("'"))
                throw new XMLSyntaxRuleException("Invalid table column mapping");
            return col.substring(1, col.length()-1);
        }
        return rt.getItem(col, pm, index);
    }

    public String printParsedResponse() {
        StringWriter sw=new StringWriter();
        printParsedResponse(new PrintWriter(sw));
        //sw.close();
        return sw.toString();
    }

    public void printParsedResponse(PrintStream ps) {
        PrintWriter pw=new PrintWriter(new OutputStreamWriter(ps));
        printParsedResponse(pw);
        pw.close();
    }

    public void printParsedResponse(PrintWriter pw) {
        Iterator it=parsedResponse.infSets.entrySet().iterator();
        while (it.hasNext()) {
            Entry e=(Entry)it.next();
            pw.print(" ------------------------- ");
            pw.print(e.getKey());
            pw.println(" ------------------------- ");
            pw.println(e.getValue());
        }
        it=parsedResponse.infVectorSets.entrySet().iterator();
        while (it.hasNext()) {
            Entry e=(Entry)it.next();
            pw.print(" ------------------------- ");
            pw.print(e.getKey());
            pw.println(" ------------------------- ");
            //pw.println(e.getValue());
            Vector v=(Vector)e.getValue();
            for (int i=0; i<v.size(); i++) {
                pw.println(v.get(i));
                pw.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
            }
        }
    }
}
