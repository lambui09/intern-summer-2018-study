package asiantech.internship.summer.asynctack_thread_handler;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageDownloaderHandler extends Handler {

    ImageDownloaderHandler(){

    }

    public void handleMessage(Message message) {
        try {
            URL url = new URL((String) message.obj);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return;
            }
            int fileLength = connection.getContentLength();
            InputStream inputStream = connection.getInputStream();
            byte[] buffer = new byte[fileLength];
            int total = 0;
            while (total < fileLength) {
                int count = inputStream.read(buffer, total, fileLength - total);
                if (count < 1) break;
                total += count;
                mListener.updateProcess(total * 100 / fileLength);
            }
            Bitmap bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
            mListener.updateImage(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {

    }
}
