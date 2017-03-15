package com.clariontools;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.hardware.Camera;
import android.util.DisplayMetrics;import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.Manifest;
import android.content.pm.PackageManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.List;
import java.util.Arrays;

public class CameraPreview extends CordovaPlugin implements CameraActivity.CameraPreviewListener {
    
    private final String TAG = "CameraPreview";
    private final String setOnPictureTakenHandlerAction = "setOnPictureTakenHandler";
    private final String setOnCameraPreviewReadyHandlerAction = "setOnCameraPreviewReadyHandler";
    private final String setOnOrientationChangeHandlerAction = "setOnOrientationChangeHandler";
    private final String setOnCameraDebugMessageHandlerAction = "setOnCameraDebugMessageHandler";
    private final String startCameraAction = "startCamera";
    private final String stopCameraAction = "stopCamera";
    private final String switchCameraAction = "switchCamera";
    private final String takePictureAction = "takePicture";
    private final String showCameraAction = "showCamera";
    private final String hideCameraAction = "hideCamera";
    private final String setFlashModeAction = "setFlashMode";
    private final String setZoomLevelAction = "setZoomLevel";
    private final String setCameraOrientationAction = "setCameraOrientation";
    private final String setCameraDebugMessageLoggingAction = "setCameraDebugMessageLogging";
    private final String getZoomLevelsAction = "getZoomLevels";
    
    private CameraActivity fragment;
    private CallbackContext takePictureCallbackContext;
    private CallbackContext cameraPreviewReadyCallbackContext;
    private CallbackContext orientationChangeCallbackContext;
    private CallbackContext cameraDebugMessageCallbackContext;
    private int containerViewId = 1;
    
    private final String [] permissions = {
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    
    private final int permissionsReqId = 0;
    private CallbackContext execCallback;
    private JSONArray execArgs;
    
    public CameraPreview(){
        super();
        Log.d(TAG, "Constructing");
    }
    
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        
        if (setOnPictureTakenHandlerAction.equals(action)) {
            return setOnPictureTakenHandler(args, callbackContext);
        }
        else if (setOnCameraPreviewReadyHandlerAction.equals(action)) {
            return setOnCameraPreviewReadyHandler(args, callbackContext);
        }
        else if (setOnOrientationChangeHandlerAction.equals(action)) {
            return setOnOrientationChangeHandler(args, callbackContext);
        }
        else if (setOnCameraDebugMessageHandlerAction.equals(action)) {
            return setOnCameraDebugMessageHandler(args, callbackContext);
        }
        else if (startCameraAction.equals(action)) {
            if (cordova.hasPermission(permissions[0])) {
                // Only call startCamera if permision has been granted
                return startCamera(args, callbackContext);
            } else {
                // Otherwise request permissions
                execCallback = callbackContext;
                execArgs = args;
                cordova.requestPermissions(this, permissionsReqId, permissions);
            }
        }
        else if (takePictureAction.equals(action)){
            return takePicture(args, callbackContext);
        }
        else if (stopCameraAction.equals(action)){
            return stopCamera(args, callbackContext);
        }
        else if (hideCameraAction.equals(action)){
            return hideCamera(args, callbackContext);
        }
        else if (showCameraAction.equals(action)){
            return showCamera(args, callbackContext);
        }
        else if (switchCameraAction.equals(action)){
            return switchCamera(args, callbackContext);
        }
        else if (setFlashModeAction.equals(action)) {
            return setFlashMode(args, callbackContext);
        }
        else if (setZoomLevelAction.equals(action)) {
            return setZoomLevel(args, callbackContext);
        }
        else if (setCameraOrientationAction.equals(action)) {
            return setCameraOrientation(args, callbackContext);
        }
        else if (setCameraDebugMessageLoggingAction.equals(action)) {
            return setCameraDebugMessageLogging(args, callbackContext);
        }
        else if (getZoomLevelsAction.equals(action)) {
            return getZoomLevels(args, callbackContext);
        }
        
        return false;
    }
    
