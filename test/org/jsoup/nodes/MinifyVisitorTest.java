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
package org.jsoup.nodes;

import java.util.Arrays;
import org.jsoup.Jsoup;
import org.junit.Test;
import static org.junit.Assert.*;

public class MinifyVisitorTest {

    public MinifyVisitorTest() {
    }

    private void testPage(String input, String expected) {
        assertEquals(expected,
                MinifyVisitor.minify(Jsoup.parse(input)));
    }

    private void testBodySnippet(String input, String expected) {
        Document doc = Jsoup.parse("<html><head></head><body>" + input + "</body></html>");
        String result = MinifyVisitor.minify(doc);
        assertEquals("<html><head></head><body>" + expected + "</body></html>", result);
    }

    private void testHeadSnippet(String input, String expected) {
        Document doc = Jsoup.parse("<html>" + input + "<body></body></html>");
        String result = MinifyVisitor.minify(doc);
        assertEquals("<html>" + expected + "<body></body></html>", result);
    }

    @Test
    public void testMinifyExamplePage() {
        testPage("<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "<meta charset=\"UTF-8\">\n"
                + "<title>Title of the document</title>\n"
                + "</head>\n"
                + "\n"
                + "<body>\n"
                + "Content of the document......\n"
                + "</body>\n"
                + "\n"
                + "</html> ",
                "<!doctype html><html><head><meta charset=UTF-8><title>Title of the document</title></head> <body>Content of the document......</body></html>");
    }

    @Test
    public void testBasicWhitespaceReduction() {
        // Basic white-space reduction between same-level elements
        testBodySnippet("<span>1</span> <span>2</span>", "<span>1</span> <span>2</span>");
        testBodySnippet("<span>1</span>       <span>2</span>", "<span>1</span> <span>2</span>");
        testBodySnippet("   <span>1</span>\n   <span>2</span>   ", "<span>1</span> <span>2</span>");
        testBodySnippet("<span>1</span><span>2</span>", "<span>1</span><span>2</span>");
        testBodySnippet("<span>1 </span><span>2</span>", "<span>1 </span><span>2</span>");

        // Basic white-space reduction between nested elements
        testBodySnippet("<div><span>1</span></div>", "<div><span>1</span></div>");
        testBodySnippet("<div>\n<span>1</span>\n</div>", "<div><span>1</span></div>");
        testBodySnippet("   <div>\n     <span>1</span>\n   </div>", "<div><span>1</span></div>");
        testBodySnippet("<div> <span>1</span> </div>", "<div><span>1</span></div>");
    }

    @Test
    public void testCaseNormalization() {
        testBodySnippet("<P>foo</p>", "<p>foo</p>");
        testBodySnippet("<DIV>boo</DIV>", "<div>boo</div>");
        testBodySnippet("<DIV title=\"moo\">boo</DiV>", "<div title=moo>boo</div>");
        testBodySnippet("<DIV TITLE=\"blah\">boo</DIV>", "<div title=blah>boo</div>");
        testBodySnippet("<DIV tItLe=\"blah\">boo</DIV>", "<div title=blah>boo</div>");
        testBodySnippet("<DiV tItLe=\"blah\">boo</DIV>", "<div title=blah>boo</div>");
    }

    @Test
    public void testSpaceNormalizationBetweenAttributes() {
        testBodySnippet("<p title=\"bar\">foo</p>", "<p title=bar>foo</p>");
        testBodySnippet("<img src=\"test\"/>", "<img src=test>");
        testBodySnippet("<p title = \"bar\">foo</p>", "<p title=bar>foo</p>");
        testBodySnippet("<p title\n\n\t  =\n     \"bar\">foo</p>", "<p title=bar>foo</p>");
        testBodySnippet("<img src=\"test\" \n\t />", "<img src=test>");
        testBodySnippet("<input title=\"bar\"       id=\"boo\"    value=\"hello world\">", "<input title=bar id=boo value=\"hello world\">");
    }

