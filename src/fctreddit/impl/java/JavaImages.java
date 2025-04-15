package fctreddit.impl.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import fctreddit.api.User;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;

public class JavaImages implements Image {

    private final String baseDir;
    private JavaUsers users;
    //private JavaContent content;

    public JavaImages() {
        baseDir = System.getProperty("user.dir") + File.separator + "images" + File.separator;
    }

    public void setUsers(JavaUsers users) {
        this.users = users;
    }

    
    @Override
    public Result<String> createImage(String userId, byte[] imageContents, String password) {
        Result<User> resUser = users.getUser(userId, password);
        if (!resUser.isOK()) {
            return Result.error(resUser.error());
        }
        if (imageContents.length == 0) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        Path userDir = Paths.get(baseDir, userId);
        
        if (!Files.exists(userDir)) {
            try {
                Files.createDirectories(userDir);
            } catch (IOException e) {
                return Result.error(Result.ErrorCode.BAD_REQUEST);
            }
        }

        String imageName = UUID.randomUUID().toString() + ".png";
        Path imagePath = userDir.resolve(imageName);

        try {
            Files.write(imagePath, imageContents);
        } catch (IOException e) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        String imageUri = imagePath.toUri().toString();
        
        return Result.ok(imageUri); //perguntar se é suposto ser o uri absoluto ou só esta situation
    }

    @Override
    public Result<byte[]> getImage(String userId, String imageId) {
        Path imagePath = Paths.get(baseDir, userId, imageId);
        if (!Files.exists(imagePath)) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        try {
            byte[] imageData = Files.readAllBytes(imagePath);
            return Result.ok(imageData);
        } catch (IOException e) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
    }

    @Override
    public Result<Void> deleteImage(String userId, String imageId, String password) {
        if(password == null){
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        Result<User> resUser = users.getUser(userId, password);
        if(!resUser.isOK()){
            return Result.error(resUser.error());
        }
        Path imagePath = Paths.get(baseDir, userId, imageId);
        if (!Files.exists(imagePath)) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        try {
            Files.delete(imagePath);
            return Result.ok(null);
        } catch (IOException e) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
    }
    
}
