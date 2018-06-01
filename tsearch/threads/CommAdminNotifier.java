package ro.cst.tsearch.threads;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.Search;
import ro.cst.tsearch.community.CommunityAttributes;
import ro.cst.tsearch.community.CommunityUtils;
import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DBReports;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.database.rowmapper.NameMapper;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.reports.data.CommAdminData;
import ro.cst.tsearch.reports.data.DayReportLineData;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.URLMaping;

public class CommAdminNotifier extends TimerTask
{
    private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
    private static final Logger logger = Logger.getLogger(CommAdminNotifier.class);
    
    private int daysBack;
    private int daysInterval;
    
    public static void init()
    {
        int defaultDaysBack = 5;
        int defaultDaysInterval = 1;
        
        // if the commadmin.notifier.start is defined and has "false" property, then do not run the notifier
        boolean start = true;
        try{	
        	start = Boolean.parseBoolean( rbc.getString( "commadmin.notifier.start" ).trim() );
        }catch(Exception e){        	
        }
        if(!start){
        	System.err.println("commadmin.notifier.start=false");
        	return;
        }
        
        try
        {
            defaultDaysBack = Integer.parseInt( rbc.getString( "commadmin.notifier.daysback" ).trim() );
        }
        catch( Exception e )
        {}

        try
        {
            defaultDaysInterval = Integer.parseInt( rbc.getString( "commadmin.notifier.dayinterval" ).trim() );
        }
        catch( Exception e )
        {}
        
        CommAdminNotifier commAdminNotifierScheduler = new CommAdminNotifier( defaultDaysBack, defaultDaysInterval );
        
        Timer timer = new Timer();
        
        //default start hour
        int startHour = 0;
        Calendar cal = Calendar.getInstance();
        Date firstTime = cal.getTime();
        
        try
        {
            startHour = Integer.parseInt( rbc.getString( "commadmin.notifier.starthour" ).trim() );
            cal.set( Calendar.HOUR_OF_DAY, startHour );
            cal.set( Calendar.MINUTE, 0 );
            cal.set( Calendar.SECOND, 0 );
            cal.set( Calendar.MILLISECOND, 0 );
            
            firstTime = cal.getTime();
        }
        catch( Exception e )
        {}
        
        timer.scheduleAtFixedRate( (TimerTask) commAdminNotifierScheduler, firstTime , 24 * 60 * 60 * 1000 );       
        logger.info( "CommAdminNotifier Task scheduled for execution starting " + firstTime + " every 24 Hours " );
        logger.info( "CommAdminNotifier info : daysBack = " + defaultDaysBack + ", daysInterval = " + defaultDaysInterval );
    }
    
    public CommAdminNotifier( int daysBack, int daysInterval )
    {
        this.daysBack = daysBack;
        this.daysInterval = daysInterval;
    }
    
    public void run()
    {
        logger.info( "CommAdminNotifier START" );
        
        generateReports();
        
        logger.info( "CommAdminNotifier ENDED" );
    }
    
    public static String setReportHeader( String dayInfo )
    {
        StringBuilder header = new StringBuilder("<TABLE cellSpacing=0 cellPadding=0 width=\"100%\" align=center border=0>\n");
        header.append("\t<TBODY>\n")
	        .append("\t\t<TR>\n")
	        .append("\t\t\t<TD align=center>" + dayInfo + "</TD>\n")
	        .append("\t\t</TR>\n")
	        .append("\t\t<TR>\n")
	        .append("\t\t\t<TD align=center><TABLE borderColor=#a5a2a5 cellSpacing=0 cellPadding=1 width=\"98%\" border=1>\n")
	        .append("\t\t\t<TBODY>\n")
	        .append("\t\t\t\t<TR class=headerSubDetailsRow>\n")
	        .append("\t\t\t\t\t<TD align=\"center\">Abstractor</TD>\n")
	        .append("\t\t\t\t\t<TD align=\"center\">Owner</TD>\n")
	        .append("\t\t\t\t\t<TD align=\"center\">Agent</TD>\n")
	        .append("\t\t\t\t\t<TD align=\"center\">County</TD>\n")
	        .append("\t\t\t\t\t<TD align=\"center\">Property Address</TD>\n")
	        .append("\t\t\t\t\t<TD align=\"center\">TS Order <br>(GMT <SPAN id=\"timezone2\"></span>)</TD>\n")
	        .append("\t\t\t\t\t<TD align=\"center\">TS ID</TD>")
	        .append("\t\t\t\t\t<TD align=\"center\">Status</TD>")
	        .append("\t\t\t\t</TR>");
        return header.toString();
    }
    
