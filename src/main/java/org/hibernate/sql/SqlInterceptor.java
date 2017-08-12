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

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.dialect.Dialect;

import com.ruesga.phoenix.dialect.PhoenixDialect;

@Aspect
public class SqlInterceptor {

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
        }
        return statement;
    }

    private Dialect getDialect(Object target) {
        if (target instanceof Insert) {
            return ((Insert) target).getDialect();
        }
        if (target instanceof InsertSelect) {
            return ((Insert) target).getDialect();
        }
        return null;
    }
}
