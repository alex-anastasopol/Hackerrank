package ro.cst.tsearch.utils;

import java.net.URLDecoder;
import java.util.*;
import java.util.regex.*;

public class ParamUtils
{
    public static HashMap getParamMap( String paramString, String paramSeparator )
    {
        
        if(paramString == null || paramString.equals( "" ) )
        {
            return null;
        }
        
        HashMap params = new HashMap();
        Pattern paramRegex = Pattern.compile( "(?s)(?i)([^=]*)=([^" + paramSeparator + "]*)" + paramSeparator + "?" );        
        Matcher m = paramRegex.matcher( paramString );
        
        while( m.find() )
        {
            String paramName = m.group( 1 );
            String paramValue = m.group( 2 );
            
            params.put( paramName, paramValue );
        }
        
        return params;
    }
    
    public static HashMap paramDiff( HashMap paramSet1, HashMap paramSet2 )
    {
        HashMap diff = new HashMap();
        
        if( paramSet1 == null )
        {
            return diff;
        }
        
        if( paramSet2 == null )
        {
            return paramSet1;
        }
        
        Iterator keys = paramSet1.keySet().iterator();
        while( keys.hasNext() )
        {
            String name = ( String ) keys.next();
            String value = (String) paramSet1.get( name );
            
            if( paramSet2.get( name ) == null )
            {
                diff.put( name, value );
            }
        }
        
        return diff;
    }
    
    public static HashMap valueDiff( HashMap paramSet1, HashMap paramSet2 )
    {
        HashMap valueDiff = new HashMap();
        
        if( paramSet1 != null && paramSet2 != null )
        {
            Iterator keys = paramSet1.keySet().iterator();
            while( keys.hasNext() )
            {
                String name = ( String ) keys.next();
                String value1 = (String) paramSet1.get( name );
                String value2 = (String) paramSet2.get( name );
                
                if( value2 != null )
                {
                    value1 = URLDecoder.decode( value1 );
                    value2 = URLDecoder.decode( value2 );
                    
                    if( !value2.equals( value1 ) )
                    {
                        String[] values = new String[2];
                        
                        values[0] = value1 ;
                        values[1] =  value2 ;
                        
                        valueDiff.put( name, values );
                    }
                }
            }
        }
        
        return valueDiff;
    }
}