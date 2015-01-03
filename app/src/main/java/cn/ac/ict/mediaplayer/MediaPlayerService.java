package cn.ac.ict.mediaplayer;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class MediaPlayerService extends Service {

    private String TAG = "MediaPlayerService";
    private MediaPlayer mediaPlayer = null;
    private Handler uiHandler = null;

    @Override
    public IBinder onBind(Intent intent) {
        MyBinder myBinder = new MyBinder();
        myBinder.createMediaPlayer();
        return myBinder;

    }

    class MyBinder extends Binder {

        public void sendHandler(Handler handler) {
            uiHandler = handler;
        }

        public void createMediaPlayer() {
            // create a media player
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(new MusicCompletionListener());
            mediaPlayer.setLooping(false);
        }

        public boolean isPlaying() {
            return mediaPlayer.isPlaying();
        }

        public int getDuration() {
            return mediaPlayer.getDuration();
        }

        public int getCurrentPosition() {
            return mediaPlayer.getCurrentPosition();
        }

        public void seekTo(int progress) {
            mediaPlayer.seekTo(progress);
        }

        //   public void stopMusic() {
        //      mediaPlayer.stop();
        //      mediaPlayer.reset();
        //  }

        public void pauseMusic() {
            mediaPlayer.pause();
        }

        public void playMusic(Uri uri) {
            // play a music, from initial state to start
            try {

                mediaPlayer.reset();
                mediaPlayer.setDataSource(MediaPlayerService.this, uri);
                mediaPlayer.prepare();
                startMusic();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void startMusic() {
            mediaPlayer.start();
        }

    }

    class MusicCompletionListener implements MediaPlayer.OnCompletionListener {

        @Override
        public void onCompletion(MediaPlayer mp) {
            // the music is completed,send message to ui
            uiHandler.sendEmptyMessage(1);
            Log.i(TAG, "MusicCompletionListener");
        }
    }
}
