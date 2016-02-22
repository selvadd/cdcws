/**
 * 
 */
package com.cdc.ws.savedsearch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import wsutils.WebUsageUtil;

import com.google.gson.Gson;

/**
 * @author Selva
 *
 */

@RestController
@RequestMapping(value = "/services")
public class SavedSearchResultsController {

	private Logger log = Logger.getLogger(SavedSearchResultsController.class.getName());

	int size = 0;

	/**
	 * compareDates - checks whether the 2 given dates are equal, greater than or equal, less than or equal, greater than and less than
	 * 
	 * @param date1
	 *            - date string in MM/DD/YYYY format
	 * @param date2
	 *            - date string in MM/DD/YYYY format
	 * @return int code to identify equality status return codes are returned as LeadsConfig.<CONSTANT_NAME> return code EQ(0) when two dates are EQUAL return
	 *         code GT(1) when date 1 is greater than date 2 return code LT(2) when date 1 is less than date 2 returns -1 on EXCEPTION
	 */

	public static int compareDates(String date1, String date2) {

		// Locale usLocale = new Locale("EN", "us");
		int code = -1;
		if (date1 == null || date2 == null)
			return -1;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
			Date fromDate = sdf.parse(date1);
			Date toDate = sdf.parse(date2);

			if (fromDate.equals(toDate)) {
				code = 0;
				return code;
			} else if (fromDate.after(toDate)) {
				code = 1;
				return code;
			} else if (fromDate.before(toDate)) {
				code = 2;
				return code;
			}

		} catch (Exception e) {
			code = -1;
		}
		return code;

	} // End of compareDates()

	// get SAVED SEARCH TODAY RUN DATE
	public String getLastRunDate(String loginId, String searchName) {

		String sql = null;
		Connection con = null;
		PreparedStatement prepStmt = null;
		ResultSet rs = null;
		String todayRunDate = "N";
		// int compareDate = 0;

		try {

			con = JDBCUtil.getDBConnectionFromDatabase();

			sql = "SELECT  today_run_date,last_run_date FROM emailHotList " + " WHERE login_id='" + loginId + "' and saved_search_name = '" + searchName + "'";

			prepStmt = con.prepareStatement(sql);
			rs = prepStmt.executeQuery();

			while (rs.next()) {

				todayRunDate = rs.getString("today_run_date");

				if (compareDates(LibraryFunctions.getDateFromDBDate(todayRunDate), (LibraryFunctions.getTodayDate())) == 0) {

					todayRunDate = rs.getString("last_run_date");

				}

			}

		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (prepStmt != null) {
					prepStmt.close();
				}

				if (con != null) {
					con.close();
				}

			} catch (SQLException se) {
				log.error("!exception3.2!SQL error in getTodayRunDate " + se);

			} finally {
				try {
					if (prepStmt != null) {
						prepStmt.close();
					}
				} catch (SQLException se) {
					log.error("!exception3.3!SQL error in getTodayRunDate " + se);

				}
			}
		}
		return todayRunDate;

	} // end of getTodayRunDate

	public List<String> getSavedSearchContentIdsPL(int sessionId, String securityKey, String stateIds, String editionDays, String sectionIdList,
			int newUpdatedFlag, String subSectionList, String constructionTypes, String divisionIdList, String projectKeywords, String contactKeywords,
			String planningStages, String conMethod, String industry, String subIndustry, String estiAmountHigh, String estiAmountLow, String unit,
			String unitMin, String unitMax, String storiesMin, String storiesMax, String bidDateFrom, String bidDateTo, int showAll, String displayMode,
			String todayRunDate, String jobType, String counties, int pageNumber, int recordsPerPage, String sortOrder, String sortType, String searchText,
			String contactSearchText, String refinePKeywordText, String refineCKeywordText, Connection con) throws IOException {

		ResultSet rs = null;
		Statement stmt = null;
		Statement stmtCounty = null;
		ResultSet rsCounty = null;
		String sql = null;
		String sqlFinal = null;
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
		stateIds = LoginUtil.getSavedSearchStateIds(3, stateIds, con);
		industry = LoginUtil.getSavedSearchIndustryNames(1, industry, con);
		subIndustry = LoginUtil.getSavedSearchSubIndustryNames(2, subIndustry, con);
		sectionIdList = LoginUtil.getSavedSearchSectionIds(4, sectionIdList, con);
		int startIndex = 0;
		int endIndex = 0;

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

		ArrayList<String> contentIdList = new ArrayList<String>();

		try {

			// SHOW ALL
			if (showAll == 1) {
				sqlFinal =
						"select distinct id,cdc_id,job_type,bid_date,bids_details,title,  "
								+ " estimated_amount_lower,sub_section,entry_date,county_name ,state_abb,plan_availability_status"
								+ ",county_id, state_id,leads_id,leads_entry_date,city,IsProjectTracked from (";

			} else {
				sqlFinal =
						"select distinct top 1000 id,cdc_id,job_type,bid_date,bids_details,title,  "
								+ " estimated_amount_lower,sub_section,entry_date,county_name ,state_abb,plan_availability_status"
								+ ",county_id, state_id,leads_id,leads_entry_date,city,IsProjectTracked from (";

			}

			sql =
					" select distinct c.id,c.cdc_id,c.publication_id,c.section_id,c.sub_section" + ",c.title,c.state_id,c.estimated_amount_lower,c.bid_date"
							+ ",c.prebid_mtg,c.bids_details,c.plan_express,c.entry_date" + ",c.short_cdcid,c.state_multiple,c.county_multiple  "
							+ ",c.plan_availability_status,c.leads_id,c.county_id,gc.county_name" + ",s.state_abb,const_add,const_ren,const_alt,const_new"
							+ ",c.leads_entry_date,c.city,c.job_type " + ",(CASE (SELECT count(cdc_id) from save_job where cdc_id = c.cdc_id and login_id='"
							+ lmData.getLogin() + "') " + "WHEN 0 THEN 'N' ELSE 'Y' END) AS IsProjectTracked "
							+ "from content c,pub_section ps,state s,county gc";

			// CONTENT_DIVISIONS
			if (divisionIdList != null && divisionIdList.equals("") != true) {

				sql += ",content_divisions_2004 cd";

			}

			// CONTENT_DETAILS
			if (((storiesMin != null && storiesMin.equals("") == false) || (storiesMax != null && storiesMax.equals("") == false))
					|| (nationalChainUser != null && nationalChainUser.equals("Y") == true)) {
				sql += ",content_details cdt";
			}
			// CONTENT_INDUSTRY----- Need to pass the industry ids from SS table and return indus name
			if ((nationalChainUser != null && nationalChainUser.equals("Y") == true) || (industry != null && industry.equals("") != true)) {

				sql += ",content_industry ci";

			}

			/* ENTRY DATE AND ACTIVATE CHECK */
			sql += " where c.activate =1 and c.state_id=s.state_id and c.id=ps.content_id  and c.county_id=gc.county_id ";
			// NEW/UPDATED FLAG
			if (newUpdatedFlag == 1) {
				sql += "AND c.new_updated = 'N'";

			}
			// IF RUN HOTLIST BUTTON IS CLICKED
			if (todayRunDate != null && !todayRunDate.equals("N")) {

				sql += "AND entry_date >= '" + todayRunDate + "'";

				String exclusionIDs = null;
				exclusionIDs = getExcludeProjectIds(lmData.getLogin(), con);
				sql += " AND id not in(" + exclusionIDs + ")";

			}

			/* FOR EDITION DAYS -Need to check for newupdated condition */

			if ((todayRunDate != null && todayRunDate.equals("N")) && editionDays != null) {

				sql += " AND entry_date >= convert(varchar,dateadd(day,-" + editionDays + ",getdate()),101)";

			}

			// Throttling of search query
			// Commentted by Muthu Coz Result Count is diff from LM while working on LM Mobile on 10/02/14
			/*
			 * sql +=" AND ((substring(cdc_id,len(cdc_id),1) in (1,3,5,7,9) and job_type like  'public%') "+
			 * " OR (substring(cdc_id,len(cdc_id),1) in (5) and job_type like 'private%'))";
			 */

			/* NATIONAL CHAIN FLAG CHECK */
			if (nationalChainUser != null && nationalChainUser.equals("Y") == true) {
				sql += "and c.id=cdt.content_id and cdt.national_chain='Y'";
			}

			/* stateIds */

			if (stateIds != null && !stateIds.equals("")) {

				sql += "AND c.state_id in (" + stateIds + ")";

			}
			/* FOR FILTER BASED ON COUNTIES SUBSCRIBED BY USER */
			/*
			 * log("counties:"+counties); log("UserCountyids:"+UserCountyids); log.info("UserCountyids:"+UserCountyids);
			 * 
			 * 
			 * if (counties != null && !counties.equals("")) {
			 * 
			 * sql += "AND c.county_id in (" + counties + ")";
			 * 
			 * }
			 * 
			 * //FOR FILTER BASED ON COUNTIES SUBSCRIBED BY USER
			 * 
			 * if (UserCountyids != null) {
			 * 
			 * sql += "AND c.county_id in (" + UserCountyids + ")";
			 * 
			 * }
			 */
			String user_county_list_new = null;

			String countyQuery = "select distinct county_id from county where " + "county_id in (" + userCountyIds + ") and state_id in (" + stateIds + ")";

			stmtCounty = con.createStatement();

			rsCounty = stmtCounty.executeQuery(countyQuery);
			while (rsCounty.next()) {

				if (user_county_list_new == null)
					user_county_list_new = rsCounty.getString("county_id");
				else
					user_county_list_new = user_county_list_new + "," + (rsCounty.getString("county_id"));

			}

			/* counties: Selected county ids */
			/* user_county_list_new: county ids that pertaining to a particular state */
			/* userCountyIdList: All county ids that the user subscribed */
			log("counties:" + counties);
			log("user_county_list_new:" + user_county_list_new);

			/* Check if users has not selected any county. Then load all counties. Added on 10/18/12 */
			if (counties == null || counties.trim().equals("")) {
				if (user_county_list_new != null && !user_county_list_new.trim().equals("")) {
					sql += "AND gc.county_id in( " + user_county_list_new + ")";

				} else if (userCountyIds != null && !userCountyIds.trim().equals("")) {
					sql += "AND gc.county_id in( " + userCountyIds + ")";

				}
			} else {
				sql += "AND gc.county_id in( " + counties + ")";
			}

			/* NATIONAL CHAIN FLAG CHECK */
			if (nationalChainUser != null && nationalChainUser.equals("Y") == true) {
				sql += "and c.id=cdt.content_id and cdt.national_chain='Y'";
			}

			/* Section-- NEED TO GET THE SECTION IDS */

			if (sectionIdList != null && !sectionIdList.equals("")) {

				sql += "and ps.section_id in (" + sectionIdList + ")";

			}

			/* Sub Section */

			if (subSectionList != null && !subSectionList.equals("")) {

				sql += "and sub_section in ('" + subSectionList.replaceAll(",", "','") + "')";

			}

			// Throttling of search query
			// Commentted by Muthu Coz Result Count is diff from LM while working on LM Mobile on 10/02/14
			/*
			 * sql +=" AND ((substring(cdc_id,len(cdc_id),1) in (1,3,5,7,9) and job_type like  'public%') "+
			 * " OR (substring(cdc_id,len(cdc_id),1) in (5) and job_type like 'private%'))";
			 */

			// JOB TYPE-(PRIVATE/PUBLIC)
			if (jobType != null && !jobType.equals("")) {

				String bidType[] = null;

				bidType = jobType.split("\\,");

				for (int i = 0; i < bidType.length; i++) {

					if (i == 0) {

						sql += " and( c.job_type like ('" + bidType[i] + "%')";

					} else {

						sql += " or c.job_type like ('" + bidType[i] + "%')";

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

			String cons[] = null;
			if (constructionTypes != null && constructionTypes.equals("") != true) {
				cons = constructionTypes.split("\\,");
				// String a = "";

				for (int x = 0; x < cons.length; x++) {

					// String num;
					if (x == 0) {
						sql += "and (";
					} else {
						sql += " or ";
					}
					sql += " (c.const_" + cons[x] + "='Y'";

					if (unit != null && unit.equals("") != true) {

						sql += "and c.sqrft_unit_" + cons[x] + "='" + unit + "'";

					}
					if (unitMin != null && unitMin.equals("") != true) {
						sql += " and c.sqrft_" + cons[x] + ">=" + unitMin + "";

					}
					if (unitMax != null && unitMax.equals("") != true) {
						sql += " and c.sqrft_" + cons[x] + "<=" + unitMax + "";

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

			String constStories[] = null;
			if ((constructionTypes != null && constructionTypes.equals("") != true)
					&& ((storiesMin != null && storiesMin.equals("") == false) || (storiesMax != null && storiesMax.equals("") == false))) {
				constStories = constructionTypes.split("\\,");

				for (int x = 0; x < constStories.length; x++) {

					if (x == 0) {
						sql += "AND  c.id=cdt.content_id  and (";
					} else {
						sql += "OR ";
					}

					if (storiesMin != null && storiesMin.equals("") == false && storiesMax.equals("") == true) {
						sql += " ( cdt.stories_" + constStories[x] + ">=" + storiesMin + "";

					}
					if (storiesMax != null && storiesMax.equals("") == false && storiesMin.equals("") == true) {
						sql += " cdt.stories_" + constStories[x] + "<=" + storiesMax;

					}
					if ((storiesMin != null && storiesMin.equals("") == false) && (storiesMax != null && storiesMax.equals("") == false)) {
						sql += "  cdt.stories_" + constStories[x] + ">=" + storiesMin + "";

					}
					if ((storiesMin != null && storiesMin.equals("") == false) && (storiesMax != null && storiesMax.equals("") == false)) {
						sql += "AND  cdt.stories_" + constStories[x] + "<=" + storiesMax;

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

			if (estiAmountHigh != null && estiAmountLow != null && estiAmountHigh.equals("") == false && estiAmountLow.equals("") == false

			) {

				sql +=
						" and (c.estimated_amount_lower <=" + estiAmountHigh + " and c.estimated_amount_lower >=" + estiAmountLow
								+ ") and c.estimated_amount_upper <=" + estiAmountHigh + "";

			} else if (estiAmountLow != null && estiAmountLow.equals("") == false) {

				sql += "and c.estimated_amount_lower>=" + estiAmountLow + "";

			} else if (estiAmountHigh != null && estiAmountHigh.equals("") == false) {

				sql += "and c.estimated_amount_upper <=" + estiAmountHigh + " and c.estimated_amount_lower<=" + estiAmountHigh + "";

			}

			sqlFinal += sql + ") as MainContent WHERE id=id";
			// Keywords Search if searchText is present.
			searchText = projectKeywords;
			String formattedKeywordQuery = null;
			log("projectKeywords: " + projectKeywords);
			if (searchText != null && !searchText.trim().equals("")) {
				formattedKeywordQuery = LibraryFunctions.getKeywordFormattedQueryString(searchText);
				formattedKeywordQuery = formattedKeywordQuery.replaceFirst("c.id", "id");
				sqlFinal += formattedKeywordQuery;
				log("formattedKeywordQuery: " + formattedKeywordQuery);
			}
			// end of keywords search if searchText is present.

			// formattedContactKeywordQuery
			// Keywords Search if contactSearchText is present.
			contactSearchText = contactKeywords;
			log("contactKeywords: " + contactKeywords);
			String formattedContactKeywordQuery = null;
			if (contactSearchText != null && !contactSearchText.equals("")) {
				formattedContactKeywordQuery = LibraryFunctions.getContactKeywordFormattedQueryString(contactSearchText);
				formattedContactKeywordQuery = formattedContactKeywordQuery.replaceFirst("c.id", "id");
				sqlFinal += formattedContactKeywordQuery;

				log("formattedKeywordQuery: " + formattedKeywordQuery);
				// log.info("formattedContactKeywordQuery"+formattedContactKeywordQuery);
			}
			// end of keywords search if searchText is present.

			// REFINED KEYWORD-PROJECTS
			String formattedPKeywordRefineQuery = null;
			if (refinePKeywordText != null && !refinePKeywordText.equals("")) {
				formattedPKeywordRefineQuery = LibraryFunctions.getKeywordFormattedQueryString(refinePKeywordText);
				formattedPKeywordRefineQuery = formattedPKeywordRefineQuery.replaceFirst("c.id", "id");
				sqlFinal += formattedPKeywordRefineQuery;
			}

			// END OF REFINED KEYWORDS-PROJECTS

			// REFINED KEYWORD-CONTACTS
			String formattedCKeywordRefineQuery = null;
			if (refineCKeywordText != null && !refineCKeywordText.equals("")) {
				formattedCKeywordRefineQuery = LibraryFunctions.getKeywordFormattedQueryString(refineCKeywordText);
				formattedPKeywordRefineQuery = formattedPKeywordRefineQuery.replaceFirst("c.id", "id");
				sqlFinal += formattedCKeywordRefineQuery;
			}

			/*
			 * sql += " order by c." + sortType + " " + sortOrder + " , c.entry_date desc,c.id";
			 */
			sqlFinal += " order by " + sortType + " " + sortOrder + " , entry_date desc";

			log("QUERY: " + sqlFinal);

			stmt = con.createStatement();
			rs = stmt.executeQuery(sqlFinal);

			while (rs.next()) {
				contentIdList.add(String.valueOf((rs.getInt("id"))));

			} // while.
			log("contentIdList.size: " + contentIdList.size());
			if (stmt != null)
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
			log("SQLException : " + se);
			se.printStackTrace();
		} catch (Exception e) {
			log("Exception : " + e);
			e.printStackTrace();
		} finally {
			try {

				if (rsCounty != null)
					rsCounty.close();
				if (stmtCounty != null)
					stmtCounty.close();

				if (rs != null) {
					rs.close();
				}

			} catch (SQLException se) {
				log.error("!exception3.2!SQL error in getSavedSearchContentIds " + se);

			} finally {
				try {
					if (stmt != null) {
						stmt.close();
					}
				} catch (SQLException se) {
					log.error("!exception3.3!SQL error in getSavedSearchContentIds " + se);

				}
			}
		}
		if (contentIdList.size() > 0) {
			return contentIdList.subList(startIndex, endIndex);
		} else {
			return null;
		}

	} //

	public String getSavedSearchInfoPL(int sessionId, String securityKey, String loginId, String searchName, String runHotlist, int pageNumber,
			int recordsPerPage, String sortOrder, String sortType, String searchText, String contactSearchText, String refinePKeywordText,
			String refineCKeywordText, Connection con) throws IOException {

		String stateIds = null;
		String editionDays = null;
		String sectionIdList = null;
		int newUpdatedFlag = 0;
		String subSectionList = null;
		String constructionTypes = null;
		String divisionIdList = null;
		String projectKeywords = null;
		String contactKeywords = null;
		String planningStages = null;
		String conMethod = null;
		String industry = null;
		String subIndustry = null;
		String estiAmountHigh = null;
		String estiAmountLow = null;
		String unit = null;
		String unitMin = null;
		String unitMax = null;
		String storiesMin = null;
		String storiesMax = null;
		String bidDateFrom = null;
		String bidDateTo = null;
		int showAll = 0;
		String displayMode = null;
		// String runHotlist = null;
		String jobType = null;
		String countyIdList = null;

		ResultSet rs = null;
		CallableStatement cstmt = null;

		String searchField = null;
		String searchValue = null;
		String contentIds = null;

		String todayRunDate = "N";

		boolean lastRunDateUpdated = false;

		String currentPage = null;
		currentPage = "Run Saved Search";
		if (runHotlist != null && runHotlist.equals("Y")) {

			currentPage = "Run New and Updated";
			todayRunDate = getLastRunDate(loginId, searchName);
			log("todayRunDate: " + todayRunDate);
			// Update last run date:

			if (compareDates(LibraryFunctions.getDateFromDBDate(todayRunDate), (LibraryFunctions.getTodayDate())) != 0) {
				lastRunDateUpdated = updateLastRunDate(loginId, searchName);

			}

			log("last run date for saved search " + searchName + "lastRunDateUpdated" + lastRunDateUpdated);

		} else {
			todayRunDate = "N";

		}

		try {

			// Start Web Usage Feed Script
			WebUsageUtil webUsage = null;
			webUsage = new WebUsageUtil();
			webUsage.webUsageFeed(loginId, currentPage, null, "");
			// End Web Usage Feed Script

			cstmt = con.prepareCall("{call SSP_GETSAVEDSEARCHBYNAME(?,?)}");

			cstmt.setString(1, loginId);

			cstmt.setString(2, searchName);

			rs = cstmt.executeQuery();

			while (rs.next()) {

				searchField = rs.getString("search_field");
				searchValue = rs.getString("search_value");
				if (searchField.equalsIgnoreCase("section")) {
					sectionIdList = searchValue;
				}
				if (searchField.equalsIgnoreCase("subsection")) {
					subSectionList = searchValue;

				}
				if (searchField.equalsIgnoreCase("state_name")) {
					stateIds = searchValue;
					// log.info(stateIds + "::23423423::::::::::");
				}

				if (searchField.equalsIgnoreCase("county")) {
					countyIdList = searchValue;
				}

				if (searchField.equalsIgnoreCase("editiondays")) {
					editionDays = searchValue;
				}
				if (searchField.equalsIgnoreCase("newjobs")) {
					newUpdatedFlag = Integer.parseInt(searchValue);
				}
				if (searchField.equalsIgnoreCase("bidtype")) {
					jobType = searchValue;
				}
				if (searchField.equalsIgnoreCase("division")) {
					divisionIdList = searchValue;
				}
				if (searchField.equalsIgnoreCase("biddatefrom")) {
					bidDateFrom = searchValue;
				}

				if (searchField.equalsIgnoreCase("biddateto")) {
					bidDateTo = searchValue;
				}
				if (searchField.equalsIgnoreCase("estilowercost")) {
					estiAmountLow = searchValue;
				}

				if (searchField.equalsIgnoreCase("estihighercost")) {
					estiAmountHigh = searchValue;
				}
				if (searchField.equalsIgnoreCase("industry")) {
					industry = searchValue;
				}
				if (searchField.equalsIgnoreCase("sub_industry")) {
					subIndustry = searchValue;
				}

				if (searchField.equalsIgnoreCase("constType")) {
					constructionTypes = searchValue;
				}
				if (searchField.equalsIgnoreCase("sqrft_unit")) {
					unit = searchValue;
				}

				if (searchField.equalsIgnoreCase("sqrft_from")) {
					unitMin = searchValue;
				}
				if (searchField.equalsIgnoreCase("sqrft_to")) {
					unitMax = searchValue;
				}
				if (searchField.equalsIgnoreCase("stories_from")) {
					storiesMin = searchValue;
				}
				if (searchField.equalsIgnoreCase("stories_from")) {
					storiesMin = searchValue;
				}
				if (searchField.equalsIgnoreCase("stories_to")) {
					storiesMax = searchValue;
				}
				if (searchField.equalsIgnoreCase("planningStages")) {
					planningStages = searchValue;
				}
				if (searchField.equalsIgnoreCase("con_method")) {
					conMethod = searchValue;
				}
				if (searchField.equalsIgnoreCase("d_mode")) {
					displayMode = searchValue;
				}
				if (searchField.equalsIgnoreCase("keyword")) {
					projectKeywords = searchValue;

				}
				if (searchField.equalsIgnoreCase("ckeyword")) {
					contactKeywords = searchValue;
				}
				if (searchField.equalsIgnoreCase("allRecords")) {
					if (searchValue != null && !searchValue.trim().equals(""))
						showAll = 1;
				}

			} // while.

			// log.info(conMethod);
			// String hotlist = null;
			// String securityKey = "A19C998B-D583-4CBB-A901-99442939971F";
			// int sessionId = 127;
			// stateIds = "190,215,33,177,189";
			// sectionIdList = "";
			// industry = "";
			// subIndustry = "";
			/*
			 * HotlistManager hm = new HotlistManager();
			 * 
			 * String entryDate = null; String strNewUpdatedFlag = null;
			 * 
			 * String currentDate = ValidateDate.getDateLikeDBDate(ValidateDate.getTodayDateMMDDYY()); log("WEB_DB_URL:"+CDCConfig.WEB_DB_URL);
			 * 
			 * 
			 * SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); Calendar c = Calendar.getInstance(); c.setTime(sdf.parse(currentDate));
			 * c.add(Calendar.DATE, Integer.parseInt("-"+editionDays)); // number of days to add currentDate = sdf.format(c.getTime()); // dt is now the new
			 * date
			 * 
			 * strNewUpdatedFlag = String.valueOf(newUpdatedFlag);
			 * 
			 * log("currentDate: "+currentDate); log("editiondays: "+Integer.parseInt("-"+editionDays)); todayRunDate = currentDate; if (todayRunDate != null &&
			 * !todayRunDate.equals("N")) entryDate = todayRunDate;
			 * 
			 * String query = hm.buildQuery(loginId, entryDate, stateIds, countyIdList, strNewUpdatedFlag , sectionIdList, subSectionList, constructionTypes,
			 * jobType , divisionIdList, projectKeywords, contactKeywords, planningStages , conMethod, industry, subIndustry, estiAmountLow , estiAmountHigh,
			 * unit, unitMin, unitMax , storiesMin, storiesMax, bidDateFrom, bidDateTo , null); log("Query New: "+query);
			 */

			contentIds =
					LibraryFunctions.ListToString(getSavedSearchContentIdsPL(sessionId, securityKey, stateIds, editionDays, sectionIdList, newUpdatedFlag,
							subSectionList, constructionTypes, divisionIdList, projectKeywords, contactKeywords, planningStages, conMethod, industry,
							subIndustry, estiAmountHigh, estiAmountLow, unit, unitMin, unitMax, storiesMin, storiesMax, bidDateFrom, bidDateTo, showAll,
							displayMode, todayRunDate, jobType, countyIdList, pageNumber, recordsPerPage, sortOrder, sortType, searchText, contactSearchText,
							refinePKeywordText, refineCKeywordText, con));

			cstmt.close();
		} catch (SQLException se) {
			log("SQLException : " + se);
			se.printStackTrace();
		} catch (Exception e) {
			log("Exception : " + e);
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}

			} catch (SQLException se) {
				log.error("!exception3.2!SQL error in getSavedSearchInfo " + se);

			}

			finally {
				try {
					if (cstmt != null) {
						cstmt.close();
					}
				} catch (SQLException se) {
					log.error("!exception3.3!SQL error in getSavedSearchInfo " + se);

				}

			}
		}

		return contentIds;
	} // getSavedSearchInfo

	public String getContentDetailsPL(int sessionId, String securityKey, String searchName, String runHotlist, int pageNumber, int recordsPerPage,
			String sortOrder, String sortType, String searchText, String contactSearchText, String refinePKeywordText, String refineCKeywordText)
			throws IOException {

		Map<String, Object> map = null;
		Gson gson = null;
		LeadManagerSessionData lmData = null;

		String contentIds = null;

		Connection con = null;

		try {
			// PropertyConfigurator.configure("ws_log4j.properties");
			// log2=Category.getInstance(SearchResults.class.getName());
			// log2.info("Test......");

			gson = new Gson();
			map = new HashMap<String, Object>();

			boolean validSessionId = false;

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);
				log("loginID: " + lmData.getLogin());

				contentIds =
						getSavedSearchInfoPL(sessionId, securityKey, lmData.getLogin(), searchName, runHotlist, pageNumber, recordsPerPage, sortOrder,
								sortType, searchText, contactSearchText, refinePKeywordText, refineCKeywordText, con);

				int totRecords = size;
				// int totRecords = LibraryFunctions.getListFromString(contentIds).size();

				log("ContentIds: " + contentIds);
				log("iTotalRecords: " + totRecords);

				map.put("sEcho", "1");
				map.put("iTotalRecords", String.valueOf(totRecords));
				map.put("iTotalDisplayRecords", String.valueOf(totRecords));

				map.put("aaData", LoginUtil.getBriefProjectDetailsPL(contentIds, sessionId, sortOrder, sortType, con));

				map.put("aoColumns",
						"ID,CDC ID,Job Type,Bid Date,Title,estimated_amount_lower,Sub Section,county,state,Plan Availability Status,PT STATUS,New Job Status,City");

				size = 0;

			} else {
				log("Invalid Session :sessionId: " + sessionId);
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

		} catch (Exception ex) {
			// log("Exception getContentDetailsPL() "+ex);
			log.error("Exception in getContentDetails() for Advanced search " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);

	}

	public String getSavedSearchContentIds(int sessionId, String securityKey, String stateIds, String editionDays, String sectionIdList, int newUpdatedFlag,
			String subSectionList, String constructionTypes, String divisionIdList, String projectKeywords, String contactKeywords, String planningStages,
			String conMethod, String industry, String subIndustry, String estiAmountHigh, String estiAmountLow, String unit, String unitMin, String unitMax,
			String storiesMin, String storiesMax, String bidDateFrom, String bidDateTo, int showAll, String displayMode, String todayRunDate, String jobType,
			Connection con) {

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

		stateIds = LoginUtil.getSavedSearchStateIds(3, stateIds, con);
		industry = LoginUtil.getSavedSearchIndustryNames(1, industry, con);
		subIndustry = LoginUtil.getSavedSearchSubIndustryNames(2, subIndustry, con);
		sectionIdList = LoginUtil.getSavedSearchSectionIds(4, sectionIdList, con);

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
						"select distinct top 200 c.id,c.cdc_id,c.publication_id,c.section_id,c.sub_section,"
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
			// CONTENT_INDUSTRY----- Need to pass the industry ids from SS table and return indus name
			if ((nationalChainUser != null && nationalChainUser.equals("Y") == true) || (industry != null && industry.equals("") != true)) {

				sql += ",content_industry ci";

			}

			/* ENTRY DATE AND ACTIVATE CHECK */
			sql += " where c.activate =1 and c.state_id=s.state_id and c.id=ps.content_id  and c.county_id=gc.county_id ";
			// NEW/UPDATED FLAG
			if (newUpdatedFlag == 1) {
				sql += "AND c.new_updated = 'N'";

			}
			// IF RUN HOTLIST BUTTON IS CLICKED
			if (todayRunDate != null && !todayRunDate.equals("N")) {

				sql += "AND entry_date >= '" + todayRunDate + "'";

			}

			/* FOR EDITION DAYS -Need to check for newupdated condition */

			if (todayRunDate.equals("N") && editionDays != null) {

				sql += "AND entry_date >= dateadd(day,-" + editionDays + ",getdate())";

			}
			/* NATIONAL CHAIN FLAG CHECK */
			if (nationalChainUser != null && nationalChainUser.equals("Y") == true) {
				sql += "and c.id=cdt.content_id and cdt.national_chain='Y'";
			}

			/* stateIds */

			if (stateIds != null && !stateIds.equals("")) {

				sql += "AND s.state_id in (" + stateIds + ")";

			}

			/* FOR FILTER BASED ON COUNTIES SUBSCRIBED BY USER */

			if (userCountyIds != null) {

				sql += "AND c.county_id in (" + userCountyIds + ")";

			}

			/* NATIONAL CHAIN FLAG CHECK */
			if (nationalChainUser != null && nationalChainUser.equals("Y") == true) {
				sql += "and c.id=cdt.content_id and cdt.national_chain='Y'";
			}

			/* Section-- NEED TO GET THE SECTION IDS */

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

						sql += " and( c.job_type like ('%" + bidType[i] + "%')";

					} else {

						sql += " or c.job_type like ('%" + bidType[i] + "%')";

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

			if (estiAmountHigh != null && estiAmountLow != null && estiAmountHigh.equals("") == false && estiAmountLow.equals("") == false

			) {

				sql +=
						" and (c.estimated_amount_lower <=" + estiAmountHigh + " and c.estimated_amount_lower >=" + estiAmountLow
								+ ") and c.estimated_amount_upper <=" + estiAmountHigh + "";

			} else if (estiAmountLow != null && estiAmountLow.equals("") == false) {

				sql += "and c.estimated_amount_lower>=" + estiAmountLow + "";

			} else if (estiAmountHigh != null && estiAmountHigh.equals("") == false) {

				sql += "and c.estimated_amount_upper <=" + estiAmountHigh + " and c.estimated_amount_lower<=" + estiAmountHigh + "";

			}

			sql += " order by c.bid_date asc , c.entry_date desc";

			// log.info("I  M IN SAVED SEARCH RESULTS"+sql);
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
				log.error("!exception3.2!SQL error in getSavedSearchContentIds " + se);

			} finally {
				try {
					if (stmt != null) {
						stmt.close();
					}
				} catch (SQLException se) {
					log.error("!exception3.3!SQL error in getSavedSearchContentIds " + se);

				}
			}
		}
		return contentIdList;
	} //

	public String getSavedSearchInfo(int sessionId, String securityKey, String loginId, String searchName, String runHotlist) {

		Connection con = null;

		String stateIds = null;
		String editionDays = null;
		String sectionIdList = null;
		int newUpdatedFlag = 0;
		String subSectionList = null;
		String constructionTypes = null;
		String divisionIdList = null;
		String projectKeywords = null;
		String contactKeywords = null;
		String planningStages = null;
		String conMethod = null;
		String industry = null;
		String subIndustry = null;
		String estiAmountHigh = null;
		String estiAmountLow = null;
		String unit = null;
		String unitMin = null;
		String unitMax = null;
		String storiesMin = null;
		String storiesMax = null;
		String bidDateFrom = null;
		String bidDateTo = null;
		int showAll = 0;
		String displayMode = null;

		String jobType = null;
		// String countyIdList = null;

		ResultSet rs = null;
		CallableStatement cstmt = null;

		String searchField = null;
		String searchValue = null;
		String contentIds = null;
		String todayRunDate = null;

		if (runHotlist != null && runHotlist.equals("Y")) {

			todayRunDate = getLastRunDate(loginId, searchName);
		} else {
			todayRunDate = "N";

		}

		try {
			if (con == null) {
				con = JDBCUtil.getDBConnectionFromDatabase();
			}
			cstmt = con.prepareCall("{call SSP_GETSAVEDSEARCHBYNAME(?,?)}");

			cstmt.setString(1, loginId);

			cstmt.setString(2, searchName);

			rs = cstmt.executeQuery();

			while (rs.next()) {
				searchField = rs.getString("search_field");
				searchValue = rs.getString("search_value");
				if (searchField.equals("section")) {
					sectionIdList = searchValue;
				}
				if (searchField.equals("sub_section")) {
					subSectionList = searchValue;
				}
				if (searchField.equals("state_name")) {
					stateIds = searchValue;
				}

				/*
				 * if (searchField.equals("county")) { countyIdList = searchValue; }
				 */

				if (searchField.equals("editiondays")) {
					editionDays = searchValue;
				}
				if (searchField.equals("newjobs")) {
					newUpdatedFlag = Integer.parseInt(searchValue);
				}
				if (searchField.equals("bidtype")) {
					jobType = searchValue;
				}
				if (searchField.equals("division")) {
					divisionIdList = searchValue;
				}
				if (searchField.equals("biddatefrom")) {
					bidDateFrom = searchValue;
				}

				if (searchField.equals("biddateto")) {
					bidDateTo = searchValue;
				}
				if (searchField.equals("estilowercost")) {
					estiAmountLow = searchValue;
				}

				if (searchField.equals("estihighercost")) {
					estiAmountHigh = searchValue;
				}
				if (searchField.equals("industry")) {
					industry = searchValue;
				}
				if (searchField.equals("sub_industry")) {
					subIndustry = searchValue;
				}

				if (searchField.equals("constType")) {
					constructionTypes = searchValue;
				}
				if (searchField.equals("sqrft_unit")) {
					unit = searchValue;
				}

				if (searchField.equals("sqrft_from")) {
					unitMin = searchValue;
				}
				if (searchField.equals("sqrft_to")) {
					unitMax = searchValue;
				}
				if (searchField.equals("stories_from")) {
					storiesMin = searchValue;
				}
				if (searchField.equals("stories_from")) {
					storiesMin = searchValue;
				}
				if (searchField.equals("stories_to")) {
					storiesMax = searchValue;
				}
				if (searchField.equals("planningStages")) {
					planningStages = searchValue;
				}
				if (searchField.equals("con_method")) {
					conMethod = searchValue;
				}
				if (searchField.equals("d_mode")) {
					displayMode = searchValue;
				}

			} // while.

			// log.info(conMethod);
			// String hotlist = null;
			// String securityKey = "A19C998B-D583-4CBB-A901-99442939971F";
			// int sessionId = 127;
			// stateIds = "190,215,33,177,189";
			// sectionIdList = "";
			// industry = "";
			// subIndustry = "";

			contentIds =
					getSavedSearchContentIds(sessionId, securityKey, stateIds, editionDays, sectionIdList, newUpdatedFlag, subSectionList, constructionTypes,
							divisionIdList, projectKeywords, contactKeywords, planningStages, conMethod, industry, subIndustry, estiAmountHigh, estiAmountLow,
							unit, unitMin, unitMax, storiesMin, storiesMax, bidDateFrom, bidDateTo, showAll, displayMode, todayRunDate, jobType, con);

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
				if (con != null) {
					con.close();
				}

			} catch (SQLException se) {
				log.error("!exception3.2!SQL error in getSavedSearchInfo " + se);

			}

			finally {
				try {
					if (cstmt != null) {
						cstmt.close();
					}
				} catch (SQLException se) {
					log.error("!exception3.3!SQL error in getSavedSearchInfo " + se);

				}

			}
		}

		return contentIds;
	} // getSavedSearchInfo

	/**
	 * 
	 * @param sessionId
	 * @param securityKey
	 * @param searchName
	 * @param runHotlist
	 * @return
	 */
	@RequestMapping(value = "/savedSearchResults")
	public String getContentDetails(@RequestParam("sessionId") int sessionId, @RequestParam("securityKey") String securityKey,
			@RequestParam("searchName") String searchName, @RequestParam("runHotlist") String runHotlist) {

		Map<String, Object> map = null;
		Gson gson = null;
		LeadManagerSessionData lmData = null;

		String contentIds = null;

		Connection con = null;

		try {
			gson = new Gson();
			map = new HashMap<String, Object>();

			boolean validSessionId = false;

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			// log.info(validSessionId);
			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);
				contentIds = getSavedSearchInfo(sessionId, securityKey, lmData.getLogin(), searchName, runHotlist);

				map.put("aaData", LoginUtil.getBriefProjectDetails(contentIds, sessionId, con));

				map.put("aoColumns", "ID,CDC ID,Job Type,Bid Date,Title,estimated_amount_lower,Sub Section,county,state,Plan Availability Status");

			} else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

		} catch (Exception ex) {
			log.error("Exception in getContentDetails() for Advanced search " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			JDBCUtil.releaseDBConnection(con);
		}

		return gson.toJson(map);

	}

	// Update last/today run date for emailhotlist table.
	public boolean updateLastRunDate(String loginId, String ssName) {

		Connection con = null;

		ResultSet rs = null;
		CallableStatement cstmt = null;
		boolean updated = false;

		try {
			if (con == null) {
				con = JDBCUtil.getDBConnectionFromDatabase();
				cstmt = con.prepareCall("{call ssp_pl_update_run_date(?,?)}");
				cstmt.setString(1, loginId);
				cstmt.setString(2, ssName);

				cstmt.executeUpdate();
				updated = true;

			}
		} catch (SQLException sqlEx) {
			sqlEx.printStackTrace();
			updated = false;
		} catch (Exception ex) {
			ex.printStackTrace();
			updated = false;
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (con != null) {
					con.close();
				}

			} catch (SQLException se) {
				log.error("!exception3.2!SQL error in updateLastRunDate " + se);

			} finally {
				try {
					if (cstmt != null) {
						cstmt.close();
					}
				} catch (SQLException se) {
					log.error("!exception3.3!SQL error in updateLastRunDate " + se);

				}
			}
		}
		return updated;

	}

	// updateLastRunDate

	// Get content id based on login id for exclude project - new feature added by Johnson on Sept13th 2013
	public String getExcludeProjectIds(String loginId, Connection con) {

		CallableStatement cstmt = null;
		ResultSet rs = null;

		String idList = null;
		try {

			cstmt = con.prepareCall("{call EXCLUSION_LIST(?)}");

			// loginId
			cstmt.setString(1, loginId);

			rs = cstmt.executeQuery();
			while (rs.next()) {
				if (idList == null) {
					idList = rs.getString("id");
				} else {
					idList = idList + "," + rs.getString("id");
				}

			}

		} catch (Exception e) {

			log.error("ERROR IN getExcludeProjectIds " + e.toString());
			e.printStackTrace();

		}

		finally {

			try {
				if (rs != null)
					rs.close();
				if (cstmt != null) {
					cstmt.close();
				}

			} catch (SQLException sqle) {
				log.error("SQLERROR IN getExcludeProjectIds " + sqle.toString());
				sqle.printStackTrace();

			}

		}
		return idList;

	} // End of getExcludeProjectIds()

	/**
	 * Init log writer Creates log file in wslogs folder
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
	 * write log info to the created log file in wslogs folder
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
