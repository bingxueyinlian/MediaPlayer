package cn.ac.ict.mediaplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;

import cn.ac.ict.mediaplayer.MediaPlayerService.MyBinder;

public class MainActivity extends ActionBarActivity {
    private final String TAG = "MainActivity";
    private ListView listView1;
    private SeekBar seekBar1;
    private Button buttonMiddle;
    private Button buttonPrevOne;
    private Button buttonNextOne;

    private String musicDirectory;
    private String[] musicNames;

    private int curPosition = -1;// cur playing position

    private SeekBarThread seekBarThread;// for change seekBar
    private Handler seekBarHandler;// for change seekBar
    private Handler uiHandler;// when music is completed,set ui to change
    private MyBinder myBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // findViewById
        initUIControl();

        // get music list
        initMusicList();

        // bond XML to java
        // load the music list
        initListView();

        initHandler();
        // init ThreadSeekBar
        seekBarThread = new SeekBarThread();

        initSeekBar();

        // bing service
        connectService();
    }

    private void connectService() {
        Intent service = new Intent(MainActivity.this, MediaPlayerService.class);
        startService(service);
        ServiceConnection sc = new MyServiceConnection();
        bindService(service, sc, BIND_AUTO_CREATE);
    }

    private void initSeekBar() {

        seekBar1.setIndeterminate(false);
        seekBar1.setProgress(0);
        // set selected item attri
        seekBar1.setOnSeekBarChangeListener(new SeekBarChangeListener());
    }

    private void initHandler() {
        uiHandler = new UIHandler();
        seekBarHandler = new Handler();
    }

    private void initListView() {
        ArrayAdapter<String> adapter = new MyArrayAdapter<>(this, R.layout.textview, musicNames);
        listView1.setAdapter(adapter);
        listView1.setOnItemClickListener(new ListViewItemClickListener());
    }

    private void initMusicList() {
        String sdCardRoot = Environment.getExternalStorageDirectory().getAbsolutePath();
        musicDirectory = sdCardRoot + File.separator + "Music" + File.separator;
        File file = new File(musicDirectory);
        musicNames = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".mp3");
            }
        });
    }

    private void initUIControl() {
        listView1 = (ListView) findViewById(R.id.listView1);
        buttonMiddle = (Button) findViewById(R.id.buttonMiddle);
        buttonPrevOne = (Button) findViewById(R.id.buttonPrevOne);
        buttonNextOne = (Button) findViewById(R.id.buttonNextOne);
        seekBar1 = (SeekBar) findViewById(R.id.seekBar1);
    }

    private void playSelectedMusic() {
        scrollToVisiblePosition();
        // play new music
        Log.i(TAG, curPosition + "");
        int index = getCurPositionInListView();
        Log.i(TAG, "index:" + index);
        View view = listView1.getChildAt(index);
        view.setBackgroundColor(Color.BLUE);
        String name = musicNames[curPosition];
        File file = new File(musicDirectory, name);
        Uri uri = Uri.fromFile(file);
        myBinder.playMusic(uri);
        seekBar1.setProgress(0);
        seekBar1.setMax(myBinder.getDuration());
        startSeekBarThread();
        // change button text
        String pause = getResources().getString(R.string.pause);
        if (!buttonMiddle.getText().equals(pause)) {
            buttonMiddle.setText(pause);
        }
    }

    private void scrollToVisiblePosition() {
        int lastIndex = listView1.getLastVisiblePosition();
        int firstIndex = listView1.getFirstVisiblePosition();
        Log.i(TAG, "curPosition:" + curPosition + ",lastIndex:" + lastIndex + ",firstIndex:" + firstIndex);
        if (curPosition >= lastIndex) {
            listView1.setSelection(lastIndex);
        } else if (curPosition <= firstIndex) {
            listView1.setSelection(firstIndex);
        }
    }

    private void pauseSelectedMusic() {
        myBinder.pauseMusic();
        stopSeekBarThread();
        // change button text
        String play = getResources().getString(R.string.play);
        if (!buttonMiddle.getText().equals(play)) {
            buttonMiddle.setText(play);
        }
    }

    public void OnClickEvent(View button) {
        if (button.getId() == buttonMiddle.getId()) {// buttonMiddle
            buttonMiddleClick();
        } else if (button.getId() == buttonPrevOne.getId()) {// buttonPrevOne
            playPrevMusic();
        } else if (button.getId() == buttonNextOne.getId()) {// buttonNextOne
            playNextMusic();
        }

    }

    private void buttonMiddleClick() {

        if (myBinder.isPlaying()) {
            // a music is playing,pause the music
            pauseSelectedMusic();
        } else {
            // no music is playing,start a music
            if (curPosition == -1) {
                // no music is selected, start 1st music
                curPosition = 0;
                playSelectedMusic();
            } else {
                // continue the same music
                myBinder.startMusic();
                startSeekBarThread();
            }
        }
    }

    private void playPrevMusic() {
        clearCurPositionBackgroundColor();
        if (curPosition == 0) {
            // if position is the first,go to the last
            curPosition = musicNames.length - 1;
            //listView1.scrollTo(0, 0);
        } else {
            curPosition--;
        }
        // a music is playing
        playSelectedMusic();
    }

    private void playNextMusic() {
        if (curPosition == musicNames.length - 1) {
            // if position is the last,go to the first
            //curPosition = 0;
            Toast.makeText(MainActivity.this, R.string.no_music, Toast.LENGTH_SHORT).show();
            return;
        }
        clearCurPositionBackgroundColor();
        curPosition++;
        // a music is playing
        playSelectedMusic();
    }

    private void clearCurPositionBackgroundColor() {
        int index = getCurPositionInListView();
        int allCount = listView1.getChildCount();
        if (index >= 0 && index < allCount) {
            listView1.getChildAt(index).setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private int getCurPositionInListView() {
        int position = listView1.getFirstVisiblePosition();
        return curPosition - position;
    }

    class MyServiceConnection implements ServiceConnection {

        // boolean flagIsPlaying;
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBinder = (MyBinder) service;
            myBinder.sendHandler(uiHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

    }

    private void stopSeekBarThread() {
        if (seekBarThread != null) {
            // stop SeekBarThread
            seekBarHandler.removeCallbacks(seekBarThread);
        }
    }

    private void startSeekBarThread() {
        if (seekBarThread != null) {
            // start SeekBarThread
            seekBarHandler.post(seekBarThread);
        }
    }

    class UIHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            // the music is completed
            Log.i(TAG, "UIHandler-->handleMessage");

            stopSeekBarThread();

            playNextMusic();
        }
    }

    class SeekBarThread implements Runnable {
        @Override
        public void run() {
            seekBar1.setProgress(myBinder.getCurrentPosition());
            seekBarHandler.postDelayed(seekBarThread, 100);
        }
    }

    class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            if (fromUser) {
                myBinder.seekTo(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }

    class ListViewItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {

            if (myBinder.isPlaying()) {
                // toutch the same item with playing position,do nothing
                if (curPosition == position) {
                    return;
                }
            }

            // no music is playing
            // play the selected music

            // clear old background
            clearCurPositionBackgroundColor();

            // change button text
            String pause = getResources().getString(R.string.pause);
            buttonMiddle.setText(pause);

            curPosition = position;// set new position
            playSelectedMusic();
        }
    }

    class MyArrayAdapter<T> extends ArrayAdapter<T> {

        public MyArrayAdapter(Context context, int resource, T[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            if (position == curPosition) {
                view.setBackgroundColor(Color.BLUE);
            } else {
                view.setBackgroundColor(Color.TRANSPARENT);
            }
            return view;
        }

    }

}
