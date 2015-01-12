/*
 * Written by Volker Simonis, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 *
 *
 * Basic test for direct, "dead-reckoning" access to @Intrinsic fields in C2.
 *

from <ObjectLayout>/hotspot do:

javac -cp ../ObjectLayout/target/classes \
      examples/IntrinsicFieldDeadReckonTest.java

Default, pointer de-referencing field access
============================================

java -cp ../ObjectLayout/target/classes:examples \
     -XX:CompileCommand="print,IntrinsicFieldDeadReckonTest.shift" \
     -Xbatch -XX:-TieredCompilation \
     IntrinsicFieldDeadReckonTest
...
{method}
 - method holder:     'IntrinsicFieldDeadReckonTest'
 - name:              'shift'
 - signature:         '(LIntrinsicFieldDeadReckonTest$Triangle;II)V'
...
000   B1: #	B8 B2 <- BLOCK HEAD IS JUNK   Freq: 1
000   	# stack bang (136 bytes)
	pushq   rbp	# Save rbp
	subq    rsp, #16	# Create frame

00c   	movl    R11, [RSI + #16 (8-bit)]	# compressed ptr ! Field: IntrinsicFieldDeadReckonTest$Triangle.p
010   	NullCheck RSI
010
010   B2: #	B7 B3 <- B1  Freq: 0,999999
010   	testl   R11, R11	# compressed ptr
013   	je,s   B7  P=0,000001 C=-1,000000
013
015   B3: #	B9 B4 <- B2  Freq: 0,999998
015   	addl    [R11 + #12 (8-bit)], RDX	# int ! Field: IntrinsicFieldDeadReckonTest$Point.x
019   	addl    [R11 + #16 (8-bit)], RCX	# int ! Field: IntrinsicFieldDeadReckonTest$Point.y
01d   	movl    R11, [RSI + #12 (8-bit)]	# compressed ptr ! Field: IntrinsicFieldDeadReckonTest$Triangle.l
021   	movl    R10, [R11 + #12 (8-bit)]	# compressed ptr ! Field: IntrinsicFieldDeadReckonTest$Line.p1
025   	NullCheck R11
025
025   B4: #	B10 B5 <- B3  Freq: 0,999997
025   	addl    [R10 + #12 (8-bit)], RDX	# int ! Field: IntrinsicFieldDeadReckonTest$Point.x
029   	NullCheck R10
029
029   B5: #	B11 B6 <- B4  Freq: 0,999996
029   	addl    [R10 + #16 (8-bit)], RCX	# int ! Field: IntrinsicFieldDeadReckonTest$Point.y
02d   	movl    R11, [R11 + #16 (8-bit)]	# compressed ptr ! Field: IntrinsicFieldDeadReckonTest$Line.p2
031   	addl    [R11 + #12 (8-bit)], RDX	# int ! Field: IntrinsicFieldDeadReckonTest$Point.x
035   	NullCheck R11
035
035   B6: #	N1 <- B5  Freq: 0,999995
035   	addl    [R11 + #16 (8-bit)], RCX	# int ! Field: IntrinsicFieldDeadReckonTest$Point.y
039   	addq    rsp, 16	# Destroy frame
	popq   rbp
	testl  rax, [rip + #offset_to_poll_page]	# Safepoint: poll for GC
044   	ret

Optimized, dead-reckoning field access
======================================

java -cp ../ObjectLayout/target/classes:examples \
     -XX:CompileCommand="print,IntrinsicFieldDeadReckonTest.shift" \
     -Xbatch -XX:-TieredCompilation -XX:+OptimizeObjectLayout \
     IntrinsicFieldDeadReckonTest
...
{method}
 - method holder:     'IntrinsicFieldDeadReckonTest'
 - name:              'shift'
 - signature:         '(LIntrinsicFieldDeadReckonTest$Triangle;II)V'
...
000   B1: #	B3 B2 <- BLOCK HEAD IS JUNK   Freq: 1
000   	# stack bang (136 bytes)
	pushq   rbp	# Save rbp
	subq    rsp, #16	# Create frame

00c   	addl    [RSI + #92 (8-bit)], RDX	# int
00f   	NullCheck RSI
00f
00f   B2: #	N1 <- B1  Freq: 0,999999
00f   	addl    [RSI + #44 (8-bit)], RDX	# int
012   	addl    [RSI + #96 (8-bit)], RCX	# int
015   	addl    [RSI + #68 (8-bit)], RDX	# int
018   	addl    [RSI + #48 (8-bit)], RCX	# int
01b   	addl    [RSI + #72 (8-bit)], RCX	# int
01e   	addq    rsp, 16	# Destroy frame
	popq   rbp
	testl  rax, [rip + #offset_to_poll_page]	# Safepoint: poll for GC
029   	ret
 *
 *
 * When running with -XX:+OptimizeObjectLayout all the reference loads in the
 * "shift()" method are eliminated in favour of direct access to the intrinsified
 * nested Point and Line fields. Notice that also the implcit null checks could
 * be eliminated because the nested fields ca never be null.
 *
 * so the code for the Java line "t.getLine().getP1().x += x;" is collapsed from:
 *
 * movl    R11, [RSI + #12 (8-bit)]	! Field: IntrinsicFieldDeadReckonTest$Triangle.l
 * movl    R10, [R11 + #12 (8-bit)]	! Field: IntrinsicFieldDeadReckonTest$Line.p1
 * NullCheck R11
 * addl    [R11 + #12 (8-bit)], RDX	! Field: IntrinsicFieldDeadReckonTest$Point.x
 * NullCheck R11
 *
 * to:
 *
 * addl    [RSI + #44 (8-bit)], RDX
 *
 * where 44 is the offset of the x field of a Lines "p1" Point field inside a Triangle.
 */

import org.ObjectLayout.Intrinsic;
import org.ObjectLayout.IntrinsicObjects;


public class IntrinsicFieldDeadReckonTest {

    public static class Point {
        public int x;
        public int y;
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
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
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

    public static void shift(Triangle t, int x, int y) {
        t.getPoint().x += x;
        t.getPoint().setY(t.getPoint().getY() + y);
        t.getLine().getP1().x += x;
        t.getLine().getP1().y += y;
        t.getLine().getP2().x += x;
        t.getLine().getP2().y += y;
    }

    public static void main(String args[]) {
        int COUNT = args.length > 0 ? Integer.parseInt(args[0]) : 10_000;

        Point p1 = new Point(1, 1);
        Point p2 = new Point(2, 2);
        Point p3 = new Point(3, 3);

        Line l1 = new Line(p1, p2);

        Triangle t1 = new Triangle(l1, p3);

        System.out.println("t1 = " + t1);
        System.out.println("Shifting t1 by (" + COUNT + ", " + COUNT + ")");

        for (int i = 0; i < COUNT; i++) {
            shift(t1, 1, 1);
        }

        System.out.println("t1 = " + t1);
    }
}
