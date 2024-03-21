package com.architecture.admin.libraries;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/*****************************************************
 * S3 라이브러리
 ****************************************************/
@Service
@RequiredArgsConstructor
public class S3Library {
    @Value("${env.server}")
    private String server; // local/dev

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.img.url}")
    private String cfImgURL;

    // 이미지 FULL URL (도메인 포함)
    private String fullUrl;

    private final AmazonS3 amazonS3;
    private final int UPLOAD_MAX_SIZE = 16740480; // 16MB
    private final List<String> IMAGE_EXTENSION_LIST = Arrays.asList("jpg", "jpeg", "png");

    public List<String> uploadFile(List<MultipartFile> multipartFile) {
        List<String> fileNameList = new ArrayList<>();

        // forEach 구문을 통해 multipartFile로 넘어온 파일들 하나씩 fileNameList에 추가
        multipartFile.forEach(file -> {
            String fileName = createFileName(file.getOriginalFilename());
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(file.getSize());
            objectMetadata.setContentType(file.getContentType());

            try (InputStream inputStream = file.getInputStream()) {
                amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata)
                        .withCannedAcl(CannedAccessControlList.PublicRead));
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fail File Upload");
            }

            // 도메인 없이 설정
            // String fileUrl = "https://" + bucket.toString() + "/" + fileName;
            String fileUrl = "/" + fileName;

            //fileNameList.add(fileName);
            fileNameList.add(fileUrl);
        });

        return fileNameList;
    }

    public List<String> uploadFile(List<MultipartFile> multipartFile, String dirName) {
        List<String> fileNameList = new ArrayList<>();

        // forEach 구문을 통해 multipartFile로 넘어온 파일들 하나씩 fileNameList에 추가
        multipartFile.forEach(file -> {
            String fileName = dirName + "/" + createFileName(file.getOriginalFilename());
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(file.getSize());
            objectMetadata.setContentType(file.getContentType());

            try (InputStream inputStream = file.getInputStream()) {
                amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata)
                        .withCannedAcl(CannedAccessControlList.PublicRead));
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fail File Upload.");
            }

            String fileUrl = "https://" + bucket.toString() + "/" + fileName;

            //fileNameList.add(fileName);
            fileNameList.add(fileUrl);
        });

        return fileNameList;
    }

    // 파일 정보 list map 리턴 추가
    public List<HashMap<String,Object>> uploadFileNew(List<MultipartFile> multipartFile) {
        List<HashMap<String,Object>> fileUploadList = new ArrayList<>();

        // forEach 구문을 통해 multipartFile로 넘어온 파일들 하나씩 fileUploadList에 추가
        multipartFile.forEach(file -> {

            if (!file.isEmpty()) {// 있을 경우
                HashMap<String, Object> fileInfoList = new HashMap<>();

                String fileName = createFileName(file.getOriginalFilename());
                ObjectMetadata objectMetadata = new ObjectMetadata();
                objectMetadata.setContentLength(file.getSize());
                objectMetadata.setContentType(file.getContentType());

                try (InputStream inputStream = file.getInputStream()) {
                    amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata)
                            .withCannedAcl(CannedAccessControlList.PublicRead));
                } catch (IOException e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fail File Upload.");
                }

                fileInfoList.put("fileUrl", "/" + fileName);// url 설정 정보(도메인 없이 설정)
                fileInfoList.put("newFileName", fileName);// 업로드 시 변경된 파일 이름
                fileInfoList.put("orgFileName", file.getOriginalFilename());// 파일 원 이름
                fileInfoList.put("fileSize", file.getSize());// 파일 사이즈
                fileInfoList.put("fileExtension", file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")));// 파일 확장자 => getFileExtension() 메소드 미사용은 exception 때문에,,
                fileInfoList.put("fileContentType", file.getContentType());// 파일 ContentType

                fileUploadList.add(fileInfoList);// 리턴할 정보에 입력처리
            }
        });

        return fileUploadList;
    }

    // 파일 정보 list map 리턴 추가
    public List<HashMap<String,Object>> uploadFileNew(List<MultipartFile> multipartFile, String dirName) {
        List<HashMap<String,Object>> fileUploadList = new ArrayList<>();

        // forEach 구문을 통해 multipartFile로 넘어온 파일들 하나씩 fileUploadList에 추가
        multipartFile.forEach(file -> {

            if (!file.isEmpty()) {// 있을 경우
                HashMap<String, Object> fileInfoList = new HashMap<>();

                String fileName = server + "/" + dirName + "/" + createFileName(file.getOriginalFilename());
                ObjectMetadata objectMetadata = new ObjectMetadata();
                objectMetadata.setContentLength(file.getSize());
                objectMetadata.setContentType(file.getContentType());

                try (InputStream inputStream = file.getInputStream()) {
                    amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata)
                            .withCannedAcl(CannedAccessControlList.PublicRead));
                } catch (IOException e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fail File Upload.");
                }

                fileInfoList.put("fileUrl", "/" + fileName);// url 설정 정보(도메인 없이 설정)
                fileInfoList.put("newFileName", fileName);// 업로드 시 변경된 파일 이름
                fileInfoList.put("orgFileName", file.getOriginalFilename());// 파일 원 이름
                fileInfoList.put("fileSize", file.getSize());// 파일 사이즈
                fileInfoList.put("fileExtension", file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")));// 파일 확장자 => getFileExtension() 메소드 미사용은 exception 때문에,,
                fileInfoList.put("fileContentType", file.getContentType());// 파일 ContentType

                BufferedImage image = null;
                try {
                    image = ImageIO.read(file.getInputStream());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                fileInfoList.put("imgWidth", image.getWidth());   // 이미지 가로 size
                fileInfoList.put("imgHeight", image.getHeight()); // 이미지 세로 size

                fileUploadList.add(fileInfoList);// 리턴할 정보에 입력처리
            }
        });

        return fileUploadList;
    }

    public void deleteFile(String fileName) {
        boolean isExistObject = amazonS3.doesObjectExist(bucket, fileName);
        if (Boolean.TRUE.equals(isExistObject)) {
            amazonS3.deleteObject(new DeleteObjectRequest(bucket, fileName));
        }
    }

    /*
     * 동일 버킷의 파일 복사
     * !! 복사 완료 후 복사대상 파일 삭제처리(tmp 파일임으로) - data/tmpUpload(cloud.aws.s3.tmpFolder) 폴더 내 파일은 수정날짜 기준 24시간 이후 자동 삭제 추가 설정 예정
     * @param string sourceFileName  S3에 저장되어 있는 복사대상 파일 이름 (예시:cce2a417-b4fa-4706-ad91-f956134f895b.jpg)
     * @param string sourcePath  S3에 저장되어 있는 복사대상 파일의 경로 (S3도메인 및 파일 이름을 제외한 파일의 경로, 예시:data/tmpUpload) => '/'가 앞뒤에 없음 주의!!
     * @param string destinationPath  S3에 복사할 S3 파일의 경로 (S3도메인을 제외한 파일의 경로, 예시: store/숫자(idx)) => '/'가 앞뒤에 없음 주의!!
     */
    public void copyFile(String sourceFileName, String sourcePath, String destinationPath) {

        String sourceKey = sourcePath + "/" + sourceFileName;// 복사대상 파일의 S3 KEY
        String destinationKey = destinationPath + "/" + sourceFileName;// 복사할 S3 KEY

        try {
            boolean isExistObject = amazonS3.doesObjectExist(bucket, sourceKey);// 복사대상 파일 존재여부 체크
            if (Boolean.TRUE.equals(isExistObject)) {
                //Copy 객체 생성
                CopyObjectRequest copyObjRequest = new CopyObjectRequest(
                        bucket,
                        sourceKey,
                        bucket,
                        destinationKey
                );
                //Copy
                amazonS3.copyObject(copyObjRequest);

                boolean isExistDestinationObject = amazonS3.doesObjectExist(bucket, destinationKey);// 복사 완료한 파일 존재여부 체크
                if (Boolean.TRUE.equals(isExistDestinationObject)) {
                    //deleteFile(sourceKey);// 복사대상 파일 삭제처리
                }
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fail File Copy.");
        }
    }

    /*
     * 기 저장된 url에서 업로드 된 파일이름만 추출하여 리턴
     * @param string fileUrl  기 db 저장된 url 또는 fullUrl (예시: /data/tmpUpload/55f24d86-3ef3-4dc1-a4d1-08afd03d4674.jpg 또는 //dev-imgs.devlabs.co.kr/data/tmpUpload/90a19f7b-ca87-422e-a31e-b65b28d8f5c7.jpg)
     * @param string path  기 db 저장된 path (예시: store/26) => '/'가 앞뒤에 없음 주의!!
     */
    public String getUploadedFileName(String fileUrl, String path) {
        try {
            String tmpFileUrl = fileUrl.replace("//" + cfImgURL, "");// 도메인 제거
            return tmpFileUrl.replace("/" + path + "/", "");
        } catch (StringIndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TO get uploaded file(" + fileUrl + ") name is wrong url.");
        }
    }

    /*
     * cf domain 포함하여 full url 리턴
     * @param string fileUrl  기 db 저장된 url (예시: /store/26/55f24d86-3ef3-4dc1-a4d1-08afd03d4674.jpg)
     */
    public String getUploadedFullUrl(String fileUrl) {
        try {
            return "https://" + cfImgURL + fileUrl;
        } catch (StringIndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TO get uploaded file(" + fileUrl + ") full url is error.");
        }
    }

    private String createFileName(String fileName) { // 먼저 파일 업로드 시, 파일명을 난수화하기 위해 random으로 돌립니다.
        return UUID.randomUUID().toString().concat(getFileExtension(fileName));
    }

    private String getFileExtension(String fileName) { // file 형식이 잘못된 경우를 확인하기 위해 만들어진 로직이며, 파일 타입과 상관없이 업로드할 수 있게 하기 위해 .의 존재 유무만 판단하였습니다.
        try {
            return fileName.substring(fileName.lastIndexOf("."));
        } catch (StringIndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file(" + fileName + ") is malformed.");
        }
    }

    /*
     * 이미지 유효성 체크
     * contentType 체크 & 확장자 체크 & 이미지 사이즈 체크
     * @param uploadFiles
     */
    public void checkUploadFiles(List<MultipartFile> uploadFiles) {

        for (MultipartFile multipartFile : uploadFiles) {

            /** 이미지 여부 체크 **/
            if(multipartFile.isEmpty()) {
                throw new CustomException(CustomError.IMAGE_NOT_EXIST_ERROR); // 이미지가 존재하지 않습니다.
            }

            /** 컨텐트 타입 체크 **/
            String contentType = multipartFile.getContentType();
            contentType = contentType.substring(0, contentType.indexOf("/")); // image

            // 확장자 체크
            if (ObjectUtils.isEmpty(contentType) || !contentType.equals("image")) {
                throw new CustomException(CustomError.IMAGE_EXTENSION_ERROR); // 허용하지 않는 확장자를 가진 파일입니다.
            }

            String fileName = multipartFile.getOriginalFilename();

            /** 확장자 체크 **/
            if (fileName != null) {
                String extention = fileName.substring(fileName.lastIndexOf(".") + 1);

                if (!IMAGE_EXTENSION_LIST.contains(extention.toLowerCase())) { // 허용하지 않는 확장자
                    throw new CustomException(CustomError.IMAGE_EXTENSION_ERROR); // 허용하지 않는 확장자를 가진 파일입니다.
                }
            }

            /** 이미지 용량 체크 **/
            long size = multipartFile.getSize();

            // 이미지 크기 16MB 보다 크면[2048 * 2730 / (24/8) = 16MB]
            if (size > UPLOAD_MAX_SIZE) {
                throw new CustomException(CustomError.IMAGE_SIZE_LIMIT_OVER);     // 등록할 이미지 용량이 너무 큽니다.
            }

        }
    }
}
