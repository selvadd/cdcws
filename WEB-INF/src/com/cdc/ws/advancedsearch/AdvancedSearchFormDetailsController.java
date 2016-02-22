/**
 * 
 */
package com.cdc.ws.advancedsearch;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import wsdatamodel.LeadManagerSessionData;
import wsdatamodel.SearchFormData;
import wsutils.BasicDataUtil;
import wsutils.JDBCUtil;
import wsutils.LoginUtil;

import com.google.gson.Gson;

/**
 * @author Selva
 *
 */

@RestController
@RequestMapping(value = "/services/advancedSearch")
public class AdvancedSearchFormDetailsController {

	private Logger log = Logger.getLogger(AdvancedSearchFormDetailsController.class.getName());

	public ArrayList<SearchFormData> getAdvancedSearchFormInfo(int sessionId, String securityKey) {

		String userStateIdList = null;
		String userCountyIdList = null;
		String geoUserStateIdList = null;
		String geoUserCountyIdList = null;
		String userStateIds = null;
		String userCountyIds = null;
		Connection con = null;

		// log.info(UserCountyids+"COUNTYE");

		ArrayList<SearchFormData> searchFormList = new ArrayList<SearchFormData>();
		SearchFormData sfData = new SearchFormData();

		try {

			if (con == null || con.isClosed()) {
				con = JDBCUtil.getDBConnectionFromDatabase();
			}
			LeadManagerSessionData lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);
			// USER SUBSCRIBED STATE AND COUNTY LIST
			userStateIdList = lmData.getUserStateList();
			userCountyIdList = lmData.getUserCountyList();
			geoUserStateIdList = lmData.getGeoUserStateList();
			geoUserCountyIdList = lmData.getGeoUserCountyList();

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

			// STATES
			sfData.setStateIdList(BasicDataUtil.getBasicData(12, userStateIds));
			sfData.setCountyIdList(BasicDataUtil.getBasicData(13, userCountyIds));
			sfData.setEditionDays("90 days,60 days,45 days,30 days,15 days," + " 10 days,7 days,5 days,48 hours,24 hours");
			// NEW PROJECTS
			sfData.setNewProjectList("0,1");
			sfData.setSectionList(BasicDataUtil.getBasicData(5, null));
			sfData.setSubSectionList(BasicDataUtil.getBasicData(4, null));
			sfData.setConstructionTypeList("New Construction,Renovation,Alteration,Addition");
			sfData.setProjectKeywords("");
			sfData.setContactKeywords("");
			sfData.setDivisionsList(BasicDataUtil.getBasicData(9, null));
			sfData.setPlanningStages(BasicDataUtil.getBasicData(6, null));
			sfData.setContractinMethod(BasicDataUtil.getBasicData(8, null));
			sfData.setIndustryList(BasicDataUtil.getBasicData(10, null));
			sfData.setSubIndustryList(BasicDataUtil.getBasicData(11, null));
			sfData.setEstimatedMin("");
			sfData.setEstimatedMax("");
			sfData.setUnitValue("Unit,SF,GSF");
			sfData.setUnitMin("");
			sfData.setUnitMax("");
			sfData.setBidDateTo("");
			sfData.setBidDateFrom("");
			sfData.setShowAllList("0,1");
			sfData.setDisplayModeList("Brief,Details");
			searchFormList.add(sfData);

		} catch (Exception e) {

			e.printStackTrace();
			log.error("Exception in AdvancedSearchFormDetails() for ADVANCED search " + e.getMessage());
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return searchFormList;
	}

	@RequestMapping(value = "/getAdvancedSearchFormat")
	public String getAdvancedSearchFormat(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey) {

		Map<String, Object> map = new HashMap<String, Object>();
		Gson gson = new Gson();
		Connection con = null;
		try {

			if (con == null || con.isClosed()) {
				con = JDBCUtil.getDBConnectionFromDatabase();
			}

			boolean validSessionId = false;
			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			log.info(validSessionId);
			if (validSessionId == true) {

				map.put("iTotalRecords", "1");
				map.put("Status", "Success");
				map.put("Message", "");
				map.put("Data", getAdvancedSearchFormInfo(sessionId, securityKey));

			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Exception in getAdvancedSearchFormat() for QUICK search " + ex.getMessage());
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);

	}

}
