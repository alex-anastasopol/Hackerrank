/*
 * 
 * author:  
 *
 *deprecated
 */
package ro.cst.tsearch.instructor;

//WARNING:
// - on JDK 1.3.1 uses interface KeyListener  
// - on JDK 1.4.0+ uses interface KeyEventDispatcher

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.DefaultKeyboardFocusManager;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;

import ro.cst.tsearch.wrapper2.ExampleFileFilter;
import ro.cst.tsearch.wrapper2.HTML_region;
import ro.cst.tsearch.wrapper2.RuleTreeManipulator;
import ro.cst.tsearch.wrapper2.limits;
import ro.cst.tsearch.wrapper2.my_node;
import ro.cst.tsearch.wrapper2.my_parser;
import ro.cst.tsearch.wrapper2.my_rule;
import ro.cst.tsearch.wrapper2.my_token;
import ro.cst.tsearch.wrapper2.nodeDialog;
import ro.cst.tsearch.wrapper2.nodePropertiesDialog;
import ro.cst.tsearch.wrapper2.node_constants;

/**This is the main application/applet class */
public class app extends JApplet implements ActionListener,TreeSelectionListener,KeyEventDispatcher,Runnable
{
	private static final Logger logger = Logger.getLogger(app.class);
	
	//using HTMLCorector
	// 	1 - use it
	//  0 - don't use it
	
   private final int use_corector=1;	
  /** our Thread */
  private Thread ourThread=null;
  /** flag to signal if running as application */
  private boolean applic=false;
 /**the content Panel; this is the container for all the GUI elements */   
  JSplitPane contentPane;
 /** Layout manager */
  BorderLayout borderLayout1 = new BorderLayout();
 /** Split Panel */
  JSplitPane jSplitPane1 = new JSplitPane();
 /** Split Panel */
  JSplitPane jSplitPane2 = new JSplitPane();  
 /** Scroll Panel - tokens*/
  JScrollPane jScrollPane1 = new JScrollPane();
 /** Scroll Panel - HTML file*/
  JScrollPane jScrollPane2 = new JScrollPane();
 /** Scroll Panel - Tree*/
  JScrollPane jScrollPane3 = new JScrollPane();

  /**  modulul pt generare reguli iterative **/
   iterative it_module;
  
  /** modul editor reguli**/
   ruleeditor re=null;  
   
  /** Editor Panel for HTML Display */
  JEditorPane html = new JEditorPane();  
  /** this is the token list from the HTML file **/
  private javax.swing.JList lista_tokeni;


  /* my vars  */
  //current profile
  /** this string contains the tokens currently displayed in the token-panel */
//  	private String buffered_tokens;
  /** this string contains the original html file*/	  	
	private String original_html_text;
  /** this string contains the html file with <font> tags inserted to mark the selection*/	  		
	private String new_html_text;
  /** this Vector contains the tokens parsed from the HTML File */
	private Vector rez;//results vector
  /** this int specifies the current profile number  (1,2,3 or 4);
      each profile has its own buffered_tokens,original_html_text,...
  */	
	private int crt_prof;
    
    /** flag - iterative module rule save **/
     private int it_flag;   
        
        //filechooser for html files
	/** File filter for HTML File open File dialog */
        private ExampleFileFilter filter;
	/** File Chooser for HTML */
        private JFileChooser jc;
	/** Path memory for HTML */
        private String last_path;
        
        //filechooser for tree-files
	/** File filter for Tree File open File dialog */
        private ExampleFileFilter tree_filter;
	/** File Chooser for Tree file */
        private JFileChooser tree_jc;
	/** Path memory for Tree file */
        private String tree_last_path;
		
	/** tree model ... Java stuff*/
	protected DefaultTreeModel treeModel;
	/** tree object to be displayed*/
	private JTree  tree;//arbore de reguli curent
	/** root of the tree (undeletable, always marked)*/	
	private my_node root;//radacina
	/** currently selected node */
	private my_node crt_node;
	/** node to be marked with the current selection*/
	private my_node next_node;
	/** token index in the rez Vector coreponding to the first token
	    displayed in the parsed Tokens panel
	*/
	private int start_tk;//nr token-ului de la care	
	                     //incepe afisarea in lista_tokeni
        /** token selection start */			     
	private int tk_inf;
	/** token selection end */			     
	private int tk_sup;//intervalul de token-i selectat    		
        
        
     
     //profiles data
        /** profiles proerties array */
        //private String prof_buffered_tokens[]=new String[my_node.MAX_PROF];
	/** profiles proerties array */
	private String  prof_original_html_text[]=new String[my_node.MAX_PROF];
	/** profiles proerties array */
        private Vector  prof_rez[]=new Vector[my_node.MAX_PROF];//results vector
                		
   //-----------end profiles
   /** semaphore variabile (to ignore certain events while updating display)*/   
    int enabled=0; //pt. a ignora evenimente ce apar
                   //in timpul executiei rutinelor de refresh 
    /** selection index */    
    int sel_idx1;
    /** selection index */    
    int sel_idx2;//indecsi selectie
    /** selection index (used for multiselect)*/    
    int orig_idx1;
    /** selection index (used for multiselect)*/    
    int orig_idx2;//indecsi originali in cazul multiselect        

   /*My controls*/
   /** add new node/mark node */
   JButton lb=new JButton("Add key");//...or Mark next
   /** build rules */
   JButton jb_build_rules=new JButton("Build Rules");
  /** insert DEFAULTTOKEN */
   JButton jb_ins_token=new JButton("Insert Token");//inserts token 
  /** switch to iterative rules interface */
   JButton jb_iterative=new JButton("Iterative module");
   /** switch to iterative rules interface */
   JButton jb_rule_editor=new JButton("Rule editor");
   /** switch to iterative rules interface */
   JButton jb_save_rules=new JButton("Save Rules");
   JButton jb_node_properties=new JButton("Node Attributes");
   
  /** Refresh HTML Panel */
   JButton rh=new JButton("Refresh HTML");  
   
   /** Radio Button for file 1 (main)*/
   JRadioButton jr1=new JRadioButton("1",true);
   /** Radio Button for file 2*/
   JRadioButton jr2=new JRadioButton("2",false);
   /** Radio Button for file 3*/
   JRadioButton jr3=new JRadioButton("3",false);
   /** Radio Button for file 4*/
   JRadioButton jr4=new JRadioButton("4",false);
   /** Group Radio Buttons */
   ButtonGroup bg=new ButtonGroup();
   
   /**Browse Button associated with Radio 1 */
   JButton jb1=new JButton("Browse...");//select file
   /**Browse Button associated with Radio 2 */
   JButton jb2=new JButton("Browse...");
   /**Browse Button associated with Radio 3 */
   JButton jb3=new JButton("Browse...");
   /**Browse Button associated with Radio 4 */
   JButton jb4=new JButton("Browse...");
   
   /** Searches the next key to be marked */
   JButton jbnext=new JButton("Next key");
   /** Save tree to a file */
   JButton sv_tree=new JButton("Save Tree");
   /** Load tree from a file */
   JButton ld_tree=new JButton("Load Tree");
   
   /** Save tree to a file */
	  JButton exp_tree=new JButton("Export Tree to Text");
	  /** Load tree from a file */
	  JButton imp_tree=new JButton("Import Tree from Text");
   
   /** Status label */
   JLabel status=new JLabel("Ready");
   /** Label to display the name of the key to be marked */
   JLabel key_name=new JLabel(" ");
   /** info Label - currently selected node */
   JLabel lb_crt_node=new JLabel();
   /** info Label - total number of nodes */
   JLabel lb_total_nodes=new JLabel(" ");
   /** info Label - total number of marked nodes */
   JLabel lb_marked=new JLabel(" ");
   /** info Label - number of marked nodes in the current sub-tree*/
   JLabel lb_crt_marked=new JLabel(" ");
   
