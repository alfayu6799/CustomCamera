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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private TextView ocrResult;

    private ImageView photoSave, photoDel;
    private TextView  messages;

    private Bitmap bitmap;

    private boolean isCameraPermission = false;

    //tess two (ORC) 語言包放在/mnt/sdcard/Tess/tessdata目錄下
    public static String TESS_DATA = "/tessdata";
    public static String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/Tess";
    private TessBaseAPI tessBaseAPI;

    //20200428
    private static final String DATAPATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    private static final String tessdata = DATAPATH + File.separator + "tessdata";
    private static final String DEFAULT_LANGUAGE = "chi_tra";
    private static final String DEFAULT_LANGUAGE_NAME = DEFAULT_LANGUAGE + ".traineddata";
    private static final String LANGUAGE_PATH = tessdata + File.separator + DEFAULT_LANGUAGE_NAME;

    private String result = "No Result !!";

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

        ocrResult = (TextView) findViewById(R.id.tv_ocr_result);
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
                Log.d(TAG, "onRequestPermissionsResult: copyToSD");
                copyToSD(LANGUAGE_PATH, DEFAULT_LANGUAGE_NAME); //將自庫寫入設備中
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
            copyToSD(LANGUAGE_PATH, DEFAULT_LANGUAGE_NAME); //語言包check

            setPic(imageFilePath);               //拍完後的照片顯示

            //OCRResult(bitmap);
            messages.setVisibility(View.VISIBLE);
            mBtnPic.setVisibility(View.INVISIBLE);

            //根據有辨識到特定字元才會顯示存檔icon
            if (result.contains(getString(R.string.key_word))){
                Log.d(TAG, "onActivityResult: ??");
                photoSave.setVisibility(View.VISIBLE);
            }

            //儲存照片
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

    //語言包copy到設備端 (assets的中文識別字庫檔創建到設備端,不然系統會crash)
    private void copyToSD(String path, String name) {
//        Log.d(TAG, "copyToSD: " + path);
//        Log.d(TAG, "copyToSD: " + name);

        //如果存在就删掉
        File f = new File(path);
        if (f.exists()){
            f.delete();
        }
        if (!f.exists()){
            File p = new File(f.getParent());
            if (!p.exists()){
                p.mkdirs();
            }
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        InputStream is=null;
        OutputStream os=null;
        try {
            is = this.getAssets().open(name);
            File file = new File(path);
            os = new FileOutputStream(file);
            byte[] bytes = new byte[2048];
            int len = 0;
            while ((len = is.read(bytes)) != -1) {
                os.write(bytes, 0, len);
            }
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
                if (os != null)
                    os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void OCRResult(Bitmap bitmap) {
//        Bitmap bmp = BitmapFactory.decodeResource(this.getResources(), R.mipmap.image_1);
        tessBaseAPI.init(DATAPATH, DEFAULT_LANGUAGE);  //中文辨識
        tessBaseAPI.setImage(bitmap);
        result = tessBaseAPI.getUTF8Text();
        tessBaseAPI.end();
        ocrResult.setText(result);  //辨識後結果顯示
        Log.d(TAG, "OCRResult Result : " + result);
    }


    //檢查語言包是否存在設備的sdcard內
    private void prepareTessdata() {
        try {
            File dir = getExternalFilesDir(TESS_DATA);
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    Toast.makeText(MainActivity.this, "The Folder" + dir.getPath() + "was not created", Toast.LENGTH_SHORT).show();
                }
            }

            //AssetsCopy(dir);

        }catch (Exception e){
            Log.d(TAG, "prepareTessdata: " + e.getMessage());
        }
    }

    private void AssetsCopy(File dir) throws IOException {
        String fileList[] = getAssets().list("");
        for (String fileName : fileList){
            String pathToDataFile = dir + "/" + fileName;
            if (!new File(pathToDataFile).exists())
            {
                InputStream in = getAssets().open(fileName);
                OutputStream out = new FileOutputStream(pathToDataFile);
                byte[] buff = new byte[1024];
                int len;
                while ((len = in.read(buff)) > 0 ){
                    out.write(buff,0, len);
                }
                in.close();
                out.close();
            }
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

//        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mShowImage.setImageBitmap(bitmap);

        OCRResult(bitmap); //開始識別
    }

    //back - key Listener
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d(TAG, "onBackPressed: ");
//        delPic(imageFilePath);
    }
}
