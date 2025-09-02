package com.example.tcmhaa;

public final class nav {
    public static final String EXTRA_NEXT = "NEXT";          // Warning 後要去哪
    public static final String NEXT_TO_PHOTO = "TO_PHOTO";   // 去 PhotoActivity (選相簿)
    public static final String NEXT_TO_CAMERA = "TO_CAMERA"; // 去 CameraActivity (拍照)

    // 可選：跨頁傳圖用 key（如果需要）
    public static final String EXTRA_IMG_URI = "IMG_URI";
    public static final String EXTRA_IMAGE_BASE64 = "IMAGE_BASE64";

    private nav() {}
}
