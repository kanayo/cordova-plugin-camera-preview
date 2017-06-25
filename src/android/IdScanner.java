package com.kanayo.cordova.idscanner;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.kanayo.cordova.idscanner.iso7064.PureSystemCalculator;
import com.kanayo.cordova.idscanner.ocr.OcrDetectorProcessor;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

public class IdScanner extends CordovaPlugin implements IdScannerActivity.IScanController {

    private static final String TAG = "IdScanner";
    private static final String START_SCAN_ACTION = "startScan";
    private static final String STOP_SCAN_ACTION = "stopScan";

    static final String CAMERA_DIRECTION_PARAM = "cameraDirection";
    static final String INSTRUCTIONS_PARAM = "instructions";
    static final String CANCEL_TEXT_PARAM = "cancelText";
    static final String SWITCH_TEXT_PARAM = "switchText";

    static final String CAMERA_DIRECTION_FRONT = "front";
    static final String CAMERA_DIRECTION_BACK = "back";

    private static final int CAM_REQ_CODE = 0;
    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA
    };

    private IdScannerActivity mFragment;
    private CallbackContext mScanCallbackContext;
    private Bundle mExecArgs;
    private FrameLayout mContainerView;

    private PureSystemCalculator mChecksumCalculator;
    private String mCandidateExpression;
    private String mVerifyExpression;

    private boolean prepareVerifications( String verifyCheckSum ) {

        mChecksumCalculator = null;
        if( !verifyCheckSum.isEmpty() ){
            try {
                Class<?>clazz = Class.forName("com.kanayo.cordova.idscanner.iso7064." + verifyCheckSum);
                mChecksumCalculator = (PureSystemCalculator) clazz.newInstance();
            } catch (ClassNotFoundException e) {
                return false;
            } catch (InstantiationException e) {
                return false;
            } catch (IllegalAccessException e) {
                return false;
            }
        }

        return true;
    }





    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (START_SCAN_ACTION.equals(action)) {
            this.mScanCallbackContext = callbackContext;

            this.mExecArgs = new Bundle();
            this.mExecArgs.putString(INSTRUCTIONS_PARAM, args.getString(0));
            this.mExecArgs.putString(CAMERA_DIRECTION_PARAM, args.getString(4));
            this.mExecArgs.putString(CANCEL_TEXT_PARAM, args.getString(5));
            this.mExecArgs.putString(SWITCH_TEXT_PARAM, args.getString(6));

            mCandidateExpression = args.getString(1);
            mVerifyExpression = args.getString(2);

            String checkSumClassName = args.getString(3);
            if( !prepareVerifications( checkSumClassName ) ){
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Unknown checksum class:" + checkSumClassName ));
                return true;
            }

            if (cordova.hasPermission(PERMISSIONS[0])) {
                return startScan(callbackContext);
            } else {
                cordova.requestPermissions(this, CAM_REQ_CODE, PERMISSIONS);
            }
        } else if (STOP_SCAN_ACTION.equals(action)) {
            return stopScan();
        }

        return false;
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                mScanCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION));
                return;
            }
        }
        if (requestCode == CAM_REQ_CODE) {
            startScan(this.mScanCallbackContext);
        }
    }

    private boolean startScan(CallbackContext callbackContext) {
        Log.d(TAG, "start camera action");
        if (mFragment != null) {
            callbackContext.error("Camera already started");
            return true;
        }

        mFragment = new IdScannerActivity();
        mFragment.setArguments(mExecArgs);
        mFragment.setControllerListener(this);

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Activity activity = cordova.getActivity();
                if (mContainerView == null) {
                    mContainerView = new FrameLayout(activity.getApplicationContext());
                    // Look up a view id we inject to ensure there are no conflicts
                    int cameraViewId = activity.getResources().getIdentifier(activity.getClass().getPackage().getName() + ":id/id_scanner_container", null, null);
                    mContainerView.setId(cameraViewId);
                }

                if (mContainerView.getParent() != webView.getView().getParent()) {
                    if (mContainerView.getParent() != null) {
                        ((ViewGroup) mContainerView.getParent()).removeView(mContainerView);
                    }
                    FrameLayout.LayoutParams containerLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                    ((ViewGroup) webView.getView().getParent()).addView(mContainerView, containerLayoutParams);
                }

                mContainerView.bringToFront();
                webView.getView().setVisibility(View.GONE);

                //add the mFragment to the container
                FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.add(mContainerView.getId(), mFragment);
                fragmentTransaction.commit();
            }
        });

        return true;
    }


    private boolean stopScan() {
        Log.d(TAG, "scan stopped");

        FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.remove(mFragment);
        fragmentTransaction.commit();
        mFragment = null;

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.getView().setVisibility(View.VISIBLE);
            }
        });

        return true;
    }

    @Override
    public void onScanFinished(String message) {
        Log.d(TAG, "scan finished");
        if (mScanCallbackContext != null) {
            JSONArray data = new JSONArray();
            data.put(message);

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, data);
            pluginResult.setKeepCallback(true);
            mScanCallbackContext.sendPluginResult(pluginResult);
            mScanCallbackContext = null;
        }
    }

    @Override
    public OcrDetectorProcessor.VerifyResult verifyScannedText(String text ) {

        OcrDetectorProcessor.VerifyResult result;
        result = new OcrDetectorProcessor.VerifyResult();

        result.processedText = text.replaceAll("\\s", "");
        result.isCandidate = mCandidateExpression.isEmpty() || result.processedText.matches(mCandidateExpression);
        boolean checked  = mChecksumCalculator == null || mChecksumCalculator.verify(result.processedText);
        boolean matches = mVerifyExpression.isEmpty() || result.processedText.matches(mVerifyExpression);
        result.isVerified = matches && checked;

        return result;
    }

    @Override
    public void changeCameraDirection() {
        stopScan();

        String direction = this.mExecArgs.getString(CAMERA_DIRECTION_PARAM);
        direction = direction.equals(CAMERA_DIRECTION_FRONT) ? CAMERA_DIRECTION_BACK : CAMERA_DIRECTION_FRONT;
        this.mExecArgs.putString(CAMERA_DIRECTION_PARAM, direction);

        startScan(mScanCallbackContext);
    }

}
