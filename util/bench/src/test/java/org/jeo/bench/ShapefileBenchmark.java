package org.jeo.bench;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.jeo.Tests;
import org.jeo.data.Query;
import org.jeo.data.VectorDataset;
import org.jeo.feature.Feature;
import org.jeo.ogr.Shapefile;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

import com.vividsolutions.jts.geom.Envelope;

public class ShapefileBenchmark extends MacroBenchmark {

    File shpFile;

    VectorDataset shp;

    FilterFactory ff;
    SimpleFeatureSource gtshp;

    List<Envelope> boxes;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        setUpFile();
        setUpShp();
        setUpGeoToolsShp();
        setUpBoxes();
    }

    void setUpFile() throws Exception {
        File tmp = Tests.newTmpDir("shp", "bench");
        Tests.unzip(getClass().getResourceAsStream("countries.zip"), tmp);
        shpFile = new File(tmp, "countries.shp");
    }

    void setUpShp() throws Exception {
        shp = Shapefile.open(shpFile);
    }

    void setUpGeoToolsShp() throws Exception {
        ShapefileDataStore shp = new ShapefileDataStore(shpFile.toURI().toURL());
        gtshp = shp.getFeatureSource();

        ff = CommonFactoryFinder.getFilterFactory();
    }

    void setUpBoxes() {
        boxes = new ArrayList<Envelope>();
        Random r = new Random();
        for (int i = 0; i < 100; i++) {
            double x = r.nextInt(350) - 180 + r.nextDouble();
            double y = r.nextInt(170) - 90 + r.nextDouble();
            double x1 = x+r.nextInt(5) + r.nextDouble();
            double y1 = y+r.nextInt(5) + r.nextDouble();
            boxes.add(new Envelope(x,x1,y,y1));
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        tearDownShp();
        tearDownGeoToolsShp();
    }

    void tearDownShp() {
        shp.close();
    }

    void tearDownGeoToolsShp() {
        gtshp.getDataStore().dispose();
    }

    public void timeShp(int reps) throws Exception {
        for (int i = 0; i < reps; i++) {
            for (Envelope box : boxes) {
                for (Feature f : shp.cursor(new Query().bounds(box))) {
                }
            }
        }
    }

    public void timeGeoToolsShp(int reps) throws Exception {
        for (int i = 0; i < reps; i++) {
            for (Envelope box : boxes) {
                Filter f = ff.bbox(
                    "the_geom", box.getMinX(), box.getMinY(), box.getMinY(), box.getMaxY(), null);
                
                SimpleFeatureCollection features = gtshp.getFeatures(f);
                SimpleFeatureIterator it = features.features();
                try {
                    while(it.hasNext()) {
                        it.next();
                    }
                }
                finally {
                    it.close();
                }

            }
        }
    }

    public static void main(String[] args) throws Exception {
        new ShapefileBenchmark().configure(3,100).run();
    }
}
