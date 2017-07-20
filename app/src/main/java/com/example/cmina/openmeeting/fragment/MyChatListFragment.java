package com.example.cmina.openmeeting.fragment;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.cmina.openmeeting.activity.ChatActivity;
import com.example.cmina.openmeeting.activity.LoginActivity;
import com.example.cmina.openmeeting.service.SocketService;
import com.example.cmina.openmeeting.utils.ChatListItem;
import com.example.cmina.openmeeting.utils.ManchanItem;
import com.example.cmina.openmeeting.R;
import com.example.cmina.openmeeting.utils.SaveSharedPreference;
import com.example.cmina.openmeeting.utils.SimpleDividerItemDecoration;

import java.util.ArrayList;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

import static com.example.cmina.openmeeting.R.id.chatImage;
import static com.example.cmina.openmeeting.R.id.custom;
import static com.example.cmina.openmeeting.activity.MainActivity.cursor;
import static com.example.cmina.openmeeting.activity.MainActivity.myDatabaseHelper;

/**
 * Created by cmina on 2017-06-09.
 */

public class MyChatListFragment extends Fragment {


    public SocketService socketService; //연결할 서비스
    public boolean IsBound ; //서비스 연결여부

    //내가 참여한 채팅방의 목록
    //sqlite에 담아둔 것을 불러오는 것

    RecyclerView mychatlistRecyclerview;
    LinearLayoutManager linearLayoutManager;
    MyChatListAdapter myChatListAdapter;
    TextView myChatTextView;
    TextView noChatTextView;
    ArrayList<ChatListItem> items = new ArrayList<ChatListItem>();

    //방별로 최신메시지와 시간을 알려주기 위한 커서.
    Cursor msgCursor;
    Cursor timeCursor;
    Cursor typeCursor;

