package com.epam.rd.autocode.assessment.appliances.web.error;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.servlet.ModelAndView;

public final class MvcErrorViews {
    private MvcErrorViews() {}

    public static ModelAndView render(HttpStatusCode status) {
        String template =
                switch (status.value()) {
                    case 400, 401, 403, 404, 405, 409, 429 -> "error/" + status.value();
                    default -> status.is4xxClientError() ? "error/4xx" : "error/5xx";
                };
        var view = new ModelAndView(template);
        view.setStatus(status);
        view.addObject("status", status.value());
        return view;
    }
}
