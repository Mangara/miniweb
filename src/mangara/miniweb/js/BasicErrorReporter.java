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

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

public class BasicErrorReporter implements ErrorReporter {

    private final String location;

    public BasicErrorReporter(String location) {
        this.location = location;
    }
    
    @Override
    public void warning(String message, String sourceName,
            int line, String lineSource, int lineOffset) {
        // Ignore
    }

    @Override
    public void error(String message, String sourceName,
            int line, String lineSource, int lineOffset) {
        System.err.println("[ERROR] in " + location);
        if (line < 0) {
            System.err.println("  " + message);
        } else {
            System.err.println("  " + line + ':' + lineOffset + ':' + message);
        }
    }

    @Override
    public EvaluatorException runtimeError(String message, String sourceName,
            int line, String lineSource, int lineOffset) {
        error(message, sourceName, line, lineSource, lineOffset);
        return new EvaluatorException(message);
    }
}
