package com.vhakulinen.pushtoolapp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;

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

    public static final String DEFAULT_BACKEND_ADDRESS = "https://lotsof.coffee/p/";

    public static final String BACKEND_ADDRESS = DEFAULT_BACKEND_ADDRESS;

    public static final String BACKEND_POOL_ADDRESS = BACKEND_ADDRESS + "pool/";
    public static final String BACKED_RETRIEVE_ADDRESS = BACKEND_ADDRESS + "retrieve/";
    public static final String BACKED_GCM_REGISTER = BACKEND_ADDRESS + "gcm/";

    public static boolean ON_BACKGROUD = true;

    private static final String SENDER_ID = "1096855422643";

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    public static final String PROPERTY_TOKEN_ID = "token_id";
    private static final String PROPERTY_APP_VERSION = "push_v0.01";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private final static String TAG = "PushTool";

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

        String fileContent = DataHelper.ReadAll(context);
        Log.i(TAG, fileContent);
        String[][] data;
        try {
            data = DataHelper.fromJSONArray(DataHelper.fromString(fileContent));
        } catch (Exception e) {
            Log.i(TAG, e.toString());
            return;
        }
        ((LinearLayout)mMainView).removeViews(0, ((LinearLayout)mMainView).getChildCount());
        for (String[] item : data) {
            addDataToMainView(item[0], item[1], item[2]);
        }
    }

    private void addDataToMainView(String title, String body, String date) {
        ViewGroup newView = (ViewGroup) LayoutInflater.from(context).inflate(
                R.layout.list_item, (ViewGroup)mMainView, false);
        ((ViewGroup) mMainView).addView(newView, 0);

        ((TextView) newView.findViewById(R.id.title)).setText(title);
        // ((TextView) newView.findViewById(R.id.title)).setTextColor(Color.BLACK);
        ((TextView) newView.findViewById(R.id.body)).setText(body);
        ((TextView) newView.findViewById(R.id.date)).setText(date);
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
            // context.sendBroadcast(new Intent("com.google.android.intent.action.GTALK_HEARTBEAT"));
            // context.sendBroadcast(new Intent("com.google.android.intent.action.MCS_HEARTBEAT"));
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
            String urlParams = String.format("email=%s&password=%s", email, password);

            try {
                URL url = new URL(MainActivity.BACKED_RETRIEVE_ADDRESS);
                URLConnection conn = url.openConnection();
                HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
                httpsConn.setRequestMethod("POST");
                httpsConn.setDoOutput(true); 
                httpsConn.setDoInput(true);

                // Send post request
                httpsConn.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(httpsConn.getOutputStream());
                wr.writeBytes(urlParams);
                wr.flush();
                wr.close();
         
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(httpsConn.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
         
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
         
                this.response = response.toString();
                // return httpsConn.getResponseCode();
                return httpsConn.getResponseCode();
            } catch (MalformedURLException e) {
                Log.v("MalformedURLException", e.toString());
                return internalError;
            } catch (IOException e) {
                Log.v("IOException", e.toString());
                return internalError;
            }
        }

        private Integer registerGCMToBackend(String token) {
            String urlParams = String.format("gcmid=%s&token=%s", regid, token);

            try {
                URL url = new URL(MainActivity.BACKED_GCM_REGISTER);
                URLConnection conn = url.openConnection();
                HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
                httpsConn.setRequestMethod("POST");
                httpsConn.setDoOutput(true); 
                httpsConn.setDoInput(true);

                // Send post request
                httpsConn.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(httpsConn.getOutputStream());
                wr.writeBytes(urlParams);
                wr.flush();
                wr.close();
         
                return httpsConn.getResponseCode();
            } catch (MalformedURLException e) {
                Log.v("MalformedURLException", e.toString());
                return internalError;
            } catch (IOException e) {
                Log.v("IOException", e.toString());
                return internalError;
            }
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
                String title;
                String body;
                Date time = new Date();

                try {
                    json = jsonArray.getJSONObject(i);
                    title = json.getString("Title");
                    body = json.getString("Body");

                    try {
                        Long timestamp = json.getLong("UnixTimeStamp");
                        if (timestamp != 0) {
                            time.setTime(timestamp*1000);
                        }
                    } catch (Exception e) {
                    }
                } catch (Exception e) {
                    Log.v("JSON", e.toString());
                    continue;
                }

                Format df = new SimpleDateFormat("MM/dd HH:mm");
                String dateString = df.format(time);

                addDataToMainView(title, body, dateString);
                // ViewGroup newView = (ViewGroup) LayoutInflater.from(context).inflate(
                        // R.layout.list_item, (ViewGroup)mMainView, false);
                // ((ViewGroup) mMainView).addView(newView, 0);

                // ((TextView) newView.findViewById(R.id.title)).setText(title);
                // ((TextView) newView.findViewById(R.id.body)).setText(body);
                // ((TextView) newView.findViewById(R.id.date)).setText(dateString);
                // if (MainActivity.ON_BACKGROUD) {
                    // Notification n  = new Notification.Builder(MainActivity.this)
                            // .setContentTitle(title)
                            // .setContentText(body)
                            // .setSmallIcon(R.drawable.ic_launcher)
                            // .setAutoCancel(true).build();

                    // NotificationManager notificationManager = 
                      // (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                    // notificationManager.notify(0, n); 
                // }
            }
        }
    }
}
