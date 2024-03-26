/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.espresso.substitutions;

import static com.oracle.truffle.espresso.substitutions.Target_java_util_regex_Pattern.convertFlags;
import static java.lang.Math.max;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.espresso.impl.Field;
import com.oracle.truffle.espresso.meta.EspressoError;
import com.oracle.truffle.espresso.meta.Meta;
import com.oracle.truffle.espresso.nodes.bytecodes.InvokeInterface;
import com.oracle.truffle.espresso.runtime.EspressoContext;
import com.oracle.truffle.espresso.runtime.staticobject.StaticObject;

@EspressoSubstitutions
public final class Target_java_util_regex_Matcher {
    private Target_java_util_regex_Matcher() {
    }

    @ImportStatic({Target_java_util_regex_Pattern.class, Target_java_util_regex_Matcher.class})
    @Substitution(hasReceiver = true, methodName = "<init>")
    abstract static class Init extends SubstitutionNode {
        abstract void execute(@JavaType(Matcher.class) StaticObject self, @JavaType(Pattern.class) StaticObject parent, @JavaType(CharSequence.class) StaticObject text);

        @Specialization(guards = {"!isUnsupportedPattern(parent, meta)", "isString(text, meta)"})
        void doStringConversion(StaticObject self, StaticObject parent, StaticObject text,
                        @Bind("getMeta()") Meta meta,
                        @Shared("fromJavaString") @Cached TruffleString.FromJavaStringNode fromJavaStringNode,
                        @Shared("original") @Cached("create(getMeta().java_util_regex_Matcher_init.getCallTargetNoSubstitution())") DirectCallNode original) {
            assert getContext().regexSubstitutionsEnabled();
            saveTruffleString(self, fromJavaStringNode, text, meta);
            original.call(self, parent, text);
        }

        @Specialization(guards = {"!isUnsupportedPattern(parent, meta)", "!isString(text, meta)"})
        void doCharSeqConversion(StaticObject self, StaticObject parent, StaticObject text,
                        @Bind("getMeta()") Meta meta,
                        @Shared("fromJavaString") @Cached TruffleString.FromJavaStringNode fromJavaStringNode,
                        @Shared("original") @Cached("create(getMeta().java_util_regex_Matcher_init.getCallTargetNoSubstitution())") DirectCallNode original,
                        @Cached("create(meta.java_lang_CharSequence_toString)") InvokeInterface toStringCall) {
            assert getContext().regexSubstitutionsEnabled();
            StaticObject espressoString = (StaticObject) toStringCall.execute(new Object[]{text});
            saveTruffleString(self, fromJavaStringNode, espressoString, meta);
            original.call(self, parent, text);
        }

        @Specialization(guards = "isUnsupportedPattern(parent, meta)")
        static void doFallback(StaticObject self, StaticObject parent, StaticObject text,
                        @Bind("getMeta()") @SuppressWarnings("unused") Meta meta,
                        @Shared("original") @Cached("create(getMeta().java_util_regex_Matcher_init.getCallTargetNoSubstitution())") DirectCallNode original) {
            original.call(self, parent, text);
        }
    }

    @ImportStatic(Target_java_util_regex_Matcher.class)
    @Substitution(hasReceiver = true)
    abstract static class Reset extends SubstitutionNode {
        abstract @JavaType(Matcher.class) StaticObject execute(@JavaType(Matcher.class) StaticObject self, @JavaType(CharSequence.class) StaticObject text);

        @Specialization(guards = {"!isUnsupportedMatcher(self, meta)", "isString(text, meta)"})
        StaticObject doStringConversion(StaticObject self, StaticObject text,
                        @Bind("getMeta()") Meta meta,
                        @Shared("fromJavaString") @Cached TruffleString.FromJavaStringNode fromJavaStringNode,
                        @Shared("original") @Cached("create(getMeta().java_util_regex_Matcher_reset.getCallTargetNoSubstitution())") DirectCallNode original) {
            assert getContext().regexSubstitutionsEnabled();
            saveTruffleString(self, fromJavaStringNode, text, meta);
            return (StaticObject) original.call(self, text);
        }

