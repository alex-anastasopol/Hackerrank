package ro.cst.tsearch.extractor;

/*
 * extractor.java
 *
 * author: 
 * 
 * Created on July 12, 2003, 10:02 PM
 */

//Deprecated
import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.tree.DefaultTreeModel;

import org.apache.log4j.Category;

import ro.cst.tsearch.database.ConnectionPool;
import ro.cst.tsearch.database.DBConnection;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.wrapper2.ExampleFileFilter;
import ro.cst.tsearch.wrapper2.RuleTreeManipulator;


public class extractor extends javax.swing.JApplet implements Runnable { 
	
	private static final long serialVersionUID = 2381787123997480371L;
	private static final Category logger= Category.getInstance(extractor.class.getName());
	
    /** Creates new form extractor */
    public extractor() {
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        lb_html = new javax.swing.JLabel();
        jb_browse_html = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        lb_rules = new javax.swing.JLabel();
        jb_browse_rules = new javax.swing.JButton();
        jb_rules = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jb_extract = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        outPanel = new javax.swing.JTextPane();
        
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jPanel1.setLayout(new java.awt.GridLayout(0, 3));
        
        jPanel1.setMaximumSize(new java.awt.Dimension(750, 100));
        jPanel1.setMinimumSize(new java.awt.Dimension(750, 100));
        jPanel1.setPreferredSize(new java.awt.Dimension(750, 100));
        jLabel1.setText("File :");
        jPanel1.add(jLabel1);
        
        lb_html.setText(" ");
        jPanel1.add(lb_html);
        
        jb_browse_html.setText("Browse...");
        jb_browse_html.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb_browse_htmlActionPerformed();
            }
        });
        
        jPanel1.add(jb_browse_html);
        
        //adding labels for database retrieval
		jPanel1.add(lb_servers);
		jPanel1.add(lb_pages);
		jPanel1.add(go);
        /////////////////////////////
        //adding visual controls for rules identification

		Server_id_list.addItemListener(new java.awt.event.ItemListener() {
			public void itemStateChanged(java.awt.event.ItemEvent evt) {
				Server_id_listItemStateChanged(evt);  
			}
		});

        
        
        
        jPanel1.add(Server_id_list);
		jPanel1.add(Pages_id_list);
		jb_fetch.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						jb_fetchActionPerformed();
					}
				});
		jPanel1.add(jb_fetch);
		////////////////////////////////////////////////////
        
        jLabel3.setText("Tree of Rules:");
        jPanel1.add(jLabel3);
        
        lb_rules.setText(" ");
        jPanel1.add(lb_rules);
        
        jb_browse_rules.setText("Browse...");
        jb_browse_rules.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb_browse_rulesActionPerformed();
            }
        });
        
        jPanel1.add(jb_browse_rules);
        
        jb_rules.setText("Show Rules");
        jb_rules.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb_rulesActionPerformed();
            }
        });
        
        jPanel1.add(jb_rules);
        
        jLabel5.setText(" ");
        jPanel1.add(jLabel5);
        
        jb_extract.setText("Extract");
        jb_extract.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jb_extractActionPerformed();
            }
        });
        
        jPanel1.add(jb_extract);
        
        jSplitPane1.setLeftComponent(jPanel1);
        
        jScrollPane1.setMaximumSize(new java.awt.Dimension(750, 400));
        jScrollPane1.setMinimumSize(new java.awt.Dimension(750, 400));
        jScrollPane1.setPreferredSize(new java.awt.Dimension(750, 400));
        outPanel.setMinimumSize(new java.awt.Dimension(750, 400));
        outPanel.setPreferredSize(new java.awt.Dimension(750, 400));
        jScrollPane1.setViewportView(outPanel);
        
        jSplitPane1.setRightComponent(jScrollPane1);
        
        getContentPane().add(jSplitPane1, java.awt.BorderLayout.CENTER);
        
    }//GEN-END:initComponents
	private void Server_id_listItemStateChanged(java.awt.event.ItemEvent evt)
	{
		if(evt.getStateChange()==ItemEvent.SELECTED && srv_flag==1)
	    {
	       logger.info("New Server id selected"+evt.getItem());	
	       getPageIDs();
	    }			
	}

    private void jb_extractActionPerformed() {//GEN-FIRST:event_jb_extractActionPerformed
        // Add your handling code here:
        /*
         int i;
        String result;
         wrapper w=new wrapper(file_html,file_rules);
	 w.printstatus();	  
	 w.LoadHTMLfile();
	 w.LoadRulesfile();
	 
	 String rez[]=w.getAvailableRules();
         result=new String("The Data extracted :\n");
         for(i=0;i<rez.length;i++)
         {             
             logger.info("Caut cu regula:"+rez[i]);
             result+=rez[i]+" : ["+w.ExtractFieldValue2(rez[i])+"]";
             result+="\n";
         }
         
         */
         //w.parse_html();
         wp.LoadHTMLfile(file_html);
         ParsedResponse pr=new ParsedResponse();
         try {
         	String sir=new String(wp.html_to_string());
         	if(sir==null)
         	     logger.info("oooops null string!!");
         	/*
			logger.info("Start corector!!");
         	if(use_corector==1)
         	      sir=HTMLCorrector.correctAndVerify(sir,"C:\\Temp\\");
			logger.info("End corector!!");     
         	  */   
         	  /*
			Vector tokens;
		    my_parser p=new my_parser(new ByteArrayInputStream(sir.getBytes()));
			tokens=p.getResult();
			*/
			 
			wp.Parse_HTML(pr,sir,wp.getModel());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         //outPanel.setText(result);
         outPanel.setText(wp.out);
    }//GEN-LAST:event_jb_extractActionPerformed

    private void jb_rulesActionPerformed() {//GEN-FIRST:event_jb_rulesActionPerformed
        // Add your handling code here:        
        String result=new String("Rules found are:\n");
         
         if(wp!=null)
		   result+=wp.getRulesFromModel();
		outPanel.setText(result);		
    }//GEN-LAST:event_jb_rulesActionPerformed

	private void jb_fetchActionPerformed() {
		//this function fetches the TreeModel from Database coreponding to current
		//Server_ID and PAGE_id
	    
	    DBConnection conn = null;
		try
		{
		    		//			connect to database
		    	conn = ConnectionPool.getInstance().requestConnection();
				  String stm;                   
				  logger.info("Connecting to DB....");
				  logger.info("done connect");
				  
				  //get blob id
				  stm="SELECT TREE_RULES_ID FROM TS_EXTRULES WHERE"
				  +" SERVER_ID="+Server_id_list.getSelectedItem()
				  + " AND "
				  +" PAGE_ID="+ Pages_id_list.getSelectedItem();
		
				  DatabaseData data=conn.executeSQL(stm);
		
				  stm="SELECT DATA FROM TS_EXTRULES_ATTACH WHERE"
				  +" ID=?";
				  //+data.getValue(1,0);	
				  logger.info("Model id:"+data.getValue(1,0));		
				  //get blob
				  
				  
				  PreparedStatement pStmt = conn.prepareStatement(stm);
				  pStmt.setLong(1, Long.parseLong(data.getValue(1, 0).toString()));
				  ResultSet resultBlob = pStmt.executeQuery();
				  //read tree model
				  
				  if (resultBlob.next()) {
					  //resultBlob.getBytes(1);
					  
					  Blob blobtree = resultBlob.getBlob(1);
					  InputStream IStream = ( (com.mysql.jdbc.Blob)blobtree).getBinaryStream();								
					  ObjectInputStream Objin = new ObjectInputStream(IStream);	
		
					  model=(DefaultTreeModel)Objin.readObject();
   
					  Objin.close();
					  IStream.close();
					  resultBlob.close();
					  pStmt.close();
					  if(model!=null) {
						  logger.info("Model has smth");
					  }
					  else
					  {
						  logger.info("Model is null! Ooops");															  	
					  }
				}
													  
				//init wrapper
				wp=new wrapper();
				//set tree model
				wp.setModel(model);				
				//display found rules
				stm=new String("The extraction rules found (leaves of the tree):\n");
				stm+=wp.getRulesFromModel();
				outPanel.setText(stm);
													  
		
		}
		catch (Exception e)
		{
				logger.error("Ooops read model");
				e.printStackTrace();
		}finally{
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    e.printStackTrace();
			}			
		}
				
		
	}
    private void jb_browse_rulesActionPerformed() {//GEN-FIRST:event_jb_browse_rulesActionPerformed
        // Add your handling code here:
         if(last_path_rules==null)                        
		  	    jc_rules=new JFileChooser();                        
        else
                            jc_rules=new JFileChooser(last_path_rules); 
         //file filter settings
                               jc_rules.addChoosableFileFilter(filter_rules);
                               jc_rules.setAcceptAllFileFilterUsed(true);
                               jc_rules.setFileFilter(filter_rules);
                               
		  	if(jc_rules.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)
		  	{
		  		try
		  		   {
		  		   	 file_rules=jc_rules.getSelectedFile();		  	          
		  	            if(file_rules!=null)
		  	             {                                         
                                         last_path_rules=file_rules.getPath();                               		  	                               
		  	          	 lb_rules.setText(file_rules.getName());
		  	          	 wp=new wrapper();
		  	          	 RuleTreeManipulator rtm=new RuleTreeManipulator();
		  	          	 rtm.setF(file_rules);
		  	          	 model=rtm.ReadFromFile();
		  	          	 wp.setModel(model);
		  	          	 
		  	          	 
		  	          	 
                                     }
                                    }     
                                 catch(Exception e)
                                 {
                                     logger.error("Exception Open Rules:"+e);
                                 }
                        }    
    }//GEN-LAST:event_jb_browse_rulesActionPerformed

    private void jb_browse_htmlActionPerformed() {//GEN-FIRST:event_jb_browse_htmlActionPerformed
        // Add your handling code here:
        if(last_path_html==null)                        
		  	    jc_html=new JFileChooser();                        
        else
                            jc_html=new JFileChooser(last_path_html); 
         //file filter settings
                               jc_html.addChoosableFileFilter(filter_html);
                               jc_html.setAcceptAllFileFilterUsed(true);
                               jc_html.setFileFilter(filter_html);
                               
		  	if(jc_html.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)
		  	{
		  		try
		  		   {
		  		   	 file_html=jc_html.getSelectedFile();		  	          
		  	            if(file_html!=null)
		  	             {                                         
                                         last_path_html=file_html.getPath();                               		  	                               
		  	          	 lb_html.setText(file_html.getName());
                                     }
                                    }     
                                 catch(Exception e)
                                 {
                                     logger.error("Exception Open HTML:"+e);
                                 }
                        }       
    }//GEN-LAST:event_jb_browse_htmlActionPerformed

    public void destroy() {
    }    

    public java.lang.String getAppletInfo() {
        return "Info";
    }    

    public void init() {
      //----file path settings---------
      last_path_html=null;      
      filter_html=new ExampleFileFilter(new String[]{"htm","html"},"HTML files");
      
      //----file path settings---------
      last_path_rules=null;      
      filter_rules=new ExampleFileFilter(new String("txt"),"Trees with rules files");
      
    }
    
    public void start() {
        if(ourThread==null)
  	  {
  	  	  ourThread=new Thread(this);
  	  	  ourThread.start();
  	  }
    }
    
    public void stop() {
         if(ourThread!=null)
  	  {
  	  	  //ourThread.stop(); 
  	  	  ourThread=null;  	  	  
  	  }
    }
    public void run()
    {
        while(true)
        {
            
            try
  	 	{  	 	
  	 		            //no html file loaded or no model of rules present
                        if((lb_html.getText()).equals(" ") || model==null)
                        {
							jb_rules.setEnabled(false);
						    jb_extract.setEnabled(false);
                        }
                        else
                        {
							jb_rules.setEnabled(true);
							jb_extract.setEnabled(true);
                        }
                        /*
                        if((lb_html.getText()).equals(" ") || (lb_rules.getText()).equals(" "))
                        {
                             jb_rules.setEnabled(false);
                             jb_extract.setEnabled(false);
                        }
                        else
                        {
                               jb_rules.setEnabled(true);
                               jb_extract.setEnabled(true);
                        } */               
            
  	 		Thread.sleep(500);
  	 	}  	 	
  	 	catch(InterruptedException e)
  	 	{
  	 		stop();
  	 	}
            
        }
        
    }
     
  private void getPageIDs()
  {
  	
    DBConnection conn = null;
	try{
			  //connect to database
			//get connection
			String stm;                   
			logger.info("Connecting to DB....");			
			conn = ConnectionPool.getInstance().requestConnection();
			logger.info("done connect");    
		//extract from database page id's for first server
		  stm="SELECT PAGE_ID FROM TS_EXTRULES WHERE SERVER_ID="+mServer.getSelectedItem();		  
		  DatabaseData data = conn.executeSQL(stm);
		  int n=data.getRowNumber();
	  
	     mPages.removeAllElements();
//		  set Pages Combo model
			for(int i=0;i<n;i++)
			  mPages.addElement(data.getValue(1,i));
		}  	
		catch(Exception e)
		{
		  logger.error("OOOoops fetching server id's: "+e);
		}finally{
			try{
			    ConnectionPool.getInstance().releaseConnection(conn);
			}catch(BaseException e){
			    e.printStackTrace();
			}			
		}
  	   
  }
  public void getServerIDs()
  {
      DBConnection conn = null;
  	try{
          //connect to database
		//get connection
		String stm;                   
		logger.info("Connecting to DB....");
		conn = ConnectionPool.getInstance().requestConnection();		
		logger.info("done connect");    
		  //retrieve all DISTINCT server ID's
		  stm="SELECT DISTINCT SERVER_ID FROM TS_EXTRULES";		  
		DatabaseData data = conn.executeSQL(stm);
		int n=data.getRowNumber();  
		  //build vector from results
		srv_flag=0;
		  mServer.removeAllElements();
		  //Vector numbers=new Vector();
//		set Servers Combo model
		  for(int i=0;i<n;i++)
		    mServer.addElement(data.getValue(1,i));
		    
		srv_flag=1;
		/*
	//extract from database page id's for first server
	  stm="SELECT PAGE_ID FROM TS_EXTRULES WHERE SERVER_ID="+data.getValue(1,0);		  
	  data = conn.executeSQL(stm);
	  n=data.getRowNumber();
	  
	
//	  set Pages Combo model
		for(int i=0;i<n;i++)
		  mPages.addElement(data.getValue(1,i));
	  
	  */
	  getPageIDs();
		    
  	}  	
  	catch(Exception e)
  	{
  	  logger.error("OOOoops fetching server id's: "+e);
  	}finally{
		try{
		    ConnectionPool.getInstance().releaseConnection(conn);
		}catch(BaseException e){
		    e.printStackTrace();
		}			
	}
       
  }
    /** finally, the main thing */
  public static void main(String args[])
  {
     javax.swing.JFrame frame=new javax.swing.JFrame("Extractor - Application or applet");
      
      //inner class to exit when close button is hit
      class FrameClose extends WindowAdapter{
      	public void windowClosing(WindowEvent e)
      	{
      		System.exit(0);
      	}
      }
     frame.addWindowListener(new FrameClose());
     extractor applet=new extractor();
     applet.applic=true;
     applet.getServerIDs();
     
     frame.getContentPane().add(applet,BorderLayout.CENTER);
     frame.setSize(780,580);
     applet.init();
     applet.start();
     frame.setVisible(true);
     
  }   
  /** our Thread */
  private Thread ourThread=null;
  /** flag to signal if running as application */
  private boolean applic=false;
  /** File Chooser for HTML */
  private JFileChooser jc_html;
  /** Path memory for HTML */
  private String last_path_html;
  /** File filter for HTML File open File dialog */
  private ExampleFileFilter filter_html;
  /** File Chooser for HTML */
  private JFileChooser jc_rules;
  /** Path memory for HTML */
  private String last_path_rules;
  /** File filter for Rules File open File dialog */
  private ExampleFileFilter filter_rules;
  
  /** HTML file object **/
  private File file_html;
  /** Rules file object **/
  private File file_rules;
    
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JSplitPane jSplitPane1;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel lb_html;
  private javax.swing.JButton jb_browse_html;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JLabel lb_rules;
  private javax.swing.JButton jb_browse_rules;
  private javax.swing.JButton jb_rules;
  private javax.swing.JLabel jLabel5;
  private javax.swing.JButton jb_extract;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JTextPane outPanel;
  // End of variables declaration//GEN-END:variables
  private JLabel lb_servers=new JLabel("Server ID");
  private JLabel lb_pages=new JLabel("Page ID");
  private JLabel go=new JLabel("Fetch rules");
  
  private DefaultComboBoxModel mServer=new DefaultComboBoxModel();
  private int srv_flag=1;   //semafor event 
  
  
  // 1 use corector
  // 0 don't use
  private final int use_corector=1;
  
  private DefaultComboBoxModel mPages=new DefaultComboBoxModel();
  private JComboBox Server_id_list=new JComboBox(mServer);
  private JComboBox Pages_id_list=new JComboBox(mPages);
  private JButton   jb_fetch=new JButton("Fetch rules");
  private DefaultTreeModel model;
  private wrapper wp;
}
