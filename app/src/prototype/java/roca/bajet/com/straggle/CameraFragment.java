package roca.bajet.com.straggle;

import android.content.Context;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this mCameraFragment must implement the
 * {@link CameraFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CameraFragment#newInstance} factory method to
 * create an instance of this mCameraFragment.
 */
public class CameraFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the mCameraFragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String LOG_TAG = "CameraFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;


    @BindView(R.id.x_translate_textview) TextView mXTranslateTextView;
    @BindView(R.id.y_translate_textview) TextView mYTranslateTextView;
    @BindView(R.id.z_translate_textview) TextView mZTranslateTextView;
    @BindView(R.id.x_rotate_textview) TextView mXrotateTextView;
    @BindView(R.id.y_rotate_textview) TextView mYrotateTextView;
    @BindView(R.id.z_rotate_textview) TextView mZrotateTextView;

    @BindView(R.id.setlook_x_eye_textview) TextView mXEyeTextView;
    @BindView(R.id.setlook_y_eye_textview) TextView mYEyeTextView;
    @BindView(R.id.setlook_z_eye_textview) TextView mZEyeTextView;
    @BindView(R.id.setlook_x_target_textview) TextView mXTargetTextView;
    @BindView(R.id.setlook_y_target_textview) TextView mYTargetTextView;
    @BindView(R.id.setlook_z_target_textview) TextView mZTargetTextView;
    @BindView(R.id.setlook_x_up_textview) TextView mXupTextView;
    @BindView(R.id.setlook_y_up_textview) TextView mYupTextView;
    @BindView(R.id.setlook_z_up_textview) TextView mZupTextView;

    @BindView(R.id.seekbar_x_translate) SeekBar mSeekBarTransX;
    @BindView(R.id.seekbar_y_translate) SeekBar mSeekBarTransY;
    @BindView(R.id.seekbar_z_translate) SeekBar mSeekBarTransZ;
    @BindView(R.id.seekbar_x_rotate)    SeekBar mSeekBarRotX;
    @BindView(R.id.seekbar_y_rotate)    SeekBar mSeekBarRotY;
    @BindView(R.id.seekbar_z_rotate)    SeekBar mSeekBarRotZ;

    @BindView(R.id.seekbar_x_eye) SeekBar mSeekBarEyeX;
    @BindView(R.id.seekbar_y_eye) SeekBar mSeekBarEyeY;
    @BindView(R.id.seekbar_z_eye) SeekBar mSeekBarEyeZ;
    @BindView(R.id.seekbar_x_target)    SeekBar mSeekBarTargetX;
    @BindView(R.id.seekbar_y_target)    SeekBar mSeekBarTargetY;
    @BindView(R.id.seekbar_z_target)    SeekBar mSeekBarTargetZ;
    @BindView(R.id.seekbar_x_up)    SeekBar mSeekBarUpX;
    @BindView(R.id.seekbar_y_up)    SeekBar mSeekBarUpY;
    @BindView(R.id.seekbar_z_up)    SeekBar mSeekBarUpZ;
    @BindView(R.id.gl_surfaceview) GLSurfaceView mGLSurfaceView;

    @BindView(R.id.orientation_textview) TextView mOrientationTextView;
    CameraRenderer mCameraRenderer;


    public CameraFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this mCameraFragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of mCameraFragment CameraFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CameraFragment newInstance(String param1, String param2) {
        CameraFragment fragment = new CameraFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public static CameraFragment newInstance() {
        CameraFragment fragment = new CameraFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_camera_setlookatm, container, false);

        ButterKnife.bind(this, rootView);


        mCameraRenderer = new CameraRenderer(getContext(), mGLSurfaceView);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGLSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mGLSurfaceView.setZOrderOnTop(true);
        mGLSurfaceView.setRenderer(mCameraRenderer);
        //mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);



        mCameraRenderer.setOnOrientationCallback(new CameraRenderer.OrientationCallback() {
            @Override
            public void onOrientationChange(float[] orientation) {
                mOrientationTextView.setText("Z : " + String.format("%+3.1f", orientation[0]) + ", X : " + String.format("%+3.1f", orientation[1]) + ", Y : " + String.format("%+3.1f", orientation[2]));
            }
        });

        mSeekBarTransX.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float offset = i*0.1f - 3f;


                mCameraRenderer.positionObjectInScene(new Float[]{offset, null, null, null, null, null});

                mXTranslateTextView.setText(getString(R.string.title_x_translation) + ": " + String.format("%3.1f",offset));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSeekBarTransY.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float offset = i*0.1f - 3f;
                mCameraRenderer.positionObjectInScene(new Float[]{null, offset, null, null, null, null});
                mYTranslateTextView.setText(getString(R.string.title_y_translation) + ": " + String.format("%3.1f",offset));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSeekBarTransZ.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                //float offset = i*0.1f - 4f;
                float offset = -i*0.1f + 20f;

                mCameraRenderer.positionObjectInScene(new Float[]{null, null, offset, null, null, null});
                mZTranslateTextView.setText(getString(R.string.title_z_translation) + ": " + String.format("%3.1f",offset));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSeekBarRotX.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float offset = i - 180f;


                mCameraRenderer.positionObjectInScene(new Float[]{null, null, null, offset, null, null});

                mXrotateTextView.setText(getString(R.string.title_x_rotation) + ": " + String.format("%3.1f",offset));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSeekBarRotY.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float offset = i - 180f;
                mCameraRenderer.positionObjectInScene(new Float[]{null, null, null, null, offset, null});
                mYrotateTextView.setText(getString(R.string.title_y_rotation) + ": " + String.format("%3.1f",offset));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSeekBarRotZ.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float offset = i - 180f;
                mCameraRenderer.positionObjectInScene(new Float[]{null, null, null, null, null, offset});
                mZrotateTextView.setText(getString(R.string.title_z_rotation) + ": " + String.format("%3.1f",offset));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSeekBarEyeX.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float offset = i*0.01f;
                mCameraRenderer.positionViewCameraInScene(new Float[]{offset, null, null, null, null, null, null,null, null});
                mXEyeTextView.setText(getString(R.string.title_x_setlook_eye) + ": " + String.format("%3.1f",offset));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSeekBarEyeY.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float offset = i - 50f;
                mCameraRenderer.positionViewCameraInScene(new Float[]{null, offset, null, null, null, null, null,null, null});
                mYEyeTextView.setText(getString(R.string.title_y_setlook_eye) + ": " + String.format("%3.1f",offset));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSeekBarEyeZ.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float offset = i - 50f;
                mCameraRenderer.positionViewCameraInScene(new Float[]{null, null, offset, null, null, null, null,null, null});
                mZEyeTextView.setText(getString(R.string.title_z_setlook_eye) + ": " + String.format("%3.1f",offset));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSeekBarTargetX.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float offset = i - 50f;


                mCameraRenderer.positionViewCameraInScene(new Float[]{null, null, null, offset, null, null, null,null, null});

                mXTargetTextView.setText(getString(R.string.title_x_setlook_target) + ": " + String.format("%3.1f",offset));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSeekBarTargetY.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float offset = i - 50f;
                mCameraRenderer.positionViewCameraInScene(new Float[]{null, null, null, null, offset, null, null,null, null});
                mYTargetTextView.setText(getString(R.string.title_y_setlook_target) + ": " + String.format("%3.1f",offset));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSeekBarTargetZ.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float offset = i - 50f;
                mCameraRenderer.positionViewCameraInScene(new Float[]{null, null, null, null, null, offset, null,null, null});
                mZTargetTextView.setText(getString(R.string.title_z_setlook_target) + ": " + String.format("%3.1f",offset));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSeekBarUpX.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float offset = i - 50f;
                mCameraRenderer.positionViewCameraInScene(new Float[]{null, null, null, null, null, null, offset,null, null});
                mXupTextView.setText(getString(R.string.title_x_setlook_up) + ": " + String.format("%3.1f",offset));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSeekBarUpY.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float offset = i - 50f;
                mCameraRenderer.positionViewCameraInScene(new Float[]{null, null, null, null, null, null, null,offset, null});
                mYupTextView.setText(getString(R.string.title_y_setlook_up) + ": " + String.format("%3.1f",offset));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSeekBarUpZ.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float offset = i - 50f;
                mCameraRenderer.positionViewCameraInScene(new Float[]{null, null, null, null, null, null, null,null, offset});
                mZupTextView.setText(getString(R.string.title_z_setlook_up) + ": " + String.format("%3.1f",offset));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


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







    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /*
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }*/


    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * mCameraFragment to allow an interaction in this mCameraFragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
