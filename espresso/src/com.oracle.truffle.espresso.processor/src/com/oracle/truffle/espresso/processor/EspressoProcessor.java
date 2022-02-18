/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.espresso.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.oracle.truffle.espresso.processor.builders.ClassBuilder;
import com.oracle.truffle.espresso.processor.builders.ClassFileBuilder;
import com.oracle.truffle.espresso.processor.builders.FieldBuilder;
import com.oracle.truffle.espresso.processor.builders.JavadocBuilder;
import com.oracle.truffle.espresso.processor.builders.MethodBuilder;
import com.oracle.truffle.espresso.processor.builders.ModifierBuilder;
import com.oracle.truffle.espresso.processor.builders.SignatureBuilder;

/**
 * Helper class for creating all kinds of Substitution processor in Espresso. A processor need only
 * implement its own process method, along with providing three strings:
 * <li>The import sequence for the class generated.
 * <li>The constructor code for the class generated.
 * <li>The invoke method code for the class generated.
 * <p>
 * <p>
 * All other aspects of code generation are provided by this class.
 */
public abstract class EspressoProcessor extends BaseProcessor {
    // @formatter:off
    /* An example of a generated class is:
     *
     * /* Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
     *  * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
     *  *
     *  * This code is free software; you can redistribute it and/or modify it
     *  * under the terms of the GNU General Public License version 2 only, as
     *  * published by the Free Software Foundation.
     *  *
     *  * This code is distributed in the hope that it will be useful, but WITHOUT
     *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
     *  * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
     *  * version 2 for more details (a copy is included in the LICENSE file that
     *  * accompanied this code).
     *  *
     *  * You should have received a copy of the GNU General Public License version
     *  * 2 along with this work; if not, write to the Free Software Foundation,
     *  * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
     *  *
     *  * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
     *  * or visit www.oracle.com if you need additional information or have any
     *  * questions.
     *  * /
     *
     * package com.oracle.truffle.espresso.substitutions;
     *
     * import com.oracle.truffle.espresso.substitutions.Collect;
     *
     * import com.oracle.truffle.espresso.substitutions.JavaSubstitution;
     * import com.oracle.truffle.espresso.runtime.StaticObject;
     * import com.oracle.truffle.espresso.substitutions.Target_java_lang_invoke_MethodHandleNatives.Resolve;
     *
     * /**
     *  * Generated by: {@link com.oracle.truffle.espresso.substitutions.Target_java_lang_invoke_MethodHandleNatives.Resolve}
     *  * /
     * public final class Target_java_lang_invoke_MethodHandleNatives_Resolve_2 extends JavaSubstitution {
     *
     *     @Collect(com.oracle.truffle.espresso.substitutions.Substitution.class)
     *     public static final class Factory extends JavaSubstitution.Factory {
     *         public Factory() {
     *             super(
     *                 "resolve",
     *                 "Target_java_lang_invoke_MethodHandleNatives",
     *                 "Ljava/lang/invoke/MemberName;",
     *                 new String[]{
     *                     "Ljava/lang/invoke/MemberName;",
     *                     "Ljava/lang/Class;"
     *                 },
     *                 false
     *             );
     *         }
     *
     *         @Override
     *         public final JavaSubstitution create() {
     *             return new Target_java_lang_invoke_MethodHandleNatives_Resolve_2();
     *         }
     *     }
     *
     *     private @Child Resolve node;
     *
     *     @SuppressWarnings("unused")
     *     private Target_java_lang_invoke_MethodHandleNatives_Resolve_2() {
     *         this.node = com.oracle.truffle.espresso.substitutions.Target_java_lang_invoke_MethodHandleNativesFactory.ResolveNodeGen.create();
     *     }
     *
     *     @Override
     *     public final Object invoke(Object[] args) {
     *         StaticObject arg0 = (StaticObject) args[0];
     *         StaticObject arg1 = (StaticObject) args[1];
     *         return this.node.execute(arg0, arg1);
     *     }
     * }
     */
    // @formatter:on