        @Specialization(guards = {"!isUnsupportedMatcher(self, meta)", "!isString(text, meta)"})
        StaticObject doCharSeqConversion(StaticObject self, StaticObject text,
                        @Bind("getMeta()") Meta meta,
                        @Shared("fromJavaString") @Cached TruffleString.FromJavaStringNode fromJavaStringNode,
                        @Shared("original") @Cached("create(getMeta().java_util_regex_Matcher_reset.getCallTargetNoSubstitution())") DirectCallNode original,
                        @Cached("create(meta.java_lang_CharSequence_toString)") InvokeInterface toStringCall) {
            assert getContext().regexSubstitutionsEnabled();
            StaticObject espressoString = (StaticObject) toStringCall.execute(new Object[]{text});
            saveTruffleString(self, fromJavaStringNode, espressoString, meta);
            return (StaticObject) original.call(self, text);
        }

        @Specialization(guards = "isUnsupportedMatcher(self, meta)")
        static StaticObject doFallback(StaticObject self, StaticObject text,
                        @Bind("getMeta()") @SuppressWarnings("unused") Meta meta,
                        @Shared("original") @Cached("create(getMeta().java_util_regex_Matcher_reset.getCallTargetNoSubstitution())") DirectCallNode original) {
            return (StaticObject) original.call(self, text);
        }
    }

    private static final int NOANCHOR = 0;
    private static final int ENDANCHOR = 1;

    private enum MatchMode {
        MATCH_NOANCHOR,
        MATCH_ENDANCHOR,
        SEARCH
    }

    @ImportStatic(Target_java_util_regex_Matcher.class)
    @Substitution(hasReceiver = true)
    abstract static class Match extends SubstitutionNode {
        abstract boolean execute(@JavaType(Matcher.class) StaticObject self, int from, int anchor);

        @Specialization(guards = {"!isUnsupportedMatcher(self, meta)", "regexObject != null"})
        boolean doLazy(StaticObject self, int from, int anchor,
                        @Bind("getMeta()") Meta meta,
                        @Bind("getRegexObject(self, anchor, meta)") Object regexObject,
                        @Shared("exec") @Cached JavaRegexExecNode javaRegexExecNode,
                        @Bind("this") Node node) {
            assert getContext().regexSubstitutionsEnabled();
            meta.java_util_regex_Matcher_HIDDEN_searchFromBackup.setHiddenObject(self, from);
            meta.java_util_regex_Matcher_HIDDEN_matchingModeBackup.setHiddenObject(self, getMatchMode(anchor));
            return checkResult(self, from, anchor, regexObject, javaRegexExecNode, node, meta);
        }

        @Specialization(guards = {"getRegexObject(self, anchor, meta) == null", "!isUnsupportedMatcher(self, meta)"})
        static boolean doCompile(StaticObject self, int from, int anchor,
                        @Bind("getContext()") EspressoContext context,
                        @Bind("context.getMeta()") Meta meta,
                        @Shared("exec") @Cached JavaRegexExecNode javaRegexExecNode,
                        @Cached JavaRegexCompileNode javaRegexCompileNode,
                        @Bind("this") Node node) {
            assert context.regexSubstitutionsEnabled();
            meta.java_util_regex_Matcher_HIDDEN_searchFromBackup.setHiddenObject(self, from);
            meta.java_util_regex_Matcher_HIDDEN_matchingModeBackup.setHiddenObject(self, getMatchMode(anchor));
            String method;
            Field destination;
            if (anchor == ENDANCHOR) {
                method = "fullmatch";
                destination = meta.java_util_regex_Pattern_HIDDEN_tregexFullmatch;
            } else {
                method = "match";
                destination = meta.java_util_regex_Pattern_HIDDEN_tregexMatch;
            }
            Object regexObject = javaRegexCompileNode.execute(node, self, method, destination, context);
            return checkResult(self, from, anchor, regexObject, javaRegexExecNode, node, meta);
        }

        @Specialization(guards = "isUnsupportedMatcher(self, context.getMeta())")
        static boolean doFallback(StaticObject self, int from, int anchor,
                        @Bind("getContext()") EspressoContext context,
                        @Cached("create(context.getMeta().java_util_regex_Matcher_match.getCallTargetNoSubstitution())") DirectCallNode original) {
            if (context.regexSubstitutionsEnabled()) {
                Meta meta = context.getMeta();
                meta.java_util_regex_Matcher_HIDDEN_matchingModeBackup.setHiddenObject(self, StaticObject.NULL);
                compileFallBackIfRequired(self, meta);
            }
            return (Boolean) original.call(self, from, anchor);
        }

