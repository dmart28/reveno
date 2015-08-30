/**
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.reveno.atp.core.api.channel;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Not used in core processing as such approach is very inefficient.
 * Currently, it's usage is limited only for zero-copy with ObjectOutputStream.
 */
public class RevenoBufferOutputStream extends OutputStream {

    protected final Buffer buffer ;

    @Override
    public void write(int b) throws IOException {
        buffer.writeByte((byte) (b & 0xff));
    }

    public RevenoBufferOutputStream(Buffer buffer) {
        this.buffer = buffer;
    }

}
