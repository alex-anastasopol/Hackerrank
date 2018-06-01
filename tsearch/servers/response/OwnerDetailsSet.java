package ro.cst.tsearch.servers.response;

/**
 * 
 * unused class , keep here just for deserialization purposes
 *
 */
@Deprecated
public class OwnerDetailsSet extends InfSet {

	private static final long serialVersionUID = 3320746757351034192L;

	/**
	 * unused
	 */
	public OwnerDetailsSet() {
        super(new String[]{
        		"Book",  
        		"Page",  
        		"MailingAddress",  
        		"MailingCity",  
        		"MailingZipCode",  
        		"MailingState",  
        		"MailingStreetNumber",  
        		"MailingStreetName"
        });
    }
	
	public enum  OwnerDetailsSetKey {
		BOOK("Book"),
		PAGE("Page"),
		MAILING_ADDRESS("MailingAddress"),
		MAILING_CITY("MailingCity"),
		MAILING_ZIP_CODE("MailingZipCode"),
		MAILING_STATE("MailingState"),
		MAILING_STREET_NUMBER("MailingStreetNumber"),
		MAILING_STREET_NAME("MailingStreetName");

		String keyName;

		OwnerDetailsSetKey(String keyName) {
			this.keyName = "OwnerDetailsSet." + keyName;
		}

		public String getKeyName() {
			return keyName;
		}
	}

}
