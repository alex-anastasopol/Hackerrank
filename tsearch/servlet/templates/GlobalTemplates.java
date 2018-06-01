package ro.cst.tsearch.servlet.templates;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.templates.GlobalTemplatesManagementBean;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.search.filter.testnamefilter.GenericNameFilterTestConf;
import ro.cst.tsearch.search.filter.testnamefilter.GenericNameFilterTestError;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.templates.GlobalTemplateFactory;
import ro.cst.tsearch.templates.Template;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.URLMaping;

public class GlobalTemplates extends BaseServlet {
	final public static String ERROR_UPLOADING = "Error uploading file.";
	final public static String ERROR_WRONG_TYPE = "Only text/xml/doc/html files are accepted.";
	final public static String ERROR_SAVING_UPLOADED = "Error saving the uploaded file.";
	final public static String ERROR_MISSING_ACTION = "What do you want from me??!";
	final public static String ERROR_MISSING_UPLOAD_FILE = "Please provide a file.";
	final public static String ERROR_MISSING_FILE_LIST = "Please select a template.";
	final public static String ERROR_UNLOADED_TEMPLATE = "Template is not loaded!";
	final public static String ERROR_WRONG_FILE_NAME = "Please provide a correct file name. File names can't contain path separators! File names have to have extension!";
	final public static String ERROR_TEMPLATE_EXISTS = "A template with the provided name already exists.";



	public void doRequest(HttpServletRequest request,
			
			HttpServletResponse response) throws IOException, ServletException {
		User currentUser = (User) request.getSession().getAttribute(
				SessionParams.CURRENT_USER);
		if (currentUser.getUserAttributes().isTSAdmin()) {
			String m = request.getMethod().toUpperCase();
			if (m.equals("GET"))
				doGet(request, response);
			else if (m.equals("POST"))
				doPost(request, response);
		} else {
			// we should
			throw new ServletException(
					GenericNameFilterTestConf.ERROR_NO_TSADMIN);
		}
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		// we remove the names bean from session
		HttpSession s = request.getSession();
		s.removeAttribute("postedparams");
		// if the method is get, then I have no input from my form
		String searchId = (String) request
				.getParameter(RequestParams.SEARCH_ID);
		if (searchId == null)
			searchId = Integer.toString(Search.SEARCH_NONE);
		forward(request, response, URLMaping.GLOBAL_TEMPLATES_JSP + "?"
				+ RequestParams.SEARCH_ID + "=" + searchId);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		// variables that defines our upload
		// just to debug, remove it in final
		HttpSession s = request.getSession();
		GlobalTemplatesManagementBean b = (GlobalTemplatesManagementBean) s
				.getAttribute("postedparams");
		b.reset();
		GenericNameFilterTestError errorMessage = new GenericNameFilterTestError();
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		// upload the file and save/compute the parameters
		if (isMultipart) {
			// start getting the file
			DiskFileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload upl = new ServletFileUpload(factory);
			// Parse the request
			try {
				List items = upl.parseRequest(request);
				// Process the uploaded items
				Iterator iter = items.iterator();
				while (iter.hasNext()) {
					FileItem item = (FileItem) iter.next();
					if (item.isFormField()) {
						// do the non file fields
						String fN = item.getFieldName();
						String fV = item.getString();
						setValues(b, fN, fV);
					} else {
						if (item.getSize() > 0) {
							String contentType = item.getContentType();
							if (contentType.equals("text/xml")
									|| contentType.equals("text/plain")
									|| contentType.equals("application/msword")
									|| contentType.equals("text/html")) {
								try {
									File saveTo = File.createTempFile("globaltemplate", ".tmp",
											new File(ServerConfig.getConnectionTemplatesPath()));
									item.write(saveTo);
									b.setTempFile(saveTo.getName());
								} catch(Exception e){
									errorMessage.setError(ERROR_SAVING_UPLOADED + ": " + e.getMessage());
								}
							} else {
								errorMessage.setError(ERROR_WRONG_TYPE);
							}
						}
					}
				}
			} catch (FileUploadException e){ 
				errorMessage.setError(ERROR_UPLOADING + ": " + e.getMessage());
			}
		}
		
		if (b.getAction().equalsIgnoreCase("Reload Templates")){
			reloadTemplates();
		} else if (b.getAction().equalsIgnoreCase("Add")){
			uploadTemplate(b, errorMessage, false);
		} else if (b.getAction().equalsIgnoreCase("Edit")){
			updateTemplate(b, errorMessage);
		} else if (b.getAction().equalsIgnoreCase("Remove")){
			removeTemplate(b, errorMessage);
		} else if (b.getAction().equalsIgnoreCase("View")){
			viewTemplate(b, errorMessage);
		} else {
			errorMessage.setError(ERROR_MISSING_ACTION);
		}
		
		if (b.getTempFile().length() > 0){
			new File(ServerConfig.getConnectionTemplatesPath() + File.separator + b.getTempFile()).delete();
		}
		b.setError(errorMessage.toString());
		s.setAttribute("postedparams", b);
		String searchId = b.getSearchId();
		if (searchId == null)
				searchId = Integer.toString(Search.SEARCH_NONE);
		forward(request, response, URLMaping.GLOBAL_TEMPLATES_JSP + "?"
				+ RequestParams.SEARCH_ID + "=" + searchId);
		// response.sendRedirect(URLMaping.path + URLMaping.GenericNameFilter);

	}
	
