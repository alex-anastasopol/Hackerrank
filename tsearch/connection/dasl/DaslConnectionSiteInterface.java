package ro.cst.tsearch.connection.dasl;

import java.util.Hashtable;
import java.util.Map;


public interface DaslConnectionSiteInterface {

	public static final int ORDER_ERROR = -1;

	public static final int ORDER_REJECTED = -2;

	public static final int ORDER_PLACED = 2;

	public static final int ORDER_COMPLETED = 6;

	
	public class DaslResponse implements Cloneable {				
		public int status = ORDER_ERROR;
		public int id = 0;
		public String certificationDate = "";
		public String xmlResponse = null;	
		public String xmlQuery = null;	
		public boolean isFake;
		private transient Hashtable<String, Object> attributes = new Hashtable<String, Object>();
		
		@Override
		public DaslResponse clone() {
			try{
				return ( (DaslResponse)super.clone() );
			}
			catch(CloneNotSupportedException e){
				throw new RuntimeException(e);
			}
		}
		
		public void setAttribute(String key, Object attribute) {
			if (key != null && attribute != null)
				attributes.put(key, attribute);
		}
		public Object getAttribute(String key) {
			return attributes.get(key);
		}
		public void clearAttributes() {
			attributes = new Hashtable<String, Object>();
		}
		
		public void setAttributes(Map<String, Object> extraParams) {
			if(extraParams != null) {
				for (String key : extraParams.keySet()) {
					setAttribute(key, extraParams.get(key));
				}
			}
		}
		public Hashtable<String, Object> getAttributes() {
			return attributes;
		}
	}		
	
	public abstract DaslResponse performSearch(String query, long searchId);

	/**
	 * Search for an image
	 * @param xmlQuery
	 * @return response or null if errors appeared
	 */
	public abstract String performImageSearch(String xmlQuery);
	
	public boolean continueSeach();

}