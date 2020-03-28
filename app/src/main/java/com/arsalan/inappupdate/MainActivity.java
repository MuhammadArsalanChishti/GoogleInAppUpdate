package com.arsalan.inappupdate;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.OnSuccessListener;

public class MainActivity extends AppCompatActivity implements OnSuccessListener<AppUpdateInfo> {

    private AppUpdateManager appUpdateManager;
    private boolean mNeedsFlexibleUpdate;
    public static final int REQUEST_CODE = 1234;
    private TextView mVersionNumber;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mVersionNumber = findViewById(R.id.mainVersionNumber);
        setVersionText();

        mNeedsFlexibleUpdate = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(this);
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        appUpdateManager = AppUpdateManagerFactory.create(context);
        return super.onCreateView(parent, name, context, attrs);
    }

    private void setVersionText() {
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (Exception e) {

        }
        mVersionNumber.setText(""+packageInfo.versionName);
    }

    @Override
    public void onSuccess(AppUpdateInfo appUpdateInfo) {
        if (appUpdateInfo.updateAvailability()
                == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
            // If an in-app update is already running, resume the update.
            startUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE);
        } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
            // If the update is downloaded but not installed,
            // notify the user to complete the update.
            popupSnackbarForCompleteUpdate();
        } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
            if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                startUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE);
            } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                mNeedsFlexibleUpdate = true;
                showFlexibleUpdateNotification();
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Log.i(MainActivity.class.getSimpleName(), "Update flow completed! Result code: " + resultCode);
            } else {
                Log.e(MainActivity.class.getSimpleName(), "Update flow failed! Result code: " + resultCode);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void startUpdate(final AppUpdateInfo appUpdateInfo, final int appUpdateType) {
        final Activity activity = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    appUpdateManager.startUpdateFlowForResult(appUpdateInfo,
                            appUpdateType,
                            activity,
                            REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /* Displays the snackbar notification and call to action. */
    private void popupSnackbarForCompleteUpdate() {
        Snackbar snackbar =
                Snackbar.make(
                        findViewById(R.id.main_activity),
                        "An update has just been downloaded.",
                        Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("RESTART", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                appUpdateManager.completeUpdate();
            }
        });
        snackbar.setActionTextColor(getResources().getColor(R.color.colorAccent));
        snackbar.show();
    }

    private void showFlexibleUpdateNotification() {
        Snackbar snackbar =
                Snackbar.make(
                        findViewById(R.id.main_activity),
                        "An update is available and accessible in More.",
                        Snackbar.LENGTH_LONG);
        snackbar.show();
    }
}
