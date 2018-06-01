package ro.cst.tsearch.templates.emptytemplateedit.client;

//import ro.cst.tsearch.data.DataException;
//import ro.cst.tsearch.exceptions.BaseException;

import com.google.gwt.user.client.rpc.RemoteService;

public interface EmptyTemplateEditService extends RemoteService{

	public Boolean saveTemplate(long policyId,
													 int commId,
													 String policyName, 
													 String shortPolicyName,
													 String fileContent)throws EmptyTemplateEditException;
	
	
	public Boolean deleteTemplate(int comm_Id, int policyId)throws  EmptyTemplateEditException;
					
	public String getTemplateContent(int fileId)throws EmptyTemplateEditException;
	
	
}
