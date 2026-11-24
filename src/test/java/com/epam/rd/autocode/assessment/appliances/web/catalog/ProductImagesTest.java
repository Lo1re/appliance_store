package com.epam.rd.autocode.assessment.appliances.web.catalog;

import static org.assertj.core.api.Assertions.*;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProductImagesTest {
    private final ProductImages pictures = new ProductImages();

    @ParameterizedTest
    @CsvSource({
        "Samsung,Galaxy S10,/images/catalog/samsung-galaxy-s10/1.webp",
        "Apple,MacBook Pro 14 M1 Pro,/images/catalog/apple-macbook-pro-14/1.webp",
        "Dell,XPS 13 9300,/images/catalog/dell-xps-13-9300/1.webp",
        "Apple,iPhone 13 Pro,/images/catalog/apple-iphone-13-pro/1.webp",
        "Lenovo,Yoga 920,/images/catalog/lenovo-yoga-920/1.webp",
        "Asus,Zenbook Pro Duo,/images/catalog/asus-zenbook-pro-duo/1.webp",
        "Huawei,MateBook X Pro,/images/catalog/huawei-matebook-x-pro/1.webp",
        "Oppo,A57,/images/catalog/oppo-a57/1.webp",
        "Oppo,F19 Pro Plus,/images/catalog/oppo-f19-pro-plus/1.webp",
        "Realme,C35,/images/catalog/realme-c35/1.webp",
        "Realme,X,/images/catalog/realme-x/1.webp",
        "Realme,XT,/images/catalog/realme-xt/1.webp",
        "Samsung,Galaxy S8,/images/catalog/samsung-galaxy-s8/1.webp",
        "Apple,iPad mini 2021,/images/catalog/apple-ipad-mini-2021/1.webp",
        "Samsung,Galaxy Tab S8 Plus,/images/catalog/samsung-galaxy-tab-s8-plus/1.webp",
        "Amazon,Echo Plus,/images/catalog/amazon-echo-plus/1.webp",
        "Apple,AirPods,/images/catalog/apple-airpods/1.webp",
        "Apple,AirPods Max,/images/catalog/apple-airpods-max-silver/1.webp",
        "Apple,HomePod mini,/images/catalog/apple-homepod-mini/1.webp",
        "Beats,Flex Wireless,/images/catalog/beats-flex-wireless/1.webp"
    })
    void knownModelsHaveMatchingLocalPhotographs(String brand, String model, String photo) {
        var product = new ApplianceResponse();
        product.setManufacturerName(brand);
        product.setModel(model);
        assertThat(pictures.pathFor(product)).isEqualTo(photo);
        for (String image : pictures.photosFor(product)) {
            assertThat(getClass().getResource("/static" + image)).isNotNull();
        }
    }

    @Test
    void unknownAndUntrustedModelsUseANeutralPlaceholder() {
        var product = new ApplianceResponse();
        assertThat(pictures.pathFor(product)).isEqualTo("/images/catalog/no-photo.svg");
        product.setModel("../../private");
        product.setManufacturerName("https://untrusted.example");
        assertThat(pictures.photosFor(product)).containsExactly("/images/catalog/no-photo.svg");
    }
}
