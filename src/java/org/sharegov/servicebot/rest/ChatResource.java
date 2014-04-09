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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import mjson.Json;

import org.sharegov.servicebot.FrontDesk;
import org.sharegov.servicebot.HelpBooth;
import org.sharegov.servicebot.PrologUtils;
import org.sharegov.servicebot.UserContext;
import org.sharegov.servicebot.rest.ChatDialogResource.ContextType;
import org.sharegov.servicebot.train.BotCoach;

import com.google.gson.Gson;

/**
 * Resource that creates servicebot chat rooms and exposes several
 * admistrative resources.
 * 
 * @author  Alfonso Boza    <ABOZA@miamidade.gov>
 * @version 1.0
 */
@Path("chat")
public class ChatResource
{
    // HTTP header for parent bot dialog IDs.
    final private static String HTTP_HEADER_BOT = ("X-Bot-Dialog").toLowerCase();
    // HTTP header for parent user dialog IDs.
    final private static String HTTP_HEADER_USER = ("X-User-Dialog").toLowerCase();
    // HTTP header for scenario IDs.
    final private static String HTTP_HEADER_SCENARIO = ("X-Scenario").toLowerCase();
    
    // Chat bot coach.
    private static BotCoach botCoach;
    // Front desk used to get chat bot agent.
    private static FrontDesk frontDesk;
    
    /**
     * Initializes the chat bot's coach and front desk immediately.
     */
    static
    {
        botCoach = new BotCoach();
    	frontDesk = new FrontDesk();
    }
    
    /**
     * Creates a new chat room for user to interact with the chat bot. 
     * 
     * @return  The JID of the newly-created chat room.
     */
    @GET
    @Path("room")
    @Produces("application/json")
    public String getRoomJID(@QueryParam("context") String context, @QueryParam("frame") String frame)
    {
    	try
    	{
	        Map<String, String> room = new HashMap<String, String>();
	        UserContext uctx = new UserContext();
	        uctx.setFrameContext(context);
	        uctx.setCurrentFrame(frame);        
	        room.put("jid", frontDesk.assignAgent(uctx));
	    	return Json.object("ok", true).with(Json.make(room)).toString();
    	}
    	catch (Throwable t)
    	{
    		return Json.object("ok", false, "error", t.toString()).toString();
    	}
    }
    
    @GET
    @Path("saveTrace/{roomid}/{filename}")
    public String saveTrace(@PathParam("roomid") String roomid, @PathParam("filename") String filename)
    {
    	HelpBooth booth = frontDesk.getBooth(roomid);
    	if (booth == null)
    		return Json.object("ok", false, "error", "Can't find room with id " + roomid).toString();
    	try
    	{
    		booth.getBot().saveTrace(filename);
    		return Json.object("ok", true).toString();
    	}
    	catch (Throwable t)
    	{
    		t.printStackTrace(System.err);
    		return Json.object("ok", false, "error", t.toString()).toString();
    	}    	
    }
    
    @GET
    @Path("predicateValue/{roomid}/{predicate}")
    @Produces("application/json")
    public String getPredicateValue(@PathParam("roomid") String roomid, @PathParam("predicate") String predicate)
    {
    	HelpBooth booth = frontDesk.getBooth(roomid);
    	if (booth == null)
    		return Json.object("ok", false, "error", "Can't find room with id " + roomid).toString();
    	try
    	{
    		return Json.object("ok", true, "value", PrologUtils.toJson(
    				booth.getBot().getBot().queryOne(predicate))).toString();
    	}
    	catch (Throwable t)
    	{
    		t.printStackTrace(System.err);
    		return Json.object("ok", false, "error", t.toString()).toString();
    	}
    }
     
    /**
     * Retrieves resource manager of chat dialogs.
     * 
     * @param   headers                 HTTP headers sent by client.
     * @return                          Chat dialog resource.
     * @throws  NoSuchMethodException
     * @throws  SecurityException
     */
    @Path("question")
    public ChatDialogResource questionResource(@Context HttpHeaders headers) throws NoSuchMethodException,
                                                                                    SecurityException
    {
        ChatDialogResource resource = new ChatDialogResource(botCoach);
        MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
        if (requestHeaders.containsKey(HTTP_HEADER_SCENARIO))
        {
            resource.setContext(ContextType.SCENARIO, requestHeaders.getFirst(HTTP_HEADER_SCENARIO));
        }
        else if (requestHeaders.containsKey(HTTP_HEADER_BOT))
        {
            resource.setContext(ContextType.BOT, requestHeaders.getFirst(HTTP_HEADER_BOT));
        }
        else if (requestHeaders.containsKey(HTTP_HEADER_USER))
        {
            resource.setContext(ContextType.USER, requestHeaders.getFirst(HTTP_HEADER_USER));
        }
        return resource;
    }
    
    /**
     * Retrieves resource to manage chat scenarios.
     * 
     * @return  Chat scenario resource.
     */
    @Path("scenario")
    public ChatScenarioResource scenarioResource()
    {
    	return new ChatScenarioResource(botCoach);
    }
}
