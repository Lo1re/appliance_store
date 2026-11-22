package com.epam.rd.autocode.assessment.appliances.security.token;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class DecoderLengthFailureTest {@Test void aFaultyDecoderCannotBypassTheExplicitByteLengthGuard(){OpaqueTokens.generate();var decoder=mock(Base64.Decoder.class);String token="A".repeat(43);when(decoder.decode(token)).thenReturn(new byte[31]);try(var base64=mockStatic(Base64.class,CALLS_REAL_METHODS)){base64.when(Base64::getUrlDecoder).thenReturn(decoder);assertThat(OpaqueTokens.isCanonical(token)).isFalse();}}}
