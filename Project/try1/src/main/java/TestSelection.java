import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.NodeDecorator;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil.dumpCG;
import static com.ibm.wala.viz.DotUtil.spawnDot;

/**
 * @auther ChongyuWang
 * @date 2020/11/18
 */
public class TestSelection {

    public static final String DOT_EXE = "D:\\Graphviz 2.44.1\\bin\\dot.exe";
  public final static String DOT_ROOT = "E:\\学习\\自动化测试\\AutomatedTesting2020\\Report\\";
    public final static String PDF_FILE = "E:\\学习\\自动化测试\\经典大作业\\temp.pdf";
    public final static String CLASS_TARGET_PATH = "E:\\学习\\自动化测试\\经典大作业\\ClassicAutomatedTesting\\";

    public static void main(String[] args) throws IOException, InvalidClassFileException, WalaException, ClassNotFoundException, CancelException {
//        System.out.println("Hallo World!");
        //生成dot的五个项目名字
        String[] projects = {
                "1-ALU",
                "2-DataLog",
                "3-BinaryHeap",
                "4-NextDay",
                "5-MoreTriangle"
        };
        String tmpProject = projects[4];
        String DOT_FILE = DOT_ROOT+"method-"+tmpProject+".dot";
        String PROJECT_PATH = CLASS_TARGET_PATH+tmpProject+"\\target";

        //获取命令行参数
        String typeCommand = "-m";

        // 生成分析域
        AnalysisScope scope = AnalysisScopeReader.readJavaScope(
                "scope.txt",
                new File("E:\\学习\\自动化测试\\try1\\src\\main\\resources\\exclusion.txt"),
                ClassLoader.getSystemClassLoader());
        //读取生产文件与测试文件
        //String folderPath = CLASS_TARGET_PATH+"\\test-classes\\net\\mooctest";
        String proRoot = PROJECT_PATH+"\\classes";
        ArrayList<File> proClass = new ArrayList<File>();
        int IOExcptionFlag = getFile(proRoot,proClass);
        if(IOExcptionFlag!=0){
            return;
        }
        String testRoot = PROJECT_PATH+"\\test-classes";
        ArrayList<File> testClass = new ArrayList<File>();
        IOExcptionFlag = getFile(testRoot,testClass);
        if(IOExcptionFlag!=0){
            return;
        }
//        File file = new File(folderPath);
//        File clazz;
//        if(!file.exists()){
//            System.out.println("路径不存在");
//        }else{
//            File[] files = file.listFiles();
//            for(File f:files){
//                clazz = new FileProvider().getFile(folderPath+"\\"+f.getName());
//                scope.addClassFileToScope(ClassLoaderReference.Application, clazz);
//            }
//        }
//
//        clazz = new FileProvider().getFile("E:\\学习\\自动化测试\\经典大作业\\ClassicAutomatedTesting\\1-ALU\\target\\classes\\net\\mooctest\\ALU.class");
//        scope.addClassFileToScope(ClassLoaderReference.Application, clazz);
        // File exFile=new FileProvider().getFile("exclusion.txt");
        //AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope("zookeeper-3.3.6.jar", exFile);

        //初始化测试类签名列表、生产方法列表和测试方法列表
        ArrayList<String> testSignatures = new ArrayList<String>();
        ArrayList<Method> testMethods = new ArrayList<Method>();
        ArrayList<Method> proMethods= new ArrayList<Method>();

       //单独创建test的层次图，便于后续分离
        for(File clazz:testClass){
            scope.addClassFileToScope(ClassLoaderReference.Application,clazz);
        }
        ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);
        Iterable<Entrypoint> eps = new AllApplicationEntrypoints(scope, cha);
        //类层次分析方法
        CHACallGraph cg = new CHACallGraph(cha);
        cg.init(eps);
//        System.out.println(CallGraphStats.getStats(cg));
//0-CFA
//        AnalysisOptions option = new AnalysisOptions(scope, eps);
//        SSAPropagationCallGraphBuilder builder = Util.makeZeroCFABuilder(Language.JAVA, option, new AnalysisCacheImpl(), cha, scope);
//        CallGraph cg = builder.makeCallGraph(builder.getOptions());
//        dumpCG(builder.getCFAContextInterpreter(), builder.getPointerAnalysis(), cg);
// 4.遍历cg中所有的节点
        for(CGNode node: cg) {
// node中包含了很多信息，包括类加载器、方法信息等，这里只筛选出需要的信息
            if(node.getMethod() instanceof ShrikeBTMethod) {
// node.getMethod()返回一个比较泛化的IMethod实例，不能获取到我们想要的信息
// 一般地，本项目中所有和业务逻辑相关的方法都是ShrikeBTMethod对象
                ShrikeBTMethod shrikeBTMethod = (ShrikeBTMethod) node.getMethod();
// 使用Primordial类加载器加载的类都属于Java原生类，我们一般不关心。
                if("Application".equals(shrikeBTMethod.getDeclaringClass().getClassLoader().toString())) {
// 获取声明该方法的类的内部表示
//                    String classInnerName = method.getDeclaringClass().getName().toString();
// 获取方法签名
                    testSignatures.add(shrikeBTMethod.getSignature());
//                    System.out.println(classInnerName + " " + signature);
                }
            }
        }

