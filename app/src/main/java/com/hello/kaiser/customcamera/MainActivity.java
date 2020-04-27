package com.hello.kaiser.customcamera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();

    private Activity activity;
    public static final int PermissionCode = 1000;
    public static final int GetPhotoCode = 1001;

    private ImageView mBtnPic;
    private ImageView mShowImage;
    String  imageFilePath;

    private ImageView photoSave, photoDel;
    private TextView  messages;

    private boolean isCameraPermission = false;

    //tess two (ORC) 語言包放在/mnt/sdcard/Tess/tessdata目錄下
    public static String TESSBASE_PATH = Environment.getExternalStorageDirectory().toString() + "/Tess";
    private TessBaseAPI tessBaseAPI;
    private static String lang = "eng";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        activity = this;
        initView();
        initListener();
        //Tess two
        tessBaseAPI = new TessBaseAPI();

    }

    private void initView() {
        mBtnPic = (ImageView) findViewById(R.id.btn_take_pic);
        mShowImage = (ImageView) findViewById(R.id.show_image);
        //
        photoSave = (ImageView) findViewById(R.id.btn_photo_save);
        photoDel = (ImageView) findViewById(R.id.btn_photo_del);
        messages = (TextView) findViewById(R.id.tv_messages);
    }

    private void initListener() {
        mBtnPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionCode) {
            //假如允許了
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isCameraPermission = true;
                //do something
                Toast.makeText(this, "感謝賜予權限！", Toast.LENGTH_SHORT).show();
                startActivityForResult(new Intent(MainActivity.this, TakePicActivity.class), GetPhotoCode);
            }
            //假如拒絕了
            else {
                isCameraPermission = false;
                //do something
                Toast.makeText(this, "CAMERA權限FAIL，請給權限", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //創造檔案名稱、和存擋路徑
    private File createImageFile() throws IOException {
        String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss",
                        Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir =
                getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        imageFilePath = image.getAbsolutePath();
        return image;
    }

    private void openCamera() {
        //已獲得權限
        if (isCameraPermission) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                Log.d("checkpoint", "error for createImageFile 創建路徑失敗");
            }
            //成功創建路徑的話
            if (photoFile != null) {
                Intent intent = new Intent(MainActivity.this, TakePicActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("url", photoFile.getAbsolutePath());
                intent.putExtras(bundle);
                startActivityForResult(intent, GetPhotoCode);
            }
        }
        //沒有獲得權限
        else {
            getPermission();
        }
    }

    private void getPermission() {
        //檢查是否取得權限
        final int permissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
        //沒有權限時
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            isCameraPermission = false;
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PermissionCode);
        } else { //已獲得權限
            isCameraPermission = true;
            openCamera();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GetPhotoCode) {
            Log.d(TAG, "onActivityResult: " + imageFilePath);
            setPic(imageFilePath); //拍完後的照片顯示
            prepareTessdata(); //check 語言包是否存在
            startOCR(imageFilePath);
            //顯示刪除 or 存檔 (根據辨識特定字元)
            messages.setVisibility(View.VISIBLE);
            mBtnPic.setVisibility(View.INVISIBLE);

            photoSave.setVisibility(View.VISIBLE);
            photoSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(MainActivity.this, getString(R.string.picture_save_ok),Toast.LENGTH_SHORT).show();
                    openCamera();  //return open camera and take picture
                }
            });
            photoDel.setVisibility(View.VISIBLE);
            photoDel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    delPic(imageFilePath);
                    openCamera(); //return open camera and take picture again
                }
            });
        }
    }

    private void startOCR(String imageFilePath) {
        Bitmap bmp = BitmapFactory.decodeResource(this.getResources(), R.mipmap.image_1);
        tessBaseAPI.init(TESSBASE_PATH, "chi_tra"); //中文繁體字庫
        tessBaseAPI.setImage(bmp);
        String result = "No Result !!";
        result = tessBaseAPI.getUTF8Text();
        tessBaseAPI.end();

        Log.d(TAG, "startOCR Result : " + result);
    }

    private void prepareTessdata() {
        File dir = getExternalFilesDir(TESSBASE_PATH);
        if (!dir.exists()){
            Toast.makeText(MainActivity.this, "The Folder" + dir.getPath() + "was not created", Toast.LENGTH_SHORT).show();
        }else{
            Log.d(TAG, "prepareTessdata: ");
        }
        
    }

    private void delPic(String imageFilePath) {
//        Log.d(TAG, "delPic: " + imageFilePath);
        File file = new File(imageFilePath);
        if (file.exists()) {
            file.delete();
        }
    }


    private void setPic(String mCurrentPhotoPath) {
        // Get the dimensions of the View
        int targetW = mShowImage.getWidth();
        int targetH = mShowImage.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mShowImage.setImageBitmap(bitmap);
    }

    //back - key Listener
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d(TAG, "onBackPressed: ");
//        delPic(imageFilePath);
    }
}
