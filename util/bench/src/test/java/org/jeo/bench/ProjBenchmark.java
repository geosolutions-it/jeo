/* Copyright 2014 The jeo project. All rights reserved.
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
package org.jeo.bench;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.geotools.referencing.CRS;
import org.jeo.proj.Proj;
import org.opengis.referencing.operation.MathTransform;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;

import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

/**
 * Benchmark for coordinate re-projection.
 * 
 * @author Justin Deoliveira, Boundless
 */
public class ProjBenchmark extends SimpleBenchmark{

    CoordinateTransform projTx;
    ProjCoordinate pc;

    MathTransform gtTx;
    double[] d;

    List<Double> points;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        setUpProj();
        setUpGeoTools();
        setUpPoints();
    }

    void setUpPoints() {
        points = new ArrayList<Double>(1000);
        Random r = new Random();
        for (int i = 0; i < 1000; i++) {
            double x = r.nextInt(360) - 180 + r.nextDouble();
            double y = r.nextInt(180) - 90 + r.nextDouble();

            points.add(x);
            points.add(y);
        }
    }

    void setUpProj() {
        CoordinateTransformFactory txFactory = new CoordinateTransformFactory();
        projTx = txFactory.createTransform(Proj.crs(4326), Proj.crs(3005));

        pc = new ProjCoordinate();
    }

    void setUpGeoTools() throws Exception {
        gtTx = CRS.findMathTransform(CRS.decode("EPSG:4326"), CRS.decode("EPSG:3005"), true);
        d = new double[2];
    }

    public void timeFromGeo(int reps) {
        for (int i = 0; i < reps; i++) {
            for (int p = 0; p < points.size(); p+=2) {
                pc.x = points.get(p);
                pc.y = points.get(p+1);

                projTx.transform(pc, pc);
            }
        }
    }

    public void timeFromGeoGeoTools(int reps) throws Exception {
        for (int i = 0; i < reps; i++) {
            for (int p = 0; p < points.size(); p+=2) {
                d[0] = points.get(p);
                d[1] = points.get(p+1);

                gtTx.transform(d, 0, d, 0, 1);
            }
        }
    }

    public static void main(String[] args) {
        Runner.main(ProjBenchmark.class, args);
    }
}
