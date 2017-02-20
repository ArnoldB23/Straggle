package roca.bajet.com.straggle;


import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * A simple {@link Fragment} subclass.
 */
public class CameraFragment extends Fragment {

    private final String LOG_TAG = getClass().getSimpleName();

    @BindView(R.id.gl_surfaceview)
    public GLSurfaceView mGLSurfaceView;

    public CameraRenderer mCameraRenderer;
    public Camera mCamera;
    public CameraPreview mCameraPreview;

    public CameraFragment() {
        // Required empty public constructor
    }

    public static CameraFragment newInstance() {
        CameraFragment fragment = new CameraFragment();
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_camera, container, false);

        ButterKnife.bind(this, rootView);


        mCameraRenderer = new CameraRenderer(getContext(), mGLSurfaceView);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGLSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mGLSurfaceView.setZOrderOnTop(true);
        mGLSurfaceView.setRenderer(mCameraRenderer);
        //mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);


        mCamera = getCameraInstance();
        mCameraPreview = new CameraPreview(getContext(), mCamera);


        setCameraDisplayOrientation(getActivity(), 0, mCamera);
        FrameLayout preview = (FrameLayout) rootView.findViewById(R.id.camera_preview);
        preview.addView(mCameraPreview);
        return rootView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mCameraRenderer.startReadingSensor();

        if (mCamera == null)
        {
            mCamera = getCameraInstance();
            setCameraDisplayOrientation(getActivity(), 0, mCamera);
            mCameraPreview.mCamera = mCamera;

        }

    }

    @Override
    public void onPause()
    {
        super.onPause();

        mCameraRenderer.stopReadingSensor();

        Log.d(LOG_TAG, "onPause");
        if (mCamera != null)
        {


            Log.d(LOG_TAG, "onPause releasing camera");
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;



        }


    }

    private void setupCameraPreview()
    {

    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

}
