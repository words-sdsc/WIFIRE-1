package com.example.yuzur.maps;

import android.app.NotificationManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.SimpleTimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class UploadFileService extends JobService {
    private static final String TAG = MapsActivity.class.getSimpleName();




    public String encode(String key, String data) {
        try {

            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            String encoded = new String(Hex.encodeHex(sha256_HMAC.doFinal(data.getBytes("UTF-8"))));
            Log.wtf(TAG, encoded);
            Log.wtf(TAG, data);
            return encoded;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                Date date = new Date();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                sdf.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
                String timestamp = sdf.format(date);

                try {
                    final String user = (String) params.getExtras().get("user");
                    final String publicKey = (String) params.getExtras().get("publicKey");
                    final String privateKey = (String) params.getExtras().get("privateKey");

                    String url_String = String.format("https://swat.sdsc.edu:5443/users/%s/images?publicKey=%s",user, publicKey);
                    URL url = new URL(url_String);

                    String data = "POST" + "|" + url_String + '|' + publicKey + '|' + timestamp;

                    String signature = encode(privateKey, data);

                    connection = (HttpURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("X-Imbo-Authenticate-Signature", signature);
                    connection.setRequestProperty("X-Imbo-Authenticate-Timestamp", timestamp);


                    //Write data
                    FileInputStream instream = new FileInputStream((String)params.getExtras().get("input"));
                    IOUtils.copy(instream, connection.getOutputStream());

                    //Server Response
                    String res = IOUtils.toString(connection.getInputStream());
                    Log.wtf(TAG,"Response: " + res);


                }
                catch ( MalformedURLException e){
                    Log.wtf(TAG, "Bad URL");
                }
                catch (IOException e){
                    Log.wtf(TAG, "Connection Error");
                }
                finally {
                    if ( connection != null){
                        connection.disconnect();
                        Log.wtf(TAG, (String) params.getExtras().get("input"));
                        if(params.getExtras().get("delete").equals("true"))
                        {
                            File image = new File((String) params.getExtras().get("input"));
                            image.delete();
                        }

                        jobFinished(params, false);
                    }
                }
            }
        });

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        //mJobHandler.removeMessages( 1 );
        return false;
    }
}
