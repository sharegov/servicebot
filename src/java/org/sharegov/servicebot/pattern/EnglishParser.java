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
package org.sharegov.servicebot.pattern;

import java.util.List;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.app.dataflow.Channel;
import org.hypergraphdb.app.dataflow.DataFlowNetwork;
import org.hypergraphdb.handle.UUIDHandleFactory;
import org.sharegov.servicebot.BotApp;

import relex.entity.EntityMaintainer;
import disko.AnalysisContext;
import disko.HTMLDocument;
import disko.ParagraphAnn;
import disko.SentenceAnn;
import disko.TextDocument;
import disko.data.relex.RelexParse;
import disko.data.relex.SentenceInterpretation;
import disko.flow.analyzers.AccumulatingNode;
import disko.flow.analyzers.DiskoSearchAnalyzer;
import disko.flow.analyzers.EntityAnalyzer;
import disko.flow.analyzers.LinkGrammarProcessor;
import disko.flow.analyzers.ParagraphAnalyzer;
import disko.flow.analyzers.ParseSelectProcessor;
import disko.flow.analyzers.RelexProcessor;
import disko.flow.analyzers.SentenceAnalyzer;
import disko.flow.analyzers.ToRelOccProcessor;

public class EnglishParser
{
	private DataFlowNetwork<AnalysisContext<TextDocument>> network = null;
	private AccumulatingNode<AnalysisContext<TextDocument>, SentenceInterpretation> parseAccumulator;
	private AccumulatingNode<AnalysisContext<TextDocument>, SentenceAnn> sentenceAccumulator;
	private LinkGrammarProcessor linkGrammar = new LinkGrammarProcessor();
	
	private synchronized void ensureNetwork()
	{
		if (network != null)
			return;
		
		network = createNetwork();
	}
	
	public List<SentenceInterpretation> getParses()
	{
		return parseAccumulator.getData();
	}

	public List<SentenceAnn> getSentences()
	{
		return sentenceAccumulator.getData();
	}
	
	public List<SentenceInterpretation> parse(HyperGraph graph, String input)
	{
		ensureNetwork();
		HTMLDocument doc = new HTMLDocument(input);
		AnalysisContext<TextDocument> ctx = new AnalysisContext<TextDocument>(graph, doc);
		try
		{
			parseAccumulator.getData().clear();
			sentenceAccumulator.getData().clear();
			network.start(ctx).get();
			return parseAccumulator.getData();
		} 
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public DataFlowNetwork<AnalysisContext<TextDocument>> createNetwork()
	{		
		DataFlowNetwork<AnalysisContext<TextDocument>> network = new
			DataFlowNetwork<AnalysisContext<TextDocument>>();

		network.addChannel(new Channel<ParagraphAnn>(
						  ParagraphAnalyzer.PARAGRAPH_CHANNEL, 
						  new ParagraphAnn(0, 0), 30));
		
        network.addChannel(new Channel<SentenceAnn>(
                SentenceAnalyzer.SENTENCE_CHANNEL, 
                new SentenceAnn(0, 0), 30));
        
        network.addChannel(new Channel<EntityMaintainer>(
                EntityAnalyzer.ENTITY_CHANNEL, 
                EntityAnalyzer.EMPTY_EM, 30));
                
        network.addChannel(new Channel<RelexParse>(
                LinkGrammarProcessor.PARSED_SENTENCE_CHANNEL, 
                LinkGrammarProcessor.EOS_PARSE, 30));

        network.addChannel(new Channel<RelexParse>(
                RelexProcessor.RELEX_ANNOTATED_CHANNEL, 
                LinkGrammarProcessor.EOS_PARSE, 30));
        
        network.addChannel(new Channel<RelexParse>(
                ParseSelectProcessor.SELECTED_PARSE_CHANNEL, 
                LinkGrammarProcessor.EOS_PARSE, 30));
        
        network.addChannel(new Channel<SentenceInterpretation>(
                ToRelOccProcessor.SENTENCE_INTERPRETATIONS, 
                ToRelOccProcessor.EOS_SENTENCE_INTERPRETATION, 30));      
		
        network.addChannel(new Channel<HGHandle>(
                DiskoSearchAnalyzer.SEARCH_RESULTS, 
                UUIDHandleFactory.I.nullHandle(), 30));      

		network.addNode(new ParagraphAnalyzer(), 
						new String[] {},
						new String[] { 
							ParagraphAnalyzer.PARAGRAPH_CHANNEL 
						});

		network.addNode(new SentenceAnalyzer(), 
						new String[] { ParagraphAnalyzer.PARAGRAPH_CHANNEL },
						new String[] { SentenceAnalyzer.SENTENCE_CHANNEL });

        network.addNode(new EntityAnalyzer(), 
	                new String[]{SentenceAnalyzer.SENTENCE_CHANNEL}, 
	                new String[]{EntityAnalyzer.ENTITY_CHANNEL});
        
        network.addNode(linkGrammar,
                new String[] { EntityAnalyzer.ENTITY_CHANNEL },
                new String[] { LinkGrammarProcessor.PARSED_SENTENCE_CHANNEL});
        
        network.addNode(new RelexProcessor(),
                new String[] { LinkGrammarProcessor.PARSED_SENTENCE_CHANNEL},
                new String[] { RelexProcessor.RELEX_ANNOTATED_CHANNEL });
        
        network.addNode(new ParseSelectProcessor(),
                new String[] { EntityAnalyzer.ENTITY_CHANNEL, RelexProcessor.RELEX_ANNOTATED_CHANNEL },
                new String[] { ParseSelectProcessor.SELECTED_PARSE_CHANNEL });
        
        network.addNode(new ToRelOccProcessor(),
                new String[] { EntityAnalyzer.ENTITY_CHANNEL, ParseSelectProcessor.SELECTED_PARSE_CHANNEL },
                new String[] { ToRelOccProcessor.SENTENCE_INTERPRETATIONS });        
 
        parseAccumulator = 
        	new AccumulatingNode<AnalysisContext<TextDocument>, 
        					    SentenceInterpretation>(ToRelOccProcessor.SENTENCE_INTERPRETATIONS);
        sentenceAccumulator = 
        	new AccumulatingNode<AnalysisContext<TextDocument>, 
        					    SentenceAnn>(SentenceAnalyzer.SENTENCE_CHANNEL);
        
		network.addNode(parseAccumulator, 
						new String[] {ToRelOccProcessor.SENTENCE_INTERPRETATIONS}, 
						new String[]{});
        network.addNode(sentenceAccumulator, 
        				new String[] {SentenceAnalyzer.SENTENCE_CHANNEL}, 
        				new String[]{});
        
		linkGrammar.setHost(BotApp.config().at("linkgrammarServer", "localhost").asString());
		linkGrammar.setPort(9000);
		
        return network;
	}

	public LinkGrammarProcessor getLinkGrammar()
	{
		return linkGrammar;
	}

	public void setLinkGrammar(LinkGrammarProcessor linkGrammar)
	{
		this.linkGrammar = linkGrammar;
	}
}
