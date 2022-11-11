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

    // OCR 관련 변수
    InputImage image;           // ML 모델이 인식할 인풋 이미지
    TextRecognizer recognizer;  // 텍스트 인식에 사용될 모델
    ImageView imageView;        // 카메라로 찍은 이미지를 보여줄 뷰
    Bitmap bitmap;              // 사용되는 이미지
    TextView notLoadFromServerTv; // 서버로부터 번호를 가져오지 못하셨나요?\n번호를 직접 촬영해 주세요.
    Button cameraBtn;           // 사진 찍는 버튼
    Button ocrBtn;              // 텍스트 추출 버튼
    private String imageFilePath; //이미지 파일 경로
    private Uri uri;
    static final int REQUEST_IMAGE_CAPTURE = 672;

    // 스피너 관련 변수
    ArrayList<String> itemList = new ArrayList<String>();
    private boolean userInputFlag = false;

    // 서버 통신 관련 변수
    private String SERVER_URL = "http://124.49.91.86:8002/cars";
    static RequestQueue requestQueue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_number);

        init();
        makeRequest();      // 서버로부터 차량정보를 받아와 itemList에 넣는 함수


        // 배경 클릭 시 키보드 숨김
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

        // OCR 관련 초기화
        cameraLayout = findViewById(R.id.cameraLayout);
        ocrLayout = findViewById(R.id.ocrLayout);
        imageView = findViewById(R.id.imageView);
        recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
        cameraBtn = (Button)findViewById(R.id.cameraBtn);
        ocrBtn = (Button)findViewById(R.id.ocrBtn);
        notLoadFromServerTv = findViewById(R.id.notLoadFromServerTv);

        // 최초 숨김
        //inputEt.setVisibility(View.GONE);
        inputEt.setVisibility(View.INVISIBLE);
        notLoadFromServerTv.setVisibility(View.INVISIBLE);
        cameraLayout.setVisibility(View.INVISIBLE);
        ocrLayout.setVisibility(View.INVISIBLE);

        // RequestQueue 객체 생성하기
        if(requestQueue == null) {
            requestQueue = Volley.newRequestQueue(getApplicationContext());
        }

        // 로그인 화면에서 입력한 id를 받아오는 인텐트
        Intent intent = getIntent();
        id = intent.getStringExtra("id");
    }

    private void SettingListener() {
        // 스피너 셋팅
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, itemList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d("===== SettingListener()", "onItemSelected() 실행");
                carNum = itemList.get(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("===== SettingListener()", "onNothingSelected() 실행");
            }
        });

        // '다음' 버튼에 클릭 이벤트 적용
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(userInputFlag) {
                    if(inputEt.getText().toString().trim().length() == 0){
                        Toast.makeText(getApplicationContext(), "차량번호를 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        carNum = inputEt.getText().toString();    // 차량번호 입력받는 editText. 스피너 이용시 불필요
                    }
                } else if(carNum == null) {
                    Toast.makeText(getApplicationContext(), "차량번호를 입력해주세요", Toast.LENGTH_SHORT).show();
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
                // 차량현황 페이지 전환하는 부분 구현
                Intent intent = new Intent(getApplicationContext(), TotalCarsStatsActivity.class);
                startActivity(intent);

            }
        });

        // 사진 찍는 버튼
        cameraBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                sendTakePhotoIntent();
            }
        });

        // 텍스트 추출 버튼
        ocrBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                // 가져와진 사진을 bitmap으로 추출
                BitmapDrawable d = (BitmapDrawable) imageView.getDrawable();
                bitmap = d.getBitmap();
                if(bitmap == null){
                    Toast.makeText(getApplicationContext(), "촬영 버튼을 눌러주세요", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    TextRecognition(recognizer);
                }
            }
        });
    }


    // 서버로 데이터 전송하는 부분
    public void makeRequest() {
        String url = SERVER_URL;

        dialog = new ProgressDialog(NumberActivity.this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("차량정보 받는중...");
        dialog.show();

        StringRequest request = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("===== makeRequest()", "서버응답 : " + response);
                        // 프로그레스바 종료
                        if(dialog != null){
                            dialog.dismiss();
                        }
                        processResponse(response);  // 응답으로 받은 json 데이터 파싱하여 itemList에 값 넣기
                        SettingListener();          // 스피너 및 다음 버튼 구현
                    }
                },
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("===== makeRequest()", "통신에러 : " + error.getMessage());
                        // 프로그레스바 종료
                        if(dialog != null){
                            dialog.dismiss();
                        }
                        Toast.makeText(getApplicationContext(), "차량번호를 직접 입력해주세요", Toast.LENGTH_SHORT).show();
                        userInputFlag = true;

                        inputEt.setVisibility(View.VISIBLE);
                        notLoadFromServerTv.setVisibility(View.VISIBLE);
                        cameraLayout.setVisibility(View.VISIBLE);
                        carNumTv.setVisibility(View.INVISIBLE);
                        spinner.setVisibility(View.INVISIBLE);

                        SettingListener();          // 스피너 및 다음 버튼 구현
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
                Log.d(TAG,"이미지 -> 비트맵 변경");
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
                // 이미지 인식 성공시 실행되는 리스너
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text text) {
                        Log.d(TAG, "텍스트 인식 성공!");
                        String resultText = text.getText();
                        inputEt.setText(resultText);
                        Log.d(TAG, "resultText : " + resultText);

                        ocrLayout.setVisibility(View.VISIBLE);
                        ocrLayout.setVisibility(View.INVISIBLE);
                    }
                })
                // 이미지 인식 실패시 실행되는 리스너
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "텍스트 인식 실패....." + e.getMessage());

                        ocrLayout.setVisibility(View.VISIBLE);
                        ocrLayout.setVisibility(View.INVISIBLE);
                    }
                });
    }

}