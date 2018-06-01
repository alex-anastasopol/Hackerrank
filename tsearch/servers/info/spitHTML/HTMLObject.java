package ro.cst.tsearch.servers.info.spitHTML;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import ro.cst.tsearch.servlet.BaseServlet;


public abstract class HTMLObject {
	
	
	public static final int ORIENTATION_HORIZONTAL = 0;
	public static final int ORIENTATION_VERTICAL = 1;
	
	public static final int INCLUDE_PAGEZONES = 1;
	
	public static final int PIXELS = 0;
	public static final int PERCENTS = 1;
	public String restriction="";
	private static final double majorFactor = 1.8;
	
	protected HTMLObject parent;
	
	
	//	orientarea default este orizontal
	public int orientation = ORIENTATION_HORIZONTAL;
	//	sizeul se da in procente si default este 1%
	protected int size = 1;
	
	private int[] colIndex = {-1};
	private int[] rowIndex = {-1};

	private double widthFactor = 1;
	private double heightFactor = 1;
	
	public String label;
	public String name;
	
	protected int formIndex = -1;

	private boolean displayed = false;
	private boolean visible = true;
	private boolean hiddenParam = false;
	
	private HashMap attachedHiddenFields = new HashMap();

	public abstract String render() throws FormatException;
	
	/**
	 * Arrayul de intrare trebuie sa aiba unul 
	 * sau doua elemente mai mari sau egale cu 1.
	 * <br><br>
	 * In cazul in care sunt 2 elemente, primul trebuie sa fie mai mic decat al doilea.
	 * @param inArray
	 * @return
	 */
	private boolean checkArray(int[] inArray)
	{
		if (inArray.length == 2) {
			if (inArray[0] < inArray[1]
			        && inArray[0] >= 1
			        && inArray[1] >= 2)
			{
				return true;
			}
			return false;
		}
		if (inArray.length == 1 
				&& inArray[0] >= 1)
		{
			return true;
		}
		return false;
	}
	
	/**
	 * @return Coloana de sfarsit
	 */
	public int getMaxCol() {
		if (colIndex.length == 1)
		{
			return colIndex[0];
		}
		return colIndex[1];
	}
	
	/**
	 * @return Linia de sfarsit
	 */
	public int getMaxRow() {
		if (rowIndex.length == 1)
		{
			return rowIndex[0];
		}
		return rowIndex[1];
	}
	
	/**
	 * @return Coloana de inceput
	 */
	public int getMinCol() {
		if (colIndex.length == 1)
		{
			return colIndex[0];
		}
		return colIndex[0];
	}
	
	/**
	 * @return Linia de inceput
	 */
	public int getMinRow() {
		if (rowIndex.length == 1)
		{
			return rowIndex[0];
		}
		return rowIndex[0];
	}
	
	/** Returneaza latimea
	 * @return
	 */
	public int getWidth()
	{
		if (colIndex.length == 1)
		{
			return 1;
		}
		return colIndex[1] - colIndex[0] + 1;
	}
	
	/**
	 * Returneaza inaltimea
	 * @return
	 */
	public int getHeight()
	{
		if (rowIndex.length == 1)
		{
			return 1;
		}
		return rowIndex[1] - rowIndex[0] + 1;
	}
	
	/**
	 * Verifica daca o celulca din matrice este acoperita sau nu de 
	 * acest HTMObject.
	 * @param row
	 * @param col
	 * @return
	 */
	public boolean containsCellInMatrix(int row, int col)
	{
		if (colIndex.length == 1
				&& rowIndex.length == 1)
		{
			if (colIndex[0] == col
					&& rowIndex[0] == row)
			{
				return true;
			}
			return false;
		}
		else if (colIndex.length == 1
				&& rowIndex.length == 2)
		{
			if (colIndex[0] == col
					&& rowIndex[0] <= row 
					&& row <= rowIndex[1]) {
				return true;
			}
			return false;
		}
		else if (colIndex.length == 2
				&& rowIndex.length == 1)
		{
			if (colIndex[0] <= col
					&& col <= colIndex[1]
					&& rowIndex[0] == row)
			{
				return true;
			}
			return false;
		}
		else if (colIndex.length == 2
				&& rowIndex.length == 2) 
		{
			if (colIndex[0] <= col
					&& col <= colIndex[1]
					&& rowIndex[0] <= row
					&& row <= rowIndex[1])
			{
				return true;
			}
			return false;
		}
		return false;
	}
	
	/**
	 * @return Returns the orientation.
	 */
	public int getOrientation() {
		return orientation;
	}

	/**
	 * @param orientation The orientation to set.
	 */
	public void setOrientation(int orientation) {
		this.orientation = orientation;
	}

	/**
	 * @return Returns the size.
	 */
	public int getSize() {
		return size;
	}

	/**
	 * @param size The size to set.
	 */
	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * @return Returns the label.
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @param label The label to set.
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return Returns the colIndex.
	 */
	public int[] getColIndex() {
		return colIndex;
	}

