package ro.cst.tsearch.templates.edit.client;

import com.google.gwt.user.client.rpc.IsSerializable;

public class TemplateInfoResultG implements IsSerializable  {
	private String	templateNewPath;
	private String	templateContent;
	private String	serverURL;
	private String	templateId;
	private String	statementsText;
	public String getTemplateNewPath() {
		return templateNewPath;
	}
	public void setTemplateNewPath(String templateNewPath) {
		this.templateNewPath = templateNewPath;
	}
	public String getTemplateContent() {
		return templateContent;
	}
	public void setTemplateContent(String templateContent) {
		this.templateContent = templateContent;
	}
	public String getServerURL() {
		return serverURL;
	}
	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}
	public String getTemplateId() {
		return templateId;
	}
	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}
	public String getStatementsText() {
		return statementsText;
	}
	public void setStatementsText(String statementsText) {
		this.statementsText = statementsText;
	}
}
