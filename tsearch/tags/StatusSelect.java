package ro.cst.tsearch.tags;

import java.util.HashMap;
import java.util.Map;

import ro.cst.tsearch.data.InvoiceSearchStatus;
import ro.cst.tsearch.generic.Util;
import ro.cst.tsearch.generic.tag.SelectTag;
import ro.cst.tsearch.utils.RequestParams;

public class StatusSelect extends SelectTag {

	private static final long serialVersionUID = 1L;
	
	public static final int STATUS_D_AND_NOT_T = 17;
	public static final int STATUS_STARTER = 18;
	public static final int STATUS_NOT_STARTER = 19;
	public static final int STATUS_FVS = 22;

	private static final InvoiceSearchStatus allStatuses[] = { 
		new InvoiceSearchStatus(0, "Due"),
		new InvoiceSearchStatus(1, "Paid"),
		new InvoiceSearchStatus(2, "Not Invoiced"),
		new InvoiceSearchStatus(3, "Invoiced"),
		new InvoiceSearchStatus(6, "Not Confirmed"),
  		new InvoiceSearchStatus(7, "Confirmed"),
		new InvoiceSearchStatus(8, "Not Archived"),
  		new InvoiceSearchStatus(9, "Archived"),
		new InvoiceSearchStatus(10, "N"),
		new InvoiceSearchStatus(11, "T"),
		new InvoiceSearchStatus(12, "K"),
		new InvoiceSearchStatus(13, "O"), 
		new InvoiceSearchStatus(21, "E"),
		new InvoiceSearchStatus(STATUS_FVS, "F"),
		new InvoiceSearchStatus(14, "(N OR T) AND NOT O"),
		new InvoiceSearchStatus(20, "(N OR T) AND NOT (K OR E)"),
		new InvoiceSearchStatus(15, "(D OR K) AND NOT I"),
		new InvoiceSearchStatus(STATUS_D_AND_NOT_T, "D AND NOT T"),
		new InvoiceSearchStatus(STATUS_STARTER, "Base File"),
		new InvoiceSearchStatus(STATUS_NOT_STARTER, "Not Base File")
	};

	private int[] reportStatus = {-1};	
	public void setReportStatus(String s) {
		reportStatus = Util.extractArrayFromString(s);
	}	
	public String getReportStatus() {
		return Util.getStringFromArray(reportStatus);
	}

	/**
	 * @see ro.cst.tsearch.generic.tag.SelectTag#createOptions()
	 */
	protected String createOptions() throws Exception {

		loadAttribute(RequestParams.REPORTS_STATUS);

		StringBuffer sb = new StringBuffer(3000);		
		sb.append(allOption(reportStatus));
		for (int i = 0; i < allStatuses.length; i++) {
			sb.append(
				"<option "
					+ (Util.isValueInArray(allStatuses[i].getId(), reportStatus) ? "selected" : "")
					+ " value='"
					+ allStatuses[i].getId()
					+ "'>"
					+ allStatuses[i].getName()
					+ "</option>");
		}
		return sb.toString();
	}

	protected  String allOption(int[] id)	throws Exception {

		if(all) {
			return "<option "+(Util.isValueInArray(-1, id)?"selected":"")+" value='-1'>All Statuses</option>" ;
		} else {
			return "";
		}
	}
	public static InvoiceSearchStatus[] getAllStatuses() {
		return allStatuses;
	}
	public static Map<Integer, String> getAllStatusesMapById() {
		Map<Integer, String> result = new HashMap<Integer, String>();
		for (InvoiceSearchStatus status : allStatuses) {
			result.put(status.getId(), status.getName());
		}
		return result;
	}

}
