/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 * 
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 * 
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.ui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.provider.MediaStore;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.sys.android.selfview.RecordButton;
import com.sys.android.selfview.RecordButton.OnFinishedRecordListener;
import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.SettingsManager.ChatsHideKeyboard;
import com.xabber.android.data.SettingsManager.SecurityOtrMode;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.archive.MessageArchiveManager;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.extension.muc.RoomState;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.SecurityLevel;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageItem;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.OnChatChangedListener;
import com.xabber.android.data.message.RegularChat;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.ui.adapter.ChatViewerAdapter;
import com.xabber.android.ui.adapter.OnTextChangedListener;
import com.xabber.android.ui.adapter.UploadUtil;
import com.xabber.android.ui.adapter.UploadUtil.OnUploadProcessListener;
import com.xabber.android.ui.dialog.ConfirmDialogListener;
import com.xabber.android.ui.dialog.DialogBuilder;
import com.xabber.android.ui.dialog.ExportChatDialogBuilder;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.android.ui.widget.PageSwitcher;
import com.xabber.android.ui.widget.PageSwitcher.OnSelectListener;
import com.xabber.androiddev.R;

/**
 * Chat activity.
 * 
 * Warning: {@link PageSwitcher} is to be removed and related implementation is
 * to be fixed.
 * 
 * @author alexander.ivanov
 * 
 */
