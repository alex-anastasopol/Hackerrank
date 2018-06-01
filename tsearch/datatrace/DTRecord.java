package ro.cst.tsearch.datatrace;

import static ro.cst.tsearch.datatrace.Utils.formatCase;
import static ro.cst.tsearch.datatrace.Utils.formatDate;
import static ro.cst.tsearch.datatrace.Utils.formatInstrument;
import static ro.cst.tsearch.datatrace.Utils.formatLegal;
import static ro.cst.tsearch.datatrace.Utils.formatNamePair;
import static ro.cst.tsearch.datatrace.Utils.formatPair;
import static ro.cst.tsearch.datatrace.Utils.notNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.data.CountyConstants;
import ro.cst.tsearch.data.StateContants;
import ro.cst.tsearch.extractor.xml.Bridge;
import ro.cst.tsearch.extractor.xml.GenericFunctions;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.extractor.xml.ResultMap;
import ro.cst.tsearch.extractor.xml.StringFormats;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.response.CourtDocumentIdentificationSet;
import ro.cst.tsearch.servers.response.CrossRefSet;
import ro.cst.tsearch.servers.response.GranteeSet;
import ro.cst.tsearch.servers.response.GrantorSet;
import ro.cst.tsearch.servers.response.OtherInformationSet;
import ro.cst.tsearch.servers.response.OtherInformationSet.OtherInformationSetKey;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet.PropertyIdentificationSetKey;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.SaleDataSet.SaleDataSetKey;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.servers.types.TSServersFactory;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.base.legal.LegalI;
import com.stewart.ats.base.legal.SubdivisionI;
import com.stewart.ats.base.legal.TownShipI;
import com.stewart.ats.base.property.Property;
import com.stewart.ats.base.property.PropertyI;

public class DTRecord {

	private Map<String,String> instrumentInfo = null;

	private Map<String,String> aliasInfo = null;
	private Map<String,String> caseInfo = null;			
	private Set<List<Map<String,String>>> grantorList = null;
	private Set<List<Map<String,String>>> granteeList = null;	
	private Set<Map<String,String>> propertyList = null;		
	private Set<Map<String,String>> partyRefList = null;	
	private Set<Map<String,String>> refInstList  = null;
	private List<DTRecord> referencedDocs = null;	
	private Set<String> remarks = null;	
	
	public Set<String> getRemarks() {
		return remarks;
	}

	public void setRemarks(Set<String> remarks) {
		this.remarks = remarks;
	}

	public DTRecord (
			Map<String,String>instInfo,
			Map<String,String> aliasInfo,
			Map<String,String> caseInfo,
			Set<List<Map<String,String>>> grantorList,
			Set<List<Map<String,String>>> granteeList,
			Set<Map<String,String>> propertyList,
			Set<Map<String,String>> partyRefList,
			Set<Map<String,String>> refInstList,
			List<DTRecord> referencedDocs,
			Set<String> remarks
	){
		this.instrumentInfo = instInfo;
		this.aliasInfo = aliasInfo;
		this.caseInfo = caseInfo;
		this.grantorList = grantorList;
		this.granteeList = granteeList;
		this.propertyList = propertyList;
		this.partyRefList = partyRefList;
		this.refInstList = refInstList;
		this.referencedDocs = referencedDocs;
		this.remarks = remarks;	
	}
	
	public String getBook(){
		String book = null;
		if(instrumentInfo != null){
			book = instrumentInfo.get("book");
		}
		return (book != null) ? book : "";
	}

	public String getPage(){
		String page = null;
		if(instrumentInfo != null){
			page = instrumentInfo.get("page");
		}
		return (page != null) ? page : "";
	}
	
	public Map<String, String> getInstrumentInfo() {
		return instrumentInfo;
	}

	public void setInstrumentInfo(Map<String, String> instrumentInfo) {
		this.instrumentInfo = instrumentInfo;
	}
	
	public Map<String, String> getAliasInfo() {
		return aliasInfo;
	}

	public void setAliasInfo(Map<String, String> aliasInfo) {
		this.aliasInfo = aliasInfo;
	}
	/**
	 * return book_page
	 * @return
	 */
	public String getBookPage(){
		if(instrumentInfo == null){ return null; }
		String book = instrumentInfo.get("book");
		String page = instrumentInfo.get("page");
		if(book == null || page == null){ return null;}
		
		String serverDoctype = instrumentInfo.get("type");
		
		String id = book + "_" + page;
		if (StringUtils.isNotEmpty(serverDoctype)){
			id += "_" + serverDoctype.replaceAll("\\s+", "");
		}
		return id;
	}
	
	/**
	 * return case number
	 * @return
	 */
	public String getCaseNo(){
		if(caseInfo == null){ return null; }
		return caseInfo.get("number");
	}
	
	/**
	 * return instrument number
	 * @return
	 */
	public String getInstNo(){
		if(instrumentInfo == null){ return null; }
		String year = instrumentInfo.get("recorded.year");
		if(StringUtils.isEmpty(year)){
			year = instrumentInfo.get("year");
		}
		String number = instrumentInfo.get("number");
		if(year == null || number == null){ return null;}
		
		String serverDoctype = instrumentInfo.get("type");
		
		String id = year + "" + number;
		if (StringUtils.isNotEmpty(serverDoctype)){
			id += "_" + serverDoctype.replaceAll("\\s+", "");
		}
		
		return id;
	}
	
	/**
	 * return instrument year
	 * @return
	 */
	public String getInstYear(){
		if(instrumentInfo == null){ return null; }
		String year =  instrumentInfo.get("year");
		if(year==null){
			year = instrumentInfo.get("recorded.year");
		}
		return year;
	}
	
	/**
	 * return serverdoctype
	 * @return
	 */
	public String getServerDocType(){
		if (instrumentInfo == null){ return null; }
		String type = instrumentInfo.get("type");
		return type;
	}
	
	public String getInstMonth(){
		if(instrumentInfo == null){ return null; }
		String year =  instrumentInfo.get("month");
		if(year==null){
			year = instrumentInfo.get("recorded.month");
		}
		return year;
	}
	
	public String getInstDay(){
		if(instrumentInfo == null){ return null; }
		String year =  instrumentInfo.get("day");
		if(year==null){
			year = instrumentInfo.get("recorded.day");
		}
		return year;
	}
	
	
	/**
	 * return book_page, case_no or null
	 * @return
	 */
	public String getId(){
		String id = getBookPage();
		if(id != null){ return id;}
		id = getInstNo();
		if(id != null){ return id;}		
		return getCaseNo();
	}
	
	public Collection<DTRecord> getReferencedDocs(){
		if(referencedDocs == null){
			return Collections.unmodifiableCollection(new LinkedList<DTRecord>());
		}
		return Collections.unmodifiableCollection(referencedDocs);
	}
	
	public Collection<Map<String,String>> getPartyRefList(){
		if(partyRefList == null){
			return Collections.unmodifiableCollection(new LinkedList<Map<String,String>>());
		}
		return Collections.unmodifiableCollection(partyRefList);
	}
	
	public Collection<Map<String,String>> getRefInstList(){
		if (refInstList == null){
			return Collections.unmodifiableCollection(new LinkedList<Map<String,String>>());
		}
		return Collections.unmodifiableCollection(refInstList);
	}
	
	public void addToRemarks(String newElem) {
		Set<String> rmks = getRemarks();
		if (StringUtils.isNotEmpty(newElem)) {
			rmks.add(newElem);
			setRemarks(rmks);
		}
	}
	
	/**
	 * return instrumentInfo number
	 * first try alias number, so that the images will work with real instrument number
	 * @return
	 */
	public String getInstrumentNoFromAlias(){
		if(aliasInfo != null){
			String number = aliasInfo.get("number");
			if(!StringUtils.isBlank(number)){
				return number;
			}
		}
		
		return "unknown";
	}
	
	/**
	 * return instrumentInfo number
	 * first try book_page, so that the images will work
	 * @return
	 */
	public String getInstrumentNo(){
		
		if(instrumentInfo != null){
			String book = instrumentInfo.get("book");
			String page = instrumentInfo.get("page");
			if(!StringUtils.isBlank(book) && !StringUtils.isBlank(page)){
				return book + "_" + page;
			}
			String number = instrumentInfo.get("number");
			if(!StringUtils.isBlank(number)){
				return number;
			}			
		}
		
		if(caseInfo != null){
			String number = caseInfo.get("number");
			if(!StringUtils.isBlank(number)){
				return number;
			}
		}
		
		if(aliasInfo != null){
			String number = aliasInfo.get("number");
			if(!StringUtils.isBlank(number)){
				return number;
			}
		}
		
		return "unknown";
	
	}
	
	public String getRecordedDate() {
		if(instrumentInfo == null) {
			return "";
		}
		return notNull(formatDate(instrumentInfo, "recorded"));
	}
		
