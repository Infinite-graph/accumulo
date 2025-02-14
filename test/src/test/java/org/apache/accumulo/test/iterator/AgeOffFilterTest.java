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
package org.apache.accumulo.test.iterator;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.TreeMap;

import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.user.AgeOffFilter;
import org.apache.accumulo.iteratortest.IteratorTestCaseFinder;
import org.apache.accumulo.iteratortest.IteratorTestInput;
import org.apache.accumulo.iteratortest.IteratorTestOutput;
import org.apache.accumulo.iteratortest.junit4.BaseJUnit4IteratorTest;
import org.apache.accumulo.iteratortest.testcases.IteratorTestCase;
import org.junit.runners.Parameterized.Parameters;

/**
 * Iterator test harness tests for AgeOffFilter
 */
public class AgeOffFilterTest extends BaseJUnit4IteratorTest {
  private static final long NOW = 12345678; // arbitrary base time
  private static final long TTL = 30_000; // 30 seconds

  @Parameters
  public static Object[][] parameters() {
    var opts = createOpts();
    var input = createInputData(opts);
    var output = createOutputData(input);
    assertEquals(15, input.getInput().size());
    assertEquals(10, output.getOutput().size());

    var tests = IteratorTestCaseFinder.findAllTestCases();
    return BaseJUnit4IteratorTest.createParameters(input, output, tests);
  }

  // set up the iterator
  private static Map<String,String> createOpts() {
    IteratorSetting setting = new IteratorSetting(50, AgeOffFilter.class);
    AgeOffFilter.setCurrentTime(setting, NOW);
    AgeOffFilter.setTTL(setting, TTL);
    return setting.getOptions();
  }

  // create data with timestamps greater than, equal to, and less than NOW
  // the ones that would be dropped are ones that are older than NOW - TTL
  private static IteratorTestInput createInputData(Map<String,String> opts) {
    TreeMap<Key,Value> data = new TreeMap<>();
    final Value value = new Value("a");

    data.put(new Key("1", "a", "a", NOW + 25_000), value);
    data.put(new Key("2", "a", "a", NOW + 35_000), value);
    data.put(new Key("3", "a", "a", NOW + 55_000), value);
    data.put(new Key("4", "a", "a", NOW + 00_000), value);
    data.put(new Key("5", "a", "a", NOW - 29_000), value);
    data.put(new Key("6", "a", "a", NOW - 28_000), value);
    data.put(new Key("7", "a", "a", NOW - 40_000), value); // Will drop
    data.put(new Key("8", "a", "a", NOW - 30_000), value); // Not aged out (exclusive comparison)
    data.put(new Key("9", "a", "a", NOW - 31_000), value); // Will drop
    data.put(new Key("a", "-", "-", NOW - 50_000), value); // Will drop
    data.put(new Key("a", "a", "-", NOW - 20_000), value);
    data.put(new Key("a", "a", "a", NOW + 50_000), value);
    data.put(new Key("a", "a", "b", NOW - 15_000), value); // Will drop
    data.put(new Key("a", "a", "c", NOW - 32_000), value); // Will drop
    data.put(new Key("a", "a", "d", NOW - 32_000), value);

    return new IteratorTestInput(AgeOffFilter.class, opts, new Range(), data);
  }

  // create expected output data
  // data should include all input data except those older than the time at NOW - TTL
  private static IteratorTestOutput createOutputData(IteratorTestInput input) {
    var data = new TreeMap<Key,Value>(input.getInput());
    data.entrySet().removeIf(e -> e.getKey().getTimestamp() < NOW - TTL);
    return new IteratorTestOutput(data);
  }

  public AgeOffFilterTest(IteratorTestInput input, IteratorTestOutput expectedOutput,
      IteratorTestCase testCase) {
    super(input, expectedOutput, testCase);
  }

}
