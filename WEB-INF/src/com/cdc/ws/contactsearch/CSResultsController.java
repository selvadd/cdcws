/**
 * 
 */
package com.cdc.ws.contactsearch;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import wscontactsearch.WSContactSearch;
import wsdatamodel.ContactData;
import wsdatamodel.LeadManagerSessionData;
import wsexception.LoginException;
import wsutils.ContactSearchUtil;
import wsutils.EJBClient;
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
public class CSResultsController {

	private Logger log = Logger.getLogger(CSResultsController.class.getName());

	private String userStateIds = null;
	private String userCountyIds = null;
	private String loginId = null;

	private WSContactSearch getUserContactSearchBean() {
		WSContactSearch wsContactSearch = null;
		try {
			wsContactSearch = EJBClient.getWSContactSearchBean();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return wsContactSearch;
	}

	// GET CONTACT ID LIST
	@RequestMapping(value = "/getContactSearchIdList")
	public String getContactSearchIdList(@RequestParam("typeId") int typeId, @RequestParam("nchainUser") String nchainUser,
			@RequestParam("industryList") String industryList, @RequestParam("stateList") String stateList, @RequestParam("countyList") String countyList,
			@RequestParam("sectionList") String sectionList, @RequestParam("bidStageList") String bidStageList,
			@RequestParam("companyName") String companyName, @RequestParam("contactTypeId") int contactTypeId,
			@RequestParam("contactTypeIdSub") int contactTypeIdSub, @RequestParam("contactStateId") int contactStateId) {

		Map<String, Object> map = null;
		Gson gson = null;
		String contactIds = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			// GETTING CONTACTIDLIST
			ArrayList contactIdList =
					ContactSearchUtil.getContactIDs(typeId, nchainUser, industryList, stateList, countyList, sectionList, bidStageList, companyName,
							contactTypeId, contactTypeIdSub, contactStateId);

			for (int i = 0; i < contactIdList.size(); i++) {
				if (contactIds == null) {
					contactIds = (String) contactIdList.get(i);
				} else {
					contactIds = contactIds + "," + (String) contactIdList.get(i);
				}

			}

			map.put("contactIdList", contactIds);

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("EXCEPTION IN getContactSearchIdList " + ex.toString());
		}
		return gson.toJson(map);
	}

