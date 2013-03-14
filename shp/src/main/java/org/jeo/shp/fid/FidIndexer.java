/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2003-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.jeo.shp.fid;

import static org.jeo.shp.file.ShpFileType.*;

import java.io.IOException;
import java.net.URL;

import org.jeo.shp.file.ShpFiles;
import org.jeo.shp.file.StorageFile;
import org.jeo.shp.shp.IndexFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Creates a .fix file (fid index).
 * 
 * @author Jesse
 *
 *
 *
 * @source $URL$
 */
public class FidIndexer {
    static Logger LOGGER = LoggerFactory.getLogger(FidIndexer.class);

    /**
     * Generates the FID index file for the shpFile
     */
    public static synchronized void generate(URL shpURL) throws IOException {
        generate(new ShpFiles(shpURL));
    }

    /**
     * Generates the FID index file for the shpFiles
     */
    public static void generate(ShpFiles shpFiles) throws IOException {
        LOGGER.debug("Generating fids for " + shpFiles.get(SHP));

        
        IndexFile indexFile = null;
        StorageFile file = shpFiles.getStorageFile(FIX);
        IndexedFidWriter writer = null;
        
        try {
            indexFile = new IndexFile(shpFiles, false);

            // writer closes channel for you.
            writer = new IndexedFidWriter(shpFiles, file);

            for (int i = 0, j = indexFile.getRecordCount(); i < j; i++) {
                writer.next();
            }

        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                file.replaceOriginal();
            } finally {
                if (indexFile != null) {
                    indexFile.close();
                }
            }
        }
    }

}