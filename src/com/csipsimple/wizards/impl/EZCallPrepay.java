/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.csipsimple.wizards.impl;

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import tw.net.ezcall.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.api.SipUri.ParsedSipContactInfos;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.HashMap;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

public class EZCallPrepay extends BaseImplementation {
	protected static final String THIS_FILE = "EZCall Prepay Implementation";

	private LinearLayout customWizard;
	private TextView customWizardText;

	private String accountUsername;
	private String accountPassword;

	private boolean isFirstOpen = true;
	
	protected EditTextPreference accountDisplayName;
	protected EditTextPreference accountRegCode;

	protected static String DISPLAY_NAME = "display_name";
	protected static String REG_CODE = "reg_code";

	// 預設系統碼
	private String systemCode;
	private String serialCode;

	private String refillCode;

	// AgentTel
	private String agentName = "";
	private String agentTel = "";

	// BankCode for ATM
	private String bankCode = "0";
	private String bankNo = "0";

	// tcl Method
	protected static final int TCL_REGIST = 0;
	protected static final int TCL_REFILL = 1;
	protected static final int TCL_AMT = 2;

	private int tclMethod = TCL_REGIST;

	private SipProfile Account = null;

	protected void bindFields() {
		accountDisplayName = (EditTextPreference) findPreference(DISPLAY_NAME);
		accountRegCode = (EditTextPreference) findPreference(REG_CODE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void fillLayout(final SipProfile account) {
		bindFields();

		Account = account;

		String display_name = account.display_name;
		if (TextUtils.isEmpty(display_name)) {
			display_name = getDefaultName();
		}
		accountDisplayName.setText(display_name);

		String reg_code = account.ezcall_reg_code;
		if (!TextUtils.isEmpty(reg_code)) {
			accountRegCode.setText(reg_code);
			systemCode = reg_code.substring(0, 2);
			serialCode = reg_code.substring(2);
		}

		ParsedSipContactInfos parsedInfo = SipUri
				.parseSipContact(account.acc_id);

		setUsername(parsedInfo.userName);
		setPassword(account.data);

		// accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);

		// Get wizard specific row
		customWizardText = (TextView) parent
				.findViewById(R.id.custom_wizard_text);
		customWizard = (LinearLayout) parent
				.findViewById(R.id.custom_wizard_row);

		customWizard.setOnClickListener(onRefillClickListener);

		// custom dialogLayout onClick
		accountRegCode
				.setOnPreferenceClickListener(onAccountRegCodeClickListener);

		if (!TextUtils.isEmpty(reg_code))
			updateAccountInfos(account, TCL_AMT);
	}

	OnClickListener onRefillClickListener = new OnClickListener() {

		AlertDialog rDialog;

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub

			rDialog = (new AlertDialog.Builder(v.getContext()))
					.setTitle("充值")
					.setView(
							parent.getLayoutInflater().inflate(
									R.layout.wizard_ezcall_refill, null))
					.setPositiveButton(R.string.ok,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									// TODO Auto-generated method stub

									refillCode = ((EditText) rDialog
											.findViewById(R.id.serial_code))
											.getText().toString();

									if (!TextUtils.isEmpty(refillCode))
										updateAccountInfos(Account, TCL_REFILL);

									rDialog = null;
								}
							})
					.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									// TODO Auto-generated method stub
									rDialog = null;
								}
							}).create();

			rDialog.show();

			// 每個帳號systemCode不同
			TextView systemCodeView = (TextView) rDialog
					.findViewById(R.id.system_code);
			systemCodeView.setText(systemCode);

			// Agent
			TextView agentView = (TextView) rDialog
					.findViewById(R.id.readme_agent);
			agentView.setText("若您使用有任何問題，請聯絡您的營運商\n營  運  商 ：" + agentName
					+ "\n連絡電話 ：" + agentTel + "\n帳　　號 ：" + accountUsername
					+ "\n==============================");

			// ATM
			TextView atmView = (TextView) rDialog
					.findViewById(R.id.readme_atm_data);
			if (bankCode.equals("0") && bankNo.equals("0"))
				atmView.setVisibility(View.GONE);
			else {
				atmView.setText("==============================\nATM充值\n銀行代碼 ："
						+ bankCode + "\n轉帳帳號 ：" + bankNo
						+ "\n==============================\n");
			}
		}
	};

	OnPreferenceClickListener onAccountRegCodeClickListener = new OnPreferenceClickListener() {

		Dialog serialDialog;
		EditText systemEditText;
		EditText serialEditText;

		@Override
		public boolean onPreferenceClick(Preference preference) {
			// TODO Auto-generated method stub

			serialDialog = ((EditTextPreference) preference).getDialog();
			systemEditText = (EditText) serialDialog
					.findViewById(R.id.system_code);
			serialEditText = (EditText) serialDialog
					.findViewById(R.id.serial_code);

			String rCode = accountRegCode.getText();
			if (!TextUtils.isEmpty(rCode)) {
				systemCode = rCode.substring(0, 2);
				if (!TextUtils.isEmpty(systemCode))
					systemEditText.setText(systemCode);

				serialCode = rCode.substring(2);
				if (!TextUtils.isEmpty(serialCode))
					serialEditText.setText(serialCode);
			}

			((AlertDialog) serialDialog).setButton(
					DialogInterface.BUTTON_POSITIVE, "OK",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							systemCode = systemEditText.getText().toString();
							serialCode = serialEditText.getText().toString();
							if (TextUtils.isEmpty(systemCode)
									&& TextUtils.isEmpty(serialCode))
								accountRegCode.setText("");
							else
								accountRegCode.setText(systemCode + serialCode);

							updateAccountInfos(Account, TCL_REGIST);

						}
					});

			return false;
		}
	};

	/**
	 * Set descriptions for fields managed by the simple implementation.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public void updateDescriptions() {
		setStringFieldSummary(DISPLAY_NAME);
		setStringFieldSummary(REG_CODE);
	}

	private static HashMap<String, Integer> SUMMARIES = new HashMap<String, Integer>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = -5743705263738203617L;

		{
			put(DISPLAY_NAME, R.string.w_common_display_name_desc);
			put(REG_CODE, R.string.ezcall_reg_code_desc);
		}
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDefaultFieldSummary(String fieldName) {
		Integer res = SUMMARIES.get(fieldName);
		if (res != null) {
			return parent.getString(res);
		}
		return "";
	}

	@Override
	public boolean canSave() {
		boolean isValid = true;

		if (isFirstOpen) {
			isValid = false;
			isFirstOpen = false;
		}

		isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
		isValid &= checkField(accountRegCode, isEmpty(accountRegCode));

		return isValid;
	}

	/**
	 * Basic implementation of the account building based on simple
	 * implementation fields. A specification of this class could extend and add
	 * its own post processing here.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public SipProfile buildAccount(SipProfile account) {
		account.display_name = accountDisplayName.getText().trim();
		account.acc_id = "<sip:" + SipUri.encodeUser(accountUsername.trim())
				+ "@" + getDomain() + ">";

		String regUri = "sip:" + getDomain();
		account.reg_uri = regUri;
		account.proxies = new String[] { regUri };

		account.realm = "*";
		account.username = accountUsername.trim();
		account.data = accountPassword;
		account.scheme = SipProfile.CRED_SCHEME_DIGEST;
		account.datatype = SipProfile.CRED_DATA_PLAIN_PASSWD;
		
		//參考 src/com/csipsimple/pjsip/PjSipAccount.java
//		account.sip_stun_use = 0;
//		account.media_stun_use = 0;
//		account.ice_cfg_use = 0;
//		account.ice_cfg_enable = 0;

		// Try to Clean Registers
		account.try_clean_registers = 0;

		// Contact Rewrite Mdthod
		account.allow_contact_rewrite = false;
		account.contact_rewrite_method = 1;

		account.reg_timeout = 1800;

		account.transport = SipProfile.TRANSPORT_AUTO;

		account.ezcall_reg_code = accountRegCode.getText();

		return account;
	}

	/**
	 * Get the server domain to use by default for registrar, proxy and user
	 * domain.
	 * 
	 * @return The server name / ip of the sip domain
	 */
	// protected abstract String getDomain();
	protected String sipdomain = "sipgo.ttinet.com.tw";

	protected String getDomain() {
		return sipdomain;
	}

	/**
	 * Get the default display name for this account.
	 * 
	 * @return The display name to use by default for this account
	 */
	// protected abstract String getDefaultName();
	protected String getDefaultName() {
		return "EZCall Prepay";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getBasePreferenceResource() {
		return R.xml.w_ezcall_preferences;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean needRestart() {
		return false;
	}

	public void setUsername(String username) {
		if (!TextUtils.isEmpty(username)) {
			accountUsername = username.trim();
		}
	}

	public void setPassword(String password) {
		if (!TextUtils.isEmpty(password)) {
			accountPassword = password;
		}
	}

	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);

		Log.d(THIS_FILE, "setDefaultParams...");

		// ICE
//		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_ICE, false);

		// Stun
//		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, false);
//		prefs.setPreferenceStringValue(SipConfigManager.STUN_SERVER, "stun.stunprotocol.org:3478");
//		prefs.addStunServer("stun.stunprotocol.org:3478");

		// Use 3G
		prefs.setPreferenceBooleanValue(SipConfigManager.USE_3G_IN, true);
		prefs.setPreferenceBooleanValue(SipConfigManager.USE_3G_OUT, true);

		// Disable TCP switch
//		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_TCP, false);
//		prefs.setPreferenceBooleanValue(SipConfigManager.DISABLE_TCP_SWITCH, true);

		// User agent -- useful?
//		prefs.setPreferenceStringValue(SipConfigManager.USER_AGENT, "EZCall");

		// Codecs -- Assume they have legal rights to provide g729 to each users
		// As they activate it by default in their forked app.

		// For Narrowband
//		prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_NB, "0");
//		prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB, "0");
		prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB, "60");
		prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB, "50");
