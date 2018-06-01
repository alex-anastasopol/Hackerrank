package ro.cst.tsearch;

import java.io.File;

public class MailConfig extends ServerConfig {
	
	/**
	 * Configuration key checked <b>mail.order.start</b><br>
	 * Default value <b>false</b>
	 * @return
	 */
	public static boolean isMailOrderStart() {
		return getBoolean("mail.order.start", false);
	}
	/**
	 * Configuration key checked <b>mail.order.smtp.host</b><br>
	 * Default value <b>null</b>
	 * @return
	 */
	public static String getMailOrderSmtpHost() {
		return getString("mail.order.smtp.host", null);
	}
	/**
	 * Configuration key checked <b>mail.order.attachpath</b><br>
	 * Default value <b>getFilePath()/mailOrders/</b>
	 * @return
	 */
	public static String getMailOrderAttachpath() {
		return getString("mail.order.attachpath", getFilePath() + "mailOrders" + File.separator);
	}
	/**
	 * Configuration key checked <b>mail.order.host</b><br>
	 * If not found it will throw an exception
	 * @return
	 */
	public static String getMailOrderHost() {
		return getString("mail.order.host");
	}
	/**
	 * Configuration key checked <b>mail.order.username</b><br>
	 * If not found it will throw an exception
	 * @return
	 */
	public static String getMailOrderUsername() {
		return getString("mail.order.username");
	}
	/**
	 * Configuration key checked <b>mail.order.password</b><br>
	 * If not found it will throw an exception
	 * @return
	 */
	public static String getMailOrderPassword() {
		return getString("mail.order.password");
	}
	/**
	 * Configuration key checked <b>mail.order.searchorders.address</b><br>
	 * This is the address from where the orders are read<br>
	 * If not found it will throw an exception
	 * @return
	 */
	public static String getMailOrderSearchordersAddress() {
		return getString("mail.order.searchorders.address");
	}
	/**
	 * Configuration key checked <b>mail.order.orders.address</b><br>
	 * This is the address where successful searches are sent (as confirmation)<br>
	 * Default value <b>atsmail@stewart.com</b>
	 * @return
	 */
	public static String getMailOrderOrdersAddress() {
		return getString("mail.order.orders.address", "atsmail@stewart.com");
	}
	/**
	 * Configuration key checked <b>mail.order.delay</b><br>
	 * If not found it will throw an exception
	 * @return
	 */
	public static int getMailOrderDelay() {
		return getInteger("mail.order.delay");
	}
	/**
	 * Configuration key checked <b>mail.order.interval</b><br>
	 * If not found it will throw an exception
	 * @return
	 */
	public static int getMailOrderInterval() {
		return getInteger("mail.order.interval");
	}
	/**
	 * Configuration key checked <b>mail.order.succeededfolder</b><br>
	 * This is the folder on the IMAP server where successful emails are copied<br>
	 * Default value <b>SucceededEmailOrders</b>
	 * @return
	 */
	public static String getMailOrderSucceededfolder() {
		return getString("mail.order.succeededfolder", "SucceededEmailOrders");
	}
	/**
	 * Configuration key checked <b>mail.order.failedfolder</b><br>
	 * This is the folder on the IMAP server where failed emails are copied<br>
	 * Default value <b>FailedEmailOrders</b>
	 * @return
	 */
	public static String getMailOrderFailedfolder() {
		return getString("mail.order.failedfolder", "FailedEmailOrders");
	}
	/**
	 * Configuration key checked <b>mail.order.support.address</b><br>
	 * This is the address where (usually) problems are sent (as confirmation)<br>
	 * Default value <b>atsmail@stewart.com</b>
	 * @return
	 */
	public static String getMailOrderSupportAddress() {
		return getString("mail.order.support.address");
	}
	
	/**
	 * Configuration key checked <b>mail.order.maxmessages</b><br>
	 * Max number of messages to be processed at each pass<br>
	 * Default value <b>5</b>
	 * @return
	 */
	public static int getMailOrderMaxmessages() {
		return getInteger("mail.order.maxmessages", 5);
	}
	/**
	 * Configuration key checked <b>mail.order.replyenabled</b><br>
	 * If true, confirmation email will be sent after processing the each email to the sender of the email<br>
	 * Default value <b>false</b>
	 * @return
	 */
	public static boolean getMailOrderReplyenabled() {
		return getBoolean("mail.order.replyenabled", false);
	}
	public static String getOrdersEmailAddress(){
		return ServerConfig.getString("orders.address", "");
	}
	/**
	 * Configuration key checked <b>mail.from</b><br>
	 * Default value <b>empty</b>
	 * @return
	 */
	public static String getMailFrom(){
		return ServerConfig.getString("mail.from", "");
	}
	public static String getMailBcc(){
		return ServerConfig.getString("mail.bcc", "").trim();
	}
	public static String getMailSmtpHost(){
		return ServerConfig.getString("mail.smtp.host","").trim();
	}
	public static String getMailLoggerSubject(){
		return ServerConfig.getString("mail.logger.subject", "");
	}
	/**
	 * Configuration key checked <b>mail.logger.status.address</b><br>
	 * Expected value: email with important notifications (atsstatus@stewart.com)<br>
	 * Default value: <b>getMailLoggerToEmailAddress()</b>
	 * @return
	 */
	public static String getMailLoggerStatusAddress(){
		return ServerConfig.getString("mail.logger.status.address", MailConfig.getMailLoggerToEmailAddress());
	}
	/**
	 * Configuration key checked <b>mail.logger.to</b><br>
	 * Expected value: email with exceptions (atsexceptions@stewart.com)<br>
	 * Default value: <b>empty string</b>
	 * @return
	 */
	public static String getMailLoggerToEmailAddress(){
		return ServerConfig.getString("mail.logger.to", "");
	}
	/* EMAIL ADDRESSES */
	public static String getSupportEmailAddress(){
		return ServerConfig.getString("support.address","");
	}
	
	/**
	 * Configuration key checked <b>ticket.support.address</b><br>
	 * Expected value: email where users send problems and tickets are created<br>
	 * Default value: <b>atssupport@propertyinfo.com</b>
	 * @return
	 */
	public static String getTicketSupportEmailAddress(){
		return ServerConfig.getString("ticket.support.address","atssupport@propertyinfo.com");
	}
	public static String getExceptionEmail() {
		return ServerConfig.getString("ats.exceptions.email", getMailLoggerToEmailAddress());
	}
	public static String getAddressTSAdminChanged() {
		return ServerConfig.getString("address.tsadmin.changed", getSupportEmailAddress());
	}
	public static String getReplicationNotificationEmail(){
		return ServerConfig.getString("replication.notification.email", getMailLoggerStatusAddress() + ", " + getSupportEmailAddress() + ", tmoisa@stewart.com, aalecu@gmail.com");
	}
	public static String getMailDiskNotificationEmail() {
		return ServerConfig.getString("mail.disk.notification.email");
	}
	/**
	 * Configuration key checked <b>mail.order.protocol</b><br>
	 * This is the protocol used to open the mail box<br>
	 * Default value <b>imap</b>
	 * @return
	 */
	public static String getMailOrderProtocol() {
		return ServerConfig.getString("mail.order.protocol", "imap");
	}
}
