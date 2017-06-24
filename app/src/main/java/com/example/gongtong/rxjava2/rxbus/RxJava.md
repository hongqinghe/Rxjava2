关于Rxjava
===========
     创建Observable  ————>输出Observerable—————>对事件进行加工变换——————>订阅（创建Observer）
--------
 ```java
 //用于对图片路径的转化
 //map
 Observable.create(new Observable.just(getFilePath))
           //耗时操作  切换线程到子线程
            .subscribeOn(Schedlers.newThread)
            .onserveOn(Schedlers.io())
            //使用map操作符完成类型的转化
            .map(new Func1<String ,Bitmap>){
            @Override
            public Bitmap call(Stirng s){
                return createBitmaoFromPath(s);
            }
            })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
            new SubScribe<Bitmap>(){
            @Override
            public void onCompleted(){
               //完成观察
            }
            @Override
            public void onError(Throwable e){
            //错误
            }
            @Override
            public void OnNext(Bitmap s){
            showBitmp(s);//处理事件
            }
               );
            }
 ```
flatMap
---------
       这里提到flatmap操作符  ，他可以将一个集合列表转换成另一个观察者的集合表（每个班——————>每个班的具体学生（观察者观察到的对象就为每个具体的学生，不在是各个班级 ））  使用flatmap解决多次嵌套，因为FlatMap可以再次包装新的Observable,而每个Observable都可以使用from(T[])方法来创建自己，这个方法接受一个列表，然后将列表中的数据包装成一系列事件。
 --------
 异步（线程调度）
 ---------
  ![](xiancheng.png)
  线程的调度发生在subscribeOn()----->他只能在被观察者创建阶段使用 ，且只能使用一次（仅有一次的作用） 和
        ObserveOn()-------可用与加工变化阶段和最终观察者处理阶段
参数详解：
1.关于ObServable 的创建 必须使用create 内部传入onSubscribe(他是Observable 内部的唯一属性，是连接Observable和subscribe的关键)
2.只有在发生订阅的时候才能被观察者才会发送消息调用onNext（）和onCompete（）
3.onSubscribe的工作就是调用call（Subscriber）
操作符的具体实现
-----------
####map
```java
   map方法
   //需要的参数 自定义的一个func1的一个实例
     public  final<R> Observable <R> map (Func1<? super T,? extend R>func){
     //返回了一个ObServable
     //SubscribeMap实现的是OnSubscribe(也就是需要实现call（）方法)
        return unsafeCreate(new SubscribeMap<T,R>(this,func))
     }
```
SubscribeMap 的实现
```java
   public  final class OnSubscroneMap <T,R> implements OnSubscribe<R>{
             //用于接收真实的Observable
       final ObServable<T> source;
       final Func1<？ super T,? extends R>   transformer;
   public OnSubscribeMap(Observable <T> source,Func1<? super T,?  extends R>  transformer){
         this.source=source;
         this.transformer=transformer;
      }
   @Override
   //传入的实Subscriber
   public void call(final Subscriber<? super R> o){
         //把外部传入的真实观察者传入到MapSubscribe，构造一个代理的观察者
           MapSubscriber<T ,R> parent =new MapSubscriber<T,R> (o,transformer);
         o.add (parent);
        //让外部的Observable去订阅这个代理的观察者
          source.unsafeSubscriber(parent);
      }
      //Subsciber的子类，由于构建一个代理的观察者
  static final class  mmapSubscriver<T,R>  extends Subscriber <T>{
        //真实的观察者
         final Subscriber<? super R>  actual;
         //自定义的func1
         final  Func1<? super T,? extends R> mapper;
         booolean   done;
       public Map Subscriber (Subscriber <? super R>  actual,Func1 <? super T,? super R> map){
            this.actal=actual;
            this.mapper=map;
       }
       //外部的Observable发送的onNext（） 事件传递到代理观察者这里
       @Override
       public voi onNext(T  t){
         R  result;
         try{
            //call（） 开始变化数据
            result =mapper.call(t);
         }catch(Throwable ex){
             Exceptions.throwIfFlatal(ex);
             unsubscribe();
             onError(OnErrorThrowable.addValueAsLastCause(ex,t));
             return;
         }
         //调用真实的观察者的OnNext(),变换数据之后，把数据传送到真实的观察者手中
            actual.onNext(result);
       }
       //同OnNext()方法一样
       @Override
       public void OnError(Throwable e){
             if(done){
              RxJavaHooks.onError(e);
              return;
             }
             done=true;
             actual.onError(e);
       }
       @Override
     public void OnCompleted(){
         if(done){
             return;
                   }
         actual.onCompleted();
            }
      @Overrde
    public void setProducer(Producer p){
         actual.setProducer(p);
           }
       }
   }
```
这些就是map源码，内部使用call进行创建代理观察者，有被观察者订阅 ，在代理观察者处理完消息后，通知真实观察者
![](map.png)

