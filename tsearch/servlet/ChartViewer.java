package ro.cst.tsearch.servlet;

import java.io.*;
import java.awt.image.*;
import javax.servlet.*;
import javax.servlet.http.*;
import com.keypoint.PngEncoder;

public class ChartViewer extends BaseServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4254180786114727794L;

	public void init() throws ServletException {
	}

	public void doRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		
		// get the chart from session
		
		String chartName = (String)request.getParameter("chartName");
		//System.err.println("ChartViewer: Nume " + chartName);
		HttpSession session = request.getSession();
		BufferedImage chartImage = (BufferedImage) session.getAttribute(chartName);

		// set the content type so the browser can see this as a picture
		response.setContentType("image/png");

		// send the picture
		PngEncoder encoder = new PngEncoder(chartImage, false, 0, 9);
		response.getOutputStream().write(encoder.pngEncode());
		session.removeAttribute(chartName);
	}
}