//		prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_NB, "0");
//		prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_NB, "0");

		/*
		 * Disable by default
		 */
//		prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_NB, "0");
//		prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_NB, "0");
//		prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_NB, "0");
//		prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_NB, "0");
//		prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_NB, "0");
//		prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_NB, "0");
//		prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_NB, "0");
//		prefs.setCodecPriority("CODEC2/8000/1", SipConfigManager.CODEC_NB, "0");
//		prefs.setCodecPriority("G7221/16000/1", SipConfigManager.CODEC_NB, "0");
//		prefs.setCodecPriority("G7221/32000/1", SipConfigManager.CODEC_NB, "0");
//		prefs.setCodecPriority("ISAC/16000/1", SipConfigManager.CODEC_NB, "0");
//		prefs.setCodecPriority("ISAC/32000/1", SipConfigManager.CODEC_NB, "0");
//		prefs.setCodecPriority("AMR/8000/1", SipConfigManager.CODEC_NB, "0");

		// For Wideband
//		prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_WB, "0");
//		prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB, "0");
		prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB, "60");
		prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB, "50");
//		prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_WB, "0");
//		prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_WB, "0");

		/*
		 * Disable by default
		 */
//		 prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_WB, "0");
//		 prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_WB, "0");
//		 prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_WB, "0");
//		 prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_WB, "0");
//		 prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_WB, "0");
//		 prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_WB, "0");
//		 prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_WB, "0");
//		 prefs.setCodecPriority("CODEC2/8000/1", SipConfigManager.CODEC_WB, "0");
//		 prefs.setCodecPriority("G7221/16000/1", SipConfigManager.CODEC_WB, "0");
//		 prefs.setCodecPriority("G7221/32000/1", SipConfigManager.CODEC_WB, "0");
//		 prefs.setCodecPriority("ISAC/16000/1", SipConfigManager.CODEC_WB, "0");
//		 prefs.setCodecPriority("ISAC/32000/1", SipConfigManager.CODEC_WB, "0");
//		 prefs.setCodecPriority("AMR/8000/1", SipConfigManager.CODEC_WB, "0");
	}

	private void launchBalanceHelper(SipProfile acc, int tcl) {
		tclMethod = tcl;
		customWizard.setVisibility(View.GONE);
		accountBalanceHelper.launchRequest(acc);
	}

	private void updateAccountInfos(final SipProfile acc, int tcl) {

		launchBalanceHelper(acc, tcl);

		String msg = "";
		switch (tclMethod) {
		case TCL_REGIST:
			msg = "註冊中...";
			break;
		case TCL_REFILL:
			msg = "充值中...";
			break;
		case TCL_AMT:
			msg = "餘額查詢...";
			break;
		default:
			break;
		}

		Toast.makeText(parent, msg, Toast.LENGTH_SHORT).show();

	}

	@SuppressWarnings("deprecation")
	protected String getDeviceId() {

		String MacAdds = "0";
		String IMEI = "0";
		String Simcard = "0";
		String UDID = "0";
		String EmailID = "0";

		WifiManager localWifiManager = (WifiManager) parent
				.getSystemService("wifi");
		if (localWifiManager != null) {
			WifiInfo localWifiInfo = localWifiManager.getConnectionInfo();
			if ((localWifiInfo != null)
					&& (localWifiInfo.getMacAddress() != null))
				MacAdds = URLEncoder.encode(localWifiInfo.getMacAddress());
		}

		TelephonyManager localTelephonyManager = (TelephonyManager) parent
				.getSystemService("phone");
		if (localTelephonyManager != null) {
			if (localTelephonyManager.getDeviceId() != null)
				IMEI = URLEncoder.encode(localTelephonyManager.getDeviceId());

			if (localTelephonyManager.getSubscriberId() != null)
				Simcard = URLEncoder.encode(localTelephonyManager
						.getSubscriberId());

			AccountManager accountManager = AccountManager.get(parent);
			if (accountManager != null) {
				android.accounts.Account[] arrayOfAccount = accountManager
						.getAccountsByType("com.google");
				if (arrayOfAccount.length != 0)
					EmailID = arrayOfAccount[0].name;
			}
		}

		return "&MacAdds=" + MacAdds + "&IMEI=" + IMEI + "&Simcard=" + Simcard
				+ "&UDID=" + UDID + "&EmailID=" + EmailID;
	}

	private AccountBalanceHelper accountBalanceHelper = new AccountBalance(this);

	private static class AccountBalance extends AccountBalanceHelper {

		WeakReference<EZCallPrepay> w;

		private int tclMethod = TCL_REGIST;

		AccountBalance(EZCallPrepay wizard) {
			w = new WeakReference<EZCallPrepay>(wizard);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public HttpRequestBase getRequest(SipProfile acc) throws IOException {
			EZCallPrepay wizard = w.get();

			if (wizard == null) {
				return null;
			}

			// keep the method code
			tclMethod = wizard.tclMethod;

			String requestURL = "http://203.66.46.19/web/";
			switch (tclMethod) {
			case TCL_REGIST:
				// http://203.66.46.19/web/EzcallPin.tcl?SysType=1&Pin=" + str1 + "&MacAdds="
				// + RefillActivity.this.getDeviceId());
				// http://203.66.46.19/web/EzcallPin.tcl?SysType=1&Pin=106589026516546&MacAdds=0&IMEI=0&Simcard=0&UDID=0&EmailID=0
				// http://203.66.46.19/web/EzcallPin.tcl?SysType=1&Pin=106589026516546&MacAdds=7C%3A61%3A93%3A0F%3A8A%3A4E&IMEI=355302042096611&Simcard=466923102302759&UDID=0&EmailID=sharon233925@gmail.com
				requestURL += "EzcallPin.tcl?SysType=1";
				requestURL += "&Pin=" + wizard.systemCode + wizard.serialCode
						+ getPin(wizard.serialCode);
				requestURL += wizard.getDeviceId();

				return new HttpGet(requestURL);
			case TCL_REFILL:
				// "http://203.66.46.19/web/EzcallRef.tcl?SysType=1&Pin=" +
				// RefillActivity.this.getAcntURLEncode() + "&RefPin=" + str1 +
				// "&MacAdds=" + RefillActivity.this.getDeviceId());
				requestURL += "EzcallRef.tcl?SysType=1";
				requestURL += "&Pin=" + wizard.accountUsername;
				requestURL += "&RefPin=" + wizard.systemCode
						+ wizard.refillCode + getPin(wizard.refillCode);
				requestURL += wizard.getDeviceId();

				return new HttpGet(requestURL);
			case TCL_AMT:
				// http://203.66.46.19/web/EzcallAmt.tcl?SysType=1&Pin=" + getAcntURLEncode() + "&MacAdds="
				// + getDeviceId());
				requestURL += "EzcallAmt.tcl?SysType=1";
				requestURL += "&Pin=" + wizard.accountUsername;
				requestURL += wizard.getDeviceId();

				return new HttpGet(requestURL);
			default:
				return null;
			}
		}

		private String getPin(String serialCode) {
			String magicCode = "705619487056";

			if (serialCode.length() != magicCode.length()) {
				System.out.println("length mismatched");
				return "";
			}

			Integer localInteger = 0;

			for (int j = 0; j < magicCode.length(); j++) {
				localInteger += Character.getNumericValue(magicCode.charAt(j))
						* Character.getNumericValue(serialCode.charAt(j));
			}

			localInteger %= 10;

			return (localInteger != 0) ? String.valueOf(10 - localInteger)
					: "0";
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String parseResponseLine(String line) {
			// Log.e(THIS_FILE, line);

			return line;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void applyResultError() {
			EZCallPrepay wizard = w.get();
			if (wizard != null) {
				wizard.customWizard.setVisibility(View.GONE);
				Toast.makeText(wizard.parent, "Error...", Toast.LENGTH_LONG)
						.show();
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void applyResultSuccess(String balanceText) {
			EZCallPrepay wizard = w.get();

			String msg = "";
			String bmsg = (!balanceText.equals("1")) ? "成功!" : "失敗!";

			switch (tclMethod) {
			case TCL_REGIST:
				msg = "註冊" + bmsg;
				break;
			case TCL_REFILL:
				msg = "充值" + bmsg;
				break;
			case TCL_AMT:
				msg = "餘額查詢" + bmsg;
				break;
			default:
				break;
			}

			Toast.makeText(wizard.parent, msg, Toast.LENGTH_SHORT).show();

			if (balanceText.equals("1")) {

				if (tclMethod == TCL_REGIST) {
					// set Default to empty
					wizard.accountUsername = "";
					wizard.accountPassword = "";
				}

				if (tclMethod == TCL_REFILL) {
					// 充值失敗後, 查詢一次
					wizard.launchBalanceHelper(wizard.Account, TCL_AMT);
				}

				return;
			}

			String[] response = balanceText.split(";");

			if (wizard != null) {
				float balance = 0;

				switch (tclMethod) {
				case TCL_REGIST:
					// 0;0901065890265;654K12VK;50.00;sipgo.ttinet.com.tw
					wizard.setUsername(response[1]);
					wizard.setPassword(response[2]);
					balance = Float.parseFloat(response[3]);
					wizard.sipdomain = response[4];

					wizard.Account = wizard.buildAccount(wizard.Account);

					// 註冊完成後, 查詢一次
					wizard.launchBalanceHelper(wizard.Account, TCL_AMT);

					break;
				case TCL_REFILL:
					// 0;10.00;1
					balance = Float.parseFloat(response[1]);

					// 充值完成後, 查詢一次
					wizard.launchBalanceHelper(wizard.Account, TCL_AMT);

					break;
				case TCL_AMT:
					// 9;50.00;;;011;24934560802650;http://www.ezcall.net.tw/app/AND/Android.htm;1
					balance = Float.parseFloat(response[1]);
					wizard.agentName = response[2];
					wizard.agentTel = response[3];
					wizard.bankCode = response[4];
					wizard.bankNo = response[5];

					break;
				default:
					break;
				}

				// 查詢動作完成才顯示餘額
				if (tclMethod == TCL_AMT) {
					wizard.customWizardText.setText("餘額: NT$" + balance);
					wizard.customWizard.setVisibility(View.VISIBLE);
				}
			}
		}
	};
}