package com.example.pixelkids;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {

    private static final int PIDO_PERMISOS_CAMARA = 100 ;
    private static final int CODIGO_GALERIA = 101;
    private CascadeClassifier cascadeClassifier;
    private Mat rgb, cambiar_rgb;
    private MatOfRect rects;
    private Button btnGaleria, btnCamara;
    private Bitmap bitmapGaleria, bitmap;
    private ImageView imageView, imagePixelada;

    private ReconocimientoEdad reconocimientoEdad;

    //Llamada asyncrona de la propia libreria de OpenCV para poder declarar las variables
    //de la propia libreria
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.d("OpenCV", "OpenCV loaded successfully");
                    rgb = new Mat();
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
        setContentView(R.layout.activity_main);

        btnCamara = findViewById(R.id.btn);
        btnGaleria = findViewById(R.id.btnGaleria);
        imageView = findViewById(R.id.imageView);
        imagePixelada = findViewById(R.id.imagePixelada);

        btnGaleria.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, CODIGO_GALERIA);
                if(OpenCVLoader.initDebug()){
                    mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
                };
            }
        });

        btnCamara.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CamaraActivity.class));
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==CODIGO_GALERIA && data!= null){
            try {
                //CaffeModel caffeModel = new CaffeModel("deploy_agenet.prototxt", "age_net.caffemodel");

                bitmapGaleria = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());

                Utils.bitmapToMat(bitmapGaleria,rgb);

                //Cambiar posicion
                cambiar_rgb = rgb.t();

                int height = (int)(cambiar_rgb.height()*0.1);

                //Detector de caras
                cascadeClassifier.detectMultiScale(cambiar_rgb, rects, 1.1, 2,2 ,
                        new Size(height,height), new Size());

                //Pixelar cara
                for(Rect rect : rects.toList()){
                    Mat submat = cambiar_rgb.submat(rect);

                    //float[] pre = caffeModel.prediccion(submat);

                    Imgproc.blur(submat, submat,new Size(100,100));
                    Imgproc.rectangle(cambiar_rgb, rect, new Scalar(0,0,0,1), 0);
                }

                Utils.matToBitmap(cambiar_rgb.t(),bitmapGaleria);


                //TENSORFLOW MODEL.TFLITE

                int inputSize = 96;
                reconocimientoEdad = new ReconocimientoEdad(getAssets(), MainActivity.this, "model.tflite", inputSize);

                Utils.matToBitmap(reconocimientoEdad.reconocimientoImagen(rgb,getApplicationContext()),bitmapGaleria);



                imageView.setImageBitmap(bitmapGaleria);
                imagePixelada.setImageBitmap(bitmap);


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Leer fichero XML de frontalface.xml
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

