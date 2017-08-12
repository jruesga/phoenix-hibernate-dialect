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

import java.sql.Types;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import de.invesdwin.instrument.DynamicInstrumentationLoader;

public class PhoenixDialect extends Dialect {

    public static final String HINT_SECONDARY_INDEX = "phoenix.secondary.index";

    public static class SecondaryIndexHint {
        private final String table;
        private final String index;

        public SecondaryIndexHint(String table, String index) {
            this.table = table;
            this.index = index;
        }

        public SecondaryIndexHint(Class<?> annotated, String index) {
            javax.persistence.Table table = (javax.persistence.Table) annotated.getAnnotation(
                    javax.persistence.Table.class);
            String name = table.name();
            String schema = table.schema();
            this.table = (schema != null ? schema + "." : "") + name;
            this.index = index;
        }

        public String build() {
            return "/*+ INDEX(" + table + " " + index + ") */";
        }
    }

    @SuppressWarnings("unused")
    private static ClassPathXmlApplicationContext ctx;
    static {
        DynamicInstrumentationLoader.waitForInitialized();
        DynamicInstrumentationLoader.initLoadTimeWeavingContext();
        ctx = new ClassPathXmlApplicationContext("/META-INF/phoenix-spring-context.xml");
    }

    public PhoenixDialect() {
        super();

        registerColumnType(Types.BIT, "boolean");
        registerColumnType(Types.BIGINT, "bigint");
        registerColumnType(Types.SMALLINT, "smallint");
        registerColumnType(Types.TINYINT, "tinyint");
        registerColumnType(Types.INTEGER, "integer");
        registerColumnType(Types.FLOAT, "float");
        registerColumnType(Types.DOUBLE, "double");
        registerColumnType(Types.NUMERIC, "decimal($p,$s)");
        registerColumnType(Types.DECIMAL, "decimal($p,$s)");
        registerColumnType(Types.DATE, "date");
        registerColumnType(Types.TIME, "time");
        registerColumnType(Types.TIMESTAMP, "timestamp");
        registerColumnType(Types.BOOLEAN, "boolean");
        registerColumnType(Types.VARCHAR, 255, "varchar($l)");
        registerColumnType(Types.CHAR, "char(1)");
        registerColumnType(Types.BINARY, "binary($l)" );
        registerColumnType(Types.VARBINARY, "varbinary");
        registerColumnType(Types.ARRAY, "array[$l]");

        // TODO Add Phoenix functions (https://phoenix.apache.org/language/functions.html)
        registerFunction("current_date", new NoArgSQLFunction("current_date", StandardBasicTypes.TIMESTAMP, true));
        registerFunction("current_time", new NoArgSQLFunction("current_time", StandardBasicTypes.TIME, true));
    }


    // lock acquisition support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public boolean supportsLockTimeouts() {
        return false;
    }

    @Override
    public boolean supportsOuterJoinForUpdate() {
        return false;
    }


     // limit/offset support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public LimitHandler getLimitHandler() {
        return new AbstractLimitHandler() {
            @Override
            public boolean supportsLimit() {
                return true;
            }
        };
    }


    // current timestamp support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public boolean supportsCurrentTimestampSelection() {
        return true;
    }

    @Override
    public boolean isCurrentTimestampSelectStringCallable() {
        return false;
    }

    @Override
    public String getCurrentTimestampSelectString() {
        return "select current_date()";
    }

    @Override
    public String getCurrentTimestampSQLFunctionName() {
        return "current_date";
    }


    // union subclass support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public boolean supportsUnionAll() {
        return true;
    }



    // DDL support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public boolean supportsIfExistsBeforeTableName() {
        return true;
    }

    @Override
    public String[] getDropSchemaCommand(String schemaName) {
        return new String[] {"drop schema if exists " + schemaName};
    }

    @Override
    public String getAddColumnString() {
        return " add ";
    }

    @Override
    public boolean supportsColumnCheck() {
        return false;
    }

    @Override
    public boolean supportsTableCheck() {
        return false;
    }

    @Override
    public boolean supportsEmptyInList() {
        return false;
    }

    @Override
    public boolean supportsRowValueConstructorSyntax() {
        return true;
    }

    @Override
    public boolean supportsRowValueConstructorSyntaxInInList() {
        return true;
    }

    @Override
    public boolean supportsBindAsCallableArgument() {
        return false;
    }

    @Override
    public boolean supportsTupleDistinctCounts() {
        return false;
    }

    @Override
    public String getQueryHintString(String query, List<String> hints) {
        return QueryUtils.removeQueryComments(query).trim().replaceFirst(
                "select", "select " + StringUtils.join(hints, " ") + " ");
    }

    @Override
    public NameQualifierSupport getNameQualifierSupport() {
        return NameQualifierSupport.SCHEMA;
    }

    @Override
    public boolean hasAlterTable() {
        return false;
    }

    @Override
    public boolean dropConstraints() {
        return false;
    }

    @Override
    public UniqueDelegate getUniqueDelegate() {
        return new UniqueDelegate() {
            @Override
            public String getTableCreationUniqueConstraintsFragment(Table table) {
                return "";
            }

            @Override
            public String getColumnDefinitionUniquenessFragment(Column column) {
                return "";
            }

            @Override
            public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata) {
                final JdbcEnvironment jdbcEnvironment = metadata.getDatabase().getJdbcEnvironment();

                final String tableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
                        uniqueKey.getTable().getQualifiedTableName(), PhoenixDialect.this);

                final String constraintName = PhoenixDialect.this.quote( uniqueKey.getName() );

                return "drop index if exists " + constraintName + " on " + tableName;
            }

            @Override
            public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata) {
                final JdbcEnvironment jdbcEnvironment = metadata.getDatabase().getJdbcEnvironment();

                final String tableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
                        uniqueKey.getTable().getQualifiedTableName(), PhoenixDialect.this);

                final String constraintName = PhoenixDialect.this.quote( uniqueKey.getName() );

                final StringBuilder columns = new StringBuilder();
                final Iterator<org.hibernate.mapping.Column> columnIterator = uniqueKey.columnIterator();
                while ( columnIterator.hasNext() ) {
                    final org.hibernate.mapping.Column column = columnIterator.next();
                    columns.append(column.getQuotedName(PhoenixDialect.this));
                    if (uniqueKey.getColumnOrderMap().containsKey(column)) {
                        columns.append(" ").append(uniqueKey.getColumnOrderMap().get(column));
                    }
                    if (columnIterator.hasNext()) {
                        columns.append(", ");
                    }
                }

                return "create index " + constraintName + " on " + tableName
                        + " (" + columns.toString() + ")";
            }
        };
    }

}
