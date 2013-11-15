package org.jeo.ogr;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.gdal.ogr.DataSource;
import org.gdal.ogr.Driver;
import org.gdal.ogr.ogr;
import org.jeo.data.Disposable;
import org.jeo.data.FileVectorDriver;
import org.jeo.feature.Schema;
import org.jeo.util.Key;
import org.jeo.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OGR extends FileVectorDriver<OGRWorkspace> implements Disposable {

    public static final Key<String> DRIVER = new Key<String>("driver", String.class);

    static final Logger LOG = LoggerFactory.getLogger(OGR.class);

    public static void init() throws Throwable {
        if (ogr.GetDriverCount() == 0) {
            ogr.RegisterAll();
        }
    }

    static {
        try {
            init();
        }
        catch(Throwable e) {
            LOG.debug("gdal initialization failed", e);
        }
    }

    public static OGRWorkspace open(File file) throws IOException {
        return new OGR().open(file, null);
    }

    org.gdal.ogr.Driver ogrDrv;

    public OGR() {
    }

    public OGR(org.gdal.ogr.Driver ogrDrv) {
        this.ogrDrv = ogrDrv;
    }

    @Override
    public void close() {
        if (ogrDrv != null) {
            ogrDrv.delete();
            ogrDrv = null;
        }
    }

    @Override
    public String getName() {
        return ogrDrv != null ? ogrDrv.GetName() : "OGR";
    }

    @Override
    public Class<OGRWorkspace> getType() {
        return OGRWorkspace.class;
    }

    @Override
    public List<Key<?>> getKeys() {
        return (List) Arrays.asList(FILE, DRIVER);
    }

    @Override
    protected boolean canCreate(File file, Map<?, Object> opts, Messages msgs) {
        return false;
    }

    @Override
    protected OGRWorkspace create(File file, Map<?, Object> opts, Schema schema) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canOpen(Map<?, Object> opts, Messages msgs) {
        try {
            init();
        }
        catch(Throwable t) {
            Messages.of(msgs).report(t);
            return false;
        }

        return super.canOpen(opts, msgs);
    }

    @Override
    protected boolean canOpen(File file, Map<?, Object> opts, Messages msgs) {
        msgs = Messages.of(msgs);

        if (DRIVER.has(opts)) {
            String drvName = DRIVER.get(opts);
            Driver drv = ogr.GetDriverByName(drvName);
            if (drv == null) {
                msgs.report("Unknown driver: " + drvName);
                return false;
            }

            try {
                DataSource data = drv.Open(file.getAbsolutePath());
                if (data == null) {
                    msgs.report("Driver: " + drvName + " unable to open file: " + file);
                    return false;
                }
            }
            catch(Exception e) {
                msgs.report(e);
                return false;
            }
        }

        return true;
    }

    @Override
    protected OGRWorkspace open(File file, Map<?, Object> opts) throws IOException {
        // was driver explicitly specified
        if (DRIVER.has(opts)) {
            Driver drv = ogr.GetDriverByName(DRIVER.get(opts));
            return new OGRWorkspace(file, new OGR(drv));
        }

        DataSource ds = ogr.OpenShared(file.getAbsolutePath());
        if (ds == null) {
            throw new IOException("Unable to open file: " + file);
        }

        try {
            return new OGRWorkspace(file, new OGR(ds.GetDriver()));
        }
        finally {
            ds.delete();
        }
    }

}
