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

package org.apache.geronimo.gshell.layout.model;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * A command instance.
 *
 * @version $Rev$ $Date$
 */
@XStreamAlias("command")
public class Command
    extends Node
{
    //
    // FIXME: This isn't really goning to be the implementation, but its a key to reference a command which has been discovered previously
    //        So, rename this shizzz
    //
    
    protected String implementation;

    public Command(final String name, final String implementation) {
        super(name);

        assert implementation != null;

        this.implementation = implementation;
    }

    public String getImplementation() {
        return implementation;
    }
}