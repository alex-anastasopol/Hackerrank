package ro.cst.tsearch.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.util.SimpleNodeIterator;
import org.springframework.util.ObjectUtils;

import ro.cst.tsearch.parser.SimpleHtmlParser;


public class HttpUtils {

    public static Pattern inputPattern = Pattern.compile( "(?is)<input(.*?)>" );
    public static Pattern selectPattern = Pattern.compile( "(?is)<select(.*?)>(.*?)</select>" );
    public static Pattern optionPattern = Pattern.compile( "(?is)<option(.*?)>" );
    public static Pattern textareaPattern = Pattern.compile( "(?is)<textarea(.*?)>(.*?)</textarea>" );
    private static final HttpClient client = new HttpClient();
    public static Pattern namePattern = Pattern.compile( "(?is)name\\s*=\\s*([^\\s/]*)" );
    
    public static Pattern valuePatternWC = Pattern.compile( "(?is)value\\s*=\\s*[\"'](.*?)[\"']" );
    public static Pattern valuePatternWOC = Pattern.compile( "(?is)value\\s*=\\s*([^\\s]*)" );

    public static String[] monthArray = new String[]{ "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
    
    public static HashMap<String,String> getParamsFromLink(String link) {
		
    	HashMap<String,String> conparams = new HashMap<String,String>();
		String paramName = null , paramValue = null;
		
		int index = -1;
		if( (index=link.indexOf("?")) >0 ){
			link = link.substring( index +1 );
		}
		
		String[] params = link.split("&");
		
		for ( int i=0; i<params.length; i++ ) {
			index = params[i].indexOf("=");
			if (index == -1){
				continue;
			}
			paramName = params[i].substring(0, index);
			paramValue = params[i].substring(index + 1, params[i].length());
			
			conparams.put(paramName, paramValue);
		}
		return conparams;
	}
    
    
	public static HashMap<String,String> getConparamsFromString(String query, boolean isEncoded) {
		HashMap<String,String> conparams = new HashMap<String,String>();
		
		String paramName, paramValue;
		
		String[] params = query.split("&");
		for (int i = 0; i < params.length; i++) {
			
			int index = params[i].indexOf("=");
			
			if (index == -1)
				continue;
			
			paramName = params[i].substring(0, index);
			paramValue = params[i].substring(index + 1, params[i].length());
			
			if (isEncoded) {
				try {
					paramName = URLDecoder.decode(paramName, "UTF-8");
					paramValue = URLDecoder.decode(paramValue, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			
			conparams.put(paramName, paramValue);
		}
		return conparams;
	}
	
	public static String getConparamFromString(String query, boolean isEncoded, String paramName) {
		
		HashMap<String,String> params = getConparamsFromString( query, isEncoded );
		
		return params.get( paramName );
	}
    	
    public static Vector<String> getFormParamsOrdered( String formString )
    {
    	Vector<String> formParams = new Vector<String>();
        
        //search for inputs
        Matcher m = inputPattern.matcher( formString );
        Matcher nameMatcher = null;
        Matcher valueMatcher = null;
        
        String name = "";
        String value = "";
        
        
        while( m.find() )
        {
            String inputStr = m.group( 1 );
            name = "";
            value = "";
            
            nameMatcher = namePattern.matcher( inputStr );
            if( nameMatcher.find() )
            {
                name = cleanQuotes(nameMatcher.group( 1 ));
                
                valueMatcher = valuePatternWC.matcher( inputStr );
                if( valueMatcher.find() )
                {
                    value = cleanQuotes(valueMatcher.group( 1 ));
                }
                else {
	                valueMatcher = valuePatternWOC.matcher( inputStr );
	                if( valueMatcher.find() )
	                {
	                    value = cleanQuotes(valueMatcher.group( 1 ));
	                }
                }
                
                try
                {
                    value = URLEncoder.encode( value, "UTF-8" );
                }catch( Exception e ) {}
                
                formParams.add( name );
            }
        }
        
        //search for select inputs
        m = selectPattern.matcher( formString );
        while( m.find() )
        {
            String infoStr = m.group( 1 );
            String optionStr = m.group( 2 );
            
            name = "";
            value = "";
            
            nameMatcher = namePattern.matcher( infoStr );
            if( nameMatcher.find() )
            {
                name = cleanQuotes(nameMatcher.group( 1 ));
                
                valueMatcher = optionPattern.matcher( optionStr );
                while( valueMatcher.find() )
                {
                    String optionData = valueMatcher.group( 1 );
                    if( optionData.indexOf( "selected" ) >= 0 || optionData.indexOf( "SELECTED" ) >= 0 )
                    {
                        //found selected value
                    	
                    	valueMatcher = valuePatternWC.matcher( optionData );
                        if( valueMatcher.find() )
                        {
                            value = cleanQuotes(valueMatcher.group( 1 ));
                        }
                        else {
        	                valueMatcher = valuePatternWOC.matcher( optionData );
        	                if( valueMatcher.find() )
        	                {
        	                    value = cleanQuotes(valueMatcher.group( 1 ));
        	                }
                        }
                    }
                }
                
                formParams.add( name );
            }
        }
        
        //search for textarea inputs
        m = textareaPattern.matcher( formString );
        while( m.find() )
        {
            String infoStr = m.group( 1 );
            value = m.group( 2 );
            
            name = "";
            
            nameMatcher = namePattern.matcher( infoStr );
            
            if( nameMatcher.find() )
            {
                name = cleanQuotes(nameMatcher.group( 1 ));
                                
                formParams.add( name );
            }
        }
        return formParams;
    }
	
    public static HashMap<String, String> getFormParams(String formString, boolean utf8Encode){
        HashMap<String,String> formParams = new HashMap<String,String>();
        
        //search for inputs
        Matcher m = inputPattern.matcher( formString );
        Matcher nameMatcher = null;
        Matcher valueMatcher = null;
        
        String name = "";
        String value = "";
        
        
        while( m.find() )
        {
            String inputStr = m.group( 1 );
            name = "";
            value = "";
            
            nameMatcher = namePattern.matcher( inputStr );
            if( nameMatcher.find() )
            {
                name = cleanQuotes(nameMatcher.group( 1 ));
                
                valueMatcher = valuePatternWC.matcher( inputStr );
                if( valueMatcher.find() )
                {
                    value = cleanQuotes(valueMatcher.group( 1 ));
                }
                else {
	                valueMatcher = valuePatternWOC.matcher( inputStr );
	                if( valueMatcher.find() )
	                {
	                    value = cleanQuotes(valueMatcher.group( 1 ));
	                }
                }
                
                if( utf8Encode ) {
	                try
	                {
	                    value = URLEncoder.encode( value, "UTF-8" );
	                }catch( Exception e ) {}
                }
                formParams.put( name, value );
            }
        }
        
        //search for select inputs
        m = selectPattern.matcher( formString );
        while( m.find() )
        {
            String infoStr = m.group( 1 );
            String optionStr = m.group( 2 );
            
            name = "";
            value = "";
            
            nameMatcher = namePattern.matcher( infoStr );
            if( nameMatcher.find() )
            {
                name = cleanQuotes(nameMatcher.group( 1 ));
                
                valueMatcher = optionPattern.matcher( optionStr );
                while( valueMatcher.find() )
                {
                    String optionData = valueMatcher.group( 1 );
                    if( optionData.indexOf( "selected" ) >= 0 || optionData.indexOf( "SELECTED" ) >= 0 )
                    {
                        //found selected value
                    	
                    	valueMatcher = valuePatternWC.matcher( optionData );
                        if( valueMatcher.find() )
                        {
                            value = cleanQuotes(valueMatcher.group( 1 ));
                        }
                        else {
        	                valueMatcher = valuePatternWOC.matcher( optionData );
        	                if( valueMatcher.find() )
        	                {
        	                    value = cleanQuotes(valueMatcher.group( 1 ));
        	                }
                        }
                    }
                }
                
                formParams.put( name, value );
            }
        }
        
        //search for textarea inputs
        m = textareaPattern.matcher( formString );
        while( m.find() )
        {
            String infoStr = m.group( 1 );
            value = m.group( 2 );
            
            name = "";
            
            nameMatcher = namePattern.matcher( infoStr );
            
            if( nameMatcher.find() )
            {
                name = cleanQuotes(nameMatcher.group( 1 ));
                
                formParams.put( name, value );
            }
        }
        return formParams;
    }
    
    public static HashMap<String,String> getFormParams( String formString ) {
    	return getFormParams(formString, true);
    }

    public static String cleanQuotes(String str)
    {
        // problema in cazul in care valoarea unui input e de tipul value="     --    "
        try
        {
            char c = str.charAt( 0 );
            
            if( c == '\"' || c == '\'' )
            {
                str = str.substring( 1 );
            }
            
            c = str.charAt( str.length() - 1 );
    
            if( c == '\"' || c == '\'' )
            {
                str = str.substring( 0, str.length() - 1 );
            }
        }
        catch( Exception e )
        {
            //e.printStackTrace();
        }
        return str;
    }
    
    public static String getYearSelect( int startYear, int endYear, String paramName, String selectedValue )
    {
        String selectStr = "<select NAME=\"" + paramName + "\">";
        
        for( int i = startYear ; i <= endYear ; i ++ )
        {
            boolean selected = selectedValue.equals( "" + i );
            
            selectStr += "<option value=\"" + i + "\" " + ( selected ? "selected" : "" ) + " >" + i + "\n";
        }
        
        selectStr += "</select>";
        
        return selectStr;
    }
    
    public static String getMonthSelect( String selectedMonth, String paramName )
    {
        String selectStr = "<select NAME=\"" + paramName + "\">";

        for( int i = 0 ; i < monthArray.length ; i ++ )
        {
            selectStr += "<option value=\"" + monthArray[i] + "\" " + ( selectedMonth.equals( (i + 1) + "" ) ? "selected" : "" ) + " >" + monthArray[i] + "\n";
        }
        
        selectStr += "</select>";
        return selectStr;
    }
    
    public static String getDaySelect( String selectedDay, String paramName )
    {
        String selectStr = "<select NAME=\"" + paramName + "\">";

        for( int i = 1 ; i < 32 ; i ++ )
        {
            selectStr += "<option value=\"" + i + "\" " + ( selectedDay.equals( i + "" ) ? "selected" : "" ) + " >" + i + "\n";
        }
        
        selectStr += "</select>";
        return selectStr;
    }    
    
	public static byte[] downloadImage(String url) throws IOException{
			return downloadUrl(url);
	}
	
	public static String downloadPage(String url) throws IOException{
		return new String(downloadUrl(url));
	}
	
	public static byte[] downloadUrl(String url) throws IOException{
	GetMethod method = new GetMethod(url);
	method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));
	//logger.info(" _____ getImage ---  url= "+url);
	try {
		int statusCode = client.executeMethod(method);
	
		if (statusCode != HttpStatus.SC_OK) {
			System.err.println("Method failed: " + method.getStatusLine());
		}
		byte []b = method.getResponseBody();
		if( b.length>0 ){
			//logger.info(" _____ getImage ---  SUCCESS url= "+url +" size="+b.length);
		}
		return b;
	} catch (HttpException e) {
		//logger.error(ExceptionUtils.getFullStackTrace(e));
		System.err.println("Fatal protocol violation: " + e.getMessage());
		throw e;
	} catch (java.io.IOException e) {
		//logger.error(ExceptionUtils.getFullStackTrace(e));
		System.err.println("Fatal transport error: " + e.getMessage());
		throw e;
	} finally {
		method.releaseConnection();
	}
}	
	public static String buildNextLink(FormTag formTag) {
		SimpleNodeIterator elements = formTag.getFormInputs().elements();
		StringBuilder link = new StringBuilder();

		while (elements.hasMoreNodes()) {
			InputTag input = (InputTag) elements.nextNode();
			String nameAttribute = input.getAttribute("name");
			String valueAttribute = input.getAttribute("value");
			link.append(nameAttribute + "=" + URLEncoder.encode(ObjectUtils.nullSafeToString(valueAttribute)));
			link.append("&");
		}
//		link.insert(0, formTag.getAttribute("actionB") + "?");
		return link.toString();
	}
	
	public static Map<String,String> getInputsFromFormTag(FormTag formTag) {
		SimpleNodeIterator elements = formTag.getFormInputs().elements();
		StringBuilder link = new StringBuilder();
		HashMap<String, String> parameters = new HashMap<String,String>();
		while (elements.hasMoreNodes()) {
			InputTag input = (InputTag) elements.nextNode();
			String nameAttribute = input.getAttribute("name");
			String valueAttribute = StringUtils.defaultIfEmpty(input.getAttribute("value"),"");
			parameters.put(nameAttribute, URLEncoder.encode(ObjectUtils.nullSafeToString(valueAttribute)));
		}
		return parameters;
	}
	
	public static Map<String, String> isolateParams(String page, String form, String[] ADD_PARAM_NAMES){
		Map<String, String> params = new SimpleHtmlParser(page).getForm(form).getParams();
		Map<String,String> addParams = new HashMap<String,String>();
		for (String key : ADD_PARAM_NAMES) {
			String value = "";
			if (params.containsKey(key)) {
				value = params.get(key);
			}
			addParams.put(key, value);
		}
		
		return addParams;
	}

}