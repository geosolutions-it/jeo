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
package org.jeo.geom;

import java.util.Iterator;

import org.jeo.geojson.GeoJSONWriter;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Geometry module utility module. 
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class Geom {

    /**
     * Geometry type enumeration.
     */
    public enum Type {
        POINT(Point.class),
        LINESTRING(LineString.class),
        POLYGON(Polygon.class),
        MULTIPOINT(MultiPoint.class),
        MULTILINESTRING(MultiLineString.class),
        MULTIPOLYGON(MultiPolygon.class),
        GEOMETRY(Geometry.class),
        GEOMETRYCOLLECTION(GeometryCollection.class);
        
        private final Class<? extends Geometry> type;
        private final String name;
        private final String simpleName;
        
        private Type(Class<? extends Geometry> type) {
            this.type = type;
            this.name = type.getSimpleName();
            this.simpleName = (name.startsWith("Multi") ? name.substring(5) : name);
        }
        
        /**
         * Return the {@code Geometry} class associated with this type.
         *
         * @return the {@code Geometry} class
         */
        public Class<? extends Geometry> getType() {
            return type;
        }
        
        /**
         * Equivalent to {@linkplain #getName()}.
         *
         * @return the name of this type
         */
        @Override
        public String toString() {
            return name;
        }
        
        /**
         * Return a name for this type that is suitable for text descriptions.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Get the 'simple name'. Returns the same value as {@linkplain #getName()}
         * except for MULTIPOINT, MULTILINESTRING and MULTIPOLYGON, for which it returns
         * the name without the 'Multi' prefix.
         *
         * @return the simple name
         */
        public String getSimpleName() {
            return simpleName;
        }
        
        /**
         * Get the {@code Geometries} for the given object.
         *
         * @param geom a JTS Geometry object
         *
         * @return the {@code Geometries} for the argument's class, or {@code null}
         *         if the argument is {@code null}
         */
        public static Type from(Geometry geom) {
            if (geom != null) {
                return from(geom.getClass());
            }
        
            return null;
        }
        
        /**
         * Get the {@code Geometries} for the given {@code Geometry} class.
         *
         * @param geomClass the class
         *
         * @return the constant for this class
         */
        public static Type from(Class<?> geomClass) {
            for (Type gt : Type.values()) {
                if (gt.type == geomClass) {
                    return gt;
                }
            }
            
            //no direct match look for a subclass
            Type match = null;
        
            for (Type gt : Type.values()) {
                if (gt == GEOMETRY || gt == GEOMETRYCOLLECTION) {
                    continue;
                }
                
                if (gt.type.isAssignableFrom(geomClass)) {
                    if (match == null) {
                        match = gt;
                    } else {
                        // more than one match
                        return null;
                    }
                }
            }
            
            if (match == null) {
                //no matches from concrete classes, try abstract classes
                if (GeometryCollection.class.isAssignableFrom(geomClass)) {
                    return GEOMETRYCOLLECTION;
                }
                if (Geometry.class.isAssignableFrom(geomClass)) {
                    return GEOMETRY;
                }
            }
            
            return match;
        }
        
        /**
         * Get the {@code Geometries} for the specified name.
         * 
         * @param name The name of the geometry, eg: "POINT"
         * 
         * @return The constant for the name.
         */
        public static Type from(String name) {
            for (Type gt : Type.values()) {
                if (gt.getName().equalsIgnoreCase(name)) {
                    return gt;
                }
            }
            return null;
        }
    }

    /**
     * Creates a new geometry builder.
     */
    public static GeomBuilder build() {
        return new GeomBuilder();
    }

    /**
     * Convenience method to build a Point geometry.
     */
    public static Point point(double x, double y) {
        return build().point(x, y).toPoint();
    }

    /**
     * Convenience method to build a LineString geometry.
     * 
     * @param ord Even number of ordinates forming coordinates for the line string.
     */
    public static LineString lineString(double... ord) {
        return build().points(ord).toLineString();
    }

    /**
     * Convenience method to build a Polygon geometry.
     * 
     * @param ord Even number of ordinates forming coordinates for the outer ring of the polygon.
     */
    public static Polygon polygon(double... ord) {
        return build().points(ord).toPolygon();
    }

    /**
     * Returns an iterable over the points of a multipoint.
     */
    public static Iterable<Point> iterate(MultiPoint mp) {
        return new GeometryIterable<Point>(mp);
    }

    /**
     * Returns an iterable over the lines of a multilinestring.
     */
    public static Iterable<LineString> iterate(MultiLineString ml) {
        return new GeometryIterable<LineString>(ml);
    }

    /**
     * Returns an iterable over the polygons of a multipolygon.
     */
    public static Iterable<Polygon> iterate(MultiPolygon mp) {
        return new GeometryIterable<Polygon>(mp);
    }
    
    /**
     * Returns an iterable over the geometries of a geometry collection.. 
     */
    public static Iterable<Geometry> iterate(GeometryCollection gc) {
        return new GeometryIterable<Geometry>(gc);
    }

    /*
     * Private iterable class.
     */
    private static class GeometryIterable<T extends Geometry> implements Iterable<T> {

        GeometryCollection gc;

        GeometryIterable(GeometryCollection gc) {
            this.gc = gc;
        }

        @Override
        public Iterator<T> iterator() {
            return new GeometryIterator<T>(gc);
        }
    }

    /*
     * Private iterator class.
     */
    private static class GeometryIterator<T extends Geometry> implements Iterator<T> {

        int i = 0; 
        GeometryCollection gc;

        GeometryIterator(GeometryCollection gc) {
            this.gc = gc;
        }

        @Override
        public boolean hasNext() {
            return i < gc.getNumGeometries();
        }

        @SuppressWarnings("unchecked")
        @Override
        public T next() {
            return (T) gc.getGeometryN(i++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Returns the GeoJSON representation of the Geometry object.
     * 
     * @param g The geometry.
     * 
     * @return The GeoJSON string.
     * 
     * @see {@Link GeoJSONWriter}
     */
    public String json(Geometry g) {
        return GeoJSONWriter.toString(g);
    }
}
