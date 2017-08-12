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

import org.junit.Assert;
import org.junit.Test;

public class QueryUtilsTest {

    @Test
    public void testRemoveQueryComments() {
        // TODO Ignore hints
        final String QUERY = "/*this is a \n" +
                "multiline comment\n" +
                "*/ select /*+ INDEX(myTable myIndex) */* \n" +
                "from -- a single comment\n" +
                "dual /* another multiline inline comment */\n" +
                "-- order by;";
        final String EXPECTED = "select * from dual";
        final String result = QueryUtils.removeQueryComments(QUERY).replaceAll("\n", "").trim();
        Assert.assertEquals(EXPECTED, result);
    }
}
