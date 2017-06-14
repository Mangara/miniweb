# MiniWeb #

MiniWeb is a full-website minifier that compresses integrated HTML, CSS, and JavaScript while preserving full functionality. It removes HTML classes that aren't referenced by the CSS and optimally shortens the remaining class names based on their frequency. MiniWeb handles HTML minification itself, while relying on [YUI Compressor](http://yui.github.io/yuicompressor/) for CSS and JS minification. It is designed to be used both from the command-line and as a Java library.

### Getting Started ###

From the command-line:

1. Make sure you have a recent version of [Java](https://www.java.com/en/download/).
2. Download [the latest release](https://bitbucket.org/Mangara/miniweb/downloads).
3. Run `java -jar MiniWeb.jar [... html files ...]`.

As a library:

1. Download [the latest release](https://bitbucket.org/Mangara/miniweb/downloads).
2. Add `import miniweb.MiniWeb` to your source code.
3. Call the relevant `MiniWeb.minify(...)` method.

### Configuration ###

* MiniWeb has several command-line options. Run `java -jar MiniWeb.jar --help` to get an overview.
* If you need to exclude specific class names from deletion or munging, add a file called `miniweb.properties` in a directory that contains one of your HTML files, and give it the following text:

        removeUnusedClasses = [true or false]
        dontRemove = [a space-separated list of class names]

        mungeClassNames = [true or false]
        dontMunge = [a space-separated list of class names]

    For example, the following prevents the class names `selected` and `test` from being shortened and prevents `test` from being removed:

        removeUnusedClasses = true
        dontRemove = test

        mungeClassNames = true
        dontMunge = test selected

### Contribution guidelines ###

* [Bug reports](https://bitbucket.org/Mangara/miniweb/issues?status=new&status=open) and pull requests are welcome!
* By submitting a pull request, you assert that you own the rights to any code you contribute and grant me an irrevocable right to publish this code under the current license of this project.

### License ###

MiniWeb is © 2016-2017 [Sander Verdonschot](http://cglab.ca/~sander/) and is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

MiniWeb includes distributable versions of the following libraries and their dependencies:

* **YUI Compressor**, released under the [BSD license](http://opensource.org/licenses/bsd-license.php) with parts under the [Mozilla Public License (MPL)](http://www.mozilla.org/MPL/).
* **JSoup**, released under the [MIT license](https://jsoup.org/license).
* **jStyleParser**, released under the [GNU Lesser General Public License (LGPL), version 3](https://www.gnu.org/licenses/lgpl-3.0.en.html).
* **JCommander**, released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).