	/**
	 * return HTML string representation
	 */
	public String toString(){
		
		StringBuilder retVal = new StringBuilder("<table cellspacing=\"0\">");

		if(grantorList != null && grantorList.size() != 0){
			
			boolean firstLine = true;
			for(List<Map<String,String>> grantor: grantorList){
				if(firstLine){
					retVal.append("<tr><td><b>");
					retVal.append((grantorList.size() == 1) ? "Grantor" : "Grantors");
					retVal.append(":</b></td><td>");
					retVal.append(formatNamePair(grantor));
					retVal.append("</td></tr>");
					firstLine = false;
				} else {
					retVal.append("<tr><td>&nbsp;</td><td>");
					retVal.append(formatNamePair(grantor) );
					retVal.append("</td></tr>");
				}
			}
		}

		if(granteeList != null && granteeList.size() != 0){
			boolean firstLine = true;
			for(List<Map<String,String>> grantee: granteeList){
				if(firstLine){
					retVal.append("<tr><td><b>" );
					retVal.append((granteeList.size() == 1) ? "Grantee" : "Grantees");
					retVal.append(":</b></td><td>" );
					retVal.append(formatNamePair(grantee) );
					retVal.append("</td></tr>");
					firstLine = false;
				} else {
					retVal.append("<tr><td>&nbsp;</td><td>");
					retVal.append(formatNamePair(grantee));
					retVal.append("</td></tr>");
				}
			}
		}
						
		if(propertyList != null && propertyList.size() != 0){
			Boolean firstLine = true;
			for(Map<String,String> property: propertyList){
				if(firstLine) {
					retVal.append("<tr><td valign=\"top\"><b>");
					retVal.append((propertyList.size() == 1) ? "Property" : "Properties");
					retVal.append(":</b></td><td>" );
					retVal.append(formatLegal(property) );
					retVal.append("</td></tr>");
					firstLine = false;
				}else{
					retVal.append("<tr><td>&nbsp;</td><td>" );
					retVal.append(formatLegal(property) );
					retVal.append("</td></tr>");
				}			
			}
		}
		
		if(instrumentInfo != null && instrumentInfo.size() != 0){
			retVal.append("<tr><td valign=\"top\"><b>Instrument: </b></td><td>" );
			retVal.append(formatInstrument(instrumentInfo) );
			retVal.append("</td></tr>");
		}
		
		if(aliasInfo != null && aliasInfo.size() != 0){
			retVal.append("<tr><td valign=\"top\"><b>Alias: </b></td><td>" );
			retVal.append(formatInstrument(aliasInfo) );
			retVal.append("</td></tr>");
		}
		
		if(caseInfo != null && caseInfo.size() != 0){
			retVal.append("<tr><td valign=\"top\"><b>Case: </b></td><td>" );
			retVal.append(formatCase(caseInfo));
			retVal.append("</td></tr>");
		}			 
		 
		if(remarks != null){
			StringBuilder rem = new StringBuilder();
			for(String r : remarks){
				rem.append( r );
				rem.append(" ");
			}
			
			String remStr = rem.toString().trim();
			remStr = remStr.replaceAll("_", "/");
			if(!"".equals(remStr)){
				retVal.append("<tr><td valign=\"top\"><b>Remarks: </b></td><td>");
				retVal.append(remStr);
				retVal.append("</td></tr>");
			}
		}		
		
		// collect all references in one place
		Set<Map<String,String>> refs = new LinkedHashSet<Map<String,String>>();
		if(partyRefList != null){ refs.addAll(partyRefList); }
		if(refInstList != null){ refs.addAll(refInstList); }
		
		// display all references
		if(refs.size() != 0){
			boolean firstLine = true;
			for(Map<String,String> ref : refs){
				if(firstLine){
					retVal.append("<tr><td valign=\"top\"><b>" );
					retVal.append((refs.size() == 1) ? "Ref. Doc" : "Ref. Docs");
					retVal.append(":</b></td><td>" );
					retVal.append(formatInstrument(ref) );
					retVal.append("</td></tr>");
					firstLine = false;
				} else {
					retVal.append("<tr><td>&nbsp;</td><td>");
					retVal.append(formatInstrument(ref));
					retVal.append("</td></tr>");
				}
			}
		}		
		
		retVal.append("</table>" );
		
		return retVal.toString();
	}
	
