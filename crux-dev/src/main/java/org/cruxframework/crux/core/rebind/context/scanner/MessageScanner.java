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
package org.cruxframework.crux.core.rebind.context.scanner;

import java.util.HashMap;
import java.util.Map;

import org.cruxframework.crux.core.client.i18n.MessageName;
import org.cruxframework.crux.core.rebind.CruxGeneratorException;
import org.cruxframework.crux.core.rebind.context.JClassScanner;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.i18n.client.LocalizableResource;

/**
 * Class for retrieve the messages interface, based on the annotation MessageName.
 * @author Thiago da Rosa de Bustamante
 */
public class MessageScanner 
{
	private boolean initialized = false;	
	private JClassScanner jClassScanner;
	private Map<String, String> messagesClasses = null;

	public MessageScanner(JClassScanner jClassScanner)
	{
		this.jClassScanner =  jClassScanner;
	}

	/**
	 * Return the className associated with the name informed
	 * @param interfaceName
	 * @return
	 */
	public String getMessageClass(String message)
	{
		initializeMessages();
		return messagesClasses.get(message);
	}

	/**
	 * @param urls
	 */
	protected void initializeMessages()
	{
		if (!initialized)
		{
			if (messagesClasses == null)
			{
				messagesClasses = new HashMap<String, String>();
				JClassType[] messagesTypes;
				try 
				{
					messagesTypes =  jClassScanner.searchClassesByInterface(LocalizableResource.class.getCanonicalName());
				} 
				catch (Exception e) 
				{
					throw new CruxGeneratorException("Error initializing messages scanner.",e);
				}
				if (messagesTypes != null)
				{
					for (JClassType messageClass : messagesTypes) 
					{
						MessageName messageNameAnnot = messageClass.getAnnotation(MessageName.class);
						if (messageNameAnnot != null)
						{
							String messageKey = messageNameAnnot.value();
							if (messagesClasses.containsKey(messageKey))
							{
								throw new CruxGeneratorException("Duplicated Message Key: ["+messageKey+"].");
							}
							messagesClasses.put(messageKey, messageClass.getQualifiedSourceName());
						}
					}
				}
			}
			initialized = true;
		}
	}
}
