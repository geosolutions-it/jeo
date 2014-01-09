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
package org.jeo.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jeo.json.JSONObject;
import org.jeo.json.JSONValue;
import org.jeo.filter.Filter;
import org.jeo.util.Convert;
import org.jeo.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository defined by a JSON object.
 * <p>
 * The repository is made up a JSON object whose keys are names of the workspaces in the repo. Each 
 * key maps to an object that contains the following properties.
 * <ul>
 *   <li>driver - The name or alias identifying the driver for the workspace.
 *   <li>keys - Object containing the key / values pairs defining the connection options.
 * </ul>
 * The following is an example consisting of a single GeoJSON dataset.
 * <code><pre>
 * {
 *   "states": {
 *      "driver": "GeoJSON", 
 *      "keys": {
 *        "file": "states.json"
 *      }
 *   }
 * }
 * </pre></code>
 * </p>
 * @author Justin Deoliveira, Boundless
 *
 */
public class JSONRepository implements DataRepository {

    static Logger LOG = LoggerFactory.getLogger(JSONRepository.class);

    JSONObject obj;
    File file;
    DriverRegistry drivers;

    public JSONRepository(JSONObject obj) {
        this(obj, Drivers.REGISTRY);
    }

    public JSONRepository(JSONObject obj, DriverRegistry drivers) {
        this.obj = obj;
        this.drivers = drivers;
    }

    public JSONRepository(File file) {
        this(file, Drivers.REGISTRY);
    }

    public JSONRepository(File file, DriverRegistry drivers) {
        this.file = file;
        this.drivers = drivers;
    }

    @Override
    public Iterable<Handle<?>> query(Filter<? super Handle<?>> filter)
            throws IOException {

        JSONObject reg = obj();

        List<Handle<?>> list = new ArrayList<Handle<?>>();
        
        for (Object k : reg.keySet()) {
            String key = k.toString();

            Object o = reg.get(key);
            if (!(o instanceof JSONObject)) {
                LOG.debug(key + " is not an object");
                return null;
            }

            JSONObject obj = (JSONObject) o;
            if (!obj.containsKey("driver")) {
                LOG.debug(key + " does not define a 'driver' key");
                return null;
            }

            String driver = obj.get("driver").toString();
            Driver<?> drv = Drivers.find(driver, drivers);
            if (drv == null) {
                LOG.debug("unable to load driver: " + driver);
                return null;
            }

            Handle<?> h = Handle.to(key, drv, this);
            if (filter.apply(h)) {
                list.add(h);
            }
        }
        return list;
    }

    @Override
    public <T> T get(String name, Class<T> type) throws IOException {
        JSONObject reg = obj();

        JSONObject wsObj = (JSONObject) reg.get(name);
        if (wsObj == null) {
            return null;
        }

        if (!wsObj.containsKey("driver")) {
            throw new IOException(name + " does not define a 'driver' key");
        }

        String driver = wsObj.get("driver").toString();
        Driver<?> drv = Drivers.find(driver, drivers);
        if (drv == null) {
            throw new IOException("unable to load driver: " + driver);
        }

        Map<Object,Object> opts = new HashMap<Object,Object>();
        if (wsObj.containsKey("keys")) {
            opts.putAll((Map)wsObj.get("keys"));
        }
        else if (wsObj.containsKey("file")){
            opts.put("file", wsObj.get("file"));
        }

        // look for any file keys, make relative paths relative to the Repository file
        if (file != null) {
            for (Map.Entry<Object, Object> kv : opts.entrySet()) {
                if ("file".equalsIgnoreCase(kv.getKey().toString())) {
                    Optional<File> file = Convert.toFile(kv.getValue());
                    if (file.has()) {
                        if (!file.get().isAbsolute()) {
                            File f = new File(this.file.getParentFile(), file.get().getPath());
                            kv.setValue(f);
                        }
                    }
                }
            }
        }
        
        Object data = drv.open(opts);
        if (data != null) {
            if (data instanceof Dataset) {
                data = new SingleWorkspace((Dataset)data);
            }
        }
        else {
            LOG.debug("Unable to open from options: " + opts);
        }

        return type.cast(data);
    }

    @Override
    public void close() {
    }

    JSONObject obj() throws IOException {
        if (obj != null) {
            return obj;
        }

        BufferedReader r = new BufferedReader(new FileReader(file));
        try {
            return (JSONObject) JSONValue.parseWithException(r);
        }
        catch(Exception e) {
            throw new IOException("Error parsing json file: " + file.getPath(), e);
        }
        finally {
            r.close();
        }
    }


}
