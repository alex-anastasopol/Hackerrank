package ro.cst.tsearch.templates.edit.client;

import com.google.gwt.user.client.rpc.RemoteService;

import java.util.*;

/**
 * @author Cristian Stochina
 */
public interface TemplateEditService extends RemoteService{
	
	public TemplateInfoResultG getTemplateInfo( long searchId, long userId, String templateName) throws TemplateSearchPermisionException;
	
	public Boolean saveTemplate( long searchId, long userId, String templateName, int templateId, String  templateContent, String buttonLabel, boolean makeBackup, HashMap<String, HashMap<String, Boolean>> statementsMap)throws TemplateSearchPermisionException;
	
	public Boolean deleteGeneratedTemplate( long searchId, long userId, String templateName, int templateId, boolean force )throws TemplateSearchPermisionException;
	
	public String  getState( long searchId, String templateName, int templateId );
	
	public List<String>  getLabelList ( long searchId, long userId);
	
	public int getMaxInactiveIntervalForTemplateEditing( );
	
	public String getElement( long searchId, long userId, String label, List<InstrumentStructForUndefined> instrumentList);
	
	public Boolean makeTempBackup( long searchId, long userId, String templateName, int templateId, String  templateContent);
	
	public Boolean restoreTempBackup( long searchId, long userId, String templateName, int templateId);
	
	public String previewTemplate(String templateName, String templateContent, long searchId);

	void logMessage(long searchId, String message);

	String[] getLinkForLastCopiedChapter(long searchId, boolean forcePdf) throws Exception;
	
	public Boolean uploadToSSf(long searchId, long userId, String templateName)  throws Exception;
}