        private static MatchMode getMatchMode(int anchor) {
            MatchMode mode;
            if (anchor == NOANCHOR) {
                mode = MatchMode.MATCH_NOANCHOR;
            } else {
                assert anchor == ENDANCHOR;
                mode = MatchMode.MATCH_ENDANCHOR;
            }
            return mode;
        }

        public static Object getRegexObject(StaticObject self, int anchor, Meta meta) {
            StaticObject parentPattern = meta.java_util_regex_Matcher_parentPattern.getObject(self);
            if (anchor == ENDANCHOR) {
                return meta.java_util_regex_Pattern_HIDDEN_tregexFullmatch.getHiddenObject(parentPattern);
            } else {
                assert anchor == NOANCHOR;
                return meta.java_util_regex_Pattern_HIDDEN_tregexMatch.getHiddenObject(parentPattern);
            }
        }

        private static boolean checkResult(StaticObject self, int from, int anchor, Object regexObject, JavaRegexExecNode javaRegexExecNode, Node node, Meta meta) {
            saveBackup(self, meta);
            if (javaRegexExecNode.execute(node, regexObject, self, from, meta)) {
                if (anchor == ENDANCHOR) {
                    int last = meta.java_util_regex_Matcher_last.getInt(self);
                    int to = meta.java_util_regex_Matcher_to.getInt(self);
                    if (last == to) {
                        return true;
                    }
                } else {
                    assert anchor == NOANCHOR;
                    return true;
                }
            }
            meta.java_util_regex_Matcher_first.setInt(self, -1);
            return false;
        }
    }

    @ImportStatic(Target_java_util_regex_Matcher.class)
    @Substitution(hasReceiver = true)
    abstract static class Search extends SubstitutionNode {
        abstract boolean execute(@JavaType(Matcher.class) StaticObject self, int from);

        @Specialization(guards = {"!isUnsupportedMatcher(self, meta)", "regexObject != null"})
        boolean doLazy(StaticObject self, int from,
                        @Bind("getMeta()") Meta meta,
                        @Bind("getRegexSearchObject(self, meta)") Object regexObject,
                        @Shared("exec") @Cached JavaRegexExecNode javaRegexExecNode,
                        @Bind("this") Node node) {
            assert getContext().regexSubstitutionsEnabled();
            meta.java_util_regex_Matcher_HIDDEN_searchFromBackup.setHiddenObject(self, from);
            meta.java_util_regex_Matcher_HIDDEN_matchingModeBackup.setHiddenObject(self, MatchMode.SEARCH);

            saveBackup(self, meta);
            boolean isMatch = javaRegexExecNode.execute(node, regexObject, self, from, meta);
            if (!isMatch) {
                meta.java_util_regex_Matcher_first.setInt(self, -1);
            }
            return isMatch;
        }

        @Specialization(guards = {"!isUnsupportedMatcher(self, meta)", "getRegexSearchObject(self, meta) == null"})
        static boolean doCompile(StaticObject self, int from,
                        @Bind("getContext()") EspressoContext context,
                        @Bind("context.getMeta()") Meta meta,
                        @Shared("exec") @Cached JavaRegexExecNode javaRegexExecNode,
                        @Cached JavaRegexCompileNode javaRegexCompileNode,
                        @Bind("this") Node node) {
            assert context.regexSubstitutionsEnabled();
            meta.java_util_regex_Matcher_HIDDEN_searchFromBackup.setHiddenObject(self, from);
            meta.java_util_regex_Matcher_HIDDEN_matchingModeBackup.setHiddenObject(self, MatchMode.SEARCH);

            Object regexObject = javaRegexCompileNode.execute(node, self, "search", meta.java_util_regex_Pattern_HIDDEN_tregexSearch, context);
            saveBackup(self, meta);
            boolean isMatch = javaRegexExecNode.execute(node, regexObject, self, from, meta);
            if (!isMatch) {
                meta.java_util_regex_Matcher_first.setInt(self, -1);
            }
            return isMatch;
        }

