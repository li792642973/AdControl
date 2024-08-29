package org.ad.processor;

import androidx.annotation.Keep;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.ad.annotation.Adc;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
public class AnnotationProcessor extends AbstractProcessor {
    final String PREFIX = "org.ad.process";
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        // 当前的注解处理类能够处理哪些注解类型
        Set<String> set = new HashSet<>();
        set.add(Adc.class.getName());
        return set;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        // 设置为能够支持最新版本
        return SourceVersion.latestSupported();
    }

    // ProcessingEnvironment 是注解处理器的上下文环境，提供了在注解处理器执行过程中访问所需工具和信息的方法
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        return processControl(set, roundEnvironment);
    }

    // RoundEnvironment 提供了每一轮注解处理时的上下文环境。在注解处理器的 process 方法中使用，用来访问当前这一轮中被注解处理器处理的元素。
    private boolean processControl(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(Adc.class);
        if (elements.isEmpty()) {
            // 因为process方法会多次调用，所以使用需要将无用的调用移除掉
            return false;
        }

        // 使用AdControl注解的map集合
        HashMap<String, String> controlMap = new HashMap<>();
        // 重复的Key
        StringBuilder repeatBuilder = new StringBuilder();
        for (Element element : elements) {
            if (element.getKind() != ElementKind.CLASS) {
                // 只过滤class类文件
                continue;
            }

            Adc control = element.getAnnotation(Adc.class);
            TypeElement typeElement = (TypeElement) element;
            if (controlMap.containsKey(control.name())) {
                if (repeatBuilder.length() > 0) {
                    repeatBuilder.append(",");
                }
                repeatBuilder.append(control.name());
                continue;
            }

            String className = addControlField(control, typeElement);
            controlMap.put(control.name(), className);
        }

        if (repeatBuilder.length() > 0) {
            // control带有重复性，提示一下
            print("重复的广告控制器，广告商名称: " + repeatBuilder);
            return false;
        }

        CodeBlock.Builder builder = CodeBlock.builder();
        for (Map.Entry<String, String> entry : controlMap.entrySet()) {
            builder.addStatement("adControls.put(\"" + entry.getKey() + "\", " + entry.getValue() + ".class)");
        }
        // 将注册的类全部写到常量中
        FieldSpec fc = FieldSpec.builder(HashMap.class, "adControls", Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .addAnnotation(AnnotationSpec.builder(Keep.class).build())
                .initializer("new HashMap()").build();
        TypeSpec.Builder typeSpec = TypeSpec.classBuilder("AdControlMacro")
                .addField(fc)
                .addAnnotation(AnnotationSpec.builder(Keep.class).build())
                .addStaticBlock(builder.build())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        JavaFile constants = JavaFile.builder(PREFIX + ".macro", typeSpec.build()).build();
        try {
            constants.writeTo(processingEnv.getFiler());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 生成新的类，并返回该类对应的名称全路径
     *
     * @param control 控制器注解
     * @param element 类型
     * @return 新类对应的名称全路径
     */
    private String addControlField(Adc control, TypeElement element) {
        String packageName = getPackageName(element);

        String superClassName = packageName + "." + element.getSimpleName().toString();
        String className = element.getSimpleName() + "Imp";
        String impPackage = PREFIX + ".imp";
        String fullPackage = impPackage + "." + className;
        // 返回广告商名称
        MethodSpec cpNameMethod = MethodSpec.methodBuilder("getCpName")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Keep.class).build())
                .returns(String.class)
                .addStatement("return \"" + control.name() + "\"")
                .build();

        // 返回广告商是否是SDK
        MethodSpec isSdkMethod = MethodSpec.methodBuilder("isSdk")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Keep.class).build())
                .returns(boolean.class)
                .addStatement("return " + control.sdk())
                .build();

        // 生成新的类
        TypeSpec typeSpec = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Keep.class).build())
                .superclass(ClassName.bestGuess(superClassName))
                .addMethod(cpNameMethod)
                .addMethod(isSdkMethod)
                .build();

        // 创建Java文件
        JavaFile javaFile = JavaFile.builder(impPackage, typeSpec).build();
        // 将代码写入文件
        try {
            javaFile.writeTo(this.processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fullPackage;
    }

    /*
     * 获取包名
     *
     * @param element 元素
     * @return 包名
     */
    private String getPackageName(TypeElement element) {
        return this.processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();
    }

    /*
     * 日志输出
     *
     * @param kind 日志级别
     * @param message 日志
     */
    private void print(String message) {
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
    }
}
