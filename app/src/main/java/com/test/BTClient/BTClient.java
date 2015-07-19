/* Project: App Controller for copter 4(including Crazepony)
 * Author: 	Huang Yong xiang 
 * Brief:	This is an open source project under the terms of the GNU General Public License v3.0
 * TOBE FIXED:  1. disconnect and connect fail with Bluetooth due to running thread 
 * 				2. Stick controller should be drawn in dpi instead of pixel.  
 * 
 * */

package com.test.BTClient;

import java.io.InputStream;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;

@SuppressLint("NewApi")
public class BTClient extends Activity {

    private final static String TAG = BTClient.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";


    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService; //BLE收发服务
    private boolean mConnected = false;

	private final static int REQUEST_CONNECT_DEVICE = 1; // 宏定义查询设备句柄

	private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB"; // SPP服务UUID号

	//update IMU data period，跟新IMU数据周期
	private final static int UPDATE_MUV_STATE_PERIOD=500;
    Handler timeHandler = new Handler();    //定时器周期，用于跟新IMU数据等
	

	//
	private InputStream is; // 输入流，用来接收蓝牙数据
	private String smsg = ""; // 显示用数据缓存
	private String fmsg = ""; // 保存用数据缓存
	 
	private TextView throttleText,yawText,pitchText,rollText;
	private TextView pitchAngText,rollAngText,yawAngText,altText,distanceText,voltageText;
	private Button armButton,lauchLandButton,headFreeButton,altHoldButton,accCaliButton;
	private ArrayAdapter<String> adapter;


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





    // Code to manage Service lifecycle.
    // 管理BLE数据收发服务整个生命周期
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    // 定义处理BLE收发服务的各类事件接收机mGattUpdateReceiver，主要包括下面几种：
    // ACTION_GATT_CONNECTED: 连接到GATT
    // ACTION_GATT_DISCONNECTED: 断开GATT
    // ACTION_GATT_SERVICES_DISCOVERED: 发现GATT下的服务
    // ACTION_DATA_AVAILABLE: BLE收到数据
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            int reCmd=-2;

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.Disconnect);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.Connect);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                // Show all the supported services and characteristics on the user interface.
                // 获得所有的GATT服务，对于Crazepony的BLE透传模块，包括GAP（General Access Profile），
                // GATT（General Attribute Profile），还有Unknown（用于数据读取）
                mBluetoothLeService.getSupportedGattServices();

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);

                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for(byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));

                    Log.i(TAG, "RX Data:"+stringBuilder);
                }


                //解析得到的数据，获得MSP命令编号
                reCmd=Protocol.processDataIn( data,data.length);
                updateIMUdata(reCmd);
            }
        }
    };

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                timeHandler.postDelayed(this, UPDATE_MUV_STATE_PERIOD);

                //request for IMU data update，请求IMU跟新
                btSendBytes(Protocol.getSendData(Protocol.FLY_STATE, Protocol.getCommandData(Protocol.FLY_STATE)));

                // process stick movement，处理摇杆数据
                if(stickView.touchReadyToSend==true){
                    btSendBytes(Protocol.getSendData(Protocol.SET_4CON, Protocol.getCommandData(Protocol.SET_4CON)));

                    stickView.touchReadyToSend=false;
                }
            } catch (Exception e) {

            }
        }
    };


    //跟新Connect按钮
    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button btnConnect = (Button) findViewById(R.id.Button03);
                btnConnect.setText(resourceId);
            }
        });
    }



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
		//pitchAngText,rollAngText,yawAngText,altText,voltageText
		pitchAngText = (TextView)findViewById(R.id.pitchAngText);
		rollAngText = (TextView)findViewById(R.id.rollAngText);
		yawAngText = (TextView)findViewById(R.id.yawAngText);
		altText = (TextView)findViewById(R.id.altText);
		voltageText = (TextView)findViewById(R.id.voltageText);
		distanceText= (TextView)findViewById(R.id.distanceText);
		 
		//摇杆
		stickView=(MySurfaceView)findViewById(R.id.stickView);
		//按钮
		armButton=(Button)findViewById(R.id.armButton);
		lauchLandButton=(Button)findViewById(R.id.lauchLandButton);
		headFreeButton=(Button)findViewById(R.id.headFreeButton);
		altHoldButton=(Button)findViewById(R.id.altHoldButton);
		accCaliButton=(Button)findViewById(R.id.accCaliButton);

        //绑定BLE收发服务mServiceConnection
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        //开启IMU数据跟新定时器
        timeHandler.postDelayed(runnable, UPDATE_MUV_STATE_PERIOD); //每隔1s执行
	}

    @Override
    protected void onResume() {
        super.onResume();

        //注册BLE收发服务接收机mGattUpdateReceiver
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            Log.d(TAG, "mBluetoothLeService NOT null");
        }

    }

	@Override
	public void onPause()
	{
		super.onPause();
        //注销BLE收发服务接收机mGattUpdateReceiver
        unregisterReceiver(mGattUpdateReceiver);
	}
	@Override
	public void onStop()
	{
		super.onStop();
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //解绑BLE收发服务mServiceConnection
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
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
            mBluetoothLeService.writeCharacteristic(data);
		}
	}
	
	// 接收扫描结果，响应startActivityForResult()
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			if (resultCode == Activity.RESULT_OK){
                mDeviceName = data.getExtras().getString(EXTRAS_DEVICE_NAME);
                mDeviceAddress = data.getExtras().getString(EXTRAS_DEVICE_ADDRESS);

                Log.i(TAG, "mDeviceName:"+mDeviceName+",mDeviceAddress:"+mDeviceAddress);

                //连接该BLE Crazepony模块
                if (mBluetoothLeService != null) {
                    final boolean result = mBluetoothLeService.connect(mDeviceAddress);
                    Log.d(TAG, "Connect request result=" + result);
                }
            }
			break;
		default:
			break;
		}
	}


    private void updateIMUdata(int msg){
        if(msg==2)
        {
            throttleText.setText("Throttle:"+Integer.toString(Protocol.throttle));
            yawText.setText("Yaw:"+Integer.toString(Protocol.yaw));
            pitchText.setText("Pitch:"+Integer.toString(Protocol.pitch));
            rollText.setText("Roll:"+Integer.toString(Protocol.roll));
        }
        else if(msg==Protocol.FLY_STATE)
        {
            pitchAngText.setText("Pitch Ang: "+Protocol.pitchAng);
            rollAngText.setText("Roll Ang: "+Protocol.rollAng);
            yawAngText.setText("Yaw Ang: "+Protocol.yawAng);
            altText.setText("Alt:"+Protocol.alt + "m");

            voltageText.setText("Voltage:"+Protocol.voltage + " V");
            distanceText.setText("speedZ:"+Protocol.speedZ + "m/s");
        }
    }



	// 连接按键响应函数
	public void onConnectButtonClicked(View v) {
		if (!mConnected) {
            //进入扫描页面
			Intent serverIntent = new Intent(this, DeviceScanActivity.class); // 跳转程序设置
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE); // 设置返回宏定义

		} else {
            //断开连接
            mBluetoothLeService.disconnect();
		}
	}


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}