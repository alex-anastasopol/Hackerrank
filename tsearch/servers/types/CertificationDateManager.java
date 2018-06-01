package ro.cst.tsearch.servers.types;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.utils.FormatDate;

//rename from ro.cst.tsearch.servers.types.certificationDate
public class CertificationDateManager {
	
    protected static final Logger logger = Logger.getLogger(CertificationDateManager.class);

    static final long serialVersionUID = 10000000;

	private static Hashtable<String, CachedDate> CachedDates = new Hashtable<String, CachedDate>();

	static SimpleDateFormat sdfIn = new SimpleDateFormat("MMM dd, yyyy");

	static SimpleDateFormat sdfOut = new SimpleDateFormat("MM/dd/yyyy");

    public static final int DEFAULT_CERTIFICATION_DATE_OFFSET = 7;
    
    public static final int CERTIFICATION_DATE_OFFSET_FOR_EMPTY_INPUT = -100;
    
    protected ArrayList<CertificationDateDS> certificationDates = null;
    
    /**
     * Log here specific certification date issues (they are recorded separately)
     * @return the logger
     */
    public static Category getLogger() {
		return logger;
	}

    public CertificationDateManager(ArrayList<CertificationDateDS> certificationDates){
    	
    	if (certificationDates == null){
    		certificationDates = new ArrayList<CertificationDateDS>();
		}
    	this.certificationDates = certificationDates;
    }
    
    public boolean addCertificationDate(CertificationDateDS certificationDate){
    	return certificationDates.add(certificationDate);
    }
    
    /**
     * Check if we already have the certification date for today.
     * @param dataSite
     * @return
     */
    public static boolean isCertificationDateInCache(DataSite dataSite) {
    	
    	String key = dataSite.getSiteType() + "_" + dataSite.getCountyIdAsString();
    	CachedDate d = (CachedDate) CachedDates.get(key);
        boolean isInCache = false;
        
        if (d != null){
//        	Calendar dateNow = Calendar.getInstance();
//	        long diff = (dateNow.getTimeInMillis() - d.getTstamp().getTimeInMillis()) / (1000 * 60 * 60 * 24);
//	    	if (diff == 0){
//	        	isInCache = true;
//	    	}
        	
        	if (d.getTstamp().get(Calendar.DAY_OF_MONTH) == Calendar .getInstance().get(Calendar.DAY_OF_MONTH)){
        		isInCache = true;
        	}
        } else{
        	isInCache = false;
        }
        return isInCache;
    }
    
    public static String getCertificationDateFromCache(DataSite dataSite) {
    	
    	String key = dataSite.getSiteType() + "_" + dataSite.getCountyIdAsString();
    	
    	CachedDate d = (CachedDate) CachedDates.get(key);
        if (d != null && d.getTstamp().get(Calendar.DAY_OF_MONTH) == Calendar .getInstance().get(Calendar.DAY_OF_MONTH)) {
        	
        	try {
				return d.getValue();
			} catch (Exception e) {
				logger.error("Cannot parse date " + d.getValue() + " for county id " + dataSite.getCountyIdAsString(), e);
			}
        	
        }
        return null;
    }
    
    /**
     * Certification date must be in format MM/dd/yyyy or an exception will be thrown
     * @param dataSite
     * @param date
     * @throws ParseException 
     */
    public static void cacheCertificationDate(DataSite dataSite , String date) throws ParseException {
    	
    	new SimpleDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY).parse(date);
    	
    	String key = dataSite.getSiteType() + "_" + dataSite.getCountyIdAsString();
    	
    	CachedDate cd = new CachedDate(date, Calendar.getInstance());

    	if(logger.isDebugEnabled()) {
	    	String toLog = "Caching certification date for county " + dataSite.getCountyIdAsString() + " with value " + date;
	    	CachedDate oldDate = CachedDates.get(key);
	    	if(oldDate != null) {
	    		toLog += " (old value " + oldDate.getValue() + ") on " + cd.getTstamp().getTime();
	    	} else {
	    		toLog += " (old value null) on " + cd.getTstamp().getTime();
	    	}
	    	logger.debug(toLog);
    	}
    	
        CachedDates.put(key, cd);
    }
    
    /**
     * 
     * @param siteType
     * @return
     */
    public boolean hasCertificationDateForSite(int siteType){
    	
    	for (CertificationDateDS cdObject : certificationDates){
    		if (siteType == cdObject.getSiteType()){
    			return true;
    		}
    	}
    	return false;
    }
    
    public boolean isEmpty() {
    	return certificationDates.isEmpty();
    }
    
    /**
     * Checks if at least one usable date exists<br>
     * Usable means a date with skipInCalculation flag set to false
     * @return
     */
    public boolean hasUsableDates() {
    	if(certificationDates.isEmpty()) {
    		return false;
    	}
    	for (CertificationDateDS certificationDateDS : certificationDates) {
			if(!certificationDateDS.isSkipInCalculation()) {
				return true;
			}
		}
    	return false;
    }
    
    public CertificationDateDS getCertificationDateBySiteAndType(int site, CertificationDateDS.CDType type){
    	
    	for (CertificationDateDS cdObject : certificationDates){
    		if (site == cdObject.getSiteType() && type.equals(cdObject.getType())){
    			return cdObject;
    		}
    	}
    	return null;
    }

	public String toHtml() {
		
		if(certificationDates.isEmpty()) {
			return "";
		}
		
		StringBuilder html = new StringBuilder();
		for (CertificationDateDS cd : certificationDates) {
			if(html.length() > 0) {
				html.append("<br/>");
			} else {
				html.append("<div id=\"certificationDates\">Certification Dates:<br/>");
			}
			html.append(cd.toHtml());
		}
		html.append("</div>");
		return html.toString();
	}

	public boolean updateCertificationDate(CertificationDateDS newDate, SearchAttributes searchAttributes) {
		int siteType = newDate.getSiteType();
		CertificationDateDS certDate = getCertificationDateBySiteAndType(siteType, newDate.getType());
		if (certDate == null){
			return addCertificationDate(newDate);
		} else{
			if (!searchAttributes.isDateDown()){
				if(newDate.getCertificationDateDS().after(certDate.getCertificationDateDS())) {
					newDate.setCertificationDateDS(certDate.getCertificationDateDS());;
					return true;
				}
			}
		}
		
		return false;
	}

	public CertificationDateDS getSearchCertificationDate() {
		List<CertificationDateDS> validDates = new ArrayList<>();
		for (CertificationDateDS certificationDateDS : certificationDates) {
			if(!certificationDateDS.isSkipInCalculation()) {
				validDates.add(certificationDateDS);
			}
		}
		Collections.sort(validDates, new Comparator<CertificationDateDS>() {
			@Override
			public int compare(CertificationDateDS cd1, CertificationDateDS cd2) {
				return cd2.getCertificationDateDS().compareTo(cd1.getCertificationDateDS());
			}
		});
		
		if(!validDates.isEmpty()) {
			return validDates.get(0);
		}
		return null;
		
	}

}