/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.geronimo.gshell.remote.client.proxy;

import java.io.File;
import java.net.InetAddress;

import org.apache.geronimo.gshell.remote.client.RshClient;
import org.apache.geronimo.gshell.shell.ShellInfo;

/**
 * ???
 *
 * @version $Rev$ $Date$
 */
public class RemoteShellInfoProxy
    implements ShellInfo
{
    private final RshClient client;

    public RemoteShellInfoProxy(final RshClient client) {
        assert client != null;

        this.client = client;
    }

    public File getHomeDir() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public InetAddress getLocalHost() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getUserName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}