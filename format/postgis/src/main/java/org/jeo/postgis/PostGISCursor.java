package org.jeo.postgis;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jeo.data.Cursor;
import org.jeo.feature.DiffFeature;
import org.jeo.feature.Feature;
import org.jeo.feature.MapFeature;
import org.jeo.sql.PrimaryKey;
import org.jeo.sql.PrimaryKeyColumn;

import com.vividsolutions.jts.io.WKBReader;

public class PostGISCursor extends Cursor<Feature> {

    ResultSet rs;
    Connection cx;
    PostGISDataset dataset;
    Boolean hasNext;
    Feature next;

    PostGISCursor(ResultSet rs, Connection cx, Mode mode, PostGISDataset dataset) {
        super(mode);
        this.rs = rs;
        this.cx = cx;
        this.dataset = dataset;
    }

    @Override
    public boolean hasNext() throws IOException {
        if (hasNext == null) {
            try {
                hasNext = rs.next();
            } catch (SQLException e) {
                handle(e);
            }
        }
        return hasNext;
    }

    @Override
    public Feature next() throws IOException {
        if (hasNext != null && hasNext.booleanValue()) {
            try {
                Map<String,Object> map = new LinkedHashMap<String, Object>();
                ResultSetMetaData md = rs.getMetaData();
                for (int i = 0; i < md.getColumnCount(); i++) {
                    Object obj = rs.getObject(i+1);
                    String col = md.getColumnName(i+1);

                    if (dataset.getSchema().field(col).isGeometry()) {
                        obj = new WKBReader().read(rs.getBytes(i+1));
                    }

                    map.put(col, obj);
                }

                PrimaryKey key = dataset.getTable().getPrimaryKey();
                StringBuilder sb = new StringBuilder();
                for (PrimaryKeyColumn pkcol : key.getColumns()) {
                    sb.append(map.get(pkcol.getName())).append(".");
                }
                if (!key.getColumns().isEmpty()) {
                    sb.setLength(sb.length()-1);
                }

                next = new MapFeature(sb.toString(), map, dataset.getSchema());
                return next = mode == Cursor.UPDATE ? new DiffFeature(next) : next;
            }
            catch(Exception e) {
                handle(e);
            }
            finally {
                hasNext = null;
            }
        }

        return null;
    }

    @Override
    protected void doWrite() throws IOException {
        dataset.doUpdate(next, ((DiffFeature) next).getChanged(), cx);
    }

    @Override
    public void close() throws IOException {
        if (rs != null) {
            Statement st = null;
            try {
                st = rs.getStatement();
            } catch (SQLException e) {}

            try {
                rs.close();
            } catch (SQLException e) {}

            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {}
            }
            if (cx != null) {
                try {
                    cx.close();
                } catch (SQLException e) {}
            }
        }
    }

    void handle(Exception e) throws IOException {
        close();
        throw new IOException(e);
    }
}
