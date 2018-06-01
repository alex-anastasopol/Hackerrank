package ro.cst.tsearch.reports.invoice;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JExcelApiExporter;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;
import ro.cst.a2pdf.Html2Pdf;
import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.InvoiceATS2FABean;
import ro.cst.tsearch.bean.InvoiceSolomonBean;
import ro.cst.tsearch.bean.InvoiceSolomonBeanEntry;
import ro.cst.tsearch.data.GenericState;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBInvoice;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.emailClient.EmailClient;
import ro.cst.tsearch.exceptions.ServerResponseException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.generic.template.InvoiceDetailsTemplate;
import ro.cst.tsearch.generic.template.InvoiceTemplate;
import ro.cst.tsearch.reports.data.DayReportLineData;
import ro.cst.tsearch.reports.data.DiscountData;
import ro.cst.tsearch.reports.data.InvoiceData;
import ro.cst.tsearch.reports.data.InvoiceXmlData;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.settings.Settings;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TSOpCode;
import ro.cst.tsearch.utils.URLMaping;

public class InvoiceServlet extends BaseServlet {

	/**
	 */
	private static final long serialVersionUID = 1L;

	private final String SEND_BY_EMAIL_SERVLET = URLMaping.EMAIL;
	private final String MULTIPLE_EMAIL_PAGE = URLMaping.MULTIPLE_EMAIL;
	public static final String ACTION_FROM_INVOICE_SERVLET = "from_invoice_servlet";
	public static final String ACTION_MULTIPLE_AGENT_INVOICES = "multiple_agent_invoices";
	public static final String ACTION_MULTIPLE_AGENT_INVOICES_CLOSE = "multiple_agent_invoices_close";
	public static final String ACTION_MULTIPLE_AGENT_INVOICES_NO_SEARCHES = "multiple_agent_invoices_no_searches";

	public void doRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		HttpSession session = request.getSession(true);
		User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		UserAttributes ua = currentUser.getUserAttributes();

		boolean isTSAdmin = false;
		boolean isCommAdmin = false;
		
		Vector<Long> agentIds = new Vector<Long>();
		Vector<Long> sendEmailAgentIds = new Vector<Long>();
		boolean sendToAll = request.getParameter("sendToAll")!=null && request.getParameter("sendToAll").equals("1");
		if(!sendToAll) agentIds.add(-1L);
		
		try {
			isTSAdmin = UserUtils.isTSAdmin(ua);
			isCommAdmin = UserUtils.isCommAdmin(ua) || UserUtils.isTSCAdmin(ua);
		} catch (Exception e) {}

		int opCode = -1;

		int yearReport, monthReport, dayReport, fromDay, fromMonth, fromYear, toDay, toMonth, toYear;
		String reqParam;
		boolean isIntervalInvoice = true;
		fromDay = fromMonth = fromYear = toDay = toMonth = toYear = -1;

		// setting current time
		Calendar c = Calendar.getInstance();
		yearReport = c.get(Calendar.YEAR);
		monthReport = c.get(Calendar.MONTH) + 1;
		dayReport = c.get(Calendar.DAY_OF_MONTH);

		reqParam = request.getParameter(TSOpCode.OPCODE);
		try {
			opCode = Integer.parseInt(reqParam);
		} catch (Exception e) {
			e.printStackTrace();
		}

		int commId = -1;
		String commIdStr = (String) request.getSession().getAttribute("commId");
		try {
			commId = Integer.parseInt(commIdStr);
		} catch (Exception e) {
			e.printStackTrace();
		}

		
		reqParam = request.getParameter(RequestParams.REPORTS_SEARCH_ALL);
		if("true".equalsIgnoreCase(reqParam)) {
			isIntervalInvoice = true;
			fromDay = 1;
			fromMonth = 1;
			fromYear = 2000;
			toDay = dayReport;
			toMonth = monthReport;
			toYear = yearReport;
		} else {
		
			// reading from request parameters specific to the type of invoice
			reqParam = request.getParameter(RequestParams.INVOICE_FROM_DAY);
			if (StringUtils.isEmpty(reqParam) ) {
				isIntervalInvoice = false;
			}
			if (isIntervalInvoice) {
				fromDay = Integer.parseInt(reqParam);
				fromMonth = Integer.parseInt(request.getParameter(RequestParams.INVOICE_FROM_MONTH));
				fromYear = Integer.parseInt(request.getParameter(RequestParams.INVOICE_FROM_YEAR));
				toDay = Integer.parseInt(request.getParameter(RequestParams.INVOICE_TO_DAY));
				toMonth = Integer.parseInt(request.getParameter(RequestParams.INVOICE_TO_MONTH));
				toYear = Integer.parseInt(request.getParameter(RequestParams.INVOICE_TO_YEAR));
			} else {
				dayReport = Integer.parseInt(request.getParameter(RequestParams.REPORTS_DAY));
				monthReport = Integer.parseInt(request.getParameter(RequestParams.REPORTS_MONTH));
				yearReport = Integer.parseInt(request.getParameter(RequestParams.REPORTS_YEAR));
			}
		
		}

