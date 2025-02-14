/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.accumulo.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Accumulo;
import org.apache.accumulo.core.client.AccumuloClient;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.TableId;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection;
import org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.DataFileColumnFamily;
import org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.ServerColumnFamily;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.harness.AccumuloClusterHarness;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ImportTable didn't correctly place absolute paths in metadata. This resulted in the imported
 * table only being usable when the actual HDFS directory for Accumulo was the same as
 * Property.INSTANCE_DFS_DIR. If any other HDFS directory was used, any interactions with the table
 * would fail because the relative path in the metadata table (created by the ImportTable process)
 * would be converted to a non-existent absolute path.
 * <p>
 * ACCUMULO-3215
 */
public class ImportExportIT extends AccumuloClusterHarness {

  private static final Logger log = LoggerFactory.getLogger(ImportExportIT.class);

  @Override
  protected int defaultTimeoutSeconds() {
    return 60;
  }

  @Test
  public void testExportImportThenScan() throws Exception {
    try (AccumuloClient client = Accumulo.newClient().from(getClientProps()).build()) {

      String[] tableNames = getUniqueNames(2);
      String srcTable = tableNames[0], destTable = tableNames[1];
      client.tableOperations().create(srcTable);

      try (BatchWriter bw = client.createBatchWriter(srcTable)) {
        for (int row = 0; row < 1000; row++) {
          Mutation m = new Mutation(Integer.toString(row));
          for (int col = 0; col < 100; col++) {
            m.put(Integer.toString(col), "", Integer.toString(col * 2));
          }
          bw.addMutation(m);
        }
      }

      client.tableOperations().compact(srcTable, null, null, true, true);

      // Make a directory we can use to throw the export and import directories
      // Must exist on the filesystem the cluster is running.
      FileSystem fs = cluster.getFileSystem();
      log.info("Using FileSystem: " + fs);
      Path baseDir = new Path(cluster.getTemporaryPath(), getClass().getName());
      fs.deleteOnExit(baseDir);
      if (fs.exists(baseDir)) {
        log.info("{} exists on filesystem, deleting", baseDir);
        assertTrue("Failed to deleted " + baseDir, fs.delete(baseDir, true));
      }
      log.info("Creating {}", baseDir);
      assertTrue("Failed to create " + baseDir, fs.mkdirs(baseDir));
      Path exportDir = new Path(baseDir, "export");
      fs.deleteOnExit(exportDir);
      Path importDirA = new Path(baseDir, "import-a");
      Path importDirB = new Path(baseDir, "import-b");
      fs.deleteOnExit(importDirA);
      fs.deleteOnExit(importDirB);
      for (Path p : new Path[] {exportDir, importDirA, importDirB}) {
        assertTrue("Failed to create " + baseDir, fs.mkdirs(p));
      }

      Set<String> importDirs = Set.of(importDirA.toString(), importDirB.toString());

      Path[] importDirAry = new Path[] {importDirA, importDirB};

      log.info("Exporting table to {}", exportDir);
      log.info("Importing table from {}", importDirs);

      // Offline the table
      client.tableOperations().offline(srcTable, true);
      // Then export it
      client.tableOperations().exportTable(srcTable, exportDir.toString());

      // Make sure the distcp.txt file that exporttable creates is available
      Path distcp = new Path(exportDir, "distcp.txt");
      fs.deleteOnExit(distcp);
      assertTrue("Distcp file doesn't exist", fs.exists(distcp));
      FSDataInputStream is = fs.open(distcp);
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));

      // Copy each file that was exported to one of the imports directory
      String line;

      while ((line = reader.readLine()) != null) {
        Path p = new Path(line.substring(5));
        assertTrue("File doesn't exist: " + p, fs.exists(p));
        Path importDir = importDirAry[random.nextInt(importDirAry.length)];
        Path dest = new Path(importDir, p.getName());
        assertFalse("Did not expect " + dest + " to exist", fs.exists(dest));
        FileUtil.copy(fs, p, fs, dest, false, fs.getConf());
      }

      reader.close();

      log.info("Import dir A: {}", Arrays.toString(fs.listStatus(importDirA)));
      log.info("Import dir B: {}", Arrays.toString(fs.listStatus(importDirB)));

      // Import the exported data into a new table
      client.tableOperations().importTable(destTable, importDirs);

      // Get the table ID for the table that the importtable command created
      final String tableId = client.tableOperations().tableIdMap().get(destTable);
      assertNotNull(tableId);

      // Get all `file` colfams from the metadata table for the new table
      log.info("Imported into table with ID: {}", tableId);

      try (Scanner s = client.createScanner(MetadataTable.NAME, Authorizations.EMPTY)) {
        s.setRange(TabletsSection.getRange(TableId.of(tableId)));
        s.fetchColumnFamily(DataFileColumnFamily.NAME);
        ServerColumnFamily.DIRECTORY_COLUMN.fetch(s);

        // Should find a single entry
        for (Entry<Key,Value> fileEntry : s) {
          Key k = fileEntry.getKey();
          String value = fileEntry.getValue().toString();
          if (k.getColumnFamily().equals(DataFileColumnFamily.NAME)) {
            // The file should be an absolute URI (file:///...), not a relative path
            // (/b-000.../I000001.rf)
            String fileUri = k.getColumnQualifier().toString();
            assertFalse("Imported files should have absolute URIs, not relative: " + fileUri,
                looksLikeRelativePath(fileUri));
          } else if (k.getColumnFamily().equals(ServerColumnFamily.NAME)) {
            assertFalse("Server directory should have absolute URI, not relative: " + value,
                looksLikeRelativePath(value));
          } else {
            fail("Got expected pair: " + k + "=" + fileEntry.getValue());
          }
        }

      }
      // Online the original table before we verify equivalence
      client.tableOperations().online(srcTable, true);

      verifyTableEquality(client, srcTable, destTable);
    }
  }

  private void verifyTableEquality(AccumuloClient client, String srcTable, String destTable)
      throws Exception {
    Iterator<Entry<Key,Value>> src =
        client.createScanner(srcTable, Authorizations.EMPTY).iterator(),
        dest = client.createScanner(destTable, Authorizations.EMPTY).iterator();
    assertTrue("Could not read any data from source table", src.hasNext());
    assertTrue("Could not read any data from destination table", dest.hasNext());
    while (src.hasNext() && dest.hasNext()) {
      Entry<Key,Value> orig = src.next(), copy = dest.next();
      assertEquals(orig.getKey(), copy.getKey());
      assertEquals(orig.getValue(), copy.getValue());
    }
    assertFalse("Source table had more data to read", src.hasNext());
    assertFalse("Dest table had more data to read", dest.hasNext());
  }

  private boolean looksLikeRelativePath(String uri) {
    if (uri.startsWith("/" + Constants.BULK_PREFIX)) {
      return uri.charAt(10) == '/';
    } else {
      return uri.startsWith("/" + Constants.CLONE_PREFIX);
    }
  }
}