    public static String setDayReportRow( String abstractorName, String ownerName, String agentName, 
    		String countyName, String propertyAddress, 
    		String tsOrder, String fileName, String searchStatus, String abstractorEmail, 
    		String rowClass)
    {
        StringBuilder row = new StringBuilder("<TR class=" + rowClass + ">");
        
        row.append("\t\t\t\t\t<TD align=\"center\"><A href=\"mailto:" + abstractorEmail + "\">" + abstractorName + "</A></TD>\n")
	        .append("\t\t\t\t\t<TD align=\"center\">" + ownerName + "</TD>\n")
	        .append("\t\t\t\t\t<TD align=\"center\">" + agentName + "</TD>\n")
	        .append("\t\t\t\t\t<TD align=\"center\">" + countyName + "</TD>\n")
	        .append("\t\t\t\t\t<TD align=\"center\">" + propertyAddress + "</TD>\n")
	        .append("\t\t\t\t\t<TD align=\"center\">" + tsOrder + "</TD>\n")
	        .append("\t\t\t\t\t<TD align=\"center\">" + fileName + "</TD>")
	        .append("\t\t\t\t\t<TD align=\"center\">" + searchStatus + "</TD>")
	        .append("</TR>");
        
        return row.toString();
    }
    
    public static String setReportFooter()
    {
        StringBuilder footer = new StringBuilder("</TBODY>");
        
        footer.append("</TABLE>")
	        .append("</TD>")
	        .append("<TD height=\"10\"></TD>")
	        .append("</TR>")
	        .append("</TBODY></TABLE>");
        
        return footer.toString();
    }
    
    public void generateReports()
    {
    	
    	
        DBConnection conn = null;
        String fullReport = "";
        
	    try
	    {
	        conn = ConnectionPool.getInstance().requestConnection();
	        DatabaseData dbData = conn.executeSQL(
	        		"SELECT a.COMM_ID, b.EMAIL, b.A_EMAIL FROM " + 
	        		DBConstants.TABLE_COMMUNITY + " a, " +
	        		DBConstants.TABLE_USER + " b WHERE a.COMM_ADMIN = b.USER_ID");
	        
	        int rows = dbData.getRowNumber();
	        
	        for( int i = 0 ; i < rows ; i++ )
	        {
	            fullReport = "";
	            String communityId = dbData.getValue( 1,i ).toString();
	            String commAdminEmailAddress = dbData.getValue( 2,i ).toString();
	            String commAdminEmailAddress2 = dbData.getValue( 3,i ).toString();
	            
	            String emailTo = commAdminEmailAddress;
	            if( "".equals( commAdminEmailAddress ) )
	            {
	                emailTo = commAdminEmailAddress2;
	            }
	            
	            //generate the report for each community	            
	            fullReport = buildReportsCommunity( Long.parseLong( communityId ) );
	            
		        if( fullReport.equals( "" ) == false )
		        {
		            //send the email only if there are old searches
			        fullReport = "<HTML><HEAD><LINK media=screen href='http://ats.advantagetitlesearch.com:9000/title-search/web-resources/images/default_reports.css' type=text/css rel=stylesheet></HEAD><BODY>" + fullReport + "</BODY></HTML>";
	        
			        logger.info( "Sending old search notify email to " + emailTo );
		            MimeMessage mail = Log.prepareMailMessage(MailConfig.getMailFrom(),
		                    emailTo, "", "", 
		                    "Old Searches",
		                    fullReport);
		            mail.setContent( fullReport,"text/html" );
		            Transport.send(mail);

		        }
	        }
	    }
	    catch( Exception ex )
	    {
	        ex.printStackTrace();
	    }
	    finally
	    {
	        try
	        {
	            ConnectionPool.getInstance().releaseConnection(conn);
	        }
	        catch (BaseException e)
	        {
	            logger.error(e);
	        }
	    }
    }
    