        @Specialization(guards = "isUnsupportedMatcher(self, meta)")
        static boolean doFallback(StaticObject self, int from,
                        @Bind("getContext()") EspressoContext context,
                        @Bind("context.getMeta()") Meta meta,
                        @Cached("create(meta.java_util_regex_Matcher_search.getCallTargetNoSubstitution())") DirectCallNode original) {
            if (context.regexSubstitutionsEnabled()) {
                meta.java_util_regex_Matcher_HIDDEN_matchingModeBackup.setHiddenObject(self, StaticObject.NULL);
                compileFallBackIfRequired(self, meta);
            }
            return (Boolean) original.call(self, from);
        }

        public static Object getRegexSearchObject(StaticObject self, Meta meta) {
            StaticObject parentPattern = meta.java_util_regex_Matcher_parentPattern.getObject(self);
            return meta.java_util_regex_Pattern_HIDDEN_tregexSearch.getHiddenObject(parentPattern);
        }
    }

    @Substitution(hasReceiver = true)
    public static boolean hitEnd(StaticObject self, @Inject EspressoContext context) {
        Meta meta = context.getMeta();
        if (context.regexSubstitutionsEnabled()) {
            executeLastWithFallback(self, meta);
        }
        return meta.java_util_regex_Matcher_hitEnd.getBoolean(self);
    }

    @Substitution(hasReceiver = true)
    public static boolean requireEnd(StaticObject self, @Inject EspressoContext context) {
        Meta meta = context.getMeta();
        if (context.regexSubstitutionsEnabled()) {
            executeLastWithFallback(self, meta);
        }
        return meta.java_util_regex_Matcher_requireEnd.getBoolean(self);
    }

    @Substitution(hasReceiver = true)
    abstract static class GroupCount extends SubstitutionNode {
        abstract int execute(@JavaType(Matcher.class) StaticObject self);

        @Specialization
        static int doDefault(StaticObject self,
                        @Bind("getContext()") EspressoContext context,
                        @Cached("create(context.getMeta().java_util_regex_Matcher_groupCount.getCallTargetNoSubstitution())") DirectCallNode original) {
            if (context.regexSubstitutionsEnabled()) {
                Meta meta = context.getMeta();
                StaticObject parentPattern = meta.java_util_regex_Matcher_parentPattern.getObject(self);
                if (meta.java_util_regex_Pattern_capturingGroupCount.getInt(parentPattern) == -1) {
                    meta.java_util_regex_Pattern_capturingGroupCount.setInt(parentPattern, 1);
                    compileFallBackIfRequired(self, meta);
                }
            }
            return (Integer) original.call(self);
        }
    }

    @GenerateInline
    @GenerateUncached
    public abstract static class JavaRegexCompileNode extends Node {
        public abstract Object execute(Node node, StaticObject self, String method, Field destination, EspressoContext context);

        @Specialization
        static Object doDefault(StaticObject self, String method, Field destination, EspressoContext context,
                        @CachedLibrary(limit = "3") InteropLibrary regexObjectInterop,
                        @CachedLibrary(limit = "3") InteropLibrary integerInterop,
                        @CachedLibrary(limit = "3") InteropLibrary mapInterop,
                        @CachedLibrary(limit = "3") InteropLibrary stringInterop,
                        @CachedLibrary(limit = "3") InteropLibrary arrayInterop) {
            Meta meta = context.getMeta();
            StaticObject patternObject = meta.java_util_regex_Matcher_parentPattern.getObject(self);
            String pattern = meta.toHostString(meta.java_util_regex_Pattern_pattern.getObject(patternObject));

            Source src = getSource(method, pattern, patternObject, meta);

            Object regexObject = context.getEnv().parseInternal(src).call();
            destination.setHiddenObject(patternObject, regexObject);

            int groupCount = getGroupCount(regexObject, regexObjectInterop, integerInterop);
            meta.java_util_regex_Pattern_capturingGroupCount.setInt(patternObject, groupCount);
            reallocateGroupsArrayIfNecessary(self, patternObject, meta);

            if (meta.java_util_regex_Pattern_namedGroups_field.getObject(patternObject) == null) {
                Object map = getGroups(regexObject, regexObjectInterop);
                Object keys = getMembers(map, mapInterop);
                long size = getArraySize(keys, arrayInterop);

                StaticObject guestMap = meta.java_util_HashMap.allocateInstance();
                meta.java_util_HashMap_init.invokeDirect(guestMap, (int) size);

                for (long i = 0; i < size; i++) {
                    String key = getKey(keys, i, stringInterop, arrayInterop);
                    Object value = getValue(map, key, mapInterop);
                    StaticObject guestKey = meta.toGuestString(key);

                    Object integerValue = meta.java_lang_Integer_valueOf.invokeDirect(value);
                    meta.java_util_HashMap_put.invokeDirect(guestMap, guestKey, integerValue);
                }
                meta.java_util_regex_Pattern_namedGroups_field.setObject(patternObject, guestMap);
            }
            return regexObject;
        }
    }

