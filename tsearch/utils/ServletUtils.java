package ro.cst.tsearch.utils;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Cristian Stochina
 */
public class ServletUtils {
	
	public static boolean writeImageToClient(byte[] content, String contentType, HttpServletResponse servletResponse) {
		boolean retVal = true;
		servletResponse.setContentType(contentType);
		servletResponse.setContentLength(content.length);
		OutputStream outputStream = null;
		try {
			outputStream = servletResponse.getOutputStream();
			outputStream.write(content);
		} catch (IOException e) {
			retVal = false;
		} finally {
			if (outputStream != null) {
				try {
					outputStream.flush();
					outputStream.close();
				} catch (IOException e) {
				}
			}
		}
		return retVal;
	}

}
