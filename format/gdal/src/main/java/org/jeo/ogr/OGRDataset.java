package org.jeo.ogr;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import org.gdal.ogr.DataSource;
import org.gdal.ogr.FeatureDefn;
import org.gdal.ogr.FieldDefn;
import org.gdal.ogr.Layer;
import org.gdal.osr.SpatialReference;
import org.jeo.data.Cursor;
import org.jeo.data.Cursors;
import org.jeo.data.Driver;
import org.jeo.data.FileData;
import org.jeo.data.Query;
import org.jeo.data.QueryPlan;
import org.jeo.data.VectorDataset;
import org.jeo.feature.Feature;
import org.jeo.feature.Schema;
import org.jeo.feature.SchemaBuilder;
import org.jeo.geom.Envelopes;
import org.jeo.proj.Proj;
import org.jeo.util.Key;
import org.jeo.util.Pair;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import static org.gdal.ogr.ogrConstants.*;

public class OGRDataset implements VectorDataset, FileData {

    public static final Logger LOG = LoggerFactory.getLogger(OGR.class);

    String name;
    OGRWorkspace workspace;

    public OGRDataset(String name, OGRWorkspace workspace) {
        this.name = name;
        this.workspace = workspace;
    }

    @Override
    public File getFile() {
        return workspace.getFile();
    }

    @Override
    public Driver<?> getDriver() {
        return workspace.getDriver();
    }

    @Override
    public Map<Key<?>, Object> getDriverOptions() {
        return workspace.getDriverOptions();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTitle() {
        return null;
    }
    
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Schema schema() throws IOException {
        Pair<Layer,DataSource> data = open();
        try {
            return toSchema(data.first());
            
        }
        finally {
            close(data);
        }
    }

    Schema toSchema(Layer l) {

        FeatureDefn defn = l.GetLayerDefn();

        SchemaBuilder sb = Schema.build(defn.GetName());

        Class<? extends Geometry> geotype = toGeometryType(defn);
        if (geotype != null) {
            CoordinateReferenceSystem crs = toCRS(l);
            sb.field("geometry", geotype, crs);
        }

        for (int i = 0; i < defn.GetFieldCount(); i++) {
            FieldDefn fd = defn.GetFieldDefn(i);
            sb.field(fd.GetName(), toType(fd));
        }

        return sb.schema();
    }

    CoordinateReferenceSystem toCRS(Layer l) {
        SpatialReference sref = l.GetSpatialRef();
        if (sref != null) {
            return Proj.crs(sref.ExportToProj4());
        }
        return null;
    }

    Class<? extends Geometry> toGeometryType(FeatureDefn defn) {
        int g = defn.GetGeomType();

        if (g == wkbNone) {
            return null;
        }
        if (g == wkbPoint || g == wkbPoint25D) {
            return Point.class;
        } 
        if (g == wkbLinearRing) {
            return LinearRing.class;
        } 
        if (g == wkbLineString || g == wkbLineString25D
            || g == wkbMultiLineString || g == wkbMultiLineString25D) {
            return MultiLineString.class;
        } 
        if (g == wkbPolygon || g == wkbPolygon25D
                || g == wkbMultiPolygon || g == wkbMultiPolygon25D) {
            return MultiPolygon.class;
        } 
        if (g == wkbGeometryCollection || g == wkbGeometryCollection25D) {
            return GeometryCollection.class;
        }
        if (g == wkbUnknown) {
            return Geometry.class;
        }

        LOG.debug("unknown ogr geometry type: " + g);
        return null;
    }

    Class<?> toType(FieldDefn defn) {
        int type = defn.GetFieldType();
        int width = defn.GetWidth();

        if (type == OFTInteger) {
            if (width <= 3) {
                return Byte.class;
            } else if (width <= 5) {
                return Short.class;
            } else if (width <= 9) {
                return Integer.class;
            } else if (width <= 19) {
                return Long.class;
            } else {
                return BigDecimal.class;
            }
        }
        
        if (type == OFTIntegerList) {
            return int[].class;
        }

        if (type == OFTReal) {
            if (width <= 12) {
                return Float.class;
            } else if (width <= 22) {
                return Double.class;
            } else {
                return BigDecimal.class;
            }
        }

        if (type == OFTRealList) {
            return double[].class;
        }

        if (type == OFTBinary) {
            return byte[].class;
        }

        if (type == OFTDate) {
            return java.sql.Date.class;
        } 

        if (type == OFTTime) {
            return java.sql.Time.class;
        }

        if (type == OFTDateTime) {
            return java.sql.Timestamp.class;
        }

        if (type == OFTString) {
            return String.class;
        }

        if (type == OFTStringList) {
            return String[].class;
        }

        LOG.debug("unknown field type:" + type);
        return String.class;
    }

    @Override
    public CoordinateReferenceSystem crs() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public Envelope bounds() throws IOException {
        Pair<Layer,DataSource> data = open();
        try {
            double[] d = data.first().GetExtent(true);
            return new Envelope(d[0], d[1], d[2], d[3]);
        }
        catch(Exception e) {
            throw new IOException("Error calculating bounds", e);
        }
        finally {
            close(data);
        }
    }

    @Override
    public long count(Query q) throws IOException {

        Pair<Layer,DataSource> data = open();
        try {
            Layer l = data.first();

            if (!Envelopes.isNull(q.getBounds())) {
                Envelope bb = q.getBounds();
                l.SetSpatialFilterRect(bb.getMinX(), bb.getMinY(), bb.getMaxX(), bb.getMaxY());
            }

            if (!q.isFiltered()) {
                return q.adjustCount(l.GetFeatureCount());
            }

            //TODO: convert attribute filter to ogr sql
            return Cursors.size(cursor(q));

        }
        catch(Exception e) {
            throw new IOException("Error calculating count", e);
        }
        finally {
            close(data);
        }
    }

    @Override
    public Cursor<Feature> cursor(Query q) throws IOException {
        Pair<Layer,DataSource> data = open();

        Layer l = data.first();

        QueryPlan qp = new QueryPlan(q);
        if (!Envelopes.isNull(q.getBounds())) {
            Envelope bb = q.getBounds();
            l.SetSpatialFilterRect(bb.getMinX(), bb.getMinY(), bb.getMaxX(), bb.getMaxY());
            qp.bounded();
        }

        return qp.apply(new OGRCursor(l, data.second(), schema()));
    }

    Pair<Layer,DataSource> open() throws IOException {
        DataSource ds = workspace.open();
        return Pair.of(ds.GetLayer(0), ds);
    }

    void close(Pair<Layer,DataSource> data) {
        data.first().delete();
        data.second().delete();
    }

    @Override
    public void close() {
    }
}
