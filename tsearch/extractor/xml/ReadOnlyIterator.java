package ro.cst.tsearch.extractor.xml;

import java.util.*;

public class ReadOnlyIterator implements Iterator {
    
    protected Iterator iterator;

    public ReadOnlyIterator(Iterator it) {
        iterator=it;
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public Object next() throws NoSuchElementException {
        return iterator.next();
    }

    public void remove() {;}
}
