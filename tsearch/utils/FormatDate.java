/*
 * Text here
 */

package ro.cst.tsearch.utils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 *
 */
public class FormatDate {

    /**
     *
     */
    public static final String DISC_FORMAT_1;
    public static final String DISC_FORMAT_2;
    public static final String DISC_FORMAT_1_DATE;
    public static final String DISC_FORMAT_1_TIME;
    public static final String LASTLOGIN_FORMAT;
    /**
     *
     */
    public static final String PRJ_FORMAT_1;

    /**
     *
     */
    public static final String PRJ_FORMAT_DATE_TIME;

    /**
     *
     */
    public static final String TR_FORMAT_1;

    /**
     *
     */
    public static final String PRJ_FORMAT_2;

    /**
     *
     */
    public static final String MONTH_TEXT;

    /**
     *
     */
    public static final String MONTH_NUMBER;

    /**
     *
     */
    public static final String DAY_OF_MONTH;

    /**
     *
     */
    public static final String DAY_OF_WEEK;

    /**
     *
     */
    public static final String YEAR;

    /**
     *
     */
    public static final String MONTH_YEAR;

    /**
     *
     */
    public static final String CPL_FORMAT_1;
    /**
     *
     */
    public static final String TN_TIME_FORMAT;
    /**
     *
     */
    public static final String EVA_BOTTOM_LABELS;
    public static final String DAY_YEAR;
    public static final String BUDGET_FORMAT;
	public static final String TIMESTAMP;
	public static final String SAVE_SEARCH_DISK_FORMAT;
	
	public static final String PATTERN_MMddyyyy_HHmmss = "MMddyyyy_HHmmss";
	public static final String PATTERN_MM_SLASH_DD_SLASH_YYYY = "MM/dd/yyyy";
	public static final String PATTERN_yyyy_MM_dd;
	public static final String PATTERN_yyyyMMdd;
	public static final String PATTERN_yyyyMMddDash;
	public static final String PATTERN_MMddyyyy;
	public static final String PATTERN_MMddyy;
	public static final String PATTERN_MMMddcyyyy;
	public static final String PATTERN_yy;
	public static final String PATTERN_yyyy;
	public static final String PATTERN_MM;
	public static final String PATTERN_DD;
	public static final String PATTERN_MM_MINUS_dd_MINUS_yyyy = "MM-dd-yyyy";
	public static final String PATTERN_MM_SLASH_DD_SLASH_YYYY_SPACE_HH_COLON_mm_COLON_ss = "MM/dd/yyyy HH:mm:ss";
	
	public static final long MILLIS_IN_DAYS = 24 * 60 * 60 * 1000;
	
    static {

        TN_TIME_FORMAT          = new String ("MMM, dd yyyy HH:mm");
        DISC_FORMAT_1           = new String ("MMM, dd yyyy HH:mm:ss");
        DISC_FORMAT_2           = new String ("MMM, dd yyyy HH:mm:ss z");
        DISC_FORMAT_1_DATE      = new String ("MMM, dd yyyy");
        DISC_FORMAT_1_TIME      = new String ("HH:mm:ss");
        LASTLOGIN_FORMAT        = new String ("MM/dd/yy HH:mm");
        TR_FORMAT_1             = new String ("MM/dd");
        CPL_FORMAT_1            = new String ("MMMM, dd yyyy");
        //PRJ_FORMAT_1 = new String ("MM/dd/yyyy");
        PRJ_FORMAT_1            = new String ("MMM dd, yyyy");
        PRJ_FORMAT_2            = new String ("HH:mm");
        PRJ_FORMAT_DATE_TIME    = new String ("MMM dd, yyyy - HH:mm");
        DAY_OF_MONTH            = new String ("dd");
        DAY_OF_WEEK             = new String ("EEE");
        MONTH_TEXT              = new String ("MMM");
        MONTH_NUMBER            = new String ("MM");
        MONTH_YEAR              = new String ("MMM yyyy");
        YEAR                    = new String ("yyyy");
        EVA_BOTTOM_LABELS       = new String ("MMM,dd,yy");
        DAY_YEAR                = new String ("dd,yy");
        BUDGET_FORMAT           = DAY_OF_WEEK + ", " +new String ("MMM dd, yyyy");
        TIMESTAMP				= new String ("dd-MM-yyyy HH:mm:ss");
        SAVE_SEARCH_DISK_FORMAT	= "yyyy_MM_dd";
        PATTERN_yyyy_MM_dd = SAVE_SEARCH_DISK_FORMAT;
        PATTERN_yyyyMMdd = "yyyyMMdd";
        PATTERN_yyyyMMddDash = "yyyy-MM-dd";
        PATTERN_MMddyyyy = "MMddyyyy";
        PATTERN_MMddyy = "MMddyy";
        PATTERN_yy = "yy";
        PATTERN_yyyy = "yyyy";
        
        PATTERN_MM = "MM";
        PATTERN_DD = "dd";
        PATTERN_MMMddcyyyy = PRJ_FORMAT_1;
        
    }
    
    public static String translateToMysql(String javaFormat){
    	String rez = javaFormat;
    	rez = rez.replace("mm",	"%i");
    	rez = rez.replace("HH", "%H");
    	rez = rez.replace("ss", "%S");
    	rez = rez.replace("dd", "%d");
    	rez = rez.replace("DD", "%d");
    	rez = rez.replace("YYYY", "%Y");
    	rez = rez.replace("yyyy", "%Y");
    	rez = rez.replace("MM", "%m");
    	return rez;
    }

     /**
      *
      */
     private String formatString;

