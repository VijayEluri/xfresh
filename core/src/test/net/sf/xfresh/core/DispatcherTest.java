package net.sf.xfresh.core;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import net.sf.xfresh.core.SimpleInternalRequest;
import net.sf.xfresh.core.SimpleInternalResponse;
import net.sf.xfresh.core.Dispatcher;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Date: 20.04.2007
 * Time: 17:50:41
 *
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
public class DispatcherTest extends TestCase {
    private static final String TEST_CONTENT = "<?xml version=\"1.0\" encoding=\"windows-1251\"?>\n<page><a>����</a><data id=\"addTestInfo\"/></page>";
    private static final String TEST_TRANSFORMED_CONTENT = "<html><head><META http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1251\"><title>����</title></head><body><h1>��������111</h1></body></html>";
    private Dispatcher dispatcher;
    private ByteArrayOutputStream baos;
    private SimpleInternalResponse response;
    private SimpleInternalRequest request;

    protected void setUp() throws Exception {
        super.setUp();

        dispatcher = new Dispatcher();
        dispatcher.setApplicationContext(new ClassPathXmlApplicationContext("testApplicationContext.xml"));


        baos = new ByteArrayOutputStream();
        response = new SimpleInternalResponse(null);
        response.setWriter(new PrintWriter(baos));
    }

    public void testProcessFile() throws Exception {
        request = new SimpleInternalRequest(null, "test.xml");
        request.setNeedTransform(false);
        dispatcher.process(request, response, new RedirHandler(null));
        assertEquals(TEST_CONTENT,
                new String(baos.toByteArray()).trim());
    }

    public void testTransformFile() throws Exception {
        request = new SimpleInternalRequest(null, "test.xml");

        dispatcher.process(request, response, new RedirHandler(null));
        assertEquals(TEST_TRANSFORMED_CONTENT,
                new String(baos.toByteArray()).trim());
    }

    public void testTransformXFile() throws Exception {
        request = new SimpleInternalRequest(null, "xtest.xml");

        dispatcher.process(request, response, new RedirHandler(null));
        assertEquals(TEST_CONTENT,
                new String(baos.toByteArray()).trim());
    }
}
