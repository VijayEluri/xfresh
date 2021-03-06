package net.sf.xfresh.core;

import net.sf.xfresh.core.impl.SimpleInternalResponse;
import net.sf.xfresh.jetty.AbstractJettyTest;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Date: 21.04.2007
 * Time: 12:23:01
 *
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
public class InternalResponseTest extends AbstractJettyTest {

    @Test
    public void testDoubleRedir() throws Exception {
        InternalResponse response = new SimpleInternalResponse(null);
        final String path = "test.xml";
        response.redirectTo(path); // ok
        try {
            response.redirectTo(path);
            fail("Expected IlleagalStateException");
        } catch (IllegalStateException e) {
            // ok
        }
    }

    @Test
    public void testAddCookie() throws Exception {
        HttpResponse response = httpClient.execute(buildRequest("test-addcookie.xml"));
        assertEquals(200, response.getStatusLine().getStatusCode());
        Header[] headers = response.getHeaders("Set-Cookie");
        for (int i = 0; i < headers.length; i++) {
            Header header = headers[i];
            System.out.println(header.getName() + " : " + header.getValue());
        }
        assertEquals(3, headers.length);

        for (int i = 0; i < 3; ++i)
            assertEquals(true, headers[i].getValue().contains("key=value"));

        for (int i = 1; i < 3; ++i)
            assertEquals(true, headers[i].getValue().contains("Expires="));

        for (int i = 1; i < 3; ++i)
            assertEquals(true, headers[i].getValue().contains("Domain=localhost"));

        for (int i = 2; i < 3; ++i)
            assertEquals(true, headers[i].getValue().contains("Path=test-addcookie.xml"));

        for (int i = 2; i < 3; ++i)
            assertEquals(true, headers[i].getValue().contains("HttpOnly"));
    }
}
