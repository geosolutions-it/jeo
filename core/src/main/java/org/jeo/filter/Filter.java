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
package org.jeo.filter;

/**
 * Predicate that applies a boolean filter for a given input. 
 *   
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class Filter<T> {

    /**
     * Returns a new filter builder.
     */
    public static FilterBuilder build() {
        return new FilterBuilder();
    }

    /**
     * Applies the filter to the specified input.
     * 
     * @param obj The input.
     * 
     * @return The result, <code>true</code> if the filter matches the specific input, otherwise
     *   <code>false</code>.
     */
    public abstract boolean apply(T obj);

    /**
     * Creates a new filter that is a logical AND of this filter and the specified filter.
     */
    public Filter<T> and(Filter<T> other) {
        if (other instanceof All) {
            return this;
        }
        if (other instanceof None) {
            return other;
        }

        return new Logic<T>(Logic.Type.AND, this, other);
    }

    /**
     * Creates a new filter that is a logical OR of this filter and the specified filter.
     */
    public Filter<T> or(Filter<T> other) {
        if (other instanceof All) {
            return other;
        }
        if (other instanceof None) {
            return this;
        }
        return new Logic<T>(Logic.Type.OR, this, other);
    }

    /**
     * Creates a new filter that is the negation of this filter.
     */
    public Filter<T> not() {
        return new Logic<T>(Logic.Type.NOT, this); 
    }

    /**
     * Applies a visitor to the filter.
     */
    public Object accept(FilterVisitor v, Object obj) {
        return v.visit(this, obj);
    }

}
