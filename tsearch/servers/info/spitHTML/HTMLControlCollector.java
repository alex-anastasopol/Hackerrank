package ro.cst.tsearch.servers.info.spitHTML;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;

public class HTMLControlCollector extends HTMLObject {

	protected HashMap components = new HashMap();
	protected HTMLObject[][] matrix;
	protected int maxColIndex = -1;
	protected int maxRowIndex = -1;
	
	public String render() {
		String outData = "";
		try
		{
			//Iterator i = components.entrySet().iterator();
			outData = "<table width='100%' height = '100%' cellpadding='0' cellspacing='0'>";
			for (int i = 0; i < maxRowIndex; i++)
			{
				outData += "<tr>";
				for (int j = 0; j < maxColIndex; j++)
				{
					if (matrix[i][j] != null)
					{
						//	numai daca nu a fost deja asignat intr-o celula il asignam
						if (!matrix[i][j].isDisplayed())
						{
							if (matrix[i][j] instanceof PageZone){
								outData += "<td " +
										/*"align='center' "+*/
										"valign='middle' "+
										"colspan='" + 
										matrix[i][j].getWidth() + 
										"' rowspan='" + 
										matrix[i][j].getHeight() +
										"'>";
							} else {
								outData += "<td " +
										"class='bodyData' " +
										/*"align='center' "+*/
										"valign='middle' "+
										"colspan='" + 
										matrix[i][j].getWidth() + 
										"' rowspan='" + 
										matrix[i][j].getHeight() +
										"'>";
							}
							matrix[i][j].setWidthFactor(1.0/matrix[i][j].getWidth());
							matrix[i][j].setHeightFactor(1.0/matrix[i][j].getHeight());
							/////////////////////////////////////////
							//	randeaza obiectul propriu-zis
							
							outData += matrix[i][j].render();
							
							/////////////////////////////////////////
							outData += "</td>";
							matrix[i][j].setDisplayed(true);	
						}
					}
					else
					{
						outData += "<td class='bodyData'>" + "&nbsp;" + "</td>";
					}
				}
				outData += "</tr>";
			}
			outData += "</table>";
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return outData;
	}

	public HTMLControlCollector (
			HashMap allObjects,
			boolean removeFromHash,
			int integrationMode)
			throws FormatException
	{
		Vector objectsToBeRemoved = new Vector();
		Iterator i = allObjects.entrySet().iterator();
		//	vedem maximele pentru a limita matricea.
		while (i.hasNext())
		{
			Entry mapEntry = (Entry) i.next();
			HTMLObject ho = (HTMLObject) (mapEntry).getValue();
			if (integrationMode == INCLUDE_PAGEZONES || ho instanceof HTMLControl)
			{
				int currentColIndex = ((HTMLObject) mapEntry.getValue()).getMaxCol(); 
				if ( currentColIndex > maxColIndex )
				{
					maxColIndex = currentColIndex;
				}
				int currentRowIndex = ((HTMLObject) mapEntry.getValue()).getMaxRow(); 
				if ( currentRowIndex > maxRowIndex )
				{
					maxRowIndex = currentRowIndex;
				}
				components.put(mapEntry.getKey(), mapEntry.getValue());
				if (removeFromHash)
				{
					objectsToBeRemoved.add(mapEntry.getKey());
					//allObjects.remove(mapEntry.getKey());
				}
			}
		}
		if (maxRowIndex != -1
				&& maxColIndex != -1)
		{
			matrix = new HTMLObject[maxRowIndex][maxColIndex];
			fillMatrix(components);
			removeCollectedObjects(allObjects, objectsToBeRemoved);	
		}
	}
	
	public void fillMatrix(HashMap components)
			throws FormatException
	{
		Iterator iter = components.entrySet().iterator();
		while (iter.hasNext())
		{
			HTMLObject ho = (HTMLObject) ((Entry) iter.next()).getValue();
			for (int j = ho.getMinCol() - 1; j <= ho.getMaxCol() - 1; j++)
			{
				for (int i = ho.getMinRow() - 1; i <= ho.getMaxRow() - 1; i++)
				{
					if (matrix[i][j] == null)
					{
						matrix[i][j] = ho;
					}
					else
					{
						if (ho instanceof HTMLControl
								&& ((HTMLControl) ho).isHiddenParam()
								&& !((HTMLControl) ho).isDisplayed())
						{
							matrix[i][j].addHiddenField(ho);
						}
						else if (matrix[i][j] instanceof HTMLControl
									&& ((HTMLControl) matrix[i][j]).isHiddenParam())
						{
							//HTMLControl hiddenParam = (HTMLControl) matrix[i][j];
							ho.addHiddenField((HTMLControl) matrix[i][j]);
							matrix[i][j] = ho;
						}
						else
						{
							throw new FormatException("More than one value in a single cell is not allowed.");
						}
					}
				}
			}
		}
	}
	
	/**
	 * Sterge din HashMapul cu obiecte generale obiectele care deja 
	 * le-a grupat in HTMLControlCollector 
	 * @param allObjects
	 * @param rmvObjcts
	 */
	private void removeCollectedObjects(HashMap allObjects, Vector rmvObjcts)
	{
		for (int i = 0; i < rmvObjcts.size(); i++)
		{
			allObjects.remove(rmvObjcts.elementAt(i));
		}
	}

	/**
	 * @return Returns the components.
	 */
	public HashMap getComponents() {
		return components;
	}

	/**
	 * @param components The components to set.
	 */
	public void setComponents(HashMap components) {
		this.components = components;
	}
	
}
