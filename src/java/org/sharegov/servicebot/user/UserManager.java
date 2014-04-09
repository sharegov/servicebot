/*******************************************************************************
 * Copyright 2014 Miami-Dade County
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.sharegov.servicebot.user;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oracle.jdbc.driver.OraclePreparedStatement;


public class UserManager {

	private String driver = "oracle.jdbc.OracleDriver";
	private String url = "jdbc:oracle:thin:@(DESCRIPTION =(ADDRESS = (PROTOCOL = TCP)(HOST = s0142084.miamidade.gov)(PORT = 1521))(ADDRESS = (PROTOCOL = TCP)(HOST = s0142085.miamidade.gov)(PORT = 1521))(LOAD_BALANCE = yes)(CONNECT_DATA =(SERVER = DEDICATED)(SERVICE_NAME = twas2.miamidade.gov)))";
	private String user = "portalcore";
	private String password = "portalcore";
	
	public Connection getConnection() {
		Connection connection = null;
		
		try {
			Class.forName(driver);
			connection = DriverManager.getConnection(url, user, password);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 
		catch (SQLException e) {
			e.printStackTrace();
		}
		
		return connection;
	}
	
	/**
	 * Save a user's check list.
	 * 
	 * @param username
	 * @param business
	 * @param checklist
	 */
	public void saveChecklist(String username, String business, String checklist) {
		
		Connection connection = this.getConnection();
		
		// Save
		try {
			String insert = "INSERT INTO portt_eco_checklist (id, username, checklist, created_date, business) VALUES (PORTSQ_HIBERNATE_SEQUENCE.nextval, ?, ?, ?, ?)";
			
			OraclePreparedStatement pstmt = (OraclePreparedStatement) connection.prepareStatement(insert);
			pstmt.setString(1, username);
			pstmt.setStringForClob(2, checklist);
			long now = Calendar.getInstance().getTimeInMillis();
			pstmt.setDate(3, new Date(now));
			pstmt.setString(4, business);
			
			pstmt.executeUpdate();
			pstmt.close();
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get a users saved check lists.
	 * 
	 * @param username
	 * @return List of saved check lists.
	 */
	public List<Map<String, String>> getChecklist(String username) {
		List<Map<String, String>> checklists = new ArrayList<Map<String, String>>();
		
		Connection connection = this.getConnection();
		
		try {
			String select = "SELECT * FROM portt_eco_checklist WHERE username = ? order by created_date desc";
			
			PreparedStatement pstmt = connection.prepareStatement(select);
			pstmt.setString(1, username);
			ResultSet resultSet = pstmt.executeQuery();
			
			while(resultSet.next()) {
				Map<String, String> checklist = new HashMap<String, String>();
				
				// Check list
				Clob clob = resultSet.getClob("checklist");
				int numChar = (int)clob.length();
				checklist.put("checklist", clob.getSubString(1, numChar));
				
				// Business
				checklist.put("business", resultSet.getString("business"));
				
				// Date
				Date createdDate = resultSet.getDate("created_date");
				String formatedDate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(createdDate);
				checklist.put("date", formatedDate);
				
				checklists.add(checklist);				
			}
			
			pstmt.close();
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return checklists;
	}
	
	public static void main(String[] args) {
		UserManager manager = new UserManager();
		List<Map<String, String>> checklists = manager.getChecklist("julian");
		for(Map<String, String> checklist : checklists) {
			System.out.println(checklist.get("checklist"));
		}
		
		//manager.saveChecklist(null, null);
		//System.out.println(manager.getDatabaseVersion());
	}
	
	private String getDatabaseVersion() {
		Connection connection = this.getConnection();
		String metadata = null;
		
		try {
			metadata = connection.getMetaData().getDatabaseProductVersion();
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return metadata;
	}
}
