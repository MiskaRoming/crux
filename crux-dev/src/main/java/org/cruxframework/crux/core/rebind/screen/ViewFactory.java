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
package org.cruxframework.crux.core.rebind.screen;
 
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cruxframework.crux.core.client.Legacy;
import org.cruxframework.crux.core.client.utils.StringUtils;
import org.cruxframework.crux.core.declarativeui.ViewProcessor;
import org.cruxframework.crux.core.rebind.CruxGeneratorException;
import org.cruxframework.crux.core.rebind.context.RebindContext;
import org.cruxframework.crux.core.utils.RegexpPatterns;
import org.cruxframework.crux.core.utils.StreamUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import com.google.gwt.dev.resource.Resource;


/**
 * Creates a representation for Crux views
 *  
 * @author Thiago Bustamante
 *
 */
public class ViewFactory 
{
	private static final Log logger = LogFactory.getLog(ViewFactory.class);
	private Map<String, View> cache = new HashMap<String, View>();
	private RebindContext context;
	private ViewProcessor viewProcessor;
	
	/**
	 * Default Constructor
	 */
	public ViewFactory(RebindContext context) 
	{
		this.context = context;
		this.viewProcessor = new ViewProcessor(context.getScreenLoader().getViewLoader());
	}
	
	/**
	 * Factory method for views.
	 * @param id viewId
	 * @param device device property for this permutation being compiled
	 * @return
	 * @throws ScreenConfigException
	 */
	public View getView(String id, String device) throws ScreenConfigException
	{
		String cacheKey = id + "_" + device; 
		if (cache.containsKey(cacheKey))
		{
			return cache.get(cacheKey);
		}
		
		Resource resource = context.getScreenLoader().getViewLoader().getView(id);
		if (resource == null)
		{
			throw new ScreenConfigException("View ["+id+"] not found!");
		}
		InputStream inputStream;
        try
        {
	        inputStream = resource.openContents();
        }
        catch (IOException e)
        {
			throw new ScreenConfigException("View ["+id+"] not found!");
		}
		Document viewDoc = viewProcessor.getView(inputStream, id, device);
		StreamUtils.safeCloseStream(inputStream);
		
		View view = getView(id, device, viewDoc, resource.getLastModified(), false);
		cache.put(cacheKey, view);
		return view;
	}
	
	/**
	 * Factory method for views.
	 * @param id
	 * @param view
	 * @param rootView
	 * @return
	 * @throws ScreenConfigException
	 */
	public View getView(String id, String device, Document view, long lastModified, boolean rootView) throws ScreenConfigException
	{
		try 
		{
			JSONObject metadata = viewProcessor.extractWidgetsMetadata(id, view, rootView);
			View result = parseView(id, metadata, rootView);
			result.setLastModified(lastModified);
			return result;
		} 
		catch (Exception e) 
		{
			throw new ScreenConfigException("Error retrieving view ["+id+"].", e);
		}
	}
	
	/**
	 * 
	 * @param id
	 * @param device
	 * @param stream
	 * @return
	 * @throws ScreenConfigException
	 */
	public Document getViewDocument(String id, String device, InputStream stream) throws ScreenConfigException
	{
		return viewProcessor.getView(stream, id, device);
	}
	
	public List<String> getViews(String viewLocator)
    {
	    return context.getScreenLoader().getViewLoader().getViews(viewLocator);
    }
	
	protected void generateHTML(String viewId, Document view, OutputStream out)
	{
		viewProcessor.generateHTML(viewId, view, out);
	}
	
