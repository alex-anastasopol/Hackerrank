package ro.cst.tsearch.extractor.xml;

public class SimpleParseTokenResult {
	private String tokenParsed;
	private String initialSource;
	private String finalSource;
	public String getTokenParsed() {
		return tokenParsed;
	}
	public void setTokenParsed(String tokenParsed) {
		this.tokenParsed = tokenParsed;
	}
	public String getInitialSource() {
		return initialSource;
	}
	public void setInitialSource(String initialSource) {
		this.initialSource = initialSource;
	}
	public String getFinalSource() {
		return finalSource;
	}
	public void setFinalSource(String finalSource) {
		this.finalSource = finalSource;
	}
}
