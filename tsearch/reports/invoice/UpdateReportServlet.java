/*
 * Created on Jan 10, 2005
 *
 */
package ro.cst.tsearch.reports.invoice;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JExcelApiExporter;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringEscapeUtils;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.OrdersExportATS2ReportBean;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DBReports;
import ro.cst.tsearch.database.DBSearch;
import ro.cst.tsearch.database.rowmapper.NoteMapper;
import ro.cst.tsearch.database.rowmapper.NoteMapper.TYPE;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.exceptions.UpdateDBException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.reports.data.DayReportLineData;
import ro.cst.tsearch.reports.tags.SearchReportLoopTag;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.servers.types.ASKServer;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.servlet.FileServlet;
import ro.cst.tsearch.tags.StatusSelect;
import ro.cst.tsearch.threads.ASMaster;
import ro.cst.tsearch.threads.ASThread;
import ro.cst.tsearch.threads.CommAdminNotifier;
import ro.cst.tsearch.threads.GPMaster;
import ro.cst.tsearch.threads.GPThread;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserManager;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

import com.stewart.ats.base.legal.LegalUtils;

public class UpdateReportServlet extends BaseServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	
	public void doRequest(HttpServletRequest request, HttpServletResponse response) 
				throws ServletException, IOException, BaseException {

		User user=(User)request.getSession().getAttribute(SessionParams.CURRENT_USER);
		UserAttributes ua = user.getUserAttributes();
		//loading parameters from request
		String operation = request.getParameter( RequestParams.REPORTS_LIST_OPERATION );
		String listChk = request.getParameter( RequestParams.REPORTS_LIST_CHK );
		
        long timestamp = System.currentTimeMillis();
        String closedDate = new FormatDate(FormatDate.TIMESTAMP).getDate(timestamp);
        String sqlClosedDate = "str_to_date( '" + closedDate + "' , '" + FormatDate.translateToMysql(FormatDate.TIMESTAMP) + "' )";
    	GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance();
    	String dateTimeStr = (cal.get( GregorianCalendar.MONTH ) + 1) + "/" +
    							cal.get( GregorianCalendar.DAY_OF_MONTH ) + "/" + cal.get( GregorianCalendar.YEAR ) + " " +
    							cal.get( GregorianCalendar.HOUR_OF_DAY ) + ":" + cal.get( GregorianCalendar.MINUTE ) + ":" + cal.get( GregorianCalendar.SECOND );
    	boolean exportFileAvailable = false;
    	
		//executing the required operation
		if ( !"".equals(operation) ){
			boolean closeAndKeepAbstractor = operation.equals( RequestParamsValues.REPORTS_SET_K_STATUS_KEEP_ABSTRACTOR );
			
			
			if ( (operation.equals(RequestParamsValues.REPORTS_SET_K_STATUS) || closeAndKeepAbstractor ) 
					&& listChk != null){
				Vector<Long> toCloseAndTakeTheSearch = new Vector<Long>();
				Vector<Long> toCloseAndKeepAbstractor = new Vector<Long>();
				long countyId = InstanceManager.getManager().getCurrentInstance(Integer.parseInt(request.getParameter("searchId")))
						.getCurrentCounty().getCountyId().longValue();
			    String[] ids = listChk.split(",");
                if (!UserUtils.isTSAdmin(ua)) {
				    long id;
				    String errorMessage = null;
				    for (int i = 0; i < ids.length; i++) {
				        try {
							id = Long.parseLong(ids[i]);

							ASThread currentAS = ASMaster.getSearch(id);
							GPThread gpt = GPMaster.getThread(id);

							// pentru a se putea face unlock pe search, nu
							// trebuie sa existe vreun thread de generare TSR sau automatic!
							Search search = null;

							try {
								search = SearchManager.getSearchFromDisk(id);
								search.disposeTime = 0;
								// pentru searchurile deserealizate nu mai are voie sa faca update de date pe agent
								search.setAllowGetAgentInfoFromDB(false);
							} catch (Exception e) {
								e.printStackTrace();
							}
							if (currentAS != null) {
								errorMessage = (errorMessage != null ? errorMessage+ "\\n": "")
										+ "Automatic in progress for "+DBManager.getSearchFileNo(id);
							} else if (gpt != null) {
								errorMessage = (errorMessage != null ? errorMessage+ "\\n": "")
										+ "Create TSR in progress for "+DBManager.getSearchFileNo(id);
							} else if (search != null && DBManager.getTSRGenerationStatus(search.getID())== Search.SEARCH_TSR_CREATED ) {
								toCloseAndKeepAbstractor.add(id);
							}
							else {
								if(closeAndKeepAbstractor) {
									toCloseAndKeepAbstractor.add(id);
								} else {
									toCloseAndTakeTheSearch.add(id);
								}
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
				    }
				    
				    if (toCloseAndKeepAbstractor.size() > 0 || toCloseAndTakeTheSearch.size() > 0) {
				        try {
				        	for (Long searchIdToClose : toCloseAndKeepAbstractor) {
				        		String userLogin = ua.getNiceName();
								String msgStr = "\n</div><div><BR><B>Search Closed</B> on: " + SearchLogger.getTimeStampFormat1(userLogin, "&#44; ") + "</BR></div>\n";
			        			SearchLogger.infoUpdateToDB(msgStr, searchIdToClose);
							}
				        	for (Long searchIdToClose : toCloseAndTakeTheSearch) {
				        		String userLogin = ua.getNiceName();
								String msgStr = "\n</div><div><BR><B>Search Closed</B> on: " + SearchLogger.getTimeStampFormat1(userLogin, "&#44; ") + "</BR></div>\n";
			        			SearchLogger.infoUpdateToDB(msgStr, searchIdToClose);
							}
				        	
				        	DBManager.setKStatus(toCloseAndTakeTheSearch, ua, sqlClosedDate, false, countyId);
				        	DBManager.setKStatus(toCloseAndKeepAbstractor, ua, sqlClosedDate, true, countyId);
				        	DBManager.updateAbstractorRateId( toCloseAndTakeTheSearch, ua );
							
                            
				        } catch (Exception e) {
							logger.error("Error while closing searches!", e);
						} 
				    }
				    if (errorMessage != null) {
				        errorMessage = "Error closing searches.\\n" + errorMessage ;
				        request.setAttribute("error", errorMessage);
				    }
			    } else {
			        try {

						for (int i=0; i<ids.length; i++)
						{
							Long searchIdAux = Long.parseLong(ids[i]);
					    	String userLogin = ua.getNiceName();
					    	String msgStr = "\n</div><div><BR><B>Search Closed</B> on: " + SearchLogger.getTimeStampFormat1(userLogin, "&#44; ") + "</BR></div>\n";
		        			SearchLogger.infoUpdateToDB(msgStr, searchIdAux);
		        			
		        			if(DBManager.getTSRGenerationStatus(searchIdAux)== Search.SEARCH_TSR_CREATED ) {
		        				toCloseAndKeepAbstractor.add(searchIdAux);
		        			} else {
		        				if(closeAndKeepAbstractor) {
									toCloseAndKeepAbstractor.add(searchIdAux);
								} else {
									toCloseAndTakeTheSearch.add(searchIdAux);
								}
		        			}
						}
			        	
                        DBManager.setKStatus(toCloseAndKeepAbstractor, ua, sqlClosedDate, true, countyId);
                        DBManager.setKStatus(toCloseAndTakeTheSearch, ua, sqlClosedDate, false, countyId);
                        DBManager.updateAbstractorRateId( toCloseAndTakeTheSearch, ua );                            
                        
                    } catch (Exception e) {
			        	logger.error("SetKStatus for " + listChk);
                    	logger.error("Error while closing searches!", e);
                    }
			    }
			}
			if (operation.equals(RequestParamsValues.REPORTS_UNLOCK_SEARCHES) && listChk != null){
			    
			    String[] ids = listChk.split(",");
			    
			    long id;
			    String errorMessage = null;
			    Vector<Long> toUnclose = new Vector<Long>();
			    for (int i = 0; i < ids.length; i++) {
			        
			        try {
			            
			            id = Long.parseLong(ids[i]);
			            
			            Search search = SearchManager.getSearchByDBId(id);
			            
			            long searchId = 0;
			            if (search != null)
			            	searchId = search.getSearchID();
			            
			            ASThread currentAS = ASMaster.getSearch(searchId);
			            GPThread gpt = GPMaster.getThread(id);
			            
			            //pentru a se putea face unlock pe search, nu trebuie sa existe vreun thread de generare TSR sau automatic!
			            
                        //commadmin and tsadmin can unlock the search even if in automatic
                        if( UserUtils.isTSAdmin( ua ) || UserUtils.isCommAdmin( ua )||UserUtils.isTSCAdmin( ua ) ) {
                            currentAS = null;
                        }
                        
			            if (currentAS != null) {
			            	errorMessage = (errorMessage != null ? errorMessage + "\n" : "") + DBManager.getSearchFileNo(id);
			            } else if (gpt != null) {
			            	errorMessage = (errorMessage != null ? errorMessage + "\n" : "") + DBManager.getSearchFileNo(id);
			            } else if (search != null ) {
			            	errorMessage = (errorMessage != null ? errorMessage + "\n" : "") + DBManager.getSearchFileNo(id);
			            } else if (ua.isAbstractor() || ua.isAgent()) {
			            	
			            	Map<String, Object> searchAbstractorIdAndFileId = DBSearch.getSearchAbstractorIdAndFileId(id);
			            	if(searchAbstractorIdAndFileId == null) {
			            		errorMessage = (errorMessage != null ? errorMessage + "\n" : "") + "SearchID = " + id;
			            	} else if(ua.getID().longValue() != ((BigInteger)searchAbstractorIdAndFileId.get(DBConstants.FIELD_SEARCH_ABSTRACT_ID)).longValue() )   {
			            		errorMessage = (errorMessage != null ? errorMessage + "\n" : "") + searchAbstractorIdAndFileId.get(DBConstants.FIELD_SEARCH_ABSTRACT_FILENO);
			            	} else {
			            		toUnclose.add(id);	
			            	}
			            	
			            } else {
			            	toUnclose.add(id);
			            }			            
			        } catch (Exception e) {
			            e.printStackTrace();
			        }
			    }
			    
			    if (toUnclose.size() > 0) {
			    	
			    	for (Long searchIdAux : toUnclose) {
						String userLogin = ua.getNiceName();
				    	String msgStr = "\n</div><div><BR><B>Search Unlocked</B> on: " + SearchLogger.getTimeStampFormat1(userLogin, "&#44; ") + "</BR></div>\n";
	        			SearchLogger.infoUpdateToDB(msgStr, searchIdAux);
					}

			    	DBManager.updateSearchesInvoiceStatus(
			    			org.apache.commons.lang.StringUtils.join(toUnclose.iterator(),","), "CHECKED_BY", 0);
			    	DBManager.uncloseSearch(toUnclose);
			    	
			    	StringBuffer note = new StringBuffer();
			    	note.append("\nUnlocked At: ").append(dateTimeStr).append(" by ").append(ua.getNiceName());
			    	NoteMapper.setSearchNote(org.apache.commons.lang.StringUtils.join(toUnclose.iterator(),","),	note.toString(), 
			    											ua.getID().intValue(), TYPE.UNLOCKED.getValue(), cal.getTime());
			    	
			    	for(long searchId:toUnclose){
			    		DBManager.unlockSearchExternal(searchId);
			    	}
			    }
			    
			    if (errorMessage != null) {
			        
			        errorMessage = "The following searches cannot be unlocked because they are in use.\n" + errorMessage + "\n" + 
			        	"Please try to Unlock it later or ask the search's owner to use the Save Unlocked option";
			        errorMessage = StringEscapeUtils.escapeJavaScript(errorMessage);
			        request.setAttribute("error", errorMessage);
			    }
				
			}
			if (operation.equals(RequestParamsValues.REPORTS_DELETE_SEARCHES)){
			    //if (!UserUtils.isTSAdmin(ua)) {
			    String[] ids = listChk.split(",");
			    
			    long id;
			    String availableIdList = null, errorMessage = null;
			    for (int i = 0; i < ids.length; i++) {
			        try {
			            
			            id = Long.parseLong(ids[i]);
			            
			            DBManager.SearchAvailabitily searchAvailable = DBManager.checkAvailability(id,ua.getID().longValue(), DBManager.CHECK_AVAILABLE, false);
			    		
			            Search search = SearchManager.getSearchByDBId(id);					            

			            long searchId = 0;
			            if (search != null)
			            	searchId = search.getSearchID();

			            ASThread currentAS = ASMaster.getSearch(searchId);
			            GPThread gpt = GPMaster.getThread(id);
			            
			            //pentru a se putea face unlock pe search, nu trebuie sa existe vreun thread de generare TSR sau automatic!
			            
			            if (currentAS != null) {
			            	errorMessage = (errorMessage != null ? errorMessage + "\\n" : "") + DBManager.getSearchFileNo(id);
			            } else if (gpt != null) {
			            	errorMessage = (errorMessage != null ? errorMessage + "\\n" : "") + DBManager.getSearchFileNo(id);
			            } else if (search != null) {
			            	errorMessage = (errorMessage != null ? errorMessage + "\\n" : "") + DBManager.getSearchFileNo(id);
			            }else if (searchAvailable.status != DBManager.SEARCH_AVAILABLE) {
			                errorMessage = (errorMessage != null ? errorMessage + "\\n" : "") + DBManager.getSearchFileNo(id);
			            }else {
			            	availableIdList = (availableIdList != null ? availableIdList + "," : "") + id;
			            }
			            			            
			        } catch (Exception e) {
			            e.printStackTrace();
			        }
		       }
			        
		        if (availableIdList != null) {
			        try {
			            DBManager.deleteSearch(availableIdList, ua);
			        }catch (BaseException e) {
						e.printStackTrace();
					} catch (UpdateDBException e2) {
						request.setAttribute("error", e2.toString().substring(e2.toString().indexOf(" ")));
					}
			    }
			    
			    if (errorMessage != null) {
			    	if ("null".equalsIgnoreCase(errorMessage)){
			    		errorMessage = "The search don't exist.";
			    	} else {
			    		errorMessage = "The following searches cannot be deleted because they are in use.\\n" + errorMessage;
			    		request.setAttribute("error", errorMessage);
			    	}		        
			    }
				/*}
			    else {
			        try {
                        DBManager.deleteSearch(listChk, ua);
                    } catch (Exception e) {
                    	logger.error("Error trying to delete searches", e);
                    } 
			    }*/
			}
			
			if (operation.equals(RequestParamsValues.REPORTS_ASSIGN_ABSTRACTOR) ||
				operation.equals(RequestParamsValues.REPORTS_ASSIGN_AGENT)
				) {
				
				boolean assignToAgent = operation.equals(RequestParamsValues.REPORTS_ASSIGN_AGENT)?true:false ;
				
				//assign abstractor permitted only to TSAdmin or commadmin or tscadmin
			    if (UserUtils.isTSAdmin(ua) || UserUtils.isCommAdmin(ua)|| UserUtils.isTSCAdmin( ua )) {
				    String[] ids = listChk.split(",");
				    
				    long id;
				    String availableIdList = null, errorMessage = null, errorMessage2 = null;
				    for (int i = 0; i < ids.length; i++) {
				        try {
				            
				            id = Long.parseLong(ids[i]);
				    		
				            Search search = SearchManager.getSearchByDBId(id);					            

				            ASThread currentAS = ASMaster.getSearch(id);
				            GPThread gpt = GPMaster.getThread(id);
				            
				            if (currentAS != null) {
				            	errorMessage = (errorMessage != null ? errorMessage + "\\n" : "") + DBManager.getSearchFileNo(id);
				            } else if (gpt != null) {
				            	errorMessage = (errorMessage != null ? errorMessage + "\\n" : "") + DBManager.getSearchFileNo(id);
				            } else if (search != null ) {
				            	errorMessage = (errorMessage != null ? errorMessage + "\\n" : "") + DBManager.getSearchFileNo(id);
				            }else {
				            	availableIdList = (availableIdList != null ? availableIdList + "," : "") + id;
				            }
				            			            
				        } catch (Exception e) {
				            e.printStackTrace();
				        }
				    }
		        	
		        	// check if we have a list of searches and at least one of XML and Email assign methods
			        if (availableIdList != null) {
			        	
			        	String[] abstractorIdValues = (assignToAgent)?
						        			request.getParameterValues( "reportAgent" ):
						        			request.getParameterValues( "reportAbstractor" );
			        	
			        	// assign the search 
			        	if(DBManager.overrideAbstractor(availableIdList, ua, abstractorIdValues,  assignToAgent)){
			        		//the abstractor was changed succesfully so we can proceed to sending the order by email or xml
			        		String[] searchIds = availableIdList.split(",");
							UserAttributes abstractorUserAttributes = UserUtils.getUserFromId(new BigDecimal(abstractorIdValues[0]));
							
							StringBuffer note = new StringBuffer();
							note.append("\nAssigned At: ").append(dateTimeStr).append(" to: ").append(abstractorUserAttributes.getNiceName())
														.append(" by ").append(ua.getNiceName());
			        		NoteMapper.setSearchNote(availableIdList, note.toString(), ua.getID().intValue(), TYPE.ASSIGNED.getValue(), cal.getTime());

							if (!assignToAgent)
							{
				        		for (int i = 0; i < searchIds.length; i++)
								{
				        			Long searchIdAux = Long.parseLong(searchIds[i]);
									String msgStr = "\n</div><div><BR><B>Order re-assigned to Abstractor</B>: " + abstractorUserAttributes.getNiceName() + " on " + dateTimeStr.replaceFirst(" ", ", ") + "</BR></div>\n";
				        			SearchLogger.infoUpdateToDB(msgStr, searchIdAux);
								}
							}
							else
							{
				        		for (int i = 0; i < searchIds.length; i++)
								{
				        			Long searchIdAux = Long.parseLong(searchIds[i]);
									String msgStr = "\n</div><div><BR><B>Search re-assigned to Agent</B>: " + abstractorUserAttributes.getNiceName() + " on " + dateTimeStr.replaceFirst(" ", ", ") + "</BR></div>\n";
				        			SearchLogger.infoUpdateToDB(msgStr, searchIdAux);
								}
							}
			        		boolean deliveryNotification = false;
			        		deliveryNotification = ((user != null) && (user.getUserAttributes().getMyAtsAttributes().getReceive_notification() == 1));
			        		for (int i = 0; i < searchIds.length; i++) {
			        			
			        			long currentSearchId = Long.parseLong(searchIds[i]);
			        			if(UserAttributes.OS_SUS.equalsIgnoreCase(abstractorUserAttributes.getOUTSOURCE())){
			        				byte orderFileAsByteArray[] = DBManager.getSearchOrderLogs(currentSearchId, FileServlet.VIEW_ORDER, false);
				        			if(orderFileAsByteArray!=null){
				        				String orderFile = new String(orderFileAsByteArray);
					        			sendMail(orderFile, abstractorUserAttributes, Integer.parseInt(request.getParameter("searchId")), assignToAgent);
				        			} else {
				        				errorMessage2 = errorMessage2 != null ? errorMessage2 + "\\n" : "";
				        				String error = "Search " + DBManager.getSearchFileNo(currentSearchId) +  " does not have an HTML order file!";
				        				errorMessage2 += error;
					        		}
			        			}else if(UserAttributes.OS_DISABLED.equalsIgnoreCase(abstractorUserAttributes.getOUTSOURCE())){
			        				
			        			}else { 
			        					//automatic integration
			        					if(UserAttributes.OS_ASK == abstractorUserAttributes.getOUTSOURCE()){
			        					String askAddress = DBManager.getConfigByName("ask.send.order.address");
					        			byte [] htmlOrder = DBManager.getSearchOrderLogs(currentSearchId, FileServlet.VIEW_ORDER, false);
					        			String xmlOrder = null;
					        			if(htmlOrder != null){
					        				xmlOrder = Search.getXmlOrder(new String(htmlOrder));
					        			}
					        			if(xmlOrder == null){				        				
					        				errorMessage2 = errorMessage2 != null ? errorMessage2 + "\\n" : "";
					        				String error = "Search " + DBManager.getSearchFileNo(currentSearchId) +  " does not have an XML order file!";
					        				errorMessage2 += error;
					        				continue;
					        			}
					        			// send the order to ASK server
										ASKServer.sendXmlOrder(xmlOrder, askAddress);
				        			}else { //abstractor does not support automatic integration 				        				
				        				errorMessage2 = errorMessage2 != null ? errorMessage2 + "\\n" : "";
				        				String error = "Abstractor " + abstractorUserAttributes.getLOGIN() +  " cannot be assinged XML order.";				        				
				        				errorMessage2 += error;
				        				continue;
				        			}
			        			}
			        		}
			        	}
				    }
				    
			        // set the error message
				    if (errorMessage != null || errorMessage2 != null) {
				    	String error = "";
				    	if(errorMessage != null){
				    		error = "The following searches abstractor cannot be modified because they are in use.\\n" + errorMessage;
				    	}
				    	if(errorMessage2 != null){
				    		if(!StringUtils.isEmpty(error)){
				    			error += "\n";
				    		}
				    		error += errorMessage2;
				    	}
				        request.setAttribute("error", error);
				    }
				}
			}
			
			if ((RequestParamsValues.REPORTS_SET_FVS_SEARCHES.equals(operation) 
					|| RequestParamsValues.REPORTS_RESET_FVS_SEARCHES.equals(operation)) 
				&& listChk != null){
				
				String[] ids = listChk.split(",");
				Vector<Long> toFVS = new Vector<Long>();
			    
				boolean resetFVSFlag = RequestParamsValues.REPORTS_RESET_FVS_SEARCHES.equals(operation);
			    long id;
			    String errorMessage = null;
			    String successMessage = null;
			    
			    for (int i = 0; i < ids.length; i++) {
			        try {
			            
			            id = Long.parseLong(ids[i]);
			            
			            DBManager.SearchAvailabitily searchAvailable = DBManager.checkAvailability(id,ua.getID().longValue(), DBManager.CHECK_AVAILABLE, false);
			    		
			            Search search = SearchManager.getSearchByDBId(id);					            

			            long searchId = 0;
			            if (search != null)
			            	searchId = search.getSearchID();

			            ASThread currentAS = ASMaster.getSearch(searchId);
			            GPThread gpt = GPMaster.getThread(id);
			            
			            //pentru a se putea face unlock pe search, nu trebuie sa existe vreun thread de generare TSR sau automatic!
			            
			            if (currentAS != null) {
			            	//errorMessage = (errorMessage != null ? errorMessage + "\\n" : "") + DBManager.getSearchFileNo(id);
			            } else if (gpt != null) {
			            	//errorMessage = (errorMessage != null ? errorMessage + "\\n" : "") + DBManager.getSearchFileNo(id);
			            } else if (DBManager.getTSRGenerationStatus(id) != Search.SEARCH_TSR_CREATED ){
			            	//errorMessage = (errorMessage != null ? errorMessage + "\\n" : "") + "TSR not created for " + DBManager.getSearchFileNo(id);
						} else if (search != null) {
			            	//errorMessage = (errorMessage != null ? errorMessage + "\\n" : "") + DBManager.getSearchFileNo(id);
			            }else if (searchAvailable.status != DBManager.SEARCH_AVAILABLE) {
			               // errorMessage = (errorMessage != null ? errorMessage + "\\n" : "") + DBManager.getSearchFileNo(id);
			            } else {
			            	toFVS.add(id);
			            }
			            			            
			        } catch (Exception e) {
			            e.printStackTrace();
			        }
		       }
			    
			    for (Long search_id : toFVS) {
			        try {
			            DBManager.setFVSFlagSearch(search_id, ua, resetFVSFlag);
			            successMessage = "FVS flag has been Set for complete orders selected.";
			            
			            if (resetFVSFlag){
			            	successMessage = "FVS flag has been Reset for flagged orders selected.";
			            }
			        }catch (BaseException e) {
						e.printStackTrace();
					} catch (UpdateDBException e2) {
						request.setAttribute("error", e2.toString().substring(e2.toString().indexOf(" ")));
					}
			    }
			    
			    if (successMessage != null){
			        request.setAttribute("success", successMessage);
			    }

//			    if (errorMessage != null) {
//			        
//			        errorMessage = "The following searches cannot be flagged because they are in use or tsr was not created.\\n" + errorMessage;
//			        request.setAttribute("error", errorMessage);
//			    }
			} else if (RequestParamsValues.REPORTS_EXPORT_DASHBOARD.equals(operation)){
				exportFileAvailable = exportReports(request, response, ua);
			}
		}
		if (!exportFileAvailable){
			forward(request, response, request.getParameter( RequestParams.REPORTS_PAGE_NAME ).substring(URLMaping.path.length()));
		}
	}

	/**
	 * @param request
	 * @param response
	 * @param ua
	 * @param ss
	 * @return
	 */
	public boolean exportReports(HttpServletRequest request, HttpServletResponse response, UserAttributes ua) {
		int[] reportStatus = {-1};
		if (request.getParameterValues(RequestParams.REPORTS_STATUS) != null){
			try {
				String statuses = Arrays.toString(request.getParameterValues(RequestParams.REPORTS_STATUS)).replaceAll("[\\[\\]\\s]+", "");
				reportStatus = Util.extractArrayFromString(statuses);
			} catch (Exception e) {
			}
		}
		int[] reportState = {-1};
		if (request.getParameterValues(RequestParams.REPORTS_STATE) != null){
			try {
				String states = Arrays.toString(request.getParameterValues(RequestParams.REPORTS_STATE)).replaceAll("[\\[\\]\\s]+", "");
				reportState = Util.extractArrayFromString(states);
			} catch (Exception e) {
			}
		}
		int[] reportCounty = {-1};
		if (request.getParameterValues(RequestParams.REPORTS_COUNTY) != null){
			try {
				String counties = Arrays.toString(request.getParameterValues(RequestParams.REPORTS_COUNTY)).replaceAll("[\\[\\]\\s]+", "");
				reportCounty = Util.extractArrayFromString(counties);
			} catch (Exception e) {
			}
		}
		int[] reportAgent = {-1};
		if (request.getParameterValues(RequestParams.REPORTS_AGENT) != null){
			try {
				String agents = Arrays.toString(request.getParameterValues(RequestParams.REPORTS_AGENT)).replaceAll("[\\[\\]\\s]+", "");
				reportAgent = Util.extractArrayFromString(agents);
			} catch (Exception e) {
			}
		}
		int[] reportAbstractor = {-1};
		if (request.getParameterValues(RequestParams.REPORTS_ABSTRACTOR) != null){
			try {
				String abs = Arrays.toString(request.getParameterValues(RequestParams.REPORTS_ABSTRACTOR)).replaceAll("[\\[\\]\\s]+", "");
				reportAbstractor = Util.extractArrayFromString(abs);
			} catch (Exception e) {
			}
		}
		String[] reportCompanyAgent = {"-1"};
		if (request.getParameterValues(RequestParams.REPORTS_COMPANY_AGENT) != null){
			try {
				String compAgents = Arrays.toString(request.getParameterValues(RequestParams.REPORTS_COMPANY_AGENT)).replaceAll("[\\[\\]\\s]+", "");
				reportCompanyAgent = Util.extractStringArrayFromString(compAgents);
			} catch (Exception e) {
			}
		}
		int fromDay = -1; 
		if (org.apache.commons.lang.StringUtils.isNotBlank(request.getParameter(RequestParams.REPORTS_FROM_DAY))){
			try {
				fromDay = Integer.parseInt(request.getParameter(RequestParams.REPORTS_FROM_DAY));
			} catch (Exception e) {
			}
		}
		int fromMonth = -1; 
		if (org.apache.commons.lang.StringUtils.isNotBlank(request.getParameter(RequestParams.REPORTS_FROM_MONTH))){
			try {
				fromMonth = Integer.parseInt(request.getParameter(RequestParams.REPORTS_FROM_MONTH));
			} catch (Exception e) {
			}
		}
		int fromYear = -1;
		if (org.apache.commons.lang.StringUtils.isNotBlank(request.getParameter(RequestParams.REPORTS_FROM_YEAR))){
			try {
				fromYear = Integer.parseInt(request.getParameter(RequestParams.REPORTS_FROM_YEAR));
			} catch (Exception e) {
			}
		}
		int toDay = -1;
		if (org.apache.commons.lang.StringUtils.isNotBlank(request.getParameter(RequestParams.REPORTS_TO_DAY))){
			try {
				toDay = Integer.parseInt(request.getParameter(RequestParams.REPORTS_TO_DAY));
			} catch (Exception e) {
			}
		}
		int toMonth = -1;
		if (org.apache.commons.lang.StringUtils.isNotBlank(request.getParameter(RequestParams.REPORTS_TO_MONTH))){
			try {
				toMonth = Integer.parseInt(request.getParameter(RequestParams.REPORTS_TO_MONTH));
			} catch (Exception e) {
			}
		}
		int toYear = -1;
		if (org.apache.commons.lang.StringUtils.isNotBlank(request.getParameter(RequestParams.REPORTS_TO_YEAR))){
			try {
				toYear = Integer.parseInt(request.getParameter(RequestParams.REPORTS_TO_YEAR));
			} catch (Exception e) {
			}
		}

		String orderBy = request.getParameter(RequestParams.REPORTS_ORDER_BY);
		String orderType = request.getParameter(RequestParams.REPORTS_ORDER_TYPE);
		
		int commId = -1;
		if (org.apache.commons.lang.StringUtils.isNotBlank(request.getParameter(RequestParams.COMM_ID))){
			try {
				commId = Integer.parseInt(request.getParameter(RequestParams.COMM_ID));
			} catch (Exception e) {
			}
		}
		int reportPage = -1;
		if (org.apache.commons.lang.StringUtils.isNotBlank(request.getParameter(RequestParams.REPORTS_PAGE))){
			try {
				reportPage = Integer.parseInt(request.getParameter(RequestParams.REPORTS_PAGE));
			} catch (Exception e) {
			}
		}
		int invoice = 0;
		
		int reportDateType = RequestParamsValues.REPORTS_DATE_TYPE_COMBINED; //default to lastest date
		try{
			reportDateType = Integer.parseInt(request.getParameter(RequestParams.REPORTS_DATE_TYPE));
		} catch(NumberFormatException e){
			reportDateType = RequestParamsValues.REPORTS_DATE_TYPE_COMBINED;
		}
		
		int monthReport = -1;
		if (org.apache.commons.lang.StringUtils.isNotBlank(request.getParameter(RequestParams.REPORTS_MONTH))){
			try {
				monthReport = Integer.parseInt(request.getParameter(RequestParams.REPORTS_MONTH));
			} catch (Exception e) {
			}
		}
		
		int yearReport = -1;
		if (org.apache.commons.lang.StringUtils.isNotBlank(request.getParameter(RequestParams.REPORTS_YEAR))){
			try {
				yearReport = Integer.parseInt(request.getParameter(RequestParams.REPORTS_YEAR));
			} catch (Exception e) {
			}
		}
		
		int dayReport = -1;
		if (org.apache.commons.lang.StringUtils.isNotBlank(request.getParameter(RequestParams.REPORTS_DAY))){
			try {
				dayReport = Integer.parseInt(request.getParameter(RequestParams.REPORTS_DAY));
			} catch (Exception e) {
			}
		}
		
		int rowsPerPage = 10000;
		int offset = (reportPage - 1)* rowsPerPage;
		String reportsPageName = request.getParameter(RequestParams.REPORTS_PAGE_NAME);
		
		String TSRsearchString = request.getParameter(RequestParams.REPORTS_SEARCH_TSR);
		String reportsSearchField = request.getParameter(RequestParams.REPORTS_SEARCH_FIELD);
		
		try {
			//reading data from the DB
			DayReportLineData[] allReports = null;
			
			if (reportsPageName.contains(URLMaping.REPORTS_INTERVAL)){
				//interval report
				allReports = DBReports.getIntervalReportData(
						reportCounty, reportAbstractor, reportAgent, reportState, reportCompanyAgent, 
						fromDay, fromMonth, fromYear, 
						toDay, toMonth, toYear, 
						orderBy, orderType, commId, reportStatus, invoice, offset, rowsPerPage,ua, reportDateType);
			} else if (reportsPageName.contains(URLMaping.REPORT_DAY)){
				//day report
				allReports = DBReports.getIntervalReportData(
						reportCounty, reportAbstractor, reportAgent, reportState, reportCompanyAgent, 
						dayReport, monthReport, yearReport, 
						dayReport, monthReport, yearReport, 
						orderBy, orderType, commId, reportStatus, invoice,offset,rowsPerPage,ua, reportDateType);
			} else if (reportsPageName.contains(URLMaping.REPORTS_MONTH_DETAILED)){
				//month report
				Calendar c = Calendar.getInstance();
				c.set(Calendar.YEAR, yearReport);
				c.set(Calendar.MONTH, monthReport - 1);
				
				allReports = DBReports.getIntervalReportData(
						reportCounty, reportAbstractor, reportAgent, reportState, reportCompanyAgent, 
						1, monthReport, yearReport, 
						c.getActualMaximum(Calendar.DAY_OF_MONTH), monthReport, yearReport, 
						orderBy, orderType, commId, reportStatus, invoice, offset, rowsPerPage,ua, reportDateType);
			} else if (reportsPageName.contains(URLMaping.REPORT_SEARCH)){
				//search report
				
				int payrateType = 0;
				if (UserUtils.isTSAdmin(ua))
					payrateType = 1;

				// convert search term to upper case
				TSRsearchString = TSRsearchString.toUpperCase();
				//by default All statuses is used for search
				DayReportLineData[] ReportData = new DayReportLineData[0];
				
				HashMap<String, Object> extraSearchFields = new HashMap<String, Object>();
				if (reportsSearchField.equalsIgnoreCase("Legal Description")) {
					extraSearchFields = LegalUtils.getLegalParams(TSRsearchString);
					if (extraSearchFields.get("SNname") != null) {
						extraSearchFields.put("SNname", ((String)extraSearchFields.get("SNname")).replaceAll("[\\s-]", ""));
					}
					
				} else {
					if (reportsSearchField.equalsIgnoreCase("Property Address")) {
						extraSearchFields.put("SNname", TSRsearchString.replaceAll("[\\s-]", ""));
					} else {
						if ("Property Owners".equalsIgnoreCase(reportsSearchField)) {
							extraSearchFields.put("SNname", TSRsearchString.replaceAll("[\\s-]", ""));
						} else if("TSR File ID".equalsIgnoreCase(reportsSearchField)){
							extraSearchFields.put("SNname", TSRsearchString.replaceAll("[\\s-_]", ""));
						} else {
							extraSearchFields.put("SNname", TSRsearchString.replaceAll("[\\s-]", ""));
						}
						
					}
				}
				// read data from DB		
				long searchId = Long.parseLong(request.getParameter(RequestParams.SEARCH_ID));
				
				String reportsSearchFieldFrom = org.apache.commons.lang.StringUtils.defaultString(request.getParameter(RequestParams.REPORTS_SEARCH_FIELD_FROM), "");
				String reportsSearchAll = org.apache.commons.lang.StringUtils.defaultString(request.getParameter(RequestParams.REPORTS_SEARCH_ALL));
				
				if (reportsSearchFieldFrom.equals("starter") && StringUtils.isEmpty(TSRsearchString)){
					
					ReportData = SearchReportLoopTag.getStarterData(
							InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext(), 
							ua, 
							payrateType,
							reportCounty, 
							reportAbstractor, 
							reportAgent, 
							reportState, 
							reportCompanyAgent, 
							orderBy, 
							orderType, 
							reportStatus, 
							invoice, 
							reportDateType);
					
					for (int i = 0; i < ReportData.length; i++) {
						String fileLink = ReportData[i].getFileLink();
						fileLink = fileLink.replaceAll("menusForStarters\\[(\\d+)\\]", "menusForStarters[" + i + "]");
						ReportData[i].setFileLink(fileLink);
					}
					
				} else {
					if (reportsSearchAll.equals("on")){
						ReportData = DBReports.getSearchReportAllInOne(
							reportCounty, reportAbstractor, reportAgent, reportState, reportCompanyAgent, orderBy, orderType, commId, reportStatus, invoice, 
							extraSearchFields.containsKey("SNname") ? (String)extraSearchFields.get("SNname") : "",
							1, 1, 1950, 31, 12, 2050, reportsSearchField, ua, payrateType,
							extraSearchFields.containsKey("LOT") ? (String)extraSearchFields.get("LOT") : "",
							extraSearchFields.containsKey("BLOCK") ? (String)extraSearchFields.get("BLOCK") : "",
							extraSearchFields.containsKey("PHASE") ? (String)extraSearchFields.get("PHASE") : "",
							extraSearchFields.containsKey("SECTION") ? (String)extraSearchFields.get("SECTION") : "",
							false, reportDateType
						);
			
					} else{
						// type of current search window 
						String searchType = request.getParameter(RequestParams.TSR_SEARCH_TYPE);
			
						if (searchType.equals(RequestParamsValues.TSR_SEARCH_INTERVAL) ||
								searchType.equals(RequestParamsValues.TSR_SEARCH_DAY)){
								ReportData = DBReports.getSearchReportAllInOne(
									reportCounty, reportAbstractor, reportAgent, reportState, reportCompanyAgent, orderBy, orderType, commId, reportStatus, invoice, 
									extraSearchFields.containsKey("SNname") ? (String)extraSearchFields.get("SNname") :" ",
									fromDay, fromMonth, fromYear, toDay, toMonth, toYear, reportsSearchField, ua, payrateType,
									extraSearchFields.containsKey("LOT") ? (String)extraSearchFields.get("LOT") : "",
									extraSearchFields.containsKey("BLOCK") ? (String)extraSearchFields.get("BLOCK") : "",
									extraSearchFields.containsKey("PHASE") ? (String)extraSearchFields.get("PHASE") : "",
									extraSearchFields.containsKey("SECTION") ? (String)extraSearchFields.get("SECTION") : "",
									false, reportDateType
								);
										
					    } else if (searchType.equals(RequestParamsValues.TSR_SEARCH_MONTH)){
					    	
						    	Calendar now = Calendar.getInstance();
						    	now.set(Calendar.MONTH, monthReport - 1);
						    	now.set(Calendar.YEAR, yearReport);
								
								ReportData = DBReports.getSearchReportAllInOne(
									reportCounty, reportAbstractor, reportAgent, reportState, reportCompanyAgent, orderBy, orderType, commId, reportStatus, invoice, 
									extraSearchFields.containsKey("SNname") ? (String)extraSearchFields.get("SNname"):"",
									1, monthReport, yearReport, now.getActualMaximum(Calendar.DAY_OF_MONTH), monthReport, yearReport, reportsSearchField, ua, payrateType,
									extraSearchFields.containsKey("LOT") ? (String)extraSearchFields.get("LOT") : "",
									extraSearchFields.containsKey("BLOCK") ?( String)extraSearchFields.get("BLOCK") : "",
									extraSearchFields.containsKey("PHASE") ? (String)extraSearchFields.get("PHASE") : "",
									extraSearchFields.containsKey("SECTION") ? (String)extraSearchFields.get("SECTION") : "",
									false, reportDateType
								);
					    			    	
					    } else if (searchType.equals(RequestParamsValues.TSR_SEARCH_YEAR)){
					    	
						    	ReportData = DBReports.getSearchReportAllInOne(
										reportCounty, reportAbstractor, reportAgent, reportState, reportCompanyAgent, orderBy, orderType, commId, reportStatus, invoice, 
										extraSearchFields.containsKey("SNname") ? (String)extraSearchFields.get("SNname") : "",
										1, 1, yearReport, 31, 12, yearReport, reportsSearchField, ua, payrateType,
										extraSearchFields.containsKey("LOT") ? (String)extraSearchFields.get("LOT") : "",
										extraSearchFields.containsKey("BLOCK") ? (String)extraSearchFields.get("BLOCK") : "",
										extraSearchFields.containsKey("PHASE") ? (String)extraSearchFields.get("PHASE") : "",
										extraSearchFields.containsKey("SECTION") ? (String)extraSearchFields.get("SECTION") : "",
										false, reportDateType
									);
					    }
					}
				}
				
				if (Util.isValueInArray(14, reportStatus)){
				    reportStatus = Util.NandTnotO(reportStatus);
				}
				
				if (ReportData != null){
					allReports = CommAdminNotifier.filterResults(ReportData, reportStatus, new BigDecimal(String.valueOf(commId)));
				}
			} 
			
			if (allReports != null && allReports.length > 0){
				CommunityAttributes commInfo = CommunityUtils.getCommunityFromId(commId);
				
				List<OrdersExportATS2ReportBean> orders = new LinkedList<OrdersExportATS2ReportBean>();
				for (DayReportLineData report : allReports) {
					OrdersExportATS2ReportBean order = new OrdersExportATS2ReportBean();
					order.setAbstractor(StringEscapeUtils.unescapeHtml(report.getAbstractorColumn()).replaceAll("(?is)<br>", "\n").replaceAll("&", "&amp;"));
					order.setOwners((StringEscapeUtils.unescapeHtml(report.getOwnerName()).replaceAll("(?is)<br>", "\n")).replaceAll("&", "&amp;"));
					order.setAgent(StringEscapeUtils.unescapeHtml(report.getAgentName()).replaceAll("(?is)<br>", "\n").replaceAll("&", "&amp;"));
					order.setCounty(StringEscapeUtils.unescapeHtml(report.getPropertyFullCounty()).replaceAll("(?is)<br>", "\n"));
					order.setPropertyAddress(StringEscapeUtils.unescapeHtml(report.getPropertyAddress()).replaceAll("(?is)<br>", "\n").replaceAll("&", "&amp;"));
					order.setTsOrder(StringEscapeUtils.unescapeHtml(report.getDateHour()));
					order.setTsDone(StringEscapeUtils.unescapeHtml(report.getTSRDateHourNew()).replaceAll("(?is)<br>", "\n"));
					order.setTotalTimeWorked(StringEscapeUtils.unescapeHtml(report.getTotalTimeWorkedFormatted()));
					order.setTsrFileId(StringEscapeUtils.unescapeHtml(report.getFileId()));
					order.setStatus(StringEscapeUtils.unescapeHtml(report.getStatus()));
					order.setNote(StringEscapeUtils.unescapeHtml(report.getSearchDueDate()));
					order.setLogs(StringEscapeUtils.unescapeHtml(report.getLogFiles().replaceAll("(?is)</?span[^>]*>", "")));
					
					orders.add(order);
				}
				
				if (orders.size() > 0){
					HashMap<String, String> filters = new HashMap<String, String>();
					if (fromMonth != -1 && fromDay != -1 && fromYear != -1){
						String fromDate = fromMonth + "/" + fromDay + "/" + fromYear;
						filters.put("FromDate", fromDate);
					}
					if (toMonth != -1 && toDay != -1 && toYear != -1){
						String toDate = toMonth + "/" + toDay + "/" + toYear;
						filters.put("ToDate", toDate);
					}
					StringBuilder states = new StringBuilder();
					if (reportState.length == 1){
						if (reportState[0] != -1){
							String stateAbbrev = State.getState(reportState[0]).getStateAbv();
							states.append(stateAbbrev);
						}
					} else{
						for (int i : reportState) {
							String stateAbbrev = State.getState(i).getStateAbv();
							if (states.length() == 0){
								states.append(stateAbbrev);
							} else{
								states.append(", ").append(stateAbbrev);
							}
						}
					}
					if (states.length() == 0){
						states.append("All");
					}
					filters.put("States", states.toString());
					
					StringBuilder counties = new StringBuilder();
					if (reportCounty.length == 1){
						if (reportCounty[0] != -1){
							County cunt = County.getCounty(reportCounty[0]);
							String county = cunt.getState().getStateAbv() + " " + cunt.getName();
							counties.append(county);
						}
					} else{
						for (int i : reportCounty) {
							County cunt = County.getCounty(i);
							String county = cunt.getState().getStateAbv() + " " + cunt.getName();
							if (counties.length() == 0){
								counties.append(county);
							} else{
								counties.append(", ").append(county);
							}
						}
					}
					if (counties.length() == 0){
						counties.append("All");
					}
					filters.put("Counties", counties.toString());
					
					StringBuilder abstractors = new StringBuilder();
					if (reportAbstractor.length == 1){
						if (reportAbstractor[0] != -1){
							String abstractor = UserManager.getUser(new BigDecimal(reportAbstractor[0])).getUserFullName();
							abstractors.append(abstractor);
						}
					} else{
						for (int i : reportAbstractor) {
							String abstractor = UserManager.getUser(new BigDecimal(i)).getUserFullName();
							if (abstractors.length() == 0){
								abstractors.append(abstractor);
							} else{
								abstractors.append(", ").append(abstractor);
							}
						}
					}
					if (abstractors.length() == 0){
						abstractors.append("All");
					}
					filters.put("Abstractors", abstractors.toString());
					
					StringBuilder agents = new StringBuilder();
					if (reportAgent.length == 1){
						if (reportAgent[0] != -1){
							String agent = UserManager.getUser(new BigDecimal(reportAgent[0])).getUserFullName();
							agents.append(agent);
						}
					} else{
						for (int i : reportAgent) {
							String agent = UserManager.getUser(new BigDecimal(i)).getUserFullName();
							if (agents.length() == 0){
								agents.append(agent);
							} else{
								agents.append(", ").append(agent);
							}
						}
					}
					if (agents.length() == 0){
						agents.append("All");
					}
					filters.put("Agents", agents.toString());
					
					StringBuilder agencies = new StringBuilder();
					if (reportCompanyAgent.length == 1){
						if (!"-1".equals(reportCompanyAgent[0])){
							agencies.append(reportCompanyAgent[0]);
						}
					} else{
						for (String comp : reportCompanyAgent) {
							if (agencies.length() == 0){
								agencies.append(comp);
							} else{
								agencies.append(", ").append(comp);
							}
						}
					}
					if (agencies.length() == 0){
						agencies.append("All");
					}
					filters.put("Agencies", agencies.toString());
					
					StringBuilder statuses = new StringBuilder();
					Map<Integer, String> allStatusesMapById = StatusSelect.getAllStatusesMapById();
					if (reportStatus.length == 1){
						if (reportStatus[0] != -1){
							statuses.append(allStatusesMapById.get(reportStatus[0]));
						}
					} else{
						for (int i : reportStatus) {
							if (statuses.length() == 0){
								statuses.append(allStatusesMapById.get(i));
							} else{
								statuses.append(", ").append(allStatusesMapById.get(i));
							}
						}
					}
					if (statuses.length() == 0){
						statuses.append("All");
					}
					filters.put("Statuses", statuses.toString());
					
					if (org.apache.commons.lang.StringUtils.isNotBlank(TSRsearchString) && org.apache.commons.lang.StringUtils.isNotBlank(reportsSearchField)){
						filters.put("FindField", reportsSearchField);
						filters.put("FindValue", TSRsearchString);
					}
					String path = exportXlsReport(allReports, commInfo.getNAME(), orders, filters);
					
					if (StringUtils.isNotEmpty(path)) {
						File f = new File(path);
						response.setHeader("Content-Disposition", " attachment; filename=\"" + FilenameUtils.getName(path) + "\"");
						response.setContentType(".xls");
						response.setContentLength((int)f.length());
						
						InputStream in=new BufferedInputStream(new FileInputStream(f));
						OutputStream out=response.getOutputStream();
						
						byte[] buff=new byte[100];
						int n;
						while ( (n=in.read(buff)) > 0) {
						 	 out.write(buff, 0, n);
						}
						in.close();					 
						out.close();
						return true;
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public String exportXlsReport(DayReportLineData[] reportData, String community, List<OrdersExportATS2ReportBean> orders, HashMap<String, String> filters) {
		
		String templateName = "reportsXLStemplate.jrxml";
		String fileLocation = null;

		// MAKE SURE THE COLUMNS DON'T OVERLAP IN TEMPLATE
		String pathTemplate = ServerConfig.getRealPath() + File.separator + "WEB-INF" + File.separator + "classes"
				+ File.separator + "ro" + File.separator + "cst" + File.separator + "tsearch" + File.separator
				+ "reports" + File.separator + "templates" + File.separator + templateName;


		Map<String, Object> mapParameter = new HashMap<String, Object>();

		try {
			mapParameter.put("TotalOrders", Integer.toString(reportData.length));
			mapParameter.put("CommunityName", community);
			if (filters.containsKey("FromDate")){
				mapParameter.put("FromDate", filters.get("FromDate"));
			}
			if (filters.containsKey("ToDate")){
				mapParameter.put("ToDate", filters.get("ToDate"));
			}
			if (filters.containsKey("States")){
				mapParameter.put("States", filters.get("States"));
			}
			if (filters.containsKey("Counties")){
				mapParameter.put("Counties", filters.get("Counties"));
			}
			if (filters.containsKey("Abstractors")){
				mapParameter.put("Abstractors", filters.get("Abstractors"));
			}
			if (filters.containsKey("Agents")){
				mapParameter.put("Agents", filters.get("Agents"));
			}
			if (filters.containsKey("Agencies")){
				mapParameter.put("Agencies", filters.get("Agencies"));
			}
			if (filters.containsKey("Statuses")){
				mapParameter.put("Statuses", filters.get("Statuses"));
			}
			
			if (filters.containsKey("FindField") && filters.containsKey("FindValue")){
				mapParameter.put("FindField", filters.get("FindField"));
				mapParameter.put("FindValue", filters.get("FindValue"));
			}

			String reportFileName = community + "_OrdersReport.xls";

			String path = ServerConfig.getTsrCreationTempFolder();
			String directoryName = "orderReports" + Long.toString(System.currentTimeMillis());
			File tmpDir = new File("");
			if (StringUtils.isNotEmpty(path)) {
				path += File.separator + directoryName + File.separator;
				tmpDir = new File(path);
				if (!tmpDir.isDirectory())
					tmpDir.mkdir();
			}
			fileLocation = path + File.separator + reportFileName;

			JasperReport jRep = JasperCompileManager.compileReport(pathTemplate);

			
			JasperPrint jPrint = JasperFillManager.fillReport(jRep, mapParameter, new JRBeanCollectionDataSource(orders));
			JExcelApiExporter excelExporter = new JExcelApiExporter();
			excelExporter.setParameter(JRExporterParameter.JASPER_PRINT, jPrint);
			excelExporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, fileLocation);
			excelExporter.setParameter(JRXlsAbstractExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE);
			excelExporter.setParameter(JRXlsAbstractExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
			excelExporter.exportReport();

		} catch (Exception e) {
			fileLocation = null;
			e.printStackTrace();
		}

		return fileLocation;
	}

	public static  boolean sendMail(String body, UserAttributes userAttributes, long searchId, boolean assignToAgent) {
        
        try {
        	
        	UserAttributes commAdmin = InstanceManager.getManager().getCurrentInstance(searchId).getCurrentUser();
            
			Properties props = System.getProperties();
			props.put("mail.smtp.host", MailConfig.getMailSmtpHost());
			Session session = Session.getDefaultInstance(props,null);
			MimeMessage msg = new MimeMessage(session);
			
			InternetAddress fromAddress = null;
			try {
			    fromAddress = new InternetAddress(commAdmin.getEMAIL());
			} catch (Exception ex) {
			    fromAddress = new InternetAddress(MailConfig.getMailFrom());
			}
			msg.setFrom(fromAddress);
			
			if(assignToAgent)
				msg.setSubject("A new search was assigned to agent " + userAttributes.getUserFullName() + ". ");
			else
				msg.setSubject("A new search was assigned to abstractor " + userAttributes.getUserFullName() + ". ");
			
			msg.setRecipients(javax.mail.Message.RecipientType.TO, 
							InternetAddress.parse(userAttributes.getEMAIL()));
			msg.setContent(body, "text/html");
			Transport.send(msg);
			
		} catch(Exception e){
			e.printStackTrace();
			return false;
		}
		
		return true;
    }
}