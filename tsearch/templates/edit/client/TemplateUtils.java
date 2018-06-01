package ro.cst.tsearch.templates.edit.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TemplateUtils {
	
	private static final long serialVersionUID = 1021859831156566284L;
	
	public static String   LOADING_TEMPLATE_TEXT = "<center><br/><br/><br/><br/><br/><b>LOADING ... </b></center>";
	public static String EMPTY_SPACES = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
	public static final int 	SEND_BY_MAIL=0, VIEW_PDF=1;
	
	public static final int NR_COLS_IN_EDITOR_FOR_PXT = 90;
	public static final int NR_COLS_IN_EDITOR_FOR_ATS = 70;
	
	public static final int DIM_LARGE_TEXT_AREA = 30;
	
	public static final int DIM_SMALL_TEXT_AREA = 3;
	
	public static String editorLineSeparator ="\n";
	
	public static List  dataFromBoilerPlates=null;
	
	public static final String DASL_CODE = "DASL_";

	public  static class TemplateExtensions{
		
		public static String ATS_SMALL_EXTENSION = "ats";
		public static String ATS_BIG_EXTENSION = "ATS";
		
		public static String PXT_SMALL_EXTENSION = "pxt";
		public static String PXT_BIG_EXTENSION = "PXT";
		
		public static String TSD_SMALL_EXTENSION = "html";
		public static String TSD_BIG_EXTENSION = "HTML";
		
	}
	

	public static final String []elementEditorExtension ={
		TemplateExtensions.ATS_SMALL_EXTENSION,
		TemplateExtensions.ATS_BIG_EXTENSION,
		TemplateExtensions.PXT_SMALL_EXTENSION,
		TemplateExtensions.PXT_BIG_EXTENSION,
		""
		};
	
	
	public static String getFileExtension(String newpath){
		String ret="";
		int poz = newpath.lastIndexOf( "." );
		if( poz>0){
			ret = newpath.substring(
					poz+1 , newpath.length()
					);
		}		
		return ret;
	}

	public static final HashMap  aimEscapeCharacters=new HashMap ();
	static {
		aimEscapeCharacters.put("'", "&apos;");
//		aimEscapeCharacters.put("“", "&ldquo;");
//		aimEscapeCharacters.put("”", "&rdquo;");
		aimEscapeCharacters.put("<", "&lt;");
		aimEscapeCharacters.put(">", "&gt;");
		aimEscapeCharacters.put("\"", "&quot;");
		aimEscapeCharacters.put("" + ((char) 186), "&#176;");// "º"
		aimEscapeCharacters.put("" + ((char) 176), "&#176;"); // '°'
		aimEscapeCharacters.put("" + ((char) 146), "&apos;");// ’
	}
	
	static final HashMap aimEscapeCharactersInvert=new HashMap ();
	static {
		aimEscapeCharactersInvert.put("&apos;", "'");
//		aimEscapeCharactersInvert.put("&ldquo;", "“");
//		aimEscapeCharactersInvert.put("&rdquo;", "”");
		aimEscapeCharactersInvert.put("&lt;", "<");
		aimEscapeCharactersInvert.put("&gt;", ">");
		aimEscapeCharactersInvert.put("&quot;", "\"");
		aimEscapeCharactersInvert.put("&#176;", "" + ((char) 186)); // "º"
		aimEscapeCharactersInvert.put("&#176;", "" + ((char) 176)); // "°"
		aimEscapeCharactersInvert.put("&amp;", "&");
	}
	
	public static String revertStringFromHtmlEncoding(String str){
		
		HashMap map=TemplateUtils.aimEscapeCharactersInvert;
		
		Iterator it=map.keySet().iterator();
		if(str!=null){
			while(it.hasNext()){
				String strnext=(String)it.next();
				str=str.replaceAll(strnext,(String)map.get(strnext));
				
			}
		}
		return str;
		
	}
	
	public static boolean isForEditorElement(String newpath){
		boolean ret = false;
		String ext = getFileExtension(newpath);
		
		for(int i=0;i<TemplateUtils.elementEditorExtension.length;i++){
			if(TemplateUtils.elementEditorExtension[i].equals(ext)){
				ret = true;
				break;
			}	
		}
		
		return ret;
	}	
	
	public static String replaceAmpForAIM(String input){
		StringBuffer ret = new StringBuffer(input);
		
		String aux ="";
		int i=-1,len=0;
		while(true){
			i++;
			i = ret.indexOf("&",i);
			if(i==-1)
				break;
			len = ret.length();
			if(i+1+3 > len){
				ret.insert(i+1,"amp;");
				continue;
			}
			aux = ret.substring(i+1, i+1+3);
			if(aux.equalsIgnoreCase("lt;") || aux.equalsIgnoreCase("gt;")){
				continue;
			}
			if(i+1+4 > len){
				ret.insert(i+1,"amp;");
				continue;
			}
			aux = ret.substring(i+1, i+1+4);
			if(aux.equalsIgnoreCase("amp;"))
				continue;
			if(i+1+5 > len){
				ret.insert(i+1,"amp;");
				continue;
			}
			aux = ret.substring(i+1, i+1+5);
			if(
					 aux.equalsIgnoreCase("quot;") || 
					 aux.equalsIgnoreCase("apos;") || 
					 aux.equalsIgnoreCase("#176;")
					 )
				continue;
			//daca s-a ajuns aici nu s-a facut match
			ret.insert(i+1,"amp;");
		}
		return ret.toString();
	}
	
	
public static String cleanStringForAIM(String str,boolean andTest, boolean ignoreEscaped){
		
		HashMap map=TemplateUtils.aimEscapeCharacters;
		if(andTest) {
			str = replaceAmpForAIM(str);
		}
		Iterator it=map.keySet().iterator();
		if(str!=null){
			if(ignoreEscaped){
				while(it.hasNext()){
					String strnext=(String)it.next();
					str=cleanButIgnoreEscaped(str,strnext,(String)map.get(strnext));
				}
			}
			else{
				while(it.hasNext()){
					String strnext=(String)it.next();
					str=str.replaceAll(strnext,(String)map.get(strnext));
				}
			}
		}
		return str;
}

public static String cleanStringForAIMNew(String str,boolean andTest, boolean ignoreEscaped){
	
	if(andTest) {
		str = replaceAmpForAIM(str);
	}
		
	for(Entry<String,String> chr : ((Map<String,String>) TemplateUtils.aimEscapeCharacters).entrySet()) {
		str = cleanNew(str, chr.getKey(), chr.getValue(), ignoreEscaped);
	}

	return str;
}

private static String cleanButIgnoreEscaped(String strUnclean,String strnext,String replaceValue){
	
	String ret = "";
	boolean haveEscapedCharacters = true;
	String last		=strUnclean;
		
	while(haveEscapedCharacters){
		int start 	= last.indexOf("[");
		int end 	= last.indexOf("]");
		int size    = last.length();
		String first 	="";
		String character ="";
		
		if( start>=0 && end>0 && start+2==end ){//we have escaped characters
			character = last.substring(start,end+1);
			first = last.substring(0,start);
			last = last.substring(end+1,size);
			ret = ret + first.replaceAll(strnext, replaceValue)+ character;
		}
		else if(start>=0){//we have [ but not a escaped character
			first = last.substring(0,start);
			if( start<size-1 ){
				last = last.substring(start+1,size);			
			}
			else{
				last="";
			}
			character ="[";
			ret = ret + first.replaceAll(strnext, replaceValue)+ character;
		}
		else{
			ret = ret + last.replaceAll(strnext,replaceValue);
			haveEscapedCharacters = false; //stop the cicle
		}
	}
	
	return ret;
}

public static String cleanNew(String strUnclean,String strnext,String replaceValue, boolean ignoreEscaped) {
		
	if(ignoreEscaped) {
		strUnclean = strUnclean .replaceAll("\\["+strnext+"\\]",serialVersionUID+"1")
								.replaceAll("\\\\"+strnext,serialVersionUID+"2");
	}
	
	strUnclean = strUnclean.replaceAll(strnext, replaceValue);
	
	if(ignoreEscaped) {
		strUnclean = strUnclean .replaceAll(serialVersionUID+"1","\\["+strnext+"\\]")
								.replaceAll(serialVersionUID+"2",strnext);
	}
	
	return strUnclean;
	
}

public static void main(String[] args) {
	//cleanButIgnoreEscaped("da\" nu[\"]", "\"" , "&apos;" );
	//cleanButIgnoreEscaped("\" nu va fi [\"] nu va fi[\"]va fi\" nu are elemente" , "\"" , "&apos;" );
	System.out.println(cleanButIgnoreEscaped("\" test \\\" [ nu va fi [\"] nu va fi[\"]va fi\" nu are elemente" , "\"" , "&apos;" ));
	System.out.println(cleanNew("\" test \\\" [ nu va fi [\"] nu va fi[\"]va fi\" nu are elemente" , "\"" , "&apos;" ,true));
	//System.out.println(cleanStringForAIM("<PartyFirstName>[<]!--%$FIRST$%--[>]",true , true));
	//System.out.println(Double.parseDouble("123,456.78"));
}
		
}
