/**
 * 
 */
package com.cdc.ws.contacttracker;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import wsdatamodel.LeadManagerSessionData;
import wsexception.LoginException;
import wsutils.JDBCUtil;
import wsutils.LoginUtil;
import wsutils.WebUsageUtil;

import com.google.gson.Gson;

/**
 * @author Selva
 *
 */

@RestController
@RequestMapping(value = "/services")
public class ContactTrackerController {

	private Logger log = Logger.getLogger(ContactTrackerController.class.getName());

	/**
	 * [Web Service] To get the list of contacts trakced by an user.
	 * 
	 * @param typeId
	 * @param loginId
	 * @return
	 */
	@RequestMapping(value = "/getTrackedContacts")
	public String wsgetTrackedContacts(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey,
			@RequestParam("typeId") int typeId) {

		Map<String, Object> map = null;
		Gson gson = null;
		ArrayList ctList = null;
		LeadManagerSessionData lmData = null;
		String lastEntry = null;
		Connection con = null;

		try {
			gson = new Gson();
			map = new HashMap<String, Object>();
			ctList = new ArrayList();
			boolean validSessionId = false;

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);
				lastEntry = LoginUtil.getLastLogin(lmData.getLogin(), con);

				String currentPage = "Contact Tracker";
				if (typeId == 2)
					currentPage = "Updated Tracked Contacts";
				// Start Web Usage Feed Script
				WebUsageUtil webUsage = null;
				webUsage = new WebUsageUtil();
				webUsage.webUsageFeed(lmData.getLogin(), currentPage, null, "");
				// End Web Usage Feed Script

				ctList = getTrackedContactsByLoginId(typeId, lmData.getLogin(), lastEntry, con);
				if (ctList != null && ctList.size() > 0) {
					map.put("iTotalRecords", String.valueOf(ctList.size()));
					map.put("aaData", ctList);
				} else {
					map.put("iTotalRecords", "0");
					map.put("aaData", "");
				}
			} else {
				map.put("iTotalRecords", "0");
				map.put("aaData", "");
				map.put("message", "Invalid Login");
			}
		} catch (Exception ex) {
			log.error("Exception occurred while getting Tracked Contacts List : " + ex);
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}
		return gson.toJson(map);
	}

	/**
	 * This function gets the list of contacts trakced by an user.
	 * 
	 * @param typeId
	 *            ------ 1-CT list,2-Updated CT list
	 * @param loginId
	 * @param lastEntry
	 * @throws LoginException
	 */
	public ArrayList getTrackedContactsByLoginId(int typeId, String loginId, String lastEntry, Connection con) throws LoginException {

		ArrayList contactsList = null;
		Map ctMap = null;

		ResultSet rs = null;
		CallableStatement cstmt = null;

		try {
			contactsList = new ArrayList();

			cstmt = con.prepareCall("{call SSP_CONTACT_TRACKER(?,?,?)}");
			// TYPE ID BASED ON WHICH EDIT/INSERT/DELE FOR PT
			cstmt.setInt(1, typeId);

			// LOGIN ID
			cstmt.setString(2, loginId);

			// Last login
			cstmt.setString(3, lastEntry);

			rs = cstmt.executeQuery();
			while (rs.next()) {
				ctMap = new HashMap();
				ctMap.put("contact_id", String.valueOf(rs.getInt("contact_id")));
				ctMap.put("company_name", rs.getString("company_name"));

				contactsList.add(ctMap);
			}
		} catch (SQLException se) {
			log.error("SQLException occurred in ContactTrakcer : " + se);
		} catch (Exception ex) {
			log.error("Exception occurred in ContactTrakcer : " + ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (cstmt != null)
					cstmt.close();

			} catch (Exception ex) {
				log.error("Exception 3.1 while releasing connections at ContactTracker Web Service : " + ex);
			}

		}

		return contactsList;
	}

}
