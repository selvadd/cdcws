/**
 * 
 */
package com.cdc.ws.addbidder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import wsdatamodel.BidderData;
import wsdatamodel.LeadManagerSessionData;
import wsexception.LoginException;
import wsutils.JDBCUtil;
import wsutils.LoginUtil;

import com.google.gson.Gson;

/**
 * @author Selva
 *
 */

@RestController
@RequestMapping(value = "/services")
public class AddBidderController {

	private static Logger log = Logger.getLogger(AddBidderController.class.getName());

	// get Job Type based on CDCID
	public static String getJobType(String cdcId) {

		String sql = null;
		Connection con = null;
		PreparedStatement prepStmt = null;
		ResultSet rs = null;
		String jobType = null;

		try {

			con = JDBCUtil.getDBConnectionFromDatabase();

			sql = "SELECT distinct(job_type) jobType FROM content " + " WHERE cdc_id ='" + cdcId + "'";

			prepStmt = con.prepareStatement(sql);
			rs = prepStmt.executeQuery();

			while (rs.next()) {

				jobType = rs.getString("jobType");
			}

		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (prepStmt != null) {
					prepStmt.close();
				}

				if (con != null) {
					con.close();
				}

			} catch (SQLException se) {
				log.error("!exception3.2!SQL error in getJobType" + se);

			} finally {
				try {
					if (prepStmt != null) {
						prepStmt.close();
					}
				} catch (SQLException se) {
					log.error("!exception3.3!SQL error in getJobType" + se);

				}
			}
		}
		return jobType;

	}

	// end of getJobType
	// get getSEDetails
	public static ArrayList<BidderData> getSEDetails(String officeCode, String jobType) {

		String sql = null;
		Connection con = null;
		PreparedStatement prepStmt = null;
		ResultSet rs = null;
		BidderData bdata = null;
		ArrayList<BidderData> sedetailslist = new ArrayList<BidderData>();

		try {

			con = JDBCUtil.getDBConnectionFromDatabase();

			sql =
					"SELECT oc.office_email email, oc.description, s.first_name, s.last_name, s.phone " + " FROM  office_code oc, subscriber s"
							+ " WHERE oc.office_code ='" + officeCode + "' AND oc.private_public='" + jobType + "' AND oc.office_email = s.email";

			prepStmt = con.prepareStatement(sql);
			rs = prepStmt.executeQuery();

			while (rs.next()) {
				bdata = new BidderData();
				bdata.setSeniorEditorName(rs.getString("first_name") + "" + rs.getString("last_name"));
				bdata.setSeniorEditorEmail(rs.getString("email"));
				bdata.setSeniorEditorPhone(rs.getString("phone"));
				bdata.setSeniorEditorDesc(rs.getString("description"));
				sedetailslist.add(bdata);
			}

		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (prepStmt != null) {
					prepStmt.close();
				}

				if (con != null) {
					con.close();
				}

			} catch (SQLException se) {
				log.error("!exception3.2!SQL error in getSEDetails" + se);

			} finally {
				try {
					if (prepStmt != null) {
						prepStmt.close();
					}
				} catch (SQLException se) {
					log.error("!exception3.3!SQL error in getSEDetails" + se);

				}
			}
		}
		return sedetailslist;

	}

	// end of getSEDetails

	/**
	 * composeMail - This method Composes the mail that is sent to the composeMailToEditor client.
	 *
	 * @param String
	 *            message text.
	 * @return void.
	 */
	public static void composeMailToEditor(String loginId, String cdcId, String title, String bidderName, String bidderEmail, String bidderCompany,
			String address, String city, String state, String zip, String phone, String fax, String requestType, String SEEmail) throws LoginException {

		// StringBuffer textmessage = new StringBuffer();
		try {

			/* MAIL CONNECTION AND COMPOSITION PART GOES HERE */

			// SUBSTITUTE YOUR EMAIL ADDRESSES HERE!!!
			String to = SEEmail;
			String from = "reminder@cdcnews.com";
			// SUBSTITUTE YOUR ISP'S MAIL SERVER HERE!!!

			/* The below line is commented for not using any external providers...remove if needed */
			// String host = "smtp.bizmail.yahoo.com";

			String host = "172.16.22.29";
			// Create properties, get Session
			Properties props = new Properties();

			// If using static Transport.send(),
			// need to specify which host to send it to
			props.put("mail.smtp.host", host);
			// To see what is going on behind the scene
			props.put("mail.debug", "true");

			javax.mail.Session session = javax.mail.Session.getInstance(props, null);
			// Instantiate a message
			try {

				javax.mail.internet.MimeMessage msg = new javax.mail.internet.MimeMessage(session);

				// Set message attributes
				msg.setFrom(new InternetAddress(from));

				log.info("test" + to);

				// If the to message address has a comma in it, then it must be a comma separated list of email recipients
				log.info("before token");
				StringTokenizer st = new StringTokenizer(to, ",;");
				int tokenCount = st.countTokens();
				InternetAddress[] recipientList = new InternetAddress[tokenCount];

				// Tokenize the recipient list, and create the Internet Address Array of Recipients

				for (int i = 0; st.hasMoreTokens(); i++) {
					// Get the next token
					String msgTo = st.nextToken();
					log.info("token" + msgTo);

					// Ensure the token received is a valid address
					if (msgTo != null && msgTo.trim().length() > 0) {
						// If we only have one email address then we can display the to name
						if (tokenCount == 1) {
							recipientList[i] = new InternetAddress(msgTo);
						}
						// Otherwise just display the email address as the to name.
						else {
							recipientList[i] = new InternetAddress(msgTo);
						}
					}
				}

				msg.setRecipients(javax.mail.internet.MimeMessage.RecipientType.TO, recipientList);

				msg.setSubject("Project Info Request");

				// Set message content
				msg.setText("-- Project Info Request -- \\n" + "Login ID: " + loginId + "\\n" + "CDC ID: " + cdcId + "\\n" + "Project Title: " + title + "\\n"
						+ "Contact Name: " + bidderName + "\\n" + "Email: " + bidderEmail + "\\n" + "Company Name: " + bidderCompany + "\\n" + "Address: "
						+ address + "\\n" + "City: " + city + "\\n" + "State: " + state + "\\n" + "Zip Code: " + zip + "\\n" + "Phone Number: " + phone + "\\n"
						+ "Fax Number: " + fax + "\\n" + "Request: " + requestType + "");

				// Send the message
				msg.saveChanges();

				Transport.send(msg);

			}

			catch (MessagingException mex) {
				// Prints all nested (chained) exceptions as well
				mex.printStackTrace();
			}

		} catch (Exception io) {
			log.error("Error compose mail composeMailToEditor." + io.toString());
			io.printStackTrace();
		}
	} // end of composeMailToEditor

	/**
	 * composeMail - This method Composes the mail that is sent to the composeMailToUser
	 *
	 * @param String
	 *            message text.
	 * @return void.
	 */
	public static void composeMailToUser(String title, String bidderName, String bidderEmail, String requestType, String SEName, String SEPhone,
			String SEemail, String SEDesc) throws LoginException {

		// StringBuffer textmessage = new StringBuffer();

		try {

			/* MAIL CONNECTION AND COMPOSITION PART GOES HERE */

			// SUBSTITUTE YOUR EMAIL ADDRESSES HERE!!!
			String to = bidderEmail;
			String from = "postmaster@cdcnews.com";
			// SUBSTITUTE YOUR ISP'S MAIL SERVER HERE!!!

			/* The below line is commented for not using any external providers...remove if needed */
			// String host = "smtp.bizmail.yahoo.com";

			String host = "172.16.22.29";
			// Create properties, get Session
			Properties props = new Properties();

			// If using static Transport.send(),
			// need to specify which host to send it to
			props.put("mail.smtp.host", host);
			// To see what is going on behind the scene
			props.put("mail.debug", "true");

			javax.mail.Session session = javax.mail.Session.getInstance(props, null);
			// Instantiate a message
			try {

				javax.mail.internet.MimeMessage msg = new javax.mail.internet.MimeMessage(session);

				// Set message attributes
				msg.setFrom(new InternetAddress(from));

				log.info("test" + to);

				// If the to message address has a comma in it, then it must be a comma separated list of email recipients
				log.info("before token");
				StringTokenizer st = new StringTokenizer(to, ",;");
				int tokenCount = st.countTokens();
				InternetAddress[] recipientList = new InternetAddress[tokenCount];

				// Tokenize the recipient list, and create the Internet Address Array of Recipients

				for (int i = 0; st.hasMoreTokens(); i++) {
					// Get the next token
					String msgTo = st.nextToken();
					log.info("token" + msgTo);

					// Ensure the token received is a valid address
					if (msgTo != null && msgTo.trim().length() > 0) {
						// If we only have one email address then we can display the to name
						if (tokenCount == 1) {
							recipientList[i] = new InternetAddress(msgTo);
						}
						// Otherwise just display the email address as the to name.
						else {
							recipientList[i] = new InternetAddress(msgTo);
						}
					}
				}

				msg.setRecipients(javax.mail.internet.MimeMessage.RecipientType.TO, recipientList);

				msg.setSubject("Project Info Request");

				// Set message content
				msg.setText("Dear " + bidderName + ", \n\n " + "Your request for " + requestType + " on Project " + title + " has been sent to " + SEName
						+ ", \n" + "of the " + SEDesc + " editorial office. \n" + "If you need any additional information you may contact " + SEPhone + " or "
						+ SEemail + ". \n\n" + "Thank you and have a good day! \n CDC News");

				// Send the message
				msg.saveChanges();

				Transport.send(msg);

			}

			catch (MessagingException mex) {
				// Prints all nested (chained) exceptions as well
				mex.printStackTrace();
			}

		} catch (Exception io) {
			log.error("Error compose mail .composeMailToUser" + io.toString());
			io.printStackTrace();
		}
	} // composeMailToUser

	// getOfficeCode based on CDCID
	public static String getOfficeCode(String cdcId) {

		String officeCode = null;
		if (cdcId != null) {

			// int len = cdcId.length();
			officeCode = cdcId.substring(0, 2);
			// check for PA1 CDCID
			if (cdcId.substring(0, 3).equals("PA1")) {
				officeCode = "PA1";
			}
			// check for PA2 CDCID
			else if (cdcId.substring(0, 3).equals("PA2")) {
				officeCode = "PA2";
			}

		}
		return officeCode;
	}

	// removeJobTypeHypen if contains
	public static String removeJobTypewithHypen(String jobType) {

		String newJobType = jobType;
		try {

			if (jobType != null) {

				int len = jobType.indexOf("-");
				if (len > 0) {
					newJobType = jobType.substring(0, len);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error in removeJobTypeHypen " + e.toString());
		}
		return newJobType;
	}

	// sendMail
	public static boolean sendMail(int sessionId, String title, String bidderName, String bidderEmail, String requestType, String cdcId, String bidderCompany,
			String address, String city, String state, String zip, String phone, String fax) throws LoginException {

		String officeCode = getOfficeCode(cdcId);
		String jobType = getJobType(cdcId);
		ArrayList<BidderData> seList = getSEDetails(officeCode, jobType);
		String SEName = null;
		String SEEmail = null;
		String SEPhone = null;
		String SEDesc = null;
		boolean mailSent = false;
		Connection con = null;

		try {
			if (con == null || con.isClosed()) {
				con = JDBCUtil.getDBConnectionFromDatabase();
			}
			BidderData bData = null;
			Iterator<BidderData> itr = seList.iterator();

			while (itr.hasNext()) {

				bData = (BidderData) itr.next();
				SEName = bData.getSeniorEditorName();
				SEEmail = bData.getSeniorEditorEmail();
				SEPhone = bData.getSeniorEditorPhone();
				SEDesc = bData.getSeniorEditorDesc();
				log.info(bData.getSeniorEditorName());
			}
			LeadManagerSessionData lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);
			composeMailToUser(title, bidderName, bidderEmail, requestType, SEName, SEPhone, SEEmail, SEDesc);
			composeMailToEditor(lmData.getLogin(), cdcId, title, bidderName, bidderEmail, bidderCompany, address, city, state, zip, phone, fax, requestType,
					SEEmail);
			mailSent = true;

		} catch (Exception e) {
			mailSent = false;
			e.printStackTrace();
			log.error("Error in sendMail method" + e.toString());
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}
		return mailSent;
	}

	// end of sendMail

	// deleteSubUser
	@RequestMapping(value = "/bidderRequestMail")
	public String bidderRequestMail(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey,
			@RequestParam("title") String title, @RequestParam("bidderName") String bidderName, @RequestParam("bidderEmail") String bidderEmail,
			@RequestParam("requestType") String requestType, @RequestParam("cdcId") String cdcId, @RequestParam("bidderCompany") String bidderCompany,
			@RequestParam("address") String address, @RequestParam("city") String city, @RequestParam("state") String state, @RequestParam("zip") String zip,
			@RequestParam("phone") String phone, @RequestParam("fax") String fax) {

		Map<String, Object> map = new HashMap<String, Object>();
		Gson gson = new Gson();

		boolean validSessionId = false;
		boolean mailSentFlag = false;
		// LeadManagerSessionData lmData = null;
		Connection con = null;
		try {

			if (con == null || con.isClosed()) {
				con = JDBCUtil.getDBConnectionFromDatabase();
			}
			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			if (validSessionId == true) {

				mailSentFlag = sendMail(sessionId, title, bidderName, bidderEmail, requestType, cdcId, bidderCompany, address, city, state, zip, phone, fax);

				if (mailSentFlag == true) {

					map.put("Status", "Success");
					map.put("Message", "Mail Sent Successfully");
				} else {
					map.put("Status", "Failure");
					map.put("Message", "Unable to send email.");
				}

			} // end of if for validsession check

			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

			// log.info("deleteSubUser : " +Map.toString());
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}
		return gson.toJson(map);
	}

}
