package ro.cst.tsearch.database;

import java.util.Vector;
import java.util.Hashtable;

/** Class for getting results from database */
public class DatabaseData {
	private Hashtable data = new Hashtable();
	private String[] columnsNames;


	/** Add a value at the last row of the specified column */
	public void addValue( String column, Object value ) {
		column = column.toUpperCase();
		Vector dataVector = (Vector)(data.get(column));
		if (value != null) {
			dataVector.addElement(value);
		} else {
			dataVector.addElement(new Vector());
		}
		data.put(column, dataVector);
	}


	/** Add a row of values */
	public void addRow( Vector values ) {
		if (values.size() == columnsNames.length)
			for (int i = 0; i < columnsNames.length; i++) {
				addValue(columnsNames[i], values.elementAt(i));
			}
	}


	/** Get the number of rows.  */
	public int getRowNumber() {
		if (!data.isEmpty())
			return((Vector)(data.get(columnsNames[0]))).size();
		else
			return 0;
	}


	/** Return true if there are no rows in the DatabaseData. */
	public boolean isEmpty() {
		return getRowNumber() == 0;
	}


	/** Get the value at the given column and row */
	public Object getValue(String column, int row) {
		column = column.toUpperCase();
		Object object = data.get(column);

		if (object != null && ((Vector)object).size() != 0) {

			Object item = null;
			try {
				item = ((Vector)object).elementAt(row);
			} catch (ArrayIndexOutOfBoundsException e ) {}

			if (item instanceof Vector) {
				return null;
			} else {
				return item;
			}
		} return null;		
	}


	/** Get the value at the given column and row */
	public Object getValue(int column, int row) {
		/*Object object = data.get(columnsNames[column-1]);

		if (object != null || ((Vector)object).size() != 0) {
			return((Vector)object).elementAt(row);
		} else
			return null;*/
			
		return getValue(getColumnName(column), row);
	}


	/** Get the name of the column specified. The first index is 1. */
	public String getColumnName( int column ) {
		return columnsNames[column-1].toUpperCase();
	}


	/** Initialise an object having the given column names  */
	public DatabaseData(String[] columnsNames) {
		this.columnsNames = columnsNames;
		if (this.columnsNames != null) {
			for ( int i = 0; i < columnsNames.length; i++) {
				data.put(columnsNames[i].toUpperCase(), new Vector());                
			}
		}
	}

	public int getColumnNumber(){
		return columnsNames.length;
	}
}















