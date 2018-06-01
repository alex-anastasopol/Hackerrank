package ro.cst.tsearch.pdftiff;

import java.io.File;





import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
//import ro.cst.tsearch.pdftiff.util.Chronometer;
import org.apache.log4j.Logger;

import com.stewart.ats.base.document.ImageI.IType;

public class Test 
{	
	private static final Logger logger = Logger.getLogger(Test.class);
	
	public static void main(String args[]) throws Exception 
	{
		
		transformFolder("\\\\atsserver\\tiffs", "\\\\atsserver\\pdfs");
		
		//Chronometer c_all = new Chronometer();
//		DP dp = new DP(	"output", "iesire");
//		
//		File ff = new File("\\\\atsserver\\tiffs");
//		File[] f = ff.listFiles();
//		
//		//dp.seek(3);
//		
//		for (int i = 0; i < f.length; i++)
//		{
//			
//			DPPage p = dp.appendImage( DP.IMAGE_TYPE_TIFF, f[i].getPath(), false );
//			p.setDocumentName("document " + i);
//
//		}
//		
//		
//		
//		DPPage p = dp.appendImage( DP.IMAGE_TYPE_TIFF, "c:\\test\\197605124860101.tif", true );
//		p.setDocumentName("un plat");
//		DPPage pp = dp.appendImage( DP.IMAGE_TYPE_TIFF, "c:\\test\\197605124860101-landscape.tif", true );
//		pp.setDocumentName("un plat");

		
		/*p = dp.appendPDF( "test4.ps" );
		p.setDocumentName("un ps");
		//dp.seek(0);


		
		
		
		
		//aici ziceam ca bagam htmlu
		File html = new java.io.File( "index.html" );
		PrintWriter pw = new PrintWriter(new java.io.FileOutputStream(html));
		pw.print("<table border='1' width='95%' style='margin:30px;'>");
		pw.print( "<tr height='40'><td>fisier</td><td>pagina</td></tr>" );
		for ( int i = 0; i < dp.getPageCount(); i++ )
		{
			DPPage dpp = dp.getPage(i);
			if ( dpp.isMain() )
			{
				pw.print( "<tr  height='100'><td>" + dpp.getDocumentName() + "</td><td>@@" + dpp.getDocumentName() + "@@</td></tr>" );
			}			
		}
		pw.print( "</table>" );
		pw.close();
		
		int htmlPageCount = Util.evalPageCount(dp, "index.html");
		
		dp.shift( htmlPageCount );
		dp.seek(0);
		
		
		StringBuffer sbFile = new StringBuffer();
		BufferedReader br = new BufferedReader(new FileReader( html ));
		String line;
		while ( (line=br.readLine())!=null )
		{
			sbFile.append(line);
		}
		br.close();

		String sFile = sbFile.toString();
				
		for ( int i = htmlPageCount; i < dp.getPageCount(); i++ )
		{
			DPPage dpp = dp.getPage(i);
			if ( dpp.isMain() )
			{
				sFile = sFile.replaceAll("@@" + dpp.getDocumentName() + "@@", String.valueOf(dpp.getIndex() + 1) );
			}			
		}
		
		pw = new PrintWriter(new java.io.FileOutputStream(html));
		pw.print(sFile);
		pw.close();
		
		
		dp.appendHTML("index.html");		*/
		
//		dp.process(-1);
		
		//c_all.stop();
		
		//logger.info( "Timp total " + c_all.get()/1000 + "sec.");
	}
	
	public static void transformFolder(String inputFolderName, String outputFolderName) {
		File inputFolder = new File(inputFolderName);
		if(!inputFolder.isDirectory()) {
			System.err.println("Input folder " + inputFolderName + " is not a directory");
			return;
		}
		File[] listFiles = inputFolder.listFiles();
		System.out.println("Found " + listFiles.length + " in " + inputFolderName);
		for (File file : listFiles) {
			DP dp = new DP(	FilenameUtils.getBaseName(file.getName()), outputFolderName);
			dp.setOutputType(IType.PDF);
			dp.appendImage(DP.IMAGE_TYPE_TIFF, file.getPath(), false);
			String process = dp.process(-1);
			FileUtils.deleteQuietly(new File(process));
			
		}
	}
	
	
}
