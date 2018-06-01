package ro.cst.tsearch.database.rowmapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import ro.cst.tsearch.database.DBManager;
import ro.cst.tsearch.exceptions.BaseException;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.user.UserUtils;
import ro.cst.tsearch.utils.FormatDate;
import ro.cst.tsearch.utils.StringUtils;

public class NoteMapper implements ParameterizedRowMapper<NoteMapper> {

	/**
	 * Mapper for notes table	
	 */
	
	private static final Logger logger = Logger.getLogger(NoteMapper.class);
	
	public static final String TABLE_NOTE = "note";	
	
	public static final String FIELD_NOTE_ID = "note_id";
	public static final String FIELD_NOTE_SEARCH_ID = "note_search_id";
	public static final String FIELD_NOTE_NOTE = "note_note";
	public static final String FIELD_NOTE_OPERATION = "note_operation";
	public static final String FIELD_NOTE_USER_ID = "note_user_id";
	public static final String FIELD_NOTE_TIMESTAMP = "note_timestamp";
	public static final String FIELD_NOTE_CLOSED = "note_closed";
	
	public static enum TYPE{
        OPEN(0),
        SAVE(1),
        UNLOCKED(2),
        ASSIGNED(3),
        CHANGE_DUE_DATE(4),
        FROM_ADD_INFO(5),
        OLD_NOTE(6);
        
        private final int value;

        public int getValue() { return value; }
        
        private TYPE(final int newValue) {
            value = newValue;
        }
    }
	
	private long 	note_id;
	private long 	note_search_id;
	private String 	note_note;
	private int 	note_operation;
	private int 	note_user_id;
	private Date 	note_timestamp;
	private boolean	note_closed;
	
	
	@Override
	public NoteMapper mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		NoteMapper notesMapper = new NoteMapper();
		
		notesMapper.setNoteId(resultSet.getLong(FIELD_NOTE_ID));
		notesMapper.setNoteSearchId(resultSet.getLong(FIELD_NOTE_SEARCH_ID));
		notesMapper.setNoteNote(resultSet.getString(FIELD_NOTE_NOTE));
		notesMapper.setNoteOperation(resultSet.getInt(FIELD_NOTE_OPERATION));
		notesMapper.setNoteUserId(resultSet.getInt(FIELD_NOTE_USER_ID));
		notesMapper.setNoteTimestamp(resultSet.getTimestamp(FIELD_NOTE_TIMESTAMP));
		notesMapper.setNoteClosed(resultSet.getBoolean(FIELD_NOTE_CLOSED));

