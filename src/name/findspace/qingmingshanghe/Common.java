/* 
 * @Title:  Common.java 
 * @Copyright:  XXX Co., Ltd. Copyright YYYY-YYYY,  All rights reserved 
 * @Description:  TODO<请描述此文件是做什么的> 
 * @author:  ZhuZiQiang 
 * @data:  2014-4-4 下午4:06:20 
 * @version:  V1.0 
 */

package name.findspace.qingmingshanghe;


public class Common {

	/**
	 * 初始化状态常量
	 */
	public static final int STATUS_INIT = 1;

	/**
	 * 图片放大状态常量
	 */
	public static final int STATUS_OUT = 2;

	/**
	 * 图片缩小状态常量
	 */
	public static final int STATUS_IN = 3;

	/**
	 * 图片拖动状态常量
	 */
	public static final int STATUS_MOVE = 4;
	/**@Description 需要播放声音的区域所在的坐标
	 * 一个区域以左上角和右下角两个点的坐标划分，(500,300)是左上角(1100,600)是右下角*/
	public static final int ZonesX[]={-500,-1100};
	public static final int ZonesY[]={-300,-600};
	
}
