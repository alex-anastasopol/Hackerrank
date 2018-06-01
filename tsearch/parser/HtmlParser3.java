package ro.cst.tsearch.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.htmlunit.corejs.javascript.ast.NodeVisitor;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.Text;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableHeader;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

public class HtmlParser3 {

	Parser parser = new Parser();
	NodeList nodeList = new NodeList();
	String html = null;

	public HtmlParser3(String html) {
		this.html = html;
		parser = Parser.createParser(html, null);
		try {
			nodeList = parser.parse(null);
		} catch (ParserException e) {
			e.printStackTrace();
		}
	}

	
	public HashMap<String,String> getListOfPostParams (String formName) {
		NodeList params = this.getNodeList().extractAllNodesThatMatch(new HasAttributeFilter("name", formName),true)
				.extractAllNodesThatMatch(new TagNameFilter("input"), true);
		HashMap<String,String> postParams = null;
		
		if (params.size() == 0) 
			params = this.getNodeList().extractAllNodesThatMatch(new HasAttributeFilter("id", formName),true)
				.extractAllNodesThatMatch(new TagNameFilter("input"), true);
		
		if (params.size() > 0) {
			postParams = new HashMap<String, String>(params.size()); 
			for (int i=0; i < params.size(); i++) {
				InputTag input = (InputTag) params.elementAt(i);
				if (input != null) {
					String key = input.getAttribute("name");
					if (StringUtils.isEmpty(key)) {
						key = input.getAttribute("id");
					}
					String value = input.getAttribute("value");
					if (StringUtils.isNotEmpty(key)) {
						postParams.put(key, value);
					}
				}
				
			}
		}
		
		return postParams;
	}
	
	
	public Node getNodeById(String id) {
		return getNodeById(id, true);
	}

	public Node getNodeById(String id, boolean recursive) {
		NodeList localNodeList = nodeList;
		return getNodeByID(id, localNodeList, recursive);
	}

