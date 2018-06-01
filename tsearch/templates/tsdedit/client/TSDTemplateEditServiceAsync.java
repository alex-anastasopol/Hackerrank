package ro.cst.tsearch.templates.tsdedit.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * @author Cristian Stochina
 */
public interface TSDTemplateEditServiceAsync {
	
	@SuppressWarnings("unchecked")
	public void	getTSDTemplateContent( long searchId, long userId, String templateName, AsyncCallback callback );
	
	@SuppressWarnings("unchecked")
	public void	saveTemplate( long searchId, long userId, String templateName, int templateId, String templateContent, String buttonLabel, AsyncCallback callback );
	
	@SuppressWarnings("unchecked")
	public void	deleteGeneratedTemplate( long searchId, long userId, String templateName, int templateId, boolean force, AsyncCallback callback );

	void logMessage(long searchId, String message, AsyncCallback<Void> callback);
}

