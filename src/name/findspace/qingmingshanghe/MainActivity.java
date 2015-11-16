package name.findspace.qingmingshanghe;




import java.util.HashMap;
import java.util.LinkedHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

	final static String TAG="qingming";
	Button autoLeft,autoRight,stop,listenup;
	OnlyImageView  view;
	private static Handler handler=new Handler();
	
	/**当前漫游的状态  left:-1 right:1 silent:0*/
	private int autoState;
	
	/**语音交互部分*/
	private SpeechRecognizer mIat;
	private SharedPreferences mSharedPreferences;
	/** 用HashMap存储听写结果*/
	private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
	/**要监听的重点命令文字*/
	final String [] cmds={"左","右","停止"};
	private Toast mToast;

	/**加速度感应部分*/
	private SensorManager sensorManager=null;
	private Sensor sensor=null;
	private SensorEventListener sensorEventListener=null;
	boolean sensorLeft=false,sensorRight=false;
	
	/**播放音频池*/
	protected SoundPool soundPool;
	
	/**记录多次触发向左或者向右的值，如果需要切换方向，则先把这个值给抵消掉*/
	private static int offset=0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		view= (OnlyImageView) findViewById(R.id.image_view);
		view.setCaller(this);
		Bitmap bitmap;
		bitmap=BitmapFactory.decodeResource(getResources(),  R.drawable.tsingming);
		view.setImageBitmap(bitmap);
		mToast = Toast.makeText(this,"",Toast.LENGTH_SHORT);
		setSoundPool();
		//语音监听部分
		SpeechUtility.createUtility(this, SpeechConstant.APPID+"=56372416");
		mIat= SpeechRecognizer.createRecognizer(MainActivity.this, null);
		setListener();
		setParam();
		//加速度感应部分
		setSensor();
	}
	/**@Description 设置加速度感应器相关*/
	private void setSensor(){
		sensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
		sensor=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorEventListener=new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent event) {
				float x = event.values[0];
				float y = event.values[1];
				responseSensor(x,y);
		}
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub
				
			}
		};
		
	}
	/**@Description 设置声音音频播放池*/
	private void setSoundPool(){
		soundPool=new SoundPool(5, AudioManager.STREAM_MUSIC, 5);
		//根据加载的顺序决定了id，所以需要和规定统一。目前1：水（船） 2：人群（集市） id从1开始
		soundPool.load(this,R.raw.water,1);
		soundPool.load(this, R.raw.peopleshort,1);
	}
	
	/**@Description 设置语音api的参数*/
	private void setParam(){
		mSharedPreferences = getSharedPreferences("com.iflytek.setting",Activity.MODE_PRIVATE);

		
		//听写的参数
		mIat.setParameter(SpeechConstant.DOMAIN, "iat");
		mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
		mIat.setParameter(SpeechConstant.ACCENT, "mandarin ");
		// 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
		mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "4000"));
		
		// 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
		mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));
	}
	private void setListener(){
		stop=(Button)findViewById(R.id.stop);
		stop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				autoState=0;
//				new Thread(new AutoMove(-offset)).start();
			}
		});
		autoRight=(Button)findViewById(R.id.autoRight);
		autoRight.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				stop.performClick();
				new Thread(new AutoMove(1)).start();
				offset+=1;
			}
		});
		autoLeft=(Button)findViewById(R.id.autoLeft);
		autoLeft.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stop.performClick();
				new Thread(new AutoMove(-1)).start();
				offset-=1;
			}
		});
		listenup=(Button)findViewById(R.id.listenup);
		listenup.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int ret = mIat.startListening(mRecoListener);
				if (ret != ErrorCode.SUCCESS) {
					showTip("听写失败,错误码：" + ret);
				} else {
					showTip("start");
				}
			}
		});
		
	}
	/**@Description 播放音效
	 */
	public void playSound(int id){
		if(id==0)return;
		soundPool.play(id, 1, 1, 1, 0, 1);
	}
	/**@Description 漫游更新画布的类，因为向左和向右漫游都调用了runnable，仅参数不同*/
	class AutoMove implements Runnable{
		int moveStep;
		int state;
		/**实际上传入的是autostate*/
		public AutoMove(int moveStep) {
			state=moveStep;
			this.moveStep=moveStep*2;
		}
		@Override
		public void run() {
			autoState=state;
			while(autoState==state){
				handler.post(new Runnable() {
					@Override
					public void run() {
						view.moveByVoice(moveStep);
					}
				});
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	/**@Description 对加速度感应器进行感应*/
	public void responseSensor(float x,float y){
		int xx=(int )x;
		int yy=(int)y;
		//右侧被抬起，向左跑
		if(yy<=1 &&xx>3&&!sensorLeft ){
			stop.performClick();
			autoLeft.performClick();
			sensorLeft=true;
			sensorRight=false;
		}
		else {
			if(yy<=1 &&xx<=-3 &&!sensorRight){
				stop.performClick();
				autoRight.performClick();
				sensorRight=true;
				sensorLeft=false;
			}
		}
		Log.e(TAG, String.format("x %f xx %d y %f yy %d", x,xx,y,yy));
	}
	/**@Description 对听到的命令进行响应*/
	public void responseCommand(String cmd){
		int theID=-1;
		for(int i=0;i<cmd.length();i++){
			if(cmd.indexOf(cmds[i])>=0){
				theID=i;
				break;
			}
		}
		switch (theID) {
		case 0:
			autoLeft.performClick();
			break;
		case 1:
			autoRight.performClick();
			break;
		case 2:
			stop.performClick();
			break;
		default:
			Log.e(TAG, "I can't recognise this command");
			break;
		}
	}
	private void showTip(final String str) {
		mToast.setText(str);
		mToast.show();
	}
	private RecognizerListener mRecoListener = new RecognizerListener(){
		//听写结果回调接口(返回Json格式结果,用户可参见附录12.1);
		//一般情况下会通过onResults接口多次返回结果,完整的识别内容是多次结果的累加;
		//关于解析Json的代码可参见MscDemo中JsonParser类;
		//isLast等于true时会话结束。
		public void onResult(RecognizerResult results, boolean isLast) {
			String text=JsonParser.parseIatResult(results.getResultString());
			String sn = null;
			// 读取json结果中的sn字段
			try {
				JSONObject resultJson = new JSONObject(results.getResultString());
				sn = resultJson.optString("sn");
			} catch (JSONException e) {
				e.printStackTrace();
			}

			mIatResults.put(sn, text);

			StringBuffer resultBuffer = new StringBuffer();
			for (String key : mIatResults.keySet()) {
				resultBuffer.append(mIatResults.get(key));
			}
			Log.e(TAG,resultBuffer.toString());
			String tempStr=resultBuffer.toString();
			if(tempStr.length()>=2)
				responseCommand(tempStr);
			mIatResults.clear();
//			Log.e(TAG,results.getResultString ());
		}
		//会话发生错误回调接口
		public void onError(SpeechError error) {
			error.getPlainDescription(true);
		//获取错误码描述
		}
		//开始录音
		public void onBeginOfSpeech() {}
		//结束录音
		public void onEndOfSpeech() {}
		//扩展用接口
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {}
		@Override
		public void onVolumeChanged(int arg0, byte[] arg1) {
			
		}
		};
	
	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(sensorEventListener, sensor,SensorManager.SENSOR_DELAY_GAME);
	};
	@Override
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(sensorEventListener);
	}
	protected void onDestroy() {
		super.onDestroy();
		mIat.cancel();
		mIat.destroy();
	};
	
}

