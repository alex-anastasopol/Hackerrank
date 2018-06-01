package ro.cst.tsearch.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.titledocument.abstracts.DocTypeReader;
import ro.cst.tsearch.titledocument.abstracts.DocumentTypes;
import ro.cst.tsearch.utils.MultipartParameterParser;

/**
 * 
 * @author Oprina George
 * 
 *         Dec 10, 2012
 */
public class DocTypeServlet extends HttpServlet {

	private static final long	serialVersionUID	= 1L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		MultipartParameterParser mpp = new MultipartParameterParser(request);

		String actionType = mpp.getMultipartStringParameter("actionType");

		if ("reload".equals(actionType)) {
			DocumentTypes.loadDocType();
			response.getWriter().write("Doctype reloaded!");
			return;
		}
		
		int state = mpp.getMultipartIntParameter(SearchAttributes.P_STATE, 0);

		if (state == 0) {
			response.getWriter().write("No state selected for doctype!");
			return;
		}

		String stateAvrev = "";

		if (state == -1) {
			stateAvrev = "XX";
		} else if (state == -2) {
			stateAvrev = "DEFAULT";
		} else {
			try {
				stateAvrev = State.getState(state).getStateAbv();
			} catch (BaseException e1) {
				e1.printStackTrace();
			}
		}

		if ("download".equals(actionType)) {
			try {
				String fileName = BaseServlet.REAL_PATH + File.separator + ServerConfig.getDoctypeFilePath() + File.separator + "doctypeFor" + stateAvrev
						+ ".xml";

				response.setContentType("Content-Type=application/x-xpinstall");
				response.setHeader("Content-Disposition", " inline; filename=\"doctypeFor" + stateAvrev + ".xml\"");

				File in = new File(fileName);

				if (!in.exists()) {
					fileName = BaseServlet.REAL_PATH + File.separator + ServerConfig.getDoctypeFilePath() + File.separator + "doctypeDEFAULT.xml";
					in = new File(fileName);
				}

				IOUtils.copy(new FileInputStream(in), response.getOutputStream());
				response.setContentLength(new Long(in.length()).intValue());
				return;

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if ("upload".equals(actionType)) {
			try {
				File file = (File) mpp.getFileParameters("doctypefile").elementAt(0);

				if (file == null) {
					response.getWriter().write("Error uploading file! for " + stateAvrev);
					return;
				}

				// checkFile;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PrintWriter pw = new PrintWriter(baos);

				boolean result = DocTypeReader.checkGeneratedDocType(file, pw);

				if (!result) {
					response.getWriter().write(
							"Doctype for state " + stateAvrev + " contains errors.\n" + IOUtils.toString(new ByteArrayInputStream(baos.toByteArray())));
					return;
				}

				String docName = "doctypeFor" + stateAvrev + ".xml";
				if("DEFAULT".equals(stateAvrev)) {
					docName = "doctype" + stateAvrev + ".xml";
				}
				String path = BaseServlet.REAL_PATH + File.separator + ServerConfig.getDoctypeFilePath() + File.separator;

				// create a backup
				File oldDoctype = new File(path + docName);

				if (oldDoctype.exists()) {
					IOUtils.copy(new FileInputStream(oldDoctype), new FileOutputStream(new File(path + "_backup" + System.currentTimeMillis() + docName)));
				}

				// upload file in web directory only and load it
				IOUtils.copy(new FileInputStream(file), new FileOutputStream(new File(path + docName)));

				// load file
				DocumentTypes.loadDocType();

				response.getWriter().write("File uploaded successfully! for " + stateAvrev);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
