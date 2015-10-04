package com.test.Crazepony;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.WindowManager;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;

//import com.test.BTClient.BTClient;
//Runnable接口方法创建线程，匿名类
@SuppressLint("NewApi")
public class MySurfaceView extends SurfaceView  implements Callback, Runnable {

    private final static String TAG = MySurfaceView.class.getSimpleName();

	private Thread th;
	private SurfaceHolder sfh;
	private Canvas canvas;
	private Paint paint;
	private boolean flag;

	private float LEFT_CENTERX;
	private float LEFT_CENTERY;
	private float RIGHT_CENTERX;
	private float RIGHT_CENTERY;
	
	private float BACK_RECT_SIZE;

	//左右两边摇杆白色背景坐标
	private float BackRectLeft,BackRectTop,BackRectRight,BackRectButtom;
    private float BackRectLeft2,BackRectTop2,BackRectRight2,BackRectButtom2;

	//摇杆的X,Y坐标以及摇杆的半径
	public float SmallRockerCircleX;
	public float SmallRockerCircleY;
	private float SmallRockerCircleR;
	
	//固定摇杆背景圆形的X,Y坐标以及半径
    private float RockerCircleX;
    private float RockerCircleY;
    private float RockerCircleR;
	private float RockerCircleX2;
	private float RockerCircleY2;
	private float RockerCircleR2;
    private RectF logoLocation;

	//摇杆的X,Y坐标以及摇杆的半径
	public float SmallRockerCircleX2;
	public float SmallRockerCircleY2;
	private float SmallRockerCircleR2;

	public float leftTouchStartX,leftTouchStartY,rightTouchStartX,rightTouchStartY;

	static final int YAW_STOP_CONTROL=0;
    public int altCtrlMode=0;
	
	public boolean leftTouching=false,rightTouching=false;
	private int leftTouchIndex=0,rightTouchIndex=0;

	public boolean touchReadyToSend=false;
	
