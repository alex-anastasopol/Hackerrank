package ro.cst.tsearch.templates.tsdedit.client;

import com.google.gwt.user.client.rpc.RemoteService;

/**
 * @author Cristian Stochina
 */
public interface TSDTemplateEditService extends RemoteService{
	
	public String  getTSDTemplateContent( long searchId, long userId, String templateName ) throws TSDTemplateSearchPermisionException;
	
	public Boolean saveTemplate( long searchId, long userId, String templateName, int templateId, String  templateContent, String buttonLabel )throws TSDTemplateSearchPermisionException;
	
	public Boolean deleteGeneratedTemplate( long searchId, long userId, String templateName, int templateId, boolean force )throws TSDTemplateSearchPermisionException;

	void logMessage(long searchId, String message);
	
}
