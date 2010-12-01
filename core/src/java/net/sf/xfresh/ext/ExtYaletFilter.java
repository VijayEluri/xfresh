package net.sf.xfresh.ext;

import net.sf.xfresh.core.*;
import net.sf.xfresh.util.XmlUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.UniqueTag;
import org.springframework.util.FileCopyUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

/**
 * Date: Nov 24, 2010
 * Time: 11:13:37 PM
 *
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
public class ExtYaletFilter extends YaletFilter {
    private static final Logger log = Logger.getLogger(ExtYaletFilter.class);

    private static final String XFRESH_EXT_URI = "http://xfresh.sf.net/ext";
    private static final String HTTP_ELEMENT = "http";
    private static final String JS_ELEMENT = "js";
    private String httpUrl;
    private StringBuilder jsContent;
    private Context jsContext;
    private Scriptable jsScope;
    private static final String JS_OUT_NAME = "out";
    private HttpLoader httpLoader;
    private static final int DEFAULT_TIMEOUT = 300;
    private static final String JS_SRC_ATTR = "src";
    private String resourceBase;

    public ExtYaletFilter(final YaletResolver yaletResolver,
                          final SaxGenerator saxGenerator,
                          final InternalRequest request,
                          final InternalResponse response, final String resourceBase) {
        super(yaletResolver, saxGenerator, request, response);
        this.resourceBase = resourceBase;
        httpLoader = new HttpLoader();
    }

    @Override
    public void startElement(final String uri,
                             final String localName,
                             final String qName,
                             final Attributes atts) throws SAXException {
        if (isHttpBlock(uri, localName)) {
            final String urlValue = atts.getValue("url").trim();
            if (urlValue.startsWith("js:")) {
                checkAndInitJsContext();
                httpUrl = processJsContent(wrapFunction(urlValue.substring(3)));
            } else {
                httpUrl = urlValue;
            }
        } else if (isJsBlock(uri, localName)) {
            if (jsContent != null) {
                log.error("Nested js blocks not possible, previous ignored");
            }
            jsContent = new StringBuilder();
            checkAndInitJsContext();
            final String src = atts.getValue(JS_SRC_ATTR);
            if (!StringUtils.isEmpty(src)) {
                try {
                    final String jsFileContent =
                            FileCopyUtils.copyToString(new FileReader(resourceBase + File.separatorChar + src));
                    processJsAndWrite(jsFileContent);
                } catch (IOException e) {
                    log.error("Can't read js from file: " + src, e); //ignored
                }
            }
        } else {
            super.startElement(uri, localName, qName, atts);
        }
    }

    private void checkAndInitJsContext() {
        if (jsContext == null) {
            jsContext = Context.enter();
            jsScope = jsContext.initStandardObjects();
            jsScope.put("writer", jsScope, new ContentWriter(getContentHandler()));
            jsScope.put("request", jsScope, request);
            jsScope.put("httpLoader", jsScope, httpLoader);
        }
    }

    @Override
    public void characters(final char[] chars, final int start, final int length) throws SAXException {
        if (jsContent != null) {
            jsContent.append(chars, start, length);
        } else {
            super.characters(chars, start, length);
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        if (isHttpBlock(uri, localName)) {
            if (checkHttpUrl()) {
                processHttpBlock();
            }
            httpUrl = null;
        } else if (isJsBlock(uri, localName)) {
            if (jsContent != null) {
                processJs();
            }
            jsContent = null;
        } else {
            super.endElement(uri, localName, qName);
        }
    }

    private void processJs() throws SAXException {
        final String content = jsContent.toString();
        processJsAndWrite(content);
    }

    private void processJsAndWrite(final String content) throws SAXException {
        final String result = processJsContent(content);
        if (result != null) {
            XmlUtil.text(getContentHandler(), result);
        }
    }

    private String processJsContent(final String content) {
        if (!StringUtils.isEmpty(content)) {
            try {
                evaluateJs(content);
                final Object value = jsScope.get(JS_OUT_NAME, jsScope);
                if (isCorrectReturnedValue(value)) {
                    return value.toString();
                }
            } catch (Throwable e) {
                log.error("Error while processing JS (ignored): " + content, e); //ignored
            } finally {
                jsScope.put(JS_OUT_NAME, jsScope, null);
            }
        }
        return null;
    }

    private void evaluateJs(final String content) {
        jsContext.evaluateString(jsScope, content, null, 1, null);
    }

    private boolean isCorrectReturnedValue(final Object value) {
        return value != null && !(value instanceof UniqueTag) && !(value instanceof Undefined);
    }

    private String wrapFunction(final String content) {
        return "function _f_wrpr() {" + content + " }; var " + JS_OUT_NAME + " = _f_wrpr();";
    }

    private void processHttpBlock() {
        try {
            final InputStream content = httpLoader.loadAsStream(httpUrl, DEFAULT_TIMEOUT);
            final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.setXIncludeAware(true);
            final SAXParser saxParser = parserFactory.newSAXParser();
            final XMLReader xmlReader = saxParser.getXMLReader();
            xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
            xmlReader.setContentHandler(getContentHandler());
            xmlReader.parse(new InputSource(content));
        } catch (IOException e) {
            log.error("Error while read url: " + httpUrl, e); //ignored
        } catch (ParserConfigurationException e) {
            log.error("Error while parse url: " + httpUrl, e); //ignored
        } catch (SAXException e) {
            log.error("Error while parse url: " + httpUrl, e); //ignored
        }
    }

    private boolean checkHttpUrl() {
        return httpUrl != null && httpUrl.startsWith("http://");
    }

    private boolean isHttpBlock(final String uri, final String localName) {
        return XFRESH_EXT_URI.equalsIgnoreCase(uri) && HTTP_ELEMENT.equalsIgnoreCase(localName);
    }

    private boolean isJsBlock(final String uri, final String localName) {
        return XFRESH_EXT_URI.equalsIgnoreCase(uri) && JS_ELEMENT.equalsIgnoreCase(localName);
    }
}