背压
====
背压就是当被观察者发送的速度远远地高于观察者接受处理的速度 这是就会产生MissingBackpressureException,而背压是流速控制的一种策略
需要注意：
* 背压策略必须实异步环境，也就是说，被观察者和观察者处于不同的线程
* **背压**（Backpressure）并不是一个像flatMap一样可以在程序中直接使用的操作符，他只是一种控制事件流速的策略
 通常的情况下Rxjava模型是被观察者主动 推送消息给观察者，观察者等待接收而处理背压我们可以反过来、
 响应式拉取
 -------
 ```java
       //注意这里使用的是（ range 操作符 而不是interval--->不支持响应式拉取）
     ObServable observable =Observable.range(1,1000);
     class MuSubscriber extends Subscriber<T>{
        @Override
        public void start(){
           //在这里请求数据  由被观察者发送
           request(1);//请求一条
        }
        @Override
        public void OnComplete(){
        //
        }
        public void OnNext(T n){
              //在这里处理消息 ，并在消息处理完后发送下一个事件给被观察
              request(1);//如果想取消则调用request(Long.MAX_VALUE);
        }
     }
     observanble.observeOn(Schedulers.newThead())
                .subscribe(MySubscriber);
 ```
**Observable 的分类**
    *  Hot Observable（创建之后就开始发送数据）--->不支持背压
    *  Cold Observable(订阅后才能发送数据)---->支持背压（但是由iterval ,timer创建的不支持）

####流速控制相关的操作符

  **过滤（抛弃）**
就是虽然生产者产生事件的速度很快，但是把大部分的事件都直接过滤（浪费）掉，从而间接的降低事件发送的速度。

相关类似的操作符：Sample，ThrottleFirst....
 **缓存**
 先缓存一部分，然后慢慢读取（buffer   ,window）
**也可使用两个特殊的操作符**
   OnBackpressurebuffer,onBackpressureDrop
   * onBackpressurebuffer：把observable发送出来的事件做缓存，当request方法被调用的时候，给下层流发送一个item(如果给这个缓存区设置了大小，那么超过了这个大小就会抛出异常)。
   * onBackpressureDrop：将observable发送的事件抛弃掉，直到subscriber再次调用request（n）方法的时候，就发送给它这之后的n个事件。



关于RxJava2
=========
被观察者的分类
                        **不支持背压**
Observable  --------------------------------->Observer
                        **支持背压**
Flowable ---------------------------------------Subscriber

Observable 的正常用法
```java
     Observable mObservable=Observable.create(new ObservableOnSubscribe<Interger>){
            @Orrivide
           public void subscribe(ObservableEmitter<Integer> e) throws Exception{
             e.onNext(1);
             e.onComplete();
     }
     });
     Observable mObserver=new Observer<Integer>(){
       //这是新加入的方法，在订阅后发送数据之前，
            //回首先调用这个方法，而Disposable可用于取消订阅
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Integer value) {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };
     }
     mObservable.subscribe(mObserver);
    ````
Flowale 的用法
```java
    Flowable.range(0,10)
    .subscribe(new Subscriber(<Integer>){
        Subscription sub;
        //当订阅后，会首先调用这个方法，其实就相当于onStrat()
        //传入的Subscription  s参数可以用于请求数据或者取消订阅
        @Override
        public void onSubscribe(Subscription s){
            sub=s;
            sub.request(1);
        }
        @Override
        public void onNext(Integer o){
            sub.request(1);
        }
           @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }
            @Override
            public void onComplete() {
                Log.w("TAG","onComplete");
            }
         }
    });
