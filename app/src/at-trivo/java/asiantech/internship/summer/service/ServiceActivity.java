package asiantech.internship.summer.service;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import asiantech.internship.summer.R;
import asiantech.internship.summer.service.model.Song;
import asiantech.internship.summer.service.music_recycler_view.SongAdapter;
import asiantech.internship.summer.service.music_recycler_view.SongInteractListener;

public class ServiceActivity extends AppCompatActivity implements SongInteractListener {
    private final int READ_EXTERNAL_PERMISSION_REQUEST_CODE = 3;

    private SongAdapter mAdapter;
    private List<Song> mSongs;

    private ImageButton mBttPlay;
    private TextView mTvSongTitle;
    private SeekBar mSeekBar;

    private MusicPlayer mMusicPlayer;
    private PlayMusicReceiver mReceiver;
    private Intent mPlayIntent;
    private boolean mIsBounding;
    private ServiceConnection mServiceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);
        initView();
        setUpRecyclerView();
        if (checkReadExternalPermission()) {
            setUpPlayMusicService();
        }
    }

    protected void onResume() {
        super.onResume();
        if (mReceiver == null) {
            mReceiver = new PlayMusicReceiver();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(PlayMusicService.ACTION_START_PLAY_SONG);
        filter.addAction(PlayMusicService.ACTION_UPDATE_TIME);
        registerReceiver(mReceiver, filter);
    }

    protected void onPause() {
        super.onPause();
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
    }

    private void initView() {
        mBttPlay = findViewById(R.id.bttPlay);
        mTvSongTitle = findViewById(R.id.tvSongTitle);
        mSeekBar = findViewById(R.id.seekBar);
        mBttPlay.setOnClickListener(view -> {
            if (mIsBounding) {
                if (!mMusicPlayer.isPausing()) {
                    mMusicPlayer.pause();
                    mBttPlay.setImageDrawable(getResources().getDrawable(R.drawable.ic_play));
                } else {
                    mMusicPlayer.play();
                    mBttPlay.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
                }
            }
        });
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mMusicPlayer.endNotifyTime();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mMusicPlayer.setSeekPosition(seekBar.getProgress());
            }
        });
        Button bttEnd = findViewById(R.id.bttEnd);
        bttEnd.setOnClickListener(view -> {
            Intent intent = new Intent(ServiceActivity.this, PlayMusicService.class);
            intent.setAction(PlayMusicService.ACTION_STOP_SERVICE);
            startService(intent);
        });
    }

    private void setUpRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        mSongs = new ArrayList<>();
        mAdapter = new SongAdapter(mSongs, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);
    }

    private void setUpPlayMusicService() {
        getSongs();
        initServiceConnection();
        if (mPlayIntent == null) {
            mPlayIntent = new Intent(this, PlayMusicService.class);
            mPlayIntent.setAction(PlayMusicService.ACTION_START_SERVICE);
            startService(mPlayIntent);
            bindService(mPlayIntent, mServiceConnection, BIND_AUTO_CREATE);
        }
    }

    private void getSongs() {
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            do {
                long id = cursor.getLong(idColumn);
                String title = cursor.getString(titleColumn);
                String artist = cursor.getString(artistColumn);
                mSongs.add(new Song(id, title, artist));
            } while (cursor.moveToNext());
            cursor.close();
        }
        mAdapter.notifyDataSetChanged();
    }

    private void initServiceConnection() {
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                PlayMusicService.MusicBinder binder = (PlayMusicService.MusicBinder) iBinder;
                mMusicPlayer = binder.getMusicPlayer();
                mMusicPlayer.init(mSongs);
                mMusicPlayer.play();
                mIsBounding = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mIsBounding = false;
            }
        };
    }

    private boolean checkReadExternalPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == READ_EXTERNAL_PERMISSION_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setUpPlayMusicService();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void songClicked(int position) {
        if (mIsBounding) {
            mMusicPlayer.playAt(position);
            mBttPlay.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
        }
    }

    private class PlayMusicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case PlayMusicService.ACTION_START_PLAY_SONG:
                    String title = intent.getStringExtra(PlayMusicService.SONG_TITLE);
                    int duration = intent.getIntExtra(PlayMusicService.SONG_DURATION, 0);
                    mTvSongTitle.setText(title);
                    mSeekBar.setMax(duration);
                    mSeekBar.setProgress(0);
                    break;
                case PlayMusicService.ACTION_UPDATE_TIME:
                    int time = intent.getIntExtra(PlayMusicService.CURRENT_TIME, 0);
                    mSeekBar.setProgress(time);
                    break;
            }
        }
    }
}
