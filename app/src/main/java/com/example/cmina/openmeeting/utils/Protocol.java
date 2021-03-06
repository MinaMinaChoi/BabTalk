package com.example.cmina.openmeeting.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;

import static android.R.attr.data;
import static android.R.attr.thumb;

/**
 * Created by cmina on 2017-06-17.
 */

public class Protocol implements Serializable {

    public static final int PT_UNDEFINED = -1; //프로토콜 지정 안되었을때
    public static final int PT_CREATEROOM = 0; //방만들기
    public static final int PT_JOINROOM = 1; //방에 들어가기
    public static final int PT_CHECK = 5; //소켓연결체크

    //프로토콜 수정
    public static final int PT_CHAT_MSG = 6;
    public static final int PT_CHAT_IMG = 7;
    public static final int PT_CHAT_MOVIE = 9;
    public static final int PT_OFFSET = 2; //이어받기 오프셋위치 확인용

    public static final int cliToServer = 1;
    public static final int serverToCli = 2;

    public static final int SOCKET_CHECK = 8; //클라이언트에서 소켓체크하기 위해 보내는 데이터

    public static final int LEN_MSG_ID = 20; //메시지 id, 서버에 도착했을 때의 time

    public static final int LEN_OFFSET_CHECK = 10; //다운로드 중단 위치의 길이

    public static final int LEN_CHECK_TYPE = 1; //클라->서버 타입인지, 서버->클라 타입인지

    public static final int LEN_TOTAL_LEN = 32; //젤처음에, 패킷전체 길이

    public static final int LEN_FILE_NAME = 100; //이미지파일 이름 길이
    public static final int LEN_SOCKET_CHECK = 1; // 소켓연결상태 확인
    public static final int LEN_ROOM_ID = 10; //roomid 길이
    public static final int LEN_USER_ID = 20; //userid 길이
    public static final int LEN_USER_IMG = 50; //userimg 길이, http://13.124.77.49/thumbnail/userid.jpg
    public static final int LEN_PROTOCOL_TYPE = 1;  //프로토콜타입 길이
    public static final int LEN_MAX = 4096;    //최대 데이타 길이


    protected int protocolType;
    private byte[] packet; //프로토콜과 데이터의 저장공간이 되는 바이트 배열.

    protected int totalLength;

    //생성자
    public Protocol() {
        getPacket(LEN_MAX);
    }

    //생성자
    public Protocol(int packetLen) {
        // this.protocolType = protocolType;
        //어떤 상수를 생성자에 넣어 프로코톨 클래스를 생성하느냐에 따라서 바이트 배열의 packet length가 결정.
        getPacket(packetLen);
    }

/*
    public Protocol(int protocolType, int total) {
        this.totalLength = total;
        this.protocolType = protocolType;
        //어떤 상수를 생성자에 넣어 프로코톨 클래스를 생성하느냐에 따라서 바이트 배열의 packet length가 결정.
        getPacket( protocolType, total);
    }
*/

/*

    public byte[] getPacket(int protocolType, int dataLen) {
        if (packet == null) {
            switch (protocolType) {
                case PT_CREATEROOM: //CreateRoom/roomid/userid 이렇게
                    packet = new byte[LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID];
                    break;
                case PT_JOINROOM: //JoinRoom/roomid/userid
                    packet = new byte[LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID];
                    break;
                case PT_CHAT_MSG :
                    packet = new byte[LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG + LEN_MSG_ID+ dataLen];
                    break;

                case PT_CHAT_IMG :
                    packet = new byte[LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG + LEN_MSG_ID+ dataLen];
                    break;

                case PT_CHAT_MOVIE :
                    packet = new byte[LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG + LEN_MSG_ID + LEN_FILE_NAME  + dataLen];
                    break;

                case PT_OFFSET : //244
                    packet = new byte[LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG  + LEN_MSG_ID +LEN_FILE_NAME + LEN_OFFSET_CHECK + LEN_CHECK_TYPE ];
                    break;

                case PT_UNDEFINED:
                    packet = new byte[LEN_MAX];
                    break;
                case PT_CHECK: //메시지를 받았다는 ok사인으로.
                    packet = new byte[LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_SOCKET_CHECK];
                    break;
            }
        }

        packet[0] = (byte) protocolType; //packet 바이트배열의 첫번째 방에 프로토콜타입 상수를 셋팅해놓는다.

        return packet;
    }
*/

    public byte[] getPacket(int totalLen) { //전체길이만큼 바이트배열을 준비!
        if (packet == null) {
            packet = new byte[totalLen];
        }
        return packet;
    }


