package com.test.exoplayer2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.EnvironmentCompat;

import android.os.Environment;
import android.util.Log;
import android.view.View;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

import java.io.File;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

  public static final String AES_ALGORITHM = "AES";
  public static final String AES_TRANSFORMATION = "AES/CTR/NoPadding";

  private static final String ENCRYPTED_FILE_NAME = "BigBuckBunnyEncrypted.exo.enc";
  private static final String FILE_NAME = "/BigBuckBunnyEncrypted.exo.enc";
  private static final String URL_1 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
  private static final String URL_2 = "https://www.radiantmediaplayer.com/media/bbb-360p.mp4";

  private Cipher mCipher;
  private SecretKeySpec mSecretKeySpec;
  private IvParameterSpec mIvParameterSpec;

  private File mEncryptedFile;

  private SimpleExoPlayerView mSimpleExoPlayerView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mSimpleExoPlayerView = findViewById(R.id.simpleexoplayerview);

    mEncryptedFile = new File(getFilesDir(), ENCRYPTED_FILE_NAME);

    Log.d(getClass().getCanonicalName(), getFilesDir().getAbsolutePath());

    SecureRandom secureRandom = new SecureRandom();
    final byte[] iv = { 65, 1, 2, 23, 4, 5, 6, 7, 32, 21, 10, 11, 12, 13, 84, 45 };
    final byte[] key = { 0, 42, 2, 54, 4, 45, 6, 7, 65, 9, 54, 11, 12, 13, 60, 15 };
    /*secureRandom.nextBytes(key);
    secureRandom.nextBytes(iv);*/

    mSecretKeySpec = new SecretKeySpec(key, AES_ALGORITHM);
    mIvParameterSpec = new IvParameterSpec(iv);

    try {
      mCipher = Cipher.getInstance(AES_TRANSFORMATION);
      mCipher.init(Cipher.DECRYPT_MODE, mSecretKeySpec, mIvParameterSpec);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private boolean hasFile() {
    return mEncryptedFile != null
        && mEncryptedFile.exists()
        && mEncryptedFile.length() > 0;
  }

  public void encryptVideo(View view) {
    if (hasFile()) {
      Log.d(getClass().getCanonicalName(), "encrypted file found, no need to recreate");
      return;
    }
    try {

      Cipher encryptionCipher = Cipher.getInstance(AES_TRANSFORMATION);
      encryptionCipher.init(Cipher.ENCRYPT_MODE, mSecretKeySpec, mIvParameterSpec);
      // TODO:
      // you need to encrypt a video somehow with the same key and iv...  you can do that yourself and update
      // the ciphers, key and iv used in this demo, or to see it from top to bottom,
      // supply a url to a remote unencrypted file - this method will download and encrypt it
      // this first argument needs to be that url, not null or empty...
      /*int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
      if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
      } else {
        new DownloadAndEncryptFileTask(URL_2, mEncryptedFile, encryptionCipher).execute();
      }*/

      new DownloadAndEncryptFileTask(URL_2, mEncryptedFile, encryptionCipher).execute();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void playVideo(View view) {
    DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
    TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveVideoTrackSelection.Factory(bandwidthMeter);
    TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
    LoadControl loadControl = new DefaultLoadControl();
    SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
    mSimpleExoPlayerView.setPlayer(player);
    DataSource.Factory dataSourceFactory = new EncryptedFileDataSourceFactory(mCipher, mSecretKeySpec, mIvParameterSpec, bandwidthMeter);
    ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
    try {
      Uri uri = Uri.fromFile(mEncryptedFile);
      MediaSource videoSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null);
      player.prepare(videoSource);
      player.setPlayWhenReady(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public void choosePath(View view) {
    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    i.addCategory(Intent.CATEGORY_DEFAULT);
    startActivityForResult(Intent.createChooser(i, "Choose directory"), 9999);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 9999) {
      assert data != null;
      Log.i("Test", "Result URI " + data.getData());

      String path = FileUtil.getFullPathFromTreeUri(data.getData(), this);
      Log.i("Test", "Path " + data.getData());

      //mEncryptedFile = new File("/storage/emulated/0/score_videos", FILE_NAME);
      mEncryptedFile = new File("/storage/6563-3130/Download", FILE_NAME);

      String state = EnvironmentCompat.getStorageState(mEncryptedFile);
      Log.i("Test", "getStorageState " + EnvironmentCompat.getStorageState(mEncryptedFile));
      if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
        Log.i("Test", "Read only ");
      } else {
        Log.i("Test", "Full access");
      }
    }
  }
}
