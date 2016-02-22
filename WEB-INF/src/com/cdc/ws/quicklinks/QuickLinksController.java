/**
 * 
 */
package com.cdc.ws.quicklinks;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import wsdatamodel.LeadManagerSessionData;
import wsutils.JDBCUtil;
import wsutils.LoginUtil;

import com.google.gson.Gson;

/**
 * @author Selva
 *
 */

@RestController
@RequestMapping(value = "/services/quicklinks")
public class QuickLinksController {

	private Logger log = Logger.getLogger(QuickLinksController.class.getName());

	public String getSubSectionContentIdList(int sessionId, String securityKey, String classCode, String subSection, Connection con) {

		String sql;
		Statement stmt = null;

		ResultSet rs = null;
		String contentIdList = null;
		String userStateIdList = null;
		String userCountyIdList = null;
		String geoUserStateIdList = null;
		String geoUserCountyIdList = null;
		String UserStateids = null;
		String UserCountyids = null;
		String industryList = null;
		String subIndustryList = null;
		// String sectionIdList = null;
		// String subSectionList = null;
		String nationalChainUser = null;
		try {
			LeadManagerSessionData lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);
			// USER SUBSCRIBED STATE AND COUNTY LIST
			userStateIdList = lmData.getUserStateList();
			userCountyIdList = lmData.getUserCountyList();
			geoUserStateIdList = lmData.getGeoUserStateList();
			geoUserCountyIdList = lmData.getGeoUserCountyList();
			nationalChainUser = lmData.getNationalFlag();
			// INDUSTRY LIST SUBSCRIBED BY USER
			industryList = LoginUtil.getUserSubscribedIndustryList(lmData.getLogin(), con);
			// SUBINDUSTRY LIST SUBSCRIBED BY USER
			subIndustryList = LoginUtil.getUserSubscribedSubIndustryList(lmData.getLogin(), con);
			// SUBSECTION LIST SUBSCRIBED BY USER
			// sectionIdList = LoginUtil.getUserSubscribedSectionIds(lmData.getLogin(), con);
			// SECTIONID LIST SUBSCRIBED BY USER
			// subSectionList = LoginUtil.getUserSubscribedSubSectionIds(lmData.getLogin(), con);

			/* User state list */
			if (geoUserStateIdList != null && userStateIdList != null) {

				UserStateids = userStateIdList + "," + geoUserStateIdList;
			} else if (userStateIdList != null) {

				UserStateids = userStateIdList;
			} else if (geoUserStateIdList != null) {

				UserStateids = geoUserStateIdList;
			}
			/* User county list */
			if (geoUserCountyIdList != null && userCountyIdList != null) {

				UserCountyids = userCountyIdList + "," + geoUserCountyIdList;
			} else if (userCountyIdList != null) {

				UserCountyids = userCountyIdList;
			} else if (geoUserCountyIdList != null) {

				UserCountyids = geoUserCountyIdList;
			}

			sql = "select top 200 ct.id as id from content ct,pub_section ps ";

			if (!industryList.equals("") || nationalChainUser.equals("Y")) {
				sql += " ,content_industry ci";
			}
			if (nationalChainUser.equals("Y")) {
				sql += " ,content_details cdt";
			}
			sql += " WHERE ct.id = ct.id";

			// STATE AND COUNTY FILTER
			if (nationalChainUser.equals("N")) {
				sql += " AND state_id in (" + UserStateids + ")";
				if (UserCountyids != null) {
					sql += " AND county_id in (" + UserCountyids + ")";
				}
			}
			// FILTER BASED ON ACTIVATE AND ENTRYDATE

			sql += " AND ct.id=ps.content_id AND ct.activate = 1 AND entry_date>=dateadd(day,-90,getdate())";

			// CLASSCODE AND SUBSECTION

			sql += " AND ps.classcode ='" + classCode + "' AND ct.sub_section='" + subSection + "'";

			// INDUSTRY LIST
			if (!industryList.equals("")) {
				sql += " AND ct.id = ci.content_id";
				sql += " AND ci.industry in ('" + industryList.replaceAll(",", "','") + "')";
			}
			// SUBINDUSTRY LIST

			if (!subIndustryList.equals("")) {

				sql += " AND ci.sub_industry in ('" + subIndustryList.replaceAll(",", "','") + "')";
			}
			// NATIONAL CHAIN USER
			if (nationalChainUser.equals("Y")) {
				sql += " AND ct.id=cdt.content_id";
				sql += " AND cdt.national_chain='Y'";
			}

			stmt = con.createStatement();

			rs = stmt.executeQuery(sql);

			while (rs.next()) {

				if (contentIdList == null) {
					contentIdList = String.valueOf(rs.getInt("id"));
				} else {
					contentIdList = contentIdList + "," + String.valueOf(rs.getInt("id"));
				}

			}

		}

		catch (SQLException sqle) {

			log.error("SQLException occurred in " + "getting getSubSectionSearchInfoList Error:" + sqle.getMessage());

		} catch (Exception e) {
			log.error("Exception occurred in " + "getting getSubSectionSearchInfoList for projectId Id Error:" + e.getMessage());

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (stmt != null) {
					stmt.close();
				}

			} catch (Exception sqel) {
				log.error("SQLException occurred in " + "releasing DB in getSubSectionSearchInfoList() Error:" + sqel.getMessage());
			}

		}

		return contentIdList;

	} // end of getSubSectionSearchInfoList()

	@RequestMapping(value = "/getContentDetails")
	public String getContentDetails(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey,
			@RequestParam("classCode") String classCode, @RequestParam("subSection") String subSection) {
		Map<String, Object> map = null;
		Gson gson = null;

		String ContentIds = null;

		Connection con = null;
		try {

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			gson = new Gson();
			map = new HashMap<String, Object>();

			boolean validSessionId = false;

			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			if (validSessionId == true) {

				/*
				 * map.put("iTotalRecords", "1"); map.put("Status", "Success"); map.put("Message", "");
				 */

				ContentIds = getSubSectionContentIdList(sessionId, securityKey, classCode, subSection, con);

				map.put("aaData", LoginUtil.getBriefProjectDetails(ContentIds, sessionId, con));

				/*
				 * map.put("aoColumns", "ID,CDC ID,Job Type,Title,Sub Section,Bid Date,Plan Availability Status,,estimated_amount_lower,county,state");
				 */

			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Exception in getContentDetails() for Subsection search " + ex.getMessage());
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);

	}

}
