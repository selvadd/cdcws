/**
 * 
 */
package com.cdc.ws.ecommerce;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import wsdatamodel.EcommData;
import wsdatamodel.RatesData;
import wsexception.LoginException;
import wsutils.EcommUtil;
import wsutils.JDBCUtil;
import wsutils.LibraryFunctions;
import wsutils.SecretEncryptDecrypt;

/**
 * @author Selva
 *
 */

public class PLEcommerceRenewal {

	private static Logger log = Logger.getLogger(PLEcommerceRenewal.class.getName());

	// Get getMaxInvoiceId-clc-invoice table
	public static String getInvoiceId() throws LoginException {

		CallableStatement cstmt = null;
		ResultSet rs = null;
		Connection con = null;
		String invoiceId = null;
		// String sql = null;

		try {

			if (con == null) {
				con = JDBCUtil.getDBConnectionFromDatabase();
			}
			cstmt = con.prepareCall("{call ssp_clc_invoiceid()}");

			rs = cstmt.executeQuery();
			while (rs.next()) {
				invoiceId = String.valueOf(rs.getInt("invoiceid"));

			}

		} catch (Exception e) {
			log.error("Error in getInvoiceId" + e.toString());
			e.printStackTrace();

		} finally {

			try {
				if (cstmt != null) {
					cstmt.close();
				}
				if (con != null) {
					con.close();
				}

			} catch (SQLException sqle) {
				// sqle.printStackTrace();
				log.error("SQLError in getInvoiceId" + sqle.toString());

			}

		}

		return invoiceId;
	} // End of getInvoiceId()

	// MAIL ATTACHMENT
	public static void mailAttachment(String filename, String dateformat) {
		String from = "clcinvoice@constructionleads.com";
		String to = "invoice@cdcnews.com,TCummings@cdcnews.com,gcolangelo@cdcnews.com,subhash@creatus.com,johnson@tentsoftware.com,sathya@tentsoftware.com";
		String subject = "CLC Auto Renewal Information Report - " + dateformat;
		String bodyText = "CLC Auto Renewal Information Report";

		Properties properties = new Properties();

		properties.put("mail.smtp.host", "192.168.22.68");
		properties.put("mail.smtp.port", "25");
		Session session = Session.getDefaultInstance(properties, null);

		try {
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));

			StringTokenizer st = new StringTokenizer(to, ",;");
			int tokenCount = st.countTokens();
			InternetAddress[] recipientList = new InternetAddress[tokenCount];

			// Tokenize the recipient list, and create the Internet Address Array of Recipients

			for (int i = 0; st.hasMoreTokens(); i++) {
				// Get the next token
				String msgTo = st.nextToken();

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

			message.setRecipients(javax.mail.internet.MimeMessage.RecipientType.TO, recipientList);

			// message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
			message.setSubject(subject);
			message.setSentDate(new Date());

			//
			// Set the email message text.
			//
			MimeBodyPart messagePart = new MimeBodyPart();
			messagePart.setText(bodyText);

			//
			// Set the email attachment file
			//
			MimeBodyPart attachmentPart = new MimeBodyPart();
			FileDataSource fileDataSource = new FileDataSource(filename);
			attachmentPart.setDataHandler(new DataHandler(fileDataSource));
			attachmentPart.setFileName(filename);

			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messagePart);
			multipart.addBodyPart(attachmentPart);

			message.setContent(multipart);