    @Test
    public void testSpaceNormalizationAroundText() {
        testBodySnippet("   <p>blah</p>\n\n\n   ", "<p>blah</p>");

        Arrays.asList("a", "b", "big", "button", "code", "em", "font", "i", "kbd", "mark", "q", "s", "small", "span", "strike", "strong", "sub", "sup", "time", "tt", "u")
                .stream().forEach(el -> {
                    testBodySnippet("<p>foo <" + el + ">baz</" + el + "> bar</p>", "<p>foo <" + el + ">baz</" + el + "> bar</p>");
                    testBodySnippet("<p>foo<" + el + ">baz</" + el + ">bar</p>", "<p>foo<" + el + ">baz</" + el + ">bar</p>");
                    testBodySnippet("<p>foo <" + el + ">baz</" + el + ">bar</p>", "<p>foo <" + el + ">baz</" + el + ">bar</p>");
                    testBodySnippet("<p>foo<" + el + ">baz</" + el + "> bar</p>", "<p>foo<" + el + ">baz</" + el + "> bar</p>");
                    testBodySnippet("<p>foo <" + el + "> baz </" + el + "> bar</p>", "<p>foo <" + el + ">baz</" + el + "> bar</p>");
                    testBodySnippet("<p>foo<" + el + "> baz </" + el + ">bar</p>", "<p>foo<" + el + ">baz</" + el + ">bar</p>");
                    testBodySnippet("<p>foo <" + el + "> baz </" + el + ">bar</p>", "<p>foo <" + el + ">baz</" + el + ">bar</p>");
                    testBodySnippet("<p>foo<" + el + "> baz </" + el + "> bar</p>", "<p>foo<" + el + ">baz</" + el + "> bar</p>");
                });

        testBodySnippet("<p>foo <img> bar</p>", "<p>foo <img> bar</p>");
        testBodySnippet("<p>foo<img>bar</p>", "<p>foo<img>bar</p>");
        testBodySnippet("<p>foo <img>bar</p>", "<p>foo <img>bar</p>");
        testBodySnippet("<p>foo<img> bar</p>", "<p>foo<img> bar</p>");
    }

    @Test
    public void testRemovingComments() {
        testBodySnippet("<!-- test -->", "");
        testBodySnippet("<!-- foo --><div>baz</div><!-- bar\n\n moo -->", "<div>baz</div>");
        testBodySnippet("<p title=\"<!-- comment in attribute -->\">foo</p>", "<p title=\"<!-- comment in attribute -->\">foo</p>");
        testBodySnippet("<script><!-- alert(1) --></script>", "<script><!-- alert(1) --></script>");
        testBodySnippet("<STYLE><!-- alert(1) --></STYLE>", "<style><!-- alert(1) --></style>");
    }

    @Test
    public void testRemovingEmptyAttributes() {
        testBodySnippet("<p id=\"\" class=\"\" STYLE=\" \" title=\"\n\" lang=\"\" dir=\"\">x</p>", "<p title=\"\n\">x</p>");
        testBodySnippet("<p onclick=\"\"   ondblclick=\" \" onmousedown=\"\" ONMOUSEUP=\"\" onmouseover=\" \" onmousemove=\"\" onmouseout=\"\" onkeypress=\n\n  \"\n     \" onkeydown=\n\"\" onkeyup\n=\"\">x</p>", "<p>x</p>");
        testBodySnippet("<input onfocus=\"\" onblur=\"\" onchange=\" \" value=\" boo \">", "<input value=\" boo \">");
        testBodySnippet("<input value=\"\" name=\"foo\">", "<input name=foo>");
        testBodySnippet("<input value=\" \" name=\"foo\">", "<input value=\" \" name=foo>");

        // Preserve image src and alt attributes
        testBodySnippet("<img src=\"\" alt=\"\">", "<img src alt>");
        testBodySnippet("<img src alt>", "<img src alt>");

        // Preserve unrecognized attributes
        testBodySnippet("<div data-foo class id style title lang dir onfocus onblur onchange onclick ondblclick onmousedown onmouseup onmouseover onmousemove onmouseout onkeypress onkeydown onkeyup></div>", "<div data-foo title></div>");
    }

    @Test
    public void testCleanClassAttributes() {
        String input = "<p class=\" foo bar  \">foo bar baz</p>";
        testBodySnippet(input, "<p class=\"foo bar\">foo bar baz</p>");

        input = "<p class=\" foo      \">foo bar baz</p>";
        testBodySnippet(input, "<p class=foo>foo bar baz</p>");

        input = "<p class=\"\n  \n foo   \n\n\t  \t\n   \">foo bar baz</p>";
        String output = "<p class=foo>foo bar baz</p>";
        testBodySnippet(input, output);

        input = "<p class=\"\n  \n foo   \n\n\t  \t\n  class1 class-23 \">foo bar baz</p>";
        output = "<p class=\"foo class1 class-23\">foo bar baz</p>";
        testBodySnippet(input, output);
    }

