package com.wdl.apt_processor;


import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.wdl.apt_annotation.BindView;

import java.io.IOException;
import java.text.MessageFormat;
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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

@SuppressWarnings("unused")
@AutoService(Processor.class)
public class BindViewProcessor extends AbstractProcessor {
    // Element代表程序的元素，例如包、类、方法。
    private Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        elementUtils = processingEnvironment.getElementUtils();
    }

    /**
     * @return 指定java版本。
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 指定该目标的注解对象,指定注解处理器是注册给哪个注解的，返回指定支持的注解类集合。
     *
     * @return Set<String> getCanonicalName即包名.类名，不同的对象获取的值不同，可能为空
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> hashSet = new HashSet<>();
        hashSet.add(BindView.class.getCanonicalName());
        return hashSet;
    }

    /**
     * 处理包含指定注解对象的代码元素
     * 获取控件变量的引用以及对应的viewId,先遍历出每个Activity所包含的所有注解对象
     *
     * @param set              Set<? extends TypeElement>
     * @param roundEnvironment RoundEnvironment 所有注解的集合
     * @return true
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        // 获取所有包含BindView注解的元素
        Set<? extends Element> elementSet = roundEnvironment.getElementsAnnotatedWith(BindView.class);
        // 此处的TypeElement就是Activity
        // Activity中包含的 id以及对应的属性（控件）
        Map<TypeElement, Map<Integer, VariableElement>> typeElementMapHashMap = new HashMap<>();
        for (Element element : elementSet) {
            // 注解的是FIELD，因此可以直接转换
            VariableElement variableElement = (VariableElement) element;
            // 获取最里层的元素，此处就是Activity
            TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();
            // 获取对应Activity中的Map viewId View
            Map<Integer, VariableElement> variableElementMap = typeElementMapHashMap.get(typeElement);
            if (variableElementMap == null) {
                variableElementMap = new HashMap<>();
                typeElementMapHashMap.put(typeElement, variableElementMap);
            }
            // 获取注解对象
            BindView bindView = variableElement.getAnnotation(BindView.class);
            // 获取注解值
            int id = bindView.value();
            variableElementMap.put(id, variableElement);
        }

        for (TypeElement key : typeElementMapHashMap.keySet()) {
            Map<Integer, VariableElement> elementMap = typeElementMapHashMap.get(key);
            String packageName = elementUtils.getPackageOf(key).getQualifiedName().toString();

            JavaFile javaFile = JavaFile.builder(packageName, generateCodeByPoet(key,
                    elementMap)).build();
            try {
                javaFile.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    private TypeSpec generateCodeByPoet(TypeElement typeElement, Map<Integer, VariableElement> variableElementMap) {
        //自动生成的文件以 Activity名 + ViewBinding 进行命名
        return TypeSpec.classBuilder(typeElement.getSimpleName().toString() + "ViewBinding")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(generateMethodByPoet(typeElement, variableElementMap))
                .build();
    }

    /**
     * @param typeElement        注解对象的根元素，即Activity
     * @param variableElementMap Activity包含的注解对象以及注解的目标对象
     * @return MethodSpec
     */
    private MethodSpec generateMethodByPoet(TypeElement typeElement, Map<Integer, VariableElement> variableElementMap) {
        ClassName className = ClassName.bestGuess(typeElement.getQualifiedName().toString());
        //  _mainActivity.btn_serializeSingle = (android.widget.Button) (_mainActivity.findViewById(2131165221));
        // 第一个转小写+下划线
        String parameter = "_" + Utils.toLowerCaseFirstChar(className.simpleName());
        MethodSpec.Builder builder = MethodSpec.methodBuilder("bind") // 方法名
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)             // public static
                .returns(void.class)// 返回类型
                .addParameter(className, parameter);
        for (int viewId : variableElementMap.keySet()) {
            VariableElement variableElement = variableElementMap.get(viewId);
            // 变量名
            String fieldName = variableElement.getSimpleName().toString();
            // 变量父类的全称
            String fieldType = variableElement.asType().toString();
            String text = "{0}.{1} = ({2})({3}.findViewById({4}));";
            builder.addCode(MessageFormat.format(text, parameter, fieldName, fieldType, parameter, String.valueOf(viewId)));
        }
        return builder.build();
    }
}

