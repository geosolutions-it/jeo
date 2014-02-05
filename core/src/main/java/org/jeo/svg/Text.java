package org.jeo.svg;

class Text {
    enum Anchor {
        start, middle, end
    }
    enum Direction {
        ltr, rtl, inherit
    }

    Text(String value) {
        this.value = value;
    }

    String value;
    Font font = new Font();
    Anchor anchor = Anchor.start;
    Direction direction = Direction.ltr;
}
