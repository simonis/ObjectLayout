/*
 * Written by Volker Simonis, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 *
 *
 * Basic test for @Intrinsic fields laied out within their containing objects.
 *

from <ObjectLayout>/hotspot do:

javac -cp ../ObjectLayout/target/classes examples/IntrinsicFieldTest.java

Canonical Java object layout
============================

java  -cp ../ObjectLayout/target/classes:examples \
      -XX:+PrintFieldLayout IntrinsicFieldTest
...
IntrinsicFieldTest$Point: field layout
  @ 12 --- instance fields start ---
  @ 12 "x" I
  @ 16 "y" I
  @ 20 --- instance fields end ---
  @ 24 --- instance ends ---
...
IntrinsicFieldTest$Line: field layout
  @ 12 --- instance fields start ---
  @ 12 "p1" LIntrinsicFieldTest$Point;
  @ 16 "p2" LIntrinsicFieldTest$Point;
  @ 20 --- instance fields end ---
  @ 24 --- instance ends ---
...
IntrinsicFieldTest$Triangle: field layout
  @ 12 --- instance fields start ---
  @ 12 "l" LIntrinsicFieldTest$Line;
  @ 16 "p" LIntrinsicFieldTest$Point;
  @ 20 --- instance fields end ---
  @ 24 --- instance ends ---

Optimized object layout with intrinsified "value objects"
=========================================================

java -cp ../ObjectLayout/target/classes:examples \
     -XX:+PrintFieldLayout -XX:+OptimizeObjectLayout IntrinsicFieldTest
...
IntrinsicFieldTest$Line: field layout
  @ 12 --- instance fields start ---
  @ 12 --- instance fields end ---
  @ 16 --- intrinsic fields start ---
  @ 16 "p1" LIntrinsicFieldTest$Point;
  @ 40 "p2" LIntrinsicFieldTest$Point;
  @ 64 --- intrinsic fields end ---
  @ 64 --- instance ends ---
...
IntrinsicFieldTest$Triangle: field layout
  @ 12 --- instance fields start ---
  @ 12 --- instance fields end ---
  @ 16 --- intrinsic fields start ---
  @ 16 "l" LIntrinsicFieldTest$Line;
  @ 80 "p" LIntrinsicFieldTest$Point;
  @104 --- intrinsic fields end ---
  @104 --- instance ends ---
...

 *
 * The two Point fields "p1" and "p2" (with a size of 24 bytes) of a Line object
 * are laied out within their containing Line object.
 *
 * The Line field "l" (with a size of 64 bytes) and the Point field "p" (with a
 * size of 24 bytes) are both laied out within their containing Triangle object.
 *
 * NOTICE that the -XX:+PrintFieldLayout option is only available in not-product builds!
 */

import org.ObjectLayout.Intrinsic;
import org.ObjectLayout.IntrinsicObjects;

public class IntrinsicFieldTest {

    public static class Point {
        protected int x;
        protected int y;
        public Point() {}
        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
        public void set(Point p) {
            if (p != null) {
                this.x = p.x;
                this.y = p.y;
            }
        }
        public String toString() { return "Point(" + x + ", " + y + ")"; }
    }

    public static class Line {
        @Intrinsic
        private final Point p1 = IntrinsicObjects.constructWithin("p1", this);
        @Intrinsic
        private final Point p2 = IntrinsicObjects.constructWithin("p2", this);
        public Line() {}
        public Line(Point p1, Point p2) {
            this.p1.x = p1.x;
            this.p1.y = p1.y;
            this.p2.set(p2);
        }
        public void set(Line l) {
            if (l != null) {
                this.p1.set(l.p1);
                this.p2.set(l.p2);
            }
        }
        public String toString() { return "Line(" + p1 + ", " + p2 + ")"; }
    }

    public static class Triangle {
        @Intrinsic
        private final Line l = IntrinsicObjects.constructWithin("l", this);
        @Intrinsic
        private final Point p = IntrinsicObjects.constructWithin("p", this);
        public Triangle() {}
        public Triangle(Line l, Point p) {
            this.l.set(l);
            this.p.set(p);
        }
        public String toString() { return "Triangle(" + l + ", " + p + ")"; }
    }

    public static void main(String args[]) {
        Point p1 = new Point(1, 1);
        Point p2 = new Point(2, 2);
        Point p3 = new Point(3, 3);

        Line l = new Line();
        Line l1 = new Line(p1, p2);
        // Call constructor a second time to exercise the 'fast' bytecodes to
        // which 'normal' bytecodes are rewritten after the first execution
        // (i.e. getfield -> _fast_agetfield)
        l1 = new Line(p1, p2);
        Triangle t1 = new Triangle(l1, p3);
        p3.set(new Point(5, 5));

        System.out.println("\np1 = " + p1 +
                           "\np2 = " + p2 + 
                           "\np3 = " + p3 +
                           "\nl1 = " + l1 +
                           "\nt1 = " + t1);
    }
}
