<#--

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

-->
      <div class="row">
        <div class="col-xs-12">
          <h3>${title}</h3>
        </div>
      </div>
    <#if coordinatorRunning == true>
    <div id="ecDiv">
      <div class="row">
        <div class="col-xs-12">
          <div class="panel panel-primary">
            <div class="panel-heading">Compaction&nbsp;Coordinator&nbsp;running&nbsp;on:&nbsp;<span id="ccHostname" title="The hostname of the compaction coordinator server"></span></div>
            <div class="panel-body">
                Queues&nbsp;<span id="ccNumQueues" class="badge" title="Number of queues configured">0</span></span>&nbsp;&nbsp;&nbsp;&nbsp;
                Compactors&nbsp;<span id="ccNumCompactors" class="badge" title="Number of compactors running">0</span>&nbsp;&nbsp;&nbsp;&nbsp;
                Last&nbsp;Contact&nbsp;<span id="ccLastContact" class="badge" title="Last time data was fetched. Server fetches on refresh, at most every minute."></span>
            </div>
          </div>
        </div>
      </div>
      <div class="row">
      <div class="col-xs-12">
        <table id="compactorsTable" class="table table-bordered table-striped table-condensed">
          <caption><span class="table-caption">Compactors</span>
          <a href="javascript:refreshCompactors();"><span class="glyphicon glyphicon-refresh"/></a></caption>
          <thead>
            <tr>
              <th class="firstcell" title="The hostname the compactor is running on.">Server</th>
              <th title="The name of the queue this compactor is assigned.">Queue</th>
              <th class="duration" title="Last time data was fetched. Server fetches on refresh, at most every minute.">Last Contact</th>
            </tr>
          </thead>
        </table>
      </div>
      <div class="row">
          <div class="col-xs-12">
            <table id="runningTable" class="table table-bordered table-striped table-condensed">
              <caption><span class="table-caption">Running Compactions</span>
              <a href="javascript:refreshRunning();"><span class="glyphicon glyphicon-refresh"/></a></caption>
              <thead>
                <tr>
                  <th class="firstcell" title="The hostname the compactor is running on.">Server Hostname</th>
                  <th title="The type of compaction.">Kind</th>
                  <th title="The status returned by the last update.">Status</th>
                  <th title="The name of the queue this compactor is assigned.">Queue</th>
                  <th title="The ID of the table being compacted.">Table ID</th>
                  <th title="The number of files being compacted."># of Files</th>
                  <th title="The progress of the compaction." class="progBar">Progress</th>
                  <th class="duration" title="The time of the last update for the compaction">Last Update</th>
                  <th class="duration" title="How long compaction has been running">Duration</th>
                  <th class="details-control">More</th>
                </tr>
              </thead>
              <tbody></tbody>
            </table>
          </div>
      </div>
    </div>
   <#else>
    <div id="ccBanner"><div class="alert alert-danger" role="alert">Compaction Coordinator Not Running</div></div>
  </#if>
