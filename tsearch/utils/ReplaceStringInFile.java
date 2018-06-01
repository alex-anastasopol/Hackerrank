/*
 * Created on Nov 26, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ro.cst.tsearch.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * @author george
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ReplaceStringInFile {
	public static void replaceInFile (File fisier, String toBeRepl, String withText) throws IOException{
		try {
			String line = new String();
			String fisierText = new String();
			BufferedReader inRead = new BufferedReader( new InputStreamReader(new FileInputStream(fisier)));
			while((line = inRead.readLine()) != null){
				fisierText += line + "\n";
			}
			inRead.close();
			fisierText = fisierText.replaceAll(toBeRepl,withText);
			OutputStreamWriter outFile = new OutputStreamWriter (new FileOutputStream(fisier));
			outFile.write(fisierText);
			outFile.flush();
			outFile.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}

}
