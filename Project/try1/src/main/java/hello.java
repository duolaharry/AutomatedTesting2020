import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.ShrikeBTMethod;
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

import java.io.File;
import java.io.IOException;

import static com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil.dumpCG;
import static com.ibm.wala.viz.DotUtil.spawnDot;

public class hello {

    public static final String DOT_EXE = "D:\\Graphviz 2.44.1\\bin\\dot.exe";
    public final static String DOT_FILE = "E:\\学习\\自动化测试\\经典大作业\\temp.dot";
    public final static String PDF_FILE = "E:\\学习\\自动化测试\\经典大作业\\temp.pdf";
    public final static String CLASS_TARGET_PATH = "E:\\学习\\自动化测试\\经典大作业\\ClassicAutomatedTesting\\1-ALU\\target";

    public static void main(String[] args) throws IOException, InvalidClassFileException, WalaException, ClassNotFoundException, CancelException {
//        System.out.println("Hallo World!");
        AnalysisScope scope = AnalysisScopeReader.readJavaScope("scope.txt", new File("E:\\学习\\自动化测试\\try1\\src\\main\\resources\\exclusion.txt"), ClassLoader.getSystemClassLoader());
        String folderPath = CLASS_TARGET_PATH+"\\test-classes\\net\\mooctest";
        File file = new File(folderPath);
        File clazz;
        if(!file.exists()){
            System.out.println("路径不存在");
        }else{
            File[] files = file.listFiles();
            for(File f:files){
                clazz = new FileProvider().getFile(folderPath+"\\"+f.getName());
                scope.addClassFileToScope(ClassLoaderReference.Application, clazz);
            }
        }

        clazz = new FileProvider().getFile("E:\\学习\\自动化测试\\经典大作业\\ClassicAutomatedTesting\\1-ALU\\target\\classes\\net\\mooctest\\ALU.class");
        scope.addClassFileToScope(ClassLoaderReference.Application, clazz);
        // File exFile=new FileProvider().getFile("exclusion.txt");
        //AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope("zookeeper-3.3.6.jar", exFile);
        ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);
        Iterable<Entrypoint> eps = new AllApplicationEntrypoints(scope, cha);

//类层次分析
//        CHACallGraph cg = new CHACallGraph(cha);
//        cg.init(eps);
//        System.out.println(CallGraphStats.getStats(cg));
//0-CFA
        AnalysisOptions option = new AnalysisOptions(scope, eps);
        SSAPropagationCallGraphBuilder builder = Util.makeZeroCFABuilder(Language.JAVA, option, new AnalysisCacheImpl(), cha, scope);
        CallGraph cg = builder.makeCallGraph(builder.getOptions());
        dumpCG(builder.getCFAContextInterpreter(), builder.getPointerAnalysis(), cg);
// 4.遍历cg中所有的节点
        for(CGNode node: cg) {
// node中包含了很多信息，包括类加载器、方法信息等，这里只筛选出需要的信息
            if(node.getMethod() instanceof ShrikeBTMethod) {
// node.getMethod()返回一个比较泛化的IMethod实例，不能获取到我们想要的信息
// 一般地，本项目中所有和业务逻辑相关的方法都是ShrikeBTMethod对象
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
// 使用Primordial类加载器加载的类都属于Java原生类，我们一般不关心。
                if("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
// 获取声明该方法的类的内部表示
                    String classInnerName = method.getDeclaringClass().getName().toString();
// 获取方法签名
                    String signature = method.getSignature();
                    System.out.println(classInnerName + " " + signature);
                }
            } else {
                System.out.println(String.format("'%s'不是一个ShrikeBTMethod：%s",node.getMethod(), node.getMethod().getClass()));
            }
        }


        System.out.println(CallGraphStats.getStats(cg));

        dotify(cg, null, "tmp.dot",DOT_FILE, null, DOT_EXE);



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
}
