package ro.cst.tsearch.servers.types;

public class TXBexarTR extends TXGenericACTTR{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6228239512302423449L;

	public TXBexarTR(String rsRequestSolverName, String rsSitePath, String rsServerID, String rsPrmNameLink, long searchId, int mid) {
		super(rsRequestSolverName, rsSitePath, rsServerID, rsPrmNameLink, searchId, mid);
	}
	
	@Override
	protected String cleanIntermediaryResponse(String rsResponse, String linkStart) {
		String contents;
		String delimiterString = "<table width=\"90%\" cellpadding=\"6\" cellspacing=\"0\" border=\"1\"";
		int indexOf = rsResponse.indexOf(delimiterString);
		contents = rsResponse.substring(indexOf);		
		contents = contents.replace("(?im)<caption align=\"right\">Your search took 0 seconds.</caption>.*", "</table>");
		
		//contents = rsResponse.replaceAll("(?is).*To\\s*view\\s*the\\s*full\\s*record[^>]+>[^>]+>\\s*<table[^>]*>\\s*<tr[^>]*>\\s*<td[^>]*>(.*)<caption[^>]*>Your\\s*search\\s*took.*", "$1</table>");
		contents = contents.replaceAll("(?is)<a\\s+href\\s*=\\s*\\\"showlist.jsp\\?sort[^\\\"]+\\\"[^>]*>", "");
		
		//TO DO for each county change link in a separate method		
		contents = contents.replaceAll("(?is)<a\\s+href\\s*=\\s*'([^']+)'[^>]*>", "<a href=\"" + linkStart + "/act_webdev/bexar/$1\">");
		
		contents = contents.replaceAll("(?is)\\s+<table", "<table");
		contents = contents.replaceAll("(?is)onMouse[^\\\"]*\\\"[^\\\"]*\\\"", "");
		contents = contents.replaceAll("(?is)<!--[^-]*-->", "");
		contents = contents.replaceAll("(?is)<tr\\s{2,}", "<tr ");
		contents = contents.replaceAll("(?is)&nbsp;", " ");
		return contents;

	}

}