    /**
     * Does the actual work of the processor. The pattern used in espresso is:
     * <li>Initialize the {@link TypeElement} of the annotations that will be used, along with their
     * {@link AnnotationValue}, as necessary.
     * <li>Iterate over all methods annotated with what was returned by
     * {@link Processor#getSupportedAnnotationTypes()}, and process them so that each one spawns a
     * class.
     *
     * @see EspressoProcessor#commitSubstitution(Element, String, String, String)
     */
    abstract void processImpl(RoundEnvironment roundEnvironment);

    /**
     * Returns a list of expected imports of the current substitutor.
     * <p>
     * Note that the required imports vary between classes, as some might not be used, triggering
     * style issues, which is why this is delegated.
     *
     * @see EspressoProcessor#IMPORT_INTEROP_LIBRARY
     * @see EspressoProcessor#IMPORT_STATIC_OBJECT
     * @see EspressoProcessor#IMPORT_TRUFFLE_OBJECT
     */
    abstract List<String> expectedImports(String className, String targetMethodName, List<String> parameterTypeName, SubstitutionHelper helper);

    /**
     * Generates the builder corresponding to the Constructor for the current substitutor. In
     * particular, it should call its super class substitutor's constructor.
     *
     * @see EspressoProcessor#substitutor
     */
    abstract ClassBuilder generateFactoryConstructor(ClassBuilder factoryBuilder, String className, String targetMethodName, List<String> parameterTypeName, SubstitutionHelper helper);

    /**
     * Generates the builder that corresponds to the code of the invoke method for the current
     * substitutor. Care must be taken to correctly unwrap and cast the given arguments (given in an
     * Object[]) so that they correspond to the substituted method's signature. Furthermore, all
     * TruffleObject nulls must be replaced with Espresso nulls (Null check can be done through
     * truffle libraries).
     *
     * @see EspressoProcessor#castTo(String, String)
     * @see EspressoProcessor#IMPORT_INTEROP_LIBRARY
     * @see EspressoProcessor#STATIC_OBJECT_NULL
     */
    abstract ClassBuilder generateInvoke(ClassBuilder classBuilder, String className, String targetMethodName, List<String> parameterTypeName, SubstitutionHelper helper);

    EspressoProcessor(String substitutionPackage, String substitutor) {
        this.substitutorPackage = substitutionPackage;
        this.substitutor = substitutor;
    }

    // Instance specific constants
    protected final String substitutorPackage;
    private final String substitutor;

    // Processor local info
    protected boolean done = false;

    // Special annotations
    TypeElement inject;
    private static final String INJECT = "com.oracle.truffle.espresso.substitutions.Inject";

    TypeElement noSafepoint;
    private static final String NO_SAFEPOINT = "com.oracle.truffle.espresso.jni.NoSafepoint";

    TypeElement substitutionProfiler;
    private static final String SUBSTITUTION_PROFILER = "com.oracle.truffle.espresso.substitutions.SubstitutionProfiler";

    TypeElement staticObject;
    private static final String STATIC_OBJECT = "com.oracle.truffle.espresso.runtime.StaticObject";

    TypeElement javaType;
    private static final String JAVA_TYPE = "com.oracle.truffle.espresso.substitutions.JavaType";

    TypeElement meta;
    private static final String META = "com.oracle.truffle.espresso.meta.Meta";

    TypeElement espressoContext;
    private static final String ESPRESSO_CONTEXT = "com.oracle.truffle.espresso.runtime.EspressoContext";

    TypeElement truffleNode;
    private static final String TRUFFLE_NODE = "com.oracle.truffle.api.nodes.Node";

    // Global constants
    protected static final String FACTORY = "Factory";

    static final String SUPPRESS_UNUSED = "@SuppressWarnings(\"unused\")";

    static final String IS_TRIVIAL = "isTrivial";

    static final String STATIC_OBJECT_NULL = "StaticObject.NULL";

    static final String IMPORT_INTEROP_LIBRARY = "com.oracle.truffle.api.interop.InteropLibrary";
    static final String IMPORT_STATIC_OBJECT = "com.oracle.truffle.espresso.runtime.StaticObject";
    static final String IMPORT_TRUFFLE_OBJECT = "com.oracle.truffle.api.interop.TruffleObject";
    static final String IMPORT_ESPRESSO_CONTEXT = "com.oracle.truffle.espresso.runtime.EspressoContext";
    static final String IMPORT_PROFILE = "com.oracle.truffle.espresso.substitutions.SubstitutionProfiler";
    static final String IMPORT_COLLECT = "com.oracle.truffle.espresso.substitutions.Collect";

