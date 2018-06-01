package ro.cst.tsearch.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ro.cst.tsearch.data.MaintenanceMessage;

/**
 * 
 * @author Oprina George
 * 
 *         Mar 13, 2013
 */

public class MaintenanceMessageServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 2073336992828855007L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		res.getWriter().write(MaintenanceMessage.getFormattedMessageOld(req));
	}

}
