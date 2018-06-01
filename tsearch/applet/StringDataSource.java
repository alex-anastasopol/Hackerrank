		    /**
 * @(#) StringDataSource.java 1.3 5/25/01
 * Copyright (c) 1999-2000 CornerSoft Technologies.
 * Bucharest, Romania
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of CornerSoft
 * Technologies ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with CornerSoft.
 *
 * @version 1.0
 */

package ro.cst.tsearch.applet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;
/**
 * StringDataSource class holds the functionality for handling attachements 
 * as a datasource
 * The content type is "text/plain"
 * @author <a href="mailto:Ciprian.Dragoi@cst.ro">Ciprian Dragoi</a>
 */
public class StringDataSource implements DataSource{
    
    /**
     * Constructs this object from a String representing the data wrapped by 
     * this object and the content type of the datas handled 
     * ( used for decoding )
     * @param data is the String object containtng the datas encoded according
     *             with content type
     * @param contentType is the tontent type of the datas supplied 
     *             by the data parameter
     */
    public StringDataSource(String data, String contentType){
        _data = data;
        _contentType = contentType;
    }

    /**
     * Constructs this object from a byte array
     * @param bytes is the <code>byte[]</code> instance containing the entire
     *        value of the datas wrapped
     * @param contentType is the tontent type of the datas supplied 
     *             by the data parameter
     */
    public StringDataSource(byte[] bytes, String contentType){
        _data = new String(bytes);
        _contentType = contentType;
    }
    
    /**
     * Gets the InputStream for handling internal informations
     * @return InputStream
     */
    public InputStream getInputStream() {
        return new ByteArrayInputStream(_data.getBytes());
    }
    
    /**
     * Gets the OutputStream for handling internal informations
     * @return OutputStream
     */
    public OutputStream getOutputStream() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(_data.length());
        byte[] buff = _data.getBytes();
        baos.write(buff,0,buff.length);
        return baos;
    }
    
    /**
     * Gets the content type of the containing datas
     * @return String
     */
    public String getContentType() {
        return _contentType;
    }
        
    /**
     * Gets the name of the DataSource
     * @return String 
     */
    public String getName() {
        return "StringDataSource";
    }
    
    //-----------------------------------------------------------PRIVATE Members
    /** The mimeType of this message*/
    private String _contentType    = "text/plain";
    /** An array with all informations about this object*/
    private String _data           = "";
}


