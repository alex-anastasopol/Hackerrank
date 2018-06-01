package ro.cst.tsearch.data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.database.DatabaseData;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author nae
 */
public abstract class DataAttribute implements Serializable, Cloneable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected Object[] attrValues = new Object[getAttrCount()];

    public static final int ORA_ESCAPED = 0;
    
    public Object getAttribute(int index, int attrType) {
        if (index >= 0 && index < getAttrCount())
        	if (attrType == ORA_ESCAPED) {
        		return StringUtils.oraEscape(attrValues[index]);
        	} else {
        		return attrValues[index];
        	}
        return null;
    }
    
    public Object getAttribute(int index) {
        if (index >= 0 && index < getAttrCount())
            return attrValues[index];
        return null;
    }

    public void setAttribute(int attributeIndex, DatabaseData databaseData, String columnName, int row) {
        attrValues[attributeIndex] = databaseData.getValue(columnName, row);
    }

    public void setAttribute(int index, Object value) {
        attrValues[index] = value;
    }

    protected abstract int getAttrCount();

    /**
     * Method transformNull.
     * 
     * @param string
     * @return String
     */
    public String transformNull(String string) {
        return (string != null ? string : "");
    }

    public static String transformBlank(String value) {
        return (value.equals("") ? "''" : "'" + value + "'");
    }

    public static String transformBlankToNumber(String value) {
        return (value.equals("") ? "-1" : value);
    }

    public static void updateAttributes(String tableName, String columnName, String columnValue, String condition)
            throws BaseException, DataException {

        String stm = "UPDATE " + tableName + " SET " + DBManager.sqlColumnName(columnName) + "= ? WHERE " + condition;

		try {
			DBManager.getSimpleTemplate().update(stm,columnValue);
		} catch (Exception e) {
			throw new BaseException("Error to update " + columnName + " in " + tableName + " Details: " + e.getMessage());
		}
    }
    
    public synchronized Object clone() {
        
        try {
		    
		    DataAttribute dataAttribute = (DataAttribute) super.clone();
		    
		    dataAttribute.attrValues = new Object[attrValues.length];
		    
		    for (int i = 0; i < attrValues.length; i++) {
		        
		        if (attrValues[i] == null)
		            continue;
		        
		        if (attrValues[i] instanceof String) {
		            
		            dataAttribute.attrValues[i] = new String( (String) attrValues[i]);
		            
		        } else if (attrValues[i] instanceof BigDecimal) {
		            
		            dataAttribute.attrValues[i] = new BigDecimal( ((BigDecimal) attrValues[i]).doubleValue() );
		            
				} else if (attrValues[i] instanceof BigInteger) {
						            
					dataAttribute.attrValues[i] = new BigInteger( attrValues[i].toString() );
						            
				} else if (attrValues[i] instanceof Double) {
				    
				    dataAttribute.attrValues[i] = new Double( ((Double) attrValues[i]).doubleValue() );
				    
				} else if (attrValues[i] instanceof Float) {
				    
				    dataAttribute.attrValues[i] = new Float( ((Float) attrValues[i]).floatValue() );
				} else if (attrValues[i] instanceof Integer) {
				    
				    dataAttribute.attrValues[i] = new Integer( ((Integer) attrValues[i]).intValue() );
				    
				} else if (attrValues[i] instanceof Long) {
				    
				    dataAttribute.attrValues[i] = new Long( ((Long) attrValues[i]).longValue() );
				} else if (attrValues[i] instanceof Date) {
		            
		            dataAttribute.attrValues[i] = ((Date) attrValues[i]).clone();
		            
				} else if (attrValues[i] instanceof Boolean) {
		            
		            dataAttribute.attrValues[i] = new Boolean(((Boolean) attrValues[i]).booleanValue());
		            
		        } else {
		            System.err.println("DataAttribute(clone error): TIP " + attrValues[i].getClass());
		        }
		    }
		    
		    return dataAttribute;
		    
		} catch (CloneNotSupportedException cnse) {
	        throw new InternalError(); 
	    }	 
    }

}