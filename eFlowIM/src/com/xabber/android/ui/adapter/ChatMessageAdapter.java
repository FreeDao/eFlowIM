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
package com.xabber.android.ui.adapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.CharacterStyle;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.SettingsManager.ChatsDivide;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomContact;
import com.xabber.android.data.message.ChatAction;
import com.xabber.android.data.message.MessageItem;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.ContactList;
import com.xabber.android.utils.Emoticons;
import com.xabber.android.utils.StringUtils;
import com.xabber.androiddev.R;

/**
 * Adapter for the list of messages in the chat.
 * 
 * @author alexander.ivanov
 * 
 */
public class ChatMessageAdapter extends BaseAdapter implements UpdatableAdapter {

	private static final int TYPE_MESSAGE = 0;
	private static final int TYPE_HINT = 1;
	private static final int TYPE_EMPTY = 2;

	private final Activity activity;
	private String account;
	private String user;
	private boolean isMUC;
	private List<MessageItem> messages;

	/**
	 * Message font appearance.
	 */
	private final int appearanceStyle;

	/**
	 * Divider between header and body.
	 */
	private final String divider;

	/**
	 * Text with extra information.
	 */
	private String hint;
	
	private int percent;

	public ChatMessageAdapter(Activity activity) {
		this.activity = activity;
		messages = Collections.emptyList();
		account = null;
		user = null;
		hint = null;
		percent = 0;
		appearanceStyle = SettingsManager.chatsAppearanceStyle();
		ChatsDivide chatsDivide = SettingsManager.chatsDivide();
		if (chatsDivide == ChatsDivide.always
				|| (chatsDivide == ChatsDivide.portial && !activity
						.getResources().getBoolean(R.bool.landscape)))
			divider = "\n";
		else
			divider = " ";
	}

	@Override
	public int getCount() {
		return messages.size() + 1;
	}

