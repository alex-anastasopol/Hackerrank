package ro.cst.tsearch.extractor.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Category;
public class ResultTable {
	
	protected static final Category logger= Category.getInstance(ResultTable.class.getName());

    protected String[][] body;
    protected String[] head;
    protected Map map;
    protected boolean readOnly=false;

    public String[][] getBodyRef(){
    	return body;
    }
    

    public String[][] getBody() {
        if (body==null)
            return null;
        String[][] ret=new String[body.length][];
        for (int i=0; i<ret.length; i++) {
            ret[i]=new String[body[i].length];
            for (int j=0; j<ret[i].length; j++)
                ret[i][j]=body[i][j];
        }
        return ret;
    }

    public String[] getHead() {
        if (head==null)
            return null;
        String[] ret=new String[head.length];
        for (int i=0; i<ret.length; i++)
            ret[i]=head[i];
        return ret;
    }

    public int getLength() {
        return body.length;
    }

    public Map getMapRefference()
    {
    	return map;
    }
    
    public ResultMap getMap() {
        return new ResultMap(map);
    }

    public boolean isReadOnly() {
    	return readOnly;
    }
    
    public void setReadOnly() {
        readOnly=true;
    }

    public void setReadOnly(boolean readO){
    	readOnly = readO;
    }
    protected void setReadWrite() {
        readOnly=false;
    }

    public void setMap(Map m) throws Exception {
//        if (map!=null)
//            throw new Exception("Map already set");
        map=m;
    }

    public void setHead(List al) throws Exception {
        if (readOnly)
            throw new Exception("This object is read-only");
        if (al==null)
            throw new Exception("Initialization with null value");
        head=new String[al.size()];
        for (int i=0; i<head.length; i++) {
            if (al.get(i)==null || !(al.get(i) instanceof String))
                throw new Exception("The parameter should be a List of Strings");
            head[i]=(String)al.get(i);
        }
    }

    public void setHead(String[] v) throws Exception {
        if (readOnly)
            throw new Exception("This object is read-only");
        if (v==null)
            throw new Exception("Initialization with null value");
        head=new String[v.length];
        for (int i=0; i<head.length; i++) {
            if (v[i]==null)
                throw new Exception("The parameter should be an array of Strings");
            head[i]=v[i];
        }
    }

    public void setBody(List al) throws Exception {
        if (readOnly)
            throw new Exception("This object is read-only");
        if (head==null)
            throw new Exception("The header should be set first");
        if (al==null)
            throw new Exception("Initialization with null value");
        if (al.size()==0) {
            body=new String[0][head.length];
            return;
        }
        if (al.get(0)==null || !(al.get(0) instanceof List))
            throw new Exception("The parameter should be a List of Lists of Strings");
        int n=((List)al.get(0)).size();
        if (n!=head.length)
            throw new Exception("The length of header and the length of body rows are different");
        body=new String[al.size()][n];
        for (int i=0; i<body.length; i++) {
            if (al.get(i)==null || !(al.get(i) instanceof List))
                throw new Exception("The parameter should be a List of Lists of Strings");
            List li=(List)al.get(i);
            if (li.size()!=n)
                throw new Exception("The table rows' length is not constant");
            for (int j=0; j<n; j++) {
                if (li.get(j)==null || !(li.get(j) instanceof String))
                    throw new Exception("The parameter should be a List of Lists of Strings");
                body[i][j]=(String)li.get(j);
            }
        }
    }

    public void setBody(String[][] v) throws Exception {
        if (readOnly)
            throw new Exception("This object is read-only");
        if (head==null)
            throw new Exception("The header should be set first");
        if (v==null)
            throw new Exception("Initialization with null value");
        if (v.length==0) {
            body=new String[0][head.length];
            return;
        }
        if (v[0]==null)
            throw new Exception("The parameter should be an array of arrays of Strings");
        int n=v[0].length;
        if (n!=head.length)
            throw new Exception("The length of header and the length of body rows are different");
        body=new String[v.length][n];
        for (int i=0; i<body.length; i++) {
            if (v[i]==null)
                throw new Exception("The parameter should be an array of arrays of Strings");
            if (v[i].length!=n)
                throw new Exception("The table rows' length is not constant");
            for (int j=0; j<n; j++) {
                if (v[i][j]==null)
                    throw new Exception("The parameter should be an array of arrays of Strings");
                body[i][j]=v[i][j];
            }
        }
    }

