package com.crap.mukluk;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;

public abstract class AnsiUtility
{
    private static final String TAG = "AnsiUtility";

    private static final Pattern ANSI_PATTERN = Pattern.compile("\\e\\[[\\d+;?]+m");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    public static final IndexedColors xtermColors = new IndexedColors();

    public static final int BLACK = Color.rgb(0, 0, 0);
    private static final int RED = Color.rgb(128, 0, 0);
    private static final int GREEN = Color.rgb(0, 128, 0);
    private static final int YELLOW = Color.rgb(128, 128, 0);
    private static final int BLUE = Color.rgb(0, 0, 128);
    private static final int MAGENTA = Color.rgb(128, 0, 12);
    private static final int CYAN = Color.rgb(0, 128, 128);
    private static final int GRAY = Color.rgb(192, 192, 192);

    private static final int DARK_GRAY = Color.rgb(128, 128, 128);
    private static final int BRIGHT_RED = Color.rgb(255, 0, 0);
    private static final int BRIGHT_GREEN = Color.rgb(0, 255, 0);
    private static final int BRIGHT_YELLOW = Color.rgb(255, 255, 0);
    private static final int BRIGHT_BLUE = Color.rgb(0, 0, 255);
    private static final int BRIGHT_MAGENTA = Color.rgb(255, 0, 255);
    private static final int BRIGHT_CYAN = Color.rgb(0, 255, 255);
    private static final int WHITE = Color.rgb(255, 255, 255);

    public static ArrayList<Integer> getAnsiCodeFromStyle(CharacterStyle charStyle)
    {
        ArrayList<Integer> returnStyle = new ArrayList<Integer>(3);

        if (charStyle instanceof StyleSpan)
        {
            StyleSpan ss = (StyleSpan) charStyle;
            int style = ss.getStyle();

            if (style == android.graphics.Typeface.ITALIC)
                returnStyle.add(3);
        }
        else if (charStyle instanceof UnderlineSpan)
        {
            returnStyle.add(4);
        }
        else if (charStyle instanceof StrikethroughSpan)
        {
            returnStyle.add(9);
        }
        else if (charStyle instanceof ForegroundColorSpan)
        {
            ForegroundColorSpan fs = (ForegroundColorSpan) charStyle;
            int color = fs.getForegroundColor();

            if (color == BLACK)
                returnStyle.add(30);
            else if (color == RED)
                returnStyle.add(31);
            else if (color == GREEN)
                returnStyle.add(32);
            else if (color == YELLOW)
                returnStyle.add(33);
            else if (color == BLUE)
                returnStyle.add(34);
            else if (color == MAGENTA)
                returnStyle.add(35);
            else if (color == CYAN)
                returnStyle.add(36);
            else if (color == GRAY)
                returnStyle.add(37);
            else if (color == DARK_GRAY)
                returnStyle.add(90);
            else if (color == BRIGHT_RED)
                returnStyle.add(91);
            else if (color == BRIGHT_GREEN)
                returnStyle.add(92);
            else if (color == BRIGHT_YELLOW)
                returnStyle.add(93);
            else if (color == BRIGHT_BLUE)
                returnStyle.add(94);
            else if (color == BRIGHT_MAGENTA)
                returnStyle.add(95);
            else if (color == BRIGHT_CYAN)
                returnStyle.add(96);
            else if (color == WHITE)
                returnStyle.add(97);
            else
            {
                returnStyle.add(38);
                returnStyle.add(5);
                returnStyle.add(color);
            }
        }
        else if (charStyle instanceof BackgroundColorSpan)
        {
            BackgroundColorSpan bs = (BackgroundColorSpan) charStyle;
            int color = bs.getBackgroundColor();

            if (color == BLACK)
                returnStyle.add(40);
            else if (color == RED)
                returnStyle.add(41);
            else if (color == GREEN)
                returnStyle.add(42);
            else if (color == YELLOW)
                returnStyle.add(43);
            else if (color == BLUE)
                returnStyle.add(44);
            else if (color == MAGENTA)
                returnStyle.add(45);
            else if (color == CYAN)
                returnStyle.add(46);
            else if (color == GRAY)
                returnStyle.add(47);
            else if (color == DARK_GRAY)
                returnStyle.add(100);
            else if (color == BRIGHT_RED)
                returnStyle.add(101);
            else if (color == BRIGHT_GREEN)
                returnStyle.add(102);
            else if (color == BRIGHT_YELLOW)
                returnStyle.add(103);
            else if (color == BRIGHT_BLUE)
                returnStyle.add(104);
            else if (color == BRIGHT_MAGENTA)
                returnStyle.add(105);
            else if (color == BRIGHT_CYAN)
                returnStyle.add(106);
            else if (color == WHITE)
                returnStyle.add(107);
            else
            {
                returnStyle.add(48);
                returnStyle.add(5);
                returnStyle.add(color);
            }
        }

        if (returnStyle.size() == 0)
            returnStyle.add(-1);

        return returnStyle;
    }

