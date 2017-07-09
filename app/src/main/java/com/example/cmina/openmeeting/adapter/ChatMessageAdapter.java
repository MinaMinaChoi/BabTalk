package com.example.cmina.openmeeting.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
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
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.cmina.openmeeting.utils.ChatMessage;
import com.example.cmina.openmeeting.R;
import com.example.cmina.openmeeting.utils.SaveSharedPreference;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

import static android.R.attr.path;
import static com.example.cmina.openmeeting.R.id.chatMessage;

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
            view.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) view.getTag();
        }


        viewHolder.chatTimeTextView.setText(chatMessage.getMsgTime().substring(12));
        viewHolder.chatUseridTextView.setText(chatMessage.getUserid());

        if (chatMessage.getImgUrl().equals("")) {
            Glide.with(context).load(R.drawable.userdefault).bitmapTransform(new CropCircleTransformation(context)).into(viewHolder.profileImageView);
        } else {
            Glide.with(context).load(chatMessage.getImgUrl()).bitmapTransform(new CropCircleTransformation(context)).into(viewHolder.profileImageView);
        }

        LinearLayout chatMsgContainer = (LinearLayout) view.findViewById(R.id.chatContainer);

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
                Glide.with(context).load(chatMessage.getMsg()).into(viewHolder.chatImageView);

            } else {
                viewHolder.chatMessageTextView.setVisibility(View.GONE);
                viewHolder.chatImageView.setVisibility(View.VISIBLE);
                viewHolder.videoImage.setVisibility(View.VISIBLE);
                Bitmap image = ThumbnailUtils.createVideoThumbnail(chatMessage.getMsg(), android.provider.MediaStore.Video.Thumbnails.MINI_KIND);

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

        } else {
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
                Glide.with(context).load(chatMessage.getMsg()).into(viewHolder.chatImageView);
            } else {  //동영상일 경우 섬네일이미지 셋팅.
                viewHolder.chatMessageTextView.setVisibility(View.GONE);
                viewHolder.chatImageView.setVisibility(View.VISIBLE);
                viewHolder.videoImage.setVisibility(View.VISIBLE);
                Bitmap image =ThumbnailUtils.createVideoThumbnail(chatMessage.getMsg() , android.provider.MediaStore.Video.Thumbnails.MINI_KIND);
                if (image != null) {
                    viewHolder.chatImageView.setImageBitmap(image);
                } else {
                    Glide.with(context).load(R.drawable.videodefault).into(viewHolder.chatImageView);
                }
            }

            chatMsgContainer.setGravity(Gravity.LEFT);
        }


        viewHolder.chatImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (chatMessage.getMsgtype() == 2) { //동영상일 경우
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    File videoFile = new File(chatMessage.getMsg());
                    Uri uriFromVideoFile = Uri.fromFile(videoFile);
                    Log.d("확인", "path : "+chatMessage.getMsg() + " uri : "+ uriFromVideoFile);

                    intent.setDataAndType(uriFromVideoFile, "video/*");
                    context.startActivity(intent);
                }
            }
        });

        return view;

    }

    public void addChatMsg(String roomid, String imgUrl, String userid, String msg, String msgTime, int msgtype) {
        ChatMessage chatMessage = new ChatMessage(roomid, imgUrl, userid, msg,msgTime, msgtype);

        chatMessage.setRoomid(roomid);
        chatMessage.setImgUrl(imgUrl);
        chatMessage.setUserid(userid);
        chatMessage.setMsg(msg);
        chatMessage.setMsgTime(msgTime);
        chatMessage.setMsgtype(msgtype);

        msgs.add(chatMessage);

    }
}
