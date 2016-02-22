package com.cdc.ws.quicksearch;

import wsdatamodel.*;
import wsutils.*;

import java.sql.Connection;
import java.util.*;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;

/**
 * @author Selva
 *
 */

@RestController
@RequestMapping(value = "/services")
public class QuickSearchFormDetailsController {

	private Logger log = Logger.getLogger(QuickSearchFormDetailsController.class.getName());

	/**
	 * 
	 * @param sessionId
	 * @param securityKey
	 * @param con
	 * @return
	 */
	public ArrayList<SearchFormData> getQuickSearchFormInfo(int sessionId, String securityKey, Connection con) {

		String userStateIdList = null;
		// String userCountyIdList = null;
		String geoUserStateIdList = null;
		// String geoUserCountyIdList = null;
		String userStateIds = null;
		// String userCountyIds = null;
		LeadManagerSessionData lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);
		// USER SUBSCRIBED STATE AND COUNTY LIST
		userStateIdList = lmData.getUserStateList();
		// userCountyIdList = lmData.getUserCountyList();
		geoUserStateIdList = lmData.getGeoUserStateList();
		// geoUserCountyIdList = lmData.getGeoUserCountyList();

		/* User state list */
		if (geoUserStateIdList != null && userStateIdList != null) {

			userStateIds = userStateIdList + "," + geoUserStateIdList;
		} else if (userStateIdList != null) {

			userStateIds = userStateIdList;
		} else if (geoUserStateIdList != null) {

			userStateIds = geoUserStateIdList;
		}
		ArrayList<SearchFormData> searchFormList = new ArrayList<SearchFormData>();
		SearchFormData sfData = new SearchFormData();

		try {

			// searchFormList.add("states" + ":" +BasicDataUtil.getBasicData(12, userStateIds));
			/*
			 * searchFormList.add("{"+"states"+":"); searchFormList.add(BasicDataUtil.getBasicData(12, userStateIds)+"}");
			 * 
			 * //New Projects searchFormList.add("New Projects:[0,1]"); //Section
			 * 
			 * searchFormList.add("section" + ":" + BasicDataUtil.getBasicData(5, null)); //Subsection
			 * 
			 * searchFormList.add("subsection" + ":" + BasicDataUtil.getBasicData(4, null)); //construct Type searchFormList.add(
			 * "construction Type:[New Construction,Renovation,Alteration,Addition]"); //Divisions
			 * 
			 * searchFormList.add("divisions" + ":" + BasicDataUtil.getBasicData(9, null)); //show all searchFormList.add("Show All:[0,1]"); //display mode
			 * searchFormList.add("Show All:[Brief,Details]");
			 */
			sfData.setStateIdList(BasicDataUtil.getBasicData(12, userStateIds));
			sfData.setNewProjectList("0,1");
			sfData.setSectionList(BasicDataUtil.getBasicData(5, null));
			sfData.setSubSectionList(BasicDataUtil.getBasicData(4, null));
			sfData.setConstructionTypeList("new:New Construction,ren:Renovation,alt:Alteration,add:Addition");
			sfData.setDivisionsList(BasicDataUtil.getBasicData(9, null));
			sfData.setShowAllList("0,1");
			sfData.setDisplayModeList("Brief,Details");
			searchFormList.add(sfData);

		} catch (Exception e) {

			log.error("ERROR IN getQuickSearchFormInfo " + e.getMessage());
			e.printStackTrace();

		}
		return searchFormList;
	}

	/**
	 * /online-product/quick-search
	 * 
	 * @param sessionId
	 * @param securityKey
	 * @return
	 */
	@RequestMapping(value = "/quickSearchFormDetails")
	public String getQuickSearchFormat(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey) {

		Map<String, Object> map = null;
		Gson gson = null;
		Connection con = null;

		try {

			gson = new Gson();
			map = new HashMap<String, Object>();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			boolean validSessionId = false;
			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			log.info(validSessionId);
			if (validSessionId == true) {

				map.put("iTotalRecords", "1");
				map.put("Status", "Success");
				map.put("Message", "");
				// jsonArray.add(jsonObject);
				map.put("Data", getQuickSearchFormInfo(sessionId, securityKey, con));
			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");
				// jsonArray.add(jsonObject);
			}

			// jsonArray = jsonArray.fromObject(getQuickSearchFormInfo(sessionId,securityKey));

			log.info(gson.toJson(map) + " JSON ARRAY LIST");

		} catch (Exception ex) {
			log.error("Exception in getQuickSearchFormat() for QUICK search " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}
		return gson.toJson(map);

	}

}