public class ChatViewer extends ManagedActivity implements
		View.OnClickListener, View.OnKeyListener, OnSelectListener,
		OnChatChangedListener, OnContactChangedListener,
		OnAccountChangedListener, OnEditorActionListener,
		ConfirmDialogListener, OnTextChangedListener, Callback,
		OnUploadProcessListener{

	/**
	 * Attention request.
	 */
	private static final String ACTION_ATTENTION = "com.xabber.android.data.ATTENTION";

	/**
	 * Minimum number of new messages to be requested from the server side
	 * archive.
	 */
	private static final int MINIMUM_MESSAGES_TO_LOAD = 10;

	private static final String SAVED_ACCOUNT = "com.xabber.android.ui.ChatViewer.SAVED_ACCOUNT";
	private static final String SAVED_USER = "com.xabber.android.ui.ChatViewer.SAVED_USER";
	private static final String SAVED_EXIT_ON_SEND = "com.xabber.android.ui.ChatViewer.EXIT_ON_SEND";

	private static final int OPTION_MENU_VIEW_CONTACT_ID = 0x02;
	private static final int OPTION_MENU_CHAT_LIST_ID = 0x03;
	private static final int OPTION_MENU_CLOSE_CHAT_ID = 0x04;
	private static final int OPTION_MENU_SHOW_HISTORY_ID = 0x05;
	private static final int OPTION_MENU_SETTINGS_ID = 0x08;
	private static final int OPTION_MENU_CLEAR_HISTORY_ID = 0x09;
	private static final int OPTION_MENU_CLEAR_MESSAGE_ID = 0x0a;
	private static final int OPTION_MENU_EXPORT_CHAT_ID = 0x0c;
	private static final int OPTION_MENU_CALL_ATTENTION_ID = 0x0d;

	private static final int OPTION_MENU_LEAVE_ROOM_ID = 0x10;
	private static final int OPTION_MENU_JOIN_ROOM_ID = 0x11;
	private static final int OPTION_MENU_MUC_INVITE_ID = 0x12;
	private static final int OPTION_MENU_EDIT_ROOM_ID = 0x13;
	private static final int OPTION_MENU_OCCUPANT_LIST_ID = 0x14;

	private static final int OPTION_MENU_START_OTR_ID = 0x20;
	private static final int OPTION_MENU_END_OTR_ID = 0x21;
	private static final int OPTION_MENU_VERIFY_FINGERPRINT_ID = 0x22;
	private static final int OPTION_MENU_VERIFY_QUESTION_ID = 0x23;
	private static final int OPTION_MENU_VERIFY_SECRET_ID = 0x24;
	private static final int OPTION_MENU_REFRESH_OTR_ID = 0x25;

	private static final int CONTEXT_MENU_QUOTE_ID = 0x100;
	private static final int CONTEXT_MENU_REPEAT_ID = 0x101;
	private static final int CONTEXT_MENU_COPY_ID = 0x102;
	private static final int CONTEXT_MENU_REMOVE_ID = 0x103;

	private static final int DIALOG_EXPORT_CHAT_ID = 0x200;

	private ChatViewerAdapter chatViewerAdapter;
	private PageSwitcher pageSwitcher;

	private String actionWithAccount;
	private String actionWithUser;
	private View actionWithView;
	private MessageItem actionWithMessage;

	private boolean exitOnSend;

	private boolean isVisible;
	
	LinearLayout talkmessage_media;
	RecordButton mRecordButton;
	EditText chat_input;
	Button chat_send, voice_input_toggle;
	private static final int PICK_WITH_DATA = 3033;
	private static final int CAMERA_WITH_DATA = 3023;
	private static final int HANDLER_MSG_SEND_SUCCESS = 1;
	private static final int HANDLER_MSG_SEND_PERCENT = 2;
	
	private static final int UPLOAD_IN_PROCESS = 10;
	private static final int UPLOAD_INIT_PROCESS = 11;
	private static final int UPLOAD_FILE_DONE = 12;
	public static final int UPLOAD_SUCCESS_CODE = 1; //上传成功
	private static final int DOWNLOAD_FILE_DONE_SUC = 13;
	private static final int DOWNLOAD_FILE_DONE_FAI = 14;
	
	public String sCapturePath = "";
	private OutgoingFileTransfer sendTransfer;
	private Handler mHandler;
	String mImagePath = "";
	WebView webView;
	ImageView imageView2;
	ProgressDialog pd;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isFinishing())
			return;

		Intent intent = getIntent();
		String account = getAccount(intent);//qing@121.199.3.19/androidUBxmSpx7,  自己的账号
		String user = getUser(intent);//xjs@openfire,  对方的账号
		if (PageSwitcher.LOG)
			LogManager.i(this, "Intent: " + account + ":" + user);
		if (account == null || user == null) {
			Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
			finish();
			return;
		}
		if (hasAttention(intent))
			AttentionManager.getInstance().removeAccountNotifications(account, user);
		actionWithAccount = null;
		actionWithUser = null;
		actionWithView = null;
		actionWithMessage = null;
		mHandler = new Handler(this);

		setContentView(R.layout.chat_viewer);
		chatViewerAdapter = new ChatViewerAdapter(this, account, user);
		chatViewerAdapter.setOnClickListener(this);
		chatViewerAdapter.setOnKeyListener(this);
		chatViewerAdapter.setOnEditorActionListener(this);
		chatViewerAdapter.setOnCreateContextMenuListener(this);
		chatViewerAdapter.setOnTextChangedListener(this);
		pageSwitcher = (PageSwitcher) findViewById(R.id.switcher);
		pageSwitcher.setAdapter(chatViewerAdapter);
		pageSwitcher.setOnSelectListener(this);

		if (savedInstanceState != null) {
			actionWithAccount = savedInstanceState.getString(SAVED_ACCOUNT);
			actionWithUser = savedInstanceState.getString(SAVED_USER);
			exitOnSend = savedInstanceState.getBoolean(SAVED_EXIT_ON_SEND);
		}
		if (actionWithAccount == null)
			actionWithAccount = account;
		if (actionWithUser == null)
			actionWithUser = user;
		
		
		//actionWithAccount---qing@121.199.3.19/androidUBxmSpx7
		//actionWithUser---xjs@openfire
		selectChat(actionWithAccount, actionWithUser);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Application.getInstance().addUIListener(OnChatChangedListener.class,
				this);
		Application.getInstance().addUIListener(OnContactChangedListener.class,
				this);
		Application.getInstance().addUIListener(OnAccountChangedListener.class,
				this);
		chatViewerAdapter.onChange();
		if (actionWithView != null)
			chatViewerAdapter.onChatChange(actionWithView, false);
		Intent intent = getIntent();
		if (Intent.ACTION_SEND.equals(intent.getAction())) {
			String additional = intent.getStringExtra(Intent.EXTRA_TEXT);
			if (additional != null) {
				intent.removeExtra(Intent.EXTRA_TEXT);
				exitOnSend = true;
				if (actionWithView != null)
					insertText(additional);
			}
		}
		isVisible = true;
		showHistoryMsg();
		
		if ( actionWithView != null ){
			talkmessage_media = (LinearLayout) actionWithView.findViewById(R.id.talkmessage_media);
			mRecordButton = (RecordButton) actionWithView.findViewById(R.id.record_button);
			chat_input = (EditText) actionWithView.findViewById(R.id.chat_input);
			chat_send = (Button) actionWithView.findViewById(R.id.chat_send);
			voice_input_toggle = (Button) actionWithView.findViewById(R.id.voice_input_toggle);
			
			String path = ContactList.RECORD_ROOT_PATH;
			File file = new File(path);
			file.mkdirs();
			path += "/" + System.currentTimeMillis() + ".amr";
			mRecordButton.setSavePath(path);
			mRecordButton.setOnFinishedRecordListener(new OnFinishedRecordListener() {

				@Override
				public void onFinishedRecord(String audioPath, int time) {

					if (audioPath != null) {
						try {
//							// 自己显示消息
//							Msg myChatMsg = new Msg(pUSERID, time + "”",
//									TimeRender.getDate(), Msg.FROM_TYPE[1],
//									Msg.TYPE[0], Msg.STATUS[3], time + "",
//									audioPath);
//							listMsg.add(myChatMsg);
//							String[] pathStrings = audioPath.split("/"); // 文件名
//							
//							//发送 对方的消息
//							String fileName = null ;
//							if (pathStrings!=null && pathStrings.length>0) {
//								fileName = pathStrings[pathStrings.length-1];
//							}
//							Msg sendChatMsg = new Msg(pUSERID, time + "”",
//									TimeRender.getDate(), Msg.FROM_TYPE[0],
//									Msg.TYPE[0], Msg.STATUS[3], time + "",
//									fileName);
//							
//							// 刷新适配器
//							adapter.notifyDataSetChanged();
//
//							// 发送消息
//							newchat.sendMessage(Msg.toJson(sendChatMsg));
//							sendFile(audioPath, myChatMsg);
							
							String[] pathStrings = audioPath.split("/"); // 文件名
							String fileName = null ;
							if ( pathStrings != null && pathStrings.length > 0 ) {
								fileName = pathStrings[pathStrings.length-1];
							}
							sendMessage("attachment://record-"+ fileName);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
//						Toast.makeText(ChatActivity.this, "发送失败",	Toast.LENGTH_SHORT).show();
					}

				}
			});
			
			final ListView listView = (ListView) actionWithView.findViewById(android.R.id.list);
			webView = (WebView) actionWithView.findViewById(R.id.webview);
			listView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					actionWithMessage = (MessageItem) listView.getAdapter().getItem(position);
					
					if ( actionWithMessage.getText() != null && actionWithMessage.getText() != ""
							&& actionWithMessage.getText().startsWith(ContactList.prefix) ) {
						
						if ( actionWithMessage.getText().endsWith("amr") ){
							return;
						}
						
						String [] stp2 = actionWithMessage.getText().split("/");
						String filan = ContactList.IMAGE_ROOT_PATH + stp2[stp2.length - 1];
						if ( !actionWithMessage.isIncoming() &&  new File(filan).exists() ) {
							Intent intent = new Intent(Intent.ACTION_VIEW);
				            Uri mUri = Uri.parse("file://" + filan);
				            intent.setDataAndType(mUri, "image/*");
				            startActivity(intent);
						} else if (actionWithMessage.isIncoming()) {
							showPOP(actionWithMessage.getText());
//							WebSettings mWebSettings = webView.getSettings();
//			                mWebSettings.setBuiltInZoomControls(true);
//							
//							webView.setVisibility(View.VISIBLE);
//							webView.loadUrl(actionWithMessage.getText());
//							webView.setWebViewClient(new WebViewClientDemo());
//							Toast.makeText(ChatViewer.this, "加载图片中...", Toast.LENGTH_SHORT).show();
						}
						
					}
					
					
				}
			});
		}
		
	}


	public void showPOP(final String url) {
		pd = new ProgressDialog(ChatViewer.this);
		pd.setMessage("加载中...");
		pd.show();
		
		View subview2 = getLayoutInflater().inflate(R.layout.pop_search2, null);
		PopupWindow m_popupWindow = new PopupWindow(subview2, LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT,true);
		m_popupWindow.setFocusable(true);
		m_popupWindow.setOutsideTouchable(true);
		ColorDrawable dw = new ColorDrawable(-00000);
		m_popupWindow.setBackgroundDrawable(new BitmapDrawable());
		m_popupWindow.showAtLocation((View) webView, Gravity.TOP, 0, 0);
		
		imageView2 = (ImageView) subview2.findViewById(R.id.imageView2);
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Bitmap bitmap = getRemoteImage(new URL(url));
					Message msg = new Message();
					msg.what = DOWNLOAD_FILE_DONE_SUC;
					msg.obj = bitmap;
					mHandler.sendMessage(msg);
					
				} catch (MalformedURLException e) {
					Message msg = new Message();
					msg.what = DOWNLOAD_FILE_DONE_FAI;
					mHandler.sendMessage(msg);
					
					e.printStackTrace();
				} catch (Exception e) {
					Message msg = new Message();
					msg.what = DOWNLOAD_FILE_DONE_FAI;
					mHandler.sendMessage(msg);
				}
				
			}
		}).start();
		
		
	}
	
	public Bitmap getRemoteImage(final URL aURL) { 
        try { 
             final URLConnection conn = aURL.openConnection(); 
             conn.connect(); 
             final BufferedInputStream bis = new BufferedInputStream(conn.getInputStream()); 
             final Bitmap bm = BitmapFactory.decodeStream(bis); 
             bis.close();
             return bm; 
        } catch (IOException e) {
//        	Toast.makeText(getApplicationContext(), "图片加载失败", Toast.LENGTH_SHORT).show();
        }
        return null; 
   }
	
	
	
	class WebViewClientDemo extends WebViewClient { 
        @Override 
        // 在WebView中而不是默认浏览器中显示页面 
        public boolean shouldOverrideUrlLoading(WebView view, String url) { 
            view.loadUrl(url); 
            return true; 
        } 
    }
	
	//进行16进制编码
	public String changeToUnicode(String str) {
		StringBuffer strBuff = new StringBuffer();
		for (int i = 0; i < str.length(); i++) {
			String temp = Integer.toHexString(str.charAt(i));
			if (temp.length() != 4) {
				temp = "00" + temp;
			}
			if (temp.equals("00d")) {
				temp = "0" + temp;
			}
			if (temp.equals("00a")) {
				temp = "0" + temp;
			}
			strBuff.append(temp.substring(0, temp.length() - 2));
			strBuff.append(temp.substring(temp.length() - 2, temp.length()));
		}
		String returnData = strBuff.toString();
		return returnData;
	}
	
	//进行16进制解码
	public String changeToChineseWord(String str) {
		String retData = null;
		String tempStr = new String(str);
		String[] chStr = new String[str.length() / 4];
		for (int i = 0; i < str.length(); i++) {
			if (i % 4 == 3) {
				chStr[i / 4] = new String(tempStr.substring(0, 4));
				tempStr = tempStr.substring(4, tempStr.length());
			}
		}
		char[] retChar = new char[chStr.length];
		for (int i = 0; i < chStr.length; i++) {
			retChar[i] = (char) Integer.parseInt(chStr[i], 16);
		}
		retData = String.valueOf(retChar, 0, retChar.length);
		return retData;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (PageSwitcher.LOG)
			LogManager.i(this, "onSave: " + actionWithAccount + ":"
					+ actionWithUser);
		outState.putString(SAVED_ACCOUNT, actionWithAccount);
		outState.putString(SAVED_USER, actionWithUser);
		outState.putBoolean(SAVED_EXIT_ON_SEND, exitOnSend);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Application.getInstance().removeUIListener(OnChatChangedListener.class,
				this);
		Application.getInstance().removeUIListener(
				OnContactChangedListener.class, this);
		Application.getInstance().removeUIListener(
				OnAccountChangedListener.class, this);
		MessageManager.getInstance().removeVisibleChat();
		pageSwitcher.saveState();
		isVisible = false;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (isFinishing())
			return;

		String account = getAccount(intent);
		String user = getUser(intent);
		if (account == null || user == null) {
			Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
			return;
		}
		if (hasAttention(intent))
			AttentionManager.getInstance().removeAccountNotifications(account,
					user);

		chatViewerAdapter.onChange();
		if (!selectChat(account, user))
			Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.clear();
		AbstractChat abstractChat = MessageManager.getInstance().getChat(
				actionWithAccount, actionWithUser);
		if (abstractChat != null && abstractChat instanceof RoomChat) {
			if (((RoomChat) abstractChat).getState() == RoomState.unavailable)
				menu.add(0, OPTION_MENU_JOIN_ROOM_ID, 0,
						getResources().getText(R.string.muc_join)).setIcon(
						android.R.drawable.ic_menu_add);
			else
				menu.add(0, OPTION_MENU_MUC_INVITE_ID, 0,
						getResources().getText(R.string.muc_invite)).setIcon(
						android.R.drawable.ic_menu_add);
		} else {
			menu.add(0, OPTION_MENU_VIEW_CONTACT_ID, 0,
					getResources().getText(R.string.contact_editor)).setIcon(
					android.R.drawable.ic_menu_edit);
		}
		menu.add(0, OPTION_MENU_CHAT_LIST_ID, 0, getText(R.string.chat_list))
				.setIcon(R.drawable.ic_menu_friendslist);
		menu.add(0, OPTION_MENU_SETTINGS_ID, 0, getText(R.string.chat_settings))
				.setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, OPTION_MENU_SHOW_HISTORY_ID, 0,
				getText(R.string.show_history)).setIcon(
				R.drawable.ic_menu_archive);
		if (abstractChat != null
				&& abstractChat instanceof RoomChat
				&& ((RoomChat) abstractChat).getState() != RoomState.unavailable) {
			if (((RoomChat) abstractChat).getState() == RoomState.error)
				menu.add(0, OPTION_MENU_EDIT_ROOM_ID, 0,
						getResources().getText(R.string.muc_edit)).setIcon(
						android.R.drawable.ic_menu_edit);
			else
				menu.add(0, OPTION_MENU_LEAVE_ROOM_ID, 0,
						getResources().getText(R.string.muc_leave)).setIcon(
						android.R.drawable.ic_menu_close_clear_cancel);
		} else {
			menu.add(0, OPTION_MENU_CLOSE_CHAT_ID, 0,
					getResources().getText(R.string.close_chat)).setIcon(
					android.R.drawable.ic_menu_close_clear_cancel);
		}
		menu.add(0, OPTION_MENU_CLEAR_MESSAGE_ID, 0,
				getResources().getText(R.string.clear_message)).setIcon(
				R.drawable.ic_menu_stop);
		menu.add(0, OPTION_MENU_CLEAR_HISTORY_ID, 0,
				getText(R.string.clear_history));
		menu.add(0, OPTION_MENU_EXPORT_CHAT_ID, 0,
				getText(R.string.export_chat));
		if (abstractChat != null && abstractChat instanceof RegularChat) {
			menu.add(0, OPTION_MENU_CALL_ATTENTION_ID, 0,
					getText(R.string.call_attention));
			SecurityLevel securityLevel = OTRManager.getInstance()
					.getSecurityLevel(abstractChat.getAccount(),
							abstractChat.getUser());
			SubMenu otrMenu = menu.addSubMenu(getText(R.string.otr_encryption));
			otrMenu.setHeaderTitle(R.string.otr_encryption);
			if (securityLevel == SecurityLevel.plain)
				otrMenu.add(0, OPTION_MENU_START_OTR_ID, 0,
						getText(R.string.otr_start))
						.setEnabled(
								SettingsManager.securityOtrMode() != SecurityOtrMode.disabled);
			else
				otrMenu.add(0, OPTION_MENU_REFRESH_OTR_ID, 0,
						getText(R.string.otr_refresh));
			otrMenu.add(0, OPTION_MENU_END_OTR_ID, 0, getText(R.string.otr_end))
					.setEnabled(securityLevel != SecurityLevel.plain);
			otrMenu.add(0, OPTION_MENU_VERIFY_FINGERPRINT_ID, 0,
					getText(R.string.otr_verify_fingerprint)).setEnabled(
					securityLevel != SecurityLevel.plain);
			otrMenu.add(0, OPTION_MENU_VERIFY_QUESTION_ID, 0,
					getText(R.string.otr_verify_question)).setEnabled(
					securityLevel != SecurityLevel.plain);
			otrMenu.add(0, OPTION_MENU_VERIFY_SECRET_ID, 0,
					getText(R.string.otr_verify_secret)).setEnabled(
					securityLevel != SecurityLevel.plain);
		}
		if (abstractChat != null && abstractChat instanceof RoomChat
				&& ((RoomChat) abstractChat).getState() == RoomState.available)
			menu.add(0, OPTION_MENU_OCCUPANT_LIST_ID, 0, getResources()
					.getText(R.string.occupant_list));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case OPTION_MENU_VIEW_CONTACT_ID:
			startActivity(ContactEditor.createIntent(this, actionWithAccount,
					actionWithUser));
			return true;
		case OPTION_MENU_CHAT_LIST_ID:
			startActivity(ChatList.createIntent(this));
			return true;
		case OPTION_MENU_CLOSE_CHAT_ID:
			MessageManager.getInstance().closeChat(actionWithAccount,
					actionWithUser);
			NotificationManager.getInstance().removeMessageNotification(
					actionWithAccount, actionWithUser);
			close();
			return true;
		case OPTION_MENU_CLEAR_HISTORY_ID:
			MessageManager.getInstance().clearHistory(actionWithAccount,
					actionWithUser);
			chatViewerAdapter.onChatChange(actionWithView, false);
			return true;
		case OPTION_MENU_SHOW_HISTORY_ID:
			showHistoryMsg();
			return true;
		case OPTION_MENU_SETTINGS_ID:
			startActivity(ChatEditor.createIntent(this, actionWithAccount,
					actionWithUser));
			return true;
		case OPTION_MENU_CLEAR_MESSAGE_ID:
			((EditText) actionWithView.findViewById(R.id.chat_input))
					.setText("");
			return true;
		case OPTION_MENU_EXPORT_CHAT_ID:
			showDialog(DIALOG_EXPORT_CHAT_ID);
			return true;
		case OPTION_MENU_CALL_ATTENTION_ID:
			try {
				AttentionManager.getInstance().sendAttention(actionWithAccount,
						actionWithUser);
			} catch (NetworkException e) {
				Application.getInstance().onError(e);
			}
			return true;
		case OPTION_MENU_JOIN_ROOM_ID:
			MUCManager.getInstance().joinRoom(actionWithAccount,
					actionWithUser, true);
			return true;
		case OPTION_MENU_LEAVE_ROOM_ID:
			MUCManager.getInstance().leaveRoom(actionWithAccount,
					actionWithUser);
			MessageManager.getInstance().closeChat(actionWithAccount,
					actionWithUser);
			NotificationManager.getInstance().removeMessageNotification(
					actionWithAccount, actionWithUser);
			close();
			return true;
		case OPTION_MENU_MUC_INVITE_ID:
			startActivity(ContactList.createRoomInviteIntent(this,
					actionWithAccount, actionWithUser));
			return true;
		case OPTION_MENU_EDIT_ROOM_ID:
			startActivity(MUCEditor.createIntent(this, actionWithAccount,
					actionWithUser));
			return true;
		case OPTION_MENU_OCCUPANT_LIST_ID:
			startActivity(OccupantList.createIntent(this, actionWithAccount,
					actionWithUser));
			return true;
		case OPTION_MENU_START_OTR_ID:
			try {
				OTRManager.getInstance().startSession(actionWithAccount,
						actionWithUser);
			} catch (NetworkException e) {
				Application.getInstance().onError(e);
			}
			return true;
		case OPTION_MENU_REFRESH_OTR_ID:
			try {
				OTRManager.getInstance().refreshSession(actionWithAccount,
						actionWithUser);
			} catch (NetworkException e) {
				Application.getInstance().onError(e);
			}
			return true;
		case OPTION_MENU_END_OTR_ID:
			try {
				OTRManager.getInstance().endSession(actionWithAccount,
						actionWithUser);
			} catch (NetworkException e) {
				Application.getInstance().onError(e);
			}
			return true;
		case OPTION_MENU_VERIFY_FINGERPRINT_ID:
			startActivity(FingerprintViewer.createIntent(this,
					actionWithAccount, actionWithUser));
			return true;
		case OPTION_MENU_VERIFY_QUESTION_ID:
			startActivity(QuestionViewer.createIntent(this, actionWithAccount,
					actionWithUser, true, false, null));
			return true;
		case OPTION_MENU_VERIFY_SECRET_ID:
			startActivity(QuestionViewer.createIntent(this, actionWithAccount,
					actionWithUser, false, false, null));
			return true;
		}
		return false;
	}

	public void showHistoryMsg(){
		MessageManager.getInstance().requestToLoadLocalHistory(
				actionWithAccount, actionWithUser);
		MessageArchiveManager.getInstance().requestHistory(
				actionWithAccount, actionWithUser,
				MINIMUM_MESSAGES_TO_LOAD, 0);
		chatViewerAdapter.onChange();
		chatViewerAdapter.onChatChange(actionWithView, false);
	}
	
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		pageSwitcher.stopMovement();

		ListView listView = (ListView) actionWithView.findViewById(android.R.id.list);
		actionWithMessage = (MessageItem) listView.getAdapter().getItem(
				info.position);
		if (actionWithMessage != null && actionWithMessage.getAction() != null)
			actionWithMessage = null; // Skip action message
		if (actionWithMessage == null)
			return;
		if (actionWithMessage.isError()) {
			menu.add(0, CONTEXT_MENU_REPEAT_ID, 0,
					getResources().getText(R.string.message_repeat));
		}
