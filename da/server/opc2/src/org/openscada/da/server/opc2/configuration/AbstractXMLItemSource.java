/*
 * This file is part of the OpenSCADA project
 * Copyright (C) 2006-2008 inavare GmbH (http://inavare.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.openscada.da.server.opc2.configuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openscada.core.Variant;
import org.openscada.da.opc.configuration.InitialItemType;
import org.openscada.da.opc.configuration.InitialItemsType;
import org.openscada.da.server.common.AttributeMode;
import org.openscada.da.server.common.DataItemCommand;
import org.openscada.da.server.common.DataItemCommand.Listener;
import org.openscada.da.server.common.chain.DataItemInputChained;
import org.openscada.da.server.common.item.factory.FolderItemFactory;

public abstract class AbstractXMLItemSource extends AbstractItemSource
{
    private static Logger logger = Logger.getLogger ( AbstractXMLItemSource.class );

    private boolean active = false;

    private final FolderItemFactory parentItemFactory;

    protected FolderItemFactory itemFactory;

    private final String baseId;

    private DataItemCommand reloadCommandItem;

    private DataItemInputChained stateItem;

    private Listener reloadListener;

    public AbstractXMLItemSource ( final FolderItemFactory parentItemFactory, final String baseId )
    {
        super ();
        this.parentItemFactory = parentItemFactory;
        this.baseId = baseId;
    }

    @Override
    public void activate ()
    {
        this.itemFactory = this.parentItemFactory.createSubFolderFactory ( this.baseId );

        this.reloadCommandItem = this.itemFactory.createCommand ( "reload" );
        this.stateItem = this.itemFactory.createInput ( "state" );

        this.reloadCommandItem.addListener ( this.reloadListener = new DataItemCommand.Listener () {

            public void command ( final Variant value )
            {
                AbstractXMLItemSource.this.reload ();
            }
        } );

        setSuccessState ( "IDLE" );

        this.active = true;
        reload ();
    }

    @Override
    public void deactivate ()
    {
        super.deactivate ();

        this.active = false;

        this.itemFactory.dispose ();
        this.itemFactory = null;

        this.reloadCommandItem.removeListener ( this.reloadListener );
        this.reloadCommandItem = null;
        this.stateItem = null;
    }

    protected void reload ()
    {
        if ( !this.active )
        {
            return;
        }

        try
        {
            setSuccessState ( "READ" );
            final InitialItemsType initialItems = parse ();
            setSuccessState ( "NOTIFY" );
            handleItems ( initialItems );
            setSuccessState ( "IDLE" );
        }
        catch ( final Throwable e )
        {
            handleError ( e );
        }
    }

    protected abstract InitialItemsType parse () throws Exception;

    private void handleItems ( final InitialItemsType initialItems )
    {
        final Set<ItemDescription> items = new HashSet<ItemDescription> ();

        logger.debug ( "Number of items: " + initialItems.getItemList ().size () );

        for ( final InitialItemType item : initialItems.getItemList () )
        {
            logger.debug ( "Found new item: " + item.getId () );

            final ItemDescription newItem = new ItemDescription ();
            newItem.setId ( item.getId () );
            newItem.setDescription ( item.getDescription () );
            newItem.setAccessPath ( item.getAccessPath () );

            if ( newItem.getId () != null )
            {
                items.add ( newItem );
            }
        }

        fireAvailableItemsChanged ( items );
    }

    private void handleError ( final Throwable e )
    {
        final Map<String, Variant> attributes = new HashMap<String, Variant> ();
        attributes.put ( "error", new Variant ( true ) );
        attributes.put ( "error.message", new Variant ( e.getMessage () ) );

        this.stateItem.updateData ( new Variant ( "ERROR" ), attributes, AttributeMode.UPDATE );
    }

    private void setSuccessState ( final String state )
    {
        final Map<String, Variant> attributes = new HashMap<String, Variant> ();
        attributes.put ( "error", null );
        attributes.put ( "error.message", null );

        this.stateItem.updateData ( new Variant ( state ), attributes, AttributeMode.UPDATE );
    }

}
