/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.core.client.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.data.KeyExtent;
import org.apache.accumulo.core.data.thrift.IterInfo;


/**
 * 
 */
public class ActiveCompaction {
  
  private org.apache.accumulo.core.tabletserver.thrift.ActiveCompaction tac;
  private Instance instance;

  ActiveCompaction(Instance instance, org.apache.accumulo.core.tabletserver.thrift.ActiveCompaction tac) {
    this.tac = tac;
    this.instance = instance;
  }

  static enum CompactionType {
    /**
     * compaction to flush a tablets memory
     */
    MINOR,
    /**
     * compaction to flush a tablets memory and merge it with the tablets smallest file. This type compaction is done when a tablet has too many files
     */
    MERGE,
    /**
     * compaction that merges a subset of a tablets files into one file
     */
    MAJOR,
    /**
     * compaction that merges all of a tablets files into one file
     */
    FULL
  };
  
  static enum CompactionReason {
    /**
     * compaction initiated by user
     */
    USER,
    /**
     * Compaction initiated by system
     */
    SYSTEM,
    /**
     * Compaction initiated by merge operation
     */
    CHOP,
    /**
     * idle compaction
     */
    IDLE,
    /**
     * Compaction initiated to close a unload a tablet
     */
    CLOSE
  };
  
  /**
   * 
   * @return name of the table the compaction is running against
   * @throws TableNotFoundException
   */
  
  public String getTable() throws TableNotFoundException {
    return Tables.getTableName(instance, getExtent().getTableId().toString());
  }
  
  /**
   * @return tablet thats is compacting
   */

  public KeyExtent getExtent() {
    return new KeyExtent(tac.getExtent());
  }
  
  /**
   * @return how long the compaction has been running in milliseconds
   */

  public long getAge() {
    return tac.getAge();
  }
  
  /**
   * @return number of files compaction is reading
   */

  public int getInputFiles() {
    return tac.getInputFiles();
  }
  
  /**
   * @return file compactions is writing too
   */

  public String getOutputFile() {
    return tac.getOutputFile();
  }
  
  /**
   * @return the type of compaction
   */
  public CompactionType getType() {
    return CompactionType.valueOf(tac.getType().name());
  }
  
  /**
   * @return the reason the compaction was started
   */

  public CompactionReason getReason() {
    return CompactionReason.valueOf(tac.getReason().name());
  }
  
  /**
   * @return the locality group that is compacting
   */

  public String getLocalityGroup() {
    return tac.getLocalityGroup();
  }
  
  /**
   * @return the number of key/values read by the compaction
   */

  public long getEntriesRead() {
    return tac.getEntriesRead();
  }
  
  /**
   * @return the number of key/values written by the compaction
   */

  public long getEntriesWritten() {
    return tac.getEntriesWritten();
  }
  
  /**
   * @return the per compaction iterators configured
   */

  public List<IteratorSetting> getIterators() {
    ArrayList<IteratorSetting> ret = new ArrayList<IteratorSetting>();
    
    for (IterInfo ii : tac.getSsiList()) {
      IteratorSetting settings = new IteratorSetting(ii.getPriority(), ii.getIterName(), ii.getClassName());
      Map<String,String> options = tac.getSsio().get(ii.getIterName());
      settings.addOptions(options);
      
      ret.add(settings);
    }
    
    return ret;
  }
}
