package cn.kennylee.banyan.openapi.sign.web;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.util.Strings;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 可重复读请求包装器：缓存原始 body，并允许拦截器通过请求属性替换为明文。
 *
 * <p>明文放在请求属性而不是包装器字段里，外层再包装（如 Spring Security）时，
 * 下游读取仍能看到替换后的内容。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    /** 缓存原始 body 的属性名。 */
    public static final String CACHED_BODY_ATTRIBUTE =
            CachedBodyRequestWrapper.class.getName() + ".CACHED_BODY";

    /** 替换为明文的属性名。 */
    public static final String PLAIN_BODY_ATTRIBUTE =
            CachedBodyRequestWrapper.class.getName() + ".PLAIN_BODY";

    /** 原始请求体。 */
    private final byte[] cachedBody;

    /**
     * 读取并缓存请求体。
     *
     * @param request 原始请求
     * @throws IOException 读取失败
     */
    public CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
        setAttribute(CACHED_BODY_ATTRIBUTE, cachedBody);
    }

    @Override
    public ServletInputStream getInputStream() {
        return new BodyInputStream(new ByteArrayInputStream(currentBody()));
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), charset()));
    }

    @Override
    public int getContentLength() {
        return currentBody().length;
    }

    @Override
    public long getContentLengthLong() {
        return currentBody().length;
    }

    /** 当前应读取的内容：优先明文属性，否则原始缓存。 */
    private byte[] currentBody() {
        Object plaintext = getAttribute(PLAIN_BODY_ATTRIBUTE);
        return plaintext instanceof byte[] bytes ? bytes : cachedBody;
    }

    /** 请求字符集，缺省 UTF-8。 */
    private Charset charset() {
        String encoding = getCharacterEncoding();
        return Strings.isBlank(encoding) ? StandardCharsets.UTF_8 : Charset.forName(encoding);
    }

    /**
     * 基于字节数组的同步输入流。
     *
     * @author kennylee
     * @since 0.1.0
     */
    private static final class BodyInputStream extends ServletInputStream {

        /** 委托流。 */
        private final ByteArrayInputStream delegate;

        /**
         * 构造输入流。
         *
         * @param delegate 字节数组流
         */
        BodyInputStream(ByteArrayInputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() {
            return delegate.read();
        }

        @Override
        public int read(byte[] bytes, int offset, int length) {
            return delegate.read(bytes, offset, length);
        }

        @Override
        public boolean isFinished() {
            return delegate.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException(
                    Messages.get(ErrorCode.OPENAPI_SIGN_ASYNC_UNSUPPORTED));
        }
    }
}
