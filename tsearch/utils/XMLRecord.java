package ro.cst.tsearch.utils;

import java.util.HashMap;
import java.util.Hashtable;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/*
 * Use this class to map the DASL xml objects
 */
public class XMLRecord {

	HashMap<String, String> elements = new HashMap<String, String>();
    HashMap<String, Hashtable<String, String>> attributes = new HashMap<String, Hashtable<String, String>>();
	
	Node node;
	
	public XMLRecord (Node node) {
		
		this.node = node;
		
		NodeList nodes = node.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			addNode(nodes.item(i), null);
		}
	}
	
	private void addNode(Node node, String prefix) {

		String name = node.getNodeName();
		
		if ( node.getNodeType() == Node.TEXT_NODE )
			name = prefix;
		else if ( prefix != null )
			name = prefix + "." + name;
        
        if( name != null )
        {
            name = name.trim();
        }
		
		if ( node.hasChildNodes() ) {
			
			NodeList nodes = node.getChildNodes();

			for (int i = 0; i < nodes.getLength(); i++) 
			{
				Node child = nodes.item(i);
				addNode(child, name);
			}
		}
		else
		{
			String value = node.getNodeValue();

			if ( value == null )
				value = "";
			
			if ( name != null )
            {
				elements.put( name, value.trim() );
                NamedNodeMap namedNodeMap = node.getAttributes();
                if( namedNodeMap != null )
                {
                    Hashtable<String, String> allAttributes = new Hashtable<String, String>();
                    for( int i = 0 ; i < namedNodeMap.getLength() ; i ++ )
                    {
                        Node attr = namedNodeMap.item( i );
                        
                        allAttributes.put( attr.getNodeName(), attr.getNodeValue() );
                    }
                    this.attributes.put( name, allAttributes );
                }
            }
		}
	}
	
	public Node getNode() {
		return node;
	}
	
	public String getText(String name) {
		
		String attribute = elements.get( name );
		
		if (attribute != null)
			return attribute;
		
		return "";
	}
    
    public String getAttribute( String name, String attributeName )
    {
        String attribute = "";
        
        if( attributes.containsKey( name ) )
        {
            attribute =  attributes.get( name ).get( attributeName );
        }
        
        if (attribute != null)
            return attribute;
        
        return "";
    }
	
	public String getTextContent() {
		return node.getTextContent();
	}
	
	public String toString() {
		return elements.toString();
	}
}