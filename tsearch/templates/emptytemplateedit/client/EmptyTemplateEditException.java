package ro.cst.tsearch.templates.emptytemplateedit.client;

import com.google.gwt.user.client.rpc.IsSerializable;

public class EmptyTemplateEditException  extends Exception  implements IsSerializable{


	private static final long serialVersionUID = 116957033312148440L;
	
	private String msg;
	   
	public EmptyTemplateEditException()
	{
	     super(); 
	}
	   
	   public EmptyTemplateEditException(String msg)
	   {
	     super(msg);
	     this.msg = msg;
	   }
	   
	   public String getMessage()
	   {
	     return this.msg;
	   }
	
	
}
