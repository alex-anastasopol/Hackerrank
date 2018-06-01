package ro.cst.tsearch.servers.response;

import java.util.*;
import java.io.*;
import java.util.regex.*;

import org.apache.log4j.Category;

public class DavidsonSplit {
	protected static final Category logger= Category.getInstance(DavidsonSplit.class.getName());
    protected String html, links;
    protected ArrayList bounds=new ArrayList();
    protected boolean hasNext, hasPrev;

    public void setDoc(String s) {
        html=s;
        parse();
        /*try {
            write("out1.html",s );
        } catch (Exception e) {
            e.printStackTrace();
        } */
    }

    public String getDoc() {
        return html;
    }

/*    public String getDocWithButtons(String link) {
        StringBuffer sb=new StringBuffer(html);
        for (int i=bounds.size()-1; i>0; i--) {
            int j=((Integer)bounds.get(i)).intValue();
            sb.insert(j, "<br><input type=\"button\" value=\"Save to TSD\" class=\"button\" onclick=\""+link+"&xindex="+(i-1)+"\"><br>");
        }
        return sb.toString();
    }*/

    public String getSplitDoc(int i) throws Exception {
        if (i>=getSplitNo() || i<0)
            throw new Exception("DavidsonSplit : Index out of range : "+i+" from "+getSplitNo());
        int b=((Integer)bounds.get(i)).intValue(),
            e=((Integer)bounds.get(i+1)).intValue();
       // StringBuffer sb=new StringBuffer("<html><body>");
	   StringBuffer sb=new StringBuffer();
        sb.append(html.substring(b+4, e));
      //  sb.append("</html></body>");
        return sb.toString();
    }

    public int getSplitNo() {
        return bounds.size()-1;
    }

    public String getNextLink() throws Exception {
        int e, b;
        if (!hasNext)
            return "";
        Matcher m=Pattern.compile("<[aA]").matcher(links);
        if (m.find())
            if (hasPrev)
                if (m.find())
                    b=m.start();
                else
                    throw new Exception("<A > tag not found");
            else
                b=m.start();
        else
            throw new Exception("<A > tag not found");
        m=Pattern.compile("</[aA]>").matcher(links);
        if (m.find(b))
            e=m.end();
        else
            throw new Exception("</A> tag not found");
        return links.substring(b, e);
    }

    public String getPrevLink() throws Exception {
        int e, b;
        if (!hasPrev)
            return "";
        Matcher m=Pattern.compile("<[aA]").matcher(links);
        if (m.find())
            b=m.start();
        else
            throw new Exception("<A > tag not found");
        m=Pattern.compile("</[aA]>").matcher(links);
        if (m.find(b))
            e=m.end();
        else
            throw new Exception("</A> tag not found");
        return links.substring(b, e);
    }

    private void parse() {
        Matcher m=Pattern.compile("(?i)Previous</[aA]>").matcher(html);
        hasPrev=m.find();
        m=Pattern.compile("(?i)Next</[aA]>").matcher(html);
        hasNext=m.find();
        if (hasNext || hasPrev) {
            int a1=html.lastIndexOf("</t");
            int a2=html.lastIndexOf("</T");
            a1=(a2>a1?a2:a1)+8;
            String s=html.substring(a1);
            m=Pattern.compile("(?i)<HR>").matcher(s);
            if (!m.find()) {
                html=html.substring(0, a1)+"<hr>"+html.substring(a1);
            }
        }

        int i=0;
        while ((i=html.indexOf("<hr>", i))!=-1) {
            bounds.add(new Integer(i));
            i+=4;
        }
        if (!hasNext && !hasPrev) {
            bounds.add(new Integer(html.length()));
        }
        if (hasNext || hasPrev) {
        	int b1=html.lastIndexOf("<hr>");
        	int b2=html.lastIndexOf("<HR>");
        	b1=b2>b1?b2:b1;
            links=html.substring(b1+4);
        }
    }

    public static void main (String args[]) throws Exception {
        DavidsonSplit ds=new DavidsonSplit();
        ds.read("test\\out2.html");
        //logger.info(">>>>>"+ds.getSplitDoc(ds.getSplitNo()-1));
        logger.info("Next link = "+ds.getNextLink());
        logger.info("Prev link = "+ds.getPrevLink());
    }

    private void read(String file) throws Exception {
        BufferedReader in=new BufferedReader(new InputStreamReader( new BufferedInputStream(new FileInputStream(file))));
        StringBuffer sb=new StringBuffer();
        String s1;
        while ((s1=in.readLine())!=null) {
            sb.append(s1);
        }
        in.close();
        setDoc(sb.toString());
    }

    private void write(String file, String s) throws Exception {
        PrintStream out=new PrintStream(new BufferedOutputStream(new FileOutputStream(file)));
        out.print(s);
        out.close();
    }
}
