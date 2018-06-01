package ro.cst.tsearch.emailClient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.ResourceBundle;

import javax.mail.Session;

import org.apache.commons.mail.*;

import ro.cst.tsearch.MailConfig;
import ro.cst.tsearch.utils.StringUtils;
import ro.cst.tsearch.utils.URLMaping;

/**
 * @author Cornel Send Email Client
 */
public class EmailClient implements Runnable {

	private LinkedList<Name> to = null;

	private String containt = "";

	private String subject = "";

	private String dirTemp = "";

	private LinkedList<Name> cc = null;

	private LinkedList<Name> bcc = null;

	private ResourceBundle rbc = null;
	private Session session = null;
	private LinkedList<EmailAttachment> attach = null;

	private LinkedList<FileContaints> fileContaints = null;

	private MultiPartEmail email = null;

	private SimpleEmail sEmail = null;

	public final int MAX_PRIORITY = 10;

	public final int MIN_PRIORITY = 1;

	public final int NORM_PRIORITY = 5;

	private int priority;

	public EmailClient() {
		super();
		this.rbc = ResourceBundle.getBundle(URLMaping.SERVER_CONFIG);
		this.email = new MultiPartEmail();
		this.sEmail = new SimpleEmail();
		this.email.setHostName(MailConfig.getMailSmtpHost());

		try {
			this.email.setFrom(MailConfig.getMailFrom(), "Abstractor Sender");
		} catch (EmailException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.sEmail.setHostName(MailConfig.getMailSmtpHost());
		try {
			this.sEmail.setFrom(MailConfig.getMailFrom(), "Abstractor Sender");

		} catch (EmailException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.priority = this.NORM_PRIORITY;
		this.to = new LinkedList<Name>();
		this.cc = new LinkedList<Name>();
		this.bcc = new LinkedList<Name>();
		this.attach = new LinkedList<EmailAttachment>();
		this.fileContaints = new LinkedList<FileContaints>();
	}
	
	public String getMailFrom() {
		return MailConfig.getMailFrom();
	}
	

	public void setFrom(String from) {
		try {
			this.email.setFrom(from, "");
		} catch (EmailException e) {
			e.printStackTrace();
		}
		try {
			this.sEmail.setFrom(from, "");
		} catch (EmailException e) {
			e.printStackTrace();
		}
	}

	public void setSesion(Session s) {
		this.session = s;
	}

	public Session getSession() {
		return this.session;
	}

	public void addFrom(String from, String name) {
		try {
			this.email.setFrom(from, name);
		} catch (EmailException e) {
			e.printStackTrace();
		}
		try {
			this.sEmail.setFrom(from, name);
		} catch (EmailException e) {
			e.printStackTrace();
		}
	}

	public void setHostName(String hostName) {
		this.email.setHostName(hostName);
	}

	public String getHostName() {
		return this.email.getHostName();
	}

	public boolean addMultipleAttachment(String multiplePathFile) {
		String[] files = multiplePathFile.split(",");
		for (int i = 0; i < files.length; i++) {
			addAttachment(files[i]);
		}
		return true;
	}

	public boolean addAttachmentURL(String fileName, String url) {
		if ((!"".equalsIgnoreCase(fileName)) || (!"".equalsIgnoreCase(url))) {
			EmailAttachment atac = new EmailAttachment();
			try {
				atac.setURL(new URL(url));
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			atac.setDisposition(EmailAttachment.ATTACHMENT);
			atac.setDescription(fileName);
			atac.setName(fileName);
			attach.add(atac);

		}
		return true;
	}

	public boolean addAttachmentContaints(String fileName, String containts) {
		if (!"".equalsIgnoreCase(fileName)) {
			FileContaints con = new FileContaints();
			Date data = new Date();
			if (fileName.indexOf(".") > -1) {
				con.setPathFile(this.getDirTemp() + fileName.substring(0, fileName.lastIndexOf(".") - 1) + data.getTime() + fileName.substring(fileName.lastIndexOf("."), fileName.length()));
				con.setName(fileName);
				con.setDescription("");
			} else {
				con.setPathFile(this.getDirTemp() + fileName + data.getTime());
				con.setName(fileName);
				con.setDescription("");
			}
			this.toFile(con.getPathFile(), containts);
			this.fileContaints.add(con);
		}
		return true;
	}

	public boolean addAttachmentContaints(String fileName, String description, String containts) {
		if (!"".equalsIgnoreCase(fileName)) {
			FileContaints con = new FileContaints();
			Date data = new Date();
			if (fileName.indexOf(".") > -1) {
				con.setPathFile(this.getDirTemp() + fileName.substring(0, fileName.lastIndexOf(".") - 1) + data.getTime() + fileName.substring(fileName.lastIndexOf("."), fileName.length()));
				con.setName(fileName);
				con.setDescription(description);
			} else {
				con.setPathFile(this.getDirTemp() + fileName + data.getTime());
				con.setName(fileName);
				con.setDescription(description);
			}
			this.toFile(con.getPathFile(), containts);
			this.fileContaints.add(con);
		}
		return true;
	}

	public boolean addAttachment(String pathFile) {
		if (!"".equalsIgnoreCase(pathFile)) {
			int i = 0;
			String[] multi = pathFile.split(",");
			for (i = 0; i < multi.length; i++) {
				EmailAttachment atac = new EmailAttachment();
				atac.setPath(multi[i]);
				atac.setDisposition(EmailAttachment.ATTACHMENT);
				atac.setDescription(multi[i].substring(multi[i].lastIndexOf(File.separator) + 1, multi[i].lastIndexOf(".")));
				atac.setName(multi[i].substring(multi[i].lastIndexOf(File.separator) + 1, multi[i].length()));
				attach.add(atac);
			}
		}
		return true;
	}

	public boolean addAttachment(String pathFile, String name, String description) {
		if (!"".equalsIgnoreCase(pathFile)) {
			EmailAttachment atac = new EmailAttachment();
			atac.setPath(pathFile);
			atac.setDisposition(EmailAttachment.ATTACHMENT);
			atac.setDescription(description);
			atac.setName(name);
			attach.add(atac);
		}
		return true;
	}

	public boolean addTo(String email) {
		if (email != null) {
			if (!"".equalsIgnoreCase(email)) {
				int i = 0;
				String[] multi = email.split("[, ]+");
				for (i = 0; i < multi.length; i++) {
					Name na = new Name();
					na.setEmail(multi[i]);
					na.setName("");
					to.add(na);

				}
			}
		}
		return true;
	}

	public boolean addTo(String email, String name) {
		if (!"".equalsIgnoreCase(email)) {
			Name na = new Name();
			na.setEmail(email);
			na.setName(name);
			to.add(na);
		}
		return true;
	}

	public boolean addCc(String email) {
		if (email != null) {
			if (!"".equalsIgnoreCase(email)) {
				int i = 0;
				String[] multi = email.split(",");
				for (i = 0; i < multi.length; i++) {
					Name na = new Name();
					na.setEmail(multi[i]);
					na.setName("");
					cc.add(na);
				}
			}
		}
		return true;
	}

	public boolean addCc(String email, String name) {
		if (!"".equalsIgnoreCase(email)) {
			Name na = new Name();
			na.setEmail(email);
			na.setName(name);
			cc.add(na);
		}
		return true;
	}

	public boolean addBcc(String email) {
		if (email != null) {
			if (!"".equalsIgnoreCase(email)) {
				int i = 0;
				String[] multi = email.split(",");
				for (i = 0; i < multi.length; i++) {
					Name na = new Name();
					na.setEmail(multi[i]);
					na.setName("");
					bcc.add(na);
				}
			}
		}
		return true;
	}

	public boolean addBcc(String email, String name) {
		if (!"".equalsIgnoreCase(email)) {
			Name na = new Name();
			na.setEmail(email);
			na.setName(name);
			bcc.add(na);
		}
		return true;
	}

	public void setThreadPriorty(int priority) {
		this.priority = priority;
	}

	public String sendAsynchronous() {
		Thread t = new Thread(this);
		t.setPriority(this.priority);
		if (t != null) {
			t.start();

		}
		return "";
	}

	public String sendNow() {
		run();
		return "";
	}

	public void run() {
		int i = 0;
		if ((attach.size() < 1) && (this.fileContaints.size() < 1)) {
			if (this.session != null) {
				this.sEmail.setMailSession(this.session);
			}

			for (i = 0; i < to.size(); i++) {
				try {
					this.sEmail.addTo(this.to.get(i).getEmail(), this.to.get(i).getName());
				} catch (EmailException e) {
					e.printStackTrace();
				}
			}
			for (i = 0; i < cc.size(); i++) {
				try {
					this.sEmail.addCc(this.cc.get(i).getEmail(), this.cc.get(i).getName());
				} catch (EmailException e) {
					e.printStackTrace();
				}
			}
			for (i = 0; i < bcc.size(); i++) {
				try {
					this.sEmail.addBcc(this.bcc.get(i).getEmail(), this.bcc.get(i).getName());
				} catch (EmailException e) {
					e.printStackTrace();
				}
			}

			this.sEmail.setSubject(this.getSubject());

			if (!StringUtils.isEmpty(containt)) {
				try {
					this.sEmail.setMsg(this.getContaint());
				} catch (EmailException e) {
					e.printStackTrace();
				}
			}

			try {
				this.sEmail.send();
			} catch (EmailException e1) {
				e1.printStackTrace();
			}
		}

		else {
			if (this.session != null) {
				this.email.setMailSession(this.session);
			}

			for (i = 0; i < to.size(); i++) {
				try {
					this.email.addTo(this.to.get(i).getEmail(), this.to.get(i).getName());
				} catch (EmailException e) {
					e.printStackTrace();
				}
			}

			for (i = 0; i < cc.size(); i++) {
				try {
					this.email.addCc(this.cc.get(i).getEmail(), this.cc.get(i).getName());
				} catch (EmailException e) {
					e.printStackTrace();
				}
			}

			for (i = 0; i < bcc.size(); i++) {
				try {
					this.email.addBcc(this.bcc.get(i).getEmail(), this.bcc.get(i).getName());
				} catch (EmailException e) {
					e.printStackTrace();
				}
			}

			this.email.setSubject(this.getSubject());

			try {
				this.email.setMsg(this.getContaint());
			} catch (EmailException e) {
				e.printStackTrace();
			}
			for (i = 0; i < attach.size(); i++) {
				try {
					this.email.attach(attach.get(i));
				} catch (EmailException e) {
					e.printStackTrace();
				}
			}
			for (i = 0; i < this.fileContaints.size(); i++) {
				try {
					EmailAttachment atac = new EmailAttachment();
					atac.setPath(this.fileContaints.get(i).getPathFile());
					atac.setDisposition(EmailAttachment.ATTACHMENT);
					atac.setName(this.fileContaints.get(i).getName());
					atac.setDescription(this.fileContaints.get(i).getDescription());
					this.email.attach(atac);
				} catch (EmailException e) {
					e.printStackTrace();
				}
			}

			try {
				this.email.send();

			} catch (EmailException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			for (i = 0; i < this.fileContaints.size(); i++) {
				java.io.File file = new File(this.fileContaints.get(i).getPathFile());
				java.io.FileWriter out;
				try {
					out = new FileWriter(file);
					out.close(); // THIS IS THE IMPORTANT BIT
					if (new File(this.fileContaints.get(i).getPathFile()).delete()) {
						System.out.println("File succesfully deleted!");
					} else
						System.out.println("Problems to delete file");

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public void toFile(String name, String data) {
		RandomAccessFile rand = null;
		try {
			rand = new RandomAccessFile(name, "rw");
			rand.setLength(0);
			rand.seek(0);
			rand.write(data.getBytes());
			rand.close();
		} catch (Exception v) {
			v.printStackTrace();
		} finally {
			try {
				rand.close();
			} catch (Exception e) {

			}
		}
	}

	public String getContaint() {
		if ((this.containt == null) || ("".equalsIgnoreCase(this.containt))) {
			return " ";
		} else {
			return containt;
		}
	}

	public void addContent(String content) {
		this.containt = content;
	}

	public String getSubject() {
		if ((subject == null) || ("".equalsIgnoreCase(subject))) {
			return "~ No Subject ~";
		} else {
			return subject;
		}
	}

	public void setSubject(String subject) {
		if (subject != null) {
			if (!"".equals(subject)) {
				this.subject = subject;
			} else {
				this.subject = "No Subject";
			}
		} else {
			this.subject = "No Subject";
		}
	}

	public String getDirTemp() {
		return dirTemp;
	}

	public void setDirTemp(String dirTemp) {
		this.dirTemp = dirTemp;
	}

	public void setContent(String body, String type) {
		this.sEmail.setContent(body, type);
	}
}

class Name {
	private String email = "";

	private String name = "";

	public Name(String email, String name) {
		super();
		this.email = email;
		this.name = name;
	}

	public Name() {
		super();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

}

class FileContaints {
	private String pathFile = "";

	private String name = "";

	private String description = "";

	public FileContaints(String pathFile, String name, String description) {
		super();
		this.pathFile = pathFile;
		this.name = name;
		this.description = description;
	}

	public FileContaints() {
		super();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPathFile() {
		return pathFile;
	}

	public void setPathFile(String pathFile) {
		this.pathFile = pathFile;
	}

}
