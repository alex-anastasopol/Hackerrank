package ro.cst.tsearch.searchsites.client;
import com.google.gwt.user.client.rpc.IsSerializable;

public class SearchSitesException extends Exception implements IsSerializable{
	
	   
	private static final long serialVersionUID = 415500647340753527L;
	
	private String msg;
	   
		public SearchSitesException()
	   {
	     super(); 
	   }
	   public SearchSitesException(String msg)
	   {
	     super(msg);
	     this.msg = msg;
	   } 
	   public SearchSitesException(Throwable cause,String msg)
	   {
		     super(cause);
		     this.msg = msg;
		}
	   public String getMessage()
	   {
	     return this.msg;
	   }
	
}