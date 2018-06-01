package ro.cst.tsearch.templates;

import com.stewart.ats.tsrindex.client.shared.CompileTemplateResultG;

import ro.cst.tsearch.templates.edit.client.TemplateInfoResultG;

public class CompileTemplateResult {

	private String	templateNewPath;
	private String	templateContent;
	private String	serverURL;
	private String	templateId;
	private String	statementsText;
	private String	processedContent;
	
	public String getTemplateNewPath() {
		return templateNewPath;
	}

	public String getTemplateContent() {
		return templateContent;
	}

	public String getServerURL() {
		return serverURL;
	}

	public String getTemplateId() {
		return templateId;
	}
	
	public String getStatementsText() {
		return statementsText;
	}

	public void setTemplateNewPath(String templateNewPath) {
		this.templateNewPath = templateNewPath;
	}

	public void setTemplateContent(String templateContent) {
		this.templateContent = templateContent;
	}

	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

	public void setStatementsText(String statementsText) {
		this.statementsText = statementsText;
	}

	public String getProcessedContent() {
		return processedContent;
	}

	public void setProcessedContent(String processedContent) {
		this.processedContent = processedContent;
	}

	public TemplateInfoResultG toTemplateInfoResultG() {
		TemplateInfoResultG resultG = new TemplateInfoResultG();
		resultG.setStatementsText(getStatementsText());
		resultG.setServerURL(getServerURL());
		resultG.setTemplateContent(getTemplateContent());
		resultG.setTemplateId(getTemplateId());
		resultG.setTemplateNewPath(getTemplateNewPath());
		return resultG;
	}
	
	public CompileTemplateResultG toCompileTemplateResultG() {
		CompileTemplateResultG resultG = new CompileTemplateResultG();
		resultG.setStatementsText(getStatementsText());
		resultG.setServerURL(getServerURL());
		resultG.setTemplateContent(getTemplateContent());
		resultG.setTemplateId(getTemplateId());
		resultG.setTemplateNewPath(getTemplateNewPath());
		return resultG;
	}
}
