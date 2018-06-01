package ro.cst.tsearch.servers.functions.smartUpload;

import com.stewart.ats.tsrindex.client.SimpleChapter;

public class ParseResult {
	private SimpleChapter  simpleChapter;
	
	private String index;
	
	private String imageLink;
	
	public SimpleChapter getSimpleChapter() {
		return simpleChapter;
	}
	public void setSimpleChapter(SimpleChapter simpleChapter) {
		this.simpleChapter = simpleChapter;
	}
	public String getIndex() {
		return index;
	}
	public void setIndex(String index) {
		this.index = index;
	}
	
	public void setImageLink(String imageLink) {
		this.imageLink = imageLink;
	}
	
	public String getImageLink() {
		return imageLink;
	}
}
