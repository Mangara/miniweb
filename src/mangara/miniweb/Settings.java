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
package mangara.miniweb;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public class Settings {

    private boolean removeUnusedClasses = true;
    private final Set<String> dontRemove = new LinkedHashSet<>();

    private boolean mungeClassNames = true;
    private final Set<String> dontMunge = new LinkedHashSet<>();

    public boolean isRemoveUnusedClasses() {
        return removeUnusedClasses;
    }

    public void setRemoveUnusedClasses(boolean removeUnusedClasses) {
        this.removeUnusedClasses = removeUnusedClasses;
    }

    public Set<String> getDontRemove() {
        return new LinkedHashSet<>(dontRemove);
    }

    public void setDontRemove(Collection<? extends String> dontRemove) {
        this.dontRemove.clear();
        this.dontRemove.addAll(dontRemove);
    }

    public boolean isMungeClassNames() {
        return mungeClassNames;
    }

    public void setMungeClassNames(boolean mungeClassNames) {
        this.mungeClassNames = mungeClassNames;
    }

    public Set<String> getDontMunge() {
        return new LinkedHashSet<>(dontMunge);
    }

    public void setDontMunge(Collection<? extends String> dontMunge) {
        this.dontMunge.clear();
        this.dontMunge.addAll(dontMunge);
    }

    /**
     * Attempts to parse the given settings file. Returns null in case of an
     * error.
     *
     * @param settingsFile
     * @return
     */
    public static Settings parse(Path settingsFile) {
        Settings settings = new Settings();
        Properties props = new Properties();

        try (BufferedReader in = Files.newBufferedReader(settingsFile)) {
            props.load(in);
        } catch (IOException ex) {
            System.err.println("Could not parse settings file at \"" + settingsFile + "\": " + ex.getLocalizedMessage());
            return null;
        }

        // remove
        settings.setRemoveUnusedClasses(Boolean.parseBoolean(props.getProperty("removeUnusedClasses", Boolean.toString(settings.removeUnusedClasses))));
        settings.setDontRemove(Arrays.asList(props.getProperty("dontRemove", "").split("\\s+")));

        // munge
        settings.setMungeClassNames(Boolean.parseBoolean(props.getProperty("mungeClassNames", Boolean.toString(settings.mungeClassNames))));
        settings.setDontMunge(Arrays.asList(props.getProperty("dontMunge", "").split("\\s+")));

        return settings;
    }

    @Override
    public String toString() {
        return "Settings{" + "removeUnusedClasses=" + removeUnusedClasses + ", dontRemove=" + dontRemove + ", mungeClassNames=" + mungeClassNames + ", dontMunge=" + dontMunge + '}';
    }
}