    private boolean startCamera(final JSONArray args, final CallbackContext callbackContext) {
        if(fragment != null){
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            return false;
        }
        fragment = new CameraActivity();

        try {
            // get args that are used to set fragment variables important to do this very early
            // and not on another thread, so other methods can access set values when called
            String defaultCamera = args.getString(4);
            int maxCaptureLength = args.isNull(6) ? 0 : args.getInt(6);
            DisplayMetrics metrics = cordova.getActivity().getResources().getDisplayMetrics();
            int x = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, args.getInt(0), metrics);
            int y = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, args.getInt(1), metrics);
            int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, args.getInt(2), metrics);
            int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, args.getInt(3), metrics);
            int lockRotation = args.isNull(7) ? -1 : args.getInt(7);
            int debugLevel = args.isNull(8) ? 0 : args.getInt(8);
            String filePrefix = args.isNull(10) ? "picture" : args.getString(9);
            
            fragment.defaultCamera = defaultCamera;
            fragment.maxCaptureLength = maxCaptureLength;
            fragment.setRect(x, y, width, height);
            fragment.lockRotation = lockRotation;
            fragment.filePrefix = filePrefix;
            fragment.setCameraDebugMessageLogging(debugLevel);
        } catch (Exception e) {
            e.printStackTrace();
        }

        fragment.setEventListener(this);
        
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // get args that are used for the container layout.
                    Boolean toBack = args.getBoolean(5);
                    float containerAlpha = args.isNull(9) ? 1.0f : Float.parseFloat(args.getString(8));
                    
                    // create or update the layout params for the container view
                    boolean containerFound = true;
                    FrameLayout containerView = (FrameLayout)cordova.getActivity().findViewById(containerViewId);
                    if (containerView == null) {
                        containerFound = false;
                        containerView = new FrameLayout(cordova.getActivity().getApplicationContext());
                        containerView.setId(containerViewId);
                        
                        FrameLayout.LayoutParams containerLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                        cordova.getActivity().addContentView(containerView, containerLayoutParams);
                    }
                    
                    if (toBack) {
                        // Display camera preview under the webview by bringing the webview to the front
                        webView.getView().setBackgroundColor(0x00000000);
                        ((ViewGroup)webView.getView()).bringToFront();
                    } else {
                        // Display camera preview on top of the webview set alpha so can see through if needed
                        containerView.setAlpha(containerAlpha);
                        containerView.bringToFront();
                    }
                    
                    // Add the fragment to the container
                    FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.add(containerView.getId(), fragment);
                    fragmentTransaction.commit();
                    
                    // Return the JavaScript Promise
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
                    pluginResult.setKeepCallback(false);
                    callbackContext.sendPluginResult(pluginResult);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return true;
    }
    
    private boolean takePicture(final JSONArray args, CallbackContext callbackContext) {
        if (fragment == null) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            return false;
        }
        
        try {
            double maxWidth = args.getDouble(0);
            double maxHeight = args.getDouble(1);
            int quality = args.getInt(2);
            fragment.takePicture(maxWidth, maxHeight, quality);

            // Return the JavaScript Promise
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            pluginResult.setKeepCallback(false);
            callbackContext.sendPluginResult(pluginResult);
        } catch (Exception e) {
            e.printStackTrace();
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            return false;
        }
        return true;
    }
    
    public void onPictureTaken(String originalPicturePath) {
        fragment.resetPreview();  // camera preview freezing with still image of capture fix

        JSONArray data = new JSONArray();
        data.put(originalPicturePath);
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, data);
        pluginResult.setKeepCallback(true);
        takePictureCallbackContext.sendPluginResult(pluginResult);
        Log.d(TAG, "Called reset preview onPictureTaken.");
    }

    public void onCameraPreviewReady() {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        pluginResult.setKeepCallback(true);
        cameraPreviewReadyCallbackContext.sendPluginResult(pluginResult);
        Log.d(TAG, "Called onCameraPreviewReady.");
    }

    public void onOrientationChange(String orientation) {
        
        JSONArray data = new JSONArray();
        data.put(orientation);
        
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, data);
        pluginResult.setKeepCallback(true);
        orientationChangeCallbackContext.sendPluginResult(pluginResult);
        Log.d(TAG, "Called onOrientationChange.");
    }

    public void onCameraDebugMessage(String msg) {
       
        JSONArray data = new JSONArray();
        data.put(msg);
        
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, data);
        pluginResult.setKeepCallback(true);
        cameraDebugMessageCallbackContext.sendPluginResult(pluginResult);
        Log.d(TAG, "Called onCameraDebugMessage.");
    }

    private boolean stopCamera(final JSONArray args, CallbackContext callbackContext) {
        if(fragment == null) {
            Log.d(TAG, "ERROR: Called stop camera but not fragment found!");
            return false;
        }
        
        FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.remove(fragment);
        fragmentTransaction.commit();
        fragment = null;
        
        Log.d(TAG, "stopCamera called returning true.");
        return true;
    }
    
    private boolean showCamera(final JSONArray args, CallbackContext callbackContext) {
        if (fragment == null) {
            return false;
        }
        
        FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.show(fragment);
        fragmentTransaction.commit();
        
        return true;
    }
    
    private boolean hideCamera(final JSONArray args, CallbackContext callbackContext) {
        if (fragment == null) {
            return false;
        }
        
        FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.hide(fragment);
        fragmentTransaction.commit();
        
        return true;
    }
    
    private boolean switchCamera(final JSONArray args, CallbackContext callbackContext) {
        if (fragment == null) {
            return false;
        }
        
        fragment.switchCamera();
        return true;
    }
    
    private boolean setOnPictureTakenHandler(JSONArray args, CallbackContext callbackContext) {
        Log.d(TAG, "setOnPictureTakenHandler");
        takePictureCallbackContext = callbackContext;
        return true;
    }
    
    private boolean setOnCameraPreviewReadyHandler(JSONArray args, CallbackContext callbackContext) {
        Log.d(TAG, "setOnCameraPreviewReadyHandler");
        cameraPreviewReadyCallbackContext = callbackContext;
        return true;
    }

    private boolean setOnOrientationChangeHandler(JSONArray args, CallbackContext callbackContext) {
        Log.d(TAG, "setOnOrientationChangeHandler");
        orientationChangeCallbackContext = callbackContext;
        return true;
    }

    private boolean setOnCameraDebugMessageHandler(JSONArray args, CallbackContext callbackContext) {
        Log.d(TAG, "setOnCameraDebugMessageHandler");
        cameraDebugMessageCallbackContext = callbackContext;
        return true;
    }

    private boolean setFlashMode(final JSONArray args, CallbackContext callbackContext) {
        try {
            int mode = args.getInt(0);
            fragment.setFlashMode(mode);
            return true;
        } catch (JSONException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    private boolean setCameraOrientation(final JSONArray args, CallbackContext callbackContext) {
        try {
            int orientation = args.getInt(0);
            fragment.setCameraOrientation(orientation);
            return true;
        } catch (JSONException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private boolean setCameraDebugMessageLogging(final JSONArray args, CallbackContext callbackContext) {
        try {
            int debugLevel = args.getInt(0);
            fragment.setCameraDebugMessageLogging(debugLevel);
            return true;
        } catch (JSONException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private boolean setZoomLevel(final JSONArray args, CallbackContext callbackContext) {
        try {
            int zoom = args.getInt(0);

            if (zoom >= 0) {
                Log.d(TAG, "Calling setZoom from private with: " + String.valueOf(zoom));
                fragment.setZoom(zoom);
            } else {
                throw new IllegalArgumentException(String.format("Zoom level %d unsupported.", zoom));
            }
            return true;
        } catch (JSONException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    private boolean getZoomLevels(final JSONArray args, CallbackContext callbackContext) {
        if (fragment == null) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            Log.d(TAG, "Called getZoomLevels no fragment found.");
            return false;
        }
        
        try {
            boolean targetWholeNumbers = args.getBoolean(0);
            int maxZoomLevels = args.getInt(1);
            int maxZoomRatio = args.getInt(2);

            int zoomCount = 0;
            int zoomCountTrimmed = 0;
            int currentWholeNumber = 1;
            float maxZoomFloat = (float)maxZoomRatio / 100;
            float currentFloatNumber = 1.0f;
            float nextFloatIncrement = 0.0f;
            int wholeNumberZoomCount = 0;
            float ratio = 1.0f;

            String ratioString;
            String formattedString;
            int strLen = 0;
            boolean useFloatStrategy = false;
            
            Log.d(TAG, "From getZoomLevels maxZoomRatio: " + String.valueOf(maxZoomRatio) + " maxZoomFloat: " + String.valueOf(maxZoomFloat));

            JSONArray zoomLevels = new JSONArray();
            JSONArray zoomRatios = new JSONArray();
            
            JSONObject zoomResults = new JSONObject();
            
            Camera mCamera = fragment.getCamera();
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.isZoomSupported()) {
                zoomCount = parameters.getMaxZoom();
                if (zoomCount > 0) {
                    final List<Integer> zooms = parameters.getZoomRatios();
                    
                    //Log.d(TAG, "From public setZoom zoom: " + String.valueOf(zoom) + " maxZoom: " + String.valueOf(maxZoom));
                    if (targetWholeNumbers == true || maxZoomRatio > 0) {
                        // loop through all the ratios reported
                        for ( int i = 0; i <= zoomCount; i++ ) {
                            ratio = (float)zooms.get(i) / 100;
                            if (maxZoomRatio > 0 && ratio > maxZoomFloat) {
                                // we have exceeded the maxZoomRatio that was specified by the caller
                                ratio = (float)zooms.get(i-1) / 100;
                                zoomCount = i-1;
                                break;  // we are done and all code following will use trimmed list based on zoomCount
                            }
                            if (targetWholeNumbers == true && ratio >= (float)wholeNumberZoomCount) {
                                wholeNumberZoomCount += 1;
                            }
                        }
                        if (wholeNumberZoomCount > 0) {
                            // If whole numbers were found first one 1.00 is never counted as an actual zoom in the count
                            wholeNumberZoomCount -= 1;
                        }
                    }
                    Log.d(TAG, "zoomCount = " + String.valueOf(zoomCount) +  " wholeNumberZoomCount = " + String.valueOf(wholeNumberZoomCount) + " ratio = " + String.valueOf(ratio));
                    
                    
                    if (targetWholeNumbers == true && maxZoomLevels > 0) {
                        // if more than maxZoomLevels then use float strategy
                        if (wholeNumberZoomCount > maxZoomLevels) {
                            // switch to float strategy to pick up a more even spread
                            useFloatStrategy = true;
                            nextFloatIncrement = (float)((ratio - 1.0f) / (float)maxZoomLevels);
                            Log.d(TAG, "wholeNumbers Requested but wholeNumberZoomCount count is greater than maxZoomLevels and targetWholeNumbers is true switch to float strategy with nextFloatIncrement = " + String.valueOf(nextFloatIncrement));
                        }
                    } else if (targetWholeNumbers == false && maxZoomLevels > 0) {
                        if (zoomCount > maxZoomLevels) {
                            // switch to float strategy to pick up a more even spread
                            useFloatStrategy = true;
                            ratio = (float)zooms.get(zoomCount) / 100;  // get the maximum zoom ratio
                            nextFloatIncrement = (float)((ratio - 1.0f) / (float)maxZoomLevels);
                            Log.d(TAG, "zoomCount count is greater than maxZoomLevels and targetWholeNumbers is false nextFloatIncrement = " + String.valueOf(nextFloatIncrement));
                        }
                    }
                    
                    for ( int i = 0; i <= zoomCount; i++ ) {
                        ratio = (float)zooms.get(i) / 100;
                        if (targetWholeNumbers == true && useFloatStrategy == false) {
                            if (ratio >= (float)currentWholeNumber) {
                                currentWholeNumber += 1;
                                zoomLevels.put(i);
                                zoomRatios.put(String.format("%.2f", ratio));
                                Log.d(TAG, String.valueOf(i) + ": Whole Number Zoom: " + String.valueOf(zooms.get(i)) + ", " + String.valueOf(ratio));
                            }
                        } else if (useFloatStrategy == true) {
                            if (ratio >= currentFloatNumber) {
                                currentFloatNumber += nextFloatIncrement;
                                currentWholeNumber += 1; // borrow for total count
                                zoomLevels.put(i);
                                zoomRatios.put(String.format("%.2f", ratio));
                                Log.d(TAG, String.valueOf(i) + ": Balanced Float Zoom: " + String.valueOf(zooms.get(i)) + ", " + String.valueOf(ratio));
                            }
                        } else {
                            zoomLevels.put(i);
                            zoomRatios.put(String.format("%.2f", ratio));
                            Log.d(TAG, String.valueOf(i) + ": Zoom: " + String.valueOf(zooms.get(i)) + ", " + String.valueOf(ratio));
                        }
                    }
                }
            }
            
            if (zoomCount == 0) {
                // Either zoom is unsupported, or zoomCount returned 0
                targetWholeNumbers = false; // this is irrelevant at this point so turn it off
                zoomLevels.put(0);
                zoomRatios.put("1.00");
            }
            
            // setup the JSON object for return
            if (targetWholeNumbers == true || useFloatStrategy == true) {
                zoomResults.put("zoomCount",currentWholeNumber-2); // 1.00 does not count as a zoom
            } else {
                zoomResults.put("zoomCount",zoomCount);
            }
            zoomResults.put("zoomLevels",zoomLevels);
            zoomResults.put("zoomRatios",zoomRatios);
            
            // return the javascript Promise keep callback false
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, zoomResults.toString());
            pluginResult.setKeepCallback(false);
            callbackContext.sendPluginResult(pluginResult);
            Log.d(TAG, "Called getZoomLevels.");

        } catch (Exception e) {
            e.printStackTrace();
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            Log.d(TAG, "Called getZoomLevels failed in try catch.");
            return false;
        }
        return true;
    }
    
    // Once permission has been requested, start the camera if permission was granted
    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        for (int r:grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                execCallback.sendPluginResult(new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION));
                return;
            }
        }
        if (requestCode == permissionsReqId) {
            startCamera(execArgs, execCallback);
        }
    }
    
}
