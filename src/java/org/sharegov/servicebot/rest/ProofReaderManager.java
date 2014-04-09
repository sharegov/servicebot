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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.sharegov.servicebot.BotApp;
import static mjson.Json.*;
import mjson.Json;
import org.sharegov.servicebot.proofread.ProofReader;

@Path("proofreaders")
public class ProofReaderManager
{
	private HyperGraph graph = BotApp.get().getGraph();
	
	@GET
	@Path("/list")
	@Produces("application/json")
	public String listReaders()
	{
		List<ProofReader> L = hg.getAll(graph, hg.typePlus(ProofReader.class));
		Collections.sort(L, new Comparator<ProofReader>() {
			public int compare(ProofReader o1, ProofReader o2)
			{
				return o1.getName().compareTo(o2.getName());
			}			
		});
		Json result = array();		
		for (ProofReader pr : L)
			result.add(object().set("handle", graph.getHandle(pr).getPersistent().toString())
									.set("type", pr.getClass().getName())
									.set("name", pr.getName()));
		return result.toString();
	}

	@SuppressWarnings("unchecked")
	@POST
	@Path("/new/{type}/{name}")
	@Produces("application/json")
	public String newProofReader(@PathParam("type") String type, @PathParam("name") String name)
	{
		Json result = null;
		try
		{
			Class<? extends ProofReader> cl = (Class<? extends ProofReader>)Class.forName(type);
			ProofReader pr = cl.newInstance();
			pr.setName(name);
			HGHandle h = graph.add(pr);
			result = object().set("handle", h.getPersistent().toString()).set("type", type);
		}
		catch (ClassNotFoundException ex)
		{
			result = make("type-not-found");
		}
		catch (Exception e)
		{
			result = object().set("error", e.toString());
		}
		return result.toString();		
	}
	
	@PUT
	@Path("")
	@Produces("application/json")
	public String saveProofReader()
	{
		Json result = object();
		return result.toString();
	}
	
	@DELETE
	@Path("/{id}")
	@Produces("application/json")
	public String deleteProofReader(@PathParam("id")String id)
	{
		HGHandle handle = graph.getHandleFactory().makeHandle(id);
		graph.remove(handle);
		return object().set("status", "ok").toString();
	}	
}