   /** Split pane for Tree/HTML controls */
  JSplitPane jSplitPane3 = new JSplitPane();
  /** container */
  JPanel jPanel3 = new JPanel(new GridLayout(0,2)); 
  /** container */
  JPanel jPanel4 = new JPanel(new GridLayout(0,2));
  /** container */
  JPanel jPanel5 = new JPanel(new GridLayout(1,2));

  //---------------------------------

  //Construct the frame
  /** constructor */
  public app() {
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    try {
      jbInit();      
      //uncomment only for JDK 1.4.0+
      DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }
  //Component initialization
  /** initalization */
  private void jbInit() throws Exception  {
    
   //contentPane = (JPanel) this.getContentPane();
  
    //contentPane.setLayout(borderLayout1);
    this.setSize(new Dimension(750,550));
    //this.setTitle("Instructor");
    jSplitPane1.setOrientation(JSplitPane.VERTICAL_SPLIT);
    jSplitPane1.setDoubleBuffered(true);
  
    jSplitPane1.setPreferredSize(new Dimension(690, 490));
    jSplitPane1.setContinuousLayout(true);
    
    contentPane=jSplitPane1;
    
    //setContentPane(jSplitPane1);
    jScrollPane1.setPreferredSize(new Dimension(330, 300));
    jScrollPane2.setPreferredSize(new Dimension(330, 300));
    
    //for tree
    jScrollPane3.setPreferredSize(new Dimension(200, 180));
    
    html.setPreferredSize(new Dimension(300, 290));    
    html.setContentType("text/html");
    
    jSplitPane2.setMaximumSize(new Dimension(690, 280));
    jSplitPane2.setMinimumSize(new Dimension(690, 280));
    jSplitPane2.setPreferredSize(new Dimension(690, 280));
    
    jSplitPane3.setMaximumSize(new Dimension(690, 190));
    jSplitPane3.setMinimumSize(new Dimension(690, 190));
    jSplitPane3.setPreferredSize(new Dimension(690, 190));
    
    jPanel3.setMaximumSize(new Dimension(300, 180));
    jPanel3.setMinimumSize(new Dimension(300, 180));
    jPanel3.setPreferredSize(new Dimension(300, 180));
    
    
    jPanel5.setMaximumSize(new Dimension(300, 180));
    jPanel5.setMinimumSize(new Dimension(300, 180));
    jPanel5.setPreferredSize(new Dimension(300, 180));

    /* my init code */
    start_tk=0;
    tk_inf=0;
    tk_sup=0;
    
    
    //init lista tokeni
    lista_tokeni = new javax.swing.JList();
    lista_tokeni.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    
    crt_prof=1;
        

    root=new my_node("None",0,0);
    
    treeModel = new DefaultTreeModel(root);
    crt_node=root;
    tree=new JTree(treeModel);
    tree.addTreeSelectionListener(this);        
         
    html.setDoubleBuffered(true);    

     html.setEditable(false);
 


      lb.addActionListener(this);      
      jb_ins_token.addActionListener(this);      
      jb_build_rules.addActionListener(this);          
     
      
      rh.addActionListener(this);
                
      jr1.addActionListener(this);     
      jr1.setEnabled(false);
      jr2.addActionListener(this);   
      jr2.setEnabled(false);
      jr3.addActionListener(this);     
      jr3.setEnabled(false);
      jr4.addActionListener(this);
      jr4.setEnabled(false);
                            
      jb1.addActionListener(this);
      jb2.addActionListener(this);
      jb3.addActionListener(this);
      jb4.addActionListener(this);
      
      sv_tree.addActionListener(this);
      ld_tree.addActionListener(this);
      
	  exp_tree.addActionListener(this);
	  imp_tree.addActionListener(this);
      
      jbnext.addActionListener(this);
      jb_iterative.addActionListener(this);
      jb_rule_editor.addActionListener(this);
      jb_save_rules.addActionListener(this);
      jb_node_properties.addActionListener(this);
      
      bg.add(jr1);
      bg.add(jr2);
      bg.add(jr3);
      bg.add(jr4);
            
      

      jPanel3.add(new JLabel("Next key:"));
      jPanel3.add(key_name);
      
      //jPanel3.add(new JLabel(" "));
      jPanel3.add(jb_build_rules);
      jPanel3.add(jb_ins_token);
      
      jPanel3.add(lb);
      //jPanel3.add(new JLabel(" "));
      jPanel3.add(jb_iterative);
	   jb_iterative.setEnabled(false);
      jPanel3.add(jr1);
      jPanel3.add(jb1);
      
      jPanel3.add(jr2);
      jPanel3.add(jb2);
      
      jPanel3.add(jr3);
      jPanel3.add(jb3);
      
      jPanel3.add(jr4);      
      jPanel3.add(jb4);                  
      
      jPanel3.add(new JLabel("Status:"));                  
      jPanel3.add(status);                  
      
            
      jPanel4.add(new JLabel("Current node: "));
      jPanel4.add(lb_crt_node);
      jPanel4.add(new JLabel("Total nodes: "));
      jPanel4.add(lb_total_nodes);
      jPanel4.add(new JLabel("Total Marked: "));
      jPanel4.add(lb_marked);
      jPanel4.add(new JLabel("Crt. node Marked: "));
      jPanel4.add(lb_crt_marked);
      
            
      jPanel4.add(rh);
      jPanel4.add(jbnext);
      jPanel4.add(sv_tree);
      jPanel4.add(ld_tree);
      
	   jPanel4.add(exp_tree);
	   jPanel4.add(imp_tree);
      
      jPanel4.add(jb_rule_editor);
      //jPanel4.add(jb_save_rules);
	  jPanel4.add(jb_node_properties);
	   
      
      jb_save_rules.setEnabled(false);
      
      
      jScrollPane3.getViewport().add(tree, null);
      
      //jPanel4.add(jScrollPane3);
      jPanel5.add(jPanel4);
      jPanel5.add(jScrollPane3);

      //----file path settings---------
      last_path=null;
      
      filter=new ExampleFileFilter(new String[]{"htm","html"},"HTML files");
      
      //----file path settings---------
      tree_last_path=null;
      
      tree_filter=new ExampleFileFilter(new String("tre"),"Tree files");
      
      //---------------disable all buttons-------
       jb_ins_token.setEnabled(false);
       jb2.setEnabled(false);
       jb3.setEnabled(false);
       jb4.setEnabled(false);
       
       jbnext.setEnabled(false);
       jb_iterative.setEnabled(false);
       lb.setEnabled(false);
       rh.setEnabled(false);
       
       sv_tree.setEnabled(false);
       ld_tree.setEnabled(false);
       
    //----------------------------

    //contentPane.add(jSplitPane1, BorderLayout.CENTER);
    jSplitPane1.add(jSplitPane2, JSplitPane.TOP);        
    jSplitPane2.add(jScrollPane1, JSplitPane.RIGHT);
    jScrollPane1.getViewport().add(html, null);
    jSplitPane2.add(jScrollPane2, JSplitPane.LEFT);
    jScrollPane2.getViewport().add(lista_tokeni, null);
    
    jScrollPane3.setVisible(true);
    
    jSplitPane1.add(jSplitPane3, JSplitPane.BOTTOM);    
    
    jSplitPane3.add(jPanel3, JSplitPane.TOP);
    //jSplitPane3.add(jPanel4, JSplitPane.BOTTOM);        
    jSplitPane3.add(jPanel5, JSplitPane.BOTTOM);   
    
    contentPane.setVisible(true);     
    this.setContentPane(contentPane);
  }
  
  
    
  /** Overridden so we can exit when window is closed*/
  /*
  protected void processWindowEvent(WindowEvent e) {
    super.processWindowEvent(e);
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      System.exit(0);
    }
    
  }
  */
  /* my functions */
  /** refreshes the html panel */
  	private void refresh_html()
	{

		   int i;
          
			//high-speed
			//String tmp=new String(buffered_tokens.getBytes(),0,sel_idx2);
			//StringTokenizer strtk=new StringTokenizer(tmp.substring(0,sel_idx1),"\n");
			//tk_inf=strtk.countTokens()+(int)start_tk;
			//strtk=new StringTokenizer(tmp,"\n");
			//tk_sup=strtk.countTokens()+(int)start_tk;						
			
			int	selected[]=lista_tokeni.getSelectedIndices();
			if(selected!=null) //try to add/mark new key			
			{
				//get selection interval
				tk_inf=start_tk+selected[0];
				tk_sup=start_tk+selected[selected.length-1]+1;
			}	
			
		//	String str;
			String str2;

			  //logger.info("Token bounds : ["+tk_inf+","+tk_sup+"]");

			  my_token mtk1,mtk2,mtk;
			  mtk1=(my_token)rez.get(tk_inf);
			  mtk2=mtk1;
			  str2=new String(original_html_text.substring(0,(int)mtk1.idx_start));			  
			  
			  for(i=tk_inf;i<tk_sup;i++)
			  {
			     mtk2=(my_token)rez.get(i);
			  //   str=mtk2.tk;
			     mtk=mtk2;
			     if(i+1<rez.size())
			       mtk=(my_token)rez.get(i+1);
			     if(mtk2.cod!=my_token.TK_TAG)
			     {
			        str2+=new String("<font color=\"red\" bgcolor=\"green\">");
			        str2+=new String(original_html_text.substring((int)mtk2.idx_start,(int)mtk.idx_start));
			        str2+="</font>";
			     }
			     else
			           str2+=new String(original_html_text.substring((int)mtk2.idx_start,(int)mtk.idx_start));

			  }

			  if(tk_sup<rez.size())
			  mtk2=(my_token)rez.get(tk_sup);
			  str2+=new String(original_html_text.substring((int)mtk2.idx_start));


			  new_html_text=new String(str2);
			  html.setText(str2);

	}
	/** loads the file into a slot and selects it as the current profile */
	private void LoadFile(FileReader FR,int p)
	{
		//open file
			 String str=new String(""),tmp;
			// int i;
			 
			 logger.info("Start loading...");
		//reset all counters
		sel_idx1=-1;
		sel_idx2=-1;
		orig_idx1=-1;
		orig_idx2=-1;


			//Loading...
			try{
			  BufferedReader   br=new BufferedReader(FR);
               logger.info("File opened.");
			  while((tmp=br.readLine())!=null)
			  {
			  	 str+=tmp;
			  	 //str+=Character.toString((char)i);
			  	 str+="\n";
			  }
			  br.close();
		     }
		     catch(IOException f)
		     {
//			   logger.error("Cannot open file: "+tf.getText());
		     }
//		    str+="\n"; 
             logger.info("Done reading.");		    

		    //Parsing...
            logger.info("Start parsing...");
            /*
            try {
            	if(use_corector==1)
				str=HTMLCorrector.correctAndVerify(str,"C:\\Temp\\");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
            //////////////////////
		    original_html_text=new String(str);
		    ByteArrayInputStream bi=new ByteArrayInputStream(str.getBytes());
		    my_parser mp=new my_parser((InputStream)bi);
		    mp.parseEngine();		    

		    //setting results
		    logger.info("Done parsing.");
		    rez=mp.getResult();
		    logger.info("Token number:"+rez.size());

		    //buffered_tokens=new String("");
		    /*
		    for(i=0;i<rez.size();i++)
		    {
		     // buffered_tokens+=i+". ";
		      buffered_tokens+=((my_token)rez.get(i)).tk;		   
		      buffered_tokens+="\n";
		    }
		    
		    logger.info("Done formating tokens");                          
		    */
		    jScrollPane3.getViewport().remove(tree);
		    
		    
		    
		    root.label=new String("HTML");
                    root.setCoords(p,0,rez.size()-1);		    
		    		  
                    
                    
                    //tree.setSelectionRow(0);//select root
                    crt_node=root;                                                                                
            
            jScrollPane3.getViewport().add(tree,null);                        		    
		    jScrollPane3.repaint();		  
		    
		    
		    logger.info("Done init tree.");                        
		    
		    enabled=0;                        
            lista_tokeni.setListData(rez);
		    enabled=1;
		    
		    
		    logger.info("Done display parsed_tokens.");                        
		    
		    
		    html.setText(str);
		    logger.info("Done display in HTML Pane.");                        
	}
	
	/**
	 *inserts "DEFAULTTOKEN" at tk_inf index in parsed
	 *tokens and corespondingly into HTML file
	 **/
	private void ins_token()
	{
	    my_token crt_tok;
		String msg;
		StringBuffer str_buf;
		//current selection index -- tk_inf
		//validate index
		msg=null;
		crt_tok=null;
		if(tk_inf>=0 && tk_inf<rez.size()-1)
		{
		   crt_tok=(my_token)rez.get(tk_inf);
		   msg=new String("Are you sure you want to insert DEFAULT_TOKEN after ");
		   msg+="'"+crt_tok.tk+"' ?";
		   msg+="\n Warning: this will ";
		   if(crt_prof==1) msg+="remove node";
		   else msg+="unmark node";		   
		   msg+=" '"+crt_node.label+"' and all it's children!";		   
		}  						
		
		//show confirm dialogue
		if(msg!=null && crt_tok!=null)
		{
   	      if(JOptionPane.YES_OPTION==JOptionPane.showConfirmDialog(null,msg,"Insert token",JOptionPane.YES_NO_OPTION))  
   	      {   	      		
		     //init new token		     
		     my_token new_tk=new my_token();
		     
		     new_tk.tk=new String("DEFAULTTOKEN");
		     new_tk.cod=my_token.TK_ALFABETIC;
		     new_tk.des=new String("Alfabetic");
		     new_tk.idx_start=crt_tok.idx_start+crt_tok.tk.length();  
		     
		     //insert in token vector
		       rez.insertElementAt(new_tk,tk_inf+1);		        		        
		     //insert in html file
		       str_buf=new StringBuffer(original_html_text);
		       str_buf=str_buf.insert((int)new_tk.idx_start,new_tk.tk);
		       original_html_text=str_buf.substring(0);		       
		    //re-index  tokens 
		       for(int i=tk_inf+2;i<rez.size();i++)
		           ((my_token)rez.get(i)).idx_start+=new_tk.tk.length();
		    //for profile 1		       
		       //DELETE affected nodes from tree		    
		    //for profile > 1		       
		       //DELETE MARKERS for affected nodes 		       
		       delete_crt_node();
		    //re-claculate tree node
		      crt_node.setCoords(crt_prof,crt_node.getStartCoord(crt_prof),crt_node.getStopCoord(crt_prof)+1);
		    //re-display
		    build_parsed_content();
		    //save tokens to current profile
		    prof_rez[crt_prof]=new Vector(rez);
		   }    
		  status.setText("Ready");
		}
		else //status
		  status.setText("Error! Select smth. first!");		      		
    }

    /** this function searches for the next unmarked node (if any );
       uses DFS (Depth First Search) algorithm
    */
        private my_node get_next_key(my_node start)
        {
            //lookup in tree first unmarked node for crt profile
            int i;
            my_node c,rezult;
            for(i=0;i<start.getChildCount();i++)
            {
	           c=(my_node)start.getChildAt(i);
                   if(c.marked[crt_prof]==false) return c;
                   
                   rezult=get_next_key(c);//test children, if any
                   if(rezult!=null) return rezult;                   
            }       
            return null;//all nodes are marked
        }
	/** this function calls get_next_key() and updates display 
	   
	@see #get_next_key
	*/
        private void prepare_next_key()
        {            	
              enabled=0;
                //locate next unmarked node
                if(crt_prof!=1) //search next unmarked key
                {
                 
                    next_node=get_next_key(root);
                    if(next_node!=null)
                    {
                        key_name.setText(next_node.label);
                        logger.info("Node 2 be marked: "+next_node);
                        //update panels
                        crt_node=(my_node)next_node.getParent();
                        if(crt_node!=null)
                        {
                            tree.setSelectionPath(new TreePath(crt_node.getPath()));
                            tree.scrollPathToVisible(new TreePath(crt_node.getPath()));
                            
                        }
                        build_parsed_content();                        
                    }
                    else
                    {
                        key_name.setText(" ");                        
                    }
                }       
              update_labels();
              enabled=1;
        }
	
	/**
	   Function saves the current profile data (tokens,HTML)
	*/
    private void backup_profile(int b)
    {
    	if(crt_prof>=1 && crt_prof<=4 && crt_prof!=b)
    	{
    	    //prof_buffered_tokens[crt_prof]=new String(buffered_tokens);
            prof_original_html_text[crt_prof]=new String(original_html_text);
            prof_rez[crt_prof]=new Vector(rez);    	
        }    
    }    
      /** This function selects another profile; calls backup first
         @see #backup_profile
      */
	private void select_profile(int i)
	{
		//int j;	
		enabled=0;
                
                //update panels
                
            //backup old_profile stuff
            
            backup_profile(i);
            
            //change profile
            //prof_buffered_tokens[i]!=null &&
                if( prof_original_html_text[i]!=null &&
		   prof_rez[i]!=null)
		{
		      	//buffered_tokens=new String(prof_buffered_tokens[i]);
		        original_html_text=new String(prof_original_html_text[i]);
		        rez=new Vector(prof_rez[i]);
		        
                        html.setText(original_html_text);		        
		        lista_tokeni.setListData(rez);
		          
                  //       tree.setSelectionRow(0);
                        crt_node=root; 
                        tree.setSelectionPath(new TreePath(root.getPath()));
                        //set new profile
                        crt_prof=i;
	      }
	      else
	       {
	           html.setText("No file loaded");	        
	        }  
                
                //next key
                prepare_next_key();
                
		//reset all counters
		sel_idx1=-1;
		sel_idx2=-1;
		orig_idx1=-1;
		orig_idx2=-1;
                tk_inf=-1;
                tk_sup=-1;

					                  
		//logger.info("Am selectat profilul"+i);
		//treeModel.reload();
                update_labels();
                build_parsed_content();
		enabled=1;
	}
	/** this function validates the current token selection
	     before adding/marking a node 
	  */
	private boolean validate_limits(int inf,int sup)
	{
		
		int lvl;//test level
		int i;
		my_node m;							    	           
	    
	    lvl=1+crt_node.getLevel();
	    i=0;
	    
	    while(i<crt_node.getChildCount())
	    {
	    	m=(my_node)crt_node.getChildAt(i);
	    	if(m.getLevel()==lvl && m.marked[crt_prof]==true)
	    	  if((inf>=m.cds[crt_prof][my_node.tk_start] && inf<=m.cds[crt_prof][my_node.tk_stop])||
	    	     (sup>=m.cds[crt_prof][my_node.tk_start] && sup<=m.cds[crt_prof][my_node.tk_stop])||
	    	     (m.cds[crt_prof][my_node.tk_start]>=inf && m.cds[crt_prof][my_node.tk_start]<=sup) ||
	    	     (m.cds[crt_prof][my_node.tk_stop]>=inf && m.cds[crt_prof][my_node.tk_stop]<=sup))
	    	     {
	    	     	return false;
	    	     }	    	       
	    	i++;
	    }		
		return true;
	}
	/** make buffer for iteration module **/
	private Vector make_it_buffer()
	{
			Vector buffer=new Vector();
			int i;
			
			if(crt_node.getType()==0) //extraction
			{
			  for(i=crt_node.cds[1][0];i<=crt_node.cds[1][1];i++)
			  buffer.add((my_token)rez.get(i));
			}
			else
			   buffer=get_iterative_content((my_node)crt_node.getParent(),crt_node.rule_fwd,crt_node.rule_bkd);  
			   
			return buffer;   
	}
	
	private void disable_kbd()
	{
		//disable kbd
				DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
		
	}
	
	private void enable_kbd()
		{
//			enable kbd
					 DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
		}
	
	private void export_tree_to_text()
	{
    //		disable  kbd
    disable_kbd();
    
		try
		{
			JFileChooser jc=new JFileChooser();
			
			if(jc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
			{
				File f=jc.getSelectedFile();
				RuleTreeManipulator rtm=new RuleTreeManipulator();
				
				rtm.setF(f); 
				rtm.setModel(treeModel);				
				rtm.WriteToFile();
				
			}
		}
		catch(Exception e)
		{
			logger.error("OOoops exporting tree"+e);
			e.printStackTrace();
		}
		
		//enable kbd
		enable_kbd();
	}
	
	private void import_tree_from_text()
		{
		//		disable  kbd
		disable_kbd();
    
			try
			{
				JFileChooser jc=new JFileChooser();
			
				if(jc.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)
				{
					File f=jc.getSelectedFile();
					RuleTreeManipulator rtm=new RuleTreeManipulator();				
					rtm.setF(f);
					
//					unload tree
					  jScrollPane3.getViewport().remove(tree);
                    
					
					//read data: tree model
					 									
					treeModel=rtm.ReadFromFile();
				//set root & crt_node
					 root=(my_node)treeModel.getRoot();
					 crt_node=root; 		    		                      
			  //build tree
					 tree=new JTree(treeModel);
					 tree.addTreeSelectionListener(this);                           
 //load tree into panel                           
			   jScrollPane3.getViewport().add(tree,null);                        		    
			   jScrollPane3.repaint();
					
				
				}
			}
			catch(Exception e)
			{
				logger.error("OOoops importing tree"+e);
				e.printStackTrace();
			}
		
			//enable kbd
			enable_kbd();
		}
	
	/** The main action  takes place here !!!  */
	public void actionPerformed(ActionEvent e)
	{
		Object src=e.getSource();
		if(src==imp_tree)
		{
			import_tree_from_text();
		}
		else
		if(src==exp_tree)
		{
			export_tree_to_text();
		}
		else
		if(src==jb_node_properties)
		{
			//disable kbd
			DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
			
		   nodePropertiesDialog ndp=new nodePropertiesDialog(null,true,crt_node);
		   ndp.showNodePropertiesDialog();
		   //enable kbd
		   DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
		}
		else
		if(src==jb_save_rules)
		{
			save_rules();
		}
		else
		if(src==jb_rule_editor)
		{
			re=null;
			if(crt_node!=null && crt_node!=root)
			{
				DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
				re=new ruleeditor(crt_node.getForward(),crt_node.getBackward(),crt_node.getLabel());
				re.run();
			}
		}
		else
		if(src==jb_iterative)		                
		{
			it_module=null;			
			//formare buffer pt. iteration module
			//Vector buffer=new Vector();
			/*
			int i;
			
			if(crt_node.getType()==0) //extraction
			{
			  for(i=crt_node.cds[1][0];i<=crt_node.cds[1][1];i++)
			  buffer.add((my_token)rez.get(i));
			}
			else
			   buffer=get_iterative_content((my_node)crt_node.getParent(),crt_node.rule_fwd,crt_node.rule_bkd);  
			
			*/
			it_module=new iterative(make_it_buffer());
			it_flag=1;//it rules not saved
			
			it_module.run();			
		}
		else
		if(src==lb)
		{
		
			boolean v;
			int selected[];
			
			selected=lista_tokeni.getSelectedIndices();
			if(selected!=null) //try to add/mark new key			
			{
				//get selection interval
				tk_inf=start_tk+selected[0];
				tk_sup=start_tk+selected[selected.length-1]+1;
				
				v=validate_limits(tk_inf,tk_sup-1);
			if(v) //only in first profile u can add new keys
			{
                            if(crt_prof==1)//add key
                            {
                            	//call add_node function
                            	add_tree_node();
        
        /*                    	
                                //invalidate keyboardlistener
                            
			DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
			
			String raspuns=JOptionPane.showInputDialog("Rule I.D. ?");
			
			//revalidate listener
			DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
			
			   if(raspuns!=null )//&& selectedValue!=null)
			   {
                               
		             
                             logger.info(raspuns+"=["+tk_inf+","+(tk_sup-1)+"]");
                             my_node tmp_node = new my_node(raspuns,tk_inf,tk_sup-1);
                                                          
                             tmp_node.rule_type=0; 
                             
                             treeModel.insertNodeInto(tmp_node, crt_node,
                                                       crt_node.getChildCount());
                             tree.repaint();
		             jScrollPane3.repaint();            
		             status.setText("Ready");                               
		           }    */
                                
                            }
                            else  //mark key
                            {
                                if(next_node!=null)
                                {   
                                    
                                    
                                 if(JOptionPane.YES_OPTION==JOptionPane.showConfirmDialog(null,"Set selected zone for node: '"+next_node.label+"'?","Mark",JOptionPane.YES_NO_OPTION))
                                 {
  		          	   next_node.setCoords(crt_prof,tk_inf,tk_sup-1);
                                
                                   prepare_next_key();
                                 }
                                }                                  
                            }                                			
		    }
		    else
		      status.setText("Select error!");   
				
			}
			
		}		
		else
		if(src==rh)
		{
			refresh_html();		
		}
		else
		if(src==jr1)
		{		 
                  // crt_prof=1;
		   select_profile(1);
                   lb.setText("Add key");
		   
		}   
		else
		if(src==jr2)
		{		   
                //   crt_prof=2;
		   select_profile(2);
                   lb.setText("Mark key");
		   
		}   
		else
		if(src==jr3)
		{
                  // crt_prof=3;
		   select_profile(3);				   
                   lb.setText("Mark key");		   
		 }  
		else
		if(src==jr4)
		{
                   //crt_prof=4;
		   select_profile(4);	
                   lb.setText("Mark key");
		   
		 }  
		else
		if(src==jb1)
		  {
                              //invalidate keyboardlistener
                            
			DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
                        
                        if(last_path==null)                        
		  	    jc=new JFileChooser();                        
                        else
                            jc=new JFileChooser(last_path);                        
                       //file filter settings
                               jc.addChoosableFileFilter(filter);
                               jc.setAcceptAllFileFilterUsed(true);
                               jc.setFileFilter(filter);
                               
		  	if(jc.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)
		  	{
		  		   try
		  		   {
		  		   	 File fr=jc.getSelectedFile();		  	          
		  	          if(fr!=null)
		  	          {
                                      last_path=fr.getPath();                                      
                                      backup_profile(1);
		  	              LoadFile(new FileReader(fr),1);
		  	              
                          jr1.setEnabled(true);                          
		  	              jr1.setSelected(true);
		  	              crt_prof=1;
		  	  		  	         
		  	            jr1.setText(fr.getName());
                                    
		  	            
                                    
                            //prof_buffered_tokens[crt_prof]=new String(buffered_tokens);
	                    prof_original_html_text[crt_prof]=new String(original_html_text);
	                    prof_rez[crt_prof]=new Vector(rez);	
                            
                            update_labels();
	                    	                   
                          /*
                           *enable buttons
                           */
                          jb_ins_token.setEnabled(true); 
                          jb2.setEnabled(true);	                    	                                              
                          lb.setEnabled(true);
                          rh.setEnabled(true);
                          
                          sv_tree.setEnabled(true);
                          ld_tree.setEnabled(true);
		  	          }		  	              		  	          
		  	       }
		  	       catch(Exception ex)
		  	       {
		  	       }
		    }
                               			//revalidate listener
			DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
		  }   
		  else
		if(src==jb2)
		  {
		  	        //invalidate keyboardlistener
                            
			DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
                      
                      if(last_path==null)                        
		  	    jc=new JFileChooser();                        
                        else
                            jc=new JFileChooser(last_path);                        
                       //file filter settings
                               jc.addChoosableFileFilter(filter);
                               jc.setAcceptAllFileFilterUsed(true);
                               jc.setFileFilter(filter);
                               
		  	if(jc.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)
		  	{
		  		   try
		  		   {
		  		   	 File fr=jc.getSelectedFile();		  	          
		  	          if(fr!=null)
		  	          {
                                         last_path=fr.getPath();
                                         backup_profile(2);
		  	          	LoadFile(new FileReader(fr),2);
                                        jr2.setEnabled(true);
		  	          	jr2.setSelected(true);
		  	          	crt_prof=2;		  	    		  	            
		  	                jr2.setText(fr.getName());
                                        lb.setText("Mark key");
                              
                                        //prof_buffered_tokens[crt_prof]=new String(buffered_tokens);
	                                prof_original_html_text[crt_prof]=new String(original_html_text);
                                        prof_rez[crt_prof]=new Vector(rez);	                    
                                        
                                        update_labels();
                          /*
                           *enable buttons
                           */
                          jb3.setEnabled(true);	
                          jbnext.setEnabled(true); 
		  	          }		  	              		  	          
		  	       }
		  	       catch(Exception ex)
		  	       {
		  	       }
		    }
                               			//revalidate listener
			DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
		  }   
		else
		if(src==jb3)
		  {
                              //invalidate keyboardlistener
                            
			DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
                      
		  	if(last_path==null)                        
		  	    jc=new JFileChooser();                        
                        else
                            jc=new JFileChooser(last_path);                        
                       //file filter settings
                               jc.addChoosableFileFilter(filter);
                               jc.setAcceptAllFileFilterUsed(true);
                               jc.setFileFilter(filter);
                               
		  	if(jc.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)
		  	{
		  		   try
		  		   {
		  		   	 File fr=jc.getSelectedFile();		  	          
		  	          if(fr!=null)
		  	          {
                                      
                                         last_path=fr.getPath();
                                   backup_profile(3);      
		  	          	LoadFile(new FileReader(fr),3);		  	      
                                        jr3.setEnabled(true);
		  	          	jr3.setSelected(true);
		  	          	crt_prof=3;
		  	         
		  	                jr3.setText(fr.getName());
                                        lb.setText("Mark key");
                                    
		  	                //prof_buffered_tokens[crt_prof]=new String(buffered_tokens);
	                                prof_original_html_text[crt_prof]=new String(original_html_text);
	                                prof_rez[crt_prof]=new Vector(rez);		                   	                  
                                        
                                        update_labels();
                          /*
                           *enable buttons
                           */
                          jb4.setEnabled(true);	 
		  	          }		  	              		  	          
		  	       }
		  	       catch(Exception ex)
		  	       {
		  	       }
		    }
                               			//revalidate listener
			DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
		  }   
		  else
		  if(src==jb4)
		  {                      
                              //invalidate keyboardlistener
                            
			DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
                        
		  	if(last_path==null)                        
		  	    jc=new JFileChooser();                        
                        else
                            jc=new JFileChooser(last_path);                             
                       //file filter settings
                               jc.addChoosableFileFilter(filter);
                               jc.setAcceptAllFileFilterUsed(true);
                               jc.setFileFilter(filter);
                               
		  	if(jc.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)
		  	{
		  		try
		  		   {
		  		   	 File fr=jc.getSelectedFile();		  	          
		  	            if(fr!=null)
		  	             {
                                         last_path=fr.getPath();
                               backup_profile(4);          
		  	          	LoadFile(new FileReader(fr),4);
                        jr4.setEnabled(true);
		  	          	jr4.setSelected(true);
		  	          	crt_prof=4;
		  	  
		  	            lb.setText("Mark key");
		  	            jr4.setText(fr.getName());
		  	            //prof_buffered_tokens[crt_prof]=new String(buffered_tokens);
	                            prof_original_html_text[crt_prof]=new String(original_html_text);
	                            prof_rez[crt_prof]=new Vector(rez);		                                   
                            
                                    update_labels();
		  	          }		  	              		  	          
		  	       }
		  	       catch(Exception ex)
		  	       {
		  	       }
		    }
                               			//revalidate listener
			DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
		  } 
                  else
                      if(src==jbnext)
                      {
                          logger.info("Copii: "+root.getChildCount());                                                    
                          prepare_next_key();
                      }
                  else
                      if(src==sv_tree)
                      {
                                  //invalidate keyboardlistener
                            
			DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
                          
                          //save tree
                          if(tree_last_path==null)                        
		  	    tree_jc=new JFileChooser();                        
                          else
                            tree_jc=new JFileChooser(tree_last_path);                        
                       //file filter settings
                               tree_jc.addChoosableFileFilter(tree_filter);
                               tree_jc.setAcceptAllFileFilterUsed(true);
                               tree_jc.setFileFilter(tree_filter);
                         if(tree_jc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
		  	{
                          try{
                          FileOutputStream ostream = new FileOutputStream(tree_jc.getSelectedFile());
                          ObjectOutputStream p = new ObjectOutputStream(ostream);
                          p.writeObject(treeModel);                          
                          p.flush();
                          ostream.close();
                          }
                          catch(Exception ex)
                          {
                              logger.error("Save failed: "+e);
                          }
                         }
                               			//revalidate listener
			DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
                      }
                  else
                     if(src==ld_tree)
                      {
                          enabled=0;
                                  //invalidate keyboardlistener
                            
			DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
                          
                          /*load tree file */
                         if(tree_last_path==null)                        
		  	    tree_jc=new JFileChooser();                        
                        else
                            tree_jc=new JFileChooser(tree_last_path);                        
                       //file filter settings
                               tree_jc.addChoosableFileFilter(tree_filter);
                               tree_jc.setAcceptAllFileFilterUsed(true);
                               tree_jc.setFileFilter(tree_filter);
                               
		  	if(tree_jc.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)
		  	{
		  	 try{
                          FileInputStream istream = new FileInputStream(tree_jc.getSelectedFile());
                          ObjectInputStream p = new ObjectInputStream(istream);
                          
                          //unload tree
                           jScrollPane3.getViewport().remove(tree);
                           
                          //read data: tree model
		     
                           treeModel=(DefaultTreeModel) p.readObject();		           	    
                           
		              //set root & crt_node
                           root=(my_node)treeModel.getRoot();
		           crt_node=root; 		    		                      
                        //build tree
                           tree=new JTree(treeModel);
                           tree.addTreeSelectionListener(this);                           
                       //load tree into panel                           
                             jScrollPane3.getViewport().add(tree,null);                        		    
		             jScrollPane3.repaint();
                         //close streams
                             istream.close();
                          }
                          catch(Exception ex)
                          {
                              logger.error("Load failed: "+e);
                          }                         	   	 		  	
                        }           
                          
                          update_labels();
                          enabled=1;
                          			//revalidate listener
			DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
                      }
           else
            if(src==jb_ins_token)
            {
            	ins_token();
            }           
            else
             if(src==jb_build_rules)
             {    
                 build_rules2();
                 check_rules();
             }
	}
	
	/** this function returns a sub-vector **/
	private Vector subVector(Vector source,int start,int end)
	{
		
		Vector dest=new Vector();
		int i;
		for(i=start;i<end;i++)
		   dest.add(source.get(i));
		   
		return dest;   
	}
    /**
     *  this function returns a Vector of tokens asigned to a tree node
     *  useful if that tree node is an iterative one.
     *
     *  returns a vector of my_tokens
     */
     
     /* old version
     private Vector get_iterative_content(my_node nod,my_rule fwd,my_rule bkd)
     {
     	HTML_region ex;
     	Vector  buff,result=null,tmp;
     	
     	int start,end,i,j;
     	Vector intervals;// intervals limits returned
     	
     	limits lim;
     	
     	my_rule newfwd,newbkd;
     	my_node newnode;
     	
     	if(nod.getType()==0) //extraction
     	{
     		start=nod.cds[crt_prof][0];
     		end=nod.cds[crt_prof][1];
     		
     		buff=subVector(rez,start,end);
     		ex=new HTML_region(buff,new limits(0,buff.size()-1),new limits(0,buff.size()-1));
     		
     		intervals=iterative.Iterative_extraction(ex,fwd,bkd);
     		
     		result=new Vector();
     	    for(i=0;i<intervals.size();i++)
     	    {
     	         	lim=(limits)intervals.get(i);
     	         	for(j=lim.getInf();j<=lim.getSup();j++)
     	         	{
     	         		result.add(buff.get(j));
     	         	}
     	    }	     	    
     	}
     	else //the node is iterative; go up until find an extraction one
     	{
     		tmp=null;
     		newfwd=nod.rule_fwd;
     		newbkd=nod.rule_bkd;
     		newnode=(my_node)nod.getParent();
     		tmp=get_iterative_content(newnode,newfwd,newbkd);
     		ex=new HTML_region(tmp,new limits(0,tmp.size()-1),new limits(0,tmp.size()-1));
     		
     		intervals=iterative.Iterative_extraction(ex,fwd,bkd);
     		
     		result=new Vector();
     	    for(i=0;i<intervals.size();i++)
     	    {
     	         	lim=(limits)intervals.get(i);
     	         	for(j=lim.getInf();j<=lim.getSup();j++)
     	         	{
     	         		result.add(tmp.get(j));
     	         	}
     	    }	     	    
     		
     	}
     	
     	return result;     	
     }
     */
     
     private Vector get_iterative_content(my_node nod,my_rule fwd,my_rule bkd)
     {
     	HTML_region ex;
     	Vector  buff,result=null;//,tmp;
     	
     	int start,end,j;
     	Vector intervals;// intervals limits returned
     	
     	limits lim;
     	
     	//my_rule newfwd,newbkd;
     	my_node newnode;
     	
     	 //the node is iterative; go up until find an extraction one     		
     	//	newfwd=nod.rule_fwd;
     	//	newbkd=nod.rule_bkd;
     		newnode=(my_node)nod.getParent();
     	 //page limits	
     	    start=newnode.cds[crt_prof][0];
     		end=newnode.cds[crt_prof][1];
     		
     		buff=subVector(rez,start,end);
     		ex=new HTML_region(buff,new limits(0,buff.size()-1),new limits(0,buff.size()-1));     		     		     		
     		
     		intervals=iterative.Iterative_extraction(ex,fwd,bkd);
     		
     		//only the firs record
     		result=new Vector();
     		if(intervals!=null)
     		   if(intervals.size()>0)
     		   {
     		   	  lim=(limits)intervals.get(0);
     		   	  
     		   	  	for(j=lim.getInf();j<=lim.getSup();j++)
     		   	   result.add(buff.get(j)); 
     		   	   //setting limits
     	           nod.cds[crt_prof][0]=start+lim.getInf();
     	           nod.cds[crt_prof][1]=start+lim.getSup();
     		   }     	      	     		
     	return result;     	
     }
     
     
     
	/**
	    refreshes token panel 
	*/
   private void build_parsed_content()
	{			
			int i,k;
			if(crt_node.getType()==0) //extraction node
			{
				String tokens[]=new String[crt_node.cds[crt_prof][my_node.tk_stop]-crt_node.cds[crt_prof][my_node.tk_start]+1];
			start_tk=crt_node.cds[crt_prof][my_node.tk_start];
			 
			 //logger.info("build parsed start="+crt_node.tk_start+" stop="+crt_node.tk_stop);
			k=0;
					
				
			 for(i=crt_node.cds[crt_prof][my_node.tk_start];i<=crt_node.cds[crt_prof][my_node.tk_stop];i++)
  			     {				
	             tokens[k++]=new String(((my_token)rez.get(i)).tk);
		          }
		    
		    enabled=0;                                		    		    
		    lista_tokeni.setListData(tokens);

		    enabled=1;								
		    //logger.info("End rebuild parsed");
			}
			else
			{
				enabled=0;                                		    		    
		        //lista_tokeni.setListData(get_iterative_content((my_node)crt_node.getParent(),crt_node.rule_fwd,crt_node.rule_bkd));
		        lista_tokeni.setListData(get_iterative_content(crt_node,crt_node.rule_fwd,crt_node.rule_bkd));
		        start_tk=crt_node.cds[crt_prof][my_node.tk_start];

		       enabled=1;								
			}
	}
	/**
	    counts marked nodes starting at a node, using DFS
	*/
        private int count_marked_nodes(my_node start)
        {
           int cnt,i;
           cnt=0;           
            my_node c;
            if(start.marked[crt_prof]==true)
            {    
              if(start.getChildCount()>0)
                 for(i=0;i<start.getChildCount();i++)
                 {
	             c=(my_node)start.getChildAt(i);
                     if(c.marked[crt_prof]==true)
                             cnt+=count_marked_nodes(c);
                  }       
               return cnt+1;
            }  
               else            
                    return 0;                                                   
        }
        /**
	    counts marked nodes starting at a node, using DFS
	*/
        private int count_marked_nodes(my_node start,int prof)
        {
           int cnt,i;
           cnt=0;           
            my_node c;
            if(start.marked[prof]==true)
            {    
              if(start.getChildCount()>0)
                 for(i=0;i<start.getChildCount();i++)
                 {
	             c=(my_node)start.getChildAt(i);
                     if(c.marked[prof]==true)
                             cnt+=count_marked_nodes(c,prof);
                  }       
               return cnt+1;
            }  
               else            
                    return 0;                                                   
        }
	/** counts tree nodes (uses DFS) */
        private int count_nodes(my_node start)
        {
           int cnt,i;
           cnt=0;
           //lookup in tree first unmarked node for crt profile            
            my_node c;
            if(start.getChildCount()>0)
            {    
               for(i=0;i<start.getChildCount();i++)
               {
	           c=(my_node)start.getChildAt(i);
                   cnt+=count_nodes(c);
               }       
               return 1+cnt;
            }   
            else
                 return 1;
            //return cnt+start.getChildCount();//all nodes are marked
        }
	/** Updates the Labels: current node, total nodes, marked nodes */
        private void update_labels()
        {
            enabled=0;
            String txt;
            logger.info("Execut upd_labels");
           
            lb_crt_node.setText(crt_node.label);
            lb_total_nodes.setText(new Integer(count_nodes(root)).toString());
            //compute total marked nodes
            txt=new String(new Integer(count_marked_nodes(root)).toString());
            txt=txt+"/"+lb_total_nodes.getText();
            lb_marked.setText(txt);
            //compute marked nodes for current node
            txt=new String(new Integer(count_marked_nodes(crt_node)).toString());
            txt=txt+"/";
            txt+=new Integer(count_nodes(crt_node)).toString();
            lb_crt_marked.setText(txt);  
           
           
           // jPanel4.repaint();
            enabled=1;
        }
        /** used to update display when a differnt node is slected from the tree */
	public void valueChanged(TreeSelectionEvent event) {
		    if(enabled==1)		    
		    {		     
		       status.setText("Busy");
              crt_node=(my_node)tree.getLastSelectedPathComponent();
           //  start_tk=crt_node.tk_start;
              //logger.info(crt_node.label);              
              build_parsed_content();
              update_labels();              
              status.setText("Ready");
            }  
     }       
 /** unmarks node in the current profile */
 private void delete_markers(my_node start)
 {
 	
 	int i;       
    my_node c;       
    start.unmark(crt_prof);   
    for(i=0;i<start.getChildCount();i++)
    {
	           c=(my_node)start.getChildAt(i);
	           delete_markers(c);
    }                   
 }
 /** deletes a node if in 1st profile, otherwise calls delete_markers */
 private void delete_crt_node()
 { 	
 		   my_node p;       	
 	enabled=0;   	   
    if(crt_prof==1)
    {      
  	  crt_node.removeAllChildren();
  	  p=(my_node)crt_node.getParent();
  	  if(p!=null)
  	   {
  		   p.remove(crt_node);
  		   crt_node=p;  		          		
  	   }  		          	 
  	   treeModel.reload();
       tree.setSelectionPath(new TreePath(crt_node.getPath()));
       update_labels();       
     } 
     else
     if(crt_prof>1 && crt_node!=root)
     {
     	//delete markings
         	delete_markers(crt_node);                     	                     	                     	
       	//select next key
          	prepare_next_key();                     	
     }
     enabled=1;
     update_labels();  	 	     
 }
 //use this routine for JDK 1.4.0+ 
 /** JDK 1.4.0+ keyboard event handler :P */
  public boolean dispatchKeyEvent(KeyEvent e)  
   {  
   int kc=e.getKeyCode();
   
  //	my_node p;
  	enabled=0;
  	status.setText("Busy");
  	    	
  	if(e.getID()==KeyEvent.KEY_PRESSED)  	
  	switch(kc)
  	{
  		case KeyEvent.VK_1:  		        
		        select_profile(1);
		        //crt_prof=1;
  		        jr1.setSelected(true);
  		     break;
  		case KeyEvent.VK_2:
 		        
		        select_profile(2);
		        //crt_prof=2;
  		        jr2.setSelected(true);
  		     break;
  		case KeyEvent.VK_3:
		        select_profile(3);
		        //crt_prof=3;
  		        jr3.setSelected(true);
  		     break;
  		case KeyEvent.VK_4:
  		        
		        select_profile(4);
		        //crt_prof=4;
  		        jr4.setSelected(true);
  		     break;               	
		case KeyEvent.VK_N:
  		          prepare_next_key();		
  		    break;                    
                case KeyEvent.VK_I:
                       ins_token();     		          
  		    break;                                        
  		case KeyEvent.VK_DELETE:
  		          if(crt_node!=null)
  		          {
  		          	//crt_prof==1 -> delete node & children
  		          	if((crt_prof==1) && JOptionPane.YES_OPTION==JOptionPane.showConfirmDialog(null,"Delete node '"+crt_node.label+"' and it's children ?","Remove node(s)",JOptionPane.YES_NO_OPTION))
  		          	     delete_crt_node();
  		          	else
  		          	   if((crt_prof>1) &&  JOptionPane.YES_OPTION==JOptionPane.showConfirmDialog(null,"Delete markers for '"+crt_node.label+"' and it's children ?","Delete marker(s)",JOptionPane.YES_NO_OPTION))  
  		          	     delete_crt_node();
  		          	     
  		          	     build_parsed_content();
  		          }
  		    break;             
  	}  	  	 
  	status.setText("Ready");
  	enabled=1;   
       return true;
   } 
   
   /** This function checks the possibility to build rules from marked examples **/
   private boolean check_build()
   {
       boolean rez=true;
       int nodes=count_nodes(root);       
       if(nodes>1)
       {
           //for each profile
           if(jr2.isEnabled() && nodes!=count_marked_nodes(root,2))
                 rez=false;
           if(jr3.isEnabled() && nodes!=count_marked_nodes(root,3))
                 rez=false;
           if(jr4.isEnabled() && nodes!=count_marked_nodes(root,4))
                 rez=false;           
       }
       else 
           rez=false;              
       return rez;
   }   
  //-------------applet functions--------------------
  
  /** called by Internet browser **/
  public String getAppletInfo()
  {
  	return "Application or Applet - HTML data extraction Interface - \r\n";
  }
  /** applet init **/
  public void init()
  {
  	 logger.info("Init is empty for now...");
  }
  /** applet start **/
  public void start()
  {
  	  if(ourThread==null)
  	  {
  	  	  ourThread=new Thread(this);
  	  	  ourThread.start();
  	  }
  }
  /** applet stop **/
  public void stop()
  {
  	  if(ourThread!=null)
  	  {
  	  	 // ourThread.stop(); 
  	  	  ourThread=null;  	  	  
  	  }
  }
  /** destroy applet **/
  public void destroy()
  {  	
  }
  /** running **/
  public void run()
  {
  	 while (true)
  	 {
  	 	try
  	 	{
  	 	//	repaint();
  	 	/*
  	 	         if(jr1.isEnabled()&&jr1.isSelected())
  	 	             jb_iterative.setEnabled(true);
  	 	          else
  	 	                jb_iterative.setEnabled(false);
  	 	  */
  	 	             
                        if(check_build()==true)
                            jb_build_rules.setEnabled(true);                        
                        else
                            jb_build_rules.setEnabled(false);
            
            //module rule editor                
             if(re!=null)                           
                if(!re.notfinished())                   
                {
                	logger.info("Activez kbd dupa Rule editor");
                	DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
                   	 if(re.rules_saved==true)
                   	 {
                   	 	logger.info("Rules were saved");
                   	 	crt_node.rule_fwd=re.getForward();
                   	 	crt_node.rule_bkd=re.getBackward();
                   	 }
                   	 else
                   	    logger.info("Rules were NOT saved");
                   	    
                   	 re=null;                   	 
                 }  	 
                   	  
              
              /*     
                            
             if(it_module!=null && it_flag==1)              
             if(it_module.notfinished()==false)
             {
             		logger.info("Reguli generate in Iteration module");
			        logger.info("FWD: "+it_module.getForward()
			              +"\n"+"BKD: "+it_module.getBackward()+"\n");   
			              it_flag=0;			              
			       my_rule fwdr=it_module.getForward();
			       my_rule bkdr=it_module.getBackward();
			       if(fwdr!=null && bkdr!=null) //adaug nod iterativ
			       {
			       	DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
			       	   String raspuns=JOptionPane.showInputDialog("Iterative node name ?");	
			       	//revalidate listener
			        DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
			       	   if(raspuns!=null)
			       	   {
			       	   	   	my_node tmp_node = new my_node(raspuns,0,0);
			       	   	   	// set rule type                             
                                tmp_node.rule_type=1;
                                logger.info("Iteration rule");
                            //set rules    
                               tmp_node.rule_fwd=fwdr;
                               tmp_node.rule_bkd=bkdr;
                               
                                treeModel.insertNodeInto(tmp_node, crt_node,
                                                       crt_node.getChildCount());
                                tree.repaint();
			       	   	
			       	   }
			       }
             }
             */
             if(it_module!=null)              
             if(it_module.notfinished()==false)
             {
             		logger.info("Reguli generate in Iteration module");
			        logger.info("FWD: "+it_module.getForward()
			              +"\n"+"BKD: "+it_module.getBackward()+"\n");   
			       if(it_module.saved()==true)
                      	           {
                      	           	  crt_node.setBackward(it_module.getBackward());
                      	           	  crt_node.setForward(it_module.getForward());
                      	           }
                 it_module=null;
             }
                            
  	 		Thread.sleep(500);
  	 	}  	 	
  	 	catch(InterruptedException e)
  	 	{
  	 		stop();
  	 	}
  	 }
  }
  public void paint(Graphics g)
  {
  	  contentPane.repaint();
  }
  //--------------------------------------------------
 
   
  /** finally, the main thing */
  public static void main(String args[])
  {
     JFrame frame=new JFrame("Application or applet");
      
      //inner class to exit when close button is hit
      class FrameClose extends WindowAdapter{
      	public void windowClosing(WindowEvent e)
      	{
      		System.exit(0);
      	}
      }
     frame.addWindowListener(new FrameClose());
     app applet=new app();
     applet.applic=true;
     
     frame.getContentPane().add(applet,BorderLayout.CENTER);
     frame.setSize(780,580);
     applet.init();
     applet.start();
     frame.setVisible(true);
     
  }   
  
 
 /** this function generates the rules for the marked examples  **/
  private void build_rules2() {
  	  int i;
      //DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
      //String raspuns=JOptionPane.showInputDialog("File name where to save rules?(<name>.rls) Please specify extension!");	
      //DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
     // if(raspuns!=null)
      //{
      builder b=new builder(treeModel);
      int maxp=1;
      //Vector R1=null;
      //choose number of examples      
      if(jr2.isEnabled())
            maxp=2;
      if(jr3.isEnabled())
            maxp=3;          
      if(jr4.isEnabled())
            maxp=4;
      //generate rules
      Vector profiles_tokens[]=new Vector[maxp+1];
      
      for(i=1;i<=maxp;i++) 
         profiles_tokens[i]=prof_rez[i];
         
      b.gen_rules2(profiles_tokens,maxp);
      //save rules
      //b.save_rules(raspuns,R1);
      //}
  }
  
  private void save_rules()
  {
  	DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
  	String raspuns=JOptionPane.showInputDialog("File name where to save rules?(<name>.rls) Please specify extension!");	
    DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
    
      builder b=new builder(treeModel);
      
    if(raspuns!=null)
       b.save_rules2(raspuns);
  }
   private void check_null_rules_nodes(my_node nod,String rez)
   {
   	  if(nod!=null)
   	  {
   	  	  int i;//cnt,
           //cnt=0;           
          my_node c;                    
               logger.info(nod.getLabel()+"\nFWD: "+nod.getForward()+"\nBKD: "+
                                  nod.getBackward()); 
               if((nod!=root) &&(nod.getForward()==null || nod.getBackward()==null))
                 rez+=nod.getLabel()+"\n";
                 
               for(i=0;i<nod.getChildCount();i++)
               {
	           c=(my_node)nod.getChildAt(i);
	           check_null_rules_nodes(c,rez);
	           }          
   	  }
   }

	private void check_rules() {
		//for each node check if both rules are not null
	    String rez=new String();
		check_null_rules_nodes(root,rez);
		if(rez.length()!=0)
		{
		   logger.info("Build rules failed! Nodes:"+rez);
		   JOptionPane.showMessageDialog(null, "Rule generation error; nodes with now rules:\n"+rez, "Rules error", JOptionPane.ERROR_MESSAGE); 
		   
		}  		   
	}

	private void add_tree_node() {
		
		  int node_type;
		  int node_sec_type;
		  String node_name;
		  
		                        //invalidate keyboardlistener
                            
			DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
			
			nodeDialog nd=new nodeDialog(null,true);
			
			boolean rezultat=nd.showNodeDialog();
			
			if(rezultat==true)
			{
				logger.info("I will add a node!");
				node_type=nd.getNodeType();
				node_sec_type=nd.getNodeSecondType();
				node_name=nd.getNodeName();
				
				 my_node tmp_node = new my_node(node_name,tk_inf,tk_sup-1);
                  
                  tmp_node.setNodeType(node_type);
                  //tmp_node.setNodeSecondType(node_sec_type);
                  tmp_node.setNode_category_type(nd.getCategory());
				  tmp_node.setNode_basic_field(nd.getField());
                 //set rules type accordingly                                        
                 if(node_type==node_constants.ntype_extract || node_type==node_constants.ntype_basic)
                       tmp_node.rule_type=0; 
                       
                 if(node_type==node_constants.ntype_iterat)
                      {                      	                      	 
                      	         tmp_node.rule_type=1; //iteration rule
                      	         //start an iteration module
                      	            it_module=new iterative(make_it_buffer());                      	                               	         
                      	         //retrieve it_rules
                      	         it_module.run();
						         
						         treeModel.insertNodeInto(tmp_node, crt_node,crt_node.getChildCount());		
						         crt_node=tmp_node;
                      	         
                      	         //halt until it_module closes
                      	         /*
                      	         while(it_module.notfinished())
                      	         {
                      	         	try{
                      	         		ourThread.sleep(500);
                      	         		}
                      	         	catch(Exception e)
                      	         	{
                      	         		logger.error("oops add key");
                      	         	}
                      	         }
                      	         
                      	         if(it_module.saved()==true)
                      	           {
                      	           	  tmp_node.setBackward(it_module.getBackward());
                      	           	  tmp_node.setForward(it_module.getForward());
                      	           }
                      	           */
                      }
                   else
                   {
//					insert in tree              
				   treeModel.insertNodeInto(tmp_node, crt_node,crt_node.getChildCount());												
					//crt_node=tmp_node;                          
                   }
              
			}
			
			//revalidate listener
			DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
		
	}  
}