    @Test
    public void testCleanStyleAttributes() {
        String input = "<p style=\"    color: red; background-color: rgb(100, 75, 200);  \"></p>";
        String output = "<p style=\"color: red; background-color: rgb(100, 75, 200)\"></p>";
        testBodySnippet(input, output);

        input = "<p style=\"font-weight: bold  ; \"></p>";
        output = "<p style=\"font-weight: bold\"></p>";
        testBodySnippet(input, output);
    }

    @Test
    public void testCleanURIAttributes() {
        String input = "<a href=\"   http://example.com  \">x</a>";
        String output = "<a href=http://example.com>x</a>";
        testBodySnippet(input, output);

        input = "<a href=\"  \t\t  \n \t  \">x</a>";
        output = "<a href>x</a>";
        testBodySnippet(input, output);

        input = "<img src=\"   http://example.com  \" title=\"bleh   \" longdesc=\"  http://example.com/longdesc \n\n   \t \">";
        output = "<img src=http://example.com title=\"bleh   \" longdesc=http://example.com/longdesc>";
        testBodySnippet(input, output);

        input = "<img src=\"\" usemap=\"   http://example.com  \">";
        output = "<img src usemap=http://example.com>";
        testBodySnippet(input, output);

        input = "<form action=\"  somePath/someSubPath/someAction?foo=bar&baz=qux     \"></form>";
        output = "<form action=\"somePath/someSubPath/someAction?foo=bar&baz=qux\"></form>";
        testBodySnippet(input, output);

        input = "<BLOCKQUOTE cite=\" \n\n\n http://www.mycom.com/tolkien/twotowers.html     \"><P>foobar</P></BLOCKQUOTE>";
        output = "<blockquote cite=http://www.mycom.com/tolkien/twotowers.html><p>foobar</p></blockquote>";
        testBodySnippet(input, output);

        input = "<head profile=\"       http://gmpg.org/xfn/11    \"></head>";
        output = "<head profile=http://gmpg.org/xfn/11></head>";
        testHeadSnippet(input, output);

        input = "<object codebase=\"   http://example.com  \"></object>";
        output = "<object codebase=http://example.com></object>";
        testBodySnippet(input, output);

        /* I don't know why these would be different.        
         input = "<span profile=\"   1, 2, 3  \">foo</span>";
         testBodySnippet(input, input);

         input = "<div action=\"  foo-bar-baz \">blah</div>";
         testBodySnippet(input, input);*/
    }

    @Test
    public void testEscapeAmbiguousAmpersands() {
        String input = "<form action=\"  somePath/someSubPath/someAction?foo=bar&baz=qux     \"></form>";
        String output = "<form action=\"somePath/someSubPath/someAction?foo=bar&baz=qux\"></form>";
        testBodySnippet(input, output);

        input = "<form action=\"  somePath/someSubPath/someAction?foo=bar&baz;=qux     \"></form>";
        output = "<form action=\"somePath/someSubPath/someAction?foo=bar&amp;baz;=qux\"></form>";
        testBodySnippet(input, output);
    }

    @Test
    public void testCleanNumberAttributes() {
        String input = "<a href=\"#\" tabindex=\"   1  \">x</a><button tabindex=\"   2  \">y</button>";
        String output = "<a href=# tabindex=1>x</a><button tabindex=2>y</button>";
        testBodySnippet(input, output); // { cleanAttributes: true }

        input = "<input value=\"\" maxlength=\"     5 \">";
        output = "<input maxlength=5>";
        testBodySnippet(input, output); // { cleanAttributes: true }

        input = "<select size=\"  10   \t\t \"><option>x</option></select>";
        output = "<select size=10><option>x</option></select>";
        testBodySnippet(input, output); // { cleanAttributes: true }

        input = "<textarea rows=\"   20  \" cols=\"  30      \"></textarea>";
        output = "<textarea rows=20 cols=30></textarea>";
        testBodySnippet(input, output); // { cleanAttributes: true }

        input = "<TABLE><COLGROUP><COL span=\"  39 \"></COLGROUP></TABLE>";
        output = "<table><colgroup><col span=39></colgroup></table>";
        testBodySnippet(input, output); // { cleanAttributes: true }

        input = "<TABLE><COLGROUP span=\"   40  \"></COLGROUP></TABLE>";
        output = "<table><colgroup span=40></colgroup></table>";
        testBodySnippet(input, output); // { cleanAttributes: true }

        input = "<table><tr><td colspan=\"    2   \">x</td><td rowspan=\"   3 \"></td></tr></table>";
        output = "<table><tbody><tr><td colspan=2>x</td><td rowspan=3></td></tr></tbody></table>";
        testBodySnippet(input, output); // { cleanAttributes: true }
    }

