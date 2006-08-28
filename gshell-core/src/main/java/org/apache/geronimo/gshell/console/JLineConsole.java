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

package org.apache.geronimo.gshell.console;

import jline.ConsoleReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.NullArgumentException;

import java.io.IOException;

/**
 * A console backed up by <a href="http://jline.sf.net">JLine</a>.
 *
 * @version $Rev$ $Date$
 */
public class JLineConsole
    implements Console
{
    private static final Log log = LogFactory.getLog(SimpleConsole.class);

    private final IO io;

    private final ConsoleReader reader;

    public JLineConsole(final IO io, final ConsoleReader reader) throws IOException {
        if (io == null) {
            throw new NullArgumentException("io");
        }
        if (reader == null) {
            throw new NullArgumentException("reader");
        }

        this.io = io;
        this.reader = reader;
    }

    public JLineConsole(final IO io) throws IOException {
        if (io == null) {
            throw new NullArgumentException("io");
        }

        this.io = io;
        this.reader = new ConsoleReader(io.inputStream, io.out);
    }

    public String readLine(final String prompt) throws IOException {
        assert prompt != null;

        return reader.readLine(prompt);
    }

    public IO getIO() {
        return io;
    }

    public ConsoleReader getReader() {
        return reader;
    }
}