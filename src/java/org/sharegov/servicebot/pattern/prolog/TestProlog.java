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
package org.sharegov.servicebot.pattern.prolog;

import org.hypergraphdb.HGEnvironment;
import org.hypergraphdb.HyperGraph;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Theory;
import alice.tuprolog.hgdb.HGPrologLibrary;

public class TestProlog
{
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		HyperGraph graph = HGEnvironment.get("c:/data/becky");
		
		// Benchmark how lightweight creation of Prolog engines really is.
		// It seems pretty fast. However, there are ways to optimize it - 
		// each prolog engine is constructed by loading a bunch of standard
		// libraries, where loading entails consulting a theory. The chat bot
		// will have one theory in addition to that which maintains state of
		// the current conversation. That theory can be safely serialized 
		// back&forth to the client to avoid maintaining server state (and be REST).
		// Since loading of the standard library theories is fast, loading of
		// 1 conversation theory should be fast enough. In addition, the theory
		// manager makes a distinction b/w static and dynamic factbase, where the 
		// static factbase contains theories from the libraries. If tuProlog is
		// re-factored a little bit to allow for sharing of the static factbase
		// b/w engines (the same way, the HGDB instance is shared), that'll save 
		// engine instantiation time and speed up request processing.
		long start = System.currentTimeMillis();		
		for (int i = 0; i < 1000; i++)
		{
			try
			{
				Prolog p = new Prolog();
				HGPrologLibrary.attach(graph, p);
				p.setTheory(new Theory("add(X,Y,Z):- Z is X + Y."));
				SolveInfo info = p.solve("add(34, 345, Z).");
				//System.out.println(info.getTerm("Z"));
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		long end = System.currentTimeMillis();
		System.out.println("Total= " + ((double)(end-start)/1000.0));
	}
}
