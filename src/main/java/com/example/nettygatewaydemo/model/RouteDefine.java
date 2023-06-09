package com.example.nettygatewaydemo.model;

/**
 * @description: 路由定义
 * @create: 2022/5/11 10:34:00
 * @version: 1.0
 */
public class RouteDefine {

    private String id;

    private String uri;

    private String stripPrefix;

    private String rewritePath;

    private String path;

    private Boolean exactMatch;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getStripPrefix() {
        return stripPrefix;
    }

    public void setStripPrefix(String stripPrefix) {
        this.stripPrefix = stripPrefix;
    }

    public String getRewritePath() {
        return rewritePath;
    }

    public void setRewritePath(String rewritePath) {
        this.rewritePath = rewritePath;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Boolean getExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(Boolean exactMatch) {
        this.exactMatch = exactMatch;
    }


}
