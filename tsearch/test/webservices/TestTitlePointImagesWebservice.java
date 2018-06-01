package ro.cst.tsearch.test.webservices;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.titlepoint.www.GetDocumentReturn;

public class TestTitlePointImagesWebservice {
	
	private static final Logger logger = Logger.getLogger(TestTitlePointImagesWebservice.class);
	
	public static byte [] getData(GetDocumentReturn docRet) throws IOException{
		
		/*if(logger.isInfoEnabled()){
			logger.info("Request Status: " + docRet.getStatus());
		}
		
		if(docRet.getStatus().getCode() != 0){
			logger.error("Non zero return status code: " + docRet.getStatus().getCode());
			return null;
		}
		
		if(logger.isInfoEnabled()){
			logger.info("Number of returned documents: " + docRet.getDocuments().getDocumentResponse().length);
		}
		
		if(docRet.getDocuments().getDocumentResponse().length!= 1){
			logger.error("Number of documents not equal to one: " + docRet.getDocuments().getDocumentResponse().length);
			return null;
		}
		
		DocumentResponse docRes = docRet.getDocuments().getDocumentResponse()[0];
		
		if(logger.isInfoEnabled()){
			logger.info("Document Status: " + docRes.getDocStatus());
		}
		
		if(docRes.getDocStatus().getCode() != 0){
			logger.error("Non zero document status code: " + docRet.getStatus().getCode());
			return null;
		}

		Document doc = docRes.getDocument();
		
		if(logger.isDebugEnabled()) {
			logger.debug("Document Key: " + doc.getKey());
			logger.debug("Document Properties: " + doc.getProperties());
		} else if (logger.isInfoEnabled()) {
			logger.info("Document Key: " + ro.cst.tsearch.utils.StringUtils.getMaxCharacters(doc.getKey().toString(), HTTPSiteInterface.MAX_CHARACTERS_TO_LOG_ON_INFO));
			logger.info("Document Properties: " + ro.cst.tsearch.utils.StringUtils.getMaxCharacters(doc.getProperties().toString(), HTTPSiteInterface.MAX_CHARACTERS_TO_LOG_ON_INFO));
		}
		
		return InputStreamUtils.getBytes(doc.getBody().getBody().getInputStream());
		*/
		return null;
	}
	
	
}
