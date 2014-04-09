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

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import mjson.Json;

import org.sharegov.servicebot.rest.WebClient;

import alice.tuprolog.Struct;
import alice.tuprolog.Term;

public class AddressProcessInputTransform implements InputTransform
{
	private List<Term> getGisFacts(String addressLine, String city, String zipCode)
	{
		ArrayList<Term> result = new ArrayList<Term>();
		try
		{
			String url = getAddressUrl(addressLine, zipCode);
			String gisresp = WebClient.doGet(url);
			Json ainfo = Json.read(gisresp);
//			if (ainfo == null)
//				System.err.println("oops:" + gisresp);
			if (ainfo.asJsonList().size() > 0)
				result.add(new Struct("municipality", 
						new Struct(ainfo.at(0).at("municipality").asString())));
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
		return result;
	}
	
	@Override
	public Struct transform(InteractionFrame frame, Struct in)
	{
		if (!in.isList())
			return in;
		Struct frameHandle = new Struct(BotApp.get().getGraph().getHandle(frame).getPersistent().toString());
		List<Term> args = new ArrayList<Term>();
		String addressLine = null;
		String city = null;
		String zipCode = null;
		while (!in.isEmptyList())
		{
			Term head = in.listHead();
			if (head instanceof Struct)
			{
				Struct s = (Struct)head;
				if (s.getName().equals("form_field"))
				{
					String fieldName = ((Struct)s.getArg(0)).getName();
					String fieldValue = ((Struct)s.getArg(1)).getName();
					if (fieldName.equals("addressLine"))
						addressLine = fieldValue;
					else if (fieldName.equals("city"))
						city = fieldValue;
					else if (fieldName.equals("zipCode"))
						zipCode = fieldValue;
					args.add(new Struct("form_field", frameHandle, s.getArg(0), s.getArg(1)));					
				}
			}
			in = in.listTail();
		}
		if (addressLine != null)
			args.addAll(getGisFacts(addressLine, city, zipCode));
		return new Struct(",", args.toArray(new Term[0]));
	}
	
	public String toString()
	{
		return this.getClass().getSimpleName();
	}

	public static void main(String []argv)
	{
		BotApp.get().initialize(BotApp.config());
		
//		BotApp.get().getGraph().define(BotApp.get().getGraph().getHandleFactory().makeHandle("2bd57850-4855-4db6-824a-72bf043a0eaa"),				
//			  	   new AddressProcessInputTransform());				
		
		System.out.println(BotApp.get().getGraph().getHandleFactory().makeHandle());
	}
	
	@SuppressWarnings("deprecation")
	private String getAddressUrl(String address, String zip) {
		String url = BotApp.config().at("gisService").asString(); 
		
		StringBuilder builder = new StringBuilder();
		builder.append(url);
		builder.append("/candidates?street=");
		builder.append(URLEncoder.encode(address));
		builder.append("&zip=");
		builder.append(zip);
		
		return builder.toString();
	}
}
