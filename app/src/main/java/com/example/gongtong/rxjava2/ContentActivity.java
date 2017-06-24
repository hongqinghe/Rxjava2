package com.example.gongtong.rxjava2;

import android.content.Intent;

import android.provider.SyncStateContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gongtong.rxjava2.rxbus.RxBus;
import com.example.gongtong.rxjava2.rxbus.annotation.Subscribe;
import com.example.gongtong.rxjava2.rxbus.annotation.Tag;
import com.example.gongtong.rxjava2.rxbus.thread.EventThread;
import com.example.gongtong.rxjava2.rxbus.vo.EventType;

import org.greenrobot.eventbus.EventBus;
import org.reactivestreams.Subscriber;

import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.internal.operators.observable.ObservableElementAtSingle;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class ContentActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView textView;
    private String var;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);
        Intent intent = getIntent();
        var = intent.getStringExtra("map");

        Button work = (Button) findViewById(R.id.work);
        textView = (TextView) findViewById(R.id.textContent);
        work.setOnClickListener(this);
        RxBus.getInstance().register(this);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.work:
                if (var.equals("map")) {
                    Observable.create(new ObservableOnSubscribe<String>() {
                        @Override
                        public void subscribe(@NonNull ObservableEmitter<String> e) throws Exception {
                            e.onNext("1");
                            e.onComplete();
                        }
                    })
                            .map(new Function<String, Integer>() {
                                @Override
                                public Integer apply(@NonNull String s) throws Exception {
                                    return Integer.parseInt(s);
                                }
                            })
                            //订阅
                            .subscribe(new Observer<Integer>() {
                                @Override
                                public void onSubscribe(@NonNull Disposable d) {


                                }

                                @Override
                                public void onNext(@NonNull Integer integer) {
                                    textView.setVisibility(View.VISIBLE);
                                    System.out.println(integer);
                                    textView.setText(integer + "接受数据");
                                }

                                @Override
                                public void onError(@NonNull Throwable e) {

                                }

                                @Override
                                public void onComplete() {

                                }
                            });
                 }else if (var.equals("disposaable")){
                    disposaable();
                }
                break;
        }
    }

    private void disposaable() {
        CompositeDisposable disposable=new CompositeDisposable();
        disposable.add(sampleObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<String>() {
                    @Override
                    public void onNext(@NonNull String s) {
                        textView.setVisibility(View.VISIBLE);
                        textView.append("next"+s);
                        textView.append("\n");
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                })
        );
    }

    private Observable<String> sampleObservable() {
        return Observable.defer(new Callable<ObservableSource<? extends String>>() {
            @Override
            public ObservableSource<? extends String> call() throws Exception {
                return Observable.just("1","2");
            }
        });
    }
@Subscribe(thread = EventThread.MAIN_THREAD,tags = {@Tag()})
    public void onEvent(MainActivity.EventTypeTest eventType) {
        if (eventType!=null){
            String string = eventType.getString();
            Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
        }
    }
@org.greenrobot.eventbus.Subscribe
    public void pst(MainActivity.EventTypeTest eventTypeTest) {
    if (eventTypeTest!=null){
        String string = eventTypeTest.getString();
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
    }
    }
}
