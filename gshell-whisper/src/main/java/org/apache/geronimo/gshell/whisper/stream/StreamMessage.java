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

package org.apache.geronimo.gshell.whisper.stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import org.apache.geronimo.gshell.whisper.message.BaseMessage;
import org.apache.geronimo.gshell.whisper.message.Message;
import org.apache.mina.common.ByteBuffer;

/**
 * Write a buffer to a stream.
 *
 * @version $Rev$ $Date$
 */
public class StreamMessage
    extends BaseMessage
{
    private byte[] bytes;

    public StreamMessage(final Type type, final ByteBuffer buffer) throws IOException {
        super(type);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        WritableByteChannel channel = Channels.newChannel(baos);
        channel.write(buffer.buf());
        channel.close();

        bytes = baos.toByteArray();
    }

    public StreamMessage(final ByteBuffer buffer) throws IOException {
        this(StreamMessage.Type.IN, buffer);
    }

    public ByteBuffer getBuffer() {
        return ByteBuffer.wrap(bytes);
    }

    public static enum Type
        implements Message.Type
    {

        IN,  // (local SYSOUT to remote SYSIN)
        OUT, // ???
        ERR  // ???
        ;

        public Class<? extends Message> getType() {
            return StreamMessage.class;
        }
    }
}