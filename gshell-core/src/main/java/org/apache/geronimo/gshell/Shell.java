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

package org.apache.geronimo.gshell;

import java.util.Iterator;

import org.apache.commons.lang.time.StopWatch;
import org.apache.geronimo.gshell.command.Command;
import org.apache.geronimo.gshell.command.CommandContext;
import org.apache.geronimo.gshell.command.CommandDefinition;
import org.apache.geronimo.gshell.command.CommandManager;
import org.apache.geronimo.gshell.command.MessageSource;
import org.apache.geronimo.gshell.command.MessageSourceImpl;
import org.apache.geronimo.gshell.command.StandardVariables;
import org.apache.geronimo.gshell.command.Variables;
import org.apache.geronimo.gshell.command.VariablesImpl;
import org.apache.geronimo.gshell.commandline.CommandLine;
import org.apache.geronimo.gshell.commandline.CommandLineBuilder;
import org.apache.geronimo.gshell.console.IO;
import org.apache.geronimo.gshell.util.Arguments;

import org.codehaus.plexus.MutablePlexusContainer;
import org.codehaus.plexus.component.repository.ComponentDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the primary interface to executing named commands.
 *
 * @version $Rev$ $Date$
 */
// @Component(role = Shell.class)
public class Shell
{
    private Logger log = LoggerFactory.getLogger(getClass());

    // @Requirement
    private IO io;

    // @Requirement
    private MutablePlexusContainer container;

    // @Requirement
    private CommandManager commandManager;

    // @Requirement
    private CommandLineBuilder commandLineBuilder;

    private Variables variables = new VariablesImpl();

    public Shell() {
        //
        // HACK: Set some default variables
        //

        variables.set(StandardVariables.PROMPT, "> ");
    }

    //
    // HACK: This is for testing, need to weed out and refactor all this shiz
    //
    
    public Shell(final IO io) {
        this.io = io;
    }
    
    public Variables getVariables() {
        return variables;
    }

    public IO getIO() {
        return io;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public Object execute(final String commandLine) throws Exception {
        assert commandLine != null;
        
        if (log.isInfoEnabled()) {
            log.info("Executing (String): " + commandLine);
        }

        CommandLine cl = commandLineBuilder.create(commandLine);
        return cl.execute();
    }

    //
    // CommandExecutor
    //

    private void dump(final Variables vars) {
        Iterator<String> iter = vars.names();

        if (iter.hasNext()) {
            log.debug("Variables:");
        }

        while (iter.hasNext()) {
            String name = iter.next();

            log.debug("    " + name + "=" + vars.get(name));
        }
    }

    public Object execute(final String commandName, final Object[] args) throws Exception {
        assert commandName != null;
        assert args != null;

        boolean debug = log.isDebugEnabled();

        if (log.isInfoEnabled()) {
            log.info("Executing (" + commandName + "): " + Arguments.asString(args));
        }

        //
        // HACK: For now we need to make sure we get a mutable container
        //
        MutablePlexusContainer childContainer = (MutablePlexusContainer)
                container.createChildContainer("command", container.getContainerRealm());

        //
        // HACK: Register the command def crapo as a component descriptor in the child container
        //
        
        CommandDefinition def = commandManager.getCommandDefinition(commandName);

        ComponentDescriptor desc = new ComponentDescriptor();
        desc.setRole(Command.class.getName());
        desc.setImplementation(def.getClassName());
        desc.setRoleHint(def.getName());

        childContainer.getComponentRepository().addComponentDescriptor(desc);

        //
        // HACK: Pull out the damn command
        //

        final Command command = (Command)childContainer.lookup(Command.class, def.getName());

        //
        // HACK: Auto-wire setter-based dependencies for now
        //
        
        childContainer.autowire(command);

        //
        // TODO: DI all bits if we can, then free up "context" to replace "category" as a term
        //

        final Variables vars = new VariablesImpl(getVariables());

        command.init(new CommandContext() {
            public IO getIO() {
                return io;
            }

            public Variables getVariables() {
                return vars;
            }

            MessageSource messageSource;

            public MessageSource getMessageSource() {
                // Lazy init the messages, commands many not need them
                if (messageSource == null) {
                    messageSource = new MessageSourceImpl(command.getClass().getName() + "Messages");
                }

                return messageSource;
            }
        });

        // Setup command timings
        StopWatch watch = null;
        if (debug) {
            watch = new StopWatch();
            watch.start();
        }

        Object result;
        try {
            result = command.execute(args);

            if (debug) {
                log.debug("Command completed in " + watch);
            }
        }
        finally {
            command.destroy();

            //
            // HACK: Nuke the child container now
            //
            
            container.removeChildContainer("command");
        }

        return result;
    }

    public Object execute(final Object... args) throws Exception {
        assert args != null;
        assert args.length > 1;

        if (log.isInfoEnabled()) {
            log.info("Executing (Object...): " + Arguments.asString(args));
        }

        return execute(String.valueOf(args[0]), Arguments.shift(args));
    }
}
