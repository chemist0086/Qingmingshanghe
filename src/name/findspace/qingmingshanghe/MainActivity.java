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
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

	
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		view= (OnlyImageView) findViewById(R.id.image_view);
		Bitmap bitmap;
		bitmap=BitmapFactory.decodeResource(getResources(),  R.drawable.tsingming);
		view.setImageBitmap(bitmap);
		mToast = Toast.makeText(this,"",Toast.LENGTH_SHORT);
		setListener();
		SpeechUtility.createUtility(this, SpeechConstant.APPID+"=56372416");
		
		mIat= SpeechRecognizer.createRecognizer(MainActivity.this, null);
		setParam();

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
		autoRight=(Button)findViewById(R.id.autoRight);
		autoRight.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				new Thread(new AutoMove(1)).start();
			}
		});
		autoLeft=(Button)findViewById(R.id.autoLeft);
		autoLeft.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread(new AutoMove(-1)).start();
			}
		});
		stop=(Button)findViewById(R.id.stop);
		stop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				autoState=0;
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
	protected void onDestroy() {
		super.onDestroy();
		mIat.cancel();
		mIat.destroy();
	};
	
}

