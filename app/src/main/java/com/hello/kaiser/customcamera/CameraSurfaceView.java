package com.hello.kaiser.customcamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by Administrator on 2020/4/23 //自定義相機
 */
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.AutoFocusCallback {

    private static final String TAG = "CameraSurfaceView";

    private Context mContext;
    private SurfaceHolder holder;
    private Camera mCamera;
    private Camera.Parameters params;

    private int mScreenWidth;
    private int mScreenHeight;
    private CameraTopRectView topView;

    private String filePath;
    private Activity activity;

    //

    public CameraSurfaceView(Context context) {
        this(context, null);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        getScreenMetrix(context);
        topView = new CameraTopRectView(context, attrs);
        initView();
    }

    //取得手機螢幕大小
    private void getScreenMetrix(Context context) {
        WindowManager WM = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        WM.getDefaultDisplay().getMetrics(outMetrics);
        mScreenWidth = outMetrics.widthPixels;
        mScreenHeight = outMetrics.heightPixels;
    }

    private void initView() {
        holder = getHolder();//獲得surfaceHolder
        holder.addCallback(this);
//        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);//設置類型
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
        if (mCamera == null) {
            mCamera = Camera.open();//開啟相機
            params = mCamera.getParameters();
            try {
                mCamera.setPreviewDisplay(holder);//攝像頭畫面顯示在Surface上
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged");

        setCameraParams(mCamera, mScreenWidth, mScreenHeight);
        mCamera.startPreview();
//        mCamera.takePicture(null, null, jpeg);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        mCamera.stopPreview();//停止預覽
        mCamera.release();   //釋放相機資源
        mCamera = null;
        holder = null;
    }

    @Override
    public void onAutoFocus(boolean success, Camera Camera) {
        if (success) {
            Log.i(TAG, "onAutoFocus success=" + success);
            System.out.println(success);
        }
    }


    private void setCameraParams(Camera camera, int width, int height) {
        Log.i(TAG, "setCameraParams  width=" + width + "  height=" + height);
        Camera.Parameters parameters = mCamera.getParameters();
        // 獲取攝像頭支持的PictureSize列表
        List<Camera.Size> pictureSizeList = parameters.getSupportedPictureSizes();
        for (Camera.Size size : pictureSizeList) {
            Log.i(TAG, "pictureSizeList size.width=" + size.width + "  size.height=" + size.height);
        }
        /**從列表中選取合適的分辨率*/
        Camera.Size picSize = getProperSize(pictureSizeList, ((float) height / width));
        if (null == picSize) {
            Log.i(TAG, "null == picSize");
            picSize = parameters.getPictureSize();
        }
        Log.i(TAG, "picSize.width=" + picSize.width + "  picSize.height=" + picSize.height);
        // 根據選出的PictureSize重新設置SurfaceView大小
        float w = picSize.width;
        float h = picSize.height;
        parameters.setPictureSize(picSize.width, picSize.height);
        this.setLayoutParams(new FrameLayout.LayoutParams((int) (height * (h / w)), height));

        // 獲取攝像頭支持的PreviewSize列表
        List<Camera.Size> previewSizeList = parameters.getSupportedPreviewSizes();

        for (Camera.Size size : previewSizeList) {
            Log.i(TAG, "previewSizeList size.width=" + size.width + "  size.height=" + size.height);
        }
        Camera.Size preSize = getProperSize(previewSizeList, ((float) height) / width);
        if (null != preSize) {
            Log.i(TAG, "preSize.width=" + preSize.width + "  preSize.height=" + preSize.height);
            parameters.setPreviewSize(preSize.width, preSize.height);
        }

        parameters.setJpegQuality(100); // 設置照片質量
        if (parameters.getSupportedFocusModes().contains(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 連續對焦模式
        }
        mCamera.cancelAutoFocus();//自動對焦
        mCamera.setDisplayOrientation(270);// 設置PreviewDisplay的方向，效果就是將捕獲的畫面旋轉多少度顯示(原先是90)
        mCamera.setParameters(parameters);
    }

    /**
     * 從列表中選取合適的分辨率
     * 默認w:h = 4:3
     * <p>注意：這裡的w對應屏幕的height
     * h對應屏幕的width<p/>
     */
    private Camera.Size getProperSize(List<Camera.Size> pictureSizeList, float screenRatio) {
        Log.i(TAG, "screenRatio=" + screenRatio);
        Camera.Size result = null;
        for (Camera.Size size : pictureSizeList) {
            float currentRatio = ((float) size.width) / size.height;
            if (currentRatio - screenRatio == 0) {
                result = size;
                break;
            }
        }

        if (null == result) {
            for (Camera.Size size : pictureSizeList) {
                float curRatio = ((float) size.width) / size.height;
                if (curRatio == 4f / 3) {// 默認w:h = 4:3
                    result = size;
                    break;
                }
            }
        }

        return result;
    }


    // 拍照瞬間調用
    private Camera.ShutterCallback shutter = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            Log.i(TAG, "shutter");
            System.out.println("拍照瞬間調用");
        }
    };

    // 獲得沒有壓縮過的圖片數據
    private Camera.PictureCallback raw = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera Camera) {
            Log.i(TAG, "raw");
            System.out.println("獲得沒有壓縮過的圖片數據");
        }
    };

    //創建jpeg圖片回調數據對象
    private Camera.PictureCallback jpeg = new Camera.PictureCallback() {

        private Bitmap bitmap;

        @Override
        public void onPictureTaken(byte[] data, Camera Camera) {
            topView.draw(new Canvas());

            BufferedOutputStream bos = null;
            Bitmap bm = null;
            if (data != null) {

            }

            try {
                // 獲得圖片
                bm = BitmapFactory.decodeByteArray(data, 0, data.length);
                Log.d("checkpoint", "checkpoint - " + bm);
//                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//                    String filePath = "/sdcard/dyk" + System.currentTimeMillis() + ".JPEG";//照片保存途徑

//              //圖片儲存前旋轉
                Matrix m = new Matrix();
                int height = bm.getHeight();
                int width = bm.getWidth();
                m.setRotate(90);  //原先是90
                //旋轉後的圖片
                bitmap = Bitmap.createBitmap(bm, 0, 0, width, height, m, true);

                System.out.println("創建jpeg圖片回調數據對象");
                File file = new File(filePath);
                if (!file.exists()) {
                    file.createNewFile();
                }
                bos = new BufferedOutputStream(new FileOutputStream(file));

                Bitmap sizeBitmap = Bitmap.createScaledBitmap(bitmap,
                        topView.getViewWidth(), topView.getViewHeight(), true);
                bm = Bitmap.createBitmap(sizeBitmap, topView.getRectLeft(),
                        topView.getRectTop(),
                        topView.getRectRight() - topView.getRectLeft(),
                        topView.getRectBottom() - topView.getRectTop());// 擷取

                bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);//壓縮圖片

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    bos.flush();           //輸出
                    bos.close();          //關閉
                    bm.recycle();        // 回收bitmap
                    mCamera.stopPreview();  // 關閉預覽
                    activity.setResult(Activity.RESULT_OK);
                    activity.finish();
//                    mCamera.startPreview();  // 開啟預覽
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    };

    public void takePicture(Activity activity, String filePath) {
        this.filePath = filePath;
        this.activity = activity;
        //設置參數,並拍照
        setCameraParams(mCamera, mScreenWidth, mScreenHeight);
        // 當當調用camera.takePiture方法後，camera關閉了預覽，這時需要調用startPreview()來重新開啟預覽
        mCamera.takePicture(null, null, jpeg);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    //開啟閃光
    public  void openFlash(){
        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH); //turn on
        mCamera.setParameters(params);
    }

    //關閉閃光
    public void offFlash() {
        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF); //turn off
        mCamera.setParameters(params);
    }

    //曝光
    public int exposure(int exposure) {
        params.setExposureCompensation(exposure);
        mCamera.setParameters(params);
        return exposure;
    }
}