    public void setTotalLen(String totalLen) {
        System.arraycopy(totalLen.trim().getBytes(), 0, packet, 0, totalLen.trim().getBytes().length);
    }

    public String getTotalLen() {
        return new String(packet, 0, LEN_TOTAL_LEN).trim();
    }


    public byte[] getPacket() {
        return packet;
    }


/*    //default생성자로 생성한 후 protocol 클래스의 packet데이터를 바꾸기 위한 메소드
    public void setPacket(int pt, byte[] buf) {
        packet = null;
        packet = getPacket(pt);
        protocolType = pt;
        System.arraycopy(buf, 0, packet, 0, packet.length);

    }*/

    //default생성자로 생성한 후 protocol 클래스의 packet데이터를 바꾸기 위한 메소드
    //totalLen < buf.length
    public void setPacket(int totalLen, byte[] buf) {
        packet = null;
        packet = getPacket(totalLen);
        //  protocolType = pt;
        //arraycopy(전송원배열, 소스배열개시위치, 전송처배열, 정송처 데이터내의 게시위치, 카피되는 배열크기)
        System.arraycopy(buf, 0, packet, 0, packet.length);
    }

    //default생성자로 생성한 후 protocol 클래스의 packet데이터를 바꾸기 위한 메소드
    //totalLen > buf.length
    public void setPacket2(int totalLen, byte[] buf) {
        packet = null;
        packet = getPacket(totalLen);
        //arraycopy(전송원배열, 소스배열개시위치, 전송처배열, 정송처 데이터내의 게시위치, 카피되는 배열크기)
        System.arraycopy(buf, 0, packet, 0, buf.length);
    }


    //추가적으로 패킷을 읽어들일때,
    public void addPacket(int destPos, byte[] buf) {
        System.out.println("addpacket 더 읽은 데이터 길이 : " + buf.length);
        System.arraycopy(buf, 0, packet, destPos, buf.length);
    }

    //추가적으로 패킷을 읽어들일때,
    //전체크기에서 지금까지 받은 길이를 뺀만큼!!!
    public void addPacket2(int total, int destPos, byte[] buf) {
        System.out.println("addpacket2222222 더 읽은 데이터 길이 : " + buf.length);
        System.out.println("addpacket2222222 남은데이터 : " + (buf.length - (total - destPos)));
        System.arraycopy(buf, 0, packet, destPos, total - destPos);
    }


    public void setProtocolType(String protocolType) {
        System.arraycopy(protocolType.trim().getBytes(), 0, packet, LEN_TOTAL_LEN, protocolType.trim().getBytes().length);
    }

    public String getProtocolType() {
        return new String(packet, LEN_TOTAL_LEN, LEN_PROTOCOL_TYPE).trim();
    }

    //소켓연결 확인용 데이터
    public void setSocketCheck(String socketCheck) {
        System.arraycopy(socketCheck.trim().getBytes(), 0, packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID, LEN_SOCKET_CHECK);
    }

    public String getSocketCheck() {
        return new String(packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID, LEN_SOCKET_CHECK).trim();
    }


    //byte[] packet에 roomid를 byte[]로 만들어 프로토콜 타입 바로 뒷부분에 추가.
    public void setRoomid(String roomid) {
        System.arraycopy(roomid.trim().getBytes(), 0, packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE, roomid.trim().getBytes().length);
    }

    public String getRoomid() {
        return new String(packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE, LEN_ROOM_ID).trim();
    }

    public void setUserid(String userid) {
        System.arraycopy(userid.trim().getBytes(), 0, packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID, userid.trim().getBytes().length);
    }

    public String getUserid() {
        return new String(packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID, LEN_USER_ID).trim();
    }

    public void setUserimg(String userimg) {
        System.arraycopy(userimg.trim().getBytes(), 0, packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID, userimg.trim().getBytes().length);
    }

    public String getUserimg() {
        return new String(packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID, LEN_USER_IMG).trim();
    }

    //msgid 세팅
    public void setMsgId(String msgId) {
        System.arraycopy(msgId.trim().getBytes(), 0, packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG, msgId.getBytes().length);
    }

    public String getMsgId() {
        return new String(packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG, LEN_MSG_ID).trim();
    }


    public void setMsg(String msg) {
        System.arraycopy(msg.trim().getBytes(), 0, packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG + LEN_MSG_ID, msg.trim().getBytes().length);
    }

    public String getMsg() {
        System.out.println("메시지 길이" + (Integer.parseInt(getTotalLen()) - 133));
        return new String(packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG + LEN_MSG_ID, Integer.parseInt(getTotalLen()) - 133).trim();
    }


