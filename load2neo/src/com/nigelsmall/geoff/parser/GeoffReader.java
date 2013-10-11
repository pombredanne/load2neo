/*
 * Copyright 2013, Nigel Small
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nigelsmall.geoff.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class GeoffReader extends BufferedReader {

    private Character undo;

    public GeoffReader(Reader reader) {
        super(reader);
        this.undo = null;
    }

    public Character readChar() throws IOException {
        if (this.undo == null) {
            int ch = this.read();
            if (ch == -1)
                return null;
            else
                return (char)ch;
        } else {
            char ch = this.undo;
            this.undo = null;
            return ch;
        }
    }

    public Character peekChar() throws IOException {
        if (this.undo == null) {
            int ch = this.read();
            if (ch != -1)
                this.undo = (char)ch;
        }
        return this.undo;
    }

    public String readWhitespace() throws IOException {
        StringBuilder builder = new StringBuilder();
        while (this.peekChar() != null) {
            builder.append(this.readChar());
        }
        return builder.toString();
    }

}
