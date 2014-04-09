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

import java.beans.PropertyDescriptor;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGHandleHolder;
import org.hypergraphdb.HGPersistentHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.IncidenceSetRef;
import org.hypergraphdb.LazyRef;
import org.hypergraphdb.annotation.HGIgnore;
import org.hypergraphdb.type.BonesOfBeans;
import org.hypergraphdb.type.DefaultJavaTypeMapper;
import org.hypergraphdb.type.HGAtomType;
import org.hypergraphdb.type.HGCompositeType;
import org.hypergraphdb.type.HGProjection;
import org.hypergraphdb.type.JavaTypeFactory;
import org.hypergraphdb.util.HGUtils;
import org.sharegov.servicebot.UPattern;

/**
 * 
 * <p>
 * A <code>PatternType</code> is uniquely identified by its head. In addition it contains
 * the Java class of the pattern instances (retrieved by the HGTypeSystem HGAtomType<->Java class
 * association), a {@link MatchingFunction} that calculates a matching score for the pattern
 * in the current environment. Patterns that are inferred directly from raw input (instead of 
 * matching) do not have a scoring function - usually (but not necessarily always) they are just 
 * a representation of the input and have a score of 1.0. 
 * </p>
 *
 * @author Borislav Iordanov
 *
 */
public class PatternType implements HGCompositeType, HGHandleHolder
{
	private HyperGraph graph;
	private HGHandle handle;
	private Class<? extends UPattern> patternClass;
    private Constructor<? extends UPattern> defaultConstructor = null;
    private Constructor<? extends UPattern> linkConstructor = null;
    private Constructor<? extends UPattern> handleListConstructor = null;
	private String head;
	private MatchFunction matcher;
	
	@SuppressWarnings("unchecked")
	private void initPatternClass()
	{
//		if (patternClass == null) // this would be the case when we just created a new PatternType
//				patternClass = (Class<? extends UPattern>)
//					graph.getTypeSystem().getClassForType(handle);		
    	defaultConstructor = 
    		(Constructor<? extends UPattern>)JavaTypeFactory.findDefaultConstructor(patternClass);
    	if (defaultConstructor != null)
    	    defaultConstructor.setAccessible(true);
        try
        {
        	linkConstructor = 
        		(Constructor<? extends UPattern>)patternClass.getDeclaredConstructor(new Class[] {HGHandle[].class} );
        	if (linkConstructor != null)
        	    linkConstructor.setAccessible(true);
        }
        catch (NoSuchMethodException ex) 
        { 
            handleListConstructor = 
            	(Constructor<? extends UPattern>)JavaTypeFactory.findHandleArgsConstructor(patternClass);
            if (handleListConstructor != null)
                handleListConstructor.setAccessible(true);
        } 		
	}

	public PatternType()
	{		
	}
	
	public PatternType(String head, Class<? extends UPattern> patternClass, MatchFunction matcher)
	{
		this.head = head;
		this.patternClass = patternClass;
		this.matcher = matcher;
	}
	
