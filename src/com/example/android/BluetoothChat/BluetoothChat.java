/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.BluetoothChat;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.UUID;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Int2;
import android.text.Html;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
/**
 * 基于ARM的四轴飞行器 科研立项 安卓遥控端程序
 *                山东大学(威海)  
 */
@SuppressLint("NewApi")
public class BluetoothChat extends Activity implements SensorEventListener {
	// 调试
	private long exitTime = 0;
	private SeekBar seekBar = null;
	private static final String TAG = "BluetoothChat";
	private static boolean D = true;
	private static final String info = "junge";
	// 类型的消息发送从bluetoothchatservice处理程序
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
    public static final String BluetoothData = "fullscreen";
	public String filename = ""; // 用来保存存储的文件名
	private String newCode = "";
	private String newCode2 = "";
	private String fmsg = ""; // 保存用数据缓存
	// 键名字从收到的bluetoothchatservice处理程序
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	// 

	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	// Intent需要 编码
	public static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	// 布局控件
	private TextView mTitle;
	private EditText mInputEditText;
	private EditText mOutEditText;
	private EditText mOutEditText2;
	private Button mSendButton;
	private CheckBox HEXCheckBox;
	private Button breakButton;
	private CheckBox checkBox_sixteen;
	

	
	// 名字的连接装置
	private String mConnectedDeviceName = null;
	// 传出消息的字符串缓冲区
	private StringBuffer mOutStringBuffer;
	// 当地的蓝牙适配器
	private BluetoothAdapter mBluetoothAdapter = null;
	// 成员对象的聊天服务
	private BluetoothChatService mChatService = null;
	// 设置标识符，选择用户接受的数据格式
	private boolean dialogs;
	
    //第一次输入加入-->变量
	private int sum =1;
	private int UTF =1;
	

	// 名社民党记录当创建服务器套接字
	String mmsg = "";
	String mmsg2 = "";

	//声明控制指令
	private byte[] getCtr =new byte[32];
	private TextView valThrottle = null;
	private int count;
	private boolean isUnlock=true;
	
	//声明控制控件
	private Button btnUnlock = null;
	private Button btnStop= null;
	private Button btnRead= null;
	private Button btnSave= null;
	//
	private TextView rc_roll=null;
	private TextView rc_pit=null;
	  private SensorManager mSensorManager;  
	    private Sensor mSensor;  
	    private float mX, mY, mZ;  
	    private long lasttimestamp = 0;  
	    Calendar mCalendar;
	 private int THROTTLE;
	 private int connect;
	 private int roll;
	 private int pitch;
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
       
