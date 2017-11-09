package com.example.administrator.customtab;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.administrator.customtab.CustomTabLayout.CustomTabIndicatorLayout;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {

    private CustomTabIndicatorLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        tabLayout = (CustomTabIndicatorLayout) findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("1"));
        tabLayout.addTab(tabLayout.newTab().setText("1"));
        tabLayout.addTab(tabLayout.newTab().setText("1"));


        // 구조 파악
        // 1. bottomTabLayout- TabLayout
        // 2. bottomTabLayout.getChildAt(0) - TabLayout$SlidingTabStrip(LinearLayout)
        // 3. bottomTabLayout.getChildAt(0).getChildAt(0-4) - TabLayout$TabView
        Log.e("1회", tabLayout.toString());
        Log.e("2회", "총" + tabLayout.getChildCount() + "-" + tabLayout.getChildAt(0).toString());
        for (int i = 0; i < ((LinearLayout) tabLayout.getChildAt(0)).getChildCount(); i++) {
            Log.e("3회", ((LinearLayout) tabLayout.getChildAt(0)).getChildAt(i).toString());
        }
        for (int i = 0; i < (((LinearLayout) ((LinearLayout) tabLayout.getChildAt(0)).getChildAt(0)).getChildCount()); i++) {
            Log.e("4회", (((LinearLayout) ((LinearLayout) tabLayout.getChildAt(0)).getChildAt(0)).getChildAt(i)).toString());
        }
    }

    // the best level technique
    // javascript
    // Closure
    // java
    // Reflection
    public void setIndicator(TabLayout tabs, int leftDip, int rightDip) {
        Class<?> tabLayout = tabs.getClass();
        Field tabStrip = null;
        try {
            tabStrip = tabLayout.getDeclaredField("mTabStrip");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        tabStrip.setAccessible(true);
        LinearLayout llTab = null;
        try {
            llTab = (LinearLayout) tabStrip.get(tabs);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, leftDip, Resources.getSystem().getDisplayMetrics());
        int right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, rightDip, Resources.getSystem().getDisplayMetrics());
        for (int i = 0; i < llTab.getChildCount(); i++) {
            TextView child = (TextView) (((LinearLayout) llTab.getChildAt(i)).getChildAt(1));
            ImageView child2 = (ImageView) (((LinearLayout) llTab.getChildAt(i)).getChildAt(0));
            child.setText("텍스트");
            child.setPadding(0, 0, 0, 0);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
            params.leftMargin = left;
            params.rightMargin = right;
            child.setLayoutParams(params);
            child.invalidate();
        }
    }

    public void setIndicator2(TabLayout tabLayout) {
        try {
            Field field = TabLayout.class.getDeclaredField("mTabStrip");
            field.setAccessible(true);
            Object ob = field.get(tabLayout);
            Class<?> c = Class.forName("android.support.design.widget.TabLayout$SlidingTabStrip");
            Method method = c.getDeclaredMethod("draw", Canvas.class);
            method.setAccessible(true);
            method.invoke(ob, Color.CYAN);//now its ok
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // setIndicator 호출
    public void post(final TabLayout tabLayout) {
        tabLayout.post(new Runnable() {
            @Override
            public void run() {
                setIndicator(tabLayout, 10, 10);
            }
        });
    }

}
