package com.crap.mukluk;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class TextSizePreference extends Preference
{
    public static final int defaultTextSize = 12;
    private String foregroundColorKey;
    private String backgroundColorKey;
    private TextView txtSample;
    private SeekBar barText;
    private Button btnDefault;

    public TextSizePreference(Context context)
    {
        super(context);
    }

    public TextSizePreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(attrs);
    }

    public TextSizePreference(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs)
    {
        String namespace = "http://schemas.android.com/apk/res/com.crap.mukluk";
        foregroundColorKey = attrs.getAttributeValue(namespace, "foregroundColorKey");
        backgroundColorKey = attrs.getAttributeValue(namespace, "backgroundColorKey");
    }

    @Override
    public View onCreateView(ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.text_size_preference, null);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        int textSize = prefs.getInt(getKey(), defaultTextSize);

        txtSample = (TextView) view.findViewById(R.id.sample_text);
        txtSample.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize);
        txtSample.setBackgroundColor(prefs.getInt(backgroundColorKey, BackgroundColorPreference.defaultBackgroundColor));
        txtSample.setTextColor(prefs.getInt(foregroundColorKey, ForegroundColorPreference.defaultForegroundColor));

        barText = (SeekBar) view.findViewById(R.id.text_size_bar);
        barText.setProgress(textSize);
        barText.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                if (fromUser)
                {
                    int val = barText.getProgress();

                    txtSample.setTextSize(TypedValue.COMPLEX_UNIT_DIP, val);
                    updatePreference(val);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnDefault = (Button) view.findViewById(R.id.button_default_text_size);
        btnDefault.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                int val = defaultTextSize;

                barText.setProgress(val);
                txtSample.setTextSize(TypedValue.COMPLEX_UNIT_DIP, val);
                updatePreference(val);
            }
        });

        return view;
    }

    private void updatePreference(int newValue)
    {
        SharedPreferences.Editor editor =  getEditor();
        editor.putInt(getKey(), newValue);
        editor.commit();
    }
}
