package ro.cst.tsearch.corrector;

import javax.xml.parsers.*;

import org.w3c.dom.*;

import java.io.*;
import java.util.*;
import java.text.*;

import org.xml.sax.*;

import java.util.regex.*;

import ro.cst.tsearch.utils.*;
import ro.cst.tsearch.*;
import ro.cst.tsearch.extractor.xml.*;
import ro.cst.tsearch.generic.IOUtil;

import org.apache.log4j.Category;

public class HTMLCorrector {
    private static final Category logger = Category
            .getInstance(HTMLCorrector.class.getName());

    public static String[][] mtags = { { "TABLE", "TR", "TD" } };
    public static String[] htmlCodes = { "quot", "amp", "lt", "gt", "nbsp", "iexcl", "cent", "pound", "curren", "yen", 
                                            "brvbar", "sect", "uml", "copy", "ordf", "laquo", "not", "shy", "reg", "macr",
                                            "apos", "#39"}; // fix for bug #1395

    public static void main(String args[]) throws Exception {

        //       HTMLCorrector2();
        if (args.length != 1) {
            logger
                    .error("Usage : java ro.cst.tsearch.corrector.HTMLCorrector <file>");
            System.exit(1);
        }

        DocumentBuilder db = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                new BufferedInputStream(new FileInputStream(args[0]))));
        String s = "", s1;
        while ((s1 = in.readLine()) != null) {
            s += s1 + "\n";
        }
        in.close();
        s = s.substring(0, s.length() - 1);
        s = correct(s);
        logWithRewrite(s);
        try {
            Document d = db.parse(new InputSource(new StringReader(s)));
        } catch (SAXParseException e) {
            logger.error("line number : " + e.getLineNumber() + ",  column :"
                    + e.getColumnNumber() + ",  excep : " + e.getException());
            throw e;
        }
    }

    protected static DecimalFormat nf3 = new DecimalFormat("000");

    protected static DecimalFormat nf2 = new DecimalFormat("00");

    public static String format(long d) {
        String ret = "." + nf3.format(d % 1000);
        d /= 1000;
        ret = ":" + nf2.format(d % 60) + ret;
        d /= 60;
        ret = ":" + nf2.format(d % 60) + ret;
        d /= 60;
        ret = nf2.format(d % 60) + ret;
        return ret;
    }

    public static String correctAndVerify(String s, String context,long searchId)
            throws Exception {
        long t1 = System.currentTimeMillis();
        s = correct(s);
        long t2 = System.currentTimeMillis();
        context = context.substring(0, context.lastIndexOf(File.separatorChar));
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder();
            Document d = db.parse(new InputSource(new StringReader(s)));
            long t3 = System.currentTimeMillis();
            //            logger.debug("correct : "+format(t2-t1));
            //            logger.debug("XML parse : "+format(t3-t2));
        } catch (SAXParseException e) {
            logException(e, s, context,searchId);
        }
        return s;
    }

    protected static void logException(Exception e, PrintWriter pw)
            throws IOException {
        if (e instanceof SAXParseException) {
            SAXParseException spe = (SAXParseException) e;
            pw.println("line number : " + spe.getLineNumber() + ",  column :"
                    + spe.getColumnNumber() + ", original exception : "
                    + spe.getException());
        }
    }

    public static void logException(Exception e, String s, String context,long searchId)
            throws IOException {
        //        logger.info("line number : "+e.getLineNumber()+", column
        // :"+e.getColumnNumber()+", excep : "+e.getException());
        File dir = new File(context + File.separator + "exceptionLogs");
        if (!dir.exists())
            dir.mkdir();
        File[] exceptionFiles = dir.listFiles();
        int max = 0;
        for (int i = 0; i < exceptionFiles.length; i++)
            if (exceptionFiles[i].isFile()
                    && exceptionFiles[i].getName().startsWith("Exception_")) {
                int comp = Integer.parseInt(exceptionFiles[i].getName()
                        .substring(
                                exceptionFiles[i].getName().indexOf('_') + 1,
                                exceptionFiles[i].getName().lastIndexOf(".")));
                if (comp > max)
                    max = comp;
            }
        max++;
        String fileName = dir.getCanonicalPath() + File.separator + "Exception_" + max + ".txt";
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            logException(e, pw);
            e.printStackTrace(pw);
            pw.println();
            pw.println();
            pw.println(s);
            pw.println();
            pw.println("--------------------------- Search used --------------------------");
            pw.println(searchId);
        } finally {
            pw.close();
        }
        pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
        try {
            pw.println(sw.toString());
        } finally {
            pw.close();
        }
        	
        	