    public String buildReportsCommunity(long commId)
    {
        DBConnection conn = null;
        StringBuilder fullReport = new StringBuilder();
        String query = "";
        
	    try
	    {
	        conn = ConnectionPool.getInstance().requestConnection();
	        int offset = daysInterval + daysBack;
	        
	        while( offset > daysBack )
	        {        
		        Calendar today = GregorianCalendar.getInstance();
		        today = CommAdminNotifier.addDaysIgnoreWeekends( today, - offset );
		        
		        Date fromDate = today.getTime();
		        
		        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy 23:59:59");
		        SimpleDateFormat sdf2 = new SimpleDateFormat("MM-dd-yyyy 00:00:00");
		        
		        query = "SELECT c.EMAIL, c.FIRST_NAME abstractor_fname, c.LAST_NAME abstractor_lname, "
		            	+ "'owner_fname', 'owner_lname', "
		            	+ "e.FIRST_NAME agent_fname, e.LAST_NAME agent_lname, "
		            	+ "f.NAME county, b.ADDRESS_NAME address_name, b.ADDRESS_NO address_no, b.ADDRESS_SUFFIX address_suffix, "
		            	+ "DATE_FORMAT(a.SDATE, '%c-%e-%Y %H:&i:%S') tsr_timestamp, a.ABSTR_FILENO file_no, "
		            	+ "g.STATEABV,af.INVOICE, a.ID, af.PAID, '', af.INVOICED, af.CONFIRMED, af.ARCHIVED, "
		            	+ "a.TSR_FOLDER, af." + DBConstants.FIELD_SEARCH_FLAGS_TSR_CREATED + ", a.STATUS, h.STATUS_SHORT_NAME, af.CHECKED_BY, a.ID searchId, "
		            	+ "DATE_FORMAT(a.TSR_DATE, '%c-%e-%Y %H:&i:%S') "
		            	+ "FROM " + DBConstants.TABLE_SEARCH+" a " + 
		            		" JOIN " + DBConstants.TABLE_SEARCH_FLAGS + " af ON a.ID = af." + DBConstants.FIELD_SEARCH_FLAGS_ID +
		            		" JOIN " + DBConstants.TABLE_PROPERTY+" b ON a.property_id = b.ID " + 
		            		" LEFT JOIN " + DBConstants.TABLE_USER+" c ON a.ABSTRACT_ID = c.USER_ID " + 
		            		" LEFT JOIN " + DBConstants.TABLE_AGENT+" d ON a.OWNER_ID = d.AGENT_ID " + 
		            		" LEFT JOIN " + DBConstants.TABLE_USER+" e ON a.AGENT_ID = e.USER_ID " + 
		            		" LEFT JOIN " + DBConstants.TABLE_COUNTY+" f ON b.COUNTY_ID = f.ID " + 
		            		" LEFT JOIN " + DBConstants.TABLE_STATE+" g ON b.STATE_ID = g.ID " + 
		            		" JOIN " + DBConstants.TABLE_SEARCH_STATUS+" h ON h.STATUS_ID = a.STATUS WHERE " 
		            	+ "(a.sdate >= STR_TO_DATE('" + sdf2.format( fromDate ) + "', '%c-%e-%Y %H:&i:%S')) "
		            	+ "AND (a.sdate <= STR_TO_DATE('" + sdf.format( fromDate ) + "', '%c-%e-%Y %H:&i:%S')) "
		            	+ "AND a.COMM_ID = '" + commId + "' "
		            	+ "AND ( a.STATUS = " + Search.SEARCH_STATUS_N + " OR a.STATUS = " + Search.SEARCH_STATUS_T + " )";
		        
		        DatabaseData dbData = conn.executeSQL(query);
		        
		        int rows = dbData.getRowNumber();
		        
		        if( rows > 0 )
		        {
			        fullReport.append(CommAdminNotifier.setReportHeader( 
			        		(new SimpleDateFormat( "MM-dd-yyyy" )).format( fromDate ) ));	
			        LinkedHashMap<Long, CommAdminData> allRows = new LinkedHashMap<Long, CommAdminData>();
			        StringBuilder searchIds = new StringBuilder();
			        for( int i = 0 ; i < rows ; i++ )
			        {
			        	BigInteger searchId = (BigInteger)dbData.getValue(DBConstants.FIELD_SEARCH_ID, i);
			        	CommAdminData reportLine = new CommAdminData();
			        	allRows.put(searchId.longValue(), reportLine);
			        	reportLine.setSearchId(searchId.longValue());
			        	searchIds.append(reportLine.getSearchId() + ", ");
			        	
			            String abstractorEmail = "&nbsp;";
			            if( dbData.getValue(1,i)!= null)
			            {
			                abstractorEmail = dbData.getValue(1,i).toString();
			            }
			            
			            String abstractorName = "&nbsp;";
			            if( dbData.getValue(2,i) != null )
			            {
			                abstractorName += dbData.getValue(2,i).toString() + " ";
			            }
			            if( dbData.getValue(3,i) != null )
			            {
			                abstractorName += dbData.getValue(3,i).toString();
			            }
			            
			            String agentName = "&nbsp;";
			            if( dbData.getValue(6,i) != null )
			            {
			                agentName += dbData.getValue(6,i).toString() + " "; 
			            }
			            if( dbData.getValue(7,i) != null )
			            {
			                agentName += dbData.getValue(7,i).toString(); 
			            }
			            
			            String countyName = "&nbsp;";
			            if( dbData.getValue(14,i) != null )
			            {
			                countyName = dbData.getValue(14,i).toString() + " ";
			            }

			            if( dbData.getValue(8,i) != null )
			            {
			                countyName += dbData.getValue(8,i).toString();
			            }
			            
			            String no = dbData.getValue(10,i) != null ? dbData.getValue(10,i).toString() : "&nbsp;";
			            String street = dbData.getValue(9,i) != null ? dbData.getValue(9,i).toString() : "&nbsp;";
			            String suffix = dbData.getValue(11,i) != null ? dbData.getValue(11,i).toString() : "&nbsp;"; 
			            
			            
			            if( no.indexOf( "'" ) == 0 && no.lastIndexOf( "'" ) == no.length() - 1 )
			            {
			                no = no.substring( 1, no.length() - 1 );
			            }
			            
			            if( street.indexOf( "'" ) == 0 && street.lastIndexOf( "'" ) == street.length() - 1 )
			            {
			                street = street.substring( 1, street.length() - 1 );
			            }
			            
			            if( suffix.indexOf( "'" ) == 0 && suffix.lastIndexOf( "'" ) == suffix.length() - 1 )
			            {
			                suffix = suffix.substring( 1, suffix.length() - 1 );
			            }
			            String propertyAddress = no + "&nbsp;" + street + "&nbsp;" + suffix;
			            
			            
			            String tsOrder = "&nbsp;";
			            if( dbData.getValue(12,i) != null )
			            {
			                tsOrder += dbData.getValue(12,i).toString(); 
			            }
			            
			            
			            String fileName = "&nbsp;";
			            if( dbData.getValue(13,i) != null )
			            {
			                fileName += dbData.getValue(13,i).toString(); 
			            }
			            
			            String searchStatus = "&nbsp;";
			            if( dbData.getValue(25,i) != null )
			            {
			                searchStatus += dbData.getValue(25,i).toString(); 
			            }
			            
			            reportLine.setAbstractorName(abstractorName);
			            reportLine.setAgentName(agentName);
			            reportLine.setCountyName(countyName);
			            reportLine.setPropertyAddress(propertyAddress);
			            reportLine.setTsOrder(tsOrder);
			            reportLine.setFileName(fileName);
			            reportLine.setSearchStatus(searchStatus);
			            reportLine.setAbstractorEmail(abstractorEmail);
			            reportLine.setRowClass("row" + ((i % 2) + 1 ));
			            
			            
			            //fullReport += CommAdminNotifier.setDayReportRow(abstractorName, ownerName, agentName, countyName, 
			            //		propertyAddress, tsOrder, fileName, searchStatus, abstractorEmail, "row" + ((i % 2) + 1 ));
			        }
			        
			        if(allRows.size() > 0 ) {
			        	searchIds.delete(searchIds.length()-2, searchIds.length());
			        	
				        List<NameMapper> names = DBReports.getNamesForSearchIdsFromTable(searchIds.toString(), DBConstants.TABLE_PROPERTY_OWNER);
			        	for (NameMapper name : names) {
							CommAdminData temp = allRows.get(name.getSearchId());
							if(temp != null){
								temp.getOwners().add(name.getName());
							}
						}
			        	
			        	for (CommAdminData reportLine : allRows.values()) {
							fullReport.append( 
									CommAdminNotifier.setDayReportRow(
										reportLine.getAbstractorName(), 
										reportLine.getOwnersName(), 
										reportLine.getAgentName(), 
										reportLine.getCountyName(), 
										reportLine.getPropertyAddress(), 
										reportLine.getTsOrder(), 
										reportLine.getFileName(), 
										reportLine.getSearchStatus(), 
										reportLine.getAbstractorEmail(), 
										reportLine.getRowClass()));
						}
			        }
			        
			        fullReport.append(CommAdminNotifier.setReportFooter());
		        }
		        offset --;
	        }
	    }
	    catch( Exception ex )
	    {
	        ex.printStackTrace();
	    }
	    finally
	    {
	        try
	        {
	            ConnectionPool.getInstance().releaseConnection(conn);
	        }
	        catch (BaseException e)
	        {
	            logger.error(e);
	        }
	    }    
        
        return fullReport.toString();
    }
    
