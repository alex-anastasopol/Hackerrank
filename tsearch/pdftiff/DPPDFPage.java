package ro.cst.tsearch.pdftiff;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import ro.cst.tsearch.pdftiff.util.Util;
import ro.cst.tsearch.processExecutor.client.ClientProcessExecutor;
import ro.cst.tsearch.utils.URLMaping;

public class DPPDFPage extends DPImagePage   
{
	private static final Logger logger = Logger.getLogger(DPPDFPage.class);
	
	String pdfFileName = null;
	String password = "";
	private boolean mustCleanUp = true;
	
	protected DPPDFPage( DP dp, int index, String fileName, int page, int platIndex )
	{		
		this(dp, index, fileName, page, platIndex, false);	
	}
	
	protected DPPDFPage( DP dp, int index, String fileName, int page, int platIndex, boolean tax )
	{		
		this.dp = dp;
		this.index = index;
		this.fileName = Util.tempFileName(dp.getOutputFolder(), "tiff");
		pdfFileName = fileName;	
		
		this.imageFormat = DP.IMAGE_TYPE_TIFF;
		
		this.idxImage = page;
		if (tax){
			this.gsOutputDevice = DP.GS_DEVICE_GRAY;
		}
		convertToTIFF();
		
		try
		{
			readInfo();
			
			if (isPlat())
			{
				this.platIndex = platIndex + 1;
			}
		}
		catch (Exception e)
		{
			
		}

	}
	
	protected DPPDFPage( DPPDFPage parent, int platIndex )
	{		
		this.dp = parent.dp;
		this.index = parent.index + 1;
		this.fileName = parent.fileName;
		this.pdfFileName = parent.pdfFileName;
		this.imageFormat = parent.imageFormat;
		this.idxImage = parent.idxImage + (platIndex == 0 ? 1 : 0);
		this.mustCleanUp = true;
		parent.mustCleanUp = false;
		try
		{
			readInfo();
			
			if (isPlat())
			{
				this.platIndex = platIndex + 1;
			}
		}
		catch (Exception e)
		{			
		}
	}	
	
	
	protected Object next()
	{
		if (next == null)
		{
			//logger.info("plat:" + isPlat() + " index:" + platIndex);
			
			if (isPlat() && platIndex == 1)
				next = new DPPDFPage( this, 1);
			else
			if (idxImage != numImages - 1)
				next = new DPPDFPage( this, 0 );
		}
		
		return next;
	}	
	
	public void convertToTIFF()
	{
		if ( width == -1 )
		{
			xDPI = 300;
			yDPI = 300;
			final ResourceBundle rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
			String gsCommand = rbc.getString("GS.CommandString").trim();
			//String gsCommand = Settings.GS_COMMAND;
			try
			{
				//convert pdf to tiff
				
				String[] exec = {
						gsCommand,
					"-sPDFPassword=" + password,
					"-sDEVICE=" + gsOutputDevice,
					"-sPAPERSIZE=letter",
					"-dFIXEDMEDIA",
					"-dPDFFitPage",
					"-r500",
					"-dNOPAUSE",
					"-sOutputFile=" + fileName,
					pdfFileName,
					"-dBATCH"};
	
                ClientProcessExecutor cpe = new ClientProcessExecutor( exec, true, true);
                cpe.start();
	
				int k = cpe.getReturnValue();
                			
				super.readData(); 
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void cleanUp()
	{
		if ( mustCleanUp )
			new File( fileName ).delete();		
	}
	
	
	public String getPassword() {
		return password;
	}
	
	
	/**
	 * @param password
	 * <br><br>
	 * seteaza parola pdf-ului
	 */
	public void setPassword(String password) {
		this.password = password;
	}	
}
