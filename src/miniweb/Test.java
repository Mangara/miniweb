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
package miniweb;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {
    private static final String testFolder;
    private static final List<String> testFiles;

    static {
        //testFolder = "CG-Publy"; testFiles = Arrays.asList("CG-Lab.html");
        testFolder = "PersonalWebsite";
        testFiles = Arrays.asList("index.html", "misc.html", "oldaddress.html", "publications.html", "teaching.html");
        //testFolder = "ColorZebra"; testFiles = Arrays.asList("index.html");
    }

    private static final Path inputDir = Paths.get("testInputs/" + testFolder);
    private static final Path outputDir = Paths.get("testOutputs");

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        List<Path> inputs = new ArrayList<>();

        for (String testFile : testFiles) {
            inputs.add(inputDir.resolve(testFile));
        }

        MiniWeb.minify(inputs, inputDir, outputDir);
    }
}
