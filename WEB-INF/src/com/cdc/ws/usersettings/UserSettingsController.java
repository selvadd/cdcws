/**
 * 
 */
package com.cdc.ws.usersettings;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import wsdatamodel.LeadManagerSessionData;
import wsusersetting.WSUserSetting;
import wsutils.EJBClient;
import wsutils.JDBCUtil;
import wsutils.LoginUtil;

import com.google.gson.Gson;

/**
 * @author Selva
 *
 */
@RestController
@RequestMapping(value = "/services")
public class UserSettingsController {

	private Logger log = Logger.getLogger(UserSettingsController.class.getName());

	/**
	 * Creating Remote Interface for accessing WSUserSetting methods
	 */
	private WSUserSetting getWSUserSettingBean() {

		WSUserSetting wsUserSetting = null;
		try {
			wsUserSetting = EJBClient.getWSUserSettingBean();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return wsUserSetting;
	}

	// CHANGE PASSWORD
	@RequestMapping(value = "/changePassword")
	public String changePassword(@RequestParam("sessionId") int sessionID, @RequestParam("securityKey") String securityKey,
			@RequestParam("oldPwd") String oldPwd, @RequestParam("newPwd") String newPwd) {

		Map<String, Object> map = null;
		Gson gson = null;

		boolean validSessionId = false;
		boolean changedPwdFlag = false;
		LeadManagerSessionData lmData = null;
		Connection con = null;
		WSUserSetting wsUserSetting = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			wsUserSetting = getWSUserSettingBean();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);
			// log.info("chnage password"+changedPwdFlag);

			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionID, con);
				changedPwdFlag = wsUserSetting.changePassword(lmData.getLogin(), oldPwd, newPwd);
				// log.info("chnage password 2"+changedPwdFlag);

