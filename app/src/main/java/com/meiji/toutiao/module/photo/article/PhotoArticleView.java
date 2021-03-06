package com.meiji.toutiao.module.photo.article;

import android.os.Bundle;
import android.view.View;

import com.meiji.toutiao.Register;
import com.meiji.toutiao.adapter.DiffCallback;
import com.meiji.toutiao.bean.FooterBean;
import com.meiji.toutiao.module.base.BaseListFragment;
import com.meiji.toutiao.utils.OnLoadMoreListener;

import java.util.List;

import me.drakeet.multitype.Items;
import me.drakeet.multitype.MultiTypeAdapter;

public class PhotoArticleView extends BaseListFragment<IPhotoArticle.Presenter> implements IPhotoArticle.View {

    private static final String TAG = "PhotoArticleView";
    private String categoryId;

    public static PhotoArticleView newInstance(String categoryId) {
        Bundle bundle = new Bundle();
        bundle.putString("categoryId", categoryId);
        PhotoArticleView instance = new PhotoArticleView();
        instance.setArguments(bundle);
        return instance;
    }

    @Override
    protected void initData() {
        categoryId = getArguments().getString("categoryId");
    }

    @Override
    protected void initViews(View view) {
        super.initViews(view);
        adapter = new MultiTypeAdapter(oldItems);
        Register.registerPhotoArticleItem(adapter);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                if (canLoadMore) {
                    canLoadMore = false;
                    presenter.doLoadMoreData();
                }
            }
        });
    }

    @Override
    public void fetchData() {
        onLoadData();
    }

    @Override
    public void onRefresh() {
        presenter.doRefresh();
    }

    @Override
    public void onLoadData() {
        onShowLoading();
        presenter.doLoadData(categoryId);
    }

    @Override
    public void onSetAdapter(final List<?> list) {
        Items newItems = new Items(list);
        newItems.add(new FooterBean());
        DiffCallback.notifyDataSetChanged(oldItems, newItems, DiffCallback.PHOTO, adapter);
        oldItems.clear();
        oldItems.addAll(newItems);
        canLoadMore = true;
    }

    @Override
    public void setPresenter(IPhotoArticle.Presenter presenter) {
        if (null == presenter) {
            this.presenter = new PhotoArticlePresenter(this);
        }
    }
}
