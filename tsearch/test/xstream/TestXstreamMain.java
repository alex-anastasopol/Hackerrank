package ro.cst.tsearch.test.xstream;

import java.util.Calendar;
import java.util.Date;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class TestXstreamMain {
	
	public static void main(String[] args) {
		/*TestObject1 to = new TestObject1("andrei");
		TestObject1 to2 = null;
		XStream xStream = new XStream(new DomDriver());
        xStream.registerConverter(new TestObject1Converter());
        //xStream.alias("person", Person.class);
        //System.out.println(xStream.toXML(to));
        
        to2 = (TestObject1)xStream.fromXML("<ro.cst.tsearch.test.xstream.TestObject1>" +
        		"<field2>mimi</field2>" +
        		"<field1>2</field1>" +
        		"<field4><string>andrei</string></field4>" +
        		"<field5><field1>TestObject2</field1></field5>" +
        		"</ro.cst.tsearch.test.xstream.TestObject1>");
        
        System.out.println(to2);
        */
		Calendar c = Calendar.getInstance();
		System.out.println(c.getTime());
		c.add(Calendar.MONTH, -24);
		c.add(Calendar.DAY_OF_MONTH,-1);
		System.out.println(c.getTime());
        
	}

}