    static final String CONTEXT = "EspressoContext.get(this)";
    static final String CONTEXT_META = "EspressoContext.get(this).getMeta()";
    static final String META_TYPE = "Meta";

    static final String PROFILE_CLASS = "SubstitutionProfiler";
    static final String PROFILE_ARG_CALL = "this";

    static final String ESPRESSO_CONTEXT_CLASS = "EspressoContext ";

    static final String CREATE = "create";

    static final String SPLIT = "split";

    static final String ARGS_NAME = "args";
    static final String ARG_NAME = "arg";

    static final String SAFEPOINT_POLL = "com.oracle.truffle.api.TruffleSafepoint.poll(this);";

    public static NativeType classToType(TypeKind typeKind) {
        // @formatter:off
        switch (typeKind) {
            case BOOLEAN : return NativeType.BOOLEAN;
            case BYTE    : return NativeType.BYTE;
            case SHORT   : return NativeType.SHORT;
            case CHAR    : return NativeType.CHAR;
            case INT     : return NativeType.INT;
            case FLOAT   : return NativeType.FLOAT;
            case LONG    : return NativeType.LONG;
            case DOUBLE  : return NativeType.DOUBLE;
            case VOID    : return NativeType.VOID;
            default:
                return NativeType.OBJECT;
        }
        // @formatter:on
    }

    /**
     * Returns the name of the substituted method.
     */
    protected String getSubstutitutedMethodName(Element targetElement) {
        return targetElement.getSimpleName().toString();
    }

    /**
     * Returns the target method to be called by a substitution.
     *
     * Returns the targetElement itself for method substitutions; or the execute* method of the
     * Truffle node, for node substitutions.
     */
    protected ExecutableElement getTargetMethod(Element targetElement) {
        if (targetElement.getKind() == ElementKind.CLASS) {
            return findNodeExecute((TypeElement) targetElement);
        }
        return (ExecutableElement) targetElement;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
    }