    @Test
    public void testCleanOtherAttributes() {
        String input = "<a href=\"#\" onclick=\"  window.prompt(\'boo\'); \" onmouseover=\" \n\n alert(123)  \t \n\t  \">blah</a>";
        String output = "<a href=# onclick=\"window.prompt(\'boo\')\" onmouseover=alert(123)>blah</a>";
        testBodySnippet(input, output); // { cleanAttributes: true }

        input = "<div onload=\"  foo();   bar() ;  \"><p>x</div>";
        output = "<div onload=\"foo();   bar()\"><p>x</p></div>";
        testBodySnippet(input, output); // { cleanAttributes: true }
    }

    @Test
    public void testRemovingRedundantFormMethod() {
        String input = "<form method=\"get\">hello world</form>";
        testBodySnippet(input, "<form>hello world</form>"); // { removeRedundantAttributes: true }

        input = "<form method=\"post\">hello world</form>";
        testBodySnippet(input, "<form method=post>hello world</form>"); // { removeRedundantAttributes: true }
    }

    @Test
    public void testRemovingRedundantInputTupe() {
        String input = "<input type=\"text\">";
        testBodySnippet(input, "<input>"); // { removeRedundantAttributes: true }

        input = "<input type=\"  TEXT  \" value=\"foo\">";
        testBodySnippet(input, "<input value=foo>"); // { removeRedundantAttributes: true }

        input = "<input type=\"checkbox\">";
        testBodySnippet(input, "<input type=checkbox>"); // { removeRedundantAttributes: true }
    }

    @Test
    public void testRemovingRedundantAnchorNameAndID() {
        String input = "<a id=\"foo\" name=\"foo\">blah</a>";
        String output = "<a id=foo>blah</a>";
        testBodySnippet(input, output); // { removeRedundantAttributes: true }

        input = "<input id=\"foo\" name=\"foo\">";
        output = "<input id=foo name=foo>";
        testBodySnippet(input, output); // { removeRedundantAttributes: true }

        input = "<a name=\"foo\">blah</a>";
        output = "<a name=foo>blah</a>";
        testBodySnippet(input, output); // { removeRedundantAttributes: true }

        input = "<a href=\"...\" name=\"  bar  \" id=\"bar\" >blah</a>";
        output = "<a href=... id=bar>blah</a>";
        testBodySnippet(input, output); // { removeRedundantAttributes: true }
    }

    @Test
    public void testRemovingRedundantScriptCharset() {
        String input = "<script type=\"text/javascript\" charset=\"UTF-8\">alert(222);</script>";
        String output = "<script>alert(222)</script>";
        testBodySnippet(input, output); // { removeRedundantAttributes: true }

        input = "<script type=\"text/javascript\" src=\"http://example.com\" charset=\"UTF-8\">alert(222);</script>";
        output = "<script src=http://example.com charset=UTF-8>alert(222);</script>";
        testBodySnippet(input, output); // { removeRedundantAttributes: true }

        input = "<script CHARSET=\" ... \">alert(222);</script>";
        output = "<script>alert(222)</script>";
        testBodySnippet(input, output); // { removeRedundantAttributes: true }
    }

    @Test
    public void testRemovingRedundantScriptLanguage() {
        String input = "<script language=\"Javascript\">x=2,y=4</script>";
        testBodySnippet(input, "<script>x=2,y=4</script>"); // { removeRedundantAttributes: true }

        input = "<script LANGUAGE = \"  javaScript  \">x=2,y=4</script>";
        testBodySnippet(input, "<script>x=2,y=4</script>"); // { removeRedundantAttributes: true }
    }

    @Test
    public void testRemovingRedundantAreaShape() {
        String input = "<area shape=\"rect\" coords=\"696,25,958,47\" href=\"#\" title=\"foo\">";
        String output = "<area coords=696,25,958,47 href=# title=foo>";
        testBodySnippet(input, output); // { removeRedundantAttributes: true }
    }

