package ro.cst.tsearch.servers.functions.smartUpload;

import java.util.List;

import com.stewart.ats.base.document.DocumentI;
import com.stewart.ats.smartupload.client.FrameContent;

public interface SmartUpload {

	List<ParseResult> parseDocument(FrameContent content);
	
	boolean downloadAndSetImage(DocumentI doc, FrameContent content, ParseResult parseResults, long searchId);
}
