package com.example.quizlocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log

/* 부팅이 완료되었을 때 브로드캐스트 메시지를 수신하는 리시버 */
class BootCompleteReceiver : BroadcastReceiver() {

    /* 부팅 완료 브로드캐스트 메세지(ACTION_BOOT_COMPLETED)가 수신되면 자동 호출되는 콜백 함수
       - 퀴즈잠금화면 설정이 true 이면 단말기의 OS 버전을 체크하여 서비스(LockScreenService)를 START 시키는 기능 구현
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        when {
            // 부팅이 완료될 때의 메세지(ACTION_BOOT_COMPLETED) 인지 확인
            intent?.action == Intent.ACTION_BOOT_COMPLETED -> {
                Log.d("quizlocker", "부팅이 완료됨")

                context?.let {
                    /* SharedPreferences 객체를 가져와서 퀴즈잠금화면 설정값이 ON 인지 확인
                       - 환경 설정화면 정보(UI변경 정보)를 저장하는 preference(xml/pref.xml)에서
                         퀴즈잠금화면 설정값(key: useLockScreen)을 가져온다.
                    */
                    val pref = PreferenceManager.getDefaultSharedPreferences(context)
                    val useLockScreen = pref.getBoolean("useLockScreen", false)

                    /* 퀴즈잠금화면 설정이 true이면 단말기의 OS 버전을 체크하여 서비스(LockScreenService)를 START 시킴
                      - 오레오 버전부터는 백그라운드 서비스 제한이 걸려있기 때문에 포그라운드 서비스 실행
                        (startForegroundService(LockScreenService))
                      - 오레오 이전버전은 startService(LockScreenService)
                    */
                    if (useLockScreen) {
                        // LockScreenService 시작
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            //서비스 실행을 사용자에게 노티피케이션하도록 포그라운드 서비스 실행
                            Log.d("quizlocker", "BootCompleteReceiver-onReceive 호출(startForegroundService)")
                            it.startForegroundService(Intent(context, LockScreenService::class.java))

                        } else {
                            Log.d("quizlocker", "BootCompleteReceiver-onReceive 호출(startService)")
                            it.startService(Intent(context, LockScreenService::class.java))
                        }
                    }
                }
            }
        }
    }
}
