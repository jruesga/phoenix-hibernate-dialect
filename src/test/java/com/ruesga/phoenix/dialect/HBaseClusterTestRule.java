/*
 * Copyright (C) 2017 Jorge Ruesga
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
package com.ruesga.phoenix.dialect;

import java.io.File;
import java.io.IOException;

import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.MiniHBaseCluster;
import org.apache.log4j.Logger;
import org.junit.rules.ExternalResource;

public class HBaseClusterTestRule extends ExternalResource {

    private final static Logger LOG = Logger.getLogger(HBaseClusterTestRule.class);

    private final String[] extraResources;

    private TestingServer zk;
    private MiniHBaseCluster cluster;
    private HBaseTestingUtility util;
    private Configuration configuration;

    public HBaseClusterTestRule(String... extraResources) {
        this.extraResources = extraResources;
    }

    @Override
    protected void before() throws Throwable {
        LOG.info("Starting HBase cluster");

        // Start Zookeeper mock server
        TestingServer zk = new TestingServer(2181, true);

        // Start an HBase cluster instance with one master and one regionserver
        configuration = createConfiguration(zk);
        util = new HBaseTestingUtility(configuration);
        util.cleanupTestDir();
        cluster = util.startMiniHBaseCluster(1, 1);
    }

    @Override
    protected void after() {
        LOG.info("Stopping HBase cluster");

        // Stop Hbase cluster instance
        if (cluster != null) {
            try {
                cluster.shutdown();
                cluster.waitUntilShutDown();
            } catch (IOException e) {
            }

            try {
                util.cleanupTestDir();
            } catch (IOException e) {
            }
        }

        // Stop Zookepper
        if (zk != null) {
            try {
                zk.stop();
                zk.close();
            } catch (IOException e) {
            }
        }
    }

    private Configuration createConfiguration(TestingServer zk) {
        Configuration conf = HBaseConfiguration.create();
        conf.setInt(HConstants.MASTER_PORT, InstanceSpec.getRandomPort());
        conf.setInt(HConstants.REGIONSERVER_PORT, InstanceSpec.getRandomPort());
        conf.setInt(HConstants.MASTER_INFO_PORT, -1);
        conf.setInt(HConstants.REGIONSERVER_INFO_PORT, -1);
        conf.setBoolean(HConstants.REPLICATION_ENABLE_KEY, false);

        conf.setInt(HConstants.ZOOKEEPER_MAX_CLIENT_CNXNS, 80);
        conf.set(HConstants.ZOOKEEPER_QUORUM, zk.getConnectString());
        conf.setInt(HConstants.ZOOKEEPER_CLIENT_PORT, zk.getPort());

        for (String resource : extraResources) {
            String name = new File(resource).getName();
            conf.addResource(getClass().getClassLoader().getResourceAsStream(resource), name);
        }
        return conf;
    }

    public Configuration getHBaseConfiguration() {
        return cluster.getConfiguration();
    }

    public String getZookeeperQuorum() {
        return zk.getConnectString();
    }
}
