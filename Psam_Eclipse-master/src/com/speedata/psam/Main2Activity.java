package com.speedata.psam;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.serialport.DeviceControlSpd;
import android.serialport.SerialPortSpd;
import android.support.annotation.NonNull;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.speedata.libutils.ConfigUtils;
import com.speedata.libutils.DataConversionUtils;
import com.speedata.libutils.ReadBean;
import com.speedata.utils.ProgressDialogUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import speedatacom.a3310libs.PsamManager;
import speedatacom.a3310libs.inf.IPsam;

public class Main2Activity extends Activity implements View.OnClickListener {
	public ReadBean.PasmBean psam;
	String send_data = "";
	int yourChoice;
	private Button btn1Activite, btn2Activite, btnGetRomdan, btnSendAdpu,
			btnClear, btnOpenSerial, btnPsam4442, btn4442Cmd;
	private EditText edvADPU;
	private TextView tvShowData;
	private int psamflag = 0;
	private Context mContext;
	private TextView tvVerson;
	private TextView tvConfig;
	private TextView imgReset;
	private Button btnOriginalCmd, btnChangeB;
	private SerialPortSpd serialPort;
	private DeviceControlSpd deviceControl1;
	// 获取psam实例
	private IPsam psamIntance = PsamManager.getPsamIntance();

