package ro.cst.tsearch.servlet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.regex.Matcher;

import org.apache.log4j.Category;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.UploadPolicyDocException;
import ro.cst.tsearch.servlet.community.UploadPolicyDoc;
import ro.cst.tsearch.templates.AddDocsTemplates;
import ro.cst.tsearch.templates.emptytemplateedit.client.EmptyTemplateEditException;
import ro.cst.tsearch.templates.emptytemplateedit.client.EmptyTemplateEditService;
import ro.cst.tsearch.utils.DBConstants;
import ro.cst.tsearch.utils.StringUtils;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class EmptyTemplateEditServlet extends RemoteServiceServlet implements
		EmptyTemplateEditService {

	private static final long serialVersionUID = 2698298800349471435L;
	private static final Category logger = Category
			.getInstance(EmptyTemplateEditServlet.class);

	/**
	 * Save the template content
	 * 
	 * @param policyId
	 * @param commId
	 * @param policyName
	 * @param shortPolicyName
	 * @param fileContent
	 * @return
	 * @throws EmptyTemplateEditException
	 */

	public Boolean saveTemplate(long policyId, int commId, String policyName,
			String shortPolicyName, String fileContent)
			throws EmptyTemplateEditException {

		Boolean returnCode = Boolean.TRUE;
		File tempDir = new File(UploadPolicyDoc.getTemplatesPath(commId));

		// prepare the form content for insert
		String fileName = "";

		if (policyId > 0) {
			HashMap values = DBManager.fetchOneRow((int) policyId,
					DBConstants.TABLE_COMMUNITY_TEMPLATES,
					DBConstants.FIELD_COMMUNITY_TEMPLATES_ID);
			fileName = values.get(
					DBConstants.FIELD_COMMUNITY_TEMPLATES_FILENAME).toString();
		} else {
			fileName = policyName;
		}

		int extSeparatorPos = fileName.lastIndexOf(".");
		String fileExt = fileName.substring(extSeparatorPos + 1);
		fileContent = formatInsertContent(fileContent, fileExt);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent
				.getBytes());

		try {
			policyId = UploadPolicyDoc.uploadFile(policyId, policyName,
					shortPolicyName, fileName, inputStream, commId, tempDir,
					new StringBuilder(), new StringBuilder());
		} catch (UploadPolicyDocException ue) {
			returnCode = Boolean.FALSE;
			throw new EmptyTemplateEditException(
					"There was a problem saving the file !!!");
		}

		return returnCode;
	}

	/**
	 * Removes the template using the editor
	 * 
	 * @param commId
	 * @param policyId
	 * @throws EmptyTemplateEditException
	 */

	public Boolean deleteTemplate(int commId, int policyId)
			throws EmptyTemplateEditException {

		Boolean returnCode = Boolean.TRUE;
		String dirPath = UploadPolicyDoc.getTemplatesPath(commId);
		try {
			UploadPolicyDoc.removeFile(new File(dirPath), policyId);
		} catch (UploadPolicyDocException upde) {
			upde.printStackTrace();
			returnCode = Boolean.FALSE;
			throw new EmptyTemplateEditException(
					"There was en error in deleting the files!");
		}
		return returnCode;
	}

	/**
	 * Returns file content
	 * 
	 * @param fileId
	 * @return
	 * @throws EmptyTemplateEditException
	 */

	public String getTemplateContent(int fileId)
			throws EmptyTemplateEditException {

		String pageContent = "";

		// set blob content info
		DBFileView.setBlobFileInfo(fileId,
				DBConstants.TABLE_COMMUNITY_TEMPLATES);

		// get blob content
		pageContent = DBFileView.getBlobContentAsString();

		return pageContent;
	}

	/*
	 * parameters: string file content string file extension return: formatted
	 * file content description: For html files this function replaces strict
	 * html elements with coresponding text characters in IF THEN ELSE blocks
	 * For non html files , it will make replace of all html elements as
	 * described above
	 */
	private String formatInsertContent(String content, String fileExt) {

		if (isHtmlFile(fileExt)) {
			StringBuffer strBufContent = new StringBuffer(content);

			Matcher mat = AddDocsTemplates.pattExpresionBlock
					.matcher(strBufContent);
			int xx = 0, escape = 0;
			while (mat.find()) {
				xx = 0;
				while (xx < escape) {
					mat.find();
					xx++;
				}
				escape++;
				try {
					strBufContent.replace(mat.start(), mat.end(), StringUtils
							.prepareStringForHTML(mat.group()));
					mat.reset();
				} catch (IllegalStateException e) {
					break;
				}
			}
			content = strBufContent.toString();
			content = content.replaceAll("&lt;#([^#\n\r]*)/#&gt;", "<#$1/#>");

			// makes some editor replacements
			content = content.replaceAll("<strong>", "<b>");
			content = content.replaceAll("</strong>", "</b>");

		}

		return content;
	}

	/**
	 * Decides if a file is html or not
	 * 
	 * @param fileExt
	 * @return boolean isHtml
	 */
	private static boolean isHtmlFile(String fileExt) {

		String[] htmlExtensions = { "htm", "html" };
		for (int i = 0; i < htmlExtensions.length; i++)
			if (htmlExtensions[i].equalsIgnoreCase(fileExt)) {
				return true;
			}

		return false;
	}

}
