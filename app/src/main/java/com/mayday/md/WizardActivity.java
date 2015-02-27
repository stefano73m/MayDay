package com.mayday.md;


import com.mayday.md.common.AppConstants;
import com.mayday.md.common.ApplicationSettings;
import com.mayday.md.common.ContactPickerFragment;
import com.mayday.md.common.MyTagHandler;
import com.mayday.md.data.PBDatabase;
import com.mayday.md.fragment.LanguageSettingsFragment;
import com.mayday.md.fragment.SetupCodeFragment;
import com.mayday.md.fragment.SetupContactsFragment;
import com.mayday.md.fragment.SetupMessageFragment;
import com.mayday.md.fragment.SimpleFragment;
import com.mayday.md.fragment.WarningFragment;
import com.mayday.md.fragment.WizardAlarmTestDisguiseFragment;
import com.mayday.md.fragment.WizardAlarmTestHardwareFragment;
import com.mayday.md.fragment.WizardTestDisguiseCodeFragment;
import com.mayday.md.fragment.WizardTestDisguiseOpenFragment;
import com.mayday.md.fragment.WizardTestDisguiseUnlockFragment;
import com.mayday.md.model.Page;
import com.mayday.md.trigger.HardwareTriggerService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


public class WizardActivity extends BaseFragmentActivity {

    Page currentPage;
    String pageId;
    String selectedLang;

    TextView tvToastMessage;
    Boolean flagRiseFromPause = false;

    private static final int PICK_CONTACT_REQUEST_ID = 65636;
    private static final int RESULT_OK = -1;

    private Handler inactiveHandler = new Handler();

