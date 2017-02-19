package roca.bajet.com.straggle;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

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
}