		D = false;
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");
		Log.i(info, "" + dialogs);
		// 设置窗口布局
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				
				R.layout.custom_title);
		//实例化控制控件
		btnRead=(Button)findViewById(R.id.btn_read);
		btnSave=(Button)findViewById(R.id.btn_save);
		
		btnUnlock=(Button)findViewById(R.id.btn_unlock);
	
		rc_roll=(TextView)findViewById(R.id.rc_roll);
		rc_pit=(TextView)findViewById(R.id.rc_pitch);
		valThrottle=(TextView)findViewById(R.id.text_val);
		seekBar = (SeekBar) findViewById(R.id.throttle);
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);  
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);// TYPE_GRAVITY  
        if (null == mSensorManager) {  
            Log.d(TAG, "deveice not support SensorManager");  
        }  
        // 参数三，检测的精准度  
        mSensorManager.registerListener(this, mSensor,  
                SensorManager.SENSOR_DELAY_NORMAL);// SENSOR_DELAY_GAME  
		btnUnlock.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if(isUnlock==true){
					btnUnlock.setText("油门加锁");				
					isUnlock=false;
					getCtr[0]=(byte) 0xAA;
					getCtr[1]=(byte) 0xBB;
					getCtr[2]=(byte) 0;
					getCtr[3]=(byte) 0;
					getCtr[4]=(byte) 50;
					getCtr[5]=(byte) 50;
					getCtr[6]=(byte) (50);
					for(int j=7;j<30;j++)getCtr[j]=0;
					getCtr[31]=(byte) 0xA5;
					sendHEX(getCtr);
				}
				else{
					btnUnlock.setText("油门解锁");
					isUnlock=true;
					getCtr[0]=(byte) 0xAA;
					getCtr[1]=(byte) 0xBB;
					getCtr[2]=(byte) 0;
					getCtr[3]=(byte) 0;
					getCtr[4]=(byte) 50;
					getCtr[5]=(byte) 50;
					getCtr[6]=(byte) (50);
					for(int j=7;j<30;j++)getCtr[j]=0;
					getCtr[31]=(byte) 0x00;
					sendHEX(getCtr);
					seekBar.setProgress(0);
				}
			}
			
		});
		btnRead.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Uri uri = Uri.parse("http://www.crazepony.com");  
				Intent it = new Intent(Intent.ACTION_VIEW, uri);  
				startActivity(it);
			}
		
		});
		btnSave.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				if((System.currentTimeMillis()-exitTime) > 2000){  
		            Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();                                
		            exitTime = System.currentTimeMillis();   
		        } else {
		            finish();
		            System.exit(0);
		        }
			}
		
		});
	
        //设置该进度条的最大值,默认情况下为O
        seekBar.setMax(1000);
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
        	
			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				// TODO Auto-generated method stub
				connect=1;
				THROTTLE=seekBar.getProgress();
				valThrottle.setText("油门:\n"+THROTTLE);
				
				if(THROTTLE>50){
					getCtr[0]=(byte) 0xAA;
					getCtr[1]=(byte) 0xBB;
					getCtr[2]=(byte) ((byte)(THROTTLE&0xff));
					getCtr[3]=(byte) ((byte)(THROTTLE>>8));
					getCtr[4]=(byte) pitch;
					getCtr[5]=(byte) roll;
					getCtr[6]=(byte) 50;
					for(int j=7;j<30;j++)getCtr[j]=0;
					getCtr[31]=(byte) 0xA5;
				}
				else{
					getCtr[0]=(byte) 0xAA;
					getCtr[1]=(byte) 0xBB;
					getCtr[2]=(byte) ((byte)(THROTTLE&0xff));
					getCtr[3]=(byte) ((byte)(THROTTLE>>8));
					getCtr[4]=(byte) pitch;
					getCtr[5]=(byte) roll;
					getCtr[6]=(byte) 50;
					for(int j=7;j<30;j++)getCtr[j]=0;
					getCtr[31]=(byte) 0x00;
				}
			//	if(THROTTLE<=20)getCtr[31]=(byte) 0x00;
				
				
			
				if(count>=10){//不要发太多，连续十次滑动发一次遥控数据
					sendHEX(getCtr);
					count=0;
				}
				else{
					count++;
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				THROTTLE=seekBar.getProgress();
				valThrottle.setText("油门:\n"+THROTTLE);
				
				if(THROTTLE>50){
				getCtr[0]=(byte) 0xAA;
				getCtr[1]=(byte) 0xBB;
				getCtr[2]=(byte) ((byte)(THROTTLE&0xff));
				getCtr[3]=(byte) ((byte)(THROTTLE>>8));
				getCtr[4]=(byte) pitch;
				getCtr[5]=(byte) roll;
				getCtr[6]=(byte) (50);
				for(int j=7;j<30;j++)getCtr[j]=0;
				getCtr[31]=(byte) 0xA5;}
				else{
					getCtr[0]=(byte) 0xAA;
					getCtr[1]=(byte) 0xBB;
					getCtr[2]=(byte) ((byte)(THROTTLE&0xff));
					getCtr[3]=(byte) ((byte)(THROTTLE>>8));
					getCtr[4]=(byte) pitch;
					getCtr[5]=(byte) roll;
					getCtr[6]=(byte) (50);
					for(int j=7;j<30;j++)getCtr[j]=0;
					getCtr[31]=(byte) 0x00;}
				
				sendHEX(getCtr);
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				seekBar.getProgress();
				THROTTLE=seekBar.getProgress();
				valThrottle.setText("油门:\n"+THROTTLE);
				if(THROTTLE>50){
					getCtr[0]=(byte) 0xAA;
					getCtr[1]=(byte) 0xBB;
					getCtr[2]=(byte) ((byte)(THROTTLE&0xff));
					getCtr[3]=(byte) ((byte)(THROTTLE>>8));
					getCtr[4]=(byte) pitch;
					getCtr[5]=(byte) roll;
					getCtr[6]=(byte) (50);
					for(int j=7;j<30;j++)getCtr[j]=0;
					getCtr[31]=(byte) 0xA5;
				}else{
					getCtr[0]=(byte) 0xAA;
					getCtr[1]=(byte) 0xBB;
					getCtr[2]=(byte) ((byte)(THROTTLE&0xff));
					getCtr[3]=(byte) ((byte)(THROTTLE>>8));
					getCtr[4]=(byte) pitch;
					getCtr[5]=(byte) roll;
					getCtr[6]=(byte) (50);
					for(int j=7;j<30;j++)getCtr[j]=0;
					getCtr[31]=(byte) 0x00;
				}
				//if(THROTTLE==0)getCtr[31]=(byte) 0x00;
				sendHEX(getCtr);
			}
        });
        
		mInputEditText = (EditText) findViewById(R.id.editText1);
		mInputEditText.setGravity(Gravity.TOP);
		mInputEditText.setSelection(mInputEditText.getText().length(),
		mInputEditText.getText().length());
		mInputEditText.clearFocus();
		mInputEditText.setFocusable(false);
		//设置ImageView
		
		
		// 设置文本的标题
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText(R.string.app_name);
		mTitle = (TextView) findViewById(R.id.title_right_text);
		// 初始化Radiobutton]
		HEXCheckBox = (CheckBox) findViewById(R.id.radioMale);
		breakButton = (Button) findViewById(R.id.button_break);
		// 得到当地的蓝牙适配器
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// 初始化CheckBox
		checkBox_sixteen = (CheckBox) findViewById(R.id.checkBox_sixteen);
		//点击图片跳转到公司页面
		

		 if(getWindow().getAttributes().softInputMode==WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED)

		 {

		   //隐藏软键盘

		   getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

		 }


		// 初始化Socket
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_LONG)
					.show();
			finish();
			return;
		}
	    checkBox_sixteen
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
//						String getValue = mInputEditText.getText().toString();
//						if (isChecked) {
//							mInputEditText.setText(CodeFormat.stringToHex(getValue));
//							
//						} else {
//							mInputEditText.setText(fmsg);
//
//						}
					}
				});
		HEXCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					mOutEditText.setText("");
					mOutEditText.setVisibility(View.GONE);
					mOutEditText2.setVisibility(View.VISIBLE);
				} else {
					mOutEditText.setVisibility(View.VISIBLE);
					mOutEditText2.setVisibility(View.GONE);
				}

			}
		});
	}

	@SuppressLint("NewApi")
	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// 如果是没有，要求它启用。
		// setupchat()将被称为在onactivityresult
		if (!mBluetoothAdapter.isEnabled()) {
	//以为这样会无提示，结果无效，fu'c'k		
    //			mBluetoothAdapter.enable();
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// 否则，设置聊天会话
		} else {
			if (mChatService == null)
				setupChat();
		}
	}

	// 连接按键响应函数
	public void onConnectButtonClicked(View v) {

		if (breakButton.getText().equals("连接四轴")||breakButton.getText().equals("connect")) {
			Intent serverIntent = new Intent(this, DeviceListActivity.class); // 跳转程序设置
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE); // 设置返回宏定义
			breakButton.setText(R.string.duankai);

		} else {
			// 关闭连接socket
			try {
				// 关闭蓝牙
				breakButton.setText(R.string.button_break);
				mChatService.stop();

			} catch (Exception e) {
			}
		}
		return;
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (D)
			Log.e(TAG, "+ ON RESUME +");

		// 执行此检查onresume()涵盖的案件中，英国电信
		// 不可在onstart()，所以我们停下来让它…
		// onresume()将被调用时，action_request_enable活动返回。
		if (mChatService != null) {
			// 只有国家是state_none，我们知道，我们还没有开始
			if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
				// 启动蓝牙聊天服务
				mChatService.start();
			}
		}
	}

	@SuppressLint("NewApi")
	private void setupChat() {
		Log.d(TAG, "setupChat()");
		// 初始化撰写与听众的返回键
		mOutEditText = (EditText) findViewById(R.id.edit_text_out);
		mOutEditText.setOnEditorActionListener(mWriteListener);
		mOutEditText2 = (EditText) findViewById(R.id.edit_text_out2);
		mOutEditText2.setOnEditorActionListener(mWriteListener);

		// 初始化发送按钮，单击事件侦听器
		mSendButton = (Button) findViewById(R.id.button_send);
		mSendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// 发送消息使用内容的文本编辑控件
				TextView view = (TextView) findViewById(R.id.edit_text_out);
				TextView view2 = (TextView) findViewById(R.id.edit_text_out2);
				String message = view.getText().toString();
				String message2 = view2.getText().toString();
				
				try {
					message.getBytes("ISO_8859_1");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}   
				if (HEXCheckBox.isChecked()) {
				//	sendMessage(message2);
			
					
				}else {
					
					sendMessage(message);
				}
			}
		});

		// 初始化bluetoothchatservice执行蓝牙连接
		mChatService = new BluetoothChatService(this, mHandler);

		// 缓冲区初始化传出消息
		mOutStringBuffer = new StringBuffer("");
	}

	public void onMyButtonClick(View view) {
		if (view.getId() == R.id.button_clean) {
			mInputEditText.setText("");
			fmsg="";
			sum =0;
		}
		if (view.getId() == R.id.button_break) {

			onConnectButtonClicked(breakButton);
		}
		if (view.getId()== R.id.button_full_screen) {
			String Data =mInputEditText.getText().toString();
			if (Data.length()>0) {
				Intent intent = new Intent(); 
			intent.putExtra(BluetoothData,Data);
			//intent.setClass(BluetoothChat.this, FullScreen.class);
			startActivity(intent); 
			}else {
				Toast.makeText(this, R.string.prompt_message, Toast.LENGTH_LONG).show();
			}
			
		}
		
	}
	@Override
	public synchronized void onPause() {
		super.onPause();
		if (D)
			
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// 蓝牙聊天服务站
		if (mChatService != null)
			mChatService.stop();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
	}

	@SuppressLint("NewApi")
	private void ensureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}
	/**
	 * 发送一个字节
	 * 
	 * @param message
	 *            一个文本字符串发送.
	 */
	private void sendHEX(byte[] ctrlVal) {
		// 检查我们实际上在任何连接
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
		//	Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();//显示未连接
			return;
		}

		// 检查实际上有东西寄到
		else{
			mChatService.write(ctrlVal);
			// 重置字符串缓冲区零和清晰的文本编辑字段
			//mOutEditText.setText(mOutStringBuffer);
			//mOutEditText2.setText(mOutStringBuffer);

		}
		// }else if(message.length()<=0){
		// Toast.makeText(BluetoothChat.this, "连接已断开",
		// Toast.LENGTH_LONG).show();
		// // 用户未启用蓝牙或发生错误
		// mChatService = new BluetoothChatService(this, mHandler);
		// Intent serverIntent = new Intent(BluetoothChat.this,
		// DeviceListActivity.class);
		// startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
		// }
	}
	/**
	 * 发送一个消息
	 * 
	 * @param message
	 *            一个文本字符串发送.
	 */
	private void sendMessage(String message) {
		// 检查我们实际上在任何连接
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// 检查实际上有东西寄到
		if (message.length() > 0) {
			// 得到消息字节和告诉bluetoothchatservice写
			byte[] send = message.getBytes();
			mChatService.write(send);
			// 重置字符串缓冲区零和清晰的文本编辑字段
			//mOutEditText.setText(mOutStringBuffer);
			//mOutEditText2.setText(mOutStringBuffer);

		}
		// }else if(message.length()<=0){
		// Toast.makeText(BluetoothChat.this, "连接已断开",
		// Toast.LENGTH_LONG).show();
		// // 用户未启用蓝牙或发生错误
		// mChatService = new BluetoothChatService(this, mHandler);
		// Intent serverIntent = new Intent(BluetoothChat.this,
		// DeviceListActivity.class);
		// startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
		// }
	}

	// 行动的倾听者的编辑框控件，听回车键
	@SuppressLint("NewApi")
	private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
		@TargetApi(Build.VERSION_CODES.CUPCAKE)
		@SuppressLint("NewApi")
		public boolean onEditorAction(TextView view, int actionId,
				KeyEvent event) {

			// 如果行动是一个关键行动事件的返回键，发送消息
			if (actionId == EditorInfo.IME_NULL
					&& event.getAction() == KeyEvent.ACTION_UP) {
				if (view.getId() == R.id.edit_text_out2) {
					String tmp = view.getText().toString();
					String d;
					for(int i=0;i<tmp.length();i++){
						d=tmp.charAt(i)+"";
						if(i%2!=0){
							d+=" ";
						}
						
							sendMessage("\n"+ d);
						
					}
                
					
				}

			}
			if (D)
				Log.i(TAG, "END onEditorAction");
			return true;
		}
	};

	private int convertB2I(byte val){
		int result = 0;
		result=val&0x0ff;
		return result;
	}
	private byte convertI2B(int intValue){
		byte byteValue=0;
		int temp = intValue %256;
		if(intValue<0){
			byteValue = (byte)(temp<-128?256+temp:temp);
		}
		else{
			byteValue = (byte)(temp>127?temp-256:temp);
		}
		
		return byteValue;
	}
	// 处理程序，获取信息的bluetoothchatservice回来
	private final Handler mHandler = new Handler() {
		

		@SuppressLint("NewApi")
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					mTitle.setText(R.string.title_connected_to);
					mTitle.append(mConnectedDeviceName);
					mInputEditText.setText("");
					break;
				case BluetoothChatService.STATE_CONNECTING:
					mTitle.setText(R.string.title_connecting);
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					mTitle.setText(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// 构建一个字符串缓冲区
				String writeMessage = new String(writeBuf);
				sum=1;
				UTF=1;
				mmsg += writeMessage;
				if (checkBox_sixteen.isChecked()) {
					newCode = CodeFormat.Stringspace("\n<--"+writeMessage+"\n");
					
					mInputEditText.getText().append(newCode);
                    fmsg+="\n<--"+newCode+"\n";
				} else {

					mInputEditText.getText().append("\n<--"+writeMessage+"\n");
                    fmsg+="\n<--"+writeMessage+"\n";
				}

				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// 构建一个字符串从有效字节的缓冲区
				if(readBuf[0]==0x71){
					Toast.makeText(BluetoothChat.this,"保存成功！",Toast.LENGTH_SHORT).show();		
				}
				/*
				switch(readBuf[0]){
				case 0x01:
					Toast.makeText(BluetoothChat.this,"读取1成功！",Toast.LENGTH_SHORT).show();
					rowP.setInputType(InputType.TYPE_CLASS_TEXT);
					rowP.setText(Integer.toString((convertB2I(readBuf[1])<<8)+convertB2I(readBuf[2])));
					rowP.setInputType(InputType.TYPE_CLASS_NUMBER);
					break;
				case 0x02:
					
					break;
				case 0x03:
					Toast.makeText(BluetoothChat.this,"读取3成功！",Toast.LENGTH_SHORT).show();
					rowD.setInputType(InputType.TYPE_CLASS_TEXT);
					rowD.setText(Integer.toString((convertB2I(readBuf[1])<<8)+convertB2I(readBuf[2])));
					rowD.setInputType(InputType.TYPE_CLASS_NUMBER);
					break;
				case 0x04:
					Toast.makeText(BluetoothChat.this,"读取4成功！",Toast.LENGTH_SHORT).show();
					pitP.setInputType(InputType.TYPE_CLASS_TEXT);
					pitP.setText(Integer.toString((convertB2I(readBuf[1])<<8)+convertB2I(readBuf[2])));
					pitP.setInputType(InputType.TYPE_CLASS_NUMBER);
					break;
				case 0x05:
					
					break;
				case 0x06:
					Toast.makeText(BluetoothChat.this,"读取6成功！",Toast.LENGTH_SHORT).show();
					pitD.setInputType(InputType.TYPE_CLASS_TEXT);
					pitD.setText(Integer.toString((convertB2I(readBuf[1])<<8)+convertB2I(readBuf[2])));
					pitD.setInputType(InputType.TYPE_CLASS_NUMBER);
					break;
				case 0x07:
					Toast.makeText(BluetoothChat.this,"读取7成功！",Toast.LENGTH_SHORT).show();
					yawP.setInputType(InputType.TYPE_CLASS_TEXT);
					yawP.setText(Integer.toString((convertB2I(readBuf[1])<<8)+convertB2I(readBuf[2])));
					yawP.setInputType(InputType.TYPE_CLASS_NUMBER);break;
				case 0x08:break;
				case 0x09:
					Toast.makeText(BluetoothChat.this,"读取9成功！",Toast.LENGTH_SHORT).show();
					yawD.setInputType(InputType.TYPE_CLASS_TEXT);
					yawD.setText(Integer.toString((convertB2I(readBuf[1])<<8)+convertB2I(readBuf[2])));
					yawD.setInputType(InputType.TYPE_CLASS_NUMBER);
					break;
				}*/
				if(readBuf[0]==0x70){
					
					Toast.makeText(BluetoothChat.this,"读取0x70成功！",Toast.LENGTH_SHORT).show();	
				
				}
				if (sum==1) {
					mInputEditText.getText().append(Html.fromHtml("<font color=\"blue\">"+"\n-->\n"+"</font>"));
					fmsg+="\n-->\n";
					sum++;
				}else {
					sum++;
				}
				String readMessage = new String(readBuf, 0, msg.arg1);
			/*	if (checkBox_sixteen.isChecked()) {
					if (UTF==1) {
						newCode2 = CodeFormat.bytesToHexStringTwo(readBuf, 7);
						mInputEditText.getText().append(Html.fromHtml("<font color=\"blue\">"+CodeFormat.Stringspace(newCode2)+"</font>"));
						fmsg+=Html.fromHtml("<font color=\"blue\">"+CodeFormat.bytesToHexStringTwo(readBuf, 7)+"</font>");
						UTF++;
					}else {
						UTF++;
					}
				} else*/
				{
				
                    mInputEditText.getText().append(Html.fromHtml("<font color=\"blue\">"+readMessage+"</font>"));
                    fmsg+=Html.fromHtml("<font color=\"blue\">"+readMessage+"</font>");
				}

				break;
			case MESSAGE_DEVICE_NAME:
				// 保存该连接装置的名字
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"已连接 " + mConnectedDeviceName, Toast.LENGTH_SHORT)
						.show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	public String changeCharset(String str, String newCharset)
			throws UnsupportedEncodingException {
		if (str != null) {
			// 用默认字符编码解码字符串。
			byte[] bs = str.getBytes();
			// 用新的字符编码生成字符串
			return new String(bs, newCharset);
		}
		return null;
	}

	/**
	 * 将字符编码转换成UTF-8码
	 */
	public String toUTF_8(String str) throws UnsupportedEncodingException {
		return this.changeCharset(str, "UTF_8");
	}

	@SuppressLint("NewApi")
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// 当devicelistactivity返回连接装置
			if (resultCode == Activity.RESULT_OK) {
				// 获得设备地址
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// 把蓝牙设备对象
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				// 试图连接到装置
				mChatService.connect(device);
			}
			break;
		case REQUEST_ENABLE_BT:
			// 当请求启用蓝牙返回
			if (resultCode == Activity.RESULT_OK) {
				// 蓝牙已启用，所以建立一个聊天会话
				setupChat();
			} else {
				// 用户未启用蓝牙或发生错误
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.scan:
			// 用户未启用蓝牙或发生错误
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			return true;
		case R.id.discoverable:
			// 确保此设备是发现别人
			ensureDiscoverable();
			return true;

		case R.id.setup:
			new AlertDialog.Builder(this)
					.setTitle("设置可选参数")
					.setIcon(android.R.drawable.ic_dialog_info)
					.setSingleChoiceItems(new String[] { "十六进制", "字符串" }, 0,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {

									if (dialog.equals("十六进制")) {
										Log.d(TAG, "十六进制");
										dialogs = true;
									} else {
										dialogs = false;
										Log.d(TAG, "字符串");
									}
									dialog.dismiss();
								}
							}).setNegativeButton("取消", null).show();
			     return true;

		case R.id.clenr:
			finish();
			return true;
		}
		return false;
	}
	public boolean onKeyDown(int keyCode, KeyEvent event)  {  
		
		if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN){   
	        if((System.currentTimeMillis()-exitTime) > 2000){  
	            Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();                                
	            exitTime = System.currentTimeMillis();   
	        } else {
	            finish();
	            System.exit(0);
	        }
	        return true;   
	    }
	    return super.onKeyDown(keyCode, event);
		      }

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@SuppressLint("NewApi")
	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
				if (event.sensor == null) {  
		            return;  
		        }  
		  
		        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
		        	
		            float x = (float) event.values[0];  
		            float y = (float) event.values[1];  
		            float z = (float) event.values[2];  
		            mCalendar = Calendar.getInstance();  
		            long stamp = mCalendar.getTimeInMillis() / 1000l;// 1393844912  
		          
		            int second = mCalendar.get(Calendar.SECOND);// 53  
		            rc_roll.setText("俯仰:\n"+(int)(-x*10)+"°");
					rc_pit.setText("横滚:\n"+(int)(-y*10)+"°");
		            float px = Math.abs(mX - x);  
		            float py = Math.abs(mY - y);  
		            float pz = Math.abs(mZ - z);  
		            Log.d(TAG, "pX:" + px + "  pY:" + py + "  pZ:" + pz + "    stamp:"  
		                    + stamp + "  second:" + second);  
		            float maxvalue = getMaxValue(px, py, pz);  
		            if (maxvalue > 2 && (stamp - lasttimestamp) > 30) {  
		                lasttimestamp = stamp;  
		                Log.d(TAG, " sensor isMoveorchanged....");  
		                 
		            }  
		  
		            mX = x;  
		            mY = y;  
		            mZ = z;  
		       /*     if(x>=8)x=8;if(x<=-8)x=-8;//手机最大倾角为+-9
		            if(y>=8)y=8;if(y<=-8)y=-8;
		            if(y<=2&&y>=-2)y=0;
		            else if(y>2)y-=2;
		            else if(y<-2)y+=2;e
					if(x<=2&&x>=-2)x=0;//在中间+-3不控制。
				    else if(x>2)x-=2;
			        else if(x<-2)x+=2;
			     */
		            if(connect==1){
		            roll=(int)(y*10);
		            if(roll>50)roll=50;
		            else if(roll<-50)roll=-50;
		            pitch=(int)(-x*10);
		            if(pitch>50)pitch=50;
		            else if(pitch<-50)pitch=-50;
		            roll+=50;
		            pitch+=50;
		            getCtr[0]=(byte) 0xAA;
					getCtr[1]=(byte) 0xBB;
					getCtr[2]=(byte) ((byte)(THROTTLE&0xff));
					getCtr[3]=(byte) ((byte)(THROTTLE>>8));
					
					getCtr[4]=(byte)(int)pitch;//a*5=50度a=10
					getCtr[5]=(byte)(int)roll;
					getCtr[6]=(byte) (50);
					for(int j=7;j<30;j++)getCtr[j]=0;
					getCtr[31]=(byte) 0x01;
					sendHEX(getCtr);
		            }
		        }  
			}   
			 /** 
		     * 获取一个最大值 
		     *  
		     * @param px 
		     * @param py 
		     * @param pz 
		     * @return 
		     */  
		    public float getMaxValue(float px, float py, float pz) {  
		        float max = 0;  
		        if (px > py && px > pz) {  
		            max = px;  
		        } else if (py > px && py > pz) {  
		            max = py;  
		        } else if (pz > px && pz > py) {  
		            max = pz;  
		        }  
		  
		        return max;  
		    }  
		
}