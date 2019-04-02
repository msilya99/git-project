package com.gsu.android.iamaslov.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {

    private static String TAG = "PhotoGallery";
    private RecyclerView mPhotoRecyclerView;
    private TextView mCurrentPageText;
    private GridLayoutManager mGridManager;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<Integer> mThumbnailDownloader;
    boolean asyncFetching = false;
    int mCurrentPage = 1;
    int mMaxPage = 1;
    int mItemsPerPage = 1;
    int mFirstItemPosition, mLastItemPosition;


    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemTask().execute();
        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<Integer>() {
            @Override
            public void onThumbnailDownloaded(Integer position, Bitmap thumbnail) {
                mPhotoRecyclerView.getAdapter().notifyItemChanged(position);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mCurrentPageText = (TextView) v.findViewById(R.id.currentPageText);
        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.photo_recycler_view);
        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                float columnWidthInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 140, getActivity().getResources().getDisplayMetrics());
                int width = mPhotoRecyclerView.getWidth();
                int columnNumber = Math.round(width / columnWidthInPixels);
                mGridManager = new GridLayoutManager(getActivity(), columnNumber);
                mPhotoRecyclerView.setLayoutManager(mGridManager);
                mPhotoRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int lastVisibleItem = mGridManager.findLastVisibleItemPosition();
                int firstVisibleItem = mGridManager.findFirstVisibleItemPosition();
                if (mLastItemPosition != lastVisibleItem || mFirstItemPosition != firstVisibleItem) {
                    Log.d(TAG, "Showing item " + firstVisibleItem + " to " + lastVisibleItem);
                    updatePageText(firstVisibleItem);
                    mLastItemPosition = lastVisibleItem;
                    mFirstItemPosition = firstVisibleItem;
                    int begin = Math.max(firstVisibleItem - 10, 0);
                    int end = Math.min(lastVisibleItem + 10, mItems.size() - 1);
                    for (int position = begin; position <= end; position++) {
                        String url = mItems.get(position).getUrl();
                        if (mThumbnailDownloader.mPhotoCache.get(url) == null) {
                            Log.d(TAG, "Requesting Download at position: " + position);
                            mThumbnailDownloader.queueThumbnail(position, url);
                        }

                    }
                }
                if (!(asyncFetching) && (dy > 0) && (mCurrentPage < mMaxPage) && (lastVisibleItem >= (mItems.size() - 1))) {
                    Log.d(TAG, "Fetching more items");
                    new FetchItemTask().execute();
                }
            }
        });

        if (mPhotoRecyclerView.getAdapter() == null) setupAdapter();

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
        mThumbnailDownloader.clearCache();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");

    }

    private void updatePageText(int pos) {
        mCurrentPage = pos / mItemsPerPage + 1;
        String pageText = getString(R.string.page_text, mCurrentPage, mMaxPage);
        mCurrentPageText.setText(pageText);
    }

    private void setupAdapter() {
        if (isAdded()) {
            Log.d(TAG, "Setup Adapter");
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);
            mItemImageView = (ImageView) itemView.findViewById(R.id.item_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @NonNull
        @Override
        public PhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoHolder photoHolder, int position) {
            Log.d(TAG, "Binding item " + position + " to " + photoHolder.hashCode());
            GalleryItem galleryItem = mGalleryItems.get(position);
            String url = galleryItem.getUrl();
            Bitmap bitmap = mThumbnailDownloader.mPhotoCache.get(url);
            if (bitmap == null) {
                Drawable placeholder = getResources().getDrawable(R.drawable.gun_space_laser_weapon_galaxy_war);
                photoHolder.bindDrawable(placeholder);
            } else {
                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                photoHolder.bindDrawable(drawable);
            }

        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemTask extends AsyncTask<Void, Void, List<GalleryItem>> {
        @Override
        protected List<GalleryItem> doInBackground(Void... params) {

            asyncFetching = true;
            return new FlickrFetchr().fetchItems(mCurrentPage + 1);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            asyncFetching = false;
            mItems.addAll(items);
            GalleryPage pge = GalleryPage.getGalleryPage();
            mMaxPage = pge.getTotalPages();
            mItemsPerPage = pge.getItemPerPage();
            if (mPhotoRecyclerView.getAdapter() == null) setupAdapter();
            mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
            updatePageText(mGridManager.findFirstVisibleItemPosition());
        }
    }
}