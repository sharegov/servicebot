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
package org.sharegov.servicebot.proofread;

import java.util.HashMap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.HGQuery.hg;
import org.hypergraphdb.app.wordnet.data.Word;
import org.sharegov.servicebot.BotApp;
import org.sharegov.servicebot.UPattern;
import org.sharegov.servicebot.WordOccurrence;
import org.sharegov.servicebot.pattern.PatternOccurrence;
import org.sharegov.servicebot.pattern.SyntacticPattern;
import org.sharegov.servicebot.pattern.SyntaxPatternRecognizer;

import disko.SentenceAnn;
import disko.data.relex.SentenceInterpretation;

public class WordBanProofReader extends AbstractProofReader
{
	HyperGraph graph = BotApp.get().getGraph();
	
	public void checkSentence(String s, int offset, Set<String> bannedSet, Set<RedInk> S)
	{
		String [] A = s.split("\\b");
		int pos = 0;
		for (String w : A)
		{
			if (w.matches("\\w+") && 
				bannedSet.contains(w.toLowerCase()))
				S.add(new RedInk(offset + pos, offset + pos + w.length(), w, null));
			pos+=w.length();
		}
	}
	
	public Set<RedInk> read(String text)
	{		
		Set<String> bannedSet = getBannedWords();
		SyntaxPatternRecognizer recognizer = new SyntaxPatternRecognizer();
		recognizer.setHyperGraph(graph);
		recognizer.setBundleAsSinglePattern(false);
		Set<PatternOccurrence> patterns = recognizer.detectPatterns(new HashMap<HGHandle, Double>(), text);
		
		System.out.println("Found sentences:");
		for (SentenceAnn ann : recognizer.getParser().getSentences())
			System.out.println("\n@" + ann.getInterval().getStart() + "\n" + ann.getSentence() + "\n\n");
		Set<RedInk> redink = new HashSet<RedInk>();
		for (PatternOccurrence occ : patterns)
		{
			SyntacticPattern p = graph.get(occ.getPattern());
			if (p.getArity() != 1)
				continue;
			Object first = graph.get(p.getTargetAt(0));
			if (! (first instanceof WordOccurrence))
				continue;
			WordOccurrence w = (WordOccurrence)first;
			if (!bannedSet.contains(w.getText()))
				continue;
			RedInk ink = new RedInk();
			ink.setFrom(occ.getBegin());
			if (occ.getBegin() != occ.getEnd())
				ink.setTo(occ.getEnd());
			else
				ink.setTo(occ.getBegin() + w.getText().length());
			ink.setText(w.getText());
			redink.add(ink);
		}
//		for (SentenceInterpretation sen : recognizer.getParses())
//			if (sen.getRelOccs().isEmpty())
//				checkSentence(sen.getSentence(), bannedSet, redink);
		Iterator<SentenceAnn> sentences = recognizer.getParser().getSentences().iterator();
		Iterator<SentenceInterpretation> parses = recognizer.getParser().getParses().iterator();		
		while (sentences.hasNext())
		{
			if (!parses.next().getRelOccs().isEmpty())
				sentences.next(); // skip, already dealt with
			else // couldn't parse it, so just do a word-by-word split
			{
				SentenceAnn ann = sentences.next();
				this.checkSentence(ann.getSentence(), ann.getInterval().getStart(), bannedSet, redink);
			}
		}
		return redink;
	}
	
	public Set<String> getBannedWords()
	{
		HyperGraph graph = BotApp.get().getGraph();		
		HGHandle thisHandle = graph.getHandle(this);
		List<Word> bannedList = hg.getAll(graph, 
				hg.apply(hg.targetAt(graph, 1),
						 hg.and(hg.type(BanLink.class), hg.incident(thisHandle))));
		Set<String> bannedSet = new HashSet<String>();
		for (Word w : bannedList)
			bannedSet.add(w.getLemma());
		return bannedSet;
	}
	
	public void banWord(HGHandle word)
	{
		HyperGraph graph = BotApp.get().getGraph();
//		HGHandle lhandle = hg.findOne(graph, hg.and(hg.type(BanLink.class),
//				hg.orderedLink(graph.getHandle(this), word)));
//		if (lhandle == null)
//			graph.add(new BanLink(graph.getHandle(this), word));
		hg.assertAtom(graph, new BanLink(graph.getHandle(this), word), true);
	}
	
	public void allowWord(HGHandle word)
	{
		HyperGraph graph = BotApp.get().getGraph();		
		HGHandle thisHandle = graph.getHandle(this);
		HGHandle banlink = hg.findOne(graph, 
						hg.and(hg.type(BanLink.class),
							   hg.orderedLink(thisHandle, word)));
		if (banlink != null)
			graph.remove(banlink);
	}
}
