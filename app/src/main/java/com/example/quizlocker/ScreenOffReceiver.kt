package com.example.quizlocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/* 화면이 off 될 때 브로드캐스트 메시지를 수신하는 리시버
   - 화면이 off 되는 브로드캐스트 메시지(ACTION_SCREEN_OFF)를 받으면,
     퀴즈잠김 화면(QuizLockerActivity)을 실행시키는 역할 수행
*/
class ScreenOffReceiver : BroadcastReceiver() {

    /* 화면이 off 될 때 퀴즈잠김 화면(QuizLockerActivity)을 실행
       - 이미 QuizLockerActivity가 실행중인 경우라면 퀴즈의 문제 내용을 바꾸기 위해
         FLAG_ACTIVITY_NEW_TASK, FLAG_ACTIVITY_CLEAR_TOP을 FLAG 옵션 값으로 인텐트에 추가
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        when {
            //수신한 브로드캐스트 메시지가 ACTION_SCREEN_OFF 이면
            intent?.action == Intent.ACTION_SCREEN_OFF -> {
                Log.d("quizlocker", "ScreenOffReceiver-onReceive 호출")

                // 새로운 액티비티(QuizLockerActivity) 생성
                val intent = Intent(context, QuizLockerActivity::class.java)

                // 새로운 태스크(TASK)를 생성하여, 그 태스크 안에  액티비티 추가하는 FLAG 설정
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                /* 기존의 액티비티를 스택에서 제거
                   - 스택에 호출하려는 액티비티가 이미 존재하면, 새로 만들지 않고 존재하는 액티비티를
                     포그라운드로 가져오고, 그 위에 존재하는 기존의 모든 액티비티를 삭제함
                 */
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

                //액티비티(QuizLockerActivity) 시작
                context?.startActivity(intent)
            }
        }
    }
}
