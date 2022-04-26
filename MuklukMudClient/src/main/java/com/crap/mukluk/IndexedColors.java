package com.crap.mukluk;

import android.graphics.Color;

public class IndexedColors
{
    public int[] colorTable = new int[256];

    public IndexedColors()
    {
        // first 16 colors are light and dark versions of the basic 8 VGA colors
        colorTable[0] = Color.parseColor("#000000");
        colorTable[1] = Color.parseColor("#cd0000");
        colorTable[2] = Color.parseColor("#00cd00");
        colorTable[3] = Color.parseColor("#cdcd00");
        colorTable[4] =  Color.parseColor("#0000ee");
        colorTable[5] = Color.parseColor("#cd00cd");
        colorTable[6] = Color.parseColor("#00cdcd");
        colorTable[7] = Color.parseColor("#e5e5e5");
        colorTable[8] =  Color.parseColor("#7f7f7f");
        colorTable[9] = Color.parseColor("#ff0000");
        colorTable[10] = Color.parseColor("#00ff00");
        colorTable[11] = Color.parseColor("#ffff00");
        colorTable[12] =  Color.parseColor("#5c5cff");
        colorTable[13] = Color.parseColor("#ff00ff");
        colorTable[14] = Color.parseColor("#00ffff");
        colorTable[15] = Color.parseColor("#ffffff");

        // middle 216
        for (int red = 0; red <= 5; red++)
        {
            for (int green = 0; green <= 5; green++)
            {
                for (int blue = 0; blue <= 5; blue++)
                {
                    int index = 16 + (red * 36) + (green * 6) + blue;

                    colorTable[index] = Color.rgb((red * 40) + 55, (green * 40) + 55, (blue * 40) + 55);
                }
            }
        }

        // last 24 are a greyscale ramp
        for (int i = 0; i <= 23; i++)
        {
            int whiteness = (i * 10) + 8;
            colorTable[i + 232] = Color.rgb(whiteness, whiteness, whiteness);
        }
    }
}