    public static boolean isOldSearch( Date timeStamp, BigDecimal commId)
    {
        return isOldSearch( (new SimpleDateFormat( "dd-MM-yyyy HH:mm:ss" )).format( timeStamp ), commId);
    }
    
    public static Calendar addDaysIgnoreWeekends( Calendar cal, int numberOfDays )
    {
        
        int i = 0;
        int sign = numberOfDays >= 0 ? 1 : -1;
        
        while( i < sign * numberOfDays )
        {
            cal.add(GregorianCalendar.DAY_OF_MONTH, sign);
            
            if( cal.get( GregorianCalendar.DAY_OF_WEEK ) != GregorianCalendar.SATURDAY && cal.get( GregorianCalendar.DAY_OF_WEEK ) != GregorianCalendar.SUNDAY )
            {
                i ++;
            }
        }
        return cal;
    }
    
    public static boolean isOldSearch( String searchDateString, BigDecimal commId )
    {
        //returns true if the date specified is between the configured days interval for old searches
        SimpleDateFormat sdf = new SimpleDateFormat( "dd-MM-yyyy HH:mm:ss" );
        Date searchDateObject;
        
        try
        {
            searchDateObject = sdf.parse( searchDateString );
        }
        catch (ParseException e)
        {
            e.printStackTrace();
            return false;
        }
        
        CommunityAttributes ca = null;
        
        try {
			ca = CommunityUtils.getCommunityFromId(commId.longValue());
		} catch (BaseException e1) {
			e1.printStackTrace();
		}
        
		int defaultHoursBack = ca.getDEFAULTSLA().intValue();
        
        Calendar today = GregorianCalendar.getInstance();
        today = CommAdminNotifier.addHoursIgnoreWeekends( today, -  defaultHoursBack );
        today.set( GregorianCalendar.HOUR_OF_DAY, 23 );
        today.set( GregorianCalendar.MINUTE, 59 );
        today.set( GregorianCalendar.SECOND, 59 );
        
        if( searchDateObject.compareTo( today.getTime() ) <= 0 )
        {
            return true;
        }
        
        return false;
    }
    
