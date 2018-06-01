package ro.cst.tsearch.servlet;

import org.apache.log4j.Logger;

import ro.cst.tsearch.templates.AddDocsTemplates;
import ro.cst.tsearch.templates.TemplateUtils;
import ro.cst.tsearch.templates.tsdedit.client.TSDTemplateEditService;
import ro.cst.tsearch.templates.tsdedit.client.TSDTemplateSearchPermisionException;
import ro.cst.tsearch.utils.SearchLogger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.stewart.ats.tsrindex.server.TsdIndexPageServer;

/**
 *The class implements  TemplateEditService and respond to RPC calls from client
 * @author Cristian Stochina
 */
public class TSDTemplateServlet extends RemoteServiceServlet  implements TSDTemplateEditService{
    
	private static final long serialVersionUID = 5254303027139652880L;
	protected static final Logger logger= Logger.getLogger(AddDocsTemplates.class);
	
	@Override
	public String  getTSDTemplateContent(long searchId,long userId, String templateName)throws TSDTemplateSearchPermisionException{
		TemplateUtils.TestAvailable  test = TemplateUtils.isSearchAvailable(searchId,userId);
		if (!test.available) {
			throw new TSDTemplateSearchPermisionException(test.errorBody);
		}
		Object obj = TemplateUtils.tempTSDContent.get(searchId);
		String str = "";
		if(obj!=null){
			str=(String)obj;
			TemplateUtils.tempTSDContent.remove(searchId);
		}else {
			try {
				str = TsdIndexPageServer.compileTemplateImpl(searchId,userId,templateName, false, null).getTemplateContent();
			}catch(Exception e) {
				e.printStackTrace();
			}
		}

		return str;
	}
	
	@Override
	public Boolean saveTemplate(long searchId,long userId, String templateName, int templateId, String templateContent, String buttonLabel) throws TSDTemplateSearchPermisionException{
		
		try{ return TemplateUtils.saveTemplate(this.getThreadLocalRequest().getSession(), this.getThreadLocalResponse(), searchId, userId, templateName, (long)templateId, templateContent, buttonLabel);}
		catch(Exception e){ throw new TSDTemplateSearchPermisionException(e.getMessage());}
	}

	@Override
	public Boolean deleteGeneratedTemplate(long searchId, long userId, String path, int templateId, boolean force) throws TSDTemplateSearchPermisionException{
		try{ return TemplateUtils.deleteGeneratedTemplate(this.getThreadLocalRequest(), searchId, userId, path, (long) templateId, force);}
		catch(Exception e){ throw new TSDTemplateSearchPermisionException( e.getMessage() );}
	}
	
	@Override
	public void logMessage( long searchId  , String message) {
		SearchLogger.info(message + SearchLogger.getTimeStamp(searchId) +".<BR><div>", searchId);
	}
}

