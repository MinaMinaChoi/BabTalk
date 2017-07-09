package com.example.cmina.openmeeting.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.cmina.openmeeting.R;
import com.example.cmina.openmeeting.utils.SaveSharedPreference;
import com.example.cmina.openmeeting.activity.LoginActivity;
import com.example.cmina.openmeeting.activity.MainActivity;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

/**
 * Created by cmina on 2017-06-09.
 */

public class MyPageFragment extends Fragment {

    ImageView profileImage;
    TextView userid, userphone, userEmail, userArea, userBrief;
    Button goLoginBtn;


    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {


        if (SaveSharedPreference.getUserid(getContext()).length() > 0 ) { //로그인상태면 해당유저의 정보보여주기

            setHasOptionsMenu(true); //로그인상태면 옵션메뉴 있도록

            View view = inflater.inflate(R.layout.fragment_mypage, container, false) ;

            profileImage = (ImageView) view.findViewById(R.id.userImageView);
            userid = (TextView) view.findViewById(R.id.userid);
            userphone = (TextView) view.findViewById(R.id.userPhone);
            userEmail = (TextView) view.findViewById(R.id.userEmail);
            userArea = (TextView) view.findViewById(R.id.userArea);
            userBrief = (TextView) view.findViewById(R.id.userBrief);

            if (SaveSharedPreference.getUserimage(getContext()).length() == 4 ) {
                Glide.with(getContext()).load(R.drawable.userdefault).bitmapTransform(new CropCircleTransformation(getContext())).into(profileImage);
            } else {
                Glide.with(getContext()).load(SaveSharedPreference.getUserimage(getContext())).bitmapTransform(new CropCircleTransformation(getContext())).into(profileImage);
            }

          //  Log.e("my page", SaveSharedPreference.getUserimage(getContext()).length()+"");

            userid.setText(SaveSharedPreference.getUserid(getContext()));
            userphone.setText(SaveSharedPreference.getUserphone(getContext()));
            userEmail.setText(SaveSharedPreference.getUseremail(getContext()));
            userArea.setText(SaveSharedPreference.getUserarea(getContext()));
            Log.e("my page", SaveSharedPreference.getUserbrief(getContext())+"");

            if (SaveSharedPreference.getUserbrief(getContext()).equals("null")) {
                userBrief.setText("");
            } else {
                userBrief.setText(SaveSharedPreference.getUserbrief(getContext()));

            }

            return view;

        } else { //로그아웃상태이면
            View view = inflater.inflate(R.layout.fragment_nonlogin, container, false);

            goLoginBtn = (Button) view.findViewById(R.id.gologinBtn);

            goLoginBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intent);
                }
            });

            return view;

        }


    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.mypagetoolbar, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.logout :
                //자체로그아웃
                SaveSharedPreference.clearUserInfo(getContext());

                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                return true;

            case R.id.update :
                Toast.makeText(getContext(), "회원정보수정으로 이동", Toast.LENGTH_SHORT).show();
                return true;

        }
        return false;
    }

}
