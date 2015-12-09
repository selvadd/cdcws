package com.cdc.ws.savedsearch;

//import org.json.simple.JSONObject;
//import org.json.simple.JSONArray;
import wsdatamodel.SavedSearchData;
import wsdatamodel.LeadManagerSessionData;
import wsutils.EJBClient;
import wsutils.JDBCUtil;

import java.sql.*;

import com.google.gson.Gson;

import wssavedsearch.*;
import wsutils.LoginUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/services")
public class SavedSearchController {

	private Logger log = Logger.getLogger(SavedSearchController.class.getName());

	/**
	 * Creating Remote Interface for accessing WSSavedSearchBean methods
	 */
	private WSSavedSearch getUserSavedSearchBean() {
		WSSavedSearch savedSearch = null;
		try {
			savedSearch = EJBClient.WSSavedSearchBean();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return savedSearch;
	}

	// GET SAVED SEARCH NAMES FOR A LOGIN ID
	@RequestMapping(value="/savedSearchList")
	public String getSavedSearches(@RequestParam("sessionId") int sessionID, @RequestParam("securityKey") String securityKey) {

		Map<String, Object> map = null;
		Gson gson = null;
		Connection con = null;
		boolean validSessionId = false;
		LeadManagerSessionData lmData = null;
		WSSavedSearch savedSearch = null;
		
		try {
			gson = new Gson();
			map = new HashMap<String, Object>();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			savedSearch = getUserSavedSearchBean();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);
			// log.info(validSessionId);
			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionID, con);
				
				map.put("iTotalRecords", "1");
				map.put("Status", "Success");
				map.put("Message", "");				 
				map.put("aaData", savedSearch.getSavedSearchInfo(1, lmData.getLogin()));

			} // end of if for valid session check

			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");
			}

			// log.info("getSavedSearches OUTPUT: " +gson.toJson(map));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}
		return gson.toJson(map);
	}

	// GET SAVED SEARCH INFO FOR A LOGIN.BY SEARCH_NAME
	public ArrayList<SavedSearchData> getSavedSearchInfobyName(String loginId, String SearchName, Connection con) {

		SavedSearchData ssData = null;
		ResultSet rs = null;
		CallableStatement cstmt = null;
		ArrayList<SavedSearchData> savedSearchList = new ArrayList<SavedSearchData>();

		try {

			cstmt = con.prepareCall("{call SSP_GETSAVEDSEARCHBYNAME(?,?)}");

			cstmt.setString(1, loginId);

			cstmt.setString(2, SearchName);

			rs = cstmt.executeQuery();

			while (rs.next()) {
				ssData = new SavedSearchData();
				ssData.setLoginId(loginId);
				ssData.setSearchName(SearchName);
				ssData.setSearchField(rs.getString("search_field"));
				ssData.setSearchValue(rs.getString("search_value"));
				savedSearchList.add(ssData);

			} // while.
			cstmt.close();
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
				log.error("!exception3.2! SQL error in getSavedSearchbyName " + se);

			} finally {
				try {
					if (cstmt != null) {
						cstmt.close();
					}
				} catch (SQLException se) {
					log.error("!exception3.3! SQL error in getSavedSearchbyName " + se);

				}
			}
		}
		return savedSearchList;
	} // getSavedSearchbyName

	// GET SAVED SEARCH INFO FOR A LOGIN ID BY SEARCH NAME
	@RequestMapping(value="/savedSearchInfo")
	public String getSavedSearchInfoBySearchName(@RequestParam("sessionId") int sessionID, @RequestParam("securityKey") String securityKey, @RequestParam("savedSearchName") String savedSearchName) {

		Map<String, Object> map = null;
		Gson gson = null;
		Connection con = null;

		boolean validSessionId = false;
		LeadManagerSessionData lmData = null;
		
		try {
			gson = new Gson();
			map = new HashMap<String, Object>();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);

			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionID, con);
				
				map.put("iTotalRecords", "1");
				map.put("Status", "Success");
				map.put("Message", "");
				map.put("aaData", getSavedSearchInfobyName(lmData.getLogin(), savedSearchName, con));

			} // end of if for valid session check

			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

			// log.info("getSavedSearches OUTPUT: " +gson.toJson(map));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);
	}

	// UPDATE/DELETE SAVED SEARCH INFO

	/*
	 * typeId- 1 for update 2 for delete
	 */
	@RequestMapping(value="/updateDeleteSavedSearch")
	public String updateDeleteSavedSearch(int sessionID, String securityKey, int typeId, String searchName, String oldSearchName) {
		// log.info("Inside updateDeleteSavedSearch:loginId= " );
		Map<String, Object> map = null;
		Gson gson = null;
		Connection con = null;
		boolean validSessionId = false;
		boolean updateDeleteflag = false;
		LeadManagerSessionData lmData = null;
		WSSavedSearch savedSearch = null;
		
		try {
			gson = new Gson();
			map = new HashMap<String, Object>();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			savedSearch = getUserSavedSearchBean();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);

			// log.info(validSessionId);
			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionID, con);
				updateDeleteflag = savedSearch.updateDeleteSavedSearchInfo(typeId, lmData.getLogin(), searchName, oldSearchName);

				if (updateDeleteflag == true) {

					map.put("iTotalRecords", "1");
					map.put("Status", "Success");
					map.put("Message", "");
				} else {
					map.put("iTotalRecords", "0");
					map.put("Status", "Failure");
					map.put("Message", "");
				}

			} // end of if for valid session check

			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

			// log.info("updateDeleteSavedSearch OUTPUT: " +gson.toJson(map));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);
	}// END OF updateDeleteSavedSearch
	
	/**
	 * Gets the saved search name from the provided parameters by accessing the saved search bean and updates the info on emailhotlist table
	 *
	 * @param savedSearchName
	 * @param freq
	 * @param periodValue
	 * @param notes
	 * @param emailTo
	 * @param enabledFlag
	 * @return
	 * @throws RemoteException
	 * @throws LoginException
	 */
	@RequestMapping(value="/updateEmailReminder")
	public String updateSavedSearchEmailReminder(int sessionID, String securityKey, String savedSearchName, String freq, String periodValue, String notes,
			String emailTo, String enabledFlag) {
		
		// log.info("Inside updateSavedSearchEmailReminder:loginId= " );
		Map<String, Object> map = null;
		Gson gson = null;
		boolean returnValue = false;
		boolean validSessionId = false;
		LeadManagerSessionData lmData = null;
		Connection con = null;
		WSSavedSearch savedSearch = null;
		
		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			savedSearch = getUserSavedSearchBean();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);
			// log.info(validSessionId);
			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionID, con);
				returnValue =
						savedSearch
								.updateSavedSearchEmailReminder(lmData.getLogin(), savedSearchName, freq.charAt(0), periodValue, notes, emailTo, enabledFlag);
				if (returnValue) {
					map.put("iTotalRecords", "1");
					map.put("Status", "Success");
					map.put("Message", "");

				} else {
					map.put("iTotalRecords", "0");
					map.put("Status", "Failure");
					map.put("Message", "");
				}
			} // end of if for valid session check

			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

			// log.info("updateSavedSearchEmailReminder OUTPUT: " +gson.toJson(map));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}
		return gson.toJson(map);
	}

	/**
	 *
	 * @param sessionID
	 * @param securityKey
	 * @param savedSearchId
	 * @param states
	 * @param counties
	 * @param noOfdays
	 * @param jobs
	 * @param sections
	 * @param stages
	 * @param constructionTypes
	 * @param bidTypes
	 * @param Divisions
	 * @param projKeywords
	 * @param contactsKeyword
	 * @param planningStages
	 * @param contractingMethods
	 * @param indTypes
	 * @param valueMin
	 * @param valueMax
	 * @param unitOfMsrmt
	 * @param unitOfMsrmtMin
	 * @param unitOfMsrmtMax
	 * @param bidDateFrom
	 * @param bidDateTo
	 * @param showAll
	 * @param displayMode
	 * @return
	 */
	@RequestMapping(value="/updateSavedSearch")	
	public String updateSavedSearchCriteria(int sessionID, String securityKey, String savedSearchName, String states, String counties, int noOfdays,
			String jobs, String sections, String stages, String constructionTypes, String bidTypes, String divisions, String projKeywords,
			String contactsKeyword, String planningStages, String contractingMethods, String indTypes, String valueMin, String valueMax, String unitOfMsrmt,
			String unitOfMsrmtMin, String unitOfMsrmtMax, String bidDateFrom, String bidDateTo, String showAll, String displayMode) {
		
		// log.info("Inside updateSavedSearchCriteria:loginId= ");
		Map<String, Object> map = null;
		Gson gson = null;
		boolean returnValue = false;
		boolean validSessionId = false;
		LeadManagerSessionData lmData = null;
		Connection con = null;
		WSSavedSearch savedSearch = null;
		
		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			savedSearch = getUserSavedSearchBean();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);
			// log.info(validSessionId);
			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionID, con);
				returnValue =
						savedSearch.updateSavedSearchCriteria(lmData.getLogin(), savedSearchName, states, counties, noOfdays, jobs, sections, stages,
								constructionTypes, bidTypes, divisions, projKeywords, contactsKeyword, planningStages, contractingMethods, indTypes, valueMin,
								valueMax, unitOfMsrmt, unitOfMsrmtMin, unitOfMsrmtMax, bidDateFrom, bidDateTo, showAll, displayMode);
				if (returnValue) {
					map.put("iTotalRecords", "1");
					map.put("Status", "Success");
					map.put("Message", "");

				} else {
					map.put("iTotalRecords", "0");
					map.put("Status", "Failure");
					map.put("Message", "");
				}
			} // end of if for valid session check

			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

			// log.info("updateSavedSearchCriteria OUTPUT: " +gson.toJson(map));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);

	}

	/**
	 *
	 * @param sessionID
	 * @param securityKey
	 * @param savedSearchId
	 * @param states
	 * @param counties
	 * @param noOfdays
	 * @param jobs
	 * @param sections
	 * @param stages
	 * @param constructionTypes
	 * @param bidTypes
	 * @param Divisions
	 * @param projKeywords
	 * @param contactsKeyword
	 * @param planningStages
	 * @param contractingMethods
	 * @param indTypes
	 * @param valueMin
	 * @param valueMax
	 * @param unitOfMsrmt
	 * @param unitOfMsrmtMin
	 * @param unitOfMsrmtMax
	 * @param bidDateFrom
	 * @param bidDateTo
	 * @param showAll
	 * @param displayMode
	 * @return
	 */
	@RequestMapping(value="/insertSavedSearch")
	public String insertSavedSearchCriteria(int sessionID, String securityKey, String savedSearchName, String states, String counties, int noOfdays,
			String jobs, String sections, String stages, String constructionTypes, String bidTypes, String divisions, String projKeywords,
			String contactsKeyword, String planningStages, String contractingMethods, String indTypes, String valueMin, String valueMax, String unitOfMsrmt,
			String unitOfMsrmtMin, String unitOfMsrmtMax, String bidDateFrom, String bidDateTo, String showAll, String displayMode) {
		
		// log.info("Inside insertSavedSearchCriteria:loginId= ");
		Map<String, Object> map = null;
		Gson gson = null;
		boolean returnValue = false;
		boolean validSessionId = false;
		LeadManagerSessionData lmData = null;
		Connection con = null;
		WSSavedSearch savedSearch = null;
		
		try {
			gson = new Gson();
			map = new HashMap<String, Object>();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			savedSearch = getUserSavedSearchBean();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);
			// log.info(validSessionId);
			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionID, con);
				returnValue =
						savedSearch.insertSavedSearchCriteria(lmData.getLogin(), savedSearchName, states, counties, noOfdays, jobs, sections, stages,
								constructionTypes, bidTypes, divisions, projKeywords, contactsKeyword, planningStages, contractingMethods, indTypes, valueMin,
								valueMax, unitOfMsrmt, unitOfMsrmtMin, unitOfMsrmtMax, bidDateFrom, bidDateTo, showAll, displayMode);
				if (returnValue) {
					map.put("iTotalRecords", "1");
					map.put("Status", "Success");
					map.put("Message", "");

				} else {
					map.put("iTotalRecords", "0");
					map.put("Status", "Failure");
					map.put("Message", "");
				}
			} // end of if for valid session check

			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

			// log.info("insertSavedSearchCriteria OUTPUT: " +gson.toJson(map));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);

	}

	/**
	 ****************** COPYING A SAVED SEARCH **************************
	 * 
	 * @param loginId
	 * @param savedSearchId
	 * @param copyToUser
	 * @return
	 */
	@RequestMapping(value="/copySavedSearch")
	public String copySavedSearch(int sessionID, String securityKey, String savedSearchName, String copyToUser) {
		// log.info("Inside copySavedSearch:loginId= ");
		Map<String, Object> map = null;
		Gson gson = null;
		boolean returnValue = false;
		boolean validSessionId = false;
		LeadManagerSessionData lmData = null;
		Connection con = null;
		WSSavedSearch savedSearch = null;
		
		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			savedSearch = getUserSavedSearchBean();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);
			// log.info(validSessionId);
			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionID, con);
				returnValue = savedSearch.copySavedSearch(lmData.getLogin(), savedSearchName, copyToUser);
				if (returnValue) {
					map.put("iTotalRecords", "1");
					map.put("Status", "Success");
					map.put("Message", "");

				} else {
					map.put("iTotalRecords", "0");
					map.put("Status", "Failure");
					map.put("Message", "");
				}
			} // end of if for valid session check

			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}
			// log.info("copySavedSearch OUTPUT: " + gson.toJson(map));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);

	}

	/**
	 * SAVE NEW SEARCH
	 * 
	 * @param loginId
	 * @param searchName
	 * @param emailOption
	 * @param ifEveryWeek
	 * @param ifEveryMonth
	 * @param specificDate
	 * @param sendResultTo
	 * @param notes
	 * @param stateIds
	 * @param countyIds
	 * @param days
	 * @param jobs
	 * @param sections
	 * @param stages
	 * @param constructionTypes
	 * @param bidTypes
	 * @param Div
	 * @param projKeyword
	 * @param contactsKeyword
	 * @param planningStages
	 * @param constrMethods
	 * @param indTypes
	 * @param valueMin
	 * @param valueMax
	 * @param unitOfMeasure
	 * @param unitOfMeasureMin
	 * @param unitOfMeasureMax
	 * @param bidDateFrom
	 * @param bidDateTo
	 * @param showAll
	 * @param disMode
	 * @return
	 */
	@RequestMapping(value="/saveSearch")
	public String saveNewSearch(int sessionID, String securityKey, String searchName, String emailOption, String ifEveryWeek, String ifEveryMonth,
			String specificDate, String sendResultTo, String notes, String stateIds, String countyIds, int days, String jobs, String sections, String stages,
			String constructionTypes, String bidTypes, String Div, String projKeyword, String contactsKeyword, String planningStages, String constrMethods,
			String indTypes, String subIndTypes, String valueMin, String valueMax, String unitOfMeasure, String unitOfMeasureMin, String unitOfMeasureMax,
			String bidDateFrom, String bidDateTo, String showAll, String disMode) {

		Map<String, Object> map = null;
		Gson gson = null;
		boolean validSessionId = false;
		boolean returnValue = false;
		LeadManagerSessionData lmData = null;
		Connection con = null;
		WSSavedSearch savedSearch = null;
		
		try {
			gson = new Gson();
			map = new HashMap<String, Object>();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			savedSearch = getUserSavedSearchBean();
			
			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);

			if (validSessionId) {
				lmData = LoginUtil.getLeadManagerSessionDetails(sessionID, con);
				returnValue =
						savedSearch.saveNewSearch(lmData.getLogin(), searchName, emailOption, ifEveryWeek, ifEveryMonth, specificDate, sendResultTo, notes,
								stateIds, countyIds, days, jobs, sections, stages, constructionTypes, bidTypes, Div, projKeyword, contactsKeyword,
								planningStages, constrMethods, indTypes, subIndTypes, valueMin, valueMax, unitOfMeasure, unitOfMeasureMin, unitOfMeasureMax,
								bidDateFrom, bidDateTo, showAll, disMode);

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
			// log.info("saveNewSearch OUTPUT: "+gson.toJson(map));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);

	}
}