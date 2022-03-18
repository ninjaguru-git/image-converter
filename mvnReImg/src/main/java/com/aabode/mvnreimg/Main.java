/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aabode.mvnreimg;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author sunrise
 */
public class Main {
    
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    
    private Set<PosixFilePermission> getFilePermissions() {
        Set<PosixFilePermission> filePermissions = new HashSet<>();
        filePermissions.add(PosixFilePermission.OWNER_READ);
        filePermissions.add(PosixFilePermission.OWNER_WRITE);
        filePermissions.add(PosixFilePermission.GROUP_READ);
        filePermissions.add(PosixFilePermission.GROUP_EXECUTE);
        filePermissions.add(PosixFilePermission.OTHERS_READ);
        return filePermissions;
    }
    
    private Set<PosixFilePermission> getFolderPermissions() {
        Set<PosixFilePermission> folderPermissions = new HashSet<>();
        folderPermissions.add(PosixFilePermission.OWNER_READ);
        folderPermissions.add(PosixFilePermission.OWNER_WRITE);
        folderPermissions.add(PosixFilePermission.OWNER_EXECUTE);
        folderPermissions.add(PosixFilePermission.GROUP_READ);
        folderPermissions.add(PosixFilePermission.GROUP_WRITE);
        folderPermissions.add(PosixFilePermission.GROUP_EXECUTE);
        folderPermissions.add(PosixFilePermission.OTHERS_READ);
        folderPermissions.add(PosixFilePermission.OTHERS_EXECUTE);
        return folderPermissions;
    }

    private boolean resizeImage( File oriPicture, BufferedImage originalImageRaw, ImageTypes imageType) throws IOException {
        boolean success = true;
        BufferedImage scaledImg;
        
        String imgRootPath = oriPicture.getAbsoluteFile().getParent();
        String pictureId = FilenameUtils.getBaseName(oriPicture.getName());
        pictureId = pictureId.substring(0, pictureId.length()-4);
        
        File image = new File(imgRootPath + "/" + imageType.getFileName(pictureId));
        if (originalImageRaw.getHeight() < originalImageRaw.getWidth()) {
            scaledImg = Scalr.resize(originalImageRaw, Scalr.Mode.FIT_TO_WIDTH, imageType.getWidth(), imageType.getHeight());
            if ( scaledImg.getHeight() < imageType.getHeight() || scaledImg.getWidth() < imageType.getWidth() ) {
                scaledImg = Scalr.resize(originalImageRaw, Scalr.Mode.FIT_TO_HEIGHT, imageType.getWidth(), imageType.getHeight());
            }
        } else {
            scaledImg = Scalr.resize(originalImageRaw, Scalr.Mode.FIT_TO_HEIGHT, imageType.getWidth(), imageType.getHeight());
            if ( scaledImg.getHeight() < imageType.getHeight() || scaledImg.getWidth() < imageType.getWidth() ) {
                scaledImg = Scalr.resize(originalImageRaw, Scalr.Mode.FIT_TO_WIDTH, imageType.getWidth(), imageType.getHeight());
            }
        }
        int x = (scaledImg.getWidth() - imageType.getWidth()) / 2;
        int y = (scaledImg.getHeight() - imageType.getHeight()) / 2;

        scaledImg = Scalr.crop(scaledImg, x, y, imageType.getWidth(), imageType.getHeight());
        ImageIO.write(scaledImg, "png", image);
        image.setReadable(true);
        image.setWritable(true);
        image.setExecutable(true);
        System.out.printf("%s generated. \n", image.getName());
        scaledImg.flush();
        return success;
    }
    
    private BufferedImage rotateImage(BufferedImage originalImage, File originalFile) {

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new FileInputStream(originalFile));
            ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

            int orientation = -1;
            try {
                orientation = exifIFD0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            } catch (Exception ex) {
                log.info("No EXIF information found for image: " + originalFile.getName());
            }