	/**
	 * Creates a widget based in its metadata information.
	 * 
	 * @param elem
	 * @param view
	 * @return
	 * @throws ScreenConfigException
	 */
	private Widget createWidget(JSONObject elem, View view) throws ScreenConfigException
	{
		if (!elem.has("id"))
		{
			throw new CruxGeneratorException("The id attribute is required for CRUX Widgets. " +
					"On view ["+view.getId()+"], there is an widget of type ["+elem.optString("_type")+"] without id.");
		}
		String widgetId;
        try
        {
	        widgetId = elem.getString("id");
        }
        catch (JSONException e)
        {
			throw new CruxGeneratorException("The id attribute is required for CRUX Widgets. " +
					"On view ["+view.getId()+"], there is an widget of type ["+elem.optString("_type")+"] without id.");
        }
		Widget widget = view.getWidget(widgetId);
		if (widget != null)
		{
			throw new ScreenConfigException("Error creating widget. Duplicated identifier ["+widgetId+"].");
		}
		
		widget = newWidget(elem, widgetId);
		if (widget == null)
		{
			throw new ScreenConfigException("Can not create widget ["+widgetId+"]. Verify the widget type.");
		}
		
		view.addWidget(widget);
		
		createWidgetChildren(elem, view, widgetId, widget);
		
		return widget;
	}

	/**
	 * @param elem
	 * @param view
	 * @param widgetId
	 * @param widget
	 * @throws ScreenConfigException
	 */
	private void createWidgetChildren(JSONObject elem, View view, String widgetId, Widget widget) throws ScreenConfigException
    {
	    if (elem.has("_children"))
		{
			try
            {
	            JSONArray children = elem.getJSONArray("_children");
	            if (children != null)
	            {
	            	for (int i=0; i< children.length(); i++)
	            	{
	            		JSONObject childElem = children.getJSONObject(i);
	            		if (isValidWidget(childElem))
	            		{
	            			Widget child = createWidget(childElem, view);
	            			child.setParent(widget);
	            		}
	            		else if (isScreenDefinition(childElem))
	            		{
	    					parseViewElement(view,childElem);
	            		}
	            	}
	            }
            }
			catch (JSONException e)
            {
				throw new ScreenConfigException("Can not create widget ["+widgetId+"]. Verify the widget type.", e);
            }
		}
    }

	/**
	 * Test if a target json object represents a View definition for Crux.
	 * @param cruxObject
	 * @return
	 * @throws JSONException 
	 */
	private boolean isScreenDefinition(JSONObject cruxObject) throws JSONException
	{
		if (cruxObject.has("_type"))
		{
			String type = cruxObject.getString("_type");
			return (type != null && "screen".equals(type));
		}
		return false;
	}
	
	/**
	 * Builds a new widget, based on its metadata.
	 * @param elem
	 * @param widgetId
	 * @return
	 * @throws ScreenConfigException
	 */
	private Widget newWidget(JSONObject elem, String widgetId) throws ScreenConfigException
	{
		try 
		{
			String type = elem.getString("_type");
			Widget widget = new Widget(elem);
			widget.setId(widgetId);
			widget.setType(type);
			return widget;
		} 
		catch (Throwable e) 
		{
			throw new ScreenConfigException("Can not create widget ["+widgetId+"]. Verify the widget type.", e);
		} 
	}
	
	/**
	 * Parse the HTML page and build the Crux View. 
	 * @param id
	 * @param metaData
	 * @return
	 * @throws IOException
	 * @throws ScreenConfigException 
	 */
	private View parseView(String id, JSONObject metaData, boolean rootView) throws IOException, ScreenConfigException
	{
		try
		{
			JSONArray elementsMetadata = metaData.getJSONArray("elements");
			JSONObject lazyDependencies = metaData.getJSONObject("lazyDeps");
			String html = metaData.getString("_html");
			
			View view = new View(id, elementsMetadata, lazyDependencies, html, rootView);

			int length = elementsMetadata.length();
			for (int i = 0; i < length; i++) 
			{
				JSONObject compCandidate = elementsMetadata.getJSONObject(i);

				if (isScreenDefinition(compCandidate))
				{
					parseViewElement(view,compCandidate);
				}
				else if (isValidWidget(compCandidate))
				{
					try 
					{
						createWidget(compCandidate, view);
					} 
					catch (ScreenConfigException e) 
					{
						throw new ScreenConfigException("Error creating widget on view ["+id+"].", e);
					}
				}
			}
			return view;
		}
		catch (JSONException e)
		{
			throw new ScreenConfigException("Error parsing view ["+id+"].", e);
		}
	}