    public static ResultTable joinHorizontal(ResultTable a, ResultTable b) throws Exception {
        ResultTable r=new ResultTable();
        String[] ha, hb, hc;
        ha=a.getHead();
        hb=b.getHead();
        hc=new String[ha.length+hb.length];
        System.arraycopy(ha, 0, hc, 0, ha.length);
        System.arraycopy(hb, 0, hc, ha.length, hb.length);
        r.setHead(hc);
        String[][] ba, bb, bc;
        ba=a.getBody();
        bb=b.getBody();
//        if (ba.length!=bb.length)
//            throw new Exception("Tables are different in length");
        bc=new String[ba.length+bb.length][hc.length];
        for (int i=0; i<ba.length; i++) {
            Arrays.fill(bc[i], ha.length, hc.length, "");
            System.arraycopy(ba[i], 0, bc[i], 0, ha.length);
        }
        for (int i=ba.length; i<bc.length; i++) {
            Arrays.fill(bc[i], 0, ha.length, "");
            System.arraycopy(bb[i-ba.length], 0, bc[i], ha.length, hb.length);
        }
        r.setBody(bc);
        r.setReadOnly();
        return r;
    }
    
    public static ResultTable joinHorizontalWithMap(ResultTable a, ResultTable b) throws Exception {
        ResultTable r=new ResultTable();
        String[] ha, hb, hc;
        ha=a.getHead();
        hb=b.getHead();
        hc=new String[ha.length+hb.length];
        System.arraycopy(ha, 0, hc, 0, ha.length);
        System.arraycopy(hb, 0, hc, ha.length, hb.length);
        r.setHead(hc);
        String[][] ba, bb, bc;
        ba=a.getBody();
        bb=b.getBody();
//        if (ba.length!=bb.length)
//            throw new Exception("Tables are different in length");
        bc=new String[ba.length+bb.length][hc.length];
        for (int i=0; i<ba.length; i++) {
            Arrays.fill(bc[i], ha.length, hc.length, "");
            System.arraycopy(ba[i], 0, bc[i], 0, ha.length);
        }
        for (int i=ba.length; i<bc.length; i++) {
            Arrays.fill(bc[i], 0, ha.length, "");
            System.arraycopy(bb[i-ba.length], 0, bc[i], ha.length, hb.length);
        }
        r.setMap(mergeMapsFromHead(r.getHead(), a.getMapRefference(), b.getMapRefference()));
        r.setBody(bc);
        r.setReadOnly();
        return r;
    }    

    /**
     * creates a default map from a header
     * @param header
     * @return
     */
    public static Map<String, String[]> createMapFromHead(String[] header){
    	Map<String,String[]> map = new HashMap<String,String[]>();
		for (int i=0; i< header.length; i++){
		   map.put(header[i], new String[]{header[i], ""});
		}
		return map;
    }
    
    /**
     * Merge 2 maps based on the header. It's used for <b>joinHorizontalWithMap</b> <br>
     * Map a has priority<br>
     * If a header is not mapped, it will be mapped with default settings<b>
     * @param header - the header of a joined table 
     * @param a - first map
     * @param b - second map
     * @return
     */
    public static Map<String, String[]> mergeMapsFromHead(String[] header, Map<String, String[]> a, Map<String, String[]> b){
    	Map<String,String[]> map = new HashMap<String,String[]>();
    	map.putAll(a);
    	map.putAll(b);
		return map;
    }
    
    public static ResultTable joinHorizontalFull(ResultTable a, ResultTable b) throws Exception {
        ResultTable r=new ResultTable();
        String[] ha, hb, hc;
        ha=a.getHead();
        hb=b.getHead();
        hc=new String[ha.length+hb.length];
        System.arraycopy(ha, 0, hc, 0, ha.length);
        System.arraycopy(hb, 0, hc, ha.length, hb.length);
        r.setHead(hc);
        String[][] ba, bb, bc;
        ba=a.getBody();
        bb=b.getBody();
        if (ba.length!=bb.length)
            throw new Exception("Tables are different in length");
        bc=new String[ba.length][hc.length];
        for (int i=0; i<ba.length; i++) {
            System.arraycopy(ba[i], 0, bc[i], 0, ha.length);
            System.arraycopy(bb[i], 0, bc[i], ha.length, hb.length);
        }
        r.setBody(bc);
        r.setReadOnly();
        return r;
    }

