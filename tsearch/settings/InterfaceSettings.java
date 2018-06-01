package ro.cst.tsearch.settings;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.Serializable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.utils.StringUtils;

		/**
		 * mihaib
		 */

public class InterfaceSettings{
	
	public static String SEARCH_PAGE_HEIGHT  = "sph";
	public static String SEARCH_PAGE_WIDTH = "spw";
	public static String REPORTS_HEIGHT = "rh";	
	public static String REPORTS_WIDTH  = "rw";
	
	public static final int OP_ADD	  = 0;
	
	
	private List<Dimensions> interfaceSettings = null;//new ArrayList<Dimensions>();
	
	public static class Dimensions implements Serializable{
    	
		private static final long serialVersionUID = 7867867861L;
		public final double sp_height;
    	public final double sp_width;
    	public final double rep_height;
    	public final double rep_width;

    	
    	public Dimensions(double sp_height, double sp_width, double rep_height, double rep_width){
    		this.sp_height = sp_height;
    		this.sp_width = sp_width;
    		this.rep_height = rep_height;
    		this.rep_width = rep_width;
    	}
    }

	private InterfaceSettings(){	
		load();
	}

	/**
	 * 
	 */
	private void load() {
		interfaceSettings = DBManager.getInterfaceSettings();
	}
	
    /**
     * safe and fast lazy initialization for getInstance()
     */
	private static class Holder{
		private static InterfaceSettings instance = new InterfaceSettings();
	}
	
	/**
	 * return instance of this class
	 * @return
	 */
	public static InterfaceSettings getInstance(){
		return Holder.instance;
	}
		
	public List<Dimensions> getDimensions(){
		load();
		return interfaceSettings;
	}
	
	public List<Dimensions> getDimensionsForJSP(){
		if (interfaceSettings == null){
			load();
		}
		return interfaceSettings;
	}
	
	public void doUpdates( HttpServletRequest req ){
			
		// get operation
		int operation = OP_ADD;
		try{
			operation = Integer.parseInt(req.getParameter("operation" ));
		}
		catch(RuntimeException e){
			return;
		}
		
		String sp_height = req.getParameter(SEARCH_PAGE_HEIGHT);
		String sp_width = req.getParameter(SEARCH_PAGE_WIDTH);
		String rep_height = req.getParameter(REPORTS_HEIGHT);
		String rep_width = req.getParameter(REPORTS_WIDTH);
		if(StringUtils.isEmpty(sp_height)){
			sp_height = "0";
		}
		if(StringUtils.isEmpty(sp_width)){
			sp_width = "0";
		}
		if(StringUtils.isEmpty(rep_height)){
			rep_height = "0";
		}
		if(StringUtils.isEmpty(rep_width)){
			rep_width = "0";
		}					

		Dimensions dimensions = new Dimensions(Double.parseDouble(sp_height), Double.parseDouble(sp_width), Double.parseDouble(rep_height), Double.parseDouble(rep_width));
		switch(operation){
		case OP_ADD:
			DBManager.updateDimensions(dimensions);
			break;
		default:
			dimensions = getDimensions().get(0);
			break;
		}
	}
	
	//##these are only for server side
	/*public static double getSearchPageHeight(double sp_height){
		int screen_height = getScreenHeight();
		
		if (StringUtils.isEmpty(Double.toString(sp_height))){
			sp_height = 0;
		}
		sp_height = (double) (sp_height * 0.01 * screen_height);
		
		return sp_height;
	}
	
	public static double getSearchPageWidth(double sp_width){
		int screen_width = getScreenWidth();
		
		if (StringUtils.isEmpty(Double.toString(sp_width))){
			sp_width = 0;
		}
		sp_width = (double) (sp_width * 0.01 * screen_width);
		
		return sp_width;
	}
	
	public static double getReportsHeight(double rep_height){
		int screen_height = getScreenHeight();

		if (StringUtils.isEmpty(Double.toString(rep_height))){
			rep_height = 0;
		}
		rep_height = (double) (rep_height * 0.01 * screen_height);
		
		return rep_height;
	}
	
	public static double getReportsWidth(double rep_width){
		int screen_width = getScreenWidth();
		
		if (StringUtils.isEmpty(Double.toString(rep_width))){
			rep_width = 0;
		}
		rep_width = (double) (rep_width * 0.01 * screen_width);
		
		return rep_width;
	}
	
	public static int getScreenHeight(){
		Toolkit toolkit =  Toolkit.getDefaultToolkit ();
	    Dimension dim = toolkit.getScreenSize();
	    
	    return dim.height;
	}
	
	public static int getScreenWidth(){
		Toolkit toolkit =  Toolkit.getDefaultToolkit ();
	    Dimension dim = toolkit.getScreenSize();
	    
	    return dim.width;
	}
	
	
	public static Dimension getScreenDimensions(){
		Toolkit toolkit =  Toolkit.getDefaultToolkit();
	    Dimension dim = toolkit.getScreenSize();
	    
	    return dim;
	}
	
	public static int getScreenHeight(Dimension dim){
		
	    return dim.height;
	}
	
	public static int getScreenWidth(Dimension dim){

	    return dim.width;
	}
	*/
	//#####
	
	public static void main(String[] args){
	    Toolkit toolkit =  Toolkit.getDefaultToolkit ();
	    Dimension dim = toolkit.getScreenSize();
	    
	    //double dsds = getSearchPageHeight(55);
	    
	    //System.out.println("dim height is " + dsds + " pixels");
	    
	    System.out.println("Width of Screen Size is " + dim.width + " pixels");
	    System.out.println("Height of Screen Size is " + dim.height + " pixels");
	  }

}
