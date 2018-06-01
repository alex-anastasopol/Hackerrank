package ro.cst.tsearch.connection.http2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.ATSConnConstants;
import ro.cst.tsearch.connection.LoginResponse;
import ro.cst.tsearch.connection.LoginResponse.LoginStatus;
import ro.cst.tsearch.connection.http.HTTPRequest;
import ro.cst.tsearch.connection.http.HTTPRequest.ParametersVector;
import ro.cst.tsearch.connection.http.HTTPResponse;
import ro.cst.tsearch.data.SitesPasswords;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.types.GenericSKLD;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Exception;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ConnectionDroppedException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.RequestNotSupportedException;
import com.ibm.as400.access.SpooledFile;
import com.ibm.as400.access.SpooledFileList;
import com.stewart.ats.connection.skld.encoding.CharMappings;
import com.stewart.ats.connection.skld.telnet.DataStreamProducer;
import com.stewart.ats.connection.skld.telnet.DataStreamQueue;
import com.stewart.ats.connection.skld.telnet.Stream5250;
import com.stewart.ats.connection.skld.utils.TN5250jConstants;

public class GenericSKLDSite extends HttpSite implements Runnable {
	
	
	protected static final Logger logger = Logger.getLogger(GenericSKLDSite.class);
	
	private static final long READ_RESPONSE_TIMEOUT = 1000;
	private static final long MAX_TIMEOUT_INTERRUPT_REQUEST = 210000;
	private static final long MAX_ON_BEFORE_REQUEST_EXCEPTION_COUNT = 3;
	
	private static final String CHAR_ENCODING  = "cp037";
	private static final int MAX_PARENT_SITE_CONSECUTIVE_REQUESTS = 5;
	
	private LinkedBlockingQueue<HTTPRequest> requestParentSiteQueue;
	private LinkedBlockingQueue<HTTPRequest> requestAutomaticQueue;
	private Semaphore requestSemaphore;
	private int parentSiteExecutionCounter;
	private AS400 printerConnection;
	private SpooledFileList spfList;
	
	
	private Socket socket;
	private BufferedInputStream bin;
	private BufferedOutputStream bout;
	private ByteArrayOutputStream baosp = null;
	private ByteArrayOutputStream baosrsp = null;
	private DataStreamQueue dsq;
	private Stream5250 bk;
	private DataStreamProducer producer;
	
	
	private String user;
	private String password;
	private int devSeq = -1;
	private String devName;
	private String devNameUsed;
	private String library;
	private String initialMenu;
	private String program;
	private boolean enhanced = true;
	private Thread pthread;
	
	protected com.stewart.ats.connection.skld.encoding.CodePage codePage;
	
	
	
	// negotiating commands
	private static final byte IAC = (byte) -1; // 255 FF
	//private static final byte DONT = (byte) -2; //254 FE
	private static final byte DO = (byte) -3; //253 FD
	private static final byte WONT = (byte) -4; //252 FC
	private static final byte WILL = (byte) -5; //251 FB
	private static final byte SB = (byte) -6; //250 Sub Begin FA
	private static final byte SE = (byte) -16; //240 Sub End F0
	private static final byte EOR = (byte) -17; //239 End of Record EF
	private static final byte TERMINAL_TYPE = (byte) 24; // 18
	private static final byte OPT_END_OF_RECORD = (byte) 25; // 19
	private static final byte TRANSMIT_BINARY = (byte) 0; // 0
	private static final byte QUAL_IS = (byte) 0; // 0
	private static final byte TIMING_MARK = (byte) 6; // 6
	private static final byte NEW_ENVIRONMENT = (byte) 39; // 27
	private static final byte IS = (byte) 0; // 0
	//private static final byte SEND = (byte) 1; // 1
	//private static final byte INFO = (byte) 2; // 2
	private static final byte VAR = (byte) 0; // 0
	private static final byte VALUE = (byte) 1; // 1
	private static final byte NEGOTIATE_ESC = (byte) 2; // 2
	private static final byte USERVAR = (byte) 3; // 3
	// miscellaneous
	private static final byte ESC = 0x04; // 04
	
	private static byte[] enterLink;
	private static byte[] f3Link = new byte[] {
		(byte)1,(byte)1,(byte)51, 
	};
	
	private static final String TITLE_OFFICER_CODE = "SW";
	
	private Thread myThread;
	
	public GenericSKLDSite() {
		requestParentSiteQueue = new LinkedBlockingQueue<HTTPRequest>();
		requestAutomaticQueue = new LinkedBlockingQueue<HTTPRequest>();
		requestSemaphore = new Semaphore(0);
		parentSiteExecutionCounter = 0;
		
		myThread = new Thread(this);
		myThread.start();
		
	}
	
	public LoginResponse onLogin(long searchId, DataSite dataSite) {
		try {
			String errorMsg = "";
			
			//if(socket == null) {
				socket = new Socket(dataSite.getLink(), 23);
			//} 
			printerConnection = new AS400(dataSite.getLink(), 
					SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(searchId), "GenericSKLD","SKLDMainUserName"), 
					SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(searchId), "GenericSKLD","SKLDMainPassword") );
			
			if(socket != null) {
				
				spfList = new SpooledFileList(printerConnection);
				spfList.setQueueFilter("/QSYS.LIB/QUSRSYS.LIB/" + 
						SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(searchId), "GenericSKLD","SKLDPrinter") + 
						".OUTQ");
				spfList.setUserFilter("*ALL");
				cleanPrinterQueue();
				
				devName = "SWATS2";
				enterLink = new byte[] {(byte)1,(byte)1,(byte)-15};
				
				socket.setKeepAlive(true);
				socket.setTcpNoDelay(true);
				socket.setSoLinger(false, 0);
				codePage = CharMappings.getCodePage("37"); 
				InputStream in = socket.getInputStream();
				OutputStream out = socket.getOutputStream();
				
				bin = new BufferedInputStream(in, 8192);
				bout = new BufferedOutputStream(out);
				baosp = new ByteArrayOutputStream();
				baosrsp = new ByteArrayOutputStream();

				byte abyte0[];
				while (negotiate(abyte0 = readNegotiations()))
					;
				
