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
package org.sharegov.servicebot;


import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import mjson.Json;

import org.hypergraphdb.HGConfiguration;
import org.hypergraphdb.HGEnvironment;
import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.app.management.HGManagement;
import org.hypergraphdb.atom.HGSubsumes;
import org.hypergraphdb.atom.HGTypeStructuralInfo;
import org.hypergraphdb.indexing.ByPartIndexer;
import org.hypergraphdb.util.HGUtils;
import org.sharegov.servicebot.pattern.AndPattern;
import org.sharegov.servicebot.pattern.ArithmeticMean;
import org.sharegov.servicebot.pattern.MaxFunction;
import org.sharegov.servicebot.pattern.MinFunction;
import org.sharegov.servicebot.pattern.OrPattern;
import org.sharegov.servicebot.pattern.PatternType;
import org.sharegov.servicebot.pattern.PhrasePattern;
import org.sharegov.servicebot.pattern.PhraseRecognizer;
import org.sharegov.servicebot.pattern.RegexRecognizer;
import org.sharegov.servicebot.pattern.SyntacticPattern;
import org.sharegov.servicebot.pattern.SyntaxPatternRecognizer;
import org.sharegov.servicebot.pattern.SyntaxStructure;
import org.sharegov.servicebot.pattern.TextPattern;
import org.sharegov.servicebot.pattern.UtteranceRecognizer;

import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.hgdb.PrologHGDBApp;
import alice.tuprolog.hgdb.PrologNode;
import disko.DISKOApplication;
import disko.data.BasicWords;
import disko.data.UnknownWord;
import disko.data.relex.SyntacticPredicate;

/**
 * 
 * <p>
 * Represents the AskBecky application - a gateway to global information, and
 * some global operations.
 * </p>
 * 
 * @author Borislav Iordanov
 * 
 */
public class BotApp
{
	private static final BotApp inst = new BotApp();
	private Properties configuration = new Properties();
	private Json config = Json.object("workingDir", "c:/work/servicebot", "port", 8222);
	
	public static BotApp get()
	{
		return inst;
	}

	public static Json config()
	{
		return get().config;
	}
	
	private HyperGraph graph;
	private PrologNode prologNode;
	
	private void initDisko(String databaseLocation)
	{
		String diskoHome = config.at("diskoHome", "/home/borislav/disko").asString();
		System.setProperty("gate.home", 
							getConfigProperty("gate.home", diskoHome + "/data/gate"));
		System.setProperty("relex.morphy.Morphy", 
							getConfigProperty("relex.morphy.Morphy", "disko.relex.MorphyHGDB"));
		//System.setProperty("relex.morphy.Morphy", "relex.morphy.MapMorphy");
		//System.setProperty("relex.algs.Morphy", "relex.algs.Morphy");
		System.setProperty("morphy.hgdb.location", 
							getConfigProperty("morphy.hgdb.location", databaseLocation));
		System.setProperty("relex.algpath", 
						   getConfigProperty("relex.algpath", diskoHome + "/data/relex-semantic-algs.txt"));
		//System.setProperty("relex.parser.LinkParser.pathname", "D:/work/disco/trunk/data/linkparser"); 
		//System.setProperty("relex.parser.LinkParser.pathname", "D:/work/disco/trunk/data/linkparser"); 
		//relex.RelexProperties.setProperty("relex.parser.LinkParser.pathname", System.getProperty("relex.parser.LinkParser.pathname"));
		System.setProperty("wordnet.configfile", 
						   getConfigProperty("wordnet.configfile", diskoHome + "/data/wordnet/file_properties.xml"));
		System.setProperty("EnglishModelFilename", 
							getConfigProperty("EnglishModelFilename", diskoHome + "/data/sentence-detector/EnglishSD.bin.gz"));		
	}
	
