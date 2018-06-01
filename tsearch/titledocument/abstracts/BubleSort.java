/*
 * Created on Apr 21, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.titledocument.abstracts;

/**
 * @author adrian
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

public class BubleSort  {    // Sort an array's values into ascending order
	 	
  public String[] bubbleSortTech( String b[] )
  {
	boolean flag=true; 
	while (flag) 
	{
	  flag = false;
	  for (int i=1; i<b.length-1; i++)
		if (b[i].compareToIgnoreCase(b[i+1]) > 0 )
		{
			swap(b, i, i+1);   // call another method which is defined below!
			flag = true;
		}  // from if
	}  // from while
    return b;
  } // from bubleSortTech method         
                                
  private void swap( String c[], int first, int second )
  {
    String work;
	work = c[first];
	c[first] = c[second];
	c[second] = work;   
  } // from swap method   

} // from class
