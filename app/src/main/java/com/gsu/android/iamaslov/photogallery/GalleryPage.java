package com.gsu.android.iamaslov.photogallery;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GalleryPage {

    PhotoPages photos;
    String stat;

    public static GalleryPage sGalleryPage;

    public static GalleryPage getGalleryPage() {
        if (sGalleryPage == null) {
            sGalleryPage = new GalleryPage();
        }
        return sGalleryPage;
    }

    private GalleryPage() {  }

    public List<GalleryItem> getGalleryItemList() {
        return photos.getPhotoList();
    }

    public int getTotalPages() {
        return photos.pages;
    }
    public int getItemPerPage() {
        return photos.perpage;
    }
}

class PhotoPages {
    int page;
    int pages;
    int perpage;
    int total;
    @SerializedName("photo")
    List<GalleryItem> photoList;

    List<GalleryItem> getPhotoList() {
        return photoList;
    }
}
