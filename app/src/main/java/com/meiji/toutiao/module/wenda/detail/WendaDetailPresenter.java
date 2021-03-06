package com.meiji.toutiao.module.wenda.detail;

import com.meiji.toutiao.ErrorAction;
import com.meiji.toutiao.RetrofitFactory;
import com.meiji.toutiao.api.IMobileNewsApi;
import com.meiji.toutiao.api.IMobileWendaApi;
import com.meiji.toutiao.bean.news.NewsCommentMobileBean;
import com.meiji.toutiao.utils.SettingsUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

/**
 * Created by Meiji on 2017/5/23.
 */

public class WendaDetailPresenter implements IWendaDetail.Presenter {

    private IWendaDetail.View view;
    private String groupId;
    private int offset = 0;
    private List<NewsCommentMobileBean.DataBean.CommentBean> commentsBeanList = new ArrayList<>();

    WendaDetailPresenter(IWendaDetail.View view) {
        this.view = view;
    }

    @Override
    public void doRefresh() {
        if (commentsBeanList.size() != 0) {
            commentsBeanList.clear();
            offset = 0;
        }
        view.onLoadData();
    }

    @Override
    public void doLoadData(String url) {
        RetrofitFactory.getRetrofit().create(IMobileWendaApi.class)
                .getWendaAnsDetail(url)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<ResponseBody>() {
                    @Override
                    public void accept(@NonNull ResponseBody responseBody) throws Exception {
                        String result = getHTML(responseBody.string());
                        if (result != null) {
                            view.onSetWebView(result, true);
                        } else {
                            view.onSetWebView(null, false);
                        }
                        view.onHideLoading();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        view.onSetWebView(null, false);
                        view.onHideLoading();
                        ErrorAction.print(throwable);
                    }
                });
    }

    @Override
    public void doLoadComment(String... ansId) {

        try {
            if (null == groupId) {
                this.groupId = ansId[0];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        RetrofitFactory.getRetrofit().create(IMobileNewsApi.class)
                .getNewsComment(groupId, offset)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .map(new Function<NewsCommentMobileBean, List<NewsCommentMobileBean.DataBean.CommentBean>>() {
                    @Override
                    public List<NewsCommentMobileBean.DataBean.CommentBean> apply(@NonNull NewsCommentMobileBean newsCommentMobileBean) throws Exception {
                        List<NewsCommentMobileBean.DataBean.CommentBean> data = new ArrayList<>();
                        for (NewsCommentMobileBean.DataBean bean : newsCommentMobileBean.getData()) {
                            data.add(bean.getComment());
                        }
                        return data;
                    }
                })
                .compose(view.<List<NewsCommentMobileBean.DataBean.CommentBean>>bindToLife())
                .subscribe(new Consumer<List<NewsCommentMobileBean.DataBean.CommentBean>>() {
                    @Override
                    public void accept(@NonNull List<NewsCommentMobileBean.DataBean.CommentBean> list) throws Exception {
                        if (list.size() > 0) {
                            doSetAdapter(list);
                        } else {
                            doShowNoMore();
                        }
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
    public void doLoadMoreComment() {
        offset += 10;
        doLoadComment();
    }

    @Override
    public void doSetAdapter(List<NewsCommentMobileBean.DataBean.CommentBean> list) {
        commentsBeanList.addAll(list);
        view.onSetAdapter(commentsBeanList);
        view.onHideLoading();
    }

    @Override
    public void doShowNetError() {
        view.onHideLoading();
        view.onShowNetError();
    }

    @Override
    public void doShowNoMore() {
        view.onHideLoading();
        view.onShowNoMore();
    }

    private String getHTML(String response) {
        Document doc = Jsoup.parse(response, "UTF-8");
        Elements elements = doc.getElementsByClass("con-words");
        String content = null;
        for (Element element : elements) {
            content = element.toString();
            break;
        }
        if (content != null) {

            String css = "<link rel=\"stylesheet\" href=\"file:///android_asset/toutiao_light.css\" type=\"text/css\">";
            if (SettingsUtil.getInstance().getIsNightMode()) {
                css = css.replace("toutiao_light", "toutiao_dark");
            }

            String html = "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">" +
                    css +
                    "<body>\n" +
                    "<article class=\"article-container\">\n" +
                    "    <div class=\"article__content article-content\">" +
                    content +
                    "    </div>\n" +
                    "</article>\n" +
                    "</body>\n" +
                    "</html>";

            return html;
        } else {
            return null;
        }
    }
}
