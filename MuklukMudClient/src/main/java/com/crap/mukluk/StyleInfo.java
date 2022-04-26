package com.crap.mukluk;

import android.text.style.CharacterStyle;

public class StyleInfo {
    CharacterStyle style;
    int start;
    int end;

    public StyleInfo(CharacterStyle style, int start) {
	this.style = style;
	this.start = start;
	this.end = -1;
    }
}