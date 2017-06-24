package com.example.gongtong.rxjava2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.gongtong.rxjava2.rxbus.RxBus;
import com.example.gongtong.rxjava2.rxbus.annotation.Subscribe;
import com.example.gongtong.rxjava2.rxbus.thread.EventThread;

import org.greenrobot.eventbus.EventBus;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Button map, flatMap,disposaable,rxBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

      initView();
    }

    private void initView() {
        map = (Button) findViewById(R.id.map);
        flatMap = (Button) findViewById(R.id.flatMap);
        disposaable = (Button) findViewById(R.id.disposaable);
        rxBus = (Button) findViewById(R.id.RxBus);

        initEvent();
    }

    private void initEvent() {
        map.setOnClickListener(this);
        flatMap.setOnClickListener(this);
        disposaable.setOnClickListener(this);
        rxBus.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id){
          case R.id.map:
              Intent intent=new Intent(this,ContentActivity.class);
              intent.putExtra("map","map");
              startActivity(intent);
              break;
            case R.id.disposaable:
                Intent intent2=new Intent(this,ContentActivity.class);
                intent2.putExtra("map","disposaable");
                startActivity(intent2);
                break;
            case R.id.RxBus:
//                Intent intent3=new Intent(this,ContentActivity.class);
//                intent3.putExtra("map","test");
//                startActivity(intent3);
                RxBus.getInstance().post(new EventTypeTest("test"));
//                EventBus.getDefault().post(new EventTypeTest("test2"));
                break;
      }
    }
   public   class EventTypeTest{
         String string;

         public EventTypeTest(String string) {
             this.string = string;
         }

         public String getString() {
             return string;
         }

         public void setString(String string) {
             this.string = string;
         }
     }
}