				if (changedPwdFlag == true) {

					map.put("iTotalRecords", "1");
					map.put("Status", "Success");
					map.put("Message", "");
				} else {
					map.put("iTotalRecords", "0");
					map.put("Status", "Failure");
					map.put("Message", "Unable to Change Password");
				}

			} // end of if for validsession check

			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

			// log.info("Change Password : " +gson.toJson(map));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);
	}

	// changeUserView
	@RequestMapping(value = "/changeUserView")
	public String changeUserView(@RequestParam("sessionId") int sessionID, @RequestParam("securityKey") String securityKey,
			@RequestParam("setView") String setView) {

		Map<String, Object> map = null;
		Gson gson = null;

		boolean validSessionId = false;
		boolean changedUserViewFlag = false;
		LeadManagerSessionData lmData = null;
		Connection con = null;
		WSUserSetting wsUserSetting = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			wsUserSetting = getWSUserSettingBean();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);

			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionID, con);
				changedUserViewFlag = wsUserSetting.changeUserView(lmData.getLogin(), setView);

				if (changedUserViewFlag == true) {

					map.put("iTotalRecords", "1");
					map.put("Status", "Success");
					map.put("Message", "");
				} else {
					map.put("iTotalRecords", "0");
					map.put("Status", "Failure");
					map.put("Message", "Unable to Change User View");
				}

			} // end of if for validsession check

			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

			// log.info(" changeUserView : " +gson.toJson(map));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);
	}

	// changeUserReminderDays
	@RequestMapping(value = "/changeUserReminderDays")
	public String changeUserReminderDays(@RequestParam("sessionId") int sessionID, @RequestParam("securityKey") String securityKey,
			@RequestParam("reminderDays") int reminderDays) {

		Map<String, Object> map = null;
		Gson gson = null;

		boolean validSessionId = false;
		boolean changedUserReminderFlag = false;
		LeadManagerSessionData lmData = null;
		Connection con = null;
		WSUserSetting wsUserSetting = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			wsUserSetting = getWSUserSettingBean();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);

			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionID, con);
				changedUserReminderFlag = wsUserSetting.setUserReminderDays(lmData.getLogin(), reminderDays);

				if (changedUserReminderFlag == true) {

					map.put("iTotalRecords", "1");
					map.put("Status", "Success");
					map.put("Message", "");
				} else {
					map.put("iTotalRecords", "0");
					map.put("Status", "Failure");
					map.put("Message", "Unable to Change User Reminder Days");
				}

			} // end of if for validsession check

			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

			// log.info(" changeUserReminderDays : " +gson.toJson(map));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);
	}

	// createSubUser
	@RequestMapping(value = "/createSubUser")
	public String createSubUser(@RequestParam("sessionId") int sessionID, @RequestParam("securityKey") String securityKey,
			@RequestParam("loginId") String loginId, @RequestParam("pwd") String pwd, @RequestParam("fname") String fname, @RequestParam("lname") String lname,
			@RequestParam("email") String email, @RequestParam("pubs") String pubs, @RequestParam("countyIdsGeo") String countyIdsGeo,
			@RequestParam("subUserPlanFlag") String subUserPlanFlag) {

		Map<String, Object> map = null;
		Gson gson = null;

		boolean validSessionId = false;
		boolean insertFlag = false;
		LeadManagerSessionData lmData = null;
		Connection con = null;
		WSUserSetting wsUserSetting = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			wsUserSetting = getWSUserSettingBean();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);

			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionID, con);
				// loginId variable=sub user login
				insertFlag = wsUserSetting.createSubUser(lmData.getLogin(), loginId, pwd, fname, lname, email, pubs, countyIdsGeo, subUserPlanFlag);

				if (insertFlag == true) {

					map.put("iTotalRecords", "1");
					map.put("Status", "Success");
					map.put("Message", "");
				} else {
					map.put("iTotalRecords", "0");
					map.put("Status", "Failure");
					map.put("Message", "Unable to Create Sub User ");
				}

			} // end of if for validsession check

			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

			// log.info(" createSubUser : " +gson.toJson(map));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);
	}

	// editSubUser
	@RequestMapping(value = "/editSubUser")
	public String editSubUser(@RequestParam("sessionId") int sessionID, @RequestParam("securityKey") String securityKey,
			@RequestParam("loginId") String loginId, @RequestParam("pwd") String pwd, @RequestParam("fname") String fname, @RequestParam("lname") String lname,
			@RequestParam("email") String email, @RequestParam("pubs") String pubs, @RequestParam("countyIdsGeo") String countyIdsGeo,
			@RequestParam("subUserPlanFlag") String subUserPlanFlag) {

		Map<String, Object> map = null;
		Gson gson = null;

		boolean validSessionId = false;
		boolean updateFlag = false;
		LeadManagerSessionData lmData = null;
		Connection con = null;
		WSUserSetting wsUserSetting = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			wsUserSetting = getWSUserSettingBean();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);

			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionID, con);
				// loginId variable=sub user login
				updateFlag = wsUserSetting.editSubUser(lmData.getLogin(), loginId, pwd, fname, lname, email, pubs, countyIdsGeo, subUserPlanFlag);

				if (updateFlag == true) {

					map.put("iTotalRecords", "1");
					map.put("Status", "Success");
					map.put("Message", "");
				} else {
					map.put("iTotalRecords", "0");
					map.put("Status", "Failure");
					map.put("Message", "Unable to Edit Sub User ");
				}

			} // end of if for validsession check

			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

			// log.info(" editSubUser : " +gson.toJson(map));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);
	}

	// deleteSubUser
	@RequestMapping(value = "/deleteSubUser")
	public String deleteSubUser(@RequestParam("sessionId") int sessionID, @RequestParam("securityKey") String securityKey,
			@RequestParam("loginId") String loginId) {

		Map<String, Object> map = null;
		Gson gson = null;

		boolean validSessionId = false;
		boolean deleteSubUserFlag = false;
		LeadManagerSessionData lmData = null;
		Connection con = null;
		WSUserSetting wsUserSetting = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			wsUserSetting = getWSUserSettingBean();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);

			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionID, con);
				// loginId=sub user
				deleteSubUserFlag = wsUserSetting.deleteSubUser(lmData.getLogin(), loginId);

				if (deleteSubUserFlag == true) {

					map.put("iTotalRecords", "1");
					map.put("Status", "Success");
					map.put("Message", "");
				} else {
					map.put("iTotalRecords", "0");
					map.put("Status", "Failure");
					map.put("Message", "Unable to deleteSubUser");
				}

			} // end of if for validsession check

			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

			// log.info("deleteSubUser : " +gson.toJson(map));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);
	}

	// changeSubUserPassword
	@RequestMapping(value = "/changeSubUserPassword")
	public String changeSubUserPassword(@RequestParam("sessionId") int sessionID, @RequestParam("securityKey") String securityKey,
			@RequestParam("oldPwd") String oldPwd, @RequestParam("newPwd") String newPwd) {

		Map<String, Object> map = null;
		Gson gson = null;

		boolean validSessionId = false;
		boolean changedPwdFlag = false;
		LeadManagerSessionData lmData = null;
		Connection con = null;
		WSUserSetting wsUserSetting = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			wsUserSetting = getWSUserSettingBean();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);

			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionID, con);
				changedPwdFlag = wsUserSetting.changePassword(lmData.getLogin(), oldPwd, newPwd);

				if (changedPwdFlag == true) {

					map.put("iTotalRecords", "1");
					map.put("Status", "Success");
					map.put("Message", "");
				} else {
					map.put("iTotalRecords", "0");
					map.put("Status", "Failure");
					map.put("Message", "Unable to Change Password");
				}

			} // end of if for validsession check

			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

			// log.info("changeSubUserPassword : " +gson.toJson(map));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);
	}

	// subUserLoginReport
	@RequestMapping(value = "/subUserLoginReport")
	public String subUserLoginReport(@RequestParam("sessionId") int sessionID, @RequestParam("securityKey") String securityKey) {

		Map<String, Object> map = null;
		Gson gson = null;

		boolean validSessionId = false;
		LeadManagerSessionData lmData = null;
		Connection con = null;
		WSUserSetting wsUserSetting = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			wsUserSetting = getWSUserSettingBean();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);
			// log.info(validSessionId);
			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionID, con);

				map.put("aaData", wsUserSetting.subUserLoginReport(lmData.getLogin()));

			} // end of if for validsession check

			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

			// log.info("subUserLoginReport : " +gson.toJson(map));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);
	}

	// getSubUserInfo
	@RequestMapping(value = "/getSubUserInfo")
	public String getSubUserInfo(@RequestParam("sessionId") int sessionID, @RequestParam("securityKey") String securityKey) {

		Map<String, Object> map = null;
		Gson gson = null;

		boolean validSessionId = false;
		LeadManagerSessionData lmData = null;
		Connection con = null;
		WSUserSetting wsUserSetting = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			wsUserSetting = getWSUserSettingBean();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionID, securityKey, con);
			// log.info(validSessionId);
			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionID, con);

				map.put("aaData", wsUserSetting.getSubUserInfo(lmData.getLogin()));

			} // end of if for validsession check

			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

			// log.info("getSubUserInfo : " +gson.toJson(map));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);
	}

}
