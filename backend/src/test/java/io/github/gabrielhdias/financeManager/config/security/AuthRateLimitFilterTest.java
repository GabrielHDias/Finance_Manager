package io.github.gabrielhdias.financeManager.config.security;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;
import java.time.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.assertj.core.api.Assertions.assertThat;
class AuthRateLimitFilterTest {
    @Test void boundedWindowsIgnoreForwardedHeadersAndRecover() throws Exception {
        AtomicLong seconds = new AtomicLong(0);
        Clock clock = new Clock() {
            public ZoneId getZone() {return ZoneOffset.UTC;}
            public Clock withZone(ZoneId zone) {return this;}
            public Instant instant() {return Instant.ofEpochSecond(seconds.get());}
        };
        var filter = new AuthRateLimitFilter(JsonMapper.builder().findAndAddModules().build(), clock, 2, 1, 60, 10);
        assertThat(request(filter,"1","fake1").getStatus()).isEqualTo(200);
        assertThat(request(filter,"1","fake2").getStatus()).isEqualTo(200);
        var blocked = request(filter,"1","fake3");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");
        assertThat(blocked.getContentAsString()).contains("Too Many Requests").doesNotContain("fake3");
        assertThat(request(filter,"2","fake3").getStatus()).isEqualTo(200);
        seconds.set(60);
        assertThat(request(filter,"1","fake3").getStatus()).isEqualTo(200);
        assertThat(filter.rejectedCount()).isEqualTo(1);
    }
    private MockHttpServletResponse request(AuthRateLimitFilter filter, String ip, String forwarded) throws Exception {
        var req = new MockHttpServletRequest("POST","/auth/login");
        req.setRemoteAddr(ip); req.addHeader("X-Forwarded-For",forwarded);
        var res = new MockHttpServletResponse(); filter.doFilter(req,res,(a,b)->{}); return res;
    }
}
