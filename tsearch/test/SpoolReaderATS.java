package ro.cst.tsearch.test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Enumeration;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.CharConverter;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.PrintObject;
import com.ibm.as400.access.PrintObjectTransformedInputStream;
import com.ibm.as400.access.PrintParameterList;
import com.ibm.as400.access.ProgramCall;
import com.ibm.as400.access.SpooledFile;
import com.ibm.as400.access.SpooledFileList;

import de.schlichtherle.io.FileOutputStream;

public class SpoolReaderATS {
	
	public static void main(String[] args) {
		
		SpoolReaderATS spoolReaderATS = new SpoolReaderATS();
		spoolReaderATS.downloadAll();
		
		if(true) {
			return;
		}
		try {
			PrintStream dw = new java.io.PrintStream(new FileOutputStream("D://gogu.txt"));
			
			// Create an AS400 object.  The system name was passed
	         // as the first command line argument.
	         AS400 system = new AS400 ("134.127.16.1");

	         String splfName = "TM4701AO";
	         int splfNumber = 12;
	         String _jobName = "QPRTJOB";
	         String _jobUser = "PITESTSW";
	         String _jobNumber = "997050";

	         /*
	         SpooledFile splF = new SpooledFile(system,
	         splfName,
	         splfNumber,
	         _jobName,
	         _jobUser,
	         //"014343");
	         _jobNumber);
	         */
	         system.setUserId("STEWART");
	         system.setPassword("CELL@GOAT");
	         
	         system.connectService(AS400.PRINT);
	         
	         SpooledFile splF = new SpooledFile(system,
	                 "TM4701AO", //splfName,
	                 12, //splfNumber,
	                 "QPRTJOB",
	                 _jobUser,
	                 "997050");
	                 //_jobNumber);

	         PrintParameterList printParms = new PrintParameterList();
	         printParms.setParameter(PrintObject.ATTR_WORKSTATION_CUST_OBJECT,
	         "/QSYS.LIB/QWPDEFAULT.WSCST");
	         printParms.setParameter(PrintObject.ATTR_MFGTYPE, "*WSCST");

	         // get the text (via a transformed input stream) from the spooled file
	         PrintObjectTransformedInputStream inStream = splF.getTransformedInputStream(printParms);
			
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void downloadAll(){
		SpooledFileList spfList = null;
		try {
			PrintParameterList prtParam = new PrintParameterList();
			
			//prtParam.setParameter(PrintObject.ATTR_SCS2ASCII, "*YES");
			//prtParam.setParameter(PrintObject.ATTR_3812SCS, "*YES");
			prtParam.setParameter(PrintObject.ATTR_MFGTYPE, "*WSCST");
			prtParam.setParameter(PrintObject.ATTR_WORKSTATION_CUST_OBJECT, "/QSYS.LIB/QWPDEFAULT.WSCST");
			//prtParam.setParameter(PrintObject.ATTR_RPLUNPRT, "*YES");
			//prtParam.setParameter(PrintObject.ATTR_RPLCHAR, "+");
			//prtParam.setParameter(PrintObject.ATTR_DATA_QUEUE, "/QSYS.LIB/QUSRSYS.LIB/PSWATS2.DTAQ");
			//prtParam.setParameter(PrintObject.ATTR_DESTINATION, "*OTHER");
			
			//prtParam.setParameter(PrintObject.ATTR_ASCIITRANS, "*YES");
			
			//prtParam.setParameter(PrintObject.ATTR_WORKSTATION_CUST_OBJECT, "/QSYS.LIB/QWPDEFAULT.WSCST");
			//prtParam.setParameter(PrintObject.ATTR_MFGTYPE, "*WSCSTCONT132");
			
			AS400 conMain = new AS400("134.127.16.1", "STEWART", "CELL@GOAT");
			
			CharConverter conv=new CharConverter(conMain.getCcsid());
			
			spfList = new SpooledFileList(conMain);
			spfList.setQueueFilter("/QSYS.LIB/QUSRSYS.LIB/PSWATS2.OUTQ");
			spfList.setUserFilter("*ALL");
			spfList.openSynchronously();
			Enumeration<SpooledFile> spfEnum = spfList.getObjects();
			while (spfEnum.hasMoreElements())
			{
			   SpooledFile spfMain = (SpooledFile)spfEnum.nextElement();
			   if (spfMain != null)
			   {
			      String strSpfFName =
			      spfMain.getStringAttribute(SpooledFile.ATTR_SPOOLFILE);
			      PrintObjectTransformedInputStream tisIn = null;
			      InputStream is = null;
			      InputStreamReader inp = null; 
			      
			      
			      byte[] buf = new byte[32767];
			      StringBuffer sbuf = new StringBuffer();
			      int bytesRead = 0;
			      try
			      {
			         try
			         {
			        	 //spfMain.getStringAttribute(PrintObject.ATTR_SCS2ASCII);;
			        	 //spfMain.getPageInputStream(prtParam);
			        	 is = spfMain.getInputStream();
			        	 //tisIn = spfMain.getTransformedInputStream(prtParam);
			        	 /*inp = new InputStreamReader(is,"Cp037");
			        	 BufferedReader reader = new BufferedReader(inp);
					      String output1 = reader.readLine();
			        	 System.out.println(output1);*/
			        	 
			        	 byte[] byteArr = new byte[is.available()];

			        	 is.read(byteArr); 

			        	 String output1 = new String(byteArr,"Cp037");
			        	 System.out.println(output1);
			            do
			            {
			               bytesRead = tisIn.read(buf);
			               if (bytesRead > 0)
			               {
			                  System.out.println("\tbytesRead: " + String.valueOf(bytesRead));
			                  String strTmp = new String(buf, 0, bytesRead);
			                  if (bytesRead != 1)
			                     sbuf.append(strTmp);
			               }
			            } while (bytesRead != -1);
			      
			            String output = sbuf.toString();
			            FileWriter fwriter = new FileWriter("D:\\print\\" + strSpfFName  + spfMain.getJobNumber() + "_2.pdf", true);
			            fwriter.write(output);
			            fwriter.close();
			         }
			         finally
			         {
			            if (tisIn != null)
			               tisIn.close();
			         }
			      }
			      catch (Exception e)
			      {
			         e.printStackTrace();
			      }
			   }
			}
			
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		spfList.close();
		
	}

}
