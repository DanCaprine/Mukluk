package com.crap.mukluk;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.LinkedList;

import org.codehaus.jackson.map.ObjectMapper;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.util.Log;

public class ConsoleTextHistory
{
    private static final String TAG = "ConsoleTextHistory";
    private static final ObjectMapper mapper = new ObjectMapper();

    private LinkedList<SpannableStringBuilder> _items;
    private int _maximumSize;
    private boolean _lastLineComplete; // does previous line end with \n

    public ConsoleTextHistory(int maxLength)
    {
        _items = new LinkedList<SpannableStringBuilder>();
        _maximumSize = maxLength;
        _lastLineComplete = false;
    }

    public void add(CharSequence characters)
    {
        if (_items.size() == 0 || _lastLineComplete)
        {
            _items.add(new SpannableStringBuilder(characters));
            adjustSize();
        }
        else
            _items.getLast().append(characters);

        if (characters.length() == 0)
            _lastLineComplete = false;
        else
            _lastLineComplete = (characters.charAt(characters.length() - 1) == '\n');
    }

    public void setMaximumSize(int newMaximumSize)
    {
        _maximumSize = newMaximumSize;

        adjustSize();
    }

    // styleStream may be null if file doesn't exist
    public SpannableStringBuilder loadFromFile(FileInputStream fileStream, FileInputStream styleJacksonStream)
    {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        BufferedReader br = null;

        try
        {
            String line;
            br = new BufferedReader(new InputStreamReader(fileStream));
            //int lineNum = 0;

            try
            {
                while ((line = br.readLine()) != null)
                {
                    line += "\n";

                    add(line);
                    //lineNum++;
                }
            }
            catch (IOException ex)
            {
                Log.e(TAG, "IOException trying to load console text history for world.");
                Log.e(TAG, ex.toString());
            }
        }
        finally
        {
            try
            {
                br.close();
            }
            catch (Exception ex) {}
        }

        LineStyleInfo[] lsiList = null;


        if (styleJacksonStream != null)
        {
            try
            {
                lsiList = mapper.readValue(styleJacksonStream, LineStyleInfo[].class);
            }
            catch (Exception ex)
            {
                Log.e(TAG, "Error occured reading Jackson JSON file: " + ex.toString());
            }
        }

        if (lsiList != null)
        {
            for (LineStyleInfo lsi : lsiList)
            {
                if (lsi.lineNumber < _items.size() && lsi.start >= 0
                        && lsi.end < _items.get(lsi.lineNumber).length())
                {
                    _items.get(lsi.lineNumber).setSpan(AnsiUtility.getStyleFromAnsiCode(lsi.ansiCode, false),
                                                       lsi.start, lsi.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                else
                {
                    Log.w(TAG, "Invalid span in history: " + "Line: " + lsi.lineNumber + " Start: "
                          + lsi.start + " End: " + lsi.end);

                    if (lsi.lineNumber > _items.size())
                        Log.w(TAG, _items.size() + " lines in history, span is for line " + lsi.lineNumber);
                    else if (lsi.start < 0)
                        Log.w(TAG, "span started at character " + lsi.start);
                    else if (lsi.end >= _items.get(lsi.lineNumber).length())
                        Log.w(TAG,
                              "span ends at " + lsi.end + " but line is only "
                              + _items.get(lsi.lineNumber).length());
                }
            }
        }

        for (SpannableStringBuilder ssb : _items)
            sb.append(ssb);

        return sb;
    }

    // Returns whether or not there was anything to write
    public boolean writeToFiles(FileOutputStream textFileStream, FileOutputStream styleJacksonStream)
    {
        if (_items.size() > 0)
        {
            PrintWriter textPw = null;
            LinkedList<LineStyleInfo> allStyles = new LinkedList<LineStyleInfo>();

            try
            {
                textPw = new PrintWriter(textFileStream);

                int lineNum = 0;
                String s;
                CharacterStyle[] lineSpans;

                for (SpannableStringBuilder ssb : _items)
                {
                    s = ssb.toString();
                    textPw.print(s);

                    lineSpans = ssb.getSpans(0, s.length() - 1, CharacterStyle.class);

                    for (CharacterStyle cs : lineSpans)
                    {
                        LineStyleInfo lsi = new LineStyleInfo();

                        lsi.lineNumber = lineNum;
                        lsi.start = ssb.getSpanStart(cs);
                        lsi.end = ssb.getSpanEnd(cs);
                        lsi.ansiCode = AnsiUtility.getAnsiCodeFromStyle(cs);

                        allStyles.add(lsi);
                    }

                    lineNum++;
                }
            }
            finally
            {
                try
                {
                    textPw.close();
                }
                catch (Exception ex) {}
            }

            try
            {
                LineStyleInfo[] lsiArray = allStyles.toArray(new LineStyleInfo[0]);
                mapper.writeValue(styleJacksonStream, lsiArray);
            }
            catch (Exception ex)
            {
                Log.e(TAG, "Error writing jackson JSON to file: " + ex.toString());
            }

            return true;
        }
        else
            return false;
    }

    private void adjustSize()
    {
        int currentSize = _items.size();
        int diff = currentSize - _maximumSize;

        // Clear list
        if (diff > 0)
            _items.subList(0, currentSize - _maximumSize).clear();
    }
}