    ContactPickerFragment contactPickerFragment = new ContactPickerFragment();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.root_layout);

        tvToastMessage = (TextView) findViewById(R.id.tv_toast);

        try {
            pageId = getIntent().getExtras().getString("page_id");
        } catch (Exception e) {
            pageId = "home-not-configured";
            e.printStackTrace();
        }
        selectedLang = ApplicationSettings.getSelectedLanguage(this);

        Log.e("WizardActivity.onCreate", "pageId = " + pageId);

        PBDatabase dbInstance = new PBDatabase(this);
        dbInstance.open();
        currentPage = dbInstance.retrievePage(pageId, selectedLang);
        dbInstance.close();

        if (currentPage == null) {
            Log.e(">>>>>>", "page = null");
            Toast.makeText(this, "Still to be implemented.", Toast.LENGTH_SHORT).show();
            AppConstants.PAGE_FROM_NOT_IMPLEMENTED = true;
            finish();
            return;
        } else if (currentPage.getId().equals("home-ready")) {
//            ApplicationSettings.setFirstRun(WizardActivity.this, false);
            ApplicationSettings.setWizardState(WizardActivity.this, AppConstants.WIZARD_FLAG_HOME_READY);
            changeAppIcontoCalculator();

            startService(new Intent(this, HardwareTriggerService.class));

            Intent i = new Intent(WizardActivity.this, MainActivity.class);
            i.putExtra("page_id", pageId);
            startActivity(i);

            callFinishActivityReceiver();

            finish();
            return;
        } else {

            /*
            setup the milestone of the wizard state if the app flow reaches to THREE specific page,
            i.e. home-not-configured, home-not-configured-alarm and home-not-configured-disguise.
             */
            if (currentPage.getId().equals("home-not-configured")) {
                ApplicationSettings.setWizardState(WizardActivity.this, AppConstants.WIZARD_FLAG_HOME_NOT_CONFIGURED);
            } else if (currentPage.getId().equals("home-not-configured-alarm")) {
                ApplicationSettings.setWizardState(WizardActivity.this, AppConstants.WIZARD_FLAG_HOME_NOT_CONFIGURED_ALARM);
            } else if (currentPage.getId().equals("home-not-configured-disguise")) {
                ApplicationSettings.setWizardState(WizardActivity.this, AppConstants.WIZARD_FLAG_HOME_NOT_CONFIGURED_DISGUISE);
            }

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            Fragment fragment = null;

            if (currentPage.getType().equals("simple")) {
                tvToastMessage.setVisibility(View.INVISIBLE);
                fragment = new SimpleFragment().newInstance(pageId, AppConstants.FROM_WIZARD_ACTIVITY);
            } else if (currentPage.getType().equals("warning")) {
                tvToastMessage.setVisibility(View.INVISIBLE);
                fragment = new WarningFragment().newInstance(pageId, AppConstants.FROM_WIZARD_ACTIVITY);
            } else {          // type = interactive
                if (currentPage.getComponent().equals("contacts"))
                    fragment = new SetupContactsFragment().newInstance(pageId, AppConstants.FROM_WIZARD_ACTIVITY);
                else if (currentPage.getComponent().equals("message"))
                    fragment = new SetupMessageFragment().newInstance(pageId, AppConstants.FROM_WIZARD_ACTIVITY);
                else if (currentPage.getComponent().equals("code"))
                    fragment = new SetupCodeFragment().newInstance(pageId, AppConstants.FROM_WIZARD_ACTIVITY);
                else if (currentPage.getComponent().equals("language"))
                    fragment = new LanguageSettingsFragment().newInstance(pageId, AppConstants.FROM_WIZARD_ACTIVITY);
                else if (currentPage.getComponent().equals("alarm-test-hardware")) {
                    tvToastMessage.setVisibility(View.VISIBLE);
                    if (currentPage.getIntroduction() != null) {
                        tvToastMessage.setText(Html.fromHtml(currentPage.getIntroduction(), null, new MyTagHandler()));
                    }
                    fragment = new WizardAlarmTestHardwareFragment().newInstance(pageId);
                } else if (currentPage.getComponent().equals("alarm-test-disguise")) {
                    tvToastMessage.setVisibility(View.VISIBLE);
                    if (currentPage.getIntroduction() != null) {
                        tvToastMessage.setText(Html.fromHtml(currentPage.getIntroduction(), null, new MyTagHandler()));
                    }
                    fragment = new WizardAlarmTestDisguiseFragment().newInstance(pageId);
                } else if (currentPage.getComponent().equals("disguise-test-open")) {
                    findViewById(R.id.wizard_layout_root).setBackgroundColor(Color.BLACK);
                    tvToastMessage.setVisibility(View.VISIBLE);
                    if (currentPage.getIntroduction() != null) {
                        tvToastMessage.setText(Html.fromHtml(currentPage.getIntroduction(), null, new MyTagHandler()));
                    }
                    fragment = new WizardTestDisguiseOpenFragment().newInstance(pageId);
                } else if (currentPage.getComponent().equals("disguise-test-unlock")) {
                    tvToastMessage.setVisibility(View.VISIBLE);
                    if (currentPage.getIntroduction() != null) {
                        tvToastMessage.setText(Html.fromHtml(currentPage.getIntroduction(), null, new MyTagHandler()));
                    }

                    fragment = new WizardTestDisguiseUnlockFragment().newInstance(pageId);
                } else if (currentPage.getComponent().equals("disguise-test-code")) {
                    tvToastMessage.setVisibility(View.VISIBLE);
                    if (currentPage.getIntroduction() != null) {
                        tvToastMessage.setText(Html.fromHtml(currentPage.getIntroduction(), null, new MyTagHandler()));
                    }
                    fragment = new WizardTestDisguiseCodeFragment().newInstance(pageId);
                } else
                    fragment = new SimpleFragment().newInstance(pageId, AppConstants.FROM_WIZARD_ACTIVITY);
            }
            fragmentTransaction.add(R.id.fragment_container, fragment);
            fragmentTransaction.commit();
        }
    }

    private void changeAppIcontoCalculator() {
        Log.e("WizardActivity.changeAppIcontoCalculator", "");

/*
        getPackageManager().setComponentEnabledSetting(
                new ComponentName("com.mayday.md", "com.mayday.md.HomeActivity-calculator"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        getPackageManager().setComponentEnabledSetting(
                new ComponentName("com.mayday.md", "com.mayday.md.HomeActivity-setup"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("WizardActivity.onPause", "page = " + pageId);

//        if (currentPage.getId().equals("home-ready") && ApplicationSettings.isRestartedSetup(WizardActivity.this)) {
//            Log.e("WizardActivity.onPause", "false->RestartedSetup");
//            ApplicationSettings.setRestartedSetup(WizardActivity.this, false);
//        }

        /*
        It's part of setup-alarm-test-hardware page testing to press power button consecutively 5 times that calls the activity's onPause method every time
        the device goes locked & onResume method every time the device is unlocked. That's why we omit this page for this below condition, otherwise every time
        device goes to pause state by pressing power button, flagRiseFromPause becomes true & display home-not-configured-alarm page, thus our test can't be successful.

        In short, we block this page - setup-alarm-test-hardware for pause-resume action
         */
        if (!pageId.equals("setup-alarm-test-hardware")) {
            Log.e(">>>>>>", "assert flagRiseFromPause = " + true);
            flagRiseFromPause = true;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("WizardActivity.onStop", "page = " + pageId);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("WizardActivity", "onDestroy");
        inactiveHandler.removeCallbacks(runnableInteractive);
    }



    @Override
    protected void onStart() {
        super.onStart();
        Log.d("WizardActivity.onStart", "page = " + pageId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("WizardActivity.onResume", "pageId = " + pageId + " and flagRiseFromPause = " + flagRiseFromPause);

        int wizardState = ApplicationSettings.getWizardState(WizardActivity.this);

        /*
        Check-1
        if this page is resumed from the page still not implemented, then we'll handle it here.
        If we don't do this check, then the resume procedure falls under Check-3 & execute that code snippet, which is not proper.
         */
        if (AppConstants.PAGE_FROM_NOT_IMPLEMENTED) {
            Log.e("WizardActivity.onResume", "returning from not-implemented page.");
            AppConstants.PAGE_FROM_NOT_IMPLEMENTED = false;
            return;
        }

        /*
        Check-2
        if this page is resumed by navigating-back from the next page, then we'll handle it here.
        If we don't do this check, then the resume procedure falls under Check-3 & execute that code snippet, which is not proper.
         */
        if (AppConstants.IS_BACK_BUTTON_PRESSED) {
            Log.e("WizardActivity.onResume", "back button pressed");
            AppConstants.IS_BACK_BUTTON_PRESSED = false;
            return;
        }

        /*
        Check-3
        The below code snippet partitions the wizard into 3 check-point, so if we pass the latest one, that will be marked as the opening page of the app
        if we closes the app at some point of wizard-configuration & resume later. We have 2 conditions to make this code executed -

        1. flagRiseFromPause -
        Every time device gets resumed, flagRiseFromPause becomes true(except for setup-alarm-test-hardware) & we only want this part of code executed in that case.
        if we don't user flagRiseFromPause flag, every-time activity comes to onResume, this code executes, calls another activity, which will make an infinite loop.

        2. !pageId.equals("setup-alarm-test-hardware-success") -
        It's part of setup-alarm-test-hardware page test to press power button consecutively 5 times that calls the activity's onPause
        method every time the device goes locked & onResume method every time the device is unlocked. setup-alarm-test-hardware-success
        comes when we successfully test this scenario.
        So setup-alarm-test-hardware-success always comes with flagRiseFromPause = true, so we have to handle this page explicitly so that app shouldn't
        go to the mile-stone page every time this page loads.
        Side Effect - if we are in this page - setup-alarm-test-hardware-success & go to home & come back, still this page will be there as the
        opening page of the app.
         */
        if (flagRiseFromPause && !pageId.equals("setup-alarm-test-hardware-success")) {
            flagRiseFromPause = false;

            if (wizardState == AppConstants.WIZARD_FLAG_HOME_NOT_CONFIGURED) {
                pageId = "home-not-configured";
            } else if (wizardState == AppConstants.WIZARD_FLAG_HOME_NOT_CONFIGURED_ALARM) {
                pageId = "home-not-configured-alarm";
            } else if (wizardState == AppConstants.WIZARD_FLAG_HOME_NOT_CONFIGURED_DISGUISE) {
                pageId = "home-not-configured-disguise";
            } else if (wizardState == AppConstants.WIZARD_FLAG_HOME_READY) {
                pageId = "home-ready";
            }

            Intent i = new Intent(WizardActivity.this, WizardActivity.class);
            i.putExtra("page_id", pageId);
            startActivity(i);

            callFinishActivityReceiver();
            finish();
        }
    }

    @Override
    public void onUserInteraction() {
        Log.e("WizardActivity", "onUserInteraction");
        super.onUserInteraction();
        hideToastMessageInInteractiveFragment();
        if (currentPage != null && currentPage.getComponent() != null &&
                (
                        currentPage.getComponent().equals("alarm-test-hardware")
                                || currentPage.getComponent().equals("alarm-test-disguise")
                                || currentPage.getComponent().equals("disguise-test-open")
                                || currentPage.getComponent().equals("disguise-test-unlock")
                                || currentPage.getComponent().equals("disguise-test-code")
                )
                ) {
            inactiveHandler.postDelayed(runnableInteractive, Integer.parseInt(currentPage.getTimers().getFail()) * 1000);
        }
    }

    public void hideToastMessageInInteractiveFragment() {
        tvToastMessage.setVisibility(View.INVISIBLE);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AppConstants.IS_BACK_BUTTON_PRESSED = true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//if ((requestCode == PICK_CONTACT_REQUEST_ID) && (resultCode == RESULT_OK)) {
//Context context = getApplicationContext();
//contactPickerFragment.onActivityResult(requestCode, resultCode, data, context);
//}

        Log.i("", "Called from WizardActivity");

        super.onActivityResult(requestCode, resultCode, data);
    }

    private Runnable runnableInteractive = new Runnable() {
        public void run() {

            String pageId = currentPage.getFailedId();

            Intent i = new Intent(WizardActivity.this, WizardActivity.class);
            i.putExtra("page_id", pageId);
            startActivity(i);
            finish();
        }
    };


}
