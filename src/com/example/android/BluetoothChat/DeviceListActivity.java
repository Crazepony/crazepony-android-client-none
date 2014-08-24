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

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * 这项活动似乎是一个对话框。它列出的任何配对的设备和装置
 *在现场发现后的发现。当一个设备是由用户选择，
 *地址的设备发送给家长活动的
 *结果意图。
 */
public class DeviceListActivity extends Activity
{
	// 调试
	private static final String TAG = "DeviceListActivity";
	private static final boolean D = true;

	// 返回别的意图
	public static String EXTRA_DEVICE_ADDRESS = "device_address";

	// 适配器
	private BluetoothAdapter mBtAdapter;
	private ArrayAdapter<String> mPairedDevicesArrayAdapter;
	private ArrayAdapter<String> mNewDevicesArrayAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// 指定窗口样式
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.device_list);

		// 结果取消如果用户备份
		setResult(Activity.RESULT_CANCELED);

		// 结果取消如果用户备份
		Button scanButton = (Button) findViewById(R.id.button_scan);
		scanButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				doDiscovery();
				v.setVisibility(View.GONE);
			}
		});

		// 初始化数组适配器。一个已配对装置和

         //一个新发现的设备
		mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.device_name);
		mNewDevicesArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.device_name);

		//寻找和建立配对设备列表
		ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
		pairedListView.setAdapter(mPairedDevicesArrayAdapter);
		pairedListView.setOnItemClickListener(mDeviceClickListener);

		// 寻找和建立为新发现的设备列表
		ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
		newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
		newDevicesListView.setOnItemClickListener(mDeviceClickListener);

		// 注册时发送广播给设备
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);

		// 广播时发现已完成注册
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(mReceiver, filter);

		// 获取本地蓝牙适配器
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();

		// 得到一套目前配对设备
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

		// If there are paired devices, add each one to the ArrayAdapter
		if (pairedDevices.size() > 0)
		{
			findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
			for (BluetoothDevice device : pairedDevices)
			{
				mPairedDevicesArrayAdapter.add(device.getName() + "\n"
						+ device.getAddress());
			}
		}
		else
		{
			String noDevices = getResources().getText(R.string.none_paired)
					.toString();
			mPairedDevicesArrayAdapter.add(noDevices);
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		// 确保我们没有发现了
		if (mBtAdapter != null)
		{
			mBtAdapter.cancelDiscovery();
		}

		// 注销广播听众
		this.unregisterReceiver(mReceiver);
	}

	/**
	 * 发现与bluetoothadapter启动装置
	 */
	private void doDiscovery()
	{
		if (D) Log.d(TAG, "doDiscovery()");

		// 显示扫描的称号
		setProgressBarIndeterminateVisibility(true);
		setTitle(R.string.scanning);

		// 打开新设备的字幕
		findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

		// 如果我们已经发现，阻止它
		if (mBtAdapter.isDiscovering())
		{
			mBtAdapter.cancelDiscovery();
		}

		// 要求从bluetoothadapter发现
		mBtAdapter.startDiscovery();
	}

	// 点击听众的所有设备在listviews
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener()
	{
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3)
		{
			// 因为它是浪费的，取消发现我们的连接
			mBtAdapter.cancelDiscovery();

			// 获得设备地址，这是近17字的
			//视图
			String info = ((TextView) v).getText().toString();
			String address = info.substring(info.length() - 17);

			//创建结果意图和包括地址
			Intent intent = new Intent();
			intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

			//结果，完成这项活动
			setResult(Activity.RESULT_OK, intent);
			finish();
		}
	};

	// 该broadcastreceiver监听设备和
	// 变化的标题时，发现完成
	private final BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();

			// 当发现设备
			if (BluetoothDevice.ACTION_FOUND.equals(action))
			{
				//把蓝牙设备对象的意图
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// 如果它已经配对，跳过它，因为它的上市
				// 早已
				if (device.getBondState() != BluetoothDevice.BOND_BONDED)
				{
					mNewDevicesArrayAdapter.add(device.getName() + "\n"
							+ device.getAddress());
				}
				//当发现后，改变活动名称
			}
			else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
			{
				setProgressBarIndeterminateVisibility(false);
				setTitle(R.string.select_device);
				if (mNewDevicesArrayAdapter.getCount() == 0)
				{
					String noDevices = getResources().getText(
							R.string.none_found).toString();
					mNewDevicesArrayAdapter.add(noDevices);
				}
			}
		}
	};

}
