package ro.cst.tsearch.extractor.xml;

import java.io.File;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import ro.cst.tsearch.corrector.HTMLCorrector;
import ro.cst.tsearch.servers.HashCountyToIndex;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.utils.InstanceManager;

public class XMLExtractor { 
	
	
	protected static final Category logger = Logger.getLogger(XMLExtractor.class);
	
    protected Document ruleDoc, parseDoc;
    protected ResultMap def=new ResultMap();
    protected String htmlString, context;
    protected Object[] rulePm;
    protected boolean isParentSite = false;
    
    //the name of the xml file that commands the parser
    private String parserConfigurationFilePrefix = null;
    
    //overwrite the  value for the  parserConfigurationFileName field when has different  value that empty_string or null
    //because we have a funy parser arhitecture we need in some cases like Parser.PAGE_ROWS_NAME2 to use this and not the other one
    //find more details in ro.cst.tsearch.servers.response.Parser class
   
    protected long searchId=-1;
    
    public XMLExtractor(String html, 
    		
    		Object[] o, 
    		
    		String c,long searchId, String parserConfigurationFileName, boolean isParentSite) {
    	//this.miServerID = miServerID;
        htmlString=html;
        rulePm=o;
        context=c;
        ruleDoc=null;
        this.searchId = searchId;
        this.parserConfigurationFilePrefix = parserConfigurationFileName;
        this.isParentSite = isParentSite;
       
    }
    
    public XMLExtractor(String html, Document rules, String c,long searchId,
    		String parserConfigurationFileName) {
        htmlString=html;        
        context=c;
        ruleDoc=rules;
        this.searchId = searchId;
        this.parserConfigurationFilePrefix = parserConfigurationFileName;
      
    }

    public Document getRuleDocument() {
        return ruleDoc;
    }

    public Document getParseDocument() {
        return parseDoc;
    }

    public ResultMap getDefinitions() {
        return def;
    }
    
    public void process() throws Exception {
        try {
            if(parseDoc==null){
           		parseDoc = XMLUtils.read(HTMLCorrector.correct(htmlString));            
            }
//            ruleDoc = null;
			if(ruleDoc==null){
					
					File f =null;
					
					f = new File(getRuleFile());
					
				    ruleDoc=XMLUtils.read(f, getRulePath()); 
			}
			if(isParentSite)
				def.put("tmp_isParentSite", "true");
            ActionFactory.createAction(this, ruleDoc.getDocumentElement(),searchId).processException();
            removeTempDef();
            def.setReadOnly();
        } catch (Exception e) {
            HTMLCorrector.logException(e, htmlString, context,searchId);
            throw e;
        }
    }
    
    public void process(String ruleFile) throws Exception {
        try {
            if(parseDoc==null){
           		parseDoc = XMLUtils.read(HTMLCorrector.correct(htmlString));            
            }
			if(ruleDoc==null){
					
					File f =null;
					
					f = new File(ruleFile);
					
				    ruleDoc=XMLUtils.read(f, getRulePath()); 
			}
			if(isParentSite)
				def.put("tmp_isParentSite", "true");
            ActionFactory.createAction(this, ruleDoc.getDocumentElement(),searchId).processException();
            removeTempDef();
            def.setReadOnly();
        } catch (Exception e) {
            HTMLCorrector.logException(e, htmlString, context,searchId);
            throw e;
        }
    }    

    protected void removeTempDef() {
         Iterator it=def.entrySetIterator();
         while (it.hasNext()) {
             Entry e=(Entry)it.next();
             if (((String)e.getKey()).startsWith("tmp"))
                 it.remove();
         }
    }

    protected String getRulePath() {
        return context+"WEB-INF/classes/rules";
    }

    protected File getRuleFile(String fileName) throws Exception {
    	return new File(fileName);
    }
    
    protected String getRuleFile() throws Exception {
        
    	long serverID=((Long)rulePm[0]).intValue();
        
    	DataSite data =  HashCountyToIndex.getDateSiteForMIServerID( 
    			InstanceManager.getManager().getCommunityId(searchId), 
    			serverID );
    	if(data == null) {
    		 throw new Exception("Unknown serverId : "+serverID);
    	}
    
    	String fileParserNameSufix = data.getParserFilenameSufix();
    	
        String suffix = null;
        
        if (rulePm.length>1){
            suffix=(String)rulePm[1];
        }
        
        String fileName = data.getName() ;

        String ret = null ;
        String path = getRulePath(); 
        
        if (suffix==null) {
        	if( fileParserNameSufix != null ){  //take the data from database
        		ret = path+File.separator+fileParserNameSufix+parserConfigurationFilePrefix+".xml";
        	}
        	else{//old mechanism should be replaced
        		ret=path+File.separator+fileName+".xml";
        	}
        }
        else{	// intermediar results
        	if( suffix.equals("")){ //take the data from database
        		ret = path+File.separator+"inter"+File.separator+fileParserNameSufix+parserConfigurationFilePrefix+".xml";
        	}
        	else{//old mechanism should be replaced
        		ret=path+File.separator+"inter"+File.separator+fileName+suffix+".xml";
        		System.err.println(" ///////////// --------- ////////  Please modify the class coresponding to "+fileName+suffix+".xml to  support new changes to the parser");
        	}
        }

        return ret;
    }
    
    public XMLExtractor(Document ruleDoc, Document parseDoc,long searchId) {
    	this.parseDoc = parseDoc;
    	this.ruleDoc = ruleDoc;
        this.searchId = searchId;
        this.context = BaseServlet.REAL_PATH;
    }
    
    /**
     * 
     * @param pr
     * @param parseDoc
     * @param ruleDoc
     * @param searchId
     */
    public static void parseXmlDoc(ParsedResponse pr, Document parseDoc, Document ruleDoc, long searchId){
    	try{
	    	XMLExtractor xmle = new XMLExtractor(ruleDoc, parseDoc, searchId);
	    	xmle.process(); 
	    	ResultMap resultMap = xmle.getDefinitions();    
	    	Bridge bridge = new Bridge(pr, resultMap, searchId);
	    	pr.setDocument(bridge.importData());
    	}catch(Exception e){
    		e.printStackTrace();
    		throw new RuntimeException(e);
    	}
    }

	public boolean isParentSite() {
		return isParentSite;
	}

	public void setParentSite(boolean isParentSite) {
		this.isParentSite = isParentSite;
	}
     
}
