package com.cdc.ws.loginvalidation;

import wsdatamodel.*;
import wsutils.*;

import java.util.*;

import datavalidation.ValidateDate;

import java.sql.*;

import wslogin.*;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;

@RestController
public class UserLoginValidationController {

	private Logger log = Logger.getLogger(UserLoginValidationController.class.getName());

	/**
	 * Initialize WSContent
	 */
	private WSLogin getWSLogin() {
		WSLogin wsLoginEJB = null;
		try {
			wsLoginEJB = EJBClient.getWSLoginBean();
		} catch (Exception ex) {
			log.warn("Exception while getting WSLogin : " + ex);
		}
		return wsLoginEJB;
	}

	@RequestMapping(value = "/online-product/login/validation", method = RequestMethod.GET)
	public String checkLoggedUser(@RequestParam("username") String username, 
			@RequestParam("pwd") String pwd, @RequestParam("siteId") int siteId) {

		Map<String, Object> map = null;
		Gson gson = null;
		boolean userSiteAccess = false;
		Connection con = null;
		WSLogin wsLoginEJB = null;

		try {
			wsLoginEJB = getWSLogin();
			gson = new Gson();

			if (con == null || con.isClosed()) {
				con = JDBCUtil.getDBConnectionFromDatabase();
			}

			map = new HashMap<String, Object>();

			// CHECK IF LOGIN EXISTS
			boolean userExists = wsLoginEJB.checkValidUser(username, pwd);

			userSiteAccess = LoginUtil.checkValidUserAccessSite(username, siteId, con);
			// boolean insertFlag = false;
			if (userSiteAccess == true && userExists == true) {

				LoginData lData = wsLoginEJB.LoginInfo(username, pwd);
				// INSERTION INTO LEADMANAGER SESSION TABLE

				// insertFlag = wsLoginEJB.insertLoginInfo(lData);
				map.put("iTotalRecords", "1");
				map.put("Success", "success");
				map.put("Message", "");

				map.put("sessionId", String.valueOf(lData.getSessionId()));

				map.put("securityKey", lData.getSecurityKey());
				if (lData.getMaster() != null) {
					map.put("AccountType", "S");
				} else {
					map.put("AccountType", "M");
				}

			}
			// Added for LM Mobile. by Muthu on 09/23/14
			else if (userExists && siteId == 0) {
				String lastEntry = null;
				String currentDate = ValidateDate.getDateLikeDBDate(ValidateDate.getTodayDateMMDDYY());

				LoginData lData = wsLoginEJB.LoginInfo(username, pwd);

				// Updates user todays login date
				lastEntry = lData.getLastEntry();
				lastEntry = ValidateDate.getDateFromDBDate(lastEntry);
				currentDate = ValidateDate.getDateFromDBDate(currentDate);
				if (ValidateDate.compareDates(lastEntry, currentDate) != 0) { // 2-LT;1-GT;0-EQ
					// int uptFlag = wsLoginEJB.updateLastLogin(username);
					/*
					 * if(uptFlag>0) log.info("User Last Login Updated : "+username);
					 */
				}

				// Start Web Usage Feed Script
				WebUsageUtil webUsage = null;
				webUsage = new WebUsageUtil();

				webUsage.webUsageFeed(username, "Home Page", "Login Page", "");
				// End Web Usage Feed Script

				// INSERTION INTO LEADMANAGER SESSION TABLE

				// ResultSet rs = null;
				CallableStatement cstmt = null;

				try {

					cstmt = con.prepareCall("{call sp_loginhistory(?,?,?)}");
					cstmt.setInt(1, 1);
					cstmt.setString(2, username);
					cstmt.setString(3, "M");
					cstmt.execute();
				} catch (Exception ex) {
					log.error("Exception while recording login history : " + ex);
					ex.printStackTrace();
				} finally {
					try {
						if (cstmt != null)
							cstmt.close();
						if (con != null)
							con.close();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
				// insertFlag = wsLoginEJB.insertLoginInfo(lData);

				map.put("iTotalRecords", "1");
				map.put("Success", "success");
				map.put("Message", "cdcnews siteId with 0");

				map.put("sessionId", String.valueOf(lData.getSessionId()));

				map.put("securityKey", lData.getSecurityKey());
				if (lData.getMaster() != null) {
					map.put("AccountType", "S");
				} else {
					map.put("AccountType", "M");
				}
			}

			// IF LOGIN DOESNT EXISTS
			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");
				map.put("siteIds", "0-cdcnews 1-South Texas 2-ConstructionLeads 3-BT");

			}

		} catch (Exception e) {
			log.error("Exception in login Authentication : " + e);
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}
		return gson.toJson(map);
		// return true;

	} // End of checkLoggedUser

	/**
	 * 
	 * @param loginId
	 * @return
	 */
	public String getMasterSubUserDetails(String loginId) {

		Gson gson = null;
		ArrayList masterSubUserInfoList = null;
		WSLogin wsLoginEJB = null;

		try {

			wsLoginEJB = getWSLogin();
			gson = new Gson();

			masterSubUserInfoList = wsLoginEJB.getMasterSubUserInfo(loginId);

		} catch (Exception ex) {
			log.error("EXCEPTION IN getMasterSubUserDetails " + ex);
			ex.printStackTrace();
		}

		return gson.toJson(masterSubUserInfoList);

	} // End of getMasterSubUserDetails

	/**
	 * Called while user logging out
	 * 
	 * @param sessionId
	 * @param securityKey
	 * @return
	 */
	@RequestMapping(value = "/online-product/logout", method = RequestMethod.GET)
	public String logOut(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey) {

		Map<String, Object> map = null;
		Gson gson = null;
		Connection con = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			// INSERTION INTO LEADMANAGER SESSION TABLE
			//ResultSet rs = null;
			CallableStatement cstmt = null;
			LeadManagerSessionData lmData = null;

			try {
				if (con == null) {
					con = JDBCUtil.getDBConnectionFromDatabase();
				}

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);

				cstmt = con.prepareCall("{call sp_loginhistory(?,?,?,?,?)}");
				cstmt.setInt(1, 2);
				cstmt.setString(2, lmData.getLogin());
				cstmt.setString(3, "M");
				cstmt.setString(4, "");
				cstmt.setString(5, "L");

				cstmt.execute();

			} catch (Exception ex) {
				log.error("Exception while recording login history : " + ex);
				ex.printStackTrace();
			} finally {
				try {
					if (cstmt != null)
						cstmt.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}

			}

			boolean deleteSessionId = LoginUtil.LogOutSession(sessionId, securityKey, con);

			log.info("Log Out " + deleteSessionId);

			if (deleteSessionId == true) {
				map.put("iTotalRecords", "1");
				map.put("Status", "Success");
				map.put("Message", "");

			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

		} catch (Exception ex) {
			log.error("EXCEPTION IN logOut in UserValidation Webservice " + ex);
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);

	} // End of logOut

	/**
	 * 
	 * @param typeId
	 * @param loginId
	 * @param expiryDate
	 * @param customerLogin
	 * @return
	 */
	public String modifySubscriberTrial(int typeId, String loginId, String expiryDate, String customerLogin) {

		Map<String, Object> map = null;
		Gson gson = null;
		boolean updateFlag = true;
		WSLogin wsLoginEJB = null;

		try {

			wsLoginEJB = getWSLogin();

			gson = new Gson();
			map = new HashMap<String, Object>();

			updateFlag = wsLoginEJB.subscriberTrialUpdate(typeId, loginId, expiryDate, customerLogin);

			map.put("trialSubscriberUpdateFlag", String.valueOf(updateFlag));

		} catch (Exception ex) {
			log.error("Exception in modifySubscriberTrial() " + ex);
			ex.printStackTrace();
		}

		return gson.toJson(map);

	} // End of modifySubscriberTrial

}