        //加入生产类构建整体图,并将方法分成生产类与测试类两类便于后续处理
        for(File clazz:proClass){
            scope.addClassFileToScope(ClassLoaderReference.Application,clazz);
        }
        cha = ClassHierarchyFactory.makeWithRoot(scope);
        eps = new AllApplicationEntrypoints(scope, cha);
        //类层次分析方法
        cg = new CHACallGraph(cha);
        cg.init(eps);
// 4.遍历cg中所有的节点
        for(CGNode node: cg) {
// node中包含了很多信息，包括类加载器、方法信息等，这里只筛选出需要的信息
            if(node.getMethod() instanceof ShrikeBTMethod) {
// node.getMethod()返回一个比较泛化的IMethod实例，不能获取到我们想要的信息
// 一般地，本项目中所有和业务逻辑相关的方法都是ShrikeBTMethod对象
                ShrikeBTMethod shrikeBTMethod = (ShrikeBTMethod) node.getMethod();
// 使用Primordial类加载器加载的类都属于Java原生类，我们一般不关心。
                if("Application".equals(shrikeBTMethod.getDeclaringClass().getClassLoader().toString())) {
// 获取声明该方法的类的内部表示
                    String classInnerName = shrikeBTMethod.getDeclaringClass().getName().toString();
// 获取方法签名
                    String signature = shrikeBTMethod.getSignature();
// 新建method类
                    Method method = new Method(classInnerName,signature);
//获取调用信息
                    for (CallSiteReference callSiteReference : shrikeBTMethod.getCallSites()) {
                        String callMethodSignature = callSiteReference.getDeclaredTarget().getSignature();
                        String callClassName = callSiteReference.getDeclaredTarget().getDeclaringClass().getName().toString();
                        callClassName = callClassName.split("\\$")[0];
                        method.getCalls().add(new Method(callClassName,callMethodSignature));
                    }
                    if(testSignatures.contains(signature)){
                        testMethods.add(method);
                    }
                    else{
                        proMethods.add(method);
                    }
                }
            }
        }

//        调试方法
//        for(Method method:proMethods){
//            System.out.println(method.getClassName());
//            System.out.println(method.getSignature());
//        }
//        for(Method method:testMethods){
//            System.out.println(method.getClassName());
//            System.out.println(method.getSignature());
//        }
//        System.out.println(CallGraphStats.getStats(cg));
//        dotify(cg, null, "tmp.dot",DOT_FILE, null, DOT_EXE);

//生成dot文件
        //收集所有methods便于遍历
        ArrayList<Method> allMethods = new ArrayList<Method>();
        for(Method method:proMethods){
            allMethods.add(method);
        }
        for(Method method:testMethods){
            allMethods.add(method);
        }
        //新建预备输出的唯一依赖信息
        ArrayList<String> outDot = new ArrayList<String>();
        File file = new File(DOT_FILE);
        FileOutputStream fileOutputStream = null;
        String tmpOutput = "";
        try {
            fileOutputStream = new FileOutputStream(file,true);
            if(typeCommand.equals("-m")){
                tmpOutput = "digraph _method {\n";
                for(int i=0;i<tmpOutput.length();i++){
                    fileOutputStream.write(tmpOutput.charAt(i));
                }
            }
            else if(typeCommand.equals("-c")){
                tmpOutput = "digraph _class {\n";
                for(int i=0;i<tmpOutput.length();i++){
                    fileOutputStream.write(tmpOutput.charAt(i));
                }
            }
            for (Method method : allMethods) {
                for (Method callMethod:method.getCalls()) {
                    if(typeCommand.equals("-m")) {
                        String callDotSignature = callMethod.getSignature();
                        String dotSignature = method.getSignature();
                        if (callDotSignature.charAt(0) == dotSignature.charAt(0)) {
                            tmpOutput = "\"" + callDotSignature + "\"" + " " + "->" + " " + "\"" + dotSignature + "\"" + ";\n";
                            if (!outDot.contains(tmpOutput)) {
                                outDot.add(tmpOutput);
                            }
                        }
                    }
                    else if(typeCommand.equals("-c")){
                        String callDotClassName = callMethod.getClassName();
                        String dotClassName = method.getClassName();
                        if (callDotClassName.charAt(1) == dotClassName.charAt(1)) {
                            tmpOutput = "\"" + callDotClassName + "\"" + " " + "->" + " " + "\"" + dotClassName + "\"" + ";\n";
                            if(!outDot.contains(tmpOutput)) {
                                outDot.add(tmpOutput);
                            }
                        }
                    }
                }
            }
            Collections.sort(outDot);
            for(String s:outDot){
                for(int i=0;i<s.length();i++){
                    fileOutputStream.write(s.charAt(i));
                }
            }
            fileOutputStream.write('}');
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static <T> void dotify(Graph<T> g, NodeDecorator<T> labels, String title, String dotFile, String outputFile, String dotExe)
            throws WalaException {
        if (g == null) {
            throw new IllegalArgumentException("g is null");
        }
        File f = DotUtil.writeDotFile(g, labels, title, dotFile);
        if (dotExe != null && outputFile != null) { //如果输出pdf不为空，那么输出pdf文件
            spawnDot(dotExe, outputFile, f);
        }
    }

    public static int getFile(String dir,ArrayList<File> classes) {
        File root = new File(dir);
        if(!root.exists()){
            System.out.println("路径不存在");
            return -1;
        }
        if(root.listFiles()!=null) {
            for (File file : root.listFiles()) {
                if (!file.isDirectory()) {
                    classes.add(file);
                }
                else {
                    getFile(file.getAbsolutePath(), classes);
                }
            }
        }
        return 0;
    }


}
