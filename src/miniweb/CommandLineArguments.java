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

import com.beust.jcommander.Parameter;
import java.util.ArrayList;
import java.util.List;

public class CommandLineArguments {

    // Standard GNU options
    @Parameter(names = {"-?", "--help"}, description = "Display this usage information", help = true)
    private boolean help = false;
    @Parameter(names = {"-v", "-V", "--version"}, description = "Display version information")
    private boolean version = false;

    // Files
    @Parameter(description = "HTML files", required = true)
    private List<String> htmlFiles = new ArrayList<>();

    // Options
    @Parameter(names = {"-r", "--replace"}, description = "Overwrite input files with minified output.")
    private boolean replace = false;
    @Parameter(names = {"--inputdir"}, description = "Directory containing all input files. Must be used together with --outputdir.")
    private String inputdir = null;
    @Parameter(names = {"--outputdir"}, description = "Directory to write the minified output files to. Each output file is placed at the same relative location to this directory as the input file is with respect to the input directory.")
    private String outputdir = null;
    @Parameter(names = {"-m", "--dontmunge"}, description = "Do not shorten CSS class names.")
    private boolean dontMunge = false;
    @Parameter(names = {"-k", "--dontremove"}, description = "Do not remove CSS classes from HTML elements.")
    private boolean dontRemove = false;

    public boolean isHelp() {
        return help;
    }

    public boolean isVersion() {
        return version;
    }

    public List<String> getHtmlFiles() {
        return htmlFiles;
    }

    public boolean isReplace() {
        return replace;
    }

    public String getInputdir() {
        return inputdir;
    }

    public String getOutputdir() {
        return outputdir;
    }

    public boolean isDontMunge() {
        return dontMunge;
    }

    public boolean isDontRemove() {
        return dontRemove;
    }
}
