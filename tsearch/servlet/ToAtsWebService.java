package ro.cst.tsearch.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ToAtsWebService extends HttpServlet{
	
	private static final long serialVersionUID = -6125118371895926083L;

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse res)throws ServletException, IOException {
		if(req.getRequestURI().toLowerCase().contains("ats2")){
			 getServletConfig().getServletContext().getRequestDispatcher("/services/Ats2Service").forward(req,res);
		}
		else{
			getServletConfig().getServletContext().getRequestDispatcher("/services/AtsService").forward(req,res);
		}
	}
}
