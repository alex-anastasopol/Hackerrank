package ro.cst.tsearch.utils;

import static ro.cst.tsearch.utils.StringUtils.isEmpty;
import static ro.cst.tsearch.utils.XmlUtils.getChildren;
import static ro.cst.tsearch.utils.XmlUtils.getNodeValue;
import static ro.cst.tsearch.utils.XmlUtils.parseXml;

import java.io.File;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ro.cst.tsearch.servlet.BaseServlet;


@SuppressWarnings("unchecked")
public class NameEquivalents
{
    @SuppressWarnings("unchecked")
	private Vector equivList = null;
    
    private static NameEquivalents equivInstance = null;    
    
    private long searchId = -1;
    
    /**
	 * map with all aliases
	 */
	private final static Vector aliases = new Vector();
    
    static {
		String FILE_NAME = BaseServlet.REAL_PATH
				+ "/WEB-INF/classes/resource/utils/name_aliases.xml";
		Document doc = parseXml(new File(FILE_NAME));
		for (Node n0 : getChildren(doc)) {
			if (n0.getNodeName().equals("aliases")) {
				for (Node n1 : getChildren(n0)) {
					if (n1.getNodeName().equals("entry")) {
						String name = null;
						String line = "";
						for (Node n2 : getChildren(n1)) {
							String n2Name = n2.getNodeName();
							if ("name".equals(n2Name)) {
								name = getNodeValue(n2);
								line += name;
							} else if ("nicknames".equals(n2Name)) {
								for (Node n3 : getChildren(n2)) {
									if ("name".equals(n3.getNodeName())) {
										String nick = getNodeValue(n3);
										if (!isEmpty(nick)) {
											//nicks.add(nick.toUpperCase());
											line += "##" + nick.toUpperCase();
										}
									}
								}
							}
						}
						if (!StringUtils.isEmpty(line)) {
							if (!line.contains("YAHWEH")){
								String[] items = line.split("##");
								aliases.add(items);
							}
						}
					}
				}
			}
		}
	}
    
    @SuppressWarnings("unchecked")
	private NameEquivalents(long searchId)
    {	this.searchId = searchId;
        equivList = new Vector();
        equivList = aliases;
        
        //equivList.add( new String[] {"Steven", "Steve"} );
        equivList.add( new String[] {"Joe", "Jo"} );
        
       
    }
    
    public static synchronized NameEquivalents getInstance(long searchId)
    {
        if( equivInstance == null )
        {
            equivInstance = new NameEquivalents(searchId);
        }
        
        return equivInstance;
    }
   
    
    public String[] getEquivalent( String originalStr )
    {
    	String[] equivalents = null;
    	
    	if(originalStr == null)
    		return equivalents;
        /*
         * returns the equivalent of originalStr using the equivalent lists
         */
        @SuppressWarnings("unused")
		String equivalent = "";
        int noOfEquiv = 0;
        
        
        boolean found = false;

        for( int i = 0 ; i < equivList.size() ; i ++ ){
              equivalents = (String[]) equivList.elementAt( i );
              for( int j = 0 ; j < equivalents.length ; j++ ){
                  if( originalStr.equalsIgnoreCase( equivalents[j] ) ){
                       noOfEquiv = equivalents.length - 1;
                       found = true;
                       break;
                   }
               }
              if (found){
            	  break;
              }
         }
            
        if(!found){
        	equivalents = null;
         }
        
        if(found){
            IndividualLogger.info( "NameEquivalents: found " + noOfEquiv + " equivalences ", searchId );
        }
        
        return equivalents;
    }
   
}