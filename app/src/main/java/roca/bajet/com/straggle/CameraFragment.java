package roca.bajet.com.straggle;


import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 */
public class CameraFragment extends Fragment {
    @BindView(R.id.gl_surfaceview)
    GLSurfaceView mGLSurfaceView;
    CameraRenderer mCameraRenderer;

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

        return rootView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mCameraRenderer.startReadingSensor();


    }

    @Override
    public void onPause()
    {
        super.onPause();

        mCameraRenderer.stopReadingSensor();

    }

}
