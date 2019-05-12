package com.example.quizlocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log

/* 화면이 off 될 때 실행되는 ScreenOffReceiver를 동적으로 등록하는 역할 수행
    - 화면이 꺼졌을 때, 즉 액티비티가 종료되었을 때도 퀴즈잠김화면이 백엔드에서 계속 실행하도록 하기 위해
      서비스에서 브로드캐스트 리시버를 동적으로 등록함
    - 사용자에게 서비스가 동작 중임을 알리는 Notification 포그라운드 서비스 기능 수행
*/
class LockScreenService : Service() {

    // 화면이 꺼질때 브로드캐스트 메세지를 수신하는 리시버(ScreenOffReceiver) 변수 선언
    var receiver: ScreenOffReceiver? = null

    //id 상수 선언
    private val ANDROID_CHANNEL_ID = "kr.ac.quizlocker"
    private val NOTIFICATION_ID = 9999

    // 서비스가 최초 생성될때 호출되는 콜백 함수
    override fun onCreate() {
        super.onCreate()
        Log.d("quizlocker", "LockScreenService-onCreate 호출")

        /* 브로드캐스트 리시버가 null 인 경우 ScreenOffReceiver()를 생성하여,
           생성한 리시버를 서비스에서 동적으로 런타임에 등록
           - 오레오 버전부터는 브로드캐스트 수신이 제한되어, 암시적 인텐트가 허용되지 않는
             메시지(성능 및 밧데리에 영향을 주는 메시지)를 수신하기 위해서는
             앱 또는 서비스 실행(Runtime) 중에 브로드캐스트 리시버를 등록해야 수신할 수 있음
         */
        if (receiver == null) {

            //브로드캐스트 리시버(ScreenOffReceiver()) 객체 생성
            receiver = ScreenOffReceiver()

            //ACTION_SCREEN_OFF 메시지를 필터링할 인텐트 필터 생성
            val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)

            //브로드캐스트 리시버를 런타임에 등록(리시버 객체, 인텐트 필터)
            registerReceiver(receiver, filter)
        }
    }

    // 서비스를 호출하는 클라이언트가 startService() 함수를 호출할 때마다 호출되는 콜백 함수
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("quizlocker", "LockScreenService-onStartCommand 호출")

        // 브로드캐스트 리시버가 null 인 경우에만 실행
        if (intent != null) {
            if (intent.action == null) {
                /* 서비스가 최초 실행이 아닌 경우 onCreate 가 불리지 않을 수 있음.
                   이 경우 receiver 가 null이면 브로드캐스트 리시버(ScreenOffReceiver()) 객체를 새로 생성하고,
                   생성한 리시버를 서비스에서 동적으로 런타임에 등록
                */
                if (receiver == null) {
                    receiver = ScreenOffReceiver()
                    val filter = IntentFilter(
                        Intent.ACTION_SCREEN_OFF)
                    registerReceiver(receiver, filter)
                }
            }
        }

        /* 오레오 버전부터 백그라운드 제약이 있기 때문에 포그라운드 서비스를 실행해야함.
           - 사용자에게 서비스가 실행 중임을 알리기 위해 Notification 알림 객체를 생성하여
             Notification 알림과 함께 포그라운드 서비스 시작
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Notification(상단 알림) 채널 생성 및 설정
            val channel= NotificationChannel(ANDROID_CHANNEL_ID, "MyService", NotificationManager.IMPORTANCE_NONE)
            channel.lightColor = Color.BLUE
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

            // Notification 서비스 객체를 가져옴
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

            // Notification 알림 객체 생성
            val builder = Notification.Builder(this, ANDROID_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_search)
                .setContentTitle(getString(R.string.app_name)+ " 서비스")
                .setContentText("LockScreenService Running")
            val notification = builder.build()

            // Notification 알림과 함께 포그라운드 서비스 시작
            startForeground(NOTIFICATION_ID, notification)
        }

        /* 시스템이 서비스를 강제종료시키면, 서비스를 재생성하고 onStartCommand()메서드를 호출하도록 START_REDELIVER_INTENT 반환
          - 서비스가 강제 종료되기 전에 전달된 마지막 Intent도 다시 전달해 줌
        */
        return Service.START_REDELIVER_INTENT
    }

    //서비스 종료시 호출되는 콜백 함수
    override fun onDestroy() {
        Log.d("quizlocker", "LockScreenService-onDestroy 호출")
        super.onDestroy()

        // 서비스가 종료될때 브로드캐스트 리시버 등록도 해제
        if (receiver != null) {
            unregisterReceiver(receiver)
        }
    }

    /* BindService 콜백 함수
       - 서비스와 액티비티 사이에서 데이터를 주고 받을 때 사용
     */
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
