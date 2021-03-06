package com.orangeline.foregroundstudy;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;


public class SetAlarm extends Service {

    String tag = "SetAlarm";
    String UserID = "123";

    public SetAlarm() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(tag, "onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(tag, "onStartCommand()");

        String addroom = intent.getStringExtra("addkey");
        String delroom = intent.getStringExtra("delkey");

        Log.d(tag, "전달받은 데이터\naddroom: " + addroom + " delroom: " + delroom);

        if (addroom != null){
            Log.d(tag, "not null");

            getMeeting(addroom);
        }
        else if(delroom != null){
            delAlarm(Integer.parseInt(delroom));
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
        Log.d("SetAlarm", "onDestroy()");
    }

    private void getMeeting(final String roomid){              // getMeeting이랑 getTime은 child리스너가 더 좋을거 같은데 Meeting meeting = snapshot.getValue(Meeting.class) 이런 식으로 하면 에러
        Log.d(tag, "in getMeeting()");
        Log.d(tag, roomid);

        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        DatabaseReference roomdate = mDatabase.getReference("users").child(UserID).child("room").child(roomid).child("arrtime");

        roomdate.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String meeting = snapshot.getValue().toString();
                LocalDateTime meettime = LocalDateTime.parse(meeting, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));    // T가 없을 때

                Log.e(tag, "changed!!!: " + meettime);
                System.out.println(meettime.getClass().getName());
                addAlarm(Integer.parseInt(roomid), meettime);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void addAlarm(int reqcode, LocalDateTime t){     // 알람 추가
        Log.d(tag, "addAlarm()");
        System.out.println(t.getHour() + "시 "+ t.getMinute() + "분" + t.getYear() + "년" + t.getMonthValue() + "월" + t.getDayOfMonth()+"일");
        System.out.println(t);
        System.out.println(reqcode);

        if (t.isAfter(LocalDateTime.now())) {
            Log.d(tag, "알람 추가하자!");
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            Calendar calendar = Calendar.getInstance();

            Intent intent = new Intent(this, AlarmReceiver.class);
            //Intent intent = new Intent("com.orangeline.foregroundstudy.ALARM_START");
            PendingIntent pIntent = PendingIntent.getBroadcast(this, reqcode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            if (pIntent != null && alarmManager != null) {
                alarmManager.cancel(pIntent);
            }

            calendar.set(Calendar.YEAR, t.getYear());
            calendar.set(Calendar.MONTH, t.getMonthValue());      // n월 이면 int형으론 n-1 // 즉 8월이면 int형으론 7
            calendar.set(Calendar.DATE, t.getDayOfMonth());
            calendar.set(Calendar.HOUR_OF_DAY, t.getHour());
            calendar.set(Calendar.MINUTE, t.getMinute());
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pIntent);
            System.out.println("추가완료");
        }
    }

    private void delAlarm(int reqcode){     // 알람 삭제
        Log.d(tag, "delAlarm()");

        AlarmManager alarmManager = (AlarmManager)getApplicationContext().getSystemService(ALARM_SERVICE);

        //Intent intent = new Intent("com.orangeline.foregroundstudy.ALARM_START");
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pIntent = PendingIntent.getBroadcast(this, reqcode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (pIntent != null){
            alarmManager.cancel(pIntent);
            pIntent.cancel();
        }
    }

}
