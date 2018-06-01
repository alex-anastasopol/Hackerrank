package ro.cst.tsearch.servlet.templates;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ro.cst.tsearch.bean.templates.GlobalTemplatesManagementBean;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.generic.IOUtil;
import ro.cst.tsearch.search.filter.testnamefilter.GenericNameFilterTestConf;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.download.DownloadFile;
import ro.cst.tsearch.templates.GlobalTemplateFactory;
import ro.cst.tsearch.templates.Template;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.SessionParams;

public class GlobalTemplatesView extends BaseServlet{
	
	private static final long serialVersionUID = -3084641769115044809L;

	public void doRequest(HttpServletRequest request,
						HttpServletResponse response) throws IOException, ServletException {
		
		User currentUser = (User) request.getSession().getAttribute(
				SessionParams.CURRENT_USER);
		if (currentUser.getUserAttributes().isTSAdmin()) {
				doGet(request, response);
		} else {
			throw new ServletException(
					GenericNameFilterTestConf.ERROR_NO_TSADMIN);
		}
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		HttpSession s = request.getSession();
		GlobalTemplatesManagementBean b = (GlobalTemplatesManagementBean) s
				.getAttribute("postedparams");
		String output = b.getContent();
		String ext = FileUtils.getFileExtension(b.getFileList());
		boolean isImage = ext.equalsIgnoreCase(".pdf") || 
				ext.equalsIgnoreCase(".tif")  || 
				ext.equalsIgnoreCase(".tiff") || 
				ext.equalsIgnoreCase(".doc") ||
				ext.equalsIgnoreCase(".gif") ||
				ext.equalsIgnoreCase(".jpg") ;
		
		if(isImage) {
			Template templateByName = GlobalTemplateFactory.getInstance().getTemplateByName(b.getFileList());
			if(templateByName != null) {
				
				response.setContentType(DownloadFile.getSpecificExt(b.getFileList()));
				response.setHeader(
	                    "Content-Disposition",
	                    " attachment; filename="
	                        + b.getFileList());
				
				File file = new File(templateByName.getTemplateFileSystemPath());
				int length = (int) file.length();
	            FileInputStream source = new FileInputStream(file);
	            response.setContentLength( length);
                IOUtil.copy(source, response.getOutputStream());
			} else {
				try{
					String favIco = "<link rel='shortcut icon' href='/title-search/favicon.ico' type='image/x-icon'>" + 
							"<link rel='icon' href='/title-search/favicon.ico' type='image/x-icon'>";
					//file not found
					response.setContentType( "text/html" );
					String string = "<html><head>" + favIco + "</head><body>" +  
							"File Not Found!</body></html>";
					byte [] dataToDisplay = string.getBytes();
					IOUtil.copy( new ByteArrayInputStream( dataToDisplay ) , response.getOutputStream());
					}catch(Exception e){
						e.printStackTrace();
					}
			}
			
			
            
		} else {
			if (output != null && ! output.equals("")){
				response.setContentType(DownloadFile.getSpecificExt(b.getFileList()));
	           	response.setHeader(
	                       "Content-Disposition",
	                       " attachment; filename="
	                           + b.getFileList());
	           	try {
	           		PrintWriter out = response.getWriter();
	           		out.append(output);
	           		out.flush();
	           	}catch(Exception e){}
			}
		}
	}
}
