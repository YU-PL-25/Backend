package PL_25.shuttleplay.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/****************************************
 * 프론트 + 백 병합 시 React 라우터 관리용 컨트롤러
 * ***************************************/

@Controller
public class WebPageController {

    // 확장자가 없는 요청이 들어올 시 모두 index.html 으로 리다이렉트되게 함
    @RequestMapping(value = { "/", "/{path:[^\\.]*}" })
    public String redirect() {
        return "forward:/index.html";
    }
}