				dsq = new DataStreamQueue();
				producer = new DataStreamProducer(this, bin, dsq, abyte0);
				pthread = new Thread(producer);
				//         pthread.setPriority(pthread.MIN_PRIORITY);
				pthread.setPriority(Thread.NORM_PRIORITY / 2);
				pthread.start();
				
				
				bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read the login screen
				String response = new String(bk.getBuffer(), CHAR_ENCODING);
				if(!response.contains("Password")) {
					errorMsg = searchId + ": GenericSKLDSite: Could not login - screen does not contain password!";
					logger.error(errorMsg);
					return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, errorMsg);
				}
				
				writeGDS(0, 3, getLoginLink(
						SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(searchId), "GenericSKLD","SKLDUserName"), 
						SitesPasswords.getInstance().getPasswordValue(getCurrentCommunityId(searchId), "GenericSKLD","SKLDPassword")
						));					//send credentials
				bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read first response after sending credentials
				parseIncoming();
				bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read first response after sending credentials
				//[0, 17, 18, -96, 0, 0, 4, 0, 0, 3, 4, -13, 0, 5, -39, 112, 0, -1, -17]
				if (bk != null && bk.getOpCode() == 3) {
					int i = 0;
					while (i < 10) {
						response = new String(bk.getBuffer(), CHAR_ENCODING);
						if(!response.contains("Press Enter to continue") && 
								!response.contains("Press ENTER to exit")
							) {
							errorMsg = searchId + ": GenericSKLDSite: Could not continue - NO ENTER TO PRESS - check expiration on server!";
							logger.error(errorMsg);
							return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, errorMsg);	
						}
						parseIncoming();
						writeGDS(0, 3, enterLink);					//send first enter
						bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read first response after sending enter
						
						while (bk.getOpCode() == 12 || bk.getOpCode() == 11) {
							bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read second response after sending enter
						}
						if(bk.getOpCode() == 2) {
							bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read first real page. I am now logged in
							response = new String(bk.getBuffer(), CHAR_ENCODING);
							if(response.contains("F13=Variables")) {
								return new LoginResponse(LoginStatus.STATUS_SUCCESS, "Logged in");
							} else if(response.contains("is allocated to another job")) {
								bk = dsq.get(READ_RESPONSE_TIMEOUT);
							}
							
						}
						i++;
					}
					
					
				} else {
					errorMsg = searchId + ": GenericSKLDSite: Could not continue - NO ENTER TO PRESS - check expiration on server!";
					logger.error(errorMsg);
					return new LoginResponse(LoginStatus.STATUS_INVALID_PAGE, errorMsg);
				}
				
				
				
			} else {
				errorMsg = searchId + ": GenericSKLDSite: Could not create socket!";
				logger.error(errorMsg);
				return new LoginResponse(LoginStatus.STATUS_UNKNOWN, errorMsg);
			}
			
			
			
		} catch (Exception e) {
			String errorMsg = searchId + ": GenericSKLDSite: Exception!";
			logger.error(errorMsg, e);
			return new LoginResponse(LoginStatus.STATUS_UNKNOWN, errorMsg);
		}
		
		return new LoginResponse(LoginStatus.STATUS_UNKNOWN, "Login failed");
	}

	@SuppressWarnings("unchecked")
	private void cleanPrinterQueue() throws AS400Exception,
			AS400SecurityException, ConnectionDroppedException,
			ErrorCompletingRequestException, InterruptedException, IOException,
			RequestNotSupportedException {
		spfList.openSynchronously();
		Enumeration<SpooledFile> spfEnum = spfList.getObjects();
		while (spfEnum.hasMoreElements()) {
			SpooledFile spfMain = (SpooledFile)spfEnum.nextElement();
			if (spfMain != null) {
				logger.info(searchId + ": Deleting spoolFile " + 
						spfMain.getJobName() + ":" + 
						spfMain.getJobNumber() + ":" + 
						spfMain.getNumber() + ":" +
						spfMain.getCreateDate() + "-" + spfMain.getCreateTime());
				spfMain.delete();
			}
		}
		spfList.close();
	}
	
	@Override
	public void onBeforeRequest(HTTPRequest req) {
		//Adams - [17, 15, -15, 17, 17, 14, -15, 64, 64] - the first page request
		
		
		if(bk != null ) {
			String temp;
			
			try {
				temp = new String(bk.buffer, "cp037");
				if(temp.contains("Function key not allowed")) {
					//setDestroy(true);
					//throw new RuntimeException("Function key not allowed. Session needs to be destroyed");
					logger.error(searchId + ": GenericSKDLSite: Function key not allowed tring to disconnect " + temp);
					disconnect();
					boolean loggedIn = onLogin(req.getSearchId(), req.getDataSite()).getStatus() == LoginStatus.STATUS_SUCCESS;
					if(loggedIn){
						logger.debug(searchId + ": GenericSKDLSite: Succeded to reconnect ");
					} else {
						logger.error(searchId + ": GenericSKDLSite: Failed to reconnect ");
					}
				} else if(temp.contains("User") && temp.contains("Password")) {
					logger.error(searchId + ": GenericSKDLSite: Found User or Password where it should not be: " + temp);
					disconnect();
					boolean loggedIn = onLogin(req.getSearchId(), req.getDataSite()).getStatus() == LoginStatus.STATUS_SUCCESS;
					if(loggedIn){
						logger.debug(searchId + ": GenericSKDLSite: Succeded to reconnect ");
					} else {
						logger.error(searchId + ": GenericSKDLSite: Failed to reconnect ");
					}
				}
			} catch (Exception e) {
				logger.error(searchId + ": Exception when on onBeforeRequest bk is not null and something else happened", e);
				disconnect();
				boolean loggedIn = onLogin(req.getSearchId(), req.getDataSite()).getStatus() == LoginStatus.STATUS_SUCCESS;
				if(loggedIn){
					logger.debug(searchId + ": GenericSKDLSite: Succeded to reconnect ");
				} else {
					logger.error(searchId + ": GenericSKDLSite: Failed to reconnect ");
				}
			}
			
		}
		try {
			cleanPrinterQueue();	
		} catch (Throwable t) {
			logger.error(searchId + ": Error while cleaning printer queue cleanPrinterQueue()" , t);
		}
		
		
		
		
		for (int exceptionCount = 0; exceptionCount < MAX_ON_BEFORE_REQUEST_EXCEPTION_COUNT; exceptionCount++) {
			//first we need to select state
			byte[] bytesToSend = getSelectCountyBytes(req.getPostFirstParameter("countyNo"));
			try {
				writeGDS(0, 3, bytesToSend);
				bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
				parseIncoming();							//parse screen
				bytesToSend = getSelectSearchMenuBytes(req.getPostFirstParameter("destinationPage"), true);
				writeGDS(0, 3, bytesToSend);
				bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
				parseIncoming();							//parse screen
				
				while(!dsq.isEmpty()) {
					logger.error(searchId + ": DATA SOURCE WAS NOT EMPTY before the search!!! WARNING!!!");
					bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
				}
				
				HTTPResponse response = sendSearchQuery( req.getPostParameters(), req.getSearchId() );
				
				while(!dsq.isEmpty()) {
					logger.error(searchId + ": DATA SOURCE WAS NOT EMPTY after the search!!! WARNING!!!");
					bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
				}
				
				goToCountyScreen();
				
				while(!dsq.isEmpty()) {
					logger.error(searchId + ": DATA SOURCE WAS NOT EMPTY after going to county screen!!! WARNING!!!");
					bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
				}
				
				req.setBypassResponse(response);
				logger.debug(searchId + ": Succesfully finished onBeforeRequest!");
				break;
				
			} catch (Exception e) {
				logger.error(searchId + ": Main Error is onBeforRequest", e);
				disconnect();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					logger.error(searchId + ": Kind of impossible? Don't you think?", e1);
				}
				if(exceptionCount + 1 < MAX_ON_BEFORE_REQUEST_EXCEPTION_COUNT) {
					boolean loggedIn = onLogin(req.getSearchId(), req.getDataSite()).getStatus() == LoginStatus.STATUS_SUCCESS;
					if(loggedIn){
						logger.debug(searchId + ": GenericSKDLSite: Succeded to reconnect ");
					} else {
						logger.error(searchId + ": GenericSKDLSite: Failed to reconnect ");
					}
				}
			}
		}
		
		
		
	}
	
	@Override
	protected HTTPResponse exec(HTTPRequest request) {
		for (int i = 0; i < 3; i++) {
			try {
				HTTPResponse response = readSpoolFile();
				if(response != null) {
					return response;
				}
				try {
					Thread.sleep(2500);
				} catch (Exception es) {
					logger.error(searchId + ": Error while waiting for input stream in main exec function", es);
				}
			} catch (Exception e) {
				logger.error(searchId + ": Iteration " + i + " problem reading spoolfile", e);
			}	
		}
		
		HTTPResponse response = new HTTPResponse();
		response.is = IOUtils.toInputStream("Site error. Please try again.");
		response.returnCode = ATSConnConstants.HTTP_OK;
		response.contentType = "text/html";
		response.headers = new HashMap<String, String>();
		return response;
		
	}

	private HTTPResponse readSpoolFile() 
		throws AS400Exception, ConnectionDroppedException, AS400SecurityException, 
		ErrorCompletingRequestException, InterruptedException, IOException, RequestNotSupportedException 
	{
		try {
			spfList.openSynchronously();
			
			if(spfList.size() == 0) {
				try {
					spfList.close();
					Thread.sleep(3000);
						
				} catch (Exception es) {
					logger.error(searchId + ": Error while waiting for input stream ", es);
				} finally {
					spfList.openSynchronously();
				}
			}
			
			if(spfList.size() > 1) {
				logger.error(searchId + ": MAJOR ERROR but not fatal - spfList.size() is " + spfList.size());
			}
			if(spfList.size() > 0) {
				
				int readCounter = 5;
				StringBuffer sbuf = new StringBuffer();
				SpooledFile spfMain = null;
				HTTPResponse response = new HTTPResponse();
				while(sbuf.indexOf("** END OF SEARCH **") < 0 && readCounter-- > 0) {
					sbuf.delete(0, sbuf.length());
					spfMain = (SpooledFile) spfList.getObject(spfList.size() - 1);
					
//					try {
//						 PrintParameterList prtParm = new PrintParameterList();
//						 prtParm.setParameter(PrintObject.ATTR_MFGTYPE,"*WSCST");
//						 prtParm.setParameter(PrintObject.ATTR_WORKSTATION_CUST_OBJECT, "/QSYS.LIB/QWPDEFAULT.WSCST");
//	
//						 PrintObjectTransformedInputStream inpStream = null;
//						 inpStream = spfMain.getTransformedInputStream(prtParm);
//						 Document document = new Document();
//						 
//						 // Read the input stream buffer and create a string buffer
//						 byte[] buf = new byte[32767];
//						 StringBuffer buffer = new StringBuffer();
//						 int bytesRead = 0;
//	
//						 do {
//						 bytesRead = inpStream.read( buf );
//						 if (bytesRead > 0) {
//						 buffer.append(new String(buf,0,bytesRead));
//						 }
//						 } while ( bytesRead != -1 );
//	
//						 PdfWriter pdf = PdfWriter.getInstance(document, new FileOutputStream("d:/sp_xxxx_TXT.pdf"));
//	
//						 Font courierFont = FontFactory.getFont(FontFactory.COURIER, 10);
//						 Paragraph para = new Paragraph(buffer.toString(), courierFont);
//	
//						 document.open();
//						 document.add(para);
//						 pdf.resume();
//					 document.close();
//					} catch (Exception e1) {
//						e1.printStackTrace();
//					}
					
					try {
						response.is = spfMain.getInputStream();
					} catch (Exception e) {
						logger.error(searchId + ": First Error while trying to read the spool list", e);
						try {
							Thread.sleep(2500);
						} catch (Exception es) {
							logger.error(searchId + ": Error while waiting for input stream ", es);
						}
						
						response.is = spfMain.getInputStream();							
						logger.debug(searchId + "Second try to read the spool list was succesfull", e);
					}
					
					sbuf.append(IOUtils.toString(response.is, CHAR_ENCODING));
				}
				if(spfMain != null) {
					spfMain.delete();
				}
				
				if(sbuf.indexOf("** END OF SEARCH **") < 0 && 
						sbuf.indexOf("NO MATCHES FOUND FOR SEARCH CRITERIA ENTERED") < 0 &&
						sbuf.indexOf("NO RECORDS SELECTED") < 0 &&
						sbuf.indexOf("NO LEGAL STRUCTURE RECORD FOUND") < 0) {
					logger.error(sbuf);
					sbuf.delete(0, sbuf.length());
					sbuf.append("Incomplete results found - No End Of Search Received - please try your search again or rescrict search parameters");
					logger.error(searchId + ": Incomplete results found - No End Of Search Received - please try your search again or rescrict search parameters");
					
				}
				
				response.is = IOUtils.toInputStream(sbuf.toString());
				response.returnCode = ATSConnConstants.HTTP_OK;
				response.contentType = "text/html";
				response.headers = new HashMap<String, String>();
				logger.debug(searchId + ": Read was successfull________________________________________________________");
				return response;
			} else {
				logger.error(searchId + ": spfList.size() is 0 (zero)");
				return null;
			}
		
		} finally {
			spfList.close();
		}

	}
	
	private byte[] getLoginLink(String userName, String password) {
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(9); stream.write(74); stream.write(241);
		stream.write(17); stream.write(8); stream.write(66);
		
		for (int i = 0; i < userName.length(); i++) {
			stream.write(codePage.uni2ebcdic(userName.charAt(i)));
		}
		stream.write(17); stream.write(9); stream.write(66);
		for (int i = 0; i < password.length(); i++) {
			stream.write(codePage.uni2ebcdic(password.charAt(i)));
		}
		
		return stream.toByteArray();
	}
	
	private void goToCountyScreen() throws IOException, InterruptedException {
		int count = 0;
		while(true) {
			
			writeGDS(0, 3, f3Link);
			bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
			parseIncoming();
			
			if(bk.getOpCode() == 2) {
				bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
				parseIncoming();
			}
			
			String response = new String(bk.getBuffer(), CHAR_ENCODING);
			if(response.contains("COUNTY SEARCH MENU")) {
				break;
			}
			
			count ++;
			if(count >= 10) {
				throw new RuntimeException("Too many requests to go to CountyScreen - something is wrong");
			}
		}
		
		
	}

	private HTTPResponse sendSearchQuery(HashMap<String, ParametersVector> parametersMap, long searchId) throws IOException, InterruptedException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		String field = parametersMap.get("destinationPage").getFirstParameter();
		if(field == null) {
			throw new RuntimeException("No destinationPage");
		}
		
		Search search = getSearch(searchId);
		
		if("25".equals(field) || "4".equals(field) || "5".equals(field) || "26".equals(field)) {
			//just do lookup
//		} else if ("1".equals(field)){
//			stream.write(19); stream.write(12); stream.write(241);	//send command key - which is ENTER
//			String searchName = search.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO);
//			if(searchName.length() > 15) {
//				searchName = searchName.substring(0, 15);
//			}
//			
//			for (int i = 0; i < searchName.length(); i++) {
//				stream.write(codePage.uni2ebcdic(searchName.charAt(i)));
//			}
//			writeGDS(0, 3, stream.toByteArray());
//			bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
//			
//			stream.reset();
//			
//			stream.write(10); stream.write(12); stream.write(241);
//			writeGDS(0, 3, stream.toByteArray());
//			bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
//			
//			stream.reset();
//			
//			String temp = new String(bk.buffer, "cp037");
//			if(temp.contains("ENTER THE OFFICER CODE")) {
//				logger.error(searchId + ": Error - we need to enter Officer Code - Invalid State - Trying reset");
//				search.getSa().setSearchIdSKLD(null);
//				throw new RuntimeException("Error - we need to enter Officer Code - Invalid State - Trying reset");
//			}
		} else if("30".equals(field)) {
			
			String temp = new String(bk.buffer, "cp037");
			HTTPResponse response = new HTTPResponse();
			response.is = IOUtils.toInputStream(temp);
			response.returnCode = ATSConnConstants.HTTP_OK;
			response.contentType = "text/html";
			response.headers = new HashMap<String, String>();

			return response;
			
		} else {
		
			if(search.getSa().getSearchIdSKLD() == null) {
				//I must create the order first
				stream.write(10); stream.write(14); stream.write(241);	//send command key - which is ENTER
				
				//String searchName = "ATS" + searchId;			//B5764

//				String searchName = search.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO) + "_";
//				if(searchName.length() > 15) {
//					searchName = searchName.substring(0, 15);
//				} else {
//					long originalSearchId = search.getSa().getOriginalSearchId();
//					
//					if(originalSearchId<=0){
//						originalSearchId = searchId;
//					}
//					
//					String searchIdString = Long.toString(originalSearchId);
//					if( searchName.length() + searchIdString.length() > 15 ) {
//						searchName += searchIdString.substring(searchIdString.length() - (15 - searchName.length()));
//					} else {
//						searchName += searchIdString;
//					}
//				}
				//Start 9930 
				String searchName = search.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO);
				if(searchName.length() > 15) {
					searchName = searchName.substring(0, 15);
				}
				
				stream.write(17); stream.write(10); stream.write(8);
				for (int i = 0; i < searchName.length(); i++) {
					stream.write(codePage.uni2ebcdic(searchName.charAt(i)));
				}
				writeGDS(0, 3, stream.toByteArray());
				bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
				parseIncoming();
				stream.reset();
				
				stream.write(13); stream.write(25); stream.write(53);	//send command key - which is F5
				stream.write(17); stream.write(13); stream.write(23);
				for (int i = 0; i < TITLE_OFFICER_CODE.length(); i++) {
					stream.write(codePage.uni2ebcdic(TITLE_OFFICER_CODE.charAt(i)));
				}
				writeGDS(0, 3, stream.toByteArray());
				bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
				parseIncoming();
				stream.reset();
				
				search.getSa().setSearchIdSKLD(searchName);
				
			} else {
				stream.write(10); stream.write(14); stream.write(241);	//send command key - which is ENTER
				
				String searchName = search.getSa().getSearchIdSKLD();
				stream.write(17); stream.write(10); stream.write(8);
				for (int i = 0; i < searchName.length(); i++) {
					stream.write(codePage.uni2ebcdic(searchName.charAt(i)));
				}
				writeGDS(0, 3, stream.toByteArray());
				bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
				
				stream.reset();
				
				String temp = new String(bk.buffer, "cp037");
				if(temp.contains("ENTER THE OFFICER CODE")) {
					logger.error(searchId + ": Error - we need to enter Officer Code - Invalid State - Trying reset");
					search.getSa().setSearchIdSKLD(null);
					throw new RuntimeException("Error - we need to enter Officer Code - Invalid State - Trying reset");
				}
			}
		}
		
		/*
		 * [20, 46, 56, 
		 * 17, 19, 16, -14, -13, -15, -14, -12, 97, -15, -14, -13, 64, 64, 64, 64, 64, 64, 
		 * 17, 20, 11, -16, -15, -16, -15, 64, 
		 * 17, 20, 17, -14, -16, -16, -16, 
		 * 17, 20, 24, -16, -14, -16, -14, 64, 
		 * 17, 20, 30, -14, -16, -16, -7, 
		 * 17, 20, 43, -63, -58, -58, 64, 64, 64, 64]
		 * 
		 * 
		 * 
		 * The Highlighted Value(s) are Invalid
		*/
		if("3".equals(field)) {
			return doNameSearch(parametersMap, stream);
		} else if("4".equals(field)) {
			return doAddressSearch(parametersMap, stream);
		} else if("5".equals(field)) {
			return doBookPageInstrumentSearch(parametersMap, stream);
		} else if("25".equals(field)) {
			return doSubdivisionLookup(parametersMap, stream);
		} else if("26".equals(field)) {
			return doSubdivisionSecondaryLookup(parametersMap, stream);
		} else if("2".equals(field)) {
			return doSubdivisionSearch(parametersMap, stream);
		} else if("21".equals(field)) {
			return doSectionSearch(parametersMap, stream);
		} else if("22".equals(field)) {
			return doClaimSearch(parametersMap, stream);
		} else if("23".equals(field)) {
			return doFreeFormSearch(parametersMap, stream);
		} else if("1".equals(field)) {
			return doOrderSearch(parametersMap, stream);
		}
		return null;
		
	}

	private HTTPResponse doFreeFormSearch(
			HashMap<String, ParametersVector> parametersMap,
			ByteArrayOutputStream stream) throws IOException,InterruptedException{
		String field = null;
		stream.reset();
		
		String temp = new String(bk.buffer, CHAR_ENCODING);
		int count = 0;
		while(!temp.contains("B/L-A") && !temp.contains("S-A-M") && count++ < 10) {
			if(bk.getOpCode() == 3) {
				stream.write(11); stream.write(10); stream.write(59);	//write command key - F11
				writeGDS(0, 3, stream.toByteArray());
				stream.reset();
			}
			bk = dsq.get(READ_RESPONSE_TIMEOUT);
			temp = new String(bk.buffer, CHAR_ENCODING);
			
		}
		
		
		stream.write(1); stream.write(3); stream.write(56);	//write command key - F8
		
		
		
		field = parametersMap.get("blaLow").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			if(field.length() > 20) {
				field = field.substring(0, 20);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(11); stream.write(11);
			for (int i = 0; i < field.length() && i < 15; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		field = parametersMap.get("blaHigh").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			if(field.length() > 20) {
				field = field.substring(0, 20);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(11); stream.write(34);
			for (int i = 0; i < field.length() && i < 15; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		field = parametersMap.get("sam").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			if(field.length() > 20) {
				field = field.substring(0, 20);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(11); stream.write(61);
			for (int i = 0; i < field.length() && i < 15; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		field = parametersMap.get("name").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			if(field.length() > 25) {
				field = field.substring(0, 25);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(12); stream.write(10);
			for (int i = 0; i < field.length() && i < 15; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		field = parametersMap.get("platName").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			if(field.length() > 14) {
				field = field.substring(0, 14);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(12); stream.write(54);
			for (int i = 0; i < field.length() && i < 15; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		if(parametersMap.get("legalType") != null) {
			field = parametersMap.get("legalType").getFirstParameter();
			if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
				field = field.toUpperCase();
				stream.write(17); stream.write(12); stream.write(74);
				stream.write(codePage.uni2ebcdic(field.charAt(0)));
				stream.write(codePage.uni2ebcdic(field.charAt(0)));	
			}
		}
		
		//--- start Sending Start Month/Day
		field = parametersMap.get("startMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming Start Month/Day to 4 characters");
				field = field.substring(0, 5);
			}
			field = field.toUpperCase(); 
			stream.write(17); stream.write(13); stream.write(11);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Month/Day
		
		//--- start Sending Start Year
		field = parametersMap.get("startYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming Start Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase(); 
			stream.write(17); stream.write(13); stream.write(17);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Year
		
		//--- start Sending End Month/Day
		field = parametersMap.get("endMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming End Month/Day to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			//stream.write(17); stream.write(4); stream.write(24); 
			stream.write(17); stream.write(13); stream.write(24);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Month/Day
		//--- start Sending End Year
		field = parametersMap.get("endYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming End Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			//stream.write(17); stream.write(4); stream.write(30);
			stream.write(17); stream.write(13); stream.write(30);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Year
		
		//--- start Sending Doctype
		field = parametersMap.get("doctype").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 7) {
				logger.info(searchId + ": Trimming Doctype to 7 characters");
				field = field.substring(0, 7);
			} else {
				field = org.apache.commons.lang.StringUtils.rightPad(field, 7);
			}
			field = field.toUpperCase();
			//stream.write(17); stream.write(4); stream.write(43); 
			stream.write(17); stream.write(13); stream.write(42);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Doctype
		if(parametersMap.get("gen") != null) {
			field = parametersMap.get("gen").getFirstParameter();
			if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
				field = field.toUpperCase();
				stream.write(17); stream.write(13); stream.write(56);
				stream.write(codePage.uni2ebcdic(field.charAt(0)));	
			}
			//------------------ end Sending Gen
		}
		
		
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
		parseIncoming();
		HTTPResponse response = null;
		count = 10;
		boolean redoInvestigative = false;
		while (count-- > 0) {
			try {
				temp = new String(bk.buffer, CHAR_ENCODING);
				
				if(temp.contains("Order updated &") || temp.contains("Starter Created")) {
					redoInvestigative = true;
					break;
				}
				
				if(temp.contains("INVALID LEGAL ENTERED") || 
						temp.contains("THE LEGAL IS ALREADY A PART OF THE ORDER") ||
						temp.contains("You must key in a secondary legal")) {
					response = doInvestigativeFreeFormSearch(parametersMap, stream);
					break;
				}
				
				if(isInvalidSearchData(temp)) {
					response = new HTTPResponse();
					response.is = IOUtils.toInputStream(temp);
					response.returnCode = ATSConnConstants.HTTP_OK;
					response.contentType = "text/html";
					response.headers = new HashMap<String, String>();

					return response;
				}
				bk = dsq.get(READ_RESPONSE_TIMEOUT);			//read screen
				while(bk.getOpCode() == 2) {
					bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen	
				}
				
			} catch (Exception e) {
				logger.error(searchId + ": Some error while sending data", e);
			}
		}
		
		if(redoInvestigative) {			
			response = doInvestigativeFreeFormSearch(parametersMap, stream);
		}
		
		return response;
	}
	
	private boolean isInvalidSearchData(String responseAsString) {
		
		return responseAsString.contains("INVALID LEGAL ENTERED") || 
					responseAsString.contains("field cannot be equal") || 
					responseAsString.contains("Primary legal must be entered") ||
					responseAsString.contains("MAP EDIT NOT FOUND FOR THE NAME GIVEN") ||
					responseAsString.contains("SUBDIVISION LEGAL KEYED NOT FOUND IN MAP EDIT") ||
					responseAsString.contains("SURVEY LEGAL KEYED DOES NOT EXIST IN MAP EDIT") ||
					responseAsString.contains("HIGH OR LOW SUBDIVISION SECONDARY LEGAL INVALID AS KEYED") ||
					responseAsString.contains("HIGH OR LOW ACREAGE SECONDARY") ||
					responseAsString.toLowerCase().contains("required in low field") ||
					responseAsString.contains("You must key in a secondary legal") ||
					responseAsString.toLowerCase().contains("required in high field");
	}
		
	
	private HTTPResponse doInvestigativeFreeFormSearch(
			HashMap<String, ParametersVector> parametersMap,
			ByteArrayOutputStream stream) throws IOException,InterruptedException{
		try {
			cleanPrinterQueue();
		} catch (Exception e) {
			logger.error(searchId + ": Error in doInvestigativeFreeFormSearch while cleaningPrinterQueue", e);
		} 
		
		logger.debug(searchId + ": doInvestigativeFreeFormSearch - entering ");
		String field = null;
		boolean checkDoctype = false;
		writeGDS(0, 3, f3Link);
		bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
		parseIncoming();
		
		if(bk.getOpCode() == 2) {
			bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
			parseIncoming();
		}
		
		//be sure we are safe
		if(stream == null) {
			stream = new ByteArrayOutputStream();
		} else {
			stream.reset();	
		}
		
		stream.write(getSelectSearchMenuBytes("2", false));
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);
		
		int count = 0;
		stream.reset();
		String temp = new String(bk.buffer, CHAR_ENCODING);
		while(!temp.contains("B/L-A") && !temp.contains("S-A-M") && count++ <= 10 ) {
			stream.write(1); stream.write(2); stream.write(59);	//write command key - F11
			writeGDS(0, 3, stream.toByteArray());
			bk = dsq.get(READ_RESPONSE_TIMEOUT);
			temp = new String(bk.buffer, CHAR_ENCODING);
			stream.reset();
		}
		
		stream.write(1); stream.write(2); stream.write(56);	//write command key - F8
		
		
		field = parametersMap.get("blaLow").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			if(field.length() > 20) {
				field = field.substring(0, 20);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(2); stream.write(11);
			for (int i = 0; i < field.length() && i < 15; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		field = parametersMap.get("blaHigh").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			if(field.length() > 20) {
				field = field.substring(0, 20);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(2); stream.write(34);
			for (int i = 0; i < field.length() && i < 15; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		field = parametersMap.get("sam").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			if(field.length() > 20) {
				field = field.substring(0, 20);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(2); stream.write(61);
			for (int i = 0; i < field.length() && i < 15; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		field = parametersMap.get("name").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			if(field.length() > 25) {
				field = field.substring(0, 25);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(3); stream.write(10);
			for (int i = 0; i < field.length() && i < 15; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		field = parametersMap.get("platName").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			if(field.length() > 14) {
				field = field.substring(0, 14);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(3); stream.write(53);
			for (int i = 0; i < field.length() && i < 15; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		if(parametersMap.get("legalType") != null) {
			field = parametersMap.get("legalType").getFirstParameter();
			if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
				field = field.toUpperCase();
				stream.write(17); stream.write(3); stream.write(79);
				stream.write(codePage.uni2ebcdic(field.charAt(0)));
				stream.write(codePage.uni2ebcdic(field.charAt(1)));
			}
		}
		
		//--- start Sending Start Month/Day
		field = parametersMap.get("startMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming Start Month/Day to 4 characters");
				field = field.substring(0, 5);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(11); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Month/Day
		
		//--- start Sending Start Year
		field = parametersMap.get("startYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming Start Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(17); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Year
		
		//--- start Sending End Month/Day
		field = parametersMap.get("endMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming End Month/Day to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(24); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Month/Day
		//--- start Sending End Year
		field = parametersMap.get("endYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming End Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(30);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Year
		
		//--- start Sending Doctype
		field = parametersMap.get("doctype").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			checkDoctype = true;
			if(field.length() > 7) {
				logger.info(searchId + ": Trimming Doctype to 7 characters");
				field = field.substring(0, 7);
			} else {
				field = org.apache.commons.lang.StringUtils.rightPad(field, 7);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(42); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Doctype
		if(parametersMap.get("gen") != null) {
			field = parametersMap.get("gen").getFirstParameter();
			if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
				field = field.toUpperCase();
				stream.write(17); stream.write(4); stream.write(57);
				stream.write(codePage.uni2ebcdic(field.charAt(0)));	
			}
			//------------------ end Sending Gen
		}
		
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
		
		HTTPResponse response = null;
		
		
		count = 10;
		while (count-- > 0 && bk != null) {
			try {
				temp = new String(bk.buffer, CHAR_ENCODING);
				
				if(temp.contains("Print request was submitted")){
					break;
				}
				
				if(isInvalidSearchData(temp)) {
					response = new HTTPResponse();
					response.is = IOUtils.toInputStream(temp);
					response.returnCode = ATSConnConstants.HTTP_OK;
					response.contentType = "text/html";
					response.headers = new HashMap<String, String>();

					return response;
				}
				if(checkDoctype && bk.getOpCode() == 3) {
					response = checkDoctype(temp, stream);
					if(response != null) {
						cleanPrinterQueue();
						return response;
					}
				}
				
				bk = dsq.get(READ_RESPONSE_TIMEOUT);			//read screen
				while(bk.getOpCode() == 2) {
					bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen	
				}
				
				
			} catch (Exception e) {
				logger.error(searchId + ": Some error while sending data", e);
				break;
			}
		}
		return response;
	}

	private HTTPResponse doClaimSearch(
			HashMap<String, ParametersVector> parametersMap,
			ByteArrayOutputStream stream)throws IOException,InterruptedException{
		String field = null;
		stream.reset();
		
		String temp = new String(bk.buffer, CHAR_ENCODING);
		int count = 0;
		while(!temp.contains("Claim") && count++ < 10) {
			if(bk.getOpCode() == 3) {
				stream.write(11); stream.write(10); stream.write(59);	//write command key - F11
				writeGDS(0, 3, stream.toByteArray());
				stream.reset();
			}
			bk = dsq.get(READ_RESPONSE_TIMEOUT);
			temp = new String(bk.buffer, CHAR_ENCODING);
			
		}
		
		
		stream.write(1); stream.write(3); stream.write(56);	//write command key - F8
		
		
		
		field = parametersMap.get("claimName").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(11); stream.write(16);
			for (int i = 0; i < field.length() && i < 15; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		field = parametersMap.get("claimNo").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(11); stream.write(43);
			for (int i = 0; i < field.length() && i < 15; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		field = parametersMap.get("district").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(11); stream.write(63);
			for (int i = 0; i < field.length() && i < 15; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		//--- start Sending Start Month/Day
		field = parametersMap.get("startMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming Start Month/Day to 4 characters");
				field = field.substring(0, 5);
			}
			field = field.toUpperCase(); 
			stream.write(17); stream.write(13); stream.write(11);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Month/Day
		
		//--- start Sending Start Year
		field = parametersMap.get("startYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming Start Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase(); 
			stream.write(17); stream.write(13); stream.write(17);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Year
		
		//--- start Sending End Month/Day
		field = parametersMap.get("endMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming End Month/Day to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			//stream.write(17); stream.write(4); stream.write(24); 
			stream.write(17); stream.write(13); stream.write(24);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Month/Day
		//--- start Sending End Year
		field = parametersMap.get("endYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming End Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			//stream.write(17); stream.write(4); stream.write(30);
			stream.write(17); stream.write(13); stream.write(30);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Year
		
		//--- start Sending Doctype
		field = parametersMap.get("doctype").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 7) {
				logger.info(searchId + ": Trimming Doctype to 7 characters");
				field = field.substring(0, 7);
			} else {
				field = org.apache.commons.lang.StringUtils.rightPad(field, 7);
			}
			field = field.toUpperCase();
			//stream.write(17); stream.write(4); stream.write(43); 
			stream.write(17); stream.write(13); stream.write(42);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Doctype
		if(parametersMap.get("gen") != null) {
			field = parametersMap.get("gen").getFirstParameter();
			if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
				field = field.toUpperCase();
				stream.write(17); stream.write(13); stream.write(56);
				stream.write(codePage.uni2ebcdic(field.charAt(0)));	
			}
			//------------------ end Sending Gen
		}
		
		
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
		parseIncoming();
		HTTPResponse response = null;
		count = 10;
		boolean redoInvestigative = false;
		while (count-- > 0) {
			try {
				temp = new String(bk.buffer, CHAR_ENCODING);
				
				if(temp.contains("Order updated &") || temp.contains("Starter Created")) {
					redoInvestigative = true;
					break;
				}
				
				if(temp.contains("THE LEGAL IS ALREADY A PART OF THE ORDER")) {
					response = doInvestigativeClaimSearch(parametersMap, stream);
					break;
				}
				if(temp.contains("INVALID LEGAL ENTERED")) {
					response = doInvestigativeClaimSearch(parametersMap, stream);
					break;
				}
				if(temp.contains("field cannot be equal") || 
						temp.contains("Primary legal must be entered") ||
						temp.contains("MAP EDIT NOT FOUND FOR THE NAME GIVEN") ||
						temp.toLowerCase().contains("required in low field") ||
						temp.contains("HIGH OR LOW ACREAGE SECONDARY") ||
						temp.toLowerCase().contains("required in high field")) {
					response = new HTTPResponse();
					response.is = IOUtils.toInputStream(temp);
					response.returnCode = ATSConnConstants.HTTP_OK;
					response.contentType = "text/html";
					response.headers = new HashMap<String, String>();

					return response;
				}
				bk = dsq.get(READ_RESPONSE_TIMEOUT);			//read screen
				while(bk.getOpCode() == 2) {
					bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen	
				}
				
			} catch (Exception e) {
				logger.error(searchId + ": Some error while sending data", e);
			}
		}
		
		if(redoInvestigative) {			
			response = doInvestigativeClaimSearch(parametersMap, stream);
		}
		
		return response;
	}

	private HTTPResponse doInvestigativeClaimSearch(
			HashMap<String, ParametersVector> parametersMap,
			ByteArrayOutputStream stream) 
	throws IOException,InterruptedException{
		
		try {
			cleanPrinterQueue();
		} catch (Exception e) {
			logger.error(searchId + ": Error in doInvestigativeClaimSearch while cleaningPrinterQueue", e);
		} 
		
		logger.debug(searchId + ": doInvestigativeClaimSearch - entering ");
		String field = null;
		boolean checkDoctype = false;
		writeGDS(0, 3, f3Link);
		bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
		parseIncoming();
		
		if(bk.getOpCode() == 2) {
			bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
			parseIncoming();
		}
		
		//be sure we are safe
		if(stream == null) {
			stream = new ByteArrayOutputStream();
		} else {
			stream.reset();	
		}
		
		stream.write(getSelectSearchMenuBytes("2", false));
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);
		
		int count = 0;
		stream.reset();
		String temp = new String(bk.buffer, CHAR_ENCODING);
		while(!temp.contains("Claim") && count++ <= 10 ) {
			stream.write(1); stream.write(2); stream.write(59);	//write command key - F11
			writeGDS(0, 3, stream.toByteArray());
			bk = dsq.get(READ_RESPONSE_TIMEOUT);
			temp = new String(bk.buffer, CHAR_ENCODING);
			stream.reset();
		}
		
		stream.write(1); stream.write(2); stream.write(56);	//write command key - F8
		
		
		
		field = parametersMap.get("claimName").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(2); stream.write(16);
			for (int i = 0; i < field.length() && i < 15; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		field = parametersMap.get("claimNo").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(2); stream.write(43);
			for (int i = 0; i < field.length() && i < 15; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		field = parametersMap.get("district").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(2); stream.write(63);
			for (int i = 0; i < field.length() && i < 15; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		//--- start Sending Start Month/Day
		field = parametersMap.get("startMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming Start Month/Day to 4 characters");
				field = field.substring(0, 5);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(11); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Month/Day
		
		//--- start Sending Start Year
		field = parametersMap.get("startYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming Start Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(17); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Year
		
		//--- start Sending End Month/Day
		field = parametersMap.get("endMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming End Month/Day to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(24); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Month/Day
		//--- start Sending End Year
		field = parametersMap.get("endYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming End Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(30);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Year
		
		//--- start Sending Doctype
		field = parametersMap.get("doctype").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			checkDoctype = true;
			if(field.length() > 7) {
				logger.info(searchId + ": Trimming Doctype to 7 characters");
				field = field.substring(0, 7);
			} else {
				field = org.apache.commons.lang.StringUtils.rightPad(field, 7);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(42); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Doctype
		if(parametersMap.get("gen") != null) {
			field = parametersMap.get("gen").getFirstParameter();
			if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
				field = field.toUpperCase();
				stream.write(17); stream.write(4); stream.write(57);
				stream.write(codePage.uni2ebcdic(field.charAt(0)));	
			}
			//------------------ end Sending Gen
		}
		
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
		
		HTTPResponse response = null;
		
		
		count = 10;
		while (count-- > 0 && bk != null) {
			try {
				temp = new String(bk.buffer, CHAR_ENCODING);
				
				if(temp.contains("Print request was submitted")){
					break;
				}
				
				if(temp.contains("INVALID LEGAL ENTERED") || 
						temp.contains("field cannot be equal") || 
						temp.toLowerCase().contains("required in low field") ||
						temp.contains("MAP EDIT NOT FOUND FOR THE NAME GIVEN") ||
						temp.contains("required in High field") ||
						temp.contains("HIGH OR LOW ACREAGE SECONDARY") ||
						temp.contains("Primary legal must be entered")) {
					response = new HTTPResponse();
					response.is = IOUtils.toInputStream(temp);
					response.returnCode = ATSConnConstants.HTTP_OK;
					response.contentType = "text/html";
					response.headers = new HashMap<String, String>();

					return response;
				}
				if(checkDoctype && bk.getOpCode() == 3) {
					response = checkDoctype(temp, stream);
					if(response != null) {
						cleanPrinterQueue();
						return response;
					}
				}
				
				bk = dsq.get(READ_RESPONSE_TIMEOUT);			//read screen
				while(bk.getOpCode() == 2) {
					bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen	
				}
				
				
			} catch (Exception e) {
				logger.error(searchId + ": Some error while sending data", e);
				break;
			}
		}
		return response;
	}

	private HTTPResponse doSubdivisionSearch(
			HashMap<String, ParametersVector> parametersMap,
			ByteArrayOutputStream stream) 
		throws IOException,InterruptedException{
		String field = null;
		stream.reset();
		stream.write(13); stream.write(45); stream.write(56);	//write command key - F8
		boolean checkDoctype = false;
		
		
		field = parametersMap.get("lotLow").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
		
			/*if(StringUtils.isEmpty(field)) {
				field = "*";
			}*/
			
			field = field.toUpperCase();
			stream.write(17); stream.write(11); stream.write(10);
			for (int i = 0; i < field.length() && i < 28; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		field = parametersMap.get("blockLow").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			/*if(StringUtils.isEmpty(field)) {
				field = "*";
			}*/
			field = field.toUpperCase();
			stream.write(17); stream.write(11); stream.write(23);
			for (int i = 0; i < field.length() && i < 28; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		field = parametersMap.get("lotHigh").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(11); stream.write(35);
			for (int i = 0; i < field.length() && i < 28; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		field = parametersMap.get("blockHigh").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(11); stream.write(48);
			for (int i = 0; i < field.length() && i < 28; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		field = parametersMap.get("mapplat").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(11); stream.write(63);
			for (int i = 0; i < field.length() && i < 28; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		field = parametersMap.get("mapbook").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(11); stream.write(71);
			for (int i = 0; i < field.length() && i < 28; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		field = parametersMap.get("subdivisionName").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(12); stream.write(15);
			for (int i = 0; i < field.length() && i < 28; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		//--- start Sending Start Month/Day
		field = parametersMap.get("startMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming Start Month/Day to 4 characters");
				field = field.substring(0, 5);
			}
			field = field.toUpperCase();
			//stream.write(17); stream.write(4); stream.write(11); 
			stream.write(17); stream.write(13); stream.write(11);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Month/Day
		
		//--- start Sending Start Year
		field = parametersMap.get("startYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming Start Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			//stream.write(17); stream.write(4); stream.write(17); 
			stream.write(17); stream.write(13); stream.write(17);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Year
		
		//--- start Sending End Month/Day
		field = parametersMap.get("endMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming End Month/Day to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			//stream.write(17); stream.write(4); stream.write(24); 
			stream.write(17); stream.write(13); stream.write(24);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Month/Day
		//--- start Sending End Year
		field = parametersMap.get("endYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming End Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			//stream.write(17); stream.write(4); stream.write(30);
			stream.write(17); stream.write(13); stream.write(30);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Year
		
		//--- start Sending Doctype
		field = parametersMap.get("doctype").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 7) {
				logger.info(searchId + ": Trimming Doctype to 7 characters");
				field = field.substring(0, 7);
			} else {
				field = org.apache.commons.lang.StringUtils.rightPad(field, 7);
			}
			checkDoctype = true;
			field = field.toUpperCase();
			//stream.write(17); stream.write(4); stream.write(43); 
			stream.write(17); stream.write(13); stream.write(42);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Doctype
		
		//--- start Sending Starters
		field = parametersMap.get("starter").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			field = field.toUpperCase(); 
			stream.write(17); stream.write(13); stream.write(63);
			stream.write(codePage.uni2ebcdic(field.charAt(0)));	
		}
		//------------------ end Sending Starters
		
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
		parseIncoming();
		HTTPResponse response = null;
		String temp;
		int count = 10;
		boolean redoInvestigative = false;
		while (count-- > 0) {
			try {
				temp = new String(bk.buffer, CHAR_ENCODING);
				
				if(temp.contains("Order updated &") || temp.contains("Starter Created")) {
					redoInvestigative = true;
					break;
				}
				
				if(temp.contains("THE LEGAL IS ALREADY A PART OF THE ORDER")) {
					response = doInvestigativeSubdivisionSearch(parametersMap, stream);
					break;
				}
				if(temp.contains("INVALID LEGAL ENTERED")) {
					response = doInvestigativeSubdivisionSearch(parametersMap, stream);
					break;
				}
				if(temp.contains("field cannot be equal") || 
						temp.contains("Primary legal must be entered") ||
						temp.toLowerCase().contains("required in low field") ||
						temp.toLowerCase().contains("required in high field")) {
					response = new HTTPResponse();
					response.is = IOUtils.toInputStream(temp);
					response.returnCode = ATSConnConstants.HTTP_OK;
					response.contentType = "text/html";
					response.headers = new HashMap<String, String>();

					return response;
				}
				
				if(checkDoctype && bk.getOpCode() == 3) {
					response = checkDoctype(temp, stream);
					if(response != null) {
						cleanPrinterQueue();
						return response;
					}
				}
				
				bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
				parseIncoming();
				
			} catch (Exception e) {
				logger.error(searchId + ": Some error while sending data", e);
			}
		}
		
		if(redoInvestigative) {
			response = doInvestigativeSubdivisionSearch(parametersMap, stream);
		}
		
		return response;
	}
	
	private HTTPResponse doInvestigativeSubdivisionSearch(
			HashMap<String, ParametersVector> parametersMap,
			ByteArrayOutputStream stream) 
		throws IOException,InterruptedException{
		
		try {
			cleanPrinterQueue();
		} catch (Exception e) {
			logger.error(searchId + ": Error in doInvestigativeSubdivisionSearch while cleaningPrinterQueue", e);
		} 
		
		logger.debug(searchId + ": doInvestigativeSubdivisionSearch - entering ");
		logger.debug(searchId + ": parametersMap: " + parametersMap.toString());
		String field = null;
		writeGDS(0, 3, f3Link);
		bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
		parseIncoming();
		boolean checkDoctype = false;
		if(bk.getOpCode() == 2) {
			bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
			parseIncoming();
		}
		
		//be sure we are safe
		if(stream == null) {
			stream = new ByteArrayOutputStream();
		} else {
			stream.reset();	
		}
		
		stream.write(getSelectSearchMenuBytes("2", false));
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);
		stream.reset();
		
		stream.write(2); stream.write(2); stream.write(56);	//write command key - F8
			
		field = parametersMap.get("lotLow").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
		field = field.toUpperCase();
			stream.write(17); stream.write(2); stream.write(10);
			for (int i = 0; i < field.length() && i < 28; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		field = parametersMap.get("blockLow").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(2); stream.write(23);
			for (int i = 0; i < field.length() && i < 28; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		field = parametersMap.get("lotHigh").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(2); stream.write(35);
			for (int i = 0; i < field.length() && i < 28; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		field = parametersMap.get("blockHigh").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(2); stream.write(48);
			for (int i = 0; i < field.length() && i < 28; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		field = parametersMap.get("mapplat").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(2); stream.write(64);
			for (int i = 0; i < field.length() && i < 28; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		field = parametersMap.get("mapbook").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(2); stream.write(72);
			for (int i = 0; i < field.length() && i < 28; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		field = parametersMap.get("subdivisionName").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(3); stream.write(15);
			for (int i = 0; i < field.length() && i < 28; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		//--- start Sending Start Month/Day
		field = parametersMap.get("startMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming Start Month/Day to 4 characters");
				field = field.substring(0, 5);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(11); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Month/Day
		
		//--- start Sending Start Year
		field = parametersMap.get("startYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming Start Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(17); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Year
		
		//--- start Sending End Month/Day
		field = parametersMap.get("endMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming End Month/Day to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(24); 
			
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Month/Day
		//--- start Sending End Year
		field = parametersMap.get("endYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming End Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(30);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Year
		
		//--- start Sending Doctype
		field = parametersMap.get("doctype").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 7) {
				logger.info(searchId + ": Trimming Doctype to 7 characters");
				field = field.substring(0, 7);
			} else {
				field = org.apache.commons.lang.StringUtils.rightPad(field, 7);
			}
			field = field.toUpperCase(); 
			stream.write(17); stream.write(4); stream.write(42);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Doctype
		
		//--- start Sending Starters
		field = parametersMap.get("starter").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			field = field.toUpperCase(); 
			stream.write(17); stream.write(4); stream.write(65);
			stream.write(codePage.uni2ebcdic(field.charAt(0)));	
		}
		//------------------ end Sending Starters
		
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
		
		HTTPResponse response = null;
		String temp;
		int count = 10;
		while (count-- > 0 && bk != null) {
			try {
				temp = new String(bk.buffer, CHAR_ENCODING);
				
				if(temp.contains("Print request was submitted")){
					break;
				}
				
				if(temp.contains("INVALID LEGAL ENTERED") || 
						temp.contains("SUBDIVISION LEGAL KEYED NOT FOUND IN MAP EDIT") ||
						temp.contains("field cannot be equal") || 
						temp.toLowerCase().contains("required in low field") ||
						temp.contains("required in High field") ||
						temp.contains("HIGH OR LOW SUBDIVISION SECONDARY") ||
						temp.contains("MAP EDIT NOT FOUND FOR THE NAME GIVEN") ||
						temp.contains("Primary legal must be entered")) {
					response = new HTTPResponse();
					response.is = IOUtils.toInputStream(temp);
					response.returnCode = ATSConnConstants.HTTP_OK;
					response.contentType = "text/html";
					response.headers = new HashMap<String, String>();

					return response;
				}
				
				if(checkDoctype && bk.getOpCode() == 3) {
					response = checkDoctype(temp, stream);
					if(response != null) {
						cleanPrinterQueue();
						return response;
					}
				}
				
				bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
				
				
			} catch (Exception e) {
				logger.error(searchId + ": Some error while sending data", e);
				break;
			}
		}
		return response;
		
	}
	
	private HTTPResponse doSectionSearch(
			HashMap<String, ParametersVector> parametersMap,
			ByteArrayOutputStream stream) 
		throws IOException,InterruptedException{
		String field = null;
		stream.reset();
		
		String temp = new String(bk.buffer, CHAR_ENCODING);
		int count = 0;
		while(!temp.contains("Arb") && !temp.contains("Rng") && count++ < 10) {
			if(bk.getOpCode() == 3) {
				stream.write(11); stream.write(10); stream.write(59);	//write command key - F11
				writeGDS(0, 3, stream.toByteArray());
				stream.reset();
			}
			bk = dsq.get(READ_RESPONSE_TIMEOUT);
			temp = new String(bk.buffer, CHAR_ENCODING);
			
		}
		
		
		stream.write(1); stream.write(3); stream.write(56);	//write command key - F8
		
		
		
		field = parametersMap.get("arb").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(11); stream.write(9);
			for (int i = 0; i < field.length() && i < 15; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		field = parametersMap.get("section").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(11); stream.write(22);
			for (int i = 0; i < field.length() && i < 5; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		field = parametersMap.get("township").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(11); stream.write(31);
			for (int i = 0; i < field.length() && i < 5; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		field = parametersMap.get("range").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(11); stream.write(39);
			for (int i = 0; i < field.length() && i < 8; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		//--- start Sending Start Month/Day
		field = parametersMap.get("startMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming Start Month/Day to 4 characters");
				field = field.substring(0, 5);
			}
			field = field.toUpperCase();
			//stream.write(17); stream.write(4); stream.write(11); 
			stream.write(17); stream.write(13); stream.write(11);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Month/Day
		
		//--- start Sending Start Year
		field = parametersMap.get("startYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming Start Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			//stream.write(17); stream.write(4); stream.write(17); 
			stream.write(17); stream.write(13); stream.write(17);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Year
		
		//--- start Sending End Month/Day
		field = parametersMap.get("endMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming End Month/Day to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			//stream.write(17); stream.write(4); stream.write(24); 
			stream.write(17); stream.write(13); stream.write(24);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Month/Day
		//--- start Sending End Year
		field = parametersMap.get("endYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming End Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			//stream.write(17); stream.write(4); stream.write(30);
			stream.write(17); stream.write(13); stream.write(30);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Year
		
		//--- start Sending Doctype
		field = parametersMap.get("doctype").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 7) {
				logger.info(searchId + ": Trimming Doctype to 7 characters");
				field = field.substring(0, 7);
			} else {
				field = org.apache.commons.lang.StringUtils.rightPad(field, 7);
			}
			field = field.toUpperCase();
			//stream.write(17); stream.write(4); stream.write(43); 
			stream.write(17); stream.write(13); stream.write(42);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Doctype
		
		field = parametersMap.get("gen").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(13); stream.write(56);
			stream.write(codePage.uni2ebcdic(field.charAt(0)));	
		}
		//------------------ end Sending Gen
		
		//--- start Sending Starters
		field = parametersMap.get("starter").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			field = field.toUpperCase(); 
			stream.write(17); stream.write(13); stream.write(63);
			stream.write(codePage.uni2ebcdic(field.charAt(0)));	
		}
		//------------------ end Sending Starters
		
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
		parseIncoming();
		HTTPResponse response = null;
		count = 10;
		boolean redoInvestigative = false;
		while (count-- > 0) {
			try {
				temp = new String(bk.buffer, CHAR_ENCODING);
				
				if(temp.contains("Order updated &") || temp.contains("Starter Created")) {
					redoInvestigative = true;
					break;
				}
				
				if(temp.contains("THE LEGAL IS ALREADY A PART OF THE ORDER")) {
					response = doInvestigativeSectionSearch(parametersMap, stream);
					break;
				}
				if(temp.contains("INVALID LEGAL ENTERED")) {
					response = doInvestigativeSectionSearch(parametersMap, stream);
					break;
				}
				if(temp.contains("field cannot be equal") || 
						temp.contains("Primary legal must be entered") ||
						temp.toLowerCase().contains("required in low field") ||
						temp.contains("HIGH OR LOW ACREAGE SECONDARY") ||
						temp.toLowerCase().contains("required in high field")) {
					response = new HTTPResponse();
					response.is = IOUtils.toInputStream(temp);
					response.returnCode = ATSConnConstants.HTTP_OK;
					response.contentType = "text/html";
					response.headers = new HashMap<String, String>();

					return response;
				}
				bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
				parseIncoming();
				
			} catch (Exception e) {
				logger.error(searchId + ": Some error while sending data", e);
			}
		}
		
		if(redoInvestigative) {
			try {
				cleanPrinterQueue();
			} catch (Exception e) {
				logger.error(searchId + ": Error in reinvestigative section while cleaningPrinterQueue", e);
			} 
			response = doInvestigativeSectionSearch(parametersMap, stream);
		}
		
		return response;
	}
	

	private HTTPResponse doInvestigativeSectionSearch(
			HashMap<String, ParametersVector> parametersMap,
			ByteArrayOutputStream stream) 
		throws IOException,InterruptedException{
		
		logger.debug(searchId + ": doInvestigativeSectionSearch - entering ");
		String field = null;
		boolean checkDoctype = false;
		writeGDS(0, 3, f3Link);
		bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
		parseIncoming();
		
		if(bk.getOpCode() == 2) {
			bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
			parseIncoming();
		}
		
		//be sure we are safe
		if(stream == null) {
			stream = new ByteArrayOutputStream();
		} else {
			stream.reset();	
		}
		
		stream.write(getSelectSearchMenuBytes("2", false));
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);
		
		
		stream.reset();
		String temp = new String(bk.buffer, CHAR_ENCODING);
		while(!temp.contains("Arb") && !temp.contains("Rng")) {
			logger.debug(searchId + ": " + temp);
			stream.write(1); stream.write(2); stream.write(59);	//write command key - F11
			writeGDS(0, 3, stream.toByteArray());
			bk = dsq.get(READ_RESPONSE_TIMEOUT);
			temp = new String(bk.buffer, CHAR_ENCODING);
			stream.reset();
		}
		
		
		
		stream.write(1); stream.write(2); stream.write(56);	//write command key - F8
		
		
		
		field = parametersMap.get("arb").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(2); stream.write(9);
			for (int i = 0; i < field.length() && i < 15; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		field = parametersMap.get("section").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(2); stream.write(22);
			for (int i = 0; i < field.length() && i < 5; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		field = parametersMap.get("township").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(2); stream.write(31);
			for (int i = 0; i < field.length() && i < 5; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		field = parametersMap.get("range").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(2); stream.write(39);
			for (int i = 0; i < field.length() && i < 8; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		field = parametersMap.get("sectionName").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(3); stream.write(10);
			for (int i = 0; i < field.length() && i < 25; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		field = parametersMap.get("platName").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(3); stream.write(47);
			for (int i = 0; i < field.length() && i < 20; i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		field = parametersMap.get("platFlag").getFirstParameter();
		if(!StringUtils.isEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(3); stream.write(68);
			stream.write(codePage.uni2ebcdic(field.charAt(0)));
		}
		
		//--- start Sending Start Month/Day
		field = parametersMap.get("startMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming Start Month/Day to 4 characters");
				field = field.substring(0, 5);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(11); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Month/Day
		
		//--- start Sending Start Year
		field = parametersMap.get("startYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming Start Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(17); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Year
		
		//--- start Sending End Month/Day
		field = parametersMap.get("endMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming End Month/Day to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(24); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Month/Day
		//--- start Sending End Year
		field = parametersMap.get("endYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming End Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(30);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Year
		
		//--- start Sending Doctype
		field = parametersMap.get("doctype").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			checkDoctype = true;
			if(field.length() > 7) {
				logger.info(searchId + ": Trimming Doctype to 7 characters");
				field = field.substring(0, 7);
			} else {
				field = org.apache.commons.lang.StringUtils.rightPad(field, 7);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(42); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Doctype
		
		field = parametersMap.get("gen").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(57);
			stream.write(codePage.uni2ebcdic(field.charAt(0)));	
		}
		//------------------ end Sending Gen
		
		//--- start Sending Starters
		field = parametersMap.get("starter").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			field = field.toUpperCase(); 
			stream.write(17); stream.write(4); stream.write(65);
			stream.write(codePage.uni2ebcdic(field.charAt(0)));	
		}
		//------------------ end Sending Starters
		
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
		
		HTTPResponse response = null;
		
		
		int count = 10;
		while (count-- > 0 && bk != null) {
			try {
				temp = new String(bk.buffer, CHAR_ENCODING);
				
				if(temp.contains("Print request was submitted")){
					break;
				}
				
				if(isInvalidSearchData(temp)) {
					response = new HTTPResponse();
					response.is = IOUtils.toInputStream(temp);
					response.returnCode = ATSConnConstants.HTTP_OK;
					response.contentType = "text/html";
					response.headers = new HashMap<String, String>();

					return response;
				}
				if(checkDoctype && bk.getOpCode() == 3) {
					response = checkDoctype(temp, stream);
					if(response != null) {
						cleanPrinterQueue();
						return response;
					}
				}
				
				bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
				
				
			} catch (Exception e) {
				logger.error(searchId + ": Some error while sending data", e);
				break;
			}
		}
		return response;
	}

	private HTTPResponse checkDoctype(String originalResponse, ByteArrayOutputStream stream) 
	throws IOException, InterruptedException {
		if(originalResponse.contains("The Highlighted Value(s) are Invalid")) {
			stream.reset();
			stream.write(4); stream.write(71); stream.write(241); stream.write(17); stream.write(4); stream.write(71);
			writeGDS(0, 3, stream.toByteArray());
			bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
			
			while(bk.getOpCode() != 3) {
				bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen	
			}
			
			String temp = new String(bk.buffer, CHAR_ENCODING);
			if(temp.contains("The Highlighted Value(s) are Invalid")) {
				stream.reset();
				stream.write(4); stream.write(71); stream.write(241);
				writeGDS(0, 3, stream.toByteArray());
				bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
				while(bk.getOpCode() != 3) {
					bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen	
				}
				temp = new String(bk.buffer, CHAR_ENCODING);
				if(temp.contains("The Highlighted Value(s) are Invalid")) {
					disconnect();
					return null;
				} else {
					stream.reset();
					stream.write(4); stream.write(71); stream.write(241);
					writeGDS(0, 3, stream.toByteArray());
					bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
					while(bk.getOpCode() != 3) {
						bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen	
					}
					
					HTTPResponse response = new HTTPResponse();
					response.is = IOUtils.toInputStream("Invalid Doctype Entered");
					response.returnCode = ATSConnConstants.HTTP_OK;
					response.contentType = "text/html";
					response.headers = new HashMap<String, String>();
					
					return response;
				}
			}
			
		}
		return null;
	}

	private HTTPResponse doSubdivisionLookup(HashMap<String, ParametersVector> parametersMap,
			ByteArrayOutputStream stream) 
		throws IOException,InterruptedException {
				
		String field = null;
		stream.write(3); stream.write(26); stream.write(52);	//write command key - F4
		
		field = parametersMap.get("firstSubdivisionName").getFirstParameter();
		if(StringUtils.isEmpty(field)) {
			field = parametersMap.get("subdivisionName").getFirstParameter();
			if(StringUtils.isEmpty(field)) {
				field = "";
			}
		}
		
		stream.write(17); stream.write(3); stream.write(15);	//write data
		field = field.toUpperCase();
		for (int i = 0; i < field.length() && i < 28; i++) {
			stream.write(codePage.uni2ebcdic(field.charAt(i)));
		}
		
		writeGDS(0, 3, stream.toByteArray());					//send full command
		bk = dsq.get(READ_RESPONSE_TIMEOUT);					//read screen
		
		field = parametersMap.get("prevLink").getFirstParameter();
		if(StringUtils.isNotEmpty(field) && !"0".equals(field)) {
			//the previous link was pushed so we do a page up
			stream.reset();
			stream.write(7); stream.write(5); stream.write(244);	//write data
			
			writeGDS(0, 3, stream.toByteArray());					//send full command
			bk = dsq.get(READ_RESPONSE_TIMEOUT);					//read screen
			
		} else {
			field = parametersMap.get("nextLink").getFirstParameter();
			if(StringUtils.isNotEmpty(field)&& !"0".equals(field)) {
				//the previous link was pushed so we do a page down
				stream.reset();
				stream.write(7); stream.write(5); stream.write(245);	//write data
				
				writeGDS(0, 3, stream.toByteArray());					//send full command
				bk = dsq.get(READ_RESPONSE_TIMEOUT);					//read screen
				
			}	
		}
		
		HTTPResponse response = new HTTPResponse();
		response.is = IOUtils.toInputStream(new String(bk.getBuffer(), CHAR_ENCODING));
		response.returnCode = ATSConnConstants.HTTP_OK;
		response.contentType = "text/html";
		response.headers = new HashMap<String, String>();

		return response;

	}
	
	private HTTPResponse doSubdivisionSecondaryLookup(HashMap<String, ParametersVector> parametersMap,
			ByteArrayOutputStream stream) 
		throws IOException,InterruptedException {
				
		String field = null;
		stream.write(3); stream.write(26); stream.write(52);	//write command key - F4
		
		field = parametersMap.get("firstSubdivisionName").getFirstParameter();
		if(StringUtils.isEmpty(field)) {
			field = parametersMap.get("subdivisionName").getFirstParameter();
			if(StringUtils.isEmpty(field)) {
				field = "";
			}
		}
		
		stream.write(17); stream.write(3); stream.write(15);	//write data
		field = field.toUpperCase();
		for (int i = 0; i < field.length() && i < 28; i++) {
			stream.write(codePage.uni2ebcdic(field.charAt(i)));
		}
		
		writeGDS(0, 3, stream.toByteArray());					//send full command
		bk = dsq.get(READ_RESPONSE_TIMEOUT);					//read screen
		
		String rsResponce = new String(bk.getBuffer(), CHAR_ENCODING);
		
		int tempIndex = rsResponce.indexOf("F3=Exit");
		if(tempIndex >= 0) {
			rsResponce = rsResponce.substring(tempIndex + "F3=Exit".length() + 17);
			rsResponce = rsResponce.replaceAll("[^\\p{Alnum}\\p{Punct}\\s]+", " ");
			rsResponce = rsResponce.replaceAll("", " ");
		
			Matcher matcher = GenericSKLD.SUBDIVISION_LOOKUP_PATTERN.matcher(rsResponce);
			
			String mapIdBook = parametersMap.get("mapidbook").getFirstParameter();
			if(StringUtils.isEmpty(mapIdBook)) {
				mapIdBook = "";
			}
			String mapIdPage = parametersMap.get("mapidpage").getFirstParameter();
			if(StringUtils.isEmpty(mapIdPage)) {
				mapIdPage = "";
			}
			
			int indexForLink = 7;
			int foundIndex = 0;
			
			while (matcher.find()) {
				try {
					
					
					
					if(mapIdBook.equals(matcher.group(2)) && mapIdPage.equals(matcher.group(3)) ) {
						//found correct legal
						foundIndex = indexForLink;
						
						stream.reset();
						//stream.write(4); stream.write(17); stream.write(241);	//send enter
						stream.write(7); stream.write(17); stream.write(241);	//send enter
						
						stream.write(17); stream.write(foundIndex); stream.write(5);	//write data
						
						stream.write(codePage.uni2ebcdic('V'));
						
						
						writeGDS(0, 3, stream.toByteArray());					//send full command
						bk = dsq.get(READ_RESPONSE_TIMEOUT);					//read screen
						
						byte[] sendScreenSecondaryLegal = new byte[] {
								(byte)4, (byte)18, (byte)0, (byte)0, (byte)24, (byte)80, (byte)2, (byte)-123, (byte)2, (byte)53, (byte)33, (byte)64, (byte)-59, (byte)-124, 
								(byte)-119, (byte)-93, (byte)64, (byte)-45, (byte)-106, (byte)-106, (byte)-110, (byte)-92, (byte)-105, (byte)64, (byte)-62, (byte)-88, 
								(byte)64, (byte)-43, (byte)-127, (byte)-108, (byte)-123, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, 
								(byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, 
								(byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, 
								(byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)-29, (byte)-44, (byte)-14, 
								(byte)-13, (byte)-15, (byte)-16, (byte)-62, (byte)-26, (byte)64, (byte)64, (byte)64, (byte)32, (byte)33, (byte)64, (byte)-59, (byte)-124, 
								(byte)-119, (byte)-93, (byte)64, (byte)-45, (byte)-106, (byte)-106, (byte)-110, (byte)-92, (byte)-105, (byte)64, (byte)-62, (byte)-88, 
								(byte)64, (byte)-43, (byte)-127, (byte)-108, (byte)-123, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, 
								(byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, 
								(byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, 
								(byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)-29, (byte)-44, (byte)-14, 
								(byte)-13, (byte)-15, (byte)-16, (byte)-62, (byte)-26, (byte)64, (byte)64, (byte)64, (byte)32, (byte)33, (byte)64, (byte)32, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)33, (byte)64, (byte)32, (byte)33, (byte)64, (byte)32, (byte)-41, 
								(byte)-106, (byte)-94, (byte)-119, (byte)-93, (byte)-119, (byte)-106, (byte)-107, (byte)64, (byte)-93, (byte)-106, (byte)122, (byte)36, (byte)0,
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)32, (byte)-29, (byte)-88, (byte)-105, (byte)-123, (byte)122, (byte)33, (byte)-30, (byte)32, (byte)0, (byte)0, (byte)33, (byte)64, 
								(byte)32, (byte)33, (byte)64, (byte)32, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)33, 
								(byte)64, (byte)32, (byte)33, (byte)64, (byte)38, (byte)-42, (byte)-105, (byte)-93, (byte)38, (byte)-59, (byte)-124, (byte)-119, (byte)-93, 
								(byte)64, (byte)-107, (byte)-127, (byte)-108, (byte)-123, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, 
								(byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, 
								(byte)64, (byte)64, (byte)64, (byte)64, (byte)38, (byte)-30, (byte)97, (byte)-41, (byte)64, (byte)64, (byte)38, (byte)-41, (byte)-103, 
								(byte)-119, (byte)-108, (byte)-127, (byte)-103, (byte)-88, (byte)64, (byte)-45, (byte)-123, (byte)-121, (byte)-127, (byte)-109, (byte)64, 
								(byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)32, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)33, 
								(byte)64, (byte)32, (byte)33, (byte)64, (byte)32, (byte)36, (byte)-27, (byte)32, (byte)32, (byte)-61, (byte)-42, (byte)-39, (byte)-43, (byte)-59, 
								(byte)-39, (byte)-30, (byte)-29, (byte)-42, (byte)-43, (byte)-59, (byte)64, (byte)-56, (byte)-42, (byte)-44, (byte)-59, (byte)-30, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)64, (byte)64, (byte)64, (byte)64, (byte)-43, (byte)32, (byte)-28, (byte)-15, (byte)97, (byte)-15, (byte)-14, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)33, (byte)64, (byte)32, (byte)33, (byte)64, (byte)32, (byte)36, (byte)0, (byte)32, (byte)32, (byte)-61, (byte)-42, (byte)-39, (byte)-43, (byte)-59, (byte)-39, (byte)-30, (byte)-29, (byte)-42, (byte)-43, (byte)-59, (byte)64, (byte)-39, (byte)-59, (byte)-30, (byte)-55, (byte)-60, (byte)-59, (byte)-43, (byte)-61, 
								(byte)-59, (byte)-30, (byte)64, (byte)-41, (byte)-60, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)64, (byte)64, (byte)64, (byte)64, (byte)-43, (byte)32, (byte)-12, (byte)-15, (byte)97, (byte)-15, (byte)-13, (byte)-10, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)33, (byte)64, (byte)32, (byte)33, (byte)64, (byte)32, (byte)36, (byte)0, (byte)32, (byte)32, (byte)-61, (byte)-42, (byte)-39, (byte)-43, (byte)-59, (byte)-39, (byte)-30, (byte)-29, (byte)-42, (byte)-43, (byte)-59, (byte)107, (byte)64, (byte)-58, (byte)-45, (byte)-57, (byte)64, (byte)-15, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)32, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)32, (byte)-13, (byte)-13, (byte)97, (byte)-7, (byte)-7, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)33, (byte)64, (byte)32, (byte)33, (byte)64, (byte)32, (byte)36, (byte)0, (byte)32, (byte)32, (byte)-61, (byte)-42, (byte)-39, (byte)-43, (byte)-59, (byte)-39, (byte)-30, (byte)-29, (byte)-42, (byte)-43, (byte)-59, (byte)107, (byte)64, (byte)-41, (byte)-62, (byte)-57, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)32, (byte)64, (byte)64, (byte)64, (byte)64, (byte)-43, (byte)32, (byte)-60, (byte)-13, (byte)-12, (byte)97, (byte)-15, (byte)-13, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)33, (byte)64, (byte)32, (byte)33, (byte)64, (byte)32, (byte)36, (byte)0, (byte)32, (byte)32, (byte)-61, (byte)-42, (byte)-39, (byte)-43, (byte)-58, (byte)-42, (byte)-39, (byte)-29, (byte)-56, (byte)64, (byte)-56, (byte)-29, (byte)-30, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)64, (byte)64, 
								(byte)64, (byte)64, (byte)64, (byte)32, (byte)-10, (byte)97, (byte)-15, (byte)-9, (byte)-63, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)33, (byte)64, (byte)32, (byte)33, (byte)64, (byte)32, (byte)36, (byte)0, (byte)32, (byte)32, (byte)-61, (byte)-42, (byte)-39, (byte)-43, (byte)-26, (byte)-63, (byte)-45, (byte)-45, (byte)64, (byte)-61, (byte)-42, (byte)-43, (byte)-60, (byte)-42, (byte)107, (byte)64, (byte)-29, (byte)-56, (byte)-59, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)32, (byte)-61, (byte)-12, (byte)97, (byte)-15, (byte)-16, (byte)-8, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)33, (byte)64, (byte)32, (byte)33, (byte)64, (byte)32, (byte)36, (byte)0, (byte)32, (byte)32, (byte)-61, (byte)-42, (byte)-39, (byte)-42, (byte)-43, (byte)-63, (byte)64, (byte)-41, (byte)-63, (byte)-39, (byte)-46, (byte)64, (byte)-61, (byte)-42, (byte)-43, (byte)-60, (byte)-42, (byte)-30, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)32, (byte)-13, (byte)-11, (byte)97, (byte)-11, (byte)-14, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)33, (byte)64, (byte)32, (byte)33, (byte)64, (byte)32, (byte)36, (byte)0, 
								(byte)32, (byte)32, (byte)-61, (byte)-42, (byte)-39, (byte)-42, (byte)-43, (byte)-63, (byte)64, (byte)-41, (byte)-45, (byte)-63, (byte)-61, (byte)-59, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)32, (byte)-61, (byte)-12, (byte)-11, (byte)97, (byte)-10, (byte)-11, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)33, (byte)64, (byte)32, (byte)33, (byte)64, (byte)32, (byte)36, (byte)0, (byte)32, (byte)32, (byte)-61, 
								(byte)-42, (byte)-39, (byte)-42, (byte)-43, (byte)-63, (byte)64, (byte)-30, (byte)-40, (byte)-28, (byte)-63, (byte)-39, (byte)-59, (byte)64, (byte)-29, (byte)-42, (byte)-26, (byte)-43, (byte)-56, (byte)-42, (byte)-28, (byte)-30, (byte)-59, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)32, (byte)-61, (byte)-15, (byte)-11, (byte)97, (byte)-10, (byte)-12, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)33, (byte)64, (byte)32, (byte)33, (byte)64, (byte)32, (byte)36, (byte)0, (byte)32, (byte)32, (byte)-61, (byte)-42, 
								(byte)-39, (byte)-42, (byte)-43, (byte)-63, (byte)-60, (byte)-42, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)32, (byte)-13, (byte)97, (byte)-14, (byte)-16, (byte)-63, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)33, (byte)64, (byte)32, (byte)33, (byte)64, (byte)32, (byte)36, (byte)0, (byte)32, (byte)32, (byte)-61, (byte)-42, (byte)-39, (byte)-42, (byte)-43, (byte)-63, (byte)-60, (byte)-42, (byte)64, (byte)-63, (byte)-41, (byte)-29, (byte)-30, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)32, (byte)-41, (byte)-15, (byte)97, (byte)-7, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)33, (byte)64, (byte)32, (byte)33, (byte)64, (byte)32, (byte)36, (byte)0, (byte)32, (byte)32, (byte)-61, (byte)-42, (byte)-39, (byte)-42, (byte)-43, (byte)-63, (byte)-60, (byte)-42, (byte)64, (byte)-63, (byte)-41, (byte)-29, (byte)-30, (byte)107, (byte)64, (byte)-58, (byte)-45, (byte)-57, (byte)64, (byte)-14, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)32, (byte)-41, (byte)-15, (byte)97, (byte)-13, (byte)-12, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)33, (byte)64, (byte)32, (byte)33, (byte)64, (byte)32, (byte)36, (byte)0, (byte)32, (byte)32, (byte)-61, (byte)-42, (byte)-39, (byte)-42, (byte)-43, (byte)-63, (byte)-60, (byte)-42, (byte)64, (byte)-56, (byte)-29, (byte)-30, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)32, (byte)-15, (byte)-14, (byte)97, (byte)-15, (byte)-12, (byte)-63, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)33, (byte)64, (byte)32, (byte)33, (byte)64, (byte)32, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)33, (byte)64, (byte)32, (byte)33, (byte)64, 
								(byte)-30, (byte)126, (byte)-30, (byte)-123, (byte)-109, (byte)-123, (byte)-125, (byte)-93, 
								(byte)64, (byte)64, (byte)64, (byte)-27, (byte)126, (byte)-27, (byte)-119, (byte)-123, (byte)-90, (byte)64, (byte)64, (byte)64, (byte)64, (byte)-58, (byte)-13, (byte)126, 
								(byte)-59, (byte)-89, (byte)-119, (byte)-93, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)64, (byte)32, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)32, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, 
								(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)34, (byte)64, (byte)32, (byte)0, (byte)14, (byte)36, (byte)1, (byte)0, (byte)0, (byte)0, (byte)50, (byte)80, (byte)32, (byte)0, (byte)0, (byte)36, (byte)1, (byte)-28, (byte)1, (byte)0, (byte)1, (byte)64, (byte)32, (byte)0, (byte)0, (byte)36, (byte)2, (byte)52, (byte)0, (byte)0, (byte)1, (byte)64, (byte)32, (byte)0, (byte)0, (byte)36, (byte)2, (byte)-124, (byte)0, (byte)0, (byte)1, (byte)64, (byte)32, (byte)0, (byte)0, (byte)36, (byte)2, (byte)-44, (byte)0, (byte)0, (byte)1, (byte)64, (byte)32, (byte)0, (byte)0, (byte)36, (byte)3, (byte)36, (byte)0, (byte)0, (byte)1, (byte)64, (byte)32, (byte)0, (byte)0, 
								(byte)36, (byte)3, (byte)116, (byte)0, (byte)0, (byte)1, (byte)64, (byte)32, (byte)0, (byte)0, 
								(byte)36, (byte)3, (byte)-60, (byte)0, (byte)0, (byte)1, (byte)64, (byte)32, (byte)0, (byte)0, (byte)36, (byte)4, (byte)20, (byte)0, (byte)0, (byte)1, (byte)64, 
								(byte)32, (byte)0, (byte)0, (byte)36, (byte)4, (byte)100, (byte)0, (byte)0, (byte)1, (byte)64, (byte)32, (byte)0, (byte)0, (byte)36, (byte)4, (byte)-76, (byte)0, (byte)0, (byte)1, (byte)64, (byte)32, (byte)0, (byte)0, (byte)36, (byte)5, (byte)4, (byte)0, (byte)0, (byte)1, (byte)64, (byte)32, (byte)0, (byte)0, (byte)36, (byte)5, (byte)84, (byte)0, (byte)0, (byte)1, (byte)64, (byte)32, (byte)0, (byte)0, (byte)36, (byte)5, (byte)-92, (byte)0, (byte)0, (byte)1, (byte)64, (byte)32, (byte)0, (byte)0
							};
						
						writeGDS(0, 3, sendScreenSecondaryLegal);					//send full command
						bk = dsq.get(READ_RESPONSE_TIMEOUT);
						
						String tempResponse = new String(bk.getBuffer(), CHAR_ENCODING);
						
						rsResponce = null;
						
						do {
							
							if(rsResponce == null) {
								rsResponce = tempResponse;
							} else {
								rsResponce += GenericSKLD.DELIMITER_INTERESTING_DATA + tempResponse;
							}
							
							writeGDS(0, 3, new byte[] {(byte)11,(byte)3,(byte)245});
							bk = dsq.get(READ_RESPONSE_TIMEOUT);
							tempResponse = new String(bk.getBuffer(), CHAR_ENCODING);
							
						}
						while(!tempResponse.contains("Attempt to roll past bottom of list")); 
						
						//send F12
						writeGDS(0, 3, new byte[] {(byte)1,(byte)1,(byte)60});
						bk = dsq.get(READ_RESPONSE_TIMEOUT);
						while(bk.getOpCode() != 3) {
							bk = dsq.get(READ_RESPONSE_TIMEOUT);	
						}
						
						//finished getting VIEW screen
						break;
						
					}
					
					
				} catch (Exception e) {
					logger.error("Some error while parsing " + matcher.group());
					break;
				} finally {
					indexForLink++;
				}
			}
			
			
		} else {
			//bad bad boy
		}
		
		/*
		field = parametersMap.get("prevLink").getFirstParameter();
		if(StringUtils.isNotEmpty(field) && !"0".equals(field)) {
			//the previous link was pushed so we do a page up
			stream.reset();
			stream.write(7); stream.write(5); stream.write(244);	//write data
			
			writeGDS(0, 3, stream.toByteArray());					//send full command
			bk = dsq.get(READ_RESPONSE_TIMEOUT);					//read screen
			
		} else {
			field = parametersMap.get("nextLink").getFirstParameter();
			if(StringUtils.isNotEmpty(field)&& !"0".equals(field)) {
				//the previous link was pushed so we do a page down
				stream.reset();
				stream.write(7); stream.write(5); stream.write(245);	//write data
				
				writeGDS(0, 3, stream.toByteArray());					//send full command
				bk = dsq.get(READ_RESPONSE_TIMEOUT);					//read screen
				
			}	
		}
		*/
		HTTPResponse response = new HTTPResponse();
		response.is = IOUtils.toInputStream(rsResponce);
		response.returnCode = ATSConnConstants.HTTP_OK;
		response.contentType = "text/html";
		response.headers = new HashMap<String, String>();

		return response;

	}

	/**
	 * @param parametersMap
	 * @param stream
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private HTTPResponse doBookPageInstrumentSearch(
			HashMap<String, ParametersVector> parametersMap,
			ByteArrayOutputStream stream) throws IOException,
			InterruptedException {
		String field;
		stream.write(2); stream.write(2); stream.write(56);	//send command key
		//stream.write(20); stream.write(46); stream.write(56);	//send command key
		boolean checkDoctype = false;
		field = parametersMap.get("instrumentNo").getFirstParameter();
		if(StringUtils.isEmpty(field)) {
			field = parametersMap.get("book").getFirstParameter().trim();
			if(StringUtils.isEmpty(field)) {
				throw new RuntimeException("BookField Is Empty");
			}
			if(StringUtils.isEmpty(parametersMap.get("page").getFirstParameter())) {
				throw new RuntimeException("PageField Is Empty");
			}
			field+= "/" + parametersMap.get("page").getFirstParameter().trim();
			
			stream.write(17); stream.write(2); stream.write(15);	//send command key
			//stream.write(17); stream.write(19); stream.write(16);	//send command key
			
			field = field.toUpperCase();
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		} else {
			
			if(parametersMap.get("year") != null && 
					!StringUtils.isEmpty(parametersMap.get("year").getFirstParameter())) {
				field+= "-" + parametersMap.get("year").getFirstParameter().trim();
			} else {
				//field+= "-*";
			}
			
			stream.write(17); stream.write(2); stream.write(50);	//send command key
			//stream.write(17); stream.write(19); stream.write(50);	//send command key
			field = field.toUpperCase();
			 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		//--- start Sending Start Month/Day
		field = parametersMap.get("startMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming Start Month/Day to 4 characters");
				field = field.substring(0, 5);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(11); 
			//stream.write(17); stream.write(20); stream.write(11);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Month/Day
		
		//--- start Sending Start Year
		field = parametersMap.get("startYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming Start Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(17); 
			//stream.write(17); stream.write(20); stream.write(17);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Year
		
		//--- start Sending End Month/Day
		field = parametersMap.get("endMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming End Month/Day to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(24); 
			//stream.write(17); stream.write(20); stream.write(24);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Month/Day
		//--- start Sending End Year
		field = parametersMap.get("endYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming End Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(30);
			//stream.write(17); stream.write(20); stream.write(30);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Year
		
		//--- start Sending Doctype
		field = parametersMap.get("doctype").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 7) {
				logger.info(searchId + ": Trimming Doctype to 7 characters");
				field = field.substring(0, 7);
			} else {
				field = org.apache.commons.lang.StringUtils.rightPad(field, 7);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(42); 
			//42 - investigative and 43 - inside order
			//stream.write(17); stream.write(20); stream.write(43);
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Doctype
		
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
		HTTPResponse response = null;
		String temp;
		int count = 10;
		while (count-- > 0) {
			try {
				temp = new String(bk.buffer, "cp037");
				
				if(temp.contains("Print request was submitted")){
					break;
				}
				
				if(temp.contains("Order updated &")) {
					break;
				}
				
				if(temp.contains("Welcome to SKLD Search Menu")) {
					break;
				}
				
				if(checkDoctype && bk.getOpCode() == 3) {
					response = checkDoctype(temp, stream);
					if(response != null) {
						cleanPrinterQueue();
						return response;
					}
				}
				
				bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
				parseIncoming();
				
			} catch (Exception e) {
				logger.error(searchId + ": Some error while sending data", e);
				if(bk == null) {
					return null;
				}
			}
		}
		return null;
	}

	/**
	 * @param parametersMap
	 * @param stream
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private HTTPResponse doAddressSearch(
			HashMap<String, ParametersVector> parametersMap,
			ByteArrayOutputStream stream) throws IOException,
			InterruptedException {
		String field;
		stream.write(2); stream.write(2); stream.write(56);	//send command key
		boolean checkDoctype = false;
		//--- start street name
		field = parametersMap.get("StreetName").getFirstParameter();
		if(StringUtils.isEmpty(field)) {
			throw new RuntimeException("Street Name Is Empty");
		}
		if(field.length() > 40) {
			logger.info(searchId + ": Trimming Street Name to 40 characters");
			field = field.substring(0, 40);
		}
		field = field.toUpperCase();
		stream.write(17); stream.write(2); stream.write(11); 
		for (int i = 0; i < field.length(); i++) {
			stream.write(codePage.uni2ebcdic(field.charAt(i)));
		}
		//--- end street name
		
		//--- start street number
		field = parametersMap.get("StreetNumber").getFirstParameter().trim();
		if(!StringUtils.isEmpty(field)) {
			stream.write(17); stream.write(2); stream.write(71);	//send command key
			
			field = field.toUpperCase();
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//--- end street number
		
		//--- start city name
		if(parametersMap.get("cityName") != null) {
			field = parametersMap.get("cityName").getFirstParameter().trim();
			if(!StringUtils.isEmpty(field)) {
				stream.write(17); stream.write(2); stream.write(11);	//send command key
				
				field = field.toUpperCase();
				for (int i = 0; i < field.length(); i++) {
					stream.write(codePage.uni2ebcdic(field.charAt(i)));
				}	
			}
		}
		//--- end city name
		
		//--- start unit
		if(parametersMap.get("unit") != null) {
			field = parametersMap.get("unit").getFirstParameter().trim();
			if(!StringUtils.isEmpty(field)) {
				stream.write(17); stream.write(3); stream.write(38);	//send command key
				
				field = field.toUpperCase();
				for (int i = 0; i < field.length(); i++) {
					stream.write(codePage.uni2ebcdic(field.charAt(i)));
				}	
			}
		}
		//--- end unit
		
		//--- start suffix
		if(parametersMap.get("suffix") != null) {
			field = parametersMap.get("suffix").getFirstParameter().trim();
			if(!StringUtils.isEmpty(field)) {
				stream.write(17); stream.write(3); stream.write(52);	//send command key
				
				field = field.toUpperCase();
				for (int i = 0; i < field.length(); i++) {
					stream.write(codePage.uni2ebcdic(field.charAt(i)));
				}	
			}
		}
		//--- end suffix
		
		
		//--- start Sending Start Year
		field = parametersMap.get("startYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming Start Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(17); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Year
		
		//--- start Sending End Month/Day
		field = parametersMap.get("endMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming End Month/Day to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(24); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Month/Day
		//--- start Sending End Year
		field = parametersMap.get("endYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming End Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(30); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Year
		
		//--- start Sending Doctype
		field = parametersMap.get("doctype").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 7) {
				logger.info(searchId + ": Trimming Doctype to 7 characters");
				field = field.substring(0, 7);
			} else {
				field = org.apache.commons.lang.StringUtils.rightPad(field, 7);
			}
			checkDoctype = true;
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(42); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Doctype
		
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
		HTTPResponse response = null;
		try {
			String temp = new String(bk.buffer, CHAR_ENCODING);
			if(checkDoctype && bk.getOpCode() == 3) {
				response = checkDoctype(temp, stream);
				if(response != null) {
					cleanPrinterQueue();
					return response;
				}
			}
		} catch (Exception e) {
			logger.error(searchId + ": Error in Address Search, while checking for doctype error", e);
		}
		
		return null;
	}

	/**
	 * @param parametersMap
	 * @param stream
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private HTTPResponse doNameSearch(
			HashMap<String, ParametersVector> parametersMap,
			ByteArrayOutputStream stream) throws IOException,
			InterruptedException {
		String field;
		stream.write(17); stream.write(46); stream.write(56);	//send command key
		boolean checkDoctype = false;
		//--- start Sending LastName
		field = parametersMap.get("lastName").getFirstParameter();
		if(field == null) {
			throw new RuntimeException("No Last Name and You must have one");
		}
		if(field.length() > 40) {
			logger.info(searchId + ": Trimming Last Name to 40 characters");
			field = field.substring(0, 40);
		}
		field = field.toUpperCase();
		stream.write(17); stream.write(15); stream.write(10); 
		for (int i = 0; i < field.length(); i++) {
			stream.write(codePage.uni2ebcdic(field.charAt(i)));
		}
		//------------------- end Sending LastName
		
		//--- start Sending FirstName
		field = parametersMap.get("firstName").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 10) {
				logger.info(searchId + ": Trimming First Name to 10 characters");
				field = field.substring(0, 10);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(15); stream.write(51); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending FirstName
		
		//--- start Sending MiddleInitial
		field = parametersMap.get("middleInitial").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 1) {
				logger.info(searchId + ": Trimming MiddleInitial to 1 characters");
				field = field.substring(0, 1);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(15); stream.write(62); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending MiddleInitial
		
		//--- start Sending Additional FirstName
		field = parametersMap.get("additionalFirstName").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 10) {
				logger.info(searchId + ": Trimming Additional First Name to 10 characters");
				field = field.substring(0, 10);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(15); stream.write(65); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Additional FirstName
		
		//--- start Sending Additional MiddleInitial
		field = parametersMap.get("additionalMiddleInitial").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 1) {
				logger.info(searchId + ": Trimming Additional Middle Initial to 1 characters");
				field = field.substring(0, 1);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(15); stream.write(76); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Additional MiddleInitial
		
		//--- start Sending Prop
		field = parametersMap.get("propType").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			field = field.toUpperCase();
			if(field.charAt(0) != 'N') {
				stream.write(17); stream.write(16); stream.write(18); 
				stream.write(codePage.uni2ebcdic(field.charAt(0)));
			}
		}
		//------------------ end Sending Prop
		
		
		//--- start Sending Start Month/Day
		field = parametersMap.get("startMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming Start Month/Day to 4 characters");
				field = field.substring(0, 5);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(11); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Month/Day
		//--- start Sending Start Year
		field = parametersMap.get("startYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming Start Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(17); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Year
		
		//--- start Sending End Month/Day
		field = parametersMap.get("endMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming End Month/Day to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(24); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Month/Day
		//--- start Sending End Year
		field = parametersMap.get("endYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming End Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(30); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Year
		
		//--- start Sending Doctype
		field = parametersMap.get("doctype").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 7) {
				logger.info(searchId + ": Trimming Doctype to 7 characters");
				field = field.substring(0, 7);
			}
			checkDoctype = true;
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(43); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Doctype
		
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
		HTTPResponse response = null;
		String temp;
		int count = 10;
		boolean didInvestigative = false;
		while (count-- > 0) {
			try {
				temp = new String(bk.buffer, "cp037");
				
				if(temp.contains("Order updated &")) {
					break;
				}
				
				if(temp.contains("THE NAME IS ALREADY A PART OF THE ORDER")) {
					doInvestigativeNameSearch(parametersMap, stream);
					didInvestigative = true;
					break;
				}
				
				if(checkDoctype && bk.getOpCode() == 3) {
					response = checkDoctype(temp, stream);
					if(response != null) {
						cleanPrinterQueue();
						return response;
					}
				}
				
				bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
				
			} catch (Exception e) {
				logger.error(searchId + ": Some error while sending data", e);
			}
		}
		
		if(!didInvestigative) {
			try {
				cleanPrinterQueue();
			} catch (Exception e) {
				logger.error(searchId + ": Error in doNameSearch while cleaningPrinterQueue", e);
			} 
			return doInvestigativeNameSearch(parametersMap, stream);
		}
		return null;
	}

	private HTTPResponse doInvestigativeNameSearch(
			HashMap<String, ParametersVector> parametersMap,
			ByteArrayOutputStream stream) throws IOException, InterruptedException {
		logger.debug(searchId + ": doInvestigativeNameSearch - entering ");
		String field = null;
		writeGDS(0, 3, f3Link);
		bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
		parseIncoming();
		boolean checkDoctype = false;
		if(bk.getOpCode() == 2) {
			bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
			parseIncoming();
		}
		
		//be sure we are safe
		if(stream == null) {
			stream = new ByteArrayOutputStream();
		} else {
			stream.reset();	
		}
		
		stream.write(getSelectSearchMenuBytes("3", false));
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);
		stream.reset();
		
		
		stream.write(2); stream.write(10); stream.write(56);	//send command key
		
		//--- start Sending LastName
		field = parametersMap.get("lastName").getFirstParameter();
		if(field == null) {
			throw new RuntimeException("No Last Name and You must have one");
		}
		if(field.length() > 40) {
			logger.info(searchId + ": Trimming Last Name to 40 characters");
			field = field.substring(0, 40);
		}
		field = field.toUpperCase();
		logger.debug(searchId + ": doInvestigativeNameSearch - lastName " + field);
		stream.write(17); stream.write(2); stream.write(10); 
		for (int i = 0; i < field.length(); i++) {
			stream.write(codePage.uni2ebcdic(field.charAt(i)));
		}
		//------------------- end Sending LastName
		
		//--- start Sending FirstName
		field = parametersMap.get("firstName").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 10) {
				logger.info(searchId + ": Trimming First Name to 10 characters");
				field = field.substring(0, 10);
			}
			field = field.toUpperCase();
			logger.debug(searchId + ": doInvestigativeNameSearch - firstName " + field);
			stream.write(17); stream.write(2); stream.write(51); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending FirstName
		
		//--- start Sending MiddleInitial
		field = parametersMap.get("middleInitial").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 1) {
				logger.info(searchId + ": Trimming MiddleInitial to 1 characters");
				field = field.substring(0, 1);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(2); stream.write(62); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending MiddleInitial
		
		//--- start Sending Additional FirstName
		field = parametersMap.get("additionalFirstName").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 10) {
				logger.info(searchId + ": Trimming Additional First Name to 10 characters");
				field = field.substring(0, 10);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(2); stream.write(65); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Additional FirstName
		
		//--- start Sending Additional MiddleInitial
		field = parametersMap.get("additionalMiddleInitial").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 1) {
				logger.info(searchId + ": Trimming Additional Middle Initial to 1 characters");
				field = field.substring(0, 1);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(2); stream.write(76); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Additional MiddleInitial
		
		//--- start Sending PropType
		field = parametersMap.get("propType").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			field = field.toUpperCase();
			if(field.charAt(0) != 'N') {
				stream.write(17); stream.write(3); stream.write(18); 
				stream.write(codePage.uni2ebcdic(field.charAt(0)));
			}
		}
		//------------------ end Sending PropType
		
		
		//--- start Sending Start Month/Day
		field = parametersMap.get("startMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming Start Month/Day to 4 characters");
				field = field.substring(0, 5);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(11); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Month/Day
		//--- start Sending Start Year
		field = parametersMap.get("startYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming Start Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(17); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Start Year
		
		//--- start Sending End Month/Day
		field = parametersMap.get("endMonthDay").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 5) {
				logger.info(searchId + ": Trimming End Month/Day to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(24); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Month/Day
		//--- start Sending End Year
		field = parametersMap.get("endYear").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 4) {
				logger.info(searchId + ": Trimming End Year to 4 characters");
				field = field.substring(0, 4);
			}
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(30); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending End Year
		
		//--- start Sending Doctype
		field = parametersMap.get("doctype").getFirstParameter();
		if(ro.cst.tsearch.utils.StringUtils.isNotEmpty(field)) {
			if(field.length() > 7) {
				logger.info(searchId + ": Trimming Doctype to 7 characters");
				field = field.substring(0, 7);
			}
			checkDoctype = true;
			field = field.toUpperCase();
			stream.write(17); stream.write(4); stream.write(43); 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}	
		}
		//------------------ end Sending Doctype
		
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
		HTTPResponse response = null;
		String temp;
		try {
			temp = new String(bk.buffer, "cp037");
			if(checkDoctype && bk.getOpCode() == 3) {
				response = checkDoctype(temp, stream);
				if(response != null) {
					cleanPrinterQueue();
					return response;
				}
			}
		} catch (Exception e) {
			logger.error(searchId + ": Error in doInvestigativeNameSearch while checking for doctype Error",e);
		}
		
		logger.debug(searchId + ": doInvestigativeNameSearch - exiting ");
		return null;
	}

	/**
	 * @param parametersMap
	 * @param stream
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private HTTPResponse doOrderSearch(
			HashMap<String, ParametersVector> parametersMap,
			ByteArrayOutputStream stream) throws IOException,
			InterruptedException {
		
		stream.write(19); stream.write(12); stream.write(241);	//send command key
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);
		
		stream.reset();
		stream.write(10); stream.write(12); stream.write(241);	//send command key
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);
		
		stream.reset();
		
		String field;
		boolean checkDoctype = false;
		field = parametersMap.get("fileNo").getFirstParameter();
		if (StringUtils.isEmpty(field)){
				throw new RuntimeException("FileNo Is Empty");
		} else{
			stream.write(11); stream.write(10); stream.write(56);	//send command key
			field = field.toUpperCase();
			 
			for (int i = 0; i < field.length(); i++) {
				stream.write(codePage.uni2ebcdic(field.charAt(i)));
			}
		}
		
		//------------------ end Sending FileNo
		
		writeGDS(0, 3, stream.toByteArray());
		bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
		HTTPResponse response = null;
		String temp;
		int count = 10;
		while (count-- > 0) {
			try {
				temp = new String(bk.buffer, "cp037");
				
				if (temp.contains("Print request was submitted")){
					break;
				}
				
				if (temp.contains("Order updated &")) {
					break;
				}
				
				if (temp.contains("Welcome to SKLD Search Menu")) {
					break;
				}
				
				if (checkDoctype && bk.getOpCode() == 3) {
					response = checkDoctype(temp, stream);
					if (response != null) {
						cleanPrinterQueue();
						return response;
					}
				}
				
				bk = dsq.get(READ_RESPONSE_TIMEOUT);		//read screen
				parseIncoming();
				
			} catch (Exception e) {
				logger.error(searchId + ": Some error while sending data", e);
				if (bk == null) {
					return null;
				}
			}
		}
		return null;
	}

	
	private byte[] getSelectCountyBytes(String countyNo) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		countyNo = org.apache.commons.lang.StringUtils.rightPad(countyNo, 3, ' ');
		stream.write(17);
		stream.write(15);
		stream.write(241);
		stream.write(17);
		stream.write(17);
		stream.write(14);
		for (int i = 0; i < countyNo.length(); i++) {
			stream.write(codePage.uni2ebcdic(countyNo.charAt(i)));
		}
		return stream.toByteArray();
	}
	
	private byte[] getSelectSearchMenuBytes(String option, boolean override) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(19);
		stream.write(12);
		stream.write(241);
		stream.write(17);
		stream.write(19);
		stream.write(11);
		if(override) {
			if("25".equalsIgnoreCase(option) || "26".equalsIgnoreCase(option)) {
				stream.write(codePage.uni2ebcdic('2'));
			} else if("4".equalsIgnoreCase(option)){
				stream.write(codePage.uni2ebcdic('4'));
			} else if("5".equalsIgnoreCase(option)){
				stream.write(codePage.uni2ebcdic('5'));
			} else {
				stream.write(codePage.uni2ebcdic('1'));	//try order
			}
		} else {
			if("25".equalsIgnoreCase(option) || "26".equalsIgnoreCase(option) ) {
				stream.write(codePage.uni2ebcdic('2'));
			} else if("4".equalsIgnoreCase(option)){
				stream.write(codePage.uni2ebcdic('4'));
			} else if("5".equalsIgnoreCase(option)){
				stream.write(codePage.uni2ebcdic('5'));
			} else {
				for (int i = 0; i < option.length() && i < 3; i++) {
					stream.write(codePage.uni2ebcdic(option.charAt(i)));
				}
			}
		}
		return stream.toByteArray();
	}
	
	
	private final byte[] readNegotiations() throws IOException {
		int i = bin.read();
		if (i < 0) {
			throw new IOException("Connection closed.");
		} else {
			int j = bin.available();
			byte abyte0[] = new byte[j + 1];
			abyte0[0] = (byte) i;
			bin.read(abyte0, 1, j);
			return abyte0;
		}
	}
	
	public final boolean negotiate(byte abyte0[]) throws IOException {
		int i = 0;


      	//from server negotiations
		if(abyte0[i] == IAC) { // -1

			while(i < abyte0.length && abyte0[i++] == -1)

            switch(abyte0[i++]) {

               // we will not worry about what it WONT do
               case WONT:            // -4
               default:
                 break;

               case DO: //-3

                  // not sure why but since moving to V5R2 we are receiving a
                  //   DO with no option when connecting a second session with
                  //   device name.  Can not find the cause at all.  If anybody
                  //   is interested please debug this until then this works.
                  if (i < abyte0.length) {
                     switch(abyte0[i]) {
                        case TERMINAL_TYPE: // 24
                           baosp.write(IAC);
                           baosp.write(WILL);
                           baosp.write(TERMINAL_TYPE);
                           writeByte(baosp.toByteArray());
                           baosp.reset();

                           break;

                       case OPT_END_OF_RECORD: // 25

                           baosp.write(IAC);
                           baosp.write(WILL);
                           baosp.write(OPT_END_OF_RECORD);
                           writeByte(baosp.toByteArray());
                           baosp.reset();
                           break;

                       case TRANSMIT_BINARY: // 0

                           baosp.write(IAC);
                           baosp.write(WILL);
                           baosp.write(TRANSMIT_BINARY);
                           writeByte(baosp.toByteArray());
                           baosp.reset();

                           break;

                       case TIMING_MARK: // 6   rfc860
   //                        System.out.println("Timing Mark Received and notifying " +
   //                        "the server that we will not do it");
                           baosp.write(IAC);
                           baosp.write(WONT);
                           baosp.write(TIMING_MARK);
                           writeByte(baosp.toByteArray());
                           baosp.reset();

                           break;

                       case NEW_ENVIRONMENT: // 39 rfc1572
                           if (devName == null && user == null) {
                              baosp.write(IAC);
                              baosp.write(WONT);
                              baosp.write(NEW_ENVIRONMENT);
                              writeByte(baosp.toByteArray());
                              baosp.reset();

                           }
                           else {
                              baosp.write(IAC);
                              baosp.write(WILL);
                              baosp.write(NEW_ENVIRONMENT);
                              writeByte(baosp.toByteArray());
                              baosp.reset();

                           }
                           break;

                       default:  // every thing else we will not do at this time
                           baosp.write(IAC);
                           baosp.write(WONT);
                           baosp.write(abyte0[i]); // either
                           writeByte(baosp.toByteArray());
                           baosp.reset();

                           break;
                     }
                 }

                 i++;
                 break;

               case WILL:

                 switch(abyte0[i]) {
                    case OPT_END_OF_RECORD: // 25
                        baosp.write(IAC);
                        baosp.write(DO);
                        baosp.write(OPT_END_OF_RECORD);
                        writeByte(baosp.toByteArray());
                        baosp.reset();

                        break;

                    case TRANSMIT_BINARY: // '\0'
                        baosp.write(IAC);
                        baosp.write(DO);
                        baosp.write(TRANSMIT_BINARY);
                        writeByte(baosp.toByteArray());
                        baosp.reset();

                        break;
                 }
                 i++;
                 break;

               case SB: // -6

                  if(abyte0[i] == NEW_ENVIRONMENT && abyte0[i + 1] == 1) {
                     negNewEnvironment();

                     while (++i < abyte0.length && abyte0[i + 1] != IAC);
                  }

                  if(abyte0[i] == TERMINAL_TYPE && abyte0[i + 1] == 1) {
                     baosp.write(IAC);
                     baosp.write(SB);
                     baosp.write(TERMINAL_TYPE);
                     baosp.write(QUAL_IS);
                     
                     baosp.write("IBM-3179-2".getBytes());
                     
                     baosp.write(IAC);
                     baosp.write(SE);
                     writeByte(baosp.toByteArray());
                     baosp.reset();

                     i++;
                  }
                  i++;
                  break;
            }
            return true;
      }
      else {
         return false;
      	}
	}
	
	/**
	 * Negotiate new environment string for device name
	 *
	 * @throws IOException
	 */
	private void negNewEnvironment() throws IOException {

		baosp.write(IAC);
		baosp.write(SB);
		baosp.write(NEW_ENVIRONMENT);
		baosp.write(IS);

		if (devName != null) {
			baosp.write(USERVAR);

			baosp.write("DEVNAME".getBytes());

			baosp.write(VALUE);

			baosp.write(negDeviceName().getBytes());
		}

		if (user != null) {

			baosp.write(VAR);
			baosp.write("USER".getBytes());
			baosp.write(VALUE);
			baosp.write(user.getBytes());

			if (password != null) {
				baosp.write(USERVAR);
				baosp.write("IBMRSEED".getBytes());
				baosp.write(VALUE);
				baosp.write(NEGOTIATE_ESC);
				baosp.write(0x0);
				baosp.write(0x0);
				baosp.write(0x0);
				baosp.write(0x0);
				baosp.write(0x0);
				baosp.write(0x0);
				baosp.write(0x0);
				baosp.write(0x0);
				baosp.write(USERVAR);
				baosp.write("IBMSUBSPW".getBytes());
				baosp.write(VALUE);
				baosp.write(password.getBytes());
			}

			if (library != null) {
				baosp.write(USERVAR);
				baosp.write("IBMCURLIB".getBytes());
				baosp.write(VALUE);
				baosp.write(library.getBytes());
			}

			if (initialMenu != null) {
				baosp.write(USERVAR);
				baosp.write("IBMIMENU".getBytes());
				baosp.write(VALUE);
				baosp.write(initialMenu.getBytes());
			}

			if (program != null) {
				baosp.write(USERVAR);
				baosp.write("IBMPROGRAM".getBytes());
				baosp.write(VALUE);
				baosp.write(program.getBytes());
			}
		}
		baosp.write(IAC);
		baosp.write(SE);

		writeByte(baosp.toByteArray());
		baosp.reset();

	}
	
	/**
	 * This will negotiate a device name with controller. if the sequence is
	 * less than zero then it will send the device name as specified. On each
	 * unsuccessful attempt a sequential number is appended until we find one or
	 * the controller says no way.
	 *
	 * @return String
	 */
	private String negDeviceName() {

		if (devSeq++ == -1) {
			devNameUsed = devName;
			return devName;
		} else {
			StringBuffer sb = new StringBuffer(devName + devSeq);
			int ei = 1;
			while (sb.length() > 10) {

				sb.setLength(0);
				sb.append(devName.substring(0, devName.length() - ei++));
				sb.append(devSeq);

			}
			devNameUsed = sb.toString();
			return devNameUsed;
		}
	}
	
	private final void writeByte(byte abyte0[]) throws IOException {

		bout.write(abyte0);
		bout.flush();
	}
	
	public void dump (byte[] abyte0) {
	      try {

	         

	         StringBuffer h = new StringBuffer();
	         for (int x = 0; x < abyte0.length; x++) {
	            if (x % 16 == 0) {
	               System.out.println("  " + h.toString());
	         

	               h.setLength(0);
	               h.append("+0000");
	               h.setLength(5 - Integer.toHexString(x).length());
	               h.append(Integer.toHexString(x).toUpperCase());

	               System.out.print(h.toString());
	         

	               h.setLength(0);
	            }
	            char ac = codePage.ebcdic2uni(abyte0[x]);
	            if (ac < ' ')
	               h.append('.');
	            else
	               h.append(ac);
	            if (x % 4 == 0) {
	               System.out.print(" ");

	            }

	            if (Integer.toHexString(abyte0[x] & 0xff).length() == 1){
	               System.out.print("0" + Integer.toHexString(abyte0[x] & 0xff).toUpperCase());


	            }
	            else {
	               System.out.print(Integer.toHexString(abyte0[x] & 0xff).toUpperCase());
	            }

	         }
	         System.out.println();

	      }
	      catch(Exception _ex) {
	         logger.warn("Cannot dump from host\n\r");
	      }

	   }
	// write gerneral data stream
	private final void writeGDS(int flags, int opcode, byte abyte0[])
			throws IOException {

		// Added to fix for JDK 1.4 this was null coming from another method.
		//  There was a weird keyRelease event coming from another panel when
		//  using a key instead of the mouse to select button.
		//  The other method was fixed as well but this check should be here
		// anyway.
		if (bout == null)
			return;

		int length;
		if (abyte0 != null)
			length = abyte0.length + 10;
		else
			length = 10;

		// refer to rfc1205 - 5250 Telnet interface
		// Section 3. Data Stream Format

		// Logical Record Length - 16 bits
		baosrsp.write(length >> 8); // Length LL
		baosrsp.write(length & 0xff); //        LL

		// Record Type - 16 bits
		// It should always be set to '12A0'X to indicate the
		// General Data Stream (GDS) record type.
		baosrsp.write(18); // 0x12
		baosrsp.write(160); // 0xA0

		// the next 16 bits are not used
		baosrsp.write(0); // 0x00
		baosrsp.write(0); // 0x00

		//  The second part is meant to be variable in length
		//  currently this portion is 4 octets long (1 byte or 8 bits for us ;-O)
		baosrsp.write(4); // 0x04

		baosrsp.write(flags); // flags
		// bit 0 - ERR
		// bit 1 - ATN Attention
		// bits 2-4 - reserved
		// bit 5 - SRQ system request
		// bit 6 - TRQ Test request key
		// bit 7 - HLP
		baosrsp.write(0); // reserved - set to 0x00
		baosrsp.write(opcode); // opcode

		if (abyte0 != null)
			baosrsp.write(abyte0, 0, abyte0.length);

		baosrsp = appendByteStream(baosrsp.toByteArray());

		// make sure we indicate no more to be sent
		baosrsp.write(IAC);
		baosrsp.write(EOR);

		baosrsp.writeTo(bout);

		//        byte[] b = new byte[baosrsp.size()];
		//        b = baosrsp.toByteArray();
		//      dump(b);
		bout.flush();
		//      baos = null;
		baosrsp.reset();
	}
	
	private final ByteArrayOutputStream appendByteStream(byte abyte0[]) {
		ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
		for (int i = 0; i < abyte0.length; i++) {
			bytearrayoutputstream.write(abyte0[i]);
			if (abyte0[i] == -1)
				bytearrayoutputstream.write(-1);
		}

		return bytearrayoutputstream;
	}
	
	private void parseIncoming() {

		//boolean controlChars = false;
		//byte control0;
		//byte control1;
		boolean done = false;
		boolean error = false;

		try {
			while (bk.hasNext() && !done) {
				byte b = bk.getNextByte();

				switch (b) {
				case 0:
				case 1:
					break;
				case TN5250jConstants.CMD_SAVE_SCREEN: // 0x02 2 Save Screen
				case 3: // 0x03 3 Save Partial Screen
					System.out.println("CMD_SAVE_SCREEN");
					//saveScreen();
					break;

				case ESC: // ESCAPE
					break;
				case 7: // audible bell
					//Toolkit.getDefaultToolkit().beep();
					bk.getNextByte();
					bk.getNextByte();
					break;
				case TN5250jConstants.CMD_WRITE_TO_DISPLAY: // 0x11 17 write to display
					//System.out.println("CMD_WRITE_TO_DISPLAY");
					bk.pos = bk.getBuffer().length;
					//error = writeToDisplay(true);
					// WVL - LDC : TR.000300 : Callback scenario from 5250
					// Only scan when WRITE_TO_DISPLAY operation (i.e. refill
					// screen buffer)
					// has been issued!
					//if (scan)
					//	scan();

					break;
				case TN5250jConstants.CMD_RESTORE_SCREEN: // 0x12 18 Restore Screen
				case 13: // 0x13 19 Restore Partial Screen
					logger.debug(searchId + ": restore screen partial");
					//restoreScreen();
					break;

				case TN5250jConstants.CMD_CLEAR_UNIT_ALTERNATE: // 0x20 32 clear unit alternate
					int param = bk.getNextByte();
					if (param != 0) {
						logger.debug(searchId + ":  clear unit alternate error "
								+ Integer.toHexString(param));
						sendNegResponse(TN5250jConstants.NR_REQUEST_ERROR, 03, 01, 05,
								" clear unit alternate not supported");
						done = true;
					} else {
						//if (screen52.getRows() != 27)
						//	screen52.setRowsCols(27, 132);

						//screen52.clearAll();
						//if (sfParser != null && sfParser.isGuisExists())
						//   sfParser.clearGuiStructs();


					}
					break;

				case TN5250jConstants.CMD_WRITE_ERROR_CODE: // 0x21 33 Write Error Code
					System.out.println("CMD_WRITE_ERROR_CODE");
					//writeErrorCode();
					//error = writeToDisplay(false);
					break;
				case TN5250jConstants.CMD_WRITE_ERROR_CODE_TO_WINDOW: // 0x22 34
					System.out.println("CMD_WRITE_ERROR_CODE_TO_WINDOW");
					// Write Error Code to window
					//writeErrorCodeToWindow();
					//error = writeToDisplay(false);
					break;

				case TN5250jConstants.CMD_READ_SCREEN_IMMEDIATE: // 0x62 98
				case TN5250jConstants.CMD_READ_SCREEN_TO_PRINT: // 0x66 102 read screen to print
					//readScreen();
					break;

				case TN5250jConstants.CMD_CLEAR_UNIT: // 64 0x40 clear unit
					//System.err.println("CMD_CLEAR_UNIT");
					//if (screen52.getRows() != 24)
					//	screen52.setRowsCols(24, 80);
					//screen52.clearAll();
					//if (sfParser != null && sfParser.isGuisExists())
					//   sfParser.clearGuiStructs();

					break;

				case TN5250jConstants.CMD_CLEAR_FORMAT_TABLE: // 80 0x50 Clear format table
					//screen52.clearTable();
					break;

				case TN5250jConstants.CMD_READ_INPUT_FIELDS: //0x42 66 read input fields
				case TN5250jConstants.CMD_READ_MDT_FIELDS: // 0x52 82 read MDT Fields
					bk.getNextByte();
					bk.getNextByte();
					//readType = b;
					//screen52.goHome();
					// do nothing with the cursor here it is taken care of
					//   in the main loop.
					//////////////// screen52.setCursorOn();
					//waitingForInput = true;
					//pendingUnlock = true;
					//                  screen52.setKeyboardLocked(false);
					break;
				case TN5250jConstants.CMD_READ_MDT_IMMEDIATE_ALT: // 0x53 83
					System.out.println("CMD_READ_MDT_IMMEDIATE_ALT");
					//readType = b;
					//                  screen52.goHome();
					//                  waitingForInput = true;
					//                  screen52.setKeyboardLocked(false);
					//readImmediate(readType);
					break;
				case TN5250jConstants.CMD_WRITE_STRUCTURED_FIELD: // 243 0xF3 -13 Write
												 // structured field
					writeStructuredField();
					break;
				case TN5250jConstants.CMD_ROLL: // 0x23 35 Roll Not sure what it does right now
					//int updown = bk.getNextByte();
					//int topline = bk.getNextByte();
					//int bottomline = bk.getNextByte();
					//screen52.rollScreen(updown, topline, bottomline);
					bk.getNextByte();bk.getNextByte();bk.getNextByte();
					break;

				default:
					done = true;
					sendNegResponse(TN5250jConstants.NR_REQUEST_ERROR, 03, 01, 01,"parseIncoming");
					break;
				}

				if (error)
					done = true;
			}
			//       BEGIN FRAMEWORK
			//  I took this out for debugging a problem
//			ScreenField[] a = this.screen52.getScreenFields().getFields();
//			if (log.isDebugEnabled()) {
//				for (int x = 0; x < a.length; x++) {
//					log.debug(a[x].toString());
//				}
//			}
//
//			String strokes = this.screen52.getKeys();
//			if (!strokes.equals("")) {
//				Tn5250jKeyEvents e = new Tn5250jKeyEvents(this.screen52,
//						strokes);
//				//from the previous screen.
//				Tn5250jController.getCurrent().handleEvent(e);
//			}
//
//			Tn5250jEvent event = new Tn5250jEvent(screen52);
//			Tn5250jController.getCurrent().handleEvent(event);
//
//			//END FRAMEWORK
		} catch (Exception exc) {
			logger.warn("incoming " + exc.getMessage());
		}
		;
	}
	
	public final boolean disconnect() {
		try {
			
			status = STATUS_NOT_KNOWN;
			if (socket != null) {
				logger.info(searchId + ": Closing socket");
				socket.close();
			}
			if (bin != null)
				bin.close();
			if (bout != null)
				bout.close();
			if(printerConnection != null) {
				printerConnection.disconnectAllServices();
			}


		} catch (Exception exception) {
			logger.error(exception);
			devSeq = -1;
			return false;

		}
		devSeq = -1;
		return true;
	}
	
	private void writeStructuredField() {

		boolean done = false;

		try {
			
			while (bk.hasNext() && !done) {
				switch (bk.getNextByte()) {

				case -39: // SOH - Start of Header Order

					switch (bk.getNextByte()) {
					case 112: // 5250 Query
						bk.getNextByte(); // get null required field
						sendQueryResponse();
						break;
					default:
						//logger.debug(searchId + ": invalid structured field sub command " + bk.getByteOffset(-1));
						break;
					}
					break;
				default:
					//logger.debug(searchId + ": invalid structured field command " + bk.getByteOffset(-1));
					break;
				}
			}
		} catch (Exception e) {
			logger.error(searchId + ": Error writeStructuredField ", e );
		}


	}
	
	/**
	 * Method sendQueryResponse
	 *
	 * The query command is used to obtain information about the capabilities of
	 * the 5250 display.
	 *
	 * The Query command must follow an Escape (0x04) and Write Structured Field
	 * command (0xF3).
	 *
	 * This section is modeled after the rfc1205 - 5250 Telnet Interface section
	 * 5.3
	 *
	 * @throws IOException
	 */
	private final void sendQueryResponse() throws IOException {

		logger.debug(searchId + ": sending query response");
		byte abyte0[] = new byte[64];
		abyte0[0] = 0; // Cursor Row/column (set to zero)
		abyte0[1] = 0; //           ""
		abyte0[2] = -120; // X'88' inbound write structure Field aid
		if (enhanced == true) {
			abyte0[3] = 0; // 0x003D (61) length of query response
			abyte0[4] = 64; //       "" see note below ?????????
		} else {
			abyte0[3] = 0; // 0x003A (58) length of query response
			abyte0[4] = 58; //       ""
			//  the length between 58 and 64 seems to cause
			//  different formatting codes to be sent from
			//  the host ???????????????? why ???????
			//    Well the why can be found in the manual if
			//       read a little more ;-)
		}
		abyte0[5] = -39; // command class 0xD9
		abyte0[6] = 112; // Command type query 0x70
		abyte0[7] = -128; // 0x80 Flag byte
		abyte0[8] = 6; // Controller Hardware Class
		abyte0[9] = 0; // 0x0600 - Other WSF or another 5250 Emulator
		abyte0[10] = 1; // Controller Code Level
		abyte0[11] = 1; //    Version 1 Rel 1.0
		abyte0[12] = 0; //       ""

		abyte0[13] = 0; // 13 - 28 are reserved so set to 0x00
		abyte0[14] = 0; //       ""
		abyte0[15] = 0; //       ""
		abyte0[16] = 0; //       ""
		abyte0[17] = 0; //       ""
		abyte0[18] = 0; //       ""
		abyte0[19] = 0; //       ""
		abyte0[20] = 0; //       ""
		abyte0[21] = 0; //       ""
		abyte0[22] = 0; //       ""
		abyte0[23] = 0; //       ""
		abyte0[24] = 0; //       ""
		abyte0[25] = 0; //       ""
		abyte0[26] = 0; //       ""
		abyte0[27] = 0; //       ""
		abyte0[28] = 0; //       ""
		abyte0[29] = 1; // Device type - 0x01 5250 Emulator
		abyte0[30] = codePage.uni2ebcdic('5'); // Device type character
		abyte0[31] = codePage.uni2ebcdic('2'); //          ""
		abyte0[32] = codePage.uni2ebcdic('5'); //          ""
		abyte0[33] = codePage.uni2ebcdic('1'); //          ""
		abyte0[34] = codePage.uni2ebcdic('0'); //          ""
		abyte0[35] = codePage.uni2ebcdic('1'); //          ""
		abyte0[36] = codePage.uni2ebcdic('1'); //          ""

		abyte0[37] = 2; // Keyboard Id - 0x02 Standard Keyboard
		abyte0[38] = 0; // extended keyboard id
		abyte0[39] = 0; // reserved

		abyte0[40] = 0; // 40 - 43 Display Serial Number
		abyte0[41] = 36; //
		abyte0[42] = 36; //
		abyte0[43] = 0; //

		abyte0[44] = 1; // Maximum number of display fields - 256
		abyte0[45] = 0; // 0x0100
		abyte0[46] = 0; // 46 -48 Reserved set to 0x00
		abyte0[47] = 0;
		abyte0[48] = 0;
		abyte0[49] = 1; // 49 - 53 Controller Display Capability
		abyte0[50] = 17; //      see rfc - tired of typing :-)
		abyte0[51] = 0; //          ""
		abyte0[52] = 0; //          ""

		//  53
		//    Bit 0-2: B'000' - no graphics capability
		//             B'001' - 5292-2 style graphics
		//    Bit 3-7: B '00000' = reserved (it seems for Client access)

		if (enhanced == true) {
			//         abyte0[53] = 0x5E; // 0x5E turns on ehnhanced mode
			//         abyte0[53] = 0x27; // 0x5E turns on ehnhanced mode
			abyte0[53] = 0x7; //  0x5E turns on ehnhanced mode
			//logger.debug(searchId + ": enhanced options");
		} else
			abyte0[53] = 0x0; //  0x0 is normal emulation

		abyte0[54] = 24; // 54 - 60 Reserved set to 0x00
		//  54 - I found out is used for enhanced user
		//       interface level 3. Bit 4 allows headers
		//       and footers for windows
		abyte0[54] = 8; // 54 - 60 Reserved set to 0x00
		//  54 - I found out is used for enhanced user
		//       interface level 3. Bit 4 allows headers
		//       and footers for windows
		abyte0[55] = 0;
		abyte0[56] = 0;
		abyte0[57] = 0;
		abyte0[58] = 0;
		abyte0[59] = 0;
		abyte0[60] = 0;
		abyte0[61] = 0; // gridlines are not supported
		abyte0[62] = 0; // gridlines are not supported
		abyte0[63] = 0;
		writeGDS(0, 0, abyte0); // now tell them about us
		abyte0 = null;

	}
	
	/**
	 * This routine handles sending negative responses back to the host.
	 *
	 * You can find a description of the types of responses to be sent back by
	 * looking at section 12.4 of the 5250 Functions Reference manual
	 *
	 *
	 * @param cat
	 * @param modifier
	 * @param uByte1
	 * @param uByte2
	 * @param from
	 *
	 */
	protected void sendNegResponse(int cat, int modifier, int uByte1,
			int uByte2, String from) {

		try {

			int os = bk.getByteOffset(-1) & 0xf0;
			int cp = (bk.getCurrentPos() - 1);
			logger.debug(searchId + ": invalid " + from + " command " + os
					+ " at pos " + cp);
		} catch (Exception e) {

			logger.warn("Send Negative Response error " + e.getMessage());
		}

		baosp.write(cat);
		baosp.write(modifier);
		baosp.write(uByte1);
		baosp.write(uByte2);

		try {
			writeGDS(128, 0, baosp.toByteArray());
		} catch (IOException ioe) {

			logger.warn(ioe.getMessage());
		}
		baosp.reset();

	}

	@Override
	public void run() {
		
		
		Timer timerNow = null;
		GenericSKLDTimer timer = null;
		
		while(true) {
			HTTPRequest request = null;
			try {
				requestSemaphore.acquire();
				
				if(parentSiteExecutionCounter < MAX_PARENT_SITE_CONSECUTIVE_REQUESTS) {
					request = requestParentSiteQueue.poll();
					if(request != null) {
						parentSiteExecutionCounter++;
					} else {
						request = requestAutomaticQueue.poll();
						parentSiteExecutionCounter = 0;
					}
				} else {
					request = requestAutomaticQueue.poll();
					if(request == null) {
						request = requestParentSiteQueue.poll();
					} else {
						parentSiteExecutionCounter = 0;
					}
				}
				
				timerNow = new Timer("GenericSKLDTimer");
				timer = new GenericSKLDTimer(this);
				timerNow.schedule((TimerTask) timer, MAX_TIMEOUT_INTERRUPT_REQUEST, MAX_TIMEOUT_INTERRUPT_REQUEST);
				
				HTTPResponse response = process(request);
				if(response == null ) {
					logger.error(request.getSearchId() + " GenericSKLDSite: Error while processing request - response is null - retry");
					response = process(request);
				} else {
					String responseAsString = response.getResponseAsString();
					if(responseAsString.contains("Site error") || responseAsString.contains("No End Of Search Received")) {
						logger.error(request.getSearchId() + " GenericSKLDSite: Error while processing request - response is Site error - retry");
						response = process(request);
					} else {
						response.is = IOUtils.toInputStream(responseAsString);
					}
				}
				request.setBypassResponse(response);
				
			} catch (Exception e) {
				logger.error(searchId + ": Error while running thread", e);
				HTTPResponse response = new HTTPResponse();
				response.is = IOUtils.toInputStream("Exception received: " + e.getMessage());
				response.returnCode = ATSConnConstants.HTTP_OK;
				response.contentType = "text/html";
				response.headers = new HashMap<String, String>();
				if(request != null) {
					request.setBypassResponse(response);
				}
				
			} finally {
				if(request != null) {
					synchronized (request) {
						request.notify();	
					}
				}
				timerNow.cancel();
				timer.cancel();
			}
		}
	}

	public HTTPResponse addAndExecuteRequest(HTTPRequest request) {
		try {
			synchronized (request) {
				if("1".equals(request.getPostParameter("isParentSite"))){
					requestParentSiteQueue.add(request);
				} else {
					requestAutomaticQueue.add(request);
				}
				requestSemaphore.release();
				request.wait();
			}
			
		} catch (InterruptedException e) {
			logger.error(searchId + ": Interrupt in addAndExecuteRequest", e);
		}
		return request.getBypassResponse();
	}
	
	/**
	 * Process a request
	 */
	public HTTPResponse process(HTTPRequest request) {		
		
		HTTPResponse response = null;		
		
		try{
			
			setSearchId(request.getSearchId());
			
			// destroy session
			if(isDestroySession()){
				setHttpClient(HttpSiteManager.createHttpClient()); 
				getAttributes().clear();
				status = STATUS_NOT_KNOWN;
				setDestroySession(false);
				disconnect();
			}
			
			// login if necessary
			if(status == STATUS_NOT_KNOWN){
				getSiteManager().requestPermit(this);
				status = STATUS_LOGGING_IN;
				try{
					if(onLogin(request.getSearchId(), request.getDataSite()).getStatus() == LoginStatus.STATUS_SUCCESS){
						status = STATUS_LOGGED_IN;
					} else {
						status = STATUS_NOT_KNOWN;
						disconnect();
						throw new RuntimeException("Login failed!");
					}
				}catch(RuntimeException e){
					status = STATUS_NOT_KNOWN;
					throw e;
				}
			}
			
			// call before request
			onBeforeRequest(request);
			
			if(request.getBypassResponse() != null){
				
				// use the bypass response set by onBeforeRequest
				response = request.getBypassResponse();
				
			} else {

				// enforce timing constraints
				getSiteManager().requestPermit(this);
				
				// execute request
				response = exec(request);				
			}
				
			// call after request
			onAfterRequest(request, response);	
			
		}catch(Exception e){
			
			// call after request
			onAfterRequest(request, response);
			
			logger.error(e);
			throw new RuntimeException(e);
			
		}
		
		return response;
	}
	
	public Search getSearch(long searchId) {
		return InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
	}
	
	
	private class GenericSKLDTimer extends TimerTask {
		
		GenericSKLDSite managedSite;

		public GenericSKLDTimer(GenericSKLDSite managedSite) {
			this.managedSite = managedSite;
			
		}
		
		@Override
		public void run() {
			boolean debug = true;
			if(debug) {
				managedSite.myThread.interrupt();
			} 
			
		}
		
	}
	
}
