package com.houwei.guaishang.activity;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import cn.beecloud.BCPay;
import cn.beecloud.async.BCCallback;
import cn.beecloud.async.BCResult;
import cn.beecloud.entity.BCPayResult;

import com.houwei.guaishang.R;
import com.houwei.guaishang.bean.BaseResponse;
import com.houwei.guaishang.bean.FloatResponse;
import com.houwei.guaishang.bean.IntResponse;
import com.houwei.guaishang.easemob.PreferenceManager;
import com.houwei.guaishang.manager.MyUserBeanManager.CheckMoneyListener;
import com.houwei.guaishang.tools.HttpUtil;
import com.houwei.guaishang.tools.JsonParser;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

public class PayBaseActivity extends BaseActivity implements CheckMoneyListener, View.OnClickListener {

	private static final int NETWORK_IDEAPAY = 0x96;

	protected static final int PAY_TYPE_WEIXIN = 0;
	protected static final int PAY_TYPE_ALI = 1;
	protected static final int PAY_TYPE_DONGDONG = 2;
	protected static final int PAY_TYPE_CUSTOMER = 3;
	protected static final int PAY_TYPE_OFFLINE = 4;

	protected CheckBox alipay_cb, weixin_cb,dongdon_cb,customer_cb,offline_cb;
	protected LinearLayout alipay_ll, weixin_ll,dongdon_ll,customer_ll,offline_ll;

	protected int payType = 0;

	protected float moneyRequire; // 减去钱包之后，还需要支付多少钱。子类可以随意修改
	
	private float balanceMoney;// 用户总共余额，由网络返回，返回后不得修改

	private MyHandler handler = new MyHandler(this);

	private static class MyHandler extends Handler {

		private WeakReference<Context> reference;

		public MyHandler(Context context) {
			reference = new WeakReference<Context>(context);
		}

		@Override
		public void handleMessage(Message msg) {
			final PayBaseActivity activity = (PayBaseActivity) reference.get();
			if (activity == null) {
				return;
			}
			activity.progress.dismiss();
			switch (msg.what) {
			case BaseActivity.NETWORK_SUCCESS_DATA_RIGHT:
				
				break;
			case NETWORK_IDEAPAY:// 全额虚拟币支付
				BaseResponse response = (BaseResponse)msg.obj;
				if (response.isSuccess()) {
					activity.paySuccess();
				}else{
					activity.showErrorToast(response.getMessage());
				}
				
				break;
			}
		}
	};

	
	//访问余额网络成功，交给子类处理
	protected void getBalenceMoneyFromNetwork(float balanceMoney){}
	
