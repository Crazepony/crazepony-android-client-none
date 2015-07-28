/* Project: App Controller for copter 4(including Crazepony)
 * Author: 	Huang Yong xiang 
 * Brief:	This is an open source project under the terms of the GNU General Public License v3.0
 * TOBE FIXED:  1. disconnect and connect fail with Bluetooth due to running thread 
 * 				2. Stick controller should be drawn in dpi instead of pixel.  
 * 
 * */

package com.test.Crazepony;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

	//向BLE发送数据的周期，现在是两类数据，一是摇杆的4通道值，二是请求IMU数据跟新命令
    //BLE模块本身传输速率有限，尽量减少数据发送量
	private final static int WRITE_DATA_PERIOD=40;
    private static int IMU_CNT = 0; //update IMU period，跟新IMU数据周期，40*10ms
    Handler timeHandler = new Handler();    //定时器周期，用于跟新IMU数据等
	 
	private TextView throttleText,yawText,pitchText,rollText;
	private TextView pitchAngText,rollAngText,yawAngText,altText,distanceText,voltageText;
	private Button connectButton,armButton,lauchLandButton,headFreeButton,altHoldButton;

    //摇杆界面实现类，joystick UI
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
                resetButtonValue(R.string.Disconnect);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                resetButtonValue(R.string.Connect);
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
                updateLogData(1);   //跟新IMU数据，update the IMU data
            }
        }
    };

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                timeHandler.postDelayed(this, WRITE_DATA_PERIOD);

                if(IMU_CNT >= 10){
                    IMU_CNT = 0;
                    //request for IMU data update，请求IMU跟新
                    btSendBytes(Protocol.getSendData(Protocol.FLY_STATE,
                            Protocol.getCommandData(Protocol.FLY_STATE)));
                }
                IMU_CNT++;


                // process stick movement，处理摇杆数据
                if(stickView.touchReadyToSend==true){
                    btSendBytes(Protocol.getSendData(Protocol.SET_4CON,
                            Protocol.getCommandData(Protocol.SET_4CON)));

                    Log.i(TAG,"Thro: " +Protocol.throttle +",yaw: " +Protocol.yaw+ ",roll: "
                            + Protocol.roll +",pitch: "+ Protocol.pitch);

                    stickView.touchReadyToSend=false;
                }

                //跟新显示摇杆数据，update the joystick data
                updateLogData(0);

            } catch (Exception e) {

            }
        }
    };


    //设置按键显示为默认初始化值
    //在连接成功或者断开的时候，都需要把button的值复位
    private void resetButtonValue(final int connectBtnId) {
        connectButton.setText(connectBtnId);
        armButton.setText(R.string.Unarm);
        lauchLandButton.setText(R.string.Launch);
        headFreeButton.setTextColor(Color.WHITE);
        altHoldButton.setTextColor(Color.WHITE);
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
        connectButton=(Button)findViewById(R.id.connectButton);
		armButton=(Button)findViewById(R.id.armButton);
		lauchLandButton=(Button)findViewById(R.id.lauchLandButton);
		headFreeButton=(Button)findViewById(R.id.headFreeButton);
		altHoldButton=(Button)findViewById(R.id.altHoldButton);


        //绑定BLE收发服务mServiceConnection
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        //开启IMU数据跟新定时器
        timeHandler.postDelayed(runnable, WRITE_DATA_PERIOD); //每隔1s执行
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

    public void onSendArmButtonClicked(View v)
	{
        String arm = getResources().getString(R.string.Arm);
        String unarm = getResources().getString(R.string.Unarm);
        String disconnectToast = getResources().getString(R.string.DisconnectToast);

		if(mConnected){
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
        String launch  = getResources().getString(R.string.Launch);
        String land = getResources().getString(R.string.Land);
        String disconnectToast = getResources().getString(R.string.DisconnectToast);

        if(mConnected){
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
        String disconnectToast = getResources().getString(R.string.DisconnectToast);

		if(mConnected){
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
        String disconnectToast = getResources().getString(R.string.DisconnectToast);

		if(mConnected){
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
        String disconnectToast = getResources().getString(R.string.DisconnectToast);

		if(mConnected){
			btSendBytes(Protocol.getSendData(Protocol.MSP_ACC_CALIBRATION, Protocol.getCommandData(Protocol.MSP_ACC_CALIBRATION)));
		}else {
            Toast.makeText(this, disconnectToast, Toast.LENGTH_SHORT).show();
        }
	}

	public void btSendBytes(byte[] data)
	{
        //当已经连接上时才发送
		if(mConnected){
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

    //跟新Log相关的数据，主要是飞控传过来的IMU数据和摇杆值数据
    //update log,included the IMU data from FC and joysticks data
    //msg 0 -> joystick data
    //msg 1 -> IMU data
    private void updateLogData(int msg){
        if(0 == msg)
        {
            throttleText.setText("Throttle:"+Integer.toString(Protocol.throttle));
            yawText.setText("Yaw:"+Integer.toString(Protocol.yaw));
            pitchText.setText("Pitch:"+Integer.toString(Protocol.pitch));
            rollText.setText("Roll:"+Integer.toString(Protocol.roll));
        }
        else if(1 == msg)
        {
            pitchAngText.setText("Pitch Ang: "+Protocol.pitchAng);
            rollAngText.setText("Roll Ang: "+Protocol.rollAng);
            yawAngText.setText("Yaw Ang: "+Protocol.yawAng);
            altText.setText("Alt:"+Protocol.alt + "m");

            voltageText.setText("Voltage:"+Protocol.voltage + " V");
            distanceText.setText("speedZ:"+Protocol.speedZ + "m/s");
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