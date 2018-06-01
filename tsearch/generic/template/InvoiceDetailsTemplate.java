package ro.cst.tsearch.generic.template;

import java.util.*;

import ro.cst.tsearch.reports.data.DayReportLineData;
import ro.cst.tsearch.utils.RequestParams;
import ro.cst.tsearch.utils.StringUtils;


public class InvoiceDetailsTemplate extends BaseTemplate implements RequestParams {    
    
	DayReportLineData data = null;
	                   
    /**
     *  get replacements 
     */   
    public Hashtable getReplacements() {

        Hashtable ht = super.getReplacements();
        if (data==null) {
            return ht;
        }
        
		try {
			ht.put(RequestParams.SEARCH_ABS_NAME, 		data.getAbstractorName());
			ht.put(RequestParams.SEARCH_ABS_COLUMN, 	data.getAbstractorColumn());
			ht.put(RequestParams.SEARCH_AGN_NAME, 		data.getAgentName());
			ht.put(RequestParams.SEARCH_OWN_NAME, 		data.getOwnerName());
			ht.put(RequestParams.SEARCH_PROP_ADDRESS,	data.getPropertyAddress());
			ht.put(RequestParams.SEARCH_COUNTY_NAME, 	data.getPropertyFullCounty());
			ht.put(RequestParams.SEARCH_HOUR, 			data.getTSRDateHour());
//			ht.put(RequestParams.SEARCH_TIMEZONE,	 	data.getTimeZone());
			ht.put(RequestParams.SEARCH_FILE_ID, 		StringUtils.truncString(data.getFileId(),30));
			ht.put(RequestParams.SEARCH_SENT_TO, 		data.getSendTo());
            ht.put( RequestParams.SEARCH_PRODUCT_TYPE, data.getProductType() );
            ht.put( RequestParams.SEARCH_PRODUCT_NAME, data.getProductName() );
            ht.put( RequestParams.SEARCH_FEE, data.getFee() );
            ht.put( "imageCount", data.getImageCount() );
            ht.put( "requestCount", data.getRequestCountDescription() );
            ht.put( "dataSource", data.getDataSource() );
		} catch (Exception e) {
			e.printStackTrace();
		}                            
        
        return ht;
    }

	public DayReportLineData getData() {
		return data;
	}

	public void setData(DayReportLineData data) {
		this.data = data;
	}

}        
