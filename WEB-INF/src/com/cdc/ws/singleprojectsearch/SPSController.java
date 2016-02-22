/**
 * 
 */
package com.cdc.ws.singleprojectsearch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import wsdatamodel.LeadManagerSessionData;
import wsutils.BasicDataUtil;
import wsutils.JDBCUtil;
import wsutils.LibraryFunctions;
import wsutils.LoginUtil;

import com.google.gson.Gson;

/**
 * @author Selva
 *
 */
@RestController
@RequestMapping(value = "/services/sps")
public class SPSController {

	private Logger log = Logger.getLogger(SPSController.class.getName());

	int size = 0;

	public List<String> getContentIdListSearchByBidDateTitlePL(int sessionId, String securityKey, String cdcid, String title, String stateId, String bidDate,
			int pageNumber, int recordsPerPage, String sortOrder, String sortType, String searchText, Connection con) {

		String sql = null;

		PreparedStatement contentIdStmt = null;
		ResultSet rs = null;
		// String idList = null;
		String userStateIdList = null;
		String userCountyIdList = null;
		String geoUserStateIdList = null;
		String geoUserCountyIdList = null;
		String userStateIds = null;
		String userCountyIds = null;
		String industryList = null;
		String subIndustryList = null;
		// String sectionIdList = null;
		// String subSectionList = null;
		String nationalChainUser = null;
		int startIndex = 0;
		int endIndex = 0;
		ArrayList<String> contentIdList = new ArrayList<String>();
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

				userStateIds = userStateIdList + "," + geoUserStateIdList;
			} else if (userStateIdList != null) {

				userStateIds = userStateIdList;
			} else if (geoUserStateIdList != null) {

				userStateIds = geoUserStateIdList;
			}
			/* User county list */
			if (geoUserCountyIdList != null && userCountyIdList != null) {

				userCountyIds = userCountyIdList + "," + geoUserCountyIdList;
			} else if (userCountyIdList != null) {

				userCountyIds = userCountyIdList;
			} else if (geoUserCountyIdList != null) {

				userCountyIds = geoUserCountyIdList;
			}

			// Content tables query
			sql = "select distinct top 1000 c.id as id,c.bid_date,c.entry_date from content c,pub_section ps ";

			if (!industryList.equals("") || nationalChainUser.equals("Y")) {
				sql += " ,content_industry ci";
			}
			if (nationalChainUser.equals("Y")) {
				sql += " ,content_details cdt";
			}
			sql += " WHERE c.id = c.id";
			// STATE AND COUNTY FILTER
			if (nationalChainUser.equals("N")) {
				sql += " AND state_id in (" + userStateIds + ")";
				if (userCountyIds != null) {
					sql += " AND county_id in (" + userCountyIds + ")";
				}
			}
			// FILTER BASED ON ACTIVATE AND ENTRYDATE

			sql += " AND c.id=ps.content_id AND c.activate = 1 AND entry_date >= convert(varchar,dateadd(day,-90,getdate()),101)";
			// CDCID SEARCH

			if (cdcid != null && !cdcid.equals("")) {
				sql += "AND c.cdc_id like '%" + cdcid + "%'";
			}
			// TITLE-BIDDATE SEARCH
			else {
				if (title != null && !title.equals("")) {
					sql += "AND c.title like '%" + title + "%'";
				}
				if (bidDate != null && !bidDate.equals("")) {
					sql += "AND c.bid_date ='" + bidDate + "'";
				}
				if (stateId != null && !stateId.equals("")) {
					sql += "AND c.state_id ='" + stateId + "'";
				}
			}

			// INDUSTRY LIST
			if (!industryList.equals("")) {
				sql += " AND c.id = ci.content_id";
				sql += " AND ci.industry in ('" + industryList.replaceAll(",", "','") + "')";
			}
			// SUBINDUSTRY LIST

			if (!subIndustryList.equals("")) {

				sql += " AND ci.sub_industry in ('" + subIndustryList.replaceAll(",", "','") + "')";
			}
			// NATIONAL CHAIN USER
			if (nationalChainUser.equals("Y")) {
				sql += " AND c.id=cdt.content_id";
				sql += " AND cdt.national_chain='Y'";
			}
			// Keywords Search if searchText is present.
			String formattedKeywordQuery = null;
			if (!searchText.equals("") && searchText != null) {
				formattedKeywordQuery = sql + LibraryFunctions.getKeywordFormattedQueryString(searchText);
				sql = formattedKeywordQuery;
			}
			// end of keywords search if searchText is present.

