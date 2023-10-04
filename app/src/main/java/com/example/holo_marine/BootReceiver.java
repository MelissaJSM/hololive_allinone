package com.example.holo_marine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        // 전달된 값이 '부팅완료' 인 경우에만 동작 하도록 조건문을 설정
        if (action.equals("android.intent.action.BOOT_COMPLETED")) {
            Log.v("Test", "Boot");
            // 부팅 이후 처리해야 코드 작성
            // 액티비티 호출
            Intent i = new Intent(context,  MainActivity.class);

            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
            // 서비스 호출 하고 싶으면 서비스도 같이 실행됨, AndroidManifest.xml에 <service>추가 해야함
            i = new Intent(context, MainActivity.class);
            context.startService(i);
        }

    }
}