	private void createPatternTypes()
	{
		graph.add(new HGTypeStructuralInfo(graph.getPersistentHandle(
				graph.add(new PatternType("phrase", PhrasePattern.class, null))),
				Integer.MAX_VALUE, false));
		graph.add(new HGTypeStructuralInfo(graph.getPersistentHandle(
				graph.add(new PatternType("one_of", OrPattern.class, new MaxFunction()))),
				Integer.MAX_VALUE, false));
		graph.add(new HGTypeStructuralInfo(graph.getPersistentHandle(
				graph.add(new PatternType("all_of", AndPattern.class, new MinFunction()))),
				Integer.MAX_VALUE, false));
		graph.add(new HGTypeStructuralInfo(graph.getPersistentHandle(
				graph.add(new PatternType("syntax_structure", SyntaxStructure.class, new ArithmeticMean()))),
				Integer.MAX_VALUE, false));
		HGHandle textType = graph.add(new PatternType("text_asis", TextPattern.class, null));
		HGHandle utteranceType = graph.add(new PatternType("utterance_asis", Utterance.class, null));
		HGHandle beckyUtteranceType = graph.add(new PatternType("becky_utterance", BotUtterance.class, null));		
		HGHandle wordType = graph.add(new PatternType("word_asis", WordOccurrence.class, null));
		
		graph.add(new HGSubsumes(textType, wordType));
		graph.add(new HGSubsumes(textType, utteranceType));
		graph.add(new HGSubsumes(utteranceType, beckyUtteranceType));
		
		List<SyntacticPredicate> predicates = hg.getAll(graph, hg.type(SyntacticPredicate.class));
		for (SyntacticPredicate pred : predicates)
			graph.add(new PatternType(pred.getName(), SyntacticPattern.class, null));
		for (String name : BasicWords.PREPOSITIONS)
			graph.add(new PatternType(name, SyntacticPattern.class, null));
		for (String name : BasicWords.PARTICLES)
			graph.add(new PatternType(name, SyntacticPattern.class, null));
		for (String name : BasicWords.CONJUNCTIONS)
			graph.add(new PatternType(name, SyntacticPattern.class, null));
		for (String name : BasicWords.PRONOUNS)
			graph.add(new PatternType(name, SyntacticPattern.class, null));

		graph.add(new PatternType("person", SyntacticPattern.class, null));
		graph.add(new PatternType("pronoun", SyntacticPattern.class, null));		
		graph.add(new PatternType("conjunction", SyntacticPattern.class, null));
		graph.add(new PatternType("polyword", SyntacticPattern.class, null));
		graph.add(new PatternType("present_perfect", SyntacticPattern.class, null));
	}

	private void createPatternRecognizers()
	{
		graph.add(new UtteranceRecognizer());
		graph.add(new SyntaxPatternRecognizer());
		graph.add(new PhraseRecognizer());
		graph.add(new RegexRecognizer());
	}
	
	private void createInteractionFrameEnvironment()
	{
		HGManagement.ensureInstalled(graph, new PrologHGDBApp());
		InteractionFrame topFrame = new InteractionFrame();
		HGHandle greetingOutput = getPrologNode().add(new Struct("utterance",
				new Struct("Hi, How may I help you?"))); 
		topFrame.setBotOutput(greetingOutput);
		topFrame.setName("Welcome");
		HGHandle itransform = getGraph().getHandleFactory().makeHandle("2b76c7be-fe53-4a5a-b895-a9f522a98770");
		getGraph().define(itransform, 
						  new EnglishParseInputTransform());
		getGraph().define(getGraph().getHandleFactory().makeHandle("bfce41d7-c431-49b3-936d-a8da9245cca2"),				
				  	   new HtmlFormSubmitTransform());
		getGraph().define(getGraph().getHandleFactory().makeHandle("2bd57850-4855-4db6-824a-72bf043a0eaa"),				
			  	   new AddressProcessInputTransform());				
		topFrame.setInputTransform(itransform);
		HGHandle frameHandle = getPrologNode().add(topFrame);
		getPrologNode().add(new Activates(graph.getHandleFactory().nullHandle(), frameHandle));
	}
	
