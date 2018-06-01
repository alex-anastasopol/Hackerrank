package ro.cst.tsearch.extractor.xml;

import org.w3c.dom.Node;

interface NodeProcessor {
    boolean nodeProcess(Node n) throws Exception;
}
