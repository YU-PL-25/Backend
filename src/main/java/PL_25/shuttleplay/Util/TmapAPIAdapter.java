package PL_25.shuttleplay.Util;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TmapAPIAdapter {

    public String generateMapUrl(String keyword) {
        // 예: 위치를 기반으로 지도 이미지 URL 생성
        return "https://map.tmap.co.kr/search?query=" + keyword;
    }

    public List<String> getNearbyCourts(String keyword) {
        // TMAP Open API 호출 예시
        // 실제로는 RestTemplate 등을 써서 HTTP 요청함
        return List.of("서울체육관", "송파스포츠센터", "강남구민체육관");
    }
}

