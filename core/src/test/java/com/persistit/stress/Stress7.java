/**
 * END USER LICENSE AGREEMENT (“EULA”)
 *
 * READ THIS AGREEMENT CAREFULLY (date: 9/13/2011):
 * http://www.akiban.com/licensing/20110913
 *
 * BY INSTALLING OR USING ALL OR ANY PORTION OF THE SOFTWARE, YOU ARE ACCEPTING
 * ALL OF THE TERMS AND CONDITIONS OF THIS AGREEMENT. YOU AGREE THAT THIS
 * AGREEMENT IS ENFORCEABLE LIKE ANY WRITTEN AGREEMENT SIGNED BY YOU.
 *
 * IF YOU HAVE PAID A LICENSE FEE FOR USE OF THE SOFTWARE AND DO NOT AGREE TO
 * THESE TERMS, YOU MAY RETURN THE SOFTWARE FOR A FULL REFUND PROVIDED YOU (A) DO
 * NOT USE THE SOFTWARE AND (B) RETURN THE SOFTWARE WITHIN THIRTY (30) DAYS OF
 * YOUR INITIAL PURCHASE.
 *
 * IF YOU WISH TO USE THE SOFTWARE AS AN EMPLOYEE, CONTRACTOR, OR AGENT OF A
 * CORPORATION, PARTNERSHIP OR SIMILAR ENTITY, THEN YOU MUST BE AUTHORIZED TO SIGN
 * FOR AND BIND THE ENTITY IN ORDER TO ACCEPT THE TERMS OF THIS AGREEMENT. THE
 * LICENSES GRANTED UNDER THIS AGREEMENT ARE EXPRESSLY CONDITIONED UPON ACCEPTANCE
 * BY SUCH AUTHORIZED PERSONNEL.
 *
 * IF YOU HAVE ENTERED INTO A SEPARATE WRITTEN LICENSE AGREEMENT WITH AKIBAN FOR
 * USE OF THE SOFTWARE, THE TERMS AND CONDITIONS OF SUCH OTHER AGREEMENT SHALL
 * PREVAIL OVER ANY CONFLICTING TERMS OR CONDITIONS IN THIS AGREEMENT.
 */

package com.persistit.stress;

import com.persistit.Exchange;
import com.persistit.Key;
import com.persistit.test.TestResult;
import com.persistit.util.ArgParser;

/**
 * Test to try all split and join locations and conditions. Plan: 1. For each of
 * several key and valuesizes, create a tree with enough key/value pairs of that
 * size to yield 3-level tree 2. Between each key position on one or more pages,
 * insert one or more key/value pairs to force a split, then delete them to
 * cause a rejoin 3. Make sure the resulting tree is valid.
 * 
 */
public class Stress7 extends StressBase {
    private final static String SHORT_DESCRIPTION = "Exercise page splits and rejoin";

    private final static String LONG_DESCRIPTION = "   Inserts and deletes key/value pairs in a pattern that tests\r\n"
            + "   split and rejoin logic extensively.";

    @Override
    public String shortDescription() {
        return SHORT_DESCRIPTION;
    }

    @Override
    public String longDescription() {
        return LONG_DESCRIPTION;
    }

    private final static String[] ARGS_TEMPLATE = { "repeat|int:1:0:1000000000|Repetitions",
            "count|int:1000:0:100000|Number of nodes to populate", "size|int:500:1:10000|Max splitting value size",
            "seed|int:1:1:20000|Random seed",

    };

    int _size;
    int _seed;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        _ap = new ArgParser("com.persistit.Stress7", _args, ARGS_TEMPLATE);
        _total = _ap.getIntValue("count");
        _repeatTotal = _ap.getIntValue("repeat");
        _total = _ap.getIntValue("count");
        _size = _ap.getIntValue("size");
        _seed = _ap.getIntValue("seed");

        _dotGranularity = 100;

        try {
            _exs = getPersistit().getExchange("persistit", "shared", true);
        } catch (final Exception ex) {
            handleThrowable(ex);
        }
    }

    /**
     * Implements tests with long keys and values of borderline length
     */
    @Override
    public void executeTest() {
        for (_repeat = 0; (_repeat < _repeatTotal) && !isStopped(); _repeat++) {
            try {
                _exs.getValue().putString("");
                final int keyLength = (_repeat) + 10;
                _sb1.setLength(0);
                _sb2.setLength(0);
                for (int i = 0; i < keyLength; i++) {
                    _sb1.append('x');
                }
                for (int i = 0; i < 500; i++) {
                    _sb2.append('x');
                }

                verboseln();
                verboseln();
                verboseln("Starting test cycle " + _repeat + " at " + tsString());
                describeTest("Deleting all records");
                setPhase("@");
                _exs.clear().append("stress7").append(_threadIndex).remove(Key.GTEQ);
                verboseln();

                describeTest("Creating baseline records");
                setPhase("a");
                for (_count = 0; (_count < _total) && !isStopped(); _count++) {
                    dot();
                    _exs.clear().append("stress7").append(_threadIndex).append(_count).append(_sb1);
                    _exs.store();
                }
                verboseln();

                describeTest("Splitting and joining");
                setPhase("b");
                for (_count = 0; (_count < _total) && !isStopped(); _count++) {
                    dot();
                    _exs.clear().append("stress7").append(_threadIndex).append(_count).append(_sb1);
                    _sb2.setLength(0);
                    final int toSize = random(1, _size);
                    for (int size = 0; (size < toSize) && !isStopped(); size += 4) {
                        _sb2.append('y');
                        _exs.getValue().putString(_sb2);
                        _exs.store();
                        _exs.remove();
                    }
                }
                verboseln();

                describeTest("Verifying");
                setPhase("c");
                for (_count = 0; (_count < _total) && !isStopped(); _count++) {
                    dot();
                    _exs.clear().append("stress7").append(_threadIndex).append(_count).append(_sb1);
                    _exs.fetch();
                    if (_exs.getValue().isDefined()) {
                        _result = new TestResult(false, "Value for key " + _exs.getKey()
                                + " is defined but should not be");
                        forceStop();
                        break;
                    }
                }
                verboseln();

            } catch (final Exception de) {
                handleThrowable(de);
            }
            verboseln("Done at " + tsString());
        }
    }

    private void setupKey(final Exchange ex, final int length, final int depth, final int a, final int b,
            final char fill) {
        _sb1.setLength(0);
        for (int i = 0; i < length; i++) {
            _sb1.append(fill);
        }
        fillLong(b, depth, 5, true);
        ex.clear().append(a).append(_sb1);
    }

    public static void main(final String[] args) {
        new Stress7().runStandalone(args);
    }
}
