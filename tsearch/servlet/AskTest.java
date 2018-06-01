package ro.cst.tsearch.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.MultipartParameterParser;


public class AskTest extends BaseServlet {
	
	private static final long serialVersionUID = 1L;
	
	public void doRequest(HttpServletRequest request, HttpServletResponse response)  throws IOException, BaseException {
		
		try{
			
			MultipartParameterParser mpp = new  MultipartParameterParser(request);
			File fileToUpload = mpp.getFileParameter("xmlData");
			String xmlData = FileUtils.readXMLFile(fileToUpload.getAbsolutePath());
			request.setAttribute("xmlData", xmlData);
			forward(request, response, "/AskResponse");
			
		}catch(Exception e){}
		
		PrintWriter out = response.getWriter();
		out.println("<html>");
		out.println("<title>AskTest</title>");
		out.println("<form action=\"/title-search/AskTest\" enctype=\"multipart/form-data\" method=\"post\">");
		out.println("Select File: &nbsp;&nbsp;<input type=\"file\" name=\"xmlData\" size=\"40\"></p> <input type=\"submit\" value=\"Submit\"> </form>");		
		out.println("</html>");

	}
}
