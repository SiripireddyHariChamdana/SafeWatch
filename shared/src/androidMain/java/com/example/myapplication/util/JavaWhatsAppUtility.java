package com.example.myapplication.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import java.net.URLEncoder;

/**
 * Backend WhatsApp Utility (Java)
 * Handles WhatsApp message dispatch.
 */
public class JavaWhatsAppUtility {

    public static void openWhatsApp(Context context, String phone, String message) {
        try {
            String url = "https://api.whatsapp.com/send?phone=" + phone + "&text=" + URLEncoder.encode(message, "UTF-8");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
