package face.zhen.com.imooc_how_old;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.facepp.error.FaceppParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private ImageView mPhoto;
    private Button mDectect;
    private Button mGetImg;
    private TextView mTip;
    private View mWatting;

    private String mCurrentPhote;
    private int PICC_COIDE = 0X33;
    private Bitmap mPhoteImg;
    private Paint mPaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        initEvent();
        mPaint = new Paint();
    }

    private void initEvent() {
        mGetImg.setOnClickListener(this);
        mDectect.setOnClickListener(this);

    }

    private void initViews() {
        mPhoto = (ImageView) findViewById(R.id.id_pohoto);
        mDectect = (Button) findViewById(R.id.id_dectet);
        mGetImg = (Button) findViewById(R.id.id_getimage);
        mTip = (TextView) findViewById(R.id.id_tip);
        mWatting = findViewById(R.id.id_waitting);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICC_COIDE){
            if (data != null){
                Uri uri = data.getData();
                Cursor cursor = getContentResolver().query(uri,null,null,null,null);
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                mCurrentPhote =  cursor.getString(idx);
                cursor.close();

                resizePhote();
                mPhoto.setImageBitmap(mPhoteImg);
                mTip.setText("Click Detect ==>");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void resizePhote() {
        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhote,options);
        //这里要*1.0F/1024 不要搞错写成*0.1d/1024
        double ratio = Math.max(options.outWidth*1.0d/1024f,options.outHeight*1.0d/1024f);
        options.inSampleSize = (int) Math.ceil(ratio);
        options.inJustDecodeBounds = false;
        mPhoteImg = BitmapFactory.decodeFile(mCurrentPhote,options);

    }

    private static final int SUCCEE_MSG = 1;
    private static final int ERROR_MSG = 2;

    private Handler mHandler;

    {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case SUCCEE_MSG:
                        mWatting.setVisibility(View.GONE);
                        JSONObject jsonObject = (JSONObject) msg.obj;
                        //解析json
                        prepareBitmap(jsonObject);
                        mPhoto.setImageBitmap(mPhoteImg);
                        break;
                    case ERROR_MSG:
                        mWatting.setVisibility(View.GONE);
                        String e = (String) msg.obj;
                        if(TextUtils.isEmpty(e)){
                            mTip.setText("Error");
                        }else{
                            mTip.setText(e);
                        }
                        break;
                }
                super.handleMessage(msg);
            }
        };
    }

    private void prepareBitmap(JSONObject rs) {
        Bitmap bitmap = Bitmap.createBitmap(mPhoteImg.getWidth(),mPhoteImg.getHeight(),mPhoteImg.getConfig());
        Canvas canvas = new Canvas(bitmap);
        //需要在解析前，将bitmap画上来。。
        canvas.drawBitmap(mPhoteImg,0,0,mPaint);
        try {
            JSONArray faces = rs.getJSONArray("face");
            int faceCount = faces.length();
            mTip.setText("find"+faceCount);
            if(faceCount >0)
            for (int i = 0;i <= faceCount; i++){
                //拿到单独的face对象
                JSONObject face = faces.getJSONObject(i);
                JSONObject posObj = face.getJSONObject("position");
                float x = (float) posObj.getJSONObject("center").getDouble("x");
                float y = (float) posObj.getJSONObject("center").getDouble("y");

                float w = (float) posObj.getDouble("width");
                float h = (float) posObj.getDouble("height");

                x = x /100 *bitmap.getWidth();
                y = y/100 *bitmap.getHeight();

                w = w /100 *bitmap.getWidth();
                h = h/100 *bitmap.getHeight();

                mPaint.setColor(0xffffffff);
                mPaint.setStrokeWidth(3);
                //画BOX
                canvas.drawLine(x-w/2,y-h/2,x-w/2,y+h/2,mPaint);
                canvas.drawLine(x-w/2,y-h/2,x+w/2,y-h/2,mPaint);
                canvas.drawLine(x+w/2,y-h/2,x+w/2,y+h/2,mPaint);
                canvas.drawLine(x-w/2,y+h/2,x+w/2,y+h/2,mPaint);

                //get age and gender
                int age = face.getJSONObject("attribute").getJSONObject("age").getInt("value");
                String gender = face.getJSONObject("attribute").getJSONObject("gender").getString("value");

                //年龄和性别的图片
                Bitmap ageBitmap = buidldAgeBitmap(age,"Male".equals(gender));
                int ageWidth = ageBitmap.getWidth();
                int ageHeight = ageBitmap.getHeight();
                if(ageBitmap.getWidth()<mPhoto.getWidth()&&ageBitmap.getHeight()<mPhoto.getHeight()){
                    float ratio = Math.max(bitmap.getWidth()*1.0f/mPhoto.getWidth(),bitmap.getHeight()*1.0f/mPhoto.getHeight());
                    ageBitmap = Bitmap.createScaledBitmap(ageBitmap,(int)(ageWidth*ratio),(int)(ageHeight*ratio),false);
                }
                canvas.drawBitmap(ageBitmap,x-ageBitmap.getWidth()/2,y-h/2-ageBitmap.getHeight(),null);
            }
            mPhoteImg = bitmap;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Bitmap buidldAgeBitmap(int age, boolean isMale) {
        TextView textView = (TextView) mWatting.findViewById(R.id.id_textView);
        textView.setText(age+"");
        if(isMale){
            textView.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.nam),null,null,null);
        }else {
            textView.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.nu),null,null,null);
        }
        textView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(textView.getDrawingCache());
        textView.destroyDrawingCache();
        return bitmap;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.id_getimage:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, PICC_COIDE);
                break;
            case R.id.id_dectet:
                mWatting.setVisibility(View.VISIBLE);
                if(mCurrentPhote != null&&!mCurrentPhote.trim().equals("")){
                    resizePhote();
                }else{
                    mPhoteImg = BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher);
                }
                FaceppDectect.dectect(mPhoteImg,new FaceppDectect.CallBack() {
                    @Override
                    public void success(JSONObject result) {
                        Message msg = Message.obtain();
                        msg.what = SUCCEE_MSG;
                        msg.obj = result;
                        mHandler.sendMessage(msg);
                    }

                    @Override
                    public void errror(FaceppParseException exception) {
                        Message msg = Message.obtain();
                        msg.what = ERROR_MSG;
                        msg.obj = exception;
                        mHandler.sendMessage(msg);
                    }
                });
                break;
        }
    }
}
