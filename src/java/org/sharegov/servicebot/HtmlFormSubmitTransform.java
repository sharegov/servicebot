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

import java.util.ArrayList;
import java.util.List;
import org.hypergraphdb.handle.UUIDHandleFactory;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;

public class HtmlFormSubmitTransform implements InputTransform 
{
	@Override
	public Struct transform(InteractionFrame frame, Struct in) 
	{
		if (!in.isList())
			return in;
		Struct frameHandle = new Struct(BotApp.get().getGraph().getHandle(frame).getPersistent().toString());
		List<Term> args = new ArrayList<Term>();
		while (!in.isEmptyList())
		{
			Term head = in.listHead();
			if (head instanceof Struct)
			{
				Struct s = (Struct)head;
				if (s.getName().equals("form_field"))
					args.add(new Struct("form_field", frameHandle, s.getArg(0), s.getArg(1)));
			}
			in = in.listTail();
		}
		return new Struct(",", args.toArray(new Term[0]));
	}

	public String toString()
	{
		return this.getClass().getSimpleName();
	}
	
	public static void main(String []argv)
	{
		System.out.println(UUIDHandleFactory.I.makeHandle().toString());
	}
}
