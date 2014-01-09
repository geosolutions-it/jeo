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
package org.jeo.mongo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.jeo.data.Cursor;
import org.jeo.data.Query;
import org.jeo.feature.Feature;
import org.jeo.feature.Schema;
import org.jeo.geom.GeomBuilder;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public class MongoTest {

    static DB db;

    @BeforeClass
    public static void connect() {
        Exception error = null;
        try {
            db = new MongoOpts("jeo").connect();
            db.getCollectionNames();
        } catch (Exception e) {
            error = e;
        }

        Assume.assumeNoException(error);
        Assume.assumeNotNull(db);
    }

    protected MongoWorkspace mongo;
    MongoTestData testData;

    @Before
    public void setUpTestData() {
        testData = createTestData();
    }

    protected MongoTestData createTestData() {
        return new MongoTestData();
    }

    @Before
    public void setUp() throws Exception {
        DBCollection states = db.getCollection("states");
        if (states != null) {
            states.remove(new BasicDBObject());
        }

        mongo = new MongoWorkspace(db);
        testData.setUp(states, mongo);
    }

    @After
    public void tearDown() throws Exception {
        mongo.close();
    }

    @Test
    public void testGet() throws Exception {
        MongoDataset states = mongo.get("states");
        assertNotNull(states);

        Schema schema = states.schema();
        assertNotNull(schema);
    }

    @Test
    public void testBounds() throws Exception {
        MongoDataset states = mongo.get("states");
        Envelope bbox = states.bounds();
        assertNotNull(bbox);
    }

    @Test
    public void testCount() throws Exception {
        MongoDataset states = mongo.get("states");
        assertEquals(49, states.count(new Query()));
    }

    @Test
    public void testCountBBOX() throws Exception {
        MongoDataset states = mongo.get("states");
        Envelope bbox = new Envelope(-73.44, -71.51, 42.73, 45.01);

        int match = Iterables.size(filter(states.cursor(new Query()), bbox));
        assertEquals(match, states.count(new Query().bounds(bbox)));
    }

    @Test
    public void testReadAll() throws Exception {
        MongoDataset states = mongo.get("states");
        int count = 0;
        for (Feature f : states.cursor(new Query())) {
            count++;
            assertNotNull(f.geometry());
            assertNotNull(f.get("STATE_NAME"));
        }

        assertEquals(49, count);
    }

    @Test
    public void testReadBBOX() throws Exception {
        MongoDataset states = mongo.get("states");
        Envelope bbox = new Envelope(-73.44, -71.51, 42.73, 45.01);

        Set<String> names = Sets.newHashSet(
            Iterables.transform(filter(states.cursor(new Query()),bbox), 
            new Function<Feature,String>() {
                @Override
                public String apply(Feature input) {
                    return (String) input.get("STATE_NAME");
                }
            }));
        assertFalse(names.isEmpty());

        for (Feature f : states.cursor(new Query())) {
            names.remove(f.get("STATE_NAME"));
        }
        assertTrue(names.isEmpty());
    }

    @Test
    public void testAppend() throws Exception {
        MongoDataset states = mongo.get("states");
        Cursor<Feature> c = states.cursor(new Query().append());

        Geometry g = new GeomBuilder().point(0,0).toPoint().buffer(1);
        
        Feature f = c.next();
        f.put(g);
        //f.put("geometry", g);
        f.put("STATE_NAME", "Nowhere");
        c.write();

        assertEquals(50, states.count(new Query()));

        c = states.cursor(new Query().bounds(g.getEnvelopeInternal()));
        assertTrue(c.hasNext());
        f = c.next();

        assertTrue(g.equals(f.geometry()));
        assertEquals("Nowhere", f.get("STATE_NAME"));
    }

    @Test
    public void testUpdate() throws Exception {
        MongoDataset states = mongo.get("states");
        
        Geometry g = new GeomBuilder().point(0,0).toPoint().buffer(1);
        assertEquals(0, states.count(new Query().bounds(g.getEnvelopeInternal())));
        assertEquals(0, states.count(new Query().filter("STATE_NAME = 'foo'")));

        Cursor<Feature> c = states.cursor(new Query().filter("STATE_ABBR = 'NY'").update());
        assertTrue(c.hasNext());

        Feature f = c.next();
        f.put(g);
        f.put("STATE_NAME", "foo");
        c.write();
        c.close();

        assertEquals(0, states.count(new Query().filter("STATE_NAME = 'New York'")));
        assertEquals(1, states.count(new Query().bounds(g.getEnvelopeInternal())));
        assertEquals(1, states.count(new Query().filter("STATE_NAME = 'foo'")));

        c = states.cursor(new Query().filter("STATE_NAME = 'foo'").update());
        assertTrue(c.hasNext());
        c.remove();
        c.close();

        assertEquals(48, states.count(new Query()));
        assertEquals(0, states.count(new Query().filter("STATE_NAME = 'New York'")));
        assertEquals(0, states.count(new Query().filter("STATE_NAME = 'foo'")));
    }

    Iterable<Feature> filter(Cursor<Feature> features, final Envelope bbox) {
        return Iterables.filter(features, new Predicate<Feature>() {
            @Override
            public boolean apply(Feature input) {
                return input.geometry().getEnvelopeInternal().intersects(bbox);
            }
        }); 
    }

    
}
