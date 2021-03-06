package org.jeo.mongo;

import java.io.IOException;

import org.jeo.TestData;
import org.jeo.data.Query;
import org.jeo.data.VectorData;
import org.jeo.feature.Feature;
import org.jeo.geom.Geom;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

public class NestedTestData extends MongoTestData {

    @Override
    public void setUp(DBCollection dbcol, MongoWorkspace workspace) throws IOException {
        VectorData data = TestData.states();
        
        for (Feature f : data.cursor(new Query())) {
            Geometry g = f.geometry();
            g = Geom.iterate((MultiPolygon) f.geometry()).iterator().next();

            DBObject obj = new BasicDBObject();

            DBObject geo = new BasicDBObject();
            geo.put("shape", GeoJSON.toObject(g));
            geo.put("center", GeoJSON.toObject(g.getCentroid()));
            obj.put("geo", geo);
            
            obj.put("STATE_NAME", f.get("STATE_NAME"));
            obj.put("STATE_ABBR", f.get("STATE_ABBR"));

            DBObject pop = new BasicDBObject();
            pop.put("total", f.get("SAMP_POP"));
            pop.put("male", f.get("P_MALE"));
            pop.put("female", f.get("P_FEMALE"));
            obj.put("pop", pop);

            dbcol.insert(obj);
        }

        dbcol.ensureIndex(BasicDBObjectBuilder.start().add("geo.shape", "2dsphere").get());

        Mapping mapping = new Mapping().geometry("geo.shape").geometry("geo.center");
        workspace.setMapper(new DefaultMapper(mapping));
    }
}
