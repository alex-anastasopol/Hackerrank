/*
 * Created on Nov 17, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.validator;

import java.io.File;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import org.apache.log4j.Category;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.propertyInformation.Address;
import ro.cst.tsearch.search.filter.matchers.algorithm.MatchAlgorithm;
import ro.cst.tsearch.search.filter.matchers.subdiv.SubdivMatcher;
import ro.cst.tsearch.servers.ServletServerComm;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.ServerResponse;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.ATSDecimalNumberFormat;
import ro.cst.tsearch.utils.IndividualLogger;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class DoctypeValidator extends DocsValidator {

	
	private static final long serialVersionUID = -8944545180090771488L;
	private static final Category logger = Category.getInstance(RegisterDocsValidator.class.getName());
	private static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + RegisterDocsValidator.class.getName());
	
	private SubdivMatcher subdivMatcher =null;
	private String addressMatcher = null; // not actually used. remained here so that searches can be de-serialized
	
	private boolean matchAddress = true; // used to enable/disable the address match in validAddresOrSubdivision()
	
	private long searchId=-1;
	
	public DoctypeValidator(Search search) {
		super(search);
		this.searchId = search.getID();
		subdivMatcher= new SubdivMatcher(searchId);
	}

	public boolean isValid(ServerResponse response) {
		int searchType = search.getSearchType();
		boolean searchFinished = (new Boolean ((String)search.getSa().getAtribute(SearchAttributes.SEARCHFINISH))).booleanValue();

		boolean valid = true;
		if (searchType == Search.PARENT_SITE_SEARCH
			&& searchFinished) {
			valid = isValidOnParentSite(response);
		} else {
			valid = isValidOnAutomatic(response);
		}
		
		/*if (valid){
			removeTooManyCrossRef(response);
		}*/
		return valid;
	}


	protected boolean isValidOnAutomatic(ServerResponse response) {
		boolean valid = validDoc(response);
		loggerDetails.debug("validAutomaticSearch = " + valid);
        IndividualLogger.info( "validAutomaticSearch = " + valid ,searchId);
		return valid;
	}

	protected boolean isValidOnParentSite(ServerResponse response) {
		/*boolean validDate = validDate(response, search.getSa());
		if (logger.isDebugEnabled())
			logger.debug(" validParentSite : validDate" + " = " + validDate);*/
		return  true; // always valid on parent site
	}




	/**
	 * Method ValidDoc.
	 * @param result
	 * @param sa
	 * @param i
	 * @return boolean
	 */
	protected boolean validDoc(ServerResponse result) {
		SearchAttributes sa = search.getSa();
		String docType = result.getParsedResponse().getSaleDataSet(0).getAtribute("DocumentType").trim();

		int[] types = 	new int[]{	DocumentTypes.PLAT_INT,
				DocumentTypes.EASEMENT_INT,
 				DocumentTypes.RESTRICTION_INT,
 				DocumentTypes.CCER_INT};
		
		return isOfDocType(docType, types, searchId);
	}
	



	
	public static boolean isOfDocType (String docType, int[] types,long searchId){
		boolean rez = false;
		for (int i = 0; i < types.length; i++) {
			if (DocumentTypes.checkDocumentType(docType, types[i],null,searchId)){
				rez = true;
				break;
			}
		}
		return rez;
	}

}