		return notesMapper;
	}

	/**
	 * @return the notes_id
	 */
	public long getNoteId() {
		return note_id;
	}

	/**
	 * @param notes_id the notes_id to set
	 */
	public void setNoteId(long notes_id) {
		this.note_id = notes_id;
	}

	/**
	 * @return the notes_search_id
	 */
	public long getNoteSearchId() {
		return note_search_id;
	}

	/**
	 * @param notes_search_id the notes_search_id to set
	 */
	public void setNoteSearchId(long notes_search_id) {
		this.note_search_id = notes_search_id;
	}

	/**
	 * @return the notes_note
	 */
	public String getNoteNote() {
		return note_note;
	}

	/**
	 * @param notes_note the notes_note to set
	 */
	public void setNoteNote(String notes_note) {
		this.note_note = notes_note;
	}

	/**
	 * @return the notes_operation
	 */
	public int getNoteOperation() {
		return note_operation;
	}

	/**
	 * @param notes_operation the notes_operation to set
	 */
	public void setNoteOperation(int notes_operation) {
		this.note_operation = notes_operation;
	}

	/**
	 * @return the notes_timestamp
	 */
	public Date getNoteTimestamp() {
		return note_timestamp;
	}

	/**
	 * @param notes_timestamp the notes_timestamp to set
	 */
	public void setNoteTimestamp(Date notes_timestamp) {
		this.note_timestamp = notes_timestamp;
	}

	/**
	 * @return the note_user_id
	 */
	public int getNoteUserId() {
		return note_user_id;
	}

	/**
	 * @param note_user_id the note_user_id to set
	 */
	public void setNoteUserId(int note_user_id) {
		this.note_user_id = note_user_id;
	}

	public boolean isNoteClosed() {
		return note_closed;
	}

	public void setNoteClosed(boolean noteClosed) {
		this.note_closed = noteClosed;
	}

	public String getMessage(){
		StringBuffer message = new StringBuffer();
		UserAttributes ua = null;
		try {
			ua = UserUtils.getUserFromId(new BigDecimal(this.getNoteUserId()));
		} catch (BaseException e) {
			e.printStackTrace();
		}
		
		if (ua != null){
			String date = "";
			if (this.getNoteTimestamp() != null){
				date = FormatDate.getDateFormat(FormatDate.PATTERN_MM_SLASH_DD_SLASH_YYYY_SPACE_HH_COLON_mm_COLON_ss).format(this.getNoteTimestamp());
			}
			switch (this.getNoteOperation()) {
				case 0:
					message.append("\nOpened At: ").append(date).append(" by ").append(ua.getNiceName());
					break;
				case 1:
					message.append("\n").append(this.getNoteNote()).append("\nSaved At: ").append(date).append(" by ").append(ua.getNiceName());
					break;
				case 2:
					message.append("\nUnlocked At: ").append(date).append(" by ").append(ua.getNiceName());
					break;
				case 3:
					message.append("\nAssigned At: ").append(date).append(" to: ").append(ua.getNiceName()).append(" by ").append(ua.getNiceName());
					break;
				case 4:
					message.append("\n").append(this.getNoteNote()).append("  At: ").append(date).append(" by ").append(ua.getNiceName());
					break;
				default:
					break;
				}
		
		}
		return message.toString();
		 
	}

	/**
	 * 
	 * insert note to the existing note table
	 * 
	 * @param availableIdList
	 *            - ',' separated list of search IDs
	 * @param note
	 *            - the note that needs to be appended
	 * @param userId
	 * @param noteOperation
	 * @param dateTimeStr
	 */
	public static void setSearchNote(String availableIdList, String note, int userId, int noteOperation, Date dateTimeStr) {
		availableIdList = StringUtils.makeValidNumberList(availableIdList);
		String[] searchIds = availableIdList.split("\\s*,\\s*");
		for (String searchId : searchIds) {
			long searchid = -1;
			try {
				searchid = Long.parseLong(searchId);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			if (searchid > -1) {
				try {
					NoteMapper newNote = new NoteMapper();
					newNote.setNoteSearchId(searchid);
					newNote.setNoteNote(note);
					newNote.setNoteOperation(noteOperation);
					newNote.setNoteUserId(userId);
					newNote.setNoteTimestamp(dateTimeStr);

					setSearchNote(newNote);
				} catch (Exception e) {
					logger.error("Cannot add a new note for searchid " + searchid, e);
				}
			}
		}

	}

	/**
	 * Add a note
	 * 
	 * @param note
	 * 
	 */
	public static boolean setSearchNote(NoteMapper note) {

		Map<String, Object> query_params = new HashMap<String, Object>();
		query_params.put(FIELD_NOTE_SEARCH_ID, note.getNoteSearchId());
		query_params.put(FIELD_NOTE_NOTE, note.getNoteNote());
		query_params.put(FIELD_NOTE_OPERATION, note.getNoteOperation());
		query_params.put(FIELD_NOTE_USER_ID, note.getNoteUserId());
		query_params.put(FIELD_NOTE_TIMESTAMP, note.getNoteTimestamp());
		query_params.put(FIELD_NOTE_CLOSED, note.isNoteClosed());

		try {
			SimpleJdbcInsert sjt = DBManager.getSimpleJdbcInsert().withTableName(TABLE_NOTE);

			sjt.execute(query_params);
		} catch (Throwable t) {
			logger.error("setSearchNote error " + t);
			return false;
		}

		return true;
	}

	public static int markNoteClosed(long searchId, int operation, int userId, long noteId) {
		String select = "UPDATE " + TABLE_NOTE + " SET " + FIELD_NOTE_CLOSED + " = ? "
				+ " WHERE " + FIELD_NOTE_ID + " = ? "
				+ " AND " + FIELD_NOTE_NOTE + " IS NOT NULL "
				+ " AND " + FIELD_NOTE_NOTE + " != ''"
				+ " AND " + FIELD_NOTE_SEARCH_ID + " = ?"
				+ " AND " + FIELD_NOTE_OPERATION + " = ?"
				+ " AND " + FIELD_NOTE_USER_ID + " != ?";
		try {
			return DBManager.getSimpleTemplate().update(select, 1, noteId, searchId, operation, userId);
		} catch (Exception e) {
			logger.error("Cannot markNoteClosed noteId " + noteId + " in search " + searchId, e);
		}
		return 0;
	}

	public static void updateSearchNoteByType(long searchId, int type, String note) {
		String update = "UPDATE " + TABLE_NOTE + " SET " + FIELD_NOTE_NOTE + " =? "
				+ " WHERE " + FIELD_NOTE_SEARCH_ID + " = ?"
				+ " AND " + FIELD_NOTE_OPERATION + " =?";
		try {
			DBManager.getSimpleTemplate().update(update, note, searchId, type);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static List<NoteMapper> getSearchNoteByType(long searchId, int type) {
		String select = "SELECT * FROM " + TABLE_NOTE + " WHERE " + FIELD_NOTE_SEARCH_ID + " = ?"
				+ " AND " + FIELD_NOTE_OPERATION + " =?";
		try {
			List<NoteMapper> result = DBManager.getSimpleTemplate().query(select, new NoteMapper(), searchId, type);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static List<NoteMapper> getAllNotes(long searchId) {
		String select = "SELECT * FROM " + TABLE_NOTE + " WHERE " + FIELD_NOTE_SEARCH_ID + " = ?"
				+ " ORDER BY " + FIELD_NOTE_TIMESTAMP + " ASC";
		try {
			List<NoteMapper> result = DBManager.getSimpleTemplate().query(select, new NoteMapper(), searchId);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static List<NoteMapper> getAllNonEmptyNotes(long searchId, int operation, int userId) {
		String select = "SELECT * FROM " + TABLE_NOTE + " WHERE " + FIELD_NOTE_NOTE + " IS NOT NULL "
				+ " AND " + FIELD_NOTE_NOTE + " != ''"
				+ " AND " + FIELD_NOTE_SEARCH_ID + " = ?"
				+ " AND " + FIELD_NOTE_OPERATION + " = ?"
				+ " AND " + FIELD_NOTE_USER_ID + " != ?"
				+ " AND " + FIELD_NOTE_CLOSED + " = ?";
		try {
			List<NoteMapper> result = DBManager.getSimpleTemplate().query(select, new NoteMapper(), searchId, operation, userId, 0);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