	@Override
	public Object getItem(int position) {
		if (position < messages.size())
			return messages.get(position);
		else
			return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getViewTypeCount() {
		return 3;
	}

	@Override
	public int getItemViewType(int position) {
		if (position < messages.size())
			return TYPE_MESSAGE;
		else
			return hint == null ? TYPE_EMPTY : TYPE_HINT;
	}

	private void append(SpannableStringBuilder builder, CharSequence text,
			CharacterStyle span) {
		int start = builder.length();
		builder.append(text);
		builder.setSpan(span, start, start + text.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final int type = getItemViewType(position);
		View view;
		
		if (convertView == null) {
			final int resourceLayout;
			if (type == TYPE_MESSAGE){
				resourceLayout = R.layout.chat_viewer_msg_in;
			}
			else if (type == TYPE_HINT)
				resourceLayout = R.layout.chat_viewer_info;
			else if (type == TYPE_EMPTY)
				resourceLayout = R.layout.chat_viewer_empty;
			else
				throw new IllegalStateException();
			view = activity.getLayoutInflater().inflate(resourceLayout, parent, false);
			if (type == TYPE_MESSAGE)
				((TextView) view.findViewById(R.id.text)).setTextAppearance(
						activity, appearanceStyle);
		} else{
			view = convertView;
		}
			

		if (type == TYPE_EMPTY)
			return view;

		if (type == TYPE_HINT) {
			TextView textView = ((TextView) view.findViewById(R.id.info));
			textView.setText(hint);
			textView.setTextAppearance(activity, R.style.ChatInfo_Warning);
			return view;
		}

		final MessageItem messageItem = (MessageItem) getItem(position);
		final boolean incoming = messageItem.isIncoming();
		final boolean issend = messageItem.isSent();
		final String account = messageItem.getChat().getAccount();
		final String user = messageItem.getChat().getUser();
		final String resource = messageItem.getResource();
		final String name;
		
		if (isMUC) {
			name = resource;
		} else {
			if (incoming)
				name = "";//RosterManager.getInstance().getName(account, user);
			else
				name = "";//AccountManager.getInstance().getNickName(account);
		}
		
		
		
		if (incoming) {
			view = activity.getLayoutInflater().inflate(R.layout.chat_viewer_msg_in, parent, false);
		} else {
			view = activity.getLayoutInflater().inflate(R.layout.chat_viewer_msg_out, parent, false);
		}
		
		Spannable text = messageItem.getSpannable();
		TextView textView = (TextView) view.findViewById(R.id.text);
		TextView svText = (TextView) view.findViewById(R.id.svText);
		TextView bitmapTX = (TextView) view.findViewById(R.id.bitmapTX);
		TextView messContent = (TextView) view.findViewById(R.id.messContent);
		
		
		ImageView avatarView = (ImageView) view.findViewById(R.id.avatar);
		ImageView bitmapSC = (ImageView) view.findViewById(R.id.bitmapSC);
		ChatAction action = messageItem.getAction();
		String time = StringUtils.getSmartTimeText(messageItem.getTimestamp());
		RelativeLayout.LayoutParams layParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		layParams.addRule(RelativeLayout.BELOW, R.id.bitmapTX); 
		layParams.setMargins(0, 0, 0, 0);
		bitmapSC.setLayoutParams(layParams);
		
		textView.setVisibility(View.GONE);
		
		if ( text.toString().startsWith(ContactList.prefix) ){
			layParams.setMargins(0, 5, 0, 0);
			bitmapSC.setLayoutParams(layParams);
			
			if ( incoming ) {
				Application.mFinalBitmap.display(bitmapSC, text.toString());
				
			}else {
				String st2 = text.toString().substring("http://121.199.3.19:8080".length());
				String[] st3 = st2.split("/");
				String fileName = st3[st3.length-1];
				Bitmap bitmap = null;
				if ( new File(ContactList.IMAGE_ROOT_PATH + fileName).exists() ){
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inJustDecodeBounds = true;
					Bitmap bitmap2 = BitmapFactory.decodeFile(ContactList.IMAGE_ROOT_PATH + fileName, options);
					if ( options.outHeight < ContactList.imageMaxHW || options.outWidth < ContactList.imageMaxHW ){
						bitmap = BitmapFactory.decodeFile(ContactList.IMAGE_ROOT_PATH + fileName);
						bitmapSC.setBackgroundDrawable(new BitmapDrawable(bitmap));
						
					}else {
						
						int intnt = 2;
						while ( true ) {
							Options op = new Options();
					        op.inJustDecodeBounds = true;
					        bitmap = BitmapFactory.decodeFile(ContactList.IMAGE_ROOT_PATH + fileName, op);
					        op.inSampleSize = intnt; 
					        op.inJustDecodeBounds = false; 
					        bitmap = BitmapFactory.decodeFile(ContactList.IMAGE_ROOT_PATH + fileName, op);
					        
					        if ( bitmap != null ){
					        	intnt = intnt+1;
					        	if ( bitmap.getWidth() < ContactList.imageMaxHW || bitmap.getHeight() < ContactList.imageMaxHW ) {
						        	break;
						        }
					        }
					        
						}
						
						bitmapSC.setBackgroundDrawable(new BitmapDrawable(bitmap));
					}
				}else {
					bitmapSC.setBackgroundResource(R.drawable.image_download_fail_icon);
					
				}
			}
			
			
			bitmapTX.setText(time);
			messContent.setVisibility(View.GONE);
			
			/*if ( "record".equals(pathStrings[0]) ){
				if ( incoming ) {
					bitmapSC.setBackgroundResource(R.drawable.voice_chat_in);
				} else {
					bitmapSC.setBackgroundResource(R.drawable.voice_chat_out);
				}
			}else if ( "image".equals(pathStrings[0]) ) {
//				bitmapTX.setVisibility(View.VISIBLE);
//				bitmapTX.setText(this.percent + "%");
				if ( incoming ) {
					bitmapSC.setBackgroundResource(R.drawable.download_image_icon);
				} else {
					bitmapSC.setBackgroundResource(R.drawable.download_image_icon);
				}
			}*/
			
		}else {
			bitmapSC.setBackgroundResource(0);
			bitmapTX.setText(time);
			messContent.setVisibility(View.VISIBLE);
			messContent.setText(text.toString());
		}
		
		
		
		SpannableStringBuilder builder = new SpannableStringBuilder();
		if (action == null) {
			int messageResource = R.drawable.ic_message_delivered;
			if (!incoming) {
				if (messageItem.isError())
					messageResource = R.drawable.ic_message_has_error;
				else if (!messageItem.isSent())
					messageResource = R.drawable.ic_message_not_sent;
				else if (!messageItem.isDelivered())
					messageResource = R.drawable.ic_message_not_delivered;
			}
			/*去掉发送成功状态
			append(builder, " ", new ImageSpan(activity, messageResource));*/
			append(builder, "", new TextAppearanceSpan(activity,
					R.style.ChatHeader));
			append(builder, time, new TextAppearanceSpan(activity,
					R.style.ChatHeader_Time));
			append(builder, " ", new TextAppearanceSpan(activity,
					R.style.ChatHeader));
			
			append(builder, name, new TextAppearanceSpan(activity,
					R.style.ChatHeader_Name));
			append(builder, divider, new TextAppearanceSpan(activity,
					R.style.ChatHeader));
			
			/*如果信息是延迟发送出去, 将显示"message was entered at xxx", 这里去掉, 不需要显示这个信息
			Date timeStamp = messageItem.getDelayTimestamp();
			if (timeStamp != null) {
				String delay = activity.getString(
						incoming ? R.string.chat_delay : R.string.chat_typed,
						StringUtils.getSmartTimeText(timeStamp));
				append(builder, delay, new TextAppearanceSpan(activity,
						R.style.ChatHeader_Delay));
				append(builder, divider, new TextAppearanceSpan(activity,
						R.style.ChatHeader));
			}*/
			if (messageItem.isUnencypted()) {
				append(builder,
						activity.getString(R.string.otr_unencrypted_message),
						new TextAppearanceSpan(activity,
								R.style.ChatHeader_Delay));
				append(builder, divider, new TextAppearanceSpan(activity,
						R.style.ChatHeader));
			}
			Emoticons.getSmiledText(activity.getApplication(), text);
			if (messageItem.getTag() == null)
				builder.append(text);
			else
				append(builder, text, new TextAppearanceSpan(activity,
						R.style.ChatRead));
		} else {
			append(builder, time, new TextAppearanceSpan(activity,
					R.style.ChatHeader_Time));
			append(builder, " ", new TextAppearanceSpan(activity,
					R.style.ChatHeader));
			text = Emoticons.newSpannable(action.getText(activity, name,
					text.toString()));
			Emoticons.getSmiledText(activity.getApplication(), text);
			append(builder, text, new TextAppearanceSpan(activity,
					R.style.ChatHeader_Delay));
		}
		textView.setText(builder);
		textView.setMovementMethod(LinkMovementMethod.getInstance());
		
		if (SettingsManager.chatsShowAvatars()) {
			avatarView.setVisibility(View.VISIBLE);
			if (!incoming
					|| (isMUC && MUCManager.getInstance()
							.getNickname(account, user)
							.equalsIgnoreCase(resource))) {
				avatarView.setImageDrawable(AvatarManager.getInstance()
						.getAccountAvatar(account));
			} else {
				if (isMUC) {  //多人聊天
					if ("".equals(resource)) {
						avatarView.setImageDrawable(AvatarManager.getInstance()
								.getRoomAvatar(user));
					} else {
						avatarView.setImageDrawable(AvatarManager.getInstance()
								.getOccupantAvatar(user + "/" + resource));
					}
				} else {
					avatarView.setImageDrawable(AvatarManager.getInstance()
							.getUserAvatar(user));
				}
			}
			((RelativeLayout.LayoutParams) textView.getLayoutParams()).addRule(
					RelativeLayout.RIGHT_OF, R.id.avatar);
		} else {
			avatarView.setVisibility(View.GONE);
		}
		return view;
	}

	public String getAccount() {
		return account;
	}

	public String getUser() {
		return user;
	}

	/**
	 * Changes managed chat.
	 * 
	 * @param account
	 * @param user
	 */
	public void setChat(String account, String user) {
		this.account = account;
		this.user = user;
		this.isMUC = MUCManager.getInstance().hasRoom(account, user);
		onChange();
	}

	@Override
	public void onChange() {
		messages = new ArrayList<MessageItem>(MessageManager.getInstance()
				.getMessages(account, user));
		hint = getHint();
		notifyDataSetChanged();
	}

	/**
	 * @return New hint.
	 */
	private String getHint() {
		AccountItem accountItem = AccountManager.getInstance().getAccount(
				account);
		boolean online;
		if (accountItem == null)
			online = false;
		else
			online = accountItem.getState().isConnected();
		final AbstractContact abstractContact = RosterManager.getInstance()
				.getBestContact(account, user);
		if (!online) {
			if (abstractContact instanceof RoomContact)
				return activity.getString(R.string.muc_is_unavailable);
			else
				return activity.getString(R.string.account_is_offline);
		} else if (!abstractContact.getStatusMode().isOnline()) {
			if (abstractContact instanceof RoomContact)
				return activity.getString(R.string.muc_is_unavailable);
			else
				return activity.getString(R.string.contact_is_offline,
						abstractContact.getName());
		}
		return null;
	}

	/**
	 * Contact information has been changed. Renews hint and updates data if
	 * necessary.
	 */
	public void updateInfo() {
		String info = getHint();
		if (this.hint == info || (this.hint != null && this.hint.equals(info)))
			return;
		this.hint = info;
		notifyDataSetChanged();
	}
	
	public void updatePercent(int per) {
		this.percent = per;
		notifyDataSetChanged();
	}
	
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
	

}
