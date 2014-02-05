package org.jeo.svg;

class Font {

    static final float DEFAULT_SIZE = 14f;

    enum Weight {
        normal, bold, bolder, lighter, inherit
    }

    enum Style {
        normal, italic, oblique, inherit
    }

    enum Variant {
        normal, smallcaps, inherit
    }

    String family = "sans-serif";
    Weight weight = Weight.normal;
    Style style = Style.normal;
    float size = DEFAULT_SIZE;
    Unit unit = Unit.pixel;
}
