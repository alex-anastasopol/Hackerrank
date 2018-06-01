package ro.cst.tsearch.test;

import java.awt.geom.AffineTransform;
import java.io.FileOutputStream;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

public class TestPdf {
	public static void main(String[] args) {
		try {
			PdfReader reader = new PdfReader("D:\\R83499.pdf");
			Rectangle rectangle = PageSize.A4;
			Document document = new Document(rectangle, 0,0,0,0);
			PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("D:\\R83499_1.pdf"));
			document.open();

			PdfContentByte cb = writer.getDirectContent();
			
			
			
			for (int i = 1; i <= reader.getNumberOfPages(); i++) {
				 //cb.transform(AffineTransform.getTranslateInstance( 1, 1)) ;
				 //cb.concatCTM(1f, 0f, 0f, -1f, 0f, PageSize.A4.getHeight());
				 // put the page
				 PdfImportedPage page = writer.getImportedPage(reader,i);
				 double sx = rectangle.getWidth() / reader.getPageSize(i).getWidth();
				 double sy = rectangle.getHeight() / reader.getPageSize(i).getHeight();
				 cb.transform(AffineTransform.getScaleInstance(sx, sy)) ;
				 cb.addTemplate(page, 1, 0, 0, 1, 0, 0);
				 document.newPage();
			}
	
			//document.add(new Paragraph("Hello World"));
			document.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
