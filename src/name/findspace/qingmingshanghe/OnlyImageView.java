

package name.findspace.qingmingshanghe;

import name.findspace.qingmingshanghe.Common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * @Description 用于画布的绘图，提供核心接口给其他功能
 * @author FindHao
 * @Date 2015.11.2
 */
public class OnlyImageView extends View {

	/**
	 * 用于对图片进行移动和缩放变换的矩阵
	 */
	private Matrix matrix = new Matrix();

	/**待展示的Bitmap对象 */
	private Bitmap sourceBitmap;
	/**记录当前操作的状态，可选值为 1、STATUS_INIT 2、STATUS_OUT 3、STATUS_IN 4、STATUS_MOVE	 */
	private int currentStatus;
	/**ZoomImageView控件的宽度，高度*/
	private int width,height;
	/**记录两指同时放在屏幕上时，中心点的横坐标、纵坐标值 */
	private float centerPointX,centerPointY;
	/**记录当前图片的宽度、高度，图片被缩放时，这个值会一起变动	 */
	private float currentBitmapWidth,currentBitmapHeight;
	/**记录上次手指移动时的横坐标\纵坐标 */
	private float lastXMove = -1,lastYMove = -1;
	/**记录手指在横坐标\纵坐标方向上的移动距离	 */
	private float movedDistanceX, movedDistanceY;
	/**
	 * 记录图片在矩阵上的横向偏移值\纵向偏移值
	 */
	private float totalTranslateX,totalTranslateY;

	/**
	 * 记录图片在矩阵上的总缩放比例
	 */
	private float totalRatio;

	/**
	 * 记录手指移动的距离所造成的缩放比例
	 */
	private float scaledRatio;

	/**
	 * 记录图片初始化时的缩放比例
	 */
	private float initRatio;