//            Log.sendEmail(ServerConfig.getAtsExceptionEmail(), "XML Parse Error", sw.toString(), new String[] {search.getSearchDir() + "orderFile.html"});
		Log.sendEmail(MailConfig.getExceptionEmail(), "XML Parse Error", sw.toString(), null);

    }

    protected static String[] 
        simpleTags = { "BASE", "IMG", "SIZE", "COL", "INPUT", "AREA" }, 
        closeTags = { "TEXTAREA", "DIV", "NOBR", "FONT", "CENTER", "SMALL", "STRONG", "THEAD", "TBODY", "ABBR", "ACRONYM", "BIG", "CITE", "EM", "TT", "SPACER", "NAMEAS" }, 
        completeTags = { "HEAD", "SCRIPT", "CAPTION" },
        removeTags = { "LEFT" };

    public static String correct(String s) throws Exception {
    	
        //logger.info(s);
        //logger.info("\n\n\n");
        s = s.replaceAll("\t", " ");
        s = s.replaceAll( "!\n", "" );
        s = tagsToUpperCase(s);
        s = s.replaceAll("<!DOCTYPE[^>]*>", "");
        if (s.startsWith(">")) { // pt spargerea pe randuri....
            s = s.substring(1);
        }
        if (s.lastIndexOf('>') < s.lastIndexOf('<')) { // pt spargerea pe
                                                       // randuri....
            s += ">";
        }
        for (int i = 0; i < completeTags.length; i++)
            s = s.replaceAll("(?s)<" + completeTags[i] + ".*?</"
                    + completeTags[i] + ">", "");
        s = s.replaceAll(" {2,}", " ");
        s = s.replaceAll("(?s)</?BR.*?>", "  ");
        s = s.replaceAll("\\bon[a-zA-Z]+=\\\"[^\\\"]+\\\"", "");	// remove (onMouseOver=".....")
        s = s.replaceAll("(?s)<A[^A-Z].*?>", "");
        s = s.replaceAll("</A>", "");
        s = s.replaceAll("(?s)</?[BIUP](?:>|\\s.*?>)", "");
        s = s.replaceAll("</?H\\d(?:>| .*?>)", "");
        s = s.replaceAll("</FORM>", "");
        s = s.replaceAll("(<FORM.*?>)", "$1</FORM>");
        s = s.replaceAll("FULL>=", "FULL&gt;=");
        
        for (int i = 0; i < simpleTags.length; i++)
            s = s.replaceAll("(?s)<" + simpleTags[i] + ".*?>", "");
        
        for (int i = 0; i < removeTags.length; i++){			// fix for bug #2091
        	s = s.replace("<"+removeTags[i]+">", "");
        	s = s.replace("</"+removeTags[i]+">", "");
        }
        
        /*s = s.replaceAll("(?i)<INPUT((?!readonly)[^>])*>", "");  // replace readonly input tags with their value - needed in FL PalmBeachTR        
        s = s.replaceAll("(?is)<INPUT(?:(?!value).)*value=\"([^\"]+)\"[^>]*>", "$1");
        s = s.replaceAll("(?s)<INPUT[^>]*>", "");*/        
        
        for (int i = 0; i < closeTags.length; i++)
            s = s.replaceAll("(?s)</?" + closeTags[i] + ".*?>", "");
        s = s.replaceAll("(?s)(</?)TH(.*?>)", "$1TD$2");
        s = s.replaceAll("<HR.*?>", "<HR/>");
        s = s.replaceAll("([a-z]+=\"\\d+%\")([a-z]*=\"\\w+)", "$1 $2"); //fix for bug #993 (width="49%"class="detailData" is changed to width="49%" class="detailData")
        
        //s=s.replaceAll("&([a-zA-Z]*[^a-zA-Z;])", "&amp;$1");
        s = parseApersand(s);
        s = s.replaceAll("&nbsp;>&nbsp;", "&nbsp;&gt;&nbsp;");
        s = s.replaceAll( " < ", " &lt; " );
        s = s.replaceAll(" ?&nbsp; ?", " ");
        s = s.replaceAll("&copy;", "");
        s = s.replaceAll("\u0015", " ");
        s = s.replaceAll("\u00a0", " ");
        s = s.replaceAll("(?s)<!--.*?-->", "");
        
        s = s.replaceAll("(>\\w+)>(?=\\s*\\d+\\b)", "$1&gt;"); //<TD>P>6028</TD>	        
        s = s.replaceAll("(<)([^/<>]*<)", "&lt;$2");
        s = s.replaceAll("(<TD(?:\\s\\w+=\"[:;\\w\\s]+%?\")+>)([^/<>]+)>", "$1$2&gt;"); //fix for a parser bug reported by ATS on 10/20/2006
        s = s.replaceAll("(>)([^/<>]*>)", "&gt;$2");
        s = s.replaceAll("<(\\d[^/<>]*)>", "&lt;$1&gt;");  // fix for a parser bug reported by ATS on 11/13/2006

        s = s.replaceAll(">\\s+<", "><");
        s = s.replaceAll("(<TD[^>]*>)(</TD>)", "$1 $2");
        s = s.replaceAll("(\\w=\"[^\"]+\")(\\w)", "$1 $2");
        s = s.replaceAll("([^=\\\\]\")\"\\s*>", "$1>");
        s = s.replace('\002', ' ');
        
        s = s.replaceAll("(?i)(<TD[^,>]*),([^>]+>)" , "$1 $2" );
        s = s.replaceAll("(?i)(<TD[^>]*\"[ ]+)\"(>)", "$1 $2" );
        
        s = s.replaceAll("(?i)</TD<", "</TD><");
        s = s.replaceAll("-->","");
        s = s.replaceAll("(?i)\\\\([M])", "$1");
        //s = s.replaceAll("--&gt;","");
        CorrectTable ct = new CorrectTable(s);
        s = ct.process();

        StringBuffer sb = new StringBuffer(s);
        delimAttributes(sb);
        XMLattributes(sb);
        s = sb.toString();
       
        s = s.replaceFirst("(?s)<HTML[^>]+>", "");
        if (s.indexOf("<HTML>") == -1) {        	
        	if (s.indexOf("<BODY") == -1){
            	s = "<BODY>" + s;
            }
            s = "<HTML>" + s;
        } else {
        	s = s.replaceFirst("^(.+)(<HTML><BODY[^>]*>)", "$2$1"); // fix for a parser bug reported by ATS on 11/14/2006
        }
        
        if (s.indexOf("</HTML>") == -1) {
            s += "</BODY></HTML>";
        }
        
        s = s.replaceFirst("(?s)(?<=</HTML>).+", "");        
        //s=s.replaceAll("(?s)(<TD[^<]*?)</A>", "$1");
        //s=s.replaceAll("(?s)(<A[^<]*?)</TD>", "$1</A></TD>");

        //logger.info(s);
        //System.exit(0);
        s= s.replaceAll("(?i)TABLE&gt;\\s*]*>", "TABLE>");//2405
        s= s.replaceAll("(?i)(<TD[^>]*/>)\\s*</TD>","$1");//2405
        //s= s.replaceAll("(?i)TR&gt;\\s*]*>", "TR>");
        return s;
    }
    
    protected static String parseApersand(String s) {
        StringBuffer sb = new StringBuffer(s);
        String code = "";
        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == '&') {
                int j = i + 1;
                code = "";
                for (; j < sb.length(); j++)
                {
                    if (!Character.isLetter(sb.charAt(j)) && !Character.isDigit(sb.charAt(j)) && (sb.charAt(j) != '#'))
                    {
                        break;
                    }
                    else
                    {
                        code += sb.charAt( j );
                    }
                }
                
                if( j == sb.length() )
                {
                    sb.replace(i, i + 1, "&amp;");
                }
                else if (sb.charAt(j) != ';' || j == i + 1) {
                    sb.replace(i, i + 1, "&amp;");
                    i = j + 3;
                } else {
                    //found ;
                    int k = 0;
                    for( ; k < htmlCodes.length ; k++ )
                    {
                        if( htmlCodes[k].equalsIgnoreCase( code ) )
                        {
                            //replace with the original html code from htmlCodes, since the code parsed from file may have uppercases
                        	sb.replace(i, j+1, "&"+htmlCodes[k]+";");
                            i = j;
                            break;
                        }
                    }
                    
                    if( k == htmlCodes.length )
                    {
                        //not found --> replace
                        sb.replace(i, i + 1, "&amp;");
                        i = j + 3;
                    }
                }
            }
        }
        return sb.toString();
    }

    protected static String tagsToUpperCase(String s) {
        Matcher m = Pattern.compile("</?\\w*").matcher(s);
        StringBuffer sb = new StringBuffer();
        int i = 0;
        while (m.find()) {
            sb.append(s.substring(i, m.start()));
            sb.append(m.group().toUpperCase());
            i = m.end();
        }
        sb.append(s.substring(i));
        return sb.toString();
    }

    protected static void logWithRewrite(String s) throws Exception {
        PrintStream out = new PrintStream(new BufferedOutputStream(
                new FileOutputStream("e.html")));
        out.print(s);
        out.close();
    }

    protected static void delimAttributes(StringBuffer sb) {
        int lt, gt, pos = 0, eq, peq;
        lt = sb.indexOf("<", pos);
        gt = sb.indexOf(">", pos);
        while (lt != -1) {
            for (peq = lt; (eq = sb.indexOf("=", peq)) < gt; gt = sb.indexOf(
                    ">", pos)) {
                if (eq == -1)
                    break;
                int q;
                for (q = eq + 1; q < gt && Character.isWhitespace(sb.charAt(q)); q++)
                    ;
                char c = sb.charAt(q);
                if (c != '\'' && c != '"') {
                    sb.insert(q, "\"");
                    peq = Integer.MAX_VALUE;
                    int i = sb.indexOf(" ", q + 1);
                    if (i != -1)
                        peq = i;
                    i = sb.indexOf(">", q);
                    if (i != -1 && i < peq)
                        peq = i;
                    if (c != '\'' && c != '"') {
                        sb.insert(peq, '"');
                    }
                } else {
                    int i = sb.indexOf(String.valueOf(c), q + 1);
                    if (i == -1 || i > gt) {
                        int j = sb.indexOf(" ", q + 1);
                        if (j == -1 || j > gt)
                            j = gt;
                        sb.insert(j, c);
                        peq = j;
                    } else {
                        peq = i;
                    }
                }
            }
            pos = gt + 1;
            lt = sb.indexOf("<", pos);
            gt = sb.indexOf(">", pos);
        }
    }

    protected static Pattern p1 = Pattern.compile("\\w+\\s+\\w"), pe = Pattern
            .compile("\\s*\\w+\\s*");

    protected static void XMLattributes(StringBuffer sb) {
        int lt, gt, pos = 0;
        lt = sb.indexOf("<", pos);
        a: while (lt != -1) {
            gt = sb.indexOf(">", lt);
            //            String s=sb.substring(lt+1, sb.charAt(gt-1)=='/'?gt-1:gt);
            String s = sb.substring(lt + 1, gt);
            int pos2;
            Matcher m1 = p1.matcher(s);
            if (m1.find()) {
                Matcher me = pe.matcher(s);
                int b = m1.end() - 1;
                while (me.find(b)) {
                    int e = me.end();
                    if (e != s.length() && s.charAt(e) == ';' && "amp".equals(me.group().trim())){
                    	b = e + 1;
                    	sb.delete(lt + e - 3, lt + 2 + e);
                    	continue;
                    } else if (e == s.length() || s.charAt(e) != '=') {
                        sb.insert(lt + 1 + e, "=\"true\" ");
                        continue a;
                    } else {
                        do {
                            e++;
                        } while (Character.isWhitespace(s.charAt(e)));
                        char c = s.charAt(e);
                        b = s.indexOf(c, e + 1) + 1;
                    }
                }
            }
            pos = gt + 1;
            lt = sb.indexOf("<", pos);
            gt = sb.indexOf(">", pos);
        }
    }

    protected static StringBuffer correctTags(StringBuffer sb) {
        return correctTags(sb, mtags);
    }

    protected static StringBuffer correctTags(StringBuffer sb, String[][] tags) {
        for (int i = 0; i < tags.length; i++) {
            sb = correctTag(sb, tags, i, 0);
        }
        return sb;
    }

    protected static StringBuffer correctTag(StringBuffer sb, String[][] tags,
            int i, int i2) {
        logger.info("[ correctTag(" + sb.toString() + ", tags, " + i + ", "
                + i2 + ")");
        String crtTag = tags[i][i2];
        int pos = 0, st = 0;
        Pattern p = Pattern.compile("</?" + crtTag);
        Matcher m = p.matcher(sb);

        if (tags[i].length > i2 + 1) {
            StringBuffer ret = new StringBuffer();
            String[] splits = split(sb, m);
            for (int j = 0; j < splits.length; j += 2) {
                StringBuffer sbs = new StringBuffer(splits[j]);
                sbs = correctTag(sbs, tags, i, i2 + 1);
                ret.append(sbs);
                if (j < splits.length - 1)
                    ret.append(splits[j + 1]);
            }
            sb = ret;
        }

        m = p.matcher(sb);
        while (m.find(pos)) {
            int j = m.start();
            if (sb.charAt(j + 1) == '/')
                st--;
            else {
                if (st == 0) {
                    int k = sb.lastIndexOf("</" + crtTag, j - 1);
                    if (k == -1)
                        k = 0;
                    else
                        k = sb.indexOf(">", k + 2) + 1;
                    if (containsKids(sb.substring(k, j), tags, i, i2)) {
                        sb.insert(j, "</" + crtTag + ">");
                        sb.insert(k, "<" + crtTag + ">");
                        j += crtTag.length() * 2 + 5;
                    }
                }
                st++;
            }

            if (st == -1) {
                int prev = sb.lastIndexOf("</" + crtTag, j - 1);
                if (containsKids(sb.substring(prev == -1 ? 0 : prev, j), tags,
                        i, i2)) {
                    if (prev == -1) {
                        // insert tag
                        sb.insert(0, "<" + crtTag + ">");
                        pos = j + crtTag.length() + 3;
                    } else {
                        // remove first
                        sb.delete(prev, sb.indexOf(">", prev + 2) + 1);
                        pos = sb.indexOf("</" + crtTag, prev) + 2;
                    }
                } else {
                    // remove last
                    sb.delete(j, sb.indexOf(">", j + 2) + 1);
                    pos = j;
                }
                st = 0;
            } else {
                pos = j + 1;
            }
        }
        if (st > 0) {
            pos = sb.length();
            int prevE = sb.lastIndexOf("</" + crtTag, pos);
            int prevB = sb.lastIndexOf("<" + crtTag, pos);
            while (st > 0) {
                while (st > 0 && prevB > prevE) {
                    if (containsKids(sb.substring(prevB, pos), tags, i, i2)) {
                        // insert end tag
                        sb.insert(pos, "</" + crtTag + ">");
                        pos = prevB - 1;
                    } else {
                        // remove tag
                        int j = sb.indexOf(">", prevB) + 1;
                        pos -= j - prevB;
                        sb.delete(prevB, j);
                    }
                    st--;
                    prevB = sb.lastIndexOf("<" + crtTag, pos);
                }
                pos = prevE;
                prevE = sb.lastIndexOf("</" + crtTag, pos - 1);
            }
        }

        int j = sb.length(), k = sb.lastIndexOf("</" + crtTag, j - 1);
        if (k == -1)
            k = 0;
        else
            k = sb.indexOf(">", k + 2) + 1;
        if (containsKids(sb.substring(k, j), tags, i, i2)) {
            sb.insert(j, "</" + crtTag + ">");
            sb.insert(k, "<" + crtTag + ">");
        }
        logger.info("] return " + sb);
        return sb;
    }

    private static boolean containsKids(String s, String[][] tags, int k1,
            int k2) {
        for (int i = k2 + 1; i < tags[k1].length; i++) {
            Matcher m = Pattern.compile("</?" + tags[k1][i]).matcher(s);
            if (m.find())
                return true;
        }
        return false;
    }

    /**
     * 
     * @param htmlResponse
     * @param tableStart
     * @param tableEnd
     * @return the html response with the table between tebleStart and tableEnd corrected
     */
    public static String fixTable( String htmlResponse, int tableStart, int tableEnd )
    {
        String result = htmlResponse;
        
        try
        {
            String table = htmlResponse.substring( tableStart, tableEnd );
            table = table.replaceAll( "<th\\s", "<td " );
            table = table.replaceAll( "<th>", "<td>" );
            table = table.replaceAll( "</th>", "</td>" );
            
            int trStart = -1;
            int trEnd = -1;
            
            int maxTD = -1;
            
            //compute the maximum number of td's in a row
            do
            {
                trStart = table.indexOf( "<tr", trStart + 1 );
                if( trStart >=0 )
                {
                    trEnd = table.indexOf( "</tr", trStart );
                    if( trEnd < 0 )
                    {
                        break;
                    }
                    
                    trEnd += 5;
                    //count the TD's
                    String trString = table.substring( trStart, trEnd );
                    int tdCount = trString.split( "<td" ).length - 1;
                    if( maxTD < tdCount )
                    {
                        maxTD = tdCount;
                    }
                }
            } while( trStart >= 0 );
            
            
            //add td's if not equal with the max td count
            trStart = -1;
            do
            {
                trStart = table.indexOf( "<tr", trStart + 1 );
                if( trStart >=0 )
                {
                    trEnd = table.indexOf( "</tr", trStart );
                    if( trEnd < 0 )
                    {
                        break;
                    }
                    
                    trEnd += 5;
                    //count the TD's
                    String trString = table.substring( trStart, trEnd );
                    int tdCount = trString.split( "<td" ).length - 1;
                    if( tdCount < maxTD )
                    {
                        String tdToAdd = generateTD( maxTD - tdCount );
                        
                        trString = trString.replaceAll( "</tr", tdToAdd + "</tr" );
                        
                        table = table.substring( 0, trStart ) + trString + table.substring( trEnd );
                    }
                }
            } while( trStart >= 0 );
            
            
            result = result.substring( 0, tableStart ) + table + result.substring( tableEnd );
        }
        catch ( Exception e )
        {
            Log.sendExceptionViaEmail( MailConfig.getExceptionEmail(), "HTMLCorrector error!!!", e );
        }
        
        return result;
    }
    
    private static String generateTD( int count )
    {
        String result = "";
        
        while( count > 0 )
        {
            result += "<td>&nbsp;</td>";
            
            count --;
        }
        
        return result;
    }
    
    private static String[] split(StringBuffer sb, Matcher m) {
        ArrayList al = new ArrayList();
        int pos = 0;
        while (m.find(pos)) {
            int j = m.start();
            al.add(sb.substring(pos, j));
            pos = sb.indexOf(">", m.end()) + 1;
            al.add(sb.substring(j, pos));
        }
        al.add(sb.substring(pos));
        return (String[]) al.toArray(new String[0]);
    }

    public static void HTMLCorrector2() throws Exception {
        String s = new String(
                "								<td align=\"left\" class=\"BodyTextNarrow\" valign=\"middle\">\n\n"
                        + "									<a class=\"QuickHelpLink\" href=\"/tax_search\">New&nbsp;Search</a>\n"
                        + "");
        s = correct(s);
        logger.info(s);
        System.exit(0);
    }
}

