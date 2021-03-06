package com.mob.bbssdk.sample;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mob.MobSDK;
import com.mob.bbssdk.gui.BBSViewBuilder;
import com.mob.bbssdk.gui.pages.forum.PageForumThreadDetail;
import com.mob.bbssdk.gui.utils.AppUtils;
import com.mob.bbssdk.gui.utils.ToastUtils;
import com.mob.bbssdk.theme0.BBSTheme0;
import com.mob.bbssdk.theme1.BBSTheme1;
import com.mob.bbssdk.utils.CheckKeyUtils;
import com.mob.bbssdk.utils.StringUtils;
import com.mob.moblink.Scene;
import com.mob.moblink.SceneRestorable;
import com.mob.tools.utils.ResHelper;
import com.mob.tools.utils.SharePrefrenceHelper;
import com.mob.tools.utils.UIHandler;

public class InitActivity extends Activity implements SceneRestorable {
	private static final String SP_NAME = "sp_init";
	private static final int SP_VERSION = 1;
	private static final String SP_KEY = "sp_key";
	private static final String SP_SECRET = "sp_secret";
	private static final String REGREX_KEY = "[A-Za-z0-9]{1,32}";//不超过32个字符
	private static final String REGREX_SECRET = "[A-Za-z0-9]{1,64}";//不超过64个字符

	private LinearLayout layoutParamsContainer;
	private EditText editTextKey;
	private EditText editTextSecret;
	private TextView textHint;
	private View layoutEnterOfficial;
	private String strKeyFromDisk;
	private String strSecretFromDisk;
	private SharePrefrenceHelper spInit;
	private Integer initTheme = null;
	private String strUrl;
	private String strFid;
	private String strTid;
	private BBSUncaughtExceptionHandler bbsUncaughtExceptionHandler;
	private String strDefaultKey;
	private String strDefaultSecret;

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	protected void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(ResHelper.getLayoutRes(InitActivity.this, "activity_init"));
		strDefaultKey = getStringByResName("BBS_APPKEY");
		strDefaultSecret = getStringByResName("BBS_APPSECRET");
		editTextKey = (EditText) findViewById(ResHelper.getIdRes(InitActivity.this, "editTextKey"));
		editTextSecret = (EditText) findViewById(ResHelper.getIdRes(InitActivity.this, "editTextSecret"));
		layoutEnterOfficial = findViewById(ResHelper.getIdRes(InitActivity.this, "layoutEnterOfficial"));
		layoutParamsContainer = (LinearLayout) findViewById(ResHelper.getIdRes(InitActivity.this, "layoutParamsContainer"));
		textHint = (TextView) findViewById(ResHelper.getIdRes(InitActivity.this, "textHint"));
		loadKeyAndSecret();
		if (!StringUtils.isEmpty(strKeyFromDisk) && !strKeyFromDisk.equals(strDefaultKey)) {
			editTextKey.setText(strKeyFromDisk);
		}
		if (!StringUtils.isEmpty(strSecretFromDisk) && !strSecretFromDisk.equals(strDefaultSecret)) {
			editTextSecret.setText(strSecretFromDisk);
		}

