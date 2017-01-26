package com.clariontools;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.apache.cordova.LOG;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class CameraActivity extends Fragment {
    
    public static final int FREE_ROTATION = -1;
    
    public interface CameraPreviewListener {
        public void onPictureTaken(String originalPicturePath);
        public void onCameraPreviewReady();
    }
    
    private CameraPreviewListener eventListener;
    private SimpleOrientationListener orientationListener;
    private static final String TAG = "CameraActivity";
    public FrameLayout mainLayout;
    public FrameLayout frameContainerLayout;
    
    private Preview mPreview;
    private boolean canTakePicture = true;
    
    private View view;
    private Camera.Parameters cameraParameters;
    private Camera mCamera;
    private int numberOfCameras;
    private int cameraCurrentlyLocked;
    
    // The first rear facing camera
    private int defaultCameraId;
    public String defaultCamera;
    public int maxCaptureLength;
    public int lockRotation;
    public String filePrefix;
    
    public int width;
    public int height;
    public int x;
    public int y;
    
    public void setEventListener(CameraPreviewListener listener){
        eventListener = listener;
    }
    
    private String appResourcesPackage;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        appResourcesPackage = getActivity().getPackageName();
        
        // Inflate the layout for this fragment
        view = inflater.inflate(getResources().getIdentifier("camera_activity", "layout", appResourcesPackage), container, false);
        createCameraPreview();
        return view;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    public void setRect(int x, int y, int width, int height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    private void createCameraPreview() {
        
        if (orientationListener == null && lockRotation == FREE_ROTATION) {
            orientationListener = new SimpleOrientationListener(this.getActivity().getApplicationContext());
        }
        
        if(mPreview == null) {
            setDefaultCameraId();
            
            //set box position and size
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height);
            layoutParams.setMargins(x, y, 0, 0);
            frameContainerLayout = (FrameLayout) view.findViewById(getResources().getIdentifier("frame_container", "id", appResourcesPackage));
            frameContainerLayout.setLayoutParams(layoutParams);
            
            //video view
            mPreview = new Preview(getActivity());
            mPreview.maxCaptureLength = maxCaptureLength;
            mainLayout = (FrameLayout) view.findViewById(getResources().getIdentifier("video_view", "id", appResourcesPackage));
            mainLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
            mainLayout.addView(mPreview);
            mainLayout.setEnabled(false);
        }
    }
    
    private void setDefaultCameraId(){
        
        // Find the total number of cameras available
        numberOfCameras = Camera.getNumberOfCameras();
        
        int camId = defaultCamera.equals("front") ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
        
        // Find the ID of the default camera
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == camId) {
                defaultCameraId = camId;
                break;
            }
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (lockRotation == FREE_ROTATION) {
            orientationListener.enable();
        }
        
        mCamera = Camera.open(defaultCameraId);
        
        if (cameraParameters != null) {
            mCamera.setParameters(cameraParameters);
        }

        cameraCurrentlyLocked = defaultCameraId;
        
        if(mPreview.mPreviewSize == null){
            mPreview.setCamera(mCamera, cameraCurrentlyLocked);
        } else {
            mPreview.switchCamera(mCamera, cameraCurrentlyLocked);
            mCamera.startPreview();
        }
        
        Log.d(TAG, "cameraCurrentlyLocked:" + cameraCurrentlyLocked);

        // tell javascript callback that the preview is ready and the camera parameters can be queried
        new Thread() {
            public void run() {
               eventListener.onCameraPreviewReady();
            }
        }.start();
        
        final FrameLayout frameContainerLayout = (FrameLayout) view.findViewById(getResources().getIdentifier("frame_container", "id", appResourcesPackage));
        ViewTreeObserver viewTreeObserver = frameContainerLayout.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    frameContainerLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    frameContainerLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    final RelativeLayout frameCamContainerLayout = (RelativeLayout) view.findViewById(getResources().getIdentifier("frame_camera_cont", "id", appResourcesPackage));
                    
                    FrameLayout.LayoutParams camViewLayout = new FrameLayout.LayoutParams(frameContainerLayout.getWidth(), frameContainerLayout.getHeight());
                    camViewLayout.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
                    frameCamContainerLayout.setLayoutParams(camViewLayout);
                }
            });
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (lockRotation == FREE_ROTATION) {
            orientationListener.disable();
        }
        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            mPreview.setCamera(null, -1);
            mCamera.release();
            mCamera = null;
        }
    }
    
    public Camera getCamera() {
        return mCamera;
    }
    
    public void resetPreview() {
        if (mCamera != null) {  // Release this camera in cameraCurrentlyLocked
            mCamera.stopPreview();
            mPreview.setCamera(null, -1);
            mCamera.release();
            mCamera = null;
        }
        mCamera = Camera.open(cameraCurrentlyLocked);  // Reopen the released camera and set parameters
        if (cameraParameters != null) {
            mCamera.setParameters(cameraParameters);
        }

        mPreview.switchCamera(mCamera, cameraCurrentlyLocked);  // Restart the preview
        mCamera.startPreview();
    }

    public void switchCamera() {
        // check for availability of multiple cameras
        if (numberOfCameras == 1) {
            //There is only one camera available
        }
        Log.d(TAG, "numberOfCameras: " + numberOfCameras);
        
        // OK, we have multiple cameras.
        // Release this camera -> cameraCurrentlyLocked
        if (mCamera != null) {
            mCamera.stopPreview();
            mPreview.setCamera(null, -1);
            mCamera.release();
            mCamera = null;
        }
        
        // Acquire the next camera and request Preview to reconfigure
        // parameters.
        mCamera = Camera.open((cameraCurrentlyLocked + 1) % numberOfCameras);
        
        if (cameraParameters != null) {
            mCamera.setParameters(cameraParameters);
        }
        
        cameraCurrentlyLocked = (cameraCurrentlyLocked + 1) % numberOfCameras;
        mPreview.switchCamera(mCamera, cameraCurrentlyLocked);
        
        Log.d(TAG, "cameraCurrentlyLocked new: " + cameraCurrentlyLocked);
        
        // Start the preview
        mCamera.startPreview();
    }

    public void setCameraParameters(Camera.Parameters params) {
        cameraParameters = params;
        
        if (mCamera != null && cameraParameters != null) {
            mCamera.setParameters(cameraParameters);
        }
    }
    
    public boolean hasFrontCamera(){
        return getActivity().getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }
    
    public Bitmap cropBitmap(Bitmap bitmap, Rect rect) {
        int w = rect.right - rect.left;
        int h = rect.bottom - rect.top;
        Bitmap ret = Bitmap.createBitmap(w, h, bitmap.getConfig());
        Canvas canvas= new Canvas(ret);
        canvas.drawBitmap(bitmap, -rect.left, -rect.top, null);
        return ret;
    }
    
    public void takePicture(final double maxWidth, final double maxHeight, final int quality) {
        final ImageView pictureView = (ImageView) view.findViewById(getResources().getIdentifier("picture_view", "id", appResourcesPackage));
        if(mPreview != null) {
            
            if (canTakePicture == false) {
                return;
            } else {
                canTakePicture = false;
            }
            
            final Camera.PictureCallback mPicture = new Camera.PictureCallback() {
                
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    
                    final int orientation = lockRotation >= 0
                    ? lockRotation
                    : orientationListener.getCurrentOrientation();
                    Log.d(TAG, "Current device orientation: " + orientation);
                    
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    final Matrix matrix = new Matrix();
                    switch(orientation){
                        case Surface.ROTATION_0:
                            matrix.postRotate(90);
                            Log.d(TAG, "Rotating 90°");
                            break;
                        case Surface.ROTATION_90:
                            matrix.postRotate(180);
                            Log.d(TAG, "Rotating 180°");
                            break;
                        case Surface.ROTATION_180:
                            matrix.postRotate(-90);
                            Log.d(TAG, "Rotating -90°");
                            break;
                        case Surface.ROTATION_270:
                            matrix.postRotate(0);
                            Log.d(TAG, "Rotating 0°");
                            break;
                        default:
                            matrix.postRotate(0);
                            Log.d(TAG, "Rotating 0° (unknown orientation)");
                    }
                    final Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    
                    final File pictureFile = getOutputMediaFile(filePrefix, "");
                    if (pictureFile == null) {
                        Log.d(TAG, "Cannot save a null picture ");
                        canTakePicture = true;
                        return;
                    }
                    
                    try {
                        FileOutputStream fos = new FileOutputStream(pictureFile);
                        rotated.compress(Bitmap.CompressFormat.JPEG, quality, fos);
                        fos.close();
                        new Thread() {
                            public void run() {
                                eventListener.onPictureTaken(pictureFile.getAbsolutePath());
                            }
                        }.start();
                    } catch (FileNotFoundException e) {
                        Log.d(TAG, "File not found: " + e.getMessage());
                    } catch (IOException e) {
                        Log.d(TAG, "Error accessing file: " + e.getMessage());
                    } finally {
                        canTakePicture = true;
                    }
                }
            };

            final Camera.AutoFocusCallback _pfnAutoFocusCallback = new Camera.AutoFocusCallback() {
                
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    Log.d(TAG, "AutoFocusCallback returned onAutoFocus with value: " + String.valueOf(success));
                    mCamera.autoFocus(null);                    // If another autoFocus event is fired make sure we don't return back here
                    mCamera.takePicture(null, null, mPicture);  // Take the picture could add code if success false to refocus if needed
                }
            };
            mCamera.autoFocus(_pfnAutoFocusCallback); // to start or check current focus in auto or continuous focus mode.
            
        } else {
            canTakePicture = true;
        }
    }
    
    private File getOutputMediaFile(String prefix, String suffix){
        
        File mediaStorageDir = getActivity().getApplicationContext().getFilesDir();
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                throw new IllegalStateException("Directory "+mediaStorageDir.getAbsolutePath()+" does not exist.");
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("dd_MM_yyyy_HHmm_ss").format(new Date());
        File mediaFile;
        String mImageName = prefix + timeStamp + suffix + ".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }
    
    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        
        if (height > reqHeight || width > reqWidth) {
            
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
    
    private Bitmap loadBitmapFromView(View v) {
        Bitmap b = Bitmap.createBitmap( v.getMeasuredWidth(), v.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        v.draw(c);
        return b;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void setFlashMode(int mode) {
        final Camera.Parameters parameters = mCamera.getParameters();
        boolean validFlashModeFound = false;
        
        switch (mode) {
            case 0:
                if (parameters.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    validFlashModeFound = true;
                }
                break;
            case 1:
                if (parameters.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_ON)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                    validFlashModeFound = true;
                }
                break;
            case 2:
                if (parameters.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_OFF)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    validFlashModeFound = true;
                }
                break;
            case 3:
                if (parameters.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    validFlashModeFound = true;
                }
                break;
            default:
                break;
                // Do nothing if error because we did not set any mode
        }

        if ( validFlashModeFound ) {
            Log.d(TAG, "From public setFlashMode going to setFlashMode with mode: " + mode);
            mCamera.setParameters(parameters);
        } else {
            Log.d(TAG, "From public setFlashMode valid mode was not passed, mode value: " + mode);
            mode = -1;
        }

        if (mPreview != null) {
            mPreview.flashMode = mode;
        }

    }

    public void setZoom(int zoom) {
        final Camera.Parameters parameters = mCamera.getParameters();
        if (parameters.isZoomSupported()) {
            final int maxZoom = parameters.getMaxZoom();
            final List<Integer> zooms = parameters.getZoomRatios();
            Log.d(TAG, "From public setZoom zoom: " + String.valueOf(zoom) + " maxZoom: " + String.valueOf(maxZoom));
            Log.d(TAG, "From public setZoom zoom ratios supported:");
            for ( int i = 0; i <= maxZoom; i++ ) {
                Log.d(TAG, String.valueOf(i) + ": Zoom: " + String.valueOf(zooms.get(i)));
            }
            
            if (zoom < 0 || zoom > maxZoom) {
                Log.d(TAG, "From public throw execption zoom: " + String.valueOf(zoom) + " is unsupported.");
                throw new IllegalArgumentException(String.format("Zoom level %d unsupported.", zoom));
            }
            Log.d(TAG, "From public setZoom goint to setZoom with zoom: " + String.valueOf(zoom));
            if (mPreview != null) {
                Log.d(TAG, "From public setZoom mPreview is active!");
                mPreview.zoomLevel = zoom;
            }
            parameters.setZoom(zoom);
            mCamera.setParameters(parameters);
        }
    }

    public static class SimpleOrientationListener extends OrientationEventListener {
        
        private volatile int defaultScreenOrientation = Configuration.ORIENTATION_UNDEFINED;
        public int prevOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
        private Context ctx;
        private ReentrantLock lock = new ReentrantLock(true);
        private int currentOrientation = Configuration.ORIENTATION_UNDEFINED;
        
        public SimpleOrientationListener(Context context) {
            super(context);
            ctx = context;
        }
        
        public SimpleOrientationListener(Context context, int rate) {
            super(context, rate);
            ctx = context;
        }
        
        @Override
        public void onOrientationChanged(final int rotation) {
            int normalizedRotation = OrientationEventListener.ORIENTATION_UNKNOWN;
            if (rotation >= 330 || rotation < 30) {
                normalizedRotation = Surface.ROTATION_0;
            } else if (rotation >= 60 && rotation < 120) {
                normalizedRotation = Surface.ROTATION_90;
            } else if (rotation >= 150 && rotation < 210) {
                normalizedRotation = Surface.ROTATION_180;
            } else if (rotation >= 240 && rotation < 300) {
                normalizedRotation = Surface.ROTATION_270;
            }
            
            if (prevOrientation != normalizedRotation && rotation != OrientationEventListener.ORIENTATION_UNKNOWN) {
                prevOrientation = normalizedRotation;
                if (normalizedRotation != OrientationEventListener.ORIENTATION_UNKNOWN)
                    currentOrientation = normalizedRotation;
            }
            
        }
        
        private void reportOrientationChanged(final int normalizedRotation) {
            
            int defaultOrientation = getDeviceDefaultOrientation();
            int orthogonalOrientation = defaultOrientation == Configuration.ORIENTATION_LANDSCAPE
            ? Configuration.ORIENTATION_PORTRAIT
            : Configuration.ORIENTATION_LANDSCAPE;
            
            int toReportOrientation;
            if (normalizedRotation == Surface.ROTATION_0 || normalizedRotation == Surface.ROTATION_180) {
                toReportOrientation = defaultOrientation;
            } else {
                toReportOrientation = orthogonalOrientation;
            }
            this.currentOrientation = toReportOrientation;
        }
        
        /**
         * Must determine what is default device orientation (some tablets can have default landscape). Must be initialized when device orientation is defined.
         *
         * @return value of {@link Configuration#ORIENTATION_LANDSCAPE} or {@link Configuration#ORIENTATION_PORTRAIT}
         */
        private int getDeviceDefaultOrientation() {
            if (defaultScreenOrientation == Configuration.ORIENTATION_UNDEFINED) {
                lock.lock();
                defaultScreenOrientation = initDeviceDefaultOrientation(ctx);
                lock.unlock();
            }
            return defaultScreenOrientation;
        }
        
        /**
         * Provides device default orientation
         *
         * @return value of {@link Configuration#ORIENTATION_LANDSCAPE} or {@link Configuration#ORIENTATION_PORTRAIT}
         */
        private int initDeviceDefaultOrientation(Context context) {
            
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Configuration config = context.getResources().getConfiguration();
            int rotation = windowManager.getDefaultDisplay().getRotation();
            
            boolean isLand = config.orientation == Configuration.ORIENTATION_LANDSCAPE;
            boolean isDefaultAxis = rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180;
            
            int result;
            if ((isDefaultAxis && isLand) || (!isDefaultAxis && !isLand)) {
                result = Configuration.ORIENTATION_LANDSCAPE;
            } else {
                result = Configuration.ORIENTATION_PORTRAIT;
            }
            return result;
        }
        
        public int getCurrentOrientation() {
            return currentOrientation;
        }
        
    }
}

class Preview extends RelativeLayout implements SurfaceHolder.Callback {
    private final String TAG = "Preview";
    
    public int zoomLevel;
    public int flashMode;
    
    CustomSurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Camera.Size mPreviewSize;
    Camera.Size mPictureSize;
    List<Camera.Size> mSupportedPreviewSizes;
    List<Camera.Size> mSupportedPictureSizes;
    Camera mCamera;
    int cameraId;
    int displayOrientation;
    int maxCaptureLength;

    Preview(Context context) {
        super(context);
        
        mSurfaceView = new CustomSurfaceView(context);
        addView(mSurfaceView);
        
        requestLayout();
        
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    
    public void setCamera(Camera camera, int cameraId) {
        mCamera = camera;
        this.cameraId = cameraId;
        if (mCamera != null) {
            mSupportedPictureSizes = mCamera.getParameters().getSupportedPictureSizes();
            Collections.sort(mSupportedPictureSizes, new CameraSizeComparator());
            for (Camera.Size s: mSupportedPictureSizes) {
                Log.d(TAG, "Supported picture size: " + s.width + "x" + s.height);
            }
            
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            Collections.sort(mSupportedPreviewSizes, new CameraSizeComparator());
            for (Camera.Size s: mSupportedPreviewSizes) {
                Log.d(TAG, "Supported preview size: " + s.width + "x" + s.height);
            }
            
            setCameraDisplayOrientation();            
            // Get the camera parameters
            Camera.Parameters params = mCamera.getParameters();
            
            Log.d(TAG, "setting camera in FOCUS MODE CONTINUOUS PICTURE");
            if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            
            Log.d(TAG, "setting camera flash mode to " + this.flashMode + " as requested.");
            // If flashMode is -1 don't bother to try and set the flash mode call to CameraActivity checks for valid modes
            if (this.flashMode != -1) {
                switch (this.flashMode) {
                    case 0:
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                        break;
                    case 1:
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                        break;
                    case 2:
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        break;
                    case 3:
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        break;
                    default:
                        // shouldn't get here but don't set anything if you do
                        break;
                }
            }

            Log.d(TAG, "setting camera zoom level to current level " + this.zoomLevel + " as requested.");
            if (params.isZoomSupported()) {
                params.setZoom(this.zoomLevel);
            }

            // set parameters based on params modifications made above;
            mCamera.setParameters(params);
        }
    }
    
    public int getDisplayOrientation() {
        return displayOrientation;
    }
    
    private void setCameraDisplayOrientation() {

        int rotation = ((Activity)getContext()).getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees=0;
                break;
            case Surface.ROTATION_90:
                degrees=90;
                break;
            case Surface.ROTATION_180:
                degrees=180;
                break;
            case Surface.ROTATION_270:
                degrees=270;
                break;
        }
        
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            displayOrientation = (info.orientation + degrees) % 360;
            displayOrientation = (360 - displayOrientation) % 360;
        } else {
            displayOrientation = (info.orientation - degrees + 360) % 360;
        }
        
        Log.d(TAG, "screen is rotated " + degrees + "deg from natural");
        Log.d(TAG, (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ? "front" : "back")
              + " camera is oriented -" + info.orientation + "deg from natural");
        Log.d(TAG, "need to rotate preview " + displayOrientation + "deg");
        
        mCamera.setDisplayOrientation(displayOrientation);
    }
    
    public void switchCamera(Camera camera, int cameraId) {
        setCamera(camera, cameraId);
        try {
            camera.setPreviewDisplay(mHolder);
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            parameters.setPictureSize(mPictureSize.width, mPictureSize.height);
            camera.setParameters(parameters);
        }
        catch (IOException exception) {
            Log.e(TAG, exception.getMessage());
        }
        //requestLayout();
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a wrapper to
        // a SurfaceView that centers the camera preview instead of stretching it.
        //
        // This does not seem to be called on the CorodovaCameraPreview
        //
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        
        setMeasuredDimension(width, height);
        
        LOG.d(TAG, "setMeasuredDimension from onMeasure Override width:" + width + " height:" + height);
        
        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalImageSize(mSupportedPreviewSizes, width, height, null);
            LOG.d(TAG, "Set mPreviewSize from onMeasure Override width:" + mPreviewSize.width + " height:" + mPreviewSize.height);
        } else {
            LOG.d(TAG, "DID NOT SET mPreviewSize from onMeasure Override width:" + mPreviewSize.width + " height:" + mPreviewSize.height);
        }
        
        if (mSupportedPictureSizes != null) {
            mPictureSize = getOptimalImageSize(mSupportedPictureSizes, width, height, mPreviewSize);
            LOG.d(TAG, "Set mPictureSize from onMeasure Override width:" + mPictureSize.width + " height:" + mPictureSize.height);
        } else {
            LOG.d(TAG, "DID NOT SET mPictureSize from onMeasure Override width:" + mPictureSize.width + " height:" + mPictureSize.height);
        }
    }
    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);
            
            int width = r - l;
            int height = b - t;
            
            int previewWidth = width;
            int previewHeight = height;
            
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
                
                if (displayOrientation == 90 || displayOrientation == 270) {
                    previewWidth = mPreviewSize.height;
                    previewHeight = mPreviewSize.width;
                }
                
                LOG.d(TAG, "previewWidth:" + previewWidth + " previewHeight:" + previewHeight);
            }
            
            int nW;
            int nH;
            int top;
            int left;
            
            float scale = 1.0f;
            
            // Center the child SurfaceView within the parent.
            if (width * previewHeight < height * previewWidth) {
                Log.d(TAG, "center horizontally");
                int scaledChildWidth = (int)((previewWidth * height / previewHeight) * scale);
                nW = (width + scaledChildWidth) / 2;
                nH = (int)(height * scale);
                top = 0;
                left = (width - scaledChildWidth) / 2;
            } else {
                Log.d(TAG, "center vertically");
                int scaledChildHeight = (int)((previewHeight * width / previewWidth) * scale);
                nW = (int)(width * scale);
                nH = (height + scaledChildHeight) / 2;
                top = (height - scaledChildHeight) / 2;
                left = 0;
            }
            
            child.layout(left, top, nW, nH);
            
            Log.d("layout", "left:" + left);
            Log.d("layout", "top:" + top);
            Log.d("layout", "right:" + nW);
            Log.d("layout", "bottom:" + nH);
        }
    }
    
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                mSurfaceView.setWillNotDraw(false);
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }
    
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }
    
    private Camera.Size getOptimalImageSize(List<Camera.Size> sizes, int w, int h, Camera.Size reference) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;

        if (displayOrientation == 90 || displayOrientation == 270) {
            targetRatio = (double) h / w;
        }

        if (sizes == null) {
            return null;
        }
        
        Camera.Size optimalSize = null;

        if (maxCaptureLength == 640) {
            Log.d(TAG, "DETECTED 640 maxCaptureLength setting..");

            optimalSize = sizes.get(0); // Set the default to the first reported size by camera if no 640 found

            // Try to find an size match for the maxCaptureLength
            for (Camera.Size size : sizes) {
                optimalSize = size;
                if (optimalSize.width == maxCaptureLength) {
                    Log.d(TAG, "SUPPORTS 640x480 Capture or Preview size: w: " + optimalSize.width + " h: " + optimalSize.height);
                    return optimalSize;
                }
            }
        
            Log.d(TAG, "Capture or Preview does not support 640x480 default to first report size: w: " + optimalSize.width + " h: " + optimalSize.height);
            
            return optimalSize;
        }

        double minDiff = Double.MAX_VALUE;
        
        int targetHeight = h;
        
        Log.d(TAG, "Searching optimal size: aspect ratio " + targetRatio);
        
        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
                Log.d(TAG, "Candidate w: " + optimalSize.width + " h: " + optimalSize.height +"; new height target threshold is " + minDiff);
                
                if (reference != null && optimalSize.width >= reference.width && optimalSize.height >= reference.height) {
                    Log.d(TAG, "Candidate closest to reference found; w: " + optimalSize.width + " h: " + optimalSize.height);
                    return optimalSize;
                }
            }
        }
        
        // Cannot find the one match the aspect ratio, ignore the requirement
        // I don't think this really works in all cases and ends up returning the
        // first supported size which could be a bad choice
        if (optimalSize == null) {
            Log.d(TAG, "No optimal size found, searching for widest preview");
            optimalSize = sizes.get(0);
            for (int i=0; i < sizes.size(); i++) {
                Camera.Size s = sizes.get(i);
                Log.d(TAG, "Size "+s.width+"x"+s.height);
                if (s.width >= optimalSize.width) {
                    optimalSize = s;
                    if (reference != null && optimalSize.width >= reference.width && optimalSize.height >= reference.height) {
                        Log.d(TAG, "Candidate closest to reference found; w: " + optimalSize.width + " h: " + optimalSize.height);
                        return optimalSize;
                    }
                }
            }
        }
        
        Log.d(TAG, "chosen size: w: " + optimalSize.width + " h: " + optimalSize.height);

        return optimalSize;
    }
    
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if(mCamera != null) {
            // Now that the size is known, set up the camera parameters and begin
            // the preview.
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            parameters.setPictureSize(mPictureSize.width, mPictureSize.height);
            requestLayout();
            
            mCamera.setParameters(parameters);
            mCamera.startPreview();
        }
    }
    
    public byte[] getFramePicture(byte[] data, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        int format = parameters.getPreviewFormat();
        
        //YUV formats require conversion
        if (format == ImageFormat.NV21 || format == ImageFormat.YUY2 || format == ImageFormat.NV16) {
            int w = parameters.getPreviewSize().width;
            int h = parameters.getPreviewSize().height;
            
            // Get the YuV image
            YuvImage yuvImage = new YuvImage(data, format, w, h, null);
            // Convert YuV to Jpeg
            Rect rect = new Rect(0, 0, w, h);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // For preview capture not utilized, if needed consider making quality value a param
            yuvImage.compressToJpeg(rect, 80, outputStream);
            return outputStream.toByteArray();
        }
        return data;
    }
    
    public void setOneShotPreviewCallback(Camera.PreviewCallback callback) {
        if(mCamera != null) {
            mCamera.setOneShotPreviewCallback(callback);
        }
    }
    
    public static class CameraSizeComparator implements Comparator<Camera.Size> {
        
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            final int w = new Integer(lhs.width).compareTo(new Integer(rhs.width));
            if (w != 0) {
                return w;
            }
            
            return new Integer(lhs.height).compareTo(new Integer(rhs.height));
        }
    }
}

class CustomSurfaceView extends SurfaceView implements SurfaceHolder.Callback{
    private final String TAG = "CustomSurfaceView";
    
    CustomSurfaceView(Context context){
        super(context);
    }
    
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }
    
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }
}
