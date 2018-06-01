/*
 * Created on Jun 30, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.log4j.Category;

import ro.cst.tsearch.search.filter.matchers.name.NameMatcher;
import ro.cst.tsearch.search.name.NameUtils;
import ro.cst.tsearch.search.tokenlist.NameTokenList;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author elmarie
 */
public class RegisterNameFilterResponse extends NameFilterResponse{
	protected static final Category logger = Category.getInstance(RegisterNameFilterResponse.class.getName());
	protected static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + RegisterNameFilterResponse.class.getName());
		

	public RegisterNameFilterResponse(String key, NameMatcher nm,long searchId){
		super(key, nm,searchId);
		invalidPatterns.add(Pattern.compile(".*\\W+TR(\\W+.*|$)", Pattern.CASE_INSENSITIVE));
		invalidPatterns.add(Pattern.compile(".*\\W+TRUSTEE(\\W+.*|$)", Pattern.CASE_INSENSITIVE));
		threshold = new BigDecimal("0.8");
	}

	protected List getCandNames(ParsedResponse row) {
		List candNameGrantors = nameSetVector2NameTokenListList(row.getGrantorNameSet());
		List candNameGrantees = nameSetVector2NameTokenListList(row.getGranteeNameSet());

		List allNames = candNameGrantors;
		allNames.addAll(candNameGrantees);

		if (logger.isDebugEnabled()){
			
			Vector elements = row.getGrantorNameSet();
			StringBuffer sb = new StringBuffer();
			for (int i = 0; elements != null && i < elements.size(); i++)
				sb.append(elements.elementAt(i) + " ");
			if (elements != null && 0 < elements.size())
				logger.debug("grantor= " + sb.toString().replaceAll("\n\r", "").replaceAll("\\s+", " "));
			
			elements = row.getGranteeNameSet();
			sb = new StringBuffer();
			for (int i = 0; elements != null && i < elements.size(); i++)
				sb.append(elements.elementAt(i) + " ");
			if (elements != null && 0 < elements.size())
				logger.debug("grantee= " + sb.toString().replaceAll("\n\r", "").replaceAll("\\s+", " "));
			
			logger.debug("getCandNames returns " + allNames.size());
		}
		return allNames;
	}
	
	

	protected BigDecimal matchOwnerName(List candNames, NameTokenList[] owners){
		return matchOwnerOrSpouse(candNames, owners);
	}


	private BigDecimal matchOwnerOrSpouse(List candNames, NameTokenList[] owners) {
		BigDecimal validOwner = matchNameAllFM(candNames, owners);
		
		BigDecimal validSpouse = matchNameAllFM(candNames, new NameTokenList[]{owners[1], owners[0]}) ;
		
		if(validOwner.equals(ATSDecimalNumberFormat.NA))
			if(validSpouse.equals(ATSDecimalNumberFormat.NA))
				return ATSDecimalNumberFormat.ONE;
			else
				return validSpouse;
		else {
			return validOwner.max(validSpouse);
		}
		
		//BigDecimal validName = validOwner.max(validSpouse);
		
		/*	
		if (validName){
			logger.debug(candNameList + " e valid ");
		}else {
			//logger.debug(candNameList + " NU e valid ");
		}*/
		
		//return validName;
		//return false;
	}

	private BigDecimal matchNameAllFM(List intialCandNames, NameTokenList[] owners) {
		List candNames = (List)((ArrayList)intialCandNames).clone();
		
		String ownerLastName = NameTokenList.getString(owners[0].getLastName());
		String ownerMiddleName = NameTokenList.getString(owners[0].getMiddleName());
		String ownerMiddleNameInitial = NameTokenList.getInitial(ownerMiddleName);
		String ownerFirstName = NameTokenList.getString(owners[0].getFirstName());
		
		for (int i = 0; i < owners.length; i++) {
			if(!owners[i].isEmpty())
				break;
			else
				if(i==owners.length-1)
					return ATSDecimalNumberFormat.NA;
		}
		
		BigDecimal validCompanyName = ATSDecimalNumberFormat.ZERO;	
		if(NameUtils.isCompany(owners[0]) || 
				(!StringUtils.isStringBlank(ownerLastName) && StringUtils.isStringBlank(ownerFirstName) && StringUtils.isStringBlank(ownerMiddleName))) { // fix for bug #1313
            IndividualLogger.info( "ValidCompanyName score" ,searchId);
			validCompanyName = matchName(candNames,owners);
			if(validCompanyName.compareTo(ATSDecimalNumberFormat.ONE) == 0){
				return ATSDecimalNumberFormat.ONE;
			}			
		}		
		
		nameMatcher.setMatchInitial(true); // activate initial matching
		BigDecimal validAllNames = ATSDecimalNumberFormat.ZERO;
		if (!StringUtils.isStringBlank(ownerFirstName) || !StringUtils.isStringBlank(ownerMiddleName) ){
            IndividualLogger.info( "ValidAllNames score" ,searchId);
			validAllNames = matchName(candNames, owners);
			if(validAllNames.compareTo(ATSDecimalNumberFormat.ONE) == 0){
				return ATSDecimalNumberFormat.ONE;
			}			
		}
		nameMatcher.setMatchInitial(false); // de-activate initial matching
				
		BigDecimal validFirstName = ATSDecimalNumberFormat.ZERO;
		if (!StringUtils.isStringBlank(ownerFirstName)){
			NameTokenList newOwner = new NameTokenList(ownerLastName, ownerFirstName,"");
            IndividualLogger.info( "ValidFirstName score" ,searchId);
			validFirstName = matchName(candNames, new NameTokenList[]{newOwner, owners[1]});
			if(validFirstName.compareTo(ATSDecimalNumberFormat.ONE) == 0){
				return ATSDecimalNumberFormat.ONE;
			}
		}
		
		BigDecimal validMiddleNameInitial = ATSDecimalNumberFormat.ZERO;
		if (!StringUtils.isStringBlank(ownerMiddleName) && (!ownerMiddleNameInitial.equals(ownerMiddleName))){
			NameTokenList newOwner = new NameTokenList(ownerLastName, ownerFirstName, ownerMiddleNameInitial);
            IndividualLogger.info( "ValidMiddleNameInitial score" ,searchId);
			
			//fix for bug #820: purpose: reject candidate [MORRISON EVELYN N] when Ref is [MORRISON N E] while Ref in SA is [MORRISON N ELISHA] 
			// counts how many initials the reference has; we know for sure it has at least one initial, as middle name
			int cntInitialsRef = 1;
			if (ownerFirstName.equals(NameTokenList.getInitial(ownerFirstName))) 
				cntInitialsRef ++;
			// counts how many initials each candidat has; 
			// remove the candidates with different number of initials as the reference 
			// because their matching or lack of matching to the reference is already computed in ValidAllNames section
			int cntInitialsCand;
			String candFirstName, candMiddleName;
			for (Iterator iter = candNames.iterator(); iter.hasNext();) {
				NameTokenList[] candName  = (NameTokenList[]) iter.next();
				candFirstName = NameTokenList.getString(candName[0].getFirstName());
				candMiddleName = NameTokenList.getString(candName[0].getMiddleName());
				cntInitialsCand = 0;
				if (candFirstName.equals(NameTokenList.getInitial(candFirstName)))
					cntInitialsCand ++;
				if (candMiddleName.equals(NameTokenList.getInitial(candMiddleName)))
					cntInitialsCand ++;
				if (cntInitialsCand != cntInitialsRef)
					iter.remove();
			}
			
			validMiddleNameInitial = matchName(candNames, new NameTokenList[]{newOwner, owners[1]});
			if(validMiddleNameInitial.compareTo(ATSDecimalNumberFormat.ONE) == 0){
				return ATSDecimalNumberFormat.ONE;
			}			
		}
		
		BigDecimal validName = validCompanyName.max(validAllNames.max(validMiddleNameInitial.max(validFirstName)));
			
		return validName;
	}


	public void addInvalidPattern(String p){
		invalidPatterns.add(Pattern.compile(p, Pattern.CASE_INSENSITIVE));
	}


}
