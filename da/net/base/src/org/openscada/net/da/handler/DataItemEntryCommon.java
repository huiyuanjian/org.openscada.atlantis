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

package org.openscada.net.da.handler;

import java.util.EnumSet;
import java.util.Map;

import org.openscada.da.core.IODirection;
import org.openscada.da.core.browser.DataItemEntry;
import org.openscada.da.core.data.Variant;

public class DataItemEntryCommon extends EntryCommon implements DataItemEntry
{
    private String _id = "";
    private EnumSet<IODirection> _directions = EnumSet.noneOf ( IODirection.class );
    
    public DataItemEntryCommon ( String name, EnumSet<IODirection> directions, Map<String, Variant> attributes, String id )
    {
        super ( name, attributes );
        _directions = directions;
        _id = id;
    }
    
    public String getId ()
    {
        return _id;
    }

    public EnumSet<IODirection> getIODirections ()
    {
        return _directions;
    }
}