		if (opCode == TSOpCode.INVOICE_EMAIL) {

			// checking if it is an individual or community invoice
			reqParam = request.getParameter(RequestParams.REPORTS_AGENT);
			int[] agentId = { -1 };
			if (reqParam != "" && reqParam != null)
				try {
					agentId[0] = Integer.parseInt(reqParam.trim());
				} catch (Exception e) {
				}

			// reading parameters for ordering
			String orderField = "TSR", orderType = "asc";
			reqParam = request.getParameter(RequestParams.REPORTS_ORDER_BY);
			if (!reqParam.equals("@@orderBy@@") && reqParam != null)
				orderField = reqParam;
			reqParam = request.getParameter(RequestParams.REPORTS_ORDER_TYPE);
			if (!reqParam.equals("@@orderType@@") && reqParam != null)
				orderType = reqParam;

			String invFileName = "INVOICE";
			// append date/time to file name
			invFileName += "_" + new SimpleDateFormat(FormatDate.PATTERN_MMddyyyy_HHmmss).format(new Date());
			String displayInvFileName = invFileName;
			
			String dirPath = ServerConfig.getInvoiceCreationPath()
					+ File.separator;
			File dirFile = new File(dirPath);
			if(!dirFile.exists()) {
				dirFile.mkdirs();
			}
			// append prefix and user directory to file name
			invFileName = dirPath + invFileName;

			// ---------discount area--------------
			/**
			 * The discount goes like this: - first we determine if we can apply
			 * a discount -> this means discount value si greater that 0 - if we
			 * can apply a discount we compute the discountRatio and set a
			 * flag(applyDiscount) to true - if applyDiscount is true, we call a
			 * function in DBManager that gets all searches that will be
			 * invoiced - having those searches already sorted by finish date,
			 * we will determine the ones that need to be discounted - we will
			 * apply discount if needed
			 */

			boolean applyDiscount = false;
			reqParam = request.getParameter(RequestParams.INVOICE_DISCOUNT_VALUE);
			float discountRatio = 1;
			float discountValue = 0;

			if (reqParam != null) { // this means the discount value exists
				try {
					discountValue = Float.parseFloat(reqParam);
				} catch (Exception e) {
					logger.error("Not using discount value because of: "
							+ e.getMessage());
				}
				if (discountValue > 0) {
					// i will apply discount in the folowing way
					// i will set a field in the TS_SEARCH table discount_ratio
					// this field will pe multiplyed with the price and will
					// result in a new value
					// so the whole deal is to compute this ratio
					// intriguing right?!?!!
					discountRatio = (100 - discountValue) / 100;
					// this is deffensive against discountValues over 100%
					if (discountRatio >= 0) {
						applyDiscount = true;
					}
				}
			} // else we do nothing

			long[] searchIds = new long[0];
			int[] temp = { -1 };
			String[] tempString = { "-1" };
			Calendar ctemp = Calendar.getInstance();
			ctemp.set(Calendar.DAY_OF_MONTH, 1);
			ctemp.set(Calendar.MONTH, monthReport - 1);
			ctemp.set(Calendar.YEAR, yearReport);

			if (applyDiscount) {
				if (isIntervalInvoice)
					searchIds = DBManager.getSearchesFromIntervalInvoiced(temp,
							agentId, agentId, temp, tempString, fromDay,
							fromMonth, fromYear, toDay, toMonth, toYear,
							commId, (isTSAdmin) ? 1 : 0);
				else {
					reqParam = request
							.getParameter(RequestParams.INVOICE_PAGE_NAME);
					if (reqParam != null
							&& reqParam.contains(URLMaping.INVOICE_DAY)) {
						searchIds = DBManager.getSearchesFromIntervalInvoiced(
								temp, agentId, agentId, temp, tempString,
								dayReport, monthReport, yearReport, dayReport,
								monthReport, yearReport, commId,
								(isTSAdmin) ? 1 : 0);
					} else
						searchIds = DBManager.getSearchesFromIntervalInvoiced(
								temp, agentId, agentId, temp, tempString, 1,
								monthReport, yearReport,
								ctemp.getActualMaximum(Calendar.DAY_OF_MONTH),
								monthReport, yearReport, commId,
								(isTSAdmin) ? 1 : 0);
				}
				// now lets determine how many searches will will need to
				// discount
				reqParam = request.getParameter(RequestParams.INVOICE_DISCOUNT_NO_SEARCHES);

				int maxDiscounts = searchIds.length;

				if (reqParam != null) {
					try {
						maxDiscounts -= Integer.parseInt(reqParam);
						if (maxDiscounts<0) maxDiscounts=0;
					} catch (Exception e) {
						e.printStackTrace();
						maxDiscounts = 0; // nobody to discount
					}

				}
				String[] searchesToBeDiscounted = new String[maxDiscounts];
				for (int i = 0; i < searchesToBeDiscounted.length; i++) {
					searchesToBeDiscounted[i] = String.valueOf(searchIds[i]);
				}
				// create the object that will be stored on the session to
				// maintain last discount state
				DiscountData discData = null;

				// apply discount only if the number of searches surpass the
				// limit set in page
				if (maxDiscounts > 0) {
					int rowsAffected = DBManager.updateDiscountRatio(Util
							.getStringsList(searchesToBeDiscounted, ","),
							discountRatio);
					if (rowsAffected > 0) {
						// setting discountData only if there were some rows
						// updated
						discData = new DiscountData();
						discData.setCommId(commId);
						discData.setPage(request
								.getParameter(RequestParams.INVOICE_PAGE_NAME));
						discData.setDiscountValue(discountValue);
						if (isIntervalInvoice) {
							discData.setFromDay(fromDay);
							discData.setFromMonth(fromMonth);
							discData.setFromYear(fromYear);

							discData.setToDay(toDay);
							discData.setToMonth(toMonth);
							discData.setToYear(toYear);
						} else {
							reqParam = request
									.getParameter(RequestParams.INVOICE_PAGE_NAME);
							if (reqParam != null
									&& reqParam.contains(URLMaping.INVOICE_DAY)) {
								discData.setFromDay(dayReport);
								discData.setToDay(dayReport);
							} else {
								discData.setFromDay(1);
								discData
										.setToDay(ctemp
												.getActualMaximum(Calendar.DAY_OF_MONTH));
							}
							discData.setFromMonth(monthReport);
							discData.setFromYear(yearReport);

							discData.setToMonth(monthReport);
							discData.setToYear(yearReport);
						}
						discData.setSearches(searchesToBeDiscounted);
					}
				}
				session.setAttribute(SessionParams.INVOICE_DISCOUNT_LAST,
						discData);

			}

			// ------------- end discount area---------------

			HashSet<InvoicedSearch> listChk = new HashSet<InvoicedSearch>();;
			Vector<String> sendFormats = new Vector<String>();
			StringBuffer toBeDiscounted = new StringBuffer();

			HashSet<InvoicedSearch> listCheckPdf = sendPdf(request, invFileName,
					isIntervalInvoice, agentId, fromDay, fromMonth, fromYear,
					toDay, toMonth, toYear, dayReport, monthReport, yearReport,
					orderField, orderType, commId, c, isTSAdmin, isCommAdmin,
					toBeDiscounted,sendToAll,agentIds);
			if (listCheckPdf != null) {
				listChk.addAll(listCheckPdf);
				sendFormats.add(".pdf");
			}

			HashSet<InvoicedSearch> listCheckXmlXls = sendXmlXls(request, invFileName,
					sendFormats, isIntervalInvoice, agentId, fromDay,
					fromMonth, fromYear, toDay, toMonth, toYear, dayReport,
					monthReport, yearReport, orderField, orderType, commId, c,
					isTSAdmin, isCommAdmin, toBeDiscounted,sendToAll,agentIds);

			if (listCheckXmlXls != null && listChk.size() == 0) {
				// update listChk if it is not already set
				listChk.addAll(listCheckXmlXls);
			}

			HashSet<InvoicedSearch> listCheckSolomon = sendSolomon(request,
					invFileName, sendFormats, isIntervalInvoice, agentId,
					fromDay, fromMonth, fromYear, toDay, toMonth, toYear,
					dayReport, monthReport, yearReport, orderField, orderType,
					commId, c, isTSAdmin, isCommAdmin, toBeDiscounted, displayInvFileName.substring("INVOICE_".length()),sendToAll,agentIds);

			if (listCheckSolomon != null && listChk.size() == 0) {
				// update listChk if it is not already set
				listChk.addAll(listCheckSolomon);
			}

			if (isTSAdmin) {
				DBManager.updateSearchesInvoiceStatus(listChk, "invoiced", false);
				DBManager.updateSearchesInvoiceStatus(listChk, "invoice",  true);
			} else if (isCommAdmin) {
				DBManager.updateSearchesInvoiceStatus(listChk, "invoiced_cadm", false);
				DBManager.updateSearchesInvoiceStatus(listChk, "invoice_cadm",  true);
			}
			String emailBox = ua.getEMAIL();
			String text="";
			if(sendToAll) {
				sendEmailAgentIds.addAll(agentIds);
				sendEmailAgentIds.remove(Long.parseLong("-1"));
				try {			
					for(Long agent : agentIds) {
						if(agent <= 0) continue;
						String attachFiles = "";
						for (Iterator<String> iter = sendFormats.iterator(); iter.hasNext();) {
							String format = (String) iter.next();
							attachFiles += invFileName + "_" + agent + format + ",";
						}
			
						attachFiles = attachFiles.substring(0, attachFiles.length() - 1);
						String[] separatorFiles = attachFiles.split(",");
			
						UserAttributes uaAgent = null;
			
						try {
							uaAgent = UserUtils.getUserFromId(agent);
						} catch (Exception e) {
							e.printStackTrace();
							continue;
						}
						
						if(uaAgent.getMyAtsAttributes().getInvoiceEditEmail()==0) {
							sendEmailAgentIds.remove(agent);
						}
						else {
							continue;
						}
						
						String mailTo = "";
						mailTo = uaAgent.getEMAIL();
						
			
						String files;
						String propSubject = "";
			
						for (int nrfile = 0; nrfile < separatorFiles.length; nrfile++) {
							files = separatorFiles[nrfile];
							files = files.substring(files.lastIndexOf(File.separator)
									+ File.separator.length());
							propSubject = propSubject + files + "  ";
						}
						
						EmailClient email=new EmailClient();
						
						email.setFrom(emailBox);
						email.addTo(mailTo);
						email.setSubject(propSubject);
						email.addContent(propSubject);
						for(String file : separatorFiles) 
							email.addAttachment(file,file.substring(file.lastIndexOf(File.separator)+ File.separator.length()),file.substring(file.lastIndexOf(File.separator)+ File.separator.length()));
						
						email.sendAsynchronous();
						logger.info(" Send Mail  started ");			
					}
					//if we have remaining agents to which to send custom email invoices, we redirect to the send email servlet
					if(sendEmailAgentIds.size()>0) {
						String searchId = request.getParameter("searchId");
						String to = "";
						String subject = "";
						try {
							UserAttributes uaFirst = UserUtils.getUserFromId(sendEmailAgentIds.get(0));
							to = uaFirst.getEMAIL();
							subject = "Invoice for " + uaFirst.getUserFullName();
							
							for(String format : sendFormats) {
			       				String file =invFileName + "_" + uaFirst.getID() + format + ", ";
					    		String shortName = file.substring(file.lastIndexOf(File.separator)+ File.separator.length());
			       				text+= shortName + " ";
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
						if(searchId == null) searchId = "-1";
						
						forward(request, response, 
								MULTIPLE_EMAIL_PAGE
									+ "?searchId=" + URLEncoder.encode(searchId, "UTF-8")
									+ "&agentIds=" + URLEncoder.encode(sendEmailAgentIds.toString().replaceAll("[\\[\\]\\s]", ""), "UTF-8")
									+ "&invoice=" + URLEncoder.encode(invFileName, "UTF-8")
									+ "&formats=" + URLEncoder.encode(sendFormats.toString().replaceAll("[\\[\\]\\s]", ""), "UTF-8")
									+ "&subject=" + URLEncoder.encode(subject, "UTF-8")
									+ "&from=" + URLEncoder.encode(emailBox, "UTF-8")
									+ "&to=" + URLEncoder.encode(to, "UTF-8")
									+ "&text=" + URLEncoder.encode(text, "UTF-8")
									+ "&agent=0"
									+ "&" + RequestParams.ACTION + "="
									+ ACTION_MULTIPLE_AGENT_INVOICES
									);
					}
					else 
						forward(request, response, URLMaping.MULTIPLE_EMAIL
								+ "?" + RequestParams.ACTION + "=" 
								+ ((agentIds.size()>0)
									? InvoiceServlet.ACTION_MULTIPLE_AGENT_INVOICES_CLOSE
									: InvoiceServlet.ACTION_MULTIPLE_AGENT_INVOICES_NO_SEARCHES
									)
								);
				}catch(Exception e) {
					e.printStackTrace();
					logger.error("Could not send invoice email");
				}

			}
			else {
				String attachFiles = "";
				for (Iterator<String> iter = sendFormats.iterator(); iter.hasNext();) {
					String format = (String) iter.next();
					attachFiles += invFileName + format + ",";
				}
	
	
	
				attachFiles = attachFiles.substring(0, attachFiles.length() - 1);
				String[] separatorFiles = attachFiles.split(",");
	
				String agentID = request.getParameter("reportAgent");
	
				UserAttributes uaAgent = null;
	
				try {
					uaAgent = UserUtils.getUserFromId(Long.parseLong(agentID));
				} catch (Exception e) {
					e.printStackTrace();
				}
				String mailTo = "";
				if ((Long.parseLong(agentID) > 0) && (agentID != null)) {
					mailTo = uaAgent.getEMAIL();
				}
	
				String files;
				String propSubject = "";
	
				for (int nrfile = 0; nrfile < separatorFiles.length; nrfile++) {
					files = separatorFiles[nrfile];
					files = files.substring(files.lastIndexOf(File.separator)
							+ File.separator.length());
					propSubject = propSubject + files + "  ";
				}
	
				forward(request, response, SEND_BY_EMAIL_SERVLET
						+ "?attachPdfFile=" + attachFiles + "&displayPdfFile="
						+ displayInvFileName + ".pdf" + "&saveSearch=false" + "&"
						+ "sursa" + "=" + "0" + "&ProposeSubject=" + propSubject
						+ "&ProposeFrom=" + emailBox + "&CurPageURL=" + ""
						+ "&ProposeTo=" + mailTo + "&" + RequestParams.ACTION + "="
						+ ACTION_FROM_INVOICE_SERVLET);
			}
		} else if (opCode == TSOpCode.INVOICE_RESET_LAST_DISCOUNT) {
			DiscountData discData = (DiscountData) session
					.getAttribute(RequestParams.INVOICE_DISCOUNT_LAST);

			if (discData != null) {
				boolean discValid = true;
				if (!request.getParameter(RequestParams.INVOICE_PAGE_NAME)
						.contains(discData.getPage())) {
					discValid = false;
				}
				if (discValid && discData.getCommId() != commId) {
					discValid = false;
				}
				if (isIntervalInvoice) {
					if (discData.getToDay() != toDay
							|| discData.getToMonth() != toMonth
							|| discData.getToYear() != toYear
							|| discData.getFromDay() != fromDay
							|| discData.getFromMonth() != fromMonth
							|| discData.getFromYear() != fromYear)
						discValid = false;
				} else {
					reqParam = request
							.getParameter(RequestParams.INVOICE_PAGE_NAME);
					if (reqParam != null
							&& reqParam.contains(URLMaping.INVOICE_DAY)) {
						// must check for exact "toDay" :)
						if (discData.getFromDay() != dayReport
								|| discData.getFromMonth() != monthReport
								|| discData.getFromYear() != yearReport)
							discValid = false;
					} else {
						// must check only month and year
						if (discData.getFromMonth() != monthReport
								|| discData.getFromYear() != yearReport)
							discValid = false;
					}
				}
				if (discValid) {
					logger.error("DiscountData is ok! Lets apply discount.");
					DBManager.resetDiscountRatio(Util.getStringsList(discData
							.getSearches(), ","), 1);
				}
				session.setAttribute(RequestParams.INVOICE_DISCOUNT_LAST, null);

			} else {
				logger.error("DiscountData should not be null!!!");
			}
			String address = "";
			if (isIntervalInvoice) {
				address = URLMaping.INVOICE_INTERVAL + "?searchId="
						+ request.getParameter("searchId");
			} else {
				reqParam = request
						.getParameter(RequestParams.INVOICE_PAGE_NAME);
				if (reqParam != null
						&& reqParam.contains(URLMaping.INVOICE_DAY)) {
					address = URLMaping.INVOICE_DAY + "?searchId="
							+ request.getParameter("searchId");
				} else
					address = URLMaping.INVOICE_MONTH_DETAILED + "?searchId="
							+ request.getParameter("searchId");
			}
			forward(request, response, address);
		} else {
			// nothing
			logger.error("OpCode unknown: " + opCode);
		}

	}

	private HashSet<InvoicedSearch> sendPdf(HttpServletRequest request, String emailFileName,
			boolean isIntervalInvoice, int[] agentId, int fromDay,
			int fromMonth, int fromYear, int toDay, int toMonth, int toYear,
			int dayReport, int monthReport, int yearReport, String orderField,
			String orderType, int commId, Calendar c, boolean isTSAdmin,
			boolean isCommAdmin, StringBuffer toBeDiscounted, boolean sendToAll, Vector<Long> agentIds) {
		String sendFormPdf = request
				.getParameter(RequestParams.INVOICE_SEND_FORM_PDF);
		if ("".equals(sendFormPdf)) {
			return null;
		}

		HttpSession session = request.getSession(true);
		User currentUser = (User) session
				.getAttribute(SessionParams.CURRENT_USER);

		
		UserAttributes ua = currentUser.getUserAttributes();
		DayReportLineData[] ReportData = new DayReportLineData[0];

		int[] temp = { -1 };
		String[] tempString = { "-1" };
		// checking if is interval or monthly invoice and getting data
		if (!isIntervalInvoice) {
			fromDay = 1;
			fromMonth = monthReport;
			fromYear = yearReport;
			Calendar lastDayCal = Calendar.getInstance();
			lastDayCal.set(Calendar.MONTH, monthReport - 1);
			lastDayCal.set(Calendar.YEAR, yearReport);
			toDay = lastDayCal.getActualMaximum(Calendar.DATE);
			toMonth = monthReport;
			toYear = yearReport;
			
		}
		
		ReportData = DBInvoice.getIntervalInvoiceData(temp, temp,
				agentId, temp, tempString,
				fromDay, fromMonth, fromYear, 
				toDay, toMonth, toYear, 
				orderField,	orderType, commId, isTSAdmin?1:0, ua);
		
		/*
		List<DayReportLineData> newLines = new ArrayList<DayReportLineData>();
		for (DayReportLineData dayReportLineData : ReportData) {
			if(dayReportLineData.getFeeAsDouble() > 0) {
				newLines.add(dayReportLineData);
			}
		}
		ReportData = newLines.toArray(new DayReportLineData[newLines.size()]);
		*/
		
		if(sendToAll) 
			for(DayReportLineData d : ReportData) 
				if(!agentIds.contains(d.getAgentId())) agentIds.add(d.getAgentId());

		String originalFileName = emailFileName;
		for(Long agent : agentIds) {
			emailFileName = originalFileName + ((sendToAll)?("_" + agent):"");
			if(sendToAll && agent <= 0) continue;	//no agent
		// details converting to html
		int checkbox = 0;
		try {
			checkbox = Settings.selectAttributes(Settings.SETTINGS_MODULE,
					Settings.INVOICE_DETAILS, ua.getID());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("InvoiceServlet exception: " + e.getMessage());
		}
		String detailsHTML = "";
		String GMTOffset = "";
		if (ReportData.length > 0) {
			GMTOffset = "<BR>( GMT " + ReportData[0].getTimeZone() + " )";
		}

		// creating the details of the invoice
		if (checkbox == 1 || !isTSAdmin) {
			detailsHTML = "<TABLE borderColor=#a5a2a5 cellSpacing=0 cellPadding=0 width='98%' border=1>"
					+ "<TR class=headerSubDetailsRow>"
					+ "<TD align='center'>Abstractor</TD>"
					+ "<TD align='center'>Owner</TD>"
					+ "<TD align='center'>Agent</TD>"
					+ "<TD align='center'>County</TD>"
					+ "<TD align='center'>Property Address</TD>"
					+ "<TD align='center'>TS Done"
					+ GMTOffset
					+ "</TD>"
					+ "<TD align='center'>File ID</TD>"
				//	+ "<TD align='center'>TSR Sent To</TD>"
				//	+ "<TD align='center'>Product</TD>"
					+ "<TD align='center'>Fee</TD>" 
					+ "<TD align='center'>DS (Image#)</TD>" 
					+ "</TR>";
			
			InvoiceDetailsTemplate templDetails = new InvoiceDetailsTemplate();
			
			for (int i = 0; i < ReportData.length; i++) {
				if(sendToAll && ReportData[i].getAgentId() != agent) continue;
				templDetails.setData(ReportData[i]);
				try {
					detailsHTML += templDetails
							.getTemplateAfterReplacements(REAL_PATH
									+ File.separator + "WEB-INF"
									+ File.separator + "classes"
									+ File.separator + "ro" + File.separator
									+ "cst" + File.separator + "tsearch"
									+ File.separator + "reports"
									+ File.separator + "templates"
									+ File.separator
									+ "invoiceDetailsRow.template");
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			detailsHTML += "</TABLE>";
		}

		InvoiceData invData = getSummaryFrom(ReportData, commId, sendToAll?agent:agentId[0], ua, sendToAll);
		
		invData.setInvoiceTimestamp(new Date(System.currentTimeMillis()));
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, fromMonth - 1);
        cal.set(Calendar.YEAR, fromYear);
        cal.set(Calendar.DAY_OF_MONTH, fromDay);
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        invData.setStartInterval(cal.getTime());
        Calendar now = Calendar.getInstance();
        if(cal.after(now)) //if the date is in the future we set end interval at the same date
        	invData.setEndInterval(cal.getTime());
        else {
	        cal.set(Calendar.DAY_OF_MONTH, toDay);
	        cal.set(Calendar.MONTH, toMonth - 1);
	        cal.set(Calendar.YEAR, toYear);
	        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMaximum(Calendar.HOUR_OF_DAY));
	        cal.set(Calendar.MINUTE, cal.getActualMaximum(Calendar.MINUTE));
	        cal.set(Calendar.SECOND, cal.getActualMaximum(Calendar.SECOND));
	        if(cal.after(now))		//if the end date is in the future we set the end date to current date
	        	cal = now;
	        invData.setEndInterval(cal.getTime());
        }
		
		InvoiceTemplate templ = new InvoiceTemplate();

		// set the products
		invData.setInvoiceProduct(commId);

		invData.setDetails(detailsHTML);
		// logo image path
		String logoPath = REAL_PATH + File.separator + "web-resources"
				+ File.separator + "images" + File.separator + "cst_logo.jpg";
		invData.setLogoFile(logoPath);

		templ.setData(invData);

		// create any inexistent directories
		FileUtils.CreateOutputDir(emailFileName);
		String templateFileName = "";
		try {
			templateFileName = REAL_PATH + File.separator + "WEB-INF"
					+ File.separator + "classes" + File.separator + "ro"
					+ File.separator + "cst" + File.separator + "tsearch"
					+ File.separator + "reports" + File.separator + "templates"
					+ File.separator;
			if (isTSAdmin) {
				templateFileName += "invoiceATS.template";
			} else if (isCommAdmin) {
				if (agentId[0] == -1) { // invoice from commAdmin to all....
					templateFileName += "invoiceCommAdmin.template";
				} else { // invoice from commAdmin to an agent...
					templateFileName += "invoiceCommAdminToAgent.template";
				}
			}
			templ.saveTemplateToFile(templateFileName, emailFileName + ".html");

		} catch (Exception e) {
			e.printStackTrace();
		}

		String[] tmpArray = new String[1];
		tmpArray[0] = emailFileName + ".html";
		Html2Pdf conv = new Html2Pdf(tmpArray, emailFileName + ".pdf","");
		conv.execute();
		if(!sendToAll) break;
		}
		// changing searches status to invoiced
		HashSet<InvoicedSearch> listChk = new HashSet<InvoicedSearch>();
		for (int i = 0; i < ReportData.length; i++) {
			InvoicedSearch is = new InvoicedSearch();
			is.setSearchId(ReportData[i].getId());
			
			int currentInvoiceStatus = ReportData[i].getInvoiced();
			boolean searchFinished = ReportData[i].getSearchFlags().isTsrCreated();
			
			if(currentInvoiceStatus == InvoicedSearch.SEARCH_NOT_INVOICED) {
				//was not invoiced
				if(searchFinished) {
					is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_FINISHED);
				} else {
					is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_INDEX);
				}
			} else if(currentInvoiceStatus == InvoicedSearch.SEARCH_INVOICED_INDEX) {
				//was invoiced only as index
				if(searchFinished) {
					is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_INDEX_AND_FINISHED);
				} else {
					is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_INDEX);
				}
			} else if(currentInvoiceStatus == InvoicedSearch.SEARCH_INVOICED_FINISHED) {
				//was invoiced only after was finished
				is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_FINISHED);
			} else if(currentInvoiceStatus == InvoicedSearch.SEARCH_INVOICED_INDEX_AND_FINISHED) {
				//keep state
				is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_INDEX_AND_FINISHED);
			} else {
				//default state - maximum price
				is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_FINISHED);
			}
			listChk.add(is);
		}

		return listChk;
	}

	private InvoiceData getSummaryFrom(DayReportLineData[] reportData,
			int commId, long abstractorId, UserAttributes ua, boolean sendToAll) {
		boolean isTSAdmin = false;
        boolean isCommAdmin = false;
        
        try
        {
            isTSAdmin = UserUtils.isTSAdmin( ua );
            isCommAdmin = UserUtils.isCommAdmin( ua )||UserUtils.isTSCAdmin( ua);
        }catch( Exception e ){}
        
        InvoiceData invoiceTotal = null;
        if(isTSAdmin)
        	invoiceTotal = DBInvoice.getInvoiceRecipientData(commId, abstractorId);
        else if(isCommAdmin){
        	invoiceTotal = DBInvoice.getInvoiceRecipientDataAsCommAdmin(commId, abstractorId);
        }
        
        HashMap<String, HashMap<Double, Integer>> subtotals = new HashMap<String, HashMap<Double,Integer>>();
        
        for (DayReportLineData dayReportLineData : reportData) {
        	if(sendToAll && dayReportLineData.getAgentId() != abstractorId) continue;
        	
        	String productName = dayReportLineData.getProductName();
        	
        	if(dayReportLineData.getDiscountRatio() != 1f && isTSAdmin) {
        		productName += "Discount";
        	}
        	
        	HashMap<Double, Integer> elements = subtotals.get(productName);
        	if(elements == null) {
        		elements = new HashMap<Double, Integer>();
        		subtotals.put(productName, elements);
        	}
        	Integer numberOfSearches = elements.get(dayReportLineData.getFeeAsDouble());
        	if(numberOfSearches == null) {
	        	elements.put(dayReportLineData.getFeeAsDouble(), 1);
        	} else {
        		elements.put(dayReportLineData.getFeeAsDouble(), numberOfSearches + 1);
        	}
		}
        
        invoiceTotal.setSubTotals(subtotals);
        
        return invoiceTotal;
	}

	private HashSet<InvoicedSearch> sendXmlXls(HttpServletRequest request, String invFileName,
			Vector<String> sendFormats, boolean isIntervalInvoice,
			int[] agentId, int fromDay, int fromMonth, int fromYear, int toDay,
			int toMonth, int toYear, int dayReport, int monthReport,
			int yearReport, String orderField, String orderType, int commId,
			Calendar c, boolean isTSAdmin, boolean isCommAdmin,
			StringBuffer toBeDiscounted, boolean sendToAll, Vector<Long> agentIds) {
		String sendFormXml = request
				.getParameter(RequestParams.INVOICE_SEND_FORM_XML);
		String sendFormXls = request
				.getParameter(RequestParams.INVOICE_SEND_FORM_XLS);
		if (sendFormXml == null) {
			sendFormXml = "";
		}
		if (sendFormXls == null) {
			sendFormXls = "";
		}
		if (("".equals(sendFormXml) || (sendFormXml.compareTo("null") == 0))
				&& ("".equals(sendFormXls) || (sendFormXls.compareTo("null") == 0))) {
			return null;
		}

		String reqParam;
		InvoiceXmlData[] invoiceXmlData = new InvoiceXmlData[0];
		int[] temp = { -1 };
		String[] tempString = { "-1" };
		// checking if is interval or monthly invoice and getting data
		
		if (!isIntervalInvoice) {
			fromDay = 1;
			fromMonth = monthReport;
			fromYear = yearReport;
			Calendar lastDayCal = Calendar.getInstance();
			lastDayCal.set(Calendar.MONTH, monthReport - 1);
			lastDayCal.set(Calendar.YEAR, yearReport);
			toDay = lastDayCal.getActualMaximum(Calendar.DATE);
			toMonth = monthReport;
			toYear = yearReport;
			
		}
		
		invoiceXmlData = DBInvoice.getIntervalInvoiceXmlData(
				temp, temp, agentId, temp, tempString, 
				fromDay, fromMonth, fromYear, 
				toDay, toMonth, toYear, 
				orderField, orderType, commId, isTSAdmin?1:0);
		
		/*
		List<InvoiceXmlData> newLines = new ArrayList<InvoiceXmlData>();
		for (InvoiceXmlData entry : invoiceXmlData) {
			if(entry.getSearchFee() > 0) {
				newLines.add(entry);
			}
		}
		invoiceXmlData = newLines.toArray(new InvoiceXmlData[newLines.size()]); 
		*/
		
		Vector<Long> agentIdsVect = new Vector<Long>();
		agentIdsVect.add(-1L);
		
		if(sendToAll) 
			for(InvoiceXmlData d : invoiceXmlData) {
				if(!agentIdsVect.contains(d.getAgentId())) agentIdsVect.add(d.getAgentId());
				if(!agentIds.contains(d.getAgentId())) agentIds.add(d.getAgentId());
			}
		String originalFileName = invFileName;
		Vector<InvoiceATS2FABean> invoiceDataATS2FA = new Vector<InvoiceATS2FABean>();
		for (Long agent : agentIdsVect) {
			invFileName = originalFileName + ((sendToAll) ? ("_" + agent) : "");
			if (sendToAll && agent <= 0)
				continue; // no agent
			invoiceDataATS2FA = new Vector<InvoiceATS2FABean>();
			String[] totals = getInvoiceDataATS2FA(invoiceXmlData, commId, sendToAll, agent, invoiceDataATS2FA);

			StringBuffer sb = new StringBuffer();
			String fromDate, toDate;
			GregorianCalendar gCal = null;
			if (isIntervalInvoice) {
				gCal = new GregorianCalendar(fromYear, fromMonth - 1, fromDay);
				fromDate = fromMonth + "/" + fromDay + "/" + fromYear;

				// gCal = new GregorianCalendar(toYear,toMonth,toDay);
				// toDate = getMinimumDate(gCal,c);
				if (gCal.before(c)) { // if the start interval is in the past
					// gCal.set(Calendar.DAY_OF_MONTH,
					// calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
					if (fromYear == c.get(Calendar.YEAR) && fromMonth == (c.get(Calendar.MONTH) + 1)) {
						// if this is the current month we send end date to the
						// minimum between today and last day of month
						if (c.get(Calendar.DAY_OF_MONTH) <= toDay)
							toDate = toMonth + "/" + c.get(Calendar.DAY_OF_MONTH) + "/" + toYear;
						else
							toDate = toMonth + "/" + toDay + "/" + toYear;
					} else {
						// this is not the current month so we get la whole
						// month
						toDate = toMonth + "/" + toDay + "/" + toYear;
					}
				} else {
					// if the date is in the future we set end interval at start
					// interval
					toDate = fromDate;
					// toDate = monthReport + "/" +
					// calendar.getActualMaximum(Calendar.DAY_OF_MONTH) + "/" +
					// yearReport;
				}

			} else {
				GregorianCalendar calendar = new GregorianCalendar(yearReport, monthReport - 1, 1);
				reqParam = request.getParameter(RequestParams.INVOICE_PAGE_NAME);
				if (reqParam != null && reqParam.contains(URLMaping.INVOICE_DAY)) {
					// dayReport = Integer.parseInt(request.getParameter(
					// RequestParams.REPORTS_DAY ));
					fromDate = monthReport + "/" + dayReport + "/" + yearReport;
					toDate = fromDate;
				} else {
					fromDate = monthReport + "/" + "1" + "/" + yearReport;
					if (calendar.before(c)) { // if the start interval is in the
												// past
						if (yearReport == c.get(Calendar.YEAR) && monthReport == (c.get(Calendar.MONTH) + 1)) {
							// if this is the current month we send end date to
							// the
							// minimum between today and last day of month
							if (c.get(Calendar.DAY_OF_MONTH) <= calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
								toDate = monthReport + "/" + c.get(Calendar.DAY_OF_MONTH) + "/" + yearReport;
							else
								toDate = monthReport + "/" + calendar.getActualMaximum(Calendar.DAY_OF_MONTH) + "/"
										+ yearReport;
						} else {
							// this is not the current month so we get la whole
							// month
							toDate = monthReport + "/" + calendar.getActualMaximum(Calendar.DAY_OF_MONTH) + "/"
									+ yearReport;
						}
					} else {
						// if the date is in the future we set end interval at
						// start
						// interval
						toDate = fromDate;
					}
				}

			}

			// if xml is selected
			if (!"".equals(sendFormXml)) {
				writeToXML(invoiceDataATS2FA, sb, fromDate, toDate);

				PrintWriter pw = null;

				try {
					pw = new PrintWriter(invFileName + ".fai");
					pw.print(sb.toString());
				} catch (Exception e) {
					e.printStackTrace();
					User currentUser = (User) request.getSession().getAttribute(SessionParams.CURRENT_USER);
					EmailClient email = new EmailClient();
					email.addTo(MailConfig.getExceptionEmail());
					email.setSubject("InvoiceServlet.sendXmlXls problem on " + URLMaping.INSTANCE_DIR);
					String content = "General Exception in ro.cst.tsearch.reports.invoice.InvoiceServlet.sendXmlXls: \n"
							+ "CurrentUser: "
							+ currentUser.getUserAttributes().getLOGIN()
							+ " "
							+ currentUser.getUserAttributes().getID()
							+ "\n"
							+ "\n\n Stack Trace: "
							+ e.getMessage()
							+ " \n\n " + ServerResponseException.getExceptionStackTrace(e, "\n") + "\n";
					email.addContent(content);
					email.sendAsynchronous();
				} finally {
					try {
						pw.flush();
						pw.close();
					} catch (Exception e) {
					}
				}
				if (!new File(invFileName + ".fai").exists()) {
					EmailClient email = new EmailClient();
					email.addTo(MailConfig.getExceptionEmail());
					email.setSubject("InvoiceServlet.sendXmlXls problem on " + URLMaping.INSTANCE_DIR);
					String content = "There is no " + invFileName + ".fai on this server\n";
					email.addContent(content);
					email.sendAsynchronous();
				}
				if (!sendFormats.contains(".fai"))
					sendFormats.add(".fai");
			}
			// if xls is selected
			if (!"".equals(sendFormXls)) {
				String templateName = null;
				if (isTSAdmin) {
					templateName = "invoiceXLStemplate_tsadmin.jrxml";
				} else {
					templateName = "invoiceXLStemplate_comm.jrxml";
				}

				// MAKE SURE THE COLUMNS DON'T OVERLAP IN TEMPLATE
				String pathTemplate = REAL_PATH + File.separator + "WEB-INF" + File.separator + "classes"
						+ File.separator + "ro" + File.separator + "cst" + File.separator + "tsearch" + File.separator
						+ "reports" + File.separator + "templates" + File.separator + templateName;
				JasperReport jRep = null;
				JasperPrint jPrint = null;
				Map<String, Object> mapParameter = new HashMap<String, Object>();
				mapParameter.put("InvoiceBlockStartDate", fromDate);
				mapParameter.put("InvoiceBlockEndDate", toDate);
				mapParameter.put("InvoiceTotal", totals[0]);
				mapParameter.put("TotalImages", totals[1]);
				try {
					jRep = JasperCompileManager.compileReport(pathTemplate);
					if (invoiceDataATS2FA.size() == 0) {
						invoiceDataATS2FA.add(new InvoiceATS2FABean("", "", "", "", "", "", "", "", "", "", "", "", "",
								"", "", "", "", "", "", "", "", ""));
					}
					jPrint = JasperFillManager.fillReport(jRep, mapParameter, new JRBeanCollectionDataSource(
							invoiceDataATS2FA));
					JExcelApiExporter excelExporter = new JExcelApiExporter();
					excelExporter.setParameter(JRExporterParameter.JASPER_PRINT, jPrint);
					excelExporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, invFileName + ".xls");
					excelExporter.setParameter(JRXlsAbstractExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS,
							Boolean.TRUE);
					excelExporter.setParameter(JRXlsAbstractExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
					excelExporter.exportReport();

				} catch (Exception e) {
					e.printStackTrace();
				}
				if (!sendFormats.contains(".xls")) {
					sendFormats.add(".xls");
				}
			}
			if (!sendToAll)
				break;
		}

		// changing searches status to invoiced
		HashSet<InvoicedSearch> listChk = new HashSet<InvoicedSearch>();
		for (int i = 0; i < invoiceXmlData.length; i++) {
			InvoicedSearch is = new InvoicedSearch();
			is.setSearchId(invoiceXmlData[i].getId());
			
			int currentInvoiceStatus = invoiceXmlData[i].getInvoiced();
			boolean searchFinished = invoiceXmlData[i].getSearchFlags().isTsrCreated();
			
			if(currentInvoiceStatus == InvoicedSearch.SEARCH_NOT_INVOICED) {
				//was not invoiced
				if(searchFinished) {
					is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_FINISHED);
				} else {
					is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_INDEX);
				}
			} else if(currentInvoiceStatus == InvoicedSearch.SEARCH_INVOICED_INDEX) {
				//was invoiced only as index
				if(searchFinished) {
					is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_INDEX_AND_FINISHED);
				} else {
					is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_INDEX);
				}
			} else if(currentInvoiceStatus == InvoicedSearch.SEARCH_INVOICED_FINISHED) {
				//was invoiced only after was finished
				is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_FINISHED);
			} else if(currentInvoiceStatus == InvoicedSearch.SEARCH_INVOICED_INDEX_AND_FINISHED) {
				//keep state
				is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_INDEX_AND_FINISHED);
			} else {
				//default state - maximum price
				is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_FINISHED);
			}
			listChk.add(is);
		}

		return listChk;
	}

	private HashSet<InvoicedSearch> sendSolomon(HttpServletRequest request, String invFileName,
			Vector<String> sendFormats, boolean isIntervalInvoice,
			int[] agentId, int fromDay, int fromMonth, int fromYear, int toDay,
			int toMonth, int toYear, int dayReport, int monthReport,
			int yearReport, String orderField, String orderType, int commId,
			Calendar c, boolean isTSAdmin, boolean isCommAdmin,
			StringBuffer toBeDiscounted, String invoiceNumber, boolean sendToAll, Vector<Long> agentIds) {
		String sendFormCsv = request
				.getParameter(RequestParams.INVOICE_SEND_FORM_CSV);
		if (sendFormCsv == null || "".equals(sendFormCsv)
				|| "null".equalsIgnoreCase(sendFormCsv)) {
			return null;
		}

		HttpSession session = request.getSession(true);
		User currentUser = (User) session
				.getAttribute(SessionParams.CURRENT_USER);
		
		int[] temp = { -1 };
		String[] tempString = { "-1" };
		// checking if is interval or monthly invoice and getting data
		
		if(!isIntervalInvoice) {
			fromDay = 1;
			fromMonth = monthReport;
			fromYear = yearReport;
			
			Calendar cal = new GregorianCalendar(fromYear, fromMonth - 1, fromDay);			
			toDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
			toMonth = monthReport;
			toYear = yearReport;
			
		}
		
		Vector<InvoiceSolomonBean> invoiceSolomonData = null;
		
		if (isTSAdmin) {
			invoiceSolomonData = DBInvoice.getIntervalInvoiceSolomonData(temp,
					temp, agentId, temp, tempString, fromDay, fromMonth,
					fromYear, toDay, toMonth, toYear, orderField,
					orderType, commId, 1, invoiceNumber, currentUser);
		} else if (isCommAdmin) {
			invoiceSolomonData = DBInvoice.getIntervalInvoiceSolomonData(temp,
					temp, agentId, temp, tempString, fromDay, fromMonth,
					fromYear, toDay, toMonth, toYear, orderField,
					orderType, commId, 0, invoiceNumber, currentUser);
		}
		
		
		StringBuffer sb = null;
		HashSet<InvoicedSearch> listChk = new HashSet<InvoicedSearch>();
		// if xml is selected
		if (!"".equals(sendFormCsv)) {
			
			Vector<Long> agentIdsVect = new Vector<Long>();
			agentIdsVect.add(-1L);

			if(sendToAll) 
				for(InvoiceSolomonBean d : invoiceSolomonData) {
					if(!agentIdsVect.contains(d.getAgentId())) agentIdsVect.add(d.getAgentId());
					if(!agentIds.contains(d.getAgentId())) agentIds.add(d.getAgentId());
				}
			
			String originalFileName = invFileName;
			for(Long agent : agentIdsVect) {
				if(sendToAll && agent <= 0) continue;	//no agent
				invFileName = originalFileName + ((sendToAll)?("_" + agent):"");
				sb = new StringBuffer();
				listChk.addAll(writeToCSV(invoiceSolomonData, sb, sendToAll, agent));
	
				PrintWriter pw = null;
	
				try {
					pw = new PrintWriter(invFileName + ".csv");
					pw.print(sb.toString());
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						pw.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if(!sendToAll) break;
			}
			sendFormats.add(".csv");
		}

		return listChk;
	}

	private String removeSpecialCharacters(String input) {
		if(input == null)
			return "";
		input = input.replaceAll("&amp;", "&");
		input = input.replaceAll("&", "&amp;");
		input = input.replaceAll("<", "&lt;");
		input = input.replaceAll(">", "&gt;");
		input = input.replaceAll("'", "&#39;");
		input = input.replaceAll("\"", "&quot;");
		return input;
	}

	/**
	 * Writes data from the InvoiceATS2FABean Vector to the StringBuffer sb
	 * 
	 * @param data
	 *            vector containing data for the attachment
	 * @param sb
	 *            StringBuffer used to write data for the attachment
	 * @param fromDate
	 *            the beginning of the interval
	 * @param toDate
	 *            the end of the interval
	 */
	private void writeToXML(Vector<InvoiceATS2FABean> data, StringBuffer sb,
			String fromDate, String toDate) {
		sb.append("<ATSInvoices>\r\n");
		sb.append("\t<InvoiceBlockStartDate>" + fromDate
				+ "</InvoiceBlockStartDate>\r\n");
		sb.append("\t<InvoiceBlockEndDate>" + toDate
				+ "</InvoiceBlockEndDate>\r\n");
		for (Iterator<InvoiceATS2FABean> iter = data.iterator(); iter.hasNext();) {
			InvoiceATS2FABean elem = (InvoiceATS2FABean) iter.next();
			sb.append("\t<FAMemoSaleInvoice>\r\n");
			sb.append("\t\t<PartyBranch>" + elem.getStateAbbr_Property() + "_"
					+ elem.getCounty_Property() + "</PartyBranch>\r\n");
			sb.append("\t\t<FileNum>" + elem.getFileNum() + "</FileNum>\r\n");
			sb.append("\t\t<Party_Customer>" + elem.getParty_Customer()
					+ "</Party_Customer>\r\n");
			sb.append("\t\t<AdrsLines_Customer>" + elem.getAdrsLines_Customer()
					+ "</AdrsLines_Customer>\r\n");
			sb.append("\t\t<City_Customer>" + elem.getCity_Customer()
					+ "</City_Customer>\r\n");
			sb.append("\t\t<StateAbbr_Customer>" + elem.getStateAbbr_Customer()
					+ "</StateAbbr_Customer>\r\n");
			sb.append("\t\t<Zip_Customer>" + elem.getZip_Customer()
					+ "</Zip_Customer>\r\n");
			sb.append("\t\t<InvcDateTime>" + elem.getInvcDate()
					+ "</InvcDateTime>\r\n");
			sb.append("\t\t<ATSProduct>" + elem.getATSProduct()
					+ "</ATSProduct>\r\n");
			sb.append("\t\t<TranDtlAmt>" + elem.getTranDtlAmt()
					+ "</TranDtlAmt>\r\n");
			sb.append("\t\t<BuyerName>" + elem.getBuyerName()
					+ "</BuyerName>\r\n");
			sb.append("\t\t<SellerName>" + elem.getSellerName()
					+ "</SellerName>\r\n");
			sb.append("\t\t<AdrsLines_Property>" + elem.getAdrsLines_Property()
					+ "</AdrsLines_Property>\r\n");
			sb.append("\t\t<City_Property>" + elem.getCity_Property()
					+ "</City_Property>\r\n");
			sb.append("\t\t<County_Property>" + elem.getCounty_Property()
					+ "</County_Property>\r\n");
			sb.append("\t\t<StateAbbr_Property>" + elem.getStateAbbr_Property()
					+ "</StateAbbr_Property>\r\n");
			sb.append("\t\t<Zip_Property>" + elem.getZip_Property()
					+ "</Zip_Property>\r\n");
			sb.append("\t</FAMemoSaleInvoice>\r\n");
		}
		sb.append("</ATSInvoices>");
	}
	
	private Collection<? extends InvoicedSearch> writeToCSV(Vector<InvoiceSolomonBean> invoiceSolomonData, StringBuffer sb,boolean sendToAll, long agent) {
		HashSet<InvoicedSearch> result = new HashSet<InvoicedSearch>();
		if(invoiceSolomonData == null)
			return result;
		for (InvoiceSolomonBean userData : invoiceSolomonData) {
			if(sendToAll && userData.getAgentId() != agent ) continue;	//we only want this agent
			sb.append(userData.toString() + "\r\n");
			for (InvoiceSolomonBeanEntry beanEntry : userData.getEntries()) {
				if(sendToAll && beanEntry.getAgentId() != agent ) continue;	//we only want this agent
				sb.append(beanEntry.toString() + "\r\n");
				InvoicedSearch is = new InvoicedSearch();
				is.setSearchId(Long.parseLong(beanEntry.getSearchId()));
				
				int currentInvoiceStatus = beanEntry.getInvoiced();
				boolean searchFinished = beanEntry.isTsrCreated();
				
				if(currentInvoiceStatus == InvoicedSearch.SEARCH_NOT_INVOICED) {
					//was not invoiced
					if(searchFinished) {
						is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_FINISHED);
					} else {
						is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_INDEX);
					}
				} else if(currentInvoiceStatus == InvoicedSearch.SEARCH_INVOICED_INDEX) {
					//was invoiced only as index
					if(searchFinished) {
						is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_INDEX_AND_FINISHED);
					} else {
						is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_INDEX);
					}
				} else if(currentInvoiceStatus == InvoicedSearch.SEARCH_INVOICED_FINISHED) {
					//was invoiced only after was finished
					is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_FINISHED);
				} else if(currentInvoiceStatus == InvoicedSearch.SEARCH_INVOICED_INDEX_AND_FINISHED) {
					//keep state
					is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_INDEX_AND_FINISHED);
				} else {
					//default state - maximum price
					is.setInvoiced(InvoicedSearch.SEARCH_INVOICED_FINISHED);
				}
				
				result.add(is);
			}
		}
		
		return result;
	}

	private static SimpleDateFormat sdf1 = new SimpleDateFormat(
			"MM/dd/yyyy HH:mm:ss");

	private static SimpleDateFormat sdf2 = new SimpleDateFormat(
			"dd-MM-yyyy HH:mm:ss z");

	private static SimpleDateFormat sdf3 = new SimpleDateFormat(
			"dd-MM-yyyy HH:mm:ss");

	/**
	 * Transforms database data for the invoice
	 * 
	 * @param invoiceXmlData
	 *            data taken from the database
	 */
	protected String[] getInvoiceDataATS2FA(InvoiceXmlData[] invoiceXmlData,
			long commId,boolean sendToAll,long agent, Vector<InvoiceATS2FABean> invoiceDataATS2FA) {
		
		System.err.println("Avem intrari in numar de " + invoiceXmlData.length);

		// GregorianCalendar calendar = new GregorianCalendar();
		String fileId;
		GenericState[] states = DBManager.getAllStates(); // get all states
		Vector<String> statesAbv = new Vector<String>(); // just for states
															// abbreviation

		TreeSet<String> treeSetStatesAbv = null; // TreeSet just for states
											// abbreviation
		TreeMap<String, String> treeMapStatesAbv = new TreeMap<String, String>(); // TreeMap
																					// for
																					// name-abbrev
																					// pairs
		for (int i = 0; i < states.length; i++) {
			statesAbv.add(states[i].getStateAbv());
			treeMapStatesAbv.put(states[i].getName().toLowerCase(), states[i]
					.getStateAbv());
		}
		treeSetStatesAbv = new TreeSet<String>(statesAbv);
		Double total = 0d;
		
		Map<String, Integer> imagesCount = new TreeMap<String, Integer>();

		Pattern pattern = Pattern.compile("([^\\(]+)\\((\\d+)\\)");
		Matcher matcher = null;
		
		for (int i = 0; i < invoiceXmlData.length; i++) {
			if(sendToAll && !invoiceXmlData[i].getAgentId().equals(agent)) { 
				continue;
			}
			
			String dataSource = invoiceXmlData[i].getDataSource();
			matcher = pattern.matcher(dataSource);
			while(matcher.find()) {
				String key = matcher.group(1).trim();
				int value = Integer.parseInt(matcher.group(2));
				
				Integer previosValue = imagesCount.get(key);
				if(previosValue == null) {
					imagesCount.put(key, value);
				} else {
					imagesCount.put(key, value + previosValue);
				}
				
			}
			
			
			fileId = invoiceXmlData[i].getAbstrFileNo();

			int istart = fileId.indexOf("-");
			int iend = fileId.indexOf("_");
			if (istart > -1 && iend > -1)
				fileId = fileId.substring(istart + 1, iend);

			String agState = invoiceXmlData[i].getAgentStateAbv();
			if (!agState.equalsIgnoreCase("N/A")) {
				if (agState.length() == 2) { // which means a possible
												// abbreviation
					if (!treeSetStatesAbv.contains(agState.toUpperCase())) {
						agState = "N/A";
					} else { // it stays the same because it is good
						agState = agState.toUpperCase();
					}
				} else { // which might mean a possible name
					agState = treeMapStatesAbv.get(agState.toLowerCase());
					if (agState == null) { // if we don't find a correct name,
											// we set N/A
						agState = "N/A";
					} // else it stays the same because it is good
				}
			}
			String doneTime = "";

			Date doneDatetime = invoiceXmlData[i].getDoneTime();

			if (doneDatetime != null) {
				try {
					doneTime = sdf1.format(sdf2.parse((sdf3
							.format(doneDatetime))
							+ " GMT"));
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			total += invoiceXmlData[i].getSearchFee();

			invoiceDataATS2FA
					.add(new InvoiceATS2FABean(
							removeSpecialCharacters(invoiceXmlData[i]
									.getPropertyCounty()),
							removeSpecialCharacters(fileId),
							removeSpecialCharacters(invoiceXmlData[i]
									.getAgentCompany()),
							removeSpecialCharacters(invoiceXmlData[i]
									.getAgentWorkAddress()),
							removeSpecialCharacters(invoiceXmlData[i]
									.getAgentCity()),
							removeSpecialCharacters(agState),
							removeSpecialCharacters(invoiceXmlData[i]
									.getAgentZip()),
							doneTime,
							removeSpecialCharacters(invoiceXmlData[i].getOperatingAccountingID()),
							(invoiceXmlData[i].getSearchFlags().isTsrCreated()?
								removeSpecialCharacters(DBManager
										.getProductNameFromCommunity(commId,
												invoiceXmlData[i].getProductType())):"Index"),
							new Double(invoiceXmlData[i].getSearchFee())
									.toString(),
							removeSpecialCharacters(invoiceXmlData[i]
									.getBuyerName()),
							removeSpecialCharacters(invoiceXmlData[i]
									.getOwnersName()),
							removeSpecialCharacters(invoiceXmlData[i]
									.getPropertyAddress()),
							removeSpecialCharacters(invoiceXmlData[i]
									.getPropertyCity()),
							removeSpecialCharacters(invoiceXmlData[i]
									.getPropertyCounty()),
							removeSpecialCharacters(invoiceXmlData[i]
									.getPropertyStateAbv()),
							removeSpecialCharacters(invoiceXmlData[i]
									.getPropertyZip()),
							removeSpecialCharacters(invoiceXmlData[i]
							        .getPlantInvoice()),
							removeSpecialCharacters(invoiceXmlData[i]
									.getAgentLogin()),
							removeSpecialCharacters(invoiceXmlData[i]
									.getDataSource()),
							removeSpecialCharacters(invoiceXmlData[i]
									.getImageCount())
							)
					);
		}
		
		String[] results = new String[2];
		results[0] = total.toString();
		int posPoint = results[0].indexOf(".");
		if (posPoint > 0 && posPoint + 3 <= results[0].length()) {
			results[0] = results[0].substring(0, posPoint + 3);
		} else {
			results[0] = results[0];
		}
		
		StringBuilder sb = new StringBuilder();
		
		Integer totalImages = 0;
		for (String key : imagesCount.keySet()) {
			totalImages += imagesCount.get(key);
			if(sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(key).append("(").append(imagesCount.get(key)).append(")");
		}
		
		sb.insert(0, totalImages + " ");
		results[1] = sb.toString();
		
		return results;
	}

}