    //서비스에 바인드하기 위해서, ServiceConnection인터페이스를 구현하는 개체를 생성
    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            SocketService.LocalBinder binder = (SocketService.LocalBinder) iBinder;
            socketService = binder.getService(); //서비스 받아옴
            // socketService.registerCallback(callback); //콜백 등록
            IsBound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            //socketService = null;
            IsBound = false;
        }

    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("마이챗fragment - oncreate", "oncreate");

    }

    @Override
    public void onStop() {
        super.onStop();
        doUnbindService();
    }

    @Override
    public void onResume() {
        super.onResume();

        doBindService();
        myChatListAdapter.notifyDataSetChanged();
    }



    private void doBindService() {
        if (!IsBound) {
            getActivity().bindService(new Intent(getContext(), SocketService.class), serviceConnection, Context.BIND_AUTO_CREATE);
            Log.d("my chat 프래그먼트 onResume", "바인드서비스"+IsBound);
            IsBound = true;
        }
    }

    private void doUnbindService() {
        if (IsBound) {
            getActivity().unbindService(serviceConnection);
            Log.d("my chat 프래그먼트 onStop", "언바인드서비스"+IsBound);
            IsBound = false;
        }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_mychatlist, container, false);

        mychatlistRecyclerview = (RecyclerView) view.findViewById(R.id.mychatlistRecyclerview);
        mychatlistRecyclerview.setHasFixedSize(true);
        linearLayoutManager = new LinearLayoutManager(getContext());
        mychatlistRecyclerview.setLayoutManager(linearLayoutManager);

        myChatListAdapter = new MyChatListAdapter(items, getContext());
        mychatlistRecyclerview.setAdapter(myChatListAdapter);

        myChatTextView = (TextView) view.findViewById(R.id.mychatTextView);
        noChatTextView = (TextView) view.findViewById(R.id.nochatlist);

        if (SaveSharedPreference.getUserid(getContext()).toString().equals("")) { //로그인이 된 상태가 아니라면,

        } else {
            myChatTextView.setVisibility(View.GONE);

            //로그인 중이라면, 저장된 채팅방목록이 있으면 보여주고, 없으면 텍스트뷰만
            cursor = myDatabaseHelper.getChatRooms(SaveSharedPreference.getUserid(getContext()));

            Log.e("chatList 개수확인", "Count = " + cursor.getCount());

            if (cursor.getCount() != 0) {
                //리스트 보여주기
                mychatlistRecyclerview.setVisibility(View.VISIBLE);

            } else { //참여한 채팅방 없을 때.
                noChatTextView.setVisibility(View.VISIBLE);
            }

            //sqlite에서 목록 가져오기

            while (cursor.moveToNext()) {

                String roomid = cursor.getString(cursor.getColumnIndex("roomid"));
                msgCursor = myDatabaseHelper.getRecentMsg(roomid);
                timeCursor = myDatabaseHelper.getRecentTime(roomid);
                typeCursor = myDatabaseHelper.getRecentMsgType(roomid);
                msgCursor.moveToFirst();
                timeCursor.moveToFirst();
                typeCursor.moveToFirst();
                //cursor.getString() : 테이블의 실제 데이터 가져옴
                //cursor.getColumnIndex() : 테이블의 해당 컬럼이름을 가져옴
                if (msgCursor.getCount() != 0 && timeCursor.getCount() != 0 && typeCursor.getCount() != 0) {
                    myChatListAdapter.addItem(cursor.getString(cursor.getColumnIndex("roomid")), cursor.getString(cursor.getColumnIndex("roomtitle")),
                            cursor.getString(cursor.getColumnIndex("imageurl")),
                            msgCursor.getString(msgCursor.getColumnIndex("msg")),
                            timeCursor.getString(timeCursor.getColumnIndex("time")).substring(12),
                            typeCursor.getInt(typeCursor.getColumnIndex("type")));
                } else {
                    myChatListAdapter.addItem(cursor.getString(cursor.getColumnIndex("roomid")), cursor.getString(cursor.getColumnIndex("roomtitle")),
                            cursor.getString(cursor.getColumnIndex("imageurl")),
                            "",
                            "", 0);
                }

                myChatListAdapter.notifyDataSetChanged();

            }

            mychatlistRecyclerview.addItemDecoration(new SimpleDividerItemDecoration(getContext()));

            cursor.close();

        }
        return view;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private class MyChatListAdapter extends RecyclerView.Adapter<MyChatListAdapter.ViewHolder> {

        private Context context;
        private ArrayList<ChatListItem> items;

        MyChatListAdapter(ArrayList<ChatListItem> items, Context context) {
            this.items = items;
            this.context = context;
        }

        @Override
        public MyChatListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mychatlistitem, parent, false);
            MyChatListAdapter.ViewHolder holder = new MyChatListAdapter.ViewHolder(view);

            return holder;
        }

        @Override
        public void onBindViewHolder(MyChatListAdapter.ViewHolder holder, int position) {
            holder.chatTitle.setText(items.get(position).getRoomtitle());
            if (items.get(position).getRecent_msg_type() == 0) {
                holder.recent_msg.setText(items.get(position).getRecent_msg());
            } else if (items.get(position).getRecent_msg_type() == 1) {
                holder.recent_msg.setText("이미지");
            } else if (items.get(position).getRecent_msg_type() == 2) {
                holder.recent_msg.setText("동영상");
            }
            holder.recent_msg_time.setText(items.get(position).getRecent_msg_time());
            if (items.get(position).getImgurl().equals("")) {
                Glide.with(context).load(R.drawable.userdefault).bitmapTransform(new CropCircleTransformation(context)).into(holder.chatImageView);
            } else {
                Glide.with(context).load(items.get(position).getImgurl()).bitmapTransform(new CropCircleTransformation(context)).into(holder.chatImageView);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public void addItem(String roomid, String roomtitle, String imageurl, String recent_msg, String recent_time, int recent_type) {
            ChatListItem item = new ChatListItem(roomid, roomtitle, imageurl, recent_msg, recent_time, recent_type);

            item.setRoomid(roomid);
            item.setRoomtitle(roomtitle);
            item.setImgurl(imageurl);
            item.setRecent_msg(recent_msg);
            item.setRecent_msg_time(recent_time);
            item.setRecent_msg_type(recent_type);

            items.add(item);

        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView chatTitle;
            TextView recent_msg;
            TextView recent_msg_time;
            ImageView chatImageView;

            public ViewHolder(View itemView) {
                super(itemView);

                chatImageView = (ImageView) itemView.findViewById(R.id.chatImage);
                chatTitle = (TextView) itemView.findViewById(R.id.chatTitle);
                recent_msg = (TextView) itemView.findViewById(R.id.recent_msg);
                recent_msg_time = (TextView) itemView.findViewById(R.id.recent_time);

                //해당 채팅방으로 이동
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Intent intent = new Intent(getActivity(), ChatActivity.class);
                        intent.putExtra("roomid", items.get(getPosition()).getRoomid());
                        startActivity(intent);

                    }
                });


                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {

                        AlertDialog dialog;
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        dialog = builder.setMessage("해당방을 삭제하겠습니까?")
                                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        boolean delChatroom = myDatabaseHelper.deleteChatRoom(items.get(getPosition()).getRoomid());
                                        if (delChatroom) {
                                            //채팅방목록 새로 갱신하기 위해 기존의 것은 지우고.
                                            myChatListAdapter.items.clear();
                                            myChatListAdapter.notifyDataSetChanged();
                                            Toast.makeText(getContext(), "채팅방지움성공", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getContext(), "지움실패", Toast.LENGTH_SHORT).show();
                                        }

                                        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                                        transaction.detach(MyChatListFragment.this).attach(MyChatListFragment.this).commit();

                                    }
                                })
                                .setNegativeButton("아니오", null)
                                .create();
                        dialog.show();

                        return false;
                    }
                });
            }
        }
    }

}