    @Test
    public void testRemovingRedundantJavascript() {
        String input = "<p onclick=\"javascript:alert(1)\">x</p>";
        testBodySnippet(input, "<p onclick=alert(1)>x</p>"); // { cleanAttributes: true }

        input = "<p onclick=\"javascript:x\">x</p>";
        testBodySnippet(input, "<p onclick=x>x</p>"); // { cleanAttributes: true, removeAttributeQuotes: true }

        input = "<p onclick=\" JavaScript: x\">x</p>";
        testBodySnippet(input, "<p onclick=x>x</p>"); // { cleanAttributes: true }

        input = "<p title=\"javascript:(function() { /* some stuff here */ })()\">x</p>";
        testBodySnippet(input, input); // { cleanAttributes: true }
    }

    @Test
    public void testRemovingRedundantJavascriptType() {
        String input = "<script type=\"text/javascript\">alert(1)</script>";
        String output = "<script>alert(1)</script>";
        testBodySnippet(input, output); // { removeScriptTypeAttributes: true }

        input = "<SCRIPT TYPE=\"  text/javascript \">alert(1)</script>";
        output = "<script>alert(1)</script>";
        testBodySnippet(input, output); // { removeScriptTypeAttributes: true }

        input = "<script type=\"application/javascript;version=1.8\">alert(1)</script>";
        output = "<script type=\"application/javascript;version=1.8\">alert(1)</script>";
        testBodySnippet(input, output); // { removeScriptTypeAttributes: true }

        input = "<script type=\"text/vbscript\">MsgBox(\"foo bar\")</script>";
        output = "<script type=text/vbscript>MsgBox(\"foo bar\")</script>";
        testBodySnippet(input, output); // { removeScriptTypeAttributes: true }
    }

    @Test
    public void testRemovingRedundantCssType() {
        String input = "<style type=\"text/css\">.foo { color: red }</style>";
        String output = "<style>.foo{color:red}</style>";
        testBodySnippet(input, output); // { removeStyleLinkTypeAttributes: true }

        input = "<STYLE TYPE = \"  text/CSS \">body { font-size: 1.75em }</style>";
        output = "<style>body{font-size:1.75em}</style>";
        testBodySnippet(input, output); // { removeStyleLinkTypeAttributes: true }

        input = "<style type=\"text/plain\">.foo { background: green }</style>";
        output = "<style type=text/plain>.foo{background:green}</style>";
        testBodySnippet(input, output); // { removeStyleLinkTypeAttributes: true }

        input = "<link rel=\"stylesheet\" type=\"text/css\" href=\"http://example.com\">";
        output = "<link rel=stylesheet href=http://example.com>";
        testBodySnippet(input, output); // { removeStyleLinkTypeAttributes: true }

        input = "<link rel=\"alternate\" type=\"application/atom+xml\" href=\"data.xml\">";
        output = "<link rel=alternate type=application/atom+xml href=data.xml>";
        testBodySnippet(input, output); // { removeStyleLinkTypeAttributes: true }
    }

    @Test
    public void testRemovingAttributeQuotes() { // removing attribute quotes
        String input = "<p title=\"blah\" class=\"a23B-foo.bar_baz:qux\" id=\"moo\">foo</p>";
        testBodySnippet(input, "<p title=blah class=a23B-foo.bar_baz:qux id=moo>foo</p>");

        input = "<input value=\"hello world\">";
        testBodySnippet(input, "<input value=\"hello world\">");

        input = "<a href=\"#\" title=\"foo#bar\">x</a>";
        testBodySnippet(input, "<a href=# title=foo#bar>x</a>");

        input = "<a href=\"http://example.com/\" title=\"blah\">\nfoo\n\n</a>";
        testBodySnippet(input, "<a href=http://example.com/ title=blah>foo</a>");

        input = "<a title=\"blah\" href=\"http://example.com/\">\nfoo\n\n</a>";
        testBodySnippet(input, "<a title=blah href=http://example.com/ >foo</a>");

        input = "<p class=foo|bar:baz></p>";
        testBodySnippet(input, "<p class=foo|bar:baz></p>");

        input = "<a href=\"http://example.com/\" title=\"\">\nfoo\n\n</a>";
        testBodySnippet(input, "<a href=http://example.com/ title>foo</a>");
    }

