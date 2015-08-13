package com.androidsources.googlepluslogin;

import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

public class MainActivity extends AppCompatActivity implements OnClickListener,
        GoogleApiClient.ConnectionCallbacks, OnConnectionFailedListener {

    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    /* A flag indicating that a PendingIntent is in progress and prevents
   * us from starting further intents.
   */
    private boolean mIntentInProgress;

    private boolean mShouldResolve;

    private ConnectionResult connectionResult;

    private SignInButton signInButton;
    private Button signOutButton;
    private TextView tvName, tvMail, tvNotSignedIn;
    private LinearLayout viewContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signOutButton = (Button) findViewById(R.id.sign_out_button);
        tvName = (TextView) findViewById(R.id.tvName);
        tvMail = (TextView) findViewById(R.id.tvMail);
        tvNotSignedIn = (TextView) findViewById(R.id.notSignedIn_tv);
        viewContainer = (LinearLayout) findViewById(R.id.text_view_container);


        Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);

        signInButton.setOnClickListener(this);
        signOutButton.setOnClickListener(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();

    }

    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }


    private void resolveSignInError() {
        if (connectionResult.hasResolution()) {
            try {
                mIntentInProgress = true;
                connectionResult.startResolutionForResult(this, RC_SIGN_IN);
            } catch (SendIntentException e) {
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }

    /*
    When the GoogleApiClient object is unable to establish a connection onConnectionFailed() is called
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!result.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this,
                    0).show();
            return;
        }

        if (!mIntentInProgress) {

            connectionResult = result;

            if (mShouldResolve) {

                resolveSignInError();
            }
        }

    }

    /*
    onConnectionFailed() was started with startIntentSenderForResult and the code RC_SIGN_IN,
    we can capture the result inside Activity.onActivityResult.
     */
    @Override
    protected void onActivityResult(int requestCode, int responseCode,
                                    Intent intent) {
        if (requestCode == RC_SIGN_IN) {
            if (responseCode != RESULT_OK) {
                mShouldResolve = false;
            }

            mIntentInProgress = false;

            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onConnected(Bundle arg0) {
        mShouldResolve = false;
        try {
            if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
                Person person = Plus.PeopleApi
                        .getCurrentPerson(mGoogleApiClient);
                String personName = person.getDisplayName();
                String email = Plus.AccountApi.getAccountName(mGoogleApiClient);

                tvName.setText(personName);
                tvMail.setText(email);

                Toast.makeText(getApplicationContext(),
                        "You are Logged In " + personName, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "Couldnt Get the Person Info", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        signOutUI();

    }

    private void signOutUI() {
        signInButton.setVisibility(View.GONE);
        tvNotSignedIn.setVisibility(View.GONE);
        signOutButton.setVisibility(View.VISIBLE);
        viewContainer.setVisibility(View.VISIBLE);
    }

    private void signInUI() {
        signInButton.setVisibility(View.VISIBLE);
        tvNotSignedIn.setVisibility(View.VISIBLE);
        signOutButton.setVisibility(View.GONE);
        viewContainer.setVisibility(View.GONE);
    }

    /**
     * Fetching user's information name, email, profile pic
     */
    private void getProfileInformation() {

    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
        signInUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                onSignInClicked();
                break;
            case R.id.sign_out_button:
                onSignOutClicked();
                break;
        }
    }


    private void onSignInClicked() {
        if (!mGoogleApiClient.isConnecting()) {
            mShouldResolve = true;
            resolveSignInError();
        }
    }


    private void onSignOutClicked() {
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
            signInUI();
        }
    }

}
