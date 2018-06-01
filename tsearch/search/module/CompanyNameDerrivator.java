package ro.cst.tsearch.search.module;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.MatchEquivalents;

public class CompanyNameDerrivator extends ModuleStatesIterator
{
    private static final Category logger = Category.getInstance(ModuleStatesIterator.class.getName());
    public static Vector companyNameAbbv = null;
    public static final String xmlFilePath = BaseServlet.FILES_PATH +  "companyNames.xml";
    
    private List companyNameList = new ArrayList();
    private long searchId=-1;
    private boolean insertComma=false; 
    
    public CompanyNameDerrivator(long searchId)
    {
        super(searchId);
        this.searchId = searchId;
        this.insertComma = true;
        loadAbbv();
    }
    
    protected void initInitialState(TSServerInfoModule initial)
    {
        super.initInitialState(initial);
        SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
        String companyName = sa.getAtribute( SearchAttributes.OWNER_LNAME ).trim().toUpperCase();
        
        // bug #808 (CR): enable/disable derivations
		boolean  nameDerrivation = true;
		try{
			nameDerrivation = HashCountyToIndex.getCrtServer(searchId, false).isEnabledNameDerivation(sa.getCommId());
		}catch(Exception e){
			e.printStackTrace();
		}
		
		if(nameDerrivation){
			companyNameList = buldCompanyNameList(companyName,searchId);
		}else{
			companyNameList = new ArrayList();
			companyNameList.add(companyName);
		}
    }

    protected void setupStrategy()
    {
        StatesIterator si ;
        si = new DefaultStatesIterator( companyNameList );
        setStrategy(si);
    }
    
