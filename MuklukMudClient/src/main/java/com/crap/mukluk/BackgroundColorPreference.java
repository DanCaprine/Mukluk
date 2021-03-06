package com.crap.mukluk;

import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public class BackgroundColorPreference extends Preference
{
    public static final int defaultBackgroundColor = Color.rgb(0, 0, 0);
    private String foregroundColorKey;
    private String textSizeKey;
    private TextView txtSample;
    private SeekBar redBar;
    private SeekBar greenBar;
    private SeekBar blueBar;

    private final OnSeekBarChangeListener seekBarListener = new OnSeekBarChangeListener()
    {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
        {
            if (fromUser)
            {
                int c = Color.rgb(redBar.getProgress(), greenBar.getProgress(), blueBar.getProgress());
                txtSample.setBackgroundColor(c);
                updatePreference(c);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    };

    public BackgroundColorPreference(Context context)
    {
        super(context);
    }

    public BackgroundColorPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(attrs);
    }

    public BackgroundColorPreference(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs)
    {
        String namespace = "http://schemas.android.com/apk/res/com.crap.mukluk";
        foregroundColorKey = attrs.getAttributeValue(namespace, "foregroundColorKey");
        textSizeKey = attrs.getAttributeValue(namespace, "textSizeKey");
    }

    @Override
    public View onCreateView(ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.color_preference, null);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        int colorVal = prefs.getInt(getKey(), defaultBackgroundColor);

        txtSample = (TextView) view.findViewById(R.id.sample_text);
        txtSample.setBackgroundColor(colorVal);
        txtSample.setTextSize(TypedValue.COMPLEX_UNIT_DIP, prefs.getInt(textSizeKey, TextSizePreference.defaultTextSize));
        txtSample.setTextColor(prefs.getInt(foregroundColorKey, ForegroundColorPreference.defaultForegroundColor));

        redBar = (SeekBar) view.findViewById(R.id.red_bar);
        redBar.setOnSeekBarChangeListener(seekBarListener);
        redBar.setProgress(Color.red(colorVal));

        greenBar = (SeekBar) view.findViewById(R.id.green_bar);
        greenBar.setOnSeekBarChangeListener(seekBarListener);
        greenBar.setProgress(Color.green(colorVal));

        blueBar = (SeekBar) view.findViewById(R.id.blue_bar);
        blueBar.setOnSeekBarChangeListener(seekBarListener);
        blueBar.setProgress(Color.blue(colorVal));

        return view;
    }

    private void updatePreference(int newValue)
    {
        SharedPreferences.Editor editor =  getEditor();
        editor.putInt(getKey(), newValue);
        editor.commit();
    }
}
