package com.example.cmina.openmeeting.adapter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.cmina.openmeeting.activity.ChatActivity;
import com.example.cmina.openmeeting.activity.MainActivity;
import com.example.cmina.openmeeting.activity.ProfileActivity;
import com.example.cmina.openmeeting.activity.WebViewActivity;
import com.example.cmina.openmeeting.utils.ChatMessage;
import com.example.cmina.openmeeting.R;
import com.example.cmina.openmeeting.utils.MediaScanner;
import com.example.cmina.openmeeting.utils.OkHttpRequest;
import com.example.cmina.openmeeting.utils.Protocol;
import com.example.cmina.openmeeting.utils.SaveSharedPreference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.wasabeef.glide.transformations.CropCircleTransformation;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.example.cmina.openmeeting.R.id.chatMessage;
import static com.example.cmina.openmeeting.activity.ChatActivity.realPath;
import static com.example.cmina.openmeeting.activity.MainActivity.myDatabaseHelper;
import static com.example.cmina.openmeeting.utils.Protocol.PT_OFFSET;
import static com.example.cmina.openmeeting.utils.Protocol.cliToServer;

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
        TextView chatUseridTextView;
        ImageView profileImageView;

        ImageView chatImageView;
        ImageView videoImage;

        TextView chatTimeTextView;
        ProgressBar progressBar;
        ImageView sendfail;

        LinearLayout previewLinear;
        ImageView previewImg;
        TextView previewTitle;
        TextView previewDesc;

    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        ViewHolder viewHolder;

        final LayoutInflater inflater;

        if (view == null) {
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //view = inflater.inflate(R.layout.chatmsgitem, null);
            view = inflater.inflate(R.layout.chatmsgitem, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.chatMessageTextView = (TextView) view.findViewById(chatMessage);
            viewHolder.chatUseridTextView = (TextView) view.findViewById(R.id.useridTextView);
            viewHolder.profileImageView = (ImageView) view.findViewById(R.id.profileImage);
            viewHolder.chatImageView = (ImageView) view.findViewById(R.id.chatImg);
            viewHolder.videoImage = (ImageView) view.findViewById(R.id.videoplay);

            viewHolder.chatTimeTextView = (TextView) view.findViewById(R.id.chatTimeTextView);
            viewHolder.progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
            viewHolder.sendfail = (ImageView) view.findViewById(R.id.sendfail);

            viewHolder.previewLinear = (LinearLayout) view.findViewById(R.id.Urlpreview);
            viewHolder.previewImg = (ImageView) view.findViewById(R.id.previewImg);
            viewHolder.previewTitle = (TextView) view.findViewById(R.id.previewTitle);
            viewHolder.previewDesc = (TextView) view.findViewById(R.id.previewDesc);

            view.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        final ChatMessage chatMessage = msgs.get(position);
       // System.out.println("getview:"+position+" "+convertView +"\n"+chatMessage.getMsg());

        //viewholer에다 데이터 세팅
        //메시지 받은 시간 long으로 저장하고 보여줄때, 변환하기
        Date date = new Date(Long.valueOf(chatMessage.getMsgTime()));
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd, a hh:mm");
        String time = simpleDateFormat.format(date).toString();

        viewHolder.chatTimeTextView.setText(time.substring(12));
        viewHolder.chatUseridTextView.setText(chatMessage.getUserid());

        if (chatMessage.getProfileImg().equals("")) {
            Glide.with(context).load(R.drawable.userdefault).bitmapTransform(new CropCircleTransformation(context)).into(viewHolder.profileImageView);
        } else {
            Glide.with(context).load(chatMessage.getProfileImg()).bitmapTransform(new CropCircleTransformation(context)).into(viewHolder.profileImageView);
        }

        final LinearLayout chatMsgContainer = (LinearLayout) view.findViewById(R.id.chatContainer);

        //내가 보낸 메시지
        if (chatMessage.getUserid().equals(SaveSharedPreference.getUserid(context))) {

            viewHolder.profileImageView.setVisibility(View.GONE); //나의 이미지 안보이게
            viewHolder.chatUseridTextView.setVisibility(View.GONE); //나의 아이디도 안 보이게
            viewHolder.chatTimeTextView.setVisibility(View.VISIBLE); //메시지 시간 보이게

            if (chatMessage.getMsgtype() == 0) { //일반메시지일 경우
                viewHolder.chatMessageTextView.setVisibility(View.VISIBLE);
                viewHolder.chatMessageTextView.setBackground(context.getResources().getDrawable(R.drawable.bubble_right2));
                viewHolder.chatMessageTextView.setText(chatMessage.getMsg());
                viewHolder.chatImageView.setVisibility(View.GONE);
                viewHolder.videoImage.setVisibility(View.GONE);
                viewHolder.progressBar.setVisibility(View.GONE);
                viewHolder.sendfail.setVisibility(View.GONE);
                //viewHolder.chatTimeTextView.set

                //msg가 url형식일 경우, 미리보기 보여주기
                String regex = "^(((http(s?))\\:\\/\\/)?)([0-9a-zA-Z\\-]+\\.)+[a-zA-Z]{2,6}(\\:[0-9]+)?(\\/\\S*)?$";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(chatMessage.getMsg());

                if (matcher.find()) {
                    //형식이 도메인형식이라면..
                    viewHolder.previewLinear.setVisibility(View.VISIBLE);

                    viewHolder.previewTitle.setText(chatMessage.getPreTitle());
                    viewHolder.previewDesc.setText(chatMessage.getPreDesc());
                    Glide.with(context).load(chatMessage.getPreImg()).placeholder(R.drawable.placeholder).into(viewHolder.previewImg);

                } else {
                    //일반 메시지
                    viewHolder.previewLinear.setVisibility(View.GONE);
                }


            } else if (chatMessage.getMsgtype() == 1) { //이미지일경우,
                viewHolder.chatMessageTextView.setVisibility(View.GONE);
                viewHolder.videoImage.setVisibility(View.GONE);
                viewHolder.progressBar.setVisibility(View.GONE);
                viewHolder.sendfail.setVisibility(View.GONE);
                viewHolder.previewLinear.setVisibility(View.GONE);
                viewHolder.chatImageView.setVisibility(View.VISIBLE);
                Glide.with(context).load(context.getFileStreamPath(chatMessage.getMsg())).into(viewHolder.chatImageView);
///data/data/com.androidhuman.app/files/filename.ext

            } else { //동영상일 경우

                Bitmap image = null;

                //보내기 실패하면, 엑스표시
                if (chatMessage.getMsgtype() == 4) {
                    image = ThumbnailUtils.createVideoThumbnail(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + chatMessage.getMsg(), android.provider.MediaStore.Video.Thumbnails.MINI_KIND);
                    Log.d("msgtype =4일 때 ", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + chatMessage.getMsg());
                    viewHolder.chatTimeTextView.setVisibility(View.GONE);
                    viewHolder.sendfail.setVisibility(View.VISIBLE);
                    viewHolder.videoImage.setVisibility(View.GONE);
                } else {
                    image = ThumbnailUtils.createVideoThumbnail("" + context.getFileStreamPath(chatMessage.getMsg()), android.provider.MediaStore.Video.Thumbnails.MINI_KIND);
                    Log.d("msgtype =2일 때 ", "" + context.getFileStreamPath(chatMessage.getMsg()));
                    viewHolder.chatTimeTextView.setVisibility(View.VISIBLE);
                    viewHolder.sendfail.setVisibility(View.GONE);
                    viewHolder.videoImage.setVisibility(View.VISIBLE);
                }

                viewHolder.chatImageView.setVisibility(View.VISIBLE);
                viewHolder.videoImage.setVisibility(View.VISIBLE);
                viewHolder.chatMessageTextView.setVisibility(View.GONE);
                viewHolder.previewLinear.setVisibility(View.GONE);

                if (image != null) {
                    viewHolder.chatImageView.setImageBitmap(image);
                } else {
                    Glide.with(context).load(R.drawable.videodefault).into(viewHolder.chatImageView);
                }

            }

            chatMsgContainer.setGravity(Gravity.RIGHT);
        } else if (chatMessage.getUserid().equals("알림")) {

            chatMsgContainer.setGravity(Gravity.CENTER);
            viewHolder.chatTimeTextView.setVisibility(View.GONE);
            viewHolder.profileImageView.setVisibility(View.GONE);
            viewHolder.chatUseridTextView.setVisibility(View.GONE);
            viewHolder.chatImageView.setVisibility(View.GONE);
            viewHolder.videoImage.setVisibility(View.GONE);
            viewHolder.sendfail.setVisibility(View.GONE);
            viewHolder.progressBar.setVisibility(View.GONE);
            viewHolder.previewLinear.setVisibility(View.GONE);
            viewHolder.chatMessageTextView.setVisibility(View.VISIBLE);
            viewHolder.chatMessageTextView.setTextColor(R.color.Gray);
            viewHolder.chatMessageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            viewHolder.chatMessageTextView.setText(chatMessage.getMsg());
            viewHolder.chatMessageTextView.setBackground(null);

        } else { //다른 사람이 보낸 메시지
            viewHolder.profileImageView.setVisibility(View.VISIBLE);
            Glide.with(context).load(chatMessage.getProfileImg()).bitmapTransform(new CropCircleTransformation(context)).into(viewHolder.profileImageView);
            viewHolder.chatUseridTextView.setVisibility(View.VISIBLE);
            viewHolder.chatTimeTextView.setVisibility(View.VISIBLE); //메시지 시간 보이게

            if (chatMessage.getMsgtype() == 0) { //일반메시지일 경우
                viewHolder.chatMessageTextView.setVisibility(View.VISIBLE);
                viewHolder.chatMessageTextView.setText(chatMessage.getMsg());
                viewHolder.chatMessageTextView.setBackground(context.getResources().getDrawable(R.drawable.bubble_left2));
                viewHolder.chatImageView.setVisibility(View.GONE);
                viewHolder.videoImage.setVisibility(View.GONE);
                viewHolder.progressBar.setVisibility(View.GONE);
                viewHolder.sendfail.setVisibility(View.GONE);

                //수정중
                //msg가 url형식일 경우, 미리보기 보여주기
                String regex = "^(((http(s?))\\:\\/\\/)?)([0-9a-zA-Z\\-]+\\.)+[a-zA-Z]{2,6}(\\:[0-9]+)?(\\/\\S*)?$";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(chatMessage.getMsg());

                if (matcher.find()) {
                    Log.d("ChatMessageAdapter", "" + chatMessage.getMsg());
                    //형식이 도메인형식이라면..
                    viewHolder.previewLinear.setVisibility(View.VISIBLE);

                    viewHolder.previewTitle.setText(chatMessage.getPreTitle());
                    viewHolder.previewDesc.setText(chatMessage.getPreDesc());
                    Glide.with(context).load(chatMessage.getPreImg()).placeholder(R.drawable.placeholder).into(viewHolder.previewImg);

                } else {
                    //일반 메시지
                    viewHolder.previewLinear.setVisibility(View.GONE);
                }

            } else if (chatMessage.getMsgtype() == 1) { //이미지일경우,
                viewHolder.chatMessageTextView.setVisibility(View.GONE);
                viewHolder.chatImageView.setVisibility(View.VISIBLE);
                viewHolder.videoImage.setVisibility(View.GONE);
                viewHolder.sendfail.setVisibility(View.GONE);
                viewHolder.progressBar.setVisibility(View.GONE);
                viewHolder.previewLinear.setVisibility(View.GONE);
                Glide.with(context).load(context.getFileStreamPath(chatMessage.getMsg())).into(viewHolder.chatImageView);
            } else {  //동영상일 경우 섬네일이미지 셋팅.

                viewHolder.chatMessageTextView.setVisibility(View.GONE);
                viewHolder.chatImageView.setVisibility(View.VISIBLE);
                viewHolder.videoImage.setVisibility(View.VISIBLE);
                viewHolder.sendfail.setVisibility(View.GONE);
                viewHolder.progressBar.setVisibility(View.GONE);
                viewHolder.previewLinear.setVisibility(View.GONE);
                Bitmap image = ThumbnailUtils.createVideoThumbnail("" + context.getFileStreamPath(chatMessage.getMsg()), android.provider.MediaStore.Video.Thumbnails.MINI_KIND);
                if (image != null) {
                    viewHolder.chatImageView.setImageBitmap(image);
                } else {
                    Glide.with(context).load(R.drawable.videodefault).into(viewHolder.chatImageView);
                }
            }

            chatMsgContainer.setGravity(Gravity.LEFT);
        }
        //데이터 세팅 끝


        //동영상일 경우, 썸네일을 클릭하면, 동영상 재생이 되도록!
        viewHolder.chatImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (chatMessage.getMsgtype() == 1) { //이미지 일경우

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    AlertDialog alertDialog = builder.setMessage("이미지")
                            .setPositiveButton("다운로드", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    //2017.08.04
                                    new DownloadAsync().execute(chatMessage.getMsg());

                                }
                            }).setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    return;
                                }
                            }).create();
                    alertDialog.show();

                } else if (chatMessage.getMsgtype() == 2) { //동영상일 경우

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    AlertDialog alertDialog = builder.setMessage("동영상")
                            .setNegativeButton("바로 재생", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    Intent intent = new Intent();
                                    intent.setAction(Intent.ACTION_VIEW);
                                    File videoFile = new File("" + context.getFileStreamPath(chatMessage.getMsg()));
                                    videoFile.setReadable(true, false); //읽을 수 있도록...
                                    intent.setDataAndType(Uri.fromFile(videoFile), "video/*");
                                    // 2017.08.03
                                    //No Activity found to handle Intent { act=android.intent.action.VIEW dat=file:///data/data/com.example.cmina.openmeeting/files/1501742009022.mp4 typ=video }
                                    context.startActivity(intent);

                                }
                            }).setPositiveButton("다운로드", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //2017.08.04
                                    new DownloadAsync().execute(chatMessage.getMsg());

                                }
                            }).create();

                    alertDialog.show();


                } else if (chatMessage.getMsgtype() == 4) { //보내기 실패했을 때, 썸네일 클릭하면 다시 보내지도록..

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    AlertDialog alertDialog = builder.setMessage("파일을 재전송하시겠습니까?")
                            .setPositiveButton("재전송", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    realPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + chatMessage.getMsg();

                                    Protocol protocol = new Protocol(244);
                                    protocol.setTotalLen(String.valueOf(244));
                                    protocol.setProtocolType(String.valueOf(PT_OFFSET));
                                    protocol.setRoomid(chatMessage.getRoomid());
                                    protocol.setUserid(chatMessage.getUserid());
                                    protocol.setFileName(chatMessage.getMsg());
                                    //수정중
                                    //다시보내기일 경우, setMsgid에 전송실패시의 타임을 넣어준다.
                                    protocol.setMsgId(chatMessage.getMsgTime());
                                    protocol.setCheckType(String.valueOf(cliToServer));

                                    ((ChatActivity) context).socketService.send_byte(protocol.getPacket());
                                }
                            })
                            .setNegativeButton("삭제", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //보내기 실패한 메시지 삭제하도록
                                    //1.로컬에 저장된 메시지도 삭제
                                    //2.서버에 저장된 템프파일도 삭제하도록...->아직 안함.
                                    //서버로 filename, userid보내서
                                    JSONObject object = new JSONObject();

                                    try {
                                        object.put("userid", chatMessage.getUserid());
                                        object.put("filename", chatMessage.getMsg());
                                        Log.d("ChatMessageAdapter", "보내는 정보확인" + chatMessage.getUserid() + chatMessage.getMsg());

                                        OkHttpRequest request = new OkHttpRequest();
                                        request.post("http://13.124.77.49/delTempFile.php", object.toString(), new Callback() {
                                            @Override
                                            public void onFailure(Call call, IOException e) {
                                                Log.e("tempfile delete request failure", call.toString());
                                            }

                                            @Override
                                            public void onResponse(Call call, Response response) throws IOException {
                                                String responseStr = response.body().string();

                                                Log.d("ChatMessageAdapter", responseStr);

                                                if (responseStr.equals("1")) {
                                                    //삭제 성공
                                                    myDatabaseHelper.removetype4(chatMessage.getMsgTime());
                                                    ((ChatActivity) context).socketService.mCallback2.recvMsg(chatMessage.getRoomid(), chatMessage.getProfileImg(), chatMessage.getUserid(), chatMessage.getMsg(), chatMessage.getRoomid(), 2, chatMessage.getMsgTime(),
                                                            "", "", "");
                                                    ((ChatActivity) context).addHandler();

                                                } else {

                                                }
                                            }
                                        });
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            })
                            .create();
                    alertDialog.show();

                }
            }
        });

        viewHolder.previewLinear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!chatMessage.getMsg().equals("")) {
                    Intent intent = new Intent((ChatActivity) context, WebViewActivity.class);
                    intent.putExtra("url", chatMessage.getMsg());
                    intent.putExtra("title", chatMessage.getPreTitle());
                    context.startActivity(intent);
                }
            }
        });

        //상대방 프로필이미지 클릭
        viewHolder.profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!chatMessage.getUserid().equals(SaveSharedPreference.getUserid(context))) {
                    //서버로 userid보내서, 해당 회원의 정보 보여주기!
                    Intent intent = new Intent(context, ProfileActivity.class);
                    intent.putExtra("userid", chatMessage.getUserid());
                    context.startActivity(intent);
                }
            }
        });

        return view;
    }

    public void addChatMsg(String roomid, String userid, String imgUrl, String msg, String msgTime, int msgtype, String msgid,
                           String preImg, String preTitle, String preDesc) {
        ChatMessage chatMessage = new ChatMessage(roomid, imgUrl, userid, msg, msgTime, msgtype, msgid,
                preImg, preTitle, preDesc);

        chatMessage.setRoomid(roomid);
        chatMessage.setProfileImg(imgUrl);
        chatMessage.setUserid(userid);
        chatMessage.setMsg(msg);
        chatMessage.setMsgTime(msgTime);
        chatMessage.setMsgtype(msgtype);
        chatMessage.setMsgid(msgid);
        chatMessage.setPreImg(preImg);
        chatMessage.setPreTitle(preTitle);
        chatMessage.setPreDesc(preDesc);

        msgs.add(chatMessage);

    }

    public void delChatMsg(String msgid) {
        for (int i = 0; i < msgs.size(); i++) {
            ChatMessage chatMessage = msgs.get(i);
            //type이 5인 메시지 지우기
            if (chatMessage.getMsgtype() == 4 && chatMessage.getMsgTime().equals(msgid)) {
                msgs.remove(i);
                Log.d("ChatMessageAdapter ", chatMessage.getMsgTime());
            }
        }
    }



    private boolean copyFile(File file , String save_file){
        boolean result;
      //  File file = new File(infile);
        if(file!=null&&file.exists()){
            try {
                FileInputStream fis = new FileInputStream(file);
                FileOutputStream newfos = new FileOutputStream(save_file);
                int readcount=0;
                byte[] buffer = new byte[1024];
                while((readcount = fis.read(buffer,0,1024))!= -1){
                    newfos.write(buffer,0,readcount);
                }
                newfos.close();
                fis.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            result = true;
        }else{
            result = false;
        }
        return result;
    }


    class DownloadAsync extends AsyncTask<String, String , Boolean> {

        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("다운로드중...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {

            boolean result;
            String filedir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/babtalk/";
            File file = new File(filedir);
            if (!file.isDirectory()) {
                file.mkdir();
            }

            File videoFile = new File("" + context.getFileStreamPath(params[0]));

            if(file!=null&&file.exists()){
                try {
                    FileInputStream fis = new FileInputStream(videoFile);
                    FileOutputStream newfos = new FileOutputStream(filedir+params[0]);
                    int readcount=0;
                    byte[] buffer = new byte[1024];
                    while((readcount = fis.read(buffer,0,1024))!= -1){
                        newfos.write(buffer,0,readcount);
                    }
                    newfos.close();
                    fis.close();

                    MediaScanner scanner = MediaScanner.newInstance(context);
                    scanner.mediaScanning(filedir+params[0]);
                    Log.d("dddd", videoFile.getAbsolutePath() + "/ "+filedir+params[0]);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                result = true;
            }else{
                result = false;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            progressDialog.dismiss();
            Toast.makeText(context, "다운로드 완료되었습니다", Toast.LENGTH_SHORT).show();
        }
    }


}