    @Test
    public void testCollapsingWhitespace() {
        String input = "<p>foo</p>    <p> bar</p>\n\n   \n\t\t  <div title=\"quz\">baz  </div>";
        String output = "<p>foo</p> <p>bar</p> <div title=quz>baz</div>";
        testBodySnippet(input, output);

        input = "<p> foo    bar</p>";
        output = "<p>foo bar</p>";
        testBodySnippet(input, output);

        input = "<p>foo\nbar</p>";
        output = "<p>foo bar</p>";
        testBodySnippet(input, output);

        input = "<p> <a href=\"#\"> x </a> </p>";
        output = "<p><a href=#>x</a></p>";
        testBodySnippet(input, output);

        input = "<p> <!-- comment --> <a href=\"#\"> x </a> </p>";
        output = "<p><a href=#>x</a></p>";
        testBodySnippet(input, output);

        input = "<p>x <!-- comment --> <a href=\"#\"> x </a> </p>";
        output = "<p>x <a href=#>x</a></p>";
        testBodySnippet(input, output);

        input = "<p>x<!-- comment --> <a href=\"#\"> x </a> </p>";
        output = "<p>x <a href=#>x</a></p>";
        testBodySnippet(input, output);

        input = "<p>x<!-- comment --><a href=\"#\"> x </a> </p>";
        output = "<p>x<a href=#>x</a></p>";
        testBodySnippet(input, output);

        input = "<p> foo    <span>  blah     <i>   22</i>    </span> bar <img src=\"\"></p>";
        output = "<p>foo <span>blah <i>22</i></span> bar <img src></p>";
        testBodySnippet(input, output);

        input = "<textarea> foo bar     baz \n\n   x \t    y </textarea>";
        output = "<textarea> foo bar     baz \n\n   x \t    y </textarea>";
        testBodySnippet(input, output);

        input = "<div><textarea></textarea>    </div>";
        output = "<div><textarea></textarea></div>";
        testBodySnippet(input, output);

        input = "<div><pre> $foo = \"baz\"; </pre>    </div>";
        output = "<div><pre> $foo = \"baz\"; </pre></div>";
        testBodySnippet(input, output);

        input = "<pre title=\"some title...\">   hello     world </pre>";
        output = "<pre title=\"some title...\">   hello     world </pre>";
        testBodySnippet(input, output);

        input = "<pre title=\"some title...\"><code>   hello     world </code></pre>";
        output = "<pre title=\"some title...\"><code>   hello     world </code></pre>";
        testBodySnippet(input, output);

        input = "<script type=\"text/javascript\">  \n\t   alert(1) \n\n\n  \t </script>";
        output = "<script>alert(1)</script>";
        testBodySnippet(input, output); // { collapseWhitespace: true }

        input = "<script>alert(\"foo     bar\")    </script>";
        output = "<script>alert(\"foo     bar\")</script>";
        testBodySnippet(input, output); // { collapseWhitespace: true }

        input = "<style>alert(\"foo     bar\")    </style>";
        output = "<style>alert(\"foo     bar\")</style>";
        testBodySnippet(input, output); // { collapseWhitespace: true }

        input = "<script type=\"text/javascript\">var = \"hello\";</script>\r\n\r\n\r\n"
                + "<style type=\"text/css\">#foo { color: red;        }          </style>\r\n\r\n\r\n"
                + "<div>\r\n  <div>\r\n    <div><!-- hello -->\r\n      <div>"
                + "<!--! hello -->\r\n        <div>\r\n          <div class=\"\">\r\n\r\n            "
                + "<textarea disabled=\"disabled\">     this is a textarea </textarea>\r\n          "
                + "</div>\r\n        </div>\r\n      </div>\r\n    </div>\r\n  </div>\r\n</div>"
                + "<pre>       \r\nxxxx</pre><span>x</span> <span>Hello</span> <b>billy</b>     \r\n"
                + "<input type=\"text\">\r\n<textarea></textarea>\r\n<pre></pre>";
        output = "<script>var = \"hello\";</script> "
                + "<style>#foo{color:red}</style> "
                + "<div><div><div>"
                + "<div><div><div>"
                + "<textarea disabled>     this is a textarea </textarea>"
                + "</div></div></div></div></div></div>"
                + "<pre>       \r\nxxxx</pre><span>x</span> <span>Hello</span> <b>billy</b> "
                + "<input> <textarea></textarea> <pre></pre>";
        testBodySnippet(input, output); // { collapseWhitespace: true }
    }