    public static CharSequence getWithAnsiStyles(String strToConvert, boolean stripOnly)
    {
        Matcher mAnsi = ANSI_PATTERN.matcher(strToConvert);
        CharSequence returnText;
        String strippedText = mAnsi.replaceAll("");

        if (!stripOnly && strippedText.length() != strToConvert.length())
        {
            SpannableStringBuilder ansiString = new SpannableStringBuilder(strippedText);
            mAnsi = ANSI_PATTERN.matcher(strToConvert); // need to rebuild because replaceall() replaces object

            Matcher mNumber;
            String ansiGroup;
            int charsRemoved = 0;
            int stringPos = 0;

            ArrayList<StyleInfo> styles = new ArrayList<StyleInfo>();
            boolean brightOn = false;

            while (mAnsi.find())
            {
                int codesFound = 0;

                ansiGroup = mAnsi.group();
                mNumber = NUMBER_PATTERN.matcher(ansiGroup);

                while (mNumber.find())
                {
                    codesFound++;

                    try
                    {
                        int code = Integer.parseInt(mNumber.group());

                        if (code == 0)
                        {
                            // end all active styles
                            for (StyleInfo si : styles)
                            {
                                if (si.end == -1)
                                    si.end = strToConvert.indexOf(ansiGroup, stringPos) - charsRemoved;
                            }
                            brightOn = false;
                        }
                        else if (code == 1)
                        {
                            brightOn = true;
                        }
                        else if (code == 10)
                        {
                            // end text mods
                            for (StyleInfo si : styles)
                            {
                                if (si.style instanceof StyleSpan || si.style instanceof UnderlineSpan
                                        || si.style instanceof StrikethroughSpan)
                                    si.end = strToConvert.indexOf(ansiGroup, stringPos) - charsRemoved;
                            }
                            brightOn = false;
                        }
                        else if (code == 21)
                        {
                            // bright bright
                            brightOn = false;
                        }
                        else if (code == 23)
                        {
                            // end italic
                            for (StyleInfo si : styles)
                            {
                                if (si.style instanceof StyleSpan
                                        && ((StyleSpan) si.style).getStyle() == android.graphics.Typeface.ITALIC)
                                    si.end = strToConvert.indexOf(ansiGroup, stringPos) - charsRemoved;
                            }
                        }
                        else if (code == 24)
                        {
                            // end italic
                            for (StyleInfo si : styles)
                            {
                                if (si.style instanceof UnderlineSpan)
                                    si.end = strToConvert.indexOf(ansiGroup, stringPos) - charsRemoved;
                            }
                        }
                        else if (code == 29)
                        {
                            // end strikethrough
                            for (StyleInfo si : styles)
                            {
                                if (si.style instanceof StrikethroughSpan)
                                    si.end = strToConvert.indexOf(ansiGroup, stringPos) - charsRemoved;
                            }
                        }
                        else if (code == 38)
                        {
                            // color text

                            if (mNumber.find() && Integer.parseInt(mNumber.group()) == 5 && mNumber.find())
                            {
                                int fgColor = Integer.parseInt(mNumber.group());

                                if (fgColor < 256)
                                {
                                    styles.add(new StyleInfo(new ForegroundColorSpan(
                                                                 xtermColors.colorTable[fgColor]), strToConvert.indexOf(ansiGroup,
                                                                         stringPos) - charsRemoved));
                                }
                            }
                        }
                        else if (code == 39 || code == 99)
                        {
                            // end foreground styles
                            for (StyleInfo si : styles)
                            {
                                if (si.style instanceof ForegroundColorSpan)
                                    si.end = strToConvert.indexOf(ansiGroup, stringPos) - charsRemoved;
                            }
                        }
                        else if (code == 48)
                        {
                            // color background

                            if (mNumber.find() && Integer.parseInt(mNumber.group()) == 5 && mNumber.find())
                            {
                                int bgColor = Integer.parseInt(mNumber.group());

                                if (bgColor < 256)
                                {
                                    styles.add(new StyleInfo(new BackgroundColorSpan(
                                                                 xtermColors.colorTable[bgColor]), strToConvert.indexOf(ansiGroup,
                                                                         stringPos) - charsRemoved));
                                }
                            }
                        }
                        else if (code == 49 || code == 109)
                        {
                            // end background styles
                            for (StyleInfo si : styles)
                            {
                                if (si.style instanceof BackgroundColorSpan)
                                    si.end = strToConvert.indexOf(ansiGroup, stringPos) - charsRemoved;
                            }
                        }
                        else
                        {
                            ArrayList<Integer> fakeList = new ArrayList<Integer>(1);
                            fakeList.add(code);
                            CharacterStyle cs = getStyleFromAnsiCode(fakeList, brightOn);

                            if (cs != null)
                            {
                                // create new style
                                styles.add(new StyleInfo(cs, strToConvert.indexOf(ansiGroup, stringPos)
                                                         - charsRemoved));

                            }
                            else
                            {
                                Log.w(TAG, "Unknown ANSI code: " + code);
                            }
                        }
                    }
                    catch (NumberFormatException ex)
                    {
                        Log.w(TAG, "Not a valid ANSI number: " + mNumber.group());
                    }
                }

                if (codesFound == 0)
                {
                    // end all active styles
                    for (StyleInfo si : styles)
                    {
                        if (si.end == -1)
                            si.end = strToConvert.indexOf(ansiGroup, stringPos) - charsRemoved;
                    }
                    brightOn = false;
                }

                stringPos = strToConvert.indexOf(ansiGroup, stringPos) + ansiGroup.length();
                charsRemoved += ansiGroup.length();
            }

            // adjust locations of spans if they weren't ended and add spans to
            // spannable
            for (StyleInfo si : styles)
            {
                if (si.end == -1)
                    si.end = strippedText.length() - 1;

                if (si.start >= 0 && si.start < si.end && si.end >= 1 && si.end < strippedText.length())
                    ansiString.setSpan(si.style, si.start, si.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                else
                    Log.w(TAG, "Bad ANSI span: start: '" + si.start + "' end: '" + si.end + "'");
            }

            returnText = ansiString;
        }
        else
            returnText = strippedText;

        return returnText;
    }

    public static CharacterStyle getStyleFromAnsiCode(ArrayList<Integer> ansiCode, boolean brightOn)
    {
        CharacterStyle returnStyle = null;
        int len = ansiCode.size();

        if (len == 1)
        {
            int color = ansiCode.get(0);

            if (brightOn && ((color >= 30 && color < 38) || (color >= 40 && color < 48)))
                color += 60;

            switch (color)
            {
                // text style
                case 3: // italics
                    returnStyle = new StyleSpan(android.graphics.Typeface.ITALIC);
                    break;
                case 4: // underscore
                    returnStyle = new UnderlineSpan();
                    break;
                case 9: // strikethrough
                    returnStyle = new StrikethroughSpan();
                    break;

                // text colors
                case 30: // black
                    returnStyle = new ForegroundColorSpan(BLACK);
                    break;
                case 31: // red
                    returnStyle = new ForegroundColorSpan(RED);
                    break;
                case 32: // green
                    returnStyle = new ForegroundColorSpan(GREEN);
                    break;
                case 33: // yellow
                    returnStyle = new ForegroundColorSpan(YELLOW);
                    break;
                case 34: // blue
                    returnStyle = new ForegroundColorSpan(BLUE);
                    break;
                case 35: // magenta
                    returnStyle = new ForegroundColorSpan(MAGENTA);
                    break;
                case 36: // cyan
                    returnStyle = new ForegroundColorSpan(CYAN);
                    break;
                case 37: // white
                    returnStyle = new ForegroundColorSpan(GRAY);
                    break;

                // bright text colors
                case 90: // black
                    returnStyle = new ForegroundColorSpan(DARK_GRAY);
                    break;
                case 91: // red
                    returnStyle = new ForegroundColorSpan(BRIGHT_RED);
                    break;
                case 92: // green
                    returnStyle = new ForegroundColorSpan(BRIGHT_GREEN);
                    break;
                case 93: // yellow
                    returnStyle = new ForegroundColorSpan(BRIGHT_YELLOW);
                    break;
                case 94: // blue
                    returnStyle = new ForegroundColorSpan(BRIGHT_BLUE);
                    break;
                case 95: // magenta
                    returnStyle = new ForegroundColorSpan(BRIGHT_MAGENTA);
                    break;
                case 96: // cyan
                    returnStyle = new ForegroundColorSpan(BRIGHT_CYAN);
                    break;
                case 97: // white
                    returnStyle = new ForegroundColorSpan(WHITE);
                    break;

                // bg colors
                case 40: // black
                    returnStyle = new BackgroundColorSpan(BLACK);
                    break;
                case 41: // red
                    returnStyle = new BackgroundColorSpan(RED);
                    break;
                case 42: // green
                    returnStyle = new BackgroundColorSpan(GREEN);
                    break;
                case 43: // yellow
                    returnStyle = new BackgroundColorSpan(YELLOW);
                    break;
                case 44: // blue
                    returnStyle = new BackgroundColorSpan(BLUE);
                    break;
                case 45: // magenta
                    returnStyle = new BackgroundColorSpan(MAGENTA);
                    break;
                case 46: // cyan
                    returnStyle = new BackgroundColorSpan(CYAN);
                    break;
                case 47: // white
                    returnStyle = new BackgroundColorSpan(GRAY);
                    break;

                // bright bg colors
                case 100: // black
                    returnStyle = new BackgroundColorSpan(DARK_GRAY);
                    break;
                case 101: // red
                    returnStyle = new BackgroundColorSpan(BRIGHT_RED);
                    break;
                case 102: // green
                    returnStyle = new BackgroundColorSpan(BRIGHT_GREEN);
                    break;
                case 103: // yellow
                    returnStyle = new BackgroundColorSpan(BRIGHT_YELLOW);
                    break;
                case 104: // blue
                    returnStyle = new BackgroundColorSpan(BRIGHT_BLUE);
                    break;
                case 105: // magenta
                    returnStyle = new BackgroundColorSpan(BRIGHT_MAGENTA);
                    break;
                case 106: // cyan
                    returnStyle = new BackgroundColorSpan(BRIGHT_CYAN);
                    break;
                case 107: // white
                    returnStyle = new BackgroundColorSpan(WHITE);
                    break;
            }
        }
        else if (len == 3 && ansiCode.get(1) == 5)
        {
            int idxZero = ansiCode.get(0);
            int idxTwo = ansiCode.get(2);

            if (idxZero == 38)
            {
                returnStyle = new ForegroundColorSpan(idxTwo);
            }
            else if (idxZero == 48)
            {
                returnStyle = new BackgroundColorSpan(idxTwo);
            }
        }

        return returnStyle;
    }
}
