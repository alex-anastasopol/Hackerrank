package ro.cst.tsearch.test.xstream;

import java.util.Vector;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class TestObject1Converter implements Converter {

	@Override
	public void marshal(Object arg0, HierarchicalStreamWriter arg1,
			MarshallingContext arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		TestObject1 to = new TestObject1();
		while(reader.hasMoreChildren()) {
			reader.moveDown();
			if("field2".equals(reader.getNodeName())) {
				to.setField2(reader.getValue());
			} else if("field1".equals(reader.getNodeName())) {
				to.setField1(Integer.parseInt(reader.getValue()));
			} else if("field4".equals(reader.getNodeName())) {
				to.setField4((Vector)context.convertAnother(to, Vector.class));
			}  else if("field5".equals(reader.getNodeName())) {
				to.setField5((TestObject4)context.convertAnother(to, TestObject4.class));
			}
			reader.moveUp();
		}
		
		return to;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean canConvert(Class clazz) {
		return clazz.equals(TestObject1.class);
	}

}