    public Object current()
    {
        String companyName = ((String) getStrategy().current());
        TSServerInfoModule crtState = new TSServerInfoModule(initialState);

        for (int i =0; i< crtState.getFunctionCount(); i++)
        {
            TSServerInfoFunction fct = crtState.getFunction(i);
            if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_COMPANY_NAME)
            {
                fct.setParamValue(companyName);
            }
        }
        return  crtState ;
    }
    
    public static List<String> buldCompanyNameList( String originalCompanyName,long searchId )
    {
        //build the list for this iterator
        List companyNames = new ArrayList();
        
        //make the direct replacements from MatchEquivalences
        originalCompanyName = MatchEquivalents.getInstance(searchId).makeDirectReplacements( originalCompanyName );
        
        //split the original name into single word tokens
        originalCompanyName=originalCompanyName.replaceAll("\\bL\\s*L\\s*C\\b", "LLC");
        originalCompanyName=originalCompanyName.replaceAll("['\"\\.]+", "");
        originalCompanyName = originalCompanyName.replaceAll("\\bU\\s+S\\b", "US");
        originalCompanyName = originalCompanyName.replaceAll("\\bN\\s+A\\b", "NA");
        String tokens[] = originalCompanyName.replaceAll("\\s{2,}", " ").trim().split( " " );
        
        Vector tokenReplacementsVector = new Vector();
        Vector tokenReplacementsIndexes = new Vector();
        
        //for each token
        for( int i = 0 ; i < tokens.length ; i++ )
        {
            //find a list of replacements
            List replacements = null;
            try {
            	replacements = new ArrayList (findToken( tokens[i] ));
            } catch (Exception e) {
			}
            
            if( replacements == null )
            {
                replacements = new ArrayList();
                
                replacements.add( tokens[i] );
            }

            if ( tokens.length>=2 && i == tokens.length-2 )
            {
            	if (tokens[tokens.length-1].toLowerCase().startsWith("inc"))
            	{
            		//List replacementsAux = replacements.subList(0, 1);
            		ArrayList<String> replacementsAux = new ArrayList<String>();
            		
            		for (int j=0 ; j<replacements.size() ; j++)
            		{
            			String currentRepl = replacements.get(j).toString();
            			if(!currentRepl.endsWith(",") && currentRepl.length()>0) {
            				replacementsAux.add(currentRepl+",");
            			}
            		}
            		replacements.addAll(replacementsAux);
            	}
            }
            
            tokenReplacementsVector.add( new Vector(replacements) );
            tokenReplacementsIndexes.add( new BigDecimal( 0 ) );
        }
        
        //generate all possible combinations
        boolean done = false;
        while(!done)
        {

            String nameToAdd = "";
            
            for( int i = 0 ; i < tokenReplacementsVector.size() ; i++ )
            {
                Vector currentTokenReplacements = (Vector) tokenReplacementsVector.elementAt( i );
                BigDecimal currentTokenReplacementIndex = (BigDecimal) tokenReplacementsIndexes.elementAt( i );
                
                nameToAdd += (String) currentTokenReplacements.elementAt( currentTokenReplacementIndex.intValue() );
                
                if( i != tokenReplacementsVector.size() - 1 )
                {
                    nameToAdd += " ";
                }
                
                if( i == tokenReplacementsVector.size() - 1 && currentTokenReplacementIndex.intValue() < currentTokenReplacements.size() - 1 )
                {
                    tokenReplacementsIndexes.setElementAt( currentTokenReplacementIndex.add( new BigDecimal( 1 ) ), i );
                }
                
                if( i == tokenReplacementsVector.size() - 1 && currentTokenReplacementIndex.intValue() == currentTokenReplacements.size() - 1 )
                {
                    //reached the end of vector
                    //try to increase de previous token iterator by one
                    
                    //reset current iterator
                    tokenReplacementsIndexes.setElementAt( new BigDecimal( 0 ), i );
                    
                    //try to increase the previous iterator
                    int j = i - 1;
                    for( ; j >= 0 ; j-- )
                    {
                        BigDecimal previousIterator = (BigDecimal) tokenReplacementsIndexes.elementAt( j );
                        Vector previousReplacements = (Vector) tokenReplacementsVector.elementAt( j );
                        
                        if( previousIterator.intValue() < previousReplacements.size() - 1 )
                        {
                            previousIterator = previousIterator.add( new BigDecimal( 1 ) );
                            tokenReplacementsIndexes.setElementAt( previousIterator, j );
                            break;
                        }
                        else
                        {
                            tokenReplacementsIndexes.setElementAt( new BigDecimal( 0 ), j );
                        }
                    }
                    
                    //if we reached the start of the list --> done
                    if( j < 0 )
                    {
                        done = true;
                    }
                }
            }
            
            companyNames.add( nameToAdd.trim() );
            //System.out.println( nameToAdd.trim() );
            if(companyNames.size() > 25){
            	break;
            }
        }
        
        return companyNames;
    }
    
    public static synchronized void loadAbbv()
    {
        if( companyNameAbbv != null )
        {
            return;
        }
        
        logger.info( "Loading company Names abbreviations list from " + xmlFilePath );
        
        companyNameAbbv = new Vector();
        
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            
			//try to open it from database first
			byte[] companyNamesData = DBManager.getFileContentsFromDb( "companyNames.xml" );
			ByteArrayInputStream bais = null;
			if( companyNamesData != null ){
				//data found in the database
				
				bais = new ByteArrayInputStream( companyNamesData );
			}
			else{
				//data not in database.... open it from disk and save it to database for further reads
				
				String fileDataString = FileUtils.readFile( xmlFilePath );
				DBManager.writeFileContentsToDb( "companyNames.xml" , fileDataString.getBytes());
				
				bais = new ByteArrayInputStream( fileDataString.getBytes() );
			}
            
            Document document = builder.parse( bais );
			
//            Document document = builder.parse(new File( xmlFilePath ));
            Node root = document.getFirstChild();
            
            NodeList companyNames = root.getChildNodes();
            for (int i = 0; i < companyNames.getLength(); i++ )
            {
                Node companyNameNode = companyNames.item(i);
                if (companyNameNode.getNodeType() == Node.ELEMENT_NODE)  
                {
                    String companyFirstToken = ((Element) companyNameNode).getAttribute( "name" );
                    String nodeName = companyNameNode.getNodeName();
                    
                    List abbreviations = new ArrayList();
                    abbreviations.add( companyFirstToken );
                    
                    NodeList companyNameAbbrevChildren = companyNameNode.getChildNodes();
                    for (int j = 0; j < companyNameAbbrevChildren.getLength(); j++ )
                    {
                        Node companyNameNodeAbbrev = companyNameAbbrevChildren.item(j);
                        if (companyNameNodeAbbrev.getNodeType() == Node.ELEMENT_NODE)  
                        {
                            if( "abbreviation".equals( companyNameNodeAbbrev.getNodeName() ) )
                            {
                                String abbreviation = ((Element) companyNameNodeAbbrev).getAttribute( "name" );
                                abbreviations.add( abbreviation );
                            }
                        }
                    }
                    
                    companyNameAbbv.add( abbreviations );
                }
            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
        
        logger.info( "Loading company Names abbreviations done!" );
    }
    
    public static List findToken( String token )
    {
        //searches the list vector for the token
        //if token found, the entire list is returned
        
        if( companyNameAbbv == null )
        {
            loadAbbv();
            
            if( companyNameAbbv == null )
            {
                return null;
            }
        }
        
        for( int i = 0 ; i < companyNameAbbv.size() ; i ++ )
        {
            List l = (List) companyNameAbbv.elementAt( i );
            
            Iterator iter = l.iterator();
            while( iter.hasNext() )
            {
                String abbreviation = (String) iter.next();
                
                if( token.equalsIgnoreCase( abbreviation ) )
                {
                    return l;
                }
            }
        }
        
        //token not found
        return null;
    }
}