	/**
	 * 截取数组
	 * 
	 * @param bytes
	 *            被截取数组
	 * @param start
	 *            被截取数组开始截取位置
	 * @param length
	 *            新数组的长度
	 * @return 新数组
	 */
	public static byte[] cutBytes(byte[] bytes, int start, int length) {
		byte[] res = new byte[length];
		System.arraycopy(bytes, start, res, 0, length);
		return res;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main2);
		mContext = this;
		initUI();
		showConfig();
		initDefaultDev();
		serialPort = new SerialPortSpd();
		edvADPU.clearFocus();
	}

	private void showConfig() {
		String verson = getVersion();
		tvVerson.setText("V:" + verson);
		boolean isExit = ConfigUtils.isConfigFileExists();
		if (isExit) {
			tvConfig.setText("定制配置：\n");
		} else {
			tvConfig.setText("标准配置：\n");
		}
		psam = ConfigUtils.readConfig(this).getPasm();
		String gpio = "";
		List<Integer> gpio1 = psam.getGpio();
		for (Integer s : gpio1) {
			gpio += s + ",";
		}
		tvConfig.append("串口:" + psam.getSerialPort() + "  波特率："
				+ psam.getBraut() + " 上电类型:" + psam.getPowerType() + " GPIO:"
				+ gpio + " resetGpio:" + psam.getResetGpio());
	}

	private void initUI() {
		imgReset = (TextView) findViewById(R.id.img_reset);
		imgReset.setOnClickListener(this);
		tvConfig = (TextView) findViewById(R.id.tv_config);
		btn1Activite = (Button) findViewById(R.id.btn1_active);
		btnOpenSerial = (Button) findViewById(R.id.btn_OpenSerial);
		btn2Activite = (Button) findViewById(R.id.btn2_active);
		btnGetRomdan = (Button) findViewById(R.id.btn_get_ramdon);
		btnSendAdpu = (Button) findViewById(R.id.btn_send_adpu);
		btnClear = (Button) findViewById(R.id.btn_clear);
		tvVerson = (TextView) findViewById(R.id.tv_verson);
		btnOriginalCmd = (Button) findViewById(R.id.btn_original_cmd);
		btnChangeB = (Button) findViewById(R.id.btn_change_b);
		btnPsam4442 = (Button) findViewById(R.id.btn_get_psam4442);
		btn4442Cmd = (Button) findViewById(R.id.btn_4442_cmd);
		btnOpenSerial.setOnClickListener(this);
		btnChangeB.setOnClickListener(this);
		btnOriginalCmd.setOnClickListener(this);
		btn1Activite.setOnClickListener(this);
		btn2Activite.setOnClickListener(this);
		btnGetRomdan.setOnClickListener(this);
		btnSendAdpu.setOnClickListener(this);
		btnClear.setOnClickListener(this);
		btnPsam4442.setOnClickListener(this);
		btn4442Cmd.setOnClickListener(this);
		tvShowData = (TextView) findViewById(R.id.tv_show_message);
		tvShowData.setMovementMethod(ScrollingMovementMethod.getInstance());
		edvADPU = (EditText) findViewById(R.id.edv_adpu_cmd);
		edvADPU.setText("00A404000BA000000003454E45524759");
	}

	private void openFailed(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ProgressDialogUtils.dismissProgressDialog();

				new AlertDialog.Builder(Main2Activity.this)
						.setCancelable(false)
						.setMessage("PSAM模块初始化失败,请前往调试工具中修改参数" + msg)
						.setPositiveButton("确定",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(
											DialogInterface dialogInterface,
											int i) {
										openConfig();

									}
								}).show();
			}
		}

		);
	}

	/**
	 * 打开调试工具 修改配置
	 */
	private void openConfig() {
		// 打开失败去下载
		try {
			Intent intent = new Intent();
			intent.setAction("speedata.config");
			startActivity(intent);
		} catch (Exception e) {
			// downLoadDeviceApp();
			new AlertDialog.Builder(this).setCancelable(false)
					.setMessage("请去应用市场下载调试工具进行配置")
					.setPositiveButton("确定", null).show();
		}

	}

	private void initDefaultDev() {
		try {
			psamIntance.initDev(this);
			// psamIntance.initDev("ttyMT1",115200,
			// DeviceControlSpd.PowerType.NEW_MAIN,Main2Activity.this,75,74);
			// psamIntance.resetDev(DeviceControlSpd.PowerType.NEW_MAIN,74);
			psamIntance.resetDev();
		} catch (IOException e) {
			e.printStackTrace();
			openFailed(e.getMessage());
		}
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onClick(View v) {
		if (v == imgReset) {
			psamIntance.resetDev();

		} else if (v == btn1Activite) {
			psamflag = 1;
			byte[] data = psamIntance.PsamPower(IPsam.PowerType.Psam1);
			if (data != null) {
				tvShowData.setText("Psam1 activite \n"
						+ DataConversionUtils.byteArrayToString(data));
			} else {
				tvShowData.setText("failed");
			}
		} else if (v == btn2Activite) {
			psamflag = 2;
			byte[] data = psamIntance.PsamPower(IPsam.PowerType.Psam2);
			if (data != null) {
				tvShowData.setText("Psam2 activite \n"
						+ DataConversionUtils.byteArrayToString(data));
			} else {
				tvShowData.setText("failed");
			}
		} else if (v == btnPsam4442) {
			psamflag = 3;
			byte[] data = psamIntance.PsamPower(IPsam.PowerType.Psam4442On);
			if (data != null) {
				tvShowData.setText("Psam2 activite \n"
						+ DataConversionUtils.byteArrayToString(data));
			} else {
				tvShowData.setText("failed");
			}

		} else if (v == btnGetRomdan) {
			if (psamflag == 1) {

				try {
					tvShowData.setText("Psam1 Send data：00 84 00 00 04\n");
					byte[] data = psamIntance.WriteCmd(new byte[] { 0x00,
							(byte) 0x84, 0x00, 0x00, 0x04 },
							IPsam.PowerType.Psam1);
					if (data != null) {

						tvShowData.append("rece->"
								+ DataConversionUtils.byteArrayToString(data));
					} else {
						return;
					}
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

			} else if (psamflag == 2) {
				try {
					tvShowData.setText("Psam2 Send data：00 84 00 00 08\n");
					byte[] data = psamIntance.WriteCmd(new byte[] { 0x00,
							(byte) 0x84, 0x00, 0x00, 0x08 },
							IPsam.PowerType.Psam2);
					if (data != null) {
						tvShowData.append("rece->"
								+ DataConversionUtils.byteArrayToString(data));
					}
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		} else if (v == btnSendAdpu) {
			String temp_cmd = edvADPU.getText().toString();
			// if ("".equals(temp_cmd) || temp_cmd.length() % 2 > 0 ||
			// temp_cmd.length() < 4) {
			// Toast.makeText(mContext, "Please enter a valid instruction！",
			// Toast.LENGTH_SHORT)
			// .show();
			// return;
			// }
			send_data = temp_cmd;
			if (psamflag == 1) {
				tvShowData.setText("Psam1 Send data：\n" + send_data + "\n");
				try {
					byte[] data = psamIntance.WriteCmd(
							DataConversionUtils.HexString2Bytes(temp_cmd),
							IPsam.PowerType.Psam1);
					if (data != null) {
						tvShowData.append("rece->"
								+ DataConversionUtils.byteArrayToString(data));
					}
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

			} else if (psamflag == 2) {
				tvShowData.setText("Psam2 Send data：\n" + send_data + "\n");
				try {
					byte[] data = psamIntance.WriteCmd(
							DataConversionUtils.HexString2Bytes(temp_cmd),
							IPsam.PowerType.Psam2);
					if (data != null) {
						tvShowData.append("rece->"
								+ DataConversionUtils.byteArrayToString(data));
					}
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		} else if (v == btnOriginalCmd) {
			String temp_cmd = edvADPU.getText().toString();
			if ("".equals(temp_cmd) || temp_cmd.length() % 2 > 0
					|| temp_cmd.length() < 4) {
				Toast.makeText(mContext, "Please enter a valid instruction！",
						Toast.LENGTH_SHORT).show();
				return;
			}
			send_data = temp_cmd;
			if (psamflag == 1) {
				tvShowData.setText("Psam1 Send data：\n" + send_data + "\n");
				try {
					byte[] data = psamIntance.WriteOriginalCmd(
							DataConversionUtils.HexString2Bytes(temp_cmd),
							IPsam.PowerType.Psam1);
					if (data != null) {
						tvShowData.append("rece->"
								+ DataConversionUtils.byteArrayToString(data));
					}
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

			} else if (psamflag == 2) {
				tvShowData.setText("Psam2 Send data：\n" + send_data + "\n");
				try {
					byte[] data = psamIntance.WriteOriginalCmd(
							DataConversionUtils.HexString2Bytes(temp_cmd),
							IPsam.PowerType.Psam2);
					if (data != null) {
						tvShowData.append("rece->"
								+ DataConversionUtils.byteArrayToString(data));
					}
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		} else if (v == btnClear) {
			tvShowData.setText("");
			// test();
		} else if (v == btnChangeB) {
			CmdDialog();
		} else if (v == btnOpenSerial) {
			OpenSerialDialog();
		} else if (v == btn4442Cmd) {
			tvShowData.setText("Psam2 Send data：\n" + send_data + "\n");
			String temp_cmd = edvADPU.getText().toString();
			showSingleChoiceDialog(temp_cmd);
		}

	}

	private void showSingleChoiceDialog(final String temp_cmd) {
		final String[] items = { "上电", "下电", "读卡", "写卡", "读密", "核密", "修密" };
		yourChoice = -1;
		AlertDialog.Builder singleChoiceDialog = new AlertDialog.Builder(
				Main2Activity.this);
		singleChoiceDialog.setTitle("4442卡片指令");
		// 第二个参数是默认选项，此处设置为0
		singleChoiceDialog.setSingleChoiceItems(items, -1,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						yourChoice = which;
					}
				});
		singleChoiceDialog.setPositiveButton("确定",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (yourChoice != -1) {
							byte[] data = null;
							switch (yourChoice) {
							case 0:
								try {
									data = psamIntance.WritePsam4442Cmd(
											DataConversionUtils
													.HexString2Bytes(temp_cmd),
											IPsam.PowerType.Psam4442On);
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
								break;
							case 1:
								try {
									data = psamIntance.WritePsam4442Cmd(
											DataConversionUtils
													.HexString2Bytes(temp_cmd),
											IPsam.PowerType.Psam4442Dwon);
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
								break;
							case 2:
								try {
									data = psamIntance.WritePsam4442Cmd(
											DataConversionUtils
													.HexString2Bytes(temp_cmd),
											IPsam.PowerType.ReadPsam4442);
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
								break;
							case 3:
								try {
									data = psamIntance.WritePsam4442Cmd(
											DataConversionUtils
													.HexString2Bytes(temp_cmd),
											IPsam.PowerType.WritePsam4442);
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
								break;
							case 4:
								try {
									data = psamIntance.WritePsam4442Cmd(
											DataConversionUtils
													.HexString2Bytes(temp_cmd),
											IPsam.PowerType.PwdReadsam44P42);
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
								break;
							case 5:
								try {
									data = psamIntance.WritePsam4442Cmd(
											DataConversionUtils
													.HexString2Bytes(temp_cmd),
											IPsam.PowerType.CheckPwdPsam4442);
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
								break;
							case 6:
								try {
									data = psamIntance.WritePsam4442Cmd(
											DataConversionUtils
													.HexString2Bytes(temp_cmd),
											IPsam.PowerType.ChangePwdPsam4442);
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
								break;
							default:
								break;
							}
							if (data != null) {
								tvShowData.append("rece->"
										+ DataConversionUtils
												.byteArrayToString(data));
							} else {
								Toast.makeText(Main2Activity.this, "请检查指令",
										Toast.LENGTH_SHORT).show();
							}
						}
					}
				});
		singleChoiceDialog.show();
	}

	public void OpenSerialDialog() {
		AlertDialog.Builder customizeDialog = new AlertDialog.Builder(this);
		final View dialogView = LayoutInflater.from(this).inflate(
				R.layout.dialog_change_layout, null);

		final EditText botelv = (EditText) dialogView.findViewById(R.id.botelv);
		customizeDialog.setTitle("打开新串口");
		customizeDialog.setView(dialogView);
		customizeDialog.setPositiveButton("打开",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (!"".equals(botelv.getText().toString())) {
							int b = Integer.parseInt(botelv.getText()
									.toString());
							try {
								if (serialPort != null) {
									serialPort.CloseSerial(serialPort.getFd());
								}
								serialPort.OpenSerial(
										SerialPortSpd.SERIAL_TTYMT3, b);
								Toast.makeText(mContext, "新串口已打开",
										Toast.LENGTH_SHORT).show();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				});
		customizeDialog.show();

	}

	public void CmdDialog() {
		AlertDialog.Builder customizeDialog = new AlertDialog.Builder(this);
		final View dialogView = LayoutInflater.from(this).inflate(
				R.layout.layout, null);
		final EditText botelv = (EditText) dialogView.findViewById(R.id.b);
		final EditText cmds = (EditText) dialogView.findViewById(R.id.cmd);
		customizeDialog.setTitle("更改串口參數");
		customizeDialog.setView(dialogView);
		customizeDialog.setPositiveButton("确定修改",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (!botelv.getText().toString().equals("")
								&& !cmds.getText().toString().equals("")) {
							int b = Integer.parseInt(botelv.getText()
									.toString());
							// int b =
							// Integer.parseInt(cmds.getText().toString());
							try {
								// deviceControl = new
								// DeviceControl2(DeviceControl2.PowerType.MAIN,
								// 96);
								String ss = "aa bb 06 00 00 00 01 01 "
										+ cmds.getText().toString() + " "
										+ cmds.getText().toString();
								// byte[] dd =
								// DataConversionUtils.HexString2Bytes(ss);
								// byte[] ssss = new
								// byte[]{0x00,0x00,0x01,0x05,0x05};
								// byte tmp=0;
								// for (int i = 0; i < ssss.length; i++) {
								// tmp^=ssss[i];
								// }
								serialPort.OpenSerial(
										SerialPortSpd.SERIAL_TTYMT3, b);
								// deviceControl.PowerOnDevice();
								serialPort.WriteSerialByte(serialPort.getFd(),
										DataConversionUtils.HexString2Bytes(ss));
								byte[] sss = serialPort.ReadSerial(
										serialPort.getFd(), 512);
								if (sss != null) {
									if (Arrays.equals(new byte[] { 0x00 },
											cutBytes(sss, 8, 1))) {
										Toast.makeText(mContext, "成功",
												Toast.LENGTH_SHORT).show();
									}
								} else {

									Toast.makeText(mContext, "切换失败",
											Toast.LENGTH_SHORT).show();
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				});
		customizeDialog.show();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			if (deviceControl1 != null) {
				if (Build.MODEL.equals("SD100")) {
					deviceControl1.gtPower("psam_close");
					deviceControl1.gtPower("psam_rst_off");
				} else {
					deviceControl1.PowerOffDevice();
				}
			}
			psamIntance.releaseDev();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取当前应用程序的版本号
	 */
	private String getVersion() {
		PackageManager pm = getPackageManager();
		try {
			PackageInfo packinfo = pm.getPackageInfo(getPackageName(), 0);
			String version = packinfo.versionName;
			return version;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
			return "版本号错误";
		}
	}

}
