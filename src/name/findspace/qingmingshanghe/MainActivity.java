package name.findspace.qingmingshanghe;




import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {

	final static String TAG=MainActivity.class.getSimpleName();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final OnlyImageView view= (OnlyImageView) findViewById(R.id.image_view);
		Bitmap bitmap;
		bitmap=BitmapFactory.decodeResource(getResources(),  R.drawable.tsingming);
		view.setImageBitmap(bitmap);
		Log.d(TAG, "加载结束");
		Button bt=(Button)findViewById(R.id.button1);
		bt.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				view.moveByVoice();
			}
		
		});
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	/**
	* 计算压缩比例值
	* @param options       解析图片的配置信息
	* @param reqWidth            所需图片压缩尺寸最小宽度
	* @param reqHeight           所需图片压缩尺寸最小高度
	* @return
	*/
	public static int calculateInSampleSize(BitmapFactory.Options options,
	             int reqWidth, int reqHeight) {
	       // 保存图片原宽高值
	       final int height = options. outHeight;
	       final int width = options. outWidth;
	       // 初始化压缩比例为1
	       int inSampleSize = 10;

	       // 当图片宽高值任何一个大于所需压缩图片宽高值时,进入循环计算系统
	       if (height > reqHeight || width > reqWidth) {

	             final int halfHeight = height / 2;
	             final int halfWidth = width / 2;

	             // 压缩比例值每次循环两倍增加,
	             // 直到原图宽高值的一半除以压缩值后都~大于所需宽高值为止
	             while ((halfHeight / inSampleSize) >= reqHeight
	                        && (halfWidth / inSampleSize) >= reqWidth) {
	                  inSampleSize *= 2;
	            }
	      }

	       return inSampleSize;
	}
	
	
}
