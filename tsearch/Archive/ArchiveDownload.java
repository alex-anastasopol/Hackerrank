package ro.cst.tsearch.Archive;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.URLMaping;

public class ArchiveDownload extends BaseServlet {

	private static final Logger logger = Logger.getLogger(ArchiveDownload.class);

	public void doRequest(HttpServletRequest request, HttpServletResponse response) 
		throws ServletException, IOException
	{
		//getting the file path and name
		String sourcePath = FILES_PATH + "Archive/";
		String fileName = request.getParameter( RequestParams.ARCHIVE_FILE_NAME );
		File sf = new File (sourcePath + fileName);
		//error message
		if(!sf.exists()) {
			Writer out=response.getWriter();
			out.write("<html><head>");
			out.write("<LINK media=screen href='"+URLMaping.STYLESHEETS_DIR+"/default.css' type=text/css rel=stylesheet>\n");
			out.write("</head><body>");							
			out.write("File not found ! <br>");
			out.write("<input type=\"button\" name=\"Button\" value=\"Back\" onClick=\"javascript:history.back();\" class=\"button\">");
			out.write("</body></html>");
			out.close();
			return;				   
		}
		//setting content type
		if (fileName.endsWith(".pdf"))
			response.setContentType("application/pdf");
		else
			response.setContentType("image/tiff");
			
		//returning the file content
		response.setContentLength((int)sf.length());
		OutputStream out=response.getOutputStream();							
		InputStream in=new BufferedInputStream(new FileInputStream(sf));
		byte[] buff=new byte[3000];
		int n;
		while((n=in.read(buff))>0){
			out.write(buff, 0, n);
		}
		in.close();					 
		out.close();				   
		
		response.getOutputStream().close();
		//adding the download operation to reports	--- 
		//currently unavailable
		//DBManager.insertDownloadFromArchive(fileName);
	}
}
