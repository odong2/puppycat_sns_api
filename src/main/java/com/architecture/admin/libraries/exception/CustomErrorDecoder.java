package com.architecture.admin.libraries.exception;

import feign.Response;
import feign.codec.ErrorDecoder;

public class CustomErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            throw new CustomException(CustomError.NOT_FOUND_URL);
        }

        if (response.status() >= 500) {
            throw new CustomException(CustomError.FIVE_HUNDRED_OVER_SERVER_ERROR);
        }

        if (response.status() >= 400) {
            throw new CustomException(CustomError.FOUR_HUNDRED_OVER_SERVER_ERROR);
        }

        throw new CustomException(CustomError.SERVER_ERROR);
    }
}