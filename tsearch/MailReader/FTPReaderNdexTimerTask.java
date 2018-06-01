package ro.cst.tsearch.MailReader;

import static ro.cst.tsearch.bean.SearchAttributes.SEARCH_ORIGIN;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.WorkbookParser;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.gwtwidgets.client.util.SimpleDateFormat;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.connection.ftp.StartsWithFTPFileFilter;
import ro.cst.tsearch.data.GenericCounty;
import ro.cst.tsearch.data.GenericState;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.OrderProcessedMapper;
import ro.cst.tsearch.emailOrder.MailOrder;
import ro.cst.tsearch.emailOrder.PlaceOrder;
import ro.cst.tsearch.extractor.xml.GenericFunctions1;
import ro.cst.tsearch.search.address.StandardAddress;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.StringUtils;

import com.stewart.ats.base.name.Name;
import com.stewart.ats.base.name.NameI;
import com.stewart.ats.base.name.NameSourceType;
import com.stewart.ats.base.parties.PartyI;

public class FTPReaderNdexTimerTask extends TimerTask{
	
	
	public static final int COLUMN_FILE_NUMBER = 0;
	public static final int COLUMN_FIRST_NAME = 1;
	public static final int COLUMN_MIDDLE_INITIAL = 2;
	public static final int COLUMN_LAST_NAME = 3;
	public static final int COLUMN_SSN = 4;
	public static final int COLUMN_ADDRESS1 = 5;
	public static final int COLUMN_ADDRESS2 = 6;
	public static final int COLUMN_CITY = 7;
	public static final int COLUMN_COUNTY = 8;
	public static final int COLUMN_STATE = 9;
	public static final int COLUMN_ZIP = 10;
	public static final int COLUMN_ORIGINAL_NOTE_HOLDER_NAME = 11;
	public static final int COLUMN_LOAN_AMOUNT = 12;
	public static final int COLUMN_DATED = 13;
	public static final int COLUMN_RECORDED = 14;
	public static final int COLUMN_LEGAL = 15;
	
	
	
