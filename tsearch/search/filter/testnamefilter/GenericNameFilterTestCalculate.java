package ro.cst.tsearch.search.filter.testnamefilter;

import java.io.IOException;
import ro.cst.tsearch.Search;
import java.math.BigDecimal;
import com.Ostermiller.util.CSVParser;

import org.apache.commons.fileupload.servlet.*;
import org.apache.commons.fileupload.disk.*;
import org.apache.commons.fileupload.*;
import java.io.PrintWriter;
import org.apache.commons.io.FilenameUtils;
import java.io.InputStream;
import java.util.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import ro.cst.tsearch.search.filter.newfilters.name.GenericNameFilter;
import ro.cst.tsearch.search.filter.testnamefilter.GenericNameFilterTest;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.BaseException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.Class;
import javax.servlet.http.HttpSession;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

public class GenericNameFilterTestCalculate extends BaseServlet {

	public void doRequest(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		User currentUser = (User) request.getSession().getAttribute(
				SessionParams.CURRENT_USER);
		if (currentUser.getUserAttributes().isTSAdmin()) {
			String m = request.getMethod().toUpperCase();
			if (m.equals("GET"))
				doGet(request, response);
			else if (m.equals("POST"))
				doPost(request, response);
		} else {
			// we should
			throw new ServletException(
					GenericNameFilterTestConf.ERROR_NO_TSADMIN);
		}
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		// we remove the names bean from session
		HttpSession s = request.getSession();
		s.removeAttribute("names");
		s.removeAttribute("results");
		// if the method is get, then I have no input from my form
		String searchId = (String) request
				.getAttribute(RequestParams.SEARCH_ID);
		if (searchId == null)
			searchId = Integer.toString(Search.SEARCH_NONE);
		forward(request, response, URLMaping.GenericNameFilter + "?"
				+ RequestParams.SEARCH_ID + "=" + searchId);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		// variables that defines our upload
		// just to debug, remove it in final
		PrintWriter out = null;
		if (GenericNameFilterTestConf.debug)
			out = response.getWriter();
		HttpSession s = request.getSession();
		GenericNameFilterTestBean b = (GenericNameFilterTestBean) s
				.getAttribute("names");
		String reference[] = null;
		Vector<String[]> candidates = null;
		GenericNameFilterTestError errorMessage = new GenericNameFilterTestError();
		Vector<GenericNameFilterTestResultBean> results = new Vector();
		b.reset();
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);

		debug(out, "Is form multipart:" + Boolean.toString(isMultipart));
		// upload the file and save/compute the parameters
		if (isMultipart) {
			// start getting the file
			DiskFileItemFactory factory = new DiskFileItemFactory();
			factory.setSizeThreshold(GenericNameFilterTestConf.maxThreshold);
			ServletFileUpload upl = new ServletFileUpload(factory);
			// Set overall request size constraint
			upl.setSizeMax(GenericNameFilterTestConf.maxFileLength);
			// Parse the request
			try {
				List items = upl.parseRequest(request);
				// Process the uploaded items
				Iterator iter = items.iterator();
				while (iter.hasNext()) {
					FileItem item = (FileItem) iter.next();
					if (item.isFormField()) {
						// do the non file fields
						try {
							String fN = item.getFieldName();
							String fV = item.getString();
							boolean parse = false;
							if (!fN.equals(RequestParams.SEARCH_ID)
									&& !fN.equals("getscores")) {
								if (fN.equals("fileList")) {
									parse = GenericNameFilterValidate
											.validateBigDecimal(fV);
								} else {
									if (fN.equals("generateAranjaments")) {
										parse = GenericNameFilterValidate
												.validateOnCheck(fV);
									} else {
										parse = GenericNameFilterValidate
												.validateName(fV);
									}
								}

								if (parse)
									parseNonFile(b, fN, fV);
								else
									errorMessage
											.setError(GenericNameFilterTestConf.ERROR_FIELD_NAMES);
							}
						} catch (Exception e) {
							debug(out, "ParseNonFile: " + e.getMessage());
							errorMessage.setError(e.getMessage());
						}
					} else {
						if (item.getSize() > 0) {
							String contentType = item.getContentType();
							if (contentType.equals("text/csv")
									|| contentType.equals("text/plain")) {
								try {
									candidates = parseCSV(item.getInputStream());
								} catch (Exception e) {
									throw new ServletException(e.getMessage());
								}
								if (candidates != null) {
									if (GenericNameFilterTestConf.debug) {
										Iterator i = candidates.iterator();
										String t = "<table>";
										while (i.hasNext()) {
											t += "<tr>";
											String c[] = (String[]) i.next();
											for (int j = 0; j < c.length; j++) {
												t += "<td>" + c[j] + "</td>";
											}
											t += "</tr>";
										}
										t += "<table>";
										debug(out, "File content: " + t);
									}
									candidates = GenericNameFilterValidate
											.validateCandidatesFile(candidates);
									boolean validFile = (candidates != null && candidates
											.size() > 0);
									debug(out, "Validate file: "
											+ Boolean.toString(validFile));
									if (validFile) {
										try {
											Date now = new Date(System
													.currentTimeMillis());
											DBManager.saveFile(item
													.getInputStream(),
													FilenameUtils.getName(item
															.getName())
															+ "-" + now);
										} catch (BaseException e) {
											throw new IOException(e
													.getMessage());
										}
									} else {
										errorMessage
												.setError(GenericNameFilterTestConf.ERROR_BAD_FILE);
									}
								}
							} else {
								errorMessage
										.setError(GenericNameFilterTestConf.ERROR_CSV_ONLY);
							}
						}
					}
				}
			} catch (FileUploadException e) {
				errorMessage.setError(GenericNameFilterTestConf.ERROR_TOO_BIG);
			}
		}