    public static ResultTable joinVertical(ResultTable a, ResultTable b, boolean fill) throws Exception {
        ResultTable r=new ResultTable();
        String[] ha, hb, hc;
        ha=a.getHead();
        hb=b.getHead();
        if (ha.length!=hb.length && !fill)
            throw new Exception("Tables have different number of columns");
        hc=new String[ha.length];
        System.arraycopy(ha, 0, hc, 0, ha.length);
        r.setHead(hc);
        String[][] ba, bb, bc;
        ba=a.getBody();
        bb=b.getBody();
        bc=new String[ba.length+bb.length][hc.length];
        for (int j=0; j<ha.length; j++) {
            int j2=0;
            for (; j2<hb.length; j2++)
                if (SearchItem.compare(ha[j], hb[j2], "iw"))
                    break;
            if (j2==hb.length && !fill)
                throw new Exception("Column \""+ha[j]+"\" not found");
            for (int i=0; i<ba.length; i++)
                bc[i][j]=ba[i][j];
            for (int i=0; i<bb.length; i++)
                if (j2==hb.length)
                    bc[i+ba.length][j]="";
                else
                    bc[i+ba.length][j]=bb[i][j2];
        }
        r.map = a.map;
        r.setBody(bc);
        r.setReadOnly();
        return r;
    }

    public static ResultTable[] splitVertical(ResultTable a, int np) throws Exception {
        ResultTable[] r=new ResultTable[np];
        String[] h=a.getHead();
        String[][] b=a.getBody();
        int d=h.length/np, m=h.length%np, rr=0;
        for (int i=0; i<np; i++) {
            if (i>0 && i<=m)
                rr++;
            r[i]=new ResultTable();
            List hr=new ArrayList();
            for (int j=d*i+rr; j<d*i+rr+d+(i<m?1:0); j++)
                hr.add(h[j]);
            r[i].setHead(hr);
            List br=new ArrayList();
            for (int k=0; k<b.length; k++) {
                List br2=new ArrayList();
                for (int j=d*i+rr; j<d*i+rr+d+(i<m?1:0); j++)
                    br2.add(b[k][j]);
                br.add(br2);
            }
            r[i].setBody(br);
            r[i].setReadOnly();
        }
        return r;
    }

    public static ResultTable[] splitHorizontal(ResultTable a, int np) throws Exception {
        ResultTable[] r=new ResultTable[np];
        List l=new ArrayList();
        String[] h=a.getHead();
        l.add(h);
        String[][] b=a.getBody();
        for (int i=0; i<b.length; i++)
            l.add(b[i]);
        int d=l.size()/np, m=l.size()%np, rr=0;
        if (d<2)
            throw new Exception("Too many pieces : "+np);
        for (int i=0; i<np; i++) {
            if (i>0 && i<=m)
                rr++;
            r[i]=new ResultTable();
            r[i].setHead((String[])l.get(d*i+rr));
            b=new String[d+(i<m?1:0)-1][h.length];
            for (int j=d*i+rr+1; j<d*i+rr+d+(i<m?1:0); j++) {
                String[] c=(String[])l.get(j);
                for (int k=0; k<c.length; k++)
                    b[j-(d*i+rr+1)][k]=c[k];
            }
            r[i].setBody(b);
            r[i].setReadOnly();
        }
        return r;
    }

    public int getItemIndex(String col, String pm) throws Exception {
        boolean found=false;
        int r=-1;
        for (int i=0; i<head.length; i++)
            if (SearchItem.compare(col, head[i], pm))
                if (found)
                    throw new Exception("There are 2 columns matching : "+r+" and "+i);
                else {
                    found=true;
                    r=i;
                }
        if (r==-1)
            throw new XMLSyntaxRuleException("Column \""+col+"\" with params \""+pm+"\" not found.");

        return r;
    }

    public String getItem(String col, String pm, int index) throws Exception {
        return body[index][getItemIndex(col, pm)];
    }

    public String[] getColumn(String name, String param) throws Exception {
        int col;
        for (col=0; col<head.length && !SearchItem.compare(name, head[col], param); col++);
        if (col==head.length)
            throw new Exception("Column "+name+" ("+param+") not found");

        String[] ret=new String[body.length];
        for (int i=0; i<body.length; i++)
            ret[i]=body[i][col];
        return ret;
    }

