/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.espresso.ffi;

/**
 * Represents a valid native signature.
 */
public interface NativeSignature {
    /**
     * Return type.
     */
    NativeType getReturnType();

    /**
     * Number of parameters.
     */
    int getParameterCount();

    /**
     * Number of varargs parameters.
     */
    int getVarArgsParameterCount();

    /**
     * Returns the i-th (0-based) parameter type, guaranteed to be != {@link NativeType#VOID void}.
     *
     * @throws IndexOutOfBoundsException if the index is negative or >= {@link #getParameterCount()}
     *             + {@link #getVarArgsParameterCount()}.
     */
    NativeType parameterTypeAt(int index);

    static NativeSignature create(NativeType returnType, NativeType... parameterTypes) {
        return new NativeSignatureImpl(returnType, parameterTypes);
    }

    static NativeSignature createVarArg(NativeType returnType, NativeType[] parameterTypes, NativeType[] varArgsParameterTypes) {
        return new NativeSignatureImpl(returnType, parameterTypes, varArgsParameterTypes);
    }
}