			sql += " order by c." + sortOrder + " " + sortType + " , c.entry_date desc ,c.id";
			log.info("" + sql);
			contentIdStmt = con.prepareStatement(sql);

			rs = contentIdStmt.executeQuery();
			while (rs.next()) {

				contentIdList.add(String.valueOf((rs.getInt("id"))));

			} // while.
			contentIdStmt.close();
			size = contentIdList.size();
			startIndex = (pageNumber - 1) * recordsPerPage;
			endIndex = pageNumber * (recordsPerPage - 1);
			endIndex = endIndex + 1;

		}

		catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}

			} catch (SQLException se) {
				log.error("!exception3.2!SQL error in searchByBidDateTitle" + se);

			} finally {
				try {
					if (contentIdStmt != null) {
						contentIdStmt.close();
					}
				} catch (SQLException se) {
					log.error("!exception3.3!SQL error in searchByBidDateTitle" + se);

				}
			}
		}
		if (contentIdList.size() > 0) {
			return contentIdList.subList(startIndex, endIndex);
		} else {
			return null;
		}

	}

	@RequestMapping(value = "/getSingleProjectInfoByBidDateTitlePL")
	public String getSingleProjectInfo(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey,
			@RequestParam("cdcId") String cdcId, @RequestParam("title") String title, @RequestParam("stateId") String stateId,
			@RequestParam("bidDate") String bidDate, @RequestParam("pageNumber") int pageNumber, @RequestParam("recordsPerPage") int recordsPerPage,
			@RequestParam("sortOrder") String sortOrder, @RequestParam("sortType") String sortType, @RequestParam("searchText") String searchText) {

		Map<String, Object> map = null;
		Gson gson = null;

		String contentIds = null;
		Connection con = null;
		try {

			gson = new Gson();
			map = new HashMap<String, Object>();
			boolean validSessionId = false;

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			// log.info(validSessionId);
			if (validSessionId == true) {

				/*
				 * map.put("iTotalRecords", "1"); map.put("Status", "Success"); map.put("Message", "");
				 */
				contentIds =
						LibraryFunctions.ListToString(getContentIdListSearchByBidDateTitlePL(sessionId, securityKey, cdcId, title, stateId, bidDate,
								pageNumber, recordsPerPage, sortOrder, sortType, searchText, con));

				int totRecords = size;

				map.put("sEcho", "1");
				// map.put("iTotalRecords", String.valueOf(recordsPerPage));
				map.put("iTotalRecords", String.valueOf(totRecords));
				map.put("iTotalDisplayRecords", String.valueOf(totRecords));

				map.put("aaData", LoginUtil.getBriefProjectDetailsPL(contentIds, sessionId, sortOrder, sortType, con));

				map.put("aoColumns", "ID,CDC ID,Job Type,Title,Sub Section,Bid Date,Plan Availability Status,,estimated_amount_lower,county,state");

				size = 0;

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

	public String getContentIdListSearchByBidDateTitle(int sessionId, String securityKey, String cdcid, String title, String stateId, String bidDate,
			Connection con) {

		String sql = null;

		PreparedStatement contentIdStmt = null;
		ResultSet rs = null;
		String idList = null;
		String userStateIdList = null;
		String userCountyIdList = null;
		String geoUserStateIdList = null;
		String geoUserCountyIdList = null;
		String userStateIds = null;
		String userCountyIds = null;
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

				userStateIds = userStateIdList + "," + geoUserStateIdList;
			} else if (userStateIdList != null) {

				userStateIds = userStateIdList;
			} else if (geoUserStateIdList != null) {

				userStateIds = geoUserStateIdList;
			}
			/* User county list */
			if (geoUserCountyIdList != null && userCountyIdList != null) {

				userCountyIds = userCountyIdList + "," + geoUserCountyIdList;
			} else if (userCountyIdList != null) {

				userCountyIds = userCountyIdList;
			} else if (geoUserCountyIdList != null) {

				userCountyIds = geoUserCountyIdList;
			}

			if (con == null) {
				con = JDBCUtil.getDBConnectionFromDatabase();
			}

			// Content tables query
			sql = "select distinct top 200 c.id as id,c.bid_date,c.entry_date from content c,pub_section ps ";

			if (!industryList.equals("") || nationalChainUser.equals("Y")) {
				sql += " ,content_industry ci";
			}
			if (nationalChainUser.equals("Y")) {
				sql += " ,content_details cdt";
			}
			sql += " WHERE c.id = c.id";
			// STATE AND COUNTY FILTER
			if (nationalChainUser.equals("N")) {
				sql += " AND state_id in (" + userStateIds + ")";
				if (userCountyIds != null) {
					sql += " AND county_id in (" + userCountyIds + ")";
				}
			}
			// FILTER BASED ON ACTIVATE AND ENTRYDATE

			sql += " AND c.id=ps.content_id AND c.activate = 1 AND entry_date >= convert(varchar,dateadd(day,-90,getdate()),101)";
			// CDCID SEARCH

			if (cdcid != null && !cdcid.equals("")) {
				sql += "AND c.cdc_id like '%" + cdcid + "%'";
			}
			// TITLE-BIDDATE SEARCH
			else {
				if (title != null && !title.equals("")) {
					sql += "AND c.title like '%" + title + "%'";
				}
				if (bidDate != null && !bidDate.equals("")) {
					sql += "AND c.bid_date ='" + bidDate + "'";
				}
				if (stateId != null && !stateId.equals("")) {
					sql += "AND c.state_id ='" + stateId + "'";
				}
			}

			// INDUSTRY LIST
			if (!industryList.equals("")) {
				sql += " AND c.id = ci.content_id";
				sql += " AND ci.industry in ('" + industryList.replaceAll(",", "','") + "')";
			}
			// SUBINDUSTRY LIST

			if (!subIndustryList.equals("")) {

				sql += " AND ci.sub_industry in ('" + subIndustryList.replaceAll(",", "','") + "')";
			}
			// NATIONAL CHAIN USER
			if (nationalChainUser.equals("Y")) {
				sql += " AND c.id=cdt.content_id";
				sql += " AND cdt.national_chain='Y'";
			}

			sql += " order by c.bid_date asc , c.entry_date desc";
			// log.info("" + sql);
			contentIdStmt = con.prepareStatement(sql);

			rs = contentIdStmt.executeQuery();
			while (rs.next()) {
				if (idList == null) {
					idList = String.valueOf(rs.getInt("id"));
				} else {
					idList = idList + "," + String.valueOf(rs.getInt("id"));
				}
			}

		}

		catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}

			} catch (SQLException se) {
				log.error("!exception3.2!SQL error in searchByBidDateTitle" + se);

			} finally {
				try {
					if (contentIdStmt != null) {
						contentIdStmt.close();
					}
				} catch (SQLException se) {
					log.error("!exception3.3!SQL error in searchByBidDateTitle" + se);

				}
			}

		}
		return idList;

	}

	@RequestMapping(value = "/getSingleProjectInfoByBidDateTitle")
	public String getSingleProjectInfo(int sessionId, String securityKey, String cdcId, String title, String stateId, String bidDate) {
		Map<String, Object> map = null;
		Gson gson = null;

		String contentIds = null;
		Connection con = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();
			boolean validSessionId = false;
			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			// log.info(validSessionId);
			if (validSessionId == true) {

				/*
				 * map.put("iTotalRecords", "1"); map.put("Status", "Success"); map.put("Message", "");
				 */
				contentIds = getContentIdListSearchByBidDateTitle(sessionId, securityKey, cdcId, title, stateId, bidDate, con);

				map.put("aaData", LoginUtil.getBriefProjectDetails(contentIds, sessionId, con));

				map.put("aoColumns", "ID,CDC ID,Job Type,Title,Sub Section,Bid Date,Plan Availability Status,,estimated_amount_lower,county,state");

			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Exception in getContentDetails() for Single Project search " + ex.getMessage());
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}
		return gson.toJson(map);

	}

	@RequestMapping(value = "/getSubscriberStateList")
	public String getSubscriberStateList(int sessionId, String securityKey) {
		Map<String, Object> map = null;
		Gson gson = null;

		String userStateIdList = null;
		String geoUserStateIdList = null;
		String userStateIds = null;
		Connection con = null;
		try {

			gson = new Gson();
			map = new HashMap<String, Object>();
			boolean validSessionId = false;

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			if (validSessionId == true) {

				LeadManagerSessionData lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);
				// USER SUBSCRIBED STATE LIST
				userStateIdList = lmData.getUserStateList();
				geoUserStateIdList = lmData.getGeoUserStateList();
				/* User state list */
				if (geoUserStateIdList != null && userStateIdList != null) {

					userStateIds = userStateIdList + "," + geoUserStateIdList;
				} else if (userStateIdList != null) {

					userStateIds = userStateIdList;
				} else if (geoUserStateIdList != null) {

					userStateIds = geoUserStateIdList;
				}

				map.put("Data", BasicDataUtil.getBasicData(12, userStateIds));

			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Exception in getSubscriberStateList() for SingleProject Search  " + ex.getMessage());
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);

	}

}
