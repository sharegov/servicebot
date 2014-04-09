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

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.app.wordnet.data.SynsetLink;
import org.hypergraphdb.app.wordnet.data.Word;
import org.sharegov.servicebot.BotApp;
import static mjson.Json.*;
import mjson.Json;

@Path("words")
public class LexicalService
{	
	@GET
	@Path("/define/{word}")
	@Produces("application/json")
	public String define(@PathParam("word") String word)
	{
		HyperGraph graph = BotApp.get().getGraph();
		HGHandle h = hg.findOne(graph, hg.eq(new Word(word)));
		Json result;
		if (h == null)
			result = nil();
		else
		{
			result = array();
			List<SynsetLink> senses = hg.getAll(graph, 
					hg.and(hg.typePlus(SynsetLink.class), hg.incident(h)));
			for (SynsetLink synset : senses)
			{
				Json synonyms = array();
				for (HGHandle syn : synset)
					synonyms.add(make(graph.get(syn).toString().replace('_', ' ')));
				result.add(object()
							.set("pos", synset.getClass().getSimpleName().replace("SynsetLink", "").toLowerCase())
							.set("gloss", synset.getGloss())
							.set("synonyms", synonyms));
			}
		}
		return result.toString();
	}
}
