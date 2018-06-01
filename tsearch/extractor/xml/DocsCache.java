package ro.cst.tsearch.extractor.xml;

import java.util.*;
import java.io.*;
import org.w3c.dom.*;

public class DocsCache {
    static Map m=new HashMap();

    public synchronized static Document getDocument(String fileName, String path) throws Exception {
       /* if(fileName.indexOf("TNDavidsonAO")!=-1 && !ro.cst.tsearch.servers.response.Parser.useoldavidsonAO.equals("true"))
        {
               fileName=fileName.replaceFirst("\\.xml","new.xml");        
        }*/
        if (m.containsKey(fileName)) {
            return (Document)m.get(fileName);
        }
        Document d=XMLUtils.read(new File(fileName), path);
        m.put(fileName, d);
        return d;
    }
}
