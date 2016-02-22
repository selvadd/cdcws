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
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.mail.Transport;
import javax.mail.internet.InternetAddress;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import wsdatamodel.LeadManagerSessionData;
import wsdatamodel.RatesData;
import wsexception.LoginException;
import wsutils.EcommUtil;
import wsutils.JDBCUtil;
import wsutils.LibraryFunctions;
import wsutils.LoginUtil;
import wsutils.SecretEncryptDecrypt;

import com.google.gson.Gson;

/**
 * @author Selva
 *
 */

@RestController
@RequestMapping(value = "/services/PLEcommerce")
public class PLEcommerceController {

	private static Logger log = Logger.getLogger(PLEcommerceController.class.getName());

	public String getYear(String date) {
		String year = null;
		year = date.substring(2, date.length() - 6);
		return year;
	}

	public String getMonth(String date) {
		String month = null;
		month = date.substring(5, date.length() - 3);
		return month;
	}

	public static String decryptCc(String ccnumber) {
		String ReverseString = "$X0r#1h2N@";
		String decryptedCC = "";
		for (int i = 0; i < ccnumber.length(); i++) {
			decryptedCC = decryptedCC + ReverseString.indexOf(ccnumber.substring(i, i + 1));
		}
		return decryptedCC;
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

	// to get the sales tax info for ecommerce based on the city,state and zip.
	@RequestMapping(value = "/getSalesTaxInfo")
	public String getSalesTaxInfo(@RequestParam("city") String city, @RequestParam("state") String state, @RequestParam("zip") String zip) {

		Map<String, Object> map = null;
		Gson gson = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			map.put("aaData", String.valueOf(getSalesTax(city, state, zip)));

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Exception in getSalesTaxInfo() for getSalesTaxInfo " + ex.getMessage());
		}

		return gson.toJson(map);

	}

	// GET STATES AS PER TERM
	@RequestMapping(value = "/getStateProducts")
	public String getStateProducts(@RequestParam("term") String term) {

		Map<String, Object> map = null;
		Gson gson = null;

		try {
			gson = new Gson();
			map = new HashMap<String, Object>();

			map.put("aaData", EcommUtil.getStateProducts(term));
			map.put("aaColumns", "state_name,rate_id,state_id");
			log.info("getStateProducts" + gson.toJson(map));

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Exception in getStateProducts() for public search " + ex.getMessage());
		}

		return gson.toJson(map);

	}

	// validateLoginId
	@RequestMapping(value = "/validateLoginId")
	public String validateLoginId(@RequestParam("loginId") String loginId) {

		Map<String, Object> map = null;
		Gson gson = null;
		String message = null;

		try {
			gson = new Gson();
			map = new HashMap<String, Object>();

			message = EcommUtil.validateLoginId(loginId);
			map.put("message", message);

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Exception in validateLoginId()" + ex.getMessage());
		}

		return gson.toJson(map);

	}

	// Get getTotalB4Tax
	public static float getTotalB4Tax(String ratesIds) throws LoginException {
		CallableStatement cstmt = null;
		ResultSet rs = null;
		Connection con = null;
		float totalb4Tax = 0;
		// String sql = null;

		try {

			if (con == null) {
				con = JDBCUtil.getDBConnectionFromDatabase();
			}
			cstmt = con.prepareCall("{call ssp_pl_total_before_tax(?)}");

			cstmt.setString(1, ratesIds);

			rs = cstmt.executeQuery();
			while (rs.next()) {
				totalb4Tax = rs.getFloat("totalb4tax");

			}

		} catch (Exception e) {
			log.error("getTotalB4Tax" + e.toString());
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
				sqle.printStackTrace();

			}

		}

