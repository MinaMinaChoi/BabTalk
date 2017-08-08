package com.example.cmina.openmeeting.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.cmina.openmeeting.activity.ChatActivity;
import com.example.cmina.openmeeting.activity.MainActivity;
import com.example.cmina.openmeeting.service.SocketService;
import com.example.cmina.openmeeting.utils.ManchanItem;
import com.example.cmina.openmeeting.utils.OkHttpRequest;
import com.example.cmina.openmeeting.R;
import com.example.cmina.openmeeting.utils.Protocol;
import com.example.cmina.openmeeting.utils.SaveSharedPreference;
import com.example.cmina.openmeeting.utils.UIHandler;
import com.example.cmina.openmeeting.activity.LoginActivity;
import com.example.cmina.openmeeting.activity.OpenActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import jp.wasabeef.glide.transformations.CropCircleTransformation;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.example.cmina.openmeeting.activity.MainActivity.cursor;
import static com.example.cmina.openmeeting.activity.MainActivity.myDatabaseHelper;


/**
 * Created by cmina on 2017-06-09.
 */

public class ManchanListFragment extends Fragment {

    public SocketService socketService; //연결할 서비스
    public boolean IsBound ; //서비스 연결여부

    AlertDialog alertDialog;

    Button sortBtn;
    int sortCase;

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd a hh:mm");


    RecyclerView manchanListRecyclerView;
    LinearLayoutManager linearLayoutManager;
    ManChanAdapter manChanAdapter;

    JSONObject jsonItem;

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
        Log.d("만찬fragment - oncreate", "oncreate");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void doBindService() {
        if (!IsBound) {
            getActivity().bindService(new Intent(getContext(), SocketService.class), serviceConnection, Context.BIND_AUTO_CREATE);
            Log.d("ManchanListFragment onResume", "바인드서비스");
            IsBound = true;
        }

    }

    private void doUnbindService() {
        if (IsBound) {
            getActivity().unbindService(serviceConnection);
            Log.d("ManchanListFragment onStop", "언바인드서비스");
            IsBound = false;
        }
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
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_manchanlist, container, false);

        setHasOptionsMenu(true); //fragment에서 옵션메뉴를 동작하게 하기위해서 반드시 필요

        getActivity().setTitle("같이밥먹자");


        manchanListRecyclerView = (RecyclerView) view.findViewById(R.id.manchanListRecyclerView);
        manchanListRecyclerView.setHasFixedSize(true);

        linearLayoutManager = new LinearLayoutManager(getContext());
        manchanListRecyclerView.setLayoutManager(linearLayoutManager);

        sortBtn = (Button) view.findViewById(R.id.sortBtn);

        sortBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MaterialDialog.Builder(getContext())
                        .title("정렬")
                        .items(R.array.sort)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                                switch (position) {
                                    case 0:
                                        //기존의 리스트뷰 기본!!
                                        sortCase = 0;
                                        manChanAdapter.sort(sortCase);
                                        sortBtn.setText("등록순");
                                        break;
                                    case 1:
                                        sortCase = 1;
                                        manChanAdapter.sort(sortCase);
                                        sortBtn.setText("일시순");
                                        //일자순!!으로 다시 요청!!??
                                        //이미 받은 자료를 재정렬하는게 낫겟지;?
                                        break;
                                }
                            }
                        })
                        .negativeText("취소")
                        .show();
            }
        });

        OkHttpRequest request = new OkHttpRequest();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            try {
                request.post("http://13.124.77.49/getManchanList.php", "null", new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        UIHandler uiHandler = new UIHandler(getContext());
                        uiHandler.toastHandler("통신실패");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {

                        String responseStr = response.body().string();

                       // Log.e("manchanlist", responseStr);

                        //JsonArray로 결과 받기!!
                        try {
                            JSONObject object = new JSONObject(responseStr);

                            JSONArray array = object.getJSONArray("manchanlist");

                            ArrayList<ManchanItem> items = new ArrayList<ManchanItem>();

                            for (int i = 0; i < array.length(); i++) {

                                jsonItem = array.getJSONObject(i);

                                String a = jsonItem.getString("htime").substring(0, 2);

                                if (Integer.parseInt(a) >= 12) {
                                    if (Integer.parseInt(a) == 12) {
                                        a = "오후 " + a + " : " + jsonItem.getString("htime").substring(3, 5);
                                    } else {
                                        a = "오후 " + (Integer.parseInt(a) - 12) + " : " + jsonItem.getString("htime").substring(3, 5);
                                    }
                                } else {
                                    a = "오전 " + jsonItem.getString("htime").substring(0, 2) + " : " + jsonItem.getString("htime").substring(3, 5);
                                }


                                String datetime = jsonItem.getString("hdate") + " " + a;

                                items.add(new ManchanItem(String.valueOf(jsonItem.getInt("hostid")), jsonItem.getString("userid"), jsonItem.getString("image"), jsonItem.getString("htitle"),
                                        datetime, jsonItem.getString("harea"),
                                        jsonItem.getString("hbrief"), jsonItem.getLong("now")));
                            }

                            manChanAdapter = new ManChanAdapter(items, getContext());

                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    manchanListRecyclerView.setAdapter(manChanAdapter);
                                }
                            }, 0);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.openManchan:

                if (SaveSharedPreference.getUserid(getContext()).length() > 0) { //로그인된 상태면 모임 생성하러 고고
                    Intent intent = new Intent(getContext(), OpenActivity.class);
                    //openactivity가 쌓이지 않도록..
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intent);
                } else {

                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    alertDialog = builder.setMessage("밥모임을 열려면 로그인이 필요합니다. 로그인하겠습니까?")
                            .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                                    startActivity(intent);
                                }
                            })
                            .setNegativeButton("아니오", null)
                            .create();
                    alertDialog.show();

                }

                return true;
        }
        return false;
    }

    private class ManChanAdapter extends RecyclerView.Adapter<ManChanAdapter.ViewHolder> {

        private Context context;
        private ArrayList<ManchanItem> items;

        private void sort(int a) {
            //주관적인 규칙을 sort()함수에 전달하는 방법이 바로 Comparator
            //리스트 탐색(루프 실행) 및 리스트 테이터 재배치(메모리참조, 인덱스조절)은 Collection.sort()함수 내부에 이미 구현되어 있으므로,
            //개발자가 할 일은 재배치가 필요한지를 결정하기 위한 조건(데이터값 비교)을 Comparator를 통해 전달하는것.

            if (a == 0) {
                Collections.sort(items, ManchanItem.MAKETIME_COMPARATOR);
                notifyDataSetChanged();
            } else if (a == 1) {
                Collections.sort(items, ManchanItem.DATE_COMPARATOR);
                notifyDataSetChanged();
            }

        }

        ManChanAdapter(ArrayList<ManchanItem> items, Context context) {
            this.context = context;
            this.items = items;
        }

        @Override
        public ManChanAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.manchanlistitem, parent, false);
            ManChanAdapter.ViewHolder holder = new ManChanAdapter.ViewHolder(v);
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            //값 세팅
            if (items.get(position).getManchanuserImage().length() == 4) {
                Glide.with(getContext()).load(R.drawable.userdefault).bitmapTransform(new CropCircleTransformation(getContext())).into(holder.huserImage);
            } else {
                Glide.with(context)
                        .load(items.get(position).getManchanuserImage())
                        .bitmapTransform(new CropCircleTransformation(context))
                        .skipMemoryCache(true) //메모리캐싱끄기
                        .diskCacheStrategy(DiskCacheStrategy.NONE) //디스트 캐싱하지 않는다
                        .into(holder.huserImage);
            }

            Log.e("image setting", items.get(position).getManchanuserImage());

            holder.htitle.setText(items.get(position).getManchantitle());
            holder.hdatehtime.setText(items.get(position).getManchandatetime());
            holder.huser.setText(items.get(position).getUserid());
            holder.hdetailarea.setText(items.get(position).getManchandetailArea());
            holder.hbrief.setText(items.get(position).getManchanBrief());
            holder.hostid = items.get(position).getManchanid();

            Date now = new Date();
            String datetime = simpleDateFormat.format(now);

            if (holder.hdatehtime.getText().toString().compareTo(datetime) < 0) { //현재시간이 더 크면.
                holder.JoinButton.setBackgroundResource(R.drawable.inactivebtn);
                holder.JoinButton.setClickable(false);
            } else {
                holder.JoinButton.setBackgroundResource(R.drawable.btnround);
            }

            if (SaveSharedPreference.getUserid(context).equals(holder.huser.getText().toString())) { //내가 만든 채팅방은 참여하기 버튼 안보이게.
                holder.JoinButton.setVisibility(View.INVISIBLE);
            } else {
                holder.JoinButton.setVisibility(View.VISIBLE);
            }

            //이미 참여중인 채팅방에는 참여중이라고 알려주기 hostid
            cursor = myDatabaseHelper.getChatRooms(SaveSharedPreference.getUserid(getContext()));

            Log.d("ManchanListFragment", "Count = " + cursor.getCount());

            if (cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    Log.d("ManchanListFragment", "참여한 방목록"+cursor.getString(cursor.getColumnIndex("roomid")));
                    if (cursor.getString(cursor.getColumnIndex("roomid")).equals(holder.hostid)) {
                        holder.JoinButton.setText("참여중");
                        holder.JoinButton.setEnabled(false);
                        holder.JoinButton.setTextColor(R.color.textColorPrimary);
                        Log.d("참여중인 채팅방", holder.hostid);
                    } /*else {
                        holder.JoinButton.setText("참여하기");
                        holder.JoinButton.setEnabled(true);
                        holder.JoinButton.setTextColor(R.color.light_grey);
                    }*/
                }
            }


        }


        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            //hostuser의 profile image, htitle, hdate / htime, hmenu, detailarea
            String hostid;
            ImageView huserImage;
            TextView htitle;
            TextView hdatehtime;
            TextView huser;
            TextView hdetailarea;
            TextView hbrief;
            Button JoinButton;

            public ViewHolder(final View itemView) {
                super(itemView);

                huserImage = (ImageView) itemView.findViewById(R.id.manchanuserImage);
                htitle = (TextView) itemView.findViewById(R.id.manchantitle);
                hdatehtime = (TextView) itemView.findViewById(R.id.manchandatetime);
                huser = (TextView) itemView.findViewById(R.id.manchanuserid);
                hdetailarea = (TextView) itemView.findViewById(R.id.manchandetail);
                hbrief = (TextView) itemView.findViewById(R.id.manchanbriefTextView);
                JoinButton = (Button) itemView.findViewById(R.id.joinBtn);

                JoinButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if (SaveSharedPreference.getUserid(getContext()).length() > 0) { //로그인되어 있는 상태면
                            UIHandler handler = new UIHandler(getContext());
                            handler.toastHandler(items.get(getPosition()).getManchanid() + "");

                            final String roomid = String.valueOf(items.get(getPosition()).getManchanid());


                            final Protocol protocol = new Protocol(63);
                            protocol.setProtocolType("1");
                            protocol.setTotalLen("63");
                            protocol.setRoomid(roomid);
                            protocol.setUserid(SaveSharedPreference.getUserid(context));

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    socketService.send_byte(protocol.getPacket());
                                }
                            }).start();

                            //chat_rooms_table에 넣기!!!!!!!
                            myDatabaseHelper.insertChatrooms(SaveSharedPreference.getUserid(getContext()),
                                    roomid, items.get(getPosition()).getManchantitle(), items.get(getPosition()).getManchanuserImage(),
                                    "", "");


                            Intent intent = new Intent(getActivity(), ChatActivity.class);
                            //  intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra("roomid", roomid);
                            startActivity(intent);

                        } else {

                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            alertDialog = builder.setMessage("밥모임에 참여하려면 로그인이 필요합니다. 로그인하겠습니까?")
                                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                                            startActivity(intent);
                                        }
                                    })
                                    .setNegativeButton("아니오", null)
                                    .create();
                            alertDialog.show();
                            return;
                        }

                    }
                });

            }
        }
    }

}
