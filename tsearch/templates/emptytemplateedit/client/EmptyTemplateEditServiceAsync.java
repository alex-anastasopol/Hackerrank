package ro.cst.tsearch.templates.emptytemplateedit.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface EmptyTemplateEditServiceAsync {
	
	public void saveTemplate(long policyId,
											    int commId,
											    String policyName, 
											    String shortPolicyName,
											    String fileContent,AsyncCallback async);
	
	
	public void deleteTemplate(int comm_Id, int policyId,AsyncCallback async);
	
	
	public void getTemplateContent(int fileId,AsyncCallback async);
	
	
}
