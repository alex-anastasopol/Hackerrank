package ro.cst.tsearch.corrector;
 
import java.util.regex.*;
import java.io.*;

import org.apache.log4j.Category;;

public class CorrectTable {

	protected static final Category logger= Category.getInstance(CorrectTable.class.getName());
	
    protected int st, pos;
    protected StringBuffer sb;

    protected int[][][][] action1={{{{0, 1}, {1, 2}, {2, 3}},
                                    {{2, 0}, {0, 1}, {1, 2}},
                                    {{1, 2}, {2, 0}, {0, 1}}},
                                   {{{0, 1}, {1, 2}, {2, 0}},
                                    {{2, 0}, {0, 1}, {1, 2}},
                                    {{1, 2}, {2, 0}, {0, 1}}}};

    protected int[][][][] action2={{{{0, 1}, {1, 2}, {2, 3}},
                                    {{-1,0}, {0, 1}, {1, 2}},
                                    {{1,-1}, {-1,0}, {0, 1}}},
                                   {{{0, 1}, {1,-1}, {-1,0}},
                                    {{-1,0}, {0, 1}, {1,-1}},
                                    {{1,-1}, {-1,0}, {0, 1}}}};

    protected int[][][][] action={{{{0, 1}, {1, 2}, {2, 3}},
                                   {{-1,0}, {0, 1}, {1, 2}},
                                   {{1,-1}, {-1,0}, {0, 1}}},
                                  {{{0,-2},{-2,-1}, {-1,0}},
                                   {{-1,0}, {0, 1}, {1,-1}},
                                   {{1,-1}, {-1,0}, {0, 1}}}};

    protected String[] tags={"TABLE", "TR", "TD"};

    public CorrectTable(String s) {
        sb=new StringBuffer(s);
    }

    public void reset() {
        st=pos=0;
    }

    public void doAction(int o, int sign) {
        int st1, a;

        if (st<3) {
            st1=st;
            a=action[0][st1][o][sign];
            if (a==-9)
                a=action[1][st1][o][sign];
        } else {
            st1=st%3;
            a=action[1][st1][o][sign];
        }

        st+=a-sign*2+1;
        if (a>0)  insert(st1, 0);
        if (a>1)  insert((st1+1)%3, 0);
        if (a>2)  insert((st1+2)%3, 0);
        if (a<0)  insert((st1+2)%3, 1);
        if (a<-1) insert((st1+1)%3, 1);
    }

    public void insert(int o, int sign) {
        StringBuffer tsb=new StringBuffer("<");
        if (sign==1)
            tsb.append("/");
        tsb.append(tags[o]);
        tsb.append(">");

        sb.insert(pos, tsb);
        pos+=tsb.length();
    }

    public String process() {
        Matcher m=Pattern.compile("(?i)</?(?:TABLE|TR|TD)").matcher(sb);
        while (m.find(pos)) {
            String crt=m.group().toUpperCase();
            int o=0, sign=crt.charAt(1)=='/'?1:0;
            for (int i=1; i<tags.length; i++)
                if (crt.indexOf(tags[i])!=-1)
                    o=i;
            pos=m.start();
//logger.info("before: crt="+crt+", st="+st+", o="+o+", sign="+sign+", pos="+pos+", sb="+sb);
            doAction(o, sign);
//logger.info("after : crt="+crt+", st="+st+", o="+o+", sign="+sign+", pos="+pos+", sb="+sb);
            pos+=3;
        }
        for (pos=sb.length(); st>0; pos=sb.length(), st--) {
            insert((st-1)%3, 1);
        }
        return sb.toString();
    }

    public static void main (String args[]) throws Exception {
        if (args.length!=1) {
            logger.error("Usage : java CorrectTable <file>");
            System.exit(1);
        }
        BufferedReader in=new BufferedReader(new InputStreamReader( new BufferedInputStream(new FileInputStream(args[0]))));
        String s="", s1;
        while ((s1=in.readLine())!=null) {
            s+=s1+"\n";
        }
        in.close();
    	s=s.substring(0, s.length()-1);
        CorrectTable ct=new CorrectTable(s);
        logger.info(ct.process());
    }
}
