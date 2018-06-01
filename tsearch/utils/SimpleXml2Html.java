package ro.cst.tsearch.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.regex.Pattern;

public class SimpleXml2Html {
	   // -------------------------------------------------------------------------------------------------------------------------------------

	   public static void main(String[] args) throws IOException {
	      if (args.length != 2) {
	         System.out.println("\nUsage: java SimpleXmlToHtml <input XML file> <output HTML file>\n");
	         System.exit(2);
	      }

	      SimpleXml2Html x2h = new SimpleXml2Html();

	      // Get the contents of the XML file
	      String xml = x2h.getXmlContent(args[0]);
	      if (xml == null) System.exit(2);   // file not found

	      // Looks like a valid XML file?
	      if (!x2h.isValidXml(xml)) {
	         System.out.println("\n*** Invalid XML header ! ***\n");
	         System.exit(2);
	      }

	      // Convert it to HTML...
	      String html = SimpleXml2Html.xmlToHtml(xml);

	      // ...and save the outcome
	      x2h.saveHtmlContent(args[1], html);
	   }

	   // -------------------------------------------------------------------------------------------------------------------------------------

	   public static void translate(InputStream in, OutputStream out) throws IOException {
	      SimpleXml2Html x2h = new SimpleXml2Html();

	      // Read XML stream
	      String xml = x2h.getXmlContent(in);

	      // Looks like valid XML data?
	      if (!x2h.isValidXml(xml)) return;

	      // Convert it to HTML...
	      String html = SimpleXml2Html.xmlToHtml(xml);

	      // ...and save the outcome
	      x2h.saveHtmlContent(out, html);

	      // Close both streams
	      in.close();
	      out.close();
	   }

	   // -------------------------------------------------------------------------------------------------------------------------------------

	   private String getXmlContent(String inputFile) throws IOException {
	      BufferedReader fromFile;
	      try {
	         fromFile = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8"));
	      } catch (FileNotFoundException e) {
	         System.out.println("\n*** File not found: " + e.getMessage() + " ***\n");
	         return null;
	      }

	      StringBuffer buf = new StringBuffer(10000);
	      String xml;
	      while ((xml = fromFile.readLine()) != null) buf = buf.append(lineTransform(xml) + "\n");

	      fromFile.close();

	      return buf.toString();
	   }

	   // -------------------------------------------------------------------------------------------------------------------------------------

	   private String getXmlContent(InputStream in) throws IOException {
	      BufferedReader fromStream = new BufferedReader(new InputStreamReader(in, "UTF-8"));

	      StringBuffer buf = new StringBuffer(10000);
	      String xml;
	      while ((xml = fromStream.readLine()) != null) buf = buf.append(lineTransform(xml) + "\n");

	      fromStream.close();

	      return buf.toString();
	   }

	   // -------------------------------------------------------------------------------------------------------------------------------------

	   private void saveHtmlContent(String fileName, String html) throws IOException {
	      PrintWriter ofp = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
	      ofp.write(html, 0, html.length());
	      ofp.close();
	   }

	   // -------------------------------------------------------------------------------------------------------------------------------------

	   private void saveHtmlContent(OutputStream out, String html) throws IOException {
	      PrintWriter ofp = new PrintWriter(new BufferedOutputStream(out));
	      ofp.write(html, 0, html.length());
	      ofp.close();
	   }

	   // -------------------------------------------------------------------------------------------------------------------------------------

	   private boolean isValidXml(String s) {
	      return s.startsWith("<?xml version=");
	   }

	   // -------------------------------------------------------------------------------------------------------------------------------------

	   private static String unescape(String s) {
	      s = s.replace("&amp;#11;", "<br>");
	      s = s.replace("&lt;", "<");
	      s = s.replace("&gt;", ">");
	      s = s.replace("&amp;", "&");
	      s = s.replace("&apos;", "'");
	      s = s.replace("&quot;", "\"");
	      return s;
	   }

	   // -------------------------------------------------------------------------------------------------------------------------------------
	/*
	   private String getParentTag(String xml, int position) {
	      int i, j = position;
	      boolean skipNext = false;
	      for (i = position - 1; i >= 0; i--) {
	         if (xml.charAt(i) == '/' && xml.charAt(i + 1) == '>') skipNext = true;
	         else
	         if (xml.charAt(i) == '<' && xml.charAt(i + 1) == '/') skipNext = true;
	         else
	         if (xml.charAt(i) == '>' && !skipNext) j = i;
	         else
	         if (xml.charAt(i) == '<' && !skipNext) return xml.substring(i + 1, j);
	         else
	         if (xml.charAt(i) == '<' && skipNext) skipNext = false;
	      }
	      return "";
	   }
	*/
	   // -------------------------------------------------------------------------------------------------------------------------------------

	   private final Pattern singleLineTagPairPattern = Pattern.compile("^\\s*<(\\w+)>.+</(\\1)>\\s*$");
	   
	   private String lineTransform(String xml) {
		  if (!singleLineTagPairPattern.matcher(xml).matches()) return xml;

		  int i1 = xml.indexOf("<");
	      if (i1 == -1) return xml;
	      int i2 = xml.indexOf(">", i1);
	      if (i2 == -1) return xml;
	      String tag = xml.substring(i1 + 1, i2);
	      if (tag.equals("type") || tag.equals("text") || tag.equals("link") || tag.equals("url") || tag.equals("freeForm")) return xml;
	      int j1 = xml.indexOf("</", i2);
	      if (j1 == -1) return xml;
	      int j2 = xml.indexOf(">", j1);
	      if (j2 == -1) return xml;
	      String end = xml.substring(j1 + 2, j2);
	      if (!end.equals(tag)) return xml;
	      String text = xml.substring(i2 + 1, j1);
	      if (text.equals("")) return xml;

	      if (tag.equals("abbreviation")) tag = "state";

	      return xml.substring(0, i1) + "<--i>" + tag + "<--/i>: " + text + xml.substring(j2 + 1, xml.length());
	   }

