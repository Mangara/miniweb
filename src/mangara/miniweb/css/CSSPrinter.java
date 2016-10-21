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
package mangara.miniweb.css;

import cz.vutbr.web.css.RuleBlock;
import cz.vutbr.web.css.StyleSheet;

public class CSSPrinter {

    public static String toString(StyleSheet stylesheet) {
        StringBuilder sb = new StringBuilder();
        
        for (RuleBlock<?> rules : stylesheet) {
            String css = rules.toString();
            css = css.replaceAll(".0%", "%"); // Work-around for a bug in YUI Compressor
            css = css.replaceAll(" 0s", " 0.00s"); // Work-around for a bug in YUI Compressor
            css = css.replaceAll(" 0ms", " 0.00ms"); // Work-around for a bug in YUI Compressor
            sb.append(css);
        }
        
        return sb.toString();
    }
}
