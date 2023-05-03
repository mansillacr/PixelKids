package com.example.pixelkids;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class CamaraActivity extends CameraActivity{

    private static final String etiqueta = "MainActivity";

    private CascadeClassifier cascadeClassifier;
    private Mat mRgb, cambiar_rgb;
    private Mat mGris, cambiar_gris;
    private MatOfRect rects;
    private CameraBridgeViewBase camara;
    ImageView imageView;
    private Button btnRotar, btnHacerFoto;
    private boolean camaraFrontal = false;
    private BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.d("OpenCV", "OpenCV loaded successfully");
                    mRgb = new Mat();
                    mGris = new Mat();
                    rects = new MatOfRect();

                    LeerFicheroFrontalFace();

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camara);

        camara = findViewById(R.id.camara);
        btnRotar = findViewById(R.id.btn);
        btnHacerFoto = findViewById(R.id.btnHacerFoto);

        camara.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
            }

            @Override
            public void onCameraViewStopped() {
                mRgb.release();
                mGris.release();
                rects.release();
            }

            //Procesa la cara de entrada y devuelve lo procesado frame a frame
            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                mRgb = inputFrame.rgba();
                mGris = inputFrame.gray();

                if(camaraFrontal){
                    Core.flip(mRgb, mRgb,1);
                }

                //Cambiar posicion
                cambiar_rgb = mRgb.t();
                cambiar_gris = mGris.t();

                int height = (int)(cambiar_rgb.height()*0.1);

                //Detector de caras
                cascadeClassifier.detectMultiScale(cambiar_gris, rects, 1.1, 3,
                        0, new Size(height,height), new Size());

                //Pixelar cara
                for(Rect face : rects.toList()){
                    Mat submat = cambiar_rgb.submat(face);
                    Imgproc.blur(submat, submat,new Size(50,50));
                    Imgproc.rectangle(cambiar_rgb, face, new Scalar(0,0,0,1), 0);
                }

                return cambiar_rgb.t();
            }
        });

        if(OpenCVLoader.initDebug()){
            camara.enableView();
            btnRotar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    camara.disableView();

                    if(camaraFrontal){
                        camara.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                        camaraFrontal = false;
                    }else{
                        camara.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                        camaraFrontal = true;
                    }

                    camara.enableView();
                }
            });

            btnHacerFoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });

            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        };
    }

    void LeerFicheroFrontalFace(){

        try {
            InputStream inputStream = getResources().openRawResource(R.raw.haarcascade_frontalface_default);
            File file = new File(getDir("cascade", MODE_PRIVATE), "haarcascade_frontalface_default");
            FileOutputStream fileOutputStream = new FileOutputStream(file);

            byte[] bytes = new byte[4096];
            int leer_bytes;

            while((leer_bytes = inputStream.read(bytes)) != -1){
                fileOutputStream.write(bytes,0,leer_bytes);
            }

            cascadeClassifier = new CascadeClassifier(file.getAbsolutePath());
            if(cascadeClassifier.empty()) cascadeClassifier = null;

            //cerrar flujos
            inputStream.close();
            fileOutputStream.close();
            file.delete();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(camara);
    }

    @Override
    protected void onResume() {
        super.onResume();
        camara.enableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        camara.disableView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        camara.disableView();
    }
}