	public UPattern createNew()
	{
		try
		{
			return defaultConstructor.newInstance();
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	public TextPattern createNew(String text)
	{
		try
		{
			TextPattern p = (TextPattern)defaultConstructor.newInstance();
			p.setText(text);
			return p;
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
		
	}
	
	public CompPattern createNew(List<HGHandle> subs)	
	{
		try
		{
			if (linkConstructor != null)
				return (CompPattern)linkConstructor.newInstance(new Object[]{subs.toArray(new HGHandle[0])});
			else if (handleListConstructor != null)
				return (CompPattern)handleListConstructor.newInstance((Object[])subs.toArray(new HGHandle[0]));
			else
				throw new Exception("No appropriate constructor for CompPattern which must be a HGLink");
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}		
	}
		
	public Iterator<String> getDimensionNames()
	{
		Set<String> props = BonesOfBeans.getAllPropertyDescriptors(patternClass).keySet();
		for (Iterator<String> i = props.iterator(); i.hasNext(); )
			if (!DefaultJavaTypeMapper.includeProperty(patternClass, 
					BonesOfBeans.getPropertyDescriptor(patternClass, 
													   i.next())))
				i.remove();
		return props.iterator();
	}

	public HGProjection getProjection(String dimensionName)
	{
		final PropertyDescriptor desc = BonesOfBeans.getPropertyDescriptor(patternClass, dimensionName);
		return new HGProjection()
		{
			public int[] getLayoutPath()
			{
				return null;
			}

			public String getName()
			{
				return desc.getName();
			}

			public HGHandle getType()
			{
				return graph.getTypeSystem().getTypeHandle(desc.getPropertyType());
			}

			public void inject(Object atomValue, Object value)
			{
				BonesOfBeans.setProperty(atomValue, desc, value);
			}

			public Object project(Object atomValue)
			{
				return BonesOfBeans.getProperty(atomValue, desc);
			}			
		};
	}

	@SuppressWarnings("unchecked")
	public Object make(HGPersistentHandle handle,
					   LazyRef<HGHandle[]> targetSet, 
					   IncidenceSetRef incidenceSet)
	{
		UPattern instance = null;
		try
		{
			if (targetSet != null)
			{
				if (linkConstructor != null)
					instance = linkConstructor.newInstance(new Object[]{(HGHandle[])targetSet.deref()});
				else if (handleListConstructor != null)
					instance = handleListConstructor.newInstance((Object[])targetSet.deref());
				else
					instance = defaultConstructor.newInstance();
			}
			else
				instance = defaultConstructor.newInstance();
		}
		catch (Exception ex)
		{
			HGUtils.throwRuntimeException(ex);
		}
		if (graph.getHandleFactory().nullHandle().equals(handle))
			return instance;
		if (graph.getStore().getLink(handle) == null)
			return instance;
		
		HGAtomType mapType = graph.getTypeSystem().getAtomType(HashMap.class);
		BonesOfBeans.setPropertiesFromMap(instance, (Map<String, Object>)mapType.make(handle, null, null));		
		return instance;
	}

	public void release(HGPersistentHandle handle)
	{
		if (graph.getHandleFactory().nullHandle().equals(handle))
			return;
		HGAtomType mapType = graph.getTypeSystem().getAtomType(HashMap.class);
		if (graph.getStore().getLink(handle) == null)
			return;
		mapType.release(handle);
	}

	public HGPersistentHandle store(Object instance)
	{
		Map<String, Object> properties = BonesOfBeans.getPropertiesAsMap(instance);
		if (properties.isEmpty())
			return graph.getHandleFactory().nullHandle();
		else
		{
			HGAtomType mapType = graph.getTypeSystem().getAtomType(HashMap.class);
			return mapType.store(properties);
		}
	}

	public boolean subsumes(Object general, Object specific)
	{
		// this obviously can be expanded upon...defining some subsumption
		// relationships between patterns
		return HGUtils.eq(general, specific);
	}

	public void setHyperGraph(HyperGraph graph)
	{
		this.graph = graph;
	}

	@HGIgnore
	public HGHandle getAtomHandle()
	{
		return handle;
	}

	public void setAtomHandle(HGHandle handle)
	{
		this.handle = handle;
		initPatternClass();
	}

	public String getHead()
	{
		return head;
	}

	public void setHead(String head)
	{
		this.head = head;
	}

	public MatchFunction getMatcher()
	{
		return matcher;
	}

	public void setMatcher(MatchFunction matcher)
	{
		this.matcher = matcher;
	}	
	
	public Class<? extends UPattern> getPatternClass()
	{
		return patternClass;
	}
	
	public String getPatternClassName()
	{
		return patternClass == null ? null : patternClass.getName();
	}
	
	@SuppressWarnings("unchecked")
	public void setPatternClassName(String className)
	{
		try
		{
			patternClass = (Class<? extends UPattern>) Thread.currentThread().getContextClassLoader().loadClass(className);						
		}
		catch (ClassNotFoundException ex)
		{
			try { patternClass = (Class<? extends UPattern>) Class.forName(className); }
			catch (Exception e) { throw new RuntimeException(ex); }
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}
}