	// 支付结果返回入口
	private BCCallback bcCallback = new BCCallback() {
		@Override
		public void done(final BCResult bcResult) {
			final BCPayResult bcPayResult = (BCPayResult) bcResult;

			// 根据你自己的需求处理支付结果
			// 需要注意的是，此处如果涉及到UI的更新，请在UI主进程或者Handler操作
			PayBaseActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// 此处关闭loading界面
					progress.dismiss();

					String result = bcPayResult.getResult();
					/*
					 * 注意！ 所有支付渠道建议以服务端的状态金额为准，此处返回的RESULT_SUCCESS仅仅代表手机端支付成功
					 */
					if (result.equals(BCPayResult.RESULT_SUCCESS)) {
						paySuccess();
					} else if (result.equals(BCPayResult.RESULT_CANCEL)) {
						// Toast.makeText(ShoppingCartActivity.this, "用户取消支付",
						// Toast.LENGTH_LONG).show();
					}

					else if (result.equals(BCPayResult.RESULT_FAIL)) {
						showErrorToast("支付失败, 原因: " + bcPayResult.getErrMsg()
								+ ", " + bcPayResult.getDetailInfo());
					} else if (result.equals(BCPayResult.RESULT_UNKNOWN)) {
						// 可能出现在支付宝8000返回状态
						showErrorToast("订单状态未知");
					} else {
						showErrorToast("未知错误，请稍后再试");
					}

					if (bcPayResult.getId() != null) {
						// 你可以把这个id存到你的订单中，下次直接通过这个id查询订单
						// 根据ID查询
						// getBillInfoByID(bcPayResult.getId());
					}
				}
			});
		}
	};


	protected void initView() {
		// TODO Auto-generated method stub
		initProgressDialog();

		// 通过WXAPIFactory工厂，获取IWXAPI的实例
//		api = WXAPIFactory.createWXAPI(this, APP_ID, false);
		try {
			String initInfo=BCPay.initWechatPay(PayBaseActivity.this, HttpUtil.WECHATPAY_KEY);
			if (initInfo != null) {
				Toast.makeText(this, "微信初始化失败：" + initInfo, Toast.LENGTH_SHORT).show();
				Log.d("CCC","微信初始化失败：" + initInfo);
			}
		}catch (Exception e){
			Log.d("CCC",e.toString());
		}
		alipay_cb = (CheckBox) findViewById(R.id.alipay_cb);
		weixin_cb = (CheckBox) findViewById(R.id.weixin_cb);
		dongdon_cb = (CheckBox) findViewById(R.id.cb_company);
		customer_cb = (CheckBox) findViewById(R.id.cb_customer);
		offline_cb = (CheckBox) findViewById(R.id.cb_offline);

		weixin_cb.setChecked(true);

		alipay_ll = (LinearLayout) findViewById(R.id.ll_ali);
		weixin_ll = (LinearLayout) findViewById(R.id.ll_weixin);
		dongdon_ll = (LinearLayout) findViewById(R.id.ll_dongdong);
		customer_ll = (LinearLayout) findViewById(R.id.ll_customer);
		offline_ll = (LinearLayout) findViewById(R.id.ll_offline);

		balanceMoney = PreferenceManager.getInstance().getUserCoins();

//		getITopicApplication().getMyUserBeanManager().addOnCheckMoneyListener(this);
//		getITopicApplication().getMyUserBeanManager().startCheckMoneyRun();

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {

		}
	}

	
	protected void initListener() {
		alipay_ll.setOnClickListener(this);
		weixin_ll.setOnClickListener(this);
		dongdon_ll.setOnClickListener(this);
		customer_ll.setOnClickListener(this);
		offline_ll.setOnClickListener(this);

		alipay_cb.setOnClickListener(this);
		weixin_cb.setOnClickListener(this);
		dongdon_cb.setOnClickListener(this);
		customer_cb.setOnClickListener(this);
		offline_cb.setOnClickListener(this);
	}

	//开始支付
	protected void pay(String title,Map<String, String> mapOptional){
		if (moneyRequire == 0) {
			 //全用虚拟金钱抵消
			progress.show();
			payByIdeal(mapOptional);
			return;
		}

//		if(weixin_cb.isChecked()){
//			weixinPay(title, mapOptional);
//		}else if(alipay_cb.isChecked()){
//			aliPay(title, mapOptional);
//		}else{
//			showErrorToast("请选择一种支付方式");
//		}

		switch (payType) {
			case PAY_TYPE_WEIXIN:
				weixinPay(title, mapOptional);
				break;
			case PAY_TYPE_ALI:
				aliPay(title, mapOptional);
				break;
			case PAY_TYPE_DONGDONG:
				payBankCardPre(PAY_TYPE_DONGDONG);
				break;
			case PAY_TYPE_CUSTOMER:
				payBankCardPre(PAY_TYPE_CUSTOMER);
				break;
			case PAY_TYPE_OFFLINE:
				payBankCardPre(PAY_TYPE_OFFLINE);
				break;
			default:
				break;
		}
	}

	//子类重写
	public void payBankCardPre(int type) {

	}

	// 支付成功（无论是虚拟币还是支付渠道）,由子类处理
	protected void paySuccess() {}

	
	//让子类获取到当前用户余额
	protected float getBalanceMoney() {
		return balanceMoney;
	}

	
	
	private void payByIdeal(Map<String, String> mapOptional) {
		new Thread(new PayByIdealRun(mapOptional)).start();
	}

	private class PayByIdealRun implements Runnable {
		private Map<String, String> mapOptional;

		public PayByIdealRun(Map<String, String> mapOptional) {
			this.mapOptional = mapOptional;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			BaseResponse response = null;
			try {
				response = JsonParser.getBaseResponse(HttpUtil.postMsg(
						HttpUtil.getData(mapOptional), HttpUtil.IP
								+ "order/payideal"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (response == null) {
				response = new BaseResponse();
				response.setMessage("网络访问失败");
			}
			handler.sendMessage(handler
					.obtainMessage(NETWORK_IDEAPAY, response));
		}
	}

	private void weixinPay(String title, Map<String, String> mapOptional) {

		// 对于微信支付, 手机内存太小会有OutOfResourcesException造成的卡顿, 以致无法完成支付
		// 这个是微信自身存在的问题
		if (BCPay.isWXAppInstalledAndSupported() && BCPay.isWXPaySupported()) {
			progress.show();
			BCPay.getInstance(this).reqWXPaymentAsync(
			// 订单标题
					title, (int)(moneyRequire * 100), // 订单金额(分)
					genBillNum(), // 订单流水号
					mapOptional, // 扩展参数(可以null)
					bcCallback); // 支付完成后回调入口

		} else {
			showErrorToast("微信支付异常，请选择其他支付方式或稍后再试");
		}
	}

	private void aliPay(String title, Map<String, String> mapOptional) {
		progress.show();
		BCPay.getInstance(PayBaseActivity.this).reqAliPaymentAsync(title,
				(int)(moneyRequire * 100), // 订单金额(分)
				genBillNum(), // 订单流水号
				mapOptional, bcCallback);
	}

	private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
			"yyyyMMddHHmmssSSS", Locale.CHINA);

	private static String genBillNum() {
		return simpleDateFormat.format(new Date());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
//		getITopicApplication().getMyUserBeanManager().removeOnCheckMoneyListener(this);
		// 清理当前的activity引用
		BCPay.clear();
		// 使用微信的，在initWechatPay的activity结束时detach
		BCPay.detachWechat();
	}


	@Override
	public void onCheckMoneyFinish(FloatResponse response) {
		// TODO Auto-generated method stub
		if (response.isSuccess()) {
			balanceMoney = response.getData();
			getBalenceMoneyFromNetwork(balanceMoney);
		} else {
			showErrorToast(response.getMessage());
			getBalenceMoneyFromNetwork(0);
		}
	}
	
	
}
