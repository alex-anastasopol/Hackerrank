package ro.cst.tsearch.templates.edit.client;

import java.util.HashMap;
import java.util.List;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * @author Cristian Stochina
 */
public interface TemplateEditServiceAsync {
	
	public void getTemplateInfo(long searchId, long userId, String templateName,AsyncCallback<TemplateInfoResultG> callback);

	@SuppressWarnings("rawtypes")
	public void saveTemplate(long searchId,long userId,String templateName, int templateId, String  templateContent, String buttonLabel, boolean makeBackup, HashMap<String, HashMap<String, Boolean>> statementsMap, AsyncCallback callback);
	
	@SuppressWarnings("rawtypes")
	public void deleteGeneratedTemplate(long searchId,long userId,String templateName, int templateId, boolean force,AsyncCallback callback);
	
	@SuppressWarnings("rawtypes")
	public void  getState(long searchId,String templateName, int templateId, AsyncCallback callback);
	
	@SuppressWarnings("rawtypes")
	public void getLabelList (long searchId,long userId,AsyncCallback callback);
	
	@SuppressWarnings("rawtypes")
	public void getMaxInactiveIntervalForTemplateEditing(AsyncCallback callback);
	
	@SuppressWarnings("rawtypes")
	public void getElement(long searchId,long userId,String label,List<InstrumentStructForUndefined> instrumentList,AsyncCallback callback);
	
	@SuppressWarnings("rawtypes")
	public void makeTempBackup(long searchId,long userId,String templateName, int templateId, String  templateContent,AsyncCallback callback);
	
	@SuppressWarnings("rawtypes")
	public void restoreTempBackup(long searchId,long userId,String templateName, int templateId, AsyncCallback callback);
	
	public void previewTemplate(String templateName, String  templateContent, long searchId, AsyncCallback<String> callback);
	
	void logMessage(long searchId, String message, AsyncCallback<Void> callback);
	
	void getLinkForLastCopiedChapter(long searchId, boolean forcePdf, AsyncCallback<String[]> callback) ;

	public void uploadToSSf(long searchId, long userId, String templateName, AsyncCallback<Boolean> asyncCallback);
	
}