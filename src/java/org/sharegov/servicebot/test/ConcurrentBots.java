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
package org.sharegov.servicebot.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.sharegov.servicebot.BotApp;
import org.sharegov.servicebot.PrologUtils;
import org.sharegov.servicebot.SmartBot;
import org.sharegov.servicebot.StartUp;

import alice.tuprolog.Struct;

import mjson.Json;

public class ConcurrentBots
{
	public static class BotDialog implements Runnable
	{
		Json dialog;
		public BotDialog(Json dialog) { this.dialog = dialog; }
		
		public void run()
		{
			SmartBot bot = new SmartBot();
			try {
			for (int i = 0; i < dialog.asJsonList().size(); i++)
			{
				Json call = dialog.at(i);
				Struct input = (Struct)PrologUtils.fromJson(call);
				Struct output = bot.respondTo(input);
				//System.out.println(input.toString() + "->" + output);
				Json botresponse = PrologUtils.toJson(output);
				Json expectedResponse = dialog.at(++i);
				assert botresponse.equals(expectedResponse);
			}
			}
			catch (Throwable t)
			{
				t.printStackTrace(System.err);
				System.exit(-1);
			}
		}
	}
	
	public static void main(String []args)
	{
		Json scenario = Json.nil();
		try
		{
			BotApp.get().initialize(Json.read(new String(StartUp.getBytesFromFile(new File(args[0])))));
			scenario = Json.read(new String(StartUp.getBytesFromFile(new File(args[1]))));
		}
		catch (Throwable t)
		{
			t.printStackTrace(System.err);
			System.exit(-1);
		}
				
		int nthreads = 10;
		while (true)
		{		
			ExecutorService eservice = Executors.newFixedThreadPool(nthreads);
			for (int i = 0; i < nthreads; i++)
				eservice.execute(new BotDialog(scenario));
			eservice.shutdown();
			try
			{
				eservice.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//break;
		}
	}
}
