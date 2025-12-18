package com.wzy.testunity

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.topjohnwu.superuser.Shell


class MainActivity : AppCompatActivity() {

    private val ID_FILE_PATH = "/storage/emulated/0/Download/auto/tmp/data"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_2).setOnClickListener {
//            val result = Shell.cmd("cat $ID_FILE_PATH").exec()
            val result = Shell.cmd("dumpsys activity top | grep -E 'ACTIVITY'").exec()
            YLLogger.e(
                "code=${result.code}\noutput--->\n${result.out.joinToString("\n")}\n-----------------\nerror--->\n${
                    result.err.joinToString(
                        "\n"
                    )
                }\n----------"
            )
        }
    }

}