    /**
     * This function acts the same as the "public static boolean isOldSearch( String searchDateString, BigDecimal commId )"
     * but uses directly the defaultHoursBack. Optimization in setDetailedReportData in DBManager so the community isn't read every time 
     * @param searchDateString
     * @param defaultHoursBack
     * @return
     */
    public static boolean isOldSearch( String searchDateString, int defaultHoursBack )
    {
        //returns true if the date specified is between the configured days interval for old searches
        SimpleDateFormat sdf = new SimpleDateFormat( "dd-MM-yyyy HH:mm:ss" );
        Date searchDateObject = null ;
        
        try
        {
            searchDateObject = sdf.parse( searchDateString );
        }
        catch (ParseException e)
        {
            e.printStackTrace();
            return false;
        }
        
        Calendar today = GregorianCalendar.getInstance();
        
        today = CommAdminNotifier.addHoursIgnoreWeekends( today, -  defaultHoursBack );
        today.set( GregorianCalendar.HOUR_OF_DAY, 23 );
        today.set( GregorianCalendar.MINUTE, 59 );
        today.set( GregorianCalendar.SECOND, 59 );
        Date toDate = today.getTime();
        
        if( searchDateObject.compareTo( toDate ) <= 0 )
        {
            return true;
        }
        
        return false;
    }
    
    public static boolean arrayContains(int[] array, int value)
    {
	    boolean found = false;
	    
	    
	    for( int i = 0; i < array.length ; i++ )
	    {
	        if( array[i] == value )
	        {
	            found = true;
	        }
	    }
	    
	    return found;
    }
    
