/**
 * 
 */
package com.cdc.ws.contactsearch;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import wsutils.JDBCUtil;
import wsutils.LoginUtil;

import com.google.gson.Gson;

/**
 * @author Selva
 *
 */

@RestController
@RequestMapping(value = "/services")
public class CSFormDetailsController {

	private Logger log = Logger.getLogger(CSFormDetailsController.class.getName());

	public ArrayList getContactSearchFormInfo(int typeId, String inputId, Connection con) {

		// DetailsData dData = null;
		ResultSet rs = null;
		CallableStatement cstmt = null;
		ArrayList contactSearchFormDataList = new ArrayList();

		try {

			cstmt = con.prepareCall("{call SP_CONTACTSEARCH_FORM_DATA(?,?)}");
			cstmt.setInt(1, typeId);
			if (inputId != null) {
				cstmt.setString(2, inputId);
			} else {
				cstmt.setString(2, null);
			}

			rs = cstmt.executeQuery();

			while (rs.next()) {
				ArrayList<String> dataList = new ArrayList<String>();
				dataList.add(String.valueOf(rs.getInt("id")));
				dataList.add(rs.getString("name"));
				dataList.add(rs.getString("abb"));
				contactSearchFormDataList.add(dataList);

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
				log.error("!exception3.2!SQL error in getContactSearchFormInfo" + se);

			} finally {
				try {
					if (cstmt != null) {
						cstmt.close();
					}
				} catch (SQLException se) {
					log.error("!exception3.3!SQL error in getContactSearchFormInfo" + se);

				}
			}
		}

		return contactSearchFormDataList;
	} // getContactSearchFormInfo

	@RequestMapping(value = "/getContactSearchFormFormat")
	public String getContactSearchFormFormat(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey,
			@RequestParam("typeId") int typeId, @RequestParam("inputId") String inputId) {

		Map<String, Object> map = null;
		Gson gson = null;
		Connection con = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			boolean validSessionId = false;

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			if (validSessionId == true) {

				map.put("iTotalRecords", "1");
				map.put("Status", "Success");
				map.put("Message", "");

				map.put("Data", getContactSearchFormInfo(typeId, inputId, con));
			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Exception in getContactSearchFormInfo() for CONTACT SEARCH " + ex.getMessage());
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}
		return gson.toJson(map);

	}

}
