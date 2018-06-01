package ro.cst.tsearch.settings;

import java.util.Calendar;

import org.apache.log4j.Category;

import ro.cst.tsearch.data.Payrate;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.generic.tag.LoopTag;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.RequestParams;

public class PayratesLoopTag extends LoopTag{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected static final Category logger= Category.getInstance(PayratesLoopTag.class.getName());
	//state methods
	private long reportState = -1;//2 // default state All States 
	public void setReportState(String stateReport) {
		this.reportState = Long.parseLong(stateReport);
	}
	public String getReportState() {
		return String.valueOf(reportState);
	}
	//county methods
	private long countyId = 0;
	public void setCountyId(String countyId) {
		this.countyId = Long.parseLong(countyId);
	}
	public String getCountyId() {
		return String.valueOf(countyId);
	}

//	countyX methods
	  private long countyIdX = 0;
	  public void setCountyIDXX(String countyId) {
		  this.countyIdX = Long.parseLong(countyId);
	  }
	  public String getCountyIDXX() {
		  return String.valueOf(countyIdX);
	  }

	private String settingsOp = "";

	public String getSettingsOp() {
		return settingsOp;
	}

	public void setSettingsOp(String string) {
		settingsOp = string;
	}

	protected Object[] createObjectList() throws Exception {

		Payrate[] Payrates = new Payrate[0];
		if (logger.isDebugEnabled())
			logger.debug("Debug PayratesLoopTag#createObjectList Start ...");

		//setting the community and request parameters
		int commId = -1;
		String strCommId = (String)ses.getAttribute("commId");
		
		commId = Integer.parseInt(strCommId);
		Calendar cal = Calendar.getInstance();
		loadAttribute( RequestParams.REPORTS_STATE );
		loadAttribute( RequestParams.PAYRATE_COUNTY_ID );
		loadAttribute( RequestParams.PAYRATE_COUNTY_IDX );
		loadAttribute( RequestParams.SETTINGS_OPERATION );
		
		if (logger.isDebugEnabled())
			logger.debug("Debug:PayratesLoopTag params : " + commId + "/ " + reportState + "/ " + countyId);		
		//getting the appropriate payrate
		if (countyId < 1 && countyIdX < 1)
			Payrates = DBManager.getCurrentPayratesForCommunityAndState(commId, reportState, cal.getTime());
		else if ((countyId >= 1 && countyIdX < 1))
			Payrates = DBManager.getPayrateHistoryForCounty(commId, countyId);
		else 
			Payrates = DBManager.getPayrateHistoryForCity(commId, reportState, countyIdX);
		if (logger.isDebugEnabled())
			logger.debug("Debug:PayratesLoopTag End.");
		return Payrates;
	}

}
