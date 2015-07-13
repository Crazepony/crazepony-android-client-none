/* Project: App Controller for copter 4(including Crazepony)
 * Author: 	Huang Yong xiang 
 * Brief:	This is an open source project under the terms of the GNU General Public License v3.0
 * TOBE FIXED:  1. disconnect and connect fail with Bluetooth due to running thread 
 * 				2. Stick controller should be drawn in dpi instead of pixel.  
 * 
 * */

package com.test.BTClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.test.BTClient.DeviceListActivity; 
import com.test.BTClient.MySurfaceView;

import android.R.bool;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
//import android.view.Menu;            //如使用菜单加入此三包
//import android.view.MenuInflater;
//import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
 
import com.test.BTClient.*;

@SuppressLint("NewApi")
public class BTClient extends Activity {

	private final static int REQUEST_CONNECT_DEVICE = 1; // 宏定义查询设备句柄

	private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB"; // SPP服务UUID号
	//
	private final static int UPDATE_MUV_STATE_PERIOD=200;
	
	
	List<WayPoint> wpRoute;	//规划路线
//	private WayPoint[] wpArr=new WayPoint[3]; 
	
	//
	private InputStream is; // 输入流，用来接收蓝牙数据
	//private TextView text0; //提示栏解句柄
	private EditText edit0; // 发送数据输入句柄
	private TextView dis; // 接收数据显示区
	private ScrollView sv; // 翻页句柄，滚动条
	private String smsg = ""; // 显示用数据缓存
	private String fmsg = ""; // 保存用数据缓存
	 
	private TextView throttleText,yawText,pitchText,rollText;
	private TextView pitchAngText,rollAngText,yawAngText,altText,GPSFixText,homeFixText,distanceText,voltageText;
	private Button armButton,lauchLandButton,headFreeButton,altHoldButton,accCaliButton;
	private ArrayAdapter<String> adapter;
//	private WayPoint wp1=new WayPoint("绿地", 45443993,126373228); 
 	private Navigation nav;
 	private boolean navFirstStart=false;
	//private newButtonView newButton;

	public String filename = ""; // 用来保存存储的文件名
	BluetoothDevice _device = null; // 蓝牙设备
	BluetoothSocket _socket = null; // 蓝牙通信socket
	boolean _discoveryFinished = false;
	boolean bRun = true;
	boolean bThread = false;
	//
	boolean lauchFlag=false;
	long timeNew, timePre=0;
	//
	private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter(); // 获取本地蓝牙适配器，即蓝牙设备