//		menu.add(0, CONTEXT_MENU_QUOTE_ID, 0,
//				getResources().getText(R.string.message_quote));
//		menu.add(0, CONTEXT_MENU_COPY_ID, 0,
//				getResources().getText(R.string.message_copy));
		menu.add(0, CONTEXT_MENU_REMOVE_ID, 0,
				getResources().getText(R.string.message_remove));
	}

	/**
	 * Insert additional text to the input.
	 * 
	 * @param additional
	 */
	private void insertText(String additional) {
		EditText editView = (EditText) actionWithView
				.findViewById(R.id.chat_input);
		String source = editView.getText().toString();
		int selection = editView.getSelectionEnd();
		if (selection == -1)
			selection = source.length();
		else if (selection > source.length())
			selection = source.length();
		String before = source.substring(0, selection);
		String after = source.substring(selection);
		if (before.length() > 0 && !before.endsWith("\n"))
			additional = "\n" + additional;
		editView.setText(before + additional + after);
		editView.setSelection(selection + additional.length());
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (actionWithMessage == null)
			return false;
		super.onContextItemSelected(item);
		switch (item.getItemId()) {
		case CONTEXT_MENU_QUOTE_ID:
			insertText("> " + actionWithMessage.getText() + "\n");
			return true;
		case CONTEXT_MENU_REPEAT_ID:
			sendMessage(actionWithMessage.getText());
			return true;
		case CONTEXT_MENU_COPY_ID:
			((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
					.setText(actionWithMessage.getSpannable());
			return true;
		case CONTEXT_MENU_REMOVE_ID:
			MessageManager.getInstance().removeMessage(actionWithMessage);
			chatViewerAdapter.onChatChange(actionWithView, false);
			return true;
		}
		return false;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		super.onCreateDialog(id);
		switch (id) {
		case DIALOG_EXPORT_CHAT_ID:
			return new ExportChatDialogBuilder(this, DIALOG_EXPORT_CHAT_ID,
					this, actionWithAccount, actionWithUser).create();
		default:
			return null;
		}
	}

	@Override
	public void onClick(View view) {
		Intent intent = null;
		
		switch (view.getId()) {
		case R.id.chat_send:
			sendMessage();
			break;
		case R.id.more_function_btn:
			media_toggle();
			break;
		case R.id.talkmessage_pic:
			intent = new Intent(Intent.ACTION_PICK, null);
			intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
			startActivityForResult(intent, PICK_WITH_DATA);
			media_toggle();
			break;
			
		case R.id.talkmessage_camera:
//			MessageManager.getInstance().getChat(
//					actionWithAccount, actionWithUser)
//					.newMessage("", "发送失败", ChatAction.status, null, false, false, false, false, true);
			
			Date date = new Date(System.currentTimeMillis());
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			sCapturePath = ContactList.IMAGE_ROOT_PATH + dateFormat.format(date)	+ ".jpg";
			
			intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			File file = new File(sCapturePath);
			if (!file.exists()) {
				try {
					file.createNewFile();
				} catch (IOException e) {

				}
			}
			intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
			startActivityForResult(intent, CAMERA_WITH_DATA);
			media_toggle();
			break;
			
		case R.id.voice_input_toggle:
			if ( mRecordButton.getVisibility() == View.GONE ) {
				mRecordButton.setVisibility(View.VISIBLE);
				chat_input.setVisibility(View.GONE);
				chat_send.setVisibility(View.GONE);
				voice_input_toggle.setBackgroundResource(R.drawable.media_toggle_btn_a);
			}else {
				mRecordButton.setVisibility(View.GONE);
				chat_input.setVisibility(View.VISIBLE);
				chat_send.setVisibility(View.VISIBLE);
				voice_input_toggle.setBackgroundResource(R.drawable.media_toggle_btn_b);
			}
			talkmessage_media.setVisibility(View.GONE);
			break;
		case R.id.title:
			ListView listView = (ListView) actionWithView.findViewById(android.R.id.list);
			int size = listView.getCount();
			if (size > 0)
				listView.setSelection(size - 1);
		default:
			break;
		}
	}
	
	public void media_toggle(){
		
		if ( talkmessage_media.getVisibility() == View.VISIBLE ) {
			talkmessage_media.setVisibility(View.GONE);
		}else {
			talkmessage_media.setVisibility(View.VISIBLE);
		}
	}

	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		Uri uri = null;
		switch (requestCode) {
		case CAMERA_WITH_DATA:
			if (resultCode == RESULT_OK) {
				if (!Environment.getExternalStorageState().equals(
						Environment.MEDIA_MOUNTED)) {
					Toast.makeText(this, "SD卡不可用", Toast.LENGTH_SHORT).show();
					return;
				}else {
//					sendFile(sCapturePath);
					sendFile(doCompressBitmap(sCapturePath));
					mImagePath = sCapturePath;
				}
			} else {
				Toast.makeText(this, "取消拍照", Toast.LENGTH_SHORT).show();
			}
			
			break;
			
		case PICK_WITH_DATA:
			mImagePath = "";
			
			if (data == null) {
				Toast.makeText(this, "取消发送", Toast.LENGTH_SHORT).show();
				return;
			}
			if (resultCode == RESULT_OK) {
				if (!Environment.getExternalStorageState().equals(
						Environment.MEDIA_MOUNTED)) {
					Toast.makeText(this, "SD卡不可用", Toast.LENGTH_SHORT).show();
					return;
				}
				uri = data.getData();
				String[] proj = { MediaStore.Images.Media.DATA };
				Cursor cursor = managedQuery(uri, proj, null, null, null);
				if (cursor != null) {
					int column_index = cursor
							.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
					if (cursor.getCount() > 0 && cursor.moveToFirst()) {
						mImagePath = cursor.getString(column_index);
						if (mImagePath != null) {
							sendFile(doCompressBitmap(mImagePath));

						} else {
							Toast.makeText(this, "图片未找到", Toast.LENGTH_SHORT)
									.show();
						}
					} else {
						Toast.makeText(this, "图片未找到", Toast.LENGTH_SHORT)
								.show();
					}
				} else {
					Toast.makeText(this, "图片未找到", Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(this, "获取错误", Toast.LENGTH_SHORT).show();
			}
			
			break;
			
		}
		
		
	}
	
	public String doCompressBitmap(String mImagePath) {
		Toast.makeText(this, "压缩图片中...", Toast.LENGTH_SHORT).show();
		Bitmap bitmap = null;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		Bitmap bitmap2 = BitmapFactory.decodeFile(mImagePath, options);
//		Log.e("Test", "Bitmap Height == " + options.outHeight);
//		Log.e("Test", "Bitmap Width == " + options.outWidth);
		
		if ( options.outHeight < ContactList.imageMaxHW || options.outWidth < ContactList.imageMaxHW ){
			bitmap = BitmapFactory.decodeFile(mImagePath);
			
		}else {
			
			int intnt = 2;
			while ( true ) {
				Options op = new Options();
		        op.inJustDecodeBounds = true;
		        bitmap = BitmapFactory.decodeFile(mImagePath, op);
		        op.inSampleSize = intnt; 
		        op.inJustDecodeBounds = false; 
		        bitmap = BitmapFactory.decodeFile(mImagePath, op);
		        
		        if ( bitmap != null ){
		        	intnt = intnt+1;
		        	if ( bitmap.getWidth() < ContactList.imageMaxHW || bitmap.getHeight() < ContactList.imageMaxHW ) {
			        	break;
			        }
		        }
			}
		}
		
		

		String[] st2 = mImagePath.split("/");
		String filans = st2[st2.length-1];
		try {
			return saveMyBitmap(filans, bitmap);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	
	public String saveMyBitmap(String bitName, Bitmap mBitmap) throws IOException {
		//ContactList.IMAGE_ROOT_PATH
		File f = new File(ContactList.IMAGE_ROOT_PATH + bitName);
		if ( f.exists() ){
			f.delete();
		}
		
		f.createNewFile();
		
		FileOutputStream fOut = null;
		try {
			fOut = new FileOutputStream(f);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
		try {
			fOut.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			fOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return f.getPath();
//		sendFile(f.getPath());
	}
	
	
	public void sendFile(String sPath) {
		String fileKey = "img";
		UploadUtil uploadUtil = UploadUtil.getInstance();
		uploadUtil.setOnUploadProcessListener(ChatViewer.this);  //设置监听器监听上传状态
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("orderId", "111");
		uploadUtil.uploadFile(sPath ,fileKey, "http://121.199.3.19:8080/eFlowIM/p/file!upload",params);
		
		
		/*xmpp的方式发送文件
		XMPPConnection xmppConnection = null;
		for (String account : AccountManager.getInstance().getAccounts()) {
			ConnectionThread connectionThread = AccountManager
					.getInstance().getAccount(account)
					.getConnectionThread();
			if (connectionThread == null)
				continue;
			xmppConnection = connectionThread.getXMPPConnection();
			if (xmppConnection == null)
				continue;
		}
		
		final FileTransferManager sendFilemanager = new FileTransferManager(xmppConnection);
		sendTransfer = sendFilemanager.createOutgoingFileTransfer(actionWithUser + "/Smack");//sun@openfire/Smack
		// Send the file
		try {
			sendTransfer.sendFile(new java.io.File(sPath), "send file");
			new MyFileStatusThread(sendTransfer).start();
			*//**
			 * 监听
			 *//*
		} catch (XMPPException e) {
			e.printStackTrace();
		}*/
	}
	
	class MyFileStatusThread extends Thread {
		private FileTransfer transfer;

		public MyFileStatusThread(FileTransfer tf) {
			transfer = tf;
		}

		public void run() {
			System.out.println("status: "+transfer.getStatus());
			System.out.println("progress: "+transfer.getProgress());
			android.os.Message message = new android.os.Message();// handle
			message.what = 3;
			while (!transfer.isDone()) {
				System.out.println("status: "+ transfer.getStatus());
				int dds = (int)transfer.getProgress() * 100;
				System.out.println("progress: "+ dds + "%");
				
				Message msg = new Message();
				msg.what = HANDLER_MSG_SEND_PERCENT;
				msg.arg1 = dds;
				mHandler.sendMessage(msg);
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			if (transfer.getStatus().equals(Status.error)) {
				Log.i("", "error");
			} else if (transfer.getStatus().equals(Status.refused)) {
				Log.i("", "refuse");
			} else {
				Message msg = new Message();
				msg.what = HANDLER_MSG_SEND_SUCCESS;
				msg.obj = transfer.getFileName();
				mHandler.sendMessage(msg);
//				Log.i("", "suecess");
			}

		}
	}
	
	
	@Override
	public boolean onKey(View view, int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN
				&& keyCode == KeyEvent.KEYCODE_ENTER
				&& SettingsManager.chatsSendByEnter()) {
			sendMessage();
			return true;
		}
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
//			if ( webView.getVisibility() == View.VISIBLE ){
//				webView.setVisibility(View.GONE);
//			}else if ( webView.getVisibility() == View.GONE ){
//				close();
//			}
			
			close();
			
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void close() {
		finish();
		if (!Intent.ACTION_SEND.equals(getIntent().getAction())) {
			ActivityManager.getInstance().clearStack(false);
			if (!ActivityManager.getInstance().hasContactList(this))
				startActivity(ContactList.createIntent(this));
		}
	}

	@Override
	public void onTextChanged(EditText editText, CharSequence text) {
		ChatStateManager.getInstance().onComposing(actionWithAccount,
				actionWithUser, text);
	}

	@Override
	public void onSelect() {
		BaseEntity contactItem = (BaseEntity) pageSwitcher.getSelectedItem();
		actionWithAccount = contactItem.getAccount();
		actionWithUser = contactItem.getUser();
		if (PageSwitcher.LOG)
			LogManager.i(this, "onSelect: " + actionWithAccount + ":"
					+ actionWithUser);
		actionWithView = pageSwitcher.getSelectedView();
		actionWithMessage = null;
		if (isVisible)
			MessageManager.getInstance().setVisibleChat(actionWithAccount,
					actionWithUser);
		MessageArchiveManager.getInstance().requestHistory(
				actionWithAccount,
				actionWithUser,
				0,
				MessageManager.getInstance()
						.getChat(actionWithAccount, actionWithUser)
						.getRequiredMessageCount());
		NotificationManager.getInstance().removeMessageNotification(
				actionWithAccount, actionWithUser);
	}

	@Override
	public void onUnselect() {
		actionWithAccount = null;
		actionWithUser = null;
		actionWithView = null;
		actionWithMessage = null;
		if (PageSwitcher.LOG)
			LogManager.i(this, "onUnselect");
	}

	private void sendMessage() {
		if (actionWithView == null)
			return;
		EditText editView = (EditText) actionWithView.findViewById(R.id.chat_input);
		String text = editView.getText().toString();
		int start = 0;
		int end = text.length();
		while (start < end
				&& (text.charAt(start) == ' ' || text.charAt(start) == '\n'))
			start += 1;
		while (start < end
				&& (text.charAt(end - 1) == ' ' || text.charAt(end - 1) == '\n'))
			end -= 1;
		text = text.substring(start, end);
		if ("".equals(text))
			return;
		chatViewerAdapter.setOnTextChangedListener(null);
		editView.setText("");
		chatViewerAdapter.setOnTextChangedListener(this);
		sendMessage(text);
		
		if (exitOnSend)
			close();
		if (SettingsManager.chatsHideKeyboard() == ChatsHideKeyboard.always
				|| (getResources().getBoolean(R.bool.landscape) && SettingsManager
						.chatsHideKeyboard() == ChatsHideKeyboard.landscape)) {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(editView.getWindowToken(), 0);
		}
	}

	private void sendMessage(String text) {
		MessageManager.getInstance().sendMessage(actionWithAccount,
				actionWithUser, text);
		chatViewerAdapter.onChatChange(actionWithView, false);
	}
	
	@Override
	public void onChatChanged(final String account, final String user,
			final boolean incoming) {
		BaseEntity baseEntity;
		baseEntity = (BaseEntity) pageSwitcher.getSelectedItem();
		if (baseEntity != null && baseEntity.equals(account, user)) {
			chatViewerAdapter.onChatChange(pageSwitcher.getSelectedView(),
					incoming);
			return;
		}
		baseEntity = (BaseEntity) pageSwitcher.getVisibleItem();
		if (baseEntity != null && baseEntity.equals(account, user)) {
			chatViewerAdapter.onChatChange(pageSwitcher.getVisibleView(),
					incoming);
			return;
		}
		// Search for chat in adapter.
		final int count = chatViewerAdapter.getCount();
		for (int index = 0; index < count; index++)
			if (((BaseEntity) chatViewerAdapter.getItem(index)).equals(account,
					user))
				return;
		// New chat.
		chatViewerAdapter.onChange();
	}

	@Override
	public void onContactsChanged(Collection<BaseEntity> entities) {
		BaseEntity baseEntity;
		baseEntity = (BaseEntity) pageSwitcher.getSelectedItem();
		if (baseEntity != null && entities.contains(baseEntity)) {
			chatViewerAdapter.onChange();
			return;
		}
		baseEntity = (BaseEntity) pageSwitcher.getVisibleItem();
		if (baseEntity != null && entities.contains(baseEntity)) {
			chatViewerAdapter.onChange();
			return;
		}
	}

	@Override
	public void onAccountsChanged(Collection<String> accounts) {
		BaseEntity baseEntity;
		baseEntity = (BaseEntity) pageSwitcher.getSelectedItem();
		if (baseEntity != null && accounts.contains(baseEntity.getAccount())) {
			chatViewerAdapter.onChange();
			return;
		}
		baseEntity = (BaseEntity) pageSwitcher.getVisibleItem();
		if (baseEntity != null && accounts.contains(baseEntity.getAccount())) {
			chatViewerAdapter.onChange();
			return;
		}
	}

	@Override
	public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
		if (actionId == EditorInfo.IME_ACTION_SEND) {
			sendMessage();
			return true;
		}
		return false;
	}

	@Override
	public void onAccept(DialogBuilder dialogBuilder) {
		switch (dialogBuilder.getDialogId()) {
		case DIALOG_EXPORT_CHAT_ID:
			ExportChatDialogBuilder builder = (ExportChatDialogBuilder) dialogBuilder;
			exportChat(builder);
		}
	}
	
	private void exportChat(ExportChatDialogBuilder dialogBuilder) {
		//TODO: retain AsyncTask via retained fragment
		new ChatExportAsyncTask(dialogBuilder).execute();
	}
	
	private class ChatExportAsyncTask extends AsyncTask<Void, Void, File> {
		private ExportChatDialogBuilder builder;
		
		public ChatExportAsyncTask(ExportChatDialogBuilder builder) {
			this.builder = builder;
		}
		
		@Override
		protected File doInBackground(Void... params) {
			File file = null;
			try {
				file = MessageManager.getInstance().exportChat(
						actionWithAccount, actionWithUser, builder.getName());
			} catch (NetworkException e) {
				Application.getInstance().onError(e);
			}
			return file;
		}
		
		@Override
		public void onPostExecute(File result) {
			if (result != null) {
				if (builder.isSendChecked()) {
					Intent intent = new Intent(android.content.Intent.ACTION_SEND);
					intent.setType("text/plain");
					Uri uri = Uri.fromFile(result);
					intent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
					startActivity(Intent.createChooser(intent,
							getString(R.string.export_chat)));
				} else {
					Toast.makeText(ChatViewer.this, R.string.export_chat_done,
							Toast.LENGTH_LONG).show();
				}
			}
		}
		
	}

	@Override
	public void onDecline(DialogBuilder dialogBuilder) {
	}

	@Override
	public void onCancel(DialogBuilder dialogBuilder) {
	}

	private boolean selectChat(String account, String user) {
		for (int position = 0; position < chatViewerAdapter.getCount(); position++)
			if (((BaseEntity) chatViewerAdapter.getItem(position)).equals(
					account, user)) {
				if (PageSwitcher.LOG)
					LogManager.i(this, "setSelection: " + position + ", "
							+ account + ":" + user);
				pageSwitcher.setSelection(position);
				return true;
			}
		if (PageSwitcher.LOG)
			LogManager.i(this, "setSelection: not found, " + account + ":"
					+ user);
		return false;
	}

	public static Intent createIntent(Context context, String account,
			String user) {
		return new EntityIntentBuilder(context, ChatViewer.class)
				.setAccount(account).setUser(user).build();
	}

	public static Intent createClearTopIntent(Context context, String account,
			String user) {
		Intent intent = createIntent(context, account, user);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	/**
	 * Create intent to send message.
	 * 
	 * Contact list will not be shown on when chat will be closed.
	 * 
	 * @param context
	 * @param account
	 * @param user
	 * @param text
	 *            if <code>null</code> then user will be able to send a number
	 *            of messages. Else only one message can be send.
	 * @return
	 */
	public static Intent createSendIntent(Context context, String account,
			String user, String text) {
		Intent intent = ChatViewer.createIntent(context, account, user);
		intent.setAction(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_TEXT, text);
		return intent;
	}

	public static Intent createAttentionRequestIntent(Context context,
			String account, String user) {
		Intent intent = ChatViewer.createClearTopIntent(context, account, user);
		intent.setAction(ACTION_ATTENTION);
		return intent;
	}

	private static String getAccount(Intent intent) {
		String value = EntityIntentBuilder.getAccount(intent);
		if (value != null)
			return value;
		// Backward compatibility.
		return intent.getStringExtra("com.xabber.android.data.account");
	}

	private static String getUser(Intent intent) {
		String value = EntityIntentBuilder.getUser(intent);
		if (value != null)
			return value;
		// Backward compatibility.
		return intent.getStringExtra("com.xabber.android.data.user");
	}

	private static boolean hasAttention(Intent intent) {
		return ACTION_ATTENTION.equals(intent.getAction());
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case HANDLER_MSG_SEND_SUCCESS:
//			sendMessage("attachment://image-"+ fileName);
			String imagePath = ContactList.IMAGE_ROOT_PATH + msg.obj.toString();
//			Log.i("", "imagePath: "+imagePath);
			Bitmap bt;
			int intnt = 2;
			while ( true ) {
				Options op = new Options();
		        op.inJustDecodeBounds = true;
		        bt = BitmapFactory.decodeFile(imagePath, op);
//		        Log.i("", bt.getWidth() + " # " + bt.getHeight());
		        op.inSampleSize = intnt; 
		        op.inJustDecodeBounds = false; 
		        bt = BitmapFactory.decodeFile(imagePath, op);
		        
		        Log.i("", bt.getWidth() + " = " + bt.getHeight());
		        
		        if ( bt != null ){
		        	intnt = intnt+1;
		        	if ( bt.getWidth() < 240 || bt.getHeight() < 240 ) {
//			        	Log.i("", bt.getWidth() + " * " + bt.getHeight());
			        	break;
			        }
		        }
		        
			}
			
			Drawable drawable = new BitmapDrawable(bt); 
			ImageView bitmapSC = (ImageView) actionWithView.findViewById(R.id.bitmapSC);
			bitmapSC.setBackgroundDrawable(drawable);
			TextView bitmapTX = (TextView) actionWithView.findViewById(R.id.bitmapTX);
			bitmapTX.setVisibility(View.GONE);
			
			MessageManager.getInstance().getChat(
					actionWithAccount, actionWithUser).sendMessages();
			
			break;
			
		case HANDLER_MSG_SEND_PERCENT:
			int dds = msg.arg1;
			chatViewerAdapter.setPercent(actionWithView, dds);
			break;

		case UPLOAD_IN_PROCESS:
//			Log.i("", "in...  "+ msg.arg1);
			break;
			
		case UPLOAD_INIT_PROCESS:
			Toast.makeText(this, "开始发送...", Toast.LENGTH_SHORT).show();
			break;
			
		case UPLOAD_FILE_DONE:
			String result = "响应码："+msg.arg1+"\n响应信息："+msg.obj+"\n耗时：" + UploadUtil.getRequestTime()+"秒";
			Log.i("", "finish...  "+ result);
			
			String[] st2 = mImagePath.split("/");
			String filans = "http://121.199.3.19:8080/share/"+st2[st2.length-1];
//			Log.i("", "filans...  "+ filans);
			
			if ( msg.arg1 == UPLOAD_SUCCESS_CODE ){//上传成功后再发送消息出去
				sendMessage(filans);
			}else {
				Toast.makeText(ChatViewer.this, "发送失败", Toast.LENGTH_SHORT).show();
			}
			
			
//			MessageItem mItem = MessageManager.getInstance().sendMessage2(actionWithAccount,
//					actionWithUser, "attachment://image-"+  filans);
//			chatViewerAdapter.onChatChange(actionWithView, false);
			
			break;
			
		case DOWNLOAD_FILE_DONE_SUC:
			Bitmap bp = (Bitmap)msg.obj;
			imageView2.setImageBitmap(bp);
			if ( pd.isShowing() ){
				pd.dismiss();
			}
			break;
			
		case DOWNLOAD_FILE_DONE_FAI:
			Toast.makeText(getApplicationContext(), "图片加载失败", Toast.LENGTH_SHORT).show();
			break;
			
		default:
			break;
		}
		return false;
	}

	@Override
	public void onUploadDone(int responseCode, String message) {
		Message msg = Message.obtain();
		msg.what = UPLOAD_FILE_DONE;
		msg.arg1 = responseCode;
		msg.obj = message;
		mHandler.sendMessage(msg);
	}

	@Override
	public void onUploadProcess(int uploadSize) {
		// TODO Auto-generated method stub
		Message msg = Message.obtain();
		msg.what = UPLOAD_IN_PROCESS;
		msg.arg1 = uploadSize;
		mHandler.sendMessage(msg);
		
	}

	@Override
	public void initUpload(int fileSize) {
		Message msg = Message.obtain();
		msg.what = UPLOAD_INIT_PROCESS;
		msg.arg1 = fileSize;
		mHandler.sendMessage(msg);
		
		
	}

}
