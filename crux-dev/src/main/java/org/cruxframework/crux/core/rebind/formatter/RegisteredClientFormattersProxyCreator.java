/*
 * Copyright 2011 cruxframework.org.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.cruxframework.crux.core.rebind.formatter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.cruxframework.crux.core.client.Legacy;
import org.cruxframework.crux.core.client.collection.FastMap;
import org.cruxframework.crux.core.client.formatter.Formatter;
import org.cruxframework.crux.core.client.formatter.RegisteredClientFormatters;
import org.cruxframework.crux.core.rebind.AbstractInterfaceWrapperProxyCreator;
import org.cruxframework.crux.core.rebind.CruxGeneratorException;
import org.cruxframework.crux.core.rebind.context.RebindContext;
import org.cruxframework.crux.core.rebind.screen.View;

/**
 * @author Thiago da Rosa de Bustamante
 *
 */
@Deprecated
@Legacy
public class RegisteredClientFormattersProxyCreator extends AbstractInterfaceWrapperProxyCreator
{
	private Map<String, Boolean> formattersAdded = new HashMap<String, Boolean>();
	private final View view;

	/**
	 * Constructor
	 * @param context
	 * @param view
	 */
	public RegisteredClientFormattersProxyCreator(RebindContext context, View view)
    {
	    super(context, context.getGeneratorContext().getTypeOracle().findType(RegisteredClientFormatters.class.getCanonicalName()), false);
		this.view = view;
    }	

	@Override
	public String getProxySimpleName()
	{
		String className = view.getId(); 
		className = className.replaceAll("[\\W]", "_");
		return "RegisteredFormatters_"+className;
	}

	/**
	 * @see org.cruxframework.crux.core.rebind.AbstractProxyCreator#generateProxyContructor(com.google.gwt.user.rebind.SourcePrinter)
	 */
	@Override
    protected void generateProxyContructor(SourcePrinter srcWriter) throws CruxGeneratorException
    {
		srcWriter.println("public "+getProxySimpleName()+"(){ ");

		Iterator<String> iterator = view.iterateFormatters();
		while (iterator.hasNext())
		{
			String formatter = iterator.next();
			generateFormatterBlock(srcWriter, formatter);
		}
		srcWriter.println("}");
    }

	/**
	 * @see org.cruxframework.crux.core.rebind.AbstractProxyCreator#generateProxyFields(com.google.gwt.user.rebind.SourcePrinter)
	 */
	@Override
    protected void generateProxyFields(SourcePrinter srcWriter) throws CruxGeneratorException
    {
		srcWriter.println("private FastMap<Formatter> clientFormatters = new FastMap<Formatter>();");
    }

	/**
	 * @see org.cruxframework.crux.core.rebind.AbstractProxyCreator#generateProxyMethods(com.google.gwt.user.rebind.SourcePrinter)
	 */
	@Override
    protected void generateProxyMethods(SourcePrinter srcWriter) throws CruxGeneratorException
    {
		srcWriter.println("public Formatter getClientFormatter(String id){");
		srcWriter.println("return clientFormatters.get(id);");
		srcWriter.println("}");
    }

	/**
	 * @see org.cruxframework.crux.core.rebind.AbstractInterfaceWrapperProxyCreator#getImports()
	 */
	@Override
    protected String[] getImports()
    {
	    String[] imports = new String[] {
	    		Formatter.class.getCanonicalName(), 
	    		FastMap.class.getCanonicalName()
			};
	    return imports;
    }
	
	private void generateFormatterBlock(SourcePrinter sourceWriter, String formatter)
	{
		if (!formattersAdded.containsKey(formatter))
		{
			sourceWriter.println("clientFormatters.put(\""+formatter+"\", "+ context.getFormatters().getFormatterInstantionCommand(formatter) +");");
			formattersAdded.put(formatter, true);
		}
	}
}
