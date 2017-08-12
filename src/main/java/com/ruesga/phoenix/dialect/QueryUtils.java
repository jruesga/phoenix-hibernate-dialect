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

import java.util.regex.Pattern;

public final class QueryUtils {
    // TODO Ignore hints
    private static final Pattern MULTILINE_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern SINGLE_LINE_COMMENT = Pattern.compile("-- (.*)?$", Pattern.MULTILINE);

    public static final String removeQueryComments(String query) {
        return SINGLE_LINE_COMMENT.matcher(MULTILINE_COMMENT.matcher(query).replaceAll("")).replaceAll("");
    }
}