	public MySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.i(TAG, "MySurfaceView");
        sfh = this.getHolder();
        sfh.addCallback(this);
        paint = new Paint();
        paint.setAntiAlias(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

	public void surfaceCreated(SurfaceHolder holder) { 
		th = new Thread(this);
		flag = true;
		th.start();
	}

	//Stick Size init
    //screenWidth为整个手机屏幕的像素宽度，例如华为Mate 7为1920
    //Height为摇杆外围正方形框的边长，默认为手机屏幕像素高度的一半，例如华为Mate 7的screenHeight为
    //1080，则Height值为1080/2=540
	private void stickSizeInit(int screenWidth,int Height)
	{
        LEFT_CENTERX = Height / 2;
        LEFT_CENTERY = Height / 2;
        RIGHT_CENTERX = screenWidth - LEFT_CENTERX;
        RIGHT_CENTERY = Height / 2;
        BACK_RECT_SIZE = Height / 2 - 20; //方形背景边长的一半

        //左边圆形摇杆的自动回中的X,Y坐标以及半径
        RockerCircleX = LEFT_CENTERX;
        RockerCircleY = LEFT_CENTERY;
        RockerCircleR = (float) ((BackRectRight - BackRectLeft) / 2 * 1.41421);

        //右边圆形摇杆的自动回中的X,Y坐标以及半径
        RockerCircleX2 = RIGHT_CENTERX;
        RockerCircleY2 = RIGHT_CENTERX;
        RockerCircleR2 = RockerCircleR;

        //左边方形背景的坐标
        BackRectLeft = LEFT_CENTERX - BACK_RECT_SIZE;
        BackRectTop = LEFT_CENTERY - BACK_RECT_SIZE;
        BackRectRight = LEFT_CENTERX + BACK_RECT_SIZE;
        BackRectButtom = LEFT_CENTERY + BACK_RECT_SIZE;

        //左边摇杆的X,Y坐标以及摇杆的半径
        SmallRockerCircleX = LEFT_CENTERX;
        SmallRockerCircleY = LEFT_CENTERY;
        SmallRockerCircleR = Height / 4;

        //右边方形背景的坐标
        BackRectLeft2 = RIGHT_CENTERX - BACK_RECT_SIZE;
        BackRectTop2 = RIGHT_CENTERY - BACK_RECT_SIZE;
        BackRectRight2 = RIGHT_CENTERX + BACK_RECT_SIZE;
        BackRectButtom2 = RIGHT_CENTERY + BACK_RECT_SIZE;

        //右边摇杆的X,Y坐标以及摇杆的半径
        SmallRockerCircleX2 = RIGHT_CENTERX;
        SmallRockerCircleY2 = RIGHT_CENTERY;
        SmallRockerCircleR2 = Height / 4;


        rightTouchStartX = RIGHT_CENTERX;
        rightTouchStartY = RIGHT_CENTERY;
        leftTouchStartX = LEFT_CENTERX;
        leftTouchStartY = LEFT_CENTERY;

        logoLocation = new RectF(screenWidth/2 - 150,BackRectButtom - 107,screenWidth/2 + 150,BackRectButtom);

    }
	/***
	 * 得到两点之线与x轴的弧度
	 */
	public double getRad(float px1, float py1, float px2, float py2) {
		//得到两点X的距离
		float x = px2 - px1;
		//得到两点Y的距离
		float y = py1 - py2;
		//算出斜边长
		float xie = (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
		//得到这个角度的余弦值（通过三角函数中的定理 ：邻边/斜边=角度余弦值）
		float cosAngle = x / xie;
		//通过反余弦定理获取到其角度的弧度
		float rad = (float) Math.acos(cosAngle);
		//注意：当触屏的位置Y坐标<摇杆的Y坐标我们要取反值-0~-180
		if (py2 < py1) {
			rad = -rad;
		}
		return rad;
	}

    //采用简化的方案来实现双摇杆控制
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            int pointNum = event.getPointerCount();

            float x1, y1, x2, y2;    //Touch Positon
            float leftX = 0, leftY = 0, rightX = 0, rightY = 0;

            final float DIVIDE_X = (LEFT_CENTERX + RIGHT_CENTERX) / 2;

		    /*Release touch*/
            switch ((event.getAction() & MotionEvent.ACTION_MASK)) {
                case MotionEvent.ACTION_UP:    //the last release
                    Log.v(TAG, "ACTION_UP");
                    Log.v(TAG, "PointNum:" + Integer.toString(event.getPointerCount()) + ",actionIndex:"
                            + Integer.toString(event.getActionIndex()));
                    Log.v(TAG, "X:" + Float.toString(event.getX()) + ";Y:" + Float.toString(event.getY()));

                    leftTouching = false;
                    rightTouching = false;

                    //放手归位
                    SmallRockerCircleX = LEFT_CENTERX;
                    SmallRockerCircleY = LEFT_CENTERY;

                    if (altCtrlMode == 1)    //定高爬升
                        SmallRockerCircleY = LEFT_CENTERY;

                    SmallRockerCircleX2 = RIGHT_CENTERX;
                    SmallRockerCircleY2 = RIGHT_CENTERY;

                    leftTouchStartX = LEFT_CENTERX;
                    if (altCtrlMode == 1) {
                        leftTouchStartY = LEFT_CENTERY;
                    }

                    rightTouchStartX = RIGHT_CENTERX;
                    rightTouchStartY = RIGHT_CENTERY;

                    break;
                case MotionEvent.ACTION_POINTER_UP://first release if two finger is touching
                    if (event.getX(event.getActionIndex()) < DIVIDE_X) {
                        leftTouching = false;
                        SmallRockerCircleX = LEFT_CENTERX;
                        if (altCtrlMode == 1)    //定高爬升
                            SmallRockerCircleY = LEFT_CENTERY;

                        leftTouchStartX = LEFT_CENTERX;
                        leftTouchStartY = LEFT_CENTERY;

                        rightTouchIndex = 0;
                    } else {
                        rightTouching = false;
                        SmallRockerCircleX2 = RIGHT_CENTERX;
                        SmallRockerCircleY2 = RIGHT_CENTERY;
                        rightTouchStartX = RIGHT_CENTERX;
                        rightTouchStartY = RIGHT_CENTERY;
                    }

                    Log.v(TAG, "ACTION_POINTER_UP");
                    Log.v(TAG, "PointNum:" + Integer.toString(event.getPointerCount()) + ",actionIndex:" + Integer.toString(event.getActionIndex()));
                    break;
            }

		    /*get touch*/
            if (event.getAction() == MotionEvent.ACTION_DOWN
                    || event.getAction() == MotionEvent.ACTION_MOVE
                    || (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN) {
                if (pointNum == 1) {
                    x1 = event.getX();
                    y1 = event.getY();
                    if (x1 < DIVIDE_X) {
                        leftX = x1;
                        leftY = y1;
                        if (leftTouching == false) {
                            leftTouchStartX = leftX;
                            leftTouchStartY = leftY;
                            leftTouching = true;
                        }

                    } else if (x1 >= DIVIDE_X) {
                        rightX = x1;
                        rightY = y1;
                        if (rightTouching == false) {
                            rightTouchStartX = rightX;
                            rightTouchStartY = rightY;
                            rightTouching = true;
                        }
                    }
                } else if (pointNum > 1) {
                    x1 = event.getX();
                    y1 = event.getY();
                    x2 = event.getX(1);
                    y2 = event.getY(1);
                    if (x1 < x2) {
                        if (x1 < DIVIDE_X) {
                            leftX = x1;
                            leftY = y1;
                            if (leftTouching == false) {
                                leftTouchStartX = leftX;
                                leftTouchStartY = leftY;
                                leftTouching = true;
                            }

                        }
                        if (x2 > DIVIDE_X) {
                            rightX = x2;
                            rightY = y2;
                            if (rightTouching == false) {
                                rightTouchStartX = rightX;
                                rightTouchStartY = rightY;
                                rightTouching = true;
                            }
                        }
                    } else {
                        if (x2 < DIVIDE_X) {
                            leftX = x2;
                            leftY = y2;
                            if (leftTouching == false) {
                                leftTouchStartX = leftX;
                                leftTouchStartY = leftY;
                                leftTouching = true;
                            }
                        }
                        if (x1 > DIVIDE_X) {
                            rightX = x1;
                            rightY = y1;
                            if (rightTouching == false) {
                                rightTouchStartX = rightX;
                                rightTouchStartY = rightY;
                                rightTouching = true;
                            }
                        }
                    }
                }

                /**Process movement**/
                if (leftTouching == true)//Left Stick is touched
                {
                    Log.v(TAG, "leftX: " + Float.toString(leftX) + "  leftY: " + Float.toString(leftY));
                    SmallRockerCircleX = leftX;
                    SmallRockerCircleY = leftY;

                    Log.v(TAG, Float.toString(SmallRockerCircleY) + " " + Float.toString(BackRectButtom));

                }
                //Right Stick is touched
                if (rightTouching == true) {
                    Log.v(TAG, "rightX: " + Float.toString(rightX) + "  rightY: " + Float.toString(rightY));

                    SmallRockerCircleX2 = rightX;
                    SmallRockerCircleY2 = rightY;

                    Log.v(TAG, Float.toString(SmallRockerCircleY2) + " " + Float.toString(BackRectButtom2));
                }
            }

            //coordinate of the center of left and right joystick (x,y)
            //左右摇杆中心点坐标(x,y)
            Log.v(TAG, "left(x):"+Integer.toString((int) leftTouchStartX) + " left(y):"
                    + Integer.toString((int) leftTouchStartY) + " right(x):"
                    + Integer.toString((int) rightTouchStartX) + " right(y):"
                    + Integer.toString((int) rightTouchStartY));

            if (YAW_STOP_CONTROL == 1)
                SmallRockerCircleX = LEFT_CENTERX;    //暂不控制yaw，避免控油门时误点乱转

            //落手为起点,油门除外
            if (altCtrlMode == 0)
                Protocol.throttle = (int) (1000 + 1000 * (BackRectButtom - SmallRockerCircleY) / (BackRectButtom - BackRectTop));
            else
                Protocol.throttle = (int) (1500 - 1000 * (SmallRockerCircleY - leftTouchStartY) / (BackRectButtom - BackRectTop));
            Protocol.yaw = (int) (1500 + 1000 * ((SmallRockerCircleX - leftTouchStartX)) / (BackRectRight - BackRectLeft));
            Protocol.pitch = (int) (1500 + 1000 * (0 - (SmallRockerCircleY2 - rightTouchStartY)) / (BackRectButtom2 - BackRectTop2));
            Protocol.roll = (int) (1500 + 1000 * ((SmallRockerCircleX2 - rightTouchStartX)) / (BackRectRight2 - BackRectLeft2));

            Protocol.throttle = constrainRange(Protocol.throttle, 1000, 2000);
            Protocol.yaw = constrainRange(Protocol.yaw, 1000, 2000);
            Protocol.pitch = constrainRange(Protocol.pitch, 1000, 2000);
            Protocol.roll = constrainRange(Protocol.roll, 1000, 2000);

            Log.i(TAG, "yaw: " + Integer.toString(Protocol.yaw)
                    + " trottle: " + Integer.toString(Protocol.throttle)
                    + " pitch: " + Integer.toString(Protocol.pitch)
                    + " roll: " + Integer.toString(Protocol.roll));

            touchReadyToSend = true;


        } catch (Exception e) {//stick turn out error
            Log.e("stickError", "stickError");
            SmallRockerCircleX = LEFT_CENTERX;
            SmallRockerCircleY = LEFT_CENTERY;
            SmallRockerCircleX2 = RIGHT_CENTERX;
            SmallRockerCircleY2 = RIGHT_CENTERY;
            leftTouching = false;
            rightTouching = false;
            leftTouchIndex = 0;
            rightTouchIndex = 0;
        }
        return true;
    }

    public int constrainRange(int x,int min,int max)
	{
		if(x<min) x=min;
		if(x>max) x=max;
		
		return x;
		
	}
	
	public int rc2StickPosY(int rc)
	{
		int posY=0;
		posY=(int)(BackRectButtom-(BackRectButtom-BackRectTop)*(rc-1000)/1000.0);
		return posY;
	}
	/**
	 * 
	 * @param R
	 *            圆周运动的旋转点
	 * @param centerX
	 *            旋转点X
	 * @param centerY
	 *            旋转点Y
	 * @param rad
	 *            旋转的弧度
	 */
	public void getXY(float centerX, float centerY, float R, double rad) {
		//获取圆周运动的X坐标 
		SmallRockerCircleX = (float) (R * Math.cos(rad)) + centerX;
		//获取圆周运动的Y坐标
		SmallRockerCircleY = (float) (R * Math.sin(rad)) + centerY;
	}
	
	

	public void draw() {
		try {
			canvas = sfh.lockCanvas();
			canvas.drawColor(Color.BLACK);
			//设置颜色
			//绘制摇杆背景
			paint.setColor(Color.WHITE);
			canvas.drawRect(BackRectLeft,BackRectTop,BackRectRight,BackRectButtom,paint);///
			//绘制摇杆
			paint.setColor(0x4F94CD00); 
			canvas.drawCircle(SmallRockerCircleX, SmallRockerCircleY, SmallRockerCircleR, paint);

			//Draw another Right one
			paint.setColor(Color.WHITE); 
			canvas.drawRect(BackRectLeft2,BackRectTop2,BackRectRight2,BackRectButtom2,paint);///
			paint.setColor(0x4F94CD00);
			canvas.drawCircle(SmallRockerCircleX2, SmallRockerCircleY2, SmallRockerCircleR2, paint);

            //绘制背景
            Bitmap back = BitmapFactory.decodeResource(this.getResources(), R.drawable.logo);
            canvas.drawBitmap(back, null,logoLocation, null);


        } catch (Exception e) {
			// TODO: handle exception
		} finally {
			try {
				if (canvas != null)
					sfh.unlockCanvasAndPost(canvas);
			} catch (Exception e2) {

			}
		}
	}

    //线程的run操作，当surface被创建后，线程开启
	public void run() {
		// TODO Auto-generated method stub
		//
		while (flag) {	
			draw();
			try {
				Thread.sleep(50);	//线程休眠50ms
			} catch (Exception ex) {
			}
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.v("Himi", "surfaceChanged");

        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        int WMwidth = wm.getDefaultDisplay().getWidth();
        int WMheight = wm.getDefaultDisplay().getHeight();

        Log.v("viewSize","height:"+ WMheight + "  Width:"+WMwidth);

        stickSizeInit(WMwidth,WMheight/2);	//Obain the height of this view
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		flag = false;
		Log.v("Himi", "surfaceDestroyed");
	}
}