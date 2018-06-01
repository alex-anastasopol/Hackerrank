package ro.cst.tsearch.templates.edit.client;
import com.google.gwt.user.client.rpc.IsSerializable;

public class TemplateSearchPermisionException extends Exception implements IsSerializable{
	
	
	private static final long serialVersionUID = 2972958762187536429L;
	
	   private String msg;
	   public TemplateSearchPermisionException()
	   {
	     super(); 
	   }
	   public TemplateSearchPermisionException(String msg)
	   {
	     super(msg);
	     this.msg = msg;
	   }
	   public String getMessage()
	   {
	     return this.msg;
	   }
	
}
