/**
 * 
 */
package com.cdc.ws.advancedsearch;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
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
@RequestMapping(value = "/services")
public class ProjectScreenController {

	private Logger log = Logger.getLogger(ProjectScreenController.class.getName());

	/**
	 * [web service] Screen project for the loginId and CDCID
	 * 
	 * @param sessionId
	 * @param securityKey
	 * @param cdcId
	 * @return
	 */
	@RequestMapping(value = "/screenProject")
	public String wsScreenProject(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey, @RequestParam("cdcId") String cdcId) {

		Map<String, Object> map = null;
		Gson gson = null;

		LeadManagerSessionData lmData = null;
		Connection con = null;

		try {
			gson = new Gson();
			map = new HashMap<String, Object>();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			boolean validSessionId = false;
			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);
			if (validSessionId) {
				lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);
				int intUptStatus = screenProject(lmData.getLogin(), cdcId);
				map.put("Status", "success");
				map.put("Message", "Project Screened Successfully");
			} else {
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");
			}
		} catch (Exception ex) {
			log.error("Exception1.1 occurred in web service while screening project " + ex);
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);
	}

	/**
	 * Screen project for the loginId and CDCID
	 * 
	 * @param loginId
	 * @param cdcId
	 * @return
	 */
	public int screenProject(String loginId, String cdcId) {

		int uptStatus = 0;
		CallableStatement cstmt = null;
		Connection con = null;
		try {
			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			cstmt = con.prepareCall("{call ssp_exclude_projects(?,?)}");
			cstmt.setString(1, loginId);
			cstmt.setString(2, cdcId);

			uptStatus = cstmt.executeUpdate();

		} catch (SQLException se) {
			log.error("SQLException occurred in web service while screening project " + se);
		} catch (Exception ex) {
			log.error("Exception occurred in web service while screening project " + ex);
		} finally {
			try {
				if (cstmt != null)
					cstmt.close();
			} catch (Exception ex) {
				log.error("Exception1.2 while releasing connections at screenProject web service " + ex);
			}

			JDBCUtil.releaseDBConnection(con);
		}

		return uptStatus;

	} // End of screenProject

}
