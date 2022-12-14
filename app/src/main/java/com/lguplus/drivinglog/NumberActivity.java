package com.lguplus.drivinglog;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class NumberActivity extends AppCompatActivity {

    static String TAG = "===== Driving Log =====";

    LinearLayout linearLayout;
    EditText inputEt;
    TextView carNumTv;
    View cameraLayout;
    View ocrLayout;
    Button nextBtn;
    Button carsStatsBtn;
    Spinner spinner;
    ProgressDialog dialog;
    private String carNum;
    private String id;

    // OCR ?????? ??????
    InputImage image;           // ML ????????? ????????? ?????? ?????????
    TextRecognizer recognizer;  // ????????? ????????? ????????? ??????
    ImageView imageView;        // ???????????? ?????? ???????????? ????????? ???
    Bitmap bitmap;              // ???????????? ?????????
    TextView notLoadFromServerTv; // ??????????????? ????????? ???????????? ????????????????\n????????? ?????? ????????? ?????????.
    Button cameraBtn;           // ?????? ?????? ??????
    Button ocrBtn;              // ????????? ?????? ??????
    private String imageFilePath; //????????? ?????? ??????
    private Uri uri;
    static final int REQUEST_IMAGE_CAPTURE = 672;

    // ????????? ?????? ??????
    ArrayList<String> itemList = new ArrayList<String>();
    private boolean userInputFlag = false;

    // ?????? ?????? ?????? ??????
    private String SERVER_URL = "http://124.49.91.86:8002/cars";
    static RequestQueue requestQueue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_number);

        init();
        makeRequest();      // ??????????????? ??????????????? ????????? itemList??? ?????? ??????


        // ?????? ?????? ??? ????????? ??????
        linearLayout.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(inputEt.getWindowToken(), 0);
            }
        });

    }

    private void init() {
        linearLayout = findViewById(R.id.parentLinearLayout);
        inputEt = findViewById(R.id.inputEt);
        carNumTv = findViewById(R.id.carNumTv);
        nextBtn = findViewById(R.id.nextBtn);
        spinner = findViewById(R.id.spinner);
        carsStatsBtn = findViewById(R.id.carsStatsBtn);

        // OCR ?????? ?????????
        cameraLayout = findViewById(R.id.cameraLayout);
        ocrLayout = findViewById(R.id.ocrLayout);
        imageView = findViewById(R.id.imageView);
        recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
        cameraBtn = (Button)findViewById(R.id.cameraBtn);
        ocrBtn = (Button)findViewById(R.id.ocrBtn);
        notLoadFromServerTv = findViewById(R.id.notLoadFromServerTv);

        // ?????? ??????
        //inputEt.setVisibility(View.GONE);
        inputEt.setVisibility(View.INVISIBLE);
        notLoadFromServerTv.setVisibility(View.INVISIBLE);
        cameraLayout.setVisibility(View.INVISIBLE);
        ocrLayout.setVisibility(View.INVISIBLE);

        // RequestQueue ?????? ????????????
        if(requestQueue == null) {
            requestQueue = Volley.newRequestQueue(getApplicationContext());
        }

        // ????????? ???????????? ????????? id??? ???????????? ?????????
        Intent intent = getIntent();
        id = intent.getStringExtra("id");
    }

    private void SettingListener() {
        // ????????? ??????
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, itemList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d("===== SettingListener()", "onItemSelected() ??????");
                carNum = itemList.get(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("===== SettingListener()", "onNothingSelected() ??????");
            }
        });

        // '??????' ????????? ?????? ????????? ??????
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(userInputFlag) {
                    if(inputEt.getText().toString().trim().length() == 0){
                        Toast.makeText(getApplicationContext(), "??????????????? ??????????????????", Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        carNum = inputEt.getText().toString();    // ???????????? ???????????? editText. ????????? ????????? ?????????
                    }
                } else if(carNum == null) {
                    Toast.makeText(getApplicationContext(), "??????????????? ??????????????????", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("carNum", carNum);
                intent.putExtra("id", id);
                startActivity(intent);
            }
        });

        carsStatsBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                // ???????????? ????????? ???????????? ?????? ??????
                Intent intent = new Intent(getApplicationContext(), TotalCarsStatsActivity.class);
                startActivity(intent);

            }
        });

        // ?????? ?????? ??????
        cameraBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                sendTakePhotoIntent();
            }
        });

        // ????????? ?????? ??????
        ocrBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                // ???????????? ????????? bitmap?????? ??????
                BitmapDrawable d = (BitmapDrawable) imageView.getDrawable();
                bitmap = d.getBitmap();
                if(bitmap == null){
                    Toast.makeText(getApplicationContext(), "?????? ????????? ???????????????", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    TextRecognition(recognizer);
                }
            }
        });
    }


    // ????????? ????????? ???????????? ??????
    public void makeRequest() {
        String url = SERVER_URL;

        dialog = new ProgressDialog(NumberActivity.this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("???????????? ?????????...");
        dialog.show();

        StringRequest request = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("===== makeRequest()", "???????????? : " + response);
                        // ?????????????????? ??????
                        if(dialog != null){
                            dialog.dismiss();
                        }
                        processResponse(response);  // ???????????? ?????? json ????????? ???????????? itemList??? ??? ??????
                        SettingListener();          // ????????? ??? ?????? ?????? ??????
                    }
                },
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("===== makeRequest()", "???????????? : " + error.getMessage());
                        // ?????????????????? ??????
                        if(dialog != null){
                            dialog.dismiss();
                        }
                        Toast.makeText(getApplicationContext(), "??????????????? ?????? ??????????????????", Toast.LENGTH_SHORT).show();
                        userInputFlag = true;

                        inputEt.setVisibility(View.VISIBLE);
                        notLoadFromServerTv.setVisibility(View.VISIBLE);
                        cameraLayout.setVisibility(View.VISIBLE);
                        carNumTv.setVisibility(View.INVISIBLE);
                        spinner.setVisibility(View.INVISIBLE);

                        SettingListener();          // ????????? ??? ?????? ?????? ??????
                    }
                }){
            protected Map<String, String> getParams() throws AuthFailureError{
                Map<String, String> params = new HashMap<String, String>();

                return params;
            }
        };
        request.setShouldCache(false);
        requestQueue.add(request);
    }

    public void processResponse(String response){
        try{
            JSONArray jsonArray = new JSONArray(response);
            for(int i=0; i<jsonArray.length(); i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String carNumFromJson = jsonObject.getString("CAR_NUM");
                itemList.add(carNumFromJson);
            }

        } catch(JSONException e){
            e.printStackTrace();
        }
    }

    private void sendTakePhotoIntent(){

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }

            if (photoFile != null) {
                uri = FileProvider.getUriForFile(this, getPackageName(), photoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
        ocrLayout.setVisibility(View.INVISIBLE);
        ocrLayout.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            imageView.setImageURI(uri);
            // ExifInterface exif = null;

            try{
                InputStream in = getContentResolver().openInputStream(uri);
                bitmap = BitmapFactory.decodeStream(in);
                image = InputImage.fromBitmap(bitmap, 0);
                Log.d(TAG,"????????? -> ????????? ??????");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            /*
            try {
                exif = new ExifInterface(imageFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            int exifOrientation;
            int exifDegree;

            if (exif != null) {
                exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                exifDegree = exifOrientationToDegrees(exifOrientation);
            } else {
                exifDegree = 0;
            }


            ((ImageView)findViewById(R.id.imageView)).setImageBitmap(rotate(bitmap, exifDegree));
            */
        }

    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,      /* prefix */
                ".jpg",         /* suffix */
                storageDir          /* directory */
        );
        imageFilePath = image.getAbsolutePath();
        return image;
    }

    private void TextRecognition(TextRecognizer recognizer){
        Task<Text> result = recognizer.process(image)
                // ????????? ?????? ????????? ???????????? ?????????
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text text) {
                        Log.d(TAG, "????????? ?????? ??????!");
                        String resultText = text.getText();
                        inputEt.setText(resultText);
                        Log.d(TAG, "resultText : " + resultText);

                        ocrLayout.setVisibility(View.VISIBLE);
                        ocrLayout.setVisibility(View.INVISIBLE);
                    }
                })
                // ????????? ?????? ????????? ???????????? ?????????
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "????????? ?????? ??????....." + e.getMessage());

                        ocrLayout.setVisibility(View.VISIBLE);
                        ocrLayout.setVisibility(View.INVISIBLE);
                    }
                });
    }

}