    @Test
    public void testCollapsingBooleanAttributes() {
        String input = "<input disabled=\"disabled\">";
        testBodySnippet(input, "<input disabled>");

        input = "<input CHECKED = \"checked\" readonly=\"readonly\">";
        testBodySnippet(input, "<input checked readonly>");

        input = "<option name=\"blah\" selected=\"selected\">moo</option>";
        testBodySnippet(input, "<option name=blah selected>moo</option>");

        input = "<input autofocus=\"autofocus\">";
        testBodySnippet(input, "<input autofocus>");

        input = "<input required=\"required\">";
        testBodySnippet(input, "<input required>");

        input = "<input multiple=\"multiple\">";
        testBodySnippet(input, "<input multiple>");

        input = "<div Allowfullscreen=foo Async=foo Autofocus=foo Autoplay=foo Checked=foo Compact=foo Controls=foo "
                + "Declare=foo Default=foo Defaultchecked=foo Defaultmuted=foo Defaultselected=foo Defer=foo Disabled=foo "
                + "Enabled=foo Formnovalidate=foo Hidden=foo Indeterminate=foo Inert=foo Ismap=foo Itemscope=foo "
                + "Loop=foo Multiple=foo Muted=foo Nohref=foo Noresize=foo Noshade=foo Novalidate=foo Nowrap=foo Open=foo "
                + "Pauseonexit=foo Readonly=foo Required=foo Reversed=foo Scoped=foo Seamless=foo Selected=foo Sortable=foo "
                + "Spellcheck=foo Truespeed=foo Typemustmatch=foo Visible=foo></div>";
        String output = "<div allowfullscreen async autofocus autoplay checked compact controls declare default defaultchecked "
                + "defaultmuted defaultselected defer disabled enabled formnovalidate hidden indeterminate inert "
                + "ismap itemscope loop multiple muted nohref noresize noshade novalidate nowrap open pauseonexit readonly "
                + "required reversed scoped seamless selected sortable spellcheck truespeed typemustmatch visible></div>";
        testBodySnippet(input, output);
    }

    @Test
    public void testCollapsingEnumeratedAttributes() {
        testBodySnippet("<div draggable=\"auto\"></div>", "<div></div>"); // { collapseBooleanAttributes: true }
        testBodySnippet("<div draggable=\"true\"></div>", "<div draggable=true></div>"); // { collapseBooleanAttributes: true }
        testBodySnippet("<div draggable=\"false\"></div>", "<div draggable=false></div>"); // { collapseBooleanAttributes: true }
        testBodySnippet("<div draggable=\"foo\"></div>", "<div></div>"); // { collapseBooleanAttributes: true }
        testBodySnippet("<div draggable></div>", "<div></div>"); // { collapseBooleanAttributes: true }
        testBodySnippet("<div Draggable=\"auto\"></div>", "<div></div>"); // { collapseBooleanAttributes: true }
        testBodySnippet("<div Draggable=\"true\"></div>", "<div draggable=true></div>"); // { collapseBooleanAttributes: true }
        testBodySnippet("<div Draggable=\"false\"></div>", "<div draggable=false></div>"); // { collapseBooleanAttributes: true }
        testBodySnippet("<div Draggable=\"foo\"></div>", "<div></div>"); // { collapseBooleanAttributes: true }
        testBodySnippet("<div Draggable></div>", "<div></div>"); // { collapseBooleanAttributes: true }
        testBodySnippet("<div draggable=\"Auto\"></div>", "<div></div>"); // { collapseBooleanAttributes: true }
    }

    @Test
    public void testCustomTags() {
        String input = "<custom-component>Oh, my.</custom-component>";
        String output = "<custom-component>Oh, my.</custom-component>";

        testBodySnippet(input, output);
    }

    @Test
    public void testAnchorWithBlockElement() {
        String input = "<a href=\"#\"><div>Well, look at me! I\"m a div!</div></a>";
        String output = "<a href=#><div>Well, look at me! I\"m a div!</div></a>";

        testBodySnippet(input, output);
    }

    @Test
    public void testBootstrapSpanButtonSpan() {
        String input = "<span class=\"input-group-btn\">"
                + "\n  <button class=\"btn btn-default\" type=\"button\">"
                + "\n    <span class=\"glyphicon glyphicon-search\"></span>"
                + "\n  </button>"
                + "</span>";

        String output = "<span class=input-group-btn><button class=\"btn btn-default\" type=button><span class=\"glyphicon glyphicon-search\"></span></button></span>";

        testBodySnippet(input, output);
    }

    @Test
    public void testSourceAndTrack() {
        String input = "<audio controls=\"controls\">"
                + "<source src=\"foo.wav\">"
                + "<source src=\"far.wav\">"
                + "<source src=\"foobar.wav\">"
                + "<track kind=\"captions\" src=\"sampleCaptions.vtt\" srclang=\"en\">"
                + "</audio>";
        String output = "<audio controls>"
                + "<source src=foo.wav>"
                + "<source src=far.wav>"
                + "<source src=foobar.wav>"
                + "<track kind=captions src=sampleCaptions.vtt srclang=en>"
                + "</audio>";

        testBodySnippet(input, output); // { removeOptionalTags: true }
    }

