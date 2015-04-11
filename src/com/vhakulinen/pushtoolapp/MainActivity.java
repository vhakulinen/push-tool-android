package com.vhakulinen.pushtoolapp;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    public static boolean ON_BACKGROUD = true;

    private static final String SENDER_ID = "1096855422643";

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    public static final String PROPERTY_TOKEN_ID = "token_id";
    private static final String PROPERTY_APP_VERSION = "push_v0.01";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private final static String TAG = "PushTool";

    // Int to keep record of how many entries we have on the main view displayed
    private int displayedDataCount = 0;

    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    SharedPreferences prefs;
    Context context;

    String regid;
    String token;

    ProgressDialog dialog;
    GcmIntentServiceReceiver receiver;

    private View mRetrieveView;
    private View mMainView;
    private View mMainContainer;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        context = getApplicationContext();

        mMainView = findViewById(R.id.main_view);
        mMainContainer = findViewById(R.id.main_container);
        mRetrieveView = findViewById(R.id.retrieve_view);

        IntentFilter filter = new IntentFilter(GcmIntentServiceReceiver.PROCESS_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new GcmIntentServiceReceiver();
        registerReceiver(receiver, filter);

        MainActivity.ON_BACKGROUD = false;

        final SwipeRefreshLayout swipe = (SwipeRefreshLayout) findViewById(R.id.swipe);
        swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                class retrieveData extends AsyncTask<Void, String, String> {
                    Response response;
                    protected String doInBackground(Void... params) {
                        try {
                            response = BackendHelper.receiveDataFromBackend(
                                    getApplicationContext());
                        } catch (Exception e) {
                            return "";
                        }
                        return "";
                    }

                    protected void onPostExecute(String msg) {
                        if (!response.getMessage().equals("")) {
                            PushDataSource db = new PushDataSource(getApplicationContext());
                            JSONArray arr;
                            try {
                                arr = DataHelper.fromString(response.getMessage());
                            } catch (Exception e) {
                                return;
                            }
                            db.open();
                            for (int i = 0; i < arr.length(); i++) {
                                PushData p;

                                try {
                                    JSONObject json = arr.getJSONObject(i);
                                    p = DataHelper.fromJSONObject(json);
                                } catch (Exception e) {
                                    Log.v(TAG, "FAIL: " + e.toString());
                                    continue;
                                }

                                addDataToMainView(p.getTitle(), p.getBody(),
                                        p.getTime());
                                db.savePushData(p);
                            }
                            db.close();
                        }
                        swipe.setRefreshing(false);;
                    }
                }
                new retrieveData().execute(null, null, null);
            }
        });

        if (checkPlayServices()) {
            // Go on...
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);
            token = getToken(context);

            if (regid.isEmpty()) {
                registerInBackground();
            }
            if (token.isEmpty()) {
                mMainContainer.setVisibility(View.GONE);
                mRetrieveView.setVisibility(View.VISIBLE);
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
	}

    public void onRetrieve(View v) {
        String email;
        String password;

        email = ((TextView) findViewById(R.id.email)).getText().toString();
        password = ((TextView) findViewById(R.id.password)).getText().toString();

        InputMethodManager inputManager = (InputMethodManager)
                                          getSystemService(Context.INPUT_METHOD_SERVICE); 

        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                                             InputMethodManager.HIDE_NOT_ALWAYS);

        if (!regid.isEmpty()) {
            new RetrieveToken().execute(new String[]{email, password});
        } else {
            makeToast("Can't get the token yet (no GCM id)");
        }
    }

    private String getToken(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String token = prefs.getString(PROPERTY_TOKEN_ID, "");
        if (token.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        return token;
    }

    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }

        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    private void registerInBackground() {
        class registerInBg extends AsyncTask<Void, String, String> {
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registratio ID=" + regid;
                    Log.i(TAG, msg);

                    storeRegistrationId(context, regid);
                } catch (IOException e) {
                    msg = "Error :" + e.getMessage();
                }

                return msg;
            }

            protected void onPostExecute(String msg) {
            }
        }
        new registerInBg().execute(null, null, null);
    }

    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    private SharedPreferences getGCMPreferences(Context context) {
        return getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        MainActivity.ON_BACKGROUD = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        MainActivity.ON_BACKGROUD = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
        MainActivity.ON_BACKGROUD = false;
        this.displayedDataCount = 0;

        PushDataSource db = new PushDataSource(context);
        db.open();
        List<PushData> data = db.getNextXFrom(this.displayedDataCount, 20);
        db.close();

        ((LinearLayout)mMainView).removeViews(0, ((LinearLayout)mMainView).getChildCount());

        for (PushData d : data) {
            addDataToMainView(d.getTitle(), d.getBody(), d.getTime());
        }
    }

    private void addDataToMainView(String title, String body, String date) {
        ViewGroup newView = (ViewGroup) LayoutInflater.from(context).inflate(
                R.layout.list_item, (ViewGroup)mMainView, false);
        ((ViewGroup) mMainView).addView(newView, 0);

        ((TextView) newView.findViewById(R.id.title)).setText(title);
        ((TextView) newView.findViewById(R.id.body)).setText(body);
        ((TextView) newView.findViewById(R.id.date)).setText(date);

        this.displayedDataCount++;
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
            addDataToMainView("title","bod","date");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

    private void makeToast(String text) {
        Toast.makeText(getApplicationContext(), text, 5000).show();
    }

    private void saveToken(String token) {
        final SharedPreferences prefs = getGCMPreferences(context);
        Log.i(TAG, "Saveing token");
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_TOKEN_ID, token);
        editor.commit();
    }

    private class RetrieveToken extends AsyncTask<String, Void, Integer> {
        private final Integer internalError = 0;

        private String response;

        private boolean tokenOk = false;
        private boolean regTokenOk = false;

        private Integer getTokenFromBackend(String email, String password) {
            Response response;
            try {
                response = BackendHelper.getTokenFromBackend(email, password);
            } catch (Exception e) {
                return internalError;
            }
            this.response = response.getMessage();
            return response.getCode();
        }

        private Integer registerGCMToBackend(String token) {
            Response response;
            try {
                response = BackendHelper.registerGCMToBackend(token, regid);
            } catch (Exception e) {
                return internalError;
            }
            return response.getCode();
        }

        protected Integer doInBackground(String... details) {
            String email = details[0];
            String password = details[1];
            if (getTokenFromBackend(email, password) == 200) {
                tokenOk = true;
                if (registerGCMToBackend(this.response) == 200) {
                    regTokenOk = true;
                }
            }
            return 0;
        }

        protected void onPreExecute() {
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage("Retrieving token...");
            dialog.show();
        }

        protected void onPostExecute(Integer result) {
            dialog.dismiss();
            if (tokenOk && regTokenOk) {
                // Ok
                saveToken(this.response);
                mRetrieveView.setVisibility(View.GONE);
                mMainContainer.setVisibility(View.VISIBLE);
            } else {
                // Error
                makeToast("Failed to retreive the token");
            }
        }
    }

    public class GcmIntentServiceReceiver extends BroadcastReceiver {
 
        public static final String PROCESS_RESPONSE = "com.vhakulinen.pushtoolapp.intent.action.PROCESS_RESPONSE";
 
        @Override
        public void onReceive(Context context, Intent intent) {
            String responseMessage = intent.getStringExtra(GcmIntentService.RESPONSE_MESSAGE);
            JSONArray jsonArray;

            try {
                responseMessage = responseMessage.replace("}{", "},{");
                jsonArray = new JSONArray("[ " + responseMessage + " ]");
            } catch (Exception e) {
                Log.v("JSON", e.toString());
                return;
            }
 
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json;
                PushData p;
                Date time = new Date();

                try {
                    json = jsonArray.getJSONObject(i);
                    p = DataHelper.fromJSONObject(json);
                } catch (Exception e) {
                    continue;
                }

                time.setTime(p.getTimestamp());

                addDataToMainView(p.getTitle(), p.getBody(), p.getTime());
            }
        }
    }
}
