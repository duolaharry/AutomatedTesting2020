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
import java.util.Iterator;

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
        //生成dot的五个项目名字
        String[] projects = {
                "1-ALU",
                "2-DataLog",
                "3-BinaryHeap",
                "4-NextDay",
                "5-MoreTriangle",
                "0-CMD"
        };
        String tmpProject = projects[4];
        String DOT_FILE = DOT_ROOT+"class-"+"tmp"+".dot";

        //获取命令行参数
        String typeCommand = "-c";
        String PROJECT_PATH = CLASS_TARGET_PATH+tmpProject+"\\target";
        String changeDir = CLASS_TARGET_PATH+tmpProject+"\\change_info.txt";
        String outDir = "./";
        if(typeCommand.equals("-c")){
            outDir += "selection-class.txt";
        }
        else{
            outDir += "selection-method.txt";
        }

    // 生成分析域
    AnalysisScope scope =
        AnalysisScopeReader.readJavaScope(
            "src\\main\\resources\\scope.txt",
            new File("src\\main\\resources\\exclusion.txt"),
            ClassLoader.getSystemClassLoader());
        //读取生产文件与测试文件
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
                        if(callClassName.charAt(1) == classInnerName.charAt(1)){
                            method.getCallers().add(new Method(callClassName,callMethodSignature));
                        }
                    }
                    Iterator preNodes = cg.getPredNodes(node);
                    for (Iterator it = preNodes; it.hasNext(); ) {
                        CGNode preNode = (CGNode)it.next();
                        if(preNode.getMethod() instanceof ShrikeBTMethod){
                            ShrikeBTMethod preShrikeBTMethod = (ShrikeBTMethod) preNode.getMethod();
                            if("Application".equals(preShrikeBTMethod.getDeclaringClass().getClassLoader().toString())) {
                                String preClassInnerName = preShrikeBTMethod.getDeclaringClass().getName().toString();
                                String preSignature = preShrikeBTMethod.getSignature();
                                Method preMethod = new Method(preClassInnerName,preSignature);
                                method.getCallees().add(preMethod);
                            }
                        }
                    }
                        if(testSignatures.contains(signature)){
                            if(method.getCallees().size()!=0||method.getCallers().size()!=0){
                                testMethods.add(method);
                            }
                        }
                        else{
                            proMethods.add(method);
                        }
                    }
                }
            }

        //收集所有methods便于遍历
        ArrayList<Method> allMethods = new ArrayList<Method>();
        for(Method method:proMethods){
            allMethods.add(method);
        }
        for(Method method:testMethods){
            allMethods.add(method);
        }

//生成dot文件
        //新建预备输出的唯一依赖信息
        ArrayList<String> outDot = new ArrayList<String>();
        File file = new File(DOT_FILE);
        FileOutputStream fileOutputStream = null;
        String tmpOutput = "";
        String callDotSignature = "";
        String dotSignature = "";
        String callDotClassName = "";
        String dotClassName = "";
        try {
            fileOutputStream = new FileOutputStream(file,false);
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
                for (Method callMethod:method.getCallers()) {
                    if(typeCommand.equals("-m")) {
                        callDotSignature = callMethod.getSignature();
                        dotSignature = method.getSignature();
                        tmpOutput = "\"" + callDotSignature + "\"" + " " + "->" + " " + "\"" + dotSignature + "\"" + ";\n";
                        if (!outDot.contains(tmpOutput)) {
                            outDot.add(tmpOutput);
                        }
                    }
                    else if(typeCommand.equals("-c")){
                        callDotClassName = callMethod.getClassName();
                        dotClassName = method.getClassName();
                        tmpOutput = "\"" + callDotClassName + "\"" + " " + "->" + " " + "\"" + dotClassName + "\"" + ";\n";
                        if(!outDot.contains(tmpOutput)) {
                            outDot.add(tmpOutput);
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

//读取changeinfo
        BufferedReader bufferedReader = null;
        ArrayList<String> changeInfos = new ArrayList<>();
        bufferedReader = new BufferedReader(new FileReader(changeDir));
        String changeInfo = bufferedReader.readLine();
        while(changeInfo!=null){
            changeInfo = changeInfo.split(" ")[1];
            changeInfos.add(changeInfo);
            changeInfo = bufferedReader.readLine();
        }
        //得到输出列表并输出
        ArrayList<String> targetList = searchTestMethod(changeInfos,allMethods,testMethods,testSignatures,typeCommand);
        File nfile = new File(outDir);
        FileOutputStream nfileOutputStream = null;
        nfileOutputStream = new FileOutputStream(nfile,false);
        for(String outString:targetList){
            for(int i=0;i<outString.length();i++){
                nfileOutputStream.write(outString.charAt(i));
            }
            nfileOutputStream.write('\n');
        }
        nfileOutputStream.close();

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

    public static ArrayList<String> searchTestMethod(ArrayList<String> changeInfos,ArrayList<Method> allMethods,ArrayList<Method> testMethods,ArrayList<String> testSignatures,String typeCommand){
        ArrayList<String> targetList = new ArrayList<>();
        ArrayList<String> changeClasses = new ArrayList<>();
        String changeInfo = "";
        Method tmpMethod = new Method("","");
        while(changeInfos.size()!=0){
            changeInfo = changeInfos.remove(0);
            for(Method method:allMethods){
                if(changeInfo.equals(method.getSignature())){
                    tmpMethod = method;
                    if(!changeClasses.contains(tmpMethod.getClassName())){
                        changeClasses.add(tmpMethod.getClassName());
                    }
                    if(tmpMethod.getCallees().size()!=0){
                        for(Method tmpChangeInfo:tmpMethod.getCallees()){
                            if(!changeInfos.contains(tmpChangeInfo.getSignature())){
                                changeInfos.add(tmpChangeInfo.getSignature());
                            }
                        }
                    }
                    if( (testSignatures.contains(tmpMethod.getSignature())) && (!targetList.contains(tmpMethod.getClassName()+' '+tmpMethod.getSignature())) ){
                        targetList.add(tmpMethod.getClassName()+' '+tmpMethod.getSignature());
                    }
                    break;
                }
            }
        }
        if(typeCommand.equals("-m")){
            Collections.sort(targetList);
            return targetList;
        }
        String changeClass = "";
        while(changeClasses.size()!=0){
            changeClass = changeClasses.remove(0);
            for(Method tempMethod:testMethods){
                if(tempMethod.getCallers().size()!=0){
                    for(Method method:tempMethod.getCallers()){
                        if( (method.getClassName().equals(changeClass)) && (!targetList.contains(tempMethod.getClassName()+' '+tempMethod.getSignature())) ){
                            targetList.add(tempMethod.getClassName()+' '+tempMethod.getSignature());
                        }
                    }
                }
            }
        }
        Collections.sort(targetList);
        return targetList;
    }




}
