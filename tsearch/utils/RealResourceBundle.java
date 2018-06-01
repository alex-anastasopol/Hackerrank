/*
 * Created on Jul 22, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author cozmin
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 * 
 */
////////  Bundles handled by this class are text files
// with the following structure:
// - lines begining with '#' char are ignored (comment lines)
// - lines that contain definitions are mapped key-value
//
// timeout =  20 
// => Bundle.getKey("timeout") => "20"
//  - both keys and values are trimmed  (noe leading or trailing whitespaces)
public class RealResourceBundle {
	private String path;
	private Map h=new TreeMap();
	public RealResourceBundle()
	{
		path="";
	}
	public static RealResourceBundle getBundle(String path)
	{
		RealResourceBundle rb=new RealResourceBundle();
		rb.setPath(path);
		try
		{
			File f=new File(path);
			BufferedReader br=new BufferedReader(new FileReader(f));
			Map m=new TreeMap();
			String line=null;
			while((line=br.readLine())!=null)
			{
				if(line.trim().length()>0)
				{
					String key=line.substring(0,line.indexOf("=")).trim();
					String value=line.substring(line.indexOf("=")+1).trim();
					m.put(key,value);
				}				
			}
			rb.setH(m);
			br.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return rb;			
	}
	/**
	 * @return
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param string
	 */
	private void setPath(String string) {
		path = string;
	}

	/**
	 * @param map
	 */
	private void setH(Map map) {
		h = map;
	}
	public String getKey(String k)
	{
		return h.get(k).toString();
	}
	public Set getKeys()
	{
		return h.keySet();
	}
}