	public void uploadTemplate(GlobalTemplatesManagementBean b, GenericNameFilterTestError e, boolean overWrite){
		if (b.getTempFile().length()>0){
			if (b.getFileName().length() > 0){
					GlobalTemplateFactory gtf  = GlobalTemplateFactory.getInstance();
				File file = new File(ServerConfig.getConnectionTemplatesPath() + File.separator + b.getTempFile());
				File template = new File(ServerConfig.getConnectionTemplatesPath() + File.separator + b.getFileName());
				File templateRevert = new File(ServerConfig.getConnectionTemplatesPath() + File.separator);
				File templateBackup = new File(ServerConfig.getConnectionTemplatesPath() + File.separator + b.getFileName() + "_backup");
				if (overWrite || (gtf.getTemplateByNameExact(b.getFileName()) == null && !template.exists())){
					if(!file.renameTo(template)) {
						if(template.renameTo(templateBackup)) {
							if(!file.renameTo(template)) {
								template.renameTo(templateRevert);
							}
						} else {
							if(template.delete()) {
								template.renameTo(templateBackup);
							}
						}
					}
					b.setTempFile("");
					gtf.readFileTemplate(template);
				} else {
					e.setError(ERROR_TEMPLATE_EXISTS);
				}
			} else {
				e.setError(ERROR_WRONG_FILE_NAME);
			}
		} else {
			e.setError(ERROR_MISSING_UPLOAD_FILE);
		}
	}
	
	public void updateTemplate(GlobalTemplatesManagementBean b, GenericNameFilterTestError e){
		if (b.getFileList().length() > 0){
			uploadTemplate(b, e, true);
		} else {
			e.setError(ERROR_MISSING_FILE_LIST);
		}
	}
	
	public void removeTemplate(GlobalTemplatesManagementBean b, GenericNameFilterTestError e){
		if (!b.getFileList().equals("")){
			GlobalTemplateFactory.getInstance().removeTemplate(b.getFileList());
		} else {
			e.setError(ERROR_MISSING_FILE_LIST);
		}
	}
	
	public void viewTemplate(GlobalTemplatesManagementBean b, GenericNameFilterTestError e){
		if (!b.getFileList().equals("")){
			Template template = GlobalTemplateFactory.getInstance().getTemplateByName(b.getFileList());
			if (template != null){
				b.setContent(template.getTemplateContent());
			} else {
				e.setError(ERROR_UNLOADED_TEMPLATE);
			}
		} else {
			e.setError(ERROR_MISSING_FILE_LIST);
		}
	}
		
	public void reloadTemplates(){
		GlobalTemplateFactory.getInstance().removeAllTemplates();
		GlobalTemplateFactory.getInstance().readTemplatesFromSources(
				ServerConfig.getConnectionTemplatesPath(), 
				GlobalTemplateFactory.defaultFileFilter);
	}
	
	public void setValues(GlobalTemplatesManagementBean b, String name, String value){
		if (name.equalsIgnoreCase("filename")){
			if (!value.contains(File.separator) && value.contains(".")){
				b.setFileName(value);
			} else {
				b.setFileName("");
			}
		}
		if (name.equalsIgnoreCase("filelist")){
			b.setFileList(value);
		}
		if (name.equalsIgnoreCase("globaltemplates")){
			b.setAction(value);
		}
		if (name.equalsIgnoreCase("searchid")){
			b.setSearchId(value);
		}
	}
}
