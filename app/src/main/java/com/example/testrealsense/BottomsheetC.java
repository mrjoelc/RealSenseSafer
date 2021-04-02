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

    public void setContentBottomSheet(TextView fps,
                                      TextView msDetection,
                                      TextView depthResolution,
                                      TextView rgbResolution,
                                      Spinner modelML_spinner,
                                      Spinner distance_spinner,
                                      Spinner computation_spinner){
        this.fps = fps;
        this.msDetection = msDetection;
        this.depthResolution = depthResolution;
        this.rgbResolution  = rgbResolution;
        this.modelML_spinner = modelML_spinner;
        this.distance_spinner = distance_spinner;
        this.computation_spinner = computation_spinner;
    }

    public TextView getDistanceView() {
        return distanceView;
    }

    public void setDistanceView(TextView distanceView) {
        this.distanceView = distanceView;
    }

    public TextView getFps() {
        return fps;
    }

    public void setFps(TextView fps) {
        this.fps = fps;
    }

    public TextView getMsDetection() {
        return msDetection;
    }

    public void setMsDetection(TextView msDetection) {
        this.msDetection = msDetection;
    }

    public TextView getDepthResolution() {
        return depthResolution;
    }

    public void setDepthResolution(TextView depthResolution) {
        this.depthResolution = depthResolution;
    }

    public TextView getRgbResolution() {
        return rgbResolution;
    }

    public void setRgbResolution(TextView rgbResolution) {
        this.rgbResolution = rgbResolution;
    }

    public Spinner getModelML_spinner() {
        return modelML_spinner;
    }

    public void setModelML_spinner(Spinner modelML_spinner) {
        this.modelML_spinner = modelML_spinner;
    }

    public Spinner getDistance_spinner() {
        return distance_spinner;
    }

    public void setDistance_spinner(Spinner distance_spinner) {
        this.distance_spinner = distance_spinner;
    }

    public Spinner getComputation_spinner() {
        return computation_spinner;
    }

    public void setComputation_spinner(Spinner computation_spinner) {
        this.computation_spinner = computation_spinner;
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
