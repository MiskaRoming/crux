/*
 * Copyright 2009 Sysmap Solutions Software e Consultoria Ltda.
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
package br.com.sysmap.crux.core.client.datasource;

import java.util.Arrays;
import java.util.Comparator;

import br.com.sysmap.crux.core.client.ClientMessages;

import com.google.gwt.core.client.GWT;

/**
 * @author Thiago da Rosa de Bustamante
 *
 */
abstract class AbstractScrollableDataSource<E> implements MeasurableDataSource<E>
{
	protected Metadata metadata;
	protected DataSourceRecord<E>[] data;
	protected int currentRecord = -1;
	protected boolean loaded = false;
	protected ClientMessages messages = GWT.create(ClientMessages.class);

	public Metadata getMetadata()
	{
		return metadata;
	}
	
	public Object getValue(String columnName)
	{
		if (currentRecord > -1)
		{
			DataSourceRecord<E> dataSourceRow = data[currentRecord];
			return getValue(columnName, dataSourceRow);
		}
		return null;
	}
	
	public boolean hasNextRecord()
	{
		ensureLoaded();
		return (data != null && currentRecord < data.length -1);
	}

	public void nextRecord()
	{
		if (hasNextRecord())
		{
			currentRecord++;
		}
	}

	public void reset()
	{
		if(data != null)
		{
			data = null;
		}
		currentRecord = -1;
		loaded = false;
	}
	
	public DataSourceRecord<E> getRecord()
	{
		ensureLoaded();
		if (currentRecord > -1)
		{
			return data[currentRecord];
		}
		else
		{
			return null;
		}
	}
	
	public boolean hasPreviousRecord()
	{
		ensureLoaded();
		return (data != null && currentRecord > 0 && data.length > 0);
	}

	public void previousRecord()
	{
		if (hasPreviousRecord())
		{
			currentRecord--;
		}
	}
	
	public void sort(final String columnName, boolean ascending)
	{
		ensureLoaded();
		if (data != null)
		{
			sortArray(data,columnName, ascending);
		}
	}

	protected void sortArray(DataSourceRecord<E>[] array, final String columnName, final boolean ascending)
	{
		if (!metadata.getColumn(columnName).isSortable())
		{
			throw new DataSoureExcpetion(messages.dataSourceErrorColumnNotComparable(columnName));
		}
		Arrays.sort(array, new Comparator<DataSourceRecord<E>>(){
			public int compare(DataSourceRecord<E> o1, DataSourceRecord<E> o2)
			{
				if (ascending)
				{
					if (o1==null) return (o2==null?0:-1);
					if (o2==null) return 1;
				}
				else
				{
					if (o1==null) return (o2==null?0:1);
					if (o2==null) return -1;
				}
				
				Object value1 = getValue(columnName, o1);
				Object value2 = getValue(columnName, o2);

				if (ascending)
				{
					if (value1==null) return (value2==null?0:-1);
					if (value2==null) return 1;
				}
				else
				{
					if (value1==null) return (value2==null?0:1);
					if (value2==null) return -1;
				}

				return compareNonNullValuesByType(value1,value2,ascending);
			}

			@SuppressWarnings("unchecked")
			private int compareNonNullValuesByType(Object value1, Object value2,boolean ascending)
			{
				if (ascending)
				{
					return ((Comparable)value1).compareTo(value2);
				}
				else
				{
					return ((Comparable)value2).compareTo(value1);
				}
			}
		});
		firstRecord();
	}
	
	public int getRecordCount()
	{
		return (data!=null?data.length:0);
	}
	
	public void firstRecord()
	{
		currentRecord = -1;
		nextRecord();
	}
	
	public void lastRecord()
	{
		ensureLoaded();
		currentRecord = getRecordCount()-1;
	}
	
	protected void ensureLoaded()
	{
		if (!loaded)
		{
			throw new DataSoureExcpetion(messages.dataSourceNotLoaded());
		}
	}
	
	public Object getValue(String columnName, DataSourceRecord<E> dataSourceRecord)
	{
		return null;
	}
	
	/**
	 * @return
	 * @deprecated Use getBoundObject instead
	 */
	@Deprecated
	public E getBindedObject()
	{
		return getBindedObject(getRecord());
	}
	
	/**
	 * @param record
	 * @return
	 * @deprecated Use getBoundObject instead
	 */
	@Deprecated
	public E getBindedObject(DataSourceRecord<E> record)
	{
		return getBoundObject(record);
	}
	
	/**
	 * @see br.com.sysmap.crux.core.client.datasource.DataSource#getBoundObject()
	 */
	public E getBoundObject()
	{
	    return getBoundObject(getRecord());
	}
	
	/**
	 * @see br.com.sysmap.crux.core.client.datasource.DataSource#getBoundObject(br.com.sysmap.crux.core.client.datasource.DataSourceRecord)
	 */
	public E getBoundObject(DataSourceRecord<E> record)
	{
	    return null;
	}
}