	/**
	 * Parse view element
	 * @param view
	 * @param elem
	 * @throws ScreenConfigException 
	 */
	private void parseViewElement(View view, JSONObject elem) throws ScreenConfigException 
	{
		try
        {
	        view.setViewElement(elem);
			String[] attributes = JSONObject.getNames(elem);
	        int length = attributes.length;
	        
	        for (int i = 0; i < length; i++) 
	        {
	        	String attrName = attributes[i];
	        	
	        	if(attrName.equals("useController"))
	        	{
	        		parseViewUseControllerAttribute(view, elem);
	        	}
	        	else if(attrName.equals("useResource"))
	        	{
	        		parseViewUseResourceAttribute(view, elem);
	        	}
	        	else if(attrName.equals("useFormatter"))
	        	{
	        		parseViewUseFormatterAttribute(view, elem);
	        	}
	        	else if(attrName.equals("useDataSource"))
	        	{
	        		parseViewUseDatasourceAttribute(view, elem);
	        	}
	        	else if(attrName.equals("useView"))
	        	{
	        		parseViewUseViewAttribute(view, elem);
	        	}
	        	else if (attrName.equals("width"))
				{
					view.setWidth(elem.getString(attrName));
				}
	        	else if(attrName.equals("height"))
				{
					view.setHeight(elem.getString(attrName));
				}
	        	else if(attrName.equals("smallViewport"))
				{
					view.setSmallViewport(elem.getString(attrName));
				}
	        	else if(attrName.equals("largeViewport"))
				{
					view.setLargeViewport(elem.getString(attrName));
				}
	        	else if(attrName.equals("disableRefresh"))
				{
					view.setDisableRefresh(elem.getBoolean(attrName));
				}
	        	else if (attrName.startsWith("on"))
	        	{
	        		Event event = EventFactory.getEvent(attrName, elem.getString(attrName));
	        		if (event != null)
	        		{
	        			view.addEvent(event);
	        		}
	        	}
	        	else if (attrName.equals("title"))
	        	{
	        		String title = elem.getString(attrName);
	        		if (title != null && title.length() > 0)
	        		{
	        			view.setTitle(title);
	        		}
	        	}
	        	else if (attrName.equals("fragment"))
	        	{
	        		String fragment = elem.getString(attrName);
	        		if (fragment != null && fragment.length() > 0)
	        		{
	        			view.setFragment(fragment);
	        		}
	        	}
	        	else if (attrName.equals("dataObject"))
	        	{
	        		String dataObject = elem.getString(attrName);
	        		if (dataObject != null && dataObject.length() > 0)
	        		{
	        			view.setDataObject(dataObject);
	        		}
	        	}
	        	else if (!attrName.equals("id") && !attrName.equals("_type"))
	        	{
	        		if (logger.isInfoEnabled()) logger.info("Error setting property ["+attrName+"] for view ["+view.getId()+"].");
	        	}
	        }
        }
        catch (JSONException e)
        {
	        throw new ScreenConfigException("Error parsing view metaData. View ["+view.getId()+"].");
        }
	}

	/**
	 * @param view
	 * @param elem
	 * @throws ScreenConfigException 
	 */
	/**
	 * @param view
	 * @param elem
	 * @throws ScreenConfigException 
	 */
	private void parseViewUseControllerAttribute(View view, JSONObject elem) throws ScreenConfigException
    {
	    String handlerStr;
        try
        {
	        handlerStr = elem.getString("useController");
        }
        catch (JSONException e)
        {
        	throw new ScreenConfigException(e);
        }
	    if (handlerStr != null)
	    {
	    	String[] handlers = RegexpPatterns.REGEXP_COMMA.split(handlerStr);
	    	for (String handler : handlers)
	    	{
	    		handler = handler.trim();
	    		if (!StringUtils.isEmpty(handler))
	    		{
	    			if (!context.getControllers().hasController(handler))
	    			{
	    				throw new ScreenConfigException("Controller ["+handler+"], declared on view ["+view.getId()+"], not found!");
	    			}
	    			view.addController(handler);
	    		}
	    	}
	    }
    }

