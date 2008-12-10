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

package org.openscada.da.project.editor.realtimelist;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;
import org.openscada.core.ConnectionInformation;
import org.openscada.da.client.ItemManager;
import org.openscada.rcp.da.client.dnd.ItemTransfer;
import org.openscada.rcp.da.client.item.Item;

public class ItemDropAdapter extends ViewerDropAdapter
{

    private final RealtimeListEditor editor;

    public ItemDropAdapter ( final RealtimeListEditor editor, final Viewer viewer )
    {
        super ( viewer );
        this.editor = editor;
        setFeedbackEnabled ( true );
        setSelectionFeedbackEnabled ( true );
    }

    @Override
    public boolean performDrop ( final Object data )
    {
        if ( data instanceof Item[] )
        {
            dropItemType ( (Item[])data );
            return true;
        }
        else if ( data instanceof String[] )
        {
            dropStringType ( data );
            return true;
        }

        return false;
    }

    private void dropStringType ( final Object data )
    {
        final String[] urls = (String[])data;
        final List<Item> items = new ArrayList<Item> ();

        for ( final String urlString : urls )
        {
            final Item item = new Item ();

            final String[] toks = urlString.split ( "#", 1 );
            item.setId ( toks[1] );
            item.setConnectionString ( toks[0] );

            items.add ( item );
        }
    }

    private void dropItemType ( final Item[] items )
    {
        final ListData listData = (ListData)getViewer ().getInput ();
        final TreeViewer viewer = (TreeViewer)getViewer ();

        for ( final Item item : items )
        {
            try
            {
                dropItem ( item, listData, viewer );
            }
            catch ( final URISyntaxException e )
            {
                e.printStackTrace ();
            }
        }
    }

    private void dropItem ( final Item item, final ListData listData, final TreeViewer viewer ) throws URISyntaxException
    {
        final ConnectionInformation connectionInformation = ConnectionInformation.fromURI ( item.getConnectionString () );

        // final HiveConnection connection = Activator.getRepository ().findConnection ( connectionInformation );
        final ItemManager itemManager = org.openscada.da.project.Activator.getConnectionManager ().getItemManager ( connectionInformation, true );
        if ( itemManager != null )
        {
            listData.add ( item.getId (), new URI ( item.getConnectionString () ), itemManager );
            this.editor.makeDirty ();
        }
    }

    @Override
    public boolean validateDrop ( final Object target, final int operation, final TransferData transferType )
    {
        return ItemTransfer.getInstance ().isSupportedType ( transferType );
    }

}
