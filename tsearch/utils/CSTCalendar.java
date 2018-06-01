/*
  Text here
*/
package ro.cst.tsearch.utils;
import java.util.Calendar;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Arrays;


/**
 *
 *                                
 */
public class CSTCalendar
{
	/* in "calendarPopup.js"
	Calendar.Months = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
	Calendar.DOMonth  = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]; // Non-Leap year Month days..     
	Calendar.lDOMonth = [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]; // Leap year Month days..
	*/
	private static String msImagePath= URLMaping.IMAGES_DIR + "/";
	public static final String[] FULL_MONTHS= { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };
	public static final String[] MONTHS= { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
	/**
	 *
	 */
	private static final int CAL_COLS= 7;
	/*
	 *
	 */
	public static final String[] DAYSNAME= { new String("Sun"), new String("Mon"), new String("Tue"), new String("Wed"), new String("Thu"), new String("Fri"), new String("Sat")};
	private int date;
	/**
	 *
	 */
	private int month;
	/**
	 *
	 */
	private int year;
	/**
	 *
	 */
	private int CAL_ROWS;
	/**
	 *
	 */
	private Calendar calendar;
	/**
	 *
	 */
	public CSTCalendar()
	{
		calendar= Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		this.year= calendar.get(Calendar.YEAR);
		this.month= calendar.get(Calendar.MONTH);
		this.date= calendar.get(Calendar.DAY_OF_MONTH);
		;
	}
	/**
	 *
	 */
	public void setMonth(int month)
	{
		this.month= month;
	}
	/**
	 *
	 */
	public void setYear(int year)
	{
		this.year= year;
	}
	/**
	 *
	 */
	public void setDate(int date)
	{
		this.date= date;
	}
	/* in "calendarPopup.js"
	    var sepmoda = " ";
	    var sepy = ", ";
	    var sepdt = "/";
	    var sephm = ":";
	    var sepgmt1 = " (GMT"; var sepgmt2 = ")"; var sepgmt = "";
	*/
	private static final String sepmoda= " ";
	private static final String sepy= ", ";
	private static final String sepdt= "/";
	private static final String sephm= ":";
	private static final String sepgmt1= " (GMT";
	private static final String sepgmt2= ")";
	//      private static final String sepgmt = "";
	/**
	* a class that represents a month / day / year select control
	*/
	/**
	* @param type- "MDY" is default;
	* @param name - contol parameter( name of control in html page, it's a prefix )
	* @param initValues - current date
	* @param minValues - min date
	* @param maxValues - max date 	
	* @param formName - the form containing the date control 	
	*/
	public static String getDateControl(String type, String name, int[] initValues, int[] minValues, int[] maxValues, String formName)
	{
		if (name == null)
			name= new String("");
		if (type != null && type.length() > 3)
			type= "MDYHM";
		else
			type= "MDY";
		
		if (initValues == null)
		{
			if (type.equals(""))
			{
				initValues= getDefaultInitDate("MDYHM");
			}
			else
			{
				initValues= getDefaultInitDate("MDY");
			}
		}
		
		if (initValues[1] > 12) {
			initValues[1] = initValues[1] % 10;
		}
        
        if( initValues[1] < 0 )
        {
            initValues[1] = 0;
        }
      //[+] start of comment
      // Set date value empty when initValues is equal to a new created array 
      //     
        int[] newDate    = new int[3];
        String dateValue = "";
        
         if (!Arrays.equals(initValues, newDate))             
		   dateValue = MONTHS[initValues[1]] + sepmoda + Integer.toString(initValues[2]) + sepy + Integer.toString(initValues[0]);
	  //	   
      //[-] end of comment   
         
		String timeValue= "";
		String thegmt= "0";
		
		if (initValues.length > 3)
		{
			int thehour = initValues[3];
			int themin= initValues[4];
			timeValue = sepdt
				  + ((Integer.toString(thehour).length()==1) ? ("0"+Integer.toString(thehour)) : Integer.toString(thehour))
				  + sephm
				  + ((Integer.toString(themin).length()==1) ? ("0"+Integer.toString(themin)) : Integer.toString(themin))
				  + sepgmt1 + thegmt + sepgmt2;
		}
		String minValuesAsJSparams;
		String maxValuesAsJSparams;
		if (minValues == null) {
			minValues = new int[3];
			minValues[0] = -1;
			minValues[1] = -1;
			minValues[2] = -1;
		}
		minValuesAsJSparams    = "" + minValues[2] + "," + minValues[1] + "," + minValues[0] + "";
		if (maxValues == null) {
			maxValues = new int[3];
			maxValues[0] = -1;
			maxValues[1] = -1;
			maxValues[2] = -1;
		}
		maxValuesAsJSparams    = "" + maxValues[2] + "," + maxValues[1] + "," + maxValues[0] + "";
		String dateInput=
			"<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr>"
				+ "<td><input name=\""
				+ name
				+ "\""
				
				+" id ="
				
				+ "\""
				+ name
				+ "\""
				
				+ " value=\""
				+ dateValue
				+ "\" type=\"text\" onChange=\"validateDateControl(this)\" onFocus=\"blurDateControl(this)\" size=\"6\" class=\"datecontrol\"></td>"
				+ "<td><a href=\"#\" onClick=\"javascript:doMonthlyCalendar('"
				+ formName
				+ "."
				+ name
				+ "',"
				+ minValuesAsJSparams + ","
				+ maxValuesAsJSparams
				+ ");\" onMouseOver=\"window.status='Monthly Calendar';return true;\" onMouseOut=\"window.status='';return true;\"><img src=\""
				+ msImagePath
				+ "calendarp.gif\" alt=\"Select Date\" border=\"0\" align=\"absmiddle\"></a></td>"
				+ "</tr></table>\n";
		String dateTimeInput=
			"<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr>"
				+ "<td><input name=\""
				+ name
				+ "\" value=\""
				+ dateValue
				+ timeValue
				+ "\" type=\"text\" onChange=\"validateDateControl(this)\" onFocus=\"blurDateControl(this)\" size=\"15\" class=\"datetimecontrol\"></td>"
				+ "<td><a href=\"#\" onClick=\"javascript:doMonthlyTimeCalendar('"
				+ formName
				+ "."
				+ name
				+ "', '"
				+ thegmt
				+ "',"
				+ minValuesAsJSparams 
				+ ","
				+ maxValuesAsJSparams
				+ ");\" onMouseOver=\"window.status='Monthly Calendar';return true;\" onMouseOut=\"window.status='';return true;\"><img src=\""
				+ msImagePath
				+ "calendarp.gif\" alt=\"Select Date & Time\" border=\"0\" align=\"absmiddle\"></a></td>"
				+ "</tr></table>";
		return (type.equals("MDYHM")) ? dateTimeInput : dateInput;   
	}
	
	
	/**
	* @param type- "MDY" is default;
	* @param name - contol parameter( name of control in html page, it's a prefix )
	* @param initValues - current date
	* @param minValues - min date
	* @param maxValues - max date 	
	* @param formName - the form containing the date control 	
	*/
	public static String getCalendarLink(String type, String name, int[] initValues, int[] minValues, int[] maxValues, String formName)
	{
		if (name == null)
			name= new String("");
		if (type != null && type.length() > 3)
			type= "MDYHM";
		else
			type= "MDY";
		
		if (initValues == null)
		{
			if (type.equals(""))
			{
				initValues= getDefaultInitDate("MDYHM");
			}
			else
			{
				initValues= getDefaultInitDate("MDY");
			}
		}
		
		if (initValues[1] > 12) {
			initValues[1] = initValues[1] % 10;
		}
        
        if( initValues[1] < 0 )
        {
            initValues[1] = 0;
        }
      //[+] start of comment
      // Set date value empty when initValues is equal to a new created array 
      //     
        int[] newDate    = new int[3];
        String dateValue = "";
        
         if (!Arrays.equals(initValues, newDate))             
		   dateValue = MONTHS[initValues[1]] + sepmoda + Integer.toString(initValues[2]) + sepy + Integer.toString(initValues[0]);
	  //	   
      //[-] end of comment   
         
		String timeValue= "";
		String thegmt= "0";
		
		if (initValues.length > 3)
		{
			int thehour = initValues[3];
			int themin= initValues[4];
			timeValue = sepdt
				  + ((Integer.toString(thehour).length()==1) ? ("0"+Integer.toString(thehour)) : Integer.toString(thehour))
				  + sephm
				  + ((Integer.toString(themin).length()==1) ? ("0"+Integer.toString(themin)) : Integer.toString(themin))
				  + sepgmt1 + thegmt + sepgmt2;
		}
		String minValuesAsJSparams;
		String maxValuesAsJSparams;
		if (minValues == null) {
			minValues = new int[3];
			minValues[0] = -1;
			minValues[1] = -1;
			minValues[2] = -1;
		}
		minValuesAsJSparams    = "" + minValues[2] + "," + minValues[1] + "," + minValues[0] + "";
		if (maxValues == null) {
			maxValues = new int[3];
			maxValues[0] = -1;
			maxValues[1] = -1;
			maxValues[2] = -1;
		}
		maxValuesAsJSparams    = "" + maxValues[2] + "," + maxValues[1] + "," + maxValues[0] + "";
		String dateInput=
				"<a href=\"#\" onClick=\"javascript:doMonthlyCalendar('"
				+ formName
				+ "."
				+ name
				+ "',"
				+ minValuesAsJSparams + ","
				+ maxValuesAsJSparams
				+ ");\" onMouseOver=\"window.status='Monthly Calendar';return true;\" onMouseOut=\"window.status='';return true;\"><img src=\""
				+ msImagePath
				+ "calendarp.gif\" alt=\"Select Date\" border=\"0\" align=\"absmiddle\"></a>"
				+ "\n";
		return dateInput;
	}	
	/**
	* return default init date as current date
	*/
	public static int[] getDefaultInitDate(String type)
	{
		Calendar c= Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		int[] initValues;
		if (type.equals("MDYHM"))
		{
			initValues= new int[5];
			initValues[0]= c.get(Calendar.YEAR);
			initValues[1]= c.get(Calendar.MONTH);
			initValues[2]= c.get(Calendar.DAY_OF_MONTH);
			initValues[3]= c.get(Calendar.HOUR_OF_DAY);
			initValues[4]= c.get(Calendar.MINUTE);
		}
		else
		{
			initValues= new int[3];
			initValues[0]= c.get(Calendar.YEAR);
			initValues[1]= c.get(Calendar.MONTH);
			initValues[2]= c.get(Calendar.DAY_OF_MONTH);
		}
		return initValues;
	}
	
	public static int[] getDefaultInitDate(Calendar c,  String type)
	{		
		int[] initValues;
		if (type.equals("MDYHM"))
		{
			initValues= new int[5];
			initValues[0]= c.get(Calendar.YEAR);
			initValues[1]= c.get(Calendar.MONTH);
			initValues[2]= c.get(Calendar.DAY_OF_MONTH);
			initValues[3]= c.get(Calendar.HOUR_OF_DAY);
			initValues[4]= c.get(Calendar.MINUTE);
		}
		else
		{
			initValues= new int[3];
			initValues[0]= c.get(Calendar.YEAR);
			initValues[1]= c.get(Calendar.MONTH);
			initValues[2]= c.get(Calendar.DAY_OF_MONTH);
		}
		return initValues;
	}
	
	/**
	* return init date from a string
	*/
	
	public static int[] getInitDateFromString(String date,String type)
	{
		int[] initValues;
		if (type.equals("MDYHM"))
		{
			initValues= new int[5];
			initValues[0]= Integer.parseInt(getYearFromInput(date));
			initValues[1]= Integer.parseInt(getMonthFromInput(date));
			initValues[2]= Integer.parseInt(getDayFromInput(date));
			initValues[3]= 0;
			initValues[4]= 0;
		}
		else
		{
			initValues= new int[3];
			initValues[0]= Integer.parseInt(getYearFromInput(date));
			initValues[1]= Integer.parseInt(getMonthFromInput(date));
			initValues[2]= Integer.parseInt(getDayFromInput(date));
		}
		return initValues;
	}
	public static String getDateFromInt(int[] date,String type)
	{
		
		String rtrn = MONTHS[date[1]] + sepmoda + Integer.toString(date[2]) + sepy + Integer.toString(date[0]);
		if (type.equals("MDYHM"))
			rtrn+= sepdt + Integer.toString(date[3]) + sephm + Integer.toString(date[4]);
		return rtrn;
	}
	public static Calendar getCalendar(Calendar c1)
	{
		return getCalendar(
			c1.get(Calendar.MILLISECOND),
			c1.get(Calendar.SECOND),
			c1.get(Calendar.MINUTE),
			c1.get(Calendar.HOUR_OF_DAY),
			c1.get(Calendar.DAY_OF_MONTH),
			c1.get(Calendar.MONTH),
			c1.get(Calendar.YEAR),
			0);
	}
	///**************///
	public static Calendar getCalendar(int msec, int sec, int min, int hour, int day, int month, int year, int tz)
	{
		Calendar st= Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		st.set(Calendar.DAY_OF_MONTH, day);
		st.set(Calendar.MONTH, month);
		st.set(Calendar.YEAR, year);
		st.set(Calendar.HOUR_OF_DAY, hour);
		st.set(Calendar.MINUTE, min);
		st.set(Calendar.SECOND, sec);
		st.set(Calendar.MILLISECOND, msec);
		// in order to trigger the recomputation of the internal data of 
		// the calendar which have been set above
		st.get(Calendar.MILLISECOND);
		return st;
	}
	public static Calendar getCalendar(int day, int month, int year)
	{
		return getCalendar(0, 0, 0, 0, day, month, year, 0);
	}
	public static Calendar getCalendar()
	{
		Calendar c= Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		return c;
	}

	public static Calendar getCalendar(long timeInMilis)
	{
		Calendar c= Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		c.setTimeInMillis(timeInMilis);
		return c;
	}
	
	
	
	// DATE // MMM + sepmoda + DD  + sepy + YYYY
	// TIME //                                   + sepdt + HH + sephm + MM + tType + sepgmt1 + GMT + sepgmt2
	public static String getMonthFromInput(String inputValue)
	{
        SimpleDateFormat sdf = new SimpleDateFormat( "MM" );
        SimpleDateFormat sdfmmddyy = new SimpleDateFormat("MM/dd/yyyy");
        try {
        	Date parsed = sdfmmddyy.parse(inputValue);
        	return Integer.toString(parsed.getMonth()+1);
        } catch (Exception e1){
			if (!inputValue.equals("0"))
			{
				try
				{
				    int sepmodaIndex = inputValue.indexOf(sepmoda);
				    String theMonth = "0";
				    if( sepmodaIndex >= 0 )
				    {
				        theMonth= inputValue.substring(0, sepmodaIndex);
				    }
				    else
				    {
				        //if date is already in MMDDYY format
				        String monthNo = "01";
				        
				        if( inputValue.length() >= 2 )
				        {
				            monthNo = inputValue.substring(0, 2);
				        }
				        else
				        {
				            monthNo = sdf.format( new Date() );
				        }
				        
				        monthNo = Integer.toString( Integer.parseInt(monthNo) - 1 ); 
				        
				        return monthNo;
				    }
				    
					int i= 0;
					for (i= 0; i < MONTHS.length; i++)
						if (theMonth.equals(MONTHS[i]))
							break;
					return Integer.toString(i);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					return sdf.format( new Date() );
				}
			}
			else
				return sdf.format( new Date() );
        }
	}
	// DATE // MMM + sepmoda + DD  + sepy + YYYY
	// TIME //                                   + sepdt + HH + sephm + MM + tType + sepgmt1 + GMT + sepgmt2
	public static String getMonthNameFromInput(String inputValue)
	{
		if (!inputValue.equals("0"))
		{
			try
			{
				return  inputValue.substring(0, inputValue.indexOf(sepmoda));
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return "0";
			}
		}
		else
			return "0";
	}
	public static String getDayFromInput(String inputValue)
	{
        SimpleDateFormat sdf = new SimpleDateFormat( "dd" );
        SimpleDateFormat sdfmmddyy = new SimpleDateFormat("MM/dd/yyyy");
        try {
        	Date parsed = sdfmmddyy.parse(inputValue);
        	return Integer.toString(parsed.getDate());
        } catch (Exception e1){        
			if (!inputValue.equals("0"))
			{
				try
				{
				    String theDay = "0";
				    int sepmodaIndex = inputValue.indexOf(sepmoda);
				    int sepyIndex = inputValue.indexOf(sepy);
	
				    if( sepmodaIndex >= 0 && sepyIndex >= 0 )
				    {
				        theDay = inputValue.substring(sepmodaIndex + sepmoda.length(), sepyIndex);
				    }
				    else
				    {
				        //already in MMDDYY format
				        if( inputValue.length() >= 4  )
				        {
				            String newInputValue = inputValue.replaceAll( sepdt, "" );
				            String retVal = newInputValue.substring( 2, 4 );
				            Integer.parseInt( retVal );
				            return retVal;
				        }
				        else
				        {
				            theDay = sdf.format( new Date() );
				        }
				    }
				    
				    theDay = Integer.toString(Integer.parseInt(theDay)); 
				    
					return theDay;
				}
				catch (Exception e)
				{
					e.printStackTrace();
					return sdf.format( new Date() );
				}
			}
			else
				return sdf.format( new Date() );
        }
	}
	public static String getYearFromInput(String inputValue)
	{
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyy" );
        SimpleDateFormat sdfmmddyy = new SimpleDateFormat("MM/dd/yyyy");
        try {
        	Date parsed = sdfmmddyy.parse(inputValue);
        	return Integer.toString(parsed.getYear()+1900);
        } catch (Exception e1){
			if (!inputValue.equals("0"))
			{
				try
				{
					String theYear= "";
					if (inputValue.indexOf(sepdt) > 0 && inputValue.indexOf(sepy) >= 0 )
						theYear= inputValue.substring(inputValue.indexOf(sepy) + sepy.length(), inputValue.indexOf(sepdt));
					else
					{
					    if( inputValue.indexOf(sepy) >= 0 )
					    {
					        theYear= inputValue.substring(inputValue.indexOf(sepy) + sepy.length());
					    }
					    else
					    {
					        String newInputVal = inputValue.replaceAll( sepdt, "" );
					        theYear = ( newInputVal.length() >= 4 ? newInputVal.substring( 4 ) : sdf.format( new Date() ) );
					        
					        if( theYear.length() < 4 )
					        {
					            theYear = "11" + theYear;
					        }
					    }
					}
					
					theYear = Integer.toString(Integer.parseInt(theYear));
					
					return theYear;
				}
				catch (Exception e)
				{
					e.printStackTrace();
					return sdf.format( new Date() );
				}
			}
			else
				return sdf.format( new Date() );
        }
	}
	/**
	 * Returns the imagePath.
	 * @return String
	 */
	public static String getImagePath()
	{
		return msImagePath;
	}
	/**
	 * Sets the imagePath.
	 * @param imagePath The imagePath to set
	 */
	public static void setImagePath(String imagePath)
	{
		msImagePath= imagePath;
	}
}
