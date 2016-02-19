package com.cdc.ws.pttracker;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import wsexception.LoginException;
import wsdatamodel.LeadManagerSessionData;
import wsutils.JDBCUtil;
import wsutils.LoginUtil;


import wsutils.WebUsageUtil;

import com.google.gson.Gson;


@RestController
@RequestMapping(value="/services")
public class ProjectTrackerController {

	/**
	 * [web service] for Geting the User's PT Folders List 
	 * @param sessionId
	 * @param securityKey
	 * @return
	 */
	@RequestMapping(value="/getPTFoldersList")
	public String wsgetProjectTrackerUserFolders(@RequestParam("sessionId") int sessionId,@RequestParam("securityKey") String securityKey){
		Map<String,Object> map = null; 
		Gson gson = null;
		Connection con = null; 
		LeadManagerSessionData lmData = null;
		try {

			gson = new Gson();
			map = new HashMap<String,Object>();
			map = new HashMap<String, Object>();

			if (con == null)
				con = JDBCUtil.getDBConnectionFromDatabase();

			lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);
			boolean validSessionId = false;
			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			//System.out.println("getProjectTrackerProjectDetails"+"typeId"+typeId+"lmData.getLogin()"+lmData.getLogin()+cdcId);
			if (validSessionId == true) {

				map.put("iTotalRecords", "1");
				map.put("Status", "Success");
				map.put("Message", "");
				// jsonArray.add(jsonObject);
				map.put("Data", getUserFolders(lmData.getLogin(),con)); 
			}
			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

		}
		catch (Exception ex) {
			ex.printStackTrace();
			System.out.println(
					"Exception in projectTrackerModule() for projectTrackerModule " +
							ex.getMessage());
		}
		finally{
			JDBCUtil.releaseDBConnection(con);
		}
		return gson.toJson(map);
	}

	/**
	 * Function Returns the Folder list and it's details from the DataBase
	 * @param loginId
	 * @param con
	 * @return
	 */
	private ArrayList getUserFolders(String loginId, Connection con){


		ResultSet rs = null;
		CallableStatement cstmt = null;
		ArrayList folderDetails=new ArrayList();


		try {

			cstmt = con.prepareCall("{call SSP_PROJECT_TRACKER(?,?)}");

			cstmt.setInt(1, 11);

			//LOGIN ID
			cstmt.setString(2, loginId);

			rs = cstmt.executeQuery();
			Map folderMap=null;
			while (rs.next()) {
				folderMap=new HashMap();
				folderMap.put("ID",String.valueOf(rs.getInt("NODE_ID")));
				folderMap.put("FOLDER_NAME", rs.getString("NODE_VALUE"));
				folderMap.put("PARENT_ID", String.valueOf(rs.getInt("PARENT_ID")));
				folderDetails.add(folderMap);
			}

			cstmt.close();
		}
		catch (SQLException se) {
			System.out.println("SQLException occurred in webservice while getting ProjectTracker folder Details" + se);
		}
		catch (Exception e) {
			System.out.println("Exception occurred in webservice while getting ProjectTracker folder Details" + e);

		}
		finally {
			try {
				if (rs != null) {
					rs.close();
				}


			}
			catch (SQLException se) {
				System.out.println("SQL error in webservice while getting ProjectTracker folder Details" + se);

			}
		}

		return folderDetails;
	}

	/*

    //Typeid=5,loginid,foldername is mandatory
   //PROJECT TRACKER JOB DETAILS
	 */
	@RequestMapping(value="/ProjectTracker")
	public String getProjectTrackerProjectDetails(@RequestParam("sessionId") int sessionId,
			@RequestParam("securityKey") String securityKey,@RequestParam("typeId") int typeId,@RequestParam("folderName") String folderName,
			@RequestParam("newFolderName") String newFolderName,
			@RequestParam("cdcId")String cdcId,@RequestParam("notes") String notes) {
		Map<String,Object> map = null; 
		Gson gson = null;


		LeadManagerSessionData lmData = null;
		Connection con = null;
		try {

			gson = new Gson();
			map = new HashMap<String,Object>();
			boolean validSessionId = false;

			if (con == null)	con = JDBCUtil.getDBConnectionFromDatabase();

			validSessionId = LoginUtil.checkValidSession(sessionId, securityKey, con);

			//System.out.println("getProjectTrackerProjectDetails"+"typeId"+typeId+"lmData.getLogin()"+lmData.getLogin()+cdcId);
			if (validSessionId == true) {

				lmData = LoginUtil.getLeadManagerSessionDetails(sessionId, con);

				String currentPage = "Project Tracker";
				if(typeId==10) currentPage = "Updated Tracked Projects";
				// Start Web Usage Feed Script		
				WebUsageUtil webUsage=null; webUsage = new WebUsageUtil();	  	
				webUsage.webUsageFeed(lmData.getLogin(), currentPage, null, "");    	  	
				// End Web Usage Feed Script

				ArrayList list = projectTrackerProjectDetails(typeId, lmData.getLogin(), folderName,
						newFolderName,
						cdcId, con);
				map.put("Status", "Success");
				if(list!=null)
					map.put("iTotalRecords", String.valueOf(list.size()));
				map.put("aaData",list
						);
				map.put("aoColumns",
						"job_name,notes_id,cdc_id,job_id,short_cdcid,entry_date,content_id,"+
								" bids_details,biddate,loweramount,higheramount,sub_sec,county_multiple,"+
						" county,notes,notes_update_date");


			}
			else {
				map.put("iTotalRecords", "0");
				map.put("Status", "Failure");
				map.put("Message", "Invalid Session or Session Expired");

			}

		}
		catch (Exception ex) {
			ex.printStackTrace();
			System.out.println(
					"Exception in projectTrackerModule() for projectTrackerModule " +
							ex.getMessage());
		}
		finally{
			JDBCUtil.releaseDBConnection(con);
		}
		return gson.toJson(map);

	}

	//PROJECT TRACKER JOB DETAILS
	//Typeid=5,loginid,foldername is mandatory
	public ArrayList projectTrackerProjectDetails(int typeId, String loginId, String folderName,
			String newFolderName,
			String cdcId, Connection con) throws
			LoginException {

		ResultSet rs = null;
		CallableStatement cstmt = null;
		ArrayList jobDetails=new ArrayList();


		try {



			cstmt = con.prepareCall("{call SSP_PROJECT_TRACKER(?,?,?,?,?)}");

			cstmt.setInt(1, typeId);

			//LOGIN ID
			cstmt.setString(2, loginId);

			//FOLDER NAME
			if (folderName != null) {
				cstmt.setString(3, folderName);
			}
			else {
				cstmt.setString(3, null);
			}

			//NEW FOLDER NAME

			if (newFolderName != null) {
				cstmt.setString(4, newFolderName);
			}
			else {
				cstmt.setString(4, null);
			}
			//CDCID
			cstmt.setString(5, cdcId);


			rs = cstmt.executeQuery();
			ArrayList saveJobDetails=null;
			while (rs.next()) {
				saveJobDetails=new ArrayList();
				saveJobDetails.add(rs.getString("job_name"));
				saveJobDetails.add(String.valueOf(rs.getInt("notes_id")));
				saveJobDetails.add(rs.getString("cdc_id"));
				saveJobDetails.add(String.valueOf(rs.getInt("job_id")));
				saveJobDetails.add(rs.getString("short_cdcid"));
				saveJobDetails.add(rs.getString("entry_date"));
				saveJobDetails.add(String.valueOf(rs.getInt("content_id")));
				saveJobDetails.add(rs.getString("bids_details"));
				saveJobDetails.add(rs.getString("biddate"));
				saveJobDetails.add(rs.getString("loweramount"));
				saveJobDetails.add(rs.getString("higheramount"));
				saveJobDetails.add(rs.getString("sub_sec"));
				saveJobDetails.add(rs.getString("county_multiple"));
				saveJobDetails.add(rs.getString("county"));
				saveJobDetails.add(rs.getString("notes"));
				saveJobDetails.add(rs.getString("update_date"));
				jobDetails.add(saveJobDetails);
			}

			cstmt.close();
		}
		catch (SQLException se) {
			se.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();

		}
		finally {
			try {
				if (rs != null) {
					rs.close();
				}


			}
			catch (SQLException se) {
				System.out.println("!exception3.2!SQL error in ProjectTracker Details" + se);

			}
			finally {
				try {
					if (cstmt != null) {
						cstmt.close();
					}
				}
				catch (SQLException se) {
					System.out.println("!exception3.3!SQL error in ProjectTracker Details" +
							se);

				}
			}
		}
		return jobDetails;
	}
	// PROJECT TRACKER JOB DETAILS .
}