	/**
	 * 记录上次两指之间的距离
	 */
	private double lastFingerDis;
	/**我记录的边界值，不知道为什么和获取的一样*/
	private final  float theEdge=4665;
	
	
	/**
	 * OnlyImageView构造函数，将当前操作状态设为STATUS_INIT。
	 * 
	 * @param context
	 * @param attrs
	 */
	public OnlyImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		currentStatus = Common.STATUS_INIT;
	}

	MainActivity caller;
	/**@description 设置回调，以使用主进程的东西，比如播放音乐*/
	public void setCaller(MainActivity caller){
		this.caller=caller;
	}
	
	
	/**
	 * 将待展示的图片设置进来。
	 * 
	 * @param bitmap
	 *            待展示的Bitmap对象
	 */
	public void setImageBitmap(Bitmap bitmap) {
		sourceBitmap = bitmap;
		invalidate();
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (changed) {
			// 分别获取到ZoomImageView的宽度和高度
			width = getWidth();
			height = getHeight();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (initRatio == totalRatio) {
			getParent().requestDisallowInterceptTouchEvent(false);
		} else {
			getParent().requestDisallowInterceptTouchEvent(true);
		}
		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_POINTER_DOWN:
			if (event.getPointerCount() == 2) {
				// 当有两个手指按在屏幕上时，计算两指之间的距离
				lastFingerDis = distanceBetweenFingers(event);
			}
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_MOVE:
			if (event.getPointerCount() == 1) {
				// 只有单指按在屏幕上移动时，为拖动状态
				float xMove = event.getX();
				float yMove = event.getY();
				if (lastXMove == -1 && lastYMove == -1) {
					lastXMove = xMove;
					lastYMove = yMove;
				}
				currentStatus = Common.STATUS_MOVE;
				movedDistanceX = xMove - lastXMove;
				movedDistanceY = yMove - lastYMove;
				// 进行边界检查，不允许将图片拖出边界
				if (totalTranslateX + movedDistanceX > 0) {
					movedDistanceX = 0;
				} else if (width - (totalTranslateX + movedDistanceX) > currentBitmapWidth) {
					movedDistanceX = 0;
				}
				if (totalTranslateY + movedDistanceY > 0) {
					movedDistanceY = 0;
				} else if (height - (totalTranslateY + movedDistanceY) > currentBitmapHeight) {
					movedDistanceY = 0;
				}
				// 调用onDraw()方法绘制图片
				invalidate();
				lastXMove = xMove;
				lastYMove = yMove;
			} else if (event.getPointerCount() == 2) {
				// 有两个手指按在屏幕上移动时，为缩放状态
				centerPointBetweenFingers(event);
				double fingerDis = distanceBetweenFingers(event);
				if (fingerDis > lastFingerDis) {
					currentStatus = Common.STATUS_OUT;
				} else {
					currentStatus = Common.STATUS_IN;
				}
				// 进行缩放倍数检查，最大只允许将图片放大4倍，最小可以缩小到初始化比例
				if ((currentStatus == Common.STATUS_OUT && totalRatio < 100 * initRatio)
						|| (currentStatus == Common.STATUS_IN && totalRatio > initRatio)) {
					scaledRatio = (float) (fingerDis / lastFingerDis);
					totalRatio = totalRatio * scaledRatio;
					if (totalRatio > 100 * initRatio) {
						totalRatio = 100 * initRatio;
					} else if (totalRatio < initRatio) {
						totalRatio = initRatio;
					}
					// 调用onDraw()方法绘制图片
					invalidate();
					lastFingerDis = fingerDis;
				}
			}
			break;
		case MotionEvent.ACTION_POINTER_UP:
			if (event.getPointerCount() == 2) {
				// 手指离开屏幕时将临时值还原
				lastXMove = -1;
				lastYMove = -1;
			}
			break;
		case MotionEvent.ACTION_UP:
			Log.e(MainActivity.TAG, String.format("total x %f\ntotal y %f \ndistancex %f y %f", totalTranslateX,totalTranslateY,lastXMove,lastYMove));
			
			caller.playSound(judgeZone(totalTranslateX-lastXMove,totalTranslateY-lastYMove));
			
			// 手指离开屏幕时将临时值还原
			lastXMove = -1;
			lastYMove = -1;
			break;
		default:
			break;
		}
		return true;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		switch (currentStatus) {
		case Common.STATUS_OUT:
		case Common.STATUS_IN:
			zoom(canvas);
			break;
		case Common.STATUS_MOVE:
			move(canvas);
			break;
		case Common.STATUS_INIT:
			initBitmap(canvas);
		default:
			if (sourceBitmap != null) {
				canvas.drawBitmap(sourceBitmap, matrix, null);
			}
			break;
		}
	}

	/**
	 * 对图片进行缩放处理。
	 * 
	 * @param canvas
	 */
	private void zoom(Canvas canvas) {
		matrix.reset();
		// 将图片按总缩放比例进行缩放
		matrix.postScale(totalRatio, totalRatio);
		float scaledWidth = sourceBitmap.getWidth() * totalRatio;
		float scaledHeight = sourceBitmap.getHeight() * totalRatio;
		float translateX = 0f;
		float translateY = 0f;
		// 如果当前图片宽度小于屏幕宽度，则按屏幕中心的横坐标进行水平缩放。否则按两指的中心点的横坐标进行水平缩放
		if (currentBitmapWidth < width) {
			translateX = (width - scaledWidth) / 2f;
		} else {
			translateX = totalTranslateX * scaledRatio + centerPointX * (1 - scaledRatio);
			// 进行边界检查，保证图片缩放后在水平方向上不会偏移出屏幕
			if (translateX > 0) {
				translateX = 0;
			} else if (width - translateX > scaledWidth) {
				translateX = width - scaledWidth;
			}
		}
		// 如果当前图片高度小于屏幕高度，则按屏幕中心的纵坐标进行垂直缩放。否则按两指的中心点的纵坐标进行垂直缩放
		if (currentBitmapHeight < height) {
			translateY = (height - scaledHeight) / 2f;
		} else {
			translateY = totalTranslateY * scaledRatio + centerPointY * (1 - scaledRatio);
			// 进行边界检查，保证图片缩放后在垂直方向上不会偏移出屏幕
			if (translateY > 0) {
				translateY = 0;
			} else if (height - translateY > scaledHeight) {
				translateY = height - scaledHeight;
			}
		}
		// 缩放后对图片进行偏移，以保证缩放后中心点位置不变
		matrix.postTranslate(translateX, translateY);
		totalTranslateX = translateX;
		totalTranslateY = translateY;
		currentBitmapWidth = scaledWidth;
		currentBitmapHeight = scaledHeight;
		canvas.drawBitmap(sourceBitmap, matrix, null);
	}

	/**
	 * 对图片进行平移处理
	 * 
	 * @param canvas
	 */
	private void move(Canvas canvas) {
		matrix.reset();
		// 根据手指移动的距离计算出总偏移值
		float translateX = totalTranslateX + movedDistanceX;
		float translateY = totalTranslateY + movedDistanceY;
		// 先按照已有的缩放比例对图片进行缩放
		matrix.postScale(totalRatio, totalRatio);
		// 再根据移动距离进行偏移
		matrix.postTranslate(translateX, translateY);
		totalTranslateX = translateX;
		totalTranslateY = translateY;
		canvas.drawBitmap(sourceBitmap, matrix, null);
	}
	/**
	 * @Description 声控的接口,目前只做横向移动
	 * @author FindHao
	 * @param directionX 水平方向移动的距离
	 * */
	public void moveByVoice(int directionX){
		matrix.reset();
		float translateX = totalTranslateX+directionX;
//		Log.e("qingming", "translateX "+translateX+"scale"+totalRatio);
		if(translateX>=0)translateX=0;
		//注意这里是负值，因为左上角是0 0,而且edge值是自己测试得到的，bitmapWidth比测试的值要大，导致图片会移出边界，不知道为什么
		if(translateX<=-(theEdge*totalRatio))translateX=-(theEdge*totalRatio);
		// 先按照已有的缩放比例对图片进行缩放
		matrix.postScale(totalRatio, totalRatio);
		// 再根据移动距离进行偏移
		matrix.postTranslate(translateX, totalTranslateY);
		totalTranslateX = translateX;
		invalidate();
	}
	
	/**
	 * 对图片进行初始化操作，包括让图片居中，以及当图片大于屏幕宽高时对图片进行压缩。
	 * 
	 * @param canvas
	 */
	private void initBitmap(Canvas canvas) {
		if (sourceBitmap != null) {
			matrix.reset();
			int bitmapWidth = sourceBitmap.getWidth();
			int bitmapHeight = sourceBitmap.getHeight();
			Log.e("qingming", bitmapHeight+" "+bitmapWidth);
			if (bitmapWidth > width || bitmapHeight > height) {
				if (bitmapWidth - width > bitmapHeight - height) {
					// 当图片宽度大于屏幕宽度时，将图片等比例压缩，使它可以完全显示出来
//					float ratio = width / (bitmapWidth * 1.0f);
					float ratio = 1f;
					matrix.postScale(ratio, ratio);
//					float translateY = (height - (bitmapHeight * ratio)) / 2f;
//					// 在纵坐标方向上进行偏移，以保证图片居中显示
//					matrix.postTranslate(0, translateY);
//					totalTranslateY = translateY;
					totalRatio = initRatio = ratio;
				} else {
					// 当图片高度大于屏幕高度时，将图片等比例压缩，使它可以完全显示出来
					float ratio = 1f;
//					float ratio = height / (bitmapHeight * 1.0f);
//					matrix.postScale(ratio, ratio);
//					float translateX = (width - (bitmapWidth * ratio)) / 2f;
//					// 在横坐标方向上进行偏移，以保证图片居中显示
//					matrix.postTranslate(translateX, 0);
//					totalTranslateX = translateX;
					totalRatio = initRatio = ratio;
				}
				currentBitmapWidth = bitmapWidth * initRatio;
				currentBitmapHeight = bitmapHeight * initRatio;
			} else {
				// 当图片的宽高都小于屏幕宽高时，直接让图片居中显示
				float translateX = (width - sourceBitmap.getWidth()) / 2f;
				float translateY = (height - sourceBitmap.getHeight()) / 2f;
				matrix.postTranslate(translateX, translateY);
				totalTranslateX = translateX;
				totalTranslateY = translateY;
				totalRatio = initRatio = 1f;
				currentBitmapWidth = bitmapWidth;
				currentBitmapHeight = bitmapHeight;
			}
			canvas.drawBitmap(sourceBitmap, matrix, null);
		}
	}

	/**
	 * 计算两个手指之间的距离。
	 * 
	 * @param event
	 * @return 两个手指之间的距离
	 */
	private double distanceBetweenFingers(MotionEvent event) {
		float disX = Math.abs(event.getX(0) - event.getX(1));
		float disY = Math.abs(event.getY(0) - event.getY(1));
		return Math.sqrt(disX * disX + disY * disY);
	}

	/**
	 * 计算两个手指之间中心点的坐标。
	 * 
	 * @param event
	 */
	private void centerPointBetweenFingers(MotionEvent event) {
		float xPoint0 = event.getX(0);
		float yPoint0 = event.getY(0);
		float xPoint1 = event.getX(1);
		float yPoint1 = event.getY(1);
		centerPointX = (xPoint0 + xPoint1) / 2;
		centerPointY = (yPoint0 + yPoint1) / 2;
	}
	/**@Description 根据传入的坐标，判断是否是属于需要音效的区域
	 * <br>目前只考虑只返回一个区域值
	 * */
	private int judgeZone(float f,float g){
		int len=Common.ZonesX.length;
		for(int i=0;i<len;i+=2){
			if(f<Common.ZonesX[i]&&f>Common.ZonesX[i+1]&&g<Common.ZonesY[i]&&g>Common.ZonesY[i+1]){
				//soundpool是从1开始编号的
				return i+1;
			}
		}
		return 0;
	}
}
