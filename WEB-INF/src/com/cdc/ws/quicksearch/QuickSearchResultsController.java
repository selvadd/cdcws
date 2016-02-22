package com.cdc.ws.quicksearch;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import wsdatamodel.*;
import wsutils.*;

import com.google.gson.Gson;

/**
 * @author Selva
 *
 */

@RestController
@RequestMapping(value = "/services")
public class QuickSearchResultsController {

	private Logger log = Logger.getLogger(QuickSearchResultsController.class.getName());

	/**
	 * 
	 * @param sessionId
	 * @param securityKey
	 * @param stateIds
	 * @param sectionIdList
	 * @param newUpdatedFlag
	 * @param subSectionList
	 * @param constructionTypes
	 * @param divisionIdList
	 * @param showAll
	 * @param displayMode
	 * @param con
	 * @return
	 */
	public String getQuickSearchContentIds(int sessionId, String securityKey, String stateIds, String sectionIdList, int newUpdatedFlag, String subSectionList,
			String constructionTypes, String divisionIdList, int showAll, String displayMode, Connection con) {
		ResultSet rs = null;
		Statement stmt = null;
		String sql = null;
		// String userStateIdList = null;
		String userCountyIdList = null;
		// String geoUserStateIdList = null;
		String geoUserCountyIdList = null;
		// String userStateIds = null;
		String userCountyIds = null;
		// String nationalChainUser = null;
		String contentIdList = null;

		try {

			LeadManagerSessionData lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);
			// USER SUBSCRIBED STATE AND COUNTY LIST
			// userStateIdList = lmData.getUserStateList();
			userCountyIdList = lmData.getUserCountyList();
			// geoUserStateIdList = lmData.getGeoUserStateList();
			geoUserCountyIdList = lmData.getGeoUserCountyList();

			/* User state list */
			/*
			 * if (geoUserStateIdList != null && userStateIdList != null) { userStateIds = userStateIdList + "," + geoUserStateIdList; } else if
			 * (userStateIdList != null) { userStateIds = userStateIdList; } else if (geoUserStateIdList != null) { userStateIds = geoUserStateIdList; }
			 */

			/* User county list */
			if (geoUserCountyIdList != null && userCountyIdList != null) {
				userCountyIds = userCountyIdList + "," + geoUserCountyIdList;
			} else if (userCountyIdList != null) {
				userCountyIds = userCountyIdList;
			} else if (geoUserCountyIdList != null) {
				userCountyIds = geoUserCountyIdList;
			}

			// SHOW ALL
			if (showAll == 1) {
				sql =
						"select distinct c.id,c.cdc_id,c.publication_id,c.section_id,c.sub_section,"
								+ " c.title,c.state_id,c.estimated_amount_lower,c.bid_date,c.prebid_mtg,c.bids_details, "
								+ " c.plan_express,c.entry_date,c.short_cdcid,c.state_multiple,c.county_multiple,"
								+ " c.plan_availability_status,c.leads_id,c.county_id,gc.county_name,s.state_abb "
								+ " from content c,pub_section ps,state s,county gc";
			} else {
				sql =
						"select distinct top 200 c.id,c.cdc_id,c.publication_id,c.section_id,c.sub_section,"
								+ " c.title,c.state_id,c.estimated_amount_lower,c.bid_date,c.prebid_mtg,c.bids_details, "
								+ " c.plan_express,c.entry_date,c.short_cdcid,c.state_multiple,c.county_multiple,"
								+ " c.plan_availability_status,c.leads_id,c.county_id,gc.county_name,s.state_abb "
								+ " from content c,pub_section ps,state s,county gc";

			}
			// CONTENT_DIVISIONS
			if (divisionIdList != null && divisionIdList.equals("") != true) {

				sql += ",content_divisions_2004 cd";

			}
			/*
			 * if (nationalChainUser != null && nationalChainUser.equals("Y") == true) { sql += ",content_details cdt"; }
			 */

			sql += " where c.activate =1 and c.state_id=s.state_id and c.id=ps.content_id  and c.county_id=gc.county_id ";
			// NEW/UPDATED FLAG
			if (newUpdatedFlag == 1) {
				sql += "AND c.new_updated = 'N'";

			}

			sql += "AND entry_date >= convert(varchar,dateadd(day,-90,getdate()),101)";

			// stateIds

			if (stateIds != null && !stateIds.trim().equals("")) {

				sql += "AND s.state_id in (" + stateIds + ")";

			}

			/* FOR FILTER BASED ON COUNTIES SUBSCRIBED BY USER */

			if (userCountyIds != null && !userCountyIds.trim().equals("")) {

				sql += "AND c.county_id in (" + userCountyIds + ")";

			}

			/* NATIONAL CHAIN FLAG CHECK */
			/*
			 * if (nationalChainUser != null && nationalChainUser.equals("Y") == true) { sql += "and c.id=cdt.content_id and cdt.national_chain='Y'"; }
			 */

			/**** Section ******/

			if (sectionIdList != null && !sectionIdList.equals("")) {

				sql += "and ps.section_id in (" + sectionIdList + ")";

			}

			/**** Sub Section ******/

			if (subSectionList != null && !subSectionList.equals("")) {

				sql += "and sub_section in ('" + subSectionList.replaceAll(",", "','") + "')";

			}
			/* Divisions */
			if (divisionIdList != null && !(divisionIdList.equals(""))) {

				sql +=
						"   AND  c.id=cd.content_id   AND 	(cd.division_2004_id in (SELECT node.division_2004_id FROM  "
								+ " divisions_2004 AS node, divisions_2004 AS parent  WHERE node.lft BETWEEN parent.lft AND "
								+ "  parent.rgt AND parent.division_2004_id in (" + divisionIdList + ")))";

			}

			// CONSTRUCTION TYPE
			// String constT = null;

			String cons[] = null;
			if (constructionTypes != null && constructionTypes.equals("") != true) {
				cons = constructionTypes.split("\\,");

				for (int x = 0; x < cons.length; x++) {

					if (x == 0) {
						sql += "and (";
					} else {
						sql += " or ";
					}
					sql += " (c.const_" + cons[x] + "='Y'";

					sql += ")";

				}
				sql += ")";
			}

			sql += " order by c.bid_date asc , c.entry_date desc";

			log.info("quick search query: " + sql);

			stmt = con.createStatement();
			rs = stmt.executeQuery(sql);

			while (rs.next()) {
				if (contentIdList == null) {
					contentIdList = String.valueOf(rs.getInt("id"));
				} else {
					contentIdList = contentIdList + "," + String.valueOf(rs.getInt("id"));
				}

			} // while.
			stmt.close();
		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}

			} catch (SQLException se) {
				log.error("!exception3.2!SQL error in getContentIds" + se);

			} finally {
				try {
					if (stmt != null) {
						stmt.close();
					}
				} catch (SQLException se) {
					log.error("!exception3.3!SQL error in getContentIds" + se);

				}
			}
		}
		return contentIdList;
	} //

	/**
	 * /online-product/quick-search/search-results
	 * 
	 * @param sessionId
	 * @param securityKey
	 * @param stateIds
	 * @param sectionIdList
	 * @param newUpdatedFlag
	 * @param subSectionList
	 * @param constructionTypes
	 * @param divisionIdList
	 * @param showAll
	 * @param displayMode
	 * @return
	 */
	@RequestMapping(value = "/quickSearchResults")
	public String getContentDetails(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey,
			@RequestParam("stateIds") String stateIds, @RequestParam("sectionIdList") String sectionIdList, @RequestParam("newUpdatedFlag") int newUpdatedFlag,
			@RequestParam("subSectionList") String subSectionList, @RequestParam("constructionTypes") String constructionTypes,
			@RequestParam("divisionIdList") String divisionIdList, @RequestParam("showAll") int showAll, @RequestParam("displayMode") String displayMode) {

		Map<String, Object> map = null;
		Gson gson = null;

		Connection con = null;
		String contentIds = null;

		try {
			gson = new Gson();
			map = new HashMap<String, Object>();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			boolean validSessionId = false;
			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			log.info(validSessionId);
			if (validSessionId == true) {

				/*
				 * map.put("iTotalRecords", "1"); map.put("Status", "Success"); map.put("Message", "");
				 */
				contentIds =
						getQuickSearchContentIds(sessionId, securityKey, stateIds, sectionIdList, newUpdatedFlag, subSectionList, constructionTypes,
								divisionIdList, showAll, displayMode, con);

				log.info("QUICK SEARCH CONTENT IDS: " + contentIds);

				map.put("aaData", LoginUtil.getBriefProjectDetails(contentIds, sessionId, con));

				// ArrayList projectDetailsTitles = new ArrayList();
				// projectDetailsTitles.add("sTitle");
				// map.put("aoColumns",projectDetailsTitles);

			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

		} catch (Exception ex) {
			log.error("Exception in getContentDetails() for quick search " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);

	}

}