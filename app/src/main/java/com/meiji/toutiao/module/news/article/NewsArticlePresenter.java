package com.meiji.toutiao.module.news.article;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.meiji.toutiao.ErrorAction;
import com.meiji.toutiao.RetrofitFactory;
import com.meiji.toutiao.api.IMobileNewsApi;
import com.meiji.toutiao.bean.news.MultiNewsArticleBean;
import com.meiji.toutiao.bean.news.MultiNewsArticleDataBean;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Meiji on 2017/5/18.
 */

public class NewsArticlePresenter implements INewsArticle.Presenter {

    private static final String TAG = "NewsArticlePresenter";
    private INewsArticle.View view;
    private List<MultiNewsArticleDataBean> dataList = new ArrayList<>();
    private String category;
    private int time;
    private Gson gson = new Gson();

    NewsArticlePresenter(INewsArticle.View view) {
        this.view = view;
        this.time = (int) (new Date(System.currentTimeMillis()).getTime() / 1000);
    }

    private int getRandom() {
        if (this.time != 0) {
            Random random = new Random();
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                result.append(random.nextInt(10));
            }
            return this.time - Integer.parseInt(result.toString());
        }
        return 0;
    }

    @Override
    public void doLoadData(String... category) {

        try {
            if (this.category == null) {
                this.category = category[0];
            }
        } catch (Exception e) {
            ErrorAction.print(e);
        }

        // 释放内存
        if (dataList.size() > 100) {
            dataList.clear();
        }

        Observable<MultiNewsArticleBean> ob1 = RetrofitFactory.getRetrofit().create(IMobileNewsApi.class)
                .getNewsArticle(this.category, this.time);
        Observable<MultiNewsArticleBean> ob2 = RetrofitFactory.getRetrofit().create(IMobileNewsApi.class)
                .getNewsArticle2(this.category, this.time);

        Observable.merge(ob1, ob2)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(new Function<MultiNewsArticleBean, Observable<MultiNewsArticleDataBean>>() {
                    @Override
                    public Observable<MultiNewsArticleDataBean> apply(@NonNull MultiNewsArticleBean multiNewsArticleBean) throws Exception {
                        List<MultiNewsArticleDataBean> dataList = new ArrayList<>();
                        for (MultiNewsArticleBean.DataBean dataBean : multiNewsArticleBean.getData()) {
                            dataList.add(gson.fromJson(dataBean.getContent(), MultiNewsArticleDataBean.class));
                        }
                        return Observable.fromIterable(dataList);
                    }
                })
                .filter(new Predicate<MultiNewsArticleDataBean>() {
                    @Override
                    public boolean test(@NonNull MultiNewsArticleDataBean multiNewsArticleDataBean) throws Exception {
                        time = multiNewsArticleDataBean.getBehot_time();
                        if (TextUtils.isEmpty(multiNewsArticleDataBean.getSource())) {
                            return false;
                        }
                        try {
                            if (multiNewsArticleDataBean.getSource().contains("头条问答")
                                    || multiNewsArticleDataBean.getTag().contains("ad")
                                    || multiNewsArticleDataBean.getSource().contains("话题")) {
                                return false;
                            }
                            // 过滤头条问答新闻
                            if (multiNewsArticleDataBean.getMedia_info() == null) {
                                return false;
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                        // 过滤重复新闻(与上次刷新的数据比较)
                        for (MultiNewsArticleDataBean bean : dataList) {
                            if (bean.getTitle().equals(multiNewsArticleDataBean.getTitle())) {
                                return false;
                            }
                        }
                        return true;
                    }
                })
                .toList()
                .map(new Function<List<MultiNewsArticleDataBean>, List<MultiNewsArticleDataBean>>() {
                    @Override
                    public List<MultiNewsArticleDataBean> apply(@NonNull List<MultiNewsArticleDataBean> list) throws Exception {
                        // 过滤重复新闻(与本次刷新的数据比较,因为使用了2个请求,数据会有重复)
                        for (int i = 0; i < list.size() - 1; i++) {
                            for (int j = list.size() - 1; j > i; j--) {
                                if (list.get(j).getTitle().equals(list.get(i).getTitle())) {
                                    list.remove(j);
                                }
                            }
                        }
                        return list;
                    }
                })
                .compose(view.<List<MultiNewsArticleDataBean>>bindToLife())
                .subscribe(new Consumer<List<MultiNewsArticleDataBean>>() {
                    @Override
                    public void accept(@NonNull List<MultiNewsArticleDataBean> list) throws Exception {
                        doSetAdapter(list);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        doShowNetError();
                        ErrorAction.print(throwable);
                    }
                });
    }

    @Override
    public void doLoadMoreData() {
        doLoadData();
    }

    @Override
    public void doSetAdapter(List<MultiNewsArticleDataBean> list) {
        dataList.addAll(list);
        view.onSetAdapter(dataList);
        view.onHideLoading();
    }

    @Override
    public void doRefresh() {
        if (dataList.size() != 0) {
            dataList.clear();
            time = (int) (new Date(System.currentTimeMillis()).getTime() / 1000);
        }
        doLoadData();
    }

    @Override
    public void doShowNetError() {
        view.onHideLoading();
        view.onShowNetError();
    }
}
