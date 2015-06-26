package face.zhen.com.imooc_how_old;

import android.graphics.Bitmap;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * Created by Administrator on 2015/6/26.
 */
public class FaceppDectect {

    /**
     * dectect方法有返回值，不值一个，返回不同的返回值，进行相应的操作，
     * 定义一个接口，在成功或者失败的时候可以做一些事情
     */
    public  interface CallBack{
        void success(JSONObject result);
        void  errror(FaceppParseException exception);
    }
    public  static void dectect(final Bitmap bm, final CallBack callBack){
                if(null == bm){
                    return;
                }
        new Thread(new Runnable() {
            @Override
            public void run() {
                //request
                try {
                    HttpRequests requests = new HttpRequests(Constant.KEY,Constant.SECERT,true,true);

                    Bitmap bmSmall = Bitmap.createBitmap(bm,0,0,bm.getWidth(),bm.getHeight());
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmSmall.compress(Bitmap.CompressFormat.JPEG,100,stream);
                    byte[] arrays = stream.toByteArray();
                    PostParameters parameters = new PostParameters();
                    parameters.setImg(arrays);
                    JSONObject jsonObject = requests.detectionDetect(parameters);
                    Log.e("TAG",jsonObject.toString());
                    if(callBack !=null){
                        callBack.success(jsonObject);
                    }

                } catch (FaceppParseException e) {
                    e.printStackTrace();
                      if(callBack !=null){
                          callBack.errror(e);
                }     }
            }
        }).start();
    }
}
