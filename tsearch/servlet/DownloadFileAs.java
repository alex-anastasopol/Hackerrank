/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.servlet;

import java.io.*;
import java.math.BigDecimal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.bean.UploadDocType;

/**
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class DownloadFileAs extends HttpServlet {
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException
			{
				String filePath = request.getParameter("pdfFile");
				filePath = filePath.replaceAll("\\.\\.", "");
				String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
				filePath = filePath.substring(filePath.indexOf("/", 2) + 1);
				
				String rootPath = BaseServlet.FILES_PATH + File.separator;
				
				String filePathFinal = rootPath + filePath;
								
				//out.println("checking:["+file_src+"]");
				File f=new File(filePathFinal);
				if(!f.exists()) {
					f = new File(rootPath + File.separator + "tempZipFolder" + File.separator + filePath);
					if(!f.exists()) {	
						Writer out=response.getWriter();
						out.write("<html><head>");
						out.write("<LINK media=screen href='"+URLMaping.STYLESHEETS_DIR+"/default.css' type=text/css rel=stylesheet>\n");
						out.write("</head><body>");							
						out.write("Your document could not be found on our servers.<br><br><br><br>");
	//					request.setAttribute("serverResponse", " Image file not found ! Probably it wasn't available on the Official Document Server ");
						
	//					forward(request, response, URLMaping.PARENT_SITE_RESPONSE);
	
						out.write("<input type=\"button\" name=\"Button\" value=\"Back\" onClick=\"javascript:history.back();\" class=\"button\">");
						
						out.write("</body></html>");
						out.close();
						return;
					}
				}
				 //out.println("Bingo!!!");
				if ( fileName.toLowerCase().endsWith(".doc") ){
					response.setContentType("application/msword");
				} else if ( fileName.toLowerCase().endsWith(".csv") ){
					response.setContentType("application/vnd.ms-excel");
				} else {
					response.setContentType("unknown");
				}
				 response.setContentLength((int)f.length());
				 response.setHeader(
							"Content-Disposition",
							" attachment; filename=\""
								+ fileName
								+ "\"");
				OutputStream out=response.getOutputStream();							
				 InputStream in=new BufferedInputStream(new FileInputStream(f));
				 byte[] buff=new byte[100];
				 int n;
				 while((n=in.read(buff))>0)
				 {
				 	 out.write(buff, 0, n);
				 }
				 in.close();					 
				out.close();				   
				
			}

	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
		throws ServletException, IOException {
		doGet(request, response);
	}
	
}