    private static Object getValue(Object map, String key, InteropLibrary mapInterop) {
        try {
            return mapInterop.readMember(map, key);
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw EspressoError.shouldNotReachHere(e);
        }
    }

    private static String getKey(Object keys, long i, InteropLibrary stringInterop, InteropLibrary arrayInterop) {
        try {
            return stringInterop.asString(arrayInterop.readArrayElement(keys, i));
        } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw EspressoError.shouldNotReachHere(e);
        }
    }

    private static long getArraySize(Object keys, InteropLibrary arrayInterop) {
        try {
            return arrayInterop.getArraySize(keys);
        } catch (UnsupportedMessageException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw EspressoError.shouldNotReachHere(e);
        }
    }

    private static Object getMembers(Object map, InteropLibrary mapInterop) {
        try {
            return mapInterop.getMembers(map);
        } catch (UnsupportedMessageException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw EspressoError.shouldNotReachHere(e);
        }
    }

    private static Object getGroups(Object regexObject, InteropLibrary regexObjectInterop) {
        try {
            return regexObjectInterop.readMember(regexObject, "groups");
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw EspressoError.shouldNotReachHere(e);
        }
    }

    private static int getGroupCount(Object regexObject, InteropLibrary regexObjectInterop, InteropLibrary integerInterop) {
        try {
            return integerInterop.asInt(regexObjectInterop.readMember(regexObject, "groupCount"));
        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw CompilerDirectives.shouldNotReachHere(e);
        }
    }

    @GenerateInline
    @GenerateUncached
    public abstract static class JavaRegexExecNode extends Node {
        public abstract boolean execute(Node node, Object regexObject, StaticObject self, int from, Meta meta);

        @Specialization
        static boolean doDefault(Node node, Object regexObject, StaticObject self, int from, Meta meta,
                        @Cached TruffleString.SubstringByteIndexNode substringNode,
                        @CachedLibrary(limit = "3") InteropLibrary regexObjectInterop,
                        @CachedLibrary(limit = "3") InteropLibrary integerInterop,
                        @CachedLibrary(limit = "3") InteropLibrary booleanInterop,
                        @CachedLibrary(limit = "3") InteropLibrary execResInterop,
                        @Cached InlinedBranchProfile ioobeProfile) {
            Object execRes;
            try {
                TruffleString truffleString = (TruffleString) meta.java_util_regex_Matcher_HIDDEN_tstring.getHiddenObject(self);

                int fromClipped = max(from, 0);
                meta.java_util_regex_Matcher_first.setInt(self, fromClipped);

                // bounds of the region we feed to TRegex
                int regionFrom = meta.java_util_regex_Matcher_from.getInt(self);
                int regionTo = meta.java_util_regex_Matcher_to.getInt(self);

                TruffleString matchTruffleString = truffleString;
                int truffleStringLength = truffleString.byteLength(TruffleString.Encoding.UTF_16) >> 1;
                if (regionFrom != 0 || regionTo != truffleStringLength) {
                    if (regionFrom < 0 || regionTo < regionFrom || regionTo > truffleStringLength) {
                        ioobeProfile.enter(node);
                        meta.throwException(meta.java_lang_IndexOutOfBoundsException);
                    }
                    matchTruffleString = substringNode.execute(truffleString, regionFrom * 2, (regionTo - regionFrom) * 2, TruffleString.Encoding.UTF_16, true);
                }

                execRes = regexObjectInterop.invokeMember(regexObject, "exec", matchTruffleString, fromClipped - regionFrom);
                boolean isMatch = booleanInterop.asBoolean(execResInterop.readMember(execRes, "isMatch"));
                int modCount = meta.java_util_regex_Matcher_modCount.getInt(self);
                meta.java_util_regex_Matcher_modCount.setInt(self, modCount + 1);

                if (isMatch) {
                    int first = regionFrom + integerInterop.asInt(execResInterop.invokeMember(execRes, "getStart", 0));
                    int last = regionFrom + integerInterop.asInt(execResInterop.invokeMember(execRes, "getEnd", 0));

                    meta.java_util_regex_Matcher_first.setInt(self, first);
                    meta.java_util_regex_Matcher_last.setInt(self, last);

                    int groupCount = integerInterop.asInt(regexObjectInterop.readMember(regexObject, "groupCount"));
                    StaticObject array = meta.java_util_regex_Matcher_groups.getObject(self);
                    int[] unwrapped = array.unwrap(meta.getLanguage());
                    for (int i = 0; i < groupCount; i++) {
                        int start = regionFrom + integerInterop.asInt(execResInterop.invokeMember(execRes, "getStart", i));
                        int end = regionFrom + integerInterop.asInt(execResInterop.invokeMember(execRes, "getEnd", i));
                        unwrapped[i * 2] = start;
                        unwrapped[i * 2 + 1] = end;
                    }
                }

                return isMatch;
            } catch (UnsupportedMessageException | UnknownIdentifierException | ArityException | UnsupportedTypeException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        }
    }

    @TruffleBoundary
    private static Source getSource(String method, String pattern, StaticObject patternObject, Meta meta) {
        String combined = "Encoding=UTF-16,Flavor=JavaUtilPattern,MatchingMode=" + method + ",JavaJDKVersion=" + meta.getJavaVersion();
        String sourceStr = combined + '/' + pattern + '/' + convertFlags(meta.java_util_regex_Pattern_flags0.getInt(patternObject));
        return Source.newBuilder("regex", sourceStr, "patternExpr").build();
    }

    @TruffleBoundary
    private static void executeSearch(StaticObject self, int from, Meta meta) {
        meta.java_util_regex_Matcher_search.getCallTargetNoSubstitution().call(self, from);
    }

    @TruffleBoundary
    private static void executeMatch(StaticObject self, int from, int anchor, Meta meta) {
        meta.java_util_regex_Matcher_match.getCallTargetNoSubstitution().call(self, from, anchor);
    }

    private static void reallocateGroupsArrayIfNecessary(StaticObject self, StaticObject parentPattern, Meta meta) {
        int parentGroupCount = max(meta.java_util_regex_Pattern_capturingGroupCount.getInt(parentPattern), 10);
        StaticObject groups = meta.java_util_regex_Matcher_groups.getObject(self);
        if (groups.length(meta.getLanguage()) != parentGroupCount * 2) {
            StaticObject newGroups = meta._int.allocatePrimitiveArray(parentGroupCount * 2);
            meta.java_util_regex_Matcher_groups.setObject(self, newGroups);
        }
    }

    private static void compileFallBackIfRequired(StaticObject self, Meta meta) {
        StaticObject parentPattern = meta.java_util_regex_Matcher_parentPattern.getObject(self);
        if (StaticObject.isNull(meta.java_util_regex_Pattern_root.getObject(parentPattern))) {
            meta.java_util_regex_Pattern_compile.invokeDirect(parentPattern);
            reallocateGroupsArrayIfNecessary(self, parentPattern, meta);

            int localCount = meta.java_util_regex_Pattern_localCount.getInt(parentPattern);
            int localsTCNCount = meta.java_util_regex_Pattern_localTCNCount.getInt(parentPattern);
            StaticObject locals = meta._int.allocatePrimitiveArray(localCount);
            StaticObject localsPos = meta.java_util_regex_IntHashSet.allocateReferenceArray(localsTCNCount);
            meta.java_util_regex_Matcher_locals.setObject(self, locals);
            meta.java_util_regex_Matcher_localsPos.setObject(self, localsPos);
        }
    }

    private static void executeLastWithFallback(StaticObject self, Meta meta) {
        if (meta.java_util_regex_Matcher_HIDDEN_matchingModeBackup.getHiddenObject(self) != StaticObject.NULL) {
            int from = (int) meta.java_util_regex_Matcher_HIDDEN_searchFromBackup.getHiddenObject(self);
            MatchMode action = (MatchMode) meta.java_util_regex_Matcher_HIDDEN_matchingModeBackup.getHiddenObject(self);

            compileFallBackIfRequired(self, meta);
            applyBackup(self, meta);

            switch (action) {
                case MATCH_NOANCHOR -> executeMatch(self, from, NOANCHOR, meta);
                case MATCH_ENDANCHOR -> executeMatch(self, from, ENDANCHOR, meta);
                case SEARCH -> executeSearch(self, from, meta);
            }
        }
    }

    private static void saveBackup(StaticObject self, Meta meta) {
        meta.java_util_regex_Matcher_HIDDEN_oldLastBackup.setHiddenObject(self, meta.java_util_regex_Matcher_oldLast.getValue(self));
        meta.java_util_regex_Matcher_HIDDEN_modCountBackup.setHiddenObject(self, meta.java_util_regex_Matcher_modCount.getValue(self));
        meta.java_util_regex_Matcher_HIDDEN_transparentBoundsBackup.setHiddenObject(self, meta.java_util_regex_Matcher_transparentBounds.getValue(self));
        meta.java_util_regex_Matcher_HIDDEN_anchoringBoundsBackup.setHiddenObject(self, meta.java_util_regex_Matcher_anchoringBounds.getValue(self));
        meta.java_util_regex_Matcher_HIDDEN_fromBackup.setHiddenObject(self, meta.java_util_regex_Matcher_from.getValue(self));
        meta.java_util_regex_Matcher_HIDDEN_toBackup.setHiddenObject(self, meta.java_util_regex_Matcher_to.getValue(self));
    }

    private static void applyBackup(StaticObject self, Meta meta) {
        meta.java_util_regex_Matcher_oldLast.setValue(self, meta.java_util_regex_Matcher_HIDDEN_oldLastBackup.getHiddenObject(self));
        meta.java_util_regex_Matcher_modCount.setValue(self, meta.java_util_regex_Matcher_HIDDEN_modCountBackup.getHiddenObject(self));
        meta.java_util_regex_Matcher_transparentBounds.setValue(self, meta.java_util_regex_Matcher_HIDDEN_transparentBoundsBackup.getHiddenObject(self));
        meta.java_util_regex_Matcher_anchoringBounds.setValue(self, meta.java_util_regex_Matcher_HIDDEN_anchoringBoundsBackup.getHiddenObject(self));
        meta.java_util_regex_Matcher_from.setValue(self, meta.java_util_regex_Matcher_HIDDEN_fromBackup.getHiddenObject(self));
        meta.java_util_regex_Matcher_to.setValue(self, meta.java_util_regex_Matcher_HIDDEN_toBackup.getHiddenObject(self));
    }

    static boolean isUnsupportedMatcher(StaticObject self, Meta meta) {
        StaticObject parentPattern = meta.java_util_regex_Matcher_parentPattern.getObject(self);
        if (Target_java_util_regex_Pattern.isUnsupportedPattern(parentPattern, meta)) {
            return true;
        }
        return !meta.java_util_regex_Matcher_anchoringBounds.getBoolean(self) || meta.java_util_regex_Matcher_transparentBounds.getBoolean(self);
    }

    static boolean isString(StaticObject obj, Meta meta) {
        return obj.getKlass() == meta.java_lang_String;
    }

    private static void saveTruffleString(
                    StaticObject self,
                    TruffleString.FromJavaStringNode fromJavaStringNode,
                    StaticObject espressoString, Meta meta) {
        TruffleString truffleString = fromJavaStringNode.execute(meta.toHostString(espressoString), TruffleString.Encoding.UTF_16);
        meta.java_util_regex_Matcher_HIDDEN_tstring.setHiddenObject(self, truffleString);
    }
}