```
    这里的Flowable采用的是range创建也可使用create创建但是如果要支持背压，必须指定背压的策略 .BachpressureStrategy.BUFFER；
**其他观察者(适合发送单个数据)**

* Single/SingleObserver
* Completable/CompletableObserver
* Maybe/MaybeObserver
关于Rxjava中的操作符整理：
1. **map**：
     使用可以转化数据类型，在发送数据的时候不会发送重复的数据给你观察者
     onNext(1)   onNext(2)  onNext(2)  接收到的数据为----> 1 、 2    不会多次接收到2
2.  **flatMap**
    使用这个操作符可以数据整理发送　，可将一个集合转换为另一个集合，再从另一个几个中取数据
3. **flatMap————--------.filter**
     使用时可以利用.filter(bew Predicate<T>)  进行筛选
     网络请求中，请求出来集合是List<User>   ,我们可以先利用flatmap转化为一user为对象的Observable，在利用flatmap转化为我们想要的vo
```java
   .flatMap(new Function<List<User>, ObservableSource<User>>() { // flatMap - to return users one by one
                    @Override
                    public ObservableSource<User> apply(List<User> usersList) throws Exception {
                        return Observable.fromIterable(usersList); // returning user one by one from usersList.
                    }
                })
                .flatMap(new Function<User, ObservableSource<UserDetail>>() {
                    @Override
                    public ObservableSource<UserDetail> apply(User user) throws Exception {
                        // here we get the user one by one
                        // and returns corresponding getUserDetailObservable
                        // for that userId
                        return getUserDetailObservable(user.id);
                    }
                })
````
4. **zip**
    使用这个作为网络请求.zip(ObservableSource<? extends T1> source1, ObservableSource<? extends T2> source2, BiFunction<? super T1, ? super T2, ? extends R> zipper)等zip的构造函数soucrc1 和source2为两个ObServable获取方式：
```java
   private Observable<List<User>> getCricketFansObservable() {
        return Rx2AndroidNetworking.get("https://fierce-cove-29863.herokuapp.com/getAllCricketFans")
                .build()
                .getObjectListObservable(User.class);
    }
    ```
