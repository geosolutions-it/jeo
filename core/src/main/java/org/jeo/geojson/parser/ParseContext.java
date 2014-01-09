/* Copyright 2013 The jeo project. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jeo.geojson.parser;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class ParseContext {

    Deque<BaseHandler> handlers = new ArrayDeque<BaseHandler>();
    Node current, last;
    boolean strict = false;

    ParseContext() {
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public boolean isStrict() {
        return strict;
    }

    public void push(String name, BaseHandler h) {
        Node n = current != null ? current.newNode(name) : new Node(name, null);
        down(n);
        
        handlers.push(h);
        h.init(this, n);
    }

    public BaseHandler pop() {
        up();
        return handlers.pop();
    }

    public void down(Node n) {
        current = n;
    }

    public void up() {
        last = current;
        current = current.getParent();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("handlers=[");
        for (Iterator<BaseHandler> it =  handlers.descendingIterator(); it.hasNext();) {
            sb.append(it.next().getClass().getSimpleName()).append(", ");
        }
        if (!handlers.isEmpty()) {
            sb.setLength(sb.length()-2);
        }
        sb.append("]");
        return sb.toString();
    }
}

