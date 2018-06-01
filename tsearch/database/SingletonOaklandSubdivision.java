package ro.cst.tsearch.database;

import ro.cst.tsearch.data.ILKaneSubdivisions;
import ro.cst.tsearch.data.OaklandSubdivisions;
import ro.cst.tsearch.data.Subdivisions;

import java.util.*;

public class SingletonOaklandSubdivision {
	   // Private constructor suppresses generation of a (public) default constructor
	
	 private  OaklandSubdivisions[][] oakSubV;                 		     		 //vector with data extracted from data bases
	 private  Subdivisions[][] macombSubdiv;
	 private ILKaneSubdivisions[][] kaneSubdiv;
	 
	   private SingletonOaklandSubdivision() {
		   
		           oakSubV = new OaklandSubdivisions[2][];
		           macombSubdiv = new Subdivisions[2][];
		           kaneSubdiv = new ILKaneSubdivisions[1][];
		           
				   oakSubV[0] = DBManager.getOaklandSubdivisions("", OaklandSubdivisions.DB_OAKLAND_SUBDIVISION);     //loads data for search on subdivisions
				   oakSubV[1] = DBManager.getOaklandSubdivisions("", OaklandSubdivisions.DB_OAKLAND_CONDOMINIUM);     //loads data for search on condominium
				   
				   macombSubdiv[0] = DBManager.getMacombSubdivisions("", Subdivisions.DB_MACOMB_SUBDIVISION);     //loads data for search on subdivisions
				   macombSubdiv[1] = DBManager.getMacombSubdivisions("", Subdivisions.DB_MACOMB_CONDOMINIUM);     //loads data for search on condominium	
				   
				   kaneSubdiv[0] = DBManager.getILKaneSubdivisions("", ILKaneSubdivisions.DB_IL_KANE_SUBDIVISION);
				   
	   }
	   
	  	   
	   public OaklandSubdivisions[] getSubdivision( String nume , int type ){
		   /*
		   Searches the vector with names that are search attrbutes 
		    */

		   if( "".equals(nume) )
				   return oakSubV[type-1];
			   
		   int pozitie = 0;
		   
		   String x ="" ;
		   
		   Vector v = new Vector();
			   
			   for(int i=0; i<oakSubV[type-1].length ;i++){		//iterates the array of data from condominium and subdivision
				   
				   x =  oakSubV[type-1][i].getName();                 	 //x contains the string with the name from the data base	   
				   
				   pozitie = x.indexOf(nume);                    			    //pozitie contains the value of the index of the search string 
				   
				   if( pozitie >= 0 ){                                			    //if the search string appears in the database string 
				     
					   v.add(oakSubV[type-1][i]);                            	//ads the data to the Vector 
				   }
				      
			   }
		     
		
		   
		   OaklandSubdivisions[] oakSubReturnToSearch = new OaklandSubdivisions[v.size()];
		   //array returned by the search  
		   
			for(int i=0 ; i < v.size() ; i++)
				oakSubReturnToSearch[i] = (OaklandSubdivisions)v.elementAt(i);
			
			return oakSubReturnToSearch;
	   }
	   
	   public Subdivisions[] getMacombSubdivision( String nume , int type ){
		   /*
		   Searches the vector with names that are search attrbutes 
		    */

		   if( "".equals(nume) )
				   return macombSubdiv[type-3];
			   
		   int pozitie = 0;
		   
		   String x ="" ;
		   
		   Vector v = new Vector();
			   
			   for(int i=0; i<macombSubdiv[type-3].length ;i++){		//iterates the array of data from condominium and subdivision
				   
				   x =  macombSubdiv[type-3][i].getName();                 	 //x contains the string with the name from the data base	   
				   
				   pozitie = x.indexOf(nume);                    			    //pozitie contains the value of the index of the search string 
				   
				   if( pozitie >= 0 ){                                			    //if the search string appears in the database string 
				     
					   v.add(macombSubdiv[type-3][i]);                            	//ads the data to the Vector 
				   }
				      
			   }
		     
		
		   
		   Subdivisions[] macombSubReturnToSearch = new Subdivisions[v.size()];
		   //array returned by the search  
		   
			for(int i=0 ; i < v.size() ; i++)
				macombSubReturnToSearch[i] = (Subdivisions)v.elementAt(i);
			
			return macombSubReturnToSearch;
	   }
	   
	   public ILKaneSubdivisions[] getKaneSubdivision( String nume , int type ){
		   /*
		   Searches the vector with names that are search attributes 
		    */

		   if( "".equals(nume) )
				   return kaneSubdiv[type-5];
			   
		   int pozitie = 0;
		   
		   String x ="" ;
		   
		   Vector v = new Vector();
			   
			   for(int i=0; i<kaneSubdiv[type-5].length ;i++){		//iterates the array of data from condominium and subdivision
				   
				   x =  kaneSubdiv[type-5][i].getName();                 	 //x contains the string with the name from the data base	   
				   
				   pozitie = x.indexOf(nume);                    			    //pozitie contains the value of the index of the search string 
				   
				   if( pozitie >= 0 ){                                			    //if the search string appears in the database string 
				     
					   v.add(kaneSubdiv[type-5][i]);                            	//ads the data to the Vector 
				   }
				      
			   }

		   ILKaneSubdivisions[] kaneSubReturnToSearch = new ILKaneSubdivisions[v.size()];
		   //array returned by the search  
		   
			for(int i=0 ; i < v.size() ; i++)
				kaneSubReturnToSearch[i] = (ILKaneSubdivisions)v.elementAt(i);
			
			return kaneSubReturnToSearch;
	   }
	   
	   private static class SingletonHolder {
	     private static SingletonOaklandSubdivision instance = new SingletonOaklandSubdivision();
	   } 
	 
	   public static SingletonOaklandSubdivision getInstance() {
	     return SingletonHolder.instance;
	   }
	 }