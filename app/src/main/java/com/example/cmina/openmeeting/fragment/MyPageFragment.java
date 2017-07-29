package com.example.cmina.openmeeting.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.StringSignature;
import com.example.cmina.openmeeting.R;
import com.example.cmina.openmeeting.utils.OkHttpRequest;
import com.example.cmina.openmeeting.utils.SaveSharedPreference;
import com.example.cmina.openmeeting.activity.LoginActivity;
import com.example.cmina.openmeeting.activity.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jp.wasabeef.glide.transformations.CropCircleTransformation;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static android.app.Activity.RESULT_OK;

/**
 * Created by cmina on 2017-06-09.
 */

public class MyPageFragment extends Fragment {

    ImageView profileImage;
    TextView userid, userphone, userEmail, userArea, userBrief;
    Button goLoginBtn;
    final static int REQ_CODE_SELECT_IMAGE = 3001;

    String TAG = "MyPageFragment";

    public static native long loadCascade(String cascadeFileName);

    public static native boolean detect(long cascadeClassifier_face,
                                        long cascadeClassifier_eye, long matAddrInput, long matAddrResult);

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }

    public long cascadeClassifier_face = 0;
    public long cascadeClassifier_eye = 0;


    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            // OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            //mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void copyFile(String filename) {
        String baseDir = Environment.getExternalStorageDirectory().getPath();
        String pathDir = baseDir + File.separator + filename;

        AssetManager assetManager = getContext().getAssets();

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            Log.d(TAG, "copyFile :: 다음 경로로 파일복사 " + pathDir);
            inputStream = assetManager.open(filename);
            outputStream = new FileOutputStream(pathDir);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            inputStream = null;
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        } catch (Exception e) {
            Log.d(TAG, "copyFile :: 파일 복사 중 예외 발생 " + e.toString());
        }

    }

    private void read_cascade_file() {
        copyFile("haarcascade_frontalface_alt.xml");
        copyFile("haarcascade_eye_tree_eyeglasses.xml");

        Log.d(TAG, "read_cascade_file:");

        cascadeClassifier_face = loadCascade("haarcascade_frontalface_alt.xml");
        Log.d(TAG, "read_cascade_file:");

        cascadeClassifier_eye = loadCascade("haarcascade_eye_tree_eyeglasses.xml");
    }


    //여기서부턴 퍼미션 관련 메소드
    static final int PERMISSIONS_REQUEST_CODE = 1000;
    //String[] PERMISSIONS  = {"android.permission.CAMERA"};
    String[] PERMISSIONS = {"android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"};


    private boolean hasPermissions(String[] permissions) {
        int result;

        //스트링 배열에 있는 퍼미션들의 허가 상태 여부 확인
        for (String perms : permissions) {

            result = ContextCompat.checkSelfPermission(getContext(), perms);

            if (result == PackageManager.PERMISSION_DENIED) {
                //허가 안된 퍼미션 발견
                return false;
            }
        }

        //모든 퍼미션이 허가되었음
        return true;
    }


    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {


        if (SaveSharedPreference.getUserid(getContext()).length() > 0) { //로그인상태면 해당유저의 정보보여주기

            setHasOptionsMenu(true); //로그인상태면 옵션메뉴 있도록

            View view = inflater.inflate(R.layout.fragment_mypage, container, false);

            profileImage = (ImageView) view.findViewById(R.id.userImageView);
            userid = (TextView) view.findViewById(R.id.userid);
            userphone = (TextView) view.findViewById(R.id.userPhone);
            userEmail = (TextView) view.findViewById(R.id.userEmail);
            userArea = (TextView) view.findViewById(R.id.userArea);
            userBrief = (TextView) view.findViewById(R.id.userBrief);

            if (SaveSharedPreference.getUserimage(getContext()).length() == 4) {
                Glide.with(getContext()).load(R.drawable.userdefault).bitmapTransform(new CropCircleTransformation(getContext())).into(profileImage);
            } else {
                Glide.with(getContext())
                        .load("http://13.124.77.49/thumbnail/"+SaveSharedPreference.getUserid(getContext())+".jpg")
                        .skipMemoryCache(true) //메모리캐싱끄기
                        .diskCacheStrategy(DiskCacheStrategy.NONE) //디스트 캐싱하지 않는다
                        .bitmapTransform(new CropCircleTransformation(getContext()))
                        .into(profileImage);
            }


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //퍼미션 상태 확인
                if (!hasPermissions(PERMISSIONS)) {

                    //퍼미션 허가 안되어있다면 사용자에게 요청
                    requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
                } else {
                    read_cascade_file();
                }
            }

            profileImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //프로필 사진 변경하러 갤러리
                    //다이얼로그
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("프로필 이미지 변경");
                    builder.setMessage("프로필 이미지 변경하겠습니까?")
                            .setCancelable(true)
                            .setPositiveButton("갤러리", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    Intent i = new Intent(Intent.ACTION_PICK);
                                    i.setType(MediaStore.Images.Media.CONTENT_TYPE);
                                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    try {
                                        startActivityForResult(i, REQ_CODE_SELECT_IMAGE);
                                    } catch (android.content.ActivityNotFoundException e) {
                                        e.printStackTrace();
                                    }

                                }
                            })
                            .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();

                }

            });


            userid.setText(SaveSharedPreference.getUserid(getContext()));
            userphone.setText(SaveSharedPreference.getUserphone(getContext()));
            userEmail.setText(SaveSharedPreference.getUseremail(getContext()));
            userArea.setText(SaveSharedPreference.getUserarea(getContext()));
            Log.e("my page", SaveSharedPreference.getUserbrief(getContext()) + "");

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


    /**
     * Image SDCard Save (input Bitmap -> saved file JPEG)
     * Writer intruder(Kwangseob Kim)
     *
     * @param bitmap : input bitmap file
     * @param folder : input folder name
     * @param name   : output file name
     */
    public static void saveBitmaptoJpeg(Bitmap bitmap, String folder, String name) {
        String ex_storage = Environment.getExternalStorageDirectory().getAbsolutePath();
        // Get Absolute Path in External Sdcard
        String foler_name = "/" + folder + "/";
        String file_name = name + ".jpg";
        String string_path = ex_storage + foler_name;

        File file_path;
        try {
            file_path = new File(string_path);
            if (!file_path.isDirectory()) {
                file_path.mkdirs();
            }
            FileOutputStream out = new FileOutputStream(string_path + file_name);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.close();

        } catch (FileNotFoundException exception) {
            Log.e("FileNotFoundException", exception.getMessage());
        } catch (IOException exception) {
            Log.e("IOException", exception.getMessage());
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // super.onActivityResult(requestCode, resultCode, data);
        //갤러리에서 사진 선택
        if (requestCode == REQ_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                Log.e("image gal", uri + "");
                //선택한 이미지에 얼굴이 있는지 판단
                //uri로 mat을 가져오기
                try {
                    Bitmap image = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);

                    Mat matInput = new Mat();

                    org.opencv.android.Utils.bitmapToMat(image, matInput);
                    Mat matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());

                    //  Core.flip(matInput, matInput, 1);//영상을 반전시키는 메소드

                    Boolean found = detect(cascadeClassifier_face, cascadeClassifier_eye, matInput.getNativeObjAddr(), matResult.getNativeObjAddr());

                    if (found) {

                        image = Bitmap.createBitmap(matResult.width(), matResult.height(), Bitmap.Config.ARGB_8888);
                        org.opencv.android.Utils.matToBitmap(matResult, image);

                        long curr = System.currentTimeMillis();
                        saveBitmaptoJpeg(image, "babtalk", "" + curr);
                        final String filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/babtalk/" + curr + ".jpg";
                        //1. 비트맵을 파일로 저장하고.
                        //2. 서버에 업로드
                        //3. 업로드오케이되면, 프로필이미지에 세팅!

                        OkHttpRequest request = new OkHttpRequest();

                        request.imageUpload(new File(filepath), SaveSharedPreference.getUserid(getContext()), new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                Log.d("MyPageFragment", "request failed");
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                String responseStr = response.body().string();
                                Log.d("onResponse", responseStr);
                                String serverPath = "";

                                try {
                                    JSONObject object = new JSONObject(responseStr);
                                    serverPath = object.getString("file_url");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                final String finalServerPath = serverPath;

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {

                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                //성공했으면
                                                Log.d("serverpath", finalServerPath);
                                                Glide.with(getContext())
                                                        .load(finalServerPath)
                                                        .skipMemoryCache(true) //메모리캐싱끄기
                                                        .diskCacheStrategy(DiskCacheStrategy.NONE) //디스트 캐싱하지 않는다
                                                        .bitmapTransform(new CropCircleTransformation(getContext()))
                                                        .into(profileImage);
                                                //SaveSharedPreference.setUserimage(getContext(), filepath);
                                            }
                                        });

                                    }
                                }).start();

                            }
                        });

                    } else {
                        Toast.makeText(getContext(), "얼굴이 있는 사진만 프로필로 사용가능합니다", Toast.LENGTH_SHORT).show();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.mypagetoolbar, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.logout:
                //자체로그아웃
                SaveSharedPreference.clearUserInfo(getContext());

                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                return true;

            case R.id.update:
                Toast.makeText(getContext(), "회원정보수정으로 이동", Toast.LENGTH_SHORT).show();
                return true;

        }
        return false;
    }

}
