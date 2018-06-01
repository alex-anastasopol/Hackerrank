package ro.cst.tsearch.servlet.download;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ro.cst.tsearch.data.User;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.servlet.BaseServlet;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.FileUtils;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.ParameterParser;
import ro.cst.tsearch.utils.SessionParams;
import ro.cst.tsearch.utils.TSParameters;

public class DownloadFile extends BaseServlet {
	//se tin imaginile specifice tipului de fisier.
	private static HashMap hashCorespImg;
	// keep the extension of the all known extension files
	private static HashMap hashFileExt;

	/**The root of the documents*/
	public static String ROOT;

	public static String IMG_FILE_UNKNOWN;
	public static String EXT_UNKNOWN;

	static {
		EXT_UNKNOWN = new String("application/x-unknown");

		hashFileExt = new HashMap();

		hashFileExt.put(".hqx", new String("application/mac-binhex40"));
		hashFileExt.put(".cpt", new String("application/mac-compactpro"));
		hashFileExt.put(".doc", new String("application/msword"));
		hashFileExt.put(".DOC", new String("application/msword"));
		hashFileExt.put(".dot", new String("application/msword;"));
		hashFileExt.put(".bin", new String("application/octet-stream"));
		hashFileExt.put(".dms", new String("application/octet-stream"));
		hashFileExt.put(".lha", new String("application/octet-stream"));
		hashFileExt.put(".lzh", new String("application/octet-stream"));
		hashFileExt.put(".exe", new String("application/octet-stream"));
		hashFileExt.put(".class", new String("application/octet-stream"));
		hashFileExt.put(".oda", new String("application/oda"));
		hashFileExt.put(".pdf", new String("application/pdf"));
		hashFileExt.put(".ai", new String("application/postscript"));
		hashFileExt.put(".eps", new String("application/postscript"));
		hashFileExt.put(".ppt", new String("application/powerpoint"));
		hashFileExt.put(".rtf", new String("application/rtf"));
		hashFileExt.put(".xls", new String("application/vnd.ms-excel"));
		hashFileExt.put(".xlt", new String("application/vnd.ms-excel;"));
		hashFileExt.put(".bcpio", new String("application/x-bcpio"));
		hashFileExt.put(".vcd", new String("application/x-cdlink"));
		hashFileExt.put(".z", new String("application/x-compress"));
		hashFileExt.put(".cpio", new String("application/x-cpio"));
		hashFileExt.put(".csh", new String("application/x-csh"));
		hashFileExt.put(".dcr", new String("application/x-director"));
		hashFileExt.put(".dir", new String("application/x-director"));
		hashFileExt.put(".dxr", new String("application/x-director"));
		hashFileExt.put(".dvi", new String("application/x-dvi"));
		hashFileExt.put(".gtar", new String("application/x-gtar"));
		hashFileExt.put(".gz", new String("application/x-gzip"));
		hashFileExt.put(".hdf", new String("application/x-hdf"));
		hashFileExt.put(".cgi", new String("application/x-httpd-cgi"));
		hashFileExt.put(".skp", new String("application/x-koan"));
		hashFileExt.put(".skd", new String("application/x-koan"));
		hashFileExt.put(".skm", new String("application/x-koan"));
		hashFileExt.put(".latex", new String("application/x-latex"));
		hashFileExt.put(".mif", new String("application/x-mif"));
		hashFileExt.put(".nc", new String("application/x-netcdf"));
		hashFileExt.put(".cdf", new String("application/x-netcdf"));
		hashFileExt.put(".sh", new String("application/x-sh"));
		hashFileExt.put(".shar", new String("application/x-shar"));
		hashFileExt.put(".sit", new String("application/x-stuffit"));
		hashFileExt.put(".sv4cpio", new String("application/x-sv4cpio"));
		hashFileExt.put(".sv4crc", new String("application/x-sv4crc"));
		hashFileExt.put(".tar", new String("application/x-tar"));
		hashFileExt.put(".tcl", new String("application/x-tcl"));
		hashFileExt.put(".tex", new String("application/x-tex"));
		hashFileExt.put(".texinfo", new String("application/x-texinfo"));
		hashFileExt.put(".texi", new String("application/x-texinfo"));
		hashFileExt.put(".t", new String("application/x-troff"));
		hashFileExt.put(".tr", new String("application/x-troff"));
		hashFileExt.put(".man", new String("application/x-troff-man"));
		hashFileExt.put(".roff", new String("application/x-troff"));
		hashFileExt.put(".man", new String("application/x-troff-man"));
		hashFileExt.put(".me", new String("application/x-troff-me"));
		hashFileExt.put(".ms", new String("application/x-troff-ms"));
		hashFileExt.put(".ustar", new String("application/x-ustar"));
		hashFileExt.put(".src", new String("application/x-wais-source"));
		hashFileExt.put(".zip", new String("application/zip"));
		hashFileExt.put(".au", new String("audio/basic"));
		hashFileExt.put(".snd", new String("audio/basic"));
		hashFileExt.put(".mid", new String("audio/midi"));
		hashFileExt.put(".midi", new String("audio/midi"));
		hashFileExt.put(".mpga", new String("audio/mpeg"));
		hashFileExt.put(".mp2", new String("audio/mpeg"));
		hashFileExt.put(".aif", new String("audio/x-aiff"));
		hashFileExt.put(".aiff", new String("audio/x-aiff"));
		hashFileExt.put(".aifc", new String("audio/x-aiff"));
		hashFileExt.put(".ram", new String("audio/x-pn-realaudio"));
		hashFileExt.put(".ra", new String("audio/x-realaudio"));
		hashFileExt.put(".rpm", new String("audio/x-pn-realaudio-plugin"));
		hashFileExt.put(".wav", new String("audio/x-wav"));
		hashFileExt.put(".pdb", new String("chemical/x-pdb"));
		hashFileExt.put(".xyz", new String("chemical/x-pdb"));
		hashFileExt.put(".gif", new String("image/gif"));
		hashFileExt.put(".ief", new String("image/ief"));
		hashFileExt.put(".jpeg", new String("image/jpeg"));
		hashFileExt.put(".jpg", new String("image/jpeg"));
		hashFileExt.put(".jpe", new String("image/jpeg"));
		hashFileExt.put(".png", new String("image/png"));
		hashFileExt.put(".tiff", new String("image/tiff"));
		hashFileExt.put(".tif", new String("image/tiff"));
		hashFileExt.put(".ras", new String("image/x-cmu-raster"));
		hashFileExt.put(".pnm", new String("image/x-portable-anymap"));
		hashFileExt.put(".pbm", new String("image/x-portable-bitmap"));
		hashFileExt.put(".pgm", new String("image/x-portable-graymap"));
		hashFileExt.put(".ppm", new String("image/x-portable-pixmap"));
		hashFileExt.put(".rgb", new String("image/x-rgb"));
		hashFileExt.put(".xpm", new String("image/x-xpixmap"));
		hashFileExt.put(".xbm", new String("image/x-xbitmap"));
		hashFileExt.put(".xwd", new String("image/x-xwindowdump"));
		hashFileExt.put(".html", new String("text/html"));
		hashFileExt.put(".htm", new String("text/html"));
		hashFileExt.put(".txt", new String("text/plain"));
		hashFileExt.put(".rtx", new String("text/richtext"));
		hashFileExt.put(".tsv", new String("text/tab-separated-values"));
		hashFileExt.put(".etx", new String("text/x-setext"));
		hashFileExt.put(".sgml", new String("text/x-sgml"));
		hashFileExt.put(".mpeg", new String("video/mpeg"));
		hashFileExt.put(".mpg", new String("video/mpeg"));
		hashFileExt.put(".mpe", new String("video/mpeg"));
		hashFileExt.put(".qt", new String("video/quicktime"));
		hashFileExt.put(".mov", new String("video/quicktime"));
		hashFileExt.put(".avi", new String("video/x-msvideo"));
		hashFileExt.put(".movie", new String("video/x-sgi-movie"));
		hashFileExt.put(".ice", new String("x-conference/x-cooltalk"));
		hashFileExt.put(".wrl", new String("x-world/x-vrml"));
		hashFileExt.put(".mime", new String("message/rfc822"));
		hashFileExt.put(".txt", new String("text/plain"));
		hashFileExt.put(".xml", new String("text/xml"));
	}

