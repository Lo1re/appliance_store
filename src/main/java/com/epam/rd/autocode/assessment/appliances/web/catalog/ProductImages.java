package com.epam.rd.autocode.assessment.appliances.web.catalog;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

// Resolves known catalog models to bundled photos; product text never becomes a file path.
@Component("productImages")
public class ProductImages {
    private static final List<String> NO_PHOTO = List.of("/images/catalog/no-photo.svg");
    private static final Map<String, List<String>> GALLERIES =
            Map.ofEntries(
                    Map.entry(
                            "samsung|galaxy s10",
                            List.of(
                                    "/images/catalog/samsung-galaxy-s10/1.webp",
                                    "/images/catalog/samsung-galaxy-s10/2.webp",
                                    "/images/catalog/samsung-galaxy-s10/3.webp")),
                    Map.entry(
                            "apple|macbook pro 14 m1 pro",
                            List.of(
                                    "/images/catalog/apple-macbook-pro-14/1.webp",
                                    "/images/catalog/apple-macbook-pro-14/2.webp",
                                    "/images/catalog/apple-macbook-pro-14/3.webp")),
                    Map.entry(
                            "dell|xps 13 9300",
                            List.of(
                                    "/images/catalog/dell-xps-13-9300/1.webp",
                                    "/images/catalog/dell-xps-13-9300/2.webp",
                                    "/images/catalog/dell-xps-13-9300/3.webp")),
                    Map.entry(
                            "apple|iphone 13 pro",
                            List.of(
                                    "/images/catalog/apple-iphone-13-pro/1.webp",
                                    "/images/catalog/apple-iphone-13-pro/2.webp",
                                    "/images/catalog/apple-iphone-13-pro/3.webp")),
                    Map.entry(
                            "lenovo|yoga 920",
                            List.of(
                                    "/images/catalog/lenovo-yoga-920/1.webp",
                                    "/images/catalog/lenovo-yoga-920/2.webp",
                                    "/images/catalog/lenovo-yoga-920/3.webp")),
                    Map.entry(
                            "asus|zenbook pro duo",
                            List.of(
                                    "/images/catalog/asus-zenbook-pro-duo/1.webp",
                                    "/images/catalog/asus-zenbook-pro-duo/2.webp",
                                    "/images/catalog/asus-zenbook-pro-duo/3.webp")),
                    Map.entry(
                            "huawei|matebook x pro",
                            List.of(
                                    "/images/catalog/huawei-matebook-x-pro/1.webp",
                                    "/images/catalog/huawei-matebook-x-pro/2.webp",
                                    "/images/catalog/huawei-matebook-x-pro/3.webp")),
                    Map.entry(
                            "oppo|a57",
                            List.of(
                                    "/images/catalog/oppo-a57/1.webp",
                                    "/images/catalog/oppo-a57/2.webp",
                                    "/images/catalog/oppo-a57/3.webp")),
                    Map.entry(
                            "oppo|f19 pro plus",
                            List.of(
                                    "/images/catalog/oppo-f19-pro-plus/1.webp",
                                    "/images/catalog/oppo-f19-pro-plus/2.webp",
                                    "/images/catalog/oppo-f19-pro-plus/3.webp")),
                    Map.entry(
                            "realme|c35",
                            List.of(
                                    "/images/catalog/realme-c35/1.webp",
                                    "/images/catalog/realme-c35/2.webp",
                                    "/images/catalog/realme-c35/3.webp")),
                    Map.entry(
                            "realme|x",
                            List.of(
                                    "/images/catalog/realme-x/1.webp",
                                    "/images/catalog/realme-x/2.webp",
                                    "/images/catalog/realme-x/3.webp")),
                    Map.entry(
                            "realme|xt",
                            List.of(
                                    "/images/catalog/realme-xt/1.webp",
                                    "/images/catalog/realme-xt/2.webp",
                                    "/images/catalog/realme-xt/3.webp")),
                    Map.entry(
                            "samsung|galaxy s8",
                            List.of(
                                    "/images/catalog/samsung-galaxy-s8/1.webp",
                                    "/images/catalog/samsung-galaxy-s8/2.webp",
                                    "/images/catalog/samsung-galaxy-s8/3.webp")),
                    Map.entry(
                            "apple|ipad mini 2021",
                            List.of(
                                    "/images/catalog/apple-ipad-mini-2021/1.webp",
                                    "/images/catalog/apple-ipad-mini-2021/2.webp",
                                    "/images/catalog/apple-ipad-mini-2021/3.webp")),
                    Map.entry(
                            "samsung|galaxy tab s8 plus",
                            List.of(
                                    "/images/catalog/samsung-galaxy-tab-s8-plus/1.webp",
                                    "/images/catalog/samsung-galaxy-tab-s8-plus/2.webp",
                                    "/images/catalog/samsung-galaxy-tab-s8-plus/3.webp")),
                    Map.entry(
                            "amazon|echo plus",
                            List.of(
                                    "/images/catalog/amazon-echo-plus/1.webp",
                                    "/images/catalog/amazon-echo-plus/2.webp")),
                    Map.entry(
                            "apple|airpods",
                            List.of(
                                    "/images/catalog/apple-airpods/1.webp",
                                    "/images/catalog/apple-airpods/2.webp",
                                    "/images/catalog/apple-airpods/3.webp")),
                    Map.entry(
                            "apple|airpods max",
                            List.of("/images/catalog/apple-airpods-max-silver/1.webp")),
                    Map.entry(
                            "apple|homepod mini",
                            List.of("/images/catalog/apple-homepod-mini/1.webp")),
                    Map.entry(
                            "beats|flex wireless",
                            List.of("/images/catalog/beats-flex-wireless/1.webp")));

    public String pathFor(ApplianceResponse product) {
        return photosFor(product).get(0);
    }

    public List<String> photosFor(ApplianceResponse product) {
        String key =
                normalized(product.getManufacturerName()) + "|" + normalized(product.getModel());
        return GALLERIES.getOrDefault(key, NO_PHOTO);
    }

    private String normalized(String value) {
        return Objects.toString(value, "").strip().toLowerCase(Locale.ROOT);
    }
}
