package roca.bajet.com.straggle;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private final String LOG_TAG = getClass().getSimpleName();
    CameraFragment mCameraFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);


        FragmentManager manager = getSupportFragmentManager();
        mCameraFragment = (CameraFragment)manager.findFragmentById(R.id.fragment_container_activity_main);
        if (mCameraFragment == null) {
            mCameraFragment = CameraFragment.newInstance();
            manager.beginTransaction()
                    .add(R.id.fragment_container_activity_main, mCameraFragment)
                    .commit();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        Log.d(LOG_TAG, "onRequestPermissionsResult...");

        if (requestCode == CameraFragment.PERMISSION_REQUEST_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                mCameraFragment.mIsLocationGranted = true;
            }
            else if (grantResults[0] == PackageManager.PERMISSION_DENIED)
            {
                mCameraFragment.mIsLocationGranted = false;
            }

            Log.d(LOG_TAG, "onRequestPermissionsResult: undefined grantResults");
        }
    }
}
