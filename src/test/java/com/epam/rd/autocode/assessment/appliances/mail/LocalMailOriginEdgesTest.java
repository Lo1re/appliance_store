package com.epam.rd.autocode.assessment.appliances.mail;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class LocalMailOriginEdgesTest {
    @Test void rejectsEveryUntrustedOriginComponent(){
        for(String uri:new String[]{"ftp://localhost","http://remote.example","http://localhost?q=1","http://localhost#fragment","http://user@localhost","http://localhost/private"})
            assertThatThrownBy(()->new SecurityMailLinkFactory(uri)).as(uri).isInstanceOf(IllegalArgumentException.class);
        assertThat(new SecurityMailLinkFactory("https://127.0.0.1:8443/").passwordReset("opaque").toString()).isEqualTo("https://127.0.0.1:8443/password/reset?token=opaque");
        assertThat(new SecurityMailLinkFactory("http://localhost").registration("opaque").toString()).startsWith("http://localhost/register/verify");
    }
}
