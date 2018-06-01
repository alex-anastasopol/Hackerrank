package ro.cst.tsearch.search.validator;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.servers.response.ServerResponse;

/**
 * RegisterDocValidatorParentSite.
 */
public class RegisterDocValidatorParentSite extends DocsValidator {
	
	public RegisterDocValidatorParentSite(Search search){
		super(search);
	}
	
	public boolean isValid(ServerResponse response) {
//		boolean valid = isValidOnParentSite(response);
		/*if(valid) {
			removeTooManyCrossRef(response);
		}*/
		return true;
	}

	
	
}
