/*
 * This file is part of the OpenSCADA project
 * Copyright (C) 2006-2011 TH4 SYSTEMS GmbH (http://th4-systems.com)
 *
 * OpenSCADA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 3
 * only, as published by the Free Software Foundation.
 *
 * OpenSCADA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License version 3 for more details
 * (a copy is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with OpenSCADA. If not, see
 * <http://opensource.org/licenses/lgpl-3.0.html> for a copy of the LGPLv3 License.
 */

package org.openscada.ae.server.storage.jdbc;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.openscada.ae.Event;
import org.openscada.ae.Event.EventBuilder;
import org.openscada.ae.Event.Fields;
import org.openscada.core.Variant;
import org.openscada.core.VariantEditor;
import org.openscada.core.VariantType;
import org.openscada.utils.filter.Filter;
import org.openscada.utils.str.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcStorageDao implements StorageDao
{
    private static final Logger logger = LoggerFactory.getLogger ( JdbcStorageDao.class );

    private Connection connection;

    private String instance = "default";

    private String schema = "";

    private int maxLength = 4000;

    private final String insertEventSql = "INSERT INTO %sOPENSCADA_AE_EVENTS " //
            + "(ID, INSTANCE_ID, SOURCE_TIMESTAMP, ENTRY_TIMESTAMP, MONITOR_TYPE, EVENT_TYPE, " //
            + "VALUE_TYPE, VALUE_STRING, VALUE_INTEGER, VALUE_DOUBLE, MESSAGE, " //
            + "MESSAGE_CODE, PRIORITY, SOURCE, ACTOR_NAME, ACTOR_TYPE)" //
            + " VALUES " //
            + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final String insertAttributesSql = "INSERT INTO %sOPENSCADA_AE_EVENTS_ATTR " //
            + "(ID, KEY, VALUE_TYPE, VALUE_STRING, VALUE_INTEGER, VALUE_DOUBLE)" //
            + " VALUES " //
            + "(?, ?, ?, ?, ?, ?)";

    private final String deleteAttributesSql = "DELETE FROM %sOPENSCADA_AE_EVENTS_ATTR " //
            + "WHERE ID = ? AND KEY = ?";

    private final String selectEventSql = "SELECT E.ID, E.INSTANCE_ID, E.SOURCE_TIMESTAMP, E.ENTRY_TIMESTAMP, E.MONITOR_TYPE, E.EVENT_TYPE, " //
            + "E.VALUE_TYPE, E.VALUE_STRING, E.VALUE_INTEGER, E.VALUE_DOUBLE, E.MESSAGE, " //
            + "E.MESSAGE_CODE, E.PRIORITY, E.SOURCE, E.ACTOR_NAME, E.ACTOR_TYPE, " //
            + "A.KEY, A.VALUE_TYPE, A.VALUE_STRING, A.VALUE_INTEGER, A.VALUE_DOUBLE " //
            + "FROM %1$sOPENSCADA_AE_EVENTS E LEFT JOIN %1$sOPENSCADA_AE_EVENTS_ATTR A ON (A.ID = E.ID) ";

    private final String whereSql = " WHERE E.INSTANCE_ID = ? ";

    private final String defaultOrder = " ORDER BY E.SOURCE_TIMESTAMP DESC, E.ENTRY_TIMESTAMP DESC";

    /* (non-Javadoc)
     * @see org.openscada.ae.server.storage.jdbc.StorageDao#storeEvent(org.openscada.ae.Event)
     */
    @Override
    public void storeEvent ( final Event event ) throws Exception
    {
        final Connection con = this.connection;
        Statement stm1 = null;
        Statement stm2 = null;
        {
            final PreparedStatement stm = con.prepareStatement ( String.format ( this.insertEventSql, this.schema ) );
            stm.setString ( 1, event.getId ().toString () );
            stm.setString ( 2, this.instance );
            stm.setTimestamp ( 3, new java.sql.Timestamp ( event.getSourceTimestamp ().getTime () ) );
            stm.setTimestamp ( 4, new java.sql.Timestamp ( event.getEntryTimestamp ().getTime () ) );
            stm.setString ( 5, clip ( 32, Variant.valueOf ( event.getField ( Fields.MONITOR_TYPE ) ).asString ( "" ) ) );
            stm.setString ( 6, clip ( 32, Variant.valueOf ( event.getField ( Fields.EVENT_TYPE ) ).asString ( "" ) ) );
            stm.setString ( 7, clip ( 32, Variant.valueOf ( event.getField ( Fields.VALUE ) ).getType ().name () ) );
            stm.setString ( 8, clip ( this.maxLength, Variant.valueOf ( event.getField ( Fields.VALUE ) ).asString ( "" ) ) );
            final Long longValue = Variant.valueOf ( event.getField ( Fields.VALUE ) ).asLong ( null );
            if ( longValue == null )
            {
                stm.setNull ( 9, Types.BIGINT );
            }
            else
            {
                stm.setLong ( 9, longValue );
            }
            final Double doubleValue = Variant.valueOf ( event.getField ( Fields.VALUE ) ).asDouble ( null );
            if ( doubleValue == null )
            {
                stm.setNull ( 10, Types.DOUBLE );
            }
            else
            {
                stm.setDouble ( 10, longValue );
            }
            stm.setString ( 11, clip ( this.maxLength, Variant.valueOf ( event.getField ( Fields.MESSAGE ) ).asString ( "" ) ) );
            stm.setString ( 12, clip ( 255, Variant.valueOf ( event.getField ( Fields.MESSAGE_CODE ) ).asString ( "" ) ) );
            stm.setInt ( 13, Variant.valueOf ( event.getField ( Fields.PRIORITY ) ).asInteger ( 50 ) );
            stm.setString ( 14, clip ( 255, Variant.valueOf ( event.getField ( Fields.SOURCE ) ).asString ( "" ) ) );
            stm.setString ( 15, clip ( 128, Variant.valueOf ( event.getField ( Fields.ACTOR_NAME ) ).asString ( "" ) ) );
            stm.setString ( 16, clip ( 32, Variant.valueOf ( event.getField ( Fields.ACTOR_TYPE ) ).asString ( "" ) ) );
            stm.addBatch ();
            stm.executeBatch ();
            stm1 = stm;
        }
        {
            final PreparedStatement stm = con.prepareStatement ( String.format ( this.insertAttributesSql, this.schema ) );
            boolean hasAttr = false;
            for ( final String attr : event.getAttributes ().keySet () )
            {
                if ( SqlConverter.inlinedAttributes.contains ( attr ) )
                {
                    continue;
                }
                stm.setString ( 1, event.getId ().toString () );
                stm.setString ( 2, attr );
                stm.setString ( 3, clip ( 32, event.getAttributes ().get ( attr ).getType ().name () ) );
                stm.setString ( 4, clip ( this.maxLength, event.getAttributes ().get ( attr ).asString ( "" ) ) );
                final Long longValue = Variant.valueOf ( event.getAttributes ().get ( attr ) ).asLong ( null );
                if ( longValue == null )
                {
                    stm.setNull ( 5, Types.BIGINT );
                }
                else
                {
                    stm.setLong ( 5, longValue );
                }
                final Double doubleValue = Variant.valueOf ( event.getAttributes ().get ( attr ) ).asDouble ( null );
                if ( doubleValue == null )
                {
                    stm.setNull ( 6, Types.DOUBLE );
                }
                else
                {
                    stm.setDouble ( 6, doubleValue );
                }
                stm.addBatch ();
                hasAttr = true;
            }
            if ( hasAttr )
            {
                stm.executeBatch ();
            }
            stm2 = stm;
        }
        con.commit ();
        stm1.close ();
        stm2.close ();
    }

    /* (non-Javadoc)
     * @see org.openscada.ae.server.storage.jdbc.StorageDao#updateComment(java.util.UUID, java.lang.String)
     */
    @Override
    public void updateComment ( final UUID id, final String comment ) throws Exception
    {
        final Connection con = this.connection;
        con.setAutoCommit ( false );
        {
            final PreparedStatement stm = con.prepareStatement ( String.format ( this.deleteAttributesSql, this.schema ) );
            stm.setString ( 1, id.toString () );
            stm.setString ( 2, Event.Fields.COMMENT.getName () );
            stm.addBatch ();
        }
        {
            final PreparedStatement stm = con.prepareStatement ( String.format ( this.insertAttributesSql, this.schema ) );
            stm.setString ( 1, id.toString () );
            stm.setString ( 2, Event.Fields.COMMENT.getName () );
            stm.setString ( 3, VariantType.STRING.name () );
            stm.setString ( 4, clip ( this.maxLength, comment ) );
            stm.setLong ( 5, (Long)null );
            stm.setDouble ( 6, (Double)null );
            stm.addBatch ();
        }
        con.commit ();
    }

    private String clip ( final int i, final String string )
    {
        if ( string == null )
        {
            return null;
        }
        if ( i < 1 || string.length () <= i )
        {
            return string;
        }
        return string.substring ( 0, i );
    }

    /* (non-Javadoc)
     * @see org.openscada.ae.server.storage.jdbc.StorageDao#loadEvent(java.util.UUID)
     */
    @Override
    public Event loadEvent ( final UUID id ) throws SQLException
    {
        final Connection con = this.connection;
        final String sql = this.selectEventSql + this.whereSql + " AND E.ID = ? " + this.defaultOrder;
        final PreparedStatement stm = con.prepareStatement ( String.format ( sql, this.schema ), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY );
        stm.setString ( 1, this.instance );
        stm.setString ( 2, id.toString () );
        final ResultSet result = stm.executeQuery ();
        final List<Event> events = new ArrayList<Event> ();
        final boolean hasMore = toEventList ( result, events, true, 1 );
        if ( hasMore )
        {
            logger.warn ( "more distinct records found for id {}, this shouldn't happen at all", id );
        }
        if ( events != null && !events.isEmpty () )
        {
            return events.get ( 0 );
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.openscada.ae.server.storage.jdbc.StorageDao#queryEvents(org.openscada.utils.filter.Filter)
     */
    @Override
    public ResultSet queryEvents ( final Filter filter ) throws SQLException, NotSupportedException
    {
        final Connection con = this.connection;
        final SqlCondition condition = SqlConverter.toSql ( this.schema, filter );
        String sql = this.selectEventSql + StringHelper.join ( condition.joins, " " ) + this.whereSql;
        sql += condition.condition;
        sql += this.defaultOrder;
        final String querySql = String.format ( sql, this.schema );
        logger.debug ( "executing query: " + querySql + " with parameters " + condition.joinParameters + " / " + condition.parameters );
        final PreparedStatement stm = con.prepareStatement ( querySql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY );
        int i = 0;
        for ( final String parameter : condition.joinParameters )
        {
            i += 1;
            stm.setString ( i, parameter );
        }
        i += 1;
        stm.setString ( i, this.instance );
        for ( final Serializable parameter : condition.parameters )
        {
            i += 1;
            stm.setObject ( i, parameter );
        }
        final ResultSet rs = stm.executeQuery ();
        logger.debug ( "query completed, returning resultset" );
        return rs;
    }

    /* (non-Javadoc)
     * @see org.openscada.ae.server.storage.jdbc.StorageDao#toEventList(java.sql.ResultSet, java.util.Collection, boolean, long)
     */
    @Override
    public boolean toEventList ( final ResultSet rs, final Collection<Event> events, final boolean isBeforeFirst, final long count ) throws SQLException
    {
        UUID lastId = null;
        EventBuilder eb = Event.create ();
        boolean hasMore = true;
        long l = 0;
        while ( true )
        {
            if ( isBeforeFirst )
            {
                hasMore = rs.next ();
                if ( !hasMore )
                {
                    break;
                }
            }
            final UUID id = UUID.fromString ( rs.getString ( 1 ) );
            if ( lastId != null && !id.equals ( lastId ) )
            {
                events.add ( eb.build () );
                l += 1;
                if ( l == count )
                {
                    break;
                }
                lastId = id;
                eb = Event.create ();
            }
            else if ( lastId == null )
            {
                lastId = id;
            }
            // base event
            eb.id ( id );
            final Date sourceTimestamp = new Date ( rs.getTimestamp ( 3 ).getTime () );
            final Date entryTimestamp = new Date ( rs.getTimestamp ( 4 ).getTime () );
            final String monitorType = rs.getString ( 5 );
            final String eventType = rs.getString ( 6 );
            String valueType = rs.getString ( 7 );
            String valueString = rs.getString ( 8 );
            final String message = rs.getString ( 11 );
            final String messageCode = rs.getString ( 12 );
            final Integer priority = rs.getInt ( 13 );
            final String source = rs.getString ( 14 );
            final String actor = rs.getString ( 15 );
            final String actorType = rs.getString ( 16 );

            eb.sourceTimestamp ( sourceTimestamp );
            eb.entryTimestamp ( entryTimestamp );
            eb.attribute ( Fields.MONITOR_TYPE, monitorType );
            eb.attribute ( Fields.EVENT_TYPE, eventType );
            if ( valueType != null && valueString != null )
            {
                final VariantEditor ed = new VariantEditor ();
                ed.setAsText ( valueType + "#" + valueString );
                eb.attribute ( Fields.VALUE, ed.getValue () );
            }
            eb.attribute ( Fields.MESSAGE, message );
            eb.attribute ( Fields.MESSAGE_CODE, messageCode );
            eb.attribute ( Fields.PRIORITY, priority );
            eb.attribute ( Fields.SOURCE, source );
            eb.attribute ( Fields.ACTOR_NAME, actor );
            eb.attribute ( Fields.ACTOR_TYPE, actorType );

            // other attributes
            final String field = rs.getString ( 17 );
            valueType = rs.getString ( 18 );
            valueString = rs.getString ( 19 );
            if ( field != null )
            {
                if ( valueType != null && valueString != null )
                {
                    final VariantEditor ed = new VariantEditor ();
                    ed.setAsText ( valueType + "#" + valueString );
                    eb.attribute ( field, ed.getValue () );
                }
                else
                {
                    eb.attribute ( field, Variant.NULL );
                }
            }

            hasMore = rs.next ();
            if ( !hasMore )
            {
                events.add ( eb.build () );
                break;
            }
        }
        return hasMore;
    }

    public void setConnection ( final Connection connection )
    {
        this.connection = connection;
    }

    public void setSchema ( final String schema )
    {
        this.schema = schema;
    }

    public void setMaxLength ( final int maxLength )
    {
        this.maxLength = maxLength;
    }

    public void setInstance ( final String instance )
    {
        this.instance = instance;
    }
}
