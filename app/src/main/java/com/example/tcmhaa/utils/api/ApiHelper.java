package com.example.tcmhaa.utils.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiHelper {

    public interface ApiCallback<T> {
        void onSuccess(T resp);
        void onFailure(Throwable t);
    }

    // Android 模擬器連本機：10.0.2.2
//    private static final String BASE_URL = "https://tcmha-python.duckdns.org/api/";
//    private static final String BASE_URL = "http://10.0.2.2:6060/api/";
    private static final String BASE_URL = "http:/163.13.202.117:6060/api/";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static Retrofit retrofit;
    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    private static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(log)
                    // （可選）逾時設定
                    //.connectTimeout(15, TimeUnit.SECONDS)
                    //.readTimeout(15, TimeUnit.SECONDS)
                    //.writeTimeout(15, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    public static <Req, Resp> void httpPost(
            String path,
            Req bodyObj,
            Class<Resp> respClass,
            ApiCallback<Resp> cb
    ) {
        GenericApiService service = getClient().create(GenericApiService.class);

        String json = gson.toJson(bodyObj);
        RequestBody rb = RequestBody.create(json.getBytes(StandardCharsets.UTF_8), JSON);

        service.postData(path, rb).enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (!response.isSuccessful()) {
                        // 從 errorBody 抽出乾淨的錯誤訊息（優先 error，其次 message；否則原始字串或預設 HTTP XXX）
                        String errMsg = "HTTP " + response.code();
                        try {
                            String raw = (response.errorBody() != null) ? response.errorBody().string() : null;
                            if (raw != null && !raw.isEmpty()) {
                                // 盡量用 JSON 解析掉被 escape 的中文字
                                try {
                                    org.json.JSONObject obj = new org.json.JSONObject(raw);
                                    if (obj.has("error") && !obj.isNull("error")) {
                                        errMsg = obj.getString("error");
                                    } else if (obj.has("message") && !obj.isNull("message")) {
                                        errMsg = obj.getString("message");
                                    } else {
                                        // 不是預期的 JSON 格式，就回傳原始字串
                                        errMsg = raw;
                                    }
                                } catch (Exception notJson) {
                                    // 不是 JSON，就原樣顯示（或可再做簡單清理）
                                    errMsg = raw;
                                }
                            }
                        } catch (Exception ignore) {
                            // 解析失敗就用預設 errMsg
                        }
                        cb.onFailure(new RuntimeException(errMsg));
                        return;
                    }

                    ResponseBody respBody = response.body();
                    if (respBody == null) {
                        cb.onFailure(new RuntimeException("Empty body"));
                        return;
                    }

                    String body = respBody.string();

                    // 呼叫方要字串的話，直接回傳字串
                    if (respClass == String.class) {
                        @SuppressWarnings("unchecked")
                        Resp cast = (Resp) body;
                        cb.onSuccess(cast);
                        return;
                    }

                    Resp obj = gson.fromJson(body, respClass);
                    cb.onSuccess(obj);
                } catch (Exception e) {
                    cb.onFailure(e);
                }
            }


            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                cb.onFailure(t);
            }
        });
    }
}