	// GET CONTACT SEARCH COUNT
	@RequestMapping(value = "/getContactSearchCount")
	public String getContactSearchCount(@RequestParam("typeId") int typeId, @RequestParam("nchainUser") String nchainUser,
			@RequestParam("industryList") String industryList, @RequestParam("stateList") String stateList, @RequestParam("countyList") String countyList,
			@RequestParam("sectionList") String sectionList, @RequestParam("bidStageList") String bidStageList,
			@RequestParam("companyName") String companyName, @RequestParam("contactTypeId") int contactTypeId,
			@RequestParam("contactTypeIdSub") int contactTypeIdSub, @RequestParam("contactStateId") int contactStateId) {

		Map<String, Object> map = null;
		Gson gson = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			int contactCount =
					ContactSearchUtil.getContactCount(typeId, nchainUser, industryList, stateList, countyList, sectionList, bidStageList, companyName,
							contactTypeId, contactTypeIdSub, contactStateId);

			map.put("contactsearchcount", String.valueOf(contactCount));

		} catch (Exception ex) {
			log.error("EXCEPTION IN getContactSearchCount " + ex.toString());
			ex.printStackTrace();
		}
		return gson.toJson(map);
	}

	/**
	 * Returns contact info when company name is provided
	 * 
	 * @param companyName
	 * @return
	 */
	@RequestMapping(value = "/getContactByCompanyName")
	public String getContactByCompanyName(@RequestParam("sessionId") int sessionID, @RequestParam("securityKey") String securityKey,
			@RequestParam("companyName") String companyName, @RequestParam("stateId") int stateId) {

		ArrayList contactList = null;
		Map<String, Object> map = null;
		Gson gson = null;
		Connection con = null;
		WSContactSearch wsContactSearch = null;

		try {
			gson = new Gson();
			map = new HashMap<String, Object>();
			boolean validSessionId = false;

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);

			if (validSessionId == true) {

				wsContactSearch = getUserContactSearchBean();
				contactList = wsContactSearch.getContactByCompanyName(companyName, stateId);
				map.put("iTotalRecords", "1");
				map.put("Status", "Success");
				map.put("Message", "");
				map.put("aaData", contactList);

				// map.put("aoColumns","ID,CDC ID,Job Type,Title,Sub Section,Bid Date,Plan Availability Status,,estimated_amount_lower,county,state");

			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		// jsonArr = jsonArr.fromObject(contactList);
		return gson.toJson(map);
		// return "HEEELLLOOO";
	}

	@RequestMapping(value = "/getContactByContactType")
	public String getContactByContactType(@RequestParam("contactTypeId") int contactTypeId, @RequestParam("stateId") int stateId) {
		log.info("Inside getContactByContactType:contactTypeId= " + contactTypeId);
		ArrayList contactList = null;
		Gson gson = null;
		WSContactSearch wsContactSearch = null;

		try {
			gson = new Gson();
			wsContactSearch = getUserContactSearchBean();
			contactList = wsContactSearch.getContactByContactType(contactTypeId, stateId);
			log.info("Size of contactList = " + contactList.size());
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return gson.toJson(contactList);
		// return "HEEELLLOOO";
	}

	@RequestMapping(value = "/getContactSearchInfo")
	public String getContactSearchInfo(@RequestParam("contactIdList") String contactIdList) {

		Map<String, Object> map = null;
		Gson gson = null;
		String companyNames = null;
		String contactIds = null;
		WSContactSearch wsContactSearch = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			wsContactSearch = getUserContactSearchBean();

			// GETTING CONTACT DETAILS
			// ArrayList contactSearchInfoList = ContactSearchUtil.getContactInfo(contactIdList);
			ArrayList contactSearchInfoList = wsContactSearch.getContactInfo(contactIdList);
			// JSONArray jsonArray = JSONArray.fromObject(contactSearchInfoList);
			// log.info("jsonArray"+jsonArray.toString());

			ContactData cData = null;
			Iterator itr = contactSearchInfoList.iterator();
			// log.info("I m here");

			while (itr.hasNext()) {

				cData = (ContactData) itr.next();

				if (companyNames == null) {
					companyNames = cData.getCompanyName().trim();
				} else {
					companyNames = companyNames + "," + cData.getCompanyName().trim();

				}

				if (contactIds == null) {
					contactIds = String.valueOf(cData.getContactId());
				} else {
					contactIds = contactIds + "," + String.valueOf(cData.getContactId());

				}

			}

			map.put("companyname", companyNames);
			map.put("contactids", contactIds);

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("EXCEPTION IN getContactSearchInfo " + ex.toString());
		}
		/**/
		return gson.toJson(map);
	}

	/* METHOD TO VIEW PROJECTS BASED ON THE CONTACT CLICKED */
	public ArrayList getContactSearchViewProjects(int sessionId, String securityKey, int contactId, Connection con) throws LoginException {

		String sql;
		Statement stmt = null;

		ResultSet rs = null;
		// String contentIdList = null;
		String userStateIdList = null;
		String userCountyIdList = null;
		String geoUserStateIdList = null;
		String geoUserCountyIdList = null;
		String industryList = null;
		String subIndustryList = null;
		// String sectionIdList = null;
		// String subSectionList = null;
		String nationalChainUser = null;
		ArrayList briefInfoList = new ArrayList();

		try {

			LeadManagerSessionData lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);

			loginId = lmData.getLogin();
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

			sql = "select distinct ct.id as id,ct.cdc_id,ct.title,ct.sub_section from content ct,pub_section ps,content_contact cc ";

			if (!industryList.equals("") || nationalChainUser.equals("Y")) {
				sql += " ,content_industry ci";
			}
			if (nationalChainUser.equals("Y")) {
				sql += " ,content_details cdt";
			}
			sql += " WHERE ct.id = ct.id";

			// STATE AND COUNTY FILTER
			if (nationalChainUser.equals("N")) {
				sql += " AND state_id in (" + userStateIds + ")";
				if (userCountyIds != null) {
					sql += " AND county_id in (" + userCountyIds + ")";
				}
			}
			// FILTER BASED ON ACTIVATE AND ENTRYDATE

			sql += " AND ct.id=cc.content_id and ct.id=ps.content_id AND ct.activate = 1 AND entry_date>=dateadd(day,-90,getdate())";

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

			sql += " and cc.contact_id=" + contactId;

			stmt = con.createStatement();

			rs = stmt.executeQuery(sql);

			ArrayList<String> dataInfo = null;
			while (rs.next()) {
				dataInfo = new ArrayList<String>();
				dataInfo.add(String.valueOf(rs.getInt("id")));
				dataInfo.add(rs.getString("cdc_id"));
				dataInfo.add(rs.getString("title"));
				dataInfo.add(rs.getString("sub_section"));
				briefInfoList.add(dataInfo);

			}

		}

		catch (SQLException sqle) {

			log.error("SQLException occurred in " + "getting getContactSearchViewProjects Error:" + sqle.getMessage());

		} catch (Exception e) {
			log.error("Exception occurred in " + "getting getContactSearchViewProjects  Error:" + e.getMessage());

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (stmt != null) {
					stmt.close();
				}

			} catch (Exception sqel) {
				log.error("SQLException occurred in " + "releasing DB in getContactSearchViewProjects() Error:" + sqel.getMessage());
			}

		}

		return briefInfoList;

	} // end of getContactSearchViewProjects()

	private String getDateMax(String userStateList, Connection con) {

		ResultSet rs = null;
		CallableStatement cstmt = null;
		String dateMax = null;

		try {

			cstmt = con.prepareCall("{call SP_GET_DATEMAX(?)}");
			cstmt.setString(1, userStateList);
			rs = cstmt.executeQuery();

			while (rs.next()) {
				dateMax = rs.getString("DATEMAX");
			}
		} catch (SQLException se) {
			log.error("SQLException occurred in webservice while getting Date Max from content table :" + se);
		} catch (Exception ex) {
			log.error("Exception occurred in webservice while getting Date Max from content table :" + ex);
		}

		return dateMax;
	}

	private ArrayList getProjectSummaryByContactId(int contactId, String loginId, String userStateIdList, String userCountyIdList, String dateMax,
			Connection con) {

		ResultSet rs = null;
		CallableStatement cstmt = null;
		ArrayList pubProjectSummaryList = null;
		ArrayList<String> list = null;

		try {
			pubProjectSummaryList = new ArrayList();

			cstmt = con.prepareCall("{call SP_PUB_PROJECTSUMMARY_BYCONTACTID(?,?,?,?,?)}");
			cstmt.setInt(1, contactId);
			cstmt.setString(2, loginId);
			cstmt.setString(3, userStateIdList);
			cstmt.setString(4, userCountyIdList);
			cstmt.setString(5, dateMax);

			rs = cstmt.executeQuery();

			while (rs.next()) {
				list = new ArrayList<String>();
				list.add(rs.getString("publication_name"));
				list.add(rs.getString("number_of_projects"));

				pubProjectSummaryList.add(list);
			}

		} catch (SQLException se) {
			log.error("SQLException occurred in webservice while getting publication project summary by contactId :" + se);
		} catch (Exception ex) {
			log.error("Exception occurred in webservice while getting publication project summary by contactId :" + ex);
		}

		return pubProjectSummaryList;
	}

	@RequestMapping(value = "/getProjectsByContactId")
	public String getProjectsByContactId(@RequestParam("sessionId") int sessionID, @RequestParam("securityKey") String securityKey,
			@RequestParam("contactId") int contactId) {

		Map<String, Object> map = null;
		Gson gson = null;
		Connection con = null;
		try {
			gson = new Gson();
			map = new HashMap<String, Object>();
			boolean validSessionId = false;

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);

			if (validSessionId == true) {

				map.put("iTotalRecords", "1");
				map.put("Status", "Success");
				map.put("Message", "");
				map.put("aaData", getContactSearchViewProjects(sessionID, securityKey, contactId, con));
				// To debug
				ArrayList list = getContactSearchViewAddedToProjects(sessionID, securityKey, 3, contactId, con);

				String dateMax = getDateMax(userStateIds, con);

				// 818511
				map.put("aaProjectSummary", getProjectSummaryByContactId(contactId, loginId, userStateIds, userCountyIds, dateMax, con));

			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

		} catch (Exception ex) {
			log.error("Exception occurred in webservice while getting Projects by ContactId : " + ex);
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}
		return gson.toJson(map);

	}

	/**
	 * [web method]
	 * 
	 * @param sessionID
	 * @param key
	 * @param contactId
	 * @return
	 */
	@RequestMapping(value = "/getAddedToProjectsByContactId")
	public String getAddedToProjectsByContactId(@RequestParam("sessionId") int sessionID, @RequestParam("securityKey") String securityKey,
			@RequestParam("contactId") int contactId) {

		Map<String, Object> map = null;
		Gson gson = null;
		Connection con = null;
		try {
			gson = new Gson();
			map = new HashMap<String, Object>();
			boolean validSessionId = false;

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);

			if (validSessionId == true) {

				int typeId = 3;

				map.put("iTotalRecords", "1");
				map.put("Status", "Success");
				map.put("Message", "");
				map.put("aaData", getContactSearchViewAddedToProjects(sessionID, securityKey, typeId, contactId, con));

				String dateMax = getDateMax(userStateIds, con);

				// 818511
				map.put("aaProjectSummary", getProjectSummaryByContactId(contactId, loginId, userStateIds, userCountyIds, dateMax, con));

			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

		} catch (Exception ex) {
			log.error("Exception occurred in webservice while getting Projects by ContactId : " + ex);
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}
		return gson.toJson(map);

	}

	/* END OF GETCONTACTSEARCHVIEWPROJECT METHOD AND ITS JSON OUTPUT METHOD */

	/* METHOD TO VIEW ADDED-TO-PROJECTS BASED ON THE CONTACT CLICKED. LM MOBILE 12/12/14 */
	private ArrayList getContactSearchViewAddedToProjects(int sessionId, String securityKey, int typeId, int contactId, Connection con) throws LoginException {

		String sql;
		CallableStatement cstmt = null;
		Statement stmt = null;

		ResultSet rs = null;
		// String contentIdList = null;
		String userStateIdList = null;
		String userCountyIdList = null;
		String geoUserStateIdList = null;
		String geoUserCountyIdList = null;
		String industryList = null;
		String subIndustryList = null;
		// String sectionIdList = null;
		// String subSectionList = null;
		String nationalChainUser = null;
		String lastEntry = null;
		ArrayList briefInfoList = new ArrayList();

		try {

			LeadManagerSessionData lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);

			loginId = lmData.getLogin();
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

			lastEntry = LoginUtil.getLastLogin(lmData.getLogin(), con);
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

			log.info("getContactSearchViewAddedToProjects " + loginId);
			sql = "select distinct ct.id as id,ct.cdc_id,ct.title,ct.sub_section from content ct,pub_section ps,content_contact cc ";

			if (!industryList.equals("") || nationalChainUser.equals("Y")) {
				sql += " ,content_industry ci";
			}
			if (nationalChainUser.equals("Y")) {
				sql += " ,content_details cdt";
			}
			sql += " WHERE ct.id = ct.id";

			// STATE AND COUNTY FILTER
			if (nationalChainUser.equals("N")) {
				sql += " AND state_id in (" + userStateIds + ")";
				if (userCountyIds != null) {
					sql += " AND county_id in (" + userCountyIds + ")";
				}
			}
			// FILTER BASED ON ACTIVATE AND ENTRYDATE

			sql += " AND ct.id=cc.content_id and ct.id=ps.content_id AND ct.activate = 1 AND entry_date>=dateadd(day,-90,getdate())";

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

			sql += " and cc.contact_id=" + contactId;

			sql += " AND	cc.added_date >='" + lastEntry + "'";

			log.info("SQL: " + sql);

			stmt = con.createStatement();
			rs = stmt.executeQuery(sql);
			/*
			 * cstmt = con.prepareCall("{call SP_CONTACTSEARCH(?,?,?,?,?,?,?,?,?,?,?,?,?)}");
			 * 
			 * cstmt.setInt(1, typeId); cstmt.setString(2, nationalChainUser); cstmt.setString(3, industryList); cstmt.setString(4, userStateIds);
			 * cstmt.setString(5, userCountyIds); cstmt.setString(6, sectionIdList); cstmt.setString(7, subSectionList); cstmt.setString(8, null); //
			 * companyname cstmt.setInt(9, 0); // contactTypeId cstmt.setInt(10, 0); // contactTypesubId cstmt.setInt(11, 0); // contactstateId cstmt.setInt(12,
			 * contactId); // contactId cstmt.setString(13, lastEntry); // use last entry
			 * 
			 * rs = cstmt.executeQuery();
			 */
			ArrayList<String> dataInfo = null;
			while (rs.next()) {
				dataInfo = new ArrayList<String>();
				dataInfo.add(String.valueOf(rs.getInt("id")));
				dataInfo.add(rs.getString("cdc_id"));
				dataInfo.add(rs.getString("title"));
				dataInfo.add(rs.getString("sub_section"));
				briefInfoList.add(dataInfo);

			}

		}

		catch (SQLException sqle) {

			log.error("SQLException occurred in " + "getting getContactSearchViewAddedToProjects Error:" + sqle.getMessage());

		} catch (Exception e) {
			log.error("Exception occurred in " + "getting getContactSearchViewAddedToProjects  Error:" + e.getMessage());

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (cstmt != null) {
					cstmt.close();
				}

			} catch (Exception sqel) {
				log.error("SQLException occurred in " + "releasing DB in getContactSearchViewAddedToProjects() Error:" + sqel.getMessage());
			}

		}

		return briefInfoList;

	} // end of getContactSearchViewProjects()

	/**
	 * THIS METHOD RETURNS PROFILE INFO BASED ON THE CONTACT ID WHEN VIEW PROFILE LINK IS CLICKED -- Added few extra info for LM Mobile. by Muthu on 10/10/14
	 * 
	 * @param contactId
	 * @return
	 * @throws LoginException
	 */
	public ArrayList<ContactData> getContactSearchProfileInfo(int contactId, Connection con) throws LoginException {

		ResultSet rs = null;
		CallableStatement cstmt = null;
		ArrayList<ContactData> contactSearchProfileInfoList = null;
		ContactData contactData = null;

		try {
			contactSearchProfileInfoList = new ArrayList<ContactData>();

			cstmt = con.prepareCall("{call SP_Contacts_byContactId(?)}");
			cstmt.setInt(1, contactId);

			rs = cstmt.executeQuery();

			while (rs.next()) {
				contactData = new ContactData();
				contactData.setContactId(rs.getInt("contact_id"));
				contactData.setCompanyName(rs.getString("company_name"));
				contactData.setAddress1(rs.getString("Address1"));
				contactData.setCity(rs.getString("city"));
				contactData.setStateName(rs.getString("state_name"));
				contactData.setZip(rs.getString("zip"));
				contactData.setTelephone1(rs.getString("telephone1"));
				contactData.setFax1(rs.getString("fax1"));

				// Added for LM Mobile by Muthu on 10/10/14
				contactData.setCountyName(rs.getString("county_name"));
				contactData.setTelephone2(rs.getString("telephone2"));
				contactData.setFax2(rs.getString("fax2"));
				contactData.setEmail1(rs.getString("email1"));
				contactData.setEmail2(rs.getString("email2"));
				contactData.setContactType(rs.getString("contacttype"));
				contactData.setContactSubType(rs.getString("contactsubtype"));

				contactSearchProfileInfoList.add(contactData);
			}

		} catch (SQLException sqlEx) {
			log.error("SQLException occurred in web service while getting contact profile : " + sqlEx);
		} catch (Exception ex) {
			log.error("Exception occurred in web service while getting contact profile : " + ex);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}

			} catch (SQLException se) {
				log.error("!exception3.2!SQL error in getContactSearchProfileInfo" + se);

			} finally {
				try {
					if (cstmt != null) {
						cstmt.close();
					}
				} catch (SQLException se) {
					log.error("!exception3.3!SQL error in getContactSearchProfileInfo" + se);

				}
			}
		}
		return contactSearchProfileInfoList;

	}

	// getContactSearchProfileInfo
	@RequestMapping(value = "/getContactProfile")
	public String getContactProfile(@RequestParam("sessionId") int sessionID, @RequestParam("securityKey") String securityKey,
			@RequestParam("contactId") int contactId) {

		Map<String, Object> map = null;
		Gson gson = null;
		LeadManagerSessionData lmData = null;

		Connection con = null;

		try {
			gson = new Gson();
			map = new HashMap<String, Object>();
			boolean validSessionId = false;

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);

			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionID, con);
				String currentPage = "View Contact Detail";
				// Start Web Usage Feed Script
				WebUsageUtil webUsage = null;
				webUsage = new WebUsageUtil();
				webUsage.webUsageFeed(lmData.getLogin(), currentPage, null, "");
				// End Web Usage Feed Script

				map.put("iTotalRecords", "1");
				map.put("Status", "Success");
				map.put("Message", "");
				map.put("aaData", getContactSearchProfileInfo(contactId, con));

			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);

	}

}