	/**
	 * Get specific extention of document
	 * @param fileName
	 * @return String
	 */
	public static String getSpecificExt(String fileName) {
		try {
			//String ext = fileName.substring(fileName.length() - 4);
			String ext = FileUtils.getFileExtension(fileName);
			if (hashFileExt.containsKey(ext))
				return (String) hashFileExt.get(ext);
			else
				return EXT_UNKNOWN;
		} catch (Exception e) {

			return EXT_UNKNOWN;
		}
	}

	public void doRequest(HttpServletRequest request, HttpServletResponse response)
		throws IOException, ServletException, BaseException{

		ParameterParser pp = new ParameterParser(request);
		HttpSession session = request.getSession();
		User currentUser = (User) session.getAttribute(SessionParams.CURRENT_USER);
		UserAttributes ua =
			currentUser.getUserAttributes();
		/*CommunityAttributes ca =
			InstanceManager.getCurrentInstance().getCurrentCommunity();*/
		String action = pp.getStringParameter(TSParameters.ACTION, "");
		String user_id = pp.getStringParameter(UserAttributes.USER_ID, "");

		if (action.equals("VIEW_PICTURE")) {
			byte[] photo = UserUtils.getUserPicture(user_id);
			if (photo != null) {
				response.setContentType("image/jpeg");
				response.setContentLength(photo.length);
				ServletOutputStream sout = response.getOutputStream();
				sout.write(photo, 0, photo.length);
				sout.flush();
				sout.close();
			}
			return;
		} else if (action.equals("DOWNLOAD_RESUME")) {
			byte[] resume = UserUtils.getUserResume(new BigDecimal(user_id));
			String resumeName =
				UserUtils.getUserResumeName(new BigDecimal(user_id));

			if (resume != null) {

				//response.setContentType ("application/x-unknown");	       
				//response.setContentType ("application/x-msdownload");
				response.setContentType(
					(String)getSpecificExt(resumeName));
				response.setContentLength(resume.length);
				response.setHeader(
					"Content-Disposition",
					" attachment; filename=\""
						+ UserUtils.getUserResumeName(new BigDecimal(user_id))
						+ "\"");
				ServletOutputStream sout = response.getOutputStream();
				sout.write(resume, 0, resume.length);
				sout.flush();
				sout.close();
			}
			return;
		}  
		return;
	}
	
}