    public void addColumn(String name, String[] val) throws Exception {
        if (readOnly)
            throw new Exception("This object is read-only");
        if (val.length!=body.length)
            throw new Exception("Different columns length");
        String[] newHead=new String[head.length+1];
        System.arraycopy(head, 0, newHead, 0, head.length);
        newHead[head.length]=name;
        head=newHead;
        String[][] newBody=new String[body.length][];
        for (int i=0; i<body.length; i++) {
            newBody[i]=new String[body[i].length+1];
            System.arraycopy(body[i], 0, newBody[i], 0, body[i].length);
            newBody[i][body[i].length]=val[i];
        }
        body=newBody;
    }

    public String toString() {
        if (body==null)
            return "null";
        StringBuffer sb=new StringBuffer();
        sb.append('\n');
        for (int i=0; i<head.length; i++) {
//sb.append(">>");
            sb.append(head[i]);
//sb.append("<<");
            sb.append('\t');
        }
        sb.append("\n--------------------------------------------------------------------------------\n");
        for (int i=0; i<body.length; i++) {
            for (int j=0; j<body[i].length; j++) {
//sb.append(">>");
                sb.append(body[i][j]);
//sb.append("<<");
                sb.append('\t');
            }
            sb.append('\n');
        }
        sb.append("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - \n");
        if (map!=null) {
            Iterator it=map.entrySet().iterator();
            while (it.hasNext()) {
                Entry e=(Entry)it.next();
                sb.append(e.getKey());
                sb.append('=');
                String[] a=(String[])e.getValue();
                sb.append(a[0]);
                sb.append(':');
                sb.append(a[1]);
                sb.append('\n');
            }
        } 
        return sb.toString();
    }

    
	/**
	 * Specific utility method that adds a header of this form { "Book", "Page", "InstrumentNumber" } with it's values 
	 * that are contained in the bodyCr parameter. The final result is saved in @see {@link ResultMap}  under the key 
	 * "CrossRefSet"
	 * @param m
	 * @param bodyCR
	 * @param currentCr
	 * @param isRefSet
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public static void appendRefSetToResultMap(ResultMap m, List<List> bodyCR, ResultTable currentCr)
			throws Exception {
		List<String> line;
		boolean hasRefSet = currentCr!=null? true: false;
		if(hasRefSet ){
			for (String[] s : currentCr.getBodyRef()) {
					if (!bodyContainsLine(bodyCR, s)) {
						line = new ArrayList<String>();
						line.add(s[0]);
						line.add(s[1]);
						line.add(s[2]);
						bodyCR.add(line);
					}	
			}
		}
		if (!bodyCR.isEmpty()) {
			String[] header = { "Book", "Page", "InstrumentNumber" };
			Map<String, String[]> map = new HashMap<String, String[]>();
			map.put("Book", new String[] { "Book", "" });
			map.put("Page", new String[] { "Page", "" });
			map.put("InstrumentNumber", new String[] { "InstrumentNumber", "" });
			ResultTable cr = new ResultTable();
			cr.setHead(header);
			cr.setBody(bodyCR);
			cr.setMap(map);
			
			m.put("CrossRefSet", cr);
		}
	}
    
	@SuppressWarnings("rawtypes")
	public static boolean bodyContainsLine(List<List> bodyCR, String[] s) {
		for (List line : bodyCR) {
			if (line.size()==s.length) {
				boolean equalLine = true;
				for (int i=0;i<line.size();i++)
					equalLine = equalLine && (line.get(i)!=null) && (line.get(i).equals(s[i]));
				if (equalLine)		
					return true;
			} 
		}
		return false;
	} 
	
    public static void main (String args[]) throws Exception {
        ResultTable a=new ResultTable() ;
        a.setHead(new String[]{"a", "b", "c", "d", "e"});
        a.setBody(new String[][]{{"1", "2", "3", "4", "5"}, {"A", "B", "C", "D", "E"}, {"6", "7", "8", "9", "0"}});
        a.setReadOnly();
        ResultTable b=new ResultTable() ;
        b.setHead(new String[]{"b", "c", "d", "e", "a"});
        b.setBody(new String[][]{{"1", "2", "3", "4", "5"}, {"A", "B", "C", "D", "E"}, {"6", "7", "8", "9", "0"}});
        b.setReadOnly();
        ResultTable c=joinVertical(a, b, false);
        logger.info(c);
    }
}