	/**
	 * @param colIndex The colIndex to set.
	 */
	public void setColIndex(int[] colIndex) 
			throws FormatException
	{
		if (!checkArray(colIndex)) 
		{
			throw new FormatException("Cell Format Exception >>>  {" + colIndex[0] + "," + colIndex[1] + "}");
		}
		this.colIndex = colIndex;
	}

	/**
	 * @return Returns the rowIndex.
	 */
	public int[] getRowIndex() {
		return rowIndex;
	}

	/**
	 * @param rowIndex The rowIndex to set.
	 */
	public void setRowIndex(int[] rowIndex) 
			throws FormatException
	{
		if (!checkArray(rowIndex)) 
		{
			throw new FormatException("Cell Format Exception >>>" + rowIndex.toString());
		}
		this.rowIndex = rowIndex;
	}

	/**
	 * @return Returns the displayed, adica daca obiectul este deja asignat intr-o celula.
	 */
	public boolean isDisplayed() {
		return displayed;
	}

	/**
	 * @param displayed The displayed to set.
	 */
	public void setDisplayed(boolean displayed) {
		this.displayed = displayed;
	}
	
	/**
	 * @return Returns the widthFactor.
	 */
	public double getWidthFactor() {
		return widthFactor;
	}
	
	/**
	 * @return Returns the widthFactor as percent.
	 */
	public int getWidthFactorAsPercent() {
		double retValue = widthFactor * 100 * majorFactor;
		if (retValue > 100)
			retValue = 100;
		return new Double(retValue).intValue();
	}

	/**
	 * @param widthFactor The widthFactor to set.
	 * @throws FormatException
	 */
	public void setWidthFactor(double widthFactor)
			throws FormatException
	{
		if (widthFactor > 1 
				|| widthFactor < 0)
		{
			throw new FormatException("With factor must be beetween 0 and 1 >>>> widthFactor = " + widthFactor);
		}
		this.widthFactor = widthFactor;
	}

	/**
	 * @return Returns the heightFactor.
	 */
	public double getHeightFactor() {
		return heightFactor;
	}
	
	/**
	 * @return Returns the heightFactor as percent.
	 */
	public int getHeightFactorAsPercent() {
		double retValue = heightFactor * 100 * majorFactor;
		if (retValue > 100)
			retValue = 100;
		return new Double(retValue).intValue();
	}

	/**
	 * @param heightFactor The heightFactor to set.
	 * @throws FormatException
	 */
	public void setHeightFactor(double heightFactor)
			throws FormatException
	{
		if (heightFactor > 1 
				|| heightFactor < 0)
		{
			throw new FormatException("Height factor must be beetween 0 and 1 >>>> heightFactor = " + heightFactor);
		}
		this.heightFactor = heightFactor;
	}

	/**
	 * @return Returns the formIndex.
	 */
	public int getFormIndex() {
		return formIndex;
	}

	/**
	 * @param formIndex The formIndex to set.
	 */
	public void setFormIndex(int formIndex) {
		this.formIndex = formIndex;
	}
	
	public static String getJSResource()
	{
		StringBuffer jsCode = new StringBuffer();
		jsCode.append("\n\n");
		try {
			FileInputStream fis = new FileInputStream(new File(BaseServlet.REAL_PATH + File.separator + "web-resources" + File.separator + "javascripts" + File.separator + "PSinclude.js"));
			BufferedReader in = new BufferedReader(new InputStreamReader(new BufferedInputStream(fis)));
			String line;
			while ((line = in.readLine()) != null) {
				jsCode.append(line + "\n");
			}
			in.close();
		}
		catch (Exception e)
		{e.printStackTrace();}
		return jsCode.toString();
		
	}
	
	/**
	 * @return Returns the hiddenParam.
	 */
	public boolean isHiddenParam() {
		return hiddenParam;
	}

	/**
	 * @param hiddenParam The hiddenParam to set.
	 */
	public void setHiddenParam(boolean hiddenParam) {
		this.hiddenParam = hiddenParam;
	}

	/**
	 * @return Returns the visible.
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * @param visible The visible to set.
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public void addHiddenField (HTMLObject ho) {
		attachedHiddenFields.put(ho.getName(), ho);
	}
	
	public void removeHiddenField (String key) {
		attachedHiddenFields.remove(key);
	}

	/**
	 * @return Returns the attachedHiddenFields.
	 */
	public HashMap getAttachedHiddenFields() {
		return attachedHiddenFields;
	}

	/**
	 * @param attachedHiddenFields The attachedHiddenFields to set.
	 */
	public void setAttachedHiddenFields(HashMap attachedHiddenFields) {
		this.attachedHiddenFields = attachedHiddenFields;
	}

	public HTMLObject getParent() {
		return parent;
	}

	public void setParent(HTMLObject parent) {
		this.parent = parent;
	}
	
}
