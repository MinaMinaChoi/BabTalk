package com.example.cmina.openmeeting.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
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
import com.example.cmina.openmeeting.activity.ChatActivity;
import com.example.cmina.openmeeting.activity.WebViewActivity;
import com.example.cmina.openmeeting.utils.ChatMessage;
import com.example.cmina.openmeeting.R;
import com.example.cmina.openmeeting.utils.OGTag;
import com.example.cmina.openmeeting.utils.OkHttpRequest;
import com.example.cmina.openmeeting.utils.Protocol;
import com.example.cmina.openmeeting.utils.SaveSharedPreference;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
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
        TextView chatTimeTextView;
        TextView chatUseridTextView;
        ImageView profileImageView;

        ImageView chatImageView;
        ImageView videoImage;

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

            viewHolder.previewLinear = (LinearLayout) view.findViewById(R.id.Urlpreview);
            viewHolder.previewImg = (ImageView) view.findViewById(R.id.previewImg);
            viewHolder.previewTitle = (TextView) view.findViewById(R.id.previewTitle);
            viewHolder.previewDesc = (TextView) view.findViewById(R.id.previewDesc);

            view.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

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

            if (chatMessage.getMsgtype() == 0)  { //일반메시지일 경우
                viewHolder.chatMessageTextView.setVisibility(View.VISIBLE);
                viewHolder.chatImageView.setVisibility(View.GONE);
                viewHolder.videoImage.setVisibility(View.GONE);
                viewHolder.progressBar.setVisibility(View.GONE);
                viewHolder.sendfail.setVisibility(View.GONE);
                viewHolder.chatMessageTextView.setBackground(context.getResources().getDrawable(R.drawable.bubble_right2));
                viewHolder.chatMessageTextView.setText(chatMessage.getMsg());

                //수정중
                //msg가 url형식일 경우, 미리보기 보여주기
                String regex = "^(((http(s?))\\:\\/\\/)?)([0-9a-zA-Z\\-]+\\.)+[a-zA-Z]{2,6}(\\:[0-9]+)?(\\/\\S*)?$";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(chatMessage.getMsg());

                if (matcher.find()) {
                    //형식이 도메인형식이라면..
                    viewHolder.previewLinear.setVisibility(View.VISIBLE);
                    final OGTag ogTag = new OGTag();
                    //addOGTypeMemo(chatMessage.getMsg(), ogTag, viewHolder);
                    requestOgTag(chatMessage.getMsg(), ogTag, viewHolder);

                    viewHolder.previewLinear.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //해당 url로 이동
                            if (!ogTag.getOgUrl().toString().equals("")) {
                                Intent intent = new Intent((ChatActivity)context, WebViewActivity.class);
                                intent.putExtra("url", ogTag.getOgUrl().toString());
                                intent.putExtra("title", ogTag.getOgTitle().toString());
                                context.startActivity(intent);
                            }
                        }
                    });

                    viewHolder.chatMessageTextView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!ogTag.getOgUrl().toString().equals("")) {
                                Intent intent = new Intent((ChatActivity)context, WebViewActivity.class);
                                intent.putExtra("url", ogTag.getOgUrl().toString());
                                intent.putExtra("title", ogTag.getOgTitle().toString());
                                context.startActivity(intent);
                            }
                        }
                    });

                } else {
                   //일반 메시지
                    viewHolder.previewLinear.setVisibility(View.GONE);
                }


            } else if (chatMessage.getMsgtype() == 1){ //이미지일경우,
                viewHolder.chatMessageTextView.setVisibility(View.GONE);
                viewHolder.videoImage.setVisibility(View.GONE);
                viewHolder.progressBar.setVisibility(View.GONE);
                viewHolder.sendfail.setVisibility(View.GONE);
                viewHolder.previewLinear.setVisibility(View.GONE);
                viewHolder.chatImageView.setVisibility(View.VISIBLE);
                Glide.with(context).load(context.getFileStreamPath(chatMessage.getMsg())).into(viewHolder.chatImageView);
///data/data/com.androidhuman.app/files/filename.ext

            } else {

                Bitmap image = null;

                //보내기 실패하면, 엑스표시
                if (chatMessage.getMsgtype()== 4 ) {
                    image = ThumbnailUtils.createVideoThumbnail(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/"+chatMessage.getMsg(), android.provider.MediaStore.Video.Thumbnails.MINI_KIND);
                    Log.d("msgtype =4일 때 ", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/"+chatMessage.getMsg());
                    viewHolder.chatTimeTextView.setVisibility(View.GONE);
                    viewHolder.sendfail.setVisibility(View.VISIBLE);
                    viewHolder.videoImage.setVisibility(View.GONE);
                } else {
                    image = ThumbnailUtils.createVideoThumbnail(""+context.getFileStreamPath(chatMessage.getMsg()), android.provider.MediaStore.Video.Thumbnails.MINI_KIND);
                    Log.d("msgtype =2일 때 ", ""+context.getFileStreamPath(chatMessage.getMsg()));
                    viewHolder.chatTimeTextView.setVisibility(View.VISIBLE);
                    viewHolder.sendfail.setVisibility(View.GONE);
                    viewHolder.videoImage.setVisibility(View.VISIBLE);
                }

                viewHolder.videoImage.setVisibility(View.VISIBLE);
                viewHolder.chatMessageTextView.setVisibility(View.GONE);
                viewHolder.previewLinear.setVisibility(View.GONE);
                viewHolder.chatImageView.setVisibility(View.VISIBLE);

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
            viewHolder.sendfail.setVisibility(View.GONE);
            viewHolder.previewLinear.setVisibility(View.GONE);
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
                viewHolder.progressBar.setVisibility(View.GONE);
                viewHolder.sendfail.setVisibility(View.GONE);
                viewHolder.chatMessageTextView.setText(chatMessage.getMsg());
                viewHolder.chatMessageTextView.setBackground(context.getResources().getDrawable(R.drawable.bubble_left2));

                //수정중
                //msg가 url형식일 경우, 미리보기 보여주기
                String regex = "^(((http(s?))\\:\\/\\/)?)([0-9a-zA-Z\\-]+\\.)+[a-zA-Z]{2,6}(\\:[0-9]+)?(\\/\\S*)?$";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(chatMessage.getMsg());

                if (matcher.find()) {
                    Log.d("ChatMessageAdapter", ""+chatMessage.getMsg());
                    //형식이 도메인형식이라면..
                    viewHolder.previewLinear.setVisibility(View.VISIBLE);
                    final OGTag ogTag = new OGTag();
                    requestOgTag(chatMessage.getMsg(), ogTag, viewHolder);

                    viewHolder.previewLinear.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //해당 url로 이동
                            if (!ogTag.getOgUrl().toString().equals("")) {
                                Intent intent = new Intent((ChatActivity)context, WebViewActivity.class);
                                intent.putExtra("url", ogTag.getOgUrl().toString());
                                intent.putExtra("title", ogTag.getOgTitle().toString());
                                context.startActivity(intent);
                            }
                        }
                    });

                    viewHolder.chatMessageTextView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!ogTag.getOgUrl().toString().equals("")) {
                                Intent intent = new Intent(context, WebViewActivity.class);
                                intent.putExtra("url", ogTag.getOgUrl().toString());
                                intent.putExtra("title", ogTag.getOgTitle().toString());
                                context.startActivity(intent);
                            }
                        }
                    });

                } else {
                    //일반 메시지
                    viewHolder.previewLinear.setVisibility(View.GONE);
                }

            } else if (chatMessage.getMsgtype() == 1){ //이미지일경우,
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
                } else if (chatMessage.getMsgtype() == 4 ) { //보내기 실패했을 때, 썸네일 클릭하면 다시 보내지도록..
                    //수정중
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    AlertDialog alertDialog = builder.setMessage("파일을 재전송하시겠습니까?")
                            .setPositiveButton("재전송", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    realPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/"+chatMessage.getMsg();

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

                                    ((ChatActivity)context).socketService.send_byte(protocol.getPacket());
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
                                        Log.d("ChatMessageAdapter", "보내는 정보확인"+chatMessage.getUserid() + chatMessage.getMsg());

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
                                                    ((ChatActivity)context).socketService.mCallback2.recvMsg(chatMessage.getRoomid(), chatMessage.getProfileImg(), chatMessage.getUserid(), chatMessage.getMsg(), chatMessage.getRoomid(), 2, chatMessage.getMsgTime());
                                                    ((ChatActivity)context).addHandler();


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

    public void delChatMsg(String msgid) {
        for (int i =0; i< msgs.size(); i++) {
            ChatMessage chatMessage = msgs.get(i);
            //type이 5인 메시지 지우기
            if (chatMessage.getMsgtype() == 4 && chatMessage.getMsgTime().equals(msgid)) {
                msgs.remove(i);
                Log.d("ChatMessageAdapter ", chatMessage.getMsgTime());
            }
        }
    }

    private void requestOgTag(final String url, final OGTag ret, final ViewHolder viewHolder) {
        OkHttpRequest request = new OkHttpRequest();

        try {
            request.get(url, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("request failure", call.toString());

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                    String responseStr = response.body().string();

                    Document doc = Jsoup.parse(responseStr);
                    Elements ogTags = doc.select("meta[property^=og:]");

                    if (ogTags.size() > 0) {
                        // 필요한 OGTag를 추려낸다
                        for (int i = 0; i < ogTags.size(); i++) {
                            Element tag = ogTags.get(i);

                            String text = tag.attr("property");
                            if ("og:url".equals(text)) {
                                ret.setOgUrl(tag.attr("content"));
                            } else if ("og:image".equals(text)) {
                                ret.setOgImageUrl(tag.attr("content"));
                            } else if ("og:description".equals(text)) {
                                ret.setOgDescription(tag.attr("content"));
                            } else if ("og:title".equals(text)) {
                                ret.setOgTitle(tag.attr("content"));
                            }
                        }

                        // 필요한 작업을 한다.
                        setData(ret.getOgUrl(), ret.getOgImageUrl(), ret.getOgDescription(), ret.getOgTitle(), viewHolder);

                    } else {
                        Log.e("ogTags", "없음");

                        Elements imgs = doc.getElementsByTag("img");
                        String src="";
                        if (imgs.size() > 0) {
                            src = imgs.get(0).attr("src");
                            Log.d("<img>태그들 중에서 첫번 째 요소", "//"+src);
                        }

                        doc.title();

                        System.out.println("Title: " + doc.title());
                        System.out.println("Meta Title: " + doc.select("meta[name=title]").attr("content"));
                        System.out.println("Meta Description: " + doc.select("meta[name=description]").attr("content"));

                        setData(url,src, doc.select("meta[name=description]").attr("content"), doc.title(), viewHolder);

                        return;

                    }

                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setData(final String url, final String preImg, final String desc, final String title, final ViewHolder viewHolder) {

        final Handler handler = new Handler(Looper.getMainLooper());
        //오래 걸리는 작업 work스레드에서 실행.
        new Thread(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //work스레드내부에서 명령이 실행 되는 것이 아니라.
                        //handler.post..에 의해, 메시지 큐에 추가 되어 uithread가 차례대로 처리

                        viewHolder.chatMessageTextView.setText(url);
                        viewHolder.previewDesc.setText(desc);
                        viewHolder.previewTitle.setText(title);
                        Glide.with(context).load(preImg).into(viewHolder.previewImg);

                    }
                });
            }
        }).start();

    }

}
