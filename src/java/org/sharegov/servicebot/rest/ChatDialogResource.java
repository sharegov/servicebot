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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGHandleHolder;
import org.sharegov.servicebot.UPattern;
import org.sharegov.servicebot.train.BotCoach;

import com.google.gson.Gson;

/**
 * Resource that exposes a CRUD interface for all dialogs: scenario triggers,
 * and replies from user and/or bot.
 * 
 * @author  Alfonso Boza    <ABOZA@miamidade.gov>
 * @version 1.0
 */
public class ChatDialogResource
{
    // Chat bot coach.
    final private BotCoach coach;
    // Dialog context.
    private HGHandle context;
    // Creator method for dialog.
    private Method creator;
    // Destroyer method for dialog.
    private Method destroyer;
    // Accessor method for dialog.
    private Method getter;
    
    /**
     * Stores global bot coach instance upon initialization.
     * 
     * @param   botCoach    Global bot coach instance.  
     */
    public ChatDialogResource(BotCoach botCoach)
    {
        // Store bot coach.
        this.coach = botCoach;
    }
    
    /**
     * Creates a dialog, returning the URI for the newly-created resource.
     * 
     * TODO: Accept more than text/plain.
     * 
     * @param uriInfo   URI information context.
     * @param pattern   Dialog text.
     * @return          URI of new resource.
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @POST
    @Consumes("text/plain")
    public Response createDialog(@Context UriInfo uriInfo, String pattern) throws IllegalAccessException,
                                                                                  IllegalArgumentException,
                                                                                  InvocationTargetException
    {
        try
        {
            HGHandleHolder dialog = (HGHandleHolder) this.creator.invoke(this.coach, this.context, pattern);
            String uuid = dialog.getAtomHandle().toString();
            URI uri = uriInfo.getBaseUriBuilder().path(uuid).build();
            return Response.created(uri).build();
        }
        catch (NullPointerException e)
        {
            return Response.status(Status.BAD_REQUEST).build();
        }
    }
    
    /**
     * Deletes specified dialog.
     * 
     * @param uuid  UUID of dialog.
     * @return      An HTTP/1.1 204 response if successful.
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @DELETE
    @Path("{uuid}")
    public Response destroyDialog(@PathParam("uuid") String uuid) throws IllegalAccessException,
                                                                         IllegalArgumentException,
                                                                         InvocationTargetException
    {
        try
        {
            this.destroyer.invoke(this.coach, this.context);
            return Response.noContent().build();
        }
        catch (NullPointerException e)
        {
            return Response.status(Status.BAD_REQUEST).build();
        }
    }
    
    /**
     * Gets dialog with specified UUID.
     * 
     * TODO: Removed unchecked warning.
     * 
     * @param uuid  UUID of desired dialog.
     * @return      A JSON object literal whose values are indexed by UUID of subdialogs.
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @GET
    @Path("{uuid}")
    @Produces("application/json")
    @SuppressWarnings("unchecked")
    public Response retrieveDialog(@PathParam("uuid") String uuid) throws IllegalAccessException,
                                                                          IllegalArgumentException,
                                                                          InvocationTargetException
    {
        try
        {
            Object dialogs = this.getter.invoke(this.coach, this.context);
            List<UPattern> responses = new ArrayList<UPattern>();
            if (UPattern.class.isAssignableFrom(dialogs.getClass()))
            {
                // Chat bot response doesn't return a list, so create one.
                responses = new ArrayList<UPattern>();
                responses.add((UPattern) dialogs);
            }
            else
            {
                responses = (List<UPattern>) dialogs;
            }
            Map<String, String> patterns = new HashMap<String, String>();
            for (UPattern pattern : responses)
            {
                patterns.put(pattern.getAtomHandle().toString(),
                             this.coach.getPatternUtterances(pattern.getAtomHandle()).get(0).getText());
            }
            // TODO: Remove Gson dependency.
            return Response.ok(new Gson().toJson(patterns)).build();
        }
        catch (NullPointerException e)
        {
            return Response.status(Status.BAD_REQUEST).build();
        }
    }
    
    /**
     * Sets the context of dialog conversation.
     * 
     * @param type  Context type.
     * @param uuid  UUID of context.
     * @throws NoSuchMethodException
     * @throws SecurityException
     */
    public void setContext(ContextType type, String uuid) throws NoSuchMethodException, SecurityException
    {
        String create, destroy, retrieve;
        switch (type)
        {
            case BOT:
                create = "addReply";
                destroy = "deleteBranch";
                retrieve = "getReplies"; 
                break;
            case SCENARIO:
                create = "addTrigger";
                destroy = "deleteBranch";
                retrieve = "getTriggers";
                break;
            case USER:
                create = "setBeckyResponse";
                destroy = "deleteBeckyResponse";
                retrieve = "getBeckyResponse";
                break;
            default:
                throw new IllegalArgumentException();
        }
        this.context = this.coach.getHandleFromUUID(uuid);
        this.creator = BotCoach.class.getDeclaredMethod(create, HGHandle.class, String.class);
        this.destroyer = BotCoach.class.getDeclaredMethod(destroy, HGHandle.class);
        this.getter = BotCoach.class.getDeclaredMethod(retrieve, HGHandle.class);
    }
    
    /**
     * Enumerates the various context types.
     * 
     * @author  Alfonso Boza    <ABOZA@miamidade.gov>
     */
    public enum ContextType
    {
        BOT,        // Indicates a reply to the servicebot.
        SCENARIO,   // Indicates a trigger to begin scenario.
        USER        // Indicates a reply to a user.
    };
}
