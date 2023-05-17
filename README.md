# netty-gateway-demo

基于netty实现的简单API网关，支持一下功能：

- 1.根据配置路由
- 2.支持websocket代理
- 3.对multipart/form-data请求使用chunked处理，以支持大文件传输
- 4.支持对响应数据使用Transfer-Encoding: chunked处理