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

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.hypergraphdb.HGHandle;
import org.sharegov.servicebot.BotApp;
import org.sharegov.servicebot.Scenario;
import org.sharegov.servicebot.UPattern;
import org.sharegov.servicebot.train.BotCoach;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Resource that exposes CRUD interface for scenarios.
 * 
 * @author  Alfonso Boza    <ABOZA@miamidade.gov>
 * @version 1.0
 */
public class ChatScenarioResource
{
    // TODO: Remove Gson dependency.
    private static Gson gson;
    
    // Chat bot coach.
    final private BotCoach coach;
    // Map of scenarios, indexed by handle's UUID.
    final private Map<String, Scenario> scenarios;
    
    /**
     * Initializes JSON parser.
     */
    static
    {
        // Serializes a date into a timestamp.
        JsonSerializer<Date> serializer = new JsonSerializer<Date>()
        {
            public JsonElement serialize(Date source, Type typeOfDate, JsonSerializationContext context)
            {
                return source == null ? null : new JsonPrimitive(source.getTime());
            }
        };
        // Deserializes a timestamp into a date.
        JsonDeserializer<Date> deserializer = new JsonDeserializer<Date>()
        {
            public Date deserialize(JsonElement json, Type typeOfDate, JsonDeserializationContext context)
                    throws JsonParseException
            {
                return json == null ? null : new Date(json.getAsLong());
            }
        };
        // Create JSON parser.
        gson = new GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT)
            .registerTypeAdapter(Date.class, serializer)
            .registerTypeAdapter(Date.class, deserializer)
            .create();
    }
    
    /**
     * Initializes by indexing every scenario in bot coach.
     * 
     * @param   botCoach    Global instance of bot coach.
     */
    public ChatScenarioResource(BotCoach botCoach)
    {
        // Store bot coach.
        this.coach = botCoach;
        // Index every scenario.
        this.scenarios = new HashMap<String, Scenario>();
        for (Scenario scenario : coach.getAllScenarios())
        {
            this.scenarios.put(scenario.getAtomHandle().toString(), scenario);
        }
    }
    
    /**
     * Creates a scenario, returning the URI for the new resource.
     * 
     * @param   uriInfo URI information context.
     * @param   json    Request body in JSON sent by client.
     * @return          Absolute URI for newly created resource.
     * @throws  URISyntaxException
     */
    @POST
    @Consumes("application/json")
    public Response createScenario(@Context UriInfo uriInfo, String json)
    {
        Scenario sentScenario = ChatScenarioResource.gson.fromJson(json, Scenario.class);
        Scenario newScenario = coach.createScenario(sentScenario.getTitle(),
                                                    sentScenario.getDescription(),
                                                    sentScenario.getCreatedBy());
        String uuid = newScenario.getAtomHandle().toString();
        scenarios.put(uuid, newScenario);
        URI uri = uriInfo.getBaseUriBuilder().path(uuid).build();
        return Response.created(uri).build();
    }
    
    /**
     * Deletes an existing scenario with specified ID.
     * 
     * @param   uuid    Integer representing scenario.
     * @return          A response without content if successful.
     */
    @DELETE
    @Path("{id}")
    public Response deleteScenario(@PathParam("id") String uuid)
    {
        if (scenarios.containsKey(uuid))
        {
            coach.deleteScenario(scenarios.get(uuid).getAtomHandle());
            scenarios.remove(uuid);
            return Response.noContent().build();
        }
        else
        {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
    
    /**
     * Lists every scenario in bot coach.
     * 
     * @return  A map of every scenario, indexed by the handle's UUID.
     */
    @GET
    @Path("list")
    @Produces("application/json")
    public String listScenarios()
    {
        return gson.toJson(scenarios);
    }
    
    /**
     * Gets the scenario with specified ID.
     * 
     * @param   uuid    Integer representing scenario.
     * @return          A map of scenario's triggers, indexed by handle's UUID.
     */
    @GET
    @Path("{id}")
    @Produces("application/json")
    public Response retrieveScenario(@PathParam("id") String uuid)
    {
        if (scenarios.containsKey(uuid))
        {
            Map<String, String> triggers = new HashMap<String, String>();
            List<UPattern> patterns = this.coach.getTriggers(scenarios.get(uuid).getAtomHandle());
            for (UPattern pattern : patterns)
            {
                // TODO: Why always get the first one?
                triggers.put(pattern.getAtomHandle().toString(), coach.getPatternUtterances(pattern.getAtomHandle()).get(0).getText());
            }
            return Response.ok(gson.toJson(triggers)).build();
        }
        else
        {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
    
    /**
     * Saves an existing scenario with specified ID.
     * 
     * @param   uuid    Integer representing scenario.
     * @param   json    Request body in URL sent by client.
     * @return          Response indicating success.
     */
    @PUT
    @Path("{id}")
    @Consumes("application/json")
    public Response updateScenario(@PathParam("id") String uuid, String json)
    {
        if (scenarios.containsKey(uuid))
        {
            Scenario sentScenario = gson.fromJson(json, Scenario.class);
            HGHandle handle = coach.getHandleFromUUID(uuid);
            coach.saveScenario(handle,
                               sentScenario.getTitle(),
                               sentScenario.getDescription(),
                               sentScenario.getLastModifiedBy());
            Scenario updatedScenario = BotApp.get().getGraph().get(handle);
            scenarios.put(uuid, updatedScenario);
            return Response.ok().build();
        }
        else
        {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