	public static Node getNodeByID(String id, NodeList localNodeList, boolean recursive) {
		try {
			NodeList nl = localNodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", id), recursive);
			if (nl.size() > 0) {
				return nl.elementAt(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getNodeValueByID(String id, NodeList localNodeList, boolean recursive) {
		Node nodeByID = getNodeByID(id, localNodeList, recursive);
		String returnValue = "";
		if (nodeByID != null) {
			returnValue = nodeByID.toPlainTextString();
		}
		return returnValue;
	}

	public static NodeList getNodesByID(String id, NodeList localNodeList, boolean recursive) {
		NodeList nl = localNodeList.extractAllNodesThatMatch(new HasAttributeFilter("id", id), recursive);
		return nl;
	}

	public String getNodeContentsById(String id) {
		Node node = getNodeById(id, true);
		if (node == null) {
			return "";
		}
		return node.toHtml();
	}

	public String getNodePlainTextById(String id) {
		Node node = getNodeById(id, true);
		if (node == null) {
			return "";
		}
		return node.toPlainTextString().trim();
	}

	public static Node getNodeByAttribute(NodeList nodes, String attribute, String name, boolean recursive) {
		NodeList nl = nodes.extractAllNodesThatMatch(new HasAttributeFilter(attribute, name), recursive);
		if (nl.size() > 0) {
			return nl.elementAt(0);
		}
		return null;
	}

	public Node getNodeByAttribute(String attribute, String name, boolean recursive) {
		return getNodeByAttribute(nodeList, attribute, name, recursive);
	}

	public Node getNodeByTypeAndAttribute(String type, String attributeName, String attributeValue, boolean recursive) {
		return getNodeByTypeAndAttribute(nodeList, type, attributeName, attributeValue, recursive);
	}

	public static Node getNodeByTypeAndAttribute(NodeList nl, String type, String attributeName, String attributeValue, boolean recursive) {
		NodeList returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive).extractAllNodesThatMatch(
				new HasAttributeFilter(attributeName, attributeValue), recursive);
		if (returnList.size() > 0) {
			return returnList.elementAt(0);
		}
		return null;
	}

	public static NodeList getNodeListByTypeAndAttribute(NodeList nl, String type, String attributeName, String attributeValue,
			boolean recursive) {
		NodeList returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive).extractAllNodesThatMatch(
				new HasAttributeFilter(attributeName, attributeValue), recursive);
		return returnList;
	}

	public NodeList getNodeListByTypeAndAttribute(String type, String attributeName, String attributeValue, boolean recursive) {
		NodeList returnList = nodeList.extractAllNodesThatMatch(new TagNameFilter(type), recursive).extractAllNodesThatMatch(
				new HasAttributeFilter(attributeName, attributeValue), recursive);
		return returnList;
	}

	public static NodeList getNodeListByType(NodeList nl, String type, boolean recursive) {
		NodeList returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive);
		return returnList;
	}

	public String getNodeContentsByAttribute(String attribute, String name) {
		Node node = getNodeByAttribute(attribute, name, true);
		if (node == null) {
			return "";
		}
		return node.toHtml();
	}

	public List<List<String>> getTableAsListById(String id, boolean includeFirstRow) {

		try {
			TableTag tbl = (TableTag) getNodeById(id);
			return getTableAsList(tbl, includeFirstRow);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<List<String>>();
	}

	public static List<List<String>> getTableAsList(String table, boolean includeFirstRow) {
		Parser parser = Parser.createParser(table, null);
		try {
			NodeList nl = parser.parse(null);
			TableTag tbl = (TableTag) nl.elementAt(0);
			return getTableAsList(tbl, includeFirstRow);
		} catch (ParserException e) {
			e.printStackTrace();
		}
		return new ArrayList<List<String>>();
	}

	public static List<List<String>> getTableAsList(TableTag tbl, boolean includeFirstRow) {
		List<List<String>> rowList = new ArrayList<List<String>>();

		for (TableRow row : tbl.getRows()) {
			List<String> colList = new LinkedList<String>();
			for (TableColumn col : row.getColumns()) {
				colList.add(col.toPlainTextString());
			}
			rowList.add(colList);
		}

		if (!includeFirstRow) {
			rowList.remove(0);
		}

		return rowList;
	}

	/**
	 * The method returns a map that contains column headers as keys and a list
	 * of their values in the html table. Prior to the method call the TableTag
	 * should be cleaned bacause a matrix is expected. e.g.
	 * <table>
	 * <thead>
	 * <tr>
	 * <th>
	 * col</th>
	 * </tr>
	 * </thead>
	 * <tr>
	 * <td>
	 * val1</td>
	 * <tr>
	 * <tr>
	 * <td>
	 * val2</td>
	 * <tr>
	 * <table>
	 * 
	 * @param tbl
	 * @return
	 */
	public static Map<String, List<String>> getTableAsMap(TableTag tbl) {
		// this will hold the values for each column name.
		Map<String, List<String>> columnToValues = new HashMap<String, List<String>>();
		// this maps column names to their position in the table: i.e.
		// <col_name_1,1>
		Map<String, Integer> columnToIndex = new HashMap<String, Integer>();
		Map<Integer, String> indexToColumn = new HashMap<Integer, String>();

		// gets the headers for all the columns
		Node[] nodeArray = getTag(tbl.getChildren(), new TableHeader(), true).toNodeArray();

		for (int i = 0; i < nodeArray.length; i++) {
			Node node = nodeArray[i];
			String header = node.getChildren() == null ? "" : node.getChildren().toHtml();
			if (StringUtils.isNotEmpty(header)) {
				columnToValues.put(header, null);
				columnToIndex.put(header, new Integer(i));
				indexToColumn.put(new Integer(i), header);
			}
		}

		// gets all the rows
		Node[] allTableRows = getTag(tbl.getChildren(), new TableRow(), false).toNodeArray();
		for (int i = 0; i < allTableRows.length; i++) {
			Node node = allTableRows[i];
			// node.getChildren().toNodeArray();
			Node[] cells = node.getChildren().extractAllNodesThatMatch(new TagNameFilter(new TableColumn().getRawTagName())).toNodeArray();
			for (int j = 0; j < cells.length; j++) {
				if (cells[j] instanceof TableColumn) {
					String key = indexToColumn.get(new Integer(j));
					List<String> list = columnToValues.get(key);
					if (list == null) {
						list = new LinkedList<String>();
					}
					NodeList value = cells[j].getChildren();
					list.add(value == null ? "" : value.toHtml());
					columnToValues.put(key, list);
				}
			}
		}

		return columnToValues;
	}

	public static List<HashMap<String, String>> getTableAsListMap(String tbl) {
		Parser parser = Parser.createParser(tbl, null);
		TableTag tableTag = null;
		try {
			NodeList nl = parser.parse(null);
			tableTag = (TableTag) nl.elementAt(0);
		} catch (ParserException e) {
			e.printStackTrace();
		}

		return getTableAsListMap(tableTag);
	}

	/**
	 * Gets a properly cleaned html table with this structure: | Key1 | Key2 | |
	 * Val11| Val12| | Val21| Val22| and saves it in a List of Maps. 1st Map in
	 * the list is : {Key1=Val11, Key2=Val12} 2nd Map in the list is :
	 * {Key1=Val21, Key2=Val22)}
	 * 
	 * @param tbl
	 * @return
	 */
	public static List<HashMap<String, String>> getTableAsListMap(TableTag tbl) {

		// this will hold the values for each column name.
		Map<String, List<String>> columnToValues = new HashMap<String, List<String>>();
		List<HashMap<String, String>> listOfRows = new LinkedList<HashMap<String, String>>();

		// this maps column names to their position in the table: i.e.
		// <col_name_1,1>
		Map<String, Integer> columnToIndex = new HashMap<String, Integer>();
		Map<Integer, String> indexToColumn = new HashMap<Integer, String>();
		if (tbl != null) {

			// gets the headers for all the columns
			Node[] nodeArray = getTag(tbl.getChildren(), new TableHeader(), true).toNodeArray();
			// gets all the rows
			Node[] allTableRows = getTag(tbl.getChildren(), new TableRow(), false).toNodeArray();
			int firstDataRow = 1;
			if (allTableRows != null && allTableRows.length >= 1) {
				// if there is no th then the first row is taken as header
				if (nodeArray.length == 0) {
					nodeArray = getTag(allTableRows[0].getChildren(), new TableColumn(), false).toNodeArray();
					// firstDataRow++;
				}
				for (int i = 0; i < nodeArray.length; i++) {
					Node node = nodeArray[i];
					String header = node.getChildren() == null ? "" : node.getChildren().asString();
					if (StringUtils.isNotEmpty(header)) {
						columnToValues.put(header, null);
						columnToIndex.put(header, new Integer(i));
						indexToColumn.put(new Integer(i), header);
					}
				}

				// the first row is reserved for the header
				for (int i = firstDataRow; i < allTableRows.length; i++) {
					Node node = allTableRows[i];
					Node[] cells = node.getChildren().extractAllNodesThatMatch(new TagNameFilter(new TableColumn().getRawTagName()))
							.toNodeArray();

					HashMap<String, String> hashMap = new HashMap<String, String>();

					for (int j = 0; j < cells.length; j++) {
						if (cells[j] instanceof TableColumn) {
							String columnName = indexToColumn.get(new Integer(j));
							String key = StringUtils.defaultIfEmpty(columnName, "").trim();
							NodeList value = cells[j].getChildren();
							hashMap.put(key, value == null ? "" : ro.cst.tsearch.utils.StringUtils.cleanHtml(value.asString().trim()));
						}
					}
					listOfRows.add(hashMap);
				}
			}
		}
		return listOfRows;
	}

	public static Map<String, List<String>> getTableAsMap(String table) {
		List<List<String>> tableAsList = getTableAsList(table, true);
		// this will hold the values for each column name.
		Map<String, List<String>> columnToValues = new HashMap<String, List<String>>();
		// this maps column names to their position in the table: i.e.
		// <col_name_1,1>
		Map<String, Integer> columnToIndex = new HashMap<String, Integer>();
		Map<Integer, String> indexToColumn = new HashMap<Integer, String>();

		// get the headers
		if (tableAsList.size() > 0) {
			List<String> headers = tableAsList.get(0);
			for (int i = 0; i < headers.size(); i++) {
				String header = headers.get(i);
				columnToValues.put(header, null);
				columnToIndex.put(header, new Integer(i));
				indexToColumn.put(new Integer(i), header);
			}

			for (int i = 1; i < tableAsList.size(); i++) {
				List<String> row = tableAsList.get(i);
				for (int j = 0; j < row.size(); j++) {
					String key = indexToColumn.get(new Integer(j));
					List<String> list = columnToValues.get(key);
					if (list == null) {
						list = new LinkedList<String>();
					}
					String value = row.get(j);
					list.add(value);
					columnToValues.put(key, list);
				}
			}

		}

		return columnToValues;
	}

	public Parser getParser() {
		return parser;
	}

	public void setParser(Parser parser) {
		this.parser = parser;
	}

	public NodeList getNodeList() {
		return nodeList;
	}

	public void setNodeList(NodeList nodeList) {
		this.nodeList = nodeList;
	}

	public static Text findNode(NodeList nl, String text,StringFindingVisitorList nodeVisitor, boolean needExactMatch) {
		
		try {
			StringFindingVisitorList accountVisitor = new StringFindingVisitorList(text);
			
			if (nodeVisitor != null){
				accountVisitor = nodeVisitor;
			}
			
			accountVisitor.setNeedExactMatch(needExactMatch);
			nl.visitAllNodesWith(accountVisitor);
			Text node = accountVisitor.getFirstOccurence();
			return node;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static Text findNode(NodeList nl, String text, boolean needExactMatch) {
		return findNode(nl, text, null, needExactMatch);
	}

	public static Text findNode(NodeList nl, String text) {

		return findNode(nl, text, false);
	}

	public static NodeList findNodeList(NodeList nl, String text) {
		try {
			StringFindingVisitorList accountVisitor = new StringFindingVisitorList(text);
			nl.visitAllNodesWith(accountVisitor);
			return accountVisitor.getOccurences();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static <T> T findNodeAfter(NodeList nl, Node afterNode, Class<T> type) {
		boolean foundNode = false;
		for (Node eachNode : nl.toNodeArray()) {
			if (foundNode) {
				if (type.isInstance(eachNode)) {
					return (T) eachNode;
				}
			}
			if (eachNode.equals(afterNode)) {
				foundNode = true;
			}
		}
		return null;
	}

	public static String getValueFromSecondCell(Text text, String regex) {
		return getValueFromSecondCell(text, regex, false);
	}

	public static String getValueFromSecondCell(Text text, String regex, boolean asHtml) {
		if (text == null) {
			return "";
		}
		TableColumn tc = (((TableRow) text.getParent().getParent()).getColumns())[1];

		return getValueFromCell(tc, regex, asHtml);
	}

	public static String getValueFromAbsoluteCell(int offsetRow, int offestColumn, Text text, String regex, boolean asHtml) {
		return getValueFromAbsoluteCell(offsetRow, offestColumn, text, regex, asHtml, true);
	}

	public static String getValueFromAbsoluteCell(int offsetRow, int offestColumn, Text text, String regex, boolean asHtml,
			boolean standardTable) {
		TableColumn tc = getAbsoluteCell(offsetRow, offestColumn, text, standardTable);
		if (tc == null) {
			return "";
		}
		try {
			return getValueFromCell(tc, regex, asHtml);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public static Node findAncestorWithClass(Text from, Class type) {
		Node theStartNode = from;
		while ((theStartNode instanceof Node) && !theStartNode.getClass().equals(type)) {
			theStartNode = theStartNode.getParent();
		}
		return theStartNode;
	}

	public static TableColumn getAbsoluteCell(int offsetRow, int offestColumn, Text text, boolean standardTable) {
		if (text == null) {
			return null;
		}
		try {
			/* Assume the text is in a column, and the column is in a row */
			TableRow row = null;
			if (standardTable) {
				row = (TableRow) text.getParent().getParent();
			} else {
				row = (TableRow) findAncestorWithClass(text, TableRow.class);
			}

			int columnWithText = -1;
			if (row != null) {
				for (int col = 0; col < row.getColumnCount(); col++) {
					boolean test = row.getColumns()[col].getChildren() != null && row.getColumns()[col].getChildren().contains(text);
					if (!standardTable)
						test = row.getColumns()[col].getChildren() != null
								&& row.getColumns()[col].getChildren().toHtml().contains(text.toHtml());
					if (test) {
						columnWithText = col;
						break;
					}
				}
				if (columnWithText >= 0) {
					if (!((columnWithText + offestColumn) >= row.getColumnCount() || (columnWithText + offestColumn) < 0)) {

						TableTag table = (TableTag) getFirstParentTag(row, TableTag.class);
						TableRow[] rows = table.getRows();

						for (int i = 0; i < rows.length; i++) {
							if (rows[i] == row) {
								row = rows[i + offsetRow];
								break;
							}
						}
						if(columnWithText + offestColumn < row.getColumnCount()) {
							return row.getColumns()[columnWithText + offestColumn];
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static TableColumn getAbsoluteCell(int offsetRow, int offestColumn, Text text) {
		return getAbsoluteCell(offsetRow, offestColumn, text, true);
	}

	public static String getValueFromNearbyCell(int offestColumn, Text text, String regex, boolean asHtml) {
		return getValueFromAbsoluteCell(0, offestColumn, text, regex, asHtml);
	}

	public static String getValueFromNextCell(NodeList nl, String text, String regex, boolean asHtml) {
		return getValueFromNearbyCell(1, HtmlParser3.findNode(nl, text), regex, asHtml);
	}

	public static String getValueFromNextCell(Text text, String regex, boolean asHtml) {
		return getValueFromNearbyCell(1, text, regex, asHtml);
	}

	public static String getValueFromPreviousCell(Text text, String regex, boolean asHtml) {
		return getValueFromNearbyCell(-1, text, regex, asHtml);
	}

	public static String getValueFromCell(CompositeTag tc, String regex, boolean asHtml) {

		try {
			String str = (asHtml) ? tc.getStringText() : tc.toPlainTextString();

			if (!StringUtils.isEmpty(regex)) {
				Pattern p = Pattern.compile(regex);
				Matcher m = p.matcher(str);
				if (m.find()) {
					return m.group(1);
				}
			} else {
				return str;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	/**
	 * Retrieves all the nodes in nodeList that have the tag type a sublass of
	 * CompositeTag with or without traversing the nodes recursively.
	 * 
	 * @param nodeList
	 * @param tag
	 * @param recursive
	 * @return
	 */
	public static NodeList getTag(NodeList nodeList, CompositeTag tag, boolean recursive) {
		NodeList nl = new NodeList();
		if (tag instanceof CompositeTag) {
			nl = nodeList.extractAllNodesThatMatch(new NodeClassFilter(tag.getClass()), recursive);

		}
		return nl;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getFirstTag(NodeList nodeList, Class<T> type, boolean recursive) {
		;
		return (T) nodeList.extractAllNodesThatMatch(new NodeClassFilter(type), recursive).elementAt(0);
	}

	public static NodeList getTag(NodeList nodeList, Class type, boolean recursive) {
		;
		return nodeList.extractAllNodesThatMatch(new NodeClassFilter(type), recursive);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getFirstParentTag(Node node, Class<T> type) {
		;
		Node parent;
		if (node == null) {
			return null;
		}
		while ((parent = node.getParent()) != null) {
			if (type.isInstance(parent)) {
				return (T) parent;
			}
			node = node.getParent();
		}
		return null;
	}

	public String getHtml() {
		return html;
	}

	public void setHtml(String html) {
		this.html = html;
	}

	public String getPlainTextFromNodeById(String id) {
		Node node = getNodeById(id);
		if (node != null) {
			return node.toPlainTextString().trim();
		}
		return null;
	}

	public String getPlainTextFromNodeById(String id, String defaultValue) {
		Node node = getNodeById(id);
		if (node != null) {
			return node.toPlainTextString().trim();
		}
		return defaultValue;
	}

	public String getValueFromAbsoluteCell(int colOffSet, int rowOffSet, String labelToLookFor, boolean exactMatch) {
		return getValueFromAbsoluteCell(colOffSet, rowOffSet, labelToLookFor, exactMatch, true);
	}

	public String getValueFromAbsoluteCell(int colOffSet, int rowOffSet, String labelToLookFor, boolean exactMatch, boolean asHtml) {
		return HtmlParser3.getValueFromAbsoluteCell(rowOffSet, colOffSet, HtmlParser3.findNode(getNodeList(), labelToLookFor, exactMatch), "", asHtml);
	}
	
	public String getValueFromAbsoluteCell(int colOffSet, int rowOffSet, String labelToLookFor) {
		return getValueFromAbsoluteCell(colOffSet, rowOffSet, labelToLookFor, false);
	}
	
	/**
	 * Return the value from the next TableColumn (td)
	 * @param tc the initial table column
	 * @param regex
	 * @param asHtml
	 * @return
	 */
	public static String getValueFromNextCell(TableColumn tc, String regex, boolean asHtml) {
		if(tc != null) {
			Node parent = tc.getParent();
			if(parent instanceof TableRow) {
				TableColumn[] columns = ((TableRow)parent).getColumns();
				for (int i = 0; i < columns.length; i++) {
					if(columns[i].equals(tc)) {
						if(i+1 < columns.length) {
							TableColumn resultTd = columns[i + 1];
							String result = null;
							if(asHtml) {
								result = resultTd.toHtml().trim();
							} else {
								result = resultTd.toPlainTextString().trim();
							}
							
							if (!StringUtils.isEmpty(regex)) {
								Pattern p = Pattern.compile(regex);
								Matcher m = p.matcher(result);
								if (m.find()) {
									return m.group(1);
								}
							} else {
								return result;
							}
							
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Traverses a node list by using the values in @param indexInParentNode as
	 * the index of the children list of its direct parent.
	 * <TABLE>
	 * <TR>
	 * <TD>
	 * </TD>
	 * <TD>
	 * </TD>
	 * <TD>
	 * </TD>
	 * </TR>
	 * </TABLE>
	 * To get the 2nd TD you should put the root nodeList and {0,0,1}
	 * 
	 * @param list
	 * @param indexInParentNode
	 * @return
	 */
	public static Node traverseNodelist(NodeList list, int[] indexInParentNode) {
		Node node = null;
		NodeList children = list;
		for (int indexInArray = 0; indexInArray < indexInParentNode.length && children != null; indexInArray++) {
			int i = indexInParentNode[indexInArray];
			if (children != null) {
				if (children.size() > i) {
					node = children.elementAt(i);
					children = node != null ? node.getChildren() : null;
				}
			}
		}
		return node;
	}

	public static NodeList getNodesBetween(NodeList allNodes, Node start, Node end) {
		Node[] allNodesArray = allNodes.toNodeArray();
		NodeList newNodes = new NodeList();

		boolean foundStart = false;
		for (Node node : allNodesArray) {
			if (node == start) {
				foundStart = true;
			}
			if (foundStart) {
				newNodes.add(node);
			}
			if (node == end) {
				break;
			}
		}

		return newNodes;
	}

	/**
	 * Retrieves the value of a node from a node list identifying it by the
	 * value contained in a neighboring cell @param nodeLabel and the @param
	 * offSetRow @param offSetColumn.
	 * 
	 * @param parser
	 * @param nodeLabel
	 * @param offSetRow
	 * @param offSetColumn
	 * @return
	 */
	public static String getNodeValue(HtmlParser3 parser, String nodeLabel, int offSetRow, int offSetColumn) {
		return HtmlParser3.getValueFromAbsoluteCell(offSetRow, offSetColumn, HtmlParser3.findNode(parser.getNodeList(), nodeLabel), "",
				true);
	}

	public Collection<InputTag> getInputsOfType(FormTag form, String type) {
		if (form == null) {
			throw new NullPointerException("Parameters <form> cannot be null!");
		}
		if (type == null) {
			throw new NullPointerException("Parameters <type> cannot be null!");
		}
		Collection<InputTag> result = new ArrayList<InputTag>();
		NodeList formInputs = form.getFormInputs();
		for (int i = 0; i < formInputs.size(); i++) {
			Node n = formInputs.elementAt(i);
			if (n instanceof InputTag) {
				if (type.equalsIgnoreCase(((InputTag) n).getAttribute("type"))) {
					result.add((InputTag) n);
				}
			}
		}
		return result;
	}

	public static Node getNodeByTypeAttributeDescription(NodeList nl, String type, String attributeName, String attributeValue, String[] description,
			boolean recursive) {
		NodeList returnList = null;
	
		if (!StringUtils.isEmpty(attributeName))
			returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive).extractAllNodesThatMatch(
					new HasAttributeFilter(attributeName, attributeValue), recursive);
		else
			returnList = nl.extractAllNodesThatMatch(new TagNameFilter(type), recursive);
	
		for (int i = returnList.size() - 1; i >= 0; i--) {
			boolean flag = true;
			for (String s : description) {
				if (!StringUtils.containsIgnoreCase(returnList.elementAt(i).toHtml(), s))
					flag = false;
			}
			if (flag)
				return returnList.elementAt(i);
		}
	
		return null;
	}


	public static TableTag findTableFromColumn(TableColumn myTd) {
		if(myTd != null) {
			Node node = myTd.getParent();
			while(node != null) {
				if(node instanceof TableTag) {
					return (TableTag)node;
				}
				node = node.getParent();
			}
		}
		return null;
	}
}
