package ro.cst.tsearch.generic;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.apache.log4j.Logger;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.servers.response.OCRParsedDataStruct;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.ParameterParser;
import ro.cst.tsearch.utils.URLMaping;
 
public class Util {

	private static ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
	
	private static final Logger logger = Logger.getLogger(Util.class);
	
    public static final String FILE_SEPARATOR = File.separator;
	public static final boolean IS_DEBUG_MODE = false;
	
	static DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT,
	            Locale.US);
	    
	static SimpleDateFormat df_year = new SimpleDateFormat("yyyy");
	static SimpleDateFormat df_month_year = new SimpleDateFormat("MMMMMMMM dd, yyyy");
	    
	
    public Util() {
    }

    public static byte[] getRawData(String filePath) {
        byte[] data = null;
        
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        
        int contentLength = (int)file.length();
        if (contentLength == 0) { // file not found
            return null;
        } 
               
        try {
            data = new byte[(int)contentLength];
            FileInputStream fis = new FileInputStream(file); 
            fis.read(data);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }
    
   /**
    * Copy the bytes array to a given outputstream
    */
    public static void putRawData(byte[] data, String filePath) throws IOException {       
        FileOutputStream destination  = null;
        try {
            /*	
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write(data);
            fos.close();
            */
            destination = new FileOutputStream(filePath);
            copy(data, destination , data.length);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException ("Can not copy the data to file. Exception:" + e.getMessage());
        } finally {
            IOUtil.shutdownStream( destination );
        }        

    }

    
    /**
     * Copy from source to destination first len bytes
     */
    public static void copy( InputStream source, OutputStream destination,  int len)
    	throws IOException
    {

	int bread = 0;
	int bold = 0;
    	
    	try {
    	
	        final BufferedInputStream  input = new BufferedInputStream( source );
	        final BufferedOutputStream output = new BufferedOutputStream( destination );
	
	        final int BUFFER_SIZE = 1024 * 4;
	        final byte[] buffer = new byte[ BUFFER_SIZE ];
	
	
		while (bold<len) {
			int x = (BUFFER_SIZE<(len-bold))?BUFFER_SIZE:(len-bold);
			bread = input.read( buffer, 0, x );
			//can throws ArrayIndexOutOfBoundsException  
			output.write( buffer, 0, bread ); 
			bold += bread;
		}
            	//needed to flush cache
            	output.flush();
    	
	} 
	catch( Exception e ) { //IOException and .ArrayIndexOutOfBoundsException
		//e.printStackTrace();
		//logger.error( "Last Au venit " + bread + " bytes" );
		//logger.error( "Total Au venit " + bold + " bytes" );
		throw new IOException (bold + " bytes read  but had to be " + len + ". Cause: " + e);
	}
	/*
	 finally {
            IOUtil.shutdownStream( source );
            IOUtil.shutdownStream( destination );
        }
	*/
	logger.info(bold + " bytes read ");
    }


    /**
     * copy bytes array to destionation
     */
    public static void copy( byte[] source, OutputStream destination,  int len)
    	throws IOException
    {
        logger.info("Debug:  Util#copy CHECK ME NOW" );
	int bread = 0;
	int bold = 0;
    	
    	try {
    	        
    	        
	        //final BufferedInputStream  input = new BufferedInputStream( source );
	        final BufferedOutputStream output = new BufferedOutputStream( destination );	        	        
	        final int BUFFER_SIZE = 1024 * 4;
	        final byte[] buffer = new byte[ BUFFER_SIZE ];
	
	
		while (bold<len) {
			//			logger.info( "bold " + bold );
			//			logger.info( "len - bold " + (len-bold) );
			int x = (BUFFER_SIZE<(len-bold))?BUFFER_SIZE:(len-bold);
			//			logger.error( "Citesc " + x + " bytes" );
			//logger.error( "bold= " + bold + ";x="  + x + " bytes" );
			System.arraycopy(source, bold, buffer, 0 , x);
			//bread = input.read( buffer, 0, x );
			
			//			logger.error( "Au venit " + bread + " bytes" );
			//can throws ArrayIndexOutOfBoundsException  
			output.write( buffer, 0, x ); 
			bold += x;
			//			logger.error( "Au venit " + bold + " bytes" );
		}
		//needed to flush cache
		logger.error( "***DEBUG: Flush the  buffered output" );
    		output.flush();    		
	} 
	catch( Exception e ) { //IOException and .ArrayIndexOutOfBoundsException
		e.printStackTrace();
		//logger.error( "Last Au venit " + bread + " bytes" );
		//logger.error( "Total Au venit " + bold + " bytes" );
		throw new IOException (bold + " bytes read  but had to be " + len + ". Cause: " + e);
	}
	/*
	 finally {
            IOUtil.shutdownStream( source );
            IOUtil.shutdownStream( destination );
        }
	*/
	logger.info(bold + " bytes read ");
    }

    public static void shutdownSocket( final Socket socket )
    {
        if( null == socket ) return;

        try { socket.close(); }
        catch( final IOException ioe ) {}
    }

    /**
     * @param       str  the string to be parsed
     * @param       delim  the delimiters for tokens
     * @return      an array of String
     * Splits a string into an array of tokens using delimiters.
     */
    public static String[] getStringArray( String str, String delim )
    {
	 Vector v = new Vector();
	 StringTokenizer st = new StringTokenizer( str, delim );
	 while (st.hasMoreTokens()) {
	     v.add( st.nextToken() );
	 }
	 return (String[])v.toArray( new String[v.size()] );
    }    

	/**
	 * @param       str  the strings array
	 * @param       delim  the delimiter between original strings
	 * @return      the resulting String
	 * Concatenates the strings into another string using delimiters.
	 */
	public static String getStringsList( String[] str, String delim )
	{
		String res = null;
		boolean isFirst = true;
		for (int i = 0; i < str.length; i++)
			if (str[i] != null && isFirst){
				isFirst = false;
				res = str[i];
			}else{
				res += delim + str[i];
			}
		return res;
	}    

    /**
     * @param       s  the string to be converted to byte array
     * @param       len  the desired length of the byte array
     * @return      byte array of length len containing string s and padded with 0 bytes
     * Converts or truncates a string into a fixed length byte array.
     */
    public static byte[] getByteArray( String s, int len )
    {
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	int l = 0;
	bos.write( s.getBytes(), 0, (l = (s.length()>len)?len:s.length()) ); 
	for( int i=l; i<len; i++ )
	    bos.write( 0 );
	return bos.toByteArray();
    }

    /**
     * @param       fileName  the name of the file to be read
     * @return      byte array representing the content of the file
     * @exception   Exception 
     */
    public static byte[] loadFile( String fileName )
        throws FileNotFoundException, IOException
    {
	ByteArrayOutputStream bout = null;
	InputStream fin = null;
	try {
	    fin = new FileInputStream( fileName );
	    int l = fin.available();
	    bout = new ByteArrayOutputStream( l );
	    int remained = l;
	    int part = 4000; // this is how much we read once
	    
	    byte[] b = new byte[l];
	    
	    while (remained>0) {
		int bread = fin.read( b, 0, (part<remained)?part:remained );
		bout.write( b, 0, bread );
		remained = remained - bread;
	    }
	    return bout.toByteArray();
	}finally {
	    try {
		fin.close(); 
		bout.close();
	    }catch( Exception e )
		{}
	}
    }


    /**
     * log message
     */
    public static void log(Object o, String msg) {
        String s = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss:SSSS ").format(new java.util.Date());
        s += o.getClass().getName();
        log(s + ":" + msg);
    }
    
    /**
     * log message
     */
    public static void log(String who, String msg) {
        String s = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss:SSSS ").format(new java.util.Date());
        log(s + " " + who + ":" + msg);
    }
    

    /**
     * log message
     */
    //on release this function should not do nothing 
    //it should be used only for debug reason
    public static void log(String msg) {
    	if (IS_DEBUG_MODE)    	        
        	logger.info(msg);
    }

    /**
     * truncates or pads string s to l chars
     * with the character ch
     */
    public static String pad( String s, int l, String ch, boolean before )
    {
	try
	    {
		s = s.substring( 0, l );
	    }catch( StringIndexOutOfBoundsException iobe )
		{
		    if (before)
			{
			    String temp = "";
			    for( int i=s.length(); i<l; temp += ch, i++ );
			    s = temp + s;
			}
		    else
			for( int i=s.length(); i<l; s += ch, i++ );
		}
		return s;	
    }
    
    /**
     *  creates directory
     */
    public static void mkdirs(String path) throws Exception {
        File f = new File(path);
        if(f.isDirectory()) return;
        if(!f.mkdirs()) 
            throw new Exception("Cannot mkdirs " + path);
    }

    /**
     * 
     */
    public static String removeDuplicateTokens( String str, String separator )
    {
	String result = "";
	HashSet hs = new HashSet( 2, 5 );
	StringTokenizer st = new StringTokenizer( str, separator );
	while( st.hasMoreTokens() )
	    {
		String token = st.nextToken();
		if (! hs.contains(token))
		    {
			hs.add( token );
		    }
	    }
	Iterator it = hs.iterator();
	while( it.hasNext() )
	    {
		if (result.length()>0) result += ",";
		result += it.next().toString();
	    }
	//	logger.info( "Result:" + result );
	return result.trim();
    }


    /**
     * this method detects the date format of this date string
     * the possible formats are MM/dd/yy, MM/dd/yyyy, MM-dd-yy, MM-dd-yyyy
     */
    public static String getDateFormat( String date ) {
	String delim = "";
	if (date.indexOf("/") != -1) {
	    delim = "/";
	} else { 
	    if (date.indexOf("-") != -1) {
		delim = "-";
	    } 
	}
	if (delim.equals("")) {
	    return null;
	}
	StringTokenizer st = new StringTokenizer( date, delim );
	String month = (st.hasMoreTokens())?st.nextToken():"";
	String day = (st.hasMoreTokens())?st.nextToken():"";
	String year = (st.hasMoreTokens())?st.nextToken():"";
	if (year.length()==2) {
	    return "MM" + delim + "dd" + delim + "yy";
	} 
	return "MM" + delim + "dd" + delim + "yyyy";
    }

    //////////////////////////////////////////////////////////

    //////////////////////Tags section//////////////////////

    //////////////////////////////////////////////////////////

    public static void gotoPage(String address,
                                ServletContext sc,
                                ServletRequest request,
                                ServletResponse response)
        throws ServletException, IOException {

        RequestDispatcher dispatcher = sc.getRequestDispatcher(address);
        if(dispatcher != null)
            dispatcher.forward(request, response);
    }

    public static void gotoPage(String address, HttpServletResponse resp)
        throws IOException {

        if(resp != null)
            resp.sendRedirect(address);
    }

    public static void gotoPage(String address, PageContext ctx)
        throws ServletException, IOException {

        if(ctx != null)
            ctx.forward(address);
    }


    public static String toTitleCase(String text) {
        char[] c = text.toCharArray();
        if(c.length == 0) return text;
        c[0] = Character.toUpperCase(c[0]);
        return new String(c);
    }

    
    public static String[] split(String delimiter, String text) {
        int prev = 0;
        int pos = text.indexOf(delimiter);
        Vector v = new Vector();
        
        while(pos != -1){
            v.addElement(text.substring(prev, pos));
            prev = pos + delimiter.length();
            pos = text.indexOf(delimiter, prev);
        }
        v.addElement(text.substring(prev));
        
        return ((String[])v.toArray(new String[v.size()]));
    }

   /**
    * 
    */
    public static String[] split(String text, int len) {
      
        int lenProcess = 0;        
        int i = 0;
        String s = "";
        String[]  returnArray = new String [ 
    				 ((text.length()%len)==0)?(new Integer(text.length()/len)).intValue():(new Integer(text.length()/len)).intValue()+1
        			 	   ];
	try {
            while (text.length()!=0) {		    
    			s = text.substring( 0, len );
    			lenProcess += len;
    			returnArray[i] = s;
    			i++;
    			text = text.substring(len, text.length());
    		    	
    		}
    			
	} catch( StringIndexOutOfBoundsException iobe )	{

	    //logger.info("Pe catch =" + text);  
	    //Because now I sent all string that must be sent
	    returnArray[i] = pad(text,len);	
	}
	return returnArray;
        
    }   

    /**
     * truncates or pads string s to l chars
     */
     public static String pad( String s, int l ) {
	try
	{
		s = s.substring( 0, l );
	}catch( StringIndexOutOfBoundsException iobe )
	{
		for( int i=s.length(); i<l; s += " ", i++ );
	}
	return s;	
    }
     
    
    public static String replaceTags(String tag, String text, Hashtable values) {
        StringBuffer sb = new StringBuffer(tag.length());

        int prev = 0;
        int pos = text.indexOf(tag);
        while(pos != -1){
            sb.append(text.substring(prev, pos));
            prev = pos + tag.length();
            pos = text.indexOf(tag, prev);
            if(pos == -1) break;
            
            String name = text.substring(prev, pos);
            if(name == null || name.length() == 0)
                sb.append(tag);
            else {
                String rep = String.valueOf(values.get(name));
                if(rep != null) sb.append(rep);
                else sb.append(tag + name + tag);
            }
            

            prev = pos + tag.length();
            pos = text.indexOf(tag, prev);
        }
       if (!("  <!--0 x US$ 0.00-->".equals(sb.toString())||("  ".equals(sb.toString())/*&&"<br>".equals(text.substring(prev))*/))&&"@@".equals(tag) )//2212 fix...kind of stupid but it works
        	  sb.append(text.substring(prev));

        return sb.toString();
    }


    public static String getImgTagSelected(String image) {
        return getImgTagSelected(image, 0);
    }
    public static String getImgTagHover(String image) {
        return getImgTagHover(image, 0);
    }
    public static String getImgTag(String image) {
        return getImgTag(image, 0);
    }
    
    public static String getImgTag(String image, int border) {
        return "<img border='"+border+"' src='images/"+image+"'>";
    }
    public static String getImgTagSelected(String image, int border) {
        return "<img border='"+border+"' src='images/"+appendToFileName(image, "_selected")+"'>";
    }
    public static String getImgTagHover(String image, int border) {
        return "<img border='"+border+"' src='images/"+appendToFileName(image, "_hover")+"'>";
    }

    public static String appendToFileName(String fileName, String s) {
        int pos = fileName.lastIndexOf(".");
        if(pos!=-1) {
            return fileName.substring(0, pos-1) + s + fileName.substring(pos);
        } else {
            return fileName + s;
        }
    }
    
    public static String sprintf(String s1) {
        return StaticPrintFormat.sprintf(s1, ((Object []) (null)));
    }
    public static String sprintf(String s1, Object obj) {
        Object aobj[] = new Object[1];
        aobj[0] = obj;
        return StaticPrintFormat.sprintf(s1, aobj);
    }
    public static String sprintf(String s1, Object obj, Object obj1) {
        Object aobj[] = new Object[2];
        aobj[0] = obj;
        aobj[1] = obj1;
        return StaticPrintFormat.sprintf(s1, aobj);
    }
    public static String sprintf(String s1, Object obj, Object obj1, Object obj2) {
        Object aobj[] = new Object[3];
        aobj[0] = obj;
        aobj[1] = obj1;
        aobj[2] = obj2;
        return StaticPrintFormat.sprintf(s1, aobj);
    }
    public static String sprintf(String s1, Object obj, Object obj1, Object obj2, Object obj3) {
        Object aobj[] = new Object[4];
        aobj[0] = obj;
        aobj[1] = obj1;
        aobj[2] = obj2;
        aobj[3] = obj3;
        return StaticPrintFormat.sprintf(s1, aobj);
    }
    public static String sprintf(String s1, Object obj, Object obj1, Object obj2, Object obj3, Object obj4) {
        Object aobj[] = new Object[5];
        aobj[0] = obj;
        aobj[1] = obj1;
        aobj[2] = obj2;
        aobj[3] = obj3;
        aobj[4] = obj4;
        return StaticPrintFormat.sprintf(s1, aobj);
    }
    public static String sprintf(String s1, Object[] aobj) {
        return StaticPrintFormat.sprintf(s1, aobj);
    }

    public static String doEscapes(String s) {
        StringBuffer e = new StringBuffer(s.length());
        doEscapes(s, e);
        return e.toString();
    }

    public static void doEscapes(String s, StringBuffer stringbuffer) {
        int i = s.length();
        for(int j = 0; j < i; j++) {
            int k = s.charAt(j);
            switch(k) {
            case 13: // '\r'
                stringbuffer.append("\\r");
                break;

            case 10: // '\n'
                stringbuffer.append("\\n");
                break;

            case 9: // '\t'
                stringbuffer.append("\\t");
                break;

            case 34: // '"'
                stringbuffer.append("\\\"");
                break;

            case 39: // '\''
                stringbuffer.append("\\'");
                break;

            case 123: // '{'
                stringbuffer.append("\\{");
                break;

            case 125: // '}'
                stringbuffer.append("\\}");
                break;

            default:
                stringbuffer.append((char)k);
                break;
            }
        }

    }

    public static String undoEscapes(String s) {
        StringBuffer e = new StringBuffer(s.length());
        undoEscapes(s, e);
        return e.toString();
    }

    public static void undoEscapes(String s, StringBuffer stringbuffer) {
        boolean flag = false;
        int i = s.length();
        for(int j = 0; j < i; j++) {
            int k = s.charAt(j);
            if(flag) {
                switch(k) {
                case 110: // 'n'
                    stringbuffer.append('\n');
                    break;

                case 114: // 'r'
                    stringbuffer.append('\r');
                    break;

                case 116: // 't'
                    stringbuffer.append('\t');
                    break;

                case 123: // '{'
                    stringbuffer.append('{');
                    break;

                case 125: // '}'
                    stringbuffer.append('}');
                    break;

                case 34: // '"'
                    stringbuffer.append('"');
                    break;

                case 39: // '\''
                    stringbuffer.append('\'');
                    break;

                case 92: // '\\'
                    stringbuffer.append('\\');
                    break;

                default:
                    stringbuffer.append('\\');
                    stringbuffer.append((char)k);
                    break;
                }
                flag = false;
            } else
            if(k == 92)
                flag = true;
            else
                stringbuffer.append((char)k);
        }
    }

    /**
     * replace,in a given "text", all apparences of the "find" with "rep"
     * @param text the string in which the replace will me made
     * @param find is the token to be find and replxe
     * @param rep is the string use to replace the @param find
     */
    public static String replaceAll(String text, String find, String rep) {
        //Util.log("a intrat=" + text);
        StringBuffer sb = new StringBuffer("");
        
        if ((text==null) || (find==null) || (rep==null) ) {
            return text;        
        }
        
        if (find.length()==0) {
            return text;
        }
        
        int prev = 0;
        int pos = text.indexOf(find);
        if (pos==-1) {
            return text;
        }
        
        while(pos != -1){
            sb.append(text.substring(prev, pos));
            sb.append(rep);            
            prev = pos + find.length();
            pos = text.indexOf(find, prev);
        }
        sb.append(text.substring(prev));
        //Util.log("a iesit=" + sb.toString());
        
        return sb.toString();
    }

    public static  String getParamRequestOrSession(String param, HttpServletRequest req, HttpSession session) throws Exception {
    	String ret = "";
    	logger.debug("Debug: Util#getParamRequestOrSession PARAM-------:" + param);
    	if (param==null) {
    		throw new Exception("Util#getParamRequestOrSession: param must be different from null");
    	}        
        ParameterParser pp = new ParameterParser(req);
        ret = pp.getStringParameter(param, "");
        if (ret.equals("")) {
            ret =(String) session.getAttribute(param);
            if (ret==null) {
                ret = "";
            }
        }            
        return ret;
    }
    
    public static String getParamSessionOrRequest(String param, HttpServletRequest req, HttpSession session) 
        throws Exception {
    	String ret = "";
		logger.debug("Debug: Util#getParamRequestOrSession PARAM-------:" + param);
    	if (param==null) {
    		throw new Exception("Util#getParamSessionOrRequest: param must be different from null");
    	}
        ret =(String) session.getAttribute(param);        
        if (ret==null) {
            ParameterParser pp = new ParameterParser(req);
            ret = pp.getStringParameter(param, "");
        }            
        return ret;
    }


    /**
     * convert days to milliseconds
     */
    public static long getMillisFromDays( int days ) {
	return (long)days * 86400000;
    }

    /**
     * convert hours to milliseconds
     */
    public static long getMillisFromHours( int hours ) {
	return (long)hours * 3600000;
    }


    /** */
    public static String getLastFirstName(String firstName, String lastName) {
		if (lastName==null)
			if (firstName==null)
				return null;
			else
				return firstName;
		else	
			if (firstName==null)
				return lastName;
			else
				return lastName + ", " + firstName;
    }


    /** */
    public static String getFirstLastName( String firstName, String lastName ) {
    	if (lastName==null)
    		if (firstName==null)
    			return null;
    		else
    			return firstName;
    	else	
    		if (firstName==null)
    			return lastName;
    		else
    			return firstName + " " + lastName;
    }


    /**
     * @return how many weekend days are in interval [today, today - delayDays]
     * if today - delayDays is SUNDAY then increment ( it means to add also SATURDAY from week-end)
     * for example :
     * 		if today is MONDAY and delayDays=1 then @return should be 2 days
     *  	if today is SATURDAY and delayDays=1 then @return should be 1 day
     *		if today is SUNDAY and delayDays=1 then @return should be 2 days
     */ 	
    public static int getWeekEndDaysFromToday( int delayDays ) {    	
	Calendar c = Calendar.getInstance();
	return getWeekEndDaysFromCalendar(c, delayDays);
    }            


    /**
     * @return how many weekend days are in interval [c, c - delayDays]
     * if c - delayDays is SUNDAY then increment ( it means to add also SATURDAY from week-end)
     * for example :
     * 		if today is MONDAY and delayDays=1 then @return should be 2 days
     *  	if today is SATURDAY and delayDays=1 then @return should be 1 day
     *		if today is SUNDAY and delayDays=1 then @return should be 2 days
     */ 	
    public static int getWeekEndDaysFromCalendar( Calendar c, int delayDays ) {    	
	int watchTV =0;
	//Calendar c = Calendar.getInstance();
	for (int i=0;i<=delayDays;i++) {		
		if  (c.get( Calendar.DAY_OF_WEEK )==Calendar.SUNDAY) {
		    watchTV++;
		    c.add(Calendar.DATE, -1);
		} 
		if ( c.get( Calendar.DAY_OF_WEEK )==Calendar.SATURDAY) {
		    watchTV++;
		    c.add(Calendar.DATE, -1);
		}		
		c.add(Calendar.DATE, -1);
	}
	return watchTV;
    }            

	public static Date getDefaultDueDate(){

		Date now = new Date();
		//computing the Date of the default due date
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DAY_OF_MONTH, 15);
		cal.set(Calendar.MONTH, 2);

		cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
		cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
		cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
		if (now.after(cal.getTime()))
			cal.set(Calendar.YEAR, cal.get(Calendar.YEAR)+1);
		return cal.getTime();
	}

	public static boolean isValueInArray(int value, int[] array){
		for (int j=0; j<array.length; j++)
			if (value == array[j])
				return true;
		return false;
	}
	public static boolean isValueInArray(String value, String[] array){
		for (int i = 0; i < array.length; i++) {
			if (value.equals(array[i]))
				return true;
		}
		return false;
	}
	public static boolean isValueInArrayIgnoreCase(String value, String[] array){
		for (int i = 0; i < array.length; i++) {
			if (value.equalsIgnoreCase(array[i]))
				return true;
		}
		return false;
	}
	public static int[] extractArrayFromString(String s){
		String[] temp = Util.getStringArray(s, ",");
		int l = temp.length;
		int[] res = new int[l];
		for (int i=0; i<l; i++)
			res[i] = Integer.parseInt(temp[i]);
		return res;
	}
	/**
	 * Extracts a string array from a string. The delimiter used is comma (,). 
	 * The commas in the elements of the array were doubled.
	 * @param s
	 * @return
	 */
	public static String[] extractStringArrayFromString(String s){
		String sTemp = "";
		Vector<String> vString = new Vector<String>();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i); 
			if(c==','){
				if(i+1 != s.length() && s.charAt(i+1)==','){
					sTemp+=c;
					i++;	//jump over the next comma
				} else {
					vString.add(sTemp);
					sTemp="";
				}
			} else {
				sTemp+=c;
			}
		}
		vString.add(sTemp);
		
		return vString.toArray(new String[vString.size()]);
	}
	
	public static String getStringFromArray(int[] array){
		int l = array.length;
		String[] temp = new String[l];
		for (int i=0; i<l; i++)
			temp[i] = String.valueOf(array[i]);
		return Util.getStringsList(temp,",");
	}
	
	public static String getStringFromStringArray(String[] array){
		int l = array.length;
		if(l==0)
			return "";
		StringBuffer sb = new StringBuffer(array[0].replace(",", ",,"));
		for (int i=1; i<l; i++)
			sb.append("," + array[i].replace(",", ",,"));
		return sb.toString();
	}
	
	public static String getStringFromLongArray(Long[] array){
		int l = array.length;
		String[] temp = new String[l];
		for (int i=0; i<l; i++)
			temp[i] = String.valueOf(array[i]);
		return Util.getStringsList(temp,",");
	}
	
	public static int[] NandTnotO( int[] array )
	{
	    int initial_len = array.length;
	    int[] newArray = new int[initial_len + 2];
	    
	    int i = 0;
	    for( i = 0 ; i < initial_len ; i ++ )
	    {
            newArray[i] = array[i];
	    }
	    
	    newArray[ i++ ] = 10;
	    newArray[ i++ ] = 11;
	    return newArray;
	}
	public static int[] DandKnotI( int[] array )
	{
	    int initial_len = array.length;
	    int[] newArray = new int[initial_len + 1];
	    
	    int i = 0;
	    for( i = 0 ; i < initial_len ; i ++ )
	    {
    		newArray[i] = array[i];
	    }
	    
	    //newArray[ i++ ] = 1;		//DUE
	    //newArray[ i++ ] = 12;		//K
	    newArray[ i++ ] = 2;		//NOT_INVOICED
	    
	    return newArray;
	}
	
	public static String getFirstStringFromList(String s){
		int index = s.indexOf(','); 
		if(index != -1){
			s = s.substring(0, index);
		}
		return s;
	}
	
	public static boolean sendMail( 
						  String parFrom,
						  String parTo,
						  String parCc,
						  String parBcc,
						  String parSubject,
						  String parMessage){
		try{
			
			Properties props = System.getProperties();
			props.put("mail.smtp.host", MailConfig.getMailSmtpHost());

			Session session = Session.getDefaultInstance(props,null);
			MimeMessage msg = new MimeMessage(session);
			
			if(parFrom == null){
				parFrom = MailConfig.getMailFrom();
			}
			
			msg.setFrom(new InternetAddress(parFrom));
			
			if (parSubject.length() != 0)
					msg.setSubject(parSubject);
			else
					msg.setSubject("No Subject");
	
			msg.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(parTo));
	
			if (parCc != null) { 
					msg.setRecipients(javax.mail.Message.RecipientType.CC, InternetAddress.parse(parCc));
			}
	
			if (parBcc != null) { 
					msg.setRecipients(javax.mail.Message.RecipientType.BCC, InternetAddress.parse(parBcc));
			}
	
			//msg.setContent(parMessage, "text/html");
			msg.setText(parMessage);
			
			Transport.send(msg);
			return true;
		}
		catch(Exception e){
			logger.error ("Send mail failed ! Error message: " + e.getMessage () );
			return false;
		}
	}
	
    public static void copyOCRFiles(String from, OCRParsedDataStruct ocrData, long idSearch, String toAdd){

    	String saveOcrFile = "";
    	try {
			saveOcrFile = rbc.getString("save.ocr.files").trim();
		} catch (Exception e) {}
		
		int saveOCRFile = 0;
		
		try {saveOCRFile  = Integer.parseInt(saveOcrFile);}
		catch(Exception e){}
		
    	if (saveOCRFile != 1)
    		return;

    	BigDecimal community = InstanceManager.getManager().getCurrentInstance(idSearch).getCurrentCommunity().getID();
    	
    	BigDecimal ocrCommunity = null; 
    	String ocrCommunityStr = null;
    	try {
			ocrCommunityStr = rbc.getString("ocr.community").trim();
		} catch (Exception e) {
		}
    	
		boolean inCommunity = false;
		String[] strCommunity = ocrCommunityStr.split(","); 
		for (int i = 0; i < strCommunity.length; i++){
			ocrCommunity = BigDecimal.valueOf(Long.parseLong(strCommunity[i].trim()));
			if (community.intValue() == ocrCommunity.intValue())
				inCommunity = true;
		}
		
		if (!inCommunity)
			return;
    	String saveOCRDir = "";
	
		try {
			saveOCRDir = rbc.getString("save.ocr.dir").trim();
		} catch (Exception e) {}

    	Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
	    
		String sToday = String.valueOf(calendar.get(Calendar.YEAR));
		sToday += "_" + String.valueOf(calendar.get(Calendar.MONTH) + 1);
		sToday += "_" + String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));

    	String destDir = BaseServlet.FILES_PATH + saveOCRDir + "/" + sToday;
    	File toDir = new File(destDir);
    	
    	if (!toDir.exists())
    		toDir.mkdirs();
    	
    	String file = "";
    	String pathFile = "";
    	File toFileExists = null;
    	
    	if (from != ""){
	    	file = idSearch + ".tiff";
	    	pathFile= destDir + "/" + file;
	    	
	    	toFileExists = new File(pathFile);
	    	if (toFileExists.isFile())
	    		return;
	    	
	    	try {
		        BufferedInputStream in = new BufferedInputStream(new FileInputStream(from));
		        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(pathFile));
		        
		        int b;
		        while( (b = in.read()) != -1) {
		          out.write(b);
		        }
		        
		        in.close();
		        out.close();
	    	} catch (Exception e) {
	    		return;
	    	}
    	}
    	
    	if (ocrData != null){
    		file = idSearch + ".xml";
    		pathFile= destDir + "/" + file;
    		
    		toFileExists = new File(pathFile);
    		if (toFileExists.isFile())
    			return;
    		
			try {
				Writer output = new BufferedWriter(new FileWriter(toFileExists));
				output.write(ocrData.getXmlContents());
				
				output.close();
			} catch (Exception e) {
				return;
			}    		
    	}
    	
    	if (toAdd != ""){
    		file = idSearch + ".xml";
    		pathFile= destDir + "/" + file;
    		
    		toFileExists = new File(pathFile);
    		
    		toFileExists = new File(pathFile);
    		if (!toFileExists.exists())
    			return;
    		
			try {
				String oldXML = ""; 
				FileInputStream fstream = new FileInputStream(pathFile);
				DataInputStream in = new DataInputStream(fstream);
				
				while (in.available() != 0){
					oldXML+= in.readLine();
				}
				String closeXml = oldXML.substring(oldXML.lastIndexOf("<"));
				oldXML = oldXML.substring(0, oldXML.lastIndexOf("<"));
				
				Writer output = new BufferedWriter(new FileWriter(toFileExists));

				output.write(oldXML.replaceAll("><", ">\n<") + "\n<ABSTRACTOR-DESCRIPTION>\n" + toAdd + "\n</ABSTRACTOR-DESCRIPTION>\n" + closeXml);
				
				output.close();
			} catch (Exception e) {
				return;
			}    		


    	}
    }

	public static boolean isValidForStartInterval(String date) {
		date = date.trim();
		 Pattern now_minus_number = Pattern.compile("^now(\\-[0-9]+)?$",Pattern.CASE_INSENSITIVE);
		 Matcher matcher1 = now_minus_number.matcher(date);
		 Pattern month_day_year = Pattern.compile("^((0|1)?[0-9])[/]([0-9]{1,2})[/]([0-9]{4})$");
		 Matcher matcher2 = month_day_year.matcher(date);
		 
		 return matcher1.matches() || matcher2.matches();
	}
	
	public static Date dateParser( String dateStr )
    {
        Date d = null;
        
        if( dateStr.equals( "N/A" ) )
        {
            //daca este N/A -> anul 1000 ca sa fie primul in lista
            dateStr = "1000";
        }
        
        try
        {
            d = df.parse( dateStr );
        }
        catch( Exception e )
        {
            //e.printStackTrace();
        }
        
        if( d == null )
        {
            try
            {
                d = df_year.parse( dateStr );
                Calendar cal = GregorianCalendar.getInstance();
                
                cal.setTime(d);
                cal.set( GregorianCalendar.MONTH, 0 );
                cal.set( GregorianCalendar.DAY_OF_MONTH, 1 );
                
                d = cal.getTime();
            }
            catch( Exception e )
            {
                //e.printStackTrace();
            }            
        }
        
        if( d == null )
        {
            try
            {
                d = df_month_year.parse( dateStr );
            }
            catch( Exception e )
            {
                //e.printStackTrace();
            }            
        }
        
        if(d == null)
        {
            logger.error( "Unparseable date: " + dateStr );
            d = new Date();
        }
        
        return d;
    }
   
    /**
     * 
     * @param dateStr
     * @return
     */
	public static Date dateParser2(String dateStr) {
		SimpleDateFormat sdfs[] = new SimpleDateFormat[]{
				new SimpleDateFormat("MM/dd/yy"),
				new SimpleDateFormat("MM/dd/yyyy"),
				new SimpleDateFormat("MMMMMMMM dd, yyyy"),
				new SimpleDateFormat("yyyy"),
				new SimpleDateFormat("MMM d, yyyy"),
				new SimpleDateFormat("MMMM d, yyyy"),
				new SimpleDateFormat("MMM, dd yyyy")
				};
		
		Date date = null;
		
		for(SimpleDateFormat sdf: sdfs){
			try{
				date = sdf.parse(dateStr);
				break;
			}catch(ParseException e){}
		}
		
		return date;
	}
    
	
	/**
     * 
     * @param dateStr
     * @return
     */
	public static Date dateParser3(String dateStr) {
		SimpleDateFormat sdfs[] = new SimpleDateFormat[]{
				new SimpleDateFormat("MM/dd/yy"),
				new SimpleDateFormat("MM-dd-yy"),
				new SimpleDateFormat("MM/dd/yyyy"),
				new SimpleDateFormat("MMMMMMMM dd, yyyy"),
				new SimpleDateFormat("MMM d, yyyy"),
				new SimpleDateFormat("MMMM d, yyyy"),
				new SimpleDateFormat("MMM, dd yyyy"),
				new SimpleDateFormat("yyyy-MM-dd"),
				new SimpleDateFormat("yyyy/MM/dd"),
				new SimpleDateFormat("yyyyMMdd"),
				new SimpleDateFormat("MMM d yyyy"),//Feb 10 1987
				};
		
		Date date = null;
		
		for(SimpleDateFormat sdf: sdfs){
			try{
				sdf.setLenient(false);
				date = sdf.parse(dateStr);
				break;
			}catch(ParseException e){}
		}
		
		return date;
	}
	
	public static boolean isValidDate(String dateStr) {
		try {
			Date date = dateParser3(dateStr);
			return true;
			
		} catch (Exception e) {
	        return false;
	    }
	}
	
	
    /**
     * Sets hh:mm:ss fields of a Date object
     * @param date initial date
     * @param newTime time string using "hh:mm:ss a" format
     * @return updated Date object
     */
    public static Date setTime(Date date, String newTime){    	
    	if(newTime.length() != 0){
        	try{    
        		Calendar dateCalendar = new GregorianCalendar();
        		dateCalendar.setTime(date);
        		Date time = new SimpleDateFormat("h:mm:ss a").parse(newTime);        		
        		Calendar timeCalendar = new GregorianCalendar();
        		timeCalendar.setTime(time);
        		dateCalendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
        		dateCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));        		
        		dateCalendar.set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND));        		
        		return dateCalendar.getTime();
        	}catch(Exception e){
        		//e.printStackTrace();
        	}
    	}
    	return date;
    }
    
    /* returns true if the parameter is a list of integers >=-1 separated by commas
     * and false otherwise */
    public static boolean isParameterValid(String parameter){
    	boolean valid = true;
    	String[] list = parameter.split(",");
    	for (String param: list) {
    		try {
    			int val = Integer.parseInt(param);
    			if (val<-1) {
    				return false;
    			}
    		} catch (NumberFormatException nfe) {
    			return false;
    		}
    	}
    	return valid;
    }
    
    public static void main(String[] args) {
		Date d = dateParser3("102-05-28");
		System.out.println(d);
	}
}
