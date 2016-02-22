/**
 * 
 */
package com.cdc.ws.calendar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import wscalendar.WSUserCalendar;
import wsdatamodel.LeadManagerSessionData;
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
public class CalendarController {

	private Logger log = Logger.getLogger(CalendarController.class.getName());

	private WSUserCalendar initWSUserCalendarBean() {
		WSUserCalendar wsCalendarBean = null;
		try {

			wsCalendarBean = EJBClient.getWSUserCalendarBean();

		} catch (Exception ex) {
			log("Exception while init of WSCalendarBean : " + ex);
		}
		return wsCalendarBean;
	}

	/**
	 * [Web Service] User Calendar functionality
	 * 
	 * @param sessionId
	 * @param securityKey
	 * @param typeId
	 * @param saveJobId
	 * @param calendarId
	 * @param cdcId
	 * @param eventTitle
	 * @param eventType
	 * @param eventDate
	 * @param eventTime
	 * @param reminderFlag
	 * @param freq
	 * @param periodVal
	 * @param sendEmail
	 * @param notes
	 * @return
	 */
	@RequestMapping(value = "/calendar")
	public String wsUserCalendar(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey, @RequestParam("typeId") int typeId,
			@RequestParam("saveJobId") int saveJobId, @RequestParam("calendarId") int calendarId, @RequestParam("cdcId") String cdcId,
			@RequestParam("eventTitle") String eventTitle, @RequestParam("eventType") String eventType, @RequestParam("eventDate") String eventDate,
			@RequestParam("eventTime") String eventTime, @RequestParam("reminderFlag") String reminderFlag, @RequestParam("freq") String freq,
			@RequestParam("periodVal") String periodVal, @RequestParam("sendEmail") String sendEmail, @RequestParam("notes") String notes) {

		Map<String, Object> map = null;
		Gson gson = null;
		LeadManagerSessionData lmData = null;
		Connection con = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();
			boolean validSessionId = false;
			boolean dbFlag = false;

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			if (validSessionId == true) {

				/**
				 * 1 - getCalendar events 2 - InsertCalender event 3 - updated calendar event 4 - deleted calendar event
				 */
				lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);
				if (typeId == 1) {
					ArrayList calList = getUserCalendarInfo(calendarId, con);
					map.put("iTotalRecords", String.valueOf(calList.size()));
					map.put("aaData", calList);

					log("loginID: " + lmData.getLogin() + ": Type : " + typeId + " : iTotalRecords : " + calList.size());
				} else if (typeId == 2) {
					dbFlag =
							insertUserCalendarEvent(lmData.getLogin(), saveJobId, cdcId, eventTitle, eventType, eventDate, eventTime, reminderFlag, freq,
									periodVal, sendEmail, notes, con);

					map.put("CalendarInsertedFlag", "" + dbFlag);

					log("loginID: " + lmData.getLogin() + ": Type : " + typeId + " : dbFlag : " + dbFlag);
				} else if (typeId == 3) {
					dbFlag =
							updateUserCalendarEvent(calendarId, eventTitle, eventType, eventDate, eventTime, reminderFlag, freq, periodVal, sendEmail, notes,
									con);
					map.put("CalendarUpdatedFlag", "" + dbFlag);
					log("loginID: " + lmData.getLogin() + ": Type : " + typeId + " : dbFlag : " + dbFlag);
				} else if (typeId == 4) {
					dbFlag = deleteUserCalendarEvent(calendarId, con);
					map.put("CalendarDeletedFlag", "" + dbFlag);
					log("loginID: " + lmData.getLogin() + ": Type : " + typeId + " : dbFlag : " + dbFlag);
				}
			} else {
				map.put("iTotalRecords", "0");
				map.put("aaData", "");
				map.put("message", "Invalid Login");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			log("Exception in wsUserCalendar() " + ex);
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}
		return gson.toJson(map);

	}

	// insertUserCalendarEvent
	private boolean insertUserCalendarEvent(String loginId, int saveJobId, String cdcId, String eventTitle, String eventType, String eventDate,
			String eventTime, String reminderFlag, String freq, String periodVal, String sendEmail, String notes, Connection con) {

		boolean insertFlag = true;

		try {

			WSUserCalendar wsCalendarBean = initWSUserCalendarBean();

			// INSERT INTO CALENDAR
			insertFlag =
					wsCalendarBean.insertPTCalendarEvent(loginId, saveJobId, cdcId, eventTitle, eventType, eventDate, eventTime, reminderFlag, freq, periodVal,
							sendEmail, notes);

		} catch (Exception ex) {
			ex.printStackTrace();
			log("Exception in insertUserCalendarEvent() " + ex.toString());
		}

		return insertFlag;
	}

	// updateUserCalendarEvent
	private boolean updateUserCalendarEvent(int calendarId, String eventTitle, String eventType, String eventDate, String eventTime, String reminderFlag,
			String freq, String periodVal, String sendEmail, String notes, Connection con) {

		boolean updateFlag = true;

		try {

			WSUserCalendar wsCalendarBean = initWSUserCalendarBean();

			// UPDATE CALENDAR
			updateFlag =
					wsCalendarBean.updatePTCalendarEvent(calendarId, eventTitle, eventType, eventDate, eventTime, reminderFlag, freq, periodVal, sendEmail,
							notes);

		} catch (Exception ex) {
			ex.printStackTrace();
			log("Exception in updateUserCalendarEvent() " + ex.toString());
		}

		return updateFlag;
	}

	// deleteUserCalendarEvent
	private boolean deleteUserCalendarEvent(int calendarId, Connection con) {

		boolean deletedFlag = true;

		try {

			WSUserCalendar wsCalendarBean = initWSUserCalendarBean();

			// UPDATE CALENDAR
			deletedFlag = wsCalendarBean.DeletePTCalendarEvent(calendarId);

		} catch (Exception ex) {
			ex.printStackTrace();
			log("Exception in deleteUserCalendarEvent() " + ex.toString());
		}

		return deletedFlag;
	}

	private ArrayList getUserCalendarInfo(int calendarId, Connection con) {

		ArrayList userCalendarInfoList = null;
		try {

			WSUserCalendar wsCalendarBean = initWSUserCalendarBean();

			userCalendarInfoList = wsCalendarBean.getCalendarInfo(calendarId);
			// jsonArray = JSONArray.fromObject(userCalendarInfoList);

			/*
			 * CalendarData calData = null; Iterator itr = userCalendarInfoList.iterator(); while (itr.hasNext()) {
			 * 
			 * calData = (CalendarData) itr.next(); //jsonArray = JSONArray.fromObject(calData); //log.info(jsonArray+"JSON ARRAY"); map.put("loginId",
			 * calData.getLoginId()); map.put("savedJobId", String.valueOf(calData.getSaveJobId())); map.put("cdcId", calData.getCdcId()); map.put("eventTitle",
			 * calData.getEventTitle()); map.put("eventType", calData.getEventType()); map.put("eventDate", calData.getEventDate()); map.put("eventTime",
			 * calData.getEventTime()); map.put("reminderFlag", calData.getReminderFlag()); map.put("frequency", calData.getFrequency()); map.put("periodValue",
			 * calData.getPeriodValue()); map.put("notes", calData.getNotes()); map.put("sendMailTo", calData.getSendMailTo()); map.put("recordInsertedDate",
			 * calData.getRecordInsertDate());
			 * 
			 * 
			 * 
			 * 
			 * 
			 * 
			 * }
			 */
		} catch (Exception ex) {
			ex.printStackTrace();
			log("EXCEPTION IN getUserCalendarInfo  " + ex.toString());
		}
		/**/
		// log.info(jsonArray.toString());
		return userCalendarInfoList;
	}

	/**
	 * Init log writer
	 */
	public BufferedWriter logInit() {

		BufferedWriter bw = null;
		try {
			String date = new SimpleDateFormat("MMddyy").format(new java.util.Date());
			File f = new File("wslogs/webservice_" + date + ".log");

			if (!f.exists())
				f.createNewFile();

			FileWriter fw = new FileWriter(f, true);
			bw = new BufferedWriter(fw);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bw;
	}

	/**
	 * write log
	 * 
	 * @param info
	 */
	public void log(String info) {

		BufferedWriter bw = null;

		try {
			String logtime = new SimpleDateFormat("d MMM yyyy H:m:s,S").format(new java.util.Date());

			bw = logInit();

			if (bw != null)
				bw.write(logtime + "[" + this.getClass().getName() + "] -" + info + "\n");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
