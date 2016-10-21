/*
 * Copyright 2016 Sander Verdonschot <sander.verdonschot at gmail.com>.
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
package mangara.miniweb.js;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSClassRenamer {

    private static final Pattern classNamePattern = Pattern.compile("\\.-?[_a-zA-Z]+[_a-zA-Z0-9-]*");

    public static String renameHTMLClasses(String js, Map<String, String> compressedClassNames, String jsFileName) {
        boolean[] inStringLiteral = buildStringLiteralArray(js);

        StringBuffer result = new StringBuffer(js.length());
        Matcher matcher = classNamePattern.matcher(js);

        while (matcher.find()) {
            String classname = matcher.group().substring(1);

            if (inStringLiteral[matcher.start()] && compressedClassNames.containsKey(classname)) {
                matcher.appendReplacement(result, '.' + compressedClassNames.get(classname));
            } else {
                matcher.appendReplacement(result, matcher.group());
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private static final char NOT_IN_STRING = 0;

    private static boolean[] buildStringLiteralArray(String js) {
        boolean[] inString = new boolean[js.length()];
        boolean escape = false;
        char stringOpen = NOT_IN_STRING;

        for (int i = 0; i < js.length(); i++) {
            char c = js.charAt(i);

            if (escape) {
                escape = false;
            } else {
                switch (c) {
                    case '"': // fallthrough
                    case '\'':
                        if (stringOpen == NOT_IN_STRING) {
                            stringOpen = c;
                        } else if (stringOpen == c) {
                            stringOpen = NOT_IN_STRING;
                        }
                        break;
                    case '\\':
                        escape = true;
                        break;
                }
            }

            inString[i] = (stringOpen != NOT_IN_STRING);
        }

        return inString;
    }
}
