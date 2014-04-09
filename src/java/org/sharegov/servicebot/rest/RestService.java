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

import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import mjson.Json;

/**
 * <p>
 * Base class for REST services.
 * 
 * Merge eventually with base class from CiRM when common classes are 
 * moved away from both projects. 
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class RestService
{
	@Context Request request;
	@QueryParam("callback") String callback;
	
	protected Json ok()
	{
		return Json.object("ok", true);
	}

	protected Json ko()
	{
		return Json.object("ok", false);
	}

	protected Json ko(String error)
	{
		return Json.object("ok", false).set("error", error);
	}
	
	protected Json ko(Throwable t)
	{
		return Json.object("ok", false).set("error", t.toString());
	}
	
	protected Response ok(Json json)
	{
		return ok(json.toString());
	}
	
	/**
	 * <p>
	 * Wrap JSON response in a callback function if 'callback' query parameter
	 * is present in the request. Called by sub-classes whenever JSONP needs
	 * to be supported.
	 * </p>
	 * 
	 * @param json
	 * @return
	 */
	protected Response ok(String json)
	{
		String contentType = "application/json";
		if (callback != null && callback.length() > 0)
		{
			json = callback + "(" + json + ");";
			contentType = "application/javascript";
		}
		return Response.ok().header("Content-Type", contentType).entity(json).build();
	}
}