    public static DayReportLineData[] filterResults( DayReportLineData[] originalReportData, int[] reportStatus, BigDecimal commId  )
    {
	    Vector<DayReportLineData> temp = new Vector<DayReportLineData>(1,1);
	    Vector<DayReportLineData> temp2 = new Vector<DayReportLineData>(1,1);
	    boolean oState = CommAdminNotifier.arrayContains( reportStatus, 13 );
	    boolean nState = CommAdminNotifier.arrayContains( reportStatus, 10 );
	    boolean tState = CommAdminNotifier.arrayContains( reportStatus, 11 );
	    boolean nandtState = CommAdminNotifier.arrayContains( reportStatus, 14 );
        boolean dState = CommAdminNotifier.arrayContains( reportStatus, 0 );
        boolean dandkState = CommAdminNotifier.arrayContains( reportStatus, 15 );
	    //boolean iState = CommAdminNotifier.arrayContains( reportStatus, 3 );
        //boolean kState = CommAdminNotifier.arrayContains( reportStatus, 12 );
	    
        String stat = "";
        
	    for( int i = 0 ; i < originalReportData.length ; i++ )
	    {
            stat = originalReportData[i].getStatus();
            
            
            if( dState && !tState )
            {
                if( stat.indexOf( "T" ) >= 0 && ! originalReportData[i].getWasOpened() )
                {
                    continue;
                }
            }
            
			if( oState && ( !nState || !tState ) )
			{
			    // O state filter selected
			    
			    if( !nState && !tState  )
			    {
			        // only O selected
			        if( !CommAdminNotifier.isOldSearch( originalReportData[i].getSearchTimeStamp(), commId ) && ( originalReportData[i].getStatus().indexOf( "N" ) >= 0  || originalReportData[i].getStatus().indexOf( "T" ) >= 0 ) )
			        {
			            continue;
			        }
                    
                    if( !temp.contains( originalReportData[i] ) )
                    {
                        temp.add( originalReportData[i] );
                    }
			    }
			    else if( nState && !tState )
			    {
			        //O, N selected, T not selected
			        if( !CommAdminNotifier.isOldSearch( originalReportData[i].getSearchTimeStamp(), commId ) && originalReportData[i].getStatus().indexOf( "T" ) >= 0 )
			        {
			            continue;
			        }
                    
                    if( !temp.contains( originalReportData[i] ) )
                    {
                        temp.add( originalReportData[i] );
                    }
			    }
			    else if( !nState && tState )
			    {
			        //O, T selected, N not selected
			        if( !CommAdminNotifier.isOldSearch( originalReportData[i].getSearchTimeStamp(), commId ) &&  originalReportData[i].getStatus().indexOf( "N" ) >= 0 )
			        {
			            continue;
			        }
                    
                    if( !temp.contains( originalReportData[i] ) )
                    {
                        temp.add( originalReportData[i] );
                    }
			    }
			}
			else if( nandtState && !oState )
			{
		        if( CommAdminNotifier.isOldSearch( originalReportData[i].getSearchTimeStamp(), commId ) &&  ( originalReportData[i].getStatus().indexOf( "N" ) >= 0 || originalReportData[i].getStatus().indexOf( "T" ) >= 0) )
		        {
		            continue;
		        }
                
                if( !temp.contains( originalReportData[i] ) )
                {
                    temp.add( originalReportData[i] );
                }
			}
			else
			{
                if( !temp.contains( originalReportData[i] ) )
                {
                    temp.add( originalReportData[i] );
                }
			}
			//if(dandkState )
			
	    }
	    
	    
	    DayReportLineData dayReportData[] = new DayReportLineData[0];
	    if(dandkState){
	    	for (Iterator<DayReportLineData> iter = temp.iterator(); iter.hasNext();) {
	    		DayReportLineData element = (DayReportLineData) iter.next();
				stat = element.getStatus();
				if(stat.indexOf("D")>=0 || stat.indexOf("K")>=0)
					temp2.add(element);
			}
	    	dayReportData = new DayReportLineData[ temp2.size() ];
		    temp2.copyInto( dayReportData );
	    	
	    }
	    else {
		    dayReportData = new DayReportLineData[ temp.size() ];
		    temp.copyInto( dayReportData );
	    }
	    
	    return dayReportData;
    }
   
    public static Calendar addHoursIgnoreWeekends( Calendar cal, int numberOfHours )
    {
        
        int i = 0;
        int sign = numberOfHours >= 0 ? 1 : -1;
        
        while( i < sign * numberOfHours )
        {
            cal.add(GregorianCalendar.HOUR_OF_DAY, sign);
            
            if( cal.get( GregorianCalendar.DAY_OF_WEEK ) != GregorianCalendar.SATURDAY && cal.get( GregorianCalendar.DAY_OF_WEEK ) != GregorianCalendar.SUNDAY )
            {
                i ++;
            }
        }
        return cal;
    }

}