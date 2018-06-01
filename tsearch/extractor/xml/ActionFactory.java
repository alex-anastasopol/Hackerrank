package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.*;

public class ActionFactory {

    protected static String getName(Element el) {
        String name=el.getNodeName();
        name=name.substring(0, 1).toUpperCase()+name.substring(1).toLowerCase();
        String pack=new ActionFactory().getClass().getName();
        pack=pack.substring(0, pack.lastIndexOf('.')+1);
        name=pack+name+"Action";
        return name;
    }

    public static BaseAction createAction(XMLExtractor xmle, Element el,long searchId) throws Exception {
        return (BaseAction) Class.forName(getName(el)).getDeclaredConstructor(new Class[]{XMLExtractor.class, Element.class,long.class}).newInstance(new Object[]{xmle, el,new Long(searchId)});
    }

    public static BaseAction createAction(BaseAction ba, Element el) throws Exception {
        return (BaseAction) Class.forName(getName(el)).getDeclaredConstructor(new Class[]{BaseAction.class, Element.class,long.class}).newInstance(new Object[]{ba, el,ba.searchId});
    }
}
