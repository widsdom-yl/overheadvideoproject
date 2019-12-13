package dczh.overheadvideoproject;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.nuptboyzhb.lib.SuperSwipeRefreshLayout;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cafe.adriel.androidaudiorecorder.AndroidAudioRecorder;
import cafe.adriel.androidaudiorecorder.model.AudioChannel;
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate;
import cafe.adriel.androidaudiorecorder.model.AudioSource;
import dczh.Adapter.AlarmImageAdapter;
import dczh.Adapter.BaseAdapter;
import dczh.Bean.DevBean;
import dczh.Bean.ImageBean;
import dczh.Bean.ResponseModel;
import dczh.Bean.UploadFileRetModel;
import dczh.Util.Config;
import dczh.Util.FileUtil;
import dczh.Util.GsonUtil;
import dczh.View.LoadingDialog;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
/*通道设备图片列表*/
public class MainActivity extends AppCompatActivity implements BaseAdapter.OnItemClickListener, View.OnClickListener {
    RecyclerView imageRecyclerView;
    AlarmImageAdapter adapter;
    DevBean dev;
    List<ImageBean> imageList = null;
    Button button_capture;
    Button button_speech;
    SuperSwipeRefreshLayout swipeRefreshLayout;
    int page = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setCustomTitle(getString(R.string.string_cable_video),false);