		return totalb4Tax;
	} // End of getTotalB4Tax()

	// Get Sales Tax for the given city,state,zip
	public static float getSalesTax(String city, String state, String zip) throws LoginException {
		CallableStatement cstmt = null;
		ResultSet rs = null;
		Connection con = null;
		float salesTax = 0;
		// String sql = null;

		try {

			if (con == null) {
				con = JDBCUtil.getDBConnectionFromDatabase();
			}
			cstmt = con.prepareCall("{call ssp_pl_get_sales_rate(?,?,?)}");

			cstmt.setString(1, city);
			cstmt.setString(2, state);
			cstmt.setString(3, zip);

			rs = cstmt.executeQuery();
			while (rs.next()) {
				salesTax = rs.getFloat("sales_tax");

			}

		} catch (Exception e) {

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
				sqle.printStackTrace();

			}

		}

		return salesTax;
	} // End of getSalesTax()

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
			log.error("getInvoiceId" + e.toString());
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
				sqle.printStackTrace();

			}

		}

		return invoiceId;

	} // End of getInvoiceId()

	public static String getRowforStatesSubscribed(String rateIdList, String salesTax, String totalPrice) throws LoginException {
		String rows = null;

		ArrayList<RatesData> ratesPurchasedList = getRatesDetails(rateIdList);
		log.info("getRowforStatesSubscribed" + ratesPurchasedList.size());
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

		log.info("ROWS" + rows);

		return rows;

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

			// String host = "172.16.22.29";
			String host = "wdh0189-new";
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

			log.info(txtmessage + "mail message");

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

	// mailContentUser-mail content to user when subscribes to state_id
	/*
	 * public String mailContentUser(String pwd, String fname, String lname, String companyName, String addr1, String addr2, String city, String state, String
	 * zip, String phone, String fax, String email, String ccType, String nameOnCC, String ccNumber, String ccAddress, String shipToNumber, String
	 * stateSubscribed, String amountCharged, String transactionId, String salesTax, String rateIds, String billingcompanyName, String billingaddr, String
	 * billingcity, String billingstate, String billingzip, String loginId
	 * 
	 * ) { String mailContent = null;
	 * 
	 * try {
	 * 
	 * mailContent = "Dear <b>" + fname + "&nbsp;" + lname + ",</b><br><br> Thank you for subscribing to <b>constructionleads.com</b>.<br>" +
	 * " You can access your constructionleads.com subscription by going to our website" +
	 * " <a href=http://www.constructionleads.com target=_blank > <b>www.constructionleads.com<b></a> and logging in.<br><br>" +
	 * "The following is your access information. Please keep this in a safe place.<br>" + " <table cellspacing=0 cellpadding=0 border=1>" + " <tbody>" +
	 * "  <tr>" + "   <td colspan=2>User ID for ConstructionLeads.com </td>" + "</tr>" + "<tr>" + " <td> Login Id: <b>" + loginId + "</b> </td>" +
	 * "<td>&nbsp;Password: <b>" + pwd + "</b> </td>" + "</tr>" + "<tr>" + " <td>&nbsp;Order Confirmation #:<b>" + transactionId + "</b> </td>" +
	 * "<td>&nbsp;Dated:<b>" + LibraryFunctions.getTodayDate() + "</b> </td>" + "</tr>" + "</tbody>" + "</table>" + " <br>" + "" +
	 * getRowforStatesSubscribed(rateIds, salesTax, amountCharged) + "" + "<br>" + "<b>Subscriber Details:</b>" +
	 * " <table cellspacing=0 cellpadding=0 border=1>" + "<tbody>" + " <tr>" + " <td width=200><b>Contact Info</b></td>" +
	 * "<td width=200><b>Shipping Info</b></td>" + "  </tr>" + "  <tr>" + " <td>" + fname + "&nbsp;" + lname + "</td>" + "  <td>&nbsp;</td>" + " </tr>" +
	 * " <tr>" + " <td> " + companyName + "</td>" + "  <td> " + companyName + "</td>" + " </tr>" + " <tr>" + "  <td> " + addr1 + "</td>" + "  <td> " + addr1 +
	 * "</td>" + " </tr>" + " <tr>" + " <td></td>" + " <td></td>" + "</tr>" + " <tr>" + "  <td> " + city + " </td>" + " <td>  " + city + "</td>" + " </tr>" +
	 * "  <tr>" + "  <td> " + state + " </td>" + "  <td> " + state + "</td>" + "</tr>" + " <tr>" + " <td> " + zip + " </td>" + "<td> " + zip + "</td>" + "</tr>"
	 * + "<tr>" + "  <td>" + phone + "(T) </td>" + " <td>&nbsp;</td>" + "</tr>" + " <tr>" + "<td>" + fax + "(F)</td>" + " <td>&nbsp;</td>" + " </tr>" + " <tr>"
	 * + "   <td>" + email + " </td>" + " <td>&nbsp;</td>" + "  </tr>" + " <tr>" + "<td>&nbsp;</td>" + " <td>&nbsp;</td>" + "</tr>" + " <tr>" +
	 * "  <td><b>Billing Info</b></td>" + "   <td><b>Credit Card Info</b></td>" + " </tr>" + "<tr>" + " <td>" + billingcompanyName + "</td>" + "  <td>" + ccType
	 * + "</td>" + " </tr>" + "<tr>" + "  <td>" + billingaddr + "</td>" + " <td>****" + ccNumber.substring(12, ccNumber.length()) + "</td>" + "</tr>" + "<tr>" +
	 * "  <td></td>" + "  <td>&nbsp;</td>" + "</tr>" + "<tr>" + "  <td>" + billingcity + "</td>" + "  <td>&nbsp;</td>" + " </tr>" + " <tr>" + "<td>" +
	 * billingstate + "</td>" + " <td>&nbsp;</td>" + "</tr>" + "<tr>" + " <td>" + billingzip + "</td>" + "<td>&nbsp;</td>" + "</tr>" + "</tbody>" + "</table>" +
	 * "<br>" + "<b>-Sales-ConstructionLeads.com</b>";
	 * 
	 * } catch (Exception e) { log.error("ERROR IN mailContentUser " + e.toString()); }
	 * 
	 * return mailContent; }
	 * 
	 * //mailContentUser-mail content to user when subscribes to state_id public String mailContentInvoice(String pwd, String fname, String lname, String
	 * companyName, String addr1, String addr2, String city, String state, String zip, String phone, String fax, String email, String ccType, String nameOnCC,
	 * String ccNumber, String ccAddress, String shipToNumber, String stateSubscribed, String amountCharged, String transactionId, String salesTax, String
	 * rateIds, String billingcompanyName, String billingaddr, String billingcity, String billingstate, String billingzip, String loginId
	 * 
	 * ) { String mailContent = null; try {
	 * 
	 * mailContent = "The access information for the Subscriber <b>" + fname + "&nbsp;" + lname + ":</b><br>" + " <table cellspacing=0 cellpadding=0 border=1>"
	 * + " <tbody>" + "  <tr>" + "   <td colspan=2>User Id for ConstructionLeads.com </td>" + "</tr>" + "<tr>" + " <td> Login Id: <b>" + loginId + "</b> </td>"
	 * + "<td>&nbsp;Password: <b>" + pwd + "</b> </td>" + "</tr>" + "<tr>" + " <td>&nbsp;Order Confirmation #:<b>" + transactionId + "</b> </td>" +
	 * "<td>&nbsp;Dated:<b>" + LibraryFunctions.getTodayDate() + "</b> </td>" + "</tr>" + "</tbody>" + "</table>" + " <br>" + "" +
	 * getRowforStatesSubscribed(rateIds, salesTax, amountCharged) + "" + "<b>Subscriber Details:</b>" + " <table cellspacing=0 cellpadding=0 border=1>" +
	 * "<tbody>" + " <tr>" + " <td width=200><b>Contact Info</b></td>" + "<td width=200><b>Shipping Info</b></td>" + "  </tr>" + "  <tr>" + " <td>" + fname +
	 * "&nbsp;" + lname + "</td>" + "  <td>&nbsp;</td>" + " </tr>" + " <tr>" + " <td>contact company " + companyName + "</td>" + "  <td>shipping company " +
	 * companyName + "</td>" + " </tr>" + " <tr>" + "  <td>contact company " + addr1 + "</td>" + "  <td>shipping company " + addr1 + "</td>" + " </tr>" +
	 * " <tr>" + " <td></td>" + " <td></td>" + "</tr>" + " <tr>" + "  <td>contact company " + city + " </td>" + " <td>shipping company  " + city + "</td>" +
	 * " </tr>" + "  <tr>" + "  <td>contact " + state + " </td>" + "  <td>shipping " + state + "</td>" + "</tr>" + " <tr>" + " <td>contact " + zip + " </td>" +
	 * "<td>shipping " + zip + "</td>" + "</tr>" + "<tr>" + "  <td>" + phone + "(T) </td>" + " <td>&nbsp;</td>" + "</tr>" + " <tr>" + "<td>" + fax + "(F)</td>"
	 * + " <td>&nbsp;</td>" + " </tr>" + " <tr>" + "   <td>" + email + " </td>" + " <td>&nbsp;</td>" + "  </tr>" + " <tr>" + "<td>&nbsp;</td>" +
	 * " <td>&nbsp;</td>" + "</tr>" + " <tr>" + "  <td><b>Billing Info</b></td>" + "   <td><b>Credit Card Info</b></td>" + " </tr>" + "<tr>" + " <td>" +
	 * billingcompanyName + "</td>" + "  <td>" + ccType + "</td>" + " </tr>" + "<tr>" + "  <td>" + billingaddr + "</td>" + " <td>****" + ccNumber.substring(12,
	 * ccNumber.length()) + "</td>" + "</tr>" + "<tr>" + "  <td></td>" + "  <td>&nbsp;</td>" + "</tr>" + "<tr>" + "  <td>" + billingcity + "</td>" +
	 * "  <td>&nbsp;</td>" + " </tr>" + " <tr>" + "<td>" + billingstate + "</td>" + " <td>&nbsp;</td>" + "</tr>" + "<tr>" + " <td>" + billingzip + "</td>" +
	 * "<td>&nbsp;</td>" + "</tr>" + "</tbody>" + "</table>" + "<br>" + "<b>-Sales-ConstructionLeads.com</b>";
	 * 
	 * } catch (Exception e) { log.error("ERROR IN mailContentInvoice " + e.toString()); }
	 * 
	 * return mailContent; }
	 * 
	 * //addPLSubscriber once the credit card is processed. public static boolean addPLSubscriber(String pwd, String fname, String lname, String companyName,
	 * String addr1, String addr2, String city, String state, String zip, String phone, String fax, String email, String ccType, String nameOnCC, String
	 * ccNumber, String ccAddress, String ccExpiryDate, String shipToNumber, String stateSubscribed, String transactionNumber, String md5Hash, String
	 * shippingCompanyName, String shippingAddress, String shippingCity, String shippingState, String shippingZip, String billingCompanyName, String
	 * billingAddress, String billingCity, String billingState, String billingZip, String totalb4tax, String salestax, String rateIds, String plAccessId, String
	 * loginId, String autoEnableFlag
	 * 
	 * ) throws LoginException { CallableStatement cstmt = null; ResultSet rs = null; Connection con = null; boolean insertFlag = false; String sql = null; //
	 * See also Encrypting with DES Using a Pass Phrase.
	 * 
	 * try {
	 * 
	 * if (con == null) { con = JDBCUtil.getDBConnectionFromDatabase(); } cstmt = con.prepareCall(
	 * "{call SSP_PL_SUBSCRIBER_ADD(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}");
	 * 
	 * //END OF ENCRYPTION
	 * 
	 * //email id will be used as LoginId cstmt.setString(1, loginId); //password cstmt.setString(2, pwd); //first name cstmt.setString(3, fname); //last name
	 * cstmt.setString(4, lname); //company name if (companyName != null) { cstmt.setString(5, companyName); } else { cstmt.setString(5, null); } //address1
	 * cstmt.setString(6, addr1); //address2 if (addr2 != null) { cstmt.setString(7, addr2); } else { cstmt.setString(7, addr2); } //city cstmt.setString(8,
	 * city); //state cstmt.setString(9, state); //zipcode cstmt.setString(10, zip); //phone cstmt.setString(11, phone); //fax if (fax != null) {
	 * cstmt.setString(12, fax); } else { cstmt.setString(12, null); } //emailid cstmt.setString(13, email); //credit card type
	 * 
	 * cstmt.setString(14, ccType); //name on cc cstmt.setString(15, nameOnCC); //cc number //New DESENCRYPTER ALGORITHM FOR STORING ENCRYPTED VALUE OF CCNUM
	 * cstmt.setString(16, SecretEncryptDecrypt.encrypt(ccNumber)); //cc address
	 * 
	 * cstmt.setString(17, (ccAddress));
	 * 
	 * //cc expiry date cstmt.setString(18, ccExpiryDate);
	 * 
	 * //shipto number if (shipToNumber != null) { cstmt.setString(19, shipToNumber); } else { cstmt.setString(19, null); } //state subscribed by the user
	 * cstmt.setString(20, stateSubscribed); //transactionNumber cstmt.setString(21, transactionNumber); //md5Hash cstmt.setString(22, md5Hash);
	 * //shippingCompanyName cstmt.setString(23, shippingCompanyName); //shippingAddress cstmt.setString(24, shippingAddress); //shippingCity
	 * cstmt.setString(25, shippingCity);
	 * 
	 * //shippingState cstmt.setString(26, shippingState); //shippingZip cstmt.setString(27, shippingAddress);
	 * 
	 * //billingCompanyName cstmt.setString(28, billingCompanyName); //billingAddress cstmt.setString(29, billingAddress); //billingCity cstmt.setString(30,
	 * billingCity);
	 * 
	 * //billingState cstmt.setString(31, billingState); //billingZip cstmt.setString(32, billingZip); //totalb4tax cstmt.setString(33, totalb4tax); //salestax
	 * cstmt.setString(34, salestax); //rateIds cstmt.setString(35, rateIds); //plAccessId cstmt.setString(36, plAccessId); //autoenableFlag cstmt.setString(37,
	 * autoEnableFlag);
	 * 
	 * cstmt.executeUpdate(); insertFlag = true; } catch (Exception e) { insertFlag = false; e.printStackTrace();
	 * 
	 * }
	 * 
	 * finally {
	 * 
	 * try { if (cstmt != null) { cstmt.close(); } if (con != null) { con.close(); }
	 * 
	 * } catch (SQLException sqle) { sqle.printStackTrace(); insertFlag = false;
	 * 
	 * } return insertFlag; }
	 * 
	 * } // End of addPLSubscriber()
	 * 
	 * public String transaction(String pwd, String fname, String lname, String companyName, String addr1, String addr2, String city, String state, String zip,
	 * String phone, String fax, String email, String ccType, String nameOnCC, String ccNumber, String ccAddress, String ccExpiryDate, String shipToNumber,
	 * 
	 * String shippingCompanyName, String shippingAddress, String shippingCity, String shippingState, String shippingZip, String billingCompanyName, String
	 * billingAddress, String billingCity, String billingState, String billingZip, String rateIds, String plAccessId, String loginId, String autoEnableFlag
	 * 
	 * ) throws LoginException {
	 * 
	 * String apiResponse = null; String mailContentSubscriber = null; String mailContentInvoice = null; float amountCharged = 0; float salesTax = 0; String
	 * statesSubscribed = null; String transactionNumber = null; String invoiceEmailId = "johnson@tentsoftware.com"; String md5Hash = "md5hash"; JSONObject
	 * jsonObject = null; String ccMonthYear = null;
	 * 
	 * // By default, this sample code is designed to post to our test server for // developer accounts: https://test.authorize.net/gateway/transact.dll // for
	 * real accounts (even in test mode), please make sure that you are // posting to: https://secure.authorize.net/gateway/transact.dll try {
	 * 
	 * //TAX CALCULATION GOES HERE //FOR SALES TAX
	 * 
	 * salesTax = getTotalB4Tax(rateIds) * getSalesTax(city, state, zip);
	 * 
	 * //amount total charged amountCharged = getTotalB4Tax(rateIds) + (salesTax);
	 * 
	 * ccMonthYear = getMonth(ccExpiryDate) + getYear(ccExpiryDate);
	 * 
	 * URL post_url = new URL("https://test.authorize.net/gateway/transact.dll"); Hashtable post_values = new Hashtable(); // the API Login ID and Transaction
	 * Key must be replaced with valid values post_values.put("x_login", "3E2eayC4XgN"); post_values.put("x_tran_key", "2dy8NRK56h8kK2e5");
	 * post_values.put("x_version", "3.1"); post_values.put("x_delim_data", "TRUE"); post_values.put("x_delim_char", "|"); post_values.put("x_relay_response",
	 * "FALSE"); post_values.put("x_type", "AUTH_CAPTURE"); post_values.put("x_method", "CC"); post_values.put("x_card_num", decryptCc(ccNumber));
	 * post_values.put("x_exp_date", ccMonthYear); post_values.put("x_amount", String.valueOf(amountCharged)); post_values.put("x_description",
	 * "Sample Transaction"); post_values.put("x_first_name", fname); post_values.put("x_last_name", lname); post_values.put("x_address", ccAddress);
	 * post_values.put("x_state", state); post_values.put("x_zip", zip); post_values.put("x_ship_to_company", shippingCompanyName);
	 * post_values.put("x_ship_to_address", shippingAddress); post_values.put("x_ship_to_city", shippingCity); post_values.put("x_ship_to_state",
	 * shippingState); post_values.put("x_ship_to_zip", shippingZip); // Additional fields can be added here as outlined in the AIM integration // guide at:
	 * http://developer.authorize.net // This section takes the input fields and converts them to the proper format // for an http post. For example:
	 * "x_login=username&x_tran_key=a1B2c3D4"
	 * 
	 * StringBuffer post_string = new StringBuffer(); Enumeration keys = post_values.keys();
	 * 
	 * while (keys.hasMoreElements()) { String key = URLEncoder.encode(keys.nextElement().toString(), "UTF-8"); String value =
	 * URLEncoder.encode(post_values.get(key).toString(), "UTF-8"); post_string.append(key + "=" + value + "&");
	 * 
	 * } // Open a URLConnection to the specified post url URLConnection connection = post_url.openConnection();
	 * 
	 * connection.setDoOutput(true); connection.setUseCaches(false); // this line is not necessarily required but fixes a bug with some servers
	 * connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); // submit the post_string and close the connection DataOutputStream
	 * requestObject = new DataOutputStream(connection. getOutputStream());
	 * 
	 * requestObject.write(post_string.toString().getBytes()); requestObject.flush(); requestObject.close(); // process and read the gateway response
	 * BufferedReader rawResponse = new BufferedReader(new InputStreamReader( connection.getInputStream())); String line; String responseData =
	 * rawResponse.readLine(); rawResponse.close(); // no more data
	 * 
	 * // split the response into an array
	 * 
	 * String[] responses = responseData.split("\\|"); apiResponse = responses[2]; transactionNumber = responses[6]; jsonObject = new JSONObject(); if
	 * (responses[2].equals("1")) { apiResponse = "TRANSACTION SUCCESSFFUL";
	 * 
	 * // transactionNumber = responses[6]; //adding subscriber details statesSubscribed = EcommUtil.getStateNamesRatesId(rateIds); boolean useradded = false;
	 * useradded = addPLSubscriber(pwd, fname, lname, companyName, addr1, addr2, city, state, zip, phone, fax, email, ccType, nameOnCC, decryptCc(ccNumber),
	 * ccAddress, ccExpiryDate, shipToNumber, statesSubscribed, transactionNumber, md5Hash, shippingCompanyName, shippingAddress, shippingCity, shippingState,
	 * shippingZip, billingCompanyName, billingAddress, billingCity, billingState, billingZip, String.valueOf(getTotalB4Tax(rateIds)), String.valueOf(salesTax),
	 * rateIds, plAccessId, loginId, autoEnableFlag); log.info("useradded" + useradded); if (useradded == true) { // mailContentSubscriber =
	 * mailContentUser(pwd, fname, lname, companyName, addr1, addr2, city, state, zip, phone, fax, email, ccType, nameOnCC, decryptCc(ccNumber), ccAddress,
	 * shipToNumber, statesSubscribed, String.valueOf(amountCharged), transactionNumber, String.valueOf(salesTax), rateIds, billingCompanyName, billingAddress,
	 * billingCity, billingState, billingZip, loginId) ; mailContentInvoice = mailContentInvoice(pwd, fname, lname, companyName, addr1, addr2, city, state, zip,
	 * phone, fax, email, ccType, nameOnCC, decryptCc(ccNumber), ccAddress, shipToNumber, statesSubscribed, String.valueOf(amountCharged), transactionNumber,
	 * String.valueOf(salesTax), rateIds, billingCompanyName, billingAddress, billingCity, billingState, billingZip, loginId) ; //Mail compose goes
	 * here-subscriber composeMail(mailContentSubscriber, "Constructionleads.com - Online Order Confirmation", email); //Mail compose goes here-Invoice
	 * composeMail(mailContentInvoice, "Constructionleads.com - Online Subscriber Information", invoiceEmailId);
	 * 
	 * map.put("Message", apiResponse); } else { map.put("Message", "user not inserted"); } } else { map.put("Message", responses[3]); }
	 * 
	 * } catch (Exception e) { log.error(e); e.printStackTrace(); } return gson.toJson(map); }
	 */

	// addPLSubscriber once the credit card is processed.
	public static boolean addPLSubscriber(String pwd, String fname, String lname, String companyName, String addr1, String addr2, String city, String state,
			String zip, String phone, String fax, String email, String ccType, String nameOnCC, String ccNumber, String ccAddress, String ccExpiryDate,
			String stateSubscribed, String transactionNumber, String md5Hash, String shippingCompanyName, String shippingAddress, String shippingCity,
			String shippingState, String shippingZip, String billingCompanyName, String billingAddress, String billingCity, String billingState,
			String billingZip, String totalb4tax, String salestax, String rateIds, String plAccessId, String loginId, String autoEnableFlag)
			throws LoginException {

		CallableStatement cstmt = null;
		// ResultSet rs = null;
		Connection con = null;
		boolean insertFlag = false;
		// String sql = null;
		// See also Encrypting with DES Using a Pass Phrase.

		try {

			if (con == null) {
				con = JDBCUtil.getDBConnectionFromDatabase();
			}
			cstmt = con.prepareCall("{call SSP_PL_SUBSCRIBER_ADD(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}");

			// END OF ENCRYPTION

			// email id will be used as LoginId
			cstmt.setString(1, loginId);
			// password
			cstmt.setString(2, pwd);
			// first name
			cstmt.setString(3, fname);
			// last name
			cstmt.setString(4, lname);
			// company name
			if (companyName != null) {
				cstmt.setString(5, companyName);
			} else {
				cstmt.setString(5, null);
			}
			// address1
			cstmt.setString(6, addr1);
			// address2
			if (addr2 != null) {
				cstmt.setString(7, addr2);
			} else {
				cstmt.setString(7, addr2);
			}
			// city
			cstmt.setString(8, city);
			// state
			cstmt.setString(9, state);
			// zipcode
			cstmt.setString(10, zip);
			// phone
			cstmt.setString(11, phone);
			// fax
			if (fax != null) {
				cstmt.setString(12, fax);
			} else {
				cstmt.setString(12, null);
			}
			// emailid
			cstmt.setString(13, email);
			// credit card type

			cstmt.setString(14, ccType);
			// name on cc
			cstmt.setString(15, nameOnCC);
			// cc number
			// New DESENCRYPTER ALGORITHM FOR STORING ENCRYPTED VALUE OF CCNUM
			cstmt.setString(16, SecretEncryptDecrypt.encrypt(ccNumber));
			// cc address

			cstmt.setString(17, (ccAddress));

			// cc expiry date
			cstmt.setString(18, ccExpiryDate);

			// state subscribed by the user
			cstmt.setString(19, stateSubscribed);
			// transactionNumber
			cstmt.setString(20, transactionNumber);
			// md5Hash
			cstmt.setString(21, md5Hash);
			// shippingCompanyName
			cstmt.setString(22, shippingCompanyName);
			// shippingAddress
			cstmt.setString(23, shippingAddress);
			// shippingCity
			cstmt.setString(24, shippingCity);

			// shippingState
			cstmt.setString(25, shippingState);
			// shippingZip
			cstmt.setString(26, shippingZip);

			// billingCompanyName
			cstmt.setString(27, billingCompanyName);
			// billingAddress
			cstmt.setString(28, billingAddress);
			// billingCity
			cstmt.setString(29, billingCity);

			// billingState
			cstmt.setString(30, billingState);
			// billingZip
			cstmt.setString(31, billingZip);
			// totalb4tax
			cstmt.setString(32, totalb4tax);
			// salestax
			cstmt.setString(33, salestax);
			// rateIds
			cstmt.setString(34, rateIds);
			// plAccessId
			cstmt.setString(35, plAccessId);
			// autoenableFlag
			cstmt.setString(36, autoEnableFlag);

			cstmt.executeUpdate();
			insertFlag = true;
		} catch (Exception e) {
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
				sqle.printStackTrace();
				insertFlag = false;

			}

		}

		return insertFlag;

	} // End of addPLSubscriber()

	// mailContentUser-mail content to user when subscribes to state_id
	public String mailContentUser(String pwd, String fname, String lname, String companyName, String addr1, String addr2, String city, String state,
			String zip, String phone, String fax, String email, String ccType, String nameOnCC, String ccNumber, String ccAddress, String stateSubscribed,
			String amountCharged, String transactionId, String salesTax, String rateIds, String billingcompanyName, String billingaddr, String billingcity,
			String billingstate, String billingzip, String loginId, String shippingCompanyName, String shippingAddress, String shippingCity,
			String shippingState, String shippingZip) {

		String mailContent = null;

		try {

			mailContent =
					"Dear <b>" + fname + "&nbsp;" + lname + ",</b><br><br> Thank you for subscribing to <b>constructionleads.com</b>.<br>"
							+ " You can access your constructionleads.com subscription by going to our website"
							+ " <a href=http://www.constructionleads.com target=_blank > <b>www.constructionleads.com<b></a> and logging in.<br><br>"
							+ "The following is your access information. Please keep this in a safe place.<br>"
							+ " <table cellspacing=0 cellpadding=0 border=1>" + " <tbody>" + "  <tr>"
							+ "   <td colspan=2>User ID for ConstructionLeads.com </td>" + "</tr>" + "<tr>" + " <td> Login Id: <b>" + loginId + "</b> </td>"
							+ "<td>&nbsp;Password: <b>" + pwd + "</b> </td>" + "</tr>" + "<tr>" + " <td>&nbsp;Order Confirmation #:<b>" + transactionId
							+ "</b> </td>" + "<td>&nbsp;Dated:<b>" + LibraryFunctions.getTodayDate() + "</b> </td>" + "</tr>" + "</tbody>" + "</table>"
							+ " <br>" + "" + getRowforStatesSubscribed(rateIds, salesTax, amountCharged) + "" + "<br>" + "<b>Subscriber Details:</b>"
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

		} catch (Exception e) {
			log.error("ERROR IN mailContentUser " + e.toString());
		}

		return mailContent;
	}

	// mailContentUser-mail content to user when subscribes to state_id
	public String mailContentInvoice(String pwd, String fname, String lname, String companyName, String addr1, String addr2, String city, String state,
			String zip, String phone, String fax, String email, String ccType, String nameOnCC, String ccNumber, String ccAddress, String stateSubscribed,
			String amountCharged, String transactionId, String salesTax, String rateIds, String billingcompanyName, String billingaddr, String billingcity,
			String billingstate, String billingzip, String loginId, String shippingCompanyName, String shippingAddress, String shippingCity,
			String shippingState, String shippingZip) {

		String mailContent = null;
		try {

			mailContent =
					"The access information for the Subscriber <b>" + fname + "&nbsp;" + lname + ":</b><br>" + " <table cellspacing=0 cellpadding=0 border=1>"
							+ " <tbody>" + "  <tr>" + "   <td colspan=2>User Id for ConstructionLeads.com </td>" + "</tr>" + "<tr>" + " <td> Login Id: <b>"
							+ loginId + "</b> </td>" + "<td>&nbsp;Password: <b>" + pwd + "</b> </td>" + "</tr>" + "<tr>"
							+ " <td>&nbsp;Order Confirmation #:<b>" + transactionId + "</b> </td>" + "<td>&nbsp;Dated:<b>" + LibraryFunctions.getTodayDate()
							+ "</b> </td>" + "</tr>" + "</tbody>" + "</table>" + " <br>" + "" + getRowforStatesSubscribed(rateIds, salesTax, amountCharged)
							+ "" + "<b>Subscriber Details:</b>" + " <table cellspacing=0 cellpadding=0 border=1>" + "<tbody>" + " <tr>"
							+ " <td width=200><b>Contact Info</b></td>" + "<td width=200><b>Shipping Info</b></td>" + "  </tr>" + "  <tr>" + " <td>" + fname
							+ "&nbsp;" + lname + "</td>" + "  <td>&nbsp;</td>" + " </tr>" + " <tr>" + " <td>" + companyName + "</td>" + "  <td>"
							+ shippingCompanyName + "</td>" + " </tr>" + " <tr>" + "  <td>" + addr1 + "</td>" + "  <td>" + shippingAddress + "</td>" + " </tr>"
							+ " <tr>" + " <td></td>" + " <td></td>" + "</tr>" + " <tr>" + "  <td> " + city + " </td>" + " <td>  " + shippingCity + "</td>"
							+ " </tr>" + "  <tr>" + "  <td>" + state + " </td>" + "  <td>" + shippingState + "</td>" + "</tr>" + " <tr>" + " <td>" + zip
							+ " </td>" + "<td>" + shippingZip + "</td>" + "</tr>" + "<tr>" + "  <td>" + phone + "(T) </td>" + " <td>&nbsp;</td>" + "</tr>"
							+ " <tr>" + "<td>" + fax + "(F)</td>" + " <td>&nbsp;</td>" + " </tr>" + " <tr>" + "   <td>" + email + " </td>" + " <td>&nbsp;</td>"
							+ "  </tr>" + " <tr>" + "<td>&nbsp;</td>" + " <td>&nbsp;</td>" + "</tr>" + " <tr>" + "  <td><b>Billing Info</b></td>"
							+ "   <td><b>Credit Card Info</b></td>" + " </tr>" + "<tr>" + " <td>" + billingcompanyName + "</td>" + "  <td>" + ccType + "</td>"
							+ " </tr>" + "<tr>" + "  <td>" + billingaddr + "</td>" + " <td>****" + ccNumber.substring(12, ccNumber.length()) + "</td>"
							+ "</tr>" + "<tr>" + "  <td></td>" + "  <td>&nbsp;</td>" + "</tr>" + "<tr>" + "  <td>" + billingcity + "</td>"
							+ "  <td>&nbsp;</td>" + " </tr>" + " <tr>" + "<td>" + billingstate + "</td>" + " <td>&nbsp;</td>" + "</tr>" + "<tr>" + " <td>"
							+ billingzip + "</td>" + "<td>&nbsp;</td>" + "</tr>" + "</tbody>" + "</table>" + "<br>" + "<b>-Sales-ConstructionLeads.com</b>";

		} catch (Exception e) {
			log.error("ERROR IN mailContentInvoice " + e.toString());
		}

		return mailContent;
	}

	@RequestMapping(value = "/transaction")
	public String transaction(@RequestParam("pwd") String pwd, @RequestParam("fname") String fname, @RequestParam("lname") String lname,
			@RequestParam("companyName") String companyName, @RequestParam("addr1") String addr1, @RequestParam("addr2") String addr2,
			@RequestParam("city") String city, @RequestParam("state") String state, @RequestParam("zip") String zip, @RequestParam("phone") String phone,
			@RequestParam("fax") String fax, @RequestParam("email") String email, @RequestParam("ccType") String ccType,
			@RequestParam("nameOnCC") String nameOnCC, @RequestParam("ccNumber") String ccNumber, @RequestParam("ccAddress") String ccAddress,
			@RequestParam("ccExpiryDate") String ccExpiryDate, @RequestParam("shippingCompanyName") String shippingCompanyName,
			@RequestParam("shippingAddress") String shippingAddress, @RequestParam("shippingCity") String shippingCity,
			@RequestParam("shippingState") String shippingState, @RequestParam("shippingZip") String shippingZip,
			@RequestParam("billingCompanyName") String billingCompanyName, @RequestParam("billingAddress") String billingAddress,
			@RequestParam("billingCity") String billingCity, @RequestParam("billingState") String billingState, @RequestParam("billingZip") String billingZip,
			@RequestParam("rateIds") String rateIds, @RequestParam("plAccessId") String plAccessId, @RequestParam("loginId") String loginId,
			@RequestParam("autoEnableFlag") String autoEnableFlag) throws LoginException {

		String apiResponse = null;
		String mailContentSubscriber = null;
		String mailContentInvoice = null;
		float amountCharged = 0;
		float salesTax = 0;
		String statesSubscribed = null;
		String transactionNumber = null;
		String invoiceEmailId =
				"invoice@cdcnews.com,TCummings@cdcnews.com,gcolangelo@cdcnews.com,subhash@creatus.com,johnson@tentsoftware.com,sathya@tentsoftware.com";
		String md5Hash = "md5hash";
		Map<String, Object> map = null;
		Gson gson = null;
		String ccMonthYear = null;
		String invoiceId = null;

		// By default, this sample code is designed to post to our test server for
		// developer accounts: https://test.authorize.net/gateway/transact.dll
		// for real accounts (even in test mode), please make sure that you are
		// posting to: https://secure.authorize.net/gateway/transact.dll
		try {

			// TAX CALCULATION GOES HERE
			// FOR SALES TAX

			salesTax = getTotalB4Tax(rateIds) * getSalesTax(city, state, zip);
			log.info("Sales Tax" + salesTax + "rateIds" + rateIds + city + state + zip);

			// amount total charged
			amountCharged = getTotalB4Tax(rateIds) + (salesTax);
			log.info("amountCharged" + amountCharged);

			ccMonthYear = getMonth(ccExpiryDate) + getYear(ccExpiryDate);

			// invoice id
			invoiceId = getInvoiceId();

			URL post_url = new URL("https://secure.authorize.net/gateway/transact.dll");
			Hashtable<String, String> post_values = new Hashtable<String, String>();
			// the API Login ID and Transaction Key must be replaced with valid values
			post_values.put("x_login", "6aMj6M3f");
			post_values.put("x_tran_key", "63T6K5fq3Qr582j9");
			/*
			 * URL post_url = new URL("https://test.authorize.net/gateway/transact.dll"); Hashtable post_values = new Hashtable(); // the API Login ID and
			 * Transaction Key must be replaced with valid values post_values.put("x_login", "3E2eayC4XgN"); post_values.put("x_tran_key", "2dy8NRK56h8kK2e5");
			 */
			post_values.put("x_version", "3.1");
			post_values.put("x_delim_data", "TRUE");
			post_values.put("x_delim_char", "|");
			post_values.put("x_relay_response", "FALSE");
			post_values.put("x_type", "AUTH_CAPTURE");
			post_values.put("x_method", "CC");
			post_values.put("x_card_num", decryptCc(ccNumber));
			post_values.put("x_card_code", ccAddress);
			post_values.put("x_exp_date", ccMonthYear);
			post_values.put("x_amount", String.valueOf(amountCharged));
			post_values.put("x_description", "CLC Transaction");
			post_values.put("x_first_name", fname);
			post_values.put("x_last_name", lname);
			post_values.put("x_address", ccAddress);
			post_values.put("x_state", state);
			post_values.put("x_zip", zip);
			post_values.put("x_ship_to_company", shippingCompanyName);
			post_values.put("x_ship_to_address", shippingAddress);
			post_values.put("x_ship_to_city", shippingCity);
			post_values.put("x_ship_to_state", shippingState);
			post_values.put("x_ship_to_zip", shippingZip);
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

			String[] responses = responseData.split("\\|");
			apiResponse = responses[2];
			transactionNumber = responses[6];
			gson = new Gson();
			map = new HashMap<String, Object>();
			if (responses[2].equals("1")) {
				apiResponse = "TRANSACTION SUCCESSFFUL";

				// transactionNumber = responses[6];
				// adding subscriber details
				statesSubscribed = EcommUtil.getStateNamesRatesId(rateIds);
				boolean useradded = false;
				useradded =
						addPLSubscriber(pwd, fname, lname, companyName, addr1, addr2, city, state, zip, phone, fax, email, ccType, nameOnCC,
								decryptCc(ccNumber), ccAddress, ccExpiryDate, statesSubscribed, "CLC" + "-" + transactionNumber + "-" + invoiceId + "-"
										+ LibraryFunctions.getTodayDate(), md5Hash, shippingCompanyName, shippingAddress, shippingCity, shippingState,
								shippingZip, billingCompanyName, billingAddress, billingCity, billingState, billingZip, String.valueOf(getTotalB4Tax(rateIds)),
								String.valueOf(salesTax), rateIds, plAccessId, loginId, autoEnableFlag);
				log.info("useradded" + useradded);
				if (useradded == true) {
					//
					mailContentSubscriber =
							mailContentUser(pwd, fname, lname, companyName, addr1, addr2, city, state, zip, phone, fax, email, ccType, nameOnCC,
									decryptCc(ccNumber), ccAddress, statesSubscribed, String.valueOf(amountCharged), "CLC" + "-" + transactionNumber + "-"
											+ invoiceId + "-" + LibraryFunctions.getTodayDate(), String.valueOf(salesTax), rateIds, billingCompanyName,
									billingAddress, billingCity, billingState, billingZip, loginId, shippingCompanyName, shippingAddress, shippingCity,
									shippingState, shippingZip);
					mailContentInvoice =
							mailContentInvoice(pwd, fname, lname, companyName, addr1, addr2, city, state, zip, phone, fax, email, ccType, nameOnCC,
									decryptCc(ccNumber), ccAddress, statesSubscribed, String.valueOf(amountCharged), "CLC" + "-" + transactionNumber + "-"
											+ invoiceId + "-" + LibraryFunctions.getTodayDate(), String.valueOf(salesTax), rateIds, billingCompanyName,
									billingAddress, billingCity, billingState, billingZip, loginId,

									shippingCompanyName, shippingAddress, shippingCity, shippingState, shippingZip);
					// Mail compose goes here-subscriber
					composeMail(mailContentSubscriber, "Constructionleads.com - Online Order Confirmation", email);
					// Mail compose goes here-Invoice
					composeMail(mailContentInvoice, "Constructionleads.com - Online Subscriber Information", invoiceEmailId);

					map.put("Message", apiResponse);
				} else {
					map.put("Message", "user not inserted");
				}
			} else {
				map.put("Message", responses[3]);

				log.info("CLC order message" + responses[3]);
			}

		} catch (Exception e) {
			log.error(e);
			e.printStackTrace();
		}
		return gson.toJson(map);
	}

	// GET SUBCRIBER USER DETAILS
	@RequestMapping(value = "/getSubscriberDetails")
	public String getSubscriberDetails(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey) {
		Map<String, Object> map = null;
		Gson gson = null;
		LeadManagerSessionData lmData = null;
		boolean validSessionId = false;
		Connection con = null;
		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);
			if (validSessionId) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);

				map.put("aaData", EcommUtil.getSubscriberDetails(lmData.getLogin()));
				log.info(gson.toJson(map));
				map.put("aaColumns", "first_name,last_name,address1,address2,city," + "state,zip,phone,fax,email,cc_type,name_on_credit_Card,"
						+ "credit_card_number,credit_card_Address,credit_card_expiry_date,bill_company_name,"
						+ "bill_company_Address,billing_city,billing_state,billing_zip,shippingCompanyName, shippingAddress,"
						+ " shippingCity, shippingState,shippingZip,coverageArea,login_id");

			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Exception in getSubscriberDetails()  " + ex.getMessage());
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);

	}

	// UPDATE SUBCRIBER USER DETAILS
	@RequestMapping(value = "/updatePLSubscriberProfile")
	public String updatePLSubscriberProfile(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey,
			@RequestParam("fname") String fname, @RequestParam("lname") String lname, @RequestParam("companyName") String companyName,
			@RequestParam("addr1") String addr1, @RequestParam("addr2") String addr2, @RequestParam("city") String city, @RequestParam("state") String state,
			@RequestParam("zip") String zip, @RequestParam("phone") String phone, @RequestParam("fax") String fax, @RequestParam("email") String email,
			@RequestParam("billingCompanyName") String billingCompanyName, @RequestParam("billingAddress") String billingAddress,
			@RequestParam("billingCity") String billingCity, @RequestParam("billingState") String billingState, @RequestParam("billingZip") String billingZip,
			@RequestParam("shippingCompanyName") String shippingCompanyName, @RequestParam("shippingAddress") String shippingAddress,
			@RequestParam("shippingCity") String shippingCity, @RequestParam("shippingState") String shippingState,
			@RequestParam("shippingZip") String shippingZip, @RequestParam("ccType") String ccType, @RequestParam("nameOnCC") String nameOnCC,
			@RequestParam("ccNumber") String ccNumber, @RequestParam("ccAddress") String ccAddress, @RequestParam("ccExpiryDate") String ccExpiryDate,
			@RequestParam("autoEnableFlag") String autoEnableFlag) {

		Map<String, Object> map = null;
		Gson gson = null;
		LeadManagerSessionData lmData = null;
		boolean validSessionId = false;
		boolean returnValue = false;
		Connection con = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);
			if (validSessionId) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);

				returnValue =
						EcommUtil.updatePLSubscriberProfile(lmData.getLogin(), fname, lname, companyName, addr1, addr2, city, state, zip, phone, fax, email,
								billingCompanyName, billingAddress, billingCity, billingState, billingZip, shippingCompanyName, shippingAddress, shippingCity,
								shippingState, shippingZip, ccType, nameOnCC, ccNumber, ccAddress, ccExpiryDate, autoEnableFlag);
				if (returnValue) {
					map.put("iTotalRecords", "1");
					map.put("Status", "Success");
					map.put("Message", "");

				} else {
					map.put("iTotalRecords", "0");
					map.put("Status", "Failure");
					map.put("Message", "");
				}

			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Exception in getSubscriberDetails()  " + ex.getMessage());
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);

	}

}
