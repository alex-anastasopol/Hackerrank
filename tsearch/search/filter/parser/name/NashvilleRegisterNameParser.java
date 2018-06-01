/*
 * Created on Jun 30, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.filter.parser.name;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Category;

import ro.cst.tsearch.search.tokenlist.NameTokenList;
import ro.cst.tsearch.servers.types.TSServer;
import ro.cst.tsearch.utils.StringUtils;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class NashvilleRegisterNameParser extends NameParser {
	protected static final Category logger = Category.getInstance(NashvilleRegisterNameParser.class.getName());
	
	
	
	public NashvilleRegisterNameParser (){
		delimitsSeveralNames = new String[]{
									"/" 
									};
	}

}
