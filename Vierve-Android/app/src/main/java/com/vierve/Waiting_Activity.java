package com.vierve;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

public class Waiting_Activity extends AppCompatActivity {

    //var for the socket
    private Socket mSocket;
    private SocketHandler socketHandler;

    //values retrieved from the previous activities
    private String type;
    private String selected_college_id, selected_parkinglot_id;
    double pu_lat,pu_lng;

    private DB_Helper db_helper;



    private JSONObject user;

    boolean submitRequest;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_);



        db_helper = new DB_Helper(this,null);


        socketHandler = new SocketHandler();

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        submitRequest = bundle.getBoolean("submitRequest");
        selected_college_id = bundle.getString("selected_college_id");
        selected_parkinglot_id = bundle.getString("selected_parkinglot_id");
        type = bundle.getString("type");

        TextView cText = (TextView) findViewById(R.id.college_name);
        cText.setText(db_helper.getCollegeName(Integer.parseInt(selected_college_id)));
        TextView pText = (TextView) findViewById(R.id.parkinglot_name);
        pText.setText(db_helper.getParkinglotName(Integer.parseInt(selected_parkinglot_id)));
        if(type.equals("ride")){
            pu_lat = bundle.getFloat("pickup_lat");
            pu_lng = bundle.getFloat("pickup_lng");
        }
        else{
            pu_lat = 0;
            pu_lng = 0;
        }
        try {
            user = new JSONObject(bundle.getString("user"));
        } catch (JSONException e) {
            e.printStackTrace();
        }


        mSocket = socketHandler.getSocket();

        mSocket.off("matched_confirm");



        if(submitRequest){
            JSONObject obj = new JSONObject();
            try {
                obj.put("user",user);
                obj.put("college_id", selected_college_id);
                obj.put("parkinglot_id",selected_parkinglot_id);
                obj.put("time",0);
                obj.put("type",type);
                obj.put("pickup_lat",pu_lat);
                obj.put("pickup_lng",pu_lng);
                mSocket.emit("register",obj);
                Log.d("KTag","Register Event : "+ obj.toString());

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }



        mSocket.on("matched_confirm", new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                JSONObject obj = (JSONObject)args[0];
                try {
                    matchMade(obj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public void matchMade(JSONObject obj) throws JSONException {
        Intent intent = new Intent(this, MatchActivity.class);

        Log.d("KTag","Match : "+ obj.toString());
        intent.putExtra("pu_lat",obj.getDouble("pu_lat"));
        intent.putExtra("pu_lng", obj.getDouble("pu_lng"));
        intent.putExtra("rider_id", (String) obj.getString("rider"));
        intent.putExtra("parker_id", (String) obj.getString("parker"));
        intent.putExtra("start_timestamp", (Integer) obj.getInt("start_timestamp"));
        intent.putExtra("user", (String)user.toString());
        startActivity(intent);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void cancelRequest(View view){
        JSONObject t = new JSONObject();
        mSocket.emit("cancelRequest",t);
        finish();
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Press Cancel to return to main screen",Toast.LENGTH_LONG).show();
    }
}
