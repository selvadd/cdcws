/**
 * 
 */
package com.cdc.ws.advancedsearch;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import wsdatamodel.LeadManagerSessionData;
import wsutils.JDBCUtil;
import wsutils.LibraryFunctions;
import wsutils.LoginUtil;

import com.google.gson.Gson;

/**
 * @author Selva
 *
 */

@RestController
@RequestMapping(value = "/services/advancedSearch")
public class AdvancedSearchResultsController {

	private Logger log = Logger.getLogger(AdvancedSearchResultsController.class.getName());

	int size = 0;

	public List<String> getAdvancedSearchContentIdsPL(int sessionId, String securityKey, String stateIds, String editionDays, String sectionIdList,
			int newUpdatedFlag, String subSectionList, String constructionTypes, String divisionIdList, String projectKeywords, String contactKeywords,
			String planningStages, String conMethod, String industry, String subIndustry, String estiAmountHigh, String sessionLowAmount, String unit,
			String unitMin, String unitMax, String storiesMin, String storiesMax, String bidDateFrom, String bidDateTo, int showAll, String displayMode,
			String jobType, String counties, int pageNumber, int recordsPerPage, String sortOrder, String sortType, String searchText,
			String contactSearchText, String refinePKeywordText, String refineCKeywordText) {

		Connection con = null;
		ResultSet rs = null;
		Statement stmt = null;
		String sql = null;
		// String userStateIdList = null;
		String userCountyIdList = null;
		// String geoUserStateIdList = null;
		String geoUserCountyIdList = null;
		// String userStateIds = null;
		String userCountyIds = null;
		String nationalChainUser = null;

		int startIndex = 0;
		int endIndex = 0;

		ArrayList<String> contentIdList = new ArrayList<String>();

		try {
			if (con == null) {
				con = JDBCUtil.getDBConnectionFromDatabase();
			}

			LeadManagerSessionData lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);
			// USER SUBSCRIBED STATE AND COUNTY LIST
			// userStateIdList = lmData.getUserStateList();
			userCountyIdList = lmData.getUserCountyList();
			// geoUserStateIdList = lmData.getGeoUserStateList();
			geoUserCountyIdList = lmData.getGeoUserCountyList();

			/* User state list */
			/*
			 * if (geoUserStateIdList != null && userStateIdList != null) {
			 * 
			 * userStateIds = userStateIdList + "," + geoUserStateIdList; } else if (userStateIdList != null) {
			 * 
			 * userStateIds = userStateIdList; } else if (geoUserStateIdList != null) {
			 * 
			 * userStateIds = geoUserStateIdList; }
			 */
			/* User county list */
			if (geoUserCountyIdList != null && userCountyIdList != null) {

				userCountyIds = userCountyIdList + "," + geoUserCountyIdList;
			} else if (userCountyIdList != null) {

				userCountyIds = userCountyIdList;
			} else if (geoUserCountyIdList != null) {

				userCountyIds = geoUserCountyIdList;
			}

			// SHOW ALL
			if (showAll == 0) {
				sql =
						"select distinct c.id,c.cdc_id,c.publication_id,c.section_id,c.sub_section,"
								+ " c.title,c.state_id,c.estimated_amount_lower,c.bid_date,c.prebid_mtg,c.bids_details, "
								+ " c.plan_express,c.entry_date,c.short_cdcid,c.state_multiple,c.county_multiple,"
								+ " c.plan_availability_status,c.leads_id,c.county_id,gc.county_name,s.state_abb "
								+ " from content c,pub_section ps,state s,county gc";
			} else {
				sql =
						"select distinct top 1000 c.id,c.cdc_id,c.publication_id,c.section_id,c.sub_section,"
								+ " c.title,c.state_id,c.estimated_amount_lower,c.bid_date,c.prebid_mtg,c.bids_details, "
								+ " c.plan_express,c.entry_date,c.short_cdcid,c.state_multiple,c.county_multiple,"
								+ " c.plan_availability_status,c.leads_id,c.county_id,gc.county_name,s.state_abb "
								+ " from content c,pub_section ps,state s,county gc";

			}
			// CONTENT_DIVISIONS
			if (divisionIdList != null && divisionIdList.equals("") != true) {

				sql += ",content_divisions_2004 cd";

			}

			// CONTENT_DETAILS
			if (((storiesMin != null && storiesMin.equals("") == false) || (storiesMax != null && storiesMax.equals("") == false))
					|| (nationalChainUser != null && nationalChainUser.equals("Y") == true)) {
				sql += ",content_details cdt";
			}
			// CONTENT_INDUSTRY
			if ((nationalChainUser != null && nationalChainUser.equals("Y") == true) || (industry != null && industry.equals("") != true)) {

				sql += ",content_industry ci";

			}

			/* ENTRY DATE AND ACTIVATE CHECK */
			sql += " where c.activate =1 and c.state_id=s.state_id and c.id=ps.content_id  and c.county_id=gc.county_id ";
			// NEW/UPDATED FLAG
			if (newUpdatedFlag == 1) {
				sql += "AND c.new_updated = 'N'";

			}

			/* FOR EDITION DAYS */

			if (editionDays != null) {

				sql += "AND entry_date >= dateadd(day,-" + editionDays + ",getdate())";

			}

			/* stateIds */

			if (stateIds != null) {

				sql += "AND s.state_id in (" + stateIds + ")";

			}

			/* FOR FILTER BASED ON COUNTIES SUBSCRIBED BY USER */

			if (counties != null && !counties.equals("")) {

				sql += "AND c.county_id in (" + counties + ")";

			}

			else if (userCountyIds != null) {

				sql += "AND c.county_id in (" + userCountyIds + ")";

			}

			/* NATIONAL CHAIN FLAG CHECK */
			if (nationalChainUser != null && nationalChainUser.equals("Y") == true) {
				sql += "and c.id=cdt.content_id and cdt.national_chain='Y'";
			}

			/* Section */

			if (sectionIdList != null && !sectionIdList.equals("")) {

				sql += "and ps.section_id in (" + sectionIdList + ")";

			}

			/* Sub Section */

			if (subSectionList != null && !subSectionList.equals("")) {

				sql += "and sub_section in ('" + subSectionList.replaceAll(",", "','") + "')";

			}

			// Throttling of search query
			sql +=
					" AND ((substring(cdc_id,len(cdc_id),1) in (1,3,5,7,9) and job_type like  'public%') "
							+ " OR (substring(cdc_id,len(cdc_id),1) in (5) and job_type like 'private%'))";

			// JOB TYPE-(PRIVATE/PUBLIC)
			if (jobType != null && !jobType.equals("")) {

				String bidType[] = null;

				bidType = jobType.split("\\,");

				for (int i = 0; i < bidType.length; i++) {

					if (i == 0) {

						sql += " and( c.job_type like (" + bidType[i] + ")";

					} else {

						sql += " or c.job_type like (" + bidType[i] + ")";

					}

				}

				sql += ")";

			}

			/* Divisions */
			if (divisionIdList != null && !(divisionIdList.equals(""))) {

				sql +=
						"   AND  c.id=cd.content_id   AND 	(cd.division_2004_id in (SELECT node.division_2004_id FROM  "
								+ " divisions_2004 AS node, divisions_2004 AS parent  WHERE node.lft BETWEEN parent.lft AND "
								+ "  parent.rgt AND parent.division_2004_id in (" + divisionIdList + ")) )";

			}

			/* CONSTRUCTION TYPE */

			String Cons[] = null;
			if (constructionTypes != null && constructionTypes.equals("") != true) {
				Cons = constructionTypes.split("\\,");
				// String a = "";

				for (int x = 0; x < Cons.length; x++) {

					// String num;
					if (x == 0) {
						sql += "and (";
					} else {
						sql += " or ";
					}
					sql += " (c.const_" + Cons[x] + "='Y'";

					if (unit != null && unit.equals("") != true) {

						sql += "and c.sqrft_unit_" + Cons[x] + "='" + unit + "'";

					}
					if (unitMin != null && unitMin.equals("") != true) {
						sql += " and c.sqrft_" + Cons[x] + ">=" + unitMin + "";

					}
					if (unitMax != null && unitMax.equals("") != true) {
						sql += " and c.sqrft_" + Cons[x] + "<=" + unitMax + "";

					}
					sql += ")";

				}
				sql += ")";
			} else {
				if (unit != null && unit.equals("") != true) {

					sql += "and c.sqrft_unit ='" + unit + "'";

				}
				if (unitMin != null && unitMin.equals("") != true) {
					sql += "and c.sqrft>=" + unitMin + "";

				}
				if (unitMax != null && unitMax.equals("") != true) {
					sql += " and c.sqrft<=" + unitMax + "";

				}

			}
			// END OF CONSTRUCTION TYPE

			/* Planning Stages */

			if (planningStages != null && !planningStages.equals("")) {

				String planningStagesNew = LoginUtil.getPlanningStages(planningStages, con);

				String planStage[] = null;

				planStage = planningStagesNew.split("\\,");

				for (int i = 0; i < planStage.length; i++) {

					if (i == 0) {

						sql += " and( c.planningstages like (" + "\'" + "%" + planStage[i] + "%" + "\'" + ")";

					} else {

						sql += " or c.planningstages like (" + "\'" + "%" + planStage[i] + "%" + "\'" + ")";

					}

				}

				sql += ")";

			}
			// END OF PLANNING STAGES
			// CONTRACTING METHOD

			String contract = null;
			if (conMethod != null && !conMethod.equals("")) {

				String contractMethodarr[] = conMethod.split(",");
				for (int x = 0; x < contractMethodarr.length; x++) {

					String a = (String) contractMethodarr[x];
					int index = a.indexOf("|");
					contract = a.substring(index + 1);

					if (x == 0) {
						sql += "and(";

						sql += "c.con_method like (" + "\'" + "%" + contract + "%" + "\'" + ")";

					} else {

						sql += " or c.con_method like (" + "\'" + "%" + contract + "%" + "\'" + ")";

					}

				}
				sql += ")";
			}

			// END OF CONTRACTING METHOD

			// STORIES FROM AND TO

			String ConstStories[] = null;
			if ((constructionTypes != null && constructionTypes.equals("") != true)
					&& ((storiesMin != null && storiesMin.equals("") == false) || (storiesMax != null && storiesMax.equals("") == false))) {
				ConstStories = constructionTypes.split("\\,");

				for (int x = 0; x < ConstStories.length; x++) {

					if (x == 0) {
						sql += "AND  c.id=cdt.content_id  and (";
					} else {
						sql += "OR ";
					}

					if (storiesMin != null && storiesMin.equals("") == false && storiesMax.equals("") == true) {
						sql += " ( cdt.stories_" + ConstStories[x] + ">=" + storiesMin + "";

					}
					if (storiesMax != null && storiesMax.equals("") == false && storiesMin.equals("") == true) {
						sql += " cdt.stories_" + ConstStories[x] + "<=" + storiesMax;

					}
					if ((storiesMin != null && storiesMin.equals("") == false) && (storiesMax != null && storiesMax.equals("") == false)) {
						sql += "  cdt.stories_" + ConstStories[x] + ">=" + storiesMin + "";

					}
					if ((storiesMin != null && storiesMin.equals("") == false) && (storiesMax != null && storiesMax.equals("") == false)) {
						sql += "AND  cdt.stories_" + ConstStories[x] + "<=" + storiesMax;

					}

				}
				sql += ")";
			}

			else if ((storiesMin != null && storiesMin.equals("") == false) || (storiesMax != null && storiesMax.equals("") == false)) {

				sql += "AND  c.id=cdt.content_id  ";
				if (storiesMin.equals("") == false && storiesMax.equals("") == false) {
					sql +=
							("and ((cdt.stories_new>=" + storiesMin + " and cdt.stories_new<=" + storiesMax + " )or  " + " (cdt.stories_alt>=" + storiesMin
									+ " and cdt.stories_alt<=" + storiesMax + ")or " + " (cdt.stories_add>=" + storiesMin + " and cdt.stories_add<="
									+ storiesMax + ") or " + " (cdt.stories_ren>=" + storiesMin + " and cdt.stories_ren<=" + storiesMax + "))");

				}

				else if (storiesMin.equals("") == false && storiesMax.equals("") == true) {
					sql +=
							("and (cdt.stories_new>=" + storiesMin + " OR " + " cdt.stories_alt>=" + storiesMin + " OR  " + " cdt.stories_add>=" + storiesMin
									+ " OR  " + " cdt.stories_ren>=" + storiesMin + ")");

				} else if (storiesMin.equals("") == true && storiesMax.equals("") == false) {
					sql +=
							("and (cdt.stories_new<=" + storiesMax + " OR " + " cdt.stories_alt<=" + storiesMax + " OR  " + " cdt.stories_add<=" + storiesMax
									+ " OR  " + " cdt.stories_ren<=" + storiesMax + ")");

				}

			}

			/************************ INDUSTRY *********************************/

			String industryType = null;

			if ((industry != null && industry.equals("") == false)) {

				String word[] = null;
				String indusVal = "";
				word = industry.split("\\,");
				sql += "and c.id=ci.content_id ";
				for (int i = 0; i < word.length; i++) {

					if (i == 0) {

						indusVal = word[i];
						industryType = "'" + indusVal + "'";
					} else {
						indusVal = word[i];
						industryType = industryType + ",'" + indusVal + "'";

					}

				}

				// sql+=")";
				sql += "and  ci.industry in(" + industryType + ")";
			}

			// END OF INDUSTRY TYPE

			/************************ SUB-INDUSTRY *********************************/

			String subIndustryType = null;

			if (subIndustry != null && subIndustry.equals("") != true) {

				String word[] = null;
				// String subindusVal = "";
				word = subIndustry.split("\\,");

				for (int i = 0; i < word.length; i++) {

					if (i == 0) {

						subIndustryType = "'" + word[i] + "'";

					} else {

						subIndustryType = subIndustryType + ",'" + word[i] + "'";

					}

				}

				// sql+=")";
				sql += "and  ci.sub_industry in(" + subIndustryType + ")";
			}

			// END OF SUB INDUSTRY

			/*********** Bid Date ****************/

			if (bidDateFrom != null && bidDateFrom.equals("") == false) {

				sql += " and c.bid_date >='" + bidDateFrom + "' ";
			}
			if (bidDateTo != null && bidDateTo.equals("") == false) {
				sql += " and c.bid_date <='" + bidDateTo + "' ";
			}

			/************* AMOUNT HIGH AND LOW *************/

			if (estiAmountHigh.equals("") == false && sessionLowAmount.equals("") == false && estiAmountHigh != null && sessionLowAmount != null) {

				sql +=
						" and (c.estimated_amount_lower <=" + estiAmountHigh + " and c.estimated_amount_lower >=" + sessionLowAmount
								+ ") and c.estimated_amount_upper <=" + estiAmountHigh + "";

			} else if (sessionLowAmount.equals("") == false && sessionLowAmount != null) {

				sql += "and c.estimated_amount_lower>=" + sessionLowAmount + "";

			} else if (estiAmountHigh.equals("") == false && estiAmountHigh != null) {

				sql += "and c.estimated_amount_upper <=" + estiAmountHigh + " and c.estimated_amount_lower<=" + estiAmountHigh + "";

			}

			// Keywords Search if searchText is present.
			searchText = projectKeywords;
			String formattedKeywordQuery = null;
			// log.info("searchText"+searchText);
			if (!searchText.equals("") && searchText != null) {
				formattedKeywordQuery = LibraryFunctions.getKeywordFormattedQueryString(searchText);
				sql += formattedKeywordQuery;
				// log.info("formattedKeywordQuery"+formattedKeywordQuery);
			}
			// end of keywords search if searchText is present.

			// formattedContactKeywordQuery
			// Keywords Search if contactSearchText is present.
			contactSearchText = contactKeywords;
			String formattedContactKeywordQuery = null;
			if (!contactSearchText.equals("") && contactSearchText != null) {
				formattedContactKeywordQuery = LibraryFunctions.getContactKeywordFormattedQueryString(contactSearchText);
				sql += formattedContactKeywordQuery;
				// log.info("formattedContactKeywordQuery"+formattedContactKeywordQuery);
			}
			// end of keywords search if searchText is present.

			// REFINED KEYWORD-PROJECTS
			String formattedPKeywordRefineQuery = null;
			if (!refinePKeywordText.equals("") && refinePKeywordText != null) {
				formattedPKeywordRefineQuery = LibraryFunctions.getKeywordFormattedQueryString(refinePKeywordText);
				sql += formattedPKeywordRefineQuery;
			}

			// END OF REFINED KEYWORDS-PROJECTS

			// REFINED KEYWORD-CONTACTS
			String formattedCKeywordRefineQuery = null;
			if (!refineCKeywordText.equals("") && refineCKeywordText != null) {
				formattedCKeywordRefineQuery = LibraryFunctions.getKeywordFormattedQueryString(refineCKeywordText);
				sql += formattedCKeywordRefineQuery;
			}

			sql += " order by " + sortType + " " + sortOrder + " , entry_date desc,id";

			log.info(sql);
			stmt = con.createStatement();
			rs = stmt.executeQuery(sql);

			while (rs.next()) {
				contentIdList.add(String.valueOf((rs.getInt("id"))));

			} // while.
			stmt.close();
			size = contentIdList.size();
			/*
			 * startIndex = (pageNumber - 1) * recordsPerPage; endIndex = pageNumber * (recordsPerPage - 1); endIndex = endIndex + 1;
			 */
			startIndex = (pageNumber - 1) * recordsPerPage;
			endIndex = (pageNumber * recordsPerPage) - 1;
			endIndex = endIndex + 1;

			if (endIndex > contentIdList.size()) {
				endIndex = contentIdList.size();
			}

		} catch (SQLException se) {
			log.error("!exception3.2!SQL error in getAdvancedSearchContentIdsPL" + se.toString());
		} catch (Exception e) {
			log.error("!exception3.2!SQL error in getAdvancedSearchContentIdsPL" + e.toString());

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}

			} catch (SQLException se) {
				log.error("!exception3.2!SQL error in getAdvancedSearchContentIdsPL" + se.toString());

			} finally {
				try {
					if (stmt != null) {
						stmt.close();
					}
				} catch (SQLException se) {
					log.error("!exception3.3!SQL error in getAdvancedSearchContentIdsPL" + se.toString());

				}
			}

			JDBCUtil.releaseDBConnection(con);

		}
		if (contentIdList.size() > 0) {

			return contentIdList.subList(startIndex, endIndex);
		} else {
			return null;
		}

	} //

	@RequestMapping(value = "/getContentDetailsPL")
	public String getContentDetailsPL(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey,
			@RequestParam("stateIds") String stateIds, @RequestParam("editionDays") String editionDays, @RequestParam("sectionIdList") String sectionIdList,
			@RequestParam("newUpdatedFlag") int newUpdatedFlag, @RequestParam("subSectionList") String subSectionList,
			@RequestParam("constructionTypes") String constructionTypes, @RequestParam("divisionIdList") String divisionIdList,
			@RequestParam("projectKeywords") String projectKeywords, @RequestParam("contactKeywords") String contactKeywords,
			@RequestParam("planningStages") String planningStages, @RequestParam("conMethod") String conMethod, @RequestParam("industry") String industry,
			@RequestParam("subIndustry") String subIndustry, @RequestParam("estiAmountHigh") String estiAmountHigh,
			@RequestParam("sessionLowAmount") String sessionLowAmount, @RequestParam("unit") String unit, @RequestParam("unitMin") String unitMin,
			@RequestParam("unitMax") String unitMax, @RequestParam("storiesMin") String storiesMin, @RequestParam("storiesMax") String storiesMax,
			@RequestParam("bidDateFrom") String bidDateFrom, @RequestParam("bidDateTo") String bidDateTo, @RequestParam("showAll") int showAll,
			@RequestParam("displayMode") String displayMode, @RequestParam("jobType") String jobType, @RequestParam("countyIds") String countyIds,
			@RequestParam("pageNumber") int pageNumber, @RequestParam("recordsPerPage") int recordsPerPage, @RequestParam("sortOrder") String sortOrder,
			@RequestParam("sortType") String sortType, @RequestParam("searchText") String searchText,
			@RequestParam("contactSearchText") String contactSearchText, @RequestParam("refinePKeywordText") String refinePKeywordText,
			@RequestParam("refineCKeywordText") String refineCKeywordText) {

		Map<String, Object> map = null;
		Gson gson = null;

		log.info("called");
		String contentIds = null;
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

				contentIds =
						LibraryFunctions.ListToString(getAdvancedSearchContentIdsPL(sessionId, securityKey, stateIds, editionDays, sectionIdList,
								newUpdatedFlag, subSectionList, constructionTypes, divisionIdList, projectKeywords, contactKeywords, planningStages, conMethod,
								industry, subIndustry, estiAmountHigh, sessionLowAmount, unit, unitMin, unitMax, storiesMin, storiesMax, bidDateFrom,
								bidDateTo, showAll, displayMode, jobType, countyIds, pageNumber, recordsPerPage, sortOrder, sortType, searchText,
								contactSearchText, refinePKeywordText, refineCKeywordText));
				int totRecords = size;

				// log.info("ADVANCED SEARCH CONTENT IDS: "+ContentIds);
				map.put("sEcho", "1");
				// map.put("iTotalRecords", String.valueOf(recordsPerPage));
				map.put("iTotalRecords", String.valueOf(totRecords));
				map.put("iTotalDisplayRecords", String.valueOf(totRecords));

				map.put("aaData", LoginUtil.getBriefProjectDetailsPL(contentIds, sessionId, sortOrder, sortType, con));

				log.info(gson.toJson(map));

				size = 0;

				// ArrayList projectDetailsTitles = new ArrayList();
				// projectDetailsTitles.add("sTitle");
				// map.put("aoColumns",projectDetailsTitles);

			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Exception in getContentDetails() for advanced search " + ex.getMessage());
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}
		return gson.toJson(map);

	}

	public String getAdvancedSearchContentIds(int sessionId, String securityKey, String stateIds, String editionDays, String sectionIdList, int newUpdatedFlag,
			String subSectionList, String constructionTypes, String divisionIdList, String projectKeywords, String contactKeywords, String planningStages,
			String conMethod, String industry, String subIndustry, String estiAmountHigh, String sessionLowAmount, String unit, String unitMin, String unitMax,
			String storiesMin, String storiesMax, String bidDateFrom, String bidDateTo, int showAll, String displayMode, String jobType, String counties) {

		Connection con = null;
		ResultSet rs = null;
		Statement stmt = null;
		String sql = null;
		// String userStateIdList = null;
		String userCountyIdList = null;
		// String geoUserStateIdList = null;
		String geoUserCountyIdList = null;
		// String userStateIds = null;
		String userCountyIds = null;
		String nationalChainUser = null;
		LeadManagerSessionData lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);
		// USER SUBSCRIBED STATE AND COUNTY LIST
		// userStateIdList = lmData.getUserStateList();
		userCountyIdList = lmData.getUserCountyList();
		// geoUserStateIdList = lmData.getGeoUserStateList();
		geoUserCountyIdList = lmData.getGeoUserCountyList();

		/* User state list */
		/*
		 * if (geoUserStateIdList != null && userStateIdList != null) {
		 * 
		 * userStateIds = userStateIdList + "," + geoUserStateIdList; } else if (userStateIdList != null) {
		 * 
		 * userStateIds = userStateIdList; } else if (geoUserStateIdList != null) {
		 * 
		 * userStateIds = geoUserStateIdList; }
		 */
		/* User county list */
		if (geoUserCountyIdList != null && userCountyIdList != null) {

			userCountyIds = userCountyIdList + "," + geoUserCountyIdList;
		} else if (userCountyIdList != null) {

			userCountyIds = userCountyIdList;
		} else if (geoUserCountyIdList != null) {

			userCountyIds = geoUserCountyIdList;
		}

		String contentIdList = null;

		try {
			if (con == null) {
				con = JDBCUtil.getDBConnectionFromDatabase();
			}

			// SHOW ALL
			if (showAll == 1) {
				sql =
						"select distinct c.id,c.cdc_id,c.publication_id,c.section_id,c.sub_section,"
								+ " c.title,c.state_id,c.estimated_amount_lower,c.bid_date,c.prebid_mtg,c.bids_details, "
								+ " c.plan_express,c.entry_date,c.short_cdcid,c.state_multiple,c.county_multiple,"
								+ " c.plan_availability_status,c.leads_id,c.county_id,gc.county_name,s.state_abb "
								+ " from content c,pub_section ps,state s,county gc";
			} else {
				sql =
						"select distinct top 180 c.id,c.cdc_id,c.publication_id,c.section_id,c.sub_section,"
								+ " c.title,c.state_id,c.estimated_amount_lower,c.bid_date,c.prebid_mtg,c.bids_details, "
								+ " c.plan_express,c.entry_date,c.short_cdcid,c.state_multiple,c.county_multiple,"
								+ " c.plan_availability_status,c.leads_id,c.county_id,gc.county_name,s.state_abb "
								+ " from content c,pub_section ps,state s,county gc";

			}
			// CONTENT_DIVISIONS
			if (divisionIdList != null && divisionIdList.equals("") != true) {

				sql += ",content_divisions_2004 cd";

			}

			// CONTENT_DETAILS
			if (((storiesMin != null && storiesMin.equals("") == false) || (storiesMax != null && storiesMax.equals("") == false))
					|| (nationalChainUser != null && nationalChainUser.equals("Y") == true)) {
				sql += ",content_details cdt";
			}
			// CONTENT_INDUSTRY
			if ((nationalChainUser != null && nationalChainUser.equals("Y") == true) || (industry != null && industry.equals("") != true)) {

				sql += ",content_industry ci";

			}

			/* ENTRY DATE AND ACTIVATE CHECK */
			sql += " where c.activate =1 and c.state_id=s.state_id and c.id=ps.content_id  and c.county_id=gc.county_id ";
			// NEW/UPDATED FLAG
			if (newUpdatedFlag == 1) {
				sql += "AND c.new_updated = 'N'";

			}

			/* FOR EDITION DAYS */

			if (editionDays != null) {

				sql += "AND entry_date >= dateadd(day,-" + editionDays + ",getdate())";

			}

			/* stateIds */

			if (stateIds != null) {

				sql += "AND s.state_id in (" + stateIds + ")";

			}

			/* FOR FILTER BASED ON COUNTIES SUBSCRIBED BY USER */

			if (counties != null && !counties.equals("")) {

				sql += "AND c.county_id in (" + counties + ")";

			}

			else if (userCountyIds != null) {

				sql += "AND c.county_id in (" + userCountyIds + ")";

			}

			/* NATIONAL CHAIN FLAG CHECK */
			if (nationalChainUser != null && nationalChainUser.equals("Y") == true) {
				sql += "and c.id=cdt.content_id and cdt.national_chain='Y'";
			}

			/* Section */

			if (sectionIdList != null && !sectionIdList.equals("")) {

				sql += "and ps.section_id in (" + sectionIdList + ")";

			}

			/* Sub Section */

			if (subSectionList != null && !subSectionList.equals("")) {

				sql += "and sub_section in ('" + subSectionList.replaceAll(",", "','") + "')";

			}

			// JOB TYPE-(PRIVATE/PUBLIC)
			if (jobType != null && !jobType.equals("")) {

				String bidType[] = null;

				bidType = jobType.split("\\,");

				for (int i = 0; i < bidType.length; i++) {

					if (i == 0) {

						sql += " and( c.job_type like (" + bidType[i] + ")";

					} else {

						sql += " or c.job_type like (" + bidType[i] + ")";

					}

				}

				sql += ")";

			}

			/* Divisions */
			if (divisionIdList != null && !(divisionIdList.equals(""))) {

				sql +=
						"   AND  c.id=cd.content_id   AND 	(cd.division_2004_id in (SELECT node.division_2004_id FROM  "
								+ " divisions_2004 AS node, divisions_2004 AS parent  WHERE node.lft BETWEEN parent.lft AND "
								+ "  parent.rgt AND parent.division_2004_id in (" + divisionIdList + ")) OR cd.division_2004_id in (" + divisionIdList + "))";

			}

			/* CONSTRUCTION TYPE */

			String Cons[] = null;
			if (constructionTypes != null && constructionTypes.equals("") != true) {
				Cons = constructionTypes.split("\\,");
				// String a = "";

				for (int x = 0; x < Cons.length; x++) {

					// String num;
					if (x == 0) {
						sql += "and (";
					} else {
						sql += " or ";
					}
					sql += " (c.const_" + Cons[x] + "='Y'";

					if (unit != null && unit.equals("") != true) {

						sql += "and c.sqrft_unit_" + Cons[x] + "='" + unit + "'";

					}
					if (unitMin != null && unitMin.equals("") != true) {
						sql += " and c.sqrft_" + Cons[x] + ">=" + unitMin + "";

					}
					if (unitMax != null && unitMax.equals("") != true) {
						sql += " and c.sqrft_" + Cons[x] + "<=" + unitMax + "";

					}
					sql += ")";

				}
				sql += ")";
			} else {
				if (unit != null && unit.equals("") != true) {

					sql += "and c.sqrft_unit ='" + unit + "'";

				}
				if (unitMin != null && unitMin.equals("") != true) {
					sql += "and c.sqrft>=" + unitMin + "";

				}
				if (unitMax != null && unitMax.equals("") != true) {
					sql += " and c.sqrft<=" + unitMax + "";

				}

			}
			// END OF CONSTRUCTION TYPE

			/* Planning Stages */

			if (planningStages != null && !planningStages.equals("")) {

				String planningStagesNew = LoginUtil.getPlanningStages(planningStages, con);

				String planStage[] = null;

				planStage = planningStagesNew.split("\\,");

				for (int i = 0; i < planStage.length; i++) {

					if (i == 0) {

						sql += " and( c.planningstages like (" + "\'" + "%" + planStage[i] + "%" + "\'" + ")";

					} else {

						sql += " or c.planningstages like (" + "\'" + "%" + planStage[i] + "%" + "\'" + ")";

					}

				}

				sql += ")";

			}
			// END OF PLANNING STAGES
			// CONTRACTING METHOD

			String contract = null;
			if (conMethod != null && !conMethod.equals("")) {

				String contractMethodarr[] = conMethod.split(",");
				for (int x = 0; x < contractMethodarr.length; x++) {

					String a = (String) contractMethodarr[x];
					int index = a.indexOf("|");
					contract = a.substring(index + 1);

					if (x == 0) {
						sql += "and(";

						sql += "c.con_method like (" + "\'" + "%" + contract + "%" + "\'" + ")";

					} else {

						sql += " or c.con_method like (" + "\'" + "%" + contract + "%" + "\'" + ")";

					}

				}
				sql += ")";
			}

			// END OF CONTRACTING METHOD

			// STORIES FROM AND TO

			String ConstStories[] = null;
			if ((constructionTypes != null && constructionTypes.equals("") != true)
					&& ((storiesMin != null && storiesMin.equals("") == false) || (storiesMax != null && storiesMax.equals("") == false))) {
				ConstStories = constructionTypes.split("\\,");

				for (int x = 0; x < ConstStories.length; x++) {

					if (x == 0) {
						sql += "AND  c.id=cdt.content_id  and (";
					} else {
						sql += "OR ";
					}

					if (storiesMin != null && storiesMin.equals("") == false && storiesMax.equals("") == true) {
						sql += " ( cdt.stories_" + ConstStories[x] + ">=" + storiesMin + "";

					}
					if (storiesMax != null && storiesMax.equals("") == false && storiesMin.equals("") == true) {
						sql += " cdt.stories_" + ConstStories[x] + "<=" + storiesMax;

					}
					if ((storiesMin != null && storiesMin.equals("") == false) && (storiesMax != null && storiesMax.equals("") == false)) {
						sql += "  cdt.stories_" + ConstStories[x] + ">=" + storiesMin + "";

					}
					if ((storiesMin != null && storiesMin.equals("") == false) && (storiesMax != null && storiesMax.equals("") == false)) {
						sql += "AND  cdt.stories_" + ConstStories[x] + "<=" + storiesMax;

					}

				}
				sql += ")";
			}

			else if ((storiesMin != null && storiesMin.equals("") == false) || (storiesMax != null && storiesMax.equals("") == false)) {

				sql += "AND  c.id=cdt.content_id  ";
				if (storiesMin.equals("") == false && storiesMax.equals("") == false) {
					sql +=
							("and ((cdt.stories_new>=" + storiesMin + " and cdt.stories_new<=" + storiesMax + " )or  " + " (cdt.stories_alt>=" + storiesMin
									+ " and cdt.stories_alt<=" + storiesMax + ")or " + " (cdt.stories_add>=" + storiesMin + " and cdt.stories_add<="
									+ storiesMax + ") or " + " (cdt.stories_ren>=" + storiesMin + " and cdt.stories_ren<=" + storiesMax + "))");

				}

				else if (storiesMin.equals("") == false && storiesMax.equals("") == true) {
					sql +=
							("and (cdt.stories_new>=" + storiesMin + " OR " + " cdt.stories_alt>=" + storiesMin + " OR  " + " cdt.stories_add>=" + storiesMin
									+ " OR  " + " cdt.stories_ren>=" + storiesMin + ")");

				} else if (storiesMin.equals("") == true && storiesMax.equals("") == false) {
					sql +=
							("and (cdt.stories_new<=" + storiesMax + " OR " + " cdt.stories_alt<=" + storiesMax + " OR  " + " cdt.stories_add<=" + storiesMax
									+ " OR  " + " cdt.stories_ren<=" + storiesMax + ")");

				}

			}

			/************************ INDUSTRY *********************************/

			String industryType = null;

			if ((industry != null && industry.equals("") == false)) {

				String word[] = null;
				String indusVal = "";
				word = industry.split("\\,");
				sql += "and c.id=ci.content_id ";
				for (int i = 0; i < word.length; i++) {

					if (i == 0) {

						indusVal = word[i];
						industryType = "'" + indusVal + "'";
					} else {
						indusVal = word[i];
						industryType = industryType + ",'" + indusVal + "'";

					}

				}

				// sql+=")";
				sql += "and  ci.industry in(" + industryType + ")";
			}

			// END OF INDUSTRY TYPE

			/************************ SUB-INDUSTRY *********************************/

			String subIndustryType = null;

			if (subIndustry != null && subIndustry.equals("") != true) {

				String word[] = null;
				// String subindusVal = "";
				word = subIndustry.split("\\,");

				for (int i = 0; i < word.length; i++) {

					if (i == 0) {

						subIndustryType = "'" + word[i] + "'";

					} else {

						subIndustryType = subIndustryType + ",'" + word[i] + "'";

					}

				}

				// sql+=")";
				sql += "and  ci.sub_industry in(" + subIndustryType + ")";
			}

			// END OF SUB INDUSTRY

			/*********** Bid Date ****************/

			if (bidDateFrom != null && bidDateFrom.equals("") == false) {

				sql += " and c.bid_date >='" + bidDateFrom + "' ";
			}
			if (bidDateTo != null && bidDateTo.equals("") == false) {
				sql += " and c.bid_date <='" + bidDateTo + "' ";
			}

			/************* AMOUNT HIGH AND LOW *************/

			if (estiAmountHigh.equals("") == false && sessionLowAmount.equals("") == false && estiAmountHigh != null && sessionLowAmount != null) {

				sql +=
						" and (c.estimated_amount_lower <=" + estiAmountHigh + " and c.estimated_amount_lower >=" + sessionLowAmount
								+ ") and c.estimated_amount_upper <=" + estiAmountHigh + "";

			} else if (sessionLowAmount.equals("") == false && sessionLowAmount != null) {

				sql += "and c.estimated_amount_lower>=" + sessionLowAmount + "";

			} else if (estiAmountHigh.equals("") == false && estiAmountHigh != null) {

				sql += "and c.estimated_amount_upper <=" + estiAmountHigh + " and c.estimated_amount_lower<=" + estiAmountHigh + "";

			}

			// ESTIMATED HIGH LOW AND HIGH

			log.info(sql);
			stmt = con.createStatement();
			rs = stmt.executeQuery(sql);

			while (rs.next()) {
				if (contentIdList == null) {
					contentIdList = String.valueOf(rs.getInt("id"));
				} else {
					contentIdList = contentIdList + "," + String.valueOf(rs.getInt("id"));
				}

			} // while.
			stmt.close();
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
				log.error("!exception3.2!SQL error in getContentIds" + se);

			} finally {
				try {
					if (stmt != null) {
						stmt.close();
					}
				} catch (SQLException se) {
					log.error("!exception3.3!SQL error in getContentIds" + se);

				}

				JDBCUtil.releaseDBConnection(con);

			}
		}
		return contentIdList;
	} //

	@RequestMapping(value = "/getSubIndustryInfo")
	public String getSubIndustryInfo(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey,
			@RequestParam("industryIds") String industryIds, @RequestParam("typeId") int typeId) {

		Map<String, Object> map = null;
		Gson gson = null;

		Connection con = null;
		// String contentIds = null;

		try {
			gson = new Gson();
			map = new HashMap<String, Object>();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			boolean validSessionId = false;
			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			if (validSessionId == true) {

				map.put("aaData", LoginUtil.getSubIndustries(typeId, industryIds, con));

			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Exception in getSubIndustryInfo for advanced search " + ex.getMessage());
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);

	}

	@RequestMapping(value = "/getCountyInfo")
	public String getCountyInfo(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey,
			@RequestParam("stateIds") String stateIds, @RequestParam("typeId") int typeId) {

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

			if (validSessionId == true) {

				log.info("county info");

				map.put("aaData", LoginUtil.getCountyInfo(typeId, stateIds, con));

			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Exception in getSubIndustryInfo for advanced search " + ex.getMessage());
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);

	}

	@RequestMapping(value = "/getContentDetails")
	public String getContentDetails(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey,
			@RequestParam("stateIds") String stateIds, @RequestParam("editionDays") String editionDays, @RequestParam("sectionIdList") String sectionIdList,
			@RequestParam("newUpdatedFlag") int newUpdatedFlag, @RequestParam("subSectionList") String subSectionList,
			@RequestParam("constructionTypes") String constructionTypes, @RequestParam("divisionIdList") String divisionIdList,
			@RequestParam("projectKeywords") String projectKeywords, @RequestParam("contactKeywords") String contactKeywords,
			@RequestParam("planningStages") String planningStages, @RequestParam("conMethod") String conMethod, @RequestParam("industry") String industry,
			@RequestParam("subIndustry") String subIndustry, @RequestParam("estiAmountHigh") String estiAmountHigh,
			@RequestParam("sessionLowAmount") String sessionLowAmount, @RequestParam("unit") String unit, @RequestParam("unitMin") String unitMin,
			@RequestParam("unitMax") String unitMax, @RequestParam("storiesMin") String storiesMin, @RequestParam("storiesMax") String storiesMax,
			@RequestParam("bidDateFrom") String bidDateFrom, @RequestParam("bidDateTo") String bidDateTo, @RequestParam("showAll") int showAll,
			@RequestParam("displayMode") String displayMode, @RequestParam("jobType") String jobType, @RequestParam("countyIds") String countyIds) {

		Map<String, Object> map = null;
		Gson gson = null;

		Connection con = null;
		String ContentIds = null;

		try {
			gson = new Gson();
			map = new HashMap<String, Object>();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			boolean validSessionId = false;
			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			log.info(validSessionId);
			if (validSessionId == true) {

				ContentIds =
						getAdvancedSearchContentIds(sessionId, securityKey, stateIds, editionDays, sectionIdList, newUpdatedFlag, subSectionList,
								constructionTypes, divisionIdList, projectKeywords, contactKeywords, planningStages, conMethod, industry, subIndustry,
								estiAmountHigh, sessionLowAmount, unit, unitMin, unitMax, storiesMin, storiesMax, bidDateFrom, bidDateTo, showAll, displayMode,
								jobType, countyIds);

				// log.info("ADVANCED SEARCH CONTENT IDS: "+ContentIds);

				map.put("aaData", LoginUtil.getBriefProjectDetails(ContentIds, sessionId, con));

				// ArrayList projectDetailsTitles = new ArrayList();
				// projectDetailsTitles.add("sTitle");
				// map.put("aoColumns",projectDetailsTitles);

			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("Exception in getContentDetails() for advanced search " + ex.getMessage());
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}
		return gson.toJson(map);

	}

}
