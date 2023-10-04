package com.example.holo_marine;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private LocationManager locationManager;

    private DatabaseReference messageData;

    ImageView bg_marine;

    ImageButton btn_force;

    TypedArray typedArray_marine; // 숫자 이미지 배열

    double latitude;
    double longtitude;

    int networkStatus = 0;

    int pushStatus = 0;

    int loadImg = 0;

    String addressResult = null;

    List<Address> address = null;

    int shutdownCount = 0;

    private static final String FCM_MESSAGE_URL = "https://fcm.googleapis.com/fcm/send";
    private static final String SERVER_KEY = "구글 파이어베이스 서버키를 입력";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
            }
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        typedArray_marine = getResources().obtainTypedArray(R.array.png_marine); //chara_select가 배열 번호가된다.

        btn_force = findViewById(R.id.btn_force);

        bg_marine = findViewById(R.id.bg_marine);

        Handler gps_holoMarine_Handler = new Handler();

        gps_holoMarine_Handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (loadImg >= typedArray_marine.length()) {
                    loadImg = 38;
                }
                else {
                    bg_marine.setImageResource(typedArray_marine.getResourceId(loadImg, -1));
                    loadImg++;
                }
                gps_holoMarine_Handler.postDelayed(this, 30);
            }
        }, 0);

        btn_force.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.exit(0);
            }
        });


        Handler network_Handler = new Handler();

        Handler shutdown_Handler = new Handler();


        network_Handler.postDelayed(new Runnable() {

            @Override
            public void run() {


                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        network_check();


                        if (networkStatus == 1 && pushStatus == 0) {
                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            messageData = database.getReference("message");

                            gps_now_location_check();
                            String msg;

                            if (latitude == 0.0 && longtitude == 0.0) {
                                msg = "Ahoy! 출항 준비 완료했다구~!\n나랑 같이 모험하고 싶다면 어서 오라구~!";
                            }
                            else {
                                Geocoder g = new Geocoder(MainActivity.this);

                                try {
                                    address = g.getFromLocation(latitude, longtitude, 10);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    System.out.println("입출력 오류");
                                }
                                if (address != null) {
                                    if (address.size() == 0) {
                                        System.out.println("주소 오류");
                                    }
                                    else {
                                        addressResult = address.get(0).getAddressLine(0);
                                    }
                                }
                                msg = "Ahoy! 출항 준비 완료했다구~!\n\"" + addressResult + "\" 에서 기다리고 있을테니 어서오라구~!";
                            }


                            String name = "호쇼 마린";

                            Chat chat = new Chat();
                            chat.setName(name);
                            chat.setMsg(msg);

                            //메시지를 파이어베이스에 보냄.
                            messageData.push().setValue(chat);
                            send_Push(name, msg);


                            shutdown_Handler.postDelayed(new Runnable() {

                                @Override
                                public void run() {
                                    pushStatus = 1;
                                    shutdownCount++;
                                    if (shutdownCount >= 10) {
                                        System.exit(0);
                                    }

                                    network_Handler.postDelayed(this, 1000);
                                }
                            }, 0);


                        }

                    }

                }).start();


                network_Handler.postDelayed(this, 1000);
            }
        }, 0);

    }

    private void network_check() {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "");

        Request request = new Request.Builder().url("https://www.google.com").post(body).addHeader("accept", "application/json").addHeader("Content-Type", "application/x-www-form-urlencoded").addHeader("appKey", "l7xxc799eea58b0b44619f9d95ae52d6af2c").build();

        Response response = null;
        try {
            response = client.newCall(request).execute();
            networkStatus = 1;
            System.out.println("network ok");
        } catch (IOException e) {
            //네트워크 끊어짐
            System.err.println("network error");
        }
    }


    @SuppressLint("MissingPermission")
    public void gps_now_location_check() {
        //현재 위도와 경도 좌표 수신
        @SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        try {
            latitude = location.getLatitude();
            longtitude = location.getLongitude();
        } catch (NullPointerException e) {

            latitude = 0.0;
            longtitude = 0.0;

        }
        System.out.println("위도와 경도 : " + latitude + ", " + longtitude); // 값이 비어있는경우 0.0 이 들어오는거 확인 완료.


    }

    private void send_Push(String name, String msg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // FMC 메시지 생성 start
                    JSONObject root = new JSONObject();
                    JSONObject notification = new JSONObject();
                    notification.put("body", msg);
                    notification.put("title", name);
                    root.put("notification", notification);
                    root.put("to", "스마트폰 고유 토큰 입력"); // 이걸건드리는거다 ㅅㅂ
                    // FMC 메시지 생성 end

                    URL Url = new URL(FCM_MESSAGE_URL);
                    HttpURLConnection conn = (HttpURLConnection) Url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    conn.addRequestProperty("Authorization", "key=" + SERVER_KEY);
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setRequestProperty("Content-type", "application/json");
                    OutputStream os = conn.getOutputStream();
                    os.write(root.toString().getBytes("utf-8"));
                    os.flush();
                    conn.getResponseCode();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }
}