	/**
	 * fill a parsed response
	 * @param pr
	 * @param dataSite
	 */
	public void setParsedData(ParsedResponse pr,long searchId, DataSite dataSite){
		
		ResultMap resultMap = new ResultMap();
	
		// set other information set - add SrcType info - fix for bug #1849
		setParsedDataOtherInfo(pr, dataSite, resultMap);
		
		// set grantor
		String grantorString = setParsedDataGrantor(pr, dataSite, resultMap);
		
		// set grantee
		String granteeString = setParsedDataGrantee(pr, dataSite, resultMap);

		// set cross references
		setParsedDataCrossRef(pr, dataSite, searchId);
		
		// set sale data
		setParsedDataSale(pr, grantorString, granteeString, "", searchId, dataSite, resultMap);
		
		// set court doc info
		setParsedDataCourt(pr, dataSite);		
		
		String county  = dataSite.getCountyName();
		String state = dataSite.getStateAbbreviation();
	
		Set<String> remarks = getRemarks();
		StringBuffer remarkForJDGAndLP = new StringBuffer();
		
    	for(String remark: remarks){
    		if ("FL".equalsIgnoreCase(state)){
    			
    			Vector<CrossRefSet> crossrefSet = (Vector<CrossRefSet>) pr.infVectorSets.get("CrossRefSet");
    			if(crossrefSet==null){
    				crossrefSet = new Vector<CrossRefSet>();
    				pr.setCrossRefSet(crossrefSet);
    			}
    			String docType = (String)resultMap.get(SaleDataSetKey.DOCUMENT_TYPE.getKeyName());
    			
    			Pattern patDOD = Pattern.compile("(?is)\\bDOD\\s+\\d+\\s*-\\s*\\d+\\s*-\\s*\\d+\\b");
    			Matcher matchDOD = patDOD.matcher(remark);
    			if (matchDOD.find()){
    				String rmrk = matchDOD.group(0);
    				resultMap.put(OtherInformationSetKey.REMARKS.getKeyName(), rmrk);
    				remark = remark.replace(rmrk, "");
    			}
    			boolean isJudgmentType = "Certified Judgment".equalsIgnoreCase(DocumentTypes.getDocumentSubcategory(docType, null, searchId, false))
    										|| DocumentTypes.isJudgementDocType(docType, searchId, false);
    			boolean isJudgOrLPDoctype = (StringUtils.isNotEmpty(docType) 
 												&& (isJudgmentType || DocumentTypes.isLisPendensDocType(docType, searchId, false)));
    			if (isJudgOrLPDoctype){
    				String refset = GenericFunctions.extractFromComment(remark, "(?i)\\b([A-Z]{0,2}(?:\\s*[A-Z]{0,2})?(?:[\\d/]+)?[\\d\\s/-]{3,}(?:\\s*[A-Z]{1,5}\\s*(?:[\\d\\s/-]+)?(?:[A-Z]\\d+)?)?|\\d+[\\dA-Z]+(?:\\s+[A-Z]+)?)\\b", 1);
    				
    				remark = remark.replace(refset, "");
    				refset = refset.replaceAll("(?is)FCL\\s*$", "");
    				refset = refset.replaceAll("(?is)P\\d+\\s*$", "");
    				remarkForJDGAndLP.append(refset).append(",");
    			}
    			
    			Pattern patBookPage = Pattern.compile("\\b([0-9]{3,})/([0-9]+)\\b");
        		Matcher mat = patBookPage.matcher(remark);
        		
        		if(mat.find()){
        			String book = mat.group(1);
        			String page = mat.group(2);
        			CrossRefSet set = new CrossRefSet();
        			
        			set.setAtribute("Book", StringUtils.stripStart(book, "0"));
	         		set.setAtribute("Page", StringUtils.stripStart(page, "0"));
        			
        			if (addIfNotExists(crossrefSet, set)){
        				crossrefSet.add(set);
        			}
        		} else{
        			if (CountyConstants.FL_Gulf == dataSite.getCountyId()){
        				patBookPage = Pattern.compile("\\b([0-9]+)\\s*-\\s*([0-9]+[A-Z])\\b");
                		mat = patBookPage.matcher(remark);
                		if (mat.find()){
                			String parcelId = mat.group(1) + mat.group(2);
                			PropertyIdentificationSet pis = new PropertyIdentificationSet();
                			
                			pis.setAtribute(PropertyIdentificationSetKey.PARCEL_ID.getShortKeyName(), parcelId);
                			
                			Vector pisVector = (Vector) pr.infVectorSets.get("PropertyIdentificationSet");
                	        if (pisVector == null) {
                	        	pisVector = new Vector();
                	        	pr.infVectorSets.put("PropertyIdentificationSet", pisVector);
                	        }
                	        
                	        if (!pisVector.contains(pis)){
                        		pisVector.add(pis);
                        	}
                	        
                		}
        			}
        			remark = remark.replaceAll("\\b([0-9]+)\\s*-\\s*([0-9]+)\\s*-\\s*([0-9]+)\\b", "");//FLPolk: BP: 8868_271 has 362723-016025-00021 which is GEO
        			patBookPage = Pattern.compile("\\b([0-9]{3,})\\s*-\\s*([0-9]+)\\b");
            		mat = patBookPage.matcher(remark);
            		while (mat.find()){
            			String book = mat.group(1);
            			String page = mat.group(2);
            			CrossRefSet set = new CrossRefSet();
            			
            			set.setAtribute("Book", StringUtils.stripStart(book, "0"));
	         			set.setAtribute("Page", StringUtils.stripStart(page, "0"));
	         			
	         			if (addIfNotExists(crossrefSet, set)){
	        				crossrefSet.add(set);
	        			}
            		}
        		}
 			} else{
	    		Pattern patBookPage = Pattern.compile("([0-9]+)/([0-9]+)");
	    		Matcher mat = patBookPage.matcher(remark);
	    		if(mat.find()){
	    			String book = mat.group(1);
	    			String page = mat.group(2);
	    			Vector<CrossRefSet> crossrefSet = (Vector<CrossRefSet>) pr.infVectorSets.get("CrossRefSet");
	    			if(crossrefSet==null){
	    				crossrefSet = new Vector<CrossRefSet>();
	    				pr.setCrossRefSet(crossrefSet);
	    			}
	    			
	    			CrossRefSet set = new CrossRefSet();
	    			if("NV".equalsIgnoreCase(state)&&"Clark".equalsIgnoreCase(county)){
	        			set.setAtribute("InstrumentNumber", page);
	    			} else{
	    				set.setAtribute("Book", StringUtils.stripStart(book, "0"));
	        			set.setAtribute("Page", StringUtils.stripStart(page, "0"));
	    			}
	    			if (addIfNotExists(crossrefSet, set)){
        				crossrefSet.add(set);
        			}
	    			
	    		} else {
	    			if ("MI".equalsIgnoreCase(state) && "Wayne".equalsIgnoreCase(county)){
	    				mat.reset();
	    				patBookPage = Pattern.compile("(?is)\\bMG\\s+(?:[A-Z])?([0-9]+)\\s+([0-9]+)");
	    		    	mat = patBookPage.matcher(remark);
	    		    	if (mat.find()){
	    		    		Vector<CrossRefSet> crossrefSet = (Vector<CrossRefSet>) pr.infVectorSets.get("CrossRefSet");
	    	    			if (crossrefSet==null){
	    	    				crossrefSet = new Vector<CrossRefSet>();
	    	    				pr.setCrossRefSet(crossrefSet);
	    	    			}
	    		    		CrossRefSet set = new CrossRefSet();
	    		    		set.setAtribute("Book", StringUtils.stripStart(mat.group(1), "0"));
		        			set.setAtribute("Page", StringUtils.stripStart(mat.group(2), "0"));
		        			if (addIfNotExists(crossrefSet, set)){
		        				crossrefSet.add(set);
		        			}
	    		    	}
	    			
	    			} else if (CountyConstants.CA_Los_Angeles == dataSite.getCountyId()) {
	    				mat.reset();
	    				patBookPage = Pattern.compile("(?is)(?:\\b[A-Z]{1,2})?\\s*(\\d+)[/_\\s](\\d{1,3})\\b");
	    		    	mat = patBookPage.matcher(remark);
	    		    	if (mat.find()) {
	    		    		Vector<CrossRefSet> crossrefSet = (Vector<CrossRefSet>) pr.infVectorSets.get("CrossRefSet");
	    	    			if (crossrefSet==null){
	    	    				crossrefSet = new Vector<CrossRefSet>();
	    	    				pr.setCrossRefSet(crossrefSet);
	    	    			}
	    		    		CrossRefSet set = new CrossRefSet();
	    		    		set.setAtribute("Book", StringUtils.stripStart(mat.group(1), "0"));
		        			set.setAtribute("Page", StringUtils.stripStart(mat.group(2), "0"));
		        			if (addIfBkPgNotExists(crossrefSet, set)){
		        				crossrefSet.add(set);
		        			}
	    		    	
	    		    	} else {
	    		    		mat.reset();
		    		    	mat = Pattern.compile("(?is)(?:\\b[A-Z]{1,2}\\s*)?(\\d{2})(\\d{6,8})\\b").matcher(remark);
		    		    	if (mat.find()) {
		    		    		Vector<CrossRefSet> crossrefSet = (Vector<CrossRefSet>) pr.infVectorSets.get("CrossRefSet");
		    	    			if (crossrefSet==null){
		    	    				crossrefSet = new Vector<CrossRefSet>();
		    	    				pr.setCrossRefSet(crossrefSet);
		    	    			}
		    		    		CrossRefSet set = new CrossRefSet();
		    		    		String year = mat.group(1);
		    		    		String instr = mat.group(2).trim();
		    		    		if (instr.length() == 8) {
		    		    			year = mat.group(0).replaceFirst("(?is)(\\d{4})\\d{4}(\\d{2})", "$2");
		    		    			instr = mat.group(0).replaceFirst("(?is)(\\d{4})\\d{4}(\\d{2})", "$1");
		    		    		}
		    		    		if(year.length() == 2) {
		    						if(Integer.parseInt(year) > 20) {
		    							year = "19" + year;
		    						} else {
		    							year = "20" + year;
		    						}
		    					}
		    		    		set.setAtribute("InstrumentNumber", year + "-" + StringUtils.stripStart(instr, "0"));
			        			set.setAtribute("Year", year);
			        			if (addInstrIfNotExists(crossrefSet, set)){
			        				crossrefSet.add(set);
			        			}
	    		    	
		    		    	} else {
		    		    		mat.reset();
			    		    	mat = Pattern.compile("(?is)(?:\\b[A-Z]{1,2}\\s*)?(\\d{5,7})\\b").matcher(remark);
			    		    	if (mat.find()) {
			    		    		Vector<CrossRefSet> crossrefSet = (Vector<CrossRefSet>) pr.infVectorSets.get("CrossRefSet");
			    	    			if (crossrefSet==null){
			    	    				crossrefSet = new Vector<CrossRefSet>();
			    	    				pr.setCrossRefSet(crossrefSet);
			    	    			}
			    		    		CrossRefSet set = new CrossRefSet();
			    		    		set.setAtribute("InstrumentNumber", StringUtils.stripStart(mat.group(1), "0"));
			    		    		if (addInstrIfNotExists(crossrefSet, set)){
				        				crossrefSet.add(set);
				        			}
			    		    	} 
		    		    	}
	    		    	}
	    			}
	    		}
 			}
    	}
    	
    	if (remarkForJDGAndLP.length() > 0){
    		String rmrk = remarkForJDGAndLP.toString();
    		rmrk = rmrk.replaceFirst("(?is)\\s*,\\s*$", "");
    		resultMap.put(OtherInformationSetKey.REMARKS.getKeyName(), rmrk);
    	}
		
    	try {
			GenericFunctions1.setGranteeLanderTrustee2(resultMap, searchId, true);
		} catch (Exception e){}
    	
    	Bridge bridge = new Bridge(pr, resultMap, searchId);
    	DocumentI doc;
		try {
			doc = bridge.importData();
			//DocumentI doc = Bridge.fillDocument(null, pr, searchId);;

			if (propertyList != null && propertyList.size() > ServerConfig.getMaxNumberOfPropertiesAllowed()){
				doc.setDescription(TSServer.TOO_MANY_PROP);
			} else{
		        //	set property identification sets	
				setParsedDataPis(doc, dataSite);
				pr.setUseDocumentForSearchLogRow(true);
			}
			long siteId = TSServersFactory.getSiteId(state, county, doc.getDataSource());
			doc.setSiteId((int)siteId);
			if (state.equals(StateContants.STATE_ABBREV.NV)){
				setParsedDataExpandedArb(doc);
			}
			pr.setDocument(doc);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean addIfNotExists(Vector<CrossRefSet> crossrefSet, CrossRefSet set) {
		
		if (set.isEmpty()){
			return false;
		}
		boolean alreadyContained = false;
		for (CrossRefSet crossRef : crossrefSet){
			if (crossRef.getAtribute("Book").equals(set.getAtribute("Book"))
					&& crossRef.getAtribute("Page").equals(set.getAtribute("Page"))){
				alreadyContained = true;
				break;
			} else if (crossRef.getAtribute("InstrumentNumber").equals(set.getAtribute("InstrumentNumber"))){
				alreadyContained = true;
				break;
			}
		}
		
		if (alreadyContained){
			return false;
		}
		
		return true;
	}	
	
	 boolean addInstrIfNotExists(Vector<CrossRefSet> crossrefSet, CrossRefSet set) {
		if (set.isEmpty()){
			return false;
		}
		boolean alreadyContained = false;
		for (CrossRefSet crossRef : crossrefSet){
			if (crossRef.getAtribute("InstrumentNumber").equals(set.getAtribute("InstrumentNumber"))){
				alreadyContained = true;
				break;
			}
		}
		
		if (alreadyContained){
			return false;
		}
		
		return true;
	}	
	 
	public boolean addIfBkPgNotExists(Vector<CrossRefSet> crossrefSet, CrossRefSet set) {
		if (set.isEmpty()) {
				return false;
		}
		boolean alreadyContained = false;
		
		for (CrossRefSet crossRef : crossrefSet) {
			if (crossRef.getAtribute("Book").equals(set.getAtribute("Book")) && crossRef.getAtribute("Page").equals(set.getAtribute("Page"))) {
				alreadyContained = true;
				break;
			} 
		}	
		if (alreadyContained){
			return false;
		}
		
		return true;
	}	
	
	@SuppressWarnings("unchecked")
	private void setParsedDataCourt(ParsedResponse pr, DataSite dataSite){
		
		// if we do not have case info then return
		if (caseInfo == null) {
			 return;
		}
		
		// create vector if necessary
		Vector ctVector = (Vector) pr.infVectorSets.get("CourtDocumentIdentificationSet");
        if (ctVector == null) {
        	ctVector = new Vector();
        	pr.infVectorSets.put("CourtDocumentIdentificationSet", ctVector);
        }        
        ctVector.clear();	
        
        // create court infset
        CourtDocumentIdentificationSet ct = new CourtDocumentIdentificationSet();
        
        // fill court infset
        String number = notNull(caseInfo.get("number"));
		String type = notNull(caseInfo.get("type"));
		String filed = notNull(formatDate(caseInfo, "filed"));
		
		ct.setAtribute("CaseNumber", number);
    	ct.setAtribute("FillingDate", filed);
    	ct.setAtribute("CaseType", type);
    	
    	// add court infset to vector
    	ctVector.add(ct);
	}
	
	@SuppressWarnings("unchecked")
	private void setParsedDataCrossRef(ParsedResponse pr, DataSite dataSite, long searchId){
        
		// create vector if necessary
		Vector crVector = (Vector) pr.infVectorSets.get("CrossRefSet");
        if (crVector == null) {
        	crVector = new Vector();
        	pr.infVectorSets.put("CrossRefSet", crVector);
        }        
        crVector.clear();  
        
        // add party refences
        if(partyRefList != null){        
	        for(Map<String,String> inst: partyRefList) {
	        	
	        	CrossRefSet cr = new CrossRefSet();
	        	
				String serverDocType = notNull(inst.get("type"));
				String book = notNull(inst.get("book"));
				if (StringUtils.isEmpty(book)){
					book = notNull(inst.get("inst.book")); 
				}
				String page = notNull(inst.get("page"));
				if (StringUtils.isEmpty(page)){
					page = notNull(inst.get("inst.page")); 
				}
				String number = notNull(inst.get("number")); 
				if (StringUtils.isEmpty(number)){
					number = notNull(inst.get("inst.number")); 
				}
				String year = notNull(inst.get("year"));
				if (StringUtils.isEmpty(year)){
					year = notNull(inst.get("inst.year")); 
				}
				String month =  notNull(inst.get("month"));
				String day = notNull(inst.get("day"));
				
				if (StateContants.MO_STRING_FIPS.equals(dataSite.getStateFIPS())){
					book = book.replaceAll("(?is)[DM]+", "");
					int numberLength = number.length();
					if (numberLength > 4){
						number = number.substring(numberLength - 4);
						number = StringUtils.stripStart(number, "0");
					}
				} else if (CountyConstants.OH_Franklin == dataSite.getCountyId()) {
					String recordedDate =  month + "/" + day + "/" + year;
					if (recordedDate.matches("(?is)\\d{1,2}/\\d{1,2}/\\d{4}")){
						number = formatInstNoForOHFranklin(number, recordedDate, "MM/dd/yyyy");
					}
					if (page.matches("(?is)[A-Z]\\d")){
						page = page.replaceFirst("(?is)\\A([A-Z])(\\d)$", "$10$2");
					}
					
				} else if (CountyConstants.CA_Los_Angeles == dataSite.getCountyId()) {
					number = number.replaceFirst("(?is)\\A[A-Z]{1,2}\\s*(\\d+)", "$1");
					
				}  else if (CountyConstants.CO_Larimer == dataSite.getCountyId()){
					number = processInstrumentNumber(number, year, dataSite);
					
				}
				if (number.matches("\\d+\\s*/\\s*\\d+")){
					if (StringUtils.isEmpty(book) && StringUtils.isEmpty(page)){
						String[] bp = number.split("\\s*/\\s*");
						if (bp.length == 2){
							book = bp[0];
							page = bp[1];
						}
					}
					number = "";
				}
				if (dataSite.getStateFIPS().equals(StateContants.CA_STRING_FIPS)){
					if (StringUtils.isNotEmpty(year) && StringUtils.isNotEmpty(number)){
						number = year + "-" + number;
					}
				}
				cr.setAtribute("DocumentType", serverDocType);
		    	cr.setAtribute("Book", book );
		    	cr.setAtribute("Page", page);

		    	if(!"".equals(book) && !"".equals(page)){
		    		cr.setAtribute("Book_Page", book + "_" + page);
		    	} 
		    	
		    	cr.setAtribute("InstrumentNumber", number);
		    	cr.setAtribute("Year", year);
		    	cr.setAtribute("Month", month);
		    	cr.setAtribute("Day", day);
		    	crVector.add(cr);
	        }
        }
        
        // add document references
        if(referencedDocs != null && referencedDocs.size() != 0){
        	for(DTRecord refDoc : referencedDocs){
        		
        		CrossRefSet cr = new CrossRefSet();
        		
		        // fill sale data set
				String book = "";
				String page = "";
				String number = "";
				String caseNumber = "";
				String year = "";
				String month = "";
				String day = "";
				String recordedDate = "";
				String type = "";
				
				if(refDoc.aliasInfo != null){
					number = notNull(refDoc.aliasInfo.get("number"));
					year = notNull(refDoc.aliasInfo.get("year"));
				}
				
				if(refDoc.instrumentInfo != null){

					book = notNull(refDoc.instrumentInfo.get("book"));
					page = notNull(refDoc.instrumentInfo.get("page"));
					
					String number1 = refDoc.instrumentInfo.get("number");
					String year1 = refDoc.instrumentInfo.get("recorded.year");
					if(ro.cst.tsearch.utils.StringUtils.isEmpty(year1)) {
						year1 = refDoc.instrumentInfo.get("year");
					}
					
					if(number1 != null && year1!= null){
						number = number1;
						year = year1;
					}
					if (StringUtils.isEmpty(year)){
						year = refDoc.instrumentInfo.get("recorded.year");
					}
					month =  notNull(refDoc.instrumentInfo.get("month"));
					if (StringUtils.isEmpty(month)){
						month = notNull(refDoc.instrumentInfo.get("recorded.month"));
					}
					day =  notNull(refDoc.instrumentInfo.get("day"));
					if (StringUtils.isEmpty(day)){
						day = notNull(refDoc.instrumentInfo.get("recorded.day"));
					}
					//month=12, recorded.month=12, number=2639, year=2002, day=11
					recordedDate = notNull(formatDate(refDoc.instrumentInfo, "recorded"));
					
					type = notNull(refDoc.instrumentInfo.get("type"));
					
				} else if(refDoc.caseInfo != null){

					caseNumber = notNull(refDoc.caseInfo.get("number"));
				}
				
				if (StateContants.MO_STRING_FIPS.equals(dataSite.getStateFIPS())){
					book = book.replaceAll("(?is)[DM]+", "");
					int numberLength = number.length();
					if (numberLength > 4){
						number = number.substring(numberLength - 4);
						number = StringUtils.stripStart(number, "0");
					}
				} else if (CountyConstants.OH_Franklin == dataSite.getCountyId()) {
					number = formatInstNoForOHFranklin(number, recordedDate, "MM/dd/yyyy");
					
					if (page.matches("(?is)[A-Z]\\d")){
						page = page.replaceFirst("(?is)\\A([A-Z])(\\d)$", "$10$2");
					}
					
				} else if (CountyConstants.CA_Los_Angeles == dataSite.getCountyId()) {
					number = number.replaceFirst("(?is)\\A[A-Z]{1,2}\\s*(\\d+)", "$1");
					
				} else if (CountyConstants.CO_Larimer == dataSite.getCountyId()){
					number = processInstrumentNumber(number, year, dataSite);
					
				}
				if (StringUtils.isNotEmpty(caseNumber)){
					number = caseNumber;
				}
				if (number.matches("\\d+\\s*/\\s*\\d+")){
					if (StringUtils.isEmpty(book) && StringUtils.isEmpty(page)){
						String[] bp = number.split("\\s*/\\s*");
						if (bp.length == 2){
							book = bp[0];
							page = bp[1];
						}
					}
					number = "";
				}
				if (dataSite.getStateFIPS().equals(StateContants.CA_STRING_FIPS)){
					if (StringUtils.isNotEmpty(year) && StringUtils.isNotEmpty(number)){
						number = year + "-" + number;
					}
				}
				cr.setAtribute("InstrumentNumber",  number );
		    	cr.setAtribute("Book", book );
		    	cr.setAtribute("Page", page);
		    	cr.setAtribute("Year", year);
		    	cr.setAtribute("Month", month);
		    	cr.setAtribute("Day", day);
		    	if (StringUtils.isNotEmpty(type)){
		    		type = type.replaceAll("(?is)\\s+", "");
		    		cr.setAtribute("DocumentType", type);
		    	}
		    	if(!"".equals(book) && !"".equals(page)){
		    		String bp = book + "-" + page;
		    		cr.setAtribute("Book_Page", bp);
		    	}
		    	
			    crVector = retainCompleteReferences(crVector, cr, book, page, number, year, type);
        	}
        } else if(refInstList != null && refInstList.size() != 0){
        	for(Map<String,String> refInst: refInstList){
        		CrossRefSet cr = new CrossRefSet();
        		
		        // fill sale data set
				String book = "";
				String page = "";
				String number = "";
				String year = "";
				String month = "";
				String day = "";
				String recordedDate = "";
				String type = "";

				book = notNull(refInst.get("book"));
				page = notNull(refInst.get("page"));
					
				String number1 = refInst.get("number");
				String year1 = refInst.get("recorded.year");
				if	(ro.cst.tsearch.utils.StringUtils.isEmpty(year1)) {
					year1 = refInst.get("year");
				}
					
				if (number1 != null && year1!= null){
					number = number1;
					year = year1;
				}
					
				month =  notNull(refInst.get("month"));
				if (StringUtils.isEmpty(month)){
					month = notNull(refInst.get("recorded.month"));
				}
				day =  notNull(refInst.get("day"));
				if (StringUtils.isEmpty(day)){
					day = notNull(refInst.get("recorded.day"));
				}
				//month=12, recorded.month=12, number=2639, year=2002, day=11
				recordedDate = notNull(formatDate(refInst, "recorded"));
				type = notNull(refInst.get("type"));
				
				if (StateContants.MO_STRING_FIPS.equals(dataSite.getStateFIPS())){
					book = book.replaceAll("(?is)[DM]+", "");
					int numberLength = number.length();
					if (numberLength > 4){
						number = number.substring(numberLength - 4);
						number = StringUtils.stripStart(number, "0");
					}
				} else if (CountyConstants.OH_Franklin == dataSite.getCountyId()) {
					number = formatInstNoForOHFranklin(number, recordedDate, "MM/dd/yyyy");
					
					if (page.matches("(?is)[A-Z]\\d")){
						page = page.replaceFirst("(?is)\\A([A-Z])(\\d)$", "$10$2");
					}
					
				} else if (CountyConstants.CA_Los_Angeles == dataSite.getCountyId()) {
					number = number.replaceFirst("(?is)\\A[A-Z]{1,2}\\s*(\\d+)", "$1");
					
				} else if (CountyConstants.CO_Larimer == dataSite.getCountyId()){
					
					number = processInstrumentNumber(number, year, dataSite);
				}

				if (number.matches("\\d+\\s*/\\s*\\d+")){
					if (StringUtils.isEmpty(book) && StringUtils.isEmpty(page)){
						String[] bp = number.split("\\s*/\\s*");
						if (bp.length == 2){
							book = bp[0];
							page = bp[1];
						}
					}
					number = "";
				}
				if (dataSite.getStateFIPS().equals(StateContants.CA_STRING_FIPS)){
					if (StringUtils.isNotEmpty(year) && StringUtils.isNotEmpty(number)){
						number = year + "-" + number;
					}
				}
				cr.setAtribute("InstrumentNumber",  number );
		    	cr.setAtribute("Book", book );
		    	cr.setAtribute("Page", page);
		    	cr.setAtribute("Year", year);
		    	cr.setAtribute("Month", month);
		    	cr.setAtribute("Day", day);
		    	if (StringUtils.isNotEmpty(type)){
		    		type = type.replaceAll("\\s+", "");
		    		cr.setAtribute("DocumentType", type);
		    	}
		    	if (!"".equals(book) && !"".equals(page)){
		    		String bp = book + "-" + page;
		    		cr.setAtribute("Book_Page", bp);
		    	}	
		    	
		    	crVector = retainCompleteReferences(crVector, cr, book, page, number, year, type);
        	}
        }
	}

	/**
	 * @param number
	 * @param year
	 * @param dataSite
	 * @return
	 */
	public String processInstrumentNumber(String number, String year, DataSite dataSite) {
		if (CountyConstants.CO_Larimer == dataSite.getCountyId()){
			if (StringUtils.isNotEmpty(year)) {
				int yearInt = -1;
				yearInt = Integer.parseInt(year);
				if (yearInt > 0){
					if (yearInt >= 2000 && number.startsWith(year)){
						return number;
					} else if (yearInt >= 1981 && year.length() ==4 && number.startsWith(year.substring(2))){
						return number;
					} else{
						if (yearInt >= 2003) {
							number = year + StringUtils.leftPad(number, 7, "0");
						} else if (yearInt >= 2000) {
							number = year + StringUtils.leftPad(number, 6, "0");
						} else if (yearInt >= 1981) {
							number = (year + StringUtils.leftPad(number, 6, "0")).substring(2);
						}
					}
				}
			}
		}
		return number;
	}

	/**
	 * @param crVector
	 * @param cr
	 * @param book
	 * @param page
	 * @param number
	 * @param year
	 * @param type
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Vector retainCompleteReferences(Vector crVector, CrossRefSet cr, String book, String page, String number, String year, String type) {
		Vector tempCrVector = new Vector();
		for (Object object : crVector) {
			if (object instanceof CrossRefSet){
				CrossRefSet cross = (CrossRefSet) object;
				String b = cross.getAtribute("Book");
				String p = cross.getAtribute("Page");
				String i = cross.getAtribute("InstrumentNumber");
				String y = cross.getAtribute("Year");
				String dt = cross.getAtribute("DocumentType");
				if (StringUtils.isNotEmpty(b) && StringUtils.isNotEmpty(p) && StringUtils.isNotEmpty(book) && StringUtils.isNotEmpty(page)){
					if (b.equals(book) && p.equals(page)){
						if (StringUtils.isEmpty(dt) && StringUtils.isNotEmpty(type)){
							continue;
						} else if (StringUtils.isNotEmpty(dt) && StringUtils.isEmpty(type)){
							tempCrVector.add(cr);
							continue;
						}
					}
				}
				if (StringUtils.isNotEmpty(i) && StringUtils.isNotEmpty(y) && StringUtils.isNotEmpty(number) && StringUtils.isNotEmpty(year)){
					if (i.equals(number) && y.equals(year)){
						if (StringUtils.isEmpty(dt) && StringUtils.isNotEmpty(type)){
							continue;
						} else if (StringUtils.isNotEmpty(dt) && StringUtils.isEmpty(type)){
							tempCrVector.add(cr);
							continue;
						}
					}
				}
				tempCrVector.add(cross);
			}
		}
		tempCrVector.add(cr);
		crVector.clear();
		crVector.addAll(tempCrVector);
		
		return crVector;
	}
	
	 /*
     * set property identification sets
     */   	
	@SuppressWarnings("unchecked")
	private void setParsedDataPis(DocumentI doc, DataSite dataSite){
     
        if (propertyList == null){
        	return;
        }
        if (doc == null){
        	return;
        }
        Set<PropertyI> properties = doc.getProperties();
        
        for(Map<String,String> property: propertyList){ 
        	PropertyI prop = Property.createEmptyProperty();
        	SubdivisionI subdivision = prop.getLegal().getSubdivision();
        	TownShipI township = prop.getLegal().getTownShip();
        	
        	//prior_arb=8.95 AC, arb.district=34, arb.parcel=278, arb.split_code=41
        	
        	String priorArb = notNull(property.get("prior_arb"));
        	
        	//OHFairfield
        	String district = notNull(property.get("arb.district"));
        	String parcel = notNull(property.get("arb.parcel"));
        	String splitCode = notNull(property.get("arb.split_code"));
        	
			String pbk 		= notNull(property.get("plat.book_page.book"));
			String ppg 		= notNull(property.get("plat.book_page.page"));
			
			if (pbk.matches("(?is)[A-Z]") && ppg.matches("(?is)[A-Z]")){
				pbk = "";
				ppg = "";
			}
			
			String pn 		= notNull(property.get("plat.number"));
			String pInstYear 		= notNull(property.get("plat.year"));
			
			if(StringUtils.isBlank(pn)){
				pn = notNull(property.get("plat.plat_inst.number"));
				pInstYear = notNull(property.get("plat.plat_inst.year"));
			}
			
			//For CA
			String tract = notNull(property.get("plat.tract.number"));
			if (StringUtils.isNotEmpty(tract)){
				subdivision.setTract(tract);
			}
			
			String lot 		= notNull(property.get("lot"));
			String block 	= notNull(property.get("block"));
			String section 	= notNull(property.get("section"));
			//String qtr 	    = notNull(property.get("quarter.half_quarter"));
			String twn      = notNull(formatPair(property.get("township.number"), property.get("township.dir"), ""));
			String rng      = notNull(formatPair(property.get("range.number"), property.get("range.dir"), ""));

			subdivision.setPlatBook(pbk ); // remove "P" from start?
			subdivision.setPlatPage(ppg);
			subdivision.setPlatInstrument(pn);
			subdivision.setPlatInstrumentYear(pInstYear);
			subdivision.setLot(lot);
			subdivision.setBlock(block);
			
			township.setSection(section);
			township.setTownship(twn);
			township.setRange(rng);
        	        	
        	//for MO St Louis CityDTG
        	String cityBlock 		= notNull(property.get("plat.city_block_name.city_block"));
			String cityBlockName	= notNull(property.get("plat.city_block_name.name"));
			if (StringUtils.isNotEmpty(cityBlock) && StringUtils.isNotEmpty(cityBlockName)){
				subdivision.setNcbNumber(cityBlock);
				subdivision.setName(cityBlockName);
			}

        	//for Nevada DTG
        	String arbBook = notNull(property.get("arb.book"));
        	String arbPage = notNull(property.get("arb.page"));
        	String arbLot = notNull(property.get("arb.lot"));
        	
        	if(StringUtils.isNotBlank(district)&&StringUtils.isNotBlank(parcel)&&StringUtils.isNotBlank(splitCode)){
        		township.setArb(district + "-" + parcel + "-" + splitCode);  
        	} else if (StringUtils.isNotBlank(arbBook) && StringUtils.isNotBlank(arbPage) && StringUtils.isNotBlank(arbLot)){
        		township.setArb(arbBook + "-" + arbPage + "-" + arbLot);  
        	}
        	String arbBlock = notNull(property.get("arb.block"));
        	String arbParcel = notNull(property.get("arb.parcel"));
        	if (dataSite.getStateFIPS().equals(StateContants.CA_STRING_FIPS)){
        		township.setArbBook(arbBook);
        		township.setArbPage(arbPage);
        		township.setArbLot(arbLot);
        		township.setArbBlock(arbBlock);
        		township.setParcel(arbParcel);
        	}
        	
        	properties.add(prop);
        }
        
	}
	
	private void setParsedDataExpandedArb(DocumentI doc){
        
        if(propertyList == null){
        	return;
        }
        
        for(Map<String,String> property: propertyList){
        	
        	if (StringUtils.isNotEmpty(property.get("arb.arb_number"))){
        		String arb	= notNull(property.get("arb.arb_number"));
				String twp= notNull(formatPair(property.get("arb.township.number"), property.get("arb.township.dir"), ""));
				String range = notNull(formatPair(property.get("arb.range.number"), property.get("arb.range.dir"), ""));
				String section = notNull(property.get("arb.section"));
				String qv = notNull(property.get("arb.quarter.half_quarter"));
				
				Set<PropertyI> properties = doc.getProperties();
				for(PropertyI prop:properties){
					LegalI legal = prop.getLegal();
					if(legal!=null){
						TownShipI township = legal.getTownShip();
						if(township!=null){
							township.setArb(arb);
							township.setTownship(twp);
							township.setRange(range);
							township.setSection(section);
							township.setQuarterValue(qv);
						}
					}
				}
        	} else{
				String lot	= notNull(property.get("arb.lot"));
				String block= notNull(property.get("arb.block"));
				String book = notNull(property.get("arb.book"));
				String page = notNull(property.get("arb.page"));
				
				Set<PropertyI> properties = doc.getProperties();
				for(PropertyI prop:properties){
					LegalI legal = prop.getLegal();
					if(legal!=null){
						TownShipI township = legal.getTownShip();
						if(township!=null){
							township.setArbBlock(block);
							township.setArbLot(lot);
							township.setArbPage(page);
							township.setArbBook(book);
						}
					}
				}
        	}
        }
	}
	
    /*
     * set GrantorSet sets
     */ 	
	@SuppressWarnings("unchecked")
	private String setParsedDataGrantor(ParsedResponse pr, DataSite dataSite, ResultMap resultMap){		
       
        Vector grantorVector = (Vector) pr.infVectorSets.get("GrantorSet");
        if (grantorVector == null) {
        	grantorVector = new Vector();
        	pr.infVectorSets.put("GrantorSet", grantorVector);
        }
        grantorVector.clear();      
        
        if(grantorList == null){
        	return "";
        }
        
        String grantorString = "";
        ArrayList<List> gtorList = new ArrayList<List>();
        
        for (List<Map<String,String>> grantor: grantorList){
        	GrantorSet gr = new GrantorSet();
        	// process husband
        	{   
        		Map<String,String> husband = grantor.get(0);
        	
	        	String last = husband.get("name");
	        	String first = husband.get("first");
	        	String middle = husband.get("middle");
	        	if(middle == null){
	        		middle = husband.get("mi");
	        	}
	        	String suffix = husband.get("suffix");
	        	if (suffix != null){
	        		if (middle != null){
	        			middle = middle + " " + suffix;
	        		} else if (first != null){
	        			first = first + " " + suffix;
	        		} else {
	        			first = suffix;
	        		}
	        	}
	        	first 	= notNull(first);
	        	middle 	= notNull(middle);
	        	last 	= notNull(last);

	        	grantorString = addName(grantorString, last, first, middle);
        	}
        	// process wife
        	if (grantor.size() == 2){
        		Map<String,String> wife = grantor.get(1);
	        	String last = wife.get("name");
	        	String first = wife.get("first");
	        	String middle = wife.get("middle");
	        	if (middle == null){
	        		middle = wife.get("mi");
	        	}
	        	String suffix = wife.get("suffix");
	        	if (suffix != null){
	        		if (middle != null){
	        			middle = middle + " " + suffix;
	        		} else if (first != null){
	        			first = first + " " + suffix;
	        		} else {
	        			first = suffix;
	        		}
	        	}
	        	first 	= notNull(first);
	        	middle 	= notNull(middle);
	        	last 	= notNull(last);	        	
	        	
	        	grantorString = addName(grantorString, last, first, middle);;
        	}
        }
        Set<String> remarks = getRemarks();
        if (StringUtils.isEmpty(grantorString) && remarks != null){

	        Pattern namePat = Pattern.compile("(?is)\\b([A-Z]+\\s+[A-Z](?:\\s+[A-Z])?)\\s+([A-Z]+\\s+[A-Z](?:\\s+[A-Z])?)\\b");
	        for (String remark : remarks){
	        	 Matcher mat = namePat.matcher(remark);
	        	 if (mat.find()){
	        		 grantorString += "/" + mat.group(1);
	        	 } else {
	        		 namePat = Pattern.compile("(?is)([A-Z]+\\s+[A-Z]+(?:\\s+[A-Z]+)?(?:\\s+[A-Z])?\\s*&\\s*[A-Z]\\s+[A-Z]+)");
	        		 mat = namePat.matcher(remark);
	        		 if (mat.find()){
		        		 grantorString += "/" + mat.group(1);
		        	 }
	        	 }
			}
        }
        
        try {
        	grantorString = grantorString.replaceAll("(?is)\\s*/\\s*(TRUSTEE)\\b", " $1");
        	gtorList = parseNames(gtorList, grantorString);
			
			resultMap.put(GrantorSet.class.getSimpleName(), GenericFunctions.storeOwnerInSet(gtorList, true));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return grantorString;
	}
	
    /*
     * set GranteeSet sets
     */   	
	@SuppressWarnings("unchecked")
	private String setParsedDataGrantee(ParsedResponse pr, DataSite dataSite, ResultMap resultMap){
     
        Vector granteeVector = (Vector) pr.infVectorSets.get("GranteeSet");
        if (granteeVector == null) {
        	granteeVector = new Vector();
        	pr.infVectorSets.put("GranteeSet", granteeVector);
        }
        granteeVector.clear();   
        
        if (granteeList == null){
        	return "";
        }
        
        String granteeString = "";
        ArrayList<List> gteeList = new ArrayList<List>();
        
        for (List<Map<String,String>> grantee: granteeList){
        	GranteeSet gr = new GranteeSet();
        	// process husband
        	{   
        		Map<String,String> husband = grantee.get(0);
        	
	        	String last = husband.get("name");
	        	String first = husband.get("first");
	        	String middle = husband.get("middle");
	        	if (middle == null){
	        		middle = husband.get("mi");
	        	}
	        	String suffix = husband.get("suffix");
	        	if (suffix != null){
	        		if (middle != null){
	        			middle = middle + " " + suffix;
	        		} else if (first != null){
	        			first = first + " " + suffix;
	        		} else {
	        			first = suffix;
	        		}
	        	}
	        	first 	= notNull(first);
	        	middle 	= notNull(middle);
	        	last 	= notNull(last);
	        	
	        	granteeString = addName(granteeString, last, first, middle);
        	}
        	// process wife
        	if (grantee.size() == 2){
        		Map<String,String> wife = grantee.get(1);
	        	String last = wife.get("name");
	        	String first = wife.get("first");
	        	String middle = wife.get("middle");
	        	if (middle == null){
	        		middle = wife.get("mi");
	        	}
	        	String suffix = wife.get("suffix");
	        	if (suffix != null){
	        		if (middle != null){
	        			middle = middle + " " + suffix;
	        		} else if (first != null){
	        			first = first + " " + suffix;
	        		} else {
	        			first = suffix;
	        		}
	        	}
	        	first 	= notNull(first);
	        	middle 	= notNull(middle);
	        	last 	= notNull(last);       	

	        	granteeString = addName(granteeString, last, first, middle);
        	}
        }
        Set<String> remarks = getRemarks();
        if (StringUtils.isEmpty(granteeString) && remarks != null){
        	
	        Pattern namePat = Pattern.compile("(?is)\\b([A-Z]+\\s+[A-Z](?:\\s+[A-Z])?)\\s+([A-Z]+\\s+[A-Z](?:\\s+[A-Z])?)\\b");
	        for (String remark : remarks){
	        	 Matcher mat = namePat.matcher(remark);
	        	 if (mat.find()){
	        		 granteeString += "/" + mat.group(2);
	        	 }
			}
        } else {
        	if (StringUtils.isNotEmpty(granteeString)) {
        		granteeString = granteeString.replaceAll("(?is)\\b(?:D|M|PM|RS)\\s*(\\d+)\\s*/?\\s*(\\d+)", "$1_$2");
        		granteeString = granteeString.replaceAll("(?is)\\b(?:[A-Z])\\s*(\\d+)\\s+(\\d+)(?:\\s*PT\\b)?", "$1_$2");
        		granteeString = granteeString.replaceAll("(?is)/?\\s*\\b(?:RCD|DTD)\\b\\s*\\d{6}\\s*", "");
        		granteeString = granteeString.replaceAll("(?is)\\s*(\\d+)\\s+(\\d{1,3})\\b", "$1_$2");
        		String refInfo = "";
        		
        		Matcher m = Pattern.compile("(?is)\\s*(\\d+_\\d+)\\b").matcher(granteeString);
        		while (m.find()) {
        			refInfo = m.group(1).trim();
        			addToRemarks(refInfo.replaceFirst("-", "/"));
        			granteeString = granteeString.replaceFirst("(?is)\\s*/?\\s*" + refInfo, "");
        		}
        		
        		m.reset();
        		m = Pattern.compile("(?is)(?:\\b[A-Z]{1,2})?\\s*\\b(\\d+)\\b\\s*").matcher(granteeString);
           		while (m.find()) {
           			refInfo = m.group(0).trim();
           			addToRemarks(refInfo);
           			granteeString = granteeString.replaceFirst("/?\\s*" + m.group(0), "");
           		}
        	}
        		
        }
        try {
        	granteeString = granteeString.replaceAll("(?is)\\s*/\\s*(TRUSTEE)\\b", " $1");
        	gteeList = parseNames(gteeList, granteeString);
			
			resultMap.put(GranteeSet.class.getSimpleName(), GenericFunctions.storeOwnerInSet(gteeList, true));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return granteeString;
	}
	
	public static ArrayList<List> parseNames(ArrayList<List> nameList, String nameString) throws Exception{
		
		if (StringUtils.isNotEmpty(nameString)){
			
			nameString = nameString.replaceAll("(?is)&amp;", "&");
			nameString = nameString.replaceAll("(?is)\\A\\s*/", "");
			nameString = cleanName(nameString);
			nameString = nameString.replaceAll("(?is)\\band (?:AS\\s+)?(TRUSTEE)\\b", "$1");
			if (NameUtils.isNotCompany(nameString) && nameString.matches("(?is)\\w+\\s+\\w+\\s+\\w\\s+\\w+\\s+\\w")){
				nameString = nameString.replaceAll("(?is)(\\w+\\s+\\w+\\s+\\w)\\s+(\\w+\\s+\\w)", "$1 & $2");
			}
			String[] owners = nameString.split("\\s*/\\s*");
			
			for (String owner : owners){
				String[] names = StringFormats.parseNameNashville(owner, true);

				String[] suffixes = GenericFunctions.extractNameSuffixes(names);
				String[] type = GenericFunctions.extractAllNamesType(names);
				String[] otherType = GenericFunctions.extractAllNamesOtherType(names);
				GenericFunctions.addOwnerNames(owner, names, suffixes[0], suffixes[1], type, otherType,
							NameUtils.isCompany(names[2]), NameUtils.isCompany(names[5]), nameList);
			}
			
		}
		return nameList;
	}
	
	public static String cleanName(String name){
		name = name.replaceAll("(?is)\\bDECEASED\\b", "");
		name = name.replaceAll("(?is)\\b(CO PER|PERSONAL|PERS) REP\\b", "");
		name = name.replaceAll("(?is)\\bGUARDIANSHIP\\b", "");
		name = name.replaceAll("(?is)\\bTO WHOM IT MAY CONCERN\\b", "");
		name = name.replaceAll("(?is)\\bATTY IN FACT\\b", "");
		name = name.replaceAll("(?is)\\bNKA\\b", "");
		name = name.replaceAll("(?is)\\b[A|F]/?K/?A\\b", "");
		name = name.replaceFirst("(?is)(\\w+)\\s*/\\s*\\b\\1\\s*TR\\b\\s*\\Z", "$1");
		name = name.replaceFirst("(?is)\\bOWNER\\b\\s*/\\s*\\bASMT\\b\\s+\\bMAP\\b\\s*", "");
		name = name.replaceAll("(?is)([/\\w\\s]+)s*&?>\\s*\\b(?:V|MWSP|UM|SM|MMSP|AC)\\b\\s*", "$1");
		name = name.replaceAll("(?is)/?\\s*\\b(?:RCD|DTD)\\b\\s*\\d{6}\\s*", "");
		
		return name.trim();
	}
	
    /*
	 * Set SaleDataSet
	 */	
	@SuppressWarnings("unchecked")
	private void setParsedDataSale(ParsedResponse pr, String grantorString, String granteeString, String crossRefInstrumentString, long searchId, DataSite dataSite, ResultMap resultMap){

		// create sale data set
        Vector sdsVector = (Vector) pr.infVectorSets.get("SaleDataSet");
        if (sdsVector == null) {
        	sdsVector = new Vector();
        	pr.infVectorSets.put("SaleDataSet", sdsVector);
        }
        sdsVector.clear();
        SaleDataSet sds = new SaleDataSet();
        sdsVector.add(sds);
                
        String state = dataSite.getStateAbbreviation();
        // fill sale data set
		String docType = "";
		String recordedDate = "";
		String postedDate = "";
		String amount = "";
		String book = "";
		String page = "";
		String number = "";
		String caseNumber = "";
		@SuppressWarnings("unused")
		String year = "";
	
		
		if(instrumentInfo != null){
			
			docType = notNull(instrumentInfo.get("type"));
			recordedDate = notNull(formatDate(instrumentInfo, "recorded"));
			postedDate = notNull(formatDate(instrumentInfo, "posted"));
			amount = notNull(instrumentInfo.get("amount"));
			
			if(StringUtils.isBlank(book)){
				book = notNull(instrumentInfo.get("book"));
				page = notNull(instrumentInfo.get("page"));
				book = StringUtils.stripStart(book, "0");
				page = StringUtils.stripStart(page, "0");
			}
			
			String number1 = instrumentInfo.get("number");
			String year1 = instrumentInfo.get("recorded.year");
			if(number1 != null && year1!= null){
				number = number1;
				year = year1;
			}
			
		} else if(caseInfo != null){
			
			docType = notNull(caseInfo.get("type"));
			caseNumber = notNull(caseInfo.get("number"));
			recordedDate = notNull(formatDate(caseInfo, "filed"));
			postedDate = notNull(formatDate(caseInfo, "posted"));
		}
		
		if(aliasInfo != null){
			if(StringUtils.isBlank(number)){
				number = notNull(aliasInfo.get("number"));
				year = notNull(aliasInfo.get("year"));
			}
			if(StringUtils.isBlank(book)){
				book = notNull(aliasInfo.get("book"));
				page = notNull(aliasInfo.get("page"));
				book = StringUtils.stripStart(book, "0");
				page = StringUtils.stripStart(page, "0");
			}else if(StringUtils.isBlank(number)){
				String bookAlias = notNull(aliasInfo.get("book"));
				String pageAlias = notNull(aliasInfo.get("page"));
				if (StringUtils.isNotEmpty(bookAlias) && StringUtils.isNotEmpty(pageAlias)){
					bookAlias = StringUtils.stripStart(bookAlias, "0");
					pageAlias = StringUtils.stripStart(pageAlias, "0");
					number = bookAlias + "_" + pageAlias;
				}
			}
		}
		
		docType = docType.replaceAll("\\s{2,}", " ");
						
		if (docType.equals("DC") && dataSite.equals("MIMacombDT")){	// fix for bug #1430 
			docType = "DC-Declaration";
		}
		
		if ("MO".equals(state)){
			book = book.replaceAll("(?is)[DM]+", "");
			int numberLength = number.length();
			if (numberLength > 4){
				number = number.substring(numberLength - 4);
				number = StringUtils.stripStart(number, "0");
			}
		}
		if (CountyConstants.OH_Franklin == dataSite.getCountyId()) {
			number = formatInstNoForOHFranklin(number, recordedDate, "MM/dd/yyyy");
			
			if (page.matches("(?is)[A-Z]\\d")){
				page = page.replaceFirst("(?is)\\A([A-Z])(\\d)$", "$10$2");
			}
		} else if (CountyConstants.CA_Los_Angeles == dataSite.getCountyId()) {
			number = number.replaceFirst("(?is)\\A[A-Z]{1,2}\\s*(\\d+)", "$1");
		}
		if (dataSite.getStateFIPS().equals(StateContants.CA_STRING_FIPS)){
			if (StringUtils.isNotEmpty(year) && StringUtils.isNotEmpty(number)){
				number = year + "-" + number;
			}
		}
		
		if (StringUtils.isNotEmpty(caseNumber)){
			number = caseNumber;
		}
		
        if(!"".equals(/*year + */ number)){
        	sds.setAtribute("InstrumentNumber", /*year + */ number);
        } else {
        	if(StringUtils.isNotBlank(book)){
        		//sds.setAtribute("InstrumentNumber",  book + "_" + page);
        	}
        }
        if (StringUtils.isEmpty(book) && StringUtils.isNotEmpty(page)){
        	page = "";
        }
        if (StringUtils.isNotEmpty(book) && StringUtils.isEmpty(page)){
        	book = "";
        }
        sds.setAtribute("Book", book);
        sds.setAtribute("Page", page);
        sds.setAtribute("Book_Page", book + "_" + page);
        sds.setAtribute("RecordedDate", recordedDate);
        sds.setAtribute("PreparedDate", postedDate);
        sds.setAtribute("DocumentType", docType.replaceAll("\\s+", ""));
        sds.setAtribute("Grantor", grantorString);
        sds.setAtribute("Grantee", granteeString);
        sds.setAtribute("CrossRefInstrument", crossRefInstrumentString.replace('-','_'));
        
        resultMap.put(SaleDataSetKey.DOCUMENT_TYPE.getKeyName(), docType.replaceAll("\\s+", ""));
        
        amount = amount.replace("$", "").replace(",", "").replaceAll("(?is)[A-Z]", "");
        
        if(DocumentTypes.isMortgageDocType(docType,searchId)){        
        	sds.setAtribute("MortgageAmount", amount);
        } else if(DocumentTypes.isFederalTaxLienDocType(docType,searchId, pr.isParentSite()) || DocumentTypes.isStateTaxLienDocType(docType,searchId,pr.isParentSite()) || DocumentTypes.isJudgementDocType(docType,searchId, pr.isParentSite())){
        	sds.setAtribute("ConsiderationAmount", amount);
        }
		
	}
	
	@SuppressWarnings("unchecked")
	private void setParsedDataOtherInfo(ParsedResponse pr, DataSite dataSite, ResultMap resultMap){
		
		// create vector if necessary
        OtherInformationSet ois = (OtherInformationSet) pr.infSets.get("OtherInformationSet");
        if (ois == null) {
        	ois = new OtherInformationSet();        	
        } else {               
	        // check if other information contais SrcType info already
        	String srcType = (String) ois.getAtribute("SrcType");
        	if (srcType == null || srcType.length() == 0){
        		// fill other information infset
        		String src = dataSite.getSiteTypeAbrev();
        		ois.setAtribute("SrcType", src);
        		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), src);
        	} else{
        		resultMap.put(OtherInformationSetKey.SRC_TYPE.getKeyName(), srcType);
        	}
        }
    	pr.infSets.put("OtherInformationSet", ois);
	}
	
	private String addName(String initial, String last, String first, String middle){
		String retVal = initial; 
		if(!"".equals(last) || !"".equals(first)){
			if(!"".equals(retVal)){
				retVal += " / ";
			}
			String name = last;
			String rest = (first + " " + middle).trim();
			if(!"".equals(rest)){
				name = name + "," + rest;
			}
			retVal += name;
		}
		return retVal;
	}
	
	/**
	 * 
	 * @param info
	 */
	public void addInfo(DTRecord info){
		
		// nothing to add
		if(info == null){ return; }
		
		// skip if the two are not compatible
		if(!getId().equals(info.getId())){ return; }
		
		// missing info						
		if(aliasInfo == null){ aliasInfo = info.aliasInfo; }
		if(instrumentInfo == null){ instrumentInfo = info.instrumentInfo; }
		if(caseInfo == null){ caseInfo = info.caseInfo; }
		
		// additional info
		if(info.propertyList != null){
			propertyList = (propertyList != null) ? propertyList : new LinkedHashSet<Map<String,String>>();
			propertyList.addAll(info.propertyList);			
		}
		if(info.grantorList != null){
			grantorList = (grantorList != null) ? grantorList : new LinkedHashSet<List<Map<String,String>>>();			
			grantorList.addAll(info.grantorList);
		}
		if(info.granteeList != null){
			granteeList = (granteeList != null) ? granteeList : new LinkedHashSet<List<Map<String,String>>>();
			granteeList.addAll(info.granteeList);
		}		
		if(info.remarks != null){
			remarks = (remarks != null) ? remarks : new LinkedHashSet<String>();
			remarks.addAll(info.remarks);
		}
		if(referencedDocs == null){
			referencedDocs = info.referencedDocs;
		}
		if(info.refInstList!= null){
			refInstList = (refInstList != null) ? refInstList : new LinkedHashSet<Map<String,String>>();
			refInstList.addAll(info.refInstList);
		}
		if(info.partyRefList != null){
			partyRefList = (partyRefList != null) ? partyRefList : new LinkedHashSet<Map<String,String>>();
			partyRefList.addAll(info.partyRefList);			
		}
	}
	
	/**
	 * 
	 * @param detailsMap
	 */
	public void addAllInfo(Map<String,DTRecord>detailsMap){
		
		// nothing to do
		if(detailsMap == null){ return; }
		
        // add details to main node
		String crtId = getId();
		addInfo(detailsMap.get(crtId));
		
        // add details to referenced docs
        if(referencedDocs != null){
        	for(DTRecord refDoc: referencedDocs){
        		 String refId = refDoc.getId();
        		 if(refId == null){ continue; }
        		 refDoc.addInfo(detailsMap.get(refId));
        	}
        }  
        
        // treat the partyRefs
        fixPartyRefInfo(detailsMap);
        
	}
	
	/**
	 * 
	 * @param detailsMap
	 */
	public void fixPartyRefInfo(Map<String,DTRecord>detailsMap){
		
		// nothing to do
		if(detailsMap == null){ return; }	
		
		// nothing to do
		if(partyRefList == null){ return; }
		
		// treat the partyRefs
    	for(Iterator<Map<String,String>> it = partyRefList.iterator(); it.hasNext(); ){
    		Map<String,String> instr = it.next();
    		String book = instr.get("book");
			String page = instr.get("page");
			if(book == null || page == null){ continue; }
			String serverDoctype = instr.get("type");
			String key = book + "_" + page;
			if (StringUtils.isNotEmpty(serverDoctype)){
				key += "_" + page;
			}
			DTRecord detailsRecord = detailsMap.get(key);
			if(detailsRecord == null){ continue; }
			// remove from partyRefList
			it.remove();
			// add to referencedDocs
			referencedDocs = (referencedDocs != null) ? referencedDocs : new ArrayList<DTRecord>();
			referencedDocs.add(detailsRecord);
			// add to the refInstList
			Map<String, String> refs = new HashMap<String, String>();
			if(detailsRecord.instrumentInfo != null && detailsRecord.instrumentInfo.size() != 0){
				refs.putAll(detailsRecord.instrumentInfo);
			}
			if(detailsRecord.aliasInfo != null && detailsRecord.aliasInfo.size() != 0){
				refs.putAll(detailsRecord.aliasInfo);
			}
			if (refs.size() > 0){
				refInstList.add(refs);
			}
    	}
	}
	
	
	/**
	 * returns image info
	 * @return String[2] containing id,description of image. null if no image exists
	 */
	public String [] getDTImageInfo(){
		String id = null;
		String description = null;
		
		if(instrumentInfo != null){
			id = instrumentInfo.get("image.image_params.document_index_id");
			if(id != null){
				description = instrumentInfo.get("image.image_params.description");
			}
		}
		if(id == null && aliasInfo != null){
			id = aliasInfo.get("image.image_params.document_index_id");
			if(id != null){
				description = aliasInfo.get("image.image_params.description");
			}
		}
		if(id != null && description != null){
			return new String[] {id, description};
		} else {
			return null;
		}
	}
	
	public static String formatInstNoForOHFranklin(String instNo, String recordedDate, String dateFormat) {
		String number = instNo;
		
		if(instNo == null || recordedDate == null) {
			return instNo;
		}
		
		if(number.length() < 7 && number.matches("\\d+")) {

			number = StringUtils.leftPad(number, 7, "0");
			if(!"".equals(recordedDate.trim())) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
				try {
					number = sdf.format(new SimpleDateFormat(dateFormat).parse(recordedDate)) + number;
					instNo = number;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
		
		return instNo;
	}
}
