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

import java.util.Arrays;
import java.util.Comparator;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.app.wordnet.data.Word;
import org.sharegov.servicebot.BotApp;
import static mjson.Json.*;
import mjson.Json;
import org.sharegov.servicebot.proofread.RedInk;
import org.sharegov.servicebot.proofread.WordBanProofReader;

import disko.ContentDownloader;
import disko.utils.DiscoProxySettings;

@Path("wordban/{id}")
public class WordBanService
{
	private HyperGraph graph = BotApp.get().getGraph();	
	private @PathParam("id") String handleId;
	
	private WordBanProofReader getReader()
	{
		return graph.get(graph.getHandleFactory().makeHandle(handleId));		
	}	
	
	public WordBanService(@PathParam("id")String handleId)
	{
		this.handleId = handleId;
	}

	@POST
	@Path("/read")
	@Produces("application/json")
	public String readText(@FormParam("text") String text, @FormParam("url") String url)
	{
		if (text == null)
		{
			DiscoProxySettings.proxyHost = "proxy.miamidade.gov";
			DiscoProxySettings.proxyPort = 80;
			DiscoProxySettings.proxyUser = "miamidade/webtemp2";
			DiscoProxySettings.proxyPassword = "password";
			text = ContentDownloader.getInstance().readText(url);
		}
		RedInk [] inkA = getReader().read(text).toArray(new RedInk[0]);
		Arrays.sort(inkA, new Comparator<RedInk>(){
			public int compare(RedInk o1, RedInk o2)
			{
				return o1.getFrom() - o2.getTo();
			}			
		});
		Json A = array();
		for (RedInk ink : inkA)
			A.add(object().set("from", ink.getFrom())
					      .set("to", ink.getTo())
						  .set("text", ink.getText())
				);
		return A.toString();
	}

	@GET
	@Path("/words")
	@Produces("application/json")
	public String getWords()
	{
		String [] A = getReader().getBannedWords().toArray(new String[0]);
		Arrays.sort(A);
		return array(A).toString();
	}
	
	@POST
	@Path("/add/{word}")
	@Produces("application/json")
	public String getAddWord(@PathParam("word") String word, @FormParam("force") boolean force)
	{
		HGHandle wordHandle = hg.findOne(graph, hg.eq(new Word(word)));
		if (wordHandle != null)
		{
			getReader().banWord(wordHandle);
			return object().set("status", "ok").toString();
		}
		else if (force)
		{
			wordHandle = graph.add(new Word(word));
			getReader().banWord(wordHandle);
			return object().set("status", "ok").toString();
		}		
		else
			return object().set("status", "ko")
						   .set("error", "not-found").toString();
	}
	
	@DELETE
	@Path("/remove/{word}")
	@Produces("application/json")
	public String deleteProofReader(@PathParam("word")String word)
	{
		HGHandle wordHandle = hg.findOne(graph, hg.eq(new Word(word)));
		if (wordHandle == null)
			return object().set("status", "ko")
						   .set("error", "not-found").toString();
		getReader().allowWord(wordHandle);
		return object().set("status", "ok").toString();
	}
}
