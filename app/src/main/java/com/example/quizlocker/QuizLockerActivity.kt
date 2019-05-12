package com.example.quizlocker

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.WindowManager
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_quiz_locker.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

/* 퀴즈 잠김 화면 액티비티
   - 스마트폰의 "잠김" 버튼을 눌러 화면이 꺼졌을 때 "퀴즈 화면"이 나타나도록 구현
   - 환경설정 화면에서 "퀴즈 잠김화면 사용" 스위치가 "On"일 때 서비스 실행
 */
class QuizLockerActivity : AppCompatActivity() {

    var quiz: JSONObject? = null

    // 오답횟수 저장 SharedPreference
    val wrongAnswerPref by lazy { getSharedPreferences("wrongAnswer", Context.MODE_PRIVATE) }
    // 정답횟수 저장 SharedPreference
    val correctAnswerPref by lazy { getSharedPreferences("correctAnswer", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("quizlocker", "QuizLockerActivity-onCreate 호출")

        /* =========== 퀴즈 화면을 어떻게 표시할 것인지를 설정 =============== */

        /* 화면이 꺼진 상태에서 Activity를 호출할 때, 화면에 어떻게 표시할지를 설정
           - FLAG_SHOW_WHEN_LOCKED: FLAG_KEEP_SCREEN_ON 플래그와 함께 사용해서
             Lock 걸려 있어도 그 위에 직접적으로 창을 보여주고 싶을때 사용
             (FLAG_DISMISS_KEYGUARD 플래그와 함께 사용하면 KEYGUARD의 보호를 자동으로 없앨수 있음)

           - 오레오 8.1부터는 변경되어 FLAG 대신에 setShowWhenLocked(true) 사용
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // 스마트폰에 Lock 걸려 있어도, 그 위에 Activity를 보여 주도록 설정
            setShowWhenLocked(true)
            // 잠금 해제 설정
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            // 스마트폰에 Lock 걸려 있어도, 그 위에 Activity를 보여 주도록 FLAG 설정
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            // 기본 잠금화면을 해제(KEYGUARD 보호 해제)
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }

        // 화면을 켜진 상태로 유지하도록 FLAG 설정
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_quiz_locker)

        /* ============ 퀴즈 문제 처리 ==================== */

        // 퀴즈 데이터(capital.json)를 assets 폴더에서 읽어와서 quizArray 객체를 생성
        val json = assets.open("capital.json").reader().readText()
        val quizArray = JSONArray(json)

        // quizArray 객체에 있는 문제 중에서 랜덤하게 퀴즈를 선택한다.
        quiz = quizArray.getJSONObject(Random().nextInt(quizArray.length()))

        /* 퀴즈화면 XML 레이아웃(activity_quiz_locker.xml)에 질문과 보기에 값을 설정하여 보여줌
           - quizLabel 뷰: 퀴즈의 질문(question)을 설정
           - choice1, choice2 뷰: 답안 보기(choice1, choice2)를 설정
         */
        quizLabel.text = quiz?.getString("question")
        choice1.text = quiz?.getString("choice1")
        choice2.text = quiz?.getString("choice2")

        // 퀴즈 문항(id)별로 정답횟수 오답횟수를 보여준다.
        val id = quiz?.getInt("id").toString() ?: ""
        correctCountLabel.text = "정답횟수:${correctAnswerPref.getInt(id, 0)}"
        wrongCountLabel.text = "오답횟수: ${wrongAnswerPref.getInt(id, 0)}"


        /* ============ SeekBar로 퀴즈에 응답하는 기능 처리 ==================== */

        // SeekBar 의 값이 변경될때 불리는 리스너
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            // progress 값이 변경될때 호출되는 콜백 함수
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                when {
                    // SeekBar 의 우측 끝으로 가면 choice2 를 선택한 것으로 간주한다
                    progress > 95 -> {
                        leftImageView.setImageResource(R.drawable.padlock)
                        // 우측 이미지뷰의 자물쇠 아이콘을 열림 아이콘으로 변경
                        rightImageView.setImageResource(R.drawable.unlock)
                    }
                    // SeekBar 의 좌측 끝으로 가면 choice1 을 선택한 것으로 간주한다
                    progress < 5 -> {
                        // 좌측 이미지뷰의 자물쇠 아이콘을 열림 아이콘으로 변경
                        leftImageView.setImageResource(R.drawable.unlock)
                        rightImageView.setImageResource(R.drawable.padlock)
                    }
                    // 양쪽 끝이 아닌 경우
                    else -> {
                        // 양쪽 이미지를 모두 잠금 아이콘으로 변경
                        leftImageView.setImageResource(R.drawable.padlock)
                        rightImageView.setImageResource(R.drawable.padlock)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {  }

            // 정답을 선택하는 터치 조작을 끝낸 경우 호출되는 콜백 함수
            override fun onStopTrackingTouch(seekBar: SeekBar?) {

                val progress = seekBar?.progress ?: 50

                when {
                    // 우측 끝의 답을 선택한 경우
                    progress > 95 -> checkChoice(quiz?.getString("choice2") ?: "")
                    // 좌측 끝의 답을 선택한 경우
                    progress < 5 -> checkChoice(quiz?.getString("choice1") ?: "")
                    // 양끝이 아닌 경우 seekBar 의 progress 를 중앙값으로 초기화
                    else -> seekBar?.progress = 50
                }
            }
        })
    }

    /* 정답 체크 함수
       - 매니페스트 파일에 VIBRATOR_SERVICE 퍼미션을 요청하지 않으면 오류발생
     */
    fun checkChoice(choice: String) {
        quiz?.let {
            when {
                // choice 의 텍스트가 정답 텍스트와 같으면 Activity 종료
                choice == it.getString("answer") -> {
                    // 정답인 경우 정답횟수 증가
                    val id = it.getInt("id").toString()
                    var count = correctAnswerPref.getInt(id, 0)
                    count++
                    //정답횟수를 정답 SharedPreference에 저장
                    correctAnswerPref.edit().putInt(id, count).apply()
                    correctCountLabel.text = "정답횟수: ${count}"
                    // Activity 종료
                    finish()
                }
                else -> {
                    // 오답 횟수 증가
                    val id = it.getInt("id").toString()
                    var count = wrongAnswerPref.getInt(id, 0)
                    count++
                    //오답횟수를 오답 SharedPreference에 저장
                    wrongAnswerPref.edit().putInt(id, count).apply()
                    wrongCountLabel.text = "오답횟수: ${count}"

                    // 정답이 아닌경우 UI 초기화
                    leftImageView.setImageResource(R.drawable.padlock)
                    rightImageView.setImageResource(R.drawable.padlock)
                    seekBar?.progress = 50

                    // 정답이 아닌 경우 진동알림 추가
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

                    // SDK 버전에 따라 호출
                    if (Build.VERSION.SDK_INT >= 26) {
                        // 1초동안 100의 세기(최고 255) 로 1회 진동
                        vibrator.vibrate(VibrationEffect.createOneShot(1000, 100))
                    } else {
                        // 1초동안 진동
                        vibrator.vibrate(1000)
                    }
                }
            }
        }
    }
}