			Transport.send(message);
		} catch (MessagingException e) {
			e.printStackTrace();
		}

	}

	// USER RENEWAL FOR PL ECOMMERCE
	// GetSubscriptionDetailsForRenewal

	public static ArrayList<EcommData> getSubscriptionDetailsForRenewal() throws LoginException {

		CallableStatement cstmt = null;
		ResultSet rs = null;
		Connection con = null;
		// String states = null;
		// String sql = null;
		EcommData ecommData = null;
		ArrayList<EcommData> ecommInfoList = new ArrayList<EcommData>();
		String decryptCCnumber = null;
		String encryptCCDisplay = null;

		try {

			if (con == null) {
				con = JDBCUtil.getDBConnectionFromDatabase();
			}
			cstmt = con.prepareCall("{call SSP_PL_DETAILS_SUBSCRIPTION_RENEWAL()}");

			rs = cstmt.executeQuery();

			// ArrayList stateData;
			while (rs.next()) {
				ecommData = new EcommData();
				ecommData.setLoginId(rs.getString("login_id"));
				log.info("Login Id " + ecommData.getLoginId());
				ecommData.setRateId(rs.getString("rate_id"));
				ecommData.setCompanyName(rs.getString("company_name"));
				ecommData.setFirstName(rs.getString("first_name"));
				ecommData.setLastName(rs.getString("last_name"));
				ecommData.setAddress1(rs.getString("address1"));
				ecommData.setStateName(rs.getString("state"));
				ecommData.setCity(rs.getString("city"));
				ecommData.setZip(rs.getString("zip"));
				ecommData.setFax(rs.getString("fax"));
				ecommData.setEmail(rs.getString("email"));
				ecommData.setTelephone(rs.getString("phone"));
				ecommData.setCCType(rs.getString("cc_type"));
				ecommData.setCCName(rs.getString("name_on_credit_Card"));
				// Decrypting the ccnumber to regular decrypt way
				decryptCCnumber = SecretEncryptDecrypt.decrypt(rs.getString("credit_card_number"));
				// Encrypting ccnumber for front end
				encryptCCDisplay = EcommUtil.encryptCc(decryptCCnumber);
				ecommData.setCCNumber(encryptCCDisplay);
				ecommData.setCCAddress(rs.getString("credit_card_Address"));
				ecommData.setCCExpiryDate(rs.getString("credit_card_expiry_date"));
				ecommData.setBillingCompanyName(rs.getString("bill_company_name"));
				ecommData.setBillingAddress(rs.getString("bill_company_address"));
				ecommData.setBillingCity(rs.getString("billing_city"));
				ecommData.setBillingStateName(rs.getString("billing_state"));
				ecommData.setBillingZip(rs.getString("billing_zip"));
				ecommData.setShippingCompanyName(rs.getString("ship_company_name"));
				ecommData.setShippingAddress(rs.getString("ship_company_address"));
				ecommData.setShippingCity(rs.getString("shipping_city"));
				ecommData.setShippingStateName(rs.getString("shipping_state"));
				ecommData.setShippingZip(rs.getString("shipping_zip"));

				ecommInfoList.add(ecommData);

			}

		} catch (Exception e) {
			log.error("ERROR IN GetSubscriptionDetailsForRenewal" + e.toString());
			e.printStackTrace();

		} finally {

			try {
				if (cstmt != null) {
					cstmt.close();
				}
				if (con != null) {
					con.close();
				}

			} catch (SQLException sqle) {
				log.error("SQL ERROR IN GetSubscriptionDetailsForRenewal" + sqle.toString());
				sqle.printStackTrace();

			}

		}

		return ecommInfoList;

	} // End of GetSubscriptionDetailsForRenewal()

	// renewUser
	// 1.This function extends/renew the subscription for the user
	// 2.once renewed,the login and rate details in inserted into temp table(pl_renewal_temptable) for sending reports to invoice
	public boolean renewUser(String loginId, String rateIds, String salesTax, String total) throws LoginException {

		CallableStatement cstmt = null;
		// ResultSet rs = null;
		Connection con = null;
		boolean insertFlag = false;
		// String sql = null;

		try {

			if (con == null) {
				con = JDBCUtil.getDBConnectionFromDatabase();
			}
			cstmt = con.prepareCall("{call SSP_PL_RENEW_SUBSCRIPTIONS(?,?,?,?)}");

			// loginId
			cstmt.setString(1, loginId);
			// rateId
			cstmt.setString(2, rateIds);
			// salesTax
			cstmt.setString(3, salesTax);
			// Total
			cstmt.setString(4, total);

			cstmt.executeUpdate();
			insertFlag = true;

		} catch (Exception e) {
			log.error("ERROR IN renewUser " + e.toString());
			insertFlag = false;

			e.printStackTrace();

		} finally {

			try {
				if (cstmt != null) {
					cstmt.close();
				}
				if (con != null) {
					con.close();
				}

			} catch (SQLException sqle) {
				log.error("SQLException IN renewUser " + sqle.toString());
				sqle.printStackTrace();
				insertFlag = false;

			}

		}

		return insertFlag;
	} // End of renewUser()

	/**
	 * composeMail - This method Composes the mail that is sent to the Ecommerce client purchase
	 *
	 * @param String
	 *            message text.
	 * @return void.
	 */
	public static void composeMail(String txtmessage, String msgSubject, String mailTo) {

		// StringBuffer textmessage = new StringBuffer();

		// Write to Text file.
		try {

			/* MAIL CONNECTION AND COMPOSITION PART GOES HERE */

			// SUBSTITUTE YOUR EMAIL ADDRESSES HERE!!!
			String to = mailTo;
			// String to = "guitar_johnson@yahoo.co.in,jude@tentsoftware.com,johnson@tentsoftware.com";
			String from = "customercare@constructionleads.com";
			// SUBSTITUTE YOUR ISP'S MAIL SERVER HERE!!!

			String invoiceEmailId =
					"invoice@cdcnews.com,TCummings@cdcnews.com,gcolangelo@cdcnews.com,subhash@creatus.com,johnson@tentsoftware.com,sathya@tentsoftware.com";

			// String cdcNewsUrl = "http://www.cdcnews.com";

			/* The below line is commented for not using any external providers...remove if needed */
			// String host = "smtp.bizmail.yahoo.com";

			String host = "172.16.22.29";
			// String host = "wdh0189-new";
			// Create properties, get Session
			Properties props = new Properties();

			// If using static Transport.send(),
			// need to specify which host to send it to
			props.put("mail.smtp.host", host);
			// To see what is going on behind the scene
			props.put("mail.debug", "true");

			javax.mail.Session session = javax.mail.Session.getInstance(props, null);
			// Instantiatee a message

			javax.mail.internet.MimeMessage msg = new javax.mail.internet.MimeMessage(session);

			// Set message attributes
			msg.setFrom(new InternetAddress(from));

			// If the to message address has a comma in it, then it must be a comma separated list of email recipients

			StringTokenizer st = new StringTokenizer(to, ",;");
			int tokenCount = st.countTokens();
			InternetAddress[] recipientList = new InternetAddress[tokenCount];

			// Tokenize the recipient list, and create the Internet Address Array of Recipients

			for (int i = 0; st.hasMoreTokens(); i++) {
				// Get the next token
				String msgTo = st.nextToken();

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

			StringTokenizer bccList = new StringTokenizer(invoiceEmailId, ",;");
			int tokenCountBcc = bccList.countTokens();
			InternetAddress[] recipientListBcc = new InternetAddress[tokenCountBcc];

			// Tokenize the recipient list, and create the Internet Address Array of Recipients

			for (int i = 0; bccList.hasMoreTokens(); i++) {
				// Get the next token
				String msgToBcc = bccList.nextToken();

				// Ensure the token received is a valid address
				if (msgToBcc != null && msgToBcc.trim().length() > 0) {
					// If we only have one email address then we can display the to name
					if (tokenCountBcc == 1) {
						recipientListBcc[i] = new InternetAddress(msgToBcc);
					}
					// Otherwise just display the email address as the to name.
					else {
						recipientListBcc[i] = new InternetAddress(msgToBcc);
					}
				}
			}

			msg.setRecipients(javax.mail.internet.MimeMessage.RecipientType.BCC, recipientListBcc);

			// message subject
			msg.setSubject(msgSubject);
			msg.setSentDate(new Date());

			log.info(txtmessage + " mail message");

			// mail content-txtmessage
			// msg.setText(txtmessage);
			msg.setContent(txtmessage, "text/html");
			// Send the message
			msg.saveChanges();

			Transport.send(msg);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method removes any $ signs and commas (,) from the given string and returns the filtered string.
	 * 
	 * @param String
	 *            str containing amount
	 * @return String amount, with $ and , filtered
	 * @exception null
	 */
	public static String filterAmountField(String str) {
		char[] amtchars = str.toCharArray();
		char[] newchars = new char[amtchars.length];
		String filteredString = null;

		int j = 0;
		for (int i = 0; i < amtchars.length; i++) {
			if ((amtchars[i] != '$') && (amtchars[i] != ',')) {
				newchars[j] = amtchars[i];
				j++;
			}
		}

		if (newchars.length > 0) {
			filteredString = (new String(newchars)).trim();
		}
		return filteredString;

	}

	/**
	 * This method formats given string to US amount
	 * 
	 * @param String
	 *            amount
	 * @return String formatted_amount
	 * @exception null
	 */
	public static String formatAmountStringWithDecimal(String s, int min_decimal_places, int max_decimal_places) {
		// First, filter any commas, any dollar signs etc.
		String amtString = filterAmountField(s);
		String formattedString = null;
		if (s != null) {
			try {
				// Convert the string to double
				double amt = Double.parseDouble(amtString);
				// Create US Number formatter
				Locale usLocale = new Locale("EN", "us");
				NumberFormat usNumberFormatter = NumberFormat.getInstance(usLocale);
				usNumberFormatter.setMinimumFractionDigits(min_decimal_places);
				usNumberFormatter.setMaximumFractionDigits(max_decimal_places);

				formattedString = usNumberFormatter.format(amt);

			} catch (Exception e) {
				// If anything goes wrong, return null string.
				return null;
			}
		}

		return formattedString;

	}

	/**
	 * This method formats given string to US amount
	 * 
	 * @param String
	 *            amount
	 * @return String formatted_amount
	 * @exception null
	 */
	public static String formatAmountString(String s) {
		// First, filter any commas, any dollar signs etc.
		String amtString = filterAmountField(s);
		String formattedString = null;
		if (s != null) {
			try {
				// Convert the string to double
				double amt = Double.parseDouble(amtString);
				// Create US Number formatter
				Locale usLocale = new Locale("EN", "us");
				NumberFormat usNumberFormatter = NumberFormat.getInstance(usLocale);
				formattedString = usNumberFormatter.format(amt);

			} catch (Exception e) {
				// If anything goes wrong, return null string.
				return null;
			}
		}

		return formattedString;

	}

	// Get getRatesDetails Per Rateids
	public static ArrayList<RatesData> getRatesDetails(String rateIds) throws LoginException {

		CallableStatement cstmt = null;
		ResultSet rs = null;
		Connection con = null;
		// String states = null;
		// String sql = null;
		RatesData rData = null;
		ArrayList<RatesData> ratesList = new ArrayList<RatesData>();

		try {

			if (con == null) {
				con = JDBCUtil.getDBConnectionFromDatabase();
			}
			cstmt = con.prepareCall("{call SSP_PL_PRODUCTS_DETAILS_FOR_RATES(?)}");
			cstmt.setString(1, rateIds);

			rs = cstmt.executeQuery();

			// ArrayList stateData;
			while (rs.next()) {
				rData = new RatesData();
				rData.setRateId(rs.getInt("rate_id"));
				rData.setStateId(rs.getInt("state_id"));
				rData.setTerm(rs.getString("term"));
				rData.setPrice(rs.getFloat("price"));
				rData.setStartDate(rs.getString("start_date"));
				rData.setStartDate(rs.getString("end_date"));
				rData.setEnableFlag(rs.getString("enable_flag"));
				rData.setStateName(rs.getString("state_name"));
				ratesList.add(rData);

			}

		} catch (Exception e) {
			log.error("ERROR IN getRatesDetails Per Rateids" + e.toString());
			e.printStackTrace();

		} finally {

			try {
				if (cstmt != null) {
					cstmt.close();
				}
				if (con != null) {
					con.close();
				}

			} catch (SQLException sqle) {
				log.error("SQL ERROR IN getRatesDetails Per Rateids" + sqle.toString());
				sqle.printStackTrace();

			}

		}

		return ratesList;

	} // End of getRatesDetails Per Rateids()

	public static String getRowforStatesSubscribed(String rateIdList, String salesTax, String totalPrice) throws LoginException {

		String rows = null;

		ArrayList<RatesData> ratesPurchasedList = getRatesDetails(rateIdList);
		log.info("getRowforStatesSubscribed " + ratesPurchasedList.size());
		Iterator<RatesData> ratesItr = ratesPurchasedList.iterator();
		while (ratesItr.hasNext()) {
			RatesData rData = (RatesData) ratesItr.next();

			// PRODUCTS PURCHASED AND ITS DETAILS FROM RATES_PL TABLE AS PER RATEIDS PASSED
			if (rows == null) {
				rows =
						"The ConstructionLeads.com Product(s) subscribed:" + "<table cellspacing=0 cellpadding=0 border=1>" + "<tbody>" + "<tr>"
								+ "<td width=300><font color=#0000ff><b>" + rData.getStateName() + "&nbsp;</b></font> </td>"
								+ "<td width=75><font color=black><b>Online&nbsp;&nbsp;</b></font> </td>" + "<td width=75><font color=black><b>"
								+ rData.getTerm() + "&nbsp;&nbsp;</b></font> </td>" + "<td width=100><font color=black><b>$"
								+ formatAmountStringWithDecimal(String.valueOf(rData.getPrice()), 2, 2) + "</b></font> </td>" + "</tr>" + "</tbody>"
								+ "</table>";

			} else {
				rows =
						rows + "<table cellspacing=0 cellpadding=0 border=1>" + "<tbody>" + "<tr>" + "<td width=300><font color=#0000ff><b>"
								+ rData.getStateName() + "&nbsp;</b></font> </td>" + "<td width=75><font color=black><b>Online&nbsp;&nbsp;</b></font> </td>"
								+ "<td width=75><font color=black><b>" + rData.getTerm() + "&nbsp;&nbsp;</b></font> </td>"
								+ "<td width=100><font color=black><b>$" + formatAmountStringWithDecimal(String.valueOf(rData.getPrice()), 2, 2)
								+ "</b></font> </td>" + "</tr>" + "</tbody>" + "</table>";

			}

		}
		// SALES AND TOTAL PRICE DETAILS
		rows =
				rows + "<br>" + "<table cellspacing=0 cellpadding=0 border=1>" + "<tbody>" + "<tr>" + "<td width=75><font color=black>Sales Tax:</font> </td>"
						+ "<td width=100><font color=black><b>$" + formatAmountStringWithDecimal(salesTax, 2, 2) + "</b></font> </td>" + "</tr>" + "<tr>"
						+ "<td width=75><font color=black>Total:</font> </td>" + "<td width=100><font color=black><b>$"
						+ formatAmountStringWithDecimal(totalPrice, 2, 2) + "</b> </font> </td>" + "</tr>" + "</tbody>" + "</table>";

		log.info("ROWS " + rows);

		return rows;

	}

	// mailContentUser-mail content to user when subscribes to state_id
	public String mailContentUser(String fname, String lname, String companyName, String addr1, String city, String state, String zip, String phone,
			String fax, String email, String ccType, String nameOnCC, String ccNumber, String ccAddress, String stateSubscribed, String amountCharged,
			String transactionId, String salesTax, String rateIds, String billingcompanyName, String billingaddr, String billingcity, String billingstate,
			String billingzip, String shippingCompanyName, String shippingAddress, String shippingCity, String shippingState, String shippingZip) {

		String mailContent = null;

		try {

			mailContent =
					"Dear " + fname + "&nbsp;" + lname + ",<br><br> Your subscription to ConstructionLeads.com has auto-renewed for another month.<br>"
							+ " You can access your Constructionleads.com subscription by going to our website"
							+ " <a href=http://www.Constructionleads.com target=_blank > www.Constructionleads.com</a> and logging in.<br><br>"
							+ "The following is your access information. Please keep this in a safe place.<br>"
							+ " <table cellspacing=0 cellpadding=0 border=1>" + " <tbody>" + "  <tr>"
							+ "   <td colspan=2>User ID for ConstructionLeads.com </td>" + "</tr>" + "<tr>" + " <td> Login Id: <b>" + email + "</b> </td>" +

							"</tr>" + "<tr>" + " <td>&nbsp;Order Confirmation #:<b>" + transactionId + "</b> </td>" + "<td>&nbsp;Dated:<b>"
							+ LibraryFunctions.getTodayDate() + "</b> </td>" + "</tr>" + "</tbody>" + "</table>" + " <br>" + ""
							+ getRowforStatesSubscribed(rateIds, salesTax, amountCharged) + "" + "<br>" + "<b>Subscriber Details:</b>"
							+ " <table cellspacing=0 cellpadding=0 border=1>" + "<tbody>" + " <tr>" + " <td width=200><b>Contact Info</b></td>"
							+ "<td width=200><b>Shipping Info</b></td>" + "  </tr>" + "  <tr>" + " <td>" + fname + "&nbsp;" + lname + "</td>"
							+ "  <td>&nbsp;</td>" + " </tr>" + " <tr>" + " <td> " + companyName + "</td>" + "  <td> " + shippingCompanyName + "</td>"
							+ " </tr>" + " <tr>" + "  <td> " + addr1 + "</td>" + "  <td> " + shippingAddress + "</td>" + " </tr>" + " <tr>" + " <td></td>"
							+ " <td></td>" + "</tr>" + " <tr>" + "  <td> " + city + " </td>" + " <td>  " + shippingCity + "</td>" + " </tr>" + "  <tr>"
							+ "  <td> " + state + " </td>" + "  <td> " + shippingState + "</td>" + "</tr>" + " <tr>" + " <td> " + zip + " </td>" + "<td> "
							+ shippingZip + "</td>" + "</tr>" + "<tr>" + "  <td>" + phone + "(T) </td>" + " <td>&nbsp;</td>" + "</tr>" + " <tr>" + "<td>" + fax
							+ "(F)</td>" + " <td>&nbsp;</td>" + " </tr>" + " <tr>" + "   <td>" + email + " </td>" + " <td>&nbsp;</td>" + "  </tr>" + " <tr>"
							+ "<td>&nbsp;</td>" + " <td>&nbsp;</td>" + "</tr>" + " <tr>" + "  <td><b>Billing Info</b></td>"
							+ "   <td><b>Credit Card Info</b></td>" + " </tr>" + "<tr>" + " <td>" + billingcompanyName + "</td>" + "  <td>" + ccType + "</td>"
							+ " </tr>" + "<tr>" + "  <td>" + billingaddr + "</td>" + " <td>****" + ccNumber.substring(12, ccNumber.length()) + "</td>"
							+ "</tr>" + "<tr>" + "  <td></td>" + "  <td>&nbsp;</td>" + "</tr>" + "<tr>" + "  <td>" + billingcity + "</td>"
							+ "  <td>&nbsp;</td>" + " </tr>" + " <tr>" + "<td>" + billingstate + "</td>" + " <td>&nbsp;</td>" + "</tr>" + "<tr>" + " <td>"
							+ billingzip + "</td>" + "<td>&nbsp;</td>" + "</tr>" + "</tbody>" + "</table>" + "<br>" + "<b>-Sales-ConstructionLeads.com</b>";

			log.info(mailContent + " mail content");

		} catch (Exception e) {
			e.printStackTrace();
			log.error("ERROR IN mailContentUser " + e.toString());
		}

		return mailContent;
	}

	// mailContentUser-mail content to user when subscribes to state_id
	public String mailContentInvoice(String fname, String lname, String companyName, String addr1, String addr2, String city, String state, String zip,
			String phone, String fax, String email, String ccType, String nameOnCC, String ccNumber, String ccAddress, String stateSubscribed,
			String amountCharged, String transactionId, String salesTax, String rateIds, String billingcompanyName, String billingaddr, String billingcity,
			String billingstate, String billingzip, String shippingCompanyName, String shippingAddress, String shippingCity, String shippingState,
			String shippingZip) {

		String mailContent = null;
		try {

			mailContent =
					"The access information for the Subscriber <b>" + fname + "&nbsp;" + lname + ":</b><br>" + " <table cellspacing=0 cellpadding=0 border=1>"
							+ " <tbody>" + "  <tr>" + "   <td colspan=2>User Id for ConstructionLeads.com </td>" + "</tr>" + "<tr>" + " <td> Login Id: <b>"
							+ email + "</b> </td>" +

							"</tr>" + "<tr>" + " <td>&nbsp;Order Confirmation #:<b>" + transactionId + "</b> </td>" + "<td>&nbsp;Dated:<b>"
							+ LibraryFunctions.getTodayDate() + "</b> </td>" + "</tr>" + "</tbody>" + "</table>" + " <br>" + ""
							+ getRowforStatesSubscribed(rateIds, salesTax, amountCharged) + "" + "<b>Subscriber Details:</b>"
							+ " <table cellspacing=0 cellpadding=0 border=1>" + "<tbody>" + " <tr>" + " <td width=200><b>Contact Info</b></td>"
							+ "<td width=200><b>Shipping Info</b></td>" + "  </tr>" + "  <tr>" + " <td>" + fname + "&nbsp;" + lname + "</td>"
							+ "  <td>&nbsp;</td>" + " </tr>" + " <tr>" + " <td>" + companyName + "</td>" + "  <td>" + shippingCompanyName + "</td>" + " </tr>"
							+ " <tr>" + "  <td>" + addr1 + "</td>" + "  <td>" + shippingAddress + "</td>" + " </tr>" + " <tr>" + " <td></td>" + " <td></td>"
							+ "</tr>" + " <tr>" + "  <td> " + city + " </td>" + " <td>  " + shippingCity + "</td>" + " </tr>" + "  <tr>" + "  <td>" + state
							+ " </td>" + "  <td>" + shippingState + "</td>" + "</tr>" + " <tr>" + " <td>" + zip + " </td>" + "<td>" + shippingZip + "</td>"
							+ "</tr>" + "<tr>" + "  <td>" + phone + "(T) </td>" + " <td>&nbsp;</td>" + "</tr>" + " <tr>" + "<td>" + fax + "(F)</td>"
							+ " <td>&nbsp;</td>" + " </tr>" + " <tr>" + "   <td>" + email + " </td>" + " <td>&nbsp;</td>" + "  </tr>" + " <tr>"
							+ "<td>&nbsp;</td>" + " <td>&nbsp;</td>" + "</tr>" + " <tr>" + "  <td><b>Billing Info</b></td>"
							+ "   <td><b>Credit Card Info</b></td>" + " </tr>" + "<tr>" + " <td>" + billingcompanyName + "</td>" + "  <td>" + ccType + "</td>"
							+ " </tr>" + "<tr>" + "  <td>" + billingaddr + "</td>" + " <td>****" + ccNumber.substring(12, ccNumber.length()) + "</td>"
							+ "</tr>" + "<tr>" + "  <td></td>" + "  <td>&nbsp;</td>" + "</tr>" + "<tr>" + "  <td>" + billingcity + "</td>"
							+ "  <td>&nbsp;</td>" + " </tr>" + " <tr>" + "<td>" + billingstate + "</td>" + " <td>&nbsp;</td>" + "</tr>" + "<tr>" + " <td>"
							+ billingzip + "</td>" + "<td>&nbsp;</td>" + "</tr>" + "</tbody>" + "</table>" + "<br>" + "<b>-Sales-ConstructionLeads.com</b>";

		} catch (Exception e) {
			e.printStackTrace();
			log.error("ERROR IN mailContentInvoice " + e.toString());
		}

		return mailContent;
	}

	public String autoRenewSchedulePL() throws LoginException {

		String apiResponse = null;
		String mailContentSubscriber = null;
		String mailContentInvoice = null;
		float amountCharged = 0;
		float salesTax = 0;
		String statesSubscribed = null;
		String transactionNumber = null;
		String invoiceEmailId = "johnson@tentsoftware.com";
		ArrayList<EcommData> ecommAutoRenewList = getSubscriptionDetailsForRenewal();
		Iterator<EcommData> ecommItr = ecommAutoRenewList.iterator();
		String rateIds = null;
		String ccLastDigits = null;
		String invoiceId = null;
		int headerTitleCounter = 0;
		EcommData ecommData = null;

		try {

			PropertyConfigurator.configure("D:/CLCLog4j/log4j.properties");

			while (ecommItr.hasNext()) {
				ecommData = (EcommData) ecommItr.next();

				// TAX CALCULATION GOES HERE
				// FOR SALES TAX
				rateIds = String.valueOf(ecommData.getRateId());

				salesTax = EcommUtil.getTotalB4Tax(rateIds) * EcommUtil.getSalesTax(ecommData.getCity(), ecommData.getStateName(), ecommData.getZip());

				log.info("salesTax " + salesTax);
				amountCharged = EcommUtil.getTotalB4Tax(rateIds) + (salesTax);
				log.info("amountCharged " + amountCharged);
				String ccNumber = ecommData.getCCNumber();
				ccLastDigits = ccNumber.substring(10, 14);
				log.info("Decrypted CC NUMBER " + ccLastDigits.length() + ccLastDigits);
				// invoice id
				invoiceId = getInvoiceId();

				/*
				 * URL post_url = new URL( "https://test.authorize.net/gateway/transact.dll"); Hashtable post_values = new Hashtable(); // the API Login ID and
				 * Transaction Key must be replaced with valid values post_values.put("x_login", "3E2eayC4XgN"); post_values.put("x_tran_key",
				 * "2dy8NRK56h8kK2e5");
				 */
				URL post_url = new URL("https://secure.authorize.net/gateway/transact.dll");
				Hashtable<String, String> post_values = new Hashtable<String, String>();
				// the API Login ID and Transaction Key must be replaced with valid values
				post_values.put("x_login", "6aMj6M3f");
				post_values.put("x_tran_key", "63T6K5fq3Qr582j9");
				post_values.put("x_version", "3.1");
				post_values.put("x_delim_data", "TRUE");
				post_values.put("x_delim_char", "|");
				post_values.put("x_relay_response", "FALSE");
				post_values.put("x_type", "AUTH_CAPTURE");
				post_values.put("x_method", "CC");
				post_values.put("x_card_num", EcommUtil.decryptCc(ecommData.getCCNumber()));
				post_values.put("x_exp_date", ecommData.getCCExpiryDate());
				post_values.put("x_amount", String.valueOf(amountCharged));
				post_values.put("x_description", "CLC Transaction");
				post_values.put("x_first_name", ecommData.getFirstName());
				post_values.put("x_last_name", ecommData.getLastName());
				post_values.put("x_address", ecommData.getCCAddress());
				post_values.put("x_state", ecommData.getStateName());
				post_values.put("x_zip", ecommData.getZip());
				post_values.put("x_invoice_num", "CLC" + "-" + invoiceId + "-" + LibraryFunctions.getTodayDate());
				// Additional fields can be added here as outlined in the AIM integration
				// guide at: http://developer.authorize.net
				// This section takes the input fields and converts them to the proper format
				// for an http post. For example: "x_login=username&x_tran_key=a1B2c3D4"

				StringBuffer post_string = new StringBuffer();
				Enumeration<String> keys = post_values.keys();

				while (keys.hasMoreElements()) {
					String key = URLEncoder.encode(keys.nextElement().toString(), "UTF-8");
					String value = URLEncoder.encode(post_values.get(key).toString(), "UTF-8");
					post_string.append(key + "=" + value + "&");

				}
				// Open a URLConnection to the specified post url
				URLConnection connection = post_url.openConnection();

				connection.setDoOutput(true);
				connection.setUseCaches(false);
				// this line is not necessarily required but fixes a bug with some servers
				connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				// submit the post_string and close the connection
				DataOutputStream requestObject = new DataOutputStream(connection.getOutputStream());

				requestObject.write(post_string.toString().getBytes());
				requestObject.flush();
				requestObject.close();
				// process and read the gateway response
				BufferedReader rawResponse = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				// String line;
				String responseData = rawResponse.readLine();
				rawResponse.close(); // no more data

				// split the response into an array*/

				// String responseData = "0,1,2"; //Test value set to responseData by Johnson
				log.info("responseData " + responseData);
				String[] responses = responseData.split("\\|");
				apiResponse = responses[2];
				transactionNumber = responses[6];
				log.info("apiResponse " + apiResponse);
				log.info("apiResponse " + apiResponse);

				if (responses[2].equals("1")) {
					apiResponse = "TRANSACTION SUCCESSFFUL";
					// transactionNumber = responses[6];
					// adding subscriber details
					statesSubscribed = EcommUtil.getStateNamesRatesId(rateIds);
					renewUser(ecommData.getLoginId(), ecommData.getRateId(), String.valueOf(salesTax), String.valueOf(amountCharged));

					mailContentSubscriber =
							mailContentUser(ecommData.getFirstName(), ecommData.getLastName(), ecommData.getCompanyName(), ecommData.getAddress1(),
									ecommData.getCity(), ecommData.getStateName(), ecommData.getZip(), ecommData.getTelephone(), ecommData.getFax(),
									ecommData.getEmail(), ecommData.getCCType(), ecommData.getCCName(), EcommUtil.decryptCc(ecommData.getCCNumber()),
									ecommData.getCCAddress(), statesSubscribed, String.valueOf(amountCharged), "CLC" + "-" + transactionNumber + "-"
											+ invoiceId + "-" + LibraryFunctions.getTodayDate(), String.valueOf(salesTax), rateIds,
									ecommData.getBillingCompanyName(), ecommData.getBillingAddress(), ecommData.getBillingCity(),
									ecommData.getBillingStateName(), ecommData.getBillingZip(), ecommData.getShippingCompanyName(),
									ecommData.getShippingAddress(), ecommData.getShippingCity(), ecommData.getShippingStateName(), ecommData.getShippingZip());

					mailContentInvoice =
							mailContentInvoice(ecommData.getFirstName(), ecommData.getLastName(), ecommData.getCompanyName(), ecommData.getAddress1(),
									ecommData.getAddress2(), ecommData.getCity(), ecommData.getStateName(), ecommData.getZip(), ecommData.getTelephone(),
									ecommData.getFax(), ecommData.getEmail(), ecommData.getCCType(), ecommData.getCCName(),
									EcommUtil.decryptCc(ecommData.getCCNumber()), ecommData.getCCAddress(), statesSubscribed, String.valueOf(amountCharged),
									"CLC" + "-" + transactionNumber + "-" + invoiceId + "-" + LibraryFunctions.getTodayDate(), String.valueOf(salesTax),
									rateIds, ecommData.getBillingCompanyName(), ecommData.getBillingAddress(), ecommData.getBillingCity(),
									ecommData.getBillingStateName(), ecommData.getBillingZip(), ecommData.getShippingCompanyName(),
									ecommData.getShippingAddress(), ecommData.getShippingCity(), ecommData.getShippingStateName(), ecommData.getShippingZip());

					// Mail compose goes here-subscriber
					composeMail(mailContentSubscriber, "Constructionleads.com - Online Order Auto-Renewal Information", ecommData.getEmail());
					// Mail compose goes here-Invoice
					composeMail(mailContentInvoice, "Constructionleads.com - Online Subscription Auto-Renewal Information", invoiceEmailId);
					EcommUtil.sendCLCUpgrade(ecommData.getLoginId(), rateIds, String.valueOf(salesTax), String.valueOf(amountCharged), headerTitleCounter,
							"CLC" + "-" + transactionNumber + "-" + invoiceId + "-" + LibraryFunctions.getTodayDate());
					// headercount for printing title in excel Sheet.
					headerTitleCounter = headerTitleCounter + 1;

					log.info("PLEcommerceRenewal " + apiResponse + " invoiceId " + invoiceId);
					log.info("PLEcommerceRenewal " + apiResponse + " invoiceId " + invoiceId);

				}

				log.info("apiResponse " + apiResponse + " invoiceId " + invoiceId);
				log.info("PLEcommerceRenewal " + apiResponse + " invoiceId " + invoiceId);

			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("ERROR IN autoRenewSchedule " + e.toString());
			log.error(e);
		}
		return apiResponse;
	}

	/**
	 * @Main method
	 */
	public static void main(String[] args) {
		try {
			PLEcommerceRenewal pk = new PLEcommerceRenewal();
			String results = pk.autoRenewSchedulePL();
			log.info(results + " value");
			String dateformat = null;
			if (results != null) {
				dateformat = new SimpleDateFormat("MMddyy").format(new java.util.Date());
				mailAttachment("d:/clc_" + dateformat + ".csv", dateformat);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
