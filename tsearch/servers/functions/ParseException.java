package ro.cst.tsearch.servers.functions;

import ro.cst.tsearch.extractor.xml.ResultMap;

public class ParseException extends Exception {
	private ResultMap resultMap = null;
	private String keyToBeParsed= ""; 
	private Exception originalException = null;
	
	public ParseException(Exception e, ResultMap resultMap, String keyToBeParsed) {
		this.originalException = e;
		this.resultMap = resultMap;
		this.keyToBeParsed = keyToBeParsed;
	}
	
	@Override
	public void printStackTrace() {
		originalException.printStackTrace();
	}
}
