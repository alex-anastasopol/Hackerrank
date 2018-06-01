package ro.cst.tsearch.servers.bean;

import com.stewart.ats.base.document.RegisterDocumentI;

public class DocumentParsedResponse {
	private RegisterDocumentI document;
	private String shortImageLink;
	private String parentSiteImageLink;
	public DocumentParsedResponse(RegisterDocumentI document, String shortImageLink, String parentSiteImageLink) {
		super();
		this.document = document;
		this.shortImageLink = shortImageLink;
		this.parentSiteImageLink = parentSiteImageLink;
	}
	public RegisterDocumentI getDocument() {
		return document;
	}
	public void setDocument(RegisterDocumentI document) {
		this.document = document;
	}
	public String getShortImageLink() {
		return shortImageLink;
	}
	public void setShortImageLink(String shortImageLink) {
		this.shortImageLink = shortImageLink;
	}
	public String getParentSiteImageLink() {
		return parentSiteImageLink;
	}
	public void setParentSiteImageLink(String parentSiteImageLink) {
		this.parentSiteImageLink = parentSiteImageLink;
	}
}
