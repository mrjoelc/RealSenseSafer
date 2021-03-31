package com.example.testrealsense;

import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import androidx.annotation.NonNull;

public class BottomsheetC {
    private BottomSheetBehavior<LinearLayout> sheetBehavior;
    private LinearLayout bottomSheetLayout;
    protected ImageView bottomSheetArrowImageView;
    private LinearLayout gestureLayout;

    TextView distanceView;
    TextView fps;
    TextView msDetection;
    TextView depthResolution;
    TextView rgbResolution;
    Spinner modelML_spinner;
    Spinner distance_spinner;
    Spinner computation_spinner;
    

    public BottomsheetC(BottomSheetBehavior<LinearLayout> sheetBehavior, LinearLayout bottomSheetLayout, ImageView bottomSheetArrowImageView, LinearLayout gestureLayout) {
        this.sheetBehavior = sheetBehavior;
        this.bottomSheetLayout = bottomSheetLayout;
        this.bottomSheetArrowImageView = bottomSheetArrowImageView;
        this.gestureLayout = gestureLayout;
        bottomSheetBehavior();
    }

    public void bottomSheetBehavior(){
        //bottomsheet
        ViewTreeObserver vto = gestureLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                            gestureLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        } else {
                            gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                        //                int width = bottomSheetLayout.getMeasuredWidth();
                        int height = gestureLayout.getMeasuredHeight();

                        sheetBehavior.setPeekHeight(height);
                    }
                });
        sheetBehavior.setHideable(false);

        sheetBehavior.setBottomSheetCallback(
                new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        switch (newState) {
                            case BottomSheetBehavior.STATE_HIDDEN:
                                break;
                            case BottomSheetBehavior.STATE_EXPANDED:
                            {
                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
                            }
                            break;
                            case BottomSheetBehavior.STATE_COLLAPSED:
                            {
                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                            }
                            break;
                            case BottomSheetBehavior.STATE_DRAGGING:
                                break;
                            case BottomSheetBehavior.STATE_SETTLING:
                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                                break;
                        }
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
                });
    }


}
