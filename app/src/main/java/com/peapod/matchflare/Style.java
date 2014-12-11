package com.peapod.matchflare;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;



public class Style {

//    static char[] smallCaps = new char[]
//            {
//                    '\uf761', //A
//                    '\uf762',
//                    '\uf763',
//                    '\uf764',
//                    '\uf765',
//                    '\uf766',
//                    '\uf767',
//                    '\uf768',
//                    '\uf769',
//                    '\uf76A',
//                    '\uf76B',
//                    '\uf76C',
//                    '\uf76D',
//                    '\uf76E',
//                    '\uf76F',
//                    '\uf770',
//                    '\uf771',
//                    '\uf772',
//                    '\uf773',
//                    '\uf774',
//                    '\uf775',
//                    '\uf776',
//                    '\uf777',
//                    '\uf778',
//                    '\uf779',
//                    '\uf77A'   //Z
//            };

    public static void makeToast(Activity a, CharSequence myText) {
        LayoutInflater inflater = a.getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_layout,(ViewGroup) a.findViewById(R.id.toast_layout_root));

        TextView text = (TextView) layout.findViewById(R.id.toast_text);
        text.setText(myText);

        toOpenSans(a,text,"light");
        int duration = Toast.LENGTH_SHORT;
        Toast toast = new Toast(a.getApplicationContext());
        toast.setDuration(duration);
        toast.setView(layout);
        toast.show();
    }

    public static void toOpenSans(Context a, View v, String style)
    {
        Typeface typeface;

        if(style.equals("light"))
            typeface = Typeface.createFromAsset(a.getAssets(), "fonts/OpenSans-Light.ttf");
        else if (style.equals("bold"))
            typeface = Typeface.createFromAsset(a.getAssets(), "fonts/OpenSans-Semibold.ttf");
        else
            typeface = Typeface.createFromAsset(a.getAssets(), "fonts/OpenSans-Regular.ttf");

        ((TextView) v).setTypeface(typeface);
    }



//    public static String toSmallCaps(String input) {
//        char[] chars = input.toCharArray();
//        for(int i = 0; i < chars.length; i++) {
//            if(chars[i] >= 'a' && chars[i] <= 'z') {
//                chars[i] = smallCaps[chars[i] - 'a'];
//            }
//        }
//        return String.valueOf(chars);
//    }
}

