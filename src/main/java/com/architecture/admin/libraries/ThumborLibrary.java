package com.architecture.admin.libraries;

import com.squareup.pollexor.Thumbor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

@Component
@Data
public class ThumborLibrary {

    private final S3Library s3Library;

    @Value("${cloud.aws.s3.img.url}")
    private String domain;

    @Value("${thumbor.key}")
    private String thumborKey;

    public List<HashMap<String, Object>> getCFUrl(List<HashMap<String,Object>> uploadResponse, int width) {

        try {
            for(HashMap<String, Object> map : uploadResponse) {
                // 이미지 full url
                String fullUrl = s3Library.getUploadedFullUrl(map.get("fileUrl").toString());

                URL url = new URL(fullUrl);
                BufferedImage image = ImageIO.read(url);

                int height2 = width * image.getHeight() / image.getWidth();

                Thumbor thumbor = Thumbor.create(domain, thumborKey);

                // thumbor full url
                String thumborUrl = thumbor.buildImage(fullUrl).resize(width, height2).toUrl();
                URL path = new URL(thumborUrl);

                // thumbor domain 제외하고 path만 저장
                map.put("fileUrl", path.getPath());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return uploadResponse;
    }

    /*
     * domain 포함하여 full url 리턴
     * @param string fileUrl  기 db 저장된 url (예시: /store/26/55f24d86-3ef3-4dc1-a4d1-08afd03d4674.jpg)
     */
    public String getUploadedFullUrl(String fileUrl) {
        try {
            return domain + fileUrl;
        } catch (StringIndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TO get uploaded file(" + fileUrl + ") full url is error.");
        }
    }

}
