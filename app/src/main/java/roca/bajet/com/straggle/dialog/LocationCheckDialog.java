package roca.bajet.com.straggle.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import roca.bajet.com.straggle.R;

/**
 * Created by Arnold on 5/30/2017.
 */

public class LocationCheckDialog extends DialogFragment {

    private final String LOG_TAG = "LocationCheckDialog";

    public interface LocationCheckDialogCallback {
        void onPositiveButton();

        void onNegativeButton();

        void onCancelled();
    }

    public LocationCheckDialogCallback mLocationCheckDialogCallback;

    public void setOnLocationCheckDialogCallback(LocationCheckDialogCallback cb) {
        mLocationCheckDialogCallback = cb;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.location_check_dialog_title)
                .setMessage(R.string.location_check_dialog_message)
                .setPositiveButton(R.string.location_check_dialog_positive_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Log.d(LOG_TAG, "onPositiveButton...");

                        if (mLocationCheckDialogCallback != null) {
                            mLocationCheckDialogCallback.onPositiveButton();
                        }

                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));

                    }
                })
                .setNegativeButton(R.string.location_check_dialog_negative_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Log.d(LOG_TAG, "onNegativeButton...");

                        if (mLocationCheckDialogCallback != null) {
                            mLocationCheckDialogCallback.onNegativeButton();
                        }

                        dismiss();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        Log.d(LOG_TAG, "onCancel...");


        if (mLocationCheckDialogCallback != null) {
            mLocationCheckDialogCallback.onCancelled();
        }
        super.onCancel(dialog);
    }
}
