package roca.bajet.com.straggle;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

public class CameraActivity extends AppCompatActivity {

    private final String LOG_TAG = getClass().getSimpleName();
    private CameraFragment mCameraFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);


        FragmentManager manager = getSupportFragmentManager();
        mCameraFragment = (CameraFragment) manager.findFragmentById(R.id.fragment_container_activity_main);
        if (mCameraFragment == null) {
            mCameraFragment = CameraFragment.newInstance();
            manager.beginTransaction()
                    .add(R.id.fragment_container_activity_main, mCameraFragment)
                    .commit();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(LOG_TAG, "onRequestPermissionsResult...");

        switch (requestCode) {
            case CameraFragment.LOCATION_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mCameraFragment.mIsLocationPermissionGranted = true;
                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    mCameraFragment.mIsLocationPermissionGranted = false;
                    mCameraFragment.mTakePicButton.setVisibility(View.INVISIBLE);
                }
                break;

            case CameraFragment.CAMERA_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mCameraFragment.mIsCameraPermissionGranted = true;
                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    mCameraFragment.mIsCameraPermissionGranted = false;
                    mCameraFragment.mTakePicButton.setVisibility(View.INVISIBLE);
                }
                break;

            //resolveStoragePermission method will check write and then read permissions in order.
            case CameraFragment.WRITE_STORAGE_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mCameraFragment.mIsStoragePermissionGranted = true;
                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    mCameraFragment.mIsStoragePermissionGranted = false;
                    mCameraFragment.mTakePicButton.setVisibility(View.INVISIBLE);
                }
                break;

            case CameraFragment.NEEDED_PERMISSION_REQUEST_CODE:

                if (grantResults.length < 1) {
                    break;
                }

                for (int i = 0; i < permissions.length; i++) {
                    if (permissions[i] == android.Manifest.permission.ACCESS_FINE_LOCATION
                            && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        // permission was granted, yay! Do the
                        // contacts-related task you need to do.

                        mCameraFragment.mIsLocationPermissionGranted = true;

                        Log.d(LOG_TAG, "onRequestPermissionsResult, location permission granted!");

                    } else if (permissions[i] == android.Manifest.permission.ACCESS_FINE_LOCATION
                            && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        mCameraFragment.mIsLocationPermissionGranted = false;
                        mCameraFragment.mTakePicButton.setVisibility(View.INVISIBLE);
                        Log.d(LOG_TAG, "onRequestPermissionsResult, location permission denied!");
                    } else if (permissions[i] == Manifest.permission.WRITE_EXTERNAL_STORAGE
                            && grantResults[i] == PackageManager.PERMISSION_GRANTED) {

                        Log.d(LOG_TAG, "onRequestPermissionsResult, mIsStoragePermissionGranted granted!");
                        mCameraFragment.mIsStoragePermissionGranted = true;
                    } else if (permissions[i] == Manifest.permission.WRITE_EXTERNAL_STORAGE
                            && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        Log.d(LOG_TAG, "onRequestPermissionsResult, mIsStoragePermissionGranted denied!");
                        mCameraFragment.mIsStoragePermissionGranted = false;
                        mCameraFragment.mTakePicButton.setVisibility(View.INVISIBLE);
                    } else if (permissions[i] == Manifest.permission.CAMERA
                            && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        mCameraFragment.mIsCameraPermissionGranted = true;
                    } else if (permissions[i] == Manifest.permission.CAMERA
                            && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        mCameraFragment.mIsCameraPermissionGranted = false;
                        mCameraFragment.mTakePicButton.setVisibility(View.INVISIBLE);
                    }
                }


                break;


            default:
                Log.d(LOG_TAG, "onRequestPermissionsResult: undefined grantResults");
                break;
        }

    }
}
