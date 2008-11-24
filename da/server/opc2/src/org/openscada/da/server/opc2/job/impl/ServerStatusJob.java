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

package org.openscada.da.server.opc2.job.impl;

import org.apache.log4j.Logger;
import org.openscada.da.server.opc2.connection.OPCModel;
import org.openscada.da.server.opc2.job.JobResult;
import org.openscada.da.server.opc2.job.ThreadJob;
import org.openscada.opc.dcom.da.OPCSERVERSTATUS;

/**
 * This job queries the server status
 * @author Jens Reimann &lt;jens.reimann@inavare.net&gt;
 *
 */
public class ServerStatusJob extends ThreadJob implements JobResult<OPCSERVERSTATUS>
{
    public static final long DEFAULT_TIMEOUT = 5000L;

    private static Logger log = Logger.getLogger ( ServerStatusJob.class );

    private final OPCModel model;

    private OPCSERVERSTATUS status;

    public ServerStatusJob ( final long timeout, final OPCModel model )
    {
        super ( timeout );
        this.model = model;
    }

    @Override
    protected void perform () throws Exception
    {
        log.debug ( "Request server status" );
        this.status = this.model.getServer ().getStatus ();
    }

    public OPCSERVERSTATUS getStatus ()
    {
        return this.status;
    }

    public OPCSERVERSTATUS getResult ()
    {
        return getStatus ();
    }

}
