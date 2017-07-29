//
// Created by cmina on 2017-07-28.
//

#include <jni.h>
#include "com_example_cmina_openmeeting_fragment_MyPageFragment.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/opencv.hpp>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <string>

using namespace cv;
using namespace std;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_com_example_cmina_openmeeting_fragment_MyPageFragment_loadCascade
  (JNIEnv *env, jclass type, jstring cascadeFileName_) {


          const char *nativeFileNameString = env->GetStringUTFChars(cascadeFileName_, 0);

                       string baseDir("/storage/emulated/0/");
                       baseDir.append(nativeFileNameString);
                       const char *pathDir = baseDir.c_str();

                       jlong ret = 0;
                       ret = (jlong) new CascadeClassifier(pathDir);
                       if (((CascadeClassifier *) ret)->empty()) {
                           __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                                               "CascadeClassifier로 로딩 실패  %s", nativeFileNameString);
                       }
                       else
                           __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                                               "CascadeClassifier로 로딩 성공 %s", nativeFileNameString);


                       env->ReleaseStringUTFChars(cascadeFileName_, nativeFileNameString);

                       return ret;

  }



          float resize(Mat img_src, Mat &img_resize, int resize_width){

              float scale = resize_width / (float)img_src.cols ;
              if (img_src.cols > resize_width) {
                  int new_height = cvRound(img_src.rows * scale);
                  resize(img_src, img_resize, Size(resize_width, new_height));
              }
              else {
                  img_resize = img_src;
              }
              return scale;
          }




 JNIEXPORT jboolean JNICALL Java_com_example_cmina_openmeeting_fragment_MyPageFragment_detect
   (JNIEnv *env, jclass type, jlong cascadeClassifier_face, jlong cascadeClassifier_eye, jlong matAddrInput, jlong matAddrResult) {

         Mat &img_input = *(Mat *) matAddrInput;
                    Mat &img_result = *(Mat *) matAddrResult;

                    img_result = img_input.clone(); //clone() , copyTo() 복사본 생성

                    std::vector<Rect> faces;
                    std::vector<Rect> eyes;
                    Mat img_gray;

                    cvtColor(img_input, img_gray, COLOR_BGR2GRAY);
                    equalizeHist(img_gray, img_gray);

                    Mat img_resize;
                    float resizeRatio = resize(img_gray, img_resize, 640);
                    //인식된 얼굴 중에 제일 큰것!
                    int bigface;

                    //-- Detect faces
                    ((CascadeClassifier *) cascadeClassifier_face)->detectMultiScale( img_resize, faces, 1.1, 2, 0|CASCADE_SCALE_IMAGE, Size(30, 30) );

                    __android_log_print(ANDROID_LOG_DEBUG, (char *) "native-lib :: ", (char *) "face %d found ", faces.size());

                    for (int i = 0; i < faces.size(); i++) {

                        double real_facesize_x = faces[i].x / resizeRatio;
                        double real_facesize_y = faces[i].y / resizeRatio;
                        double real_facesize_width = faces[i].width / resizeRatio;
                        double real_facesize_height = faces[i].height / resizeRatio;

                        if (faces.size() > 1) {
                            if (real_facesize_width < (faces[bigface].width / resizeRatio)) {
                            //현재의 너비가 전에 가장큰 너비보다 작으면
                                bigface = i-1;
                            } else {
                                bigface = i;
                            }
                        } else {
                            bigface = i;
                        }

                        Point center( real_facesize_x + real_facesize_width / 2, real_facesize_y + real_facesize_height/2);
                        //얼굴 동그라미 주석처리
                      //  ellipse(img_result, center, Size( real_facesize_width / 2, real_facesize_height / 2), 0, 0, 360,
                      //          Scalar(255, 0, 255), 30, 8, 0);

                        Rect face_area(real_facesize_x, real_facesize_y, real_facesize_width,real_facesize_height);
                        Mat faceROI = img_gray( face_area );

                        //-- In each face, detect eyes
                        ((CascadeClassifier *) cascadeClassifier_eye)->detectMultiScale( faceROI, eyes, 1.1, 2, 0 |CASCADE_SCALE_IMAGE, Size(30, 30) );

                      /*  for ( size_t j = 0; j < eyes.size(); j++ )
                        {
                            Point eye_center( real_facesize_x + eyes[j].x + eyes[j].width/2, real_facesize_y + eyes[j].y + eyes[j].height/2 );
                            //눈 동그라미 주석처리
                          //  int radius = cvRound( (eyes[j].width + eyes[j].height)*0.25 );
                           // circle( img_result, eye_center, radius, Scalar( 255, 0, 0 ), 30, 8, 0 );
                        }
                        */

                    }


                     // 얼굴이 인식되었다면, 해당 얼굴을 중심으로 crop해서 img_result에에 저장
                     if (faces.size()>0 && eyes.size()>1) {
                        double divider = 2.0;
                        double cropx = (faces[bigface].x / resizeRatio) / divider;
                        double cropy = (faces[bigface].y / resizeRatio) / divider;
                        //여기에서 오류...called object type 'int' is not a function of function pointer
                        //==> img_input.cols() 를 img_input.cols로 변경!!
                        double crop_width = ((faces[bigface].width / resizeRatio) + (float)img_input.cols) / divider ;
                        double crop_height = ((faces[bigface].height / resizeRatio) + (float)img_input.rows) / divider ;
                     /*
                        double cropa = (img_input.cols -  (faces[bigface].x / resizeRatio) - (faces[bigface].width / resizeRatio) ) / divider;
                        double cropb = (img_input.rows -  (faces[bigface].y / resizeRatio) - (faces[bigface].height / resizeRatio) ) / divider;

                        double len[] = {cropx, cropy, cropa, cropb};
                       // double max = len[0];
                        double min = len[0];

                        for (int i = 1; i < len.length; i++) {
                            //if( len[i] > max )
                             // max = len[i];

                            if (len[i] < min )
                              min = len[i];
                        }
        */
                     //   Rect crop_area((faces[bigface].x / resizeRatio) - min, (faces[bigface].y / resizeRatio) -min, (faces[bigface].width / resizeRatio) + min, (faces[bigface].height / resizeRatio)+ min);
                        Rect crop_area(cropx, cropy, crop_width, crop_height);

                    //    Rect crop_area((faces[bigface].x / resizeRatio) , (faces[bigface].y / resizeRatio),
                       //                 ((faces[bigface].width / resizeRatio) ),((faces[bigface].height / resizeRatio) ) ) ;
                        img_result = img_input(crop_area);

                        __android_log_print(ANDROID_LOG_DEBUG, (char *) "native-lib :: ", (char *) "face %d 크롭을 해야하는데 ", faces.size());

                        }

                    return ((faces.size()>0 && eyes.size()>1) ? true : false);


   }

}