		findViewById(ResHelper.getIdRes(InitActivity.this, "btnConfirm")).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				enterMainActivity();
			}
		});
		layoutEnterOfficial.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				initAndStartMainActivity(strDefaultKey, strDefaultSecret);
			}
		});

		findViewById(ResHelper.getIdRes(InitActivity.this, "textViewEnterBase")).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				initTheme = null;
				enterMainActivity();
			}
		});
		findViewById(ResHelper.getIdRes(InitActivity.this, "textViewEnterTheme0")).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				initTheme = 0;
				enterMainActivity();
			}
		});
		findViewById(ResHelper.getIdRes(InitActivity.this, "textViewEnterTheme1")).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				initTheme = 1;
				enterMainActivity();
			}
		});

		//process moblink relatives.
		Intent intent = getIntent();
		if (intent != null && intent.getData() != null && !StringUtils.isEmpty(
				intent.getData().getQueryParameter("params"))) {
//			String params = intent.getData().getQueryParameter("params");
			if (!StringUtils.isEmpty(strDefaultKey) && !StringUtils.isEmpty(strDefaultSecret)) {
				textHint.setText(getStringByResName("bbs_init_restorefromurl"));
				MobSDK.init(InitActivity.this, strDefaultKey, strDefaultSecret);
				layoutParamsContainer.setVisibility(View.GONE);
			}
		}
	}

	protected void enterMainActivity() {
		String key = editTextKey.getText().toString().trim();
		String secret = editTextSecret.getText().toString().trim();
		final String finalkey, finalsecret;
		if (TextUtils.isEmpty(key) || TextUtils.isEmpty(secret)) {
			finalkey = strDefaultKey;
			finalsecret = strDefaultSecret;
		} else {
			finalkey = key;
			finalsecret = secret;
		}
		//only check key and secret validation in release version.
		if (AppUtils.isReleaseVersion()) {
			CheckKeyUtils.checkKeyLegal(InitActivity.this, finalkey, finalsecret, new CheckKeyUtils.Callback() {
				public void onCallback(CheckKeyUtils.Info info) {
					if (info != null && info.status == 200) {
						if (info.hasApp != 1) {
							ToastUtils.showToast(getApplicationContext(), getStringByResName("bbs_init_appsecret_error"));
							return;
						}
						if (info.isSet != 1) {
							ToastUtils.showToast(getApplicationContext(), getStringByResName("bbs_init_pluginnotset_error"));
							return;
						}
						if (info.isInitialize != 1) {
							ToastUtils.showToast(getApplicationContext(), getStringByResName("bbs_init_bbssdknotinit_error"));
							return;
						}
						if (initAndStartMainActivity(finalkey, finalsecret)) {
							saveKeyAndSecret(finalkey, finalsecret);
						}
					} else {
						ToastUtils.showToast(getApplicationContext(), getStringByResName("bbs_init_appsecret_error"));
					}
				}
			});
		} else {
			if (initAndStartMainActivity(finalkey, finalsecret)) {
				saveKeyAndSecret(finalkey, finalsecret);
			}
		}
	}

	@Override
	public void onReturnSceneData(Scene scene) {
		strFid = (String) scene.params.get("fid");
		strTid = (String) scene.params.get("tid");
	}

	private boolean initAndStartMainActivity(String key, String secret) {
		if (StringUtils.isEmpty(key) || !key.matches(REGREX_KEY)) {
			ToastUtils.showToast(InitActivity.this, getStringByResName("bbs_init_illegalkey"));
			return false;
		}

		if (StringUtils.isEmpty(secret) || !secret.matches(REGREX_SECRET)) {
			ToastUtils.showToast(InitActivity.this, getStringByResName("bbs_init_illegalsecret"));
			return false;
		}

		MobSDK.init(InitActivity.this, key, secret);
		registerMainThreadUncaughtExceptioinHandler();
		if (initTheme != null) {
			if (initTheme == 0) {
				BBSTheme0.init();
			} else {
				BBSTheme1.init();
			}
		}
		//init success or fail?
		finish();
		Intent intent = new Intent(InitActivity.this, MainActivity.class);
		startActivity(intent);

		//open web link page if valid
		if (!StringUtils.isEmpty(strFid) && !StringUtils.isEmpty(strTid)) {
			final PageForumThreadDetail details = BBSViewBuilder.getInstance().buildPageForumThreadDetail();
			details.setForumThread(Long.parseLong(strFid), Long.parseLong(strTid), "");
			UIHandler.sendMessageDelayed(null, 300, new Handler.Callback() {
				@Override
				public boolean handleMessage(Message msg) {
					details.show(InitActivity.this);
					return false;
				}
			});
		}
		return true;
	}

	private void loadKeyAndSecret() {
		spInit = new SharePrefrenceHelper(InitActivity.this);
		spInit.open(SP_NAME, SP_VERSION);
		strKeyFromDisk = (String) spInit.get(SP_KEY);
		strSecretFromDisk = (String) spInit.get(SP_SECRET);
	}

	private void saveKeyAndSecret(String key, String secret) {
		if (spInit != null) {
			spInit.put(SP_KEY, key);
			spInit.put(SP_SECRET, secret);
		}
	}

	private String getStringByResName(String name) {
		if (StringUtils.isEmpty(name)) {
			return "";
		}
		return getString(ResHelper.getStringRes(InitActivity.this, name));
	}

	private void registerMainThreadUncaughtExceptioinHandler() {
		if (bbsUncaughtExceptionHandler != null) {
			return;
		}
		Thread.UncaughtExceptionHandler uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
		bbsUncaughtExceptionHandler = new BBSUncaughtExceptionHandler(uncaughtExceptionHandler);
		Thread.setDefaultUncaughtExceptionHandler(bbsUncaughtExceptionHandler);
	}

	private static class BBSUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
		private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

		BBSUncaughtExceptionHandler(Thread.UncaughtExceptionHandler handler) {
			this.uncaughtExceptionHandler = handler;
		}

		@Override
		public void uncaughtException(Thread thread, Throwable ex) {
			ex.printStackTrace();
			if (uncaughtExceptionHandler != null) {
				uncaughtExceptionHandler.uncaughtException(thread, ex);
			}
		}
	}
}