	   // -------------------------------------------------------------------------------------------------------------------------------------

	   private static String blockTransform(String xml, String tag, String title) {
	      int start = 0;
	      while (true) {
	         int i = xml.indexOf("<" + tag + ">", start);
	         if (i == -1) break;
	         int j = xml.indexOf("</" + tag + ">", i);
	         if (j == -1) break;
	         String html = xml.substring(i + tag.length() + 2, j);

	         html = html.replaceAll("<\\w+>", "");
	         html = html.replaceAll("</\\w+>", "");
	         html = html.replaceAll("<\\w+\\s/>", "");
	         html = html.replaceAll("<[\\w\\s=\"]+/>", "");
	         html = html.replaceAll("\\s{2,}", "<--br>");

	         if (html.equals("")) break;

	         html = "<--br><--div><--b>" + title + "<--/b><--/div>" + html;

	         xml = xml.substring(0, i) + html + xml.substring(j + tag.length() + 3, xml.length());
	         start = i;
	      }

	      return xml;
	   }

	   // -------------------------------------------------------------------------------------------------------------------------------------

	   @SuppressWarnings("unused")
	private static String linkTransform(String xml) {
	      int start = 0;
	      while (true) {
	         int i = xml.indexOf("<link>", start);
	         if (i == -1) break;
	         int j = xml.indexOf("</link>", i);
	         if (j == -1) break;
	         String html = xml.substring(i + "link".length() + 2, j);

	         html = html.replace("<url>", "&lt;a href=\"");
	         html = html.replace("</url>", "\">");
	         if (html.indexOf("<--i>name<--/i>: ") == -1) {
	            html = html.replace("<name></name>", "&nbsp;Link&nbsp;");
	            html = html.replace("<name>", "");
	            html = html.replace("</name>", "");
	         } else {
	            html = html.replace("<--i>name<--/i>: ", "");
	            html = "<--i>name<--/i>: " + html;
	         }
	         html += "&lt;/a&gt;";

	         xml = xml.substring(0, i) + html + xml.substring(j + "link".length() + 3, xml.length());
	         start = i;
	      }

	      return xml;
	   }

	   // -------------------------------------------------------------------------------------------------------------------------------------

	   private static String cdataTransform(String xml) {
	      String startKey = "![CDATA[", endKey = "]]";

	      int start = 0;
	      while (true) {
	         int i = xml.indexOf(startKey, start);
	         if (i == -1) break;
	         int j = xml.indexOf(endKey, i);
	         if (j == -1) break;
	         String html = xml.substring(i + startKey.length(), j);

	         html = unescape(html);
	         html = html.replaceAll(">\\s+<", "><");
	         html = html.replaceAll("<\\w+>", "");
	         html = html.replaceAll("</\\w+>", " ");
	         html = html.replaceAll("<\\w+\\s/>", "");

	         xml = xml.substring(0, i) + html + xml.substring(j + endKey.length(), xml.length());
	         start = i;
	      }

	      return xml;
	   }

	   // -------------------------------------------------------------------------------------------------------------------------------------

	   public static String xmlToHtml(String xml) {
	      String html = xml;

	      html = html.replaceAll(">\\s+<", "><");

	      html = blockTransform(html, "buyer", "Buyer");
	      html = blockTransform(html, "seller", "Seller");
	      html = blockTransform(html, "address", "Address");
	      html = blockTransform(html, "pin", "PIN");
	      html = blockTransform(html, "subdivision", "Subdivision");

	      html = html.replaceAll("<\\?(.*?)>", "");
//	      html = html.replaceAll(">\\s*<starter>", " --><starter>");

//	      html = linkTransform(html);

	      html = cdataTransform(html);

//	      html = html.replace("<type>", "<--br><--br><--div><--b>");
//	      html = html.replace("</type>", "<--/b><--/div>");
//	      html = html.replace("<freeForm>", "<--br><--br><--div><--b>Legal Description<--/b><--br>");
//	      html = html.replace("</freeForm>", "<--/div>");
//
//	      html = html.replaceAll("<\\w+>", "");
//	      html = html.replaceAll("</\\w+>", "");
//	      html = html.replaceAll("<\\w+\\s/>", "");
//	      html = html.replaceAll("<[\\w\\s=\"]+/>", "");
	      html = html.replaceAll("\\r\\n", "<br>");
	      html = html.replaceAll("\\n", "<br>");
	      html = html.replaceAll("\\r", "<br>");

	      html = html.replaceAll(">\\s+<", "><");

//	      html = html.replaceAll("<--", "<");

	      html = unescape(html);
//	      html = html.replaceAll("(<br>){3,}", "<br><br>");

	      // Prepend HTML header and append HTML footer
	      html = "<!DOCTYPE HTML><html>" +
	             "<head><link rel=\"shortcut icon\" href=\"" + URLMaping.path + "/favicon.ico\" type=\"image/x-icon\"/></head>" +
	    		 "<body>" + html + "</body>" +
	             "</html>";

	      return html;
	   }

	   // -------------------------------------------------------------------------------------------------------------------------------------
	   // -------------------------------------------------------------------------------------------------------------------------------------
}