		// I'm not sure if I need to set the content type, I got mixed-results
		// during testing
		response.setHeader("content-type", "text/html");
		Vector fL = b.getFileList();
		if (!fL.isEmpty()) {
			// get the files from database
			Iterator i = fL.iterator();
			while (i.hasNext())
				try {
					candidates = addDBFile(candidates, (BigDecimal) i.next());
				} catch (Exception e) {
					throw new IOException(e.getMessage());
					// debug(out, e.getMessage());
				}
		}
		// check if enough data are filled
		if ((b.getRef_first().length() + b.getRef_middle().length() + b
				.getRef_last().length()) == 0) {
			errorMessage.setError(GenericNameFilterTestConf.ERROR_REF);
		} else {
			reference = new String[3];
			reference[0] = b.getRef_first();
			reference[1] = b.getRef_middle();
			reference[2] = b.getRef_last();
		}

		if (candidates == null || candidates.size() == 0) {
			if ((b.getCand_first().length() + b.getCand_middle().length() + b
					.getCand_last().length()) == 0) {
				errorMessage.setError(GenericNameFilterTestConf.ERROR_CAND);
			} else {
				candidates = new Vector<String[]>();
				candidates.add(new String[] { b.getCand_first(),
						b.getCand_middle(), b.getCand_last() });
			}
		}

		if (!errorMessage.isError()) {
			GenericNameFilterTest filter = new GenericNameFilterTest(12);
			Iterator i = candidates.iterator();
			boolean generateAranjaments = b.getGenerateAranjaments().equalsIgnoreCase("on");
			while (i.hasNext()) {
				String[] j = (String[]) i.next();
				GenericNameFilterTestResultBean br = new GenericNameFilterTestResultBean();
				br.setFirstName(j[0]);
				br.setLastName(j[2]);
				br.setMiddleName(j[1]);
				
				String name			= br.getFirstName() + " " + br.getMiddleName() + " " + br.getLastName();
				//sometimes we receive bad information from parser
				double doNotTrustParser = 0;
				if( StringUtils.isEmpty(name) || ( StringUtils.isEmpty(br.getFirstName()) && StringUtils.isEmpty(br.getMiddleName()) && br.getLastName().length()<=3) ){
					doNotTrustParser = 0;
				} else {
					String[] cand = name.split("[ ,-]+");
					if (cand.length > 3) {
						cand = GenericNameFilter.removeEmptyStringsCand( cand );
					}
					doNotTrustParser = filter.calculateScore(reference,
							cand, generateAranjaments);
				}
				
				
				double doTrustTokenization = filter.calculateScore(reference,
						j, generateAranjaments);
				String res = Double.toString(doNotTrustParser>doTrustTokenization?doNotTrustParser:doTrustTokenization);
				int endIndex = GenericNameFilterTestConf.maxDecimals;
				endIndex = endIndex > res.length() ? res.length() : endIndex;
				br.setResult(res.substring(0, endIndex));
				results.add(br);
			}
		}
		// maybe I shouldn't display default results if none are computed.
		b.setError(errorMessage.toString());
		s.setAttribute("names", b);
		s.setAttribute("results", results);

		debug(out, "Content-type: " + request.getContentType());
		// out.print(System.getProperty("java.class.path"));
		if (!GenericNameFilterTestConf.debug)
			forward(request, response, URLMaping.GenericNameFilter);
		// response.sendRedirect(URLMaping.path + URLMaping.GenericNameFilter);

	}

	private void debug(PrintWriter out, String m) {
		if (GenericNameFilterTestConf.debug)
			out.print("<br>" + m);
	}

	private void parseNonFile(GenericNameFilterTestBean b, String fn, String fv)
			throws Exception {

		try {
			Method m = b.getClass().getMethod(setterName(fn),
					new Class[] { "".getClass() });
			m.invoke(b, new Object[] { new String(fv.trim()) });
		} catch (NoSuchMethodException e) {
			throw new Exception(GenericNameFilterTestConf.ERROR_WRONG_FIELD);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private String setterName(String htmlName) {
		return "set" + Character.toString(htmlName.charAt(0)).toUpperCase()
				+ htmlName.substring(1);
	}

	public Vector<String[]> addDBFile(Vector<String[]> c, BigDecimal n)
			throws Exception {
		// read file from database
		try {
			if (c == null) {
				c = new Vector<String[]>();
			}
			c.addAll(GenericNameFilterValidate
					.validateCandidatesFile(parseCSV(new ByteArrayInputStream(
							DBManager.getTestFileContentsFromDb(n)))));
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
		return c;
	}

	public Vector<String[]> parseCSV(InputStream in) throws Exception {
		CSVParser csvp = new CSVParser(in, GenericNameFilterTestConf.separator);
		Vector<String[]> c = null;
		try {
			c = new Vector<String[]>(Arrays.asList(csvp.getAllValues()));
		} catch (IOException e) {
			throw new Exception(e.getMessage());
		}
		return c;
	}
}