    //이미지의 길이만큼 바이트배열을 만들어서, 패킷에서 이미지를 나타내는 바이트배열만 data에 담는다
    public byte[] getImg(int imglength) {
        byte[] data = new byte[imglength];
        System.arraycopy(packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG + LEN_MSG_ID, data, 0, imglength);
        return data;
    }

    public void sendImage(String filename) {
        File file = new File(filename);
        try {
            //파일 읽기
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);

            int len;
            int 이전len = 0;
            int size = 4096;
            //   int total_len=0;
            byte[] data = new byte[size];
            while ((len = bis.read(data)) != -1) { //더이상 읽을 것이 없을 때 -1
                System.arraycopy(data, 0, packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG + LEN_MSG_ID + 이전len, len);
                //  total_len += len;
                이전len += len;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //바이트배열을 그대로 보내기?실시간으로 데이터 주고받을 수 있을 때
    public void sendImage(byte[] data) {
        System.arraycopy(data, 0, packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG + LEN_MSG_ID, data.length);
    }


    //파일 이어받기 시도중
    public void setOffSet(String offSet) {
        System.arraycopy(offSet.trim().getBytes(), 0, packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG + LEN_MSG_ID + LEN_FILE_NAME, offSet.trim().getBytes().length);
    }

    public String getOffSet() {
        return new String(packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG + LEN_MSG_ID + LEN_FILE_NAME, LEN_OFFSET_CHECK).trim();
    }

    public void setCheckType(String checkType) {
        System.arraycopy(checkType.trim().getBytes(), 0, packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG + LEN_MSG_ID + LEN_FILE_NAME + LEN_OFFSET_CHECK, checkType.trim().getBytes().length);
    }

    public String getCheckType() {
        return new String(packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG + LEN_MSG_ID + LEN_FILE_NAME + LEN_OFFSET_CHECK, LEN_CHECK_TYPE).trim();
    }

    //2017.08.05
    public void setThumb(byte[] thumb) {
        System.arraycopy(thumb, 0, packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG + LEN_MSG_ID + LEN_FILE_NAME + LEN_OFFSET_CHECK + LEN_CHECK_TYPE, thumb.length);
    }

    public byte[] getThumb(int thumbLen) {
        byte[] data = new byte[thumbLen];
        System.arraycopy(packet,                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG + LEN_MSG_ID + LEN_FILE_NAME + LEN_OFFSET_CHECK + LEN_CHECK_TYPE, data, 0, thumbLen);
        return data;
    }


    //특정위치부터 파일 전송하기기
    public void sendVideo(String filename, int offset) {

        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(filename, "r");
            randomAccessFile.seek(offset);

            FileInputStream fis = new FileInputStream(randomAccessFile.getFD());
            BufferedInputStream bis = new BufferedInputStream(fis);

            int len;
            int 이전len = 0;
            int size = 4096;
            //   int total_len=0;
            byte[] data = new byte[size];
            while ((len = bis.read(data)) != -1) { //더이상 읽을 것이 없을 때 -1
                System.arraycopy(data, 0, packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG + LEN_MSG_ID + LEN_FILE_NAME + 이전len, len);
                이전len += len;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //동영상 전송 테스트중
    public void setFileName(String fileName) {
        System.arraycopy(fileName.trim().getBytes(), 0, packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG + LEN_MSG_ID, fileName.trim().getBytes().length);
    }

    public String getFileName() {
        return new String(packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG + LEN_MSG_ID, LEN_FILE_NAME).trim();
    }

    //이미지의 길이만큼 바이트배열을 만들어서, 패킷에서 이미지를 나타내는 바이트배열만 data에 담는다
    public byte[] getVideo(int imglength) {
        byte[] data = new byte[imglength];
        System.arraycopy(packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG + LEN_MSG_ID + LEN_FILE_NAME, data, 0, imglength);
        return data;
    }

    public void sendVideo(String filename) {
        File file = new File(filename);
        try {
            //파일 읽기
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);

            int len;
            int 이전len = 0;
            int size = 4096;
            //   int total_len=0;
            byte[] data = new byte[size];
            while ((len = bis.read(data)) != -1) { //더이상 읽을 것이 없을 때 -1
                System.arraycopy(data, 0, packet, LEN_TOTAL_LEN + LEN_PROTOCOL_TYPE + LEN_ROOM_ID + LEN_USER_ID + LEN_USER_IMG + LEN_MSG_ID + LEN_FILE_NAME + 이전len, len);
                이전len += len;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