            switch (orientation) {
                case 1:
                    break;
                case 2: // Flip X
                    originalImage = Scalr.rotate(originalImage, Scalr.Rotation.FLIP_HORZ);
                    break;
                case 3: // PI rotation
                    originalImage = Scalr.rotate(originalImage, Scalr.Rotation.CW_180);
                    break;
                case 4: // Flip Y
                    originalImage = Scalr.rotate(originalImage, Scalr.Rotation.FLIP_VERT);
                    break;
                case 5: // - PI/2 and Flip X
                    originalImage = Scalr.rotate(originalImage, Scalr.Rotation.CW_90);
                    originalImage = Scalr.rotate(originalImage, Scalr.Rotation.FLIP_HORZ);
                    break;
                case 6: // -PI/2 and -width
                    originalImage = Scalr.rotate(originalImage, Scalr.Rotation.CW_90);
                    break;
                case 7: // PI/2 and Flip
                    originalImage = Scalr.rotate(originalImage, Scalr.Rotation.CW_90);
                    originalImage = Scalr.rotate(originalImage, Scalr.Rotation.FLIP_VERT);
                    break;
                case 8: // PI / 2
                    originalImage = Scalr.rotate(originalImage, Scalr.Rotation.CW_270);
                    break;
                default:
                    break;
            }
        } catch (ImageProcessingException | IOException e) {
            log.error("ImageProcessingException", e);
        }
        return originalImage;
    }
    
    private Boolean resizeImage(File originalImage) {
        boolean success = true;
        BufferedImage originalImageRaw = null;
        try {
            originalImageRaw = ImageIO.read(originalImage);
            // rotate image
            originalImageRaw = this.rotateImage(originalImageRaw, originalImage);
        } catch (IOException e) {
            log.error("Error resizing image " + originalImage.getName() + " " + e.getMessage(), e);
            success = false;
        }

        if ( success ) {
            try {
                if (originalImageRaw != null) {
                    // save 350x250
                    this.resizeImage(originalImage, originalImageRaw, ImageTypes.SIZE_300);
                    // save 350x250
                    this.resizeImage(originalImage, originalImageRaw, ImageTypes.SIZE_350);
                    // save 127x80
                    this.resizeImage(originalImage, originalImageRaw, ImageTypes.SIZE_127);
                    // save 780x488
                    this.resizeImage(originalImage, originalImageRaw, ImageTypes.SIZE_780);
                    // save 335x335
                    this.resizeImage(originalImage, originalImageRaw, ImageTypes.SIZE_335);
                    // 1200x900 added by Oleks 01/18/2021
                    this.resizeImage(originalImage, originalImageRaw, ImageTypes.SIZE_1200);
                } else {
                    log.error("Image " + originalImage + " does not exist");
                }
            } catch (IOException e) {
                log.error("Error resizing image " + originalImage.getName() + " " + e.getMessage(), e);
                success = false;
            }
        }
        // fix freak memory leak
        if (originalImageRaw != null) {
            originalImageRaw.flush();
            originalImageRaw = null;
        }
        return success;
    }
    
    public static void main(String[] args) throws Exception {
        // TODO code application logic here
        System.out.println("Aabode Image Resizer");
        Options options = new Options();
        Option imgPathOption = new Option("p", "path", true, "Image Path");
        imgPathOption.setRequired(false);
        options.addOption(imgPathOption);
        
        HelpFormatter formatter = new HelpFormatter();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Image path", options);
            System.exit(1);
            return;
        }
        
        String imgPath = null;
        if ( imgPathOption != null ){
            imgPath = cmd.getOptionValue(imgPathOption);
        } else {
            imgPath = "Default Path";
        }
        
        System.out.printf("Image Directory Path: %s", imgPath);
        
        File[] imgFolders = new File(imgPath).listFiles();

        if ( imgFolders != null ) {
            for (File folder : imgFolders) {
                File[] imgFiles = new File(folder.getAbsolutePath()).listFiles();
                if (imgFiles != null) {
                    for (File picture : imgFiles) {
                        if ( picture.getName().contains(".ori.") ) {
                            Main program = new Main();
                            program.resizeImage(picture);
                            System.out.printf("Image %s processing was finished", picture.getName());
                        }
                    }
                }
            }
        }
    }
    
}
