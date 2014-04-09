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
package org.sharegov.servicebot.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.engine.header.Header;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.util.Series;
import org.sharegov.servicebot.user.UserManager;

import com.google.gson.Gson;

/**
 * Service for user registration, login, and saving.
 * 
 * @author Julian Bonilla <julianb@miamidade.gov>
 */
@Path("user")
public class UserService {
	
	/**
	 * User registration.
	 * 
	 */
	@GET
	@Path("register")
	public String register() {
		return "success";
	}

	/**
	 * User login.
	 * 
	 */
	@POST
	@Path("login")
	@Produces("application/json")
	public String login(@Context HttpHeaders headers, Representation entity) {
		String auth = "false";
		
		Form form = new Form(entity);
		String username = form.getFirstValue("username");
		String password = form.getFirstValue("password");
		
		URI loginURI = this.getLoginURI(username, password);
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(loginURI);
		HttpResponse response;
		
		try {
			response = httpclient.execute(httpget);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_OK) {
				auth = "true";
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (httpget != null) {
				httpget.releaseConnection();
			}
		}
		
		Map<String, String> result = new HashMap<String, String>();
        result.put("username", username);
        result.put("auth", auth);
        
        // Set allow-origin header so AJAX response can be handled by non-secure pages.
        Response resp = Response.getCurrent();
        @SuppressWarnings("unchecked")
		Series<Header> responseHeaders = (Series<Header>) resp.getAttributes().get("org.restlet.http.headers");
        if (responseHeaders == null) {
            responseHeaders = new Series<Header>(Header.class);
            resp.getAttributes().put("org.restlet.http.headers", responseHeaders);
        }
        responseHeaders.add(new Header("Access-Control-Allow-Origin", "*"));
        
    	return new Gson().toJson(result);
	}
	
	/**
	 * Save a user's checklist.
	 * 
	 */
	@POST
	@Path("save")
	public Representation save(Representation entity) {
		Form form = new Form(entity);
		String username = form.getFirstValue("username");
		String business = form.getFirstValue("business");
		String checklist = form.getFirstValue("checklist");
		
		UserManager manager = new UserManager();
		manager.saveChecklist(username, business, checklist);
		
		return new StringRepresentation("Your checklist has been saved.");
	}
	
	/**
	 * Retrieve a user's checklist.
	 * 
	 */
	@GET
	@Path("retrieve/{user}")
	@Produces("application/json")
	public String retrieve(@PathParam("user") String user) {
		if (user == null) {
			System.out.println("Bad Request");
			// Return BAD_REQUEST
			//return Response.status(Status.BAD_REQUEST).build();
		}
		
		UserManager manager = new UserManager();
		List<Map<String, String>> checklists = manager.getChecklist(user);
		return new Gson().toJson(checklists);
	}
	
	/**
	 * Generate miamidade.gov login url.
	 * 
	 * @param username
	 * @param password
	 */
	private URI getLoginURI(String username, String password) {
		StringBuilder builder = new StringBuilder();
		builder.append("http://miamidade.gov/wps/portal/cxml/04_SD9ePMtCP1I800I_KydQvyHFUBADPmuQy");
		builder.append("?");
		builder.append("userid=");
		builder.append(username);
		builder.append("&");
		builder.append("password=");
		builder.append(password);
		
		URI uri = null;
		
		try {
			uri = new URI(builder.toString());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		return uri;
	}
}
