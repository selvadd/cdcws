/**
 * 
 */
package com.cdc.ws.publicsearch;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.mail.Transport;
import javax.mail.internet.InternetAddress;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import wscontent.WSContent;
import wsexception.LoginException;
import wsutils.EJBClient;
import wsutils.EcommUtil;
import wsutils.JDBCUtil;
import wsutils.LibraryFunctions;

import com.google.gson.Gson;

/**
 * @author Selva
 *
 */

@RestController
@RequestMapping(value = "/services/publicsearch")
public class PublicSearchController {

	private static Logger log = Logger.getLogger(PublicSearchController.class.getName());
	int size = 0;

	// int totRecords=0;
	// Search Results for Private Label -constructionleads.com-Without Login

	/**
	 * Initialize WSContent
	 */
	private WSContent initwsContent() {
		WSContent wsContentEJB = null;
		try {
			wsContentEJB = EJBClient.WSContentEJBean();
		} catch (Exception ex) {
			log.warn("Exception while Initializing wsContent : " + ex);
		}
		return wsContentEJB;
	}

	public List<String> getSearchResultsNoLogin(int stateId, int pageNumber, int recordsPerPage, String sortOrder, String sortType) throws LoginException {

		String sql;
		Statement stmt = null;
		Connection con = null;
		ResultSet rs = null;

		int startIndex = 0;
		int endIndex = 0;

		ArrayList<String> contentIdList = new ArrayList<String>();

		try {

			sql = "select distinct id,bid_date,entry_date from content ct,pub_section ps ";

			sql += " WHERE ct.id = ct.id AND ct.state_id=" + stateId + "  AND  entry_date >= dateadd(day,-90,getdate())";

			// FILTER BASED ON ACTIVATE AND ENTRYDATE

			sql += " AND sub_section!='PLANNING NEWS' AND ct.id=ps.content_id AND ct.activate = 1 ";

			sql += " order by ct." + sortType + " " + sortOrder + " , ct.entry_date desc ,ct.id";
			// log.info("" + sql);
			if (con == null) {
				con = JDBCUtil.getDBConnectionFromDatabase();
			}

			stmt = con.createStatement();

			stmt = con.createStatement();
			rs = stmt.executeQuery(sql);

			// log.info(""+sql);

			while (rs.next()) {

				contentIdList.add(String.valueOf((rs.getInt("id"))));

			} // while.
			stmt.close();
			size = contentIdList.size();
			// log.info(size);
			// log.info(sql);
			startIndex = (pageNumber - 1) * recordsPerPage;
			endIndex = (pageNumber * recordsPerPage) - 1;

			endIndex = endIndex + 1;

			if (endIndex > contentIdList.size()) {
				endIndex = contentIdList.size();

			}

		}

		catch (SQLException sqle) {

			log.error("SQLException occurred in " + "getting getSearchResultsNoLogin Error:" + sqle.getMessage());

		} catch (Exception e) {
			log.error("Exception occurred in " + "getting getSearchResultsNoLogin for projectId Id Error:" + e.getMessage());

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (stmt != null) {
					stmt.close();
				}

			} catch (Exception sqel) {
				log.error("SQLException occurred in " + "releasing DB in getSearchResultsNoLogin() Error:" + sqel.getMessage());
			}

		}
		if (contentIdList.size() > 0) {
			return contentIdList.subList(startIndex, endIndex);
		} else {
			return null;
		}

	} // end of getSearchResultsNoLogin()

	// Brief mode details for Private Label -construction leads-Without Login
	public static ArrayList getBriefProjectDetailsNoLogin(String contentIds, String sortOrder, String sortType) {

		String sql;
		Statement stmt = null;
		Connection con = null;
		ResultSet rs = null;
		// String strCountyId = null;

		// int stateId = 0;
		int contentId = 0;
		String bidDate = null;
		String formattedBidDate = null;
		String subSection = null;

		ArrayList projectDetailsInfo = new ArrayList();
		try {

			sql =
					"select id,cdc_id,bid_date,title,sub_section," + " county.county_name as county_name, "
							+ " state.state_abb as state_abb,const_new,const_alt,const_add,const_ren" + " from content,state,county,content_details"
							+ " where id in(" + contentIds + ") and  sub_section!='PLANNING NEWS' AND content.state_id=state.state_id "
							+ " and content.county_id=county.county_id and content.id=content_details.content_id" + " order by " + sortType + " " + sortOrder
							+ ",entry_date desc,id";

			if (con == null) {
				con = JDBCUtil.getDBConnectionFromDatabase();
			}

			stmt = con.createStatement();

			rs = stmt.executeQuery(sql);

			ArrayList projectDetailsInfoList = null;

			while (rs.next()) {
				projectDetailsInfoList = new ArrayList();
				// CONTENT ID
				projectDetailsInfoList.add(String.valueOf(rs.getInt("id")));
				contentId = rs.getInt("id");
				// CDC ID
				projectDetailsInfoList.add(rs.getString("cdc_id"));

				// SUBSECTION
				subSection = rs.getString("sub_section");

				// BID DATE
				bidDate = rs.getString("bid_date");
				formattedBidDate = LibraryFunctions.getDateStringBidDate(LibraryFunctions.getDateStringMMDDYY(bidDate));

				projectDetailsInfoList.add(formattedBidDate);
				// TITLE
				projectDetailsInfoList.add(rs.getString("title"));
				// SUB SECTION
				projectDetailsInfoList.add(subSection);
				// COUNTY NAME
				projectDetailsInfoList.add(rs.getString("county_name"));
				// STATE ABB
				projectDetailsInfoList.add(rs.getString("state_abb"));

				// CONSTRUCTION TYPE
				projectDetailsInfoList.add(rs.getString("const_new"));
				projectDetailsInfoList.add(rs.getString("const_alt"));
				projectDetailsInfoList.add(rs.getString("const_add"));
				projectDetailsInfoList.add(rs.getString("const_ren"));

				// GETINDUSTRY
				projectDetailsInfoList.add(EcommUtil.getIndustry(contentId));

				/* End of Plans avail URL set */

				projectDetailsInfo.add(projectDetailsInfoList);
			}

		}

		catch (SQLException sqle) {

			log.error("SQLException occurred in " + "getting getBriefProjectDetailsNoLogin Error:" + sqle.getMessage());

		} catch (Exception e) {
			log.error("Exception occurred in " + "getting getBriefProjectDetailsNoLogin Error:" + e.getMessage());

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (stmt != null) {
					stmt.close();
				}

			} catch (Exception sqel) {
				log.error("SQLException occurred in " + "releasing DB in getBriefProjectDetailsNoLogin() Error:" + sqel.getMessage());
			}

		}

		return projectDetailsInfo;

	} // end of getBriefProjectDetailsNoLogin()

	// GET CONTENT DETAILS FOR PUBLIC SEARCH
	@RequestMapping(value = "/getContentDetailsPL")
	public String getContentDetailsPL(@RequestParam("stateId") int stateId, @RequestParam("pageNumber") int pageNumber,
			@RequestParam("recordsPerPage") int recordsPerPage, @RequestParam("sortOrder") String sortOrder, @RequestParam("sortType") String sortType) {

		Map<String, Object> map = null;
		Gson gson = null;

		String contentIds = null;

		try {
			gson = new Gson();
			map = new HashMap<String, Object>();

			// int totRecords = 0;
			// int totDisplayRecords = 0;

			contentIds = LibraryFunctions.ListToString(getSearchResultsNoLogin(stateId, pageNumber, recordsPerPage, sortOrder, sortType));

			// totRecords = size;
			ArrayList publicResultList = null;
			publicResultList = getBriefProjectDetailsNoLogin(contentIds, sortOrder, sortType);
			// totDisplayRecords = publicResultList.size();

			map.put("sEcho", "1");
			map.put("iTotalRecords", String.valueOf(size));
			map.put("iTotalDisplayRecords", String.valueOf(size));
			map.put("aoColumns", "ID,CDC ID,Bid Date,Title,Sub Section,county,state,const_new,const_alt,const_add,const_ren,industry/subindustry");
			map.put("aaData", publicResultList);

			size = 0;

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Exception in getContentDetails() for public search " + ex.getMessage());
		}

		return gson.toJson(map);

	}

	// GET ALL STATES
	@RequestMapping(value = "/getAllStatesPL")
	public String getAllStatesPL() {
		Map<String, Object> map = null;
		Gson gson = null;

		try {
			gson = new Gson();
			map = new HashMap<String, Object>();

			map.put("aaData", EcommUtil.getAllStatesPL());
			log.info("PUBLIC SEARCH STATES ALL " + gson.toJson(map));

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Exception in getAllStatesPL() for public search " + ex.getMessage());
		}

		return gson.toJson(map);

	}

	// New function to get the response data for public search without security key/session id
	public ArrayList getContentDetails(String ids) {

		ArrayList contentList = new ArrayList();

		try {
			WSContent wsContentEJB = initwsContent();

			contentList = wsContentEJB.getContentList(ids);

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Exception in getContentDetails() " + ex.getMessage());
		}

		return contentList;

	}

	// getPublicSearchProjectDetails
	@RequestMapping(value = "/getPublicSearchProjectDetails")
	public String getPublicSearchProjectDetails(@RequestParam("stateId") int stateId, @RequestParam("pageNumber") int pageNumber,
			@RequestParam("recordsPerPage") int recordsPerPage, @RequestParam("sortOrder") String sortOrder, @RequestParam("sortType") String sortType) {

		Map<String, Object> map = null;
		Gson gson = null;
		String contentIds = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			contentIds = LibraryFunctions.ListToString(getSearchResultsNoLogin(stateId, pageNumber, recordsPerPage, sortOrder, sortType));

			map.put("aaData", getContentDetails(contentIds));

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Exception in getPublicSearchProjectDetails()" + ex.getMessage());
		}

		return gson.toJson(map);

	}

	// forgotPassword-PL
	public static boolean forgotPassword(String loginId) throws LoginException {
		CallableStatement cstmt = null;
		ResultSet rs = null;
		Connection con = null;
		// String sql = null;
		String fname = null;
		String pwd = null;
		String email = null;
		// String subscribertype = null;
		String shipto = null;
		boolean mailSent = false;

		String userMailContent = null;

		try {

			if (con == null) {
				con = JDBCUtil.getDBConnectionFromDatabase();
			}
			cstmt = con.prepareCall("{call ssp_pl_forgot_password(?)}");

			cstmt.setString(1, loginId);

			rs = cstmt.executeQuery();
			// ArrayList dataList;
			while (rs.next()) {

				pwd = rs.getString("password");
				email = rs.getString("email");
				fname = rs.getString("first_name");
				// subscribertype = rs.getString("subscriber_type");
				shipto = rs.getString("shipto_number");
				userMailContent =
						"<table cellspacing=0 cellpadding=5 border=0 style=font-size: small; font-family: MS Sans Serif;>" + "<tbody>" + "<tr>"
								+ "<td colspan=1>&nbsp;&nbsp;Dear&nbsp;&nbsp;"
								+ fname
								+ ",</td>"
								+ "<td>&nbsp;</td>"
								+ "</tr>"
								+ "<tr>"
								+ "<td colspan=1>&nbsp;&nbsp;Here is the password you requested </td>"
								+ "<td>&nbsp;</td>"
								+ "</tr>"
								+ "<tr>"
								+ "<td colspan=1>&nbsp;&nbsp;Password &nbsp;&nbsp;         :&nbsp;&nbsp;"
								+ pwd
								+ " </td>"
								+ "<td>&nbsp;</td>"
								+ "</tr>"
								+ "<tr>"
								+ "<td colspan=1>&nbsp;&nbsp;Your Login id is &nbsp;&nbsp; :&nbsp;&nbsp;"
								+ loginId
								+ ", &nbsp;&nbsp;your customer number is &nbsp;&nbsp;"
								+ shipto
								+ "</td>"
								+ "</tr>"
								+ "<tr>"
								+ "<td colspan=4>&nbsp;&nbsp;If you would like to see additional territories call your sales associate at 800-890-9543. </td>"
								+ "<td>&nbsp;</td>"
								+ "</tr>"
								+ "<tr>"
								+ "<td colspan=1>&nbsp;&nbsp;Thank you for using ConstructionLeads.com. </td>"
								+ "<td>&nbsp;</td>"
								+ "</tr>"
								+ "<tr>"
								+ "<td colspan=1>&nbsp;&nbsp;Sincerely,</td>"
								+ "<td>&nbsp;</td>"
								+ "</tr>"
								+ "<tr>"
								+ "<td colspan=1>&nbsp;&nbsp;ConstructionLeads. </td>" + "<td>&nbsp;</td>" + "</tr>" + "</tbody>" + "</table>";

				// mail to client-user:
				forgotPasswordClientEmail(userMailContent, "Password Request", email);
				mailSent = true;

			}

		} catch (Exception e) {

			// e.printStackTrace();
			mailSent = false;
			log.error("ERROR IN FORGOT PASSWORD" + e.toString());

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
				mailSent = false;
				log.error("SQLERROR IN FORGOT PASSWORD" + sqle.toString());

			}

		}

		return mailSent;
	} // End of forgotPassword()

	/**
	 * forgotPasswordClientEmail -
	 */
	public static void forgotPasswordClientEmail(String txtmessage, String msgSubject, String mailTo) {

		// StringBuffer textmessage = new StringBuffer();

		//
		try {

			/* MAIL CONNECTION AND COMPOSITION PART GOES HERE */

			// SUBSTITUTE YOUR EMAIL ADDRESSES HERE!!!
			String to = mailTo;

			String from = "postmaster@constructionleads.com";
			// SUBSTITUTE YOUR ISP'S MAIL SERVER HERE!!!

			/* The below line is commented for not using any external providers...remove if needed */
			// String host = "smtp.bizmail.yahoo.com";

			String host = "192.168.22.68";
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

	@RequestMapping(value = "/forgotPasswordPL")
	public String forgotPasswordPL(@RequestParam("loginId") String loginId) {

		Map<String, Object> map = null;
		Gson gson = null;
		boolean mailSent = false;

		try {
			gson = new Gson();
			map = new HashMap<String, Object>();
			mailSent = forgotPassword(loginId);

			if (mailSent == true) {

				map.put("Message", "Success");

			} else {

				map.put("Message", "Mail Not Sent");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Exception in forgotPasswordPL() for public search " + ex.getMessage());
		}

		return gson.toJson(map);

	}

}