	@Override
	public void run() {
		
		if(!ServerConfig.isFTPNdexEnable()) {
			return;
		}
		
		String url = ServerConfig.getFTPNdexUrl();
		int port = ServerConfig.getFTPNdexPort();
		String username = ServerConfig.getFTPNdexUsername();
		String password = ServerConfig.getFTPNdexPassword();
		String agent = ServerConfig.getFTPNdexAgent();
		
		FTPSClient ftp = new FTPSClient(true);
		User user = null;
		
		try {
			ftp.connect(url, port);
			boolean loggedIn = ftp.login(username, password);
			int replyCode = ftp.getReplyCode();
			if (!loggedIn || !FTPReply.isPositiveCompletion(replyCode)) {
				FTPReaderDaemon.getLogger().error(
						"Could not connet to Ndex ftp using: " + username + " with " + password + " at " + url + ":"
								+ port);
			}
			ftp.execPBSZ(0);
			ftp.execPROT("P");
			ftp.setFileType(FTP.ASCII_FILE_TYPE);
			ftp.setListHiddenFiles(true);
			
			
			ftp.enterLocalPassiveMode();
			ftp.changeWorkingDirectory("IN");
			ftp.setFileType(FTP.BINARY_FILE_TYPE);
			
			Calendar cal = Calendar.getInstance();
			Set<String> startWith = new LinkedHashSet<String>();
			Date thisMonth = cal.getTime();
			cal.add(Calendar.MONTH, -1);
			Date lastMonth = cal.getTime();
			
			SimpleDateFormat sdf = new SimpleDateFormat("_yyyy-MM-");
			
			
			startWith.add("ATS_Work_List.rpt" + sdf.format(lastMonth));
			startWith.add("ATS_Work_List.rpt" + sdf.format(thisMonth));
			StartsWithFTPFileFilter fileFilter = new StartsWithFTPFileFilter(startWith);
			
			FTPFile[] listFiles = ftp.listFiles(".", fileFilter);
			if(listFiles == null) {
				FTPReaderDaemon.getLogger().error("ftp.listFiles() on IN folder is null");
			} else {
				FTPReaderDaemon.getLogger().debug("ftp.listFiles() on IN folder has " + listFiles.length + " files");
				for (int i = listFiles.length - 1; i >= 0; i--) {
					FTPFile ftpFile = listFiles[i];
					
					String fileName = ftpFile.getName();
					FTPReaderDaemon.getLogger().debug("Fould fileName " + fileName);
					if(OrderProcessedMapper.isFileAlreadyProcessed(fileName)) {
						break;
					}
					
					if(user == null) {
						user = MailOrder.getUser(agent);
						if(user == null) {
							FTPReaderDaemon.getLogger().error("Could not create user using username " + agent);
							return;
						}
					}
					
					ByteArrayOutputStream localStream = new ByteArrayOutputStream();
					boolean retrieveFile = ftp.retrieveFile(ftpFile.getName(), localStream);
					
					if(retrieveFile) {
						//WorkbookParser.getWorkbook(new File("D:\\bugs\\7475 - NdeX\\ATS_Work_List.rpt_2012-01-28-07-35-19.xls"));
						//Workbook workbook = WorkbookParser.getWorkbook(new File("D:\\ATS_Work_List.rpt_2012-01-30-07-35-58.xls"));
						byte[] byteArray = localStream.toByteArray();
						Workbook workbook = WorkbookParser.getWorkbook(new ByteArrayInputStream(byteArray));
						
						List<Search> searches = parseExcelFile(workbook, user);
						if(!searches.isEmpty()) {
							for (Search search : searches) {
								try {
									
									SearchManager.setSearch(search, user);						
									// place order
									MailOrder mailOrder = new MailOrder();
									mailOrder.savedUser = user;
									PlaceOrder.placeOrder(search, mailOrder, true, "NDeX FTP");
								} catch (Exception e) {
									FTPReaderDaemon.getLogger().error("Error while starting search " + 
											search.getID() + "/" + search.getSa().getAtribute(SearchAttributes.ABSTRACTOR_FILENO), e);
								}
							}
						}
						OrderProcessedMapper.markFileAlreadyProcessed(fileName, byteArray);
						if(ServerConfig.isDeleteNdexSpreadsheetAfterProcess()) {
							boolean deleteFile = ftp.deleteFile(ftpFile.getName());
							if(deleteFile) {
								FTPReaderDaemon.getLogger().debug("Deleted ftp file " + ftpFile.getName());
							} else {
								FTPReaderDaemon.getLogger().error("ERROR deleting ftp file " + ftpFile.getName());
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(ftp.isConnected()) {
					ftp.disconnect();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	

	private List<Search> parseExcelFile(Workbook workbook, User user) {
		List<Search> searches = new ArrayList<Search>();
		
		for (int sheetNo = 0; sheetNo < workbook.getNumberOfSheets(); sheetNo++) {
			Sheet sheet = workbook.getSheet(sheetNo);
			int rows = sheet.getRows();
			for (int rowNum = 0; rowNum < rows; rowNum++) {
				Cell[] row = sheet.getRow(rowNum);
				if(row.length > 0 && "File Number".equals(row[0].getContents())) {
					//found header row
					continue;
				}
				
				Search search = null;
				SearchAttributes sa = null;
				try {
					
					for (int celNum = 0; celNum < row.length; celNum++) {
						switch (celNum) {
						case COLUMN_FILE_NUMBER:
							String fileNumber = row[celNum].getContents();
							if(StringUtils.isNotEmpty(fileNumber)) {
								search = new Search(DBManager.getNextId(DBConstants.TABLE_SEARCH));
								search.unFakeSearch(user);
								sa = search.getSa();
								sa.setSearchId(search.getID());
								sa.setCommId(user.getUserAttributes().getCOMMID().intValue());
								sa.setAtribute(SearchAttributes.ORDERBY_FILENO, fileNumber);
								sa.setAtribute(SearchAttributes.ABSTRACTOR_FILENO, fileNumber);
								sa.setAtribute(SearchAttributes.SEARCH_ORIGIN, "NDEX");
							}
							break;
						case COLUMN_FIRST_NAME:
							celNum = COLUMN_LAST_NAME;
							String[] parseNameResult = new String[]{
									row[COLUMN_FIRST_NAME].getContents(),
									row[COLUMN_MIDDLE_INITIAL].getContents(),
									row[COLUMN_LAST_NAME].getContents(),
									"","",""};
							if(parseNameResult[0].contains(" ")) {
								parseNameResult[1] = (parseNameResult[0].substring(parseNameResult[0].indexOf(" ")) + " " + parseNameResult[1]).trim();
								parseNameResult[0] = parseNameResult[0].substring(0, parseNameResult[0].indexOf(" "));
							}
							
							String[] suffixes = GenericFunctions1.extractNameSuffixes(parseNameResult);
							
							PartyI party = sa.getOwners();
							NameI name = new Name(parseNameResult[0], parseNameResult[1], parseNameResult[2]);
							name.setSufix(suffixes[0]);
							name.getNameFlags().setOnlyThisSourceType(new NameSourceType(NameSourceType.ORDER));
							name.setSsn4Decoded(row[COLUMN_SSN].getContents());
							party.add(name);
							
							sa.getSearchPageManualOwners().add(name);
							if(StringUtils.isNotEmpty(parseNameResult[5])) {
								name = new Name(parseNameResult[3], parseNameResult[4], parseNameResult[5]);
								name.setSufix(suffixes[1]);
								name.getNameFlags().setOnlyThisSourceType(new NameSourceType(NameSourceType.ORDER));
								party.add(name);
								sa.getSearchPageManualOwners().add(name);
							}
							break;
						case COLUMN_ADDRESS1:
							StandardAddress stdAddr = new StandardAddress(row[celNum].getContents());
							sa.setAtribute(SearchAttributes.P_STREETNO, stdAddr.getAddressElement(StandardAddress.STREET_NUMBER));
							sa.setAtribute(SearchAttributes.P_STREETNAME, stdAddr.getAddressElement(StandardAddress.STREET_NAME));
							sa.setAtribute(SearchAttributes.P_STREETDIRECTION, stdAddr.getAddressElement(StandardAddress.STREET_PREDIRECTIONAL));
							sa.setAtribute(SearchAttributes.P_STREETSUFIX, stdAddr.getAddressElement(StandardAddress.STREET_SUFFIX));
							sa.setAtribute(SearchAttributes.P_STREETUNIT, stdAddr.getAddressElement(StandardAddress.STREET_SEC_ADDR_RANGE));
							sa.setAtribute(SearchAttributes.P_STREET_POST_DIRECTION, stdAddr.getAddressElement(StandardAddress.STREET_POSTDIRECTIONAL));
							break;
						case COLUMN_CITY:
							sa.setAtribute(SearchAttributes.P_CITY, row[COLUMN_CITY].getContents());
							break;
						case COLUMN_COUNTY:
							String stateAbv = row[COLUMN_STATE].getContents();
							String countyName = row[COLUMN_COUNTY].getContents();
							
							if ((stateAbv != null) && (stateAbv.length() != 0)) {
								GenericState state = DBManager.getStateForAbv(stateAbv);
								if (state != null) {
									sa.setAtribute(SearchAttributes.P_STATE, String.valueOf(state.getId()));
									if ((countyName != null) && (countyName.length() != 0)) {
										GenericCounty countyForOrder = DBManager.getCountyForNameAndStateId(countyName, state.getId());
										if (countyForOrder != null) {
											sa.setAtribute(SearchAttributes.P_COUNTY, String.valueOf(countyForOrder.getId()));
										}
									}
								}
							}
							celNum = COLUMN_STATE;
							break;
						case COLUMN_ZIP:
							sa.setAtribute(SearchAttributes.P_ZIP, row[COLUMN_ZIP].getContents());
							break;
						case COLUMN_ORIGINAL_NOTE_HOLDER_NAME:
							//TODO: do something with this
							break;
						case COLUMN_LOAN_AMOUNT:
							//sa.setAtribute(SearchAttributes.BM1_LOADACCOUNTNO, row[COLUMN_LOAN_AMOUNT].getContents());
							break;
						case COLUMN_DATED:
							//TODO: do something with this, Recorded Date for 
							break;
						case COLUMN_RECORDED:
							String recorded = row[celNum].getContents();
							Pattern volumePagePattern = Pattern.compile("VOLUME\\s*(\\d+),?\\s*PAGE\\s*(\\d+)");
							Matcher matcher = volumePagePattern.matcher(recorded);
							String bp = null;
							while (matcher.find()) {
								if(bp == null) {
									bp = matcher.group(1) + "-" + matcher.group(2);
								} else {
									bp += ", " + matcher.group(1) + "-" + matcher.group(2);
								}
							}
							if(StringUtils.isNotEmpty(bp)) {
								sa.setAtribute(SearchAttributes.LD_BOOKPAGE, bp);
							}
							
							String instrNo = null;
							Pattern fileNoPattern = Pattern.compile("FILE N(?:O|0)\\.?\\s*(\\d+)");
							matcher = fileNoPattern.matcher(recorded);
							while (matcher.find()) {
								if(instrNo == null) {
									instrNo = matcher.group(1);
								} else {
									instrNo += ", " + matcher.group(1);
								}
							}
							if(StringUtils.isNotEmpty(instrNo)) {
								sa.setAtribute(SearchAttributes.LD_INSTRNO, instrNo);
							}
							
							break;
						case COLUMN_LEGAL:
							String legal = row[celNum].getContents();
							sa.setAtribute(SearchAttributes.LD_SUBDIVISION, legal);
							Matcher m = null;
							Pattern lotPattern = Pattern.compile("\\bLOT(:?(:?\\s+(\\d[\\dA-Z]*))|(:?[^,\\(]+\\((\\d[\\dA-Z]*)\\)))");
							
							String valueToStore = null;
							Set<String> set = new LinkedHashSet<String>();
							m = lotPattern.matcher(legal);
							while(m.find()) {
								if(StringUtils.isNotEmpty(m.group(3))) {
									set.add(m.group(3));
								}
								if(StringUtils.isNotEmpty(m.group(5))) {
									set.add(m.group(5));
								}
								legal = legal.replaceFirst(m.group(), "");
							}
							for (String singleLot : set) {
								if(valueToStore == null) {
									valueToStore = singleLot;
								} else {
									valueToStore += "," + singleLot;
								}
							}
							if(StringUtils.isNotEmpty(valueToStore)) {
								sa.setAtribute(SearchAttributes.LD_LOTNO, valueToStore);
							}
							
							valueToStore = null;
							set.clear();
							Pattern nbcPattern = Pattern.compile("\\bNCB\\s+(\\d+)|NEW CITY BLOCK\\s(\\d+)");
							m = nbcPattern.matcher(legal);
							while(m.find()) {
								if(StringUtils.isNotEmpty(m.group(1))) {
									set.add(m.group(1));
								}
								if(StringUtils.isNotEmpty(m.group(2))) {
									set.add(m.group(2));
								}
								legal = legal.replaceFirst(m.group(), "");
							}
							for (String singleValue : set) {
								if(valueToStore == null) {
									valueToStore = singleValue;
								} else {
									valueToStore += "," + singleValue;
								}
							}
							if(StringUtils.isNotEmpty(valueToStore)) {
								sa.setAtribute(SearchAttributes.LD_NCB_NO, valueToStore);
							}
							
							//-------
							
							valueToStore = null;
							set.clear();
							Pattern blockPattern = Pattern.compile("\\bBLOCK(:?(:?\\s+\"?(\\d[\\dA-Z]*)\"?)|(:?[^,\\(]+\\((\\d[\\dA-Z]*)\\)))");
							m = blockPattern.matcher(legal);
							while(m.find()) {
								if(StringUtils.isNotEmpty(m.group(3))) {
									set.add(m.group(3));
								}
								if(StringUtils.isNotEmpty(m.group(5))) {
									set.add(m.group(5));
								}
								legal = legal.replaceFirst(m.group(), "");
							}
							for (String singleValue : set) {
								if(valueToStore == null) {
									valueToStore = singleValue;
								} else {
									valueToStore += "," + singleValue;
								}
							}
							if(StringUtils.isNotEmpty(valueToStore)) {
								sa.setAtribute(SearchAttributes.LD_SUBDIV_BLOCK, valueToStore);
							}
							
							//--------
							
							Pattern bpPattern = Pattern.compile("\\bVOLUME\\s*(\\d+),?\\s*PAGES?\\s*(\\d+)");
							valueToStore = sa.getAtribute(SearchAttributes.LD_BOOKPAGE);
							m = bpPattern.matcher(legal);
							while(m.find()) {
								if(StringUtils.isNotEmpty(valueToStore)) {
									valueToStore += "," + m.group(1) + "-" + m.group(2);
								} else {
									valueToStore = m.group(1) + "-" + m.group(2);
								}
								legal = legal.replaceFirst(m.group(), "");
							}
							if(StringUtils.isNotEmpty(valueToStore)) {
								sa.setAtribute(SearchAttributes.LD_BOOKPAGE, valueToStore);
							}
							
							//---------
							
							break;
						default:
							break;
						}
					}
					
					try {
						if(sa != null) {
							for (String attribute : new String[] { 
									SearchAttributes.P_STREETNAME,
									SearchAttributes.P_STREETNO, 
									SearchAttributes.P_STREETDIRECTION,
									SearchAttributes.P_STREETSUFIX, 
									SearchAttributes.P_STREETUNIT, 
									SearchAttributes.P_CITY,
									SearchAttributes.P_ZIP, 
									SearchAttributes.LD_INSTRNO, 
									SearchAttributes.LD_BOOKPAGE,
									SearchAttributes.LD_LOTNO, 
									SearchAttributes.LD_SUBDIV_BLOCK, 
									SearchAttributes.LD_NCB_NO 
									}) {
								sa.getSearchPageManualFields().put(attribute, sa.getAtribute(attribute));
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				
				} catch (Exception e) {
					FTPReaderDaemon.getLogger().error("Error while parsing row " + sheet.getName(), e);
					search = null;
					sa = null;
				}
				
				if(search != null) {
					
					search.getSa().setAtribute(SearchAttributes.SEARCH_PRODUCT, Integer.toString(SearchAttributes.SEARCH_PROD_FULL));
					
					searches.add(search);
				}
				
				
			}
		}
		
		
		return searches;
	}
}
