package org.jeo.nano;

import static org.jeo.nano.NanoHTTPD.HTTP_OK;
import static org.jeo.nano.NanoHTTPD.MIME_JSON;

import java.io.StringWriter;

import org.jeo.data.DataRef;
import org.jeo.data.Driver;
import org.jeo.data.Registry;
import org.jeo.data.Workspace;
import org.jeo.geojson.GeoJSONWriter;
import org.jeo.nano.NanoHTTPD.Response;

public class RootHandler extends Handler {

    @Override
    public boolean canHandle(Request request, NanoJeoServer server) {
        return "/".equals(request.getUri());
    }
    
    @Override
    public Response handle(Request request, NanoJeoServer server) throws Exception {
        Registry reg = server.getRegistry();

        StringWriter out = new StringWriter();

        GeoJSONWriter writer = new GeoJSONWriter(out);
        writer.obj();
        for (Registry.Item item : reg.list()) {
            writer.key(item.getName()).obj();

            Driver<?> drv = item.getDriver();
            writer.key("driver").value(drv.getName());

            if (Workspace.class.isAssignableFrom(drv.getClass())) {
                writer.key("datasets").array();

                Workspace ws = (Workspace) reg.get(item.getName());
                try {
                    for (DataRef<?> ref : ws.list()) {
                        writer.value(ref.first());
                    }
                }
                finally {
                    ws.close();
                }

                writer.endArray();
            }
            
            writer.endObj();

        }
        writer.endObj();
        writer.flush();

        return new Response(HTTP_OK, MIME_JSON, out.toString());
    }

}