	private MySurfaceView stickView; 
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main); // 设置画面为主画面 main.xml 
		//显示 text
		throttleText = (TextView)findViewById(R.id.throttleText); // 
		yawText = (TextView)findViewById(R.id.yawText);
		pitchText = (TextView)findViewById(R.id.pitchText);
		rollText = (TextView)findViewById(R.id.rollText);
		//pitchAngText,rollAngText,yawAngText,altText,GPSFixText,homeFixText,voltageText
		pitchAngText = (TextView)findViewById(R.id.pitchAngText);
		rollAngText = (TextView)findViewById(R.id.rollAngText);
		yawAngText = (TextView)findViewById(R.id.yawAngText);
		altText = (TextView)findViewById(R.id.altText);
		GPSFixText = (TextView)findViewById(R.id.GPSFixText);
		homeFixText = (TextView)findViewById(R.id.homeFixText);
		voltageText = (TextView)findViewById(R.id.voltageText);
		distanceText= (TextView)findViewById(R.id.distanceText);
		 
		edit0 = (EditText) findViewById(R.id.Edit0); // 得到输入框句柄
		sv = (ScrollView) findViewById(R.id.ScrollView01); // 得到翻页句柄
		dis = (TextView) findViewById(R.id.in); // 得到数据显示句柄
		//摇杆
		stickView=(MySurfaceView)findViewById(R.id.stickView);
		//按钮
		armButton=(Button)findViewById(R.id.armButton);
		lauchLandButton=(Button)findViewById(R.id.lauchLandButton);
		headFreeButton=(Button)findViewById(R.id.headFreeButton);
		altHoldButton=(Button)findViewById(R.id.altHoldButton);
		accCaliButton=(Button)findViewById(R.id.accCaliButton);
		//GPS 导航
	 	nav=new Navigation();
		
	//	wp1.setWP("AA", 2, 3);
		//ReadThread.start(); 
		//Log.v("run",(stickView.touchReadyToSend?"TURE":"FALSE")); 
		// 如果打开本地蓝牙设备不成功，提示信息，结束程序
		if (_bluetooth == null) {
			Toast.makeText(this, "无法打开手机蓝牙，请确认手机是否有蓝牙功能！", Toast.LENGTH_LONG)
					.show();
			finish();
			return;
		} 
		// 设置设备可以被搜索
		new Thread() {
			public void run() {
				if (_bluetooth.isEnabled() == false) {
					_bluetooth.enable();
				}
			}
		}.start();
	}
	@Override
	public void onPause()
	{
		navFirstStart=false;
		super.onPause(); 
	}
	@Override
	public void onStop()
	{
		navFirstStart=false;
		super.onStop(); 
	}

	public void onSendArmButtonClicked(View v)
	{
		Button btnConnect=(Button) findViewById(R.id.Button03);
        String disconnect = getResources().getString(R.string.Disconnect);
        String arm = getResources().getString(R.string.Arm);
        String unarm = getResources().getString(R.string.Unarm);
        String disconnectToast = getResources().getString(R.string.DisconnectToast);

		if(btnConnect.getText() == disconnect){
			if(armButton.getText() != arm)	{
				btSendBytes(Protocol.getSendData(Protocol.ARM_IT, Protocol.getCommandData(Protocol.ARM_IT)));
				armButton.setText(arm);
			}else{
				btSendBytes(Protocol.getSendData(Protocol.DISARM_IT, Protocol.getCommandData(Protocol.DISARM_IT)));
				armButton.setText(unarm);
			}
		}else {
            Toast.makeText(this, disconnectToast, Toast.LENGTH_SHORT).show();
        }
	} 
	
	//Take off , land down
	public void onlauchLandButtonClicked(View v)
	{
        Button btnConnect=(Button) findViewById(R.id.Button03);
        String disconnect = getResources().getString(R.string.Disconnect);
        String launch  = getResources().getString(R.string.Launch);
        String land = getResources().getString(R.string.Land);
        String disconnectToast = getResources().getString(R.string.DisconnectToast);

        if(btnConnect.getText() == disconnect){
            if(lauchLandButton.getText() != land){
                btSendBytes(Protocol.getSendData(Protocol.LAUCH, Protocol.getCommandData(Protocol.LAUCH)));
                lauchLandButton.setText(land);
                Protocol.throttle=Protocol.LAUCH_THROTTLE;
                stickView.SmallRockerCircleY=stickView.rc2StickPosY(Protocol.throttle);
                stickView.touchReadyToSend=true;
            }else{
                btSendBytes(Protocol.getSendData(Protocol.LAND_DOWN, Protocol.getCommandData(Protocol.LAND_DOWN)));
                lauchLandButton.setText(launch);
                Protocol.throttle=Protocol.LAND_THROTTLE;
                stickView.SmallRockerCircleY=stickView.rc2StickPosY(Protocol.throttle);
                stickView.touchReadyToSend=true;
            }
        }else {
            Toast.makeText(this, disconnectToast, Toast.LENGTH_SHORT).show();
        }
	}

	//无头模式键
	public void onheadFreeButtonClicked(View v)
	{
		Button btnConnect=(Button) findViewById(R.id.Button03);
        String disconnect = getResources().getString(R.string.Disconnect);
        String disconnectToast = getResources().getString(R.string.DisconnectToast);

		if(btnConnect.getText() == disconnect){
			if(headFreeButton.getCurrentTextColor()!=Color.GREEN)
			{	btSendBytes(Protocol.getSendData(Protocol.HEAD_FREE, Protocol.getCommandData(Protocol.HEAD_FREE)));
				headFreeButton.setTextColor(Color.GREEN);
			}else{
				btSendBytes(Protocol.getSendData(Protocol.STOP_HEAD_FREE, Protocol.getCommandData(Protocol.STOP_HEAD_FREE)));
				headFreeButton.setTextColor(Color.WHITE);
			}
		}else {
            Toast.makeText(this, disconnectToast, Toast.LENGTH_SHORT).show();
        }
		
	}

	//定高键
	public void onaltHoldButtonClicked(View v)
	{
		Button btnConnect=(Button) findViewById(R.id.Button03);
        String disconnect = getResources().getString(R.string.Disconnect);
        String disconnectToast = getResources().getString(R.string.DisconnectToast);

		if(btnConnect.getText() == disconnect){
			if( altHoldButton.getCurrentTextColor()!=Color.GREEN )
			{	//定高定点都开
				btSendBytes(Protocol.getSendData(Protocol.HOLD_ALT, Protocol.getCommandData(Protocol.HOLD_ALT))); 
				altHoldButton.setTextColor(Color.GREEN);
				stickView.altCtrlMode=1;
			}else{
				btSendBytes(Protocol.getSendData(Protocol.STOP_HOLD_ALT, Protocol.getCommandData(Protocol.STOP_HOLD_ALT)));
				altHoldButton.setTextColor(Color.WHITE); 
				stickView.altCtrlMode=0;
			}
		}else {
            Toast.makeText(this, disconnectToast, Toast.LENGTH_SHORT).show();
        }
	}
	
	//校准
	public void onAccCaliButtonClicked(View v)
	{
		Button btnConnect=(Button) findViewById(R.id.Button03);
        String disconnect = getResources().getString(R.string.Disconnect);
        String disconnectToast = getResources().getString(R.string.DisconnectToast);

		if(btnConnect.getText() == disconnect){
			btSendBytes(Protocol.getSendData(Protocol.MSP_ACC_CALIBRATION, Protocol.getCommandData(Protocol.MSP_ACC_CALIBRATION)));
		}else {
            Toast.makeText(this, disconnectToast, Toast.LENGTH_SHORT).show();
        }
	}

	public void btSendBytes(byte[] data)
	{
		Button btnConnect=(Button) findViewById(R.id.Button03);
        String disconnect = getResources().getString(R.string.Disconnect);
        String disconnectToast = getResources().getString(R.string.DisconnectToast);

		if(btnConnect.getText() == disconnect)//当已经连接上时才发送
		{
			try {
				OutputStream os = _socket.getOutputStream(); // 蓝牙连接输出流
				os.write(data);
			} 
			catch (IOException e) {
			//	Toast.makeText(this, "发送失改", Toast.LENGTH_SHORT).show();
			} 
			catch (Exception e){
			//	Toast.makeText(this, "发送失改", Toast.LENGTH_SHORT).show();
			}
		}else {
            Toast.makeText(this,disconnectToast, Toast.LENGTH_SHORT).show();
        }
	}
	
	// 接收活动结果，响应startActivityForResult()
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

        String connectFailToast = getResources().getString(R.string.ConnectFailToast);
        String connectSuccessToast = getResources().getString(R.string.ConnectSuccessToast);
        String disconnect = getResources().getString(R.string.Disconnect);

		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
		    // 连接结果，由DeviceListActivity设置返回
			// 响应返回结果

			if (resultCode == Activity.RESULT_OK){
			    // 连接成功，由DeviceListActivity设置返回
				// MAC地址，由DeviceListActivity设置返回
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// 得到蓝牙设备句柄
				_device = _bluetooth.getRemoteDevice(address);

				// 用服务号得到socket
				try {
					_socket = _device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
				} catch (IOException e) {
					Toast.makeText(this, connectFailToast, Toast.LENGTH_SHORT).show();
				}
				// 连接socket
				Button btn = (Button) findViewById(R.id.Button03);
				try {
					//successfully Connected
					_socket.connect();
					Toast.makeText(this,connectSuccessToast,Toast.LENGTH_SHORT).show();
					btn.setText(disconnect);
				} catch (IOException e) {
					try {
						Toast.makeText(this,connectFailToast, Toast.LENGTH_SHORT).show();
						_socket.close();
						_socket = null;
					} catch (IOException ee) {
						Toast.makeText(this,connectFailToast, Toast.LENGTH_SHORT)	.show();
					}

					return;
				} 
				// 打开接收线程
				try {
					is = _socket.getInputStream(); // 得到蓝牙数据输入流
				} catch (IOException e) {
					Toast.makeText(this, "接收数据失败！", Toast.LENGTH_SHORT).show();
					return;
				}
				
				if (bThread == false) {
					Log.v("run","ThreadStart");
					ReadThread.start();
					bThread = true;
					
				} else {
					bRun = true;
				}
			}
			break;
		default:
			break;
		}
	}
	
	// 接收数据线程，（Android里，不要直接在Thread里更新UI，要更新UI，应该进入消息队列）
	Thread ReadThread = new Thread() {
		
		public void run() { 
			int num = 0,totNum=0;
			long time1=0,time2=0; 
			byte[] buffer_new = new byte[1024];
			byte[] buffer = new byte[1024]; 
			List<Byte> totBuffer=new LinkedList<Byte>();
			int reCmd=-2;
			int i = 0;
			int n = 0;
			bRun = true;
			// 接收线程
			while (true) {
				try {
					while (is.available() == 0) {//wait for BT rec Data
						while (bRun == false) { 
						//	Log.v("run","bRunFalse");
						} 
					//	Log.v("run","runing1");
						//遥控发送分支线程
						if(stickView.touchReadyToSend==true)// process stick movement
						{
							
							btSendBytes(Protocol.getSendData(Protocol.SET_4CON, Protocol.getCommandData(Protocol.SET_4CON))); 
//								System.out.println("Thro: " + a+"," +Protocol.outputData[0]+ ","+ Protocol.outputData[3] +","+ Protocol.outputData[4]);
					
							Message msg=handler.obtainMessage();
							msg.arg1=2;
							handler.sendMessage(msg);
							stickView.touchReadyToSend=false;
						} 
						
						timeNew=SystemClock.uptimeMillis();	//系统运行到此的时间
						if(timeNew-timePre>UPDATE_MUV_STATE_PERIOD)
						{
							timePre=timeNew;
						 	btSendBytes(Protocol.getSendData(Protocol.FLY_STATE, Protocol.getCommandData(Protocol.FLY_STATE))); 

						}
					}
					//有数据过来
					time1=time2=SystemClock.uptimeMillis();
					boolean frameRec=false;
					while (!frameRec) {
						num = is.read(buffer); // 读入数据（流），buffer有num个字节 
					 	n = 0; 
					 	
					 	String s0 = new String(buffer, 0, num);
						fmsg += s0; // 保存收到数据
						for (i = 0; i < num; i++) {
							if ((buffer[i] == 0x0d) && (buffer[i + 1] == 0x0a)) {
								buffer_new[n] = 0x0a;
								i++;
							} else {
								buffer_new[n] = buffer[i];
							}
							n++;
						}
						String s = new String(buffer_new, 0, n);
						smsg += s; // 写入接收缓存 
						reCmd=Protocol.processDataIn( buffer,num);
					/*	for(int j=0;j<num;j++)
							totBuffer.add(buffer[j]);
					 	totNum+=num; 
						while(is.available()==0 && !frameRec)//wait for more data in 5ms
						{ 
							time1=SystemClock.uptimeMillis();
							if(time1-time2>30)	//5ms间隔 内认为连续 
								frameRec=true;  
						}
						time2=time1;  */
						   if (is.available() == 0)	 
							   frameRec=true; // 短时间没有数据才跳出进行显示
					} 

					totNum=0;
					if(reCmd>=0)
					{
						Message msg=handler.obtainMessage();
						msg.arg1=reCmd;//
						handler.sendMessage(msg); 
					}
					// 消息添加到handler的消息队列，handler接到后，进行操作：显示刷新等 
//					Message msg=handler.obtainMessage();
//					msg.arg1=3;//
//					handler.sendMessage(msg);
				} catch (IOException e) {
				}
			}
		}
	};
	//------------------Handler---------------------//
	//创建一个消息处理队列，主要是用于线程通知到此，以 刷新UI
	Handler handler = new Handler() {
		//显示接收到的数据
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if(msg.arg1==1)
			{
				dis.setText(smsg); 
				sv.scrollTo(0, dis.getMeasuredHeight()); // 跳至数据最后一页
			}
			else if(msg.arg1==2)
			{
				throttleText.setText("Throttle:"+Integer.toString(Protocol.throttle));
				yawText.setText("Yaw:"+Integer.toString(Protocol.yaw));
				pitchText.setText("Pitch:"+Integer.toString(Protocol.pitch));
				rollText.setText("Roll:"+Integer.toString(Protocol.roll));
			}
			else if(msg.arg1==3)//更新状态
			{
		//		 throttleText.setText(throttleText.getText()+"1");
			//	byte temp[]={1,2};
	
			}
			else if(msg.arg1==Protocol.FLY_STATE)
			{
			//	throttleText.setText(throttleText.getText()+"1");
			 	pitchAngText.setText("Pitch Ang: "+Protocol.pitchAng);
			 	rollAngText.setText("Roll Ang: "+Protocol.rollAng);
			 	yawAngText.setText("Yaw Ang: "+Protocol.yawAng);
			 	altText.setText("Alt:"+Protocol.alt + "m");
			 	if(Protocol.GPSFix!=0) 
			 	{
			 		GPSFixText.setTextColor(Color.GREEN); 
			 		GPSFixText.setText("GPS Fixed:"+Protocol.staNum);
			 	}
			 	else  
			 	{
			 		GPSFixText.setTextColor(Color.RED);
			 		GPSFixText.setText("GPS Not Fix:"+Protocol.staNum);
			 	}
			 	if(Protocol.GPSFixHome!=0)
			 	{
			 		homeFixText.setTextColor(Color.GREEN); 
			 		homeFixText.setText("Home Not Fix");
			 	}
			 	else
			 	{
			 		homeFixText.setTextColor(Color.RED); 
			 		homeFixText.setText("Home Fixed");
			 	}
			 	voltageText.setText("Voltage:"+Protocol.voltage + " V");
			 	distanceText.setText("speedZ:"+Protocol.speedZ + "m/s");
			}
		}
	};

	// 关闭程序掉用处理部分
	public void onDestroy() {
		super.onDestroy();
		if (_socket != null) // 关闭连接socket
			try {
				_socket.close();
			} catch (IOException e) {
			}
		  _bluetooth.disable(); //关闭蓝牙服务
	}

	// 连接按键响应函数
	public void onConnectButtonClicked(View v) {

        String openBT = getResources().getString(R.string.OpenBT);
        String disconnectToast = getResources().getString(R.string.DisconnectToast);
        String connect = getResources().getString(R.string.Connect);

		if (_bluetooth.isEnabled() == false) { // 如果蓝牙服务不可用则提示
			Toast.makeText(this,openBT, Toast.LENGTH_LONG).show();
			return;
		}

		// 如未连接设备则打开DeviceListActivity进行设备搜索
		Button btn = (Button) findViewById(R.id.Button03);
		if (_socket == null) {
			Intent serverIntent = new Intent(this, DeviceListActivity.class); // 跳转程序设置
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE); // 设置返回宏定义
		} else {	//已连接上，断开
			// 关闭连接socket
			try {
				is.close();
				_socket.close();
				_socket = null;
				bRun = false;
				btn.setText(connect);
			} catch (IOException e) {
			}
		}
		return;
	}

	// 保存按键响应函数
	public void onSaveButtonClicked(View v) {
		Save();
	}

	// 清除按键响应函数
	public void onClearButtonClicked(View v) {
		smsg = "";
		fmsg = "";
		dis.setText(smsg);
		return;
	}

	// 退出按键响应函数
	public void onQuitButtonClicked(View v) {
		finish();
	}

	// 保存功能实现
	private void Save() {
		// 显示对话框输入文件名
		LayoutInflater factory = LayoutInflater.from(BTClient.this); // 图层模板生成器句柄
		final View DialogView = factory.inflate(R.layout.sname, null); // 用sname.xml模板生成视图模板
		new AlertDialog.Builder(BTClient.this).setTitle("文件名")
				.setView(DialogView) // 设置视图模板
				.setPositiveButton("确定", new DialogInterface.OnClickListener() // 确定按键响应函数
						{
							public void onClick(DialogInterface dialog,
									int whichButton) {
								EditText text1 = (EditText) DialogView
										.findViewById(R.id.sname); // 得到文件名输入框句柄
								filename = text1.getText().toString(); // 得到文件名

								try {
									if (Environment.getExternalStorageState()
											.equals(Environment.MEDIA_MOUNTED)) { // 如果SD卡已准备好

										filename = filename + ".txt"; // 在文件名末尾加上.txt
										File sdCardDir = Environment
												.getExternalStorageDirectory(); // 得到SD卡根目录
										File BuildDir = new File(sdCardDir,
												"/data"); // 打开data目录，如不存在则生成
										if (BuildDir.exists() == false)
											BuildDir.mkdirs();
										File saveFile = new File(BuildDir,
												filename); // 新建文件句柄，如已存在仍新建文档
										FileOutputStream stream = new FileOutputStream(
												saveFile); // 打开文件输入流
										stream.write(fmsg.getBytes());
										stream.close();
										Toast.makeText(BTClient.this, "存储成功！",
												Toast.LENGTH_SHORT).show();
									} else {
										Toast.makeText(BTClient.this, "没有存储卡！",
												Toast.LENGTH_LONG).show();
									}

								} catch (IOException e) {
									return;
								}

							}
						}).setNegativeButton("取消", // 取消按键响应函数,直接退出对话框不做任何处理
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
							}
						}).show(); // 显示对话框
	}
	
	
}