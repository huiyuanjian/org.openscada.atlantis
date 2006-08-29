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

package org.openscada.da.core.common.chain;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.openscada.core.InvalidOperationException;
import org.openscada.core.NotConvertableException;
import org.openscada.core.NullValueException;
import org.openscada.core.Variant;
import org.openscada.da.core.common.DataItemInformationBase;
import org.openscada.da.core.common.ItemListener;
import org.openscada.da.core.server.DataItemInformation;
import org.openscada.da.core.server.IODirection;

public class DataItemInputChained extends DataItemBaseChained
{
    protected Variant _primaryValue = new Variant ();
    protected Variant _secondaryValue = new Variant ();
    
    public DataItemInputChained ( DataItemInformation di )
    {
        super ( di );
    }
    
    public DataItemInputChained ( String id )
    {
        this ( new DataItemInformationBase ( id, EnumSet.of ( IODirection.INPUT ) ) );
    }
    
    synchronized public void updateValue ( Variant value )
    {
        if ( _primaryValue.equals ( value ) )
            return;
        
        _primaryValue = new Variant ( value );
        process ();
    }
    
    @Override
    protected void process ()
    {
        Variant primaryValue = new Variant ( _primaryValue );
        Map<String, Variant> primaryAttributes = new HashMap<String, Variant> ( _primaryAttributes );
        
        for ( ChainProcessEntry entry : _chain )
        {
            if ( entry.getWhen ().contains ( IODirection.INPUT ) )
                entry.getWhat ().process ( primaryValue, primaryAttributes );
        }
        
        if ( !_secondaryValue.equals ( primaryValue ) )
        {
            _secondaryValue = new Variant ( primaryValue );
            notifyValue ( _secondaryValue );
        }
        _secondaryAttributes.set ( primaryAttributes );
    }

    public Variant getValue () throws InvalidOperationException
    {
        return new Variant ( _secondaryValue );
    }

    public void setValue ( Variant value ) throws InvalidOperationException, NullValueException, NotConvertableException
    {
        throw new InvalidOperationException ();
    }
    
    @Override
    public void setListener ( ItemListener listener )
    {
        super.setListener ( listener );
        if ( listener != null )
        {
            if ( !_secondaryValue.isNull () )
                notifyValue ( _secondaryValue );
            if ( _secondaryAttributes.get ().size () > 0 )
                notifyAttributes ( _secondaryAttributes.get () );
        }
    }
    
}
