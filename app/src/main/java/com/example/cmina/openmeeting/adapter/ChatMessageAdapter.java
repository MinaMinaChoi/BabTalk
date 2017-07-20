package com.example.cmina.openmeeting.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.cmina.openmeeting.utils.ChatMessage;
import com.example.cmina.openmeeting.R;
import com.example.cmina.openmeeting.utils.SaveSharedPreference;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

/**
 * Created by cmina on 2017-06-13.
 */

public class ChatMessageAdapter extends BaseAdapter {

    //adapter에 추가된 데이터를 저장하기 위한 ArrayList
    private List<ChatMessage> msgs = new ArrayList<ChatMessage>();
    private Context context;

    public ChatMessageAdapter(List<ChatMessage> chatMessages, Context context) {
        this.context = context;
        msgs = chatMessages;
    }

    @Override
    public int getCount() {
        return msgs.size();
    }

    @Nullable
    @Override
    public Object getItem(int position) {
        return (ChatMessage) msgs.get(position);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public class ViewHolder {
        TextView chatMessageTextView;
        TextView chatTimeTextView;
        TextView chatUseridTextView;
        ImageView profileImageView;

        ImageView chatImageView;
        ImageView videoImage;

        ProgressBar progressBar;
        ImageView sendfail;

    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        ViewHolder viewHolder;

        final ChatMessage chatMessage = msgs.get(position);
        LayoutInflater inflater;

        if (view == null) {
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.chatmsgitem, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.chatMessageTextView = (TextView) view.findViewById(R.id.chatMessage);
            viewHolder.chatTimeTextView = (TextView) view.findViewById(R.id.chatTimeTextView);
            viewHolder.chatUseridTextView = (TextView) view.findViewById(R.id.useridTextView);
            viewHolder.profileImageView = (ImageView) view.findViewById(R.id.profileImage);
            viewHolder.chatImageView = (ImageView) view.findViewById(R.id.chatImg);
            viewHolder.videoImage = (ImageView) view.findViewById(R.id.videoplay);

            viewHolder.progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
            viewHolder.sendfail = (ImageView) view.findViewById(R.id.sendfail);

            view.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) view.getTag();
        }


        viewHolder.chatTimeTextView.setText(chatMessage.getMsgTime().substring(12));
        viewHolder.chatUseridTextView.setText(chatMessage.getUserid());

        if (chatMessage.getProfileImg().equals("")) {
            Glide.with(context).load(R.drawable.userdefault).bitmapTransform(new CropCircleTransformation(context)).into(viewHolder.profileImageView);
        } else {
            Glide.with(context).load(chatMessage.getProfileImg()).bitmapTransform(new CropCircleTransformation(context)).into(viewHolder.profileImageView);
        }

        LinearLayout chatMsgContainer = (LinearLayout) view.findViewById(R.id.chatContainer);


        //내가 보낸 메시지
        if (chatMessage.getUserid().equals(SaveSharedPreference.getUserid(context))) {

            viewHolder.profileImageView.setVisibility(View.GONE); //나의 이미지 안보이게
            viewHolder.chatUseridTextView.setVisibility(View.GONE); //나의 아이디도 안 보이게

            if (chatMessage.getMsgtype() == 0)  { //일반메시지일 경우
                viewHolder.chatMessageTextView.setVisibility(View.VISIBLE);
                viewHolder.chatImageView.setVisibility(View.GONE);
                viewHolder.videoImage.setVisibility(View.GONE);
                viewHolder.chatMessageTextView.setBackground(context.getResources().getDrawable(R.drawable.bubble_right2));
                viewHolder.chatMessageTextView.setText(chatMessage.getMsg());

            } else if (chatMessage.getMsgtype() == 1){ //이미지일경우,
                viewHolder.chatMessageTextView.setVisibility(View.GONE);
                viewHolder.videoImage.setVisibility(View.GONE);
                viewHolder.chatImageView.setVisibility(View.VISIBLE);
                Glide.with(context).load(context.getFileStreamPath(chatMessage.getMsg())).into(viewHolder.chatImageView);
///data/data/com.androidhuman.app/files/filename.ext

            } else {
            /*    //다운로딩 중이면...
                if (duringDownload) {
                    viewHolder.progressBar.setVisibility(View.VISIBLE);
                    viewHolder.videoImage.setVisibility(View.GONE);

                } else {
                    viewHolder.videoImage.setVisibility(View.VISIBLE);
                    viewHolder.progressBar.setVisibility(View.GONE);
                }*/

                viewHolder.videoImage.setVisibility(View.VISIBLE);
                viewHolder.chatMessageTextView.setVisibility(View.GONE);
                viewHolder.chatImageView.setVisibility(View.VISIBLE);
                Bitmap image = ThumbnailUtils.createVideoThumbnail(""+context.getFileStreamPath(chatMessage.getMsg()), android.provider.MediaStore.Video.Thumbnails.MINI_KIND);

                if (image != null) {
                    viewHolder.chatImageView.setImageBitmap(image);
                } else {
                    Glide.with(context).load(R.drawable.videodefault).into(viewHolder.chatImageView);
                }

            }

            chatMsgContainer.setGravity(Gravity.RIGHT);
        } else if (chatMessage.getUserid().equals("알림")) {

            chatMsgContainer.setGravity(Gravity.CENTER);
            viewHolder.profileImageView.setVisibility(View.GONE);
            viewHolder.chatUseridTextView.setVisibility(View.GONE);
            viewHolder.chatImageView.setVisibility(View.GONE);
            viewHolder.chatMessageTextView.setVisibility(View.VISIBLE);
            viewHolder.chatMessageTextView.setText(chatMessage.getMsg());
            viewHolder.chatMessageTextView.setBackground(null);

        } else { //다른 사람이 보낸 메시지
            viewHolder.profileImageView.setVisibility(View.VISIBLE);
            viewHolder.chatUseridTextView.setVisibility(View.VISIBLE);

            if (chatMessage.getMsgtype() == 0)  { //일반메시지일 경우
                viewHolder.chatMessageTextView.setVisibility(View.VISIBLE);
                viewHolder.chatImageView.setVisibility(View.GONE);
                viewHolder.videoImage.setVisibility(View.GONE);

                viewHolder.chatMessageTextView.setText(chatMessage.getMsg());
                viewHolder.chatMessageTextView.setBackground(context.getResources().getDrawable(R.drawable.bubble_left2));

            } else if (chatMessage.getMsgtype() == 1){ //이미지일경우,
                viewHolder.chatMessageTextView.setVisibility(View.GONE);
                viewHolder.chatImageView.setVisibility(View.VISIBLE);
                viewHolder.videoImage.setVisibility(View.GONE);
                Glide.with(context).load(context.getFileStreamPath(chatMessage.getMsg())).into(viewHolder.chatImageView);
            } else {  //동영상일 경우 섬네일이미지 셋팅.
                viewHolder.chatMessageTextView.setVisibility(View.GONE);
                viewHolder.chatImageView.setVisibility(View.VISIBLE);
                viewHolder.videoImage.setVisibility(View.VISIBLE);
                Bitmap image =ThumbnailUtils.createVideoThumbnail(""+context.getFileStreamPath(chatMessage.getMsg()), android.provider.MediaStore.Video.Thumbnails.MINI_KIND);
                if (image != null) {
                    viewHolder.chatImageView.setImageBitmap(image);
                } else {
                    Glide.with(context).load(R.drawable.videodefault).into(viewHolder.chatImageView);
                }
            }

            chatMsgContainer.setGravity(Gravity.LEFT);
        }


        //동영상일 경우, 썸네일을 클릭하면, 동영상 재생이 되도록!
        viewHolder.chatImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (chatMessage.getMsgtype() == 2 ) { //동영상일 경우
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    File videoFile = new File(""+context.getFileStreamPath(chatMessage.getMsg()));
                    videoFile.setReadable(true, false); //읽을 수 있도록...
                    Uri uriFromVideoFile = Uri.fromFile(videoFile);
                    Log.d("확인", "path : "+context.getFileStreamPath(chatMessage.getMsg()) + " uri : "+ uriFromVideoFile);

                    intent.setDataAndType(uriFromVideoFile, "video/*");
                    context.startActivity(intent);
                }
            }
        });

        return view;

    }

    public void addChatMsg(String roomid, String imgUrl, String userid, String msg, String msgTime, int msgtype, String msgid) {
        ChatMessage chatMessage = new ChatMessage(roomid, imgUrl, userid, msg,msgTime, msgtype, msgid);

        chatMessage.setRoomid(roomid);
        chatMessage.setProfileImg(imgUrl);
        chatMessage.setUserid(userid);
        chatMessage.setMsg(msg);
        chatMessage.setMsgTime(msgTime);
        chatMessage.setMsgtype(msgtype);
        chatMessage.setMsgid(msgid);

        msgs.add(chatMessage);

    }
}
