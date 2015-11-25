/**
 * 
 */
package com.cdc.ws.contentdetails;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import wscontent.WSContent;
import wsdatamodel.LeadManagerSessionData;
import wsutils.EJBClient;
import wsutils.JDBCUtil;
import wsutils.LoginUtil;
import wsutils.WebUsageUtil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author Selva
 *
 */

@RestController
@RequestMapping(value="/services")
public class ContentDetailsController {

	private static Logger log = Logger.getLogger(ContentDetailsController.class.getName());

	/**
	 * Initialize WSContent
	 */
	private WSContent initwsContent() {
		WSContent wsContent = null;
		try {
			wsContent = EJBClient.WSContentEJBean();
		} catch (Exception ex) {
			log.warn("Exception while Initializing wsContent : " + ex);
		}
		return wsContent;
	}

	/**
	 * 
	 * @param loginId
	 * @param ids
	 * @return
	 */
	public ArrayList getContentDetails(String loginId, String ids) {

		ArrayList contentList = new ArrayList();
		WSContent wsContent = null;
		try {

			wsContent = initwsContent();

			contentList = wsContent.getContentList(ids, loginId);

		} catch (Exception ex) {
			log.error("Exception in getContentDetails() " + ex.getMessage());
			ex.printStackTrace();
		}

		return contentList;

	}

	/**
	 * 
	 * @param cdcids
	 * @return
	 */
	public ArrayList getContentDetailsByCdcId(String cdcids) {

		ArrayList contentList = new ArrayList();
		WSContent wsContent = null;
		try {
			wsContent = initwsContent();

			contentList = wsContent.getContentListByCdcIds(cdcids);

		} catch (Exception ex) {
			log.error("Exception in getContentDetailsByCdcId() " + ex.getMessage());
			ex.printStackTrace();
		}

		return contentList;

	}// end of getContentDetailsByCdcId

	/**
	 * /online-product/project-details-by-ids
	 * @param sessionId
	 * @param securityKey
	 * @param ids
	 * @return
	 */
	@RequestMapping(value = "/projectDetails")
	public String getProjectDetails(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey, @RequestParam("ids") String ids) {
		Map<String, Object> map = null;
		Gson gson = null;
		Connection con = null;
		try {

			gson = new GsonBuilder().serializeNulls().create();
			map = new HashMap<String, Object>();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			LeadManagerSessionData lmData = null;
			boolean validSessionId = false;
			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			// log.info(validSessionId);
			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);

				String currentPage = "View Project Details";
				// Start Web Usage Feed Script
				WebUsageUtil webUsage = null;
				webUsage = new WebUsageUtil();
				webUsage.webUsageFeed(lmData.getLogin(), currentPage, null, "");
				// End Web Usage Feed Script

				map.put("iTotalRecords", "1");
				map.put("Status", "Success");
				map.put("Message", "");
				// jsonArray.add(jsonObject);
				// jsonArray.add(getContentDetails(sessionId, securityId, ids));
				map.put("Data", getContentDetails(lmData.getLogin(), ids));
			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");
				// jsonArray.add(jsonObject);
			}

		} catch (Exception ex) {
			log.error("Exception in getProjectDetails() " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map).replaceAll("null", "\"\"");

	}

	/**
	 * getContentDetailsPublicSearch for public search project display without login
	 * 
	 * @param ids
	 * @return
	 */
	public ArrayList getContentDetailsPublicSearch(String ids) {

		ArrayList contentList = new ArrayList();
		WSContent wsContent = null;
		try {
			wsContent = initwsContent();

			contentList = wsContent.getContentList(ids);

		} catch (Exception ex) {
			log.error("Exception in getContentDetailsPublicSearch() " + ex.getMessage());
			ex.printStackTrace();
		}

		return contentList;

	}

	/**
	 * getPubProjectDetails for public search project display without login
	 * /online-product/pub-project-details
	 * @param ids
	 * @return
	 */
	@RequestMapping(value = "/publicProjectDetails")
	public String getPubProjectDetails(@RequestParam("ids") String ids) {
		Map<String, Object> map = null;
		Gson gson = null;

		try {

			gson = new GsonBuilder().serializeNulls().create();
			map = new HashMap<String, Object>();

			map.put("iTotalRecords", "1");
			map.put("Status", "Success");
			map.put("Message", "");
			map.put("Data", getContentDetailsPublicSearch(ids));

		} catch (Exception ex) {
			log.error("Exception in getPubProjectDetails() " + ex.getMessage());
			ex.printStackTrace();
		}

		return gson.toJson(map).replaceAll("null", "\"\"");

	}

	/**
	 * /online-product/project-details-by-cdcids
	 * @param sessionId
	 * @param securityKey
	 * @param cdcids
	 * @return
	 */
	@RequestMapping(value = "/projectDetailsByCdcId")
	public String getProjectDetailsByCdcId(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey,
			@RequestParam("cdcids") String cdcids) {
		Map<String, Object> map = null;
		Gson gson = null;
		Connection con = null;
		try {

			gson = new GsonBuilder().serializeNulls().create();
			map = new HashMap<String, Object>();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			boolean validSessionId = false;
			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			log.info(validSessionId);
			if (validSessionId == true) {

				int size = 1;
				ArrayList contentDetails = null;
				contentDetails = getContentDetailsByCdcId(cdcids);

				if (contentDetails != null) {
					size = contentDetails.size();
				}

				// log.info(size+"SIZE");
				map.put("iTotalRecords", String.valueOf(size));
				map.put("Status", "Success");
				map.put("Message", "");
				// jsonArray.add(jsonObject);
				// jsonArray.add(getContentDetails(sessionId, securityId, ids));
				map.put("Data", contentDetails);
			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");
				// jsonArray.add(jsonObject);
			}

			// log.info(gson.toJson(map).replaceAll("null","\"\"")+"Content Details BY CDC ID");

		} catch (Exception ex) {
			log.error("Exception in getProjectDetailsByCdcId() " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map).replaceAll("null", "\"\"");

	}

}
