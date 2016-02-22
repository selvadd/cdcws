/**
 * 
 */
package com.cdc.ws.email;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import wsdatamodel.LeadManagerSessionData;
import wsemail.WSEmailProject;
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
public class EmailController {

	private Logger log = Logger.getLogger(EmailController.class.getName());

	/**
	 * Initialize WSEmailProject
	 */
	private WSEmailProject initWSEmailProject() {
		WSEmailProject wsEmailEJB = null;
		try {
			wsEmailEJB = EJBClient.getWSEmailProjectBean();
		} catch (Exception ex) {
			log.warn("Exception while Initializing WSEmailProject : " + ex);
		}
		return wsEmailEJB;
	}

	/**
	 * [Web Service]
	 * 
	 * Emails project Details
	 * 
	 * @param sessionId
	 * @param securityKey
	 * @param cdcId
	 * @param toEmail
	 * @param fromEmail
	 * @param subject
	 * @param emailNote
	 * @return
	 */
	@RequestMapping(value = "/emailProjectDetailByCdcId")
	public String wsEmailProjectDetailByCdcId(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey,
			@RequestParam("cdcId") String cdcId, @RequestParam("toEmail") String toEmail, @RequestParam("fromEmail") String fromEmail,
			@RequestParam("subject") String subject, @RequestParam("emailNote") String emailNote) {

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

			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);

			if (validSessionId == true) {

				WSEmailProject wsEmailEJB = initWSEmailProject();

				boolean emailStatus = wsEmailEJB.emailProjectDetailBycdcId(cdcId, toEmail, fromEmail, subject, emailNote);

				log("To: " + toEmail + ": From: " + fromEmail + ": Subject: " + subject);
				log("loginID: " + lmData.getLogin() + ": emailStatus : " + emailStatus);

				if (emailStatus) {
					map.put("Message", "Project Details have been Emailed Successfully.");
					map.put("EmailStatus", "Success");
				} else {
					map.put("Message", "Sorry, Unable to Process your request. Please Try again!");
					map.put("EmailStatus", "Failure");
				}

			} else {
				map.put("iTotalRecords", "0");
				map.put("aaData", "");
				map.put("message", "Invalid Login");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			log("Exception in wsEmailProject() " + ex.toString());
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}
		return gson.toJson(map);
	}

	/**
	 * [Web Service]
	 * 
	 * Emails project Details
	 * 
	 * @param sessionId
	 * @param securityKey
	 * @param cdcId
	 * @param toEmail
	 * @param fromEmail
	 * @param subject
	 * @param emailNote
	 * @return
	 */
	@RequestMapping(value = "/emailProjectDetailByProjectId")
	public String wsEmailProjectDetailByProjectId(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey,
			@RequestParam("projectId") int projectId, @RequestParam("toEmail") String toEmail, @RequestParam("fromEmail") String fromEmail,
			@RequestParam("subject") String subject, @RequestParam("emailNote") String emailNote) {

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

			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);

			log("ByProjectId validSessionId: " + validSessionId);
			if (validSessionId == true) {

				WSEmailProject wsEmailEJB = initWSEmailProject();

				boolean emailStatus = wsEmailEJB.emailProjectDetailByProjectId(projectId, toEmail, fromEmail, subject, emailNote);

				log("loginID: " + lmData.getLogin() + ": emailStatus : " + emailStatus);

				if (emailStatus) {
					map.put("Message", "Project Details have been Emailed Successfully.");
					map.put("EmailStatus", "Success");
				} else {
					map.put("Message", "Sorry, Unable to Process your request. Please Try again!");
					map.put("EmailStatus", "Failure");
				}

			} else {
				map.put("iTotalRecords", "0");
				map.put("aaData", "");
				map.put("message", "Invalid Login");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			log("Exception in wsEmailProject() " + ex.toString());
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);
	}

	/**
	 * Init log writer
	 */
	public BufferedWriter logInit() {

		BufferedWriter bw = null;
		try {
			String date = new SimpleDateFormat("MMddyy").format(new java.util.Date());
			File f = new File("D:/wslogs/webservice_" + date + ".log");

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
