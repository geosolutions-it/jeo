package org.jeo.bench;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.google.caliper.SimpleBenchmark;
import com.google.common.base.Stopwatch;

/**
 * Base class for macro benchmarks.
 * 
 * @author Justin Deoliveira, Boundless
 */
public class MacroBenchmark extends SimpleBenchmark {

    int sets = 3;
    int reps = 10;

    public void run() throws Exception {
        List<Bench> benchmarks = loadBenchmarks();

        // warm up
        doRun(benchmarks, false);
        doRun(benchmarks, true);
    }

    public MacroBenchmark configure(int sets, int reps) {
        this.sets = sets;
        this.reps = reps;
        return this;
    }

    void doRun(List<Bench> benchmarks, boolean report) throws Exception {
        setUp();
        for (Bench b : benchmarks) {
            b.run(sets, reps);
            if (report) {
                System.out.println(String.format("%s: %f ms", b.name(), b.average()));
            }
        }
        tearDown();
    }

    protected List<Bench> loadBenchmarks() {
        List<Bench> l = new ArrayList<Bench>();
        for (final Method m : getClass().getMethods()) {
            if (m.getName().startsWith("time")) {
                l.add(new Bench(this, m));
            }
        }
        return l;
    }

    static class Bench {
        SimpleBenchmark bm;
        Method m;

        Stopwatch watch = new Stopwatch(); 
        double avg;

        Bench(SimpleBenchmark bm, Method m) {
            this.bm = bm;
            this.m = m;
        }

        public void run(int sets, int reps) throws Exception {
            long total = 0;
            for (int i = 0; i < sets; i++) {
                watch.start();
                m.invoke(bm, reps);
                watch.stop();
                total += watch.elapsedMillis();
            }
            avg = total / ((double)(sets * reps));
        }

        public String name() {
            return m.getName().substring(4);
        }

        public double average() {
            return avg;
        }
    }

}
