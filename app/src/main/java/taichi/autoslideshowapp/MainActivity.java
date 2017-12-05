package taichi.autoslideshowapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
//View
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
//Timer,ハンドラ
import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;
//ライブラリ接続
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
//画像取得
import android.content.ContentResolver;
import android.content.ContentUris;
import android.provider.MediaStore;
import android.database.Cursor;
import android.net.Uri;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    Boolean isPlaying = false;

    Button playButton;
    Button prevButton;
    Button nextButton;
    ImageView imageView;

    Timer timer;
    Handler handler = new Handler();

    Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);

        playButton = findViewById(R.id.playButton);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);

        playButton.setOnClickListener(this);
        prevButton.setOnClickListener(this);
        nextButton.setOnClickListener(this);

        confirmExternalStoragePermission();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.playButton) {
            prevButton.setEnabled(isPlaying);
            nextButton.setEnabled(isPlaying);
            playButton.setText(isPlaying ? "START" : "STOP");
            toggleTimer();
            isPlaying ^= true;
        } else if (v.getId() == R.id.prevButton) {
            incrementImage(cursor);
        } else if (v.getId() == R.id.nextButton) {
            decrementImage(cursor);
        } else {
            Log.d("UI_PARTS", "エラー！");
        }
    }

    private void toggleTimer() {
        if(isPlaying) {
            // ストップ
            if (timer == null) { return; }
            timer.cancel();
        } else {
            // スタート
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            incrementImage(cursor);
                        }
                    });
                }
            }, 100, 2000);
        }
    }

    private void incrementImage(Cursor cursor) {
        if (cursor == null) { return; }
        if (cursor.moveToNext()) {
            setImage(cursor);
        } else {
            cursor.moveToFirst();
            setImage(cursor);
        }
    }

    private void decrementImage(Cursor cursor) {
        if (cursor == null) { return; }
        if (cursor.moveToPrevious()) {
            setImage(cursor);
        } else {
            cursor.moveToLast();
            setImage(cursor);
        }
    }

    private void setImage(Cursor cursor) {
        // indexからIDを取得し、そのIDから画像のURIを取得する
        int fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
        Long id = cursor.getLong(fieldIndex);
        Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

        Log.d("ANDROID", "URI : " + imageUri.toString());
        imageView.setImageURI(imageUri);
    }

    private void getContentsInfo() {

        // 画像の情報を取得する
        ContentResolver resolver = getContentResolver();
        cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
                null, // 項目(null = 全項目)
                null, // フィルタ条件(null = フィルタなし)
                null, // フィルタ用パラメータ
                null // ソート (null ソートなし)
        );

        if (cursor.moveToFirst()) {
            setImage(cursor);
        }
    }

    private void confirmExternalStoragePermission() {
        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                getContentsInfo();
            } else {
                // 許可されていないので許可ダイアログを表示する
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
            }
            // Android 5系以下の場合
        } else {
            getContentsInfo();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length == 0) { return; }
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getContentsInfo();
        } else {
            Toast.makeText(this, "権限なし", Toast.LENGTH_SHORT).show();
        }
    }
}