	private HGConfiguration dbConfig()
	{
		HGConfiguration config = new HGConfiguration();
		//config.setStoreImplementation(new BJEStorageImplementation());
		return config;
	}
	
	/**
	 * <p>
	 * Setup a new AskBecky database at the given location, possibly deleting an
	 * existing one there (if the <code>deleteExisting</code> parameter is
	 * true).
	 * </p>
	 */
	public void setup(String databaseLocation, boolean deleteExisting)
	{
		System.out.println("Setting up becky app.");
		if (deleteExisting)
			HGUtils.dropHyperGraphInstance(databaseLocation);
		graph = HGEnvironment.get(databaseLocation, dbConfig());
		
		if (graph.getTypeSystem().getTypeHandleIfDefined(UnknownWord.class) == null)
			new DISKOApplication().install(graph);
		
		prologNode = new PrologNode(graph);
		createInteractionFrameEnvironment();
		createPatternTypes();
		createPatternRecognizers();
		// graph.getIndexManager().register(
		// new
		// ByPartIndexer(graph.getTypeSystem().getTypeHandle(Scenario.class),
		// "createdBy"));
		graph.getIndexManager().register(
				new ByPartIndexer(findPatternType("text_asis"), "text"));
		graph.runMaintenance();
	}

	/**
	 * <p>
	 * Initialize the AskBecky application at the given database location. If no
	 * database exists at that location, it will be created and initialized via
	 * a call to the <code>setup</code> method.
	 * </p>
	 * 
	 * @param databaseLocation
	 */
	public void initialize(Json config)
	{
		this.config = config;
		String databaseLocation = config.at("workingDir").asString() + "/data/db";
		try
		{
			if (!HGEnvironment.exists(databaseLocation) && false)				
				setup(databaseLocation, false);
			else
			{
				graph = HGEnvironment.get(databaseLocation, dbConfig());
				prologNode = new PrologNode(graph);
			}
			
			if (hg.count(graph, hg.type(PatternType.class)) == 0)
				setup(databaseLocation, false);
			initDisko(databaseLocation);
		}
		catch (Throwable t)
		{
			System.err.println("Exception during initialization of BotApp, stack trace follows...");
			t.printStackTrace(System.err);
		}
	}

	public HyperGraph getGraph()
	{
		return graph;
	}
	
	public PrologNode getPrologNode()
	{
		return prologNode;
	}
	
	public HGHandle findPatternType(String head)
	{
		return hg.findOne(graph, hg.and(hg.type(PatternType.class), hg.eq("head", head)));		
	}

	public Properties getConfiguration()
	{
		return configuration;
	}

	public void setConfiguration(Properties configuration)
	{
		this.configuration = configuration;
	}
	
	public String getConfigProperty(String name, String def)
	{
		String x = getConfiguration().getProperty(name);
		return x != null ? x : def;
	}
	
	public static void main(String []argv)
	{
//		HyperGraph graph = HGEnvironment.get("c:/data/becky");
//		System.out.println("DB opened successfully.");
//		graph.close();
		Calendar al = Calendar.getInstance();
		al.setTime(new Date(2011-1900, 4, 1));
		System.out.println(al.getTimeInMillis());
		al.setTime(new Date(2011-1900, 5, 1));
		System.out.println(al.getTimeInMillis());
		BotApp.get().initialize(BotApp.get().config);

		PrologNode prologNode = BotApp.get().getPrologNode();
		Struct s1 = new Struct("sss", new Struct("baasdf"));
		Struct s2 = new Struct("sss", new Struct("basdfgasdgdsfgd"));
		HGHandle h1 = prologNode.add(s1);
		HGHandle h2 = prologNode.add(s2);
		prologNode.release(h1);
		prologNode.release(h2);
	}
}
