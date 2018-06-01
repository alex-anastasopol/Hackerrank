package ro.cst.tsearch.templates.tsdedit.client;

import com.google.gwt.user.client.rpc.IsSerializable;

public class TSDTemplateSearchPermisionException extends Exception implements IsSerializable{
	
	
	private static final long serialVersionUID = 2972958762187536429L;
	
	   private String msg;
	   public TSDTemplateSearchPermisionException()
	   {
	     super(); 
	   }
	   public TSDTemplateSearchPermisionException(String msg)
	   {
	     super(msg);
	     this.msg = msg;
	   }
	   public String getMessage()
	   {
	     return this.msg;
	   }
	
}

