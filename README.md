phoenix-hibernate-dialect
=========================

### Description

An Apache Phoenix Hibernate dialect.

### Build


[![Maven Central](https://img.shields.io/maven-central/v/com.ruesga.phoenix/phoenix-hibernate-dialect.svg?maxAge=2592000)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.ruesga.phoenix%22%20AND%20a%3A%22phoenix-hibernate-dialect%22)

The project builds with maven. Just type the next command in the project root directory.

    mvn -Dmaven.test.skip=true clean install

To build the project passing all the test, just create a file called database.properties in the
src/test/resources folder and add the following properties:

```
test.phoenix.dfs.nodenames: HBase zookeepers nodenames in the form <server>:<port>
test.phoenix.dfs.db.path: HBase path in HDFS
```

Then type

    mvn clean test


### Usage

Add the following dependency to your pom.xml.

```xml
    <dependencies>
        ...
        <dependency>
            <groupId>com.ruesga.phoenix</groupId>
            <artifactId>phoenix-hibernate-dialect</artifactId>
            <version>0.0.1</version>
        </dependency>
        ...
    </dependencies>
```

Register the dialect in your persistence unit in the persistence.xml file.

```xml
    <persistence-unit name="jpa" transaction-type="RESOURCE_LOCAL">
        <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
        <properties>
            ...
            <property name="javax.persistence.jdbc.driver" value="org.apache.phoenix.jdbc.PhoenixDriver" />
            <property name="javax.persistence.jdbc.url" value="jdbc:phoenix:<server:ip>:<path>" />
            <property name="javax.persistence.jdbc.user" value="" />
            <property name="javax.persistence.jdbc.password" value="" />
            <property name="hibernate.dialect" value="com.ruesga.phoenix.dialect.PhoenixDialect" />
            ...
        </properties>
    </persistence-unit>
```

If you want to use indexes, just add the following properties to your client and server
hbase-site.xml configuration.

```xml
    <configuration>
        ...
        <property>
            <name>phoenix.schema.isNamespaceMappingEnabled</name>
            <value>true</value>
        </property>
        <property>
            <name>hbase.regionserver.wal.codec</name>
            <value>org.apache.hadoop.hbase.regionserver.wal.IndexedWALEditCodec</value>
        </property>
        ...
    </configuration>
```

### Want to contribute?

Just file new issues and features or send pull requests.

### Licenses

This source was released under the terms of [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) license.

```
Copyright (C) 2017 Jorge Ruesga

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
