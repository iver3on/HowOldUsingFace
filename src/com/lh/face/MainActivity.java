package com.lh.face;

import com.faceplusplus.api.FaceDetecter;
import com.faceplusplus.api.FaceDetecter.Face;
import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import com.lh.face.R;

public class MainActivity extends Activity {

	private Bitmap curBitmap;
	private final static int REQUEST_GET_PHOTO = 1;
	ImageView imageView = null;
	HandlerThread detectThread = null;
	Handler detectHandler = null;
	Button button = null;
	FaceDetecter detecter = null;
	HttpRequests request = null;// 在线api

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// 创建一个HandlerThread，即创建了一个包含Looper的线程。
		detectThread = new HandlerThread("detect");
		detectThread.start();
		// 获取HandlerThread的Looper,创建Handler，通过Looper初始化
		detectHandler = new Handler(detectThread.getLooper());
		imageView = (ImageView) findViewById(R.id.imageview);
		curBitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.demo);
		imageView.setImageBitmap(curBitmap);
		detecter = new FaceDetecter();
		detecter.init(this, "82876b3b30967b8540d9b4bea44d1bd4");

		// FIXME 替换成申请的key
		request = new HttpRequests("82876b3b30967b8540d9b4bea44d1bd4",
				"bZ7YeJzAG-S0w36PdGQ_FjF687ExO4GR");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		detecter.release(this);// 释放引擎
	}

	public static Bitmap getFaceInfoBitmap(Face[] faceinfos, Bitmap oribitmap) {
		Bitmap tmp;
		tmp = oribitmap.copy(Bitmap.Config.ARGB_8888, true);

		Canvas localCanvas = new Canvas(tmp);
		Paint localPaint = new Paint();
		localPaint.setStrokeWidth(20); 
		localPaint.setColor(0xffff0000);
		// 让画出的图像是空心的
		localPaint.setStyle(Paint.Style.STROKE);
		// left, top, right bottom
		for (Face localFaceInfo : faceinfos) {
			RectF rect = new RectF(oribitmap.getWidth() * localFaceInfo.left,
					oribitmap.getHeight() * localFaceInfo.top,
					oribitmap.getWidth() * localFaceInfo.right,
					oribitmap.getHeight() * localFaceInfo.bottom);
			// 圈出人脸
			localCanvas.drawRect(rect, localPaint);
		}
		return tmp;
	}

	public static Bitmap getScaledBitmap(String fileName, int dstWidth) {
		BitmapFactory.Options localOptions = new BitmapFactory.Options();
		localOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(fileName, localOptions);
		int originWidth = localOptions.outWidth;
		int originHeight = localOptions.outHeight;

		localOptions.inSampleSize = originWidth > originHeight ? originWidth
				/ dstWidth : originHeight / dstWidth;
		localOptions.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(fileName, localOptions);
	}

	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.pick:
			startActivityForResult(new Intent("android.intent.action.PICK",
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
					REQUEST_GET_PHOTO);
			break;
		case R.id.takephoto:
			Intent  intent = new Intent(MainActivity.this,CameraPreview.class);
			startActivity(intent);
			break;
		case R.id.detect:
			detectHandler.post(new Runnable() {

				@Override
				public void run() {
					//通知detecte api 进行人脸检测，获取图片里面所有的人脸
					Face[] faceinfo = detecter.findFaces(curBitmap);// 进行人脸检测
					if (faceinfo == null) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(MainActivity.this, "未发现人脸信息",
										Toast.LENGTH_LONG).show();
							}
						});
						return;
					}

					// offline api交互
					try {
						request.offlineDetect(detecter.getImageByteArray(),
								detecter.getResultJsonString(),
								new PostParameters());
					} catch (FaceppParseException e) {
						// TODO 自动生成的 catch 块
						e.printStackTrace();
					}
					final Bitmap bit = getFaceInfoBitmap(faceinfo, curBitmap);
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							imageView.setImageBitmap(bit);
							System.gc();
						}
					});
				}
			});
		}
	}
	
//打开本地图片
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case REQUEST_GET_PHOTO: {
				if (data != null) {
					final String str;
					Uri localUri = data.getData();
					String[] arrayOfString = new String[1];
					arrayOfString[0] = "_data";
					Cursor localCursor = getContentResolver().query(localUri,
							arrayOfString, null, null, null);
					if (localCursor == null)
						return;
					localCursor.moveToFirst();
					str = localCursor.getString(localCursor
							.getColumnIndex(arrayOfString[0]));
					localCursor.close();
					if ((curBitmap != null) && (!curBitmap.isRecycled()))
						curBitmap.recycle();
					curBitmap = getScaledBitmap(str, 600);
					imageView.setImageBitmap(curBitmap);
				}
				break;
			}
			}

		}
	}
}