    /** Inits this instance with PRJ_FORMAT_1 as default format pattern*/
    public FormatDate(){
        this(PRJ_FORMAT_1);
    }
    /** Inits this instance with a specified pattern*/
    public FormatDate(String formatString){
        this.formatString = formatString;
    }
    /** Formats a date from a Calendar instance*/
    public String getDate(Calendar calendar){
        return getDate(calendar.getTime().getTime());
    }
    /** Formats the date from a Date instance*/
    public String getDate(Date date){
        long time = date.getTime();
        return (time == 0) ? "N/A" : getDate(time);
    }
    /** Formats the date form a long value representing the time in millis*/
    public String getDate(long dateInMillis){
        /*if (dateInMillis == 0)
            return "N/A";*/
        //Calendar crtTime = Calendar.getInstance();
        //Date date = FormatDate.getDateInstance(dateInMillis 
        //        - crtTime.get(Calendar.ZONE_OFFSET)
        //        - crtTime.get(Calendar.DST_OFFSET));
        //Date date = FormatDate.getDateInstance(dateInMillis);
        //SimpleDateFormat formatter = new SimpleDateFormat (formatString, Locale.getDefault());
        
        //formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
//        Log.debug ("date =" + formatter.getTimeZone());
//        Log.debug ("date =" + date.getTime());
        //String sDate = formatter.format(date);
 //       Log.debug("sdate=" + sDate);
        return getDate(dateInMillis, TimeZone.getTimeZone("GMT"));
    }
    
    public String getDate(long dateInMillis, TimeZone tz){
    	Date date = FormatDate.getDateInstance(dateInMillis);
    	SimpleDateFormat formatter = new SimpleDateFormat (formatString, Locale.getDefault());
    	formatter.setTimeZone(tz);
    	String sDate = formatter.format(date);
    	return sDate;
    }
    /** Formats a date from a Calendar instance*/
    public String getLocalDate(Calendar calendar){
        return getLocalDate(calendar.getTime().getTime());
    }
    /** Formats the date from a Date instance*/
    public String getLocalDate(Date date){
        long time = date.getTime();
        return (time == 0) ? "N/A" : getLocalDate(time);
    }
    public String getLocalDate(long dateInMillis){
        if (dateInMillis == 0)
            return "N/A";
        //Calendar crtTime = Calendar.getInstance();
        //Date date = FormatDate.getDateInstance(dateInMillis 
        //        - crtTime.get(Calendar.ZONE_OFFSET)
        //        - crtTime.get(Calendar.DST_OFFSET));
        Date date = FormatDate.getDateInstance(dateInMillis);
        SimpleDateFormat formatter = new SimpleDateFormat (formatString, Locale.getDefault());
        
        //formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        // Log.debug ("date =" + formatter.getTimeZone());
        // Log.debug ("date =" + date.getTime());
        String sDate = formatter.format(date);
        //Log.debug("sdate=" + sDate);
        return sDate;
    }

    /*...*/
    public static long currentTimeMillis() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        return (cal.getTime().getTime());
        //return (cal.getTime().getTime() - cal.getTimeZone().getRawOffset());
    }

    public static Date getDateInstance(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        return calendar.getTime();
    }

    public static Date getDateInstance(long time) {
        // the cal has the local time zone 
        /*Calendar cal = Calendar.getInstance();
        // usually the time param is grab from the db, and is a GMT related time
        // so setTime called over the cal instance is setting the calendar
        // with the GMT time +/- the current time_zone offset
        cal.setTime(new Date(time));
        // adding the rowOffset value will set the cal object to the time value
        // as thi is interpreted within the DB, meaning with no
        // time_zone offset
        cal.add(Calendar.MILLISECOND, cal.getTimeZone().getRawOffset());
        return cal.getTime();*/
        Date dateTime = Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime();
        dateTime.setTime(time);
        return dateTime;
    }

    public static Date getDateInstance() {
        return Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime();
    }

    /**
     * Gets a calendar intance in GMT settings
     */
    public static Calendar getCalendarInstance(){
        return Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    }

    /**
     * Gets a calendar instance in GMT settings and setting the time to the given limit
     */
    public static Calendar getCalendarInstance(long time){
        Calendar calendar = getCalendarInstance();
        calendar.setTime(new Date(time));
        return calendar;
    }
    
	/** Formats the date from a string value representing the time and its format*/
	public static Date getDateFromFormatedString(String date, String format){

		Date date1 = new Date(0);// = new Date(date);
		SimpleDateFormat formatter = new SimpleDateFormat (format);
		
		try {
			date1 = formatter.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date1;
	}
    
	public static Date getDateFromFormatedStringGMT(String dateGigi, String format){

		Date date1 = new Date(0);// = new Date(date);
		SimpleDateFormat formatter = new SimpleDateFormat (format);
		formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        
		try {
			date1 = formatter.parse(dateGigi);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date1;
	}
	
	/**
	 * Ignores the given time-zone of the received date and forces it to GMT returning the formated value 
	 * @param gmtDate
	 * @return
	 */
	public static Date getDateFromGMTDate(Date gmtDate) {
		SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
	    String formaterDate = isoFormat.format(gmtDate) + " UTC";
	    SimpleDateFormat iso2Format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S z");
		
		try {
			return iso2Format.parse(formaterDate);
		} catch (ParseException e) {
			return null;
		}
	}
	
	/** 
	 * Formats the date from a string value representing the time and its format
	 */
	public static Calendar getCalendarFromFormattedString(String date, String format){
		try {
			Calendar cal = Calendar.getInstance();
			cal.setTime((new SimpleDateFormat(format)).parse(date));
			return cal;
		} catch (ParseException e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args) {
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yy");
		
		System.out.println(sdf.format(Calendar.getInstance().getTime()));
	}
	
	public static SimpleDateFormat getDateFormat(String pattern) {
		return new SimpleDateFormat(pattern);
	}
}
