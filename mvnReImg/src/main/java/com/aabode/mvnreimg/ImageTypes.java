/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aabode.mvnreimg;

import java.util.logging.Logger;

/**
 *
 * @author sunrise
 */
public enum ImageTypes {
    SIZE_300(300, 176), // addImage
    SIZE_350(350, 206), // search
    SIZE_127(127, 75), // small images at accommodation view
    SIZE_780(780, 458), // large images at accommodation view
    SIZE_274(274, 274), // home page images
    SIZE_335(335, 335), // home page images
    SIZE_592(592, 274), // search page images
    SIZE_1200(1200, 900); // maximum image for API
    
    private int width;
    private int height;

    ImageTypes(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    public String getFileName(String pictureId) {
        return pictureId + "." + width + ".png";
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
    
    
}
