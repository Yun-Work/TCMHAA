package com.example.tcmhaa.api;

import com.google.gson.Gson;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApiHelper {

    public interface ApiCallback<R> {
        void onSuccess(R response);
        void onFailure(Throwable t);
    }


    //API路徑, request內容
    public static <T, R> void httpPost(
            String apiPath,
            T requestModel,
            Class<R> responseClass,
            ApiCallback<R> callback
    ) {
        GenericApiService service = ApiClient.getInstance().create(GenericApiService.class);

        Call<ResponseBody> call = service.postData(apiPath, requestModel);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        Gson gson = new Gson();
                        R parsed = gson.fromJson(json, responseClass);
                        callback.onSuccess(parsed);
                    } catch (Exception e) {
                        callback.onFailure(e);
                    }
                } else {
                    callback.onFailure(new Exception("API Error: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onFailure(t);
            }
        });
    }
}
