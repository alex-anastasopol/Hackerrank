package ro.cst.tsearch.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import ro.cst.tsearch.test.TokenizersTest;
import ro.cst.tsearch.utils.StringUtils;

public class TestCaseFileServlet extends HttpServlet {

	private static final long serialVersionUID = -1735108104531235426L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		String file = req.getParameter("file");
	
		if(StringUtils.isEmpty(file)){
			res.setContentType("text/html");
			PrintWriter writer = res.getWriter();
			writer.append("File Not Specified!");			
			writer.flush();
			return;
		}
		
		file = file.replace("'", "");
		String testFolder = TokenizersTest.unitTestRootFolder;		
		String fileName = file;
		
		File f = new File(fileName);
		if(!f.exists()){
			res.setContentType("text/html");
			PrintWriter writer = res.getWriter();
			writer.append("File Not Found!");			
			writer.flush();
			return;
		}
		
		String absoluteFileName = f.getAbsolutePath();
		String absoluteFolderName = new File(testFolder).getAbsolutePath();
		
		if(!absoluteFileName.startsWith(absoluteFolderName)){
			res.setContentType("text/html");
			PrintWriter writer = res.getWriter();
			writer.append("File is not a testcase file!");			
			writer.flush();
			return;
		}
		
		res.setContentType("text/plain");		
		OutputStream os = res.getOutputStream();		
		InputStream is = new FileInputStream(absoluteFileName);
		try{ 
			byte[] buffer = new byte[1024];
			int len;
			while( (len = is.read(buffer)) != -1){
				os.write(buffer, 0, len);
			}
		} finally{
			is.close();
		}
		os.flush();
	}

}
