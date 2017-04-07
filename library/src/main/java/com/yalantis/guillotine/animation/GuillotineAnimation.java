package com.yalantis.guillotine.animation;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.res.Resources;
import android.os.Build;
import android.support.v7.widget.AppCompatTextView;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yalantis.guillotine.interfaces.GuillotineListener;
import com.yalantis.guillotine.util.ActionBarInterpolator;
import com.yalantis.guillotine.util.GuillotineInterpolator;

/**
 * Created by Dmytro Denysenko on 5/6/15.
 */
public class GuillotineAnimation {
    private static final String ROTATION = "rotation";
    private static final float GUILLOTINE_CLOSED_ANGLE = -90f;
    private static final float GUILLOTINE_OPENED_ANGLE = 0f;
    private static final int DEFAULT_DURATION = 625;
    private static final float ACTION_BAR_ROTATION_ANGLE = 3f;
    private static final int MIN_GESTURE_DISTANCE = 50;

    private final View mGuillotineView;
    private final long mDuration;
    private final ObjectAnimator mOpeningAnimation;
    private final ObjectAnimator mClosingAnimation;
    private final GuillotineListener mListener;
    private final boolean mIsRightToLeftLayout;
    private final TimeInterpolator mInterpolator;
    private final View mActionBarView;
    private final long mDelay;

    private boolean isOpening;
    private boolean isClosing;
    private boolean isClosed;

    private TextView titleTextView;

    private GuillotineAnimation(GuillotineBuilder builder) {
        this.mActionBarView = builder.actionBarView;
        //  Add view behind action bar
        ViewParent parent = this.mActionBarView.getParent();
        View view = new View(this.mActionBarView.getContext());
        view.setBackground(this.mActionBarView.getBackground());
        ViewGroup.LayoutParams viewParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 70);
        view.setLayoutParams(viewParams);
        if (parent instanceof LinearLayout) {
            ViewParent grandparentView = parent.getParent();
            if (grandparentView instanceof FrameLayout) {
                ((FrameLayout) grandparentView).addView(view, 0);
            }
        }

        this.mListener = builder.guillotineListener;
        this.mGuillotineView = builder.guillotineView;
        this.mDuration = builder.duration > 0 ? builder.duration : DEFAULT_DURATION;
        this.mDelay = builder.startDelay;
        this.mIsRightToLeftLayout = builder.isRightToLeftLayout;
        this.mInterpolator = builder.interpolator == null ? new GuillotineInterpolator() : builder.interpolator;
        setUpOpeningView(builder.openingView);
        setUpClosingView(builder.closingView);
        this.mOpeningAnimation = buildOpeningAnimation();
        this.mClosingAnimation = buildClosingAnimation();
        if (builder.isClosedOnStart) {
            isClosed = true;
            mGuillotineView.setRotation(GUILLOTINE_CLOSED_ANGLE);
            mGuillotineView.setVisibility(View.INVISIBLE);
        }
        //TODO handle right-to-left layouts
        //TODO handle landscape orientation

