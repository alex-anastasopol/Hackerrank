package ro.cst.tsearch.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.w3c.dom.Document;


public class Tidy {

	static final HashMap<String,String> tidyMapProps=new HashMap<String,String>();
    static
	{
    tidyMapProps.put("add-xml-decl","no");
    tidyMapProps.put("break-before-br","no");
    tidyMapProps.put("doctype" ,"omit");
    tidyMapProps.put("drop-empty-paras","no");
    tidyMapProps.put("wrap","0");
    tidyMapProps.put("show-body-only","yes");
    tidyMapProps.put("tidy-mark","no");
    tidyMapProps.put( "quote-ampersand", "no" );
    tidyMapProps.put("quiet","yes");
    tidyMapProps.put("show-warnings", "no");
    }
    
    public static String tidyParse(String strHtml,HashMap<String,String> propMap){
		
		if(strHtml!=null){
			ByteArrayInputStream inputStream=new ByteArrayInputStream (strHtml.getBytes());
			ByteArrayOutputStream outputStream=new ByteArrayOutputStream ();
			org.w3c.tidy.Tidy tidy=new org.w3c.tidy.Tidy();
			
			if(propMap==null){
				propMap = tidyMapProps;
			}
				Iterator it=propMap.keySet().iterator();
				while(it.hasNext()){
					Properties temProp=new Properties ();
					String key=(String)it.next();
					temProp.setProperty(key,propMap.get(key));
					tidy.setConfigurationFromProps(temProp);
				}
				
			tidy.parse(inputStream,outputStream);
			String rezultat=outputStream.toString();
			
			if(StringUtils.isStringBlank(rezultat)) {
				System.err.println(">>>>>>>>>> TIDY NU POATE CORECTA HTML-UL REVIN LA FORMA INITIALA ");
				return strHtml;
			}
			
			return rezultat;
		}
		
		return "";
	}
	
    public static Document tidyParse(String strHtml){
		
		if(strHtml!=null){
			ByteArrayInputStream inputStream=new ByteArrayInputStream (strHtml.getBytes());
			org.w3c.tidy.Tidy tidy=new org.w3c.tidy.Tidy();
			
			Iterator it=tidyMapProps.keySet().iterator();
				while(it.hasNext()){
					Properties temProp=new Properties ();
					String key=(String)it.next();
					temProp.setProperty(key,tidyMapProps.get(key));
					tidy.setConfigurationFromProps(temProp);
				}
				
			return tidy.parseDOM(inputStream,null);
		}
		
		return null;
	}
}
