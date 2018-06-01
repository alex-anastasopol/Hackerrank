/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.servlet;

import java.io.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.URLMaping;
import ro.cst.tsearch.bean.UploadDocType;

/**
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class GetImagefromDisk extends BaseServlet {
	public void doRequest(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException
			{
				String file_name=request.getParameter("image");
				String sId=request.getParameter(RequestParams.SEARCH_ID);
				String file_path;
				String file_src;
				//obtaining current search object
				HttpSession session = request.getSession();
				User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
				Search global = (Search) currentUser.getSearch(Integer.parseInt(sId));
				
				
				file_path=global.getSearchDir()+UploadDocType.REGISTER_FOLDER+File.separator;
				file_src=file_path+file_name;
				//out.println("checking:["+file_src+"]");
				File f=new File(file_src);
				if(!f.exists()) {
					Writer out=response.getWriter();
					out.write("<html><head>");
					out.write("<LINK media=screen href='"+URLMaping.STYLESHEETS_DIR+"/default.css' type=text/css rel=stylesheet>\n");
					out.write("</head><body>");							
					out.write("Image file not found ! Probably it wasn't available on the Official Document Server. " +
						"If you can get a copy of the document from another source, please scan it and click on " +
						"NO IMAGE link in the Remarks field of Index of Found Documents to upload the image, before " +
						"you click Create TSR. <br><br><br><br>");
//					request.setAttribute("serverResponse", " Image file not found ! Probably it wasn't available on the Official Document Server ");
					
//					forward(request, response, URLMaping.PARENT_SITE_RESPONSE);

					out.write("<input type=\"button\" name=\"Button\" value=\"Back\" onClick=\"javascript:history.back();\" class=\"button\">");
					
					out.write("</body></html>");
					out.close();
					return;				   
				}
				
				 //out.println("Bingo!!!");
				 response.setContentType("image/tiff");
				 response.setContentLength((int)f.length());
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
}
