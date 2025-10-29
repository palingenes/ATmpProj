package com.wzy.testunity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.SnapshotsClient.DataOrConflict
import com.google.android.gms.games.snapshot.Snapshot
import com.google.android.gms.games.snapshot.SnapshotMetadata
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import java.io.IOException
import java.math.BigInteger
import java.util.Random


class MainActivity : AppCompatActivity() {


    val RC_SAVED_GAMES: Int = 9009

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_2).setOnClickListener {

            showSavedGamesUI()
        }
    }

    private fun showSavedGamesUI() {
        val snapshotsClient =
            PlayGames.getSnapshotsClient(this)
        val maxNumberOfSavedGamesToShow = 5

        val intentTask = snapshotsClient.getSelectSnapshotIntent(
            "See My Saves", true, true, maxNumberOfSavedGamesToShow
        )

        intentTask.addOnSuccessListener(OnSuccessListener { intent ->
            if (intent != null) {
                startActivityForResult(intent, RC_SAVED_GAMES)
            }
            YLLogger.e(intent.toString())
        })
        intentTask.addOnFailureListener {
            YLLogger.e("失败：\n${it}")
        }
    }

    private var mCurrentSaveName = "snapshotTemp"


    override fun onActivityResult(
        requestCode: Int, resultCode: Int,
        intent: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (intent != null) {
            if (intent.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA)) {
                // Load a snapshot.
                val snapshotMetadata =
                    intent.getParcelableExtra<SnapshotMetadata?>(SnapshotsClient.EXTRA_SNAPSHOT_METADATA)
                mCurrentSaveName = snapshotMetadata!!.uniqueName

                // Load the game data from the Snapshot
                // ...
            } else if (intent.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_NEW)) {
                // Create a new snapshot named with a unique string
                val unique = BigInteger(281, Random()).toString(13)
                mCurrentSaveName = "snapshotTemp-$unique"

                // Create the new snapshot
                // ...
            }
        }
    }

}
