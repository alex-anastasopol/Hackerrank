/*
 * Created on Aug 26, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.wrapper2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import javax.swing.tree.DefaultTreeModel;

import org.apache.log4j.Category;

/**
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class RuleTreeManipulator {
	
	/** treemodel for i/o **/
	private DefaultTreeModel model;
	private String fileName;
	private File F;
	
	protected static final Category logger= Category.getInstance(RuleTreeManipulator.class.getName());
	 
	/**
	 * @return
	 */
	public File getF() {
		return F;
	}

	/**
	 * @return
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @return
	 */
	public DefaultTreeModel getModel() {
		return model;
	}

	/**
	 * @param file
	 */
	public void setF(File file) {
		F = file;
	}

	/**
	 * @param string
	 */
	public void setFileName(String string) {
		fileName = string;
	}

	/**
	 * @param model
	 */
	public void setModel(DefaultTreeModel model) {
		this.model = model;
	}

    private void write_node_data(my_node crt,PrintWriter pw) throws Exception
    {
       
          crt.dumpInfo(pw);
          int nr=crt.getChildCount();
		  pw.println(nr+"; No of chidren for node :["+crt.getLabel()+"]");
          
		    for (int i=0;i<nr;i++)
		       write_node_data((my_node)crt.getChildAt(i),pw);          
    }
	/** extrage primul token  (pana la  ; )**/
			   private String get_first_Token(String s)
			   {
				   StringTokenizer t=new StringTokenizer(s,";");
				   return t.nextToken();
			   }
    public void WriteToFile()
    {
    	try
    	{
    	     PrintWriter pw=new PrintWriter(new FileOutputStream(F));
    	     pw.println("DO NOT MODIFY UNLESS U KNOW WHAT IT DOES!!!");
    	     //for all nodes in tree model
    	     my_node root=(my_node) model.getRoot();
    	     write_node_data(root,pw);
    	     pw.flush();
    	     pw.close();
    	}
    	catch(Exception e)
    	{
    		logger.error("Tree writer/reader exception" +e);
    	}
    }
    
	public DefaultTreeModel ReadFromFile()
		{
			try
			{
				 BufferedReader br=new BufferedReader(new FileReader(F));
				 br.readLine(); //that DO Not...
				 //for all nodes in tree model
				 //my_node root=(my_node) model.getRoot();
				 my_node root=read_node_data(br);
				 
				 model=new DefaultTreeModel(root);
				 				 
				 br.close();
			}
			catch(Exception e)
			{
				logger.error("Tree writer/reader exception" +e);
			}
			
			return model;
		}
    
    private my_node read_node_data(BufferedReader br) throws Exception
    {
    	my_node rez=new my_node("tmp",0,0);
    	my_node chnode;
    	rez.readInfo(br);
    	//read child count
		int nr=Integer.parseInt(get_first_Token(br.readLine()));		      
		for (int i=0;i<nr;i++)
		{
			chnode=read_node_data(br);
			rez.insert((my_node)chnode,i);
		}
		     	
		return rez;    	
    }
}