而在这里使用的是BiFunction<T1, T2, R>，其余的一样
5. **flatMap with Zip**
      这个可以基于两个flatmap 加zip，如上的两个flatmap如产生两个vo可以使用Pair组合Pair<F, S> {
    public final F first;
    public final S second;
    这时拿到第一个vo个一个集合 使用zip发送消息，被观察者重新发送new BiFunction(可将vo）转换为合并的pair的vo     进行return
```java
.flatMap(new Function<List<User>, ObservableSource<User>>() { // flatMap - to return users one by one
                    @Override
                    public ObservableSource<User> apply(List<User> usersList) throws Exception {
                     return Observable.fromIterable(usersList); // returning user one by one from usersList.
                    }
                })
                .flatMap(new Function<User, ObservableSource<Pair<UserDetail, User>>>() {
                    @Override
                    public ObservableSource<Pair<UserDetail, User>> apply(User user) throws Exception {
                        // here we get the user one by one and then we are zipping
                        // two observable - one getUserDetailObservable (network call to get userDetail)
                        // and another Observable.just(user) - just to emit user
                        return Observable.zip(getUserDetailObservable(user.id),
                                Observable.just(user),
                                new BiFunction<UserDetail, User, Pair<UserDetail, User>>() {
                                    @Override
                public Pair<UserDetail, User> apply(UserDetail userDetail, User user) throws Exception {
                                        // runs when network call completes
                                        // we get here userDetail for the corresponding user
                              return new Pair<>(userDetail, user); // returning the pair(userDetail, user)
                                    }
                                });
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Pair<UserDetail, User>>() {
                    @Override
                    public void onComplete() {
                        // do something onCompleted
                        Log.d(TAG, "onComplete");
                    }

                    @Override
                    public void onError(Throwable e) {
                        // handle error
                        Utils.logError(TAG, e);
                    }

                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Pair<UserDetail, User> pair) {
                        // here we are getting the userDetail for the corresponding user one by one
                        UserDetail userDetail = pair.first;
                        User user = pair.second;
                        Log.d(TAG, "user : " + user.toString());
                        Log.d(TAG, "userDetail : " + userDetail.toString());
                    }
                });
```
6. **take**
   网络请求是可以限制被观察者发送的数据，有100条可以使用take(5) 发送5条结果
7. **disposables**
     实现方式是构造后使用add方式添加
```java
private final CompositeDisposable disposables = new CompositeDisposable();
     disposables.add( sampleObservable()
                // Run on a background thread
                .subscribeOn(Schedulers.io())
                // Be notified on the main thread
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<String>() {
                    @Override
                    public void onComplete() {
                        textView.append(" onComplete");
                        textView.append(AppConstant.LINE_SEPARATOR);
                        Log.d(TAG, " onComplete");
                    }

                    @Override
                    public void onError(Throwable e) {
                        textView.append(" onError : " + e.getMessage());
                        textView.append(AppConstant.LINE_SEPARATOR);
                        Log.d(TAG, " onError : " + e.getMessage());
                    }

                    @Override
                    public void onNext(String value) {
                        textView.append(" onNext : value : " + value);
                        textView.append(AppConstant.LINE_SEPARATOR);
                        Log.d(TAG, " onNext value : " + value);
                    }
                }));
    }
    static Observable<String> sampleObservable() {
        return Observable.defer(new Callable<ObservableSource<? extends String>>() {
            @Override
            public ObservableSource<? extends String> call() throws Exception {
                // Do some long running operation
                SystemClock.sleep(2000);
                return Observable.just("one", "two", "three", "four", "five");
                }
                }
```
8. **timer**
     实现了一直中计时操作（可直接返回Observable）
     Observable.timer(3,TimeUnit.SECONDS)
9. **iterval**
     实现了一种从什么时间开始间隔多长时间开始发送
     Observable.interval(0, 2, TimeUnit.SECONDS)
10. **SingleObserver**
      发送单条数据的观察者
      Single.just("1").subscribe(new SingleObserver)  内部实现的方法是onSubscribe onSuccess  onError
11. **Completable**
      只是通知接收完成
       Completable completable = Completable.timer(1000, TimeUnit.MILLISECONDS);回调方式  onSubscribe onComplete onError
12. **flowable**
```java
  Flowable<Integer> observable = Flowable.just(1, 2, 3,4);

        observable.reduce(50, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }
        }).subscribe(getObserver());

    }

    private SingleObserver<Integer> getObserver() {

        return new SingleObserver<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.d(TAG, " onSubscribe : " + d.isDisposed());
            }

            @Override
            public void onSuccess(Integer value) {
                textView.append(" onSuccess : value : " + value);
                textView.append(AppConstant.LINE_SEPARATOR);
                Log.d(TAG, " onSuccess : value : " + value);
            }

            @Override
            public void onError(Throwable e) {
                textView.append(" onError : " + e.getMessage());
                textView.append(AppConstant.LINE_SEPARATOR);
                Log.d(TAG, " onError : " + e.getMessage());
            }
        };
    }
```
13. **reduce**
     用于相加运算  两个构造方法   一个可以给定基数在次基础上相加，一种是被观察者的数值直接运算
14. **buffer**
   这个操作符可以对数据进行缓存处理可设置缓存的大小，并可设置缓存跳过第几条数据开始
   buffer(int count, int skip)    buffer(int count)
15. **filter**
     对数据进行过滤
```java
   Observable.just(1, 2, 3, 4, 5, 6)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer integer) throws Exception {
                        return integer % 2 == 0;
                    }
                })
                .subscribe(getObserver());
             ```
16. **skip**
     跳过几条数据后 进行发送
     比如 1 2  3  4  5    .ship(2)   ------>3   4        5
17. **Scan**
     和reduce的用法相同 求和  1 2 3 4 5  但是会逐条发送结果 显示结果为  1 3 6 10 15 
18. **replay**

```java
  PublishSubject<Integer> source = PublishSubject.create();
        ConnectableObservable<Integer> connectableObservable = source.replay(3); // bufferSize = 3 to retain 3 values to replay
        connectableObservable.connect(); // connecting the connectableObservable

        connectableObservable.subscribe(getFirstObserver());

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        source.onNext(4);
        source.onComplete();

        /*
         * it will emit 2, 3, 4 as (count = 3), retains the 3 values for replay
         */
        connectableObservable.subscribe(getSecondObserver());

    }


    private Observer<Integer> getFirstObserver() {
        return new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {
                Log.d(TAG, " First onSubscribe : " + d.isDisposed());
            }

            @Override
            public void onNext(Integer value) {
                textView.append(" First onNext : value : " + value);
                textView.append(AppConstant.LINE_SEPARATOR);
                Log.d(TAG, " First onNext value : " + value);
            }

            @Override
            public void onError(Throwable e) {
                textView.append(" First onError : " + e.getMessage());
                textView.append(AppConstant.LINE_SEPARATOR);
                Log.d(TAG, " First onError : " + e.getMessage());
            }

            @Override
            public void onComplete() {
                textView.append(" First onComplete");
                textView.append(AppConstant.LINE_SEPARATOR);
                Log.d(TAG, " First onComplete");
            }
        };
    }

    private Observer<Integer> getSecondObserver() {
        return new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {
                textView.append(" Second onSubscribe : isDisposed :" + d.isDisposed());
                Log.d(TAG, " Second onSubscribe : " + d.isDisposed());
                textView.append(AppConstant.LINE_SEPARATOR);
            }

            @Override
            public void onNext(Integer value) {
                textView.append(" Second onNext : value : " + value);
                textView.append(AppConstant.LINE_SEPARATOR);
                Log.d(TAG, " Second onNext value : " + value);
            }

            @Override
            public void onError(Throwable e) {
                textView.append(" Second onError : " + e.getMessage());
                Log.d(TAG, " Second onError : " + e.getMessage());
            }

            @Override
            public void onComplete() {
                textView.append(" Second onComplete");
                Log.d(TAG, " Second onComplete");
            }
        };
    }

```
19 .  **concat**
      表示有一个观察者观察了多个被观察者
      Observable.concat(aObservable, bObservable)两个被观察者发送数据一个观察者接收
20 . **Merge**
   合并被观察者merge(ObservableSource<? extends T> source1, ObservableSource<? extends T> source2)
21 .**defer**
    用于创建 Observable
 1.  create操作在call方法结束之后，需要手动调用subscriber.next()或subscriber.complete()方法。
 2. just，from或defer都是在producer中调用的onComplete、onNext、onError方法。
 3. future，首先ObSubscribeToObservableFuture.call方法，在该方法中，当future取到callable的返回值后，将回调onNext、onComplete、OnError方法

```java
public class Car {

    private String brand;

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Observable<String> brandDeferObservable() {
        return Observable.defer(new Callable<ObservableSource<? extends String>>() {
            @Override
            public ObservableSource<? extends String> call() throws Exception {
                return Observable.just(brand);
            }
        });
    }
}

  Car car = new Car();

        Observable<String> brandDeferObservable = car.brandDeferObservable();

        car.setBrand("BMW");  // Even if we are setting the brand after creating Observable
        // we will get the brand as BMW.
        // If we had not used defer, we would have got null as the brand.

        brandDeferObservable
                .subscribe(getObserver());
        接收"BAW"

```
22 . **distinct**
  将不同的数值发送，

```java
      getObservable()
                .distinct()
                .subscribe(getObserver());
    }

    private Observable<Integer> getObservable() {
        return Observable.just(1, 2, 1, 1, 2, 3, 4, 6, 4);
    }
    输出值为 1  2  3  4   6
    ```
23 . **last**
   发送最后一个消息
24 . **ReplaySubject**
     重新发送被观察者 发送重复的事件  123----->123  123  无论它们是何时订阅的。
25 . **PublishSubject**
    在有两个订阅者的时候，可以在发送事件的时候选择发送  在订阅的时候位置的选取不同
    只会给在订阅者订阅的时间点之后的数据发送给观察者。
```java
  PublishSubject<Integer>  source=PublishSubject.create();
  source.subscribe(oneObserver());
  source.onNext(1);
  source.onNext(2);
  source.subscribe(twoObserver());
  source.onNext(3);
  source.onComplete();


输出值：  First onNext value :1
First onNext value : 2
Second onSubscribe : false
First onNext value : 4
Second onNext value : 4
```
26 . ** BehaviorSubject**
    重复上一个订阅者接受的最后一条消息，在发送剩余的消息
    和PubishSubject的结果不同
    在订阅者订阅时，会发送其最近发送的数据（如果此时还没有收到任何数据，它会发送一个默认值）

```java
         输出值：  First onNext value : 1
         First onNext value : 2
         Second onSubscribe : false
         Second onNext value : 2
         First onNext value : 4
         Second onNext value : 4
         ```
27 .  **AsyncSubject**
     只发送最后一个订阅者订阅后的消息（异步消息机制） 
     只在原Observable事件序列完成后，发送最后一个数据，后续如果还有订阅者继续订阅该Subject, 则可以直接接收到最后一个值
```java
         输出值：
         First onNext value : 4
         First onComplete
         Second onNext value : 4
         Second onComplete
         ```
28 . **ThrottleFirst***
   设置一个事件段，在这个事件段中发送事件，过一个时间段发送一个数据可设置在Android中的一个点击事件  防止多次点击
```java
     private void doSomeWork() {
        getObservable()
                .throttleLast(500, TimeUnit.MILLISECONDS)
                // Run on a background thread
                .subscribeOn(Schedulers.io())
                // Be notified on the main thread
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getObserver());
    }

    private Observable<Integer> getObservable() {
        return Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                // send events with simulated time wait
                Thread.sleep(0);
                emitter.onNext(1); // skip
                emitter.onNext(2); // deliver
                Thread.sleep(505);
                emitter.onNext(3); // skip
                Thread.sleep(99);
                emitter.onNext(4); // skip
                Thread.sleep(100);
                emitter.onNext(5); // skip
                emitter.onNext(6); // deliver
                Thread.sleep(305);
                emitter.onNext(7); // deliver
                Thread.sleep(510);
                emitter.onComplete();
            }
        });
    }
    输出：1   3   7
````
29 . **ThrottleLast**
     发送间隔后的消息
```java
     输出：2     6   7
```
30 . **Dobounce**
     防抖动  仅在过了一段指定的时间还没发射数据时才发射一个数据
```java
    getObservable()
                .debounce(500, TimeUnit.MILLISECONDS)
                // Run on a background thread
                .subscribeOn(Schedulers.io())
                // Be notified on the main thread
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getObserver());
    }

    private Observable<Integer> getObservable() {
        return Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                // send events with simulated time wait
                emitter.onNext(1); // skip
                Thread.sleep(400);
                emitter.onNext(2); // deliver
                Thread.sleep(505);
                emitter.onNext(3); // skip
                Thread.sleep(100);
                emitter.onNext(4); // deliver
                Thread.sleep(605);
                emitter.onNext(5); // deliver
                Thread.sleep(510);
                emitter.onComplete();
            }
        });
    }
    输出：   2    4   5
```
31 .**window**
   和buffer类似，但不是发射来自原始Observable的数据包，它发射的是Observables，这些Observables中的每一个都发射原始Observable数据的一个子集，最后发射一个onCompleted通知。
32  . **delay**
延迟一段指定的时间再发射来自Observable的发射物
33 . **Sample**
Sample操作符会定时地发射源Observable最近发射的数据，其他的都会被过滤掉，等效于ThrottleLast操作符。
