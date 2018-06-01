package ro.cst.tsearch;

import java.io.FileOutputStream;
import java.util.Hashtable;

import ro.cst.tsearch.utils.Log;

public class Logme {
    
    private static Hashtable map = new Hashtable();
    
    private Logme() {}
    
    public static void log(String log, Object key) {
        
        String text = (String) map.get(key);
        if (text == null)
            text = new String();
        
        map.put(key, text + log);
        
    }

    public static boolean flush(Object key) {
        
        try {
            
	        FileOutputStream fos = new FileOutputStream("log" + System.currentTimeMillis());
	        
	        String text = (String) map.get(key);
	        
	        fos.write(text.getBytes());
	        fos.flush();
	        fos.close();
	        
	        map.remove(key);
	        
        } catch (Exception e) {
            e.printStackTrace();
            
            return false;
        }
        
        return true;
    }
    
    public static void email (String to, Object key) {
        
        String text = (String) map.get(key);
        Log.sendEmail(to, text);
        map.remove(key);
        
    }
}
