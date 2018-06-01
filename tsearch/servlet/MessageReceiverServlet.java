package ro.cst.tsearch.servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;

import ro.cst.tsearch.ServerConfig;
import ro.cst.tsearch.database.rowmapper.NoteMapper;
import ro.cst.tsearch.database.rowmapper.NoteMapper.TYPE;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.TSOpCode;

/**
 * 
 * @author MihaiB
 * 
 * 
 */

public class MessageReceiverServlet extends HttpServlet {

	private static final long serialVersionUID = 3875602459113202789L;

	public static final String	CODE_GET_LAST_NOTE	= "1";
	public static final String	CANCEL_NOTE			= "2";
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		String searchIdString = req.getParameter("searchId");
		String userIdString = req.getParameter("userId");
		String opCode = req.getParameter(TSOpCode.OPCODE);
		
		if(opCode == null) {
			return;
		}
		
		switch (opCode) {
		case CODE_GET_LAST_NOTE: 
		{
			if(searchIdString == null || !searchIdString.matches("\\d+") 
					|| userIdString == null || !userIdString.matches("\\d+")) {
				return;
			}
			
			res.setContentType("application/json");
			
			JSONObject jsonObject = new JSONObject(getJsonMapMessage(Long.parseLong(searchIdString), Integer.parseInt(userIdString)));
			
			res.getWriter().write(jsonObject.toString());
		}
			break;
		case CANCEL_NOTE:
		{
			String nid = req.getParameter("nid");
			if(searchIdString == null || !searchIdString.matches("\\d+") 
					|| userIdString == null || !userIdString.matches("\\d+")
					|| nid == null || !nid.matches("\\d+")) {
				return;
			}

			NoteMapper.markNoteClosed(Long.parseLong(searchIdString), TYPE.SAVE.getValue(), Integer.parseInt(userIdString), Long.parseLong(nid));
			
		}
			break;
		default:
			break;
		}
		
		
	}
	
	private Map<String, Object> getJsonMapMessage(long searchId, int userId) {
		List<NoteMapper> notes = NoteMapper.getAllNonEmptyNotes(searchId, TYPE.SAVE.getValue(), userId);
		
		Map<String, Object> jsonMap = new HashMap<>();
		
		Calendar now = Calendar.getInstance();
		
		if (notes != null && notes.size() > 0){
			NoteMapper note = notes.get(notes.size() -1);
			
			Date timeStampDate = note.getNoteTimestamp();
			if (timeStampDate != null){
				Calendar cal = Calendar.getInstance();
				cal.setTime(timeStampDate);
				cal.add(Calendar.MINUTE, ServerConfig.getNoteDisplayValabilityPeriod());
				
				if (cal.after(now)){
					
					String message = note.getNoteNote();
					if(message != null) {
						message = message.replaceAll("\\n", "<br>").trim();
					}
					
					if(StringUtils.isBlank(message)) {
						return jsonMap;
					}
					
					jsonMap.put("message", message);
					jsonMap.put("nid", note.getNoteId());
					UserAttributes ua = null;
					try {
						ua = UserUtils.getUserFromId(new BigDecimal(note.getNoteUserId()));
					} catch (BaseException e) {
					}
					if (ua != null){
						jsonMap.put("sentBy", 
								"&nbsp;&nbsp;Sent at: "
									+ FormatDate.getDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY_SPACE_HH_COLON_mm_COLON_ss).format(note.getNoteTimestamp()) 
									+ note.getNoteTimestamp()
									+ " by " 
									+ ua.getNiceName()
								);
					}
				}
			}
		}
		
		return jsonMap;
	}

}
