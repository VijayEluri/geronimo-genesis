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

package org.apache.geronimo.gshell.remote.client;

import java.util.concurrent.atomic.AtomicReference;

import jline.Terminal;
import org.apache.geronimo.gshell.ExitNotification;
import org.apache.geronimo.gshell.ansi.Renderer;
import org.apache.geronimo.gshell.command.IO;
import org.apache.geronimo.gshell.console.Console;
import org.apache.geronimo.gshell.console.JLineConsole;
import org.apache.geronimo.gshell.remote.RemoteShell;
import org.apache.geronimo.gshell.remote.stream.StreamFeeder;
import org.apache.geronimo.gshell.shell.Environment;
import org.apache.geronimo.gshell.shell.InteractiveShell;
import org.apache.geronimo.gshell.shell.ShellInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a shell interface which will proxy to a remote shell instance.
 *
 * @version $Rev$ $Date$
 */
public class RemoteShellProxy
    implements RemoteShell, InteractiveShell
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private RshClient client;

    private IO io;

    private Terminal terminal;

    private StreamFeeder outputFeeder;

    private boolean opened;

    public RemoteShellProxy(final RshClient client, final IO io, final Terminal terminal) throws Exception {
        assert client != null;
        assert io != null;
        assert terminal != null;

        this.client = client;
        this.io = io;
        this.terminal = terminal;

        //
        // TODO: send over some client-side details, like the terminal features, etc, as well, verbosity too)
        //       If any problem or denial occurs, throw an exception, once created the proxy is considered valid.
        //
        
        client.openShell();

        //
        // HACK: Turn this off for the moment to test more...
        //
        
        // Copy the client's input stream to our outputstream so users see command output
        // outputFeeder = new StreamFeeder(client.getInputStream(), io.outputStream);
        // outputFeeder.createThread().start();

        opened = true;
    }

    public Environment getEnvironment() {
        ensureOpened();
        
        throw new Error("TODO");
    }

    public ShellInfo getShellInfo() {
        ensureOpened();

        throw new Error("TODO");
    }

    private void ensureOpened() {
        if (!opened) {
            throw new IllegalStateException("Remote shell proxy has been closed");
        }
    }

    public boolean isOpened() {
        return opened;
    }

    public void close() {
        // outputFeeder.close();

        try {
            client.closeShell();
        }
        catch (Exception e) {
            log.error("Failure occured while closing remote shell; ignoring", e);
        }

        opened = false;
        
        log.info("Closed");
    }

    //
    // Command Execution
    //

    public Object execute(final String line) throws Exception {
        ensureOpened();

        return client.execute(line);
    }

    public Object execute(final Object... args) throws Exception {
        ensureOpened();

        return client.execute((Object[])args);
    }

    public Object execute(final String path, final Object[] args) throws Exception {
        ensureOpened();

        return client.execute(path, args);
    }

    //
    // Interactive Shell
    //

    public void run(final Object... args) throws Exception {
        assert args != null;

        ensureOpened();

        log.debug("Starting interactive console; args: {}", args);

        //
        // FIXME: We need a hook into the session state here so that we can abort the console muck when the session closes
        //
        
        //
        // TODO: Request server to load...
        //
        // loadUserScript(branding.getInteractiveScriptName());

        final AtomicReference<ExitNotification> exitNotifHolder = new AtomicReference<ExitNotification>();
        final AtomicReference<Object> lastResultHolder = new AtomicReference<Object>();

        Console.Executor executor = new Console.Executor() {
            public Result execute(final String line) throws Exception {
                assert line != null;

                try {
                    Object result = RemoteShellProxy.this.execute(line);

                    lastResultHolder.set(result);
                }
                catch (ExitNotification n) {
                    exitNotifHolder.set(n);

                    return Result.STOP;
                }

                return Result.CONTINUE;
            }
        };

        JLineConsole console = new JLineConsole(executor, io, terminal);

        console.setPrompter(new Console.Prompter() {
            Renderer renderer = new Renderer();

            public String prompt() {
                //
                // TODO: Get the real details and ue them...
                //

                String userName = "user"; // shellInfo.getUserName();
                String hostName = "remote"; // shellInfo.getLocalHost().getHostName();
                String path = "/";

                return renderer.render("@|bold " + userName + "|@" + hostName + ":@|bold " + path + "|> ");
            }
        });

        console.setErrorHandler(new Console.ErrorHandler() {
            public Result handleError(final Throwable error) {
                assert error != null;

                //
                // TODO: Do something here...
                //

                return Result.CONTINUE;
            }
        });

        //
        // TODO: What are we to do with history here?  Really should be history on the server...
        //

        /*
        // Hook up a nice history file (we gotta hold on to the history object at some point so the 'history' command can get to it)
        History history = new History();
        console.setHistory(history);
        console.setHistoryFile(new File(branding.getUserDirectory(), branding.getHistoryFileName()));
        */

        // Unless the user wants us to shut up, then display a nice welcome banner
        /*
        if (!io.isQuiet()) {
            io.out.println(branding.getWelcomeBanner());
        }
        */

        // Check if there are args, and run them and then enter interactive
        if (args.length != 0) {
            execute(args);
        }

        // And then spin up the console and go for a jog
        console.run();

        // If any exit notification occured while running, then puke it up
        ExitNotification n = exitNotifHolder.get();
        if (n != null) {
            throw n;
        }
    }
}