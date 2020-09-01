package edu.hawaii.its.api.type;

public class GroupPath {
    String path;
    String parentPath;
    String name;

    public GroupPath() {
    }

    public GroupPath(String path) {
        this.path = path;
        setParentPath(makeParentPath());
        setName(makeName());
    }

    public String getName() {
        return name;
    }

    public String getParentPath() {
        return parentPath;
    }

    public String getPath() {
        return path;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String toString() {
        return "path: " + path + "; " +
                "parentPath: " + parentPath + "; " +
                "name: " + name + ";";
    }

    private String makeParentPath() {
        if (null == path) {
            return "";
        }
        return path.substring(0, path.lastIndexOf(":"));
    }

    private String makeName() {
        if (null == parentPath) {
            return "";
        }
        return parentPath.substring(parentPath.lastIndexOf(":") + 1, parentPath.length());
    }

}
