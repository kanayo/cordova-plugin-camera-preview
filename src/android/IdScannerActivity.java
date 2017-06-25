package com.kanayo.cordova.idscanner;

import android.app.Dialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.Fragment;
import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.text.TextRecognizer;
import com.kanayo.cordova.idscanner.camera.CameraSource;
import com.kanayo.cordova.idscanner.camera.CameraSourcePreview;
import com.kanayo.cordova.idscanner.camera.GraphicOverlay;
import com.kanayo.cordova.idscanner.ocr.OcrDetectorProcessor;
import com.kanayo.cordova.idscanner.ocr.OcrGraphic;

import java.io.IOException;

@SuppressWarnings("deprecation")
public class IdScannerActivity extends Fragment {

    private static final String TAG = "IdScannerActivity";

    // Intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<OcrGraphic> mGraphicOverlay;

    // Helper objects for detecting taps and pinches.
    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mGestureDetector;

    private IScanController mControllerListener;
    private View mView;
    private String mAppResourcesPackage;

    interface IScanController {
        void changeCameraDirection();
        void onScanFinished(String message);
        OcrDetectorProcessor.VerifyResult verifyScannedText(String text );
    }

    public void setControllerListener(IScanController listener) {
        mControllerListener = listener;
    }

    Camera getCamera() {
        return mCameraSource.getCamera();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mAppResourcesPackage = getActivity().getPackageName();

        // Inflate the layout for this fragment
        mView = inflater.inflate(getResources().getIdentifier("id_scanner_activity", "layout", mAppResourcesPackage), container, false);
        createCameraPreview();
        return mView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void createCameraPreview() {
        if (mPreview == null) {

            mPreview = (CameraSourcePreview) mView.findViewById(getResources().getIdentifier("camera_preview", "id", mAppResourcesPackage));
            mGraphicOverlay = (GraphicOverlay<OcrGraphic>) mView.findViewById(getResources().getIdentifier("graphic_overlay", "id", mAppResourcesPackage));

            final Bundle args = this.getArguments();

            Button cancelButton = (Button) mView.findViewById(getResources().getIdentifier("cancel_scan", "id", mAppResourcesPackage));
            cancelButton.setText(args.getString(IdScanner.CANCEL_TEXT_PARAM));
            cancelButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    mControllerListener.onScanFinished("");
                }
            });

            Button switchButton = (Button) mView.findViewById(getResources().getIdentifier("switch_cameras", "id", mAppResourcesPackage));
            switchButton.setText(args.getString(IdScanner.SWITCH_TEXT_PARAM));
            switchButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    mControllerListener.changeCameraDirection();
                }
            });

            int direction = CameraSource.CAMERA_FACING_BACK;
            if (args.getString(IdScanner.CAMERA_DIRECTION_PARAM).equals( IdScanner.CAMERA_DIRECTION_FRONT) ) {
                direction = CameraSource.CAMERA_FACING_FRONT;
            }

            createCameraSource(true, false, direction);

            mGestureDetector = new GestureDetector(getActivity().getApplicationContext(), new CaptureGestureListener());
            mScaleGestureDetector = new ScaleGestureDetector(getActivity().getApplicationContext(), new ScaleListener());

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mPreview.setClickable(true);
                    mPreview.setOnTouchListener(new View.OnTouchListener() {

                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            boolean b = mScaleGestureDetector.onTouchEvent(event);

                            boolean c = mGestureDetector.onTouchEvent(event);

                            return b || c;
                        }

                    });

                    showInstructions();
                }
            });
        }
    }

    private void showInstructions() {
        Bundle args = this.getArguments();
        Toast.makeText(getActivity(), args.getString(IdScanner.INSTRUCTIONS_PARAM), Toast.LENGTH_LONG).show();
    }


    private void createCameraSource(boolean autoFocus, boolean useFlash, int direction) {
        Context context = getActivity().getApplicationContext();

        // A text recognizer is created to find text.  An associated multi-processor instance
        // is set to receive the text recognition results, track the text, and maintain
        // graphics for each text block on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each text block.
        TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();
        textRecognizer.setProcessor(new OcrDetectorProcessor(mGraphicOverlay, new OcrDetectorProcessor.IdScanListener() {
            @Override
            public void onScanFinished(String message) {
                mControllerListener.onScanFinished(message);
            }

            @Override
            public OcrDetectorProcessor.VerifyResult verifyScannedText(String message) {
                return mControllerListener.verifyScannedText( message);
            }
        }));

        if (!textRecognizer.isOperational()) {
            // Note: The first time that an app using a Vision API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any text,
            // barcodes, or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = getActivity().registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(getActivity(), "LOW....", Toast.LENGTH_LONG).show();
                Log.w(TAG, "LOW storage");
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the text recognizer to detect small pieces of text.
        mCameraSource =
                new CameraSource.Builder(getActivity().getApplicationContext(), textRecognizer)
                        .setFacing(direction)
                        .setRequestedPreviewSize(1280, 1024)
                        .setRequestedFps(2.0f)
                        .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                        .setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null)
                        .build();
    }

    @Override
    public void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
            mPreview = null;
        }
    }


    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getActivity().getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }


    private boolean onTap(float rawX, float rawY) {
        showInstructions();
        return true;
    }

    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return onTap(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);
        }
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         * <p/>
         * Once a scale has ended, {@link ScaleGestureDetector#getFocusX()}
         * and {@link ScaleGestureDetector#getFocusY()} will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mCameraSource != null) {
                mCameraSource.doZoom(detector.getScaleFactor());
            }
        }
    }
}
