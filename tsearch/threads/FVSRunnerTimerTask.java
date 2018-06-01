package ro.cst.tsearch.threads;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.TimerTask;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.community.Products;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.rowmapper.FVSMapper;
import ro.cst.tsearch.emailOrder.MailOrder;
import ro.cst.tsearch.emailOrder.PlaceOrder;
import ro.cst.tsearch.exceptions.InvalidEmailOrderException;
import ro.cst.tsearch.exceptions.UpdateDBException;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.servlet.ValidateInputs;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.SearchLogger;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.TSOpCode;

public class FVSRunnerTimerTask extends TimerTask{

	@Override
	public void run() {
		if (ServerConfig.isFVSRunnerEnabled()){

			try {
				List<FVSMapper> flaggedSearches = null;
				try {
					flaggedSearches = DBManager.getFVSFlaggedSearches();
				} catch (UpdateDBException e) {
					e.printStackTrace();
				}
					
				if (flaggedSearches == null)
					return;
					
				if (flaggedSearches.size() == 0)
					return;
					
				Search search = null;
				SearchAttributes sa = null;
				
				Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
				
				for (FVSMapper map : flaggedSearches){
					
					try {
						Date run_time = map.getRun_time();
						if (run_time != null) {
							Calendar cal_run_time1 = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
							cal_run_time1.setTime(run_time);
							Calendar cal_run_time2 = Calendar.getInstance(TimeZone.getTimeZone("GMT-1"));
							cal_run_time2.setTime(run_time);
							
							boolean allow = ((cal.get(Calendar.HOUR_OF_DAY) == cal_run_time1.get(Calendar.HOUR_OF_DAY))
												|| (cal.get(Calendar.HOUR_OF_DAY) == cal_run_time2.get(Calendar.HOUR_OF_DAY)));
							
							if (allow) {

								String fileNumber = map.getAbstr_fileno();

								if (StringUtils.isNotEmpty(fileNumber)) {
									long comm_id = map.getComm_id();

									UserAttributes userA = UserUtils.getUserFromId(map.getAgent_id());

									if (userA == null)
										return;

									CommunityAttributes ca = CommunityUtils.getCommunityFromId(comm_id);
									int numberOfDays = ca.getNUMBEROFDAYS();

									Date flag_date = map.getFlag_date();
									Calendar cal_flag = Calendar.getInstance();
									cal_flag.setTime(flag_date);

									int flag_day = cal_flag.get(Calendar.DAY_OF_YEAR);
									int today = cal.get(Calendar.DAY_OF_YEAR);

									if (numberOfDays > 0 /*&& today > flag_day*/&& ((today - flag_day) % numberOfDays == 0)) {

										long search_id = map.getSearch_id();
										int numberOfUpdates = ca.getNUMBEROFUPDATES();
										int numberOfUpdatesRunned = 0;
										try {
											numberOfUpdatesRunned = DBManager.getFVSUpdatesRunned(search_id);
										} catch (UpdateDBException e) {
											e.printStackTrace();
										}

										if (numberOfUpdates > 0 && numberOfUpdates > numberOfUpdatesRunned) {

											User user = MailOrder.getUser(userA.getLOGIN());

											search = new Search(user, DBManager.getNextId(DBConstants.TABLE_SEARCH));
											search.setAgent(userA);
											InstanceManager.getManager().getCurrentInstance(search.getID()).setCurrentCommunity(ca);
											sa = search.getSa();

											sa.setSearchId(search.getID());
											sa.setCommId((int) comm_id);
											sa.setAtribute(SearchAttributes.ORDERBY_FILENO, fileNumber);
											sa.setAtribute(SearchAttributes.ABSTRACTOR_FILENO, fileNumber);
											sa.setAtribute(SearchAttributes.SEARCH_PRODUCT, Integer.toString(Products.FVS_PRODUCT));
											sa.setAtribute(SearchAttributes.FVS_UPDATE, "true");
											sa.setAtribute(SearchAttributes.FVS_UPDATE_AUTO_LAUNCHED, "true");
											sa.setAtribute(SearchAttributes.P_COUNTY, ((Long) map.getCounty_id()).toString());
											search.setFVSParentSearchID(search_id);

											numberOfUpdatesRunned++;
											boolean lastScheduled = false;
											if (numberOfUpdatesRunned == numberOfUpdates) {
												lastScheduled = true;
												sa.setAtribute(SearchAttributes.LAST_SCHEDULED_FVS_UPDATE, "true");
											}
											
											SearchManager.setSearch(search, user);
											//long searchId = search.getSearchID();
											
											ValidateInputs.checkIfUpdate(search, TSOpCode.SUBMIT_ORDER, null);
											boolean cancelFVS = false;
											
											if (search.getParentSA() != null) {
												if (search.getParentSA().getCertificationDate() == null
														|| search.getParentSA().getCertificationDate().getDate() == null) {
													cancelFVS = true;
												}
											} else{
												cancelFVS = true;
											}
											if (cancelFVS){
												lastScheduled = true;
												if (search.getParentSearchId() != Search.NO_UPDATED_SEARCH) {
													try {
														DBManager.setFVSFlagSearch(search.getParentSearchId(), user.getUserAttributes(), true);
													} catch (UpdateDBException e) {
														e.printStackTrace();
													}
												}
//												SearchLogger.info("</div><div><br>FVS Update aborted because Effective Date is missing on Original File "
//														+ SearchLogger.getTimeStampAndLocation(searchId) + "<br></div>", searchId);
												System.out.println("FVS Update not launched for searchId: " + search_id);
																						        
										        String commAdminEmail = null;
												try {
													UserAttributes commAdmin = UserUtils.getUserFromId(CommunityUtils.getCommunityAdministrator(ca));
													commAdminEmail = commAdmin.getEMAIL();
												} catch (Exception e) {
													e.printStackTrace();
												}
												Util.sendMail(null, user.getUserAttributes().getEMAIL(), commAdminEmail, null,
														"FVS Report on " + SearchLogger.getCurDateTimeCST() + " for fileID: " + fileNumber + ", searchid: " + search_id,
														"Missing Effective Date on Original File!\n\nFVS flag on Original File was reseted.");
											} else{
												// place order
												MailOrder mailOrder = new MailOrder();
												mailOrder.savedUser = user;
												try {
													PlaceOrder.placeOrder(search, mailOrder, true, "FVS Runner");
												} catch (IOException e) {
													e.printStackTrace();
												} catch (InvalidEmailOrderException e) {
													e.printStackTrace();
												}
												System.out.println("FVS Update launched for searchId: " + search_id);
											}
											try {
												DBManager.updateFVSUpdatesRunned(numberOfUpdatesRunned, search_id, lastScheduled);
											} catch (UpdateDBException e) {
												e.printStackTrace();
											}
										}
									}
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} 
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}
}