        Bundle bundle = this.getIntent().getExtras();
        if(bundle != null){
            dev = (DevBean) bundle.getSerializable("param");
            getSupportActionBar().setTitle(dev.getNme());
        }
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_main);


        imageRecyclerView = findViewById(R.id.imageListView);

        imageRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        imageRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        button_capture = findViewById(R.id.button_capture);
        button_speech = findViewById(R.id.button_speech);
        button_capture.setOnClickListener(this);
        button_speech.setOnClickListener(this);
        requestImage(false);

        swipeRefreshLayout = (SuperSwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        //swipeRefreshLayout.setHeaderView(createHeaderView());// add headerView
        swipeRefreshLayout.setFooterView(createFooterView());
        swipeRefreshLayout
                .setOnPullRefreshListener(new SuperSwipeRefreshLayout.OnPullRefreshListener() {

                    @Override
                    public void onRefresh() {
                        //TODO 开始刷新
                        requestImage(false);
                    }

                    @Override
                    public void onPullDistance(int distance) {
                        //TODO 下拉距离
                    }

                    @Override
                    public void onPullEnable(boolean enable) {
                        //TODO 下拉过程中，下拉的距离是否足够出发刷新
                    }
                });

        swipeRefreshLayout
                .setOnPushLoadMoreListener(new SuperSwipeRefreshLayout.OnPushLoadMoreListener() {


                    @Override
                    public void onLoadMore() {
                        //TODO 开始刷新
                        requestImage(true);
                    }

                    @Override
                    public void onPushDistance(int i) {
                        //TODO 上拉拉距离
                    }

                    @Override
                    public void onPushEnable(boolean b) {
                        //TODO 上拉过程中，上拉的距离是否足够出发刷新
                    }
                });







    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_refresh, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                this.finish(); // back button
                return true;
            case R.id.action_refresh:
                requestImage(false);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void requestImage(final Boolean loadMore) {
        if (imageList == null){
            imageList = new ArrayList<>();
        }
        if (loadMore){
            ++page;
        }
        else{
            page = 1;
        }
        if (lod == null)
        {
            lod = new LoadingDialog(this);
        }
        lod.dialogShow();


        OkHttpClient client = new OkHttpClient();




        final Request request = new Request.Builder()
                .url(Config.serverUrl+"pic_list3.php?dev="+dev.getDev()+"&page="+page)
                .get()
                .build();

        Call call = client.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                        swipeRefreshLayout.setLoadMore(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String res = response.body().string();
                Log.e(tag,"res is "+res);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //
                        swipeRefreshLayout.setRefreshing(false);
                        swipeRefreshLayout.setLoadMore(false);
                        lod.dismiss();
                        {
                            // String body = new Gson().toJson(res);
                            List<ImageBean> lists = GsonUtil.parseJsonArrayWithGson(res, ImageBean[].class);
                            Log.e(tag,"list count is :"+lists.size());
                            if (loadMore){
                                imageList.addAll(lists);
                            }
                            else{
                                imageList.clear();
                                imageList.addAll(lists);

                            }
                            if(lists.size()>0){
                                if(adapter == null){
                                    adapter = new AlarmImageAdapter(imageList);
                                    imageRecyclerView.setAdapter(adapter);
                                    adapter.setOnItemClickListener(MainActivity.this);
                                }
                                else{
                                    adapter.resetMList(imageList);
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }

                    }
                });
            }
        });




    }




    @Override
    public void onItemClick(View view, int position) {
        Intent intent = new Intent(this,MediaDetailActivity.class);
        Bundle bundle = new Bundle();
        if (imageList.size()>0){
            bundle.putString("param1",imageList.get(position).getUrl());
        }
        intent.putExtras(bundle);
        startActivity(intent);

    }

    void recordWav(){
        FileUtil.makeDir(FileUtil.getSDRootPath() +"/record");
        String filePath = FileUtil.getSDRootPath() + "/record/recorded.wav";

        int color = getResources().getColor(R.color.appGreenColor);
        int requestCode = 0;
        AndroidAudioRecorder.with(this)
                // Required
                .setFilePath(filePath)
                .setColor(color)
                .setRequestCode(requestCode)

                // Optional
                .setSource(AudioSource.MIC)
                .setChannel(AudioChannel.MONO)
                .setSampleRate(AudioSampleRate.HZ_8000)
                .setAutoStart(true)
                .setKeepDisplayOn(true)

                // Start recording
                .record();
    }

    /*上传文件*/
    public void uploadWavData(String filepath){


        if (lod == null)
        {
            lod = new LoadingDialog(this);
        }
        lod.dialogShow();


        OkHttpClient client = new OkHttpClient();


        MultipartBody.Builder multiBuilder=new MultipartBody.Builder();


        MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");


        // 设置请求体
        multiBuilder.setType(MultipartBody.FORM);
//这里是 封装上传图片参数

//        for (int i = 0;i<compressFiles.size();++i){
//            File file=new File(compressFiles.get(i));
//            RequestBody filebody = MultipartBody.create(MEDIA_TYPE_PNG, file);
//            multiBuilder.addFormDataPart("file"+(i+1), file.getName(), filebody);
//        }

        File file=new File(filepath);
        RequestBody filebody = MultipartBody.create(MEDIA_TYPE_PNG, file);
        multiBuilder.addFormDataPart("file", file.getName(), filebody);



        // 封装请求参数,这里最重要
        HashMap<String, String> params = new HashMap<>();

//        params.put("token",AccountManager.getInstance().getToken());
        params.put("ext","wav");

        //参数以添加header方式将参数封装，否则上传参数为空
        if (params != null && !params.isEmpty()) {
            for (String key : params.keySet()) {
                multiBuilder.addPart(
                        Headers.of("Content-Disposition", "form-data; name=\"" + key + "\""),
                        RequestBody.create(null, params.get(key)));
            }
        }




        RequestBody multiBody=multiBuilder.build();


        final Request request = new Request.Builder()
                .url(Config.serverUrl+"upload.php")
                .post(multiBody)
                .build();

        Call call = client.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                lod.dismiss();
                Toast.makeText(MainActivity.this, getString(R.string.upload_failed), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String res = response.body().string();
                Log.e(tag,"upload res is "+res);
                final ResponseModel model  = GsonUtil.parseJsonWithGson(res,ResponseModel.class);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //
                        lod.dismiss();
                        if (model != null && model.error_code==0){
                            String body = new Gson().toJson(model.data);
                            UploadFileRetModel model = GsonUtil.parseJsonWithGson(body, UploadFileRetModel.class);
                            Toast.makeText(MainActivity.this, getString(R.string.upload_success), Toast.LENGTH_LONG).show();
                            mqttCommand(model.getUrl(),"/a1ZoMm31X8K/"+dev.getDev()+"/user/get");
//

                        }
                        else{
                            Toast.makeText(MainActivity.this, getString(R.string.upload_failed), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                // Great! User has recorded and saved the audio file
                String filePath = FileUtil.getSDRootPath() + "/record/recorded.wav";
                uploadWavData(filePath);

            } else if (resultCode == RESULT_CANCELED) {
                // Oops! User has canceled the recording
            }
        }
    }

    //http://gz.aliyuns.vip/svr/aii/iot_msg.php?topic=/a1ZoMm31X8K/BRSBJ000000000001/user/get&param=http://39.100.246.127:8080/app/demo.wav


    public void mqttCommand(String wavUrl,String topic) {

        if (lod == null)
        {
            lod = new LoadingDialog(this);
        }
        lod.dialogShow();


        OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder()
                .url(Config.serverUrl+"iot_msg.php?topic="+topic+"&param="+wavUrl)
                .get()
                .build();

        Call call = client.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String res = response.body().string();
                Log.e(tag,"res is "+res);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //
                        lod.dismiss();
                        {
                            if (res.contains("bool(true)")){
                                Toast.makeText(MainActivity.this,getString(R.string.string_mqtt_success),Toast.LENGTH_SHORT).show();
                            }
                            else{

                            }
                        }

                    }
                });
            }
        });

    }


    public void mqttCapCommand(String topic) {

        if (lod == null)
        {
            lod = new LoadingDialog(this);
        }
        lod.dialogShow();


        OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder()
                .url(Config.serverUrl+"iot_cap.php?topic="+topic)
                .get()
                .build();

        Call call = client.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,getString(R.string.string_capture_failed),Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String res = response.body().string();
                Log.e(tag,"res is "+res);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //
                        lod.dismiss();
                        {
                            if (res.contains("bool(true)")){
                                Toast.makeText(MainActivity.this,getString(R.string.string_capture_success),Toast.LENGTH_SHORT).show();
                            }
                            else{
                                Toast.makeText(MainActivity.this,getString(R.string.string_capture_failed),Toast.LENGTH_SHORT).show();
                            }
                        }

                    }
                });
            }
        });

    }

    @Override
    public void onLongClick(View view, int position) {

    }
    final static String tag = "MainActivity";
    LoadingDialog lod;

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.button_capture:
                mqttCapCommand("/a1ZoMm31X8K/"+dev.getDev()+"/user/get");
                break;
            case R.id.button_speech:
                recordWav();
                break;
            default:
                break;
        }
    }

    View createFooterView()
    {
        View view = LayoutInflater.from(this).inflate(R.layout.load_more, null);

        return view;
    }

}
