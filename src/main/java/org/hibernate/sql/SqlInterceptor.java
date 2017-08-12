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
package org.hibernate.sql;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.dialect.Dialect;

import com.ruesga.phoenix.dialect.PhoenixDialect;

@Aspect
public class SqlInterceptor {

    private Field updateDialectField = null;
    private Field updateColumnsField = null;
    private Field updatePkColumnsField = null;
    private Field updateCommentField = null;
    private Field updateWhereColumnsField = null;

    public SqlInterceptor() {
        try {
            updateColumnsField = Update.class.getDeclaredField("columns");
            updateColumnsField.setAccessible(true);
            updatePkColumnsField = Update.class.getDeclaredField("primaryKeyColumns");
            updatePkColumnsField.setAccessible(true);
            updateWhereColumnsField = Update.class.getDeclaredField("whereColumns");
            updateWhereColumnsField.setAccessible(true);
            updateCommentField = Update.class.getDeclaredField("comment");
            updateCommentField.setAccessible(true);
            updateDialectField = Update.class.getDeclaredField("dialect");
            updateDialectField.setAccessible(true);
        } catch (Exception e) {;
        }
    }

    @Around("execution(java.lang.String org.hibernate.sql.*.toStatementString(..))")
    public String toStatementStringAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Dialect dialect = getDialect(joinPoint.getTarget());
        if (!(dialect instanceof PhoenixDialect)) {
            // Nothing to deal with
            return (String) joinPoint.proceed();
        }

        String statement = (String) joinPoint.proceed();
        if (joinPoint.getTarget() instanceof Insert || joinPoint.getTarget() instanceof InsertSelect) {
            return statement.replaceFirst("insert into", "upsert into");
        } else if (joinPoint.getTarget() instanceof Update) {
            return createUpsertValues((Update) joinPoint.getTarget());
        }
        return statement;
    }

    private Dialect getDialect(Object target) throws Throwable {
        if (target instanceof Insert) {
            return ((Insert) target).getDialect();
        }
        if (target instanceof InsertSelect) {
            return ((Insert) target).getDialect();
        }
        if (target instanceof Update && updateDialectField != null) {
            return (Dialect) updateDialectField.get(target);
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private String createUpsertValues(Update target) throws Throwable {
        StringBuilder sb = new StringBuilder();

        String comment = (String) updateCommentField.get(target);
        if (comment!=null) {
            sb.append("/* ").append(comment).append(" */ ");
        }

        sb.append("upsert into ")
            .append(target.getTableName())
            .append(" (");
        Map columns = (Map) updateColumnsField.get(target);
        Map pkColumns = (Map) updatePkColumnsField.get(target);
        Map whereColumns = (Map) updateWhereColumnsField.get(target);

        // Set columns before pkcolumns and where columns to match parameter binding
        StringBuilder names = new StringBuilder();
        StringBuilder values = new StringBuilder();
        updateUpsertBuffers(names, values, columns, false);
        // TODO handle assignments
        updateUpsertBuffers(names, values, pkColumns, true);
        updateUpsertBuffers(names, values, whereColumns, true);

        sb.append(names).append(") values (").append(values).append(")");
        return sb.toString();
    }

    @SuppressWarnings("rawtypes")
    private void updateUpsertBuffers(StringBuilder names, StringBuilder values, Map columns, boolean hasPrevColumns) {
        if (columns != null) {
            Iterator it = columns.entrySet().iterator();
            if (hasPrevColumns && it.hasNext()) {
                names.append(",");
                values.append(",");
            }
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                boolean hasNext = it.hasNext();
                names.append(e.getKey()).append(hasNext ? "," : "");
                String value = String.valueOf(e.getValue());
                if (value.startsWith("=")) {
                    value = value.substring(1);
                }
                values.append(value).append(hasNext ? "," : "");
            }
        }
    }
}