    @Override
    public boolean doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (done) {
            return false;
        }
        inject = getTypeElement(INJECT);
        noSafepoint = getTypeElement(NO_SAFEPOINT);
        staticObject = getTypeElement(STATIC_OBJECT);
        javaType = getTypeElement(JAVA_TYPE);
        meta = getTypeElement(META);
        espressoContext = getTypeElement(ESPRESSO_CONTEXT);
        substitutionProfiler = getTypeElement(SUBSTITUTION_PROFILER);
        truffleNode = getTypeElement(TRUFFLE_NODE);
        processImpl(roundEnv);
        done = true;
        return false;
    }

    // Utility Methods

    static AnnotationMirror getAnnotation(TypeMirror e, TypeElement type) {
        for (AnnotationMirror annotationMirror : e.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().asElement().equals(type)) {
                return annotationMirror;
            }
        }
        return null;
    }

    static AnnotationMirror getAnnotation(Element e, TypeElement type) {
        for (AnnotationMirror annotationMirror : e.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().asElement().equals(type)) {
                return annotationMirror;
            }
        }
        return null;
    }

    /**
     * For substitutions that use a node, find the execute* method. Suitable methods must be
     * non-private and be called execute*.
     */
    ExecutableElement findNodeExecute(TypeElement node) {
        if (!env().getTypeUtils().isSubtype(node.asType(), truffleNode.asType())) {
            getMessager().printMessage(Diagnostic.Kind.ERROR, "(Node) Substitution must inherit from " + truffleNode.getQualifiedName(), node);
        }
        ExecutableElement executeMethod = null;
        TypeElement curElement = node;
        while (true) {
            for (Element method : curElement.getEnclosedElements()) {
                if (method.getKind() == ElementKind.METHOD) {
                    // Match abstract non-private execute* .
                    if (method.getSimpleName().toString().startsWith("execute") &&
                                    !method.getModifiers().contains(Modifier.PRIVATE) &&
                                    method.getModifiers().contains(Modifier.ABSTRACT)) {
                        if (executeMethod != null) {
                            getMessager().printMessage(Diagnostic.Kind.ERROR, "Ambiguous execute* methods found: a unique non-private abstract execute* method is required", node);
                        }
                        executeMethod = (ExecutableElement) method;
                    }
                }
            }
            TypeMirror superClass = curElement.getSuperclass();
            if (TypeKind.NONE.equals(superClass.getKind())) {
                break;
            }
            curElement = asTypeElement(superClass);
        }
        if (executeMethod == null) {
            getMessager().printMessage(Diagnostic.Kind.ERROR, "Node execute* method not found", node);
        }
        return executeMethod;
    }

    boolean hasInjectedParameter(ExecutableElement method, TypeMirror supportedType) {
        List<? extends VariableElement> params = method.getParameters();
        for (VariableElement e : params) {
            if (getAnnotation(e.asType(), inject) != null) {
                if (env().getTypeUtils().isSameType(e.asType(), supportedType)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean hasProfileInjection(ExecutableElement method) {
        return hasInjectedParameter(method, substitutionProfiler.asType());
    }

    boolean hasMetaInjection(ExecutableElement method) {
        return hasInjectedParameter(method, meta.asType());
    }

    boolean hasContextInjection(ExecutableElement method) {
        return hasInjectedParameter(method, espressoContext.asType());
    }

    boolean skipsSafepoint(Element target) {
        return getAnnotation(target, noSafepoint) != null;
    }

    boolean isActualParameter(VariableElement param) {
        return getAnnotation(param.asType(), inject) == null;
    }

    static boolean checkFirst(StringBuilder str, boolean first) {
        if (!first) {
            str.append(", ");
        }
        return false;
    }

    private static StringBuilder signatureSuffixBuilder(List<String> parameterTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append("__");
        for (String param : parameterTypes) {
            // @formatter:off
            switch (param) {
                case "boolean" : sb.append("Z"); break;
                case "byte"    : sb.append("B"); break;
                case "char"    : sb.append("C"); break;
                case "int"     : sb.append("I"); break;
                case "long"    : sb.append("J"); break;
                case "float"   : sb.append("F"); break;
                case "double"  : sb.append("D"); break;
                case "void"    : sb.append("V"); break;
                default:
                    assert (param.startsWith("[") || param.startsWith("L")) && param.endsWith(";");
                    sb.append("L"); break;
            }
            // @formatter:on
        }
        return sb;
    }

    static String getSubstitutorClassName(String className, String methodName, List<String> parameterTypes) {
        return String.format("%s_%s%s", className, methodName, signatureSuffixBuilder(parameterTypes));
    }

    static String castTo(String obj, String clazz) {
        if (clazz.equals("Object")) {
            return obj;
        }
        return "(" + clazz + ") " + obj;
    }

    static String extractSimpleType(String arg) {
        // The argument can be a fully qualified type e.g. java.lang.String, int, long...
        // Or an annotated type e.g. "(@com.example.Annotation :: long)",
        // "(@com.example.Annotation :: java.lang.String)".
        // javac always includes annotations, ecj does not.

        // Purge enclosing parentheses.
        String result = arg;
        if (result.startsWith("(")) {
            result = result.substring(1, result.length() - 1);
        }

        // Purge leading annotations.
        String[] parts = result.split("::");
        result = parts[parts.length - 1].trim();
        // Prune additional spaces produced by javac 11.
        parts = result.split(" ");
        result = parts[parts.length - 1].trim();

        // Get unqualified name.
        int beginIndex = result.lastIndexOf('.');
        if (beginIndex >= 0) {
            result = result.substring(beginIndex + 1);
        }
        return result;
    }

    /**
     * Injects the meta information in the substitution call.
     */
    static boolean appendInvocationMetaInformation(StringBuilder str, boolean first, SubstitutionHelper helper) {
        boolean f = first;
        if (helper.hasMetaInjection) {
            f = injectMeta(str, f);
        }
        if (helper.hasProfileInjection) {
            f = injectProfile(str, f);
        }
        if (helper.hasContextInjection) {
            f = injectContext(str, f);
        }
        return f;
    }

    // Commits a single substitution.
    void commitSubstitution(Element method, String targetPackage, String substitutorName, String classFile) {
        try {
            // Create the file
            JavaFileObject file = processingEnv.getFiler().createSourceFile(targetPackage + "." + substitutorName, method);
            Writer wr = file.openWriter();
            wr.write(classFile);
            wr.close();
        } catch (IOException ex) {
            /* nop */
        }
    }

    private static JavadocBuilder generateGeneratedBy(String className, List<String> parameterTypes, SubstitutionHelper helper) {
        JavadocBuilder javadocBuilder = new JavadocBuilder();

        if (helper.isNodeTarget()) {
            javadocBuilder.addGeneratedByLine(helper.getNodeTarget().getQualifiedName());
            return javadocBuilder;
        }

        SignatureBuilder linkSignature = new SignatureBuilder(className + "#" + helper.getMethodTarget().getSimpleName());

        for (String param : parameterTypes) {
            linkSignature.addParam(param);
        }
        if (helper.hasMetaInjection) {
            linkSignature.addParam(META_TYPE);
        }
        if (helper.hasProfileInjection) {
            linkSignature.addParam(PROFILE_CLASS);
        }
        if (helper.hasContextInjection) {
            linkSignature.addParam(ESPRESSO_CONTEXT_CLASS);
        }

        javadocBuilder.addGeneratedByLine(linkSignature);
        return javadocBuilder;
    }

    static SignatureBuilder generateNativeSignature(NativeType[] signature) {
        SignatureBuilder sb = new SignatureBuilder("NativeSignature.create");
        for (NativeType t : signature) {
            sb.addParam("NativeType." + t);
        }
        return sb;
    }

    // @formatter:off
    /**
     * Generates the following.
     * 
     * @Collect(ImplAnnotation.class)
     * public static final class Factory extends SUBSTITUTOR.Factory {
     *     private Factory() {
     *         super(
     *             "SUBSTITUTED_METHOD",
     *             "SUBSTITUTION_CLASS",
     *             "RETURN_TYPE",
     *             new String[]{
     *                 SIGNATURE
     *             },
     *             HAS_RECEIVER
     *         );
     *     }
     *     @Override
     *     public final SUBSTITUTOR create() {
     *         return new className();
     *     }
     * }
     */
    // @formatter:on
    private ClassBuilder generateFactory(String className, String targetMethodName, List<String> parameterTypeName, SubstitutionHelper helper) {
        ClassBuilder factory = new ClassBuilder(FACTORY) //
                        .withAnnotation("@Collect(", helper.getImplAnnotation().getQualifiedName().toString(), ".class)") //
                        .withQualifiers(new ModifierBuilder().asPublic().asStatic().asFinal()) //
                        .withSuperClass(substitutor + "." + FACTORY);
        generateFactoryConstructor(factory, className, targetMethodName, parameterTypeName, helper);
        factory.withMethod(new MethodBuilder(CREATE) //
                        .withOverrideAnnotation() //
                        .withModifiers(new ModifierBuilder().asPublic().asFinal()) //
                        .withReturnType(substitutor) //
                        .addBodyLine("return new ", className, "();"));
        return factory;
    }

    private static void generateChildInstanceField(ClassBuilder cb, SubstitutionHelper helper) {
        if (helper.isNodeTarget()) {
            FieldBuilder field = new FieldBuilder(helper.getNodeTarget().getSimpleName(), "node") //
                            .withAnnotation("@Child") //
                            .withQualifiers(new ModifierBuilder().asPrivate());
            cb.withField(field);
        }
    }

    /**
     * Generates the constructor for the substitutor.
     */
    private static MethodBuilder generateConstructor(String substitutorName, SubstitutionHelper helper) {
        MethodBuilder constructor = new MethodBuilder(substitutorName) //
                        .asConstructor() //
                        .withModifiers(new ModifierBuilder().asPrivate());

        if (helper.isNodeTarget()) {
            TypeElement enclosing = (TypeElement) helper.getNodeTarget().getEnclosingElement();
            constructor.addBodyLine("this.node = ", enclosing.getQualifiedName(), "Factory.", helper.getNodeTarget().getSimpleName(), "NodeGen", ".create();");
        }
        return constructor;
    }

    private static boolean isTrivial(Element element, TypeElement implAnnotation) {
        AnnotationMirror mirror = getAnnotation(element, implAnnotation);
        try {
            Boolean value = getAnnotationValue(mirror, "isTrivial", Boolean.class);
            return value != null && value;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Generates isTrivial getter.
     */
    private static MethodBuilder generateIsTrivial(SubstitutionHelper helper) {
        MethodBuilder isTrivialMethod = new MethodBuilder(IS_TRIVIAL) //
                        .withOverrideAnnotation() //
                        .withModifiers(new ModifierBuilder().asPublic()) //
                        .withReturnType("boolean") //
                        .addBodyLine("return ", isTrivial(helper.getTarget(), helper.getImplAnnotation()), ';');
        return isTrivialMethod;
    }

    static boolean injectMeta(StringBuilder str, boolean first) {
        checkFirst(str, first);
        str.append(CONTEXT_META);
        return false;
    }

    static boolean injectProfile(StringBuilder str, boolean first) {
        checkFirst(str, first);
        str.append(PROFILE_ARG_CALL);
        return false;
    }

    static boolean injectContext(StringBuilder str, boolean first) {
        checkFirst(str, first);
        str.append(CONTEXT);
        return false;
    }

    /**
     * Creates the substitutor.
     *
     * @param className The name of the host class where the substituted method is found.
     * @param targetMethodName The name of the substituted method.
     * @param parameterTypeName The list of *Host* parameter types of the substituted method.
     * @param helper A helper structure.
     * @return The string forming the substitutor.
     */
    String spawnSubstitutor(String substitutorName, String targetPackage, String className, String targetMethodName, List<String> parameterTypeName, SubstitutionHelper helper) {
        ClassFileBuilder substitutorFile = new ClassFileBuilder() //
                        .withCopyright() //
                        .inPackage(targetPackage);

        // Prepare imports
        List<String> expectedImports = expectedImports(substitutorName, targetMethodName, parameterTypeName, helper);

        if (helper.hasMetaInjection || helper.hasContextInjection) {
            expectedImports.add(IMPORT_ESPRESSO_CONTEXT);
        }
        expectedImports.add(IMPORT_COLLECT);

        // Add imports (filter useless import)
        for (String toImport : expectedImports) {
            String maybePackage = toImport.substring(0, toImport.lastIndexOf('.'));
            if (!targetPackage.equals(maybePackage)) {
                substitutorFile.withImport(toImport);
            }
        }

        ClassBuilder substitutorClass = new ClassBuilder(substitutorName) //
                        .withSuperClass(substitutor) //
                        .withJavaDoc(generateGeneratedBy(className, parameterTypeName, helper)) //
                        .withQualifiers(new ModifierBuilder().asPublic().asFinal()) //
                        .withInnerClass(generateFactory(substitutorName, targetMethodName, parameterTypeName, helper));

        if (helper.isNodeTarget() || helper.hasMetaInjection || helper.hasProfileInjection || helper.hasContextInjection) {
            generateChildInstanceField(substitutorClass, helper);
        }

        MethodBuilder constructor = generateConstructor(substitutorName, helper) //
                        .withAnnotation(SUPPRESS_UNUSED);
        substitutorClass.withMethod(constructor);

        substitutorClass.withMethod(generateSplit());

        if (isTrivial(helper.getTarget(), helper.getImplAnnotation())) {
            substitutorClass.withMethod(generateIsTrivial(helper));
        }

        generateInvoke(substitutorClass, className, targetMethodName, parameterTypeName, helper);

        substitutorFile.withClass(substitutorClass);
        return substitutorFile.build();
    }

    /**
     * Injects override of 'split()' methods.
     */
    private MethodBuilder generateSplit() {
        MethodBuilder method = new MethodBuilder(SPLIT) //
                        .withOverrideAnnotation() //
                        .withModifiers(new ModifierBuilder().asPublic().asFinal()) //
                        .withReturnType(substitutor) //
                        .addBodyLine("return new ", FACTORY, "().", CREATE, "();");
        return method;
    }

    public Messager getMessager() {
        return processingEnv.getMessager();
    }
}
