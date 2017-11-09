# CustomTabIndicator
탭레이아웃 Indicator 커스터마이징

![](https://github.com/qskeksq/CustomTabIndicatorLayout/blob/master/pic/AC_%5B20171109-105450%5D.gif)

### 커스터마이징 과정

1. setCustomView() : 뷰2개를 갈아 끼우는 형식이기 때문에 indicator가 부드럽게 움직이지 않는다
2. 상속 :  extends TabLayout 할 경우 탭을 그려주는 핵심 클래스인 SlidingTabStrip이 private이기 때문에 오버라이드 불가
3. Reflection : indicator는 위젯(or 레이아웃)이 아니라 canvas에 그려지는 동적인 '그림'이다. 따라서 Reflection을 통해 declaredField, declaredMethod로 메서드나 변수에 접근한다 하더라도 indicator가 그려지는 핵심 메서드인 draw(Canvas canvas)를 시스템에서 호출하기 때문에 public으로 설정된 indicator의 속성인 높이, 두께, 색상 이외에는 커스터마이징 할 수 없다
4. 직접 구현 :  따라서 오픈소스로 되어 있는 TabLayout을 전부 복사해서 라이브러리를 따로 만들고, 그 중 draw()부분을 customize 해 주면 된다.
- 참고1. TintManager()가 deprecated가 되었기 때문에 일단 getIcon 또한 바꿔줘야 한다
- 참고2. private영역으로 선언된 클래스가 상당히 많기 때문에 전부 외부 클래스로 복사해줘야 한다

### 뷰 하위구조 파악하기

```java
// 구조
// 1. bottomTabLayout- TabLayout
// 2. bottomTabLayout.getChildAt(0) - TabLayout$SlidingTabStrip(LinearLayout)
// 3. bottomTabLayout.getChildAt(0).getChildAt(0-4) - TabLayout$TabView
Log.e("1회", bottomTabLayout.toString());
Log.e("2회", "총"+bottomTabLayout.getChildCount()+"-"+bottomTabLayout.getChildAt(0).toString());
for (int i = 0; i < ((LinearLayout)bottomTabLayout.getChildAt(0)).getChildCount(); i++) {
    Log.e("3회", ((LinearLayout)bottomTabLayout.getChildAt(0)).getChildAt(i).toString());
}
for (int i = 0; i < (((LinearLayout)((LinearLayout)bottomTabLayout.getChildAt(0)).getChildAt(0)).getChildCount())  ; i++) {
    Log.e("4회", (((LinearLayout)((LinearLayout)bottomTabLayout.getChildAt(0)).getChildAt(0)).getChildAt(i)).toString());
}
```

### Reflection 시도

> try 1

```java
public void setIndicator2(TabLayout tabLayout){
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
    } catch (ClassNotFoundException e){
        e.printStackTrace();
    }
}
```

> try 2

```java
public void setIndicator (TabLayout tabs, int leftDip, int rightDip){

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

    // 구조를 파악한 결과 SlidingTabLayout은 indicator를 뜻하는 게 아니라 탭 레이아웃에 포함되는 뷰 전체이다
    for (int i = 0; i < llTab.getChildCount(); i++) {
        TextView child = (TextView) (((LinearLayout) llTab.getChildAt(i)).getChildAt(1));
        ImageView child2 = (ImageView) (((LinearLayout) llTab.getChildAt(i)).getChildAt(0));
        child.setText("텍스트");
        child2.setImageResource(R.drawable.top_indicator);
        child.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
        params.leftMargin = left;
        params.rightMargin = right;
        child.setLayoutParams(params);
        child.invalidate();
    }
}
```

> 호출

```java
// setIndicator 호출
public void post(final TabLayout tabLayout){
    tabLayout.post(new Runnable() {
        @Override
        public void run() {
            setIndicator(tabLayout, 10, 10);
        }
    });
}
```

### SlidingTabIndicator 오픈소스 코드 이용

- draw()영역에서 canvas에 drawBitmap을 할 경우 자원이 극도로 낭비될 수 있다.

```java
private class SlidingTabStrip extends LinearLayout {
    private int mSelectedIndicatorHeight;
    private final Paint mSelectedIndicatorPaint;

    private int mSelectedPosition = -1;
    private float mSelectionOffset;

    private int mIndicatorLeft = -1;
    private int mIndicatorRight = -1;

    private ValueAnimatorCompat mCurrentAnimator;

    SlidingTabStrip(Context context) {
        super(context);
        setWillNotDraw(false);
        mSelectedIndicatorPaint = new Paint();
    }

    void setSelectedIndicatorColor(int color) {
        if (mSelectedIndicatorPaint.getColor() != color) {
            mSelectedIndicatorPaint.setColor(color);
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    void setSelectedIndicatorHeight(int height) {
        if (mSelectedIndicatorHeight != height) {
            mSelectedIndicatorHeight = height;
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    boolean childrenNeedLayout() {
        for (int i = 0, z = getChildCount(); i < z; i++) {
            final View child = getChildAt(i);
            if (child.getWidth() <= 0) {
                return true;
            }
        }
        return false;
    }

    void setIndicatorPositionFromTabPosition(int position, float positionOffset) {
        mSelectedPosition = position;
        mSelectionOffset = positionOffset;
        updateIndicatorPosition();
    }

    float getIndicatorPosition() {
        return mSelectedPosition + mSelectionOffset;
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
            // HorizontalScrollView will first measure use with UNSPECIFIED, and then with
            // EXACTLY. Ignore the first call since anything we do will be overwritten anyway
            return;
        }

        if (mMode == MODE_FIXED && mTabGravity == GRAVITY_CENTER) {
            final int count = getChildCount();

            // First we'll find the widest tab
            int largestTabWidth = 0;
            for (int i = 0, z = count; i < z; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == VISIBLE) {
                    largestTabWidth = Math.max(largestTabWidth, child.getMeasuredWidth());
                }
            }

            if (largestTabWidth <= 0) {
                // If we don't have a largest child yet, skip until the next measure pass
                return;
            }

            final int gutter = dpToPx(FIXED_WRAP_GUTTER_MIN);
            boolean remeasure = false;

            if (largestTabWidth * count <= getMeasuredWidth() - gutter * 2) {
                // If the tabs fit within our width minus gutters, we will set all tabs to have
                // the same width
                for (int i = 0; i < count; i++) {
                    final LayoutParams lp =
                            (LayoutParams) getChildAt(i).getLayoutParams();
                    if (lp.width != largestTabWidth || lp.weight != 0) {
                        lp.width = largestTabWidth;
                        lp.weight = 0;
                        remeasure = true;
                    }
                }
            } else {
                // If the tabs will wrap to be larger than the width minus gutters, we need
                // to switch to GRAVITY_FILL
                mTabGravity = GRAVITY_FILL;
                updateTabViews(false);
                remeasure = true;
            }

            if (remeasure) {
                // Now re-measure after our changes
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (mCurrentAnimator != null && mCurrentAnimator.isRunning()) {
            // If we're currently running an animation, lets cancel it and start a
            // new animation with the remaining duration
            mCurrentAnimator.cancel();
            final long duration = mCurrentAnimator.getDuration();
            animateIndicatorToPosition(mSelectedPosition,
                    Math.round((1f - mCurrentAnimator.getAnimatedFraction()) * duration));
        } else {
            // If we've been layed out, update the indicator position
            updateIndicatorPosition();
        }
    }

    private void updateIndicatorPosition() {
        final View selectedTitle = getChildAt(mSelectedPosition);
        int left, right;

        if (selectedTitle != null && selectedTitle.getWidth() > 0) {
            left = selectedTitle.getLeft();
            right = selectedTitle.getRight();
            Log.e("selectedTitle", left+"");
            Log.e("selectedTitle", right+"");

            if (mSelectionOffset > 0f && mSelectedPosition < getChildCount() - 1) {
                // Draw the selection partway between the tabs
                View nextTitle = getChildAt(mSelectedPosition + 1);
                left = (int) (mSelectionOffset * nextTitle.getLeft() +
                        (1.0f - mSelectionOffset) * left) + 204;
                right = (int) (mSelectionOffset * nextTitle.getRight() +
                        (1.0f - mSelectionOffset) * right);
            }
        } else {
            left = right = -1;
        }
        setIndicatorPosition(left, right);
    }

    private void setIndicatorPosition(int left, int right) {
        if (left != mIndicatorLeft || right != mIndicatorRight) {
            // If the indicator's left/right has changed, invalidate
            mIndicatorLeft = left;
            mIndicatorRight = right;
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    void animateIndicatorToPosition(final int position, int duration) {
        final boolean isRtl = ViewCompat.getLayoutDirection(this)
                == ViewCompat.LAYOUT_DIRECTION_RTL;

        final View targetView = getChildAt(position);
        final int targetLeft = targetView.getLeft();
        final int targetRight = targetView.getRight();
        final int startLeft;
        final int startRight;

        if (Math.abs(position - mSelectedPosition) <= 1) {
            // If the views are adjacent, we'll animate from edge-to-edge
            startLeft = mIndicatorLeft;
            startRight = mIndicatorRight;
        } else {
            // Else, we'll just grow from the nearest edge
            final int offset = dpToPx(MOTION_NON_ADJACENT_OFFSET);
            if (position < mSelectedPosition) {
                // We're going end-to-start
                if (isRtl) {
                    startLeft = startRight = targetLeft - offset;
                } else {
                    startLeft = startRight = targetRight + offset;
                }
            } else {
                // We're going start-to-end
                if (isRtl) {
                    startLeft = startRight = targetRight + offset;
                } else {
                    startLeft = startRight = targetLeft - offset;
                }
            }
        }

        if (startLeft != targetLeft || startRight != targetRight) {
            ValueAnimatorCompat animator = mIndicatorAnimator = ViewUtils.createAnimator();
            animator.setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR);
            animator.setDuration(duration);
            animator.setFloatValues(0, 1);
            animator.setUpdateListener(new ValueAnimatorCompat.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimatorCompat animator) {
                    final float fraction = animator.getAnimatedFraction();
                    setIndicatorPosition(
                            AnimationUtils.lerp(startLeft, targetLeft, fraction),
                            AnimationUtils.lerp(startRight, targetRight, fraction));
                }
            });
            animator.setListener(new ValueAnimatorCompat.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(ValueAnimatorCompat animator) {
                    mSelectedPosition = position;
                    mSelectionOffset = 0f;
                }

                @Override
                public void onAnimationCancel(ValueAnimatorCompat animator) {
                    mSelectedPosition = position;
                    mSelectionOffset = 0f;
                }
            });
            animator.start();
            mCurrentAnimator = animator;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        Log.e("좌", mIndicatorLeft+"");      // <- 호출되는 좌, 우 좌표가 TabView의 위치에 따라 유동적으로 움직인다
        Log.e("우", mIndicatorRight+"");

        int temp_width = getChildAt(0).getWidth()/5*2;
        if (mIndicatorLeft >= 0 && mIndicatorRight > mIndicatorLeft) {
            canvas.drawRoundRect(
                    mIndicatorLeft + temp_width,                // 좌
                    getHeight() - mSelectedIndicatorHeight*2,   // 상
                    mIndicatorRight - temp_width,               // 우
                    getHeight(),                                // 좌
                    getHeight(), getHeight(),                   // radius
                    mSelectedIndicatorPaint);                   // paint
            //Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.indicator_top);
            //bitmap = Bitmap.createScaledBitmap(bitmap, 72, 32, true);
            //canvas.drawBitmap(bitmap, /*width*mSelectedPosition+width/2-36*/  mIndicatorLeft+204 , getHeight() - 32 , mSelectedIndicatorPaint);
        }
    }

}
```