        //  Add events for gestures
        this.mActionBarView.setOnTouchListener(new View.OnTouchListener() {
            private float y1,y2;
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch(motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        y1 = motionEvent.getY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        y2 = motionEvent.getY();
                        float deltaX = y1 - y2;
                        if (Math.abs(deltaX) > MIN_GESTURE_DISTANCE) {
                            open();
                        }
                        break;
                }
                return false;
            }
        });

        this.mGuillotineView.setOnTouchListener(new View.OnTouchListener() {
            private float x1,x2;
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch(motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x1 = motionEvent.getX();
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (x1 < 70) {
                            x2 = motionEvent.getX();
                            float deltaX = x2 - x1;
                            if (Math.abs(deltaX) > MIN_GESTURE_DISTANCE) {
                                close();
                            }
                        }
                        break;
                }
                return false;
            }
        });
    }

    public void open() {
        if (!isOpening) {
            addTitleActionBarToGuillotineView();
            mOpeningAnimation.start();
        }
    }

    public void close() {
        if (!isClosing) {
            if (titleTextView != null) {
                titleTextView.animate().setDuration(mDelay).alpha(1).start();
            }
            mClosingAnimation.start();
        }
    }

    public boolean isClosed() {
        return isClosed;
    }

    private void setUpOpeningView(final View openingView) {
        if (mActionBarView != null) {
            mActionBarView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        mActionBarView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        mActionBarView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                    mActionBarView.setPivotX(calculatePivotX(openingView));
                    mActionBarView.setPivotY(calculatePivotY(openingView));
                }
            });
        }
        openingView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open();
            }
        });
    }

    private void setUpClosingView(final View closingView) {
        mGuillotineView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mGuillotineView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    mGuillotineView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                mGuillotineView.setPivotX(calculatePivotX(closingView));
                mGuillotineView.setPivotY(calculatePivotY(closingView));
            }
        });

        closingView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                close();
            }
        });
    }

    private ObjectAnimator buildOpeningAnimation() {
        ObjectAnimator rotationAnimator = initAnimator(ObjectAnimator.ofFloat(mGuillotineView, ROTATION, GUILLOTINE_CLOSED_ANGLE, GUILLOTINE_OPENED_ANGLE));
        //rotationAnimator.setInterpolator(mInterpolator);
        //rotationAnimator.setDuration(mDuration);
        rotationAnimator.setDuration(425);
        rotationAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mGuillotineView.setVisibility(View.VISIBLE);
                mActionBarView.setVisibility(View.INVISIBLE);
                isOpening = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isOpening = false;
                if (mListener != null) {
                    mListener.onGuillotineOpened();
                }
                isClosed = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        return rotationAnimator;
    }

    private ObjectAnimator buildClosingAnimation() {
        ObjectAnimator rotationAnimator = initAnimator(ObjectAnimator.ofFloat(mGuillotineView, ROTATION, GUILLOTINE_OPENED_ANGLE, GUILLOTINE_CLOSED_ANGLE));
        //rotationAnimator.setDuration((long) (mDuration * GuillotineInterpolator.ROTATION_TIME));
        rotationAnimator.setDuration(290);
        rotationAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                isClosing = true;
                mGuillotineView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isClosing = false;
                mActionBarView.setVisibility(View.VISIBLE);
                mActionBarView.setAlpha(1);
                mGuillotineView.setVisibility(View.GONE);
                removeTitleActionBarFromGuillotineView();
                //startActionBarAnimation();

                isClosed = true;
                if (mListener != null) {
                    mListener.onGuillotineClosed();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        return rotationAnimator;
    }

    private void startActionBarAnimation() {
        ObjectAnimator actionBarAnimation = ObjectAnimator.ofFloat(mActionBarView, ROTATION, GUILLOTINE_OPENED_ANGLE, ACTION_BAR_ROTATION_ANGLE);
        actionBarAnimation.setDuration((long) (mDuration * (GuillotineInterpolator.FIRST_BOUNCE_TIME + GuillotineInterpolator.SECOND_BOUNCE_TIME)));
        actionBarAnimation.setInterpolator(new ActionBarInterpolator());
        actionBarAnimation.start();
    }

    private ObjectAnimator initAnimator(ObjectAnimator animator) {
        //animator.setStartDelay(mDelay);
        return animator;
    }

    private void addTitleActionBarToGuillotineView() {
        if (mActionBarView instanceof android.support.v7.widget.Toolbar) {
            android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) mActionBarView;
            for (int i = 0; i < toolbar.getChildCount(); i++) {
                View v = toolbar.getChildAt(i);
                if (v instanceof TextView || v instanceof AppCompatTextView) {
                    titleTextView = (TextView) v;
                    float yPosition = titleTextView.getY();
                    float marginBottom = ((titleTextView.getWidth() - titleTextView.getHeight()) / 2) - yPosition ;

                    ((android.support.v7.widget.Toolbar) mActionBarView).removeView(titleTextView);
                    ((RelativeLayout) mGuillotineView).addView(titleTextView);

                    DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
                    int width = metrics.widthPixels;
                    int center = width/2;
                    int position = center - this.titleTextView.getWidth()/2;

                    titleTextView.setTranslationX(-marginBottom);
                    titleTextView.setY(position + (this.titleTextView.getWidth() - this.titleTextView.getHeight())/2);
                    titleTextView.setRotation(90);
                }
            }
        }
        if (titleTextView != null) {
            titleTextView.animate().setDuration(mDelay).alpha(0).start();
        }
    }

    private void removeTitleActionBarFromGuillotineView() {
        if (titleTextView != null) {
            titleTextView.setRotation(0);
            ((RelativeLayout) mGuillotineView).removeView(titleTextView);

            android.support.v7.widget.Toolbar myToolbar = (android.support.v7.widget.Toolbar) mActionBarView;

            float leftMargin = 0;
            for (int j = 0; j < myToolbar.getChildCount(); j++) {
                View v2 = ((android.support.v7.widget.Toolbar) mActionBarView).getChildAt(j);
                leftMargin += v2.getWidth();
            }
            ((android.support.v7.widget.Toolbar) mActionBarView).addView(titleTextView);

            DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
            int width = metrics.widthPixels;

            ViewGroup.LayoutParams layoutParams = myToolbar.getLayoutParams();
            layoutParams.width = width + 100;
            myToolbar.setLayoutParams(layoutParams);

            int center = width/2;
            int position = center - this.titleTextView.getWidth()/2;
            titleTextView.setX(position - leftMargin);
            titleTextView.setTranslationY(0);
        }
    }

    private float calculatePivotY(View burger) {
        return burger.getTop() + burger.getHeight() / 2;
    }

    private float calculatePivotX(View burger) {
        return burger.getLeft() + burger.getWidth() / 2;
    }

    public static class GuillotineBuilder {
        private final View guillotineView;
        private final View openingView;
        private final View closingView;
        private View actionBarView;
        private GuillotineListener guillotineListener;
        private long duration;
        private long startDelay;
        private boolean isRightToLeftLayout;
        private TimeInterpolator interpolator;
        private boolean isClosedOnStart;

        public GuillotineBuilder(View guillotineView, View closingView, View openingView) {
            this.guillotineView = guillotineView;
            this.openingView = openingView;
            this.closingView = closingView;
        }

        public GuillotineBuilder setActionBarViewForAnimation(View view) {
            this.actionBarView = view;
            return this;
        }

        public GuillotineBuilder setGuillotineListener(GuillotineListener guillotineListener) {
            this.guillotineListener = guillotineListener;
            return this;
        }

        public GuillotineBuilder setDuration(long duration) {
            this.duration = duration;
            return this;
        }

        public GuillotineBuilder setStartDelay(long startDelay) {
            this.startDelay = startDelay;
            return this;
        }

        public GuillotineBuilder setRightToLeftLayout(boolean isRightToLeftLayout) {
            this.isRightToLeftLayout = isRightToLeftLayout;
            return this;
        }

        public GuillotineBuilder setInterpolator(TimeInterpolator interpolator) {
            this.interpolator = interpolator;
            return this;
        }

        public GuillotineBuilder setClosedOnStart(boolean isClosedOnStart) {
            this.isClosedOnStart = isClosedOnStart;
            return this;
        }

        public GuillotineAnimation build() {
            return new GuillotineAnimation(this);
        }
    }
}
