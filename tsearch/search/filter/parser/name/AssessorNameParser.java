/*
 * Created on Jun 30, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ro.cst.tsearch.search.filter.parser.name;

import org.apache.log4j.Category;

/**
 * @author elmarie
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class AssessorNameParser extends NameParser{
	protected static final Category logger = Category.getInstance(AssessorNameParser.class.getName());
	
	
	public AssessorNameParser (){
		delimitsOwnerSpouseNames = new String[]{"\\bAND\\b", "&"};
	}
	
}
