package com.example.pixelkids;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.Collections;
import java.util.List;

public class MainActivity extends CameraActivity {

    private static final int PIDO_PERMISOS_CAMARA = 100 ;
    private CameraBridgeViewBase camara;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camara = findViewById(R.id.javaCameraView);

        camara.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {

            }

            @Override
            public void onCameraViewStopped() {

            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                return inputFrame.rgba();
            }
        });

        if(permisosCamara()){
            if(OpenCVLoader.initDebug()){
                camara.enableView();
            };
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

    //Confirmación de los pedidos de la Camara
    boolean permisosCamara(){

        if(ContextCompat.checkSelfPermission(this, "android.permission.CAMERA")
                == PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "ADIOS", Toast.LENGTH_SHORT).show();
            return true;
        }else{
            Toast.makeText(this, "HOLA", Toast.LENGTH_SHORT).show();
            AvisoPermisos();

            if(ContextCompat.checkSelfPermission(this, "android.permission.CAMERA")
                    == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "ADIOS", Toast.LENGTH_SHORT).show();
                return true;
            }else{
                return false;
            }
        }
    }

    //Dialogo de aviso al usuario para los permisos
    void AvisoPermisos(){
        AlertDialog AD;

        AlertDialog.Builder ADBuilder = new AlertDialog.Builder(MainActivity.this);
        ADBuilder.setMessage("Para poder utilizar esta App necesitas permisos de la cámara." +
                "Sino los aceptas no podrás utilizar la funcionalidad de PixelKids");

        ADBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Solicitamos permisos
                ActivityCompat.requestPermissions(
                        MainActivity.this,
                        new String[]{"android.permission.CAMERA"},
                        PIDO_PERMISOS_CAMARA);
            }
        });

        AD = ADBuilder.create();
        AD.show();
    }

}

