package ro.cst.tsearch.test.xstream;

import java.util.Vector;

public class TestObject1 {
	private String field2 = "mimi";
	private int field1 = 2;
	//private String field1 = "gogu";
	//private int field2 = 1;
	private Vector<String> field4 = new Vector<String>();
	private TestObject4 field5 = new TestObject4();
	
	public TestObject1() {
		// TODO Auto-generated constructor stub
	}
	
	public TestObject1(String param) {
		field4.add(param);
	}

	/**
	 * @return the field2
	 */
	public String getField2() {
		return field2;
	}

	/**
	 * @param field2 the field2 to set
	 */
	public void setField2(String field2) {
		this.field2 = field2;
	}

	/**
	 * @return the field1
	 */
	public int getField1() {
		return field1;
	}

	/**
	 * @param field1 the field1 to set
	 */
	public void setField1(int field1) {
		this.field1 = field1;
	}

	/**
	 * @return the field4
	 */
	public Vector<String> getField4() {
		return field4;
	}

	/**
	 * @param field4 the field4 to set
	 */
	public void setField4(Vector<String> field4) {
		this.field4 = field4;
	}

	/**
	 * @return the field5
	 */
	public TestObject4 getField5() {
		return field5;
	}

	/**
	 * @param field5 the field5 to set
	 */
	public void setField5(TestObject4 field5) {
		this.field5 = field5;
	}

}
