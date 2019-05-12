package com.example.quizlocker

import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v14.preference.MultiSelectListPreference
import android.support.v14.preference.SwitchPreference
import android.support.v7.preference.PreferenceFragmentCompat
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

/* 퀴즈 잠김화면 앱 메인 액티비티
   - 스마트폰의 "잠금" 버튼을 눌러 화면이 꺼졌을 때 "퀴즈 화면"이 나타나도록 구현
   - 환경설정 화면에서 "퀴즈 잠김화면 사용" 스위치가 "On"일 때 서비스 실행
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("quizlocker", "MainActivity-onCreate 호출")

        /* 앱의 환경설정 정보를 액티비티에 설정
           - 사용자에게 설정된 환경정보(MyPreferenceFragment)를 보여주기 위해 액티비티에 replace
           - 메인 액티비티(activity_main.xml)의 preferenceContent 뷰(FrameLayout) 영역을
             환경설정 화면인 MyPreferenceFragment()로 교체
        */
        supportFragmentManager.beginTransaction().replace(R.id.preferenceContent, MyPreferenceFragment()).commit()

        //초기화버튼(initButton)이 클릭되면 initAnswerCount() 함수를 호출하여 환경설정 정보를 초기화
        initButton.setOnClickListener {
            initAnswerCount()
        }
    }

    /* 앱의 환경설정을 위한 PreferenceFragment 클래스 선언
       - Preference XML 리소스인 pref.xml을 PreferenceFragment로서 화면에 보여주고,
         사용자가 "퀴즈 잠김화면 사용" 스위치를 On으로 설정한 경우, 퀴즈 잠김화면 서비스를 시작하는 기능 수행
     */
    class MyPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(p0: Bundle?, p1: String?) {

            // 환경설정을 위한 리소스 파일(pref.xml) 적용 - Preference XML 리소스 적용
            addPreferencesFromResource(R.xml.pref)

            /* =============  퀴즈 잠금화면 사용 스위치 preference 처리 로직 =============== */

            /*  퀴즈 잠금화면 사용 스위치 정보(퀴즈 잠금화면 사용 스위치, key:useLockScreen)를 가져와서
                SwitchPreference로 형을 변환하여 useLockScreenPref에 저장
               - import android.support.v14.preference.SwitchPreference
            */
            val useLockScreenPref = findPreference("useLockScreen") as SwitchPreference

            // 퀴즈 잠금화면 사용 스위치 preference에 클릭 이벤트처리를 위한 리스너 설정
            useLockScreenPref.setOnPreferenceClickListener {
                when {
                    /* 퀴즈 잠금화면 사용이 체크(on)된 경우 LockScreenService 실행
                       - 오레오 버전부터는 포그라운드 서비스를 위해 startForegroundService로 서비스 시작하고,
                         아니면, startService로 서비스 시작
                     */
                    useLockScreenPref.isChecked -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            requireActivity().startForegroundService(Intent(requireActivity(), LockScreenService::class.java))
                        } else {
                            requireActivity().startService(Intent(requireActivity(), LockScreenService::class.java))
                        }
                    }
                    // 퀴즈 잠금화면 사용이 체크 해제(off)된 경우 LockScreenService 서비스 중단
                    else -> requireActivity().stopService(Intent(requireActivity(), LockScreenService::class.java))
                }
                true
            }

            /* 앱이 시작 되었을때 이미 퀴즈잠금화면 사용이 체크되어있으면  LockScreenService 서비스 실행
               - 오레오 버전부터는 포그라운드 서비스를 위해 startForegroundService로 서비스 시작하고,
                 아니면, startService로 서비스 시작
            */
            if (useLockScreenPref.isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireActivity().startForegroundService(Intent(requireActivity(), LockScreenService::class.java))
                } else {
                    requireActivity().startService(Intent(requireActivity(), LockScreenService::class.java))
                }
            }

            /* =============  퀴즈종류 선택 preference 처리 로직 =============== */

            /* 저장된 환경 설정 정보(퀴즈종류, key:category)를 가져와, 그 내용들을 환경설정의 summary(요약정보)에 설정
               - 퀴즈종류의 경우 MultiSelectListPreference 태그로 선언되어 있기 때문에,
                 사용자가 태그를 클릭하기 전까지는 현재 선택된 퀴즈가 어떤 것인지를 알 수 없음
                 따라서 퀴즈정보를 요약정보(summary)에 보여주도록 설정함
               - findPreference(): key로 Preference 찾는 메서드
               - import android.support.v14.preference.MultiSelectListPreference
            */
            val categoryPref = findPreference("category") as MultiSelectListPreference
            categoryPref.summary = categoryPref.values.joinToString(", ")

            /* 사용자가 환경설정 정보에서 퀴즈종류를 변경하면 동작하는 이벤트 리스너 등록
               - 사용자가 퀴즈종류를 변경할 때마다 요약정보(summary)를 변경해준다
               - Preference도 일반 뷰처럼 이벤트 처리를 할 수 있음
             */
            categoryPref.setOnPreferenceChangeListener { preference, newValue ->
                Log.d("quizlocker", "setOnPreferenceChangeListener : ${newValue.toString()}")

                /* 요약정보(summary)에서 퀴즈종류를 변경하면 "On"으로 설정된 아이템만 newValue 매개변수를 통해 전달됨
                   - 전달된 newValue값을 HashSet(순서가 없고, data 중복을 제거하는 집합저장공간)컬렉션으로 캐스팅하여,
                     null이 아니면 newValueSet에 할당하고, null이면 return
                 */
                val newValueSet = newValue as? HashSet<*> ?: return@setOnPreferenceChangeListener true

                // 선택된 퀴즈종류를 요약정보(summary)로 보여줌
                categoryPref.summary = newValue.joinToString(", ")
                true
            }
        }
    }

    //정답, 오답 설정 정보를 초기화
    fun initAnswerCount() {
        //정답횟수, 오답횟수 설정 정보를 가져온다.
        val correctAnswerPref = getSharedPreferences("correctAnswer", Context.MODE_PRIVATE)
        val wrongAnswerPref = getSharedPreferences("wrongAnswer", Context.MODE_PRIVATE)

        //정/오답 SharedPreference 파일 초기화
        correctAnswerPref.edit().clear().apply()
        wrongAnswerPref.edit().clear().apply()
    }
}
