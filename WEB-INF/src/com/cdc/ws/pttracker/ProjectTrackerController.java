package com.cdc.ws.pttracker;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import wsdatamodel.LeadManagerSessionData;
import wsutils.JDBCUtil;
import wsutils.LoginUtil;

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
}
