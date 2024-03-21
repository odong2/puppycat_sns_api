echo Start

@rem ######## 스프링 부트 빌드 처리 ##################
CALL .\gradlew.bat build
echo build Complete

@rem ######## jar 파일 복사 ##########################
COPY .\build\libs\PET-SNS-API.jar D:\Docker\docker_springboot_api\conf\springboot\app\ROOT.jar
echo Copy Complete

@rem ######## 도커 경로로 이동 #######################
cd D:\Docker\docker_springboot_api
echo Move Complete

@rem ######## 스프링부트 컨테이너 재빌딩 #############
docker compose up -d --build springboot
echo Rebuild Complete