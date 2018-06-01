package ro.cst.tsearch.reports.invoice;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.SearchManager;
import ro.cst.tsearch.data.User;
import ro.cst.tsearch.database.DBInvoice;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.exceptions.UpdateDBException;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.threads.ASMaster;
import ro.cst.tsearch.threads.ASThread;
import ro.cst.tsearch.threads.GPMaster;
import ro.cst.tsearch.threads.GPThread;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.RequestParamsValues;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.URLMaping;

public class UpdateInvoiceServlet extends BaseServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void doRequest(HttpServletRequest request, HttpServletResponse response) 
				throws ServletException, IOException
	{

		//loading parameters from request
		String operation = request.getParameter( RequestParams.INVOICE_LIST_OPERATION );
        if( operation == null )
        {
            operation = "";
        }
        
		String listChk = request.getParameter( RequestParams.INVOICE_LIST_CHK );
		String listUnChk = request.getParameter( RequestParams.INVOICE_LIST_UNCHK );
        boolean isTSAdmin = false;
        boolean isCommAdmin = false;
        try
        {
        	UserAttributes ua=((User) request.getSession().getAttribute(SessionParams.CURRENT_USER)).getUserAttributes();
            isTSAdmin = UserUtils.isTSAdmin( ua);
            isCommAdmin = UserUtils.isCommAdmin(ua)|| UserUtils.isTSCAdmin( ua );
        }catch( Exception e ){}
		//executing the required operation
		if (!"".equals( operation )){
			if (operation.equals(RequestParamsValues.INVOICE_SET_PAID)){
				if(isTSAdmin)
					DBManager.updateSearchesInvoiceStatus(listChk, "paid", 1);
				else if(isCommAdmin)
					DBManager.updateSearchesInvoiceStatus(listChk, "paid_cadm", 1);
				//DBManager.updateSearchesInvoiceStatus(listUnChk, "paid", 0);
			}
			if (operation.equals(RequestParamsValues.INVOICE_SET_UNPAID)){
				if(isTSAdmin)
					DBManager.updateSearchesInvoiceStatus(listChk, "paid", 0);
				else if(isCommAdmin)
					DBManager.updateSearchesInvoiceStatus(listChk, "paid_cadm", 0);
			}
			if (operation.equals(RequestParamsValues.INVOICE_TO_INVOICE)){
				if(isTSAdmin){
					DBManager.updateSearchesInvoiceStatus(listChk, "invoice", 1);
					DBManager.updateSearchesInvoiceStatus(listUnChk, "invoice", 0);
				} else if(isCommAdmin){
					DBManager.updateSearchesInvoiceStatus(listChk, "invoice_cadm", 1);
					DBManager.updateSearchesInvoiceStatus(listUnChk, "invoice_cadm", 0);
				}
			}
			if (operation.equals(RequestParamsValues.INVOICE_SET_RECEIVED)){
				if(isTSAdmin)
					DBManager.updateSearchesInvoiceStatus(listChk, "received", 1);
				else if(isCommAdmin)
					DBManager.updateSearchesInvoiceStatus(listChk, "received_cadm", 1);
				//DBManager.updateSearchesInvoiceStatus(listUnChk, "received", 0);
			}
			if (operation.equals(RequestParamsValues.INVOICE_CLEAR_RECEIVED)){
				if(isTSAdmin)
					DBManager.updateSearchesInvoiceStatus(listChk, "received", 0);
				else if(isCommAdmin)
					DBManager.updateSearchesInvoiceStatus(listChk, "received_cadm", 0);
			}
			if (operation.equals(RequestParamsValues.INVOICE_CLEAR_INVOICED)){
				if(isTSAdmin) {
					DBInvoice.clearInvoiceFor(listChk, "invoiced");
					//DBManager.updateSearchesInvoiceStatus(listChk, "invoiced", 0);
				}
				else {
					DBInvoice.clearInvoiceFor(listChk, "invoiced_cadm");
					//DBManager.updateSearchesInvoiceStatus(listChk, "invoiced_cadm", 0);
				}
			}
			if (operation.equals(RequestParamsValues.INVOICE_DELETE_SEARCHES)){
				User user=(User)request.getSession().getAttribute(SessionParams.CURRENT_USER);
				UserAttributes ua = user.getUserAttributes();
				if (!ua.isTSAdmin()) {
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
					        
					        errorMessage = "The following searches cannot deleted because they are in use.\\n" + errorMessage;
					        request.setAttribute("error", errorMessage);
					    }
				}
			    else {
			        try {
                        DBManager.deleteSearch(listChk, ua);
                    } catch (Exception e) {
                    	logger.error("Error trying to delete searches", e);
                    } 
			    }
			}
		}
		
        forward(request, response, request.getParameter( RequestParams.INVOICE_PAGE_NAME ).substring(URLMaping.path.length()));
	}
}