	/**
	 * @param view
	 * @param elem
	 * @throws ScreenConfigException 
	 */
	@Deprecated
	@Legacy
	private void parseViewUseDatasourceAttribute(View view, JSONObject elem) throws ScreenConfigException
    {
	    String datasourceStr;
        try
        {
        	datasourceStr = elem.getString("useDataSource");
        }
        catch (JSONException e)
        {
			throw new ScreenConfigException(e);
        }
	    if (datasourceStr != null)
	    {
	    	String[] datasources = RegexpPatterns.REGEXP_COMMA.split(datasourceStr);
	    	for (String datasource : datasources)
	    	{
	    		datasource = datasource.trim();
	    		if (!StringUtils.isEmpty(datasource))
	    		{
	    			if (!context.getDataSources().hasDataSource(datasource))
	    			{
	    				throw new ScreenConfigException("Datasource ["+datasource+"], declared on view ["+view.getId()+"], not found!");
	    			}
	    			view.addDataSource(datasource);
	    		}
	    	}
	    }
    }

	/**
	 * @param view
	 * @param elem
	 * @throws ScreenConfigException 
	 */
	@Deprecated
	@Legacy
	private void parseViewUseFormatterAttribute(View view, JSONObject elem) throws ScreenConfigException
    {
	    String formatterStr;
        try
        {
        	formatterStr = elem.getString("useFormatter");
        }
        catch (JSONException e)
        {
			throw new ScreenConfigException(e);
        }
	    if (formatterStr != null)
	    {
	    	String[] formatters = RegexpPatterns.REGEXP_COMMA.split(formatterStr);
	    	for (String formatter : formatters)
	    	{
	    		formatter = formatter.trim();
	    		if (!StringUtils.isEmpty(formatter))
	    		{
	    			if (context.getFormatters().getFormatter(formatter) == null)
	    			{
	    				throw new ScreenConfigException("Formatter ["+formatter+"], declared on view ["+view.getId()+"], not found!");
	    			}
	    			view.addFormatter(formatter);
	    		}
	    	}
	    }
    }
	
	/**
	 * @param view
	 * @param elem
	 * @throws ScreenConfigException 
	 */
	private void parseViewUseResourceAttribute(View view, JSONObject elem) throws ScreenConfigException
    {
	    String handlerStr;
        try
        {
	        handlerStr = elem.getString("useResource");
        }
        catch (JSONException e)
        {
        	throw new ScreenConfigException(e);
        }
	    if (handlerStr != null)
	    {
	    	String[] handlers = RegexpPatterns.REGEXP_COMMA.split(handlerStr);
	    	for (String res : handlers)
	    	{
	    		res = res.trim();
	    		if (!StringUtils.isEmpty(res))
	    		{
	    			if (!context.getResources().hasResource(res))
	    			{
	    				throw new ScreenConfigException("Resource ["+res+"], declared on view ["+view.getId()+"], not found!");
	    			}
	    			view.addResource(res);
	    		}
	    	}
	    }
    }

	/**
	 * 
	 * @param view
	 * @param elem
	 * @throws ScreenConfigException
	 */
	private void parseViewUseViewAttribute(View view, JSONObject elem) throws ScreenConfigException
    {
	    String handlerStr;
        try
        {
	        handlerStr = elem.getString("useView");
        }
        catch (JSONException e)
        {
        	throw new ScreenConfigException(e);
        }
	    if (handlerStr != null)
	    {
	    	String[] views = RegexpPatterns.REGEXP_COMMA.split(handlerStr);
	    	for (String useView : views)
	    	{
	    		useView = useView.trim();
	    		if (!StringUtils.isEmpty(useView))
	    		{
	    			if (!context.getScreenLoader().getViewLoader().isValidViewLocator(useView))
	    			{
	    				throw new ScreenConfigException("View ["+useView+"], declared on view ["+view.getId()+"], not found!");
	    			}
	    			view.addView(useView);
	    		}
	    	}
	    }
    }

	/**
	 * Test if a target json object represents a widget definition for Crux.
	 * @param cruxObject
	 * @return
	 * @throws JSONException
	 */
	public static boolean isValidWidget(JSONObject cruxObject) throws JSONException
	{
		if (cruxObject.has("_type"))
		{
			String type = cruxObject.getString("_type");
			return (type != null && !"screen".equals(type));
		}
		return false;
	}
}