    @Test
    public void testNestedQuotes() {
        String input = "<div data=\"{'test':'\\\\'test\\\\''}\"></div>";
        testBodySnippet(input, input);

        input = "<div data='{\"test\":\"\\\\\"test\\\\\"\"}'></div>";
        testBodySnippet(input, input);
    }

    @Test
    public void testValuelessAttributes() {
        String input = "<br foo>";
        testBodySnippet(input, input);
    }

    @Test
    public void testNewlinesToSpaces() {
        String input = "test\n\n<input>\n\ntest";
        String output = "test <input> test";
        testBodySnippet(input, output);
    }

    @Test
    public void testMetaViewport() {
        String input = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">";
        String output = "<meta name=viewport content=\"width=device-width,initial-scale=1\">";
        testBodySnippet(input, output);

        input = "<meta name=\"viewport\" content=\"initial-scale=1.01, maximum-scale=1.0\">";
        output = "<meta name=viewport content=\"initial-scale=1.01,maximum-scale=1\">";
        testBodySnippet(input, output);

        input = "<meta name=\"viewport\" content=\"initial-scale=1.1, maximum-scale=1.0000\">";
        output = "<meta name=viewport content=\"initial-scale=1.1,maximum-scale=1\">";
        testBodySnippet(input, output);

        input = "<meta name=\"viewport\" content=\"initial-scale=1.00, maximum-scale=1.04\">";
        output = "<meta name=viewport content=\"initial-scale=1,maximum-scale=1.04\">";
        testBodySnippet(input, output);

        input = "<meta name=\"viewport\" content=\"width= 500 ,  initial-scale=1\">";
        output = "<meta name=viewport content=\"width=500,initial-scale=1\">";
        testBodySnippet(input, output);
    }

    @Test
    public void testMetaHttpEquiv() {
        String input = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">";
        String output = "<meta charset=utf-8>";
        testBodySnippet(input, output);

        input = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\">";
        output = "<meta charset=ISO-8859-1>";
        testBodySnippet(input, output);

        input = "<meta http-equiv=\"Content-Type\" content=\"text/html; \t charset=Shift_JIS \">";
        output = "<meta charset=Shift_JIS>";
        testBodySnippet(input, output);
    }

    @Test
    public void testNoscript() {
        String input = "<SCRIPT SRC=\"x\"></SCRIPT><NOSCRIPT>x</NOSCRIPT>";
        testBodySnippet(input, "<script src=x></script><noscript>x</noscript>");

        input = "<noscript>\n<!-- anchor linking to external file -->\n"
                + "<a href=\"#\" onclick=\"javascript:\">External Link</a>\n</noscript>";
        testBodySnippet(input, "<noscript><a href=#>External Link</a></noscript>");
    }

    @Test
    public void testCSS() {
        String pre = "<head><style>", post = "</style></head>";

        String input = "";
        String output = "";
        testHeadSnippet(pre + input + post, pre + output + post);

        input = " \n \t   \r";
        output = "";
        testHeadSnippet(pre + input + post, pre + output + post);

        input = "body { font-size: 1.75em }";
        output = "body{font-size:1.75em}";
        testHeadSnippet(pre + input + post, pre + output + post);

        input = "body { font-size: 400% }";
        output = "body{font-size:400%}";
        testHeadSnippet(pre + input + post, pre + output + post);

        input = "#clicker {\n"
                + "    text-align:center;\n"
                + "    letter-spacing:1ex;\n"
                + "    margin-right:-1ex;\n"
                + "    font-family:\"Lucida Sans Typewriter\",\"Lucida Console\",Monaco,\"Bitstream Vera Sans Mono\",monospace;\n"
                + "    font-size:400%;\n"
                + "    font-weight:700;\n"
                + "    margin-top:1em;\n"
                + "    margin-bottom:1em\n"
                + "}";
        output = "#clicker{text-align:center;letter-spacing:1ex;margin-right:-1ex;font-family:\"Lucida Sans Typewriter\",\"Lucida Console\",Monaco,\"Bitstream Vera Sans Mono\",monospace;font-size:400%;font-weight:700;margin-top:1em;margin-bottom:1em}";
        testHeadSnippet(pre + input + post, pre + output + post);
    }
}
