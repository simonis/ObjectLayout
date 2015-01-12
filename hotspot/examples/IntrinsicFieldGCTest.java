/*
 * Written by Volker Simonis, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 *
 *
 * Basic test for @Intrinsic fields laied out within their containing objects.
 *

from <ObjectLayout>/hotspot do:

javac -cp ../ObjectLayout/target/classes examples/IntrinsicFieldTest.java

java -cp ../ObjectLayout/target/classes:examples \
     -XX:-DoEscapeAnalysis -XX:-TieredCompilation \
     -XX:+OptimizeObjectLayout IntrinsicFieldGCTest

 *
 * This test currently runs in the following error due to live references to an
 * intrinisfied field as well as to its enclosing object:
 *

createLine(i, null, null) - OK
createLine(i, null, lines) - OK
createLine(i, points, null) - OK
createLine(i, points, lines -
# To suppress the following error report, specify this argument
# after -XX: or in .hotspotrc:  SuppressErrorAt=/bitMap.inline.hpp:37
#
# A fatal error has been detected by the Java Runtime Environment:
#
#  Internal Error (/share/OpenJDK/jdk9-dev/hotspot/src/share/vm/utilities/bitMap.inline.hpp:37), pid=13767, tid=140533431748352
#  assert(beg_index <= end_index) failed: BitMap range error

 */

import org.ObjectLayout.Intrinsic;
import org.ObjectLayout.IntrinsicObjects;


public class IntrinsicFieldGCTest {

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
        public Point getP1() { return p1; }
        public Point getP2() { return p2; }
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
        public Line getLine() { return l; }
        public Point getPoint() { return p; }
        public String toString() { return "Triangle(" + l + ", " + p + ")"; }
    }

    static Point[] points;
    static Line[] lines;

    static void createLine(int i, Point[] points, Line[] lines) {
        Point p1 = new Point(i, 2*i);
        Point p2 = new Point(2*i, i);
        Line l1 = new Line(p1 ,p2);
        if (lines != null && i < lines.length) lines[i] = l1;
        if (points != null && i < points.length) points[i] = l1.getP1();
    }

    static void check(Line[] lines) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].getP1().x != i || lines[i].getP1().y != 2*i) {
                System.out.println("Bad line " + i + ": " + lines[i]);
                System.exit(-1);
            }
        }
    }

    static void check(Point[] points) {
        for (int i = 0; i < points.length; i++) {
            if (points[i].x != i || points[i].y != 2*i) {
                System.out.println("Bad point " + i + ": " + points[i]);
                System.exit(-1);
            }
        }
    }

    public static void main(String args[]) {
        int SIZE = args.length > 0 ? Integer.parseInt(args[0]) : 10_000;
        points = new Point[SIZE];
        lines = new Line[SIZE];

        System.out.print("createLine(i, null, null) - ");
        for (int i = 0; i < SIZE; i++) {
            createLine(i, null, null);
        }
        System.gc();
        System.out.println("OK");

        System.out.print("createLine(i, null, lines) - ");
        for (int i = 0; i < SIZE; i++) {
            createLine(i, null, lines);
        }
        System.gc();
        check(lines);
        System.out.println("OK");

        System.out.print("createLine(i, points, null) - ");
        for (int i = 0; i < SIZE; i++) {
            createLine(i, points, null);
        }
        System.gc();
        check(points);
        System.out.println("OK");

        System.out.print("createLine(i, points, lines - ");
        for (int i = 0; i < SIZE; i++) {
            createLine(i, points, lines);
        }
        System.gc();
        check(lines);
        check(points);
        System.out.println("OK");
    }
}
