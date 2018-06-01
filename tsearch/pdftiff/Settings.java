package ro.cst.tsearch.pdftiff;

import java.util.ResourceBundle;


public class Settings 
{
	private static ResourceBundle rb = ResourceBundle.getBundle("ro.cst.tsearch.pdftiff.Settings");
	
	
	public static final String GS_COMMAND;
	public static final String HTMLDOC_COMMAND;
	
	
	static
	{
		GS_COMMAND = rb.getString("GS_COMMAND");
		
		HTMLDOC_COMMAND = rb.getString("HTMLDOC_COMMAND");
	}
	
}
