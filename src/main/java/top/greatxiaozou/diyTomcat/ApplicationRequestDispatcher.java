package top.greatxiaozou.diyTomcat;

import lombok.Getter;
import lombok.Setter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * 用于实现服务端跳转的分发类。
 */
@Getter
@Setter
public class ApplicationRequestDispatcher implements RequestDispatcher {
    private String uri;

    public ApplicationRequestDispatcher(String uri) {
        if (!uri.startsWith("/")){
            uri = "/" + uri;
        }
        this.uri = uri;
    }

    @Override
    public void forward(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        Request request = (Request) servletRequest;
        Response response = ((Response) servletResponse);
        //重新设置request中的uri
        request.setUri(this.uri);

        HttpProcessor processor = new HttpProcessor();
        processor.executor(request.getSocket(),request,response);
        request.setForwarded(true);
    }

    @Override
    public void include(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {

    }
}
