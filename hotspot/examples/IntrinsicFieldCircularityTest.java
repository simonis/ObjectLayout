/*
 * Written by Volker Simonis, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 *
 *
 * Basic test for an illegal, circular dependency between two @Intrinsic fields.
 *

from <ObjectLayout>/hotspot do:

javac -cp ../ObjectLayout/target/classes examples/IntrinsicFieldCircularityTest.java

java  -cp ../ObjectLayout/target/classes:examples -XX:+OptimizeObjectLayout IntrinsicFieldCircularityTest

OK - catched java.lang.ClassCircularityError: IntrinsicFieldCircularityTest$FooBar
OK - catched java.lang.ClassCircularityError: IntrinsicFieldCircularityTest$Foo

 *
 * NOTICE: running without -XX:+OptimizeObjectLayout currently leads to a StackOverflowException
 *         This has to be fixed in the pure-Java implementation of IntrinsicObjects.constructWithin
 */

import org.ObjectLayout.Intrinsic;
import org.ObjectLayout.IntrinsicObjects;

public class IntrinsicFieldCircularityTest {
    
    public static class FooBar {
        int x ,y;
        @Intrinsic
        private final FooBar foobar = IntrinsicObjects.constructWithin("foobar", this);
    }

    public static class Foo {
        int x, y;
        @Intrinsic
        private final Bar bar = IntrinsicObjects.constructWithin("bar", this);
    }
    
    public static class Bar {
        int x, y;
        @Intrinsic
        private final Foo foo = IntrinsicObjects.constructWithin("foo", this);
    }
    
    public static void main(String args[]) {
        try {
            FooBar foobar = new FooBar();
        } catch(ClassCircularityError e) {
            System.out.println("OK - catched " + e);
        }

        try {
            Foo foo = new Foo();
        } catch(ClassCircularityError e) {
            System.out.println("OK - catched " + e);
        }
    }
}
