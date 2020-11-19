import java.util.ArrayList;

/**
 * @auther ChongyuWang
 * @date 2020/11/18
 */
public class Method {
    private final String className;
    private final String signature;
    private final ArrayList<Method> callers = new ArrayList<Method>();
    private final ArrayList<Method> callees = new ArrayList<Method>();
    public Method(String className,String signature){
        this.signature = signature;
        this.className = className;
    }
    public String getClassName() {
        return className;
    }
    public String getSignature() { return signature; }
    public ArrayList<Method> getCallers() {
        return callers;
    }
    public ArrayList<Method> getCallees() { return callees;}
}
