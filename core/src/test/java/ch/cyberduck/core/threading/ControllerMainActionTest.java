package ch.cyberduck.core.threading;

import ch.cyberduck.core.AbstractController;
import ch.cyberduck.core.AbstractTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @version $Id$
 */
public class ControllerMainActionTest extends AbstractTestCase {

    @Test
    public void testLock() throws Exception {
        final AbstractController c = new AbstractController() {
            @Override
            public void invoke(final MainAction runnable, final boolean wait) {
                //
            }
        };
        assertEquals(c, new ControllerMainAction(c) {

            @Override
            public void run() {
                //
            }
        }.lock());
    }
}