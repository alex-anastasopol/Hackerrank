package ro.cst.tsearch.search.validator;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.servers.response.ServerResponse;

public class AlwaysRejectDocsValidator extends DocsValidator {


	private static final long serialVersionUID = -8869872442936880593L;

	public AlwaysRejectDocsValidator(Search search) {
		super(search);
	}

	@Override
	public boolean isValid(ServerResponse response) {
		return false;
	}
}
