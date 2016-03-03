package test.y3k.testvideocaching;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.VideoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    VideoView videoView;
    File videoCache;

    final static String URL = "http://www.cmoremap.com.tw/askey_adv/video_10.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        videoView = (VideoView)findViewById(R.id.video);
        videoView.setOnErrorListener(onErrorListener);
        try {
            videoCache = File.createTempFile("video_cache","mp4");
            videoCache.deleteOnExit();
            new Thread(){
                @Override
                public void run() {
                    try {
                        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(URL).openConnection();
                        httpURLConnection.setDoInput(true);
                        httpURLConnection.getResponseCode();
                        InputStream inputStream = httpURLConnection.getInputStream();
                        byte[] buffer = new byte[1024*1024];
                        int readCount;
                        FileOutputStream fileOutputStream = new FileOutputStream(videoCache);
                        while((readCount=inputStream.read(buffer))>0){
                            fileOutputStream.write(buffer,0,readCount);
                            handler.sendEmptyMessage(readCount);
                        }
                        fileOutputStream.close();
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    MediaPlayer.OnErrorListener onErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
            Log.d("video", "mediaPlayer.getCurrentPosition()="+mediaPlayer.getCurrentPosition());
            try {
                mediaPlayer.setDataSource(videoCache.getPath());
                mediaPlayer.prepare();
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.start();
                    }
                });
            } catch (IOException e){
                e.printStackTrace();
            }
            return true;
        }
    };

    Handler handler = new Handler(new Handler.Callback(){
        int cachedSize = 0;
        boolean inited = false;
        @Override
        public boolean handleMessage(Message msg) {
            this.cachedSize += msg.what;
            Log.d("video", "cached Size = "+this.cachedSize);
            if(!inited&&cachedSize>(2*1024*1024)) {
                videoView.setVideoPath(videoCache.getPath());
                videoView.start();
                inited = true;
            }
            else if(inited){
                Log.d("video", "videoView Buffered Percantage = "+videoView.getBufferPercentage());
            }
            return true;
        }
    });
}
