package com.example.cmina.openmeeting.utils;

import java.util.Comparator;

/**
 * Created by cmina on 2017-06-09.
 */

public class ManchanItem {

    // 상세페이지 출력하는 것까지 이 클래스에서 정의하고,
    //만찬리스트와 상세페이지 모두에서 사용하자?
    //아니다. 상세페이지는 걍 서버에서 셀렉트하기만하면 되는군
    //아니다...다시 요청보내지말구, 상세내역까지 다 받아두고 있다가 상세페이지에서 보여주자.
    //만찬리스트

    String manchanid; // textview에 출력은 하지 않지만. 만찬의 유니크한 값이니까 갖고 있어야.
    String manchanuserImage;
    String manchantitle;
    String manchandatetime;
   // String manchanmenu;
    String manchandetailarea;
  //  int peopleNum;
    String manchanBrief;
    String userid;
    long maketime;


    //set하는 것은 없다.
    //디비로부터 정보 받아오는 것이어서.

    public long getMaketime() {
        return maketime;
    }

    public String getManchanBrief() {
        return manchanBrief;
    }

    public String getUserid() {
        return userid;
    }

    public String getManchanid() {
        return manchanid;
    }

    public String getManchanuserImage() {
        return manchanuserImage;
    }

    public String getManchantitle() {
        return manchantitle;
    }

    public String getManchandatetime() {
        return manchandatetime;
    }

    public String getManchandetailArea() {
        return manchandetailarea;
    }

    public ManchanItem(String manchanid, String userid, String image, String title, String datetime, String detailarea,
                       String manchanBrief, long maketime) {
        this.manchanid= manchanid;
        this.userid = userid;
        this.manchanuserImage = image;
        this.manchantitle = title;
        this.manchandatetime = datetime;
      //  this.manchanmenu = menu;
        this.manchandetailarea = detailarea;
        this.manchanBrief = manchanBrief;
        this.maketime = maketime;
       // this.peopleNum = peopleNum;
    }


    public final static Comparator<ManchanItem> DATE_COMPARATOR = new Comparator<ManchanItem>() {
        @Override
        public int compare(ManchanItem i1, ManchanItem i2) {
            int ret;
            if (i1.getManchandatetime().compareTo(i2.getManchandatetime()) < 0 ) { //i1이 작은경우
                ret =1;
            } else {
                ret = -1;
            }
            return ret;
        }
    };


    public final static Comparator<ManchanItem> MAKETIME_COMPARATOR = new Comparator<ManchanItem>() {
        @Override
        public int compare(ManchanItem i1, ManchanItem i2) {
            int ret;
            if (i1.getMaketime() < i2.getMaketime() ) { //i1이 작은경우
                ret =1;
            } else {
                ret = -1;
            }
            return ret;
        }
    };
}
