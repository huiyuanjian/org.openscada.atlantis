/*
 * This file is part of the OpenSCADA project
 * Copyright (C) 2006 inavare GmbH (http://inavare.com)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.openscada.da.core.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openscada.core.InvalidOperationException;
import org.openscada.core.NotConvertableException;
import org.openscada.core.NullValueException;
import org.openscada.core.Variant;
import org.openscada.da.core.server.WriteAttributesOperationListener.Results;

public class DataItemCommand extends DataItemOutput {

	public DataItemCommand(String name)
    {
		super(name);
	}

	public interface Listener
	{
		public void command ( Variant value );
	}	
	
	private List<Listener> _listeners = new ArrayList<Listener>();
	
	public void setValue(Variant value) throws InvalidOperationException,
	NullValueException, NotConvertableException {
		
		List<Listener> listeners;
		synchronized ( _listeners )
		{
			listeners = new ArrayList<Listener>(_listeners);
		}
		
		for ( Listener listener : listeners )
		{
			try
			{
				listener.command(value);
			}
			catch ( Exception e )
			{
			}
		}
	}
	
	public void addListener ( Listener listener )
	{
		synchronized ( _listeners )
		{
			_listeners.add(listener);
		}
	}
	public void removeListener ( Listener listener )
	{
		synchronized ( _listeners )
		{
			_listeners.remove(listener);
		}
	}
	
	public Map<String, Variant> getAttributes()
    {
		return new HashMap<String,Variant>();
	}
	
	public Results setAttributes ( Map<String, Variant> attributes )
    {
        return WriteAttributesHelper.errorUnhandled ( null, attributes );
	}
	
}
